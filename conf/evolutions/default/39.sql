# --- !Ups
ALTER TABLE "APP"."CATEGORY" ADD COLUMN "TYPE" SMALLINT NOT NULL DEFAULT '1';


UPDATE "APP"."CATEGORY"
SET "TYPE" = 2
WHERE "PEDIGREE_ASSOCIATION" = TRUE;



INSERT INTO "APP"."GROUP" ("ID", "NAME") VALUES
  ('AM_DVI', 'Muestras Ante Mortem DVI'),
  ('PM_DVI', 'Muestras Post Mortem DVI');

INSERT INTO "APP"."CATEGORY" ("GROUP", "ID", "NAME", "IS_REFERENCE", "PEDIGREE_ASSOCIATION", "TYPE") VALUES
  ('AM_DVI', 'IR_DVI', 'Individuos de  Referencia', true, true,3),
  ('PM_DVI', 'PFNI_DVI', 'Personas fallecidas cuya identidad quiera analizarse', false, true,3),
  ('PM_DVI', 'RNN_DVI', 'Restos Biológicos no identificados', false, true,3),
  ('PM_DVI', 'ENN_DVI', 'Elementos Personales hallados de la persona desaparecida', false, true,3),
  ('PM_DVI', 'INN_DVI', 'Personas fallecidas no identificadas', false, true,3);

INSERT INTO "APP"."CATEGORY_MATCHING" ("CATEGORY", "CATEGORY_RELATED", "PRIORITY", "MINIMUM_STRINGENCY", "FAIL_ON_MATCH", "FORWARD_TO_UPPER", "MATCHING_ALGORITHM", "MIN_LOCUS_MATCH", "MISMATCHS_ALLOWED", "TYPE") VALUES
  ('IR_DVI', 'ENN_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 1),
  ('ENN_DVI', 'IR_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 1),
  ('INN_DVI', 'IR_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 1),
  ('INN_DVI', 'ENN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('IR_DVI', 'INN_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 1),
  ('ENN_DVI', 'INN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('RNN_DVI', 'IR_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 1),
  ('RNN_DVI', 'INN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('RNN_DVI', 'ENN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('IR_DVI', 'RNN_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 1),
  ('INN_DVI', 'RNN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('ENN_DVI', 'RNN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('INN_DVI', 'INN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('RNN_DVI', 'RNN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('ENN_DVI', 'ENN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('PFNI_DVI', 'IR_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 1),
  ('PFNI_DVI', 'ENN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('IR_DVI', 'PFNI_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 1),
  ('ENN_DVI', 'PFNI_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('RNN_DVI', 'PFNI_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('PFNI_DVI', 'RNN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('PFNI_DVI', 'PFNI_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('IR_DVI', 'ENN_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 2),
  ('ENN_DVI', 'IR_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 2),
  ('INN_DVI', 'IR_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 2),
  ('INN_DVI', 'ENN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('IR_DVI', 'INN_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 2),
  ('ENN_DVI', 'INN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('RNN_DVI', 'IR_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 2),
  ('RNN_DVI', 'INN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('RNN_DVI', 'ENN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('IR_DVI', 'RNN_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 2),
  ('INN_DVI', 'RNN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('ENN_DVI', 'RNN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('INN_DVI', 'INN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('RNN_DVI', 'RNN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('ENN_DVI', 'ENN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('PFNI_DVI', 'IR_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 2),
  ('PFNI_DVI', 'ENN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('IR_DVI', 'PFNI_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 2),
  ('ENN_DVI', 'PFNI_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('RNN_DVI', 'PFNI_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('PFNI_DVI', 'RNN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('PFNI_DVI', 'PFNI_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('IR_DVI', 'ENN_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 3),
  ('INN_DVI', 'ENN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('IR_DVI', 'INN_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 3),
  ('ENN_DVI', 'INN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('RNN_DVI', 'IR_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 3),
  ('RNN_DVI', 'INN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('RNN_DVI', 'ENN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('IR_DVI', 'RNN_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 3),
  ('INN_DVI', 'RNN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('ENN_DVI', 'RNN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('INN_DVI', 'INN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('RNN_DVI', 'RNN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('ENN_DVI', 'ENN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('PFNI_DVI', 'IR_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 3),
  ('PFNI_DVI', 'ENN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('IR_DVI', 'PFNI_DVI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 3),
  ('ENN_DVI', 'PFNI_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('RNN_DVI', 'PFNI_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('PFNI_DVI', 'RNN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('PFNI_DVI', 'PFNI_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('IR_DVI', 'ENN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('ENN_DVI', 'IR_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('INN_DVI', 'IR_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('INN_DVI', 'ENN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('IR_DVI', 'INN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('ENN_DVI', 'INN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('IR_DVI', 'INNV', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('RNN_DVI', 'IR_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('RNN_DVI', 'INN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('RNN_DVI', 'ENN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('IR_DVI', 'RNN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('INN_DVI', 'RNN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('ENN_DVI', 'RNN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('INN_DVI', 'INN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('RNN_DVI', 'RNN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('ENN_DVI', 'ENN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('PFNI_DVI', 'ENN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('IR_DVI', 'PFNI_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('ENN_DVI', 'PFNI_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('RNN_DVI', 'PFNI_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('PFNI_DVI', 'RNN_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('PFNI_DVI', 'PFNI_DVI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4);

INSERT INTO "APP"."CATEGORY_CONFIGURATION"
("CATEGORY", "TYPE", "COLLECTION_URI", "DRAFT_URI", "MIN_LOCUS_PER_PROFILE", "MAX_OVERAGE_DEVIATED_LOCI", "MAX_ALLELES_PER_LOCUS")
VALUES
  ('INN_DVI', 1, '', '', 'K', '2', 2),
  ('RNN_DVI', 1, '', '', 'K', '2', 2),
  ('ENN_DVI', 1, '', '', 'K', '2', 2),
  ('IR_DVI', 1, '', '', 'K', '2', 2),
  ('PFNI_DVI', 1, '', '', 'K', '2', 2),
  ('INN_DVI', 2, '', '', 'K', '2', 2),
  ('RNN_DVI', 2, '', '', 'K', '2', 2),
  ('ENN_DVI', 2, '', '', 'K', '2', 2),
  ('IR_DVI', 2, '', '', 'K', '2', 2),
  ('PFNI_DVI', 2, '', '', 'K', '2', 2),
  ('INN_DVI', 3, '', '', 'K', '2', 2),
  ('RNN_DVI', 3, '', '', 'K', '2', 2),
  ('ENN_DVI', 3, '', '', 'K', '2', 2),
  ('IR_DVI', 3, '', '', 'K', '2', 2),
  ('PFNI_DVI', 3, '', '', 'K', '2', 2);



# --- !Downs

ALTER TABLE "APP"."CATEGORY" DROP COLUMN "TYPE";

DELETE FROM "APP"."CATEGORY_MATCHING" WHERE "CATEGORY_RELATED" IN ('INN_DVI', 'RNN_DVI', 'ENN_DVI', 'IR_DVI','PFNI_DVI');
DELETE FROM "APP"."CATEGORY_MATCHING" WHERE "CATEGORY" IN ('INN_DVI','RNN_DVI', 'ENN_DVI', 'IR_DVI','PFNI_DVI');
DELETE FROM "APP"."CATEGORY_CONFIGURATION" WHERE "CATEGORY" IN ( 'INN_DVI', 'RNN_DVI', 'ENN_DVI', 'IR_DVI','PFNI_DVI');
DELETE FROM "APP"."CATEGORY_ASSOCIATION" WHERE "CATEGORY" IN ('INN_DVI', 'RNN_DVI', 'ENN_DVI', 'IR_DVI','PFNI_DVI');
DELETE FROM "APP"."PROFILE_DATA" WHERE "CATEGORY" IN ( 'INN_DVI',  'RNN_DVI', 'ENN_DVI', 'IR_DVI','PFNI_DVI');
DELETE FROM "APP"."CATEGORY" WHERE "GROUP" IN ('AM_DVI', 'PM_DVI');
DELETE FROM "APP"."GROUP" WHERE "ID" IN ('AM_DVI', 'PM_DVI');