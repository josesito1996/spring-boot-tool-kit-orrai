package com.library.support.orrai.handler;

import com.library.support.orrai.exception.InternalErrorException;
import com.library.support.orrai.exception.ForbiddenException;
import com.library.support.orrai.exception.NotFoundException;
import com.library.support.orrai.exception.UnauthorizedException;
import com.library.support.orrai.model.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles InternalErrorException.
     *
     * @param ex the InternalErrorException that was thrown.
     * @return an error response with HTTP status 500 (INTERNAL_SERVER_ERROR) and the exception message.
     */
    @ExceptionHandler(InternalErrorException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleInternalErrorException(InternalErrorException ex) {
        log.error("Calling the handleInternalErrorException method {}", ex.getMessage());
        return ErrorResponse.builder()
                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .message(ex.getMessage())
                .build();
    }

    /**
     * Handles NotFoundException.
     *
     * @param ex the NotFoundException that was thrown.
     * @return an error response with HTTP status 404 (NOT FOUND) and the exception message.
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFoundException(NotFoundException ex) {
        log.error("Calling the handleNotFoundException method {}", ex.getMessage());
        return ErrorResponse.builder()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .message(ex.getMessage())
                .build();
    }

    /**
     * Handles UnauthorizedException.
     *
     * @param ex the UnauthorizedException that was thrown.
     * @return an error response with HTTP status 401 (UNAUTHORIZED) and the exception message.
     */
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleUnauthorizedException(UnauthorizedException ex) {
        log.error("Calling the handleUnauthorizedException method {}", ex.getMessage());
        return ErrorResponse.builder()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .message(ex.getMessage())
                .build();
    }

    /**
     * Handles ForbiddenException.
     *
     * @param ex the ForbiddenException that was thrown.
     * @return an error response with HTTP status 403 (FORBIDDEN) and the exception message.
     */
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleForbiddenException(ForbiddenException ex) {
        log.error("Calling the handleForbiddenException method {}", ex.getMessage());
        return ErrorResponse.builder()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .message(ex.getMessage())
                .build();
    }
}
