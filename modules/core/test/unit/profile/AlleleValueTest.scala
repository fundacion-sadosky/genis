package unit.profile

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import profile.*
import play.api.libs.json.{JsSuccess, Json}

class AlleleValueTest extends AnyWordSpec with Matchers {

  "AlleleValue" must {

    "parse a decimal number" in {
      val av = AlleleValue("10")
      av mustBe a[Allele]
      av.asInstanceOf[Allele].count mustBe BigDecimal(10)
    }

    "parse a decimal with fraction" in {
      val av = AlleleValue("9.3")
      av mustBe a[Allele]
      av.asInstanceOf[Allele].count mustBe BigDecimal(9.3)
    }

    "parse an out-of-ladder allele with <" in {
      val av = AlleleValue("5<")
      av mustBe a[OutOfLadderAllele]
    }

    "parse an out-of-ladder allele with >" in {
      val av = AlleleValue("30>")
      av mustBe a[OutOfLadderAllele]
    }

    "parse a microvariant" in {
      val av = AlleleValue("10.x")
      av mustBe a[MicroVariant]
      av.asInstanceOf[MicroVariant].count mustBe 10
    }

    "parse XY alleles" in {
      AlleleValue("X") mustBe a[XY]
      AlleleValue("Y") mustBe a[XY]
    }

    "throw on invalid allele text" in {
      an[IllegalArgumentException] must be thrownBy AlleleValue("INVALID")
    }

    "serialize Allele to JSON" in {
      val allele: AlleleValue = Allele(BigDecimal(10))
      val json = Json.toJson(allele)
      json mustBe play.api.libs.json.JsNumber(BigDecimal(10))
    }

    "deserialize Allele from JSON string" in {
      val json = Json.toJson("10")
      val parsed = json.validate[AlleleValue]
      parsed mustBe a[JsSuccess[?]]
      parsed.get mustBe a[Allele]
    }

    "deserialize Allele from JSON number" in {
      val json = Json.toJson(10)
      val parsed = json.validate[AlleleValue]
      parsed mustBe a[JsSuccess[?]]
    }

    "round-trip XY through JSON" in {
      val xy: AlleleValue = XY('X')
      val json = Json.toJson(xy)
      val back = json.as[AlleleValue]
      back mustBe a[XY]
    }
  }
}
