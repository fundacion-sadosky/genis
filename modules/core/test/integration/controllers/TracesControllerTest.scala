package integration.controllers

import fixtures.StubLdapHealthService
import org.scalatestplus.play.*
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import security.{StubUserRepository, StubRoleRepository, UserRepository}
import trace.{Trace, TraceSearch, TraceSearchPedigree, TraceService, TracePedigree, TraceModule}
import types.SampleCode
import user.{LdapHealthService, RoleRepository, UsersModule}

import java.util.Date
import scala.concurrent.Future

class TracesControllerTest extends PlaySpec with GuiceOneAppPerTest:

  import trace.{ProfileDataInfo, PedigreeDiscardInfo}

  private val dummyTrace = Trace(SampleCode("AR-B-SHDG-1234"), "userId", new Date(), ProfileDataInfo)
  private val dummyPedigreeTrace = TracePedigree(15L, "userId", new Date(), PedigreeDiscardInfo("x", 15L, "userId", 1))

  private val stubTraceService: TraceService = new TraceService:
    override def add(t: Trace): Future[Either[String, Long]]                          = Future.successful(Right(1L))
    override def search(ts: TraceSearch): Future[Seq[Trace]]                          = Future.successful(Seq(dummyTrace))
    override def count(ts: TraceSearch): Future[Int]                                  = Future.successful(1)
    override def searchPedigree(ts: TraceSearchPedigree): Future[Seq[TracePedigree]]  = Future.successful(Seq(dummyPedigreeTrace))
    override def countPedigree(ts: TraceSearchPedigree): Future[Int]                  = Future.successful(2)
    override def addTracePedigree(t: TracePedigree): Future[Either[String, Long]]     = Future.successful(Right(1L))
    override def getFullDescription(id: Long): Future[String]                         = Future.successful("Descripción completa")

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .disable[TraceModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository],
        bind[TraceService].toInstance(stubTraceService),
        bind[LdapHealthService].toInstance(new StubLdapHealthService)
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()

  private val validSearchBody = Json.obj(
    "page"        -> 0,
    "pageSize"    -> 30,
    "profile"     -> "AR-B-SHDG-1234",
    "user"        -> "userId",
    "isSuperUser" -> false
  )

  private val invalidBody = Json.obj("user" -> "userId")

  private val validPedigreeBody = Json.obj(
    "page"        -> 0,
    "pageSize"    -> 30,
    "pedigreeId"  -> 15,
    "user"        -> "userId",
    "isSuperUser" -> false
  )

  "POST /api/v2/trace/search" should:
    "return 200 with valid JSON body" in:
      val result = route(app, FakeRequest(POST, "/api/v2/trace/search").withJsonBody(validSearchBody)).get
      status(result) mustBe OK

    "return 400 with invalid JSON body" in:
      val result = route(app, FakeRequest(POST, "/api/v2/trace/search").withJsonBody(invalidBody)).get
      status(result) mustBe BAD_REQUEST

  "POST /api/v2/trace/total" should:
    "return 200 with X-TRACE-LENGTH header" in:
      val result = route(app, FakeRequest(POST, "/api/v2/trace/total").withJsonBody(validSearchBody)).get
      status(result) mustBe OK
      header("X-TRACE-LENGTH", result) mustBe Some("1")

    "return 400 with invalid JSON body" in:
      val result = route(app, FakeRequest(POST, "/api/v2/trace/total").withJsonBody(invalidBody)).get
      status(result) mustBe BAD_REQUEST

  "GET /api/v2/trace/full-description/:id" should:
    "return 200 with description" in:
      val result = route(app, FakeRequest(GET, "/api/v2/trace/full-description/1")).get
      status(result) mustBe OK

  "POST /api/v2/trace/search-pedigree" should:
    "return 200 with valid JSON body" in:
      val result = route(app, FakeRequest(POST, "/api/v2/trace/search-pedigree").withJsonBody(validPedigreeBody)).get
      status(result) mustBe OK

    "return 400 with invalid JSON body" in:
      val result = route(app, FakeRequest(POST, "/api/v2/trace/search-pedigree").withJsonBody(invalidBody)).get
      status(result) mustBe BAD_REQUEST

  "POST /api/v2/trace/total-pedigree" should:
    "return 200 with X-TRACE-LENGTH header" in:
      val result = route(app, FakeRequest(POST, "/api/v2/trace/total-pedigree").withJsonBody(validPedigreeBody)).get
      status(result) mustBe OK
      header("X-TRACE-LENGTH", result) mustBe Some("2")

    "return 400 with invalid JSON body" in:
      val result = route(app, FakeRequest(POST, "/api/v2/trace/total-pedigree").withJsonBody(invalidBody)).get
      status(result) mustBe BAD_REQUEST
