-- SCRUM-47: código de 6 dígitos para verificar el correo institucional (RF01).
-- Una fila por estudiante: pedir un código nuevo reemplaza el anterior.
-- Se guarda la huella SHA-256 del código, nunca el código en claro.
CREATE TABLE cundiapp.codigo_verificacion (
    id_estudiante      INTEGER      NOT NULL,
    hash_codigo        VARCHAR(64)  NOT NULL,
    fecha_emision      TIMESTAMPTZ  NOT NULL,
    fecha_expiracion   TIMESTAMPTZ  NOT NULL,
    intentos_fallidos  INTEGER      NOT NULL DEFAULT 0,
    fecha_uso          TIMESTAMPTZ,
    CONSTRAINT pk_codigo_verificacion PRIMARY KEY (id_estudiante),
    CONSTRAINT fk_codigo_verificacion_estudiante FOREIGN KEY (id_estudiante)
        REFERENCES cundiapp.estudiante (id_estudiante) ON DELETE CASCADE,
    CONSTRAINT ck_codigo_verificacion_hash CHECK (hash_codigo ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_codigo_verificacion_fechas CHECK (fecha_expiracion > fecha_emision),
    CONSTRAINT ck_codigo_verificacion_intentos CHECK (intentos_fallidos BETWEEN 0 AND 5)
);
