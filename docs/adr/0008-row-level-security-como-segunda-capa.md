# ADR 0008: Row-Level Security como segunda capa de defensa de la base

- **Estado:** propuesta aprobada por Cristian el 24 de octubre de 2026 (SCRUM-91). La migración y la prueba se escriben después de revisar este documento.
- **Fecha:** 24 de octubre de 2026

## Contexto

Ninguna migración activa Row-Level Security (RLS). Hoy el riesgo es bajo: el esquema `cundiapp` no se expone por la API de datos de Supabase y el único acceso es el backend. Aun así, si alguien expusiera el esquema por error, o se filtrara una credencial distinta de la del backend, no habría una segunda barrera entre esa persona y los datos de los estudiantes (Ley 1581 de 2012).

Lo que hay hoy, comprobado en el repositorio:

- **Un solo usuario para todo.** El backend y Flyway se conectan con el mismo usuario (`postgres.<ref>` en Supabase, `cundiapp` en local) por `DB_USERNAME`. Ese usuario crea las tablas, así que es su **dueño**.
- **El dueño no pasa por RLS.** En PostgreSQL el dueño de una tabla se salta sus políticas (salvo `FORCE ROW LEVEL SECURITY`), y en Supabase el rol `postgres` además tiene `BYPASSRLS`. Activar RLS no cambia lo que el backend puede hacer.
- **Los roles de la API de datos de Supabase** son `anon`, `authenticated` y `service_role`. No existen en el PostgreSQL de Docker ni en el de Testcontainers.
- **El esquema tiene 32 tablas y 4 vistas** (más `flyway_schema_history`). Las vistas se ejecutan con los permisos de su dueño, no con los de quien las consulta, así que RLS por sí solo no las alcanza.
- **`public.latido` y `public.latir()` no están en ninguna migración.** `docs/base-de-datos.md` (§3.7) decía que se agregarían con SCRUM-60. El flujo `latido.yml` solo consulta un endpoint público del dominio y no usa esa tabla.

## Opciones

| Opción | A favor | En contra |
|---|---|---|
| **A. Activar RLS sin políticas y quitar permisos a los roles de la API** (elegida) | El backend sigue igual; todo rol que no sea el dueño queda sin acceso, y aunque se le diera `SELECT` leería 0 filas; sin credenciales nuevas | La protección real descansa en que el backend sea el dueño; no hay política por estudiante |
| B. `FORCE ROW LEVEL SECURITY` con una política para el usuario del backend | El dueño también queda sujeto a RLS | La política tendría que nombrar al usuario, que cambia entre local y Supabase (`cundiapp` y `postgres.<ref>`): migración frágil y distinta por ambiente |
| C. Rol de ejecución propio, sin `BYPASSRLS`, distinto del que migra | Es el mínimo privilegio de verdad: un SQL inyectado en el backend tampoco leería de más | Hay que cambiar credenciales en Supabase, en AWS y en el CI; es un cambio de operación, no solo de esquema |
| D. Políticas por estudiante (`id_estudiante = ...`) | RLS "completa" | El backend no manda el estudiante a la base: lo valida en la aplicación. Exigiría `SET LOCAL` por transacción en cada caso de uso |

## Decisión

**Opción A.** La migración nueva (`V<n>__row_level_security.sql`) hace cuatro cosas, todas dentro del esquema `cundiapp`:

1. **`ENABLE ROW LEVEL SECURITY` en todas las tablas del esquema**, recorriéndolas con `pg_tables` y no una por una, y **sin ninguna política**: se niega por defecto. No se usa `FORCE`. Las 32 tablas y `flyway_schema_history` llevan la misma política: ninguna, porque el único acceso legítimo es el del dueño.
2. **`REVOKE ALL` para `anon`, `authenticated` y `service_role`** sobre el esquema, las tablas, las secuencias y las funciones. Cada `REVOKE` se ejecuta solo si el rol existe (`pg_roles`), de modo que la misma migración corre en Docker, en Testcontainers y en Supabase sin condiciones por ambiente.
3. **`ALTER DEFAULT PRIVILEGES`** para esos roles, de modo que las tablas, secuencias y funciones futuras también nazcan cerradas.
4. **`security_invoker = true` en las vistas**, para que al consultarlas se aplique RLS de quien pregunta y no el de su dueño. Al backend, que es el dueño, no le cambia nada.

**`public.latido` y `public.latir()`:** si existen en el proyecto de Supabase, la migración les activa RLS y les quita los permisos a los mismos roles, **sin borrar nada**. Si no existen, no hace nada y el ajuste se da por no aplicable. Qué pasó en cada proyecto se anota en la incidencia.

Lo que no cambia: el rol con el que conecta el backend conserva el acceso completo porque es el dueño de las tablas; no se crean ni rotan credenciales; ninguna política depende de un nombre de usuario.

## Cómo se prueba

- **Prueba nueva con Testcontainers** (en la suite de integridad de la base): se crea un rol `NOLOGIN` sin permisos y se comprueba que no puede leer ninguna tabla (error de permiso); se le da `GRANT SELECT` a otro rol y se comprueba que lee **0 filas** por la RLS sin políticas.
- **Guarda hacia adelante:** otra prueba falla si alguna tabla de `cundiapp` queda con `relrowsecurity = false`. Así una migración futura que olvide activarla rompe el CI.
- Las pruebas de integración existentes siguen en verde: demuestran que el backend, siendo el dueño, no pierde acceso.
- **Antes de producción**, en el Supabase de preproducción: el despliegue aplica la migración, el *smoke* y la colección de Postman pasan, y en el editor SQL se comprueba que todas las tablas muestran `rowsecurity = true` en `pg_tables`.

## Consecuencias

- Mientras el backend use el mismo usuario para migrar y para ejecutar, la barrera protege contra **otros** roles y contra exposición accidental, **no** contra una inyección de SQL dentro del propio backend. Esa defensa sigue siendo el uso de parámetros enlazados en todo el código.
- Si se cambia el usuario con el que conecta el backend por uno que no sea dueño de las tablas, deja de tener acceso: habría que darle los permisos y una política. Esa decisión pertenece a la opción C.
- Toda tabla nueva debe llevar RLS; la prueba de guarda lo exige.

## Mejora futura (fuera de este alcance)

**Opción C:** separar el usuario que migra del que ejecuta, con un rol de ejecución sin `BYPASSRLS` y con `GRANT` solo de lo que usa la aplicación. Cambia credenciales en Supabase, AWS y el CI, y se hace con su propia incidencia y con la aprobación del equipo.
