package stats

import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, Json, Reads, Writes}
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

//object PopulationBaseFrequencyView {
//  implicit val populationBaseFrequencyViewFormat = Json.format[PopulationBaseFrequencyView]
//}

object PopulationBaseFrequencyView {
  // Define los Reads y Writes para Option[BigDecimal]
  implicit val optionBigDecimalReads: Reads[Option[BigDecimal]] = Reads.optionWithNull[BigDecimal]
  implicit val optionBigDecimalWrites: Writes[Option[BigDecimal]] = Writes.optionWithNull[BigDecimal]

  // Define los implicits para listas anidadas
  implicit val listOfListOptionBigDecimalReads: Reads[List[List[Option[BigDecimal]]]] = Reads.list(Reads.list(optionBigDecimalReads))
  implicit val listOfListOptionBigDecimalWrites: Writes[List[List[Option[BigDecimal]]]] = Writes.list(Writes.list(optionBigDecimalWrites))

  implicit val populationBaseFrequencyViewFormat: Format[PopulationBaseFrequencyView] = Json.format[PopulationBaseFrequencyView]

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