# Base de datos de CundiApp: paso a paso

Cómo levantar la base de datos en tu máquina con Docker, cómo llevarla a Supabase y cómo mirarla. El diseño está en [modelo-de-datos.md](modelo-de-datos.md).

## Cómo funciona

- El esquema **no se crea a mano**. Lo crea Flyway cuando arranca el backend, con los archivos de `src/main/resources/db/migration`:
  - `V1__esquema_inicial.sql`: las 27 tablas, las 14 restricciones de integridad, los disparadores y las 4 vistas.
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

5. Comprueba que quedó todo (deben salir 27 tablas más la de historial de Flyway, y 4 vistas):

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

Postman no se conecta a PostgreSQL: es un cliente de APIs, así que sirve más adelante para probar los endpoints del backend (SCRUM-43). Para **ver tablas, filas y relaciones** usa una de estas:

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

## 3. Llevar el esquema a Supabase (preproducción)

Esto lo haces tú porque requiere tu cuenta. **La contraseña de la base nunca se escribe en el repositorio ni se comparte por chat.**

1. Crea un proyecto en [supabase.com](https://supabase.com) (por ejemplo `cundiapp-pre`) y define una contraseña de base de datos segura. Guárdala en tu gestor de contraseñas.
2. En el proyecto pulsa **Connect** y elige **Session pooler** (puerto **5432**). Anota el host, el usuario (con la forma `postgres.REFERENCIA`) y la base (`postgres`).
   - Nunca uses el *Transaction pooler* (puerto 6543): rompe las sentencias preparadas de Hibernate.
   - La conexión directa de Supabase solo tiene IPv6; por eso se usa el Session pooler.
3. En una terminal PowerShell, define las variables del perfil de preproducción (valen solo para esa terminal):

   ```powershell
   $env:PERFIL = "pre"
   $env:DB_URL = "jdbc:postgresql://HOST_DEL_POOLER:5432/postgres?sslmode=require"
   $env:DB_USERNAME = "postgres.REFERENCIA"
   $env:DB_PASSWORD = "tu_contraseña_de_supabase"
   ```

4. Arranca el backend una vez. Flyway crea el esquema `cundiapp` en Supabase y aplica V1 y V2:

   ```bash
   ./mvnw spring-boot:run
   ```

5. En Supabase entra a **Table Editor**, cambia el esquema a `cundiapp` y comprueba que aparecen las 27 tablas. Con **Database → Migrations/Schema Visualizer** también puedes ver el diagrama.
6. **Seguridad:** en *Project Settings → Data API* no agregues `cundiapp` a los esquemas expuestos. La aplicación habla con la base por el backend, nunca directamente por la API pública.
7. **Latido:** para que el plan gratuito no pause el proyecto se usa la tabla `public.latido` y el workflow `latido-supabase.yml`. Se agregan en SCRUM-60, después de esta guía.

## 4. Cambiar el esquema más adelante

1. Crea `src/main/resources/db/migration/V3__descripcion_corta.sql` (el número siempre sube; nunca reutilices uno).
2. Escribe el cambio con `ALTER TABLE` o el DDL que corresponda; no borres datos sin aprobación del equipo.
3. Ejecuta `./mvnw verify`. Las pruebas levantan un PostgreSQL limpio, aplican todas las migraciones y comprueban las 14 restricciones.
4. Si tocaste el modelo, actualiza [modelo-de-datos.md](modelo-de-datos.md).

## 5. Pruebas de la base de datos

```bash
./mvnw verify
```

Necesita Docker encendido: Testcontainers levanta un PostgreSQL 16 temporal, aplica las migraciones y ejecuta las pruebas de `EsquemaBaseDeDatosTest` (27 tablas, 204 campos, 4 vistas y datos de arranque) y de `RestriccionesDeIntegridadTest` (una por cada una de las 14 restricciones, con casos que se rechazan y casos que se aceptan).

## Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| `failed to connect to the docker API` | Docker Desktop está apagado. Enciéndelo y espera a que diga *Engine running*. |
| `required variable DB_PASSWORD is missing` | Falta el archivo `.env` o la variable en él. Repite el paso 1. |
| `password authentication failed` después de cambiar `DB_PASSWORD` | El volumen conserva la contraseña anterior. Usa `docker compose down -v` y vuelve a levantar. |
| `port is already allocated` (5432) | Ya tienes un PostgreSQL en ese puerto. En `docker-compose.yml` cambia `5432:5432` por `5433:5432` y en `.env` pon el puerto 5433 en `DB_URL`. |
| `Migration checksum mismatch` | Alguien editó una migración ya aplicada. No la edites: revierte el cambio y crea `V3`. En tu base local, `docker compose down -v` y vuelve a empezar. |
| Error de conexión a Supabase | Revisa que usas el *Session pooler* (puerto 5432), que el usuario es `postgres.REFERENCIA` y que la URL termina en `?sslmode=require`. |
| `Could not find a valid Docker environment` al correr las pruebas | Docker está apagado. |
