package inbox

import java.util.Date
import play.api.Logger
import play.api.libs.json.*
import types.SampleCode

// -----------------------------------------------------------------------
// Domain model
// -----------------------------------------------------------------------

case class Notification(
  id: Long,
  user: String,
  creationDate: Date,
  updateDate: Option[Date],
  flagged: Boolean,
  pending: Boolean,
  info: NotificationInfo
):
  val description: String    = info.description
  val url: String            = info.url
  val kind: NotificationType = info.kind

object Notification:
  given writes: Writes[Notification] = Writes { n =>
    Json.obj(
      "id"           -> n.id,
      "user"         -> n.user,
      "kind"         -> n.kind,
      "creationDate" -> n.creationDate,
      "updateDate"   -> n.updateDate,
      "flagged"      -> n.flagged,
      "pending"      -> n.pending,
      "description"  -> n.description,
      "url"          -> n.url,
      "info"         -> Json.toJson(n.info)
    )
  }

// -----------------------------------------------------------------------
// NotificationInfo trait + subtypes (idénticos al legacy)
// -----------------------------------------------------------------------

trait NotificationInfo:
  val description: String
  val url: String
  val kind: NotificationType

case class UserPendingInfo(userName: String) extends NotificationInfo:
  override val kind        = NotificationType.userNotification
  override val description = s"El usuario: $userName está pendiente de aprobación"
  override val url         = "/users"

case class InferiorInstancePendingInfo(urlInstance: String) extends NotificationInfo:
  override val kind        = NotificationType.inferiorInstancePending
  override val description = s"La instancia inferior: $urlInstance está pendiente de aprobación"
  override val url         = "/inferior-instances"

case class HitInfoFormat(
  globalCode: SampleCode, matchedProfile: SampleCode, matchingId: String, userName: String
) extends NotificationInfo:
  override val kind        = NotificationType.hitMatch
  override val description = s"El usuario $userName confirmó el match del perfil: ${matchedProfile.text} "
  override val url         = s"/comparison/${globalCode.text}/matchedProfileId/${matchedProfile.text}/matchingId/$matchingId"

case class DiscardInfoFormat(
  globalCode: SampleCode, matchedProfile: SampleCode, matchingId: String, userName: String
) extends NotificationInfo:
  override val kind        = NotificationType.discardMatch
  override val description = s"El usuario $userName descartó el match del perfil: ${matchedProfile.text} "
  override val url         = s"/comparison/${globalCode.text}/matchedProfileId/${matchedProfile.text}/matchingId/$matchingId"

case class DeleteProfileInfo(globalCode: SampleCode) extends NotificationInfo:
  override val kind        = NotificationType.deleteProfile
  override val description = s"El perfil: ${globalCode.text} fue dado de baja"
  override val url         = s"/profile/${globalCode.text}"

case class DeleteProfileInInferiorInstanceInfo(
  globalCode: SampleCode, userName: String, operationOriginatedInInstance: String
) extends NotificationInfo:
  override val kind        = NotificationType.deletedProfileInInferiorInstance
  override val description = s"El perfil: ${globalCode.text} fue dado de baja en la instancia inferior: $operationOriginatedInInstance por el usuario: $userName"
  override val url         = s"/trace/${globalCode.text}"

case class DeleteProfileInSuperiorInstanceInfo(
  globalCode: SampleCode, userName: String, operationOriginatedInInstance: String
) extends NotificationInfo:
  override val kind        = NotificationType.deletedProfileInSuperiorInstance
  override val description = s"El perfil: ${globalCode.text} fue dado de baja en la instancia superior: $operationOriginatedInInstance por el usuario: $userName"
  override val url         = s"/trace/${globalCode.text}"

case class ApprovedProfileInfo(
  globalCode: SampleCode, userName: String, isCategoryModification: Option[Boolean] = Some(false)
) extends NotificationInfo:
  override val kind = NotificationType.aprovedProfile
  override val description =
    if isCategoryModification.getOrElse(false) then
      s"El cambio de categoría del perfil ${globalCode.text} fue aceptado en la instancia superior"
    else
      s"El perfil: ${globalCode.text} fue aprobado en la instancia superior por el usuario: $userName"
  override val url = s"/trace/${globalCode.text}"

case class RejectedProfileInfo(
  globalCode: SampleCode, userName: String, isCategoryModification: Option[Boolean]
) extends NotificationInfo:
  override val kind = NotificationType.rejectedProfile
  override val description =
    if isCategoryModification.getOrElse(false) then
      s"El cambio de categoría del perfil ${globalCode.text} fue rechazado en la instancia superior" + s"por el usuario: $userName"
    else
      s"El perfil: ${globalCode.text} fue rechazado en la instancia superior" + s"por el usuario: $userName"
  override val url = s"/trace/${globalCode.text}"

case class ProfileDataInfo(internalSampleCode: String, globalCode: SampleCode) extends NotificationInfo:
  override val kind        = NotificationType.profileData
  override val description = s"Nuevo Perfil: ${globalCode.text} - $internalSampleCode"
  override val url         = s"/profile/${globalCode.text}"

case class ProfileDataAssociationInfo(internalSampleCode: String, globalCode: SampleCode) extends NotificationInfo:
  override val kind        = NotificationType.profileDataAssociation
  override val description = s"Perfil para Asociación: ${globalCode.text} - $internalSampleCode"
  override val url         = s"/profile/${globalCode.text}"

case class ProfileUploadedInfo(globalCode: SampleCode) extends NotificationInfo:
  override val kind        = NotificationType.profileUploaded
  override val description = s"El perfil: ${globalCode.text} proveniente de una instancia inferior esta pendiente de aprobación"
  override val url         = "/profile-approval"

case class CategoryChangeInfo(globalCode1: SampleCode, cat1: String, cat2: String) extends NotificationInfo:
  override val kind        = NotificationType.profileChangeCategory
  override val description = s"El cambio de categoría del perfil ${globalCode1.text} de $cat1 a $cat2 está pendiente de aprobación"
  override val url         = "/profile-approval"

case class MatchingInfo(
  globalCode: SampleCode, matchedProfile: SampleCode, matchingId: String, isDesktop: Boolean
) extends NotificationInfo:
  override val kind = NotificationType.matching
  override val description: String =
    if isDesktop then "Coincidencia con perfil de escritorio"
    else s"Nueva coincidencia pendiente entre: ${globalCode.text} y ${matchedProfile.text}"
  override val url: String =
    if isDesktop then "/profiles/bulkupload-step1"
    else s"/comparison/${globalCode.text}/matchedProfileId/${matchedProfile.text}/matchingId/$matchingId"

case class MatchingHit(
  globalCode: SampleCode, matchedProfile: SampleCode, matchingId: String, userName: String
) extends NotificationInfo:
  override val kind        = NotificationType.hitMatch
  override val description = s"Coincidencia validada entre: ${globalCode.text} y ${matchedProfile.text} por el usuario: $userName"
  override val url         = s"/comparison/${globalCode.text}/matchedProfileId/${matchedProfile.text}/matchingId/$matchingId"

case class MatchingDiscard(
  globalCode: SampleCode, matchedProfile: SampleCode, matchingId: String, userName: String
) extends NotificationInfo:
  override val kind        = NotificationType.discardMatch
  override val description = s"Coincidencia descartada entre: ${globalCode.text} y ${matchedProfile.text} por el usuario: $userName"
  override val url         = s"/comparison/${globalCode.text}/matchedProfileId/${matchedProfile.text}/matchingId/$matchingId"

case class BulkUploadInfo(protoProfileId: String, sampleName: String) extends NotificationInfo:
  override val kind        = NotificationType.bulkImport
  override val description = s"Nuevo perfil para importación: $sampleName"
  override val url         = s"/profiles/bulkupload-step2/protoprofile/$protoProfileId"

case class PedigreeMatchingInfo(
  matchedProfile: SampleCode, caseType: Option[String] = None, courtCaseId: Option[String] = None
) extends NotificationInfo:
  override val kind        = NotificationType.pedigreeMatching
  override val description = s"Nueva coincidencia de búsqueda de personas (${caseType.getOrElse("")}) para el perfil: ${matchedProfile.text}"
  override val url: String =
    if caseType.contains("DVI") then s"/court-case/${courtCaseId.getOrElse("")}?tab=6"
    else s"pedigreeMatchesGroups/groups.html?g=profile&p=${matchedProfile.text}"

case class PedigreeLRInfo(pedigreeId: Long, courtCaseId: Long, scenarioName: String) extends NotificationInfo:
  override val kind        = NotificationType.pedigreeLR
  override val description = s"El escenario $scenarioName finalizó el cálculo del LR"
  override val url         = s"/pedigree/$courtCaseId/$pedigreeId?s=$scenarioName"

case class CollapsingInfo(courtCaseId: Long, result: Boolean) extends NotificationInfo:
  override val kind        = NotificationType.collapsing
  override val description =
    if result then s"Finalizó la búsqueda de agrupaciones para el caso $courtCaseId "
    else s"No se encontraron agrupaciones para el caso $courtCaseId"
  override val url = s"/court-case/$courtCaseId/?tab=5"

case class PedigreeConsistencyInfo(courtCaseId: Long, pedigreeId: Long, pedigreeName: String) extends NotificationInfo:
  override val kind        = NotificationType.pedigreeConsistency
  override val description = s"Finalizó el chequeo de consistencia para el pedigrí $pedigreeName"
  override val url         = s"/pedigree-consistency/$courtCaseId/$pedigreeId"

// -----------------------------------------------------------------------
// JSON codecs for NotificationInfo
// -----------------------------------------------------------------------

object NotificationInfo:
  private val logger = Logger(this.getClass)

  given userPendingFormat: Format[UserPendingInfo]                                     = Json.format
  given profileDataFormat: Format[ProfileDataInfo]                                     = Json.format
  given profileDataAssociationFormat: Format[ProfileDataAssociationInfo]               = Json.format
  given matchingFormat: Format[MatchingInfo]                                           = Json.format
  given bulkUploadFormat: Format[BulkUploadInfo]                                      = Json.format
  given pedigreeMatchingFormat: Format[PedigreeMatchingInfo]                          = Json.format
  given pedigreeLRFormat: Format[PedigreeLRInfo]                                      = Json.format
  given inferiorInstancePendingFormat: Format[InferiorInstancePendingInfo]            = Json.format
  given hitInfoFormat: Format[HitInfoFormat]                                          = Json.format
  given hitMatchWrites: Writes[MatchingHit]                                           = Json.writes
  given discardMatchWrites: Writes[MatchingDiscard]                                   = Json.writes
  given discardInfoFormat: Format[DiscardInfoFormat]                                  = Json.format
  given deleteProfileFormat: Format[DeleteProfileInfo]                                = Json.format
  given collapsingFormat: Format[CollapsingInfo]                                      = Json.format
  given pedigreeConsistencyFormat: Format[PedigreeConsistencyInfo]                    = Json.format
  given profileUploadedFormat: Format[ProfileUploadedInfo]                            = Json.format
  given categoryChangeFormat: Format[CategoryChangeInfo]                              = Json.format
  given approvedProfileFormat: Format[ApprovedProfileInfo]                            = Json.format
  given rejectedProfileFormat: Format[RejectedProfileInfo]                            = Json.format
  given deletedInInferiorFormat: Format[DeleteProfileInInferiorInstanceInfo]          = Json.format
  given deletedInSuperiorFormat: Format[DeleteProfileInSuperiorInstanceInfo]          = Json.format

  def unapply(info: NotificationInfo): Option[(NotificationType, JsValue)] =
    info match
      case x: UserPendingInfo                     => Some((x.kind, Json.toJson(x)))
      case x: ProfileDataInfo                     => Some((x.kind, Json.toJson(x)))
      case x: ProfileDataAssociationInfo          => Some((x.kind, Json.toJson(x)))
      case x: MatchingInfo                        => Some((x.kind, Json.toJson(x)))
      case x: BulkUploadInfo                      => Some((x.kind, Json.toJson(x)))
      case x: PedigreeMatchingInfo                => Some((x.kind, Json.toJson(x)))
      case x: PedigreeLRInfo                      => Some((x.kind, Json.toJson(x)))
      case x: InferiorInstancePendingInfo         => Some((x.kind, Json.toJson(x)))
      case x: HitInfoFormat                       => Some((x.kind, Json.toJson(x)))
      case x: MatchingHit                         => Some((x.kind, Json.toJson(x)))
      case x: MatchingDiscard                     => Some((x.kind, Json.toJson(x)))
      case x: DiscardInfoFormat                   => Some((x.kind, Json.toJson(x)))
      case x: DeleteProfileInfo                   => Some((x.kind, Json.toJson(x)))
      case x: CollapsingInfo                      => Some((x.kind, Json.toJson(x)))
      case x: PedigreeConsistencyInfo             => Some((x.kind, Json.toJson(x)))
      case x: ProfileUploadedInfo                 => Some((x.kind, Json.toJson(x)))
      case x: CategoryChangeInfo                  => Some((x.kind, Json.toJson(x)))
      case x: ApprovedProfileInfo                 => Some((x.kind, Json.toJson(x)))
      case x: RejectedProfileInfo                 => Some((x.kind, Json.toJson(x)))
      case x: DeleteProfileInInferiorInstanceInfo => Some((x.kind, Json.toJson(x)))
      case x: DeleteProfileInSuperiorInstanceInfo => Some((x.kind, Json.toJson(x)))
      case _                                      => None

  /** Reconstruye un NotificationInfo desde la DB (kind + JSON almacenado). */
  def apply(kind: NotificationType, json: JsValue): NotificationInfo =
    val result: JsResult[NotificationInfo] = kind match
      case NotificationType.matching                          => Json.fromJson[MatchingInfo](json)
      case NotificationType.profileDataAssociation           => Json.fromJson[ProfileDataAssociationInfo](json)
      case NotificationType.profileData                      => Json.fromJson[ProfileDataInfo](json)
      case NotificationType.bulkImport                       => Json.fromJson[BulkUploadInfo](json)
      case NotificationType.userNotification                 => Json.fromJson[UserPendingInfo](json)
      case NotificationType.pedigreeMatching                 => Json.fromJson[PedigreeMatchingInfo](json)
      case NotificationType.pedigreeLR                       => Json.fromJson[PedigreeLRInfo](json)
      case NotificationType.inferiorInstancePending          => Json.fromJson[InferiorInstancePendingInfo](json)
      case NotificationType.hitMatch                         => Json.fromJson[HitInfoFormat](json)
      case NotificationType.discardMatch                     => Json.fromJson[DiscardInfoFormat](json)
      case NotificationType.deleteProfile                    => Json.fromJson[DeleteProfileInfo](json)
      case NotificationType.collapsing                       => Json.fromJson[CollapsingInfo](json)
      case NotificationType.pedigreeConsistency              => Json.fromJson[PedigreeConsistencyInfo](json)
      case NotificationType.profileUploaded                  => Json.fromJson[ProfileUploadedInfo](json)
      case NotificationType.aprovedProfile                   => Json.fromJson[ApprovedProfileInfo](json)
      case NotificationType.rejectedProfile                  => Json.fromJson[RejectedProfileInfo](json)
      case NotificationType.deletedProfileInSuperiorInstance => Json.fromJson[DeleteProfileInSuperiorInstanceInfo](json)
      case NotificationType.deletedProfileInInferiorInstance => Json.fromJson[DeleteProfileInInferiorInstanceInfo](json)
      case NotificationType.profileChangeCategory            => Json.fromJson[CategoryChangeInfo](json)
    result.fold(
      errors =>
        logger.error(s"Error parseando notificación '$kind': $errors")
        throw new RuntimeException(s"Error parseando notificación '$kind': $errors"),
      identity
    )

  given writes: Writes[NotificationInfo] = Writes { ni =>
    unapply(ni).map(_._2).getOrElse(JsNull)
  }
