-- ============================================================================
--  CundiApp - datos de arranque
--
--  Solo lo que la aplicación necesita para funcionar desde el primer día:
--    * la plantilla de evaluación predeterminada de tres cortes (30 / 30 / 40 %)
--    * las categorías de la guía institucional (RF11)
--  Los datos de demostración para PRE van fuera de las migraciones
--  (src/main/resources/db/demo).
-- ============================================================================

SET search_path TO cundiapp;

INSERT INTO plantilla_evaluacion (nombre_plantilla, descripcion, es_predeterminada)
VALUES ('Tres cortes', 'Tres cortes de 30 %, 30 % y 40 %. Se puede ajustar por asignatura.', TRUE);

INSERT INTO item_de_plantilla (id_plantilla, consec_item, nombre_item, porcentaje)
SELECT p.id_plantilla, i.consec, i.nombre, i.porcentaje
  FROM plantilla_evaluacion p
 CROSS JOIN (VALUES (1, 'Primer corte', 30.00),
                    (2, 'Segundo corte', 30.00),
                    (3, 'Tercer corte', 40.00)) AS i (consec, nombre, porcentaje)
 WHERE p.nombre_plantilla = 'Tres cortes';

INSERT INTO categoria_de_recurso (nombre_categoria, descripcion, orden) VALUES
    ('Reglamentos',   'Reglamento estudiantil y normas de la universidad.', 1),
    ('Formatos',      'Formatos oficiales para solicitudes y trámites.', 2),
    ('Convocatorias', 'Convocatorias abiertas para estudiantes.', 3);
