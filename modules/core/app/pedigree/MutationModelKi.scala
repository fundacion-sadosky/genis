package pedigree

import play.api.libs.json.{Json, OFormat}

case class MutationModelKi(
  id: Long,
  idMutationModelParameter: Long,
  allele: Double,
  ki: scala.math.BigDecimal
)

object MutationModelKi:
  given OFormat[MutationModelKi] = Json.format[MutationModelKi]
