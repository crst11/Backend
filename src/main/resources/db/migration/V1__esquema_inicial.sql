-- ============================================================================
--  CundiApp - esquema inicial (PostgreSQL 16)
--
--  Fuente: diccionario de datos del documento V1 (27 tablas, 204 campos) y sus
--  14 restricciones de integridad. Los nombres de tablas y columnas son los del
--  diccionario, en minúsculas.
--
--  Reglas de esta migración:
--    * No borra nada (no hay DROP): Flyway la aplica una sola vez.
--    * Los cálculos (nota acumulada, nota requerida, riesgo, avance) no son
--      columnas: salen de las vistas del final.
--    * No se guarda el documento de identidad de estudiantes ni de docentes.
--    * Los "consecutivos" de las entidades débiles (consec_*) los asigna la
--      aplicación: siguiente = máximo actual + 1 dentro de su propietario.
--    * Los valores de los campos de estado son los de los CHECK de cada tabla,
--      en minúsculas y sin tildes. Caben en el tamaño que fija el diccionario:
--      estado_cuenta y estado_importacion usan pendiente (esperando el código
--      de verificación o la confirmación) y estado_entrega usa sin_calificar
--      (entregada, todavía sin nota).
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS cundiapp;
SET search_path TO cundiapp;

-- ============================================================================
--  1. CATÁLOGO ACADÉMICO
-- ============================================================================

CREATE TABLE programa_academico (
    codigo_programa  VARCHAR(20)  NOT NULL,
    nombre_programa  VARCHAR(120) NOT NULL,
    facultad         VARCHAR(80)  NOT NULL,
    sede             VARCHAR(60)  NOT NULL,
    total_creditos   INTEGER      NOT NULL,
    numero_periodos  INTEGER      NOT NULL,
    CONSTRAINT pk_programa_academico PRIMARY KEY (codigo_programa),
    CONSTRAINT ck_programa_creditos  CHECK (total_creditos > 0),
    CONSTRAINT ck_programa_periodos  CHECK (numero_periodos > 0)
);

CREATE TABLE plan_de_estudios (
    codigo_plan      VARCHAR(20) NOT NULL,
    codigo_programa  VARCHAR(20) NOT NULL,
    version          VARCHAR(20) NOT NULL,
    anio_vigencia    INTEGER     NOT NULL,
    estado_plan      VARCHAR(20) NOT NULL DEFAULT 'vigente',
    CONSTRAINT pk_plan_de_estudios PRIMARY KEY (codigo_plan),
    CONSTRAINT fk_plan_programa    FOREIGN KEY (codigo_programa) REFERENCES programa_academico (codigo_programa),
    CONSTRAINT ck_plan_estado      CHECK (estado_plan IN ('vigente', 'cerrado'))
);

CREATE TABLE asignatura (
    codigo_asignatura  VARCHAR(20)  NOT NULL,
    codigo_plan        VARCHAR(20)  NOT NULL,
    nombre_asignatura  VARCHAR(120) NOT NULL,
    creditos           INTEGER      NOT NULL,
    tipo_asignatura    VARCHAR(30)  NOT NULL DEFAULT 'obligatoria',
    periodo_sugerido   INTEGER,
    CONSTRAINT pk_asignatura       PRIMARY KEY (codigo_asignatura),
    CONSTRAINT fk_asignatura_plan  FOREIGN KEY (codigo_plan) REFERENCES plan_de_estudios (codigo_plan),
    CONSTRAINT ck_asignatura_creditos CHECK (creditos >= 0),
    CONSTRAINT ck_asignatura_tipo  CHECK (tipo_asignatura IN ('obligatoria', 'electiva', 'profundizacion')),
    CONSTRAINT ck_asignatura_periodo CHECK (periodo_sugerido IS NULL OR periodo_sugerido >= 1)
);

CREATE TABLE prerrequisito (
    codigo_asignatura  VARCHAR(20) NOT NULL,
    codigo_requerida   VARCHAR(20) NOT NULL,
    tipo_requisito     VARCHAR(30) NOT NULL DEFAULT 'prerrequisito',
    CONSTRAINT pk_prerrequisito PRIMARY KEY (codigo_asignatura, codigo_requerida),
    CONSTRAINT fk_prerreq_asignatura FOREIGN KEY (codigo_asignatura) REFERENCES asignatura (codigo_asignatura) ON DELETE CASCADE,
    CONSTRAINT fk_prerreq_requerida  FOREIGN KEY (codigo_requerida)  REFERENCES asignatura (codigo_asignatura) ON DELETE CASCADE,
    CONSTRAINT ck_prerreq_tipo       CHECK (tipo_requisito IN ('prerrequisito', 'correquisito')),
    -- Restricción 5 (parte 1): una asignatura no puede ser prerrequisito de sí misma
    CONSTRAINT ck_prerreq_no_a_si_misma CHECK (codigo_asignatura <> codigo_requerida)
);

CREATE TABLE periodo_academico (
    codigo_periodo  VARCHAR(10) NOT NULL,
    anio            INTEGER     NOT NULL,
    semestre        INTEGER     NOT NULL,
    fecha_inicio    DATE        NOT NULL,
    fecha_fin       DATE        NOT NULL,
    estado_periodo  VARCHAR(20) NOT NULL DEFAULT 'programado',
    CONSTRAINT pk_periodo_academico PRIMARY KEY (codigo_periodo),
    CONSTRAINT ck_periodo_semestre  CHECK (semestre IN (1, 2)),
    CONSTRAINT ck_periodo_fechas    CHECK (fecha_fin > fecha_inicio),
    CONSTRAINT ck_periodo_estado    CHECK (estado_periodo IN ('programado', 'en_curso', 'finalizado'))
);

-- ============================================================================
--  2. IDENTIDAD Y ACCESO
-- ============================================================================

CREATE TABLE estudiante (
    id_estudiante         SERIAL,
    codigo_plan           VARCHAR(20),
    nombres               VARCHAR(80)  NOT NULL,
    apellidos             VARCHAR(80)  NOT NULL,
    correo_institucional  VARCHAR(120) NOT NULL,
    url_foto              VARCHAR(500),
    estado_cuenta         VARCHAR(20)  NOT NULL DEFAULT 'pendiente',
    consentimiento_datos  BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_consentimiento  TIMESTAMPTZ,
    fecha_registro        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_estudiante PRIMARY KEY (id_estudiante),
    CONSTRAINT fk_estudiante_plan FOREIGN KEY (codigo_plan) REFERENCES plan_de_estudios (codigo_plan),
    CONSTRAINT ck_estudiante_estado CHECK (estado_cuenta IN ('pendiente', 'activa', 'inactiva')),
    -- Ley 1581: si hay consentimiento, queda registrada la fecha en que se otorgó
    CONSTRAINT ck_estudiante_consentimiento CHECK (NOT consentimiento_datos OR fecha_consentimiento IS NOT NULL)
);
-- El correo no se repite, sin distinguir mayúsculas de minúsculas
CREATE UNIQUE INDEX uk_estudiante_correo ON estudiante (lower(correo_institucional));

CREATE TABLE credencial_acceso (
    id_estudiante          INTEGER      NOT NULL,
    proveedor              VARCHAR(20)  NOT NULL,
    identificador_externo  VARCHAR(255),
    hash_contrasena        VARCHAR(60),
    correo_proveedor       VARCHAR(120) NOT NULL,
    correo_verificado      BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_vinculacion      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_ultimo_acceso    TIMESTAMPTZ,
    activa                 BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_credencial_acceso PRIMARY KEY (id_estudiante, proveedor),
    CONSTRAINT fk_credencial_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiante (id_estudiante) ON DELETE CASCADE,
    -- Restricción 10: el identificador de un proveedor externo no se repite entre cuentas
    CONSTRAINT uk_credencial_identificador_externo UNIQUE (identificador_externo),
    CONSTRAINT ck_credencial_proveedor CHECK (proveedor IN ('local', 'google')),
    -- Restricción 9 (parte 1): cada método es coherente consigo mismo.
    --   local: exige contraseña y no admite identificador externo
    --   federado: exige identificador externo y no admite contraseña
    CONSTRAINT ck_credencial_coherente CHECK (
        (proveedor = 'local'  AND hash_contrasena IS NOT NULL AND identificador_externo IS NULL)
     OR (proveedor <> 'local' AND identificador_externo IS NOT NULL AND hash_contrasena IS NULL)
    )
);

CREATE TABLE dispositivo (
    id_estudiante         INTEGER      NOT NULL,
    consec_dispositivo    INTEGER      NOT NULL,
    nombre_dispositivo    VARCHAR(80)  NOT NULL,
    endpoint_push         VARCHAR(500),
    clave_p256dh          VARCHAR(120),
    clave_auth            VARCHAR(60),
    fecha_sincronizacion  TIMESTAMPTZ,
    activo                BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_dispositivo PRIMARY KEY (id_estudiante, consec_dispositivo),
    CONSTRAINT fk_dispositivo_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiante (id_estudiante) ON DELETE CASCADE,
    CONSTRAINT ck_dispositivo_consec CHECK (consec_dispositivo > 0),
    -- La suscripción push está completa o no existe (un dispositivo puede usarse sin avisos)
    CONSTRAINT ck_dispositivo_push CHECK (
        (endpoint_push IS NULL AND clave_p256dh IS NULL AND clave_auth IS NULL)
     OR (endpoint_push IS NOT NULL AND clave_p256dh IS NOT NULL AND clave_auth IS NOT NULL)
    )
);

CREATE TABLE sesion (
    id_estudiante        INTEGER      NOT NULL,
    consec_sesion        INTEGER      NOT NULL,
    consec_dispositivo   INTEGER,
    proveedor_origen     VARCHAR(20)  NOT NULL,
    hash_token_refresco  VARCHAR(64)  NOT NULL,
    fecha_inicio         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_expiracion     TIMESTAMPTZ  NOT NULL,
    fecha_revocacion     TIMESTAMPTZ,
    motivo_revocacion    VARCHAR(40),
    user_agent           VARCHAR(200),
    ip_origen            VARCHAR(45),
    CONSTRAINT pk_sesion PRIMARY KEY (id_estudiante, consec_sesion),
    CONSTRAINT fk_sesion_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiante (id_estudiante) ON DELETE CASCADE,
    CONSTRAINT fk_sesion_dispositivo FOREIGN KEY (id_estudiante, consec_dispositivo)
        REFERENCES dispositivo (id_estudiante, consec_dispositivo) ON DELETE SET NULL (consec_dispositivo),
    -- Restricción 11 (parte 1): la huella del token de refresco es única
    CONSTRAINT uk_sesion_hash_token UNIQUE (hash_token_refresco),
    CONSTRAINT ck_sesion_consec CHECK (consec_sesion > 0),
    CONSTRAINT ck_sesion_proveedor CHECK (proveedor_origen IN ('local', 'google')),
    CONSTRAINT ck_sesion_hash CHECK (hash_token_refresco ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_sesion_fechas CHECK (fecha_expiracion > fecha_inicio),
    CONSTRAINT ck_sesion_motivo CHECK (motivo_revocacion IS NULL OR motivo_revocacion IN
        ('cierre_sesion', 'rotacion', 'reuso_detectado', 'expiracion')),
    -- La fecha y el motivo de revocación van juntos
    CONSTRAINT ck_sesion_revocacion CHECK ((fecha_revocacion IS NULL) = (motivo_revocacion IS NULL))
);

CREATE TABLE configuracion_estudiante (
    id_estudiante             INTEGER       NOT NULL,
    meta_calificacion         NUMERIC(3,2)  NOT NULL DEFAULT 3.0,
    umbral_riesgo_medio       NUMERIC(3,2)  NOT NULL DEFAULT 3.5,
    umbral_riesgo_alto        NUMERIC(3,2)  NOT NULL DEFAULT 4.5,
    anticipacion_aviso_horas  INTEGER       NOT NULL DEFAULT 24,
    notificaciones_activas    BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_configuracion_estudiante PRIMARY KEY (id_estudiante),
    CONSTRAINT fk_configuracion_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiante (id_estudiante) ON DELETE CASCADE,
    CONSTRAINT ck_configuracion_meta   CHECK (meta_calificacion BETWEEN 0 AND 5),
    CONSTRAINT ck_configuracion_umbrales CHECK (
        umbral_riesgo_medio BETWEEN 0 AND 5 AND umbral_riesgo_alto BETWEEN 0 AND 5
        AND umbral_riesgo_medio < umbral_riesgo_alto),
    CONSTRAINT ck_configuracion_anticipacion CHECK (anticipacion_aviso_horas >= 0)
);

CREATE TABLE importacion (
    id_importacion         SERIAL,
    id_estudiante          INTEGER      NOT NULL,
    tipo_reporte           VARCHAR(40)  NOT NULL,
    nombre_archivo         VARCHAR(200) NOT NULL,
    fecha_carga            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    estado_importacion     VARCHAR(20)  NOT NULL DEFAULT 'pendiente',
    registros_detectados   INTEGER      NOT NULL DEFAULT 0,
    registros_confirmados  INTEGER      NOT NULL DEFAULT 0,
    mensaje_resultado      VARCHAR(300),
    CONSTRAINT pk_importacion PRIMARY KEY (id_importacion),
    CONSTRAINT fk_importacion_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiante (id_estudiante) ON DELETE CASCADE,
    CONSTRAINT ck_importacion_tipo CHECK (tipo_reporte IN ('horario', 'historial', 'plan_de_estudios')),
    CONSTRAINT ck_importacion_estado CHECK (estado_importacion IN
        ('pendiente', 'confirmada', 'revertida', 'fallida')),
    CONSTRAINT ck_importacion_registros CHECK (
        registros_detectados >= 0 AND registros_confirmados >= 0
        AND registros_confirmados <= registros_detectados)
);

-- ============================================================================
--  3. MATRÍCULA
-- ============================================================================

CREATE TABLE plantilla_evaluacion (
    id_plantilla      SERIAL,
    nombre_plantilla  VARCHAR(80)  NOT NULL,
    descripcion       VARCHAR(200),
    es_predeterminada BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_plantilla_evaluacion PRIMARY KEY (id_plantilla),
    CONSTRAINT uk_plantilla_nombre UNIQUE (nombre_plantilla)
);
-- A lo sumo una plantilla es la predeterminada
CREATE UNIQUE INDEX uk_plantilla_predeterminada ON plantilla_evaluacion (es_predeterminada) WHERE es_predeterminada;

CREATE TABLE item_de_plantilla (
    id_plantilla  INTEGER      NOT NULL,
    consec_item   INTEGER      NOT NULL,
    nombre_item   VARCHAR(80)  NOT NULL,
    porcentaje    NUMERIC(5,2) NOT NULL,
    CONSTRAINT pk_item_de_plantilla PRIMARY KEY (id_plantilla, consec_item),
    CONSTRAINT fk_item_plantilla FOREIGN KEY (id_plantilla) REFERENCES plantilla_evaluacion (id_plantilla) ON DELETE CASCADE,
    CONSTRAINT ck_item_consec CHECK (consec_item > 0),
    CONSTRAINT ck_item_porcentaje CHECK (porcentaje > 0 AND porcentaje <= 100)
);

CREATE TABLE matricula_asignatura (
    id_matricula       SERIAL,
    id_estudiante      INTEGER      NOT NULL,
    codigo_asignatura  VARCHAR(20)  NOT NULL,
    codigo_periodo     VARCHAR(10)  NOT NULL,
    id_plantilla       INTEGER,
    id_importacion     INTEGER,
    codigo_grupo       VARCHAR(20),
    prioridad          VARCHAR(20)  NOT NULL DEFAULT 'media',
    estado_matricula   VARCHAR(20)  NOT NULL DEFAULT 'en_curso',
    nota_final         NUMERIC(3,2),
    nota_habilitacion  NUMERIC(3,2),
    nota_definitiva    NUMERIC(3,2),
    fuente_notas       VARCHAR(30)  NOT NULL DEFAULT 'manual',
    fallas             INTEGER      NOT NULL DEFAULT 0,
    actualizado_en     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_matricula_asignatura PRIMARY KEY (id_matricula),
    CONSTRAINT fk_matricula_estudiante  FOREIGN KEY (id_estudiante)     REFERENCES estudiante (id_estudiante) ON DELETE CASCADE,
    CONSTRAINT fk_matricula_asignatura  FOREIGN KEY (codigo_asignatura) REFERENCES asignatura (codigo_asignatura),
    CONSTRAINT fk_matricula_periodo     FOREIGN KEY (codigo_periodo)    REFERENCES periodo_academico (codigo_periodo),
    CONSTRAINT fk_matricula_plantilla   FOREIGN KEY (id_plantilla)      REFERENCES plantilla_evaluacion (id_plantilla) ON DELETE SET NULL,
    CONSTRAINT fk_matricula_importacion FOREIGN KEY (id_importacion)    REFERENCES importacion (id_importacion) ON DELETE SET NULL,
    -- Restricción 4: estudiante, asignatura y período son únicos en la matrícula
    CONSTRAINT uk_matricula_estudiante_asignatura_periodo UNIQUE (id_estudiante, codigo_asignatura, codigo_periodo),
    CONSTRAINT ck_matricula_prioridad CHECK (prioridad IN ('alta', 'media', 'baja')),
    CONSTRAINT ck_matricula_estado CHECK (estado_matricula IN ('en_curso', 'aprobada', 'reprobada', 'cancelada')),
    CONSTRAINT ck_matricula_fuente CHECK (fuente_notas IN ('importacion', 'manual')),
    -- Restricción 6 (aplicada también a las notas consolidadas): entre 0.0 y 5.0
    CONSTRAINT ck_matricula_notas CHECK (
        (nota_final        IS NULL OR nota_final        BETWEEN 0 AND 5) AND
        (nota_habilitacion IS NULL OR nota_habilitacion BETWEEN 0 AND 5) AND
        (nota_definitiva   IS NULL OR nota_definitiva   BETWEEN 0 AND 5)),
    CONSTRAINT ck_matricula_fallas CHECK (fallas >= 0)
);

CREATE TABLE resumen_periodo (
    id_estudiante          INTEGER      NOT NULL,
    codigo_periodo         VARCHAR(10)  NOT NULL,
    id_importacion         INTEGER,
    creditos_matriculados  INTEGER      NOT NULL DEFAULT 0,
    creditos_aprobados     INTEGER      NOT NULL DEFAULT 0,
    promedio_periodo       NUMERIC(3,2),
    promedio_acumulado     NUMERIC(3,2),
    fuente                 VARCHAR(30)  NOT NULL DEFAULT 'importacion',
    CONSTRAINT pk_resumen_periodo PRIMARY KEY (id_estudiante, codigo_periodo),
    CONSTRAINT fk_resumen_estudiante  FOREIGN KEY (id_estudiante)  REFERENCES estudiante (id_estudiante) ON DELETE CASCADE,
    CONSTRAINT fk_resumen_periodo     FOREIGN KEY (codigo_periodo) REFERENCES periodo_academico (codigo_periodo),
    CONSTRAINT fk_resumen_importacion FOREIGN KEY (id_importacion) REFERENCES importacion (id_importacion) ON DELETE SET NULL,
    CONSTRAINT ck_resumen_creditos CHECK (
        creditos_matriculados >= 0 AND creditos_aprobados >= 0 AND creditos_aprobados <= creditos_matriculados),
    CONSTRAINT ck_resumen_promedios CHECK (
        (promedio_periodo   IS NULL OR promedio_periodo   BETWEEN 0 AND 5) AND
        (promedio_acumulado IS NULL OR promedio_acumulado BETWEEN 0 AND 5)),
    CONSTRAINT ck_resumen_fuente CHECK (fuente IN ('importacion', 'manual'))
);

-- ============================================================================
--  4. HORARIO
-- ============================================================================

CREATE TABLE docente (
    id_docente            SERIAL,
    nombres               VARCHAR(80)  NOT NULL,
    apellidos             VARCHAR(80)  NOT NULL,
    nombre_normalizado    VARCHAR(160) NOT NULL,
    correo_institucional  VARCHAR(120),
    CONSTRAINT pk_docente PRIMARY KEY (id_docente),
    CONSTRAINT uk_docente_nombre_normalizado UNIQUE (nombre_normalizado)
);

CREATE TABLE espacio_fisico (
    id_espacio    SERIAL,
    nomenclatura  VARCHAR(30) NOT NULL,
    bloque        VARCHAR(30),
    sede          VARCHAR(60) NOT NULL,
    tipo_espacio  VARCHAR(30) NOT NULL DEFAULT 'aula',
    CONSTRAINT pk_espacio_fisico PRIMARY KEY (id_espacio),
    CONSTRAINT ck_espacio_tipo CHECK (tipo_espacio IN ('aula', 'laboratorio', 'auditorio'))
);

CREATE TABLE bloque_de_horario (
    id_matricula    INTEGER     NOT NULL,
    consec_bloque   INTEGER     NOT NULL,
    id_espacio      INTEGER,
    id_docente      INTEGER,
    id_importacion  INTEGER,
    dia_semana      VARCHAR(15) NOT NULL,
    hora_inicio     TIME        NOT NULL,
    hora_fin        TIME        NOT NULL,
    CONSTRAINT pk_bloque_de_horario PRIMARY KEY (id_matricula, consec_bloque),
    CONSTRAINT fk_bloque_matricula   FOREIGN KEY (id_matricula)   REFERENCES matricula_asignatura (id_matricula) ON DELETE CASCADE,
    CONSTRAINT fk_bloque_espacio     FOREIGN KEY (id_espacio)     REFERENCES espacio_fisico (id_espacio) ON DELETE SET NULL,
    CONSTRAINT fk_bloque_docente     FOREIGN KEY (id_docente)     REFERENCES docente (id_docente) ON DELETE SET NULL,
    CONSTRAINT fk_bloque_importacion FOREIGN KEY (id_importacion) REFERENCES importacion (id_importacion) ON DELETE SET NULL,
    CONSTRAINT ck_bloque_consec CHECK (consec_bloque > 0),
    CONSTRAINT ck_bloque_dia CHECK (dia_semana IN
        ('lunes', 'martes', 'miercoles', 'jueves', 'viernes', 'sabado', 'domingo')),
    -- Restricción 7: la hora de inicio es anterior a la de fin
    CONSTRAINT ck_bloque_horas CHECK (hora_inicio < hora_fin)
);

-- ============================================================================
--  5. ESTRUCTURA DE EVALUACIÓN (árbol ponderado de dos niveles)
-- ============================================================================

CREATE TABLE categoria_evaluacion (
    id_matricula      INTEGER      NOT NULL,
    consec_categoria  INTEGER      NOT NULL,
    nombre_categoria  VARCHAR(80)  NOT NULL,
    porcentaje        NUMERIC(5,2) NOT NULL,
    origen_categoria  VARCHAR(30)  NOT NULL DEFAULT 'estudiante',
    actualizado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_categoria_evaluacion PRIMARY KEY (id_matricula, consec_categoria),
    CONSTRAINT fk_categoria_matricula FOREIGN KEY (id_matricula) REFERENCES matricula_asignatura (id_matricula) ON DELETE CASCADE,
    CONSTRAINT ck_categoria_consec CHECK (consec_categoria > 0),
    CONSTRAINT ck_categoria_porcentaje CHECK (porcentaje > 0 AND porcentaje <= 100),
    CONSTRAINT ck_categoria_origen CHECK (origen_categoria IN ('plantilla', 'estudiante'))
);

CREATE TABLE actividad_evaluativa (
    id_matricula       INTEGER      NOT NULL,
    consec_categoria   INTEGER      NOT NULL,
    consec_actividad   INTEGER      NOT NULL,
    nombre_actividad   VARCHAR(120) NOT NULL,
    porcentaje         NUMERIC(5,2) NOT NULL,
    fecha_programada   DATE,
    tipo_actividad     VARCHAR(30)  NOT NULL DEFAULT 'taller',
    estado_entrega     VARCHAR(20)  NOT NULL DEFAULT 'no_entregada',
    actualizado_en     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_actividad_evaluativa PRIMARY KEY (id_matricula, consec_categoria, consec_actividad),
    CONSTRAINT fk_actividad_categoria FOREIGN KEY (id_matricula, consec_categoria)
        REFERENCES categoria_evaluacion (id_matricula, consec_categoria) ON DELETE CASCADE,
    CONSTRAINT ck_actividad_consec CHECK (consec_actividad > 0),
    CONSTRAINT ck_actividad_porcentaje CHECK (porcentaje > 0 AND porcentaje <= 100),
    CONSTRAINT ck_actividad_tipo CHECK (tipo_actividad IN ('parcial', 'taller', 'quiz', 'exposicion')),
    CONSTRAINT ck_actividad_estado CHECK (estado_entrega IN
        ('no_entregada', 'sin_calificar', 'calificada'))
);

CREATE TABLE calificacion (
    id_matricula      INTEGER      NOT NULL,
    consec_categoria  INTEGER      NOT NULL,
    consec_actividad  INTEGER      NOT NULL,
    nota_obtenida     NUMERIC(3,2) NOT NULL,
    fecha_registro    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    origen_registro   VARCHAR(30)  NOT NULL DEFAULT 'estudiante',
    observacion       VARCHAR(300),
    actualizado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- Relación identificadora 1:1 opcional con la actividad
    CONSTRAINT pk_calificacion PRIMARY KEY (id_matricula, consec_categoria, consec_actividad),
    CONSTRAINT fk_calificacion_actividad FOREIGN KEY (id_matricula, consec_categoria, consec_actividad)
        REFERENCES actividad_evaluativa (id_matricula, consec_categoria, consec_actividad) ON DELETE CASCADE,
    -- Restricción 6: toda calificación está entre 0.0 y 5.0
    CONSTRAINT ck_calificacion_nota CHECK (nota_obtenida BETWEEN 0 AND 5),
    CONSTRAINT ck_calificacion_origen CHECK (origen_registro IN ('estudiante', 'importacion'))
);

-- ============================================================================
--  6. PENDIENTES, SIMULACIONES Y AVISOS
-- ============================================================================

CREATE TABLE pendiente (
    id_pendiente      SERIAL,
    id_estudiante     INTEGER      NOT NULL,
    id_matricula      INTEGER,
    consec_categoria  INTEGER,
    consec_actividad  INTEGER,
    titulo            VARCHAR(120) NOT NULL,
    descripcion       VARCHAR(300),
    fecha_limite      TIMESTAMPTZ  NOT NULL,
    prioridad         VARCHAR(20)  NOT NULL DEFAULT 'media',
    estado_pendiente  VARCHAR(20)  NOT NULL DEFAULT 'abierto',
    fecha_creacion    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_pendiente PRIMARY KEY (id_pendiente),
    CONSTRAINT fk_pendiente_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiante (id_estudiante) ON DELETE CASCADE,
    -- Al borrar la matrícula se borran sus pendientes. Una categoría o una actividad
    -- con pendientes asociados no se puede borrar hasta desvincularlos (la
    -- aplicación pone en NULL consec_categoria y consec_actividad antes de borrar).
    CONSTRAINT fk_pendiente_matricula  FOREIGN KEY (id_matricula)  REFERENCES matricula_asignatura (id_matricula) ON DELETE CASCADE,
    CONSTRAINT fk_pendiente_categoria  FOREIGN KEY (id_matricula, consec_categoria)
        REFERENCES categoria_evaluacion (id_matricula, consec_categoria),
    CONSTRAINT fk_pendiente_actividad  FOREIGN KEY (id_matricula, consec_categoria, consec_actividad)
        REFERENCES actividad_evaluativa (id_matricula, consec_categoria, consec_actividad),
    CONSTRAINT ck_pendiente_prioridad CHECK (prioridad IN ('alta', 'media', 'baja')),
    CONSTRAINT ck_pendiente_estado CHECK (estado_pendiente IN ('abierto', 'cumplido', 'vencido')),
    -- Restricción 13: una categoría o una actividad solo se señalan junto con la
    -- matrícula a la que pertenecen (las llaves foráneas compuestas verifican
    -- que la categoría y la actividad existan dentro de esa matrícula)
    CONSTRAINT ck_pendiente_origen CHECK (
        (consec_categoria IS NULL OR id_matricula IS NOT NULL)
    AND (consec_actividad IS NULL OR consec_categoria IS NOT NULL))
);

CREATE TABLE notificacion (
    id_notificacion     SERIAL,
    id_estudiante       INTEGER      NOT NULL,
    consec_dispositivo  INTEGER,
    id_pendiente        INTEGER,
    id_matricula        INTEGER,
    consec_categoria    INTEGER,
    consec_actividad    INTEGER,
    tipo_notificacion   VARCHAR(40)  NOT NULL,
    titulo              VARCHAR(120) NOT NULL,
    mensaje             TEXT         NOT NULL,
    fecha_programada    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_generacion    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    fecha_envio         TIMESTAMPTZ,
    estado_envio        VARCHAR(20)  NOT NULL DEFAULT 'pendiente',
    leida               BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_notificacion PRIMARY KEY (id_notificacion),
    CONSTRAINT fk_notificacion_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiante (id_estudiante) ON DELETE CASCADE,
    CONSTRAINT fk_notificacion_dispositivo FOREIGN KEY (id_estudiante, consec_dispositivo)
        REFERENCES dispositivo (id_estudiante, consec_dispositivo) ON DELETE SET NULL (consec_dispositivo),
    CONSTRAINT fk_notificacion_pendiente FOREIGN KEY (id_pendiente) REFERENCES pendiente (id_pendiente) ON DELETE CASCADE,
    CONSTRAINT fk_notificacion_matricula FOREIGN KEY (id_matricula) REFERENCES matricula_asignatura (id_matricula) ON DELETE CASCADE,
    CONSTRAINT fk_notificacion_actividad FOREIGN KEY (id_matricula, consec_categoria, consec_actividad)
        REFERENCES actividad_evaluativa (id_matricula, consec_categoria, consec_actividad) ON DELETE CASCADE,
    CONSTRAINT ck_notificacion_tipo CHECK (tipo_notificacion IN
        ('vencimiento_proximo', 'novedad_registrada', 'cambio_riesgo')),
    CONSTRAINT ck_notificacion_estado CHECK (estado_envio IN ('pendiente', 'enviada', 'fallida')),
    -- Restricción 12: un aviso tiene a lo sumo un origen (un pendiente, una
    -- actividad evaluativa o una asignatura matriculada), nunca más de uno
    CONSTRAINT ck_notificacion_un_solo_origen CHECK (
        (id_pendiente IS NULL AND id_matricula IS NULL AND consec_categoria IS NULL AND consec_actividad IS NULL)
     OR (id_pendiente IS NOT NULL AND id_matricula IS NULL AND consec_categoria IS NULL AND consec_actividad IS NULL)
     OR (id_pendiente IS NULL AND id_matricula IS NOT NULL AND consec_categoria IS NOT NULL AND consec_actividad IS NOT NULL)
     OR (id_pendiente IS NULL AND id_matricula IS NOT NULL AND consec_categoria IS NULL AND consec_actividad IS NULL))
);

CREATE TABLE simulacion (
    id_simulacion     SERIAL,
    id_matricula      INTEGER      NOT NULL,
    fecha_simulacion  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    meta_utilizada    NUMERIC(3,2) NOT NULL,
    guardada          BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_simulacion PRIMARY KEY (id_simulacion),
    CONSTRAINT fk_simulacion_matricula FOREIGN KEY (id_matricula) REFERENCES matricula_asignatura (id_matricula) ON DELETE CASCADE,
    -- Destino de la llave compuesta de DETALLE_SIMULACION (restricción 14)
    CONSTRAINT uk_simulacion_matricula UNIQUE (id_simulacion, id_matricula),
    CONSTRAINT ck_simulacion_meta CHECK (meta_utilizada BETWEEN 0 AND 5)
);

CREATE TABLE detalle_simulacion (
    id_simulacion     INTEGER      NOT NULL,
    consec_detalle    INTEGER      NOT NULL,
    id_matricula      INTEGER      NOT NULL,
    consec_categoria  INTEGER      NOT NULL,
    consec_actividad  INTEGER      NOT NULL,
    nota_hipotetica   NUMERIC(3,2) NOT NULL,
    CONSTRAINT pk_detalle_simulacion PRIMARY KEY (id_simulacion, consec_detalle),
    -- Restricción 14: la actividad simulada pertenece a la misma matrícula de la
    -- simulación. La primera llave fija la matrícula de la simulación y la
    -- segunda exige que la actividad exista dentro de esa misma matrícula.
    CONSTRAINT fk_detalle_simulacion FOREIGN KEY (id_simulacion, id_matricula)
        REFERENCES simulacion (id_simulacion, id_matricula) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_actividad FOREIGN KEY (id_matricula, consec_categoria, consec_actividad)
        REFERENCES actividad_evaluativa (id_matricula, consec_categoria, consec_actividad) ON DELETE CASCADE,
    CONSTRAINT uk_detalle_actividad UNIQUE (id_simulacion, id_matricula, consec_categoria, consec_actividad),
    CONSTRAINT ck_detalle_consec CHECK (consec_detalle > 0),
    CONSTRAINT ck_detalle_nota CHECK (nota_hipotetica BETWEEN 0 AND 5)
);

-- ============================================================================
--  7. GUÍA INSTITUCIONAL (única parte accesible sin cuenta)
-- ============================================================================

CREATE TABLE categoria_de_recurso (
    id_categoria      SERIAL,
    nombre_categoria  VARCHAR(80)  NOT NULL,
    descripcion       VARCHAR(200),
    orden             INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT pk_categoria_de_recurso PRIMARY KEY (id_categoria),
    CONSTRAINT uk_categoria_recurso_nombre UNIQUE (nombre_categoria)
);

CREATE TABLE recurso_institucional (
    id_recurso              SERIAL,
    id_categoria            INTEGER      NOT NULL,
    titulo                  VARCHAR(160) NOT NULL,
    descripcion             VARCHAR(300),
    url                     VARCHAR(500) NOT NULL,
    tipo_recurso            VARCHAR(30)  NOT NULL DEFAULT 'documento',
    hash_contenido          VARCHAR(64),
    requiere_autenticacion  BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_publicacion       DATE,
    fecha_verificacion      DATE,
    estado_recurso          VARCHAR(25)  NOT NULL DEFAULT 'vigente',
    CONSTRAINT pk_recurso_institucional PRIMARY KEY (id_recurso),
    CONSTRAINT fk_recurso_categoria FOREIGN KEY (id_categoria) REFERENCES categoria_de_recurso (id_categoria),
    CONSTRAINT ck_recurso_tipo CHECK (tipo_recurso IN ('documento', 'formato', 'enlace')),
    CONSTRAINT ck_recurso_estado CHECK (estado_recurso IN ('vigente', 'pendiente_revision', 'retirado'))
);

-- ============================================================================
--  8. ÍNDICES sobre llaves foráneas que no cubre ninguna llave primaria o única
-- ============================================================================

CREATE INDEX ix_estudiante_plan            ON estudiante (codigo_plan);
CREATE INDEX ix_plan_programa              ON plan_de_estudios (codigo_programa);
CREATE INDEX ix_asignatura_plan            ON asignatura (codigo_plan);
CREATE INDEX ix_prerrequisito_requerida    ON prerrequisito (codigo_requerida);
CREATE INDEX ix_importacion_estudiante     ON importacion (id_estudiante);
CREATE INDEX ix_matricula_asignatura       ON matricula_asignatura (codigo_asignatura);
CREATE INDEX ix_matricula_periodo          ON matricula_asignatura (codigo_periodo);
CREATE INDEX ix_matricula_importacion      ON matricula_asignatura (id_importacion);
CREATE INDEX ix_resumen_periodo            ON resumen_periodo (codigo_periodo);
CREATE INDEX ix_bloque_importacion         ON bloque_de_horario (id_importacion);
CREATE INDEX ix_pendiente_estudiante_fecha ON pendiente (id_estudiante, fecha_limite);
CREATE INDEX ix_pendiente_matricula        ON pendiente (id_matricula, consec_categoria, consec_actividad);
CREATE INDEX ix_notificacion_estudiante    ON notificacion (id_estudiante, leida);
CREATE INDEX ix_notificacion_por_enviar    ON notificacion (fecha_programada) WHERE estado_envio = 'pendiente';
CREATE INDEX ix_simulacion_matricula       ON simulacion (id_matricula);
CREATE INDEX ix_recurso_categoria          ON recurso_institucional (id_categoria);
CREATE INDEX ix_sesion_vigente             ON sesion (id_estudiante, fecha_expiracion) WHERE fecha_revocacion IS NULL;

-- ============================================================================
--  9. REGLAS DE INTEGRIDAD QUE NO SE DERIVAN DE LAS LLAVES
--     Los disparadores "DEFERRABLE INITIALLY DEFERRED" se comprueban al final de
--     la transacción, para poder insertar un conjunto completo (por ejemplo las
--     categorías de una matrícula) antes de exigir que sume 100 %.
-- ============================================================================

-- Marca de la última modificación (base de la sincronización sin conexión)
CREATE FUNCTION marcar_actualizacion() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    NEW.actualizado_en := now();
    RETURN NEW;
END $$;

CREATE TRIGGER trg_matricula_actualizacion BEFORE UPDATE ON matricula_asignatura
    FOR EACH ROW EXECUTE FUNCTION marcar_actualizacion();
CREATE TRIGGER trg_categoria_actualizacion BEFORE UPDATE ON categoria_evaluacion
    FOR EACH ROW EXECUTE FUNCTION marcar_actualizacion();
CREATE TRIGGER trg_actividad_actualizacion BEFORE UPDATE ON actividad_evaluativa
    FOR EACH ROW EXECUTE FUNCTION marcar_actualizacion();
CREATE TRIGGER trg_calificacion_actualizacion BEFORE UPDATE ON calificacion
    FOR EACH ROW EXECUTE FUNCTION marcar_actualizacion();
CREATE TRIGGER trg_pendiente_actualizacion BEFORE UPDATE ON pendiente
    FOR EACH ROW EXECUTE FUNCTION marcar_actualizacion();

-- ---- Restricción 1: las categorías de una matrícula suman exactamente 100 % ----
CREATE FUNCTION comprobar_suma_categorias(p_matricula INTEGER) RETURNS void LANGUAGE plpgsql AS $$
DECLARE
    v_filas  INTEGER;
    v_total  NUMERIC;
BEGIN
    SELECT count(*), COALESCE(sum(porcentaje), 0) INTO v_filas, v_total
      FROM categoria_evaluacion WHERE id_matricula = p_matricula;
    IF v_filas > 0 AND v_total <> 100 THEN
        RAISE EXCEPTION 'Los porcentajes de las categorías de la matrícula % suman %, deben sumar 100', p_matricula, v_total
            USING ERRCODE = 'check_violation';
    END IF;
END $$;

CREATE FUNCTION tg_suma_categorias() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP <> 'DELETE' THEN
        PERFORM comprobar_suma_categorias(NEW.id_matricula);
    END IF;
    IF TG_OP = 'DELETE' OR (TG_OP = 'UPDATE' AND OLD.id_matricula <> NEW.id_matricula) THEN
        PERFORM comprobar_suma_categorias(OLD.id_matricula);
    END IF;
    RETURN NULL;
END $$;

CREATE CONSTRAINT TRIGGER trg_suma_categorias
    AFTER INSERT OR UPDATE OR DELETE ON categoria_evaluacion
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION tg_suma_categorias();

-- ---- Restricción 2: las actividades de una categoría suman exactamente 100 % ----
CREATE FUNCTION comprobar_suma_actividades(p_matricula INTEGER, p_categoria INTEGER) RETURNS void LANGUAGE plpgsql AS $$
DECLARE
    v_filas  INTEGER;
    v_total  NUMERIC;
BEGIN
    SELECT count(*), COALESCE(sum(porcentaje), 0) INTO v_filas, v_total
      FROM actividad_evaluativa WHERE id_matricula = p_matricula AND consec_categoria = p_categoria;
    IF v_filas > 0 AND v_total <> 100 THEN
        RAISE EXCEPTION 'Los porcentajes de las actividades de la categoría % de la matrícula % suman %, deben sumar 100',
            p_categoria, p_matricula, v_total USING ERRCODE = 'check_violation';
    END IF;
END $$;

CREATE FUNCTION tg_suma_actividades() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP <> 'DELETE' THEN
        PERFORM comprobar_suma_actividades(NEW.id_matricula, NEW.consec_categoria);
    END IF;
    IF TG_OP = 'DELETE' OR (TG_OP = 'UPDATE' AND (OLD.id_matricula, OLD.consec_categoria) <> (NEW.id_matricula, NEW.consec_categoria)) THEN
        PERFORM comprobar_suma_actividades(OLD.id_matricula, OLD.consec_categoria);
    END IF;
    RETURN NULL;
END $$;

CREATE CONSTRAINT TRIGGER trg_suma_actividades
    AFTER INSERT OR UPDATE OR DELETE ON actividad_evaluativa
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION tg_suma_actividades();

-- ---- Restricción 3: los ítems de una plantilla suman exactamente 100 % ----
CREATE FUNCTION comprobar_suma_items(p_plantilla INTEGER) RETURNS void LANGUAGE plpgsql AS $$
DECLARE
    v_filas  INTEGER;
    v_total  NUMERIC;
BEGIN
    SELECT count(*), COALESCE(sum(porcentaje), 0) INTO v_filas, v_total
      FROM item_de_plantilla WHERE id_plantilla = p_plantilla;
    IF v_filas > 0 AND v_total <> 100 THEN
        RAISE EXCEPTION 'Los porcentajes de los ítems de la plantilla % suman %, deben sumar 100', p_plantilla, v_total
            USING ERRCODE = 'check_violation';
    END IF;
END $$;

CREATE FUNCTION tg_suma_items() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP <> 'DELETE' THEN
        PERFORM comprobar_suma_items(NEW.id_plantilla);
    END IF;
    IF TG_OP = 'DELETE' OR (TG_OP = 'UPDATE' AND OLD.id_plantilla <> NEW.id_plantilla) THEN
        PERFORM comprobar_suma_items(OLD.id_plantilla);
    END IF;
    RETURN NULL;
END $$;

CREATE CONSTRAINT TRIGGER trg_suma_items
    AFTER INSERT OR UPDATE OR DELETE ON item_de_plantilla
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION tg_suma_items();

-- ---- Restricción 5 (parte 2): la cadena de prerrequisitos no admite ciclos ----
-- Se agrega A exige B. Hay ciclo si B exige A, directa o transitivamente.
-- Los correquisitos pueden ser mutuos, por eso solo se revisan los prerrequisitos.
CREATE FUNCTION validar_prerrequisito_sin_ciclos() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    -- La auto-referencia la rechaza el CHECK ck_prerreq_no_a_si_misma
    IF NEW.codigo_asignatura = NEW.codigo_requerida THEN
        RETURN NEW;
    END IF;
    IF NEW.tipo_requisito = 'prerrequisito' AND EXISTS (
        WITH RECURSIVE cadena(codigo) AS (
            SELECT NEW.codigo_requerida
            UNION
            SELECT p.codigo_requerida
              FROM prerrequisito p
              JOIN cadena c ON p.codigo_asignatura = c.codigo
             WHERE p.tipo_requisito = 'prerrequisito'
        )
        SELECT 1 FROM cadena WHERE codigo = NEW.codigo_asignatura
    ) THEN
        RAISE EXCEPTION 'El prerrequisito % de la asignatura % crearía un ciclo', NEW.codigo_requerida, NEW.codigo_asignatura
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_prerrequisito_sin_ciclos BEFORE INSERT OR UPDATE ON prerrequisito
    FOR EACH ROW EXECUTE FUNCTION validar_prerrequisito_sin_ciclos();

-- ---- Restricción 8: sin calificación para una actividad "no entregada" ----
CREATE FUNCTION validar_calificacion_de_actividad_entregada() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM actividad_evaluativa
         WHERE id_matricula = NEW.id_matricula AND consec_categoria = NEW.consec_categoria
           AND consec_actividad = NEW.consec_actividad AND estado_entrega = 'no_entregada'
    ) THEN
        RAISE EXCEPTION 'No se puede calificar una actividad no entregada (matrícula %, categoría %, actividad %)',
            NEW.id_matricula, NEW.consec_categoria, NEW.consec_actividad USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_calificacion_actividad_entregada BEFORE INSERT OR UPDATE ON calificacion
    FOR EACH ROW EXECUTE FUNCTION validar_calificacion_de_actividad_entregada();

CREATE FUNCTION validar_actividad_con_calificacion() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.estado_entrega = 'no_entregada' AND EXISTS (
        SELECT 1 FROM calificacion
         WHERE id_matricula = NEW.id_matricula AND consec_categoria = NEW.consec_categoria
           AND consec_actividad = NEW.consec_actividad
    ) THEN
        RAISE EXCEPTION 'La actividad (matrícula %, categoría %, actividad %) tiene calificación y no puede quedar no entregada',
            NEW.id_matricula, NEW.consec_categoria, NEW.consec_actividad USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_actividad_con_calificacion BEFORE UPDATE OF estado_entrega ON actividad_evaluativa
    FOR EACH ROW EXECUTE FUNCTION validar_actividad_con_calificacion();

-- ---- Restricción 9 (parte 2): toda cuenta conserva al menos un método activo ----
CREATE FUNCTION comprobar_metodo_de_acceso_activo(p_estudiante INTEGER) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM estudiante WHERE id_estudiante = p_estudiante)
       AND NOT EXISTS (SELECT 1 FROM credencial_acceso WHERE id_estudiante = p_estudiante AND activa) THEN
        RAISE EXCEPTION 'La cuenta % debe conservar al menos un método de acceso activo', p_estudiante
            USING ERRCODE = 'check_violation';
    END IF;
END $$;

CREATE FUNCTION tg_metodo_de_acceso_activo() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_TABLE_NAME = 'estudiante' THEN
        PERFORM comprobar_metodo_de_acceso_activo(NEW.id_estudiante);
    ELSE
        IF TG_OP <> 'DELETE' THEN
            PERFORM comprobar_metodo_de_acceso_activo(NEW.id_estudiante);
        END IF;
        IF TG_OP = 'DELETE' OR (TG_OP = 'UPDATE' AND OLD.id_estudiante <> NEW.id_estudiante) THEN
            PERFORM comprobar_metodo_de_acceso_activo(OLD.id_estudiante);
        END IF;
    END IF;
    RETURN NULL;
END $$;

CREATE CONSTRAINT TRIGGER trg_credencial_metodo_activo
    AFTER INSERT OR UPDATE OR DELETE ON credencial_acceso
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION tg_metodo_de_acceso_activo();
CREATE CONSTRAINT TRIGGER trg_estudiante_metodo_activo
    AFTER INSERT ON estudiante
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION tg_metodo_de_acceso_activo();

-- ---- Restricción 11 (parte 2): una sesión revocada no vuelve a ser vigente ni rota su token ----
CREATE FUNCTION validar_sesion_revocada_inmutable() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.fecha_revocacion IS NOT NULL AND (
           NEW.fecha_revocacion    IS DISTINCT FROM OLD.fecha_revocacion
        OR NEW.motivo_revocacion   IS DISTINCT FROM OLD.motivo_revocacion
        OR NEW.hash_token_refresco <> OLD.hash_token_refresco
        OR NEW.fecha_expiracion    <> OLD.fecha_expiracion) THEN
        RAISE EXCEPTION 'La sesión % del estudiante % ya fue revocada y no puede modificarse', OLD.consec_sesion, OLD.id_estudiante
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_sesion_revocada_inmutable BEFORE UPDATE ON sesion
    FOR EACH ROW EXECUTE FUNCTION validar_sesion_revocada_inmutable();

-- ============================================================================
--  10. VISTAS: aquí se calcula lo derivado, no se guarda en columnas
-- ============================================================================

-- Nota de cada categoría de una matrícula, solo con lo ya calificado
--   nota_parcial          promedio ponderado de las actividades calificadas (0.0 a 5.0)
--   porcentaje_evaluado   peso ya calificado dentro de la categoría (0 a 100)
--   aporte_a_nota_final   puntos que la categoría suma a la nota final de la asignatura
CREATE VIEW v_nota_categoria AS
SELECT ce.id_matricula,
       ce.consec_categoria,
       ce.nombre_categoria,
       ce.porcentaje AS porcentaje_categoria,
       COALESCE(sum(a.porcentaje) FILTER (WHERE c.nota_obtenida IS NOT NULL), 0) AS porcentaje_evaluado,
       sum(c.nota_obtenida * a.porcentaje) FILTER (WHERE c.nota_obtenida IS NOT NULL)
         / NULLIF(sum(a.porcentaje) FILTER (WHERE c.nota_obtenida IS NOT NULL), 0) AS nota_parcial,
       COALESCE(sum(c.nota_obtenida * a.porcentaje / 100.0) * ce.porcentaje / 100.0, 0) AS aporte_a_nota_final
  FROM categoria_evaluacion ce
  LEFT JOIN actividad_evaluativa a
         ON a.id_matricula = ce.id_matricula AND a.consec_categoria = ce.consec_categoria
  LEFT JOIN calificacion c
         ON c.id_matricula = a.id_matricula AND c.consec_categoria = a.consec_categoria
        AND c.consec_actividad = a.consec_actividad
 GROUP BY ce.id_matricula, ce.consec_categoria, ce.nombre_categoria, ce.porcentaje;

-- Estado de cada matrícula frente a la meta del estudiante
--   nota_requerida   nota promedio que necesita en lo que falta para llegar a la meta
--                    (0 si ya la alcanzó; sin valor si no queda nada por evaluar)
--   nivel_riesgo     bajo / medio / alto según los umbrales del estudiante;
--                    alto también cuando la meta ya es inalcanzable (más de 5.0)
CREATE VIEW v_estado_asignatura AS
SELECT t.id_matricula,
       t.id_estudiante,
       t.codigo_asignatura,
       t.codigo_periodo,
       t.estado_matricula,
       t.nota_acumulada,
       t.porcentaje_evaluado,
       t.porcentaje_pendiente,
       t.meta_calificacion,
       t.nota_requerida,
       CASE
           WHEN t.porcentaje_pendiente = 0 THEN t.nota_acumulada >= t.meta_calificacion
           ELSE t.nota_requerida <= 5
       END AS meta_alcanzable,
       CASE
           WHEN t.porcentaje_pendiente = 0 THEN
               CASE WHEN t.nota_acumulada >= t.meta_calificacion THEN 'bajo' ELSE 'alto' END
           WHEN t.nota_requerida > 5                THEN 'alto'
           WHEN t.nota_requerida > t.umbral_alto    THEN 'alto'
           WHEN t.nota_requerida > t.umbral_medio   THEN 'medio'
           ELSE 'bajo'
       END AS nivel_riesgo
  FROM (
        SELECT m.id_matricula,
               m.id_estudiante,
               m.codigo_asignatura,
               m.codigo_periodo,
               m.estado_matricula,
               COALESCE(ac.nota_acumulada, 0)        AS nota_acumulada,
               COALESCE(ac.porcentaje_evaluado, 0)   AS porcentaje_evaluado,
               100 - COALESCE(ac.porcentaje_evaluado, 0) AS porcentaje_pendiente,
               COALESCE(cf.meta_calificacion, 3.0)   AS meta_calificacion,
               COALESCE(cf.umbral_riesgo_medio, 3.5) AS umbral_medio,
               COALESCE(cf.umbral_riesgo_alto, 4.5)  AS umbral_alto,
               CASE WHEN 100 - COALESCE(ac.porcentaje_evaluado, 0) > 0 THEN
                    GREATEST(0, (COALESCE(cf.meta_calificacion, 3.0) - COALESCE(ac.nota_acumulada, 0))
                                / ((100 - COALESCE(ac.porcentaje_evaluado, 0)) / 100.0))
               END AS nota_requerida
          FROM matricula_asignatura m
          LEFT JOIN (
                SELECT id_matricula,
                       sum(aporte_a_nota_final) AS nota_acumulada,
                       sum(porcentaje_categoria * porcentaje_evaluado / 100.0) AS porcentaje_evaluado
                  FROM v_nota_categoria
                 GROUP BY id_matricula
               ) ac ON ac.id_matricula = m.id_matricula
          LEFT JOIN configuracion_estudiante cf ON cf.id_estudiante = m.id_estudiante
       ) t;

-- Avance de cada estudiante en su carrera: créditos aprobados sobre el total del programa
CREATE VIEW v_avance_carrera AS
SELECT e.id_estudiante,
       e.codigo_plan,
       pr.total_creditos,
       COALESCE(ap.creditos_aprobados, 0) AS creditos_aprobados,
       round(100.0 * COALESCE(ap.creditos_aprobados, 0) / NULLIF(pr.total_creditos, 0), 2) AS porcentaje_avance
  FROM estudiante e
  LEFT JOIN plan_de_estudios pl ON pl.codigo_plan = e.codigo_plan
  LEFT JOIN programa_academico pr ON pr.codigo_programa = pl.codigo_programa
  LEFT JOIN (
        SELECT x.id_estudiante, sum(x.creditos) AS creditos_aprobados
          FROM (SELECT DISTINCT m.id_estudiante, m.codigo_asignatura, a.creditos
                  FROM matricula_asignatura m
                  JOIN asignatura a ON a.codigo_asignatura = m.codigo_asignatura
                 WHERE m.estado_matricula = 'aprobada') x
         GROUP BY x.id_estudiante
       ) ap ON ap.id_estudiante = e.id_estudiante;

-- Sesiones abiertas de cada estudiante (sin revocar y sin vencer). No expone la huella del token.
CREATE VIEW v_sesion_vigente AS
SELECT s.id_estudiante,
       s.consec_sesion,
       s.consec_dispositivo,
       s.proveedor_origen,
       s.fecha_inicio,
       s.fecha_expiracion,
       s.user_agent,
       s.ip_origen
  FROM sesion s
 WHERE s.fecha_revocacion IS NULL
   AND s.fecha_expiracion > now();
