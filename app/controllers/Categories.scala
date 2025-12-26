package controllers

import play.api.Logger

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
import profiledata._

import scala.util.{Left, Right}
import play.api.mvc._
import play.api.libs.json._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import java.nio.file.{Files, Paths}
import models.Tables.CategoryRow
import play.api.libs.Files.TemporaryFile
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.functional.syntax._
import profile.{ProfileRepository, ProfileService}
import models.Tables.{CategoryAliasRow, CategoryMatchingRow, CategoryConfigurationRow}

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

  def exportGroups = Action.async {
    categoryService.listGroups map { group =>
      val json = Json.toJson(group)
      Ok(json).as("application/json").withHeaders("Content-Disposition" -> "attachment; filename=groups.json")
    }
  }
  
  implicit val categoryConfigurationRowWrites: Writes[CategoryConfigurationRow] = Json.writes[CategoryConfigurationRow]

  def exportConfigurations = Action.async {
    categoryService.listConfigurations map { conf =>
      val json = Json.toJson(conf)
      Ok(json).as("application/json").withHeaders("Content-Disposition" -> "attachment; filename=categoryConfigurations.json")
    }
  }

  def exportAssociations = Action.async {
    categoryService.listAssociations map { assoc =>
      val json = Json.toJson(assoc)
      Ok(json).as("application/json").withHeaders("Content-Disposition" -> "attachment; filename=categoryAssociations.json")
    }
  }
  implicit val categoryAliasRowWrites: Writes[CategoryAliasRow] = Json.writes[CategoryAliasRow]
  def exportAlias = Action.async {
    categoryService.listAlias map { alias =>
      val json = Json.toJson(alias)
      Ok(json).as("application/json").withHeaders("Content-Disposition" -> "attachment; filename=categoryAlias.json")
    }
  }
  implicit val categoryMatchingRowWrites: Writes[CategoryMatchingRow] = Json.writes[CategoryMatchingRow]
  def exportMatchingRules = Action.async {
    categoryService.listMatchingRules map { matchingRule =>
      val json = Json.toJson(matchingRule)
      Ok(json).as("application/json").withHeaders("Content-Disposition" -> "attachment; filename=categoryMatchingRules.json")
    }
  }

  def exportModifications = Action.async {
    categoryService.retrieveAllCategoryModificationAllowed.map { mods =>
      val json = Json.toJson(
        mods.map {
          case (from, to) =>
            Json.obj(
              "from" -> from.text,
              "to"   -> to.text
            )
        }
      )

      Ok(json)
        .as("application/json")
        .withHeaders("Content-Disposition" -> "attachment; filename=categoryModifications.json")
    }
  }


  def exportMappings = Action.async {
    categoryService.listCategoriesMapping map { mapping =>
      val json = Json.toJson(mapping)
      Ok(json).as("application/json").withHeaders("Content-Disposition" -> "attachment; filename=categoryMappings.json")
    }
  }
  
  def importGroups: Action[MultipartFormData[play.api.libs.Files.TemporaryFile]] = Action.async(parse.multipartFormData) { request =>
    request.body.file("file").map { file =>
      // Guardar archivo temporalmente
      val path = new java.io.File("/tmp/" + file.filename)
      file.ref.moveTo(path, replace = true)

      // Leer contenido del archivo
      val source = scala.io.Source.fromFile(path, "UTF-8")
      val jsonString = try source.mkString finally source.close()

      // Parsear JSON a lista de Group
      Json.parse(jsonString).validate[List[Group]] match {
        case JsSuccess(importedGroups, _) =>
          // Proceso de importación: eliminar grupos existentes y añadir los nuevos
          processImportGroups(importedGroups)
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
  private def processImportGroups(importedGroups: List[Group]): Future[Result] = {
    // Supongo no existencia de perfiles
    Logger.info("Eliminando grupos")

    categoryService.removeAllGroups().flatMap { nGroupsRemoved =>

      Logger.info("Cantidad de grupos eliminados: " + nGroupsRemoved)

      val addFutures = importedGroups.map { group =>
        Logger.info("Agregando grupo: " + group)
        categoryService.addGroup(group)
      }

      Future.sequence(addFutures).map { addResults =>
        // Verificar errores de adición
        val addErrors = addResults.collect { case Left(error) => error }
        Logger.info("Cantidad de errores en adds: " + addErrors.size)

        if (addErrors.nonEmpty) {
          InternalServerError(Json.obj(
            "status" -> "error",
            "message" -> "Error al importar grupos",
            "details" -> addErrors
          ))
        } else {
          // Importación exitosa
          Ok(Json.obj(
            "status" -> "success",
            "message" -> "Importación de grupos exitosa",
            "count" -> importedGroups.size
          ))
        }
      }
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

  private def readJsonFile[T: Reads](
                                      file: MultipartFormData.FilePart[TemporaryFile]
                                    ): Future[T] = Future {

    val path = new java.io.File("/tmp/" + file.filename)
    file.ref.moveTo(path, replace = true)

    val source = scala.io.Source.fromFile(path, "UTF-8")
    val jsonString = try source.mkString finally source.close()

    Json.parse(jsonString).as[T]
  }
  private def addAllCategories(categories: List[CategoryRow]): Future[Unit] = {
    val futures = categories.map { c =>
      categoryService.addCategory(
        Category(
          AlphanumericId(c.id),
          AlphanumericId(c.group),
          c.name,
          c.isReference,
          c.description
        )
      )
    }

    Future.sequence(futures).map(_ => ())
  }

  private def addAllGroups(groups: List[Group]): Future[Unit] = {
    val futures = groups.map { g =>
      categoryService.addGroup(
        Group(
          g.id,
          g.name,
          g.description
        )
      )
    }

    Future.sequence(futures).map(_ => ())
  }
  def importGroupsAndCategories: Action[MultipartFormData[TemporaryFile]] =
    Action.async(parse.multipartFormData) { request =>

      val maybeGroupsFile     = request.body.file("groups")
      val maybeCategoriesFile = request.body.file("categories")

      (maybeGroupsFile, maybeCategoriesFile) match {
        case (Some(groups), Some(categories)) =>

          val groupsJsonF     = readJsonFile[List[Group]](groups)
          val categoriesJsonF = readJsonFile[List[CategoryRow]](categories)

          for {
            groups     <- groupsJsonF
            categories <- categoriesJsonF
            result     <- processImport(groups, categories)
          } yield result

        case _ =>
          Future.successful(
            BadRequest(Json.obj(
              "status" -> "error",
              "message" -> "Se requieren los archivos groups y categories"
            ))
          )
      }
    }

  // Método auxiliar para procesar la importación de categorías
  private def processImport(
                             groups: List[Group],
                             categories: List[CategoryRow]
                           ): Future[Result] = {

    for {
      _ <- categoryService.removeAllCategories()
      _ <- categoryService.removeAllGroups()

      _ <- addAllGroups(groups)

      _ <- addAllCategories(categories)

    } yield {
      Ok(Json.obj(
        "status" -> "success",
        "groups" -> groups.size,
        "categories" -> categories.size
      ))
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
