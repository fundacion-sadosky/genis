package scenarios

import play.api.libs.json.*
import types.SampleCode

case class NCorrectionRequest(
  firingCode: SampleCode,
  matchingCode: SampleCode,
  bigN: Long,
  lr: Double
)

object NCorrectionRequest:
  implicit val nCorrectionRequestFormat: Format[NCorrectionRequest] = Json.format[NCorrectionRequest]
