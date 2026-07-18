package com.library.support.orrai.vault;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.boot.EnvironmentPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the Spring Boot 4 registration contract: the processor must implement the current
 * {@code org.springframework.boot.EnvironmentPostProcessor} interface and be declared in
 * {@code META-INF/spring.factories} under that exact key. Boot 4 moved the interface out of the
 * legacy {@code org.springframework.boot.env} package, so a stale import or key would silently
 * stop the processor from ever running.
 */
class OrraiVaultRegistrationTest {

    private static final String SPRING_FACTORIES = "META-INF/spring.factories";
    private static final String EPP_KEY = "org.springframework.boot.EnvironmentPostProcessor";

    @Test
    void implementsTheBoot4EnvironmentPostProcessorInterface() {
        assertThat(EnvironmentPostProcessor.class)
                .isAssignableFrom(OrraiVaultEnvironmentPostProcessor.class);
    }

    @Test
    void isRegisteredInSpringFactoriesUnderTheBoot4Key() throws IOException {
        assertThat(springFactoriesValueFor(EPP_KEY))
                .contains(OrraiVaultEnvironmentPostProcessor.class.getName());
    }

    private String springFactoriesValueFor(String key) throws IOException {
        Enumeration<URL> resources = getClass().getClassLoader().getResources(SPRING_FACTORIES);
        StringBuilder values = new StringBuilder();
        while (resources.hasMoreElements()) {
            Properties props = new Properties();
            try (InputStream in = resources.nextElement().openStream()) {
                props.load(in);
            }
            String value = props.getProperty(key);
            if (value != null) {
                values.append(value).append(',');
            }
        }
        return values.toString();
    }
}
