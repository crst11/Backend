# CundiApp - Backend

API REST de CundiApp, la plataforma centralizada de información académica para los estudiantes de la Universidad de Cundinamarca (programa piloto: Ingeniería de Sistemas, sede Fusagasugá).

Proyecto integrador de Ingeniería de Software I (2026-2).

## Tecnologías previstas

- Java 21 y Spring Boot
- PostgreSQL 16 con migraciones Flyway
- Arquitectura hexagonal (dominio, aplicación e infraestructura)

## Ramas

| Rama | Uso |
|---|---|
| `feature/CUN-XX-descripcion` | Trabajo de una historia de Jira |
| `desarrollo` | Integración del sprint en curso |
| `preproduccion` | Demostración ante el Comité de Arquitectura |
| `main` | Producción |

El código fluye siempre de la rama de la historia a `desarrollo`, luego a `preproduccion` y por último a `main`, siempre mediante pull request o merge.

## Autores

- Cristian Albeiro Morales Urrea
- Naomi Xannathl Ochoa Trejo
