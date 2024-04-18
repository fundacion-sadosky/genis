package user

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import specs.PdgSpec
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import stubs.Stubs

class RoleServiceTest extends PdgSpec with MockitoSugar{

  val duration = Duration(10, SECONDS)
  
  val userRepoMockProvider = mock[UserRepository]
  when(userRepoMockProvider.listAllUsers).thenReturn(Future(Seq(Stubs.ldapUser)))
  
  "A Role service" must {
    "reject the deletetion of a role if it's being assigned to a user" in {
      val target = new RoleServiceImpl(null, null, userRepoMockProvider)
      val f = target.deleteRole("geneticist")
      val res = Await.result(f, duration)
      res.isLeft mustBe true
    }
  }

  "A Role service" must {
    "get all permissions" in {
      val target = new RoleServiceImpl(null, null, userRepoMockProvider)
      val result = target.listPermissions()

      result.size mustBe >(0)
    }
  }

  "A Role service" must {
    "translate an operation in the most exact way possible" in {
      val target = new RoleServiceImpl(null, null, userRepoMockProvider)
      val result = target.translatePermission("GET", "/geneticist/50")

      result mustBe "GeneticistRead"
    }
  }

  "A Role service" must {
    "get all operations" in {
      val target = new RoleServiceImpl(null, null, userRepoMockProvider)
      val result = target.listOperations()

      result.size mustBe >(0)
    }
  }
  
}