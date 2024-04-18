package configdata

import play.api.libs.json.Json
import types.AlphanumericId

case class BioMaterialType(
  id: AlphanumericId,
  name: String,
  description: Option[String])

object BioMaterialType {
  implicit val bioMaterialTypeFormat = Json.format[BioMaterialType]
}
