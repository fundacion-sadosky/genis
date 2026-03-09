package unit.kits

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import kits.StrKitLocus

class StrKitLocusTest extends AnyWordSpec with Matchers:

  "StrKitLocus JSON serialization" must {
    "roundtrip with all optional fields populated" in {
      val locus = StrKitLocus("D3S1358", "D3S1358", Some("3"), 1, 4, Some("Blue"), 1, required = true)
      val json = Json.toJson(locus)
      val parsed = json.as[StrKitLocus]
      parsed mustBe locus
      parsed.chromosome mustBe Some("3")
      parsed.fluorophore mustBe Some("Blue")
      parsed.required mustBe true
    }

    "roundtrip with chromosome and fluorophore = None" in {
      val locus = StrKitLocus("AMEL", "Amelogenin", None, 1, 2, None, 5)
      val json = Json.toJson(locus)
      val parsed = json.as[StrKitLocus]
      parsed mustBe locus
      parsed.chromosome mustBe None
      parsed.fluorophore mustBe None
    }

    "roundtrip with required = false (default)" in {
      val locus = StrKitLocus("TH01", "TH01", Some("11"), 1, 3, Some("Green"), 3)
      val json = Json.toJson(locus)
      val parsed = json.as[StrKitLocus]
      parsed.required mustBe false
    }
  }
