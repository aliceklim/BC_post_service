package faang.school.postservice.service;

import faang.school.postservice.client.ProjectServiceClient;
import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.dto.post.CreatePostDto;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.dto.post.RedisPostDto;
import faang.school.postservice.dto.project.ProjectDto;
import faang.school.postservice.dto.user.UserDto;
import faang.school.postservice.exception.DataValidationException;
import faang.school.postservice.mapper.PostMapperImpl;
import faang.school.postservice.messaging.publisher.KafkaPostViewProducer;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.ad.Ad;
import faang.school.postservice.messaging.publisher.PostViewEventPublisher;
import faang.school.postservice.model.redis.RedisPost;
import faang.school.postservice.repository.PostRepository;
import faang.school.postservice.repository.ad.AdRepository;
import faang.school.postservice.validator.PostValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {
    @Mock
    private PublisherService publisherService;
    @Mock
    private ThreadPoolTaskExecutor postEventTaskExecutor;
    @Mock
    private RedisCacheService redisCacheService;
    @Mock
    private PostRepository postRepository;
    @Spy
    private PostMapperImpl postMapper;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private PostViewEventPublisher postViewEventPublisher;
    @Mock
    private ProjectServiceClient projectServiceClient;
    @Mock
    private KafkaPostViewProducer kafkaPostViewProducer;
    @Mock
    private PostValidator postValidator;
    @InjectMocks
    private PostService postService;
    private Post postOne;
    private Post postTwo;
    private Post postTree;
    private PostDto postDtoOne;
    private PostDto postDtoTwo;
    private PostDto postDtoTree;
    List<PostDto> posts;
    private PostDto publishPostDto;
    private PostDto updatePostDto;
    private Post post;
    private Post secondPost;
    private List<Post> twoPostsList;
    private RedisPost redisPost;
    private UserDto userDto;
    private LocalDateTime currentTime;
    private LocalDateTime scheduledAt;
    private LocalDateTime updatedScheduledAt;
    private final Long authorId = 1L;
    private final Long projectId = 1L;
    private final Long postId = 1L;
    private final Long secondPostId = 2L;
    private final Long firstFollowerId = 10L;
    private final Long secondFollowerId = 20L;
    private final String content = "Content";
    private final String updatedContent = "UpdatedContent";


    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(postService, "sublistSize", 100);
        ReflectionTestUtils.setField(postService, "batchSize", 1000);
        currentTime = LocalDateTime.now();
        scheduledAt = LocalDateTime.now().plusDays(1);
        updatedScheduledAt = LocalDateTime.now().plusDays(2);
        publishPostDto = PostDto.builder()
                .content(content)
                .authorId(authorId)
                .build();
        updatePostDto = PostDto.builder()
                .id(postId)
                .content(updatedContent)
                .scheduledAt(updatedScheduledAt)
                .build();
        post = Post.builder()
                .id(postId)
                .projectId(projectId)
                .content(content)
                .authorId(authorId)
                .published(false)
                .deleted(false)
                .scheduledAt(scheduledAt)
                .createdAt(currentTime)
                .build();
        secondPost = Post.builder()
                .id(secondPostId)
                .projectId(projectId)
                .content(updatedContent)
                .authorId(authorId)
                .published(true)
                .deleted(true)
                .scheduledAt(updatedScheduledAt)
                .createdAt(currentTime)
                .build();
        twoPostsList = new ArrayList<>(List.of(post, secondPost));
        redisPost = RedisPost.builder()
                .postId(postId)
                .content(content)
                .authorId(authorId)
                .version(1)
                .build();
        userDto = UserDto.builder()
                .id(authorId)
                .followerIds(List.of(firstFollowerId, secondFollowerId))
                .build();

    }

    @Test
    void cratePostTest() {
        when(postRepository.save(postMapper.toEntity(publishPostDto))).thenReturn(post);

        PostDto result = postService.crateDraftPost(publishPostDto);

        assertEquals(postMapper.toDto(post), result);

        verify(postValidator).validateData(publishPostDto);
        verify(postRepository).save(postMapper.toEntity(publishPostDto));
    }

    @Test
    void publishPostTest() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(redisCacheService.findUserBy(authorId)).thenReturn(userDto);
        when(redisCacheService.mapPostToRedisPostAndSetDefaultVersion(any(Post.class))).thenReturn(redisPost);

        PostDto result = postService.publishPost(postId);

        assertTrue(result.isPublished());

        verify(postRepository).findById(postId);
        verify(publisherService).publishPostEventToRedis(any(Post.class));
        verify(redisCacheService).findUserBy(authorId);
        verify(redisCacheService).updateOrCacheUser(userDto);
        verify(redisCacheService).mapPostToRedisPostAndSetDefaultVersion(any(Post.class));
        verify(redisCacheService).saveRedisPost(redisPost);
        verify(postEventTaskExecutor).execute(any(Runnable.class));
    }

    @Test
    void updatePostTest() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        PostDto expected = postMapper.toDto(post);
        expected.setContent(updatedContent);

        PostDto result = postService.updatePost(updatePostDto);

        assertEquals(updatedContent, result.getContent());

        verify(postRepository).findById(postId);
        verify(postValidator).validateAuthorUpdate(post, updatePostDto);
    }

    @Test
    void softDeleteTest() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(redisCacheService.findUserBy(authorId)).thenReturn(userDto);
        PostDto result = postService.softDeletePost(postId);

        assertTrue(result.isDeleted());

        verify(redisCacheService).findUserBy(authorId);
        verify(redisCacheService).updateOrCacheUser(userDto);
        verify(postEventTaskExecutor).execute(any(Runnable.class));
    }

    @Test
    void getPostTest() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        post.setPublished(true);
        PostDto result = postService.getPostById(postId);

        assertEquals(postMapper.toDto(post), result);

        verify(postRepository).findById(postId);
        verify(publisherService).publishPostEventToRedis(post);
        verify(postEventTaskExecutor).execute(any(Runnable.class));
    }

    @Test
    void getUserDraftsTest() {
        when(postRepository.findByAuthorId(authorId)).thenReturn(twoPostsList);
        List<PostDto> result = postService.getUserDrafts(authorId);

        assertEquals(1, result.size());
        assertEquals(postMapper.toDto(post), result.get(0));

        verify(postRepository).findByAuthorId(authorId);
        verify(postValidator).validateUserId(authorId);
    }

    @Test
    void getProjectDraftsTest() {
        when(postRepository.findByProjectId(1L)).thenReturn(twoPostsList);
        secondPost.setPublished(false);
        secondPost.setDeleted(false);

        PostDto firstExpected = postMapper.toDto(post);
        PostDto secondExpected = postMapper.toDto(secondPost);
        List<PostDto> result = postService.getProjectDrafts(1L);

        assertEquals(2, result.size());
        assertEquals(secondExpected, result.get(1));
        assertEquals(List.of(firstExpected, secondExpected), result);
        verify(postRepository).findByProjectId(1L);
        verify(postValidator).validateProjectId(1L);
    }

    @Test
    void getAllPostsByAuthorIdTest() {
        when(postRepository.findByAuthorIdWithLikes(authorId)).thenReturn(twoPostsList);
        List<PostDto> result = postService.getAllPostsByAuthorId(authorId);

        assertEquals(2, result.size());
        assertEquals(postMapper.toDto(post), result.get(0));

        verify(postValidator).validateUserId(authorId);
        verify(postRepository).findByAuthorIdWithLikes(authorId);
        verify(publisherService).publishPostEventToRedis(post);
        verify(postEventTaskExecutor).execute(any(Runnable.class));
    }

    @Test
    void getProjectPostsTest() {
        lenient().when(postRepository.findByProjectId(1L)).thenReturn(twoPostsList);
        secondPost.setPublished(false);
        secondPost.setDeleted(false);
        PostDto firstExpected = postMapper.toDto(post);
        PostDto secondExpected = postMapper.toDto(secondPost);

        List<PostDto> result = postService.getAllPostsByProjectId(1L);

        assertEquals(2, result.size());
        assertEquals(secondExpected, result.get(1));
        assertEquals(List.of(firstExpected, secondExpected), result);
        verify(postValidator).validateProjectId(1L);
        verify(postRepository).findByProjectId(1L);
        verify(publisherService, times(2)).publishPostEventToRedis(any(Post.class));
    }

    @Test
    void findByPostTest() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        Post result = postService.findPostBy(postId);

        assertEquals(post, result);
    }





   /* @Test
    void testCreatePostDataValidationException() {
        assertThrows(DataValidationException.class, () -> postService.createPost(publishPostDto));
    }

    @Test
    void testCreatePostMockAuthorDataValidationException() {
        when(userServiceClient.getUserInternal(1L)).thenReturn(null);
        assertThrows(DataValidationException.class, () -> postService.createPost(publishPostDto));
    }

    @Test
    void testCreatePostMockProjectDataValidationException() {
        when(projectServiceClient.getProject(1L)).thenReturn(null);
        assertThrows(DataValidationException.class, () -> postService.createPost(publishPostDto));
    }

    @Test
    void testCreatePost() {
        Post post = Post.builder()
                .authorId(null).projectId(1L)
                .deleted(false).published(false).build();
        CreatePostDto createPostDto = CreatePostDto.builder().authorId(null).projectId(1L).build();
        RedisPostDto redisPostDto = RedisPostDto.builder().id(1L).content("test").authorId(1L).build();

        when(projectServiceClient.getProject(1L)).thenReturn(new ProjectDto());
        postService.createPost(createPostDto);
        postRedisRepository.save(redisPostDto);
        verify(postRepository).save(post);
    }

    @Test
    void testPublishPostArrayList() {
        when(postRepository.findReadyToPublish()).thenReturn(new ArrayList<>());
        assertEquals(new ArrayList<>(), postService.publishPost());
    }

    @Test
    void testPublishPostSave() {
        Post postTwo2 = Post.builder().id(2L)
                .createdAt(LocalDateTime.of(2022, 1, 1, 0, 0))
                .deleted(false).published(true).build();

        when(postRepository.findReadyToPublish()).thenReturn(new ArrayList<>(List.of(postOne, postTwo, postTree)));
        postService.publishPost();
        verify(postRepository).save(postTwo2);
    }

    @Test
    void testPublishPost() {
        Post postTwo2 = Post.builder().id(2L)
                .createdAt(LocalDateTime.of(2022, 1, 1, 0, 0))
                .deleted(false).published(true).build();

        when(postRepository.findReadyToPublish()).thenReturn(new ArrayList<>(List.of(postOne, postTwo, postTree)));
        postService.publishPost();
        verify(postMapper).toDtoList(List.of(postOne, postTwo2, postTree));
    }

    @Test
    void testUpdatePostDataValidationException() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(DataValidationException.class, () -> postService.updatePost(1L, updatePostDto));
    }

    @Test
    void testUpdatePostAdDataValidationException() {
        Post post123 = Post.builder().id(1L).ad(Ad.builder().id(1L).build()).build();

        when(postRepository.findById(1L)).thenReturn(Optional.of(post123));
        when(adRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(DataValidationException.class, () -> postService.updatePost(1L, updatePostDto));
    }

    @Test
    void testUpdatePost() {
        Post post = Post.builder().id(1L).build();
        PostDto postDto = PostDto.builder().id(1L).build();

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(adRepository.findById(1L)).thenReturn(Optional.of(Ad.builder().id(1L).build()));
        when(postMapper.toDto(post)).thenReturn(postDto);
        when(postRepository.save(post)).thenReturn(post);

        assertEquals(postDto, postService.updatePost(1L, updatePostDto));
    }

    @Test
    void softDeletePostDataValidationException() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(DataValidationException.class, () -> postService.softDeletePost(1L));
    }

    @Test
    void softDeletePost() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(postOne));
        when(postMapper.toDto(postOne)).thenReturn(postDtoOne);
        when(postRepository.save(postOne)).thenReturn(postOne);

        assertEquals(postDtoOne, postService.softDeletePost(1L));
    }

    @Test
    void getPostByIdDataValidationException() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(DataValidationException.class, () -> postService.getPostById(1L));
    }

    @Test
    void getPostById() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(postOne));
        when(postMapper.toDto(postOne)).thenReturn(postDtoOne);
        doNothing().when(kafkaPostViewProducer).sendMessage(Mockito.any(KafkaPostView.class));
        assertEquals(postDtoOne, postService.getPostById(1L));
    }

    @Test
    void getAllPostsByAuthorId() {
        when(postRepository.findByAuthorId(1L)).thenReturn(List.of(postOne, postTwo, postTree));
        when(postMapper.toDto(postTwo)).thenReturn(postDtoTwo);

        assertEquals(List.of(postDtoTwo), postService.getAllPostsByAuthorId(1L));
    }

    @Test
    void getAllPostsByProjectId() {
        when(postRepository.findByProjectId(1L)).thenReturn(List.of(postOne, postTwo, postTree));
        when(postMapper.toDto(postTwo)).thenReturn(postDtoTwo);

        assertEquals(List.of(postDtoTwo), postService.getAllPostsByProjectId(1L));
    }

    @Test
    void getAllPostsByAuthorIdAndPublished() {
        when(postRepository.findByAuthorId(1L)).thenReturn(List.of(postOne, postTwo, postTree));
        when(postMapper.toDto(postOne)).thenReturn(postDtoOne);
        when(postMapper.toDto(postTree)).thenReturn(postDtoTree);

        assertEquals(posts, postService.getAllPostsByAuthorIdAndPublished(1L));
    }

    @Test
    void getAllPostsByProjectIdAndPublished() {
        when(postRepository.findByProjectId(1L)).thenReturn(List.of(postOne, postTwo, postTree));
        when(postMapper.toDto(postOne)).thenReturn(postDtoOne);
        when(postMapper.toDto(postTree)).thenReturn(postDtoTree);

        assertEquals(posts, postService.getAllPostsByProjectIdAndPublished(1L));
    }
*/}