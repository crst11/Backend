-- SCRUM-266: actividades extra en el horario.
--
-- Una actividad extra es algo de la universidad con día y hora que no es una asignatura: una clase de baile, un
-- semillero, deporte. Se ve en Hoy y en Mi horario como una clase más. No puede ser una asignatura falsa: entraría
-- en créditos, notas, promedio y Brújula Académica, y ninguna de esas cosas le corresponde.
--
-- Una fila por día: «lunes y miércoles» son dos filas, y cada una se quita sola. El lugar y el responsable son texto
-- libre y opcionales («Cancha 2», «Profe Ana»): espacio_fisico exige sede y docente exige apellidos.
--
-- Como el resto de las tablas del estudiante (V17) lleva la seguridad a nivel de fila activa, sin FORCE y sin políticas:
-- la aplicación se conecta como dueña de la tabla y la API del proveedor (que no es dueña) no ve nada. Al borrar al
-- estudiante se borran sus actividades.

CREATE TABLE actividad_extra (
    id_actividad_extra SERIAL      PRIMARY KEY,
    id_estudiante      INTEGER     NOT NULL,
    nombre             VARCHAR(80) NOT NULL,
    dia_semana         VARCHAR(15) NOT NULL,
    hora_inicio        TIME        NOT NULL,
    hora_fin           TIME        NOT NULL,
    lugar              VARCHAR(80),
    responsable        VARCHAR(80),
    CONSTRAINT fk_actividad_extra_estudiante FOREIGN KEY (id_estudiante)
        REFERENCES estudiante (id_estudiante) ON DELETE CASCADE,
    CONSTRAINT ck_actividad_extra_nombre CHECK (length(btrim(nombre)) > 0),
    CONSTRAINT ck_actividad_extra_dia CHECK (dia_semana IN
        ('lunes', 'martes', 'miercoles', 'jueves', 'viernes', 'sabado', 'domingo')),
    CONSTRAINT ck_actividad_extra_horas CHECK (hora_inicio < hora_fin)
);

CREATE INDEX ix_actividad_extra_estudiante ON actividad_extra (id_estudiante);

ALTER TABLE actividad_extra ENABLE ROW LEVEL SECURITY;
