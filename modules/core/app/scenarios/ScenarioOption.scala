package scenarios

import play.api.libs.json.*
import types.{AlphanumericId, SampleCode}

case class ScenarioOption(
  globalCode: SampleCode,
  internalSampleCode: String,
  categoryId: AlphanumericId,
  sharedAllelePonderation: Double,
  contributors: Int,
  dropOuts: Int,
  associated: Boolean
)

object ScenarioOption:
  implicit val scenarioOptionFormat: Format[ScenarioOption] = Json.format[ScenarioOption]
