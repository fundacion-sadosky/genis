package unit.types

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsString, Json}
import types.TotpToken

class TotpTokenTest extends AnyWordSpec with Matchers:

  "TotpToken" must {
    "accept a valid 6-digit string '000000'" in {
      TotpToken("000000").text mustBe "000000"
    }

    "accept a valid 6-digit string '123456'" in {
      TotpToken("123456").text mustBe "123456"
    }

    "accept a valid 6-digit string '999999'" in {
      TotpToken("999999").text mustBe "999999"
    }

    "reject strings shorter than 6 digits" in {
      an[IllegalArgumentException] must be thrownBy TotpToken("12345")
    }

    "reject strings longer than 6 digits" in {
      an[IllegalArgumentException] must be thrownBy TotpToken("1234567")
    }

    "reject strings with non-digit characters" in {
      an[IllegalArgumentException] must be thrownBy TotpToken("12345a")
    }

    "reject empty string" in {
      an[IllegalArgumentException] must be thrownBy TotpToken("")
    }
  }

  "TotpToken JSON serialization" must {
    "roundtrip through reads/writes" in {
      val token = TotpToken("654321")
      val json = Json.toJson(token)
      json mustBe JsString("654321")
      json.as[TotpToken].text mustBe "654321"
    }
  }
