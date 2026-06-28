package com.library.support.orrai.handler;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;

/**
 * Internal helper that builds RFC 7807 {@link ProblemDetail} responses with the
 * library's standard extension properties ({@code timestamp}, {@code errorCode},
 * {@code path}). Shared by the exception handlers to avoid duplication.
 */
final class ProblemDetails {

    private static final String URI_PREFIX = "uri=";

    private ProblemDetails() {
    }

    static ProblemDetail create(int status, String detail, String errorCode, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
        // Serialize as an ISO-8601 string so output is independent of the consumer's Jackson config.
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("errorCode", errorCode);
        String path = resolvePath(request);
        if (path != null) {
            problem.setProperty("path", path);
        }
        return problem;
    }

    private static String resolvePath(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        if (request != null) {
            String description = request.getDescription(false);
            return description.startsWith(URI_PREFIX) ? description.substring(URI_PREFIX.length()) : description;
        }
        return null;
    }
}
