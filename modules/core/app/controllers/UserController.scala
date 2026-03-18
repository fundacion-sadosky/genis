package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, ControllerComponents, Action, BodyParsers}
import play.api.libs.json.{Json, JsError, Writes}
import scala.concurrent.{ExecutionContext, Future}
import services.UserService
import user.UserStatus
import play.api.libs.json._

  // Implicit Writes for Either[String, Int]
  implicit val eitherWrites: Writes[Either[String, Int]] = new Writes[Either[String, Int]] {
    def writes(e: Either[String, Int]): JsValue = e match {
      case Left(err) => Json.obj("error" -> err)
      case Right(value) => Json.obj("value" -> value)
    }
  }

  // Implicit Writes for Any (fallback)
  implicit val anyWrites: Writes[Any] = new Writes[Any] {
    def writes(a: Any): JsValue = a match {
      case u: types.User => Json.toJson(u)
      case i: Int => JsNumber(i)
      case s: String => JsString(s)
      case b: Boolean => JsBoolean(b)
      case seq: Seq[_] => JsArray(seq.map(writes))
      case opt: Option[_] => opt.map(writes).getOrElse(JsNull)
      case _ => JsString(a.toString)
    }
  }
@Singleton
class UserController @Inject()(
    cc: ControllerComponents,
    userService: UserService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {


  def signupRequest = Action.async(parse.json) { request =>
    val solicitude = request.body // TODO: parse to correct type
    userService.signupRequest(solicitude).map {
      case Left(err)  => BadRequest(Json.obj("error" -> err))
      case Right(res: JsValue) => Ok(res)
      case Right(res) => Ok(Json.toJson(res)(anyWrites))
    }
  }

  def clearPassRequest = Action.async(parse.json) { request =>
    val solicitude = request.body // TODO: parse to correct type
    userService.clearPassRequest(solicitude).map {
      case Left(err)  => BadRequest(Json.obj("error" -> err))
      case Right(res: JsValue) => Ok(res)
      case Right(res) => Ok(Json.toJson(res)(anyWrites))
    }
  }

  def signupConfirmation = Action.async(parse.json) { request =>
    val confirmation = request.body // TODO: parse to correct type
    userService.signupConfirmation(confirmation).map(result => Ok(Json.toJson(result)(eitherWrites)))
  }

  def clearPassConfirmation = Action.async(parse.json) { request =>
    val confirmation = request.body // TODO: parse to correct type
    userService.clearPassConfirmation(confirmation).map(result => Ok(Json.toJson(result)(eitherWrites)))
  }

  def listUsers = Action.async {
    userService.listAllUsers().map(result => Ok(Json.toJson(result)(Writes.seq(anyWrites))))
  }

  def setStatus(userId: String) = Action.async(parse.json) { request =>
    val status = request.body.as[String] // TODO: parse to UserStatus
    userService.setStatus(userId, userStatusFromString(status)).map(result => Ok(Json.toJson(result)(eitherWrites)))
  }

  def updateUser = Action.async(parse.json) { request =>
    // TODO: parse to correct type
    userService.updateUser(request.body).map { i =>
      Ok(Json.obj("status" -> i))
    }
  }

  private def userStatusFromString(status: String): UserStatus = {
    // TODO: implement conversion
    UserStatus.active // placeholder
  }
}
