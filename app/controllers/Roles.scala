package controllers

import scala.concurrent.Future

import javax.inject.Inject
import javax.inject.Singleton
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.JsError
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Writes
import play.api.libs.json.__
import play.api.mvc.Action
import play.api.mvc.BodyParsers
import play.api.mvc.Controller
import security.StaticAuthorisationOperation
import types.Permission
import user.Role
import user.RoleService
import user.UserService

@Singleton
class Roles @Inject() (userService: UserService, roleService: RoleService) extends Controller with JsonActions {

  def listPermissions() = Action {
    Ok(Json.toJson(roleService.listPermissions()))
  }

  def listOperations() = Action {
    Ok(Json.toJson(roleService.listOperations()))
  }

  def listFullPermissions() = Action {
    implicit val staticAuthorisationOperationWrites: Writes[StaticAuthorisationOperation] = (
      (__ \ "resource").write[String] and
      (__ \ "action").write[String] and
      (__ \ "isSensitive").write[Boolean] and
      (__ \ "descriptionKey").write[String])((s: StaticAuthorisationOperation) => (
        s.resource.toString(),
        s.action.toString(),
        s.isSensitive,
        s.descriptionKey))

    implicit val permissionWrites: Writes[Permission] = (
      (__ \ "id").write[String] and
      (__ \ "operations").write[Set[StaticAuthorisationOperation]])((p: Permission) => (
        p.toString(),
        p.operations))

    Ok(Json.toJson(roleService.listPermissions()))
  }

  def getRoles = Action.async {
    roleService.getRoles.map { result => Ok(Json.toJson(result)) }
  }

  def getRolesForSignUp = Action.async {
    roleService.getRolesForSignUp().map { result => Ok(Json.toJson(result)) }
  }

  def addRole() = Action.async(BodyParsers.parse.json) { request =>
    request.body.validate[Role] fold (
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors)))),
      rol => roleService.addRole(rol).map(result => Ok(Json.toJson(result)).withHeaders("X-CREATED-ID" -> rol.id) ))
  }

  def updateRole() = Action.async(BodyParsers.parse.json) { request =>
    request.body.validate[Role] fold (
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors)))),
      rol => roleService.updateRole(rol).map(result => Ok(Json.toJson(result))))
  }

  def deleteRole(id: String) = Action.async { request =>
    roleService.deleteRole(id) map { result =>
      result match {
        case Right(b)    => Ok(Json.obj("result" -> b))
        case Left(error) => Ok(Json.obj("result" -> false, "error" -> error))
      }
    }
  }

}
