#!/usr/bin/env bash
# Pone el ambiente de desarrollo (la pila local de Docker) al dia con lo ultimo mergeado en la rama "desarrollo" (SCRUM-270).
#
#   bash infra/actualizar-desarrollo.sh              actualiza Backend y Frontend, reconstruye y verifica
#   RUTA_FRONTEND=../Frontend bash infra/actualizar-desarrollo.sh
#
# Hace, en orden:
#   1. Se niega a seguir si algun repositorio tiene cambios sin guardar (los archivos nuevos sin seguir no cuentan) o no se puede avanzar limpio.
#   2. Cambia a "desarrollo" en los dos repositorios y los trae de origin (solo avance, sin merges).
#   3. Reconstruye backend y frontend con el commit del Frontend como VERSION.
#   4. Espera a que la aplicacion conteste y compara /version.json con el commit del Frontend.
#
# Despues de correrlo, si el navegador sigue mostrando lo anterior es la cache del service worker: Ctrl+Shift+R.
# Nunca apunta a preproduccion ni a produccion: solo toca esta maquina.
set -euo pipefail
# Git Bash en Windows convierte "/algo" en rutas de Windows al pasarlas a docker.exe.
export MSYS_NO_PATHCONV=1
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND="$(cd "$AQUI/.." && pwd)"
FRONTEND="$(cd "$BACKEND/${RUTA_FRONTEND:-../Frontend}" && pwd)"
RAMA="desarrollo"
URL_DE_LA_APP="http://localhost:4200"
INTENTOS=60

fallar() { echo "ERROR: $*" >&2; exit 1; }
# "git -C" no entiende las rutas /d/... de Git Bash cuando git es el de Windows: se entra a la carpeta y se corre alli.
g() { local carpeta="$1"; shift; (cd "$carpeta" && git "$@"); }

actualizar() {
  local nombre="$1" carpeta="$2"
  [[ -z "$(g "$carpeta" status --porcelain --untracked-files=no)" ]] \
    || fallar "$nombre tiene cambios sin guardar; guardalos (commit o stash) y vuelve a correr el script."
  g "$carpeta" fetch origin "$RAMA" --quiet
  g "$carpeta" checkout "$RAMA" --quiet
  g "$carpeta" merge --ff-only "origin/$RAMA" --quiet \
    || fallar "$nombre: '$RAMA' local no avanza limpio hacia origin/$RAMA (tiene commits propios)."
  echo "$nombre en $RAMA: $(g "$carpeta" rev-parse --short HEAD)"
}

actualizar Backend "$BACKEND"
actualizar Frontend "$FRONTEND"

VERSION="$(g "$FRONTEND" rev-parse --short HEAD)"
export VERSION

cd "$BACKEND"
docker compose -f docker-compose.yml -f docker-compose.correo.yml --profile app up -d --build backend frontend

echo "Esperando a que la aplicacion sirva la version $VERSION..."
for ((i = 1; i <= INTENTOS; i++)); do
  SERVIDA="$(curl -fsS "$URL_DE_LA_APP/version.json" 2>/dev/null | sed -n 's/.*"version":"\([^"]*\)".*/\1/p' || true)"
  if [[ "$SERVIDA" == "$VERSION" ]]; then
    echo "Listo: desarrollo sirve la version $VERSION en $URL_DE_LA_APP (Ctrl+Shift+R si el navegador muestra otra)."
    exit 0
  fi
  sleep 2
done
fallar "tras $((INTENTOS * 2)) s $URL_DE_LA_APP/version.json sirve '${SERVIDA:-nada}' y no '$VERSION'."
