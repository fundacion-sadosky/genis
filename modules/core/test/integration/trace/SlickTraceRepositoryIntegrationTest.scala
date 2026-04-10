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
