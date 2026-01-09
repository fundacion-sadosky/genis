package services

import models.notification.{Notification, NotificationSearch, NotificationType}
import repositories.NotificationRepository
import javax.inject.{Inject, Singleton}
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotificationService @Inject() (
  notificationRepository: NotificationRepository
)(implicit ec: ExecutionContext) {

  /**
   * Crea una nueva notificación
   */
  def createNotification(
    userId: String,
    notificationType: NotificationType.Value,
    title: String,
    description: String,
    url: Option[String] = None,
    metadata: Option[String] = None
  ): Future[Notification] = {
    val notification = Notification(
      id = 0,
      userId = userId,
      createdAt = LocalDateTime.now(),
      updatedAt = None,
      flagged = false,
      pending = true,
      notificationType = notificationType,
      title = title,
      description = description,
      url = url.getOrElse(""),
      metadata = metadata
    )
    notificationRepository.create(notification)
  }

  /**
   * Obtiene todas las notificaciones de un usuario
   */
  def getNotifications(userId: String): Future[Seq[Notification]] = {
    notificationRepository.findByUserId(userId)
  }

  /**
   * Búsqueda avanzada de notificaciones
   */
  def searchNotifications(search: NotificationSearch): Future[Seq[Notification]] = {
    notificationRepository.search(search)
  }

  /**
   * Cuenta notificaciones pendientes de un usuario
   */
  def countPendingNotifications(userId: String): Future[Int] = {
    notificationRepository.countPending(userId)
  }

  /**
   * Cuenta notificaciones según criterios de búsqueda
   */
  def countSearchResults(search: NotificationSearch): Future[Int] = {
    notificationRepository.countSearch(search)
  }

  /**
   * Marca una notificación como leída (pending = false)
   */
  def markAsRead(id: Long): Future[Int] = {
    notificationRepository.findById(id).flatMap {
      case Some(notification) =>
        notificationRepository.updateFlags(id, notification.flagged, false)
      case None => Future.successful(0)
    }
  }

  /**
   * Cambia el estado de flag (marcado/desmarcado)
   */
  def toggleFlag(id: Long, flagged: Boolean): Future[Int] = {
    notificationRepository.findById(id).flatMap {
      case Some(notification) =>
        notificationRepository.updateFlags(id, flagged, notification.pending)
      case None => Future.successful(0)
    }
  }

  /**
   * Elimina una notificación
   */
  def deleteNotification(id: Long): Future[Int] = {
    notificationRepository.delete(id)
  }

  /**
   * Elimina todas las notificaciones de un usuario
   */
  def deleteAllNotifications(userId: String): Future[Int] = {
    notificationRepository.deleteByUserId(userId)
  }
}
