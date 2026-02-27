package unit.user

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import user.Role
import types.Permission

class RoleTest extends AnyWordSpec with Matchers:

  "Role" must {
    "serialize and deserialize via JSON Format" in {
      val role = Role("admin", "Administrator", Set(Permission.DNA_PROFILE_CRUD))
      val json = Json.toJson(role)
      val roundtripped = json.as[Role]
      roundtripped.id mustBe "admin"
      roundtripped.roleName mustBe "Administrator"
      roundtripped.permissions must contain(Permission.DNA_PROFILE_CRUD)
    }

    "handle empty permissions set" in {
      val role = Role("empty", "Empty Role", Set.empty)
      val roundtripped = Json.toJson(role).as[Role]
      roundtripped.permissions mustBe empty
    }

    "handle multiple permissions" in {
      val role = Role("multi", "Multi Role", Set(Permission.DNA_PROFILE_CRUD, Permission.USER_CRUD))
      val roundtripped = Json.toJson(role).as[Role]
      roundtripped.permissions must contain(Permission.DNA_PROFILE_CRUD)
      roundtripped.permissions must contain(Permission.USER_CRUD)
      roundtripped.permissions.size mustBe 2
    }
  }
