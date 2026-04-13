---
paths:
  - "modules/core/app/**/*Repository*.scala"
  - "modules/core/app/**/*Service*.scala"
---

# Database Patterns

## Slick Transactions (Slick 2.1 -> 3.5 migration pattern)
- **Repository trait**: exposes only `Future[T]` — no DBIO/Slick types leak
- **Repository impl**: private `DBIO` methods (granular: `insertLocus`, `insertAlias`, etc.) composed into public methods via `db.run(action.transactionally)`
- **Service**: stays thin (delegates to repo + cache + side-effects)
- Legacy used `implicit Session` + `runInTransactionAsync`; in Slick 3.5 there are no sessions — transactional composition requires DBIO

## Cache Invalidation
- Invalidate cache **after** the DB transaction commits (in `.map` of the successful Future), **not** inside the transactional block
- This fixes a subtle legacy bug: the legacy invalidates cache inside the transaction, creating a window where another request can re-cache stale data between invalidation and commit
```scala
// CORRECT — after commit
repo.add(full).map { result =>
  result.foreach(_ => cache.pop(CacheKey))
  result
}
// WRONG — inside DBIO action
// DBIO.successful(cache.pop(...))  <- don't do this
```
