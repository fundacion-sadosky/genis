---
paths:
  - "modules/core/app/**/*.scala"
  - "app/**/*.scala"
---

# Migration Patterns

When migrating a feature from legacy to core:

1. **Read the legacy implementation** in `app/<domain>/` to understand behavior
2. **Prioritize fidelity to the original code** — keep the new implementation as close as possible to the legacy to minimize the risk of introducing bugs. This is a forensic platform; behavioral equivalence matters more than code elegance.
3. **Create the new implementation** in `modules/core/app/<domain>/` preserving the package name from legacy (e.g., `user`, not `ldap`)
4. **Report all changes** from the original, classified as:
   - **Syntactic** — unavoidable syntax differences from the new stack (Scala 3 syntax, Play 3 API changes). Mechanical and low-risk.
   - **Structural** — changes forced by architectural differences (e.g., Slick 3.5 vs 2.1, different DI wiring, sync MongoDB driver vs ReactiveMongo). Inevitable but require review.
   - **Recommended** — improvements suggested but not required (e.g., better error handling, idiomatic Scala 3 patterns). Optional, discuss before applying.
5. **Validation errors**: Legacy uses `JsError.toFlatJson` (flat JSON: `{"/field": ["error"]}`). Play 3 only has `JsError.toJson` (nested JSON: `{"obj.field": [{"msg":...}]}`). This is an accepted structural change — the frontend does not parse validation error bodies.
6. **Review fidelity** using `/migration-fidelity-reviewer` — verifies behavioral equivalence with legacy
7. **Review quality** using `/code-quality-reviewer` — checks failure modes, security, conventions
8. **Write tests** using `/migration-test-writer`
9. **Register the Guice module** in `application.conf`
10. **Add routes** in `modules/core/conf/routes` with `/api/v2/` prefix
