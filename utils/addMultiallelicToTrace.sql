UPDATE "APP"."TRACE" AS t
SET
    "TRACE" = jsonb_set(
        t."TRACE"::jsonb,
        '{categoryConfiguration, multiallelic}',
        to_jsonb(false),
        true -- Create the key if it doesn't exist
    )::character varying
WHERE
    t."TRACE"::jsonb ? 'categoryConfiguration' -- Ensure 'categoryConfiguration' key exists
    AND NOT (t."TRACE"::jsonb -> 'categoryConfiguration' ? 'multiallelic'); -- Ensure 'multiallelic' is NOT already present

