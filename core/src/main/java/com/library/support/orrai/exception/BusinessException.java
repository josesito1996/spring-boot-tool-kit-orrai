package com.library.support.orrai.exception;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Base type for all business (domain) exceptions handled by the library.
 *
 * <p>Carries the HTTP status as a plain {@code int} (not {@code org.springframework.http.HttpStatus})
 * and a stable, machine-readable error code, so this module stays free of any Spring dependency
 * and can be shared by both the Spring Boot 2 (javax) and Spring Boot 3 (jakarta) adapters.
 *
 * <p><b>Note:</b> the {@code message} is returned to the HTTP client in the error response.
 * Pass only client-safe text; attach low-level technical context via the {@code cause}
 * constructor (it is logged by the handlers, not exposed).
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    private final String errorCode;

    // Union of both adapters' base fields: RFC 7807 + ProblemDetails extensions (Boot 3) and
    // ApiError's "message" (Boot 2). A property must never overwrite the error contract.
    private static final Set<String> RESERVED_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "type", "title", "status", "detail", "instance", "timestamp", "errorCode", "path", "errors", "message")));

    // Transient: values are arbitrary objects, so serializing the exception must not fail on them.
    // Lazily created, hence null both when unused and after deserialization.
    private transient Map<String, Object> properties;

    public BusinessException(int statusCode, String errorCode, String message) {
        this(statusCode, errorCode, message, null);
    }

    public BusinessException(int statusCode, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    /** Builds an exception from an {@link ErrorCode}, using its default client-safe message. */
    public BusinessException(ErrorCode errorCode) {
        this(errorCode.getStatusCode(), errorCode.getCode(), errorCode.getDefaultMessage(), null);
    }

    /** Builds an exception from an {@link ErrorCode}, overriding its default message. */
    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode.getStatusCode(), errorCode.getCode(), message, null);
    }

    /** Builds an exception from an {@link ErrorCode} with a custom message and a logged cause. */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode.getStatusCode(), errorCode.getCode(), message, cause);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Attaches an extra property that the handlers add to the error response body, next to the
     * standard fields. Intended to be chained where the exception is thrown:
     * <pre>{@code
     * throw new BusinessException(409, "DOCUMENTO_DUPLICADO", "...").withProperty("paciente_id", 42);
     * }</pre>
     *
     * <p>The key is written to the JSON <b>exactly as given</b>: it does not go through Jackson's
     * {@code PropertyNamingStrategy}. Use {@code "paciente_id"} if the client expects snake_case.
     *
     * <p>Like the message, the value is returned to the HTTP client, so pass only client-safe data.
     *
     * @param key   non-blank key; must not be one of the reserved base fields ({@code type},
     *              {@code title}, {@code status}, {@code detail}, {@code instance},
     *              {@code timestamp}, {@code errorCode}, {@code path}, {@code errors},
     *              {@code message})
     * @param value property value; {@code null} leaves the key absent from the response
     * @return this exception, for chaining
     * @throws IllegalArgumentException if the key is null, blank or reserved
     */
    public BusinessException withProperty(String key, Object value) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Property key must not be null or blank");
        }
        if (RESERVED_KEYS.contains(key)) {
            throw new IllegalArgumentException("Property key '" + key + "' is reserved for the base error fields");
        }
        if (value == null) {
            return this;
        }
        if (properties == null) {
            properties = new LinkedHashMap<>();
        }
        properties.put(key, value);
        return this;
    }

    /** Extra response properties, in insertion order, as an unmodifiable view. Never {@code null}. */
    public Map<String, Object> getProperties() {
        return properties == null ? Collections.emptyMap() : Collections.unmodifiableMap(properties);
    }
}
