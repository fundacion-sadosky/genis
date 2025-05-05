package controllers

import javax.inject.Inject
import javax.inject.Singleton
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, JsValue, Json, __}
import play.api.mvc.{Action, AnyContent, BodyParsers, Controller}
import types.AlphanumericId

import scala.collection.immutable.Map
import configdata._
import play.api.i18n.Messages

import scala.concurrent.Future
import profiledata._//{ProfileDataAttempt, ProfileDataRepository}

import scala.util.{Left, Right}
import play.api.mvc._
import play.api.libs.json._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import java.nio.file.{Files, Paths}
import models.Tables.CategoryRow
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.functional.syntax._
import profile.{ProfileRepository, ProfileService}

@Singleton
class Categories @Inject() (
                             categoryService: CategoryService,
                             profileService: ProfileService,
                             profileDataService: ProfileDataService
                           ) extends Controller with JsonActions {

  def categoryTree = Action.async {
    val fct = Future.successful(categoryService.categoryTree)
    fct map { tree =>
      val treeMap = tree map {
        case (group, categories) =>
          val grp = Json.obj(
            "id" -> group.id,
            "name" -> group.name,
            "subcategories" -> categories)
          group.id.text -> grp
      }

      Ok(Json.toJson(treeMap))
    }
  }

  def list = Action.async {
    val fcs = Future.successful(categoryService.listCategories)
    fcs map { list =>
      val catList = list map {
        case (id, category) => id.text -> category
      }

      Ok(Json.toJson(catList))
    }
  }

  def exportCategories: Action[AnyContent] = Action {
    categoryService.exportCategories("/tmp/categories.json") match {
      case Right(_) =>
        Ok.sendFile(new java.io.File("/tmp/categories.json"), inline = false)
      case Left(errorMessage) =>
        InternalServerError(errorMessage)
    }
  }

  implicit val categoryRowReads: Reads[CategoryRow] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "group").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "isReference").read[Boolean] and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "filiationDataRequired").read[Boolean].orElse(Reads.pure(false)) and
      (JsPath \ "replicate").read[Boolean].orElse(Reads.pure(true)) and
      (JsPath \ "pedigreeAssociation").read[Boolean].orElse(Reads.pure(false)) and
      (JsPath \ "allowManualLoading").read[Boolean].orElse(Reads.pure(true)) and
      (JsPath \ "tipo").read[Int].orElse(Reads.pure(1))
    )(CategoryRow.apply _)
  //Json.reads[CategoryRow]

  def importCategories: Action[MultipartFormData[play.api.libs.Files.TemporaryFile]] = Action.async(parse.multipartFormData) { request =>
    request.body.file("file").map { file =>
      // Guardar archivo temporalmente
      val path = new java.io.File("/tmp/" + file.filename)
      file.ref.moveTo(path, replace = true)

      // Leer contenido del archivo
      val source = scala.io.Source.fromFile(path, "UTF-8")
      val jsonString = try source.mkString finally source.close()

      // Parsear JSON a lista de CategoryRow
      Json.parse(jsonString).validate[List[CategoryRow]] match {
        case JsSuccess(importedCategories, _) =>
          // Proceso de importación: eliminar categorías existentes y añadir las nuevas
          processImportCategories(importedCategories)
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

  // Método auxiliar para procesar la importación de categorías
  private def processImportCategories(importedCategories: List[CategoryRow]): Future[Result] = {
    // 0. Eliminar todos los perfiles (y demás)
    //profileService.removeAll()
    profileDataService.removeAll()

    // 1. Obtener las categorías existentes
    val existingCategoriesFuture = Future.successful(categoryService.listCategories)

    existingCategoriesFuture.flatMap { existingCategories =>
      // 2. Eliminar categorías existentes
      val deleteFutures = existingCategories.keys.map { categoryId =>
        categoryService.removeCategory(categoryId)
      }

      Future.sequence(deleteFutures.toSeq).flatMap { deleteResults =>
        // Verificar errores de eliminación
        val deleteErrors = deleteResults.collect { case Left(error) => error }

        if (deleteErrors.nonEmpty) {
          Future.successful(InternalServerError(Json.obj(
            "status" -> "error",
            "message" -> "Error al eliminar categorías existentes",
            "details" -> deleteErrors
          )))
        } else {
          // 3. Agregar nuevas categorías
          val addFutures = importedCategories.map { categoryRow =>
            // Convertir CategoryRow a Category
            val category = Category(
              AlphanumericId(categoryRow.id),
              AlphanumericId(categoryRow.group),
              categoryRow.name,
              categoryRow.isReference,
              categoryRow.description
            )

            categoryService.addCategory(category)
          }

          Future.sequence(addFutures).map { addResults =>
            // Verificar errores de adición
            val addErrors = addResults.collect { case Left(error) => error }

            if (addErrors.nonEmpty) {
              InternalServerError(Json.obj(
                "status" -> "error",
                "message" -> "Error al importar categorías",
                "details" -> addErrors
              ))
            } else {
              // Importación exitosa
              Ok(Json.obj(
                "status" -> "success",
                "message" -> "Importación exitosa",
                "count" -> importedCategories.size
              ))
            }
          }
        }
      }
    }
  }
  def listWithProfiles = Action.async {
    val fcs = Future.successful(categoryService.listCategoriesWithProfiles)
    fcs map { list =>
      val catList = list map {
        //case (id, category) => id.text -> category
        case (id, category) => Json.obj("id"->id.text,"category"->category)
      }
      Ok(Json.toJson(catList))
    }
  }

  def categoryTreeManualLoading = Action.async {
    val fct = Future.successful(categoryService.categoryTreeManualLoading)
    fct map { tree =>
      val treeMap = tree map {
        case (group, categories) =>
          val grp = Json.obj(
            "id" -> group.id,
            "name" -> group.name,
            "subcategories" -> categories)
          group.id.text -> grp
      }

      Ok(Json.toJson(treeMap))
    }
  }

  def addCategory = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[Category]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      input => {
        val result = categoryService.addCategory(input)
        result map { outEither =>
          outEither.fold(
            error => BadRequest(Json.toJson(error)),
            success => Ok(Json.toJson(success)).withHeaders("X-CREATED-ID" -> success.id.toString()))
        }
      })
  }

  private val updateCategoryFun = (category: Category) => categoryService.updateCategory(category)
  def updateCategory(catId: AlphanumericId) = JsonAction(updateCategoryFun)

  private val updateFullCategoryFun = (category: FullCategory) => categoryService.updateCategory(category)
  def updateFullCategory(catId: AlphanumericId) = JsonAction(updateFullCategoryFun)

  def removeCategory(categoryId: AlphanumericId) = Action.async {
    categoryService.removeCategory(categoryId) map { result =>
      result.fold({ err =>
        BadRequest(err)
      }, { ok =>
        Ok(ok.toString())
      })
    }
  }

  def addGroup = Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[Group]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      input => {
        val result = categoryService.addGroup(input)
        result map { outEither =>
          outEither.fold(
            error => BadRequest(Json.toJson(error)),
            success => Ok(Json.toJson(success)).withHeaders("X-CREATED-ID" -> success.text))
        }
      })
  }

  def updateGroup(groupId: AlphanumericId) = JsonAction { (group: Group) =>
    categoryService.updateGroup(group)
  }

  def removeGroup(groupId: AlphanumericId) = Action.async {
    categoryService.removeGroup(groupId) map { result =>
      result.fold({ err =>
        BadRequest(err)
      }, { ok =>
        Ok(ok.toString())
      })
    }
  }

  def listCategoriesMapping = Action.async {
    val x = categoryService.listCategoriesMapping map {
      result => Ok(Json.toJson(result))
    }
    x
  }

  def insertOrUpdateCategoriesMapping =  Action.async(BodyParsers.parse.json) { request =>
    val input = request.body.validate[CategoryMappingList]
    input.fold(
      errors => {
        Future.successful(BadRequest(JsError.toFlatJson(errors)))
      },
      input => {
        categoryService.insertOrUpdateMapping(input) map {
          case Left(e) => BadRequest(Json.obj("message" -> e))
          case Right(_) => Ok
        }

    })
  }

  def getCategoryTreeCombo = Action.async {
    val fct = Future.successful(categoryService.categoryTree)
    fct map { tree =>
      val treeMap = tree.filter(x => !List("AM","PM").contains(x._1.id.text) ) map {
        case (group, categories) =>
          val grp = Json.obj(
            "id" -> group.id,
            "name" -> group.name,
            "subcategories" -> categories.map(x => CategoryCombo(x.id,x.name)) )
          group.id.text -> grp
      }
      Ok(Json.toJson(treeMap))
    }
  }

  
  def unregisterCategoryModification(
    from: AlphanumericId,
    to: AlphanumericId
  ): Action[AnyContent] = Action
    .async {
      Future {
        categoryService
          .unregisterCategoryModification(from, to)
            match {
              case x if x == 0 => ("error", Messages("error.E0602"))
              case _ => ("success", Messages("success.S0601"))
            }
        }
        .map {
          case (status, message) =>
            Json.obj("status" -> status, "message" -> message)
        }
        .map(x => Ok(x))
    }
  
  
  def registerCategoryModification(
    from: AlphanumericId,
    to: AlphanumericId
  ): Action[AnyContent] = Action
    .async {
      Future {
        categoryService
          .registerCategoryModification(from, to)
            match {
              case None => ("error", Messages("error.E0603"))
              case Some(0) => ("error", Messages("error.E0601"))
              case _ => ("success", Messages("success.S0600"))
            }
        }
        .map {
          case (status, message) =>
            Json.obj("status" -> status, "message" -> message)
        }
        .map(x => Ok(x))
  }

  def allCategoryModifications : Action[AnyContent] = Action
    .async {
      categoryService
        .retrieveAllCategoryModificationAllowed
        .map(
          mods => mods
            .map { case (from, to) => (from.text, to.text) }
            .map { case (from, to) => Json.obj("from" -> from, "to" -> to) }
        )
        .map(x => Json.toJson(x))
        .map(x => Ok(x))
    }

  def getCategoryModifications(catId: AlphanumericId): Action[AnyContent] = Action
    .async {
      categoryService
        .getCategoryModificationAllowed(catId)
        .map { x => x.map(_.text) }
        .map { x => Json.toJson(x) }
        .map { x => Ok(x) }
    }
}
