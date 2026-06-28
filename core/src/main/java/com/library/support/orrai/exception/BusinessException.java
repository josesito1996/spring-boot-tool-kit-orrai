package com.library.support.orrai.exception;

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

    public BusinessException(int statusCode, String errorCode, String message) {
        this(statusCode, errorCode, message, null);
    }

    public BusinessException(int statusCode, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
