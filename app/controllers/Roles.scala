package controllers

import play.api.Logger

import scala.concurrent.Future
import javax.inject.Inject
import javax.inject.Singleton
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsError, JsSuccess, Json, Writes, __}
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.{Action, BodyParsers, Controller, MultipartFormData, Result}
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

  def exportRoles = Action.async {
    roleService.getRoles map { role =>
      val json = Json.toJson(role)
      Ok(json).as("application/json").withHeaders("Content-Disposition" -> "attachment; filename=roles.json")
    }
  }

  def importRoles:  Action[MultipartFormData[play.api.libs.Files.TemporaryFile]] = Action.async(parse.multipartFormData) { request =>
    request.body.file("file").map { file =>
      // Guardar archivo temporalmente
      val path = new java.io.File("/tmp/" + file.filename)
      file.ref.moveTo(path, replace = true)

      // Leer contenido del archivo
      val source = scala.io.Source.fromFile(path, "UTF-8")
      val jsonString = try source.mkString finally source.close()

      // Parsear JSON a lista de locus
      Json.parse(jsonString).validate[List[Role]] match {
        case JsSuccess(importedRoles, _) =>
          // Proceso de importación: eliminar categorías existentes y añadir las nuevas
          processImportRoles(importedRoles)
        case JsError(errors) =>
          Future.successful(BadRequest(Json.obj(
            "status" -> "error",
            "message" -> "Error en el formato JSON",
            "details" -> JsError.toFlatJson(errors)
          )))
      }
    }.getOrElse {
      Future.successful(BadRequest(Json.obj(
        "status" -> "error",
        "message" -> "No se encontró ningún archivo"
      )))
    }
  }

  // Método auxiliar para procesar la importación de kits
  private def processImportRoles(importedRoles: List[Role]): Future[Result] = {

    // 1. Obtener los Kits existentes
    val existingRolesFuture = roleService.getRoles()

    existingRolesFuture.flatMap { existingRoles =>
      // 2. Eliminar Locus existentes
      val deleteFutures = existingRoles.map(_.id).map { roleId =>
        roleService.deleteRole(roleId)
      }

      Future.sequence(deleteFutures).flatMap { deleteResults =>
        // Verificar errores de eliminación
        val deleteErrors = deleteResults.collect { case Left(error) => error }

        if (deleteErrors.nonEmpty) {
          // log warning
          Logger.warn("Some roles could not be deleted: " + deleteErrors.mkString(", "))
          
        }
        // 3. Agregar nuevas categorías
        val addFutures = importedRoles.map { role =>
          roleService.addRole(role)
        }

        Future.sequence(addFutures).map { addResults =>
          // Verificar errores de adición
          val addErrors = addResults.collect { case false => "Error al añadir rol" }

          if (addErrors.nonEmpty) {
            InternalServerError(Json.obj(
              "status" -> "error",
              "message" -> "Error al importar rol",
              "details" -> addErrors
            ))
          } else {
            // Importación exitosa
            Ok(Json.obj(
              "status" -> "success",
              "message" -> "Importación de roles exitosa",
              "count" -> importedRoles.size
            ))
          }
        }
      }
    }
  }

}
