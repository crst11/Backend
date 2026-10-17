# Plan de trabajo: funcionalidades que faltan (sprints 4 a 6)

Documento de trabajo, escrito el 16 de octubre de 2026. Parte de lo que dice Jira, de lo que ya existe en el código y de cómo resuelven lo mismo otras aplicaciones, y termina en el orden en que se construye y qué se prueba en cada paso.

## 1. Dónde estamos

| Sprint | Meta | Estado |
|---|---|---|
| 3 (activo, termina el 17 de octubre) | Horario por día y estructura de evaluación con notas | Todo Finalizado |
| 4 | El estudiante ve qué tiene hoy y qué se le vence | RF07 Finalizado; **RF06 (SCRUM-29 y 30) en revisión**: Backend #78 y Frontend #77 |
| 5 | Nota requerida e indicador de riesgo (Brújula Académica) | RF08 (SCRUM-33, 34) y RF09 (SCRUM-35, 36) pendientes |
| 6 | Aviso antes de un vencimiento y consulta sin internet | RF10 (SCRUM-37, 38, 50) y RF12 (SCRUM-39, 40) pendientes |
| 7 | Estabilización, carga, documentación y producción | SCRUM-44, 45, 46, más SCRUM-79 y SCRUM-91 que quedaron sueltas |

**Por qué el tablero «solo muestra Hecho»:** el tablero enseña únicamente el sprint activo, que es el 3, y todo lo del 3 está Finalizado. El trabajo en curso (RF06) está en el sprint 4, que todavía figura como futuro, así que no aparece. Se arregla cerrando el sprint 3 y arrancando el 4; lo hace el equipo porque cerrar un sprint no se puede deshacer.

Lo que ya hay y se reutiliza, para no construir dos veces lo mismo:

- `CalculadoraDeNotas` (dominio) ya calcula los puntos ganados y el porcentaje evaluado de una asignatura. Dice en su documentación que es la base del simulador y de la Brújula.
- `configuracion_estudiante` ya guarda la meta (3.0), los umbrales de riesgo (3.5 y 4.5), la anticipación del aviso (24 h) y si las notificaciones están activas. **No hace falta migración** para RF08 ni RF09.
- `notificacion` y `dispositivo` ya existen para RF10. `simulacion` existe, pero SCRUM-34 pide que lo simulado **no se guarde**, así que no se usa.

## 2. Qué hacen otras aplicaciones y qué decidimos

**Calculadoras de nota final y «qué-pasa-si»** (Pearson, RemNote, Calcuris y similares): resuelven lo mismo, qué nota necesito en lo que falta dado lo que llevo y mi meta, con un promedio ponderado. Si lo que se necesita pasa del máximo, lo dicen y no inventan un número. Algunas normalizan los pesos mientras la estructura está incompleta. Advierten que no cubren reglas del docente (nota que se elimina, supletorios).

Decisiones para CundiApp:

- El cálculo vive en el dominio y la pantalla no calcula nada, como en toda la app. Para que el resultado se vea «al instante» (criterio de SCRUM-34), la pantalla pregunta al servidor con una pausa corta mientras se escribe.
- Nota requerida = (meta − puntos ganados) / peso que falta. Se redondea **hacia arriba** a dos decimales: redondear hacia abajo diría que basta con una nota con la que en realidad no se llega.
- Tres situaciones, explícitas: la meta **ya se alcanzó**, **es alcanzable** (con la nota requerida) o **ya no es alcanzable** (hace falta más de 5.0, o no queda nada por evaluar y no se llegó). También se muestra el máximo que aún se puede sacar.
- Una asignatura sin estructura de evaluación no se simula: responde 422 con un mensaje que lleva a definirla, en vez de mostrar una cuenta sin sentido.

**Planificadores de estudiantes y listas de tareas** (Todoist, My Study Life y similares): muestran la fecha como tiempo relativo («en 3 días»), resaltan lo vencido y permiten ajustar la anticipación de los recordatorios para no saturar. Decisión: lo vencido y lo urgente ya se distinguen con etiqueta escrita (RF06); en RF10 la anticipación y el interruptor de avisos salen de `configuracion_estudiante`, y no se agregan «puntos» ni castigos por atraso.

**Web Push** (web.dev, Vaadin, bibliotecas `webpush-java`): el navegador se suscribe con la llave VAPID pública y el servidor guarda la suscripción por estudiante y dispositivo; el servidor firma y cifra el envío con la llave privada. Los envíos llevan un tiempo de vida (TTL) para que un aviso no llegue cuando ya no sirve. Decisión: `webpush-java`, llaves VAPID por variable de entorno (nunca en Git), TTL igual a lo que falta para el vencimiento, y cada aviso se genera una sola vez.

**Aplicaciones sin conexión (PWA)**: lo de solo lectura que cambia poco se sirve de caché y se refresca en segundo plano; lo propio de cada usuario va primero a la red con un plazo corto y, si falla, se sirve lo guardado **diciendo que está guardado**. Hay que mostrar cuándo se sincronizó. Decisión: RF12 muestra «Última actualización» en cada pantalla que lee del dispositivo y bloquea escribir sin red, que es lo que pide la guía del proyecto (solo lectura).

## 3. Orden de construcción

Cada funcionalidad sigue los seis pasos de la guía: base de datos, dominio, persistencia, API, pantalla y demostración. Un PR de Backend y uno de Frontend por funcionalidad, pequeños, con Postman, CHANGELOG y README al día.

### 3.1 RF08 · Nota que necesito y simulador (SCRUM-33 y 34), sprint 5

**Backend**
1. Sin migración.
2. Dominio: `ProyeccionDeNota` (resultado) y `CalculadoraDeNotaRequerida`, que usa `CalculadoraDeNotas`. Soporta notas hipotéticas que reemplazan a las reales de la misma actividad. Pruebas puras, sin base de datos: sin notas, notas parciales, meta ya alcanzada, meta inalcanzable, nada por evaluar, redondeo hacia arriba, notas simuladas, cortes sin desglosar.
3. Puertos y servicios: `ConsultarMiProyeccionDeNota`, `SimularMiNota`, `ConsultarMiConfiguracion` y `CambiarMiMeta`. La meta y los umbrales viven en `configuracion_estudiante`.
4. API:
   - `GET /api/mis/matriculas/{id}/proyeccion?meta=`: la proyección con la meta guardada, o con la que se mande (sin guardarla).
   - `POST /api/mis/matriculas/{id}/simulaciones`: cuerpo con las notas hipotéticas; devuelve la proyección. No escribe nada.
   - `GET` y `PATCH /api/mis/configuracion`: la meta se puede cambiar (0.0 a 5.0, dos decimales).
5. Pruebas de integración con PostgreSQL (que simular no deje filas, que la meta se guarde y que lo ajeno responda como si no existiera) y carpeta 21 de Postman.

**Frontend**
- Pantalla «Simulador» (`/simulador`, entrada nueva en el menú): elegir asignatura, meta editable, la nota que necesita y el aviso si ya no se alcanza, y una lista de actividades sin nota donde se prueban notas y se ve el resultado. *Mis notas* enlaza a ella.

### 3.2 RF09 · Brújula Académica (SCRUM-35 y 36), sprint 5

- Riesgo de cada asignatura a partir de la nota requerida: **alto** si pasa el umbral alto (4.5) o es inalcanzable, **medio** si pasa el medio (3.5), **bajo** en otro caso, y **sin datos** si no hay estructura. Lo calcula el dominio sobre `ProyeccionDeNota`; los umbrales salen de la configuración (SCRUM-36 los hace editables, con validación de que el medio sea menor que el alto).
- `GET /api/mis/brujula`: una fila por asignatura en curso, ordenada de mayor a menor riesgo, con la frase que lo explica («Necesitas 4.2 en lo que falta»). Nunca se llama «semáforo».
- Pantalla: tarjeta en *Hoy* y lista en `/brujula`; los umbrales se ajustan en la misma pantalla.

### 3.3 RF10 · Motor de notificaciones (SCRUM-37, 38 y 50), sprint 6

- Migración: tabla de suscripciones Web Push por dispositivo (`endpoint`, llaves del navegador, vigencia).
- Generador programado: cada pocos minutos busca pendientes y actividades que vencen dentro de la anticipación del estudiante y crea una `notificacion` por cada uno, **una sola vez**. Con `cambio_riesgo` (SCRUM-50) avisa cuando el nivel de una asignatura cambia al registrar notas.
- Entrega: dentro de la app (campana con las no leídas) y Web Push con `webpush-java`, con reintento limitado y marcando `enviada` o `fallida`. Las suscripciones muertas (410) se borran.
- Configuración (SCRUM-38): interruptor general y anticipación en horas.
- Seguridad: llaves VAPID por entorno, validar que la suscripción sea del estudiante, no mandar el título del pendiente en logs.

### 3.4 RF12 · Consulta sin conexión (SCRUM-39 y 40), sprint 6

- Interceptor que guarda en IndexedDB la última respuesta buena de las lecturas permitidas (horario, estructura y notas, pendientes, día) y la sirve cuando no hay red, marcándola como guardada.
- «Última actualización» visible en cada pantalla (SCRUM-40) y un aviso de «sin conexión: solo lectura»; las acciones que escriben se desactivan sin red.
- Al volver la conexión se vuelve a pedir y se actualiza. Al cerrar sesión se borra lo guardado en el dispositivo.
- `ngsw-config.json`: cascarón en `assetGroups`, y las lecturas en `dataGroups` con estrategia `freshness` y plazo corto.

### 3.5 Sprint 7 y deudas

SCRUM-79 (créditos aprobados contra matriculados), SCRUM-91 (Row-Level Security, necesita acuerdo del equipo), pruebas de carga y seguridad (SCRUM-45), documentación y producción (SCRUM-46).

## 4. Reglas que no cambian

Plan y aprobación antes de cambiar nada; rama `feature/SCRUM-XX` desde `desarrollo`; commits `tipo(módulo): descripción SCRUM-XX`; `./mvnw verify` y `npm run lint && npm test && npm run build` en verde; nadie fusiona a `desarrollo` sin PR revisado; la tarjeta pasa a En desarrollo al crear la rama y a En revisión al abrir el PR.
