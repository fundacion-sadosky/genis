package inbox

import types.SampleCode

// TODO: Agregar campo `kind: NotificationType` cuando se migre el módulo de notificaciones completo
trait NotificationInfo:
  val description: String
  val url: String

case class UserPendingInfo(userName: String) extends NotificationInfo:
  override val description: String = s"El usuario: $userName está pendiente de aprobación"
  override val url: String = "/users"

case class ProfileDataInfo(internalSampleCode: String, globalCode: SampleCode) extends NotificationInfo:
  override val description: String = s"Perfil $globalCode creado"
  override val url: String = s"/profiles/$globalCode"

case class ProfileDataAssociationInfo(internalSampleCode: String, globalCode: SampleCode) extends NotificationInfo:
  override val description: String = s"Asociación de perfil $globalCode"
  override val url: String = s"/profiles/$globalCode"

case class BulkUploadInfo(protoProfileId: String, sampleName: String) extends NotificationInfo:
  override val description: String = s"Nuevo perfil para importación: $sampleName"
  override val url: String = s"/profiles/bulkupload-step2/protoprofile/$protoProfileId"

case class PedigreeMatchingInfo(
  matchedProfile: SampleCode,
  caseType: Option[String] = None,
  courtCaseId: Option[String] = None
) extends NotificationInfo:
  override val description: String =
    s"Nueva coincidencia de búsqueda de personas (${caseType.getOrElse("")}) " +
      s"para el perfil: ${matchedProfile.text}"
  override val url: String =
    if caseType.contains("DVI") then
      s"/court-case/${courtCaseId.getOrElse("")}?tab=6"
    else
      s"pedigreeMatchesGroups/groups.html?g=profile&p=${matchedProfile.text}"

case class PedigreeLRInfo(
  pedigreeId: Long,
  courtCaseId: Long,
  scenarioName: String
) extends NotificationInfo:
  override val description: String = s"El escenario $scenarioName finalizó el cálculo del LR"
  override val url: String = s"/pedigree/$courtCaseId/$pedigreeId?s=$scenarioName"

trait NotificationService:
  def push(userId: String, info: NotificationInfo): Unit
  def solve(userId: String, info: NotificationInfo): Unit

class NoOpNotificationService extends NotificationService:
  override def push(userId: String, info: NotificationInfo): Unit = ()
  override def solve(userId: String, info: NotificationInfo): Unit = ()
