package com.library.support.orrai.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Error response body for the Spring Boot 2 (javax) adapter. Spring Framework 5 has no
 * {@code ProblemDetail}, so this DTO plays the equivalent role, mirroring the same fields the
 * Boot 3 adapter exposes via RFC 7807 ({@code timestamp}, {@code status}, {@code errorCode},
 * {@code message}, {@code path}, {@code errors}).
 *
 * <p>Plain immutable class (Java 11). {@code null} properties are omitted from the JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiError {

    private final String timestamp;

    private final int status;

    private final String errorCode;

    private final String message;

    private final String path;

    private final List<ValidationError> errors;

    public ApiError(String timestamp, int status, String errorCode, String message, String path,
                    List<ValidationError> errors) {
        this.timestamp = timestamp;
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.path = path;
        this.errors = errors;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }
}
