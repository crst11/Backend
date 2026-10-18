# CundiApp - Backend

API REST de CundiApp, la plataforma centralizada de información académica para los estudiantes de la Universidad de Cundinamarca (programa piloto: Ingeniería de Sistemas, sede Fusagasugá).

Proyecto integrador de Ingeniería de Software I (2026-2).

## Objetivo

Reunir en un solo lugar el horario, las notas, los salones y las fechas del estudiante, y interpretarlos a tiempo: calcular la nota que necesita para alcanzar su meta, clasificar el riesgo de cada asignatura y avisarle antes de que se venza una entrega. El backend expone esa lógica mediante una API REST que consume la aplicación web (repositorio Frontend).

## Estado del proyecto (Review 1)

| Funcionalidad | Estado |
|---|---|
| Esquema de base de datos: 28 tablas, 4 vistas y las 14 restricciones de integridad (Flyway) | Hecho |
| RF01 · Registro con correo institucional | Hecho |
| RF01 · Verificación del correo con código de 6 dígitos | Hecho: el código llega al correo institucional por SMTP (en local, sin SMTP configurado, sale en la consola) |
| RF01 · Iniciar y cerrar sesión con JWT y refresco rotativo | Hecho |
| RF01 · Iniciar sesión con Google (SCRUM-48) | Hecho: se vincula desde Mi cuenta y luego se entra con un toque |
| RF11 · Guía institucional (SCRUM-19) | Hecho: 20 recursos oficiales en 5 categorías, incluidos los accesos a la plataforma institucional, al correo y a las aulas virtuales (Moodle); búsqueda sin tildes con sugerencias, plantillas descargables, vigencia y fecha de verificación |
| RF02 a RF10 y RF12 | Siguientes sprints |

## Tecnologías

- Java 21 y Spring Boot 4 (Web, Validation, Data JPA, Security, OAuth2 Resource Server, Mail)
- API externa: Google Identity Services (validación del ID token con las llaves públicas de Google)
- PostgreSQL 16 con migraciones Flyway (Hibernate solo valida el esquema)
- Arquitectura hexagonal verificada con ArchUnit; cobertura mínima del 80 % en el dominio con JaCoCo
- JUnit 5, Mockito y Testcontainers
- Docker para la base de datos local

## Arquitectura

```
src/main/java/co/edu/ucundinamarca/cundiapp
  domain/            reglas de negocio en Java puro (model, exception)
  application/       casos de uso: port/in (contratos), service (implementación), port/out (lo que necesitan)
  infrastructure/    config, adapter/in/rest (controllers), adapter/out/{persistence,security,clock,notification}
```

Petición típica: `Controller → UseCase → Dominio → puerto → Repository (JPA) → PostgreSQL`. El detalle, con diagramas, está en [docs/review-1/architecture.md](docs/review-1/architecture.md).

## Requisitos previos

- Java 21
- Docker Desktop
- Git

## Levantar en local

```powershell
Copy-Item .env.example .env      # la primera vez; define DB_PASSWORD con una clave local
docker compose up -d --wait      # PostgreSQL 16 en Docker
.\mvnw.cmd spring-boot:run       # Flyway crea el esquema al arrancar
```

La API queda en `http://localhost:8080/api`. `.env` para la base local:

```properties
DB_URL=jdbc:postgresql://localhost:5432/cundiapp
DB_USERNAME=cundiapp
DB_PASSWORD=una_clave_local
COOKIE_SECURE=false
```

### Correo del código de verificación

Sin `CORREO_SMTP_HOST`, en local el código sale en la consola del backend (`[DEV] Código de verificación para ...`). Para recibirlo en el correo institucional, con una cuenta de Gmail del proyecto:

1. En esa cuenta, activar la verificación en dos pasos y crear una *contraseña de aplicación* (Cuenta de Google → Seguridad → Contraseñas de aplicaciones).
2. Agregar al `.env`:
   ```properties
   CORREO_SMTP_HOST=smtp.gmail.com
   CORREO_SMTP_USUARIO=la.cuenta@gmail.com
   CORREO_SMTP_CLAVE=laclavedeaplicacionsinespacios
   ```
3. Reiniciar el backend. En preproducción y producción estas tres variables son obligatorias.

Si el servidor de correo rechaza el envío, la API responde 503 (`Correo no enviado`) y el log dice si fue el usuario o la clave.

### Inicio con Google

Google es la API externa del proyecto. Hace falta un ID de cliente de OAuth (es público; el *client secret* no se usa):

1. En [Google Cloud Console](https://console.cloud.google.com/) crear un proyecto y configurar la pantalla de consentimiento (tipo *Externo*).
2. *Credenciales* → *Crear credenciales* → *ID de cliente de OAuth* → *Aplicación web*. En *Orígenes de JavaScript autorizados* agregar `http://localhost` y `http://localhost:4200` (y la URL del frontend en cada ambiente).
3. Poner el ID en `GOOGLE_CLIENT_ID` del `.env` (backend) y en `googleClientId` de `src/environments` (frontend).

Sin `GOOGLE_CLIENT_ID`, las rutas de Google responden 503 y el resto de la app funciona igual. En preproducción y producción la variable es obligatoria.

`JWT_SECRETO` no hace falta en local (hay un valor de desarrollo); en preproducción y producción es obligatorio. No dejes una línea `JWT_SECRETO=` vacía: anula ese valor y el backend no arranca.

La guía completa de la base de datos (mirar las tablas, llevar el esquema a Supabase y resolver problemas) está en [docs/base-de-datos.md](docs/base-de-datos.md).

### Avisos de vencimiento (Web Push)

La aplicación avisa antes de que venza un pendiente (24 horas antes por defecto). El aviso siempre se ve dentro de la aplicación; para que además llegue **aunque esté cerrada** hace falta Web Push, que usa un par de llaves VAPID:

```bash
node -e "const c=require('crypto');const k=c.createECDH('prime256v1');k.generateKeys();console.log('VAPID_PUBLICA='+k.getPublicKey('base64url'));console.log('VAPID_PRIVADA='+k.getPrivateKey('base64url'))"
```

Se generan **una sola vez** (cambiarlas deja sin avisos a todos los dispositivos ya suscritos) y se ponen en el `.env`, junto con `VAPID_SUJETO` (un correo `mailto:alguien@ejemplo.com` o una dirección `https`). La privada es un secreto: nunca va a Git. En preproducción y producción se entregan como variables de entorno del hosting.

Sin llaves, o con unas inválidas (un `VAPID_SUJETO` sin `mailto:`, llaves que no son pareja P-256), no falla nada: el servidor arranca igual, el envío por Web Push queda apagado, el log dice el motivo (sin imprimir ningún valor) y los avisos se ven solo dentro de la aplicación. El cifrado del mensaje es el del estándar (RFC 8291 y 8292) con lo que trae el JDK, sin bibliotecas adicionales, y solo se aceptan suscripciones de los servicios de avisos de Chrome y Edge, Firefox, Safari y Windows.

Una tarea corre cada 5 minutos (`AVISOS_CADA`) y crea los avisos de lo que está por vencer («Vence pronto») y de lo que se venció en las últimas 48 horas («Se venció», sin importar la prioridad, una sola vez por pendiente), y entrega los pendientes de entrega. Para una demostración, `AVISOS_CADA=PT10S` y `AVISOS_ESPERA_INICIAL=PT5S` la acortan.

## API

| Método y ruta | Uso | Sesión |
|---|---|---|
| `GET /api/publico/guia/categorias` | Categorías de la guía institucional | No |
| `GET /api/publico/guia/recursos?buscar=&categoria=` | Buscar documentos oficiales | No |
| `GET /api/publico/guia/sugerencias` | Temas sugeridos para buscar | No |
| `POST /api/publico/auth/registro` | Crear cuenta | No |
| `POST /api/publico/auth/verificacion` | Verificar el correo con el código | No |
| `POST /api/publico/auth/verificacion/reenvio` | Pedir otro código | No |
| `POST /api/publico/auth/login` | Iniciar sesión | No |
| `POST /api/publico/auth/refresco` | Renovar la sesión (cookie + `X-XSRF-TOKEN`) | Cookie |
| `POST /api/publico/auth/logout` | Cerrar sesión | Cookie |
| `POST /api/publico/auth/google` | Entrar con Google (ID token) | No |
| `GET /api/mis/cuenta` | Datos de mi cuenta | `Bearer` |
| `DELETE /api/mis/cuenta` | Eliminar mi cuenta (queda inactiva, no se borra) | `Bearer` |
| `GET /api/mis/google` | Saber si tengo Google vinculado | `Bearer` |
| `POST /api/mis/google` | Vincular mi cuenta de Google | `Bearer` |
| `DELETE /api/mis/google` | Quitar Google | `Bearer` |
| `GET /api/mis/sesiones` | Mis sesiones abiertas | `Bearer` |
| `DELETE /api/mis/sesiones/{consecutivo}` | Cerrar una sesión | `Bearer` |
| `DELETE /api/mis/sesiones` | Cerrar todas mis sesiones | `Bearer` |
| `GET /api/programas` | Programas académicos disponibles | No |
| `GET /api/programas/{codigo}/plan` | Plan de estudios de un programa | No |
| `GET /api/mis/perfil-academico` | Mi programa y mi plan | `Bearer` |
| `PUT /api/mis/perfil-academico` | Elegir mi programa | `Bearer` |
| `GET /api/mis/historial` | Mis períodos, notas, promedios y avance | `Bearer` |
| `POST /api/mis/importaciones/analisis` | Leer un reporte en PDF sin guardar nada | `Bearer` |
| `POST /api/mis/importaciones` | Confirmar y guardar lo detectado | `Bearer` |
| `GET /api/mis/importaciones` | Mis cargas anteriores | `Bearer` |
| `POST /api/mis/importaciones/{id}/reversion` | Deshacer la última carga | `Bearer` |
| `GET /api/mis/dia` | La pantalla del día: mis clases de hoy (o de la fecha pedida) con la próxima destacada, y el próximo día con clases | `Bearer` |
| `GET /api/mis/avance-de-informacion` | Cuánta información llevo cargada (reportes y actividades de mis asignaturas en curso) y qué me falta | `Bearer` |
| `GET /api/calendario/dias` | Qué clase de día es cada fecha (lectivo, festivo, fuera de período) y la fecha de hoy en Bogotá | `Bearer` |
| `GET /api/mis/matriculas` | Mis asignaturas matriculadas; con `?semestre=actual` solo las que cursa ahora | `Bearer` |
| `GET /api/mis/horario` | Mi horario de la semana, por día; filtros opcionales `asignatura`, `sede` y `orden` | `Bearer` |
| `GET` y `PUT /api/mis/preferencias-de-horario` | Mi vista guardada del horario: vista, sede y orden | `Bearer` |
| `POST /api/mis/importaciones/horario/analisis` | Lee el Reporte de Horario (PDF) y muestra lo detectado, sin guardar | `Bearer` |
| `POST /api/mis/importaciones/horario` | Guarda el horario confirmado: asignaturas en curso y sus clases | `Bearer` |
| `POST /api/mis/matriculas/{id}/horario` | Agregar una clase al horario de una asignatura en curso | `Bearer` |
| `DELETE /api/mis/matriculas/{id}/horario/{n}` | Quitar una clase del horario | `Bearer` |
| `GET /api/mis/matriculas/{id}/evaluacion` | Cómo me evalúan en una asignatura | `Bearer` |
| `PUT /api/mis/matriculas/{id}/evaluacion` | Guardar categorías y actividades | `Bearer` |
| `PUT /api/mis/matriculas/{id}/evaluacion/categorias/{c}/actividades/{a}/calificacion` | Anotar o corregir la nota de una actividad (devuelve el acumulado) | `Bearer` |
| `DELETE /api/mis/matriculas/{id}/evaluacion/categorias/{c}/actividades/{a}/calificacion` | Quitar la nota de una actividad | `Bearer` |
| `GET /api/mis/matriculas/{id}/proyeccion` | La nota que necesito en lo que falta para llegar a mi meta; `?meta=` prueba otra sin guardarla | `Bearer` |
| `POST /api/mis/matriculas/{id}/simulaciones` | Probar notas hipotéticas y ver qué necesito; no guarda nada | `Bearer` |
| `GET` y `PATCH /api/mis/configuracion` | Mi meta de calificación (de 0.0 a 5.0, dos decimales) | `Bearer` |
| `PUT /api/mis/configuracion/umbrales` | Cambiar mis umbrales de riesgo (medio y alto, de 0.0 a 5.0, el medio menor que el alto) | `Bearer` |
| `GET /api/mis/brujula` | La Brújula Académica: el riesgo de cada asignatura que cursa ahora, de lo más urgente a lo menos, con el motivo | `Bearer` |
| `GET /api/mis/avisos` | Mis avisos, del más reciente al más antiguo, con cuántos no he leído; `?soloSinLeer=true` | `Bearer` |
| `PATCH /api/mis/avisos/{id}` | Marcar un aviso como leído o devolverlo a sin leer | `Bearer` |
| `POST /api/mis/avisos/leidos` | Marcar todos mis avisos como leídos | `Bearer` |
| `GET /api/mis/dispositivos/llave-publica` | La llave con la que el navegador se suscribe a los avisos, o que no hay Web Push | `Bearer` |
| `PUT /api/mis/dispositivos/suscripcion` | Activar los avisos en este dispositivo con la suscripción Web Push del navegador | `Bearer` |
| `GET /api/mis/dispositivos` | Los dispositivos que hoy reciben mis avisos (sin su dirección ni sus llaves) | `Bearer` |
| `DELETE /api/mis/dispositivos/{consecutivo}` | Quitar un dispositivo de mis avisos | `Bearer` |
| `GET /api/mis/configuracion/avisos` | Mis preferencias de avisos: general, vencimientos, cambios de riesgo y anticipación | `Bearer` |
| `PUT /api/mis/configuracion/avisos` | Cambiar mis preferencias de avisos (anticipación de 1 a 168 horas) | `Bearer` |

Los errores siguen el formato `application/problem+json` (RFC 9457). La colección de Postman con todas las peticiones, sus casos de error y pruebas automáticas está en [docs/postman](docs/postman/CundiApp.postman_collection.json).

## Pruebas

```powershell
.\mvnw.cmd clean verify
```

Necesita Docker encendido: Testcontainers levanta un PostgreSQL 16 temporal. Ejecuta las pruebas de dominio, casos de uso, adaptadores, API y seguridad, las reglas de arquitectura (ArchUnit) y la cobertura mínima del dominio (JaCoCo).

## Ramas y flujo de trabajo

| Rama | Uso |
|---|---|
| `feature/SCRUM-XX-descripcion` | Trabajo de una incidencia de Jira |
| `desarrollo` | Integración del sprint en curso (rama por defecto) |
| `preproduccion` | Demostración ante el Comité de Arquitectura |
| `produccion` | Versión presentada y aceptada |

El código fluye de la rama de la incidencia a `desarrollo`, luego a `preproduccion` y por último a `produccion`. Los commits siguen el formato `tipo(módulo): descripción SCRUM-XX`. Los cambios de cada versión están en [CHANGELOG.md](CHANGELOG.md).

## Documentación

- [Review 1: arquitectura](docs/review-1/architecture.md), [casos de uso e historias](docs/review-1/use-cases.md) y [guion de la demo](docs/review-1/demo-script.md).
- [Guía del proyecto](docs/guia-proyecto.md): contexto, reglas, arquitectura, ambientes, pruebas y flujo de trabajo.
- [Base de datos, paso a paso](docs/base-de-datos.md) y [modelo de datos](docs/modelo-de-datos.md).
- [Decisiones de arquitectura](docs/adr/) (ADR).

## Autores

- Cristian Albeiro Morales Urrea
- Naomi Xannathl Ochoa Trejo

## Dependencias y avisos de seguridad

El CI tiene un trabajo, *Dependencias sin vulnerabilidades conocidas*, que corre [OSV-Scanner](https://google.github.io/osv-scanner/) sobre `pom.xml` (también las transitivas) y falla con cualquier aviso. Para atender un aviso:

1. Mirar el aviso (el CI muestra el identificador `GHSA-…` y la versión corregida).
2. Subir la dependencia a la versión corregida. Si viene de Spring Boot y todavía no la trae, se fija con una propiedad en `pom.xml` (ejemplo: `tomcat.version`) con un comentario, y se quita cuando Boot la incluya.
3. Si no se puede corregir todavía (no hay versión arreglada o no aplica a cómo usamos la librería), se anota en `osv-scanner.toml` con el motivo y una fecha límite para volver a mirarlo. Sin excepción documentada, el CI no pasa.
4. Probarlo igual que en el CI: `docker run --rm -v "$PWD:/src:ro" ghcr.io/google/osv-scanner:v2.7.0 scan source --config=/src/osv-scanner.toml --lockfile=/src/pom.xml`.
