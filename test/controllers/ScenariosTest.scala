package controllers

import java.util.Date

import matching.{MatchResult, MatchingCalculatorService, MatchingService, NewMatchingResult}
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, status, _}
import probability.LRResult
import profile.{Profile, ProfileService}
import scenarios._
import specs.PdgSpec
import stubs.Stubs
import types.{MongoDate, MongoId, SampleCode, StatOption}
import user.UserService

import scala.concurrent.Future

class ScenariosTest extends PdgSpec with MockitoSugar with Results {

  "A Scenarios Controller" must {
    "find options for existing scenario" in {
      val validated = Scenario(MongoId("1"), "Test", ScenarioStatus.Pending,
                "esurijon", Stubs.calculationScenario, MongoDate(new Date()), false, None, None)

      val mockResult = List(Stubs.scenarioOption)
      val mockScenarioService = mock[ScenarioService]
      when(mockScenarioService.get(Matchers.eq(validated.geneticist), any[MongoId], any[Boolean])).thenReturn(Future.successful(Some(validated)))
      when(mockScenarioService.findExistingMatches(any[Scenario])).thenReturn(Future.successful(mockResult))
      val userService = mock[UserService]
      when(userService.isSuperUser(validated.geneticist)).thenReturn(Future.successful((false)))

      val target = new Scenarios(mockScenarioService, null, null, null, userService = userService)
      val request = FakeRequest().withHeaders("X-USER" -> validated.geneticist).withHeaders("X-SUPERUSER" -> "false")
      val result: Future[Result] = target.findMatches(Some("1"), None, None).apply(request)

      status(result) mustBe OK
      contentAsJson(result).as[List[ScenarioOption]] mustBe mockResult
    }

    "find options for new restricted scenario" in {
      val mockResult = List(Stubs.scenarioOption)
      val mockScenarioService = mock[ScenarioService]
      when(mockScenarioService.findMatchesForRestrainedScenario(Matchers.eq("esurijon"), any[SampleCode], any[SampleCode], any[Boolean])).thenReturn(Future.successful(mockResult))
      val userService = mock[UserService]
      when(userService.isSuperUser("esurijon")).thenReturn(Future.successful((false)))

      val target = new Scenarios(mockScenarioService, null, null, null, userService = userService)
      val request = FakeRequest().withHeaders("X-USER" -> "esurijon").withHeaders("X-SUPERUSER" -> "false")
      val result: Future[Result] = target.findMatches(None, Some(SampleCode("AR-B-SHDG-1")), Some(SampleCode("AR-B-SHDG-2"))).apply(request)

      status(result) mustBe OK
      contentAsJson(result).as[List[ScenarioOption]] mustBe mockResult
    }

    "find options for new scenario" in {
      val mockResult = List(Stubs.scenarioOption)
      val mockScenarioService = mock[ScenarioService]
      when(mockScenarioService.findMatchesForScenario(Matchers.eq("esurijon"), any[SampleCode], any[Boolean])).thenReturn(Future.successful(mockResult))
      val userService = mock[UserService]
      when(userService.isSuperUser("esurijon")).thenReturn(Future.successful((false)))

      val target = new Scenarios(mockScenarioService, null, null, null, userService = userService)
      val request = FakeRequest().withHeaders("X-USER" -> "esurijon").withHeaders("X-SUPERUSER" -> "false")
      val result: Future[Result] = target.findMatches(None, Some(SampleCode("AR-B-SHDG-1")), None).apply(request)

      status(result) mustBe OK
      contentAsJson(result).as[List[ScenarioOption]] mustBe mockResult
    }


  }

  "calculateLRMix" must {
    "fail with invalid input" in {
      val target = new Scenarios(null, null, null, null)
      val request = FakeRequest().withBody(Json.obj("sample" -> "AR-C-SHDG-1108"))

      val result: Future[Result] = target.calculateLRMix().apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "validate input" must {
      val scenario = CalculationScenario(Stubs.sampleCode, Hypothesis(List(Stubs.sampleCode), List(), 0, 0),
        Hypothesis(List(), List(Stubs.sampleCode), 1, 0), StatOption("baufest", "model", 0, 0, Some(0)))

      val mockScenarioService = mock[ScenarioService]
      when(mockScenarioService.getLRMix(any[CalculationScenario],any[Option[NewMatchingResult.AlleleMatchRange]])).thenReturn(Future.successful(Some(LRResult(0.0, Map()))))

      val target = new Scenarios(mockScenarioService, null, null, null)

      val request = FakeRequest().withBody(Json.toJson(scenario))

      val result: Future[Result] = target.calculateLRMix().apply(request)

      status(result) mustBe OK
    }
  }

  "A Scenarios Controller" must {
    "search bad request" in {
      val userService = mock[UserService]
      when(userService.isSuperUser("geneticist")).thenReturn(Future.successful((false)))
      val request = FakeRequest().withHeaders("X-USER" -> "geneticist").withHeaders("X-SUPERUSER" -> "false").withBody(Json.obj())
      val target = new Scenarios(null, null, null, null, userService = userService)

      val result: Future[Result] = target.search().apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "search ok" in {
      val scenarioSearch = ScenarioSearch(Stubs.sampleCode)
      val scenario = Stubs.newScenario
      val jsRequest = Json.toJson(scenarioSearch)
      val scenarioServiceMock = mock[ScenarioService]
      when(scenarioServiceMock.search("geneticist", scenarioSearch, false)).thenReturn(Future.successful(List(scenario)))
      val userService = mock[UserService]
      when(userService.isSuperUser("geneticist")).thenReturn(Future.successful((false)))

      val request = FakeRequest().withHeaders("X-USER" -> "geneticist").withHeaders("X-SUPERUSER" -> "false").withBody(jsRequest)
      val target = new Scenarios(scenarioServiceMock, null, null, null, userService = userService)
      val result: Future[Result] = target.search().apply(request)

      status(result) mustBe OK
      contentType(result).get mustBe "application/json"
    }

    "create bad request" in {
      val request = FakeRequest().withBody(Json.obj())
      val target = new Scenarios(null, null, null, null)
      val result: Future[Result] = target.create().apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "create case left" in {
      val scenario = Stubs.newScenario
      val jsRequest = Json.toJson(scenario)
      val scenarioServiceMock = mock[ScenarioService]
      when(scenarioServiceMock.add(scenario)).thenReturn(Future.successful(Left("Error")))

      val request = FakeRequest().withBody(jsRequest)
      val target = new Scenarios(scenarioServiceMock, null, null, null)
      val result: Future[Result] = target.create().apply(request)

      status(result) mustBe BAD_REQUEST
      contentType(result).get mustBe "application/json"

      val jsonVal = contentAsJson(result).as[String]

      jsonVal mustBe "Error"
    }

    "create case right" in {
      val scenario = Stubs.newScenario
      val jsRequest = Json.toJson(scenario)
      val scenarioServiceMock = mock[ScenarioService]
      when(scenarioServiceMock.add(scenario)).thenReturn(Future.successful(Right("Id")))

      val request = FakeRequest().withBody(jsRequest)
      val target = new Scenarios(scenarioServiceMock, null, null, null)
      val result: Future[Result] = target.create().apply(request)

      status(result) mustBe OK
      contentType(result).get mustBe "application/json"
    }

    "update bad request" in {
      val userService = mock[UserService]
      when(userService.isSuperUser("geneticist")).thenReturn(Future.successful((false)))

      val request = FakeRequest().withHeaders("X-USER" -> "geneticist").withHeaders("X-SUPERUSER" -> "false").withBody(Json.obj())
      val target = new Scenarios(null, null, null, null, userService = userService)
      val result: Future[Result] = target.update().apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "update case left" in {
      val scenario = Stubs.newScenario
      val jsRequest = Json.toJson(scenario)
      val scenarioServiceMock = mock[ScenarioService]
      when(scenarioServiceMock.update("geneticist", scenario, false)).thenReturn(Future.successful(Left("Error")))
      val userService = mock[UserService]
      when(userService.isSuperUser("geneticist")).thenReturn(Future.successful((false)))

      val request = FakeRequest().withHeaders("X-USER" -> "geneticist").withHeaders("X-SUPERUSER" -> "false").withBody(jsRequest)
      val target = new Scenarios(scenarioServiceMock, null, null, null, userService = userService)
      val result: Future[Result] = target.update().apply(request)

      status(result) mustBe BAD_REQUEST
      contentType(result).get mustBe "application/json"

      val jsonVal = contentAsJson(result).as[String]

      jsonVal mustBe "Error"
    }

    "update case right" in {
      val scenario = Stubs.newScenario
      val jsRequest = Json.toJson(scenario)
      val scenarioServiceMock = mock[ScenarioService]
      when(scenarioServiceMock.update("geneticist", scenario, false)).thenReturn(Future.successful(Right("Id")))
      val userService = mock[UserService]
      when(userService.isSuperUser("geneticist")).thenReturn(Future.successful((false)))

      val request = FakeRequest().withHeaders("X-USER" -> "geneticist").withHeaders("X-SUPERUSER" -> "false").withBody(jsRequest)
      val target = new Scenarios(scenarioServiceMock, null, null, null, userService = userService)
      val result: Future[Result] = target.update().apply(request)

      status(result) mustBe OK
      contentType(result).get mustBe "application/json"
    }

    "delete case left" in {
      val scenario = Stubs.newScenario
      val jsRequest = Json.toJson(scenario._id)
      val scenarioServiceMock = mock[ScenarioService]
      when(scenarioServiceMock.delete("geneticist", scenario._id, false)).thenReturn(Future.successful(Left("Error")))
      val userService = mock[UserService]
      when(userService.isSuperUser("geneticist")).thenReturn(Future.successful((false)))

      val request = FakeRequest().withHeaders("X-USER" -> "geneticist").withHeaders("X-SUPERUSER" -> "false").withBody(jsRequest)
      val target = new Scenarios(scenarioServiceMock, null, null, null, userService = userService)
      val result: Future[Result] = target.search().apply(request)

      status(result) mustBe BAD_REQUEST
      contentType(result).get mustBe "application/json"
    }

    "delete case right" in {
      val scenario = Stubs.newScenario
      val jsRequest = Json.toJson(scenario._id)
      val scenarioServiceMock = mock[ScenarioService]
      when(scenarioServiceMock.delete("geneticist", scenario._id, false)).thenReturn(Future.successful(Right("Id")))
      val userService = mock[UserService]
      when(userService.isSuperUser("geneticist")).thenReturn(Future.successful((false)))

      val request = FakeRequest().withHeaders("X-USER" -> "geneticist").withHeaders("X-SUPERUSER" -> "false").withBody(jsRequest)
      val target = new Scenarios(scenarioServiceMock, null, null, null, userService = userService)
      val result: Future[Result] = target.delete().apply(request)

      status(result) mustBe OK
      contentType(result).get mustBe "application/json"
    }

    "validate bad request" in {
      val userService = mock[UserService]
      when(userService.isSuperUser("geneticist")).thenReturn(Future.successful((false)))

      val request = FakeRequest().withHeaders("X-USER" -> "geneticist").withHeaders("X-SUPERUSER" -> "false").withBody(Json.obj())
      val target = new Scenarios(null, null, null, null, userService = userService)
      val result: Future[Result] = target.validate().apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "validate case left" in {
      val scenario = Stubs.newScenario
      val jsRequest = Json.toJson(scenario)
      val matchingServiceMock = mock[MatchingService]
      when(matchingServiceMock.validate(scenario)).thenReturn(Future.successful(Right("Ok")))
      val scenarioServiceMock = mock[ScenarioService]
      when(scenarioServiceMock.validate("geneticist", scenario, false)).thenReturn(Future.successful(Left("Error")))
      val userService = mock[UserService]
      when(userService.isSuperUser("geneticist")).thenReturn(Future.successful((false)))

      val request = FakeRequest().withHeaders("X-USER" -> "geneticist").withHeaders("X-SUPERUSER" -> "false").withBody(jsRequest)
      val target = new Scenarios(scenarioServiceMock, matchingServiceMock, null, null, userService = userService)
      val result: Future[Result] = target.validate().apply(request)

      status(result) mustBe BAD_REQUEST
      contentType(result).get mustBe "application/json"

    }

    "validate case right" in {
      val scenario = Stubs.newScenario
      val jsRequest = Json.toJson(scenario)

      val scenarioServiceMock = mock[ScenarioService]
      when(scenarioServiceMock.validate("geneticist", scenario, false)).thenReturn(Future.successful(Right("Id")))

      val matchingServiceMock = mock[MatchingService]
      when(matchingServiceMock.validate(scenario)).thenReturn(Future.successful(Right("Ok")))
      val userService = mock[UserService]
      when(userService.isSuperUser("geneticist")).thenReturn(Future.successful((false)))

      val request = FakeRequest().withHeaders("X-USER" -> "geneticist").withHeaders("X-SUPERUSER" -> "false").withBody(jsRequest)
      val target = new Scenarios(scenarioServiceMock, matchingServiceMock, null, null, userService = userService)
      val result: Future[Result] = target.validate().apply(request)

      status(result) mustBe OK
      contentType(result).get mustBe "application/json"
    }

    "get case none" in {
      val scenario = Stubs.newScenario
      val jsRequest = Json.toJson(scenario._id)

      val scenarioServiceMock = mock[ScenarioService]
      when(scenarioServiceMock.get(scenario.geneticist, scenario._id, false)).thenReturn(Future.successful(None))
      val userService = mock[UserService]
      when(userService.isSuperUser(scenario.geneticist)).thenReturn(Future.successful((false)))

      val request = FakeRequest().withHeaders("X-USER" -> scenario.geneticist).withHeaders("X-SUPERUSER" -> "false").withBody(jsRequest)
      val target = new Scenarios(scenarioServiceMock, null, null, null, userService = userService)
      val result: Future[Result] = target.get().apply(request)

      status(result) mustBe NOT_FOUND
    }

    "get case ok" in {
      val scenario = Stubs.newScenario
      val jsRequest = Json.toJson(scenario._id)

      val scenarioServiceMock = mock[ScenarioService]
      when(scenarioServiceMock.get(scenario.geneticist, scenario._id, false)).thenReturn(Future.successful(Some(scenario)))
      val userService = mock[UserService]
      when(userService.isSuperUser(scenario.geneticist)).thenReturn(Future.successful((false)))

      val request = FakeRequest().withHeaders("X-USER" -> scenario.geneticist).withHeaders("X-SUPERUSER" -> "false").withBody(jsRequest)
      val target = new Scenarios(scenarioServiceMock, null, null, null, userService = userService)
      val result: Future[Result] = target.get().apply(request)

      status(result) mustBe OK
    }

    "get default scenario case bad request" in {
      val request = FakeRequest().withBody(Json.obj("stats" -> 1))
      val target = new Scenarios(null, null, null, null)
      val result: Future[Result] = target.getDefaultScenario(Stubs.sampleCode, Stubs.sampleCode).apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "get default scenario case ok" in {
      val scenario = Stubs.newScenario

      val scenarioServiceMock = mock[ScenarioService]
      when(scenarioServiceMock.get(scenario.geneticist, scenario._id, false)).thenReturn(Future.successful(Some(scenario)))

      val matchingCalculatorService = mock[MatchingCalculatorService]
      when(matchingCalculatorService.createDefaultScenario(any[Profile], any[Profile], any[StatOption])).thenReturn(scenario.calculationScenario)

      val profileService = mock[ProfileService]
      when(profileService.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.newProfile)))

      val request = FakeRequest().withBody(Json.toJson(Stubs.statOption))
      val target = new Scenarios(scenarioServiceMock, null, matchingCalculatorService, profileService)
      val result: Future[Result] = target.getDefaultScenario(Stubs.sampleCode, Stubs.sampleCode).apply(request)

      status(result) mustBe OK
    }
  }
}