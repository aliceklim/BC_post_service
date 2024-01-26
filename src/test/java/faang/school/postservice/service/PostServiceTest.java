package faang.school.postservice.service;

import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.dto.user.UserDto;
import faang.school.postservice.exception.AlreadyDeletedException;
import faang.school.postservice.exception.AlreadyPostedException;
import faang.school.postservice.exception.EntityNotFoundException;
import faang.school.postservice.exception.NotPublishedPostException;
import faang.school.postservice.mapper.PostMapperImpl;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.redis.RedisPost;
import faang.school.postservice.repository.PostRepository;
import faang.school.postservice.validator.PostValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    private PostValidator postValidator;
    @InjectMocks
    private PostService postService;
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
    void getAllPostsByAuthorIdAndPublishedTest(){
        secondPost.setDeleted(false);
        when(postRepository.findByAuthorId(authorId)).thenReturn(twoPostsList);

        List<PostDto> result = postService.getAllPostsByAuthorIdAndPublished(authorId);

        assertEquals(1, result.size());
        assertEquals(postMapper.toDto(secondPost), result.get(0));
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

    @Test
    void getPostByIdDataValidationExceptionTest() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> postService.getPostById(1L));
    }

    @Test
    void createPostDataValidationExceptionTest() {
        when(postRepository.findById(secondPostId)).thenReturn(Optional.of(secondPost));

        assertThrows(AlreadyPostedException.class, () -> postService.publishPost(secondPostId));
    }

    @Test
    void softDeletePostExceptionTest() {
        when(postRepository.findById(secondPostId)).thenReturn(Optional.of(secondPost));

        assertThrows(AlreadyDeletedException.class, () -> postService.softDeletePost(secondPostId));
    }

    @Test
    void getPostByIdExceptionTest() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThrows(NotPublishedPostException.class, () -> postService.getPostById(postId));
    }
 }