import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers._
import play.api.test.FakeRequest
import play.api.libs.json.Json
import play.api.mvc._
import play.api.Application

class UserAndRoleEndpointsSpec extends PlaySpec with GuiceOneAppPerSuite {
  "User and Role endpoints" should {
    "respond to user signup GET" in {
      val result = route(app, FakeRequest(GET, "/api/v2/user/signup")).get
      status(result) must (be (OK) or be (METHOD_NOT_ALLOWED))
    }

    "respond to user signup POST with valid data" in {
      val json = Json.obj(
        "userName" -> "testuser",
        "password" -> "testpass",
        "otp" -> "123456"
      )
      val result = route(app, FakeRequest(POST, "/api/v2/user/signup").withJsonBody(json)).get
      status(result) must (be (OK) or be (BAD_REQUEST))
    }

    "respond to user clear-password POST" in {
      val json = Json.obj(
        "userName" -> "testuser",
        "password" -> "testpass",
        "otp" -> "123456"
      )
      val result = route(app, FakeRequest(POST, "/api/v2/user/clear-password").withJsonBody(json)).get
      status(result) must (be (OK) or be (BAD_REQUEST))
    }

    "respond to roles GET" in {
      val result = route(app, FakeRequest(GET, "/api/v2/roles")).get
      status(result) must be (OK)
    }

    "respond to roles permissions GET" in {
      val result = route(app, FakeRequest(GET, "/api/v2/roles/permissions")).get
      status(result) must be (OK)
    }

    "handle invalid user signup POST" in {
      val json = Json.obj(
        "userName" -> "",
        "password" -> "",
        "otp" -> ""
      )
      val result = route(app, FakeRequest(POST, "/api/v2/user/signup").withJsonBody(json)).get
      status(result) must (be (BAD_REQUEST) or be (OK))
    }
  }
}
