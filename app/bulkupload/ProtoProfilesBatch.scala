package bulkupload

import java.util.Date
import play.api.libs.json.Json

case class ProtoProfilesBatch(
  id: Long,
  user: String,
  creationDate: Date,
  label: Option[String]                            )

case class ProtoProfilesBatchView(
  id: Long,
  user: String,
  creationDate: Date,
  totalAnalysis: Int,
  approvedAnalysis: Int,
  pendingAnalysis: Int,
  rejectedAnalysis: Int,
  label: Option[String] ,
  totalForApprovalOrImport: Int,
  totalForIncomplete: Int = 0,
  analysisType: String                               )

object ProtoProfilesBatchView {
  implicit val viewFormat = Json.format[ProtoProfilesBatchView]
}

case class BatchDetails(
  batch: ProtoProfilesBatchView,
  protoProfiles: Seq[ProtoProfile])

object BatchDetails {
  implicit val viewFormat = Json.writes[BatchDetails]
}