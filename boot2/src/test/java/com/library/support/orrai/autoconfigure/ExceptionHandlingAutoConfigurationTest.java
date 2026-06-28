package com.library.support.orrai.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.library.support.orrai.handler.GlobalExceptionHandler;
import com.library.support.orrai.handler.ValidationExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class ExceptionHandlingAutoConfigurationTest {

    private final WebApplicationContextRunner webRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ExceptionHandlingAutoConfiguration.class));

    @Test
    @DisplayName("registers both handlers in a servlet web app (validation on classpath)")
    void registersHandlersInWebApp() {
        webRunner.run(context -> {
            assertThat(context).hasSingleBean(GlobalExceptionHandler.class);
            assertThat(context).hasSingleBean(ValidationExceptionHandler.class);
        });
    }

    @Test
    @DisplayName("backs off when the consumer defines its own GlobalExceptionHandler")
    void backsOffOnUserBean() {
        webRunner.withUserConfiguration(CustomConfig.class).run(context -> {
            assertThat(context).hasSingleBean(GlobalExceptionHandler.class);
            assertThat(context.getBean(GlobalExceptionHandler.class))
                    .isInstanceOf(CustomGlobalExceptionHandler.class);
        });
    }

    @Test
    @DisplayName("does not activate outside a servlet web application")
    void inactiveOutsideWebApp() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ExceptionHandlingAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(GlobalExceptionHandler.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomConfig {
        @Bean
        GlobalExceptionHandler customHandler() {
            return new CustomGlobalExceptionHandler();
        }
    }

    static class CustomGlobalExceptionHandler extends GlobalExceptionHandler {
    }
}
