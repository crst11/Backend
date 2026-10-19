#!/usr/bin/env bash
# Dice, para el commit actual de produccion, que imagen de preproduccion se puede promover tal cual en vez de
# reconstruirla (12 factores, factor 5, SCRUM-215). Imprime su etiqueta (preprod-<sha7>) o no imprime nada.
#
# Una promocion a produccion es un commit de fusion cuyo segundo padre trae lo de preproduccion. La imagen que se probo
# en preproduccion se etiqueto con el commit de esa rama, asi que se recorre desde ese padre hacia atras mientras el
# CONTENIDO (el arbol de git) sea identico al de este commit, y se devuelve la primera para la que ECR tiene imagen.
# Eso cubre las dos formas de promover: la directa (el padre es la punta de preproduccion) y la rama de promocion que une
# la historia de produccion con una fusion "-s ours" (el padre es esa rama y su primer padre es la punta de preproduccion).
#
# Si el contenido es distinto (un conflicto resuelto a mano, un arreglo que solo esta en produccion), la imagen de
# preproduccion NO es lo que se va a desplegar y no se devuelve nada: se construye. Tampoco devuelve nada si el commit
# no es una fusion o si la imagen ya no esta en ECR (la politica conserva solo las 2 ultimas de cada ambiente).
#
# Necesita el historial completo (actions/checkout con fetch-depth: 0) y las credenciales de AWS del Environment.
set -uo pipefail

REPOSITORIO="${REPOSITORIO_ECR:-cundiapp-backend}"
REVISIONES_HACIA_ATRAS=10

padre="$(git rev-parse --verify --quiet 'HEAD^2' || true)"
[ -n "$padre" ] || exit 0
arbol="$(git rev-parse 'HEAD^{tree}')"

for commit in $(git rev-list --first-parent -n "$REVISIONES_HACIA_ATRAS" "$padre"); do
  [ "$(git rev-parse "${commit}^{tree}")" = "$arbol" ] || exit 0
  etiqueta="preprod-${commit:0:7}"
  if aws ecr describe-images --repository-name "$REPOSITORIO" --image-ids "imageTag=${etiqueta}" >/dev/null 2>&1; then
    echo "$etiqueta"
    exit 0
  fi
done
exit 0
