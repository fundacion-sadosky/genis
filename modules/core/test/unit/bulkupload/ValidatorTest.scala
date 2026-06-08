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

  // Helper para los metodos puros basados en mapas (alias de kit/locus/categoria, geneticistas).
  private def validatorWith(
    kits: Map[String, List[String]] = Map.empty,
    kitAlias: Map[String, String] = Map.empty,
    locusAlias: Map[String, String] = Map.empty,
    geneticists: List[user.UserView] = Nil,
    categoryAlias: Map[String, types.AlphanumericId] = Map.empty
  ): Validator =
    Validator(BulkValidationCache.empty, kits, kitAlias, locusAlias, geneticists, categoryAlias, messages)

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

  // ---- Metodos puros basados en mapas (tenian cobertura en el legacy ProtoProfileBuilderTest) ----

  "Validator.validateKit" should {
    "resolver el alias del kit (case-insensitive) sin error" in {
      val v = validatorWith(kitAlias = Map("kit1" -> "IDKIT1"))
      v.validateKit("KIT1") mustBe (None, "IDKIT1")
    }
    "devolver E0691 cuando el kit no esta definido" in {
      val (err, kit) = validatorWith().validateKit("DESCONOCIDO")
      err.exists(_.contains("E0691")) mustBe true
      kit mustBe "DESCONOCIDO"
    }
  }

  "Validator.validateMarker" should {
    "resolver el alias del marcador (case-insensitive) sin error cuando el kit no restringe" in {
      val v = validatorWith(locusAlias = Map("marker1" -> "IDMARKER1"))
      v.validateMarker("KIT1", "MARKER1") mustBe (Nil, "IDMARKER1")
    }
    "devolver E0680 cuando el marcador no esta definido (autosomal)" in {
      val (errs, mrk) = validatorWith().validateMarker("KIT1", "MARKER2")
      errs.exists(_.contains("E0680")) mustBe true
      mrk mustBe "MARKER2"
    }
    "devolver E0681 cuando el marcador no pertenece al kit (autosomal)" in {
      val v = validatorWith(
        kits = Map("kit1" -> List("IDMARKER2")),
        locusAlias = Map("marker1" -> "IDMARKER1")
      )
      val (errs, mrk) = v.validateMarker("KIT1", "MARKER1")
      errs.exists(_.contains("E0681")) mustBe true
      mrk mustBe "MARKER1"
    }
    "no devolver error cuando el marcador pertenece al kit" in {
      val v = validatorWith(
        kits = Map("kit1" -> List("IDMARKER1")),
        locusAlias = Map("marker1" -> "IDMARKER1")
      )
      v.validateMarker("KIT1", "MARKER1") mustBe (Nil, "IDMARKER1")
    }
    "devolver E0310 cuando el marcador no esta definido (mitocondrial)" in {
      val (errs, _) = validatorWith().validateMarker("KIT1", "MARKER2", mitocondrial = true)
      errs.exists(_.contains("E0310")) mustBe true
    }
  }

  "Validator.validateCategory" should {
    "resolver la categoria por alias" in {
      val id = AlphanumericId("IDCAT")
      validatorWith(categoryAlias = Map("alias" -> id)).validateCategory("alias") mustBe Some(id)
    }
    "devolver None cuando la categoria no esta en el mapa" in {
      validatorWith().validateCategory("desconocida") mustBe None
    }
  }

  "Validator.validateAssignee" should {
    "no devolver error cuando el geneticista existe por geneMapperId" in {
      val g = user.UserView("g1", "G", "Uno", "g1@test.com", Seq.empty, user.UserStatus.active, "GM1", "")
      validatorWith(geneticists = List(g)).validateAssignee("GM1") mustBe None
    }
    "devolver E0650 cuando el geneticista no existe" in {
      validatorWith().validateAssignee("GM1").exists(_.contains("E0650")) mustBe true
    }
  }
