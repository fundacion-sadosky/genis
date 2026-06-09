package configdata

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import specs.PdgSpec
import types.AlphanumericId
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import matching.{Algorithm, Stringency}

import scala.util.Random

class CategoryRepositoryTest extends PdgSpec {

  val duration = Duration(10, SECONDS)

  // Test data for categories that the tests depend on
  val testGroupId = AlphanumericId("TEST_GROUP")
  val testGroup = Group(testGroupId, "Test Group", None)

  val sospechosoCat = Category(AlphanumericId("SOSPECHOSO"), testGroupId, "Sospechoso", isReference = true, Some("Test sospechoso category"))
  val condenadoCat = Category(AlphanumericId("CONDENADO"), testGroupId, "Individuo Condenado", isReference = true, Some("Test condenado category"))
  val multipleCat = Category(AlphanumericId("MULTIPLE"), testGroupId, "Multiple", isReference = false, Some("Test multiple category"))
  val irCat = Category(AlphanumericId("IR"), testGroupId, "IR Category", isReference = true, Some("Test IR category"))

  private def ensureTestCategoriesExist(): Unit = {
    val repo = new SlickCategoryRepository

    // Create test group if it doesn't exist
    try {
      Await.result(repo.addGroup(testGroup), duration)
    } catch {
      case _: Exception => // Group might already exist, ignore
    }

    val existingCategories = Await.result(repo.listCategories, duration).map(_.id)

    if (!existingCategories.contains(sospechosoCat.id)) {
      Await.result(repo.addCategory(sospechosoCat), duration)
    }
    if (!existingCategories.contains(condenadoCat.id)) {
      Await.result(repo.addCategory(condenadoCat), duration)
    }
    if (!existingCategories.contains(multipleCat.id)) {
      Await.result(repo.addCategory(multipleCat), duration)
    }
    if (!existingCategories.contains(irCat.id)) {
      Await.result(repo.addCategory(irCat), duration)
    }
  }

  private def cleanupTestCategories(): Unit = {
    val repo = new SlickCategoryRepository

    // Clean up test data - remove in reverse order due to dependencies
    try {
      Await.result(repo.removeCategory(sospechosoCat.id), duration)
    } catch { case _: Exception => }
    try {
      Await.result(repo.removeCategory(condenadoCat.id), duration)
    } catch { case _: Exception => }
    try {
      Await.result(repo.removeCategory(multipleCat.id), duration)
    } catch { case _: Exception => }
    try {
      Await.result(repo.removeCategory(irCat.id), duration)
    } catch { case _: Exception => }
    try {
      Await.result(repo.removeGroup(testGroupId), duration)
    } catch { case _: Exception => }
  }

  "A Category repository" must {
    "retrive category tree from db" in {
      // Setup: Ensure test categories exist
      ensureTestCategoriesExist()

      try {
        val categoryRepository = new SlickCategoryRepository
        val listFuture = categoryRepository.listCategories
        val list = Await.result(listFuture, duration)

        list.exists(_.id == AlphanumericId("SOSPECHOSO")) mustBe true
      } finally {
        cleanupTestCategories()
      }
    }

    "retrive categories from db" in {
      // Setup: Ensure test categories exist
      ensureTestCategoriesExist()

      try {
        val target = new SlickCategoryRepository
        val listFuture = target.listCategories
        val list = Await.result(listFuture, duration)

        list.exists(c => c.id == AlphanumericId("CONDENADO")) mustBe true
        val s = list.find(sc => sc.id == AlphanumericId("CONDENADO")).get
        s.id mustBe (AlphanumericId("CONDENADO"))
        s.name mustBe "Individuo Condenado"
        s.isReference mustBe true

        val sMezcla = list.find(sc => sc.id == AlphanumericId("MULTIPLE")).get
        sMezcla.isReference mustBe false
      } finally {
        cleanupTestCategories()
      }
    }

    "add category to db" in {
      val group = Group(AlphanumericId("GRUPO"), "GRUPO", None)
      val cat = Category(AlphanumericId("CATEGORIA"), AlphanumericId("GRUPO"), "CATEGORIA", true, Option("description of subcategory A1"))

      val target = new SlickCategoryRepository
      val resultGroup = Await.result(target.addGroup(group), duration)
      val resultCat = Await.result(target.addCategory(cat), duration)

      resultGroup mustBe group.id
      resultCat mustBe cat.id

      val categories = Await.result(target.listCategories, duration)
      val retrieved = categories.find(c => c.id == cat.id)

      retrieved must not be None
      retrieved.get.id mustBe cat.id
      retrieved.get.description mustBe cat.description
      retrieved.get.isReference mustBe cat.isReference
      retrieved.get.name mustBe cat.name

      Await.result(target.removeCategory(cat.id), duration)
      Await.result(target.removeGroup(group.id), duration)
    }

    "add repeated category match with different algorithm" in {
      val group = Group(AlphanumericId("GRUPO"), "GRUPO", None)
      val cat = Category(AlphanumericId("CATEGORIA"), AlphanumericId("GRUPO"), "CATEGORIA", true, Option("description of subcategory A1"))

      val target = new SlickCategoryRepository
      val resultGroup = Await.result(target.addGroup(group), duration)
      val resultCat = Await.result(target.addCategory(cat), duration)

      val rules = Seq(MatchingRule(1, cat.id, Stringency.ModerateStringency, true, true, Algorithm.ENFSI, 10, 0, true),
            MatchingRule(1, cat.id, Stringency.ModerateStringency, true, true, Algorithm.GENIS_MM, 10, 0, true))

      Await.result(target.runInTransactionAsync { implicit session => target.addMatchingRules(cat.id, rules) }, duration)

      val categories = Await.result(target.listCategories, duration)
      val retrieved = categories.find(c => c.id == cat.id)

      retrieved must not be None
      retrieved.get.id mustBe cat.id
      retrieved.get.matchingRules mustBe rules

      Await.result(target.runInTransactionAsync { implicit session => target.deleteMatchingRules(cat.id) }, duration)
      Await.result(target.removeCategory(cat.id), duration)
      Await.result(target.removeGroup(group.id), duration)
    }

    "add category configuration" in {
      val group = Group(AlphanumericId("GRUPO"), "GRUPO", None)
      val cat = Category(AlphanumericId("CATEGORIA"), AlphanumericId("GRUPO"), "CATEGORIA", true, Option("description of subcategory A1"))
      val conf = CategoryConfiguration("", "", "K", "0", 6)

      val target = new SlickCategoryRepository
      Await.result(target.addGroup(group), duration)
      Await.result(target.addCategory(cat), duration)

      Await.result(target.runInTransactionAsync { implicit session => target.addConfigurations(cat.id, Map(1 -> conf)) }, duration)

      val categories = Await.result(target.listCategories, duration)
      val retrieved = categories.find(c => c.id == cat.id)

      retrieved must not be None
      retrieved.get.id mustBe cat.id
      retrieved.get.configurations.get(1) mustBe Some(conf)

      Await.result(target.runInTransactionAsync { implicit session => target.deleteConfigurations(cat.id) }, duration)
      Await.result(target.removeCategory(cat.id), duration)
      Await.result(target.removeGroup(group.id), duration)
    }

    "delete category configuration" in {
      val group = Group(AlphanumericId("GRUPO"), "GRUPO", None)
      val cat = Category(AlphanumericId("CATEGORIA"), AlphanumericId("GRUPO"), "CATEGORIA", true, Option("description of subcategory A1"))
      val conf = CategoryConfiguration("", "", "K", "0", 6)

      val target = new SlickCategoryRepository
      Await.result(target.addGroup(group), duration)
      Await.result(target.addCategory(cat), duration)

      Await.result(target.runInTransactionAsync { implicit session => target.addConfigurations(cat.id, Map(1 -> conf)) }, duration)
      Await.result(target.runInTransactionAsync { implicit session => target.deleteConfigurations(cat.id) }, duration)

      val categories = Await.result(target.listCategories, duration)
      val retrieved = categories.find(c => c.id == cat.id)

      retrieved must not be None
      retrieved.get.id mustBe cat.id
      retrieved.get.configurations mustBe Map.empty

      Await.result(target.removeCategory(cat.id), duration)
      Await.result(target.removeGroup(group.id), duration)
    }

    "delete category configuration with mapping" in {
      val group = Group(AlphanumericId("GRUPO"), "GRUPO", None)
      val cat = Category(AlphanumericId("CATEGORIA"), AlphanumericId("GRUPO"), "CATEGORIA", true, Option("description of subcategory A1"))
      val conf = CategoryConfiguration("", "", "K", "0", 6)

      val target = new SlickCategoryRepository
      Await.result(target.addGroup(group), duration)
      Await.result(target.addCategory(cat), duration)

      Await.result(target.runInTransactionAsync { implicit session => target.addConfigurations(cat.id, Map(1 -> conf)) }, duration)
      Await.result(target.runInTransactionAsync { implicit session => target.deleteConfigurations(cat.id) }, duration)
      Await.result(target.insertOrUpdateMapping(CategoryMappingList(List(CategoryMapping(cat.id,"ER")))), duration)

      val categories = Await.result(target.listCategories, duration)
      val retrieved = categories.find(c => c.id == cat.id)

      retrieved must not be None
      retrieved.get.id mustBe cat.id
      retrieved.get.configurations mustBe Map.empty

      Await.result(target.removeCategory(cat.id), duration)
      Await.result(target.removeGroup(group.id), duration)
    }
    "listCategoriesMapping" in {
      // Setup: Ensure IR category exists
      ensureTestCategoriesExist()

      val target = new SlickCategoryRepository
      val testMappingValue = "test-superior-mapping"

      // Setup: Create a test mapping
      val testMapping = CategoryMappingList(List(CategoryMapping(irCat.id, testMappingValue)))
      Await.result(target.insertOrUpdateMapping(testMapping), duration)

      try {
        val result = Await.result(target.listCategoriesMapping, duration)

        // Verify that our specific mapping is in the list
        val ourMapping = result.find(_.id == irCat.id)
        ourMapping must not be None
        ourMapping.get.id mustBe irCat.id
        ourMapping.get.idSuperior mustBe testMappingValue
      } finally {
        // Cleanup
        Await.result(target.deleteCategoryMappingById("IR"), duration)
        cleanupTestCategories()
      }
    }

    "insertOrUpdateMapping no ok" in {
      val target = new SlickCategoryRepository
      // Use category IDs that definitely don't exist to test failure case
      val categoryMappingList: List[CategoryMapping] = List(
        CategoryMapping(AlphanumericId("NONEXISTENT_CAT_1"), "nada234"),
        CategoryMapping(AlphanumericId("NONEXISTENT_CAT_2"), "nada2er3")
      )
      val result = Await.result(target.insertOrUpdateMapping(CategoryMappingList(categoryMappingList)), duration)
      result.isLeft mustBe true

    }
    "insertOrUpdateMapping ok" in {
      // Setup: Ensure IR category exists
      ensureTestCategoriesExist()

      try {
        val target = new SlickCategoryRepository
        val categoryMappingList: List[CategoryMapping] = List(
          CategoryMapping(AlphanumericId("IR"), "nada234")
        )
        val result = Await.result(target.insertOrUpdateMapping(CategoryMappingList(categoryMappingList)), duration)
        result.isRight mustBe true

        Await.result(target.deleteCategoryMappingById("IR"), duration)
      } finally {
        cleanupTestCategories()
      }
    }

    "getCategoriesMappingById ok" in {
      // Setup: Ensure IR category exists (but without a mapping)
      ensureTestCategoriesExist()

      try {
        val target = new SlickCategoryRepository
        val result = Await.result(target.getCategoriesMappingById(AlphanumericId("IR")), duration)
        result mustBe None
      } finally {
        cleanupTestCategories()
      }
    }

  }

}