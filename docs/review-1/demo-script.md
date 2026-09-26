# Guion de la demo técnica (Review 1)

Demo en local, como indicó el profesor. Tiempo objetivo: 6 a 7 minutos de los 10 a 15 de la presentación. Todo se prepara **antes** de que empiece el turno del equipo.

## 0. Preparación (10 minutos antes)

1. Encender **Docker Desktop** y esperar a que diga *Engine running*.
2. Base de datos (carpeta `Backend`):
   ```powershell
   docker compose up -d --wait
   ```
3. Backend (carpeta `Backend`, en una terminal que se deja abierta y visible, porque ahí sale el código de verificación):
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
   Listo cuando aparece `Started CundiappApplication`. El `.env` debe tener `COOKIE_SECURE=false` (viene en `.env.example`) para que Postman envíe la cookie de refresco por http, `GOOGLE_CLIENT_ID` para el inicio con Google y las variables `CORREO_SMTP_*` para que el código llegue al correo.
4. Frontend (carpeta `Frontend`, otra terminal):
   ```powershell
   npm start
   ```
   Abrir `http://localhost:4200`.
5. Postman: importar `Backend/docs/postman/CundiApp.postman_collection.json` (si ya estaba importada, reemplazarla) y borrar las cookies de `localhost` (*Cookies* → *localhost* → borrar todo).
6. Una terminal lista para consultar la base de datos:
   ```powershell
   docker exec -it cundiapp-db psql -U cundiapp -d cundiapp
   ```
   (Alternativa con interfaz: `docker compose --profile herramientas up -d` y abrir pgAdmin en `http://localhost:8081`.)

## 1. APIs con Postman (2.1 de la guía)

Para cada petición se muestra **Request** (método, URL, cuerpo), **Response** (cuerpo) y **código de estado**, y la pestaña *Test Results* en verde.

| Paso | Petición | Qué decir |
|---|---|---|
| 1 | Carpeta 1 → *Consultar categorías* | **GET**, ruta pública, 200 con datos que vienen de PostgreSQL. |
| 2 | Carpeta 2 → *1. Registro* | **POST** que crea un registro: 201 y la cuenta queda `pendiente`. El correo se genera solo en cada corrida. |
| 3 | Correo institucional (o la terminal del backend) | Con SMTP configurado, abrir el correo que llega a la cuenta registrada; sin SMTP, la línea `[DEV] Código de verificación para ...` del backend. Copiar los 6 dígitos a la variable de colección `codigo`. Para ver el correo en vivo, cambiar la variable `correo` por un correo institucional real del equipo antes del paso 2. |
| 4 | *3. Verificar con código incorrecto* | 422 con "Te quedan 4 intentos": manejo de errores con ProblemDetail. |
| 5 | *4. Verificar con el código correcto* | 200 y la cuenta pasa a `activa`. |
| 6 | Carpeta 3 → *2. Login* | 200, token de acceso en el cuerpo, refresco en cookie HttpOnly (pestaña *Cookies*). |
| 7 | *3. Mi cuenta* | **GET** protegido con `Authorization: Bearer`: el estudiante sale del token. |
| 8 | Carpeta 4 → *Mi cuenta sin token* y *Registro con correo repetido* | 401 y 409: la API responde con códigos HTTP adecuados. |
| 9 | Carpeta 5 → *2. Entrar con un token que no es de Google* | La API externa: el backend no confía en el navegador y valida el token contra las llaves de Google (401 si no lo firmó Google). |

## 2. Flujo completo frontend + backend (2.2 de la guía)

1. En `http://localhost:4200/cuenta/registro` crear una cuenta con un correo nuevo (por ejemplo `demo.review@ucundinamarca.edu.co`).
   - **Acción en el frontend:** enviar el formulario.
   - **Llamado al backend:** abrir las herramientas del navegador (F12 → *Network*) y mostrar el `POST /api/publico/auth/registro` con 201.
   - **Respuesta al usuario:** la app lleva sola a *Verifica tu correo*.
2. Escribir el código que llega al correo institucional (sin SMTP configurado, el de la consola del backend) → "Tu correo quedó verificado".
3. *Iniciar sesión* con una contraseña equivocada → mensaje genérico. Luego la correcta → *Mi cuenta* con los datos.
4. Recargar la página (F5): sigue en *Mi cuenta* (renovación silenciosa con la cookie).
5. En *Mi cuenta* → *Vincular con Google* y elegir una cuenta de Google → aparece el correo de Google vinculado.
6. *Cerrar sesión* → vuelve a *Iniciar sesión*; escribir `/cuenta/mi-cuenta` en la barra ya no abre.
7. *Continuar con Google* → entra a *Mi cuenta* sin escribir la contraseña (API externa en vivo). En F12 → *Network* se ve el `POST /api/publico/auth/google` con 200.

## 3. Validación en la base de datos (2.3 de la guía)

En la terminal de `psql`, con el correo usado en el paso 2:

```sql
-- La cuenta existe, quedó activa y guardó la fecha del consentimiento (Ley 1581)
SELECT id_estudiante, nombres, correo_institucional, estado_cuenta, fecha_consentimiento
  FROM cundiapp.estudiante WHERE correo_institucional = 'demo.review@ucundinamarca.edu.co';

-- La contraseña NO está en claro: es un hash bcrypt ($2a$12$...), y el correo quedó verificado
SELECT proveedor, left(hash_contrasena, 7) AS bcrypt, correo_verificado, fecha_ultimo_acceso
  FROM cundiapp.credencial_acceso c JOIN cundiapp.estudiante e USING (id_estudiante)
 WHERE e.correo_institucional = 'demo.review@ucundinamarca.edu.co';

-- El código de verificación se guarda como huella, con los intentos fallidos contados
SELECT left(hash_codigo, 10) AS huella, intentos_fallidos, fecha_uso IS NOT NULL AS usado
  FROM cundiapp.codigo_verificacion v JOIN cundiapp.estudiante e USING (id_estudiante)
 WHERE e.correo_institucional = 'demo.review@ucundinamarca.edu.co';

-- Sesiones: la del login se rotó al recargar y la última quedó cerrada por el logout
SELECT consec_sesion, left(hash_token_refresco, 8) AS huella, motivo_revocacion
  FROM cundiapp.sesion s JOIN cundiapp.estudiante e USING (id_estudiante)
 WHERE e.correo_institucional = 'demo.review@ucundinamarca.edu.co' ORDER BY consec_sesion;
```

Lo que se debe hacer notar: lo que se envió desde la pantalla es exactamente lo que quedó guardado (coherencia), y los datos sensibles nunca se guardan en claro.

## 4. Si algo falla en vivo

| Síntoma | Causa probable | Arreglo rápido |
|---|---|---|
| El backend no arranca: "Connection refused" | Docker apagado | Encender Docker Desktop y `docker compose up -d --wait` |
| El backend no arranca: "JWT_SECRETO debe tener al menos 32 caracteres" | Línea `JWT_SECRETO=` vacía en `.env` | Borrar esa línea del `.env` |
| Postman: *Refrescar* da 403 | Faltan las cookies (se borraron o `COOKIE_SECURE` no es `false`) | Volver a correr *Login* y revisar el `.env` |
| La página de Angular sale en blanco | `npm start` aún compilando | Esperar a "Application bundle generation complete" |
| "Correo ya registrado" | El correo ya se usó en un ensayo | Usar otro correo |

Para dejar la base limpia después de los ensayos: `docker compose down -v` y `docker compose up -d --wait` (Flyway vuelve a crear el esquema al arrancar el backend).
