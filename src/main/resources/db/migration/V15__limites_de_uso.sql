-- SCRUM-216: los limitadores salen de la memoria del proceso y pasan a la base de datos (12 factores, factor 6).
--
-- Hasta ahora los intentos fallidos de inicio de sesión, las peticiones por ventana (el análisis de PDF) y los correos
-- por IP se contaban en un mapa en memoria. Eso tenía dos problemas:
--   * cada despliegue lo reiniciaba, así que reemplazar el contenedor borraba los contadores de quien estaba en pleno
--     intento de adivinar una contraseña;
--   * con más de una instancia cada una contaría por su lado y el límite se multiplicaría.
--
-- Una sola tabla sirve a los tres, porque los tres son «cuántas veces desde cuándo»:
--
--   clave    el HMAC-SHA256 (64 caracteres hexadecimales) del tipo de límite y de lo que se cuenta. NUNCA el correo ni la
--            IP en claro: ni una copia de seguridad ni quien lea la tabla puede saber de quién son los contadores
--            (Ley 1581 de 2012). Se firma con un secreto del servidor y no se hace un hash simple, porque el espacio
--            de direcciones IP es tan chico que un hash simple se revertiría probándolas todas.
--   conteo   cuántas veces va en la ventana actual.
--   primera  cuándo empezó la ventana actual (los límites por ventana fija la miden desde aquí).
--   ultima   cuándo fue la última vez (el límite de intentos fallidos se levanta a los 15 minutos de la última).
--
-- Se escribe con INSERT ... ON CONFLICT DO UPDATE: una sola operación atómica, sin leer y luego escribir, así que dos
-- instancias a la vez no pierden cuentas.
--
-- UNLOGGED porque son contadores efímeros que viven minutos: sin el registro de escritura anticipada cada fallo de
-- inicio de sesión cuesta mucho menos. Si el servidor de base de datos se cae sin cerrar bien, la tabla queda vacía,
-- que para un límite de 15 minutos es aceptable (y es lo mismo que pasaba al reiniciar la aplicación).
--
-- No cambia ninguna fila existente ni toca ninguna otra tabla.

CREATE UNLOGGED TABLE limite_de_uso (
    clave    CHAR(64)    NOT NULL,
    conteo   INTEGER     NOT NULL,
    primera  TIMESTAMPTZ NOT NULL,
    ultima   TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_limite_de_uso PRIMARY KEY (clave),
    CONSTRAINT ck_limite_de_uso_conteo CHECK (conteo > 0),
    CONSTRAINT ck_limite_de_uso_orden CHECK (ultima >= primera)
);

-- La limpieza programada borra lo que ya no puede bloquear a nadie y busca por la última vez.
CREATE INDEX ix_limite_de_uso_ultima ON limite_de_uso (ultima);
