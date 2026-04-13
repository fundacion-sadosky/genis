---
paths:
  - "modules/core/app/controllers/**/*.scala"
  - "modules/core/app/**/Service*.scala"
  - "modules/core/app/**/*Service.scala"
  - "modules/core/app/**/Repository*.scala"
  - "modules/core/app/**/*Repository.scala"
---

# Security Checklist

Verify that migrated or new code does not introduce security regressions.
GENis is a forensic genetic platform — data integrity and auditability are critical.

## 1. Authentication & Authorization
- [ ] Endpoint is covered by auth filters (`BodyDecryptFilter`/`LogginFilter`), or is explicitly listed as public in `AuthService.isPublicResource()`
- [ ] Permission checks preserved from legacy — `canPerform()` role-based authorization still applies
- [ ] No accidental bypass: new endpoints default to protected (unlisted in public resources = requires auth)

## 2. Sensitive Data Protection
- [ ] Error responses return generic messages (i18n `Messages("error.XXXX")` or hardcoded Spanish), NOT raw `e.getMessage` (which leaks JDBC schema details)
- [ ] Logs do not include request/response bodies containing forensic data (genetic profiles, match results, personal identity)
- [ ] API responses return only necessary fields — no full domain objects when a subset suffices

## 3. Audit Trail
- [ ] PEO system or legacy filters still capture operations on migrated endpoints (if PEO not yet migrated, legacy `LogginFilter` must cover `/api/v2/` routes)
- [ ] Logical deletes (`deleted = true`) preferred over physical deletes for forensic data

## 4. Input Validation
- [ ] Controller validates input via `request.body.validate[T]` with Play JSON formats
- [ ] No string interpolation in Slick queries — all filters use parameterized `===`

## 5. Error Handling
- [ ] Exceptions logged server-side with sufficient context (`logger.error(msg, exception)`) before returning `Left`
- [ ] Client-facing error messages are user-friendly Spanish strings, not raw exception details
