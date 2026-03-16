package configdata

import fixtures.CategoryFixtures._
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import types.AlphanumericId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}

class CategoryServiceTest extends AnyWordSpec with Matchers with MockitoSugar {

  val duration = Duration(10, SECONDS)

  // ─── categoryTree ──────────────────────────────────────────────────────────

  "CategoryServiceImpl.categoryTree" must {

    "group categories by group" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.listGroupsAndCategories).thenReturn(Future.successful(Seq(
        grpA -> Some(catA),
        grpA -> Some(catB),
        grpB -> Some(catC)
      )))

      val result = Await.result(service.categoryTree, duration)
      result(grpA) must contain(catA)
      result(grpA) must contain(catB)
      result(grpB) must contain(catC)
    }
  }

  // ─── listCategories ────────────────────────────────────────────────────────

  "CategoryServiceImpl.listCategories" must {

    "return a map keyed by AlphanumericId" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.listCategories).thenReturn(Future.successful(Seq(fcA, fcB)))

      val result = Await.result(service.listCategories, duration)
      result.keys must contain(AlphanumericId("CAT_A"))
      result(AlphanumericId("CAT_A")) mustBe fcA
      result(AlphanumericId("CAT_B")) mustBe fcB
    }
  }

  // ─── listGroups ────────────────────────────────────────────────────────────

  "CategoryServiceImpl.listGroups" must {

    "return distinct groups" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.listGroupsAndCategories).thenReturn(Future.successful(Seq(
        grpA -> Some(catA),
        grpA -> Some(catB),
        grpB -> Some(catC)
      )))

      val result = Await.result(service.listGroups, duration)
      result must have size 2
      result must contain(grpA)
      result must contain(grpB)
    }
  }

  // ─── listCategoriesWithProfiles ────────────────────────────────────────────

  "CategoryServiceImpl.listCategoriesWithProfiles" must {

    "return map id -> name" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.listCategoriesWithProfiles).thenReturn(Future.successful(List(catA, catB)))

      val result = Await.result(service.listCategoriesWithProfiles, duration)
      result(AlphanumericId("CAT_A")) mustBe "Categoria A"
      result(AlphanumericId("CAT_B")) mustBe "Categoria B"
    }
  }

  // ─── getCategory ───────────────────────────────────────────────────────────

  "CategoryServiceImpl.getCategory" must {

    "return Some when category exists" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.listCategories).thenReturn(Future.successful(Seq(fcA)))

      val result = Await.result(service.getCategory(AlphanumericId("CAT_A")), duration)
      result mustBe Some(fcA)
    }

    "return None when category does not exist" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.listCategories).thenReturn(Future.successful(Seq(fcA)))

      val result = Await.result(service.getCategory(AlphanumericId("UNKNOWN")), duration)
      result mustBe None
    }
  }

  // ─── getCategoryTypeFromFullCategory ───────────────────────────────────────

  "CategoryServiceImpl.getCategoryTypeFromFullCategory" must {

    "return MPI when tipo is 2" in {
      val service = new CategoryServiceImpl(mock[CategoryRepository])
      service.getCategoryTypeFromFullCategory(fcA) mustBe Some("MPI")
    }

    "return DVI when tipo is 3" in {
      val service = new CategoryServiceImpl(mock[CategoryRepository])
      service.getCategoryTypeFromFullCategory(fcB) mustBe Some("DVI")
    }

    "return None for other tipo values" in {
      val service = new CategoryServiceImpl(mock[CategoryRepository])
      service.getCategoryTypeFromFullCategory(fcC) mustBe None
    }

    "return None when tipo is absent" in {
      val service = new CategoryServiceImpl(mock[CategoryRepository])
      service.getCategoryTypeFromFullCategory(mkFull(catA)) mustBe None
    }
  }

  // ─── addCategory ───────────────────────────────────────────────────────────

  "CategoryServiceImpl.addCategory" must {

    "return Right with FullCategory on success" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.addCategory(catA)).thenReturn(Future.successful(catA.id))

      val result = Await.result(service.addCategory(catA), duration)
      result.isRight mustBe true
      result.toOption.get.id mustBe catA.id
      result.toOption.get.name mustBe catA.name
    }

    "return Left with error message on failure" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.addCategory(catA)).thenReturn(Future.failed(new RuntimeException("DB error")))

      val result = Await.result(service.addCategory(catA), duration)
      result mustBe Left("DB error")
    }
  }

  // ─── addGroup ───────────────────────────────────────────────────────────────

  "CategoryServiceImpl.addGroup" must {

    "return Right with id on success" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.addGroup(grpA)).thenReturn(Future.successful(grpA.id))

      val result = Await.result(service.addGroup(grpA), duration)
      result mustBe Right(grpA.id)
    }

    "return Left with error message on failure" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.addGroup(grpA)).thenReturn(Future.failed(new RuntimeException("duplicate key")))

      val result = Await.result(service.addGroup(grpA), duration)
      result mustBe Left("duplicate key")
    }
  }

  // ─── registerCategoryModification ─────────────────────────────────────────

  "CategoryServiceImpl.registerCategoryModification" must {

    "return None when from equals to" in {
      val service = new CategoryServiceImpl(mock[CategoryRepository])
      val id      = AlphanumericId("CAT_A")

      val result = Await.result(service.registerCategoryModification(id, id), duration)
      result mustBe None
    }

    "return None when modification already exists" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      val from    = AlphanumericId("CAT_A")
      val to      = AlphanumericId("CAT_B")
      when(repo.categoryModificationExists(from, to)).thenReturn(Future.successful(true))

      val result = Await.result(service.registerCategoryModification(from, to), duration)
      result mustBe None
    }

    "return Some with inserted rows when modification is new" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      val from    = AlphanumericId("CAT_A")
      val to      = AlphanumericId("CAT_B")
      when(repo.categoryModificationExists(from, to)).thenReturn(Future.successful(false))
      when(repo.addCategoryModification(from, to)).thenReturn(Future.successful(1))

      val result = Await.result(service.registerCategoryModification(from, to), duration)
      result mustBe Some(1)
    }
  }

  // ─── getCategoriesMappingById ──────────────────────────────────────────────

  "CategoryServiceImpl.getCategoriesMappingById" must {

    "return the id directly when category has pedigreeAssociation" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.listCategories).thenReturn(Future.successful(Seq(fcPedigree)))

      val result = Await.result(service.getCategoriesMappingById(AlphanumericId("CAT_A")), duration)
      result mustBe Some("CAT_A")
    }

    "delegate to repo when category has no pedigreeAssociation" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.listCategories).thenReturn(Future.successful(Seq(fcA)))
      when(repo.getCategoriesMappingById(AlphanumericId("CAT_A")))
        .thenReturn(Future.successful(Some("CAT_SUPERIOR")))

      val result = Await.result(service.getCategoriesMappingById(AlphanumericId("CAT_A")), duration)
      result mustBe Some("CAT_SUPERIOR")
    }
  }

  // ─── getCategoryType ─────────────────────────────────────────────────────

  "CategoryServiceImpl.getCategoryType" must {

    "return MPI when category has tipo 2" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.listCategories).thenReturn(Future.successful(Seq(fcA)))

      val result = Await.result(service.getCategoryType(AlphanumericId("CAT_A")), duration)
      result mustBe Some("MPI")
    }

    "return DVI when category has tipo 3" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.listCategories).thenReturn(Future.successful(Seq(fcB)))

      val result = Await.result(service.getCategoryType(AlphanumericId("CAT_B")), duration)
      result mustBe Some("DVI")
    }

    "return None when category does not exist" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.listCategories).thenReturn(Future.successful(Seq.empty))

      val result = Await.result(service.getCategoryType(AlphanumericId("UNKNOWN")), duration)
      result mustBe None
    }
  }

  // ─── categoryTreeManualLoading ─────────────────────────────────────────────

  "CategoryServiceImpl.categoryTreeManualLoading" must {

    "group categories by group using manual loading repo method" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.listGroupsAndCategoriesManualLoading).thenReturn(Future.successful(Seq(
        grpA -> Some(catA),
        grpB -> Some(catC)
      )))

      val result = Await.result(service.categoryTreeManualLoading, duration)
      result(grpA) must contain(catA)
      result(grpB) must contain(catC)
    }
  }

  // ─── updateCategory ────────────────────────────────────────────────────────

  // TODO: agregar tests para updateCategory(Category) cuando el repo tenga
  // un método updateCategory. Actualmente el service llama a repo.addCategory
  // por error (bug conocido).

  "CategoryServiceImpl.updateCategory(FullCategory)" must {

    "return Right(1) on success" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.updateFullCategory(fcA)).thenReturn(Future.successful(Right(fcA.id)))

      val result = Await.result(service.updateCategory(fcA), duration)
      result mustBe Right(1)
    }

    "return Left on repo failure" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.updateFullCategory(fcA)).thenReturn(Future.successful(Left("update failed")))

      val result = Await.result(service.updateCategory(fcA), duration)
      result mustBe Left("update failed")
    }
  }

  // ─── removeCategory ────────────────────────────────────────────────────────

  "CategoryServiceImpl.removeCategory" must {

    "return Right(1) on success" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.removeCategory(catA.id)).thenReturn(Future.successful(Right(())))

      val result = Await.result(service.removeCategory(catA.id), duration)
      result mustBe Right(1)
    }

    "return Left on repo failure" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.removeCategory(catA.id)).thenReturn(Future.successful(Left("FK constraint")))

      val result = Await.result(service.removeCategory(catA.id), duration)
      result mustBe Left("FK constraint")
    }
  }

  // ─── updateGroup ───────────────────────────────────────────────────────────

  "CategoryServiceImpl.updateGroup" must {

    "return Right(1) on success" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.updateGroup(grpA)).thenReturn(Future.successful(1))

      val result = Await.result(service.updateGroup(grpA), duration)
      result mustBe Right(1)
    }

    "return Left with error message on failure" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.updateGroup(grpA)).thenReturn(Future.failed(new RuntimeException("duplicate name")))

      val result = Await.result(service.updateGroup(grpA), duration)
      result mustBe Left("duplicate name")
    }
  }

  // ─── removeGroup (error path) ──────────────────────────────────────────────

  "CategoryServiceImpl.removeGroup" must {

    "return Right with rows affected" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.removeGroup(grpA.id)).thenReturn(Future.successful(1))

      val result = Await.result(service.removeGroup(grpA.id), duration)
      result mustBe Right(1)
    }

    "return Left with error message on failure" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.removeGroup(grpA.id)).thenReturn(Future.failed(new RuntimeException("has children")))

      val result = Await.result(service.removeGroup(grpA.id), duration)
      result mustBe Left("has children")
    }
  }

  // ─── unregisterCategoryModification ────────────────────────────────────────

  "CategoryServiceImpl.unregisterCategoryModification" must {

    "delegate to repo and return rows affected" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      val from    = AlphanumericId("CAT_A")
      val to      = AlphanumericId("CAT_B")
      when(repo.removeCategoryModification(from, to)).thenReturn(Future.successful(1))

      val result = Await.result(service.unregisterCategoryModification(from, to), duration)
      result mustBe 1
    }

    "return 0 when modification does not exist" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      val from    = AlphanumericId("CAT_A")
      val to      = AlphanumericId("CAT_B")
      when(repo.removeCategoryModification(from, to)).thenReturn(Future.successful(0))

      val result = Await.result(service.unregisterCategoryModification(from, to), duration)
      result mustBe 0
    }
  }

  // ─── insertOrUpdateMapping ─────────────────────────────────────────────────

  "CategoryServiceImpl.insertOrUpdateMapping" must {

    "delegate to repo and return Right on success" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      val mappings = CategoryMappingList(List(CategoryMapping(AlphanumericId("CAT_A"), "CAT_SUP")))
      when(repo.insertOrUpdateMapping(mappings)).thenReturn(Future.successful(Right(())))

      val result = Await.result(service.insertOrUpdateMapping(mappings), duration)
      result mustBe Right(())
    }

    "delegate to repo and return Left on failure" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      val mappings = CategoryMappingList(List(CategoryMapping(AlphanumericId("BAD"), "nope")))
      when(repo.insertOrUpdateMapping(mappings)).thenReturn(Future.successful(Left("FK error")))

      val result = Await.result(service.insertOrUpdateMapping(mappings), duration)
      result mustBe Left("FK error")
    }
  }

  // ─── exportCategories ──────────────────────────────────────────────────────

  "CategoryServiceImpl.exportCategories" must {

    "write JSON to file and return Right on success" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.listCategories).thenReturn(Future.successful(Seq(fcA)))

      val tmpFile = java.io.File.createTempFile("test-export-", ".json")
      tmpFile.deleteOnExit()

      val result = Await.result(service.exportCategories(tmpFile.getAbsolutePath), duration)
      result.isRight mustBe true

      val content = scala.io.Source.fromFile(tmpFile).mkString
      content must include("CAT_A")
    }

    "return Left with error message on failure" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.listCategories).thenReturn(Future.failed(new RuntimeException("connection lost")))

      val result = Await.result(service.exportCategories("/tmp/test-export.json"), duration)
      result.isLeft mustBe true
      result.swap.toOption.get must include("connection lost")
    }
  }

  // ─── getCategoriesMappingReverseById ───────────────────────────────────────

  "CategoryServiceImpl.getCategoriesMappingReverseById" must {

    "return the id directly when category has pedigreeAssociation" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      when(repo.listCategories).thenReturn(Future.successful(Seq(fcPedigree)))
      val id = AlphanumericId("CAT_A")

      val result = Await.result(service.getCategoriesMappingReverseById(id), duration)
      result mustBe Some(id)
    }

    "delegate to repo when category has no pedigreeAssociation" in {
      val repo    = mock[CategoryRepository]
      val service = new CategoryServiceImpl(repo)
      val superior = AlphanumericId("CAT_SUPERIOR")
      when(repo.listCategories).thenReturn(Future.successful(Seq(fcA)))
      when(repo.getCategoriesMappingReverseById(AlphanumericId("CAT_A")))
        .thenReturn(Future.successful(Some(superior)))

      val result = Await.result(service.getCategoriesMappingReverseById(AlphanumericId("CAT_A")), duration)
      result mustBe Some(superior)
    }
  }
}
