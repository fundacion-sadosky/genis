package unit.scenarios

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*
import scenarios.*
import scenarios.ScenarioStatus.*
import matching.MongoId
import types.{MongoDate, SampleCode, AlphanumericId, StatOption}
import probability.LRResult
import org.bson.types.ObjectId

import java.util.Date

class ScenarioModelsTest extends AnyWordSpec with Matchers:

  "ScenarioStatus" must {
    "serialize to JSON" in {
      Json.toJson(ScenarioStatus.Pending) mustBe JsString("Pending")
      Json.toJson(ScenarioStatus.Validated) mustBe JsString("Validated")
      Json.toJson(ScenarioStatus.Deleted) mustBe JsString("Deleted")
    }

    "deserialize from JSON" in {
      JsString("Pending").as[ScenarioStatus] mustBe ScenarioStatus.Pending
      JsString("Validated").as[ScenarioStatus] mustBe ScenarioStatus.Validated
      JsString("Deleted").as[ScenarioStatus] mustBe ScenarioStatus.Deleted
    }

    "fail on unknown value" in {
      JsString("Unknown").validate[ScenarioStatus].isError mustBe true
    }

    "reject non-string JSON" in {
      JsNumber(0).validate[ScenarioStatus].isError mustBe true
      JsNumber(999).validate[ScenarioStatus].isError mustBe true
      Json.obj("value" -> "Pending").validate[ScenarioStatus].isError mustBe true
    }
  }

  "MongoId" must {
    "reject non-object JSON" in {
      JsString("507f1f77bcf86cd799439011").validate[MongoId].isError mustBe true
      JsNumber(123).validate[MongoId].isError mustBe true
      JsNull.validate[MongoId].isError mustBe true
    }
  }

  "Hypothesis" must {
    "serialize and deserialize" in {
      val hypothesis = Hypothesis(
        List(SampleCode("AR-B-LAB-1")),
        List(SampleCode("AR-B-LAB-2")),
        3,
        0.4
      )
      val json = Json.toJson(hypothesis)
      json.as[Hypothesis] mustBe hypothesis
    }
  }

  "CalculationScenario" must {
    "serialize and deserialize" in {
      val cs = CalculationScenario(
        sample = SampleCode("AR-B-LAB-1"),
        prosecutor = Hypothesis(List(SampleCode("AR-B-LAB-2")), List.empty, 3, 0.4),
        defense = Hypothesis(List.empty, List(SampleCode("AR-B-LAB-3")), 1, 0.5),
        stats = StatOption("table1", "model1", 1.0, 0.4, Some(0.5))
      )
      val json = Json.toJson(cs)
      json.as[CalculationScenario] mustBe cs
    }
  }

  "ScenarioOption" must {
    "serialize and deserialize" in {
      val option = ScenarioOption(
        globalCode = SampleCode("AR-B-LAB-1"),
        internalSampleCode = "INTERNAL-1",
        categoryId = AlphanumericId("SOSPECHOSO"),
        sharedAllelePonderation = 0.75,
        contributors = 1,
        dropOuts = 2,
        associated = false
      )
      val json = Json.toJson(option)
      json.as[ScenarioOption] mustBe option
    }
  }

  "ScenarioSearch" must {
    "serialize with minimal fields" in {
      val search = ScenarioSearch(profile = SampleCode("AR-B-LAB-1"))
      val json = Json.toJson(search)
      json.as[ScenarioSearch] mustBe search
    }

    "serialize with all fields" in {
      val search = ScenarioSearch(
        profile = SampleCode("AR-B-LAB-1"),
        name = Some("test scenario"),
        hourFrom = Some(new Date(1000000)),
        hourUntil = Some(new Date(2000000)),
        state = Some(true),
        ascending = true,
        sortField = "name"
      )
      val json = Json.toJson(search)
      json.as[ScenarioSearch] mustBe search
    }
  }

  "NCorrectionRequest" must {
    "serialize and deserialize" in {
      val request = NCorrectionRequest(
        firingCode = SampleCode("AR-C-HIBA-500"),
        matchingCode = SampleCode("AR-B-IMBICE-500"),
        bigN = 1000,
        lr = 2.5
      )
      val json = Json.toJson(request)
      json.as[NCorrectionRequest] mustBe request
    }
  }

  "NCorrectionResponse" must {
    "serialize and deserialize" in {
      val response = NCorrectionResponse(dmp = 0.5, donnellyBaldwin = 2.0, n = 1)
      val json = Json.toJson(response)
      json.as[NCorrectionResponse] mustBe response
    }
  }

  "LRResult" must {
    "serialize and deserialize" in {
      val result = LRResult(total = 1.5, detailed = Map("LOCUS1" -> Some(2.0), "LOCUS2" -> None))
      val json = Json.toJson(result)
      val deserialized = json.as[LRResult]
      deserialized.total mustBe 1.5
      deserialized.detailed("LOCUS1") mustBe Some(2.0)
      deserialized.detailed("LOCUS2") mustBe None
    }
  }

  "Scenario" must {
    "deserialize with defaults for missing fields" in {
      val json = Json.obj(
        "name" -> "Test Scenario",
        "geneticist" -> "user1",
        "calculationScenario" -> Json.obj(
          "sample" -> "AR-B-LAB-1",
          "prosecutor" -> Json.obj(
            "selected" -> Json.arr("AR-B-LAB-2"),
            "unselected" -> Json.arr(),
            "unknowns" -> 1,
            "dropOut" -> 0.3
          ),
          "defense" -> Json.obj(
            "selected" -> Json.arr(),
            "unselected" -> Json.arr("AR-B-LAB-3"),
            "unknowns" -> 0,
            "dropOut" -> 0.1
          ),
          "stats" -> Json.obj(
            "frequencyTable" -> "A",
            "probabilityModel" -> "1",
            "theta" -> 1,
            "dropIn" -> 0.4
          )
        ),
        "isRestricted" -> false
      )
      val scenario = json.as[Scenario]
      scenario.name mustBe "Test Scenario"
      scenario.state mustBe ScenarioStatus.Pending
      scenario.geneticist mustBe "user1"
      scenario.isRestricted mustBe false
      scenario.result mustBe None
      scenario.description mustBe None
    }

    "deserialize from MongoDB Extended JSON format" in {
      val oid = new ObjectId()
      val dateMillis = 1712592000000L
      val json = Json.obj(
        "_id" -> Json.obj("$oid" -> oid.toString),
        "name" -> "Scenario Forense",
        "state" -> "Validated",
        "geneticist" -> "genetista1",
        "calculationScenario" -> Json.obj(
          "sample" -> "AR-B-LAB-1",
          "prosecutor" -> Json.obj(
            "selected" -> Json.arr("AR-B-LAB-2"),
            "unselected" -> Json.arr(),
            "unknowns" -> 3,
            "dropOut" -> 0.4
          ),
          "defense" -> Json.obj(
            "selected" -> Json.arr(),
            "unselected" -> Json.arr("AR-B-LAB-3"),
            "unknowns" -> 1,
            "dropOut" -> 0.5
          ),
          "stats" -> Json.obj(
            "frequencyTable" -> "A",
            "probabilityModel" -> "1",
            "theta" -> 1,
            "dropIn" -> 0.4,
            "dropOut" -> 0.5
          )
        ),
        "date" -> Json.obj("$date" -> dateMillis),
        "isRestricted" -> true,
        "result" -> Json.obj(
          "total" -> 1.5,
          "detailed" -> Json.obj("TPOX" -> 2.0)
        ),
        "description" -> "Descripción del escenario"
      )
      val scenario = json.as[Scenario]
      scenario._id mustBe MongoId(oid.toString)
      scenario.name mustBe "Scenario Forense"
      scenario.state mustBe ScenarioStatus.Validated
      scenario.geneticist mustBe "genetista1"
      scenario.date.date.getTime mustBe dateMillis
      scenario.isRestricted mustBe true
      scenario.result.get.total mustBe 1.5
      scenario.result.get.detailed("TPOX") mustBe Some(2.0)
      scenario.description mustBe Some("Descripción del escenario")
      scenario.calculationScenario.prosecutor.unknowns mustBe 3
      scenario.calculationScenario.defense.dropOut mustBe 0.5
    }

    "generate valid 24-hex-char ObjectId as default _id" in {
      val json = Json.obj(
        "name" -> "Test",
        "geneticist" -> "user1",
        "calculationScenario" -> Json.obj(
          "sample" -> "AR-B-LAB-1",
          "prosecutor" -> Json.obj(
            "selected" -> Json.arr(),
            "unselected" -> Json.arr(),
            "unknowns" -> 0,
            "dropOut" -> 0.0
          ),
          "defense" -> Json.obj(
            "selected" -> Json.arr(),
            "unselected" -> Json.arr(),
            "unknowns" -> 0,
            "dropOut" -> 0.0
          ),
          "stats" -> Json.obj(
            "frequencyTable" -> "A",
            "probabilityModel" -> "1",
            "theta" -> 0,
            "dropIn" -> 0
          )
        ),
        "isRestricted" -> false
      )
      val scenario = json.as[Scenario]
      scenario._id.id must have length 24
      scenario._id.id must fullyMatch regex "[0-9a-f]{24}"
    }

    "round-trip serialize with full data" in {
      val scenario = Scenario(
        _id = MongoId("test-id-123"),
        name = "Scenario A",
        state = ScenarioStatus.Pending,
        geneticist = "Genetista",
        calculationScenario = CalculationScenario(
          sample = SampleCode("AR-B-LAB-1"),
          prosecutor = Hypothesis(List(SampleCode("AR-B-LAB-2")), List.empty, 3, 0.4),
          defense = Hypothesis(List.empty, List(SampleCode("AR-B-LAB-3")), 1, 0.5),
          stats = StatOption("A", "1", 1.0, 0.4, Some(0.5))
        ),
        date = MongoDate(new Date(1000000)),
        isRestricted = false,
        result = Some(LRResult(0.0, Map())),
        description = Some("Description A")
      )
      val json = Json.toJson(scenario)
      val deserialized = json.as[Scenario]
      deserialized.name mustBe scenario.name
      deserialized.state mustBe scenario.state
      deserialized.geneticist mustBe scenario.geneticist
      deserialized.isRestricted mustBe scenario.isRestricted
      deserialized.description mustBe scenario.description
      deserialized.calculationScenario.sample mustBe scenario.calculationScenario.sample
      deserialized.calculationScenario.prosecutor.unknowns mustBe 3
      deserialized.calculationScenario.prosecutor.dropOut mustBe 0.4
      deserialized.calculationScenario.defense.unknowns mustBe 1
      deserialized.calculationScenario.defense.dropOut mustBe 0.5
      deserialized.calculationScenario.stats.frequencyTable mustBe "A"
      deserialized.result.get.total mustBe 0.0
      deserialized.result.get.detailed.size mustBe 0
    }
  }
