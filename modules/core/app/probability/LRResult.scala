package probability

import play.api.libs.json.*
import play.api.libs.functional.syntax.*
import profile.Profile

import scala.util.Try

case class LRResult(total: Double, detailed: Map[Profile.Marker, Option[Double]])

object LRResult:

  implicit val doubleReads: Reads[Double] = Reads:
    case JsNumber(n) => JsSuccess(n.toDouble)
    case JsString(s) => JsSuccess(Try(s.toDouble).getOrElse(1.0))
    case _ => JsError("Expected number or string")

  implicit val doubleWrites: Writes[Double] = Writes(l => JsString(l.toString))

  implicit val odoubleReads: Reads[Option[Double]] = Reads:
    case JsString(s) => JsSuccess(Try(s.toDouble).toOption)
    case JsNumber(n) => JsSuccess(Some(n.toDouble))
    case JsNull => JsSuccess(None)
    case _ => JsError("Expected string, number, or null")

  implicit val lrResultReads: Reads[LRResult] = (
    (__ \ "total").read[Double] ~
    (__ \ "detailed").read[Map[Profile.Marker, Option[Double]]]
  )(LRResult.apply _)

  implicit val lrResultWrites: OWrites[LRResult] = (
    (__ \ "total").write[Double] ~
    (__ \ "detailed").write[Map[Profile.Marker, Option[Double]]]
  )((lrResult: LRResult) => (lrResult.total, lrResult.detailed))

  implicit val lrResultFormat: OFormat[LRResult] = OFormat(lrResultReads, lrResultWrites)
