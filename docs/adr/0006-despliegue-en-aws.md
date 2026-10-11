# ADR 0006 - Despliegue en AWS sin costo: una instancia EC2, S3 y CloudFront

- **Estado:** propuesta, pendiente de que el equipo la acepte
- **Fecha:** 9 de octubre de 2026
- **Incidencias:** SCRUM-70 y SCRUM-71
- **Reemplaza en parte a:** [ADR 0005](0005-el-despliegue-se-hace-al-final.md) y cierra la decisión de hosting que dejó abierta el [ADR 0001](0001-ramas-de-ambiente.md)

## Contexto

El ADR 0005 dejó el despliegue para el Sprint 7 porque no había hosting y pagarlo antes de tener funcionalidad no se
justificaba. El hosting ya está decidido (AWS, con Supabase para la base de datos) y se quiere adelantar SCRUM-70 y SCRUM-71.

La primera versión de este ADR usaba ECS Fargate detrás de un Application Load Balancer: ≈ 45–50 USD por ambiente al mes
según precios de lista. El equipo fijó una **regla de costo cero**: todo dentro del nivel gratuito de AWS, sin balanceador de
carga ni tareas Fargate continuas, con la base de datos en el plan gratuito de Supabase. Esta versión cumple esa regla.

## Decisión

- **Backend:** imagen de Docker en ECR, ejecutada con Docker Compose en **una sola instancia EC2**. Aloja los dos ambientes
  en la nube: producción en el puerto 80 y preproducción en el 81, cada uno con su contenedor, su base de datos y sus secretos.
- **Una nginx de entrada** (contenedor `borde`) hace el trabajo que haría el balanceador: comprueba un secreto compartido con
  CloudFront (`X-Origin-Verify`, distinto por ambiente) y reenvía al backend de ese ambiente.
- **Frontend:** archivos estáticos en **S3** privado detrás de **CloudFront**. `/api/*` se reenvía a la instancia.
- **Un solo origen.** El navegador solo habla con el dominio de CloudFront. La cookie de refresco (`SameSite=Strict`) es del
  mismo sitio y no hay CORS entre frontend y API.
- **Desarrollo no usa la nube.** Se queda en Docker Compose local, como dice el ADR 0005. La rama `desarrollo` solo compila,
  prueba y arma la imagen en el ejecutor del CI; no despliega.
- **Base de datos:** dos proyectos de Supabase en el plan gratuito, CundiApp-Pre y CundiApp-Prod, por **Session pooler**
  (5432). Las migraciones las gobierna Flyway al arrancar la aplicación.
- **Despliegue sin balanceador ni SSH.** El CI construye la imagen, la sube a ECR y le ordena a la instancia por **SSM Run
  Command** que la baje y la levante (`infra/aws/instancia/desplegar.sh`). La instancia no tiene el puerto 22 abierto.
- **Secretos.** Viven en los **Environments de GitHub** (`preproduccion` y `produccion`; este último con revisores
  obligatorios). El CI los copia a Parameter Store (`/cundiapp/<ambiente>/…`, SecureString) y la instancia los lee al
  desplegar y los pasa al contenedor por el entorno del proceso: nunca a un archivo ni a un log.
- **Infraestructura como código:** CloudFormation, tres plantillas en `infra/aws/` (`compartido`, `servidor`, `frontend`).
  Las aplica una persona con permisos de administrador, una vez. El usuario de IAM del CI no crea ni modifica infraestructura:
  su política mínima está en `infra/aws/politica-de-ci.json`.
- **Jira:** cada despliegue comenta en las incidencias; **solo producción las cierra**.

## Por qué una sola instancia

El nivel gratuito de EC2 son 750 horas al mes: una instancia encendida todo el mes. Dos instancias agotarían las horas a
mitad de mes. Por eso los dos ambientes comparten máquina. Medido con la imagen real y límites de memoria: cada JVM usa
unos 300–315 MiB (Serial GC, `MaxRAMPercentage=60`, pilas de 512 KiB); dos suman ≈ 630 MiB, y con Docker, el sistema y el
agente de SSM se acercan a 1 GiB. Por eso: **t4g.small (2 GiB) aloja los dos; t3.micro (1 GiB) solo aguanta uno.**

## Consecuencias

**Lo bueno**

- Costo esperado de infraestructura: 0 USD, siempre que las cifras del nivel gratuito de la cuenta sean las que se asumen
  (ver «Lo que se acepta»).
- Sin balanceador, sin NAT, sin Fargate, sin llaves en el servidor y sin puertos de administración abiertos.
- Si la imagen nueva no queda sana, el script vuelve solo a la anterior.
- El flujo `feature → desarrollo → preproduccion → produccion` termina en un ambiente real en preproducción y producción.

**Lo que costó, y salió a la luz por intentarlo**

- El healthcheck de Actuator estaba cerrado (`denyAll`). Se abrieron solo `liveness` y `readiness`.
- Detrás de CloudFront y de la nginx, todos los estudiantes llegaban con la misma IP (medido: `172.20.0.4`, la del
  contenedor nginx). Los límites por IP (intentos de inicio de sesión, correos) compartían un solo cupo. `IpDelCliente`
  lee `X-Forwarded-For` sin creerle al cliente y el número de proxies de confianza es configuración (`SALTOS_DE_PROXY`: 0
  en local, 2 aquí). Comprobado con la nginx real: un `X-Forwarded-For: 9.9.9.9, 190.1.2.3` queda registrado como `190.1.2.3`.
- La simulación de la instancia encontró un defecto que ninguna otra prueba veía: la nginx no podía leer su configuración
  porque el script la guardaba con permisos de root.

**Lo que se acepta**

- **No hay despliegue sin caída.** Al reemplazar el contenedor hay una interrupción de unos segundos. Con un solo contenedor
  por ambiente y sin balanceador no es evitable. CloudFront no cachea `/api/*`, así que el estudiante ve un error breve.
- **Preproducción y producción comparten máquina.** Un fallo de la instancia tumba los dos, y un pico de preproducción
  compite por memoria con producción. Los límites de memoria por contenedor lo acotan, no lo eliminan.
- **El nivel gratuito tiene fecha y condiciones, y esto no se ha verificado en la cuenta real.** La prueba gratuita de
  t4g.small tiene una fecha de fin (se recordó como finales de 2026: confirmar en Billing > Free Tier). Si ya no aplica,
  t3.micro entra en el nivel gratuito de las cuentas nuevas pero solo aloja un ambiente. Una IPv4 pública asociada a una
  instancia en uso entra en el nivel gratuito; una Elastic IP sin asociar se cobra.
- **ECR gratuito: 500 MB.** Una imagen pesa ≈ 430 MB, así que **caben menos de dos**. Las reglas de ciclo de vida conservan
  las dos más recientes por ambiente; ECR puede quedar por encima de 500 MB y cobrar centavos. Se mitiga con una imagen más
  pequeña o con un repositorio público en ECR. Es el punto más probable de un cargo y por eso hay que mirar el primer mes.
- **Tramo CloudFront → instancia sin cifrar**, porque no hay dominio ni certificado. Se compensa con el grupo de seguridad (solo
  la lista de prefijos de CloudFront) más el encabezado secreto. Con un dominio y un certificado se cierra.
- **Supabase gratuito pausa el proyecto tras una semana sin actividad.** El flujo `latido.yml` hace una consulta pública cada
  tres días a cada ambiente.
- **Sin WAF y sin CSP.** Los límites por IP de la aplicación son la única defensa contra abuso por ahora.
- **Una sola cuenta de AWS** para los dos ambientes.
- **Migraciones.** Flyway Community no deshace migraciones: volver atrás es una migración nueva o restaurar un respaldo.
- **Se reconstruye la imagen en cada rama** en vez de promover la misma.

## Cambios a las reglas del equipo que esto implica

El equipo debe decidir estos dos puntos; este ADR no los cambia por sí solo:

1. **Tablero.** La guía dice que una tarjeta llega a Hecho al fusionarse en `desarrollo`. El flujo de despliegue hace que
   **solo producción cierre** la incidencia. Si se adopta, la regla del tablero y la Definición de Terminado vuelven a pedir
   despliegue, tal como preveía la nota de SCRUM-71.
2. **Plan de sprints.** SCRUM-70 y SCRUM-71 salen del Sprint 7. La guía los sitúa allí y habría que moverlos.
