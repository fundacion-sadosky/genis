package probability

import play.api.libs.json._
import profile.Profile

case class LRResult(total: Double, detailed: Map[Profile.Marker, Option[Double]]) {}

object LRResult {
  implicit val lrFormat = Json.format[LRResult]
}
