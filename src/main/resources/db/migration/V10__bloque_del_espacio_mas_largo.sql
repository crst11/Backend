-- SCRUM-81: importar mi horario desde el Reporte de Horario (RF04).
--
-- El Reporte de Horario de la universidad escribe el nombre del bloque del edificio completo, por
-- ejemplo «BLOQUE D- ACADEMICO Y ADMINISTRATIVO» (36 caracteres), y la columna solo admitía 30.
-- Se amplía a 60, el mismo largo de la sede, en vez de recortar el nombre y perder de qué edificio
-- se habla. Ampliar una columna de texto no toca ninguna fila existente.

ALTER TABLE espacio_fisico ALTER COLUMN bloque TYPE VARCHAR(60);
