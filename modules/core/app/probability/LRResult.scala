package probability

import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import profile.Profile

import scala.util.Try

case class LRResult(total: Double, detailed: Map[Profile.Marker, Option[Double]])

object LRResult {

  implicit val doubleReads: Reads[Double] = new Reads[Double] {
    def reads(jv: JsValue): JsResult[Double] = JsSuccess(Try(jv.asInstanceOf[JsString].value.toDouble).getOrElse(1))
  }

  implicit val doubleWrites: Writes[Double] = new Writes[Double] {
    def writes(l: Double): JsValue = Json.toJson(l.toString)
  }

  implicit val odoubleReads: Reads[Option[Double]] = new Reads[Option[Double]] {
    def reads(jv: JsValue): JsResult[Option[Double]] = JsSuccess(Try(jv.asInstanceOf[JsString].value.toDouble).toOption)
  }

  implicit val lrResultReads: Reads[LRResult] = (
    (__ \ "total").read[Double] ~
      (__ \ "detailed").read[Map[Profile.Marker, Option[Double]]]
  )(LRResult.apply)

  implicit val lrResultWrites: OWrites[LRResult] = (
    (__ \ "total").write[Double] ~
      (__ \ "detailed").write[Map[Profile.Marker, Option[Double]]]
  )((lrResult: LRResult) => (lrResult.total, lrResult.detailed))

  implicit val lrResultFormat: OFormat[LRResult] = OFormat(lrResultReads, lrResultWrites)
}
