package probability

import play.api.libs.functional.syntax.{functionalCanBuildApplicative, toFunctionalBuilderOps}
import play.api.libs.json._
import profile.Profile

import scala.math.Fractional.Implicits.infixFractionalOps
import scala.util.Try

case class LRResult(total: Double, detailed: Map[Profile.Marker, Option[Double]]) {}

object LRResult {
   // implicit val lrFormat = Json.format[LRResult]

   implicit val doubleReads: Reads[Double] = new Reads[Double] {

    def reads(jv: JsValue): JsResult[Double] = JsSuccess(Try{jv.asInstanceOf[JsString].value.toDouble}.getOrElse(1))

    //def reads(jv: JsValue): JsResult[Option[Double]] = JsSuccess(Try{jv.asInstanceOf[JsString].value.toDouble}.getOrElse(JsNull.asInstanceOf[Double].t))
    //def reads(jv: JsValue): JsResult[Double] = JsSuccess(Try{jv.asInstanceOf[JsString].value.toDouble}.toOption)
    //def reads(jv: JsValue): JsResult[Double] = JsSuccess(Try{jv.asInstanceOf[JsString].value.toDouble}.getOrElse(None))
    //def reads(jv: JsValue): JsResult[Double] = JsSuccess(jv.toString().substring(1,jv.toString().length-1).toDouble)
    //def reads(jv: JsValue): JsResult[Double] = JsSuccess(jv.asInstanceOf[JsString].value.toDouble)
    //def reads(jv: JsValue): JsResult[Double] = JsSuccess(jv.as[String].toDouble)
  }

  implicit val doubleWrites: Writes[Double] = new Writes[Double] {
    def writes(l: Double): JsValue = Json.toJson(l.toString)
    //def writes(l: Double): JsValue = Json.toJson(l)
  }

  //implicit val doubleFormat: Format[Double] = Format(doubleReads, doubleWrites)

  implicit val odoubleReads: Reads[Option[Double]] = new Reads[Option[Double]] {
    def reads(jv: JsValue): JsResult[Option[Double]] = JsSuccess(Try{jv.asInstanceOf[JsString].value.toDouble}.toOption)
    //def reads(jv: JsValue): JsResult[Option[Double]] = JsSuccess(Try{jv.toString().toDouble}.toOption)
    //def reads(jv: JsValue): JsResult[Double] = JsSuccess(Try{jv.asInstanceOf[JsString].value.toDouble}.getOrElse(None))
  }
//
//  implicit val doublesWrites: Writes[Option[Double]] = new Writes[Option[Double]] {
//    def writes(l: Option[Double]): JsValue = Json.toJson(l.toString)
//  }
//  implicit val doublesFormat: Format[Option[Double]] = Format(doublesReads, doublesWrites)

  ////

  implicit val lrResultReads: Reads[LRResult] = (
    (__ \ "total").read[Double] ~
      (__ \ "detailed").read[Map[Profile.Marker, Option[Double]]]
    )(LRResult.apply _)

  implicit val lrResultWrites: OWrites[LRResult] = (
    (__ \ "total").write[Double] ~
      (__ \ "detailed").write[Map[Profile.Marker, Option[Double]]]
    )((lrResult: LRResult) => (
    lrResult.total,
    lrResult.detailed
  ))

  implicit val lrResultFormat: OFormat[LRResult] = OFormat(lrResultReads, lrResultWrites)

}
