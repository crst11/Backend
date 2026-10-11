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
  # El puerto interno es donde escucha la nginx de borde; CloudFront entra por el 80 (prod) o el 81 (preprod).
  prod)    PERFIL=prod; PUERTO_INTERNO=8080 ;;
  preprod) PERFIL=pre; PUERTO_INTERNO=8081 ;;
  *) echo "Ambiente invalido: $ENT (solo preprod o prod)"; exit 2 ;;
esac

RAIZ=/opt/cundiapp
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIR="$RAIZ/$ENT"
CONTENEDOR="cundiapp-$ENT-backend"
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

ANTERIOR="$(docker inspect -f '{{.Config.Image}}' "$CONTENEDOR" 2>/dev/null || true)"
cp "$AQUI/compose-backend.yml" "$DIR/compose.yml"
export IMAGEN PERFIL ENTORNO="$ENT"
compose() { docker compose -p "cundiapp-$ENT" -f "$DIR/compose.yml" "$@"; }

esperar_sano() { # devuelve 0 si el contenedor queda "healthy" antes de ~3 minutos
  local estado
  for _ in $(seq 1 90); do
    estado="$(docker inspect -f '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{end}}' "$CONTENEDOR" 2>/dev/null || echo ausente)"
    case "$estado" in
      *" healthy") return 0 ;;
      exited*|dead*|ausente) return 1 ;;
    esac
    sleep 2
  done
  return 1
}

registro_sin_secretos() {
  docker logs --tail 25 "$CONTENEDOR" 2>&1 | sed -E 's#(password|pwd|secret|token)=[^& ]+#\1=***#Ig' || true
}

# ---- 3. Desplegar, y volver a la imagen anterior si la nueva no queda sana --------------------------------
echo "Desplegando $ENT: $IMAGEN"
compose pull --quiet
compose up -d --remove-orphans

if ! esperar_sano; then
  echo "La imagen nueva NO quedo sana. Ultimas lineas del registro (sin secretos):"
  registro_sin_secretos
  if [ -n "$ANTERIOR" ] && [ "$ANTERIOR" != "$IMAGEN" ]; then
    echo "Volviendo a la imagen anterior: $ANTERIOR"
    IMAGEN="$ANTERIOR" compose up -d
    if esperar_sano; then echo "Volvio a la anterior y quedo sana."; else echo "La anterior tampoco quedo sana."; fi
  else
    echo "No hay imagen anterior a la cual volver."
  fi
  exit 1
fi
echo "Sano: $CONTENEDOR"

# ---- 4. Puerta de entrada (nginx) -------------------------------------------------------------------------
# Se abre DESPUES de que el backend este sano: asi CloudFront nunca recibe trafico de un backend a medias.
sed -e "s/__PUERTO_INTERNO__/$PUERTO_INTERNO/" -e "s/__SECRETO__/$ORIGIN_VERIFY/" -e "s/__DESTINO__/backend-$ENT/" \
    "$AQUI/borde.conf.template" > "$RAIZ/borde/conf.d/$ENT.conf"
# Lleva el secreto: solo la lee el usuario de nginx (uid 101 en la imagen nginx-unprivileged), nadie mas.
chown 101:101 "$RAIZ/borde/conf.d/$ENT.conf"
chmod 400 "$RAIZ/borde/conf.d/$ENT.conf"
cp "$AQUI/compose-borde.yml" "$RAIZ/borde/compose.yml"
docker compose -p cundiapp-borde -f "$RAIZ/borde/compose.yml" up -d
docker exec cundiapp-borde nginx -t >/dev/null 2>&1
docker exec cundiapp-borde nginx -s reload

# ---- 5. Limpieza ------------------------------------------------------------------------------------------
# Las imagenes sin uso de hace mas de una semana. La anterior se conserva para volver atras, y si ya se
# borro se vuelve a bajar de ECR.
docker image prune -af --filter "until=168h" >/dev/null || true

echo "Listo: $ENT en $IMAGEN"
docker ps --format 'table {{.Names}}\t{{.Status}}' | grep -E 'cundiapp' || true
