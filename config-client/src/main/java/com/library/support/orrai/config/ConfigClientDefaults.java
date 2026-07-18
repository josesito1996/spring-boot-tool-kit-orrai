package com.library.support.orrai.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure translation of {@link ConfigClientProperties} into the {@code spring.cloud.config.*} defaults
 * consumed by Spring Cloud Config Client. No Spring {@code Environment} mutation happens here, so the
 * mapping stays trivially unit-testable and transparent — mirroring {@code OrraiVaultDefaults}.
 *
 * <p>The application name is emitted as a literal {@code ${spring.application.name}} placeholder when
 * unset, so it resolves after Config Data loads rather than in this class. The caller guarantees a
 * non-blank {@link ConfigClientProperties#uri()} before invoking {@link #build}, keeping every emitted
 * value non-null (required by {@link Map#copyOf}).
 */
final class ConfigClientDefaults {

    private static final String PREFIX = "spring.cloud.config.";
    private static final String APP_NAME_PLACEHOLDER = "${spring.application.name}";

    private ConfigClientDefaults() {
    }

    static Map<String, Object> build(ConfigClientProperties props) {
        var map = new LinkedHashMap<String, Object>();

        map.put(PREFIX + "uri", props.uri());
        map.put(PREFIX + "name", hasText(props.applicationName()) ? props.applicationName() : APP_NAME_PLACEHOLDER);
        map.put(PREFIX + "label", props.label());
        map.put(PREFIX + "fail-fast", props.failFast());
        map.put(PREFIX + "request-connect-timeout", props.connectTimeout());
        map.put(PREFIX + "request-read-timeout", props.readTimeout());

        if (hasText(props.profile())) {
            map.put(PREFIX + "profile", props.profile());
        }
        applyBasicAuth(map, props);
        applyRetry(map, props);

        return Map.copyOf(map);
    }

    private static void applyBasicAuth(Map<String, Object> map, ConfigClientProperties props) {
        if (hasText(props.username())) {
            map.put(PREFIX + "username", props.username());
        }
        if (hasText(props.password())) {
            map.put(PREFIX + "password", props.password());
        }
    }

    private static void applyRetry(Map<String, Object> map, ConfigClientProperties props) {
        if (!props.retryEnabled()) {
            return;
        }
        map.put(PREFIX + "retry.max-attempts", props.maxAttempts());
        map.put(PREFIX + "retry.initial-interval", props.initialInterval());
        map.put(PREFIX + "retry.multiplier", props.multiplier());
        map.put(PREFIX + "retry.max-interval", props.maxInterval());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}