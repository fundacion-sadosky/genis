package integration.trace

// Requiere Docker: postgres en localhost:5432
// Iniciar con: cd utils/docker && docker-compose up -d

import fixtures.PostgresSpec
import models.Tables
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import trace.*
import types.SampleCode

import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class SlickTraceRepositoryIntegrationTest
    extends AnyWordSpec with Matchers with PostgresSpec with BeforeAndAfterEach:

  private val timeout = 10.seconds
  private lazy val repo = new SlickTraceRepository(db)

  private val testProfile1 = SampleCode("AR-B-TEST-1")
  private val testProfile2 = SampleCode("AR-B-TEST-2")

  private val traceInfo = trace.ProfileDataInfo

  private def cleanTestData(): Unit =
    import slick.jdbc.PostgresProfile.api.*
    val cleanup = DBIO.seq(
      Tables.Trace.filter(_.profile inSet Set(testProfile1.text, testProfile2.text)).delete,
      Tables.TracePedigree.filter(_.pedigree === 9999L).delete
    )
    Await.result(db.run(cleanup), timeout)

  override protected def beforeEach(): Unit =
    super.beforeEach()
    cleanTestData()

  override protected def afterEach(): Unit =
    cleanTestData()
    super.afterEach()

  private def addTrace(profile: SampleCode, user: String): Long =
    val t = Trace(profile, user, new Date(), traceInfo)
    Await.result(repo.add(t), timeout).toOption.get

  "SlickTraceRepository.add" must:
    "insert a trace and return Right with non-zero id" in:
      val t      = Trace(testProfile1, "user1", new Date(), traceInfo)
      val result = Await.result(repo.add(t), timeout)
      result.isRight mustBe true
      result.toOption.get must be > 0L

  "SlickTraceRepository.search" must:
    "return only traces matching the given user" in:
      addTrace(testProfile1, "user1")
      addTrace(testProfile1, "user2")
      addTrace(testProfile1, "user2")

      val result = Await.result(
        repo.search(TraceSearch(0, 30, testProfile1, "user1", isSuperUser = false)),
        timeout
      )
      result.size mustBe 1

    "return empty sequence when user has no traces for the profile" in:
      addTrace(testProfile1, "user1")

      val result = Await.result(
        repo.search(TraceSearch(0, 30, testProfile1, "other_user", isSuperUser = false)),
        timeout
      )
      result.size mustBe 0

    "return all traces when isSuperUser is true regardless of user filter" in:
      addTrace(testProfile1, "user1")
      addTrace(testProfile1, "user2")
      addTrace(testProfile1, "user2")

      val result = Await.result(
        repo.search(TraceSearch(0, 30, testProfile1, "other_user", isSuperUser = true)),
        timeout
      )
      result.size mustBe 3

    "return empty sequence when profile has no matching traces" in:
      addTrace(testProfile1, "user1")

      val result = Await.result(
        repo.search(TraceSearch(0, 30, testProfile2, "other_user", isSuperUser = true)),
        timeout
      )
      result.size mustBe 0

    "respect pagination — pageSize limits the number of results" in:
      addTrace(testProfile1, "user1")
      addTrace(testProfile1, "user1")
      addTrace(testProfile1, "user1")

      val result = Await.result(
        repo.search(TraceSearch(0, 2, testProfile1, "user1", isSuperUser = false)),
        timeout
      )
      result.size mustBe 2

  "SlickTraceRepository.count" must:
    "count only traces matching the given user and profile" in:
      addTrace(testProfile1, "user1")
      addTrace(testProfile1, "user1")
      addTrace(testProfile1, "user2")

      val result = Await.result(
        repo.count(TraceSearch(0, 30, testProfile1, "user1", isSuperUser = false)),
        timeout
      )
      result mustBe 2

    "count all traces when isSuperUser is true" in:
      addTrace(testProfile1, "user1")
      addTrace(testProfile1, "user2")
      addTrace(testProfile1, "user3")

      val result = Await.result(
        repo.count(TraceSearch(0, 30, testProfile1, "other_user", isSuperUser = true)),
        timeout
      )
      result mustBe 3

  "SlickTraceRepository.getById" must:
    "return Some(trace) for an existing id" in:
      val id = addTrace(testProfile1, "user1")
      val result = Await.result(repo.getById(id), timeout)
      result.isDefined mustBe true
      result.get.profile mustBe testProfile1
      result.get.user mustBe "user1"

    "return None for a non-existing id" in:
      val result = Await.result(repo.getById(-999L), timeout)
      result mustBe None

  "SlickTraceRepository.addTracePedigree" must:
    "insert a pedigree trace and return Right with non-zero id" in:
      val t      = TracePedigree(9999L, "user1", new Date(), traceInfo)
      val result = Await.result(repo.addTracePedigree(t), timeout)
      result.isRight mustBe true
      result.toOption.get must be > 0L

  "SlickTraceRepository.searchPedigree" must:
    "return only pedigree traces matching the given user" in:
      Await.result(repo.addTracePedigree(TracePedigree(9999L, "user1", new Date(), traceInfo)), timeout)
      Await.result(repo.addTracePedigree(TracePedigree(9999L, "user2", new Date(), traceInfo)), timeout)

      val result = Await.result(
        repo.searchPedigree(TraceSearchPedigree(0, 30, 9999, "user1", isSuperUser = false)),
        timeout
      )
      result.size mustBe 1
      result.head.user mustBe "user1"

    "return all pedigree traces when isSuperUser is true" in:
      Await.result(repo.addTracePedigree(TracePedigree(9999L, "user1", new Date(), traceInfo)), timeout)
      Await.result(repo.addTracePedigree(TracePedigree(9999L, "user2", new Date(), traceInfo)), timeout)

      val result = Await.result(
        repo.searchPedigree(TraceSearchPedigree(0, 30, 9999, "other_user", isSuperUser = true)),
        timeout
      )
      result.size mustBe 2

  "SlickTraceRepository.countPedigree" must:
    "count only pedigree traces matching the given user" in:
      Await.result(repo.addTracePedigree(TracePedigree(9999L, "user1", new Date(), traceInfo)), timeout)
      Await.result(repo.addTracePedigree(TracePedigree(9999L, "user1", new Date(), traceInfo)), timeout)
      Await.result(repo.addTracePedigree(TracePedigree(9999L, "user2", new Date(), traceInfo)), timeout)

      val result = Await.result(
        repo.countPedigree(TraceSearchPedigree(0, 30, 9999, "user1", isSuperUser = false)),
        timeout
      )
      result mustBe 2
