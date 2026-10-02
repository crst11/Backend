# Cambios

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y versionado semántico. Cada versión corresponde a lo que se promueve a la rama `produccion`.

## [Sin publicar]

## [0.3.0] - 2026-10-01 · Sprint 1 (cierre)

### Agregado
- Cabeceras de seguridad en todas las respuestas (SCRUM-69): política de seguridad de contenido que no permite ejecutar ni incrustar nada (la API solo devuelve JSON), política de referente en `no-referrer`, HSTS de un año y política de permisos que apaga cámara, micrófono, ubicación, pagos y USB.
- Límite de correos por IP (SCRUM-69): registro, reenvío del código y recuperación admiten 20 solicitudes por IP cada 15 minutos, configurable con `LIMITE_CORREOS_POR_IP`. Sin esto, un guion podría usar la app para llenar de correos la bandeja de alguien o agotar la cuota del servidor. Es un filtro y no una regla del dominio porque limitar por IP es un asunto del transporte; las reglas de negocio (los 5 intentos del código, la espera de 60 segundos) siguen en el dominio. El inicio de sesión no entra: ya tiene su límite por intentos fallidos.
- Recuperar la contraseña olvidada (SCRUM-68): `POST /api/publico/auth/recuperacion` envía un código de 6 dígitos al correo institucional y `POST /api/publico/auth/recuperacion/confirmacion` define la contraseña nueva. El primero responde 202 siempre, exista o no la cuenta, para no revelar quién está registrado; una cuenta pendiente o que solo entra con Google recibe esa misma respuesta neutra. Al cambiarla se revocan todas las sesiones abiertas, porque quien recupera su contraseña suele sospechar que alguien más entró. La contraseña nueva cumple la política de SCRUM-66 y no puede ser igual a la anterior.
- Migración V5: la tabla `codigo_verificacion` gana la columna `proposito` y su llave primaria pasa a ser (estudiante, propósito). Así el código de recuperación reutiliza la vigencia, los intentos, el uso único y la huella SHA-256 que ya existían, en vez de duplicar la tabla y su lógica. El correo dice algo distinto según para qué se pidió el código.

### Agregado
- Política de contraseña segura (SCRUM-66): al crear una cuenta se exigen 8 caracteres con mayúsculas, minúsculas, números y símbolos. También se rechaza la que contenga el usuario del correo institucional y la que supere los 72 bytes, porque bcrypt ignora lo que pase de ahí y daría una falsa sensación de seguridad. La política vive en el dominio (`Contrasena`), no en el DTO, para que el registro y el restablecimiento exijan lo mismo sin repetirla. El 422 trae la regla completa en una sola frase, como la escriben Google y las demás plataformas, en vez de ir soltando lo que falta: enumerar los fallos uno a uno también le dice a quien ataca cuánto le queda. No se aplica al iniciar sesión: una cuenta creada antes sigue entrando con lo que tenía.

### Cambiado
- El código de verificación se envía fuera de la petición (SCRUM-67, ADR 0004). Antes el registro esperaba a que Gmail aceptara el correo: conexión, TLS, autenticación y entrega ocurrían dentro de la petición. Ahora responde apenas la cuenta queda guardada y el correo sale en un pool propio y acotado, para que un servidor de correo lento no consuma los hilos que atienden peticiones. Es un decorador del puerto `EnviadorDeCodigoPort`, así que la capa de aplicación no se entera de que hay hilos de por medio.
- Se puede pedir otro código a los 60 segundos del anterior, sin esperar los 15 minutos de vigencia: como el envío ya no informa su falla a tiempo, obligar a esperar dejaría atascada a la persona cuyo correo no llegó.
- El registro ya no responde 503 `Correo no enviado`: cuando responde, todavía no se sabe si el correo saldrá. Si falla queda en el log y la pantalla de verificación permite pedir otro código.

## [0.2.0] - 2026-09-29 · Sprint 1 (Review 1, avance)

### Agregado
- Guía institucional completa (SCRUM-19): 19 documentos oficiales de la universidad (reglamentos, calendario, trámites, plantillas de Word, Excel y PowerPoint, convocatorias y plataformas) en 5 categorías, cargados con la migración V4 y enlazados a su fuente oficial. `GET /api/publico/guia/recursos` busca por título, descripción y categoría sin distinguir tildes, mayúsculas ni plural; `GET /api/publico/guia/sugerencias` devuelve los temas sugeridos, configurables en `cundiapp.guia.sugerencias`.
- El código de verificación llega al correo institucional por SMTP (Gmail con contraseña de aplicación), con una plantilla HTML con los colores de la app y versión en texto plano. En local, sin `CORREO_SMTP_HOST`, sigue saliendo en la consola.
- Si el correo no sale, la API responde 503 `Correo no enviado`; en el registro el mensaje aclara que la cuenta quedó creada y que se puede pedir otro código.
- Tres sugerencias de búsqueda más en la guía institucional (SCRUM-19): *Primer ingreso*, *Correo institucional* y *Retiro de semestre*, cada una verificada para que encuentre el documento correcto.

- Inicio de sesión con Google (SCRUM-48), la API externa del proyecto: `POST /api/publico/auth/google` para entrar y `GET`, `POST` y `DELETE /api/mis/google` para ver, vincular y quitar el vínculo. El backend valida el ID token con las llaves públicas de Google (firma, emisor, destinatario y vigencia). Una cuenta de Google solo se vincula a una cuenta, y solo si el correo institucional ya está verificado.
- Las sesiones guardan con qué método se abrieron (`proveedor_origen`: `local` o `google`) y lo conservan al renovarse.
- Colección de Postman: carpeta *5. Inicio con Google*, con sus casos de error y dos peticiones con un token real que se saltan si no hay token.
- Eliminar cuenta (SCRUM-64): `DELETE /api/mis/cuenta` deja la cuenta inactiva (no la borra) y revoca todas sus sesiones vigentes. El login por contraseña y por Google ya rechazaban cualquier cuenta que no esté `ACTIVA`, así que queda sin forma de volver a entrar. Sin migración de base de datos: reutiliza el estado `INACTIVA` que ya existía sin usar. Registrarse de nuevo con el correo de una cuenta eliminada reescribe esa misma cuenta (nombre, contraseña y verificación del correo quedan como en un registro nuevo) en vez de rechazarla como correo repetido.
- Documentación automática de la API con Swagger (SCRUM-65): `/swagger-ui.html` y `/v3/api-docs` listan los cinco controladores reales, deshabilitados en producción.

### Cambiado
- La sugerencia de búsqueda "Derechos pecuniarios" pasó a llamarse "Costos de trámites" (SCRUM-19): más simple para el estudiante. El documento oficial conserva su nombre real.
- En preproducción y producción `CORREO_SMTP_HOST`, `CORREO_SMTP_USUARIO` y `CORREO_SMTP_CLAVE` son obligatorias, igual que `GOOGLE_CLIENT_ID`.
- La apertura de sesión se comparte entre el login con contraseña y el de Google (`AbridorDeSesion`).
- `/actuator/health` no revisa el servidor de correo.
- ArchUnit analiza solo el código de producción.

### Corregido
- La búsqueda de la guía institucional (SCRUM-19) comparaba por subcadena en el respaldo de singular/plural, así que "grados" también traía el Reglamento Estudiantil solo porque su descripción menciona "pregrado". Ahora la comparación es por palabra completa.

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
