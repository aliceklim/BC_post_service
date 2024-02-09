package faang.school.postservice.controller;

import faang.school.postservice.exception.DataValidationException;
import faang.school.postservice.exception.EntityNotFoundException;
import faang.school.postservice.exception.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertEquals;

@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerTest {
    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    void handleDataValidationExceptionTest() {
        DataValidationException exception = new DataValidationException("Validation error message");

        ErrorResponse errorResponse = globalExceptionHandler.handleDataValidationException(exception);

        assertEquals("Validation error message", errorResponse.message());
    }

    @Test
    void handleEntityNotFoundException(){
        EntityNotFoundException exception = new EntityNotFoundException("Entity not found");

        ErrorResponse errorResponse = globalExceptionHandler.handleEntityNotFoundException(exception);

        assertEquals("Entity not found", errorResponse.message());
    }

    @Test
    void handleRuntimeException(){
        EntityNotFoundException exception = new EntityNotFoundException("Runtime error");

        ErrorResponse errorResponse = globalExceptionHandler.handleRuntimeException(exception);

        assertEquals("Runtime error", errorResponse.message());
    }
}