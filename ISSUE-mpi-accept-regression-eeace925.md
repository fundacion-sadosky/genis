# Regresión: al aceptar un perfil MPI/DVI no se ejecuta el cotejo de pedigree (5.1.13)

## Síntoma

Un caso de parentesco (p. ej. Hijo categoría `IR` + Padre `RNN`) que **daba match en 5.1.12 deja de darlo en 5.1.13**. Al aceptar el/los perfiles desde *bulk upload* (step 2), el perfil se guarda pero queda `matcheable: false` y **no se crea ningún match ni pedigreeMatch**. Las categorías no-pedigree (p. ej. `SOSPECHOSO`, type 1) no se ven afectadas.

## Causa raíz

Commit **`eeace925`** (*"#202 Falta actualizar estado en bulkupload2 cuando se acepta un perfil"*) hizo la aceptación **bloqueante hasta que termina el matching**. Al reescribir `ProfileService.upsert`, el disparo del cotejo pasó de tener en cuenta el tipo de categoría a pasar siempre `None`:

```scala
// Antes (parent de eeace925):
matchingService.findMatches(globalCode, categoryService.getCategoryTypeFromFullCategory(category))
// Después (eeace925):
fireMatching(profile.globalCode)   // == matchingService.findMatches(gc, None)
```

`MatchingServiceSpark.findMatches` rutea por tipo:

```scala
matchType match {
  case None         => spak2Matcher.findMatchesInBackGround(globalCode)               // matcher directo
  case Some("MPI")  => pedigreeSparkMatcher.findMatchesInBackGround(globalCode, "MPI") // pedigree
  case _            => ()
}
```

Con `None`, los perfiles de categorías de pedigree (MPI/DVI) van al **matcher directo**, que **deliberadamente los ignora** (`Spark2Matcher.findMatchesBlocking`):

```scala
if (!category.pedigreeAssociation) {
  setMatcheableAndProcessed(globalCode)   // sólo categorías NO pedigree
}
```

→ no se marcan `matcheable`, no se cruzan, no hay match.

### Por qué no se "arregla" sólo re-ruteando

La espera bloqueante nueva sólo completa su `Promise` con los estados del matcher **directo**:

```scala
case MatchJobEndend => matchEndPromise.success(status)
case MatchJobFail   => matchEndPromise.success(status)
case _              => // ignora
```

pero el `PedigreeSparkMatcher` pushea **`PedigreeMatchJobStarted` / `PedigreeMatchJobEnded`** (estados distintos). Si sólo se re-rutea a pedigree sin tocar la espera, la aceptación de MPI/DVI **se cuelga** (el promise nunca se completa). Por eso `eeace925` terminó pasando `None` (la espera "funcionaba" para el caso directo, a costa de romper MPI/DVI).

## Fix

`app/profile/ProfileService.scala`: se extrajo la lógica a `triggerMatchingAndAwait(profile, category)` y se corrigieron las dos cosas:

1. **Ruteo por tipo**: `findMatches(gc, categoryService.getCategoryTypeFromFullCategory(category))` (en vez de `fireMatching` con `None`).
2. **Espera bloqueante**: completa el promise también con `PedigreeMatchJobEnded`.

```scala
private[profile] def triggerMatchingAndAwait(profile: Profile, category: FullCategory): Future[MatchJobStatus] = {
  matchingService.findMatches(profile.globalCode, categoryService.getCategoryTypeFromFullCategory(category))
  val matchEndPromise = Promise[MatchJobStatus]()
  matchingProcessStatus.getJobStatus().apply(
    Iteratee.foreach[MatchJobStatus] { status =>
      if (!matchEndPromise.isCompleted) status match {
        case MatchJobEndend | PedigreeMatchJobEnded => matchEndPromise.success(status)
        case MatchJobFail                           => matchEndPromise.success(status)
        case _                                      =>
      }
    }
  )
  matchEndPromise.future
}
```

`upsert` delega en este método (cubre tanto `importToProfile` como `importLinkedProtoProfile`).

Test: `test/profile/ProfileServiceMatchingRoutingTest.scala` (RED→GREEN): verifica que una categoría MPI rutea a `Some("MPI")` y completa con `PedigreeMatchJobEnded`, y que una type-1 rutea a `None` y completa con `MatchJobEndend`.

## Observaciones / cuestiones abiertas para el equipo

- **DVI (type 3):** `getCategoryTypeFromFullCategory` devuelve `Some("DVI")`, pero `findMatches` no lo maneja (`case _ => ()`). Conviene revisar si DVI debe rutear al pedigree matcher también (parece un gap preexistente, no introducido por eeace925).
- **Otros disparos de matching con `None`** (no son la aceptación bulk, pero comparten el mismo riesgo si se usan con perfiles de pedigree): `InterconnectionService:1396` y `controllers/ProfileData:187` siguen llamando `fireMatching` (None). Revisar caso por caso.
- **Robustez (bug latente aparte):** `ProfileService.validateAnalysis` hace `kits.find(k => k.id == kitId).get` sobre la lista **cacheada** de kits; si un kit existe en DB pero no en la caché (p. ej. cargado por SQL sin invalidar `Keys.strKits`), revienta con `None.get` y un mensaje inútil. Debería manejar el kit ausente con un error claro.
