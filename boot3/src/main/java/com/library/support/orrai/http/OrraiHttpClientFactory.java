package com.library.support.orrai.http;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

/**
 * Builds {@link OrraiHttpClient} instances from an {@link OrraiClientConfig}.
 *
 * <p>Each client gets its own {@link RestClient} (cloned from the shared auto-configured builder so
 * message converters are reused) with per-client base URL and connect/read timeouts. When the config
 * enables retries, the {@code RestClient} adapter is wrapped in a {@link RetryingOrraiHttpClient};
 * because retry is a transport-agnostic decorator, the same policy will apply to a future
 * {@code WebClient} or OpenFeign adapter without change.
 */
public class OrraiHttpClientFactory {

    private final RestClient.Builder builder;

    public OrraiHttpClientFactory(RestClient.Builder builder) {
        this.builder = builder;
    }

    /** Creates a fully configured client (timeouts applied, retry decorator added when enabled). */
    public OrraiHttpClient create(OrraiClientConfig config) {
        Assert.hasText(config.baseUrl(), "orrai.http base-url must be set for each configured client");

        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(config.connectTimeout())
                .withReadTimeout(config.readTimeout());
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);

        RestClient.Builder clientBuilder = builder.clone()
                .baseUrl(config.baseUrl())
                .requestFactory(requestFactory);
        config.headers().forEach(clientBuilder::defaultHeader);
        RestClient restClient = clientBuilder.build();

        OrraiHttpClient client = new RestClientOrraiHttpClient(restClient);
        if (config.maxRetries() <= 0) {
            return client;
        }

        RetryPolicy policy = new RetryPolicy(
                config.maxRetries(),
                config.retryDelay().toMillis(),
                config.retryableStatusCodes(),
                config.retryOnTransportError());
        return new RetryingOrraiHttpClient(client, policy);
    }
}