package com.library.support.orrai.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BusinessExceptionTest {

    @Test
    @DisplayName("subclasses expose the correct status code and error code")
    void subclasses_mapStatusAndErrorCode() {
        assertStatusAndCode(new ResourceNotFoundException("nope"), 404, "RESOURCE_NOT_FOUND");
        assertStatusAndCode(new UnauthorizedException("nope"), 401, "UNAUTHORIZED");
        assertStatusAndCode(new ForbiddenException("nope"), 403, "FORBIDDEN");
        assertStatusAndCode(new InternalErrorException("nope"), 500, "INTERNAL_ERROR");
    }

    @Test
    @DisplayName("cause constructor preserves the original throwable")
    void causeConstructor_preservesCause() {
        Throwable cause = new IllegalStateException("root");
        ResourceNotFoundException ex = new ResourceNotFoundException("not found", cause);

        assertSame(cause, ex.getCause());
        assertEquals("not found", ex.getMessage());
    }

    @Test
    @DisplayName("no-cause constructor leaves cause null")
    void noCauseConstructor_hasNullCause() {
        assertNull(new ForbiddenException("denied").getCause());
    }

    @Test
    @DisplayName("base BusinessException accepts an arbitrary status code")
    void baseException_acceptsArbitraryStatus() {
        BusinessException ex = new BusinessException(418, "IM_A_TEAPOT", "short and stout");
        assertEquals(418, ex.getStatusCode());
        assertEquals("IM_A_TEAPOT", ex.getErrorCode());
    }

    @Test
    @DisplayName("ErrorCode constructor uses the enum's status, code and default message")
    void errorCodeConstructor_usesEnumDefaults() {
        BusinessException ex = new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);

        assertEquals(503, ex.getStatusCode());
        assertEquals("SERVICE_UNAVAILABLE", ex.getErrorCode());
        assertEquals(ErrorCode.SERVICE_UNAVAILABLE.getDefaultMessage(), ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    @DisplayName("ErrorCode constructor overrides the default message while keeping status and code")
    void errorCodeConstructor_overridesMessage() {
        BusinessException ex = new BusinessException(ErrorCode.BAD_GATEWAY, "odds provider timed out");

        assertEquals(502, ex.getStatusCode());
        assertEquals("BAD_GATEWAY", ex.getErrorCode());
        assertEquals("odds provider timed out", ex.getMessage());
    }

    @Test
    @DisplayName("ErrorCode constructor with cause keeps the original throwable")
    void errorCodeConstructor_keepsCause() {
        Throwable cause = new IllegalStateException("socket closed");
        BusinessException ex = new BusinessException(ErrorCode.INTERNAL_ERROR, "boom", cause);

        assertEquals(500, ex.getStatusCode());
        assertEquals("INTERNAL_ERROR", ex.getErrorCode());
        assertEquals("boom", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    private void assertStatusAndCode(BusinessException ex, int expectedStatus, String expectedCode) {
        assertEquals(expectedStatus, ex.getStatusCode());
        assertEquals(expectedCode, ex.getErrorCode());
    }
}
