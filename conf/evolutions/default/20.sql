# --- !Ups
UPDATE "APP"."GROUP" SET "NAME" = 'Muestras Ante Mortem' WHERE "ID" = 'AM';
UPDATE "APP"."GROUP" SET "NAME" = 'Muestras Post Mortem' WHERE "ID" = 'PM';
# --- !Downs
