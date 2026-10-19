#!/usr/bin/env bash
# Prueba la comparacion de filas de probar-respaldo.sh con casos buenos y malos (12 factores, factor 12, SCRUM-219).
# No usa Docker ni base de datos: corre en segundos en el CI. Un verificador que solo se ha visto decir "OK" no demuestra nada;
# aqui se comprueba que tambien sabe decir "NO".
set -euo pipefail
cd "$(dirname "$0")"

carpeta="$(mktemp -d)"
trap 'rm -rf "$carpeta"' EXIT
fallos=0

# caso "nombre" esperado(0|1) "antes" "despues" "copia"   (cada uno: lineas tabla|filas separadas por \n)
caso() {
  local nombre="$1" esperado="$2"
  printf '%b\n' "$3" > "$carpeta/antes.txt"
  printf '%b\n' "$4" > "$carpeta/despues.txt"
  printf '%b\n' "$5" > "$carpeta/copia.txt"
  local salida obtenido=0
  salida="$(awk -F'|' -f comparar-filas.awk "$carpeta/antes.txt" "$carpeta/despues.txt" "$carpeta/copia.txt")" || obtenido=1
  if [[ "$obtenido" -ne "$esperado" ]]; then
    echo "FALLA: $nombre (esperaba salida $esperado, dio $obtenido)"
    echo "$salida"
    fallos=1
  else
    echo "ok:    $nombre"
  fi
}

caso "copia identica"                         0 "estudiante|25\nsesion|4" "estudiante|25\nsesion|4" "estudiante|25\nsesion|4"
caso "tabla vacia identica"                   0 "aviso|0"                "aviso|0"                "aviso|0"
caso "a la copia le faltan filas"             1 "estudiante|25"          "estudiante|25"          "estudiante|24"
caso "a la copia le sobran filas"             1 "estudiante|25"          "estudiante|25"          "estudiante|26"
caso "a la copia le falta una tabla"          1 "estudiante|25\nsesion|4" "estudiante|25\nsesion|4" "estudiante|25"
caso "la copia trae una tabla que no existe"  1 "estudiante|25"          "estudiante|25"          "estudiante|25\nfantasma|1"
caso "cambio durante el respaldo, dentro"     0 "sesion|4"               "sesion|6"               "sesion|5"
caso "cambio durante el respaldo, en el borde" 0 "sesion|4"              "sesion|6"               "sesion|6"
caso "cambio durante el respaldo, fuera"      1 "sesion|4"               "sesion|6"               "sesion|9"
caso "la tabla bajo durante el respaldo"      0 "sesion|6"               "sesion|4"               "sesion|5"
caso "una tabla bien y otra mal"              1 "a|1\nb|2"               "a|1\nb|2"               "a|1\nb|3"
# 9 contra 10: comparadas como texto, "9" > "10"; tienen que compararse como numeros.
caso "numeros de distinta cantidad de cifras" 0 "sesion|9"               "sesion|10"              "sesion|10"

exit "$fallos"
