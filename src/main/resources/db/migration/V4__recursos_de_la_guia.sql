-- SCRUM-19: documentos oficiales de la guía institucional (RF11).
-- Solo datos: la estructura de categoria_de_recurso y recurso_institucional ya existe desde V1.
-- Cada recurso enlaza a su fuente oficial en el portal público de la Universidad de Cundinamarca;
-- CundiApp no guarda copias. Los enlaces se verificaron (respuesta 200) el 26 de septiembre de 2026.

-- Las categorías quedan en el orden en que un estudiante las busca.
UPDATE categoria_de_recurso SET orden = 1 WHERE nombre_categoria = 'Reglamentos';
UPDATE categoria_de_recurso
   SET nombre_categoria = 'Plantillas y formatos',
       descripcion = 'Plantillas institucionales y formatos oficiales para descargar.',
       orden = 3
 WHERE nombre_categoria = 'Formatos';
UPDATE categoria_de_recurso SET orden = 4 WHERE nombre_categoria = 'Convocatorias';

INSERT INTO categoria_de_recurso (nombre_categoria, descripcion, orden) VALUES
    ('Trámites y calendario', 'Fechas del semestre y cómo hacer los trámites académicos.', 2),
    ('Plataformas', 'Accesos oficiales a los servicios en línea de la universidad.', 5);

INSERT INTO recurso_institucional (id_categoria, titulo, descripcion, url, tipo_recurso, fecha_verificacion, estado_recurso)
SELECT c.id_categoria, r.titulo, r.descripcion, r.url, r.tipo, DATE '2026-09-26', 'vigente'
  FROM (VALUES
    -- Reglamentos
    ('Reglamentos', 'Reglamento Estudiantil (versión 4)',
     'Norma que regula la vida académica de los estudiantes de pregrado: derechos, deberes, evaluación y cancelaciones.',
     'https://www.ucundinamarca.edu.co/documents/estudiantes/REGLAMENTO%20ESTUDIANTIL%20V4.pdf', 'documento'),
    ('Reglamentos', 'Estatuto Estudiantil',
     'Principios, derechos, deberes y participación de los estudiantes en la vida universitaria.',
     'https://www.ucundinamarca.edu.co/documents/estudiantes/ESTATUTO-ESTUDIANTIL.pdf', 'documento'),
    ('Reglamentos', 'Estatuto General de la Universidad (Acuerdo 007 de 2015)',
     'Norma que organiza la universidad: gobierno, consejos y estructura institucional.',
     'https://www.ucundinamarca.edu.co/documents/normatividad/acuerdos_superior/acuerdo_007_2015.pdf', 'documento'),
    ('Reglamentos', 'Normatividad institucional (Gaceta)',
     'Acuerdos y resoluciones oficiales de la universidad, organizados por consejo y por año.',
     'https://www.ucundinamarca.edu.co/gaceta/', 'enlace'),
    ('Reglamentos', 'Derechos de los estudiantes',
     'Resumen de los derechos de los estudiantes de la universidad.',
     'https://www.ucundinamarca.edu.co/index.php/noticias-ucundinamarca/180-estudiantes/5021-derechos-de-los-estudiantes', 'enlace'),
    -- Trámites y calendario
    ('Trámites y calendario', 'Calendario académico 2026-2',
     'Fechas del semestre: inscripciones, matrícula y pagos, adiciones y cancelaciones, transferencias y grados.',
     'https://www.ucundinamarca.edu.co/documents/admisiones/2026/CALENDARIO-ACADEMICO-2026-V19-2.pdf', 'documento'),
    ('Trámites y calendario', 'Calendario académico (todas las versiones)',
     'Página oficial del calendario académico con sus versiones y anexos.',
     'https://www.ucundinamarca.edu.co/index.php/calendarioacademico', 'enlace'),
    ('Trámites y calendario', 'Adición y cancelación de materias',
     'Cómo adicionar o cancelar materias, y cómo cancelar el semestre o pedir retiro voluntario (página de Estudiantes).',
     'https://www.ucundinamarca.edu.co/index.php/estudiantes', 'enlace'),
    ('Trámites y calendario', 'Instructivo de inscripción a grados',
     'Paso a paso para inscribirse a grados y los documentos que se deben presentar.',
     'https://www.ucundinamarca.edu.co/documents/admisiones/2026/INSTRUCTIVO-INCRIPCION-GRADOS.pdf', 'formato'),
    ('Trámites y calendario', 'Grados ordinarios 2026-2',
     'Fechas y requisitos de la ceremonia de grados del segundo periodo de 2026.',
     'https://www.ucundinamarca.edu.co/index.php/admisiones-y-registro/5394-grados-ordinarios-2026-2', 'enlace'),
    ('Trámites y calendario', 'Derechos pecuniarios',
     'Valores oficiales de los trámites y servicios que tienen costo, y los acuerdos que los fijan.',
     'https://www.ucundinamarca.edu.co/index.php/derechos-pecuniarios', 'enlace'),
    ('Trámites y calendario', 'Ventanilla virtual de radicación',
     'Radicar solicitudes, peticiones, quejas y reclamos ante la universidad.',
     'https://universidad-cundinamarca.netsaia.com/ws/radicacion_entrada/index.html', 'enlace'),
    -- Plantillas y formatos
    ('Plantillas y formatos', 'Plantilla para presentaciones institucionales (2026)',
     'Plantilla oficial de PowerPoint con la imagen de la universidad para exposiciones y sustentaciones.',
     'https://www.ucundinamarca.edu.co/documents/comunicaciones/PLANTILLA_2026-V2.pptx', 'formato'),
    ('Plantillas y formatos', 'Plantilla de documento en Word',
     'Plantilla oficial de Word del Sistema de Gestión de Calidad para documentos e informes.',
     'https://www.ucundinamarca.edu.co/sgc/documents/plantillas/PLANTILLA-FORMATO-WORD.docx', 'formato'),
    ('Plantillas y formatos', 'Plantilla de hoja de cálculo en Excel',
     'Plantilla oficial de Excel del Sistema de Gestión de Calidad para formatos con tablas.',
     'https://www.ucundinamarca.edu.co/sgc/documents/plantillas/PLANTILLA-FORMATO-EXCEL.xlsx', 'formato'),
    -- Convocatorias
    ('Convocatorias', 'Convocatorias vigentes',
     'Convocatorias institucionales: elecciones de representantes, designaciones y otros procesos, con sus formatos de inscripción.',
     'https://www.ucundinamarca.edu.co/index.php/convocatorias-2022', 'enlace'),
    ('Convocatorias', 'Apoyo financiero',
     'Fraccionamiento de matrícula, acuerdos de pago, créditos con el ICETEX e instructivos para pagar.',
     'https://www.ucundinamarca.edu.co/index.php/servicios/apoyo-financiero', 'enlace'),
    -- Plataformas
    ('Plataformas', 'Plataforma institucional',
     'Consultar tu usuario e ingresar a la plataforma académica de la universidad, con indicaciones para el primer ingreso.',
     'https://www.ucundinamarca.edu.co/index.php/servicios/plataforma-institucional', 'enlace'),
    ('Plataformas', 'Correo institucional',
     'Consultar e ingresar al correo institucional (Office 365) y sus instructivos.',
     'https://www.ucundinamarca.edu.co/index.php/servicios/correo-institucional', 'enlace')
  ) AS r (categoria, titulo, descripcion, url, tipo)
  JOIN categoria_de_recurso c ON c.nombre_categoria = r.categoria;
