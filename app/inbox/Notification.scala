package inbox

import java.util.Date

import pedigree.PedigreeMatchKind
import play.api.libs.json.{Json, Writes, _}
import types.SampleCode

case class Notification(
  id: Long,
  user: String,
  creationDate: Date,
  updateDate: Option[Date],
  flagged: Boolean,
  pending: Boolean,
  info: NotificationInfo
) {
  val description = info.description
  val url = info.url
  val kind = info.kind
}

trait NotificationInfo {
  val description: String
  val url: String
  val kind: NotificationType.Value
}

case class UserPendingInfo(userName: String) extends NotificationInfo {
  override val kind = NotificationType.userNotification
  override val description = s"El usuario: $userName está pendiente de aprobación"
  override val url = s"/users"
}

case class InferiorInstancePendingInfo(urlInstance: String) extends NotificationInfo {
  override val kind = NotificationType.inferiorInstancePending
  override val description = s"La instancia inferior: $urlInstance está pendiente de aprobación"
  override val url = s"/inferior-instances"
}

case class HitInfoInbox(globalCode: SampleCode,
                        matchedProfile: SampleCode,
                        matchingId: String) extends NotificationInfo {
  override val kind = NotificationType.hitMatch
  override val description = s"Se confirmó el match del perfil: ${matchedProfile.text} "
  override val url = s"/comparison/${globalCode.text}/matchedProfileId/${matchedProfile.text}/matchingId/$matchingId"
}

case class DiscardInfoInbox(globalCode: SampleCode,
                            matchedProfile: SampleCode,
                            matchingId: String) extends NotificationInfo {
  override val kind = NotificationType.discardMatch
  override val description = s"Se descartó el match del perfil: ${matchedProfile.text} "
  override val url = s"/comparison/${globalCode.text}/matchedProfileId/${matchedProfile.text}/matchingId/$matchingId"
}

case class DeleteProfileInfo(globalCode: SampleCode) extends NotificationInfo {
  override val kind = NotificationType.deleteProfile
  override val description = s"El perfil: ${globalCode.text} fue dado de baja"
  override val url = s"/profile/${globalCode.text}"
}

case class AprovedProfileInfo(globalCode: SampleCode) extends NotificationInfo {
  override val kind = NotificationType.aprovedProfile
  override val description = s"El perfil: ${globalCode.text} fue aprobado en la instancia superior"
  override val url = s"/search/profiledata"
}

case class RejectedProfileInfo(globalCode: SampleCode) extends NotificationInfo {
  override val kind = NotificationType.rejectedProfile
  override val description = s"El perfil: ${globalCode.text} fue rechazado en la instancia superior"
  override val url = s"/search/profiledata"
}

case class ProfileDataInfo(
    internalSampleCode: String,
    globalCode: SampleCode) extends NotificationInfo {
  override val kind = NotificationType.profileData
  override val description = s"Nuevo Perfil: ${globalCode.text} - $internalSampleCode"
  override val url = s"/profile/${globalCode.text}"
}

case class ProfileDataAssociationInfo(
   internalSampleCode: String,
   globalCode: SampleCode) extends NotificationInfo {
  override val kind = NotificationType.profileDataAssociation
  override val description = s"Perfil para Asociación: ${globalCode.text} - $internalSampleCode"
  override val url = s"/profile/${globalCode.text}"
}

case class ProfileUploadedInfo(globalCode: SampleCode) extends NotificationInfo {
  override val kind = NotificationType.profileUploaded
  override val description = s"El perfil: ${globalCode.text} proveniente de una instancia inferior esta pendiente de aprobación"
  override val url = s"/profile-approval"
}

case class MatchingInfo(
   globalCode: SampleCode,
   matchedProfile: SampleCode,
   matchingId: String) extends NotificationInfo {
  override val kind = NotificationType.matching
  override val description = s"Nueva coincidencia pendiente entre: ${globalCode.text} y ${matchedProfile.text}"
  override val url = s"/comparison/${globalCode.text}/matchedProfileId/${matchedProfile.text}/matchingId/$matchingId"
}

case class PedigreeMatchingInfo(
   matchedProfile: SampleCode,
   caseType: Option[String]= None,
   courtCaseId: Option[String]= None ) extends NotificationInfo {

  override val kind = NotificationType.pedigreeMatching

  override val description = s"Nueva coincidencia de búsqueda de personas (${caseType.getOrElse("")}) para el perfil: ${matchedProfile.text}"
  override val url = if(caseType.get.equals("DVI")) {
  s"/court-case/${courtCaseId.getOrElse("")}?tab=6"
  }else{
  s"pedigreeMatchesGroups/groups.html?g=profile&p=${matchedProfile.text}"
  }
  }

case class BulkUploadInfo(
   protoProfileId: String,
   sampleName: String) extends NotificationInfo {
  override val kind = NotificationType.bulkImport
  val description = s"Nuevo perfil para importación: $sampleName"
  val url = s"/profiles/bulkupload-step2/protoprofile/$protoProfileId"
}

case class PedigreeLRInfo(
    pedigreeId: Long,
    courtCaseId: Long,
    scenarioName: String) extends NotificationInfo {
  override val kind = NotificationType.pedigreeLR
  override val description = s"El escenario $scenarioName finalizó el cálculo del LR"
  override val url = s"/pedigree/$courtCaseId/$pedigreeId?s=$scenarioName"
}

case class CollapsingInfo(courtCaseId: Long,
                          result:Boolean) extends NotificationInfo {
  override val kind = NotificationType.collapsing
  override val description = if(result){s"Finalizó la búsqueda de agrupaciones para el caso $courtCaseId "}else{
    s"No se encontraron agrupaciones para el caso $courtCaseId"
  }

  override val url = s"/court-case/$courtCaseId/?tab=5"
}

case class PedigreeConsistencyInfo(courtCaseId: Long,
                                   pedigreeId: Long,
                                   pedigreeName:String) extends NotificationInfo {
  override val kind = NotificationType.pedigreeConsistency
  override val description = s"Finalizó el chequeo de consistencia para el pedigrí $pedigreeName"

  override val url = s"/pedigree-consistency/$courtCaseId/$pedigreeId"
}

object NotificationInfo {
  implicit val userPendingFormat = Json.format[UserPendingInfo]
  implicit val profileDataFormat = Json.format[ProfileDataInfo]
  implicit val profileDataAssociationFormat = Json.format[ProfileDataAssociationInfo]
  implicit val matchingFormat = Json.format[MatchingInfo]
  implicit val bulkUploadFormat = Json.format[BulkUploadInfo]
  implicit val pedigreeMatchingFormat = Json.format[PedigreeMatchingInfo]
  implicit val pedigreeLRFormat = Json.format[PedigreeLRInfo]
  implicit val inferiorInstancePendingFormat = Json.format[InferiorInstancePendingInfo]
  implicit val hitMatchFormat = Json.format[HitInfoInbox]
  implicit val discardMatchFormat = Json.format[DiscardInfoInbox]
  implicit val deleteProfileFormat = Json.format[DeleteProfileInfo]
  implicit val collapsingFormat = Json.format[CollapsingInfo]
  implicit val pedigreeConsistencyFormat = Json.format[PedigreeConsistencyInfo]
  implicit val profileUploadedFormat = Json.format[ProfileUploadedInfo]
  implicit val aprovedProfileFormat = Json.format[AprovedProfileInfo]
  implicit val rejectedProfileFormat = Json.format[RejectedProfileInfo]


  def unapply(info: NotificationInfo): Option[(NotificationType.Value, JsValue)] = {
    info match {
      case x: UserPendingInfo => Some((x.kind, Json.toJson(x)(userPendingFormat)))
      case x: ProfileDataInfo => Some((x.kind, Json.toJson(x)(profileDataFormat)))
      case x: ProfileDataAssociationInfo => Some((x.kind, Json.toJson(x)(profileDataAssociationFormat)))
      case x: MatchingInfo => Some((x.kind, Json.toJson(x)(matchingFormat)))
      case x: BulkUploadInfo => Some((x.kind, Json.toJson(x)(bulkUploadFormat)))
      case x: PedigreeMatchingInfo => Some((x.kind, Json.toJson(x)(pedigreeMatchingFormat)))
      case x: PedigreeLRInfo => Some((x.kind, Json.toJson(x)(pedigreeLRFormat)))
      case x: InferiorInstancePendingInfo => Some((x.kind, Json.toJson(x)(inferiorInstancePendingFormat)))
      case x: HitInfoInbox => Some((x.kind, Json.toJson(x)(hitMatchFormat)))
      case x: DiscardInfoInbox => Some((x.kind, Json.toJson(x)(discardMatchFormat)))
      case x: DeleteProfileInfo => Some((x.kind, Json.toJson(x)(deleteProfileFormat)))
      case x: CollapsingInfo => Some((x.kind, Json.toJson(x)(collapsingFormat)))
      case x: PedigreeConsistencyInfo => Some((x.kind, Json.toJson(x)(pedigreeConsistencyFormat)))
      case x: ProfileUploadedInfo => Some((x.kind, Json.toJson(x)(profileUploadedFormat)))
      case x: AprovedProfileInfo => Some((x.kind, Json.toJson(x)(aprovedProfileFormat)))
      case x: RejectedProfileInfo => Some((x.kind, Json.toJson(x)(rejectedProfileFormat)))

      case _ => None
    }
  }

  def apply(kind: NotificationType.Value, json: JsValue): NotificationInfo = {
    (kind match {
    case NotificationType.matching => Json.fromJson[MatchingInfo](json)
    case NotificationType.profileDataAssociation => Json.fromJson[ProfileDataAssociationInfo](json)
    case NotificationType.profileData => Json.fromJson[ProfileDataInfo](json)
    case NotificationType.bulkImport => Json.fromJson[BulkUploadInfo](json)
    case NotificationType.userNotification => Json.fromJson[UserPendingInfo](json)
    case NotificationType.pedigreeMatching => Json.fromJson[PedigreeMatchingInfo](json)
    case NotificationType.pedigreeLR => Json.fromJson[PedigreeLRInfo](json)
    case NotificationType.inferiorInstancePending => Json.fromJson[InferiorInstancePendingInfo](json)
    case NotificationType.hitMatch => Json.fromJson[HitInfoInbox](json)
    case NotificationType.discardMatch => Json.fromJson[DiscardInfoInbox](json)
    case NotificationType.deleteProfile => Json.fromJson[DeleteProfileInfo](json)
    case NotificationType.collapsing => Json.fromJson[CollapsingInfo](json)
    case NotificationType.pedigreeConsistency => Json.fromJson[PedigreeConsistencyInfo](json)
    case NotificationType.profileUploaded => Json.fromJson[ProfileUploadedInfo](json)
    case NotificationType.aprovedProfile => Json.fromJson[AprovedProfileInfo](json)
    case NotificationType.rejectedProfile => Json.fromJson[RejectedProfileInfo](json)

    case _ => JsError()
    }).get
  }

  implicit val writes = new Writes[NotificationInfo] {
    def writes(ni: NotificationInfo): JsValue = {
      NotificationInfo.unapply(ni).get._2
    }
  }
}

object Notification {
  implicit val writes = new Writes[Notification] {
    def writes(ni: Notification): JsValue = {
      Json.obj(
        "id" -> ni.id,
        "user" -> ni.user,
        "kind" -> ni.kind,
        "creationDate" -> ni.creationDate,
        "updateDate" -> ni.updateDate,
        "flagged" -> ni.flagged,
        "pending" -> ni.pending,
        "description" -> ni.description,
        "url" -> ni.url
      )
    }
  }
}