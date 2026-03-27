package controllers

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.mvc.{AbstractController, ControllerComponents, Action}
import play.api.libs.json.{Json, JsError, JsSuccess, Writes, __}
import play.api.libs.functional.syntax.*
import scala.concurrent.{ExecutionContext, Future}
import types.Permission
import security.{RoleService, StaticAuthorisationOperation}
import user.Role

@Singleton
class RoleController @Inject()(
    cc: ControllerComponents,
    roleService: RoleService
)(using ec: ExecutionContext) extends AbstractController(cc):

  private val logger = Logger(this.getClass)

  def listPermissions = Action {
    Ok(Json.toJson(roleService.listPermissions()))
  }

  def listFullPermissions = Action {
    given staticAuthorisationOperationWrites: Writes[StaticAuthorisationOperation] = (
      (__ \ "resource").write[String] and
      (__ \ "action").write[String] and
      (__ \ "isSensitive").write[Boolean] and
      (__ \ "descriptionKey").write[String]
    )((s: StaticAuthorisationOperation) => (
      s.resource.toString(),
      s.action.toString(),
      s.isSensitive,
      s.descriptionKey
    ))

    given permissionWrites: Writes[Permission] = (
      (__ \ "id").write[String] and
      (__ \ "operations").write[Set[StaticAuthorisationOperation]]
    )((p: Permission) => (
      p.toString(),
      p.operations
    ))

    Ok(Json.toJson(roleService.listPermissions()))
  }

  def listOperations = Action {
    Ok(Json.toJson(roleService.listOperations()))
  }

  def getRoles = Action.async {
    roleService.getRoles.map(result => Ok(Json.toJson(result)))
  }

  def getRolesForSignUp = Action.async {
    roleService.getRolesForSignUp().map(result => Ok(Json.toJson(result)))
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

  def exportRoles = Action.async {
    roleService.getRoles.map { roles =>
      val json = Json.toJson(roles)
      Ok(json).as("application/json").withHeaders("Content-Disposition" -> "attachment; filename=roles.json")
    }
  }

  def importRoles = Action.async(parse.multipartFormData) { request =>
    request.body.file("file").map { file =>
      val path = new java.io.File("/tmp/" + file.filename)
      file.ref.moveTo(path, replace = true)

      val source = scala.io.Source.fromFile(path, "UTF-8")
      val jsonString = try source.mkString finally source.close()

      Json.parse(jsonString).validate[List[Role]] match
        case JsSuccess(importedRoles, _) =>
          processImportRoles(importedRoles)
        case JsError(errors) =>
          Future.successful(BadRequest(Json.obj(
            "status" -> "error",
            "message" -> "Error en el formato JSON",
            "details" -> JsError.toJson(errors)
          )))
    }.getOrElse {
      Future.successful(BadRequest(Json.obj(
        "status" -> "error",
        "message" -> "No se encontró ningún archivo"
      )))
    }
  }

  private def processImportRoles(importedRoles: List[Role]): Future[play.api.mvc.Result] =
    roleService.getRoles.flatMap { existingRoles =>
      val deleteFutures = existingRoles.map(_.id).map(roleService.deleteRole)

      Future.sequence(deleteFutures).flatMap { deleteResults =>
        val deleteErrors = deleteResults.collect { case Left(error) => error }
        if deleteErrors.nonEmpty then
          logger.warn("Some roles could not be deleted: " + deleteErrors.mkString(", "))

        val addFutures = importedRoles.map(roleService.addRole)

        Future.sequence(addFutures).map { addResults =>
          val addErrors = addResults.collect { case false => "Error al añadir rol" }
          if addErrors.nonEmpty then
            InternalServerError(Json.obj(
              "status" -> "error",
              "message" -> "Error al importar rol",
              "details" -> Json.toJson(addErrors)
            ))
          else
            Ok(Json.obj(
              "status" -> "success",
              "message" -> "Importación de roles exitosa",
              "count" -> importedRoles.size
            ))
        }
      }
    }
