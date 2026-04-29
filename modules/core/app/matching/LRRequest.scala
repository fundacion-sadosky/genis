package matching

import play.api.libs.json.Json
import types.{SampleCode, StatOption}

case class LRRequest(
  firingCode: SampleCode,
  matchingCode: SampleCode,
  stats: Option[StatOption],
  matchingId: Option[String] = None
)

object LRRequest:
  implicit val format: play.api.libs.json.OFormat[LRRequest] = Json.format[LRRequest]
