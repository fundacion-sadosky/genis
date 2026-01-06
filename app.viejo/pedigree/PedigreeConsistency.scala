package pedigree

import play.api.libs.json._
import play.api.libs.json.Json.JsValueWrapper
import profile.GenotypificationByType.GenotypificationByType
import profile.Profile

case class PedigreeConsistency(globalCode:String,
                             internalCode: String,
                             locus:List[String])
object PedigreeConsistency {
  implicit val pedigreeConsistency = Json.format[PedigreeConsistency]
}
case class PedigreeConsistencyCheck(globalCode:String,
                               locus:List[String])
object PedigreeConsistencyCheck {
  implicit val pedigreeConsistencyCheck = Json.format[PedigreeConsistencyCheck]
}

