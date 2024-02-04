package faang.school.postservice.controller;

import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.dto.comment.CommentDto;
import faang.school.postservice.service.CommentService;
import faang.school.postservice.validator.CommentValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/comments")
@Tag(name = "CommentController", description = "Creates, updates. deletes gets comments for a post")
@Slf4j
public class CommentController {

    private final CommentService commentService;
    private final CommentValidator commentValidator;

    @PostMapping("/new")
    @Operation(
            summary = "Comment creation",
            description = "Creates a comment for a post"
    )
    @Parameter(description = "Receives a commentDto")
    public CommentDto createComment(@RequestHeader("x-user-id")Long currentUserId,
                                    @Valid @RequestBody CommentDto commentDto) {
        log.info("Endpoint <createComment>, uri='/comments/new' was called");
        commentValidator.validateNewComment(
                commentDto.getId(), commentDto.getPostId(), currentUserId, commentDto.getAuthorId());

        return commentService.create(commentDto);
    }

    @PutMapping("/edit")
    @Operation(
            summary = "Updates",
            description = "Updates an existing comment"
    )
    @Parameter(description = "Receives a commentDto to update existing comment")
    public CommentDto updateComment(@RequestHeader("x-user-id")Long currentUserId,
                                    @Valid @RequestBody CommentDto commentDto) {
        log.info("Endpoint <updateComment>, uri='/comments/edit' was called");
        commentValidator.validateExistingComment(
                commentDto.getId(), commentDto.getPostId(), currentUserId, commentDto.getAuthorId());

        return commentService.update(commentDto);
    }

    @DeleteMapping("/{commentId}")
    @Operation(
            summary = "Deletion",
            description = "Deletes a comment by id"
    )
    @Parameter(description = "Receives a commentId to be deleted")
    public void deleteComment(@RequestHeader("x-user-id")Long currentUserId,
                              @PathVariable Long commentId) {
        log.info("Endpoint <deleteComment>, uri='/comments/{}' was called", commentId);

        commentService.delete(currentUserId, commentId);
    }

    @GetMapping("/{postId}")
    @Operation(
            summary = "Gets a list of comments for postId")
    @Parameter(description = "Receives a postId to search for its comments")
    public List<CommentDto> getCommentsForPost(@PathVariable Long postId) {
        log.info("Endpoint <getCommentsForPost>, uri='/comments/{}' was called", postId);

        return commentService.getCommentsForPost(postId);
    }
}