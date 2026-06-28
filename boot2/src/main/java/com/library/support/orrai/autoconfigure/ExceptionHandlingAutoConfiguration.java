package com.library.support.orrai.autoconfigure;

import com.library.support.orrai.handler.GlobalExceptionHandler;
import com.library.support.orrai.handler.ValidationExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration registering the library's exception handlers in any servlet web
 * application. Spring Boot 2 (javax) adapter: registered via {@code META-INF/spring.factories}
 * ({@code EnableAutoConfiguration}) for broad compatibility with Boot 2.5–2.7 (the
 * {@code AutoConfiguration.imports} mechanism is 2.7+ only).
 *
 * <p>Every bean is {@link ConditionalOnMissingBean} so a consuming service can override it.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ExceptionHandlingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnClass(name = "javax.validation.ConstraintViolationException")
    @ConditionalOnMissingBean
    public ValidationExceptionHandler validationExceptionHandler() {
        return new ValidationExceptionHandler();
    }
}
