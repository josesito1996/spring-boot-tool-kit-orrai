package com.library.support.orrai.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private final Map<String, Object> properties;

    public ApiError(String timestamp, int status, String errorCode, String message, String path,
                    List<ValidationError> errors) {
        this(timestamp, status, errorCode, message, path, errors, Collections.emptyMap());
    }

    private ApiError(String timestamp, int status, String errorCode, String message, String path,
                     List<ValidationError> errors, Map<String, Object> properties) {
        this.timestamp = timestamp;
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.path = path;
        this.errors = errors;
        this.properties = properties;
    }

    /**
     * Returns a copy of this error carrying the given extra properties, rendered as top-level JSON
     * members (the Boot 2 equivalent of {@code ProblemDetail#setProperty}). Keys are written verbatim.
     */
    public ApiError withProperties(Map<String, Object> extra) {
        Map<String, Object> merged = new LinkedHashMap<>(properties);
        merged.putAll(extra);
        return new ApiError(timestamp, status, errorCode, message, path, errors,
                Collections.unmodifiableMap(merged));
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

    /** Extra top-level members; empty unless set via {@link #withProperties(Map)}. */
    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return properties;
    }
}
