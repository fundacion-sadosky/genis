# --- !Ups

CREATE TABLE "APP"."TRACE_PEDIGREE"
(
  "ID" bigserial,
  "PEDIGREE" bigint NOT NULL,
  "USER" character varying(50) NOT NULL,
  "DATE" timestamp NOT NULL,
  "TRACE" character varying NOT NULL,
  "KIND" character varying(100) NOT NULL,
  CONSTRAINT "APP_TRACEP_ID_PKEY" PRIMARY KEY ("ID")
);

# --- !Downs

DROP TABLE "APP"."TRACE_PEDIGREE";
