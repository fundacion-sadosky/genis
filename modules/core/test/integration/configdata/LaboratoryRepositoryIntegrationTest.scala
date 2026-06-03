package configdata

import scala.concurrent.Await
import scala.concurrent.duration.*
import scala.util.Try

import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.*
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

import types.Laboratory

// Integration test — requiere genis_postgres alcanzable en la URL de slick.dbs.default
// (jdbc:postgresql://localhost:5432/genisdb). Si la base no es alcanzable, los tests se
// cancelan (assume) en lugar de fallar, para no romper `core/test` en entornos sin esa DB.
//
// Ejercita el path real de C1 que los unit tests no cubren: inserción transaccional y el
// mapeo de la violación de unicidad (PSQLState 23505, constraint LAB_PK) a un mensaje en
// español, sin filtrar e.getMessage al cliente.
//
// Nota: COUNTRY es varchar(2) y PROVINCE varchar(1) en la base real; los datos de prueba
// respetan esos largos.
class LaboratoryRepositoryIntegrationTest extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterAll {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()

  private lazy val repo = app.injector.instanceOf[LaboratoryRepository]
  private lazy val db   = app.injector.instanceOf[slick.jdbc.JdbcBackend.Database]

  private val testCode = "TEST_INT_LAB"

  private def lab(code: String): Laboratory =
    Laboratory("Lab Integración", code, "AR", "B", "Calle 1", "123", "lab@test.com", 0.1, 0.2)

  private lazy val dbUp: Boolean = {
    import slick.jdbc.PostgresProfile.api.*
    Try(Await.result(db.run(sql"select 1".as[Int]), 3.seconds)).isSuccess
  }

  // Limpieza defensiva por el código de test (no por columnas variables).
  private def cleanup(): Unit = {
    import slick.jdbc.PostgresProfile.api.*
    import models.Tables.laboratories
    Await.result(db.run(laboratories.filter(_.codeName === testCode).delete), 10.seconds)
  }

  override def beforeAll(): Unit = if (dbUp) cleanup()
  override def afterAll(): Unit = if (dbUp) cleanup()

  "LaboratoryService wiring (C2)" should {

    // No requiere DB: valida que @Named(\"labCode\") resuelve a través de Guice (configdata.Module),
    // construyendo LaboratoryServiceImpl. El unit test pasa el labCode literal y no cubre el binding.
    "construir LaboratoryServiceImpl con @Named(labCode) resuelto" in {
      app.injector.instanceOf[services.LaboratoryService] must not be null
    }
  }

  "SlickLaboratoryRepository (contra genis_postgres real)" should {

    "add devuelve Right(code) y persiste el laboratorio (C1)" in {
      assume(dbUp, "genis_postgres no alcanzable en este entorno")
      Await.result(repo.add(lab(testCode)), 10.seconds) mustBe Right(testCode)
      Await.result(repo.get(testCode), 10.seconds).map(_.code) mustBe Some(testCode)
    }

    "add de un código duplicado devuelve Left con mensaje en español (C1, 23505)" in {
      assume(dbUp, "genis_postgres no alcanzable en este entorno")
      Await.result(repo.add(lab(testCode)), 10.seconds) mustBe Left("Código de laboratorio ya utilizado")
    }
  }
}
