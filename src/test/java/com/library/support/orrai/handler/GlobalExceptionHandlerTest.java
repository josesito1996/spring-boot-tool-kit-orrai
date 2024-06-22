package com.library.support.orrai.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.library.support.orrai.exception.ForbiddenException;
import com.library.support.orrai.exception.InternalErrorException;
import com.library.support.orrai.exception.NotFoundException;
import com.library.support.orrai.exception.UnauthorizedException;
import com.library.support.orrai.model.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    public void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("test handle InternalErrorException")
    public void testHandleInternalErrorException() {
        InternalErrorException exception = new InternalErrorException("Internal error occurred");

        ErrorResponse response = exceptionHandler.handleInternalErrorException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode());
        assertEquals("Internal error occurred", response.getMessage());
    }

    @Test
    @DisplayName("test handle NotFoundException")
    public void testHandleNotFoundException() {
        NotFoundException exception = new NotFoundException("Resource not Found");

        ErrorResponse response = exceptionHandler.handleNotFoundException(exception);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode());
        assertEquals("Resource not Found", response.getMessage());
    }

    @Test
    @DisplayName("test handle UnauthorizedException")
    public void testHandleUnauthorizedException() {
        UnauthorizedException exception = new UnauthorizedException("without authorization");

        ErrorResponse response = exceptionHandler.handleUnauthorizedException(exception);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode());
        assertEquals("without authorization", response.getMessage());
    }

    @Test
    @DisplayName("test handle ForbiddenException")
    public void testHandleForbiddenException() {
        ForbiddenException exception = new ForbiddenException("insufficient permissions");

        ErrorResponse response = exceptionHandler.handleForbiddenException(exception);

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode());
        assertEquals("insufficient permissions", response.getMessage());
    }

}
