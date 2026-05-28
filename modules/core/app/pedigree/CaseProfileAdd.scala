package pedigree

import play.api.libs.json.Json
import _root_.util.PlayEnumUtils

// ---------------------------------------------------------------------------
// CaseProfileAdd — request to associate a profile to a court case.
// ---------------------------------------------------------------------------

case class CaseProfileAdd(
  courtcaseId: Long,
  globalCode: String,
  profileType: Option[String],
  groupedBy: Option[String] = None
)

object CaseProfileAdd:
  implicit val format: play.api.libs.json.OFormat[CaseProfileAdd] = Json.format[CaseProfileAdd]

case class AssociateProfile(
  profiles: List[CaseProfileAdd],
  isReference: Boolean
)

object AssociateProfile:
  implicit val format: play.api.libs.json.OFormat[AssociateProfile] = Json.format[AssociateProfile]

// ---------------------------------------------------------------------------
// CollapsingStatus — whether a profile in a court case is collapsed or not.
// ---------------------------------------------------------------------------

object CollapsingStatus extends Enumeration:
  type CollapsingStatus = Value
  val Active, Collapsed = Value

  implicit val enumTypeFormat: play.api.libs.json.Format[CollapsingStatus.Value] =
    PlayEnumUtils.enumFormat(CollapsingStatus)
