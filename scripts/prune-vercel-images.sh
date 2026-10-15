#!/usr/bin/env bash
# Borra del Vercel Container Registry las imágenes con más de MAX_AGE_DAYS días,
# en todos los repositorios del proyecto. Nunca borra la imagen que sirve producción.
#
# Requiere el CLI de Vercel logueado (`vercel login`) o VERCEL_TOKEN, y jq.
#
# Uso:
#   ./scripts/prune-vercel-images.sh             # borra
#   ./scripts/prune-vercel-images.sh --dry-run   # solo muestra qué borraría
#
# Variables opcionales:
#   VERCEL_PROJECT  proyecto de Vercel (default: split-it)
#   VERCEL_SCOPE    team de Vercel (default: el scope actual del CLI)
#   VERCEL_TOKEN    token, para correrlo sin `vercel login` (por ejemplo, en CI)
#   MAX_AGE_DAYS    antigüedad a partir de la cual se borra (default: 7)
set -euo pipefail

PROJECT="${VERCEL_PROJECT:-split-it}"
MAX_AGE_DAYS="${MAX_AGE_DAYS:-7}"
DRY_RUN=false

case "${1:-}" in
  "") ;;
  --dry-run) DRY_RUN=true ;;
  *)
    echo "Uso: $0 [--dry-run]" >&2
    exit 2
    ;;
esac

for cmd in vercel jq; do
  if ! command -v "$cmd" > /dev/null; then
    echo "❌  Falta '$cmd' en el PATH." >&2
    exit 1
  fi
done

export NO_UPDATE_NOTIFIER=1
VERCEL_CMD=(vercel --non-interactive)
if [ -n "${VERCEL_TOKEN:-}" ]; then VERCEL_CMD+=(--token "$VERCEL_TOKEN"); fi
if [ -n "${VERCEL_SCOPE:-}" ]; then VERCEL_CMD+=(--scope "$VERCEL_SCOPE"); fi

ERR_LOG=$(mktemp)
trap 'rm -f "$ERR_LOG"' EXIT

# Corre el CLI de Vercel. Su stderr (banner, spinners) solo se muestra si falla.
vc() {
  if ! "${VERCEL_CMD[@]}" "$@" 2> "$ERR_LOG" < /dev/null; then
    cat "$ERR_LOG" >&2
    return 1
  fi
}

# Imprime, una por línea, cada entrada de `.<key>` de un listado paginado del CLI.
list_all() {
  local key=$1 cursor="" page
  shift
  while :; do
    local args=("$@" --json --limit 100)
    if [ -n "$cursor" ]; then args+=(--next "$cursor"); fi
    page=$(vc "${args[@]}")
    jq -c ".${key}[]" <<< "$page"
    cursor=$(jq -r '.nextCursor // empty' <<< "$page")
    if [ -z "$cursor" ]; then break; fi
  done
}

# Tag de la imagen que sirve producción: el build de Vercel etiqueta cada imagen
# con los primeros 12 caracteres del commit desplegado.
production_tag() {
  local domain sha
  domain=$(vc api "/v9/projects/$PROJECT" | jq -r '.targets.production.alias[0] // empty')
  if [ -z "$domain" ]; then return 1; fi
  sha=$(vc api "/v13/deployments/$domain" | jq -r '.meta.githubCommitSha // empty')
  if [ -z "$sha" ]; then return 1; fi
  echo "${sha:0:12}"
}

if ! PROD_TAG=$(production_tag); then
  echo "❌  No se pudo determinar qué imagen sirve producción en '$PROJECT'. No se borra nada." >&2
  exit 1
fi

CUTOFF=$(($(date +%s) - MAX_AGE_DAYS * 86400))

echo "🧹  $PROJECT: imágenes con más de $MAX_AGE_DAYS días (producción usa la imagen $PROD_TAG)"
if $DRY_RUN; then echo "   (dry run: no se borra nada)"; fi

deleted=0
freed_bytes=0
failed=0

repositories=$(list_all repositories vcr ls --project "$PROJECT" | jq -r '.name')

for repo in $repositories; do
  old_images=$(
    list_all images vcr image ls "$repo" --project "$PROJECT" |
      jq -r --argjson cutoff "$CUTOFF" '
        select((.createdAt | sub("\\.[0-9]+Z$"; "Z") | fromdateiso8601) < $cutoff)
        | [.id, .createdAt, .sizeInBytes, (.tags | join(","))]
        | @tsv'
  )
  if [ -z "$old_images" ]; then continue; fi

  echo ""
  echo "📦  $repo"
  while IFS=$'\t' read -r id created_at size_bytes tags; do
    description="$id ($created_at, $((size_bytes / 1048576)) MB, tags: ${tags:-ninguno})"

    if [[ ",$tags," == *",$PROD_TAG,"* ]]; then
      echo "   🔒  Se conserva, la usa producción: $description"
      continue
    fi

    if $DRY_RUN; then
      echo "   -  Se borraría: $description"
    elif vc vcr image rm "$repo" "$id" --project "$PROJECT" --yes > /dev/null; then
      echo "   🗑️   Borrada: $description"
    else
      echo "   ❌  No se pudo borrar: $description" >&2
      failed=$((failed + 1))
      continue
    fi
    deleted=$((deleted + 1))
    freed_bytes=$((freed_bytes + size_bytes))
  done <<< "$old_images"
done

echo ""
if $DRY_RUN; then
  echo "✅  Se borrarían $deleted imágenes ($((freed_bytes / 1048576)) MB)."
else
  echo "✅  Se borraron $deleted imágenes ($((freed_bytes / 1048576)) MB)."
fi

if [ "$failed" -gt 0 ]; then
  echo "❌  $failed imágenes no se pudieron borrar." >&2
  exit 1
fi
