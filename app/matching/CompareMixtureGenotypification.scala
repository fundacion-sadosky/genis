package matching

import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.{Format, Writes, _}
import profile._

/*object CompareMixtureByType {
  type CompareMixtureByType = Map[Int, List[CompareMixtureGenotypification]]

  implicit val mapReads: Reads[CompareMixtureByType] = new Reads[CompareMixtureByType] {
    def reads(jv: JsValue): JsResult[CompareMixtureByType] =
      JsSuccess(jv.as[Map[String, List[CompareMixtureGenotypification]]].map{case (k, v) =>
        Integer.parseInt(k) -> v.asInstanceOf[List[CompareMixtureGenotypification]]
      })
  }

  implicit val mapWrites: Writes[CompareMixtureByType] = new Writes[CompareMixtureByType] {
    def writes(map: CompareMixtureByType): JsValue =
      Json.obj(map.map{case (s, o) =>
        val ret: (String, JsValueWrapper) = s.toString -> Json.toJson(o)
        ret
      }.toSeq:_*)
  }

  implicit val mapFormat: Format[CompareMixtureByType] = Format(mapReads, mapWrites)
}*/

case class CompareMixtureGenotypification(
    locus: Profile.Marker,
    g: Map[String,Seq[AlleleValue]]
)

object CompareMixtureGenotypification {
  implicit val compareMixtureGenotypificationFormat = Json.format[CompareMixtureGenotypification]
}

//type CompareMixtureByType = Map[Int, Seq[CompareMixtureGenotypification]]