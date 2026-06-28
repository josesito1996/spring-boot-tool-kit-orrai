# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A reusable commons **library** (no runnable app — there is **no `@SpringBootApplication`**; do not add one) providing auto-configured global REST exception handling. It is a **multi-module Maven reactor** so it can serve both Spring Boot 2.x (`javax`) and Spring Boot 3.x (`jakarta`) consumers — a single JAR cannot, because the two use binary-incompatible namespaces and Spring Boot 3's `ProblemDetail` does not exist in Spring 5.

Published to **Maven Central** under groupId `io.github.josesito1996`:

| Module dir | artifactId | Target | Notes |
|-----------|-----------|--------|-------|
| (root) | `spring-boot-tool-kit-orrai-parent` | pom | reactor parent: modules, plugin mgmt, publishing |
| `core/` | `spring-boot-tool-kit-orrai-core` | Java 11, **no Spring** | shared `BusinessException` hierarchy + `ValidationError` |
| `boot3/` | `spring-boot-tool-kit-orrai` *(legacy name)* | Java 17, jakarta | RFC 7807 `ProblemDetail`, `@AutoConfiguration` |
| `boot2/` | `spring-boot-tool-kit-orrai-boot2` | Java 11, javax | custom `ApiError` DTO, `spring.factories` (Boot 2.5–2.7) |

The Boot 3 module deliberately keeps the original published coordinate `spring-boot-tool-kit-orrai` so existing 3.x consumers are unaffected.

## Build & test commands

Uses the Maven Wrapper (`./mvnw`, or `mvnw.cmd` on Windows).

**JDK requirement:** the build needs **JDK 17** (it targets release 11 for core/boot2 and 17 for boot3 — one JDK 17 compiles all). On this machine the default JDK Maven picks up is older and fails with `error: release version 17 not supported`. JDK 17 lives at `C:\Desarrollo\Java\java17` (GraalVM CE 17.0.8); set `JAVA_HOME` before running Maven:

```bash
export JAVA_HOME="C:/Desarrollo/Java/java17"   # then run any ./mvnw command below
```

```bash
./mvnw clean test                 # build + test the whole reactor (all modules)
./mvnw -pl core test              # one module (-am to also build its dependencies)
./mvnw -pl boot3 -am test         # boot3 + its core dependency
./mvnw clean install              # build + install all modules to local ~/.m2

# run a single test class or method (add -pl <module> to scope it)
./mvnw -pl boot3 test -Dtest=GlobalExceptionHandlerTest
./mvnw -pl boot2 test -Dtest=GlobalExceptionHandlerTest#resourceNotFound_returns404
```

## Publishing (Maven Central)

Publishing config lives in the **parent** `pom.xml` (`pluginManagement`): `central-publishing-maven-plugin` (`autoPublish=true`) + `maven-source-plugin`, `maven-javadoc-plugin`, `maven-gpg-plugin` (GPG signing, bound to `verify`); each publishable module enables them in its own `build/plugins`. A release requires GPG keys and Central credentials in `~/.m2/settings.xml` (server id `central`). Bump `<version>` in the parent (children inherit it) before releasing. `./mvnw clean deploy` signs and publishes all three artifacts.

## Architecture

Package root across all modules: `com.library.support.orrai` (differs from the Maven `groupId`).

### core (framework-agnostic)
- `exception/BusinessException` — base `RuntimeException` carrying the HTTP status as a plain **`int`** (via `java.net.HttpURLConnection.HTTP_*` constants — keeps core Spring-free) plus a machine-readable `errorCode`. Constructors `(int, String, String)` and `(…, Throwable cause)`. Subclasses fix status+code: `ResourceNotFoundException` (404), `UnauthorizedException` (401), `ForbiddenException` (403), `InternalErrorException` (500). One handler covers all via the base type.
- `model/ValidationError` — plain immutable class (not a `record` — Java 11 floor) for per-field validation errors.

### boot3 (jakarta, Spring Boot 3)
- `handler/GlobalExceptionHandler` — `@RestControllerAdvice extends ResponseEntityExceptionHandler`, `@Order(LOWEST_PRECEDENCE)`. Maps `BusinessException` → RFC 7807 `ProblemDetail`, customizes `MethodArgumentNotValidException` (400 + field errors), and a catch-all `Exception` (500 `UNEXPECTED_ERROR`, logs full stack, no leak).
- `handler/ValidationExceptionHandler` — separate advice for `jakarta.validation.ConstraintViolationException`, `@Order(LOWEST_PRECEDENCE - 10)` so it wins over the catch-all. **Isolated** because validation is optional (avoids `NoClassDefFoundError`).
- `handler/ProblemDetails` — package-private helper building `ProblemDetail` (extension props `timestamp` as ISO string, `errorCode`, `path` via `ServletWebRequest`); takes status as `int`.
- `autoconfigure/ExceptionHandlingAutoConfiguration` — `@AutoConfiguration` (servlet only) registering handlers as `@ConditionalOnMissingBean`; validation bean `@ConditionalOnClass`. Hint file: `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

### boot2 (javax, Spring Boot 2.5–2.7)
Mirror of boot3 with Spring 5 idioms: `model/ApiError` DTO instead of `ProblemDetail`; `handler/ApiErrorSupport` builds it; handlers use the Spring 5 `handleMethodArgumentNotValid(…, HttpStatus, …)` signature and `javax.validation`; auto-config is a plain `@Configuration` registered via `META-INF/spring.factories` (`EnableAutoConfiguration`) for 2.5–2.7 reach.

How the pieces connect: a consumer picks the module matching its Boot version → auto-config registers the advices → throwing any `BusinessException` (or hitting a Spring MVC error) yields a consistent error body with the matching status.

### Adding a new handled exception (the common change)
Add a class in **`core`** `exception/` extending `BusinessException`, passing its status `int` + `errorCode` to `super(...)`. **No handler change** in either adapter — `handleBusinessException` covers all subclasses. Add a unit test in `core` (`BusinessExceptionTest`) and, if asserting the HTTP response, a case in the relevant adapter's `GlobalExceptionHandlerTest`.

### Consumer integration note
Both adapters are **auto-configured** — consumers get the handlers just by adding the dependency, no component scanning. Beans are `@ConditionalOnMissingBean` (override by declaring your own). Handling activates only in a servlet web application; the constraint-violation handler only when Bean Validation is on the classpath.

## Testing approach

JUnit 5 with `@DisplayName`. `core` has plain unit tests (no Spring). Each adapter's `GlobalExceptionHandlerTest` drives the advice through **MockMvc standalone** against an inner `TestController`, asserting status + error-body JSON (incl. a regression test that the validation advice wins over the catch-all despite registration order). `ExceptionHandlingAutoConfigurationTest` uses `WebApplicationContextRunner`/`ApplicationContextRunner` for bean registration, `@ConditionalOnMissingBean` back-off, and non-web inactivity. Surefire/compiler/JUnit versions are pinned in the parent `pom.xml` (no `spring-boot-starter-parent` to supply them).
