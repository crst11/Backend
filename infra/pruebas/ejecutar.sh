#!/usr/bin/env bash
# Corre una prueba de API o de carga contra un stack desechable con los limites de produccion (SCRUM-234).
#
#   bash infra/pruebas/ejecutar.sh api                 la coleccion de Postman completa, con newman
#   bash infra/pruebas/ejecutar.sh humo                1 usuario durante 30 s: la API contesta y los flujos funcionan
#   bash infra/pruebas/ejecutar.sh carga               carga normal sostenida (20 usuarios a la vez)
#   bash infra/pruebas/ejecutar.sh estres              sube la carga por escalones hasta pasar lo razonable y mide como se recupera
#   bash infra/pruebas/ejecutar.sh pico                un salto brusco de 5 a 100 usuarios (el inicio de una clase)
#   bash infra/pruebas/ejecutar.sh resistencia         carga moderada durante 10 minutos (busca fugas de memoria)
#
# Opciones: --conservar deja el stack encendido al terminar (para mirarlo a mano); USUARIOS=n cambia cuantas cuentas se crean.
# Nunca apuntes esto a produccion ni a preproduccion: este script SOLO levanta su propio stack local.
set -euo pipefail
# Git Bash en Windows convierte "/etc/..." en rutas de Windows al pasarlas a docker.exe.
export MSYS_NO_PATHCONV=1
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RAIZ="$(cd "$AQUI/../.." && pwd)"

IMAGEN_K6="grafana/k6:2.3.0@sha256:9c2dee7f8ed74d317e4027c06a10f169b625638189de8d4555d0b3486a5aeb34"
IMAGEN_NEWMAN="postman/newman:6.1.3-alpine@sha256:d9b5e780ead0026bbb37f3759cd7b10fc8a88cd4030c8266a7c5e343d75a5198"
PROYECTO="cundiapp-pruebas"
RED="${PROYECTO}_default"

MODO="${1:-}"
CONSERVAR=0
[[ "${2:-}" == "--conservar" ]] && CONSERVAR=1
case "$MODO" in
  api | humo | carga | estres | pico | resistencia) ;;
  *)
    sed -n '2,14p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//' >&2
    exit 2
    ;;
esac

# docker.exe (Git Bash en Windows) no entiende /d/...: se le pasa la ruta de Windows. En Linux, pwd -W no existe y se usa la misma.
ruta_docker() { (cd "$1" && { pwd -W 2> /dev/null || pwd; }); }
aleatorio() { head -c 32 /dev/urandom | od -An -tx1 | tr -d ' \n'; }

# Los secretos nacen aqui, en cada ejecucion, y mueren con ella: ninguno esta escrito en un archivo.
# La prueba funcional de la API corre toda la coleccion desde una sola IP: el limite de correos se sube solo para ella.
if [[ "$MODO" == "api" ]]; then
  export LIMITE_CORREOS_POR_IP=1000
  # Las carpetas de avisos de la coleccion esperan un aviso de verdad: la ronda tiene que correr cada pocos segundos.
  export AVISOS_CADA=PT5S AVISOS_ESPERA_INICIAL=PT2S
fi
DB_PASSWORD="$(aleatorio)"
JWT_SECRETO="$(aleatorio)"
export DB_PASSWORD JWT_SECRETO
ARCHIVO="$(ruta_docker "$AQUI")/compose.yml"
compose() { docker compose -p "$PROYECTO" -f "$ARCHIVO" "$@"; }

RESULTADOS="$AQUI/resultados"
mkdir -p "$RESULTADOS"
# El usuario del contenedor de k6 y el del runner no son el mismo: la carpeta tiene que dejarse escribir.
chmod 777 "$RESULTADOS"
SONDEO_MEMORIA=""

# Se invoca con trap, no por su nombre: ShellCheck no lo ve.
# shellcheck disable=SC2329,SC2317
limpiar() {
  if [[ -n "$SONDEO_MEMORIA" ]]; then kill "$SONDEO_MEMORIA" 2> /dev/null || true; fi
  if [[ "$CONSERVAR" -eq 1 ]]; then
    echo "Stack conservado: API en http://127.0.0.1:${PUERTO_API:-18080}, Mailpit solo dentro de la red $RED. Apagalo con: docker compose -p $PROYECTO -f infra/pruebas/compose.yml down -v"
  else
    compose down -v --remove-orphans > /dev/null 2>&1 || true
  fi
}
trap limpiar EXIT

# Docker Hub limita las descargas anonimas en los runners compartidos ("toomanyrequests"): una imagen que no baja a la primera no debe
# contarse como un fallo de la prueba. Se reintenta con espera antes de levantar nada.
bajar() {
  for intento in 1 2 3 4; do
    if docker pull --quiet "$1" > /dev/null 2>&1; then return 0; fi
    echo "No se pudo bajar $1 (intento $intento de 4)" >&2
    sleep $((intento * 15))
  done
  echo "No se pudo bajar $1." >&2
  return 1
}
echo "==> Bajando las imagenes"
grep -E '^\s+image:' "$AQUI/compose.yml" | sed -E 's/^\s*image:\s*//' | while read -r imagen; do bajar "$imagen" || exit 1; done
if [[ "$MODO" == "api" ]]; then bajar "$IMAGEN_NEWMAN"; else bajar "$IMAGEN_K6"; fi

echo "==> Levantando el stack (la primera vez compila el backend: unos minutos)"
# La compilacion baja la imagen de Java del registro publico de ECR, que a veces no responde o limita las descargas desde los runners
# compartidos. Se alternan los dos origenes (mismo digest en los dos, como en la accion de construir la imagen) y se reintenta con espera.
REGISTROS=("${REGISTRO:-public.ecr.aws/docker/library}" "docker.io/library")
levantado=0
for intento in 1 2 3 4; do
  if REGISTRO="${REGISTROS[$(((intento - 1) % 2))]}" compose up -d --build --wait --wait-timeout 420 > "$RESULTADOS/stack.log" 2>&1; then
    levantado=1
    break
  fi
  echo "El stack no levanto (intento $intento de 4); se reintenta con otro origen de imagenes." >&2
  tail -n 6 "$RESULTADOS/stack.log" >&2
  sleep $((intento * 10))
done
if [[ "$levantado" -ne 1 ]]; then
  tail -n 30 "$RESULTADOS/stack.log" >&2
  compose logs --tail 40 backend >&2 || true
  echo "El stack no quedo sano." >&2
  exit 1
fi
BACKEND="$(compose ps -q backend)"

# Memoria del backend cada 5 s: el pico dice mas que el valor final.
(
  while true; do
    docker stats --no-stream --format '{{.MemUsage}}' "$BACKEND" 2> /dev/null | sed 's/ .*//' >> "$RESULTADOS/memoria-$MODO.txt" || true
    sleep 5
  done
) &
SONDEO_MEMORIA=$!
: > "$RESULTADOS/memoria-$MODO.txt"

estado=0
case "$MODO" in
  api)
    echo "==> newman: colección completa de Postman"
    # Con la red del backend, "localhost:8080" de la coleccion es el backend: no hay que cambiar sus variables. Mailpit se alcanza por su nombre.
    docker run --rm --network "container:$BACKEND" \
      -v "$(ruta_docker "$RAIZ/docs/postman")":/etc/newman:ro \
      -v "$(ruta_docker "$RESULTADOS")":/resultados \
      "$IMAGEN_NEWMAN" run CundiApp.postman_collection.json \
      --env-var "mailpit_url=http://correo:8025" \
      --reporters cli,junit --reporter-junit-export /resultados/newman.xml \
      --working-dir /etc/newman --delay-request 50 || estado=$?
    ;;
  *)
    echo "==> k6: $MODO"
    # Estres y pico quitan casi toda la pausa entre pasos: asi 100 usuarios virtuales saturan de verdad el backend.
    pausa=1
    [[ "$MODO" == "estres" ]] && pausa=0.05
    [[ "$MODO" == "pico" ]] && pausa=0.1
    docker run --rm --network "$RED" \
      -v "$(ruta_docker "$AQUI/carga")":/pruebas:ro \
      -v "$(ruta_docker "$RESULTADOS")":/resultados \
      -e BASE_URL=http://backend:8080 -e MAILPIT_URL=http://correo:8025 -e USUARIOS="${USUARIOS:-30}" -e FACTOR_PAUSA="$pausa" -e DURACION="${DURACION:-10m}" \
      "$IMAGEN_K6" run --quiet --summary-export "/resultados/k6-$MODO.json" "/pruebas/$MODO.js" || estado=$?
    ;;
esac

echo "==> Estado del backend despues de la prueba"
oom="$(docker inspect -f '{{.State.OOMKilled}}' "$BACKEND")"
reinicios="$(docker inspect -f '{{.RestartCount}}' "$BACKEND")"
salud="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}sin-healthcheck{{end}}' "$BACKEND")"
pico="$(sort -h "$RESULTADOS/memoria-$MODO.txt" 2> /dev/null | tail -n 1)"
# Lo que docker stats mezcla, por separado: el pico de memoria RESIDENTE del proceso de Java (VmHWM) y, del cgroup, la memoria anonima
# (la que de verdad empuja al kernel a matar al contenedor), el cache de archivos (que el kernel suelta cuando hace falta) y los OOM.
detalle="$(docker exec "$BACKEND" sh -c 'echo "pico-residente=$(awk "/VmHWM/{printf \"%d\", \$2/1024}" /proc/1/status)MiB residente-ahora=$(awk "/VmRSS/{printf \"%d\", \$2/1024}" /proc/1/status)MiB"; awk "/^anon /{printf \"cgroup-anon=%dMiB \", \$2/1048576} /^file /{printf \"cgroup-cache=%dMiB\", \$2/1048576}" /sys/fs/cgroup/memory.stat 2> /dev/null; echo; grep -E "^(oom|oom_kill) " /sys/fs/cgroup/memory.events 2> /dev/null | tr "
" " "' 2> /dev/null || true)"
echo "    OOMKilled=$oom reinicios=$reinicios salud=$salud memoria-maxima=${pico:-?} (limite 448MiB)"
[[ -n "$detalle" ]] && echo "    $detalle" | sed 's/^/    /'
if [[ "$oom" != "false" || "$reinicios" != "0" || "$salud" != "healthy" ]]; then
  echo "FALLO: el backend se cayo, se reinicio o quedo enfermo durante la prueba." >&2
  compose logs --tail 40 backend >&2 || true
  estado=1
fi
exit "$estado"
