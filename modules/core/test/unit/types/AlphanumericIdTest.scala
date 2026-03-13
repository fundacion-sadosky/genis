package unit.types

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsString, Json}
import types.AlphanumericId

class AlphanumericIdTest extends AnyWordSpec with Matchers:

  "AlphanumericId" must {
    "accept a 2-character alphanumeric string" in {
      AlphanumericId("AB").text mustBe "AB"
    }

    "accept a longer alphanumeric string with digits" in {
      AlphanumericId("test123").text mustBe "test123"
    }

    "accept a string with underscores" in {
      AlphanumericId("with_underscore").text mustBe "with_underscore"
    }

    "reject a single character" in {
      an[IllegalArgumentException] must be thrownBy AlphanumericId("A")
    }

    "reject an empty string" in {
      an[IllegalArgumentException] must be thrownBy AlphanumericId("")
    }

    "reject a string with hyphens" in {
      an[IllegalArgumentException] must be thrownBy AlphanumericId("ab-cd")
    }

    "reject a string with spaces" in {
      an[IllegalArgumentException] must be thrownBy AlphanumericId("ab cd")
    }
  }

  "AlphanumericId JSON serialization" must {
    "roundtrip through reads/writes" in {
      val id = AlphanumericId("TLAB01")
      val json = Json.toJson(id)
      json mustBe JsString("TLAB01")
      json.as[AlphanumericId].text mustBe "TLAB01"
    }
  }

  "AlphanumericId.qsBinder" must {
    "bind a valid query string parameter" in {
      val params = Map("id" -> Seq("ABC"))
      AlphanumericId.qsBinder.bind("id", params) mustBe Some(Right(AlphanumericId("ABC")))
    }

    "return Left for invalid query string parameter" in {
      val params = Map("id" -> Seq("A"))
      AlphanumericId.qsBinder.bind("id", params) match
        case Some(Left(_)) => succeed
        case other => fail(s"Expected Some(Left(...)), got $other")
    }

    "unbind to the original text" in {
      AlphanumericId.qsBinder.unbind("id", AlphanumericId("ABC")) must include("ABC")
    }
  }

  "AlphanumericId.pathBinder" must {
    "bind a valid path parameter" in {
      AlphanumericId.pathBinder.bind("id", "ABC") mustBe Right(AlphanumericId("ABC"))
    }

    "return Left for invalid path parameter" in {
      AlphanumericId.pathBinder.bind("id", "A") mustBe a[Left[?, ?]]
    }

    "unbind to the original text" in {
      AlphanumericId.pathBinder.unbind("id", AlphanumericId("ABC")) mustBe "ABC"
    }
  }
