# GENis - Guía Completa de Arquitectura y Funcionamiento

## 📋 Tabla de Contenidos
1. [Introducción](#introducción)
2. [¿Qué es GENis?](#qué-es-genis)
3. [Arquitectura General](#arquitectura-general)
4. [Stack Tecnológico](#stack-tecnológico)
5. [Flujo de Autenticación](#flujo-de-autenticación)
6. [Componentes Principales](#componentes-principales)
7. [Base de Datos](#base-de-datos)
8. [Módulos del Sistema](#módulos-del-sistema)
9. [Flujo de Datos](#flujo-de-datos)
10. [Configuración](#configuración)

---

## Introducción

Esta guía documenta la arquitectura, componentes y funcionamiento interno de **GENis 6.0** (versión moderna con Play Framework 3.x y Scala 2.13).

---

## ¿Qué es GENis?

**GENis** (Genetic Profiles National Database) es una herramienta informática desarrollada por la **Fundación Dr. Manuel Sadosky** que permite:

### Propósito Principal
- **Comparar perfiles genéticos** de muestras biológicas
- **Vincular eventos** ocurridos en diferentes tiempos y lugares
- **Investigación forense**: Identificación de delincuentes, personas desaparecidas, víctimas de desastres
- **Análisis de ADN**: Perfiles nucleares y mitocondriales

### Casos de Uso
1. **Escenas de Crimen**: Comparar ADN de escenas con perfiles de individuos
2. **Casos de Personas Desaparecidas**: Búsqueda de familiares
3. **Gestión de Desastres**: Identificación de víctimas
4. **Análisis Genealógico**: Estudios de filiación

---

## Arquitectura General

```
┌─────────────────────────────────────────────────────────┐
│                   Cliente Web (Browser)                 │
│                    HTML + JavaScript                    │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP/HTTPS
                     │
┌─────────────────────────────────────────────────────────┐
│          Play Framework 3.x (Scala + JVM)               │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Controllers: Manejo de Requests HTTP             │  │
│  │  - UserController: Autenticación                  │  │
│  │  - ProfileController: Gestión de perfiles         │  │
│  │  - MatchingController: Búsqueda de coincidencias  │  │
│  └───────────────────────────────────────────────────┘  │
│                     │                                    │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Services: Lógica de Negocio                      │  │
│  │  - AuthService: Autenticación y autorización      │  │
│  │  - ProfileService: Gestión de perfiles            │  │
│  │  - MatchingService: Algoritmos de comparación     │  │
│  └───────────────────────────────────────────────────┘  │
│                     │                                    │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Repositories: Acceso a Datos                     │  │
│  │  - UserRepository                                 │  │
│  │  - ProfileRepository                              │  │
│  │  - MatchRepository                                │  │
│  └───────────────────────────────────────────────────┘  │
└────────────┬────────────────────┬──────────────────────┘
             │                    │
    ┌────────▼─────────┐  ┌──────▼──────────┐
    │   PostgreSQL     │  │  OpenLDAP       │
    │   (genisdb +     │  │  (Autenticación)│
    │   genislogdb)    │  │                 │
    └──────────────────┘  └─────────────────┘
```

---

## Stack Tecnológico

### Backend
| Componente | Versión | Propósito |
|-----------|---------|----------|
| **Play Framework** | 3.0.0 | Framework web y HTTP |
| **Scala** | 2.13.12 | Lenguaje de programación |
| **Java** | JDK 8 | Runtime (JVM) |
| **Slick ORM** | 6.1.0 | Acceso a bases de datos |
| **GuardianCrypt** | - | Encriptación |

### Bases de Datos
| Base de Datos | Versión | Puerto | Propósito |
|---|---|---|---|
| **PostgreSQL** | 14.9 (Docker) / 18.1 (Client) | 5455 | Datos principales (genisdb, genislogdb) |
| **OpenLDAP** | - | 1389 | Autenticación corporativa |

### Frontend (Legado)
- **AngularJS**: Framework JavaScript
- **HTML5 + CSS**: Markup y estilos
- **Bootstrap**: Framework CSS

### Herramientas de Build
| Herramienta | Versión | Uso |
|---|---|---|
| **SBT** | 1.x | Build tool para Scala |
| **Docker** | - | Contenedores para servicios |

---

## Flujo de Autenticación

### 1. Inicio de Sesión
```
Usuario → Login Page
         ↓
    [Credenciales]
         ↓
  UserController.login()
         ↓
  AuthService.authenticate()
         ↓
  ¿LDAP Habilitado?
  /    \
 SÍ    NO
  |     |
  ▼     ▼
LDAP  LocalAuth
  |     |
  └─┬───┘
    ▼
  ValidatePassword (BCrypt)
    ▼
  ¿Válido?
  /      \
 SÍ      NO
  |       ├─→ Deny
  ▼       
GenerateJWT
  ▼
SetCookie
  ▼
Redirect Dashboard
```

### 2. TOTP (Two-Factor Authentication)
```
JWT válido
  ↓
¿TOTP requerido?
  / \
 SÍ  NO
  |   └─→ Full Access
  ▼
TOTPPrompt
  ↓
User scans QR / Ingresa código
  ↓
AuthService.validateTOTP()
  ↓
¿Código válido?
  / \
 SÍ  NO
  |   └─→ Deny
  ▼
EnableFullAccess
```

### 3. Validación en Requests
```
Every HTTP Request
  ↓
AuthFilter
  ↓
Check JWT en Cookie/Header
  ↓
¿JWT válido?
  / \
 SÍ  NO
  |   └─→ 401 Unauthorized
  ▼
¿TOTP completado?
  / \
 SÍ  NO
  |   └─→ 403 Forbidden
  ▼
Extract User Info
  ↓
Context.user = CurrentUser
  ▼
Continue Request
```

---

## Componentes Principales

### 1. Controllers (Controladores HTTP)
**Ubicación**: `app/controllers/`

```scala
// UserController.scala - Gestión de usuarios y autenticación
GET  /login              → renderLoginForm()
POST /login              → authenticate()
POST /logout             → logout()
POST /totp/validate      → validateTOTP()

// ProfileController.scala - Gestión de perfiles de ADN
GET  /profiles           → listProfiles()
GET  /profiles/:id       → getProfileDetail()
POST /profiles           → createProfile()
PUT  /profiles/:id       → updateProfile()
DELETE /profiles/:id    → deleteProfile()

// MatchingController.scala - Búsqueda de coincidencias
POST /matching/search    → searchMatches()
GET  /matching/:id       → getMatchDetail()
POST /matching/:id/report → generateReport()
```

### 2. Services (Servicios de Lógica)
**Ubicación**: `app/services/`

```scala
// AuthService
- authenticate(username, password): Either[Error, JWT]
- validateTOTP(user, code): Boolean
- validateJWT(token): Option[User]
- hashPassword(plaintext): String
- comparePassword(plain, hashed): Boolean

// ProfileService
- createProfile(profileData): Profile
- getProfilesByLaboratory(lab): List[Profile]
- compareProfiles(profile1, profile2): MatchResult
- extractAlleles(profileData): List[Allele]

// MatchingService
- searchMatches(profile, criteria): List[MatchResult]
- calculateLR(genotype1, genotype2): Double
- filterByStringency(matches, threshold): List[MatchResult]
- generateReport(match): PDFReport
```

### 3. Repositories (Acceso a Datos)
**Ubicación**: `app/repositories/`

```scala
// UserRepository
- findByUsername(username): Option[User]
- save(user): User
- update(user): User
- delete(userId): Boolean
- findAll(): List[User]

// ProfileRepository
- findById(id): Option[Profile]
- findByCode(code): Option[Profile]
- save(profile): Profile
- findByLaboratory(labId): List[Profile]

// MatchRepository
- findById(matchId): Option[Match]
- save(match): Match
- findByProfile(profileId): List[Match]
```

### 4. Models (Modelos de Datos)
**Ubicación**: `app/models/`

```scala
// User.scala
case class User(
  id: String,
  username: String,
  email: String,
  passwordHash: String,
  laboratoryId: String,
  role: Role,
  active: Boolean,
  totpSecret: Option[String],
  createdAt: DateTime
)

// Profile.scala
case class Profile(
  id: String,
  code: String,
  laboratoryId: String,
  analysisType: String,  // Nuclear, Mitochondrial, etc
  alleles: Map[String, List[String]],
  status: ProfileStatus,
  createdAt: DateTime
)

// Match.scala
case class Match(
  id: String,
  profile1Id: String,
  profile2Id: String,
  matchType: MatchType,
  lr: Double,  // Likelihood Ratio
  pValue: Double,
  status: MatchStatus,
  createdAt: DateTime
)
```

### 5. Modules (Inyección de Dependencias)
**Ubicación**: `app/modules/`

```scala
// ApplicationModule.scala
class ApplicationModule extends Module {
  def bindings(env, config) = Seq(
    bind[AuthService].to[AuthServiceImpl],
    bind[ProfileService].to[ProfileServiceImpl],
    bind[LdapService].to[LdapServiceImpl],
    bind[UserRepository].to[UserRepositoryImpl]
  )
}

// LdapModule.scala
class LdapModule extends Module {
  def bindings(env, config) = Seq(
    bind[LdapService].to[LdapServiceImpl],
    bind[LdapConnectionPool].toSelf.in[Singleton]
  )
}
```

### 6. Filters (Middleware)
**Ubicación**: `app/filters/`

```scala
// AuthFilter.scala
- Intercepts all requests
- Validates JWT/TOTP
- Injects CurrentUser into Context

// LoggingFilter.scala
- Logs all requests/responses
- Captures audit information
- Stores in genislogdb
```

---

## Base de Datos

### PostgreSQL: genisdb (Datos Principales)

```sql
-- Tablas Principales

-- Usuarios
users (
  id UUID,
  username VARCHAR UNIQUE,
  email VARCHAR,
  password_hash VARCHAR,
  laboratory_id UUID,
  role VARCHAR,
  totp_secret VARCHAR,
  active BOOLEAN,
  created_at TIMESTAMP
)

-- Perfiles de ADN
profiles (
  id UUID,
  code VARCHAR UNIQUE,
  laboratory_id UUID,
  analysis_type VARCHAR,  -- Nuclear, Mitochondrial
  status VARCHAR,  -- Active, Deleted, Pending
  created_at TIMESTAMP
)

-- Alelos (datos genéticos)
alleles (
  id UUID,
  profile_id UUID,
  locus VARCHAR,
  allele_values VARCHAR[],
  created_at TIMESTAMP
)

-- Coincidencias (resultados de búsqueda)
matches (
  id UUID,
  profile_id_1 UUID,
  profile_id_2 UUID,
  match_type VARCHAR,
  lr FLOAT,  -- Likelihood Ratio
  p_value FLOAT,
  status VARCHAR,
  created_at TIMESTAMP
)

-- Laboratorios
laboratories (
  id UUID,
  name VARCHAR,
  code VARCHAR,
  country VARCHAR,
  province VARCHAR,
  active BOOLEAN
)

-- Roles y Permisos
roles (
  id UUID,
  name VARCHAR,
  permissions VARCHAR[]
)

-- Categorías
categories (
  id UUID,
  name VARCHAR,
  description TEXT
)
```

### PostgreSQL: genislogdb (Auditoría)

```sql
-- Logs de Operación
operation_logs (
  id UUID,
  user_id UUID,
  operation VARCHAR,
  entity_type VARCHAR,
  entity_id UUID,
  changes JSONB,
  timestamp TIMESTAMP,
  ip_address VARCHAR
)
```

### OpenLDAP Structure

```
dc=genis,dc=local
├── ou=people
│   ├── uid=jdoe (Usuario con atributos)
│   ├── uid=asmith
│   └── uid=bwilson
├── ou=groups
│   ├── cn=admins
│   ├── cn=geneticists
│   └── cn=operators
└── ou=categories
    ├── cn=ar-cataas-national
    └── cn=international
```

---

## Módulos del Sistema

### 1. Módulo de Autenticación (Security)
```
Input: username + password (+ TOTP code)
  ↓
LdapService.authenticate() o LocalAuth
  ↓
AuthService.generateJWT()
  ↓
Output: JWT Token + Refresh Token
```

### 2. Módulo de Perfiles (Profile Management)
```
Input: File (CSV/XML) o Manual Entry
  ↓
ProfileService.parseAndValidate()
  ↓
Extract Alleles & Metadata
  ↓
Save to ProfileRepository
  ↓
Output: Profile ID + Status
```

### 3. Módulo de Búsqueda (Matching Engine)
```
Input: Query Profile
  ↓
MatchingService.searchMatches()
  ↓
Load all Database Profiles
  ↓
For each profile:
  - Compare Alleles
  - Calculate LR (Likelihood Ratio)
  - Apply Filters
  ↓
Output: Sorted List of Matches + LR values
```

### 4. Módulo de Reportes (Reporting)
```
Input: Match ID
  ↓
ReportingService.generateReport()
  ↓
Fetch Match Data
  ↓
Calculate Statistics
  ↓
Generate PDF
  ↓
Output: PDF File
```

### 5. Módulo de Auditoría (Audit)
```
Every Operation:
  - Log User Action
  - Log Entity Changes (JSONB)
  - Store in genislogdb
  - Enable Compliance & Forensics
```

---

## Flujo de Datos

### Flujo de Búsqueda de Coincidencias

```
1. User selects Profile (A)
2. Clicks "Search for Matches"
3. HTTP POST /matching/search { profileId: "A" }
   ↓
4. MatchingController.searchMatches()
   ↓
5. MatchingService.searchMatches()
   - Loads Profile A from Database
   - Extracts Alleles from Profile A
   ↓
6. Query Database: SELECT * FROM profiles WHERE status='Active'
   ↓
7. For each Database Profile (B):
   a. Load Alleles of Profile B
   b. Compare Alleles A vs B:
      - Count matching loci
      - Calculate Likelihood Ratio (LR)
      - Calculate P-value
   c. Apply Stringency Filters
   d. If Match meets threshold: Add to Results
   ↓
8. Sort Results by LR (descending)
   ↓
9. Paginate Results
   ↓
10. Return JSON Response with Match Data
    ↓
11. Frontend renders Match Table
```

### Flujo de Ingreso de Perfil

```
1. User uploads CSV/XML file or enters data manually
2. HTTP POST /profiles { profileData }
   ↓
3. ProfileController.createProfile()
   ↓
4. ProfileService.parseAndValidate()
   - Parse file format
   - Extract: code, alleles, analysis_type, etc
   - Validate: Check format, values, required fields
   ↓
5. Check Duplicates: 
   - Query: SELECT * FROM profiles WHERE code = ?
   ↓
6. If duplicate exists: Return Error
   Else: Continue
   ↓
7. Save to ProfileRepository
   - INSERT INTO profiles (...)
   - INSERT INTO alleles (...) for each locus
   ↓
8. Update Cache (if applicable)
   ↓
9. Trigger Matching (optional):
   - Call MatchingService.searchMatches()
   ↓
10. Return Profile ID + Status
```

---

## Configuración

### Archivo: `conf/application-moderno.conf`

```properties
# ========================================
# DATABASE
# ========================================
slick.dbs.default.db.url = "jdbc:postgresql://localhost:5455/genisdb"
slick.dbs.default.db.user = "genissqladmin"
slick.dbs.default.db.password = "genissqladminp"

slick.dbs.logDb.db.url = "jdbc:postgresql://localhost:5455/genislogdb"

# ========================================
# LDAP - Autenticación
# ========================================
ldap {
  enabled = true
  provider_url = "ldap://localhost:1389"
  base_dn = "dc=genis,dc=local"
  admin_dn = "cn=admin,dc=genis,dc=local"
  admin_password = "adminp"
  user_dn_pattern = "uid={0},ou=people,dc=genis,dc=local"
}

# ========================================
# JWT - Seguridad
# ========================================
jwt {
  secret = "your-secret-key-change-in-production"
  expiration_seconds = 86400  # 24 horas
  refresh_expiration = 2592000  # 30 días
}

# ========================================
# LABORATORIO
# ========================================
laboratory {
  country = "AR"
  province = "C"  # CABA
  code = "SHDG"
}

# ========================================
# EVOLUTIONS - Migraciones BD
# ========================================
play.evolutions.enabled = true
play.evolutions.autoApply = false
play.evolutions.db.default.enabled = true
```

### Credenciales por Defecto (Desarrollo)

| Servicio | Usuario | Password | Notas |
|---|---|---|---|
| PostgreSQL | genissqladmin | genissqladminp | |
| LDAP Admin | cn=admin,dc=genis,dc=local | adminp | |
| GENis App | setup | pass | TOTP: ETZK6M66LFH3PHIG |

---

## Ejecución y Deployement

### Desarrollo Local

```bash
# Setup
chmod +x setup-moderno.sh
./setup-moderno.sh

# Compilación
sbt compile

# Ejecución
sbt run -Dconfig.file=./conf/application-moderno.conf

# Aplicar migraciones BD
sbt "run -Dplay.evolutions.autoApply=true"
```

### Production

```bash
# Build universal distribution
sbt universal:packageZipTarball

# Descomprimir
tar xzf target/universal/genis-6.0.0.develop.tgz

# Ejecutar
./genis-6.0.0.develop/bin/genis -Dconfig.file=/etc/genis/application.conf
```

---

## Resumen de Conceptos Clave

| Concepto | Explicación |
|---|---|
| **Profile (Perfil)** | Conjunto de alelos de un individuo extraído de análisis genético |
| **Allele (Alelo)** | Variante de un gen en un locus específico |
| **Locus** | Posición específica en el cromosoma (e.g., D8S1179) |
| **Match (Coincidencia)** | Resultado de comparación entre dos perfiles |
| **LR (Likelihood Ratio)** | Valor matemático que expresa la probabilidad de que dos perfiles provengan del mismo individuo |
| **TOTP** | Time-based One-Time Password (autenticación de dos factores) |
| **JWT** | JSON Web Token para manejo seguro de sesiones |
| **LDAP** | Lightweight Directory Access Protocol (directorio corporativo) |
| **Evolutions** | Sistema de migraciones de bases de datos en Play Framework |

---

## Referencias

- **Manual de Instalación**: [GitHub Files](https://github.com/fundacion-sadosky/genis/files/9739746/instalacion.pdf)
- **Manual de Usuario**: [GitHub Files](https://github.com/fundacion-sadosky/genis/files/9739748/manual.pdf)
- **Repositorio**: https://github.com/fundacion-sadosky/genis
- **Documentación Play Framework 3.x**: https://www.playframework.com/documentation/3.0.x/

---

**Última actualización**: 7 de enero de 2026  
**Versión de GENis**: 6.0.0 (Play Framework 3.0.0, Scala 2.13.12)
