# --- !Ups

CREATE TABLE "APP"."PEDIGREE"
(
  "ID" bigserial,
  "ID_COURT_CASE" bigint NOT NULL,
  "NAME" character varying(100) NOT NULL,
  "CREATION_DATE" timestamp NOT NULL,
  "STATUS" character varying(50) NOT NULL DEFAULT 'UnderConstruction'::character varying,
  CONSTRAINT "PREDIGREE_PK" PRIMARY KEY ("ID"),
  CONSTRAINT "PEDIGREE_COURT_CASE_FK" FOREIGN KEY ("ID_COURT_CASE") REFERENCES "APP"."COURT_CASE" ("ID")
);


# --- !Downs

DROP TABLE "APP"."PEDIGREE";
