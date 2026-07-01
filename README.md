# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A reusable commons **library** (no runnable app — there is **no `@SpringBootApplication`**; do not add one) providing two auto-configured feature families: global REST **exception handling** and a **data (JPA/JDBC) abstraction** layer (auditable base entities, soft-delete, base repositories, agnostic pagination, opinionated Hikari pool). It is a **multi-module Maven reactor** so it can serve both Spring Boot 2.x (`javax`) and Spring Boot 3.x (`jakarta`) consumers — a single JAR cannot, because the two use binary-incompatible namespaces (and Spring Boot 3's `ProblemDetail` does not exist in Spring 5).

Each feature follows the same shape: a framework-agnostic **core** module plus one **adapter per Boot version** (`boot2` = javax, `boot3` = jakarta). Consumers add only the adapters they need.

Published to **Maven Central** under groupId `io.github.josesito1996`:

| Module dir | artifactId | Target | Notes |
|-----------|-----------|--------|-------|
| (root) | `spring-boot-tool-kit-orrai-parent` | pom | reactor parent: modules, plugin mgmt, publishing |
| `core/` | `spring-boot-tool-kit-orrai-core` | Java 11, **no Spring** | shared `BusinessException` hierarchy + `ErrorCode` catalog + `ValidationError` |
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
- `exception/BusinessException` — base `RuntimeException` carrying the HTTP status as a plain **`int`** plus a machine-readable `errorCode`. Constructors `(int, String, String)` and `(…, Throwable cause)`, plus `ErrorCode`-based overloads `(ErrorCode)`, `(ErrorCode, message)`, `(ErrorCode, message, cause)` that derive the status/code/default-message from the enum. One handler covers all via the base type.
- `exception/ErrorCode` — enum catalog of the common HTTP errors (400, 401, 403, 404, 500, 502, 503), each bundling its numeric status (via `java.net.HttpURLConnection.HTTP_*` constants — keeps core Spring-free), a stable `code`, and a client-safe `defaultMessage`. Single source of truth for the status/code/message triple.
- Subclasses fix status+code by delegating to an `ErrorCode`: `ResourceNotFoundException` (404, `NOT_FOUND`), `UnauthorizedException` (401), `ForbiddenException` (403), `InternalErrorException` (500). 404 keeps the published `RESOURCE_NOT_FOUND` code.
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
Add a class in **`core`** `exception/` extending `BusinessException`. If the status is one of the cataloged ones, add/reuse an `ErrorCode` constant and delegate via `super(ErrorCode.X, message)`; otherwise pass a raw status `int` + `errorCode` to `super(...)`. **No handler change** in either adapter — `handleBusinessException` covers all subclasses. Add a unit test in `core` (`BusinessExceptionTest`, plus `ErrorCodeTest` for new constants) and, if asserting the HTTP response, a case in the relevant adapter's `GlobalExceptionHandlerTest`.

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
- `jpa/AuditableFields` — same audit columns as `AuditableEntity` but **without `@Id`** (does not extend `BaseEntity`). Use it on entities that already own their primary key, to add auditing without the duplicate-identifier conflict that arises when an existing `@Id` collides with `BaseEntity`'s.
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

If your entity already declares its own `@Id` (e.g. a legacy table), extend `AuditableFields` instead of `AuditableEntity` to get auditing without the id conflict:

```java
@Entity
public class Cliente extends AuditableFields {       // auditing only; you own the id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                                  // any name/type/strategy
    private String nombre;
    // getters/setters
}
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

**Pagination without depending on Spring Data** — accept/return the agnostic `PageQuery`/`PageResponse` DTOs and bridge them with `PageMappers`:

```java
@Service
public class ClienteService {

    private final ClienteRepository repository;   // extends SoftDeleteRepository<Cliente, Long>

    ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    // query = PageQuery.of(0, 20) or PageQuery.of(0, 20, List.of(new SortOrder("nombre", Direction.ASC)))
    public PageResponse<Cliente> listActive(PageQuery query) {
        Pageable pageable = PageMappers.toPageable(query);
        Page<Cliente> page = repository.findAllByDeletedFalse(pageable);  // hides soft-deleted rows
        return PageMappers.toPageResponse(page);   // content + totalPages/first/last/empty
    }

    public Cliente softDelete(Long id) {
        Cliente cliente = repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente " + id));
        repository.softDelete(cliente);   // flags deleted=true + deletedAt; row physically retained
        return cliente;
    }
}
```

`PageResponse<T>` is a plain immutable DTO (`content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`, `empty`), safe to return directly from a controller as JSON without leaking Spring Data types.

Note: `spring-boot-starter-data-jpa` is an **optional** dependency of the adapters (not propagated transitively) — your JPA app already provides it.

## Outbound HTTP client (boot3)

A third feature family: an auto-configured, multi-client HTTP consumer. Framework-agnostic **port** in `core` (`http/OrraiHttpClient` — pure `execute(...)` contract, no Spring), Spring Boot 3 **adapter** in `boot3` backed by `RestClient`. All `4xx`/`5xx` and transport failures map centrally to `core` `exception/OrraiHttpClientException` (the transport never leaks to the consumer).

- `http/RestClientOrraiHttpClient` — `RestClient` adapter; `http/RetryingOrraiHttpClient` + `RetryPolicy` — transport-agnostic retry decorator (fixed delay, retryable status codes, optional transport-error retry).
- `http/OrraiHttpClientFactory` — builds a per-client `RestClient` (own base URL + connect/read timeouts) using the Spring Boot 4 **`org.springframework.boot.http.client`** API (`HttpClientSettings.defaults()` + `ClientHttpRequestFactoryBuilder.detect()`). **`boot3` now targets Spring Boot 4.0.0** (`spring-boot.version=4.0.0`, Java 21); the other modules (`boot2`, `data-*`, `core`) keep their previous Boot versions and stay compatible. The migration follows Boot 4's package moves: `RestClientAutoConfiguration` → `org.springframework.boot.restclient.autoconfigure` (`spring-boot-restclient`), `WebMvcAutoConfiguration` → `org.springframework.boot.webmvc.autoconfigure`, and the old `ClientHttpRequestFactorySettings` → `HttpClientSettings` (`spring-boot-http-client`). Both `spring-boot-restclient` and `spring-boot-http-client` are added as **optional** dependencies.
- `http/OrraiHttpClientRegistry` — resolves clients by name (`registry.client("payments")`); the default client is also the `@Primary OrraiHttpClient` bean. `autoconfigure/OrraiHttpAutoConfiguration` binds `orrai.http.*` and registers everything.

### Consumer usage

**1. Add the dependency** (Spring Boot 4; brings `core` transitively):

```xml
<dependency>
  <groupId>io.github.josesito1996</groupId>
  <artifactId>spring-boot-tool-kit-orrai</artifactId>
  <version>0.2.4</version>
</dependency>
```

**2. Configure clients** in `application.properties` — a default one and any number of named ones:

```properties
# Default / primary client -> injectable directly as OrraiHttpClient
orrai.http.client.base-url=https://api.example.com
orrai.http.client.connect-timeout=2s
orrai.http.client.read-timeout=5s
orrai.http.client.max-retries=3
orrai.http.client.retry-delay=500ms

# Named clients -> resolved via OrraiHttpClientRegistry.client("<name>")
orrai.http.clients.google-maps-elevation.base-url=https://maps.googleapis.com/maps/api/elevation
orrai.http.clients.google-maps-elevation.read-timeout=10s
orrai.http.clients.google-maps-geocode.base-url=https://maps.googleapis.com/maps/api/geocode
```

**3a. Inject the default client directly** (it is the `@Primary OrraiHttpClient` bean):

```java
@Service
public class UserService {

    private final OrraiHttpClient http;

    UserService(OrraiHttpClient http) {   // the default client
        this.http = http;
    }

    public UserDto findUser(long id) {
        // GET https://api.example.com/users/{id}  ->  deserialized into UserDto
        return http.execute(
                "/users/" + id,   // path (appended to base-url)
                "GET",            // method (case-insensitive)
                null,             // headers   (Map<String,String> or null)
                null,             // queryParams (Map<String,String> or null)
                null,             // body      (null for GET)
                UserDto.class);   // response type
    }
}
```

**3b. Resolve named clients** through the registry when you talk to several downstreams:

```java
@Service
public class GoogleMapsServiceV2 {

    private final OrraiHttpClient elevation;
    private final OrraiHttpClient geocode;

    GoogleMapsServiceV2(OrraiHttpClientRegistry registry) {
        this.elevation = registry.client("google-maps-elevation");
        this.geocode   = registry.client("google-maps-geocode");
    }

    public GeocodeResponse geocode(String address) {
        return geocode.execute(
                "/json", "GET",
                Map.of("Accept", "application/json"),          // headers
                Map.of("address", address, "key", apiKey),     // query params
                null,
                GeocodeResponse.class);
    }

    public ElevationResponse elevation(String locations) {
        return elevation.execute("/json", "GET", null,
                Map.of("locations", locations, "key", apiKey),
                null, ElevationResponse.class);
    }
}
```

**3c. Collections / generic responses** — a `Class<R>` cannot express a generic type like `List<UserDto>` (type erasure). Use the second `execute` overload with an `OrraiTypeRef<R>` super type token, created as an **anonymous subclass** so the generic argument is retained:

```java
List<UserDto> users = http.execute(
        "/users", "GET", null, null, null,
        new OrraiTypeRef<List<UserDto>>() {});   // note the trailing {} — anonymous subclass
```

`OrraiTypeRef` lives in the Spring-free `core`; the `RestClient` adapter translates it to Spring's `ParameterizedTypeReference` internally. It works the same through the `@Primary` client, the registry, and with retries enabled. For a flat array you can still use the simpler `Class` form (`UserDto[].class`) and wrap with `List.of(...)`.

**4. Error handling** — every `4xx`/`5xx` or transport failure surfaces as a single `OrraiHttpClientException` (never a `RestClient`/transport exception). It carries a **snapshot of the error response** — status, path, raw body and headers — captured while the transport stream was still open, so you can process the error response *after* the exchange has closed:

```java
try {
    return userService.findUser(id);
} catch (OrraiHttpClientException ex) {
    int status = ex.getUpstreamStatusCode();                       // e.g. 404 (0 = transport failure)
    String body = ex.getResponseBody();                            // raw error body (may be null)
    Map<String, List<String>> headers = ex.getResponseHeaders();   // unmodifiable, never null

    List<String> retryAfter  = headers.get("Retry-After");         // null if absent
    List<String> contentType = headers.get("Content-Type");

    log.warn("Upstream failed: status={} path={} contentType={} body={}",
            status, ex.getRequestPath(), contentType, body);
    throw new ResourceNotFoundException("User " + id + " not available");
}
```

> **Why a snapshot, not the live `ClientHttpResponse`?** The transport's `ClientHttpResponse` (and its body `InputStream`) is closed as soon as the exchange completes, so it cannot be safely retained past the failure. The adapter therefore reads status + headers + body at error time and exposes them on the exception. **Transport-level failures** (connection/timeout — no response received) map to `getUpstreamStatusCode() == 0`, `getResponseBody() == null` and an empty `getResponseHeaders()`.

The single `OrraiHttpClientException` distinguishes three failure modes so the message is always accurate:

| Failure | Trigger | `getUpstreamStatusCode()` | Message |
|---------|---------|---------------------------|---------|
| **Upstream error** | Response arrived with a `4xx`/`5xx` status | the real status (e.g. `404`) | `Upstream call to '<path>' failed with status <n>` |
| **Transport error** | Could not reach/read the server (connection refused, timeout) — Spring `ResourceAccessException` | `0` | `Unable to reach upstream service at '<path>'` |
| **Deserialization error** | Response arrived `2xx` but its body could not be mapped into the requested type — Jackson `MismatchedInputException` (JSON shape ≠ target type) | `0` | `Response from '<path>' could not be deserialized into the requested type` |

If you hit the **deserialization** case, the fix is on the consumer side: make the target type match the actual JSON — use `new OrraiTypeRef<List<Foo>>() {}` (or `Foo[].class`) when the payload is an array, and add `@JsonIgnoreProperties(ignoreUnknown = true)` to tolerate extra fields. The original Jackson error is preserved as the exception's `cause`.

Retries (when `max-retries > 0`) are applied automatically by the `RetryingOrraiHttpClient` decorator for the configured `retryable-status-codes` (default `408,429,500,502,503,504`) and, when `retry-on-transport-error=true`, for connection/timeout failures.

## Testing approach

JUnit 5 with `@DisplayName`. `core` has plain unit tests (no Spring). Each adapter's `GlobalExceptionHandlerTest` drives the advice through **MockMvc standalone** against an inner `TestController`, asserting status + error-body JSON (incl. a regression test that the validation advice wins over the catch-all despite registration order). `ExceptionHandlingAutoConfigurationTest` uses `WebApplicationContextRunner`/`ApplicationContextRunner` for bean registration, `@ConditionalOnMissingBean` back-off, and non-web inactivity. Surefire/compiler/JUnit versions are pinned in the parent `pom.xml` (no `spring-boot-starter-parent` to supply them).

For the data modules: `data-core` has plain JUnit 5 unit tests (`PageQueryTest`, `PageResponseTest` — clamping, offset, derived page flags, immutability). Each data adapter has a `@DataJpaTest` (`AuditingAndSoftDeleteTest`) running against **H2**, with a `TestDataApplication` (`@SpringBootApplication`, test-only) to bootstrap the slice and a `sample/` fixture entity+repository; it asserts that auditing fills `createdAt`/`createdBy` on insert and that `softDelete` hides rows from the active finders while the row is physically retained and flagged. A second `@DataJpaTest` (`AuditableFieldsTest`, with a `CustomIdAuditableEntity` fixture that declares its own `@Id`) verifies that `AuditableFields` adds auditing to an entity owning its primary key — no duplicate-id conflict — and that `updatedAt` advances on update while `createdAt` is preserved. Test classes must be named `*Test` (Surefire only; no Failsafe configured).
