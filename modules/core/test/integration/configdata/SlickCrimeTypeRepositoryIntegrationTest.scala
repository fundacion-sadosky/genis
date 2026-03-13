package integration.configdata

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global

import configdata.{SlickCrimeTypeRepository, CrimeType, Crime}
import models.Tables
import fixtures.PostgresSpec

class SlickCrimeTypeRepositoryIntegrationTest
    extends AnyWordSpec with Matchers with PostgresSpec with BeforeAndAfterEach:

  private val timeout = 10.seconds
  private lazy val repo = new SlickCrimeTypeRepository(db)

  // TODO: cuando CrimeTypeRepository tenga add()/delete(), usar esos métodos en vez de SQL directo
  private val testCrimeTypeId = "TEST_INTEGRATION"
  private val testCrimeId1 = "TEST_CRIME_1"
  private val testCrimeId2 = "TEST_CRIME_2"

  // NOTA: import local de slick api para que === de Slick tenga
  // prioridad sobre === de ScalaTest Matchers
  private def cleanTestData(): Unit =
    import slick.jdbc.PostgresProfile.api.*
    val cleanup = DBIO.seq(
      Tables.crimeInvolved.filter(_.crimeType === testCrimeTypeId).delete,
      Tables.crimeTypes.filter(_.id === testCrimeTypeId).delete
    )
    Await.result(db.run(cleanup), timeout)

  override protected def beforeEach(): Unit =
    import slick.jdbc.PostgresProfile.api.*
    super.beforeEach()
    cleanTestData()
    val setup = DBIO.seq(
      Tables.crimeTypes += models.CrimeTypeRow(testCrimeTypeId, "Test Crime Type", Some("For integration tests")),
      Tables.crimeInvolved += models.CrimeInvolvedRow(testCrimeId1, testCrimeTypeId, "Test Crime One", Some("First")),
      Tables.crimeInvolved += models.CrimeInvolvedRow(testCrimeId2, testCrimeTypeId, "Test Crime Two", None)
    )
    Await.result(db.run(setup), timeout)

  override protected def afterEach(): Unit =
    cleanTestData()
    super.afterEach()

  "SlickCrimeTypeRepository" must {
    "list() returns CrimeTypes with their associated Crimes via join" in {
      val result = Await.result(repo.list(), timeout)
      val testType = result.find(_.id == testCrimeTypeId)
      testType mustBe defined
      testType.get.name mustBe "Test Crime Type"
      testType.get.crimes must have size 2
    }

    "list() maps each CrimeType to its correct Crime children" in {
      val result = Await.result(repo.list(), timeout)
      val testType = result.find(_.id == testCrimeTypeId).get
      val crimeIds = testType.crimes.map(_.id).toSet
      crimeIds mustBe Set(testCrimeId1, testCrimeId2)
      testType.crimes.find(_.id == testCrimeId1).get.name mustBe "Test Crime One"
      testType.crimes.find(_.id == testCrimeId2).get.name mustBe "Test Crime Two"
    }

    "list() maps optional description fields correctly" in {
      val result = Await.result(repo.list(), timeout)
      val testType = result.find(_.id == testCrimeTypeId).get
      testType.description mustBe Some("For integration tests")
      testType.crimes.find(_.id == testCrimeId1).get.description mustBe Some("First")
      testType.crimes.find(_.id == testCrimeId2).get.description mustBe None
    }
  }
