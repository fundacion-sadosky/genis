package user

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, FiniteDuration, SECONDS}
import org.scalatestplus.play.PlaySpec
import specs.PdgSpec
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import services.CacheService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import stubs.Stubs

class RoleServiceTest extends PdgSpec with MockitoSugar{

  val duration: FiniteDuration = Duration(10, SECONDS)
  val userRepoMockProvider: UserRepository = mock[UserRepository]
  when(userRepoMockProvider.listAllUsers).thenReturn(Future(Seq(Stubs.ldapUser)))
  
  "A Role service" must {
    "reject the deletetion of a role if it's being assigned to a user" in {
      val target = new RoleServiceImpl(null, null, userRepoMockProvider)
      val f = target.deleteRole("geneticist")
      val res = Await.result(f, duration)
      res.isLeft mustBe true
    }
  }
}

class RoleServiceSimpleTest extends PlaySpec with MockitoSugar{

  val userRepoMockProvider: UserRepository = mock[UserRepository]
  val cacheServiceMock: CacheService = mock[CacheService]
  val roleRepositoryMock: RoleRepository = mock[RoleRepository]
  
  val target = new RoleServiceImpl(
    roleRepositoryMock,
    cacheServiceMock,
    userRepoMockProvider
  )
  
  "A Role service" must {
    "get all permissions" in {
      val result = target.listPermissions()
      result.size mustBe >(0)
    }

    "translate an operation in the most exact way possible" in {
      val result = target
        .translatePermission("GET", "/geneticist/50")

      result mustBe "GeneticistRead"
    }

    "avoid failing when a operation translation is not possible" in {
      val result = target.translatePermission("TRACE", "/geneticist/50")
      result mustBe "No valid operation found for: (TRACE, /geneticist/50)"
    }

    "get all operations" in {
      val result = target.listOperations()
      result.size mustBe >(0)
    }
  }
}
