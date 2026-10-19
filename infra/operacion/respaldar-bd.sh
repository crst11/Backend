#!/usr/bin/env bash
# Respalda el esquema cundiapp de la base de DB_URL en un archivo (12 factores, factor 12, SCRUM-219). Solo LEE la base.
#
#   DB_URL=... DB_USERNAME=... DB_PASSWORD=... infra/operacion/respaldar-bd.sh [archivo.dump]
#
# Sin argumento escribe respaldo-cundiapp-AAAA-MM-DDThhmmss.dump en la carpeta actual. El archivo queda en formato custom de
# pg_dump (se restaura con restaurar-bd.sh) e incluye flyway_schema_history, que vive dentro del esquema: restaurarlo devuelve
# los datos y la version del esquema juntos.
#
# Los contadores de limite de uso (limite_de_uso, UNLOGGED, viven minutos) no se respaldan: no valen nada pasado un rato.
# El respaldo trae datos personales de estudiantes: guardalo fuera del repositorio y borralo cuando ya no haga falta.
set -euo pipefail
# shellcheck source=comun.sh
source "$(dirname "$0")/comun.sh"

leer_conexion
elegir_imagen_pg

destino="${1:-respaldo-cundiapp-$(date +%Y-%m-%dT%H%M%S).dump}"
[[ ! -e "$destino" ]] || fallar "$destino ya existe; no lo piso."
parcial="$destino.parcial"
trap 'rm -f "$parcial"' EXIT

herramienta_pg pg_dump -h "$(host_para_docker)" -p "$BD_PUERTO" -U "$DB_USERNAME" -d "$BD_BASE" \
  --schema=cundiapp --format=custom --no-owner --no-privileges --no-unlogged-table-data > "$parcial"

[[ -s "$parcial" ]] || fallar "pg_dump no escribio nada."
# Un respaldo que no se puede leer no sirve: se comprueba el indice antes de darlo por bueno.
herramienta_pg -i pg_restore --list < "$parcial" > /dev/null || fallar "el respaldo no se puede leer (pg_restore --list fallo)."

mv "$parcial" "$destino"
echo "Respaldo listo: $destino ($(wc -c < "$destino") bytes, herramientas $IMAGEN_PG)"
