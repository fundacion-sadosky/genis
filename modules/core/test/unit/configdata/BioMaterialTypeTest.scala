package unit.configdata

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import configdata.BioMaterialType
import types.AlphanumericId

class BioMaterialTypeTest extends AnyWordSpec with Matchers:

  "BioMaterialType JSON serialization" must {
    "roundtrip with all fields populated" in {
      val bmt = BioMaterialType(AlphanumericId("BLOOD"), "Sangre", Some("Muestra de sangre"))
      val json = Json.toJson(bmt)
      val parsed = json.as[BioMaterialType]
      parsed mustBe bmt
      parsed.id.text mustBe "BLOOD"
      parsed.name mustBe "Sangre"
      parsed.description mustBe Some("Muestra de sangre")
    }

    "roundtrip with description = None" in {
      val bmt = BioMaterialType(AlphanumericId("HAIR"), "Cabello", None)
      val json = Json.toJson(bmt)
      val parsed = json.as[BioMaterialType]
      parsed mustBe bmt
      parsed.description mustBe None
    }
  }
