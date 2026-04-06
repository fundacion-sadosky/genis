---
name: feature-migration
description: Use when migrating a GENis feature from legacy (app/) to modern (modules/core/). Orchestrates the full migration with per-layer checkpoints. Run BEFORE migration-fidelity-reviewer, code-quality-reviewer, and migration-test-writer.
---

# Feature Migration

Orchestrate the migration of a GENis feature from legacy (`app/`) to modern (`modules/core/`). This skill defines WHAT to do and WHEN. For HOW, it delegates to existing rules: `database-patterns`, `dependency-injection`, `security-checklist`, `migration-workflow`, `testing-conventions`.

<HARD-GATE>
Do NOT skip checkpoints. Each checkpoint requires explicit user approval before proceeding.
</HARD-GATE>

## Phase 1: Analysis

1. **Detect scope:**
   - User names a package without specifying layers → full migration
   - User names specific components ("solo el repo de kits") → partial migration
   - Ambiguous → ask

2. **Inventory legacy code** — read everything in `app/<domain>/`. For each component, record:
   - File path, component type (model / repo / service / controller / module)
   - Storage type (Slick / MongoDB / LDAP / none)
   - External dependencies: already migrated in `modules/core/`, in `modules/shared/`, or not migrated
   - Cache usage (keys, invalidation)

3. **Build layer plan** — ordered list, omit non-applicable:
   1. Types/Models
   2. Repository
   3. Service
   4. Controller
   5. Wiring (Guice module + routes + application.conf)

4. **CHECKPOINT — Plan:** Present inventory table, layer plan, unmigrated dependency strategy (stub / import from shared / migrate first), and risks. **Await approval.**

## Phase 2: Implementation

**Pre-step:** Resolve unmigrated dependencies per approved strategy. Stubs marked with `// TODO: migrate <package>`.

**For each layer in the plan, execute this cycle:**

### Read
- Read the full legacy component
- Identify: logic, queries, transactions, edge cases, error handling, cache interactions

### Migrate
- Convert to Scala 3 syntax
- Apply rules automatically (no need to consult user): `database-patterns`, `dependency-injection`, `security-checklist`
- Preserve Spanish domain names matching legacy
- Fidelity over elegance: preserve observable behavior
- Place files in `modules/core/app/<domain>/` mirroring legacy package structure

### Checkpoint
Present to user:
- **Migrated code** (full file or diff)
- **Changes classified:** syntactic (Scala 2→3), structural (Slick 2→3.5, Play 2→3, javax→jakarta), rules applied
- **Proposed improvements** (only high-impact / low-change): describe what and why. Do NOT implement until approved.
- **Problems found** (missing deps, legacy bugs, ambiguities)

**Await approval before next layer.**

## Phase 3: Integration

1. Create `<Domain>Module.scala` in `modules/core/app/<domain>/` — apply `dependency-injection` rule
2. Add routes in `modules/core/conf/routes` with `/api/v2/` prefix
3. Register module in `modules/core/conf/application.conf` under `play.modules.enabled`
4. Compile: `sbt "project core" compile`. If it fails, fix and report.
5. **CHECKPOINT — Integration:** Present wiring (module, routes, conf changes). **Await approval.**

## Post-migration Handoff

After integration is approved, inform the user of next steps:

1. **`/migration-fidelity-reviewer`** — verify behavioral equivalence with legacy
2. **`/code-quality-reviewer`** — review for failure modes, security, conventions
3. **`/migration-test-writer`** — write tests following the test pyramid

Offer to run them. Do NOT run automatically.

## Behavioral Rules

- **Rules-codified → apply without asking.** If a rule in `.claude/rules/` dictates how to do something, follow it.
- **Non-codified improvements → propose at checkpoint.** Implement only with explicit approval.
- **Fidelity first.** When in doubt, preserve legacy behavior.
- **Compile when possible.** After each layer if the code can stand alone; otherwise after Phase 3.
- **Partial migration:** Execute only requested layers. Stub dependencies on non-migrated layers.
