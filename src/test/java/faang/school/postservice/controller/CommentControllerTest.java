package faang.school.postservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.dto.comment.CommentDto;
import faang.school.postservice.service.CommentService;
import faang.school.postservice.validator.CommentValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.shaded.org.hamcrest.Matchers;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.servlet.function.RequestPredicates.contentType;

@ExtendWith(SpringExtension.class)

public class CommentControllerTest {
    @Mock
    private CommentService commentService;
    @Mock
    private CommentValidator commentValidator;
    @Mock
    private UserContext userContext;
    @InjectMocks
    private CommentController commentController;
    private CommentDto commentDto;
    private CommentDto newCommentDto;
    private Long commentId = 1L;
    private Long postId = 2L;
    private Long authorId = 1L;
    private Long userId= 1L;
    private MockMvc mockMvc;
    private MvcResult mvcResult;
    private String actualResponseBody;
    private ArgumentCaptor<CommentDto> commentDtoCaptor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp(){
        newCommentDto = CommentDto.builder().id(null).postId(postId).authorId(authorId).content("string").build();
        commentDto = CommentDto.builder().id(commentId).postId(postId).authorId(authorId).content("string").build();
        mockMvc = MockMvcBuilders.standaloneSetup(commentController).build();
        commentDtoCaptor = ArgumentCaptor.forClass(CommentDto.class);
        objectMapper = new ObjectMapper();
    }

    @Test
    void publishCommentTest() throws Exception {
        when(commentService.create(newCommentDto)).thenReturn(commentDto);
        doNothing().when(commentValidator).validateNewComment(commentDto.getId(), commentDto.getPostId(),
                userId, commentDto.getAuthorId());

        mvcResult = mockMvc.perform(post("/api/v1/comments/new")
                        .header("x-user-id", String.valueOf(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newCommentDto)))
                .andExpect(status().isOk())
                .andReturn();

        actualResponseBody = mvcResult.getResponse().getContentAsString();

        verify(userContext).setUserId(userId);
        verify(commentService, times(1)).create(commentDtoCaptor.capture());
        verify(commentValidator, times(1)).validateNewComment(newCommentDto.getId(),
                newCommentDto.getPostId(), userId, newCommentDto.getAuthorId());
        assertEquals(newCommentDto, commentDtoCaptor.getValue());
        assertThat(actualResponseBody).isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(commentDto));
    }

    @Test
    void publishCommentPostIdNull_returns400Test() throws Exception {
        commentDto.setPostId(null);
        mockMvc.perform(post("/api/v1/comments/new")
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publishCommentAuthorIdNull_returns400Test() throws Exception {
        commentDto.setAuthorId(null);
        mockMvc.perform(post("/api/v1/comments/new")
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publishCommentSmallContent_returns400Test() throws Exception {
        commentDto.setContent("");
        mockMvc.perform(post("/api/v1/comments/new")
                        .header("x-user-id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editComment() throws Exception{
        commentDto.setContent("updated");
        when(commentService.update(commentDto)).thenReturn(commentDto);
        mvcResult = mockMvc.perform(put("/api/v1/comments/edit")
                .header("x-user-id", String.valueOf(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isOk())
                .andReturn();

        actualResponseBody = mvcResult.getResponse().getContentAsString();

        verify(userContext).setUserId(userId);
        verify(commentService, times(1)).update(commentDtoCaptor.capture());
        verify(commentValidator, times(1)).validateExistingComment(commentDto.getId(),
                commentDto.getPostId(),userId, commentDto.getAuthorId());
        assertEquals(commentDto, commentDtoCaptor.getValue());
        assertThat(actualResponseBody).isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(commentDto));
    }

    @Test
    void deleteComment() throws Exception{
        mockMvc.perform(delete("/api/v1/comments/{commentId}", commentId)
                .header("x-user-id", String.valueOf(userId)))
                .andExpect(status().isOk());

        verify(userContext).setUserId(userId);
        verify(commentService, times(1)).delete(commentId);
    }

    @Test
    void getCommentsByPost() throws Exception{
        Pageable pageable = PageRequest.of(0, 20);

        when(commentService.getCommentsByPost(postId, pageable))
                .thenReturn(new PageImpl<>(List.of(commentDto)));

        mockMvc.perform(get("/api/v1/comments/{postId}", postId)
                        .header("x-user-id", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}