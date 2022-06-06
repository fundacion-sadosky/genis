package controllers

import javax.inject.Inject
import javax.inject.Singleton

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Controller
import types.AlphanumericId
import play.api.libs.json.JsValue

import scala.collection.immutable.Map
import configdata._

import scala.concurrent.Future
import play.api.mvc.BodyParsers
import play.api.libs.json.JsError

import scala.util.{Left, Right}

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
    categoryService.listCategoriesMapping map {
      result => Ok(Json.toJson(result))
    }
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


}
