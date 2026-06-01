package bulkupload

import java.util.Date
import play.api.libs.json.{Json, OFormat, OWrites}

case class ProtoProfilesBatch(
  id: Long,
  user: String,
  creationDate: Date,
  label: Option[String]
)

case class ProtoProfilesBatchView(
  id: Long,
  user: String,
  creationDate: Date,
  totalAnalysis: Int,
  approvedAnalysis: Int,
  pendingAnalysis: Int,
  rejectedAnalysis: Int,
  label: Option[String],
  totalForApprovalOrImport: Int,
  totalForIncomplete: Int = 0,
  analysisType: String,
  batchTotal: Int = 0
)

object ProtoProfilesBatchView:
  implicit val format: OFormat[ProtoProfilesBatchView] = Json.format

case class BatchDetails(batch: ProtoProfilesBatchView, protoProfiles: Seq[ProtoProfile])

object BatchDetails:
  implicit val writes: OWrites[BatchDetails] = Json.writes