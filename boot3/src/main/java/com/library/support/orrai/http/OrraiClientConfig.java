package com.library.support.orrai.http;

import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Set;

/**
 * Per-client HTTP configuration, bound from {@code properties}. One instance backs the default
 * client ({@code orrai.http.client.*}) and one backs each named client
 * ({@code orrai.http.clients.<name>.*}), so every downstream dependency gets its own base URL,
 * timeouts and retry behavior.
 *
 * <p>Durations accept Spring's relaxed syntax (e.g. {@code 500ms}, {@code 2s}, {@code 1m}).
 *
 * @param baseUrl               base URL every request path is resolved against (required)
 * @param connectTimeout        max time to establish the TCP connection
 * @param readTimeout           max time waiting for the response (socket read) — the blocking-client
 *                              analogue of a "read/response timeout"
 * @param maxRetries            additional attempts after the first ({@code 0} disables retrying)
 * @param retryDelay            fixed delay between retry attempts
 * @param retryableStatusCodes  upstream HTTP statuses that trigger a retry
 * @param retryOnTransportError whether connection/timeout failures (no response) are retried
 */
public record OrraiClientConfig(
        String baseUrl,
        @DefaultValue("1s") Duration connectTimeout,
        @DefaultValue("5s") Duration readTimeout,
        @DefaultValue("0") int maxRetries,
        @DefaultValue("500ms") Duration retryDelay,
        @DefaultValue({"408", "429", "500", "502", "503", "504"}) Set<Integer> retryableStatusCodes,
        @DefaultValue("true") boolean retryOnTransportError) {
}