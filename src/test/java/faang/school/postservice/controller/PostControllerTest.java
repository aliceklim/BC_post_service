package faang.school.postservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
public class PostControllerTest {
    @Mock
    private PostService postService;
    private ObjectMapper objectMapper;
    @Mock
    private UserContext userContext;
    @InjectMocks
    private PostController postController;
    private MockMvc mockMvc;
    private PostDto incorrectPostDto;
    private PostDto correctPostDto;
    private String postContent = "some content for test";
    private Long postId = 1L;
    private Long userId = 1L;
    private Long projectId = 1L;
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
        mockMvc.perform(post("/api/v1/posts/1/publish")
                        .header("x-user-id", String.valueOf(userId)))
                .andExpect(status().isOk());
    }

    @Test
    void softDeletePostTest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/posts/{id}/soft-delete", postId)
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void createDraftPostTest() throws Exception {
        mockMvc.perform(post("/api/v1/posts/drafts")
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(correctPostDto)))
                .andExpect(status().isOk());
    }

    @Test
    void createInvalidDraftPostTest() throws Exception {
        mockMvc.perform(post("/api/v1/posts/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incorrectPostDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publishPostTest() throws Exception {
        mockMvc.perform(post("/api/v1/posts/{id}/publish", postId)
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void updatePostTest() throws Exception {
        mockMvc.perform(put("/api/v1/posts/edit")
                .header("x-user-id", String.valueOf(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(correctPostDto)))
                .andExpect(status().isOk());
    }

    @Test
    void softDeletionPostTest() throws Exception {
        mockMvc.perform(put("/api/v1/posts/{id}/soft-delete", postId)
                .header("x-user-id", String.valueOf(userId))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void getPostByIdTest() throws Exception {
        mockMvc.perform(get("/api/v1/posts/{id}", postId)
                .header("x-user-id", String.valueOf(userId))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void getUserDraftsTest() throws Exception{
        mockMvc.perform(get("/api/v1/posts/drafts/users/{id}", userId)
                .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void getProjectDraftsTest() throws Exception{
        mockMvc.perform(get("/api/v1/posts/drafts/projects/{id}", projectId)
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void getAllPostsByAuthorIdTest() throws Exception{
        mockMvc.perform(get("/api/v1/posts/author/{userId}/all", userId)
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void getAllPostsByProjectIdTest() throws Exception{
        mockMvc.perform(get("/api/v1/posts/project/{projectId}/all", projectId)
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void getAllPostsByAuthorIdAndPublishedTest() throws Exception{
        mockMvc.perform(get("/api/v1/posts/all/author/{userId}/published", userId)
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void getAllPostsByProjectIdAndPublishedTest() throws Exception{
        mockMvc.perform(get("/api/v1/posts/all/project/{projectId}/published", projectId)
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}