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
1	3dda970c-40f1251e-55acdc9b-0387b8d6-d42ee2b9-27213c3b-a3e56941-5ce6b89e	2025-11-10 19:50:19.923
2	8c761e5a-0809a3c5-e5bf02df-0585485b-7f3cac99-351c130f-4c191906-d7ebac29	2025-11-10 20:20:48.985
3	c94ceb09-1f9c57a3-84fa0332-d1331ce2-59e205a4-d8b44a78-696f9c38-b3b5b7bb	2025-11-10 21:01:55.173
4	4f51b91c-6d961dca-ddf41d14-7b2abe17-42cadf0a-1775b639-3d28e015-13f7299b	2025-11-28 15:51:04.022
5	23998b7d-43b74822-3ce9b03d-775627c4-ac740c3f-530471d5-481d0627-e6294cd6	2025-11-28 18:31:01.806
6	114ce896-694765d7-e44e2cfa-fabfdeff-c3f1613e-061a1c39-287fc8ec-7f384604	2025-11-28 18:39:53.772
7	93bf3d33-2df5ad94-71a37301-ce5d17a9-e58338c9-9032e25b-e7dc4947-159ef104	2025-12-18 17:33:59.79
8	1b4f564c-26376a75-bdace879-7cc65247-a94de22f-45a78438-bfc8f21d-5d9b096c	2025-12-18 17:39:05.426
9	3a5c3d46-f05032ae-216f943d-9f468a78-aa69add1-73fb3c18-e92a4a87-f379aa3d	2025-12-18 18:12:56.929
10	d546af9a-26445522-e7123d7d-42541985-831c4bc8-4db63842-d7de2f1b-00934b01	2025-12-18 18:17:09.972
11	d8ed6d3d-8af97cc7-2fbe3b31-03acf411-2906e502-508fde7f-7b872ce9-833103d9	2025-12-18 18:44:08.434
12	fc21fa5b-755e45e0-0bdb7970-a4a445d0-8b424a3b-e5c8c124-3b6bbf18-3f511831	2025-12-18 18:57:35.697
13	eb72058d-ad8f863b-ea2ca77c-7f8773a6-514240a6-e3887d52-f3f07a7b-c1e947bc	2025-12-19 18:30:24.108
14	69fe4796-c82e8048-d5019734-581bedb6-648d5c98-8f32da96-a299375a-f06b0be6	2026-02-02 19:06:10.54
\.


--
-- Data for Name: OPERATION_LOG_RECORD; Type: TABLE DATA; Schema: LOG_DB; Owner: genissqladmin
--

COPY "LOG_DB"."OPERATION_LOG_RECORD" ("ID", "USER_ID", "OTP", "TIMESTAMP", "METHOD", "PATH", "ACTION", "BUILD_NO", "RESULT", "STATUS", "SIGNATURE", "LOT", "DESCRIPTION") FROM stdin;
1	ANONYMOUS	\N	2025-11-10 19:56:00.538	POST	/login	controllers.Authentication.login()	develop	\N	200	39c5b62f-ac41db1a-1fdbb8ce-286e0b9d-9c6e0539-b311edc9-ddb253a6-85754ceb	1	Login
2	setup	\N	2025-11-10 19:56:00.774	GET	/match-notifications	controllers.Notifications.getMatchNotifications()	develop	\N	200	a0996fbf-94ce5cae-24c38407-ba0025ca-dfb4907a-b397076a-96d06ff7-2395ee0d	1	NotificationsAll
3	setup	\N	2025-11-10 19:56:00.774	GET	/notifications	controllers.Notifications.getNotifications()	develop	\N	200	019f2b9f-fa00a6a3-baf0cdbf-7b56377a-178ccb2a-3367b5b9-d02689c1-f2ed767b	1	NotificationsAll
4	setup	\N	2025-11-10 19:56:00.809	GET	/profiledata-desktop	controllers.ProfileData.getDesktopProfiles()	develop	\N	200	d6e37fe0-100a0f85-0f64d23b-88128f18-8441986e-4fa84762-2a6322a7-5f5599cb	1	ProfiledataRead
5	setup	\N	2025-11-10 19:56:00.841	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	f24dcfc2-a61e14b5-5d9a17e9-c31e0a17-e8a53536-35e947bd-cbfe5a7c-cbfcfdba	1	NotificationsAll
6	setup	\N	2025-11-10 19:56:00.841	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	b2e01722-8d4fa6ae-33adc15f-aa3cf298-3ee5a233-68717366-a22a14cc-dc9ba04f	1	NotificationsAll
7	setup	\N	2025-11-10 19:56:00.919	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	836bccea-7545c44a-3642e73b-b3230a8b-d9bfcf1c-ad1529b6-909e5187-5160a425	1	NotificationsAll
8	setup	\N	2025-11-10 19:58:26.678	GET	/analysistypes	controllers.AnalysisTypes.list()	develop	\N	200	a80e0db8-924f1c28-e826e6bf-3d971259-82340726-415aa3c7-0dccfe77-fc43cfe7	1	AnalysisTypeRead
9	setup	\N	2025-11-10 19:58:26.709	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	97bdb455-bc4cd220-bffa4525-bcbc5241-fe808cfc-f7637d02-7f06c124-5ca1c78e	1	CategoryTreeRead
10	setup	\N	2025-11-10 19:58:26.802	GET	/categories	controllers.Categories.list()	develop	\N	200	47f763e7-f28c7849-0f691da3-35d3c1bd-64368d71-e219c44c-b0055e3a-62fe124c	1	CategoryTreeRead
11	setup	\N	2025-11-10 19:58:40.61	GET	/country	controllers.Laboratories.listCountries()	develop	\N	200	9b376607-c006a2c1-8427782a-8f77baa9-de6d9bfb-5ea8f645-2aa9c3cf-f915b8dd	1	CountryRead
12	setup	\N	2025-11-10 19:58:40.611	GET	/laboratory/descriptive	controllers.Laboratories.listDescriptive()	develop	\N	200	09e4719c-a4b0246e-53223636-52fdc68a-4f80cbd4-7b69fba8-77d112f2-31530706	1	LaboratoryRead
13	setup	\N	2025-11-10 19:59:20.193	GET	/provinces/AR	controllers.Laboratories.listProvinces()	develop	\N	200	aaf26ac4-9a4f645f-1ec0f5d4-4c5a08dc-84be4929-f9517c80-0fb2ebb7-19d218ac	1	ProvincesRead
14	setup	146743	2025-11-10 19:59:37.618	POST	/laboratory	controllers.Laboratories.addLab()	develop	SHDG	200	f11a70f9-741808a4-6c22ccd0-2b41bf98-580a2af5-a86dd943-cb141960-bcf124b9	1	LaboratoryCreate
15	setup	\N	2025-11-10 19:59:37.632	GET	/laboratory/descriptive	controllers.Laboratories.listDescriptive()	develop	\N	200	68b2d418-78a21858-7f3cca33-3d3165a5-07471c2e-542fd309-cdbafccc-43086982	1	LaboratoryRead
16	setup	\N	2025-11-10 19:59:58.747	GET	/country	controllers.Laboratories.listCountries()	develop	\N	200	f8eb6763-7e75ea28-813e7ba8-b9e7c9f1-f3577993-112f9f29-d9cb126c-421185b2	1	CountryRead
17	setup	\N	2025-11-10 19:59:58.763	GET	/laboratory/SHDG	controllers.Laboratories.getLaboratory()	develop	\N	200	113c5c06-b4bd24b8-3ffeec77-a00e8762-854f66bc-37e17290-d73c13a1-4bb839d3	1	LaboratoryRead
18	setup	\N	2025-11-10 19:59:58.816	GET	/provinces/AR	controllers.Laboratories.listProvinces()	develop	\N	200	030e5538-b4494bf4-f93c99ba-07abdf84-886ba5e1-e81daadd-47704cfe-5e2c07d1	1	ProvincesRead
19	setup	\N	2025-11-10 20:00:32.404	GET	/roles	controllers.Roles.getRoles()	develop	\N	200	248ec9f0-c5725b45-dfa8b103-28e04529-3e54e98f-0899c17b-a3df30ef-8e720e94	1	RolesRead
20	setup	\N	2025-11-10 20:00:32.42	GET	/users	controllers.Users.listUsers()	develop	\N	200	e2c36a1f-ee2b54cb-5996b059-62385e16-c729e290-ab55a279-e2d17201-921e5633	1	UserRead
21	setup	\N	2025-11-10 20:00:37.034	GET	/roles	controllers.Roles.getRoles()	develop	\N	200	005c60a6-d123f665-c0bd6131-1308f615-e054dc7f-866e38b4-b8aa63a5-8cc5e097	1	RolesRead
22	setup	\N	2025-11-10 20:00:43.265	PUT	/users	controllers.Users.updateUser()	develop	\N	200	acaf1bfe-29f8c24f-f8b83518-2ad43cb5-300e91c6-c4ec4759-f88de634-3033fe2a	1	UserUpdate
23	setup	\N	2025-11-10 20:01:06.346	POST	/login	controllers.Authentication.login()	develop	\N	200	44baf4d2-439ad620-3239281c-3c3abea0-f40ec573-7eccaa77-a36f7eca-aecca452	1	Login
24	setup	\N	2025-11-10 20:01:06.451	GET	/match-notifications	controllers.Notifications.getMatchNotifications()	develop	\N	200	b31efdca-efd0e4fd-9e71b5ba-76fb1d9a-7f47d082-1ea2900e-647850d0-01a49d5c	1	NotificationsAll
25	setup	\N	2025-11-10 20:01:06.451	GET	/notifications	controllers.Notifications.getNotifications()	develop	\N	200	3ea7a272-aa6f01dc-11c9f156-9c543a49-2dcf7aa5-fa910048-6698a6bc-3b841737	1	NotificationsAll
26	setup	\N	2025-11-10 20:01:06.454	GET	/profiledata-desktop	controllers.ProfileData.getDesktopProfiles()	develop	\N	200	3919ca88-b28b34a7-c03ea10a-6505ed72-fd8c5eb6-12a9846d-9da22326-eba5cb1b	1	ProfiledataRead
27	setup	\N	2025-11-10 20:01:06.464	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	c2bbf0ce-49f49268-fcfda6f0-01806212-c24e0eed-2076ea16-f136c3cb-3664ef10	1	NotificationsAll
28	setup	\N	2025-11-10 20:01:06.489	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	3d2b2d70-e4a262b8-41851bfb-959bdfa3-d62ee4bd-ab4a4f48-22bbeb19-e6b10ccd	1	NotificationsAll
29	setup	\N	2025-11-10 20:01:06.547	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	fcb4c9a1-60b820d0-aa554ba2-e0014ec5-6739a623-623d377b-26bc8cad-1dfe4800	1	NotificationsAll
30	setup	\N	2025-11-10 20:01:12.075	GET	/roles	controllers.Roles.getRoles()	develop	\N	200	09bb684b-5bb828c9-c36be2fe-24da3994-7215ae92-976be45d-02d0e31b-6d75e2b0	1	RolesRead
31	setup	\N	2025-11-10 20:01:12.093	GET	/permissionsfull	controllers.Roles.listFullPermissions()	develop	\N	200	dc852716-19b67fae-1dee2a3a-d23d425d-ec195eda-630c7beb-a258942f-cadb9274	1	PermissionAll
32	setup	\N	2025-11-10 20:01:27.265	PUT	/roles	controllers.Roles.updateRole()	develop	\N	200	a9f24daf-fa873955-07eee09d-4f4c3cb8-1d5b3f68-ebd7c6c1-cd96e980-53013fc2	1	RolesUpdate
33	setup	\N	2025-11-10 20:01:27.281	GET	/roles	controllers.Roles.getRoles()	develop	\N	200	0787f6fc-9fcd6ff3-1b240722-de62ee65-6818f035-55e4d1a9-fdd4a1ed-0ccf8509	1	RolesRead
34	setup	\N	2025-11-10 20:01:34.428	GET	/roles	controllers.Roles.getRoles()	develop	\N	200	20a33db8-d9898b1b-7fce4aa6-77c55a3d-dbe209ec-369aa5de-894db98c-2dabdf20	1	RolesRead
35	setup	\N	2025-11-10 20:01:34.435	GET	/permissionsfull	controllers.Roles.listFullPermissions()	develop	\N	200	00a0e716-c48dc55a-0c755092-dd3c8466-b9fc1134-bb02efc6-a6b253ee-efd16817	1	PermissionAll
36	setup	\N	2025-11-10 20:01:38.616	GET	/categories	controllers.Categories.list()	develop	\N	200	667f0341-c8ae9ee9-8ca2b313-ffb39ccb-f260f4ed-ca950b83-28d1296a-4e808ebf	1	CategoryTreeRead
41	setup	\N	2025-11-10 20:01:54.71	GET	/match-notifications	controllers.Notifications.getMatchNotifications()	develop	\N	200	3a4e856d-c12d87ec-5c7dd8e8-05bb2a28-d1ffcde8-1d699117-1a88b6be-fc0c8a24	1	NotificationsAll
37	setup	\N	2025-11-10 20:01:38.645	GET	/categoriesWithProfiles	controllers.Categories.listWithProfiles()	develop	\N	200	66272fd6-c043185d-405def4e-5412e96f-31214df2-caaaf26f-a3f0a010-475fd85c	1	CategoryTreeRead
42	setup	\N	2025-11-10 20:01:54.717	GET	/profiledata-desktop	controllers.ProfileData.getDesktopProfiles()	develop	\N	200	3389066e-fb1cb203-854cf0f3-ab6db18a-8458c799-bcbc9dba-3535d3d7-0798e39f	1	ProfiledataRead
38	setup	\N	2025-11-10 20:01:38.703	POST	/search/profileData/searchTotal	controllers.SearchProfileDatas.searchTotal()	develop	\N	200	03cdabf9-32adaa80-b475e947-ca060125-521548e9-9ac67a3c-5211cbf0-6ee6a04f	1	ProfiledataSearch
43	setup	\N	2025-11-10 20:01:54.722	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	f9ddecde-535eefa3-95660d36-a1f633fa-daec69a1-7b72f768-8afdbfeb-6cc8faf0	1	NotificationsAll
39	setup	\N	2025-11-10 20:01:54.683	POST	/login	controllers.Authentication.login()	develop	\N	200	e7bb19c8-239cfbc1-eeb3fc77-8631f830-11dbcc90-a1e77ef8-8bf98769-6a82c59f	1	Login
44	setup	\N	2025-11-10 20:01:54.729	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	53ed2c2a-adf1ca70-0fd64585-f983f9c5-c7eea782-3a2f0f7f-6a5c09e7-fca31df8	1	NotificationsAll
40	setup	\N	2025-11-10 20:01:54.704	GET	/notifications	controllers.Notifications.getNotifications()	develop	\N	200	87f38c3d-2fd37e07-4f2282d5-428568f2-7bc546f3-a774ef3e-84ad1a5b-b1a3b143	1	NotificationsAll
45	setup	\N	2025-11-10 20:01:54.747	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	e4e84d85-83626f36-6301ec3d-34e57702-c75ca975-7e825661-3d8f84ba-3485a5d7	1	NotificationsAll
46	setup	\N	2025-11-10 20:02:04.615	POST	/search/profileData/searchTotal	controllers.SearchProfileDatas.searchTotal()	develop	\N	200	43b0fa8d-4a22018b-2c7527df-9550b1d4-6975cf89-747a844b-1c46dfd3-239f33e9	1	ProfiledataSearch
47	setup	\N	2025-11-10 20:02:04.617	GET	/categoriesWithProfiles	controllers.Categories.listWithProfiles()	develop	\N	200	602b6b27-f32fa684-92866fd5-9fcece1b-a7dee350-861b0e0c-354051e9-0b1194d2	1	CategoryTreeRead
48	setup	\N	2025-11-10 20:02:04.62	GET	/categories	controllers.Categories.list()	develop	\N	200	9080c459-c367ad17-9ed48e6c-4a375eea-05efc2f2-e2b5b50a-998ecb24-90197339	1	CategoryTreeRead
49	setup	\N	2025-11-10 20:21:09.902	POST	/login	controllers.Authentication.login()	develop	\N	200	d0209131-f9a88c77-dffd7f77-3812592e-b8c80c8c-3e007089-a608dc07-4f1884c3	2	Login
50	setup	\N	2025-11-10 20:21:10.017	GET	/match-notifications	controllers.Notifications.getMatchNotifications()	develop	\N	200	f93903d8-e16d22ba-59a07452-4273f35c-a2f5dcf7-3e04b461-097096c2-8ad06d87	2	NotificationsAll
51	setup	\N	2025-11-10 20:21:10.017	GET	/notifications	controllers.Notifications.getNotifications()	develop	\N	200	4e521e3e-ab63633b-d06051d8-b40d6678-894e16f9-4f064a13-94c7db2e-d95167a4	2	NotificationsAll
52	setup	\N	2025-11-10 20:21:10.045	GET	/profiledata-desktop	controllers.ProfileData.getDesktopProfiles()	develop	\N	200	1b2c890c-d79bfeaa-8e200cb2-9fc3673a-c3d2d83d-508161ac-b05c9964-45387bec	2	ProfiledataRead
53	setup	\N	2025-11-10 20:21:10.084	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	984f9b6e-9572df62-c56b1f7a-fed97b8e-c849e1cd-db0c488b-56e39598-717693d4	2	NotificationsAll
54	setup	\N	2025-11-10 20:21:10.084	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	9c0ec175-f0c3fc3c-c7513a3a-7654333f-e06f6012-933ab0f0-b315199c-987e03a3	2	NotificationsAll
55	setup	\N	2025-11-10 20:21:10.17	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	65c7b669-67dae0e8-ad9171a8-1790ff10-e215e098-9a5ec125-c8712bf7-0fd548f6	2	NotificationsAll
56	ANONYMOUS	\N	2025-11-28 18:31:29.771	POST	/login	controllers.Authentication.login()	develop	\N	200	f2b5ca6c-2647b2bf-4acbcd21-9dc92f66-8ae4e2aa-13ca1511-a79e211f-0d40fd03	5	Login
57	tst-admin	\N	2025-11-28 18:31:30.02	GET	/match-notifications	controllers.Notifications.getMatchNotifications()	develop	\N	200	4e0a9c73-faf6137f-7ff064ea-60068bfc-92ddc66d-e060c05c-68c32137-a0cf072b	5	NotificationsAll
58	tst-admin	\N	2025-11-28 18:31:30.02	GET	/notifications	controllers.Notifications.getNotifications()	develop	\N	200	d4d30157-6f733445-1e396818-29904f58-c4147004-fa79ae8d-e8bd9194-ca0741b5	5	NotificationsAll
59	tst-admin	\N	2025-11-28 18:31:30.037	GET	/profiledata-desktop	controllers.ProfileData.getDesktopProfiles()	develop	\N	200	23538608-b2e58cc2-6a69b553-2e274fa4-39365251-cc3ec0e5-f865812b-d9ca8f84	5	ProfiledataExport
60	tst-admin	\N	2025-11-28 18:31:30.087	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	ab4c4bd4-56eaad10-8e00cd85-1d84f550-86390424-bb8539fe-da4f9527-5e8f4a32	5	NotificationsAll
61	tst-admin	\N	2025-11-28 18:31:30.087	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	0f0241df-e556eed5-a857ff0b-cb2023eb-db164ed6-f6c45421-60d324c6-82a9ecce	5	NotificationsAll
62	tst-admin	\N	2025-11-28 18:31:30.165	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	2ca18602-e3467589-26041809-22da6ad7-88848922-3f2daf4c-94ad00bc-2dd8e1d6	5	NotificationsAll
63	tst-admin	\N	2025-11-28 18:31:39.893	GET	/bulkupload/step1/batches	controllers.BulkUpload.getBatchesStep1()	develop	\N	200	94807b3a-f8b3e10a-2e9aaab9-6b5aa39a-2696578c-baeddb92-b8e235b7-6f30b4f4	5	BulkuploaderBatches
64	tst-admin	\N	2025-11-28 18:31:39.917	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	edf86ac5-ece7c41c-cd0b5c37-5f04d3e7-995503a7-78a48eb7-fd6ceafa-cba20a56	5	CategoryTreeRead
65	tst-admin	\N	2025-11-28 18:31:40.032	GET	/locus-full	controllers.Locis.listFull()	develop	\N	200	92ede090-2cd81e9a-43adc982-95145ed7-f3019834-80848b46-818a7bf7-9455bfe8	5	LocusRead
66	tst-admin	\N	2025-11-28 18:32:43.239	POST	/bulkuploader	controllers.BulkUpload.uploadProtoProfiles()	develop	1	200	7b3bba00-a4dcf866-64ad21ff-016e2a16-90eaa500-62a207ae-7b261b50-119b5abf	5	BulkuploaderCreate
67	tst-admin	\N	2025-11-28 18:32:43.26	GET	/bulkupload/step1/batches	controllers.BulkUpload.getBatchesStep1()	develop	\N	200	190c9210-26ff6bdc-f9d944a1-d21e2c4c-29b68351-b20f3f03-e67bf687-f297e105	5	BulkuploaderBatches
68	tst-admin	\N	2025-11-28 18:32:43.513	GET	/bulkupload/step1/protoprofiles	controllers.BulkUpload.getProtoProfilesStep1()	develop	\N	200	c498fc97-c97f6d4f-54caa661-d5a90751-ad8d81d3-3035c8d9-f4bc7ba9-8748208b	5	BulkuploaderBatches
69	tst-admin	\N	2025-11-28 18:33:27.408	POST	/protoprofiles/1/status	controllers.BulkUpload.updateProtoProfileStatus()	develop	\N	200	f9d46d17-d92eb55c-ab417cf1-4432aa22-b335feba-670d66d2-0f1a04b5-221528d9	5	ProtoprofileUpdateStatus
70	tst-admin	\N	2025-11-28 18:33:32.224	POST	/protoprofiles/2/status	controllers.BulkUpload.updateProtoProfileStatus()	develop	\N	200	d771cfa5-c774b488-64d12512-91a7bdf5-838f6111-3414546f-2c8509b3-0646d160	5	ProtoprofileUpdateStatus
71	tst-admin	\N	2025-11-28 18:33:32.668	POST	/protoprofiles/3/status	controllers.BulkUpload.updateProtoProfileStatus()	develop	\N	200	7ca36a62-8f3aa355-338a6b0c-dfd6e09e-1ce47ac0-e11415ee-049b58cc-0163be80	5	ProtoprofileUpdateStatus
72	tst-admin	\N	2025-11-28 18:33:33.367	POST	/protoprofiles/4/status	controllers.BulkUpload.updateProtoProfileStatus()	develop	\N	200	6626b638-d1892738-88e79f11-d2799fa9-f570d717-688c947b-3901ef27-9cf8b3fa	5	ProtoprofileUpdateStatus
73	tst-admin	\N	2025-11-28 18:33:41.914	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	b2acd435-1e5b861b-117c77a5-1d24379b-ff7528c7-34d006ed-d719db46-dd2cb716	5	CategoryTreeRead
74	tst-admin	\N	2025-11-28 18:33:41.921	GET	/locus-full	controllers.Locis.listFull()	develop	\N	200	eb10f09e-982f22d3-8bf3d528-41885666-2e7f9cdc-8915cff4-ae35fba0-cad1c287	5	LocusRead
75	tst-admin	\N	2025-11-28 18:33:41.931	GET	/bulkupload/step2/batches	controllers.BulkUpload.getBatchesStep2()	develop	\N	200	02607e9f-54738802-16bdc226-1d36ac5d-a7747335-65ce2f16-c4633f6e-34909e2e	5	ProtoprofileRead
76	tst-admin	\N	2025-11-28 18:33:41.946	GET	/categories	controllers.Categories.list()	develop	\N	200	e0688fad-aa29d9e8-d079bcca-728fa673-e81f78cd-4e4e568c-b09d2992-67446d6d	5	CategoryTreeRead
77	tst-admin	\N	2025-11-28 18:33:48.142	GET	/bulkupload/step2/protoprofiles	controllers.BulkUpload.getProtoProfilesStep2()	develop	\N	200	cbdb1a9b-21e267a5-0c271351-037b3e4d-aaec642d-8e0f9f36-8e79bb51-d9549ec7	5	BulkuploaderBatches
78	tst-admin	\N	2025-11-28 18:33:48.167	GET	/profileData/isProfileReplicated/quimey-3	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	5f4c64ee-e3bb2d35-7f38f6b8-a37d887d-17de3873-0294223f-231ea683-0e1408cb	5	ProfileDataRead
79	tst-admin	\N	2025-11-28 18:33:48.167	GET	/profileData/isProfileReplicated/quimey-1	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	89a0ac96-c5846138-0957c637-3ca88eda-b73ddd33-300715d4-49e1c340-9c56787d	5	ProfileDataRead
80	tst-admin	\N	2025-11-28 18:33:48.167	GET	/profileData/isProfileReplicated/quimey-5	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	db5e87d0-aba511e1-5a819898-ca4eaa0d-355cb0b8-d3e24683-01d7ddec-92cbb4f3	5	ProfileDataRead
81	tst-admin	\N	2025-11-28 18:33:48.167	GET	/profileData/isProfileReplicated/quimey-6	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	73afb17e-696c6560-2e6e1453-a9b2334a-c573a767-6152cc4c-d1325b76-01021043	5	ProfileDataRead
82	tst-admin	\N	2025-11-28 18:34:06.428	POST	/protoprofiles/1/status	controllers.BulkUpload.updateProtoProfileStatus()	develop	\N	200	38ca4ab3-2132a1fd-7d41fd23-ab9754b3-b2f804eb-da07ea2c-8aee5a4e-299237a0	5	ProtoprofileUpdateStatus
83	tst-admin	\N	2025-11-28 18:35:18.962	POST	/protoprofiles/2/status	controllers.BulkUpload.updateProtoProfileStatus()	develop	\N	200	0f580403-adb1cb0d-bce3c9ab-a3c9c04a-961fd9f6-0a2a65a7-f7384382-ae9dfd4c	5	ProtoprofileUpdateStatus
84	tst-admin	\N	2025-11-28 18:40:18.969	POST	/login	controllers.Authentication.login()	develop	\N	200	c3775991-ed2b880d-124c1fbe-0462a98a-5deb0b7f-e20d626e-161ec52f-82df74b5	6	Login
85	tst-admin	\N	2025-11-28 18:40:19.074	GET	/match-notifications	controllers.Notifications.getMatchNotifications()	develop	\N	200	4b409b20-01fac7ac-55dcea18-b7029ac8-d4da17d7-d8fac709-bf529d89-b665dccf	6	NotificationsAll
86	tst-admin	\N	2025-11-28 18:40:19.075	GET	/notifications	controllers.Notifications.getNotifications()	develop	\N	200	5b669ed3-dc7a35ce-4c291f5d-4eb71d3e-03fb225a-248e696b-36a6aa28-3dac0b6a	6	NotificationsAll
87	tst-admin	\N	2025-11-28 18:40:19.079	GET	/profiledata-desktop	controllers.ProfileData.getDesktopProfiles()	develop	\N	200	0be34390-3c12ee70-10464e30-ea2293fc-1217bd77-bd4f6cf2-d95b6a8c-7e974207	6	ProfiledataRead
88	tst-admin	\N	2025-11-28 18:40:19.133	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	7f97ab7d-80666c27-39cc7aec-7453054a-5de28d6b-3ad0c20a-24714fdf-e90d7501	6	NotificationsAll
89	tst-admin	\N	2025-11-28 18:40:19.133	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	406f93f9-d413c3d4-09552c24-7da1663b-5920d216-e6cb7266-568ed158-2c88bb2d	6	NotificationsAll
90	tst-admin	\N	2025-11-28 18:40:19.236	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	b45af7c3-e63107e2-b15c1f6e-bc11f8a0-cfa90622-22eb56cb-62d28bc5-3158d3da	6	NotificationsAll
91	tst-admin	\N	2025-11-28 18:40:23.915	GET	/bulkupload/step2/batches	controllers.BulkUpload.getBatchesStep2()	develop	\N	200	ce67b5b7-6fb99a28-032989f2-3eec2ee7-d4766937-61695ceb-12fadf5e-7764d95b	6	ProtoprofileRead
92	tst-admin	\N	2025-11-28 18:40:23.928	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	c36218c2-6aa89924-6773c6ba-af0e132b-a82bab37-51858080-9ffb5b34-da69c810	6	CategoryTreeRead
93	tst-admin	\N	2025-11-28 18:40:24.034	GET	/locus-full	controllers.Locis.listFull()	develop	\N	200	a968fcfb-27de3fe3-362d2510-b57ad09f-d0109d87-e52c70a6-4595a10c-eae28315	6	LocusRead
94	tst-admin	\N	2025-11-28 18:40:24.041	GET	/categories	controllers.Categories.list()	develop	\N	200	708445f3-a6a01c80-3e3235c1-6ce5865c-013af177-d593dab9-386c0526-c9bb9662	6	CategoryTreeRead
95	tst-admin	\N	2025-11-28 18:40:25.234	GET	/bulkupload/step2/protoprofiles	controllers.BulkUpload.getProtoProfilesStep2()	develop	\N	200	004e7eeb-91c69506-23766456-8928aa9f-a453507e-806b6136-3947b0c5-efe929f6	6	BulkuploaderBatches
96	tst-admin	\N	2025-11-28 18:40:25.267	GET	/profileData/isProfileReplicated/quimey-1	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	0083bbe5-240d45d2-e839a086-1d5a9ddb-b8640a33-c013d7db-93f9ced4-8030c4f4	6	ProfileDataRead
97	tst-admin	\N	2025-11-28 18:40:25.267	GET	/profileData/isProfileReplicated/quimey-5	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	46fe1f45-1660feda-6de7b82b-90a34b97-e6932f77-2d0addb7-47b7dedf-2a138268	6	ProfileDataRead
98	tst-admin	\N	2025-11-28 18:40:25.267	GET	/profileData/isProfileReplicated/quimey-3	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	3a7c294a-f6bfce9d-4c5f2205-65e1cd21-1421c9a9-f911765a-963cbb24-6183df7f	6	ProfileDataRead
99	tst-admin	\N	2025-11-28 18:40:25.267	GET	/profileData/isProfileReplicated/quimey-6	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	815ffb52-3ddb7f51-25ab7090-1a220f21-06a0484c-9d382045-53c8d1f6-98ce7932	6	ProfileDataRead
100	tst-admin	\N	2025-11-28 18:40:27.366	POST	/protoprofiles/1/status	controllers.BulkUpload.updateProtoProfileStatus()	develop	\N	200	2239c878-31a97cb7-66b3dca0-293ea54f-9a78d821-61dbf461-f226d305-b2be8623	6	ProtoprofileUpdateStatus
101	tst-admin	\N	2025-11-28 18:40:27.399	GET	/profileData/isProfileReplicated/quimey-1	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	d3ec3eaf-6dcec88f-96097455-e91dbfc9-047f2f30-1095cfbe-638b719d-9ab6c10a	6	ProfileDataRead
102	tst-admin	\N	2025-11-28 18:41:12.6	GET	/country	controllers.Laboratories.listCountries()	develop	\N	200	aae4189c-3aec6861-c813bbcb-ddb5d635-0695e87a-e21f5530-bc124535-2966e459	6	CountryRead
103	tst-admin	\N	2025-11-28 18:41:12.602	GET	/laboratory/descriptive	controllers.Laboratories.listDescriptive()	develop	\N	200	055fef25-45e7fbc0-652d94d7-8041d887-ec0d49db-c27cf400-76f1628a-e0c8ba73	6	LaboratoryRead
104	tst-admin	\N	2025-11-28 18:41:29.651	GET	/populationBaseFreq	controllers.PopulationBaseFreq.getAllBaseNames()	develop	\N	200	bcf876e0-da70034d-9a99b3db-fd67d0ac-48926bb1-e2af4986-4c3709aa-1eb1b3c1	6	PopulationBaseFreqRead
105	tst-admin	\N	2025-11-28 18:41:37.855	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	dac019f6-556534e9-9eb3a418-660fbce0-5b756648-5de77999-5e404dd3-4884a0a8	6	CategoryTreeRead
106	tst-admin	\N	2025-11-28 18:41:37.866	GET	/bulkupload/step2/batches	controllers.BulkUpload.getBatchesStep2()	develop	\N	200	3081e3a0-fc32f1c3-d1f8fa56-9199c802-fe460c42-6cb7ea9d-8620e7fc-1d050252	6	ProtoprofileRead
107	tst-admin	\N	2025-11-28 18:41:37.867	GET	/locus-full	controllers.Locis.listFull()	develop	\N	200	f9512a7c-a8b1245a-b02f2e2f-bd9d75d4-f27cdffd-8fc894b2-1da52c5d-d33dc58d	6	LocusRead
108	tst-admin	\N	2025-11-28 18:41:37.882	GET	/categories	controllers.Categories.list()	develop	\N	200	847ea4ec-f3019ef8-d8539c57-dbdd0ac1-a07f8980-c5fd1c8f-2743446f-92b50d0c	6	CategoryTreeRead
109	tst-admin	\N	2025-11-28 18:41:39.743	GET	/bulkupload/step2/protoprofiles	controllers.BulkUpload.getProtoProfilesStep2()	develop	\N	200	6f6bf770-43dd0033-feb307ab-e101da34-7303b182-02f17eed-f2120f37-c326b9e2	6	BulkuploaderBatches
110	tst-admin	\N	2025-11-28 18:41:39.76	GET	/profileData/isProfileReplicated/quimey-6	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	033be359-ee3b5ce1-1d267516-1951012b-bc6ed8a4-597878b8-9fa3fa62-b3612b6b	6	ProfileDataRead
111	tst-admin	\N	2025-11-28 18:41:39.76	GET	/profileData/isProfileReplicated/quimey-5	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	b61b14cf-627cef2a-e1d47dd5-0620a0d5-15596d7c-54476bf2-c53b0566-32af68a6	6	ProfileDataRead
112	tst-admin	\N	2025-11-28 18:41:39.76	GET	/profileData/isProfileReplicated/quimey-3	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	a83b31ea-9d1d83d0-e6c6339f-9af06020-99c83c8e-d9222b96-e57a8546-80df35bf	6	ProfileDataRead
113	tst-admin	\N	2025-11-28 18:41:39.765	GET	/profileData/isProfileReplicated/quimey-1	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	7f9800ad-a3768970-e5a28e69-fb2005d0-5ad66d88-141a0809-4b8d92cd-f4946202	6	ProfileDataRead
114	tst-admin	\N	2025-11-28 18:41:45.144	GET	/populationBaseFreq	controllers.PopulationBaseFreq.getAllBaseNames()	develop	\N	200	53d319be-203df8f3-8395759b-fc7f7c03-9c439106-e06c1a98-69f6a958-2adf7ebc	6	PopulationBaseFreqRead
115	tst-admin	\N	2025-11-28 18:41:50.651	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	472feeaa-f9b450a4-c436ebdb-661aee12-a91f3c7c-cb6b29c5-211e9469-0c18735b	6	CategoryTreeRead
116	tst-admin	\N	2025-11-28 18:41:50.656	GET	/locus-full	controllers.Locis.listFull()	develop	\N	200	43e4acd0-a65be219-a0d9eb51-0ef24313-9ae3faee-913988ed-2a5fa66d-a0a79362	6	LocusRead
117	tst-admin	\N	2025-11-28 18:41:50.666	GET	/bulkupload/step1/batches	controllers.BulkUpload.getBatchesStep1()	develop	\N	200	4dae43b6-9001e5da-545a5838-28b6d2a4-1c2eed77-d04fea40-4bb1b7f9-227ea758	6	BulkuploaderBatches
118	tst-admin	\N	2025-11-28 18:42:10.804	POST	/bulkuploader	controllers.BulkUpload.uploadProtoProfiles()	develop	2	200	55933dba-cd93feb6-b88a5cf8-7941b198-dbfb58cb-f9b8282f-cf9e660d-94f8a6d7	6	BulkuploaderCreate
119	tst-admin	\N	2025-11-28 18:42:10.82	GET	/bulkupload/step1/batches	controllers.BulkUpload.getBatchesStep1()	develop	\N	200	8a6cccd7-1ac13d68-e66b8f59-7aa63436-8cd24922-7f695c18-2e721d1e-a366346f	6	BulkuploaderBatches
120	tst-admin	\N	2025-11-28 18:42:10.889	GET	/bulkupload/step1/protoprofiles	controllers.BulkUpload.getProtoProfilesStep1()	develop	\N	200	4f775e52-5a28e116-b2b9a6cd-85200630-0f8c2861-5039df53-8582a67b-680d6d44	6	BulkuploaderBatches
121	tst-admin	\N	2025-11-28 18:42:15.687	POST	/protoprofiles/572/status	controllers.BulkUpload.updateProtoProfileStatus()	develop	\N	200	688bfd00-d99f0be2-d16034dd-b56c1e1d-fe992e60-ebb0b759-f4eced1d-808564a0	6	ProtoprofileUpdateStatus
122	tst-admin	\N	2025-11-28 18:42:42.195	POST	/bulkuploader	controllers.BulkUpload.uploadProtoProfiles()	develop	3	200	7c205965-2d3fa90c-1a7f9e67-56c524ed-184e4f99-554e7a2b-82c605cd-3a10e64e	6	BulkuploaderCreate
123	tst-admin	\N	2025-11-28 18:42:42.208	GET	/bulkupload/step1/batches	controllers.BulkUpload.getBatchesStep1()	develop	\N	200	30089f8e-dc464adf-aa15dfa5-8c5bea42-5299f255-afee2268-565436d7-b6e0ff4b	6	BulkuploaderBatches
124	tst-admin	\N	2025-11-28 18:42:42.272	GET	/bulkupload/step1/protoprofiles	controllers.BulkUpload.getProtoProfilesStep1()	develop	\N	200	82be7a3b-bd7e8f55-c7244e6e-eb7cdafc-6db965fe-5cd7d89c-4ebf7d03-a5263862	6	BulkuploaderBatches
125	tst-admin	\N	2025-11-28 18:42:45.794	POST	/protoprofiles/573/status	controllers.BulkUpload.updateProtoProfileStatus()	develop	\N	200	f0167ecf-e6d7d5e5-c9c56684-8b8c72f4-590ae307-715b1a76-2438f6b0-41eee327	6	ProtoprofileUpdateStatus
126	tst-admin	\N	2025-11-28 18:42:46.334	POST	/protoprofiles/574/status	controllers.BulkUpload.updateProtoProfileStatus()	develop	\N	200	cd8851d2-8cc10cd3-b19dea6b-80dfc3a5-1b030bdd-6c8244f1-c20ecb99-9d9d5458	6	ProtoprofileUpdateStatus
127	tst-admin	\N	2025-11-28 18:42:53.584	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	1d90815b-573b4b39-8990c2ea-3feefa66-ee021bab-7a8517a0-fba2a127-a216c345	6	CategoryTreeRead
128	tst-admin	\N	2025-11-28 18:42:53.587	GET	/locus-full	controllers.Locis.listFull()	develop	\N	200	611739f7-702a2fee-e8c948e2-f3c174ef-20f75a82-e08fffbe-4fb784aa-794452cb	6	LocusRead
129	tst-admin	\N	2025-11-28 18:42:53.589	GET	/bulkupload/step2/batches	controllers.BulkUpload.getBatchesStep2()	develop	\N	200	019d4dce-d77d332e-9598f06f-9b7f6d6b-881cd449-6d0f401f-3dd265b4-c4bb38f5	6	ProtoprofileRead
130	tst-admin	\N	2025-11-28 18:42:53.596	GET	/categories	controllers.Categories.list()	develop	\N	200	7b6f2890-6c8a3ec8-12079dcd-83dacf7c-4a2042e1-483ddc5f-870a09a4-446fa2f4	6	CategoryTreeRead
131	tst-admin	\N	2025-11-28 18:42:57.724	GET	/bulkupload/step2/protoprofiles	controllers.BulkUpload.getProtoProfilesStep2()	develop	\N	200	201537fd-0e16bc26-fdf79f12-0ec7a7a8-6859a569-7e3df577-b16b4958-253bcadb	6	BulkuploaderBatches
132	tst-admin	\N	2025-11-28 18:42:57.737	GET	/profileData/isProfileReplicated/Evi02-C1	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	f176bc09-22e78435-7efbfe77-beae6d14-73d903ed-a63a9e29-3662b90e-b9d13735	6	ProfileDataRead
133	tst-admin	\N	2025-11-28 18:42:57.741	GET	/profileData/isProfileReplicated/Vict-C1	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	f3299212-fa2b7312-67eb72c2-a11536d2-24488e3c-6c371cb5-6dcbdf4c-64261767	6	ProfileDataRead
134	tst-admin	\N	2025-11-28 18:43:03.024	POST	/protoprofiles/573/status	controllers.BulkUpload.updateProtoProfileStatus()	develop	\N	200	a62f4a91-8046e639-b3522b08-70d8e0c7-8d5e12ee-d9159cb4-3e4cc4b3-10ca2c08	6	ProtoprofileUpdateStatus
135	tst-admin	\N	2025-11-28 18:43:03.046	GET	/profileData/isProfileReplicated/Evi02-C1	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	a91c0538-6db082c3-a3257af7-9176394b-c3d86333-02da725d-105814d3-f9af0758	6	ProfileDataRead
136	tst-admin	\N	2025-11-28 18:43:04.614	POST	/protoprofiles/574/status	controllers.BulkUpload.updateProtoProfileStatus()	develop	\N	200	08bb8d6b-a09602e8-7414bbc6-3bdc0b3d-c9841c07-c51a4ffa-6e114670-451f3ba6	6	ProtoprofileUpdateStatus
137	tst-admin	\N	2025-11-28 18:43:04.632	GET	/profileData/isProfileReplicated/Vict-C1	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	3941acdf-9ff0c9d1-8a61729d-c323ee21-7380fea3-544c86bb-014db517-8837b446	6	ProfileDataRead
138	tst-admin	\N	2025-11-28 18:43:09.1	GET	/bulkupload/step2/protoprofiles	controllers.BulkUpload.getProtoProfilesStep2()	develop	\N	200	7330163b-2f2b0b43-9c3a11b5-c56593f0-34e9c382-5da3187f-6ac1b6f5-dcd233d0	6	BulkuploaderBatches
139	tst-admin	\N	2025-11-28 18:43:09.111	GET	/profileData/isProfileReplicated/Sos-C1	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	74b6eee1-2ea0618e-5d1c517a-339de0fb-bed2c1ea-c82e7f71-95837c87-39c9f88b	6	ProfileDataRead
140	tst-admin	\N	2025-11-28 18:43:12.752	GET	/bulkupload/step2/protoprofiles	controllers.BulkUpload.getProtoProfilesStep2()	develop	\N	200	b69233a3-6a456068-518eb3db-78831fc4-55577cb9-0234f0df-9b511487-fb4ba962	6	BulkuploaderBatches
141	tst-admin	\N	2025-11-28 18:43:12.766	GET	/profileData/isProfileReplicated/quimey-6	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	fd653e0b-fc1994c6-c18ae80b-b46db682-4a1ff35c-56dff4a3-51477131-de2041ed	6	ProfileDataRead
142	tst-admin	\N	2025-11-28 18:43:12.766	GET	/profileData/isProfileReplicated/quimey-5	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	6308eb79-cf2d352b-5e858ecd-3d95243c-3b24d2ba-c3e0f439-bdad9aea-bbd56874	6	ProfileDataRead
143	tst-admin	\N	2025-11-28 18:43:12.766	GET	/profileData/isProfileReplicated/quimey-3	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	7f5a1704-fcf2cbb7-d62eb282-41a219a6-8248d449-8be68812-a5b95290-7edd98b2	6	ProfileDataRead
144	tst-admin	\N	2025-11-28 18:43:12.77	GET	/profileData/isProfileReplicated/quimey-1	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	3ff9bd9a-79cd741c-7d842f54-7a5c5a55-1405689c-0d41cfd4-e6b724c9-e41aa1f7	6	ProfileDataRead
145	tst-admin	\N	2025-11-28 18:43:13.361	GET	/bulkupload/step2/protoprofiles	controllers.BulkUpload.getProtoProfilesStep2()	develop	\N	200	d07b9d87-397f9ba6-d165b0e5-bcdeff52-5e81010f-e50aebb7-0bf36684-372b430c	6	BulkuploaderBatches
146	tst-admin	\N	2025-11-28 18:43:13.371	GET	/profileData/isProfileReplicated/Sos-C1	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	eaab6cc9-391ad041-25ac0bb2-8713cd21-792842f9-dac52f31-493b751c-0acc5448	6	ProfileDataRead
147	tst-admin	\N	2025-11-28 18:43:21.926	POST	/protoprofiles/572/status	controllers.BulkUpload.updateProtoProfileStatus()	develop	\N	200	1c9c04cc-ce8b4664-45f71d29-25c51296-7b3e7081-e2db0ecd-62d3d535-00e98fc0	6	ProtoprofileUpdateStatus
148	tst-admin	\N	2025-11-28 18:43:21.946	GET	/profileData/isProfileReplicated/Sos-C1	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	91f029ce-afdbc992-d66935a9-7367e176-5bd7e9f4-dde9685f-15af482a-761a5de7	6	ProfileDataRead
149	tst-admin	\N	2025-11-28 18:43:22.827	GET	/profiledata-complete/AR-C-SHDG-1105	controllers.ProfileData.getByCode()	develop	\N	200	72be5bc3-e246a57e-22188496-f4402773-2c704cb0-735dc05d-42a3a009-1f8dd2fc	6	ProfiledataRead
150	tst-admin	\N	2025-11-28 18:43:22.827	GET	/profiledata-complete/AR-C-SHDG-1105	controllers.ProfileData.getByCode()	develop	\N	200	a333d06f-93a33017-84ffbf48-c898beb8-17f29216-d439fe3f-565bbb61-56313358	6	ProfiledataRead
151	tst-admin	\N	2025-11-28 18:43:22.827	GET	/profiledata-complete/AR-C-SHDG-1105	controllers.ProfileData.getByCode()	develop	\N	200	9ed94624-469c46bc-dc8aee76-63eb3af5-8f0a3770-68682092-8a5a8e9d-a6b84130	6	ProfiledataRead
160	tst-admin	\N	2025-11-28 18:43:32.176	POST	/search/profileData/searchTotal	controllers.SearchProfileDatas.searchTotal()	develop	\N	200	4c3d5e4f-6b0d78ac-a78c4265-06cb49d2-03ffb5a5-924f3241-84d96ff5-d0dc1c44	6	ProfiledataSearch
165	tst-admin	\N	2025-11-28 18:43:32.43	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	da10fe23-6dae0f51-a4cbfde3-a6575aa9-a849a2db-a102d078-6d56a694-a86556e9	6	MatchAll
170	tst-admin	\N	2025-11-28 18:43:41.185	GET	/strkits	controllers.StrKits.list()	develop	\N	200	f1b93dd4-00dd0e33-edd7c650-a53a83ee-22051214-92a1dfbe-c668e0fd-bad8cf28	6	StrKitsAll
175	tst-admin	\N	2025-11-28 18:43:41.24	GET	/profiles/full/AR-C-SHDG-1103	controllers.Profiles.getFullProfile()	develop	\N	200	f26dbce8-dcc31301-e49b47ac-feb0fd45-7c1a01de-48992cd5-19160609-2724104e	6	ProfilesRead
193	tst-admin	\N	2025-11-28 18:44:00.262	GET	/profiledata-complete/AR-C-SHDG-1103	controllers.ProfileData.getByCode()	develop	\N	200	903419b1-5e4b76ce-6b92cfaf-6be560ce-3735f8f3-ff92d59e-9ed43094-59331ed0	6	ProfiledataRead
152	tst-admin	\N	2025-11-28 18:43:23.919	POST	/protoprofiles/2/status	controllers.BulkUpload.updateProtoProfileStatus()	develop	\N	200	57f225bd-38096457-474b04fb-5163bc6b-18a2d335-59defa16-83ab999e-4a97cd1b	6	ProtoprofileUpdateStatus
161	tst-admin	\N	2025-11-28 18:43:32.329	POST	/search/profileData/search	controllers.SearchProfileDatas.search()	develop	\N	200	ff6f0b1e-73c7bbb2-3fcb6603-8c013249-1cb577d8-7cdd38bc-87833e34-b9fd6f88	6	ProfiledataSearch
166	tst-admin	\N	2025-11-28 18:43:32.431	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	5ede9b3a-f2a958b0-ed699d21-bd5aaa5a-cbf20f27-6010c8a6-bafd37df-ae5232d5	6	MatchAll
171	tst-admin	\N	2025-11-28 18:43:41.202	GET	/analysistypes	controllers.AnalysisTypes.list()	develop	\N	200	396356a8-b9af5a78-db3e3158-e385cc9e-81f9f4d0-a8e3c25b-6ed8de1c-58080744	6	AnalysisTypeRead
176	tst-admin	\N	2025-11-28 18:43:41.245	GET	/getFilesId	controllers.Resources.getFilesId()	develop	\N	200	6edd34db-65b1dd98-94c7db76-b762ff6f-dfac3df2-534fc75b-dcfb9b8c-27b9f3cc	6	UploadImageAll
194	tst-admin	\N	2025-11-28 18:44:00.262	GET	/profiledata-complete/AR-C-SHDG-1103	controllers.ProfileData.getByCode()	develop	\N	200	0d8a9dee-335fded7-97d0a236-8ab14d55-6170d7b5-902f00bc-107f7279-f46288e9	6	ProfiledataRead
153	tst-admin	\N	2025-11-28 18:43:23.95	GET	/profileData/isProfileReplicated/quimey-3	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	3a749bcb-8955cc36-e2aad1f5-d318e9ad-b387dd11-312a8d80-028aaccc-5aae95e9	6	ProfileDataRead
162	tst-admin	\N	2025-11-28 18:43:32.417	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	190bb4d4-372c9f17-7921e5bf-58e36fbb-c49a5fa9-c1116fd9-d2e2fdfa-5ba972c7	6	MatchAll
167	tst-admin	\N	2025-11-28 18:43:32.488	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	81cfe8ec-cbdc4313-ed8c65a7-821e90ec-e36d2506-c7601cde-c45aa3e2-0be07cab	6	MatchAll
172	tst-admin	\N	2025-11-28 18:43:41.219	GET	/profiles-labelsets	controllers.Profiles.getLabelsSets()	develop	\N	200	a1e854e0-3c4bf11e-7351037e-6b1292ab-edc314cb-92c9380a-2abead57-aa104453	6	ProfilesRead
177	tst-admin	\N	2025-11-28 18:43:41.29	GET	/strkits/Powerplex21/loci	controllers.StrKits.findLociByKit()	develop	\N	200	dceb6a35-31ebc684-e4bca87d-d0dfda07-a5b22e2e-f3428bff-4ac9131a-f7777489	6	StrKitsAll
195	tst-admin	\N	2025-11-28 18:44:00.262	GET	/profiledata-complete/AR-C-SHDG-1103	controllers.ProfileData.getByCode()	develop	\N	200	c1560fd0-af3c84f1-81b48782-2db8ddf9-bcf5ea89-75e2f41b-40a1b6e0-b14a4b97	6	ProfiledataRead
154	tst-admin	\N	2025-11-28 18:43:26.974	POST	/protoprofiles/3/status	controllers.BulkUpload.updateProtoProfileStatus()	develop	\N	200	f4d564e6-d766f31e-5e545908-63e5dd38-5aca1a2e-5af6429a-96caab42-577f6552	6	ProtoprofileUpdateStatus
180	tst-admin	\N	2025-11-28 18:43:59.605	POST	/profiles-labels	controllers.Profiles.saveLabels()	develop	\N	200	c96e5dd0-4ffe407b-acddd925-99edb234-9d633fbb-db965efe-66017524-9c58b7d2	6	ProfilesCreate
185	tst-admin	\N	2025-11-28 18:43:59.651	GET	/profiledataWithAssociations	controllers.ProfileData.findByCodeWithAssociations()	develop	\N	200	16dcece0-a1be6b00-58565456-3755b91a-55b0781d-13dcce86-d97b705a-49ab41d1	6	ProfiledataRead
190	tst-admin	\N	2025-11-28 18:43:59.873	GET	/profiledataWithAssociations	controllers.ProfileData.findByCodeWithAssociations()	develop	\N	200	439ea446-d44e05be-a0d817ff-2e16443c-0dc583e8-14aa4a70-c87e8cc7-32efe52c	6	ProfiledataRead
199	tst-admin	\N	2025-11-28 18:44:10.665	POST	/user-total-matches	controllers.Matching.getTotalMatches()	develop	\N	200	751f9579-9a87c7ed-0d63ccaf-79d62183-3b1d2d00-efb5189b-7d9794cb-8e3be1cf	6	MatchAll
204	tst-admin	\N	2025-11-28 18:44:10.925	GET	/profiles-labelsets	controllers.Profiles.getLabelsSets()	develop	\N	200	c104a392-a3b7621d-85cda35c-5424ed32-6bdc8bfc-8627e5f5-3756e677-e675f5ca	6	ProfilesRead
155	tst-admin	\N	2025-11-28 18:43:27.017	GET	/profileData/isProfileReplicated/quimey-5	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	293067bf-5b8c2de0-c0da3917-3879551a-2d8ec858-61f807ec-27ca2ab5-b452e3df	6	ProfileDataRead
181	tst-admin	\N	2025-11-28 18:43:59.641	GET	/strkits	controllers.StrKits.list()	develop	\N	200	38f7a63d-339ddf6c-0bb53107-76bfe950-8df18942-abed8fce-1301eab1-6679d431	6	StrKitsAll
186	tst-admin	\N	2025-11-28 18:43:59.665	GET	/profiles/full/AR-C-SHDG-1103	controllers.Profiles.getFullProfile()	develop	\N	200	d687b81c-39ddc30d-0bd62985-dfdd847b-2538f9f9-1b053876-3f8ace3a-8172eaf4	6	ProfilesRead
191	tst-admin	\N	2025-11-28 18:43:59.873	GET	/strkits/Powerplex21/loci	controllers.StrKits.findLociByKit()	develop	\N	200	b47caa90-3351a2f3-1c3b6ac9-9740e96c-89d13983-07fca189-9ad6b2e4-f7700f4f	6	StrKitsAll
200	tst-admin	\N	2025-11-28 18:44:10.779	POST	/user-matches	controllers.Matching.getMatches()	develop	\N	200	246e43a7-24a32a92-007d51bd-73987466-2736af88-961eba1c-6b19d103-70654b87	6	MatchAll
205	tst-admin	\N	2025-11-28 18:44:10.929	GET	/profiledataWithAssociations	controllers.ProfileData.findByCodeWithAssociations()	develop	\N	200	445dec14-b0fe2bd4-2f6ef158-1b08c7e3-a5a78f44-4c6f7f1d-888940ee-d22ad106	6	ProfiledataRead
156	tst-admin	\N	2025-11-28 18:43:27.256	POST	/protoprofiles/4/status	controllers.BulkUpload.updateProtoProfileStatus()	develop	\N	200	25a3b7b2-c131e3fc-7a8cbf8d-d1ff1ad9-699b9040-b8ee4f32-563d2d74-d34e4e35	6	ProtoprofileUpdateStatus
182	tst-admin	\N	2025-11-28 18:43:59.642	GET	/locus	controllers.Locis.list()	develop	\N	200	082f9463-7d83b315-b8c2862d-b201a606-83ac2e0c-19387ab3-337fa9e4-e703bbf6	6	LocusRead
187	tst-admin	\N	2025-11-28 18:43:59.672	GET	/getFilesId	controllers.Resources.getFilesId()	develop	\N	200	b897e327-a732ed2e-b154b7a5-328782af-927d302e-0401ccc3-7ab5d800-97bc5f0e	6	UploadImageAll
196	tst-admin	\N	2025-11-28 18:44:10.629	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	419e111f-8b04fb71-413c5246-0a11ae3e-dcf76ecc-93cb3533-f51aa729-0504ba65	6	CategoryTreeRead
201	tst-admin	\N	2025-11-28 18:44:10.89	GET	/profiledataWithAssociations	controllers.ProfileData.findByCodeWithAssociations()	develop	\N	200	6b074b68-f026dcc9-7b6d0eca-8468870a-21c5ef12-762fcba2-a04f3afc-a2f7ad0e	6	ProfiledataRead
206	tst-admin	\N	2025-11-28 18:44:10.944	GET	/profiles/full/AR-C-SHDG-1103	controllers.Profiles.getFullProfile()	develop	\N	200	53a4f7cb-fffed8d4-226d52a9-dec84534-449021dd-94a977b9-67a6b998-1941a734	6	ProfilesRead
157	tst-admin	\N	2025-11-28 18:43:27.275	GET	/profileData/isProfileReplicated/quimey-6	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	4c2385d3-0fbf0b8a-67f440f6-f7bd7b18-fd630bc4-ed842ab6-c78cc3da-269c779b	6	ProfileDataRead
183	tst-admin	\N	2025-11-28 18:43:59.642	GET	/analysistypes	controllers.AnalysisTypes.list()	develop	\N	200	3f368453-d5c216b7-5fa07eea-e777d674-393f6582-eb4e735d-95d300a8-4d55c716	6	AnalysisTypeRead
188	tst-admin	\N	2025-11-28 18:43:59.718	GET	/getFilesId	controllers.Resources.getFilesId()	develop	\N	200	56456deb-a18c8a6d-8eb1e175-ffc33326-efb5716d-54be0a59-2d57df16-bc40ac91	6	UploadImageAll
197	tst-admin	\N	2025-11-28 18:44:10.63	GET	/locus	controllers.Locis.list()	develop	\N	200	cd7afaca-e21742a1-f244b1f9-cbfe0e2c-b5d39a69-9c4fb1c7-26e112df-f2fad440	6	LocusRead
202	tst-admin	\N	2025-11-28 18:44:10.911	GET	/profiles/full/AR-C-SHDG-1105	controllers.Profiles.getFullProfile()	develop	\N	200	b10e1829-a3ae3d18-09481521-d22e1f59-eba6677f-4291bf3c-b6eb07c9-b018a318	6	ProfilesRead
158	tst-admin	\N	2025-11-28 18:43:32.12	GET	/categories	controllers.Categories.list()	develop	\N	200	29b459b4-38c6bd8d-7d427c25-f472c39a-9f037368-b89cd22d-78a2328a-76fe2222	6	CategoryTreeRead
163	tst-admin	\N	2025-11-28 18:43:32.417	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	bec38ac5-7f96a30d-f6a1758b-8249bb9c-aa28fd75-6191d20a-2d45986c-3c17d592	6	MatchAll
168	tst-admin	\N	2025-11-28 18:43:32.488	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	2de9eeea-98b85a5d-e15c4132-4e93f75c-42f71d84-020a229d-b296774d-47ea4d62	6	MatchAll
173	tst-admin	\N	2025-11-28 18:43:41.222	GET	/profiledataWithAssociations	controllers.ProfileData.findByCodeWithAssociations()	develop	\N	200	e4e65c9d-6f36cc90-a70ae059-485562f1-42a43339-a1d3bd7d-969cafea-1bac6c8f	6	ProfiledataRead
178	tst-admin	\N	2025-11-28 18:43:53.514	GET	/search/profileData/EvidenciaMezcla	controllers.SearchProfileDatas.searchProfilesAssociable()	develop	\N	200	cdaa5bbe-9bfe913e-271939b6-5428f3bb-4e463135-e9eb2af0-cbe4842f-ac9185a0	6	ProfiledataSearch
159	tst-admin	\N	2025-11-28 18:43:32.135	GET	/categoriesWithProfiles	controllers.Categories.listWithProfiles()	develop	\N	200	9d1f750c-59656022-1358bd3c-e6d7a5dc-9f97a75d-0cad799e-211342bf-9e86cc0d	6	CategoryTreeRead
164	tst-admin	\N	2025-11-28 18:43:32.417	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	992b972a-2936d1ce-10946343-6a0c8bff-6efb6443-839e980f-57d58a1a-10816f05	6	MatchAll
169	tst-admin	\N	2025-11-28 18:43:41.168	GET	/locus	controllers.Locis.list()	develop	\N	200	b4bff80a-4694d924-ef94bf28-b259f0ba-fc545e71-d2a6240c-6c7ce712-18a54911	6	LocusRead
174	tst-admin	\N	2025-11-28 18:43:41.236	GET	/getFilesId	controllers.Resources.getFilesId()	develop	\N	200	4d50b027-113ac4eb-d583bc2b-7f5d15cf-a534c86d-a9764b01-866a4612-6a7d05da	6	UploadImageAll
192	tst-admin	\N	2025-11-28 18:43:59.89	GET	/profiles/full/AR-C-SHDG-1104	controllers.Profiles.getFullProfile()	develop	\N	200	05473cc2-2d36d76c-926d3013-94b38358-5a31147e-d68923b8-27fc64cb-a65ec994	6	ProfilesRead
179	tst-admin	\N	2025-11-28 18:43:57.578	POST	/profiles-mixture-verification	controllers.Profiles.verifyMixtureAssociation()	develop	\N	200	eddb46aa-ce18564c-f88d6763-5535fdb3-8c0d13a5-5c36652a-2f34b418-77966565	6	ProfilesCreate
184	tst-admin	\N	2025-11-28 18:43:59.651	GET	/profiles-labelsets	controllers.Profiles.getLabelsSets()	develop	\N	200	b4727781-a140a5c8-83284abf-be004ab7-872b0548-fd3917c2-84ae6ccf-4fc20940	6	ProfilesRead
189	tst-admin	\N	2025-11-28 18:43:59.871	GET	/profiles-labelsets	controllers.Profiles.getLabelsSets()	develop	\N	200	eefde96b-f69079ee-af417f7a-39c4ab30-18522c50-dc42717f-765ab46a-5194f465	6	ProfilesRead
198	tst-admin	\N	2025-11-28 18:44:10.641	GET	/laboratory	controllers.Laboratories.list()	develop	\N	200	a17ca44c-bef91345-04c4889f-b86de4d6-42f501c2-64f99ade-3769077a-462dbc27	6	LaboratoryRead
203	tst-admin	\N	2025-11-28 18:44:10.916	GET	/profiles-labelsets	controllers.Profiles.getLabelsSets()	develop	\N	200	19cb2bcf-33b28f60-02d793dd-6292e39a-ea4e9efd-8a321369-ee5972da-e324868c	6	ProfilesRead
207	ANONYMOUS	\N	2025-12-18 17:35:57.564	POST	/login	controllers.Authentication.login()	develop	\N	200	5da1b521-254a96b2-edfc6ee6-a6af4388-42c4b503-27172c6b-eea5fc6a-0de8d1e7	7	Login
208	tst-admin	\N	2025-12-18 17:35:57.696	GET	/notifications	controllers.Notifications.getNotifications()	develop	\N	200	9321698f-b50ea31a-5867fac7-45230da0-cf41f8b5-20bcf048-f54a95d6-0a606252	7	NotificationsAll
209	tst-admin	\N	2025-12-18 17:35:57.696	GET	/match-notifications	controllers.Notifications.getMatchNotifications()	develop	\N	200	a2a618a1-afa67f62-bbc87c6b-0a623326-d2fcd623-d4f63a01-b6a57b62-da50efb4	7	NotificationsAll
210	tst-admin	\N	2025-12-18 17:35:57.718	GET	/profiledata-desktop	controllers.ProfileData.getDesktopProfiles()	develop	\N	200	8b5268d6-02253c81-07c1c9a1-2ffbb94b-99a23e98-daafac31-c3d30fd7-d273b64b	7	ProfiledataRead
211	tst-admin	\N	2025-12-18 17:35:57.753	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	b8b51330-90eee43e-3c4525cc-272019c5-75571c61-e0ec9068-74a6734d-b1a9132d	7	NotificationsAll
212	tst-admin	\N	2025-12-18 17:35:57.753	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	a9d5b121-892154b0-8bf2c5bd-f65c4a07-88095ae1-5c80d418-7b9ccb53-51799b2e	7	NotificationsAll
213	tst-admin	\N	2025-12-18 17:35:57.872	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	71fdf912-ffdbac74-9adadf42-5a9b33a6-96b5e3c6-8a3b1b6e-dbb7cd22-5977f3d7	7	NotificationsAll
214	tst-admin	\N	2025-12-18 17:39:26.115	POST	/login	controllers.Authentication.login()	develop	\N	200	7c97e62c-068c9be6-dd917840-f385e300-3ae209e4-38f348c6-ed103371-a6d790e2	8	Login
215	tst-admin	\N	2025-12-18 17:39:26.205	GET	/match-notifications	controllers.Notifications.getMatchNotifications()	develop	\N	200	851eb617-7faf5446-d0bc9450-1603439d-1e180ee2-f72be560-07f95bbf-c5c9eb60	8	NotificationsAll
216	tst-admin	\N	2025-12-18 17:39:26.205	GET	/notifications	controllers.Notifications.getNotifications()	develop	\N	200	4bff74be-df12a95a-deb0d1b8-1bcd7207-79849440-12473570-6cee0335-5da11af8	8	NotificationsAll
217	tst-admin	\N	2025-12-18 17:39:26.211	GET	/profiledata-desktop	controllers.ProfileData.getDesktopProfiles()	develop	\N	200	0ede62d4-7942a314-9128f999-20d5bbf9-a74f10ca-d1fd126c-77aa5d45-a2443d43	8	ProfileToLimsdataExport
218	tst-admin	\N	2025-12-18 17:39:26.265	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	ed680331-0e7fa342-bc9c6e2e-967e1144-0de08cbd-5382bb75-a10a7419-e5306906	8	NotificationsAll
219	tst-admin	\N	2025-12-18 17:39:26.265	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	21e76fe8-b81d4e7a-a388bb5a-72554d38-8f52e00c-c3d872c4-47370db2-821b74a5	8	NotificationsAll
220	tst-admin	\N	2025-12-18 17:39:26.371	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	04569a08-d81df9a8-dd64234d-d72090bf-345bf4f9-c4b8624b-7a82d327-e04335f7	8	NotificationsAll
221	tst-admin	\N	2025-12-18 17:39:33.98	GET	/bulkupload/step2/batches	controllers.BulkUpload.getBatchesStep2()	develop	\N	200	baea63a9-1d6af8ce-d249ee19-91bcad30-8a7f60f8-97d3af89-8f2c6b37-1e95c021	8	ProtoprofileRead
222	tst-admin	\N	2025-12-18 17:39:33.988	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	b04a09fe-c77396eb-e4495893-8f1dbdf9-05826216-6b4e99d8-36302aea-9d3e4388	8	CategoryTreeRead
223	tst-admin	\N	2025-12-18 17:39:34.075	GET	/categories	controllers.Categories.list()	develop	\N	200	048a8339-a39ac0ca-dde353b2-0112041b-ed41c56d-5eac9e84-54515d51-e37d50ae	8	CategoryTreeRead
224	tst-admin	\N	2025-12-18 17:39:34.084	GET	/locus-full	controllers.Locis.listFull()	develop	\N	200	a6907335-3e5f6cc0-f688b7c3-3bb5bfcf-f74ed0db-7934638b-cd00daa8-f6bd9f4b	8	LocusRead
225	tst-admin	\N	2025-12-18 17:39:35.87	GET	/bulkupload/step2/protoprofiles	controllers.BulkUpload.getProtoProfilesStep2()	develop	\N	200	0a4ea335-1aa328a3-2c6eb125-cdbd1aec-5dc3ed40-cfa3000f-2d83349a-e7a10834	8	BulkuploaderBatches
226	tst-admin	\N	2025-12-18 17:39:35.907	GET	/profileData/isProfileReplicated/Evi02-C1	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	fc5c5de4-c931ee13-2403dc83-39e855fb-74b998a7-35dc78bc-fe525e25-db8d7c94	8	ProfileDataRead
227	tst-admin	\N	2025-12-18 17:39:35.907	GET	/profileData/isProfileReplicated/Vict-C1	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	33b4d0b0-47070bbf-76a0431f-0bbffe81-28fee5e0-8407fdb9-7d7e4e53-be9f1877	8	ProfileDataRead
228	tst-admin	\N	2025-12-18 17:40:19.545	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	53d6e318-edc96be5-bfebca4f-5be0f687-6015b883-68bb99f7-7ca908bb-5057a607	8	CategoryTreeRead
229	tst-admin	\N	2025-12-18 17:40:19.551	GET	/locus-full	controllers.Locis.listFull()	develop	\N	200	ff0ce4f2-25ade109-3611c265-9253b8ee-e2bd1580-5547aa56-683b7d0b-198a1297	8	LocusRead
230	tst-admin	\N	2025-12-18 17:40:19.562	GET	/bulkupload/step1/batches	controllers.BulkUpload.getBatchesStep1()	develop	\N	200	397151f2-0a090f1a-7b5251ea-0d919c03-8f975ac7-d6569406-b3f0bf08-30fb3df3	8	BulkuploaderBatches
231	tst-admin	\N	2025-12-18 17:40:47.451	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	2b62cd1b-d6c69e30-574ed5c5-e36d5c22-3a088c0a-25d4ce8d-1cb32a4f-c1d52a1f	8	CategoryTreeRead
232	tst-admin	\N	2025-12-18 17:40:47.455	GET	/bulkupload/step2/batches	controllers.BulkUpload.getBatchesStep2()	develop	\N	200	a902bf88-7320c635-696afa96-47fe35a3-97c3ef9b-be81b795-05b58dbe-e3165930	8	ProtoprofileRead
233	tst-admin	\N	2025-12-18 17:40:47.456	GET	/locus-full	controllers.Locis.listFull()	develop	\N	200	62580910-8ecab0fd-21fcf387-ece5e6a7-f8a096e6-fd0dfb56-7327d7d9-c5c14fc0	8	LocusRead
234	tst-admin	\N	2025-12-18 17:40:47.467	GET	/categories	controllers.Categories.list()	develop	\N	200	cfc44e9e-644fb52c-432109a6-39f8c057-d281d479-6e87d0c4-047c4fdf-e1e589ed	8	CategoryTreeRead
247	tst-admin	\N	2025-12-18 17:41:28.395	POST	/search/profileData/search	controllers.SearchProfileDatas.search()	develop	\N	200	212db450-7e8fd8a3-ea436e45-e39071e0-117aebc0-4a082171-536839ab-5a1cdd17	8	ProfiledataSearch
235	tst-admin	\N	2025-12-18 17:40:56.532	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	b93bb778-3cea09f1-b9269119-8498516b-6fd81221-07e83915-3e6ef218-48a80822	8	NotificationsAll
241	tst-admin	\N	2025-12-18 17:41:14.561	GET	/bulkupload/step2/protoprofiles	controllers.BulkUpload.getProtoProfilesStep2()	develop	\N	200	d50c34c5-f9ae5e07-4273a58a-a8f63c9d-a2179194-4fb2766e-6a97a583-e755f19e	8	BulkuploaderBatches
236	tst-admin	\N	2025-12-18 17:40:56.561	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	34c6eb49-21bea1df-fa591018-37e4bfae-4ebaf9f5-857a342c-7b4fa233-07e2bf75	8	NotificationsAll
242	tst-admin	\N	2025-12-18 17:41:14.579	GET	/profileData/isProfileReplicated/Evi02-C1	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	81beb9f0-6b46e6c5-261b148d-057eb458-3fa1c41d-9d0d848e-7cf2d1d0-40e5f77a	8	ProfileDataRead
237	tst-admin	\N	2025-12-18 17:41:12.305	GET	/categoryTree	controllers.Categories.categoryTree()	develop	\N	200	13edc5db-a40635d0-79028566-197fdd2e-6d60213d-0d63c71c-9479dc8d-ae523ddf	8	CategoryTreeRead
238	tst-admin	\N	2025-12-18 17:41:12.31	GET	/locus-full	controllers.Locis.listFull()	develop	\N	200	caf961d4-209da748-572c6b75-1bb9322a-10d64132-360071cd-35c78421-340ecc39	8	LocusRead
243	tst-admin	\N	2025-12-18 17:41:14.579	GET	/profileData/isProfileReplicated/Vict-C1	controllers.ProfileData.getIsProfileReplicatedInternalCode()	develop	\N	200	9a2d365a-b46abd24-09461d99-d0fc1725-f6255d1c-9c6468d1-12abdc6d-49897bc1	8	ProfileDataRead
239	tst-admin	\N	2025-12-18 17:41:12.313	GET	/bulkupload/step2/batches	controllers.BulkUpload.getBatchesStep2()	develop	\N	200	e38f2687-9aad5047-eb789681-09ac0600-9228e1bf-f4ba80a5-77b77501-bbf4b6a2	8	ProtoprofileRead
240	tst-admin	\N	2025-12-18 17:41:12.319	GET	/categories	controllers.Categories.list()	develop	\N	200	fae8a1f0-df9a6fb2-34de8fa0-babac787-4041feed-e671cae7-f7b09c62-7ec71de6	8	CategoryTreeRead
244	tst-admin	\N	2025-12-18 17:41:28.255	GET	/categories	controllers.Categories.list()	develop	\N	200	7ea3e594-62f19adc-5166de83-4aedaeec-80940ba0-4875c82d-58bb79d4-8bad1189	8	CategoryTreeRead
245	tst-admin	\N	2025-12-18 17:41:28.273	GET	/categoriesWithProfiles	controllers.Categories.listWithProfiles()	develop	\N	200	7ae5a821-d85f8a7c-54f4b9e7-60ad3ca0-fbecf5e8-3f20f4f1-4a45220a-eab317df	8	CategoryTreeRead
246	tst-admin	\N	2025-12-18 17:41:28.33	POST	/search/profileData/searchTotal	controllers.SearchProfileDatas.searchTotal()	develop	\N	200	5de9982b-401e4448-647dba67-5476c68c-c7f99132-34682dfa-e0480d1b-6773cb0e	8	ProfiledataSearch
248	tst-admin	\N	2025-12-18 18:14:16.382	POST	/login	controllers.Authentication.login()	develop	\N	200	a2e24f9d-398620df-95512b13-a0d9ff75-bc9310ad-810af64a-8c0f7e79-bf57aa1a	9	Login
249	tst-admin	\N	2025-12-18 18:14:16.505	GET	/notifications	controllers.Notifications.getNotifications()	develop	\N	200	ba94e955-12e3e821-668e78e9-cb0f20ec-205c3cd3-8b02e7a1-170f9a66-38e3666c	9	NotificationsAll
250	tst-admin	\N	2025-12-18 18:14:16.505	GET	/match-notifications	controllers.Notifications.getMatchNotifications()	develop	\N	200	0a602834-04dafa99-ea50275b-86f764e6-f6ab8158-0c95008e-397308bc-a971179d	9	NotificationsAll
251	tst-admin	\N	2025-12-18 18:14:16.523	GET	/profiledata-desktop	controllers.ProfileData.getDesktopProfiles()	develop	\N	200	bd68eb32-8ca8512b-5d4b0c92-913c52ac-d6160787-ad55d57f-9f034def-9cc79563	9	ProfiledataExport
252	tst-admin	\N	2025-12-18 18:14:16.561	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	a4ab33e2-0450692c-64138a6f-514b3621-1ab42718-924e5c9f-328cefb6-d873fc56	9	NotificationsAll
253	tst-admin	\N	2025-12-18 18:14:16.561	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	3f4c5923-f72353d7-19043012-cc9e5690-cf0a6c9f-ec75f597-5a49fc5b-ae86ce91	9	NotificationsAll
254	tst-admin	\N	2025-12-18 18:14:16.658	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	6e1d278f-aefb4071-1d7302fb-490df3ed-4b45ea32-12f77d6b-59e7ea54-a5c7e0a9	9	NotificationsAll
255	tst-admin	\N	2025-12-18 18:14:20.553	GET	/categoriesWithProfiles	controllers.Categories.listWithProfiles()	develop	\N	200	41ae4509-8c023a5e-17247d49-c7e7ca2b-67ec6444-c0a4a05e-794bc6b3-a70b4af4	9	CategoryTreeRead
256	tst-admin	\N	2025-12-18 18:14:20.622	POST	/search/profileData/searchTotal	controllers.SearchProfileDatas.searchTotal()	develop	\N	200	b4b2ce77-fd6ed533-7d0e6c5b-19d1b72e-fd170f0a-9f746009-7a973b33-57dea45d	9	ProfiledataSearch
257	tst-admin	\N	2025-12-18 18:14:20.677	GET	/categories	controllers.Categories.list()	develop	\N	200	06e25d73-b5ed8648-5614f44e-9035b901-343bbc46-ad7029ad-e2f1be10-00b9dc87	9	CategoryTreeRead
258	tst-admin	\N	2025-12-18 18:14:20.707	POST	/search/profileData/search	controllers.SearchProfileDatas.search()	develop	\N	200	2cb23a72-7f32453f-b9d5a147-ea497e61-16976330-3776e757-d7de4d9b-e9cee70f	9	ProfiledataSearch
259	tst-admin	\N	2025-12-18 18:14:20.832	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	dd25695b-03a1cce1-4dcce995-cbbf34af-0c8eef36-f6aea458-00aa37d0-be93618d	9	MatchAll
260	tst-admin	\N	2025-12-18 18:14:20.832	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	4a1a0592-fcaf3f24-1d840f61-e867bc97-6032f98c-993626f0-beacab74-367e1918	9	MatchAll
261	tst-admin	\N	2025-12-18 18:14:20.832	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	889d38a0-ed50c202-08077c48-263ab357-e89c43ce-cab0d65a-bb4505cb-be6990fc	9	MatchAll
262	tst-admin	\N	2025-12-18 18:14:20.846	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	e916bbae-0baaf1ae-eb1a540c-992890c9-d33c81c6-ea1f2c94-47aee06a-b38102f7	9	MatchAll
263	tst-admin	\N	2025-12-18 18:14:20.846	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	bd12ae8a-53649f34-021f31ce-316e2381-1e7bb168-d117203d-cd236a9e-5c1bff05	9	MatchAll
264	tst-admin	\N	2025-12-18 18:14:20.945	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	a1a56abf-26b14fe7-33ce349d-8aa343a0-794d1bbd-be51e57a-fc7351f3-3f55ab90	9	MatchAll
265	tst-admin	\N	2025-12-18 18:14:20.945	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	7cf745ed-e1ba14de-fb30f94b-09823f32-dae708f8-0845bdd3-6f93b188-af2d38cc	9	MatchAll
266	tst-admin	\N	2025-12-18 18:14:32.882	GET	/analysistypes	controllers.AnalysisTypes.list()	develop	\N	200	5235ca1c-76acf2d5-1f9ebaad-1121aec6-bfc5f3d7-ec6995d7-75abc14a-d056ad16	9	AnalysisTypeRead
267	tst-admin	\N	2025-12-18 18:14:32.95	GET	/profiledataWithAssociations	controllers.ProfileData.findByCodeWithAssociations()	develop	\N	200	5d26cf7f-79757cec-b3952ced-1eac079e-62b81534-ff5cfaf0-395f484b-384167fb	9	ProfiledataExport
268	tst-admin	\N	2025-12-18 18:14:32.969	GET	/strkits	controllers.StrKits.list()	develop	\N	200	be9010d1-8e952901-8103a232-2b3d284f-c0c5c784-3052fd06-093affbd-084b1524	9	StrKitsAll
269	tst-admin	\N	2025-12-18 18:14:32.985	GET	/profiles-labelsets	controllers.Profiles.getLabelsSets()	develop	\N	200	2920c166-1d63b7b0-63e6c01f-00550606-791c0a5b-94cf79a6-9ef0f12a-d81bd903	9	ProfilesRead
270	tst-admin	\N	2025-12-18 18:14:33.013	GET	/getFilesId	controllers.Resources.getFilesId()	develop	\N	200	a42ba89e-34a8b2bc-d8b7719e-ae5de565-8ecd4ba9-01a3bfbb-313d079d-5ba3c855	9	UploadImageAll
271	tst-admin	\N	2025-12-18 18:14:33.031	GET	/locus	controllers.Locis.list()	develop	\N	200	26ddc014-447e10a5-7a518037-b3b2564f-71471be9-04521497-d3209ad3-a93a5631	9	LocusRead
272	tst-admin	\N	2025-12-18 18:14:33.031	GET	/profiles/full/AR-C-SHDG-1108	controllers.Profiles.getFullProfile()	develop	\N	200	908a3399-9f4f533e-279163c4-c78d91f9-7a2fac0d-180d2af5-4420ab83-ea5682fd	9	ProfilesRead
273	tst-admin	\N	2025-12-18 18:14:33.036	GET	/getFilesId	controllers.Resources.getFilesId()	develop	\N	200	007ba2a1-c150e4ea-5759c5d4-05d0e90d-be5e2665-8925ca83-bb3bcea1-5cca5b5d	9	UploadImageAll
274	tst-admin	\N	2025-12-18 18:14:33.098	GET	/strkits/Powerplex16/loci	controllers.StrKits.findLociByKit()	develop	\N	200	99dd0622-4623724c-8f4b95c6-edc805ea-1bfa635f-91914223-d42d5d8e-9614b1a3	9	StrKitsAll
275	tst-admin	\N	2025-12-18 18:14:36.248	POST	/search/profileData/searchTotal	controllers.SearchProfileDatas.searchTotal()	develop	\N	200	05ab2846-af2a0d08-3ed44fe5-3bb218a4-ee272dc6-18a54652-a3503851-ec5f7075	9	ProfiledataSearch
276	tst-admin	\N	2025-12-18 18:14:36.25	GET	/categoriesWithProfiles	controllers.Categories.listWithProfiles()	develop	\N	200	84c765b7-e7b6f537-551300fa-2fcbbd5d-a7364481-370a3a01-c7667086-c7f8291e	9	CategoryTreeRead
277	tst-admin	\N	2025-12-18 18:14:36.262	GET	/categories	controllers.Categories.list()	develop	\N	200	25f7cc42-c6f5f061-e20964a5-3b15fe62-c2bde39d-a01f56c3-4394f7b8-715d9dcd	9	CategoryTreeRead
278	tst-admin	\N	2025-12-18 18:14:36.292	POST	/search/profileData/search	controllers.SearchProfileDatas.search()	develop	\N	200	e9bc46f8-238b0044-cca1b723-f48a9eb9-714cccf0-0c6d47f4-ed788750-83d64368	9	ProfiledataSearch
279	tst-admin	\N	2025-12-18 18:14:36.353	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	dbede0d4-09de646b-c3c98f61-ef0b9e40-e970674c-6c12ea56-037cc498-06258d93	9	MatchAll
280	tst-admin	\N	2025-12-18 18:14:36.354	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	60f2a2f8-12273ef1-796724c0-f1568c84-41d4c951-04960344-271893b5-40e68368	9	MatchAll
285	tst-admin	\N	2025-12-18 18:14:36.395	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	1b5f8df9-19e6dd46-d9426e53-4e151c32-25b98da1-91b90b26-a0f0ad89-148303ca	9	MatchAll
281	tst-admin	\N	2025-12-18 18:14:36.353	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	84c00d42-3f547470-b5960540-9488c85a-92ff2172-d4911f88-48ba5ce4-b6ada043	9	MatchAll
286	tst-admin	\N	2025-12-18 18:14:37.364	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	2e4340e6-03750a4d-a52b84fd-c405eff2-0e19ca9b-2f6baa86-80ddb4f8-069db196	9	NotificationsAll
282	tst-admin	\N	2025-12-18 18:14:36.364	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	53f633eb-525f7ef9-4abee764-ebdab74a-abb7f3d2-54e0b5fc-b584d8ba-bbb20215	9	MatchAll
287	tst-admin	\N	2025-12-18 18:14:37.386	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	65abfc7b-3e7263e1-ca5243f7-e5e8e36e-6e3e3055-a586a903-531ecb73-654a6d37	9	NotificationsAll
283	tst-admin	\N	2025-12-18 18:14:36.385	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	af4b28bc-081c5b67-aacc88e0-d00a2dfd-374a7bd2-6ff44fd8-fed5f4bb-6e123278	9	MatchAll
284	tst-admin	\N	2025-12-18 18:14:36.395	GET	/matching-profile	controllers.Matching.searchMatchesProfile()	develop	\N	200	668d6bb8-b2f04b7e-09f563a8-4669e666-81740fda-ef0d811f-21d600c8-5e2a2678	9	MatchAll
288	tst-admin	\N	2025-12-18 18:17:19.25	POST	/login	controllers.Authentication.login()	develop	\N	200	a096b0c4-c7497bb2-72c28663-8f19ccdb-d887fb84-e8c8fc7f-7b7b727f-6ae705db	10	Login
289	tst-admin	\N	2025-12-18 18:17:19.37	GET	/notifications	controllers.Notifications.getNotifications()	develop	\N	200	74d22abc-8930e1d0-c2197800-6392ab03-c9b8b557-78deb9a9-9315021c-2e861c3d	10	NotificationsAll
290	tst-admin	\N	2025-12-18 18:17:19.37	GET	/match-notifications	controllers.Notifications.getMatchNotifications()	develop	\N	200	517223b2-029105b1-ec86630a-ec98f5fb-e44a347a-67f3d526-a818184e-d74de623	10	NotificationsAll
291	tst-admin	\N	2025-12-18 18:17:19.375	GET	/profiledata-desktop	controllers.ProfileData.getDesktopProfiles()	develop	\N	200	c4050853-000419c4-fd342867-8a0f7061-36cb8fb0-76fea806-89a8d753-e008fc26	10	ProfiledataRead
292	tst-admin	\N	2025-12-18 18:17:19.429	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	f0c014f5-091847cf-2a0f2a97-21d04b0d-a35acee5-d5386cca-a4b7af7d-9881da86	10	NotificationsAll
293	tst-admin	\N	2025-12-18 18:17:19.429	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	e88c7fe4-2e77a345-b86c99b0-4c9090e2-c26b44ad-34b37628-205d742d-9723cb8a	10	NotificationsAll
294	tst-admin	\N	2025-12-18 18:17:19.526	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	ddf87e28-88b10027-43f64ea7-7b75f9d6-4cffcbce-3faed490-69df158f-5d366b5d	10	NotificationsAll
295	tst-admin	\N	2025-12-18 18:17:22.349	GET	/categoriesWithProfiles	controllers.Categories.listWithProfiles()	develop	\N	200	ddc9ddec-374fdf9a-7d2c5087-7d8f5d85-b7325e03-72414f29-03006113-68a90435	10	CategoryTreeRead
296	tst-admin	\N	2025-12-18 18:17:22.42	POST	/search/profileData/searchTotal	controllers.SearchProfileDatas.searchTotal()	develop	\N	200	e380c36a-e47ac76a-ea2c4644-0e3cb8b1-1c7ef2a3-ece3e4b5-0e2f0661-cbd92cb2	10	ProfiledataSearch
297	tst-admin	\N	2025-12-18 18:17:22.469	GET	/categories	controllers.Categories.list()	develop	\N	200	e9636fbc-683f1674-e32cf75d-0ca6ef92-95ee1dc7-fc5552bb-5c31a1ef-c2cb8e1e	10	CategoryTreeRead
298	tst-admin	\N	2025-12-18 18:17:22.504	POST	/search/profileData/search	controllers.SearchProfileDatas.search()	develop	\N	200	40de2b94-b7343996-cff0772a-153be5ca-3598bda0-0bf895a1-835883df-07a3446c	10	ProfiledataSearch
299	tst-admin	\N	2025-12-18 18:17:26.075	GET	/analysistypes	controllers.AnalysisTypes.list()	develop	\N	200	df856b1d-ea297e74-ba59c4fc-15116c3a-bc59a2eb-285057e5-bb5b9fd0-8519ea37	10	AnalysisTypeRead
300	tst-admin	\N	2025-12-18 18:17:26.148	GET	/profiles-labelsets	controllers.Profiles.getLabelsSets()	develop	\N	200	fd51d790-6df2c276-7c8b051b-0a8dd74f-4e85bbba-47e88086-c110c75c-6351e309	10	ProfilesRead
301	tst-admin	\N	2025-12-18 18:17:26.158	GET	/profiledataWithAssociations	controllers.ProfileData.findByCodeWithAssociations()	develop	\N	200	dd40c75e-5e3e9002-dc9b8f6d-160f39b1-981e02d0-4afdbab4-7a077751-d9c4eccb	10	ProfiledataRead
302	tst-admin	\N	2025-12-18 18:17:26.172	GET	/strkits	controllers.StrKits.list()	develop	\N	200	6ac2417a-99cc24a7-0778e44e-734906f6-1f14c171-19f22c43-79fb1fd5-ad274d92	10	StrKitsAll
303	tst-admin	\N	2025-12-18 18:17:26.193	GET	/getFilesId	controllers.Resources.getFilesId()	develop	\N	200	429a0105-b693a103-0800f696-8bbbe5d5-86ee3d7a-3e0f0a4e-fc994229-5f16fd61	10	UploadImageAll
304	tst-admin	\N	2025-12-18 18:17:26.197	GET	/getFilesId	controllers.Resources.getFilesId()	develop	\N	200	350c9384-a9a09ae6-26c66c4b-6439fc83-03da51e9-7abfb4dd-d4ef0e6b-1ea153eb	10	UploadImageAll
305	tst-admin	\N	2025-12-18 18:17:26.205	GET	/locus	controllers.Locis.list()	develop	\N	200	56fe7f1e-ddd8f3a2-4f413e75-143cfd0f-96353ba5-495dd0c5-cf5975f5-98d31d53	10	LocusRead
306	tst-admin	\N	2025-12-18 18:44:50.611	POST	/login	controllers.Authentication.login()	develop	\N	200	83dfbe4f-3e4661af-6cf29061-6f77177e-9865feb5-c882797f-c6a22a85-d0066b77	11	Login
307	tst-admin	\N	2025-12-18 18:44:50.787	GET	/notifications	controllers.Notifications.getNotifications()	develop	\N	200	c922669a-337ce916-0130ee64-31073fb4-2462768d-0dba11b2-dc0d04fa-2ef271ff	11	NotificationsAll
308	tst-admin	\N	2025-12-18 18:44:50.787	GET	/match-notifications	controllers.Notifications.getMatchNotifications()	develop	\N	200	c7daf4c9-395a7e98-89ffe668-848f233e-34ff149e-1b8e2cab-13c4ade9-38954fd3	11	NotificationsAll
309	tst-admin	\N	2025-12-18 18:44:50.828	GET	/profiledata-desktop	controllers.ProfileData.getDesktopProfiles()	develop	\N	200	71953019-9e3ab467-39e7032d-55bfc611-5881442f-ed77d81b-3b7336ae-3e023aa2	11	ProfiledataRead
310	tst-admin	\N	2025-12-18 18:44:50.854	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	77b63c3c-42676d09-d7736c41-189d3832-00399389-e03e32f5-d5dc30ab-40dea917	11	NotificationsAll
311	tst-admin	\N	2025-12-18 18:44:50.854	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	dba3e217-5b5e4297-339520e0-6731d1cc-12396db9-d4152872-d8d2945c-12537bab	11	NotificationsAll
312	tst-admin	\N	2025-12-18 18:44:50.98	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	d5ba0b6a-6da23e90-e362ed06-c71534ca-003655bc-5b051fbc-823ba7d2-bc5666db	11	NotificationsAll
313	tst-admin	\N	2025-12-18 18:44:53.73	GET	/categoriesWithProfiles	controllers.Categories.listWithProfiles()	develop	\N	200	37d627e6-a3d17bd6-ff5dcdc3-e9f5c250-129e6cca-62a233b4-f0abfe55-0e464729	11	CategoryTreeRead
314	tst-admin	\N	2025-12-18 18:44:53.813	POST	/search/profileData/searchTotal	controllers.SearchProfileDatas.searchTotal()	develop	\N	200	f5a2e7a7-3e965efd-2782ea92-9433d566-79e2a936-0f7941f9-750d8d27-af60d594	11	ProfiledataSearch
315	tst-admin	\N	2025-12-18 18:44:53.878	GET	/categories	controllers.Categories.list()	develop	\N	200	86a7c23b-d32d9c65-a2519d87-d9906b54-3c06a2ca-5eb525bd-d875fda1-c67a5831	11	CategoryTreeRead
316	tst-admin	\N	2025-12-18 18:44:53.925	POST	/search/profileData/search	controllers.SearchProfileDatas.search()	develop	\N	200	bb5390b8-b3e2600e-b299b6b3-9faa27af-d77411a5-0052bd03-c8d06685-9ac5d4a1	11	ProfiledataSearch
317	tst-admin	\N	2025-12-18 18:58:02.728	POST	/login	controllers.Authentication.login()	develop	\N	200	bf152aa7-3acb44b1-cf15a526-9af4394b-8316ed5e-80707eae-408a0ecb-ec201969	12	Login
318	tst-admin	\N	2025-12-18 18:58:02.837	GET	/match-notifications	controllers.Notifications.getMatchNotifications()	develop	\N	200	0f9811ae-27430539-258e4b56-64ae9e39-1de7f427-0d251601-02fef7c3-af9652a0	12	NotificationsAll
319	tst-admin	\N	2025-12-18 18:58:02.837	GET	/notifications	controllers.Notifications.getNotifications()	develop	\N	200	e828a7cd-104175f0-a80f0eb1-012312d1-cdb129a8-16ad6133-263940a4-42ecf1cf	12	NotificationsAll
320	tst-admin	\N	2025-12-18 18:58:02.847	GET	/profiledata-desktop	controllers.ProfileData.getDesktopProfiles()	develop	\N	200	5107de24-6cc6d3a5-eae352d7-f104bdde-0c58c243-f977d978-56f3ffed-d88bf7bb	12	ProfiledataRead
321	tst-admin	\N	2025-12-18 18:58:02.896	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	2e62663a-087b4180-232bb382-7ec1793f-ae151cde-f74f9c15-975cb63d-0508adfa	12	NotificationsAll
325	tst-admin	\N	2025-12-18 18:58:06.657	POST	/search/profileData/searchTotal	controllers.SearchProfileDatas.searchTotal()	develop	\N	200	669b928c-d85b8706-b7d2b174-f9c4b13e-5198265f-82800d8f-5a5c496b-28ec01f1	12	ProfiledataSearch
326	tst-admin	\N	2025-12-18 18:58:06.702	GET	/categories	controllers.Categories.list()	develop	\N	200	c35f82da-3f854edf-f34c9364-ba72d41e-f078acb2-d1a0509d-ddf8f10d-703fb0c5	12	CategoryTreeRead
322	tst-admin	\N	2025-12-18 18:58:02.896	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	8f72d883-f6eeeac7-96787893-49f4a7b9-68a8f884-8c92750d-f82fa542-48415ab6	12	NotificationsAll
327	tst-admin	\N	2025-12-18 18:58:06.746	POST	/search/profileData/search	controllers.SearchProfileDatas.search()	develop	\N	200	b9a613a1-b4469ae7-ac8b6e2f-e469a191-5248a776-65ff5d3e-bc481a4c-ef843fa9	12	ProfiledataSearch
323	tst-admin	\N	2025-12-18 18:58:02.999	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	21b25eeb-2a589bf0-db4656a8-0b83f58e-ba76ec59-9c8b01c9-5043d5a7-0954b7ee	12	NotificationsAll
324	tst-admin	\N	2025-12-18 18:58:06.589	GET	/categoriesWithProfiles	controllers.Categories.listWithProfiles()	develop	\N	200	58c06e7a-65da25ad-e5f92cf2-8348a783-8e867f4b-8a78c016-65641156-2f07743d	12	CategoryTreeRead
328	tst-admin	\N	2025-12-19 18:31:06.112	POST	/login	controllers.Authentication.login()	develop	\N	200	ca4966dc-bb2d5986-4f043801-5312e728-e3683796-fcc4f533-b308114b-52bf8e80	13	Login
329	tst-admin	\N	2025-12-19 18:31:06.213	GET	/notifications	controllers.Notifications.getNotifications()	develop	\N	200	9bd3b379-3b768dd7-663abb9d-1e6822c7-15afcfa0-63ac8002-6718d788-d76b7189	13	NotificationsAll
330	tst-admin	\N	2025-12-19 18:31:06.213	GET	/match-notifications	controllers.Notifications.getMatchNotifications()	develop	\N	200	60d1ad5e-a828e733-7d00dd88-1aa3bf9b-53059d21-fe2f0837-b4c3651a-813afb7e	13	NotificationsAll
331	tst-admin	\N	2025-12-19 18:31:06.273	GET	/profiledata-desktop	controllers.ProfileData.getDesktopProfiles()	develop	\N	200	84f66702-86817589-c33b29fa-14256f6b-dd31654f-1ea19910-b40d3702-95402992	13	ProfileToLimsdataExport
332	tst-admin	\N	2025-12-19 18:31:06.305	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	0257ba23-514cd843-ff8f2671-9c266865-4eb1e874-6ed7781a-cf3cc66e-c3ed2bf0	13	NotificationsAll
333	tst-admin	\N	2025-12-19 18:31:06.305	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	f0f79401-b5208cdf-b37a8c58-f81b749b-7affb3d8-0aa410ca-f1003136-0dc6a924	13	NotificationsAll
334	tst-admin	\N	2025-12-19 18:31:06.419	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	bebd5a36-714d7faa-537f2fc9-fa20f4fe-b2acd8e9-5ba3529e-83fd85e9-6195d340	13	NotificationsAll
335	tst-admin	\N	2025-12-19 18:31:34.284	GET	/categoriesWithProfiles	controllers.Categories.listWithProfiles()	develop	\N	200	4fdb7a4c-8a3010a9-e0984c04-225aaa39-7a3ec0c5-bde64479-4273fbc0-b0294d89	13	CategoryExport
336	tst-admin	\N	2025-12-19 18:31:34.382	POST	/search/profileData/searchTotal	controllers.SearchProfileDatas.searchTotal()	develop	\N	200	0177907b-0859de25-b1701082-61480e11-2b9e51f4-488f005b-98f8adff-d7264066	13	ProfiledataSearch
337	tst-admin	\N	2025-12-19 18:31:34.503	GET	/categories	controllers.Categories.list()	develop	\N	200	98344ff2-f4406f98-b9f38c89-da0a7183-1a601994-8fa7e073-d1686bce-d36c0648	13	CategoryExport
338	tst-admin	\N	2025-12-19 18:31:34.539	POST	/search/profileData/search	controllers.SearchProfileDatas.search()	develop	\N	200	9c6df473-0ada0392-9d18fdac-76c721ed-6f637681-b331852d-9df0d7ec-3f4103e0	13	ProfiledataSearch
339	ANONYMOUS	\N	2026-02-02 19:07:20.076	POST	/login	controllers.Authentication.login()	develop	\N	404	66b710a5-de8ff70b-eebf5d4e-840cb12d-a5a33489-5f57c806-5f574a30-5a29cb30	14	Login
340	setup	\N	2026-02-02 19:07:27.615	POST	/login	controllers.Authentication.login()	develop	\N	404	478ea159-2e2b9187-269c3975-624f5d76-72fe4a4b-93641ad8-0fe5aea9-37d33a3a	14	Login
341	setup	\N	2026-02-02 19:07:47.884	POST	/login	controllers.Authentication.login()	develop	\N	200	a2214666-97defb40-937bb8e2-fe41ee4d-4d311c06-17c05205-a654112d-4d4a3c0b	14	Login
342	setup	\N	2026-02-02 19:07:48.228	GET	/match-notifications	controllers.Notifications.getMatchNotifications()	develop	\N	200	b59fc97a-bb00ab01-d3dc70b5-b7f67b3e-d5e164bb-b103d589-3d60262f-440ae55e	14	NotificationsAll
343	setup	\N	2026-02-02 19:07:48.228	GET	/notifications	controllers.Notifications.getNotifications()	develop	\N	200	4fd2e1d6-16088ea0-7c149358-676b534f-731951ac-c617b9e6-e7d97a4a-0b3dc0b5	14	NotificationsAll
344	setup	\N	2026-02-02 19:07:48.292	GET	/profiledata-desktop	controllers.ProfileData.getDesktopProfiles()	develop	\N	200	93021576-cd0f9163-d4c3e769-609a379d-4fcdf3a7-035ffc92-cb56092f-8a2bd66d	14	ProfiledataRead
345	setup	\N	2026-02-02 19:07:48.333	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	27c5de71-91630e60-9139be16-70cce2bd-dce4ef3a-e92a400c-7cc11671-ebc0d5f9	14	NotificationsAll
346	setup	\N	2026-02-02 19:07:48.333	POST	/notifications/total	controllers.Notifications.count()	develop	\N	200	cd5cacb9-b2f1d995-d01770c9-e3dc2cd3-adb5e893-6d762568-9bdc0161-e7d2a172	14	NotificationsAll
347	setup	\N	2026-02-02 19:07:48.522	POST	/notifications/search	controllers.Notifications.search()	develop	\N	200	52294552-d677a94c-f3f54eb4-9a837420-c887a1d8-a6c04b44-80b3c0dd-f59c7b17	14	NotificationsAll
\.


--
-- Data for Name: play_evolutions; Type: TABLE DATA; Schema: public; Owner: genissqladmin
--

COPY public.play_evolutions (id, hash, applied_at, apply_script, revert_script, state, last_problem) FROM stdin;
1	e5ac963be60c0030097ca657199e17fbb72c6eb5	2025-11-10 00:00:00	CREATE SCHEMA "LOG_DB";\n\nCREATE TABLE "LOG_DB"."OPERATION_LOG_LOT"\n(\n"ID" bigserial,\n"KEY_ZERO" character varying(200) NOT NULL,\n"INIT_TIME" timestamp DEFAULT now() NOT NULL,\nCONSTRAINT "OPERATION_LOG_LOT_PK" PRIMARY KEY ("ID")\n);\n\nCREATE TABLE "LOG_DB"."OPERATION_LOG_RECORD"\n(\n"ID" bigserial,\n\n"USER_ID" character varying(50) NOT NULL,\n"OTP" character varying(50),\n"TIMESTAMP" timestamp NOT NULL,\n"METHOD" character varying(50) NOT NULL,\n"PATH" character varying(1024) NOT NULL,\n"ACTION" character varying(512) NOT NULL,\n"BUILD_NO" character varying(150) NOT NULL,\n"RESULT" character varying(150),\n"STATUS" integer NOT NULL,\n"SIGNATURE" character varying(8192) NOT NULL,\n"LOT" bigint NOT NULL,\n"DESCRIPTION" character varying(1024) NOT NULL,\nCONSTRAINT "OPERATION_LOG_RECORD_PK" PRIMARY KEY ("ID"),\nCONSTRAINT "OPERATION_LOG_RECORD_FK" FOREIGN KEY ("LOT")\nREFERENCES "LOG_DB"."OPERATION_LOG_LOT" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION\n);	DROP SCHEMA  "LOG_DB" CASCADE;	applied	
\.


--
-- Name: OPERATION_LOG_LOT_ID_seq; Type: SEQUENCE SET; Schema: LOG_DB; Owner: genissqladmin
--

SELECT pg_catalog.setval('"LOG_DB"."OPERATION_LOG_LOT_ID_seq"', 14, true);


--
-- Name: OPERATION_LOG_RECORD_ID_seq; Type: SEQUENCE SET; Schema: LOG_DB; Owner: genissqladmin
--

SELECT pg_catalog.setval('"LOG_DB"."OPERATION_LOG_RECORD_ID_seq"', 347, true);


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

