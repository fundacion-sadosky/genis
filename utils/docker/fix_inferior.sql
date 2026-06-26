-- ============================================================
-- INSTANCIA INFERIOR
-- Conectarse a genisdb: psql -U genissqladmin -d genisdb
-- ============================================================

\c genisdb

-- ============================================================
-- !! CRÍTICO !! EJECUTAR ANTES DE INICIAR GENIS Y CORRER EVOLUTIONS
-- Actualizar DATE_UPLOADED con la fecha real de replicación del perfil
-- ============================================================

-- Paso 1: Setear DATE_UPLOADED usando el ID más bajo del KIND 'interconectionUpdload'
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

-- Paso 2: Normalizar formato de fecha (reemplazar 'T' por espacio y agregar offset -03:00:00)
UPDATE "APP"."PROFILE_UPLOADED"
SET "DATE_UPLOADED" = to_char(
    ("DATE_UPLOADED"::timestamp),
    'YYYY-MM-DD HH24:MI:SS.US'
) || ' -03:00:00'
WHERE "DATE_UPLOADED" LIKE '%T%';

-- ============================================================
-- FIN SECCIÓN CRÍTICA PRE-EVOLUTIONS
-- ============================================================


-- ============================================================
-- PASO PREVIO: Importar TRACE desde instancia superior
-- Requiere que la instancia superior haya ejecutado fix_superior.sql
-- y que el archivo trace_data.csv esté disponible en /home/genis-user/
-- ============================================================

CREATE TABLE IF NOT EXISTS "APP"."TRACE2" (
    "ID"      bigint,
    "PROFILE" varchar,
    "USER"    varchar,
    "DATE"    timestamp,
    "TRACE"   varchar,
    "KIND"    varchar
);

ALTER TABLE IF EXISTS "APP"."TRACE2"
    OWNER TO genissqladmin;

COPY "APP"."TRACE2" FROM '/home/genis-user/trace_data.csv' CSV HEADER;


-- ============================================================
-- Corregir NOTIFICATION.INFO: agregar userName en aprovedProfile / rejectedProfile
-- ============================================================

WITH ProcessedNotifications AS (
    SELECT
        n."ID",
        n."KIND"                          AS notification_kind,
        n."USER"                          AS notification_user,
        n."INFO"::jsonb                   AS original_info_jsonb,
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
    WHERE
        n."KIND" IN ('aprovedProfile', 'rejectedProfile')
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
-- SOLO INSTANCIA INFERIOR: insertar interconectionAproved en TRACE
-- desde los registros de interconectionUpdload en TRACE2
-- ============================================================

INSERT INTO "APP"."TRACE" ("PROFILE", "USER", "DATE", "TRACE", "KIND")
SELECT
    t2."PROFILE",
    t2."USER",
    t2."DATE",
    t2."TRACE",
    'interconectionAproved'
FROM "APP"."TRACE2" t2
WHERE
    t2."KIND" = 'interconectionUpdload'
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


-- ============================================================
-- SOLO INSTANCIA INFERIOR: actualizar STATUS = 4 para reportes
-- ============================================================

BEGIN;

UPDATE "APP"."PROFILE_UPLOADED" pu
SET    "STATUS" = 4
FROM   "APP"."TRACE2" t
WHERE  pu."GLOBAL_CODE" = t."PROFILE"
  AND  t."KIND"         = 'interconectionAproved'
  AND  (pu."STATUS" IS DISTINCT FROM 4);

COMMIT;

