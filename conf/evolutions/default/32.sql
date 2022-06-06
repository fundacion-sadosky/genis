# --- !Ups

create table "APP"."FILE_SENT"
(
  "ID" varchar(100) not null,
  "TARGET_LAB" text not null,
  "STATUS" bigint
    constraint FILE_SENT_FK1
    references "APP"."INFERIOR_INSTANCE_PROFILE_STATUS",
  "DATE" timestamp default now(),
  "FILE_TYPE" text not null,
  constraint "FILE_SENT_ID_PKEY"
  primary key ("ID", "TARGET_LAB")
)
;
INSERT INTO "APP"."INFERIOR_INSTANCE_PROFILE_STATUS" ("ID", "STATUS") VALUES (15, 'Pending file to send');
INSERT INTO "APP"."INFERIOR_INSTANCE_PROFILE_STATUS" ("ID", "STATUS") VALUES (16, 'File Sent');
# --- !Downs

DROP TABLE IF EXISTS "APP"."FILE_SENT";

DELETE FROM "APP"."INFERIOR_INSTANCE_PROFILE_STATUS" WHERE "ID" = 15;
DELETE FROM "APP"."INFERIOR_INSTANCE_PROFILE_STATUS" WHERE "ID" = 16;