package scenarios

import play.api.libs.json.Json
import profile.Profile
import types.{SampleCode, StatOption}

case class Hypothesis(selected: List[SampleCode], unselected: List[SampleCode], unknowns: Int, dropOut: Double)

object Hypothesis {
  implicit val hypothesisFormat = Json.format[Hypothesis]
}

case class CalculationScenario(sample: SampleCode, prosecutor: Hypothesis, defense: Hypothesis, stats: StatOption,profiles:Option[List[Profile]]= None, isMixMix : Boolean = false)

object CalculationScenario {
  implicit val calculationScenarioFormat = Json.format[CalculationScenario]
}
