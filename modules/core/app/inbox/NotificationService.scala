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

trait NotificationService:
  def push(userId: String, info: NotificationInfo): Unit
  def solve(userId: String, info: NotificationInfo): Unit

class NoOpNotificationService extends NotificationService:
  override def push(userId: String, info: NotificationInfo): Unit = ()
  override def solve(userId: String, info: NotificationInfo): Unit = ()
