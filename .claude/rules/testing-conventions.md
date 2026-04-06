---
paths:
  - "modules/core/test/**/*.scala"
---

# Testing Conventions

## Mock Strategy (not interchangeable)

| Dependency type | Mechanism | Where defined |
|-----------------|-----------|---------------|
| Stateless (repos, services returning fixed values) | `mock[T]` (Mockito) | Inline in test |
| Stateful (cache, stores with get/set) | In-memory stub class | `test/fixtures/` |
| Controller/integration test with Guice | Module override `bind[X].to[StubX]` | `test/<domain>/` |

## File Location & Naming

| Tier | Location | Naming | Needs Docker |
|------|----------|--------|-------------|
| Unit | `test/unit/<domain>/` | `*Test.scala` | No |
| Controller | `test/integration/controllers/` | `*Test.scala` | No |
| Infrastructure | `test/integration/<domain>/` | `*IntegrationTest.scala` | Yes |

## Imports (use these, never legacy)

```scala
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar      // NOT org.scalatest.mock.MockitoSugar
import org.mockito.ArgumentMatchers.any             // NOT org.mockito.Matchers
import org.scalatestplus.play.*                     // for controller tests
import org.scalatestplus.play.guice.GuiceOneAppPerTest
```

## Slick + ScalaTest `===` conflict

ScalaTest `Matchers` defines `===` on `Any`, shadowing Slick's `===` on `Rep[T]`.
Result: Slick silently generates `where false`.

**Rule:** In any test extending `Matchers` that builds Slick queries with `===`,
import `slick.jdbc.PostgresProfile.api.*` **local to the block** where the query is built.
Applies to `beforeEach`, `afterEach`, private helpers — any method building a Slick query.

## Infrastructure Test Rules

- **Self-contained**: create data in `beforeEach`, clean in `afterEach`
- **Defensive cleanup in `beforeEach`**: clean BEFORE inserting (in case prior run aborted)
- **Test IDs with recognizable prefix** (`TEST_INTEGRATION`, `TEST_KIT_INT`)
- **LDAP exception**: use known seed data (user `setup`), no create/delete needed

## Checklists

### Unit Test
- [ ] No external dependencies (DB, LDAP, HTTP)
- [ ] Mocks for all injected dependencies
- [ ] Located in `test/unit/<domain>/`
- [ ] Uses domain fixtures from `test/fixtures/`

### Controller/Route Test
- [ ] Uses `GuiceOneAppPerTest` with stubs (no Docker)
- [ ] Located in `test/integration/controllers/`
- [ ] Disables modules that connect to real infrastructure
- [ ] Multi-domain modules: provide stubs for ALL services the router needs

### Infrastructure Integration Test
- [ ] Documents which Docker containers are needed
- [ ] Located in `test/integration/<domain>/`
- [ ] Self-contained with defensive cleanup
- [ ] Test IDs with `TEST_*` prefix
- [ ] Local `slick.jdbc.PostgresProfile.api.*` import if using `===` with `Matchers`

## Full reference

For examples, patterns, and detailed explanations: see `.claude/TESTING_STRATEGY.md`.
