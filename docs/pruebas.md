# Pruebas de API, de carga y de estrés

Además de las pruebas unitarias y de integración de Maven (ver `docs/guia-proyecto.md`, sección 12), el repositorio trae dos pruebas que corren contra el backend **como se ejecuta en producción**: la API completa con newman y la carga con k6. Las dos usan un stack desechable de Docker Compose (`infra/pruebas/compose.yml`) que se levanta y se apaga solo.

## Cómo correrlas

Hace falta Docker. La primera vez compila el backend y baja las imágenes (unos minutos); después reutiliza la caché.

```bash
bash infra/pruebas/ejecutar.sh api          # la colección de Postman completa, con newman
bash infra/pruebas/ejecutar.sh humo         # 1 usuario, 30 s
bash infra/pruebas/ejecutar.sh carga        # 20 usuarios a la vez, unos 5 minutos
bash infra/pruebas/ejecutar.sh estres       # escalones hasta 100 usuarios sin pausa, ~9 minutos
bash infra/pruebas/ejecutar.sh pico         # de 5 a 100 usuarios en 10 segundos
bash infra/pruebas/ejecutar.sh resistencia  # 10 usuarios durante 10 minutos (DURACION=30m para más)
```

Añadir `--conservar` deja el stack encendido al terminar. Los resultados (informe de newman en JUnit, resumen de k6 en JSON y la memoria del backend cada 5 s) quedan en `infra/pruebas/resultados/`, que no va a git.

**Nunca se apuntan a producción ni a preproducción.** El script solo levanta su propio stack. Preproducción y producción comparten la misma instancia: una prueba de estrés contra una degradaría a la otra.

En GitHub:

- `ci.yml` corre `api` y `humo` en cada pull request (job «API completa (newman) y prueba de humo (k6)»).
- `pruebas-de-carga.yml` corre las largas a mano desde Actions → «Pruebas de carga». Conviene lanzarlas al cambiar algo que pueda afectar el rendimiento (una consulta, el pool, la JVM, una dependencia pesada) o antes de una fecha de mucho uso.

El mismo stack lo reutilizan las pruebas end to end del Frontend (repositorio Frontend, carpeta `e2e/`, SCRUM-235), que lo incluyen en su propio Compose y le cambian `CORS_ORIGENES` al origen desde el que abren la app.

## Qué comprueba cada una

| Prueba | Qué hace | Se da por buena si |
|---|---|---|
| `api` | Las 332 peticiones de la colección de Postman, en su orden | 0 aserciones fallidas |
| `humo` | Un usuario recorre una visita completa durante 30 s | Sin errores, p95 < 500 ms |
| `carga` | 20 usuarios a la vez, con pausas de estudiante | Errores < 1 %, p95 < 800 ms, p99 < 1,5 s |
| `estres` | Sube de 20 a 100 usuarios, casi sin pausas (cientos de peticiones por segundo), y luego baja | No se cae (sin OOM ni reinicios), errores < 30 % arriba y **se recupera** (p95 < 800 ms y errores < 1 %) |
| `pico` | De 5 a 100 usuarios en 10 s, un minuto arriba y baja | Igual que estrés |
| `resistencia` | 10 usuarios durante 10 minutos | Errores < 1 %, p95 < 800 ms |

Después de cualquier prueba de k6, `ejecutar.sh` revisa además el contenedor del backend y **falla** si lo mató el kernel por memoria (`OOMKilled`), se reinició o quedó con la salud mal. La memoria máxima y su evolución quedan en `infra/pruebas/resultados/memoria-<prueba>.txt`: en `resistencia` hay que mirar que se estabilice y no crezca sin parar.

Una «visita» de k6 es lo que hace un estudiante: mira la guía pública, su cuenta y su plan, su día y su horario, y crea, marca y borra un pendiente (9 peticiones). Las cuentas se crean de verdad por la API (registro, código leído de Mailpit, verificación, inicio de sesión y programa elegido), no con datos sembrados en la base.

## En qué se parece a producción y en qué no

Igual que producción (`infra/aws/instancia/compose-backend.yml`):

- 448 MiB de memoria, sin swap, y los mismos parámetros de la JVM (`SerialGC`, `MaxRAMPercentage=60`, `-Xss512k`, `MaxMetaspaceSize=128m`, solo el compilador C1 con `TieredStopAtLevel=1` y `ReservedCodeCacheSize=64m`) y 50 hilos de Tomcat. Para probar otros se usan `JAVA_TOOL_OPTIONS_PRUEBAS` y `SERVER_TOMCAT_THREADS_MAX` sin tocar el compose; al terminar, `ejecutar.sh` imprime el pico de memoria **residente** de la JVM y la memoria anónima del cgroup, que es lo que de verdad empuja al kernel a matar al contenedor (`docker stats` mezcla el caché de archivos).
- Pool de conexiones de 5 y la misma imagen (se construye con el mismo `Dockerfile`).
- PostgreSQL 17, como Supabase.
- Cada usuario virtual se presenta con su propia IP (`SALTOS_DE_PROXY=1` y `X-Forwarded-For`), así los límites por IP se miden como los vivirían estudiantes distintos.

Distinto, a propósito:

- El backend migra la base él mismo al arrancar (en producción lo hace un contenedor aparte).
- La sesión del token dura 3 horas, para que un token vencido no se confunda con un fallo del servidor.
- Para `api`, el límite de correos por IP sube a 1000 (la colección entera sale de una sola IP) y la ronda de avisos corre cada 5 s (las carpetas de avisos esperan un aviso de verdad).
- La base de datos vive en memoria y el correo es Mailpit: no hay red de por medio como con Supabase.
- **La CPU es la del equipo donde se corre**, con el contenedor limitado a 1 CPU. La `t4g.small` (Graviton) rinde distinto a un portátil o a un runner de GitHub: los números de tiempo sirven para comparar una versión con otra y para detectar saturación, no como promesa de latencia en producción. Lo que sí es comparable es la memoria, que es el límite más ajustado de la instancia.

## Resultados de referencia

Medidos el 20 de octubre de 2026 en un equipo con Windows y Docker Desktop (amd64), contra el commit de `desarrollo` con SCRUM-233 y SCRUM-79.

| Prueba | Usuarios | Peticiones | Errores | p95 | Máximo | Memoria máxima del backend |
|---|---|---|---|---|---|---|
| `api` | 1 | 331 | 0 de 599 aserciones | — | — | 327 MiB |
| `humo` * | 1 | 66 | 0 | 342 ms | 811 ms | 336 MiB |
| `carga` * | 20 | 6 933 | 0 | 23 ms | 742 ms | 381 MiB |
| `estres` | hasta 100, sin pausa | 42 204 (87/s) | 0 | 1,3 s | 4,6 s | 327 MiB |
| `pico` | 5 → 100 en 10 s | 13 296 (59/s) | 0 | 1,6 s | 4,3 s | **337 MiB** |
| `resistencia` | 10, 10 minutos | 9 075 | 0 | 24 ms | 0,75 s | 334 MiB |

La memoria es el **pico residente de la JVM** (`VmHWM`), salvo en las filas con asterisco, medidas antes del ajuste de memoria (SCRUM-240) con `docker stats`, que incluye el caché de archivos.

Lo que dicen:

- Con una tarde normal (20 estudiantes a la vez) el backend va muy holgado: p95 de 23 ms.
- Sin pausas, a unas 85 peticiones por segundo, el servidor ya se satura (p95 de 1,4 s, con máximos de 5 s), pero **no devuelve un solo error** y, al bajar la carga, vuelve a 20 ms. Degrada, no colapsa.
- La memoria era lo que más se acercaba al límite (448 MiB residentes en el pico, el límite exacto del contenedor) y se ajustó en SCRUM-240: ahora el pico llega a 337 MiB. Cómo se encontró y qué se descartó, más abajo.
- En 10 minutos de carga constante la memoria se queda plana en unos 316 MiB desde la segunda muestra. No hay una fuga.

El requerimiento no funcional del proyecto («menos de 3 segundos con 40 a 80 usuarios concurrentes») se cumple con margen en `carga`; en `estres` y `pico` el p95 se sigue manteniendo bajo ese límite con 100 usuarios sin pausa, que es mucho más exigente que 80 estudiantes reales.

## Ajuste de memoria en el pico (SCRUM-240)

En el pico (de 5 a 100 usuarios en 10 s) la JVM llegaba a **448 MiB residentes, el límite exacto del contenedor**, sin swap: el kernel ya soltaba todo el caché de archivos y solo faltaba un poco más para matarla. El seguimiento de memoria nativa (`-XX:NativeMemoryTracking=summary`) mostró que la JVM estaba sobrecomprometida: lo que no es heap suma unos 230 MiB (metaspace y clases ~120, código compilado ~60, símbolos ~41) y el heap puede crecer hasta 268 MiB (60 % de 448), es decir, en el peor caso quería unos 500 MiB.

Se probó una configuración por corrida de `pico` (mismo stack, mismos 448 MiB; memoria residente máxima de la JVM):

| Configuración | Pico residente | p95 | Máximo | Peticiones |
|---|---|---|---|---|
| Referencia (heap 60 %, C2, 200 hilos) | 448 MiB | 2,6 s | 8,2 s | 11 037 |
| Heap 50 % | 437 MiB | 2,8 s | 8,1 s | 10 443 |
| Heap 45 % | 435 MiB | 2,7 s | 8,5 s | 10 857 |
| Heap 50 % + 50 hilos | 405 MiB | 2,0 s | 5,0 s | 10 812 |
| Heap 50 % + solo C1 y código acotado a 64 MiB | 348 MiB | 2,1 s | 5,5 s | 12 936 |
| **Heap 60 % + solo C1 + código a 64 MiB + 50 hilos** (la adoptada) | **330 MiB** | **1,6 s** | **3,7 s** | **14 061** |

Lo que enseñó:

- **Bajar el heap no sirve**: casi no mueve el pico (el heap no era el cuello de botella) y deja menos margen si varios estudiantes analizan un PDF a la vez. Por eso el techo se queda en 60 %.
- **Los hilos de Tomcat no eran lo que parecía** (el seguimiento nativo los midió en ~1 MiB), pero 50 hilos en lugar de 200 igual ayudan: con un pool de 5 conexiones a la base, más peticiones en vuelo solo hacen fila y cada una retiene memoria.
- **El compilador C2 era lo que más pesaba.** Con 1 CPU compite con las peticiones mientras la JVM se calienta y su código ocupa memoria. Con solo C1 baja ~100 MiB y la latencia y el rendimiento mejoran.
- **No hay regresión en lo que usa mucha CPU**, medido aparte con 25 inicios de sesión (bcrypt) y 10 análisis de un PDF de 20 páginas por configuración: inicio de sesión, mediana 333 ms con C2 y 380 ms con C1 (p95 528 y 424 ms); análisis de PDF, mediana 460 ms con C2 y 190 ms con C1 (p95 1,19 s y 413 ms), porque el PDF también sufre el calentamiento de C2.

Validada con los valores por defecto nuevos: la API completa pasa (599 aserciones, 327 MiB), el estrés (87 peticiones por segundo, p95 1,3 s, 327 MiB) y 10 minutos de resistencia con la memoria plana. En producción corre `infra/aws/instancia/compose-backend.yml`, con los mismos valores.

## Cómo está armada la colección para correr sola

La colección de Postman sigue siendo la misma que se usa a mano. Para correrla de corrido hubo que resolver tres cosas, que se activan solo con la variable `mailpit_url` (la pone `ejecutar.sh`):

- **El código de verificación** (carpeta 2) se lee de Mailpit en lugar de copiarse del correo.
- **Carpetas «Preparar una cuenta con sesión»** antes de las carpetas 5, 8 y 9: la 3 cierra la sesión, la 6 elimina la cuenta y la 8 cierra la sesión propia, y las siguientes necesitan una cuenta activa con sesión. A mano se saltan solas.
- **Un PDF de ejemplo** del Registro Académico Extendido (`docs/postman/ejemplos/registro_academico_extendido_de_ejemplo.pdf`), con los datos ficticios de la prueba del lector. Ningún reporte real entra al repositorio (Ley 1581 de 2012). Si cambia el formato, se regenera con `python3 infra/pruebas/generar-registro-de-ejemplo.py`.

Un endpoint nuevo entra a la colección (petición, errores y pruebas) y, con eso, ya queda cubierto por `api` en el CI. Una advertencia sobre las carpetas con espera (`postman.setNextRequest`): salta **por nombre** y toma la primera petición del archivo con ese nombre, así que el nombre de una petición con bucle tiene que ser único en toda la colección.

## Pruebas de mutación (SCRUM-242)

La cobertura de JaCoCo dice qué líneas **ejecutan** las pruebas, no si **notarían un error**: una prueba que llama al código sin comprobar el resultado cuenta como cobertura. **PIT** hace la pregunta al revés: cambia el código del dominio (el motor académico: notas, promedio, riesgo, horarios, festivos) con errores pequeños —un `>` por `>=`, una suma por una resta, una validación que desaparece, un valor devuelto que se reemplaza por `null`— y corre las pruebas. Si alguna falla, la mutación está **eliminada**; si todas pasan, **sobrevivió**, y eso señala una prueba débil.

```bash
./mvnw -Pmutacion test-compile org.pitest:pitest-maven:mutationCoverage    # unos 2 minutos; el informe queda en target/pit-reports/index.html
```

El perfil `mutacion` de `pom.xml` no corre en la compilación normal. Muta solo `domain` (Java puro, sin Spring ni base de datos) y usa solo las pruebas de `domain`. Se probó sumar las de `application`: tardaba 11 minutos y cubría 3 mutaciones más de 975, así que no se incluyen. Falla si las mutaciones eliminadas bajan del **90 %** (`mutationThreshold`).

En GitHub corre el flujo `pruebas-de-mutacion.yml`: a mano desde Actions cuando se toca el dominio y una vez por semana (lunes). Guarda el informe 30 días. No va en cada pull request porque tarda más que el resto del CI.

**Resultado (21 de octubre de 2026):** 975 mutaciones, **935 eliminadas (95,9 %)**, fuerza de pruebas 99 % (de las mutaciones que alguna prueba alcanza, las eliminadas). La primera medición dio 84 % (818 de 975, con 71 sobrevivientes y 86 sin cobertura de las pruebas del dominio).

### Lo que encontró y se reforzó

- **Los valores límite no se probaban.** Cada texto del dominio tiene el largo de su columna en la base de datos (el nombre de la asignatura, 80; el título del aviso, 120; la dirección Web Push, 500), y las pruebas comprobaban un texto «muy largo», no el de justo el límite: cambiar `>` por `>=` rechazaba un nombre que sí cabe y nadie lo notaba. `LimitesDeTextosTest` y `LimitesDelModeloTest` prueban cada límite por los dos lados (lo último que se acepta y lo primero que se rechaza): los 72 bytes de bcrypt en la contraseña (con acentos, que pesan 2), la nota 0.0 y 5.0, los dos decimales de la meta, el máximo de intentos de un aviso.
- **Validaciones que se podían borrar.** Quitar la llamada a la regla de nombre, de consecutivo o de porcentaje en la plantilla de evaluación, sus ítems o las actividades no hacía fallar nada. Ahora cada una tiene su caso, y el mensaje de «faltan 10 / se pasan 20» se verifica.
- **La Pascua solo se probaba en 2024–2028.** El cálculo de los festivos móviles tiene términos que no cambian en esos años (corrección de siglo, la Pascua más temprana y la más tardía, los cuatro años en que el algoritmo cae una semana tarde). Se agregaron fechas conocidas de 1818 a 2100.
- **Armador de horarios:** los huecos con materias que llegan desordenadas por hora, el puntaje de los días libres cuando se cumple solo una parte, el tope de pasos justo en su límite, que un candidato mejor que llega con la lista de tres ya llena desplace al peor, y los textos de los motivos («45 min», «2 h 30 min», «te quedan libres… no se logró dejar libre…»).
- **Conversiones a la base de datos** (`ConversionesDelModeloTest`): cada estado y tipo va y vuelve, y uno desconocido se rechaza. Una excepción documentada: `EstadoDePendiente` lee lo desconocido como «abierto», a propósito, para no dar por cumplido lo que no lo está.

### Lo que queda sin matar

- **5 mutaciones equivalentes** (cambian el código pero no su comportamiento, así que ninguna prueba puede notarlo): el borde `i < oferta.size()` de `sinSolucion`; el orden de dos bloques con la misma hora de inicio en `huecosDelDia` (no pueden existir: chocarían); `<=` contra `<` al comparar puntajes iguales en `considerar` (el empate se inserta y se vuelve a sacar); el borde del bucle de `GrupoOfertado`; y `comparacion < 0` en `sumanExactamente100`, que ya descartó el caso `== 0` una línea antes.
- **2 pendientes:** un término de la Pascua (línea 85 de `FestivosDeColombia`) y la ventana de 200 días de `AgendaDelDia.siguienteDespuesDe`, que necesita un calendario construido a propósito para caer justo en el borde.
- **33 sin cobertura** de las pruebas de `domain`: casi todos son los registros del módulo de importación (`AsignaturaDetectada`, `ReporteAcademicoDetectado`…), que ejercitan las pruebas de `application`, no incluidas por lo dicho arriba.

PIT solo mide el dominio. La capa de aplicación y los adaptadores se vigilan con las pruebas de integración, la API completa y los límites de ArchUnit; mutarlos pediría arrancar Spring y Testcontainers por cada mutación.
