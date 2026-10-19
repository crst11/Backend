# Compara las filas por tabla del origen (antes y despues del respaldo) con las de la copia restaurada (SCRUM-219).
# Uso: awk -F'|' -f comparar-filas.awk antes.txt despues.txt copia.txt      (cada linea: tabla|filas)
# Los archivos se distinguen por el nombre: tienen que llamarse *antes*, *despues* y *copia*.
#
# Una base viva cambia mientras se respalda. Si una tabla tuvo las mismas filas antes y despues, la copia tiene que tener
# exactamente esas. Si cambio, la copia tiene que quedar entre las dos cifras y se avisa. Sale con 1 si algo no cuadra.
FILENAME ~ /antes/   { antes[$1] = $2; orden[++n] = $1; next }
FILENAME ~ /despues/ { despues[$1] = $2; next }
FILENAME ~ /copia/   { copia[$1] = $2; next }
END {
  malas = 0; filas = 0
  for (i = 1; i <= n; i++) {
    t = orden[i]; a = antes[t]; d = despues[t]; c = copia[t]
    if (c == "") { printf "FALTA   %s: esta en el origen y no en la copia\n", t; malas++; continue }
    filas += c
    if (a == d) {
      if (c + 0 != a + 0) { printf "DIFIERE %s: origen %s, copia %s\n", t, a, c; malas++ }
    } else {
      lo = (a + 0 < d + 0) ? a + 0 : d + 0
      hi = (a + 0 < d + 0) ? d + 0 : a + 0
      if (c + 0 < lo || c + 0 > hi) { printf "DIFIERE %s: origen %s a %s durante el respaldo, copia %s\n", t, a, d, c; malas++ }
      else printf "AVISO   %s: cambio durante el respaldo (%s a %s); la copia tiene %s\n", t, a, d, c
    }
  }
  for (t in copia) if (!(t in antes)) { printf "SOBRA   %s: esta en la copia y no en el origen\n", t; malas++ }
  printf "%d tablas comparadas, %d filas en la copia\n", n, filas
  exit (malas > 0) ? 1 : 0
}
