-- SCRUM-91, ADR 0008: Row-Level Security como segunda capa de defensa.
--
-- El backend conecta como dueño de las tablas y no pasa por RLS (el dueño se la salta, y en Supabase el rol
-- postgres además tiene BYPASSRLS), así que nada cambia para él. Cualquier otro rol queda sin acceso: sin
-- políticas, RLS niega por defecto, y aunque alguien le diera SELECT leería 0 filas.
--
-- Es idempotente y no depende del ambiente: los roles de la API de datos de Supabase (anon, authenticated y
-- service_role) no existen en Docker ni en Testcontainers, así que cada permiso se quita solo si el rol existe.

DO $$
DECLARE
    elemento record;
    rol      text;
BEGIN
    -- 1. RLS activa en todas las tablas del esquema. Sin FORCE y sin políticas. flyway_schema_history se deja
    --    fuera: Flyway la tiene bloqueada en otra conexión mientras corre la migración y el ALTER esperaría para
    --    siempre. Sus permisos sí se quitan abajo.
    FOR elemento IN SELECT tablename FROM pg_tables
                    WHERE schemaname = 'cundiapp' AND tablename <> 'flyway_schema_history' LOOP
        EXECUTE format('ALTER TABLE cundiapp.%I ENABLE ROW LEVEL SECURITY', elemento.tablename);
    END LOOP;

    -- 4. Las vistas se consultan con los permisos de quien pregunta y no con los de su dueño. Al backend, que es
    --    el dueño, no le cambia nada.
    FOR elemento IN SELECT viewname FROM pg_views WHERE schemaname = 'cundiapp' LOOP
        EXECUTE format('ALTER VIEW cundiapp.%I SET (security_invoker = true)', elemento.viewname);
    END LOOP;

    -- 2 y 3. Los roles de la API de datos de Supabase pierden todo permiso sobre cundiapp, y lo que se cree
    --        después nace igual de cerrado.
    FOR rol IN SELECT rolname FROM pg_roles WHERE rolname IN ('anon', 'authenticated', 'service_role') LOOP
        EXECUTE format('REVOKE ALL ON SCHEMA cundiapp FROM %I', rol);
        EXECUTE format('REVOKE ALL ON ALL TABLES IN SCHEMA cundiapp FROM %I', rol);
        EXECUTE format('REVOKE ALL ON ALL SEQUENCES IN SCHEMA cundiapp FROM %I', rol);
        EXECUTE format('REVOKE ALL ON ALL FUNCTIONS IN SCHEMA cundiapp FROM %I', rol);
        EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA cundiapp REVOKE ALL ON TABLES FROM %I', rol);
        EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA cundiapp REVOKE ALL ON SEQUENCES FROM %I', rol);
        EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA cundiapp REVOKE ALL ON FUNCTIONS FROM %I', rol);

        -- public.latido y public.latir() no están en ninguna migración: si existen en el proyecto de Supabase se
        -- cierran igual, sin borrar nada.
        IF to_regclass('public.latido') IS NOT NULL THEN
            EXECUTE format('REVOKE ALL ON TABLE public.latido FROM %I', rol);
        END IF;
        IF to_regprocedure('public.latir()') IS NOT NULL THEN
            EXECUTE format('REVOKE ALL ON FUNCTION public.latir() FROM %I', rol);
        END IF;
    END LOOP;

    -- Toda función nace con EXECUTE para PUBLIC, así que quitárselo solo a esos roles no basta. El dueño (el
    -- backend) conserva el suyo, y los disparadores no necesitan EXECUTE de quien escribe.
    REVOKE ALL ON ALL FUNCTIONS IN SCHEMA cundiapp FROM PUBLIC;

    IF to_regclass('public.latido') IS NOT NULL THEN
        ALTER TABLE public.latido ENABLE ROW LEVEL SECURITY;
    END IF;
    IF to_regprocedure('public.latir()') IS NOT NULL THEN
        REVOKE ALL ON FUNCTION public.latir() FROM PUBLIC;
    END IF;
END
$$;
