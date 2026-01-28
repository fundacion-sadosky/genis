# --- !Ups

CREATE SCHEMA "LOG_DB";

CREATE TABLE "LOG_DB"."OPERATION_LOG_LOT"
(
   "ID" bigserial, 
   "KEY_ZERO" character varying(200) NOT NULL, 
   "INIT_TIME" timestamp DEFAULT now() NOT NULL,
   CONSTRAINT "OPERATION_LOG_LOT_PK" PRIMARY KEY ("ID")
);

CREATE TABLE "LOG_DB"."OPERATION_LOG_RECORD"
(
   "ID" bigserial, 

   "USER_ID" character varying(50) NOT NULL, 
   "OTP" character varying(50), 
   "TIMESTAMP" timestamp NOT NULL, 
   "METHOD" character varying(50) NOT NULL,
   "PATH" character varying(1024) NOT NULL,
   "ACTION" character varying(512) NOT NULL,
   "BUILD_NO" character varying(150) NOT NULL,
   "RESULT" character varying(150),
   "STATUS" integer NOT NULL,
   "SIGNATURE" character varying(8192) NOT NULL, 
   "LOT" bigint NOT NULL,
   "DESCRIPTION" character varying(1024) NOT NULL,
   CONSTRAINT "OPERATION_LOG_RECORD_PK" PRIMARY KEY ("ID"), 
   CONSTRAINT "OPERATION_LOG_RECORD_FK" FOREIGN KEY ("LOT") 
    REFERENCES "LOG_DB"."OPERATION_LOG_LOT" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION
);

# --- !Downs

DROP SCHEMA  "LOG_DB" CASCADE;
