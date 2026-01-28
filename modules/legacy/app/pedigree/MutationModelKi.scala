package pedigree

import play.api.libs.json.Json

case class MutationModelKi(id: Long,idMutationModelParameter: Long,allele:Double,ki:scala.math.BigDecimal)
object MutationModelKi {
  implicit val mutationModelKi = Json.format[MutationModelKi]
}