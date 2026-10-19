#!/usr/bin/env bash
# Prueba que el respaldo de una base se puede restaurar y que la copia es fiel (12 factores, factor 12, SCRUM-219).
#
#   DB_URL=... DB_USERNAME=... DB_PASSWORD=... infra/operacion/probar-respaldo.sh
#
# Contra la base de origen solo LEE (pg_dump y unos count). El respaldo se restaura en un PostgreSQL desechable que el script
# arranca y borra al terminar, asi que es seguro correrlo contra preproduccion y produccion. Despues compara, tabla por tabla,
# el numero de filas del origen y de la copia, y la version de Flyway.
#
# Una base viva cambia mientras se respalda. Por eso se cuenta antes y despues del respaldo: si una tabla no cambio, la copia
# tiene que tener exactamente esas filas; si cambio, la copia tiene que quedar dentro del rango y se avisa. Sale con 1 si algo
# no cuadra. limite_de_uso (UNLOGGED, contadores de minutos) no se respalda a proposito y no se compara.
set -euo pipefail
# shellcheck source=comun.sh
source "$(dirname "$0")/comun.sh"

leer_conexion
elegir_imagen_pg

carpeta="$(mktemp -d)"
contenedor="cundiapp-prueba-respaldo-$$"
trap 'docker rm -f "$contenedor" > /dev/null 2>&1 || true; rm -rf "$carpeta"' EXIT

# Filas por tabla del esquema, "tabla|filas", sin limite_de_uso. Una sola consulta que sirve igual en origen y copia.
CONSULTA_DE_FILAS="select table_name || '|' || (xpath('/row/c/text()', query_to_xml(format('select count(*) as c from cundiapp.%I', table_name), false, true, '')))[1]::text
  from information_schema.tables where table_schema='cundiapp' and table_type='BASE TABLE' and table_name <> 'limite_de_uso' order by 1"
CONSULTA_DE_VERSION="select max(version::int) from cundiapp.flyway_schema_history where success"

en_origen() {
  herramienta_pg psql -h "$(host_para_docker)" -p "$BD_PUERTO" -U "$DB_USERNAME" -d "$BD_BASE" -tAc "$1"
}
en_copia() {
  docker exec -i "$contenedor" psql -U postgres -d prueba -tAc "$1"
}

echo "1/4 Contando el origen ($BD_HOST) antes del respaldo"
en_origen "$CONSULTA_DE_FILAS" > "$carpeta/antes.txt"
version_origen="$(en_origen "$CONSULTA_DE_VERSION")"
[[ -s "$carpeta/antes.txt" ]] || fallar "el esquema cundiapp no tiene tablas en el origen."

echo "2/4 Respaldando"
bash "$(dirname "$0")/respaldar-bd.sh" "$carpeta/respaldo.dump"
en_origen "$CONSULTA_DE_FILAS" > "$carpeta/despues.txt"

echo "3/4 Restaurando en un PostgreSQL desechable ($IMAGEN_PG)"
docker run -d --rm --name "$contenedor" -e POSTGRES_PASSWORD="$(date +%s%N)desechable" -e POSTGRES_DB=prueba "$IMAGEN_PG" > /dev/null
for _ in $(seq 1 60); do
  docker exec "$contenedor" pg_isready -U postgres -d prueba > /dev/null 2>&1 && break
  sleep 1
done
docker exec "$contenedor" pg_isready -U postgres -d prueba > /dev/null 2>&1 || fallar "el PostgreSQL desechable no arranco."
# El contenedor dice "listo" una vez durante la inicializacion y otra al terminarla: se espera un poco mas.
sleep 2
docker exec -i "$contenedor" pg_restore -U postgres -d prueba --no-owner --no-privileges --single-transaction --exit-on-error \
  < "$carpeta/respaldo.dump" || fallar "pg_restore fallo: el respaldo NO se puede restaurar."

echo "4/4 Comparando"
en_copia "$CONSULTA_DE_FILAS" > "$carpeta/copia.txt"
version_copia="$(en_copia "$CONSULTA_DE_VERSION")"

resultado=0
awk -F'|' -f "$(dirname "$0")/comparar-filas.awk" "$carpeta/antes.txt" "$carpeta/despues.txt" "$carpeta/copia.txt" || resultado=1

if [[ "$version_origen" != "$version_copia" ]]; then
  echo "DIFIERE la version de Flyway: origen $version_origen, copia $version_copia"
  resultado=1
fi

if [[ "$resultado" -ne 0 ]]; then
  echo "El respaldo NO es fiel. No lo des por bueno." >&2
  exit 1
fi
echo "OK: el respaldo se restauro y coincide con el origen (esquema en la version $version_origen de Flyway)."
