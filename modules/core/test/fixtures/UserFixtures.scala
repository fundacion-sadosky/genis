package fixtures

import security.LdapUser
import user.UserStatus

object UserFixtures:

  val ldapUser = LdapUser(
    userName = "testuser",
    firstName = "Test",
    lastName = "User",
    email = "test@example.com",
    roles = Seq("geneticist", "clerk"),
    geneMapperId = "TLAB",
    phone1 = "1234567890",
    phone2 = Some("0987654321"),
    status = UserStatus.active,
    encryptedPublicKey = Array.emptyByteArray,
    encryptedPrivateKey = Array.emptyByteArray,
    encryptrdTotpSecret = SecurityFixtures.totpSecret.getBytes("UTF-8"),
    superuser = false
  )

  val blockedLdapUser = ldapUser.copy(
    userName = "blockeduser",
    status = UserStatus.blocked
  )

  val pendingLdapUser = ldapUser.copy(
    userName = "pendinguser",
    status = UserStatus.pending
  )

  val user = security.User(
    id = "testuser",
    firstName = "Test",
    lastName = "User",
    email = "test@example.com",
    geneMapperId = "TLAB",
    roles = Seq("geneticist", "clerk"),
    permissions = SecurityFixtures.userPermissions,
    status = UserStatus.active,
    phone1 = "1234567890",
    phone2 = Some("0987654321"),
    superuser = false
  )
