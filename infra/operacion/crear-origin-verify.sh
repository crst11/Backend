#!/usr/bin/env bash
# Crea ORIGIN_VERIFY en Parameter Store, uno por ambiente (12 factores, factor 12, SCRUM-219). Es el unico secreto que el CI no
# puede inventar: CloudFront lo lee al crearse frontend.yml, asi que tiene que existir antes. Ver docs/despliegue.md §2.
#
#   infra/operacion/crear-origin-verify.sh            crea los que faltan (preprod y prod) y deja en paz los que ya existen
#   infra/operacion/crear-origin-verify.sh --ver      solo dice cuales existen
#
# NUNCA reemplaza uno que ya existe: cambiarlo a mitad de camino deja a CloudFront mandando un valor y a la nginx esperando
# otro, y la pagina responde 403. Rotarlo es un cambio coordinado y se hace a proposito, a mano. Nunca imprime el valor.
# Hace falta un perfil de AWS CLI con permiso sobre /cundiapp/* en la region us-east-2 (o la de AWS_REGION).
set -euo pipefail

REGION="${AWS_REGION:-us-east-2}"
MODO="${1:-crear}"
[[ "$MODO" == "crear" || "$MODO" == "--ver" ]] || { echo "Uso: $0 [--ver]" >&2; exit 2; }

faltan=0
for AMBIENTE in preprod prod; do
  NOMBRE="/cundiapp/$AMBIENTE/ORIGIN_VERIFY"
  if aws ssm get-parameter --region "$REGION" --name "$NOMBRE" --query 'Parameter.Name' --output text > /dev/null 2>&1; then
    echo "Ya existe: $NOMBRE (no se toca)"
    continue
  fi
  if [[ "$MODO" == "--ver" ]]; then
    echo "Falta:     $NOMBRE"
    faltan=1
    continue
  fi
  # Solo letras y numeros, 32 o mas. String (no SecureString) porque CloudFormation lo resuelve con {{resolve:ssm:...}}.
  aws ssm put-parameter --region "$REGION" --name "$NOMBRE" --type String --value "$(openssl rand -hex 32)" > /dev/null
  echo "Creado:    $NOMBRE"
done
exit "$faltan"
