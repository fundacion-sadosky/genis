package kits

import play.api.libs.json.Json

case class StrKitLocus(
   id: String,
   name: String,
   chromosome: Option[String],
   minimumAllelesQty: Int,
   maximumAllelesQty: Int,
   fluorophore: Option[String],
   order: Int,
   required: Boolean = false)

object StrKitLocus {
  implicit val viewFormat = Json.format[StrKitLocus]
}