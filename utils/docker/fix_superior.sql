-- ============================================================
-- INSTANCIA SUPERIOR / PROVINCIAL
-- Conectarse a genisdb: psql -U genissqladmin -d genisdb
-- ============================================================

\c genisdb

-- ============================================================
-- !! CRÍTICO !! EJECUTAR ANTES DE INICIAR GENIS Y CORRER EVOLUTIONS
-- Actualizar DATE_UPLOADED y DATE_RECEIVED con fechas reales de replicación
-- ============================================================

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

-- DATE_RECEIVED: usar fecha de 'importedFromInferior' si existe,
-- si no, la fecha más temprana de cualquier KIND en TRACE
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

-- ============================================================
-- FIN SECCIÓN CRÍTICA PRE-EVOLUTIONS
-- ============================================================


-- ============================================================
-- PASO PREVIO: Exportar TRACE para la instancia inferior
-- El archivo resultante debe copiarse al servidor de la instancia inferior
-- en /home/genis-user/trace_data.csv antes de ejecutar fix_inferior.sql
-- ============================================================

COPY "APP"."TRACE" TO '/home/genis-user/trace_data.csv' CSV HEADER;


-- ============================================================
-- EN TODAS LAS INSTANCIAS
-- ============================================================

-- Agregar isDesktop:false en NOTIFICATION.INFO cuando KIND = 'matching'
UPDATE "APP"."NOTIFICATION"
SET "INFO" = jsonb_set(
    "INFO"::jsonb,
    '{isDesktop}',
    'false'::jsonb,
    true
)::text
WHERE
    "KIND" = 'matching'
    AND "INFO"::jsonb ? 'isDesktop' = false;

-- Agregar userName:USER en TRACE.TRACE cuando KIND = 'categoryModification'
UPDATE "APP"."TRACE"
SET "TRACE" = jsonb_set(
    "TRACE"::jsonb,
    '{userName}',
    to_jsonb("USER"::text),
    true
)::character varying
WHERE "KIND" = 'categoryModification';

-- Agregar multiallelic:false en TRACE cuando incluye 'categoryConfiguration' y no tiene la clave
UPDATE "APP"."TRACE" AS t
SET "TRACE" = jsonb_set(
    t."TRACE"::jsonb,
    '{categoryConfiguration, multiallelic}',
    to_jsonb(false),
    true
)::character varying
WHERE
    t."TRACE"::jsonb ? 'categoryConfiguration'
    AND NOT (t."TRACE"::jsonb -> 'categoryConfiguration' ? 'multiallelic');


-- ============================================================
-- SOLO INSTANCIA SUPERIOR: motivo de rechazo en TRACE
-- ============================================================

-- Completar TRACE con el motive de PROFILE_UPLOADED cuando KIND = 'interconectionRejected'
UPDATE "APP"."TRACE" t
SET "TRACE" = json_build_object('motive', pu."MOTIVE")::text
FROM "APP"."PROFILE_UPLOADED" pu
WHERE t."PROFILE" = pu."GLOBAL_CODE"
  AND t."KIND"    = 'interconectionRejected'
  AND pu."MOTIVE" IS NOT NULL
  AND t."TRACE" IS DISTINCT FROM json_build_object('motive', pu."MOTIVE")::text;

-- Dejar TRACE con motive vacío cuando KIND = 'interconectionRejected' y TRACE es NULL o '{}'
UPDATE "APP"."TRACE"
SET "TRACE" = json_build_object('motive', '')::text
WHERE "KIND" = 'interconectionRejected'
  AND ( "TRACE" IS NULL OR btrim("TRACE") = '{}' );