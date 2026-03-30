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

  // ── User.isAsignable ─────────────────────────────────────────────

  "User.isAsignable" must {

    "return true for active status" in {
      UserFixtures.user.copy(status = UserStatus.active).isAsignable mustBe true
    }

    "return true for blocked status" in {
      UserFixtures.user.copy(status = UserStatus.blocked).isAsignable mustBe true
    }

    "return false for pending status" in {
      UserFixtures.user.copy(status = UserStatus.pending).isAsignable mustBe false
    }

    "return false for inactive status" in {
      UserFixtures.user.copy(status = UserStatus.inactive).isAsignable mustBe false
    }

    "return false for pending_reset status" in {
      UserFixtures.user.copy(status = UserStatus.pending_reset).isAsignable mustBe false
    }
  }

  // ── LdapUser.toUserView ─────────────────────────────────────────

  "LdapUser.toUserView" must {

    "map all fields correctly" in {
      val ldapUser = UserFixtures.ldapUser
      val view = LdapUser.toUserView(ldapUser)

      view.userName mustBe "testuser"
      view.firstName mustBe "Test"
      view.lastName mustBe "User"
      view.email mustBe "test@example.com"
      view.roles mustBe Seq("geneticist", "clerk")
      view.status mustBe UserStatus.active
      view.geneMapperId mustBe "TLAB"
      view.phone1 mustBe "1234567890"
      view.phone2 mustBe Some("0987654321")
      view.superuser mustBe false
    }
  }

  // ── LdapUser.fromUserView ───────────────────────────────────────

  "LdapUser.fromUserView" must {

    "map fields and use null for byte arrays" in {
      import user.UserView
      val view = UserView("testuser", "Test", "User", "test@example.com",
        Seq("geneticist"), UserStatus.active, "TLAB", "1234", Some("5678"), false)

      val ldapUser = LdapUser.fromUserView(view)

      ldapUser.userName mustBe "testuser"
      ldapUser.firstName mustBe "Test"
      ldapUser.lastName mustBe "User"
      ldapUser.email mustBe "test@example.com"
      ldapUser.roles mustBe Seq("geneticist")
      ldapUser.status mustBe UserStatus.active
      ldapUser.geneMapperId mustBe "TLAB"
      ldapUser.phone1 mustBe "1234"
      ldapUser.phone2 mustBe Some("5678")
      ldapUser.superuser mustBe false
      ldapUser.encryptedPublicKey mustBe null
      ldapUser.encryptedPrivateKey mustBe null
      ldapUser.encryptrdTotpSecret mustBe null
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
