# --- !Ups

ALTER TABLE "APP"."PEDIGREE" ADD COLUMN "ASSIGNEE" character varying(50) NOT NULL DEFAULT 'tst-admintist';

-- poner el update con el asignee del caso buscar!

ALTER TABLE "APP"."PEDIGREE" ALTER COLUMN  "ASSIGNEE" DROP DEFAULT;

# --- !Downs
ALTER TABLE "APP"."PEDIGREE" DROP COLUMN "ASSIGNEE";
