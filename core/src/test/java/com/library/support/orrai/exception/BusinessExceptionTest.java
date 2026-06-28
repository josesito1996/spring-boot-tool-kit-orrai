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

    private void assertStatusAndCode(BusinessException ex, int expectedStatus, String expectedCode) {
        assertEquals(expectedStatus, ex.getStatusCode());
        assertEquals(expectedCode, ex.getErrorCode());
    }
}
