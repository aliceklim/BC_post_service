package faang.school.postservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
public class RedisPostControllerTest {
    @Mock
    private PostService postService;
    private ObjectMapper objectMapper;
    @InjectMocks
    private PostController postController;
    private MockMvc mockMvc;
    private PostDto incorrectPostDto;
    private PostDto correctPostDto;
    private final String POST_CONTENT = "some content for test";
    private final Long CORRECT_ID = 1L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(postController).build();
        objectMapper = new ObjectMapper();
        incorrectPostDto = PostDto.builder()
                .content("   ")
                .build();
        correctPostDto = PostDto.builder()
                .id(CORRECT_ID)
                .content(POST_CONTENT)
                .authorId(CORRECT_ID)
                .build();
    }

    @Test
    void testCreateDaftPost() {
        postController.createPost(correctPostDto);

        verify(postService).crateDraftPost(correctPostDto);
    }

    @Test
    void testPublishPost() {
        postController.publishPost(CORRECT_ID);

        verify(postService).publishPost(CORRECT_ID);
    }

    @Test
    void testUpdatePost() {
        correctPostDto.setId(CORRECT_ID);

        postController.updatePost(correctPostDto);

        verify(postService).updatePost(correctPostDto);
    }

    @Test
    void testSoftDelete() {
        postController.softDeletePost(CORRECT_ID);

        verify(postService).softDeletePost(CORRECT_ID);
    }

    @Test
    void testGetUserDrafts() {
        postController.getUserDrafts(CORRECT_ID);

        verify(postService).getUserDrafts(CORRECT_ID);
    }

    @Test
    void testGetProjectDrafts() {
        postController.getProjectDrafts(CORRECT_ID);

        verify(postService).getProjectDrafts(CORRECT_ID);
    }

    @Test
    void testGetUserPosts() {
        postController.getAllPostsByAuthorId(CORRECT_ID);

        verify(postService).getAllPostsByAuthorId(CORRECT_ID);
    }

    @Test
    void testGetProjectPosts() {
        postController.getAllPostsByProjectId(CORRECT_ID);

        verify(postService).getAllPostsByProjectId(CORRECT_ID);
    }

    @Test
    void publishPost() throws Exception {
        mockMvc.perform(post("/api/v1/posts/1/publish"))
                .andExpect(status().isOk());
    }

    @Test
    void softDeletePostTest() throws Exception {
        Long postId = 1L;

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