# CundiApp - Backend

API REST de CundiApp, la plataforma centralizada de información académica para los estudiantes de la Universidad de Cundinamarca (programa piloto: Ingeniería de Sistemas, sede Fusagasugá).

Proyecto integrador de Ingeniería de Software I (2026-2).

## Objetivo

Reunir en un solo lugar el horario, las notas, los salones y las fechas del estudiante, y interpretarlos a tiempo: calcular la nota que necesita para alcanzar su meta, clasificar el riesgo de cada asignatura y avisarle antes de que se venza una entrega. El backend expone esa lógica mediante una API REST que consume la aplicación web (repositorio Frontend).

## Estado del proyecto

Repositorio base. Contiene las reglas del equipo, la guía del proyecto y las plantillas de trabajo. El proyecto Spring Boot y la base de datos se agregan en las tareas SCRUM-43 y SCRUM-41 del tablero de Jira; este README se completa con ellas.

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

Se documenta aquí, paso a paso y para levantar el proyecto en menos de diez minutos, cuando exista el proyecto (SCRUM-43). La configuración va siempre en variables de entorno: se parte de `.env.example`, que se copia como `.env` y nunca se sube a Git.

## Uso

La documentación de la API se publica con OpenAPI (springdoc) en el ambiente local y en preproducción, y se deshabilita en producción.

## Pruebas

El estándar del proyecto es que `./mvnw verify` ejecute las pruebas unitarias del dominio, las de integración con Testcontainers y las reglas de arquitectura con ArchUnit, con cobertura mínima del 80 % en el dominio.

## Ramas y flujo de trabajo

| Rama | Uso |
|---|---|
| `feature/SCRUM-XX-descripcion` | Trabajo de una incidencia de Jira |
| `desarrollo` | Integración del sprint en curso (rama por defecto) |
| `preproduccion` | Demostración ante el Comité de Arquitectura |
| `main` | Producción |

El código fluye siempre de la rama de la incidencia a `desarrollo`, luego a `preproduccion` y por último a `main`, mediante pull request o merge. Ninguna rama de ambiente admite push directo. Los commits siguen el formato `tipo(módulo): descripción SCRUM-XX`.

## Documentación

- [Guía del proyecto](docs/guia-proyecto.md): contexto, reglas, arquitectura, ambientes, pruebas y flujo de trabajo.
- [Decisiones de arquitectura](docs/adr/): registro de las decisiones tomadas (ADR).

## Autores

- Cristian Albeiro Morales Urrea
- Naomi Xannathl Ochoa Trejo
