package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, ControllerComponents, Action}
import play.api.libs.json.{Json, JsError}
import scala.concurrent.{ExecutionContext, Future}
import services.UserService
import user.{SignupSolicitude, ClearPassSolicitud, SignupChallenge, ClearPassChallenge,
  SignupResponse, ClearPassResponse, UserView, UserStatus}

@Singleton
class UserController @Inject()(
    cc: ControllerComponents,
    userService: UserService
)(using ec: ExecutionContext) extends AbstractController(cc):

  def signupRequest = Action.async(parse.json) { request =>
    request.body.validate[SignupSolicitude].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      solicitude => userService.signupRequest(solicitude).map {
        case Left(err) => BadRequest(Json.toJson(err))
        case Right(response) => Ok(Json.toJson(response))
      }
    )
  }

  def clearPassRequest = Action.async(parse.json) { request =>
    request.body.validate[ClearPassSolicitud].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      solicitude => userService.clearPassRequest(solicitude).map {
        case Left(err) => BadRequest(Json.toJson(err))
        case Right(response) => Ok(Json.toJson(response))
      }
    )
  }

  def signupConfirmation = Action.async(parse.json) { request =>
    request.body.validate[SignupChallenge].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      confirmation => userService.signupConfirmation(confirmation).map {
        case Left(err) => BadRequest(Json.toJson(err))
        case Right(v)  => Ok(Json.toJson(v))
      }
    )
  }

  def clearPassConfirmation = Action.async(parse.json) { request =>
    request.body.validate[ClearPassChallenge].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      confirmation => userService.clearPassConfirmation(confirmation).map {
        case Left(err) => BadRequest(Json.toJson(err))
        case Right(v)  => Ok(Json.toJson(v))
      }
    )
  }

  def listUsers = Action.async {
    userService.listAllUsers().map(users => Ok(Json.toJson(users)))
  }

  def setStatus(userId: String) = Action.async(parse.json) { request =>
    request.body.validate[UserStatus].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      newStatus => userService.setStatus(userId, newStatus).map {
        case Left(err) => BadRequest(Json.toJson(err))
        case Right(v)  => Ok(Json.toJson(v))
      }
    )
  }

  def updateUser = Action.async(parse.json) { request =>
    request.body.validate[UserView].fold(
      errors => Future.successful(BadRequest(JsError.toJson(errors))),
      user => userService.updateUser(user).map(res => Ok(Json.obj("status" -> res)))
    )
  }
