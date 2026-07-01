package com.library.support.orrai.http;

import com.library.support.orrai.exception.OrraiHttpClientException;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.function.Supplier;

/**
 * {@link OrraiHttpClient} decorator that retries failed calls according to a {@link RetryPolicy}.
 *
 * <p>Transport-agnostic and Spring-free: it wraps any delegate adapter, so the same retry behavior
 * applies whether the underlying transport is {@code RestClient}, {@code WebClient} or OpenFeign.
 * Only {@link OrraiHttpClientException}s the policy deems retryable trigger another attempt; any
 * other exception, or exhausting the attempts, rethrows the last failure unchanged.
 */
public class RetryingOrraiHttpClient implements OrraiHttpClient {

    private static final Logger LOG = System.getLogger(RetryingOrraiHttpClient.class.getName());

    private final OrraiHttpClient delegate;

    private final RetryPolicy policy;

    public RetryingOrraiHttpClient(OrraiHttpClient delegate, RetryPolicy policy) {
        this.delegate = delegate;
        this.policy = policy;
    }

    @Override
    public <T, R> R execute(
            String path,
            String method,
            Map<String, String> headers,
            Map<String, String> queryParams,
            T body,
            Class<R> responseType) {

        return callWithRetry(method, path,
                () -> delegate.execute(path, method, headers, queryParams, body, responseType));
    }

    @Override
    public <T, R> R execute(
            String path,
            String method,
            Map<String, String> headers,
            Map<String, String> queryParams,
            T body,
            OrraiTypeRef<R> responseType) {

        return callWithRetry(method, path,
                () -> delegate.execute(path, method, headers, queryParams, body, responseType));
    }

    /** Runs {@code call}, retrying only the retryable {@link OrraiHttpClientException}s per policy. */
    private <R> R callWithRetry(String method, String path, Supplier<R> call) {
        int attempt = 0;
        while (true) {
            try {
                return call.get();
            } catch (OrraiHttpClientException ex) {
                if (attempt >= policy.getMaxRetries() || !policy.shouldRetry(ex)) {
                    throw ex;
                }
                attempt++;
                LOG.log(Level.WARNING, "Retrying {0} {1} (attempt {2}/{3}) after upstream failure: {4}",
                        method, path, attempt, policy.getMaxRetries(), ex.getMessage());
                sleep(policy.getRetryDelayMillis(), ex);
            }
        }
    }

    /** Waits between attempts; on interruption, aborts retrying and rethrows the last failure. */
    private static void sleep(long millis, OrraiHttpClientException lastFailure) {
        if (millis <= 0L) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw lastFailure;
        }
    }
}