package faang.school.postservice.service;

import faang.school.postservice.client.ProjectServiceClient;
import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.dto.PostPair;
import faang.school.postservice.dto.kafka.EventAction;
import faang.school.postservice.dto.kafka.PostEvent;
import faang.school.postservice.dto.post.CreatePostDto;
import faang.school.postservice.dto.post.KafkaPostView;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.dto.post.PostViewEvent;
import faang.school.postservice.dto.post.PostViewEventDto;
import faang.school.postservice.dto.user.UserDto;
import faang.school.postservice.exception.*;
import faang.school.postservice.mapper.PostMapper;
import faang.school.postservice.messaging.publisher.KafkaPostProducer;
import faang.school.postservice.messaging.publisher.KafkaPostViewProducer;
import faang.school.postservice.model.redis.RedisPost;
import faang.school.postservice.messaging.publisher.PostViewEventPublisher;
import faang.school.postservice.messaging.publishing.NewPostPublisher;
import faang.school.postservice.model.Post;
import faang.school.postservice.repository.redis.PostRedisRepository;
import faang.school.postservice.repository.PostRepository;
import faang.school.postservice.repository.ad.AdRepository;
import faang.school.postservice.repository.redis.RedisPostRepository;
import faang.school.postservice.validator.PostValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PostService {
    private final PostMapper postMapper;
    private final UserServiceClient userServiceClient;
    private final ProjectServiceClient projectServiceClient;
    private final PostValidator postValidator;
    private final AdRepository adRepository;
    private final PostRepository postRepository;
    private final RedisPostRepository redisPostRepository;
    private final PublisherService publisherService;
    private final RedisCacheService redisCacheService;
    private final KafkaPostProducer kafkaPostProducer;
    private final PostViewEventPublisher postViewEventPublisher;
    private final KafkaPostViewProducer kafkaPostViewProducer;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Value("${post.moderation.scheduler.sublist-size}")
    private int sublistSize;
    @Value("${spring.data.kafka.util.batch-size}")
    private int batchSize;

    @Transactional
    public PostDto createPost(CreatePostDto createPostDto) {
        Post post = postMapper.toEntity(createPostDto);
        if (createPostDto.getAuthorId() != null && createPostDto.getProjectId() != null) {
            throw new DataValidationException("The author can be either a user or a project");
        }
        if (createPostDto.getAuthorId() != null && userServiceClient.getUserInternal(createPostDto.getAuthorId()) == null) {
            throw new DataValidationException(String.format("No author ID: %s or project ID: %s",
                    createPostDto.getAuthorId(), createPostDto.getProjectId()));
        }

        post.setDeleted(false);
        post.setPublished(false);

        return postMapper.toDto(postRepository.save(post));
    }

    @Transactional
    public PostDto publishPost(long postId) {
        Post post = findPostBy(postId);

        if (post.isPublished() || (post.getScheduledAt() != null
                && post.getScheduledAt().isBefore(LocalDateTime.now()))) {
            throw new AlreadyPostedException("You can't publish post, that has been published");
        }
        if (post.isDeleted()) {
            throw new AlreadyDeletedException(("You can't publish post, that has been deleted"));
        }

        post.setPublished(true);
        post.setPublishedAt(LocalDateTime.now());
        publisherService.publishPostEventToRedis(post);
        log.info("PostId={} was published successfully", post.getId());

        savePostAndAuthorToRedisAndSendEventToKafka(post);
        return postMapper.toDto(post);
    }

    @Transactional
    public PostDto updatePost(PostDto updatePost) {
        long postId = updatePost.getId();
        Post post = findPostBy(postId);
        postValidator.validateAuthorUpdate(post, updatePost);
        LocalDateTime updateScheduleAt = updatePost.getScheduledAt();

        if (updateScheduleAt != null && updateScheduleAt.isAfter(post.getScheduledAt())) {
            post.setScheduledAt(updateScheduleAt);
        }

        post.setContent(updatePost.getContent());
        post.setUpdatedAt(LocalDateTime.now());
        log.info("PostID: {} has been successfully updated ", postId);

        if (post.isDeleted()) {
            publishPostDeleteEventToKafka(post);
        } else if (post.isPublished()) {
            PostPair postPair = buildPostPair(postId, null);
            PostEvent event = buildPostEvent(Collections.emptyList(), postPair, EventAction.UPDATE);

            UserDto userDto = redisCacheService.findUserBy(post.getAuthorId());
            redisCacheService.updateOrCacheUser(userDto);

            kafkaPostProducer.publish(event);
        }

        return postMapper.toDto(post);
    }

    @Transactional
    public PostDto softDeletePost(Long postId) {
        Post post = findPostBy(postId);
        if (post.isDeleted()) {
            throw new AlreadyDeletedException(String.format("PostID: %d has been already deleted", postId));
        }

        post.setDeleted(true);
        postRepository.save(post);
        log.info("PostId={} was soft-deleted successfully", postId);

        publishPostDeleteEventToKafka(post);

        return postMapper.toDto(post);
    }

    @Transactional(readOnly = true)
    public PostDto getPostById(Long postId) {
        Post post = findPostBy(postId);

        if (post.isDeleted()) {
            throw new AlreadyDeletedException("This post has been already deleted");
        }
        if (!post.isPublished()) {
            throw new NotPublishedPostException("This post hasn't been published yet");
        }

        publisherService.publishPostEventToRedis(post);
        publishPostViewEventToKafka(List.of(postId));

        return postMapper.toDto(post);
    }

    public List<PostDto> getUserDrafts(Long userId) {
        postValidator.validateUserId(userId);

        List<PostDto> userDrafts = postRepository.findByAuthorId(userId).stream()
                .filter(post -> !post.isPublished() && !post.isDeleted())
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .map(postMapper::toDto)
                .toList();

        log.info("UserId={} drafts have been taken from DB", userId);
        return userDrafts;
    }

    public List<PostDto> getProjectDrafts(Long projectId) {
        postValidator.validateProjectId(projectId);

        List<PostDto> projectDrafts = postRepository.findByProjectId(projectId).stream()
                .filter(post -> !post.isPublished() && !post.isDeleted())
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .map(postMapper::toDto)
                .toList();

        log.info("Drafts for projectId={} have been taken from DB", projectId);
        return projectDrafts;
    }

    @Transactional(readOnly = true)
    public List<PostDto> getAllPostsByAuthorId(Long authorId) {
        postValidator.validateUserId(authorId);

        List<Post> userPosts = postRepository.findByAuthorIdWithLikes(authorId).stream()
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .peek(publisherService::publishPostEventToRedis)
                .toList();

        List<Long> postIds = getPostIds(userPosts);

        publishPostViewEventToKafka(postIds);

        return userPosts.stream()
                .map(postMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PostDto> getAllPostsByProjectId(Long projectId) {
        postValidator.validateProjectId(projectId);

        List<Post> userPosts = postRepository.findByProjectId(projectId).stream()
                .filter(post -> !post.isDeleted() && !post.isPublished())
                .sorted(Comparator.comparing(Post::getCreatedAt).reversed())
                .peek(publisherService::publishPostEventToRedis)
                .toList();

        List<Long> postIds = getPostIds(userPosts);

        publishPostViewEventToKafka(postIds);

        return userPosts.stream()
                .map(postMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PostDto> getAllPostsByAuthorIdAndPublished(Long authorId) {
        List<Post> userPosts = postRepository.findByAuthorId(authorId).stream()
                .filter(post -> !post.isDeleted() && post.isPublished())
                .sorted(Comparator.comparing(Post::getCreatedAt).reversed())
                .peek(publisherService::publishPostEventToRedis)
                .toList();

        List<Long> postIds = getPostIds(userPosts);

        publishPostViewEventToKafka(postIds);

        return userPosts.stream()
                .map(postMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PostDto> getAllPostsByProjectIdAndPublished(Long projectId) {
        List<Post> userPosts = postRepository.findByProjectId(projectId).stream()
                .filter(post -> !post.isDeleted() && post.isPublished())
                .sorted(Comparator.comparing(Post::getCreatedAt).reversed())
                .peek(publisherService::publishPostEventToRedis)
                .toList();

        List<Long> postIds = getPostIds(userPosts);

        publishPostViewEventToKafka(postIds);

        return userPosts.stream()
                .map(postMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PostDto> getAllPostsByHashtagId(String content, Pageable pageable){
        return postRepository.findByHashtagsContent(content, pageable)
                .map(post -> {
                    publisherService.publishPostEventToRedis(post);
                    publishPostViewEventToKafka(List.of(post.getId()));
                    return postMapper.toDto(post);
                });
    }

    @Transactional(readOnly = true)
    public Post getPostByIdInternal(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new DataValidationException("'Post not in database' error occurred while fetching post"));
    }

    @Transactional
    public Post updatePostInternal(Post post){
        return postRepository.save(post);
    }

    public void doPostModeration() {
        log.info("<doPostModeration> was called successfully");
        List<Post> notVerifiedPost = postRepository.findNotVerified();
        List<List<Post>> partitionList = new ArrayList<>();

        if (notVerifiedPost.size() > sublistSize) {
            partitionList = Lists.partition(notVerifiedPost, sublistSize);
        } else {
            partitionList.add(notVerifiedPost);
        }

        partitionList.forEach(list -> threadPoolForPostModeration.execute(() -> checkListForObsceneWords(list)));
        log.info("All posts have checked successfully");
    }

    private void publishPostDeleteEventToKafka(Post post) {
        long postId = post.getId();
        long authorId = post.getAuthorId();

        UserDto userDto = redisCacheService.findUserBy(authorId);
        redisCacheService.updateOrCacheUser(userDto);

        List<Long> followerIds = userDto.getFollowerIds();
        PostPair postPair = buildPostPair(postId, post.getPublishedAt());

        if (followerIds != null && !followerIds.isEmpty()) {
            log.info("Author of Post with ID: {} has {} amount of followeers", authorId, followerIds.size());

            publishPostPublishOrDeleteEventToKafka(followerIds, postPair, EventAction.DELETE);
        } else {
            log.warn("Author of Post with ID: {} does not have any followers", authorId);

            PostEvent event = buildPostEvent(Collections.emptyList(), postPair, EventAction.DELETE);
            kafkaPostProducer.publish(event);
        }
    }
    public void publishPostViewEventToKafka(List<Long> postIds) {
        threadPoolTaskExecutor.execute(() -> {
            postIds.forEach(postId -> {
                PostViewEvent event = buildPostViewEvent(postId);

                kafkaPostViewProducer.publish(event);
            });
        });
    }
    private void savePostAndAuthorToRedisAndSendEventToKafka(Post post) {
        long postId = post.getId();
        long authorId = post.getAuthorId();

        UserDto userDto = redisCacheService.findUserBy(authorId);
        redisCacheService.updateOrCacheUser(userDto);

        RedisPost redisPost = redisCacheService.mapPostToRedisPostAndSetDefaultVersion(post);
        redisCacheService.saveRedisPost(redisPost);

        List<Long> followerIds = userDto.getFollowerIds();
        PostPair postPair = buildPostPair(postId, post.getPublishedAt());

        publishPostPublishOrDeleteEventToKafka(followerIds, postPair, EventAction.CREATE);
    }
    private void publishPostPublishOrDeleteEventToKafka(List<Long> followersIds, PostPair postPair, EventAction eventAction) {
        threadPoolTaskExecutor.execute(() -> {
            for (int i = 0; i < followersIds.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, followersIds.size());
                List<Long> sublist = followersIds.subList(i, endIndex);

                PostEvent event = buildPostEvent(sublist, postPair, eventAction);

                kafkaPostProducer.publish(event);
            }
        });
    }

    @Transactional
    public Post findPostBy(long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException(String.format("PostID: %d, doesn't exist", postId)));
    }
    private PostPair buildPostPair(long postId, LocalDateTime publishedAt) {
        return PostPair.builder()
                .postId(postId)
                .publishedAt(publishedAt)
                .build();
    }

    private PostEvent buildPostEvent(List<Long> followerIds, PostPair postPair, EventAction eventAction) {
        return PostEvent.builder()
                .postPair(postPair)
                .followersIds(followerIds)
                .eventAction(eventAction)
                .build();
    }

    private PostViewEvent buildPostViewEvent(long postId) {
        return PostViewEvent.builder()
                .postId(postId)
                .build();
    }

    private List<Long> getPostIds(List<Post> posts){
        return posts.stream().map(Post::getId).toList();
    }
}