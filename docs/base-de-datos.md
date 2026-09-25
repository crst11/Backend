# Base de datos de CundiApp: paso a paso

Cómo levantar la base de datos en tu máquina con Docker, cómo llevarla a Supabase y cómo mirarla. El diseño está en [modelo-de-datos.md](modelo-de-datos.md).

## Cómo funciona

- El esquema **no se crea a mano**. Lo crea Flyway cuando arranca el backend, con los archivos de `src/main/resources/db/migration`:
  - `V1__esquema_inicial.sql`: las 27 tablas, las 14 restricciones de integridad, los disparadores y las 4 vistas.
  - `V3__codigo_verificacion.sql`: la tabla `codigo_verificacion` (28.ª tabla), que guarda la huella del código de 6 dígitos con el que se verifica el correo institucional (RF01, SCRUM-47).
  - `V2__datos_de_arranque.sql`: la plantilla de evaluación 30 / 30 / 40 y las categorías de la guía institucional.
- Spring Boot solo **valida** el esquema (`ddl-auto=validate`). Nunca lo crea ni lo modifica.
- Todo vive en el esquema `cundiapp` de PostgreSQL 16, con la misma migración en Docker, en preproducción y en producción.
- Nunca se edita una migración que ya se aplicó. Un cambio es un archivo nuevo (`V3__...sql`).

## Qué necesitas

- Docker Desktop **encendido** (`docker version` debe mostrar cliente y servidor).
- Java 21 y Git.
- Este repositorio clonado y la rama `desarrollo` actualizada.

## 1. Levantar la base en Docker

1. Crea tu archivo de variables copiando el ejemplo. El archivo `.env` nunca se sube a Git.

   ```powershell
   Copy-Item .env.example .env
   ```

2. Abre `.env` y completa estos tres valores (los demás quedan vacíos por ahora). La contraseña puede ser cualquiera: es solo para tu máquina.

   ```properties
   DB_URL=jdbc:postgresql://localhost:5432/cundiapp
   DB_USERNAME=cundiapp
   DB_PASSWORD=una_clave_local
   ```

3. Levanta PostgreSQL y espera a que quede saludable:

   ```bash
   docker compose up -d
   docker compose ps
   ```

   La columna de estado debe decir `healthy`. Si dice `starting`, espera unos segundos y repite.

4. Arranca el backend. Al iniciar, Flyway aplica las dos migraciones:

   ```bash
   ./mvnw spring-boot:run
   ```

   En la consola debes ver algo como `Successfully applied 2 migrations to schema "cundiapp"`. Detén el backend con `Ctrl + C` cuando quieras.

5. Comprueba que quedó todo (deben salir 28 tablas más la de historial de Flyway, y 4 vistas):

   ```bash
   docker exec -it cundiapp-db psql -U cundiapp -d cundiapp -c "\dt cundiapp.*"
   docker exec -it cundiapp-db psql -U cundiapp -d cundiapp -c "\dv cundiapp.*"
   docker exec -it cundiapp-db psql -U cundiapp -d cundiapp -c "SELECT * FROM cundiapp.categoria_de_recurso;"
   ```

Para **empezar de cero** (borra todos los datos y vuelve a aplicar las migraciones al siguiente arranque):

```bash
docker compose down -v
docker compose up -d
```

## 2. Mirar la base de datos

Postman no se conecta a PostgreSQL (más abajo se explica por qué y para qué sí sirve). Para **ver tablas, filas y relaciones** usa una de estas:

- **pgAdmin** (viene en el `docker-compose.yml` como herramienta opcional):

  ```bash
  docker compose --profile herramientas up -d
  ```

  Abre `http://localhost:8081` (usuario `admin@cundiapp.local`, contraseña `admin`), pulsa **Add New Server** y en la pestaña Connection escribe: host `db`, puerto `5432`, base `cundiapp`, usuario `cundiapp` y tu `DB_PASSWORD`. Las tablas están en *Databases → cundiapp → Schemas → cundiapp → Tables*.
- **psql** dentro del contenedor, como en el paso 5 de arriba. Para entrar de forma interactiva:

  ```bash
  docker exec -it cundiapp-db psql -U cundiapp -d cundiapp
  ```

  Dentro, `SET search_path TO cundiapp;` te deja escribir `SELECT * FROM estudiante;` sin el prefijo.
- **Supabase** (una vez que la base esté allí): el *Table Editor* permite cambiar el esquema de la lista desplegable a `cundiapp` y ver los datos.

### Postman: qué sí y qué no

Postman es un cliente de APIs: manda peticiones HTTP. PostgreSQL no habla HTTP, así que **en Postman no se pueden abrir las tablas**. Tampoco conviene "engañarlo" con la API automática de Supabase: para eso habría que exponer el esquema `cundiapp`, y sin reglas de acceso por fila cualquiera con la clave pública podría leer y escribir todos los datos. Por eso la guía del proyecto prohíbe exponerlo.

Postman sí sirve para probar la **API del backend**, que llega con SCRUM-43:

1. Instala Postman y crea una colección `CundiApp`.
2. En la colección, pestaña *Variables*, crea `baseUrl` con el valor `http://localhost:8080`.
3. Crea una petición `GET {{baseUrl}}/api/publico/guia/categorias`. Cuando exista SCRUM-43 debe responder una lista JSON con las tres categorías de la guía (`Reglamentos`, `Formatos` y `Convocatorias`), leídas de PostgreSQL.
4. Para PRE, agrega un entorno con otra `baseUrl` (la URL del backend desplegado).

Hasta entonces, para ver los datos usa pgAdmin, `psql` o el Table Editor de Supabase.

## 3. Llevar el esquema a Supabase (preproducción)

Esto lo haces tú porque requiere tu cuenta. **La contraseña de la base nunca se escribe en el repositorio, en el chat ni en capturas de pantalla.** Los nombres de menús pueden cambiar un poco según la versión de Supabase.

### 3.1 Crear el proyecto

1. Entra a [supabase.com](https://supabase.com) e inicia sesión (con GitHub o con correo).
2. Pulsa **New project** y completa:
   - **Organization:** la tuya.
   - **Name:** `cundiapp-pre`.
   - **Database Password:** pulsa *Generate a password*, cópiala y guárdala de inmediato en un gestor de contraseñas. Si la pierdes se restablece en *Project Settings → Database*.
   - **Region:** la más cercana, por ejemplo *South America (São Paulo)*.
   - **Plan:** Free.
3. Pulsa **Create new project** y espera uno o dos minutos hasta que el panel termine de cargar.

### 3.2 Copiar los datos de conexión (Session pooler)

1. En la barra superior del proyecto pulsa **Connect**.
2. Elige el método **Session pooler**. Hay tres y solo sirve este:
   - *Direct connection*: solo funciona por IPv6 y muchas redes no lo tienen.
   - *Transaction pooler* (puerto 6543): rompe las sentencias preparadas de Hibernate.
   - **Session pooler** (puerto **5432**): el correcto.
3. Anota cuatro datos:

   | Dato | Cómo se ve |
   |---|---|
   | Host | `aws-0-REGION.pooler.supabase.com` |
   | Puerto | `5432` |
   | Base de datos | `postgres` |
   | Usuario | `postgres.REFERENCIA` (con el punto y la referencia de tu proyecto) |

### 3.3 Definir las variables en tu terminal

Abre una terminal **nueva** de PowerShell en la carpeta `Backend` y escribe (con tus datos reales). Estas variables valen solo para esa ventana y pasan por encima del archivo `.env`:

```powershell
$env:PERFIL = "pre"
$env:DB_URL = "jdbc:postgresql://aws-0-REGION.pooler.supabase.com:5432/postgres?sslmode=require"
$env:DB_USERNAME = "postgres.REFERENCIA"
$env:DB_PASSWORD = "la_contraseña_de_supabase"
```

### 3.4 Aplicar el esquema

Con esa misma terminal arranca el backend una vez:

```bash
./mvnw spring-boot:run
```

En la consola debes ver, en este orden:

```
Creating schema "cundiapp" ...
Migrating schema "cundiapp" to version "1 - esquema inicial"
Migrating schema "cundiapp" to version "2 - datos de arranque"
Successfully applied 2 migrations to schema "cundiapp"
Started CundiappApplication
```

Detén el backend con `Ctrl + C` y **cierra esa terminal**: así las variables con tu contraseña desaparecen.

### 3.5 Ver el resultado en Supabase

1. En la barra izquierda abre **Table Editor**. Arriba a la izquierda hay un selector de esquema que dice `public`: cámbialo a **`cundiapp`**. Deben aparecer las 28 tablas más `flyway_schema_history`.
2. Abre `categoria_de_recurso` (3 filas), `plantilla_evaluacion` (1 fila) e `item_de_plantilla` (3 filas).
3. Abre **SQL Editor**, pulsa *New query* y ejecuta:

   ```sql
   select table_name from information_schema.tables where table_schema = 'cundiapp' order by 1;
   select version, description, success from cundiapp.flyway_schema_history;
   select * from cundiapp.v_estado_asignatura;
   ```

   La primera lista las tablas, la segunda muestra las dos migraciones con `success = true` y la tercera sale vacía porque todavía no hay estudiantes.
4. Para ver el diagrama, entra a **Database → Schema Visualizer** y elige el esquema `cundiapp`.

### 3.6 Lo que no debes hacer

- **No expongas `cundiapp` en la API de Supabase.** En *Project Settings → Data API*, la lista de esquemas expuestos debe quedar como está (`public`); no agregues `cundiapp`.
- No uses la contraseña de Supabase en el archivo `.env` que se queda en tu equipo si compartes el computador.
- Si la contraseña se filtra (chat, captura, commit), restablécela en *Project Settings → Database*.

### 3.7 Latido

Para que el plan gratuito no pause el proyecto se usan la tabla `public.latido` y el workflow `latido-supabase.yml`. Se agregan en SCRUM-60, después de esta guía, con la protección de acceso por fila aprobada.

## 4. Cambiar el esquema más adelante

1. Crea `src/main/resources/db/migration/V3__descripcion_corta.sql` (el número siempre sube; nunca reutilices uno).
2. Escribe el cambio con `ALTER TABLE` o el DDL que corresponda; no borres datos sin aprobación del equipo.
3. Ejecuta `./mvnw verify`. Las pruebas levantan un PostgreSQL limpio, aplican todas las migraciones y comprueban las 14 restricciones.
4. Si tocaste el modelo, actualiza [modelo-de-datos.md](modelo-de-datos.md).

## 5. Pruebas de la base de datos

```bash
./mvnw verify
```

Necesita Docker encendido: Testcontainers levanta un PostgreSQL 16 temporal, aplica las migraciones y ejecuta las pruebas de `EsquemaBaseDeDatosTest` (28 tablas, 210 campos, 4 vistas y datos de arranque) y de `RestriccionesDeIntegridadTest` (una por cada una de las 14 restricciones, con casos que se rechazan y casos que se aceptan).

## Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| `failed to connect to the docker API` | Docker Desktop está apagado. Enciéndelo y espera a que diga *Engine running*. |
| `required variable DB_PASSWORD is missing` | Falta el archivo `.env` o la variable en él. Repite el paso 1. |
| `password authentication failed` después de cambiar `DB_PASSWORD` | El volumen conserva la contraseña anterior. Usa `docker compose down -v` y vuelve a levantar. |
| `port is already allocated` (5432) | Ya tienes un PostgreSQL en ese puerto. En `docker-compose.yml` cambia `5432:5432` por `5433:5432` y en `.env` pon el puerto 5433 en `DB_URL`. |
| `Migration checksum mismatch` | Alguien editó una migración ya aplicada. No la edites: revierte el cambio y crea `V3`. En tu base local, `docker compose down -v` y vuelve a empezar. Mientras el esquema no se haya llevado a Supabase, `V1` se puede seguir ajustando; en ese caso cada quien reinicia su base local con `docker compose down -v`. |
| Error de conexión a Supabase | Revisa que usas el *Session pooler* (puerto 5432), que el usuario es `postgres.REFERENCIA` y que la URL termina en `?sslmode=require`. |
| `Could not find a valid Docker environment` al correr las pruebas | Docker está apagado. |
