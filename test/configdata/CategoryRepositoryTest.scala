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
  
  "A Category repository" must {
    "retrive category tree from db" in {
      val categoryRepository = new SlickCategoryRepository
      val listFuture = categoryRepository.listCategories
      val list = Await.result(listFuture, duration)

      list.exists( _.id == AlphanumericId("SOSPECHOSO") ) mustBe true
    }

    "retrive categories from db" in {
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
      val target = new SlickCategoryRepository
      val result = Await.result(target.listCategoriesMapping, duration)
      result.isEmpty mustBe false
    }

    "insertOrUpdateMapping no ok" in {
      val target = new SlickCategoryRepository
      val categoryMappingList: List[CategoryMapping] = List(
        CategoryMapping(AlphanumericId("IR"),"nada234"),
        CategoryMapping(AlphanumericId("MAL"),"nada2er3")
      )
      val result = Await.result(target.insertOrUpdateMapping(CategoryMappingList(categoryMappingList)), duration)
      result.isLeft mustBe true

    }
    "insertOrUpdateMapping ok" in {
      val target = new SlickCategoryRepository
      val categoryMappingList: List[CategoryMapping] = List(
        CategoryMapping(AlphanumericId("IR"),"nada234")
      )
      val result = Await.result(target.insertOrUpdateMapping(CategoryMappingList(categoryMappingList)), duration)
      result.isRight mustBe true

      Await.result(target.deleteCategoryMappingById("IR"), duration)

    }

    "getCategoriesMappingById ok" in {
      val target = new SlickCategoryRepository
      val result = Await.result(target.getCategoriesMappingById(AlphanumericId("IR")), duration)
      result mustBe None
    }

  }

}