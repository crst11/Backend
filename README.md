# CundiApp - Backend

API REST de CundiApp, la plataforma centralizada de información académica para los estudiantes de la Universidad de Cundinamarca (programa piloto: Ingeniería de Sistemas, sede Fusagasugá).

Proyecto integrador de Ingeniería de Software I (2026-2).

## Objetivo

Reunir en un solo lugar el horario, las notas, los salones y las fechas del estudiante, y interpretarlos a tiempo: calcular la nota que necesita para alcanzar su meta, clasificar el riesgo de cada asignatura y avisarle antes de que se venza una entrega. El backend expone esa lógica mediante una API REST que consume la aplicación web (repositorio Frontend).

## Estado del proyecto

Proyecto Spring Boot base con el esquema completo de la base de datos (27 tablas, 4 vistas y las 14 restricciones de integridad), que crean las migraciones de Flyway. Los casos de uso y la API REST llegan en las siguientes tareas del tablero de Jira.

## Tecnologías

- Java 21 y Spring Boot
- PostgreSQL 16 con migraciones Flyway
- Arquitectura hexagonal (dominio, aplicación e infraestructura)
- Maven Wrapper, JUnit 5, ArchUnit y Testcontainers
- Docker para la base de datos local

## Requisitos previos

- Java 21
- Docker
- Git

## Instalación y ejecución

La configuración va siempre en variables de entorno: se parte de `.env.example`, que se copia como `.env` y nunca se sube a Git.

```powershell
Copy-Item .env.example .env      # completa DB_URL, DB_USERNAME y DB_PASSWORD
docker compose up -d --wait      # PostgreSQL 16 en Docker
./mvnw spring-boot:run           # Flyway crea el esquema al arrancar
```

Con `.env` así (base local en Docker):

```properties
DB_URL=jdbc:postgresql://localhost:5432/cundiapp
DB_USERNAME=cundiapp
DB_PASSWORD=una_clave_local
```

La guía completa, con cómo mirar las tablas, llevar el esquema a Supabase y resolver problemas, está en [docs/base-de-datos.md](docs/base-de-datos.md).

## Uso

La documentación de la API se publica con OpenAPI (springdoc) en el ambiente local y en preproducción, y se deshabilita en producción.

## Pruebas

```bash
./mvnw verify
```

Necesita Docker encendido: Testcontainers levanta un PostgreSQL 16 temporal, aplica las migraciones y ejecuta las pruebas del esquema (27 tablas, 204 campos, 4 vistas y datos de arranque) y una por cada una de las 14 restricciones de integridad. El estándar del proyecto es que `./mvnw verify` también ejecute las reglas de arquitectura con ArchUnit y exija cobertura mínima del 80 % en el dominio, a medida que existan.

## Ramas y flujo de trabajo

| Rama | Uso |
|---|---|
| `feature/SCRUM-XX-descripcion` | Trabajo de una incidencia de Jira |
| `desarrollo` | Integración del sprint en curso (rama por defecto) |
| `preproduccion` | Demostración ante el Comité de Arquitectura |
| `produccion` | Producción |

El código fluye siempre de la rama de la incidencia a `desarrollo`, luego a `preproduccion` y por último a `produccion`, mediante pull request o merge. Ninguna rama de ambiente admite push directo. Los commits siguen el formato `tipo(módulo): descripción SCRUM-XX`.

## Documentación

- [Guía del proyecto](docs/guia-proyecto.md): contexto, reglas, arquitectura, ambientes, pruebas y flujo de trabajo.
- [Base de datos, paso a paso](docs/base-de-datos.md): levantarla en Docker, llevarla a Supabase y mirarla.
- [Modelo de datos](docs/modelo-de-datos.md): diagramas, restricciones de integridad, vistas y vocabulario de estados.
- [Decisiones de arquitectura](docs/adr/): registro de las decisiones tomadas (ADR).

## Autores

- Cristian Albeiro Morales Urrea
- Naomi Xannathl Ochoa Trejo
