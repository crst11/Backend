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

**Límites** (validados, 422 si se pasan): hasta 10 materias, 20 grupos por materia (12 en la primera versión; SCRUM-243), 8 bloques por grupo; nombres de hasta 80 caracteres y códigos de grupo de hasta 30 (antes 20); no se repite el nombre de una materia ni el código de un grupo dentro de ella.

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

Materias sugeridas desde el plan de estudios; horas ocupadas; guardar la elección y compararla con el horario real al matricular. (El lector de la página de horarios guardada entra en la versión 2, abajo.)

## Versión 2: armar mi horario con la oferta de la universidad (SCRUM-243, 244 y 245)

Estado: **diseño aprobado por el estudiante (21 de octubre de 2026); falta que el equipo lo revise**. Se construye en tres tareas, cada una entregable por sí sola y en este orden.

### Qué cambia para el estudiante

1. **Sube la tabla.** La universidad publica la oferta en «Consultar Horarios» (Proyección Horarios y Cupos del período), pero la página exige sesión: la app no la consulta ni guarda credenciales. El estudiante **sube el archivo guardado** o **pega la tabla**, y el navegador la lee.
2. **La app le dice qué semestre matricular.** Cruza su plan de estudios, su historial y lo que cursa ahora (el horario extendido que ya importó) y sugiere el **semestre a matricular**, con las materias, los **créditos** de cada una y el total. Marca las que debe repetir y las pendientes de semestres anteriores. No tiene que elegir las materias a mano.
3. **Si quiere, adelanta o cambia una materia.** Ve las que puede cursar (con los prerrequisitos cumplidos) y cuántos créditos suma o resta.
4. **Dice cómo quiere el horario.** Franja preferida (mañana, tarde o noche), pocos huecos y días libres, con lo que ya existe. «Todo en la mañana, o en su defecto la mayoría» ya es lo que hace el puntaje de franja: gana la combinación con más minutos de clase en esa jornada y cada propuesta dice el porcentaje.
5. **Recibe hasta 3 propuestas y elige una.** Ese es su horario armado.
6. **Cambia un grupo.** Toca una materia del horario armado y ve cada uno de sus otros grupos como *cabe* o *choca con* (materia, grupo, día y hora). Es una ayuda para decidir: la app no matricula nada.

Si el estudiante no tiene plan o historial importados, el paso 2 no sugiere nada y el estudiante elige su semestre y sus materias a mano.

### Las tres tareas

| Tarea | Qué entrega | Dónde |
|---|---|---|
| **SCRUM-243** | Leer la tabla (archivo o pegada) y armar el horario con esa oferta: semestre y materias elegidos a mano, preferencias y propuestas. | Frontend; el Backend solo sube el límite de grupos por materia de 12 a 20. |
| **SCRUM-244** | Sugerir el semestre a matricular, con créditos y materias adelantables. | Backend (dominio y endpoint) y Frontend (el paso 2). |
| **SCRUM-245** | Cambiar un grupo avisando si cabe o choca. | Backend (dominio y endpoint) y Frontend. |

### Decisiones de diseño

- **El navegador lee la página; el servidor calcula.** Convertir la tabla en materias, grupos y bloques es leer un formato de archivo (igual que el lector del CMAD): va en Angular y no manda el archivo a ningún lado. El choque, el puntaje y la sugerencia de semestre viven en el dominio, como pide la regla 2 de la guía.
- **Sin cambio de esquema y sin guardar la oferta.** Se lee, se usa y se borra con el borrador al cerrar sesión. La oferta es información institucional, sin datos personales.
- **Los créditos salen del plan de estudios**, no de la página: la tabla de la universidad no los trae. Los códigos de materia de la tabla y del plan **coinciden** (58 de las 60 materias del plan de Ingeniería de Sistemas, Fusagasugá).
- **Profundización I y II** llegan en la tabla por línea (`CAD612021836-A` a `-E`) y en el plan como una sola materia (`CAD612021836`). Cada línea es una **opción distinta**, con sus propios grupos, días y horas (por ejemplo `801SISFU-EASW-B` los sábados y `801SISFU-GPS-E` los miércoles por la tarde): no se fusionan ni se descartan. Se asocian al plan por el prefijo y el estudiante elige una línea. Como todas se titulan igual y el servidor no admite dos materias con el mismo nombre, el nombre de cada línea se completa con la sigla que trae el código de sus grupos (`PROFUNDIZACION I (EASW)`); sin sigla legible, con la letra de la línea (`PROFUNDIZACION I (B)`).
- **Los nivelatorios** (`DN-…`) no tienen horario ni créditos y no están en el plan: se omiten.
- **Materias del plan sin grupos en la oferta** se avisan («no se ofrece este período») en lugar de desaparecer.

### Lo que trae la página (observado el 21 de octubre de 2026, Ingeniería de Sistemas y Computación 2020, Fusagasugá)

- Una tabla con 424 filas y 9 semestres hacia abajo. Columnas: semestre, materia (campo de aprendizaje), código, grupo, cupos disponibles, día, hora de inicio, hora de fin y salón.
- La tabla usa `rowspan`: el semestre abarca todas sus materias, la materia (y su código) abarca todos sus grupos, y el grupo (y sus cupos) abarca todos sus bloques. Una fila solo trae las celdas que cambian.
- **La tabla está en el archivo interno** `…_files/pub_rep_val.html`: el `.html` de afuera solo trae el marco vacío. Si el estudiante sube ese archivo, la app le dice cuál subir.
- Las horas vienen como `0700` y `0959`: el fin `0959` es «hasta las 10:00». Se normaliza sumando un minuto al fin, para que dos clases seguidas no choquen.
- El salón puede ser `--` (sin asignar).
- «Grupo sin Horario»: una fila con una celda que ocupa cinco columnas, sin días ni horas; se omite.
- Los nombres de grupo no son uniformes (`SIS.FUSA 101M`, `SIS FUSA 202M`, `&nbsp;SIS.FUSA 201M` con espacio al inicio, `CLC1-M1-FUSA`): se limpian los espacios y la letra de jornada (M, T, N) se muestra pero **no** se usa para decidir; la franja sale de las horas.
- El archivo guardado viene en `windows-1252`: se lee con ese juego de caracteres (se detecta por el `charset` del archivo).

### SCRUM-243: leer la tabla y armar con esa oferta

| Capa | Pieza | Qué es |
|---|---|---|
| Frontend | `LectorDeOfertaDeHorarios` | Función pura: HTML (o .mht ya decodificado) a oferta por semestre. Sigue `rowspan`, normaliza horas y nombres y omite los grupos sin horario. Reutiliza `mhtml.ts` del lector del CMAD. |
| Frontend | Pantalla `/horario/armar` | Paso nuevo «Subir la oferta» antes de «Mis materias»: archivo o pegar, semestre y materias (marcadas por semestre, que el estudiante puede ajustar). El paso de escribir a mano sigue existiendo. |
| Backend | Límites | Grupos por materia: de 12 a **20**, porque «Ciudadanía del Siglo 21» trae 13 en una sola sede. Código de grupo: de 20 a **30** caracteres, porque Profundización II trae códigos de hasta 24 (`901SISFU-REDESCONVER-C`). Los demás límites no cambian (10 materias, 8 bloques por grupo). Hecho. |

### SCRUM-244: sugerir el semestre a matricular

`GET /api/mis/horario/sugerencia-de-matricula` (con sesión; solo lee; no guarda nada y no cambia el esquema). Servicio de dominio `SugeridorDeMatricula`, Java puro, con el plan, el historial y las materias en curso.

- **Semestre actual:** el semestre sugerido del plan que más créditos en curso concentra (si hay empate, el menor).
- **Semestre sugerido:** el siguiente al actual. **Sin materias en curso** (el estudiante está entre períodos) no hay semestre actual y se sugiere el **primer semestre del plan con materias sin aprobar**, no el siguiente.
- **Materias sugeridas:** las del semestre sugerido más las **pendientes** (de semestres anteriores, sin aprobar y sin cursar ahora) y las **por repetir** (reprobadas).
- **Prerrequisitos:** se consideran cumplidos si están aprobados **o en curso** (se supone que se aprobarán); la materia se marca «depende de una que cursas ahora» para que el estudiante lo sepa.
- **Adelantables:** materias de semestres posteriores al sugerido con los prerrequisitos cumplidos, con sus créditos.
- **Créditos:** suma de los de las materias que el estudiante deja marcadas. Si la universidad fija un tope por semestre, se agrega como configuración (variable de entorno) y se avisa al pasarlo; hoy no se asume ninguno.

Respuesta 200: `semestreSugerido`, `materias` (código, nombre, créditos, motivo), `creditos`, `adelantables`. Sin plan o sin historial: `semestreSugerido` nulo y un `motivo`, y la pantalla cae al flujo manual. Error 401 sin sesión.

### SCRUM-245: cambiar un grupo avisando si choca

`POST /api/mis/horario/verificaciones` (con sesión, no guarda nada): recibe el horario armado (una elección por materia, con sus bloques) y la materia que se quiere cambiar con sus grupos candidatos; responde, por cada candidato, si **cabe** o con qué **choca**. Reutiliza la regla de choque de `BloqueDeOferta` (terminar a la hora en que empieza otro no es choque) en un servicio de dominio nuevo, `VerificadorDeCambioDeGrupo`. Se prefirió a llamar al armador una vez por grupo (hasta 13 llamadas por materia). Errores con `ProblemDetail` (400, 422, 401).

En el Frontend: `VerificacionDeCambioRepository` (puerto y adaptador HTTP en `data-access`; el proyecto no usa versiones en memoria para estos repositorios) y «Cambiar grupo» en la propuesta elegida.

**El docente no viene en la tabla.** La oferta de la universidad trae el grupo, el cupo, los días, las horas y el salón, pero no el docente. La app no puede decir quién dicta cada grupo; lo que hace es dejar que la persona pruebe el grupo del docente que ella conoce (por ejemplo, el 501 en lugar del 503, que tienen el mismo horario) y decirle si cabe o con qué choca. Si algún día la universidad publica el docente en la tabla, se agrega a la oferta sin cambiar este endpoint.

### Qué no entra

Leer la página directamente de la plataforma, guardar la oferta o la elección en el servidor, matricular por el estudiante, otras sedes con un formato distinto (se prueba con Fusagasugá) y un tope de créditos fijo.

### Pruebas

- **Lector (TDD):** la tabla real recortada (con `rowspan`, un grupo sin horario, un grupo de dos bloques, el fin `0959`, un nombre con espacio inicial y un salón `--`), tablas vacías o de otro formato, el archivo de afuera (sin tabla), página en `windows-1252` y archivo `.mht`.
- **Sugeridor (TDD, sin Spring):** semestre actual y sugerido, repetidas, pendientes, prerrequisito en curso, adelantables, sin materias en curso y sin plan.
- **Verificador (TDD, sin Spring):** cabe y choca, choque al tocarse que no lo es, varios candidatos y choque con más de una materia a la vez.
- **API con la base real:** 200, 400, 422 y 401 de cada endpoint. Carpeta nueva en Postman, ejecutada contra el backend real.
- **Pantalla:** subir, semestre y materias, proponer, adelantar una materia con sus créditos, cambiar un grupo (cabe y choca) y errores. Verificación en el navegador, y la prueba de accesibilidad existente cubre la pantalla.

### Requisitos transversales (se verifican, no son opcionales)

**Seguridad, sin huecos**
- **El archivo que sube el estudiante es entrada no confiable.** Se lee con `DOMParser` en un documento inerte: nunca se inserta su HTML en la página (`innerHTML`, `bypassSecurityTrust…`), no se ejecutan sus scripts ni se cargan sus recursos. Solo se toman los textos de las celdas de la tabla y se muestran como texto.
- **Tope de tamaño** del archivo y de lo pegado (por ejemplo, 2 MB) y de filas y de longitud de cada celda; si se pasa, se rechaza con un mensaje claro. Una tabla mal formada nunca rompe la pantalla.
- **Los límites se validan otra vez en el servidor** (materias, grupos, bloques, largo de nombres y códigos, horas válidas); la validación del navegador es comodidad, no seguridad. Errores con `ProblemDetail` sin detalles internos.
- **Todo endpoint nuevo exige sesión** y solo lee datos **del estudiante autenticado**: nunca recibe un identificador de estudiante en la petición, así que no se puede consultar a otro (sin IDOR).
- **Sin consultas dinámicas** nuevas: la sugerencia usa los puertos de lectura que ya existen; la verificación no toca la base de datos.
- **Sin cambios en la política de seguridad** (CSP) ni orígenes nuevos: la app no llama a la universidad.

**SOLID**
- **Responsabilidad única:** `LectorDeOfertaDeHorarios` solo lee; `SugeridorDeMatricula` solo sugiere; `VerificadorDeCambioDeGrupo` solo verifica. Ninguno hace de controlador ni de acceso a datos.
- **Abierto/cerrado e inversión de dependencias:** casos de uso detrás de puertos de entrada (uno por caso de uso), controladores que dependen solo de puertos, adaptadores que no se llaman entre sí. Las reglas de ArchUnit existentes (SCRUM-238) vigilan esto en cada compilación y no se debilitan.
- **Segregación de interfaces:** puertos pequeños; el repositorio del Frontend expone solo la operación que usa su pantalla.
- **Sustitución:** el repositorio HTTP y el de memoria del Frontend son intercambiables y se prueban con el mismo contrato.

**Los 12 factores**
- **Configuración en el entorno:** si se agrega un tope de créditos, va en una variable de entorno con valor por defecto documentado; nada fijo en el código ni URLs ni secretos.
- **Procesos sin estado:** ningún endpoint guarda nada en memoria ni en disco; cualquier instancia responde igual. La oferta y el borrador viven en el navegador.
- **Registros:** solo eventos (qué endpoint y cuánto tardó), con el id de petición. **Nunca** se registran la oferta, el plan, el historial ni los códigos de materia del estudiante.
- **Paridad dev/prod y procesos de administración:** sin pasos manuales nuevos, sin migraciones y sin variables nuevas obligatorias.

**Sin fugas**
- **De datos:** la oferta no sale del navegador; las respuestas llevan solo lo que la pantalla muestra; los mensajes de error no revelan estructura interna; el borrador local se borra al cerrar sesión (como el actual).
- **De recursos:** el lector libera lo que abre (`FileReader`, URLs de objeto) y no deja suscripciones ni temporizadores vivos al salir de la pantalla; el cálculo del Backend tiene un tope de pasos como el armador.
- **De secretos:** el archivo de ejemplo de pruebas es la oferta pública recortada, sin datos personales, y no se commitea nada de Claude ni credenciales.

## Versión 2.1: «quiero un día libre» (SCRUM-251)

Estado: **diseño aprobado por Cristian el 22 de octubre de 2026; Backend implementado (SCRUM-251), la pantalla va en el Frontend**.

### Qué se vio al probar

Con el criterio actual, la persona elige *cuáles* días quiere libres. El puntaje de ese criterio es «de los días que pediste, cuántos quedan sin clase». Dos cosas salen mal:

1. **Si ninguna combinación deja libre el día elegido, todas valen cero y gana la primera que encuentra la búsqueda.** En la práctica queda libre «el día más cercano» y el resto se amontona (el lunes, por ejemplo). La persona no sabe qué días sí se podían liberar.
2. **Nada castiga cargar un solo día.** Una semana con seis horas el lunes y nada el martes puntúa igual que una repartida.

### Qué cambia para el estudiante

En lugar de escoger el día libre, la persona marca **«Quiero tener un día libre»**. La app le muestra **todas las opciones que dejan algún día libre, sin importar cuáles ni cuántos** (una por cada conjunto distinto de días libres, hasta ocho), cada una diciendo **qué días quedan libres**, y las ordena de la mejor repartida a la peor. Si con esas materias no se puede liberar ningún día, lo dice y propone quitar la marca.

### Decisiones

| Decisión | Qué se hace | Por qué |
|---|---|---|
| Nuevo criterio | `CriterioDeAcomodo` gana `diaLibre` (booleano). Con `diaLibre` activo se ignora `diasLibres` (el campo sigue aceptándose para no romper clientes ni Postman, y la pantalla deja de ofrecerlo). | Cambiar el significado de un campo que ya existe es un cambio rompiente; uno nuevo no. |
| Qué es «un día que se puede liberar» | Un día de la **oferta** (alguna materia elegida tiene un grupo ese día). Los días que ninguna materia usa no cuentan como opciones: ya están libres para cualquiera y no son el resultado de escoger bien. | Si no, «sábado libre» sería una opción para todos y no diría nada. |
| Cómo se halla | La misma búsqueda por retroceso, una sola pasada: por cada combinación se calcula qué días de la oferta quedan sin clase y, para cada uno de esos días, se guarda la mejor combinación vista. Al final se quitan las repetidas (una combinación que libera dos días es una sola opción con los dos días). | No hay que repetir la búsqueda seis veces ni cambiar el tope de pasos. |
| Equilibrio | Con `diaLibre` activo el puntaje suma un criterio nuevo, **equilibrio** (peso 30, repartido entre los criterios pedidos como los demás): los minutos totales de clase repartidos por igual entre los días de la oferta menos uno (se deja libre exactamente un día), divididos entre los minutos del día más cargado, con tope en 1. Todo en un solo día de cuatro vale 1/3; repartido en tres días vale 1. Una combinación que libera varios días puntúa menos que una repartida que libera uno solo, pero **igual se muestra** (SCRUM-253: la persona pidió ver todas las opciones que dejan algún día libre, sin importar cuáles ni cuántos). Franja y huecos siguen valiendo si se pidieron. | Responde lo que se vio: dos opciones que liberan el mismo día no se ordenan por azar, gana la que no amontona. |
| Fuera de `diaLibre` | Todo igual: mismas tres propuestas, mismo puntaje, mismo orden. | No se toca lo que ya funciona ni sus pruebas. |
| Resultado | Hasta **ocho** propuestas en este modo (una por cada conjunto distinto de días libres) en vez de tres. `Resumen` gana `diasLiberados`: los días de la oferta que quedan sin clase en esa combinación. Los motivos lo dicen («Deja libre el viernes») y nombran el día más cargado y sus horas. | La pantalla solo muestra lo que el servidor ya calculó. |
| Sin opciones | Si ningún día se puede liberar: cero propuestas y un mensaje («Con estas materias no se puede dejar ningún día libre…»). Si la búsqueda se cortó por el tope antes de hallar uno, el mensaje lo dice y no afirma que sea imposible. | Es la verdad y la persona decide qué hacer. |
| Esquema y seguridad | Sin cambios de esquema. Mismos límites (10 materias, 20 grupos, 8 bloques) y misma validación; el cálculo sigue sin guardar nada. | — |

### API

`criterio` acepta `diaLibre` (booleano, opcional, falso por omisión). La respuesta de cada propuesta trae `resumen.diasLiberados` (lista de días sin tilde, como `diasLibres`). Sin otros cambios.

### Pantalla

Se quita el bloque «Días libres» (seis botones) y se agrega un botón de alternar «Quiero tener un día libre», con `aria-pressed`, igual que «Pocos huecos». Cuando está activo, cada opción muestra «Días libres: viernes» (de `diasLiberados`) y el panel «Cambiar grupo» (SCRUM-245) sigue funcionando igual. El borrador guarda `diaLibre` en lugar de `diasLibres`; un borrador viejo sin el campo se lee como «no».

### Pruebas

- Dominio (TDD, primero en rojo): una opción por día liberable; una combinación que libera dos días es una sola opción con los dos; un día que todos los grupos usan no tiene opción; los días fuera de la oferta no son opciones; **la repartida le gana a la amontonada para el mismo día libre** (el caso del lunes); sin día liberable, cero propuestas y mensaje; sin `diaLibre` los resultados no cambian (las pruebas existentes).
- API con base real: el campo nuevo, el valor por omisión y que `diasLibres` se ignora si `diaLibre` está activo.
- Postman: carpeta nueva 29, ejecutada contra el backend real.
- Pantalla: el botón, que manda `diaLibre`, que muestra los días liberados y el mensaje sin opciones; e2e contra el servidor real con una oferta donde un día sí se libera y otro no.
