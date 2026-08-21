package pedigree

import play.api.libs.json.Json

case class MutationDefaultParam(locus:String,sex:String,mutationRate:Option[scala.math.BigDecimal])
object MutationDefaultParam {
  implicit val mutationDefaultParam = Json.format[MutationDefaultParam]
}