package com.library.support.orrai.autoconfigure;

import com.library.support.orrai.http.OrraiClientConfig;
import com.library.support.orrai.http.OrraiHttpClient;
import com.library.support.orrai.http.OrraiHttpClientFactory;
import com.library.support.orrai.http.OrraiHttpClientRegistry;
import com.library.support.orrai.http.OrraiHttpProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Auto-configuration for the outbound HTTP client module.
 *
 * <p>Builds one {@link OrraiHttpClient} per configured client (default + named) — each with its own
 * base URL, connect/read timeouts and retry policy taken from {@link OrraiHttpProperties} — and
 * exposes them through an {@link OrraiHttpClientRegistry}. The default client
 * ({@code orrai.http.client.*}) is additionally published as the {@code @Primary}
 * {@link OrraiHttpClient} bean for direct injection.
 *
 * <p>Every bean is guarded with {@link ConditionalOnMissingBean}, so a consumer can supply its own
 * implementation (e.g. a {@code WebClient} or OpenFeign adapter) without touching the library.
 */
@AutoConfiguration(after = RestClientAutoConfiguration.class)
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(OrraiHttpProperties.class)
public class OrraiHttpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OrraiHttpClientFactory orraiHttpClientFactory(RestClient.Builder restClientBuilder) {
        return new OrraiHttpClientFactory(restClientBuilder);
    }

    @Bean
    @ConditionalOnMissingBean
    public OrraiHttpClientRegistry orraiHttpClientRegistry(
            OrraiHttpClientFactory factory, OrraiHttpProperties properties) {
        Map<String, OrraiHttpClient> clients = new LinkedHashMap<>();

        OrraiClientConfig defaultConfig = properties.client();
        if (defaultConfig != null && StringUtils.hasText(defaultConfig.baseUrl())) {
            clients.put(OrraiHttpClientRegistry.DEFAULT_CLIENT, factory.create(defaultConfig));
        }
        properties.clients().forEach((name, config) -> clients.put(name, factory.create(config)));

        return new OrraiHttpClientRegistry(clients);
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "orrai.http.client", name = "base-url")
    public OrraiHttpClient orraiHttpClient(OrraiHttpClientRegistry registry) {
        return registry.client(OrraiHttpClientRegistry.DEFAULT_CLIENT);
    }
}