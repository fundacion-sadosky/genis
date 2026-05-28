package controllers

import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.stream.Materializer
import play.api.Logger
import play.api.libs.json.*
import play.api.mvc.*

import inbox.{NotificationSearch, NotificationService}
import matching.{MatchingProcessStatus, MatchJobStarted, MatchJobEndend, MatchJobFail, PedigreeMatchJobStarted, PedigreeMatchJobEnded}

@Singleton
class NotificationsController @Inject() (
  notificationService: NotificationService,
  matchStatusService: MatchingProcessStatus,
  val controllerComponents: ControllerComponents
)(using ec: ExecutionContext, mat: Materializer) extends BaseController:

  private val logger = Logger(this.getClass)

  def search = Action.async(parse.json) { request =>
    request.body.validate[NotificationSearch].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      input  => notificationService.search(input).map(notifs => Ok(Json.toJson(notifs)))
    )
  }

  def count = Action.async(parse.json) { request =>
    request.body.validate[NotificationSearch].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      input  => notificationService.count(input).map(n =>
        Ok("").withHeaders("X-NOTIF-LENGTH" -> n.toString)
      )
    )
  }

  def delete(id: Long) = Action.async {
    notificationService.delete(id).map {
      case Right(_)    => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
      case Left(error) => BadRequest(Json.toJson(error))
    }
  }

  def changeFlag(id: Long, flag: Boolean) = Action.async {
    notificationService.changeFlag(id, flag).map {
      case Right(_)    => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
      case Left(error) => BadRequest(Json.toJson(error))
    }
  }

  def changePending(id: Long, pending: Boolean) = Action {
    notificationService.changePending(id, pending) // fire-and-forget, igual que legacy
    Ok
  }

  def getNotifications = Action { (req: Request[AnyContent]) =>
    val userId = req.session.get("X-USER").getOrElse("ANONYMOUS")
    val source = notificationService.getNotifications(userId)
      .map(n => s"data: ${Json.toJson(n).toString}\n\n")
    Ok.chunked(source)
      .as("text/event-stream")
      .withHeaders(
        "Cache-Control"     -> "no-cache",
        "X-Accel-Buffering" -> "no"
      )
  }

  def getMatchNotifications = Action { (_: Request[AnyContent]) =>
    val source = matchStatusService.getJobStatus().map { status =>
      val payload = status match
        case MatchJobStarted         => """{"status":"started"}"""
        case MatchJobEndend          => """{"status":"ended"}"""
        case MatchJobFail            => """{"status":"fail"}"""
        case PedigreeMatchJobStarted => """{"status":"pedigreeStarted"}"""
        case PedigreeMatchJobEnded   => """{"status":"pedigreeEnded"}"""
      s"data: $payload\n\n"
    }
    Ok.chunked(source)
      .as("text/event-stream")
      .withHeaders(
        "Cache-Control"     -> "no-cache",
        "X-Accel-Buffering" -> "no"
      )
  }