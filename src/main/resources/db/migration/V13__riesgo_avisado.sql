-- SCRUM-50: recibir un aviso cuando cambia el riesgo de una asignatura (RF10).
--
-- El riesgo de una asignatura NO se guarda: se calcula en cada consulta (RF09). Pero para avisar «cambió»
-- hay que saber en qué nivel estaba la última vez que se le dijo a la persona. Esta tabla guarda solo eso:
-- el último nivel conocido de cada asignatura matriculada, y se compara con el nuevo cada vez que algo
-- que lo afecta cambia (una nota, la meta, los umbrales o la estructura de evaluación).
--
-- Una fila por matrícula. La primera vez que se calcula el riesgo de una asignatura solo se guarda el
-- nivel, sin avisar: no hay un «antes» con el que compararlo. Si la matrícula se borra, la fila se va con ella.

CREATE TABLE riesgo_avisado (
    id_matricula        INTEGER     NOT NULL,
    nivel               VARCHAR(20) NOT NULL,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_riesgo_avisado PRIMARY KEY (id_matricula),
    CONSTRAINT fk_riesgo_avisado_matricula FOREIGN KEY (id_matricula)
        REFERENCES matricula_asignatura (id_matricula) ON DELETE CASCADE,
    CONSTRAINT ck_riesgo_avisado_nivel CHECK (nivel IN ('alto', 'medio', 'bajo', 'sin_datos'))
);
