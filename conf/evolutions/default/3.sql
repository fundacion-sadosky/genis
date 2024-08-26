# --- !Ups

CREATE TABLE "APP"."NOTIFICATION"
(
  "ID" bigserial,
  "USER" character varying(50) NOT NULL,
  "KIND" character varying(100) NOT NULL,
  "INFO" character varying(1024) NOT NULL,
  "CREATION_DATE" timestamp NOT NULL,
  "UPDATE_DATE" timestamp,
  "PENDING" boolean NOT NULL,
  "FLAGGED" boolean NOT NULL,
  CONSTRAINT "APP_NOTIFICATION_ID_PKEY" PRIMARY KEY ("ID")
);

# --- !Downs

DROP TABLE "APP"."NOTIFICATION";
