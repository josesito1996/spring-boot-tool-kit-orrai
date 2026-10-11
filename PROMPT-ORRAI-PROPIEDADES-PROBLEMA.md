# PROMPT — Propiedades extra en el ProblemDetail de `BusinessException` (orrai)

> Repo: `spring-boot-tool-kit-orrai`. Cambio **aditivo**: un consumidor que no use la feature
> tiene que recibir exactamente el mismo body que hoy.

---

## Qué es y para qué

Hoy `BusinessException` solo transporta `statusCode`, `errorCode` y el mensaje, y
`GlobalExceptionHandler.handleBusinessException` arma el ProblemDetail adentro. Un consumidor que
necesita un dato estructurado en el error no tiene dónde ponerlo. Le quedan dos salidas, y las dos
son malas:

- **Meterlo en el `detail`** y que el cliente lo parsee. Odontologico lo hizo con
  `FUNCIONALIDAD_NO_DISPONIBLE`, cuyo `detail` termina en el código.
- **Una subclase más un `@RestControllerAdvice` propio** que delega en el handler de orrai y
  agrega la propiedad. Odontologico lo hizo con `DocumentoDuplicadoException` +
  `PropiedadesProblemaAdvice` para mandar `paciente_id` en un 409. Funciona, pero cada consumidor
  repite el patrón y corre el riesgo de sombrear el handler de orrai con un segundo formato.

Lo que se busca: que cualquier `BusinessException` pueda llevar propiedades, y que el handler de
orrai las vuelque al ProblemDetail.

---

## Decisiones (no las cambies sin avisarme)

- **API en `BusinessException`, no una subclase nueva.** Un método fluido
  `withProperty(String clave, Object valor)` que devuelve `this` (o el mismo tipo, si se puede sin
  genéricos feos), y `getProperties()`, que devuelve una vista **inmodificable** y en orden de
  inserción. Uso esperado:
  `throw new BusinessException(409, "DOCUMENTO_DUPLICADO", "…").withProperty("paciente_id", 42);`
  Muta la excepción, sí: se construye y se lanza en la misma línea, y un builder aparte sería
  ceremonia. Si te parece mal, proponé la alternativa antes de codear.
- **`valor == null` no agrega la clave.** "Ausente" es la semántica que el cliente necesita: no
  `"paciente_id": null`.
- **Claves reservadas → `IllegalArgumentException`**: `type`, `title`, `status`, `detail`,
  `instance`, más las que agrega el handler (`timestamp`, `errorCode`, `path`, `errors`). Una
  propiedad no puede pisar el contrato base del error.
- **La clave viaja tal cual.** Las claves del mapa de propiedades del ProblemDetail no pasan por
  la naming strategy de Jackson. Si el consumidor quiere snake_case, escribe `paciente_id`.
  Documentalo en el Javadoc.
- **El mapa es `transient`.** `BusinessException` es `Serializable` y los valores son `Object`
  arbitrarios: serializar la excepción no puede fallar por una propiedad.
- **El handler solo agrega**: después de armar el ProblemDetail como hoy,
  `ex.getProperties().forEach(problem::setProperty)`. Ningún otro cambio de formato.
- **Mismo comportamiento en todas las variantes del handler** (Boot 3 y Boot 2, si las dos lo
  manejan). Si una variante no puede soportarlo, pará y avisame; no lo implementes a medias.
- **Versión: 0.2.5** (minor aditivo). Ningún cambio rompe la compatibilidad.

---

## Tarea 0 — Verificá esto antes de escribir código. **Parate acá.**

1. En qué módulo vive `BusinessException` (`-core`) y en cuáles el handler (`spring-boot-tool-kit-orrai`,
   `-boot2`). En la 0.2.4 vi `ProblemDetails` en algunas variantes y `ApiErrorSupport` en otras:
   confirmá cuál arma el body en cada una.
2. Cómo se agregan hoy `timestamp`, `errorCode` y `path`, para definir bien la lista de claves
   reservadas.
3. Si el handler de Boot 2 (sin `ProblemDetail` de Spring 6) puede volcar propiedades extra con el
   mismo resultado en JSON.
4. Listá los archivos que vas a tocar y **esperá mi OK**.

---

## Tarea 1 — Excepción

- `withProperty`, `getProperties`, la validación de claves reservadas y de clave nula/en blanco, y
  el mapa `transient`.
- Las subclases existentes (`ResourceNotFoundException`, `ForbiddenException`, etc.) heredan la
  feature sin cambios.

## Tarea 2 — Handler(s)

- Volcar las propiedades en `handleBusinessException` de cada variante.
- No tocar `handleMethodArgumentNotValid` ni `handleUnexpected`.

## Tarea 3 — Tests

- Con una propiedad: aparece en el body con su clave exacta y su tipo (un `Long` sale como número).
- Con `withProperty("x", null)`: la clave **no** aparece.
- Clave reservada (`status`, `errorCode`…): `IllegalArgumentException` al construir.
- **Sin propiedades: el body es idéntico al de la 0.2.4**, campo por campo. Este es el test que
  protege a los consumidores actuales.
- `getProperties()` no se puede modificar desde afuera.
- Serializar y deserializar la excepción no falla.

## Tarea 4 — Docs y release

- README: la sección de errores con el ejemplo del 409 con `paciente_id`.
- CHANGELOG 0.2.5.

---

## Después, en Odontologico (otro paso, no en este repo)

Con la 0.2.5 publicada:

- Subir `spring-boot-tool-kit-orrai` a 0.2.5 en el `pom`.
- **Borrar** `DocumentoDuplicadoException` y `PropiedadesProblemaAdvice`.
- `PacienteService.editar` lanza
  `ErroresAgenda.de(DOCUMENTO_DUPLICADO, mensaje).withProperty("paciente_id", id)`.
- Actualizar en CLAUDE.md la sección que hoy dice "el ProblemDetail de orrai no admite campos
  propios", y volver a la regla de "ningún `@RestControllerAdvice` propio" sin excepción.
- Fuera de alcance, pero queda habilitado: que `FUNCIONALIDAD_NO_DISPONIBLE` mande el código como
  propiedad en vez de al final del `detail`. Eso es un cambio de contrato con el frontend, así que
  va aparte.

---

## Cómo quiero que trabajes

- Directo al grano; explicá el porqué de cada decisión en 1–2 líneas.
- Una tarea por vez, con tests verdes antes de pasar a la siguiente.
- **Comentarios en el código: solo el "por qué" no evidente.**
- Si algo contradice estas decisiones, pará y preguntame; no improvises.
