package com.library.support.orrai.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the corporate Config Server client.
 *
 * <p>The heavy lifting — injecting {@code spring.config.import=configserver:} and the
 * {@code spring.cloud.config.*} defaults — happens earlier in
 * {@link ConfigClientEnvironmentPostProcessor} (it must run before Config Data). This class only wires
 * the beans that belong to the application context: it binds {@link ConfigClientProperties} and
 * registers the {@link ConfigClientStartupLogger}.
 *
 * <ul>
 *   <li>{@code @ConditionalOnClass} — active only when Spring Cloud Config Client is on the classpath.</li>
 *   <li>{@code @ConditionalOnProperty} — honours {@code starter.config.enabled} (on by default).</li>
 *   <li>{@code @ConditionalOnMissingBean} — every bean is overridable by the consumer.</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.cloud.config.client.ConfigClientProperties")
@ConditionalOnProperty(prefix = "starter.config", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ConfigClientProperties.class)
public class ConfigClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConfigClientStartupLogger orraiConfigClientStartupLogger(ConfigClientProperties properties) {
        return new ConfigClientStartupLogger(properties);
    }
}