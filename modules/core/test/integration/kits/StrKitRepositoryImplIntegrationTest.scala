package integration.kits

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global

import kits.{StrKit, StrKitRepository}
import models.{StrKitRow, StrKitTable}
import fixtures.PostgresSpec

class StrKitRepositoryImplIntegrationTest
    extends AnyWordSpec with Matchers with PostgresSpec with BeforeAndAfterEach:

  private val timeout = 10.seconds

  // StrKitRepositoryImpl crea su propia DB via Database.forConfig.
  // Testeamos directamente con Slick queries + el mismo trait de tabla
  // para validar que la definición de tabla (schema APP, columnas) es correcta.
  // TODO: cuando StrKitRepository tenga add()/delete() implementados, usar esos métodos
  private val testKitId = "TEST_KIT_INT"
  private val testKit2Id = "TEST_KIT_INT2"

  override protected def beforeEach(): Unit =
    // NOTA: import local de slick api para que === de Slick tenga
    // prioridad sobre === de ScalaTest Matchers
    import slick.jdbc.PostgresProfile.api.*
    super.beforeEach()
    val cleanFirst = StrKitTable.query.filter(_.id === testKitId).delete andThen
                     StrKitTable.query.filter(_.id === testKit2Id).delete
    Await.result(db.run(cleanFirst), timeout)
    val setup = DBIO.seq(
      StrKitTable.query += StrKitRow(testKitId, "Test Kit Integration", 1, 16, 15),
      StrKitTable.query += StrKitRow(testKit2Id, "Test Kit Two", 2, 8, 7)
    )
    Await.result(db.run(setup), timeout)

  override protected def afterEach(): Unit =
    import slick.jdbc.PostgresProfile.api.*
    val cleanup = StrKitTable.query.filter(_.id === testKitId).delete andThen
                  StrKitTable.query.filter(_.id === testKit2Id).delete
    Await.result(db.run(cleanup), timeout)
    super.afterEach()

  "StrKitRepositoryImpl table definition" must {
    "list() returns kits from APP schema" in {
      import slick.jdbc.PostgresProfile.api.*
      val result = Await.result(
        db.run(StrKitTable.query.result),
        timeout
      )
      val testKits = result.filter(r => Set(testKitId, testKit2Id).contains(r.id))
      testKits must have size 2
    }

    "get() by ID returns matching kit" in {
      import slick.jdbc.PostgresProfile.api.*
      val result = Await.result(
        db.run(StrKitTable.query.filter(_.id === testKitId).result.headOption),
        timeout
      )
      result mustBe defined
      val kit = result.get
      kit.id mustBe testKitId
      kit.name mustBe "Test Kit Integration"
      kit.`type` mustBe 1
      kit.lociQty mustBe 16
      kit.representativeParameter mustBe 15
    }

    "get() with non-existent ID returns None" in {
      import slick.jdbc.PostgresProfile.api.*
      val result = Await.result(
        db.run(StrKitTable.query.filter(_.id === "NONEXISTENT_KIT_XYZ").result.headOption),
        timeout
      )
      result mustBe None
    }
  }
