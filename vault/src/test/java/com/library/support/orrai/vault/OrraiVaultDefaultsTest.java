package com.library.support.orrai.vault;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrraiVaultDefaultsTest {

    @Test
    void alwaysEmitsKvV2AndFailFastDefaults() {
        var defaults = OrraiVaultDefaults.build(env(Optional.of("https://vault:8200"),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));

        assertThat(defaults)
                .containsEntry("spring.cloud.vault.uri", "https://vault:8200")
                .containsEntry("spring.cloud.vault.kv.enabled", true)
                .containsEntry("spring.cloud.vault.kv.backend", "secret")
                .containsEntry("spring.cloud.vault.kv.default-context", "${spring.application.name}")
                .containsEntry("spring.cloud.vault.fail-fast", true);
    }

    @Test
    void mapsTokenAuthenticationWhenOnlyTokenIsPresent() {
        var defaults = OrraiVaultDefaults.build(env(Optional.of("http://vault:8200"),
                Optional.of("s.abc123"), Optional.empty(), Optional.empty(), Optional.empty()));

        assertThat(defaults)
                .containsEntry("spring.cloud.vault.authentication", "TOKEN")
                .containsEntry("spring.cloud.vault.token", "s.abc123")
                .doesNotContainKey("spring.cloud.vault.app-role.role-id");
    }

    @Test
    void prefersAppRoleOverTokenWhenBothArePresent() {
        var defaults = OrraiVaultDefaults.build(env(Optional.of("https://vault:8200"),
                Optional.of("s.ignored"), Optional.of("role-1"), Optional.of("secret-1"), Optional.empty()));

        assertThat(defaults)
                .containsEntry("spring.cloud.vault.authentication", "APPROLE")
                .containsEntry("spring.cloud.vault.app-role.role-id", "role-1")
                .containsEntry("spring.cloud.vault.app-role.secret-id", "secret-1")
                .doesNotContainKey("spring.cloud.vault.token");
    }

    @Test
    void omitsAuthenticationWhenNoCredentialsAreSupplied() {
        var defaults = OrraiVaultDefaults.build(env(Optional.of("https://vault:8200"),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));

        assertThat(defaults).doesNotContainKey("spring.cloud.vault.authentication");
    }

    @Test
    void includesNamespaceWhenSupplied() {
        var defaults = OrraiVaultDefaults.build(env(Optional.of("https://vault:8200"),
                Optional.of("s.abc"), Optional.empty(), Optional.empty(), Optional.of("team-a")));

        assertThat(defaults).containsEntry("spring.cloud.vault.namespace", "team-a");
    }

    @Test
    void returnedMapIsImmutable() {
        var defaults = OrraiVaultDefaults.build(env(Optional.of("https://vault:8200"),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));

        assertThat(defaults).isInstanceOf(Map.class);
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> defaults.put("x", "y"));
    }

    private static OrraiVaultEnvVars env(Optional<String> uri, Optional<String> token,
            Optional<String> roleId, Optional<String> secretId, Optional<String> namespace) {
        return new OrraiVaultEnvVars(uri, token, roleId, secretId, namespace);
    }
}