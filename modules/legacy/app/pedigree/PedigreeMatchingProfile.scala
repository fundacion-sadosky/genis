package pedigree

import matching.MatchStatus
import play.api.libs.json.{JsResult, _}
import types.SampleCode

trait PedigreeMatchingProfile {
  val idPedigree: Long
  val unknown: NodeAlias
  val status: MatchStatus.Value
  val assignee: String
  val caseType: String
  val idCourtCase: Long
}

case class PedigreeInfo(idPedigree: Long, unknown: NodeAlias, assignee: String, status: MatchStatus.Value, caseType: String,idCourtCase: Long) extends PedigreeMatchingProfile

object PedigreeInfo {
  implicit val format: Format[PedigreeInfo] = Json.format[PedigreeInfo]
}

case class PedigreeProfileInfo(idPedigree: Long, unknown: NodeAlias, globalCode: SampleCode, assignee: String, status: MatchStatus.Value, caseType: String,idCourtCase: Long) extends PedigreeMatchingProfile

object PedigreeProfileInfo {
  implicit val format: Format[PedigreeProfileInfo] = Json.format[PedigreeProfileInfo]
}

object PedigreeMatchingProfile {

  implicit val writes: OWrites[PedigreeMatchingProfile] = new OWrites[PedigreeMatchingProfile] {
    def writes(pi: PedigreeMatchingProfile): JsObject = {
      pi match {
        case p: PedigreeInfo => Json.toJson(p)(implicitly[Format[PedigreeInfo]]).as[JsObject]
        case p: PedigreeProfileInfo => Json.toJson(p)(implicitly[Format[PedigreeProfileInfo]]).as[JsObject]
      }
    }
  }

  implicit val reads: Reads[PedigreeMatchingProfile] = new Reads[PedigreeMatchingProfile] {
    def reads(js: JsValue): JsResult[PedigreeMatchingProfile] = {
      js \ "globalCode" match {
        case j: JsUndefined => Json.fromJson(js)(implicitly[Format[PedigreeInfo]])
        case _ => Json.fromJson(js)(implicitly[Format[PedigreeProfileInfo]])
      }
    }
  }

  def unapply(pedigreeInfo: PedigreeInfo) = Some((pedigreeInfo.idPedigree, pedigreeInfo.unknown, pedigreeInfo.status, pedigreeInfo.caseType))

  def apply(idPedigree: Long, unknown: NodeAlias, assignee: String, status: MatchStatus.Value, caseType: String,idCourtCase: Long) =
    PedigreeInfo(idPedigree, unknown, assignee, status, caseType,idCourtCase)

  def unapply(pedigreeProfileInfo: PedigreeProfileInfo) =
    Some((pedigreeProfileInfo.idPedigree, pedigreeProfileInfo.unknown, pedigreeProfileInfo.globalCode, pedigreeProfileInfo.assignee, pedigreeProfileInfo.status, pedigreeProfileInfo.caseType))

  def apply(idPedigree: Long, unknown: NodeAlias, globalCode: SampleCode, assignee: String, status: MatchStatus.Value, caseType: String, idCourtCase: Long) =
    PedigreeProfileInfo(idPedigree, unknown, globalCode, assignee, status, caseType, idCourtCase)

}