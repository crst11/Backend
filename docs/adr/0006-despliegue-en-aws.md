# ADR 0006 - Despliegue en AWS con ECS Fargate, S3 y CloudFront

- **Estado:** propuesta, pendiente de que el equipo la acepte
- **Fecha:** 9 de octubre de 2026
- **Incidencias:** SCRUM-70 y SCRUM-71
- **Reemplaza en parte a:** [ADR 0005](0005-el-despliegue-se-hace-al-final.md) y cierra la decisión de hosting que dejó abierta el [ADR 0001](0001-ramas-de-ambiente.md)

## Contexto

El ADR 0005 dejó el despliegue para el Sprint 7 porque entonces no había hosting decidido y pagarlo antes de tener funcionalidad
no se justificaba. Esa condición cambió: el hosting está decidido (AWS, con Supabase para la base de datos) y se quiere
adelantar SCRUM-70 y SCRUM-71. El ADR 0005 ya advertía el costo de esperar: los problemas que solo aparecen fuera de la máquina
de desarrollo se descubren tarde. Esto lo confirmó el trabajo mismo: al preparar el despliegue aparecieron dos defectos que
en local no se veían (ver «Consecuencias»).

## Decisión

- **Backend:** imagen de Docker en ECR, ejecutada en **ECS Fargate** detrás de un **Application Load Balancer**.
- **Frontend:** archivos estáticos en **S3** (privado) detrás de **CloudFront**. No se usa la imagen del frontend en ECS: un
  contenedor 24/7 para servir archivos estáticos cuesta más y no aporta nada.
- **Un solo origen.** CloudFront reenvía `/api/*` al balanceador, así que el navegador solo habla con un dominio. La cookie
  de refresco (`SameSite=Strict`) es del mismo sitio y no hay CORS entre frontend y API. Esto resuelve la decisión abierta
  sobre «dominios y cookie de refresco» de la guía sin necesitar un dominio propio.
- **Base de datos:** tres proyectos de Supabase (Dev, Preprod, Prod), por **Session pooler** (5432). Las migraciones las
  gobierna Flyway al arrancar la aplicación; la CLI de Supabase solo crea los proyectos.
- **Ambientes por rama:** `desarrollo` → DEV, `preproduccion` → PREPROD, `produccion` → PROD. Un push (fusión) despliega; un
  pull request no. Producción exige aprobación manual mediante un Environment de GitHub con revisores obligatorios.
- **Sin llaves de acceso.** GitHub Actions asume roles de AWS por OIDC, y cada rol solo lo puede asumir un repositorio concreto
  corriendo en el Environment de su ambiente. Los secretos de la aplicación viven en Parameter Store, por ambiente.
- **Infraestructura como código:** CloudFormation, cuatro plantillas (`infra/aws/`). El CI nunca crea roles.
- **Jira:** cada despliegue comenta en las incidencias; **solo producción las cierra**.

## Consecuencias

**Lo bueno**

- El flujo `feature → desarrollo → preproduccion → produccion` ahora termina en un ambiente real en cada paso.
- Despliegue sin caída (tarea nueva primero, vieja después), con reversión automática si la nueva no queda sana.
- El balanceador solo acepta tráfico de CloudFront, y solo con un secreto compartido.

**Lo que costó, y salió a la luz por intentarlo**

- El healthcheck de Actuator estaba cerrado (`denyAll`): un balanceador habría dado de baja cada tarea nueva. Se abrieron solo
  `liveness` y `readiness`. Se usa `readiness` y no la salud agregada: esa incluye la base de datos, y un parpadeo de Supabase
  tumbaría todas las tareas a la vez.
- Detrás de CloudFront y el balanceador, todos los estudiantes llegaban con la misma IP, la del balanceador. Los límites por IP
  (intentos de inicio de sesión, correos) compartían un solo cupo: uno solo podía bloquear a todos. Medido: el backend
  registraba `172.20.0.4`, la del contenedor nginx. `IpDelCliente` lee `X-Forwarded-For` sin creerle al cliente, y el número de
  proxies de confianza es configuración (`SALTOS_DE_PROXY`).

**Lo que se acepta**

- **Costo real.** ≈ 45–50 USD por ambiente al mes según precios de lista (sin verificar con la calculadora): unos 135–150 USD
  con los tres, y más con dos tareas en producción. Es exactamente lo que el ADR 0005 había querido evitar. Se mitiga apagando
  DEV fuera de horario o dejando DEV en Docker Compose local.
- **Tramo CloudFront → balanceador sin cifrar**, porque no hay dominio ni certificado. Se compensa con la lista de prefijos de
  CloudFront más un encabezado secreto. Con un dominio y un certificado de ACM se cierra.
- **Sin WAF y sin CSP.** Los límites por IP de la aplicación son la única defensa contra abuso por ahora.
- **Una sola cuenta de AWS** con tres ambientes. Aislar producción en otra cuenta es más seguro y más trabajo; los roles
  separados por ambiente y por Environment de GitHub reducen, no eliminan, el riesgo.
- **Despliegue gradual y migraciones.** Durante unos minutos conviven la versión vieja y la nueva con el mismo esquema. Las
  migraciones tienen que ser compatibles hacia atrás: se agrega primero y se quita en un despliegue posterior.
- **Flyway Community no deshace migraciones.** Volver atrás es una migración nueva que revierta, o restaurar un respaldo.
- **Se reconstruye la imagen en cada rama** en vez de promover la misma. Es un trabajo conocido y se deja anotado.

## Cambios a las reglas del equipo que esto implica

El equipo debe decidir estos dos puntos; este ADR no los cambia por sí solo:

1. **Tablero.** La guía dice que una tarjeta llega a Hecho al fusionarse en `desarrollo`, por el ajuste del 1 de octubre. El
   flujo de despliegue hace que **solo producción cierre** la incidencia. Si se adopta, la regla del tablero y la Definición
   de Terminado vuelven a pedir despliegue, tal como preveía la nota de SCRUM-71.
2. **Plan de sprints.** SCRUM-70 y SCRUM-71 salen del Sprint 7. La guía los sitúa allí y habría que moverlos.
