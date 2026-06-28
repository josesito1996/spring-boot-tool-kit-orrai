package com.library.support.orrai.handler;

import com.library.support.orrai.exception.BusinessException;
import com.library.support.orrai.model.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

/**
 * Centralized REST exception handler producing RFC 7807 {@link ProblemDetail} responses.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} so Spring MVC's built-in exceptions
 * (malformed body, unsupported media type, missing parameters, {@code @Valid} failures…)
 * are rendered in the same {@code application/problem+json} format as the library's own
 * {@link BusinessException} hierarchy.
 *
 * <p>Registered automatically via {@code ExceptionHandlingAutoConfiguration}; consumers may
 * override it by declaring their own {@code GlobalExceptionHandler} bean. Ordered at
 * {@link Ordered#LOWEST_PRECEDENCE} so its catch-all never shadows more specific advices
 * (e.g. {@code ValidationExceptionHandler}).
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Handles any {@link BusinessException}, mapping its status and error code into the response.
     * When a cause is attached it is logged at error level (with stack trace) but never exposed.
     */
    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex, WebRequest request) {
        if (ex.getCause() != null) {
            log.error("Business exception [{}]: {}", ex.getErrorCode(), ex.getMessage(), ex.getCause());
        } else {
            log.warn("Business exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        }
        return ProblemDetails.create(ex.getStatusCode(), ex.getMessage(), ex.getErrorCode(), request);
    }

    /**
     * Customizes {@code @Valid} request-body validation failures into a 400 with field-level errors.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ValidationError(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        log.warn("Request validation failed: {} error(s)", errors.size());
        ProblemDetail problem =
                ProblemDetails.create(HttpStatus.BAD_REQUEST.value(), "Validation failed", "VALIDATION_ERROR", request);
        problem.setProperty("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Last-resort handler for unanticipated errors. Logs the full stack trace server-side and
     * returns a generic 500 without leaking internal details to the client.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);
        return ProblemDetails.create(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred", "UNEXPECTED_ERROR", request);
    }
}
