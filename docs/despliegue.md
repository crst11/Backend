# Despliegue en AWS (SCRUM-70 y SCRUM-71)

Guía operativa. La decisión y sus costos están en [ADR 0006](adr/0006-despliegue-en-aws.md).

> **Nada de esto se ha aplicado a una cuenta de AWS ni a Supabase.** Lo que sí está verificado: las dos
> imágenes de Docker (construidas, ejecutadas y probadas con Compose), las plantillas (`cfn-lint`), el flujo
> de GitHub (`actionlint` y `shellcheck`), `newman` contra un backend real y el comando de Flyway contra la
> base local. El primer despliegue real encontrará cosas que aquí no se pudieron ver.

## 1. Pendientes antes del primer despliegue

- [ ] Fusionar **Backend #33 y Frontend #29** (SCRUM-77). Sin ellos, cerrar sesión deja el token vivo 20 minutos.
- [ ] Fusionar estas ramas: `feature/SCRUM-70-docker-compose` y después `feature/SCRUM-71-despliegue-en-aws`, en los dos repositorios.
- [ ] Elegir la **región** y usar la misma en AWS y en Supabase (la latencia entre ambos cuenta para el requisito de 3 s).
- [ ] Crear los tres proyectos de Supabase (§3). PRE ya existe por SCRUM-60.
- [ ] Una cuenta de AWS con una **alarma de presupuesto** antes de crear nada (§8).
- [ ] Crear los tres Environments de GitHub y poner a Naomi como revisora obligatoria de `produccion` (§5, paso 4).
- [ ] Agregar el dominio de CloudFront a los orígenes autorizados del cliente OAuth de Google (§5, paso 9).
- [ ] Decidir si DEV vive en la nube o se queda en Docker Compose local (§8: cuesta lo mismo que los otros).

## 2. Ambientes e imágenes

| Rama | Environment de GitHub | Pila AWS | Perfil de Spring | Etiqueta de imagen | Supabase | Tareas |
|---|---|---|---|---|---|---|
| `desarrollo` | `desarrollo` | `dev` | `pre` | `dev-<sha7>` | proyecto Dev | 1 |
| `preproduccion` | `preproduccion` | `preprod` | `pre` | `preprod-<sha7>` | proyecto Preprod | 1 |
| `produccion` | `produccion` (**revisores obligatorios**) | `prod` | `prod` | `prod-<semver>` si el commit lleva `vX.Y.Z`, si no `prod-<sha7>` | proyecto Prod | 2 |

Un solo repositorio ECR, `cundiapp-backend`, con etiquetas **inmutables**: `prod-1.2.0` siempre es la misma imagen.
Para que `prod-<semver>` salga con versión, la etiqueta `vX.Y.Z` debe existir **antes** de hacer el push a `produccion`.

El perfil `pre` también sirve a DEV a propósito: exige todas las variables sin valores por defecto, y así un DEV mal
configurado falla al arrancar en vez de funcionar a medias. Por lo mismo, DEV necesita SMTP real.

Construir y probar las imágenes en local:

```bash
cd Backend && docker compose --profile app up --build     # http://localhost:4200
docker build -t cundiapp-backend .                         # solo el backend
docker build -t cundiapp-frontend --build-arg CONFIGURACION=dev ../Frontend
```

Las imágenes base salen de `public.ecr.aws` (espejo de las oficiales) y no de Docker Hub, que limita las descargas
anónimas por IP y los ejecutores de CI comparten IP. Para volver: `--build-arg REGISTRO=docker.io/library`.

## 3. Variables y secretos por ambiente

Los secretos viven **solo** en Parameter Store, bajo `/cundiapp/<ambiente>/`. No están en GitHub, ni en las plantillas,
ni en el flujo. La tarea los lee al arrancar con su rol de ejecución, que solo ve el prefijo de su propio ambiente.

| Parámetro (`/cundiapp/<amb>/…`) | Tipo | Valor |
|---|---|---|
| `DB_URL` | SecureString | `jdbc:postgresql://aws-0-REGION.pooler.supabase.com:5432/postgres?sslmode=require` |
| `DB_USERNAME` | SecureString | `postgres.<ref-del-proyecto>` |
| `DB_PASSWORD` | SecureString | la del proyecto de Supabase de ese ambiente |
| `JWT_SECRETO` | SecureString | aleatorio, mínimo 32 caracteres; **distinto en cada ambiente** |
| `CORREO_SMTP_HOST`, `CORREO_SMTP_USUARIO`, `CORREO_SMTP_CLAVE` | SecureString | Gmail con contraseña de aplicación |
| `GOOGLE_CLIENT_ID` | SecureString | el del cliente OAuth |
| `ORIGIN_VERIFY` | String | aleatorio; es el secreto que CloudFront le pone al balanceador |

`ORIGIN_VERIFY` es `String` y no `SecureString` porque CloudFormation lo resuelve con `{{resolve:ssm:…}}`, y no se puede dar por hecho que `ssm-secure` funcione
en estas dos propiedades (regla del balanceador y encabezado de CloudFront). Es un candado entre CloudFront y el balanceador, no un secreto de usuarios.

```bash
AMB=dev            # dev | preprod | prod
P=/cundiapp/$AMB
aws ssm put-parameter --name $P/DB_URL      --type SecureString --value 'jdbc:postgresql://aws-0-REGION.pooler.supabase.com:5432/postgres?sslmode=require'
aws ssm put-parameter --name $P/DB_USERNAME --type SecureString --value 'postgres.<ref>'
aws ssm put-parameter --name $P/DB_PASSWORD --type SecureString --value '<clave>'
aws ssm put-parameter --name $P/JWT_SECRETO --type SecureString --value "$(openssl rand -base64 48)"
aws ssm put-parameter --name $P/ORIGIN_VERIFY --type String     --value "$(openssl rand -hex 32)"
# CORREO_SMTP_HOST, CORREO_SMTP_USUARIO, CORREO_SMTP_CLAVE y GOOGLE_CLIENT_ID: igual que arriba.
```

Variables **no secretas** de cada Environment de GitHub, en los dos repositorios:

| Variable | De dónde sale |
|---|---|
| `AWS_ROLE_ARN` | salida `RolDeDespliegueBackendArn` (en el repo Backend) o `RolDeDespliegueFrontendArn` (en el Frontend) de `entorno.yml` |
| `AWS_REGION` | la región elegida |
| `VPC_ID`, `SUBNET_IDS` | solo Backend. `SUBNET_IDS` separadas por coma, al menos dos zonas |
| `CLOUDFRONT_PREFIX_LIST_ID` | solo Backend, ver §5 paso 4 |

A nivel de repositorio: `JIRA_BASE_URL` y `JIRA_USER_EMAIL` (variables) y `JIRA_API_TOKEN` (secreto).

### Supabase: tres proyectos y las migraciones

```bash
supabase login
supabase orgs list
for AMB in dev preprod prod; do
  supabase projects create "cundiapp-$AMB" --org-id <ORG_ID> --region <REGION> --db-password "$(openssl rand -base64 24)"
done
supabase projects list        # anota el <ref> de cada uno
```

**Las migraciones las gobierna Flyway, no la CLI de Supabase.** Tener las dos escribiendo el esquema dejaría dos
historiales que se contradicen. Flyway corre solo cuando arranca la aplicación (con `ddl-auto=validate`). Para mirar o
ejecutar a mano contra cualquiera de los tres, con la **conexión por Session pooler** (puerto 5432, nunca el 6543, que
rompe las sentencias preparadas de Hibernate):

```bash
AMB=preprod
export DB_URL="jdbc:postgresql://aws-0-REGION.pooler.supabase.com:5432/postgres?sslmode=require"
export DB_USERNAME="postgres.<ref>"; export DB_PASSWORD="<clave>"     # o leerlos: aws ssm get-parameter --with-decryption

docker run --rm -v "$PWD/src/main/resources/db/migration:/flyway/sql:ro" flyway/flyway:11 \
  -url="$DB_URL" -user="$DB_USERNAME" -password="$DB_PASSWORD" \
  -schemas=cundiapp -defaultSchema=cundiapp -locations=filesystem:/flyway/sql \
  info          # también: validate  |  migrate
```

Verificado contra la base local: lista las 8 migraciones aplicadas. En Windows con Git Bash anteponer
`MSYS_NO_PATHCONV=1` y usar `$(pwd -W)`.

## 4. Imágenes

Dos Dockerfiles de dos etapas, ambos sin root:

| | Backend | Frontend |
|---|---|---|
| Compila con | `eclipse-temurin:21-jdk-alpine` | `node:24-alpine` |
| Ejecuta en | `eclipse-temurin:21-jre-alpine`, usuario `cundiapp` (uid 100) | `nginx-unprivileged:alpine`, usuario 101 |
| Tamaño | 428 MB | 92 MB |
| Capas | dependencias · loader · snapshot · aplicación | `package.json` primero, luego el código |
| Sonda | `wget …/actuator/health/readiness` | `wget …/salud` |

El frontend en **AWS no usa su imagen**: sale de S3 detrás de CloudFront. La imagen existe para Compose y como
alternativa si algún día se prefiere ECS.

## 5. Despliegue en AWS, paso a paso

Requiere AWS CLI v2 con un perfil de administrador. Una sola cuenta con tres ambientes; si el equipo prefiere aislar
producción, es otra cuenta y se repite todo (el ADR lo discute).

```bash
export AWS_REGION=<REGION>

# 1. Una vez por cuenta: proveedor OIDC de GitHub y repositorio ECR
aws cloudformation deploy --stack-name cundiapp-compartido --template-file infra/aws/compartido.yml
#    Si la cuenta ya tiene el proveedor de GitHub:  --parameter-overrides CrearProveedorOidc=false

# 2. Una vez por ambiente: roles y grupo de logs
aws cloudformation deploy --stack-name cundiapp-entorno-dev --template-file infra/aws/entorno.yml \
  --capabilities CAPABILITY_NAMED_IAM --parameter-overrides Entorno=dev EntornoGitHub=desarrollo
#    preprod -> EntornoGitHub=preproduccion      prod -> EntornoGitHub=produccion
aws cloudformation describe-stacks --stack-name cundiapp-entorno-dev --query 'Stacks[0].Outputs' --output table

# 3. Los parámetros de §3 (secretos y ORIGIN_VERIFY)

# 4. Datos de red y Environments de GitHub
aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text
aws ec2 describe-subnets --filters Name=vpc-id,Values=<VPC_ID> --query 'Subnets[].[SubnetId,AvailabilityZone]' --output table
aws ec2 describe-managed-prefix-lists --filters Name=prefix-list-name,Values=com.amazonaws.global.cloudfront.origin-facing \
  --query 'PrefixLists[0].PrefixListId' --output text
#    En GitHub (cada repositorio): Settings > Environments > crear desarrollo, preproduccion y produccion.
#    En produccion: Required reviewers = Naomi, y marcar "Prevent self-review".
for R in Backend Frontend; do
  gh variable set AWS_REGION --env desarrollo --repo crst11/$R --body "$AWS_REGION"
  gh variable set AWS_ROLE_ARN --env desarrollo --repo crst11/$R --body "<arn del paso 2 para $R>"
done
gh secret set JIRA_API_TOKEN --repo crst11/Backend; gh secret set JIRA_API_TOKEN --repo crst11/Frontend
#    Repetir las variables en preproduccion y produccion. Backend además: VPC_ID, SUBNET_IDS, CLOUDFRONT_PREFIX_LIST_ID.

# 5. Primer despliegue del backend: push a desarrollo, o a mano
gh workflow run desplegar.yml --ref desarrollo --repo crst11/Backend

# 6. Una vez por ambiente: el frontend (necesita el DNS del balanceador del paso 5)
DNS=$(aws cloudformation describe-stacks --stack-name cundiapp-backend-dev \
  --query "Stacks[0].Outputs[?OutputKey=='DnsDelBalanceador'].OutputValue" --output text)
aws cloudformation deploy --stack-name cundiapp-frontend-dev --template-file infra/aws/frontend.yml \
  --parameter-overrides Entorno=dev DnsDelBalanceador="$DNS"

# 7. Publicar los archivos del frontend
gh workflow run desplegar.yml --ref desarrollo --repo crst11/Frontend

# 8. Segunda pasada del backend: ahora CORS_ORIGENES recibe el dominio de CloudFront
gh workflow run desplegar.yml --ref desarrollo --repo crst11/Backend

# 9. Google: agregar https://<DominioDeCloudFront> a los orígenes autorizados del cliente OAuth
aws cloudformation describe-stacks --stack-name cundiapp-frontend-dev \
  --query "Stacks[0].Outputs[?OutputKey=='DominioDeCloudFront'].OutputValue" --output text
```

Por qué dos pasadas del backend: Spring compara el `Origin` del navegador con el `Host` que le llega, y detrás de
CloudFront no coinciden, así que el dominio de CloudFront tiene que estar en `CORS_ORIGENES`. Pero ese dominio no existe
hasta crear el frontend, y el frontend necesita el DNS del balanceador. Después de la primera vez todo corre en una pasada.

### Qué hace el flujo

| Evento | Trabajos |
|---|---|
| PR hacia cualquier rama de ambiente | solo `ci.yml` (compilar, probar, ArchUnit, JaCoCo). **No despliega.** |
| Push a `desarrollo` | `./mvnw verify` → imagen `dev-<sha>` → pila → comprobación → comentario en Jira |
| Push a `preproduccion` | imagen `preprod-<sha>` → pila → comprobación → `newman` (carpeta de la guía) → comentario en Jira |
| Push a `produccion` | **aprobación manual** → imagen `prod-<ver\|sha>` → pila (2 tareas) → comprobación → Jira: cerrar |

El despliegue es el de ECS: sube la tarea nueva (hasta 200 %), espera a que esté sana y solo entonces baja la vieja
(nunca menos de 100 %). Si la nueva no queda sana, el interruptor de ECS la revierte y CloudFormation falla.
Spring recibe SIGTERM, deja de aceptar peticiones (`readiness` pasa a "no acepta tráfico") y termina las que lleva
(25 s; `StopTimeout` de ECS 35 s). Probado con la imagen real: exit 143 tras "Graceful shutdown complete".

**Migraciones y el despliegue gradual:** durante unos minutos la versión vieja y la nueva usan la misma base. Una
migración que borra o renombra una columna que la versión vieja todavía lee rompe el despliegue. Regla: expandir
primero (agregar), desplegar, y recién en un despliegue posterior contraer (quitar).

## 6. Comprobaciones

```bash
AMB=prod; DOM=<DominioDeCloudFront>
curl -fsS "https://$DOM/api/publico/guia/categorias" -o /dev/null && echo OK        # CloudFront > ALB > tarea > Supabase
curl -s -o /dev/null -w '%{http_code}\n' "https://$DOM/api/mis/cuenta"               # 401
aws ecs describe-services --cluster cundiapp-$AMB --services backend \
  --query 'services[0].[desiredCount,runningCount,deployments[].[status,rolloutState]]'
TG=$(aws cloudformation describe-stack-resources --stack-name cundiapp-backend-$AMB \
  --logical-resource-id GrupoDeDestino --query 'StackResources[0].PhysicalResourceId' --output text)
aws elbv2 describe-target-health --target-group-arn "$TG" --query 'TargetHealthDescriptions[].[Target.Id,TargetHealth.State]'
aws logs tail /cundiapp/$AMB/backend --since 10m --follow
```

`/actuator/health/readiness` solo responde dentro de la red: CloudFront solo reenvía `/api/*`. La comprobación pública
es la de arriba, que atraviesa toda la cadena.

## 7. Volver atrás

**Backend.** Los despliegues fallidos se revierten solos. Para volver a una versión que sí se desplegó:

```bash
gh workflow run desplegar.yml --ref produccion --repo crst11/Backend -f etiqueta=prod-1.1.0    # no reconstruye nada
```

Sin pasar por CI (emergencia; la siguiente ejecución del flujo la sobrescribe):

```bash
aws ecs update-service --cluster cundiapp-prod --service backend --task-definition cundiapp-prod-backend:<revisión-anterior>
aws ecs describe-task-definition --task-definition cundiapp-prod-backend:<n> --query 'taskDefinition.containerDefinitions[0].image'
```

**Frontend.** El bucket tiene versiones. Lo más simple es relanzar el flujo sobre el commit anterior
(`gh run rerun <id>` del despliegue bueno). Los archivos con hash no cambian y `index.html` vuelve a apuntar a los viejos.

**Esquema de Supabase.** Flyway Community no deshace migraciones, y no hay un botón. Dos caminos:

1. *Sin pérdida de datos:* una migración nueva que revierta (`V<n+1>__revertir_…sql`), y desplegar. Es el camino normal.
2. *Con pérdida de datos:* restaurar el respaldo. El historial de Flyway vive dentro del esquema `cundiapp`, así que
   restaurar el esquema restaura también su historial.

```bash
# Respaldo ANTES de desplegar a producción una versión con migración (verifica qué respaldos incluye el plan de Supabase contratado: no asumas que hay)
docker run --rm -e PGPASSWORD="$DB_PASSWORD" public.ecr.aws/docker/library/postgres:16-alpine \
  pg_dump -h aws-0-REGION.pooler.supabase.com -p 5432 -U "$DB_USERNAME" -d postgres -n cundiapp -Fc > "respaldo-$(date +%F).dump"

# Restaurar (con la aplicación detenida: aws ecs update-service --cluster cundiapp-prod --service backend --desired-count 0)
docker run --rm -i -e PGPASSWORD="$DB_PASSWORD" public.ecr.aws/docker/library/postgres:16-alpine \
  pg_restore -h aws-0-REGION.pooler.supabase.com -p 5432 -U "$DB_USERNAME" -d postgres --clean --if-exists -n cundiapp < respaldo.dump
```

Sin probar: el respaldo y la restauración nunca se han ejecutado contra Supabase. Hacerlo una vez en Preprod antes de necesitarlo.

## 8. Costos aproximados

Estimación con precios públicos de lista de us-east-1, **sin verificar contra la calculadora de AWS**; revisarla antes de
crear nada. Por ambiente y por mes:

| Concepto | Aprox. (USD) |
|---|---|
| Balanceador de carga (hora + IPv4 públicas de sus dos nodos) | 22–25 |
| Fargate 0,5 vCPU / 1 GB, una tarea, con su IPv4 pública | 21–22 |
| CloudFront, S3, ECR, CloudWatch, Parameter Store | 1–5 con poco tráfico |
| **Total por ambiente** | **≈ 45–50** |

Tres ambientes ≈ 135–150 al mes, y producción con dos tareas suma otros ≈ 22. Es la decisión que el ADR 0005 había
descartado ("exige decidir y pagar hosting"). Formas de bajarlo:

- Apagar DEV fuera de horario: `aws ecs update-service --cluster cundiapp-dev --service backend --desired-count 0` (el balanceador sigue cobrando).
- No tener DEV en la nube y usar `docker compose --profile app up`.
- Una **alarma de presupuesto** desde el primer día: `aws budgets create-budget` o la consola de AWS Budgets.

## 9. Lo que no está resuelto

- **Sin dominio propio.** El balanceador habla http con CloudFront; el candado es la lista de prefijos más `X-Origin-Verify`,
  pero el tramo CloudFront → balanceador no va cifrado. Con un dominio y un certificado de ACM se pasa a https y se quita la excepción.
- **Sin WAF.** Los límites por IP de la aplicación son la única defensa contra abuso. AWS WAF cuesta aparte.
- **Sin CSP.** CloudFront pone HSTS, `nosniff`, `X-Frame-Options` y `Referrer-Policy`; una Content-Security-Policy para
  Angular + Google Identity + SweetAlert hay que escribirla y probarla en un navegador.
- **Raíz del contenedor con escritura.** Fargate no tiene `tmpfs`, y marcarla de solo lectura sin un volumen para `/tmp` rompe Tomcat.
- **Una imagen por rama.** Al fusionar `desarrollo` en `preproduccion` el commit de fusión tiene otro hash, así que se
  reconstruye en vez de promover los mismos bytes. Promover la misma imagen exigiría etiquetar por el hash del commit original.
- **Sin pruebas de las plantillas en una cuenta real.** `cfn-lint` valida la sintaxis y las propiedades, no los permisos
  ni los límites de la cuenta.
