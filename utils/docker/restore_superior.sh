#!/bin/bash
# =============================================================================
# restore_superior.sh
#
# Restaura la base de datos de la instancia SUPERIOR (provincial) en el
# contenedor genis_postgres, aplica todos los fixes de datos, y exporta
# TRACE a un archivo CSV para transferir a la instancia inferior.
#
# Uso:
#   ./restore_superior.sh
#
# Prerequisitos:
#   - El contenedor genis_postgres debe estar corriendo.
#   - GENis NO debe estar corriendo (para evitar conexiones activas).
#   - El archivo de dump debe estar en el mismo directorio que este script:
#       postgresREG.sql → instancia superior (provincial) → genisdb
#
# Resultado:
#   - trace_data.csv generado en el mismo directorio que este script.
#     Transferir ese archivo al servidor de la instancia inferior antes de
#     ejecutar restore_inferior.sh.
# =============================================================================

set -euo pipefail

CONTAINER="genis_postgres"
PG_SUPER_USER="postgres"
PG_SUPER_PASS="postgres"
APP_USER="genissqladmin"
APP_PASS="genissqladminp"

DUMP_SUPERIOR="postgresREG.sql"
TRACE_CSV="trace_data.csv"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Helpers
run_super() {
    docker exec -e PGPASSWORD="$PG_SUPER_PASS" "$CONTAINER" \
        psql -U "$PG_SUPER_USER" -v ON_ERROR_STOP=1 "$@"
}

run_app() {
    local db="$1"; shift
    docker exec -e PGPASSWORD="$APP_PASS" "$CONTAINER" \
        psql -U "$APP_USER" -d "$db" -v ON_ERROR_STOP=1 "$@"
}

run_app_sql() {
    local db="$1"
    local sql="$2"
    docker exec -e PGPASSWORD="$APP_PASS" "$CONTAINER" \
        psql -U "$APP_USER" -d "$db" -v ON_ERROR_STOP=1 -c "$sql"
}

# =============================================================================
# 0. Validaciones previas
# =============================================================================
echo "==> Verificando prerequisitos..."

if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
    echo "ERROR: El contenedor '${CONTAINER}' no está corriendo."
    exit 1
fi

if [ ! -f "${SCRIPT_DIR}/${DUMP_SUPERIOR}" ]; then
    echo "ERROR: No se encontró el archivo de dump: ${SCRIPT_DIR}/${DUMP_SUPERIOR}"
    exit 1
fi

echo "    OK"

# =============================================================================
# 1. Copiar dump al contenedor
# =============================================================================
echo "==> Copiando dump al contenedor..."
docker cp "${SCRIPT_DIR}/${DUMP_SUPERIOR}" "${CONTAINER}:/tmp/${DUMP_SUPERIOR}"
echo "    OK"

# =============================================================================
# 2. Recrear genisdb y restaurar dump
# =============================================================================
echo "==> Terminando conexiones activas a genisdb..."
run_super -c "SELECT pg_terminate_backend(pid)
              FROM pg_stat_activity
              WHERE datname = 'genisdb' AND pid <> pg_backend_pid();"

echo "==> Eliminando y recreando genisdb..."
run_super -c "DROP DATABASE IF EXISTS genisdb;"
run_super -c "CREATE DATABASE genisdb OWNER ${APP_USER};"

echo "==> Restaurando genisdb desde ${DUMP_SUPERIOR}..."
run_app genisdb -f "/tmp/${DUMP_SUPERIOR}"
echo "    OK"

# =============================================================================
# 3. !! CRÍTICO !! Fixes PRE-EVOLUTIONS
#    Ejecutar ANTES de iniciar GENis y correr evolutions
# =============================================================================
echo "==> [SUPERIOR] Aplicando fixes DATE_UPLOADED y DATE_RECEIVED (pre-evolutions)..."
docker exec -e PGPASSWORD="$APP_PASS" "$CONTAINER" \
    psql -U "$APP_USER" -d genisdb -v ON_ERROR_STOP=1 <<'SQL'
-- DATE_UPLOADED: fecha más temprana de KIND = 'interconectionUpdload' en TRACE
UPDATE "APP"."PROFILE_UPLOADED" pu
SET "DATE_UPLOADED" = to_char(t_min_date."min_upload_date", 'YYYY-MM-DD HH24:MI:SS')
FROM (
    SELECT
        t."PROFILE",
        MIN(t."DATE") AS min_upload_date
    FROM "APP"."TRACE" t
    WHERE t."KIND" = 'interconectionUpdload'
    GROUP BY t."PROFILE"
) AS t_min_date
WHERE pu."GLOBAL_CODE" = t_min_date."PROFILE";

-- DATE_RECEIVED: preferir fecha de 'importedFromInferior', si no hay usar MIN(DATE)
UPDATE "APP"."PROFILE_RECEIVED" pr
SET "DATE_RECEIVED" = subquery."determined_date"
FROM (
    SELECT
        t."PROFILE",
        COALESCE(
            MIN(CASE WHEN t."KIND" = 'importedFromInferior' THEN t."DATE" END),
            MIN(t."DATE")
        ) AS "determined_date"
    FROM "APP"."TRACE" t
    GROUP BY t."PROFILE"
) AS subquery
WHERE pr."GLOBAL_CODE" = subquery."PROFILE";
SQL
echo "    OK"

# =============================================================================
# 4. Exportar TRACE a CSV (para transferir a la instancia inferior)
# =============================================================================
echo "==> Exportando TRACE a /tmp/${TRACE_CSV} dentro del contenedor..."
run_app_sql genisdb \
    "COPY \"APP\".\"TRACE\" TO '/tmp/${TRACE_CSV}' CSV HEADER;"

echo "==> Copiando ${TRACE_CSV} al directorio del script..."
docker cp "${CONTAINER}:/tmp/${TRACE_CSV}" "${SCRIPT_DIR}/${TRACE_CSV}"
echo "    Archivo generado: ${SCRIPT_DIR}/${TRACE_CSV}"
echo "    *** Transferir este archivo al servidor de la instancia inferior ***"
echo "    OK"

# =============================================================================
# 5. Fixes EN TODAS LAS INSTANCIAS
# =============================================================================
echo "==> [SUPERIOR] Aplicando fixes comunes a todas las instancias..."
docker exec -e PGPASSWORD="$APP_PASS" "$CONTAINER" \
    psql -U "$APP_USER" -d genisdb -v ON_ERROR_STOP=1 <<'SQL'
-- isDesktop:false en NOTIFICATION cuando KIND = 'matching'
UPDATE "APP"."NOTIFICATION"
SET "INFO" = jsonb_set(
    "INFO"::jsonb,
    '{isDesktop}',
    'false'::jsonb,
    true
)::text
WHERE "KIND" = 'matching'
  AND "INFO"::jsonb ? 'isDesktop' = false;

-- userName:USER en TRACE cuando KIND = 'categoryModification'
UPDATE "APP"."TRACE"
SET "TRACE" = jsonb_set(
    "TRACE"::jsonb,
    '{userName}',
    to_jsonb("USER"::text),
    true
)::character varying
WHERE "KIND" = 'categoryModification';

-- multiallelic:false en TRACE cuando incluye 'categoryConfiguration' y falta la clave
UPDATE "APP"."TRACE" AS t
SET "TRACE" = jsonb_set(
    t."TRACE"::jsonb,
    '{categoryConfiguration, multiallelic}',
    to_jsonb(false),
    true
)::character varying
WHERE t."TRACE"::jsonb ? 'categoryConfiguration'
  AND NOT (t."TRACE"::jsonb -> 'categoryConfiguration' ? 'multiallelic');
SQL
echo "    OK"

# =============================================================================
# 6. Fixes SOLO INSTANCIA SUPERIOR — motivo de rechazo en TRACE
# =============================================================================
echo "==> [SUPERIOR] Aplicando fix interconectionRejected motive..."
docker exec -e PGPASSWORD="$APP_PASS" "$CONTAINER" \
    psql -U "$APP_USER" -d genisdb -v ON_ERROR_STOP=1 <<'SQL'
-- Completar TRACE con el motive de PROFILE_UPLOADED cuando KIND = 'interconectionRejected'
UPDATE "APP"."TRACE" t
SET "TRACE" = json_build_object('motive', pu."MOTIVE")::text
FROM "APP"."PROFILE_UPLOADED" pu
WHERE t."PROFILE" = pu."GLOBAL_CODE"
  AND t."KIND"    = 'interconectionRejected'
  AND pu."MOTIVE" IS NOT NULL
  AND t."TRACE" IS DISTINCT FROM json_build_object('motive', pu."MOTIVE")::text;

-- TRACE con motive vacío para rechazados sin datos
UPDATE "APP"."TRACE"
SET "TRACE" = json_build_object('motive', '')::text
WHERE "KIND" = 'interconectionRejected'
  AND ("TRACE" IS NULL OR btrim("TRACE") = '{}');
SQL
echo "    OK"

# =============================================================================
# 7. Limpieza de archivos temporales en el contenedor
# =============================================================================
echo "==> Limpiando archivos temporales en el contenedor..."
docker exec "$CONTAINER" rm -f \
    "/tmp/${DUMP_SUPERIOR}" \
    "/tmp/${TRACE_CSV}"
echo "    OK"

# =============================================================================
echo ""
echo "============================================================"
echo "  Restore y fixes SUPERIOR completados."
echo ""
echo "  genisdb (superior/provincial) → restaurado + fixes aplicados"
echo ""
echo "  PRÓXIMOS PASOS:"
echo "  1. Transferir ${SCRIPT_DIR}/${TRACE_CSV}"
echo "     al servidor de la instancia inferior (mismo directorio"
echo "     que restore_inferior.sh)"
echo "  2. En el servidor inferior ejecutar: ./restore_inferior.sh"
echo "  3. Iniciar GENis superior → corre evolutions automáticamente"
echo "============================================================"