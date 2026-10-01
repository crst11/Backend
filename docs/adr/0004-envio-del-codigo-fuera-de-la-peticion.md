# ADR 0004 - El código de verificación se envía fuera de la petición

- **Estado:** aceptada
- **Fecha:** 1 de octubre de 2026
- **Incidencia:** SCRUM-67

## Contexto

Al registrarse, la respuesta HTTP esperaba a que el correo saliera. `RegistrarEstudianteServicio` llamaba a `EmisorDeCodigoDeVerificacion`, que conectaba con Gmail, negociaba TLS, se autenticaba y entregaba el mensaje, todo dentro de la petición. A eso se sumaba bcrypt de fuerza 12. El resultado eran varios segundos mirando el formulario, sin saber si la cuenta se había creado.

El requerimiento no funcional de rendimiento pide responder en menos de 3 segundos. Se cumplía por poco, pero la experiencia se sentía lenta, y el documento del proyecto insiste en que la app se sienta ágil.

El envío síncrono tenía una ventaja: si el correo no salía, la API respondía 503 `Correo no enviado` y la pantalla lo explicaba de inmediato.

## Decisión

El envío sale del camino de la petición.

- Se agrega `EnviadorDeCodigoAsincrono`, un **decorador** del puerto `EnviadorDeCodigoPort` marcado `@Primary` y `@Async`. Envuelve al adaptador que esté activo (SMTP o consola) sin modificarlo, y la capa de aplicación no se entera de que hay hilos: sigue dependiendo del mismo puerto.
- El envío corre en un pool propio y acotado (`ejecutorDeEnvioDeCodigo`), para que un servidor de correo lento no consuma los hilos que atienden peticiones. Si la cola se llena, el envío se descarta y queda en el log en vez de romper el registro con un 500.
- `EmisorDeCodigoDeVerificacion` ahora **guarda el código antes de enviarlo**. Antes lo enviaba primero para que un fallo no dejara un código vigente bloqueando la reemisión; con el envío en segundo plano ese orden ya no es posible.
- Para no dejar atascada a la persona cuyo correo no llegó, se puede pedir otro código a los 60 segundos (`CodigoDeVerificacion.ESPERA_PARA_REEMITIR`), sin esperar los 15 minutos de vigencia. Esa espera también evita pedir códigos sin parar.
- Desaparece el 503 `Correo no enviado` en el registro: cuando se responde, todavía no se sabe si el correo saldrá. La excepción y su traducción a 503 se conservan porque el adaptador SMTP las sigue lanzando y el decorador las registra en el log.

## Consecuencias

- El registro responde apenas la cuenta queda guardada, sin esperar al servidor de correo.
- Un fallo de envío ya no se le informa a la persona en el momento. Lo compensa el aviso de la pantalla de verificación y poder pedir otro código al minuto. El equipo se entera por el log.
- Hay un pool de hilos más que vigilar. Es el costo de no bloquear la petición.
- La alternativa era mantener el envío síncrono afinando tiempos de espera y reutilizando la conexión SMTP. Habría quedado en uno o dos segundos: cumple el requerimiento, pero no se siente inmediato, y el correo externo seguiría mandando sobre el tiempo de respuesta de la API.
- Una cola persistente con reintentos sería más robusta, pero exige infraestructura que el proyecto no tiene y que no se justifica para un correo que la persona puede volver a pedir.
