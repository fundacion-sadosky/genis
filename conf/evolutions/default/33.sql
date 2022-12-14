# --- !Ups

create table "APP"."PEDCHECK"
(
  "ID" bigserial,
  "ID_PEDIGREE" bigint NOT NULL,
  "LOCUS" text not null,
  "GLOBAL_CODE" text not null,
  CONSTRAINT "PEDCHECK_PK" PRIMARY KEY ("ID"),
  CONSTRAINT "PEDCHECK_PEDIGREE_FK" FOREIGN KEY ("ID_PEDIGREE") REFERENCES "APP"."PEDIGREE" ("ID")
);

ALTER TABLE "APP"."PEDIGREE" ADD COLUMN "CONSISTENCY_RUN" boolean default false;
# --- !Downs

DROP TABLE "APP"."PEDCHECK";
ALTER TABLE "APP"."PEDIGREE" DROP COLUMN "CONSISTENCY_RUN";
