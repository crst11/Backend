# CundiApp - Backend

Guía completa: @docs/guia-proyecto.md (leer antes de trabajar).

- Spring Boot y Java 21, arquitectura hexagonal. Paquetes: `dominio` (Java puro), `aplicacion` (puertos y casos de uso, sin Spring) e `infraestructura`.
- Los casos de uso se ensamblan como `@Bean` en `infraestructura/configuracion`. Las entidades JPA van separadas del dominio.
- BD: PostgreSQL, esquema `cundiapp`, migraciones Flyway en `src/main/resources/db/migration`, `ddl-auto=validate`. No cambiar el esquema sin aprobación.
- Ramas: trabajar solo en `feature/SCRUM-XX-descripcion` o `fix/SCRUM-XX-descripcion`, creadas desde `desarrollo`. Nunca push a `desarrollo`, `preproduccion` ni `main`.
- Commits: `tipo(módulo): descripción SCRUM-XX`, en español, pequeños y con un solo propósito.
- Cada tarea sigue el orden: base de datos, dominio y puerto, persistencia, caso de uso y API, pantalla Angular, despliegue en preproducción.
- Antes de proponer código, presentar el plan de la tarea y esperar aprobación. No inventar requerimientos: el alcance es RF01 a RF12.
- Antes de dar algo por terminado: `./mvnw verify` en verde (incluye ArchUnit y JaCoCo) y mostrar el resultado.
- Sin firmas de IA: nada de `Co-Authored-By` ni "Generated with" en commits, pull requests, código ni documentación.
- Sin secretos ni URLs fijas: todo por variables de entorno.
- Código, nombres de dominio, commits y documentación en español, salvo palabras técnicas estándar.
