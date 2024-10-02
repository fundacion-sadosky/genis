package trace

import java.util.Date

import models.Tables
import org.scalatest.mock.MockitoSugar
import play.api.db.slick._
import specs.PdgSpec
import types.{AlphanumericId, MongoId, SampleCode}

import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.db.slick.Config.driver.simple.{Column, Compiled, TableQuery, booleanColumnExtensionMethods, booleanColumnType, columnExtensionMethods, queryToInsertInvoker, runnableCompiledToAppliedQueryInvoker, slickDriver, stringColumnType}
import scala.language.postfixOps
import slick.driver.PostgresDriver.simple._

class TraceRepositoryTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)

  val traceInfo: TraceInfo = HitInfo("123456", SampleCode("AR-B-SHDG-1000"), "userId", 1)
  val trace: Trace = Trace(SampleCode("AR-B-SHDG-1234"), "userId", new Date(), traceInfo)

  val traces: TableQuery[Tables.Trace] = Tables.Trace

  private def queryDefineGetTrace(id: Column[Long]) = Compiled(for (
    t <- traces if t.id === id
  ) yield t)

  "A TraceRepository" must {
    "add a trace" in {
      val repository = new SlickTraceRepository()

      val result = Await.result(repository.add(trace), duration)

      result.isRight mustBe true
      result.right.get must not be 0

      DB.withSession { implicit session =>
        queryDefineGetTrace(result.right.get).delete
      }
    }

    def previousSettings(repository: SlickTraceRepository) = {
      val id1 = Await.result(repository.add(Trace(SampleCode("AR-B-SHDG-1"), "assignee", new Date(), ProfileDataInfo)), duration).right.get
      val id2 = Await.result(repository.add(Trace(SampleCode("AR-B-SHDG-1"), "pdg", new Date(), traceInfo)), duration).right.get
      val id3 = Await.result(repository.add(Trace(SampleCode("AR-B-SHDG-1"), "pdg", new Date(), traceInfo)), duration).right.get
      (id1, id2, id3)
    }

    def afterSettings(ids: (Long, Long, Long), repository: SlickTraceRepository) =  {
      DB.withSession { implicit session =>
        ids match {
          case (id1, id2, id3) => {
            queryDefineGetTrace(id1).delete
            queryDefineGetTrace(id2).delete
            queryDefineGetTrace(id3).delete
          }
        }
      }
    }

    "list all traces" in {
      val repository = new SlickTraceRepository()
      val ids = previousSettings(repository)

      val result = Await.result(repository.search(TraceSearch(0, 30, SampleCode("AR-B-SHDG-1"), "pdg", false)), duration)

      result.size mustBe 2

      afterSettings(ids, repository)
    }

    "count traces" in {
      val repository = new SlickTraceRepository()
      val ids = previousSettings(repository)

      val result = Await.result(repository.count(TraceSearch(0, 30, SampleCode("AR-B-SHDG-1"), "pdg", false)), duration)

      result mustBe 2

      afterSettings(ids, repository)
    }

    "filter traces by user" in {
      val repository = new SlickTraceRepository()
      val ids = previousSettings(repository)

      val assignee = Await.result(repository.search(TraceSearch(0, 30, SampleCode("AR-B-SHDG-1"), "assignee", false)), duration)
      val empty = Await.result(repository.search(TraceSearch(0, 30, SampleCode("AR-B-SHDG-1"), "another_user", false)), duration)

      assignee.size mustBe 1
      empty.size mustBe 0

      afterSettings(ids, repository)
    }

    "filter traces by superuser" in {
      val repository = new SlickTraceRepository()
      val ids = previousSettings(repository)

      val results = Await.result(repository.search(TraceSearch(0, 30, SampleCode("AR-B-SHDG-1"), "another_user", true)), duration)

      results.size mustBe 3

      afterSettings(ids, repository)
    }

    "filter traces by profile" in {
      val repository = new SlickTraceRepository()
      val ids = previousSettings(repository)

      val results = Await.result(repository.search(TraceSearch(0, 30, SampleCode("AR-B-SHDG-1"), "another_user", true)), duration)
      val noResults = Await.result(repository.search(TraceSearch(0, 30, SampleCode("AR-B-SHDG-10"), "another_user", true)), duration)

      results.size mustBe 3
      noResults.size mustBe 0

      afterSettings(ids, repository)
    }

    "paginate traces" in {
      val repository = new SlickTraceRepository()
      val ids = previousSettings(repository)

      val results = Await.result(repository.search(TraceSearch(0, 2, SampleCode("AR-B-SHDG-1"), "userId", true)), duration)

      results.size mustBe 2

      afterSettings(ids, repository)
    }

  }


}