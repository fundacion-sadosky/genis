# --- !Ups
alter table "APP"."PROFILE_DATA_FILIATION" alter column "FULL_NAME" drop not null;
alter table "APP"."PROFILE_DATA_FILIATION" alter column "NICKNAME" drop not null;
alter table "APP"."PROFILE_DATA_FILIATION" alter column "BIRTHDAY" drop not null;
alter table "APP"."PROFILE_DATA_FILIATION" alter column "BIRTH_PLACE" drop not null;
alter table "APP"."PROFILE_DATA_FILIATION" alter column "NATIONALITY" drop not null;
alter table "APP"."PROFILE_DATA_FILIATION" alter column "IDENTIFICATION" drop not null;
alter table "APP"."PROFILE_DATA_FILIATION" alter column "IDENTIFICATION_ISSUING_AUTHORITY" drop not null;
alter table "APP"."PROFILE_DATA_FILIATION" alter column "ADDRESS" drop not null;