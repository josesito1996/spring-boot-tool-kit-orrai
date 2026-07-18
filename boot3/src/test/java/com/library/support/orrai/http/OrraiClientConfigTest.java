package com.library.support.orrai.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrraiClientConfig headers normalization")
class OrraiClientConfigTest {

    private static final Duration CONNECT = Duration.ofSeconds(1);
    private static final Duration READ = Duration.ofSeconds(5);
    private static final Duration RETRY_DELAY = Duration.ofMillis(500);
    private static final Set<Integer> RETRYABLE = Set.of(503);

    private OrraiClientConfig configWithHeaders(Map<String, String> headers) {
        return new OrraiClientConfig(
                "https://api.example.com", CONNECT, READ, 0, RETRY_DELAY, RETRYABLE, true, headers);
    }

    @Test
    @DisplayName("null headers are normalized to an empty map, never null")
    void nullHeaders_normalizedToEmptyMap() {
        OrraiClientConfig config = configWithHeaders(null);

        assertThat(config.headers()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("provided headers are preserved")
    void providedHeaders_arePreserved() {
        OrraiClientConfig config = configWithHeaders(Map.of("X-Api-Key", "abc123", "X-Tenant-Id", "legalbyte"));

        assertThat(config.headers())
                .containsEntry("X-Api-Key", "abc123")
                .containsEntry("X-Tenant-Id", "legalbyte");
    }

    @Test
    @DisplayName("headers are copied defensively — mutating the source map does not affect the config")
    void headers_areCopiedDefensively() {
        Map<String, String> source = new HashMap<>();
        source.put("X-Api-Key", "abc123");

        OrraiClientConfig config = configWithHeaders(source);
        source.put("X-Injected", "evil");

        assertThat(config.headers()).containsOnlyKeys("X-Api-Key");
    }

    @Test
    @DisplayName("the exposed headers map is immutable")
    void headers_areImmutable() {
        OrraiClientConfig config = configWithHeaders(Map.of("X-Api-Key", "abc123"));

        assertThatThrownBy(() -> config.headers().put("X-Injected", "evil"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}