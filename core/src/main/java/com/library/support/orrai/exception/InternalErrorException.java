package com.library.support.orrai.exception;

import java.net.HttpURLConnection;

/**
 * Thrown for explicit, known internal failures. Maps to HTTP 500 (INTERNAL SERVER ERROR).
 *
 * <p><b>Note:</b> like every {@link BusinessException}, the supplied {@code message} is
 * returned to the HTTP client. Pass only client-safe text; attach low-level technical
 * context via the {@code cause} constructor (it is logged, not exposed).
 */
public class InternalErrorException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public InternalErrorException(String message) {
        super(HttpURLConnection.HTTP_INTERNAL_ERROR, "INTERNAL_ERROR", message);
    }

    public InternalErrorException(String message, Throwable cause) {
        super(HttpURLConnection.HTTP_INTERNAL_ERROR, "INTERNAL_ERROR", message, cause);
    }
}
