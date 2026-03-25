package kits

import play.api.libs.json.*

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

object Locus {
  implicit val format: Format[Locus] = Json.format[Locus]
}
