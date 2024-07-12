--
-- PostgreSQL database dump
--

-- Dumped from database version 14.9 (Debian 14.9-1.pgdg120+1)
-- Dumped by pg_dump version 14.9 (Debian 14.9-1.pgdg120+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: LOG_DB; Type: SCHEMA; Schema: -; Owner: genissqladmin
--

CREATE SCHEMA "LOG_DB";


ALTER SCHEMA "LOG_DB" OWNER TO genissqladmin;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: OPERATION_LOG_LOT; Type: TABLE; Schema: LOG_DB; Owner: genissqladmin
--

CREATE TABLE "LOG_DB"."OPERATION_LOG_LOT" (
    "ID" bigint NOT NULL,
    "KEY_ZERO" character varying(200) NOT NULL,
    "INIT_TIME" timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE "LOG_DB"."OPERATION_LOG_LOT" OWNER TO genissqladmin;

--
-- Name: OPERATION_LOG_LOT_ID_seq; Type: SEQUENCE; Schema: LOG_DB; Owner: genissqladmin
--

CREATE SEQUENCE "LOG_DB"."OPERATION_LOG_LOT_ID_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE "LOG_DB"."OPERATION_LOG_LOT_ID_seq" OWNER TO genissqladmin;

--
-- Name: OPERATION_LOG_LOT_ID_seq; Type: SEQUENCE OWNED BY; Schema: LOG_DB; Owner: genissqladmin
--

ALTER SEQUENCE "LOG_DB"."OPERATION_LOG_LOT_ID_seq" OWNED BY "LOG_DB"."OPERATION_LOG_LOT"."ID";


--
-- Name: OPERATION_LOG_RECORD; Type: TABLE; Schema: LOG_DB; Owner: genissqladmin
--

CREATE TABLE "LOG_DB"."OPERATION_LOG_RECORD" (
    "ID" bigint NOT NULL,
    "USER_ID" character varying(50) NOT NULL,
    "OTP" character varying(50),
    "TIMESTAMP" timestamp without time zone NOT NULL,
    "METHOD" character varying(50) NOT NULL,
    "PATH" character varying(1024) NOT NULL,
    "ACTION" character varying(512) NOT NULL,
    "BUILD_NO" character varying(150) NOT NULL,
    "RESULT" character varying(150),
    "STATUS" integer NOT NULL,
    "SIGNATURE" character varying(8192) NOT NULL,
    "LOT" bigint NOT NULL,
    "DESCRIPTION" character varying(1024) NOT NULL
);


ALTER TABLE "LOG_DB"."OPERATION_LOG_RECORD" OWNER TO genissqladmin;

--
-- Name: OPERATION_LOG_RECORD_ID_seq; Type: SEQUENCE; Schema: LOG_DB; Owner: genissqladmin
--

CREATE SEQUENCE "LOG_DB"."OPERATION_LOG_RECORD_ID_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE "LOG_DB"."OPERATION_LOG_RECORD_ID_seq" OWNER TO genissqladmin;

--
-- Name: OPERATION_LOG_RECORD_ID_seq; Type: SEQUENCE OWNED BY; Schema: LOG_DB; Owner: genissqladmin
--

ALTER SEQUENCE "LOG_DB"."OPERATION_LOG_RECORD_ID_seq" OWNED BY "LOG_DB"."OPERATION_LOG_RECORD"."ID";


--
-- Name: play_evolutions; Type: TABLE; Schema: public; Owner: genissqladmin
--

CREATE TABLE public.play_evolutions (
    id integer NOT NULL,
    hash character varying(255) NOT NULL,
    applied_at timestamp without time zone NOT NULL,
    apply_script text,
    revert_script text,
    state character varying(255),
    last_problem text
);


ALTER TABLE public.play_evolutions OWNER TO genissqladmin;

--
-- Name: OPERATION_LOG_LOT ID; Type: DEFAULT; Schema: LOG_DB; Owner: genissqladmin
--

ALTER TABLE ONLY "LOG_DB"."OPERATION_LOG_LOT" ALTER COLUMN "ID" SET DEFAULT nextval('"LOG_DB"."OPERATION_LOG_LOT_ID_seq"'::regclass);


--
-- Name: OPERATION_LOG_RECORD ID; Type: DEFAULT; Schema: LOG_DB; Owner: genissqladmin
--

ALTER TABLE ONLY "LOG_DB"."OPERATION_LOG_RECORD" ALTER COLUMN "ID" SET DEFAULT nextval('"LOG_DB"."OPERATION_LOG_RECORD_ID_seq"'::regclass);


--
-- Data for Name: OPERATION_LOG_LOT; Type: TABLE DATA; Schema: LOG_DB; Owner: genissqladmin
--

COPY "LOG_DB"."OPERATION_LOG_LOT" ("ID", "KEY_ZERO", "INIT_TIME") FROM stdin;
1	5fee6d77-a9fbf908-3c3af859-316f23a3-f18b9d10-c643a1b6-6f756643-d0e1a1cc	2024-07-04 10:16:15.988
2	ae01f96a-d4d1654a-ffb2137b-37d337a6-ca23579b-c551ae00-da361202-f44a18f3	2024-07-04 10:20:50.436
\.


--
-- Data for Name: OPERATION_LOG_RECORD; Type: TABLE DATA; Schema: LOG_DB; Owner: genissqladmin
--

COPY "LOG_DB"."OPERATION_LOG_RECORD" ("ID", "USER_ID", "OTP", "TIMESTAMP", "METHOD", "PATH", "ACTION", "BUILD_NO", "RESULT", "STATUS", "SIGNATURE", "LOT", "DESCRIPTION") FROM stdin;
1	tst-admin	\N	2024-07-04 10:16:52.164	POST	/login	controllers.Authentication.login()	develop	\N	200	640cbbc8-da950bbd-01d28156-25d23d3f-0401181d-026b7728-abe6a32a-13115e0c	1	Login
2	tst-admin	\N	2024-07-04 10:16:52.349	GET	/notifications	controllers.Notifications.getNotifications()	develop	\N	200	af71a79b-2104fe91-d6c636f7-48533de2-6e12b201-044a5ae8-79fce175-bda9858f	1	NotificationsAll
3	tst-admin	\N	2024-07-04 10:16:52.352	GET	/match-notifications	controllers.Notifications.getMatchNotifications()	develop	\N	200	f8c86370-ac8e14d4-5b07214c-7e14e291-cbd323ab-df457cfd-100682e7-341d05b5	1	NotificationsAll
4	tst-admin	\N	2024-07-04 10:16:52.497	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	78ba007b-d3c295bf-a93f085c-920de544-422ede4a-54c632e7-e9300373-1d025e00	1	NotificationsAll
5	tst-admin	\N	2024-07-04 10:16:52.509	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	f631cdc7-cd23b869-eca1bbc7-15d2cc58-6d67479a-cf3e4be1-12659519-1095cb1d	1	NotificationsAll
6	tst-admin	\N	2024-07-04 10:16:52.625	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	6a655d63-6ea580bb-62eddc0e-4c39e77f-6b844e20-bf0b2061-b18b7210-ef4f87d1	1	NotificationsAll
7	tst-admin	\N	2024-07-04 10:17:01.467	GET	/analysistypes	controllers.AnalysisTypes.list()	develop	\N	200	e4bdff3f-bd969065-03f0c741-a87c5e03-b2542211-f8bae6ac-27fc0d92-e786077f	1	AnalysisTypeRead
8	tst-admin	\N	2024-07-04 10:17:01.794	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	7a0e5f64-7ed9bf8c-3267164e-3c4fd466-dca74b95-e56b42ef-029c4b95-03a1e6e2	1	CategoryTreeRead
9	tst-admin	\N	2024-07-04 10:17:02.045	GET	/categories	controllers.Categories.list()	develop	\N	200	33cdfe27-f524d7ed-832bed0a-05180c20-41f96e86-047fb252-1d403d18-abfcbc92	1	CategoryTreeRead
10	tst-admin	\N	2024-07-04 10:21:09.398	POST	/login	controllers.Authentication.login()	develop	\N	200	87b7b5b9-8fedb7fd-565e0066-d6d5aa6c-a4bf1d96-1c1fdea1-d8d644c2-54a8a0a1	2	Login
11	tst-admin	\N	2024-07-04 10:21:09.558	GET	/notifications	controllers.Notifications.getNotifications()	develop	\N	200	b49d1659-1b448bfb-3747f7b5-3f837977-1f28e467-ebdb7a3f-d74709bb-bf9b042b	2	NotificationsAll
12	tst-admin	\N	2024-07-04 10:21:09.589	GET	/match-notifications	controllers.Notifications.getMatchNotifications()	develop	\N	200	067d5557-c1a065bc-8f832c9a-71011849-05e06727-70834446-c8ad8308-70c0b934	2	NotificationsAll
13	tst-admin	\N	2024-07-04 10:21:09.676	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	99daa18e-66312d85-ae2540d9-31398c36-15d53666-8652e7fc-08eff83e-6e0692ae	2	NotificationsAll
14	tst-admin	\N	2024-07-04 10:21:09.68	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	a25552a6-d35c45dd-0f09f5c6-ffdce1f5-f0ab5ab3-e2504c1f-5c5898b6-e91eecb4	2	NotificationsAll
15	tst-admin	\N	2024-07-04 10:21:09.803	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	9d52b7b2-fe6beae7-50a5b70d-1e6b7a32-2460bb10-f07b3e01-3515626a-735bcc71	2	NotificationsAll
16	tst-admin	\N	2024-07-04 10:21:16.362	GET	/populationBaseFreq	controllers.PopulationBaseFreq.getAllBaseNames()	develop	\N	200	65ca98de-ef5f76c1-46a2d94c-7ec19e02-bfaed9ff-4ca7796f-f2702590-f1adc007	2	PopulationBaseFreqRead
17	tst-admin	\N	2024-07-04 10:21:20.01	GET	/bulkupload/step1/batches	controllers.BulkUpload.getBatchesStep1()	develop	\N	200	f3db6fd6-074398a5-5e08d7bc-451d8540-82a7beec-c22a4ef5-2a42541f-7922bb09	2	BulkuploaderBatches
18	tst-admin	\N	2024-07-04 10:21:20.136	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	cba7e3a3-6f850e53-d4990253-f4e24bd0-ad97f990-6f25d1ff-23ceb983-733175a1	2	CategoryTreeRead
19	tst-admin	\N	2024-07-04 10:21:20.306	GET	/locus-full	controllers.Locis.listFull()	develop	\N	200	b7c293b3-23530ace-7f4694c9-18ee1b3b-c2ca959f-92b313e3-9a3e31c1-31af8681	2	LocusRead
20	tst-admin	\N	2024-07-04 10:21:25.866	GET	/country	controllers.Laboratories.listCountries()	develop	\N	200	22592f31-4567b4d0-ffde5914-053103f1-639d2ea9-47f7a1fb-141a29b4-c5769c06	2	CountryRead
21	tst-admin	\N	2024-07-04 10:21:25.961	GET	/laboratory/descriptive	controllers.Laboratories.listDescriptive()	develop	\N	200	5c801c62-7234bd2b-3a8f2db7-94366436-06b83e41-6a2b3e95-b9c2efce-bf3855c3	2	LaboratoryRead
22	tst-admin	\N	2024-07-04 10:21:46.82	GET	/provinces/AR	controllers.Laboratories.listProvinces()	develop	\N	200	2afe11d4-a2685ed9-c0996f82-7aca74e5-b6922dde-b071dca8-e8321d69-2f929d86	2	ProvincesRead
23	tst-admin	382198	2024-07-04 10:22:08.219	POST	/laboratory	controllers.Laboratories.addLab()	develop	MYLAB	200	c8d27684-5db44e8d-a4caa3fc-27c3b40a-ba8d3f34-ad8deb01-79cede02-3d116683	2	LaboratoryCreate
24	tst-admin	\N	2024-07-04 10:22:08.253	GET	/laboratory/descriptive	controllers.Laboratories.listDescriptive()	develop	\N	200	f6cba30b-c24858ce-12124dad-89d6dffa-6fe45dcd-153799e1-0c7d7675-6888afb0	2	LaboratoryRead
25	tst-admin	\N	2024-07-04 10:22:42.855	GET	/bulkupload/step1/batches	controllers.BulkUpload.getBatchesStep1()	develop	\N	200	d4f8d84c-20990c26-b40aa899-c22d2e4c-4d1a16b1-202687f0-ae67947b-6cd50a87	2	BulkuploaderBatches
26	tst-admin	\N	2024-07-04 10:22:42.905	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	20b6dc50-ebd53c3a-657d6151-46919f75-1eb207f0-b7f6d8e1-20201ae2-0fee992a	2	CategoryTreeRead
27	tst-admin	\N	2024-07-04 10:22:42.946	GET	/locus-full	controllers.Locis.listFull()	develop	\N	200	f6ee997a-e42fdf83-80610e49-9e7f61ea-5275bdc5-f08cda3d-4d63cdad-4baac544	2	LocusRead
28	tst-admin	\N	2024-07-04 10:22:50.891	POST	/bulkuploader	controllers.BulkUpload.uploadProtoProfiles()	develop	1	200	27ddbf11-be89f0e0-44d5fded-64c92bcf-a7e6155b-49fe0157-67ce4474-5e1fbccd	2	BulkuploaderCreate
29	tst-admin	\N	2024-07-04 10:22:50.938	GET	/bulkupload/step1/batches	controllers.BulkUpload.getBatchesStep1()	develop	\N	200	9d4e3ab8-b6e03bdb-9af5f531-e206348e-0174595b-ca87f378-e78e8986-5f178f47	2	BulkuploaderBatches
30	tst-admin	\N	2024-07-04 10:22:51.008	GET	/bulkupload/step1/protoprofiles	controllers.BulkUpload.getProtoProfilesStep1()	develop	\N	200	46ad9da2-b7d9799c-fc0a37fe-36a0e7d3-bbb20088-39eb7e6e-b1a4cd93-08026499	2	ProtoprofileRead
31	tst-admin	\N	2024-07-04 10:22:54.267	POST	/protoprofiles/multiple-status	controllers.BulkUpload.updateBatchStatus()	develop	\N	200	ced761ae-cef07502-f80e9afc-de7207d8-1b14b456-eb7fe904-06839ad9-5ad80cfe	2	ProtoprofileUpdateStatus
32	tst-admin	\N	2024-07-04 10:22:54.349	GET	/bulkupload/step1/protoprofiles	controllers.BulkUpload.getProtoProfilesStep1()	develop	\N	200	d5b19b98-cdb854d0-d6f8b807-29301dc0-1d5d7928-ae5be15a-29c929be-acd3d0c4	2	ProtoprofileRead
34	tst-admin	\N	2024-07-04 10:22:56.917	GET	/locus-full	controllers.Locis.listFull()	develop	\N	200	ec130097-0919739e-3dd533dc-8283b2b2-d752f3ac-7ac5d161-8fbdf60d-cafed8aa	2	LocusRead
33	tst-admin	\N	2024-07-04 10:22:56.886	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	40f7819e-e1fa328a-ba86f953-de74830d-8aee568c-68a2b45d-44f9ffe4-49c1af67	2	CategoryTreeRead
48	tst-admin	\N	2024-07-04 10:24:15.932	GET	/categoriesWithProfiles	controllers.Categories.listWithProfiles()	develop	\N	200	2d4120f5-9d2a2db1-d4d0a45a-e1238142-e90dc2d1-344f9c2f-eba3999e-99137100	2	CategoryTreeRead
54	tst-admin	\N	2024-07-04 10:24:31.803	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	6e1ab733-a61484cb-98ee409b-a0d5cd99-5d52d0ca-60e3945d-8f5e2432-be8a957a	2	NotificationsAll
35	tst-admin	\N	2024-07-04 10:22:56.922	GET	/bulkupload/step2/batches	controllers.BulkUpload.getBatchesStep2()	develop	\N	200	f103997d-22356874-caeb3c15-73e9a0a1-6986a62d-9d85d5ee-22174bd4-6bbd09a4	2	ProtoprofileRead
50	tst-admin	\N	2024-07-04 10:24:16.157	POST	/search/profileData/searchTotal	controllers.SearchProfileDatas.searchTotal()	develop	\N	200	97f02a63-84668aad-8fe1497d-06bf30a5-f8f47a88-d1adfa10-79d5d4ee-c52872f1	2	ProfiledataSearch
58	tst-admin	\N	2024-07-04 10:24:48.596	POST	/search/profileData/searchTotal	controllers.SearchProfileDatas.searchTotal()	develop	\N	200	e51fa940-9c85d183-259fb8a1-31b12dc5-85b1e6c4-0cd3d9c4-e0e65451-b97e733f	2	ProfiledataSearch
36	tst-admin	\N	2024-07-04 10:22:57.002	GET	/categories	controllers.Categories.list()	develop	\N	200	5a8d3a3b-fd957fc3-801736f4-ac6720f5-ac4353ae-05581851-8f6211e8-8257550a	2	CategoryTreeRead
41	tst-admin	\N	2024-07-04 10:23:16.399	POST	/protoprofiles/2/status	controllers.BulkUpload.updateProtoProfileStatus()	develop	\N	200	88302eab-0840a0c3-6bb2a00c-e2e84f04-093ac05c-0c5ec488-5545b32a-ea31fcf2	2	ProtoprofileUpdateStatus
52	tst-admin	\N	2024-07-04 10:24:26.03	POST	/search/profileData/searchTotal	controllers.SearchProfileDatas.searchTotal()	develop	\N	200	8f76e837-8b4e9c1a-120b07d0-3e8a9006-0a10c8d2-fc6f9e97-3fe31243-0b40cca4	2	ProfiledataSearch
37	tst-admin	\N	2024-07-04 10:22:58.638	POST	/protoprofiles/multiple-status	controllers.BulkUpload.updateBatchStatus()	develop	\N	400	5fc45a29-f021622c-40562bfa-02907791-b5eba0d6-b08cd978-f9e2265f-65760ccd	2	ProtoprofileUpdateStatus
43	tst-admin	\N	2024-07-04 10:23:41.072	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	07268ec3-26c91aa1-6bc0d97f-0a00b8ac-2198c70a-9195543b-0d7038d2-4c755a2f	2	CategoryTreeRead
56	tst-admin	\N	2024-07-04 10:24:48.564	GET	/categories	controllers.Categories.list()	develop	\N	200	b51279a4-1b485d3d-674b1b06-4cf1b548-55c4e941-7796badc-395dc8c1-d483e446	2	CategoryTreeRead
38	tst-admin	\N	2024-07-04 10:22:58.7	GET	/bulkupload/step2/protoprofiles	controllers.BulkUpload.getProtoProfilesStep2()	develop	\N	200	28d76cd4-fdd60cef-9f0214eb-a915b24d-8737c46f-c3d710bc-aa4ddea9-aa094b82	2	ProtoprofileRead
44	tst-admin	\N	2024-07-04 10:23:41.104	GET	/locus	controllers.Locis.list()	develop	\N	200	c825a43e-4815d46f-9e9947a2-7704cb66-36a3d2a4-b2ed95fd-90e942b8-937f79be	2	LocusRead
45	tst-admin	\N	2024-07-04 10:23:41.22	POST	/user-total-matches	controllers.Matching.getTotalMatches()	develop	\N	200	584f876a-2303e6ac-a142a7d3-832a7500-44c4c44c-54896ea7-c666937b-9647e9e4	2	MatchAll
57	tst-admin	\N	2024-07-04 10:24:48.583	GET	/categoriesWithProfiles	controllers.Categories.listWithProfiles()	develop	\N	200	1479371d-f12d15b3-356e1c12-e65dda1c-e2907233-3dd6399a-2d1dfbec-7471b36f	2	CategoryTreeRead
39	tst-admin	\N	2024-07-04 10:23:12.552	GET	/bulkupload/step2/protoprofiles	controllers.BulkUpload.getProtoProfilesStep2()	develop	\N	200	cdd5fc92-3acfca74-4a23390a-6ea6843a-8a1e2f62-fc6b2a34-35488496-0fd2be78	2	ProtoprofileRead
40	tst-admin	\N	2024-07-04 10:23:14.324	POST	/protoprofiles/1/status	controllers.BulkUpload.updateProtoProfileStatus()	develop	\N	200	e77a26f3-f3b9d133-b5ff622a-f6a3a8cc-7f511c2f-b1ef6e9f-475e0b1f-e2cd4a30	2	ProtoprofileUpdateStatus
46	tst-admin	\N	2024-07-04 10:24:13.191	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	d92608a4-547ebc9d-9d1c9a22-d74118dd-600afa1e-8b8509a8-f17a3030-0ced6c26	2	CategoryTreeRead
42	tst-admin	\N	2024-07-04 10:23:41.052	GET	/laboratory	controllers.Laboratories.list()	develop	\N	200	1aa953b3-62e1542d-4d491588-c6733da7-caf7923b-5e66a5bb-6b4c5cca-45fcef97	2	LaboratoryRead
51	tst-admin	\N	2024-07-04 10:24:16.221	POST	/search/profileData/search	controllers.SearchProfileDatas.search()	develop	\N	200	5550c351-e5e9d853-d96c5133-d08cdde5-a12ca826-c577afa3-d23634cb-a9c51c46	2	ProfiledataSearch
59	tst-admin	\N	2024-07-04 10:24:48.63	POST	/search/profileData/search	controllers.SearchProfileDatas.search()	develop	\N	200	1b6c19d5-18d072be-89e56af0-7c739f78-e23fe843-c1ce9930-2f1b162e-fcbc55fc	2	ProfiledataSearch
47	tst-admin	\N	2024-07-04 10:24:13.339	POST	/pedigreeMatches/count	controllers.Pedigrees.countMatches()	develop	\N	200	290bc120-ed403094-722af4fa-7e977592-558447b8-18ad68ac-2c32a151-53d09dfd	2	PedigreeAll
53	tst-admin	\N	2024-07-04 10:24:26.181	POST	/search/profileData/search	controllers.SearchProfileDatas.search()	develop	\N	200	7cb11e27-d6193825-21f60d1b-05a4fc6f-93940a54-336d3399-8e7b0e07-04e6470b	2	ProfiledataSearch
49	tst-admin	\N	2024-07-04 10:24:15.932	GET	/categories	controllers.Categories.list()	develop	\N	200	88efd130-de3a0aa9-572f6e14-4ef96535-380b1496-15dd603b-cbffee0c-cdf3bec6	2	CategoryTreeRead
55	tst-admin	\N	2024-07-04 10:24:31.908	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	617b3ce2-4865f97d-eb1bce1c-dbcbdd41-bc8fb646-cdff2ad1-0f6bb7fd-50ce37f2	2	NotificationsAll
60	tst-admin	\N	2024-07-04 10:24:53.417	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	8b19d6cd-088caba2-f45386d9-f1b3f32c-4700d3ca-f93ba3c5-104f1527-58d8f23c	2	NotificationsAll
61	tst-admin	\N	2024-07-04 10:24:53.469	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	148790ed-aebd13f0-fa69fc89-01c9a68e-4fb588d3-fbe9d973-e864f1cc-0cb41c43	2	NotificationsAll
62	tst-admin	\N	2024-07-04 10:24:55.556	GET	/strkits	controllers.StrKits.list()	develop	\N	200	5ecb5f78-5fec9bdd-6f40f117-874ad2bf-6de2d555-d92b1c71-9915a09c-45263a92	2	StrKitsAll
63	tst-admin	\N	2024-07-04 10:24:55.58	GET	/locus	controllers.Locis.list()	develop	\N	200	d28c3477-9a2ac329-bbca7143-85bf2f7c-c94c978f-6969ceed-636db788-abe58c54	2	LocusRead
64	tst-admin	\N	2024-07-04 10:24:55.589	GET	/profiles-labelsets	controllers.Profiles.getLabelsSets()	develop	\N	200	03b53f52-5b88e3eb-31b0eff0-09461fd7-1f17479e-13234b5b-45b956fb-9627db3c	2	ProfilesRead
65	tst-admin	\N	2024-07-04 10:24:55.667	GET	/analysistypes	controllers.AnalysisTypes.list()	develop	\N	200	1707b784-a41cc2e3-54cb52a1-287abb3a-2ef8c65f-ee233b80-8a2f881b-057f2fb8	2	AnalysisTypeRead
66	tst-admin	\N	2024-07-04 10:24:55.721	GET	/profiles-labelsets	controllers.Profiles.getLabelsSets()	develop	\N	200	2f9d028a-bb3a48e2-950b3428-ddc5dc92-47ff1d40-bf670701-5b20d73f-ca9a408c	2	ProfilesRead
67	tst-admin	\N	2024-07-04 10:24:55.743	GET	/profiles/full/AR-C-MYLAB-1102	controllers.Profiles.getFullProfile()	develop	\N	200	916b4b41-ec5b7c98-9effc1b5-26e5ea03-5a614a2b-afe417db-6fb3a926-2c1fd4f8	2	ProfilesRead
68	tst-admin	\N	2024-07-04 10:24:55.747	GET	/profiles/full/AR-C-MYLAB-1100	controllers.Profiles.getFullProfile()	develop	\N	200	2a7de432-852081d3-5dd2f46a-67e72d2a-85d56c5e-c5e53500-142c8008-8411a007	2	ProfilesRead
69	tst-admin	\N	2024-07-04 10:24:55.749	GET	/profiledataWithAssociations	controllers.ProfileData.findByCodeWithAssociations()	develop	\N	400	0030919a-dbdffd99-45020f92-ca44ebb2-1bac93e1-3d4efe34-bf08f6de-00b7eadc	2	ProfiledataRead
70	tst-admin	\N	2024-07-04 10:24:55.816	GET	/profiledataWithAssociations	controllers.ProfileData.findByCodeWithAssociations()	develop	\N	200	e51da7d5-9b7abcdf-b4f28273-016be323-3084cfa8-fba9a556-e66756dd-9ddb0bca	2	ProfiledataRead
71	tst-admin	\N	2024-07-04 10:24:55.846	GET	/profiles/AR-C-MYLAB-1102	controllers.Profiles.findByCode()	develop	\N	200	6268319f-c1da7464-e1e0b299-d3b07f77-3e49e0ea-84431774-fbf7f7c7-a9ba7271	2	ProfilesRead
72	tst-admin	\N	2024-07-04 10:24:55.861	GET	/mixture/compare	controllers.Matching.getComparedMixtureGene()	develop	\N	200	a179ea09-2f71a371-c0316953-9937b22b-71b2df30-90870ec1-ceb085e4-6121bcd1	2	MatchAll
73	tst-admin	\N	2024-07-04 10:24:55.881	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	8aecede7-2e7f6c2c-43c7b6a7-63e06430-87ff0432-4cc3c2af-cea048be-40b0826f	2	CategoryTreeRead
74	tst-admin	\N	2024-07-04 10:24:55.933	GET	/profilesdata	controllers.ProfileData.findByCodes()	develop	\N	200	fa232e54-550e1bf7-ed5ee207-e3d94b77-b645b8a6-1e03c181-4ca21432-568201cb	2	ProfilesRead
75	tst-admin	\N	2024-07-04 10:24:55.952	GET	/getByMatchedProfileId	controllers.Matching.getByMatchedProfileId()	develop	\N	200	82b0ae43-a54a44d4-acc3af86-abbd3e8d-95f30897-0374e9a2-7b9654ed-ae00e82a	2	MatchAll
76	tst-admin	\N	2024-07-04 10:24:56.039	GET	/profiles/AR-C-MYLAB-1100	controllers.Profiles.findByCode()	develop	\N	200	8791d280-a135549e-427d4949-8c5b1ede-53de1ead-4de73914-582ec59d-45538987	2	ProfilesRead
77	tst-admin	\N	2024-07-04 10:24:56.099	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	c0f06a56-c4382f9e-3bb66ef3-6a63eb82-ebb89827-561b27f3-cb276402-29c810e5	2	CategoryTreeRead
78	tst-admin	\N	2024-07-04 10:24:56.142	GET	/populationBaseFreqCharacteristics	controllers.PopulationBaseFreq.getAllBasesCharacteristics()	develop	\N	200	d405ffc6-69b6f2ba-cb823735-eb54d352-83346319-a7f94f1c-fe08a0c6-47df94be	2	PopulationBaseFreqRead
79	tst-admin	\N	2024-07-04 10:24:56.15	GET	/profiledata-complete/AR-C-MYLAB-1102	controllers.ProfileData.getByCode()	develop	\N	200	398c47cf-2a59b545-d6b29201-3f3a28da-0b1a497d-0dc2c734-20eedea9-4c6e4865	2	ProfiledataRead
80	tst-admin	\N	2024-07-04 10:24:56.21	GET	/laboratory/MYLAB	controllers.Laboratories.getLaboratory()	develop	\N	200	c2af0f5f-ffb05353-36d5b927-b0f3e3bb-0738184d-f10a53ce-1b9be5ee-bf361137	2	LaboratoryRead
81	tst-admin	\N	2024-07-04 10:24:56.465	POST	/lr	controllers.Matching.getLR()	develop	\N	200	e7f1fb1c-c07f2b04-1b7abf40-b1926ff6-ef973eae-c0e74a1d-fbce883f-91a30bbe	2	MatchAll
82	tst-admin	\N	2024-07-04 10:26:47.81	GET	/categoriesWithProfiles	controllers.Categories.listWithProfiles()	develop	\N	200	c42ec171-87ed7b77-ded6267e-ea95bb6b-7de03ccd-1c753df8-75386357-d1254089	2	CategoryTreeRead
83	tst-admin	\N	2024-07-04 10:26:47.834	POST	/search/profileData/searchTotal	controllers.SearchProfileDatas.searchTotal()	develop	\N	200	912b1680-6977a7d2-1a38df43-4f995d08-0112a23c-43b10c3b-c995a28b-74be3a96	2	ProfiledataSearch
84	tst-admin	\N	2024-07-04 10:26:47.92	GET	/categories	controllers.Categories.list()	develop	\N	200	7dd6b331-52a40116-c88b5c4c-afdb6e73-4a4bfa5a-e48dbf89-3645eafc-8bdf2c3d	2	CategoryTreeRead
85	tst-admin	\N	2024-07-04 10:26:50.599	GET	/locus	controllers.Locis.list()	develop	\N	200	bfce7755-4ee0dc7a-8e06fde6-e50e495c-18c0c33b-b9daeea0-b768066a-aeff422a	2	LocusRead
86	tst-admin	\N	2024-07-04 10:26:50.629	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	ca81787f-b69cc947-2f91d6d5-4324f70a-f7156daa-dc02ae54-ea1d3eee-bde161f3	2	CategoryTreeRead
87	tst-admin	\N	2024-07-04 10:26:50.744	GET	/laboratory	controllers.Laboratories.list()	develop	\N	200	886a6abf-10d6d25e-1416b7a2-6d90a265-601a4833-301cd646-910a70d9-1dc0e506	2	LaboratoryRead
88	tst-admin	\N	2024-07-04 10:26:50.789	POST	/user-total-matches	controllers.Matching.getTotalMatches()	develop	\N	200	37b33b1c-0e01652f-bdd6467e-40a54a41-1afc48bc-5bedcd01-35498714-e3f459ae	2	MatchAll
89	tst-admin	\N	2024-07-04 10:26:53.955	GET	/bulkupload/step1/batches	controllers.BulkUpload.getBatchesStep1()	develop	\N	200	a8c8669d-b652ae68-69df6f12-3fcad9cc-cffdbc8c-de87c879-689e71a9-fae3c923	2	BulkuploaderBatches
90	tst-admin	\N	2024-07-04 10:26:53.966	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	bf8e9579-355c1777-ca513b17-0e4c615b-3d2d1022-5b502074-6af8d391-bbf48c8c	2	CategoryTreeRead
91	tst-admin	\N	2024-07-04 10:26:53.99	GET	/locus-full	controllers.Locis.listFull()	develop	\N	200	34ae1570-2a67cf4b-1ec7ed6a-e340d645-8a177e7b-24a4cd74-c835fe01-7d02be53	2	LocusRead
96	tst-admin	\N	2024-07-04 10:27:01.006	GET	/bulkupload/step1/protoprofiles	controllers.BulkUpload.getProtoProfilesStep1()	develop	\N	200	87112b7b-29ba6e98-d9fad6a3-09d83c37-24db5dcd-38efc36f-3e7f4915-2afe7244	2	ProtoprofileRead
106	tst-admin	\N	2024-07-04 10:27:29.532	GET	/locus	controllers.Locis.list()	develop	\N	200	8a052d53-e4c5412e-8282f4e3-ab2588c8-c9649094-4212e796-903b268c-55807b65	2	LocusRead
116	tst-admin	\N	2024-07-04 10:27:43.64	GET	/locus	controllers.Locis.list()	develop	\N	200	3a5237fe-8914ef1e-5d7fdb4d-596833cd-d1aacbb4-c7e7c5e5-a265e122-a43aa0ed	2	LocusRead
92	tst-admin	\N	2024-07-04 10:26:59.332	POST	/bulkuploader	controllers.BulkUpload.uploadProtoProfiles()	develop	2	200	671c0e20-e6c76814-ac8ddf49-057fb878-a0a08975-30717203-a78b8311-d71bf33e	2	BulkuploaderCreate
98	tst-admin	\N	2024-07-04 10:27:03.497	GET	/categories	controllers.Categories.list()	develop	\N	200	d66bb2d3-29b278d1-227ac3e4-7787f560-b3e9ea7a-eb0ebd86-af80b1c0-76d9de32	2	CategoryTreeRead
93	tst-admin	\N	2024-07-04 10:26:59.351	GET	/bulkupload/step1/batches	controllers.BulkUpload.getBatchesStep1()	develop	\N	200	803b816d-3308ab65-e1888d38-c90e58bf-e82ab2c8-fed19893-f6803217-9db977c4	2	BulkuploaderBatches
99	tst-admin	\N	2024-07-04 10:27:03.556	GET	/bulkupload/step2/batches	controllers.BulkUpload.getBatchesStep2()	develop	\N	200	0080a00c-c12db7d1-e004a353-607be0fc-ce0fa5cc-129989d3-d0c99c78-812c0a54	2	ProtoprofileRead
107	tst-admin	\N	2024-07-04 10:27:29.54	POST	/user-total-matches	controllers.Matching.getTotalMatches()	develop	\N	200	c53402a3-87114685-c155b26b-dc6cd863-2faaa687-729d956e-427d5493-5d8de2c4	2	MatchAll
120	tst-admin	\N	2024-07-04 10:27:45.849	POST	/user-total-matches-group	controllers.Matching.getTotalMatchesByGroup()	develop	\N	200	e3f05eff-30457304-17ab7ed9-f871b101-a406dfca-2813e384-117d37ab-92d5ebfd	2	MatchAll
94	tst-admin	\N	2024-07-04 10:26:59.416	GET	/bulkupload/step1/protoprofiles	controllers.BulkUpload.getProtoProfilesStep1()	develop	\N	200	2aaed68d-c585c327-99171b37-b8e4ca74-903d3390-e7396880-41757f12-7ce4f0d4	2	ProtoprofileRead
104	tst-admin	\N	2024-07-04 10:27:09.58	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	90bd7354-b5ff2e24-9b5dcf74-0aa87377-ba3f962d-08610437-c3219b47-1e8d6caf	2	NotificationsAll
95	tst-admin	\N	2024-07-04 10:27:00.932	POST	/protoprofiles/multiple-status	controllers.BulkUpload.updateBatchStatus()	develop	\N	200	9dc834e5-93c77933-a19bcab2-1d4ef97a-88f41d35-2e1f3718-7f56de07-54b2b861	2	ProtoprofileUpdateStatus
105	tst-admin	\N	2024-07-04 10:27:29.499	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	5dc52e25-c36b1cfe-98cf0705-7756a1d6-12952551-89134ef1-3acb553b-8aef57a9	2	CategoryTreeRead
97	tst-admin	\N	2024-07-04 10:27:03.481	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	a33656b0-3c84d7bb-0872c7a6-82dc3f11-c4110e88-257f2947-abca72fe-5f25513a	2	CategoryTreeRead
108	tst-admin	\N	2024-07-04 10:27:29.561	GET	/laboratory	controllers.Laboratories.list()	develop	\N	200	46c969d0-b29b72a3-47d5af05-9514e539-1d0db2a4-216bcb20-7cc66ec6-4c205038	2	LaboratoryRead
113	tst-admin	\N	2024-07-04 10:27:30.122	GET	/profiles/full/AR-C-MYLAB-1104	controllers.Profiles.getFullProfile()	develop	\N	200	efe4f7c7-d2b55b17-c8d22ae7-052a52e5-0f43a2aa-77e00baa-94fe216a-cdf9ac82	2	ProfilesRead
119	tst-admin	\N	2024-07-04 10:27:44.418	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	bcabc313-8c81665a-3dcc863f-fa52cdbe-59a3c1e1-d7945913-b50a67e4-6660baf6	2	CategoryTreeRead
125	tst-admin	\N	2024-07-04 10:27:46.548	GET	/profiles/full/AR-C-MYLAB-1105	controllers.Profiles.getFullProfile()	develop	\N	200	5a337efc-26819427-f209b0a7-d9767729-363c16fa-14733f4d-46400fb5-6bfe7171	2	ProfilesRead
100	tst-admin	\N	2024-07-04 10:27:03.571	GET	/locus-full	controllers.Locis.listFull()	develop	\N	200	a2f8920e-a2689cea-12e62a0e-84525753-589ea5a4-40c73e7c-11b27943-b92dbcb7	2	LocusRead
109	tst-admin	\N	2024-07-04 10:27:30.048	POST	/user-matches	controllers.Matching.getMatches()	develop	\N	200	f6881159-b222f36b-cd996a9c-fc2a9e91-a8c2dc94-11f5c6a5-c2a0e853-cd3e8d07	2	MatchAll
114	tst-admin	\N	2024-07-04 10:27:30.126	GET	/profiledataWithAssociations	controllers.ProfileData.findByCodeWithAssociations()	develop	\N	200	eeeb9fba-2111600a-f4df7086-3fc77b18-0554a9a9-77097cd2-6fd38f7a-e47f69bc	2	ProfiledataRead
121	tst-admin	\N	2024-07-04 10:27:45.85	POST	/user-total-matches-group	controllers.Matching.getTotalMatchesByGroup()	develop	\N	200	d282c93d-3a509d80-73be54e7-29968adb-3e680fee-f02d1bd3-b0dab50a-3812547a	2	MatchAll
126	tst-admin	\N	2024-07-04 10:27:46.723	POST	/scenarios/search	controllers.Scenarios.search()	develop	\N	200	e9f235b7-d661e39f-799626d3-718234b6-b7e4671a-0636b8d2-e398b919-fa6add02	2	ScenarioRead
101	tst-admin	\N	2024-07-04 10:27:05.372	POST	/protoprofiles/multiple-status	controllers.BulkUpload.updateBatchStatus()	develop	\N	200	8c65e70f-2dc596ca-565ddd9c-afeaf795-b1aba954-a8434714-8c7942db-b0558bdc	2	ProtoprofileUpdateStatus
110	tst-admin	\N	2024-07-04 10:27:30.087	GET	/profiledataWithAssociations	controllers.ProfileData.findByCodeWithAssociations()	develop	\N	200	e74e510b-01c2e2c6-bb9c49b0-f9a8e5ae-b108e74f-92f44dff-83ba11f8-920f5ccb	2	ProfiledataRead
115	tst-admin	\N	2024-07-04 10:27:30.134	GET	/profiles-labelsets	controllers.Profiles.getLabelsSets()	develop	\N	200	d115321b-ea5a9d95-d9cd1016-79649721-93613e2a-759896ea-af4e262a-1ad2f7bc	2	ProfilesRead
122	tst-admin	\N	2024-07-04 10:27:46.427	POST	/user-matches-group	controllers.Matching.getMatchesByGroup()	develop	\N	200	4caaace5-832f20fc-bf37ac3d-0d18f859-f56b71d8-a9708f92-84dcf4d7-5d5a7f9f	2	MatchAll
102	tst-admin	\N	2024-07-04 10:27:05.429	GET	/bulkupload/step2/protoprofiles	controllers.BulkUpload.getProtoProfilesStep2()	develop	\N	200	e93d26ed-8618951e-422c6568-1abf50ff-77a4d54b-e80b432e-21360413-d92a620b	2	ProtoprofileRead
111	tst-admin	\N	2024-07-04 10:27:30.087	GET	/profiles/full/AR-C-MYLAB-1105	controllers.Profiles.getFullProfile()	develop	\N	200	aedc133f-cb0e52b8-e9916fc6-cdf6afc2-34b08846-7acb644b-f442c86b-14bf42b7	2	ProfilesRead
117	tst-admin	\N	2024-07-04 10:27:44.229	GET	/analysistypes	controllers.AnalysisTypes.list()	develop	\N	200	0f737300-7c457405-c6ce0294-66c38d95-ac297ad4-754ecaf5-0b3383da-7326115c	2	AnalysisTypeRead
123	tst-admin	\N	2024-07-04 10:27:46.516	GET	/profiledataWithAssociations	controllers.ProfileData.findByCodeWithAssociations()	develop	\N	200	79be5f2f-063a6f96-7afdac6c-73d44791-78ef0ecb-e1b3a765-83dd0f67-68c0267b	2	ProfiledataRead
103	tst-admin	\N	2024-07-04 10:27:09.515	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	b6961cd5-1219788f-8d149c24-c53dad86-f1bf84a5-f2f67165-54b1e91d-e32b81db	2	NotificationsAll
112	tst-admin	\N	2024-07-04 10:27:30.105	GET	/profiles-labelsets	controllers.Profiles.getLabelsSets()	develop	\N	200	b23bfed2-1295c7f1-57195441-8133bf6a-3771be23-8ae3136f-e8371575-13b3f9de	2	ProfilesRead
118	tst-admin	\N	2024-07-04 10:27:44.404	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	8d8dd8b3-4e1707f1-05c87259-60ca94a1-804e5441-47605635-2a861a0a-b591e845	2	MatchAll
124	tst-admin	\N	2024-07-04 10:27:46.519	GET	/profiles-labelsets	controllers.Profiles.getLabelsSets()	develop	\N	200	d7a21b47-b4ae627c-97d6cfb1-0ba7f81e-0e9ebede-70599a39-54719307-d202d3c2	2	ProfilesRead
127	tst-admin	\N	2024-07-04 10:27:59.808	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	09ce03cd-e63f5bac-0d26fbf8-abc4f831-fc7e44b9-52d546ad-0dfc0998-6f39355a	2	NotificationsAll
128	tst-admin	\N	2024-07-04 10:27:59.899	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	fe2a7df1-8a7a9dea-9bf81e7a-8e251232-a3a6102c-360bd577-c7752783-ce3befe3	2	NotificationsAll
129	tst-admin	\N	2024-07-04 10:28:02.423	GET	/locus	controllers.Locis.list()	develop	\N	200	6406189b-574a735f-203c68e3-21e0af01-50afe5ea-b667fef4-68ea69e5-3d8a7abf	2	LocusRead
130	tst-admin	\N	2024-07-04 10:28:02.423	GET	/analysistypes	controllers.AnalysisTypes.list()	develop	\N	200	f2587a47-2dd54b5a-59c3469a-b2a907f0-4e8a8d11-d2855c78-fb7687fe-dc4ad06b	2	AnalysisTypeRead
131	tst-admin	\N	2024-07-04 10:28:02.507	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	5b3f1a6c-46df810b-ce6a684c-746a0b76-b9c50dc9-2b051231-03db8e0a-9eb01de5	2	MatchAll
132	tst-admin	\N	2024-07-04 10:28:02.521	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	f2f5ea79-df1e423b-eba02b69-c4103364-dcdbd347-6c7e78bc-3931b3f6-1fc5eaac	2	CategoryTreeRead
133	tst-admin	\N	2024-07-04 10:28:02.812	GET	/pedigree/full/undefined	NotFound.NotFound()	develop	\N	400	95577735-5f5c6b99-7f0be34f-f22de3dd-e135f168-b2a3be6d-93860ab8-e6d627c1	2	PedigreeAll
134	tst-admin	\N	2024-07-04 10:28:02.812	GET	/court-case-profiles/total	NotFound.NotFound()	develop	\N	400	195f57d6-0ceda220-dabfe777-f337acaa-52c16442-6fdbc8b6-98e6c5fb-e92e9e7d	2	PedigreeCourtCaseProfilesGet
135	tst-admin	\N	2024-07-04 10:28:02.825	GET	/pedigree/full/undefined	NotFound.NotFound()	develop	\N	400	99068b82-ab1aaf02-af42e8cc-ab30db95-1c4197be-5a608a07-c4ab23b6-33079ada	2	PedigreeAll
136	tst-admin	\N	2024-07-04 10:28:02.847	GET	/pedigree/scenarios/undefined	NotFound.NotFound()	develop	\N	400	5ec451be-8d5cd2b3-bc37fef0-7a70d081-68db6d9a-bcd3563b-7b8d0572-887767dd	2	PedigreeScenarioRead
137	tst-admin	\N	2024-07-04 10:28:02.863	GET	/locus-full	controllers.Locis.listFull()	develop	\N	200	f86167bc-abbf919a-66f3b0b1-d081d618-41543759-d053c92f-b5a670d5-c852584b	2	LocusRead
138	tst-admin	\N	2024-07-04 10:28:02.925	GET	/locus	controllers.Locis.list()	develop	\N	200	9e081d58-2c3882ef-56bd8cc3-18b81951-e9dddeba-a6d8e2c8-78d9b22a-3983322c	2	LocusRead
139	tst-admin	\N	2024-07-04 10:28:02.928	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	ef5d9264-b300f873-ac6464f2-a02b8db0-151bfb15-59bd743e-1a30cf90-86e46659	2	CategoryTreeRead
140	tst-admin	\N	2024-07-04 10:28:02.975	GET	/laboratory	controllers.Laboratories.list()	develop	\N	200	b49fd24b-2f5863a5-7bbb7d77-11b200c9-438ed3fa-b230a9e0-15b61873-c39147df	2	LaboratoryRead
141	tst-admin	\N	2024-07-04 10:28:02.989	POST	/user-total-matches-group	controllers.Matching.getTotalMatchesByGroup()	develop	\N	200	169762b5-32986b4e-ce0afac4-b0527197-22878344-9625d1cc-3ab3bb1a-35792f7f	2	MatchAll
142	tst-admin	\N	2024-07-04 10:28:03.011	GET	/court-case-profiles/total	NotFound.NotFound()	develop	\N	400	ed4e4150-e8d5916a-9ee269e3-35fcf7dd-ec42d985-9d4d9230-aaf01848-c15dfd26	2	PedigreeCourtCaseProfilesGet
143	tst-admin	\N	2024-07-04 10:28:03.045	POST	/user-total-matches-group	controllers.Matching.getTotalMatchesByGroup()	develop	\N	200	4fd801b0-a145f9bb-da6db6f8-927a8eb5-42a1058e-134dcb6e-76a8473b-5517b050	2	MatchAll
144	tst-admin	\N	2024-07-04 10:28:03.061	POST	/user-total-matches	controllers.Matching.getTotalMatches()	develop	\N	200	9eb63464-cdda64a0-b99426b5-1299c3a4-65b2a036-a36e2e43-90511701-23103d9f	2	MatchAll
145	tst-admin	\N	2024-07-04 10:28:03.097	GET	/court-case-profiles	NotFound.NotFound()	develop	\N	400	090a4790-2fcd2ea9-fbccfee9-646fad93-7d083f8f-d79c535d-37f7ce94-528daa3d	2	PedigreeCourtCaseProfilesGet
146	tst-admin	\N	2024-07-04 10:28:03.338	POST	/user-matches-group	controllers.Matching.getMatchesByGroup()	develop	\N	200	91e29898-8d75947a-1b32974c-d55a53ed-b82552c2-28b14bef-4949630d-e84719d9	2	MatchAll
147	tst-admin	\N	2024-07-04 10:28:03.354	GET	/profiles-labelsets	controllers.Profiles.getLabelsSets()	develop	\N	200	4062cf62-22d53247-7e807334-82cbb805-62f25f69-80b46ce0-8cae6968-81632fb4	2	ProfilesRead
148	tst-admin	\N	2024-07-04 10:28:03.365	POST	/scenarios/search	controllers.Scenarios.search()	develop	\N	200	277e847b-3b18c480-840ee7dd-9a2b83be-20a02791-414ef7fd-a85edbde-5c458256	2	ScenarioRead
149	tst-admin	\N	2024-07-04 10:28:03.373	GET	/profiles/full/AR-C-MYLAB-1105	controllers.Profiles.getFullProfile()	develop	\N	200	8c775701-526c84ba-137e1c57-bfe375d9-6856e824-1782e42f-d702d446-462888b8	2	ProfilesRead
150	tst-admin	\N	2024-07-04 10:28:03.375	GET	/profiledataWithAssociations	controllers.ProfileData.findByCodeWithAssociations()	develop	\N	200	2b84d5ed-4b93e03f-8f1efa48-50fb7127-573344ef-70ca195c-e0a41f02-bec5940b	2	ProfiledataRead
151	tst-admin	\N	2024-07-04 10:28:04.994	GET	/pedigree/caseTypes	controllers.Pedigrees.getCaseTypes()	develop	\N	200	9d332699-d2b3a8f7-697b551b-20be3f5a-45d91838-a654de17-938af3d4-cf9a420d	2	CaseTypeRead
152	tst-admin	\N	2024-07-04 10:28:05.035	POST	/pedigree/total-court-cases	controllers.Pedigrees.getTotalCourtCases()	develop	\N	200	8589d701-82d89f75-55b1ccb0-853da384-db1d9adf-7cafccfb-343fd605-5a56367d	2	PedigreeAll
153	tst-admin	\N	2024-07-04 10:28:06.381	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	17556e60-94ece752-677d87ce-0f7c6bab-ccbf73e4-125fb72e-27b44cff-556b177c	2	CategoryTreeRead
154	tst-admin	\N	2024-07-04 10:28:06.405	POST	/pedigreeMatches/count	controllers.Pedigrees.countMatches()	develop	\N	200	42c62195-1eeb0f36-32fe18b6-30f23323-288dec49-d3403bbe-798b482b-a28ed5d1	2	PedigreeAll
155	tst-admin	\N	2024-07-04 10:28:09.305	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	af82dc58-e7bd8da5-ae8aeafc-8d8fd6a8-3643e165-423c16ed-203b1485-2b065ca3	2	CategoryTreeRead
156	tst-admin	\N	2024-07-04 10:28:09.318	GET	/locus	controllers.Locis.list()	develop	\N	200	561c31b8-7f1ed44c-46487ccc-6a64c0a6-214847d7-2adcde4c-f9176104-0961c963	2	LocusRead
161	tst-admin	\N	2024-07-04 10:28:09.762	GET	/profiles-labelsets	controllers.Profiles.getLabelsSets()	develop	\N	200	67d3d8b0-d12a2c2e-1a00e61f-e07e089e-a1912f62-b8400a33-3ec5b2fa-740485d9	2	ProfilesRead
170	tst-admin	\N	2024-07-04 10:28:14.592	POST	/user-total-matches-group	controllers.Matching.getTotalMatchesByGroup()	develop	\N	200	e25ecc89-21edd434-bfb8a28e-42e39411-5d7437ce-077fd2d4-9af29357-cc20ffa7	2	MatchAll
180	tst-admin	\N	2024-07-04 10:28:20.657	GET	/laboratory	controllers.Laboratories.list()	develop	\N	200	dc72558a-f1a9746b-aef59b44-1107932c-2bee979d-15ffafbe-6444c4ff-65f63d18	2	LaboratoryRead
185	tst-admin	\N	2024-07-04 10:28:21.136	GET	/profiledataWithAssociations	controllers.ProfileData.findByCodeWithAssociations()	develop	\N	200	bd42a27e-27a8ff18-aad1b379-516999d4-4b17947f-75180d28-84f56fa6-0f84baa0	2	ProfiledataRead
157	tst-admin	\N	2024-07-04 10:28:09.328	POST	/user-total-matches	controllers.Matching.getTotalMatches()	develop	\N	200	577eec2d-4769e076-3b27cf11-4aa2ea46-171aa18e-b61f83b3-f7219290-d2c9ae6e	2	MatchAll
162	tst-admin	\N	2024-07-04 10:28:09.788	GET	/profiles-labelsets	controllers.Profiles.getLabelsSets()	develop	\N	200	f2e68a70-f7d6df2a-10033cbe-a750133c-f029ac5e-252d5cb1-8bb25355-86c3bcac	2	ProfilesRead
171	tst-admin	\N	2024-07-04 10:28:14.605	POST	/user-total-matches-group	controllers.Matching.getTotalMatchesByGroup()	develop	\N	200	a046597d-69ae6f16-5cd58237-c2bf2791-df79ba0e-9ddc2154-32ca95fa-850dd99b	2	MatchAll
181	tst-admin	\N	2024-07-04 10:28:21.04	POST	/user-matches	controllers.Matching.getMatches()	develop	\N	200	d8b21a4b-0d92651b-be273ce5-2e3b3121-852dea34-a825541b-10bd6cc5-9e10ab38	2	MatchAll
186	tst-admin	\N	2024-07-04 10:28:21.144	GET	/profiledataWithAssociations	controllers.ProfileData.findByCodeWithAssociations()	develop	\N	200	a2b1a6ee-07715142-5f27ff9a-9ae41171-9afec65f-1ad6538c-9d28c8f3-bd68d590	2	ProfiledataRead
158	tst-admin	\N	2024-07-04 10:28:09.346	GET	/laboratory	controllers.Laboratories.list()	develop	\N	200	3234142a-d87813d0-2301e5f0-404b6fa1-75d8c306-45e46029-2268d4eb-a52210ea	2	LaboratoryRead
166	tst-admin	\N	2024-07-04 10:28:14.09	GET	/locus	controllers.Locis.list()	develop	\N	200	ab357e0f-cdeaa08d-fdb9bc36-31e7c3be-467409e5-f5198446-791a3525-d56530d5	2	LocusRead
177	tst-admin	\N	2024-07-04 10:28:20.61	GET	/locus	controllers.Locis.list()	develop	\N	200	b57082c6-a577a315-bb050c03-bca5fb46-0b830a3d-d95a8f4f-2e77d2af-4e3eec9d	2	LocusRead
182	tst-admin	\N	2024-07-04 10:28:21.116	GET	/profiles/full/AR-C-MYLAB-1105	controllers.Profiles.getFullProfile()	develop	\N	200	fbac633f-22c2da2b-66035fda-24186974-97c49f60-5175657a-ece0d988-cbc5dcea	2	ProfilesRead
187	tst-admin	\N	2024-07-04 10:28:21.165	GET	/profiles/full/AR-C-MYLAB-1104	controllers.Profiles.getFullProfile()	develop	\N	200	00465896-838f0139-466d29d0-761f6409-499b7bf9-38bb2d1c-a0775fdf-8b2b7180	2	ProfilesRead
159	tst-admin	\N	2024-07-04 10:28:09.709	POST	/user-matches	controllers.Matching.getMatches()	develop	\N	200	aaddad30-432c3cc3-a1c25405-2d0e77f8-154b4ca5-29a9172e-c652146b-c6fafca6	2	MatchAll
168	tst-admin	\N	2024-07-04 10:28:14.164	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	636737bd-2ee7ef28-9e043cce-37e6f537-8ffba57b-5a120d15-43b648e7-f307c960	2	MatchAll
178	tst-admin	\N	2024-07-04 10:28:20.637	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	2bbe3f6b-154bb826-5258f41e-0e144813-0f87e5d3-5321c745-97d74a74-01f26bfa	2	CategoryTreeRead
183	tst-admin	\N	2024-07-04 10:28:21.126	GET	/profiles-labelsets	controllers.Profiles.getLabelsSets()	develop	\N	200	05cf0282-012b2f2f-da6c6987-5e4343a2-c1ea674b-8b682668-01c2f807-8798e146	2	ProfilesRead
160	tst-admin	\N	2024-07-04 10:28:09.76	GET	/profiles/full/AR-C-MYLAB-1105	controllers.Profiles.getFullProfile()	develop	\N	200	e1e6b35d-0c113bbe-110add56-eccbc2f3-6595887c-edd3f459-384617df-230262ee	2	ProfilesRead
163	tst-admin	\N	2024-07-04 10:28:09.792	GET	/profiledataWithAssociations	controllers.ProfileData.findByCodeWithAssociations()	develop	\N	200	dafdf89b-e2747e75-90629167-77e77e04-9c7b4af1-b06e0d4e-fd87a700-19f38bd7	2	ProfiledataRead
164	tst-admin	\N	2024-07-04 10:28:09.802	GET	/profiledataWithAssociations	controllers.ProfileData.findByCodeWithAssociations()	develop	\N	200	aa367a1e-5daf76d9-a21d6eeb-45e759b4-d2fcf07d-57a4fbdf-57822b7d-4ad25316	2	ProfiledataRead
165	tst-admin	\N	2024-07-04 10:28:09.825	GET	/profiles/full/AR-C-MYLAB-1104	controllers.Profiles.getFullProfile()	develop	\N	200	6507ad88-8525c7f9-23b7d9cd-48a24e0d-6bd29a60-68ecaef0-0e1d4abb-3cae9292	2	ProfilesRead
169	tst-admin	\N	2024-07-04 10:28:14.215	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	d4726866-cea63b81-3b944819-0ce9e232-79da7455-9eb05bd6-fc9de4da-6a05ed6e	2	CategoryTreeRead
173	tst-admin	\N	2024-07-04 10:28:14.983	GET	/profiles/full/AR-C-MYLAB-1104	controllers.Profiles.getFullProfile()	develop	\N	200	d4ef2092-254a777e-02212d67-f33f8027-132b6b5d-65ebdfbd-1e1e4a82-e5f92b2d	2	ProfilesRead
174	tst-admin	\N	2024-07-04 10:28:14.991	GET	/profiledataWithAssociations	controllers.ProfileData.findByCodeWithAssociations()	develop	\N	200	f1984df4-60edd22b-4d2bd3e7-db3c309f-52037368-1fb8de28-c7c2963c-bcd0c134	2	ProfiledataRead
175	tst-admin	\N	2024-07-04 10:28:14.997	GET	/profiles-labelsets	controllers.Profiles.getLabelsSets()	develop	\N	200	e88ca646-ebabea84-697d924f-f9607d98-85fc6b27-a7520d76-165e0a02-db787068	2	ProfilesRead
179	tst-admin	\N	2024-07-04 10:28:20.647	POST	/user-total-matches	controllers.Matching.getTotalMatches()	develop	\N	200	1c04dc21-a317da6d-18742970-14249554-0ac298fc-acccd9d0-01724f0a-921e698b	2	MatchAll
184	tst-admin	\N	2024-07-04 10:28:21.126	GET	/profiles-labelsets	controllers.Profiles.getLabelsSets()	develop	\N	200	c2033c5d-dee7e9da-607d0a8a-fa60da3f-aea675ac-fbc8dac8-7a77394f-64880845	2	ProfilesRead
167	tst-admin	\N	2024-07-04 10:28:14.123	GET	/analysistypes	controllers.AnalysisTypes.list()	develop	\N	200	bf4b0dee-c2fbedd9-d1ce72c3-177dddad-68033d8b-c87cc4a2-2cad32cd-4d81f78a	2	AnalysisTypeRead
176	tst-admin	\N	2024-07-04 10:28:15.03	POST	/scenarios/search	controllers.Scenarios.search()	develop	\N	200	cb55f29a-ca232880-462d8fca-d98d0f61-993c2dfb-38249226-8c6ff2de-311bbce6	2	ScenarioRead
172	tst-admin	\N	2024-07-04 10:28:14.948	POST	/user-matches-group	controllers.Matching.getMatchesByGroup()	develop	\N	200	4e45b5e4-d74e06f7-75c50ec4-13688958-5dd3c3ae-7b20d93d-3a78092e-9e12ea96	2	MatchAll
\.


--
-- Data for Name: play_evolutions; Type: TABLE DATA; Schema: public; Owner: genissqladmin
--

COPY public.play_evolutions (id, hash, applied_at, apply_script, revert_script, state, last_problem) FROM stdin;
1	e5ac963be60c0030097ca657199e17fbb72c6eb5	2024-07-04 00:00:00	CREATE SCHEMA "LOG_DB";\n\nCREATE TABLE "LOG_DB"."OPERATION_LOG_LOT"\n(\n"ID" bigserial,\n"KEY_ZERO" character varying(200) NOT NULL,\n"INIT_TIME" timestamp DEFAULT now() NOT NULL,\nCONSTRAINT "OPERATION_LOG_LOT_PK" PRIMARY KEY ("ID")\n);\n\nCREATE TABLE "LOG_DB"."OPERATION_LOG_RECORD"\n(\n"ID" bigserial,\n\n"USER_ID" character varying(50) NOT NULL,\n"OTP" character varying(50),\n"TIMESTAMP" timestamp NOT NULL,\n"METHOD" character varying(50) NOT NULL,\n"PATH" character varying(1024) NOT NULL,\n"ACTION" character varying(512) NOT NULL,\n"BUILD_NO" character varying(150) NOT NULL,\n"RESULT" character varying(150),\n"STATUS" integer NOT NULL,\n"SIGNATURE" character varying(8192) NOT NULL,\n"LOT" bigint NOT NULL,\n"DESCRIPTION" character varying(1024) NOT NULL,\nCONSTRAINT "OPERATION_LOG_RECORD_PK" PRIMARY KEY ("ID"),\nCONSTRAINT "OPERATION_LOG_RECORD_FK" FOREIGN KEY ("LOT")\nREFERENCES "LOG_DB"."OPERATION_LOG_LOT" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION\n);	DROP SCHEMA  "LOG_DB" CASCADE;	applied	
\.


--
-- Name: OPERATION_LOG_LOT_ID_seq; Type: SEQUENCE SET; Schema: LOG_DB; Owner: genissqladmin
--

SELECT pg_catalog.setval('"LOG_DB"."OPERATION_LOG_LOT_ID_seq"', 2, true);


--
-- Name: OPERATION_LOG_RECORD_ID_seq; Type: SEQUENCE SET; Schema: LOG_DB; Owner: genissqladmin
--

SELECT pg_catalog.setval('"LOG_DB"."OPERATION_LOG_RECORD_ID_seq"', 187, true);


--
-- Name: OPERATION_LOG_LOT OPERATION_LOG_LOT_PK; Type: CONSTRAINT; Schema: LOG_DB; Owner: genissqladmin
--

ALTER TABLE ONLY "LOG_DB"."OPERATION_LOG_LOT"
    ADD CONSTRAINT "OPERATION_LOG_LOT_PK" PRIMARY KEY ("ID");


--
-- Name: OPERATION_LOG_RECORD OPERATION_LOG_RECORD_PK; Type: CONSTRAINT; Schema: LOG_DB; Owner: genissqladmin
--

ALTER TABLE ONLY "LOG_DB"."OPERATION_LOG_RECORD"
    ADD CONSTRAINT "OPERATION_LOG_RECORD_PK" PRIMARY KEY ("ID");


--
-- Name: play_evolutions play_evolutions_pkey; Type: CONSTRAINT; Schema: public; Owner: genissqladmin
--

ALTER TABLE ONLY public.play_evolutions
    ADD CONSTRAINT play_evolutions_pkey PRIMARY KEY (id);


--
-- Name: OPERATION_LOG_RECORD OPERATION_LOG_RECORD_FK; Type: FK CONSTRAINT; Schema: LOG_DB; Owner: genissqladmin
--

ALTER TABLE ONLY "LOG_DB"."OPERATION_LOG_RECORD"
    ADD CONSTRAINT "OPERATION_LOG_RECORD_FK" FOREIGN KEY ("LOT") REFERENCES "LOG_DB"."OPERATION_LOG_LOT"("ID");


--
-- PostgreSQL database dump complete
--

