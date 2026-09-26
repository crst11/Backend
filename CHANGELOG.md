# Cambios

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y versionado semántico. Cada versión corresponde a lo que se promueve a la rama `produccion`.

## [Sin publicar]

### Agregado
- Guía institucional completa (SCRUM-19): 19 documentos oficiales de la universidad (reglamentos, calendario, trámites, plantillas de Word, Excel y PowerPoint, convocatorias y plataformas) en 5 categorías, cargados con la migración V4 y enlazados a su fuente oficial. `GET /api/publico/guia/recursos` busca por título, descripción y categoría sin distinguir tildes, mayúsculas ni plural; `GET /api/publico/guia/sugerencias` devuelve los temas sugeridos, configurables en `cundiapp.guia.sugerencias`.
- El código de verificación llega al correo institucional por SMTP (Gmail con contraseña de aplicación), con una plantilla HTML con los colores de la app y versión en texto plano. En local, sin `CORREO_SMTP_HOST`, sigue saliendo en la consola.
- Si el correo no sale, la API responde 503 `Correo no enviado`; en el registro el mensaje aclara que la cuenta quedó creada y que se puede pedir otro código.

- Inicio de sesión con Google (SCRUM-48), la API externa del proyecto: `POST /api/publico/auth/google` para entrar y `GET`, `POST` y `DELETE /api/mis/google` para ver, vincular y quitar el vínculo. El backend valida el ID token con las llaves públicas de Google (firma, emisor, destinatario y vigencia). Una cuenta de Google solo se vincula a una cuenta, y solo si el correo institucional ya está verificado.
- Las sesiones guardan con qué método se abrieron (`proveedor_origen`: `local` o `google`) y lo conservan al renovarse.
- Colección de Postman: carpeta *5. Inicio con Google*, con sus casos de error y dos peticiones con un token real que se saltan si no hay token.

### Cambiado
- En preproducción y producción `CORREO_SMTP_HOST`, `CORREO_SMTP_USUARIO` y `CORREO_SMTP_CLAVE` son obligatorias, igual que `GOOGLE_CLIENT_ID`.
- La apertura de sesión se comparte entre el login con contraseña y el de Google (`AbridorDeSesion`).
- `/actuator/health` no revisa el servidor de correo.
- ArchUnit analiza solo el código de producción.

## [0.1.0] - 2026-09-25 · Sprint 1 (Review 1)

### Agregado
- Esquema completo de la base de datos con Flyway: 28 tablas, 4 vistas y las 14 restricciones de integridad del documento del proyecto, más los datos de arranque (plantilla de evaluación 30/30/40 y categorías de la guía).
- Guía institucional: `GET /api/publico/guia/categorias`.
- Registro con correo institucional (`@ucundinamarca.edu.co`), contraseña con bcrypt y consentimiento de datos con fecha (Ley 1581 de 2012).
- Verificación del correo con código de 6 dígitos: vence a los 15 minutos, máximo 5 intentos, reenvío cuando el anterior venció.
- Inicio y cierre de sesión: JWT de acceso de 20 minutos, token de refresco de 7 días en cookie HttpOnly que se rota en cada uso, detección de reutilización, límite de intentos fallidos y protección CSRF en las rutas que usan la cookie.
- Ruta protegida `GET /api/mis/cuenta`: el estudiante se toma del token.
- Errores en formato RFC 9457 (`application/problem+json`) y logs de eventos sin datos personales.
- Colección de Postman con el flujo de demostración, casos de error y pruebas automáticas.
- Integración continua: compilación, pruebas, ArchUnit y cobertura del dominio en cada pull request.

### Cambiado
- Carpetas y paquetes en inglés (`domain`, `application`, `infrastructure`); el vocabulario del negocio se mantiene en español.

### Corregido
- La regla de cobertura del 80 % en el dominio no filtraba ninguna clase; ahora mide `domain` de verdad.

### Limitaciones conocidas
- El código de verificación se muestra en la consola en desarrollo; el envío por correo (SMTP) está pendiente.
- El límite de intentos se lleva en memoria de un solo proceso.
- Se presenta en local; el despliegue en preproducción espera la decisión de hosting.
