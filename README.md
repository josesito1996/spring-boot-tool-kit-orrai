# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A reusable commons **library** (no runnable app — there is **no `@SpringBootApplication`**; do not add one) providing two auto-configured feature families: global REST **exception handling** and a **data (JPA/JDBC) abstraction** layer (auditable base entities, soft-delete, base repositories, agnostic pagination, opinionated Hikari pool). It is a **multi-module Maven reactor** so it can serve both Spring Boot 2.x (`javax`) and Spring Boot 3.x (`jakarta`) consumers — a single JAR cannot, because the two use binary-incompatible namespaces (and Spring Boot 3's `ProblemDetail` does not exist in Spring 5).

Each feature follows the same shape: a framework-agnostic **core** module plus one **adapter per Boot version** (`boot2` = javax, `boot3` = jakarta). Consumers add only the adapters they need.

Published to **Maven Central** under groupId `io.github.josesito1996`:

| Module dir | artifactId | Target | Notes |
|-----------|-----------|--------|-------|
| (root) | `spring-boot-tool-kit-orrai-parent` | pom | reactor parent: modules, plugin mgmt, publishing |
| `core/` | `spring-boot-tool-kit-orrai-core` | Java 11, **no Spring** | shared `BusinessException` hierarchy + `ValidationError` |
| `boot3/` | `spring-boot-tool-kit-orrai` *(legacy name)* | Java 17, jakarta | RFC 7807 `ProblemDetail`, `@AutoConfiguration` |
| `boot2/` | `spring-boot-tool-kit-orrai-boot2` | Java 11, javax | custom `ApiError` DTO, `spring.factories` (Boot 2.5–2.7) |
| `data-core/` | `spring-boot-tool-kit-orrai-data-core` | Java 11, **no Spring** | pagination/sort DTOs, `DataSourcePoolProperties`, `CurrentAuditor` SPI |
| `data-boot3/` | `spring-boot-tool-kit-orrai-data-boot3` | Java 17, jakarta | JPA base entities, repos, auditing + Hikari `@AutoConfiguration` |
| `data-boot2/` | `spring-boot-tool-kit-orrai-data-boot2` | Java 11, javax | same, `spring.factories` (Boot 2.5–2.7) |

The Boot 3 exception module deliberately keeps the original published coordinate `spring-boot-tool-kit-orrai` so existing 3.x consumers are unaffected; the data adapters use explicit `-data-boot2`/`-data-boot3` coordinates.

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
./mvnw -pl data-boot2 -am test    # a data adapter + its data-core dependency
./mvnw clean install              # build + install all modules to local ~/.m2

# run a single test class or method (add -pl <module> to scope it)
./mvnw -pl boot3 test -Dtest=GlobalExceptionHandlerTest
./mvnw -pl boot2 test -Dtest=GlobalExceptionHandlerTest#resourceNotFound_returns404
```

## Publishing (Maven Central)

Publishing config lives in the **parent** `pom.xml` (`pluginManagement`): `central-publishing-maven-plugin` (`autoPublish=true`) + `maven-source-plugin`, `maven-javadoc-plugin`, `maven-gpg-plugin` (GPG signing, bound to `verify`); each publishable module enables them in its own `build/plugins`. A release requires GPG keys and Central credentials in `~/.m2/settings.xml` (server id `central`). Bump `<version>` in the parent (children inherit it) before releasing. `./mvnw clean deploy` signs and publishes all six publishable artifacts (the parent is `maven.deploy.skip=true`). For a **local** build without GPG keys, add `-Dgpg.skip=true`.

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

## Data abstraction modules (JPA + JDBC)

A second feature family, structured exactly like the exception one (agnostic `data-core` + a JPA adapter per Boot version). Package root `com.library.support.orrai.data`.

### data-core (framework-agnostic, no Spring)
- `page/` — immutable pagination DTOs: `PageQuery` (validated: page clamped `>= 0`, size clamped to `[1, MAX_PAGE_SIZE=200]`, defensively-copied sort), `PageResponse<T>` (content + derived `totalPages`/`first`/`last`/`empty`), `SortOrder` + `Direction`, `PageDefaults`. Lets app code paginate without depending on Spring Data.
- `datasource/DataSourcePoolProperties` — plain POJO of Hikari pool tuning with production defaults; the adapters bind `orrai.datasource.*` onto it via a thin `@ConfigurationProperties` subclass (no field duplication).
- `audit/CurrentAuditor` — functional SPI returning `Optional<String>` for the `@CreatedBy`/`@LastModifiedBy` value; default `SystemAuditor` returns `"system"`. Plug Spring Security, MDC, a header, etc., by declaring your own bean.

### data-boot3 (jakarta) / data-boot2 (javax)
Mirror modules differing only in the persistence namespace (`jakarta.persistence` vs `javax.persistence`), Hibernate version (6 vs 5), and auto-config registration (`AutoConfiguration.imports` + `@AutoConfiguration` vs `spring.factories` + `@Configuration`).
- `jpa/BaseEntity` — `@MappedSuperclass`, `Long` id (`IDENTITY`), identity-based `equals`/`hashCode` safe across the transient→managed transition (constant `hashCode`).
- `jpa/AuditableEntity extends BaseEntity` — `@EntityListeners(AuditingEntityListener)` with `createdAt`/`updatedAt` (`Instant`) and `createdBy`/`updatedBy` (`String`), filled automatically by Spring Data JPA auditing.
- `jpa/SoftDeletableEntity extends AuditableEntity` — `deleted` flag + `deletedAt`, with `markDeleted()`/`restore()`. **No `@Where`** (Hibernate ignores it on a `@MappedSuperclass` and it behaves inconsistently across Hibernate 5/6); filtering is explicit via the repository finders below.
- `repository/BaseRepository<T, ID>` (`@NoRepositoryBean extends JpaRepository`) and `repository/SoftDeleteRepository<T extends SoftDeletableEntity, ID>` adding `softDelete(entity)` plus derived active-row finders `findAllByDeletedFalse()` (+ `Pageable` overload), `findByIdAndDeletedFalse(id)`, `countByDeletedFalse()`. Inherited `findAll`/`findById` still see **all** rows (including deleted).
- `support/PageMappers` — bridges `PageQuery`/`PageResponse` ⇄ Spring Data `Pageable`/`Page`.
- `autoconfigure/` — `OrraiJpaAuditingAutoConfiguration` (`@EnableJpaAuditing`, on by default, disable with `orrai.jpa.auditing.enabled=false`; wires an `AuditorAware<String>` over `CurrentAuditor`) and `OrraiDataSourceAutoConfiguration` (**opt-in**: builds a tuned `HikariDataSource` only when `orrai.datasource.url` is set, and only `@ConditionalOnMissingBean(DataSource.class)`, so it never fights Spring Boot's own `spring.datasource.*` config).

### Consumer usage
Add the adapter matching your Boot version (it pulls `data-core` transitively):

```xml
<dependency>
  <groupId>io.github.josesito1996</groupId>
  <artifactId>spring-boot-tool-kit-orrai-data-boot2</artifactId>  <!-- or -data-boot3 -->
  <version>0.1.0</version>
</dependency>
```

```java
@Entity
public class Cliente extends AuditableEntity {        // gets id + auditing for free
    private String nombre;
    // getters/setters
}

public interface ClienteRepository extends BaseRepository<Cliente, Long> {}
// or, for soft-delete: extends SoftDeleteRepository<Cliente, Long>
```

Auditing is active out of the box (`createdAt`/`createdBy` filled on insert, `"system"` until you provide a `CurrentAuditor`). Optional properties:

```properties
# Opinionated Hikari pool — activates ONLY when a url is present
orrai.datasource.url=jdbc:postgresql://localhost:5432/app
orrai.datasource.username=app
orrai.datasource.password=secret
orrai.datasource.maximum-pool-size=10
orrai.datasource.minimum-idle=5
# Disable our auditing if the app declares its own @EnableJpaAuditing
orrai.jpa.auditing.enabled=false
```

Note: `spring-boot-starter-data-jpa` is an **optional** dependency of the adapters (not propagated transitively) — your JPA app already provides it.

## Testing approach

JUnit 5 with `@DisplayName`. `core` has plain unit tests (no Spring). Each adapter's `GlobalExceptionHandlerTest` drives the advice through **MockMvc standalone** against an inner `TestController`, asserting status + error-body JSON (incl. a regression test that the validation advice wins over the catch-all despite registration order). `ExceptionHandlingAutoConfigurationTest` uses `WebApplicationContextRunner`/`ApplicationContextRunner` for bean registration, `@ConditionalOnMissingBean` back-off, and non-web inactivity. Surefire/compiler/JUnit versions are pinned in the parent `pom.xml` (no `spring-boot-starter-parent` to supply them).

For the data modules: `data-core` has plain JUnit 5 unit tests (`PageQueryTest`, `PageResponseTest` — clamping, offset, derived page flags, immutability). Each data adapter has a `@DataJpaTest` (`AuditingAndSoftDeleteTest`) running against **H2**, with a `TestDataApplication` (`@SpringBootApplication`, test-only) to bootstrap the slice and a `sample/` fixture entity+repository; it asserts that auditing fills `createdAt`/`createdBy` on insert and that `softDelete` hides rows from the active finders while the row is physically retained and flagged. Test classes must be named `*Test` (Surefire only; no Failsafe configured).
