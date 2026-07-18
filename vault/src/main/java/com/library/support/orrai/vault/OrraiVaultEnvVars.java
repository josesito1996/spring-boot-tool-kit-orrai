package com.library.support.orrai.vault;

import java.util.Optional;

import org.springframework.core.env.Environment;

/**
 * Immutable snapshot of the {@code ORRAI_VAULT_*} environment variables that drive the
 * Vault connection. Reading is isolated here so the rest of the module never touches raw
 * variable names, keeping the parsing decoupled from the injection mechanism.
 */
record OrraiVaultEnvVars(
        Optional<String> uri,
        Optional<String> token,
        Optional<String> roleId,
        Optional<String> secretId,
        Optional<String> namespace) {

    static final String URI_VAR = "ORRAI_VAULT_URL";
    static final String TOKEN_VAR = "ORRAI_VAULT_TOKEN";
    static final String ROLE_ID_VAR = "ORRAI_VAULT_ROLE_ID";
    static final String SECRET_ID_VAR = "ORRAI_VAULT_SECRET_ID";
    static final String NAMESPACE_VAR = "ORRAI_VAULT_NAMESPACE";

    static OrraiVaultEnvVars from(Environment environment) {
        return new OrraiVaultEnvVars(
                read(environment, URI_VAR),
                read(environment, TOKEN_VAR),
                read(environment, ROLE_ID_VAR),
                read(environment, SECRET_ID_VAR),
                read(environment, NAMESPACE_VAR));
    }

    private static Optional<String> read(Environment environment, String name) {
        return Optional.ofNullable(environment.getProperty(name))
                .map(String::trim)
                .filter(value -> !value.isEmpty());
    }

    boolean isConfigured() {
        return uri.isPresent();
    }

    boolean hasAppRole() {
        return roleId.isPresent() && secretId.isPresent();
    }

    boolean hasToken() {
        return token.isPresent();
    }
}