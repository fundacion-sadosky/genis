package unit.user

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, JsNumber, JsString, JsSuccess, Json}
import user.UserStatus

class UserStatusTest extends AnyWordSpec with Matchers:

  "UserStatus" must {
    "have exactly 5 values" in {
      UserStatus.values.length mustBe 5
    }

    "contain active" in {
      UserStatus.valueOf("active") mustBe UserStatus.active
    }

    "contain pending" in {
      UserStatus.valueOf("pending") mustBe UserStatus.pending
    }

    "contain blocked" in {
      UserStatus.valueOf("blocked") mustBe UserStatus.blocked
    }

    "contain inactive" in {
      UserStatus.valueOf("inactive") mustBe UserStatus.inactive
    }

    "contain pending_reset" in {
      UserStatus.valueOf("pending_reset") mustBe UserStatus.pending_reset
    }
  }

  "UserStatus JSON Format" must {
    "serialize active to JsString" in {
      Json.toJson(UserStatus.active) mustBe JsString("active")
    }

    "deserialize JsString('blocked') to UserStatus.blocked" in {
      JsString("blocked").validate[UserStatus] mustBe JsSuccess(UserStatus.blocked)
    }

    "return JsError for unknown status string" in {
      JsString("test-fallido").validate[UserStatus] mustBe a[JsError]
    }

    "return JsError for non-string JSON" in {
      JsNumber(7).validate[UserStatus] mustBe a[JsError]
    }

    "roundtrip all values" in {
      UserStatus.values.foreach { status =>
        val json = Json.toJson(status)
        json.as[UserStatus] mustBe status
      }
    }
  }
