package probability

import play.api.libs.json.{Format, Json}
import profile.Profile.Genotypification
import types.StatOption

case class FullHypothesis(selected: Array[Genotypification], unselected: Array[Genotypification], unknowns: Int, dropOut: Double)

object FullHypothesis {
  implicit val hypothesisFormat: Format[FullHypothesis] = Json.format[FullHypothesis]
}

case class FullCalculationScenario(sample: Genotypification, prosecutor: FullHypothesis, defense: FullHypothesis, stats: StatOption)

object FullCalculationScenario {
  implicit val calculationScenarioFormat: Format[FullCalculationScenario] = Json.format[FullCalculationScenario]
}
