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
# --- !Downs

DROP TABLE IF EXISTS "APP"."FILE_SENT";
