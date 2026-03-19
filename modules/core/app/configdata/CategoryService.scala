package configdata


import types.AlphanumericId
import scala.concurrent.Future
import models.{FullCategory, Group, CategoryConfigurationRow, CategoryAssociationRow, CategoryAliasRow, CategoryMatchingRow}
import models.categorytypes._

abstract class CategoryService {
  def exportCategories(filePath: String): Either[String, String]
  def categoryTree: CategoryTree
  def listCategories: Map[AlphanumericId, FullCategory]
  def listGroups: Future[Seq[Group]]
  def listConfigurations: Future[Seq[CategoryConfigurationRow]]
  def listAssociations: Future[Seq[CategoryAssociationRow]]
  def listAlias: Future[Seq[CategoryAliasRow]]
  def listMatchingRules: Future[Seq[CategoryMatchingRow]]
  def listCategoriesWithProfiles: Map[AlphanumericId, String]
  def categoryTreeManualLoading: CategoryTree
  def addCategory(category: Category): Future[Either[String, FullCategory]]
  def removeCategory(categoryId: AlphanumericId): Future[Either[String, Int]]
  def removeAllCategories(): Future[Int]
  def updateCategory(category: FullCategory): Future[Either[String, Int]]
  def addGroup(group: Group): Future[Either[String, AlphanumericId]]
  def removeGroup(groupId: AlphanumericId): Future[Either[String, Int]]
  def removeAllGroups(): Future[Int]
  def updateGroup(group: Group): Future[Either[String, Int]]
  def updateCategory(category: Category): Future[Either[String, Int]]
  def getCategory(categoryId: AlphanumericId): Option[FullCategory]
  def insertOrUpdateMapping(categoryMapping: CategoryMappingList): Future[Either[String, Unit]]
  def listCategoriesMapping: Future[List[FullCategoryMapping]]
  def getCategoriesMappingById(id: AlphanumericId): Future[Option[String]]
  def getCategoriesMappingReverseById(id: AlphanumericId): Future[Option[AlphanumericId]]
  def getCategoryType(categoryId: AlphanumericId): Option[String]
  def getCategoryTypeFromFullCategory(fullCategory: FullCategory): Option[String]
}
