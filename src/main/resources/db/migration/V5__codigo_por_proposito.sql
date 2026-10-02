-- SCRUM-68: recuperar la contraseña con un código enviado al correo institucional (RF01).
-- El código de recuperación necesita lo mismo que el de verificación: huella SHA-256, vigencia de 15
-- minutos, 5 intentos y un solo uso. En vez de duplicar la tabla y su lógica, se agrega el propósito
-- y la llave primaria pasa a ser (estudiante, propósito): cada cuenta puede tener uno de cada tipo
-- al mismo tiempo sin que uno pise al otro.
ALTER TABLE cundiapp.codigo_verificacion
    ADD COLUMN proposito VARCHAR(30) NOT NULL DEFAULT 'verificar_correo';

-- Las filas que ya existen son todas de verificación del correo: el DEFAULT las deja correctas.
-- Se quita el valor por defecto para que de aquí en adelante el propósito se escriba siempre.
ALTER TABLE cundiapp.codigo_verificacion
    ALTER COLUMN proposito DROP DEFAULT;

ALTER TABLE cundiapp.codigo_verificacion
    DROP CONSTRAINT pk_codigo_verificacion;

ALTER TABLE cundiapp.codigo_verificacion
    ADD CONSTRAINT pk_codigo_verificacion PRIMARY KEY (id_estudiante, proposito);

ALTER TABLE cundiapp.codigo_verificacion
    ADD CONSTRAINT ck_codigo_verificacion_proposito
        CHECK (proposito IN ('verificar_correo', 'recuperar_contrasena'));
