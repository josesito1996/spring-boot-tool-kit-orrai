package com.library.support.orrai.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

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

    @Test
    @DisplayName("without properties, getProperties() is empty")
    void properties_emptyByDefault() {
        assertTrue(new BusinessException(409, "CONFLICT", "dup").getProperties().isEmpty());
    }

    @Test
    @DisplayName("withProperty returns the same instance and keeps insertion order")
    void withProperty_fluentAndOrdered() {
        BusinessException ex = new BusinessException(409, "DOCUMENTO_DUPLICADO", "dup");

        BusinessException returned = ex.withProperty("paciente_id", 42L).withProperty("a", "x").withProperty("b", 1);

        assertSame(ex, returned);
        assertEquals(Arrays.asList("paciente_id", "a", "b"), new ArrayList<>(ex.getProperties().keySet()));
        assertEquals(42L, ex.getProperties().get("paciente_id"));
    }

    @Test
    @DisplayName("subclasses inherit withProperty")
    void withProperty_worksOnSubclasses() {
        BusinessException ex = new ResourceNotFoundException("nope").withProperty("id", 7);

        assertTrue(ex instanceof ResourceNotFoundException);
        assertEquals(7, ex.getProperties().get("id"));
    }

    @Test
    @DisplayName("a null value does not add the key")
    void withProperty_nullValueIsIgnored() {
        BusinessException ex = new BusinessException(409, "CONFLICT", "dup").withProperty("x", null);

        assertFalse(ex.getProperties().containsKey("x"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"type", "title", "status", "detail", "instance",
            "timestamp", "errorCode", "path", "errors", "message"})
    @DisplayName("reserved keys are rejected")
    void withProperty_reservedKeyRejected(String key) {
        BusinessException ex = new BusinessException(409, "CONFLICT", "dup");

        assertThrows(IllegalArgumentException.class, () -> ex.withProperty(key, "v"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("null or blank keys are rejected")
    void withProperty_blankKeyRejected(String key) {
        BusinessException ex = new BusinessException(409, "CONFLICT", "dup");

        assertThrows(IllegalArgumentException.class, () -> ex.withProperty(key, "v"));
    }

    @Test
    @DisplayName("getProperties() cannot be modified from outside")
    void getProperties_isUnmodifiable() {
        BusinessException ex = new BusinessException(409, "CONFLICT", "dup").withProperty("k", "v");
        Map<String, Object> props = ex.getProperties();

        assertThrows(UnsupportedOperationException.class, () -> props.put("other", 1));
        assertThrows(UnsupportedOperationException.class, () -> props.remove("k"));
        assertThrows(UnsupportedOperationException.class,
                () -> new BusinessException(409, "C", "d").getProperties().put("x", 1));
    }

    @Test
    @DisplayName("serialization round-trip succeeds even with non-serializable property values")
    void serialization_roundTripDoesNotFail() throws Exception {
        BusinessException ex = new BusinessException(409, "CONFLICT", "dup")
                .withProperty("not_serializable", new Object());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(ex);
        }
        BusinessException copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = (BusinessException) in.readObject();
        }

        assertEquals(409, copy.getStatusCode());
        assertEquals("CONFLICT", copy.getErrorCode());
        assertTrue(copy.getProperties().isEmpty());
        assertEquals("v", copy.withProperty("k", "v").getProperties().get("k"));
    }

    private void assertStatusAndCode(BusinessException ex, int expectedStatus, String expectedCode) {
        assertEquals(expectedStatus, ex.getStatusCode());
        assertEquals(expectedCode, ex.getErrorCode());
    }
}
