package stats

import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.libs.json.Format
import play.api.libs.json.__
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Json
import probability.ProbabilityModel
import profile.Profile
import types.MinimunFrequencyCalc

case class PopulationBaseFrequency(
  name: String,
  theta: Double,
  model: ProbabilityModel.Value,
  base: Seq[PopulationSampleFrequency])

case class PopulationSampleFrequency(
  marker: String,
  allele: Double,
  frequency: BigDecimal)

case class PopulationBaseFrequencyGrouppedByLocus(base: Map[String,List[Double]])

case class PopulationBaseFrequencyNameView(
  name: String,
  theta: Double,
  model: String,
  state: Boolean,
  default: Boolean)

object PopulationBaseFrequencyNameView {
  implicit val populationBaseFrequencyNameViewFormat = Json.format[PopulationBaseFrequencyNameView]
}

case class PopulationBaseFrequencyView(
  alleles: List[Double],
  markers: List[String],
  frequencys: List[List[Option[BigDecimal]]])

object PopulationBaseFrequencyView {
  implicit val populationBaseFrequencyViewFormat = Json.format[PopulationBaseFrequencyView]
}

case class Fmins(
  calcOption: MinimunFrequencyCalc.MinimunFrequencyCalc,
  config: Map[Profile.Marker, Seq[Double]])

object Fmins {
  implicit val fminsFormat = Json.format[Fmins]
}

case class PopBaseFreqResult(
  status: String,
  key: Option[String],
  loci: Option[Seq[Profile.Marker]],
  errors: Seq[String],
  inserts: Option[Int])

object PopBaseFreqResult {
  implicit val popBaseFreqResultFormat = Json.format[PopBaseFreqResult]
}