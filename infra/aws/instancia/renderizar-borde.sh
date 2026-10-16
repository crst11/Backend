#!/bin/bash
# Arma la configuracion de la nginx de borde de UN ambiente a partir de borde.conf.template (SCRUM-71).
#
#   renderizar-borde.sh <puerto-interno> <secreto> <contenedor-destino>
#
# La escribe por la salida estandar. La usan desplegar.sh (en la instancia) y el CI, para que las dos
# armen la configuracion con EXACTAMENTE el mismo reemplazo: antes cada marcador se reemplazaba con un
# sed sin la opcion "g", y la linea de limit_req_zone, que lo lleva dos veces, salia con un nombre de
# zona a medias y nginx no arrancaba (SCRUM-101).
set -euo pipefail

PUERTO="${1:?uso: renderizar-borde.sh <puerto-interno> <secreto> <contenedor-destino>}"
SECRETO="${2:?falta el secreto}"
DESTINO="${3:?falta el contenedor destino}"
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Los tres valores van tal cual dentro de un sed: se exige una forma que no pueda romperlo (el secreto
# ya se valida en desplegar.sh, aqui tambien porque el CI lo llama sin pasar por alli).
[[ "$PUERTO" =~ ^[0-9]{2,5}$ ]] || { echo "Puerto invalido: $PUERTO" >&2; exit 2; }
[[ "$SECRETO" =~ ^[A-Za-z0-9]{32,}$ ]] || { echo "El secreto debe tener 32 o mas letras y numeros" >&2; exit 2; }
[[ "$DESTINO" =~ ^[A-Za-z0-9_-]+$ ]] || { echo "Destino invalido: $DESTINO" >&2; exit 2; }

salida="$(sed -e 's/\r$//' -e "s/__PUERTO_INTERNO__/$PUERTO/g" -e "s/__SECRETO__/$SECRETO/g" -e "s/__DESTINO__/$DESTINO/g" \
  "$AQUI/borde.conf.template")"

# Si quedo un marcador sin reemplazar, la configuracion no sirve: mejor fallar aqui, con el motivo claro.
if grep -q '__[A-Z_]*__' <<<"$salida"; then
  echo "Quedaron marcadores sin reemplazar en la configuracion del borde:" >&2
  grep -n '__[A-Z_]*__' <<<"$salida" >&2
  exit 1
fi
printf '%s\n' "$salida"
