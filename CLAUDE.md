# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---
name: genis
description: GENis project context — forensic DNA management system.
---

## Critical rules (always read)
- **DO NOT use git** — the user manages everything (add/commit/push/pull)
- **DO NOT make `modules/core` depend on `app/`** — they are completely independent
- **DO NOT propose rewriting the frontend** — closed decision: legacy AngularJS is reused as-is
- **DO NOT talk about "phased migration"** — modern already exists, the work is *porting* functionality
- DO NOT recommend ReactiveMongo (discarded)
- DO NOT connect to the production database

---

## Architecture: two versions with switch
```bash
./switch.sh legacy && sdk use java 8.0.472-amzn   # → port 9000
./switch.sh modern && sdk use java 17.0.17-amzn    # → port 9001
```

| | Legacy | Modern |
|---|---|---|
| Code | `app/` | `modules/core/` |
| Play / Scala | 2.3.10 / 2.11.11 | **3.0.6 / 3.3.1 LTS** |
| Status | In production | In development — **NOT production-ready yet** |

---

## Key services
| Service | Port | Usage |
|---|---|---|
| PostgreSQL legacy | 5432 | `app/` |
| PostgreSQL modern | **5433** | `modules/core/` |
| MongoDB legacy | 27017 | legacy |
| FerretDB modern | **27019** | modern (migrated data ✅) |
| LDAP | 1389 / 1636 | authentication |
| pgAdmin | 5050 | web UI |
| mongo-express | 8081 | web UI |

---

## Modern stack (`modules/core/`)
Java 17 · Scala 3.3.1 LTS · Play 3.0.6 · sbt 1.10.5 · PostgreSQL JDBC 42.7.5 · unboundid-ldapsdk 7.0.4

---

## Closed decisions
- Legacy AngularJS frontend reused — modern backend exposes compatible APIs
- `X-USER`/`X-SUPERUSER` headers from the client **must** be validated against the session (Play signed cookie)
- FerretDB (27019) operational with migrated legacy data
- Environment: WSL2 Ubuntu 24.04 on Windows / VS Code

---

## CI/CD (`.github/workflows/`)
- **`test-regression.yml`** — detects test regression on `new-dev`; notifies Telegram if tests drop
- **`auto-close-issues.yml`** — closes issues when a PR is merged (keywords: `Fixes #N`, `Closes #N`, etc.)
- Telegram secrets: `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`, `TELEGRAM_THREAD_ID` — **never hardcode**

---

## Common Commands

All commands assume the **modern config** is active (run `./switch.sh modern` first).

```bash
# Run the application (port 9001)
sbt run

# Run all tests for the modern module
sbt "core/test"

# Run a single test class
sbt "core/testOnly security.OTPServiceSpec"

# Run tests matching a description string
sbt "core/testOnly * -- -z 'some test name'"

# Build production distribution zip
sbt dist

# Check code style (Scalastyle)
sbt scalastyle

# View dependency graph
sbt dependencyTree
```

For legacy, run `sbt run` after `./switch.sh legacy` (targets `app/`, no module prefix).

---

## Modern Module Layout (`modules/core/`)

```
app/
  controllers/     — Play HTTP controllers, REST API v2 routes
  services/        — Business logic (LaboratoryService, GeneticistService, UserService, …)
  security/        — AuthService, OTPService, CryptoService + SecurityModule (Guice)
  user/            — LDAP-backed user/role repositories, UsersModule (Guice)
  configdata/      — BioMaterialTypes, CrimeTypes (repos/services/module)
  models/          — Slick table definitions (Tables.scala, StrKitTables.scala)
  types/           — Domain value types (TotpToken, Permission, User, …)
  CoreModule.scala — Top-level Guice bindings
conf/
  application.conf      — Main config (PostgreSQL, FerretDB, LDAP, security)
  application.test.conf — Test overrides: H2 in-memory DB, TestSecurityModule
  routes               — All /api/v2/* route declarations
test/
  controllers/   — Controller integration specs
  security/      — OTP/Crypto unit specs
  unit/          — Pure unit tests (no Play app started)
  fixtures/      — Test helpers (CacheFixtures, SecurityFixtures)
```

The `modules/shared/` module contains cross-compiled (Scala 2.11 + 3) domain models used by both versions.

---

## Testing Notes

- Tests load `application.test.conf`, which swaps in H2 in-memory DB and `TestSecurityModule` (stubs LDAP/external deps).
- The legacy `test/` directory (root level) contains Scala 2.11 tests — only run under legacy config.
- Modern tests live in `modules/core/test/` and run with `sbt "core/test"`.

---

## Default Setup Account

GENis uses TOTP (aerogear) + JWT + LDAP authentication.
- Username: `setup` · Password: `pass` · TOTP secret: `ETZK6M66LFH3PHIG`

Use for development only. Disable in production after creating a real admin account.

---

## Migration patterns (read before porting a controller)

### File locations
- Legacy source: `app/` (package names match directory: `app/profiledata/`, `app/controllers/`, etc.)
- Modern target: `modules/core/app/` (same package structure)
- Modern tests: `modules/core/test/controllers/` for controller specs, `modules/core/test/unit/` for pure unit tests

### Per-feature checklist
Each feature requires these files in `modules/core/`:
1. `app/<package>/XxxModel.scala` — domain models + JSON formats
2. `app/<package>/XxxRepository.scala` — trait + Slick impl (PostgreSQL 5433)
3. `app/<package>/XxxService.scala` — trait + impl injecting the repository
4. `app/<package>/XxxModule.scala` — Guice `AbstractModule` binding service → impl
5. `app/controllers/XxxController.scala` — `AbstractController`, inject service via CC + implicit EC
6. `conf/routes` — add `/api/v2/...` routes
7. `conf/application.conf` — add `play.modules.enabled += "package.XxxModule"`
8. `test/controllers/XxxControllerSpec.scala` — mock service, test each route

### Modern controller template
```scala
@Singleton
class XxxController @Inject()(cc: ControllerComponents, service: XxxService)
(implicit ec: ExecutionContext) extends AbstractController(cc) { ... }
```
- Parse JSON body with `parse.anyContent` + `request.body.asJson.map(_.validate[T])`
- Return `Ok/NotFound/BadRequest(Json.toJson(...))` or `Ok(bytes).as("application/octet-stream")`
- No `Enumerator` — Play 3 uses `Ok(bytes)` for binary

### Guice wiring
- Module binds: `bind(classOf[XxxService]).to(classOf[XxxServiceImpl])`
- `@Named("stashed")` qualifier (proto/stash variant): use `Names.named("stashed")` from `com.google.inject.name.Names`
- Disable module in tests: `.disable[XxxModule].overrides(bind[XxxService].toInstance(mockService))`

### Types available in `modules/core/app/types/`
`AlphanumericId`, `SampleCode`, `ConstrainedText`, `TotpToken`, `Permission`, `User`, `Laboratory`

### What's already ported (modules/core)
- Categories, BioMaterialTypes, CrimeTypes (`configdata/`)
- Laboratories, Geneticists (`services/`)
- STR Kits (`kits/`)
- Users / Security (LDAP + JWT + TOTP) (`security/`, `user/`)
- ProfileData — models, service, repo, controller (`profiledata/`) ✅
- ProtoProfileData — STASH flow controller (`controllers/ProtoProfileDataController`) ✅ (repo TODO)
- Motives (`motive/`)
- Disclaimer (`disclaimer/`)

### TODO / not yet ported
- ProtoProfileDataRepository (STASH schema) — `@Named("stashed")` currently points to main repo
- CacheService, TraceService, NotificationService — blocks `create`/`updateProfileData` in ProfileDataService
- ProfileService (profile module) — blocks `findByCodeWithAssociations`, `updateProfileCategoryData`
- PedigreeService, ScenarioRepository, MatchingService — blocks `deleteProfile`
- Bulk upload, Matching, Probability, Scenarios, Pedigree, Interconnect, Notifications, Reports

## Full reference
For non-urgent details (module map, porting order, detailed legacy stack, security), ask me or consult the conversation history.

## Porting order (dependency-driven)
1. **Categories** — foundation (no dependencies)
2. **Laboratories** — depends on Categories
3. **Kits** — STR marker definitions (depends on Categories)
4. **Users / Security** — depends on LDAP + Categories
5. **Profiles** — core entity (depends on Categories + Kits + Labs)
6. **Audit / Trace** — depends on Users + Profiles
7. **Bulk upload** — depends on Profiles + Kits
8. **Matching** — depends on Profiles + Categories
9. **Probability** — depends on Matching + Profiles
10. **Scenarios** — depends on Probability
11. **Pedigree** — most complex (depends on Matching + Scenarios)
12. **Interconnect** — upper/lower instances (depends on the local system being complete)
13. **Notifications** — Inbox (cross-cutting, but depends on other module states)
14. **Reports** — Reporting (depends on almost everything above)
