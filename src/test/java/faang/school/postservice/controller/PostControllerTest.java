package faang.school.postservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
public class PostControllerTest {
    @Mock
    private PostService postService;
    private ObjectMapper objectMapper;
    @InjectMocks
    private PostController postController;
    private MockMvc mockMvc;
    private PostDto incorrectPostDto;
    private PostDto correctPostDto;
    private String postContent = "some content for test";
    private Long postId = 1L;
    private Long userId = 1L;
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(postController).build();
        objectMapper = new ObjectMapper();
        incorrectPostDto = PostDto.builder()
                .content("   ")
                .build();
        correctPostDto = PostDto.builder()
                .id(postId)
                .content(postContent)
                .authorId(postId)
                .build();
    }

    @Test
    void testCreateDaftPost() {
        postController.createPost(userId, correctPostDto);

        verify(postService).crateDraftPost(correctPostDto);
    }

    @Test
    void testPublishPost() {
        postController.publishPost(userId, postId);

        verify(postService).publishPost(postId);
    }

    @Test
    void testUpdatePost() {
        correctPostDto.setId(postId);

        postController.updatePost(userId, correctPostDto);

        verify(postService).updatePost(correctPostDto);
    }

    @Test
    void testSoftDelete() {
        postController.softDeletePost(userId, postId);

        verify(postService).softDeletePost(postId);
    }

    @Test
    void testGetUserDrafts() {
        postController.getUserDrafts(userId, postId);

        verify(postService).getUserDrafts(postId);
    }

    @Test
    void testGetProjectDrafts() {
        postController.getProjectDrafts(userId, postId);

        verify(postService).getProjectDrafts(postId);
    }

    @Test
    void testGetUserPosts() {
        postController.getAllPostsByAuthorId(userId, postId);

        verify(postService).getAllPostsByAuthorId(postId);
    }

    @Test
    void testGetProjectPosts() {
        postController.getAllPostsByProjectId(userId, postId);

        verify(postService).getAllPostsByProjectId(postId);
    }

    @Test
    void publishPost() throws Exception {
        mockMvc.perform(post("/api/v1/posts/1/publish"))
                .andExpect(status().isOk());
    }

    @Test
    void softDeletePostTest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/posts/{id}/soft-delete", postId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void createPostTest() throws Exception {
        mockMvc.perform(post("/api/v1/posts/drafts")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(correctPostDto)))
                .andExpect(status().isOk());
    }

    @Test
    void createInvalidPostTest() throws Exception {
        mockMvc.perform(post("/api/v1/posts/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(incorrectPostDto)))
                .andExpect(status().isBadRequest());
    }
}