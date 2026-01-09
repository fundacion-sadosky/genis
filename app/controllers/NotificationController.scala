package controllers

import models.notification.{NotificationSearch, NotificationType}
import services.NotificationService
import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class NotificationController @Inject() (
  cc: ControllerComponents,
  notificationService: NotificationService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  /**
   * GET /notifications
   * Obtiene todas las notificaciones del usuario actual
   */
  def getNotifications: Action[AnyContent] = Action.async { request =>
    val userId = request.session.get("userId").getOrElse("unknown")
    notificationService.getNotifications(userId).map { notifications =>
      Ok(Json.toJson(notifications))
    }.recover { case ex =>
      InternalServerError(Json.obj("error" -> ex.getMessage))
    }
  }

  /**
   * POST /notifications/search
   * Búsqueda avanzada de notificaciones
   * Body: { notificationType?, pending?, flagged?, page?, pageSize? }
   */
  def search: Action[JsValue] = Action.async(parse.json) { request =>
    val userId = request.session.get("userId").getOrElse("unknown")
    
    try {
      val notificationType = (request.body \ "notificationType").asOpt[String]
        .flatMap(t => try Some(NotificationType.withName(t)) catch { case _: Exception => None })
      val pending = (request.body \ "pending").asOpt[Boolean]
      val flagged = (request.body \ "flagged").asOpt[Boolean]
      val page = (request.body \ "page").asOpt[Int].getOrElse(0)
      val pageSize = (request.body \ "pageSize").asOpt[Int].getOrElse(25)

      val search = NotificationSearch(
        userId = Some(userId),
        notificationType = notificationType,
        pending = pending,
        flagged = flagged,
        page = page,
        pageSize = pageSize
      )

      notificationService.searchNotifications(search).flatMap { notifications =>
        notificationService.countSearchResults(search).map { count =>
          Ok(Json.obj(
            "data" -> Json.toJson(notifications),
            "total" -> count
          ))
        }
      }
    } catch {
      case ex: Exception =>
        scala.concurrent.Future.successful(
          BadRequest(Json.obj("error" -> ex.getMessage))
        )
    }
  }

  /**
   * POST /notifications/total
   * Cuenta notificaciones pendientes del usuario
   */
  def count: Action[AnyContent] = Action.async { request =>
    val userId = request.session.get("userId").getOrElse("unknown")
    notificationService.countPendingNotifications(userId).map { count =>
      Ok(Json.obj("count" -> count))
    }.recover { case ex =>
      InternalServerError(Json.obj("error" -> ex.getMessage))
    }
  }

  /**
   * POST /notifications/:id/read
   * Marca una notificación como leída
   */
  def markAsRead(id: Long): Action[AnyContent] = Action.async { request =>
    notificationService.markAsRead(id).map { result =>
      if (result > 0) Ok(Json.obj("success" -> true))
      else NotFound(Json.obj("error" -> "Notification not found"))
    }.recover { case ex =>
      InternalServerError(Json.obj("error" -> ex.getMessage))
    }
  }

  /**
   * POST /notifications/:id/flag?flag=true
   * Marca/desmarca una notificación
   */
  def toggleFlag(id: Long, flag: Boolean): Action[AnyContent] = Action.async { request =>
    notificationService.toggleFlag(id, flag).map { result =>
      if (result > 0) Ok(Json.obj("success" -> true))
      else NotFound(Json.obj("error" -> "Notification not found"))
    }.recover { case ex =>
      InternalServerError(Json.obj("error" -> ex.getMessage))
    }
  }

  /**
   * DELETE /notifications/:id
   * Elimina una notificación
   */
  def delete(id: Long): Action[AnyContent] = Action.async { request =>
    notificationService.deleteNotification(id).map { result =>
      if (result > 0) Ok(Json.obj("success" -> true))
      else NotFound(Json.obj("error" -> "Notification not found"))
    }.recover { case ex =>
      InternalServerError(Json.obj("error" -> ex.getMessage))
    }
  }
}
