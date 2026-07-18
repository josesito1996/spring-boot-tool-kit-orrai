package com.library.support.orrai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Connection settings for the corporate Spring Cloud Config Server, bound from the
 * {@code starter.config} prefix. These are the <em>only</em> knobs a microservice touches; the
 * {@link ConfigClientEnvironmentPostProcessor} translates them into the underlying
 * {@code spring.cloud.config.*} properties and injects {@code spring.config.import=configserver:}.
 *
 * <pre>{@code
 * starter:
 *   config:
 *     enabled: true          # set false to disable the starter entirely
 *     uri: https://config.corp.internal:8888
 *     username: svc-user     # optional basic auth (both or neither)
 *     password: ${CONFIG_PWD}
 *     fail-fast: true
 *     retry-enabled: true
 *     max-attempts: 6
 *     initial-interval: 1000
 *     multiplier: 1.5
 *     max-interval: 5000
 *     label: main
 *     profile:               # defaults to the active Spring profiles
 *     application-name:      # defaults to ${spring.application.name}
 *     connect-timeout: 5000
 *     read-timeout: 30000
 * }</pre>
 *
 * <p>Immutable record; {@link DefaultValue} supplies the production defaults for the fields the
 * developer leaves unset, honoured both by {@code @ConfigurationProperties} binding and by the
 * {@code Binder} the environment post-processor uses before Config Data runs.
 */
@ConfigurationProperties(prefix = "starter.config")
public record ConfigClientProperties(
        @DefaultValue("true") boolean enabled,
        String uri,
        String username,
        String password,
        @DefaultValue("true") boolean failFast,
        @DefaultValue("true") boolean retryEnabled,
        @DefaultValue("6") int maxAttempts,
        @DefaultValue("1000") long initialInterval,
        @DefaultValue("1.5") double multiplier,
        @DefaultValue("5000") long maxInterval,
        @DefaultValue("main") String label,
        String profile,
        String applicationName,
        @DefaultValue("5000") int connectTimeout,
        @DefaultValue("30000") int readTimeout) {

    /**
     * All-defaults instance, used when the {@code starter.config} tree is entirely absent so the
     * {@code Binder} has nothing to bind. Mirrors the {@link DefaultValue} annotations above.
     */
    public static ConfigClientProperties withDefaults() {
        return new ConfigClientProperties(true, null, null, null, true, true, 6,
                1000L, 1.5d, 5000L, "main", null, null, 5000, 30000);
    }

    /** {@code true} when either credential is present (basic auth is intended). */
    public boolean hasBasicAuth() {
        return hasText(username) || hasText(password);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}