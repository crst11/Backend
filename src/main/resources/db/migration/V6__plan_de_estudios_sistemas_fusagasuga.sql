-- SCRUM-21: ruta de aprendizaje de Ingeniería de Sistemas y Computación, sede Fusagasugá (RF02).
--
-- Fuente: "Consultar Ruta de Aprendizaje" de Academusoft (programa INGENIERIA DE SISTEMAS Y
-- COMPUTACION 2020 - FUSAGASUGÁ, jornada MIXTA, 9 períodos), que es el sistema oficial de la
-- universidad. Los códigos, los créditos y los requisitos salen de allí tal cual.
--
-- Comprobación: los créditos por período suman 16+18+17+16+17+18+17+18+16 = 153, que es el total que
-- publica la universidad en la página del programa. Si una carga futura no da 153, está mal.
--
-- La "Ponderación Académica" de Academusoft es lo que aquí se llama créditos. Las asignaturas de
-- diagnóstico y nivelatorio (prefijo DN-) valen 0 créditos: se cursan pero no ponderan.
--
-- Facatativá comparte esta misma ruta y Chía tiene otra. Por ahora solo se carga Fusagasugá, que es
-- la sede del programa piloto.

INSERT INTO cundiapp.programa_academico
    (codigo_programa, nombre_programa, facultad, sede, total_creditos, numero_periodos) VALUES
    ('109964', 'Ingeniería de Sistemas y Computación', 'Ingeniería', 'Fusagasugá', 153, 9);

INSERT INTO cundiapp.plan_de_estudios
    (codigo_plan, codigo_programa, version, anio_vigencia, estado_plan) VALUES
    ('ISC-2020-FUSA', '109964', '2020', 2020, 'vigente');

INSERT INTO cundiapp.asignatura
    (codigo_asignatura, codigo_plan, nombre_asignatura, creditos, tipo_asignatura, periodo_sugerido) VALUES
    -- Período 1 (16 créditos)
    ('CAD612021101',     'ISC-2020-FUSA', 'Álgebra Lineal',                                               3, 'obligatoria',    1),
    ('DN-CAI1002020202', 'ISC-2020-FUSA', 'Diagnóstico y Nivelatorio Comunicación y Lectura Crítica I',   0, 'obligatoria',    1),
    ('DN-CAI1002020201', 'ISC-2020-FUSA', 'Diagnóstico y Nivelatorio Razonamiento Lógico y Cuantitativo', 0, 'obligatoria',    1),
    ('CAD612021106',     'ISC-2020-FUSA', 'Fundamentos de Electrónica',                                   4, 'obligatoria',    1),
    ('CAD612021103',     'ISC-2020-FUSA', 'Fundamentos de Ingeniería',                                    2, 'obligatoria',    1),
    ('CAD612021105',     'ISC-2020-FUSA', 'Matemáticas Discretas',                                        2, 'obligatoria',    1),
    ('CAD612021102',     'ISC-2020-FUSA', 'Pensamiento Algorítmico',                                      3, 'obligatoria',    1),
    ('CAD612021104',     'ISC-2020-FUSA', 'Pensamiento Sistémico y Automatización',                       2, 'obligatoria',    1),
    -- Período 2 (18 créditos)
    ('CAD612021207',     'ISC-2020-FUSA', 'Cálculo Diferencial',                                          4, 'obligatoria',    2),
    ('CAI1002020202',    'ISC-2020-FUSA', 'Comunicación y Lectura Crítica I',                             2, 'obligatoria',    2),
    ('DN-CAI1002020303', 'ISC-2020-FUSA', 'Diagnóstico y Nivelatorio Ciudadanía del Siglo 21',            0, 'obligatoria',    2),
    ('DN-CAI1002020304', 'ISC-2020-FUSA', 'Diagnóstico y Nivelatorio Lengua Extranjera I',                0, 'obligatoria',    2),
    ('CAD612021210',     'ISC-2020-FUSA', 'Estadística, Probabilidad e Inferencia',                       3, 'obligatoria',    2),
    ('CAD612021208',     'ISC-2020-FUSA', 'Física I',                                                     4, 'obligatoria',    2),
    ('CAD612021209',     'ISC-2020-FUSA', 'Programación I',                                               3, 'obligatoria',    2),
    ('CAI1002020201',    'ISC-2020-FUSA', 'Razonamiento Lógico y Cuantitativo',                           2, 'obligatoria',    2),
    -- Período 3 (17 créditos)
    ('CAD612021311',     'ISC-2020-FUSA', 'Cálculo Integral',                                             4, 'obligatoria',    3),
    ('CAI1002020303',    'ISC-2020-FUSA', 'Ciudadanía del Siglo 21',                                      2, 'obligatoria',    3),
    ('CAI1002020305',    'ISC-2020-FUSA', 'Comunicación y Lectura Crítica II',                            2, 'obligatoria',    3),
    ('CAD612021312',     'ISC-2020-FUSA', 'Física II',                                                    4, 'obligatoria',    3),
    ('CAI1002020304',    'ISC-2020-FUSA', 'Lengua Extranjera I',                                          2, 'obligatoria',    3),
    ('CAD612021313',     'ISC-2020-FUSA', 'Programación II',                                              3, 'obligatoria',    3),
    -- Período 4 (16 créditos)
    ('CAD612021417',     'ISC-2020-FUSA', 'Arquitectura de Computadores',                                 3, 'obligatoria',    4),
    ('CAD612021414',     'ISC-2020-FUSA', 'Cálculo Multivariado',                                         4, 'obligatoria',    4),
    ('CAD612021416',     'ISC-2020-FUSA', 'Estructuras de Información',                                   2, 'obligatoria',    4),
    ('CAD612021415',     'ISC-2020-FUSA', 'Física III',                                                   3, 'obligatoria',    4),
    ('CAD612021418',     'ISC-2020-FUSA', 'Fundamentos Administrativos',                                  2, 'obligatoria',    4),
    ('CAI1002020406',    'ISC-2020-FUSA', 'Lengua Extranjera II',                                         2, 'obligatoria',    4),
    -- Período 5 (17 créditos)
    ('CAD612021523',     'ISC-2020-FUSA', 'Base de Datos',                                                2, 'obligatoria',    5),
    ('DN-CAI1002020613', 'ISC-2020-FUSA', 'Diagnóstico y Nivelatorio Ciencia, Tecnología e Innovación I', 0, 'obligatoria',    5),
    ('DN-CAI1002020612', 'ISC-2020-FUSA', 'Diagnóstico y Nivelatorio Emprendimiento e Innovación I',      0, 'obligatoria',    5),
    ('CAD612021519',     'ISC-2020-FUSA', 'Ecuaciones Diferenciales',                                     4, 'obligatoria',    5),
    ('CAD612021521',     'ISC-2020-FUSA', 'Ingeniería de Software I',                                     3, 'obligatoria',    5),
    ('CAI1002020507',    'ISC-2020-FUSA', 'Lengua Extranjera III',                                        2, 'obligatoria',    5),
    ('CAD612021524',     'ISC-2020-FUSA', 'Planeación Estratégica',                                       2, 'obligatoria',    5),
    ('CAD612021522',     'ISC-2020-FUSA', 'Sistemas de Información',                                      2, 'obligatoria',    5),
    ('CAD612021520',     'ISC-2020-FUSA', 'Sistemas Operativos',                                          2, 'obligatoria',    5),
    -- Período 6 (18 créditos)
    ('CAI1002020609',    'ISC-2020-FUSA', 'Cátedra Generación Siglo 21',                                  1, 'obligatoria',    6),
    ('CAI1002020613',    'ISC-2020-FUSA', 'Ciencia, Tecnología e Innovación I',                           2, 'obligatoria',    6),
    ('CAD612021626',     'ISC-2020-FUSA', 'Comunicación de Datos',                                        3, 'obligatoria',    6),
    ('CAI1002020612',    'ISC-2020-FUSA', 'Emprendimiento e Innovación I',                                2, 'obligatoria',    6),
    ('CAD612021627',     'ISC-2020-FUSA', 'Ingeniería de Software II',                                    3, 'obligatoria',    6),
    ('CAI1002020608',    'ISC-2020-FUSA', 'Lengua Extranjera IV',                                         2, 'obligatoria',    6),
    ('CAD612021625',     'ISC-2020-FUSA', 'Matemáticas Especiales',                                       3, 'obligatoria',    6),
    ('CAD612021628',     'ISC-2020-FUSA', 'Networking',                                                   2, 'obligatoria',    6),
    -- Período 7 (17 créditos)
    ('CAD612021729',     'ISC-2020-FUSA', 'Análisis Numérico',                                            3, 'obligatoria',    7),
    ('CAD612021732',     'ISC-2020-FUSA', 'Ciencia de Datos',                                             2, 'obligatoria',    7),
    ('CAI1002020715',    'ISC-2020-FUSA', 'Ciencia, Tecnología e Innovación II',                          2, 'obligatoria',    7),
    ('CAI1002020714',    'ISC-2020-FUSA', 'Emprendimiento e Innovación II',                               2, 'obligatoria',    7),
    ('CAD612021731',     'ISC-2020-FUSA', 'Operativa',                                                    3, 'obligatoria',    7),
    ('CAD612021733',     'ISC-2020-FUSA', 'Redes y Comunicación',                                         3, 'obligatoria',    7),
    ('CAD612021730',     'ISC-2020-FUSA', 'Seguridad Informática',                                        2, 'obligatoria',    7),
    -- Período 8 (18 créditos)
    ('CAI1002020816',    'ISC-2020-FUSA', 'Ciencia, Tecnología e Innovación III',                         2, 'obligatoria',    8),
    ('CAD612021834',     'ISC-2020-FUSA', 'Inteligencia Artificial',                                      3, 'obligatoria',    8),
    ('CAD612021835',     'ISC-2020-FUSA', 'Lenguajes y Autómatas',                                        3, 'obligatoria',    8),
    ('CAD612021836',     'ISC-2020-FUSA', 'Profundización I',                                            10, 'profundizacion', 8),
    -- Período 9 (16 créditos)
    ('CAD612021937',     'ISC-2020-FUSA', 'Gerencia de Proyectos',                                        2, 'obligatoria',    9),
    ('CAD612021938',     'ISC-2020-FUSA', 'Modelación',                                                   3, 'obligatoria',    9),
    ('CAD612021940',     'ISC-2020-FUSA', 'Opción de Grado',                                              1, 'obligatoria',    9),
    ('CAD612021939',     'ISC-2020-FUSA', 'Profundización II',                                           10, 'profundizacion', 9);

-- Requisitos, de la columna "Requisitos" de Academusoft. Allí todos aparecen marcados con "R", que su
-- propia leyenda define como requisito; por eso aquí entran todos como prerrequisito. Si alguna vez
-- aparece una "C" (correquisito), el esquema ya la admite.
INSERT INTO cundiapp.prerrequisito (codigo_asignatura, codigo_requerida, tipo_requisito) VALUES
    -- Período 2
    ('CAI1002020202',    'DN-CAI1002020202', 'prerrequisito'),
    ('CAD612021209',     'CAD612021102',     'prerrequisito'),
    ('CAI1002020201',    'DN-CAI1002020201', 'prerrequisito'),
    -- Período 3
    ('CAD612021311',     'CAD612021207',     'prerrequisito'),
    ('CAI1002020303',    'DN-CAI1002020303', 'prerrequisito'),
    ('CAI1002020305',    'CAI1002020202',    'prerrequisito'),
    ('CAD612021312',     'CAD612021208',     'prerrequisito'),
    ('CAI1002020304',    'DN-CAI1002020304', 'prerrequisito'),
    ('CAD612021313',     'CAD612021209',     'prerrequisito'),
    -- Período 4
    ('CAD612021417',     'CAD612021106',     'prerrequisito'),
    ('CAD612021414',     'CAD612021311',     'prerrequisito'),
    ('CAD612021416',     'CAD612021209',     'prerrequisito'),
    ('CAD612021415',     'CAD612021312',     'prerrequisito'),
    ('CAI1002020406',    'CAI1002020304',    'prerrequisito'),
    -- Período 5
    ('CAD612021523',     'CAD612021313',     'prerrequisito'),
    ('DN-CAI1002020613', 'CAI1002020201',    'prerrequisito'),
    ('DN-CAI1002020613', 'CAI1002020304',    'prerrequisito'),
    ('DN-CAI1002020613', 'CAI1002020303',    'prerrequisito'),
    ('DN-CAI1002020613', 'CAI1002020202',    'prerrequisito'),
    ('DN-CAI1002020612', 'CAI1002020201',    'prerrequisito'),
    ('DN-CAI1002020612', 'CAI1002020304',    'prerrequisito'),
    ('DN-CAI1002020612', 'CAI1002020303',    'prerrequisito'),
    ('DN-CAI1002020612', 'CAI1002020202',    'prerrequisito'),
    ('CAD612021519',     'CAD612021311',     'prerrequisito'),
    ('CAD612021521',     'CAD612021416',     'prerrequisito'),
    ('CAD612021521',     'CAD612021313',     'prerrequisito'),
    ('CAI1002020507',    'CAI1002020406',    'prerrequisito'),
    ('CAD612021522',     'CAD612021313',     'prerrequisito'),
    ('CAD612021520',     'CAD612021416',     'prerrequisito'),
    ('CAD612021520',     'CAD612021313',     'prerrequisito'),
    -- Período 6
    ('CAI1002020609',    'CAD612021519',     'prerrequisito'),
    ('CAI1002020609',    'CAD612021520',     'prerrequisito'),
    ('CAI1002020609',    'CAD612021521',     'prerrequisito'),
    ('CAI1002020609',    'CAD612021522',     'prerrequisito'),
    ('CAI1002020609',    'DN-CAI1002020613', 'prerrequisito'),
    ('CAI1002020609',    'CAD612021524',     'prerrequisito'),
    ('CAI1002020609',    'CAI1002020507',    'prerrequisito'),
    ('CAI1002020609',    'DN-CAI1002020612', 'prerrequisito'),
    ('CAI1002020609',    'CAD612021523',     'prerrequisito'),
    ('CAI1002020613',    'DN-CAI1002020613', 'prerrequisito'),
    ('CAD612021626',     'CAD612021520',     'prerrequisito'),
    ('CAI1002020612',    'DN-CAI1002020612', 'prerrequisito'),
    ('CAD612021627',     'CAD612021521',     'prerrequisito'),
    ('CAI1002020608',    'CAI1002020507',    'prerrequisito'),
    ('CAD612021625',     'CAD612021519',     'prerrequisito'),
    ('CAD612021628',     'CAD612021522',     'prerrequisito'),
    -- Período 7
    ('CAD612021729',     'CAD612021625',     'prerrequisito'),
    ('CAD612021732',     'CAD612021522',     'prerrequisito'),
    ('CAI1002020715',    'CAI1002020613',    'prerrequisito'),
    ('CAI1002020714',    'CAI1002020612',    'prerrequisito'),
    ('CAD612021731',     'CAD612021625',     'prerrequisito'),
    ('CAD612021730',     'CAD612021628',     'prerrequisito'),
    -- Período 8
    ('CAI1002020816',    'CAI1002020715',    'prerrequisito'),
    ('CAD612021834',     'CAD612021733',     'prerrequisito'),
    ('CAD612021835',     'CAD612021733',     'prerrequisito'),
    ('CAD612021836',     'CAD612021733',     'prerrequisito'),
    ('CAD612021836',     'CAD612021628',     'prerrequisito'),
    -- Período 9
    ('CAD612021937',     'CAD612021524',     'prerrequisito'),
    ('CAD612021938',     'CAD612021731',     'prerrequisito'),
    ('CAD612021940',     'CAI1002020816',    'prerrequisito'),
    ('CAD612021939',     'CAD612021628',     'prerrequisito'),
    ('CAD612021939',     'CAD612021836',     'prerrequisito'),
    ('CAD612021939',     'CAD612021733',     'prerrequisito');
