package com.library.support.orrai.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class ErrorCodeTest {

    @ParameterizedTest
    @CsvSource({
        "BAD_REQUEST, 400, BAD_REQUEST",
        "UNAUTHORIZED, 401, UNAUTHORIZED",
        "FORBIDDEN, 403, FORBIDDEN",
        "NOT_FOUND, 404, RESOURCE_NOT_FOUND",
        "INTERNAL_ERROR, 500, INTERNAL_ERROR",
        "BAD_GATEWAY, 502, BAD_GATEWAY",
        "SERVICE_UNAVAILABLE, 503, SERVICE_UNAVAILABLE"
    })
    @DisplayName("each constant maps to its HTTP status and stable code")
    void constant_mapsStatusAndCode(ErrorCode errorCode, int expectedStatus, String expectedCode) {
        assertEquals(expectedStatus, errorCode.getStatusCode());
        assertEquals(expectedCode, errorCode.getCode());
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("every constant carries a non-blank default message")
    void everyConstant_hasNonBlankDefaultMessage(ErrorCode errorCode) {
        assertFalse(errorCode.getDefaultMessage() == null || errorCode.getDefaultMessage().isBlank());
    }
}