--
-- Data for Name: ANALYSIS_TYPE; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."ANALYSIS_TYPE" ("ID", "NAME", "MITOCHONDRIAL") FROM stdin;
1	Autosomal	f
2	Cx	f
3	Cy	f
4	MT	t
\.

--
-- Data for Name: BIO_MATERIAL_TYPE; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."BIO_MATERIAL_TYPE" ("ID", "NAME", "DESCRIPTION") FROM stdin;
SEMEN	Semen	\N
BLOOD	Blood	\N
SKIN	Skin	\N
SALIVA	Saliva	\N
TOOTH	Tooth	\N
BONE	Bone	\N
TEXTILE	Textile	\N
\.

--
-- Data for Name: CASE_TYPE; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."CASE_TYPE" ("ID", "NAME") FROM stdin;
MPI	MPI
DVI	DVI
\.

--
-- Data for Name: GROUP; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."GROUP" ("ID", "NAME", "DESCRIPTION") FROM stdin;
ReferenceSamples	Reference Samples	\N
ForensicEvidences	Forensic Evidences	\N
AM	Ante Mortem Samples	\N
PM	Post Mortem Samples	\N
AM_DVI	Ante Mortem Samples DVI	\N
PM_DVI	Post Mortem Samples DVI	\N
\.

--
-- Data for Name: CATEGORY; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."CATEGORY" ("ID", "GROUP", "NAME", "DESCRIPTION", "FILIATION_DATA", "REPLICATE", "PEDIGREE_ASSOCIATION", "IS_REFERENCE", "ALLOW_MANUAL_LOADING", "TYPE") FROM stdin;
ER	AM	Personal items of the missing person	\N	f	t	t	f	t	2
IR	AM	Reference Individuals	\N	f	t	t	t	t	2
INNV	AM	People who seek to know their biological identity	\N	f	t	t	t	t	2
INN	PM	Unidentified deceased persons	\N	f	t	t	f	t	2
RNN	PM	Unidentified biological remains	\N	f	t	t	f	t	2
ENN	PM	Personal items found of the missing person	\N	f	t	t	f	t	2
PFNI	PM	Deceased persons whose identity is to be analyzed	\N	f	t	t	f	t	2
IR_DVI	AM_DVI	Reference Individuals	\N	f	t	t	t	t	3
ENN_DVI	PM_DVI	Personal items found of the missing person	\N	f	t	t	f	t	3
INN_DVI	PM_DVI	Unidentified deceased persons	\N	f	t	t	f	t	3
MultiallelicOffender	ReferenceSamples	Multiallelic Offender	Individual with more than two alleles per marker	f	f	f	t	t	1
PFNI_DVI	PM_DVI	Deceased persons whose identity is to be analyzed	\N	f	f	t	f	t	3
RNN_DVI	PM_DVI	Unidentified biological remains	\N	f	f	t	f	t	3
EliminationSample	ReferenceSamples	Elimination	\N	f	f	f	t	t	1
ForensicPartial	ForensicEvidences	Forensic Partial	Evidence with loss of markers	f	f	f	f	t	1
ConvictedOffender	ReferenceSamples	Convicted Offender	\N	t	f	f	t	t	1
ForensicUnknown	ForensicEvidences	Forensic Unknown	Complete evidence without loss of markers - 1 contributor	f	f	f	f	t	1
ForensicMixture	ForensicEvidences	Forensic Mixture	Evidence with more than one contributor	f	f	f	f	t	1
Suspect	ReferenceSamples	Suspect	\N	f	f	f	t	t	1
\.


--
-- Data for Name: CATEGORY_ALIAS; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."CATEGORY_ALIAS" ("ALIAS", "CATEGORY") FROM stdin;
Victim	EliminationSample
Elimination	EliminationSample
Forensic Partial	ForensicPartial
Convicted Offender	ConvictedOffender
Convicted	ConvictedOffender
Forensic unknown	ForensicUnknown
Forensic Unknown	ForensicUnknown
Forensic, Unknown	ForensicUnknown
Forensic, unknown	ForensicUnknown
Forensic Mixture	ForensicMixture
Mixture	ForensicMixture
\.

--
-- Data for Name: CATEGORY_ASSOCIATION; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."CATEGORY_ASSOCIATION" ("CATEGORY", "CATEGORY_RELATED", "MISMATCHS", "ID", "TYPE") FROM stdin;
ForensicMixture	EliminationSample	2	27	1
\.

--
-- Data for Name: CATEGORY_CONFIGURATION; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."CATEGORY_CONFIGURATION" ("ID", "CATEGORY", "TYPE", "COLLECTION_URI", "DRAFT_URI", "MIN_LOCUS_PER_PROFILE", "MAX_OVERAGE_DEVIATED_LOCI", "MAX_ALLELES_PER_LOCUS") FROM stdin;
13	ER	1			K/2	2	2
14	INN	1			K	2	2
15	INNV	1			K	2	2
16	RNN	1			K	2	2
17	ENN	1			K	2	2
18	IR	1			K	2	2
19	ER	2			K/2	2	2
20	INN	2			K	2	2
21	INNV	2			K	2	2
22	RNN	2			K	2	2
23	ENN	2			K	2	2
24	IR	2			K	2	2
25	ER	3			K/2	2	2
26	INN	3			K	2	2
27	INNV	3			K	2	2
28	RNN	3			K	2	2
29	ENN	3			K	2	2
30	IR	3			K	2	2
31	PFNI	1			K	2	2
32	PFNI	2			K	2	2
33	PFNI	3			K	2	2
34	INN_DVI	1			K	2	2
36	ENN_DVI	1			K	2	2
37	IR_DVI	1			K	2	2
39	INN_DVI	2			K	2	2
41	ENN_DVI	2			K	2	2
42	IR_DVI	2			K	2	2
44	INN_DVI	3			K	2	2
46	ENN_DVI	3			K	2	2
47	IR_DVI	3			K	2	2
445	ForensicPartial	1			K*0.7	0	2
446	ForensicPartial	2			K	0	6
447	ForensicPartial	3			K*0.7	3	1
448	ForensicPartial	4			K	0	6
341	MultiallelicOffender	1			K	K	6
342	MultiallelicOffender	2			K	0	6
343	MultiallelicOffender	3			K	0	6
344	MultiallelicOffender	4			K	0	6
525	ForensicMixture	1			K	4	6
526	ForensicMixture	2			K	0	6
527	ForensicMixture	3			K/2	4	6
528	ForensicMixture	4			K	0	6
361	PFNI_DVI	1			K	2	2
362	PFNI_DVI	2			K	2	2
363	PFNI_DVI	3			K	2	2
364	PFNI_DVI	4			K	0	6
365	RNN_DVI	1			K	2	2
366	RNN_DVI	2			K	2	2
367	RNN_DVI	3			K	2	2
368	RNN_DVI	4			K	0	6
441	EliminationSample	1			K	2	6
442	EliminationSample	2			K	0	6
443	EliminationSample	3			K	3	6
444	EliminationSample	4			K	0	6
541	Suspect	1			K/2	2	6
542	Suspect	2			K	0	6
543	Suspect	3			K/2	3	6
544	Suspect	4			K	0	6
473	ConvictedOffender	1			K	0	6
474	ConvictedOffender	2			K	0	6
475	ConvictedOffender	3			K-1	3	6
476	ConvictedOffender	4			K	0	6
489	ForensicUnknown	1			K/2	2	2
490	ForensicUnknown	2			K	0	6
491	ForensicUnknown	3			K/2	3	2
492	ForensicUnknown	4			K	0	6
\.


--
-- Data for Name: CATEGORY_MAPPING; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."CATEGORY_MAPPING" ("ID", "ID_SUPERIOR") FROM stdin;
ForensicUnknown	ForensicUnknown
ForensicMixture	ForensicMixture
ForensicPartial	ForensicPartial
ConvictedOffender	ConvictedOffender
Suspect	Suspect
EliminationSample	EliminationSample
MultiallelicOffender	MultiallelicOffender
\.

--
-- Data for Name: CATEGORY_MATCHING; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."CATEGORY_MATCHING" ("CATEGORY", "CATEGORY_RELATED", "PRIORITY", "MINIMUM_STRINGENCY", "FAIL_ON_MATCH", "FORWARD_TO_UPPER", "MATCHING_ALGORITHM", "MIN_LOCUS_MATCH", "MISMATCHS_ALLOWED", "ID", "TYPE", "CONSIDER_FOR_N") FROM stdin;
IR	ER	1	LowStringency	f	f	ENFSI	11	2	64	1	t
IR	ENN	1	LowStringency	f	f	ENFSI	11	2	65	1	t
ER	IR	1	LowStringency	f	f	ENFSI	11	2	66	1	t
ENN	IR	1	LowStringency	f	f	ENFSI	11	2	67	1	t
INN	IR	1	LowStringency	f	f	ENFSI	11	2	68	1	t
INN	ER	1	HighStringency	f	f	ENFSI	11	2	69	1	t
INN	INNV	1	HighStringency	f	f	ENFSI	11	2	70	1	t
INN	ENN	1	HighStringency	f	f	ENFSI	11	2	71	1	t
IR	INN	1	LowStringency	f	f	ENFSI	11	2	72	1	t
ER	INN	1	HighStringency	f	f	ENFSI	11	2	73	1	t
INNV	INN	1	HighStringency	f	f	ENFSI	11	2	74	1	t
ENN	INN	1	HighStringency	f	f	ENFSI	11	2	75	1	t
ER	ENN	1	HighStringency	f	f	ENFSI	11	2	76	1	t
ENN	ER	1	HighStringency	f	f	ENFSI	11	2	77	1	t
INNV	IR	1	LowStringency	f	f	ENFSI	11	2	78	1	t
INNV	ER	1	HighStringency	f	f	ENFSI	11	2	79	1	t
INNV	ENN	1	HighStringency	f	f	ENFSI	11	2	80	1	t
IR	INNV	1	LowStringency	f	f	ENFSI	11	2	81	1	t
ER	INNV	1	HighStringency	f	f	ENFSI	11	2	82	1	t
ENN	INNV	1	HighStringency	f	f	ENFSI	11	2	83	1	t
RNN	IR	1	LowStringency	f	f	ENFSI	11	2	84	1	t
RNN	INN	1	HighStringency	f	f	ENFSI	11	2	85	1	t
RNN	ER	1	HighStringency	f	f	ENFSI	11	2	86	1	t
RNN	INNV	1	HighStringency	f	f	ENFSI	11	2	87	1	t
RNN	ENN	1	HighStringency	f	f	ENFSI	11	2	88	1	t
IR	RNN	1	LowStringency	f	f	ENFSI	11	2	89	1	t
INN	RNN	1	HighStringency	f	f	ENFSI	11	2	90	1	t
ER	RNN	1	HighStringency	f	f	ENFSI	11	2	91	1	t
INNV	RNN	1	HighStringency	f	f	ENFSI	11	2	92	1	t
ENN	RNN	1	HighStringency	f	f	ENFSI	11	2	93	1	t
ER	ER	1	HighStringency	f	f	ENFSI	11	2	94	1	t
INN	INN	1	HighStringency	f	f	ENFSI	11	2	95	1	t
RNN	RNN	1	HighStringency	f	f	ENFSI	11	2	96	1	t
ENN	ENN	1	HighStringency	f	f	ENFSI	11	2	97	1	t
IR	ER	1	LowStringency	f	f	ENFSI	11	2	98	2	t
IR	ENN	1	LowStringency	f	f	ENFSI	11	2	99	2	t
ER	IR	1	LowStringency	f	f	ENFSI	11	2	100	2	t
ENN	IR	1	LowStringency	f	f	ENFSI	11	2	101	2	t
INN	IR	1	LowStringency	f	f	ENFSI	11	2	102	2	t
INN	ER	1	HighStringency	f	f	ENFSI	11	2	103	2	t
INN	INNV	1	HighStringency	f	f	ENFSI	11	2	104	2	t
INN	ENN	1	HighStringency	f	f	ENFSI	11	2	105	2	t
IR	INN	1	LowStringency	f	f	ENFSI	11	2	106	2	t
ER	INN	1	HighStringency	f	f	ENFSI	11	2	107	2	t
INNV	INN	1	HighStringency	f	f	ENFSI	11	2	108	2	t
ENN	INN	1	HighStringency	f	f	ENFSI	11	2	109	2	t
ER	ENN	1	HighStringency	f	f	ENFSI	11	2	110	2	t
ENN	ER	1	HighStringency	f	f	ENFSI	11	2	111	2	t
INNV	IR	1	LowStringency	f	f	ENFSI	11	2	112	2	t
INNV	ER	1	HighStringency	f	f	ENFSI	11	2	113	2	t
INNV	ENN	1	HighStringency	f	f	ENFSI	11	2	114	2	t
IR	INNV	1	LowStringency	f	f	ENFSI	11	2	115	2	t
ER	INNV	1	HighStringency	f	f	ENFSI	11	2	116	2	t
ENN	INNV	1	HighStringency	f	f	ENFSI	11	2	117	2	t
RNN	IR	1	LowStringency	f	f	ENFSI	11	2	118	2	t
RNN	INN	1	HighStringency	f	f	ENFSI	11	2	119	2	t
RNN	ER	1	HighStringency	f	f	ENFSI	11	2	120	2	t
RNN	INNV	1	HighStringency	f	f	ENFSI	11	2	121	2	t
RNN	ENN	1	HighStringency	f	f	ENFSI	11	2	122	2	t
IR	RNN	1	LowStringency	f	f	ENFSI	11	2	123	2	t
INN	RNN	1	HighStringency	f	f	ENFSI	11	2	124	2	t
ER	RNN	1	HighStringency	f	f	ENFSI	11	2	125	2	t
INNV	RNN	1	HighStringency	f	f	ENFSI	11	2	126	2	t
ENN	RNN	1	HighStringency	f	f	ENFSI	11	2	127	2	t
ER	ER	1	HighStringency	f	f	ENFSI	11	2	128	2	t
INN	INN	1	HighStringency	f	f	ENFSI	11	2	129	2	t
RNN	RNN	1	HighStringency	f	f	ENFSI	11	2	130	2	t
ENN	ENN	1	HighStringency	f	f	ENFSI	11	2	131	2	t
IR	ER	1	LowStringency	f	f	ENFSI	11	2	132	3	t
IR	ENN	1	LowStringency	f	f	ENFSI	11	2	133	3	t
ER	IR	1	LowStringency	f	f	ENFSI	11	2	134	3	t
ENN	IR	1	LowStringency	f	f	ENFSI	11	2	135	3	t
INN	IR	1	LowStringency	f	f	ENFSI	11	2	136	3	t
INN	ER	1	HighStringency	f	f	ENFSI	11	2	137	3	t
INN	INNV	1	HighStringency	f	f	ENFSI	11	2	138	3	t
INN	ENN	1	HighStringency	f	f	ENFSI	11	2	139	3	t
IR	INN	1	LowStringency	f	f	ENFSI	11	2	140	3	t
ER	INN	1	HighStringency	f	f	ENFSI	11	2	141	3	t
INNV	INN	1	HighStringency	f	f	ENFSI	11	2	142	3	t
ENN	INN	1	HighStringency	f	f	ENFSI	11	2	143	3	t
ER	ENN	1	HighStringency	f	f	ENFSI	11	2	144	3	t
ENN	ER	1	HighStringency	f	f	ENFSI	11	2	145	3	t
INNV	IR	1	LowStringency	f	f	ENFSI	11	2	146	3	t
INNV	ER	1	HighStringency	f	f	ENFSI	11	2	147	3	t
INNV	ENN	1	HighStringency	f	f	ENFSI	11	2	148	3	t
IR	INNV	1	LowStringency	f	f	ENFSI	11	2	149	3	t
ER	INNV	1	HighStringency	f	f	ENFSI	11	2	150	3	t
ENN	INNV	1	HighStringency	f	f	ENFSI	11	2	151	3	t
RNN	IR	1	LowStringency	f	f	ENFSI	11	2	152	3	t
RNN	INN	1	HighStringency	f	f	ENFSI	11	2	153	3	t
RNN	ER	1	HighStringency	f	f	ENFSI	11	2	154	3	t
RNN	INNV	1	HighStringency	f	f	ENFSI	11	2	155	3	t
RNN	ENN	1	HighStringency	f	f	ENFSI	11	2	156	3	t
IR	RNN	1	LowStringency	f	f	ENFSI	11	2	157	3	t
INN	RNN	1	HighStringency	f	f	ENFSI	11	2	158	3	t
ER	RNN	1	HighStringency	f	f	ENFSI	11	2	159	3	t
INNV	RNN	1	HighStringency	f	f	ENFSI	11	2	160	3	t
ENN	RNN	1	HighStringency	f	f	ENFSI	11	2	161	3	t
ER	ER	1	HighStringency	f	f	ENFSI	11	2	162	3	t
INN	INN	1	HighStringency	f	f	ENFSI	11	2	163	3	t
RNN	RNN	1	HighStringency	f	f	ENFSI	11	2	164	3	t
ENN	ENN	1	HighStringency	f	f	ENFSI	11	2	165	3	t
IR	ER	1	HighStringency	f	f	ENFSI	11	2	166	4	t
IR	ENN	1	HighStringency	f	f	ENFSI	11	2	167	4	t
ER	IR	1	HighStringency	f	f	ENFSI	11	2	168	4	t
ENN	IR	1	HighStringency	f	f	ENFSI	11	2	169	4	t
INN	IR	1	HighStringency	f	f	ENFSI	11	2	170	4	t
INN	ER	1	HighStringency	f	f	ENFSI	11	2	171	4	t
INN	INNV	1	HighStringency	f	f	ENFSI	11	2	172	4	t
INN	ENN	1	HighStringency	f	f	ENFSI	11	2	173	4	t
IR	INN	1	HighStringency	f	f	ENFSI	11	2	174	4	t
ER	INN	1	HighStringency	f	f	ENFSI	11	2	175	4	t
INNV	INN	1	HighStringency	f	f	ENFSI	11	2	176	4	t
ENN	INN	1	HighStringency	f	f	ENFSI	11	2	177	4	t
ER	ENN	1	HighStringency	f	f	ENFSI	11	2	178	4	t
ENN	ER	1	HighStringency	f	f	ENFSI	11	2	179	4	t
INNV	IR	1	HighStringency	f	f	ENFSI	11	2	180	4	t
INNV	ER	1	HighStringency	f	f	ENFSI	11	2	181	4	t
INNV	ENN	1	HighStringency	f	f	ENFSI	11	2	182	4	t
IR	INNV	1	HighStringency	f	f	ENFSI	11	2	183	4	t
ER	INNV	1	HighStringency	f	f	ENFSI	11	2	184	4	t
ENN	INNV	1	HighStringency	f	f	ENFSI	11	2	185	4	t
RNN	IR	1	HighStringency	f	f	ENFSI	11	2	186	4	t
RNN	INN	1	HighStringency	f	f	ENFSI	11	2	187	4	t
RNN	ER	1	HighStringency	f	f	ENFSI	11	2	188	4	t
RNN	INNV	1	HighStringency	f	f	ENFSI	11	2	189	4	t
RNN	ENN	1	HighStringency	f	f	ENFSI	11	2	190	4	t
IR	RNN	1	HighStringency	f	f	ENFSI	11	2	191	4	t
INN	RNN	1	HighStringency	f	f	ENFSI	11	2	192	4	t
ER	RNN	1	HighStringency	f	f	ENFSI	11	2	193	4	t
INNV	RNN	1	HighStringency	f	f	ENFSI	11	2	194	4	t
ENN	RNN	1	HighStringency	f	f	ENFSI	11	2	195	4	t
ER	ER	1	HighStringency	f	f	ENFSI	11	2	196	4	t
INN	INN	1	HighStringency	f	f	ENFSI	11	2	197	4	t
RNN	RNN	1	HighStringency	f	f	ENFSI	11	2	198	4	t
ENN	ENN	1	HighStringency	f	f	ENFSI	11	2	199	4	t
PFNI	IR	1	LowStringency	f	f	ENFSI	11	2	200	1	t
PFNI	ER	1	HighStringency	f	f	ENFSI	11	2	201	1	t
PFNI	INNV	1	HighStringency	f	f	ENFSI	11	2	202	1	t
PFNI	ENN	1	HighStringency	f	f	ENFSI	11	2	203	1	t
IR	PFNI	1	LowStringency	f	f	ENFSI	11	2	204	1	t
ER	PFNI	1	HighStringency	f	f	ENFSI	11	2	205	1	t
INNV	PFNI	1	HighStringency	f	f	ENFSI	11	2	206	1	t
ENN	PFNI	1	HighStringency	f	f	ENFSI	11	2	207	1	t
RNN	PFNI	1	HighStringency	f	f	ENFSI	11	2	208	1	t
PFNI	RNN	1	HighStringency	f	f	ENFSI	11	2	209	1	t
PFNI	PFNI	1	HighStringency	f	f	ENFSI	11	2	210	1	t
PFNI	IR	1	LowStringency	f	f	ENFSI	11	2	211	2	t
PFNI	ER	1	HighStringency	f	f	ENFSI	11	2	212	2	t
PFNI	INNV	1	HighStringency	f	f	ENFSI	11	2	213	2	t
PFNI	ENN	1	HighStringency	f	f	ENFSI	11	2	214	2	t
IR	PFNI	1	LowStringency	f	f	ENFSI	11	2	215	2	t
ER	PFNI	1	HighStringency	f	f	ENFSI	11	2	216	2	t
INNV	PFNI	1	HighStringency	f	f	ENFSI	11	2	217	2	t
ENN	PFNI	1	HighStringency	f	f	ENFSI	11	2	218	2	t
RNN	PFNI	1	HighStringency	f	f	ENFSI	11	2	219	2	t
PFNI	RNN	1	HighStringency	f	f	ENFSI	11	2	220	2	t
PFNI	PFNI	1	HighStringency	f	f	ENFSI	11	2	221	2	t
PFNI	IR	1	LowStringency	f	f	ENFSI	11	2	222	3	t
PFNI	ER	1	HighStringency	f	f	ENFSI	11	2	223	3	t
PFNI	INNV	1	HighStringency	f	f	ENFSI	11	2	224	3	t
PFNI	ENN	1	HighStringency	f	f	ENFSI	11	2	225	3	t
IR	PFNI	1	LowStringency	f	f	ENFSI	11	2	226	3	t
ER	PFNI	1	HighStringency	f	f	ENFSI	11	2	227	3	t
INNV	PFNI	1	HighStringency	f	f	ENFSI	11	2	228	3	t
ENN	PFNI	1	HighStringency	f	f	ENFSI	11	2	229	3	t
RNN	PFNI	1	HighStringency	f	f	ENFSI	11	2	230	3	t
PFNI	RNN	1	HighStringency	f	f	ENFSI	11	2	231	3	t
PFNI	PFNI	1	HighStringency	f	f	ENFSI	11	2	232	3	t
PFNI	ER	1	HighStringency	f	f	ENFSI	11	2	233	4	t
PFNI	INNV	1	HighStringency	f	f	ENFSI	11	2	234	4	t
PFNI	ENN	1	HighStringency	f	f	ENFSI	11	2	235	4	t
IR	PFNI	1	HighStringency	f	f	ENFSI	11	2	236	4	t
ER	PFNI	1	HighStringency	f	f	ENFSI	11	2	237	4	t
INNV	PFNI	1	HighStringency	f	f	ENFSI	11	2	238	4	t
ENN	PFNI	1	HighStringency	f	f	ENFSI	11	2	239	4	t
RNN	PFNI	1	HighStringency	f	f	ENFSI	11	2	240	4	t
PFNI	RNN	1	HighStringency	f	f	ENFSI	11	2	241	4	t
PFNI	PFNI	1	HighStringency	f	f	ENFSI	11	2	242	4	t
IR_DVI	ENN_DVI	1	LowStringency	f	f	ENFSI	11	2	243	1	t
ENN_DVI	IR_DVI	1	LowStringency	f	f	ENFSI	11	2	244	1	t
INN_DVI	IR_DVI	1	LowStringency	f	f	ENFSI	11	2	245	1	t
INN_DVI	ENN_DVI	1	HighStringency	f	f	ENFSI	11	2	246	1	t
IR_DVI	INN_DVI	1	LowStringency	f	f	ENFSI	11	2	247	1	t
ENN_DVI	INN_DVI	1	HighStringency	f	f	ENFSI	11	2	248	1	t
INN_DVI	INN_DVI	1	HighStringency	f	f	ENFSI	11	2	255	1	t
ENN_DVI	ENN_DVI	1	HighStringency	f	f	ENFSI	11	2	257	1	t
IR_DVI	ENN_DVI	1	LowStringency	f	f	ENFSI	11	2	265	2	t
ENN_DVI	IR_DVI	1	LowStringency	f	f	ENFSI	11	2	266	2	t
INN_DVI	IR_DVI	1	LowStringency	f	f	ENFSI	11	2	267	2	t
INN_DVI	ENN_DVI	1	HighStringency	f	f	ENFSI	11	2	268	2	t
IR_DVI	INN_DVI	1	LowStringency	f	f	ENFSI	11	2	269	2	t
ENN_DVI	INN_DVI	1	HighStringency	f	f	ENFSI	11	2	270	2	t
INN_DVI	INN_DVI	1	HighStringency	f	f	ENFSI	11	2	277	2	t
ENN_DVI	ENN_DVI	1	HighStringency	f	f	ENFSI	11	2	279	2	t
IR_DVI	ENN_DVI	1	LowStringency	f	f	ENFSI	11	2	287	3	t
INN_DVI	ENN_DVI	1	HighStringency	f	f	ENFSI	11	2	288	3	t
IR_DVI	INN_DVI	1	LowStringency	f	f	ENFSI	11	2	289	3	t
ENN_DVI	INN_DVI	1	HighStringency	f	f	ENFSI	11	2	290	3	t
INN_DVI	INN_DVI	1	HighStringency	f	f	ENFSI	11	2	297	3	t
ENN_DVI	ENN_DVI	1	HighStringency	f	f	ENFSI	11	2	299	3	t
IR_DVI	ENN_DVI	1	HighStringency	f	f	ENFSI	11	2	307	4	t
ENN_DVI	IR_DVI	1	HighStringency	f	f	ENFSI	11	2	308	4	t
INN_DVI	IR_DVI	1	HighStringency	f	f	ENFSI	11	2	309	4	t
INN_DVI	ENN_DVI	1	HighStringency	f	f	ENFSI	11	2	310	4	t
IR_DVI	INN_DVI	1	HighStringency	f	f	ENFSI	11	2	311	4	t
ENN_DVI	INN_DVI	1	HighStringency	f	f	ENFSI	11	2	312	4	t
IR_DVI	INNV	1	HighStringency	f	f	ENFSI	11	2	313	4	t
INN_DVI	INN_DVI	1	HighStringency	f	f	ENFSI	11	2	320	4	t
ENN_DVI	ENN_DVI	1	HighStringency	f	f	ENFSI	11	2	322	4	t
PFNI_DVI	IR_DVI	1	LowStringency	f	f	ENFSI	11	2	836	1	t
PFNI_DVI	ENN_DVI	1	HighStringency	f	f	ENFSI	11	2	837	1	t
PFNI_DVI	PFNI_DVI	1	HighStringency	f	f	ENFSI	11	2	839	1	t
PFNI_DVI	IR_DVI	1	LowStringency	f	f	ENFSI	11	2	840	2	t
PFNI_DVI	ENN_DVI	1	HighStringency	f	f	ENFSI	11	2	841	2	t
PFNI_DVI	PFNI_DVI	1	HighStringency	f	f	ENFSI	11	2	843	2	t
PFNI_DVI	IR_DVI	1	LowStringency	f	f	ENFSI	11	2	844	3	t
PFNI_DVI	ENN_DVI	1	HighStringency	f	f	ENFSI	11	2	845	3	t
PFNI_DVI	PFNI_DVI	1	HighStringency	f	f	ENFSI	11	2	847	3	t
PFNI_DVI	ENN_DVI	1	HighStringency	f	f	ENFSI	11	2	848	4	t
PFNI_DVI	PFNI_DVI	1	HighStringency	f	f	ENFSI	11	2	850	4	t
IR_DVI	PFNI_DVI	1	LowStringency	f	f	ENFSI	11	2	851	1	t
ENN_DVI	PFNI_DVI	1	HighStringency	f	f	ENFSI	11	2	852	1	t
IR_DVI	PFNI_DVI	1	LowStringency	f	f	ENFSI	11	2	854	2	t
ENN_DVI	PFNI_DVI	1	HighStringency	f	f	ENFSI	11	2	855	2	t
IR_DVI	PFNI_DVI	1	LowStringency	f	f	ENFSI	11	2	857	3	t
ENN_DVI	PFNI_DVI	1	HighStringency	f	f	ENFSI	11	2	858	3	t
ENN_DVI	PFNI_DVI	1	HighStringency	f	f	ENFSI	11	2	860	4	t
RNN_DVI	IR_DVI	1	LowStringency	f	f	ENFSI	11	2	862	1	t
RNN_DVI	INN_DVI	1	HighStringency	f	f	ENFSI	11	2	863	1	t
RNN_DVI	ENN_DVI	1	HighStringency	f	f	ENFSI	11	2	864	1	t
RNN_DVI	RNN_DVI	1	HighStringency	f	f	ENFSI	11	2	865	1	t
RNN_DVI	IR_DVI	1	LowStringency	f	f	ENFSI	11	2	866	2	t
RNN_DVI	INN_DVI	1	HighStringency	f	f	ENFSI	11	2	867	2	t
RNN_DVI	ENN_DVI	1	HighStringency	f	f	ENFSI	11	2	868	2	t
RNN_DVI	RNN_DVI	1	HighStringency	f	f	ENFSI	11	2	869	2	t
RNN_DVI	IR_DVI	1	LowStringency	f	f	ENFSI	11	2	870	3	t
RNN_DVI	INN_DVI	1	HighStringency	f	f	ENFSI	11	2	871	3	t
RNN_DVI	ENN_DVI	1	HighStringency	f	f	ENFSI	11	2	872	3	t
RNN_DVI	RNN_DVI	1	HighStringency	f	f	ENFSI	11	2	873	3	t
RNN_DVI	IR_DVI	1	HighStringency	f	f	ENFSI	11	2	874	4	t
RNN_DVI	INN_DVI	1	HighStringency	f	f	ENFSI	11	2	875	4	t
RNN_DVI	ENN_DVI	1	HighStringency	f	f	ENFSI	11	2	876	4	t
RNN_DVI	RNN_DVI	1	HighStringency	f	f	ENFSI	11	2	877	4	t
RNN_DVI	PFNI_DVI	1	HighStringency	f	f	ENFSI	11	2	878	1	t
RNN_DVI	PFNI_DVI	1	HighStringency	f	f	ENFSI	11	2	879	2	t
RNN_DVI	PFNI_DVI	1	HighStringency	f	f	ENFSI	11	2	880	3	t
RNN_DVI	PFNI_DVI	1	HighStringency	f	f	ENFSI	11	2	881	4	t
IR_DVI	RNN_DVI	1	LowStringency	f	f	ENFSI	11	2	882	1	t
INN_DVI	RNN_DVI	1	HighStringency	f	f	ENFSI	11	2	883	1	t
ENN_DVI	RNN_DVI	1	HighStringency	f	f	ENFSI	11	2	884	1	t
IR_DVI	RNN_DVI	1	LowStringency	f	f	ENFSI	11	2	885	2	t
INN_DVI	RNN_DVI	1	HighStringency	f	f	ENFSI	11	2	886	2	t
ENN_DVI	RNN_DVI	1	HighStringency	f	f	ENFSI	11	2	887	2	t
IR_DVI	RNN_DVI	1	LowStringency	f	f	ENFSI	11	2	888	3	t
INN_DVI	RNN_DVI	1	HighStringency	f	f	ENFSI	11	2	889	3	t
ENN_DVI	RNN_DVI	1	HighStringency	f	f	ENFSI	11	2	890	3	t
IR_DVI	RNN_DVI	1	HighStringency	f	f	ENFSI	11	2	891	4	t
INN_DVI	RNN_DVI	1	HighStringency	f	f	ENFSI	11	2	892	4	t
ENN_DVI	RNN_DVI	1	HighStringency	f	f	ENFSI	11	2	893	4	t
PFNI_DVI	RNN_DVI	1	HighStringency	f	f	ENFSI	11	2	894	1	t
PFNI_DVI	RNN_DVI	1	HighStringency	f	f	ENFSI	11	2	895	2	t
PFNI_DVI	RNN_DVI	1	HighStringency	f	f	ENFSI	11	2	896	3	t
PFNI_DVI	RNN_DVI	1	HighStringency	f	f	ENFSI	11	2	897	4	t
ForensicPartial	ForensicPartial	1	HighStringency	f	t	ENFSI	13	2	1076	1	t
ForensicPartial	MultiallelicOffender	1	ModerateStringency	f	f	ENFSI	13	2	1077	1	t
MultiallelicOffender	ForensicPartial	1	ModerateStringency	f	f	ENFSI	13	2	1082	1	t
ConvictedOffender	ForensicPartial	1	HighStringency	f	t	ENFSI	13	2	1151	1	t
ConvictedOffender	ConvictedOffender	1	HighStringency	f	t	ENFSI	13	1	1152	1	t
ConvictedOffender	MultiallelicOffender	1	ModerateStringency	f	t	ENFSI	13	2	1153	1	t
ForensicPartial	ConvictedOffender	1	HighStringency	f	t	ENFSI	13	2	1157	1	t
MultiallelicOffender	ConvictedOffender	1	ModerateStringency	f	t	ENFSI	13	2	1158	1	t
Suspect	Suspect	1	HighStringency	f	t	ENFSI	13	2	1407	1	t
Suspect	ForensicPartial	1	HighStringency	f	t	ENFSI	13	2	1408	1	t
Suspect	ConvictedOffender	1	HighStringency	f	t	ENFSI	13	2	1409	1	t
Suspect	ForensicUnknown	1	HighStringency	f	t	ENFSI	13	2	1410	1	t
Suspect	ForensicMixture	1	ModerateStringency	f	t	ENFSI	13	2	1411	1	t
Suspect	ForensicMixture	1	ModerateStringency	f	t	ENFSI	10	0	1412	3	t
ForensicPartial	Suspect	1	HighStringency	f	t	ENFSI	13	2	1413	1	t
ConvictedOffender	Suspect	1	HighStringency	f	t	ENFSI	13	2	1414	1	t
ForensicMixture	ForensicPartial	1	ModerateStringency	f	t	ENFSI	13	2	1356	1	t
ForensicMixture	MultiallelicOffender	1	ModerateStringency	f	t	ENFSI	13	2	1357	1	t
ForensicMixture	ForensicMixture	1	ModerateStringency	f	t	GENIS_MM	13	0	1358	1	t
ForensicMixture	ConvictedOffender	1	ModerateStringency	f	t	ENFSI	13	2	1359	1	t
ForensicUnknown	ForensicUnknown	1	HighStringency	f	t	ENFSI	13	2	1193	1	t
ForensicUnknown	MultiallelicOffender	1	ModerateStringency	f	t	ENFSI	13	2	1194	1	t
ForensicUnknown	ForensicPartial	1	HighStringency	f	t	ENFSI	13	2	1196	1	t
ForensicMixture	ForensicUnknown	1	ModerateStringency	f	t	ENFSI	13	2	1360	1	t
ForensicUnknown	ConvictedOffender	1	HighStringency	f	t	ENFSI	13	2	1198	1	t
ForensicUnknown	ForensicUnknown	1	HighStringency	f	t	ENFSI	10	0	1199	3	t
ForensicMixture	ForensicUnknown	1	ModerateStringency	f	t	ENFSI	10	0	1361	3	t
ForensicUnknown	ConvictedOffender	1	HighStringency	f	t	ENFSI	10	0	1201	3	t
MultiallelicOffender	ForensicUnknown	1	ModerateStringency	f	t	ENFSI	13	2	1202	1	t
ForensicPartial	ForensicUnknown	1	HighStringency	f	t	ENFSI	13	2	1204	1	t
ForensicMixture	ConvictedOffender	1	ModerateStringency	f	t	ENFSI	10	0	1362	3	t
ConvictedOffender	ForensicUnknown	1	HighStringency	f	t	ENFSI	13	2	1206	1	t
ForensicMixture	ForensicPartial	1	ModerateStringency	f	t	ENFSI	10	0	1363	3	t
ConvictedOffender	ForensicUnknown	1	HighStringency	f	t	ENFSI	10	0	1208	3	t
ForensicPartial	ForensicMixture	1	ModerateStringency	f	t	ENFSI	13	2	1366	1	t
MultiallelicOffender	ForensicMixture	1	ModerateStringency	f	t	ENFSI	13	2	1367	1	t
ConvictedOffender	ForensicMixture	1	ModerateStringency	f	t	ENFSI	13	2	1368	1	t
ForensicUnknown	ForensicMixture	1	ModerateStringency	f	t	ENFSI	13	2	1369	1	t
ForensicUnknown	ForensicMixture	1	ModerateStringency	f	t	ENFSI	10	0	1370	3	t
ConvictedOffender	ForensicMixture	1	ModerateStringency	f	t	ENFSI	10	0	1371	3	t
ForensicPartial	ForensicMixture	1	ModerateStringency	f	t	ENFSI	10	0	1372	3	t
ForensicUnknown	Suspect	1	HighStringency	f	t	ENFSI	13	2	1415	1	t
ForensicMixture	Suspect	1	ModerateStringency	f	t	ENFSI	13	2	1416	1	t
ForensicMixture	Suspect	1	ModerateStringency	f	t	ENFSI	10	0	1417	3	t
\.

--
-- Data for Name: COUNTRY; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--
COPY "APP"."COUNTRY" ("CODE", "NAME") FROM stdin;
AE	United Arab Emirates
\.

--
-- Data for Name: CRIME_TYPE; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."CRIME_TYPE" ("ID", "NAME", "DESCRIPTION") FROM stdin;
PUBLIC_ADM	Against Public Administration	\N
AGAINST_CIVIL	Against Civil Status	\N
AGAINST_FAITH	Against Public Faith	\N
HONOR	Against Honor	\N
AGAINST_SEX	Against Sexual Integrity	\N
AGAINST_FREEDOM	Against Freedom	\N
AGAINST_ECO	Against the Economic and Financial Order	\N
AGAINST_PUB	Against Public Order	\N
AGAINST_PEOPLE	Against People	\N
AGAINST_CONST	Against Public Powers and the Constitutional Order	\N
AGAINST_PROP	Against Property	\N
SEC_NATION	Against the Security of the Nation	\N
SEC_PUB	Against Public Safety	\N
\.

--
-- Data for Name: CRIME_INVOLVED; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."CRIME_INVOLVED" ("ID", "CRIME_TYPE", "NAME", "DESCRIPTION") FROM stdin;
MURDER	AGAINST_PEOPLE	Murder	\N
ABANDONMENT	AGAINST_PEOPLE	Abandonment of People	\N
ABUSE	AGAINST_SEX	Child Sexual Abuse	\N
ILLEGAL_MARRIAGE	AGAINST_CIVIL	Illegal Marriage	\N
SLAVERY	AGAINST_FREEDOM	Slavery	\N
DEPRIVATION	AGAINST_FREEDOM	Deprivation of liberty	\N
THEFT	AGAINST_PROP	Theft	\N
HEIST	AGAINST_PROP	Heist	\N
FIRE	SEC_PUB	Fire	\N
ILLICIT_ASSOCIATION	AGAINST_PUB	Illicit association	\N
TREASON	SEC_NATION	Treason	\N
RESISTANCE	PUBLIC_ADM	Resistance to Authority	\N
COUNTERFEITING	AGAINST_FAITH	Counterfeiting of Currency	\N
OPERATION	AGAINST_ECO	Operation with Illegal Goods	\N
\.

--
-- Data for Name: INFERIOR_INSTANCE_PROFILE_STATUS; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."INFERIOR_INSTANCE_PROFILE_STATUS" ("ID", "STATUS") FROM stdin;
1	Pending submission to superior instance
2	Sent to higher instance
3	Rejected by superior instance
4	Approved at a superior instance
5	Pending deletion in superior instance
6	Deletion in superior instance
7	Match pending submission to inferior instance
8	Match sent to inferior instance
9	Hit sent to inferior instance
10	Discard Sent to inferior instance
11	Hit pending sending to inferior instance
12	Discard pending submission to inferior instance
13	Delete pending submission to inferior instance
14	Delete Sent to inferior instance
15	Pending file to send
16	File Sent
\.

-- Data for Name: PROVINCE; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."PROVINCE" ("CODE", "NAME", "COUNTRY") FROM stdin;
A	Ajman	AE
B	Abu Dhabi	AE
D	Dubai	AE
F	Fujairah	AE
L	Al Ain	AE
R	Ras Al Khaimah	AE
S	Sharjah	AE
U	Umm Al Qaiwain	AE
\.

--
-- Data for Name: INSTANCE_STATUS; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."INSTANCE_STATUS" ("ID", "STATUS") FROM stdin;
1	Pending
2	Approved
3	Disapproved
\.

--
-- Data for Name: LOCUS; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."LOCUS" ("ID", "NAME", "CHROMOSOME", "MINIMUM_ALLELES_QTY", "MAXIMUM_ALLELES_QTY", "TYPE", "REQUIRED", "MIN_ALLELE_VALUE", "MAX_ALLELE_VALUE") FROM stdin;
HV2	Variaciones Rango2	MT	1	300	4	f	\N	\N
HV3	Variaciones Rango3	MT	1	300	4	f	\N	\N
HV4_RANGE	Rango 4	\N	2	2	4	f	0.0	99.0
HV4	Variaciones Rango4	MT	1	300	4	f	0.0	99.0
HV1_RANGE	Rango 1	\N	2	2	4	f	\N	\N
HV2_RANGE	Rango 2	\N	2	2	4	f	\N	\N
HV3_RANGE	Rango 3	\N	2	2	4	f	\N	\N
HV1	Variaciones Rango1	MT	1	300	4	f	\N	\N
D6S1043	D6S1043	6	2	4	1	f	7.0	25.0
PentaD	Penta D 	21	2	4	1	f	2.2	17.0
PentaE	Penta E	15	2	4	1	f	6.0	24.0
ACTBP2	ACTBP2 (SE33)	6	2	4	1	f	4.2	37.0
YIndel	YIndel	Y	1	1	1	f	1.0	2.0
DYS456	DYS456	Y	1	1	3	f	13.0	18.0
DYS458	DYS458	Y	1	1	3	f	14.0	20.0
DYS460	DYS460	Y	1	1	3	f	7.0	14.0
DYS481	DYS481	Y	1	1	3	f	17.0	32.0
DYS518	DYS518	Y	1	1	3	f	32.0	49.0
DYS533	DYS533	Y	1	1	3	f	7.0	17.0
DYS570	DYS570	Y	1	1	3	f	10.0	25.0
DYS549	DYS549	Y	1	1	3	f	7.0	17.0
DYS576	DYS576	Y	1	1	3	f	11.0	23.0
DYS627	DYS627	Y	1	1	3	f	11.0	27.0
DYS635	DYS635	Y	1	1	3	f	20.0	26.0
DYS643	DYS643	Y	1	1	3	f	6.0	17.0
DXS10134	DXS10134	X	1	2	2	f	0.0	99.0
DXS10135	DXS10135	X	1	2	2	f	0.0	99.0
DXS10146	DXS10146	X	1	2	2	f	0.0	99.0
DXS10148	DXS10148	X	1	2	2	f	0.0	99.0
DXS7132	DXS7132	X	1	2	2	f	0.0	99.0
DXS7423	DXS7423	X	1	2	2	f	0.0	99.0
DXS8378	DXS8378	X	1	2	2	f	0.0	99.0
YGATAH4	Y_GATA_H4	Y	1	1	3	f	8.0	13.0
DYS390	DYS390	Y	1	1	3	f	18.0	27.0
DYS392	DYS392	Y	1	1	3	f	7.0	18.0
DYS393	DYS393	Y	1	1	3	f	8.0	16.0
DYS437	DYS437	Y	1	1	3	f	13.0	17.0
DYS438	DYS438	Y	1	1	3	f	8.0	12.0
DYS439	DYS439	Y	1	1	3	f	8.0	15.0
DYS448	DYS448	Y	1	1	3	f	7.0	24.0
DYS449	DYS449	Y	1	1	3	f	22.0	40.0
HPRTB	HPRTB	X	1	2	2	f	0.0	99.0
DXS10074	DXS10074	X	1	2	2	f	0.0	99.0
DXS10079	DXS10079	X	1	2	2	f	0.0	99.0
DXS10101	DXS10101	X	1	2	2	f	0.0	99.0
DXS10103	DXS10103	X	1	2	2	f	0.0	99.0
D21S11	D21S11	21	2	4	1	f	24.2	38.0
D22S1045	D22S1045	22	2	4	1	f	8.0	19.0
D2S1338	D2S1338	2	2	4	1	f	15.0	28.0
D2S441	D2S441	2	2	4	1	f	9.0	16.0
D3S1358	D3S1358	3	2	4	1	f	12.0	19.0
D5S818	D5S818	5	2	4	1	f	7.0	16.0
D7S820	D7S820	7	2	4	1	f	6.0	14.0
D8S1179	D8S1179	8	2	4	1	f	8.0	18.0
DYS391	DYS391	Y	1	1	3	f	8.0	13.0
CSF1PO	CSF1PO	5	2	4	1	f	6.0	15.0
D10S1248	D10S1248	10	2	4	1	f	8.0	18.0
D12S391	D12S391	12	2	4	1	f	14.0	27.0
D13S317	D13S317	13	2	4	1	f	8.0	15.0
D16S539	D16S539	16	2	4	1	f	5.0	15.0
D18S51	D18S51	18	2	4	1	f	9.0	26.0
D19S433	D19S433	19	2	4	1	f	9.0	17.2
D1S1656	D1S1656	1	2	4	1	f	9.0	20.3
FGA	FIBRA (FGA)	4	2	4	1	f	18.0	30.0
TH01	TH01	11	2	4	1	f	5.0	10.0
TPOX	TPOX	2	2	4	1	f	6.0	13.0
vWA	vWA	12	2	4	1	f	11.0	21.0
DYS389I	DYS389I	Y	1	1	3	f	10.0	15.0
DYS389II	DYS389II	Y	1	1	3	f	24.0	34.0
AMEL	Amelogenin	XY	2	2	1	f	\N	\N
DYS385	DYS385	Y	1	3	3	f	7.0	25.0
DYS19	DYS19	Y	1	1	3	f	10.0	19.0
DYF387S1	DYF387S1	Y	1	3	3	f	30.0	44.0
\.

--
-- Data for Name: LOCUS_ALIAS; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."LOCUS_ALIAS" ("ALIAS", "MARKER") FROM stdin;
Penta E	PentaE
Penta_E	PentaE
Penta D	PentaD
Penta_D	PentaD
VWA	vWA
SE33	ACTBP2
Amelogenin	AMEL
DYS389 I	DYS389I
DYS389 II	DYS389II
DYS385ab	DYS385
DYS387S1	DYF387S1
\.

--
-- Data for Name: MITOCHONDRIAL_RCRS; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."MITOCHONDRIAL_RCRS" ("POSITION", "BASE") FROM stdin;
1	G
2	A
3	T
4	C
5	A
6	C
7	A
8	G
9	G
10	T
11	C
12	T
13	A
14	T
15	C
16	A
17	C
18	C
19	C
20	T
21	A
22	T
23	T
24	A
25	A
26	C
27	C
28	A
29	C
30	T
31	C
32	A
33	C
34	G
35	G
36	G
37	A
38	G
39	C
40	T
41	C
42	T
43	C
44	C
45	A
46	T
47	G
48	C
49	A
50	T
51	T
52	T
53	G
54	G
55	T
56	A
57	T
58	T
59	T
60	T
61	C
62	G
63	T
64	C
65	T
66	G
67	G
68	G
69	G
70	G
71	G
72	T
73	A
74	T
75	G
76	C
77	A
78	C
79	G
80	C
81	G
82	A
83	T
84	A
85	G
86	C
87	A
88	T
89	T
90	G
91	C
92	G
93	A
94	G
95	A
96	C
97	G
98	C
99	T
100	G
101	G
102	A
103	G
104	C
105	C
106	G
107	G
108	A
109	G
110	C
111	A
112	C
113	C
114	C
115	T
116	A
117	T
118	G
119	T
120	C
121	G
122	C
123	A
124	G
125	T
126	A
127	T
128	C
129	T
130	G
131	T
132	C
133	T
134	T
135	T
136	G
137	A
138	T
139	T
140	C
141	C
142	T
143	G
144	C
145	C
146	T
147	C
148	A
149	T
150	C
151	C
152	T
153	A
154	T
155	T
156	A
157	T
158	T
159	T
160	A
161	T
162	C
163	G
164	C
165	A
166	C
167	C
168	T
169	A
170	C
171	G
172	T
173	T
174	C
175	A
176	A
177	T
178	A
179	T
180	T
181	A
182	C
183	A
184	G
185	G
186	C
187	G
188	A
189	A
190	C
191	A
192	T
193	A
194	C
195	T
196	T
197	A
198	C
199	T
200	A
201	A
202	A
203	G
204	T
205	G
206	T
207	G
208	T
209	T
210	A
211	A
212	T
213	T
214	A
215	A
216	T
217	T
218	A
219	A
220	T
221	G
222	C
223	T
224	T
225	G
226	T
227	A
228	G
229	G
230	A
231	C
232	A
233	T
234	A
235	A
236	T
237	A
238	A
239	T
240	A
241	A
242	C
243	A
244	A
245	T
246	T
247	G
248	A
249	A
250	T
251	G
252	T
253	C
254	T
255	G
256	C
257	A
258	C
259	A
260	G
261	C
262	C
263	A
264	C
265	T
266	T
267	T
268	C
269	C
270	A
271	C
272	A
273	C
274	A
275	G
276	A
277	C
278	A
279	T
280	C
281	A
282	T
283	A
284	A
285	C
286	A
287	A
288	A
289	A
290	A
291	A
292	T
293	T
294	T
295	C
296	C
297	A
298	C
299	C
300	A
301	A
302	A
303	C
304	C
305	C
306	C
307	C
308	C
309	C
310	T
311	C
312	C
313	C
314	C
315	C
316	G
317	C
318	T
319	T
320	C
321	T
322	G
323	G
324	C
325	C
326	A
327	C
328	A
329	G
330	C
331	A
332	C
333	T
334	T
335	A
336	A
337	A
338	C
339	A
340	C
341	A
342	T
343	C
344	T
345	C
346	T
347	G
348	C
349	C
350	A
351	A
352	A
353	C
354	C
355	C
356	C
357	A
358	A
359	A
360	A
361	A
362	C
363	A
364	A
365	A
366	G
367	A
368	A
369	C
370	C
371	C
372	T
373	A
374	A
375	C
376	A
377	C
378	C
379	A
380	G
381	C
382	C
383	T
384	A
385	A
386	C
387	C
388	A
389	G
390	A
391	T
392	T
393	T
394	C
395	A
396	A
397	A
398	T
399	T
400	T
401	T
402	A
403	T
404	C
405	T
406	T
407	T
408	T
409	G
410	G
411	C
412	G
413	G
414	T
415	A
416	T
417	G
418	C
419	A
420	C
421	T
422	T
423	T
424	T
425	A
426	A
427	C
428	A
429	G
430	T
431	C
432	A
433	C
434	C
435	C
436	C
437	C
438	C
439	A
440	A
441	C
442	T
443	A
444	A
445	C
446	A
447	C
448	A
449	T
450	T
451	A
452	T
453	T
454	T
455	T
456	C
457	C
458	C
459	C
460	T
461	C
462	C
463	C
464	A
465	C
466	T
467	C
468	C
469	C
470	A
471	T
472	A
473	C
474	T
475	A
476	C
477	T
478	A
479	A
480	T
481	C
482	T
483	C
484	A
485	T
486	C
487	A
488	A
489	T
490	A
491	C
492	A
493	A
494	C
495	C
496	C
497	C
498	C
499	G
500	C
501	C
502	C
503	A
504	T
505	C
506	C
507	T
508	A
509	C
510	C
511	C
512	A
513	G
514	C
515	A
516	C
517	A
518	C
519	A
520	C
521	A
522	C
523	A
524	C
525	C
526	G
527	C
528	T
529	G
530	C
531	T
532	A
533	A
534	C
535	C
536	C
537	C
538	A
539	T
540	A
541	C
542	C
543	C
544	C
545	G
546	A
547	A
548	C
549	C
550	A
551	A
552	C
553	C
554	A
555	A
556	A
557	C
558	C
559	C
560	C
561	A
562	A
563	A
564	G
565	A
566	C
567	A
568	C
569	C
570	C
571	C
572	C
573	C
574	A
575	C
576	A
577	G
578	T
579	T
580	T
581	A
582	T
583	G
584	T
585	A
586	G
587	C
588	T
589	T
590	A
591	C
592	C
593	T
594	C
595	C
596	T
597	C
598	A
599	A
600	A
601	G
602	C
603	A
604	A
605	T
606	A
607	C
608	A
609	C
610	T
611	G
612	A
613	A
614	A
615	A
616	T
617	G
618	T
619	T
620	T
621	A
622	G
623	A
624	C
625	G
626	G
627	G
628	C
629	T
630	C
631	A
632	C
633	A
634	T
635	C
636	A
637	C
638	C
639	C
640	C
641	A
642	T
643	A
644	A
645	A
646	C
647	A
648	A
649	A
650	T
651	A
652	G
653	G
654	T
655	T
656	T
657	G
658	G
659	T
660	C
661	C
662	T
663	A
664	G
665	C
666	C
667	T
668	T
669	T
670	C
671	T
672	A
673	T
674	T
675	A
676	G
677	C
678	T
679	C
680	T
681	T
682	A
683	G
684	T
685	A
686	A
687	G
688	A
689	T
690	T
691	A
692	C
693	A
694	C
695	A
696	T
697	G
698	C
699	A
700	A
701	G
702	C
703	A
704	T
705	C
706	C
707	C
708	C
709	G
710	T
711	T
712	C
713	C
714	A
715	G
716	T
717	G
718	A
719	G
720	T
721	T
722	C
723	A
724	C
725	C
726	C
727	T
728	C
729	T
730	A
731	A
732	A
733	T
734	C
735	A
736	C
737	C
738	A
739	C
740	G
741	A
742	T
743	C
744	A
745	A
746	A
747	A
748	G
749	G
750	A
751	A
752	C
753	A
754	A
755	G
756	C
757	A
758	T
759	C
760	A
761	A
762	G
763	C
764	A
765	C
766	G
767	C
768	A
769	G
770	C
771	A
772	A
773	T
774	G
775	C
776	A
777	G
778	C
779	T
780	C
781	A
782	A
783	A
784	A
785	C
786	G
787	C
788	T
789	T
790	A
791	G
792	C
793	C
794	T
795	A
796	G
797	C
798	C
799	A
800	C
801	A
802	C
803	C
804	C
805	C
806	C
807	A
808	C
809	G
810	G
811	G
812	A
813	A
814	A
815	C
816	A
817	G
818	C
819	A
820	G
821	T
822	G
823	A
824	T
825	T
826	A
827	A
828	C
829	C
830	T
831	T
832	T
833	A
834	G
835	C
836	A
837	A
838	T
839	A
840	A
841	A
842	C
843	G
844	A
845	A
846	A
847	G
848	T
849	T
850	T
851	A
852	A
853	C
854	T
855	A
856	A
857	G
858	C
859	T
860	A
861	T
862	A
863	C
864	T
865	A
866	A
867	C
868	C
869	C
870	C
871	A
872	G
873	G
874	G
875	T
876	T
877	G
878	G
879	T
880	C
881	A
882	A
883	T
884	T
885	T
886	C
887	G
888	T
889	G
890	C
891	C
892	A
893	G
894	C
895	C
896	A
897	C
898	C
899	G
900	C
901	G
902	G
903	T
904	C
905	A
906	C
907	A
908	C
909	G
910	A
911	T
912	T
913	A
914	A
915	C
916	C
917	C
918	A
919	A
920	G
921	T
922	C
923	A
924	A
925	T
926	A
927	G
928	A
929	A
930	G
931	C
932	C
933	G
934	G
935	C
936	G
937	T
938	A
939	A
940	A
941	G
942	A
943	G
944	T
945	G
946	T
947	T
948	T
949	T
950	A
951	G
952	A
953	T
954	C
955	A
956	C
957	C
958	C
959	C
960	C
961	T
962	C
963	C
964	C
965	C
966	A
967	A
968	T
969	A
970	A
971	A
972	G
973	C
974	T
975	A
976	A
977	A
978	A
979	C
980	T
981	C
982	A
983	C
984	C
985	T
986	G
987	A
988	G
989	T
990	T
991	G
992	T
993	A
994	A
995	A
996	A
997	A
998	A
999	C
1000	T
1001	C
1002	C
1003	A
1004	G
1005	T
1006	T
1007	G
1008	A
1009	C
1010	A
1011	C
1012	A
1013	A
1014	A
1015	A
1016	T
1017	A
1018	G
1019	A
1020	C
1021	T
1022	A
1023	C
1024	G
1025	A
1026	A
1027	A
1028	G
1029	T
1030	G
1031	G
1032	C
1033	T
1034	T
1035	T
1036	A
1037	A
1038	C
1039	A
1040	T
1041	A
1042	T
1043	C
1044	T
1045	G
1046	A
1047	A
1048	C
1049	A
1050	C
1051	A
1052	C
1053	A
1054	A
1055	T
1056	A
1057	G
1058	C
1059	T
1060	A
1061	A
1062	G
1063	A
1064	C
1065	C
1066	C
1067	A
1068	A
1069	A
1070	C
1071	T
1072	G
1073	G
1074	G
1075	A
1076	T
1077	T
1078	A
1079	G
1080	A
1081	T
1082	A
1083	C
1084	C
1085	C
1086	C
1087	A
1088	C
1089	T
1090	A
1091	T
1092	G
1093	C
1094	T
1095	T
1096	A
1097	G
1098	C
1099	C
1100	C
1101	T
1102	A
1103	A
1104	A
1105	C
1106	C
1107	T
1108	C
1109	A
1110	A
1111	C
1112	A
1113	G
1114	T
1115	T
1116	A
1117	A
1118	A
1119	T
1120	C
1121	A
1122	A
1123	C
1124	A
1125	A
1126	A
1127	A
1128	C
1129	T
1130	G
1131	C
1132	T
1133	C
1134	G
1135	C
1136	C
1137	A
1138	G
1139	A
1140	A
1141	C
1142	A
1143	C
1144	T
1145	A
1146	C
1147	G
1148	A
1149	G
1150	C
1151	C
1152	A
1153	C
1154	A
1155	G
1156	C
1157	T
1158	T
1159	A
1160	A
1161	A
1162	A
1163	C
1164	T
1165	C
1166	A
1167	A
1168	A
1169	G
1170	G
1171	A
1172	C
1173	C
1174	T
1175	G
1176	G
1177	C
1178	G
1179	G
1180	T
1181	G
1182	C
1183	T
1184	T
1185	C
1186	A
1187	T
1188	A
1189	T
1190	C
1191	C
1192	C
1193	T
1194	C
1195	T
1196	A
1197	G
1198	A
1199	G
1200	G
1201	A
1202	G
1203	C
1204	C
1205	T
1206	G
1207	T
1208	T
1209	C
1210	T
1211	G
1212	T
1213	A
1214	A
1215	T
1216	C
1217	G
1218	A
1219	T
1220	A
1221	A
1222	A
1223	C
1224	C
1225	C
1226	C
1227	G
1228	A
1229	T
1230	C
1231	A
1232	A
1233	C
1234	C
1235	T
1236	C
1237	A
1238	C
1239	C
1240	A
1241	C
1242	C
1243	T
1244	C
1245	T
1246	T
1247	G
1248	C
1249	T
1250	C
1251	A
1252	G
1253	C
1254	C
1255	T
1256	A
1257	T
1258	A
1259	T
1260	A
1261	C
1262	C
1263	G
1264	C
1265	C
1266	A
1267	T
1268	C
1269	T
1270	T
1271	C
1272	A
1273	G
1274	C
1275	A
1276	A
1277	A
1278	C
1279	C
1280	C
1281	T
1282	G
1283	A
1284	T
1285	G
1286	A
1287	A
1288	G
1289	G
1290	C
1291	T
1292	A
1293	C
1294	A
1295	A
1296	A
1297	G
1298	T
1299	A
1300	A
1301	G
1302	C
1303	G
1304	C
1305	A
1306	A
1307	G
1308	T
1309	A
1310	C
1311	C
1312	C
1313	A
1314	C
1315	G
1316	T
1317	A
1318	A
1319	A
1320	G
1321	A
1322	C
1323	G
1324	T
1325	T
1326	A
1327	G
1328	G
1329	T
1330	C
1331	A
1332	A
1333	G
1334	G
1335	T
1336	G
1337	T
1338	A
1339	G
1340	C
1341	C
1342	C
1343	A
1344	T
1345	G
1346	A
1347	G
1348	G
1349	T
1350	G
1351	G
1352	C
1353	A
1354	A
1355	G
1356	A
1357	A
1358	A
1359	T
1360	G
1361	G
1362	G
1363	C
1364	T
1365	A
1366	C
1367	A
1368	T
1369	T
1370	T
1371	T
1372	C
1373	T
1374	A
1375	C
1376	C
1377	C
1378	C
1379	A
1380	G
1381	A
1382	A
1383	A
1384	A
1385	C
1386	T
1387	A
1388	C
1389	G
1390	A
1391	T
1392	A
1393	G
1394	C
1395	C
1396	C
1397	T
1398	T
1399	A
1400	T
1401	G
1402	A
1403	A
1404	A
1405	C
1406	T
1407	T
1408	A
1409	A
1410	G
1411	G
1412	G
1413	T
1414	C
1415	G
1416	A
1417	A
1418	G
1419	G
1420	T
1421	G
1422	G
1423	A
1424	T
1425	T
1426	T
1427	A
1428	G
1429	C
1430	A
1431	G
1432	T
1433	A
1434	A
1435	A
1436	C
1437	T
1438	A
1439	A
1440	G
1441	A
1442	G
1443	T
1444	A
1445	G
1446	A
1447	G
1448	T
1449	G
1450	C
1451	T
1452	T
1453	A
1454	G
1455	T
1456	T
1457	G
1458	A
1459	A
1460	C
1461	A
1462	G
1463	G
1464	G
1465	C
1466	C
1467	C
1468	T
1469	G
1470	A
1471	A
1472	G
1473	C
1474	G
1475	C
1476	G
1477	T
1478	A
1479	C
1480	A
1481	C
1482	A
1483	C
1484	C
1485	G
1486	C
1487	C
1488	C
1489	G
1490	T
1491	C
1492	A
1493	C
1494	C
1495	C
1496	T
1497	C
1498	C
1499	T
1500	C
1501	A
1502	A
1503	G
1504	T
1505	A
1506	T
1507	A
1508	C
1509	T
1510	T
1511	C
1512	A
1513	A
1514	A
1515	G
1516	G
1517	A
1518	C
1519	A
1520	T
1521	T
1522	T
1523	A
1524	A
1525	C
1526	T
1527	A
1528	A
1529	A
1530	A
1531	C
1532	C
1533	C
1534	C
1535	T
1536	A
1537	C
1538	G
1539	C
1540	A
1541	T
1542	T
1543	T
1544	A
1545	T
1546	A
1547	T
1548	A
1549	G
1550	A
1551	G
1552	G
1553	A
1554	G
1555	A
1556	C
1557	A
1558	A
1559	G
1560	T
1561	C
1562	G
1563	T
1564	A
1565	A
1566	C
1567	A
1568	T
1569	G
1570	G
1571	T
1572	A
1573	A
1574	G
1575	T
1576	G
1577	T
1578	A
1579	C
1580	T
1581	G
1582	G
1583	A
1584	A
1585	A
1586	G
1587	T
1588	G
1589	C
1590	A
1591	C
1592	T
1593	T
1594	G
1595	G
1596	A
1597	C
1598	G
1599	A
1600	A
1601	C
1602	C
1603	A
1604	G
1605	A
1606	G
1607	T
1608	G
1609	T
1610	A
1611	G
1612	C
1613	T
1614	T
1615	A
1616	A
1617	C
1618	A
1619	C
1620	A
1621	A
1622	A
1623	G
1624	C
1625	A
1626	C
1627	C
1628	C
1629	A
1630	A
1631	C
1632	T
1633	T
1634	A
1635	C
1636	A
1637	C
1638	T
1639	T
1640	A
1641	G
1642	G
1643	A
1644	G
1645	A
1646	T
1647	T
1648	T
1649	C
1650	A
1651	A
1652	C
1653	T
1654	T
1655	A
1656	A
1657	C
1658	T
1659	T
1660	G
1661	A
1662	C
1663	C
1664	G
1665	C
1666	T
1667	C
1668	T
1669	G
1670	A
1671	G
1672	C
1673	T
1674	A
1675	A
1676	A
1677	C
1678	C
1679	T
1680	A
1681	G
1682	C
1683	C
1684	C
1685	C
1686	A
1687	A
1688	A
1689	C
1690	C
1691	C
1692	A
1693	C
1694	T
1695	C
1696	C
1697	A
1698	C
1699	C
1700	T
1701	T
1702	A
1703	C
1704	T
1705	A
1706	C
1707	C
1708	A
1709	G
1710	A
1711	C
1712	A
1713	A
1714	C
1715	C
1716	T
1717	T
1718	A
1719	G
1720	C
1721	C
1722	A
1723	A
1724	A
1725	C
1726	C
1727	A
1728	T
1729	T
1730	T
1731	A
1732	C
1733	C
1734	C
1735	A
1736	A
1737	A
1738	T
1739	A
1740	A
1741	A
1742	G
1743	T
1744	A
1745	T
1746	A
1747	G
1748	G
1749	C
1750	G
1751	A
1752	T
1753	A
1754	G
1755	A
1756	A
1757	A
1758	T
1759	T
1760	G
1761	A
1762	A
1763	A
1764	C
1765	C
1766	T
1767	G
1768	G
1769	C
1770	G
1771	C
1772	A
1773	A
1774	T
1775	A
1776	G
1777	A
1778	T
1779	A
1780	T
1781	A
1782	G
1783	T
1784	A
1785	C
1786	C
1787	G
1788	C
1789	A
1790	A
1791	G
1792	G
1793	G
1794	A
1795	A
1796	A
1797	G
1798	A
1799	T
1800	G
1801	A
1802	A
1803	A
1804	A
1805	A
1806	T
1807	T
1808	A
1809	T
1810	A
1811	A
1812	C
1813	C
1814	A
1815	A
1816	G
1817	C
1818	A
1819	T
1820	A
1821	A
1822	T
1823	A
1824	T
1825	A
1826	G
1827	C
1828	A
1829	A
1830	G
1831	G
1832	A
1833	C
1834	T
1835	A
1836	A
1837	C
1838	C
1839	C
1840	C
1841	T
1842	A
1843	T
1844	A
1845	C
1846	C
1847	T
1848	T
1849	C
1850	T
1851	G
1852	C
1853	A
1854	T
1855	A
1856	A
1857	T
1858	G
1859	A
1860	A
1861	T
1862	T
1863	A
1864	A
1865	C
1866	T
1867	A
1868	G
1869	A
1870	A
1871	A
1872	T
1873	A
1874	A
1875	C
1876	T
1877	T
1878	T
1879	G
1880	C
1881	A
1882	A
1883	G
1884	G
1885	A
1886	G
1887	A
1888	G
1889	C
1890	C
1891	A
1892	A
1893	A
1894	G
1895	C
1896	T
1897	A
1898	A
1899	G
1900	A
1901	C
1902	C
1903	C
1904	C
1905	C
1906	G
1907	A
1908	A
1909	A
1910	C
1911	C
1912	A
1913	G
1914	A
1915	C
1916	G
1917	A
1918	G
1919	C
1920	T
1921	A
1922	C
1923	C
1924	T
1925	A
1926	A
1927	G
1928	A
1929	A
1930	C
1931	A
1932	G
1933	C
1934	T
1935	A
1936	A
1937	A
1938	A
1939	G
1940	A
1941	G
1942	C
1943	A
1944	C
1945	A
1946	C
1947	C
1948	C
1949	G
1950	T
1951	C
1952	T
1953	A
1954	T
1955	G
1956	T
1957	A
1958	G
1959	C
1960	A
1961	A
1962	A
1963	A
1964	T
1965	A
1966	G
1967	T
1968	G
1969	G
1970	G
1971	A
1972	A
1973	G
1974	A
1975	T
1976	T
1977	T
1978	A
1979	T
1980	A
1981	G
1982	G
1983	T
1984	A
1985	G
1986	A
1987	G
1988	G
1989	C
1990	G
1991	A
1992	C
1993	A
1994	A
1995	A
1996	C
1997	C
1998	T
1999	A
2000	C
2001	C
2002	G
2003	A
2004	G
2005	C
2006	C
2007	T
2008	G
2009	G
2010	T
2011	G
2012	A
2013	T
2014	A
2015	G
2016	C
2017	T
2018	G
2019	G
2020	T
2021	T
2022	G
2023	T
2024	C
2025	C
2026	A
2027	A
2028	G
2029	A
2030	T
2031	A
2032	G
2033	A
2034	A
2035	T
2036	C
2037	T
2038	T
2039	A
2040	G
2041	T
2042	T
2043	C
2044	A
2045	A
2046	C
2047	T
2048	T
2049	T
2050	A
2051	A
2052	A
2053	T
2054	T
2055	T
2056	G
2057	C
2058	C
2059	C
2060	A
2061	C
2062	A
2063	G
2064	A
2065	A
2066	C
2067	C
2068	C
2069	T
2070	C
2071	T
2072	A
2073	A
2074	A
2075	T
2076	C
2077	C
2078	C
2079	C
2080	T
2081	T
2082	G
2083	T
2084	A
2085	A
2086	A
2087	T
2088	T
2089	T
2090	A
2091	A
2092	C
2093	T
2094	G
2095	T
2096	T
2097	A
2098	G
2099	T
2100	C
2101	C
2102	A
2103	A
2104	A
2105	G
2106	A
2107	G
2108	G
2109	A
2110	A
2111	C
2112	A
2113	G
2114	C
2115	T
2116	C
2117	T
2118	T
2119	T
2120	G
2121	G
2122	A
2123	C
2124	A
2125	C
2126	T
2127	A
2128	G
2129	G
2130	A
2131	A
2132	A
2133	A
2134	A
2135	A
2136	C
2137	C
2138	T
2139	T
2140	G
2141	T
2142	A
2143	G
2144	A
2145	G
2146	A
2147	G
2148	A
2149	G
2150	T
2151	A
2152	A
2153	A
2154	A
2155	A
2156	A
2157	T
2158	T
2159	T
2160	A
2161	A
2162	C
2163	A
2164	C
2165	C
2166	C
2167	A
2168	T
2169	A
2170	G
2171	T
2172	A
2173	G
2174	G
2175	C
2176	C
2177	T
2178	A
2179	A
2180	A
2181	A
2182	G
2183	C
2184	A
2185	G
2186	C
2187	C
2188	A
2189	C
2190	C
2191	A
2192	A
2193	T
2194	T
2195	A
2196	A
2197	G
2198	A
2199	A
2200	A
2201	G
2202	C
2203	G
2204	T
2205	T
2206	C
2207	A
2208	A
2209	G
2210	C
2211	T
2212	C
2213	A
2214	A
2215	C
2216	A
2217	C
2218	C
2219	C
2220	A
2221	C
2222	T
2223	A
2224	C
2225	C
2226	T
2227	A
2228	A
2229	A
2230	A
2231	A
2232	A
2233	T
2234	C
2235	C
2236	C
2237	A
2238	A
2239	A
2240	C
2241	A
2242	T
2243	A
2244	T
2245	A
2246	A
2247	C
2248	T
2249	G
2250	A
2251	A
2252	C
2253	T
2254	C
2255	C
2256	T
2257	C
2258	A
2259	C
2260	A
2261	C
2262	C
2263	C
2264	A
2265	A
2266	T
2267	T
2268	G
2269	G
2270	A
2271	C
2272	C
2273	A
2274	A
2275	T
2276	C
2277	T
2278	A
2279	T
2280	C
2281	A
2282	C
2283	C
2284	C
2285	T
2286	A
2287	T
2288	A
2289	G
2290	A
2291	A
2292	G
2293	A
2294	A
2295	C
2296	T
2297	A
2298	A
2299	T
2300	G
2301	T
2302	T
2303	A
2304	G
2305	T
2306	A
2307	T
2308	A
2309	A
2310	G
2311	T
2312	A
2313	A
2314	C
2315	A
2316	T
2317	G
2318	A
2319	A
2320	A
2321	A
2322	C
2323	A
2324	T
2325	T
2326	C
2327	T
2328	C
2329	C
2330	T
2331	C
2332	C
2333	G
2334	C
2335	A
2336	T
2337	A
2338	A
2339	G
2340	C
2341	C
2342	T
2343	G
2344	C
2345	G
2346	T
2347	C
2348	A
2349	G
2350	A
2351	T
2352	T
2353	A
2354	A
2355	A
2356	A
2357	C
2358	A
2359	C
2360	T
2361	G
2362	A
2363	A
2364	C
2365	T
2366	G
2367	A
2368	C
2369	A
2370	A
2371	T
2372	T
2373	A
2374	A
2375	C
2376	A
2377	G
2378	C
2379	C
2380	C
2381	A
2382	A
2383	T
2384	A
2385	T
2386	C
2387	T
2388	A
2389	C
2390	A
2391	A
2392	T
2393	C
2394	A
2395	A
2396	C
2397	C
2398	A
2399	A
2400	C
2401	A
2402	A
2403	G
2404	T
2405	C
2406	A
2407	T
2408	T
2409	A
2410	T
2411	T
2412	A
2413	C
2414	C
2415	C
2416	T
2417	C
2418	A
2419	C
2420	T
2421	G
2422	T
2423	C
2424	A
2425	A
2426	C
2427	C
2428	C
2429	A
2430	A
2431	C
2432	A
2433	C
2434	A
2435	G
2436	G
2437	C
2438	A
2439	T
2440	G
2441	C
2442	T
2443	C
2444	A
2445	T
2446	A
2447	A
2448	G
2449	G
2450	A
2451	A
2452	A
2453	G
2454	G
2455	T
2456	T
2457	A
2458	A
2459	A
2460	A
2461	A
2462	A
2463	A
2464	G
2465	T
2466	A
2467	A
2468	A
2469	A
2470	G
2471	G
2472	A
2473	A
2474	C
2475	T
2476	C
2477	G
2478	G
2479	C
2480	A
2481	A
2482	A
2483	T
2484	C
2485	T
2486	T
2487	A
2488	C
2489	C
2490	C
2491	C
2492	G
2493	C
2494	C
2495	T
2496	G
2497	T
2498	T
2499	T
2500	A
2501	C
2502	C
2503	A
2504	A
2505	A
2506	A
2507	A
2508	C
2509	A
2510	T
2511	C
2512	A
2513	C
2514	C
2515	T
2516	C
2517	T
2518	A
2519	G
2520	C
2521	A
2522	T
2523	C
2524	A
2525	C
2526	C
2527	A
2528	G
2529	T
2530	A
2531	T
2532	T
2533	A
2534	G
2535	A
2536	G
2537	G
2538	C
2539	A
2540	C
2541	C
2542	G
2543	C
2544	C
2545	T
2546	G
2547	C
2548	C
2549	C
2550	A
2551	G
2552	T
2553	G
2554	A
2555	C
2556	A
2557	C
2558	A
2559	T
2560	G
2561	T
2562	T
2563	T
2564	A
2565	A
2566	C
2567	G
2568	G
2569	C
2570	C
2571	G
2572	C
2573	G
2574	G
2575	T
2576	A
2577	C
2578	C
2579	C
2580	T
2581	A
2582	A
2583	C
2584	C
2585	G
2586	T
2587	G
2588	C
2589	A
2590	A
2591	A
2592	G
2593	G
2594	T
2595	A
2596	G
2597	C
2598	A
2599	T
2600	A
2601	A
2602	T
2603	C
2604	A
2605	C
2606	T
2607	T
2608	G
2609	T
2610	T
2611	C
2612	C
2613	T
2614	T
2615	A
2616	A
2617	A
2618	T
2619	A
2620	G
2621	G
2622	G
2623	A
2624	C
2625	C
2626	T
2627	G
2628	T
2629	A
2630	T
2631	G
2632	A
2633	A
2634	T
2635	G
2636	G
2637	C
2638	T
2639	C
2640	C
2641	A
2642	C
2643	G
2644	A
2645	G
2646	G
2647	G
2648	T
2649	T
2650	C
2651	A
2652	G
2653	C
2654	T
2655	G
2656	T
2657	C
2658	T
2659	C
2660	T
2661	T
2662	A
2663	C
2664	T
2665	T
2666	T
2667	T
2668	A
2669	A
2670	C
2671	C
2672	A
2673	G
2674	T
2675	G
2676	A
2677	A
2678	A
2679	T
2680	T
2681	G
2682	A
2683	C
2684	C
2685	T
2686	G
2687	C
2688	C
2689	C
2690	G
2691	T
2692	G
2693	A
2694	A
2695	G
2696	A
2697	G
2698	G
2699	C
2700	G
2701	G
2702	G
2703	C
2704	A
2705	T
2706	A
2707	A
2708	C
2709	A
2710	C
2711	A
2712	G
2713	C
2714	A
2715	A
2716	G
2717	A
2718	C
2719	G
2720	A
2721	G
2722	A
2723	A
2724	G
2725	A
2726	C
2727	C
2728	C
2729	T
2730	A
2731	T
2732	G
2733	G
2734	A
2735	G
2736	C
2737	T
2738	T
2739	T
2740	A
2741	A
2742	T
2743	T
2744	T
2745	A
2746	T
2747	T
2748	A
2749	A
2750	T
2751	G
2752	C
2753	A
2754	A
2755	A
2756	C
2757	A
2758	G
2759	T
2760	A
2761	C
2762	C
2763	T
2764	A
2765	A
2766	C
2767	A
2768	A
2769	A
2770	C
2771	C
2772	C
2773	A
2774	C
2775	A
2776	G
2777	G
2778	T
2779	C
2780	C
2781	T
2782	A
2783	A
2784	A
2785	C
2786	T
2787	A
2788	C
2789	C
2790	A
2791	A
2792	A
2793	C
2794	C
2795	T
2796	G
2797	C
2798	A
2799	T
2800	T
2801	A
2802	A
2803	A
2804	A
2805	A
2806	T
2807	T
2808	T
2809	C
2810	G
2811	G
2812	T
2813	T
2814	G
2815	G
2816	G
2817	G
2818	C
2819	G
2820	A
2821	C
2822	C
2823	T
2824	C
2825	G
2826	G
2827	A
2828	G
2829	C
2830	A
2831	G
2832	A
2833	A
2834	C
2835	C
2836	C
2837	A
2838	A
2839	C
2840	C
2841	T
2842	C
2843	C
2844	G
2845	A
2846	G
2847	C
2848	A
2849	G
2850	T
2851	A
2852	C
2853	A
2854	T
2855	G
2856	C
2857	T
2858	A
2859	A
2860	G
2861	A
2862	C
2863	T
2864	T
2865	C
2866	A
2867	C
2868	C
2869	A
2870	G
2871	T
2872	C
2873	A
2874	A
2875	A
2876	G
2877	C
2878	G
2879	A
2880	A
2881	C
2882	T
2883	A
2884	C
2885	T
2886	A
2887	T
2888	A
2889	C
2890	T
2891	C
2892	A
2893	A
2894	T
2895	T
2896	G
2897	A
2898	T
2899	C
2900	C
2901	A
2902	A
2903	T
2904	A
2905	A
2906	C
2907	T
2908	T
2909	G
2910	A
2911	C
2912	C
2913	A
2914	A
2915	C
2916	G
2917	G
2918	A
2919	A
2920	C
2921	A
2922	A
2923	G
2924	T
2925	T
2926	A
2927	C
2928	C
2929	C
2930	T
2931	A
2932	G
2933	G
2934	G
2935	A
2936	T
2937	A
2938	A
2939	C
2940	A
2941	G
2942	C
2943	G
2944	C
2945	A
2946	A
2947	T
2948	C
2949	C
2950	T
2951	A
2952	T
2953	T
2954	C
2955	T
2956	A
2957	G
2958	A
2959	G
2960	T
2961	C
2962	C
2963	A
2964	T
2965	A
2966	T
2967	C
2968	A
2969	A
2970	C
2971	A
2972	A
2973	T
2974	A
2975	G
2976	G
2977	G
2978	T
2979	T
2980	T
2981	A
2982	C
2983	G
2984	A
2985	C
2986	C
2987	T
2988	C
2989	G
2990	A
2991	T
2992	G
2993	T
2994	T
2995	G
2996	G
2997	A
2998	T
2999	C
3000	A
3001	G
3002	G
3003	A
3004	C
3005	A
3006	T
3007	C
3008	C
3009	C
3010	G
3011	A
3012	T
3013	G
3014	G
3015	T
3016	G
3017	C
3018	A
3019	G
3020	C
3021	C
3022	G
3023	C
3024	T
3025	A
3026	T
3027	T
3028	A
3029	A
3030	A
3031	G
3032	G
3033	T
3034	T
3035	C
3036	G
3037	T
3038	T
3039	T
3040	G
3041	T
3042	T
3043	C
3044	A
3045	A
3046	C
3047	G
3048	A
3049	T
3050	T
3051	A
3052	A
3053	A
3054	G
3055	T
3056	C
3057	C
3058	T
3059	A
3060	C
3061	G
3062	T
3063	G
3064	A
3065	T
3066	C
3067	T
3068	G
3069	A
3070	G
3071	T
3072	T
3073	C
3074	A
3075	G
3076	A
3077	C
3078	C
3079	G
3080	G
3081	A
3082	G
3083	T
3084	A
3085	A
3086	T
3087	C
3088	C
3089	A
3090	G
3091	G
3092	T
3093	C
3094	G
3095	G
3096	T
3097	T
3098	T
3099	C
3100	T
3101	A
3102	T
3103	C
3104	T
3105	A
3106	C
3107	N
3108	T
3109	T
3110	C
3111	A
3112	A
3113	A
3114	T
3115	T
3116	C
3117	C
3118	T
3119	C
3120	C
3121	C
3122	T
3123	G
3124	T
3125	A
3126	C
3127	G
3128	A
3129	A
3130	A
3131	G
3132	G
3133	A
3134	C
3135	A
3136	A
3137	G
3138	A
3139	G
3140	A
3141	A
3142	A
3143	T
3144	A
3145	A
3146	G
3147	G
3148	C
3149	C
3150	T
3151	A
3152	C
3153	T
3154	T
3155	C
3156	A
3157	C
3158	A
3159	A
3160	A
3161	G
3162	C
3163	G
3164	C
3165	C
3166	T
3167	T
3168	C
3169	C
3170	C
3171	C
3172	C
3173	G
3174	T
3175	A
3176	A
3177	A
3178	T
3179	G
3180	A
3181	T
3182	A
3183	T
3184	C
3185	A
3186	T
3187	C
3188	T
3189	C
3190	A
3191	A
3192	C
3193	T
3194	T
3195	A
3196	G
3197	T
3198	A
3199	T
3200	T
3201	A
3202	T
3203	A
3204	C
3205	C
3206	C
3207	A
3208	C
3209	A
3210	C
3211	C
3212	C
3213	A
3214	C
3215	C
3216	C
3217	A
3218	A
3219	G
3220	A
3221	A
3222	C
3223	A
3224	G
3225	G
3226	G
3227	T
3228	T
3229	T
3230	G
3231	T
3232	T
3233	A
3234	A
3235	G
3236	A
3237	T
3238	G
3239	G
3240	C
3241	A
3242	G
3243	A
3244	G
3245	C
3246	C
3247	C
3248	G
3249	G
3250	T
3251	A
3252	A
3253	T
3254	C
3255	G
3256	C
3257	A
3258	T
3259	A
3260	A
3261	A
3262	A
3263	C
3264	T
3265	T
3266	A
3267	A
3268	A
3269	A
3270	C
3271	T
3272	T
3273	T
3274	A
3275	C
3276	A
3277	G
3278	T
3279	C
3280	A
3281	G
3282	A
3283	G
3284	G
3285	T
3286	T
3287	C
3288	A
3289	A
3290	T
3291	T
3292	C
3293	C
3294	T
3295	C
3296	T
3297	T
3298	C
3299	T
3300	T
3301	A
3302	A
3303	C
3304	A
3305	A
3306	C
3307	A
3308	T
3309	A
3310	C
3311	C
3312	C
3313	A
3314	T
3315	G
3316	G
3317	C
3318	C
3319	A
3320	A
3321	C
3322	C
3323	T
3324	C
3325	C
3326	T
3327	A
3328	C
3329	T
3330	C
3331	C
3332	T
3333	C
3334	A
3335	T
3336	T
3337	G
3338	T
3339	A
3340	C
3341	C
3342	C
3343	A
3344	T
3345	T
3346	C
3347	T
3348	A
3349	A
3350	T
3351	C
3352	G
3353	C
3354	A
3355	A
3356	T
3357	G
3358	G
3359	C
3360	A
3361	T
3362	T
3363	C
3364	C
3365	T
3366	A
3367	A
3368	T
3369	G
3370	C
3371	T
3372	T
3373	A
3374	C
3375	C
3376	G
3377	A
3378	A
3379	C
3380	G
3381	A
3382	A
3383	A
3384	A
3385	A
3386	T
3387	T
3388	C
3389	T
3390	A
3391	G
3392	G
3393	C
3394	T
3395	A
3396	T
3397	A
3398	T
3399	A
3400	C
3401	A
3402	A
3403	C
3404	T
3405	A
3406	C
3407	G
3408	C
3409	A
3410	A
3411	A
3412	G
3413	G
3414	C
3415	C
3416	C
3417	C
3418	A
3419	A
3420	C
3421	G
3422	T
3423	T
3424	G
3425	T
3426	A
3427	G
3428	G
3429	C
3430	C
3431	C
3432	C
3433	T
3434	A
3435	C
3436	G
3437	G
3438	G
3439	C
3440	T
3441	A
3442	C
3443	T
3444	A
3445	C
3446	A
3447	A
3448	C
3449	C
3450	C
3451	T
3452	T
3453	C
3454	G
3455	C
3456	T
3457	G
3458	A
3459	C
3460	G
3461	C
3462	C
3463	A
3464	T
3465	A
3466	A
3467	A
3468	A
3469	C
3470	T
3471	C
3472	T
3473	T
3474	C
3475	A
3476	C
3477	C
3478	A
3479	A
3480	A
3481	G
3482	A
3483	G
3484	C
3485	C
3486	C
3487	C
3488	T
3489	A
3490	A
3491	A
3492	A
3493	C
3494	C
3495	C
3496	G
3497	C
3498	C
3499	A
3500	C
3501	A
3502	T
3503	C
3504	T
3505	A
3506	C
3507	C
3508	A
3509	T
3510	C
3511	A
3512	C
3513	C
3514	C
3515	T
3516	C
3517	T
3518	A
3519	C
3520	A
3521	T
3522	C
3523	A
3524	C
3525	C
3526	G
3527	C
3528	C
3529	C
3530	C
3531	G
3532	A
3533	C
3534	C
3535	T
3536	T
3537	A
3538	G
3539	C
3540	T
3541	C
3542	T
3543	C
3544	A
3545	C
3546	C
3547	A
3548	T
3549	C
3550	G
3551	C
3552	T
3553	C
3554	T
3555	T
3556	C
3557	T
3558	A
3559	C
3560	T
3561	A
3562	T
3563	G
3564	A
3565	A
3566	C
3567	C
3568	C
3569	C
3570	C
3571	C
3572	T
3573	C
3574	C
3575	C
3576	C
3577	A
3578	T
3579	A
3580	C
3581	C
3582	C
3583	A
3584	A
3585	C
3586	C
3587	C
3588	C
3589	C
3590	T
3591	G
3592	G
3593	T
3594	C
3595	A
3596	A
3597	C
3598	C
3599	T
3600	C
3601	A
3602	A
3603	C
3604	C
3605	T
3606	A
3607	G
3608	G
3609	C
3610	C
3611	T
3612	C
3613	C
3614	T
3615	A
3616	T
3617	T
3618	T
3619	A
3620	T
3621	T
3622	C
3623	T
3624	A
3625	G
3626	C
3627	C
3628	A
3629	C
3630	C
3631	T
3632	C
3633	T
3634	A
3635	G
3636	C
3637	C
3638	T
3639	A
3640	G
3641	C
3642	C
3643	G
3644	T
3645	T
3646	T
3647	A
3648	C
3649	T
3650	C
3651	A
3652	A
3653	T
3654	C
3655	C
3656	T
3657	C
3658	T
3659	G
3660	A
3661	T
3662	C
3663	A
3664	G
3665	G
3666	G
3667	T
3668	G
3669	A
3670	G
3671	C
3672	A
3673	T
3674	C
3675	A
3676	A
3677	A
3678	C
3679	T
3680	C
3681	A
3682	A
3683	A
3684	C
3685	T
3686	A
3687	C
3688	G
3689	C
3690	C
3691	C
3692	T
3693	G
3694	A
3695	T
3696	C
3697	G
3698	G
3699	C
3700	G
3701	C
3702	A
3703	C
3704	T
3705	G
3706	C
3707	G
3708	A
3709	G
3710	C
3711	A
3712	G
3713	T
3714	A
3715	G
3716	C
3717	C
3718	C
3719	A
3720	A
3721	A
3722	C
3723	A
3724	A
3725	T
3726	C
3727	T
3728	C
3729	A
3730	T
3731	A
3732	T
3733	G
3734	A
3735	A
3736	G
3737	T
3738	C
3739	A
3740	C
3741	C
3742	C
3743	T
3744	A
3745	G
3746	C
3747	C
3748	A
3749	T
3750	C
3751	A
3752	T
3753	T
3754	C
3755	T
3756	A
3757	C
3758	T
3759	A
3760	T
3761	C
3762	A
3763	A
3764	C
3765	A
3766	T
3767	T
3768	A
3769	C
3770	T
3771	A
3772	A
3773	T
3774	A
3775	A
3776	G
3777	T
3778	G
3779	G
3780	C
3781	T
3782	C
3783	C
3784	T
3785	T
3786	T
3787	A
3788	A
3789	C
3790	C
3791	T
3792	C
3793	T
3794	C
3795	C
3796	A
3797	C
3798	C
3799	C
3800	T
3801	T
3802	A
3803	T
3804	C
3805	A
3806	C
3807	A
3808	A
3809	C
3810	A
3811	C
3812	A
3813	A
3814	G
3815	A
3816	A
3817	C
3818	A
3819	C
3820	C
3821	T
3822	C
3823	T
3824	G
3825	A
3826	T
3827	T
3828	A
3829	C
3830	T
3831	C
3832	C
3833	T
3834	G
3835	C
3836	C
3837	A
3838	T
3839	C
3840	A
3841	T
3842	G
3843	A
3844	C
3845	C
3846	C
3847	T
3848	T
3849	G
3850	G
3851	C
3852	C
3853	A
3854	T
3855	A
3856	A
3857	T
3858	A
3859	T
3860	G
3861	A
3862	T
3863	T
3864	T
3865	A
3866	T
3867	C
3868	T
3869	C
3870	C
3871	A
3872	C
3873	A
3874	C
3875	T
3876	A
3877	G
3878	C
3879	A
3880	G
3881	A
3882	G
3883	A
3884	C
3885	C
3886	A
3887	A
3888	C
3889	C
3890	G
3891	A
3892	A
3893	C
3894	C
3895	C
3896	C
3897	C
3898	T
3899	T
3900	C
3901	G
3902	A
3903	C
3904	C
3905	T
3906	T
3907	G
3908	C
3909	C
3910	G
3911	A
3912	A
3913	G
3914	G
3915	G
3916	G
3917	A
3918	G
3919	T
3920	C
3921	C
3922	G
3923	A
3924	A
3925	C
3926	T
3927	A
3928	G
3929	T
3930	C
3931	T
3932	C
3933	A
3934	G
3935	G
3936	C
3937	T
3938	T
3939	C
3940	A
3941	A
3942	C
3943	A
3944	T
3945	C
3946	G
3947	A
3948	A
3949	T
3950	A
3951	C
3952	G
3953	C
3954	C
3955	G
3956	C
3957	A
3958	G
3959	G
3960	C
3961	C
3962	C
3963	C
3964	T
3965	T
3966	C
3967	G
3968	C
3969	C
3970	C
3971	T
3972	A
3973	T
3974	T
3975	C
3976	T
3977	T
3978	C
3979	A
3980	T
3981	A
3982	G
3983	C
3984	C
3985	G
3986	A
3987	A
3988	T
3989	A
3990	C
3991	A
3992	C
3993	A
3994	A
3995	A
3996	C
3997	A
3998	T
3999	T
4000	A
4001	T
4002	T
4003	A
4004	T
4005	A
4006	A
4007	T
4008	A
4009	A
4010	A
4011	C
4012	A
4013	C
4014	C
4015	C
4016	T
4017	C
4018	A
4019	C
4020	C
4021	A
4022	C
4023	T
4024	A
4025	C
4026	A
4027	A
4028	T
4029	C
4030	T
4031	T
4032	C
4033	C
4034	T
4035	A
4036	G
4037	G
4038	A
4039	A
4040	C
4041	A
4042	A
4043	C
4044	A
4045	T
4046	A
4047	T
4048	G
4049	A
4050	C
4051	G
4052	C
4053	A
4054	C
4055	T
4056	C
4057	T
4058	C
4059	C
4060	C
4061	C
4062	T
4063	G
4064	A
4065	A
4066	C
4067	T
4068	C
4069	T
4070	A
4071	C
4072	A
4073	C
4074	A
4075	A
4076	C
4077	A
4078	T
4079	A
4080	T
4081	T
4082	T
4083	T
4084	G
4085	T
4086	C
4087	A
4088	C
4089	C
4090	A
4091	A
4092	G
4093	A
4094	C
4095	C
4096	C
4097	T
4098	A
4099	C
4100	T
4101	T
4102	C
4103	T
4104	A
4105	A
4106	C
4107	C
4108	T
4109	C
4110	C
4111	C
4112	T
4113	G
4114	T
4115	T
4116	C
4117	T
4118	T
4119	A
4120	T
4121	G
4122	A
4123	A
4124	T
4125	T
4126	C
4127	G
4128	A
4129	A
4130	C
4131	A
4132	G
4133	C
4134	A
4135	T
4136	A
4137	C
4138	C
4139	C
4140	C
4141	C
4142	G
4143	A
4144	T
4145	T
4146	C
4147	C
4148	G
4149	C
4150	T
4151	A
4152	C
4153	G
4154	A
4155	C
4156	C
4157	A
4158	A
4159	C
4160	T
4161	C
4162	A
4163	T
4164	A
4165	C
4166	A
4167	C
4168	C
4169	T
4170	C
4171	C
4172	T
4173	A
4174	T
4175	G
4176	A
4177	A
4178	A
4179	A
4180	A
4181	A
4182	C
4183	T
4184	T
4185	C
4186	C
4187	T
4188	A
4189	C
4190	C
4191	A
4192	C
4193	T
4194	C
4195	A
4196	C
4197	C
4198	C
4199	T
4200	A
4201	G
4202	C
4203	A
4204	T
4205	T
4206	A
4207	C
4208	T
4209	T
4210	A
4211	T
4212	A
4213	T
4214	G
4215	A
4216	T
4217	A
4218	T
4219	G
4220	T
4221	C
4222	T
4223	C
4224	C
4225	A
4226	T
4227	A
4228	C
4229	C
4230	C
4231	A
4232	T
4233	T
4234	A
4235	C
4236	A
4237	A
4238	T
4239	C
4240	T
4241	C
4242	C
4243	A
4244	G
4245	C
4246	A
4247	T
4248	T
4249	C
4250	C
4251	C
4252	C
4253	C
4254	T
4255	C
4256	A
4257	A
4258	A
4259	C
4260	C
4261	T
4262	A
4263	A
4264	G
4265	A
4266	A
4267	A
4268	T
4269	A
4270	T
4271	G
4272	T
4273	C
4274	T
4275	G
4276	A
4277	T
4278	A
4279	A
4280	A
4281	A
4282	G
4283	A
4284	G
4285	T
4286	T
4287	A
4288	C
4289	T
4290	T
4291	T
4292	G
4293	A
4294	T
4295	A
4296	G
4297	A
4298	G
4299	T
4300	A
4301	A
4302	A
4303	T
4304	A
4305	A
4306	T
4307	A
4308	G
4309	G
4310	A
4311	G
4312	C
4313	T
4314	T
4315	A
4316	A
4317	A
4318	C
4319	C
4320	C
4321	C
4322	C
4323	T
4324	T
4325	A
4326	T
4327	T
4328	T
4329	C
4330	T
4331	A
4332	G
4333	G
4334	A
4335	C
4336	T
4337	A
4338	T
4339	G
4340	A
4341	G
4342	A
4343	A
4344	T
4345	C
4346	G
4347	A
4348	A
4349	C
4350	C
4351	C
4352	A
4353	T
4354	C
4355	C
4356	C
4357	T
4358	G
4359	A
4360	G
4361	A
4362	A
4363	T
4364	C
4365	C
4366	A
4367	A
4368	A
4369	A
4370	T
4371	T
4372	C
4373	T
4374	C
4375	C
4376	G
4377	T
4378	G
4379	C
4380	C
4381	A
4382	C
4383	C
4384	T
4385	A
4386	T
4387	C
4388	A
4389	C
4390	A
4391	C
4392	C
4393	C
4394	C
4395	A
4396	T
4397	C
4398	C
4399	T
4400	A
4401	A
4402	A
4403	G
4404	T
4405	A
4406	A
4407	G
4408	G
4409	T
4410	C
4411	A
4412	G
4413	C
4414	T
4415	A
4416	A
4417	A
4418	T
4419	A
4420	A
4421	G
4422	C
4423	T
4424	A
4425	T
4426	C
4427	G
4428	G
4429	G
4430	C
4431	C
4432	C
4433	A
4434	T
4435	A
4436	C
4437	C
4438	C
4439	C
4440	G
4441	A
4442	A
4443	A
4444	A
4445	T
4446	G
4447	T
4448	T
4449	G
4450	G
4451	T
4452	T
4453	A
4454	T
4455	A
4456	C
4457	C
4458	C
4459	T
4460	T
4461	C
4462	C
4463	C
4464	G
4465	T
4466	A
4467	C
4468	T
4469	A
4470	A
4471	T
4472	T
4473	A
4474	A
4475	T
4476	C
4477	C
4478	C
4479	C
4480	T
4481	G
4482	G
4483	C
4484	C
4485	C
4486	A
4487	A
4488	C
4489	C
4490	C
4491	G
4492	T
4493	C
4494	A
4495	T
4496	C
4497	T
4498	A
4499	C
4500	T
4501	C
4502	T
4503	A
4504	C
4505	C
4506	A
4507	T
4508	C
4509	T
4510	T
4511	T
4512	G
4513	C
4514	A
4515	G
4516	G
4517	C
4518	A
4519	C
4520	A
4521	C
4522	T
4523	C
4524	A
4525	T
4526	C
4527	A
4528	C
4529	A
4530	G
4531	C
4532	G
4533	C
4534	T
4535	A
4536	A
4537	G
4538	C
4539	T
4540	C
4541	G
4542	C
4543	A
4544	C
4545	T
4546	G
4547	A
4548	T
4549	T
4550	T
4551	T
4552	T
4553	T
4554	A
4555	C
4556	C
4557	T
4558	G
4559	A
4560	G
4561	T
4562	A
4563	G
4564	G
4565	C
4566	C
4567	T
4568	A
4569	G
4570	A
4571	A
4572	A
4573	T
4574	A
4575	A
4576	A
4577	C
4578	A
4579	T
4580	G
4581	C
4582	T
4583	A
4584	G
4585	C
4586	T
4587	T
4588	T
4589	T
4590	A
4591	T
4592	T
4593	C
4594	C
4595	A
4596	G
4597	T
4598	T
4599	C
4600	T
4601	A
4602	A
4603	C
4604	C
4605	A
4606	A
4607	A
4608	A
4609	A
4610	A
4611	A
4612	T
4613	A
4614	A
4615	A
4616	C
4617	C
4618	C
4619	T
4620	C
4621	G
4622	T
4623	T
4624	C
4625	C
4626	A
4627	C
4628	A
4629	G
4630	A
4631	A
4632	G
4633	C
4634	T
4635	G
4636	C
4637	C
4638	A
4639	T
4640	C
4641	A
4642	A
4643	G
4644	T
4645	A
4646	T
4647	T
4648	T
4649	C
4650	C
4651	T
4652	C
4653	A
4654	C
4655	G
4656	C
4657	A
4658	A
4659	G
4660	C
4661	A
4662	A
4663	C
4664	C
4665	G
4666	C
4667	A
4668	T
4669	C
4670	C
4671	A
4672	T
4673	A
4674	A
4675	T
4676	C
4677	C
4678	T
4679	T
4680	C
4681	T
4682	A
4683	A
4684	T
4685	A
4686	G
4687	C
4688	T
4689	A
4690	T
4691	C
4692	C
4693	T
4694	C
4695	T
4696	T
4697	C
4698	A
4699	A
4700	C
4701	A
4702	A
4703	T
4704	A
4705	T
4706	A
4707	C
4708	T
4709	C
4710	T
4711	C
4712	C
4713	G
4714	G
4715	A
4716	C
4717	A
4718	A
4719	T
4720	G
4721	A
4722	A
4723	C
4724	C
4725	A
4726	T
4727	A
4728	A
4729	C
4730	C
4731	A
4732	A
4733	T
4734	A
4735	C
4736	T
4737	A
4738	C
4739	C
4740	A
4741	A
4742	T
4743	C
4744	A
4745	A
4746	T
4747	A
4748	C
4749	T
4750	C
4751	A
4752	T
4753	C
4754	A
4755	T
4756	T
4757	A
4758	A
4759	T
4760	A
4761	A
4762	T
4763	C
4764	A
4765	T
4766	A
4767	A
4768	T
4769	A
4770	G
4771	C
4772	T
4773	A
4774	T
4775	A
4776	G
4777	C
4778	A
4779	A
4780	T
4781	A
4782	A
4783	A
4784	A
4785	C
4786	T
4787	A
4788	G
4789	G
4790	A
4791	A
4792	T
4793	A
4794	G
4795	C
4796	C
4797	C
4798	C
4799	C
4800	T
4801	T
4802	T
4803	C
4804	A
4805	C
4806	T
4807	T
4808	C
4809	T
4810	G
4811	A
4812	G
4813	T
4814	C
4815	C
4816	C
4817	A
4818	G
4819	A
4820	G
4821	G
4822	T
4823	T
4824	A
4825	C
4826	C
4827	C
4828	A
4829	A
4830	G
4831	G
4832	C
4833	A
4834	C
4835	C
4836	C
4837	C
4838	T
4839	C
4840	T
4841	G
4842	A
4843	C
4844	A
4845	T
4846	C
4847	C
4848	G
4849	G
4850	C
4851	C
4852	T
4853	G
4854	C
4855	T
4856	T
4857	C
4858	T
4859	T
4860	C
4861	T
4862	C
4863	A
4864	C
4865	A
4866	T
4867	G
4868	A
4869	C
4870	A
4871	A
4872	A
4873	A
4874	A
4875	C
4876	T
4877	A
4878	G
4879	C
4880	C
4881	C
4882	C
4883	C
4884	A
4885	T
4886	C
4887	T
4888	C
4889	A
4890	A
4891	T
4892	C
4893	A
4894	T
4895	A
4896	T
4897	A
4898	C
4899	C
4900	A
4901	A
4902	A
4903	T
4904	C
4905	T
4906	C
4907	T
4908	C
4909	C
4910	C
4911	T
4912	C
4913	A
4914	C
4915	T
4916	A
4917	A
4918	A
4919	C
4920	G
4921	T
4922	A
4923	A
4924	G
4925	C
4926	C
4927	T
4928	T
4929	C
4930	T
4931	C
4932	C
4933	T
4934	C
4935	A
4936	C
4937	T
4938	C
4939	T
4940	C
4941	T
4942	C
4943	A
4944	A
4945	T
4946	C
4947	T
4948	T
4949	A
4950	T
4951	C
4952	C
4953	A
4954	T
4955	C
4956	A
4957	T
4958	A
4959	G
4960	C
4961	A
4962	G
4963	G
4964	C
4965	A
4966	G
4967	T
4968	T
4969	G
4970	A
4971	G
4972	G
4973	T
4974	G
4975	G
4976	A
4977	T
4978	T
4979	A
4980	A
4981	A
4982	C
4983	C
4984	A
4985	A
4986	A
4987	C
4988	C
4989	C
4990	A
4991	G
4992	C
4993	T
4994	A
4995	C
4996	G
4997	C
4998	A
4999	A
5000	A
5001	A
5002	T
5003	C
5004	T
5005	T
5006	A
5007	G
5008	C
5009	A
5010	T
5011	A
5012	C
5013	T
5014	C
5015	C
5016	T
5017	C
5018	A
5019	A
5020	T
5021	T
5022	A
5023	C
5024	C
5025	C
5026	A
5027	C
5028	A
5029	T
5030	A
5031	G
5032	G
5033	A
5034	T
5035	G
5036	A
5037	A
5038	T
5039	A
5040	A
5041	T
5042	A
5043	G
5044	C
5045	A
5046	G
5047	T
5048	T
5049	C
5050	T
5051	A
5052	C
5053	C
5054	G
5055	T
5056	A
5057	C
5058	A
5059	A
5060	C
5061	C
5062	C
5063	T
5064	A
5065	A
5066	C
5067	A
5068	T
5069	A
5070	A
5071	C
5072	C
5073	A
5074	T
5075	T
5076	C
5077	T
5078	T
5079	A
5080	A
5081	T
5082	T
5083	T
5084	A
5085	A
5086	C
5087	T
5088	A
5089	T
5090	T
5091	T
5092	A
5093	T
5094	A
5095	T
5096	T
5097	A
5098	T
5099	C
5100	C
5101	T
5102	A
5103	A
5104	C
5105	T
5106	A
5107	C
5108	T
5109	A
5110	C
5111	C
5112	G
5113	C
5114	A
5115	T
5116	T
5117	C
5118	C
5119	T
5120	A
5121	C
5122	T
5123	A
5124	C
5125	T
5126	C
5127	A
5128	A
5129	C
5130	T
5131	T
5132	A
5133	A
5134	A
5135	C
5136	T
5137	C
5138	C
5139	A
5140	G
5141	C
5142	A
5143	C
5144	C
5145	A
5146	C
5147	G
5148	A
5149	C
5150	C
5151	C
5152	T
5153	A
5154	C
5155	T
5156	A
5157	C
5158	T
5159	A
5160	T
5161	C
5162	T
5163	C
5164	G
5165	C
5166	A
5167	C
5168	C
5169	T
5170	G
5171	A
5172	A
5173	A
5174	C
5175	A
5176	A
5177	G
5178	C
5179	T
5180	A
5181	A
5182	C
5183	A
5184	T
5185	G
5186	A
5187	C
5188	T
5189	A
5190	A
5191	C
5192	A
5193	C
5194	C
5195	C
5196	T
5197	T
5198	A
5199	A
5200	T
5201	T
5202	C
5203	C
5204	A
5205	T
5206	C
5207	C
5208	A
5209	C
5210	C
5211	C
5212	T
5213	C
5214	C
5215	T
5216	C
5217	T
5218	C
5219	C
5220	C
5221	T
5222	A
5223	G
5224	G
5225	A
5226	G
5227	G
5228	C
5229	C
5230	T
5231	G
5232	C
5233	C
5234	C
5235	C
5236	C
5237	G
5238	C
5239	T
5240	A
5241	A
5242	C
5243	C
5244	G
5245	G
5246	C
5247	T
5248	T
5249	T
5250	T
5251	T
5252	G
5253	C
5254	C
5255	C
5256	A
5257	A
5258	A
5259	T
5260	G
5261	G
5262	G
5263	C
5264	C
5265	A
5266	T
5267	T
5268	A
5269	T
5270	C
5271	G
5272	A
5273	A
5274	G
5275	A
5276	A
5277	T
5278	T
5279	C
5280	A
5281	C
5282	A
5283	A
5284	A
5285	A
5286	A
5287	A
5288	C
5289	A
5290	A
5291	T
5292	A
5293	G
5294	C
5295	C
5296	T
5297	C
5298	A
5299	T
5300	C
5301	A
5302	T
5303	C
5304	C
5305	C
5306	C
5307	A
5308	C
5309	C
5310	A
5311	T
5312	C
5313	A
5314	T
5315	A
5316	G
5317	C
5318	C
5319	A
5320	C
5321	C
5322	A
5323	T
5324	C
5325	A
5326	C
5327	C
5328	C
5329	T
5330	C
5331	C
5332	T
5333	T
5334	A
5335	A
5336	C
5337	C
5338	T
5339	C
5340	T
5341	A
5342	C
5343	T
5344	T
5345	C
5346	T
5347	A
5348	C
5349	C
5350	T
5351	A
5352	C
5353	G
5354	C
5355	C
5356	T
5357	A
5358	A
5359	T
5360	C
5361	T
5362	A
5363	C
5364	T
5365	C
5366	C
5367	A
5368	C
5369	C
5370	T
5371	C
5372	A
5373	A
5374	T
5375	C
5376	A
5377	C
5378	A
5379	C
5380	T
5381	A
5382	C
5383	T
5384	C
5385	C
5386	C
5387	C
5388	A
5389	T
5390	A
5391	T
5392	C
5393	T
5394	A
5395	A
5396	C
5397	A
5398	A
5399	C
5400	G
5401	T
5402	A
5403	A
5404	A
5405	A
5406	A
5407	T
5408	A
5409	A
5410	A
5411	A
5412	T
5413	G
5414	A
5415	C
5416	A
5417	G
5418	T
5419	T
5420	T
5421	G
5422	A
5423	A
5424	C
5425	A
5426	T
5427	A
5428	C
5429	A
5430	A
5431	A
5432	A
5433	C
5434	C
5435	C
5436	A
5437	C
5438	C
5439	C
5440	C
5441	A
5442	T
5443	T
5444	C
5445	C
5446	T
5447	C
5448	C
5449	C
5450	C
5451	A
5452	C
5453	A
5454	C
5455	T
5456	C
5457	A
5458	T
5459	C
5460	G
5461	C
5462	C
5463	C
5464	T
5465	T
5466	A
5467	C
5468	C
5469	A
5470	C
5471	G
5472	C
5473	T
5474	A
5475	C
5476	T
5477	C
5478	C
5479	T
5480	A
5481	C
5482	C
5483	T
5484	A
5485	T
5486	C
5487	T
5488	C
5489	C
5490	C
5491	C
5492	T
5493	T
5494	T
5495	T
5496	A
5497	T
5498	A
5499	C
5500	T
5501	A
5502	A
5503	T
5504	A
5505	A
5506	T
5507	C
5508	T
5509	T
5510	A
5511	T
5512	A
5513	G
5514	A
5515	A
5516	A
5517	T
5518	T
5519	T
5520	A
5521	G
5522	G
5523	T
5524	T
5525	A
5526	A
5527	A
5528	T
5529	A
5530	C
5531	A
5532	G
5533	A
5534	C
5535	C
5536	A
5537	A
5538	G
5539	A
5540	G
5541	C
5542	C
5543	T
5544	T
5545	C
5546	A
5547	A
5548	A
5549	G
5550	C
5551	C
5552	C
5553	T
5554	C
5555	A
5556	G
5557	T
5558	A
5559	A
5560	G
5561	T
5562	T
5563	G
5564	C
5565	A
5566	A
5567	T
5568	A
5569	C
5570	T
5571	T
5572	A
5573	A
5574	T
5575	T
5576	T
5577	C
5578	T
5579	G
5580	T
5581	A
5582	A
5583	C
5584	A
5585	G
5586	C
5587	T
5588	A
5589	A
5590	G
5591	G
5592	A
5593	C
5594	T
5595	G
5596	C
5597	A
5598	A
5599	A
5600	A
5601	C
5602	C
5603	C
5604	C
5605	A
5606	C
5607	T
5608	C
5609	T
5610	G
5611	C
5612	A
5613	T
5614	C
5615	A
5616	A
5617	C
5618	T
5619	G
5620	A
5621	A
5622	C
5623	G
5624	C
5625	A
5626	A
5627	A
5628	T
5629	C
5630	A
5631	G
5632	C
5633	C
5634	A
5635	C
5636	T
5637	T
5638	T
5639	A
5640	A
5641	T
5642	T
5643	A
5644	A
5645	G
5646	C
5647	T
5648	A
5649	A
5650	G
5651	C
5652	C
5653	C
5654	T
5655	T
5656	A
5657	C
5658	T
5659	A
5660	G
5661	A
5662	C
5663	C
5664	A
5665	A
5666	T
5667	G
5668	G
5669	G
5670	A
5671	C
5672	T
5673	T
5674	A
5675	A
5676	A
5677	C
5678	C
5679	C
5680	A
5681	C
5682	A
5683	A
5684	A
5685	C
5686	A
5687	C
5688	T
5689	T
5690	A
5691	G
5692	T
5693	T
5694	A
5695	A
5696	C
5697	A
5698	G
5699	C
5700	T
5701	A
5702	A
5703	G
5704	C
5705	A
5706	C
5707	C
5708	C
5709	T
5710	A
5711	A
5712	T
5713	C
5714	A
5715	A
5716	C
5717	T
5718	G
5719	G
5720	C
5721	T
5722	T
5723	C
5724	A
5725	A
5726	T
5727	C
5728	T
5729	A
5730	C
5731	T
5732	T
5733	C
5734	T
5735	C
5736	C
5737	C
5738	G
5739	C
5740	C
5741	G
5742	C
5743	C
5744	G
5745	G
5746	G
5747	A
5748	A
5749	A
5750	A
5751	A
5752	A
5753	G
5754	G
5755	C
5756	G
5757	G
5758	G
5759	A
5760	G
5761	A
5762	A
5763	G
5764	C
5765	C
5766	C
5767	C
5768	G
5769	G
5770	C
5771	A
5772	G
5773	G
5774	T
5775	T
5776	T
5777	G
5778	A
5779	A
5780	G
5781	C
5782	T
5783	G
5784	C
5785	T
5786	T
5787	C
5788	T
5789	T
5790	C
5791	G
5792	A
5793	A
5794	T
5795	T
5796	T
5797	G
5798	C
5799	A
5800	A
5801	T
5802	T
5803	C
5804	A
5805	A
5806	T
5807	A
5808	T
5809	G
5810	A
5811	A
5812	A
5813	A
5814	T
5815	C
5816	A
5817	C
5818	C
5819	T
5820	C
5821	G
5822	G
5823	A
5824	G
5825	C
5826	T
5827	G
5828	G
5829	T
5830	A
5831	A
5832	A
5833	A
5834	A
5835	G
5836	A
5837	G
5838	G
5839	C
5840	C
5841	T
5842	A
5843	A
5844	C
5845	C
5846	C
5847	C
5848	T
5849	G
5850	T
5851	C
5852	T
5853	T
5854	T
5855	A
5856	G
5857	A
5858	T
5859	T
5860	T
5861	A
5862	C
5863	A
5864	G
5865	T
5866	C
5867	C
5868	A
5869	A
5870	T
5871	G
5872	C
5873	T
5874	T
5875	C
5876	A
5877	C
5878	T
5879	C
5880	A
5881	G
5882	C
5883	C
5884	A
5885	T
5886	T
5887	T
5888	T
5889	A
5890	C
5891	C
5892	T
5893	C
5894	A
5895	C
5896	C
5897	C
5898	C
5899	C
5900	A
5901	C
5902	T
5903	G
5904	A
5905	T
5906	G
5907	T
5908	T
5909	C
5910	G
5911	C
5912	C
5913	G
5914	A
5915	C
5916	C
5917	G
5918	T
5919	T
5920	G
5921	A
5922	C
5923	T
5924	A
5925	T
5926	T
5927	C
5928	T
5929	C
5930	T
5931	A
5932	C
5933	A
5934	A
5935	A
5936	C
5937	C
5938	A
5939	C
5940	A
5941	A
5942	A
5943	G
5944	A
5945	C
5946	A
5947	T
5948	T
5949	G
5950	G
5951	A
5952	A
5953	C
5954	A
5955	C
5956	T
5957	A
5958	T
5959	A
5960	C
5961	C
5962	T
5963	A
5964	T
5965	T
5966	A
5967	T
5968	T
5969	C
5970	G
5971	G
5972	C
5973	G
5974	C
5975	A
5976	T
5977	G
5978	A
5979	G
5980	C
5981	T
5982	G
5983	G
5984	A
5985	G
5986	T
5987	C
5988	C
5989	T
5990	A
5991	G
5992	G
5993	C
5994	A
5995	C
5996	A
5997	G
5998	C
5999	T
6000	C
6001	T
6002	A
6003	A
6004	G
6005	C
6006	C
6007	T
6008	C
6009	C
6010	T
6011	T
6012	A
6013	T
6014	T
6015	C
6016	G
6017	A
6018	G
6019	C
6020	C
6021	G
6022	A
6023	G
6024	C
6025	T
6026	G
6027	G
6028	G
6029	C
6030	C
6031	A
6032	G
6033	C
6034	C
6035	A
6036	G
6037	G
6038	C
6039	A
6040	A
6041	C
6042	C
6043	T
6044	T
6045	C
6046	T
6047	A
6048	G
6049	G
6050	T
6051	A
6052	A
6053	C
6054	G
6055	A
6056	C
6057	C
6058	A
6059	C
6060	A
6061	T
6062	C
6063	T
6064	A
6065	C
6066	A
6067	A
6068	C
6069	G
6070	T
6071	T
6072	A
6073	T
6074	C
6075	G
6076	T
6077	C
6078	A
6079	C
6080	A
6081	G
6082	C
6083	C
6084	C
6085	A
6086	T
6087	G
6088	C
6089	A
6090	T
6091	T
6092	T
6093	G
6094	T
6095	A
6096	A
6097	T
6098	A
6099	A
6100	T
6101	C
6102	T
6103	T
6104	C
6105	T
6106	T
6107	C
6108	A
6109	T
6110	A
6111	G
6112	T
6113	A
6114	A
6115	T
6116	A
6117	C
6118	C
6119	C
6120	A
6121	T
6122	C
6123	A
6124	T
6125	A
6126	A
6127	T
6128	C
6129	G
6130	G
6131	A
6132	G
6133	G
6134	C
6135	T
6136	T
6137	T
6138	G
6139	G
6140	C
6141	A
6142	A
6143	C
6144	T
6145	G
6146	A
6147	C
6148	T
6149	A
6150	G
6151	T
6152	T
6153	C
6154	C
6155	C
6156	C
6157	T
6158	A
6159	A
6160	T
6161	A
6162	A
6163	T
6164	C
6165	G
6166	G
6167	T
6168	G
6169	C
6170	C
6171	C
6172	C
6173	C
6174	G
6175	A
6176	T
6177	A
6178	T
6179	G
6180	G
6181	C
6182	G
6183	T
6184	T
6185	T
6186	C
6187	C
6188	C
6189	C
6190	G
6191	C
6192	A
6193	T
6194	A
6195	A
6196	A
6197	C
6198	A
6199	A
6200	C
6201	A
6202	T
6203	A
6204	A
6205	G
6206	C
6207	T
6208	T
6209	C
6210	T
6211	G
6212	A
6213	C
6214	T
6215	C
6216	T
6217	T
6218	A
6219	C
6220	C
6221	T
6222	C
6223	C
6224	C
6225	T
6226	C
6227	T
6228	C
6229	T
6230	C
6231	C
6232	T
6233	A
6234	C
6235	T
6236	C
6237	C
6238	T
6239	G
6240	C
6241	T
6242	C
6243	G
6244	C
6245	A
6246	T
6247	C
6248	T
6249	G
6250	C
6251	T
6252	A
6253	T
6254	A
6255	G
6256	T
6257	G
6258	G
6259	A
6260	G
6261	G
6262	C
6263	C
6264	G
6265	G
6266	A
6267	G
6268	C
6269	A
6270	G
6271	G
6272	A
6273	A
6274	C
6275	A
6276	G
6277	G
6278	T
6279	T
6280	G
6281	A
6282	A
6283	C
6284	A
6285	G
6286	T
6287	C
6288	T
6289	A
6290	C
6291	C
6292	C
6293	T
6294	C
6295	C
6296	C
6297	T
6298	T
6299	A
6300	G
6301	C
6302	A
6303	G
6304	G
6305	G
6306	A
6307	A
6308	C
6309	T
6310	A
6311	C
6312	T
6313	C
6314	C
6315	C
6316	A
6317	C
6318	C
6319	C
6320	T
6321	G
6322	G
6323	A
6324	G
6325	C
6326	C
6327	T
6328	C
6329	C
6330	G
6331	T
6332	A
6333	G
6334	A
6335	C
6336	C
6337	T
6338	A
6339	A
6340	C
6341	C
6342	A
6343	T
6344	C
6345	T
6346	T
6347	C
6348	T
6349	C
6350	C
6351	T
6352	T
6353	A
6354	C
6355	A
6356	C
6357	C
6358	T
6359	A
6360	G
6361	C
6362	A
6363	G
6364	G
6365	T
6366	G
6367	T
6368	C
6369	T
6370	C
6371	C
6372	T
6373	C
6374	T
6375	A
6376	T
6377	C
6378	T
6379	T
6380	A
6381	G
6382	G
6383	G
6384	G
6385	C
6386	C
6387	A
6388	T
6389	C
6390	A
6391	A
6392	T
6393	T
6394	T
6395	C
6396	A
6397	T
6398	C
6399	A
6400	C
6401	A
6402	A
6403	C
6404	A
6405	A
6406	T
6407	T
6408	A
6409	T
6410	C
6411	A
6412	A
6413	T
6414	A
6415	T
6416	A
6417	A
6418	A
6419	A
6420	C
6421	C
6422	C
6423	C
6424	C
6425	T
6426	G
6427	C
6428	C
6429	A
6430	T
6431	A
6432	A
6433	C
6434	C
6435	C
6436	A
6437	A
6438	T
6439	A
6440	C
6441	C
6442	A
6443	A
6444	A
6445	C
6446	G
6447	C
6448	C
6449	C
6450	C
6451	T
6452	C
6453	T
6454	T
6455	C
6456	G
6457	T
6458	C
6459	T
6460	G
6461	A
6462	T
6463	C
6464	C
6465	G
6466	T
6467	C
6468	C
6469	T
6470	A
6471	A
6472	T
6473	C
6474	A
6475	C
6476	A
6477	G
6478	C
6479	A
6480	G
6481	T
6482	C
6483	C
6484	T
6485	A
6486	C
6487	T
6488	T
6489	C
6490	T
6491	C
6492	C
6493	T
6494	A
6495	T
6496	C
6497	T
6498	C
6499	T
6500	C
6501	C
6502	C
6503	A
6504	G
6505	T
6506	C
6507	C
6508	T
6509	A
6510	G
6511	C
6512	T
6513	G
6514	C
6515	T
6516	G
6517	G
6518	C
6519	A
6520	T
6521	C
6522	A
6523	C
6524	T
6525	A
6526	T
6527	A
6528	C
6529	T
6530	A
6531	C
6532	T
6533	A
6534	A
6535	C
6536	A
6537	G
6538	A
6539	C
6540	C
6541	G
6542	C
6543	A
6544	A
6545	C
6546	C
6547	T
6548	C
6549	A
6550	A
6551	C
6552	A
6553	C
6554	C
6555	A
6556	C
6557	C
6558	T
6559	T
6560	C
6561	T
6562	T
6563	C
6564	G
6565	A
6566	C
6567	C
6568	C
6569	C
6570	G
6571	C
6572	C
6573	G
6574	G
6575	A
6576	G
6577	G
6578	A
6579	G
6580	G
6581	A
6582	G
6583	A
6584	C
6585	C
6586	C
6587	C
6588	A
6589	T
6590	T
6591	C
6592	T
6593	A
6594	T
6595	A
6596	C
6597	C
6598	A
6599	A
6600	C
6601	A
6602	C
6603	C
6604	T
6605	A
6606	T
6607	T
6608	C
6609	T
6610	G
6611	A
6612	T
6613	T
6614	T
6615	T
6616	T
6617	C
6618	G
6619	G
6620	T
6621	C
6622	A
6623	C
6624	C
6625	C
6626	T
6627	G
6628	A
6629	A
6630	G
6631	T
6632	T
6633	T
6634	A
6635	T
6636	A
6637	T
6638	T
6639	C
6640	T
6641	T
6642	A
6643	T
6644	C
6645	C
6646	T
6647	A
6648	C
6649	C
6650	A
6651	G
6652	G
6653	C
6654	T
6655	T
6656	C
6657	G
6658	G
6659	A
6660	A
6661	T
6662	A
6663	A
6664	T
6665	C
6666	T
6667	C
6668	C
6669	C
6670	A
6671	T
6672	A
6673	T
6674	T
6675	G
6676	T
6677	A
6678	A
6679	C
6680	T
6681	T
6682	A
6683	C
6684	T
6685	A
6686	C
6687	T
6688	C
6689	C
6690	G
6691	G
6692	A
6693	A
6694	A
6695	A
6696	A
6697	A
6698	A
6699	G
6700	A
6701	A
6702	C
6703	C
6704	A
6705	T
6706	T
6707	T
6708	G
6709	G
6710	A
6711	T
6712	A
6713	C
6714	A
6715	T
6716	A
6717	G
6718	G
6719	T
6720	A
6721	T
6722	G
6723	G
6724	T
6725	C
6726	T
6727	G
6728	A
6729	G
6730	C
6731	T
6732	A
6733	T
6734	G
6735	A
6736	T
6737	A
6738	T
6739	C
6740	A
6741	A
6742	T
6743	T
6744	G
6745	G
6746	C
6747	T
6748	T
6749	C
6750	C
6751	T
6752	A
6753	G
6754	G
6755	G
6756	T
6757	T
6758	T
6759	A
6760	T
6761	C
6762	G
6763	T
6764	G
6765	T
6766	G
6767	A
6768	G
6769	C
6770	A
6771	C
6772	A
6773	C
6774	C
6775	A
6776	T
6777	A
6778	T
6779	A
6780	T
6781	T
6782	T
6783	A
6784	C
6785	A
6786	G
6787	T
6788	A
6789	G
6790	G
6791	A
6792	A
6793	T
6794	A
6795	G
6796	A
6797	C
6798	G
6799	T
6800	A
6801	G
6802	A
6803	C
6804	A
6805	C
6806	A
6807	C
6808	G
6809	A
6810	G
6811	C
6812	A
6813	T
6814	A
6815	T
6816	T
6817	T
6818	C
6819	A
6820	C
6821	C
6822	T
6823	C
6824	C
6825	G
6826	C
6827	T
6828	A
6829	C
6830	C
6831	A
6832	T
6833	A
6834	A
6835	T
6836	C
6837	A
6838	T
6839	C
6840	G
6841	C
6842	T
6843	A
6844	T
6845	C
6846	C
6847	C
6848	C
6849	A
6850	C
6851	C
6852	G
6853	G
6854	C
6855	G
6856	T
6857	C
6858	A
6859	A
6860	A
6861	G
6862	T
6863	A
6864	T
6865	T
6866	T
6867	A
6868	G
6869	C
6870	T
6871	G
6872	A
6873	C
6874	T
6875	C
6876	G
6877	C
6878	C
6879	A
6880	C
6881	A
6882	C
6883	T
6884	C
6885	C
6886	A
6887	C
6888	G
6889	G
6890	A
6891	A
6892	G
6893	C
6894	A
6895	A
6896	T
6897	A
6898	T
6899	G
6900	A
6901	A
6902	A
6903	T
6904	G
6905	A
6906	T
6907	C
6908	T
6909	G
6910	C
6911	T
6912	G
6913	C
6914	A
6915	G
6916	T
6917	G
6918	C
6919	T
6920	C
6921	T
6922	G
6923	A
6924	G
6925	C
6926	C
6927	C
6928	T
6929	A
6930	G
6931	G
6932	A
6933	T
6934	T
6935	C
6936	A
6937	T
6938	C
6939	T
6940	T
6941	T
6942	C
6943	T
6944	T
6945	T
6946	T
6947	C
6948	A
6949	C
6950	C
6951	G
6952	T
6953	A
6954	G
6955	G
6956	T
6957	G
6958	G
6959	C
6960	C
6961	T
6962	G
6963	A
6964	C
6965	T
6966	G
6967	G
6968	C
6969	A
6970	T
6971	T
6972	G
6973	T
6974	A
6975	T
6976	T
6977	A
6978	G
6979	C
6980	A
6981	A
6982	A
6983	C
6984	T
6985	C
6986	A
6987	T
6988	C
6989	A
6990	C
6991	T
6992	A
6993	G
6994	A
6995	C
6996	A
6997	T
6998	C
6999	G
7000	T
7001	A
7002	C
7003	T
7004	A
7005	C
7006	A
7007	C
7008	G
7009	A
7010	C
7011	A
7012	C
7013	G
7014	T
7015	A
7016	C
7017	T
7018	A
7019	C
7020	G
7021	T
7022	T
7023	G
7024	T
7025	A
7026	G
7027	C
7028	C
7029	C
7030	A
7031	C
7032	T
7033	T
7034	C
7035	C
7036	A
7037	C
7038	T
7039	A
7040	T
7041	G
7042	T
7043	C
7044	C
7045	T
7046	A
7047	T
7048	C
7049	A
7050	A
7051	T
7052	A
7053	G
7054	G
7055	A
7056	G
7057	C
7058	T
7059	G
7060	T
7061	A
7062	T
7063	T
7064	T
7065	G
7066	C
7067	C
7068	A
7069	T
7070	C
7071	A
7072	T
7073	A
7074	G
7075	G
7076	A
7077	G
7078	G
7079	C
7080	T
7081	T
7082	C
7083	A
7084	T
7085	T
7086	C
7087	A
7088	C
7089	T
7090	G
7091	A
7092	T
7093	T
7094	T
7095	C
7096	C
7097	C
7098	C
7099	T
7100	A
7101	T
7102	T
7103	C
7104	T
7105	C
7106	A
7107	G
7108	G
7109	C
7110	T
7111	A
7112	C
7113	A
7114	C
7115	C
7116	C
7117	T
7118	A
7119	G
7120	A
7121	C
7122	C
7123	A
7124	A
7125	A
7126	C
7127	C
7128	T
7129	A
7130	C
7131	G
7132	C
7133	C
7134	A
7135	A
7136	A
7137	A
7138	T
7139	C
7140	C
7141	A
7142	T
7143	T
7144	T
7145	C
7146	A
7147	C
7148	T
7149	A
7150	T
7151	C
7152	A
7153	T
7154	A
7155	T
7156	T
7157	C
7158	A
7159	T
7160	C
7161	G
7162	G
7163	C
7164	G
7165	T
7166	A
7167	A
7168	A
7169	T
7170	C
7171	T
7172	A
7173	A
7174	C
7175	T
7176	T
7177	T
7178	C
7179	T
7180	T
7181	C
7182	C
7183	C
7184	A
7185	C
7186	A
7187	A
7188	C
7189	A
7190	C
7191	T
7192	T
7193	T
7194	C
7195	T
7196	C
7197	G
7198	G
7199	C
7200	C
7201	T
7202	A
7203	T
7204	C
7205	C
7206	G
7207	G
7208	A
7209	A
7210	T
7211	G
7212	C
7213	C
7214	C
7215	C
7216	G
7217	A
7218	C
7219	G
7220	T
7221	T
7222	A
7223	C
7224	T
7225	C
7226	G
7227	G
7228	A
7229	C
7230	T
7231	A
7232	C
7233	C
7234	C
7235	C
7236	G
7237	A
7238	T
7239	G
7240	C
7241	A
7242	T
7243	A
7244	C
7245	A
7246	C
7247	C
7248	A
7249	C
7250	A
7251	T
7252	G
7253	A
7254	A
7255	A
7256	C
7257	A
7258	T
7259	C
7260	C
7261	T
7262	A
7263	T
7264	C
7265	A
7266	T
7267	C
7268	T
7269	G
7270	T
7271	A
7272	G
7273	G
7274	C
7275	T
7276	C
7277	A
7278	T
7279	T
7280	C
7281	A
7282	T
7283	T
7284	T
7285	C
7286	T
7287	C
7288	T
7289	A
7290	A
7291	C
7292	A
7293	G
7294	C
7295	A
7296	G
7297	T
7298	A
7299	A
7300	T
7301	A
7302	T
7303	T
7304	A
7305	A
7306	T
7307	A
7308	A
7309	T
7310	T
7311	T
7312	T
7313	C
7314	A
7315	T
7316	G
7317	A
7318	T
7319	T
7320	T
7321	G
7322	A
7323	G
7324	A
7325	A
7326	G
7327	C
7328	C
7329	T
7330	T
7331	C
7332	G
7333	C
7334	T
7335	T
7336	C
7337	G
7338	A
7339	A
7340	G
7341	C
7342	G
7343	A
7344	A
7345	A
7346	A
7347	G
7348	T
7349	C
7350	C
7351	T
7352	A
7353	A
7354	T
7355	A
7356	G
7357	T
7358	A
7359	G
7360	A
7361	A
7362	G
7363	A
7364	A
7365	C
7366	C
7367	C
7368	T
7369	C
7370	C
7371	A
7372	T
7373	A
7374	A
7375	A
7376	C
7377	C
7378	T
7379	G
7380	G
7381	A
7382	G
7383	T
7384	G
7385	A
7386	C
7387	T
7388	A
7389	T
7390	A
7391	T
7392	G
7393	G
7394	A
7395	T
7396	G
7397	C
7398	C
7399	C
7400	C
7401	C
7402	C
7403	A
7404	C
7405	C
7406	C
7407	T
7408	A
7409	C
7410	C
7411	A
7412	C
7413	A
7414	C
7415	A
7416	T
7417	T
7418	C
7419	G
7420	A
7421	A
7422	G
7423	A
7424	A
7425	C
7426	C
7427	C
7428	G
7429	T
7430	A
7431	T
7432	A
7433	C
7434	A
7435	T
7436	A
7437	A
7438	A
7439	A
7440	T
7441	C
7442	T
7443	A
7444	G
7445	A
7446	C
7447	A
7448	A
7449	A
7450	A
7451	A
7452	A
7453	G
7454	G
7455	A
7456	A
7457	G
7458	G
7459	A
7460	A
7461	T
7462	C
7463	G
7464	A
7465	A
7466	C
7467	C
7468	C
7469	C
7470	C
7471	C
7472	A
7473	A
7474	A
7475	G
7476	C
7477	T
7478	G
7479	G
7480	T
7481	T
7482	T
7483	C
7484	A
7485	A
7486	G
7487	C
7488	C
7489	A
7490	A
7491	C
7492	C
7493	C
7494	C
7495	A
7496	T
7497	G
7498	G
7499	C
7500	C
7501	T
7502	C
7503	C
7504	A
7505	T
7506	G
7507	A
7508	C
7509	T
7510	T
7511	T
7512	T
7513	T
7514	C
7515	A
7516	A
7517	A
7518	A
7519	A
7520	G
7521	G
7522	T
7523	A
7524	T
7525	T
7526	A
7527	G
7528	A
7529	A
7530	A
7531	A
7532	A
7533	C
7534	C
7535	A
7536	T
7537	T
7538	T
7539	C
7540	A
7541	T
7542	A
7543	A
7544	C
7545	T
7546	T
7547	T
7548	G
7549	T
7550	C
7551	A
7552	A
7553	A
7554	G
7555	T
7556	T
7557	A
7558	A
7559	A
7560	T
7561	T
7562	A
7563	T
7564	A
7565	G
7566	G
7567	C
7568	T
7569	A
7570	A
7571	A
7572	T
7573	C
7574	C
7575	T
7576	A
7577	T
7578	A
7579	T
7580	A
7581	T
7582	C
7583	T
7584	T
7585	A
7586	A
7587	T
7588	G
7589	G
7590	C
7591	A
7592	C
7593	A
7594	T
7595	G
7596	C
7597	A
7598	G
7599	C
7600	G
7601	C
7602	A
7603	A
7604	G
7605	T
7606	A
7607	G
7608	G
7609	T
7610	C
7611	T
7612	A
7613	C
7614	A
7615	A
7616	G
7617	A
7618	C
7619	G
7620	C
7621	T
7622	A
7623	C
7624	T
7625	T
7626	C
7627	C
7628	C
7629	C
7630	T
7631	A
7632	T
7633	C
7634	A
7635	T
7636	A
7637	G
7638	A
7639	A
7640	G
7641	A
7642	G
7643	C
7644	T
7645	T
7646	A
7647	T
7648	C
7649	A
7650	C
7651	C
7652	T
7653	T
7654	T
7655	C
7656	A
7657	T
7658	G
7659	A
7660	T
7661	C
7662	A
7663	C
7664	G
7665	C
7666	C
7667	C
7668	T
7669	C
7670	A
7671	T
7672	A
7673	A
7674	T
7675	C
7676	A
7677	T
7678	T
7679	T
7680	T
7681	C
7682	C
7683	T
7684	T
7685	A
7686	T
7687	C
7688	T
7689	G
7690	C
7691	T
7692	T
7693	C
7694	C
7695	T
7696	A
7697	G
7698	T
7699	C
7700	C
7701	T
7702	G
7703	T
7704	A
7705	T
7706	G
7707	C
7708	C
7709	C
7710	T
7711	T
7712	T
7713	T
7714	C
7715	C
7716	T
7717	A
7718	A
7719	C
7720	A
7721	C
7722	T
7723	C
7724	A
7725	C
7726	A
7727	A
7728	C
7729	A
7730	A
7731	A
7732	A
7733	C
7734	T
7735	A
7736	A
7737	C
7738	T
7739	A
7740	A
7741	T
7742	A
7743	C
7744	T
7745	A
7746	A
7747	C
7748	A
7749	T
7750	C
7751	T
7752	C
7753	A
7754	G
7755	A
7756	C
7757	G
7758	C
7759	T
7760	C
7761	A
7762	G
7763	G
7764	A
7765	A
7766	A
7767	T
7768	A
7769	G
7770	A
7771	A
7772	A
7773	C
7774	C
7775	G
7776	T
7777	C
7778	T
7779	G
7780	A
7781	A
7782	C
7783	T
7784	A
7785	T
7786	C
7787	C
7788	T
7789	G
7790	C
7791	C
7792	C
7793	G
7794	C
7795	C
7796	A
7797	T
7798	C
7799	A
7800	T
7801	C
7802	C
7803	T
7804	A
7805	G
7806	T
7807	C
7808	C
7809	T
7810	C
7811	A
7812	T
7813	C
7814	G
7815	C
7816	C
7817	C
7818	T
7819	C
7820	C
7821	C
7822	A
7823	T
7824	C
7825	C
7826	C
7827	T
7828	A
7829	C
7830	G
7831	C
7832	A
7833	T
7834	C
7835	C
7836	T
7837	T
7838	T
7839	A
7840	C
7841	A
7842	T
7843	A
7844	A
7845	C
7846	A
7847	G
7848	A
7849	C
7850	G
7851	A
7852	G
7853	G
7854	T
7855	C
7856	A
7857	A
7858	C
7859	G
7860	A
7861	T
7862	C
7863	C
7864	C
7865	T
7866	C
7867	C
7868	C
7869	T
7870	T
7871	A
7872	C
7873	C
7874	A
7875	T
7876	C
7877	A
7878	A
7879	A
7880	T
7881	C
7882	A
7883	A
7884	T
7885	T
7886	G
7887	G
7888	C
7889	C
7890	A
7891	C
7892	C
7893	A
7894	A
7895	T
7896	G
7897	G
7898	T
7899	A
7900	C
7901	T
7902	G
7903	A
7904	A
7905	C
7906	C
7907	T
7908	A
7909	C
7910	G
7911	A
7912	G
7913	T
7914	A
7915	C
7916	A
7917	C
7918	C
7919	G
7920	A
7921	C
7922	T
7923	A
7924	C
7925	G
7926	G
7927	C
7928	G
7929	G
7930	A
7931	C
7932	T
7933	A
7934	A
7935	T
7936	C
7937	T
7938	T
7939	C
7940	A
7941	A
7942	C
7943	T
7944	C
7945	C
7946	T
7947	A
7948	C
7949	A
7950	T
7951	A
7952	C
7953	T
7954	T
7955	C
7956	C
7957	C
7958	C
7959	C
7960	A
7961	T
7962	T
7963	A
7964	T
7965	T
7966	C
7967	C
7968	T
7969	A
7970	G
7971	A
7972	A
7973	C
7974	C
7975	A
7976	G
7977	G
7978	C
7979	G
7980	A
7981	C
7982	C
7983	T
7984	G
7985	C
7986	G
7987	A
7988	C
7989	T
7990	C
7991	C
7992	T
7993	T
7994	G
7995	A
7996	C
7997	G
7998	T
7999	T
8000	G
8001	A
8002	C
8003	A
8004	A
8005	T
8006	C
8007	G
8008	A
8009	G
8010	T
8011	A
8012	G
8013	T
8014	A
8015	C
8016	T
8017	C
8018	C
8019	C
8020	G
8021	A
8022	T
8023	T
8024	G
8025	A
8026	A
8027	G
8028	C
8029	C
8030	C
8031	C
8032	C
8033	A
8034	T
8035	T
8036	C
8037	G
8038	T
8039	A
8040	T
8041	A
8042	A
8043	T
8044	A
8045	A
8046	T
8047	T
8048	A
8049	C
8050	A
8051	T
8052	C
8053	A
8054	C
8055	A
8056	A
8057	G
8058	A
8059	C
8060	G
8061	T
8062	C
8063	T
8064	T
8065	G
8066	C
8067	A
8068	C
8069	T
8070	C
8071	A
8072	T
8073	G
8074	A
8075	G
8076	C
8077	T
8078	G
8079	T
8080	C
8081	C
8082	C
8083	C
8084	A
8085	C
8086	A
8087	T
8088	T
8089	A
8090	G
8091	G
8092	C
8093	T
8094	T
8095	A
8096	A
8097	A
8098	A
8099	A
8100	C
8101	A
8102	G
8103	A
8104	T
8105	G
8106	C
8107	A
8108	A
8109	T
8110	T
8111	C
8112	C
8113	C
8114	G
8115	G
8116	A
8117	C
8118	G
8119	T
8120	C
8121	T
8122	A
8123	A
8124	A
8125	C
8126	C
8127	A
8128	A
8129	A
8130	C
8131	C
8132	A
8133	C
8134	T
8135	T
8136	T
8137	C
8138	A
8139	C
8140	C
8141	G
8142	C
8143	T
8144	A
8145	C
8146	A
8147	C
8148	G
8149	A
8150	C
8151	C
8152	G
8153	G
8154	G
8155	G
8156	G
8157	T
8158	A
8159	T
8160	A
8161	C
8162	T
8163	A
8164	C
8165	G
8166	G
8167	T
8168	C
8169	A
8170	A
8171	T
8172	G
8173	C
8174	T
8175	C
8176	T
8177	G
8178	A
8179	A
8180	A
8181	T
8182	C
8183	T
8184	G
8185	T
8186	G
8187	G
8188	A
8189	G
8190	C
8191	A
8192	A
8193	A
8194	C
8195	C
8196	A
8197	C
8198	A
8199	G
8200	T
8201	T
8202	T
8203	C
8204	A
8205	T
8206	G
8207	C
8208	C
8209	C
8210	A
8211	T
8212	C
8213	G
8214	T
8215	C
8216	C
8217	T
8218	A
8219	G
8220	A
8221	A
8222	T
8223	T
8224	A
8225	A
8226	T
8227	T
8228	C
8229	C
8230	C
8231	C
8232	T
8233	A
8234	A
8235	A
8236	A
8237	A
8238	T
8239	C
8240	T
8241	T
8242	T
8243	G
8244	A
8245	A
8246	A
8247	T
8248	A
8249	G
8250	G
8251	G
8252	C
8253	C
8254	C
8255	G
8256	T
8257	A
8258	T
8259	T
8260	T
8261	A
8262	C
8263	C
8264	C
8265	T
8266	A
8267	T
8268	A
8269	G
8270	C
8271	A
8272	C
8273	C
8274	C
8275	C
8276	C
8277	T
8278	C
8279	T
8280	A
8281	C
8282	C
8283	C
8284	C
8285	C
8286	T
8287	C
8288	T
8289	A
8290	G
8291	A
8292	G
8293	C
8294	C
8295	C
8296	A
8297	C
8298	T
8299	G
8300	T
8301	A
8302	A
8303	A
8304	G
8305	C
8306	T
8307	A
8308	A
8309	C
8310	T
8311	T
8312	A
8313	G
8314	C
8315	A
8316	T
8317	T
8318	A
8319	A
8320	C
8321	C
8322	T
8323	T
8324	T
8325	T
8326	A
8327	A
8328	G
8329	T
8330	T
8331	A
8332	A
8333	A
8334	G
8335	A
8336	T
8337	T
8338	A
8339	A
8340	G
8341	A
8342	G
8343	A
8344	A
8345	C
8346	C
8347	A
8348	A
8349	C
8350	A
8351	C
8352	C
8353	T
8354	C
8355	T
8356	T
8357	T
8358	A
8359	C
8360	A
8361	G
8362	T
8363	G
8364	A
8365	A
8366	A
8367	T
8368	G
8369	C
8370	C
8371	C
8372	C
8373	A
8374	A
8375	C
8376	T
8377	A
8378	A
8379	A
8380	T
8381	A
8382	C
8383	T
8384	A
8385	C
8386	C
8387	G
8388	T
8389	A
8390	T
8391	G
8392	G
8393	C
8394	C
8395	C
8396	A
8397	C
8398	C
8399	A
8400	T
8401	A
8402	A
8403	T
8404	T
8405	A
8406	C
8407	C
8408	C
8409	C
8410	C
8411	A
8412	T
8413	A
8414	C
8415	T
8416	C
8417	C
8418	T
8419	T
8420	A
8421	C
8422	A
8423	C
8424	T
8425	A
8426	T
8427	T
8428	C
8429	C
8430	T
8431	C
8432	A
8433	T
8434	C
8435	A
8436	C
8437	C
8438	C
8439	A
8440	A
8441	C
8442	T
8443	A
8444	A
8445	A
8446	A
8447	A
8448	T
8449	A
8450	T
8451	T
8452	A
8453	A
8454	A
8455	C
8456	A
8457	C
8458	A
8459	A
8460	A
8461	C
8462	T
8463	A
8464	C
8465	C
8466	A
8467	C
8468	C
8469	T
8470	A
8471	C
8472	C
8473	T
8474	C
8475	C
8476	C
8477	T
8478	C
8479	A
8480	C
8481	C
8482	A
8483	A
8484	A
8485	G
8486	C
8487	C
8488	C
8489	A
8490	T
8491	A
8492	A
8493	A
8494	A
8495	A
8496	T
8497	A
8498	A
8499	A
8500	A
8501	A
8502	A
8503	T
8504	T
8505	A
8506	T
8507	A
8508	A
8509	C
8510	A
8511	A
8512	A
8513	C
8514	C
8515	C
8516	T
8517	G
8518	A
8519	G
8520	A
8521	A
8522	C
8523	C
8524	A
8525	A
8526	A
8527	A
8528	T
8529	G
8530	A
8531	A
8532	C
8533	G
8534	A
8535	A
8536	A
8537	A
8538	T
8539	C
8540	T
8541	G
8542	T
8543	T
8544	C
8545	G
8546	C
8547	T
8548	T
8549	C
8550	A
8551	T
8552	T
8553	C
8554	A
8555	T
8556	T
8557	G
8558	C
8559	C
8560	C
8561	C
8562	C
8563	A
8564	C
8565	A
8566	A
8567	T
8568	C
8569	C
8570	T
8571	A
8572	G
8573	G
8574	C
8575	C
8576	T
8577	A
8578	C
8579	C
8580	C
8581	G
8582	C
8583	C
8584	G
8585	C
8586	A
8587	G
8588	T
8589	A
8590	C
8591	T
8592	G
8593	A
8594	T
8595	C
8596	A
8597	T
8598	T
8599	C
8600	T
8601	A
8602	T
8603	T
8604	T
8605	C
8606	C
8607	C
8608	C
8609	C
8610	T
8611	C
8612	T
8613	A
8614	T
8615	T
8616	G
8617	A
8618	T
8619	C
8620	C
8621	C
8622	C
8623	A
8624	C
8625	C
8626	T
8627	C
8628	C
8629	A
8630	A
8631	A
8632	T
8633	A
8634	T
8635	C
8636	T
8637	C
8638	A
8639	T
8640	C
8641	A
8642	A
8643	C
8644	A
8645	A
8646	C
8647	C
8648	G
8649	A
8650	C
8651	T
8652	A
8653	A
8654	T
8655	C
8656	A
8657	C
8658	C
8659	A
8660	C
8661	C
8662	C
8663	A
8664	A
8665	C
8666	A
8667	A
8668	T
8669	G
8670	A
8671	C
8672	T
8673	A
8674	A
8675	T
8676	C
8677	A
8678	A
8679	A
8680	C
8681	T
8682	A
8683	A
8684	C
8685	C
8686	T
8687	C
8688	A
8689	A
8690	A
8691	A
8692	C
8693	A
8694	A
8695	A
8696	T
8697	G
8698	A
8699	T
8700	A
8701	A
8702	C
8703	C
8704	A
8705	T
8706	A
8707	C
8708	A
8709	C
8710	A
8711	A
8712	C
8713	A
8714	C
8715	T
8716	A
8717	A
8718	A
8719	G
8720	G
8721	A
8722	C
8723	G
8724	A
8725	A
8726	C
8727	C
8728	T
8729	G
8730	A
8731	T
8732	C
8733	T
8734	C
8735	T
8736	T
8737	A
8738	T
8739	A
8740	C
8741	T
8742	A
8743	G
8744	T
8745	A
8746	T
8747	C
8748	C
8749	T
8750	T
8751	A
8752	A
8753	T
8754	C
8755	A
8756	T
8757	T
8758	T
8759	T
8760	T
8761	A
8762	T
8763	T
8764	G
8765	C
8766	C
8767	A
8768	C
8769	A
8770	A
8771	C
8772	T
8773	A
8774	A
8775	C
8776	C
8777	T
8778	C
8779	C
8780	T
8781	C
8782	G
8783	G
8784	A
8785	C
8786	T
8787	C
8788	C
8789	T
8790	G
8791	C
8792	C
8793	T
8794	C
8795	A
8796	C
8797	T
8798	C
8799	A
8800	T
8801	T
8802	T
8803	A
8804	C
8805	A
8806	C
8807	C
8808	A
8809	A
8810	C
8811	C
8812	A
8813	C
8814	C
8815	C
8816	A
8817	A
8818	C
8819	T
8820	A
8821	T
8822	C
8823	T
8824	A
8825	T
8826	A
8827	A
8828	A
8829	C
8830	C
8831	T
8832	A
8833	G
8834	C
8835	C
8836	A
8837	T
8838	G
8839	G
8840	C
8841	C
8842	A
8843	T
8844	C
8845	C
8846	C
8847	C
8848	T
8849	T
8850	A
8851	T
8852	G
8853	A
8854	G
8855	C
8856	G
8857	G
8858	G
8859	C
8860	A
8861	C
8862	A
8863	G
8864	T
8865	G
8866	A
8867	T
8868	T
8869	A
8870	T
8871	A
8872	G
8873	G
8874	C
8875	T
8876	T
8877	T
8878	C
8879	G
8880	C
8881	T
8882	C
8883	T
8884	A
8885	A
8886	G
8887	A
8888	T
8889	T
8890	A
8891	A
8892	A
8893	A
8894	A
8895	T
8896	G
8897	C
8898	C
8899	C
8900	T
8901	A
8902	G
8903	C
8904	C
8905	C
8906	A
8907	C
8908	T
8909	T
8910	C
8911	T
8912	T
8913	A
8914	C
8915	C
8916	A
8917	C
8918	A
8919	A
8920	G
8921	G
8922	C
8923	A
8924	C
8925	A
8926	C
8927	C
8928	T
8929	A
8930	C
8931	A
8932	C
8933	C
8934	C
8935	C
8936	T
8937	T
8938	A
8939	T
8940	C
8941	C
8942	C
8943	C
8944	A
8945	T
8946	A
8947	C
8948	T
8949	A
8950	G
8951	T
8952	T
8953	A
8954	T
8955	T
8956	A
8957	T
8958	C
8959	G
8960	A
8961	A
8962	A
8963	C
8964	C
8965	A
8966	T
8967	C
8968	A
8969	G
8970	C
8971	C
8972	T
8973	A
8974	C
8975	T
8976	C
8977	A
8978	T
8979	T
8980	C
8981	A
8982	A
8983	C
8984	C
8985	A
8986	A
8987	T
8988	A
8989	G
8990	C
8991	C
8992	C
8993	T
8994	G
8995	G
8996	C
8997	C
8998	G
8999	T
9000	A
9001	C
9002	G
9003	C
9004	C
9005	T
9006	A
9007	A
9008	C
9009	C
9010	G
9011	C
9012	T
9013	A
9014	A
9015	C
9016	A
9017	T
9018	T
9019	A
9020	C
9021	T
9022	G
9023	C
9024	A
9025	G
9026	G
9027	C
9028	C
9029	A
9030	C
9031	C
9032	T
9033	A
9034	C
9035	T
9036	C
9037	A
9038	T
9039	G
9040	C
9041	A
9042	C
9043	C
9044	T
9045	A
9046	A
9047	T
9048	T
9049	G
9050	G
9051	A
9052	A
9053	G
9054	C
9055	G
9056	C
9057	C
9058	A
9059	C
9060	C
9061	C
9062	T
9063	A
9064	G
9065	C
9066	A
9067	A
9068	T
9069	A
9070	T
9071	C
9072	A
9073	A
9074	C
9075	C
9076	A
9077	T
9078	T
9079	A
9080	A
9081	C
9082	C
9083	T
9084	T
9085	C
9086	C
9087	C
9088	T
9089	C
9090	T
9091	A
9092	C
9093	A
9094	C
9095	T
9096	T
9097	A
9098	T
9099	C
9100	A
9101	T
9102	C
9103	T
9104	T
9105	C
9106	A
9107	C
9108	A
9109	A
9110	T
9111	T
9112	C
9113	T
9114	A
9115	A
9116	T
9117	T
9118	C
9119	T
9120	A
9121	C
9122	T
9123	G
9124	A
9125	C
9126	T
9127	A
9128	T
9129	C
9130	C
9131	T
9132	A
9133	G
9134	A
9135	A
9136	A
9137	T
9138	C
9139	G
9140	C
9141	T
9142	G
9143	T
9144	C
9145	G
9146	C
9147	C
9148	T
9149	T
9150	A
9151	A
9152	T
9153	C
9154	C
9155	A
9156	A
9157	G
9158	C
9159	C
9160	T
9161	A
9162	C
9163	G
9164	T
9165	T
9166	T
9167	T
9168	C
9169	A
9170	C
9171	A
9172	C
9173	T
9174	T
9175	C
9176	T
9177	A
9178	G
9179	T
9180	A
9181	A
9182	G
9183	C
9184	C
9185	T
9186	C
9187	T
9188	A
9189	C
9190	C
9191	T
9192	G
9193	C
9194	A
9195	C
9196	G
9197	A
9198	C
9199	A
9200	A
9201	C
9202	A
9203	C
9204	A
9205	T
9206	A
9207	A
9208	T
9209	G
9210	A
9211	C
9212	C
9213	C
9214	A
9215	C
9216	C
9217	A
9218	A
9219	T
9220	C
9221	A
9222	C
9223	A
9224	T
9225	G
9226	C
9227	C
9228	T
9229	A
9230	T
9231	C
9232	A
9233	T
9234	A
9235	T
9236	A
9237	G
9238	T
9239	A
9240	A
9241	A
9242	A
9243	C
9244	C
9245	C
9246	A
9247	G
9248	C
9249	C
9250	C
9251	A
9252	T
9253	G
9254	A
9255	C
9256	C
9257	C
9258	C
9259	T
9260	A
9261	A
9262	C
9263	A
9264	G
9265	G
9266	G
9267	G
9268	C
9269	C
9270	C
9271	T
9272	C
9273	T
9274	C
9275	A
9276	G
9277	C
9278	C
9279	C
9280	T
9281	C
9282	C
9283	T
9284	A
9285	A
9286	T
9287	G
9288	A
9289	C
9290	C
9291	T
9292	C
9293	C
9294	G
9295	G
9296	C
9297	C
9298	T
9299	A
9300	G
9301	C
9302	C
9303	A
9304	T
9305	G
9306	T
9307	G
9308	A
9309	T
9310	T
9311	T
9312	C
9313	A
9314	C
9315	T
9316	T
9317	C
9318	C
9319	A
9320	C
9321	T
9322	C
9323	C
9324	A
9325	T
9326	A
9327	A
9328	C
9329	G
9330	C
9331	T
9332	C
9333	C
9334	T
9335	C
9336	A
9337	T
9338	A
9339	C
9340	T
9341	A
9342	G
9343	G
9344	C
9345	C
9346	T
9347	A
9348	C
9349	T
9350	A
9351	A
9352	C
9353	C
9354	A
9355	A
9356	C
9357	A
9358	C
9359	A
9360	C
9361	T
9362	A
9363	A
9364	C
9365	C
9366	A
9367	T
9368	A
9369	T
9370	A
9371	C
9372	C
9373	A
9374	A
9375	T
9376	G
9377	A
9378	T
9379	G
9380	G
9381	C
9382	G
9383	C
9384	G
9385	A
9386	T
9387	G
9388	T
9389	A
9390	A
9391	C
9392	A
9393	C
9394	G
9395	A
9396	G
9397	A
9398	A
9399	A
9400	G
9401	C
9402	A
9403	C
9404	A
9405	T
9406	A
9407	C
9408	C
9409	A
9410	A
9411	G
9412	G
9413	C
9414	C
9415	A
9416	C
9417	C
9418	A
9419	C
9420	A
9421	C
9422	A
9423	C
9424	C
9425	A
9426	C
9427	C
9428	T
9429	G
9430	T
9431	C
9432	C
9433	A
9434	A
9435	A
9436	A
9437	A
9438	G
9439	G
9440	C
9441	C
9442	T
9443	T
9444	C
9445	G
9446	A
9447	T
9448	A
9449	C
9450	G
9451	G
9452	G
9453	A
9454	T
9455	A
9456	A
9457	T
9458	C
9459	C
9460	T
9461	A
9462	T
9463	T
9464	T
9465	A
9466	T
9467	T
9468	A
9469	C
9470	C
9471	T
9472	C
9473	A
9474	G
9475	A
9476	A
9477	G
9478	T
9479	T
9480	T
9481	T
9482	T
9483	T
9484	T
9485	C
9486	T
9487	T
9488	C
9489	G
9490	C
9491	A
9492	G
9493	G
9494	A
9495	T
9496	T
9497	T
9498	T
9499	T
9500	C
9501	T
9502	G
9503	A
9504	G
9505	C
9506	C
9507	T
9508	T
9509	T
9510	T
9511	A
9512	C
9513	C
9514	A
9515	C
9516	T
9517	C
9518	C
9519	A
9520	G
9521	C
9522	C
9523	T
9524	A
9525	G
9526	C
9527	C
9528	C
9529	C
9530	T
9531	A
9532	C
9533	C
9534	C
9535	C
9536	C
9537	C
9538	A
9539	A
9540	T
9541	T
9542	A
9543	G
9544	G
9545	A
9546	G
9547	G
9548	G
9549	C
9550	A
9551	C
9552	T
9553	G
9554	G
9555	C
9556	C
9557	C
9558	C
9559	C
9560	A
9561	A
9562	C
9563	A
9564	G
9565	G
9566	C
9567	A
9568	T
9569	C
9570	A
9571	C
9572	C
9573	C
9574	C
9575	G
9576	C
9577	T
9578	A
9579	A
9580	A
9581	T
9582	C
9583	C
9584	C
9585	C
9586	T
9587	A
9588	G
9589	A
9590	A
9591	G
9592	T
9593	C
9594	C
9595	C
9596	A
9597	C
9598	T
9599	C
9600	C
9601	T
9602	A
9603	A
9604	A
9605	C
9606	A
9607	C
9608	A
9609	T
9610	C
9611	C
9612	G
9613	T
9614	A
9615	T
9616	T
9617	A
9618	C
9619	T
9620	C
9621	G
9622	C
9623	A
9624	T
9625	C
9626	A
9627	G
9628	G
9629	A
9630	G
9631	T
9632	A
9633	T
9634	C
9635	A
9636	A
9637	T
9638	C
9639	A
9640	C
9641	C
9642	T
9643	G
9644	A
9645	G
9646	C
9647	T
9648	C
9649	A
9650	C
9651	C
9652	A
9653	T
9654	A
9655	G
9656	T
9657	C
9658	T
9659	A
9660	A
9661	T
9662	A
9663	G
9664	A
9665	A
9666	A
9667	A
9668	C
9669	A
9670	A
9671	C
9672	C
9673	G
9674	A
9675	A
9676	A
9677	C
9678	C
9679	A
9680	A
9681	A
9682	T
9683	A
9684	A
9685	T
9686	T
9687	C
9688	A
9689	A
9690	G
9691	C
9692	A
9693	C
9694	T
9695	G
9696	C
9697	T
9698	T
9699	A
9700	T
9701	T
9702	A
9703	C
9704	A
9705	A
9706	T
9707	T
9708	T
9709	T
9710	A
9711	C
9712	T
9713	G
9714	G
9715	G
9716	T
9717	C
9718	T
9719	C
9720	T
9721	A
9722	T
9723	T
9724	T
9725	T
9726	A
9727	C
9728	C
9729	C
9730	T
9731	C
9732	C
9733	T
9734	A
9735	C
9736	A
9737	A
9738	G
9739	C
9740	C
9741	T
9742	C
9743	A
9744	G
9745	A
9746	G
9747	T
9748	A
9749	C
9750	T
9751	T
9752	C
9753	G
9754	A
9755	G
9756	T
9757	C
9758	T
9759	C
9760	C
9761	C
9762	T
9763	T
9764	C
9765	A
9766	C
9767	C
9768	A
9769	T
9770	T
9771	T
9772	C
9773	C
9774	G
9775	A
9776	C
9777	G
9778	G
9779	C
9780	A
9781	T
9782	C
9783	T
9784	A
9785	C
9786	G
9787	G
9788	C
9789	T
9790	C
9791	A
9792	A
9793	C
9794	A
9795	T
9796	T
9797	T
9798	T
9799	T
9800	T
9801	G
9802	T
9803	A
9804	G
9805	C
9806	C
9807	A
9808	C
9809	A
9810	G
9811	G
9812	C
9813	T
9814	T
9815	C
9816	C
9817	A
9818	C
9819	G
9820	G
9821	A
9822	C
9823	T
9824	T
9825	C
9826	A
9827	C
9828	G
9829	T
9830	C
9831	A
9832	T
9833	T
9834	A
9835	T
9836	T
9837	G
9838	G
9839	C
9840	T
9841	C
9842	A
9843	A
9844	C
9845	T
9846	T
9847	T
9848	C
9849	C
9850	T
9851	C
9852	A
9853	C
9854	T
9855	A
9856	T
9857	C
9858	T
9859	G
9860	C
9861	T
9862	T
9863	C
9864	A
9865	T
9866	C
9867	C
9868	G
9869	C
9870	C
9871	A
9872	A
9873	C
9874	T
9875	A
9876	A
9877	T
9878	A
9879	T
9880	T
9881	T
9882	C
9883	A
9884	C
9885	T
9886	T
9887	T
9888	A
9889	C
9890	A
9891	T
9892	C
9893	C
9894	A
9895	A
9896	A
9897	C
9898	A
9899	T
9900	C
9901	A
9902	C
9903	T
9904	T
9905	T
9906	G
9907	G
9908	C
9909	T
9910	T
9911	C
9912	G
9913	A
9914	A
9915	G
9916	C
9917	C
9918	G
9919	C
9920	C
9921	G
9922	C
9923	C
9924	T
9925	G
9926	A
9927	T
9928	A
9929	C
9930	T
9931	G
9932	G
9933	C
9934	A
9935	T
9936	T
9937	T
9938	T
9939	G
9940	T
9941	A
9942	G
9943	A
9944	T
9945	G
9946	T
9947	G
9948	G
9949	T
9950	T
9951	T
9952	G
9953	A
9954	C
9955	T
9956	A
9957	T
9958	T
9959	T
9960	C
9961	T
9962	G
9963	T
9964	A
9965	T
9966	G
9967	T
9968	C
9969	T
9970	C
9971	C
9972	A
9973	T
9974	C
9975	T
9976	A
9977	T
9978	T
9979	G
9980	A
9981	T
9982	G
9983	A
9984	G
9985	G
9986	G
9987	T
9988	C
9989	T
9990	T
9991	A
9992	C
9993	T
9994	C
9995	T
9996	T
9997	T
9998	T
9999	A
10000	G
10001	T
10002	A
10003	T
10004	A
10005	A
10006	A
10007	T
10008	A
10009	G
10010	T
10011	A
10012	C
10013	C
10014	G
10015	T
10016	T
10017	A
10018	A
10019	C
10020	T
10021	T
10022	C
10023	C
10024	A
10025	A
10026	T
10027	T
10028	A
10029	A
10030	C
10031	T
10032	A
10033	G
10034	T
10035	T
10036	T
10037	T
10038	G
10039	A
10040	C
10041	A
10042	A
10043	C
10044	A
10045	T
10046	T
10047	C
10048	A
10049	A
10050	A
10051	A
10052	A
10053	A
10054	G
10055	A
10056	G
10057	T
10058	A
10059	A
10060	T
10061	A
10062	A
10063	A
10064	C
10065	T
10066	T
10067	C
10068	G
10069	C
10070	C
10071	T
10072	T
10073	A
10074	A
10075	T
10076	T
10077	T
10078	T
10079	A
10080	A
10081	T
10082	A
10083	A
10084	T
10085	C
10086	A
10087	A
10088	C
10089	A
10090	C
10091	C
10092	C
10093	T
10094	C
10095	C
10096	T
10097	A
10098	G
10099	C
10100	C
10101	T
10102	T
10103	A
10104	C
10105	T
10106	A
10107	C
10108	T
10109	A
10110	A
10111	T
10112	A
10113	A
10114	T
10115	T
10116	A
10117	T
10118	T
10119	A
10120	C
10121	A
10122	T
10123	T
10124	T
10125	T
10126	G
10127	A
10128	C
10129	T
10130	A
10131	C
10132	C
10133	A
10134	C
10135	A
10136	A
10137	C
10138	T
10139	C
10140	A
10141	A
10142	C
10143	G
10144	G
10145	C
10146	T
10147	A
10148	C
10149	A
10150	T
10151	A
10152	G
10153	A
10154	A
10155	A
10156	A
10157	A
10158	T
10159	C
10160	C
10161	A
10162	C
10163	C
10164	C
10165	C
10166	T
10167	T
10168	A
10169	C
10170	G
10171	A
10172	G
10173	T
10174	G
10175	C
10176	G
10177	G
10178	C
10179	T
10180	T
10181	C
10182	G
10183	A
10184	C
10185	C
10186	C
10187	T
10188	A
10189	T
10190	A
10191	T
10192	C
10193	C
10194	C
10195	C
10196	C
10197	G
10198	C
10199	C
10200	C
10201	G
10202	C
10203	G
10204	T
10205	C
10206	C
10207	C
10208	T
10209	T
10210	T
10211	C
10212	T
10213	C
10214	C
10215	A
10216	T
10217	A
10218	A
10219	A
10220	A
10221	T
10222	T
10223	C
10224	T
10225	T
10226	C
10227	T
10228	T
10229	A
10230	G
10231	T
10232	A
10233	G
10234	C
10235	T
10236	A
10237	T
10238	T
10239	A
10240	C
10241	C
10242	T
10243	T
10244	C
10245	T
10246	T
10247	A
10248	T
10249	T
10250	A
10251	T
10252	T
10253	T
10254	G
10255	A
10256	T
10257	C
10258	T
10259	A
10260	G
10261	A
10262	A
10263	A
10264	T
10265	T
10266	G
10267	C
10268	C
10269	C
10270	T
10271	C
10272	C
10273	T
10274	T
10275	T
10276	T
10277	A
10278	C
10279	C
10280	C
10281	C
10282	T
10283	A
10284	C
10285	C
10286	A
10287	T
10288	G
10289	A
10290	G
10291	C
10292	C
10293	C
10294	T
10295	A
10296	C
10297	A
10298	A
10299	A
10300	C
10301	A
10302	A
10303	C
10304	T
10305	A
10306	A
10307	C
10308	C
10309	T
10310	G
10311	C
10312	C
10313	A
10314	C
10315	T
10316	A
10317	A
10318	T
10319	A
10320	G
10321	T
10322	T
10323	A
10324	T
10325	G
10326	T
10327	C
10328	A
10329	T
10330	C
10331	C
10332	C
10333	T
10334	C
10335	T
10336	T
10337	A
10338	T
10339	T
10340	A
10341	A
10342	T
10343	C
10344	A
10345	T
10346	C
10347	A
10348	T
10349	C
10350	C
10351	T
10352	A
10353	G
10354	C
10355	C
10356	C
10357	T
10358	A
10359	A
10360	G
10361	T
10362	C
10363	T
10364	G
10365	G
10366	C
10367	C
10368	T
10369	A
10370	T
10371	G
10372	A
10373	G
10374	T
10375	G
10376	A
10377	C
10378	T
10379	A
10380	C
10381	A
10382	A
10383	A
10384	A
10385	A
10386	G
10387	G
10388	A
10389	T
10390	T
10391	A
10392	G
10393	A
10394	C
10395	T
10396	G
10397	A
10398	A
10399	C
10400	C
10401	G
10402	A
10403	A
10404	T
10405	T
10406	G
10407	G
10408	T
10409	A
10410	T
10411	A
10412	T
10413	A
10414	G
10415	T
10416	T
10417	T
10418	A
10419	A
10420	A
10421	C
10422	A
10423	A
10424	A
10425	A
10426	C
10427	G
10428	A
10429	A
10430	T
10431	G
10432	A
10433	T
10434	T
10435	T
10436	C
10437	G
10438	A
10439	C
10440	T
10441	C
10442	A
10443	T
10444	T
10445	A
10446	A
10447	A
10448	T
10449	T
10450	A
10451	T
10452	G
10453	A
10454	T
10455	A
10456	A
10457	T
10458	C
10459	A
10460	T
10461	A
10462	T
10463	T
10464	T
10465	A
10466	C
10467	C
10468	A
10469	A
10470	A
10471	T
10472	G
10473	C
10474	C
10475	C
10476	C
10477	T
10478	C
10479	A
10480	T
10481	T
10482	T
10483	A
10484	C
10485	A
10486	T
10487	A
10488	A
10489	A
10490	T
10491	A
10492	T
10493	T
10494	A
10495	T
10496	A
10497	C
10498	T
10499	A
10500	G
10501	C
10502	A
10503	T
10504	T
10505	T
10506	A
10507	C
10508	C
10509	A
10510	T
10511	C
10512	T
10513	C
10514	A
10515	C
10516	T
10517	T
10518	C
10519	T
10520	A
10521	G
10522	G
10523	A
10524	A
10525	T
10526	A
10527	C
10528	T
10529	A
10530	G
10531	T
10532	A
10533	T
10534	A
10535	T
10536	C
10537	G
10538	C
10539	T
10540	C
10541	A
10542	C
10543	A
10544	C
10545	C
10546	T
10547	C
10548	A
10549	T
10550	A
10551	T
10552	C
10553	C
10554	T
10555	C
10556	C
10557	C
10558	T
10559	A
10560	C
10561	T
10562	A
10563	T
10564	G
10565	C
10566	C
10567	T
10568	A
10569	G
10570	A
10571	A
10572	G
10573	G
10574	A
10575	A
10576	T
10577	A
10578	A
10579	T
10580	A
10581	C
10582	T
10583	A
10584	T
10585	C
10586	G
10587	C
10588	T
10589	G
10590	T
10591	T
10592	C
10593	A
10594	T
10595	T
10596	A
10597	T
10598	A
10599	G
10600	C
10601	T
10602	A
10603	C
10604	T
10605	C
10606	T
10607	C
10608	A
10609	T
10610	A
10611	A
10612	C
10613	C
10614	C
10615	T
10616	C
10617	A
10618	A
10619	C
10620	A
10621	C
10622	C
10623	C
10624	A
10625	C
10626	T
10627	C
10628	C
10629	C
10630	T
10631	C
10632	T
10633	T
10634	A
10635	G
10636	C
10637	C
10638	A
10639	A
10640	T
10641	A
10642	T
10643	T
10644	G
10645	T
10646	G
10647	C
10648	C
10649	T
10650	A
10651	T
10652	T
10653	G
10654	C
10655	C
10656	A
10657	T
10658	A
10659	C
10660	T
10661	A
10662	G
10663	T
10664	C
10665	T
10666	T
10667	T
10668	G
10669	C
10670	C
10671	G
10672	C
10673	C
10674	T
10675	G
10676	C
10677	G
10678	A
10679	A
10680	G
10681	C
10682	A
10683	G
10684	C
10685	G
10686	G
10687	T
10688	G
10689	G
10690	G
10691	C
10692	C
10693	T
10694	A
10695	G
10696	C
10697	C
10698	C
10699	T
10700	A
10701	C
10702	T
10703	A
10704	G
10705	T
10706	C
10707	T
10708	C
10709	A
10710	A
10711	T
10712	C
10713	T
10714	C
10715	C
10716	A
10717	A
10718	C
10719	A
10720	C
10721	A
10722	T
10723	A
10724	T
10725	G
10726	G
10727	C
10728	C
10729	T
10730	A
10731	G
10732	A
10733	C
10734	T
10735	A
10736	C
10737	G
10738	T
10739	A
10740	C
10741	A
10742	T
10743	A
10744	A
10745	C
10746	C
10747	T
10748	A
10749	A
10750	A
10751	C
10752	C
10753	T
10754	A
10755	C
10756	T
10757	C
10758	C
10759	A
10760	A
10761	T
10762	G
10763	C
10764	T
10765	A
10766	A
10767	A
10768	A
10769	C
10770	T
10771	A
10772	A
10773	T
10774	C
10775	G
10776	T
10777	C
10778	C
10779	C
10780	A
10781	A
10782	C
10783	A
10784	A
10785	T
10786	T
10787	A
10788	T
10789	A
10790	T
10791	T
10792	A
10793	C
10794	T
10795	A
10796	C
10797	C
10798	A
10799	C
10800	T
10801	G
10802	A
10803	C
10804	A
10805	T
10806	G
10807	A
10808	C
10809	T
10810	T
10811	T
10812	C
10813	C
10814	A
10815	A
10816	A
10817	A
10818	A
10819	A
10820	C
10821	A
10822	C
10823	A
10824	T
10825	A
10826	A
10827	T
10828	T
10829	T
10830	G
10831	A
10832	A
10833	T
10834	C
10835	A
10836	A
10837	C
10838	A
10839	C
10840	A
10841	A
10842	C
10843	C
10844	A
10845	C
10846	C
10847	C
10848	A
10849	C
10850	A
10851	G
10852	C
10853	C
10854	T
10855	A
10856	A
10857	T
10858	T
10859	A
10860	T
10861	T
10862	A
10863	G
10864	C
10865	A
10866	T
10867	C
10868	A
10869	T
10870	C
10871	C
10872	C
10873	T
10874	C
10875	T
10876	A
10877	C
10878	T
10879	A
10880	T
10881	T
10882	T
10883	T
10884	T
10885	T
10886	A
10887	A
10888	C
10889	C
10890	A
10891	A
10892	A
10893	T
10894	C
10895	A
10896	A
10897	C
10898	A
10899	A
10900	C
10901	A
10902	A
10903	C
10904	C
10905	T
10906	A
10907	T
10908	T
10909	T
10910	A
10911	G
10912	C
10913	T
10914	G
10915	T
10916	T
10917	C
10918	C
10919	C
10920	C
10921	A
10922	A
10923	C
10924	C
10925	T
10926	T
10927	T
10928	T
10929	C
10930	C
10931	T
10932	C
10933	C
10934	G
10935	A
10936	C
10937	C
10938	C
10939	C
10940	C
10941	T
10942	A
10943	A
10944	C
10945	A
10946	A
10947	C
10948	C
10949	C
10950	C
10951	C
10952	C
10953	T
10954	C
10955	C
10956	T
10957	A
10958	A
10959	T
10960	A
10961	C
10962	T
10963	A
10964	A
10965	C
10966	T
10967	A
10968	C
10969	C
10970	T
10971	G
10972	A
10973	C
10974	T
10975	C
10976	C
10977	T
10978	A
10979	C
10980	C
10981	C
10982	C
10983	T
10984	C
10985	A
10986	C
10987	A
10988	A
10989	T
10990	C
10991	A
10992	T
10993	G
10994	G
10995	C
10996	A
10997	A
10998	G
10999	C
11000	C
11001	A
11002	A
11003	C
11004	G
11005	C
11006	C
11007	A
11008	C
11009	T
11010	T
11011	A
11012	T
11013	C
11014	C
11015	A
11016	G
11017	T
11018	G
11019	A
11020	A
11021	C
11022	C
11023	A
11024	C
11025	T
11026	A
11027	T
11028	C
11029	A
11030	C
11031	G
11032	A
11033	A
11034	A
11035	A
11036	A
11037	A
11038	A
11039	C
11040	T
11041	C
11042	T
11043	A
11044	C
11045	C
11046	T
11047	C
11048	T
11049	C
11050	T
11051	A
11052	T
11053	A
11054	C
11055	T
11056	A
11057	A
11058	T
11059	C
11060	T
11061	C
11062	C
11063	C
11064	T
11065	A
11066	C
11067	A
11068	A
11069	A
11070	T
11071	C
11072	T
11073	C
11074	C
11075	T
11076	T
11077	A
11078	A
11079	T
11080	T
11081	A
11082	T
11083	A
11084	A
11085	C
11086	A
11087	T
11088	T
11089	C
11090	A
11091	C
11092	A
11093	G
11094	C
11095	C
11096	A
11097	C
11098	A
11099	G
11100	A
11101	A
11102	C
11103	T
11104	A
11105	A
11106	T
11107	C
11108	A
11109	T
11110	A
11111	T
11112	T
11113	T
11114	T
11115	A
11116	T
11117	A
11118	T
11119	C
11120	T
11121	T
11122	C
11123	T
11124	T
11125	C
11126	G
11127	A
11128	A
11129	A
11130	C
11131	C
11132	A
11133	C
11134	A
11135	C
11136	T
11137	T
11138	A
11139	T
11140	C
11141	C
11142	C
11143	C
11144	A
11145	C
11146	C
11147	T
11148	T
11149	G
11150	G
11151	C
11152	T
11153	A
11154	T
11155	C
11156	A
11157	T
11158	C
11159	A
11160	C
11161	C
11162	C
11163	G
11164	A
11165	T
11166	G
11167	A
11168	G
11169	G
11170	C
11171	A
11172	A
11173	C
11174	C
11175	A
11176	G
11177	C
11178	C
11179	A
11180	G
11181	A
11182	A
11183	C
11184	G
11185	C
11186	C
11187	T
11188	G
11189	A
11190	A
11191	C
11192	G
11193	C
11194	A
11195	G
11196	G
11197	C
11198	A
11199	C
11200	A
11201	T
11202	A
11203	C
11204	T
11205	T
11206	C
11207	C
11208	T
11209	A
11210	T
11211	T
11212	C
11213	T
11214	A
11215	C
11216	A
11217	C
11218	C
11219	C
11220	T
11221	A
11222	G
11223	T
11224	A
11225	G
11226	G
11227	C
11228	T
11229	C
11230	C
11231	C
11232	T
11233	T
11234	C
11235	C
11236	C
11237	C
11238	T
11239	A
11240	C
11241	T
11242	C
11243	A
11244	T
11245	C
11246	G
11247	C
11248	A
11249	C
11250	T
11251	A
11252	A
11253	T
11254	T
11255	T
11256	A
11257	C
11258	A
11259	C
11260	T
11261	C
11262	A
11263	C
11264	A
11265	A
11266	C
11267	A
11268	C
11269	C
11270	C
11271	T
11272	A
11273	G
11274	G
11275	C
11276	T
11277	C
11278	A
11279	C
11280	T
11281	A
11282	A
11283	A
11284	C
11285	A
11286	T
11287	T
11288	C
11289	T
11290	A
11291	C
11292	T
11293	A
11294	C
11295	T
11296	C
11297	A
11298	C
11299	T
11300	C
11301	T
11302	C
11303	A
11304	C
11305	T
11306	G
11307	C
11308	C
11309	C
11310	A
11311	A
11312	G
11313	A
11314	A
11315	C
11316	T
11317	A
11318	T
11319	C
11320	A
11321	A
11322	A
11323	C
11324	T
11325	C
11326	C
11327	T
11328	G
11329	A
11330	G
11331	C
11332	C
11333	A
11334	A
11335	C
11336	A
11337	A
11338	C
11339	T
11340	T
11341	A
11342	A
11343	T
11344	A
11345	T
11346	G
11347	A
11348	C
11349	T
11350	A
11351	G
11352	C
11353	T
11354	T
11355	A
11356	C
11357	A
11358	C
11359	A
11360	A
11361	T
11362	A
11363	G
11364	C
11365	T
11366	T
11367	T
11368	T
11369	A
11370	T
11371	A
11372	G
11373	T
11374	A
11375	A
11376	A
11377	G
11378	A
11379	T
11380	A
11381	C
11382	C
11383	T
11384	C
11385	T
11386	T
11387	T
11388	A
11389	C
11390	G
11391	G
11392	A
11393	C
11394	T
11395	C
11396	C
11397	A
11398	C
11399	T
11400	T
11401	A
11402	T
11403	G
11404	A
11405	C
11406	T
11407	C
11408	C
11409	C
11410	T
11411	A
11412	A
11413	A
11414	G
11415	C
11416	C
11417	C
11418	A
11419	T
11420	G
11421	T
11422	C
11423	G
11424	A
11425	A
11426	G
11427	C
11428	C
11429	C
11430	C
11431	C
11432	A
11433	T
11434	C
11435	G
11436	C
11437	T
11438	G
11439	G
11440	G
11441	T
11442	C
11443	A
11444	A
11445	T
11446	A
11447	G
11448	T
11449	A
11450	C
11451	T
11452	T
11453	G
11454	C
11455	C
11456	G
11457	C
11458	A
11459	G
11460	T
11461	A
11462	C
11463	T
11464	C
11465	T
11466	T
11467	A
11468	A
11469	A
11470	A
11471	C
11472	T
11473	A
11474	G
11475	G
11476	C
11477	G
11478	G
11479	C
11480	T
11481	A
11482	T
11483	G
11484	G
11485	T
11486	A
11487	T
11488	A
11489	A
11490	T
11491	A
11492	C
11493	G
11494	C
11495	C
11496	T
11497	C
11498	A
11499	C
11500	A
11501	C
11502	T
11503	C
11504	A
11505	T
11506	T
11507	C
11508	T
11509	C
11510	A
11511	A
11512	C
11513	C
11514	C
11515	C
11516	C
11517	T
11518	G
11519	A
11520	C
11521	A
11522	A
11523	A
11524	A
11525	C
11526	A
11527	C
11528	A
11529	T
11530	A
11531	G
11532	C
11533	C
11534	T
11535	A
11536	C
11537	C
11538	C
11539	C
11540	T
11541	T
11542	C
11543	C
11544	T
11545	T
11546	G
11547	T
11548	A
11549	C
11550	T
11551	A
11552	T
11553	C
11554	C
11555	C
11556	T
11557	A
11558	T
11559	G
11560	A
11561	G
11562	G
11563	C
11564	A
11565	T
11566	A
11567	A
11568	T
11569	T
11570	A
11571	T
11572	A
11573	A
11574	C
11575	A
11576	A
11577	G
11578	C
11579	T
11580	C
11581	C
11582	A
11583	T
11584	C
11585	T
11586	G
11587	C
11588	C
11589	T
11590	A
11591	C
11592	G
11593	A
11594	C
11595	A
11596	A
11597	A
11598	C
11599	A
11600	G
11601	A
11602	C
11603	C
11604	T
11605	A
11606	A
11607	A
11608	A
11609	T
11610	C
11611	G
11612	C
11613	T
11614	C
11615	A
11616	T
11617	T
11618	G
11619	C
11620	A
11621	T
11622	A
11623	C
11624	T
11625	C
11626	T
11627	T
11628	C
11629	A
11630	A
11631	T
11632	C
11633	A
11634	G
11635	C
11636	C
11637	A
11638	C
11639	A
11640	T
11641	A
11642	G
11643	C
11644	C
11645	C
11646	T
11647	C
11648	G
11649	T
11650	A
11651	G
11652	T
11653	A
11654	A
11655	C
11656	A
11657	G
11658	C
11659	C
11660	A
11661	T
11662	T
11663	C
11664	T
11665	C
11666	A
11667	T
11668	C
11669	C
11670	A
11671	A
11672	A
11673	C
11674	C
11675	C
11676	C
11677	C
11678	T
11679	G
11680	A
11681	A
11682	G
11683	C
11684	T
11685	T
11686	C
11687	A
11688	C
11689	C
11690	G
11691	G
11692	C
11693	G
11694	C
11695	A
11696	G
11697	T
11698	C
11699	A
11700	T
11701	T
11702	C
11703	T
11704	C
11705	A
11706	T
11707	A
11708	A
11709	T
11710	C
11711	G
11712	C
11713	C
11714	C
11715	A
11716	C
11717	G
11718	G
11719	G
11720	C
11721	T
11722	T
11723	A
11724	C
11725	A
11726	T
11727	C
11728	C
11729	T
11730	C
11731	A
11732	T
11733	T
11734	A
11735	C
11736	T
11737	A
11738	T
11739	T
11740	C
11741	T
11742	G
11743	C
11744	C
11745	T
11746	A
11747	G
11748	C
11749	A
11750	A
11751	A
11752	C
11753	T
11754	C
11755	A
11756	A
11757	A
11758	C
11759	T
11760	A
11761	C
11762	G
11763	A
11764	A
11765	C
11766	G
11767	C
11768	A
11769	C
11770	T
11771	C
11772	A
11773	C
11774	A
11775	G
11776	T
11777	C
11778	G
11779	C
11780	A
11781	T
11782	C
11783	A
11784	T
11785	A
11786	A
11787	T
11788	C
11789	C
11790	T
11791	C
11792	T
11793	C
11794	T
11795	C
11796	A
11797	A
11798	G
11799	G
11800	A
11801	C
11802	T
11803	T
11804	C
11805	A
11806	A
11807	A
11808	C
11809	T
11810	C
11811	T
11812	A
11813	C
11814	T
11815	C
11816	C
11817	C
11818	A
11819	C
11820	T
11821	A
11822	A
11823	T
11824	A
11825	G
11826	C
11827	T
11828	T
11829	T
11830	T
11831	T
11832	G
11833	A
11834	T
11835	G
11836	A
11837	C
11838	T
11839	T
11840	C
11841	T
11842	A
11843	G
11844	C
11845	A
11846	A
11847	G
11848	C
11849	C
11850	T
11851	C
11852	G
11853	C
11854	T
11855	A
11856	A
11857	C
11858	C
11859	T
11860	C
11861	G
11862	C
11863	C
11864	T
11865	T
11866	A
11867	C
11868	C
11869	C
11870	C
11871	C
11872	C
11873	A
11874	C
11875	T
11876	A
11877	T
11878	T
11879	A
11880	A
11881	C
11882	C
11883	T
11884	A
11885	C
11886	T
11887	G
11888	G
11889	G
11890	A
11891	G
11892	A
11893	A
11894	C
11895	T
11896	C
11897	T
11898	C
11899	T
11900	G
11901	T
11902	G
11903	C
11904	T
11905	A
11906	G
11907	T
11908	A
11909	A
11910	C
11911	C
11912	A
11913	C
11914	G
11915	T
11916	T
11917	C
11918	T
11919	C
11920	C
11921	T
11922	G
11923	A
11924	T
11925	C
11926	A
11927	A
11928	A
11929	T
11930	A
11931	T
11932	C
11933	A
11934	C
11935	T
11936	C
11937	T
11938	C
11939	C
11940	T
11941	A
11942	C
11943	T
11944	T
11945	A
11946	C
11947	A
11948	G
11949	G
11950	A
11951	C
11952	T
11953	C
11954	A
11955	A
11956	C
11957	A
11958	T
11959	A
11960	C
11961	T
11962	A
11963	G
11964	T
11965	C
11966	A
11967	C
11968	A
11969	G
11970	C
11971	C
11972	C
11973	T
11974	A
11975	T
11976	A
11977	C
11978	T
11979	C
11980	C
11981	C
11982	T
11983	C
11984	T
11985	A
11986	C
11987	A
11988	T
11989	A
11990	T
11991	T
11992	T
11993	A
11994	C
11995	C
11996	A
11997	C
11998	A
11999	A
12000	C
12001	A
12002	C
12003	A
12004	A
12005	T
12006	G
12007	G
12008	G
12009	G
12010	C
12011	T
12012	C
12013	A
12014	C
12015	T
12016	C
12017	A
12018	C
12019	C
12020	C
12021	A
12022	C
12023	C
12024	A
12025	C
12026	A
12027	T
12028	T
12029	A
12030	A
12031	C
12032	A
12033	A
12034	C
12035	A
12036	T
12037	A
12038	A
12039	A
12040	A
12041	C
12042	C
12043	C
12044	T
12045	C
12046	A
12047	T
12048	T
12049	C
12050	A
12051	C
12052	A
12053	C
12054	G
12055	A
12056	G
12057	A
12058	A
12059	A
12060	A
12061	C
12062	A
12063	C
12064	C
12065	C
12066	T
12067	C
12068	A
12069	T
12070	G
12071	T
12072	T
12073	C
12074	A
12075	T
12076	A
12077	C
12078	A
12079	C
12080	C
12081	T
12082	A
12083	T
12084	C
12085	C
12086	C
12087	C
12088	C
12089	A
12090	T
12091	T
12092	C
12093	T
12094	C
12095	C
12096	T
12097	C
12098	C
12099	T
12100	A
12101	T
12102	C
12103	C
12104	C
12105	T
12106	C
12107	A
12108	A
12109	C
12110	C
12111	C
12112	C
12113	G
12114	A
12115	C
12116	A
12117	T
12118	C
12119	A
12120	T
12121	T
12122	A
12123	C
12124	C
12125	G
12126	G
12127	G
12128	T
12129	T
12130	T
12131	T
12132	C
12133	C
12134	T
12135	C
12136	T
12137	T
12138	G
12139	T
12140	A
12141	A
12142	A
12143	T
12144	A
12145	T
12146	A
12147	G
12148	T
12149	T
12150	T
12151	A
12152	A
12153	C
12154	C
12155	A
12156	A
12157	A
12158	A
12159	C
12160	A
12161	T
12162	C
12163	A
12164	G
12165	A
12166	T
12167	T
12168	G
12169	T
12170	G
12171	A
12172	A
12173	T
12174	C
12175	T
12176	G
12177	A
12178	C
12179	A
12180	A
12181	C
12182	A
12183	G
12184	A
12185	G
12186	G
12187	C
12188	T
12189	T
12190	A
12191	C
12192	G
12193	A
12194	C
12195	C
12196	C
12197	C
12198	T
12199	T
12200	A
12201	T
12202	T
12203	T
12204	A
12205	C
12206	C
12207	G
12208	A
12209	G
12210	A
12211	A
12212	A
12213	G
12214	C
12215	T
12216	C
12217	A
12218	C
12219	A
12220	A
12221	G
12222	A
12223	A
12224	C
12225	T
12226	G
12227	C
12228	T
12229	A
12230	A
12231	C
12232	T
12233	C
12234	A
12235	T
12236	G
12237	C
12238	C
12239	C
12240	C
12241	C
12242	A
12243	T
12244	G
12245	T
12246	C
12247	T
12248	A
12249	A
12250	C
12251	A
12252	A
12253	C
12254	A
12255	T
12256	G
12257	G
12258	C
12259	T
12260	T
12261	T
12262	C
12263	T
12264	C
12265	A
12266	A
12267	C
12268	T
12269	T
12270	T
12271	T
12272	A
12273	A
12274	A
12275	G
12276	G
12277	A
12278	T
12279	A
12280	A
12281	C
12282	A
12283	G
12284	C
12285	T
12286	A
12287	T
12288	C
12289	C
12290	A
12291	T
12292	T
12293	G
12294	G
12295	T
12296	C
12297	T
12298	T
12299	A
12300	G
12301	G
12302	C
12303	C
12304	C
12305	C
12306	A
12307	A
12308	A
12309	A
12310	A
12311	T
12312	T
12313	T
12314	T
12315	G
12316	G
12317	T
12318	G
12319	C
12320	A
12321	A
12322	C
12323	T
12324	C
12325	C
12326	A
12327	A
12328	A
12329	T
12330	A
12331	A
12332	A
12333	A
12334	G
12335	T
12336	A
12337	A
12338	T
12339	A
12340	A
12341	C
12342	C
12343	A
12344	T
12345	G
12346	C
12347	A
12348	C
12349	A
12350	C
12351	T
12352	A
12353	C
12354	T
12355	A
12356	T
12357	A
12358	A
12359	C
12360	C
12361	A
12362	C
12363	C
12364	C
12365	T
12366	A
12367	A
12368	C
12369	C
12370	C
12371	T
12372	G
12373	A
12374	C
12375	T
12376	T
12377	C
12378	C
12379	C
12380	T
12381	A
12382	A
12383	T
12384	T
12385	C
12386	C
12387	C
12388	C
12389	C
12390	C
12391	A
12392	T
12393	C
12394	C
12395	T
12396	T
12397	A
12398	C
12399	C
12400	A
12401	C
12402	C
12403	C
12404	T
12405	C
12406	G
12407	T
12408	T
12409	A
12410	A
12411	C
12412	C
12413	C
12414	T
12415	A
12416	A
12417	C
12418	A
12419	A
12420	A
12421	A
12422	A
12423	A
12424	A
12425	A
12426	C
12427	T
12428	C
12429	A
12430	T
12431	A
12432	C
12433	C
12434	C
12435	C
12436	C
12437	A
12438	T
12439	T
12440	A
12441	T
12442	G
12443	T
12444	A
12445	A
12446	A
12447	A
12448	T
12449	C
12450	C
12451	A
12452	T
12453	T
12454	G
12455	T
12456	C
12457	G
12458	C
12459	A
12460	T
12461	C
12462	C
12463	A
12464	C
12465	C
12466	T
12467	T
12468	T
12469	A
12470	T
12471	T
12472	A
12473	T
12474	C
12475	A
12476	G
12477	T
12478	C
12479	T
12480	C
12481	T
12482	T
12483	C
12484	C
12485	C
12486	C
12487	A
12488	C
12489	A
12490	A
12491	C
12492	A
12493	A
12494	T
12495	A
12496	T
12497	T
12498	C
12499	A
12500	T
12501	G
12502	T
12503	G
12504	C
12505	C
12506	T
12507	A
12508	G
12509	A
12510	C
12511	C
12512	A
12513	A
12514	G
12515	A
12516	A
12517	G
12518	T
12519	T
12520	A
12521	T
12522	T
12523	A
12524	T
12525	C
12526	T
12527	C
12528	G
12529	A
12530	A
12531	C
12532	T
12533	G
12534	A
12535	C
12536	A
12537	C
12538	T
12539	G
12540	A
12541	G
12542	C
12543	C
12544	A
12545	C
12546	A
12547	A
12548	C
12549	C
12550	C
12551	A
12552	A
12553	A
12554	C
12555	A
12556	A
12557	C
12558	C
12559	C
12560	A
12561	G
12562	C
12563	T
12564	C
12565	T
12566	C
12567	C
12568	C
12569	T
12570	A
12571	A
12572	G
12573	C
12574	T
12575	T
12576	C
12577	A
12578	A
12579	A
12580	C
12581	T
12582	A
12583	G
12584	A
12585	C
12586	T
12587	A
12588	C
12589	T
12590	T
12591	C
12592	T
12593	C
12594	C
12595	A
12596	T
12597	A
12598	A
12599	T
12600	A
12601	T
12602	T
12603	C
12604	A
12605	T
12606	C
12607	C
12608	C
12609	T
12610	G
12611	T
12612	A
12613	G
12614	C
12615	A
12616	T
12617	T
12618	G
12619	T
12620	T
12621	C
12622	G
12623	T
12624	T
12625	A
12626	C
12627	A
12628	T
12629	G
12630	G
12631	T
12632	C
12633	C
12634	A
12635	T
12636	C
12637	A
12638	T
12639	A
12640	G
12641	A
12642	A
12643	T
12644	T
12645	C
12646	T
12647	C
12648	A
12649	C
12650	T
12651	G
12652	T
12653	G
12654	A
12655	T
12656	A
12657	T
12658	A
12659	T
12660	A
12661	A
12662	A
12663	C
12664	T
12665	C
12666	A
12667	G
12668	A
12669	C
12670	C
12671	C
12672	A
12673	A
12674	A
12675	C
12676	A
12677	T
12678	T
12679	A
12680	A
12681	T
12682	C
12683	A
12684	G
12685	T
12686	T
12687	C
12688	T
12689	T
12690	C
12691	A
12692	A
12693	A
12694	T
12695	A
12696	T
12697	C
12698	T
12699	A
12700	C
12701	T
12702	C
12703	A
12704	T
12705	C
12706	T
12707	T
12708	C
12709	C
12710	T
12711	A
12712	A
12713	T
12714	T
12715	A
12716	C
12717	C
12718	A
12719	T
12720	A
12721	C
12722	T
12723	A
12724	A
12725	T
12726	C
12727	T
12728	T
12729	A
12730	G
12731	T
12732	T
12733	A
12734	C
12735	C
12736	G
12737	C
12738	T
12739	A
12740	A
12741	C
12742	A
12743	A
12744	C
12745	C
12746	T
12747	A
12748	T
12749	T
12750	C
12751	C
12752	A
12753	A
12754	C
12755	T
12756	G
12757	T
12758	T
12759	C
12760	A
12761	T
12762	C
12763	G
12764	G
12765	C
12766	T
12767	G
12768	A
12769	G
12770	A
12771	G
12772	G
12773	G
12774	C
12775	G
12776	T
12777	A
12778	G
12779	G
12780	A
12781	A
12782	T
12783	T
12784	A
12785	T
12786	A
12787	T
12788	C
12789	C
12790	T
12791	T
12792	C
12793	T
12794	T
12795	G
12796	C
12797	T
12798	C
12799	A
12800	T
12801	C
12802	A
12803	G
12804	T
12805	T
12806	G
12807	A
12808	T
12809	G
12810	A
12811	T
12812	A
12813	C
12814	G
12815	C
12816	C
12817	C
12818	G
12819	A
12820	G
12821	C
12822	A
12823	G
12824	A
12825	T
12826	G
12827	C
12828	C
12829	A
12830	A
12831	C
12832	A
12833	C
12834	A
12835	G
12836	C
12837	A
12838	G
12839	C
12840	C
12841	A
12842	T
12843	T
12844	C
12845	A
12846	A
12847	G
12848	C
12849	A
12850	A
12851	T
12852	C
12853	C
12854	T
12855	A
12856	T
12857	A
12858	C
12859	A
12860	A
12861	C
12862	C
12863	G
12864	T
12865	A
12866	T
12867	C
12868	G
12869	G
12870	C
12871	G
12872	A
12873	T
12874	A
12875	T
12876	C
12877	G
12878	G
12879	T
12880	T
12881	T
12882	C
12883	A
12884	T
12885	C
12886	C
12887	T
12888	C
12889	G
12890	C
12891	C
12892	T
12893	T
12894	A
12895	G
12896	C
12897	A
12898	T
12899	G
12900	A
12901	T
12902	T
12903	T
12904	A
12905	T
12906	C
12907	C
12908	T
12909	A
12910	C
12911	A
12912	C
12913	T
12914	C
12915	C
12916	A
12917	A
12918	C
12919	T
12920	C
12921	A
12922	T
12923	G
12924	A
12925	G
12926	A
12927	C
12928	C
12929	C
12930	A
12931	C
12932	A
12933	A
12934	C
12935	A
12936	A
12937	A
12938	T
12939	A
12940	G
12941	C
12942	C
12943	C
12944	T
12945	T
12946	C
12947	T
12948	A
12949	A
12950	A
12951	C
12952	G
12953	C
12954	T
12955	A
12956	A
12957	T
12958	C
12959	C
12960	A
12961	A
12962	G
12963	C
12964	C
12965	T
12966	C
12967	A
12968	C
12969	C
12970	C
12971	C
12972	A
12973	C
12974	T
12975	A
12976	C
12977	T
12978	A
12979	G
12980	G
12981	C
12982	C
12983	T
12984	C
12985	C
12986	T
12987	C
12988	C
12989	T
12990	A
12991	G
12992	C
12993	A
12994	G
12995	C
12996	A
12997	G
12998	C
12999	A
13000	G
13001	G
13002	C
13003	A
13004	A
13005	A
13006	T
13007	C
13008	A
13009	G
13010	C
13011	C
13012	C
13013	A
13014	A
13015	T
13016	T
13017	A
13018	G
13019	G
13020	T
13021	C
13022	T
13023	C
13024	C
13025	A
13026	C
13027	C
13028	C
13029	C
13030	T
13031	G
13032	A
13033	C
13034	T
13035	C
13036	C
13037	C
13038	C
13039	T
13040	C
13041	A
13042	G
13043	C
13044	C
13045	A
13046	T
13047	A
13048	G
13049	A
13050	A
13051	G
13052	G
13053	C
13054	C
13055	C
13056	C
13057	A
13058	C
13059	C
13060	C
13061	C
13062	A
13063	G
13064	T
13065	C
13066	T
13067	C
13068	A
13069	G
13070	C
13071	C
13072	C
13073	T
13074	A
13075	C
13076	T
13077	C
13078	C
13079	A
13080	C
13081	T
13082	C
13083	A
13084	A
13085	G
13086	C
13087	A
13088	C
13089	T
13090	A
13091	T
13092	A
13093	G
13094	T
13095	T
13096	G
13097	T
13098	A
13099	G
13100	C
13101	A
13102	G
13103	G
13104	A
13105	A
13106	T
13107	C
13108	T
13109	T
13110	C
13111	T
13112	T
13113	A
13114	C
13115	T
13116	C
13117	A
13118	T
13119	C
13120	C
13121	G
13122	C
13123	T
13124	T
13125	C
13126	C
13127	A
13128	C
13129	C
13130	C
13131	C
13132	C
13133	T
13134	A
13135	G
13136	C
13137	A
13138	G
13139	A
13140	A
13141	A
13142	A
13143	T
13144	A
13145	G
13146	C
13147	C
13148	C
13149	A
13150	C
13151	T
13152	A
13153	A
13154	T
13155	C
13156	C
13157	A
13158	A
13159	A
13160	C
13161	T
13162	C
13163	T
13164	A
13165	A
13166	C
13167	A
13168	C
13169	T
13170	A
13171	T
13172	G
13173	C
13174	T
13175	T
13176	A
13177	G
13178	G
13179	C
13180	G
13181	C
13182	T
13183	A
13184	T
13185	C
13186	A
13187	C
13188	C
13189	A
13190	C
13191	T
13192	C
13193	T
13194	G
13195	T
13196	T
13197	C
13198	G
13199	C
13200	A
13201	G
13202	C
13203	A
13204	G
13205	T
13206	C
13207	T
13208	G
13209	C
13210	G
13211	C
13212	C
13213	C
13214	T
13215	T
13216	A
13217	C
13218	A
13219	C
13220	A
13221	A
13222	A
13223	A
13224	T
13225	G
13226	A
13227	C
13228	A
13229	T
13230	C
13231	A
13232	A
13233	A
13234	A
13235	A
13236	A
13237	A
13238	T
13239	C
13240	G
13241	T
13242	A
13243	G
13244	C
13245	C
13246	T
13247	T
13248	C
13249	T
13250	C
13251	C
13252	A
13253	C
13254	T
13255	T
13256	C
13257	A
13258	A
13259	G
13260	T
13261	C
13262	A
13263	A
13264	C
13265	T
13266	A
13267	G
13268	G
13269	A
13270	C
13271	T
13272	C
13273	A
13274	T
13275	A
13276	A
13277	T
13278	A
13279	G
13280	T
13281	T
13282	A
13283	C
13284	A
13285	A
13286	T
13287	C
13288	G
13289	G
13290	C
13291	A
13292	T
13293	C
13294	A
13295	A
13296	C
13297	C
13298	A
13299	A
13300	C
13301	C
13302	A
13303	C
13304	A
13305	C
13306	C
13307	T
13308	A
13309	G
13310	C
13311	A
13312	T
13313	T
13314	C
13315	C
13316	T
13317	G
13318	C
13319	A
13320	C
13321	A
13322	T
13323	C
13324	T
13325	G
13326	T
13327	A
13328	C
13329	C
13330	C
13331	A
13332	C
13333	G
13334	C
13335	C
13336	T
13337	T
13338	C
13339	T
13340	T
13341	C
13342	A
13343	A
13344	A
13345	G
13346	C
13347	C
13348	A
13349	T
13350	A
13351	C
13352	T
13353	A
13354	T
13355	T
13356	T
13357	A
13358	T
13359	G
13360	T
13361	G
13362	C
13363	T
13364	C
13365	C
13366	G
13367	G
13368	G
13369	T
13370	C
13371	C
13372	A
13373	T
13374	C
13375	A
13376	T
13377	C
13378	C
13379	A
13380	C
13381	A
13382	A
13383	C
13384	C
13385	T
13386	T
13387	A
13388	A
13389	C
13390	A
13391	A
13392	T
13393	G
13394	A
13395	A
13396	C
13397	A
13398	A
13399	G
13400	A
13401	T
13402	A
13403	T
13404	T
13405	C
13406	G
13407	A
13408	A
13409	A
13410	A
13411	A
13412	T
13413	A
13414	G
13415	G
13416	A
13417	G
13418	G
13419	A
13420	C
13421	T
13422	A
13423	C
13424	T
13425	C
13426	A
13427	A
13428	A
13429	A
13430	C
13431	C
13432	A
13433	T
13434	A
13435	C
13436	C
13437	T
13438	C
13439	T
13440	C
13441	A
13442	C
13443	T
13444	T
13445	C
13446	A
13447	A
13448	C
13449	C
13450	T
13451	C
13452	C
13453	C
13454	T
13455	C
13456	A
13457	C
13458	C
13459	A
13460	T
13461	T
13462	G
13463	G
13464	C
13465	A
13466	G
13467	C
13468	C
13469	T
13470	A
13471	G
13472	C
13473	A
13474	T
13475	T
13476	A
13477	G
13478	C
13479	A
13480	G
13481	G
13482	A
13483	A
13484	T
13485	A
13486	C
13487	C
13488	T
13489	T
13490	T
13491	C
13492	C
13493	T
13494	C
13495	A
13496	C
13497	A
13498	G
13499	G
13500	T
13501	T
13502	T
13503	C
13504	T
13505	A
13506	C
13507	T
13508	C
13509	C
13510	A
13511	A
13512	A
13513	G
13514	A
13515	C
13516	C
13517	A
13518	C
13519	A
13520	T
13521	C
13522	A
13523	T
13524	C
13525	G
13526	A
13527	A
13528	A
13529	C
13530	C
13531	G
13532	C
13533	A
13534	A
13535	A
13536	C
13537	A
13538	T
13539	A
13540	T
13541	C
13542	A
13543	T
13544	A
13545	C
13546	A
13547	C
13548	A
13549	A
13550	A
13551	C
13552	G
13553	C
13554	C
13555	T
13556	G
13557	A
13558	G
13559	C
13560	C
13561	C
13562	T
13563	A
13564	T
13565	C
13566	T
13567	A
13568	T
13569	T
13570	A
13571	C
13572	T
13573	C
13574	T
13575	C
13576	A
13577	T
13578	C
13579	G
13580	C
13581	T
13582	A
13583	C
13584	C
13585	T
13586	C
13587	C
13588	C
13589	T
13590	G
13591	A
13592	C
13593	A
13594	A
13595	G
13596	C
13597	G
13598	C
13599	C
13600	T
13601	A
13602	T
13603	A
13604	G
13605	C
13606	A
13607	C
13608	T
13609	C
13610	G
13611	A
13612	A
13613	T
13614	A
13615	A
13616	T
13617	T
13618	C
13619	T
13620	T
13621	C
13622	T
13623	C
13624	A
13625	C
13626	C
13627	C
13628	T
13629	A
13630	A
13631	C
13632	A
13633	G
13634	G
13635	T
13636	C
13637	A
13638	A
13639	C
13640	C
13641	T
13642	C
13643	G
13644	C
13645	T
13646	T
13647	C
13648	C
13649	C
13650	C
13651	A
13652	C
13653	C
13654	C
13655	T
13656	T
13657	A
13658	C
13659	T
13660	A
13661	A
13662	C
13663	A
13664	T
13665	T
13666	A
13667	A
13668	C
13669	G
13670	A
13671	A
13672	A
13673	A
13674	T
13675	A
13676	A
13677	C
13678	C
13679	C
13680	C
13681	A
13682	C
13683	C
13684	C
13685	T
13686	A
13687	C
13688	T
13689	A
13690	A
13691	A
13692	C
13693	C
13694	C
13695	C
13696	A
13697	T
13698	T
13699	A
13700	A
13701	A
13702	C
13703	G
13704	C
13705	C
13706	T
13707	G
13708	G
13709	C
13710	A
13711	G
13712	C
13713	C
13714	G
13715	G
13716	A
13717	A
13718	G
13719	C
13720	C
13721	T
13722	A
13723	T
13724	T
13725	C
13726	G
13727	C
13728	A
13729	G
13730	G
13731	A
13732	T
13733	T
13734	T
13735	C
13736	T
13737	C
13738	A
13739	T
13740	T
13741	A
13742	C
13743	T
13744	A
13745	A
13746	C
13747	A
13748	A
13749	C
13750	A
13751	T
13752	T
13753	T
13754	C
13755	C
13756	C
13757	C
13758	C
13759	G
13760	C
13761	A
13762	T
13763	C
13764	C
13765	C
13766	C
13767	C
13768	T
13769	T
13770	C
13771	C
13772	A
13773	A
13774	A
13775	C
13776	A
13777	A
13778	C
13779	A
13780	A
13781	T
13782	C
13783	C
13784	C
13785	C
13786	C
13787	T
13788	C
13789	T
13790	A
13791	C
13792	C
13793	T
13794	A
13795	A
13796	A
13797	A
13798	C
13799	T
13800	C
13801	A
13802	C
13803	A
13804	G
13805	C
13806	C
13807	C
13808	T
13809	C
13810	G
13811	C
13812	T
13813	G
13814	T
13815	C
13816	A
13817	C
13818	T
13819	T
13820	T
13821	C
13822	C
13823	T
13824	A
13825	G
13826	G
13827	A
13828	C
13829	T
13830	T
13831	C
13832	T
13833	A
13834	A
13835	C
13836	A
13837	G
13838	C
13839	C
13840	C
13841	T
13842	A
13843	G
13844	A
13845	C
13846	C
13847	T
13848	C
13849	A
13850	A
13851	C
13852	T
13853	A
13854	C
13855	C
13856	T
13857	A
13858	A
13859	C
13860	C
13861	A
13862	A
13863	C
13864	A
13865	A
13866	A
13867	C
13868	T
13869	T
13870	A
13871	A
13872	A
13873	A
13874	T
13875	A
13876	A
13877	A
13878	A
13879	T
13880	C
13881	C
13882	C
13883	C
13884	A
13885	C
13886	T
13887	A
13888	T
13889	G
13890	C
13891	A
13892	C
13893	A
13894	T
13895	T
13896	T
13897	T
13898	A
13899	T
13900	T
13901	T
13902	C
13903	T
13904	C
13905	C
13906	A
13907	A
13908	C
13909	A
13910	T
13911	A
13912	C
13913	T
13914	C
13915	G
13916	G
13917	A
13918	T
13919	T
13920	C
13921	T
13922	A
13923	C
13924	C
13925	C
13926	T
13927	A
13928	G
13929	C
13930	A
13931	T
13932	C
13933	A
13934	C
13935	A
13936	C
13937	A
13938	C
13939	C
13940	G
13941	C
13942	A
13943	C
13944	A
13945	A
13946	T
13947	C
13948	C
13949	C
13950	C
13951	T
13952	A
13953	T
13954	C
13955	T
13956	A
13957	G
13958	G
13959	C
13960	C
13961	T
13962	T
13963	C
13964	T
13965	T
13966	A
13967	C
13968	G
13969	A
13970	G
13971	C
13972	C
13973	A
13974	A
13975	A
13976	A
13977	C
13978	C
13979	T
13980	G
13981	C
13982	C
13983	C
13984	C
13985	T
13986	A
13987	C
13988	T
13989	C
13990	C
13991	T
13992	C
13993	C
13994	T
13995	A
13996	G
13997	A
13998	C
13999	C
14000	T
14001	A
14002	A
14003	C
14004	C
14005	T
14006	G
14007	A
14008	C
14009	T
14010	A
14011	G
14012	A
14013	A
14014	A
14015	A
14016	G
14017	C
14018	T
14019	A
14020	T
14021	T
14022	A
14023	C
14024	C
14025	T
14026	A
14027	A
14028	A
14029	A
14030	C
14031	A
14032	A
14033	T
14034	T
14035	T
14036	C
14037	A
14038	C
14039	A
14040	G
14041	C
14042	A
14043	C
14044	C
14045	A
14046	A
14047	A
14048	T
14049	C
14050	T
14051	C
14052	C
14053	A
14054	C
14055	C
14056	T
14057	C
14058	C
14059	A
14060	T
14061	C
14062	A
14063	T
14064	C
14065	A
14066	C
14067	C
14068	T
14069	C
14070	A
14071	A
14072	C
14073	C
14074	C
14075	A
14076	A
14077	A
14078	A
14079	A
14080	G
14081	G
14082	C
14083	A
14084	T
14085	A
14086	A
14087	T
14088	T
14089	A
14090	A
14091	A
14092	C
14093	T
14094	T
14095	T
14096	A
14097	C
14098	T
14099	T
14100	C
14101	C
14102	T
14103	C
14104	T
14105	C
14106	T
14107	T
14108	T
14109	C
14110	T
14111	T
14112	C
14113	T
14114	T
14115	C
14116	C
14117	C
14118	A
14119	C
14120	T
14121	C
14122	A
14123	T
14124	C
14125	C
14126	T
14127	A
14128	A
14129	C
14130	C
14131	C
14132	T
14133	A
14134	C
14135	T
14136	C
14137	C
14138	T
14139	A
14140	A
14141	T
14142	C
14143	A
14144	C
14145	A
14146	T
14147	A
14148	A
14149	C
14150	C
14151	T
14152	A
14153	T
14154	T
14155	C
14156	C
14157	C
14158	C
14159	C
14160	G
14161	A
14162	G
14163	C
14164	A
14165	A
14166	T
14167	C
14168	T
14169	C
14170	A
14171	A
14172	T
14173	T
14174	A
14175	C
14176	A
14177	A
14178	T
14179	A
14180	T
14181	A
14182	T
14183	A
14184	C
14185	A
14186	C
14187	C
14188	A
14189	A
14190	C
14191	A
14192	A
14193	A
14194	C
14195	A
14196	A
14197	T
14198	G
14199	T
14200	T
14201	C
14202	A
14203	A
14204	C
14205	C
14206	A
14207	G
14208	T
14209	A
14210	A
14211	C
14212	T
14213	A
14214	C
14215	T
14216	A
14217	C
14218	T
14219	A
14220	A
14221	T
14222	C
14223	A
14224	A
14225	C
14226	G
14227	C
14228	C
14229	C
14230	A
14231	T
14232	A
14233	A
14234	T
14235	C
14236	A
14237	T
14238	A
14239	C
14240	A
14241	A
14242	A
14243	G
14244	C
14245	C
14246	C
14247	C
14248	C
14249	G
14250	C
14251	A
14252	C
14253	C
14254	A
14255	A
14256	T
14257	A
14258	G
14259	G
14260	A
14261	T
14262	C
14263	C
14264	T
14265	C
14266	C
14267	C
14268	G
14269	A
14270	A
14271	T
14272	C
14273	A
14274	A
14275	C
14276	C
14277	C
14278	T
14279	G
14280	A
14281	C
14282	C
14283	C
14284	C
14285	T
14286	C
14287	T
14288	C
14289	C
14290	T
14291	T
14292	C
14293	A
14294	T
14295	A
14296	A
14297	A
14298	T
14299	T
14300	A
14301	T
14302	T
14303	C
14304	A
14305	G
14306	C
14307	T
14308	T
14309	C
14310	C
14311	T
14312	A
14313	C
14314	A
14315	C
14316	T
14317	A
14318	T
14319	T
14320	A
14321	A
14322	A
14323	G
14324	T
14325	T
14326	T
14327	A
14328	C
14329	C
14330	A
14331	C
14332	A
14333	A
14334	C
14335	C
14336	A
14337	C
14338	C
14339	A
14340	C
14341	C
14342	C
14343	C
14344	A
14345	T
14346	C
14347	A
14348	T
14349	A
14350	C
14351	T
14352	C
14353	T
14354	T
14355	T
14356	C
14357	A
14358	C
14359	C
14360	C
14361	A
14362	C
14363	A
14364	G
14365	C
14366	A
14367	C
14368	C
14369	A
14370	A
14371	T
14372	C
14373	C
14374	T
14375	A
14376	C
14377	C
14378	T
14379	C
14380	C
14381	A
14382	T
14383	C
14384	G
14385	C
14386	T
14387	A
14388	A
14389	C
14390	C
14391	C
14392	C
14393	A
14394	C
14395	T
14396	A
14397	A
14398	A
14399	A
14400	C
14401	A
14402	C
14403	T
14404	C
14405	A
14406	C
14407	C
14408	A
14409	A
14410	G
14411	A
14412	C
14413	C
14414	T
14415	C
14416	A
14417	A
14418	C
14419	C
14420	C
14421	C
14422	T
14423	G
14424	A
14425	C
14426	C
14427	C
14428	C
14429	C
14430	A
14431	T
14432	G
14433	C
14434	C
14435	T
14436	C
14437	A
14438	G
14439	G
14440	A
14441	T
14442	A
14443	C
14444	T
14445	C
14446	C
14447	T
14448	C
14449	A
14450	A
14451	T
14452	A
14453	G
14454	C
14455	C
14456	A
14457	T
14458	C
14459	G
14460	C
14461	T
14462	G
14463	T
14464	A
14465	G
14466	T
14467	A
14468	T
14469	A
14470	T
14471	C
14472	C
14473	A
14474	A
14475	A
14476	G
14477	A
14478	C
14479	A
14480	A
14481	C
14482	C
14483	A
14484	T
14485	C
14486	A
14487	T
14488	T
14489	C
14490	C
14491	C
14492	C
14493	C
14494	T
14495	A
14496	A
14497	A
14498	T
14499	A
14500	A
14501	A
14502	T
14503	T
14504	A
14505	A
14506	A
14507	A
14508	A
14509	A
14510	A
14511	C
14512	T
14513	A
14514	T
14515	T
14516	A
14517	A
14518	A
14519	C
14520	C
14521	C
14522	A
14523	T
14524	A
14525	T
14526	A
14527	A
14528	C
14529	C
14530	T
14531	C
14532	C
14533	C
14534	C
14535	C
14536	A
14537	A
14538	A
14539	A
14540	T
14541	T
14542	C
14543	A
14544	G
14545	A
14546	A
14547	T
14548	A
14549	A
14550	T
14551	A
14552	A
14553	C
14554	A
14555	C
14556	A
14557	C
14558	C
14559	C
14560	G
14561	A
14562	C
14563	C
14564	A
14565	C
14566	A
14567	C
14568	C
14569	G
14570	C
14571	T
14572	A
14573	A
14574	C
14575	A
14576	A
14577	T
14578	C
14579	A
14580	A
14581	T
14582	A
14583	C
14584	T
14585	A
14586	A
14587	A
14588	C
14589	C
14590	C
14591	C
14592	C
14593	A
14594	T
14595	A
14596	A
14597	A
14598	T
14599	A
14600	G
14601	G
14602	A
14603	G
14604	A
14605	A
14606	G
14607	G
14608	C
14609	T
14610	T
14611	A
14612	G
14613	A
14614	A
14615	G
14616	A
14617	A
14618	A
14619	A
14620	C
14621	C
14622	C
14623	C
14624	A
14625	C
14626	A
14627	A
14628	A
14629	C
14630	C
14631	C
14632	C
14633	A
14634	T
14635	T
14636	A
14637	C
14638	T
14639	A
14640	A
14641	A
14642	C
14643	C
14644	C
14645	A
14646	C
14647	A
14648	C
14649	T
14650	C
14651	A
14652	A
14653	C
14654	A
14655	G
14656	A
14657	A
14658	A
14659	C
14660	A
14661	A
14662	A
14663	G
14664	C
14665	A
14666	T
14667	A
14668	C
14669	A
14670	T
14671	C
14672	A
14673	T
14674	T
14675	A
14676	T
14677	T
14678	C
14679	T
14680	C
14681	G
14682	C
14683	A
14684	C
14685	G
14686	G
14687	A
14688	C
14689	T
14690	A
14691	C
14692	A
14693	A
14694	C
14695	C
14696	A
14697	C
14698	G
14699	A
14700	C
14701	C
14702	A
14703	A
14704	T
14705	G
14706	A
14707	T
14708	A
14709	T
14710	G
14711	A
14712	A
14713	A
14714	A
14715	A
14716	C
14717	C
14718	A
14719	T
14720	C
14721	G
14722	T
14723	T
14724	G
14725	T
14726	A
14727	T
14728	T
14729	T
14730	C
14731	A
14732	A
14733	C
14734	T
14735	A
14736	C
14737	A
14738	A
14739	G
14740	A
14741	A
14742	C
14743	A
14744	C
14745	C
14746	A
14747	A
14748	T
14749	G
14750	A
14751	C
14752	C
14753	C
14754	C
14755	A
14756	A
14757	T
14758	A
14759	C
14760	G
14761	C
14762	A
14763	A
14764	A
14765	A
14766	C
14767	T
14768	A
14769	A
14770	C
14771	C
14772	C
14773	C
14774	C
14775	T
14776	A
14777	A
14778	T
14779	A
14780	A
14781	A
14782	A
14783	T
14784	T
14785	A
14786	A
14787	T
14788	T
14789	A
14790	A
14791	C
14792	C
14793	A
14794	C
14795	T
14796	C
14797	A
14798	T
14799	T
14800	C
14801	A
14802	T
14803	C
14804	G
14805	A
14806	C
14807	C
14808	T
14809	C
14810	C
14811	C
14812	C
14813	A
14814	C
14815	C
14816	C
14817	C
14818	A
14819	T
14820	C
14821	C
14822	A
14823	A
14824	C
14825	A
14826	T
14827	C
14828	T
14829	C
14830	C
14831	G
14832	C
14833	A
14834	T
14835	G
14836	A
14837	T
14838	G
14839	A
14840	A
14841	A
14842	C
14843	T
14844	T
14845	C
14846	G
14847	G
14848	C
14849	T
14850	C
14851	A
14852	C
14853	T
14854	C
14855	C
14856	T
14857	T
14858	G
14859	G
14860	C
14861	G
14862	C
14863	C
14864	T
14865	G
14866	C
14867	C
14868	T
14869	G
14870	A
14871	T
14872	C
14873	C
14874	T
14875	C
14876	C
14877	A
14878	A
14879	A
14880	T
14881	C
14882	A
14883	C
14884	C
14885	A
14886	C
14887	A
14888	G
14889	G
14890	A
14891	C
14892	T
14893	A
14894	T
14895	T
14896	C
14897	C
14898	T
14899	A
14900	G
14901	C
14902	C
14903	A
14904	T
14905	G
14906	C
14907	A
14908	C
14909	T
14910	A
14911	C
14912	T
14913	C
14914	A
14915	C
14916	C
14917	A
14918	G
14919	A
14920	C
14921	G
14922	C
14923	C
14924	T
14925	C
14926	A
14927	A
14928	C
14929	C
14930	G
14931	C
14932	C
14933	T
14934	T
14935	T
14936	T
14937	C
14938	A
14939	T
14940	C
14941	A
14942	A
14943	T
14944	C
14945	G
14946	C
14947	C
14948	C
14949	A
14950	C
14951	A
14952	T
14953	C
14954	A
14955	C
14956	T
14957	C
14958	G
14959	A
14960	G
14961	A
14962	C
14963	G
14964	T
14965	A
14966	A
14967	A
14968	T
14969	T
14970	A
14971	T
14972	G
14973	G
14974	C
14975	T
14976	G
14977	A
14978	A
14979	T
14980	C
14981	A
14982	T
14983	C
14984	C
14985	G
14986	C
14987	T
14988	A
14989	C
14990	C
14991	T
14992	T
14993	C
14994	A
14995	C
14996	G
14997	C
14998	C
14999	A
15000	A
15001	T
15002	G
15003	G
15004	C
15005	G
15006	C
15007	C
15008	T
15009	C
15010	A
15011	A
15012	T
15013	A
15014	T
15015	T
15016	C
15017	T
15018	T
15019	T
15020	A
15021	T
15022	C
15023	T
15024	G
15025	C
15026	C
15027	T
15028	C
15029	T
15030	T
15031	C
15032	C
15033	T
15034	A
15035	C
15036	A
15037	C
15038	A
15039	T
15040	C
15041	G
15042	G
15043	G
15044	C
15045	G
15046	A
15047	G
15048	G
15049	C
15050	C
15051	T
15052	A
15053	T
15054	A
15055	T
15056	T
15057	A
15058	C
15059	G
15060	G
15061	A
15062	T
15063	C
15064	A
15065	T
15066	T
15067	T
15068	C
15069	T
15070	C
15071	T
15072	A
15073	C
15074	T
15075	C
15076	A
15077	G
15078	A
15079	A
15080	A
15081	C
15082	C
15083	T
15084	G
15085	A
15086	A
15087	A
15088	C
15089	A
15090	T
15091	C
15092	G
15093	G
15094	C
15095	A
15096	T
15097	T
15098	A
15099	T
15100	C
15101	C
15102	T
15103	C
15104	C
15105	T
15106	G
15107	C
15108	T
15109	T
15110	G
15111	C
15112	A
15113	A
15114	C
15115	T
15116	A
15117	T
15118	A
15119	G
15120	C
15121	A
15122	A
15123	C
15124	A
15125	G
15126	C
15127	C
15128	T
15129	T
15130	C
15131	A
15132	T
15133	A
15134	G
15135	G
15136	C
15137	T
15138	A
15139	T
15140	G
15141	T
15142	C
15143	C
15144	T
15145	C
15146	C
15147	C
15148	G
15149	T
15150	G
15151	A
15152	G
15153	G
15154	C
15155	C
15156	A
15157	A
15158	A
15159	T
15160	A
15161	T
15162	C
15163	A
15164	T
15165	T
15166	C
15167	T
15168	G
15169	A
15170	G
15171	G
15172	G
15173	G
15174	C
15175	C
15176	A
15177	C
15178	A
15179	G
15180	T
15181	A
15182	A
15183	T
15184	T
15185	A
15186	C
15187	A
15188	A
15189	A
15190	C
15191	T
15192	T
15193	A
15194	C
15195	T
15196	A
15197	T
15198	C
15199	C
15200	G
15201	C
15202	C
15203	A
15204	T
15205	C
15206	C
15207	C
15208	A
15209	T
15210	A
15211	C
15212	A
15213	T
15214	T
15215	G
15216	G
15217	G
15218	A
15219	C
15220	A
15221	G
15222	A
15223	C
15224	C
15225	T
15226	A
15227	G
15228	T
15229	T
15230	C
15231	A
15232	A
15233	T
15234	G
15235	A
15236	A
15237	T
15238	C
15239	T
15240	G
15241	A
15242	G
15243	G
15244	A
15245	G
15246	G
15247	C
15248	T
15249	A
15250	C
15251	T
15252	C
15253	A
15254	G
15255	T
15256	A
15257	G
15258	A
15259	C
15260	A
15261	G
15262	T
15263	C
15264	C
15265	C
15266	A
15267	C
15268	C
15269	C
15270	T
15271	C
15272	A
15273	C
15274	A
15275	C
15276	G
15277	A
15278	T
15279	T
15280	C
15281	T
15282	T
15283	T
15284	A
15285	C
15286	C
15287	T
15288	T
15289	T
15290	C
15291	A
15292	C
15293	T
15294	T
15295	C
15296	A
15297	T
15298	C
15299	T
15300	T
15301	G
15302	C
15303	C
15304	C
15305	T
15306	T
15307	C
15308	A
15309	T
15310	T
15311	A
15312	T
15313	T
15314	G
15315	C
15316	A
15317	G
15318	C
15319	C
15320	C
15321	T
15322	A
15323	G
15324	C
15325	A
15326	A
15327	C
15328	A
15329	C
15330	T
15331	C
15332	C
15333	A
15334	C
15335	C
15336	T
15337	C
15338	C
15339	T
15340	A
15341	T
15342	T
15343	C
15344	T
15345	T
15346	G
15347	C
15348	A
15349	C
15350	G
15351	A
15352	A
15353	A
15354	C
15355	G
15356	G
15357	G
15358	A
15359	T
15360	C
15361	A
15362	A
15363	A
15364	C
15365	A
15366	A
15367	C
15368	C
15369	C
15370	C
15371	C
15372	T
15373	A
15374	G
15375	G
15376	A
15377	A
15378	T
15379	C
15380	A
15381	C
15382	C
15383	T
15384	C
15385	C
15386	C
15387	A
15388	T
15389	T
15390	C
15391	C
15392	G
15393	A
15394	T
15395	A
15396	A
15397	A
15398	A
15399	T
15400	C
15401	A
15402	C
15403	C
15404	T
15405	T
15406	C
15407	C
15408	A
15409	C
15410	C
15411	C
15412	T
15413	T
15414	A
15415	C
15416	T
15417	A
15418	C
15419	A
15420	C
15421	A
15422	A
15423	T
15424	C
15425	A
15426	A
15427	A
15428	G
15429	A
15430	C
15431	G
15432	C
15433	C
15434	C
15435	T
15436	C
15437	G
15438	G
15439	C
15440	T
15441	T
15442	A
15443	C
15444	T
15445	T
15446	C
15447	T
15448	C
15449	T
15450	T
15451	C
15452	C
15453	T
15454	T
15455	C
15456	T
15457	C
15458	T
15459	C
15460	C
15461	T
15462	T
15463	A
15464	A
15465	T
15466	G
15467	A
15468	C
15469	A
15470	T
15471	T
15472	A
15473	A
15474	C
15475	A
15476	C
15477	T
15478	A
15479	T
15480	T
15481	C
15482	T
15483	C
15484	A
15485	C
15486	C
15487	A
15488	G
15489	A
15490	C
15491	C
15492	T
15493	C
15494	C
15495	T
15496	A
15497	G
15498	G
15499	C
15500	G
15501	A
15502	C
15503	C
15504	C
15505	A
15506	G
15507	A
15508	C
15509	A
15510	A
15511	T
15512	T
15513	A
15514	T
15515	A
15516	C
15517	C
15518	C
15519	T
15520	A
15521	G
15522	C
15523	C
15524	A
15525	A
15526	C
15527	C
15528	C
15529	C
15530	T
15531	T
15532	A
15533	A
15534	A
15535	C
15536	A
15537	C
15538	C
15539	C
15540	C
15541	T
15542	C
15543	C
15544	C
15545	C
15546	A
15547	C
15548	A
15549	T
15550	C
15551	A
15552	A
15553	G
15554	C
15555	C
15556	C
15557	G
15558	A
15559	A
15560	T
15561	G
15562	A
15563	T
15564	A
15565	T
15566	T
15567	T
15568	C
15569	C
15570	T
15571	A
15572	T
15573	T
15574	C
15575	G
15576	C
15577	C
15578	T
15579	A
15580	C
15581	A
15582	C
15583	A
15584	A
15585	T
15586	T
15587	C
15588	T
15589	C
15590	C
15591	G
15592	A
15593	T
15594	C
15595	C
15596	G
15597	T
15598	C
15599	C
15600	C
15601	T
15602	A
15603	A
15604	C
15605	A
15606	A
15607	A
15608	C
15609	T
15610	A
15611	G
15612	G
15613	A
15614	G
15615	G
15616	C
15617	G
15618	T
15619	C
15620	C
15621	T
15622	T
15623	G
15624	C
15625	C
15626	C
15627	T
15628	A
15629	T
15630	T
15631	A
15632	C
15633	T
15634	A
15635	T
15636	C
15637	C
15638	A
15639	T
15640	C
15641	C
15642	T
15643	C
15644	A
15645	T
15646	C
15647	C
15648	T
15649	A
15650	G
15651	C
15652	A
15653	A
15654	T
15655	A
15656	A
15657	T
15658	C
15659	C
15660	C
15661	C
15662	A
15663	T
15664	C
15665	C
15666	T
15667	C
15668	C
15669	A
15670	T
15671	A
15672	T
15673	A
15674	T
15675	C
15676	C
15677	A
15678	A
15679	A
15680	C
15681	A
15682	A
15683	C
15684	A
15685	A
15686	A
15687	G
15688	C
15689	A
15690	T
15691	A
15692	A
15693	T
15694	A
15695	T
15696	T
15697	T
15698	C
15699	G
15700	C
15701	C
15702	C
15703	A
15704	C
15705	T
15706	A
15707	A
15708	G
15709	C
15710	C
15711	A
15712	A
15713	T
15714	C
15715	A
15716	C
15717	T
15718	T
15719	T
15720	A
15721	T
15722	T
15723	G
15724	A
15725	C
15726	T
15727	C
15728	C
15729	T
15730	A
15731	G
15732	C
15733	C
15734	G
15735	C
15736	A
15737	G
15738	A
15739	C
15740	C
15741	T
15742	C
15743	C
15744	T
15745	C
15746	A
15747	T
15748	T
15749	C
15750	T
15751	A
15752	A
15753	C
15754	C
15755	T
15756	G
15757	A
15758	A
15759	T
15760	C
15761	G
15762	G
15763	A
15764	G
15765	G
15766	A
15767	C
15768	A
15769	A
15770	C
15771	C
15772	A
15773	G
15774	T
15775	A
15776	A
15777	G
15778	C
15779	T
15780	A
15781	C
15782	C
15783	C
15784	T
15785	T
15786	T
15787	T
15788	A
15789	C
15790	C
15791	A
15792	T
15793	C
15794	A
15795	T
15796	T
15797	G
15798	G
15799	A
15800	C
15801	A
15802	A
15803	G
15804	T
15805	A
15806	G
15807	C
15808	A
15809	T
15810	C
15811	C
15812	G
15813	T
15814	A
15815	C
15816	T
15817	A
15818	T
15819	A
15820	C
15821	T
15822	T
15823	C
15824	A
15825	C
15826	A
15827	A
15828	C
15829	A
15830	A
15831	T
15832	C
15833	C
15834	T
15835	A
15836	A
15837	T
15838	C
15839	C
15840	T
15841	A
15842	A
15843	T
15844	A
15845	C
15846	C
15847	A
15848	A
15849	C
15850	T
15851	A
15852	T
15853	C
15854	T
15855	C
15856	C
15857	C
15858	T
15859	A
15860	A
15861	T
15862	T
15863	G
15864	A
15865	A
15866	A
15867	A
15868	C
15869	A
15870	A
15871	A
15872	A
15873	T
15874	A
15875	C
15876	T
15877	C
15878	A
15879	A
15880	A
15881	T
15882	G
15883	G
15884	G
15885	C
15886	C
15887	T
15888	G
15889	T
15890	C
15891	C
15892	T
15893	T
15894	G
15895	T
15896	A
15897	G
15898	T
15899	A
15900	T
15901	A
15902	A
15903	A
15904	C
15905	T
15906	A
15907	A
15908	T
15909	A
15910	C
15911	A
15912	C
15913	C
15914	A
15915	G
15916	T
15917	C
15918	T
15919	T
15920	G
15921	T
15922	A
15923	A
15924	A
15925	C
15926	C
15927	G
15928	G
15929	A
15930	G
15931	A
15932	T
15933	G
15934	A
15935	A
15936	A
15937	A
15938	C
15939	C
15940	T
15941	T
15942	T
15943	T
15944	T
15945	C
15946	C
15947	A
15948	A
15949	G
15950	G
15951	A
15952	C
15953	A
15954	A
15955	A
15956	T
15957	C
15958	A
15959	G
15960	A
15961	G
15962	A
15963	A
15964	A
15965	A
15966	A
15967	G
15968	T
15969	C
15970	T
15971	T
15972	T
15973	A
15974	A
15975	C
15976	T
15977	C
15978	C
15979	A
15980	C
15981	C
15982	A
15983	T
15984	T
15985	A
15986	G
15987	C
15988	A
15989	C
15990	C
15991	C
15992	A
15993	A
15994	A
15995	G
15996	C
15997	T
15998	A
15999	A
16000	G
16001	A
16002	T
16003	T
16004	C
16005	T
16006	A
16007	A
16008	T
16009	T
16010	T
16011	A
16012	A
16013	A
16014	C
16015	T
16016	A
16017	T
16018	T
16019	C
16020	T
16021	C
16022	T
16023	G
16024	T
16025	T
16026	C
16027	T
16028	T
16029	T
16030	C
16031	A
16032	T
16033	G
16034	G
16035	G
16036	G
16037	A
16038	A
16039	G
16040	C
16041	A
16042	G
16043	A
16044	T
16045	T
16046	T
16047	G
16048	G
16049	G
16050	T
16051	A
16052	C
16053	C
16054	A
16055	C
16056	C
16057	C
16058	A
16059	A
16060	G
16061	T
16062	A
16063	T
16064	T
16065	G
16066	A
16067	C
16068	T
16069	C
16070	A
16071	C
16072	C
16073	C
16074	A
16075	T
16076	C
16077	A
16078	A
16079	C
16080	A
16081	A
16082	C
16083	C
16084	G
16085	C
16086	T
16087	A
16088	T
16089	G
16090	T
16091	A
16092	T
16093	T
16094	T
16095	C
16096	G
16097	T
16098	A
16099	C
16100	A
16101	T
16102	T
16103	A
16104	C
16105	T
16106	G
16107	C
16108	C
16109	A
16110	G
16111	C
16112	C
16113	A
16114	C
16115	C
16116	A
16117	T
16118	G
16119	A
16120	A
16121	T
16122	A
16123	T
16124	T
16125	G
16126	T
16127	A
16128	C
16129	G
16130	G
16131	T
16132	A
16133	C
16134	C
16135	A
16136	T
16137	A
16138	A
16139	A
16140	T
16141	A
16142	C
16143	T
16144	T
16145	G
16146	A
16147	C
16148	C
16149	A
16150	C
16151	C
16152	T
16153	G
16154	T
16155	A
16156	G
16157	T
16158	A
16159	C
16160	A
16161	T
16162	A
16163	A
16164	A
16165	A
16166	A
16167	C
16168	C
16169	C
16170	A
16171	A
16172	T
16173	C
16174	C
16175	A
16176	C
16177	A
16178	T
16179	C
16180	A
16181	A
16182	A
16183	A
16184	C
16185	C
16186	C
16187	C
16188	C
16189	T
16190	C
16191	C
16192	C
16193	C
16194	A
16195	T
16196	G
16197	C
16198	T
16199	T
16200	A
16201	C
16202	A
16203	A
16204	G
16205	C
16206	A
16207	A
16208	G
16209	T
16210	A
16211	C
16212	A
16213	G
16214	C
16215	A
16216	A
16217	T
16218	C
16219	A
16220	A
16221	C
16222	C
16223	C
16224	T
16225	C
16226	A
16227	A
16228	C
16229	T
16230	A
16231	T
16232	C
16233	A
16234	C
16235	A
16236	C
16237	A
16238	T
16239	C
16240	A
16241	A
16242	C
16243	T
16244	G
16245	C
16246	A
16247	A
16248	C
16249	T
16250	C
16251	C
16252	A
16253	A
16254	A
16255	G
16256	C
16257	C
16258	A
16259	C
16260	C
16261	C
16262	C
16263	T
16264	C
16265	A
16266	C
16267	C
16268	C
16269	A
16270	C
16271	T
16272	A
16273	G
16274	G
16275	A
16276	T
16277	A
16278	C
16279	C
16280	A
16281	A
16282	C
16283	A
16284	A
16285	A
16286	C
16287	C
16288	T
16289	A
16290	C
16291	C
16292	C
16293	A
16294	C
16295	C
16296	C
16297	T
16298	T
16299	A
16300	A
16301	C
16302	A
16303	G
16304	T
16305	A
16306	C
16307	A
16308	T
16309	A
16310	G
16311	T
16312	A
16313	C
16314	A
16315	T
16316	A
16317	A
16318	A
16319	G
16320	C
16321	C
16322	A
16323	T
16324	T
16325	T
16326	A
16327	C
16328	C
16329	G
16330	T
16331	A
16332	C
16333	A
16334	T
16335	A
16336	G
16337	C
16338	A
16339	C
16340	A
16341	T
16342	T
16343	A
16344	C
16345	A
16346	G
16347	T
16348	C
16349	A
16350	A
16351	A
16352	T
16353	C
16354	C
16355	C
16356	T
16357	T
16358	C
16359	T
16360	C
16361	G
16362	T
16363	C
16364	C
16365	C
16366	C
16367	A
16368	T
16369	G
16370	G
16371	A
16372	T
16373	G
16374	A
16375	C
16376	C
16377	C
16378	C
16379	C
16380	C
16381	T
16382	C
16383	A
16384	G
16385	A
16386	T
16387	A
16388	G
16389	G
16390	G
16391	G
16392	T
16393	C
16394	C
16395	C
16396	T
16397	T
16398	G
16399	A
16400	C
16401	C
16402	A
16403	C
16404	C
16405	A
16406	T
16407	C
16408	C
16409	T
16410	C
16411	C
16412	G
16413	T
16414	G
16415	A
16416	A
16417	A
16418	T
16419	C
16420	A
16421	A
16422	T
16423	A
16424	T
16425	C
16426	C
16427	C
16428	G
16429	C
16430	A
16431	C
16432	A
16433	A
16434	G
16435	A
16436	G
16437	T
16438	G
16439	C
16440	T
16441	A
16442	C
16443	T
16444	C
16445	T
16446	C
16447	C
16448	T
16449	C
16450	G
16451	C
16452	T
16453	C
16454	C
16455	G
16456	G
16457	G
16458	C
16459	C
16460	C
16461	A
16462	T
16463	A
16464	A
16465	C
16466	A
16467	C
16468	T
16469	T
16470	G
16471	G
16472	G
16473	G
16474	G
16475	T
16476	A
16477	G
16478	C
16479	T
16480	A
16481	A
16482	A
16483	G
16484	T
16485	G
16486	A
16487	A
16488	C
16489	T
16490	G
16491	T
16492	A
16493	T
16494	C
16495	C
16496	G
16497	A
16498	C
16499	A
16500	T
16501	C
16502	T
16503	G
16504	G
16505	T
16506	T
16507	C
16508	C
16509	T
16510	A
16511	C
16512	T
16513	T
16514	C
16515	A
16516	G
16517	G
16518	G
16519	T
16520	C
16521	A
16522	T
16523	A
16524	A
16525	A
16526	G
16527	C
16528	C
16529	T
16530	A
16531	A
16532	A
16533	T
16534	A
16535	G
16536	C
16537	C
16538	C
16539	A
16540	C
16541	A
16542	C
16543	G
16544	T
16545	T
16546	C
16547	C
16548	C
16549	C
16550	T
16551	T
16552	A
16553	A
16554	A
16555	T
16556	A
16557	A
16558	G
16559	A
16560	C
16561	A
16562	T
16563	C
16564	A
16565	C
16566	G
16567	A
16568	T
16569	G
\.

--
-- Data for Name: MOTIVE_TYPE; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."MOTIVE_TYPE" ("ID", "DESCRIPTION") FROM stdin;
1	Profile rejection
2	Profile deletion
\.

--
-- Data for Name: MOTIVE; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."MOTIVE" ("ID", "MOTIVE_TYPE", "DESCRIPTION", "FREE_TEXT", "DELETED") FROM stdin;
1	1	Other	t	f
2	2	Other	t	f
3	1	Invalid	f	f
4	2	Court request	f	f
\.

--
-- Data for Name: MUTATION_DEFAULT_PARAMETER; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."MUTATION_DEFAULT_PARAMETER" ("ID", "LOCUS", "SEX", "MUTATION_RATE") FROM stdin;
1	CSF1PO	F	0.00030000
2	CSF1PO	M	0.00150000
3	CSF1PO	I	0.00160000
4	FGA	F	0.00050000
5	FGA	M	0.00320000
6	FGA	I	0.00280000
7	TH01	F	0.00009000
8	TH01	M	0.00009000
9	TH01	I	0.00010000
10	TPOX	F	0.00004000
11	TPOX	M	0.00012000
12	TPOX	I	0.00010000
13	vWA	F	0.00030000
14	vWA	M	0.00170000
15	vWA	I	0.00170000
16	D3S1358	F	0.00015000
17	D3S1358	M	0.00130000
18	D3S1358	I	0.00120000
19	D5S818	F	0.00025000
20	D5S818	M	0.00120000
21	D5S818	I	0.00110000
22	D7S820	F	0.00013000
23	D7S820	M	0.00120000
24	D7S820	I	0.00100000
25	D8S1179	F	0.00020000
26	D8S1179	M	0.00160000
27	D8S1179	I	0.00140000
28	D13S317	F	0.00040000
29	D13S317	M	0.00140000
30	D13S317	I	0.00140000
31	D16S539	F	0.00030000
32	D16S539	M	0.00110000
33	D16S539	I	0.00110000
34	D18S51	F	0.00060000
35	D18S51	M	0.00220000
36	D18S51	I	0.00220000
37	D21S11	F	0.00110000
38	D21S11	M	0.00150000
39	D21S11	I	0.00190000
40	PentaD	F	0.00060000
41	PentaD	M	0.00090000
42	PentaD	I	0.00140000
43	PentaE	F	0.00065000
44	PentaE	M	0.00135000
45	PentaE	I	0.00160000
46	D2S1338	F	0.00021000
47	D2S1338	M	0.00100000
48	D2S1338	I	0.00120000
49	D19S433	F	0.00050000
50	D19S433	M	0.00075000
51	D19S433	I	0.00110000
52	ACTBP2	F	0.00300000
53	ACTBP2	M	0.00640000
54	ACTBP2	I	0.00640000
\.

--
-- Data for Name: MUTATION_MODEL_TYPE; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."MUTATION_MODEL_TYPE" ("ID", "DESCRIPTION") FROM stdin;
1	Equal
2	Stepwise
3	Stepwise extended
\.

--
-- Data for Name: POPULATION_BASE_FREQUENCY_NAME; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."POPULATION_BASE_FREQUENCY_NAME" ("ID", "NAME", "THETA", "MODEL", "ACTIVE", "DEFAULT") FROM stdin;
1	SHDG	0	HardyWeinberg	t	t
\.


--
-- Data for Name: POPULATION_BASE_FREQUENCY; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."POPULATION_BASE_FREQUENCY" ("ID", "BASE_NAME", "MARKER", "ALLELE", "FREQUENCY") FROM stdin;
1	1	CSF1PO	6	0.00006000
2	1	CSF1PO	7	0.00167000
3	1	CSF1PO	8	0.00448000
4	1	CSF1PO	8.30000000000000071	0.00006000
5	1	CSF1PO	9	0.02185000
6	1	CSF1PO	10	0.26929000
7	1	CSF1PO	10.3000000000000007	0.00006000
8	1	CSF1PO	11	0.28222000
9	1	CSF1PO	12	0.34753000
10	1	CSF1PO	13	0.06209000
11	1	CSF1PO	14	0.00868000
12	1	CSF1PO	15	0.00167000
13	1	D10S1248	11	0.00424000
14	1	D10S1248	12	0.04240000
15	1	D10S1248	13	0.27300000
16	1	D10S1248	14	0.33900000
17	1	D10S1248	15	0.21200000
18	1	D10S1248	16	0.09960000
19	1	D10S1248	17	0.02540000
20	1	D10S1248	18	0.00212000
21	1	D10S1248	19	0.00212000
22	1	D12S391	11	0.00351000
23	1	D12S391	12	0.02285000
24	1	D12S391	12.1999999999999993	0.00439000
25	1	D12S391	13	0.05624000
26	1	D12S391	13.1999999999999993	0.01142000
27	1	D12S391	14	0.08875000
28	1	D12S391	14.1999999999999993	0.00879000
29	1	D12S391	15	0.07030000
30	1	D12S391	15.1999999999999993	0.01142000
31	1	D12S391	16	0.02548000
32	1	D12S391	16.1999999999999993	0.00264000
33	1	D12S391	17	0.05272000
34	1	D12S391	17.1999999999999993	0.00088000
35	1	D12S391	17.3000000000000007	0.00967000
36	1	D12S391	18	0.15114000
37	1	D12S391	18.3000000000000007	0.01318000
38	1	D12S391	19	0.12390000
39	1	D12S391	19.1000000000000014	0.00088000
40	1	D12S391	19.3000000000000007	0.00527000
41	1	D12S391	20	0.13972000
42	1	D12S391	20.3000000000000007	0.00088000
43	1	D12S391	21	0.07293000
44	1	D12S391	22	0.05800000
45	1	D12S391	23	0.04218000
46	1	D12S391	24	0.01406000
47	1	D12S391	25	0.00879000
48	1	D13S317	6	0.00054000
49	1	D13S317	7	0.00027000
50	1	D13S317	8	0.09117000
51	1	D13S317	9	0.15994000
52	1	D13S317	10	0.07562000
53	1	D13S317	11	0.22469000
54	1	D13S317	12	0.24182000
55	1	D13S317	13	0.12787000
56	1	D13S317	14	0.07606000
57	1	D13S317	15	0.00196000
58	1	D13S317	16	0.00005000
59	1	D16S539	5	0.00005000
60	1	D16S539	7	0.00011000
61	1	D16S539	8	0.01758000
62	1	D16S539	9	0.16177000
63	1	D16S539	10	0.10915000
64	1	D16S539	11	0.27673000
65	1	D16S539	12	0.27623000
66	1	D16S539	13	0.13790000
67	1	D16S539	14	0.01917000
68	1	D16S539	15	0.00131000
69	1	D18S51	9	0.00038000
70	1	D18S51	10	0.00728000
71	1	D18S51	10.1999999999999993	0.00011000
72	1	D18S51	11	0.01144000
73	1	D18S51	12	0.12708000
74	1	D18S51	13	0.11537000
75	1	D18S51	13.1999999999999993	0.00011000
76	1	D18S51	14	0.20496000
77	1	D18S51	14.1999999999999993	0.00016000
78	1	D18S51	15	0.14191000
79	1	D18S51	15.1999999999999993	0.00005000
80	1	D18S51	16	0.12002000
81	1	D18S51	16.1999999999999993	0.00005000
82	1	D18S51	17	0.12407000
83	1	D18S51	18	0.06715000
84	1	D18S51	19	0.03639000
85	1	D18S51	20	0.01954000
86	1	D18S51	21	0.01144000
87	1	D18S51	22	0.00859000
88	1	D18S51	22.1999999999999993	0.00005000
89	1	D18S51	23	0.00235000
90	1	D18S51	24	0.00104000
91	1	D18S51	25	0.00016000
92	1	D18S51	26	0.00005000
93	1	D18S51	27	0.00016000
94	1	D19S433	9	0.00289017
95	1	D19S433	11	0.02601156
96	1	D19S433	11.1999999999999993	0.00289017
97	1	D19S433	12	0.06936416
98	1	D19S433	12.1999999999999993	0.02312139
99	1	D19S433	13	0.19942197
100	1	D19S433	13.1999999999999993	0.09248555
101	1	D19S433	14	0.28612717
102	1	D19S433	14.1999999999999993	0.05780347
103	1	D19S433	15	0.13872832
104	1	D19S433	15.1999999999999993	0.05491329
105	1	D19S433	16	0.03468208
106	1	D19S433	16.1999999999999993	0.01156069
107	1	D1S1656	10	0.00350000
108	1	D1S1656	11	0.04553000
109	1	D1S1656	12	0.09545000
110	1	D1S1656	13	0.09895000
111	1	D1S1656	14	0.09019000
112	1	D1S1656	14.3000000000000007	0.00175000
113	1	D1S1656	15	0.13660000
114	1	D1S1656	15.3000000000000007	0.04466000
115	1	D1S1656	16	0.16637000
116	1	D1S1656	16.3000000000000007	0.04729000
117	1	D1S1656	17	0.04729000
118	1	D1S1656	17.3000000000000007	0.14799000
119	1	D1S1656	18	0.00876000
120	1	D1S1656	18.3000000000000007	0.04904000
121	1	D1S1656	19.3000000000000007	0.01138000
122	1	D1S1656	20	0.00175000
123	1	D1S1656	21	0.00088000
124	1	D1S1656	22	0.00175000
125	1	D1S1656	23	0.00088000
126	1	D21S11	19	0.00005000
127	1	D21S11	24	0.00005000
128	1	D21S11	24.1999999999999993	0.00087000
129	1	D21S11	25	0.00033000
130	1	D21S11	25.1999999999999993	0.00054000
131	1	D21S11	26	0.00158000
132	1	D21S11	26.1999999999999993	0.00016000
133	1	D21S11	27	0.01638000
134	1	D21S11	27.1999999999999993	0.00011000
135	1	D21S11	28	0.09375000
136	1	D21S11	28.1999999999999993	0.00076000
137	1	D21S11	29	0.19643000
138	1	D21S11	29.1999999999999993	0.00207000
139	1	D21S11	30	0.27228000
140	1	D21S11	30.1999999999999993	0.02536000
141	1	D21S11	31	0.06209000
142	1	D21S11	31.1999999999999993	0.11312000
143	1	D21S11	32	0.00920000
144	1	D21S11	32.1000000000000014	0.00016000
145	1	D21S11	32.2000000000000028	0.13843000
146	1	D21S11	33	0.00212000
147	1	D21S11	33.1000000000000014	0.00011000
148	1	D21S11	33.2000000000000028	0.05430000
149	1	D21S11	33.2999999999999972	0.00011000
150	1	D21S11	34	0.00044000
151	1	D21S11	34.2000000000000028	0.00637000
152	1	D21S11	35	0.00103000
153	1	D21S11	35.1000000000000014	0.00027000
154	1	D21S11	35.2000000000000028	0.00087000
155	1	D21S11	36	0.00027000
156	1	D21S11	36.2000000000000028	0.00005000
157	1	D21S11	37	0.00005000
158	1	D22S1045	10	0.01480000
159	1	D22S1045	11	0.06360000
160	1	D22S1045	12	0.01270000
161	1	D22S1045	13	0.00847000
162	1	D22S1045	14	0.02750000
163	1	D22S1045	15	0.42600000
164	1	D22S1045	16	0.35000000
165	1	D22S1045	17	0.09110000
166	1	D22S1045	18	0.00636000
167	1	D2S1338	14	0.00568200
168	1	D2S1338	16	0.05965900
169	1	D2S1338	17	0.23295500
170	1	D2S1338	18	0.07102300
171	1	D2S1338	19	0.15625000
172	1	D2S1338	20	0.12784100
173	1	D2S1338	21	0.02556800
174	1	D2S1338	22	0.05681800
175	1	D2S1338	23	0.13920500
176	1	D2S1338	24	0.05397700
177	1	D2S1338	25	0.05113600
178	1	D2S1338	26	0.01704500
179	1	D2S1338	27	0.00284100
180	1	D2S441	10	0.33700000
181	1	D2S441	11	0.29900000
182	1	D2S441	11.3000000000000007	0.04450000
183	1	D2S441	12	0.03600000
184	1	D2S441	12.3000000000000007	0.00212000
185	1	D2S441	13	0.02330000
186	1	D2S441	13.3000000000000007	0.00138600
187	1	D2S441	14	0.20600000
188	1	D2S441	15	0.04870000
189	1	D2S441	16	0.00212000
190	1	D2S441	17	0.00212000
191	1	D3S1358	10	0.00005000
192	1	D3S1358	11	0.00055000
193	1	D3S1358	12	0.00186000
194	1	D3S1358	14	0.07485000
195	1	D3S1358	15	0.35112000
196	1	D3S1358	16	0.27983000
197	1	D3S1358	17	0.16096000
198	1	D3S1358	18	0.11706000
199	1	D3S1358	19	0.00891000
200	1	D3S1358	20	0.00071000
201	1	D3S1358	21	0.00011000
202	1	D5S818	7	0.06930000
203	1	D5S818	8	0.00635000
204	1	D5S818	9	0.04210000
205	1	D5S818	10	0.05326000
206	1	D5S818	11	0.42216000
207	1	D5S818	12	0.27108000
208	1	D5S818	13	0.12503000
209	1	D5S818	14	0.00843000
210	1	D5S818	15	0.00208000
211	1	D5S818	16	0.00022000
212	1	D6S1043	9	0.00179000
213	1	D6S1043	10	0.00896000
214	1	D6S1043	11	0.20072000
215	1	D6S1043	12	0.18728000
216	1	D6S1043	13	0.08154000
217	1	D6S1043	14	0.13082000
218	1	D6S1043	15	0.01523000
219	1	D6S1043	15.3000000000000007	0.00090000
220	1	D6S1043	16	0.01434000
221	1	D6S1043	17	0.04480000
222	1	D6S1043	17.3000000000000007	0.00179000
223	1	D6S1043	18	0.09588000
224	1	D6S1043	19	0.07616000
225	1	D6S1043	19.3000000000000007	0.00538000
226	1	D6S1043	20	0.02509000
227	1	D6S1043	20.3000000000000007	0.02419000
228	1	D6S1043	21	0.00538000
229	1	D6S1043	21.3000000000000007	0.05914000
230	1	D6S1043	22.3000000000000007	0.01703000
231	1	D6S1043	23.3000000000000007	0.00358000
232	1	D7S820	6	0.00011000
233	1	D7S820	7	0.01408000
234	1	D7S820	8	0.10337000
235	1	D7S820	9	0.08820000
236	1	D7S820	9.09999999999999964	0.00005000
237	1	D7S820	10	0.26243000
238	1	D7S820	11	0.30968000
239	1	D7S820	12	0.18630000
240	1	D7S820	12.0999999999999996	0.00005000
241	1	D7S820	12.1999999999999993	0.00005000
242	1	D7S820	12.3000000000000007	0.00005000
243	1	D7S820	13	0.03094000
244	1	D7S820	14	0.00462000
245	1	D7S820	15	0.00016000
246	1	D8S1179	8	0.00898000
247	1	D8S1179	9	0.00762000
248	1	D8S1179	10	0.06872000
249	1	D8S1179	11	0.07247000
250	1	D8S1179	12	0.14353000
251	1	D8S1179	13	0.30446000
252	1	D8S1179	14	0.22236000
253	1	D8S1179	15	0.13732000
254	1	D8S1179	16	0.03047000
255	1	D8S1179	17	0.00337000
256	1	D8S1179	18	0.00065000
257	1	D8S1179	19	0.00005000
258	1	FGA	16	0.00038000
259	1	FGA	17	0.00223000
260	1	FGA	18	0.01084000
261	1	FGA	18.1999999999999993	0.00038000
262	1	FGA	19	0.08764000
263	1	FGA	19.1999999999999993	0.00016000
264	1	FGA	20	0.09248000
265	1	FGA	20.1999999999999993	0.00022000
266	1	FGA	21	0.14417000
267	1	FGA	21.1999999999999993	0.00202000
268	1	FGA	22	0.12609000
269	1	FGA	22.1999999999999993	0.00398000
270	1	FGA	22.3000000000000007	0.00005000
271	1	FGA	23	0.12130000
272	1	FGA	23.1999999999999993	0.00245000
273	1	FGA	24	0.15153000
274	1	FGA	24.1999999999999993	0.00071000
275	1	FGA	25	0.15044000
276	1	FGA	25.1999999999999993	0.00005000
277	1	FGA	25.3000000000000007	0.00005000
278	1	FGA	26	0.07375000
279	1	FGA	26.1999999999999993	0.00005000
280	1	FGA	27	0.02200000
281	1	FGA	28	0.00599000
282	1	FGA	29	0.00054000
283	1	FGA	30	0.00016000
284	1	FGA	31	0.00022000
285	1	PentaD	2.20000000000000018	0.00358000
286	1	PentaD	3.20000000000000018	0.00011000
287	1	PentaD	5	0.00146000
288	1	PentaD	6	0.00039000
289	1	PentaD	7	0.01014000
290	1	PentaD	8	0.01366000
291	1	PentaD	9	0.19036000
292	1	PentaD	9.19999999999999929	0.00056000
293	1	PentaD	10	0.20671000
294	1	PentaD	11	0.16045000
295	1	PentaD	12	0.17008000
296	1	PentaD	13	0.16840000
297	1	PentaD	14	0.05410000
298	1	PentaD	15	0.01512000
299	1	PentaD	16	0.00364000
300	1	PentaD	17	0.00090000
301	1	PentaD	18	0.00017000
302	1	PentaD	19	0.00006000
303	1	PentaE	5	0.03706000
304	1	PentaE	6	0.00051000
305	1	PentaE	7	0.09435000
306	1	PentaE	8	0.02785000
307	1	PentaE	9	0.00695000
308	1	PentaE	10	0.06017000
309	1	PentaE	11	0.08780000
310	1	PentaE	12	0.18220000
311	1	PentaE	13	0.09463000
312	1	PentaE	13.1999999999999993	0.00011000
313	1	PentaE	14	0.07299000
314	1	PentaE	15	0.09814000
315	1	PentaE	16	0.05927000
316	1	PentaE	16.3000000000000007	0.00090000
317	1	PentaE	16.3999999999999986	0.00006000
318	1	PentaE	17	0.05322000
319	1	PentaE	18	0.03859000
320	1	PentaE	19	0.02616000
321	1	PentaE	20	0.02537000
322	1	PentaE	21	0.02107000
323	1	PentaE	22	0.00757000
324	1	PentaE	23	0.00345000
325	1	PentaE	24	0.00113000
326	1	PentaE	25	0.00028000
327	1	PentaE	26	0.00006000
328	1	TH01	4	0.00006000
329	1	TH01	5	0.00064000
330	1	TH01	6	0.29564000
331	1	TH01	7	0.26031000
332	1	TH01	8	0.08001000
333	1	TH01	9	0.12342000
334	1	TH01	9.30000000000000071	0.23091000
335	1	TH01	10	0.00808000
336	1	TH01	11	0.00070000
337	1	TH01	13.3000000000000007	0.00006000
338	1	TPOX	5	0.00016000
339	1	TPOX	6	0.00261000
340	1	TPOX	7	0.00142000
341	1	TPOX	8	0.48349000
342	1	TPOX	9	0.07790000
343	1	TPOX	10	0.04761000
344	1	TPOX	11	0.28963000
345	1	TPOX	12	0.09435000
346	1	TPOX	13	0.00256000
347	1	TPOX	14	0.00011000
348	1	TPOX	16	0.00005000
349	1	TPOX	21	0.00005000
350	1	TPOX	21.1999999999999993	0.00005000
351	1	vWA	11	0.00087000
352	1	vWA	12	0.00082000
353	1	vWA	13	0.00414000
354	1	vWA	14	0.06419000
355	1	vWA	15	0.09430000
356	1	vWA	16	0.30923000
357	1	vWA	17	0.28278000
358	1	vWA	18	0.16705000
359	1	vWA	19	0.06425000
360	1	vWA	20	0.01123000
361	1	vWA	21	0.00093000
362	1	vWA	22	0.00005000
363	1	vWA	23	0.00005000
364	1	TH01	-1	0.00250000
365	1	D1S1656	-1	0.00250000
366	1	D2S1338	-1	0.00250000
367	1	CSF1PO	-1	0.00250000
368	1	PentaD	-1	0.00250000
369	1	D18S51	-1	0.00250000
370	1	D8S1179	-1	0.00250000
371	1	D3S1358	-1	0.00250000
372	1	D2S441	-1	0.00250000
373	1	D22S1045	-1	0.00250000
374	1	D7S820	-1	0.00250000
375	1	D19S433	-1	0.00250000
376	1	D10S1248	-1	0.00250000
377	1	FGA	-1	0.00250000
378	1	D16S539	-1	0.00250000
379	1	D13S317	-1	0.00250000
380	1	vWA	-1	0.00250000
381	1	D5S818	-1	0.00250000
382	1	D6S1043	-1	0.00250000
383	1	TPOX	-1	0.00250000
384	1	D12S391	-1	0.00250000
385	1	D21S11	-1	0.00250000
386	1	PentaE	-1	0.00250000
\.

--
-- Data for Name: STRKIT; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."STRKIT" ("ID", "NAME", "LOCI_QTY", "REPRESENTATIVE_PARAMETER", "TYPE") FROM stdin;
ArgusX8	Argus X-8	9	9	2
InvestigatorArgusX12	Investigator Argus X-12 Kit	13	13	2
PowerPlexY	PowerPlex Y	11	11	3
PowerPlexY23	PowerPlex Y23	22	22	3
Yfiler	Yfiler	16	16	3
MtGenericPrimers	mt Generic primers	1	1	4
Mitocondrial	Mitocondrial	8	2	4
Identifiler	Identifiler	16	15	1
YfilerPlus	YfilerPlus	25	25	3
PowerplexESX17	Powerplex ESX 17 (Fast)	17	16	1
NGM	NGM	16	15	1
NGMSelect	NGM Select	17	16	1
Powerplex16	Powerplex 16	16	15	1
Powerplex18D	Powerplex 18D	18	17	1
Powerplex21	Powerplex 21	21	20	1
PowerplexESI17	Powerplex ESI 17 (Fast)	17	16	1
PowerplexESX16	Powerplex ESX 16 (Fast)	16	15	1
IdentifilerPlus	IdentifilerPlus	16	15	1
IdentifilerDirect	IdentifilerDirect	16	15	1
PowerplexESI16	Powerplex ESI 16 (Fast)	16	16	1
GlobalFiler	GlobalFiler	24	20	1
GlobalfilerExpress	GlobalfilerExpress	24	20	1
Investigator24plexGO	Investigator24plexGO	23	20	1
Investigator24plexQS	Investigator24plexQS	23	20	1
PowerplexFusion	Powerplex Fusion	24	20	1
PowerplexFusion6C	PowerplexFusion6C	27	20	1
VerifilerExpress	VerifilerExpress	23	20	1
VeriFilerPlus	VeriFilerPlus	25	20	1
InvestigatorIDplexPlus	InvestigatorIDplexPlus	16	15	1
InvestigatorESSplexSEPlus	InvestigatorESSplexSEPlus	17	16	1
PFAKit	PFAKit	8	8	1
\.


--
-- Data for Name: STRKIT_ALIAS; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."STRKIT_ALIAS" ("KIT", "ALIAS") FROM stdin;
Identifiler	Identifiller
Powerplex16	Powerplex_16
Powerplex16	Powerplex 16
YfilerPlus	YFILERPLUS-M
GlobalFiler	Globalfiler
YfilerPlus	YfilerTMPlus
PowerplexFusion	PowerplexFusion5C
PowerPlexY23	PowerPlexY23-2
VeriFilerPlus	Verifiler
Investigator24plexQS	24plex-qs
\.


--
-- Data for Name: STRKIT_LOCUS; Type: TABLE DATA; Schema: APP; Owner: genissqladmin
--

COPY "APP"."STRKIT_LOCUS" ("STRKIT", "LOCUS", "FLUOROPHORE", "ORDER") FROM stdin;
ArgusX8	AMEL	BFP	1
ArgusX8	DXS8378	BFP	2
ArgusX8	HPRTB	BFP	3
ArgusX8	DXS7423	BFP	4
ArgusX8	DXS7132	BFP	5
ArgusX8	DXS10134	BFP	6
ArgusX8	DXS10074	GFP	7
ArgusX8	DXS10101	GFP	8
ArgusX8	DXS10135	GFP	9
GlobalfilerExpress	D3S1358	BFP	1
GlobalfilerExpress	vWA	BFP	2
GlobalfilerExpress	D16S539	BFP	3
GlobalfilerExpress	CSF1PO	BFP	4
GlobalfilerExpress	TPOX	BFP	5
GlobalfilerExpress	YIndel	GFP	6
GlobalfilerExpress	AMEL	GFP	7
GlobalfilerExpress	D8S1179	GFP	8
GlobalfilerExpress	D21S11	GFP	9
GlobalfilerExpress	D18S51	GFP	10
GlobalfilerExpress	DYS391	GFP	11
GlobalfilerExpress	D2S441	YFP	12
GlobalfilerExpress	D19S433	YFP	13
GlobalfilerExpress	TH01	YFP	14
GlobalfilerExpress	FGA	YFP	15
GlobalfilerExpress	D22S1045	RFP	16
GlobalfilerExpress	D5S818	RFP	17
GlobalfilerExpress	D13S317	RFP	18
GlobalfilerExpress	D7S820	RFP	19
GlobalfilerExpress	ACTBP2	RFP	20
GlobalfilerExpress	D10S1248	VFP	21
GlobalfilerExpress	D1S1656	VFP	22
GlobalfilerExpress	D12S391	VFP	23
GlobalfilerExpress	D2S1338	VFP	24
Identifiler	D8S1179	CFP	1
Identifiler	D21S11	CFP	2
Identifiler	D7S820	CFP	3
Identifiler	CSF1PO	CFP	4
Identifiler	D3S1358	GFP	5
Identifiler	TH01	GFP	6
Identifiler	D13S317	GFP	7
Identifiler	D16S539	GFP	8
Identifiler	D2S1338	GFP	9
Identifiler	D19S433	YFP	10
Identifiler	vWA	YFP	11
Identifiler	TPOX	YFP	12
Identifiler	D18S51	YFP	13
Identifiler	AMEL	RFP	14
Identifiler	D5S818	RFP	15
Identifiler	FGA	RFP	16
IdentifilerPlus	D8S1179	CFP	1
IdentifilerPlus	D21S11	CFP	2
IdentifilerPlus	D7S820	CFP	3
IdentifilerPlus	CSF1PO	CFP	4
IdentifilerPlus	D3S1358	GFP	5
IdentifilerPlus	TH01	GFP	6
IdentifilerPlus	D13S317	GFP	7
IdentifilerPlus	D16S539	GFP	8
IdentifilerPlus	D2S1338	GFP	9
IdentifilerPlus	D19S433	YFP	10
IdentifilerPlus	vWA	YFP	11
IdentifilerPlus	TPOX	YFP	12
IdentifilerPlus	D18S51	YFP	13
IdentifilerPlus	AMEL	RFP	14
IdentifilerPlus	D5S818	RFP	15
IdentifilerPlus	FGA	RFP	16
IdentifilerDirect	D8S1179	CFP	1
IdentifilerDirect	D21S11	CFP	2
IdentifilerDirect	D7S820	CFP	3
IdentifilerDirect	CSF1PO	CFP	4
IdentifilerDirect	D3S1358	GFP	5
IdentifilerDirect	TH01	GFP	6
IdentifilerDirect	D13S317	GFP	7
IdentifilerDirect	D16S539	GFP	8
IdentifilerDirect	D2S1338	GFP	9
IdentifilerDirect	D19S433	YFP	10
IdentifilerDirect	vWA	YFP	11
IdentifilerDirect	TPOX	YFP	12
IdentifilerDirect	D18S51	YFP	13
IdentifilerDirect	AMEL	RFP	14
IdentifilerDirect	D5S818	RFP	15
IdentifilerDirect	FGA	RFP	16
InvestigatorArgusX12	AMEL	BFP	1
InvestigatorArgusX12	DXS10103	BFP	2
InvestigatorArgusX12	DXS8378	BFP	3
InvestigatorArgusX12	DXS10101	BFP	4
InvestigatorArgusX12	DXS10134	BFP	5
InvestigatorArgusX12	DXS10074	GFP	6
InvestigatorArgusX12	DXS7132	GFP	7
InvestigatorArgusX12	DXS10135	GFP	8
InvestigatorArgusX12	DXS7423	NFP	9
InvestigatorArgusX12	DXS10146	NFP	10
InvestigatorArgusX12	DXS10079	NFP	11
InvestigatorArgusX12	HPRTB	RFP	12
InvestigatorArgusX12	DXS10148	RFP	13
InvestigatorArgusX12	D21S11	RFP	14
Mitocondrial	HV1	NULL	2
Mitocondrial	HV1_RANGE	NULL	1
Mitocondrial	HV2	NULL	4
Mitocondrial	HV2_RANGE	NULL	3
Mitocondrial	HV3	NULL	6
Mitocondrial	HV3_RANGE	NULL	5
Mitocondrial	HV4	NULL	8
Mitocondrial	HV4_RANGE	NULL	7
MtGenericPrimers	HV1	NULL	2
MtGenericPrimers	HV1_RANGE	NULL	1
MtGenericPrimers	HV2	NULL	4
MtGenericPrimers	HV2_RANGE	NULL	3
MtGenericPrimers	HV3	NULL	6
MtGenericPrimers	HV3_RANGE	NULL	5
NGM	D10S1248	BFP	1
NGM	vWA	BFP	2
NGM	D16S539	BFP	3
NGM	D2S1338	BFP	4
NGM	AMEL	GFP	5
NGM	D8S1179	GFP	6
NGM	D21S11	GFP	7
NGM	D18S51	GFP	8
NGM	D22S1045	NFP	9
NGM	D19S433	NFP	10
NGM	TH01	NFP	11
NGM	FGA	NFP	12
NGM	D2S441	RFP	13
NGM	D3S1358	RFP	14
NGM	D1S1656	RFP	15
NGM	D12S391	RFP	16
NGMSelect	D10S1248	BFP	1
NGMSelect	vWA	BFP	2
NGMSelect	D16S539	BFP	3
NGMSelect	D2S1338	BFP	4
NGMSelect	AMEL	GFP	5
NGMSelect	D8S1179	GFP	6
NGMSelect	D21S11	GFP	7
NGMSelect	D18S51	GFP	8
NGMSelect	D22S1045	NFP	9
NGMSelect	D19S433	NFP	10
NGMSelect	TH01	NFP	11
NGMSelect	FGA	NFP	12
NGMSelect	D2S441	RFP	13
NGMSelect	D3S1358	RFP	14
NGMSelect	D1S1656	RFP	15
NGMSelect	D12S391	RFP	16
NGMSelect	ACTBP2	RFP	17
Powerplex16	D3S1358	CFP	1
Powerplex16	TH01	CFP	2
Powerplex16	D21S11	CFP	3
Powerplex16	D18S51	CFP	4
Powerplex16	PentaE	CFP	5
Powerplex16	D5S818	GFP	6
Powerplex16	D13S317	GFP	7
Powerplex16	D7S820	GFP	8
Powerplex16	D16S539	GFP	9
Powerplex16	CSF1PO	GFP	10
Powerplex16	PentaD	GFP	11
Powerplex16	AMEL	YFP	12
Powerplex16	vWA	YFP	13
Powerplex16	D8S1179	YFP	14
Powerplex16	TPOX	YFP	15
Powerplex16	FGA	YFP	16
Powerplex18D	D3S1358	BFP	1
Powerplex18D	TH01	BFP	2
Powerplex18D	D21S11	BFP	3
Powerplex18D	D18S51	BFP	4
Powerplex18D	PentaE	BFP	5
Powerplex18D	D5S818	YFP	6
Powerplex18D	D13S317	GFP	7
Powerplex18D	D7S820	GFP	8
Powerplex18D	D16S539	GFP	9
Powerplex18D	CSF1PO	GFP	10
Powerplex18D	PentaD	GFP	11
Powerplex18D	AMEL	NFP	12
Powerplex18D	vWA	NFP	13
Powerplex18D	D8S1179	NFP	14
Powerplex18D	TPOX	NFP	15
Powerplex18D	FGA	NFP	16
Powerplex18D	D19S433	RFP	17
Powerplex18D	D2S1338	RFP	18
Powerplex21	AMEL	BFP	1
Powerplex21	D3S1358	BFP	2
Powerplex21	D1S1656	BFP	3
Powerplex21	D6S1043	BFP	4
Powerplex21	D13S317	BFP	5
Powerplex21	PentaE	BFP	6
Powerplex21	D16S539	GFP	7
Powerplex21	D18S51	GFP	8
Powerplex21	D2S1338	GFP	9
Powerplex21	CSF1PO	GFP	10
Powerplex21	PentaD	GFP	11
Powerplex21	TH01	NFP	12
Powerplex21	vWA	NFP	13
Powerplex21	D21S11	NFP	14
Powerplex21	D7S820	NFP	15
Powerplex21	D5S818	NFP	16
Powerplex21	TPOX	NFP	17
Powerplex21	D8S1179	RFP	18
Powerplex21	D12S391	RFP	19
Powerplex21	D19S433	RFP	20
Powerplex21	FGA	RFP	21
PowerplexESI16	AMEL	BFP	1
PowerplexESI16	D3S1358	BFP	2
PowerplexESI16	D19S433	BFP	3
PowerplexESI16	D2S1338	BFP	4
PowerplexESI16	D22S1045	BFP	5
PowerplexESI16	D16S539	GFP	6
PowerplexESI16	D18S51	GFP	7
PowerplexESI16	D1S1656	GFP	8
PowerplexESI16	D10S1248	GFP	9
PowerplexESI16	D2S441	GFP	10
PowerplexESI16	TH01	NFP	11
PowerplexESI16	vWA	NFP	12
PowerplexESI16	D12S391	NFP	13
PowerplexESI16	D21S11	NFP	13
PowerplexESI16	D8S1179	RFP	14
PowerplexESI16	FGA	RFP	15
PowerplexESI17	AMEL	BFP	1
PowerplexESI17	D3S1358	BFP	2
PowerplexESI17	D19S433	BFP	3
PowerplexESI17	D2S1338	BFP	4
PowerplexESI17	D22S1045	BFP	5
PowerplexESI17	D16S539	GPF	6
PowerplexESI17	D18S51	GPF	7
PowerplexESI17	D1S1656	GPF	8
PowerplexESI17	D10S1248	GPF	9
PowerplexESI17	D2S441	GPF	10
PowerplexESI17	TH01	NFP	11
PowerplexESI17	vWA	NFP	12
PowerplexESI17	D21S11	NFP	13
PowerplexESI17	D12S391	NFP	14
PowerplexESI17	D8S1179	RFP	15
PowerplexESI17	FGA	RFP	16
PowerplexESI17	ACTBP2	RFP	17
PowerplexESX16	AMEL	BFP	1
PowerplexESX16	D3S1358	BFP	2
PowerplexESX16	TH01	BFP	3
PowerplexESX16	D21S11	BFP	4
PowerplexESX16	D18S51	BFP	5
PowerplexESX16	D10S1248	GFP	6
PowerplexESX16	D1S1656	GFP	7
PowerplexESX16	D2S1338	GFP	8
PowerplexESX16	D16S539	GFP	9
PowerplexESX16	D22S1045	NFP	10
PowerplexESX16	vWA	NFP	11
PowerplexESX16	D8S1179	NFP	12
PowerplexESX16	FGA	NFP	13
PowerplexESX16	D2S441	RFP	14
PowerplexESX16	D12S391	RFP	15
PowerplexESX16	D19S433	RFP	16
PowerplexESX17	AMEL	BFP	1
PowerplexESX17	D3S1358	BFP	2
PowerplexESX17	TH01	BFP	3
PowerplexESX17	D21S11	BFP	4
PowerplexESX17	D18S51	BFP	5
PowerplexESX17	D10S1248	GFP	6
PowerplexESX17	D1S1656	GFP	7
PowerplexESX17	D2S1338	GFP	8
PowerplexESX17	D16S539	GFP	9
PowerplexESX17	D22S1045	NFP	10
PowerplexESX17	vWA	NFP	11
PowerplexESX17	D8S1179	NFP	12
PowerplexESX17	FGA	NFP	13
PowerplexESX17	D2S441	RFP	14
PowerplexESX17	D12S391	RFP	15
PowerplexESX17	D19S433	RFP	16
PowerplexESX17	ACTBP2	RFP	17
PowerplexFusion	AMEL	BFP	1
PowerplexFusion	D3S1358	BFP	2
PowerplexFusion	D1S1656	BFP	3
PowerplexFusion	D2S441	BFP	4
PowerplexFusion	D10S1248	BFP	5
PowerplexFusion	D13S317	BFP	6
PowerplexFusion	PentaE	BFP	7
PowerplexFusion	D16S539	GFP	8
PowerplexFusion	D18S51	GFP	9
PowerplexFusion	D2S1338	GFP	10
PowerplexFusion	CSF1PO	GFP	11
PowerplexFusion	PentaD	GFP	12
PowerplexFusion	TH01	YFP	13
PowerplexFusion	vWA	YFP	14
PowerplexFusion	D21S11	YFP	15
PowerplexFusion	D7S820	YFP	16
PowerplexFusion	D5S818	YFP	17
PowerplexFusion	TPOX	YFP	18
PowerplexFusion	DYS391	YFP	19
PowerplexFusion	D8S1179	RFP	20
PowerplexFusion	D12S391	RFP	21
PowerplexFusion	D19S433	RFP	22
PowerplexFusion	FGA	RFP	23
PowerplexFusion	D22S1045	RFP	24
PowerPlexY	DYS391	CFP	1
PowerPlexY	DYS389I	CFP	2
PowerPlexY	DYS439	CFP	3
PowerPlexY	DYS389II	CFP	4
PowerPlexY	DYS438	GFP	5
PowerPlexY	DYS437	GFP	6
PowerPlexY	DYS19	GFP	7
PowerPlexY	DYS392	GFP	8
PowerPlexY	DYS393	YFP	9
PowerPlexY	DYS390	YFP	10
PowerPlexY23	DYS576	BFP	1
PowerPlexY23	DYS389I	BFP	2
PowerPlexY23	DYS448	BFP	3
PowerPlexY23	DYS389II	BFP	4
PowerPlexY23	DYS19	BFP	5
PowerPlexY23	DYS391	GFP	6
PowerPlexY23	DYS481	GFP	7
PowerPlexY23	DYS549	GFP	8
PowerPlexY23	DYS533	GFP	9
PowerPlexY23	DYS438	GFP	10
PowerPlexY23	DYS437	GFP	11
PowerPlexY23	DYS570	NFP	12
PowerPlexY23	DYS635	NFP	13
PowerPlexY23	DYS390	NFP	14
PowerPlexY23	DYS439	NFP	15
PowerPlexY23	DYS392	NFP	16
PowerPlexY23	DYS643	NFP	17
PowerPlexY23	DYS393	RFP	18
PowerPlexY23	DYS458	RFP	19
Yfiler	DYS456	CFP	1
Yfiler	DYS389I	CFP	2
Yfiler	DYS390	CFP	3
Yfiler	DYS389II	CFP	4
Yfiler	DYS458	GFP	5
Yfiler	DYS19	GFP	6
Yfiler	DYS393	YFP	8
Yfiler	DYS439	YFP	9
Yfiler	DYS635	YFP	10
Yfiler	DYS392	YFP	11
Yfiler	DYS391	YFP	12
Yfiler	YGATAH4	RFP	13
Yfiler	DYS437	RFP	14
Yfiler	DYS438	RFP	15
Yfiler	DYS448	RFP	16
VerifilerExpress	D3S1358	BFP	1
VerifilerExpress	vWA	BFP	2
VerifilerExpress	D16S539	BFP	3
VerifilerExpress	CSF1PO	BFP	4
VerifilerExpress	TPOX	BFP	5
VerifilerExpress	YIndel	GFP	6
VerifilerExpress	AMEL	GFP	7
VerifilerExpress	D8S1179	GFP	8
VerifilerExpress	D21S11	GFP	9
VerifilerExpress	D18S51	GFP	10
VerifilerExpress	PentaE	GFP	11
VerifilerExpress	D2S441	NFP	12
VerifilerExpress	D19S433	NFP	13
VerifilerExpress	TH01	NFP	14
VerifilerExpress	FGA	NFP	15
VerifilerExpress	D22S1045	RFP	16
VerifilerExpress	D5S818	RFP	17
VerifilerExpress	D13S317	RFP	18
VerifilerExpress	D7S820	RFP	19
VerifilerExpress	D6S1043	RFP	20
VerifilerExpress	D10S1248	VFP	21
VerifilerExpress	D1S1656	VFP	22
VerifilerExpress	D12S391	VFP	23
VerifilerExpress	D2S1338	VFP	24
VerifilerExpress	PentaD	VFP	25
PowerplexFusion6C	AMEL	BFP	1
PowerplexFusion6C	D3S1358	BFP	2
PowerplexFusion6C	D1S1656	BFP	3
PowerplexFusion6C	D2S441	BFP	4
PowerplexFusion6C	D10S1248	BFP	5
PowerplexFusion6C	D13S317	BFP	6
PowerplexFusion6C	PentaE	BFP	7
PowerplexFusion6C	D16S539	GFP	8
PowerplexFusion6C	D18S51	GFP	9
PowerplexFusion6C	D2S1338	GFP	10
PowerplexFusion6C	CSF1PO	GFP	11
PowerplexFusion6C	PentaD	GFP	12
PowerplexFusion6C	TH01	NFP	13
PowerplexFusion6C	vWA	NFP	14
PowerplexFusion6C	D21S11	NFP	15
PowerplexFusion6C	D7S820	NFP	16
PowerplexFusion6C	D5S818	NFP	17
PowerplexFusion6C	TPOX	NFP	18
PowerPlexY23	DYS456	RFP	21
PowerPlexY23	YGATAH4	RFP	22
Yfiler	DYS385	GFP	7
PowerPlexY	DYS385	YFP	11
PowerplexFusion6C	D8S1179	RFP	19
PowerplexFusion6C	D12S391	RFP	20
PowerplexFusion6C	D19S433	RFP	21
PowerplexFusion6C	ACTBP2	RFP	22
PowerplexFusion6C	D22S1045	RFP	23
PowerplexFusion6C	DYS391	VFP	24
PowerplexFusion6C	FGA	VFP	25
PowerplexFusion6C	DYS576	VFP	26
PowerplexFusion6C	DYS570	VFP	27
Investigator24plexQS	AMEL	BFP	1
Investigator24plexQS	TH01	BFP	2
Investigator24plexQS	D3S1358	BFP	3
Investigator24plexQS	vWA	BFP	4
Investigator24plexQS	D21S11	BFP	5
Investigator24plexQS	TPOX	GFP	6
Investigator24plexQS	DYS391	GFP	7
Investigator24plexQS	D1S1656	GFP	8
Investigator24plexQS	D12S391	GFP	9
Investigator24plexQS	ACTBP2	GFP	10
Investigator24plexQS	D10S1248	NFP	11
Investigator24plexQS	D22S1045	NFP	12
Investigator24plexQS	D19S433	NFP	13
Investigator24plexQS	D8S1179	NFP	14
Investigator24plexQS	D2S1338	NFP	15
Investigator24plexQS	D2S441	RFP	16
Investigator24plexQS	D18S51	RFP	17
Investigator24plexQS	FGA	RFP	18
Investigator24plexQS	D16S539	VFP	19
Investigator24plexQS	CSF1PO	VFP	20
Investigator24plexQS	D13S317	VFP	21
Investigator24plexQS	D5S818	VFP	22
Investigator24plexQS	D7S820	VFP	23
Investigator24plexGO	AMEL	BFP	1
Investigator24plexGO	TH01	BFP	2
Investigator24plexGO	D3S1358	BFP	3
Investigator24plexGO	vWA	BFP	4
Investigator24plexGO	D21S11	BFP	5
Investigator24plexGO	TPOX	GFP	6
Investigator24plexGO	DYS391	GFP	7
Investigator24plexGO	D1S1656	GFP	8
Investigator24plexGO	D12S391	GFP	9
Investigator24plexGO	ACTBP2	GFP	10
Investigator24plexGO	D10S1248	NFP	11
Investigator24plexGO	D22S1045	NFP	12
Investigator24plexGO	D19S433	NFP	13
Investigator24plexGO	D8S1179	NFP	14
Investigator24plexGO	D2S1338	NFP	15
Investigator24plexGO	D2S441	RFP	16
Investigator24plexGO	D18S51	RFP	17
Investigator24plexGO	FGA	RFP	18
Investigator24plexGO	D16S539	VFP	19
Investigator24plexGO	CSF1PO	VFP	20
Investigator24plexGO	D13S317	VFP	21
Investigator24plexGO	D5S818	VFP	22
Investigator24plexGO	D7S820	VFP	23
YfilerPlus	DYS576	BFP	1
YfilerPlus	DYS389I	BFP	2
YfilerPlus	DYS635	BFP	3
YfilerPlus	DYS389II	BFP	4
YfilerPlus	DYS627	BFP	5
YfilerPlus	DYS460	GFP	6
YfilerPlus	DYS458	GFP	7
YfilerPlus	DYS19	GFP	8
YfilerPlus	YGATAH4	GFP	9
YfilerPlus	DYS448	GFP	10
YfilerPlus	DYS391	GFP	11
YfilerPlus	DYS456	NFP	12
YfilerPlus	DYS390	NFP	13
YfilerPlus	DYS438	NFP	14
YfilerPlus	DYS392	NFP	15
YfilerPlus	DYS518	NFP	16
YfilerPlus	DYS570	RFP	17
YfilerPlus	DYS437	RFP	18
YfilerPlus	DYS449	RFP	20
YfilerPlus	DYS393	VFP	21
YfilerPlus	DYS439	VFP	22
YfilerPlus	DYS481	VFP	23
YfilerPlus	DYS533	VFP	25
PowerPlexY23	DYS385	RFP	20
GlobalFiler	D3S1358	BFP	1
GlobalFiler	vWA	BFP	2
GlobalFiler	D16S539	BFP	3
GlobalFiler	CSF1PO	BFP	4
GlobalFiler	TPOX	BFP	5
GlobalFiler	YIndel	GFP	6
GlobalFiler	AMEL	GFP	7
GlobalFiler	D8S1179	GFP	8
GlobalFiler	D21S11	GFP	9
GlobalFiler	D18S51	GFP	10
GlobalFiler	DYS391	GFP	11
GlobalFiler	D2S441	YFP	12
GlobalFiler	D19S433	YFP	13
GlobalFiler	TH01	YFP	14
GlobalFiler	FGA	YFP	15
GlobalFiler	D22S1045	RFP	16
GlobalFiler	D5S818	RFP	17
GlobalFiler	D13S317	RFP	18
GlobalFiler	D7S820	RFP	19
GlobalFiler	ACTBP2	RFP	20
GlobalFiler	D10S1248	VFP	21
GlobalFiler	D1S1656	VFP	22
GlobalFiler	D12S391	VFP	23
GlobalFiler	D2S1338	VFP	24
VeriFilerPlus	D3S1358	BFP	1
VeriFilerPlus	vWA	BFP	2
VeriFilerPlus	D16S539	BFP	3
VeriFilerPlus	CSF1PO	BFP	4
VeriFilerPlus	D6S1043	BFP	5
VeriFilerPlus	YIndel	GFP	6
VeriFilerPlus	AMEL	GFP	7
VeriFilerPlus	D8S1179	GFP	8
VeriFilerPlus	D21S11	GFP	9
VeriFilerPlus	D18S51	GFP	10
VeriFilerPlus	D5S818	GFP	11
VeriFilerPlus	D2S441	NFP	12
VeriFilerPlus	D19S433	NFP	13
VeriFilerPlus	FGA	NFP	14
VeriFilerPlus	D10S1248	NFP	15
VeriFilerPlus	D22S1045	RFP	16
VeriFilerPlus	D1S1656	RFP	17
VeriFilerPlus	D13S317	RFP	18
VeriFilerPlus	D7S820	RFP	19
VeriFilerPlus	PentaE	RFP	20
VeriFilerPlus	PentaD	VFP	21
YfilerPlus	DYF387S1	VFP	24
YfilerPlus	DYS385	RFP	19
VeriFilerPlus	TH01	VFP	22
VeriFilerPlus	D12S391	VFP	23
VeriFilerPlus	D2S1338	VFP	24
VeriFilerPlus	TPOX	VFP	25
InvestigatorIDplexPlus	AMEL	BFP	1
InvestigatorIDplexPlus	TH01	BFP	2
InvestigatorIDplexPlus	D3S1358	BFP	3
InvestigatorIDplexPlus	vWA	BFP	4
InvestigatorIDplexPlus	D21S11	BFP	5
InvestigatorIDplexPlus	TPOX	GFP	6
InvestigatorIDplexPlus	D7S820	GFP	7
InvestigatorIDplexPlus	D19S433	GFP	8
InvestigatorIDplexPlus	D5S818	GFP	9
InvestigatorIDplexPlus	D2S1338	GFP	10
InvestigatorIDplexPlus	D16S539	NFP	11
InvestigatorIDplexPlus	CSF1PO	NFP	12
InvestigatorIDplexPlus	D13S317	NFP	13
InvestigatorIDplexPlus	FGA	NFP	14
InvestigatorIDplexPlus	D18S51	RFP	15
InvestigatorIDplexPlus	D8S1179	RFP	16
InvestigatorESSplexSEPlus	AMEL	BFP	1
InvestigatorESSplexSEPlus	TH01	BFP	2
InvestigatorESSplexSEPlus	D3S1358	BFP	3
InvestigatorESSplexSEPlus	vWA	BFP	4
InvestigatorESSplexSEPlus	D21S11	BFP	5
InvestigatorESSplexSEPlus	D16S539	GFP	6
InvestigatorESSplexSEPlus	D1S1656	GFP	7
InvestigatorESSplexSEPlus	D19S433	GFP	8
InvestigatorESSplexSEPlus	ACTBP2	GFP	9
InvestigatorESSplexSEPlus	D10S1248	NFP	10
InvestigatorESSplexSEPlus	D22S1045	NFP	11
InvestigatorESSplexSEPlus	D12S391	NFP	12
InvestigatorESSplexSEPlus	D8S1179	NFP	13
InvestigatorESSplexSEPlus	D2S1338	NFP	14
InvestigatorESSplexSEPlus	D2S441	RFP	15
InvestigatorESSplexSEPlus	D18S51	RFP	16
InvestigatorESSplexSEPlus	FGA	RFP	17
PFAKit	D3S1358	\N	1
PFAKit	vWA	\N	2
PFAKit	FGA	\N	3
PFAKit	D8S1179	\N	4
PFAKit	D21S11	\N	5
PFAKit	D5S818	\N	6
PFAKit	D13S317	\N	7
PFAKit	D7S820	\N	8
PFAKit	AMEL	\N	10
\.


--
-- Name: ANALYSIS_TYPE_ID_seq; Type: SEQUENCE SET; Schema: APP; Owner: genissqladmin
--

SELECT pg_catalog.setval('"APP"."ANALYSIS_TYPE_ID_seq"', 4, true);

--
-- Name: CATEGORY_ASSOCIATION_ID_seq; Type: SEQUENCE SET; Schema: APP; Owner: genissqladmin
--

SELECT pg_catalog.setval('"APP"."CATEGORY_ASSOCIATION_ID_seq"', 27, true);

--
-- Name: CATEGORY_CONFIGURATION_ID_seq; Type: SEQUENCE SET; Schema: APP; Owner: genissqladmin
--

SELECT pg_catalog.setval('"APP"."CATEGORY_CONFIGURATION_ID_seq"', 544, true);

--
-- Name: CATEGORY_MATCHING_ID_seq; Type: SEQUENCE SET; Schema: APP; Owner: genissqladmin
--

SELECT pg_catalog.setval('"APP"."CATEGORY_MATCHING_ID_seq"', 1417, true);

--
-- Name: MOTIVE_ID_seq; Type: SEQUENCE SET; Schema: APP; Owner: genissqladmin
--

SELECT pg_catalog.setval('"APP"."MOTIVE_ID_seq"', 4, true);

--
-- Name: MUTATION_DEFAULT_PARAMETER_ID_seq; Type: SEQUENCE SET; Schema: APP; Owner: genissqladmin
--

SELECT pg_catalog.setval('"APP"."MUTATION_DEFAULT_PARAMETER_ID_seq"', 54, true);

--
-- Name: POPULATION_BASE_FREQUENCY_ID_seq; Type: SEQUENCE SET; Schema: APP; Owner: genissqladmin
--

SELECT pg_catalog.setval('"APP"."POPULATION_BASE_FREQUENCY_ID_seq"', 386, true);

--
-- Name: POPULATION_BASE_FREQUENCY_NAME_ID_seq; Type: SEQUENCE SET; Schema: APP; Owner: genissqladmin
--

SELECT pg_catalog.setval('"APP"."POPULATION_BASE_FREQUENCY_NAME_ID_seq"', 1, true);

--
-- Name: INSTANCE_STATUS_ID_seq; Type: SEQUENCE SET; Schema: APP; Owner: genissqladmin
--

SELECT pg_catalog.setval('"APP"."INSTANCE_STATUS_ID_seq"', 3, true);

--
-- Name: INFERIOR_INSTANCE_PROFILE_STATUS_ID_seq; Type: SEQUENCE SET; Schema: APP; Owner: genissqladmin
--

SELECT pg_catalog.setval('"APP"."INFERIOR_INSTANCE_PROFILE_STATUS_ID_seq"', 16, true);

--
-- Name: MOTIVE_TYPE_ID_seq; Type: SEQUENCE SET; Schema: APP; Owner: genissqladmin
--

SELECT pg_catalog.setval('"APP"."MOTIVE_TYPE_ID_seq"', 2, true);

--
-- Name: MUTATION_MODEL_TYPE_ID_seq; Type: SEQUENCE SET; Schema: APP; Owner: genissqladmin
--

SELECT pg_catalog.setval('"APP"."MUTATION_MODEL_TYPE_ID_seq"',3, true);

