package com.library.support.orrai.model;

import java.util.Objects;

/**
 * A single field-level validation failure, exposed under the {@code errors} property of the
 * error responses produced by the web adapters.
 *
 * <p>Plain immutable class (not a {@code record}) so it compiles on Java 11, the floor for the
 * shared {@code core} and {@code boot2} modules. Jackson serializes it via its getters.
 */
public final class ValidationError {

    private final String field;

    private final String message;

    public ValidationError(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public String getField() {
        return field;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ValidationError)) {
            return false;
        }
        ValidationError that = (ValidationError) o;
        return Objects.equals(field, that.field) && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, message);
    }

    @Override
    public String toString() {
        return "ValidationError{field='" + field + "', message='" + message + "'}";
    }
}
