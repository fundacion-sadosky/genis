# Estrategia de Testing - GENis

## Estrategia General: Test Pyramid

```
        /\
       /E2E\        <- Pocos, solo flujos criticos completos
      /------\
     /Integration\  <- Algunos, contra Dockers reales
    /--------------\
   /   Unit Tests   \ <- Muchos, con mocks, sin I/O
  /------------------\
```

## Framework

**ScalaTest** con estilo "must":
```scala
class MyServiceTest extends AnyWordSpec with Matchers {
  "MyService" must {
    "do something" in {
      result mustBe expected
    }
  }
}
```

## Estructura de Carpetas

```
modules/core/test/
  unit/
    security/
      AuthServiceTest.scala
      CryptoServiceTest.scala
      OTPServiceTest.scala
    types/
      ConstrainedTextTest.scala
      PermissionTest.scala
      TotpTokenTest.scala
    user/
      LdapUserRepositoryTest.scala
      RoleTest.scala
      UserStatusTest.scala
  integration/
    controllers/
      AuthenticationTest.scala
      StatusControllerTest.scala
    security/
      AuthServiceIntegrationTest.scala
    user/
      LdapUserRepositoryIntegrationTest.scala
  fixtures/
    UserFixtures.scala
    SecurityFixtures.scala
    CacheFixtures.scala
    ...
  security/
    TestSecurityModule.scala
```

## Unit Tests

**Caracteristicas:**
- Sin I/O (no DB, no LDAP, no HTTP)
- Instanciacion directa del servicio bajo test (sin Guice)
- Muy rapidos (~ms por test)
- Testean logica de negocio aislada

**Ejemplo:**
```scala
class AuthServiceTest extends AnyWordSpec with Matchers with MockitoSugar {

  "AuthService" must {
    "reject user with invalid OTP" in {
      val userRepo = mock[UserRepository]
      val otpService = mock[OTPService]
      val cacheService = mock[CacheService]

      when(userRepo.bind("user", "pass")).thenReturn(Future.successful(true))
      when(userRepo.get("user")).thenReturn(Future.successful(UserFixtures.activeUser))
      when(otpService.validate(any, any)).thenReturn(false)  // OTP invalido

      val service = new AuthServiceImpl(cacheService, userRepo, otpService, ...)

      val result = Await.result(service.authenticate("user", "pass", "000000"), 5.seconds)
      result mustBe None
    }
  }
}
```

## Estrategia de Mocks y Stubs

Conviven dos mecanismos segun el tipo de test. No son intercambiables:

### Mockito → Unit tests (`test/unit/`)

Usar `mock[T]` con `when(...).thenReturn(...)` para dependencias **sin estado**
(repositories, servicios que devuelven valores fijos por llamada).

**Ventajas:** cada test configura su propio comportamiento, se puede `verify()`,
sin boilerplate de clases, no necesita Guice.

```scala
// Cada test define que devuelve el mock
val userRepo = mock[UserRepository]
when(userRepo.bind("user", "pass")).thenReturn(Future.successful(true))
when(userRepo.get("user")).thenReturn(Future.successful(fixtures.ldapUser))

val service = new AuthServiceImpl(mockCache, userRepo, mockOtp, ...)
```

### Stubs in-memory → Dependencias con estado

Usar implementaciones reales in-memory para servicios que tienen **logica interna
con estado** (ej: cache con get/set/pop que interactuan entre si).

Mockear estos servicios es fragil: hay que programar la secuencia de llamadas
a mano y se pierde el comportamiento integrado.

```scala
// StubCacheService tiene un HashMap real — set() y get() interactuan
class StubCacheService extends CacheService:
  private val store = scala.collection.mutable.HashMap.empty[String, Any]
  override def get[T](key: CacheKey[T]) = store.get(key.cacheKey).map(_.asInstanceOf[T])
  override def set[T](key: CacheKey[T], value: T) = store.put(key.cacheKey, value)
  // ...
```

Ubicar estos stubs en `test/fixtures/` junto con los datos de prueba.

### Guice overrides → Controller/integration tests

Para tests que levantan la app Play (`GuiceOneAppPerTest`), usar modulos de test
que reemplazan bindings completos. Aca Mockito no aplica porque la inyeccion
la maneja el container.

```scala
// TestSecurityModule reemplaza modulos de produccion
override def fakeApplication() = new GuiceApplicationBuilder()
  .disable[UsersModule]
  .overrides(bind[UserRepository].to[StubUserRepository])
  .build()
```

Ubicar estos modulos en `test/security/` (o el package del dominio correspondiente).

### Resumen rapido

| Situacion | Mecanismo | Ubicacion |
|-----------|-----------|-----------|
| Dependencia sin estado en unit test | `mock[T]` (Mockito) | inline en el test |
| Dependencia con estado (cache, stores) | Stub in-memory | `test/fixtures/` |
| Controller/integration test con Guice | Module override con stubs | `test/<dominio>/` |

## Integration Tests

Dos subcategorias:

### Controller/route tests (`test/integration/controllers/`)
- Levantan la app Play con `GuiceOneAppPerTest` y stubs (no Dockers)
- Verifican routing, status codes, content types
- Nombre termina en `Test.scala`
- Mas rapidos que infrastructure tests (~segundos)
- **Modulos multi-dominio:** Si se deshabilita un modulo Guice que contiene bindings
  de multiples dominios (ej: `StrKitModule` bindea `StrKitService` y `LocusService`),
  hay que proveer stubs para **todos** los servicios que el router necesita, no solo
  el que se esta testeando. El router instancia todos los controllers de `conf/routes`
  al arrancar, y falla si falta algun binding.

### Infrastructure tests (`test/integration/<dominio>/`)
- Usan infraestructura real (Dockers)
- Mas lentos (~segundos por test)
- Testean integracion con sistemas externos
- Requieren Dockers corriendo: `genis_postgres`, `genis_ldap`, `genis_mongo`

#### Principio: tests auto-contenidos

Los integration tests **no deben depender de datos pre-existentes** en la base
de datos. Si alguien modifica los datos seed, los tests se rompen sin que haya
un bug real. Cada test debe crear los datos que necesita y limpiarlos al terminar.

| Infraestructura | ¿Auto-contenido? | Estrategia |
|-----------------|-------------------|------------|
| **Postgres** | Si | INSERT en `beforeEach`, DELETE en `afterEach` con Slick directo |
| **MongoDB** | Si | Insert/drop con driver directo |
| **LDAP** | No (aceptable) | Usar datos seed conocidos (ej: usuario `setup`). Crear/borrar entradas LDAP requiere codigo complejo para pocos metodos read-only |

**Reglas para tests de Postgres auto-contenidos:**

1. **Cleanup defensivo en `beforeEach`**: limpiar ANTES de insertar, por si una
   ejecucion anterior aborto sin ejecutar `afterEach`.
2. **IDs de test con prefijo reconocible** (ej: `TEST_INTEGRATION`, `TEST_KIT_INT`)
   para no colisionar con datos seed y facilitar limpieza manual si hace falta.
3. **TODO para evolucion**: cuando el repositorio no tiene `add()`/`delete()`,
   usar Slick directo para setup/teardown y dejar un TODO para migrar cuando
   esos metodos esten implementados.

**Trait compartido para Postgres:**
```scala
// test/fixtures/PostgresSpec.scala
trait PostgresSpec extends BeforeAndAfterAll { self: Suite =>
  protected val db: Database = Database.forURL(
    url = "jdbc:postgresql://localhost:5432/genisdb",
    user = "genissqladmin",
    password = "genissqladminp",
    driver = "org.postgresql.Driver"
  )
  override protected def afterAll(): Unit =
    db.close()
    super.afterAll()
}
```

**Ejemplo Postgres (auto-contenido):**
```scala
class SlickCrimeTypeRepositoryIntegrationTest
    extends AnyWordSpec with Matchers with PostgresSpec with BeforeAndAfterEach:

  private val testId = "TEST_INTEGRATION"
  // TODO: usar repo.add()/delete() cuando esten implementados
  private def cleanTestData(): Unit =
    import slick.jdbc.PostgresProfile.api.*   // import local (ver seccion conflicto ===)
    Await.result(db.run(
      Tables.crimeTypes.filter(_.id === testId).delete
    ), timeout)

  override def beforeEach(): Unit =
    super.beforeEach()
    cleanTestData()                            // defensivo
    Await.result(db.run(
      Tables.crimeTypes += CrimeTypeRow(testId, "Test", None)
    ), timeout)

  override def afterEach(): Unit =
    cleanTestData()
    super.afterEach()
```

**Ejemplo LDAP (datos seed conocidos):**
```scala
class LdapUserRepositoryIntegrationTest
    extends AnyWordSpec with Matchers with BeforeAndAfterAll:

  // Conexion directa, sin Guice
  override def beforeAll(): Unit =
    searchConnection = new LDAPConnection("localhost", 1389)
    connectionPool = new LDAPConnectionPool(new LDAPConnection("localhost", 1389), 2)
    repo = new LdapUserRepository(connectionPool, searchConnection, usersDn)

  // Usa datos seed — usuario "setup" siempre existe en el Docker
  "bind" must {
    "return true for valid credentials" in {
      Await.result(repo.bind("setup", "pass"), timeout) mustBe true
    }
  }
```

## Fixtures (Datos de Prueba)

**Organizacion por dominio** (no un archivo monolitico):

```scala
// test/fixtures/UserFixtures.scala
object UserFixtures {
  val activeUser = LdapUser(
    userName = "testuser",
    firstName = "Test",
    lastName = "User",
    email = "test@example.com",
    roles = Seq("admin"),
    geneMapperId = "testuser",
    phone1 = "12345678",
    status = UserStatus.active,
    // ...
  )

  val blockedUser = activeUser.copy(status = UserStatus.blocked)
  val pendingUser = activeUser.copy(status = UserStatus.pending)
}

// test/fixtures/SecurityFixtures.scala
object SecurityFixtures {
  val validAuthPair = AuthenticatedPair(
    verifier = "d59b81ea658b20e4d3a1712b75bb21d5",
    key = "9e3ba370183beccf7df540ca812b3985",
    iv = "e932d6e8f6920f1efa4103226391a570"
  )

  val testTotpSecret = "ETZK6M66LFH3PHIG"
}
```

## Ejecucion de Tests

```bash
# Solo unit tests
sbt "project core" "testOnly *Test"

# Solo integration tests
sbt "project core" "testOnly *IntegrationTest"

# Todos los tests
sbt "project core" test
```

## Dockers Requeridos para Integration Tests

| Container | Puerto | Uso |
|-----------|--------|-----|
| genis_ldap | 1389 | Autenticacion, usuarios, roles |
| genis_postgres | 5432 | Base de datos principal |
| genis_mongo | 27017 | Perfiles, matches |

## Slick + ScalaTest: conflicto de `===`

ScalaTest `Matchers` define `===` como extension method en `Any`.
Slick define `===` como extension method en `Rep[T]`.

Cuando ambos estan en el mismo scope (import de archivo + herencia de `Matchers`),
**ScalaTest gana**: `Rep[String] === "literal"` se evalua como comparacion de objetos
Scala → `false` → Slick genera `where false` silenciosamente.

**Regla:** en cualquier test que extienda `Matchers` y construya queries Slick con `===`,
importar `slick.jdbc.PostgresProfile.api.*` **local al bloque** donde se construye el query.
El import local tiene mayor prioridad que los miembros heredados.

```scala
// MAL — import a nivel de archivo, Matchers.=== gana → where false
import slick.jdbc.PostgresProfile.api.*

class MyTest extends AnyWordSpec with Matchers with PostgresSpec:
  "repo" must {
    "find by id" in {
      db.run(MyTable.query.filter(_.id === "abc").result) // → where false!
    }
  }

// BIEN — import local al bloque, Slick.=== gana
class MyTest extends AnyWordSpec with Matchers with PostgresSpec:
  "repo" must {
    "find by id" in {
      import slick.jdbc.PostgresProfile.api.*
      db.run(MyTable.query.filter(_.id === "abc").result) // → where "ID" = 'abc'
    }
  }
```

Aplica a `beforeEach`, `afterEach`, helpers privados — cualquier metodo que
construya un query Slick con `===` dentro de una clase que extienda `Matchers`.

## Play 3 import gotchas

### `HandlerDef` vive en `play.api.routing`, no en `play.api.mvc`

En Play 3.0 el caso-clase `HandlerDef` (el que describe el controller/method
asociado a una ruta) esta en el paquete `play.api.routing`. El atributo
`Router.Attrs.HandlerDef` lo devuelve desde ese mismo paquete.

Intentar importarlo de `play.api.mvc` falla con un error que puede confundir:

```
[error] value HandlerDef is not a member of play.api.mvc - did you mean mvc.Handler?
```

En Play 2.x legacy el tipo estaba en un lugar distinto, por eso es un error
facil de cometer al migrar filtros.

**Mal:**
```scala
import play.api.mvc.{EssentialAction, HandlerDef, RequestHeader}  // no compila
```

**Bien:**
```scala
import play.api.mvc.{EssentialAction, RequestHeader}
import play.api.routing.{HandlerDef, Router}
```

**Uso tipico en un test de filtro:**
```scala
val handlerDef = HandlerDef(
  classLoader   = getClass.getClassLoader,
  routerPackage = "",
  controller    = "controllers.ProfilesController",
  method        = "findByCode",
  parameterTypes = Nil,
  verb          = "GET",
  path          = "/api/v2/profiles",
  comments      = "",
  modifiers     = Seq.empty
)
val req = FakeRequest(GET, "/api/v2/profiles")
  .addAttr(Router.Attrs.HandlerDef, handlerDef)
```

## Cleanup en integration tests: usar marcador del padre, no de la columna que varia

Cuando un test de Postgres escribe filas con valores **variados** en la columna
que normalmente usarias para limpiar (ej: varios `userId` distintos para testear
ordenamiento por `userId`), limpiar por esa columna deja filas huerfanas que
rompen el delete del padre por la FK.

**Sintoma:**
```
PSQLException: ERROR: update or delete on table "OPERATION_LOG_LOT"
  violates foreign key constraint "OPERATION_LOG_RECORD_FK" on table "OPERATION_LOG_RECORD".
  Detail: Key (ID)=(183) is still referenced from table "OPERATION_LOG_RECORD".
```

El test escribe records con `testUser + "_A"`, `testUser + "_B"`, etc. La
primera version del cleanup borraba solo `userId === testUser`, asi que los
records con sufijo quedaban y bloqueaban el delete del lote.

**Mal** — limpiar por la columna que el test varia:
```scala
private def cleanTestData(): Unit =
  import slick.jdbc.PostgresProfile.api.*
  val cleanup = DBIO.seq(
    Tables.OperationLogRecord.filter(_.userId === testUser).delete,
    Tables.OperationLogLot.filter(_.keyZero === markerKZero.asHexaString()).delete
  )
  Await.result(logDb.run(cleanup), timeout)
  // ^ aborta con FK violation si algun test escribio records con userIds distintos
```

**Bien** — limpiar hijos via el marcador del padre:
```scala
private val markerKZero = Key(Seq.fill(32)(0xAA.toByte))  // distintivo de este suite

private def cleanTestData(): Unit =
  import slick.jdbc.PostgresProfile.api.*
  val markerHex = markerKZero.asHexaString()
  val ourLotIds = Tables.OperationLogLot.filter(_.keyZero === markerHex).map(_.id)
  val cleanup = DBIO.seq(
    Tables.OperationLogRecord.filter(_.lot.in(ourLotIds)).delete,
    Tables.OperationLogLot.filter(_.keyZero === markerHex).delete
  )
  Await.result(logDb.run(cleanup), timeout)
```

**Regla general:** cuando el padre tiene una columna identificable (aca
`kZero`), elegi un valor marcador de test para esa columna y cascadea la
limpieza desde ahi. No confies en una columna del hijo que el test pueda
variar.

## Probar `.recover` con un throw sincronico

Cuando el codigo usa `.recover { case e => Left("error.XXXX") }`, escribir al
menos un test que dispare la excepcion por una via *sincronica* —
`opt.getOrElse(throw ...)`, `require(...)`, division por cero — y no solo por
un Future failed. El recover puede quedar mal ubicado (dentro del `flatMap` en
lugar de envolver el Future externo) y no atrapar el throw sincronico, dejando
escapar la excepcion como Future failed.

```scala
// MAL: el throw escapa, el recover solo aplica al for-comprehension interno
repo.get(id).flatMap { optX =>
  val x = optX.getOrElse(throw new RuntimeException("error.E0201"))
  (for ... yield ...).recover { case _ => Left("error.E0630") }
}

// BIEN: el recover envuelve TODO incluido el throw sincronico
repo.get(id).flatMap { optX =>
  val x = optX.getOrElse(throw new RuntimeException("error.E0201"))
  for ... yield ...
}.recover { case _ => Left("error.E0630") }
```

Test que detecta esta variante:

```scala
"recover atrapa throw sincronico (getOrElse) → Left("error.E0630")" in {
  val ped = pedigreeGenogram().copy(frequencyTable = None)   // dispara getOrElse(throw)
  when(repo.get(id)).thenReturn(Future.successful(Some(ped)))
  await(svc.method(id)) mustBe Left("error.E0630")
}
```

## Tests del helper `localize` en controllers

Cuando el controller usa un helper tipo `localize("error.EXXXX|arg1|arg2")`
para traducir errores via `messagesApi.preferred(...)`, los tests del endpoint
deben asertar dos cosas:

- **positivamente** que el mensaje traducido aparezca: `body must include("E0XXXX")`
- **negativamente** que la clave cruda NO aparezca: `body must not include "error.EXXXX"`

Sin la asercion negativa, si la clave se borra del `conf/messages` el test
sigue verde por casualidad — Play devuelve la clave verbatim cuando no la
encuentra, y `must include("E0XXXX")` matchea igual.

Para args con `MessageFormat`, asertar tambien la sustitucion:
`body must not include "{0}"`.

## Checklist para Nuevos Tests

### Unit Test
- [ ] Sin dependencias externas (DB, LDAP, HTTP)
- [ ] Mocks para todas las dependencias inyectadas
- [ ] Ubicado en `test/unit/<dominio>/`
- [ ] Nombre termina en `Test.scala`
- [ ] Usa fixtures del dominio correspondiente
- [ ] Si el codigo usa `.recover`, hay al menos un test que dispara excepcion sincronica para validar el alcance del recover

### Controller/Route Test
- [ ] Usa `GuiceOneAppPerTest` con stubs (no Dockers)
- [ ] Ubicado en `test/integration/controllers/`
- [ ] Nombre termina en `Test.scala`
- [ ] Deshabilita modulos que conectan a infraestructura real
- [ ] Si el controller traduce errores con `localize`, asertar positiva y negativamente (la clave cruda NO debe aparecer en el body)

### Infrastructure Integration Test
- [ ] Documenta que Dockers necesita
- [ ] Ubicado en `test/integration/<dominio>/`
- [ ] Nombre termina en `IntegrationTest.scala`
- [ ] Auto-contenido: crea sus datos en `beforeEach`, limpia en `afterEach`
- [ ] Cleanup defensivo en `beforeEach` (limpiar antes de insertar)
- [ ] IDs de test con prefijo reconocible (`TEST_*`)
- [ ] No depende de datos pre-existentes en la DB (excepto LDAP seed)
- [ ] Si usa Slick `===` dentro de clase con `Matchers`: import local de `slick api.*`
- [ ] TODO en setup/teardown si usa SQL directo por falta de metodos del repo

## Migracion desde Legacy

El archivo `test/stubs/Stubs.scala` del legacy contiene fixtures mezclados.
Al migrar tests, extraer los fixtures relevantes a archivos por dominio en `test/fixtures/`.
