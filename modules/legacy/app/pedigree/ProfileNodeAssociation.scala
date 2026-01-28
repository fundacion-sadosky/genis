package pedigree

import play.api.libs.json._

case class ProfileNodeAssociation(globalCode:String,
                                  internalSampleCode: String,
                                  category:String,
                                  assignee:String,
                                  label: Option[String])
object ProfileNodeAssociation {
  implicit val searchFormat = Json.format[ProfileNodeAssociation]
}
