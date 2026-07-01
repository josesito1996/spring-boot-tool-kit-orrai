package com.library.support.orrai.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Raised by an {@link com.library.support.orrai.http.OrraiHttpClient} adapter when an outbound call
 * fails — either because the upstream returned a {@code 4xx}/{@code 5xx} status or because the
 * request could not be delivered (connection/timeout/transport error).
 *
 * <p>Extends {@link BusinessException} so, in a consumer that also uses this library's exception
 * handling, it is rendered as an RFC 7807 response automatically. From the consumer's perspective a
 * failed downstream dependency is a gateway problem, so it maps to {@code 502 BAD_GATEWAY} with the
 * stable error code {@code UPSTREAM_HTTP_ERROR}; the original upstream status and raw response body
 * are preserved on the exception for inspection and logging, never auto-exposed to the end client.
 */
public class OrraiHttpClientException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /** Stable, machine-readable code surfaced to clients for any upstream HTTP failure. */
    private static final String ERROR_CODE = "UPSTREAM_HTTP_ERROR";

    /** {@code 0} when the failure is transport-level (no HTTP response was received). */
    private static final int NO_UPSTREAM_STATUS = 0;

    private final int upstreamStatusCode;

    private final String responseBody;

    private final Map<String, List<String>> responseHeaders;

    private final String requestPath;

    /**
     * Builds an exception for an upstream HTTP error response ({@code 4xx}/{@code 5xx}) without
     * captured headers. Kept for backward compatibility; prefer the overload that also takes the
     * response headers.
     *
     * @param upstreamStatusCode the status returned by the upstream service (e.g. {@code 404})
     * @param requestPath        the path that was called
     * @param responseBody       the raw upstream response body, for diagnostics (may be {@code null})
     * @param message            client-safe summary of the failure
     */
    public OrraiHttpClientException(int upstreamStatusCode, String requestPath, String responseBody, String message) {
        this(upstreamStatusCode, requestPath, responseBody, null, message);
    }

    /**
     * Builds an exception for an upstream HTTP error response ({@code 4xx}/{@code 5xx}), capturing a
     * snapshot of the response (status, headers and body) so the consumer can process the error
     * response after the underlying transport stream has been closed.
     *
     * @param upstreamStatusCode the status returned by the upstream service (e.g. {@code 404})
     * @param requestPath        the path that was called
     * @param responseBody       the raw upstream response body, for diagnostics (may be {@code null})
     * @param responseHeaders    the upstream response headers (may be {@code null}); defensively copied
     * @param message            client-safe summary of the failure
     */
    public OrraiHttpClientException(int upstreamStatusCode, String requestPath, String responseBody,
            Map<String, List<String>> responseHeaders, String message) {
        super(ErrorCode.BAD_GATEWAY.getStatusCode(), ERROR_CODE, message);
        this.upstreamStatusCode = upstreamStatusCode;
        this.responseBody = responseBody;
        this.responseHeaders = copyHeaders(responseHeaders);
        this.requestPath = requestPath;
    }

    /**
     * Builds an exception for a transport-level failure (no HTTP response received). The technical
     * {@code cause} is logged by the handlers but never exposed to the client.
     *
     * @param requestPath the path that was called
     * @param message     client-safe summary of the failure
     * @param cause       the underlying transport exception
     */
    public OrraiHttpClientException(String requestPath, String message, Throwable cause) {
        super(ErrorCode.BAD_GATEWAY.getStatusCode(), ERROR_CODE, message, cause);
        this.upstreamStatusCode = NO_UPSTREAM_STATUS;
        this.responseBody = null;
        this.responseHeaders = Map.of();
        this.requestPath = requestPath;
    }

    /** Defensive, deep-immutable copy of the header map ({@code null}/entries tolerated). */
    private static Map<String, List<String>> copyHeaders(Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> copy = new LinkedHashMap<>();
        headers.forEach((name, values) -> copy.put(name, (values == null) ? List.of() : List.copyOf(values)));
        return Collections.unmodifiableMap(copy);
    }

    /** The HTTP status returned by the upstream service, or {@code 0} for a transport-level failure. */
    public int getUpstreamStatusCode() {
        return upstreamStatusCode;
    }

    /** The raw upstream response body, or {@code null} when unavailable. */
    public String getResponseBody() {
        return responseBody;
    }

    /**
     * The upstream response headers captured at error time, as an unmodifiable map (never
     * {@code null}; empty for transport-level failures). Lets the consumer inspect the error
     * response — content type, correlation ids, {@code Retry-After}, etc.
     */
    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

    /** The path that was called when the failure occurred. */
    public String getRequestPath() {
        return requestPath;
    }
}