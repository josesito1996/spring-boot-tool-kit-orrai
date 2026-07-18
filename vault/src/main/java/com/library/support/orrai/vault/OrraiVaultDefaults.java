package com.library.support.orrai.vault;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure translation of an {@link OrraiVaultEnvVars} snapshot into the {@code spring.cloud.vault.*}
 * defaults consumed by spring-cloud-vault. No Spring {@code Environment} mutation happens here so
 * the mapping stays trivially unit-testable and transparent to future Java upgrades.
 *
 * <p>The KV v2 path convention is {@code secret/<spring.application.name>}; the application name is
 * emitted as a literal placeholder so it resolves after Config Data loads, never in this class.
 */
final class OrraiVaultDefaults {

    private static final String PREFIX = "spring.cloud.vault.";
    private static final String APP_NAME_PLACEHOLDER = "${spring.application.name}";

    private enum AuthMethod {
        APPROLE, TOKEN, NONE
    }

    private OrraiVaultDefaults() {
    }

    static Map<String, Object> build(OrraiVaultEnvVars env) {
        var props = new LinkedHashMap<String, Object>();

        env.uri().ifPresent(uri -> props.put(PREFIX + "uri", uri));
        env.namespace().ifPresent(namespace -> props.put(PREFIX + "namespace", namespace));

        props.put(PREFIX + "kv.enabled", true);
        props.put(PREFIX + "kv.backend", "secret");
        props.put(PREFIX + "kv.default-context", APP_NAME_PLACEHOLDER);
        props.put(PREFIX + "kv.application-name", APP_NAME_PLACEHOLDER);
        props.put(PREFIX + "fail-fast", true);

        applyAuthentication(props, env);

        return Map.copyOf(props);
    }

    private static void applyAuthentication(Map<String, Object> props, OrraiVaultEnvVars env) {
        switch (authMethod(env)) {
            case APPROLE -> {
                props.put(PREFIX + "authentication", "APPROLE");
                props.put(PREFIX + "app-role.role-id", env.roleId().orElseThrow());
                props.put(PREFIX + "app-role.secret-id", env.secretId().orElseThrow());
            }
            case TOKEN -> {
                props.put(PREFIX + "authentication", "TOKEN");
                props.put(PREFIX + "token", env.token().orElseThrow());
            }
            case NONE -> {
                // No credentials in the environment: the consumer configures authentication
                // explicitly and spring-cloud-vault fails fast if it is ultimately missing.
            }
        }
    }

    private static AuthMethod authMethod(OrraiVaultEnvVars env) {
        if (env.hasAppRole()) {
            return AuthMethod.APPROLE;
        }
        if (env.hasToken()) {
            return AuthMethod.TOKEN;
        }
        return AuthMethod.NONE;
    }
}