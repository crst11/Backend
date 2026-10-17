-- SCRUM-38: configurar mis avisos (RF10).
--
-- La configuración del estudiante ya guardaba el interruptor general de los avisos
-- (`notificaciones_activas`) y con cuánta anticipación avisar (`anticipacion_aviso_horas`, 24 por defecto).
-- Faltaba lo que pide la historia: «activar o desactivar cada tipo». Se agrega un interruptor por tipo:
--
--   avisar_vencimientos   el aviso de un pendiente por vencer (SCRUM-37).
--   avisar_cambio_riesgo  el aviso de que una asignatura cambió de nivel de riesgo (SCRUM-50).
--
-- Los dos arrancan encendidos, así que nadie nota nada hasta que apague uno. El interruptor general sigue
-- mandando: apagado, no sale ningún aviso sin importar estos.
--
-- La anticipación ya tenía un mínimo de cero; ahora también un máximo de una semana (168 horas). Avisar con
-- meses de adelanto no es un recordatorio sino ruido, y sin tope la tarea que busca pendientes por vencer
-- miraría una ventana enorme en cada ronda.

ALTER TABLE configuracion_estudiante
    ADD COLUMN avisar_vencimientos  BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN avisar_cambio_riesgo BOOLEAN NOT NULL DEFAULT TRUE,
    ADD CONSTRAINT ck_configuracion_anticipacion_maxima CHECK (anticipacion_aviso_horas <= 168);
