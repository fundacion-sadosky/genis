#!/usr/bin/env bash
#
# restore_mongo_backup.sh
#
# Recrea la base "pdgdb" en el contenedor genis_mongo a partir de una
# carpeta generada por mongodump (colecciones .bson + .metadata.json).
#
# Uso:
#   ./restore_mongo_backup.sh <carpeta_dump>
#
# <carpeta_dump> puede ser la carpeta que contiene directamente los
# archivos .bson, o cualquier carpeta padre que contenga en algún nivel
# una subcarpeta "pdgdb" (el script la busca automáticamente), por ej.:
#   /home/genis-user/backups/mongodump_pdgdb_20260710
#     └── mongodump_20260710/pdgdb/*.bson
#
# Prerequisitos:
#   - El contenedor genis_mongo debe estar corriendo
#     (ver utils/docker/docker-compose.yml).
#   - GENis NO debe estar corriendo (para evitar conexiones activas).
#
# El script DROPEA y RECREA la base pdgdb — se pierde todo lo que haya
# en esa base dentro del contenedor.

set -euo pipefail

CONTAINER="${GENIS_MONGO_CONTAINER:-genis_mongo}"
DB="pdgdb"

if [[ $# -ne 1 ]]; then
  echo "Uso: $0 <carpeta_dump>" >&2
  exit 1
fi

DUMP_DIR_HOST="$1"

if [[ ! -d "$DUMP_DIR_HOST" ]]; then
  echo "ERROR: no se encontró la carpeta de dump: ${DUMP_DIR_HOST}" >&2
  exit 1
fi

echo "==> Buscando carpeta '${DB}' dentro de ${DUMP_DIR_HOST}..."
PDGDB_DIR_HOST="$(find "$DUMP_DIR_HOST" -type d -name "$DB" | head -n1)"
if [[ -z "$PDGDB_DIR_HOST" ]]; then
  echo "ERROR: no se encontró ninguna carpeta '${DB}' dentro de ${DUMP_DIR_HOST}" >&2
  exit 1
fi
echo "    OK: ${PDGDB_DIR_HOST}"

echo "==> Verificando que el contenedor '${CONTAINER}' esté corriendo..."
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
  echo "ERROR: El contenedor '${CONTAINER}' no está corriendo." >&2
  exit 1
fi
echo "    OK"

DUMP_DIR_CONTAINER="/tmp/restore_${DB}_$(date +%s)"

echo "==> Copiando dump al contenedor..."
docker exec "$CONTAINER" mkdir -p "$DUMP_DIR_CONTAINER"
docker cp "${PDGDB_DIR_HOST}/." "${CONTAINER}:${DUMP_DIR_CONTAINER}"
echo "    OK"

echo "==> Eliminando base '${DB}' existente..."
docker exec -i "$CONTAINER" mongo "$DB" --quiet --eval "db.dropDatabase()"
echo "    OK"

echo "==> Restaurando '${DB}' desde $(basename "$PDGDB_DIR_HOST")..."
docker exec "$CONTAINER" mongorestore --db "$DB" "$DUMP_DIR_CONTAINER"
echo "    OK"

echo "==> Limpiando dump temporal del contenedor..."
docker exec "$CONTAINER" rm -rf "$DUMP_DIR_CONTAINER"
echo "    OK"

echo
echo "============================================================"
echo "  Restore completado: '${DB}' recreada en el contenedor"
echo "  '${CONTAINER}' a partir de: ${PDGDB_DIR_HOST}"
echo "============================================================"