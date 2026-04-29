package pedigree

import play.api.libs.json.{Format, Json, OFormat}

case class MutationModel(
  id: Long,
  name: String,
  mutationType: Long,
  active: Boolean,
  ignoreSex: Boolean,
  cantSaltos: Long
)

object MutationModel:
  given OFormat[MutationModel] = Json.format[MutationModel]

case class MutationModelFull(
  header: MutationModel,
  parameters: List[MutationModelParameter]
)

object MutationModelFull:
  given OFormat[MutationModelFull] = Json.format[MutationModelFull]
