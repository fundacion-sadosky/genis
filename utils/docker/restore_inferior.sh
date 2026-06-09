#!/bin/bash
# =============================================================================
# restore_inferior.sh
#
# Restaura la base de datos de la instancia INFERIOR en el contenedor
# genis_postgres, importa TRACE desde la instancia superior (TRACE2),
# y aplica todos los fixes de datos ANTES de iniciar GENis y correr evolutions.
#
# Uso:
#   ./restore_inferior.sh
#
# Prerequisitos:
#   - El contenedor genis_postgres debe estar corriendo.
#   - GENis NO debe estar corriendo (para evitar conexiones activas).
#   - Los siguientes archivos deben estar en el mismo directorio que este script:
#       postgresLP.sql   → dump de la instancia inferior → genisdb
#       trace_data.csv   → TRACE exportado desde la instancia superior
#                          (generado por restore_superior.sh)
# =============================================================================

set -euo pipefail

CONTAINER="genis_postgres"
PG_SUPER_USER="postgres"
PG_SUPER_PASS="postgres"
APP_USER="genissqladmin"
APP_PASS="genissqladminp"

DUMP_INFERIOR="postgresLP.sql"
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

# =============================================================================
# 0. Validaciones previas
# =============================================================================
echo "==> Verificando prerequisitos..."

if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
    echo "ERROR: El contenedor '${CONTAINER}' no está corriendo."
    exit 1
fi

for f in "$DUMP_INFERIOR" "$TRACE_CSV"; do
    if [ ! -f "${SCRIPT_DIR}/${f}" ]; then
        echo "ERROR: No se encontró el archivo: ${SCRIPT_DIR}/${f}"
        exit 1
    fi
done

echo "    OK"

# =============================================================================
# 1. Copiar archivos al contenedor
# =============================================================================
echo "==> Copiando archivos al contenedor..."
docker cp "${SCRIPT_DIR}/${DUMP_INFERIOR}" "${CONTAINER}:/tmp/${DUMP_INFERIOR}"
docker cp "${SCRIPT_DIR}/${TRACE_CSV}"     "${CONTAINER}:/tmp/${TRACE_CSV}"
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

echo "==> Restaurando genisdb desde ${DUMP_INFERIOR}..."
run_app genisdb -f "/tmp/${DUMP_INFERIOR}"
echo "    OK"

# =============================================================================
# 3. !! CRÍTICO !! Fix PRE-EVOLUTIONS
#    Ejecutar ANTES de iniciar GENis y correr evolutions
# =============================================================================
echo "==> [INFERIOR] Aplicando fix DATE_UPLOADED (pre-evolutions)..."
docker exec -e PGPASSWORD="$APP_PASS" "$CONTAINER" \
    psql -U "$APP_USER" -d genisdb -v ON_ERROR_STOP=1 <<'SQL'
-- Setear DATE_UPLOADED usando el ID más bajo del KIND 'interconectionUpdload'
UPDATE "APP"."PROFILE_UPLOADED" p
SET "DATE_UPLOADED" = to_char(t."DATE", 'YYYY-MM-DD"T"HH24:MI:SS')
FROM "APP"."TRACE" t
WHERE t."ID" = (
    SELECT MIN(t2."ID")
    FROM "APP"."TRACE" t2
    WHERE t2."PROFILE" = p."GLOBAL_CODE"
      AND t2."KIND" = 'interconectionUpdload'
)
AND t."KIND" = 'interconectionUpdload';

-- Normalizar formato: reemplazar 'T' y agregar offset -03:00:00
UPDATE "APP"."PROFILE_UPLOADED"
SET "DATE_UPLOADED" = to_char(
    ("DATE_UPLOADED"::timestamp),
    'YYYY-MM-DD HH24:MI:SS.US'
) || ' -03:00:00'
WHERE "DATE_UPLOADED" LIKE '%T%';
SQL
echo "    OK"

# =============================================================================
# 4. Crear TRACE2 e importar TRACE desde instancia superior
# =============================================================================
echo "==> [INFERIOR] Creando TRACE2 e importando trace_data.csv..."
docker exec -e PGPASSWORD="$APP_PASS" "$CONTAINER" \
    psql -U "$APP_USER" -d genisdb -v ON_ERROR_STOP=1 <<SQL
DROP TABLE IF EXISTS "APP"."TRACE2";

CREATE TABLE "APP"."TRACE2" (
    "ID"      bigint,
    "PROFILE" varchar,
    "USER"    varchar,
    "DATE"    timestamp,
    "TRACE"   varchar,
    "KIND"    varchar
);

ALTER TABLE "APP"."TRACE2" OWNER TO ${APP_USER};

COPY "APP"."TRACE2" FROM '/tmp/${TRACE_CSV}' CSV HEADER;
SQL
echo "    OK"

# =============================================================================
# 5. Corregir NOTIFICATION.INFO con userName
#    Requiere TRACE2 ya importado (paso 4)
# =============================================================================
echo "==> [INFERIOR] Corrigiendo NOTIFICATION.INFO con userName..."
docker exec -e PGPASSWORD="$APP_PASS" "$CONTAINER" \
    psql -U "$APP_USER" -d genisdb -v ON_ERROR_STOP=1 <<'SQL'
WITH ProcessedNotifications AS (
    SELECT
        n."ID",
        n."KIND"                           AS notification_kind,
        n."USER"                           AS notification_user,
        n."INFO"::jsonb                    AS original_info_jsonb,
        (n."INFO"::jsonb ->> 'globalCode') AS extracted_global_code_text,
        COALESCE(
            (SELECT t."USER"
             FROM "APP"."TRACE2" AS t
             WHERE t."PROFILE" = (n."INFO"::jsonb ->> 'globalCode')
             ORDER BY
                 CASE
                     WHEN t."KIND" = 'interconectionAproved' THEN 1
                     WHEN t."KIND" = 'aprovedProfile'        THEN 2
                     WHEN t."TRACE"::jsonb = '{}'            THEN 3
                     ELSE 999
                 END ASC,
                 t."DATE" DESC
             LIMIT 1),
            CASE
                WHEN n."KIND" = 'rejectedProfile' THEN n."USER"
                ELSE NULL
            END
        ) AS final_user_name_text
    FROM "APP"."NOTIFICATION" AS n
    WHERE n."KIND" IN ('aprovedProfile', 'rejectedProfile')
      AND (n."INFO"::jsonb ? 'globalCode')
      AND NOT (n."INFO"::jsonb ? 'userName')
)
UPDATE "APP"."NOTIFICATION" AS n
SET "INFO" = (
    SELECT
        '{' ||
        '"globalCode":' || to_json(pn.extracted_global_code_text)::text || ',' ||
        '"userName":'   || COALESCE(to_json(pn.final_user_name_text)::text, 'null') ||
        CASE
            WHEN (pn.original_info_jsonb - 'globalCode' - 'userName')::text = '{}'
            THEN ''
            ELSE ',' || substring(
                (pn.original_info_jsonb - 'globalCode' - 'userName')::text
                FROM 2
                FOR length((pn.original_info_jsonb - 'globalCode' - 'userName')::text) - 2
            )
        END
        || '}'
    FROM ProcessedNotifications AS pn_sub
    WHERE pn_sub."ID" = pn."ID"
)::text
FROM ProcessedNotifications AS pn
WHERE n."ID" = pn."ID";

-- Fallback: si TRACE2 estaba vacía, userName quedó como JSON null → usar USER de la notificación
UPDATE "APP"."NOTIFICATION"
SET "INFO" = jsonb_set("INFO"::jsonb, '{userName}', to_jsonb("USER"), false)::text
WHERE "KIND" IN ('aprovedProfile', 'rejectedProfile')
  AND "INFO"::jsonb ? 'userName'
  AND ("INFO"::jsonb ->> 'userName') IS NULL;
SQL
echo "    OK"

# =============================================================================
# 6. Fixes EN TODAS LAS INSTANCIAS
# =============================================================================
echo "==> [INFERIOR] Aplicando fixes comunes a todas las instancias..."
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
# 7. Insertar interconectionAproved en TRACE desde TRACE2
# =============================================================================
echo "==> [INFERIOR] Insertando interconectionAproved en TRACE desde TRACE2..."
docker exec -e PGPASSWORD="$APP_PASS" "$CONTAINER" \
    psql -U "$APP_USER" -d genisdb -v ON_ERROR_STOP=1 <<'SQL'
INSERT INTO "APP"."TRACE" ("PROFILE", "USER", "DATE", "TRACE", "KIND")
SELECT
    t2."PROFILE",
    t2."USER",
    t2."DATE",
    t2."TRACE",
    'interconectionAproved'
FROM "APP"."TRACE2" t2
WHERE t2."KIND" = 'interconectionUpdload'
  AND EXISTS (
      SELECT 1 FROM "APP"."TRACE" t_dest
      WHERE t_dest."PROFILE" = t2."PROFILE"
        AND t_dest."KIND"    = 'interconectionUpdload'
  )
  AND NOT EXISTS (
      SELECT 1 FROM "APP"."TRACE" t_dest
      WHERE t_dest."PROFILE" = t2."PROFILE"
        AND t_dest."KIND"    = 'interconectionAproved'
  )
  AND NOT EXISTS (
      SELECT 1 FROM "APP"."TRACE" t_dup
      WHERE t_dup."PROFILE" = t2."PROFILE"
        AND t_dup."KIND"    = 'interconectionAproved'
        AND t_dup."DATE"    = t2."DATE"
  );
SQL
echo "    OK"

# =============================================================================
# 8. Actualizar STATUS = 4 en PROFILE_UPLOADED para reportes
# =============================================================================
echo "==> [INFERIOR] Actualizando STATUS = 4 en PROFILE_UPLOADED para reportes..."
docker exec -e PGPASSWORD="$APP_PASS" "$CONTAINER" \
    psql -U "$APP_USER" -d genisdb -v ON_ERROR_STOP=1 <<'SQL'
BEGIN;

UPDATE "APP"."PROFILE_UPLOADED" pu
SET    "STATUS" = 4
FROM   "APP"."TRACE2" t
WHERE  pu."GLOBAL_CODE" = t."PROFILE"
  AND  t."KIND"         = 'interconectionAproved'
  AND  (pu."STATUS" IS DISTINCT FROM 4);

COMMIT;

-- Extra: perfiles sin interconectionAproved que igualmente fueron cargados
UPDATE "APP"."PROFILE_UPLOADED"
SET    "STATUS" = 4
WHERE  "GLOBAL_CODE" IN (
    'AR-B-SCJBA-1261', 'AR-B-SCJBA-1262', 'AR-B-SCJBA-1263',
    'AR-B-SCJBA-1264', 'AR-B-SCJBA-1265', 'AR-B-SCJBA-1266',
    'AR-B-SCJBA-1267', 'AR-B-SCJBA-1268', 'AR-B-SCJBA-1269',
    'AR-B-SCJBA-1270', 'AR-B-SCJBA-1274', 'AR-B-SCJBA-1285'
);
SQL
echo "    OK"

# =============================================================================
# 9. Limpieza de archivos temporales en el contenedor
# =============================================================================
echo "==> Limpiando archivos temporales en el contenedor..."
docker exec "$CONTAINER" rm -f \
    "/tmp/${DUMP_INFERIOR}" \
    "/tmp/${TRACE_CSV}"
echo "    OK"

# =============================================================================
echo ""
echo "============================================================"
echo "  Restore y fixes INFERIOR completados."
echo ""
echo "  genisdb (inferior) → restaurado + fixes aplicados"
echo ""
echo "  PRÓXIMOS PASOS:"
echo "  1. Iniciar GENis inferior apuntando a genisdb"
echo "  2. GENis corre evolutions automáticamente"
echo "============================================================"