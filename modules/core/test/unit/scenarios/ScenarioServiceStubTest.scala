package unit.scenarios

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import scenarios.*
import matching.MongoId
import types.{MongoDate, SampleCode, StatOption}
import probability.LRResult

import java.util.Date
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.*

class ScenarioServiceStubTest extends AnyWordSpec with Matchers:

  private val timeout = 2.seconds

  private val sampleScenario = Scenario(
    _id = MongoId("test-id"),
    name = "Test",
    state = ScenarioStatus.Pending,
    geneticist = "user1",
    calculationScenario = CalculationScenario(
      sample = SampleCode("AR-B-LAB-1"),
      prosecutor = Hypothesis(List.empty, List.empty, 0, 0.0),
      defense = Hypothesis(List.empty, List.empty, 0, 0.0),
      stats = StatOption("table", "model", 0.0, 0.0, None)
    ),
    date = MongoDate(new Date()),
    isRestricted = false,
    result = None,
    description = None
  )

  "ScenarioServiceStub" must {
    "return None for getLRMix" in {
      val stub = new ScenarioServiceStub
      val result = Await.result(stub.getLRMix(sampleScenario.calculationScenario), timeout)
      result mustBe None
    }

    "return empty for findExistingMatches" in {
      val stub = new ScenarioServiceStub
      val result = Await.result(stub.findExistingMatches(sampleScenario), timeout)
      result mustBe empty
    }

    "return empty for findMatchesForScenario" in {
      val stub = new ScenarioServiceStub
      val result = Await.result(stub.findMatchesForScenario("user1", SampleCode("AR-B-LAB-1"), false), timeout)
      result mustBe empty
    }

    "return empty for findMatchesForRestrainedScenario" in {
      val stub = new ScenarioServiceStub
      val result = Await.result(stub.findMatchesForRestrainedScenario("user1", SampleCode("AR-B-LAB-1"), SampleCode("AR-B-LAB-2"), false), timeout)
      result mustBe empty
    }

    "return empty for search" in {
      val stub = new ScenarioServiceStub
      val search = ScenarioSearch(profile = SampleCode("AR-B-LAB-1"))
      val result = Await.result(stub.search("user1", search, false), timeout)
      result mustBe empty
    }

    "return Right for add" in {
      val stub = new ScenarioServiceStub
      val result = Await.result(stub.add(sampleScenario), timeout)
      result mustBe Right("test-id")
    }

    "return Right for delete" in {
      val stub = new ScenarioServiceStub
      val result = Await.result(stub.delete("user1", MongoId("test-id"), false), timeout)
      result mustBe Right("test-id")
    }

    "return None for get" in {
      val stub = new ScenarioServiceStub
      val result = Await.result(stub.get("user1", MongoId("test-id"), false), timeout)
      result mustBe None
    }

    "return Right for validate" in {
      val stub = new ScenarioServiceStub
      val result = Await.result(stub.validate("user1", sampleScenario, false), timeout)
      result mustBe Right("test-id")
    }

    "return Right for update" in {
      val stub = new ScenarioServiceStub
      val result = Await.result(stub.update("user1", sampleScenario, false), timeout)
      result mustBe Right("test-id")
    }

    "return Left for getNCorrection" in {
      val stub = new ScenarioServiceStub
      val request = NCorrectionRequest(SampleCode("AR-C-HIBA-500"), SampleCode("AR-B-IMBICE-500"), 1000, 2.0)
      val result = Await.result(stub.getNCorrection(request), timeout)
      result.isLeft mustBe true
    }
  }

  "ScenarioRepositoryStub" must {
    "return None for get by id" in {
      val stub = new ScenarioRepositoryStub
      val result = Await.result(stub.get("user1", MongoId("test-id"), false), timeout)
      result mustBe None
    }

    "return empty for get by search" in {
      val stub = new ScenarioRepositoryStub
      val result = Await.result(stub.get("user1", ScenarioSearch(SampleCode("AR-B-LAB-1")), false), timeout)
      result mustBe empty
    }

    "return Right for add" in {
      val stub = new ScenarioRepositoryStub
      val result = Await.result(stub.add(sampleScenario), timeout)
      result mustBe Right("test-id")
    }

    "return Right for delete" in {
      val stub = new ScenarioRepositoryStub
      val result = Await.result(stub.delete("user1", MongoId("test-id"), false), timeout)
      result mustBe Right("test-id")
    }

    "return Right for validate" in {
      val stub = new ScenarioRepositoryStub
      val result = Await.result(stub.validate("user1", sampleScenario, false), timeout)
      result mustBe Right("test-id")
    }

    "return Right for update" in {
      val stub = new ScenarioRepositoryStub
      val result = Await.result(stub.update("user1", sampleScenario, false), timeout)
      result mustBe Right("test-id")
    }

    "return empty for getByProfile" in {
      val stub = new ScenarioRepositoryStub
      val result = Await.result(stub.getByProfile(SampleCode("AR-B-LAB-1")), timeout)
      result mustBe empty
    }

    "return empty for getByMatch" in {
      val stub = new ScenarioRepositoryStub
      val result = Await.result(stub.getByMatch(SampleCode("AR-B-LAB-1"), SampleCode("AR-B-LAB-2"), "user1", false), timeout)
      result mustBe empty
    }
  }
