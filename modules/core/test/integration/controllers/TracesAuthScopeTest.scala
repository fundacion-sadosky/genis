package integration.controllers

import fixtures.{StubLdapHealthService, StubUserService}
import org.scalatestplus.play.*
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import security.{StubRoleRepository, StubUserRepository, UserRepository}
import services.UserService
import trace.{Trace, TraceModule, TracePedigree, TraceSearch, TraceSearchPedigree, TraceService}
import user.{LdapHealthService, RoleRepository, UsersModule}

import scala.concurrent.Future

/**
 * Gap G2 (#214 review): the legacy read `isSuperUser` from the request body (Json.format),
 * letting any client escalate to superuser scope and read everyone's traces. The migration
 * forces `isSuperUser = false` in the Reads and resolves it server-side from the session.
 * This test pins that boundary: a body claiming `isSuperUser = true` with NO session must
 * reach the service as `isSuperUser = false`.
 */
class TracesAuthScopeTest extends PlaySpec with GuiceOneAppPerTest:

  // Capturing stub: records the TraceSearch the controller actually passes to the service.
  private class CapturingTraceService extends TraceService:
    @volatile var lastSearch: Option[TraceSearch] = None
    @volatile var lastSearchPedigree: Option[TraceSearchPedigree] = None
    override def add(t: Trace): Future[Either[String, Long]]                         = Future.successful(Right(1L))
    override def search(ts: TraceSearch): Future[Seq[Trace]]                         = { lastSearch = Some(ts); Future.successful(Seq.empty) }
    override def count(ts: TraceSearch): Future[Int]                                 = { lastSearch = Some(ts); Future.successful(0) }
    override def searchPedigree(ts: TraceSearchPedigree): Future[Seq[TracePedigree]] = { lastSearchPedigree = Some(ts); Future.successful(Seq.empty) }
    override def countPedigree(ts: TraceSearchPedigree): Future[Int]                 = { lastSearchPedigree = Some(ts); Future.successful(0) }
    override def addTracePedigree(t: TracePedigree): Future[Either[String, Long]]    = Future.successful(Right(1L))
    override def getFullDescription(id: Long): Future[String]                        = Future.successful("")

  private val capturing = new CapturingTraceService

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .disable[TraceModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository],
        bind[TraceService].toInstance(capturing),
        bind[UserService].to[StubUserService],
        bind[LdapHealthService].toInstance(new StubLdapHealthService)
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()

  // Body deliberately tries to escalate via isSuperUser=true.
  private val escalatingBody = Json.obj(
    "page"        -> 0,
    "pageSize"    -> 30,
    "profile"     -> "AR-B-SHDG-1234",
    "user"        -> "userId",
    "isSuperUser" -> true
  )

  private val escalatingPedigreeBody = Json.obj(
    "page"        -> 0,
    "pageSize"    -> 30,
    "pedigreeId"  -> 15,
    "user"        -> "userId",
    "isSuperUser" -> true
  )

  "POST /api/v2/trace/search" must:
    "ignore isSuperUser from the body when there is no session (no privilege escalation)" in:
      val result = route(app, FakeRequest(POST, "/api/v2/trace/search").withJsonBody(escalatingBody)).get
      status(result) mustBe OK
      capturing.lastSearch.map(_.isSuperUser) mustBe Some(false)

  "POST /api/v2/trace/total" must:
    "ignore isSuperUser from the body when there is no session" in:
      val result = route(app, FakeRequest(POST, "/api/v2/trace/total").withJsonBody(escalatingBody)).get
      status(result) mustBe OK
      capturing.lastSearch.map(_.isSuperUser) mustBe Some(false)

  "POST /api/v2/trace/search-pedigree" must:
    "ignore isSuperUser from the body when there is no session" in:
      val result = route(app, FakeRequest(POST, "/api/v2/trace/search-pedigree").withJsonBody(escalatingPedigreeBody)).get
      status(result) mustBe OK
      capturing.lastSearchPedigree.map(_.isSuperUser) mustBe Some(false)
