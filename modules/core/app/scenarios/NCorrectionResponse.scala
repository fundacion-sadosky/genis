package scenarios

import play.api.libs.json.*

case class NCorrectionResponse(
  dmp: Double,
  donnellyBaldwin: Double,
  n: Long
)

object NCorrectionResponse:
  implicit val nCorrectionResponseFormat: Format[NCorrectionResponse] = Json.format[NCorrectionResponse]
