package audit

import javax.inject.{Inject, Named, Singleton}
import scala.collection.immutable.IndexedSeq
import scala.concurrent.{ExecutionContext, Future}

abstract class OperationLogService {
  def add(logEntry: OperationLogEntryAttemp): Future[Unit]
  def listLotsView(page: Int, pageSize: Int): Future[Seq[OperationLogLotView]]
  def checkLot(id: Long): Future[Either[(SignedOperationLogEntry, Key), Unit]]
  def getLotsLength(): Future[Int]
  def searchLogs(search: OperationLogSearch): Future[Seq[OperationLogEntry]]
  def getLogsLength(search: OperationLogSearch): Future[Int]
}

@Singleton
class PeoOperationLogService @Inject()(
  logRepository: OperationLogRepository,
  @Named("logEntrySigner") signerService: PEOSignerService,
  @Named("lotSize")   val lotSize:   Int,
  @Named("chunkSize") val chunkSize: Int
)(implicit ec: ExecutionContext) extends OperationLogService {

  override def add(entry: OperationLogEntryAttemp): Future[Unit] = {
    val build = (log: OperationLogEntryAttemp, key: Key, lotId: Long) => {
      val sig = Signature.computeSignature(log.stringify + lotId, key)
      SignedOperationLogEntry(
        index       = 0L,
        userId      = log.userId,
        otp         = log.otp,
        timestamp   = log.timestamp,
        method      = log.method,
        path        = log.path,
        action      = log.action,
        buildNo     = log.buildNo,
        result      = log.result,
        status      = log.status,
        lotId       = lotId,
        signature   = sig,
        description = PermissionHelper.translatePermission(log.method, log.path)
      )
    }
    val persist = (e: SignedOperationLogEntry) => logRepository.add(e)
    signerService.addEntry(PEOEntry(entry, build, persist))
  }

  override def getLogsLength(search: OperationLogSearch): Future[Int] =
    logRepository.countLogs(search)

  override def getLotsLength(): Future[Int] =
    logRepository.countLots()

  override def listLotsView(page: Int, pageSize: Int): Future[Seq[OperationLogLotView]] = {
    val offset = page * pageSize
    logRepository.listLots(pageSize, offset).map {
      _.map(lot => OperationLogLotView(lot.id, lot.initDate))
    }
  }

  override def checkLot(id: Long): Future[Either[(SignedOperationLogEntry, Key), Unit]] =
    for {
      lot    <- logRepository.getLot(id)
      count  <- logRepository.countLogs(OperationLogSearch(lot.id))
      result <- PEOAlgorithm.verifyEntries(
        page => logRepository.searchLogs(
          OperationLogSearch(lot.id, page = page, pageSize = chunkSize,
            ascending = Some(true), sortField = Some("id"))
        ),
        count, chunkSize, lot.kZero
      )
    } yield result

  override def searchLogs(search: OperationLogSearch): Future[Seq[OperationLogEntry]] =
    logRepository.searchLogs(search).map {
      _.map { e =>
        OperationLogEntry(e.index, e.userId, e.otp, e.timestamp, e.method,
          e.path, e.action, e.buildNo, e.result, e.status, e.lotId, e.description)
      }
    }
}
