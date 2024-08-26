package pedigree

import play.api.libs.json._
import play.api.libs.json.Json.JsValueWrapper
import profile.GenotypificationByType.GenotypificationByType
import profile.Profile

case class CourtCasePedigree(globalCode:String,
                             internalCode: String,
                             genotypification: GenotypificationByType,
                             idBatch:Option[Long] = None,
                             batchLabel:Option[String] = None,
                             statusProfile: String,
                             groupedBy:Option[String] = None)
object CourtCasePedigree {
  implicit val mapReads: Reads[GenotypificationByType] = new Reads[GenotypificationByType] {
    def reads(jv: JsValue): JsResult[GenotypificationByType] =
      JsSuccess(jv.as[Map[String, Profile.Genotypification]].map{case (k, v) =>
        Integer.parseInt(k) -> v.asInstanceOf[Profile.Genotypification]
      })
  }

  implicit val mapWrites: Writes[GenotypificationByType] = new Writes[GenotypificationByType] {
    def writes(map: GenotypificationByType): JsValue =
      Json.obj(map.map{case (s, o) =>
        val ret: (String, JsValueWrapper) = s.toString -> Json.toJson(o)
        ret
      }.toSeq:_*)
  }
  implicit val searchFormat = Json.format[CourtCasePedigree]
}

