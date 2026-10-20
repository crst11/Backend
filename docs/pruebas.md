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

- 448 MiB de memoria, sin swap, y los mismos parámetros de la JVM (`SerialGC`, `MaxRAMPercentage=60`, `-Xss512k`, `MaxMetaspaceSize=128m`).
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
| `api` | 1 | 332 | 0 de 600 aserciones | — | — | 362 MiB |
| `humo` | 1 | 66 | 0 | 342 ms | 811 ms | 336 MiB |
| `carga` | 20 | 6 933 | 0 | 23 ms | 742 ms | 381 MiB |
| `estres` | hasta 100, sin pausa | 38 424 (84/s) | 0 | 1,4 s | 5,1 s | 402 MiB |
| `pico` | 5 → 100 en 10 s | 11 037 (52/s) | 0 | 2,5 s | 7,6 s | **428 MiB** |
| `resistencia` | 10, 10 minutos | 9 156 | 0 | 23 ms | 1,17 s | 379 MiB |

Lo que dicen:

- Con una tarde normal (20 estudiantes a la vez) el backend va muy holgado: p95 de 23 ms.
- Sin pausas, a unas 85 peticiones por segundo, el servidor ya se satura (p95 de 1,4 s, con máximos de 5 s), pero **no devuelve un solo error** y, al bajar la carga, vuelve a 20 ms. Degrada, no colapsa.
- La memoria es lo que más se acerca al límite. En el pico llegó a 428 de 448 MiB (95 %) y se mantuvo, pero es poco margen: un pico más largo o más ancho en la instancia real, con menos CPU, podría pasarse y la JVM saldría por `ExitOnOutOfMemoryError`. Si esto preocupa, el primer ajuste a probar es limitar los hilos de Tomcat (`server.tomcat.threads.max`) para que un pico no abra cientos de hilos de 512 KiB; se mide con `pico` antes y después.
- En 10 minutos de carga constante la memoria sube de 320 a 372 MiB durante el calentamiento y se queda ahí. No hay una fuga.

El requerimiento no funcional del proyecto («menos de 3 segundos con 40 a 80 usuarios concurrentes») se cumple con margen en `carga`; en `estres` y `pico` el p95 se sigue manteniendo bajo ese límite con 100 usuarios sin pausa, que es mucho más exigente que 80 estudiantes reales.

## Cómo está armada la colección para correr sola

La colección de Postman sigue siendo la misma que se usa a mano. Para correrla de corrido hubo que resolver tres cosas, que se activan solo con la variable `mailpit_url` (la pone `ejecutar.sh`):

- **El código de verificación** (carpeta 2) se lee de Mailpit en lugar de copiarse del correo.
- **Carpetas «Preparar una cuenta con sesión»** antes de las carpetas 5, 8 y 9: la 3 cierra la sesión, la 6 elimina la cuenta y la 8 cierra la sesión propia, y las siguientes necesitan una cuenta activa con sesión. A mano se saltan solas.
- **Un PDF de ejemplo** del Registro Académico Extendido (`docs/postman/ejemplos/registro_academico_extendido_de_ejemplo.pdf`), con los datos ficticios de la prueba del lector. Ningún reporte real entra al repositorio (Ley 1581 de 2012). Si cambia el formato, se regenera con `python3 infra/pruebas/generar-registro-de-ejemplo.py`.

Un endpoint nuevo entra a la colección (petición, errores y pruebas) y, con eso, ya queda cubierto por `api` en el CI. Una advertencia sobre las carpetas con espera (`postman.setNextRequest`): salta **por nombre** y toma la primera petición del archivo con ese nombre, así que el nombre de una petición con bucle tiene que ser único en toda la colección.
