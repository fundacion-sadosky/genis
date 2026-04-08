# Audit de Compliance - modules/core/

Fecha: 2026-04-01
Rules auditadas: database-patterns, dependency-injection, security-checklist, testing-conventions

## Resumen

| Grupo | Archivos | Con violaciones | CRITICAL | IMPORTANT | SUGGESTION |
|-------|----------|----------------|----------|-----------|------------|
| Repositories | 17 | 7 | 3 | 6 | 2 |
| Controllers | 16 | 7 | 6 | 6 | 4 |
| Services | 33 | 11 | 3 | 9 | 8 |
| Tests | 56 | 14 | 2 | 18 | 6 |
| **Total** | **122** | **39** | **14** | **39** | **20** |

Nota: los conteos de la tabla son por violación individual. En la lista de abajo, múltiples violaciones del mismo archivo o del mismo tipo se consolidan en una sola entrada para facilitar la acción.

---

## Issues por archivo

### CRITICAL

#### C01 - PopulationBaseFrequencyRepositoryImpl.scala
- **Rule:** dependency-injection
- **Issue:** Crea `Database.forConfig(...)` en línea 15 en vez de recibir `db: Database` via `@Inject()`
- **Status:** FIXED — constructor ahora recibe `db: Database` via `@Inject()`, igual que los demás repos
- **Complejidad:** baja

#### C02 - MongoProfileRepository.scala
- **Rule:** dependency-injection
- **Issue:** Crea su propio `MongoClient` internamente (líneas 25-27) sin wrapper ni lifecycle management (no hay shutdown hook)
- **Status:** FIXED — recibe `MongoDatabase` inyectado; `MongoDatabaseProvider` maneja lifecycle con `ApplicationLifecycle` shutdown hook
- **Complejidad:** media — requiere crear wrapper service + Guice binding

#### C03 - MongoController.scala
- **Rule:** dependency-injection + security-checklist
- **Issue:** (a) Instancia `MongoClients.create()` directamente en el controller (líneas 7, 30-31). (b) `e.getMessage` retornado al cliente en 3 endpoints (líneas 51, 76, 109)
- **Status:** FIXED — usa `MongoHealthService` inyectado; endpoints de diagnóstico reemplazados por `/status`; `e.getMessage` eliminado
- **Complejidad:** media — requiere crear un MongoHealthService similar a LdapHealthService

#### C04 - PostgresController.scala
- **Rule:** dependency-injection + security-checklist
- **Issue:** (a) Usa `java.sql.DriverManager` directamente (líneas 9, 35, 78). (b) `e.getMessage` + `e.getClass.getSimpleName` retornados al cliente (líneas 61-62, 112-113). (c) Username de DB expuesto en response (líneas 49, 99)
- **Status:** FIXED — usa `PostgresHealthService` inyectado con `Database` de Slick; endpoints de diagnóstico reemplazados por `/status`; `e.getMessage` y username eliminados
- **Complejidad:** media — requiere crear un PostgresHealthService similar a LdapHealthService

#### C05 - ProfilesController.scala
- **Rule:** database-patterns
- **Issue:** `Await.result(..., Duration.Inf)` bloqueante en exporterProfiles (líneas 219, 228) y exporterLimsFiles (líneas 237, 242)
- **Status:** FIXED — `exporterProfiles` y `exporterLimsFiles` convertidos a `Action.async`, eliminados Await.result
- **Complejidad:** baja

#### C06 - ProfileService.scala
- **Rule:** database-patterns + security-checklist
- **Issue:** (a) `Await.result` bloqueante en validateAnalysis (línea 275) y getAnalysisTypeOf (líneas 356-358). (b) `t.getMessage` retornado en recover del método upsert (línea 556)
- **Status:** FIXED — (a) `kitLoci` movido al for-comprehension en `validateAnalysis`, `getAnalysisTypeOf` convertido a `Future[List[AnalysisType]]` con `Future.sequence`. (b) `t.getMessage` reemplazado por mensaje genérico + `logger.error`
- **Complejidad:** media

#### C07 - ProfileExporterService.scala
- **Rule:** database-patterns
- **Issue:** `Await.result(..., Duration.Inf)` en exportProfiles (línea 49)
- **Status:** FIXED — `getMtRcrs()` y assignee lookup ejecutados en paralelo con for-comprehension
- **Complejidad:** baja

#### C08 - AuthService.scala (Await)
- **Rule:** database-patterns
- **Issue:** `Await.result(..., Duration.Inf)` en verifyInferiorInstance (líneas 300-301), 2 llamadas
- **Status:** FIXED — `verifyInferiorInstance` retorna `Future[Try[String]]`, las 2 llamadas a repos compuestas en for-comprehension, trait + tests actualizados
- **Complejidad:** media

#### C09 - UserAndRoleEndpointsTest.scala
- **Rule:** testing-conventions
- **Issue:** (a) Usa `GuiceOneAppPerSuite` en vez de `GuiceOneAppPerTest`. (b) No deshabilita módulos de infraestructura (LDAP/DB). (c) Usa "should" en vez de "must"
- **Status:** FIXED — archivo eliminado, 100% redundante con UserAndRoleControllerTest.scala
- **Complejidad:** baja

### IMPORTANT

#### I01 - CategoryRepository.scala
- **Rule:** database-patterns
- **Issue:** Trait expone `Tables.*Row` types (Slick) en return types (líneas 16-19)
- **Status:** FIXED — creados DTOs en `CategoryExportTypes.scala` (`CategoryConfigurationExport`, `CategoryAssociationExport`, `CategoryAliasExport`, `CategoryMatchingExport`); traits de repo y service retornan DTOs; controller usa DTOs en Reads/Writes/import; JSON de export/import preservado idéntico
- **Complejidad:** media — requiere crear domain types y mapear

#### I02 - LdapRepository.scala + LdapRoleRepository.scala + LdapUserRepository.scala
- **Rule:** dependency-injection
- **Issue:** Inyectan `LDAPConnectionPool`/`LDAPConnection` directamente sin wrapper project-owned
- **Status:** OPEN
- **Complejidad:** media — pero LdapConnectionPoolFactory ya existe como wrapper parcial

#### I03 - LdapRoleRepository.scala (Await)
- **Rule:** database-patterns
- **Issue:** `Await.result(getRoles, 10.seconds)` bloqueante en rolePermissionMap (línea 46)
- **Status:** FIXED — extraído `fetchRolesSync()` y `updateRoleSync()` como helpers síncronos, eliminados los 3 Await del archivo
- **Complejidad:** baja-media

#### I04 - CategoryService.scala
- **Rule:** security-checklist
- **Issue:** `e.getMessage` retornado en recover de exportCategories (línea 192), sin logger.error previo
- **Status:** FIXED
- **Complejidad:** baja

#### I05 - UserService.scala
- **Rule:** security-checklist
- **Issue:** (a) `t.getMessage` retornado en recover de setStatus (línea 207). (b) `.get` sin guard en isSuperUserByGeneMapper (línea 260)
- **Status:** FIXED
- **Complejidad:** baja

#### I06 - GeneticistsController.scala
- **Rule:** dependency-injection
- **Issue:** Catch de `PSQLException` (PostgreSQL JDBC) directamente en controller (línea 7, 37). Debería manejarse en service/repo
- **Status:** FIXED — `GeneticistService.add` retorna `Future[Either[String, Int]]`, PSQLException se maneja en el service, controller mapea Either
- **Complejidad:** baja-media

#### I07 - StatusController.scala
- **Rule:** security-checklist
- **Issue:** `e.getMessage` retornado al cliente en ldapStatus (línea 63)
- **Status:** FIXED
- **Complejidad:** baja

#### I08 - ProfilesController.scala (deletes)
- **Rule:** security-checklist
- **Issue:** `removeAll()` (líneas 175-179) y `removeProfile()` (líneas 182-187) hacen delete físico de datos forenses en MongoDB
- **Status:** PARTIAL — `removeAll()` eliminado (código muerto: endpoint legacy retornaba `???`, borraba colecciones enteras). `removeProfile()` mantenido como hard delete: su uso real es cleanup de perfiles parcialmente creados durante bulk upload fallido, no borrado de datos forenses en producción
- **Complejidad:** alta — requiere schema changes, campo deleted, lógica de filtrado

#### I09 - CryptoService.scala
- **Rule:** security-checklist
- **Issue:** Salt hardcodeado estático `"agentsalt"` para PBKDF2 (línea 71)
- **Status:** WONTFIX — heredado del legacy, se migrarán datos encriptados con este salt
- **Complejidad:** alta — cambiar rompe backward compatibility con datos encriptados existentes

#### I10 - AuthService.scala (OTP logging)
- **Rule:** security-checklist
- **Issue:** Loguea el valor del OTP token en logger.warn (línea 279)
- **Status:** FIXED
- **Complejidad:** baja

#### I11 - CategoriesController.scala
- **Rule:** security-checklist
- **Issue:** (a) `removeAllCategories()`/`removeAllGroups()` en processImport — delete físico. (b) Path hardcodeado `/tmp/categories.json` + `/tmp/` en readJsonFile
- **Status:** PARTIAL — (b) FIXED: `exportCategories` usa `Files.createTempFile`; `readJsonFile` lee directo de Play `TemporaryFile`. (a) ACCEPTED: el delete físico es comportamiento intencional del import de configuración (reemplazo total), no dato forense; legacy hace lo mismo
- **Complejidad:** media

#### I12 - LimsArchivesExporterService.scala
- **Rule:** security-checklist
- **Issue:** `.get` en `Option[Date]` (from.get, to.get) sin check, lanza NoSuchElementException (líneas 67-69)
- **Status:** FIXED
- **Complejidad:** baja

#### I13 - RoleServiceImpl.scala
- **Rule:** database-patterns
- **Issue:** Race condition en cache invalidation — `foreach` + `flatMap` pueden ejecutar en orden no determinístico (líneas 37-38)
- **Status:** FIXED — `addRole` usa `flatMap` sin cleanCache propio (delega a `updateRole`), `updateRole` limpia cache en `.map` después del commit
- **Complejidad:** baja-media

#### I14 - Tests: PlaySpec en unit tests (8 archivos)
- **Rule:** testing-conventions
- **Issue:** Usan `PlaySpec` en vez de `AnyWordSpec with must.Matchers` — no necesitan Play infrastructure
- **Archivos:** AlleleValueTest, AnalysisTest, ExportFiltersTest, LabelTest, ProfileAsociationTest, ProfileServiceTest, ProfileTest, MotiveServiceTest
- **Status:** FIXED
- **Complejidad:** baja — mecánico

#### I15 - Tests: "should" en vez de "must" (11 archivos)
- **Rule:** testing-conventions
- **Issue:** Usan `"should"` style blocks en vez de `"must"`
- **Archivos:** MotiveControllerTest, ProfilesControllerTest, UserAndRoleEndpointsTest (eliminado), MotiveServiceTest, AlleleValueTest, AnalysisTest, ExportFiltersTest, LabelTest, ProfileAsociationTest, ProfileServiceTest, ProfileTest, PopulationBaseFrequencyServiceTest
- **Status:** FIXED
- **Complejidad:** baja — mecánico

---

## Triage

### Fix ahora (mecánico, bajo riesgo)
- ~~I14 + I15 — Tests: PlaySpec→AnyWordSpec + should→must~~ DONE
- ~~C09 — UserAndRoleEndpointsTest~~ DONE (eliminado, redundante con UserAndRoleControllerTest)
- ~~I04, I05, I07 — e.getMessage → mensaje genérico + logger.error~~ DONE
- ~~I10 — AuthService: no loguear el OTP token~~ DONE
- ~~I12 — LimsArchivesExporterService: .get → manejo seguro~~ DONE

### Fix pronto (moderada complejidad)
- ~~C01 — PopBaseFreqRepoImpl: inyectar db en vez de Database.forConfig~~ DONE
- ~~C05, C06, C07, C08 — Await.result → async~~ DONE
- ~~I03 — LdapRoleRepository Await~~ DONE
- ~~I06 — GeneticistsController: mover catch PSQLException al repo/service~~ DONE
- ~~I13 — RoleServiceImpl: cache invalidation race condition~~ DONE

### Requiere decisión de diseño
- ~~C02, C03, C04~~ DONE — DI refactoring Mongo/Postgres controllers + MongoProfileRepository
- ~~I01~~ DONE — CategoryRepository: DTOs reemplazan Slick Row types en traits
- I02 — LDAP repos: wrapping de infraestructura
- ~~I08~~ PARTIAL — `removeAll()` eliminado (dead code); `removeProfile()` mantenido (cleanup de uploads parciales)
- ~~I11~~ DONE — CategoriesController: path hardcodeado fixeado; physical deletes aceptados (dato de configuración)

### Deuda aceptada / Won't fix
- I09 — CryptoService salt hardcodeado: heredado del legacy, se migrarán datos encriptados con este salt
- I11a — Physical deletes en import de categorías: comportamiento intencional del feature (reemplazo total de configuración), no dato forense; legacy idéntico
