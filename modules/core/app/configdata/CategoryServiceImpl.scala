package configdata

import javax.inject.Singleton
import types.AlphanumericId
import models.{FullCategory, Group, CategoryConfigurationRow, CategoryAssociationRow, CategoryAliasRow, CategoryMatchingRow}
import models.categorytypes._
import scala.concurrent.Future

@Singleton
class CategoryServiceImpl extends CategoryService {
  // Métodos stub: devuelven valores vacíos o nulos
  def exportCategories(filePath: String) = Left("Not implemented")
  def categoryTree: CategoryTree = null
  def listCategories: Map[AlphanumericId, FullCategory] = Map.empty
  def listGroups: Future[Seq[Group]] = Future.successful(Seq.empty)
  def listConfigurations: Future[Seq[CategoryConfigurationRow]] = Future.successful(Seq.empty)
  def listAssociations: Future[Seq[CategoryAssociationRow]] = Future.successful(Seq.empty)
  def listAlias: Future[Seq[CategoryAliasRow]] = Future.successful(Seq.empty)
  def listMatchingRules: Future[Seq[CategoryMatchingRow]] = Future.successful(Seq.empty)
  def listCategoriesWithProfiles: Map[AlphanumericId, String] = Map.empty
  def categoryTreeManualLoading: CategoryTree = null
  def addCategory(category: Category) = Future.successful(Left("Not implemented"))
  def removeCategory(categoryId: AlphanumericId) = Future.successful(Left("Not implemented"))
  def removeAllCategories() = Future.successful(0)
  def updateCategory(category: FullCategory) = Future.successful(Left("Not implemented"))
  def addGroup(group: Group) = Future.successful(Left("Not implemented"))
  def removeGroup(groupId: AlphanumericId) = Future.successful(Left("Not implemented"))
  def removeAllGroups() = Future.successful(0)
  def updateGroup(group: Group) = Future.successful(Left("Not implemented"))
  def updateCategory(category: Category) = Future.successful(Left("Not implemented"))
  def getCategory(categoryId: AlphanumericId) = None
  def insertOrUpdateMapping(categoryMapping: CategoryMappingList) = Future.successful(Left("Not implemented"))
  def listCategoriesMapping = Future.successful(List.empty)
  def getCategoriesMappingById(id: AlphanumericId) = Future.successful(None)
  def getCategoriesMappingReverseById(id: AlphanumericId) = Future.successful(None)
  def getCategoryType(categoryId: AlphanumericId) = None
  def getCategoryTypeFromFullCategory(fullCategory: FullCategory) = None
}
