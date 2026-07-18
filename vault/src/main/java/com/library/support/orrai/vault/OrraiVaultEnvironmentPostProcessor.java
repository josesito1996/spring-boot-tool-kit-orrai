package com.library.support.orrai.vault;

import java.util.Map;

import org.apache.commons.logging.Log;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * Injects zero-configuration Vault defaults into the environment before Config Data resolves,
 * so spring-cloud-vault can fetch secrets with nothing more than {@code spring.application.name}
 * and {@code ORRAI_VAULT_*} credentials.
 *
 * <p>Defaults are added with {@link MutablePropertySources#addLast} (lowest precedence): anything
 * the consumer sets always wins. The actual authentication, KV v2 lookup, TLS and lease renewal are
 * delegated to spring-cloud-vault. Fail-fast ({@code spring.cloud.vault.fail-fast=true}) is a default,
 * so a missing or unreachable Vault halts startup instead of running without credentials.
 */
public class OrraiVaultEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String DEFAULTS_SOURCE_NAME = "orraiVaultDefaults";
    static final String IMPORT_SOURCE_NAME = "orraiVaultConfigImport";
    static final String CONFIG_IMPORT_PROPERTY = "spring.config.import";
    static final String VAULT_IMPORT = "vault://";

    private final Log log;

    public OrraiVaultEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this(logFactory.getLog(OrraiVaultEnvironmentPostProcessor.class));
    }

    OrraiVaultEnvironmentPostProcessor(Log log) {
        this.log = log;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        var env = OrraiVaultEnvVars.from(environment);
        if (!env.isConfigured()) {
            log.debug(OrraiVaultEnvVars.URI_VAR + " is not set; skipping Orrai Vault defaults.");
            return;
        }

        var sources = environment.getPropertySources();
        addDefaultsIfAbsent(sources, OrraiVaultDefaults.build(env));
        ensureVaultImport(environment, sources);

        log.info("Orrai Vault defaults registered; secrets will be resolved by spring-cloud-vault.");
    }

    private void addDefaultsIfAbsent(MutablePropertySources sources, Map<String, Object> defaults) {
        if (!sources.contains(DEFAULTS_SOURCE_NAME)) {
            sources.addLast(new MapPropertySource(DEFAULTS_SOURCE_NAME, defaults));
        }
    }

    private void ensureVaultImport(ConfigurableEnvironment environment, MutablePropertySources sources) {
        if (environment.getProperty(CONFIG_IMPORT_PROPERTY) != null || sources.contains(IMPORT_SOURCE_NAME)) {
            return;
        }
        sources.addLast(new MapPropertySource(
                IMPORT_SOURCE_NAME,
                Map.of(CONFIG_IMPORT_PROPERTY, VAULT_IMPORT)));
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER - 1;
    }
}