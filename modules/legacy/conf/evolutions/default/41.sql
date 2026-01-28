# --- !Ups
-- This section applies the changes.

ALTER TABLE IF EXISTS "APP"."MATCH_UPDATE_SEND_STATUS"
    ADD COLUMN "USER_NAME" text;

COMMENT ON COLUMN "APP"."MATCH_UPDATE_SEND_STATUS"."USER_NAME" IS 'User associated with hit or discard match.';


# --- !Downs

ALTER TABLE IF EXISTS "APP"."MATCH_UPDATE_SEND_STATUS"
    DROP COLUMN "USER_NAME";