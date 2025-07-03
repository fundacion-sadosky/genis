# --- !Ups
ALTER TABLE "APP"."PROFILE_UPLOADED" ADD COLUMN "INTERCONNECTION_ERROR" VARCHAR(255);
ALTER TABLE "APP"."PROFILE_SENT" ADD COLUMN "INTERCONNECTION_ERROR" VARCHAR(255);
INSERT INTO "APP"."INFERIOR_INSTANCE_PROFILE_STATUS"("ID", "STATUS") VALUES
                                                                         (17, 'Perfil proveniente de instancia inferior rechazado en  instancia. Instancia inferior notificada'),
                                                                         (18, 'Perfil proveniente de instancia inferior aprobado en esta instancia. Instanica inferior notificada'),
                                                                         (19, 'Perfil eliminado en esta instancia y pendiente de notificar a instancia inferior'),
                                                                         (20, 'Instancia inferior notificada de la eliminación del perfil en la instancia superior'),
                                                                         (21, 'Perfil proveniente de instancia inferior rechazado en  instancia . Pendiente notificación a instancia inferior' ),
                                                                         (22, 'Perfil proveniente de instancia inferior aprobado en esta instancia. Pendiente notificación a instancia inferior'),
                                                                         (23, 'Perfil eliminado en la instancia inferior');
UPDATE "APP"."INFERIOR_INSTANCE_PROFILE_STATUS" SET "STATUS" = 'Pendiente envío de notificación de eliminación a instancia superior' WHERE "ID" = 5;
UPDATE "APP"."INFERIOR_INSTANCE_PROFILE_STATUS" SET "STATUS" = 'Notificación de eliminación recibida en la instancia superior' WHERE "ID" = 6;

CREATE SEQUENCE IF NOT EXISTS "APP"."PROFILE_RECEIVED_ID_seq"
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

CREATE TABLE IF NOT EXISTS "APP"."PROFILE_RECEIVED"
(
    "ID" bigint NOT NULL DEFAULT nextval('"APP"."PROFILE_RECEIVED_ID_seq"'::regclass),
    "LABCODE" text COLLATE pg_catalog."default" NOT NULL,
    "GLOBAL_CODE" character varying(100) COLLATE pg_catalog."default" NOT NULL,
    "STATUS" bigint,
    "MOTIVE" text COLLATE pg_catalog."default",
    "INTERCONNECTION_ERROR" character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT "PROFILE_RECEIVED_ID_PKEY" PRIMARY KEY ("ID","LABCODE")
    ) TABLESPACE pg_default;

ALTER SEQUENCE "APP"."PROFILE_RECEIVED_ID_seq" OWNED BY "APP"."PROFILE_RECEIVED"."ID";

ALTER SEQUENCE "APP"."PROFILE_RECEIVED_ID_seq" OWNER TO genissqladmin;
ALTER TABLE IF EXISTS "APP"."PROFILE_RECEIVED" OWNER TO genissqladmin;

ALTER TABLE "APP"."PROFILE_RECEIVED"
    ADD CONSTRAINT "PROFILE_RECEIVED_FK" FOREIGN KEY ("STATUS")
        REFERENCES "APP"."INFERIOR_INSTANCE_PROFILE_STATUS" ("ID") MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION;
# --- !Downs
DROP TABLE IF EXISTS "APP"."PROFILE_RECEIVED";

DROP SEQUENCE IF EXISTS "APP"."PROFILE_RECEIVED_ID_seq";

DELETE FROM "APP"."INFERIOR_INSTANCE_PROFILE_STATUS" WHERE "ID" IN (17, 18, 19, 20, 21, 22);

ALTER TABLE "APP"."PROFILE_SENT" DROP COLUMN IF EXISTS "INTERCONNECTION_ERROR";

ALTER TABLE "APP"."PROFILE_UPLOADED" DROP COLUMN IF EXISTS "INTERCONNECTION_ERROR";