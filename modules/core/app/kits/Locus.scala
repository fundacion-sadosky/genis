package kits

import play.api.libs.json.{Json, OFormat}

case class Locus(
  id: String,
  name: String,
  chromosome: Option[String],
  minimumAllelesQty: Int,
  maximumAllelesQty: Int,
  analysisType: Int,
  required: Boolean = true,
  minAlleleValue: Option[BigDecimal] = None,
  maxAlleleValue: Option[BigDecimal] = None
)

object Locus:
  given OFormat[Locus] = Json.format[Locus]

case class LocusLink(
  locus: String,
  factor: Double,
  distance: Double
)

object LocusLink:
  given OFormat[LocusLink] = Json.format[LocusLink]

case class FullLocus(
  locus: Locus,
  alias: List[String],
  links: List[LocusLink]
)

object FullLocus:
  given OFormat[FullLocus] = Json.format[FullLocus]

case class AleleRange(min: BigDecimal, max: BigDecimal)

object AleleRange:
  given OFormat[AleleRange] = Json.format[AleleRange]
