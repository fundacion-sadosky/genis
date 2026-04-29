package unit.audit

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.*

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import play.api.libs.streams.Accumulator
import play.api.mvc.{EssentialAction, RequestHeader, Result, Results}
import play.api.routing.{HandlerDef, Router}
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import audit.*
import security.AuthService
import types.TotpToken

class OperationLogFilterSpec extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfterAll:

  given system: ActorSystem = ActorSystem("op-log-filter-test")
  given mat: Materializer   = Materializer(system)
  given ec: ExecutionContext = system.dispatcher

  private val awaitTimeout = 5.seconds

  override protected def afterAll(): Unit =
    scala.concurrent.Await.result(system.terminate(), 5.seconds)
    super.afterAll()

  // Downstream action returning a known result; optionally sets X-CREATED-ID.
  private def downstream(result: Result = Results.Ok): EssentialAction =
    EssentialAction(_ => Accumulator.done(result))

  private def newAuth(
      publicPaths: Set[String] = Set.empty,
      interconnectionPaths: Set[String] = Set.empty
  ): AuthService =
    val auth = mock[AuthService]
    when(auth.isPublicResource(any[String])).thenAnswer(inv => publicPaths.contains(inv.getArgument[String](0)))
    when(auth.isInterconnectionResource(any[String])).thenAnswer(inv => interconnectionPaths.contains(inv.getArgument[String](0)))
    auth

  private def newService: OperationLogService =
    val svc = mock[OperationLogService]
    when(svc.add(any[OperationLogEntryAttemp])).thenReturn(Future.successful(()))
    svc

  private def runFilter(
      filter: OperationLogFilter,
      request: RequestHeader,
      result: Result = Results.Ok
  ): Result =
    val action = filter.apply(downstream(result))
    val run    = action(request).run()
    scala.concurrent.Await.result(run, awaitTimeout)

  private def captureEntry(svc: OperationLogService): OperationLogEntryAttemp =
    val captor = ArgumentCaptor.forClass(classOf[OperationLogEntryAttemp])
    verify(svc).add(captor.capture())
    captor.getValue

  "OperationLogFilter.shouldLog" must {

    "skip logging for public non-interconnection resources" in {
      val svc    = newService
      val auth   = newAuth(publicPaths = Set("/assets/x"))
      val filter = new OperationLogFilter(svc, auth, "build1")

      runFilter(filter, FakeRequest(GET, "/assets/x"))
      // Give the post-map Future a chance to run (it only runs inside shouldLog branch).
      Thread.sleep(50)
      verifyNoInteractions(svc)
    }

    "log for non-public resources" in {
      val svc    = newService
      val auth   = newAuth()
      val filter = new OperationLogFilter(svc, auth, "build1")

      runFilter(filter, FakeRequest(GET, "/api/v2/profiles"))
      eventually(awaitTimeout) { verify(svc).add(any[OperationLogEntryAttemp]) }
    }

    "log for interconnection resources even when marked public" in {
      val svc    = newService
      val auth   = newAuth(publicPaths = Set("/status"), interconnectionPaths = Set("/status"))
      val filter = new OperationLogFilter(svc, auth, "build1")

      runFilter(filter, FakeRequest(GET, "/status"))
      eventually(awaitTimeout) { verify(svc).add(any[OperationLogEntryAttemp]) }
    }

    "log paths starting with /login even when marked public" in {
      val svc    = newService
      val auth   = newAuth(publicPaths = Set("/login"))
      val filter = new OperationLogFilter(svc, auth, "build1")

      runFilter(filter, FakeRequest(POST, "/login"))
      eventually(awaitTimeout) { verify(svc).add(any[OperationLogEntryAttemp]) }
    }

    "log paths starting with /api/v2/login even when marked public" in {
      val svc    = newService
      val auth   = newAuth(publicPaths = Set("/api/v2/login"))
      val filter = new OperationLogFilter(svc, auth, "build1")

      runFilter(filter, FakeRequest(POST, "/api/v2/login"))
      eventually(awaitTimeout) { verify(svc).add(any[OperationLogEntryAttemp]) }
    }
  }

  "OperationLogFilter user resolution" must {

    "use X-USER header for non-interconnection resources" in {
      val svc    = newService
      val auth   = newAuth()
      val filter = new OperationLogFilter(svc, auth, "build1")

      val req = FakeRequest(GET, "/api/v2/profiles").withHeaders("X-USER" -> "alice")
      runFilter(filter, req)
      eventually(awaitTimeout) { verify(svc).add(any[OperationLogEntryAttemp]) }
      captureEntry(svc).userId mustBe "alice"
    }

    "use X-URL-INSTANCIA-INFERIOR header for interconnection resources" in {
      val svc    = newService
      val auth   = newAuth(interconnectionPaths = Set("/superior/profile"))
      val filter = new OperationLogFilter(svc, auth, "build1")

      val req = FakeRequest(GET, "/superior/profile").withHeaders(
        "X-URL-INSTANCIA-INFERIOR" -> "http://inferior.example",
        "X-USER"                   -> "ignored"
      )
      runFilter(filter, req)
      eventually(awaitTimeout) { verify(svc).add(any[OperationLogEntryAttemp]) }
      captureEntry(svc).userId mustBe "http://inferior.example"
    }

    "fall back to session when header is missing" in {
      val svc    = newService
      val auth   = newAuth()
      val filter = new OperationLogFilter(svc, auth, "build1")

      val req = FakeRequest(GET, "/api/v2/profiles").withSession("X-USER" -> "bob")
      runFilter(filter, req)
      eventually(awaitTimeout) { verify(svc).add(any[OperationLogEntryAttemp]) }
      captureEntry(svc).userId mustBe "bob"
    }

    "fall back to ANONYMOUS when neither header nor session is present" in {
      val svc    = newService
      val auth   = newAuth()
      val filter = new OperationLogFilter(svc, auth, "build1")

      runFilter(filter, FakeRequest(GET, "/api/v2/profiles"))
      eventually(awaitTimeout) { verify(svc).add(any[OperationLogEntryAttemp]) }
      captureEntry(svc).userId mustBe "ANONYMOUS"
    }
  }

  "OperationLogFilter metadata extraction" must {

    "map X-TOTP header to Some(TotpToken)" in {
      val svc    = newService
      val auth   = newAuth()
      val filter = new OperationLogFilter(svc, auth, "build1")

      val req = FakeRequest(GET, "/api/v2/profiles").withHeaders("X-TOTP" -> "654321")
      runFilter(filter, req)
      eventually(awaitTimeout) { verify(svc).add(any[OperationLogEntryAttemp]) }
      captureEntry(svc).otp mustBe Some(TotpToken("654321"))
    }

    "produce None for otp when X-TOTP is missing" in {
      val svc    = newService
      val auth   = newAuth()
      val filter = new OperationLogFilter(svc, auth, "build1")

      runFilter(filter, FakeRequest(GET, "/api/v2/profiles"))
      eventually(awaitTimeout) { verify(svc).add(any[OperationLogEntryAttemp]) }
      captureEntry(svc).otp mustBe None
    }

    "extract X-CREATED-ID from the downstream response into result" in {
      val svc    = newService
      val auth   = newAuth()
      val filter = new OperationLogFilter(svc, auth, "build1")

      val downstreamResult = Results.Created.withHeaders("X-CREATED-ID" -> "new-id-42")
      runFilter(filter, FakeRequest(POST, "/api/v2/profiles"), downstreamResult)
      eventually(awaitTimeout) { verify(svc).add(any[OperationLogEntryAttemp]) }
      captureEntry(svc).result mustBe Some("new-id-42")
    }

    "use HandlerDef attr to build the action string" in {
      val svc    = newService
      val auth   = newAuth()
      val filter = new OperationLogFilter(svc, auth, "build1")

      val handlerDef = HandlerDef(
        classLoader = getClass.getClassLoader,
        routerPackage = "",
        controller = "controllers.ProfilesController",
        method = "findByCode",
        parameterTypes = Nil,
        verb = "GET",
        path = "/api/v2/profiles",
        comments = "",
        modifiers = Seq.empty
      )
      val req = FakeRequest(GET, "/api/v2/profiles")
        .addAttr(Router.Attrs.HandlerDef, handlerDef)
      runFilter(filter, req)
      eventually(awaitTimeout) { verify(svc).add(any[OperationLogEntryAttemp]) }
      captureEntry(svc).action mustBe "controllers.ProfilesController.findByCode()"
    }

    "use \"NotFound\" as action when no HandlerDef attr is present" in {
      val svc    = newService
      val auth   = newAuth()
      val filter = new OperationLogFilter(svc, auth, "build1")

      runFilter(filter, FakeRequest(GET, "/api/v2/profiles"))
      eventually(awaitTimeout) { verify(svc).add(any[OperationLogEntryAttemp]) }
      captureEntry(svc).action mustBe "NotFound"
    }

    "carry method, path, buildNo and status to OperationLogEntryAttemp" in {
      val svc    = newService
      val auth   = newAuth()
      val filter = new OperationLogFilter(svc, auth, "my-build")

      runFilter(filter, FakeRequest(POST, "/api/v2/profiles"), Results.BadRequest)
      eventually(awaitTimeout) { verify(svc).add(any[OperationLogEntryAttemp]) }
      val entry = captureEntry(svc)
      entry.method  mustBe "POST"
      entry.path    mustBe "/api/v2/profiles"
      entry.buildNo mustBe "my-build"
      entry.status  mustBe 400
    }
  }

  "OperationLogFilter failure handling" must {

    "let the request succeed when operationLogService.add fails" in {
      val svc    = mock[OperationLogService]
      when(svc.add(any[OperationLogEntryAttemp]))
        .thenReturn(Future.failed(new RuntimeException("db down")))
      val auth   = newAuth()
      val filter = new OperationLogFilter(svc, auth, "build1")

      val result = runFilter(filter, FakeRequest(GET, "/api/v2/profiles"))
      result.header.status mustBe 200
    }
  }

  // Minimal polling until a mock assertion stops throwing or the timeout expires.
  private def eventually(timeout: FiniteDuration)(check: => Unit): Unit =
    val deadline = System.nanoTime() + timeout.toNanos
    var last: Throwable = null
    while System.nanoTime() < deadline do
      try
        check
        return
      catch
        case t: Throwable =>
          last = t
          Thread.sleep(20)
    if last != null then throw last
