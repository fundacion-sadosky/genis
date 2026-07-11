#!/usr/bin/env bash
#
# Genera los INSERT de un kit STR (tabla STRKIT) y sus loci (STRKIT_LOCUS)
# para poder insertarlos en otra base de datos GENis.
#
# Uso:
#   ./export_strkit.sh <KIT_ID> [archivo_salida.sql]
#
# Variables de entorno opcionales (default = conexión legacy local):
#   PGHOST PGPORT PGDATABASE PGUSER PGPASSWORD
#
# Nota: los loci referenciados en STRKIT_LOCUS ("LOCUS") deben existir
# previamente en la tabla LOCUS de la base destino (son datos de referencia,
# no se exportan acá).

set -euo pipefail

KIT_ID="${1:?Uso: $0 <KIT_ID> [archivo_salida.sql]}"
OUT_FILE="${2:-strkit_${KIT_ID}.sql}"

: "${PGHOST:=localhost}"
: "${PGPORT:=5432}"
: "${PGDATABASE:=genisdb}"
: "${PGUSER:=genissqladmin}"
: "${PGPASSWORD:=genissqladminp}"
export PGHOST PGPORT PGDATABASE PGUSER PGPASSWORD

STRKIT_SQL=$(psql -X -q -t -A -v ON_ERROR_STOP=1 -v kit_id="${KIT_ID}" <<'SQL'
  SELECT format(
    'INSERT INTO "APP"."STRKIT" ("ID", "NAME", "TYPE", "LOCI_QTY", "REPRESENTATIVE_PARAMETER") VALUES (%L, %L, %L, %L, %L) ON CONFLICT ("ID") DO NOTHING;',
    "ID", "NAME", "TYPE", "LOCI_QTY", "REPRESENTATIVE_PARAMETER"
  )
  FROM "APP"."STRKIT"
  WHERE "ID" = :'kit_id';
SQL
)

if [[ -z "$STRKIT_SQL" ]]; then
  echo "Error: no existe ningún STRKIT con ID = '${KIT_ID}' en ${PGDATABASE}@${PGHOST}:${PGPORT}" >&2
  exit 1
fi

STRKIT_LOCUS_SQL=$(psql -X -q -t -A -v ON_ERROR_STOP=1 -v kit_id="${KIT_ID}" <<'SQL'
  SELECT format(
    'INSERT INTO "APP"."STRKIT_LOCUS" ("STRKIT", "LOCUS", "FLUOROPHORE", "ORDER") VALUES (%L, %L, %L, %L) ON CONFLICT ("STRKIT", "LOCUS") DO NOTHING;',
    "STRKIT", "LOCUS", "FLUOROPHORE", "ORDER"
  )
  FROM "APP"."STRKIT_LOCUS"
  WHERE "STRKIT" = :'kit_id'
  ORDER BY "ORDER";
SQL
)

{
  echo "-- INSERTs del kit '${KIT_ID}' generados el $(date -Iseconds) desde ${PGDATABASE}@${PGHOST}:${PGPORT}"
  echo "BEGIN;"
  echo
  echo "$STRKIT_SQL"
  echo
  echo "$STRKIT_LOCUS_SQL"
  echo
  echo "COMMIT;"
} > "$OUT_FILE"

echo "Generado: $OUT_FILE ($(grep -c '^INSERT' "$OUT_FILE") INSERTs)"