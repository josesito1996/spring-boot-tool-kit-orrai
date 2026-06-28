package com.library.support.orrai.exception;

import java.net.HttpURLConnection;

/**
 * Thrown when a requested resource does not exist. Maps to HTTP 404 (NOT FOUND).
 */
public class ResourceNotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(HttpURLConnection.HTTP_NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(HttpURLConnection.HTTP_NOT_FOUND, "RESOURCE_NOT_FOUND", message, cause);
    }
}
