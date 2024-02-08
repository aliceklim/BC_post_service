package faang.school.postservice.validator;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.dto.user.UserDto;
import faang.school.postservice.exception.ActionNotPermittedException;
import faang.school.postservice.exception.DataNotFoundException;
import faang.school.postservice.exception.EntityNotFoundException;
import faang.school.postservice.exception.InvalidIdException;
import faang.school.postservice.model.Comment;
import faang.school.postservice.model.Post;
import faang.school.postservice.repository.CommentRepository;
import faang.school.postservice.repository.PostRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CommentValidatorTest {
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private PostRepository postRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private UserContext userContext;
    @InjectMocks
    private CommentValidator commentValidator;
    private Long userId;
    private Long postId;
    private Long commentId;
    private Long currentUser;
    private UserDto userDto;
    private String dataNotFoundExceptionExceptionText = "User with id=%d doesn't exist";
    private String nullInvalidIdExceptionText = "ID must be null for publishing a comment";
    private String notNullInvalidIdExceptionText = "ID mustn't be null for editing a comment";
    private String actionNotPermittedExceptionText = "Only author can perform this action";
    private String postNotFoundExceptionText = "PostId=%d doesn't exist";
    private String commentNotFoundExceptionText = "CommentId=%d doesn't exist";


    @BeforeEach
    void setUp(){
        userId = 1l;
        commentId = 5L;
        postId = 3L;
        currentUser = 2L;
        userDto = UserDto.builder().id(userId).build();
    }

    @Test
    void validateUserExistsTest(){
        Mockito.when(userServiceClient.getUser(userId, userId)).thenReturn(userDto);
        Mockito.when(userContext.getUserId()).thenReturn(userId);

        commentValidator.validateUserExist(userId);
    }

    @Test
    void validateUserExistsExceptionTest(){
        doThrow(FeignException.class).when(userServiceClient).getUser(anyLong(), anyLong());

        DataNotFoundException exception = assertThrows(DataNotFoundException.class,
                () -> commentValidator.validateUserExist(userId));

        assertEquals(String.format(dataNotFoundExceptionExceptionText, userId), exception.getMessage());
    }
    @Test
    void validateIdIsNullTest(){
        assertDoesNotThrow(() -> commentValidator.validateIdIsNull(null));
    }
    @Test
    void validateIdIsNullExceptionTest(){
        InvalidIdException exception = assertThrows(InvalidIdException.class,
                () -> commentValidator.validateIdIsNull(userId));

        assertEquals(nullInvalidIdExceptionText, exception.getMessage());
    }
    @Test
    void validateIdIsNotNullTest(){
        assertDoesNotThrow(() -> commentValidator.validateIdIsNotNull(userId));
    }

    @Test
    void validateIdIsNotNullExceptionTest(){
        InvalidIdException exception = assertThrows(InvalidIdException.class,
                () -> commentValidator.validateIdIsNotNull(null));

        assertEquals(notNullInvalidIdExceptionText, exception.getMessage());

    }
    @Test
    void validateCommentAuthorTest(){
        assertDoesNotThrow(() -> commentValidator.validateCommentAuthor(currentUser, currentUser));
    }

    @Test
    void validateCommentAuthorExceptionTest(){
        ActionNotPermittedException exception = assertThrows(ActionNotPermittedException.class,
                () -> commentValidator.validateCommentAuthor(currentUser, userId));

        assertEquals(actionNotPermittedExceptionText, exception.getMessage());
    }
    @Test
    void validatePostExistsTest(){
        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(Post.builder().id(1L).build()));
        assertDoesNotThrow(() -> commentValidator.validatePostExists(postId));
    }
    @Test
    void validatePostExistsExceptionTest(){
        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(null));

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> commentValidator.validatePostExists(postId));
        assertEquals(String.format(postNotFoundExceptionText, postId), exception.getMessage());
    }
    @Test
    void validateCommentExistsTest(){
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(Comment.builder().id(commentId).build()));

        assertDoesNotThrow(() -> commentValidator.validateCommentExists(commentId));
    }

    @Test
    void validateCommentExistsExceptionTest(){
        when(commentRepository.findById(commentId)).thenReturn(Optional.ofNullable(null));

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> commentValidator.validateCommentExists(commentId));
        assertEquals(String.format(commentNotFoundExceptionText, commentId), exception.getMessage());
    }
}