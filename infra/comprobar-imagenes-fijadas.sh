#!/usr/bin/env bash
# Falla si alguna imagen base del repositorio no va fijada por digest (12 factores, factor 2, SCRUM-214). Es la comprobacion
# barata y sin red del CI: no dice si el digest esta al dia (eso lo hace actualizar-imagenes-base.sh --ver), solo que
# nadie vuelva a escribir "FROM algo:etiqueta" o "image: algo:etiqueta" a secas.
set -euo pipefail
cd "$(dirname "$0")/.."

sin_fijar=0

# Dockerfile: todo FROM que baja una imagen de un registro (las etapas se llaman por su nombre, sin registro).
if grep -nE '^FROM .*\$\{REGISTRO[A-Z_]*\}' Dockerfile | grep -v '@sha256:[0-9a-f]\{64\}'; then
  echo "^ Dockerfile: imagen base sin digest" >&2
  sin_fijar=1
fi

# Compose: toda "image:" salvo la que viene de una variable (la imagen que se acaba de construir, por su etiqueta de ECR).
for archivo in docker-compose.yml docker-compose.correo.yml infra/aws/instancia/compose-borde.yml infra/aws/instancia/compose-backend.yml; do
  if grep -nE '^\s*image:' "$archivo" | grep -v '\${' | grep -v '@sha256:[0-9a-f]\{64\}'; then
    echo "^ $archivo: imagen sin digest" >&2
    sin_fijar=1
  fi
done

# CI: las imagenes que corre el flujo de integracion continua.
if grep -nE '(nginx-unprivileged|osv-scanner|postgres):' .github/workflows/ci.yml | grep -v '^[0-9]*:\s*#' | grep -v '@sha256:[0-9a-f]\{64\}'; then
  echo "^ .github/workflows/ci.yml: imagen sin digest" >&2
  sin_fijar=1
fi

if [[ "$sin_fijar" -ne 0 ]]; then
  echo "Fija la imagen: infra/actualizar-imagenes-base.sh resuelve el digest y lo escribe." >&2
  exit 1
fi
echo "Todas las imagenes base van fijadas por digest."
