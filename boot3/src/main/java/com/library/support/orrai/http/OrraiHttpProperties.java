package com.library.support.orrai.http;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.Map;

/**
 * Root configuration for the HTTP client module, bound from the {@code orrai.http} prefix.
 *
 * <pre>{@code
 * # Default / primary client  -> injectable directly as OrraiHttpClient
 * orrai.http.client.base-url=https://api.example.com
 * orrai.http.client.connect-timeout=2s
 * orrai.http.client.read-timeout=5s
 * orrai.http.client.max-retries=3
 * orrai.http.client.retry-delay=500ms
 *
 * # Additional named clients -> resolved via OrraiHttpClientRegistry.client("payments")
 * orrai.http.clients.payments.base-url=https://payments.example.com
 * orrai.http.clients.payments.read-timeout=10s
 * orrai.http.clients.payments.max-retries=5
 * orrai.http.clients.inventory.base-url=https://inventory.example.com
 * }</pre>
 *
 * @param client  the default client configuration ({@code orrai.http.client.*}); may be {@code null}
 * @param clients additional named clients ({@code orrai.http.clients.<name>.*}); never {@code null}
 */
@ConfigurationProperties(prefix = "orrai.http")
public record OrraiHttpProperties(
        @NestedConfigurationProperty OrraiClientConfig client,
        Map<String, OrraiClientConfig> clients) {

    public OrraiHttpProperties {
        clients = (clients == null) ? Map.of() : clients;
    }
}