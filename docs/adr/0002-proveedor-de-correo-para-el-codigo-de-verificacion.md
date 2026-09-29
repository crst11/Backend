# ADR 0002 - Proveedor de correo para el código de verificación

- **Estado:** aceptada
- **Fecha:** 26 de septiembre de 2026
- **Incidencia:** SCRUM-47

## Contexto

La sección 14 de la guía del proyecto dejaba abierta la elección del proveedor que entrega el código de 6 dígitos con el que se verifica el correo institucional (RF01). El desarrollo local necesitaba mostrar el código en algún lado mientras la decisión no se tomaba, y esa salida de emergencia (la consola) no podía convertirse en la solución definitiva porque ningún estudiante ve su propia consola del servidor.

Dos caminos eran razonables para el tamaño actual del proyecto: un proveedor transaccional dedicado (SendGrid, Amazon SES o similar) o una cuenta de Gmail normal por SMTP con una contraseña de aplicación.

## Decisión

Se envía el código por SMTP con una cuenta de Gmail y contraseña de aplicación, detrás de `EnviadorDeCodigoPort`. En desarrollo local, si no hay `CORREO_SMTP_HOST` configurado, el código sigue saliendo por consola para no depender de credenciales reales en cada máquina del equipo. En preproducción y en producción las tres variables de SMTP (`CORREO_SMTP_HOST`, `CORREO_SMTP_USUARIO`, `CORREO_SMTP_CLAVE`) son obligatorias: sin ellas la aplicación no arranca. Si el envío falla, la API responde 503 y el registro aclara que la cuenta sí quedó creada y que se puede pedir un código nuevo, en vez de dejar al estudiante sin saber qué pasó.

## Consecuencias

- Sin costo y sin trámite de aprobación de dominio, lo que encaja con el tiempo y el presupuesto de un proyecto de curso.
- El puerto `EnviadorDeCodigoPort` ya aísla esta decisión: cambiar de Gmail a un proveedor transaccional es un adaptador nuevo, no una reescritura del caso de uso.
- Gmail impone un límite diario de envíos por cuenta que un proveedor transaccional no tendría. Aceptable mientras el número de estudiantes sea el de un piloto; se revisa si el volumen crece.
- Si el proyecto llega a tener un dominio institucional propio, la recomendación de la guía sigue siendo cambiar a un proveedor transaccional, y esta ADR queda superada por una nueva.
