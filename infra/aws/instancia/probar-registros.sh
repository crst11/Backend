#!/usr/bin/env bash
# Prueba el chequeo previo de CloudWatch Logs de desplegar.sh (12 factores, factor 11, SCRUM-222) con un "aws" de mentira, y que
# Compose arme bien el registro de awslogs. Un error aqui dejaria el backend sin arrancar en un despliegue, asi que se prueba:
#
#   bash infra/aws/instancia/probar-registros.sh
#
# No toca AWS: el "aws" es un script que responde lo que cada caso le pide.
set -euo pipefail
# Evita que Git Bash en Windows convierta "/cundiapp/..." en una ruta de Windows al pasarlo a docker.
export MSYS_NO_PATHCONV=1
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# docker.exe (Git Bash en Windows) no entiende /d/...: se le pasa la ruta de Windows. En Linux, pwd -W no existe y se usa la misma.
RUTA_DOCKER="$(cd "$AQUI" && { pwd -W 2>/dev/null || pwd; })"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
fallos=0

mkdir -p "$TMP/bin"
cat > "$TMP/bin/aws" <<'EOF'
#!/usr/bin/env bash
# aws de mentira: guarda los argumentos y responde segun CASO.
echo "$*" >> "$LLAMADAS"
case "$CASO" in
  creado)    exit 0 ;;
  existe)    echo "An error occurred (ResourceAlreadyExistsException) when calling the CreateLogStream operation: The specified log stream already exists" >&2; exit 254 ;;
  sin_permiso) echo "An error occurred (AccessDeniedException) when calling the CreateLogStream operation: User is not authorized to perform: logs:CreateLogStream" >&2; exit 254 ;;
  sin_grupo) echo "An error occurred (ResourceNotFoundException) when calling the CreateLogStream operation: The specified log group does not exist." >&2; exit 254 ;;
  sin_red)   echo "Could not connect to the endpoint URL: \"https://logs.us-east-2.amazonaws.com/\"" >&2; exit 255 ;;
esac
EOF
chmod +x "$TMP/bin/aws"
export LLAMADAS="$TMP/llamadas.txt"

# shellcheck source=registros.sh
source "$AQUI/registros.sh"

esperar() { # esperar <caso> <se espera: si|no>
  local caso="$1" esperado="$2" obtuvo=no
  : > "$LLAMADAS"
  if CASO="$caso" PATH="$TMP/bin:$PATH" cloudwatch_alcanzable /cundiapp/prod/backend; then obtuvo=si; fi
  if [[ "$obtuvo" == "$esperado" ]]; then
    echo "ok       $caso -> $obtuvo"
  else
    echo "FALLO    $caso -> $obtuvo (se esperaba $esperado)"
    fallos=$((fallos + 1))
  fi
}

esperar creado si
esperar existe si
esperar sin_permiso no
esperar sin_grupo no
esperar sin_red no

# Debe preguntar por el grupo que se le pide y por el stream de sondeo: ni crear grupos ni escribir eventos.
: > "$LLAMADAS"
CASO=creado PATH="$TMP/bin:$PATH" cloudwatch_alcanzable /cundiapp/preprod/backend
if grep -q -- "logs create-log-stream --log-group-name /cundiapp/preprod/backend --log-stream-name sondeo" "$LLAMADAS" \
   && ! grep -qE "create-log-group|put-log-events" "$LLAMADAS"; then
  echo "ok       la llamada pide solo el stream de sondeo del grupo indicado"
else
  echo "FALLO    la llamada a aws no es la esperada: $(cat "$LLAMADAS")"
  fallos=$((fallos + 1))
fi

# Compose: con el archivo de CloudWatch el registro es awslogs y NO arrastra las opciones de json-file.
if docker compose version > /dev/null 2>&1; then
  export AWS_DEFAULT_REGION=us-east-2 GRUPO_DE_REGISTROS=/cundiapp/prod/backend
  export IMAGEN=x ENTORNO=prod PERFIL=prod DB_URL=x DB_USERNAME=x DB_PASSWORD=x JWT_SECRETO=x CORREO_SMTP_HOST=x \
         CORREO_SMTP_USUARIO=x CORREO_SMTP_CLAVE=x GOOGLE_CLIENT_ID=x CORS_ORIGENES=x
  for par in "compose-backend.yml compose-backend.cloudwatch.yml /cundiapp/prod/backend" "compose-borde.yml compose-borde.cloudwatch.yml /cundiapp/borde"; do
    read -r base extra grupo <<< "$par"
    final="$(docker compose -f "$RUTA_DOCKER/$base" -f "$RUTA_DOCKER/$extra" config 2>&1)" || { echo "FALLO    compose config $extra: $final"; fallos=$((fallos + 1)); continue; }
    if grep -q "driver: awslogs" <<< "$final" \
       && grep -q "awslogs-group: $grupo" <<< "$final" \
       && grep -q "awslogs-region: us-east-2" <<< "$final" \
       && grep -q 'awslogs-create-group: "false"' <<< "$final" \
       && grep -q "mode: non-blocking" <<< "$final" \
       && ! grep -qE "max-size|max-file|json-file" <<< "$final"; then
      echo "ok       $extra arma awslogs sin restos de json-file"
    else
      echo "FALLO    $extra no arma el registro esperado:"
      grep -A9 "logging:" <<< "$final"
      fallos=$((fallos + 1))
    fi
  done
  # Sin el archivo de CloudWatch todo sigue igual que antes.
  if docker compose -f "$RUTA_DOCKER/compose-backend.yml" config 2>&1 | grep -q "driver: json-file"; then
    echo "ok       sin el archivo de CloudWatch el backend sigue en json-file"
  else
    echo "FALLO    compose-backend.yml ya no registra en json-file por defecto"
    fallos=$((fallos + 1))
  fi
else
  echo "omitido  docker compose no esta disponible: no se prueba la fusion de archivos"
fi

if [[ "$fallos" -gt 0 ]]; then
  echo "$fallos prueba(s) fallaron." >&2
  exit 1
fi
echo "Todo bien."
