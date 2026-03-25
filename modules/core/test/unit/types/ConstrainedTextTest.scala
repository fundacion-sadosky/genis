package unit.types

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, JsNumber, JsString, JsSuccess}
import types.{ConstrainedText, TotpToken}

class ConstrainedTextTest extends AnyWordSpec with Matchers:

  // Using TotpToken (pattern: ^\d{6}$) as concrete ConstrainedText

  "ConstrainedText" must {
    "accept a string matching the validation regex" in {
      val token = TotpToken("123456")
      token.text mustBe "123456"
    }

    "reject a string not matching the validation regex" in {
      an[IllegalArgumentException] must be thrownBy TotpToken("abcdef")
    }

    "reject an empty string" in {
      an[IllegalArgumentException] must be thrownBy TotpToken("")
    }

    "reject a partially matching string" in {
      an[IllegalArgumentException] must be thrownBy TotpToken("12345")
    }

    "be equal to another instance with the same text" in {
      TotpToken("123456") mustBe TotpToken("123456")
    }

    "not be equal to an instance with different text" in {
      TotpToken("123456") must not be TotpToken("654321")
    }
  }

  "ConstrainedText.readsOf" must {
    "parse a valid JSON string into the constrained type" in {
      val result = TotpToken.reads.reads(JsString("654321"))
      result mustBe a[JsSuccess[?]]
      result.get.text mustBe "654321"
    }

    "return JsError for a JSON string that fails validation" in {
      val result = TotpToken.reads.reads(JsString("abc"))
      result mustBe a[JsError]
    }

    "return JsError for non-string JSON types" in {
      val result = TotpToken.reads.reads(JsNumber(123456))
      result mustBe a[JsError]
    }
  }

  "ConstrainedText.writesOf" must {
    "serialize a constrained type to JsString" in {
      val token = TotpToken("999999")
      TotpToken.writes.writes(token) mustBe JsString("999999")
    }
  }

  "ConstrainedText.qsBinderOf" must {
    "bind a valid query string parameter" in {
      val params = Map("otp" -> Seq("123456"))
      val result = TotpToken.qsBinder.bind("otp", params)
      result mustBe Some(Right(TotpToken("123456")))
    }

    "return Left for invalid query string parameter" in {
      val params = Map("otp" -> Seq("abc"))
      val result = TotpToken.qsBinder.bind("otp", params)
      result match
        case Some(Left(_)) => succeed
        case other => fail(s"Expected Some(Left(...)), got $other")
    }

    "unbind to the original text" in {
      val token = TotpToken("123456")
      val result = TotpToken.qsBinder.unbind("otp", token)
      result must include("123456")
    }
  }

  "ConstrainedText.pathBinderOf" must {
    "bind a valid path parameter" in {
      val result = TotpToken.pathBinder.bind("otp", "123456")
      result mustBe Right(TotpToken("123456"))
    }

    "return Left for invalid path parameter" in {
      val result = TotpToken.pathBinder.bind("otp", "abc")
      result mustBe a[Left[?, ?]]
    }

    "unbind to the original text" in {
      val token = TotpToken("123456")
      TotpToken.pathBinder.unbind("otp", token) mustBe "123456"
    }
  }
