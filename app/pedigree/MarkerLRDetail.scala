package pedigree

import play.api.libs.json.{Format, Json}

case class MarkerLRDetail(lr: Double, classification: String)

object MarkerLRDetail {
  implicit val format: Format[MarkerLRDetail] = Json.format[MarkerLRDetail]
}
