#!/usr/bin/env bash
# Funciones compartidas por los scripts de operacion (12 factores, factor 12, SCRUM-219). No se ejecuta solo: lo cargan los demas.
#
# Leen la conexion de las mismas variables que usa la aplicacion (DB_URL, DB_USERNAME, DB_PASSWORD), asi que no hay nada fijo
# ni secreto en los scripts. Corren desde cualquier maquina con Docker: las herramientas de PostgreSQL vienen en una imagen,
# no hay que instalar nada.

# Evita que Git Bash en Windows convierta las rutas de los argumentos de docker.
export MSYS_NO_PATHCONV=1

fallar() {
  echo "Error: $*" >&2
  exit 1
}

# DB_URL = jdbc:postgresql://host:puerto/base?parametros  ->  BD_HOST, BD_PUERTO, BD_BASE, BD_SSL
leer_conexion() {
  [[ -n "${DB_URL:-}" && -n "${DB_USERNAME:-}" && -n "${DB_PASSWORD:-}" ]] \
    || fallar "faltan DB_URL, DB_USERNAME o DB_PASSWORD (las mismas variables que usa la aplicacion)."
  local resto="${DB_URL#jdbc:postgresql://}"
  [[ "$resto" != "$DB_URL" ]] || fallar "DB_URL debe empezar con jdbc:postgresql://"
  local autoridad="${resto%%/*}"
  local ruta="${resto#*/}"
  BD_HOST="${autoridad%%:*}"
  BD_PUERTO="5432"
  [[ "$autoridad" == *:* ]] && BD_PUERTO="${autoridad##*:}"
  BD_BASE="${ruta%%\?*}"
  BD_SSL="prefer"
  if [[ "$ruta" == *sslmode=* ]]; then
    BD_SSL="${ruta#*sslmode=}"
    BD_SSL="${BD_SSL%%&*}"
  fi
  [[ -n "$BD_HOST" && -n "$BD_BASE" ]] || fallar "no pude leer el servidor y la base de DB_URL."
  # El 6543 es el Transaction pooler de Supabase: rompe pg_dump y las sentencias preparadas. Solo sirve el 5432.
  [[ "$BD_PUERTO" != "6543" ]] || fallar "DB_URL apunta al puerto 6543 (Transaction pooler). Usa el Session pooler, puerto 5432."
}

# Desde un contenedor, "localhost" es el contenedor mismo: la base local de la maquina esta en host.docker.internal.
host_para_docker() {
  case "$BD_HOST" in
    localhost|127.0.0.1|::1) echo "host.docker.internal" ;;
    *) echo "$BD_HOST" ;;
  esac
}

# Corre una herramienta de PostgreSQL contra la base de DB_URL. La clave viaja por el entorno del contenedor, nunca en los argumentos.
#   herramienta_pg [-i] comando args...
herramienta_pg() {
  local interactivo=()
  if [[ "${1:-}" == "-i" ]]; then
    interactivo=(-i)
    shift
  fi
  docker run --rm "${interactivo[@]}" --add-host=host.docker.internal:host-gateway \
    -e PGPASSWORD="$DB_PASSWORD" -e PGSSLMODE="$BD_SSL" \
    "$IMAGEN_PG" "$@"
}

# Pregunta por la version del servidor y elige las herramientas de esa misma version: pg_dump no lee servidores mas nuevos que el.
elegir_imagen_pg() {
  if [[ -n "${IMAGEN_PG:-}" ]]; then
    return
  fi
  docker info > /dev/null 2>&1 || fallar "Docker no esta en marcha. Abre Docker Desktop (o inicia el servicio) y vuelve a intentar."
  IMAGEN_PG="postgres:16"
  local version
  # El error de psql se muestra tal cual (servidor inalcanzable, clave mala, base inexistente...): la clave viaja por el entorno
  # y psql nunca la imprime.
  version="$(herramienta_pg psql -h "$(host_para_docker)" -p "$BD_PUERTO" -U "$DB_USERNAME" -d "$BD_BASE" -tAc 'show server_version_num')" \
    || fallar "no pude conectarme a $BD_HOST:$BD_PUERTO (detalle arriba)."
  local mayor=$((version / 10000))
  [[ "$mayor" -ge 10 ]] || fallar "version de servidor inesperada: $version"
  IMAGEN_PG="postgres:$mayor"
}

# Nombres de las tablas del esquema, una por linea.
tablas_del_esquema() {
  herramienta_pg psql -h "$(host_para_docker)" -p "$BD_PUERTO" -U "$DB_USERNAME" -d "$BD_BASE" -tA \
    -c "select table_name from information_schema.tables where table_schema='cundiapp' and table_type='BASE TABLE' order by 1"
}
