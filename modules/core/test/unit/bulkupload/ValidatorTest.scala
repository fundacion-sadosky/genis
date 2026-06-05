package unit.bulkupload

import bulkupload.{BulkValidationCache, Validator}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.{Lang, MessagesImpl}
import play.api.test.Helpers.stubMessagesApi
import types.{AlphanumericId, SampleCode}

// #218 review S5Q3: el Validator dejo de hacer Await por-perfil; ahora resuelve las 3 validaciones
// que dependen de BD contra un BulkValidationCache precargado. Estos tests fijan esa semantica
// (equivalente a la del repo: exists -> E0306, validateAssigneAndCategory -> E0662/E0663, mtExistente).
class ValidatorTest extends AnyWordSpec with Matchers:

  private val messages = MessagesImpl(Lang("es"), stubMessagesApi())

  private def validator(cache: BulkValidationCache): Validator =
    Validator(cache, Map.empty, Map.empty, Map.empty, Nil, Map.empty, messages)

  "Validator.validateSampleName (cache-backed)" should {
    "devolver error (E0306) y sin SampleCode cuando hay proto-perfil pendiente pero no perfil existente" in {
      val v = validator(BulkValidationCache(existsBySample = Map("S1" -> (None, Some(42L)))))
      val (err, sc) = v.validateSampleName("S1")
      err mustBe defined
      sc mustBe None
    }
    "no devolver error y propagar el SampleCode cuando el perfil ya existe" in {
      val gc = SampleCode("AR-B-LAB-1")
      val v = validator(BulkValidationCache(existsBySample = Map("S1" -> (Some(gc), Some(42L)))))
      val (err, sc) = v.validateSampleName("S1")
      err mustBe None
      sc mustBe Some(gc)
    }
    "no devolver error ni SampleCode cuando el sampleName no esta en el cache" in {
      val (err, sc) = validator(BulkValidationCache.empty).validateSampleName("desconocido")
      err mustBe None
      sc mustBe None
    }
  }

  "Validator.validateAssigneAndCategory (cache-backed)" should {
    val gc = SampleCode("AR-B-LAB-1")
    "devolver error (E0662) cuando no hay categoria persistida para (globalCode, assignee)" in {
      validator(BulkValidationCache.empty)
        .validateAssigneAndCategory(gc, "asg", Some(AlphanumericId("CAT"))) mustBe defined
    }
    "no devolver error cuando la categoria coincide" in {
      val v = validator(BulkValidationCache(categoryByGcAndAssignee = Map((gc.text, "asg") -> "CAT")))
      v.validateAssigneAndCategory(gc, "asg", Some(AlphanumericId("CAT"))) mustBe None
    }
    "devolver error (E0663) cuando la categoria no coincide" in {
      val v = validator(BulkValidationCache(categoryByGcAndAssignee = Map((gc.text, "asg") -> "OTRA")))
      v.validateAssigneAndCategory(gc, "asg", Some(AlphanumericId("CAT"))) mustBe defined
    }
    "no devolver error cuando category es None aunque exista la fila" in {
      val v = validator(BulkValidationCache(categoryByGcAndAssignee = Map((gc.text, "asg") -> "CAT")))
      v.validateAssigneAndCategory(gc, "asg", None) mustBe None
    }
  }

  "Validator.validarMtExistente (cache-backed)" should {
    "devolver true cuando el sampleName tiene mito existente" in {
      validator(BulkValidationCache(mtExistsBySample = Set("S1"))).validarMtExistente("S1") mustBe true
    }
    "devolver false cuando no" in {
      validator(BulkValidationCache.empty).validarMtExistente("S1") mustBe false
    }
  }
