package unit.types

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, JsNumber, JsString, JsSuccess, Json}
import types.Permission

class PermissionTest extends AnyWordSpec with Matchers:

  "Permission.list" must {
    "contain exactly 33 permissions" in {
      Permission.list.size mustBe 33
    }

    "include key permissions" in {
      Permission.list must contain(Permission.DNA_PROFILE_CRUD)
      Permission.list must contain(Permission.LOGIN_SIGNUP)
      Permission.list must contain(Permission.REPORTING_VIEW)
      Permission.list must contain(Permission.MATCHES_MANAGER)
      Permission.list must contain(Permission.PEDIGREE_CRUD)
      Permission.list must contain(Permission.USER_CRUD)
    }
  }

  "Permission.fromString" must {
    "return Some for a valid permission name" in {
      Permission.fromString("DNA_PROFILE_CRUD") mustBe Some(Permission.DNA_PROFILE_CRUD)
    }

    "return Some for LOGIN_SIGNUP" in {
      Permission.fromString("LOGIN_SIGNUP") mustBe Some(Permission.LOGIN_SIGNUP)
    }

    "return None for an unknown permission name" in {
      Permission.fromString("NONEXISTENT") mustBe None
    }

    "return None for empty string" in {
      Permission.fromString("") mustBe None
    }
  }

  "Permission JSON serialization" must {
    "write a permission as JsString of its name" in {
      Json.toJson(Permission.DNA_PROFILE_CRUD: Permission) mustBe JsString("DNA_PROFILE_CRUD")
    }

    "read a valid permission name from JsString" in {
      JsString("DNA_PROFILE_CRUD").validate[Permission] mustBe JsSuccess(Permission.DNA_PROFILE_CRUD)
    }

    "return JsError for unknown permission name" in {
      JsString("NONEXISTENT").validate[Permission] mustBe a[JsError]
    }

    "return JsError for non-string JSON" in {
      JsNumber(42).validate[Permission] mustBe a[JsError]
    }
  }

  "Permission operations" must {
    "DNA_PROFILE_CRUD must have a non-empty set of operations" in {
      Permission.DNA_PROFILE_CRUD.operations must not be empty
    }

    "ADD_MANUAL_LOCUS must have an empty set of operations" in {
      Permission.ADD_MANUAL_LOCUS.operations mustBe empty
    }

    "INTERCON_NOTIF must have an empty set of operations" in {
      Permission.INTERCON_NOTIF.operations mustBe empty
    }

    "LABORATORY_CRUD must contain a sensitive operation" in {
      Permission.LABORATORY_CRUD.operations.exists(_.isSensitive) mustBe true
    }

    "PROFILE_DATA_CRUD must contain a sensitive operation" in {
      Permission.PROFILE_DATA_CRUD.operations.exists(_.isSensitive) mustBe true
    }
  }
