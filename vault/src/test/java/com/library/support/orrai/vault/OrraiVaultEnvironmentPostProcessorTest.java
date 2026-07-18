package com.library.support.orrai.vault;

import org.junit.jupiter.api.Test;

import org.springframework.boot.logging.DeferredLog;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class OrraiVaultEnvironmentPostProcessorTest {

    private final OrraiVaultEnvironmentPostProcessor processor =
            new OrraiVaultEnvironmentPostProcessor(new DeferredLog());

    @Test
    void doesNothingWhenVaultUrlIsAbsent() {
        var environment = new MockEnvironment();

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getPropertySources().contains("orraiVaultDefaults")).isFalse();
        assertThat(environment.getProperty("spring.config.import")).isNull();
    }

    @Test
    void injectsDefaultsAndVaultImportWhenUrlIsPresent() {
        var environment = new MockEnvironment();
        environment.setProperty("ORRAI_VAULT_URL", "https://vault.internal:8200");
        environment.setProperty("ORRAI_VAULT_TOKEN", "s.token");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.cloud.vault.uri")).isEqualTo("https://vault.internal:8200");
        assertThat(environment.getProperty("spring.cloud.vault.authentication")).isEqualTo("TOKEN");
        assertThat(environment.getProperty("spring.config.import")).isEqualTo("vault://");
    }

    @Test
    void neverOverridesConsumerConfigImport() {
        var environment = new MockEnvironment();
        environment.setProperty("ORRAI_VAULT_URL", "https://vault.internal:8200");
        environment.setProperty("spring.config.import", "optional:configtree:/etc/secrets/");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.config.import")).isEqualTo("optional:configtree:/etc/secrets/");
        assertThat(environment.getPropertySources().contains("orraiVaultConfigImport")).isFalse();
    }

    @Test
    void consumerOverridesTakePrecedenceOverDefaults() {
        var environment = new MockEnvironment();
        environment.setProperty("ORRAI_VAULT_URL", "https://vault.internal:8200");
        environment.setProperty("spring.cloud.vault.kv.backend", "kv-custom");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.cloud.vault.kv.backend")).isEqualTo("kv-custom");
    }

    @Test
    void runsBeforeConfigDataProcessor() {
        assertThat(processor.getOrder())
                .isLessThan(org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor.ORDER);
    }
}