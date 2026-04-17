package pedigree

import play.api.libs.json.{Json, OFormat}

case class MutationModelType(id: Long, description: String)

object MutationModelType:
  given OFormat[MutationModelType] = Json.format[MutationModelType]
