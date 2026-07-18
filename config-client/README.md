# orrai Config Client Starter

Zero-infrastructure client for a corporate **Spring Cloud Config Server**. A microservice adds one
dependency and sets `starter.config.uri`; everything else — `spring.config.import`, connection,
credentials, retry, timeouts, fail-fast and `@RefreshScope` — is wired automatically.

> **This module does NOT run a Config Server.** It assumes your organization already operates a
> centralized Spring Cloud Config Server backed by a Git repository. This is the *client* side.

- **Coordinates:** `io.github.josesito1996:spring-boot-tool-kit-orrai-config-client`
- **Target:** Java 21 · Spring Boot 4.0.x · Spring Cloud 2025.1.x (Oakwood) · `spring-cloud-config-client` 5.0.x
- **Package root:** `com.library.support.orrai.config`

---

## Objetivo

Eliminar la configuración repetitiva que cada microservicio necesita para conectarse al Config Server
corporativo. Sin `bootstrap.yml`, sin `spring.config.import` a mano, sin duplicar propiedades de
conexión/retry/timeout en cada servicio. El desarrollador solo declara la dependencia y la URI.

## Arquitectura

```
        Git Repository (application-*.yml, common.yml)
                          │
                          ▼
              Spring Cloud Config Server   (corporativo, ya existe)
                          │
        ┌─────────────────┼─────────────────┐
        ▼                 ▼                 ▼
  Microservicio A   Microservicio B   Microservicio C
        └─────────────────┼─────────────────┘
                          ▼
              orrai-config-client-starter
        (EnvironmentPostProcessor + @AutoConfiguration)
```

**Cómo funciona** (mismo mecanismo que el módulo `vault`):

1. `ConfigClientEnvironmentPostProcessor` corre **antes** de `ConfigDataEnvironmentPostProcessor` y:
   - Enlaza `starter.config.*` con un `Binder`.
   - Valida (URI obligatoria; basic-auth completo o ausente) y falla rápido con mensajes claros.
   - Inyecta los defaults `spring.cloud.config.*` con `addLast` (mínima precedencia → lo que ponga el
     consumidor siempre gana).
   - Añade `spring.config.import=configserver:` (o `optional:configserver:` si `fail-fast=false`), solo
     si el consumidor no definió ya un import.
   - `spring.config.import` **debe** estar presente antes de Config Data; por eso esto no puede vivir en
     una `@AutoConfiguration` (correría demasiado tarde).
2. Spring Cloud Config Client hace la carga real, retry, timeouts y `@RefreshScope`.
3. `ConfigClientAutoConfiguration` solo registra los beans del contexto (binding de propiedades +
   `ConfigClientStartupLogger`).

## Instalación

### Dependencia Maven

```xml
<dependency>
  <groupId>io.github.josesito1996</groupId>
  <artifactId>spring-boot-tool-kit-orrai-config-client</artifactId>
  <version>0.2.4</version>
</dependency>
```

⚠️ El consumidor **debe** estar en el tren de Spring Cloud de Boot 4. Importa el BOM y quita cualquier
tren viejo (ver [Troubleshooting](#troubleshooting)):

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

## Configuración

Todo bajo el prefijo `starter.config`:

```yaml
starter:
  config:
    enabled: true          # false para deshabilitar el starter por completo
    uri: https://config.corp.internal:8888   # OBLIGATORIA cuando enabled=true
    username: svc-user       # opcional (basic auth: ambos o ninguno)
    password: ${CONFIG_PWD}
    fail-fast: true
    retry-enabled: true
    max-attempts: 6
    initial-interval: 1000
    multiplier: 1.5
    max-interval: 5000
    label: main
    profile:                 # por defecto, los perfiles activos
    application-name:        # por defecto, ${spring.application.name}
    connect-timeout: 5000
    read-timeout: 30000
```

Lo mínimo en un microservicio:

```properties
spring.application.name=prediction
starter.config.uri=https://config.corp.internal:8888
```

### Mapeo `starter.config.*` → `spring.cloud.config.*`

| `starter.config`                | Se traduce a                                   |
|---------------------------------|------------------------------------------------|
| `uri`                           | `spring.cloud.config.uri`                      |
| `application-name`              | `spring.cloud.config.name` (o `${spring.application.name}`) |
| `label`                         | `spring.cloud.config.label`                    |
| `profile`                       | `spring.cloud.config.profile`                  |
| `username` / `password`         | `spring.cloud.config.username` / `.password`   |
| `fail-fast`                     | `spring.cloud.config.fail-fast` + import `configserver:` vs `optional:configserver:` |
| `connect-timeout` / `read-timeout` | `spring.cloud.config.request-connect-timeout` / `.request-read-timeout` |
| `retry-*` / `max-attempts` / `initial-interval` / `multiplier` / `max-interval` | `spring.cloud.config.retry.*` |

Cualquier `spring.cloud.config.*` que definas a mano **gana** sobre el default inyectado.

## Ejemplos

Consumir cualquier propiedad servida por el Config Server, sin código de infraestructura:

```java
@Service
public class PricingService {

    private final String rateProvider;

    PricingService(@Value("${pricing.rate-provider}") String rateProvider) {
        this.rateProvider = rateProvider;   // viene del Config Server
    }
}
```

Refresco en caliente con `@RefreshScope` (sin configuración extra):

```java
@RefreshScope
@Service
public class FeatureFlags {
    @Value("${features.new-checkout:false}")
    private boolean newCheckout;
}
```

```bash
# tras cambiar el valor en Git + Config Server:
curl -X POST http://localhost:8080/actuator/refresh
```

## Integración con Config Server

- El import `configserver:` (mandatorio con `fail-fast=true`) obliga a resolver la config al arranque.
- Con `fail-fast=false` se usa `optional:configserver:`: si el server no responde, la app degrada en
  lugar de fallar (útil en dev).
- El retry (spring-retry, incluido) reintenta la conexión al server según `max-attempts` /
  `initial-interval` / `multiplier` / `max-interval` cuando `fail-fast=true`.

## Actuator

Con `spring-boot-starter-actuator` presente, expón lo que necesites:

```properties
management.endpoints.web.exposure.include=refresh,env,configprops,health
```

## Buenas prácticas

- **Nunca** pongas `password` en el repositorio; usa una variable de entorno (`${CONFIG_PWD}`) o un
  secreto (p. ej. el módulo `vault` de este toolkit).
- Mantén `fail-fast=true` en prod (fallar cerrado); usa `false` solo en dev para degradar.
- Deja `application-name` vacío para que herede `spring.application.name` — una sola fuente de verdad.
- Prepara OAuth2 a futuro configurando `spring.cloud.config.*` directamente; la arquitectura no lo
  bloquea (basic auth es el camino soportado hoy).

## Troubleshooting

### El contexto no arranca: `SimpleDiscoveryClientAutoConfiguration` / `WebServerInitializedEvent`

```
IllegalStateException: Error processing condition on
  ...SimpleDiscoveryClientAutoConfiguration.simpleDiscoveryProperties
Caused by: java.lang.ClassNotFoundException:
  org.springframework.boot.web.context.WebServerInitializedEvent
```

**Causa:** desalineación de versiones. Este starter arrastra `spring-cloud-commons`; si el consumidor
resuelve un tren de Spring Cloud de la era Boot 3 (2023.0.x / 2024.0.x), ese `spring-cloud-commons`
referencia un paquete que Boot 4 movió y explota antes de contactar el Config Server.

**Solución:** importa `spring-cloud-dependencies` **2025.1.2**, elimina cualquier BOM/versión de Spring
Cloud antigua y alinea el parent de Boot a 4.0.7. Verifica:

```bash
mvn dependency:tree -Dincludes=org.springframework.cloud:spring-cloud-commons
```

Debe ser `spring-cloud-commons` **5.x**, nunca 4.x.

### `starter.config.uri is required...`

El starter está habilitado pero falta la URI. Define `starter.config.uri` o desactívalo con
`starter.config.enabled=false`.

### No conecta al Config Server

Con `fail-fast=true` el arranque se detiene y Spring Cloud Config registra el detalle. Revisa URI,
red/credenciales y, si aplica, sube `max-attempts`. Para arrancar sin server en dev, usa
`starter.config.fail-fast=false`.

## Compatibilidad

| | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.x |
| Spring Framework | 7 |
| Spring Cloud | 2025.1.x (Oakwood) |
| spring-cloud-config-client | 5.0.x |