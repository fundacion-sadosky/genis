package configdata

import scala.concurrent.Future
import scala.language.postfixOps
import javax.inject.Inject
import javax.inject.Singleton
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import services.CacheService
import services.Keys
import types.AlphanumericId

import scala.util.Success
import scala.util.Try
import scala.util.Failure
import services.CacheService
import org.postgresql.util.PSQLException

import java.sql.SQLException
import scala.concurrent.Await
import scala.concurrent.duration._
import profile.Profile
import play.api.i18n.Messages
import play.api.libs.json.{JsValue, Json, Writes}

import java.io.PrintWriter
import models.Tables.{CategoryRow, Category => CategoryTable}

import javax.sql.DataSource
import scala.slick.driver.PostgresDriver.simple._

abstract class CategoryService {
  //def replaceCategories(categories: List[CategoryRow]): Future[Either[String, Unit]]

  def exportCategories(filePath: String): Either[String, String]

  def categoryTree: Category.CategoryTree

  def listCategories: Map[AlphanumericId, FullCategory]

  def listCategoriesWithProfiles: Map[AlphanumericId, String]

  def categoryTreeManualLoading: Category.CategoryTree

  def addCategory(category: Category): Future[Either[String, FullCategory]]

  def removeCategory(categoryId: AlphanumericId): Future[Either[String, Int]]

  def updateCategory(category: FullCategory): Future[Either[String, Int]]

  def addGroup(group: Group): Future[Either[String, AlphanumericId]]

  def removeGroup(groupId: AlphanumericId): Future[Either[String, Int]]

  def updateGroup(group: Group): Future[Either[String, Int]]

  def updateCategory(category: Category): Future[Either[String, Int]]

  def getCategory(categoryId: AlphanumericId): Option[FullCategory]

  def insertOrUpdateMapping(categoryMapping: CategoryMappingList): Future[Either[String, Unit]]

  def listCategoriesMapping: Future[List[FullCategoryMapping]]

  def getCategoriesMappingById(id: AlphanumericId): Future[Option[String]]

  def getCategoriesMappingReverseById(id: AlphanumericId): Future[Option[AlphanumericId]]

  def getCategoryType(categoryId: AlphanumericId): Option[String]

  def getCategoryTypeFromFullCategory(fullCategory: FullCategory): Option[String]

  /**
   * Register a new allowed category modification for undoubted profiles.
   *
   * @param from The category id of the category that will be modified.
   * @param to   The category id of the category that will be the result
   *             of the modification.
   * @return The number of new modifications registered.
   */
  def registerCategoryModification(
    from: AlphanumericId,
    to: AlphanumericId
  ): Option[Int]

  /**
   * Delete a registration of a category modification.
   *
   * @param from The category id of the category that will be modified.
   * @param to   The category id of the category that will be the result
   *             of the modification.
   * @return The number of modifications deleted.
   */
  def unregisterCategoryModification(
    from: AlphanumericId,
    to: AlphanumericId
  ): Int

  /**
   * Retrieve all the allowed category modifications for any undoubted profile.
   *
   * @return A sequence of tuples with the source and distination categories of each modification.
   */
  def retrieveAllCategoryModificationAllowed: Future[Seq[(AlphanumericId, AlphanumericId)]]

  /**
   * Retrieve the allowed categories modifications for a given undoubted profile.
   *
   * @param categoryId The undoubted profile category Id.
   * @return A sequence of tuples with the source and distination categories of each modification.
   */
  def getCategoryModificationAllowed(
    categoryId: AlphanumericId)
  : Future[Seq[AlphanumericId]]
}

@Singleton
class CachedCategoryService @Inject() (cache: CacheService, categoryRepository: CategoryRepository) extends CategoryService {

  
  // Serializador para CategoryConfiguration
  implicit val categoryConfigWrites: Writes[CategoryConfiguration] = Json.writes[CategoryConfiguration]

  // Serializador para FullCategory, incluyendo Map[Int, CategoryConfiguration]
  implicit val fullCategoryWrites: Writes[FullCategory] = new Writes[FullCategory] {
    def writes(fc: FullCategory): JsValue = Json.obj(
      "id" -> fc.id,
      "name" -> fc.name,
      "description" -> fc.description,
      "group" -> fc.group,
      "isReference" -> fc.isReference,
      "filiationDataRequired" -> fc.filiationDataRequired,
      "configurations" -> fc.configurations.map { case (k, v) => k.toString -> Json.toJson(v) },
      "associations" -> fc.associations,
      "aliases" -> fc.aliases,
      "matchingRules" -> fc.matchingRules
    )
  }

  override def exportCategories(filePath: String): Either[String, String] = {
    // Esto se está usando?
    Try {
      // Obtener las categorías desde el repositorio
      val futureCategories = categoryRepository.listCategories
      val categories = Await.result(futureCategories, 10.seconds)

      // Convertir a JSON
      val json = Json.toJson(categories)

      // Escribir en el archivo
      val writer = new PrintWriter(filePath)
      try {
        writer.write(Json.prettyPrint(json))
        Right(s"Exportación completada en: $filePath")
      } finally {
        writer.close()
      }
    } match {
      case Success(result) => result
      case Failure(exception) =>
        Left(s"Error durante la exportación: ${exception.getMessage}")
    }
  }
  private def cleanCache = {
    cache.pop(Keys.categories)
    cache.pop(Keys.categoryTree)
    cache.pop(Keys.categoryTreeManualLoading)
  }

  override def categoryTree: Category.CategoryTree = {
    cache.getOrElse(Keys.categoryTree) {
      val list = Await.result(categoryRepository.listGroupsAndCategories, Duration(10, SECONDS))
      list.groupBy(_._1).map { tup => (tup._1, tup._2 flatMap { _._2 }) }
    }
  }

  override def listCategories: Map[AlphanumericId, FullCategory] = {
    cache.getOrElse(Keys.categories) {
      val list = Await.result(categoryRepository.listCategories, Duration(10, SECONDS))
      list.map { category => category.id -> category }.toMap
    }
  }

  override def listCategoriesWithProfiles: Map[AlphanumericId,String] = {
   // cache.getOrElse(Keys.categories) {
      val list = Await.result(categoryRepository.listCategoriesWithProfiles, Duration(10, SECONDS))
      list.map { category => category.id -> category.name }.toMap
  //  }
  }

  override def categoryTreeManualLoading: Category.CategoryTree = {
    cache.getOrElse(Keys.categoryTreeManualLoading) {
      val list = Await.result(categoryRepository.listGroupsAndCategoriesManualLoading, Duration(10, SECONDS))
      list.groupBy(_._1).map { tup => (tup._1, tup._2 flatMap { _._2 }) }
    }
  }

  override def addCategory(category: Category): Future[Either[String, FullCategory]] = {
    val fc = FullCategory(
      category.id,
      category.name,
      category.description,
      category.group,
      category.isReference,
      filiationDataRequired = false,
      configurations = Map.empty,
      associations = Nil,
      //catReleated = Nil,
      aliases = Nil,
      matchingRules = Nil)
    val promise = categoryRepository.addCategory(category)
    promise.foreach { _ => cleanCache }
    promise
      .map { _ => Right(fc) }
      .recover { case e: SQLException if e.getSQLState.startsWith("23") => Left(Messages("error.E0664")) }
  }

  override def removeCategory(categoryId: AlphanumericId): Future[Either[String, Int]] = {
    val promise = categoryRepository.runInTransactionAsync { implicit session =>

      val deleteAliasesResult = categoryRepository.deleteAliases(categoryId)
      val deleteAssociationsResult = deleteAliasesResult.fold(Left(_), r=>categoryRepository.deleteAssociations(categoryId))
      val deleteMatchingRulesResult = deleteAssociationsResult.fold(Left(_), r=>categoryRepository.deleteMatchingRules(categoryId))
      val deleteConfigurationsResult = deleteMatchingRulesResult.fold(Left(_), r=>categoryRepository.deleteConfigurations(categoryId))

      val category = this.getCategory(categoryId).getOrElse(throw new Exception("Category not found"))
      val matchingRules = category.matchingRules.filterNot(p => categoryId == p.categoryRelated)

      val deleteReciprocateRulesResult = deleteConfigurationsResult.fold(Left(_), r =>
        matchingRules.foldLeft[Either[String,AlphanumericId]](Right(r)){
          case (prev,current) => prev.fold(Left(_),r=>categoryRepository.deleteMatchingRule(current.categoryRelated, categoryId))
        })


      val removeCategoryResult = deleteReciprocateRulesResult.fold(Left(_), r=>categoryRepository.removeCategory(categoryId))
      removeCategoryResult
    }

    promise.foreach { _ => cleanCache }
    promise
      .map { _ => Right(1) }
      .recover { case e: SQLException if e.getSQLState.startsWith("23") => Left(Messages("error.E0665")) }
    
    
    //promise.map { either => either.fold(Left(_), r=>Right(1)) }
    ///
//    val promise = categoryRepository.removeCategory(categoryId)
//    promise.foreach { _ => cleanCache }
//    promise
//      .map { Right(_) }
//      .recover { case e: SQLException if e.getSQLState.startsWith("23") => Left(Messages("error.E0665")) }
  }

  override def updateCategory(category: FullCategory): Future[Either[String, Int]] = {
    val promise = categoryRepository.runInTransactionAsync { implicit session =>

      val deleteAliasesResult = categoryRepository.deleteAliases(category.id)
      val deleteAssociationsResult = deleteAliasesResult.fold(Left(_), r=>categoryRepository.deleteAssociations(category.id))
      val deleteMatchingRulesResult = deleteAssociationsResult.fold(Left(_), r=>categoryRepository.deleteMatchingRules(category.id))
      val deleteConfigurationsResult = deleteMatchingRulesResult.fold(Left(_), r=>categoryRepository.deleteConfigurations(category.id))

      val matchingRules = category.matchingRules.filterNot(p => category.id == p.categoryRelated)

      val deleteReciprocateRulesResult = deleteConfigurationsResult.fold(Left(_), r =>
        matchingRules.foldLeft[Either[String,AlphanumericId]](Right(r)){
          case (prev,current) => prev.fold(Left(_),r=>categoryRepository.deleteMatchingRule(current.categoryRelated, category.id))
      })

      val updateResult = deleteReciprocateRulesResult.fold(Left(_), r=>categoryRepository.updateFullCategory(category))

      val addAliasesResult = updateResult.fold(Left(_), r=>categoryRepository.addAliases(category.id, category.aliases))
      val addAssociationsResult = addAliasesResult.fold(Left(_), r=>categoryRepository.addAssociations(category.id, category.associations))
      val addMatchingRulesResult = addAssociationsResult.fold(Left(_), r=>categoryRepository.addMatchingRules(category.id, category.matchingRules))
      val addConfigurationsResult = addMatchingRulesResult.fold(Left(_), r=>categoryRepository.addConfigurations(category.id, category.configurations))

      val addReciprocateRulesResult = addConfigurationsResult.fold(Left(_), r =>
        matchingRules.foldLeft[Either[String,AlphanumericId]](Right(r)){
          case (prev,current) => prev.fold(Left(_),r=>
            categoryRepository.addMatchingRules(current.categoryRelated,
              Seq(MatchingRule(
                current.`type`, category.id, current.minimumStringency, current.failOnMatch,
                current.forwardToUpper, current.matchingAlgorithm,
                current.minLocusMatch, current.mismatchsAllowed, current.considerForN))))
        })

      addReciprocateRulesResult
    }

    promise.foreach { _ => cleanCache }
    promise.map { either => either.fold(Left(_), r=>Right(1)) }
  }

  override def addGroup(group: Group): Future[Either[String, AlphanumericId]] = {
    val promise = categoryRepository.addGroup(group)
    promise.foreach { _ => cleanCache }
    promise
      .map { Right(_) }
      .recover { case e: SQLException if e.getSQLState.startsWith("23") => Left(Messages("error.E0670")) }
  }

  override def removeGroup(groupId: AlphanumericId): Future[Either[String, Int]] = {
    val promise = categoryRepository.removeGroup(groupId)
    promise.foreach { _ => cleanCache }
    promise
      .map { Right(_) }
      .recover { case e: SQLException if e.getSQLState.startsWith("23") => Left(Messages("error.E0671")) }
  }

  override def updateGroup(group: Group): Future[Either[String, Int]] = {
    val promise = categoryRepository.updateGroup(group)
    promise.foreach { _ => cleanCache }
    promise.map { Right(_) }
  }

  override def updateCategory(category: Category) = {
    val promise = categoryRepository.updateCategory(category)
    promise.foreach { _ => cleanCache }
    promise.map { Right(_) }
  }

  override def getCategory(categoryId: AlphanumericId): Option[FullCategory] = {
    this.listCategories.get(categoryId)
  }
  override def getCategoryType(categoryId: AlphanumericId): Option[String] = {
     this.listCategories.get(categoryId).flatMap(fullCategory => {
       getCategoryTypeFromFullCategory(fullCategory)
     })
  }
  override def getCategoryTypeFromFullCategory(fullCategory: FullCategory): Option[String] = {
      if (fullCategory.tipo.contains(2)) {
        Some("MPI")
      } else if (fullCategory.tipo.contains(3)) {
        Some("DVI")
      } else {
        None
      }
  }

  override def insertOrUpdateMapping(categoryMapping: CategoryMappingList): Future[Either[String, Unit]] = {
    categoryRepository.insertOrUpdateMapping(categoryMapping)
  }

  override def listCategoriesMapping: Future[List[FullCategoryMapping]] = {
    categoryRepository.listCategoriesMapping
  }
  override def getCategoriesMappingById(id:AlphanumericId): Future[Option[String]] = {
    if(isPedigreeAssociation(id)){
      Future.successful(Some(id.text))
    }else{
      categoryRepository.getCategoriesMappingById(id)
    }
  }
  override def getCategoriesMappingReverseById(id:AlphanumericId): Future[Option[AlphanumericId]] = {
    if(isPedigreeAssociation(id)){
      Future.successful(Some(id))
    }else{
      categoryRepository.getCategoriesMappingReverseById(id)
    }
  }
  def isPedigreeAssociation(id:AlphanumericId):Boolean = listCategories(id).pedigreeAssociation

  override def registerCategoryModification(
    from: AlphanumericId,
    to: AlphanumericId
  ): Option[Int] = {
    if (from == to) { None } else {
      val future = categoryRepository
        .categoryModificationExists(from, to)
        .flatMap {
          case true => Future.successful(None)
          case false =>
            categoryRepository
              .addCategoryModification(from, to)
              .map(Some(_))
        }
      Await
        .result(future, Duration(300, SECONDS))
    }
  }

  override def unregisterCategoryModification(
    from: AlphanumericId,
    to: AlphanumericId
  ): Int = {
    val future = categoryRepository
      .removeCategoryModification(from, to)
    Await
      .result(future, Duration(300, SECONDS))
  }

  override def retrieveAllCategoryModificationAllowed:
    Future[Seq[(AlphanumericId, AlphanumericId)]] = {
      categoryRepository
        .getAllCategoryModificationsAllowed
    }

  override def getCategoryModificationAllowed(
    categoryId: AlphanumericId
  )
  : Future[Seq[AlphanumericId]] = {
    categoryRepository
      .getCategoryModificationsAllowed(categoryId)
  }
}
