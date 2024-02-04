package faang.school.postservice.validator;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.exception.*;
import faang.school.postservice.repository.CommentRepository;
import faang.school.postservice.repository.PostRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentValidator {
    private final UserServiceClient userServiceClient;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public void validateNewComment(Long commentId, Long postId, Long currentUserId, Long authorId){
        validateIdIsNull(commentId);
        validatePostExists(postId);
        validateCommentAuthor(currentUserId, authorId);
        validateUserExist(authorId);
    }

    public void validateExistingComment(Long commentId, Long postId, Long currentUserId, Long authorId){
        validateIdIsNotNull(commentId);
        validatePostExists(postId);
        validateCommentAuthor(currentUserId, authorId);
        validateUserExist(authorId);
    }

    public void validateUserExist(Long userId) {
        try {
            userServiceClient.getUser(userId, userId);

            log.info("User validated successfully with ID: {}", userId);
        } catch (FeignException e) {
            throw new DataNotFoundException(String.format("User with id=%d doesn't exist", userId));
        }
    }

    public void validateIdIsNull(Long id){
        if (id != null){
            throw new InvalidIdException("ID must be null for publishing a comment");
        }
    }

    public void validateIdIsNotNull(Long id){
        if (id == null){
            throw new InvalidIdException("ID mustn't be null for editing a comment");
        }
    }

    public void validateCommentAuthor(Long currentUserId, Long authorId){
        if (currentUserId != authorId){
            throw new ActionNotPermittedException("Only author can perform this action");
        }
    }

    public void validatePostExists(Long postId){
        postRepository.findById(postId).orElseThrow(() -> new EntityNotFoundException(
                String.format("PostId=%d doesn't exist", postId)));
    }

    public void validateCommentExists(Long commentId){
        commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("CommentId=%d doesn't exist", commentId)));
    }
}