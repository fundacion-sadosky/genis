package pedigree

import play.api.libs.json.Json

case class MutationModelType(id: Long,description: String)
object MutationModelType {
  implicit val mutationModelType = Json.format[MutationModelType]
}