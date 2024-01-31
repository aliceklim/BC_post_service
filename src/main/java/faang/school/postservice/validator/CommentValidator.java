package faang.school.postservice.validator;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.exception.DataNotFoundException;
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

    public void validateUserExistence(long userId) {
        try {
            userServiceClient.getUser(userId, userId);

            log.info("User validated successfully with ID: {}", userId);
        } catch (FeignException e) {
            throw new DataNotFoundException(String.format("User with id=%d doesn't exist", userId));
        }
    }
}
