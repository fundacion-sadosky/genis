package unit.kits

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import kits.{FullStrKit, NewStrKitLocus}

class FullStrKitTest extends AnyWordSpec with Matchers:

  "NewStrKitLocus JSON serialization" must {
    "roundtrip with fluorophore = Some" in {
      val locus = NewStrKitLocus("D3S1358", Some("Blue"), 1)
      val json = Json.toJson(locus)
      val parsed = json.as[NewStrKitLocus]
      parsed mustBe locus
      parsed.fluorophore mustBe Some("Blue")
    }

    "roundtrip with fluorophore = None" in {
      val locus = NewStrKitLocus("AMEL", None, 5)
      val json = Json.toJson(locus)
      val parsed = json.as[NewStrKitLocus]
      parsed mustBe locus
      parsed.fluorophore mustBe None
    }
  }

  "FullStrKit JSON serialization" must {
    "roundtrip with alias and locus" in {
      val kit = FullStrKit(
        "PP16", "PowerPlex 16", 1, 16, 15,
        Seq("PowerPlex16", "PP16HS"),
        Seq(
          NewStrKitLocus("D3S1358", Some("Blue"), 1),
          NewStrKitLocus("AMEL", None, 2)
        )
      )
      val json = Json.toJson(kit)
      val parsed = json.as[FullStrKit]
      parsed mustBe kit
      parsed.alias must have size 2
      parsed.locus must have size 2
    }
  }
