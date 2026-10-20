#!/bin/bash
# Despliega el backend de UN ambiente en esta instancia (SCRUM-71). Lo ejecuta el CI con SSM Run Command.
#
#   desplegar.sh <preprod|prod> <imagen-de-ecr> <region>
#
# La SALIDA de este script queda en el historial de SSM y el CI la imprime en el log de GitHub, asi que
# NUNCA imprime el valor de un secreto: solo nombres. Por eso no hay "set -x" ni "env".
set -euo pipefail

ENT="${1:?uso: desplegar.sh <preprod|prod> <imagen> <region>}"
IMAGEN="${2:?falta la imagen}"
REGION="${3:?falta la region}"

case "$ENT" in
  # El puerto interno es donde escucha la nginx de borde; CloudFront entra por el 80 (prod) o el 8081 (preprod).
  prod)    PERFIL=prod; PUERTO_INTERNO=8080; OOM_SCORE_ADJ=-300; CPU_SHARES=1024 ;;
  preprod) PERFIL=pre; PUERTO_INTERNO=8081; OOM_SCORE_ADJ=300; CPU_SHARES=512 ;;
  *) echo "Ambiente invalido: $ENT (solo preprod o prod)"; exit 2 ;;
esac

RAIZ=/opt/cundiapp
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIR="$RAIZ/$ENT"
export AWS_DEFAULT_REGION="$REGION"
mkdir -p "$DIR" "$RAIZ/borde/conf.d"

# ---- 1. Secretos: de Parameter Store al entorno de este proceso ------------------------------------------
REQUERIDOS=(DB_URL DB_USERNAME DB_PASSWORD JWT_SECRETO CORREO_SMTP_HOST CORREO_SMTP_USUARIO CORREO_SMTP_CLAVE GOOGLE_CLIENT_ID CORS_ORIGENES ORIGIN_VERIFY)
while IFS=$'\t' read -r nombre valor; do
  export "${nombre##*/}=$valor"
done < <(aws ssm get-parameters-by-path --path "/cundiapp/$ENT/" --with-decryption \
          --query 'Parameters[].[Name,Value]' --output text)

faltan=()
for nombre in "${REQUERIDOS[@]}"; do
  [ -n "${!nombre:-}" ] || faltan+=("$nombre")
done
if [ "${#faltan[@]}" -gt 0 ]; then
  echo "Faltan parametros en /cundiapp/$ENT/: ${faltan[*]}"
  exit 3
fi
# El secreto se pega en la configuracion de nginx: solo letras y numeros, y largo. Sin esto, uno vacio o con
# comillas dejaria la puerta abierta o rota.
if ! [[ "$ORIGIN_VERIFY" =~ ^[A-Za-z0-9]{32,}$ ]]; then
  echo "ORIGIN_VERIFY debe tener 32 o mas letras y numeros (openssl rand -hex 32)"
  exit 3
fi

# ---- 2. Imagen --------------------------------------------------------------------------------------------
# El token de ECR vale 12 horas: se guarda en una carpeta temporal que se borra al terminar, y no en
# /root/.docker/config.json, donde se quedaria sin cifrar. El complemento de Compose vive en una ruta del
# sistema, asi que cambiar la carpeta de configuracion no lo afecta.
DOCKER_CONFIG="$(mktemp -d)"
export DOCKER_CONFIG
trap 'rm -rf "$DOCKER_CONFIG"' EXIT
REGISTRO="${IMAGEN%%/*}"
aws ecr get-login-password | docker login --username AWS --password-stdin "$REGISTRO" >/dev/null

# shellcheck source=relevo.sh
source "$AQUI/relevo.sh"
# La imagen que corre ahora (para decirlo si el despliegue nuevo no queda sano). Se busca por etiquetas de Compose: el contenedor ya
# no tiene un nombre fijo (SCRUM-229).
ANTERIOR=""
for id in $(contenedores_de "cundiapp-$ENT" backend); do
  ANTERIOR="$(docker inspect -f '{{.Config.Image}}' "$id" 2>/dev/null || true)"
done
cp "$AQUI/compose-backend.yml" "$DIR/compose.yml"
export IMAGEN PERFIL ENTORNO="$ENT" OOM_SCORE_ADJ CPU_SHARES

# Registros en CloudWatch Logs (SCRUM-222). Solo si el rol de la instancia ya puede escribir en el grupo: la plantilla
# servidor.yml se aplica a mano y aparte, y un despliegue con awslogs sin ese permiso dejaria el backend sin arrancar.
# Sin permiso, el contenedor sigue registrando en el disco (json-file) y el despliegue lo dice.
# shellcheck source=registros.sh
source "$AQUI/registros.sh"
ARCHIVOS_COMPOSE=(-f "$DIR/compose.yml")
export GRUPO_DE_REGISTROS="/cundiapp/$ENT/backend"
if cloudwatch_alcanzable "$GRUPO_DE_REGISTROS"; then
  cp "$AQUI/compose-backend.cloudwatch.yml" "$DIR/compose.cloudwatch.yml"
  ARCHIVOS_COMPOSE+=(-f "$DIR/compose.cloudwatch.yml")
  echo "Registros del backend: CloudWatch Logs ($GRUPO_DE_REGISTROS)"
else
  echo "AVISO: no puedo escribir en CloudWatch Logs ($GRUPO_DE_REGISTROS); el backend registra solo en el disco. Aplica infra/aws/servidor.yml (docs/despliegue.md, seccion 10)."
fi
compose() { docker compose -p "cundiapp-$ENT" "${ARCHIVOS_COMPOSE[@]}" "$@"; }

# ---- 3. Migrar el esquema, ANTES de tocar lo que esta sirviendo (SCRUM-217) -------------------------------
# Las migraciones corren en un contenedor de una sola ejecucion con la imagen NUEVA. Antes corrian dentro del arranque del
# backend nuevo: una migracion que fallaba lo dejaba en un bucle de reinicios, ya sin el anterior. Ahora, si falla, el
# despliegue se aborta aqui y el backend vigente sigue sirviendo sin enterarse.
#
# Ojo con la vuelta atras: si la migracion SALIO bien y el backend nuevo no queda sano, el script vuelve a la imagen
# anterior sobre un esquema ya migrado. Por eso cada migracion tiene que ser compatible con la version anterior
# (agregar antes de quitar; ver docs/despliegue.md, seccion de migraciones).
echo "Desplegando $ENT: $IMAGEN"
compose pull --quiet
echo "Migrando el esquema de $ENT con la imagen nueva"
SALIDA_MIGRACION="$(mktemp)"
trap 'rm -rf "$DOCKER_CONFIG" "$SALIDA_MIGRACION"' EXIT
if compose run --rm --no-deps -T migrador >"$SALIDA_MIGRACION" 2>&1; then
  # En preproduccion y produccion el registro sale en JSON (ECS): se imprimen solo los mensajes que importan.
  grep -aoE '"message":"(Successfully applied|Schema .{0,60}is up to date|Migraciones al d)[^"]*' "$SALIDA_MIGRACION" \
    | sed -E 's#^"message":"##' || true
else
  echo "La migracion FALLO. No se toco el backend que esta sirviendo. Ultimas lineas (sin secretos):"
  tail -n 40 "$SALIDA_MIGRACION" | sed -E 's#(password|pwd|secret|token)=[^& ]+#\1=***#Ig'
  exit 4
fi

# ---- 4. Relevo sin caida: el nuevo corre junto al viejo y solo despues se retira el viejo (SCRUM-229) -------------------------
# Antes: "compose up -d" detenia el viejo y luego arrancaba el nuevo, y mientras tanto la nginx daba 502 (medido: 15 s por despliegue).
# Ahora el contenedor nuevo arranca al lado del viejo, con el mismo alias de red; cuando esta sano se retira el viejo. Si no queda sano,
# se retira EL NUEVO y el viejo sigue sirviendo: un despliegue fallido tampoco causa caida (relevo.sh).
if ! relevar_servicio "cundiapp-$ENT" backend; then
  if [ "$RELEVO_HABIA_ANTERIOR" -eq 1 ]; then
    echo "El backend anterior (${ANTERIOR:-sin imagen conocida}) sigue sirviendo: no hubo caida."
  else
    echo "No habia un backend anterior."
  fi
  exit 1
fi
echo "Sano: $(docker inspect -f '{{.Name}}' "$RELEVO_NUEVO" | tr -d /)"

# ---- 5. Puerta de entrada (nginx) -------------------------------------------------------------------------
# Se abre DESPUES de que el backend este sano: asi CloudFront nunca recibe trafico de un backend a medias.
#
# Esta nginx la comparten preproduccion y produccion: una configuracion invalida de un ambiente impediria
# que arranque tambien la del otro la proxima vez que el contenedor se reinicie. Por eso la nueva se
# comprueba con "nginx -t" ANTES de darla por buena, y si no pasa se restaura la anterior (SCRUM-101).
CONF="$RAIZ/borde/conf.d/$ENT.conf"
if [ -f "$CONF" ]; then cp -p "$CONF" "$CONF.anterior"; else rm -f "$CONF.anterior"; fi
bash "$AQUI/renderizar-borde.sh" "$PUERTO_INTERNO" "$ORIGIN_VERIFY" "backend-$ENT" > "$CONF"
# Lleva el secreto: solo la lee el usuario de nginx (uid 101 en la imagen nginx-unprivileged), nadie mas.
chown 101:101 "$CONF"
chmod 400 "$CONF"
cp "$AQUI/compose-borde.yml" "$RAIZ/borde/compose.yml"

restaurar_borde() {
  if [ -f "$CONF.anterior" ]; then mv -f "$CONF.anterior" "$CONF"; else rm -f "$CONF"; fi
}

# Si la nginx todavia no existe o no arranca con la configuracion nueva, tambien se restaura.
ARCHIVOS_BORDE=(-f "$RAIZ/borde/compose.yml")
if cloudwatch_alcanzable /cundiapp/borde; then
  cp "$AQUI/compose-borde.cloudwatch.yml" "$RAIZ/borde/compose.cloudwatch.yml"
  ARCHIVOS_BORDE+=(-f "$RAIZ/borde/compose.cloudwatch.yml")
else
  echo "AVISO: no puedo escribir en CloudWatch Logs (/cundiapp/borde); la puerta de entrada registra solo en el disco."
fi
docker compose -p cundiapp-borde "${ARCHIVOS_BORDE[@]}" up -d || true
if ! prueba="$(docker exec cundiapp-borde nginx -t 2>&1)"; then
  echo "La configuracion NUEVA de la puerta de entrada no es valida; se deja la anterior. Respuesta de nginx:"
  echo "$prueba"
  restaurar_borde
  exit 1
fi
docker exec cundiapp-borde nginx -s reload
rm -f "$CONF.anterior"

# ---- 6. Limpieza ------------------------------------------------------------------------------------------
# Las imagenes sin uso de hace mas de una semana. La anterior se conserva para volver atras, y si ya se
# borro se vuelve a bajar de ECR.
docker image prune -af --filter "until=168h" >/dev/null || true

echo "Listo: $ENT en $IMAGEN"
docker ps --format 'table {{.Names}}\t{{.Status}}' | grep -E 'cundiapp' || true
