package pedigree

import matching.MatchStatus
import play.api.libs.json.*
import types.SampleCode

trait PedigreeMatchingProfile:
  val idPedigree: Long
  val unknown: NodeAlias
  val status: MatchStatus.Value
  val assignee: String
  val caseType: String
  val idCourtCase: Long

case class PedigreeInfo(
  idPedigree: Long,
  unknown: NodeAlias,
  assignee: String,
  status: MatchStatus.Value,
  caseType: String,
  idCourtCase: Long
) extends PedigreeMatchingProfile

object PedigreeInfo:
  implicit val format: Format[PedigreeInfo] = Json.format[PedigreeInfo]

case class PedigreeProfileInfo(
  idPedigree: Long,
  unknown: NodeAlias,
  globalCode: SampleCode,
  assignee: String,
  status: MatchStatus.Value,
  caseType: String,
  idCourtCase: Long
) extends PedigreeMatchingProfile

object PedigreeProfileInfo:
  implicit val format: Format[PedigreeProfileInfo] = Json.format[PedigreeProfileInfo]

object PedigreeMatchingProfile:

  implicit val writes: OWrites[PedigreeMatchingProfile] = new OWrites[PedigreeMatchingProfile]:
    def writes(pi: PedigreeMatchingProfile): JsObject = pi match
      case p: PedigreeInfo        => Json.toJson(p)(summon[Format[PedigreeInfo]]).as[JsObject]
      case p: PedigreeProfileInfo => Json.toJson(p)(summon[Format[PedigreeProfileInfo]]).as[JsObject]

  implicit val reads: Reads[PedigreeMatchingProfile] = new Reads[PedigreeMatchingProfile]:
    def reads(js: JsValue): JsResult[PedigreeMatchingProfile] =
      js \ "globalCode" match
        case _: JsUndefined => Json.fromJson(js)(summon[Format[PedigreeInfo]])
        case _              => Json.fromJson(js)(summon[Format[PedigreeProfileInfo]])

  def apply(idPedigree: Long, unknown: NodeAlias, assignee: String, status: MatchStatus.Value, caseType: String, idCourtCase: Long): PedigreeInfo =
    PedigreeInfo(idPedigree, unknown, assignee, status, caseType, idCourtCase)

  def apply(idPedigree: Long, unknown: NodeAlias, globalCode: SampleCode, assignee: String, status: MatchStatus.Value, caseType: String, idCourtCase: Long): PedigreeProfileInfo =
    PedigreeProfileInfo(idPedigree, unknown, globalCode, assignee, status, caseType, idCourtCase)
