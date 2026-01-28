package pedigree

import play.api.libs.json.Json
import util.PlayEnumUtils

case class CaseProfileAdd(courtcaseId:Long,globalCode:String, profileType:Option[String],groupedBy: Option[String] = None)

object CaseProfileAdd {
  implicit val searchFormat = Json.format[CaseProfileAdd]
}

case class AssociateProfile(profiles: List[CaseProfileAdd], isReference : Boolean)

object AssociateProfile{
  implicit val searchFormat = Json.format[AssociateProfile]
}

object CollapsingStatus extends Enumeration{
  type CollapsingStatus = Value
  val  Active, Collapsed = Value

  implicit val enumTypeFormat = PlayEnumUtils.enumFormat(CollapsingStatus)
}

