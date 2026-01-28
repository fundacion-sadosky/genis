package kits

import play.api.libs.json.Json

case class Locus(
  id: String,
  name: String,
  chromosome: Option[String],
  minimumAllelesQty: Int,
  maximumAllelesQty: Int,
  analysisType: Int,
  required:Boolean = true,
  minAlleleValue:Option[scala.math.BigDecimal]=None,
  maxAlleleValue:Option[scala.math.BigDecimal]=None)

object Locus {
  implicit val locusFormat = Json.format[Locus]
}
