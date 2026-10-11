# Changelog

## 0.2.5

### Added
- `BusinessException.withProperty(String key, Object value)` and `getProperties()`: any business
  exception (subclasses included) can carry extra properties that the handlers add as top-level
  members of the error body. Removes the need for consumer subclasses plus a custom
  `@RestControllerAdvice` just to send a structured field.
  ```java
  throw new BusinessException(409, "DOCUMENTO_DUPLICADO", "...").withProperty("paciente_id", 42);
  ```
  - Keys are written verbatim (not affected by Jackson's naming strategy).
  - A `null` value leaves the key absent.
  - Reserved keys (`type`, `title`, `status`, `detail`, `instance`, `timestamp`, `errorCode`,
    `path`, `errors`, `message`) and null/blank keys throw `IllegalArgumentException`.
  - `getProperties()` is an unmodifiable, insertion-ordered view. The map is `transient`, so
    serializing the exception never fails because of a property value.
- Boot 3: `GlobalExceptionHandler.handleBusinessException` copies the properties into the
  `ProblemDetail`.
- Boot 2: `ApiError.withProperties(Map)` (returns a copy) and `getProperties()` exposed via
  `@JsonAnyGetter`; `handleBusinessException` uses it.

### Compatibility
- Purely additive. Without properties the error body is identical to 0.2.4, field by field.
