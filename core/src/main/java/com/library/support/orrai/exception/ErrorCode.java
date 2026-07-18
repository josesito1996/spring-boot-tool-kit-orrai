package com.library.support.orrai.exception;

import java.net.HttpURLConnection;

/**
 * Catalog of the most common HTTP error codes handled by the library, each bundling its numeric
 * HTTP status, a stable machine-readable code, and a client-safe default message.
 *
 * <p>Kept framework-agnostic (status as a plain {@code int} from {@link HttpURLConnection}, no
 * Spring types) so it can be shared by both the Spring Boot 2 (javax) and Spring Boot 3 (jakarta)
 * adapters. Feed a constant into {@link BusinessException} to raise an error without repeating the
 * status/code/message triple at every call site.
 */
public enum ErrorCode {

    /** HTTP 400 — the request is syntactically invalid or fails validation. */
    BAD_REQUEST(HttpURLConnection.HTTP_BAD_REQUEST, "BAD_REQUEST", "The request is invalid or malformed."),

    /** HTTP 401 — authentication is missing or invalid. */
    UNAUTHORIZED(HttpURLConnection.HTTP_UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required or has failed."),

    /** HTTP 403 — the caller is authenticated but lacks permission. */
    FORBIDDEN(HttpURLConnection.HTTP_FORBIDDEN, "FORBIDDEN", "You do not have permission to access this resource."),

    /** HTTP 404 — the requested resource does not exist. */
    NOT_FOUND(HttpURLConnection.HTTP_NOT_FOUND, "RESOURCE_NOT_FOUND", "The requested resource was not found."),

    /** HTTP 422 — the request is well-formed but fails semantic/business validation. */
    UNPROCESSABLE_ENTITY(422, "UNPROCESSABLE_ENTITY", "The request could not be processed due to semantic errors."),

    /** HTTP 429 — the client has sent too many requests in a given amount of time. */
    TOO_MANY_REQUESTS(429, "TOO_MANY_REQUESTS", "Too many requests. Please try again later."),

    /** HTTP 500 — an unexpected internal failure. */
    INTERNAL_ERROR(HttpURLConnection.HTTP_INTERNAL_ERROR, "INTERNAL_ERROR", "An unexpected internal error occurred."),

    /** HTTP 502 — an upstream/downstream service returned an invalid response. */
    BAD_GATEWAY(HttpURLConnection.HTTP_BAD_GATEWAY, "BAD_GATEWAY", "The upstream service returned an invalid response."),

    /** HTTP 503 — the service is temporarily unable to handle the request. */
    SERVICE_UNAVAILABLE(HttpURLConnection.HTTP_UNAVAILABLE, "SERVICE_UNAVAILABLE", "The service is temporarily unavailable.");

    private final int statusCode;

    private final String code;

    private final String defaultMessage;

    ErrorCode(int statusCode, String code, String defaultMessage) {
        this.statusCode = statusCode;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    /** The numeric HTTP status (e.g. {@code 404}). */
    public int getStatusCode() {
        return statusCode;
    }

    /** Stable, machine-readable error code surfaced to clients (e.g. {@code RESOURCE_NOT_FOUND}). */
    public String getCode() {
        return code;
    }

    /** Client-safe default message, used when no explicit message is supplied. */
    public String getDefaultMessage() {
        return defaultMessage;
    }
}