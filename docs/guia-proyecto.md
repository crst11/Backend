# Guía del proyecto - CundiApp Backend

Documento de trabajo del equipo. Resume el contexto del producto, las reglas del proyecto y la forma de trabajar en este repositorio. Cuando esta guía y un documento más reciente del proyecto no coincidan, manda el más reciente.

## 1. Contexto del producto

CundiApp es una PWA para estudiantes de la Universidad de Cundinamarca (programa piloto: Ingeniería de Sistemas, sede Fusagasugá). Centraliza en un solo lugar la información académica que hoy está dispersa entre la plataforma institucional (Academusoft/Hermesoft) y Moodle, y además la interpreta: el problema no es que falten datos, es que nadie los interpreta a tiempo.

Prioridad del producto, en este orden:

1. **Unificación:** horario, notas, salones y fechas en un solo lugar.
2. **El plus, no el eje:** la Brújula Académica (simulador de calificaciones e indicador de riesgo). Nunca se llama "semáforo" en pantallas; la universidad ya usa esa palabra para otro reporte.
3. **Funciones activas:** Mis Pendientes con notificaciones, horario organizado por parámetros propios, importación de reportes en PDF.
4. **Guía institucional:** consulta pública de documentos oficiales sin cuenta.

### Restricción técnica fundamental

CundiApp no se conecta a ninguna plataforma institucional sin autorización. Nada de scraping, nada de pedir credenciales institucionales, nada de leer Moodle en vivo. Los datos llegan por tres fuentes intercambiables:

- **Importación de PDF** que la universidad le entrega al propio estudiante (Registro Académico Extendido, Semáforo del Creador de Oportunidades, Reporte de Horario, Consultar Notas Actuales), con confirmación antes de guardar y actualización incremental sin duplicados.
- **Registro manual** del estudiante (estructura de evaluación, notas parciales, pendientes).
- **Integración autorizada futura** (Web Services de Moodle): se prevé como un adaptador más y no se construye ahora.

La Guía institucional solo lee documentos que la universidad ya publica sin sesión en su portal.

## 2. Alcance vigente

Son doce requerimientos funcionales. No se agregan más sin acuerdo del equipo.

| Código | Nombre |
|---|---|
| RF01 | Gestión de cuenta y sesión |
| RF02 | Perfil, plan de estudios e historial |
| RF03 | Importación y actualización de reportes institucionales (PDF) |
| RF04 | Horario con ubicación y organización por parámetros |
| RF05 | Estructura de evaluación y calificaciones |
| RF06 | Mis Pendientes |
| RF07 | Pantalla del día |
| RF08 | Simulador de calificaciones |
| RF09 | Brújula Académica |
| RF10 | Motor de notificaciones |
| RF11 | Guía institucional y acceso a plataformas oficiales |
| RF12 | Consulta sin conexión |

Actores: el **estudiante** (RF01 a RF12, autenticado) y el **visitante sin cuenta** (solo RF11). No existe rol administrativo. Los parámetros los ajusta el propio estudiante.

### Reglas de negocio con valores por defecto

| Parámetro | Valor por defecto | Quién lo cambia |
|---|---|---|
| Estructura de evaluación | Árbol ponderado de dos niveles: categorías que suman 100 % y, dentro de cada una, actividades que suman 100 %. Escala 0.0 a 5.0. Plantilla inicial 30 % / 30 % / 40 % | El estudiante (RF05) |
| Meta de calificación | 3.0 | El estudiante (RF08) |
| Umbral de riesgo medio | Nota requerida mayor a 3.5 | El estudiante (RF09) |
| Umbral de riesgo alto | Nota requerida mayor a 4.5 o inalcanzable | El estudiante (RF09) |

La evaluación nunca se modela como "tres cortes fijos" ni como tres columnas.

### Requerimientos no funcionales que afectan el código

| Requerimiento | Criterio verificable |
|---|---|
| Seguridad | JWT de acceso corto (15 a 30 min) más refresco (7 días) del que solo se guarda la huella SHA-256; contraseñas con bcrypt; ningún estudiante ve datos de otro (403) |
| Rendimiento | Respuesta menor a 3 s con 40 a 80 usuarios concurrentes |
| Disponibilidad | 99 % durante el período académico |
| Usabilidad | Diseño móvil primero; registrar una nota en máximo tres interacciones |
| Portabilidad | PWA instalable, funcional desde 360 px de ancho |
| Escalabilidad | Hasta 10 000 estudiantes con respuesta menor a 3 s en prueba de carga |
| Mantenibilidad | Motor académico probado sin base de datos ni interfaz; cobertura mayor o igual a 80 % en dominio |
| Ley 1581 de 2012 | Consentimiento explícito con fecha; no se almacena documento de identidad del estudiante ni de docentes |

## 3. Reglas innegociables

1. **Arquitectura hexagonal en el backend.** El dominio es Java puro, las dependencias apuntan hacia adentro y ArchUnit lo verifica en cada compilación.
2. **Nada de lógica de negocio en controladores ni en Angular.** El frontend muestra y captura; los cálculos (nota requerida, riesgo, semana cargada) viven en el dominio.
3. **Angular nunca habla con la base de datos.** Siempre Angular, API REST de Spring Boot y PostgreSQL.
4. **Configuración fuera del código.** Ninguna URL, puerto, credencial ni secreto escrito a mano. Todo lo que va a Angular es público.
5. **Ningún secreto en Git.** `.env` y `application-local.properties` con contraseñas van en `.gitignore` desde el primer commit. Se versiona `.env.example` con las llaves vacías.
6. **Dos repositorios, tres ramas de ambiente,** código en una sola dirección (sección 5).
7. **Ningún cambio entra a `desarrollo` sin pull request** revisado por el otro integrante y con CI en verde.
8. **Una tarea está terminada** solo cuando cumple la Definición de Terminado (sección 6).
9. **Las entidades JPA no son entidades de dominio.** Viven en infraestructura y se mapean.
10. **Lo calculado no se guarda.** Promedios, nota requerida y nivel de riesgo se calculan en el dominio o salen de vistas SQL; no son columnas.
11. **Hibernate nunca crea ni altera tablas** (`spring.jpa.hibernate.ddl-auto=validate`). El esquema lo gobiernan las migraciones.
12. **Monolito modular, no microservicios.** Un solo backend desplegable, bien delimitado por dentro.
13. **Menos carpetas y más claridad.** No se crean paquetes vacíos "por si acaso" ni abstracciones sin un motivo concreto. Pregunta de control: "¿el equipo sabría en 5 segundos dónde va el próximo archivo?".

Idioma: carpetas, paquetes y nombres técnicos en inglés (`domain`, `application`, `infrastructure`, `controller`, `repository`, `DTO`). Vocabulario del negocio, mensajes de commit y documentación en español (sección 9).

## 4. Stack y versiones

| Capa | Tecnología | Nota |
|---|---|---|
| Backend | Java 21 LTS, Spring Boot (línea 4.x estable) y Maven Wrapper | Se genera desde start.spring.io. Si alguna dependencia aún no soporta 4.x, se usa la última 3.5.x y se deja escrito en un ADR |
| Dependencias | Web, Validation, Data JPA, Security, OAuth2 Resource Server (JWT), PostgreSQL Driver, Flyway (`spring-boot-starter-flyway` y `flyway-database-postgresql`), Actuator, springdoc-openapi, Apache PDFBox (sprint 2), ArchUnit, Testcontainers | No usar iText (licencia AGPL) |
| Base de datos | PostgreSQL 16, esquema `cundiapp` | Docker en local, Supabase en el ambiente compartido |
| Frontend | Angular con componentes standalone, señales y `@angular/pwa` | Repositorio Frontend |
| Notificaciones | Web Push API (service worker) más aviso dentro de la app | Sprint 6 |
| Identidad | JWT propio, "Iniciar con Google" (OpenID Connect) y verificación del correo institucional por código | Modelado como puerto para migrar a Microsoft Entra ID en la v2.0 |
| Gestión | Jira (proyecto SCRUM), GitHub y GitHub Actions | |

## 5. Ramas, commits y pull requests

Hay dos repositorios, `Backend` y `Frontend`, porque se despliegan por separado.

| Rama | Contenido | Cómo entra el código | Ambiente |
|---|---|---|---|
| `feature/SCRUM-XX-descripcion` | Trabajo de una historia o tarea | Se crea desde `desarrollo` | Local (DEV) |
| `fix/SCRUM-XX-descripcion` | Corrección de un error | Se crea desde `desarrollo` | Local (DEV) |
| `desarrollo` | Integración del sprint en curso | Pull request revisado y CI en verde | Desarrollo |
| `preproduccion` | Lo que se demuestra ante el Comité de Arquitectura | Merge desde `desarrollo` al cerrar el sprint | Preproducción (PRE) |
| `produccion` | Producción: lo que el Comité aceptó | Merge desde `preproduccion` después del comité | Producción (PROD) |

`desarrollo` es la rama por defecto. El código fluye siempre `feature` → `desarrollo` → `preproduccion` → `produccion`, siempre por merge. Ninguna rama de historia salta a `preproduccion` ni a `produccion`. Los ajustes que salgan de la demostración en `preproduccion` se hacen como pull requests pequeños hacia `desarrollo` y se vuelven a promover. Una corrección urgente en producción sale de `produccion`, se fusiona a `produccion` y se devuelve a `desarrollo`.

### Protección de ramas

`desarrollo`, `preproduccion` y `produccion` requieren pull request con 1 aprobación, checks de CI en verde, y bloquean push directo, force push y borrado.

### Commits

Se usa Conventional Commits con la llave de Jira para que quede enlazado:

```
feat(cuenta): registrar estudiante con correo institucional SCRUM-17
fix(horario): corregir choque de bloques que terminan a la misma hora SCRUM-25
test(evaluacion): cubrir suma de pesos distinta de 100 % SCRUM-27
refactor(persistencia): extraer mapeador de matrícula SCRUM-21
docs(adr): registrar decisión de Flyway SCRUM-41
chore(ci): agregar verificación de arquitectura SCRUM-43
```

Tipos: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`, `ci`, `perf`. Commits pequeños, un propósito por commit. Están prohibidos mensajes como "cambios", "arreglos" o "final".

### Pull requests

- Título: `SCRUM-XX tipo: descripción`.
- Cuerpo según la plantilla del repositorio.
- Pequeños: idealmente menos de 400 líneas cambiadas. Si una historia crece, se parte.
- El autor no aprueba su propio pull request. El otro integrante revisa con criterio constructivo.

### Versiones

Etiquetas de versionado semántico al promover a `produccion`: `v0.1.0` (sprint 1), `v0.2.0`, y así hasta `v1.0.0` (cierre del semestre). Los cambios se registran en `CHANGELOG.md`.

## 6. Jira y ciclo de trabajo

El trabajo se lleva en el proyecto SCRUM de Jira: una **épica** por requerimiento funcional, **historias** con criterios de aceptación y **tareas** técnicas. Las subtareas técnicas se crean cuando la incidencia arranca, no antes.

### Los seis pasos de cada funcionalidad

Se construye en este orden: primero base de datos, luego backend y luego frontend.

1. **Migración de base de datos** (si la historia la necesita).
2. **Dominio y puerto:** modelo, reglas y pruebas unitarias; interfaces de puertos.
3. **Adaptador de persistencia:** entidad JPA, repositorio Spring Data, mapeador y prueba de integración.
4. **Caso de uso y API:** implementación del caso de uso, controlador REST, DTOs y documentación OpenAPI.
5. **Pantalla en Angular:** repositorio de datos, componente, ruta y pruebas.
6. **Despliegue en preproducción** y demostración.

Una funcionalidad no cuenta como avance hasta recorrer los seis. Las capas sueltas nunca son historias propias: siempre son subtareas. Además de los seis pasos, cada historia lleva su historia de usuario (HU) y su caso de uso (CU) documentados.

### Tablero

Cinco columnas: Pendiente, Listo para desarrollar, En desarrollo, En revisión y Hecho, con un máximo de dos tarjetas en curso. Al crear la rama la tarjeta pasa a En desarrollo; al abrir el pull request, a En revisión; al fusionar y desplegar en preproducción, a Hecho.

### Plan de sprints

Sprints de dos semanas, dos funcionalidades por sprint, cada uno cierra con un Comité de Arquitectura.

| Sprint | Funcionalidades |
|---|---|
| 1 | Esquema de base de datos, RF01 y RF11, primer despliegue |
| 2 | RF02 y RF03 |
| 3 | RF04 y RF05 |
| 4 | RF06 y RF07 |
| 5 | RF08 y RF09 |
| 6 | RF10 y RF12 |
| 7 | Estabilización, pruebas de carga, documentación y paso a producción |

### Definición de Listo

Una historia entra al sprint solo si tiene criterios de aceptación claros, estimación y no depende de algo sin resolver.

### Definición de Terminado

- Código en la rama de la historia, siguiendo esta guía.
- Pruebas escritas y en verde (unitarias de dominio; de integración si toca persistencia).
- ArchUnit en verde.
- Pull request revisado y aprobado por el compañero, y fusionado a `desarrollo`.
- Funcionalidad desplegada en preproducción y verificable por un tercero.
- Documentación actualizada (README, OpenAPI y ADR si hubo decisión).
- Colección de Postman (`docs/postman/`) al día: cada endpoint nuevo con su petición, sus casos de error y sus pruebas, ejecutada contra el backend real. Es lo que se presenta en las demostraciones.
- Incidencia cerrada en Jira.

## 7. Ambientes y configuración

| Ambiente | Rama | Backend | Base de datos |
|---|---|---|---|
| DEV | `feature/*` y `desarrollo` | `http://localhost:8080` | PostgreSQL en Docker |
| PRE | `preproduccion` | URL de API PRE | Proyecto Supabase PRE |
| PROD | `produccion` | URL de API PROD | Proyecto Supabase PROD |

Las URLs de PRE y PROD se definen cuando se decida el hosting.

### Perfiles de Spring

`application.properties` (común, sin secretos):

```properties
spring.application.name=cundiapp
spring.profiles.active=${PERFIL:local}
server.port=${PORT:8080}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.default_schema=cundiapp
spring.flyway.schemas=cundiapp
spring.flyway.default-schema=cundiapp
cundiapp.cors.origenes-permitidos=${CORS_ORIGENES:http://localhost:4200}
cundiapp.jwt.duracion-acceso=${JWT_DURACION_ACCESO:PT20M}
cundiapp.jwt.duracion-refresco=${JWT_DURACION_REFRESCO:P7D}
```

`application-local.properties` solo lleva valores de desarrollo desechables. `application-pre.properties` y `application-prod.properties` leen `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRETO` del entorno, con `spring.datasource.hikari.maximum-pool-size=5`.

Spring Boot no lee `.env` por sí solo. En local se cargan las variables desde el IDE o con `spring.config.import=optional:file:.env[.properties]` en `application-local.properties`. En PRE y PROD las variables las entrega el hosting o GitHub Actions.

## 8. Base de datos

El modelo está diseñado: 27 tablas, 38 relaciones, 204 campos, 4 vistas y 14 restricciones de integridad en PostgreSQL 16. El centro del modelo es `matricula_de_asignatura`. Antes de escribir cualquier entidad JPA se abre el script y se copian los nombres exactos de tablas y columnas; no se adivinan.

### Migraciones con Flyway

```
src/main/resources/db/migration/
  V1__esquema_inicial.sql   (esquema sin DROP SCHEMA y con CREATE SCHEMA IF NOT EXISTS)
  V2__datos_arranque.sql    (plantilla 30/30/40 y categorías de la guía, si no vienen en V1)
  V3__...                   (cada cambio futuro; nunca se edita una migración ya aplicada)
```

- Flyway aplica las migraciones al arrancar; Hibernate solo valida.
- Se quita el `DROP SCHEMA ... CASCADE` del script original: es destructivo y Flyway no debe borrar nada.
- La tabla `flyway_schema_history` queda dentro de `cundiapp` y no cuenta como tabla del modelo.
- El mismo archivo corre en Docker, en PRE y en PROD, para que los ambientes no se desincronicen.
- Los datos de demostración para PRE van en `src/main/resources/db/demo/datos_demo.sql`, fuera de la carpeta de migraciones.
- No se cambia el esquema sin aprobación del equipo.

### Docker en local

`docker-compose.yml` en la raíz con un servicio `db` (imagen `postgres:16`, usuario y base `cundiapp`, puerto 5432 y volumen con nombre). Comandos: `docker compose up -d` y `docker compose down -v` para empezar de cero.

### Supabase (PRE y PROD)

- Conexión por Session pooler, puerto 5432. Nunca el Transaction pooler (6543): rompe las sentencias preparadas de Hibernate.
- No exponer el esquema `cundiapp` en la Data API de Supabase.
- Latido programado en GitHub Actions para que el plan gratuito no pause el proyecto.
- Recomendado: un proyecto de Supabase para PRE y otro para PROD.

## 9. Arquitectura hexagonal

```
co.edu.ucundinamarca.cundiapp
  CundiappApplication.java
  domain/                      Java puro: sin Spring, sin JPA, sin Jackson
    model/                     Estudiante, Sesion, CodigoDeVerificacion, Matricula, Calificacion...
    service/                   CalculadoraDeNotas, EvaluadorDeRiesgo, DetectorSemanaCargada
    exception/                 ReglaDeNegocioVioladaException, CredencialesInvalidasException...
  application/                 Casos de uso. Sin anotaciones de Spring
    port/in/                   Una interfaz por caso de uso (RegistrarEstudiante, IniciarSesion...)
    port/out/                  EstudianteRepositorio, SesionRepositorio, RelojPort, EnviadorDeCodigoPort...
    service/                   Implementaciones (RegistrarEstudianteServicio, IniciarSesionServicio...)
  infrastructure/              Lo único que conoce Spring, PostgreSQL, HTTP, PDF o Web Push
    config/                    @Configuration que ensambla casos de uso, seguridad, JWT, CORS, OpenAPI
    adapter/in/rest/           Controladores, DTOs (record), manejador global de errores
    adapter/in/scheduler/      Tareas @Scheduled
    adapter/out/persistence/   Entidades JPA, repositorios Spring Data y adaptadores
    adapter/out/security/      bcrypt, emisión de JWT, límite de intentos
    adapter/out/clock/         Reloj del sistema
    adapter/out/notification/  Envío del código de verificación, Web Push, aviso en la app
    adapter/out/importing/     Adaptadores PDFBox por tipo de reporte (sprint 2)
```

Equivalencia con las capas que pide la review: **Controller** = `infrastructure/adapter/in/rest`, **Service/UseCase** = `application/service` (detrás de `application/port/in`), **Repository** = `application/port/out` (el contrato) más `infrastructure/adapter/out/persistence` (la implementación con JPA).

**Convención de nombres.** Carpetas, paquetes y nombres técnicos en inglés, como en un equipo de desarrollo real. El vocabulario del negocio (Estudiante, Sesion, Matricula, Calificacion...) se mantiene en español porque es el lenguaje de la universidad y de los usuarios: así el código y las conversaciones con el cliente usan las mismas palabras. No se renombran migraciones de Flyway ya aplicadas ni las rutas de la API.

Los paquetes se crean cuando llega el primer archivo que los necesita, no antes. Si una carpeta pasa de unos 10 archivos, se subdivide por módulo (`account`, `schedule`, `grading`, `tasks`, `guide`, `importing`, `notifications`).

### Patrón a repetir en cada caso de uso

1. Modelo de dominio con comportamiento: valida en el constructor y expone operaciones (`registrarNota`, `calcularNotaRequerida`).
2. Puerto de entrada: una interfaz por caso de uso.
3. Puerto de salida: lo que el núcleo necesita del mundo.
4. Caso de uso: clase sin anotaciones de Spring que recibe los puertos por constructor.
5. Entidad JPA separada del dominio, repositorio Spring Data y adaptador que implementa el puerto de salida.
6. Ensamblado del caso de uso como `@Bean` en una clase `@Configuration` de infraestructura.
7. Controlador que depende de la interfaz del caso de uso y responde con DTOs, nunca con el modelo de dominio.

### Puertos del sistema

| Puerto de salida | Qué abstrae | Adaptador hoy | Adaptador futuro |
|---|---|---|---|
| Repositorios | Persistencia | JPA y PostgreSQL | Otro motor sin tocar el núcleo |
| `FuenteDeHorarioPort`, `FuenteHistorialAcademicoPort`, `FuentePlanDeEstudiosPort` | Origen de datos académicos | PDF (PDFBox) y registro manual | API institucional autorizada |
| `FuenteDeCalificacionesPort` | Origen de notas | Registro manual | Web Services de Moodle autorizados |
| `FuenteDeInformacionInstitucionalPort` | Documentos públicos (RF11) | `DocumentoOficialAdapter` | `PaginaPublicaAdapter` |
| `NotificadorPort` | Envío de avisos | Web Push y aviso en la app | Correo u otro canal |
| `EnviadorDeCodigoPort` | Código de verificación del correo | SMTP | Otro proveedor |
| `VerificadorDeIdentidadExternaPort` | Validar el ID token de Google | Google OIDC | Microsoft Entra ID |
| `RelojPort` | La hora actual | Reloj del sistema | Reloj fijo en pruebas |

Cada puerto existe por una razón concreta (probar sin base de datos, fuente intercambiable, integración futura, cambiar el canal de aviso). Un puerto sin motivo es complejidad.

## 10. API REST

- Prefijo `/api`. Las rutas públicas van bajo `/api/publico/**` (solo RF11 y los endpoints de registro, inicio de sesión y refresco); todo lo demás exige JWT.
- Recursos en plural y en español: `/api/matriculas/{id}/categorias`, `/api/pendientes`.
- El estudiante se toma del token, nunca de un parámetro: `/api/mis/pendientes`, no `/api/estudiantes/{id}/pendientes`.
- Validación de entrada con Bean Validation en los DTOs; las reglas de negocio se validan en el dominio.
- Errores con `ProblemDetail` (RFC 9457): 400 validación, 401 sin sesión, 403 sin permiso, 404 no encontrado, 409 conflicto (correo ya registrado), 422 regla de negocio (pesos que no suman 100 %).
- Documentación automática con springdoc en `/swagger-ui.html`, deshabilitada en PROD.
- Logs como flujo de eventos (SLF4J a la salida estándar), sin datos personales ni tokens.

## 11. Seguridad (RF01)

- **Registro:** correo institucional único, contraseña con bcrypt (fuerza 12) y consentimiento de datos obligatorio con fecha. Antes de activar la cuenta o vincular Google se verifica el correo institucional con un código de 6 dígitos que vence en 15 minutos y permite máximo 5 intentos.
- **Inicio con contraseña:** devuelve un JWT de acceso (15 a 30 min, firmado, sin estado) y un token de refresco (7 días) del que la base solo guarda la huella SHA-256 en `sesion`. Credenciales incorrectas responden con un mensaje genérico, sin revelar si el correo existe. Se limitan los intentos por IP y por correo.
- **Google:** el frontend obtiene el ID token con Google Identity Services y el backend lo verifica (firma, `aud`, `iss` y vencimiento) antes de crear o vincular la credencial con el `sub`. El correo institucional es la identidad de la cuenta.
- **Sesiones:** listar sesiones vigentes, revocar una o todas, rotar el refresco en cada uso y revocar toda la cadena si se detecta reutilización.
- **Token en el frontend:** el de acceso vive en memoria (no en `localStorage`). El de refresco, en cookie `HttpOnly; Secure; SameSite`, ruta `/api/publico/auth`. Si el hosting deja frontend y backend en dominios distintos, se revisa la política SameSite y CORS con credenciales y se registra la decisión en un ADR.
- CORS restringido a los orígenes de cada ambiente. CSRF aplica solo a la ruta de la cookie de refresco.

## 12. Pruebas

| Nivel | Qué | Herramienta | Meta |
|---|---|---|---|
| Unitaria de dominio | Cálculos, Brújula, semana cargada, validaciones | JUnit 5 puro, sin Spring | 80 % de cobertura (JaCoCo sobre `domain`) |
| Caso de uso | Orquestación con dobles de los puertos | JUnit y Mockito | Caminos principales y de error |
| Adaptador | Repositorios contra PostgreSQL real | Testcontainers (`postgres:16`) y Flyway | Consultas y restricciones críticas |
| API | Controladores, códigos HTTP y seguridad | `@WebMvcTest` y MockMvc | Cada endpoint |
| Arquitectura | Reglas de la sección 3 | ArchUnit | Siempre en verde |

Prueba estrella para la sustentación (sprint 2): el motor académico alimentado con el Registro Académico Extendido debe reproducir exactamente los promedios por período y el acumulado que calcula la universidad. El motor académico se desarrolla con TDD: primero la prueba, luego el cálculo.

Reglas mínimas de ArchUnit: el dominio no depende de `org.springframework`, `jakarta.persistence`, `com.fasterxml.jackson`, aplicación ni infraestructura; la aplicación no depende de Spring, JPA ni infraestructura; y las capas respetan el sentido de las dependencias (infraestructura → aplicación → dominio).

## 13. Integración continua y despliegue

Flujos en `.github/workflows/`:

- `ci.yml`: en cada pull request hacia `desarrollo`, `preproduccion` y `produccion`, con Temurin 21 y caché de Maven, ejecuta `./mvnw -B verify` (compila, pruebas unitarias, Testcontainers, ArchUnit y JaCoCo con umbral de 80 % en dominio). Es check obligatorio para fusionar.
- `desplegar-pre.yml`: en push a `preproduccion`, construye y despliega al hosting PRE; Flyway migra la base PRE al arrancar.
- `desplegar-prod.yml`: en push a `produccion`, igual contra PROD, y crea la etiqueta de versión.
- `latido-supabase.yml`: programado, mantiene activo Supabase.

Los secretos (`DB_URL`, `DB_PASSWORD`, `JWT_SECRETO` y tokens del hosting) se guardan en Settings → Secrets and variables → Actions, idealmente por Environment (`pre` y `prod`) con aprobación manual para `prod`.

## 14. Decisiones abiertas

Se resuelven con el equipo y se registran como ADR en `docs/adr/`.

| Decisión | Recomendación |
|---|---|
| Hosting del backend en PRE y PROD | Servicio con despliegue desde GitHub y soporte de Docker; considerar que los planes gratuitos se suspenden por inactividad |
| Hosting del frontend | Hosting estático con HTTPS |
| Proveedor de correo para el código de verificación | SMTP de un servicio con plan gratuito, detrás de `EnviadorDeCodigoPort` |
| Dominios y cookie de refresco | Mismo sitio si el hosting lo permite; si no, ajustar SameSite y CORS |
| Versión exacta de Spring Boot | La estable vigente al generar el proyecto |
