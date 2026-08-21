package probability

import play.api.libs.json.Json
import profile.Profile
import profile.Profile._
import types.StatOption


case class FullHypothesis(selected: Array[Genotypification], unselected: Array[Genotypification], unknowns: Int, dropOut: Double)

object FullHypothesis {
  implicit val hypothesisFormat = Json.format[FullHypothesis]
}

case class FullCalculationScenario(sample: Genotypification, prosecutor: FullHypothesis, defense: FullHypothesis, stats: StatOption)

object FullCalculationScenario {
  implicit val calculationScenarioFormat = Json.format[FullCalculationScenario]
}