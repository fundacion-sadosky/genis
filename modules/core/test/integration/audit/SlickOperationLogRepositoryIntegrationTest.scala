package integration.audit

import java.sql.Timestamp
import java.util.Date

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.*

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import audit.*
import fixtures.LogDbSpec
import models.Tables
import types.TotpToken

/** Integration test for [[SlickOperationLogRepository]].
 *  Requires a running Postgres on localhost:5432 with the LOG_DB schema
 *  applied to database `genislogdb`
 *  (see `conf/evolutions/logDb/1.sql`). */
class SlickOperationLogRepositoryIntegrationTest
    extends AnyWordSpec with Matchers with LogDbSpec with BeforeAndAfterEach:

  given ec: ExecutionContext = ExecutionContext.global
  private val timeout = 10.seconds

  private lazy val repo = new SlickOperationLogRepository(logDb)

  private val testUser      = "TEST_AUDIT_USER"
  private val testOperation = "Ver perfil"

  // A random-looking kZero that we recognise and clean up.
  private val markerKZero = Key(Seq.fill(32)(0xAA.toByte))

  // Delete all records in lots we created (by marker kZero), then the lots themselves.
  private def cleanTestData(): Unit =
    import slick.jdbc.PostgresProfile.api.*
    val markerHex = markerKZero.asHexaString()
    val ourLotIds = Tables.OperationLogLot.filter(_.keyZero === markerHex).map(_.id)
    val cleanup = DBIO.seq(
      Tables.OperationLogRecord.filter(_.lot.in(ourLotIds)).delete,
      Tables.OperationLogLot.filter(_.keyZero === markerHex).delete
    )
    Await.result(logDb.run(cleanup), timeout)

  override protected def beforeEach(): Unit =
    super.beforeEach()
    cleanTestData()

  override protected def afterEach(): Unit =
    cleanTestData()
    super.afterEach()

  private def newLot(): Long =
    Await.result(repo.createLot(markerKZero), timeout)

  private def makeEntry(
      userId: String = testUser,
      lotId: Long,
      status: Int = 200,
      description: String = testOperation,
      timestamp: Date = new Date(),
      signature: Key = Key(Seq.fill(32)(0.toByte)),
      otp: Option[TotpToken] = None
  ): SignedOperationLogEntry =
    SignedOperationLogEntry(
      index       = 0L,
      userId      = userId,
      otp         = otp,
      timestamp   = timestamp,
      method      = "GET",
      path        = "/api/v2/profiles",
      action      = "controllers.ProfilesController.findByCode()",
      buildNo     = "test-build",
      result      = None,
      status      = status,
      lotId       = lotId,
      signature   = signature,
      description = description
    )

  "SlickOperationLogRepository" must {

    "createLot returns an auto-generated id" in {
      val id1 = newLot()
      val id2 = newLot()
      id2 must be > id1
    }

    "getLot returns Some for an existing lot and None for a missing id" in {
      val id = newLot()

      val got = Await.result(repo.getLot(id), timeout)
      got.map(_.id) mustBe Some(id)
      got.map(_.kZero.asHexaString()) mustBe Some(markerKZero.asHexaString())

      Await.result(repo.getLot(-1L), timeout) mustBe None
    }

    "listLots returns the created lot mapped to domain OperationLogLot" in {
      val id = newLot()

      // Grab a page large enough to include our lot while tolerating pre-existing data.
      val lots = Await.result(repo.listLots(limit = 500, offset = 0), timeout)
      val ours = lots.find(_.id == id)
      ours mustBe defined
      ours.get.kZero.asHexaString() mustBe markerKZero.asHexaString()
    }

    "countLots returns the total number of lots" in {
      val before = Await.result(repo.countLots(), timeout)
      newLot()
      newLot()
      val after  = Await.result(repo.countLots(), timeout)
      after - before mustBe 2
    }

    "add persists an entry so countLogs sees it" in {
      val id = newLot()
      Await.result(repo.add(makeEntry(lotId = id)), timeout)
      val qty = Await.result(repo.countLogs(OperationLogSearch(lotId = id)), timeout)
      qty mustBe 1
    }

    "countLogs filters by user" in {
      val id = newLot()
      Await.result(repo.add(makeEntry(lotId = id)), timeout)

      val hit  = Await.result(repo.countLogs(OperationLogSearch(id, user = Some(testUser))), timeout)
      val miss = Await.result(repo.countLogs(OperationLogSearch(id, user = Some("NO_SUCH"))), timeout)
      hit must be > 0
      miss mustBe 0
    }

    "countLogs filters by operations (description inSet)" in {
      val id = newLot()
      Await.result(repo.add(makeEntry(lotId = id)), timeout)

      val hit  = Await.result(repo.countLogs(OperationLogSearch(id, operations = Some(List(testOperation)))), timeout)
      val miss = Await.result(repo.countLogs(OperationLogSearch(id, operations = Some(List("fakeOperation")))), timeout)
      hit must be > 0
      miss mustBe 0
    }

    "countLogs filters by status (true => 200 only, false => non-200)" in {
      val id = newLot()
      Await.result(repo.add(makeEntry(lotId = id, status = 200)), timeout)
      Await.result(repo.add(makeEntry(lotId = id, status = 500)), timeout)

      val only200    = Await.result(repo.countLogs(OperationLogSearch(id, result = Some(true))), timeout)
      val nonTwo00   = Await.result(repo.countLogs(OperationLogSearch(id, result = Some(false))), timeout)
      only200  mustBe 1
      nonTwo00 mustBe 1
    }

    "countLogs filters by hourFrom (inclusive lower bound)" in {
      val id  = newLot()
      val now = new Date()
      Await.result(repo.add(makeEntry(lotId = id, timestamp = now)), timeout)

      val yesterday = new Date(now.getTime - 24L * 3600 * 1000)
      val tomorrow  = new Date(now.getTime + 24L * 3600 * 1000)
      val hit  = Await.result(repo.countLogs(OperationLogSearch(id, hourFrom = Some(yesterday))), timeout)
      val miss = Await.result(repo.countLogs(OperationLogSearch(id, hourFrom = Some(tomorrow))),  timeout)
      hit must be > 0
      miss mustBe 0
    }

    "countLogs filters by hourUntil (inclusive upper bound)" in {
      val id  = newLot()
      val now = new Date()
      Await.result(repo.add(makeEntry(lotId = id, timestamp = now)), timeout)

      val yesterday = new Date(now.getTime - 24L * 3600 * 1000)
      val tomorrow  = new Date(now.getTime + 24L * 3600 * 1000)
      val hit  = Await.result(repo.countLogs(OperationLogSearch(id, hourUntil = Some(tomorrow))),  timeout)
      val miss = Await.result(repo.countLogs(OperationLogSearch(id, hourUntil = Some(yesterday))), timeout)
      hit must be > 0
      miss mustBe 0
    }

    "searchLogs returns rows matching the filter with hex signature round-tripped back into Key" in {
      val id  = newLot()
      val sig = Key(Seq.fill(32)(0x42.toByte))
      Await.result(repo.add(makeEntry(lotId = id, signature = sig)), timeout)

      val result = Await.result(
        repo.searchLogs(OperationLogSearch(id, user = Some(testUser))),
        timeout
      )
      result must have size 1
      result.head.userId   mustBe testUser
      result.head.signature mustBe sig
    }

    "searchLogs applies pagination" in {
      val id = newLot()
      (1 to 10).foreach(_ => Await.result(repo.add(makeEntry(lotId = id)), timeout))

      val page = Await.result(
        repo.searchLogs(OperationLogSearch(id, page = 0, pageSize = 5)),
        timeout
      )
      page must have size 5
    }

    "searchLogs sorts by id ascending when ascending=true, sortField=\"id\"" in {
      val id = newLot()
      (1 to 3).foreach(_ => Await.result(repo.add(makeEntry(lotId = id)), timeout))

      val rows = Await.result(
        repo.searchLogs(OperationLogSearch(id, ascending = Some(true), sortField = Some("id"))),
        timeout
      )
      rows.map(_.index).toList mustBe rows.map(_.index).sorted.toList
    }

    "searchLogs sorts by id descending by default" in {
      val id = newLot()
      (1 to 3).foreach(_ => Await.result(repo.add(makeEntry(lotId = id)), timeout))

      val rows = Await.result(
        repo.searchLogs(OperationLogSearch(id)),
        timeout
      )
      rows.map(_.index).toList mustBe rows.map(_.index).sorted.reverse.toList
    }

    "searchLogs sorts by timestamp" in {
      val id = newLot()
      val base = new Date()
      Await.result(repo.add(makeEntry(lotId = id, timestamp = new Date(base.getTime + 1000))), timeout)
      Await.result(repo.add(makeEntry(lotId = id, timestamp = new Date(base.getTime + 3000))), timeout)
      Await.result(repo.add(makeEntry(lotId = id, timestamp = new Date(base.getTime + 2000))), timeout)

      val asc = Await.result(
        repo.searchLogs(OperationLogSearch(id, ascending = Some(true), sortField = Some("timestamp"))),
        timeout
      )
      asc.map(_.timestamp.getTime).toList mustBe asc.map(_.timestamp.getTime).sorted.toList
    }

    "searchLogs sorts by userId" in {
      val id = newLot()
      Await.result(repo.add(makeEntry(userId = testUser + "_C", lotId = id)), timeout)
      Await.result(repo.add(makeEntry(userId = testUser + "_A", lotId = id)), timeout)
      Await.result(repo.add(makeEntry(userId = testUser + "_B", lotId = id)), timeout)

      val asc = Await.result(
        repo.searchLogs(OperationLogSearch(id, ascending = Some(true), sortField = Some("userId"))),
        timeout
      )
      asc.map(_.userId).toList mustBe asc.map(_.userId).sorted.toList
    }

    "searchLogs preserves otp Option mapping when set" in {
      val id = newLot()
      Await.result(repo.add(makeEntry(lotId = id, otp = Some(TotpToken("987654")))), timeout)

      val rows = Await.result(repo.searchLogs(OperationLogSearch(id, user = Some(testUser))), timeout)
      rows.head.otp mustBe Some(TotpToken("987654"))
    }
  }
