package com.library.support.orrai.config;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigClientDefaultsTest {

    @Test
    void emitsCoreConnectionAndTimeoutDefaults() {
        var defaults = ConfigClientDefaults.build(props("https://config.corp:8888", null, null, null, true));

        assertThat(defaults)
                .containsEntry("spring.cloud.config.uri", "https://config.corp:8888")
                .containsEntry("spring.cloud.config.name", "${spring.application.name}")
                .containsEntry("spring.cloud.config.label", "main")
                .containsEntry("spring.cloud.config.fail-fast", true)
                .containsEntry("spring.cloud.config.request-connect-timeout", 5000)
                .containsEntry("spring.cloud.config.request-read-timeout", 30000);
    }

    @Test
    void usesExplicitApplicationNameOverPlaceholderWhenSet() {
        var defaults = ConfigClientDefaults.build(props("https://config.corp:8888", "prediction", null, null, true));

        assertThat(defaults).containsEntry("spring.cloud.config.name", "prediction");
    }

    @Test
    void mapsRetryPropertiesWhenRetryEnabled() {
        var defaults = ConfigClientDefaults.build(props("https://config.corp:8888", null, null, null, true));

        assertThat(defaults)
                .containsEntry("spring.cloud.config.retry.max-attempts", 6)
                .containsEntry("spring.cloud.config.retry.initial-interval", 1000L)
                .containsEntry("spring.cloud.config.retry.multiplier", 1.5d)
                .containsEntry("spring.cloud.config.retry.max-interval", 5000L);
    }

    @Test
    void omitsRetryPropertiesWhenRetryDisabled() {
        var defaults = ConfigClientDefaults.build(props("https://config.corp:8888", null, null, null, false));

        assertThat(defaults).doesNotContainKey("spring.cloud.config.retry.max-attempts");
    }

    @Test
    void includesBasicAuthOnlyWhenPresent() {
        var withAuth = ConfigClientDefaults.build(props("https://config.corp:8888", null, "svc", "secret", true));
        var withoutAuth = ConfigClientDefaults.build(props("https://config.corp:8888", null, null, null, true));

        assertThat(withAuth)
                .containsEntry("spring.cloud.config.username", "svc")
                .containsEntry("spring.cloud.config.password", "secret");
        assertThat(withoutAuth)
                .doesNotContainKey("spring.cloud.config.username")
                .doesNotContainKey("spring.cloud.config.password");
    }

    @Test
    void returnedMapIsImmutable() {
        var defaults = ConfigClientDefaults.build(props("https://config.corp:8888", null, null, null, true));

        assertThat(defaults).isInstanceOf(Map.class);
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> defaults.put("x", "y"));
    }

    private static ConfigClientProperties props(String uri, String applicationName,
            String username, String password, boolean retryEnabled) {
        return new ConfigClientProperties(true, uri, username, password, true, retryEnabled,
                6, 1000L, 1.5d, 5000L, "main", null, applicationName, 5000, 30000);
    }
}