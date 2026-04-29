package pedigree

import play.api.libs.json.{Json, OFormat}

case class MutationDefaultParam(
  locus: String,
  sex: String,
  mutationRate: Option[scala.math.BigDecimal]
)

object MutationDefaultParam:
  given OFormat[MutationDefaultParam] = Json.format[MutationDefaultParam]
