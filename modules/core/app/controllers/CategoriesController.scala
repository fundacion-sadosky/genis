package controllers

import javax.inject.{Inject, Singleton}
import configdata._
import models.Tables
import matching.{Algorithm, Stringency}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, MultipartFormData}
import types.AlphanumericId
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CategoriesController @Inject()(
    categoryService: CategoryService,
    cc: ControllerComponents
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // ─── Reads for import ────────────────────────────────────────────────────

  implicit val alphanumericIdReads: Reads[AlphanumericId] =
    Reads.StringReads.map(AlphanumericId.apply)

  implicit val categoryRowReads: Reads[Tables.CategoryRow] = (
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
  )(Tables.CategoryRow.apply _)

  implicit val categoryConfigRowReads: Reads[Tables.CategoryConfigurationRow] = (
    (JsPath \ "id").read[Long] and
    (JsPath \ "category").read[String] and
    (JsPath \ "type").read[Int] and
    (JsPath \ "collectionUri").read[String].orElse(Reads.pure("")) and
    (JsPath \ "draftUri").read[String].orElse(Reads.pure("")) and
    (JsPath \ "minLocusPerProfile").read[String].orElse(Reads.pure("K")) and
    (JsPath \ "maxOverageDeviatedLoci").read[String].orElse(Reads.pure("0")) and
    (JsPath \ "maxAllelesPerLocus").read[Int].orElse(Reads.pure(6)) and
    (JsPath \ "multiallelic").read[Boolean].orElse(Reads.pure(false))
  )(Tables.CategoryConfigurationRow.apply _)

  implicit val categoryAliasRowReads: Reads[Tables.CategoryAliasRow] = (
    (JsPath \ "alias").read[String] and
    (JsPath \ "category").read[String]
  )(Tables.CategoryAliasRow.apply _)

  implicit val categoryMatchingRowReads: Reads[Tables.CategoryMatchingRow] = (
    (JsPath \ "id").read[Long] and
    (JsPath \ "category").read[String] and
    (JsPath \ "categoryRelated").read[String] and
    (JsPath \ "priority").read[Int].orElse(Reads.pure(1)) and
    (JsPath \ "minimumStringency").read[String].orElse(Reads.pure("ImpossibleMatch")) and
    (JsPath \ "failOnMatch").readNullable[Boolean].map(_.orElse(Some(false))) and
    (JsPath \ "forwardToUpper").readNullable[Boolean].map(_.orElse(Some(false))) and
    (JsPath \ "matchingAlgorithm").read[String].orElse(Reads.pure("ENFSI")) and
    (JsPath \ "minLocusMatch").read[Int].orElse(Reads.pure(10)) and
    (JsPath \ "mismatchsAllowed").read[Int].orElse(Reads.pure(0)) and
    (JsPath \ "type").read[Int] and
    (JsPath \ "considerForN").read[Boolean].orElse(Reads.pure(true))
  )(Tables.CategoryMatchingRow.apply _)

  implicit val categoryAssocRowReads: Reads[Tables.CategoryAssociationRow] = (
    (JsPath \ "id").read[Long] and
    (JsPath \ "category").read[String] and
    (JsPath \ "categoryRelated").read[String] and
    (JsPath \ "mismatchs").read[Int] and
    (JsPath \ "type").read[Int]
  )(Tables.CategoryAssociationRow.apply _)

  implicit val categoryModificationReads: Reads[(AlphanumericId, AlphanumericId)] = (
    (JsPath \ "from").read[AlphanumericId] and
    (JsPath \ "to").read[AlphanumericId]
  )((from, to) => (from, to))

  // ─── Writes for export ───────────────────────────────────────────────────

  implicit val categoryConfigRowWrites: Writes[Tables.CategoryConfigurationRow] = Json.writes[Tables.CategoryConfigurationRow]
  implicit val categoryAssocRowWrites: Writes[Tables.CategoryAssociationRow]    = Json.writes[Tables.CategoryAssociationRow]
  implicit val categoryAliasRowWrites: Writes[Tables.CategoryAliasRow]          = Json.writes[Tables.CategoryAliasRow]
  implicit val categoryMatchingRowWrites: Writes[Tables.CategoryMatchingRow]    = Json.writes[Tables.CategoryMatchingRow]

  // ─── Read/write tree ─────────────────────────────────────────────────────

  def categoryTree: Action[AnyContent] = Action.async {
    categoryService.categoryTree.map { tree =>
      val treeMap = tree.map { case (group, categories) =>
        group.id.text -> Json.obj("id" -> group.id, "name" -> group.name, "subcategories" -> categories)
      }
      Ok(Json.toJson(treeMap))
    }
  }

  def categoryTreeManualLoading: Action[AnyContent] = Action.async {
    categoryService.categoryTreeManualLoading.map { tree =>
      val treeMap = tree.map { case (group, categories) =>
        group.id.text -> Json.obj("id" -> group.id, "name" -> group.name, "subcategories" -> categories)
      }
      Ok(Json.toJson(treeMap))
    }
  }

  def getCategoryTreeCombo: Action[AnyContent] = Action.async {
    categoryService.categoryTree.map { tree =>
      val treeMap = tree
        .filterNot(kv => List("AM", "PM").contains(kv._1.id.text))
        .map { case (group, categories) =>
          group.id.text -> Json.obj("id" -> group.id, "name" -> group.name,
            "subcategories" -> categories.map(c => CategoryCombo(c.id, c.name)))
        }
      Ok(Json.toJson(treeMap))
    }
  }

  def list: Action[AnyContent] = Action.async {
    categoryService.listCategories.map { cats =>
      Ok(Json.toJson(cats.map { case (id, cat) => id.text -> cat }))
    }
  }

  def listWithProfiles: Action[AnyContent] = Action.async {
    categoryService.listCategoriesWithProfiles.map { cats =>
      Ok(Json.toJson(cats.map { case (id, name) => Json.obj("id" -> id.text, "category" -> name) }))
    }
  }

  // ─── Category CRUD ────────────────────────────────────────────────────────

  def addCategory: Action[AnyContent] = Action.async(parse.anyContent) { request =>
    request.body.asJson.map(_.validate[Category]) match {
      case None => Future.successful(BadRequest(Json.obj("message" -> "Expected JSON")))
      case Some(JsError(e)) => Future.successful(BadRequest(JsError.toJson(e)))
      case Some(JsSuccess(cat, _)) =>
        categoryService.addCategory(cat).map {
          case Left(err) => BadRequest(Json.toJson(err))
          case Right(fc) => Ok(Json.toJson(fc)).withHeaders("X-CREATED-ID" -> fc.id.text)
        }
    }
  }

  def updateCategory(catId: AlphanumericId): Action[AnyContent] = Action.async(parse.anyContent) { request =>
    request.body.asJson.map(_.validate[Category]) match {
      case None => Future.successful(BadRequest(Json.obj("message" -> "Expected JSON")))
      case Some(JsError(e)) => Future.successful(BadRequest(JsError.toJson(e)))
      case Some(JsSuccess(cat, _)) =>
        categoryService.updateCategory(cat).map {
          case Left(err) => BadRequest(Json.toJson(err))
          case Right(_)  => Ok
        }
    }
  }

  def updateFullCategory(catId: AlphanumericId): Action[AnyContent] = Action.async(parse.anyContent) { request =>
    request.body.asJson.map(_.validate[FullCategory]) match {
      case None => Future.successful(BadRequest(Json.obj("message" -> "Expected JSON")))
      case Some(JsError(e)) => Future.successful(BadRequest(JsError.toJson(e)))
      case Some(JsSuccess(cat, _)) =>
        categoryService.updateCategory(cat).map {
          case Left(err) => BadRequest(Json.toJson(err))
          case Right(_)  => Ok
        }
    }
  }

  def removeCategory(categoryId: AlphanumericId): Action[AnyContent] = Action.async {
    categoryService.removeCategory(categoryId).map {
      case Left(err) => BadRequest(err)
      case Right(n)  => Ok(n.toString)
    }
  }

  // ─── Group CRUD ───────────────────────────────────────────────────────────

  def addGroup: Action[AnyContent] = Action.async(parse.anyContent) { request =>
    request.body.asJson.map(_.validate[Group]) match {
      case None => Future.successful(BadRequest(Json.obj("message" -> "Expected JSON")))
      case Some(JsError(e)) => Future.successful(BadRequest(JsError.toJson(e)))
      case Some(JsSuccess(group, _)) =>
        categoryService.addGroup(group).map {
          case Left(err) => BadRequest(Json.toJson(err))
          case Right(id) => Ok(Json.toJson(id)).withHeaders("X-CREATED-ID" -> id.text)
        }
    }
  }

  def updateGroup(groupId: AlphanumericId): Action[AnyContent] = Action.async(parse.anyContent) { request =>
    request.body.asJson.map(_.validate[Group]) match {
      case None => Future.successful(BadRequest(Json.obj("message" -> "Expected JSON")))
      case Some(JsError(e)) => Future.successful(BadRequest(JsError.toJson(e)))
      case Some(JsSuccess(group, _)) =>
        categoryService.updateGroup(group).map {
          case Left(err) => BadRequest(Json.toJson(err))
          case Right(_)  => Ok
        }
    }
  }

  def removeGroup(groupId: AlphanumericId): Action[AnyContent] = Action.async {
    categoryService.removeGroup(groupId).map {
      case Left(err) => BadRequest(err)
      case Right(n)  => Ok(n.toString)
    }
  }

  // ─── Mapping ──────────────────────────────────────────────────────────────

  def listCategoriesMapping: Action[AnyContent] = Action.async {
    categoryService.listCategoriesMapping.map(r => Ok(Json.toJson(r)))
  }

  def insertOrUpdateCategoriesMapping: Action[AnyContent] = Action.async(parse.anyContent) { request =>
    request.body.asJson.map(_.validate[CategoryMappingList]) match {
      case None => Future.successful(BadRequest(Json.obj("message" -> "Expected JSON")))
      case Some(JsError(e)) => Future.successful(BadRequest(JsError.toJson(e)))
      case Some(JsSuccess(mappings, _)) =>
        categoryService.insertOrUpdateMapping(mappings).map {
          case Left(err) => BadRequest(Json.obj("message" -> err))
          case Right(_)  => Ok
        }
    }
  }

  // ─── Category modifications ───────────────────────────────────────────────

  def registerCategoryModification(from: AlphanumericId, to: AlphanumericId): Action[AnyContent] = Action.async {
    categoryService.registerCategoryModification(from, to).map {
      case None    => Ok(Json.obj("status" -> "error",   "message" -> "Ya existe o from == to"))
      case Some(0) => Ok(Json.obj("status" -> "error",   "message" -> "No se registró la modificación"))
      case _       => Ok(Json.obj("status" -> "success", "message" -> "Modificación registrada"))
    }
  }

  def unregisterCategoryModification(from: AlphanumericId, to: AlphanumericId): Action[AnyContent] = Action.async {
    categoryService.unregisterCategoryModification(from, to).map {
      case 0 => Ok(Json.obj("status" -> "error",   "message" -> "No se encontró la modificación"))
      case _ => Ok(Json.obj("status" -> "success", "message" -> "Modificación eliminada"))
    }
  }

  def allCategoryModifications: Action[AnyContent] = Action.async {
    categoryService.retrieveAllCategoryModificationAllowed.map { mods =>
      Ok(Json.toJson(mods.map { case (f, t) => Json.obj("from" -> f.text, "to" -> t.text) }))
    }
  }

  def getCategoryModifications(catId: AlphanumericId): Action[AnyContent] = Action.async {
    categoryService.getCategoryModificationAllowed(catId)
      .map(ids => Ok(Json.toJson(ids.map(_.text))))
  }

  // ─── Export ───────────────────────────────────────────────────────────────

  def exportGroups: Action[AnyContent] = Action.async {
    categoryService.listGroups.map { groups =>
      Ok(Json.toJson(groups)).as("application/json")
        .withHeaders("Content-Disposition" -> "attachment; filename=groups.json")
    }
  }

  def exportConfigurations: Action[AnyContent] = Action.async {
    categoryService.listConfigurations.map { conf =>
      Ok(Json.toJson(conf)).as("application/json")
        .withHeaders("Content-Disposition" -> "attachment; filename=categoryConfigurations.json")
    }
  }

  def exportAssociations: Action[AnyContent] = Action.async {
    categoryService.listAssociations.map { assoc =>
      Ok(Json.toJson(assoc)).as("application/json")
        .withHeaders("Content-Disposition" -> "attachment; filename=categoryAssociations.json")
    }
  }

  def exportAlias: Action[AnyContent] = Action.async {
    categoryService.listAlias.map { alias =>
      Ok(Json.toJson(alias)).as("application/json")
        .withHeaders("Content-Disposition" -> "attachment; filename=categoryAlias.json")
    }
  }

  def exportMatchingRules: Action[AnyContent] = Action.async {
    categoryService.listMatchingRules.map { rules =>
      Ok(Json.toJson(rules)).as("application/json")
        .withHeaders("Content-Disposition" -> "attachment; filename=categoryMatchingRules.json")
    }
  }

  def exportModifications: Action[AnyContent] = Action.async {
    categoryService.retrieveAllCategoryModificationAllowed.map { mods =>
      Ok(Json.toJson(mods.map { case (f, t) => Json.obj("from" -> f.text, "to" -> t.text) }))
        .as("application/json")
        .withHeaders("Content-Disposition" -> "attachment; filename=categoryModifications.json")
    }
  }

  def exportMappings: Action[AnyContent] = Action.async {
    categoryService.listCategoriesMapping.map { m =>
      Ok(Json.toJson(m)).as("application/json")
        .withHeaders("Content-Disposition" -> "attachment; filename=categoryMappings.json")
    }
  }

  def exportCategories: Action[AnyContent] = Action.async {
    categoryService.exportCategories("/tmp/categories.json").map {
      case Right(_)  => Ok.sendFile(new java.io.File("/tmp/categories.json"), inline = false)
      case Left(err) => InternalServerError(err)
    }
  }

  // ─── Import ───────────────────────────────────────────────────────────────

  private def readJsonFile[T: Reads](file: MultipartFormData.FilePart[play.api.libs.Files.TemporaryFile]): Future[T] = Future {
    val path = new java.io.File("/tmp/" + file.filename)
    file.ref.moveTo(path, replace = true)
    val src = scala.io.Source.fromFile(path, "UTF-8")
    try Json.parse(src.mkString).as[T] finally src.close()
  }

  private def toCategoryConfiguration(r: Tables.CategoryConfigurationRow): CategoryConfiguration =
    CategoryConfiguration(r.collectionUri, r.draftUri, r.minLocusPerProfile, r.maxOverageDeviatedLoci, r.maxAllelesPerLocus, r.multiallelic)

  private def toCategoryAssociation(r: Tables.CategoryAssociationRow): CategoryAssociation =
    CategoryAssociation(r.`type`, AlphanumericId(r.categoryRelated), r.mismatchs)

  private def toMatchingRule(r: Tables.CategoryMatchingRow): MatchingRule =
    MatchingRule(r.`type`, AlphanumericId(r.categoryRelated),
      Stringency.withName(r.minimumStringency), r.failOnMatch.getOrElse(false),
      r.forwardToUpper.getOrElse(false), Algorithm.withName(r.matchingAlgorithm),
      r.minLocusMatch, r.mismatchsAllowed, r.considerForN)

  def importGroupsAndCategories: Action[MultipartFormData[play.api.libs.Files.TemporaryFile]] =
    Action.async(parse.multipartFormData) { request =>
      (
        request.body.file("groups"),
        request.body.file("categories"),
        request.body.file("categoryConfigurations"),
        request.body.file("categoryAssociations"),
        request.body.file("categoryAlias"),
        request.body.file("categoryMatchingRules"),
        request.body.file("categoryModifications"),
        request.body.file("categoryMappings")
      ) match {
        case (Some(gf), Some(cf), Some(cnf), Some(af), Some(alF), Some(mrf), Some(modf), Some(mapF)) =>
          for {
            groups         <- readJsonFile[List[Group]](gf)
            categories     <- readJsonFile[List[Tables.CategoryRow]](cf)
            configurations <- readJsonFile[List[Tables.CategoryConfigurationRow]](cnf)
            associations   <- readJsonFile[List[Tables.CategoryAssociationRow]](af)
            aliases        <- readJsonFile[List[Tables.CategoryAliasRow]](alF)
            matchingRules  <- readJsonFile[List[Tables.CategoryMatchingRow]](mrf)
            modifications  <- readJsonFile[List[(AlphanumericId, AlphanumericId)]](modf)
            mappings       <- readJsonFile[List[FullCategoryMapping]](mapF)
            result         <- processImport(groups, categories, configurations, associations, aliases, matchingRules, modifications, mappings)
          } yield result
        case _ =>
          Future.successful(BadRequest(Json.obj("status" -> "error", "message" -> "Faltan archivos obligatorios")))
      }
    }

  private def processImport(
    groups: List[Group],
    categories: List[Tables.CategoryRow],
    configurations: List[Tables.CategoryConfigurationRow],
    associations: List[Tables.CategoryAssociationRow],
    aliases: List[Tables.CategoryAliasRow],
    matchingRules: List[Tables.CategoryMatchingRow],
    modifications: List[(AlphanumericId, AlphanumericId)],
    mappings: List[FullCategoryMapping]
  ): Future[play.api.mvc.Result] = {
    val confByCat  = configurations.groupBy(_.category)
    val assocByCat = associations.groupBy(_.category)
    val aliasByCat = aliases.groupBy(_.category)
    val rulesByCat = matchingRules.groupBy(_.category)

    for {
      _ <- categoryService.removeAllCategories()
      _ <- categoryService.removeAllGroups()
      _ <- Future.sequence(groups.map(categoryService.addGroup))
      _ <- Future.sequence(categories.map { c =>
             categoryService.addCategory(Category(AlphanumericId(c.id), AlphanumericId(c.group), c.name, c.isReference, c.description))
           })
      _ <- Future.sequence(categories.map { c =>
             val catId = AlphanumericId(c.id)
             val fc = FullCategory(
               id = catId, name = c.name, description = c.description, group = AlphanumericId(c.group),
               isReference = c.isReference, filiationDataRequired = c.filiationData,
               configurations = confByCat.getOrElse(c.id, Nil).map(r => r.`type` -> toCategoryConfiguration(r)).toMap,
               associations   = assocByCat.getOrElse(c.id, Nil).map(toCategoryAssociation),
               aliases        = aliasByCat.getOrElse(c.id, Nil).map(_.alias),
               matchingRules  = rulesByCat.getOrElse(c.id, Nil).map(toMatchingRule),
               tipo           = Some(c.tipo),
               pedigreeAssociation = c.pedigreeAssociation
             )
             categoryService.updateCategory(fc)
           })
      _ <- Future.sequence(modifications.map { case (f, t) => categoryService.registerCategoryModification(f, t) })
      _ <- categoryService.insertOrUpdateMapping(CategoryMappingList(mappings.map(m => CategoryMapping(m.id, m.idSuperior))))
    } yield Ok(Json.obj(
      "status"       -> "success",
      "groups"       -> groups.size,
      "categories"   -> categories.size,
      "configurations" -> configurations.size,
      "associations" -> associations.size,
      "aliases"      -> aliases.size,
      "matchingRules" -> matchingRules.size,
      "modifications" -> modifications.size,
      "mappings"     -> mappings.size
    ))
  }
}
