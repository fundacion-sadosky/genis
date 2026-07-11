#!/usr/bin/env bash
#
# restore_backup.sh
#
# Recrea genisdb y genislogdb en el contenedor genis_postgres a partir de
# dumps en formato custom de pg_dump (pg_restore -Fc), generados por ej.
# con:
#   pg_dump -Fc -d genisdb    > genisdb_backup_YYYYMMDD.dump
#   pg_dump -Fc -d genislogdb > genislogdb_backup_YYYYMMDD.dump
#
# Uso:
#   ./restore_backup.sh <genisdb.dump> <genislogdb.dump>
#
# Prerequisitos:
#   - El contenedor genis_postgres debe estar corriendo
#     (ver utils/docker/docker-compose.yml).
#   - GENis NO debe estar corriendo (para evitar conexiones activas).
#
# El script DROPEA y RECREA genisdb y genislogdb — se pierde todo lo que
# haya en esas bases dentro del contenedor.

set -euo pipefail

CONTAINER="${GENIS_PG_CONTAINER:-genis_postgres}"
PG_SUPER_USER="postgres"
PG_SUPER_PASS="postgres"
APP_USER="genissqladmin"
APP_PASS="genissqladminp"

if [[ $# -ne 2 ]]; then
  echo "Uso: $0 <genisdb.dump> <genislogdb.dump>" >&2
  exit 1
fi

GENISDB_DUMP="$1"
GENISLOGDB_DUMP="$2"

for f in "$GENISDB_DUMP" "$GENISLOGDB_DUMP"; do
  if [[ ! -f "$f" ]]; then
    echo "ERROR: no se encontró el archivo de dump: ${f}" >&2
    exit 1
  fi
done

run_super() {
  docker exec -i -e PGPASSWORD="$PG_SUPER_PASS" "$CONTAINER" \
    psql -U "$PG_SUPER_USER" -v ON_ERROR_STOP=1 "$@"
}

restore_db() {
  local db="$1"
  local dump_host_path="$2"
  local dump_container_path="/tmp/$(basename "$dump_host_path")"

  echo "==> [$db] Terminando conexiones activas..."
  run_super -c "SELECT pg_terminate_backend(pid)
                FROM pg_stat_activity
                WHERE datname = '${db}' AND pid <> pg_backend_pid();"

  echo "==> [$db] Eliminando y recreando la base..."
  run_super -c "DROP DATABASE IF EXISTS ${db};"
  run_super -c "CREATE DATABASE ${db} OWNER ${APP_USER};"

  echo "==> [$db] Copiando dump al contenedor..."
  docker cp "$dump_host_path" "${CONTAINER}:${dump_container_path}"

  echo "==> [$db] Restaurando desde $(basename "$dump_host_path")..."
  docker exec -i -e PGPASSWORD="$APP_PASS" "$CONTAINER" \
    pg_restore -U "$APP_USER" -d "$db" --no-owner --role="$APP_USER" \
      -v "$dump_container_path"

  echo "==> [$db] Limpiando dump temporal del contenedor..."
  docker exec "$CONTAINER" rm -f "$dump_container_path"

  echo "    OK [$db]"
}

echo "==> Verificando que el contenedor '${CONTAINER}' esté corriendo..."
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
  echo "ERROR: El contenedor '${CONTAINER}' no está corriendo." >&2
  exit 1
fi
echo "    OK"

restore_db genisdb "$GENISDB_DUMP"
restore_db genislogdb "$GENISLOGDB_DUMP"

echo
echo "============================================================"
echo "  Restore completado: genisdb y genislogdb recreadas en"
echo "  el contenedor '${CONTAINER}' a partir de:"
echo "    - ${GENISDB_DUMP}"
echo "    - ${GENISLOGDB_DUMP}"
echo "============================================================"