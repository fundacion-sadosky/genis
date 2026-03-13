// test/unit/disclaimer/DisclaimerTest.scala
package unit.disclaimer

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, JsNull, JsNumber, JsString, JsSuccess, Json}
import disclaimer.Disclaimer

class DisclaimerTest extends AnyWordSpec with Matchers:

  "Disclaimer JSON Format" must {

    "serialize Disclaimer with text to JSON" in {
      val d = Disclaimer(Some("Accept terms and conditions"))
      val json = Json.toJson(d)
      (json \ "text").as[String] mustBe "Accept terms and conditions"
    }

    "deserialize JSON with text field to Disclaimer" in {
      val json = Json.obj("text" -> "Accept terms")
      json.as[Disclaimer] mustBe Disclaimer(Some("Accept terms"))
    }

    "roundtrip Disclaimer with Some(text)" in {
      val d = Disclaimer(Some("Some disclaimer text"))
      Json.toJson(d).as[Disclaimer] mustBe d
    }

    "roundtrip Disclaimer with None" in {
      val d = Disclaimer(None)
      Json.toJson(d).as[Disclaimer] mustBe d
    }

    "deserialize JSON with absent text field as None" in {
      val json = Json.obj()
      json.as[Disclaimer] mustBe Disclaimer(None)
    }

    "deserialize JSON with explicit null text field as None" in {
      val json = Json.obj("text" -> JsNull)
      json.as[Disclaimer] mustBe Disclaimer(None)
    }

    "return JsError for non-string text field" in {
      val json = Json.obj("text" -> JsNumber(42))
      json.validate[Disclaimer] mustBe a[JsError]
    }
  }
