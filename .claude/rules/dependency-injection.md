---
paths:
  - "modules/core/app/**/*Module*.scala"
  - "modules/core/app/**/*Controller*.scala"
  - "modules/core/app/**/*Repository*.scala"
  - "modules/core/conf/application.conf"
---

# Dependency Injection

- Guice modules per domain: `SecurityModule`, `UsersModule`, `CoreModule`, `DisclaimerModule`, `StrKitModule`, `MotiveModule`, etc.
- Register new modules in `modules/core/conf/application.conf` under `play.modules.enabled`
- **Slick `Database` injection**: repositories must receive `Database` via `@Inject()(db: Database)`, not create their own `Database.forConfig(...)`. The singleton instance is bound in `configdata.Module`. Creating a local instance produces a separate connection pool, wasting resources and bypassing Guice lifecycle (no coordinated shutdown).
- **No infrastructure types in controllers**: controllers must never inject concrete external types (`LDAPConnectionPool`, `Database`, MongoDB clients, etc.) directly. Always wrap in a project-owned trait + impl (e.g., `LdapHealthService` / `LdapHealthServiceImpl`), bound in the domain's Guice module. This keeps tests clean: stubs in `test/fixtures/` instead of Mockito mocks of external classes in Guice overrides.
