#!/usr/bin/env bash
# Corre Flyway a mano contra la base de DB_URL (12 factores, factor 12, SCRUM-219). Es el mismo Flyway y las mismas migraciones
# que la aplicacion: el despliegue las aplica solo (servicio migrador, SCRUM-217); esto es para mirar o corregir a mano.
#
#   DB_URL=... DB_USERNAME=... DB_PASSWORD=... infra/operacion/migrar-a-mano.sh info       que hay aplicado y que falta
#   DB_URL=... DB_USERNAME=... DB_PASSWORD=... infra/operacion/migrar-a-mano.sh validate   que lo aplicado coincida con los archivos
#   CONFIRMO_MIGRAR_EN=<servidor> DB_URL=... ... infra/operacion/migrar-a-mano.sh migrate  aplica lo pendiente (ESCRIBE)
#
# info y validate solo leen. migrate escribe, asi que exige CONFIRMO_MIGRAR_EN igual al servidor de DB_URL.
# Flyway Community no deshace migraciones: para volver atras se escribe una migracion nueva o se restaura un respaldo.
set -euo pipefail
# shellcheck source=comun.sh
source "$(dirname "$0")/comun.sh"

IMAGEN_FLYWAY="${IMAGEN_FLYWAY:-flyway/flyway:11}"
orden="${1:-}"
case "$orden" in
  info | validate | migrate) ;;
  *) fallar "uso: migrar-a-mano.sh info|validate|migrate" ;;
esac

leer_conexion
if [[ "$orden" == "migrate" && "${CONFIRMO_MIGRAR_EN:-}" != "$BD_HOST" ]]; then
  fallar "migrate escribe en $BD_HOST. Para continuar: CONFIRMO_MIGRAR_EN=$BD_HOST"
fi

raiz="$(cd "$(dirname "$0")/../.." && pwd)"
# En Windows con Git Bash, Docker necesita la ruta de Windows para el volumen.
carpeta="$(cd "$raiz/src/main/resources/db/migration" && (pwd -W 2>/dev/null || pwd))"

# La URL se rearma con el servidor visto desde el contenedor; el usuario y la clave van por el entorno, no por la linea de comandos.
url="jdbc:postgresql://$(host_para_docker):${BD_PUERTO}/${BD_BASE}?sslmode=${BD_SSL}"
docker run --rm --add-host=host.docker.internal:host-gateway -v "$carpeta:/flyway/sql:ro" \
  -e FLYWAY_URL="$url" -e FLYWAY_USER="$DB_USERNAME" -e FLYWAY_PASSWORD="$DB_PASSWORD" \
  -e FLYWAY_SCHEMAS=cundiapp -e FLYWAY_DEFAULT_SCHEMA=cundiapp -e FLYWAY_LOCATIONS=filesystem:/flyway/sql \
  "$IMAGEN_FLYWAY" "$orden"
