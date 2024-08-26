package controllers

import javax.inject.{Inject, Singleton}

import inbox.{Notification, NotificationSearch, NotificationService}
import matching._
import play.api.Logger
import play.api.libs.EventSource
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Enumeratee
import play.api.libs.json._
import play.api.mvc.{Action, BodyParsers, Controller}

import scala.concurrent.Future

@Singleton
class Notifications @Inject() (notificationService: NotificationService, matchStatusService: MatchingProcessStatus) extends Controller {

  val logger: Logger = Logger(this.getClass())

  val toJson: Enumeratee[Notification, JsValue] = Enumeratee.map { Json.toJson(_) }

  val jason = (s:String) => Json.obj("status" -> s)

  val mjs2json: Enumeratee[MatchJobStatus, JsValue] = Enumeratee.map {
    case MatchJobStarted => jason("started")
    case MatchJobEndend  => jason("ended")
    case MatchJobFail    => jason("fail")
    case PedigreeMatchJobStarted => jason("pedigreeStarted")
    case PedigreeMatchJobEnded => jason("pedigreeEnded")
  }

  def delete(id: Long) = Action.async {
    notificationService.delete(id).map {
      case Right(result) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
      case Left(error) => BadRequest(Json.toJson(error))
    }
  }

  def changeFlag(id: Long, flag: Boolean) = Action.async {
    notificationService.changeFlag(id, flag).map {
      case Right(result) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.toString)
      case Left(error) => BadRequest(Json.toJson(error))
    }
  }

  def search = Action.async(BodyParsers.parse.json) { request =>
    val search = request.body.validate[NotificationSearch]

    search.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      input => {
        notificationService.search(input) map { notifs =>
          Ok(Json.toJson(notifs))
        }
      })

  }

  def count = Action.async(BodyParsers.parse.json) { request =>
    val search = request.body.validate[NotificationSearch]

    search.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      input => {
        notificationService.count(input) map { size =>
          Ok("").withHeaders("X-NOTIF-LENGTH" -> size.toString)
        }
      })

  }

  def getNotifications = Action { req =>
    val userId = req.session.get("X-USER").get
    Ok.feed(notificationService.getNotifications(userId)
      &> toJson
      &> EventSource()).as(EVENT_STREAM)
  }

  def getMatchNotifications = Action { req =>
    Ok.feed(matchStatusService.getJobStatus()
      &> mjs2json
      &> EventSource()).as(EVENT_STREAM)
  }

}