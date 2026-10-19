#!/usr/bin/env bash
# Prueba infra/imagen-a-promover.sh (12 factores, factor 5, SCRUM-215) con repositorios de git de mentira y un "aws" que
# solo sabe decir si una etiqueta existe. No toca el repositorio real ni AWS. Sale con 1 si algun caso falla.
set -uo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
SCRIPT="$RAIZ/infra/imagen-a-promover.sh"
TEMPORAL="$(mktemp -d)"
trap 'rm -rf "$TEMPORAL"' EXIT

export GIT_AUTHOR_NAME=prueba GIT_AUTHOR_EMAIL=prueba@ejemplo.invalid GIT_COMMITTER_NAME=prueba GIT_COMMITTER_EMAIL=prueba@ejemplo.invalid

# "aws ecr describe-images ... --image-ids imageTag=X": existe (0) si X esta en IMAGENES_EXISTENTES.
mkdir -p "$TEMPORAL/bin"
cat > "$TEMPORAL/bin/aws" <<'EOF'
#!/usr/bin/env bash
for argumento in "$@"; do
  case "$argumento" in
    imageTag=*) etiqueta="${argumento#imageTag=}" ;;
  esac
done
for existente in $IMAGENES_EXISTENTES; do
  [ "$existente" = "${etiqueta:-}" ] && exit 0
done
exit 254
EOF
chmod +x "$TEMPORAL/bin/aws"
export PATH="$TEMPORAL/bin:$PATH"

fallos=0
caso=0

# Un repositorio con produccion (contenido viejo) y preproduccion (contenido nuevo, dos commits).
nuevo_repositorio() {
  local carpeta="$TEMPORAL/repo$((++caso))"
  mkdir -p "$carpeta" && cd "$carpeta" || exit 1
  git init -q -b produccion
  git config core.autocrlf false
  echo viejo > app.txt && git add . && git commit -q -m "produccion v1"
  git checkout -q -b preproduccion
  echo uno > app.txt && git add . && git commit -q -m "cambio 1"
  echo dos >> app.txt && git add . && git commit -q -m "cambio 2"
  git checkout -q produccion
}

verificar() {
  local descripcion="$1" esperado="$2" obtenido
  obtenido="$(bash "$SCRIPT")"
  if [ "$obtenido" = "$esperado" ]; then
    echo "ok    $descripcion"
  else
    echo "FALLA $descripcion: se esperaba «$esperado» y salio «$obtenido»"
    fallos=$((fallos + 1))
  fi
}

# 1. Promocion directa: el segundo padre es la punta de preproduccion y su imagen existe.
nuevo_repositorio
git merge -q --no-ff preproduccion -m "promover"
punta="$(git rev-parse --short=7 preproduccion)"
IMAGENES_EXISTENTES="preprod-$punta" verificar "promocion directa con la imagen en ECR" "preprod-$punta"

# 2. La imagen de preproduccion ya no esta en ECR (la politica conserva solo las 2 ultimas): hay que construir.
IMAGENES_EXISTENTES="preprod-otra" verificar "la imagen ya no esta en ECR: no se promueve" ""

# 3. Rama de promocion que une la historia de produccion con "-s ours" (el caso de SCRUM-46): el padre es la rama,
#    que no tiene imagen, y la imagen es la de su primer padre, la punta de preproduccion.
nuevo_repositorio
git checkout -q -b promocion preproduccion
git merge -q -s ours produccion -m "unir la historia de produccion"
git checkout -q produccion
git merge -q --no-ff promocion -m "promover por la rama de promocion"
punta="$(git rev-parse --short=7 preproduccion)"
IMAGENES_EXISTENTES="preprod-$punta" verificar "rama de promocion: se salta hasta la punta de preproduccion" "preprod-$punta"

# 4. Con dos imagenes para el mismo contenido gana la mas reciente.
IMAGENES_EXISTENTES="preprod-$punta preprod-$(git rev-parse --short=7 promocion)" verificar "gana la imagen mas cercana al commit" "preprod-$(git rev-parse --short=7 promocion)"

# 5. El contenido cambio en la fusion (un conflicto resuelto a mano): la imagen de preproduccion NO es lo que se despliega.
nuevo_repositorio
git merge -q --no-ff --no-commit preproduccion
echo "arreglo a mano en la fusion" >> app.txt && git add . && git commit -q -m "promover con un cambio a mano"
punta="$(git rev-parse --short=7 preproduccion)"
IMAGENES_EXISTENTES="preprod-$punta" verificar "el contenido cambio en la fusion: no se promueve aunque exista la imagen" ""

# 6. Un commit que no es una fusion (un arreglo directo en produccion): se construye.
nuevo_repositorio
echo "arreglo directo" >> app.txt && git add . && git commit -q -m "arreglo directo en produccion"
IMAGENES_EXISTENTES="preprod-$(git rev-parse --short=7 preproduccion)" verificar "un commit que no es una fusion no se promueve" ""

# 7. Fusion de algo que no es preproduccion (otro contenido): no coincide ninguna imagen.
nuevo_repositorio
git checkout -q -b otra produccion
echo otro > app.txt && git add . && git commit -q -m "otra rama"
git checkout -q produccion
git merge -q --no-ff otra -m "fusion de otra rama"
IMAGENES_EXISTENTES="preprod-$(git rev-parse --short=7 preproduccion)" verificar "la fusion trae un contenido que ninguna imagen de preproduccion tiene" ""

echo
if [ "$fallos" -eq 0 ]; then echo "Todos los casos pasan."; else echo "$fallos caso(s) fallan." >&2; exit 1; fi
