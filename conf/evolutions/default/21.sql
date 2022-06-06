# --- !Ups
INSERT INTO "APP"."CATEGORY" ("GROUP", "ID", "NAME", "IS_REFERENCE", "PEDIGREE_ASSOCIATION") VALUES
  ('PM', 'PFNI', 'Personas fallecidas cuya identidad quiera analizarse', false, true);

INSERT INTO "APP"."CATEGORY_MATCHING" ("CATEGORY", "CATEGORY_RELATED", "PRIORITY", "MINIMUM_STRINGENCY", "FAIL_ON_MATCH", "FORWARD_TO_UPPER", "MATCHING_ALGORITHM", "MIN_LOCUS_MATCH", "MISMATCHS_ALLOWED", "TYPE") VALUES
  ('PFNI', 'IR', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 1),
  ('PFNI', 'ER', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('PFNI', 'INNV', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('PFNI', 'ENN', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('IR', 'PFNI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 1),
  ('ER', 'PFNI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('INNV', 'PFNI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('ENN', 'PFNI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('RNN', 'PFNI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('PFNI', 'RNN', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('PFNI', 'PFNI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 1),
  ('PFNI', 'IR', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 2),
  ('PFNI', 'ER', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('PFNI', 'INNV', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('PFNI', 'ENN', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('IR', 'PFNI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 2),
  ('ER', 'PFNI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('INNV', 'PFNI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('ENN', 'PFNI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('RNN', 'PFNI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('PFNI', 'RNN', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('PFNI', 'PFNI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 2),
  ('PFNI', 'IR', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 3),
  ('PFNI', 'ER', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('PFNI', 'INNV', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('PFNI', 'ENN', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('IR', 'PFNI', 1, 'LowStringency', false, false, 'ENFSI', 11, 2, 3),
  ('ER', 'PFNI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('INNV', 'PFNI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('ENN', 'PFNI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('RNN', 'PFNI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('PFNI', 'RNN', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('PFNI', 'PFNI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 3),
  ('PFNI', 'ER', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('PFNI', 'INNV', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('PFNI', 'ENN', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('IR', 'PFNI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('ER', 'PFNI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('INNV', 'PFNI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('ENN', 'PFNI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('RNN', 'PFNI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('PFNI', 'RNN', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4),
  ('PFNI', 'PFNI', 1, 'HighStringency', false, false, 'ENFSI', 11, 2, 4);


INSERT INTO "APP"."CATEGORY_CONFIGURATION"
("CATEGORY", "TYPE", "COLLECTION_URI", "DRAFT_URI", "MIN_LOCUS_PER_PROFILE", "MAX_OVERAGE_DEVIATED_LOCI", "MAX_ALLELES_PER_LOCUS")
VALUES
  ('PFNI', 1, '', '', 'K', '2', 2),
  ('PFNI', 2, '', '', 'K', '2', 2),
  ('PFNI', 3, '', '', 'K', '2', 2);

# --- !Downs
DELETE FROM "APP"."CATEGORY_MATCHING" WHERE "CATEGORY_RELATED" IN ('PFNI');
DELETE FROM "APP"."CATEGORY_MATCHING" WHERE "CATEGORY" IN ('PFNI');
DELETE FROM "APP"."CATEGORY_CONFIGURATION" WHERE "CATEGORY" IN ('PFNI');
DELETE FROM "APP"."CATEGORY_ASSOCIATION" WHERE "CATEGORY" IN ('PFNI');
DELETE FROM "APP"."PROFILE_DATA" WHERE "CATEGORY" IN ('PFNI');
DELETE FROM "APP"."CATEGORY" WHERE "ID" IN ('PFNI');