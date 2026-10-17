-- SCRUM-37: recibir avisos de vencimiento (RF10).
--
-- Casi todo lo que hace falta ya existía desde la V1: `dispositivo` guarda la suscripción Web Push
-- (endpoint y llaves) y `notificacion` guarda cada aviso con su tipo, su estado de entrega y si se leyó.
-- Esta migración solo agrega lo que faltaba:
--
--   intentos  cuántas veces se intentó entregar el aviso al dispositivo. Un fallo pasajero del servicio
--             de avisos del navegador se reintenta pocas veces y no para siempre.
--
--   uq_notificacion_vencimiento_por_pendiente
--             un pendiente tiene a lo sumo UN aviso de vencimiento. La tarea que los genera corre cada
--             pocos minutos y el aviso tiene que salir una sola vez; lo garantiza la base de datos, no
--             la memoria de quien lo genera.
--
--   ix_pendiente_por_vencer
--             la tarea busca, entre todos los estudiantes, los pendientes abiertos que vencen pronto. Sin
--             este índice recorrería todos los pendientes en cada ronda; con él solo mira los abiertos.
--
-- No cambia ninguna fila existente: `intentos` arranca en cero.

ALTER TABLE notificacion
    ADD COLUMN intentos SMALLINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_notificacion_intentos CHECK (intentos BETWEEN 0 AND 5);

CREATE UNIQUE INDEX uq_notificacion_vencimiento_por_pendiente
    ON notificacion (id_pendiente) WHERE tipo_notificacion = 'vencimiento_proximo';

CREATE INDEX ix_pendiente_por_vencer
    ON pendiente (fecha_limite) WHERE estado_pendiente = 'abierto';
