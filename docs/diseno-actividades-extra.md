# Diseño: actividades extra en el horario

Estado: **propuesto, pendiente de aprobación del estudiante**. Es un requerimiento nuevo: la guía del proyecto dice que no se agregan requerimientos sin acuerdo del equipo, así que se construye como propuesta y el equipo decide si entra al alcance.

## Para qué sirve

Además de sus asignaturas, el estudiante tiene otras cosas de la universidad con horario: una clase de baile, un semillero de investigación, deporte, una monitoría. Hoy no tienen dónde ir, porque «Agregar una clase» solo agrega bloques a una asignatura que ya está cursando. Una **actividad extra** es un compromiso con día y hora que **se ve en Hoy y en Mi horario**, sin ser una asignatura.

## Lo que se acordó con el estudiante

- Se llama **«Actividades extra»** (botón «Añadir actividad extra»), para no confundirse con «Armar mi horario para matricular».
- **No se quita nada** de lo que ya existe: ni «Armar mi horario para matricular» ni la importación del Reporte de Horario.
- El bloque que hoy se llama «Organizar mi horario» (asignatura, sede, ordenar) pasa a llamarse **«Filtrar y ordenar»** y sigue igual.
- Lleva una **guía** corta dentro del formulario, con el mismo estilo de las demás guías.

## Alcance de esta versión

Entra:
- Añadir una actividad extra: nombre, uno o varios días, hora de inicio y de fin, y opcionalmente lugar y responsable.
- Verla en **Hoy**, **igual que las demás clases** (misma tarjeta y mismo estado: pasada, en curso, próxima), sin etiqueta ni botones aparte. En Hoy no se añade ni se quita nada.
- Verla en **Mi horario** (vista por día y por semana) con la etiqueta «Actividad extra», para distinguirla de las asignaturas.
- Añadirla y quitarla **solo desde Mi horario**.
- Respeta el calendario académico igual que las clases: un festivo o un día fuera del período no la muestra.

No entra (a propósito, hasta que haga falta):
- Editarla (se quita y se vuelve a añadir).
- Que **Armar mi horario** la tome como hora ocupada al proponer combinaciones.
- Avisos o recordatorios de la actividad.
- Avisar si se cruza con una clase: las clases tampoco lo hacen hoy.

## Decisiones de diseño

**Una actividad extra no es una asignatura.** Meterla como asignatura falsa la volvería parte de créditos, notas, promedio y Brújula Académica, y ninguna de esas cosas le corresponde. Por eso es su propio concepto.

**Datos.** Tabla nueva `actividad_extra` (migración V18): `id`, `id_estudiante` (FK a estudiante, `ON DELETE CASCADE`), `nombre` (hasta 80), `dia_semana` (mismos valores que `bloque_horario`), `hora_inicio`, `hora_fin` (`hora_inicio < hora_fin`), `lugar` y `responsable` opcionales (hasta 80). **Una fila por día**: añadir «lunes y miércoles» crea dos filas en una sola transacción, y cada una se quita sola. Con **seguridad a nivel de fila**, como las demás tablas del estudiante (V17): solo el dueño la ve.

**Dominio.** Registro `ActividadExtra` con sus reglas (nombre obligatorio, horas al minuto, inicio antes del fin), igual que `BloqueDeHorario`. `HorarioSemanal` y `AgendaDelDia` la reciben junto a las clases, y la hora de «ahora» decide el estado igual que a una clase. Se ordena con las clases por hora.

**Servicios.** `AnadirActividadExtra`, `QuitarActividadExtra`. La lectura entra por los casos de uso que ya existen (`ConsultarMiHorario`, `ConsultarMiDia`), así que **no se agrega otro endpoint de lectura**. Rutas nuevas:
- `POST /api/mis/actividades-extra` (cuerpo con los días: crea una fila por día)
- `DELETE /api/mis/actividades-extra/{id}`

**Límites.** Hasta **20 actividades extra por estudiante** (un abuso se corta antes de llenar la base) y los textos con su largo máximo. Las respuestas de error no dejan averiguar si una actividad existe y es de otro: dicen «no encontramos esa actividad».

**Cómo llega a la pantalla.** Cada elemento de la lista de clases trae `tipo` (`ASIGNATURA` o `EXTRA`). Hoy lo muestra como cualquier clase (el tipo no cambia su aspecto). Mi horario, en cambio, pone la etiqueta «Actividad extra»; una extra no tiene `idMatricula`, no muestra «Ver lo que falta por calificar» y su botón «Quitar» llama a la ruta de actividades extra. Los filtros por asignatura y por sede ocultan las extras: no son de ninguna asignatura ni tienen sede.

**Frontend.** Una sección «Actividades extra» en Mi horario, plegable como «Filtrar y ordenar», con el formulario y la guía. La etiqueta usa la insignia del sistema de diseño y la franja lateral propia; el estado y el tipo van siempre escritos, nunca solo en color.

## La guía dentro del formulario

Pasos con el mismo componente `.steps__*` de las demás guías:

1. **Escribe qué es**: «Baile», «Semillero de IA», «Fútbol».
2. **Elige los días y la hora**: uno o varios días con la misma hora.
3. **Guarda**: aparece en Hoy y en Mi horario, y la quitas cuando termine.

Y una nota: «Una actividad extra no es una asignatura: no tiene notas ni cuenta en tu Brújula Académica».

## Pruebas

Backend: reglas del dominio (nombre vacío, horas invertidas, fuera del largo), caso de uso (límite de 20, varios días en una sola operación), integración con la base (seguridad a nivel de fila: un estudiante no ve ni quita la de otro) y del controlador (códigos de estado). Hoy: una extra cuenta como clase del día y su estado cambia con la hora.

Frontend: específicas de estilos de la sección, de la tarjeta de extra (etiqueta presente, sin botón de notas) y del formulario (guía visible, errores en la propia pantalla). e2e: añadir una actividad, verla en Hoy y en Mi horario, quitarla.

## Colección Postman

Las dos rutas nuevas entran a la colección versionada y se verifican contra el backend real, como todo endpoint nuevo.
