package configdata

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import services.CacheService
import services.Keys
import specs.PdgSpec
import stubs.Stubs
import types.AlphanumericId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Random, Success}

class CategoryServiceTest extends PdgSpec with MockitoSugar {

   val cacheMockService = mock[CacheService]
      
  val duration = Duration(10, SECONDS)
  "CategoryService" must {

    "add a category" in {
      val cat = Stubs.catA1

      val categoryRepository = mock[CategoryRepository]
      when(categoryRepository.addCategory(any[Category])).thenReturn(Future.successful(cat.id))

      val categoryService: CategoryService = new CachedCategoryService(cacheMockService, categoryRepository)
      val res = Await.result(categoryService.addCategory(cat),duration)

      val fullCat = res.right.get
      fullCat.id mustBe cat.id
      fullCat.group mustBe cat.group
      fullCat.isReference mustBe cat.isReference
      fullCat.description mustBe cat.description
    }

    "update a category" in {
      
      val categoryService: CategoryService = new CachedCategoryService(cacheMockService, new SlickCategoryRepository)
      
      val fullcat = Stubs.fullCatMixture
      
      val res = Await.result(categoryService.updateCategory(fullcat),duration)
      
      res.right.get mustBe 1
    }
    
    "not add a Group with an existing group id E0670" in {
      
      val group = Group(AlphanumericId("INDUBITADA"),"Group1",None)
      
      val categoryService: CategoryService = new CachedCategoryService(cacheMockService, new SlickCategoryRepository)
      
      val res = Await.result(categoryService.addGroup(group), duration)
      
      res.isRight mustBe false
      res.left.get mustBe "E0670: Id o nombre de grupo duplicado."
    }
    
    "not add a Group with an existing name E0670" in {
      
      val group = Group(AlphanumericId("INDUBITADA2"),"Muestra de referencia indubitada",None)
      
      val categoryService: CategoryService = new CachedCategoryService(null, new SlickCategoryRepository)
      
      val res = Await.result(categoryService.addGroup(group), duration)
      
      res.isRight mustBe false
      res.left.get mustBe "E0670: Id o nombre de grupo duplicado."
    }

    "listMapping " in {

      val categoryRepository = mock[CategoryRepository]
      val fcm = List(FullCategoryMapping(AlphanumericId("ER"),"","",""))
      when(categoryRepository.listCategoriesMapping).thenReturn(Future.successful(fcm))

      val categoryService: CategoryService = new CachedCategoryService(null, categoryRepository)

      val res = Await.result(categoryService.listCategoriesMapping, duration)

      res.size mustBe 1
    }

    "insert or update mapping " in {

      val categoryRepository = mock[CategoryRepository]
      when(categoryRepository.insertOrUpdateMapping(null)).thenReturn(Future.successful(Right(())))

      val categoryService: CategoryService = new CachedCategoryService(null, categoryRepository)

      val res = Await.result(categoryService.insertOrUpdateMapping(null), duration)

      res.isRight mustBe true
    }
    "getCategoriesMappingById " in {

      val categoryRepository = mock[CategoryRepository]

      when(categoryRepository.getCategoriesMappingById(AlphanumericId("SOSPECHOSO"))).thenReturn(Future.successful(Some("idSup")))

      when(categoryRepository.listCategories).thenReturn(Future.successful(List(Stubs.fullCatA1)))

      val categoryService = new CachedCategoryService(cacheMockService, categoryRepository)

      val categoryServiceSpy = Mockito.spy(categoryService)

      Mockito.doReturn(false).when(categoryServiceSpy).isPedigreeAssociation(AlphanumericId("SOSPECHOSO"))

      val res = Await.result(categoryServiceSpy.getCategoriesMappingById(AlphanumericId("SOSPECHOSO")), duration)
      res.get mustBe "idSup"

      Mockito.doReturn(true).when(categoryServiceSpy).isPedigreeAssociation(AlphanumericId("SOSPECHOSO"))
      val res2 = Await.result(categoryServiceSpy.getCategoriesMappingById(AlphanumericId("SOSPECHOSO")), duration)
      res2.get mustBe "SOSPECHOSO"

      when(categoryRepository.getCategoriesMappingById(AlphanumericId("SOSPECHOSO"))).thenReturn(Future.successful(None))
      Mockito.doReturn(false).when(categoryServiceSpy).isPedigreeAssociation(AlphanumericId("SOSPECHOSO"))

      val res3 = Await.result(categoryServiceSpy.getCategoriesMappingById(AlphanumericId("SOSPECHOSO")), duration)
      res3 mustBe None
    }
  }
}
