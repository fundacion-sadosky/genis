package unit.kits

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import kits.StrKit

class StrKitTest extends AnyWordSpec with Matchers:

  "StrKit JSON serialization" must {
    "roundtrip with all fields" in {
      val kit = StrKit("PP16", "PowerPlex 16", 1, 16, 15)
      val json = Json.toJson(kit)
      val parsed = json.as[StrKit]
      parsed mustBe kit
      parsed.id mustBe "PP16"
      parsed.name mustBe "PowerPlex 16"
      parsed.`type` mustBe 1
      parsed.locy_quantity mustBe 16
      parsed.representative_parameter mustBe 15
    }
  }
