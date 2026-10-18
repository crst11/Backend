-- SCRUM-204: avisar cuando un pendiente se vence (RF10).
--
-- Hasta ahora el único aviso de un pendiente era «vence pronto», antes de la fecha límite. Un pendiente que se
-- vencía sin haber entrado en esa ventana (creado ya vencido, o con el dispositivo sin avisos) no avisaba nunca.
-- Esta migración agrega el tipo `pendiente_vencido` y lo que lo hace seguro:
--
--   ck_notificacion_tipo
--             admite el tipo nuevo. Se vuelve a crear con los tres valores de siempre más el nuevo; ninguna
--             fila existente cambia, así que la restricción vuelve a validar sin problema.
--
--   uq_notificacion_vencido_por_pendiente
--             un pendiente tiene a lo sumo UN aviso de «se venció». Igual que con «vence pronto», lo garantiza
--             la base de datos y no la memoria de la tarea que lo genera, que corre cada pocos minutos.
--
-- No cambia ninguna fila existente ni toca ninguna otra tabla.

ALTER TABLE notificacion DROP CONSTRAINT ck_notificacion_tipo;

ALTER TABLE notificacion
    ADD CONSTRAINT ck_notificacion_tipo CHECK (tipo_notificacion IN
        ('vencimiento_proximo', 'novedad_registrada', 'cambio_riesgo', 'pendiente_vencido'));

CREATE UNIQUE INDEX uq_notificacion_vencido_por_pendiente
    ON notificacion (id_pendiente) WHERE tipo_notificacion = 'pendiente_vencido';
