# --- !Ups
ALTER TABLE "APP"."LOCUS" ADD COLUMN "REQUIRED" boolean NOT NULL DEFAULT true;

# --- !Downs

ALTER TABLE "APP"."LOCUS" DROP COLUMN "REQUIRED" ;





