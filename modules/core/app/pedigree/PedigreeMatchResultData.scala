package pedigree

import play.api.libs.json.Json

case class PedigreeMatchResultData(
  pedigreeMatchResult: PedigreeMatchResult,
  internalCode: String
)

object PedigreeMatchResultData:
  implicit val pedigreeMatchResultData: play.api.libs.json.OFormat[PedigreeMatchResultData] = Json.format[PedigreeMatchResultData]
