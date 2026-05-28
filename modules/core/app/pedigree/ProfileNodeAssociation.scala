package pedigree

import play.api.libs.json.Json

case class ProfileNodeAssociation(
  globalCode: String,
  internalSampleCode: String,
  category: String,
  assignee: String,
  label: Option[String]
)

object ProfileNodeAssociation:
  implicit val format: play.api.libs.json.OFormat[ProfileNodeAssociation] =
    Json.format[ProfileNodeAssociation]
