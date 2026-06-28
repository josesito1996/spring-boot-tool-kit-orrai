package com.library.support.orrai.handler;

import com.library.support.orrai.model.ValidationError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

/**
 * Handles {@link ConstraintViolationException} (raised by {@code @Validated} method/parameter
 * validation) into an RFC 7807 400 response with field-level errors.
 *
 * <p>Isolated from {@code GlobalExceptionHandler} because {@code jakarta.validation} is an
 * optional dependency: this advice is only registered when Bean Validation is on the classpath
 * (see {@code ExceptionHandlingAutoConfiguration}), avoiding {@code NoClassDefFoundError} when
 * it is absent.
 *
 * <p>Ordered ahead of {@code GlobalExceptionHandler} so its specific
 * {@link ConstraintViolationException} handler wins over that advice's catch-all.
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class ValidationExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        List<ValidationError> errors = ex.getConstraintViolations().stream()
                .map(this::toValidationError)
                .toList();
        log.warn("Constraint violation: {} error(s)", errors.size());
        ProblemDetail problem =
                ProblemDetails.create(HttpStatus.BAD_REQUEST.value(), "Validation failed", "VALIDATION_ERROR", request);
        problem.setProperty("errors", errors);
        return problem;
    }

    private ValidationError toValidationError(ConstraintViolation<?> violation) {
        return new ValidationError(lastNode(violation.getPropertyPath()), violation.getMessage());
    }

    /**
     * Returns only the final property-path node (the field/parameter name), discarding the
     * class and method nodes that {@code @Validated} adds at the service layer — those would
     * otherwise disclose internal type and method names to the caller.
     */
    private String lastNode(Path propertyPath) {
        if (propertyPath == null) {
            return "";
        }
        String field = "";
        for (Path.Node node : propertyPath) {
            if (node.getName() != null) {
                field = node.getName();
            }
        }
        return field;
    }
}
