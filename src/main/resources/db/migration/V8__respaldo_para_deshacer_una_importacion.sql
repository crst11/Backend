-- SCRUM-24: poder deshacer la última importación (RF03).
--
-- El esquema ya reservaba el estado 'revertida' en importacion, pero no guardaba en ninguna parte
-- cómo estaban las filas antes de que una importación las pisara. Sin eso, "deshacer" solo podría
-- borrar, y le quitaría al estudiante notas anteriores que sí quería conservar.
--
-- Estas dos tablas guardan el estado previo de lo que cada importación toca. Una fila con
-- existia_antes = false significa que la importación la creó: deshacer la elimina. Con true,
-- deshacer devuelve los valores guardados aquí.
--
-- Se borran en cascada con la importación: el respaldo no tiene sentido sin ella.

CREATE TABLE respaldo_matricula (
    id_respaldo        SERIAL,
    id_importacion     INTEGER      NOT NULL,
    id_estudiante      INTEGER      NOT NULL,
    codigo_asignatura  VARCHAR(20)  NOT NULL,
    codigo_periodo     VARCHAR(10)  NOT NULL,
    existia_antes      BOOLEAN      NOT NULL,
    estado_anterior    VARCHAR(20),
    nota_anterior      NUMERIC(3,2),
    fuente_anterior    VARCHAR(30),
    CONSTRAINT pk_respaldo_matricula PRIMARY KEY (id_respaldo),
    CONSTRAINT fk_respaldo_matricula_importacion FOREIGN KEY (id_importacion)
        REFERENCES importacion (id_importacion) ON DELETE CASCADE,
    CONSTRAINT fk_respaldo_matricula_estudiante FOREIGN KEY (id_estudiante)
        REFERENCES estudiante (id_estudiante) ON DELETE CASCADE,
    -- Una importación respalda cada matrícula una sola vez.
    CONSTRAINT uk_respaldo_matricula UNIQUE (id_importacion, id_estudiante, codigo_asignatura, codigo_periodo),
    CONSTRAINT ck_respaldo_matricula_nota CHECK (nota_anterior IS NULL OR nota_anterior BETWEEN 0 AND 5),
    -- Si no existía antes no hay nada que devolver; si existía, el estado anterior es obligatorio.
    CONSTRAINT ck_respaldo_matricula_coherente CHECK (
        (NOT existia_antes AND estado_anterior IS NULL AND nota_anterior IS NULL AND fuente_anterior IS NULL)
        OR (existia_antes AND estado_anterior IS NOT NULL))
);

CREATE INDEX ix_respaldo_matricula_importacion ON respaldo_matricula (id_importacion);

CREATE TABLE respaldo_resumen_periodo (
    id_respaldo                    SERIAL,
    id_importacion                 INTEGER      NOT NULL,
    id_estudiante                  INTEGER      NOT NULL,
    codigo_periodo                 VARCHAR(10)  NOT NULL,
    existia_antes                  BOOLEAN      NOT NULL,
    creditos_matriculados_anterior INTEGER,
    creditos_aprobados_anterior    INTEGER,
    promedio_periodo_anterior      NUMERIC(3,2),
    promedio_acumulado_anterior    NUMERIC(3,2),
    fuente_anterior                VARCHAR(30),
    CONSTRAINT pk_respaldo_resumen PRIMARY KEY (id_respaldo),
    CONSTRAINT fk_respaldo_resumen_importacion FOREIGN KEY (id_importacion)
        REFERENCES importacion (id_importacion) ON DELETE CASCADE,
    CONSTRAINT fk_respaldo_resumen_estudiante FOREIGN KEY (id_estudiante)
        REFERENCES estudiante (id_estudiante) ON DELETE CASCADE,
    CONSTRAINT uk_respaldo_resumen UNIQUE (id_importacion, id_estudiante, codigo_periodo),
    CONSTRAINT ck_respaldo_resumen_promedios CHECK (
        (promedio_periodo_anterior   IS NULL OR promedio_periodo_anterior   BETWEEN 0 AND 5) AND
        (promedio_acumulado_anterior IS NULL OR promedio_acumulado_anterior BETWEEN 0 AND 5)),
    CONSTRAINT ck_respaldo_resumen_coherente CHECK (
        existia_antes OR (creditos_matriculados_anterior IS NULL AND creditos_aprobados_anterior IS NULL
                          AND promedio_periodo_anterior IS NULL AND promedio_acumulado_anterior IS NULL
                          AND fuente_anterior IS NULL))
);

CREATE INDEX ix_respaldo_resumen_importacion ON respaldo_resumen_periodo (id_importacion);
