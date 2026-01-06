package bulkupload

import org.scalatest.mock.MockitoSugar
import specs.PdgSpec

import scala.concurrent.duration.{Duration, SECONDS}

class ProtoProfileBuilderTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)

  "ProtoProfileBuilder" must {
    "validateMarker - lower case" in {
      val kits = Map("KIT1" -> List("MARKER1", "MARKER2"))
      val locusAlias = Map(
        "marker1" -> "IDMARKER1",
        "MARKER2" -> "IDMARKER2"
      )

      val validator = Validator(null, kits, null, locusAlias, null, null)

      val result = validator.validateMarker("KIT1", "marker1")

      result._1 mustBe List.empty
      result._2 mustBe "IDMARKER1"
    }

    "validateMarker - upper case" in {
      val kits = Map("KIT1" -> List("MARKER1", "MARKER2"))
      val locusAlias = Map(
        "marker1" -> "IDMARKER1",
        "MARKER2" -> "IDMARKER2"
      )

      val validator = Validator(null, kits, null, locusAlias, null, null)

      val result = validator.validateMarker("KIT1", "MARKER1")

      result._1 mustBe List.empty
      result._2 mustBe "IDMARKER1"
    }

    "validateMarker - marker not found - upper case alias E0680" in {
      val kits = Map("KIT1" -> List("MARKER1", "MARKER2"))
      val locusAlias = Map(
        "marker1" -> "IDMARKER1",
        "MARKER2" -> "IDMARKER2"
      )

      val validator = Validator(null, kits, null, locusAlias, null, null)

      val result = validator.validateMarker("KIT1", "MARKER2")

      result._1.size mustBe 1
      //result._1.head mustBe "El marcador 'MARKER2' no esta definido en el sistema"
      result._1.head mustBe "E0680: El marcador 'MARKER2' no esta definido en el sistema."
      result._2 mustBe "MARKER2"
    }

    "validateMarker - marker not found in kit E0681" in {
      val kits = Map("kit1" -> List("MARKER1", "MARKER2"))
      val locusAlias = Map(
        "marker1" -> "IDMARKER1",
        "MARKER2" -> "IDMARKER2"
      )

      val validator = Validator(null, kits, null, locusAlias, null, null)

      val result = validator.validateMarker("KIT1", "MARKER1")

      result._1.size mustBe 1
     // result._1.head mustBe "El marcador 'MARKER1' no pertenece al kit 'KIT1'"
      result._1.head mustBe "E0681: El marcador 'MARKER1' no pertenece al kit 'KIT1'."
      result._2 mustBe "MARKER1"
    }

    "validateMarker - marker found in kit" in {
      val kits = Map("kit1" -> List("IDMARKER1", "IDMARKER2"))
      val locusAlias = Map(
        "marker1" -> "IDMARKER1",
        "MARKER2" -> "IDMARKER2"
      )

      val validator = Validator(null, kits, null, locusAlias, null, null)

      val result = validator.validateMarker("KIT1", "MARKER1")

      result._1 mustBe List.empty
      result._2 mustBe "IDMARKER1"
    }

    "validateKit - lower case" in {
      val kitAlias = Map(
        "kit1" -> "IDKIT1",
        "kit2" -> "IDKIT2"
      )

      val validator = Validator(null, null, kitAlias, null, null, null)

      val result = validator.validateKit("kit1")

      result._1 mustBe None
      result._2 mustBe "IDKIT1"
    }

    "validateKit - upper case" in {
      val kitAlias = Map(
        "kit1" -> "IDKIT1",
        "kit2" -> "IDKIT2"
      )

      val validator = Validator(null, null, kitAlias, null, null, null)

      val result = validator.validateKit("KIT2")

      result._1 mustBe None
      result._2 mustBe "IDKIT2"
    }

    "validateKit - kit not found E0691" in {
      val kitAlias = Map(
        "kit1" -> "IDKIT1",
        "kit2" -> "IDKIT2"
      )

      val validator = Validator(null, null, kitAlias, null, null, null)

      val result = validator.validateKit("KIT3")

      //result._1 mustBe Some("El kit 'KIT3' no esta definido en el sistema")
      result._1 mustBe Some("E0691: El kit 'KIT3' no esta definido en el sistema.")
      result._2 mustBe "KIT3"
    }

    "validateKit - kit not found - upper key E0691" in {
      val kitAlias = Map(
        "kit1" -> "IDKIT1",
        "KIT2" -> "IDKIT2"
      )

      val validator = Validator(null, null, kitAlias, null, null, null)

      val result = validator.validateKit("KIT2")

     // result._1 mustBe Some("El kit 'KIT2' no esta definido en el sistema")
      result._1 mustBe Some("E0691: El kit 'KIT2' no esta definido en el sistema.")
      result._2 mustBe "KIT2"
    }
  }
}
