# ADR 0001 - Ramas de ambiente con main como producción

- **Estado:** aceptada
- **Fecha:** 21 de septiembre de 2026
- **Incidencia:** SCRUM-42

## Contexto

CundiApp se desarrolla en dos repositorios, `Backend` y `Frontend`, con dos integrantes y un Comité de Arquitectura al cierre de cada sprint de dos semanas. Hace falta un flujo de ramas que permita integrar el trabajo del sprint, demostrarlo y dejar aparte lo que el comité aceptó, sin que nadie escriba directamente en las ramas compartidas.

## Decisión

Se usan tres ramas de ambiente y ramas cortas por tarea:

| Rama | Rol |
|---|---|
| `feature/SCRUM-XX-descripcion` y `fix/SCRUM-XX-descripcion` | Trabajo de una incidencia, siempre creadas desde `desarrollo` |
| `desarrollo` | Integración del sprint en curso; es la rama por defecto |
| `preproduccion` | Lo que se demuestra ante el comité y donde se ajusta |
| `main` | Producción: contiene el código más limpio, el que el comité aceptó |

Reglas del flujo:

- El código fluye en una sola dirección, `feature` → `desarrollo` → `preproduccion` → `main`, siempre por pull request o merge. Ninguna rama de tarea salta de ambiente.
- `desarrollo`, `preproduccion` y `main` no admiten push directo ni force push ni borrado, y exigen pull request con una aprobación. El check de CI se vuelve obligatorio cuando exista (SCRUM-43).
- Los ajustes que salgan de la demostración en `preproduccion` se hacen en ramas nuevas hacia `desarrollo` y se promueven de nuevo. No se corrige directamente en `preproduccion`.
- Una corrección urgente en producción sale de `main`, se fusiona a `main` y se devuelve a `desarrollo` para que las ramas no diverjan.
- Las promociones entre ramas de ambiente se hacen con merge sin squash, para conservar el historial.
- Cada promoción a `main` lleva una etiqueta de versión semántica (`v0.1.0`, `v0.2.0`, ...) y una entrada en `CHANGELOG.md`.
- Los commits y los pull requests llevan la llave de Jira de la incidencia.

## Consecuencias

- Hay un punto claro donde se demuestra (`preproduccion`) y otro donde queda lo aceptado (`main`), lo que encaja con el ritmo por sprint y con la revisión del comité.
- El costo es mantener tres ramas largas y disciplina en las promociones y en las correcciones urgentes. Se acepta porque el equipo es pequeño y el flujo queda documentado.
- Un flujo con una sola rama principal y despliegue continuo sería más simple, pero no ofrece el espacio de demostración y ajuste que el curso pide en cada sprint.
