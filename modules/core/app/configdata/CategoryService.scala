package configdata

import java.sql.SQLException
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.i18n.MessagesApi
import scala.concurrent.{ExecutionContext, Future}
import services.{CacheService, CategoriesKey, CategoryTreeKey, CategoryTreeManualLoadingKey}
import types.AlphanumericId

trait CategoryService {
  def categoryTree: Future[Category.CategoryTree]
  def listCategories: Future[Map[AlphanumericId, FullCategory]]
  def listGroups: Future[Seq[Group]]
  def listCategoriesWithProfiles: Future[Map[AlphanumericId, String]]
  def categoryTreeManualLoading: Future[Category.CategoryTree]
  def listConfigurations: Future[Seq[CategoryConfigurationExport]]
  def listAssociations: Future[Seq[CategoryAssociationExport]]
  def listAlias: Future[Seq[CategoryAliasExport]]
  def listMatchingRules: Future[Seq[CategoryMatchingExport]]

  def getCategory(categoryId: AlphanumericId): Future[Option[FullCategory]]
  def getCategoryType(categoryId: AlphanumericId): Future[Option[String]]
  def getCategoryTypeFromFullCategory(fc: FullCategory): Option[String]

  def addCategory(category: Category): Future[Either[String, FullCategory]]
  def updateCategory(category: Category): Future[Either[String, Int]]
  def updateCategory(category: FullCategory): Future[Either[String, Int]]
  def removeCategory(categoryId: AlphanumericId): Future[Either[String, Int]]
  def removeAllCategories(): Future[Int]

  def addGroup(group: Group): Future[Either[String, AlphanumericId]]
  def updateGroup(group: Group): Future[Either[String, Int]]
  def removeGroup(groupId: AlphanumericId): Future[Either[String, Int]]
  def removeAllGroups(): Future[Int]

  def insertOrUpdateMapping(mappings: CategoryMappingList): Future[Either[String, Unit]]
  def listCategoriesMapping: Future[List[FullCategoryMapping]]
  def getCategoriesMappingById(id: AlphanumericId): Future[Option[String]]
  def getCategoriesMappingReverseById(id: AlphanumericId): Future[Option[AlphanumericId]]

  def registerCategoryModification(from: AlphanumericId, to: AlphanumericId): Future[Option[Int]]
  def unregisterCategoryModification(from: AlphanumericId, to: AlphanumericId): Future[Int]
  def retrieveAllCategoryModificationAllowed: Future[Seq[(AlphanumericId, AlphanumericId)]]
  def getCategoryModificationAllowed(categoryId: AlphanumericId): Future[Seq[AlphanumericId]]

  def exportCategories(filePath: String): Future[Either[String, String]]
}

@Singleton
class CategoryServiceImpl @Inject()(
  repo: CategoryRepository,
  cache: CacheService,
  messagesApi: MessagesApi
)(implicit ec: ExecutionContext) extends CategoryService {

  private val logger = Logger(this.getClass)

  private implicit val messages: play.api.i18n.Messages =
    messagesApi.preferred(Seq.empty)

  private def cleanCache(): Unit = {
    cache.pop(CategoriesKey)
    cache.pop(CategoryTreeKey)
    cache.pop(CategoryTreeManualLoadingKey)
  }

  override def categoryTree: Future[Category.CategoryTree] =
    cache.asyncGetOrElse(CategoryTreeKey) {
      repo.listGroupsAndCategories.map(list =>
        list.groupBy(_._1).map { case (group, pairs) => group -> pairs.flatMap(_._2) }
      )
    }

  override def listCategories: Future[Map[AlphanumericId, FullCategory]] =
    cache.asyncGetOrElse(CategoriesKey) {
      repo.listCategories.map(_.map(fc => fc.id -> fc).toMap)
    }

  override def listGroups: Future[Seq[Group]] =
    repo.listGroupsAndCategories.map(_.map(_._1).distinct)

  override def listCategoriesWithProfiles: Future[Map[AlphanumericId, String]] =
    repo.listCategoriesWithProfiles.map(_.map(c => c.id -> c.name).toMap)

  override def categoryTreeManualLoading: Future[Category.CategoryTree] =
    cache.asyncGetOrElse(CategoryTreeManualLoadingKey) {
      repo.listGroupsAndCategoriesManualLoading.map(list =>
        list.groupBy(_._1).map { case (group, pairs) => group -> pairs.flatMap(_._2) }
      )
    }

  override def listConfigurations = repo.listConfigurations
  override def listAssociations   = repo.listAssociations
  override def listAlias          = repo.listAlias
  override def listMatchingRules  = repo.listMatchingRules

  override def getCategory(categoryId: AlphanumericId): Future[Option[FullCategory]] =
    listCategories.map(_.get(categoryId))

  override def getCategoryType(categoryId: AlphanumericId): Future[Option[String]] =
    getCategory(categoryId).map(_.flatMap(getCategoryTypeFromFullCategory))

  override def getCategoryTypeFromFullCategory(fc: FullCategory): Option[String] =
    if (fc.tipo.contains(2)) Some("MPI")
    else if (fc.tipo.contains(3)) Some("DVI")
    else None

  override def addCategory(category: Category): Future[Either[String, FullCategory]] = {
    val fc = FullCategory(category.id, category.name, category.description, category.group,
      category.isReference, filiationDataRequired = false,
      configurations = Map.empty, associations = Nil, aliases = Nil, matchingRules = Nil)
    repo.addCategory(category)
      .map { _ => cleanCache(); Right(fc) }
      .recover { case e: SQLException if e.getSQLState.startsWith("23") =>
        Left(messages("error.E0664") + category.name)
      }
  }

  override def updateCategory(category: Category): Future[Either[String, Int]] =
    repo.updateCategory(category).map { n => cleanCache(); Right(n) }

  override def updateCategory(category: FullCategory): Future[Either[String, Int]] =
    repo.updateFullCategory(category).map { r => cleanCache(); r.map(_ => 1) }

  override def removeCategory(categoryId: AlphanumericId): Future[Either[String, Int]] =
    repo.removeCategory(categoryId).map { r => cleanCache(); r.map(_ => 1) }
      .recover { case e: SQLException if e.getSQLState.startsWith("23") =>
        Left(messages("error.E0665"))
      }

  override def removeAllCategories(): Future[Int] =
    repo.removeAllCategories().map { n => cleanCache(); n }

  override def addGroup(group: Group): Future[Either[String, AlphanumericId]] =
    repo.addGroup(group).map { id => cleanCache(); Right(id) }
      .recover { case e: SQLException if e.getSQLState.startsWith("23") =>
        Left(messages("error.E0670"))
      }

  override def updateGroup(group: Group): Future[Either[String, Int]] =
    repo.updateGroup(group).map { n => cleanCache(); Right(n) }

  override def removeGroup(groupId: AlphanumericId): Future[Either[String, Int]] =
    repo.removeGroup(groupId).map { n => cleanCache(); Right(n) }
      .recover { case e: SQLException if e.getSQLState.startsWith("23") =>
        Left(messages("error.E0671"))
      }

  override def removeAllGroups(): Future[Int] =
    repo.removeAllGroups().map { n => cleanCache(); n }

  override def insertOrUpdateMapping(mappings: CategoryMappingList): Future[Either[String, Unit]] =
    repo.insertOrUpdateMapping(mappings)

  override def listCategoriesMapping: Future[List[FullCategoryMapping]] =
    repo.listCategoriesMapping

  override def getCategoriesMappingById(id: AlphanumericId): Future[Option[String]] =
    listCategories.flatMap { cats =>
      if (cats.get(id).exists(_.pedigreeAssociation)) Future.successful(Some(id.text))
      else repo.getCategoriesMappingById(id)
    }

  override def getCategoriesMappingReverseById(id: AlphanumericId): Future[Option[AlphanumericId]] =
    listCategories.flatMap { cats =>
      if (cats.get(id).exists(_.pedigreeAssociation)) Future.successful(Some(id))
      else repo.getCategoriesMappingReverseById(id)
    }

  override def registerCategoryModification(from: AlphanumericId, to: AlphanumericId): Future[Option[Int]] =
    if (from == to) Future.successful(None)
    else repo.categoryModificationExists(from, to).flatMap {
      case true  => Future.successful(None)
      case false => repo.addCategoryModification(from, to).map(Some(_))
    }

  override def unregisterCategoryModification(from: AlphanumericId, to: AlphanumericId): Future[Int] =
    repo.removeCategoryModification(from, to)

  override def retrieveAllCategoryModificationAllowed: Future[Seq[(AlphanumericId, AlphanumericId)]] =
    repo.getAllCategoryModificationsAllowed

  override def getCategoryModificationAllowed(categoryId: AlphanumericId): Future[Seq[AlphanumericId]] =
    repo.getCategoryModificationsAllowed(categoryId)

  override def exportCategories(filePath: String): Future[Either[String, String]] = {
    import play.api.libs.json.Json
    repo.listCategories.map { categories =>
      val json   = Json.toJson(categories)
      val writer = new java.io.PrintWriter(filePath)
      try { writer.write(Json.prettyPrint(json)); Right(s"Exportación completada en: $filePath") }
      finally { writer.close() }
    }.recover { case e =>
        logger.error("Error durante la exportación de categorías", e)
        Left("Error durante la exportación de categorías")
      }
  }
}
