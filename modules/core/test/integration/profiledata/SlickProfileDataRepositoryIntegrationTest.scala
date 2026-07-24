package integration.profiledata

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.*

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.Helpers.stubMessagesApi
import slick.jdbc.PostgresProfile.api.*

import fixtures.PostgresSpec
import profiledata.SlickProfileDataRepository
import types.SampleCode

/** Integration test for [[SlickProfileDataRepository.getGlobalCode]].
 *
 *  Guards the port of legacy 5.1.14 item D (issue #302): a `GLOBAL_CODE`
 *  read from the DB that does NOT match the [[SampleCode]] format must be
 *  swallowed (`Try(SampleCode(_)).toOption`) instead of throwing
 *  `IllegalArgumentException` and aborting the lookup.
 *
 *  Requires a running Postgres on localhost:5432, database `genisdb`, with the
 *  APP schema applied (`cd utils/docker && docker-compose up -d`). */
class SlickProfileDataRepositoryIntegrationTest
    extends AnyWordSpec with Matchers with PostgresSpec with BeforeAndAfterEach:

  given ec: ExecutionContext = ExecutionContext.global
  private val timeout = 10.seconds

  private lazy val repo = new SlickProfileDataRepository(db, stubMessagesApi())

  // Recognisable, self-contained test data (cleaned up defensively).
  private val grpId          = "TEST_GRP_302"
  private val catId          = "TEST_CAT_302"
  private val validGc        = "AR-A-BUE-123"
  private val malformedGc     = "CODIGO_MALFORMADO_302"
  private val validSample    = "TEST_ISC_VALID_302"
  private val malformedSample = "TEST_ISC_BAD_302"

  private def cleanTestData(): Unit =
    val cleanup = DBIO.seq(
      sqlu"""DELETE FROM "APP"."PROFILE_DATA" WHERE "CATEGORY" = $catId""",
      sqlu"""DELETE FROM "APP"."CATEGORY" WHERE "ID" = $catId""",
      sqlu"""DELETE FROM "APP"."GROUP" WHERE "ID" = $grpId"""
    )
    Await.result(db.run(cleanup.transactionally), timeout)

  private def seed(): Unit =
    val setup = DBIO.seq(
      sqlu"""INSERT INTO "APP"."GROUP" ("ID","NAME") VALUES ($grpId, 'Test Group 302')""",
      sqlu"""INSERT INTO "APP"."CATEGORY"
               ("ID","GROUP","NAME","FILIATION_DATA","REPLICATE","PEDIGREE_ASSOCIATION","IS_REFERENCE","TYPE")
             VALUES ($catId, $grpId, 'Test Cat 302', false, false, false, false, 1)""",
      sqlu"""INSERT INTO "APP"."PROFILE_DATA"
               ("ID","CATEGORY","GLOBAL_CODE","INTERNAL_CODE","INTERNAL_SAMPLE_CODE","ASSIGNEE","LABORATORY","DELETED","FROM_DESKTOP_SEARCH")
             VALUES (930201, $catId, $validGc, 'TEST_IC_VALID_302', $validSample, 'TEST_ASSIGNEE', 'TEST_LAB_302', false, false)""",
      sqlu"""INSERT INTO "APP"."PROFILE_DATA"
               ("ID","CATEGORY","GLOBAL_CODE","INTERNAL_CODE","INTERNAL_SAMPLE_CODE","ASSIGNEE","LABORATORY","DELETED","FROM_DESKTOP_SEARCH")
             VALUES (930202, $catId, $malformedGc, 'TEST_IC_BAD_302', $malformedSample, 'TEST_ASSIGNEE', 'TEST_LAB_302', false, false)"""
    )
    Await.result(db.run(setup.transactionally), timeout)

  override protected def beforeEach(): Unit =
    super.beforeEach()
    cleanTestData()
    seed()

  override protected def afterEach(): Unit =
    cleanTestData()
    super.afterEach()

  "SlickProfileDataRepository.getGlobalCode" must {

    "return Some(SampleCode) for a well-formed GLOBAL_CODE" in {
      Await.result(repo.getGlobalCode(validSample), timeout) mustBe Some(SampleCode(validGc))
    }

    "return None (not throw) when the stored GLOBAL_CODE is malformed" in {
      Await.result(repo.getGlobalCode(malformedSample), timeout) mustBe None
    }
  }
