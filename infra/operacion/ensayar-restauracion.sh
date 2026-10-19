#!/usr/bin/env bash
# Ensaya restaurar-bd.sh de punta a punta contra una base DESECHABLE (12 factores, factor 12, SCRUM-226). DESTRUCTIVO: borra el
# esquema cundiapp de la base de DB_URL y lo recrea. restaurar-bd.sh reemplaza datos y es el unico script de operacion que no se
# habia probado sobre Supabase: este es el ensayo, para hacerlo en un proyecto de Supabase creado solo para esto (ver
# docs/despliegue.md, §7) o contra un PostgreSQL local (asi corre en el CI).
#
#   CONFIRMO_ENSAYO_EN=<servidor de DB_URL> DB_URL=... DB_USERNAME=... DB_PASSWORD=... infra/operacion/ensayar-restauracion.sh
#
# Lo que hace, en orden:
#   1. Prepara la base: recrea el esquema con Flyway (migrar-a-mano.sh) y le pone 25 cuentas sinteticas.
#   2. Respalda ese estado (A). Despues lo ensucia (5 cuentas mas: estado B).
#   3. restaurar-bd.sh SIN confirmacion tiene que negarse, y la base queda en B.
#   4. restaurar-bd.sh con un respaldo DANADO tiene que fallar, y la base queda en B (no se toca).
#   5. restaurar-bd.sh con el respaldo bueno: la base tiene que quedar exactamente en A (tabla por tabla, y la version de Flyway),
#      y tiene que haber dejado el respaldo previo (antes-de-restaurar-*.dump) de lo que habia (B).
#   6. Deshacer: restaurar ese respaldo previo tiene que devolver la base a B.
#
# Barreras, porque borra datos (una base real no puede pasar ninguna de las tres):
#   - CONFIRMO_ENSAYO_EN tiene que ser igual al servidor de DB_URL.
#   - Si hay aws, se compara el servidor con los DB_URL de /cundiapp/* en Parameter Store (preproduccion y produccion) y se niega
#     si coincide. Sin aws, o sin permiso para leerlos, solo avisa: la barrera siguiente sigue ahi.
#   - Si la base ya tiene el esquema cundiapp con CUALQUIER estudiante que no sea sintetico (ensayoN@ensayo.invalid), se niega.
set -euo pipefail
# shellcheck source=comun.sh
source "$(dirname "$0")/comun.sh"

AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CORREO_SINTETICO="ensayo%@ensayo.invalid"

leer_conexion
[[ "${CONFIRMO_ENSAYO_EN:-}" == "$BD_HOST" ]] \
  || fallar "esto BORRA el esquema cundiapp de $BD_HOST y lo recrea. Solo contra una base desechable. Para continuar: CONFIRMO_ENSAYO_EN=$BD_HOST"
elegir_imagen_pg

sql() { herramienta_pg psql -h "$(host_para_docker)" -p "$BD_PUERTO" -U "$DB_USERNAME" -d "$BD_BASE" -v ON_ERROR_STOP=1 -tAq -c "$1"; }

# ---- Barrera 2: no es ninguno de los ambientes reales -----------------------------------------------------------------------
if command -v aws > /dev/null 2>&1; then
  if reales="$(aws ssm get-parameters-by-path --region "${AWS_REGION:-${AWS_DEFAULT_REGION:-us-east-2}}" --path /cundiapp/ --recursive \
      --with-decryption --query "Parameters[?ends_with(Name, '/DB_URL')].Value" --output text 2> /dev/null)" && [[ -n "$reales" ]]; then
    # Se compara solo el servidor; el valor no se imprime nunca.
    if grep -qF -- "$BD_HOST" <<< "$reales"; then
      fallar "$BD_HOST es el servidor de la base de un ambiente REAL (Parameter Store /cundiapp/*). Este ensayo borra el esquema: no."
    fi
    echo "Barrera: $BD_HOST no es ninguno de los DB_URL de /cundiapp/* en Parameter Store."
  else
    echo "AVISO: no pude leer los DB_URL de Parameter Store; no se comprobo contra los ambientes reales (sigue la barrera de los estudiantes)."
  fi
else
  echo "AVISO: no hay aws en esta maquina; no se comprobo contra los ambientes reales (sigue la barrera de los estudiantes)."
fi

# ---- Barrera 3: ningun estudiante real ----------------------------------------------------------------------------------------
hay_esquema="$(sql "select count(*) from information_schema.schemata where schema_name='cundiapp'")"
if [[ "$hay_esquema" != "0" ]]; then
  hay_tabla="$(sql "select count(*) from information_schema.tables where table_schema='cundiapp' and table_name='estudiante'")"
  if [[ "$hay_tabla" != "0" ]]; then
    reales_en_bd="$(sql "select count(*) from cundiapp.estudiante where correo_institucional not like '$CORREO_SINTETICO'")"
    [[ "$reales_en_bd" == "0" ]] \
      || fallar "$BD_HOST tiene $reales_en_bd estudiante(s) que no son sinteticos: parece una base REAL. Este ensayo no la toca."
  fi
fi

carpeta="$(mktemp -d)"
trap 'rm -rf "$carpeta"' EXIT
fallos=0
inicio="$SECONDS"
comprobar() { # comprobar <descripcion> <comando...>: bien si el comando termina con 0
  local descripcion="$1"
  shift
  if "$@"; then echo "ok       $descripcion ($((SECONDS - inicio)) s)"; else echo "FALLO    $descripcion"; fallos=$((fallos + 1)); fi
}
igual() { [[ "$1" == "$2" ]]; }
sin_diferencias() { diff -q "$1" "$2" > /dev/null; }
archivo_con_datos() { [[ -n "$1" && -s "$1" ]]; }
estado() { sql "$CONSULTA_DE_FILAS"; }
version() { sql "$CONSULTA_DE_VERSION"; }
estudiantes() { sql "select count(*) from cundiapp.estudiante"; }
# restaurar-bd.sh deja el respaldo previo en la carpeta actual: se corre dentro de la carpeta temporal.
restaurar() { (cd "$carpeta" && bash "$AQUI/restaurar-bd.sh" "$@"); }

cuentas() { # cuentas <desde> <hasta>: estudiante + credencial local, en una transaccion (hay un trigger que las exige juntas)
  sql "set search_path=cundiapp;
    begin;
    with e as (
      insert into estudiante(nombres, apellidos, correo_institucional)
      select 'Ensayo ' || g, 'Sintetico', 'ensayo' || g || '@ensayo.invalid' from generate_series($1, $2) g
      returning id_estudiante, correo_institucional)
    insert into credencial_acceso(id_estudiante, proveedor, hash_contrasena, correo_proveedor, correo_verificado)
    select id_estudiante, 'local', 'x', correo_institucional, true from e;
    commit;" > /dev/null
}

echo "1/6 Preparando la base de $BD_HOST: esquema nuevo con Flyway y 25 cuentas sinteticas"
sql "drop schema if exists cundiapp cascade" > /dev/null
CONFIRMO_MIGRAR_EN="$BD_HOST" bash "$AQUI/migrar-a-mano.sh" migrate > /dev/null
cuentas 1 25
version_a="$(version)"
estado > "$carpeta/estado-a.txt"
comprobar "base preparada: version $version_a de Flyway, $(estudiantes) cuentas" igual "$(estudiantes)" 25

echo "2/6 Respaldando el estado A y ensuciando la base (5 cuentas mas = estado B)"
bash "$AQUI/respaldar-bd.sh" "$carpeta/respaldo-a.dump" > /dev/null
cuentas 26 30
estado > "$carpeta/estado-b.txt"
comprobar "la base cambio: ahora tiene $(estudiantes) cuentas (estado B)" igual "$(estudiantes)" 30

echo "3/6 Restaurar sin confirmacion tiene que negarse"
if restaurar "$carpeta/respaldo-a.dump" > "$carpeta/sin-confirmar.log" 2>&1; then
  comprobar "se nego sin CONFIRMO_RESTAURAR_EN" false
else
  comprobar "se nego sin CONFIRMO_RESTAURAR_EN y la base sigue en B" igual "$(estudiantes)" 30
fi

echo "4/6 Un respaldo danado tiene que fallar sin tocar la base"
# Se corta por la mitad: la cabecera se lee, pero el archivo no llega al final.
head -c "$(($(wc -c < "$carpeta/respaldo-a.dump") / 2))" "$carpeta/respaldo-a.dump" > "$carpeta/danado.dump"
if CONFIRMO_RESTAURAR_EN="$BD_HOST" restaurar "$carpeta/danado.dump" > "$carpeta/danado.log" 2>&1; then
  comprobar "el respaldo danado fue rechazado" false
else
  comprobar "el respaldo danado fue rechazado" true
fi
estado > "$carpeta/estado-tras-danado.txt"
comprobar "la base no se toco (sigue en B)" sin_diferencias "$carpeta/estado-b.txt" "$carpeta/estado-tras-danado.txt"

echo "5/6 Restaurar el respaldo bueno: la base tiene que volver exactamente al estado A"
sleep 1 # el respaldo previo lleva la hora en el nombre
if CONFIRMO_RESTAURAR_EN="$BD_HOST" restaurar "$carpeta/respaldo-a.dump" > "$carpeta/restaurar.log" 2>&1; then
  comprobar "la restauracion termino sin error" true
else
  cat "$carpeta/restaurar.log" >&2
  comprobar "la restauracion termino sin error" false
fi
estado > "$carpeta/estado-tras-restaurar.txt"
comprobar "tabla por tabla, la base quedo igual al estado A" sin_diferencias "$carpeta/estado-a.txt" "$carpeta/estado-tras-restaurar.txt"
comprobar "la version de Flyway es la del respaldo ($(version))" igual "$(version)" "$version_a"
previo="$(find "$carpeta" -maxdepth 1 -name 'antes-de-restaurar-*.dump' | head -n1)"
comprobar "dejo el respaldo previo de lo que habia (antes-de-restaurar-*.dump)" archivo_con_datos "$previo"

echo "6/6 Deshacer: restaurar ese respaldo previo tiene que devolver la base al estado B"
if [[ -n "$previo" ]]; then
  sleep 1
  if CONFIRMO_RESTAURAR_EN="$BD_HOST" restaurar "$previo" > "$carpeta/deshacer.log" 2>&1; then
    comprobar "deshacer termino sin error" true
  else
    cat "$carpeta/deshacer.log" >&2
    comprobar "deshacer termino sin error" false
  fi
  estado > "$carpeta/estado-tras-deshacer.txt"
  comprobar "deshacer devolvio la base al estado B" sin_diferencias "$carpeta/estado-b.txt" "$carpeta/estado-tras-deshacer.txt"
else
  comprobar "deshacer (no hay respaldo previo que restaurar)" false
fi

if [[ "$fallos" -gt 0 ]]; then
  echo "$fallos comprobacion(es) fallaron. NO des por probado restaurar-bd.sh en $BD_HOST." >&2
  exit 1
fi
echo "OK: restaurar-bd.sh se nego sin confirmar, rechazo un respaldo danado sin tocar la base, restauro el estado exacto y se pudo deshacer ($((SECONDS - inicio)) s en $BD_HOST)."
echo "La base queda con datos sinteticos de ensayo: si es un proyecto de Supabase desechable, borralo."
