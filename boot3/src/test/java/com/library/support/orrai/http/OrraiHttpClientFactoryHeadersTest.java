package com.library.support.orrai.http;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that stands up a real HTTP server (WireMock) to verify that headers configured on
 * {@link OrraiClientConfig} are applied by {@link OrraiHttpClientFactory} as default headers and are
 * actually sent on the wire — coverage the mock-transport unit tests cannot provide, because the
 * factory builds its own {@code ClientHttpRequestFactory}.
 */
@DisplayName("OrraiHttpClientFactory default headers (WireMock)")
class OrraiHttpClientFactoryHeadersTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(options().dynamicPort())
            .build();

    record Greeting(String message) {}

    private OrraiHttpClient clientWithHeaders(Map<String, String> headers) {
        OrraiClientConfig config = new OrraiClientConfig(
                wireMock.baseUrl(),
                Duration.ofSeconds(1),
                Duration.ofSeconds(5),
                0,
                Duration.ofMillis(500),
                Set.of(503),
                true,
                headers);
        return new OrraiHttpClientFactory(RestClient.builder()).create(config);
    }

    @Test
    @DisplayName("configured headers are sent on every request")
    void configuredHeaders_areSentOnEveryRequest() {
        wireMock.stubFor(get(urlEqualTo("/greetings")).willReturn(okJson("{\"message\":\"hola\"}")));
        OrraiHttpClient client = clientWithHeaders(Map.of("X-Api-Key", "abc123", "X-Tenant-Id", "legalbyte"));

        Greeting result = client.execute("/greetings", "GET", null, null, null, Greeting.class);

        assertThat(result.message()).isEqualTo("hola");
        wireMock.verify(getRequestedFor(urlEqualTo("/greetings"))
                .withHeader("X-Api-Key", equalTo("abc123"))
                .withHeader("X-Tenant-Id", equalTo("legalbyte")));
    }

    @Test
    @DisplayName("per-request headers are merged with the configured default headers")
    void perRequestHeaders_mergeWithDefaults() {
        wireMock.stubFor(get(urlEqualTo("/greetings")).willReturn(okJson("{\"message\":\"hola\"}")));
        OrraiHttpClient client = clientWithHeaders(Map.of("X-Api-Key", "abc123"));

        client.execute("/greetings", "GET", Map.of("X-Request-Id", "req-1"), null, null, Greeting.class);

        wireMock.verify(getRequestedFor(urlEqualTo("/greetings"))
                .withHeader("X-Api-Key", equalTo("abc123"))
                .withHeader("X-Request-Id", equalTo("req-1")));
    }

    @Test
    @DisplayName("no headers configured sends only the transport defaults")
    void noHeadersConfigured_sendsNoCustomHeaders() {
        wireMock.stubFor(get(urlEqualTo("/greetings")).willReturn(okJson("{\"message\":\"hola\"}")));
        OrraiHttpClient client = clientWithHeaders(null);

        Greeting result = client.execute("/greetings", "GET", null, null, null, Greeting.class);

        assertThat(result.message()).isEqualTo("hola");
        wireMock.verify(getRequestedFor(urlEqualTo("/greetings")).withoutHeader("X-Api-Key"));
    }
}