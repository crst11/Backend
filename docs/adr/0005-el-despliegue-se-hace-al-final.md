# ADR 0005 - El despliegue se hace al final del proyecto

- **Estado:** aceptada
- **Fecha:** 1 de octubre de 2026
- **Incidencia:** SCRUM-71

## Contexto

El plan original ponía el primer despliegue en el Sprint 1 (SCRUM-43) y pedía, en la Definición de Terminado, que cada funcionalidad quedara "desplegada en preproducción y verificable por un tercero". El tablero decía lo mismo: una tarjeta llegaba a Hecho solo tras ese despliegue.

Al cerrar el Sprint 1 ese ambiente no existe. El hosting de preproducción y producción sigue como decisión abierta en el ADR 0001, y `environment.pre.ts` todavía apunta a `PENDIENTE-DECIDIR-HOSTING-PRE`. El resultado fue que diecisiete incidencias terminadas y fusionadas se quedaron en "En revisión": el tablero dejó de reflejar el avance real.

## Decisión

El equipo construye y prueba todo en local, y deja el despliegue para el final del proyecto.

- Levantar la app completa con Docker Compose (SCRUM-70) y desplegar en preproducción y producción (SCRUM-71) pasan al Sprint 7.
- SCRUM-43 pierde su criterio de despliegue, que se traslada a SCRUM-71, y se cierra con lo que sí quedó hecho: el esqueleto conectado, la arquitectura verificada con ArchUnit, la integración continua y la PWA.
- Mientras no exista preproducción, una tarjeta llega a Hecho al fusionarse en `desarrollo` con el CI en verde. La Definición de Terminado pide, en su lugar, que la funcionalidad sea verificable en local siguiendo el README.
- Las ramas `preproduccion` y `produccion` siguen funcionando como hasta ahora: son dónde vive el código promovido, no un ambiente desplegado.

## Consecuencias

- El tablero vuelve a decir la verdad y el avance se puede medir sprint a sprint.
- Se gana tiempo de los primeros sprints para construir funcionalidad, que es lo que se demuestra ante el Comité.
- El costo es real y conviene tenerlo presente: el primer despliegue llega tarde, y los problemas que solo aparecen fuera de la máquina de desarrollo (variables de entorno, dominios y cookies, latencia de la base, cuotas del correo) se van a descubrir en el Sprint 7. Por eso SCRUM-71 incluye revisar `SameSite` y CORS, y el latido de Supabase.
- La demostración ante el Comité se hace en local hasta entonces, como ya se hizo en la Review 1 por indicación del profesor.
- La alternativa era desplegar temprano para amortizar ese riesgo. Se descartó porque exige decidir y pagar hosting antes de tener funcionalidad que mostrar.
