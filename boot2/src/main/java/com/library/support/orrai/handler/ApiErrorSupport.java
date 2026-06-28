package com.library.support.orrai.handler;

import com.library.support.orrai.model.ApiError;
import com.library.support.orrai.model.ValidationError;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.List;

/**
 * Internal helper that builds {@link ApiError} responses with the adapter's standard fields.
 * Shared by the exception handlers to avoid duplication.
 */
final class ApiErrorSupport {

    private static final String URI_PREFIX = "uri=";

    private ApiErrorSupport() {
    }

    static ApiError create(int status, String message, String errorCode, WebRequest request) {
        return create(status, message, errorCode, request, null);
    }

    static ApiError create(int status, String message, String errorCode, WebRequest request,
                           List<ValidationError> errors) {
        // ISO-8601 string so output is independent of the consumer's Jackson config.
        return new ApiError(Instant.now().toString(), status, errorCode, message, resolvePath(request), errors);
    }

    private static String resolvePath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            return ((ServletWebRequest) request).getRequest().getRequestURI();
        }
        if (request != null) {
            String description = request.getDescription(false);
            return description.startsWith(URI_PREFIX) ? description.substring(URI_PREFIX.length()) : description;
        }
        return null;
    }
}
