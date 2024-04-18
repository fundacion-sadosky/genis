package scenarios

import play.api.libs.json.Json

case class NCorrectionResponse(
  dmp: Double,
  donnellyBaldwin: Double,
  n: Long)

object NCorrectionResponse {
  implicit val nCorrectionResponseFormat = Json.format[NCorrectionResponse]
}

