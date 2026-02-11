package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json

class DisclaimerControllerSpec extends PlaySpec with GuiceOneAppPerSuite {
  "DisclaimerController GET /api/v2/disclaimer" should {
    "return 200 and a disclaimer json" in {
      val request = FakeRequest(GET, "/api/v2/disclaimer")
      val result = route(app, request).get
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      (contentAsJson(result) \ "text").asOpt[String] must not be empty
    }
  }
}
