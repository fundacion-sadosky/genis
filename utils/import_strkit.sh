#!/usr/bin/env bash
#
# Aplica en otra base de datos GENis el archivo de INSERTs generado por
# export_strkit.sh.
#
# Uso:
#   ./import_strkit.sh <archivo.sql>
#
# Variables de entorno opcionales (apuntar a la base DESTINO, default =
# conexión legacy local):
#   PGHOST PGPORT PGDATABASE PGUSER PGPASSWORD
#
# Nota: los loci referenciados ("LOCUS") deben existir de antemano en la
# tabla LOCUS de la base destino; si falta alguno la importación falla y
# no queda nada aplicado (el archivo va envuelto en BEGIN/COMMIT).

set -euo pipefail

SQL_FILE="${1:?Uso: $0 <archivo.sql>}"

if [[ ! -f "$SQL_FILE" ]]; then
  echo "Error: no existe el archivo '${SQL_FILE}'" >&2
  exit 1
fi

: "${PGHOST:=localhost}"
: "${PGPORT:=5432}"
: "${PGDATABASE:=genisdb}"
: "${PGUSER:=genissqladmin}"
: "${PGPASSWORD:=genissqladminp}"
export PGHOST PGPORT PGDATABASE PGUSER PGPASSWORD

echo "Aplicando ${SQL_FILE} en ${PGDATABASE}@${PGHOST}:${PGPORT}..."

if ! RESULT=$(psql -X -v ON_ERROR_STOP=1 -f "$SQL_FILE" 2>&1); then
  echo "$RESULT" >&2
  echo "Error: falló la importación, no se aplicó ningún cambio (rollback automático)." >&2
  exit 1
fi

INSERTED=$(grep -c '^INSERT 0 1$' <<<"$RESULT" || true)
SKIPPED=$(grep -c '^INSERT 0 0$' <<<"$RESULT" || true)

echo "OK: ${INSERTED} fila(s) insertada(s), ${SKIPPED} ya existían (ON CONFLICT DO NOTHING)."