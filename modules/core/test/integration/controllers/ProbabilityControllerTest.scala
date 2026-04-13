package integration.controllers

import org.scalatestplus.play.*
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.api.Application
import play.api.test.*
import play.api.test.Helpers.*
import play.api.libs.json.{Json, JsObject}

import fixtures.{StubLdapHealthService, StubProbabilityService, StubPopulationBaseFrequencyService}
import probability.{LRResult, ProbabilityModule, ProbabilityService}
import stats.{PopulationBaseFrequency, PopulationBaseFrequencyService, ProbabilityModel}
import types.StatOption
import security.{StubRoleRepository, StubUserRepository, UserRepository}
import user.{LdapHealthService, RoleRepository, UsersModule}

import scala.concurrent.Future

class ProbabilityControllerTest extends PlaySpec with GuiceOneAppPerTest {

  private var probabilityStub: StubProbabilityService = _
  private var popFreqStub: StubPopulationBaseFrequencyService = _

  override def fakeApplication(): Application =
    probabilityStub = new StubProbabilityService
    popFreqStub = new StubPopulationBaseFrequencyService
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .disable[ProbabilityModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository],
        bind[LdapHealthService].toInstance(new StubLdapHealthService),
        bind[ProbabilityService].toInstance(probabilityStub),
        bind[PopulationBaseFrequencyService].toInstance(popFreqStub)
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()

  // ---------------------------------------------------------------------------
  // Fixtures
  // ---------------------------------------------------------------------------

  private val statOption = StatOption(
    frequencyTable = "testTable",
    probabilityModel = "HardyWeinberg",
    theta = 0.01,
    dropIn = 0.01,
    dropOut = None
  )

  private val popFreq = PopulationBaseFrequency(
    name = "testTable",
    theta = 0.01,
    model = ProbabilityModel.HardyWeinberg,
    base = Seq.empty
  )

  /** Minimal valid FullCalculationScenario JSON — empty genotypifications and zero unknowns */
  private val validScenarioJson: JsObject = Json.obj(
    "sample" -> Json.obj(),
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
      "frequencyTable" -> "testTable",
      "probabilityModel" -> "HardyWeinberg",
      "theta" -> 0.01,
      "dropIn" -> 0.01,
      "dropOut" -> Json.toJson(Option.empty[Double])
    )
  )

  // ---------------------------------------------------------------------------
  // POST /api/v2/lr-mix
  // ---------------------------------------------------------------------------

  "ProbabilityController POST /api/v2/lr-mix" must {

    "return 200 OK with LRResult JSON when freq table is found" in {
      popFreqStub.getByNameResult = Future.successful(Some(popFreq))

      val request = FakeRequest(POST, "/api/v2/lr-mix")
        .withJsonBody(validScenarioJson)
      val result = route(app, request).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      // Empty sample → no loci → detailed is empty → total = 0.0 (no positive values)
      val lr = contentAsJson(result).as[LRResult]
      lr.total mustBe 0.0
      lr.detailed mustBe Map.empty
    }

    "return 404 Not Found when frequency table is not found" in {
      popFreqStub.getByNameResult = Future.successful(None)

      val request = FakeRequest(POST, "/api/v2/lr-mix")
        .withJsonBody(validScenarioJson)
      val result = route(app, request).get

      status(result) mustBe NOT_FOUND
    }

    "return 400 Bad Request when body is invalid JSON" in {
      val request = FakeRequest(POST, "/api/v2/lr-mix")
        .withJsonBody(Json.obj("bad" -> "payload"))
      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
    }

    "return 400 Bad Request when body is missing" in {
      val request = FakeRequest(POST, "/api/v2/lr-mix")
      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
    }
  }

  // ---------------------------------------------------------------------------
  // GET /api/v2/probability/stats/:laboratory
  // ---------------------------------------------------------------------------

  "ProbabilityController GET /api/v2/probability/stats/:laboratory" must {

    "return 200 OK with StatOption JSON when stats are found" in {
      probabilityStub.getStatsResult = Future.successful(Some(statOption))

      val request = FakeRequest(GET, "/api/v2/probability/stats/NME")
      val result = route(app, request).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsJson(result).as[StatOption] mustBe statOption
    }

    "return 404 Not Found when no stats exist for laboratory" in {
      probabilityStub.getStatsResult = Future.successful(None)

      val request = FakeRequest(GET, "/api/v2/probability/stats/UNKNOWN")
      val result = route(app, request).get

      status(result) mustBe NOT_FOUND
    }
  }
}
