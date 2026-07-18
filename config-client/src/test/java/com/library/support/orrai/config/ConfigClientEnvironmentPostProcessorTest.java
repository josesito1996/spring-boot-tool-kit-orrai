package com.library.support.orrai.config;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ConfigClientEnvironmentPostProcessorTest {

    private final ConfigClientEnvironmentPostProcessor processor =
            new ConfigClientEnvironmentPostProcessor(new DeferredLog());

    @Test
    void doesNothingWhenStarterIsDisabled() {
        var environment = new MockEnvironment();
        environment.setProperty("starter.config.enabled", "false");
        environment.setProperty("starter.config.uri", "https://config.corp:8888");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getPropertySources().contains("orraiConfigClientDefaults")).isFalse();
        assertThat(environment.getProperty("spring.config.import")).isNull();
    }

    @Test
    void injectsDefaultsAndConfigServerImportWhenEnabledWithUri() {
        var environment = new MockEnvironment();
        environment.setProperty("starter.config.uri", "https://config.corp:8888");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.cloud.config.uri")).isEqualTo("https://config.corp:8888");
        assertThat(environment.getProperty("spring.cloud.config.fail-fast")).isEqualTo("true");
        assertThat(environment.getProperty("spring.cloud.config.retry.max-attempts")).isEqualTo("6");
        assertThat(environment.getProperty("spring.config.import")).isEqualTo("configserver:");
    }

    @Test
    void throwsClearExceptionWhenEnabledWithoutUri() {
        var environment = new MockEnvironment();
        environment.setProperty("starter.config.label", "release");

        assertThatExceptionOfType(ConfigClientConfigurationException.class)
                .isThrownBy(() -> processor.postProcessEnvironment(environment, null))
                .withMessageContaining("starter.config.uri is required");
    }

    @Test
    void throwsWhenBasicAuthIsHalfSpecified() {
        var environment = new MockEnvironment();
        environment.setProperty("starter.config.uri", "https://config.corp:8888");
        environment.setProperty("starter.config.username", "svc");

        assertThatExceptionOfType(ConfigClientConfigurationException.class)
                .isThrownBy(() -> processor.postProcessEnvironment(environment, null))
                .withMessageContaining("username and starter.config.password");
    }

    @Test
    void usesOptionalImportWhenFailFastIsDisabled() {
        var environment = new MockEnvironment();
        environment.setProperty("starter.config.uri", "https://config.corp:8888");
        environment.setProperty("starter.config.fail-fast", "false");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.config.import")).isEqualTo("optional:configserver:");
    }

    @Test
    void neverOverridesConsumerConfigImport() {
        var environment = new MockEnvironment();
        environment.setProperty("starter.config.uri", "https://config.corp:8888");
        environment.setProperty("spring.config.import", "vault://");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.config.import")).isEqualTo("vault://");
        assertThat(environment.getPropertySources().contains("orraiConfigClientConfigImport")).isFalse();
    }

    @Test
    void consumerOverridesTakePrecedenceOverDefaults() {
        var environment = new MockEnvironment();
        environment.setProperty("starter.config.uri", "https://config.corp:8888");
        environment.setProperty("spring.cloud.config.label", "feature-x");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.cloud.config.label")).isEqualTo("feature-x");
    }

    @Test
    void runsBeforeConfigDataProcessor() {
        assertThat(processor.getOrder()).isLessThan(ConfigDataEnvironmentPostProcessor.ORDER);
    }
}