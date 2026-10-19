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

**Actualizar la pila (no crearla): fija la AMI.** Con el parámetro `ImagenDeLaInstancia` vacío, `ImageId` se resuelve cada vez a la última AMI de Amazon Linux 2023, y en cuanto AWS publica una nueva CloudFormation **reemplaza la instancia** (el servidor de producción y preproducción), aunque lo único que cambie sea un grupo de registros (SCRUM-224). Para actualizar, pasa la AMI de la instancia que ya corre y **revisa el conjunto de cambios antes de ejecutarlo**:

```bash
AMI=$(aws ec2 describe-instances --region us-east-2 --instance-ids <IdDeInstancia> --query 'Reservations[0].Instances[0].ImageId' --output text)
aws cloudformation deploy --region us-east-2 --stack-name cundiapp-servidor --template-file servidor.yml   --capabilities CAPABILITY_NAMED_IAM --no-execute-changeset   --parameter-overrides TipoDeInstancia=t4g.small VpcId=$VPC SubredPublica=$SUBRED ListaDePrefijosCloudFront=$PL DiscoGiB=20 ImagenDeLaInstancia=$AMI
aws cloudformation describe-change-set --change-set-name <el que imprime el comando> --query 'Changes[].ResourceChange.[Action,LogicalResourceId,Replacement]'
```

`Servidor` debe salir `Conditional` o sin cambios, **nunca `True`**. `Conditional` significa que CloudFormation solo sabe al ejecutar si el valor cambia; con la AMI fijada a la que ya tiene no cambia. Si quieres que un reemplazo accidental sea imposible, pon antes una política de pila que prohíba `Update:Replace` y `Update:Delete` sobre `Servidor` y `AsociacionDeIp` (así se aplicó la de SCRUM-222): si algo fuera a reemplazarla, la actualización falla y se revierte sin tocarla.

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

**Qué está probado.** Con PostgreSQL 17 (local y en el CI, en cada pull request: trabajo `respaldo-restaurable`): migrar una base vacía, respaldar,
restaurar en otra base y comparar tabla por tabla; restaurar sobre una base con otro contenido (reemplaza todo, incluidas tablas que el respaldo
no tiene); restaurar sobre una base vacía; negarse sin confirmación; negarse con un archivo dañado sin tocar la base; y deshacer con el
respaldo previo. La base de ese trabajo del CI no es un `services:` de GitHub sino un paso propio que baja la imagen **por digest, con reintentos y de tres registros** (ECR Público, Docker Hub y `mirror.gcr.io`): con un solo registro y sin reintentos, el límite de descargas de ECR Público (`toomanyrequests`) lo hizo fallar tres veces sin que el código tuviera nada que ver (SCRUM-223).

**Contra Supabase real (19/10/2026, Preproducción).** `probar-respaldo.sh` respaldó el proyecto por el Session pooler (5432), restauró la copia en un
contenedor local y la comparó: 32 tablas, 331 filas, idénticas, y la misma versión de Flyway (15). El servidor es **PostgreSQL 17**: el script
elige las herramientas de la misma versión del servidor (con `pg_dump` 16 se habría negado). Tardó unos 50 s, casi todo bajar la imagen la primera vez.
**Qué no:** `restaurar-bd.sh` no se ha corrido sobre Supabase (reemplaza datos, así que se probó solo en PostgreSQL local y en el CI). Antes de
usarlo en un desastre real, ensayarlo contra un proyecto de Supabase desechable, con `ensayar-restauracion.sh` (siguiente apartado).

### Ensayo de `restaurar-bd.sh` sobre Supabase (SCRUM-226)

`ensayar-restauracion.sh` ensaya `restaurar-bd.sh` de punta a punta contra una base **desechable**: recrea el esquema con Flyway, le pone 25 cuentas sintéticas (`ensayoN@ensayo.invalid`), lo respalda (estado A), lo ensucia con 5 cuentas más (estado B) y comprueba:

1. que `restaurar-bd.sh` **se niega** sin `CONFIRMO_RESTAURAR_EN` y la base sigue en B;
2. que un respaldo **dañado** (cortado a la mitad) se rechaza sin tocar la base;
3. que el respaldo bueno devuelve la base **exactamente** al estado A (tabla por tabla y la versión de Flyway) y deja el respaldo previo (`antes-de-restaurar-*.dump`);
4. que restaurar ese respaldo previo **deshace** la restauración (vuelve a B).

Corre solo en el CI contra el PostgreSQL local (paso «Ensayar restaurar-bd.sh»), con la prueba de que falla si a `restaurar-bd.sh` se le quita la confirmación. Lo que el CI no puede decir es cómo se porta **Supabase** (permisos del usuario `postgres`, el pooler, tiempos), y para eso hay que correrlo a mano:

1. **Crea un proyecto de Supabase solo para esto** (por ejemplo `cundiapp-ensayo`, con una contraseña de base nueva que no se parezca a las de preproducción y producción). Si tu plan ya tiene el máximo de proyectos activos, crea el proyecto en otra organización o borra uno de ensayo viejo; **no pauses ni toques** los de preproducción y producción.
2. En el proyecto: *Connect → Session pooler* (puerto **5432**, nunca el 6543). Te da el servidor `aws-0-<región>.pooler.supabase.com` y el usuario `postgres.<ref>`.
3. En tu máquina (Docker encendido), desde la raíz del repositorio:

```bash
export DB_URL='jdbc:postgresql://aws-0-REGION.pooler.supabase.com:5432/postgres?sslmode=require'
export DB_USERNAME='postgres.REFDELPROYECTO'
read -rs DB_PASSWORD && export DB_PASSWORD        # pega la contraseña: no queda en el historial
CONFIRMO_ENSAYO_EN=aws-0-REGION.pooler.supabase.com bash infra/operacion/ensayar-restauracion.sh
```

Contra PostgreSQL local tarda unos 50 s. Debe terminar en `OK: restaurar-bd.sh se negó sin confirmar, …`; si alguna línea sale `FALLO`, **no uses `restaurar-bd.sh` en un desastre real** hasta entender por qué, y guarda la salida. Lo más probable de encontrar en Supabase: un permiso que le falta al usuario para soltar o crear el esquema, o un objeto de la extensión que el respaldo arrastra.
4. **Borra el proyecto** al terminar: queda con datos sintéticos, sin valor.

**Tres barreras** para que no se pueda apuntar a datos reales (el ensayo borra el esquema `cundiapp`):

- `CONFIRMO_ENSAYO_EN` tiene que ser igual al servidor de `DB_URL`.
- Si hay `aws`, se compara el servidor con los `DB_URL` de `/cundiapp/*` en Parameter Store (preproducción y producción) y se niega si coincide (no imprime ningún valor). Sin `aws` o sin permiso para leerlos, solo avisa.
- Si la base ya tiene el esquema `cundiapp` con **cualquier estudiante que no sea sintético**, se niega: una base real no la pasa aunque se equivoquen las dos anteriores. Está probado: con un estudiante normal en la base, el ensayo se niega y la deja intacta.

**Qué no cubre:** restaurar datos *reales* de preproducción en la base de ensayo (llevaría datos personales a un proyecto sin la misma protección), ni una restauración grande a través del pooler (el ensayo usa unas decenas de filas: los tiempos de una base grande hay que medirlos aparte).

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

Los registros se guardan en el disco de la instancia con rotación (`json-file`, 10 MB × 3) y, además, van a CloudWatch Logs cuando el rol de la instancia lo permite (ver «Registros en CloudWatch Logs», más abajo).

### Apagado ordenado (SCRUM-212)

Spring espera hasta 25 s a que terminen las peticiones en curso (`spring.lifecycle.timeout-per-shutdown-phase`). Docker mata el contenedor a los 10 s por defecto, así que `compose-backend.yml` fija `stop_grace_period: 40s`. `ApagadoOrdenadoTest` vigila que el de Docker siga siendo mayor que el de Spring.

### Varias instancias: una sola reparte los avisos (SCRUM-220)

Hoy corre una instancia del backend por ambiente, pero la aplicación no depende de eso. La ronda de avisos (cada 5 minutos) tiene dos partes:

- **Crear los avisos de vencimiento** ya era segura con varias instancias: la base de datos no admite dos avisos por pendiente (`ON CONFLICT DO NOTHING`).
- **Entregarlos por Web Push** no lo era: dos instancias leían los mismos avisos pendientes y el estudiante recibía el push repetido, con los intentos contados dos veces.

Ahora la ronda corre bajo un candado asesor de PostgreSQL (`pg_try_advisory_xact_lock`, llave `3700001`): la instancia que no lo obtiene se salta la ronda y lo vuelve a intentar a los 5 minutos. No hay tablas ni dependencias nuevas. El candado vive en una transacción propia, **sobre una conexión propia fuera del pool de la aplicación** (SCRUM-228: el pool de preproducción y producción es de 5 conexiones, y la ronda con sus envíos de push puede durar minutos; si el candado saliera de ese pool, una ronda lenta dejaría sin conexión a las peticiones de los estudiantes), así que cada instancia abre **una conexión más** a la base (6 en total). La ronda tiene además un **presupuesto de 90 segundos**: lo que no alcance a entregar queda pendiente y sale en la ronda siguiente. El candado se suelta al terminar la transacción; si la instancia muere a mitad de la ronda, cae con su conexión, así que **no puede quedar un candado trabado**. Por ir dentro de una transacción también funciona con el Transaction pooler de Supabase.

Para ver si una instancia lo está dejando pasar: nivel `DEBUG` del logger `AvisosProgramados` («Otra instancia está repartiendo la ronda de avisos»). La limpieza de límites (`LimpiezaDeLimites`) no lo necesita: borrar dos veces lo mismo es inofensivo. Lo cubre `UnaSolaInstanciaTest`, contra PostgreSQL real.

### Paridad entre desarrollo y la nube (SCRUM-221)

El factor 10 pide que desarrollo se parezca a producción. Qué es igual y qué sigue distinto:

| Pieza | Desarrollo | Nube | Estado |
|---|---|---|---|
| Motor de base de datos | PostgreSQL 17 (Compose, Testcontainers y CI) | PostgreSQL 17 (Supabase) | Igual. Antes era 16 contra 17: lo destapó la prueba de respaldo contra Supabase |
| Migraciones | Flyway, las mismas | Flyway, las mismas | Igual |
| Camino del código de verificación | SMTP a Mailpit (`docker-compose.correo.yml`) o consola | SMTP | Igual si se usa el archivo de correo; por defecto sigue saliendo en la consola |
| Web Push | Apagado salvo que se definan `VAPID_*` (ver README) | Encendido | Distinto, a propósito: necesita llaves y un navegador suscrito |
| Arquitectura de la imagen | x86 | ARM (`t4g`) | Distinto: la imagen `linux/arm64` no se ha construido ni ejecutado en el CI |

**Subir un volumen local de PostgreSQL 16 a 17.** El directorio de datos de una versión no lo abre la siguiente. Hay que respaldar, recrear el volumen y restaurar con los scripts de operación:

```bash
bash infra/operacion/respaldar-bd.sh mi-respaldo.dump        # con la base 16 todavía en marcha
docker compose down && docker volume rm backend_cundiapp-datos
docker compose up -d db --wait                               # ahora en 17
CONFIRMO_RESTAURAR_EN=localhost bash infra/operacion/restaurar-bd.sh mi-respaldo.dump
```

(Las tres variables `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` salen del `.env`; el respaldo trae datos personales: bórralo después.) El nombre del volumen lleva el del proyecto de Compose, que es el de la carpeta. Con esto se pasó la base local de 16 a 17 sin perder cuentas: 28 estudiantes antes y después, mismos pendientes y versión de Flyway.

### Registros en CloudWatch Logs (SCRUM-222)

Antes, los registros vivían solo en el disco de la instancia: si se reemplazaba, se perdían, y no había dónde buscar entre ambientes. Ahora el backend y la puerta de entrada los mandan a CloudWatch Logs con el controlador `awslogs` de Docker.

| Grupo | Qué trae | Retención |
|---|---|---|
| `/cundiapp/prod/backend` | Backend de producción (JSON ECS) | 30 días (parámetro `RetencionDeRegistrosDias`) |
| `/cundiapp/preprod/backend` | Backend de preproducción | igual |
| `/cundiapp/borde` | nginx de la puerta de entrada (los dos ambientes) | igual |

El migrador de Flyway no se envía: es de una sola ejecución y su salida ya queda en el historial de SSM del despliegue.

**Cómo se activa.** `servidor.yml` crea los tres grupos (con `Retain`: sobreviven a la pila) y le da al rol de la instancia `logs:CreateLogStream` y `logs:PutLogEvents` solo sobre ellos. Se aplica con la actualización de pila de la §3, **con la AMI fijada** (sin eso, CloudFormation reemplaza la instancia: SCRUM-224). Solo suma los grupos y la política. Después no hay que hacer nada más: el siguiente despliegue lo activa solo.

**Qué pasa si el permiso todavía no está.** `desplegar.sh` prueba antes de arrancar (`registros.sh`: crea el stream `sondeo`, o ve que ya existe). Si no puede, **el backend arranca igual con `json-file`** y el despliegue imprime «AVISO: no puedo escribir en CloudWatch Logs…». Esto importa porque `awslogs` comprueba CloudWatch al arrancar el contenedor y, sin esa red de seguridad, un permiso faltante dejaría el backend sin levantar. En el despliegue que sí lo activa verás: `Registros del backend: CloudWatch Logs (/cundiapp/prod/backend)`. Cambiar el controlador recrea el contenedor: la nginx compartida se reinicia unos segundos la primera vez.

**Si CloudWatch deja de responder con el backend en marcha.** El modo es `non-blocking` con un buzón de 4 MB: la aplicación no se queda esperando; lo que no cabe se descarta. `docker logs` sigue funcionando (Docker guarda una copia local), y `desplegar.sh` lo usa para mostrar las últimas líneas si un despliegue falla.

**Cómo consultarlos** (CloudWatch → Logs Insights, o por consola):

```bash
aws logs tail /cundiapp/prod/backend --since 15m --follow --region us-east-2
```

```
# Seguir una petición por el X-Request-Id que ve la persona
fields @timestamp, `log.level`, message
| filter peticion = "ID_DE_LA_PETICION"
| sort @timestamp asc

# Errores de la última hora
fields @timestamp, message
| filter `log.level` = "ERROR"
| sort @timestamp desc
| limit 50
```

**Costo.** El nivel gratuito incluye 5 GB de ingesta al mes; el backend escribe unos pocos MB al día. Si crece, `RetencionDeRegistrosDias` baja el almacenamiento. Los del backend no llevan datos personales ni tokens (§10, «Registros estructurados»). **Los de la puerta de entrada (`/cundiapp/borde`) tampoco llevan la IP del cliente** (SCRUM-225). El formato por defecto de nginx la imprime (la de CloudFront en `$remote_addr` y la del cliente en `X-Forwarded-For`), y con 30 días de retención son datos personales bajo la Ley 1581. `borde.conf.template` define un formato JSON propio, sin IP, sin query (`$uri` y no `$request`) y sin user agent:

```json
{"@timestamp":"2026-10-19T21:17:38-05:00","puerto":"8080","metodo":"GET","ruta":"/api/publico/guia/categorias","estado":200,"bytes":184,"duracion":0.004,"peticion":"3f2a…"}
```

`peticion` es el `X-Request-Id`, la misma clave que usa el backend: se sigue una petición de `/cundiapp/borde` a `/cundiapp/prod/backend` buscando ese valor. `puerto` distingue el ambiente (8080 producción, 8081 preproducción). El registro de **errores** de nginx también imprime `client: <IP>` (por ejemplo en cada rechazo de `limit_req`), así que en estos servidores solo se dejan los errores críticos; un 429 o un 502 se siguen viendo en `estado` del acceso. Lo que se pierde: el detalle de errores de nginx (por ejemplo, por qué falló la conexión con el backend). `probar-borde.sh` levanta la nginx real con la configuración de los dos ambientes y comprueba el registro, en cada pull request.

**Los eventos anteriores a este cambio conservan la IP** hasta que expiren (30 días) o se borre el stream: `aws logs delete-log-stream --log-group-name /cundiapp/borde --log-stream-name cundiapp-borde`.

**Estado (19/10/2026).** Aplicado a la cuenta (`servidor.yml` con la AMI fijada, la instancia no se reemplazó) y probado en preproducción: tras redesplegar, llegan a CloudWatch los registros JSON del backend y los accesos de la puerta de entrada. Producción lo activa en su próximo despliegue.
