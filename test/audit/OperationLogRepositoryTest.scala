package audit

import java.util.Date

import org.apache.commons.lang.time.DateUtils
import specs.PdgSpec
import stubs.Stubs

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

class OperationLogRepositoryTest extends PdgSpec {

  val duration = Duration(10, SECONDS)

  "An OperationLogRepository" must {
    "create a lot and return the id" in {
      val opLogRepo = new SlickOperationLogRepository
      val kZero = new Key(Vector(12, 23, 44))

      for (i <- 1 to 10) {
        val id = Await.result(opLogRepo.createLot(kZero), duration)
        id mustBe i
      }
    }
  }

  "An OperationLogRepository" must {
    "list the lots" in {
      val opLogRepo = new SlickOperationLogRepository

      val lots = Await.result(opLogRepo.listLots(1, 0), duration)
      lots.size mustBe 1
    }
  }

  "An OperationLogRepository" must {
    "add an operation to the log lot" in {
      val opLogRepo = new SlickOperationLogRepository

      Await.result(opLogRepo.createLot(new Key(Vector(12, 23, 44))), duration)
      Await.result(opLogRepo.add(Stubs.signedOpLogRecord), duration)

      val logsQty = Await.result(opLogRepo.countLogs(OperationLogSearch(Stubs.signedOpLogRecord.lotId, 0, 30)), duration)

      logsQty mustBe >(0)
    }
  }

  def previousSettings(opLogRepo: OperationLogRepository) = {
    // Create log lot
    Await.result(opLogRepo.createLot(new Key(Vector(12, 23, 44))), duration)
    // Create log in lot 1
    Await.result(opLogRepo.add(Stubs.signedOpLogRecord), duration)
  }

  "An OperationLogRepository" must {
    "count all logs in a lot" in {
      val opLogRepo = new SlickOperationLogRepository
      previousSettings(opLogRepo)

      val logsQty = Await.result(opLogRepo.countLogs(OperationLogSearch(Stubs.signedOpLogRecord.lotId, 0, 30)), duration)

      logsQty mustBe >(0)
    }
  }

  "An OperationLogRepository" must {
    "filter by user" in {
      val opLogRepo = new SlickOperationLogRepository
      previousSettings(opLogRepo)

      val filtersWithResults = OperationLogSearch(Stubs.signedOpLogRecord.lotId, 0, 30, Some(Stubs.signedOpLogRecord.userId))
      val logsQtyWithResults = Await.result(opLogRepo.countLogs(filtersWithResults), duration)

      val filtersWithoutResults = OperationLogSearch(Stubs.signedOpLogRecord.lotId, 0, 30, Some("fakeUser"))
      val logsQtyWithoutResults = Await.result(opLogRepo.countLogs(filtersWithoutResults), duration)

      logsQtyWithResults mustBe >(0)
      logsQtyWithoutResults mustBe 0
    }
    "filter by operations" in {
      val opLogRepo = new SlickOperationLogRepository
      previousSettings(opLogRepo)

      val filtersWithResults = OperationLogSearch(Stubs.signedOpLogRecord.lotId, 0, 30, None, Some(List(Stubs.signedOpLogRecord.description)))
      val logsQtyWithResults = Await.result(opLogRepo.countLogs(filtersWithResults), duration)

      val filtersWithoutResults = OperationLogSearch(Stubs.signedOpLogRecord.lotId, 0, 30, None, Some(List("fakeOperation")))
      val logsQtyWithoutResults = Await.result(opLogRepo.countLogs(filtersWithoutResults), duration)

      logsQtyWithResults mustBe >(0)
      logsQtyWithoutResults mustBe 0
    }
    "filter by status" in {
      val opLogRepo = new SlickOperationLogRepository
      previousSettings(opLogRepo)

      val filtersWithResults = OperationLogSearch(Stubs.signedOpLogRecord.lotId, 0, 30, None, None, None, None, Some(true))
      val logsQtyWithResults = Await.result(opLogRepo.countLogs(filtersWithResults), duration)

      val filtersWithoutResults = OperationLogSearch(Stubs.signedOpLogRecord.lotId, 0, 30, None, None, None, None, Some(false))
      val logsQtyWithoutResults = Await.result(opLogRepo.countLogs(filtersWithoutResults), duration)

      logsQtyWithResults mustBe >(0)
      logsQtyWithoutResults mustBe 0
    }
    "filter by starting date" in {
      val opLogRepo = new SlickOperationLogRepository
      previousSettings(opLogRepo)

      val filtersWithResults = OperationLogSearch(Stubs.signedOpLogRecord.lotId, 0, 30, None, None, Some(DateUtils.addDays(new Date(), -1)))
      val logsQtyWithResults = Await.result(opLogRepo.countLogs(filtersWithResults), duration)

      val filtersWithoutResults = OperationLogSearch(Stubs.signedOpLogRecord.lotId, 0, 30, None, None, Some(DateUtils.addDays(new Date(), 1)))
      val logsQtyWithoutResults = Await.result(opLogRepo.countLogs(filtersWithoutResults), duration)

      logsQtyWithResults mustBe >(0)
      logsQtyWithoutResults mustBe 0
    }
    "filter by ending date" in {
      val opLogRepo = new SlickOperationLogRepository
      previousSettings(opLogRepo)

      val filtersWithResults = OperationLogSearch(Stubs.signedOpLogRecord.lotId, 0, 30, None, None, None, Some(DateUtils.addDays(new Date(), 1)))
      val logsQtyWithResults = Await.result(opLogRepo.countLogs(filtersWithResults), duration)

      val filtersWithoutResults = OperationLogSearch(Stubs.signedOpLogRecord.lotId, 0, 30, None, None, None, Some(DateUtils.addDays(new Date(), -1)))
      val logsQtyWithoutResults = Await.result(opLogRepo.countLogs(filtersWithoutResults), duration)

      logsQtyWithResults mustBe >(0)
      logsQtyWithoutResults mustBe 0
    }
  }

  "An OperationLogRepository" must {
    "search logs by filters" in {
      val opLogRepo = new SlickOperationLogRepository
      previousSettings(opLogRepo)

      val filters = OperationLogSearch(Stubs.signedOpLogRecord.lotId, 0, 30, Some(Stubs.signedOpLogRecord.userId), Some(List(Stubs.signedOpLogRecord.description)))
      val result = Await.result(opLogRepo.searchLogs(filters), duration)

      result.size mustBe >(0)

    }
  }

  "An OperationLogRepository" must {
    "use pagination to return logs" in {
      val opLogRepo = new SlickOperationLogRepository
      previousSettings(opLogRepo)

      (10 to 20).foreach(id => {
        val signedOpLogRecord = new SignedOperationLogEntry(id, "user1", None, new Date(), "GET", "/some", "controller.Action()", "developbuild", Some("Ok"), 200, 1L, Key("03d98620-eacc4bbd"), "ObtenerAlgo")
        Await.result(opLogRepo.add(signedOpLogRecord), duration)
      })

      val filters = OperationLogSearch(Stubs.signedOpLogRecord.lotId, 0, 5)
      val result = Await.result(opLogRepo.searchLogs(filters), duration)

      result.size mustBe 5
    }
  }

  "An OperationLogRepository" must {
    "sort results by sort field and ascending value" in {
      val opLogRepo = new SlickOperationLogRepository
      previousSettings(opLogRepo)

      Await.result(opLogRepo.add(new SignedOperationLogEntry(50, "user1", None, new Date(), "GET", "/some", "controller.Action()", "developbuild", Some("Ok"), 200, 1L, Key("03d98620-eacc4bbd"), "ObtenerAlgo")), duration)
      Await.result(opLogRepo.add(new SignedOperationLogEntry(55, "user1", None, new Date(), "GET", "/some", "controller.Action()", "developbuild", Some("Ok"), 200, 1L, Key("03d98620-eacc4bbd"), "ObtenerAlgo")), duration)

      val filters = OperationLogSearch(Stubs.signedOpLogRecord.lotId, 0, 30, None, None, None, None, None, Some(true), Some("id"))
      val result = Await.result(opLogRepo.searchLogs(filters), duration)

      result(0).index mustBe <= (result(1).index)
    }
  }

}