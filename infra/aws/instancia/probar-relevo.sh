#!/usr/bin/env bash
# Prueba el relevo sin caida (relevo.sh, SCRUM-229) con Docker de verdad: la nginx de borde real (la configuracion que genera
# renderizar-borde.sh) delante de un "backend" de mentira que se reemplaza mientras llegan peticiones sin parar.
#
#   bash infra/aws/instancia/probar-relevo.sh            # usa la imagen fijada en compose-borde.yml
#   IMAGEN=... bash infra/aws/instancia/probar-relevo.sh
#
# Comprueba:
#   1. relevar v1 -> v2 con trafico continuo: NINGUNA peticion falla, se ven las dos versiones y al final queda un solo contenedor (v2);
#   2. un contenedor nuevo que no queda sano se retira y el viejo sigue sirviendo, sin caida;
#   3. relevar la misma configuracion no reinicia nada ("Sin cambios").
#
# El backend de mentira es la nginx de la imagen con una configuracion minima (devuelve su version y /salud), que tarda 4 s en arrancar
# como una JVM. Lo que se prueba es la mecanica (alias de red compartido, esperar sano, retirar el viejo, reintento de nginx), no la aplicacion.
set -euo pipefail
# Git Bash en Windows convierte "/etc/..." en rutas de Windows al pasarlas a docker.exe.
export MSYS_NO_PATHCONV=1
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

IMAGEN="${IMAGEN:-$(grep -E '^\s*image:' "$AQUI/compose-borde.yml" | head -n1 | sed -E 's/^\s*image:\s*//' | tr -d '[:space:]')}"
[[ -n "$IMAGEN" ]] || { echo "No pude leer la imagen de compose-borde.yml" >&2; exit 2; }

SUFIJO="$$"
RED="relevo-prueba-$SUFIJO"
PROYECTO="relevo-prueba-$SUFIJO"
BORDE="relevo-borde-$SUFIJO"
TMP="$(mktemp -d)"
SECRETO="abcdefghij0123456789abcdefghij0123456789"
fallos=0

# docker.exe (Git Bash en Windows) no entiende /tmp/...: se le pasa la ruta de Windows. En Linux, pwd -W no existe y se usa la misma.
ruta_docker() { (cd "$1" && { pwd -W 2> /dev/null || pwd; }); }

limpiar() {
  touch "$TMP/parar" 2> /dev/null || true
  docker compose -p "$PROYECTO" -f "$TMP/compose-prueba.yml" down -v --remove-orphans > /dev/null 2>&1 || true
  docker rm -f "$BORDE" > /dev/null 2>&1 || true
  docker network rm "$RED" > /dev/null 2>&1 || true
  rm -rf "$TMP"
}
trap limpiar EXIT

# ---- la nginx de borde real, para los dos ambientes ---------------------------------------------------------------------------
mkdir -p "$TMP/conf.d"
bash "$AQUI/renderizar-borde.sh" 8080 "$SECRETO" backend-prod > "$TMP/conf.d/prod.conf"
bash "$AQUI/renderizar-borde.sh" 8081 "$SECRETO" backend-preprod > "$TMP/conf.d/preprod.conf"
chmod 755 "$TMP" "$TMP/conf.d"
chmod 644 "$TMP/conf.d/"*.conf
docker network create "$RED" > /dev/null
docker run -d --name "$BORDE" --network "$RED" -v "$(ruta_docker "$TMP/conf.d"):/etc/nginx/conf.d:ro" -p 18080:8080 "$IMAGEN" > /dev/null

# ---- el servicio de mentira, definido como el real: sin container_name, con el alias de red del ambiente ----------------------------
cat > "$TMP/compose-prueba.yml" <<YML
services:
  backend:
    image: $IMAGEN
    restart: "no"
    stop_grace_period: 10s
    # Va como variable de entorno del servicio y no solo en el archivo de configuracion: asi cambia el hash de la configuracion del
    # servicio (que es lo que mira relevo.sh para saber si hay algo que relevar), igual que cambia con la imagen en el servicio real.
    environment:
      VERSION: \${VERSION}
      SALUD: \${SALUD}
    # Tarda 4 s en arrancar, como una JVM: asi el relevo tiene que esperar de verdad.
    entrypoint: ["/bin/sh", "-c", "sleep 4 && exec /docker-entrypoint.sh nginx -g 'daemon off;'"]
    configs:
      - source: conf
        target: /etc/nginx/conf.d/default.conf
    healthcheck:
      test: ["CMD", "wget", "-q", "-O", "/dev/null", "http://127.0.0.1:8080/salud"]
      interval: 1s
      timeout: 2s
      retries: 2
    networks:
      cundiapp:
        aliases: [backend-prod]
configs:
  conf:
    # La version y el codigo de /salud los pone Compose al leer el archivo: cambiar la version cambia la configuracion (y su hash).
    content: |
      server {
        listen 8080;
        location = /salud { return \${SALUD} "ok"; }
        location / { return 200 "\${VERSION}"; }
      }
networks:
  cundiapp:
    external: true
    name: $RED
YML
ARCHIVO="$(ruta_docker "$TMP")/compose-prueba.yml"
compose() { docker compose -p "$PROYECTO" -f "$ARCHIVO" "$@"; }

# shellcheck source=relevo.sh
source "$AQUI/relevo.sh"
RELEVO_INTENTOS=20 # 40 s: el de mentira tarda 4 s en arrancar
RELEVO_ESPERA_AL_VIEJO=5

comprobar() { # comprobar <descripcion> <comando...>
  local descripcion="$1"
  shift
  if "$@"; then echo "ok       $descripcion"; else echo "FALLO    $descripcion"; fallos=$((fallos + 1)); fi
}
pedir() { curl -s -m 10 -H "X-Origin-Verify: $SECRETO" -w ' %{http_code}' "http://127.0.0.1:18080/api/prueba" || echo " 000"; }
esperar_borde() {
  for _ in $(seq 1 30); do
    [[ "$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:18080/ || true)" == "404" ]] && return 0
    sleep 1
  done
  return 1
}
cuantos() { contenedores_de "$PROYECTO" backend | wc -w | tr -d ' '; }
igual() { [[ "$1" == "$2" ]]; }

esperar_borde || { echo "La nginx de borde no arranco." >&2; exit 1; }

# ---- 1. el primer despliegue (no hay viejo) y despues v1 -> v2 con trafico continuo -------------------------------------------------
export VERSION=v1 SALUD=200
relevar_servicio "$PROYECTO" backend > /dev/null 2>&1
comprobar "el primer despliegue deja un contenedor sano" igual "$(cuantos)" 1
for _ in $(seq 1 30); do [[ "$(pedir)" == "v1 200" ]] && break; sleep 1; done
comprobar "responde v1 por la nginx de borde" igual "$(pedir)" "v1 200"

( # trafico continuo: una peticion cada 0,1 s hasta que se pida parar
  while [[ ! -f "$TMP/parar" ]]; do
    pedir >> "$TMP/sondeo.txt"
    echo >> "$TMP/sondeo.txt"
    sleep 0.1
  done
) &
SONDEO=$!
sleep 2

export VERSION=v2 SALUD=200
echo "Relevando v1 -> v2 con trafico continuo..."
relevar_servicio "$PROYECTO" backend > "$TMP/relevo-v2.log" 2>&1 || { cat "$TMP/relevo-v2.log" >&2; fallos=$((fallos + 1)); }
# nginx guarda la resolucion de DNS 3 s: se sigue mirando un buen rato despues, para ver los rechazos que todavia pudieran llegar.
sleep 8
touch "$TMP/parar"
wait "$SONDEO" 2> /dev/null || true

total="$(grep -c ' [0-9][0-9][0-9]$' "$TMP/sondeo.txt" || true)"
malas="$(grep ' [0-9][0-9][0-9]$' "$TMP/sondeo.txt" | grep -vc ' 200$' || true)"
echo "         peticiones durante el relevo: $total, fallidas: $malas"
comprobar "hubo trafico suficiente durante el relevo (>= 40 peticiones)" test "${total:-0}" -ge 40
comprobar "NINGUNA peticion fallo durante el relevo" igual "${malas:-1}" 0
comprobar "se vio v1 antes del relevo" grep -q '^v1 200$' "$TMP/sondeo.txt"
comprobar "se vio v2 despues del relevo" grep -q '^v2 200$' "$TMP/sondeo.txt"
comprobar "queda un solo contenedor" igual "$(cuantos)" 1
comprobar "y es el nuevo (v2)" igual "$(pedir)" "v2 200"

# ---- 2. un nuevo que no queda sano: se retira y el viejo sigue sirviendo --------------------------------------------------------------
viejo_antes="$(contenedores_de "$PROYECTO" backend)"
export VERSION=v3 SALUD=500
RELEVO_INTENTOS=8
if relevar_servicio "$PROYECTO" backend > "$TMP/relevo-v3.log" 2>&1; then
  comprobar "un contenedor que no queda sano hace fallar el relevo" false
else
  comprobar "un contenedor que no queda sano hace fallar el relevo" true
fi
comprobar "el contenedor roto se retiro y queda el viejo" igual "$(contenedores_de "$PROYECTO" backend)" "$viejo_antes"
comprobar "el viejo sigue sirviendo (v2), sin caida" igual "$(pedir)" "v2 200"

# ---- 3. la misma configuracion: no se reinicia nada --------------------------------------------------------------------------------------
export VERSION=v2 SALUD=200
RELEVO_INTENTOS=20
relevar_servicio "$PROYECTO" backend > "$TMP/relevo-igual.log" 2>&1 || true
comprobar "la misma configuracion dice 'Sin cambios'" grep -q "Sin cambios" "$TMP/relevo-igual.log"
comprobar "y no cambio el contenedor" igual "$(contenedores_de "$PROYECTO" backend)" "$viejo_antes"

if [[ "$fallos" -gt 0 ]]; then
  echo "$fallos comprobacion(es) fallaron. Sondeo (fallidas):" >&2
  grep -v ' 200$' "$TMP/sondeo.txt" 2> /dev/null | sort | uniq -c | head >&2 || true
  exit 1
fi
echo "Todo bien: el relevo no dejo caer ni una peticion, retiro lo que no quedo sano y no reinicio lo que no cambio."
