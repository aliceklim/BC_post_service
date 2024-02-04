package faang.school.postservice.service;

import faang.school.postservice.dto.PostPair;
import faang.school.postservice.dto.kafka.EventAction;
import faang.school.postservice.dto.kafka.PostEvent;
import com.google.common.collect.Lists;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.dto.post.PostViewEvent;
import faang.school.postservice.dto.user.UserDto;
import faang.school.postservice.exception.*;
import faang.school.postservice.mapper.PostMapper;
import faang.school.postservice.messaging.publisher.KafkaPostProducer;
import faang.school.postservice.messaging.publisher.KafkaPostViewProducer;
import faang.school.postservice.model.redis.RedisPost;
import faang.school.postservice.model.Post;
import faang.school.postservice.repository.PostRepository;
import faang.school.postservice.service.moderation.ModerationDictionary;
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
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PostService {
    private final PostMapper postMapper;
    private final PostValidator postValidator;
    private final PostRepository postRepository;
    private final PublisherService publisherService;
    private final RedisCacheService redisCacheService;
    private final KafkaPostProducer kafkaPostProducer;
    private final KafkaPostViewProducer kafkaPostViewProducer;
    private final Executor threadPoolForPostModeration;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final ModerationDictionary moderationDictionary;

    @Value("${post.moderation.scheduler.sublist-size}")
    private int sublistSize;
    @Value("${spring.data.kafka.util.batch-size}")
    private int batchSize;

    @Transactional
    public PostDto crateDraftPost(PostDto postDto) {
        postValidator.validateData(postDto);

        Post savedPost = postRepository.save(postMapper.toEntity(postDto));
        log.info("Draft post was created successfully, draftId={}", savedPost.getId());
        return postMapper.toDto(savedPost);
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
        log.info("Post moderation started");
        List<Post> notVerifiedPost = postRepository.findNotVerified();
        List<List<Post>> partitionList = new ArrayList<>();

        if (notVerifiedPost.size() > sublistSize) {
            partitionList = Lists.partition(notVerifiedPost, sublistSize);
        } else {
            partitionList.add(notVerifiedPost);
        }

        partitionList.forEach(list -> threadPoolForPostModeration.execute(() -> checkListForObsceneWords(list)));
        log.info("All posts have been moderated");
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

    @Transactional
    public Optional<Post> findAlreadyPublishedAndNotDeletedPost(long postId) {
        return postRepository.findPublishedAndNotDeletedBy(postId);

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

    private void checkListForObsceneWords(List<Post> posts) {
        posts.forEach(post -> {
            boolean checkResult = moderationDictionary.checkWordContent(post.getContent());
            log.info("PostId={} has been checked for obscene words", post.getId());
            post.setVerified(!checkResult);
            post.setVerifiedDate(LocalDateTime.now());
        });

        postRepository.saveAll(posts);
    }

    public void incrementPostViewByPostId(long postId) {
        postRepository.incrementPostViewByPostId(postId);
    }

    public List<RedisPost> findRedisPostsByAndCacheThemIfNotExist(List<Long> postIds) {
        return postIds.stream()
                .map(this::findRedisPostAndCacheIfNotExist)
                .collect(Collectors.toList());
    }

    @Transactional
    public RedisPost findRedisPostAndCacheIfNotExist(long postId) {
        return redisCacheService.findRedisPostBy(postId)
                .orElseGet(() -> findPostByIdAndCacheHim(postId));
    }

    private RedisPost findPostByIdAndCacheHim(long postId) {
        log.warn("PostID {} was not found in Redis. Retrieving from the database and caching in Redis.", postId);

        Post post = findAlreadyPublishedAndNotDeletedPost(postId)
                .orElseThrow(() -> new EntityNotFoundException(String.format(
                        "Post with ID: %d not published yet or already deleted", postId)));
        return redisCacheService.cachePost(post);
    }

    @Transactional
    public List<Post> findSortedPostsByAuthorIdsLimit(List<Long> authorIds, long requiredAmount) {
        return postRepository.findSortedPostsByAuthorIdsAndLimit(authorIds, requiredAmount);
    }

    @Transactional
    public List<Post> findSortedPostsByAuthorIdsNotInPostIdsLimit(List<Long> authorIds, List<Long> usedPostIds,
                                                                  int amount) {
        return postRepository.findSortedPostsByAuthorIdsNotInPostIdsLimit(authorIds, usedPostIds, amount);
    }

    @Transactional
    public List<Post> findSortedPostsFromPostDateAndAuthorsLimit(List<Long> followees, LocalDateTime lastPostDate,
                                                                 int limit) {
        return postRepository.findSortedPostsFromPostDateAndAuthorsLimit(followees, lastPostDate, limit);
    }
}