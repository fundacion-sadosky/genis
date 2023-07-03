package profile

import configdata.MatchingRule
import play.api.libs.functional.syntax._
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._
import profile.GenotypificationByType._
import types._

object GenotypificationByType {
  type GenotypificationByType = Map[Int, Profile.Genotypification]

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
}

case class Profile(
  _id: SampleCode,
  globalCode: SampleCode,
  internalSampleCode: String,
  assignee: String,
  categoryId: AlphanumericId,
  genotypification: GenotypificationByType,
  analyses: Option[List[Analysis]],
  labeledGenotypification: Option[Profile.LabeledGenotypification],
  contributors: Option[Int],
  mismatches: Option[Profile.Mismatch],
  matchingRules: Option[Seq[MatchingRule]] = None,
  associatedTo: Option[List[SampleCode]] = None,
  deleted: Boolean = false,
  matcheable: Boolean = false,
  isReference: Boolean = true,
  processed: Boolean = false)

object Profile {

  type Marker = String
  type Genotypification = Map[Profile.Marker, List[AlleleValue]]
  type MixLabel = String
  type LabeledGenotypification = Map[Profile.MixLabel, Profile.Genotypification]
  type Mismatch = Map[String, Int]
  type LabelSets = Map[String, Map[String, Label]]

  implicit val profileReads: Reads[Profile] = (
    (__ \ "_id").read[SampleCode] and
    (__ \ "globalCode").read[SampleCode] and
    (__ \ "internalSampleCode").read[String] and
    (__ \ "assignee").read[String] and
    (__ \ "categoryId").read[AlphanumericId] and
    (__ \ "genotypification").read[GenotypificationByType] and
    (__ \ "analyses").readNullable[List[Analysis]] and
    (__ \ "labeledGenotypification").readNullable[Map[Profile.MixLabel, Profile.Genotypification]] and
    (__ \ "contributors").readNullable[Int] and
    (__ \ "mismatches").readNullable[Mismatch] and
    (__ \ "matchingRules").readNullable[Seq[MatchingRule]] and
    (__ \ "associatedTo").readNullable[List[SampleCode]] and
    (__ \ "deleted").read[Boolean] and
    (__ \ "matcheable").read[Boolean] and
      // or else because it was added later
    (__ \ "isReference").read[Boolean].orElse(Reads.pure(true)) and
    (__ \ "processed").read[Boolean].orElse(Reads.pure(false)))(Profile.apply _)

  implicit val profileWrites: OWrites[Profile] = (
    (JsPath \ "_id").write[SampleCode] and
    (JsPath \ "globalCode").write[SampleCode] and
    (JsPath \ "internalSampleCode").write[String] and
    (JsPath \ "assignee").write[String] and
    (JsPath \ "categoryId").write[AlphanumericId] and
    (JsPath \ "deleted").write[Boolean] and
    (JsPath \ "genotypification").write[GenotypificationByType] and
    (JsPath \ "analyses").write[Option[List[Analysis]]] and
    (JsPath \ "labeledGenotypification").write[Option[Map[Profile.MixLabel, Profile.Genotypification]]] and
    (JsPath \ "contributors").write[Option[Int]] and
    (JsPath \ "matchingRules").write[Option[Seq[MatchingRule]]] and
    (JsPath \ "mismatches").write[Option[Mismatch]] and
    (JsPath \ "associatedTo").writeNullable[List[SampleCode]] and
    (JsPath \ "matcheable").write[Boolean] and
    (JsPath \ "isReference").write[Boolean] and
    (JsPath \ "processed").write[Boolean]) { p: Profile =>
      (p.globalCode, p.globalCode, p.internalSampleCode, p.assignee, p.categoryId, p.deleted,
        p.genotypification, p.analyses, p.labeledGenotypification, p.contributors, p.matchingRules,
        p.mismatches, p.associatedTo, p.matcheable, p.isReference, p.processed)
    }

  implicit val profileFormat: OFormat[Profile] =
    OFormat(profileReads, profileWrites)

}



case class ProfileModelView(
  _id: Option[SampleCode],
  globalCode: Option[SampleCode],
  categoryId: Option[AlphanumericId],
  genotypification: Option[GenotypificationByType],
  analyses: List[AnalysisModelView],
  labeledGenotypification: Option[Profile.LabeledGenotypification],
  contributors: Option[Int],
  matchingRules: Option[Seq[MatchingRule]] = None,
  associable: Boolean,
  labelable: Boolean,
  editable: Boolean,
  isReference:Boolean,
  readOnly: Boolean,
  isUploadedToSuperior:Boolean
)

object ProfileModelView {
  implicit val mvf = Json.format[ProfileModelView]
}

