package com.library.support.orrai.exception;

import java.net.HttpURLConnection;

/**
 * Thrown when authentication is missing or invalid. Maps to HTTP 401 (UNAUTHORIZED).
 */
public class UnauthorizedException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(HttpURLConnection.HTTP_UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(HttpURLConnection.HTTP_UNAUTHORIZED, "UNAUTHORIZED", message, cause);
    }
}
