package unit.configdata

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import configdata.{Crime, CrimeType}

class CrimeTypeTest extends AnyWordSpec with Matchers:

  "Crime JSON serialization" must {
    "roundtrip with description = Some" in {
      val crime = Crime("C01", "Robo", Some("Robo agravado"))
      val json = Json.toJson(crime)
      val parsed = json.as[Crime]
      parsed mustBe crime
      parsed.id mustBe "C01"
      parsed.name mustBe "Robo"
      parsed.description mustBe Some("Robo agravado")
    }

    "roundtrip with description = None" in {
      val crime = Crime("C02", "Hurto", None)
      val json = Json.toJson(crime)
      val parsed = json.as[Crime]
      parsed mustBe crime
      parsed.description mustBe None
    }
  }

  "CrimeType JSON serialization" must {
    "roundtrip with crimes list" in {
      val ct = CrimeType(
        "CT01", "Contra la propiedad", Some("Delitos patrimoniales"),
        Seq(Crime("C01", "Robo", Some("Robo agravado")), Crime("C02", "Hurto", None))
      )
      val json = Json.toJson(ct)
      val parsed = json.as[CrimeType]
      parsed mustBe ct
      parsed.crimes must have size 2
    }

    "roundtrip with empty crimes list" in {
      val ct = CrimeType("CT02", "Otros", None, Seq.empty)
      val json = Json.toJson(ct)
      val parsed = json.as[CrimeType]
      parsed mustBe ct
      parsed.crimes mustBe empty
      parsed.description mustBe None
    }
  }
