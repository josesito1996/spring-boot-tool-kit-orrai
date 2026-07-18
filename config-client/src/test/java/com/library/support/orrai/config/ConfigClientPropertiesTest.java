package com.library.support.orrai.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigClientPropertiesTest {

    @Test
    void withDefaultsMatchesTheDocumentedProductionDefaults() {
        var props = ConfigClientProperties.withDefaults();

        assertThat(props.enabled()).isTrue();
        assertThat(props.failFast()).isTrue();
        assertThat(props.retryEnabled()).isTrue();
        assertThat(props.maxAttempts()).isEqualTo(6);
        assertThat(props.initialInterval()).isEqualTo(1000L);
        assertThat(props.multiplier()).isEqualTo(1.5d);
        assertThat(props.maxInterval()).isEqualTo(5000L);
        assertThat(props.label()).isEqualTo("main");
        assertThat(props.connectTimeout()).isEqualTo(5000);
        assertThat(props.readTimeout()).isEqualTo(30000);
        assertThat(props.uri()).isNull();
        assertThat(props.hasBasicAuth()).isFalse();
    }

    @Test
    void hasBasicAuthWhenEitherCredentialIsPresent() {
        var onlyUser = new ConfigClientProperties(true, "u", "svc", null, true, true,
                6, 1000L, 1.5d, 5000L, "main", null, null, 5000, 30000);
        var onlyPassword = new ConfigClientProperties(true, "u", null, "secret", true, true,
                6, 1000L, 1.5d, 5000L, "main", null, null, 5000, 30000);

        assertThat(onlyUser.hasBasicAuth()).isTrue();
        assertThat(onlyPassword.hasBasicAuth()).isTrue();
    }
}