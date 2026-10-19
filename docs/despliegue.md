# Despliegue en AWS sin costo (SCRUM-70 y SCRUM-71)

Guía operativa. La decisión y sus riesgos están en [ADR 0006](adr/0006-despliegue-en-aws.md).

> **Nada de esto se ha aplicado a una cuenta de AWS ni a Supabase.** No hay credenciales en este entorno de trabajo. Lo que
> sí está verificado: las imágenes de Docker (construidas y ejecutadas), las plantillas (`cfn-lint`), los flujos de GitHub
> (`actionlint` y `shellcheck`), el script `desplegar.sh` contra una **instancia simulada** (Docker dentro de Docker, con un
> `aws` falso: despliegue, puerta con secreto, IP real del cliente, reversión de una imagen rota, preproducción y producción
> conviviendo, memoria y ausencia de secretos en los registros) y el comando de Flyway contra la base local. El primer
> despliegue real encontrará cosas que aquí no se pudieron ver.

## 1. Arquitectura

```
Navegador ─► CloudFront ─┬─ /*      ─► S3 privado (Angular)
                         └─ /api/*  ─► EC2 (EIP) ─► nginx "borde" ─┬─ :80 (prod)    ─► backend prod    ─► Supabase Prod
                              + X-Origin-Verify                    └─ :8081 (preprod) ─► backend preprod ─► Supabase Pre
```

| Rama | Environment de GitHub | Qué hace el flujo | Perfil de Spring | Etiqueta de imagen | Base de datos |
|---|---|---|---|---|---|
| `desarrollo` | no usa | `./mvnw verify` + imagen local `dev-<sha>`; **sin nube** | `local` (Compose) | no se publica | PostgreSQL en Docker |
| `preproduccion` | `preproduccion` | imagen → ECR → instancia, puerto 8081 | `pre` | `preprod-<sha7>` | Supabase **CundiApp-Pre** |
| `produccion` | `produccion` (**revisores obligatorios**) | imagen → ECR → instancia, puerto 80 | `prod` | `prod-<semver>` si el commit lleva `vX.Y.Z`, si no `prod-<sha7>` | Supabase **CundiApp-Prod** |

Un solo repositorio ECR, `cundiapp-backend`, con etiquetas **inmutables**. Para que `prod-<semver>` salga con versión, la
etiqueta `vX.Y.Z` debe existir **antes** del push a `produccion`.

### Política de contenido (CSP)

La distribución manda una `Content-Security-Policy` con el frontend (SCRUM-93). El parámetro `ModoDeLaPoliticaDeContenido` de `frontend.yml` decide cómo:

- `solo-reporte` (por defecto): sale como `Content-Security-Policy-Report-Only`. El navegador anota en la consola lo que bloquearía y **no bloquea nada**. Así se prueba en preproducción con la PWA completa.
- `bloqueo`: sale como `Content-Security-Policy`. Se pasa a este modo cuando la consola de la PWA completa no muestra ninguna violación. Es redesplegar la pila con el parámetro cambiado.

**Qué permite y por qué** (se probó en bloqueo contra la compilación de producción: inicio de sesión y pantallas con sesión, sin violaciones):

| Directiva | Valor | Por qué |
|---|---|---|
| `default-src` | `'self'` | Todo lo que no se nombra solo viene de la propia aplicación |
| `script-src` | `'self'`, Google, Cloudflare | Sin scripts en línea y **sin `'unsafe-eval'` ni `'wasm-unsafe-eval'`**: la política bloquea los scripts en línea, los de otros sitios y el `eval`. Google: el botón de inicio de sesión. Cloudflare: el widget de Turnstile (SCRUM-92, ADR 0007), que solo se carga si hay llave pública. Hasta SCRUM-105 hacían falta `'unsafe-eval'` (opencv.js, que trae PaddleOCR), `'wasm-unsafe-eval'` y jsDelivr para el OCR de imágenes; al quitar el OCR se quitaron también |
| `style-src` | `'self'`, `'unsafe-inline'`, Google Fonts, Google | `'unsafe-inline'` solo para estilos: SweetAlert2 y el botón de Google los inyectan al dibujarse |
| `font-src` | `'self'`, `fonts.gstatic.com` | Las fuentes |
| `connect-src` | `'self'`, Google, Cloudflare | `'self'` cubre la API (`/api` sale del mismo origen) |
| `frame-src` | `accounts.google.com`, `challenges.cloudflare.com` | El marco del inicio de sesión con Google y el del widget de Turnstile |
| `worker-src` | `'self'` | El service worker de la PWA |
| `object-src`, `base-uri`, `form-action`, `frame-ancestors` | `'none'`, `'self'`, `'self'`, `'none'` | Sin plugins, sin cambiar la base, sin formularios hacia afuera y sin que otro sitio embeba la app |

**Lo que no se pudo probar localmente:** el flujo completo del inicio de sesión con Google (la cuenta y el origen autorizado solo existen en preproducción; sí se comprobó que el botón se dibuja sin violaciones). Hay que recorrerlo en preproducción antes de pasar a `bloqueo`.

**Dos cambios del Frontend que la política necesita:** la compilación ya no inserta el `onload` en línea que Angular usa para cargar la hoja de estilos crítica (`optimization.styles.inlineCritical: false`), y las URL de los archivos del OCR salen de dos CDN fijos.

### Límite de peticiones en el borde

La nginx del borde limita a **10 peticiones por segundo por cliente, con una ráfaga de 40**, y responde 429 al pasarse (`limit_req` en `infra/aws/instancia/borde.conf.template`). El cliente es la **última** entrada de `X-Forwarded-For` tal como llega, que es la que agrega CloudFront: lo que escriba el propio cliente queda a la izquierda y no se mira. Cada ambiente tiene su zona (el nombre lleva el puerto) y no comparten cupo. La aplicación suma sus propios límites: el inicio de sesión por fallos, los correos por IP y el análisis de PDF por estudiante. Para ver el 429 sin desplegar: levantar esta nginx contra cualquier servidor que responda y mandarle unas 100 peticiones seguidas.

### Por qué una sola instancia y qué cabe

El nivel gratuito de EC2 son 750 horas al mes: una instancia encendida todo el mes. Medido con la imagen real: cada JVM
usa ≈ 300–315 MiB (límite de 448 MiB por contenedor), dos ≈ 630 MiB.

| Tipo | Memoria | Aloja |
|---|---|---|
| `t4g.small` (ARM, por defecto) | 2 GiB | preproducción **y** producción |
| `t3.micro` (x86) | 1 GiB | **uno solo** |

**Verifica en la consola, Billing > Free Tier, cuál de los dos es gratis en tu cuenta y hasta cuándo** (la prueba gratuita de
`t4g.small` tiene fecha de fin, que no está confirmada aquí). La plantilla pide el tipo como parámetro y el CI elige
`linux/arm64` o `linux/amd64` leyendo la salida `Arquitectura` de la pila.

Imágenes base: salen de `public.ecr.aws` (espejo de las oficiales), no de Docker Hub, que limita las descargas anónimas por
IP. Para volver: `--build-arg REGISTRO=docker.io/library`.

**Pero `public.ecr.aws` también limita las descargas anónimas** (`429 toomanyrequests: Data limit exceeded`), y los
ejecutores de GitHub comparten IP: a veces el límite ya está agotado cuando llega nuestro turno. Pasó el 18 de octubre de 2026
(SCRUM-202): dos despliegues a preproducción fallaron al construir la imagen y el frontend quedó adelante del backend. Por eso
el CI no llama a `docker/build-push-action` directamente sino a la acción compuesta `.github/actions/construir-imagen`, que
prueba tres registros en orden y se queda con el primero que responda: `public.ecr.aws`, `mirror.gcr.io` (espejo de Docker Hub
de Google) y `docker.io`. Ninguno pide credenciales. Cuando el primero falla, el log trae un aviso (`::warning::`) que dice cuál
se usó; si fallan los tres, el error es el del último. Autenticarse en ECR Público (`registry-type: public`) también lo
resolvería, pero exige agregar `ecr-public:GetAuthorizationToken` y `sts:GetServiceBearerToken` a `infra/aws/politica-de-ci.json`,
y es un cambio en tu cuenta de AWS que esta solución evita.

Si un despliegue a preproducción falla y el frontend sí se desplegó, la causa suele ser esta; relanzar el flujo
(`gh workflow run desplegar.yml --ref preproduccion`) basta cuando fue un fallo pasajero.

```bash
cd Backend && docker compose --profile app up --build     # todo en local: http://localhost:4200
docker build -t cundiapp-backend .                         # solo el backend
```

## 2. Secretos

### Los que ya existen en los Environments `preproduccion` y `produccion` (los dos repositorios)

`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION` (`us-east-2`) y `SPRING_DATASOURCE_URL`.

### Los que **faltan** en el repositorio Backend, en cada Environment (distintos en cada uno)

| Secreto | Valor |
|---|---|
| `DB_USERNAME` | `postgres.<ref-del-proyecto>` de Supabase |
| `DB_PASSWORD` | la clave de ese proyecto de Supabase |
| `JWT_SECRETO` | aleatorio, 32+ caracteres, **distinto en cada ambiente** (`openssl rand -base64 48`) |
| `CORREO_SMTP_HOST`, `CORREO_SMTP_USUARIO`, `CORREO_SMTP_CLAVE` | Gmail con contraseña de aplicación |
| `GOOGLE_CLIENT_ID` | el del cliente OAuth |
| `TURNSTILE_SECRETO` | **Opcional.** La llave secreta de Cloudflare Turnstile (SCRUM-92, ADR 0007). Vacía, la verificación de bots queda apagada |

`SPRING_DATASOURCE_URL` debe ser la URL del **Session pooler** (puerto 5432, nunca el 6543, que rompe las sentencias
preparadas de Hibernate): `jdbc:postgresql://aws-0-<región>.pooler.supabase.com:5432/postgres?sslmode=require`.

El flujo comprueba esto **antes de construir nada** y, si falta alguno, falla diciendo solo los **nombres** que faltan.

A nivel de repositorio (opcionales, los usa el aviso a Jira): variables `JIRA_BASE_URL`, `JIRA_USER_EMAIL` y secreto
`JIRA_API_TOKEN`. Sin ellos el aviso se omite. Para el latido (§6): variables `DOMINIO_PREPROD` y `DOMINIO_PROD`.

### Cómo llegan a la aplicación

```
Environment de GitHub ─► (el CI) ─► Parameter Store /cundiapp/<amb>/… (SecureString) ─► (la instancia, al desplegar) ─► entorno del contenedor
```

La instancia solo puede leer `/cundiapp/*`; el usuario del CI solo puede **escribir** `/cundiapp/preprod/*` y
`/cundiapp/prod/*`. `desplegar.sh` nunca imprime un valor.

### `ORIGIN_VERIFY`: el único que se crea a mano, una vez por ambiente

Es el secreto que CloudFront le pone a cada petición para que la nginx de la instancia la acepte. CloudFront lo lee al
crearse `frontend.yml`, así que el CI no puede inventarlo: tiene que existir antes. Es `String` (no `SecureString`) porque
CloudFormation lo resuelve con `{{resolve:ssm:…}}`. Solo letras y números, 32 o más:

```bash
infra/operacion/crear-origin-verify.sh --ver   # dice cuales existen
infra/operacion/crear-origin-verify.sh         # crea los que faltan; nunca reemplaza uno que ya existe ni imprime el valor
```

## 3. Puesta en marcha (una vez, con un perfil de administrador de AWS CLI, región `us-east-2`)

```bash
cd Backend/infra/aws
# 1. Repositorio de imágenes
aws cloudformation deploy --region us-east-2 --stack-name cundiapp-compartido --template-file compartido.yml

# 2. ORIGIN_VERIFY (§2)

# 3. Datos de red
VPC=$(aws ec2 describe-vpcs --region us-east-2 --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)
SUBRED=$(aws ec2 describe-subnets --region us-east-2 --filters Name=vpc-id,Values=$VPC Name=default-for-az,Values=true --query 'Subnets[0].SubnetId' --output text)
PL=$(aws ec2 describe-managed-prefix-lists --region us-east-2 --filters Name=prefix-list-name,Values=com.amazonaws.global.cloudfront.origin-facing --query 'PrefixLists[0].PrefixListId' --output text)

# 4. La instancia (revisa antes el tipo en Billing > Free Tier; t3.micro solo aloja un ambiente)
aws cloudformation deploy --region us-east-2 --stack-name cundiapp-servidor --template-file servidor.yml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides TipoDeInstancia=t4g.small VpcId=$VPC SubredPublica=$SUBRED ListaDePrefijosCloudFront=$PL
DOM=$(aws cloudformation describe-stacks --region us-east-2 --stack-name cundiapp-servidor --query "Stacks[0].Outputs[?OutputKey=='DominioDelServidor'].OutputValue" --output text)

# 5. El frontend de cada ambiente (CloudFront necesita el dominio de la instancia)
aws cloudformation deploy --region us-east-2 --stack-name cundiapp-frontend-preprod --template-file frontend.yml \
  --parameter-overrides Entorno=preprod DominioDelServidor=$DOM PuertoDelServidor=8081
aws cloudformation deploy --region us-east-2 --stack-name cundiapp-frontend-prod --template-file frontend.yml \
  --parameter-overrides Entorno=prod DominioDelServidor=$DOM PuertoDelServidor=80
```

`servidor.yml` no abre el puerto 22: se entra con SSM Session Manager.

6. **Política del usuario de IAM del CI.** Sustituye `CUENTA` por el número de cuenta en
   [`infra/aws/politica-de-ci.json`](../infra/aws/politica-de-ci.json) y adjúntala al usuario cuyas llaves están en los
   Environments. Es el mínimo: subir la imagen, ordenar el despliegue por SSM, escribir los parámetros de los dos
   ambientes, leer las salidas de las pilas, publicar en S3 y limpiar el caché de CloudFront.
7. **Primer despliegue, dos pasadas.** El backend necesita el dominio de CloudFront en `CORS_ORIGENES` (Spring compara el
   `Origin` del navegador con el `Host`, que detrás de CloudFront no coinciden), y ese dominio solo existe después del paso 5.
   Por eso, si el backend se desplegó antes que el frontend, el CI deja `https://pendiente.invalid` (rechaza todo, que es lo
   seguro) y **hay que relanzar el flujo del backend una segunda vez** (`gh workflow run desplegar.yml --ref preproduccion`).
   Después de la primera vez todo corre en una pasada.
8. **Google:** agrega `https://<DominioDeCloudFront>` a los orígenes autorizados del cliente OAuth.
9. **Latido** (§6): crea las variables `DOMINIO_PREPROD` y `DOMINIO_PROD`.

### Lo que hace el flujo de despliegue

| Evento | Trabajos |
|---|---|
| Pull request | solo `ci.yml` (compilar, probar, ArchUnit, JaCoCo). **No despliega.** |
| Push a `desarrollo` | `./mvnw verify` + construir la imagen en el ejecutor. **Sin nube.** |
| Push a `preproduccion` | secretos → imagen en ECR → Parameter Store → orden SSM → comprobación pública → `newman` (carpeta de la guía) → Jira |
| Push a `produccion` | **aprobación manual** → lo mismo, puerto 80 → Jira: cerrar |

`desplegar.sh` en la instancia: lee los parámetros, baja la imagen, levanta el contenedor, espera a que quede **sano**
(hasta 3 minutos) y solo entonces abre la nginx para ese ambiente. **Si la imagen nueva no queda sana, vuelve sola a la
anterior** y el flujo falla. Probado en la instancia simulada con una imagen que muere al arrancar.

Hay una interrupción de unos segundos al reemplazar el contenedor: **no hay despliegue sin caída**.

**Migraciones como paso aparte (SCRUM-217).** Antes de tocar el contenedor que está sirviendo, `desplegar.sh` corre el servicio
`migrador` de `compose-backend.yml`: la **misma imagen nueva** arrancada con `CUNDIAPP_MIGRAR_Y_SALIR=true`, que migra con Flyway,
comprueba que no quedó nada pendiente y se apaga (`MigradorYSalida`). El backend de preproducción y producción arranca con
`SPRING_FLYWAY_ENABLED=false`, así que ya no migra a mitad del arranque.

- **Si la migración falla**, el script se aborta con código 4 y muestra las últimas líneas (sin secretos). El backend vigente **ni se
  toca**: sigue sirviendo. Antes, el contenedor nuevo caía en un bucle de reinicios y solo entonces se volvía a la imagen anterior.
- **Si el backend nuevo no queda sano después de migrar**, volver a la imagen anterior no deshace la migración. Regla: cada migración
  debe ser compatible con la versión anterior del código: expandir primero (agregar), desplegar, y recién después contraer (quitar).
- Si alguna migración faltara, el backend se niega a arrancar (`ddl-auto=validate`: «Schema-validation») en vez de levantar a medias.
- **En local nada cambia:** `docker-compose.yml` y las pruebas siguen migrando al arrancar (Flyway encendido por defecto).
- Probado con la imagen real contra un PostgreSQL vacío: migra 15 migraciones y sale con 0; una segunda vez dice «up to date» y sale con 0;
  con la contraseña equivocada sale con 1 sin tocar el backend que estaba sano; y el backend con Flyway apagado arranca sano sobre el esquema
  migrado, y se niega a arrancar sobre uno vacío.

**Promover en vez de reconstruir (SCRUM-215).** Producción debe recibir la imagen que se probó en preproducción. Cuando el commit de
`produccion` es una promoción con el mismo contenido que una imagen `preprod-<sha>` que sigue en ECR, el flujo le agrega la etiqueta
`prod-…` a esa misma imagen (`aws ecr put-image`, sin reconstruir). Si no puede, construye como siempre y deja un aviso.
Está **apagado por defecto** (en este repositorio la variable `PROMOVER_IMAGEN=true` se creó el 19/10/2026 y producción ya promovió así: `preprod-6fe6cd2` → `prod-5abe6ba`); para encenderlo, en este orden:

1. Aplicar la política de ciclo de vida nueva, que pone la regla de `prod-` antes que la de `preprod-` (una imagen con dos etiquetas
   es una sola imagen para ECR; sin ese orden, la limpieza de `preprod-` borraría la imagen de producción):
   `aws cloudformation deploy --template-file infra/aws/compartido.yml --stack-name <la pila de ECR>` (con los mismos parámetros de la puesta en marcha).
2. Probar el mecanismo en preproducción: `gh workflow run desplegar.yml --ref preproduccion -f etiqueta=preprod-prueba-promocion -f promover_de=<una etiqueta preprod-… que exista>`.
   El registro del paso «Promover la imagen de preproduccion» dice si se promovió o se construyó.
3. En Settings > Secrets and variables > Actions > Variables del repositorio Backend, crear `PROMOVER_IMAGEN` con el valor `true`.

El usuario del CI ya tiene `ecr:BatchGetImage` y `ecr:PutImage` (`infra/aws/politica-de-ci.json`). Volver atrás no cambia: sigue siendo
desplegar una etiqueta que ya existe.

## 4. Supabase y las migraciones

**Flyway las gobierna, no la CLI de Supabase** (dos escritores dejarían dos historiales que se contradicen). Para mirar o
ejecutar a mano contra CundiApp-Pre o CundiApp-Prod:

```bash
export DB_URL="jdbc:postgresql://aws-0-REGION.pooler.supabase.com:5432/postgres?sslmode=require"
export DB_USERNAME="postgres.<ref>"; export DB_PASSWORD="<clave>"

infra/operacion/migrar-a-mano.sh info       # qué hay aplicado y qué falta
infra/operacion/migrar-a-mano.sh validate   # que lo aplicado coincida con los archivos
CONFIRMO_MIGRAR_EN=aws-0-REGION.pooler.supabase.com infra/operacion/migrar-a-mano.sh migrate   # escribe
```

Con `DB_URL` en formato JDBC, como la aplicación. `info` y `validate` solo leen; `migrate` exige `CONFIRMO_MIGRAR_EN` igual al servidor de
`DB_URL`. Normalmente no hace falta: el despliegue migra solo (servicio `migrador`, SCRUM-217).
No expongas el esquema `cundiapp` en la Data API de Supabase.

### Operación a mano, como scripts (SCRUM-219)

Todo lo que antes era un bloque de comandos para copiar vive en `infra/operacion/`. Los scripts leen la misma conexión que la aplicación
(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), no llevan nada fijo ni secreto y solo necesitan Docker (las herramientas de PostgreSQL vienen en
una imagen de la misma versión que el servidor, así que no hay que instalar `pg_dump`). `shellcheck` los revisa en el CI.

| Script | Qué hace | Escribe en la base |
|---|---|---|
| `migrar-a-mano.sh info\|validate\|migrate` | Flyway a mano, con las migraciones del repositorio | solo `migrate`, con confirmación |
| `respaldar-bd.sh [archivo]` | Respaldo del esquema `cundiapp` (formato custom, incluye el historial de Flyway). No pisa un archivo que ya existe | no |
| `probar-respaldo.sh` | Respalda, restaura en un PostgreSQL desechable y compara las filas de cada tabla y la versión de Flyway | no |
| `restaurar-bd.sh respaldo.dump` | Reemplaza el esquema entero con el respaldo, en una transacción, tras sacar un respaldo de lo que había | **sí**, con confirmación |
| `crear-origin-verify.sh` | Crea `ORIGIN_VERIFY` en Parameter Store; no reemplaza el que ya existe | no (AWS) |

`probar-respaldo.sh` es seguro contra preproducción y producción: contra la base de origen solo lee, y la restauración ocurre en un contenedor
que él arranca y borra. Compara contando antes y después del respaldo, porque una base viva cambia mientras se respalda: una tabla que no
cambió tiene que tener exactamente las mismas filas; si cambió, la copia tiene que quedar dentro del rango y se avisa. `limite_de_uso`
(UNLOGGED, contadores de minutos) no se respalda a propósito.

## 5. Comprobaciones

```bash
DOM=<DominioDeCloudFront>
curl -fsS "https://$DOM/api/publico/guia/categorias" -o /dev/null && echo OK          # CloudFront > nginx > backend > Supabase
curl -s -o /dev/null -w '%{http_code}\n' "https://$DOM/api/mis/cuenta"                 # 401
# En la instancia (SSM Session Manager, sin SSH):
docker ps --format 'table {{.Names}}\t{{.Status}}'
docker logs --tail 50 cundiapp-prod-backend
docker stats --no-stream
```

## 6. Latido de Supabase

El plan gratuito pausa un proyecto tras una semana sin actividad (verifica el plazo en tu panel). `latido.yml` consulta
cada tres días la lista de categorías de cada ambiente, que sale de PostgreSQL. No usa AWS ni los Environments.

## 7. Volver atrás

**Backend.** Una imagen que no queda sana se revierte sola. Para volver a una versión anterior que sí se desplegó, sin
reconstruir:

```bash
gh workflow run desplegar.yml --ref produccion --repo crst11/Backend -f etiqueta=prod-1.1.0
```

Las etiquetas viven en ECR, pero **solo se conservan las dos más recientes por ambiente**: no se puede volver más atrás que eso
sin reconstruir.

**Frontend.** El bucket tiene versiones. Lo más simple es relanzar el flujo sobre el commit anterior (`gh run rerun <id>`).

**Esquema.** Flyway Community no deshace migraciones. O una migración nueva que revierta (el camino normal), o restaurar un
respaldo (el historial de Flyway vive dentro del esquema, así que se restaura con él):

```bash
# Verifica qué respaldos incluye tu plan de Supabase: no asumas que hay. Estos son tuyos y no dependen del plan.
infra/operacion/respaldar-bd.sh                                  # respaldo-cundiapp-AAAA-MM-DDThhmmss.dump
# Detén el backend en la instancia y restaura (reemplaza el esquema; saca antes un antes-de-restaurar-*.dump para deshacer):
CONFIRMO_RESTAURAR_EN=aws-0-REGION.pooler.supabase.com infra/operacion/restaurar-bd.sh respaldo-cundiapp-AAAA-MM-DDThhmmss.dump
```

El respaldo trae datos personales de estudiantes: guárdalo fuera del repositorio y bórralo cuando ya no haga falta.
Restaurar sobre una base nueva y vacía (un proyecto de Supabase recién creado tras un desastre) también funciona. Después de restaurar, la base
queda en la versión de Flyway del respaldo; el migrador del próximo despliegue aplica lo que falte.

**Qué está probado.** Con PostgreSQL 16 (local y en el CI, en cada pull request: trabajo `respaldo-restaurable`): migrar una base vacía, respaldar,
restaurar en otra base y comparar tabla por tabla; restaurar sobre una base con otro contenido (reemplaza todo, incluidas tablas que el respaldo
no tiene); restaurar sobre una base vacía; negarse sin confirmación; negarse con un archivo dañado sin tocar la base; y deshacer con el
respaldo previo. **Qué no:** la restauración nunca se ha corrido contra Supabase. Para comprobarlo sin riesgo, con las variables de Preprod:
`infra/operacion/probar-respaldo.sh` (solo lee Supabase). Falta correrlo una vez antes de necesitarlo, y confirmar que el usuario de la base puede
leer todo el esquema `cundiapp` y que el Session pooler acepta `pg_dump`.

## 8. Costos y vigilancia

Objetivo: **0 USD**. Es lo que se espera, no lo que está comprobado. Revisa **Billing > Free Tier** la primera semana:

| Recurso | Límite gratuito asumido | Riesgo |
|---|---|---|
| EC2 `t4g.small` o `t3.micro` | 750 h/mes de **una** instancia | fecha de fin de la prueba de `t4g.small`; dos instancias agotan las horas |
| IPv4 pública (la Elastic IP, asociada) | incluida con la instancia | si se desasocia, se cobra |
| EBS | 30 GiB (la plantilla usa 20) | — |
| ECR | 500 MB al mes | **medio riesgo**: una imagen pesa ≈ 430 MB y se conservan dos por ambiente; puede superarlo por centavos |
| CloudFront, S3, SSM Parameter Store (estándar) | niveles gratuitos amplios | bajo, con el tráfico de un piloto |
| Transferencia de salida de EC2 | 100 GB/mes | bajo (CloudFront absorbe casi todo) |

La alarma de presupuesto en cero que ya existe es la red de seguridad.

## 9. Lo que no está resuelto

- **Sin despliegue sin caída.** Un contenedor por ambiente y sin balanceador: hay una interrupción de segundos.
- **Preproducción y producción comparten una máquina.** Un fallo o una caída de la instancia tumba a las dos.
- **Cifras del nivel gratuito sin verificar en la cuenta**, en especial la fecha de fin de `t4g.small` y el límite de ECR.
- **Sin dominio propio.** El tramo CloudFront → instancia va en http; el candado es el grupo de seguridad (solo la lista de
  prefijos de CloudFront) más `X-Origin-Verify`. Con dominio y certificado se pasa a https.
- **Sin WAF.** Los límites de la aplicación y el del borde son la única defensa contra abuso. La CSP del frontend existe (SCRUM-93) y sale en `solo-reporte` hasta que se pase a `bloqueo`: ver *Política de contenido (CSP)*.
- **Supabase gratuito se pausa** sin actividad; el latido lo mitiga, no lo garantiza.
- **El Environment `produccion` no restringe ramas de despliegue.** Cualquier rama podría pedir aprobación para desplegar en
  producción; conviene limitarlo a `produccion` en Settings > Environments > Deployment branches.
- **Una imagen por rama.** Al fusionar `desarrollo` en `preproduccion` el commit de fusión tiene otro hash, así que se
  reconstruye en vez de promover los mismos bytes.
- **Plantillas sin aplicar en una cuenta real.** `cfn-lint` valida sintaxis y propiedades, no permisos ni límites de la cuenta.
- **Imagen ARM sin probar.** El Dockerfile compila nativo y solo la última etapa corre en la plataforma de destino, pero la
  imagen `linux/arm64` no se ha construido ni ejecutado aquí.

## 10. Imágenes base, registros y apagado

Tres detalles de los 12 factores que viven en el despliegue y que no se ven desde la aplicación.

### Imágenes base fijadas por digest (SCRUM-214)

Todo `FROM` y todo `image:` de este repositorio lleva `@sha256:…`: `Dockerfile`, `docker-compose.yml`, `compose-borde.yml` y el CI. Así el mismo commit construye siempre la misma imagen. El digest es el de la **lista multiplataforma** y tiene que ser el mismo en ECR Público, `mirror.gcr.io` y Docker Hub, porque el `Dockerfile` se construye contra los tres (SCRUM-202): uno que exista en un registro y no en otro rompería el respaldo.

Lo que se pierde: una etiqueta como `21-jre-alpine` recibía parches sola, y un digest no. **Hay que refrescarlos a mano** cuando salga un aviso de seguridad de Temurin, Alpine, nginx o Postgres, y de todas formas cada cierto tiempo:

```bash
bash infra/actualizar-imagenes-base.sh --ver   # no escribe: dice cuáles cambiaron (código 1 si alguno)
bash infra/actualizar-imagenes-base.sh         # resuelve, comprueba los tres registros y reescribe
```

Después: `./mvnw clean verify`, construir la imagen y abrir un pull request. El CI falla si una imagen vuelve a quedar sin digest (`infra/comprobar-imagenes-fijadas.sh`), pero **no** avisa de que un digest quedó viejo.

### Registros estructurados (SCRUM-213)

En `pre` y `prod` la salida estándar es un JSON por línea (formato ECS), con `service.environment` y la clave `peticion`. Para seguir una petición: pedir a la persona el valor del encabezado `X-Request-Id` de la respuesta y buscarlo en los registros del contenedor:

```bash
docker logs cundiapp-preprod-backend 2>&1 | grep '"peticion":"ID_DE_LA_PETICION"'
```

Los registros quedan en el disco de la instancia con rotación (`json-file`, 10 MB × 3). No hay un lugar central donde consultarlos: llevarlos a CloudWatch Logs es un paso aparte (el driver `awslogs` y un permiso en el rol de la instancia).

### Apagado ordenado (SCRUM-212)

Spring espera hasta 25 s a que terminen las peticiones en curso (`spring.lifecycle.timeout-per-shutdown-phase`). Docker mata el contenedor a los 10 s por defecto, así que `compose-backend.yml` fija `stop_grace_period: 40s`. `ApagadoOrdenadoTest` vigila que el de Docker siga siendo mayor que el de Spring.
