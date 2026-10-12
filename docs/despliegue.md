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
                              + X-Origin-Verify                    └─ :81 (preprod) ─► backend preprod ─► Supabase Pre
```

| Rama | Environment de GitHub | Qué hace el flujo | Perfil de Spring | Etiqueta de imagen | Base de datos |
|---|---|---|---|---|---|
| `desarrollo` | no usa | `./mvnw verify` + imagen local `dev-<sha>`; **sin nube** | `local` (Compose) | no se publica | PostgreSQL en Docker |
| `preproduccion` | `preproduccion` | imagen → ECR → instancia, puerto 81 | `pre` | `preprod-<sha7>` | Supabase **CundiApp-Pre** |
| `produccion` | `produccion` (**revisores obligatorios**) | imagen → ECR → instancia, puerto 80 | `prod` | `prod-<semver>` si el commit lleva `vX.Y.Z`, si no `prod-<sha7>` | Supabase **CundiApp-Prod** |

Un solo repositorio ECR, `cundiapp-backend`, con etiquetas **inmutables**. Para que `prod-<semver>` salga con versión, la
etiqueta `vX.Y.Z` debe existir **antes** del push a `produccion`.

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
for AMB in preprod prod; do
  aws ssm put-parameter --region us-east-2 --name /cundiapp/$AMB/ORIGIN_VERIFY --type String --value "$(openssl rand -hex 32)"
done
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
  --parameter-overrides Entorno=preprod DominioDelServidor=$DOM PuertoDelServidor=81
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

**Migraciones:** Flyway corre al arrancar. Si la imagen nueva trae una migración y no queda sana, volver a la imagen anterior
no deshace la migración. Regla: expandir primero (agregar), desplegar, y recién después contraer (quitar).

## 4. Supabase y las migraciones

**Flyway las gobierna, no la CLI de Supabase** (dos escritores dejarían dos historiales que se contradicen). Para mirar o
ejecutar a mano contra CundiApp-Pre o CundiApp-Prod:

```bash
export DB_URL="jdbc:postgresql://aws-0-REGION.pooler.supabase.com:5432/postgres?sslmode=require"
export DB_USERNAME="postgres.<ref>"; export DB_PASSWORD="<clave>"

docker run --rm -v "$PWD/src/main/resources/db/migration:/flyway/sql:ro" flyway/flyway:11 \
  -url="$DB_URL" -user="$DB_USERNAME" -password="$DB_PASSWORD" \
  -schemas=cundiapp -defaultSchema=cundiapp -locations=filesystem:/flyway/sql \
  info          # también: validate  |  migrate
```

Verificado contra la base local: lista las 8 migraciones. En Windows con Git Bash anteponer `MSYS_NO_PATHCONV=1` y usar `$(pwd -W)`.
No expongas el esquema `cundiapp` en la Data API de Supabase.

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
# Verifica qué respaldos incluye tu plan de Supabase: no asumas que hay
docker run --rm -e PGPASSWORD="$DB_PASSWORD" public.ecr.aws/docker/library/postgres:16-alpine \
  pg_dump -h aws-0-REGION.pooler.supabase.com -p 5432 -U "$DB_USERNAME" -d postgres -n cundiapp -Fc > "respaldo-$(date +%F).dump"
```

Sin probar: el respaldo y la restauración nunca se han ejecutado contra Supabase. Hacerlo una vez en Preprod antes de necesitarlo.

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
- **Sin WAF y sin CSP.** Los límites por IP de la aplicación son la única defensa contra abuso.
- **Supabase gratuito se pausa** sin actividad; el latido lo mitiga, no lo garantiza.
- **El Environment `produccion` no restringe ramas de despliegue.** Cualquier rama podría pedir aprobación para desplegar en
  producción; conviene limitarlo a `produccion` en Settings > Environments > Deployment branches.
- **Una imagen por rama.** Al fusionar `desarrollo` en `preproduccion` el commit de fusión tiene otro hash, así que se
  reconstruye en vez de promover los mismos bytes.
- **Plantillas sin aplicar en una cuenta real.** `cfn-lint` valida sintaxis y propiedades, no permisos ni límites de la cuenta.
- **Imagen ARM sin probar.** El Dockerfile compila nativo y solo la última etapa corre en la plataforma de destino, pero la
  imagen `linux/arm64` no se ha construido ni ejecutado aquí.
