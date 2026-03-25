package audit

import java.util.Date
import play.api.libs.json.{Json, OWrites}
import types.TotpToken

case class OperationLogEntry(
  index:       Long,
  userId:      String,
  otp:         Option[TotpToken],
  timestamp:   Date,
  method:      String,
  path:        String,
  action:      String,
  buildNo:     String,
  result:      Option[String],
  status:      Int,
  lotId:       Long,
  description: String
)

object OperationLogEntry {
  given writes: OWrites[OperationLogEntry] = Json.writes[OperationLogEntry]
}

case class OperationLogEntryAttemp(
  userId:    String,
  otp:       Option[TotpToken],
  timestamp: Date,
  method:    String,
  path:      String,
  action:    String,
  buildNo:   String,
  result:    Option[String],
  status:    Int
) extends Stringifiable {
  override def stringify: String =
    s"$userId${otp.fold("")(_.text)}${timestamp.getTime}$method$path$action$buildNo$result$status"
}

case class SignedOperationLogEntry(
  index:       Long,
  userId:      String,
  otp:         Option[TotpToken],
  timestamp:   Date,
  method:      String,
  path:        String,
  action:      String,
  buildNo:     String,
  result:      Option[String],
  status:      Int,
  lotId:       Long,
  signature:   Key,
  description: String
) extends Signature {
  override def stringify: String =
    s"$userId${otp.fold("")(_.text)}${timestamp.getTime}$method$path$action$buildNo$result$status$lotId$description"
}

object SignedOperationLogEntry {
  given writes: OWrites[SignedOperationLogEntry] = Json.writes[SignedOperationLogEntry]
}

case class OperationLogLot(id: Long, initDate: Date, kZero: Key)

case class OperationLogLotView(id: Long, initDate: Date)

object OperationLogLotView {
  given writes: OWrites[OperationLogLotView] = Json.writes[OperationLogLotView]
}
