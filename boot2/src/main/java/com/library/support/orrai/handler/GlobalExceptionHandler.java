package com.library.support.orrai.handler;

import com.library.support.orrai.exception.BusinessException;
import com.library.support.orrai.model.ApiError;
import com.library.support.orrai.model.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralized REST exception handler for the Spring Boot 2 (javax / Spring Framework 5) adapter,
 * producing {@link ApiError} responses.
 *
 * <p>Extends {@link ResponseEntityExceptionHandler} (Spring 5 signatures) so Spring MVC's
 * built-in exceptions are rendered in the same format as the library's {@link BusinessException}
 * hierarchy. Registered via {@code ExceptionHandlingAutoConfiguration} (spring.factories);
 * consumers may override it with their own bean. Ordered at {@link Ordered#LOWEST_PRECEDENCE}
 * so its catch-all never shadows more specific advices.
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException ex, WebRequest request) {
        if (ex.getCause() != null) {
            log.error("Business exception [{}]: {}", ex.getErrorCode(), ex.getMessage(), ex.getCause());
        } else {
            log.warn("Business exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        }
        ApiError body = ApiErrorSupport.create(ex.getStatusCode(), ex.getMessage(), ex.getErrorCode(), request);
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ValidationError(fieldError.getField(), fieldError.getDefaultMessage()))
                .collect(Collectors.toList());
        log.warn("Request validation failed: {} error(s)", errors.size());
        ApiError body = ApiErrorSupport.create(
                HttpStatus.BAD_REQUEST.value(), "Validation failed", "VALIDATION_ERROR", request, errors);
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);
        ApiError body = ApiErrorSupport.create(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred", "UNEXPECTED_ERROR", request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
