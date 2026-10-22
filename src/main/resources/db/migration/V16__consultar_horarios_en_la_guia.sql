-- SCRUM-248: la página pública «Consultar Horarios» de la universidad en la guía institucional (RF11).
-- Solo datos: la categoría "Plataformas" y la tabla recurso_institucional ya existen desde V4.
--
-- Es la página donde se consigue la oferta de horarios que se sube en «Armar mi horario»: poner el
-- enlace en la guía evita que cada estudiante tenga que buscarla. El enlace es el de la página
-- pública (no pide cuenta); la tabla de grupos aparece al elegir sede y programa. Es el destino
-- final del enlace corto que usa el equipo, sin el acortador de por medio.
-- Verificado (respuesta 200) el 21 de octubre de 2026.

INSERT INTO recurso_institucional (id_categoria, titulo, descripcion, url, tipo_recurso, fecha_verificacion, estado_recurso)
SELECT c.id_categoria,
       'Consultar horarios (oferta de grupos por programa)',
       'Mira los grupos y horarios que ofrece la universidad: elige tu sede y tu programa, guarda la página y súbela en Armar mi horario.',
       'https://plataforma.ucundinamarca.edu.co/aplicacionesB/condicionales/apl_gen_public.jsp?id=ConsultaHorario',
       'enlace',
       DATE '2026-10-21',
       'vigente'
  FROM categoria_de_recurso c
 WHERE c.nombre_categoria = 'Plataformas';
