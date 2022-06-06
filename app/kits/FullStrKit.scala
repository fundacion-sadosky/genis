package kits

import play.api.libs.json.Json

case class NewStrKitLocus(
     locus: String,
     fluorophore: Option[String],
     order: Int)

object NewStrKitLocus {
  implicit val format = Json.format[NewStrKitLocus]
}

case class FullStrKit(
    id: String,
    name: String,
    `type`: Int,
    locy_quantity: Int,
    representative_parameter: Int,
    alias: Seq[String],
    locus: Seq[NewStrKitLocus])

object FullStrKit {
  implicit val format = Json.format[FullStrKit]
}