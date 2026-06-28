package com.library.support.orrai.handler;

import com.library.support.orrai.model.ApiError;
import com.library.support.orrai.model.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles {@link ConstraintViolationException} ({@code @Validated} method/parameter validation)
 * into a 400 {@link ApiError} with field-level errors, for the Spring Boot 2 (javax) adapter.
 *
 * <p>Isolated from {@code GlobalExceptionHandler} because {@code javax.validation} is an optional
 * dependency: this advice is only registered when Bean Validation is on the classpath. Ordered
 * ahead of {@code GlobalExceptionHandler} so its specific handler wins over that catch-all.
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class ValidationExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        List<ValidationError> errors = ex.getConstraintViolations().stream()
                .map(this::toValidationError)
                .collect(Collectors.toList());
        log.warn("Constraint violation: {} error(s)", errors.size());
        ApiError body = ApiErrorSupport.create(
                HttpStatus.BAD_REQUEST.value(), "Validation failed", "VALIDATION_ERROR", request, errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private ValidationError toValidationError(ConstraintViolation<?> violation) {
        return new ValidationError(lastNode(violation.getPropertyPath()), violation.getMessage());
    }

    /**
     * Returns only the final property-path node (the field/parameter name), discarding the
     * class and method nodes that {@code @Validated} adds at the service layer.
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
