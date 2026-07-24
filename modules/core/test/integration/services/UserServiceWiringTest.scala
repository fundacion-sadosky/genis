package integration.services

import scala.concurrent.ExecutionContext

import org.scalatestplus.play.*
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder

import fixtures.StubLdapHealthService
import kits.StrKitModule
import probability.ProbabilityModule
import security.{StubRoleRepository, StubUserRepository, UserRepository}
import services.UserService
import user.{LdapHealthService, RoleRepository, UsersModule}

/**
 * Guards the two wiring facts the superuser memo (issue #304) depends on and that no
 * unit test can see: the named `superUsersCacheTtl` binding resolves, and the service
 * is a singleton — otherwise the memo would live per injection point and do nothing.
 * No Docker required: LDAP and infrastructure services are stubbed.
 */
class UserServiceWiringTest extends PlaySpec with GuiceOneAppPerTest {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .disable[StrKitModule]
      .disable[ProbabilityModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository],
        new fixtures.StubStrKitModule,
        new fixtures.StubProbabilityModule,
        bind[ExecutionContext].qualifiedWith("lrmix-context").toInstance(ExecutionContext.global),
        bind[LdapHealthService].toInstance(new StubLdapHealthService)
      )
      .build()

  "UserService wiring" must {

    "resolve UserServiceImpl with the named superUsersCacheTtl binding" in {
      app.injector.instanceOf[UserService].getClass.getName must include("UserServiceImpl")
    }

    "be a singleton, so the superuser memo is process-wide" in {
      app.injector.instanceOf[UserService] must be theSameInstanceAs app.injector.instanceOf[UserService]
    }
  }
}
