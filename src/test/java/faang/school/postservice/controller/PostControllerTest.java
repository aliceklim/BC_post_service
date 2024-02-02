package faang.school.postservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import java.util.List;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    private PostDto postDto;
    private String postContent = "some content for test";
    private Long postId = 1L;
    private Long userId = 1L;
    private Long projectId = 1L;
    private ArgumentCaptor<PostDto> postDtoCaptor;
    private ArgumentCaptor<Long> idCaptor;
    private MvcResult mvcResult;
    private String actualResponseBody;

    @BeforeEach
    void setUp() {
        postDtoCaptor = ArgumentCaptor.forClass(PostDto.class);
        idCaptor = ArgumentCaptor.forClass(Long.class);
        mockMvc = MockMvcBuilders.standaloneSetup(postController).build();
        objectMapper = new ObjectMapper();
        incorrectPostDto = PostDto.builder()
                .content("   ")
                .build();
        postDto = PostDto.builder()
                .id(postId)
                .content(postContent)
                .authorId(postId)
                .build();
    }

    @Test
    void publishPostTest() throws Exception {
        when(postService.publishPost(postId)).thenReturn(postDto);
        mvcResult = mockMvc.perform(post("/api/v1/posts/{id}/publish", postId)
                        .header("x-user-id", String.valueOf(userId)))
                .andReturn();

        actualResponseBody = mvcResult.getResponse().getContentAsString();

        verify(postService, times(1)).publishPost(idCaptor.capture());
        assertEquals(postId, idCaptor.getValue());
        assertThat(actualResponseBody).isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(postDto));
    }

    @Test
    void softDeletePostTest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/posts/{id}/soft-delete", postId)
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());

        verify(postService, times(1)).softDeletePost(idCaptor.capture());
        assertEquals(postId, idCaptor.getValue());
    }

    @Test
    void createDraftPostTest() throws Exception {
        when(postService.crateDraftPost(postDto)).thenReturn(postDto);
        mvcResult = mockMvc.perform(post("/api/v1/posts/drafts")
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(postDto)))
                .andExpect(status().isOk())
                        .andReturn();

        actualResponseBody = mvcResult.getResponse().getContentAsString();

        verify(postService, times(1)).crateDraftPost(postDtoCaptor.capture());
        assertEquals(postDto, postDtoCaptor.getValue());
        assertThat(actualResponseBody).isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(postDto));
    }

    @Test
    void createDraftPostWhenPostIdNull_ThenReturns400Test() throws Exception {
        postDto.setId(null);
        mockMvc.perform(post("/api/v1/posts/drafts")
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(postDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDraftPostWhenPostContentAbsent_ThenReturns400Test() throws Exception {
        postDto.setContent("");
        mockMvc.perform(post("/api/v1/posts/drafts")
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(postDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDraftPostWhenPostAuthorIsNull_ThenReturns400Test() throws Exception {
        postDto.setAuthorId(null);
        mockMvc.perform(post("/api/v1/posts/drafts")
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(postDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createInvalidDraftPostTest() throws Exception {
        mockMvc.perform(post("/api/v1/posts/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incorrectPostDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatePostTest() throws Exception {
        when(postService.updatePost(postDto)).thenReturn(postDto);
        mvcResult = mockMvc.perform(put("/api/v1/posts/edit")
                .header("x-user-id", String.valueOf(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postDto)))
                .andExpect(status().isOk())
                .andReturn();

        actualResponseBody = mvcResult.getResponse().getContentAsString();

        verify(postService, times(1)).updatePost(postDtoCaptor.capture());
        assertEquals(postDto, postDtoCaptor.getValue());
        assertThat(actualResponseBody).isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(postDto));
    }

    @Test
    void updatePostWhenPostIdNull_ThenReturns400Test() throws Exception {
        postDto.setId(null);
        mockMvc.perform(put("/api/v1/posts/edit")
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(postDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatePostWhenPostContentAbsent_ThenReturns400Test() throws Exception {
        postDto.setContent("");
        mockMvc.perform(put("/api/v1/posts/edit")
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(postDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatePostWhenPostAuthorIsNull_ThenReturns400Test() throws Exception {
        postDto.setAuthorId(null);
        mockMvc.perform(put("/api/v1/posts/edit")
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(postDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void softDeletionPostTest() throws Exception {
        mockMvc.perform(put("/api/v1/posts/{id}/soft-delete", postId)
                .header("x-user-id", String.valueOf(userId))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(postService, times(1)).softDeletePost(idCaptor.capture());
        assertEquals(postId, idCaptor.getValue());
    }

    @Test
    void getPostByIdTest() throws Exception {
        when(postService.getPostById(postId)).thenReturn(postDto);
        mvcResult = mockMvc.perform(get("/api/v1/posts/{id}", postId)
                .header("x-user-id", String.valueOf(userId))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        actualResponseBody = mvcResult.getResponse().getContentAsString();

        verify(postService, times(1)).getPostById(idCaptor.capture());
        assertEquals(postId, idCaptor.getValue());
        assertThat(actualResponseBody).isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(postDto));
    }

    @Test
    void getUserDraftsTest() throws Exception{
        postDto.setAuthorId(1l);
        when(postService.getUserDrafts(1L)).thenReturn(List.of(postDto));
        mvcResult = mockMvc.perform(get("/api/v1/posts/drafts/users/{id}", userId)
                .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        actualResponseBody = mvcResult.getResponse().getContentAsString();

        verify(postService, times(1)).getUserDrafts(idCaptor.capture());
        assertEquals(userId, idCaptor.getValue());
        assertThat(actualResponseBody).isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(List.of(postDto)));
    }

    @Test
    void getProjectDraftsTest() throws Exception{
        postDto.setProjectId(1l);
        when(postService.getProjectDrafts(1L)).thenReturn(List.of(postDto));
        mvcResult = mockMvc.perform(get("/api/v1/posts/drafts/projects/{id}", projectId)
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        actualResponseBody = mvcResult.getResponse().getContentAsString();

        verify(postService, times(1)).getProjectDrafts(idCaptor.capture());
        assertEquals(projectId, idCaptor.getValue());
        assertThat(actualResponseBody).isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(List.of(postDto)));
    }

    @Test
    void getAllPostsByAuthorIdTest() throws Exception{
        postDto.setAuthorId(1L);
        when(postService.getAllPostsByAuthorId(1L)).thenReturn(List.of(postDto));
        mvcResult = mockMvc.perform(get("/api/v1/posts/author/{userId}/all", userId)
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        actualResponseBody = mvcResult.getResponse().getContentAsString();

        verify(postService, times(1)).getAllPostsByAuthorId(idCaptor.capture());
        assertEquals(userId, idCaptor.getValue());
        assertThat(actualResponseBody).isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(List.of(postDto)));
    }

    @Test
    void getAllPostsByProjectIdTest() throws Exception{
        postDto.setProjectId(1L);
        when(postService.getAllPostsByProjectId(1L)).thenReturn(List.of(postDto));
        mvcResult = mockMvc.perform(get("/api/v1/posts/project/{projectId}/all", projectId)
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        verify(postService, times(1)).getAllPostsByProjectId(idCaptor.capture());
        assertEquals(projectId, idCaptor.getValue());
    }

    @Test
    void getAllPostsByAuthorIdAndPublishedTest() throws Exception{
        postDto.setAuthorId(1L);
        postDto.setPublished(true);
        when(postService.getAllPostsByAuthorIdAndPublished(1L)).thenReturn(List.of(postDto));
        mvcResult = mockMvc.perform(get("/api/v1/posts/all/author/{userId}/published", userId)
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        verify(postService, times(1)).getAllPostsByAuthorIdAndPublished(idCaptor.capture());
        assertEquals(userId, idCaptor.getValue());
    }

    @Test
    void getAllPostsByProjectIdAndPublishedTest() throws Exception{
        postDto.setProjectId(1L);
        when(postService.getAllPostsByProjectIdAndPublished(1L)).thenReturn(List.of(postDto));
        mvcResult = mockMvc.perform(get("/api/v1/posts/all/project/{projectId}/published", projectId)
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        verify(postService, times(1)).getAllPostsByProjectIdAndPublished(idCaptor.capture());
        assertEquals(projectId, idCaptor.getValue());
    }
}