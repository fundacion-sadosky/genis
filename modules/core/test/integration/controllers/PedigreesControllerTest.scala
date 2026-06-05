package integration.controllers

import fixtures.{StubLdapHealthService, StubProbabilityService, StubUserService}
import matching.{MatchingProcessStatus, MatchingProcessStatusImpl}
import org.scalatestplus.play.*
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import pedigree.*
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import probability.{ProbabilityModule, ProbabilityService}
import security.{StubRoleRepository, StubUserRepository, UserRepository}
import services.UserService
import trace.{TraceService, TraceServiceStub}
import user.{LdapHealthService, RoleRepository, UsersModule}

import scala.concurrent.{ExecutionContext, Future}

/** Verifies that PedigreesController.localize() actually resolves the
 *  "error.EXXXX|args" convention (Q1 — code-quality fix Bundle B) into the
 *  translated Spanish message defined in modules/core/conf/messages, instead
 *  of returning the raw key. Each spec wires a custom service stub that
 *  returns a known Left payload, then asserts the HTTP response body contains
 *  the substituted text. */
class PedigreesControllerTest extends PlaySpec with GuiceOneAppPerTest {

  // Stub configurable per-test para PedigreeMatchesService.discard
  private var matchesError: String = "error.E0642"

  private val pedigreeMatchesStub: PedigreeMatchesService = new PedigreeMatchesServiceStub:
    override def discard(matchId: String, userId: String, isSuperUser: Boolean): Future[Either[String, String]] =
      Future.successful(Left(matchesError))

  // Stub configurable para PedigreeService.changePedigreeStatus
  private var pedigreeStatusError: String = "error.E0930|UnderConstruction|UnderConstruction"

  private val pedigreeServiceStub: PedigreeService = new PedigreeServiceStub:
    override def changePedigreeStatus(pedigreeId: Long, status: PedigreeStatus.Value, userId: String, isSuperUser: Boolean): Future[Either[String, Long]] =
      Future.successful(Left(pedigreeStatusError))

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .disable[PedigreeModule]
      .disable[MutationModule]
      .disable[ProbabilityModule]
      .overrides(
        // auth/infra stubs
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository],
        bind[UserService].toInstance(new StubUserService),
        bind[LdapHealthService].toInstance(new StubLdapHealthService),
        bind[ProbabilityService].toInstance(new StubProbabilityService),
        bind[TraceService].toInstance(new TraceServiceStub),
        // pedigree domain stubs (replicate StubPedigreeModule por overrides)
        bind[BayesianNetworkService].toInstance(new BayesianNetworkServiceStub),
        bind[PedigreeService].toInstance(pedigreeServiceStub),
        bind[PedigreeMatchesService].toInstance(pedigreeMatchesStub),
        bind[PedigreeScenarioService].toInstance(new PedigreeScenarioServiceStub),
        bind[PedigreeGenotypificationService].toInstance(new PedigreeGenotypificationServiceStub),
        bind[PedCheckService].toInstance(new PedCheckServiceStub),
        bind[PedigreeMatcher].toInstance(new PedigreeMatcherStub),
        bind[MutationService].toInstance(new MutationServiceStub),
        bind[profiledata.ProfileDataService].toInstance(new profiledata.ProfileDataServiceStub),
        bind[MatchingProcessStatus].to[MatchingProcessStatusImpl],
        bind[search.FullTextSearchService].toInstance(new search.FullTextSearchServiceStub),
        bind[ExecutionContext].qualifiedWith("lrmix-context").toInstance(ExecutionContext.global)
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()

  "PedigreesController.localize (Q1)" must {

    "translate error.E0642 (sin args) en POST /api/v2/pedigree/discard" in {
      matchesError = "error.E0642"
      val result = route(app, FakeRequest(POST, "/api/v2/pedigree/discard?matchId=abc").withHeaders("X-USER" -> "alice")).get
      status(result) mustBe BAD_REQUEST
      val msg = (contentAsJson(result) \ "message").as[String]
      msg must include("E0642")
      msg must include("no tiene permisos")
      msg must not include "error.E0642"
    }

    "substitute args en error.E0930|UnderConstruction|Active vía POST /api/v2/pedigree/status" in {
      pedigreeStatusError = "error.E0930|UnderConstruction|Active"
      val req = FakeRequest(POST, "/api/v2/pedigree/status?id=1&status=Closed")
        .withHeaders("X-USER" -> "alice")
        .withJsonBody(play.api.libs.json.Json.obj())
      val result = route(app, req).get
      status(result) mustBe BAD_REQUEST
      val body = contentAsString(result)
      body must include("E0930")
      body must include("UnderConstruction")
      body must include("Active")
      body must not include "error.E0930"
      body must not include "{0}"
      body must not include "{1}"
    }

    "passthrough cuando el Left NO empieza con 'error.' (sin prefijo, devuelve string tal cual)" in {
      matchesError = "Plain text error message"
      val result = route(app, FakeRequest(POST, "/api/v2/pedigree/discard?matchId=abc").withHeaders("X-USER" -> "alice")).get
      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] mustBe "Plain text error message"
    }
  }
}
