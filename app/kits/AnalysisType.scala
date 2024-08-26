package kits

import play.api.libs.json.Json

case class AnalysisType(
  id: Int,
  name: String,
  mitochondrial: Boolean = false)

object AnalysisType {
  implicit val analysisTypeFormat = Json.format[AnalysisType]
}
