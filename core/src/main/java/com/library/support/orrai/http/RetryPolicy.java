package com.library.support.orrai.http;

import com.library.support.orrai.exception.OrraiHttpClientException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Immutable, transport-agnostic retry policy for outbound HTTP calls.
 *
 * <p>Lives in {@code core} (no Spring) so any {@link OrraiHttpClient} adapter — {@code RestClient},
 * {@code WebClient}, OpenFeign… — can be wrapped by {@link RetryingOrraiHttpClient} with identical
 * semantics. A call is retried when it fails as a transport error (no HTTP response received) or
 * when the upstream status is in {@link #retryableStatusCodes}.
 */
public final class RetryPolicy {

    private final int maxRetries;

    private final long retryDelayMillis;

    private final Set<Integer> retryableStatusCodes;

    private final boolean retryOnTransportError;

    /**
     * @param maxRetries            additional attempts after the first ({@code 0} disables retrying)
     * @param retryDelayMillis      fixed delay between attempts, in milliseconds
     * @param retryableStatusCodes  upstream HTTP statuses that should be retried
     * @param retryOnTransportError whether to retry connection/timeout failures (no response)
     */
    public RetryPolicy(int maxRetries, long retryDelayMillis, Set<Integer> retryableStatusCodes,
            boolean retryOnTransportError) {
        this.maxRetries = Math.max(0, maxRetries);
        this.retryDelayMillis = Math.max(0L, retryDelayMillis);
        this.retryableStatusCodes = retryableStatusCodes == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(retryableStatusCodes));
        this.retryOnTransportError = retryOnTransportError;
    }

    /** Additional attempts after the first one. */
    public int getMaxRetries() {
        return maxRetries;
    }

    /** Fixed delay between attempts, in milliseconds. */
    public long getRetryDelayMillis() {
        return retryDelayMillis;
    }

    /** Whether the given failure is eligible to be retried under this policy. */
    public boolean shouldRetry(OrraiHttpClientException ex) {
        if (ex.getUpstreamStatusCode() == 0) {
            // No HTTP response was received → transport-level failure.
            return retryOnTransportError;
        }
        return retryableStatusCodes.contains(ex.getUpstreamStatusCode());
    }
}