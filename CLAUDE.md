# GENis - Genetic Information System

## What This Is

GENis is a **forensic genetic analysis platform** used in **real judicial cases** in Argentina. Correctness is critical — bugs can affect criminal investigations and people's lives. Treat every change with the rigor that implies.

## Project Context: Migration in Progress

We are migrating GENis **one feature at a time** from a legacy stack to a modern one. The legacy code in `app/` is read-only reference — do not modify it unless explicitly asked.

### Legacy Stack (in `app/`, read-only)
- Scala 2.11.11 / Play 2.3.10 / SBT 0.13.15 / Java 8
- Slick 2.1 + play-slick 0.8 / ReactiveMongo 0.12 / UnboundID LDAP SDK 2.3.1
- Guice 3.0 (manual wiring, javax.inject) / Apache Spark 2.1.1

### Modern Stack (in `modules/core/`, active development)
- Scala 3.3.1 / Play 3.0.6 / SBT 1.x / Java 17
- Slick 3.5.2 (pure, no play-slick) + PostgreSQL 42.7.5
- MongoDB sync driver 5.3.1 / UnboundID LDAP SDK 7.0.4
- Guice (Play-integrated, jakarta.inject) / Play Cache + Caffeine

### Architecture

```
modules/
  shared/    -> Cross-compiled (Scala 2.11 + 3.3.1), shared types (play-json)
  core/      -> Scala 3 + Play 3 — all new code goes here
app/         -> Legacy Scala 2 code (read-only reference)
test/        -> Legacy tests (read-only reference)
```

- Core module runs on port **9001** (legacy uses 9000)
- API endpoints use prefix `/api/v2/`
- Routes: `modules/core/conf/routes`
- Guice modules registered in `modules/core/conf/application.conf`

### Build & Run

```bash
sbt "project core" run          # Run the core module
sbt "project core" compile      # Compile only core
sbt "project core" test         # All core tests
```

## Code Conventions

- **Scala 3 syntax** in `modules/core/`: no braces for single-expression methods, `enum`, `given`/`using`, extension methods
- **Spanish** for domain terms matching legacy (e.g., `Motivo`, `TipoCriminal`). English for generic concepts.
- Package structure mirrors legacy: `user/`, `security/`, `configdata/`, `kits/`, `motive/`, etc.
- **Slick 3.5.2** (pure, no play-slick). Tables in `modules/core/app/models/Tables.scala`, `StrKitTables.scala`, `LocusTables.scala`

## Testing

Testing strategy, conventions, and known pitfalls are documented in `.claude/TESTING_STRATEGY.md`. Use the **`migration-test-writer`** skill when writing tests for migrated functionality.

Docker containers for integration tests: `cd utils/docker && docker-compose up -d` (postgres:5432, ldap:1389, mongo:27017).

## CI/CD

- GitHub Actions `test-regression.yml` runs on push to `new-dev` branch
- Compares test counts against baseline, alerts via Telegram on regression
- Main branch: `new-dev`
