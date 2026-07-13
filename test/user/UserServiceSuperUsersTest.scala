package user

import java.util.concurrent.atomic.AtomicInteger

import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import stubs.Stubs
import types.Permission
import types.Permission.USER_CRUD

import scala.collection.immutable.HashMap
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS

/**
 * findSuperUsers is called once per match / per proto-profile when a batch
 * is accepted, so it must not fire one LDAP search per call (incident
 * 2026-07-09: bursts of 300+ identical searches per second killed the single
 * shared search connection). These tests exercise the memoization only, with
 * a mocked repository: no LDAP / database needed.
 */
class UserServiceSuperUsersTest extends PlaySpec with MockitoSugar {

  val duration = Duration(10, SECONDS)
  val rolePermissions: Map[String, Set[Permission]] =
    HashMap("clerk" -> Set(USER_CRUD), "geneticist" -> Set(USER_CRUD))

  "UserService findSuperUsers" must {

    "fire a single ldap search for a burst of calls" in {

      val searchCount = new AtomicInteger(0)
      val inFlight = Promise[Seq[LdapUser]]()
      val userRepo = mock[UserRepository]
      when(userRepo.findSuperUsers()).thenAnswer(new Answer[Future[Seq[LdapUser]]] {
        override def answer(invocation: InvocationOnMock): Future[Seq[LdapUser]] = {
          searchCount.incrementAndGet()
          inFlight.future
        }
      })
      val roleService = mock[RoleService]
      when(roleService.getRolePermissions()).thenReturn(rolePermissions)

      val userService = new UserServiceImpl(userRepo, null, null, null, null, roleService)

      // burst like the one fired per accepted batch (one call per match):
      // the calls keep coming while the ldap search is still in flight
      val burst = (1 to 300).map { _ => userService.findSuperUsers() }

      searchCount.get mustBe 1

      inFlight.success(Seq(Stubs.ldapUser))

      burst.foreach { future =>
        Await.result(future, duration) mustBe Seq("user")
      }
      searchCount.get mustBe 1
    }

    "not keep a failed search cached" in {

      val searchCount = new AtomicInteger(0)
      val userRepo = mock[UserRepository]
      when(userRepo.findSuperUsers()).thenAnswer(new Answer[Future[Seq[LdapUser]]] {
        override def answer(invocation: InvocationOnMock): Future[Seq[LdapUser]] = {
          if (searchCount.incrementAndGet() == 1) {
            Future.failed(new RuntimeException("ldap down"))
          } else {
            Future.successful(Seq(Stubs.ldapUser))
          }
        }
      })
      val roleService = mock[RoleService]
      when(roleService.getRolePermissions()).thenReturn(rolePermissions)

      val userService = new UserServiceImpl(userRepo, null, null, null, null, roleService)

      val thrown = the[RuntimeException] thrownBy Await.result(userService.findSuperUsers(), duration)
      thrown.getMessage mustBe "ldap down"

      // the failure is evicted asynchronously; retry until the service recovers
      val deadline = System.currentTimeMillis() + 5000
      var result: Seq[String] = Seq.empty
      while (result.isEmpty && System.currentTimeMillis() < deadline) {
        try {
          result = Await.result(userService.findSuperUsers(), duration)
        } catch {
          case _: RuntimeException => Thread.sleep(50)
        }
      }
      result mustBe Seq("user")
    }

  }

}
