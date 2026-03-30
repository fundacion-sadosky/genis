package kits

import play.api.libs.json.*

case class AnalysisType(
  id: Int,
  name: String,
  mitochondrial: Boolean = false
)

object AnalysisType {
  implicit val format: Format[AnalysisType] = Json.format[AnalysisType]
}
