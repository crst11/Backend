-- SCRUM-20: el acceso a Moodle en la guía institucional (RF11).
-- Solo datos: la categoría "Plataformas" y la tabla recurso_institucional ya existen desde V4.
--
-- En la Universidad de Cundinamarca las aulas virtuales no se llaman "Moodle" de cara al
-- estudiante: se llaman Campo Multidimensional de Aprendizaje (CMA), y Moodle es la plataforma
-- sobre la que funcionan. El título lleva los dos nombres para que la búsqueda encuentre el
-- recurso tanto por el nombre oficial como por el que la gente usa en voz alta.
--
-- El enlace va a la página oficial del CMA y no a un subdominio de ingreso (pregrado, cma,
-- institucion), por dos razones: es el mismo criterio de las otras filas de Plataformas, que
-- apuntan a la página informativa y no al login, y desde ahí el estudiante llega al aula que le
-- corresponde aunque la universidad cambie de subdominio. Verificado (respuesta 200) el 3 de
-- octubre de 2026.

INSERT INTO recurso_institucional (id_categoria, titulo, descripcion, url, tipo_recurso, fecha_verificacion, estado_recurso)
SELECT c.id_categoria,
       'Aulas virtuales - Campo Multidimensional de Aprendizaje (Moodle)',
       'Entrar a las aulas virtuales de tus asignaturas, que funcionan sobre Moodle, con sus manuales de uso e insignias digitales.',
       'https://www.ucundinamarca.edu.co/index.php/servicios2022/campo-multidimensional-de-aprendizaje',
       'enlace',
       DATE '2026-10-03',
       'vigente'
  FROM categoria_de_recurso c
 WHERE c.nombre_categoria = 'Plataformas';
