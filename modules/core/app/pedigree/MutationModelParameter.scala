package pedigree

import play.api.libs.json.{Json, OFormat}

case class MutationModelParameter(
  id: Long,
  idMutationModel: Long,
  locus: String,
  sex: String,
  mutationRate: Option[scala.math.BigDecimal],
  mutationRange: Option[scala.math.BigDecimal],
  mutationRateMicrovariant: Option[scala.math.BigDecimal]
)

object MutationModelParameter:
  given OFormat[MutationModelParameter] = Json.format[MutationModelParameter]
