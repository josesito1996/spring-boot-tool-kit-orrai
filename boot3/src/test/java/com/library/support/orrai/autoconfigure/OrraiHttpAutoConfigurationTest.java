package com.library.support.orrai.autoconfigure;

import com.library.support.orrai.http.OrraiHttpClient;
import com.library.support.orrai.http.OrraiHttpClientRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OrraiHttpAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RestClientAutoConfiguration.class, OrraiHttpAutoConfiguration.class));

    @Test
    @DisplayName("registers the default client as @Primary and named clients in the registry")
    void registersDefaultAndNamedClients() {
        runner.withPropertyValues(
                        "orrai.http.client.base-url=https://api.example.com",
                        "orrai.http.client.read-timeout=10s",
                        "orrai.http.client.max-retries=3",
                        "orrai.http.clients.payments.base-url=https://payments.example.com",
                        "orrai.http.clients.inventory.base-url=https://inventory.example.com")
                .run(context -> {
                    assertThat(context).hasSingleBean(OrraiHttpClient.class);
                    assertThat(context).hasSingleBean(OrraiHttpClientRegistry.class);

                    OrraiHttpClientRegistry registry = context.getBean(OrraiHttpClientRegistry.class);
                    assertThat(registry.names())
                            .containsExactlyInAnyOrder("default", "payments", "inventory");
                });
    }

    @Test
    @DisplayName("without a default base-url, no primary client bean is created but named clients still register")
    void noDefaultClient() {
        runner.withPropertyValues("orrai.http.clients.payments.base-url=https://payments.example.com")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OrraiHttpClient.class);
                    assertThat(context.getBean(OrraiHttpClientRegistry.class).has("payments")).isTrue();
                });
    }

    @Test
    @DisplayName("backs off when the consumer supplies its own OrraiHttpClient")
    void consumerOverride() {
        runner.withPropertyValues("orrai.http.client.base-url=https://api.example.com")
                .withUserConfiguration(CustomClientConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(OrraiHttpClient.class);
                    assertThat(context.getBean(OrraiHttpClient.class)).isSameAs(CustomClientConfig.CUSTOM);
                });
    }

    @Configuration
    static class CustomClientConfig {
        static final OrraiHttpClient CUSTOM = mock(OrraiHttpClient.class);

        @Bean
        OrraiHttpClient orraiHttpClient() {
            return CUSTOM;
        }
    }
}