package com.library.support.orrai.http;

import com.library.support.orrai.exception.OrraiHttpClientException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetryingOrraiHttpClientTest {

    private static final RetryPolicy POLICY =
            new RetryPolicy(2, 0L, Set.of(503), true);

    /** Hand-rolled delegate that fails a fixed number of times, then returns a value. */
    private static final class StubClient implements OrraiHttpClient {
        private final AtomicInteger calls = new AtomicInteger();
        private final int failuresBeforeSuccess;
        private final OrraiHttpClientException failure;

        StubClient(int failuresBeforeSuccess, OrraiHttpClientException failure) {
            this.failuresBeforeSuccess = failuresBeforeSuccess;
            this.failure = failure;
        }

        @Override
        public <T, R> R execute(String path, String method, Map<String, String> headers,
                Map<String, String> queryParams, T body, Class<R> responseType) {
            return respond();
        }

        @Override
        public <T, R> R execute(String path, String method, Map<String, String> headers,
                Map<String, String> queryParams, T body, OrraiTypeRef<R> responseType) {
            return respond();
        }

        @SuppressWarnings("unchecked")
        private <R> R respond() {
            if (calls.getAndIncrement() < failuresBeforeSuccess) {
                throw failure;
            }
            return (R) "ok";
        }
    }

    private static String call(OrraiHttpClient client) {
        return client.execute("/p", "GET", null, null, null, String.class);
    }

    @Test
    @DisplayName("retries a retryable failure and succeeds within the attempt budget")
    void retriesThenSucceeds() {
        var failure = new OrraiHttpClientException(503, "/p", "down", "unavailable");
        var stub = new StubClient(2, failure); // 2 failures, then success on the 3rd call
        var client = new RetryingOrraiHttpClient(stub, POLICY);

        assertEquals("ok", call(client));
        assertEquals(3, stub.calls.get()); // 1 initial + 2 retries
    }

    @Test
    @DisplayName("gives up and rethrows the last failure once retries are exhausted")
    void exhaustsRetries() {
        var failure = new OrraiHttpClientException(503, "/p", "down", "unavailable");
        var stub = new StubClient(Integer.MAX_VALUE, failure);
        var client = new RetryingOrraiHttpClient(stub, POLICY);

        var thrown = assertThrows(OrraiHttpClientException.class, () -> call(client));
        assertSame(failure, thrown);
        assertEquals(3, stub.calls.get()); // 1 initial + 2 retries, then give up
    }

    @Test
    @DisplayName("does not retry a non-retryable status")
    void doesNotRetryNonRetryableStatus() {
        var failure = new OrraiHttpClientException(404, "/p", "missing", "not found");
        var stub = new StubClient(Integer.MAX_VALUE, failure);
        var client = new RetryingOrraiHttpClient(stub, POLICY);

        assertThrows(OrraiHttpClientException.class, () -> call(client));
        assertEquals(1, stub.calls.get()); // no retry attempted
    }

    @Test
    @DisplayName("retries transport-level failures when enabled")
    void retriesTransportFailures() {
        var failure = new OrraiHttpClientException("/p", "connection refused", new RuntimeException());
        var stub = new StubClient(1, failure);
        var client = new RetryingOrraiHttpClient(stub, POLICY);

        assertEquals("ok", call(client));
        assertEquals(2, stub.calls.get());
    }
}