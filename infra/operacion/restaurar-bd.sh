#!/usr/bin/env bash
# Restaura un respaldo en la base de DB_URL (12 factores, factor 12, SCRUM-219). DESTRUCTIVO: reemplaza el esquema cundiapp.
#
#   CONFIRMO_RESTAURAR_EN=<servidor de DB_URL> DB_URL=... DB_USERNAME=... DB_PASSWORD=... \
#     infra/operacion/restaurar-bd.sh respaldo.dump
#
# Salvaguardas:
#   - CONFIRMO_RESTAURAR_EN tiene que ser igual al servidor de DB_URL. Obliga a leer a que base se apunta antes de escribir en ella
#     (preproduccion y produccion se parecen mucho).
#   - Antes de tocar nada saca un respaldo de lo que hay ahora (antes-de-restaurar-*.dump), para poder deshacer la restauracion.
#   - Reemplaza el esquema entero (lo que la base tenga y el respaldo no, tambien desaparece) en UNA transaccion que se detiene
#     en el primer error: si algo falla, la base queda como estaba.
#
# Detener el backend antes (docker compose ... stop backend en la instancia): una aplicacion escribiendo mientras se restaura
# deja la base a medias de verdad. El historial de Flyway viene dentro del respaldo, asi que despues de restaurar la base queda
# en la version del respaldo; si la imagen desplegada es mas nueva, el migrador del proximo despliegue aplica lo que falte.
set -euo pipefail
# shellcheck source=comun.sh
source "$(dirname "$0")/comun.sh"

respaldo="${1:-}"
[[ -n "$respaldo" && -f "$respaldo" ]] || fallar "uso: restaurar-bd.sh respaldo.dump (el archivo tiene que existir)."

leer_conexion
[[ "${CONFIRMO_RESTAURAR_EN:-}" == "$BD_HOST" ]] \
  || fallar "esto reemplaza el esquema cundiapp en $BD_HOST. Para continuar: CONFIRMO_RESTAURAR_EN=$BD_HOST"

elegir_imagen_pg
herramienta_pg -i pg_restore --list < "$respaldo" > /dev/null || fallar "$respaldo no es un respaldo legible."

previo="antes-de-restaurar-$(date +%Y-%m-%dT%H%M%S).dump"
hay_esquema="$(herramienta_pg psql -h "$(host_para_docker)" -p "$BD_PUERTO" -U "$DB_USERNAME" -d "$BD_BASE" -tAc \
  "select count(*) from information_schema.schemata where schema_name='cundiapp'")"
if [[ "$hay_esquema" == "0" ]]; then
  # Una base nueva (un proyecto de Supabase recien creado tras un desastre): no hay nada que perder ni que respaldar.
  previo="(no hay esquema cundiapp en $BD_HOST: nada que respaldar antes)"
  echo "1/2 $previo"
else
  echo "1/2 Respaldo de lo que hay ahora en $BD_HOST -> $previo"
  IMAGEN_PG="$IMAGEN_PG" bash "$(dirname "$0")/respaldar-bd.sh" "$previo"
fi

echo "2/2 Restaurando $respaldo (una sola transaccion)"
# Se suelta el esquema entero (y con el, lo que la base tenga y el respaldo no) y se recrea desde el respaldo. pg_restore --clean no
# alcanza: solo borra lo que viene en el respaldo y una tabla ajena impide soltar el esquema. psql -1 + ON_ERROR_STOP = todo o nada.
# El SQL se genera ENTERO antes de tocar la base: si pg_restore muriera a mitad (respaldo danado por dentro), psql recibiria una
# entrada truncada, llegaria al final sin error y confirmaria una restauracion a medias.
sql="$(mktemp)"
trap 'rm -f "$sql"' EXIT
herramienta_pg -i pg_restore --no-owner --no-privileges -f - < "$respaldo" > "$sql" \
  || fallar "no pude leer $respaldo hasta el final (danado por dentro). No toque la base."
{
  echo "DROP SCHEMA IF EXISTS cundiapp CASCADE;"
  cat "$sql"
} | herramienta_pg -i psql -h "$(host_para_docker)" -p "$BD_PUERTO" -U "$DB_USERNAME" -d "$BD_BASE" \
  -q -1 -v ON_ERROR_STOP=1 > /dev/null \
  || fallar "la restauracion fallo y se deshizo: la base sigue como estaba. Respaldo previo: $previo"

version="$(herramienta_pg psql -h "$(host_para_docker)" -p "$BD_PUERTO" -U "$DB_USERNAME" -d "$BD_BASE" -tAc \
  "select max(version::int) from cundiapp.flyway_schema_history where success")"
echo "Restaurado. Version del esquema (Flyway): $version."
[[ "$hay_esquema" == "0" ]] || echo "Para deshacer: restaurar-bd.sh $previo"
