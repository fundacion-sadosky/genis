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
import profiledata.ProfileDataAttempt

import scala.util.{Left, Right}

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import java.nio.file.{Files, Paths}
import models.Tables.CategoryRow

@Singleton
class Categories @Inject() (categoryService: CategoryService) extends Controller with JsonActions {

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

  implicit val categoryReads: Reads[CategoryRow] = Json.reads[CategoryRow]

  def importCategories: Action[MultipartFormData[play.api.libs.Files.TemporaryFile]] = Action.async(parse.multipartFormData) { request =>
    request.body.file("file").map { file =>
      val path = new java.io.File("/tmp/" + file.filename)
      file.ref.moveTo(path, replace = true)

      val source = scala.io.Source.fromFile(path)
      val jsonString = try source.mkString finally source.close()

      Json.parse(jsonString).validate[List[CategoryRow]] match {
        case JsSuccess(categories, _) =>
          categoryService.replaceCategories(categories).map {
            case Right(_) => Ok("Importación exitosa")
            case Left(error) => InternalServerError(s"Error al importar: $error")
          }
        case JsError(errors) =>
          Future.successful(BadRequest("Error en el formato JSON"))
      }
    }.getOrElse {
      Future.successful(BadRequest("No se encontró ningún archivo"))
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
