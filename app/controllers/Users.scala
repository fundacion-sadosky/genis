package controllers

import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.Controller
import play.api.mvc.Action
import play.api.libs.json.Json
import play.api.mvc.BodyParsers
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import user.SignupSolicitude
import play.api.libs.json.JsError
import user.UserService
import user.UserStatus
import scala.util.Try
import user.LdapUser
import services.SignupRequestKey
import user.UserView
import user.UserStatus
import user.RoleService

@Singleton
class Users @Inject() (userService: UserService) extends Controller with JsonActions {

  def signupRequest = JsonAction { solicitude => userService.signupRequest(solicitude) }

  def clearPassRequest = JsonAction { solicitude => userService.clearPassRequest(solicitude) }

  def signupConfirmation = JsonAction { confirmation => userService.signupConfirmation(confirmation) }

  def clearPassConfirmation = JsonAction { confirmation => userService.clearPassConfirmation(confirmation) }

  def listUsers = Action.async {
    userService.listAllUsers().map(result => Ok(Json.toJson(result)))
  }

  def setStatus(userId: String) = JsonAction { (status: UserStatus.Value) =>
    userService.setStatus(userId, status)
  }

  def updateUser = Action.async(BodyParsers.parse.json) { implicit request =>
    request.body.validate[UserView].fold(errors => {
      Future.successful(BadRequest(JsError.toFlatJson(errors)))
    }, user =>
      userService.updateUser(user).map { i =>
        Ok(Json.obj("status" -> i))
      })
  }


}
