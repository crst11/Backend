# ADR 0007: Protección contra bots en el registro, el inicio de sesión y la recuperación

- **Estado:** propuesta, implementada y apagada por defecto (SCRUM-92). Se activa cuando el equipo crea las llaves y acepta lo de la Ley 1581.
- **Fecha:** 14 de octubre de 2026

## Contexto

El registro, el inicio de sesión y la recuperación de contraseña son públicos y no tienen ninguna prueba de que quien los usa es una persona. Ya hay defensas por detrás: límite de correos por IP (SCRUM-69), bloqueo del inicio de sesión por fallos (cinco por correo, veinte por IP), el cupo de análisis de PDF por estudiante y el límite general del borde (SCRUM-88). Ninguna impide que un guion **cree cuentas en masa** o pruebe contraseñas repartido entre muchas IP. Esa capa es la que falta.

Restricciones propias de este proyecto: los usuarios son estudiantes en el teléfono, con redes de la universidad (muchas personas detrás de la misma IP), y los datos personales están bajo la Ley 1581 de 2012: hay que decir qué dato sale a un tercero.

## Opciones

| Opción | A favor | En contra |
|---|---|---|
| **Cloudflare Turnstile** (prueba invisible o de un clic) | Gratuita sin límite razonable, sin rompecabezas (accesible), no pide cuenta de Google, se verifica con una llamada HTTP | Es un tercero más; sin las llaves no sirve |
| reCAPTCHA v3 de Google | Muy difundida, invisible | Manda más señales a Google, que ya recibe datos con "Iniciar con Google"; la puntuación pide ajustar umbrales |
| hCaptcha | Privacidad como argumento | Suele mostrar rompecabezas: peor en el teléfono y para quien usa lector de pantalla |
| Campo trampa y retraso (honeypot) | Sin tercero, sin datos que salgan | Detiene bots torpes y nada más; un guion dirigido lo salta |

## Decisión

**Cloudflare Turnstile**, en modo administrado (invisible casi siempre). El frontend carga el widget **solo si hay llave pública configurada** y manda el resultado en el encabezado `X-Verificacion-Humana`; el backend lo verifica contra Cloudflare **antes** de procesar el registro, el inicio de sesión y la recuperación (las tres rutas, y solo `POST`). La llave secreta va por la variable `TURNSTILE_SECRETO`; la pública no es un secreto (como el `client ID` de Google) y va en los entornos del Frontend.

- **Apagado por defecto:** sin `TURNSTILE_SECRETO` el filtro no hace nada y el frontend no carga el widget. Nada cambia hasta que se activen.
- **Si Cloudflare no responde, depende de la ruta** (SCRUM-233). En el **inicio de sesión se deja pasar y se anota en el log** (falla abierta): bloquear ahí a todos cuando Cloudflare cae convierte un tercero en un punto único de falla de la entrada a la app, y quien inicia sesión ya tiene cuenta y está protegido por los límites por IP y por correo. En el **registro y la recuperación se responde 503** (falla cerrada): son las dos rutas que crean cuentas y mandan correos, y un robot querría aprovecharlas justo cuando nadie lo verifica; el costo es que durante una caída de Cloudflare no se pueden crear cuentas ni pedir la recuperación, que se resuelve reintentando en unos minutos. Un rechazo explícito de Cloudflare siempre bloquea (422).
- **No se guarda nada** de la verificación: ni el resultado ni el identificador.

## Qué sale a un tercero (Ley 1581)

- Al **cargar el widget**, el navegador del estudiante le habla a `challenges.cloudflare.com`: su IP y datos del navegador.
- Al **verificar**, el backend le manda a Cloudflare el resultado del widget y la IP del estudiante.
- No se manda el correo, la contraseña ni ningún dato de la cuenta.

**Pendiente antes de activarlo:** nombrar a Cloudflare como encargado en el aviso de tratamiento de datos y en el texto del consentimiento del registro. Esto lo decide el equipo, no el código.

## Consecuencias

- Una variable secreta nueva (`TURNSTILE_SECRETO`) y una llave pública en los entornos del Frontend.
- La Content-Security-Policy del Frontend (SCRUM-93) agrega `https://challenges.cloudflare.com` a `script-src`, `frame-src` y `connect-src`.
- Si más adelante hace falta algo más fuerte para un ataque dirigido, se puede sumar un retraso o un límite por cuenta sin cambiar este diseño.
