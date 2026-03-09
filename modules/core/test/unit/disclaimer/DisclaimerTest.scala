package unit.disclaimer

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import disclaimer.Disclaimer

class DisclaimerTest extends AnyWordSpec with Matchers:

  "Disclaimer JSON serialization" must {
    "roundtrip with text = Some" in {
      val d = Disclaimer(Some("Terms and conditions apply"))
      val json = Json.toJson(d)
      val parsed = json.as[Disclaimer]
      parsed mustBe d
      parsed.text mustBe Some("Terms and conditions apply")
    }

    "roundtrip with text = None" in {
      val d = Disclaimer(None)
      val json = Json.toJson(d)
      val parsed = json.as[Disclaimer]
      parsed mustBe d
      parsed.text mustBe None
    }
  }
