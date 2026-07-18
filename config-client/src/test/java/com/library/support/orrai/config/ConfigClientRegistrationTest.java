package com.library.support.orrai.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.EnvironmentPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the Spring Boot 4 wiring for both registration mechanisms:
 * <ul>
 *   <li>the {@link ConfigClientEnvironmentPostProcessor} implements the current
 *       {@code org.springframework.boot.EnvironmentPostProcessor} interface (Boot 4 moved it out of
 *       the legacy {@code …boot.env} package) and is declared under that exact key in
 *       {@code META-INF/spring.factories};</li>
 *   <li>the {@link ConfigClientAutoConfiguration} is listed in
 *       {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.</li>
 * </ul>
 * A stale import/key would silently stop either from ever running.
 */
class ConfigClientRegistrationTest {

    private static final String SPRING_FACTORIES = "META-INF/spring.factories";
    private static final String EPP_KEY = "org.springframework.boot.EnvironmentPostProcessor";
    private static final String IMPORTS_FILE =
            "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

    @Test
    void environmentPostProcessorImplementsTheBoot4Interface() {
        assertThat(EnvironmentPostProcessor.class)
                .isAssignableFrom(ConfigClientEnvironmentPostProcessor.class);
    }

    @Test
    void environmentPostProcessorIsRegisteredUnderTheBoot4Key() throws IOException {
        assertThat(springFactoriesValueFor(EPP_KEY))
                .contains(ConfigClientEnvironmentPostProcessor.class.getName());
    }

    @Test
    void autoConfigurationIsListedInImportsFile() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(IMPORTS_FILE)) {
            assertThat(in).as("AutoConfiguration.imports must exist").isNotNull();
            String contents = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(contents).contains(ConfigClientAutoConfiguration.class.getName());
        }
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