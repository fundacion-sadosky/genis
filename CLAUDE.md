# CLAUDE.md

## Reglas críticas (leer siempre)

- **NO usar git** — el usuario gestiona todo (add/commit/push/pull)
- **NO hacer que `modules/core` dependa de `app/`** — son completamente independientes
- **NO proponer reescribir el frontend** — decisión cerrada: se reutiliza el AngularJS legacy tal cual
- **NO hablar de "migración por fases"** — el modern ya existe, el trabajo es *portar* funcionalidad
- NO recomendar ReactiveMongo (descartado en modern)
- NO conectarse a base de datos de producción

---

## Propósito del proyecto

**GENis** (Genetic Information System) es una herramienta forense desarrollada por Fundación Sadosky para comparar perfiles de ADN usando análisis STR (Short Tandem Repeat). Vincula evidencia genética entre causas judiciales e identificación de víctimas de desastres. Versión: **5.1.12** (legacy) / **5.2.0-SNAPSHOT** (modern).

---

## Arquitectura: dos versiones con switch

```bash
./switch.sh legacy && sdk use java 8.0.472-amzn   # → puerto 9000
./switch.sh modern && sdk use java 17.0.17-amzn    # → puerto 9001
```

`switch.sh` copia `config-legacy/` o `config-modern/` sobre `build.sbt` y `project/`, y limpia caches (target/, .bsp/, .metals/).

| | Legacy | Modern |
|---|---|---|
| Código | `app/` | `modules/core/` |
| Play / Scala | 2.3.10 / 2.11.11 | 3.0.6 / 3.3.1 LTS |
| Java / sbt | 8 / 0.13.15 | 17 / 1.10.5 |
| Puerto | 9000 | 9001 |
| Estado | **En producción** | En desarrollo — módulo vacío por ahora (`core.iml` únicamente) |

---

## Servicios y puertos

| Servicio | Puerto | Uso |
|---|---|---|
| PostgreSQL legacy | 5432 | `app/` — genisdb + genislogdb |
| PostgreSQL modern | 5433 | `modules/core/` |
| MongoDB legacy | 27017 | legacy (ReactiveMongo) |
| FerretDB modern | 27019 | modern (data migrada ✅) |
| LDAP | 1389 / 1636 | autenticación (OpenLDAP) |
| pgAdmin | 5050 | UI web |
| mongo-express | 8081 | UI web |

Config de desarrollo: `/home/genis-user/IdeaProjects/genis/application-dev.conf`

---

## Stack

### Legacy (`app/`)
Java 8 · Scala 2.11.11 · Play 2.3.10 · sbt 0.13.15 · Slick 2.1 (BoneCP) · ReactiveMongo 0.12 · UnboundID LDAP SDK 2.3.1

### Modern (`modules/core/`)
Java 17 · Scala 3.3.1 LTS · Play 3.0.6 · sbt 1.10.5 · PostgreSQL JDBC 42.7.5 · Slick 3.5.2 (HikariCP) · UnboundID LDAP SDK 7.0.4

### Frontend
AngularJS 1.4 — reutilizado tal cual (legacy y modern exponen APIs compatibles). No modificar.

---




# Linting
sbt scalastyle

# Build producción
sbt dist   # genera ZIP en target/universal/
```

---

## Estructura legacy `app/`

### Guice DI
Módulo maestro: `app/pdgconf/PdgModule.scala` — instala 22 sub-módulos:

```
ProfileModule · MatchingModule · ProbabilityModule · ProfileDataModule
ConfigDataModule · NotificationModule · StatsModule · SearchModule
LaboratoryModule · BulkUploadModule · SecurityModule · OperationLogModule
UsersModule · PedigreeModule · ScenarioModule · StrKitModule
TraceModule · InterconnectionModule · DisclaimerModule · MotiveModule · ReportingModule
```

### Paquetes principales

| Paquete | Rol |
|---------|-----|
| `pdgconf/` | PdgGlobal, PdgModule, filtros (LogginFilter, BodyDecryptFilter, InterconnectionFilter) |
| `profile/` | Ingesta y consulta de perfiles; ProfileRepository (MongoDB + Postgres) |
| `matching/` | Algoritmos de coincidencia; MatchingService, MatchingCalculatorService |
| `profiledata/` | Metadata de perfiles, genotipificación, códigos de muestra |
| `pedigree/` | Gestión de pedigrees familiares |
| `probability/` | Análisis estadístico, likelihood ratios |
| `kits/` | Definiciones STR, loci, tipos de análisis |
| `user/` | Autenticación LDAP, roles; UserRepository |
| `security/` | AuthService, OTPService (TOTP), CryptoService |
| `reporting/` | Generación de PDF e informes (ver sección abajo) |
| `controllers/` | Capa MVC Play (20+ controllers) |
| `connections/` | Intercambio con instancias superiores/inferiores |
| `scenarios/` | Gestión de causas judiciales |
| `configdata/` | Datos de referencia del sistema (categorías, reglas) |
| `bulkupload/` | Importación masiva de perfiles |
| `types/` | Value objects: Permission, SampleCode, AlphanumericId, TotpToken |
| `models/` | Definiciones Slick auto-generadas |

### Autenticación
- **LDAP** (OpenLDAP, localhost:1389): pool de 10 conexiones; base DN `dc=genis,dc=local`
- **Sesión Play**: cookie firmada con secret en `application.conf`
- **`X-USER` / `X-SUPERUSER`**: headers validados contra la sesión (cookie firmada) — nunca confiar en el header solo
- **OTP/TOTP**: biblioteca Aerogear; usuario setup tiene secret `ETZK6M66LFH3PHIG`
- **CryptoService** (AES): BodyDecryptFilter descifra bodies de request

### Base de datos (legacy)
- **PostgreSQL** (5432): Slick 2.1, driver `pdgconf.ExtendedPostgresDriver` (slick-pg 0.8.5)
  - `db.default` → genisdb (transaccional)
  - `db.logDb` → genislogdb (auditoría)
  - Schema via Play Evolutions (`/conf/evolutions/`)
- **MongoDB** (27017): ReactiveMongo 0.12, JSONCollection API
  - Documentos de perfiles y datos de genotipificación crudos

### Rutas
Archivo único: `conf/routes` (formato Play 2.3).
Type binders personalizados: `types.AlphanumericId`, `types.SampleCode`.

---

## Patrón por dominio (`modules/core/`)

Cuando se porte funcionalidad al modern, cada dominio sigue esta estructura:

```
domain/
├── {Domain}Service (trait)        # Interfaz de negocio
├── {Domain}ServiceImpl            # @Singleton + @Inject
├── {Domain}Repository (trait)     # Interfaz de acceso a datos
├── {Domain}RepositoryImpl         # Implementación Slick / JDBC
└── {Domain}Module                 # Binding Guice
```

Los módulos se registran en `modules/core/conf/application.conf` bajo `play.modules.enabled`.

---

## Generación de PDF (`app/reporting/`)

Stack: **Flying Saucer** (xhtmlrenderer) + **iText 5** → HTML/CSS → PDF.

### Archivos clave
| Archivo | Rol |
|---------|-----|
| `PdfGenerator.scala` | Servicio principal; `HtmlDocumentBuilder` → `ITextRenderer` |
| `PdfUserAgent.scala` | Override de carga de recursos (imágenes, CSS); convierte URLs a rutas de classpath |
| `ReportingModule.scala` | Provee `PdfGenerator`; carga fuentes DejaVuSans.ttf y DejaVuSans-Bold.ttf |
| `ProfileReportService.scala` | Lógica de negocio; genera PDF y CSV con datos de Mongo + Postgres |

### Fuentes
- Cargadas desde classpath: `assets/stylesheets/report/DejaVuSans.ttf` y `DejaVuSans-Bold.ttf`
- En CSS de reportes usar **`font-family: 'DejaVu Sans'`** (nombre exacto del TTF)
- No usar 'Lato' ni otras fuentes no cargadas

### Recursos en PDF (imágenes, CSS)
`PdfUserAgent.toClasspathPath(uri)` convierte URLs resueltas por Flying Saucer a rutas de classpath:
- `http://HOST/assets/images/foo.png` → `public/images/foo.png`
- `http://HOST/assets/stylesheets/foo.css` → `public/stylesheets/foo.css`

**El logo en reportes** está embebido como `data:image/png;base64,...` en `app/views/headerReporting.scala.html` — no depende de ninguna URL ni ruta de archivo.

### Templates de reportes
Ubicados en `app/views/`. El `@headerReporting()` debe estar **dentro de `<body>`**, no en `<head>`.
CSS de reportes: `app/assets/stylesheets/reporting.css` (estilos de tabla incluidos; no depende de Bootstrap).

### BASE_URL para PDF
```scala
val PROTOCOL = Play.current.configuration.getString("instanceInterconnection.protocol")
val BASE_URL  = PROTOCOL + Play.current.configuration.getString("instanceInterconnection.localUrl")
```
En `application-dev.conf`: `localUrl = "192.168.5.218:9000"` — puede ser una IP no accesible desde WSL2.
El `PdfUserAgent` carga recursos desde classpath (no por HTTP), por lo que el valor de `localUrl` no afecta la carga de activos en PDF.

---

## Configuración

| Archivo | Contenido |
|---------|-----------|
| `conf/application.conf` | Incluye play.conf, genis-misc.conf, storage.conf, interconnect.conf, akka.conf |
| `conf/play.conf` | Application secret, router, global handler |
| `conf/storage.conf` | PostgreSQL, MongoDB, LDAP, BoneCP pool |
| `conf/interconnect.conf` | URLs de instancias superiores/inferiores, timeouts |
| `application-dev.conf` | Overrides de desarrollo (DB local, LDAP local, laboratório SHDG) |

---

## Decisiones cerradas

- Frontend AngularJS reutilizado — no reescribir
- `X-USER`/`X-SUPERUSER` del cliente **deben** validarse contra la sesión (cookie firmada de Play)
- FerretDB (27019) operativo con data legacy migrada
- ReactiveMongo descartado en modern
- Entorno: WSL2 Ubuntu 24.04 desde Windows / VS Code / IntelliJ IDEA Ultimate
