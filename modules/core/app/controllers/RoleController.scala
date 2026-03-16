package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, ControllerComponents, Action, BodyParsers}
import play.api.libs.json.{Json, JsError, Writes}
import scala.concurrent.{ExecutionContext, Future}
import types.Permission
import user.Role
import security.RoleService

@Singleton
class RoleController @Inject()(
    cc: ControllerComponents,
    roleService: RoleService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def listPermissions = Action {
    Ok(Json.toJson(roleService.getRolePermissions()))
  }

  def getRoles = Action.async {
    roleService.getRoles.map(result => Ok(Json.toJson(result)))
  }

  def addRole = Action.async(parse.json) { request =>
    request.body.validate[Role].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      role => roleService.addRole(role).map(result => Ok(Json.toJson(result)).withHeaders("X-CREATED-ID" -> role.id))
    )
  }

  def updateRole = Action.async(parse.json) { request =>
    request.body.validate[Role].fold(
      errors => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))),
      role => roleService.updateRole(role).map(result => Ok(Json.toJson(result)))
    )
  }

  def deleteRole(id: String) = Action.async { _ =>
    roleService.deleteRole(id).map {
      case Right(b)    => Ok(Json.obj("result" -> b))
      case Left(error) => Ok(Json.obj("result" -> false, "error" -> error))
    }
  }

  // Métodos de import/export pueden agregarse según necesidad
}
