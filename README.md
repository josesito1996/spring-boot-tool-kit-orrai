# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A reusable commons **library** (no runnable app — there is **no `@SpringBootApplication`**; do not add one) providing several auto-configured feature families: global REST **exception handling**, an outbound **HTTP client**, a **data (JPA/JDBC) abstraction** layer (auditable base entities, soft-delete, base repositories, agnostic pagination, opinionated Hikari pool), and **HashiCorp Vault** secret custody. It is a **multi-module Maven reactor** so it can serve both Spring Boot 2.x (`javax`) and Spring Boot 3.x (`jakarta`) consumers — a single JAR cannot, because the two use binary-incompatible namespaces (and Spring Boot 3's `ProblemDetail` does not exist in Spring 5).

Each feature follows the same shape: a framework-agnostic **core** module plus one **adapter per Boot version** (`boot2` = javax, `boot3` = jakarta). Consumers add only the adapters they need.

Published to **Maven Central** under groupId `io.github.josesito1996`:

| Module dir | artifactId | Target | Notes |
|-----------|-----------|--------|-------|
| (root) | `spring-boot-tool-kit-orrai-parent` | pom | reactor parent: modules, plugin mgmt, publishing |
| `core/` | `spring-boot-tool-kit-orrai-core` | Java 11, **no Spring** | shared `BusinessException` hierarchy + `ErrorCode` catalog + `ValidationError` |
| `boot3/` | `spring-boot-tool-kit-orrai` *(legacy name)* | **Java 21, Spring Boot 4**, jakarta | RFC 7807 `ProblemDetail` + outbound HTTP client, `@AutoConfiguration` |
| `boot2/` | `spring-boot-tool-kit-orrai-boot2` | Java 11, javax | custom `ApiError` DTO, `spring.factories` (Boot 2.5–2.7) |
| `data-core/` | `spring-boot-tool-kit-orrai-data-core` | Java 11, **no Spring** | pagination/sort DTOs, `DataSourcePoolProperties`, `CurrentAuditor` SPI |
| `data-boot3/` | `spring-boot-tool-kit-orrai-data-boot3` | Java 17, jakarta | JPA base entities, repos, auditing + Hikari `@AutoConfiguration` |
| `data-boot2/` | `spring-boot-tool-kit-orrai-data-boot2` | Java 11, javax | same, `spring.factories` (Boot 2.5–2.7) |
| `vault/` | `spring-boot-tool-kit-orrai-vault` | **Java 21**, jakarta | zero-config HashiCorp Vault secret custody via spring-cloud-vault (`EnvironmentPostProcessor`) |
| `config-client/` | `spring-boot-tool-kit-orrai-config-client` | **Java 21**, jakarta | zero-config Spring Cloud Config Server client via spring-cloud-config (`EnvironmentPostProcessor`) |

The Boot 3 exception module deliberately keeps the original published coordinate `spring-boot-tool-kit-orrai` so existing 3.x consumers are unaffected; the data adapters use explicit `-data-boot2`/`-data-boot3` coordinates.

### Version & JDK compatibility

| Module(s) | Spring Boot | Spring Cloud | JDK release | Namespace |
|-----------|-------------|--------------|-------------|-----------|
| `core`, `boot2`, `data-boot2` | 2.5–2.7 | — | 11 | javax |
| `data-boot3` | 3.x | — | 17 | jakarta |
| `boot3` | **4.0.0** | — | **21** | jakarta |
| `vault` | **4.0.x** | **2025.1.x** (Oakwood, spring-cloud-vault 5.0.x) | **21** | jakarta |
| `config-client` | **4.0.x** | **2025.1.x** (Oakwood, spring-cloud-config-client 5.0.x) | **21** | jakarta |

Two things to keep straight:

1. **Build runtime = JDK 21 for the whole reactor.** Because `boot3` and `vault` target release 21, JDK 21 is now the minimum to build *any* module; it cross-compiles the older release 11/17 modules transparently. An older JDK fails at `boot3` with `error: release version 21 not supported`.
2. **`vault` targets Spring Boot 4.0.x via the Spring Cloud 2025.1.x (Oakwood) train** = `spring-cloud-vault` **5.0.x** on Spring Framework 7 / Boot 4 — matching the Boot 4.0+ consumers this library serves. The `spring-cloud-vault` version comes from the imported BOM, never pinned by hand. ⚠️ **Do not use Spring Cloud 2025.0.x** — it is incompatible with Boot 4.0.x. (Spring Cloud 2023.0.x/2024.0.x are the Boot 3.2–3.5 trains; only 2025.1.x supports Boot 4.)

## Build & test commands

Uses the Maven Wrapper (`./mvnw`, or `mvnw.cmd` on Windows).

**JDK requirement:** the build now needs **JDK 21**. Since `boot3` migrated to **Spring Boot 4 / Java 21** (and `vault` is Java 21), the reactor's highest release is 21; a single JDK 21 compiles everything (it cross-compiles release 11 for core/boot2 and 17 for data-boot3). An older JDK fails at `boot3` with `error: release version 21 not supported`. JDK 21 lives at `C:\Desarrollo\Java\java21` (OpenJDK 21.0.7); set `JAVA_HOME` before running Maven:

```bash
export JAVA_HOME="C:/Desarrollo/Java/java21"   # then run any ./mvnw command below
```

```bash
./mvnw clean test                 # build + test the whole reactor (all modules)
./mvnw -pl core test              # one module (-am to also build its dependencies)
./mvnw -pl boot3 -am test         # boot3 + its core dependency
./mvnw -pl data-boot2 -am test    # a data adapter + its data-core dependency
./mvnw -pl vault test             # the Vault module (self-contained, no reactor deps)
./mvnw clean install              # build + install all modules to local ~/.m2

# run a single test class or method (add -pl <module> to scope it)
./mvnw -pl boot3 test -Dtest=GlobalExceptionHandlerTest
./mvnw -pl boot2 test -Dtest=GlobalExceptionHandlerTest#resourceNotFound_returns404
```

## Publishing (Maven Central)

Publishing config lives in the **parent** `pom.xml` (`pluginManagement`): `central-publishing-maven-plugin` (`autoPublish=true`) + `maven-source-plugin`, `maven-javadoc-plugin`, `maven-gpg-plugin` (GPG signing, bound to `verify`); each publishable module enables them in its own `build/plugins`. A release requires GPG keys and Central credentials in `~/.m2/settings.xml` (server id `central`). Bump `<version>` in the parent (children inherit it) before releasing. `./mvnw clean deploy` signs and publishes all seven publishable artifacts (the parent is `maven.deploy.skip=true`). For a **local** build without GPG keys, add `-Dgpg.skip=true`.

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

### Consumer usage
Both adapters are **auto-configured** — add the dependency and the handlers register themselves (no component scanning). Beans are `@ConditionalOnMissingBean` (override by declaring your own). Handling activates only in a servlet web application; the constraint-violation handler only when Bean Validation is on the classpath.

**1. Add the dependency** (pick the module matching your Boot version):

```xml
<dependency>
  <groupId>io.github.josesito1996</groupId>
  <artifactId>spring-boot-tool-kit-orrai</artifactId>       <!-- Spring Boot 3/4; or -boot2 for Spring Boot 2.x -->
  <version>0.2.4</version>
</dependency>
```

**2. Throw anywhere** — the advice renders a consistent error body (RFC 7807 `ProblemDetail` on boot3, `ApiError` on boot2) with the matching HTTP status:

```java
throw new ResourceNotFoundException("Cliente " + id);   // 404  errorCode=RESOURCE_NOT_FOUND
throw new UnauthorizedException("Token inválido");       // 401  errorCode=UNAUTHORIZED

// Status/code not in the ErrorCode catalog? Use the raw int constructor:
throw new BusinessException(409, "CONFLICT", "Email ya registrado");   // 409
```

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
- `http/OrraiHttpClientFactory` — builds a per-client `RestClient` (own base URL + connect/read timeouts + optional static default headers from `OrraiClientConfig.headers()`) using the Spring Boot 4 **`org.springframework.boot.http.client`** API (`HttpClientSettings.defaults()` + `ClientHttpRequestFactoryBuilder.detect()`). **`boot3` now targets Spring Boot 4.0.0** (`spring-boot.version=4.0.0`, Java 21); the other modules (`boot2`, `data-*`, `core`) keep their previous Boot versions and stay compatible. The migration follows Boot 4's package moves: `RestClientAutoConfiguration` → `org.springframework.boot.restclient.autoconfigure` (`spring-boot-restclient`), `WebMvcAutoConfiguration` → `org.springframework.boot.webmvc.autoconfigure`, and the old `ClientHttpRequestFactorySettings` → `HttpClientSettings` (`spring-boot-http-client`). Both `spring-boot-restclient` and `spring-boot-http-client` are added as **optional** dependencies.
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
# Optional static headers sent on EVERY request of this client (e.g. API key, tenant id).
# Keep secrets out of source — reference an env var (or a Vault-backed property).
orrai.http.client.headers.X-Api-Key=${DOWNSTREAM_API_KEY}
orrai.http.client.headers.X-Tenant-Id=legalbyte

# Named clients -> resolved via OrraiHttpClientRegistry.client("<name>")
orrai.http.clients.google-maps-elevation.base-url=https://maps.googleapis.com/maps/api/elevation
orrai.http.clients.google-maps-elevation.read-timeout=10s
orrai.http.clients.google-maps-geocode.base-url=https://maps.googleapis.com/maps/api/geocode
orrai.http.clients.google-maps-geocode.headers.Accept=application/json
```

Each `headers.<name>=<value>` entry is applied to the client's `RestClient` as a **default header**, so it rides on every request without touching call sites. They are **static** (same value for all requests) — for dynamic per-call headers (e.g. a rotating token or a correlation id) pass the `headers` map to `execute(...)`; per-request headers are **merged on top of** the configured defaults. The `headers` map is optional and normalized to an immutable empty map when absent (never `null`).

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

## HashiCorp Vault secret custody (vault)

A fourth feature family: **zero-infrastructure** secret loading backed by `spring-cloud-vault`. The consumer writes no Vault code — only `spring.application.name` plus credentials as env vars. Package root `com.library.support.orrai.vault`; module targets **Java 21 / Spring Boot 4.0.x + Spring Cloud 2025.1.x** (spring-cloud-vault 5.0.x — see the version-compatibility note above).

**How it works:** an `EnvironmentPostProcessor` (`OrraiVaultEnvironmentPostProcessor`, registered in `META-INF/spring.factories` under the Boot 4 key `org.springframework.boot.EnvironmentPostProcessor`, ordered just before `ConfigDataEnvironmentPostProcessor`) reads the `ORRAI_VAULT_*` env vars and — **only if the consumer hasn't already set them** — injects sane `spring.cloud.vault.*` defaults (`addLast` = lowest precedence, so consumer config always wins) plus `spring.config.import=vault://`. spring-cloud-vault then does the real authentication, KV v2 lookup, TLS and lease renewal. Reading/parsing is isolated (`OrraiVaultEnvVars` record) from the default mapping (`OrraiVaultDefaults`), keeping the mechanism unit-testable and portable across Java upgrades.

- **Path convention:** KV v2 at `secret/<spring.application.name>` (emitted as a `${spring.application.name}` placeholder, resolved after Config Data — never in the EPP, where the name isn't bound yet).
- **Auth:** AppRole (RoleID + SecretID) preferred over TOKEN when both are present; TOKEN is for local dev only.
- **Fail-fast:** `spring.cloud.vault.fail-fast=true` by default — a missing/unreachable Vault halts startup instead of running without credentials. Secret **values are never logged** (only the app context / property names).
- `spring-cloud-starter-vault-config` is a **non-optional** dependency so "just add the module" works with zero infra.

### Consumer usage

**1. Add the dependency** (brings spring-cloud-vault transitively):

```xml
<dependency>
  <groupId>io.github.josesito1996</groupId>
  <artifactId>spring-boot-tool-kit-orrai-vault</artifactId>
  <version>0.2.4</version>
</dependency>
```

**2. The only config the developer writes:**

```properties
spring.application.name=mi-microservicio
```

**3. Supply credentials as environment variables** (from the OS / deploy platform):

```bash
# Local dev — token against a -dev Vault over HTTP
export ORRAI_VAULT_URL=http://localhost:8200
export ORRAI_VAULT_TOKEN=dev-root-token

# Production — AppRole over TLS (never a root token)
export ORRAI_VAULT_URL=https://vault.internal:8200
export ORRAI_VAULT_ROLE_ID=...
export ORRAI_VAULT_SECRET_ID=...
# export ORRAI_VAULT_NAMESPACE=team-a   # optional (Vault Enterprise)
```

**4. Consume secrets like any other property** — no infra code:

```java
@Service
public class PaymentService {

    private final String stripeApiKey;

    // resolved from Vault at  secret/mi-microservicio  ->  key "stripe.api-key"
    PaymentService(@Value("${stripe.api-key}") String stripeApiKey) {
        this.stripeApiKey = stripeApiKey;
    }
}
```

Any `spring.cloud.vault.*` property the consumer sets overrides the injected default (e.g. change the KV backend/path, or set `spring.cloud.vault.fail-fast=false` to degrade gracefully in dev). If you already declare your own `spring.config.import`, add `vault://` to it explicitly — the module only sets it when absent.

Env var → property mapping:

| Env var | Maps to | Purpose |
|---------|---------|---------|
| `ORRAI_VAULT_URL` | `spring.cloud.vault.uri` | Vault address — presence triggers the whole integration |
| `ORRAI_VAULT_TOKEN` | `…authentication=TOKEN` + `…token` | dev auth |
| `ORRAI_VAULT_ROLE_ID` / `ORRAI_VAULT_SECRET_ID` | `…authentication=APPROLE` + `…app-role.*` | prod auth (preferred when both present) |
| `ORRAI_VAULT_NAMESPACE` | `spring.cloud.vault.namespace` | Vault Enterprise namespace |

### Registering a secret in Vault (e.g. `spring.datasource.password`)

The module reads **KV v2** at `secret/<spring.application.name>`, and **every key stored there becomes a Spring property with that exact name**. So to feed the datasource password, store a key literally named `spring.datasource.password` under your app's path — Spring Boot's own datasource auto-configuration then picks it up with no `@Value` and no extra code.

For app `spring.application.name=mi-microservicio`, the path is `secret/mi-microservicio` (KV v2 mount `secret`, context `mi-microservicio`).

**Option A — Vault CLI** (simplest; `vault kv` handles the KV v2 `/data/` segment for you):

```bash
export VAULT_ADDR=https://vault.internal:8200
export VAULT_TOKEN=...        # a token allowed to write this path

# Write one or more keys (dots in the key name are fine and intentional)
vault kv put secret/mi-microservicio \
    spring.datasource.password=Sup3rS3cret \
    spring.datasource.username=app

# Verify
vault kv get secret/mi-microservicio
```

**Option B — Raw HTTP API.** Note the extra `data/` in the path and the `{"data": { ... }}` wrapper — both are KV v2 specifics. (spring-cloud-vault adds the `/data/` segment automatically when *reading*, so you never configure it in the app.)

```bash
curl -sS -H "X-Vault-Token: $VAULT_TOKEN" \
     -X POST \
     -d '{"data": {"spring.datasource.password": "Sup3rS3cret", "spring.datasource.username": "app"}}' \
     "$VAULT_ADDR/v1/secret/data/mi-microservicio"
```

**Least-privilege policy** — grant the app's AppRole read-only access to its own path only (KV v2 policies target `secret/data/<app>`):

```hcl
# file: mi-microservicio-policy.hcl
path "secret/data/mi-microservicio" {
  capabilities = ["read"]
}
```

```bash
vault policy write mi-microservicio mi-microservicio-policy.hcl
# bind it to the AppRole the service authenticates with (ORRAI_VAULT_ROLE_ID / ORRAI_VAULT_SECRET_ID)
vault write auth/approle/role/mi-microservicio token_policies="mi-microservicio"
```

Two rules to keep it working:

- **The Vault key name must equal the Spring property name.** `spring.datasource.password` in Vault → `${spring.datasource.password}` in Spring. A typo just means the property isn't found (and, with `fail-fast=true`, may surface at startup).
- **Profile-specific secrets** live at `secret/mi-microservicio/<profile>` (e.g. `secret/mi-microservicio/prod`); spring-cloud-vault overlays the active profile's path on top of the base one, so put shared keys in `secret/mi-microservicio` and per-environment overrides in the profile path.

### Consumer troubleshooting: context fails to start with `SimpleDiscoveryClientAutoConfiguration`

A consumer that adds the `vault` module but is **not** on the Boot 4 Spring Cloud train fails at startup — long before Vault is ever contacted — with:

```
IllegalStateException: Error processing condition on
  org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration.simpleDiscoveryProperties
...
Caused by: java.lang.NoClassDefFoundError: org/springframework/boot/web/context/WebServerInitializedEvent
Caused by: java.lang.ClassNotFoundException: org.springframework.boot.web.context.WebServerInitializedEvent
```

**Root cause — version skew, not a Vault or secret problem.** `WebServerInitializedEvent` moved packages in Spring Boot 4 (it no longer lives in `org.springframework.boot.web.context`). The `spring-cloud-starter-vault-config` this module pulls transitively drags in `spring-cloud-commons`; if a **Boot 3-era** Spring Cloud train (2023.0.x / 2024.0.x) is resolved instead of the Boot 4 one, that old `spring-cloud-commons` is compiled against the removed package, so Boot's `@ConditionalOnMissingBean` type deduction on `SimpleDiscoveryClientAutoConfiguration` blows up while introspecting the class. This happens during bean-definition loading, which is why registering the secret in Vault changes nothing.

**Fix — force the Boot 4 train in the consumer** (this library targets **Spring Cloud 2025.1.x / spring-cloud-vault 5.0.x**):

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-dependencies</artifactId>
      <version>2025.1.2</version>   <!-- Oakwood = Boot 4 -->
      <type>pom</type><scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

Then **remove any older `spring-cloud-dependencies` import or `<spring-cloud.version>` property** in the consumer, and keep the Spring Boot parent aligned (4.0.7). Verify the winning version — it must be `spring-cloud-commons` **5.x**, never 4.x:

```bash
mvn dependency:tree -Dincludes=org.springframework.cloud:spring-cloud-commons
```

## Corporate Config Server client (config-client)

A fifth feature family: **zero-infrastructure** loading of central configuration from an existing
corporate **Spring Cloud Config Server** (this module is the *client* — it does not run a server).
Same mechanism as `vault`: a `ConfigClientEnvironmentPostProcessor` runs before Config Data and, from
the `starter.config.*` settings, injects the `spring.cloud.config.*` defaults plus
`spring.config.import=configserver:` (`optional:configserver:` when `fail-fast=false`). Retry
(spring-retry, bundled), request timeouts, fail-fast and `@RefreshScope` all work out of the box.
Package root `com.library.support.orrai.config`; full details in [`config-client/README.md`](config-client/README.md).

### Consumer usage — what to put in your microservice

**1. Add the dependency** (brings spring-cloud-config-client transitively):

```xml
<dependency>
  <groupId>io.github.josesito1996</groupId>
  <artifactId>spring-boot-tool-kit-orrai-config-client</artifactId>
  <version>0.2.4</version>
</dependency>
```

**2. Import the Boot 4 Spring Cloud train** (mandatory — an older train triggers the
`SimpleDiscoveryClientAutoConfiguration` crash documented above), and keep the Boot parent at 4.0.7:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-dependencies</artifactId>
      <version>2025.1.2</version>
      <type>pom</type><scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

**3. The only config the developer writes** (`application.properties`) — the app name plus the Config
Server URL. Everything else (`spring.config.import`, retry, timeouts, fail-fast) is injected for you:

```properties
spring.application.name=prediction
starter.config.uri=https://config.corp.internal:8888
```

Credentials, when the server requires basic auth, belong in env vars — never committed:

```properties
starter.config.username=svc-user
starter.config.password=${CONFIG_PWD}
```

**4. Consume any property served by the Config Server** — no infrastructure code:

```java
@Value("${pricing.rate-provider}") String rateProvider;   // resolved from the Config Server
```

Optional knobs (all with production defaults; override only when needed):

```properties
starter.config.enabled=true         # false disables the starter entirely
starter.config.fail-fast=true       # false => optional import, degrades if the server is down (dev)
starter.config.label=main           # Git branch/label
starter.config.profile=             # defaults to the active Spring profiles
starter.config.max-attempts=6       # retry against the Config Server
starter.config.connect-timeout=5000
starter.config.read-timeout=30000
```

Any `spring.cloud.config.*` you set by hand overrides the injected default. If you already declare your
own `spring.config.import`, add `configserver:` to it explicitly — the module only sets it when absent.

## Testing approach

JUnit 5 with `@DisplayName`. `core` has plain unit tests (no Spring). Each adapter's `GlobalExceptionHandlerTest` drives the advice through **MockMvc standalone** against an inner `TestController`, asserting status + error-body JSON (incl. a regression test that the validation advice wins over the catch-all despite registration order). `ExceptionHandlingAutoConfigurationTest` uses `WebApplicationContextRunner`/`ApplicationContextRunner` for bean registration, `@ConditionalOnMissingBean` back-off, and non-web inactivity. Surefire/compiler/JUnit versions are pinned in the parent `pom.xml` (no `spring-boot-starter-parent` to supply them).

For the data modules: `data-core` has plain JUnit 5 unit tests (`PageQueryTest`, `PageResponseTest` — clamping, offset, derived page flags, immutability). Each data adapter has a `@DataJpaTest` (`AuditingAndSoftDeleteTest`) running against **H2**, with a `TestDataApplication` (`@SpringBootApplication`, test-only) to bootstrap the slice and a `sample/` fixture entity+repository; it asserts that auditing fills `createdAt`/`createdBy` on insert and that `softDelete` hides rows from the active finders while the row is physically retained and flagged. A second `@DataJpaTest` (`AuditableFieldsTest`, with a `CustomIdAuditableEntity` fixture that declares its own `@Id`) verifies that `AuditableFields` adds auditing to an entity owning its primary key — no duplicate-id conflict — and that `updatedAt` advances on update while `createdAt` is preserved.

For the **HTTP client** (boot3): `RestClientOrraiHttpClientTest` drives the `RestClient` adapter through **`MockRestServiceServer`** (per-request headers/query params, generic `List<T>` via `OrraiTypeRef`, and the upstream/transport/deserialization error mapping). `OrraiClientConfigTest` is a plain unit test for the record's `headers` normalization (null → immutable empty map, defensive copy). The factory's own transport-building path can't be reached with `MockRestServiceServer` — it overrides the request factory — so `OrraiHttpClientFactoryHeadersTest` stands up a real HTTP server with **WireMock** (`wiremock-standalone`, test scope; the shaded uber-jar isolates WireMock's Jackson 2 / Jetty from Boot 4's Jackson 3) on a dynamic port and asserts the configured default headers are actually sent on the wire, that per-request headers merge with them, and that an unset `headers` sends none.

For the **vault** module: plain JUnit 5 + AssertJ unit tests, no Vault server needed — `OrraiVaultDefaultsTest` covers the env-var → `spring.cloud.vault.*` mapping (AppRole-over-TOKEN precedence, namespace, immutability) and `OrraiVaultEnvironmentPostProcessorTest` drives the `EnvironmentPostProcessor` against Spring's `MockEnvironment`, asserting defaults are injected only when `ORRAI_VAULT_URL` is set, that consumer-set values win, that an existing `spring.config.import` is never overridden, and that the processor is ordered before Config Data. `OrraiVaultRegistrationTest` guards the Spring Boot 4 wiring — that the class implements `org.springframework.boot.EnvironmentPostProcessor` (Boot 4 moved it out of the legacy `…boot.env` package) and is declared under that exact key in `META-INF/spring.factories`, so a stale import/key can't silently stop it from running. An end-to-end slice against a Vault `-dev` container (Testcontainers) is the recommended next step.

Test classes must be named `*Test` (Surefire only; no Failsafe configured).
