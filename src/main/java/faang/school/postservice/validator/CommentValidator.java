package faang.school.postservice.validator;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.exception.ActionNotPermittedException;
import faang.school.postservice.exception.DataNotFoundException;
import faang.school.postservice.exception.InvalidIdException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentValidator {
    private final UserServiceClient userServiceClient;

    public void validateUserExistence(Long userId) {
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

    public void validateCommentAuthor(Long currentUserId, Long authorId){
        if (currentUserId != authorId){
            throw new ActionNotPermittedException("Only author can perform this action");
        }
    }
}
