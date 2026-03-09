package unit.types

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import types.User

class UserTest extends AnyWordSpec with Matchers:

  "User JSON serialization" must {
    "roundtrip through format" in {
      val user = User("Juan", "Pérez", 42L)
      val json = Json.toJson(user)
      val parsed = json.as[User]
      parsed mustBe user
      parsed.firstName mustBe "Juan"
      parsed.lastName mustBe "Pérez"
      parsed.id mustBe 42L
    }
  }
