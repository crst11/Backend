-- SCRUM-26: organizar mi horario a mi gusto (RF04).
--
-- La preferencia con la que el estudiante quiere ver su horario se guarda en su configuración y no en
-- el navegador, para que lo siga cuando cambie de teléfono. Son tres campos:
--
--   horario_vista  con qué vista abre: el día o la semana.
--   horario_sede   de qué sede quiere ver las clases; nulo es todas.
--   horario_orden  cómo se ordenan las clases dentro de un día: por hora o por asignatura.
--
-- El filtro por asignatura NO se guarda: cambia cada período, y una preferencia atada a una materia
-- que ya no se cursa dejaría el horario vacío sin explicación.
--
-- Todos tienen valor por defecto, así que las filas que ya existen (una por estudiante) quedan con
-- "día, todas las sedes, por hora" y nadie ve nada distinto hasta que elija otra cosa.

ALTER TABLE configuracion_estudiante
    ADD COLUMN horario_vista VARCHAR(10) NOT NULL DEFAULT 'dia',
    ADD COLUMN horario_sede  VARCHAR(60),
    ADD COLUMN horario_orden VARCHAR(15) NOT NULL DEFAULT 'hora',
    ADD CONSTRAINT ck_configuracion_horario_vista CHECK (horario_vista IN ('dia', 'semana')),
    ADD CONSTRAINT ck_configuracion_horario_orden CHECK (horario_orden IN ('hora', 'asignatura'));
