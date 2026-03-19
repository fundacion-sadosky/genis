package inbox

import types.SampleCode
import play.api.libs.json.{Json, Writes}
import java.util.Date

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

case class HitInfoFormat(
  globalCode: SampleCode,
  matchedProfile: SampleCode,
  matchingId: String,
  userName: String
) extends NotificationInfo {
  override val kind = NotificationType.hitMatch
  override val description = s"El usuario ${userName} confirmó el match del perfil: ${matchedProfile.text} "
  override val url = s"/comparison/${globalCode.text}/matchedProfileId/${matchedProfile.text}/matchingId/$matchingId"
}

case class DiscardInfoFormat(globalCode: SampleCode,
                             matchedProfile: SampleCode,
                             matchingId: String,
                             userName: String) extends NotificationInfo {
  override val kind = NotificationType.discardMatch
  override val description = s"El usuario ${userName} descartó el match del perfil: ${matchedProfile.text} "
  override val url = s"/comparison/${globalCode.text}/matchedProfileId/${matchedProfile.text}/matchingId/$matchingId"
}

case class DeleteProfileInfo(globalCode: SampleCode) extends NotificationInfo {
  override val kind = NotificationType.deleteProfile
  override val description = s"El perfil: ${globalCode.text} fue dado de baja"
  override val url = s"/profile/${globalCode.text}"
}

case class DeleteProfileInInferiorInstanceInfo(globalCode: SampleCode, userName: String, operationOriginatedInInstance: String) extends NotificationInfo {
  override val kind = NotificationType.deletedProfileInInferiorInstance
  override val description = s"El perfil: ${globalCode.text} fue dado de baja en la instancia inferior: $operationOriginatedInInstance por el usuario: $userName"
  override val url = s"/trace/${globalCode.text}"
}
