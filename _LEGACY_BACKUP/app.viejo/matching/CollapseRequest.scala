package matching

import play.api.libs.json.Json

case class CollapseRequest(globalCodeParent:String,
                           globalCodeChildren:List[String],
                           courtCaseId:Long)

object CollapseRequest {
  implicit val collapseRequestFormat = Json.format[CollapseRequest]
}