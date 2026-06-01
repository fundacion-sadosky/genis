# GENis PostgreSQL Schema — genisdb (localhost:5432)

Credentials (from `application-dev.conf`): user `genissqladmin`, password `genissqladminp`.

## Schemas

- **APP** — main application schema (62 tables)
- **STASH** — stash/draft copies of profile data (3 tables, mirrors APP equivalents)
- **public** — empty (postgres default)

---

## APP Schema

### ANALYSIS_TYPE
| Column | Type | Constraints |
|--------|------|-------------|
| ID | smallint | PK, NOT NULL |
| NAME | varchar(50) | NOT NULL |
| MITOCHONDRIAL | boolean | |

### BATCH_PROTO_PROFILE
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| USER | varchar(50) | NOT NULL |
| DATE | date | NOT NULL |
| LABEL | varchar(50) | |
| ANALYSISTYPE | varchar(50) | NOT NULL |

### BIO_MATERIAL_TYPE
| Column | Type | Constraints |
|--------|------|-------------|
| ID | varchar(50) | PK, NOT NULL |
| NAME | varchar(100) | NOT NULL |
| DESCRIPTION | varchar(10000) | |

### CASE_TYPE
| Column | Type | Constraints |
|--------|------|-------------|
| ID | varchar(50) | PK, NOT NULL |
| NAME | varchar(50) | NOT NULL |

### CATEGORY
| Column | Type | Constraints |
|--------|------|-------------|
| ID | varchar(50) | PK, NOT NULL |
| GROUP | varchar(50) | NOT NULL, FK → APP.GROUP.ID |
| NAME | varchar(100) | NOT NULL |
| DESCRIPTION | varchar(1024) | |
| FILIATION_DATA | boolean | NOT NULL |
| REPLICATE | boolean | NOT NULL |
| PEDIGREE_ASSOCIATION | boolean | NOT NULL |
| IS_REFERENCE | boolean | NOT NULL |
| ALLOW_MANUAL_LOADING | boolean | |
| TYPE | smallint | NOT NULL |

### CATEGORY_ALIAS
| Column | Type | Constraints |
|--------|------|-------------|
| ALIAS | varchar(100) | PK, NOT NULL |
| CATEGORY | varchar(50) | NOT NULL, FK → APP.CATEGORY.ID |

### CATEGORY_ASSOCIATION
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| CATEGORY | varchar(50) | NOT NULL, FK → APP.CATEGORY.ID |
| CATEGORY_RELATED | varchar(50) | NOT NULL, FK → APP.CATEGORY.ID |
| MISMATCHS | integer | NOT NULL |
| TYPE | smallint | NOT NULL, FK → APP.ANALYSIS_TYPE.ID |

### CATEGORY_CONFIGURATION
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| CATEGORY | varchar(50) | NOT NULL, FK → APP.CATEGORY.ID |
| TYPE | smallint | NOT NULL, FK → APP.ANALYSIS_TYPE.ID |
| COLLECTION_URI | varchar(500) | NOT NULL |
| DRAFT_URI | varchar(500) | NOT NULL |
| MIN_LOCUS_PER_PROFILE | varchar(1024) | NOT NULL |
| MAX_OVERAGE_DEVIATED_LOCI | varchar(1024) | NOT NULL |
| MAX_ALLELES_PER_LOCUS | smallint | NOT NULL |
| MULTIALLELIC | boolean | NOT NULL |

### CATEGORY_MAPPING
| Column | Type | Constraints |
|--------|------|-------------|
| ID | varchar(50) | PK, FK → APP.CATEGORY.ID |
| ID_SUPERIOR | varchar(50) | |

### CATEGORY_MATCHING
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| CATEGORY | varchar(50) | NOT NULL, FK → APP.CATEGORY.ID |
| CATEGORY_RELATED | varchar(50) | NOT NULL, FK → APP.CATEGORY.ID |
| PRIORITY | integer | NOT NULL |
| MINIMUM_STRINGENCY | varchar(50) | |
| FAIL_ON_MATCH | boolean | |
| FORWARD_TO_UPPER | boolean | |
| MATCHING_ALGORITHM | varchar(1024) | NOT NULL |
| MIN_LOCUS_MATCH | integer | NOT NULL |
| MISMATCHS_ALLOWED | integer | NOT NULL |
| TYPE | smallint | NOT NULL, FK → APP.ANALYSIS_TYPE.ID |
| CONSIDER_FOR_N | boolean | NOT NULL |

### CATEGORY_MODIFICATIONS
| Column | Type | Constraints |
|--------|------|-------------|
| From | varchar(50) | FK → APP.CATEGORY.ID |
| To | varchar(50) | FK → APP.CATEGORY.ID |

### CONNECTION
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| NAME | varchar(200) | NOT NULL |
| URL | varchar(200) | NOT NULL |
| DELETED | boolean | NOT NULL |

### COUNTRY
| Column | Type | Constraints |
|--------|------|-------------|
| CODE | varchar(2) | PK, NOT NULL |
| NAME | varchar(50) | NOT NULL |

### COURT_CASE
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| ATTORNEY | varchar(100) | |
| COURT | varchar(100) | |
| ASSIGNEE | varchar(50) | NOT NULL |
| INTERNAL_SAMPLE_CODE | varchar(50) | NOT NULL |
| CRIME_INVOLVED | varchar(50) | |
| CRIME_TYPE | varchar(50) | |
| CRIMINAL_CASE | varchar(50) | |
| STATUS | varchar(50) | NOT NULL |
| CASE_TYPE | varchar(50) | FK → APP.CASE_TYPE.ID |

### COURT_CASE_DATA_FILIATION
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| ID_COURT_CASE | bigint | NOT NULL, FK → APP.COURT_CASE.ID |
| FIRSTNAME | varchar(100) | |
| LASTNAME | varchar(100) | |
| SEX | varchar(50) | |
| DATE_OF_BIRTH | date | |
| DATE_OF_BIRTH_FROM | date | |
| DATE_OF_BIRTH_TO | date | |
| DATE_OF_MISSING | date | |
| NATIONALITY | varchar(50) | |
| IDENTIFICATION | varchar(50) | |
| HEIGHT | varchar(50) | |
| WEIGHT | varchar(50) | |
| HAIRCOLOR | varchar(50) | |
| SKINCOLOR | varchar(50) | |
| CLOTHING | varchar(50) | |
| ALIAS | varchar(50) | NOT NULL |
| PARTICULARITIES | varchar(50) | |

### COURT_CASE_PROFILE
| Column | Type | Constraints |
|--------|------|-------------|
| ID_COURT_CASE | bigint | PK, NOT NULL, FK → APP.COURT_CASE.ID |
| GLOBAL_CODE | varchar(100) | PK, NOT NULL, FK → APP.PROFILE_DATA.GLOBAL_CODE |
| PROFILE_TYPE | varchar(50) | NOT NULL |
| GROUPED_BY | varchar(100) | FK → APP.PROFILE_DATA.GLOBAL_CODE |

### CRIME_INVOLVED
| Column | Type | Constraints |
|--------|------|-------------|
| ID | varchar(50) | PK, NOT NULL |
| CRIME_TYPE | varchar(50) | NOT NULL, FK → APP.CRIME_TYPE.ID |
| NAME | varchar(100) | NOT NULL |
| DESCRIPTION | varchar(1024) | |

### CRIME_TYPE
| Column | Type | Constraints |
|--------|------|-------------|
| ID | varchar(50) | PK, NOT NULL |
| NAME | varchar(100) | NOT NULL |
| DESCRIPTION | varchar(1024) | |

### DISCLAIMER
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| TEXT | text | NOT NULL |

### EXTERNAL_PROFILE_DATA
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| LABORATORY_INSTANCE_ORIGIN | text | NOT NULL |
| LABORATORY_INSTANCE_INMEDIATE | text | NOT NULL |

### FILE_SENT
| Column | Type | Constraints |
|--------|------|-------------|
| ID | varchar(100) | PK, NOT NULL |
| TARGET_LAB | text | PK, NOT NULL |
| STATUS | bigint | FK → APP.INFERIOR_INSTANCE_PROFILE_STATUS.ID |
| DATE | timestamp | |
| FILE_TYPE | text | NOT NULL |

### GENETICIST
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| LABORATORY | varchar(20) | NOT NULL, FK → APP.LABORATORY.CODE_NAME |
| NAME | varchar(100) | NOT NULL |
| LASTNAME | varchar(100) | NOT NULL |
| EMAIL | varchar(100) | NOT NULL |
| TELEPHONE | varchar(100) | NOT NULL |

### GROUP
| Column | Type | Constraints |
|--------|------|-------------|
| ID | varchar(50) | PK, NOT NULL |
| NAME | varchar(100) | NOT NULL |
| DESCRIPTION | varchar(1024) | |

### INFERIOR_INSTANCE
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| URL | varchar(200) | NOT NULL |
| LABORATORY | varchar(50) | NOT NULL |
| STATUS | bigint | FK → APP.INSTANCE_STATUS.ID |

### INFERIOR_INSTANCE_PROFILE_STATUS
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| STATUS | varchar(200) | NOT NULL |

### INSTANCE_STATUS
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| STATUS | varchar(200) | NOT NULL |

### LABORATORY
| Column | Type | Constraints |
|--------|------|-------------|
| CODE_NAME | varchar(20) | PK, NOT NULL |
| NAME | varchar(100) | NOT NULL |
| COUNTRY | varchar(2) | NOT NULL, FK → APP.COUNTRY.CODE |
| PROVINCE | varchar(1) | NOT NULL, FK → APP.PROVINCE.CODE |
| ADDRESS | varchar(100) | NOT NULL |
| TELEPHONE | varchar(50) | NOT NULL |
| CONTACT_EMAIL | varchar(100) | NOT NULL |
| DROP_IN | double precision | NOT NULL |
| DROP_OUT | double precision | NOT NULL |

### LOCUS
| Column | Type | Constraints |
|--------|------|-------------|
| ID | varchar(50) | PK, NOT NULL |
| NAME | varchar(100) | NOT NULL |
| CHROMOSOME | varchar(2) | |
| MINIMUM_ALLELES_QTY | integer | NOT NULL |
| MAXIMUM_ALLELES_QTY | integer | NOT NULL |
| TYPE | smallint | NOT NULL, FK → APP.ANALYSIS_TYPE.ID |
| REQUIRED | boolean | NOT NULL |
| MIN_ALLELE_VALUE | numeric | |
| MAX_ALLELE_VALUE | numeric | |

### LOCUS_ALIAS
| Column | Type | Constraints |
|--------|------|-------------|
| ALIAS | varchar(100) | PK, NOT NULL |
| MARKER | varchar(50) | NOT NULL, FK → APP.LOCUS.ID |

### LOCUS_ALLELE
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| LOCUS | varchar(50) | NOT NULL |
| ALLELE | double precision | NOT NULL |

### LOCUS_LINK
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| LOCUS | varchar(50) | NOT NULL, FK → APP.LOCUS.ID |
| LINK | varchar(50) | NOT NULL, FK → APP.LOCUS.ID |
| FACTOR | double precision | |
| DISTANCE | double precision | |

### MATCH_SEND_STATUS
| Column | Type | Constraints |
|--------|------|-------------|
| ID | varchar(100) | PK, NOT NULL |
| TARGET_LAB | text | PK, NOT NULL |
| MESSAGE | text | |
| STATUS | bigint | FK → APP.INFERIOR_INSTANCE_PROFILE_STATUS.ID |
| DATE | timestamp | |

### MATCH_UPDATE_SEND_STATUS
| Column | Type | Constraints |
|--------|------|-------------|
| ID | varchar(100) | PK, NOT NULL |
| TARGET_LAB | text | PK, NOT NULL |
| MESSAGE | text | |
| STATUS | bigint | FK → APP.INFERIOR_INSTANCE_PROFILE_STATUS.ID |
| DATE | timestamp | |
| USER_NAME | text | |

### MITOCHONDRIAL_RCRS
| Column | Type | Constraints |
|--------|------|-------------|
| POSITION | integer | PK, NOT NULL |
| BASE | varchar(1) | NOT NULL |

### MOTIVE
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| MOTIVE_TYPE | bigint | NOT NULL, FK → APP.MOTIVE_TYPE.ID |
| DESCRIPTION | text | NOT NULL |
| FREE_TEXT | boolean | |
| DELETED | boolean | |

### MOTIVE_TYPE
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| DESCRIPTION | text | NOT NULL |

### MUTATION_DEFAULT_PARAMETER
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| LOCUS | varchar(50) | NOT NULL |
| SEX | varchar(1) | NOT NULL |
| MUTATION_RATE | numeric | NOT NULL |

### MUTATION_MODEL
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| NAME | text | NOT NULL |
| MUTATION_MODEL_TYPE | bigint | NOT NULL, FK → APP.MUTATION_MODEL_TYPE.ID |
| ACTIVE | boolean | NOT NULL |
| IGNORE_SEX | boolean | NOT NULL |
| CANT_SALTOS | bigint | NOT NULL |

### MUTATION_MODEL_KI
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| ID_MUTATION_MODEL_PARAMETER | bigint | NOT NULL, FK → APP.MUTATION_MODEL_PARAMETER.ID |
| ALLELE | double precision | NOT NULL |
| KI | double precision | NOT NULL |

### MUTATION_MODEL_PARAMETER
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| ID_MUTATION_MODEL | bigint | NOT NULL, FK → APP.MUTATION_MODEL.ID |
| LOCUS | varchar(50) | NOT NULL, FK → APP.LOCUS.ID |
| SEX | varchar(1) | NOT NULL |
| MUTATION_RATE | numeric | |
| MUTATION_RANGE | numeric | |
| MUTATION_RATE_MICROVARIANT | numeric | |

### MUTATION_MODEL_TYPE
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| DESCRIPTION | text | NOT NULL |

### NOTIFICATION
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| USER | varchar(50) | NOT NULL |
| KIND | varchar(100) | NOT NULL |
| INFO | varchar(1024) | NOT NULL |
| CREATION_DATE | timestamp | NOT NULL |
| UPDATE_DATE | timestamp | |
| PENDING | boolean | NOT NULL |
| FLAGGED | boolean | NOT NULL |

### PEDCHECK
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| ID_PEDIGREE | bigint | NOT NULL, FK → APP.PEDIGREE.ID |
| LOCUS | text | NOT NULL |
| GLOBAL_CODE | text | NOT NULL |

### PEDIGREE
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| ID_COURT_CASE | bigint | NOT NULL, FK → APP.COURT_CASE.ID |
| NAME | varchar(100) | NOT NULL |
| CREATION_DATE | timestamp | NOT NULL |
| STATUS | varchar(50) | NOT NULL |
| ASSIGNEE | varchar(50) | NOT NULL |
| CONSISTENCY_RUN | boolean | |

### POPULATION_BASE_FREQUENCY
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| BASE_NAME | bigint | NOT NULL, FK → APP.POPULATION_BASE_FREQUENCY_NAME.ID |
| MARKER | varchar(50) | NOT NULL |
| ALLELE | double precision | NOT NULL |
| FREQUENCY | numeric | NOT NULL |

### POPULATION_BASE_FREQUENCY_NAME
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| NAME | varchar(50) | NOT NULL |
| THETA | double precision | NOT NULL |
| MODEL | varchar(50) | NOT NULL |
| ACTIVE | boolean | NOT NULL |
| DEFAULT | boolean | NOT NULL |

### PROFILE_DATA
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| CATEGORY | varchar(50) | NOT NULL, FK → APP.CATEGORY.ID |
| GLOBAL_CODE | varchar(100) | NOT NULL |
| INTERNAL_CODE | varchar(100) | NOT NULL |
| DESCRIPTION | varchar(1024) | |
| ATTORNEY | varchar(100) | |
| BIO_MATERIAL_TYPE | varchar(50) | FK → APP.BIO_MATERIAL_TYPE.ID |
| COURT | varchar(100) | |
| CRIME_INVOLVED | varchar(50) | |
| CRIME_TYPE | varchar(50) | |
| CRIMINAL_CASE | varchar(50) | |
| INTERNAL_SAMPLE_CODE | varchar(50) | NOT NULL |
| ASSIGNEE | varchar(50) | NOT NULL |
| LABORATORY | varchar(50) | NOT NULL |
| PROFILE_EXPIRATION_DATE | date | |
| RESPONSIBLE_GENETICIST | varchar(50) | |
| SAMPLE_DATE | date | |
| SAMPLE_ENTRY_DATE | date | |
| DELETED | boolean | NOT NULL |
| DELETED_SOLICITOR | varchar(100) | |
| DELETED_MOTIVE | varchar(8192) | |
| FROM_DESKTOP_SEARCH | boolean | NOT NULL |

### PROFILE_DATA_FILIATION
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| PROFILE_DATA | varchar(100) | NOT NULL, FK → APP.PROFILE_DATA.GLOBAL_CODE |
| FULL_NAME | varchar(150) | |
| NICKNAME | varchar(150) | |
| BIRTHDAY | date | |
| BIRTH_PLACE | varchar(100) | |
| NATIONALITY | varchar(50) | |
| IDENTIFICATION | varchar(100) | |
| IDENTIFICATION_ISSUING_AUTHORITY | varchar(100) | |
| ADDRESS | varchar(100) | |

### PROFILE_DATA_FILIATION_RESOURCES
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| PROFILE_DATA_FILIATION | varchar(100) | NOT NULL, FK → APP.PROFILE_DATA_FILIATION.PROFILE_DATA |
| RESOURCE | oid | NOT NULL |
| RESOURCE_TYPE | varchar(1) | NOT NULL |

### PROFILE_DATA_MOTIVE
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| ID_PROFILE_DATA | bigint | FK → APP.PROFILE_DATA.ID |
| DELETED_DATE | timestamp | |
| ID_DELETED_MOTIVE | bigint | NOT NULL, FK → APP.MOTIVE.ID |

### PROFILE_RECEIVED
| Column | Type | Constraints |
|--------|------|-------------|
| GLOBAL_CODE | varchar(100) | PK, NOT NULL |
| LABCODE | text | NOT NULL |
| STATUS | bigint | FK → APP.INFERIOR_INSTANCE_PROFILE_STATUS.ID |
| MOTIVE | text | |
| USER | text | |
| IS_CATEGORY_MODIFICATION | boolean | NOT NULL |
| INTERCONNECTION_ERROR | varchar(255) | |
| OPERATION_ORIGINATED_IN_INSTANCE | text | |
| DATE_RECEIVED | text | |

### PROFILE_SENT
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, FK → APP.PROFILE_DATA.ID |
| LABCODE | text | PK, NOT NULL |
| GLOBAL_CODE | varchar(100) | NOT NULL |
| STATUS | bigint | FK → APP.INFERIOR_INSTANCE_PROFILE_STATUS.ID |
| MOTIVE | text | |
| USER | varchar(255) | |
| INTERCONNECTION_ERROR | varchar(255) | |

### PROFILE_UPLOADED
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, FK → APP.PROFILE_DATA.ID |
| GLOBAL_CODE | varchar(100) | NOT NULL |
| STATUS | bigint | FK → APP.INFERIOR_INSTANCE_PROFILE_STATUS.ID |
| MOTIVE | text | |
| USER | varchar(255) | |
| INTERCONNECTION_ERROR | varchar(255) | |
| OPERATION_ORIGINATED_IN_INSTANCE | text | |
| DATE_UPLOADED | text | |

### PROTO_PROFILE
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| SAMPLE_NAME | varchar(100) | NOT NULL |
| ID_BATCH | bigint | NOT NULL, FK → APP.BATCH_PROTO_PROFILE.ID |
| ASSIGNEE | varchar(100) | NOT NULL |
| CATEGORY | varchar(100) | NOT NULL |
| STATUS | varchar(150) | NOT NULL |
| PANEL | varchar(150) | NOT NULL |
| ERRORS | text | |
| GENOTYPIFICATIONS | text | NOT NULL |
| MATCHING_RULES | text | NOT NULL |
| MISMATCHS | text | NOT NULL |
| REJECT_MOTIVE | text | |
| PREEXISTENCE | varchar(100) | |
| GENEMAPPER_LINE | varchar(50000) | NOT NULL |
| ID_REJECT_MOTIVE | bigint | FK → APP.MOTIVE.ID |
| REJECTION_USER | text | |
| REJECTION_DATE | timestamp | |

### PROVINCE
| Column | Type | Constraints |
|--------|------|-------------|
| CODE | varchar(2) | PK, NOT NULL |
| NAME | varchar(50) | NOT NULL |
| COUNTRY | varchar(2) | NOT NULL, FK → APP.COUNTRY.CODE |

### STRKIT
| Column | Type | Constraints |
|--------|------|-------------|
| ID | varchar(50) | PK, NOT NULL |
| NAME | varchar(100) | NOT NULL |
| LOCI_QTY | integer | NOT NULL |
| REPRESENTATIVE_PARAMETER | integer | NOT NULL |
| TYPE | smallint | NOT NULL, FK → APP.ANALYSIS_TYPE.ID |

### STRKIT_ALIAS
| Column | Type | Constraints |
|--------|------|-------------|
| ALIAS | varchar(100) | PK, NOT NULL |
| KIT | varchar(50) | NOT NULL, FK → APP.STRKIT.ID |

### STRKIT_LOCUS
| Column | Type | Constraints |
|--------|------|-------------|
| STRKIT | varchar(50) | PK, NOT NULL, FK → APP.STRKIT.ID |
| LOCUS | varchar(50) | PK, NOT NULL, FK → APP.LOCUS.ID |
| FLUOROPHORE | varchar(10) | |
| ORDER | integer | |

### SUPERIOR_INSTANCE_PROFILE_APPROVAL
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| GLOBAL_CODE | varchar(100) | NOT NULL |
| PROFILE | text | NOT NULL |
| LABORATORY | varchar(50) | NOT NULL |
| LABORATORY_INSTANCE_ORIGIN | varchar(50) | NOT NULL |
| LABORATORY_INSTANCE_INMEDIATE | varchar(50) | NOT NULL |
| SAMPLE_ENTRY_DATE | date | |
| RECEPTION_DATE | timestamp | |
| ERRORS | text | |
| REJECTION_USER | text | |
| REJECTION_DATE | timestamp | |
| ID_REJECT_MOTIVE | bigint | |
| REJECT_MOTIVE | text | |
| DELETED | boolean | |
| PROFILE_ASSOCIATED | text | |

### TRACE
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| PROFILE | varchar(100) | NOT NULL |
| USER | varchar(50) | NOT NULL |
| DATE | timestamp | NOT NULL |
| TRACE | varchar | NOT NULL |
| KIND | varchar(100) | NOT NULL |

### TRACE2
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | |
| PROFILE | varchar | |
| USER | varchar | |
| DATE | timestamp | |
| TRACE | varchar | |
| KIND | varchar | |

### TRACE_PEDIGREE
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| PEDIGREE | bigint | NOT NULL, FK → APP.PEDIGREE.ID |
| USER | varchar(50) | NOT NULL |
| DATE | timestamp | NOT NULL |
| TRACE | varchar | NOT NULL |
| KIND | varchar(100) | NOT NULL |

---

## STASH Schema

Stores draft/stash copies of profiles before promotion to APP. Mirrors three APP tables.

### STASH.PROFILE_DATA
Same columns as APP.PROFILE_DATA (all NOT NULL constraints identical except nullable fields).

| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| CATEGORY | varchar(50) | NOT NULL |
| GLOBAL_CODE | varchar(100) | NOT NULL |
| INTERNAL_CODE | varchar(100) | NOT NULL |
| DESCRIPTION | varchar(1024) | |
| ATTORNEY | varchar(100) | |
| BIO_MATERIAL_TYPE | varchar(50) | |
| COURT | varchar(100) | |
| CRIME_INVOLVED | varchar(50) | |
| CRIME_TYPE | varchar(50) | |
| CRIMINAL_CASE | varchar(50) | |
| INTERNAL_SAMPLE_CODE | varchar(50) | NOT NULL |
| ASSIGNEE | varchar(50) | NOT NULL |
| LABORATORY | varchar(50) | NOT NULL |
| PROFILE_EXPIRATION_DATE | date | |
| RESPONSIBLE_GENETICIST | varchar(50) | |
| SAMPLE_DATE | date | |
| SAMPLE_ENTRY_DATE | date | |
| DELETED | boolean | NOT NULL |
| DELETED_SOLICITOR | varchar(100) | |
| DELETED_MOTIVE | varchar(8192) | |
| FROM_DESKTOP_SEARCH | boolean | NOT NULL |

### STASH.PROFILE_DATA_FILIATION
All fields NOT NULL (stricter than APP.PROFILE_DATA_FILIATION).

| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| PROFILE_DATA | varchar(100) | NOT NULL, FK → STASH.PROFILE_DATA.GLOBAL_CODE |
| FULL_NAME | varchar(150) | NOT NULL |
| NICKNAME | varchar(150) | NOT NULL |
| BIRTHDAY | date | NOT NULL |
| BIRTH_PLACE | varchar(100) | NOT NULL |
| NATIONALITY | varchar(50) | NOT NULL |
| IDENTIFICATION | varchar(100) | NOT NULL |
| IDENTIFICATION_ISSUING_AUTHORITY | varchar(100) | NOT NULL |
| ADDRESS | varchar(100) | NOT NULL |

### STASH.PROFILE_DATA_FILIATION_RESOURCES
| Column | Type | Constraints |
|--------|------|-------------|
| ID | bigint | PK, NOT NULL |
| PROFILE_DATA_FILIATION | varchar(100) | NOT NULL, FK → STASH.PROFILE_DATA_FILIATION.PROFILE_DATA |
| RESOURCE | oid | NOT NULL |
| RESOURCE_TYPE | varchar(1) | NOT NULL |