package matching

import play.api.libs.json.Json

case class CollapseRequest(
  globalCodeParent: String,
  globalCodeChildren: List[String],
  courtCaseId: Long
)

object CollapseRequest:
  implicit val format: play.api.libs.json.OFormat[CollapseRequest] = Json.format[CollapseRequest]
