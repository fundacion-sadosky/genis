package fixtures

import security.*
import types.Permission
import user.Role

object SecurityFixtures:

  // Hex-encoded keys (32 hex chars = 16 bytes each for AES-128)
  val verifierAp = "d59b81ea658b20e4d3a1712b75bb21d5"
  val keyAp = "9e3ba370183beccf7df540ca812b3985"
  val ivAp = "e932d6e8f6920f1efa4103226391a570"

  val authPair = AuthenticatedPair(verifierAp, keyAp, ivAp)

  val totpSecret = "CRII5DCIVF4WPP2R"
  val totpToken = types.TotpToken("123456")

  val requestToken = RequestToken("test-request-token-123")

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

  val userCredentials = UserCredentials(
    publicKey = Array.emptyByteArray,
    privateKey = Array.emptyByteArray,
    totpSecret = totpSecret
  )

  val fullUser = FullUser(UserFixtures.user, userCredentials, authPair)

  val geneticistRole = Role("geneticist", "Geneticist", Set(Permission.DNA_PROFILE_CRUD, Permission.MATCHES_MANAGER))
  val clerkRole = Role("clerk", "Clerk", Set(Permission.PROFILE_DATA_CRUD, Permission.USER_CRUD))
