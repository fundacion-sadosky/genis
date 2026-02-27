package fixtures

import security.*
import types.Permission
import user.{Role, UserStatus}

object SecurityFixtures:

  // Hex-encoded keys (32 hex chars = 16 bytes each for AES-128)
  val verifierAp = "d59b81ea658b20e4d3a1712b75bb21d5"
  val keyAp = "9e3ba370183beccf7df540ca812b3985"
  val ivAp = "e932d6e8f6920f1efa4103226391a570"

  val authPair = AuthenticatedPair(verifierAp, keyAp, ivAp)

  val totpSecret = "CRII5DCIVF4WPP2R"
  val totpToken = types.TotpToken("123456")

  val requestToken = RequestToken("test-request-token-123")

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
    encryptrdTotpSecret = totpSecret.getBytes("UTF-8"),
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

  val rolePermissionMap: Map[String, Set[Permission]] = Map(
    "geneticist" -> Set(Permission.DNA_PROFILE_CRUD, Permission.MATCHES_MANAGER),
    "clerk" -> Set(Permission.PROFILE_DATA_CRUD, Permission.USER_CRUD)
  ).withDefaultValue(Set.empty)

  val userPermissions: Set[Permission] = Set(
    Permission.DNA_PROFILE_CRUD,
    Permission.MATCHES_MANAGER,
    Permission.PROFILE_DATA_CRUD,
    Permission.USER_CRUD
  )

  val user = User(
    id = "testuser",
    firstName = "Test",
    lastName = "User",
    email = "test@example.com",
    geneMapperId = "TLAB",
    roles = Seq("geneticist", "clerk"),
    permissions = userPermissions,
    status = UserStatus.active,
    phone1 = "1234567890",
    phone2 = Some("0987654321"),
    superuser = false
  )

  val userCredentials = UserCredentials(
    publicKey = Array.emptyByteArray,
    privateKey = Array.emptyByteArray,
    totpSecret = totpSecret
  )

  val fullUser = FullUser(user, userCredentials, authPair)

  val geneticistRole = Role("geneticist", "Geneticist", Set(Permission.DNA_PROFILE_CRUD, Permission.MATCHES_MANAGER))
  val clerkRole = Role("clerk", "Clerk", Set(Permission.PROFILE_DATA_CRUD, Permission.USER_CRUD))
