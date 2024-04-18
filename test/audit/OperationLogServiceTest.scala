package audit

import java.util.Date

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import pdgconf.PdgGlobal
import play.api.Logger
import specs.PdgSpec
import stubs.Stubs

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, SECONDS}

class OperationLogServiceTest extends PdgSpec with MockitoSugar {

  val logger = Logger(this.getClass)
  val duration = Duration(10, SECONDS)

  "Operation log service" must {
    /* test aleatorio
    "log several entries and validate log lot" in {
      val logService = PdgGlobal.injector.getInstance(classOf[OperationLogService])
      val futures: Seq[Future[Unit]] = Seq()
      for {i <- 1 to 50} {
        val logEntry = OperationLogEntryAttemp("userid", None, new Date(0), "operation", "/" + i, "controller.Action()", "develop", Some("Ok"), 200)
        futures.+:(logService.add(logEntry))
      }

      Await.result(Future.sequence(futures), duration)

      val lotsQty = Await.result(logService.getLotsLength(), duration)

      val lastLot = Await.result(logService.listLotsView(lotsQty - 1, 1), duration)

      val result = Await.result(logService.checkLot(lastLot.head.id), duration)
      result mustBe Right(())

    }*/
  }

  "Operation log service" must {
    "get logs length"  in {

      val mockRepository = mock[OperationLogRepository]
      val mockResult = 5
      when(mockRepository.countLogs(any[OperationLogSearch])).thenReturn(Future.successful(mockResult))

      val logService = new PeoOperationLogService(mockRepository, null, null, mock[PEOSignerService], 0, 0)

      val result = Await.result(logService.getLogsLength(mock[OperationLogSearch]), duration)

      result mustBe mockResult

    }
  }

  "Operation log service" must {
    "search logs" in {

      val mockRepository = mock[OperationLogRepository]
      when(mockRepository.searchLogs(any[OperationLogSearch])).thenReturn(Future.successful(List(Stubs.signedOpLogRecord).toIndexedSeq))

      val logService = new PeoOperationLogService(mockRepository, null, null, mock[PEOSignerService], 0, 0)

      val result = Await.result(logService.searchLogs(mock[OperationLogSearch]), duration)

      result.size mustBe 1
      result.head.index mustBe Stubs.signedOpLogRecord.index

    }

  }

}
