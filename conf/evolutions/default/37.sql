# --- !Ups

insert into "APP"."STRKIT" ("ID", "NAME", "TYPE", "LOCI_QTY", "REPRESENTATIVE_PARAMETER") values
  ('Mitocondrial','Mitocondrial',4,'8','2');

INSERT INTO "APP"."LOCUS"("ID", "NAME", "CHROMOSOME", "MINIMUM_ALLELES_QTY", "MAXIMUM_ALLELES_QTY", "TYPE") VALUES
  ('HV4_RANGE', 'Rango 4', null, 2, 2, (SELECT "ID" FROM "APP"."ANALYSIS_TYPE" WHERE "NAME" = 'MT')),
  ('HV4', 'Variaciones Rango4', 'MT', 1, 300, (SELECT "ID" FROM "APP"."ANALYSIS_TYPE" WHERE "NAME" = 'MT'));

UPDATE "APP"."LOCUS" SET "NAME" = 'Rango 1' WHERE "ID" = 'HV1_RANGE';
UPDATE "APP"."LOCUS" SET "NAME" = 'Rango 2' WHERE "ID" = 'HV2_RANGE';
UPDATE "APP"."LOCUS" SET "NAME" = 'Rango 3' WHERE "ID" = 'HV3_RANGE';
UPDATE "APP"."LOCUS" SET "NAME" = 'Variaciones Rango1' WHERE "ID" = 'HV1';
UPDATE "APP"."LOCUS" SET "NAME" = 'Variaciones Rango2' WHERE "ID" = 'HV2';
UPDATE "APP"."LOCUS" SET "NAME" = 'Variaciones Rango3' WHERE "ID" = 'HV3';

INSERT INTO "APP"."STRKIT_LOCUS"("STRKIT", "LOCUS", "ORDER") VALUES
  ('Mitocondrial', 'HV1_RANGE', 1),
  ('Mitocondrial', 'HV1', 2),
  ('Mitocondrial', 'HV2_RANGE', 3),
  ('Mitocondrial', 'HV2', 4),
  ('Mitocondrial', 'HV3_RANGE', 5),
  ('Mitocondrial', 'HV3', 6),
  ('Mitocondrial', 'HV4_RANGE', 7),
  ('Mitocondrial', 'HV4', 8);

# --- !Downs

DELETE FROM "APP"."STRKIT_LOCUS" WHERE "STRKIT" = 'Mitocondrial';
DELETE FROM "APP"."STRKIT" WHERE "ID" = 'Mitocondrial';
DELETE FROM "APP"."LOCUS" WHERE "ID"= 'HV4_RANGE';
DELETE FROM "APP"."LOCUS" WHERE "ID"= 'HV4';
