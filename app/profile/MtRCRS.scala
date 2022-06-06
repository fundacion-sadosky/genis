package profile

import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._


case class MtRCRS (tabla:Map[Int,String])

object MtRCRS {

  implicit val mapReads: Reads[Map[Int,String]] = new Reads[Map[Int,String]] {
    def reads(jv: JsValue): JsResult[Map[Int,String]] =
      JsSuccess(jv.as[Map[Int,String]].map{case (k, v) =>
        k -> v
      })
  }

  implicit val mapWrites: Writes[Map[Int,String]] = new Writes[Map[Int,String]] {
    def writes(map: Map[Int,String]): JsValue =
      Json.obj(map.map{case (s, o) =>
        val ret: (String, JsValueWrapper) = s.toString -> Json.toJson(o)
        ret
      }.toSeq:_*)
  }

  implicit val f = Json.format[MtRCRS]
}
