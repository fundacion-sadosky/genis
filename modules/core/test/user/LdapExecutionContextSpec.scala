package user

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.*

import org.scalatestplus.play.*
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder

import security.{StubRoleRepository, StubUserRepository, UserRepository}

// Verifica que el dispatcher dedicado `ldap-context` (configurado en application.conf)
// resuelve correctamente y que LdapExecutionContext es construible vía Guice. No necesita
// LDAP real: UsersModule se deshabilita y se inyectan stubs para evitar abrir conexiones.
class LdapExecutionContextSpec extends PlaySpec with GuiceOneAppPerTest {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository]
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()

  "LdapExecutionContext" should {

    "resolve the dedicated ldap-context dispatcher and run tasks" in {
      val ec = app.injector.instanceOf[LdapExecutionContext]
      val result = Await.result(Future("ok")(using ec), 5.seconds)
      result mustBe "ok"
    }
  }
}
