package integration.controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.api.Application
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json

import security.{UserRepository, StubUserRepository}
import user.{RoleRepository, UsersModule}
import security.StubRoleRepository

class AuthenticationTest extends PlaySpec with GuiceOneAppPerTest {

  // Estrategia correcta: dejar que Play arranque normalmente con todos sus módulos
  // built-in (ActorSystem, ExecutionContext, etc.), solo deshabilitar UsersModule
  // (que intenta conectar a LDAP) y proveer stubs para los repositorios.
  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository]
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()

  "Authentication controller" must {

    "return 400 BadRequest for malformed JSON body on POST /api/v2/login" in {
      val request = FakeRequest(POST, "/api/v2/login")
        .withBody(Json.obj("wrong_field" -> "value"))

      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
    }

    "return 415 UnsupportedMediaType when sending plain text instead of JSON on POST /api/v2/login" in {
      val request = FakeRequest(POST, "/api/v2/login")
        .withTextBody("")

      val result = route(app, request).get

      // Play rechaza content-type incorrecto antes de leer el body
      status(result) mustBe UNSUPPORTED_MEDIA_TYPE
    }

    "have /api/v2/login route defined — credentials inválidas devuelven algo distinto a 405" in {
      // otp es un ConstrainedText que se serializa como string plano, no objeto
      val request = FakeRequest(POST, "/api/v2/login")
        .withBody(Json.obj(
          "userName" -> "testuser",
          "password" -> "testpass",
          "otp"      -> "123456"
        ))

      val result = route(app, request).get

      // Con el stub el bind siempre falla → autenticación falla
      // Lo que verificamos es que la ruta EXISTE (no devuelve 405 Method Not Allowed)
      status(result) must not be METHOD_NOT_ALLOWED
    }
  }
}
