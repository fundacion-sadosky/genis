package unit.types

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import types.{Geneticist, Laboratory}

class LaboratoryTest extends AnyWordSpec with Matchers:

  val lab = Laboratory(
    name = "Lab Central",
    code = "LC01",
    country = "AR",
    province = "Buenos Aires",
    address = "Av. Siempre Viva 123",
    telephone = "1122334455",
    contactEmail = "lab@example.com",
    dropIn = 0.5,
    dropOut = 0.8
  )

  "Laboratory JSON serialization" must {
    "roundtrip with instance = None (default)" in {
      val json = Json.toJson(lab)
      val parsed = json.as[Laboratory]
      parsed mustBe lab
      parsed.instance mustBe None
    }

    "roundtrip with instance = Some(true)" in {
      val labWithInstance = lab.copy(instance = Some(true))
      val json = Json.toJson(labWithInstance)
      json.as[Laboratory] mustBe labWithInstance
    }

    "roundtrip with all fields populated" in {
      val labFull = lab.copy(instance = Some(false))
      val json = Json.toJson(labFull)
      val parsed = json.as[Laboratory]
      parsed.name mustBe "Lab Central"
      parsed.code mustBe "LC01"
      parsed.country mustBe "AR"
      parsed.province mustBe "Buenos Aires"
      parsed.dropIn mustBe 0.5
      parsed.dropOut mustBe 0.8
      parsed.instance mustBe Some(false)
    }
  }

  "Geneticist JSON serialization" must {
    val geneticist = Geneticist(
      name = "Juan",
      laboratory = "LC01",
      lastname = "Pérez",
      email = "juan@example.com",
      telephone = "1122334455",
      id = Some(42L)
    )

    "roundtrip with all fields" in {
      val json = Json.toJson(geneticist)
      json.as[Geneticist] mustBe geneticist
    }

    "roundtrip with id = None" in {
      val noId = geneticist.copy(id = None)
      val json = Json.toJson(noId)
      val parsed = json.as[Geneticist]
      parsed mustBe noId
      parsed.id mustBe None
    }
  }
