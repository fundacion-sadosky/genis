package user

import scala.concurrent.Await
import scala.concurrent.duration.*

import org.scalatestplus.play.*
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

import security.UserRepository

// Integration test — requiere genis_ldap arriba (localhost:1389, base DN dc=genis,dc=local)
// con el usuario seed `setup` / `pass`. Ejercita el path real de LDAP que los unit tests no cubren:
// D1 (LdapExecutionContext con I/O bloqueante real) y D3 (mapeo de atributos con requiredAttribute).
class LdapUserRepositoryIntegrationTest extends PlaySpec with GuiceOneAppPerSuite {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()

  private lazy val repo = app.injector.instanceOf[UserRepository]

  "LdapUserRepository (contra genis_ldap real)" should {

    "bindear setup/pass correctamente sobre el dispatcher dedicado (D1)" in {
      Await.result(repo.bind("setup", "pass"), 10.seconds) mustBe true
    }

    "rechazar una password incorrecta" in {
      Await.result(repo.bind("setup", "password-incorrecta"), 10.seconds) mustBe false
    }

    "mapear los atributos obligatorios de setup sin NPE (D3)" in {
      val user = Await.result(repo.get("setup"), 10.seconds)
      user.userName mustBe "setup"
      user.roles must not be empty
    }
  }
}
