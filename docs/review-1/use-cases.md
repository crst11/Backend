# Documento versión 2: casos de uso, historias de usuario y criterios de aceptación

Cubre lo que está implementado para la Review 1: **RF01 Gestión de cuenta y sesión** (registro, verificación del correo, inicio y cierre de sesión) y el arranque de **RF11 Guía institucional**. Cada historia está en Jira (proyecto SCRUM) con la misma llave.

Actores:
- **Visitante:** persona sin cuenta. Solo usa la guía institucional y puede registrarse.
- **Estudiante:** persona con cuenta activa y correo `@ucundinamarca.edu.co` verificado.

---

## HU-01 · Registrarme con mi correo institucional (SCRUM-17)

> Como **estudiante**
> quiero **crear mi cuenta con mi correo institucional y una contraseña**
> para **tener un espacio personal en CundiApp**.

**Criterios de aceptación**

| # | Criterio | Estado | Evidencia |
|---|---|---|---|
| 1 | Solo acepta correos del dominio `@ucundinamarca.edu.co` | Cumplido | 422 "El correo debe pertenecer al dominio institucional" |
| 2 | La contraseña se guarda cifrada | Cumplido | bcrypt fuerza 12: en la tabla `credencial_acceso` se ve `$2a$12$...` |
| 3 | Pide aceptar el tratamiento de datos (Ley 1581 de 2012) | Cumplido | Sin la casilla marcada: 422 |
| 4 | Queda registrada la fecha del consentimiento | Cumplido | Columna `estudiante.fecha_consentimiento` |
| 5 | La cuenta queda pendiente hasta verificar el correo | Cumplido | `estudiante.estado_cuenta = 'pendiente'` |
| 6 | No se pide ni se guarda el documento de identidad | Cumplido | El formulario y la tabla no tienen ese campo |

**CU-01 Registrarse**

- **Actor:** visitante.
- **Precondición:** el correo no tiene cuenta.
- **Flujo principal:**
  1. El visitante abre *Crear cuenta* y escribe nombres, apellidos, correo institucional y contraseña.
  2. Marca la aceptación del tratamiento de datos y envía el formulario.
  3. El sistema valida el formato (campos obligatorios, contraseña de 8 o más caracteres).
  4. El sistema valida las reglas de negocio (dominio institucional, consentimiento, correo no repetido).
  5. El sistema guarda la cuenta en estado pendiente con la contraseña cifrada y genera un código de verificación.
  6. El sistema responde **201 Created** y lleva al visitante a la pantalla de verificación.
- **Flujos alternos:** datos incompletos o contraseña corta → **400**; correo no institucional o sin consentimiento → **422**; correo ya registrado → **409**.
- **Postcondición:** existe la cuenta pendiente y un código de verificación vigente.
- **API:** `POST /api/publico/auth/registro`.

---

## HU-02 · Verificar mi correo institucional con un código (SCRUM-47)

> Como **estudiante**
> quiero **recibir un código en mi correo institucional**
> para **confirmar que la cuenta es mía**.

| # | Criterio | Estado | Evidencia |
|---|---|---|---|
| 1 | Al registrarme se envía un código de 6 dígitos al correo institucional | Cumplido | Llega por SMTP (Gmail) con una plantilla de la app, a través del puerto `EnviadorDeCodigoPort`. En local sin SMTP configurado sale en la consola del backend. Si el correo no sale: 503 y se puede pedir otro código. |
| 2 | El código vence a los 15 minutos y permite máximo 5 intentos | Cumplido | 422 "El código es incorrecto. Te quedan 4 intentos"; al quinto fallo se bloquea |
| 3 | La cuenta no se activa hasta verificar el código | Cumplido | Sin verificar, iniciar sesión responde 403 |
| 4 | Puedo pedir un código nuevo si el anterior venció | Cumplido | `POST .../reenvio`: 202; si el actual sigue vigente, 422 |

**CU-02 Verificar el correo**

- **Actor:** estudiante con cuenta pendiente.
- **Flujo principal:**
  1. El estudiante escribe su correo y el código de 6 dígitos.
  2. El sistema compara la huella SHA-256 del código (el código nunca se guarda en claro).
  3. Si coincide y no ha vencido, activa la cuenta y marca el correo como verificado.
  4. El sistema responde **200 OK** con la cuenta en estado `activa`.
- **Flujos alternos:** código incorrecto → **422** y se descuenta un intento; vencido o con 5 intentos → **422** pidiendo uno nuevo; formato distinto de 6 dígitos → **400**; cuenta ya verificada → **422**.
- **API:** `POST /api/publico/auth/verificacion` y `POST /api/publico/auth/verificacion/reenvio`.

---

## HU-03 · Iniciar y cerrar sesión (SCRUM-18)

> Como **estudiante**
> quiero **iniciar y cerrar sesión de forma segura**
> para **proteger mi información académica**.

| # | Criterio | Estado | Evidencia |
|---|---|---|---|
| 1 | Credenciales incorrectas muestran un mensaje claro | Cumplido | 401 "Correo o contraseña incorrectos" |
| 2 | La sesión expira tras un tiempo sin uso | Cumplido | Token de acceso de 20 minutos; refresco de 7 días que cambia en cada uso |
| 3 | Cerrar sesión borra los datos del dispositivo | Cumplido | El token vive solo en memoria; el cierre borra las cookies y revoca la sesión en la base de datos |
| 4 | El mensaje de error no revela si el correo existe | Cumplido | Mismo mensaje y mismo tiempo de respuesta para correo inexistente y contraseña errada |
| 5 | Se limitan los intentos fallidos seguidos | Cumplido | 5 por correo y 20 por IP → **429** durante 15 minutos |
| 6 | Ningún estudiante puede ver los datos de otro | Cumplido | `GET /api/mis/cuenta` toma el estudiante del token, nunca de la URL |

**CU-03 Iniciar sesión**

- **Actor:** estudiante con cuenta activa.
- **Flujo principal:**
  1. El estudiante escribe correo y contraseña en *Iniciar sesión*.
  2. El sistema revisa que no esté bloqueado por intentos fallidos.
  3. El sistema compara la contraseña con el hash bcrypt.
  4. El sistema abre una sesión (guarda solo la huella del token de refresco) y emite un JWT de acceso.
  5. El sistema responde **200 OK** con el token en el cuerpo y el refresco en una cookie `HttpOnly`.
  6. El frontend muestra *Mi cuenta*.
- **Flujos alternos:** credenciales incorrectas → **401**; cuenta pendiente → **403** con enlace para verificar; demasiados intentos → **429**.
- **API:** `POST /api/publico/auth/login`.

**CU-04 Consultar mi cuenta**

- **Actor:** estudiante con sesión.
- **Flujo:** el frontend llama `GET /api/mis/cuenta` con `Authorization: Bearer`; el sistema responde **200** con los datos de la cuenta del token. Sin token o con un token alterado: **401**.

**CU-05 Mantener la sesión (renovar)**

- **Actor:** el frontend, en nombre del estudiante.
- **Flujo:** cuando el token de acceso vence (401), el frontend llama `POST /api/publico/auth/refresco` con la cookie y el encabezado `X-XSRF-TOKEN`; recibe un token nuevo y un refresco nuevo. Si alguien reutiliza un refresco ya cambiado, el sistema cierra todas las sesiones de la cuenta.

**CU-06 Cerrar sesión**

- **Actor:** estudiante con sesión.
- **Flujo:** pulsa *Cerrar sesión*; el sistema revoca la sesión, borra las cookies (**204 No Content**) y el frontend vuelve a *Iniciar sesión*. La página protegida ya no abre.
- **API:** `POST /api/publico/auth/logout`.

---

## HU-04 · Consultar documentos oficiales sin cuenta (SCRUM-19, parcial)

> Como **visitante**
> quiero **consultar reglamento, formatos y convocatorias sin iniciar sesión**
> para **encontrar información institucional en un solo lugar**.

| # | Criterio | Estado |
|---|---|---|
| 1 | Accesible sin cuenta | Cumplido (`/guia`, ruta pública) |
| 2 | Los documentos se agrupan por categoría | Cumplido: lista de categorías |
| 3 | Cada documento enlaza a su fuente oficial | Pendiente |
| 4 | Buscar por título | Pendiente |
| 5 | Descargar el documento | Pendiente |
| 6 | Vigencia y fecha de última verificación | Pendiente |

**CU-07 Consultar categorías de la guía**

- **Actor:** visitante.
- **Flujo:** abre `/guia`; el frontend llama `GET /api/publico/guia/categorias`; el sistema responde **200** con las categorías ordenadas (Reglamentos, Formatos, Convocatorias) desde la tabla `categoria_de_recurso`.

---

## HU-05 · Iniciar sesión con mi cuenta de Google (SCRUM-48)

> Como **estudiante**
> quiero **iniciar sesión con mi cuenta de Google**
> para **entrar más rápido sin recordar otra contraseña**.

| # | Criterio | Estado | Evidencia |
|---|---|---|---|
| 1 | Puedo entrar con Google además de con mi contraseña | Cumplido | `POST /api/publico/auth/google` abre la misma sesión que el login (token + cookie de refresco); la contraseña sigue funcionando |
| 2 | Ambos métodos quedan vinculados a la misma cuenta institucional | Cumplido | La cuenta de Google se guarda como otra credencial (`credencial_acceso`, proveedor `google`) del mismo estudiante |
| 3 | Una cuenta de Google no puede quedar vinculada a dos cuentas | Cumplido | 409 `Google ya vinculado`; además la base lo impide (`uk_credencial_identificador_externo`) |
| 4 | Solo puedo vincular Google si mi correo institucional ya está verificado | Cumplido | Vincular exige sesión y cuenta activa; una cuenta pendiente recibe 422 |

**CU-07 Vincular Google a mi cuenta**

- **Actor:** estudiante con sesión (correo institucional ya verificado).
- **Flujo principal:**
  1. En *Mi cuenta* pulsa el botón de Google y elige su cuenta.
  2. Google entrega al frontend un ID token firmado.
  3. El frontend lo envía a `POST /api/mis/google` con su token de acceso.
  4. El backend valida el ID token contra las llaves públicas de Google (firma, emisor, destinatario y vigencia) y guarda el vínculo con el identificador de Google (`sub`).
  5. Responde **200** con el correo de Google vinculado.
- **Flujos alternos:** token que no es de Google → **422** (con sesión, el 401 se reserva para la sesión de CundiApp); cuenta de Google ya usada por otro estudiante o el estudiante ya tiene otra → **409**; Google no responde o no está configurado → **503**.

**CU-08 Entrar con Google**

- **Actor:** estudiante que ya vinculó su cuenta de Google.
- **Flujo:** en *Iniciar sesión* pulsa *Continuar con Google*; el frontend envía el ID token a `POST /api/publico/auth/google`; el backend lo valida, busca la cuenta vinculada y abre la sesión (**200**, igual que el login). Si esa cuenta de Google no está vinculada → **404** con el mensaje de cómo vincularla. Se puede quitar desde *Mi cuenta* (`DELETE /api/mis/google`, **204**).

---

## Resumen de la API implementada

| Método y ruta | Uso | Respuestas |
|---|---|---|
| `GET /api/publico/guia/categorias` | Categorías de la guía | 200 |
| `POST /api/publico/auth/registro` | Crear cuenta | 201, 400, 409, 422, 503 |
| `POST /api/publico/auth/verificacion` | Verificar el correo | 200, 400, 422 |
| `POST /api/publico/auth/verificacion/reenvio` | Pedir otro código | 202, 422, 503 |
| `POST /api/publico/auth/login` | Iniciar sesión | 200, 400, 401, 403, 429 |
| `POST /api/publico/auth/refresco` | Renovar la sesión | 200, 401, 403 |
| `POST /api/publico/auth/logout` | Cerrar sesión | 204, 403 |
| `POST /api/publico/auth/google` | Entrar con Google | 200, 400, 401, 403, 404, 503 |
| `GET /api/mis/cuenta` | Datos de mi cuenta | 200, 401 |
| `GET /api/mis/google` | ¿Tengo Google vinculado? | 200, 401 |
| `POST /api/mis/google` | Vincular Google | 200, 400, 401 (sin sesión), 409, 422, 503 |
| `DELETE /api/mis/google` | Quitar Google | 204, 401 |

Todos los errores usan el formato estándar `application/problem+json` (RFC 9457): `title`, `status` y `detail`.

El único `DELETE` es quitar el vínculo con Google. No hay `PUT` todavía: ninguna historia implementada edita registros desde la API (la guía de la review lo pide "si aplica"). Llegan con RF02 (editar perfil) y RF05/RF06 (calificaciones y pendientes).
