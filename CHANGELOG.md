# Cambios

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y versionado semántico. Cada versión corresponde a lo que se promueve a la rama `produccion`.

## [Sin publicar]

### Agregado
- Mi historial y mis promedios (SCRUM-22): `GET /api/mis/historial` devuelve las asignaturas cursadas agrupadas por período, con su nota y su estado, el promedio de cada período y el acumulado, los créditos aprobados sobre el total del programa y el porcentaje de avance. El motor académico vive en el dominio (`HistorialAcademico`, `PeriodoCursado`, `AsignaturaCursada`) y se desarrolló con TDD contra un Registro Académico Extendido real: reproduce exactos los créditos por período (16-18-17-16) y los 67 aprobados. El promedio es ponderado por créditos, no el promedio simple de las notas, y las de diagnóstico y nivelatorio (0 créditos) no ponderan. Una asignatura sin nota todavía no entra al promedio: contarla como 0.0 bajaría la nota con un número que nadie puso.
- **Cuando el reporte oficial trae el promedio, es ese el que se muestra**, y la respuesta lo dice en `fuenteDelPromedio`. Academusoft publica las notas con un decimal pero las guarda con dos, así que nuestro cálculo puede diferir en centésimas: en el reporte de referencia un período trunca (4.456 → 4.4) y otro redondea (4.575 → 4.6), de modo que ninguna regla única los explica. Contradecirle a la universidad su propia nota por una centésima que ni siquiera publica sería un error de la app. El motor propio cubre lo que todavía no tiene reporte, como el período en curso, y es la base del simulador y la Brújula. La prueba del motor fija la fórmula con una tolerancia de 0.1, que no es un número a ojo: son los dos redondeos que se acumulan, el de cada nota y el del promedio impreso.

### Agregado
- Acceso a las aulas virtuales en la guía institucional (SCRUM-20, migración V7): la categoría *Plataformas* gana el Campo Multidimensional de Aprendizaje, que es como la universidad llama a sus aulas de Moodle. El título lleva los dos nombres porque el oficial es el que publica la universidad y "Moodle" es el que usa la gente; así la búsqueda lo encuentra de las dos formas. El enlace va a la página oficial del CMA y no a un subdominio de ingreso (`pregrado`, `cma`, `institucion`): es el mismo criterio de las otras filas de *Plataformas*, que apuntan a la página informativa y no al login, y no se rompe si la universidad cambia de subdominio. Con esto la guía queda en 20 recursos y la prueba que fijaba una sola fecha de verificación para todos pasa a comprobar que cada recurso tenga la suya, porque cada uno se verifica el día que entra.
- Elegir mi programa y ver mi plan de estudios (SCRUM-21): `GET /api/programas` lista los programas que ya tienen ruta cargada, `GET /api/programas/{codigo}/plan` devuelve la ruta agrupada por período con los créditos de cada uno y los prerrequisitos de cada asignatura, y `GET`/`PUT /api/mis/perfil-academico` consultan y cambian el programa del estudiante. Agrupar por período vive en el dominio (`PlanDeEstudios`), no en el controlador ni en la pantalla: es cómo se lee un plan, no cómo se dibuja. Elegir programa deja fijado el plan, que es contra lo que después se comparan el historial y el avance. Sin elegir, el perfil lo dice en vez de fallar. Dentro de cada período las asignaturas van en orden alfabético del español: comparando por código de carácter la "Á" de Álgebra Lineal cae después de la "D" de Diagnóstico, y la lista se vería al revés. El catálogo no va bajo `/api/mis/**` porque no es información del estudiante, así que la configuración de seguridad gana una regla propia para `/api/programas/**`, que exige sesión igual.
- Mis sesiones (SCRUM-49): `GET /api/mis/sesiones` lista desde qué dispositivos hay una sesión abierta, con el método de acceso, cuándo empezó, hasta cuándo vale y desde qué IP. `DELETE /api/mis/sesiones/{consecutivo}` cierra una y `DELETE /api/mis/sesiones` las cierra todas, incluida la de quien lo pide: quien usa esa opción suele sospechar que alguien más entró, y dejar viva la suya le obligaría a cerrarla aparte. Una sesión cerrada deja de renovar en el acto. Nunca viaja la huella del token de refresco. Cerrar una que ya estaba cerrada responde 422 y no 401, porque un 401 haría creer que la sesión de quien pide es la que murió.
- Ruta de aprendizaje de Ingeniería de Sistemas y Computación, sede Fusagasugá (SCRUM-21, migración V6): el programa con sus 153 créditos y 9 períodos, las 60 asignaturas con su código oficial, créditos y período sugerido, y los 64 prerrequisitos. Los datos salen de *Consultar Ruta de Aprendizaje* de Academusoft, que es el sistema de la universidad. Las de diagnóstico y nivelatorio (prefijo `DN-`) valen 0 créditos: se cursan pero no ponderan. Hay pruebas que comprueban que los créditos por período dan 16-18-17-16-17-18-17-18-16 y que suman los 153 que publica la universidad; si alguien edita la migración y se equivoca, el CI lo caza. Otra prueba verifica que ningún prerrequisito quede en un período posterior al de la asignatura que lo exige.

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
