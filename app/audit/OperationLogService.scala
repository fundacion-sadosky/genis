package audit

import javax.inject.{Inject, Named, Singleton}

import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.JsResult
import search.FullTextSearch
import user.RoleService

import scala.collection.immutable.IndexedSeq
import scala.concurrent.Future
import scala.language.postfixOps

abstract class OperationLogService {
  def add(logEntry: OperationLogEntryAttemp): Future[Unit]
  def listLotsView(page: Int, pageSize: Int): Future[Seq[OperationLogLotView]]
  def checkLot(id: Long): Future[Either[(SignedOperationLogEntry, Key), Unit]]
  def getLotsLength(): Future[Int]
  def searchLogs(operationLogSearch: OperationLogSearch): Future[Seq[OperationLogEntry]]
  def getLogsLength(operationLogSearch: OperationLogSearch): Future[Int]
}

@Singleton
class PeoOperationLogService @Inject() (
    logRepository: OperationLogRepository,
    fullTextSearch: FullTextSearch,
    roleService: RoleService,
    @Named("logEntrySigner") signerService: PEOSignerService,
    @Named("lotSize") val lotSize: Int,
    @Named("chunkSize") val chunkSize: Int) extends OperationLogService {

  implicit val logExecutionContext = Akka.system.dispatchers.lookup("play.akka.actor.log-context")

  private def getLogEntryEnumeator(lotId: Long, index: Int, chunkSize: Int): Enumerator[IndexedSeq[SignedOperationLogEntry]] = {

    val entryEnumerator: Enumerator[IndexedSeq[SignedOperationLogEntry]] = Enumerator.unfoldM(0)(offset => {
      if (offset < index) {
        logRepository.searchLogs(OperationLogSearch(lotId, chunkSize, offset)).map {
          entries =>
            if (entries.isEmpty) {
              None
            } else {
              Some((offset + chunkSize, entries))
            }
        }
      } else {
        Future.successful(None)
      }
    })

    entryEnumerator

  }

  override def add(entry: OperationLogEntryAttemp): Future[Unit] = {
    val build = (log: OperationLogEntryAttemp, key: Key, lotId: Long) => {
      val signature = Signature.computeSignature(
        log.stringify + lotId,
        key)
      SignedOperationLogEntry(
        0,
        log.userId,
        log.otp,
        log.timestamp,
        log.method,
        log.path,
        log.action,
        log.buildNo,
        log.result,
        log.status,
        lotId,
        signature,
        roleService.translatePermission(log.method, log.path))
    }

    val persist = (entry: SignedOperationLogEntry) => {
      logRepository.add(entry)
    }

    val peoEntry = PEOEntry(entry, build, persist)

    signerService.addEntry(peoEntry) map { result => () }
  }

  override def getLogsLength(operationLogSearch: OperationLogSearch): Future[Int] = {
    logRepository.countLogs(operationLogSearch)
  }

  override def getLotsLength(): Future[Int] = {
    logRepository.countLots
  }

  override def listLotsView(page: Int, pageSize: Int): Future[Seq[OperationLogLotView]] = {
    val offset = page * pageSize
    logRepository.listLots(pageSize, offset) map { opLogLots =>
      opLogLots map { opLogLot => OperationLogLotView(opLogLot.id, opLogLot.initDate) }
    }
  }

  override def checkLot(id: Long): Future[Either[(SignedOperationLogEntry, Key), Unit]] = {
    logRepository.getLot(id).flatMap { lot =>
      val entries = getLogEntryEnumeator(lot.id, lotSize, chunkSize)
      PEOAlgorithm.verifyEntries(entries, lot.kZero)
    }
  }

  override def searchLogs(operationLogSearch: OperationLogSearch): Future[Seq[OperationLogEntry]] = {
    logRepository.searchLogs(operationLogSearch).map {
      _.map { entry =>
        OperationLogEntry(entry.index, entry.userId, entry.otp, entry.timestamp, entry.method, entry.path, entry.action, entry.buildNo, entry.result, entry.status, entry.lotId, entry.description)
      }
    }
  }

}
