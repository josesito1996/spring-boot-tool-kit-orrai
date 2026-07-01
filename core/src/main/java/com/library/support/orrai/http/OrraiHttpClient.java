package com.library.support.orrai.http;

import java.util.Map;

/**
 * Framework-agnostic <b>port</b> for outbound HTTP consumption (Ports &amp; Adapters).
 *
 * <p>This is a pure Java contract with no Spring (or any HTTP library) dependency, so it lives in
 * the {@code core} module and can be shared by every adapter. Consumers depend only on this
 * interface; the concrete transport ({@code RestClient}, {@code WebClient}, OpenFeign…) is wired by
 * an adapter behind an auto-configuration and can be swapped without touching consumer code.
 *
 * <p>Implementations are expected to centralize HTTP error handling: any {@code 4xx}/{@code 5xx}
 * response — or a transport failure — must surface as a
 * {@link com.library.support.orrai.exception.OrraiHttpClientException} rather than a
 * transport-specific exception, keeping the consumer fully decoupled from the underlying client.
 */
public interface OrraiHttpClient {

    /**
     * Executes an HTTP request and deserializes the response body into {@code responseType}.
     *
     * @param path         path appended to the adapter's configured base URL (e.g. {@code /users/42})
     * @param method       HTTP method name, case-insensitive (e.g. {@code GET}, {@code POST})
     * @param headers      request headers to add; may be {@code null} or empty
     * @param queryParams  query parameters to append; may be {@code null} or empty
     * @param body         request body to serialize; {@code null} for methods without a body
     * @param responseType type the response body is deserialized into
     * @param <T>          request body type
     * @param <R>          response body type
     * @return the deserialized response body
     * @throws com.library.support.orrai.exception.OrraiHttpClientException on any HTTP error
     *         response ({@code 4xx}/{@code 5xx}) or transport failure
     */
    <T, R> R execute(
            String path,
            String method,
            Map<String, String> headers,
            Map<String, String> queryParams,
            T body,
            Class<R> responseType);

    /**
     * Same as {@link #execute(String, String, Map, Map, Object, Class)} but for <b>generic</b>
     * response types the erasure of {@code Class<R>} cannot express — typically collections such as
     * {@code List<UserDto>}. Pass the target type with an {@link OrraiTypeRef}:
     *
     * <pre>{@code
     * List<UserDto> users = client.execute(
     *         "/users", "GET", null, null, null, new OrraiTypeRef<List<UserDto>>() {});
     * }</pre>
     *
     * @param responseType super type token describing the (possibly generic) response type
     * @throws com.library.support.orrai.exception.OrraiHttpClientException on any HTTP error
     *         response ({@code 4xx}/{@code 5xx}) or transport failure
     */
    <T, R> R execute(
            String path,
            String method,
            Map<String, String> headers,
            Map<String, String> queryParams,
            T body,
            OrraiTypeRef<R> responseType);
}