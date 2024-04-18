package scenarios

import play.api.libs.json.Json
import types.SampleCode

case class NCorrectionRequest(
  firingCode: SampleCode,
  matchingCode: SampleCode,
  bigN: Long,
  lr: Double)

object NCorrectionRequest {
  implicit val nCorrectionRequestFormat = Json.format[NCorrectionRequest]
}

