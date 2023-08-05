package faang.school.postservice.controller.handler;

import faang.school.postservice.exception.AlreadyDeletedException;
import faang.school.postservice.exception.AlreadyPostedException;
import faang.school.postservice.exception.EmptyContentInPostException;
import faang.school.postservice.exception.NoPublishedPostException;
import faang.school.postservice.exception.SamePostAuthorException;
import faang.school.postservice.exception.UpdatePostException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AlreadyDeletedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorResponse handleAlreadyDeletedException(AlreadyDeletedException e) {
        log.error("Already deleted exception", e);
        return new ErrorResponse(e.getMessage(), e, HttpStatus.METHOD_NOT_ALLOWED, LocalDateTime.now());
    }

    @ExceptionHandler(AlreadyPostedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorResponse handleAlreadyPostedException(AlreadyPostedException e) {
        log.error("Already posted exception", e);
        return new ErrorResponse(e.getMessage(), e, HttpStatus.METHOD_NOT_ALLOWED, LocalDateTime.now());
    }

    @ExceptionHandler(EmptyContentInPostException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleEmptyContentInPostException(EmptyContentInPostException e) {
        log.error("Empty content in post exception", e);
        return new ErrorResponse(e.getMessage(), e, HttpStatus.BAD_REQUEST, LocalDateTime.now());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleEntityNotFoundException(EntityNotFoundException e) {
        log.error("Entity not found exception", e);
        return new ErrorResponse(e.getMessage(), e, HttpStatus.NOT_FOUND, LocalDateTime.now());
    }

    @ExceptionHandler(NoPublishedPostException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorResponse handleNoPublishedPostException(NoPublishedPostException e) {
        log.error("Post isn't published exception", e);
        return new ErrorResponse(e.getMessage(), e, HttpStatus.METHOD_NOT_ALLOWED, LocalDateTime.now());
    }

    @ExceptionHandler(SamePostAuthorException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleSamePostAuthorException(SamePostAuthorException e) {
        log.error("Same author of the post exception", e);
        return new ErrorResponse(e.getMessage(), e, HttpStatus.BAD_REQUEST, LocalDateTime.now());
    }

    @ExceptionHandler(UpdatePostException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUpdatePostException(UpdatePostException e) {
        log.info("Update exception", e);
        return new ErrorResponse(e.getMessage(), e, HttpStatus.BAD_REQUEST, LocalDateTime.now());
    }
}