package com.library.support.orrai.autoconfigure;

import com.library.support.orrai.handler.GlobalExceptionHandler;
import com.library.support.orrai.handler.ValidationExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that registers the library's exception handlers in any servlet web
 * application. Consumers get consistent RFC 7807 error responses simply by adding this
 * dependency — no component scanning required.
 *
 * <p>Every bean is guarded with {@link ConditionalOnMissingBean} so a consuming service can
 * supply its own handler to override the default.
 */
@AutoConfiguration
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ExceptionHandlingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnClass(name = "jakarta.validation.ConstraintViolationException")
    @ConditionalOnMissingBean
    public ValidationExceptionHandler validationExceptionHandler() {
        return new ValidationExceptionHandler();
    }
}
