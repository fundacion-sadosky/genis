package fixtures

import configdata._
import types.AlphanumericId

import scala.concurrent.Future

object CategoryFixtures:

  val grpA = Group(AlphanumericId("GRP_A"), "Grupo A", None)
  val grpB = Group(AlphanumericId("GRP_B"), "Grupo B", Some("desc"))

  val catA = Category(AlphanumericId("CAT_A"), AlphanumericId("GRP_A"), "Categoria A", isReference = true, None)
  val catB = Category(AlphanumericId("CAT_B"), AlphanumericId("GRP_A"), "Categoria B", isReference = false, Some("desc B"))
  val catC = Category(AlphanumericId("CAT_C"), AlphanumericId("GRP_B"), "Categoria C", isReference = true, None)

  def mkFull(cat: Category, tipo: Option[Int] = None, pedigree: Boolean = false): FullCategory =
    FullCategory(
      id = cat.id, name = cat.name, description = cat.description,
      group = cat.group, isReference = cat.isReference,
      filiationDataRequired = false,
      configurations = Map.empty, associations = Nil, aliases = Nil, matchingRules = Nil,
      tipo = tipo, pedigreeAssociation = pedigree
    )

  val fcA        = mkFull(catA, tipo = Some(2))          // MPI
  val fcB        = mkFull(catB, tipo = Some(3))          // DVI
  val fcC        = mkFull(catC, tipo = Some(1))          // sin tipo
  val fcPedigree = mkFull(catA, pedigree = true)

class StubCategoryService extends CategoryService:
  import CategoryFixtures._

  override def categoryTree                    = Future.successful(Map(grpA -> Seq(catA)))
  override def listCategories                  = Future.successful(Map(catA.id -> fcA))
  override def listGroups                      = Future.successful(Seq(grpA))
  override def listCategoriesWithProfiles      = Future.successful(Map(catA.id -> catA.name))
  override def categoryTreeManualLoading       = Future.successful(Map(grpA -> Seq(catA)))
  override def listConfigurations              = Future.successful(Seq.empty)
  override def listAssociations                = Future.successful(Seq.empty)
  override def listAlias                       = Future.successful(Seq.empty)
  override def listMatchingRules               = Future.successful(Seq.empty)
  override def getCategory(id: AlphanumericId) = Future.successful(Some(fcA))
  override def getCategoryType(id: AlphanumericId) = Future.successful(None)
  override def getCategoryTypeFromFullCategory(fc: FullCategory) = None
  override def addCategory(cat: Category)      = Future.successful(Right(fcA))
  override def updateCategory(cat: Category)   = Future.successful(Right(1))
  override def updateCategory(cat: FullCategory) = Future.successful(Right(1))
  override def removeCategory(id: AlphanumericId) = Future.successful(Right(1))
  override def removeAllCategories()           = Future.successful(0)
  override def addGroup(g: Group)              = Future.successful(Right(g.id))
  override def updateGroup(g: Group)           = Future.successful(Right(1))
  override def removeGroup(id: AlphanumericId) = Future.successful(Right(1))
  override def removeAllGroups()               = Future.successful(0)
  override def insertOrUpdateMapping(m: CategoryMappingList) = Future.successful(Right(()))
  override def listCategoriesMapping           = Future.successful(List.empty)
  override def getCategoriesMappingById(id: AlphanumericId) = Future.successful(None)
  override def getCategoriesMappingReverseById(id: AlphanumericId) = Future.successful(None)
  override def registerCategoryModification(from: AlphanumericId, to: AlphanumericId) = Future.successful(Some(1))
  override def unregisterCategoryModification(from: AlphanumericId, to: AlphanumericId) = Future.successful(1)
  override def retrieveAllCategoryModificationAllowed = Future.successful(Seq.empty)
  override def getCategoryModificationAllowed(id: AlphanumericId) = Future.successful(Seq.empty)
  override def exportCategories(filePath: String) = Future.successful(Right("ok"))
