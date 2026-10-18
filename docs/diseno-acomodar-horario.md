# Diseño: acomodar mi horario para matricular

Estado: **aprobado por el estudiante; el equipo decide si entra al alcance**. Es un requerimiento nuevo (RF13): la guía del proyecto dice que no se agregan requerimientos sin acuerdo del equipo, así que se construye como propuesta y el equipo decide si entra al alcance.

## Para qué sirve

Cuando la universidad publica los horarios y se abre el registro académico, el estudiante tiene que elegir **un grupo por cada materia** que quiere cursar, y los grupos de materias distintas pueden chocar. Hoy lo resuelve a mano. La función le propone **combinaciones sin choques** y las ordena según lo que prefiere: por ejemplo, que la mayoría de sus clases queden en la mañana.

## Lo que se acordó con el estudiante

- La universidad no publica la oferta en una página que la app pueda leer sin sesión, y la app no extrae datos de la plataforma institucional. Por eso, en esta versión **el estudiante escribe los grupos y horarios que ve** al matricular. Un lector de la página guardada queda para después, cuando exista un archivo de ejemplo.
- Es una **ayuda para decidir**: la app no matricula nada ni se conecta a la plataforma.

## Alcance de esta versión

Entra:
- Escribir materias, sus grupos y los bloques de cada grupo (día, hora de inicio y de fin).
- Preferencias: franja preferida (mañana, tarde, noche o ninguna), pocos huecos entre clases y días que quiere libres.
- Proponer hasta **3 combinaciones** sin choques, ordenadas, cada una con su puntaje y el motivo en palabras.
- Si no hay ninguna combinación, decir **por qué**: qué materias chocan en todos sus grupos.

No entra (a propósito, hasta que haga falta):
- Leer la página de la universidad (falta un archivo de ejemplo).
- Sugerir materias desde el plan de estudios y el historial (RF02).
- Horas ocupadas por trabajo u otros compromisos.
- Guardar el horario elegido en el servidor: la propuesta se calcula al momento y **no se guarda nada**.

## Decisiones de diseño

**Sin cambio de esquema.** Todo viaja en la petición y el cálculo no guarda nada, así que no hay migraciones ni tablas nuevas. Lo calculado no se guarda (regla 10 de la guía).

**El cálculo vive en el dominio** (regla 2: nada de lógica de negocio en controladores ni en Angular). Es Java puro, se prueba sin base de datos ni Spring, igual que la Brújula.

**Reutiliza `DiaDeLaSemana`.** No reutiliza `BloqueDeHorario`: ese lleva consecutivo, espacio físico y docente que aquí no existen; se crea un bloque propio más pequeño.

### Dominio (`domain`)

| Pieza | Qué es |
|---|---|
| `BloqueDeOferta` | día, hora de inicio, hora de fin (inicio antes que fin; al minuto). Dos bloques chocan si son del mismo día y se solapan; **terminar a la misma hora a la que empieza el otro no es choque**. |
| `GrupoOfertado` | código del grupo y sus bloques (al menos uno). |
| `AsignaturaOfertada` | nombre y sus grupos (al menos uno). |
| `CriterioDeAcomodo` | franja, pocos huecos y días libres. (`PreferenciaDeHorario` ya existe en RF04 con otro significado: cómo ver el horario.) |
| `PropuestaDeHorario` | una elección de grupo por materia, con su puntaje, sus números y sus motivos. |
| `ArmadorDeHorario` | el servicio: recibe la oferta y el criterio y devuelve las mejores propuestas o la explicación de por qué no hay. |

### Cómo se elige (algoritmo)

Búsqueda por retroceso: se elige un grupo por materia, en orden, y se descarta una rama en cuanto un grupo choca con los ya elegidos. Con los límites de abajo termina en milisegundos; además tiene un tope de 200 000 pasos, y si lo alcanza devuelve lo mejor encontrado y lo dice.

**Puntaje (0 a 100).** Cada criterio que el estudiante pidió suma, y los que no pidió no cuentan:
- *Franja*: el porcentaje de minutos de clase que cae en la franja preferida (mañana: antes de las 12:00; tarde: de 12:00 a 18:00; noche: desde las 18:00). Peso 60.
- *Pocos huecos*: menos minutos libres entre la primera y la última clase de cada día, sumados en la semana. Se mide contra un tope fijo: sin huecos vale el máximo y con 10 horas libres (600 minutos) vale cero. Un tope fijo, y no «el peor caso entre las combinaciones», permite puntuar cada combinación al encontrarla sin guardarlas todas. Peso 25.
- *Días libres*: la fracción de los días pedidos que quedan sin clase. Peso 15.
- Si el estudiante no pide nada, todas valen igual y se muestran en el orden en que las escribió.
- Con puntajes iguales manda el orden de entrada, para que el resultado sea estable.

Los pesos se reparten entre los criterios pedidos para que el máximo siga siendo 100.

**Sin solución.** Si ninguna combinación funciona, se buscan las parejas de materias en las que **todos** sus grupos chocan entre sí y se devuelven en palabras («Cálculo y Física: todos sus grupos chocan»).

**Límites** (validados, 422 si se pasan): hasta 10 materias, 12 grupos por materia, 8 bloques por grupo; nombres de hasta 80 caracteres y códigos de hasta 20; no se repite el nombre de una materia ni el código de un grupo dentro de ella.

### API (`infrastructure/adapter/in/rest`)

`POST /api/mis/horario/propuestas` (con sesión, como todo `/api/mis`). No guarda nada.

Petición:
```json
{
  "asignaturas": [
    { "nombre": "Cálculo", "grupos": [
      { "codigo": "1", "bloques": [ { "dia": "lunes", "inicio": "08:00", "fin": "10:00" } ] }
    ] }
  ],
  "criterio": { "franja": "manana", "pocosHuecos": true, "diasLibres": ["viernes"] }
}
```

Respuesta 200: `propuestas` (0 a 3), cada una con `puntaje`, `elecciones` (materia y grupo), `resumen` (minutos en cada franja, minutos de huecos, días con clase, días libres) y `motivos` (frases). Si no hay ninguna: `propuestas` vacío y `choques` con las parejas que no se pueden juntar. `busquedaIncompleta` indica si se llegó al tope.

Errores con `ProblemDetail`: 400 si falta un campo; 422 si una hora no es válida, un grupo no tiene bloques, se repite un nombre o se pasan los límites; 401 sin sesión.

### Pantalla Angular (`features/schedule/plan-schedule`)

Ruta `/horario/armar`, con enlace desde «Mi horario» y desde el menú. En una sola pantalla, móvil primero:
1. **Mis materias**: agregar una materia, y dentro de ella sus grupos y los bloques (día, de, hasta).
2. **Mi preferencia**: franja, pocos huecos y días libres, con chips.
3. **Proponer**: muestra hasta 3 propuestas; cada una con el puntaje, los grupos a matricular por materia, el horario por día y los motivos. Si no hay, muestra los choques.

El borrador se guarda en el navegador por cuenta y se borra al cerrar sesión, para que recargar no obligue a escribir todo otra vez. La pantalla **no calcula nada**: manda la oferta y muestra lo que devuelve el servidor.

## Pruebas

- Dominio (TDD, sin Spring): choque y no choque al tocarse, una materia con un solo grupo, preferencia de mañana, pocos huecos, días libres, desempate estable, sin solución con la explicación, límites y el tope de búsqueda.
- API: 200, 400, 422 y 401, con la base real.
- Colección de Postman (carpeta 27) ejecutada contra el backend real.
- Frontend: repositorio y pantalla (agregar y quitar materias, grupos y bloques; validaciones; proponer; mostrar choques; error del servidor).
- Verificación en el navegador de la pantalla.

## Qué sigue después (no se construye ahora)

Lector de la página de horarios guardada; materias sugeridas desde el plan de estudios; horas ocupadas; guardar la elección y compararla con el horario real al matricular.
