package connections

import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._
import profile.{Analysis, Profile}
import profile.GenotypificationByType.GenotypificationByType

case class PendingProfileApproval(
                                   globalCode: String,
                                   laboratory: String,
                                   category: String,
                                   analyses: Option[scala.List[Analysis]],
                                   receptionDate: String = "",
                                   errors: Option[String] = None,
                                   genotypification: GenotypificationByType,
                                   hasAssociatedProfile:Boolean = false,
                                   hasElectropherogram:Boolean = false,
                                   assignee :String,
                                   hasFiles:Boolean = false
                                 )

object PendingProfileApproval {
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

  implicit val mapFormat: Format[GenotypificationByType] = Format(mapReads, mapWrites)
  implicit val format = Json.format[PendingProfileApproval]
}