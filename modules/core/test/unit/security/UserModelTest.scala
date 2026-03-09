package unit.security

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import fixtures.{SecurityFixtures, UserFixtures}
import security.{LdapUser, User}
import types.Permission
import user.UserStatus

class UserModelTest extends AnyWordSpec with Matchers {

  // ── LdapUser.toUser ───────────────────────────────────────────────

  "LdapUser.toUser" must {

    "convert LdapUser to User with permissions resolved from rolePermissionMap" in {
      val ldapUser = UserFixtures.ldapUser // roles = Seq("geneticist", "clerk")
      val roleMap = SecurityFixtures.rolePermissionMap

      val user = LdapUser.toUser(ldapUser, roleMap)

      user.id mustBe "testuser"
      user.firstName mustBe "Test"
      user.lastName mustBe "User"
      user.email mustBe "test@example.com"
      user.geneMapperId mustBe "TLAB"
      user.roles mustBe Seq("geneticist", "clerk")
      user.status mustBe UserStatus.active
      user.phone1 mustBe "1234567890"
      user.phone2 mustBe Some("0987654321")
      user.superuser mustBe false
      // Permissions are the union of geneticist + clerk permissions
      user.permissions mustBe SecurityFixtures.userPermissions
    }

    "return empty permissions when roles have no match in the map" in {
      val ldapUser = UserFixtures.ldapUser.copy(roles = Seq("unknown_role"))
      val roleMap = SecurityFixtures.rolePermissionMap

      val user = LdapUser.toUser(ldapUser, roleMap)

      user.permissions mustBe empty
    }

    "return empty permissions when user has no roles" in {
      val ldapUser = UserFixtures.ldapUser.copy(roles = Seq.empty)

      val user = LdapUser.toUser(ldapUser, SecurityFixtures.rolePermissionMap)

      user.permissions mustBe empty
    }
  }

  // ── User.canLogin ─────────────────────────────────────────────────

  "User.canLogin" must {

    "return true for active status" in {
      val user = UserFixtures.user // status = active
      user.canLogin mustBe true
    }

    "return false for blocked status" in {
      val user = UserFixtures.user.copy(status = UserStatus.blocked)
      user.canLogin mustBe false
    }

    "return false for pending status" in {
      val user = UserFixtures.user.copy(status = UserStatus.pending)
      user.canLogin mustBe false
    }
  }

  // ── User.fullName ─────────────────────────────────────────────────

  "User.fullName" must {

    "format as 'lastName firstName'" in {
      val user = UserFixtures.user
      user.fullName mustBe "User Test"
    }
  }

  // ── LdapUser.fullName ─────────────────────────────────────────────

  "LdapUser.fullName" must {

    "format as 'lastName firstName'" in {
      val ldapUser = UserFixtures.ldapUser
      ldapUser.fullName mustBe "User Test"
    }
  }
}
