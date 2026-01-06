package audit

import java.util.Date
import play.api.libs.json.Json
import scala.util.Random
import types.TotpToken

case class OperationLogEntry(
  index: Long,
  userId: String,
  otp: Option[TotpToken],
  timestamp: Date,
  method: String,
  path: String,
  action: String,
  buildNo: String,
  result: Option[String],
  status: Int,
  lotId: Long,
  description: String)

object OperationLogEntry {
  implicit val writes = Json.writes[OperationLogEntry]
}

case class OperationLogEntryAttemp(
    userId: String,
    otp: Option[TotpToken],
    timestamp: Date,
    method: String,
    path: String,
    action: String,
    buildNo: String,
    result: Option[String],
    status: Int) extends Serializable {

  override def stringify = s"$userId${otp.fold("")(_.text)}${timestamp.getTime}$method$path$action$buildNo$result$status"

}

case class SignedOperationLogEntry(
    index: Long,
    userId: String,
    otp: Option[TotpToken],
    timestamp: Date,
    method: String,
    path: String,
    action: String,
    buildNo: String,
    result: Option[String],
    status: Int,
    lotId: Long,
    signature: Key,
    description: String) extends Signature {

  override def stringify = s"$userId${otp.fold("")(_.text)}${timestamp.getTime}$method$path$action$buildNo$result$status$lotId$description"

}

object SignedOperationLogEntry {
  implicit val writes = Json.writes[SignedOperationLogEntry]
}

case class OperationLogLot(
  id: Long,
  initDate: Date,
  kZero: Key)

case class OperationLogLotView(
  id: Long,
  initDate: Date)

object OperationLogLotView {
  implicit val writes = Json.writes[OperationLogLotView]
}
