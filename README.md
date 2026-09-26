# CundiApp - Backend

API REST de CundiApp, la plataforma centralizada de información académica para los estudiantes de la Universidad de Cundinamarca (programa piloto: Ingeniería de Sistemas, sede Fusagasugá).

Proyecto integrador de Ingeniería de Software I (2026-2).

## Objetivo

Reunir en un solo lugar el horario, las notas, los salones y las fechas del estudiante, y interpretarlos a tiempo: calcular la nota que necesita para alcanzar su meta, clasificar el riesgo de cada asignatura y avisarle antes de que se venza una entrega. El backend expone esa lógica mediante una API REST que consume la aplicación web (repositorio Frontend).

## Estado del proyecto (Review 1)

| Funcionalidad | Estado |
|---|---|
| Esquema de base de datos: 28 tablas, 4 vistas y las 14 restricciones de integridad (Flyway) | Hecho |
| RF01 · Registro con correo institucional | Hecho |
| RF01 · Verificación del correo con código de 6 dígitos | Hecho: el código llega al correo institucional por SMTP (en local, sin SMTP configurado, sale en la consola) |
| RF01 · Iniciar y cerrar sesión con JWT y refresco rotativo | Hecho |
| RF11 · Guía institucional | Parcial: lista de categorías |
| RF02 a RF10 y RF12 | Siguientes sprints |

## Tecnologías

- Java 21 y Spring Boot 4 (Web, Validation, Data JPA, Security, OAuth2 Resource Server)
- PostgreSQL 16 con migraciones Flyway (Hibernate solo valida el esquema)
- Arquitectura hexagonal verificada con ArchUnit; cobertura mínima del 80 % en el dominio con JaCoCo
- JUnit 5, Mockito y Testcontainers
- Docker para la base de datos local

## Arquitectura

```
src/main/java/co/edu/ucundinamarca/cundiapp
  domain/            reglas de negocio en Java puro (model, exception)
  application/       casos de uso: port/in (contratos), service (implementación), port/out (lo que necesitan)
  infrastructure/    config, adapter/in/rest (controllers), adapter/out/{persistence,security,clock,notification}
```

Petición típica: `Controller → UseCase → Dominio → puerto → Repository (JPA) → PostgreSQL`. El detalle, con diagramas, está en [docs/review-1/architecture.md](docs/review-1/architecture.md).

## Requisitos previos

- Java 21
- Docker Desktop
- Git

## Levantar en local

```powershell
Copy-Item .env.example .env      # la primera vez; define DB_PASSWORD con una clave local
docker compose up -d --wait      # PostgreSQL 16 en Docker
.\mvnw.cmd spring-boot:run       # Flyway crea el esquema al arrancar
```

La API queda en `http://localhost:8080/api`. `.env` para la base local:

```properties
DB_URL=jdbc:postgresql://localhost:5432/cundiapp
DB_USERNAME=cundiapp
DB_PASSWORD=una_clave_local
COOKIE_SECURE=false
```

### Correo del código de verificación

Sin `CORREO_SMTP_HOST`, en local el código sale en la consola del backend (`[DEV] Código de verificación para ...`). Para recibirlo en el correo institucional, con una cuenta de Gmail del proyecto:

1. En esa cuenta, activar la verificación en dos pasos y crear una *contraseña de aplicación* (Cuenta de Google → Seguridad → Contraseñas de aplicaciones).
2. Agregar al `.env`:
   ```properties
   CORREO_SMTP_HOST=smtp.gmail.com
   CORREO_SMTP_USUARIO=la.cuenta@gmail.com
   CORREO_SMTP_CLAVE=laclavedeaplicacionsinespacios
   ```
3. Reiniciar el backend. En preproducción y producción estas tres variables son obligatorias.

Si el servidor de correo rechaza el envío, la API responde 503 (`Correo no enviado`) y el log dice si fue el usuario o la clave.

`JWT_SECRETO` no hace falta en local (hay un valor de desarrollo); en preproducción y producción es obligatorio. No dejes una línea `JWT_SECRETO=` vacía: anula ese valor y el backend no arranca.

La guía completa de la base de datos (mirar las tablas, llevar el esquema a Supabase y resolver problemas) está en [docs/base-de-datos.md](docs/base-de-datos.md).

## API

| Método y ruta | Uso | Sesión |
|---|---|---|
| `GET /api/publico/guia/categorias` | Categorías de la guía institucional | No |
| `POST /api/publico/auth/registro` | Crear cuenta | No |
| `POST /api/publico/auth/verificacion` | Verificar el correo con el código | No |
| `POST /api/publico/auth/verificacion/reenvio` | Pedir otro código | No |
| `POST /api/publico/auth/login` | Iniciar sesión | No |
| `POST /api/publico/auth/refresco` | Renovar la sesión (cookie + `X-XSRF-TOKEN`) | Cookie |
| `POST /api/publico/auth/logout` | Cerrar sesión | Cookie |
| `GET /api/mis/cuenta` | Datos de mi cuenta | `Bearer` |

Los errores siguen el formato `application/problem+json` (RFC 9457). La colección de Postman con todas las peticiones, sus casos de error y pruebas automáticas está en [docs/postman](docs/postman/CundiApp.postman_collection.json).

## Pruebas

```powershell
.\mvnw.cmd clean verify
```

Necesita Docker encendido: Testcontainers levanta un PostgreSQL 16 temporal. Ejecuta las pruebas de dominio, casos de uso, adaptadores, API y seguridad, las reglas de arquitectura (ArchUnit) y la cobertura mínima del dominio (JaCoCo).

## Ramas y flujo de trabajo

| Rama | Uso |
|---|---|
| `feature/SCRUM-XX-descripcion` | Trabajo de una incidencia de Jira |
| `desarrollo` | Integración del sprint en curso (rama por defecto) |
| `preproduccion` | Demostración ante el Comité de Arquitectura |
| `produccion` | Versión presentada y aceptada |

El código fluye de la rama de la incidencia a `desarrollo`, luego a `preproduccion` y por último a `produccion`. Los commits siguen el formato `tipo(módulo): descripción SCRUM-XX`. Los cambios de cada versión están en [CHANGELOG.md](CHANGELOG.md).

## Documentación

- [Review 1: arquitectura](docs/review-1/architecture.md), [casos de uso e historias](docs/review-1/use-cases.md) y [guion de la demo](docs/review-1/demo-script.md).
- [Guía del proyecto](docs/guia-proyecto.md): contexto, reglas, arquitectura, ambientes, pruebas y flujo de trabajo.
- [Base de datos, paso a paso](docs/base-de-datos.md) y [modelo de datos](docs/modelo-de-datos.md).
- [Decisiones de arquitectura](docs/adr/) (ADR).

## Autores

- Cristian Albeiro Morales Urrea
- Naomi Xannathl Ochoa Trejo
