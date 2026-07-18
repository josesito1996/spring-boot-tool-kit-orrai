package com.library.support.orrai.config;

import java.util.Map;

import org.apache.commons.logging.Log;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * Injects zero-configuration Spring Cloud Config Client defaults into the environment before Config
 * Data resolves, so a microservice consumes the corporate Config Server with nothing more than the
 * {@code starter.config.*} connection settings.
 *
 * <p>Runs <strong>before</strong> {@link ConfigDataEnvironmentPostProcessor}: {@code spring.config.import}
 * must be present at that point or the {@code configserver:} import is ignored — which is exactly why
 * this cannot live in an {@code @AutoConfiguration}. Defaults are added with
 * {@link MutablePropertySources#addLast} (lowest precedence), so any {@code spring.cloud.config.*} the
 * consumer sets by hand always wins. Spring Cloud Config Client then performs the real fetch, retry,
 * timeouts and {@code @RefreshScope} wiring. Mirrors {@code OrraiVaultEnvironmentPostProcessor}.
 *
 * <p>Credentials are never logged — only the URI, application, profile and label (all non-secret).
 */
public class ConfigClientEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String DEFAULTS_SOURCE_NAME = "orraiConfigClientDefaults";
    static final String IMPORT_SOURCE_NAME = "orraiConfigClientConfigImport";
    static final String CONFIG_IMPORT_PROPERTY = "spring.config.import";
    static final String CONFIG_SERVER_IMPORT = "configserver:";
    static final String OPTIONAL_CONFIG_SERVER_IMPORT = "optional:configserver:";
    static final String PREFIX = "starter.config";

    private final Log log;

    public ConfigClientEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this(logFactory.getLog(ConfigClientEnvironmentPostProcessor.class));
    }

    ConfigClientEnvironmentPostProcessor(Log log) {
        this.log = log;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        var properties = bind(environment);
        if (!properties.enabled()) {
            log.debug(PREFIX + ".enabled=false; skipping Orrai Config Client defaults.");
            return;
        }
        validate(properties);

        var sources = environment.getPropertySources();
        addDefaultsIfAbsent(sources, ConfigClientDefaults.build(properties));
        ensureConfigServerImport(environment, sources, properties);

        logStartup(properties);
    }

    private ConfigClientProperties bind(ConfigurableEnvironment environment) {
        return Binder.get(environment)
                .bind(PREFIX, Bindable.of(ConfigClientProperties.class))
                .orElseGet(ConfigClientProperties::withDefaults);
    }

    private void validate(ConfigClientProperties properties) {
        if (!hasText(properties.uri())) {
            throw new ConfigClientConfigurationException(
                    PREFIX + ".uri is required when the Config Client starter is enabled "
                            + "(set it, or disable the starter with " + PREFIX + ".enabled=false).");
        }
        boolean hasUser = hasText(properties.username());
        boolean hasPassword = hasText(properties.password());
        if (hasUser != hasPassword) {
            throw new ConfigClientConfigurationException(
                    "Basic authentication requires both " + PREFIX + ".username and " + PREFIX
                            + ".password to be set (or neither).");
        }
    }

    private void addDefaultsIfAbsent(MutablePropertySources sources, Map<String, Object> defaults) {
        if (!sources.contains(DEFAULTS_SOURCE_NAME)) {
            sources.addLast(new MapPropertySource(DEFAULTS_SOURCE_NAME, defaults));
        }
    }

    private void ensureConfigServerImport(ConfigurableEnvironment environment,
            MutablePropertySources sources, ConfigClientProperties properties) {
        if (environment.getProperty(CONFIG_IMPORT_PROPERTY) != null || sources.contains(IMPORT_SOURCE_NAME)) {
            return;
        }
        String importValue = properties.failFast() ? CONFIG_SERVER_IMPORT : OPTIONAL_CONFIG_SERVER_IMPORT;
        sources.addLast(new MapPropertySource(IMPORT_SOURCE_NAME,
                Map.of(CONFIG_IMPORT_PROPERTY, importValue)));
    }

    private void logStartup(ConfigClientProperties properties) {
        log.info("Loading configuration from Config Server...");
        log.info("Config Server URI: " + properties.uri());
        log.info("Application: " + (hasText(properties.applicationName())
                ? properties.applicationName() : "${spring.application.name}"));
        log.info("Profile: " + (hasText(properties.profile()) ? properties.profile() : "(active profiles)"));
        log.info("Label: " + properties.label());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER - 1;
    }
}