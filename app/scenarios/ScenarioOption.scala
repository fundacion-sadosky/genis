package scenarios

import play.api.libs.json.Json
import types.{AlphanumericId, SampleCode}

// A row to choose as Profile in the Hypothesis

case class ScenarioOption(
     globalCode: SampleCode,
     internalSampleCode: String,
     categoryId: AlphanumericId,
     sharedAllelePonderation: Double,
     contributors: Int,
     dropOuts: Int,
     associated: Boolean
)

object ScenarioOption {
  implicit val scenarioOptionFormat = Json.format[ScenarioOption]
}