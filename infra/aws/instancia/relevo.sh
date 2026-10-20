#!/usr/bin/env bash
# Relevo sin caida de un servicio de Compose (SCRUM-229). Lo carga desplegar.sh con "source"; no se ejecuta solo.
#
# Antes, "docker compose up -d" detenia el contenedor viejo y luego arrancaba el nuevo: medido en preproduccion, 15 s de 502 por
# despliegue (23 de 161 peticiones a 5 por segundo). Como el backend no guarda estado en el proceso (sesiones, limites y el candado de
# las tareas viven en PostgreSQL), el nuevo puede correr JUNTO al viejo:
#
#   1. se arranca el nuevo al lado del viejo, con el mismo alias de red (Docker DNS devuelve las dos IP y nginx reparte entre ellas);
#   2. se espera a que el nuevo este sano;
#   3. solo entonces se retira el viejo (SIGTERM: deja de aceptar conexiones nuevas y termina las que lleva; nginx reintenta en la
#      otra IP si le llega una conexion rechazada).
#
# Si el nuevo no queda sano, se retira EL NUEVO y el viejo sigue sirviendo: un despliegue fallido tampoco causa caida.
#
# Quien lo cargue tiene que definir compose(): "docker compose" con el proyecto y los archivos de ese ambiente.

# RELEVO_NUEVO y RELEVO_HABIA_ANTERIOR las lee quien carga este archivo (desplegar.sh): ShellCheck no lo ve desde aqui.
# shellcheck disable=SC2034

# Intentos de 2 s que se espera a que el contenedor nuevo quede sano (90 = 3 minutos). Se puede bajar desde afuera (las pruebas).
RELEVO_INTENTOS="${RELEVO_INTENTOS:-90}"
# Segundos que se le da al viejo para terminar lo que lleva (el apagado ordenado de Spring espera hasta 25).
RELEVO_ESPERA_AL_VIEJO="${RELEVO_ESPERA_AL_VIEJO:-40}"
# Segundos que se espera, con el nuevo ya sano y el viejo todavia sirviendo, antes de retirar al viejo. nginx guarda 3 s la
# resolucion del alias (resolver ... valid=3s en borde.conf.template): si el viejo se retira antes de que nginx haya vuelto a
# resolver, su cache puede tener solo la IP vieja y rechazaria conexiones hasta renovarse (visto en probar-relevo.sh: un 502). Con los
# dos sirviendo no se pierde nada por esperar; tiene que ser mayor que ese "valid".
RELEVO_ESPERA_DNS="${RELEVO_ESPERA_DNS:-6}"
# Resultado de relevar_servicio, para quien lo llamo.
RELEVO_NUEVO=""
RELEVO_HABIA_ANTERIOR=0

# Ids de los contenedores de un servicio de Compose, tambien los parados.
contenedores_de() {
  docker ps -aq --filter "label=com.docker.compose.project=$1" --filter "label=com.docker.compose.service=$2"
}

# "estado salud" de un contenedor, o "ausente" si ya no existe.
estado_de() {
  docker inspect -f '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{end}}' "$1" 2> /dev/null || echo ausente
}

# 0 si el contenedor queda "healthy" antes de RELEVO_INTENTOS x 2 s; 1 si se cae o se acaba el plazo.
esperar_sano() {
  local estado
  for _ in $(seq 1 "$RELEVO_INTENTOS"); do
    estado="$(estado_de "$1")"
    case "$estado" in
      *" healthy") return 0 ;;
      exited* | dead* | ausente) return 1 ;;
    esac
    sleep 2
  done
  return 1
}

# Las ultimas lineas del registro de un contenedor, sin lo que parezca un secreto (la salida queda en el historial de SSM).
registro_sin_secretos() {
  docker logs --tail 25 "$1" 2>&1 | sed -E 's#(password|pwd|secret|token)=[^& ]+#\1=***#Ig' || true
}

# relevar_servicio <proyecto> <servicio>
#   0: el servicio queda con el contenedor nuevo sano (RELEVO_NUEVO) y el viejo retirado, o sin cambios si ya estaba al dia.
#   1: el nuevo no quedo sano; se imprimio su registro y se retiro. El viejo, si lo habia, sigue sirviendo.
relevar_servicio() {
  local proyecto="$1" servicio="$2" viejos nuevos nuevo candidato viejo hash_nuevo hash_viejo
  RELEVO_NUEVO=""
  viejos="$(contenedores_de "$proyecto" "$servicio")"
  RELEVO_HABIA_ANTERIOR=0
  [[ -n "$viejos" ]] && RELEVO_HABIA_ANTERIOR=1

  # Nada que relevar: un solo contenedor, sano, con la misma configuracion (imagen y variables). Compose guarda el hash de la
  # configuracion en una etiqueta; si coincide, arrancar otro seria reiniciar lo mismo para nada.
  if [[ -n "$viejos" && "$(wc -w <<< "$viejos")" -eq 1 ]]; then
    hash_nuevo="$(compose config --hash "$servicio" 2> /dev/null | awk '{print $2}')"
    hash_viejo="$(docker inspect -f '{{index .Config.Labels "com.docker.compose.config-hash"}}' "$viejos" 2> /dev/null || true)"
    if [[ -n "$hash_nuevo" && "$hash_nuevo" == "$hash_viejo" && "$(estado_de "$viejos")" == *" healthy" ]]; then
      echo "Sin cambios: $servicio ya corre esta configuracion; no se toca."
      RELEVO_NUEVO="$viejos"
      return 0
    fi
  fi

  # --no-recreate deja el viejo como esta; --scale agrega UNO mas, que sale con la configuracion nueva.
  compose up -d --no-recreate --scale "$servicio=$(($(wc -w <<< "$viejos") + 1))" "$servicio"
  nuevos="$(contenedores_de "$proyecto" "$servicio")"
  nuevo=""
  for candidato in $nuevos; do
    if ! grep -qw "$candidato" <<< "$viejos"; then
      nuevo="$candidato"
    fi
  done
  if [[ -z "$nuevo" ]]; then
    echo "No pude encontrar el contenedor nuevo de $servicio." >&2
    return 1
  fi

  if ! esperar_sano "$nuevo"; then
    echo "El contenedor nuevo de $servicio NO quedo sano. Ultimas lineas del registro (sin secretos):"
    registro_sin_secretos "$nuevo"
    docker rm -f "$nuevo" > /dev/null 2>&1 || true
    return 1
  fi

  # El nuevo ya sirve, pero nginx puede no haberlo descubierto todavia (cache de DNS): se espera a que lo conozca antes de retirar al viejo.
  if [[ -n "$viejos" ]]; then
    sleep "$RELEVO_ESPERA_DNS"
  fi

  # Se retira el viejo: SIGTERM, y el apagado ordenado termina lo que lleva.
  for viejo in $viejos; do
    docker stop -t "$RELEVO_ESPERA_AL_VIEJO" "$viejo" > /dev/null 2>&1 || true
    docker rm "$viejo" > /dev/null 2>&1 || true
  done
  RELEVO_NUEVO="$nuevo"
  return 0
}
