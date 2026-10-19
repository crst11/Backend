#!/usr/bin/env bash
# Prueba la nginx de borde de verdad, con la configuracion que genera renderizar-borde.sh y los dos ambientes juntos (SCRUM-225): que
# sus registros NO lleven la IP del cliente, ni la query, y que si lleven lo que sirve para operar (estado, ruta, id de peticion).
#
#   bash infra/aws/instancia/probar-borde.sh            # usa la imagen fijada en compose-borde.yml
#   IMAGEN=... bash infra/aws/instancia/probar-borde.sh
#
# Los accesos van a CloudWatch con 30 dias de retencion: con la IP serian datos personales (Ley 1581). nginx la imprime por dos
# caminos, el acceso (X-Forwarded-For y la de CloudFront) y el registro de errores ("client: ..."), y se prueban los dos.
set -euo pipefail
# Git Bash en Windows convierte "/etc/..." y "/cundiapp/..." en rutas de Windows al pasarlas a docker.exe.
export MSYS_NO_PATHCONV=1
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

IMAGEN="${IMAGEN:-$(grep -E '^\s*image:' "$AQUI/compose-borde.yml" | head -n1 | sed -E 's/^\s*image:\s*//' | tr -d '[:space:]')}"
[[ -n "$IMAGEN" ]] || { echo "No pude leer la imagen de compose-borde.yml" >&2; exit 2; }

SUFIJO="$$"
RED="borde-prueba-$SUFIJO"
BORDE="borde-prueba-$SUFIJO"
BACKEND="backend-prod"
TMP="$(mktemp -d)"
SECRETO="abcdefghij0123456789abcdefghij0123456789"
IP_DE_PRUEBA="203.0.113.77"
fallos=0

limpiar() {
  docker rm -f "$BORDE" "$BACKEND" > /dev/null 2>&1 || true
  docker network rm "$RED" > /dev/null 2>&1 || true
  rm -rf "$TMP"
}
trap limpiar EXIT

# La configuracion real de los dos ambientes, igual que en desplegar.sh.
mkdir -p "$TMP/conf.d"
bash "$AQUI/renderizar-borde.sh" 8080 "$SECRETO" backend-prod > "$TMP/conf.d/prod.conf"
bash "$AQUI/renderizar-borde.sh" 8081 "$SECRETO" backend-preprod > "$TMP/conf.d/preprod.conf"
chmod 755 "$TMP" "$TMP/conf.d"
chmod 644 "$TMP/conf.d/"*.conf
# docker.exe (Git Bash en Windows) no entiende /tmp/...: se le pasa la ruta de Windows. En Linux, pwd -W no existe y se usa la misma.
RUTA_CONF="$(cd "$TMP/conf.d" && { pwd -W 2> /dev/null || pwd; })"

docker network create "$RED" > /dev/null
# Un backend de mentira para produccion (la nginx de la imagen, que escucha en el 8080). Preproduccion no tiene: su 502 tambien se prueba.
docker run -d --name "$BACKEND" --network "$RED" "$IMAGEN" > /dev/null
docker run -d --name "$BORDE" --network "$RED" -v "$RUTA_CONF:/etc/nginx/conf.d:ro" -p 18080:8080 -p 18081:8081 "$IMAGEN" > /dev/null

for _ in $(seq 1 30); do
  [[ "$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:18080/ || true)" == "404" ]] && break
  sleep 1
done

pedir() { # pedir <puerto> <ruta> [cabeceras de curl...]
  local puerto="$1" ruta="$2"
  shift 2
  curl -s -o /dev/null "$@" "http://127.0.0.1:$puerto$ruta" || true
}

# Una peticion con la IP "del cliente" en X-Forwarded-For y datos personales en la query, para ver que no salgan en el registro.
pedir 18080 "/api/prueba?correo=ana@example.test&token=abc123" -H "X-Origin-Verify: $SECRETO" -H "X-Forwarded-For: $IP_DE_PRUEBA"
pedir 18080 "/api/prueba" -H "X-Forwarded-For: $IP_DE_PRUEBA"                                  # sin secreto: 403
pedir 18081 "/api/prueba" -H "X-Origin-Verify: $SECRETO" -H "X-Forwarded-For: $IP_DE_PRUEBA"   # preproduccion sin backend: 502
# Una rafaga que pase el limite (10 por segundo y 40 de rafaga): los 429 escriben en el registro de ERRORES, donde nginx pone "client: IP".
seq 1 150 | xargs -P 30 -I{} curl -s -o /dev/null -H "X-Origin-Verify: $SECRETO" -H "X-Forwarded-For: $IP_DE_PRUEBA" "http://127.0.0.1:18080/api/rafaga" || true
sleep 1

salida="$(docker logs "$BORDE" 2>&1)"
accesos="$(grep '^{' <<< "$salida" || true)"

comprobar() { # comprobar <descripcion> <comando...>
  local descripcion="$1"
  shift
  if "$@"; then
    echo "ok       $descripcion"
  else
    echo "FALLO    $descripcion"
    fallos=$((fallos + 1))
  fi
}
tiene() { grep -qE "$1" <<< "$2"; }
no_tiene() { ! grep -qE "$1" <<< "$2"; }

comprobar "hay accesos registrados en JSON" tiene '^\{"@timestamp"' "$accesos"
comprobar "el acceso trae metodo, ruta y estado" tiene '"metodo":"GET","ruta":"/api/prueba","estado":[0-9]+' "$accesos"
comprobar "el acceso trae el id de peticion (la clave del backend)" tiene '"peticion":"[0-9a-f]{32}"' "$accesos"
comprobar "se registra cada ambiente por su puerto" tiene '"puerto":"8081"' "$accesos"
comprobar "se ve el 403 de una peticion sin secreto" tiene '"estado":403' "$accesos"
comprobar "se ve el 502 de preproduccion sin backend" tiene '"estado":502' "$accesos"
comprobar "se ve el 429 del limite de peticiones" tiene '"estado":429' "$accesos"
comprobar "la IP de X-Forwarded-For no sale en ninguna linea" no_tiene "${IP_DE_PRUEBA//./\\.}" "$salida"
comprobar "ninguna IP en los accesos (ni la de CloudFront ni la del cliente)" no_tiene '([0-9]{1,3}\.){3}[0-9]{1,3}' "$accesos"
comprobar "la query no sale (ni el correo ni el token)" no_tiene 'ana@example|abc123|correo=|token=' "$salida"
comprobar "el registro de errores no imprime 'client:'" no_tiene 'client: ' "$(grep -v '^{' <<< "$salida" || true)"

if [[ "$fallos" -gt 0 ]]; then
  echo "$fallos comprobacion(es) fallaron. Primeras lineas del registro:" >&2
  head -n 12 <<< "$salida" | cut -c1-220 >&2
  exit 1
fi
echo "Todo bien: los registros de la puerta de entrada no llevan la IP."
