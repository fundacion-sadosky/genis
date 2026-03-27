package inbox

// TODO: Agregar campo `kind: NotificationType` cuando se migre el módulo de notificaciones completo
trait NotificationInfo:
  val description: String
  val url: String

case class UserPendingInfo(userName: String) extends NotificationInfo:
  override val description: String = s"El usuario: $userName está pendiente de aprobación"
  override val url: String = "/users"

trait NotificationService:
  def push(userId: String, info: NotificationInfo): Unit
  def solve(userId: String, info: NotificationInfo): Unit

class NoOpNotificationService extends NotificationService:
  override def push(userId: String, info: NotificationInfo): Unit = ()
  override def solve(userId: String, info: NotificationInfo): Unit = ()
