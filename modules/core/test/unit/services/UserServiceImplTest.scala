package unit.services

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*

import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.DefaultMessagesApi

import fixtures.{SecurityFixtures, StubCacheService, UserFixtures}
import inbox.NoOpNotificationService
import security.{AuthenticatedPair, CryptoService, LdapUser, OTPService, RoleService, UserCredentials, UserRepository}
import services.{ClearPassRequestKey, FullUserKey, SignupRequestKey, UserServiceImpl}
import types.{Permission, TotpToken}
import user.*

class UserServiceImplTest extends AnyWordSpec with Matchers with MockitoSugar:

  given ExecutionContext = ExecutionContext.global
  private val timeout: Duration = Duration(5, "seconds")

  private val messagesApi = new DefaultMessagesApi(Map(
    "default" -> Map(
      "error.E0641" -> "E0641: No existe el usuario.",
      "error.E0646" -> "E0646: No se puede pasar el usuario del estado {0} al estado {1}.",
      "error.E0802" -> "E0802: La sesión es inválida o ha expirado.",
      "error.E0803" -> "E0803: Token inválido."
    )
  ))

  private def buildService(
      userRepository: UserRepository = mock[UserRepository],
      cacheService: services.CacheService = new StubCacheService,
      cryptoService: CryptoService = mock[CryptoService],
      otpService: OTPService = mock[OTPService],
      roleService: RoleService = mock[RoleService]
  ): UserServiceImpl =
    new UserServiceImpl(
      userRepository, cacheService, cryptoService, otpService,
      new NoOpNotificationService, roleService, messagesApi
    )

  // ── signupRequest ──────────────────────────────────────────────

  "UserServiceImpl.signupRequest" must {

    "generate unique username candidates excluding existing users" in {
      val userRepo = mock[UserRepository]
      when(userRepo.finfByUid(any[Seq[String]])).thenReturn(Future.successful(Seq.empty))

      val cryptoSvc = mock[CryptoService]
      when(cryptoSvc.giveTotpSecret).thenReturn("TOTP_SECRET")
      when(cryptoSvc.giveRsaKeys).thenReturn((Array.emptyByteArray, Array.emptyByteArray))

      val service = buildService(userRepository = userRepo, cryptoService = cryptoSvc)

      val solicitude = SignupSolicitude("user", "lastone", "enter", "a@g.cc", Seq("rol1"), "idgen", "14789652", None, false)
      val result = Await.result(service.signupRequest(solicitude), timeout)

      result.isRight mustBe true
      val response = result.toOption.get
      response.userNameCandidates.head mustBe "ulastone"
      response.totpSecret mustBe "TOTP_SECRET"
      response.signupRequestId must not be empty
    }

    "exclude already taken usernames from candidates" in {
      val userRepo = mock[UserRepository]
      val existingUser = UserFixtures.ldapUser.copy(userName = "ulastone")
      when(userRepo.finfByUid(any[Seq[String]])).thenReturn(Future.successful(Seq(existingUser)))

      val cryptoSvc = mock[CryptoService]
      when(cryptoSvc.giveTotpSecret).thenReturn("SECRET")
      when(cryptoSvc.giveRsaKeys).thenReturn((Array.emptyByteArray, Array.emptyByteArray))

      val service = buildService(userRepository = userRepo, cryptoService = cryptoSvc)

      val solicitude = SignupSolicitude("user", "lastone", "enter", "a@g.cc", Seq("rol1"), "idgen", "14789652", None, false)
      val result = Await.result(service.signupRequest(solicitude), timeout)

      val candidates = result.toOption.get.userNameCandidates
      candidates must not contain "ulastone"
      candidates.head mustBe "u.lastone"
    }
  }

  // ── clearPassRequest ───────────────────────────────────────────

  "UserServiceImpl.clearPassRequest" must {

    "return Right for user with pending_reset status" in {
      val userRepo = mock[UserRepository]
      when(userRepo.get("resetuser")).thenReturn(Future.successful(UserFixtures.pendingResetLdapUser))

      val cryptoSvc = mock[CryptoService]
      when(cryptoSvc.giveTotpSecret).thenReturn("TOTP_SECRET")
      when(cryptoSvc.giveRsaKeys).thenReturn((Array.emptyByteArray, Array.emptyByteArray))

      val service = buildService(userRepository = userRepo, cryptoService = cryptoSvc)

      val result = Await.result(service.clearPassRequest(ClearPassSolicitud("resetuser", "newpass")), timeout)

      result.isRight mustBe true
      result.toOption.get.totpSecret mustBe "TOTP_SECRET"
    }

    "return Left with E0641 for user not in pending_reset status" in {
      val userRepo = mock[UserRepository]
      when(userRepo.get("testuser")).thenReturn(Future.successful(UserFixtures.ldapUser))

      val cryptoSvc = mock[CryptoService]
      when(cryptoSvc.giveTotpSecret).thenReturn("S")
      when(cryptoSvc.giveRsaKeys).thenReturn((Array.emptyByteArray, Array.emptyByteArray))

      val service = buildService(userRepository = userRepo, cryptoService = cryptoSvc)

      val result = Await.result(service.clearPassRequest(ClearPassSolicitud("testuser", "newpass")), timeout)

      result.isLeft mustBe true
      result match
        case Left(error) => error must include("E0641")
        case _ => fail("Expected Left")
    }

    "return Left with E0641 for nonexistent user" in {
      val userRepo = mock[UserRepository]
      when(userRepo.get("nobody")).thenReturn(Future.failed(new RuntimeException("not found")))

      val cryptoSvc = mock[CryptoService]
      when(cryptoSvc.giveTotpSecret).thenReturn("S")
      when(cryptoSvc.giveRsaKeys).thenReturn((Array.emptyByteArray, Array.emptyByteArray))

      val service = buildService(userRepository = userRepo, cryptoService = cryptoSvc)

      val result = Await.result(service.clearPassRequest(ClearPassSolicitud("nobody", "newpass")), timeout)

      result.isLeft mustBe true
      result match
        case Left(error) => error must include("E0641")
        case _ => fail("Expected Left")
    }
  }

  // ── signupConfirmation ─────────────────────────────────────────

  "UserServiceImpl.signupConfirmation" must {

    "return Right when OTP is valid and user is created" in {
      val cache = new StubCacheService
      val userRepo = mock[UserRepository]
      val cryptoSvc = mock[CryptoService]
      val otpSvc = mock[OTPService]
      val roleSvc = mock[RoleService]

      val solicitude = SignupSolicitude("John", "Doe", "pass123", "john@example.com", Seq("admin"), "GM01", "1234", None, false)
      val credentials = SecurityFixtures.userCredentials
      val candidates = Seq("jdoe", "j.doe", "doej")
      cache.set(SignupRequestKey("test-token"), (solicitude, credentials, candidates))

      when(otpSvc.validate(any[TotpToken], any[String])).thenReturn(true)
      when(cryptoSvc.generateDerivatedCredentials("pass123")).thenReturn(SecurityFixtures.authPair)
      when(cryptoSvc.encrypt(any[Array[Byte]], any[AuthenticatedPair])).thenReturn(Array.emptyByteArray)
      when(userRepo.create(any[LdapUser], any[String])).thenReturn(Future.successful("jdoe"))
      when(roleSvc.getRolePermissions()).thenReturn(Map.empty[String, Set[Permission]].withDefaultValue(Set.empty))
      when(userRepo.findByRole(any[String])).thenReturn(Future.successful(Seq.empty))

      val service = buildService(
        userRepository = userRepo, cacheService = cache,
        cryptoService = cryptoSvc, otpService = otpSvc, roleService = roleSvc
      )

      val confirmation = SignupChallenge("test-token", 0, TotpToken("123456"))
      val result = Await.result(service.signupConfirmation(confirmation), timeout)

      result mustBe Right(0)
    }

    "return Left with E0803 when OTP is invalid" in {
      val cache = new StubCacheService
      val otpSvc = mock[OTPService]

      val solicitude = SignupSolicitude("John", "Doe", "pass123", "john@example.com", Seq("admin"), "GM01", "1234", None, false)
      cache.set(SignupRequestKey("test-token"), (solicitude, SecurityFixtures.userCredentials, Seq("jdoe")))
      when(otpSvc.validate(any[TotpToken], any[String])).thenReturn(false)

      val service = buildService(cacheService = cache, otpService = otpSvc)

      val result = Await.result(service.signupConfirmation(SignupChallenge("test-token", 0, TotpToken("000000"))), timeout)

      result.isLeft mustBe true
      result match
        case Left(error) => error must include("E0803")
        case _ => fail("Expected Left")
    }

    "return Left with E0802 when session has expired" in {
      val cache = new StubCacheService // empty — no entry

      val service = buildService(cacheService = cache)

      val result = Await.result(service.signupConfirmation(SignupChallenge("expired", 0, TotpToken("123456"))), timeout)

      result.isLeft mustBe true
      result match
        case Left(error) => error must include("E0802")
        case _ => fail("Expected Left")
    }
  }

  // ── clearPassConfirmation ──────────────────────────────────────

  "UserServiceImpl.clearPassConfirmation" must {

    "return Right when OTP is valid" in {
      val cache = new StubCacheService
      val userRepo = mock[UserRepository]
      val cryptoSvc = mock[CryptoService]
      val otpSvc = mock[OTPService]
      val roleSvc = mock[RoleService]

      val solicitud = ClearPassSolicitud("testuser", "newpass123")
      cache.set(ClearPassRequestKey("test-token"), (solicitud, SecurityFixtures.userCredentials, solicitud.userName))

      when(otpSvc.validate(any[TotpToken], any[String])).thenReturn(true)
      when(cryptoSvc.generateDerivatedCredentials("newpass123")).thenReturn(SecurityFixtures.authPair)
      when(cryptoSvc.encrypt(any[Array[Byte]], any[AuthenticatedPair])).thenReturn(Array.emptyByteArray)
      when(userRepo.clearPass(anyString(), any[UserStatus], anyString(), any[Array[Byte]])).thenReturn(Future.successful("testuser"))
      when(roleSvc.getRolePermissions()).thenReturn(Map.empty[String, Set[Permission]].withDefaultValue(Set.empty))
      when(userRepo.findByRole(any[String])).thenReturn(Future.successful(Seq.empty))

      val service = buildService(
        userRepository = userRepo, cacheService = cache,
        cryptoService = cryptoSvc, otpService = otpSvc, roleService = roleSvc
      )

      val result = Await.result(service.clearPassConfirmation(ClearPassChallenge("test-token", TotpToken("123456"))), timeout)

      result mustBe Right(0)
    }

    "return Left with E0803 when OTP is invalid" in {
      val cache = new StubCacheService
      val otpSvc = mock[OTPService]

      val solicitud = ClearPassSolicitud("testuser", "newpass123")
      cache.set(ClearPassRequestKey("test-token"), (solicitud, SecurityFixtures.userCredentials, solicitud.userName))
      when(otpSvc.validate(any[TotpToken], any[String])).thenReturn(false)

      val service = buildService(cacheService = cache, otpService = otpSvc)

      val result = Await.result(service.clearPassConfirmation(ClearPassChallenge("test-token", TotpToken("000000"))), timeout)

      result.isLeft mustBe true
      result match
        case Left(error) => error must include("E0803")
        case _ => fail("Expected Left")
    }

    "return Left with E0802 when session has expired" in {
      val cache = new StubCacheService

      val service = buildService(cacheService = cache)

      val result = Await.result(service.clearPassConfirmation(ClearPassChallenge("expired", TotpToken("123456"))), timeout)

      result.isLeft mustBe true
      result match
        case Left(error) => error must include("E0802")
        case _ => fail("Expected Left")
    }
  }

  // ── listAllUsers ───────────────────────────────────────────────

  "UserServiceImpl.listAllUsers" must {

    "return Seq[UserView] converted from LdapUsers" in {
      val userRepo = mock[UserRepository]
      when(userRepo.listAllUsers()).thenReturn(Future.successful(Seq(UserFixtures.ldapUser)))

      val service = buildService(userRepository = userRepo)

      val result = Await.result(service.listAllUsers(), timeout)

      result.length mustBe 1
      result.head.userName mustBe "testuser"
      result.head.status mustBe UserStatus.active
    }
  }

  // ── getUser ────────────────────────────────────────────────────

  "UserServiceImpl.getUser" must {

    "return UserView with correct fields" in {
      val userRepo = mock[UserRepository]
      when(userRepo.get("testuser")).thenReturn(Future.successful(UserFixtures.ldapUser))

      val service = buildService(userRepository = userRepo)

      val result = Await.result(service.getUser("testuser"), timeout)

      result.userName mustBe "testuser"
      result.firstName mustBe "Test"
      result.lastName mustBe "User"
      result.status mustBe UserStatus.active
    }
  }

  // ── getUserOrEmpty ─────────────────────────────────────────────

  "UserServiceImpl.getUserOrEmpty" must {

    "return Some(UserView) for existing user" in {
      val userRepo = mock[UserRepository]
      when(userRepo.getOrEmpty("testuser")).thenReturn(Future.successful(Some(UserFixtures.ldapUser)))

      val service = buildService(userRepository = userRepo)

      val result = Await.result(service.getUserOrEmpty("testuser"), timeout)

      result.isDefined mustBe true
      result.get.userName mustBe "testuser"
    }

    "return None for missing user" in {
      val userRepo = mock[UserRepository]
      when(userRepo.getOrEmpty("nobody")).thenReturn(Future.successful(None))

      val service = buildService(userRepository = userRepo)

      val result = Await.result(service.getUserOrEmpty("nobody"), timeout)

      result mustBe None
    }
  }

  // ── findByGeneMapper ───────────────────────────────────────────

  "UserServiceImpl.findByGeneMapper" must {

    "return Some(UserView) when found" in {
      val userRepo = mock[UserRepository]
      when(userRepo.findByGeneMapper("TLAB")).thenReturn(Future.successful(Some(UserFixtures.ldapUser)))

      val service = buildService(userRepository = userRepo)

      val result = Await.result(service.findByGeneMapper("TLAB"), timeout)

      result.isDefined mustBe true
      result.get.userName mustBe "testuser"
    }

    "return None when not found" in {
      val userRepo = mock[UserRepository]
      when(userRepo.findByGeneMapper("UNKNOWN")).thenReturn(Future.successful(None))

      val service = buildService(userRepository = userRepo)

      val result = Await.result(service.findByGeneMapper("UNKNOWN"), timeout)

      result mustBe None
    }
  }

  // ── updateUser ─────────────────────────────────────────────────

  "UserServiceImpl.updateUser" must {

    "delegate to repository and return result" in {
      val userRepo = mock[UserRepository]
      when(userRepo.updateUser(any[LdapUser])).thenReturn(Future.successful(true))

      val service = buildService(userRepository = userRepo)

      val userView = UserView("testuser", "Test", "User", "test@example.com", Seq("admin"), UserStatus.active, "TLAB", "1234", None, false)
      val result = Await.result(service.updateUser(userView), timeout)

      result mustBe true
    }
  }

  // ── setStatus ──────────────────────────────────────────────────

  "UserServiceImpl.setStatus" must {

    "allow valid transition from pending to active" in {
      val userRepo = mock[UserRepository]
      when(userRepo.get("pendinguser")).thenReturn(Future.successful(UserFixtures.pendingLdapUser))
      when(userRepo.setStatus("pendinguser", UserStatus.active)).thenReturn(Future.successful(()))

      val roleSvc = mock[RoleService]
      when(roleSvc.getRolePermissions()).thenReturn(Map.empty[String, Set[Permission]].withDefaultValue(Set.empty))

      val service = buildService(userRepository = userRepo, roleService = roleSvc)

      val result = Await.result(service.setStatus("pendinguser", UserStatus.active), timeout)

      result.isRight mustBe true
    }

    "allow transition from pending to blocked" in {
      val userRepo = mock[UserRepository]
      when(userRepo.get("pendinguser")).thenReturn(Future.successful(UserFixtures.pendingLdapUser))
      when(userRepo.setStatus("pendinguser", UserStatus.blocked)).thenReturn(Future.successful(()))

      val roleSvc = mock[RoleService]
      when(roleSvc.getRolePermissions()).thenReturn(Map.empty[String, Set[Permission]].withDefaultValue(Set.empty))

      val service = buildService(userRepository = userRepo, roleService = roleSvc)

      val result = Await.result(service.setStatus("pendinguser", UserStatus.blocked), timeout)

      result.isRight mustBe true
    }

    "reject invalid transition from active to pending" in {
      val userRepo = mock[UserRepository]
      when(userRepo.get("testuser")).thenReturn(Future.successful(UserFixtures.ldapUser))

      val service = buildService(userRepository = userRepo)

      val result = Await.result(service.setStatus("testuser", UserStatus.pending), timeout)

      result.isLeft mustBe true
      result match
        case Left(error) => error must include("E0646")
        case _ => fail("Expected Left")
    }

    "allow transition from pending_reset to pending" in {
      val userRepo = mock[UserRepository]
      when(userRepo.get("resetuser")).thenReturn(Future.successful(UserFixtures.pendingResetLdapUser))
      when(userRepo.setStatus("resetuser", UserStatus.pending)).thenReturn(Future.successful(()))

      val roleSvc = mock[RoleService]
      when(roleSvc.getRolePermissions()).thenReturn(Map.empty[String, Set[Permission]].withDefaultValue(Set.empty))

      val service = buildService(userRepository = userRepo, roleService = roleSvc)

      val result = Await.result(service.setStatus("resetuser", UserStatus.pending), timeout)

      result.isRight mustBe true
    }
  }

  // ── findByStatus ───────────────────────────────────────────────

  "UserServiceImpl.findByStatus" must {

    "return matching users" in {
      val userRepo = mock[UserRepository]
      when(userRepo.findByStatus(UserStatus.active)).thenReturn(Future.successful(Seq(UserFixtures.ldapUser)))

      val service = buildService(userRepository = userRepo)

      val result = Await.result(service.findByStatus(UserStatus.active), timeout)

      result.size mustBe 1
      result.head.status mustBe UserStatus.active
    }
  }

  // ── findUserAssignable ─────────────────────────────────────────

  "UserServiceImpl.findUserAssignable" must {

    "return users from roles with DNA_PROFILE_CRUD and MATCHES_MANAGER" in {
      val userRepo = mock[UserRepository]
      val roleSvc = mock[RoleService]

      val rolePermissions = Map(
        "geneticist" -> Set(Permission.DNA_PROFILE_CRUD, Permission.MATCHES_MANAGER),
        "clerk" -> Set(Permission.PROFILE_DATA_CRUD)
      ).withDefaultValue(Set.empty[Permission])

      when(roleSvc.getRolePermissions()).thenReturn(rolePermissions)
      when(userRepo.findByRole("geneticist")).thenReturn(Future.successful(Seq(UserFixtures.ldapUser)))

      val service = buildService(userRepository = userRepo, roleService = roleSvc)

      val result = Await.result(service.findUserAssignable, timeout)

      result must not be empty
      result.head.id mustBe "testuser"
    }
  }

  // ── findUserAssignableByRole ───────────────────────────────────

  "UserServiceImpl.findUserAssignableByRole" must {

    "return only assignable users (active or blocked)" in {
      val userRepo = mock[UserRepository]
      val roleSvc = mock[RoleService]

      when(roleSvc.getRolePermissions()).thenReturn(SecurityFixtures.rolePermissionMap)
      when(userRepo.findByRole("geneticist")).thenReturn(
        Future.successful(Seq(
          UserFixtures.ldapUser,        // active → assignable
          UserFixtures.blockedLdapUser,  // blocked → assignable
          UserFixtures.pendingLdapUser   // pending → NOT assignable
        ))
      )

      val service = buildService(userRepository = userRepo, roleService = roleSvc)

      val result = Await.result(service.findUserAssignableByRole("geneticist"), timeout)

      result.map(_.id) must contain("testuser")
      result.map(_.id) must contain("blockeduser")
      result.map(_.id) must not contain "pendinguser"
    }
  }

  // ── findUsersIdWithPermission ──────────────────────────────────

  "UserServiceImpl.findUsersIdWithPermission" must {

    "return user IDs matching the permission" in {
      val userRepo = mock[UserRepository]
      val roleSvc = mock[RoleService]

      val rolePermissions = Map(
        "geneticist" -> Set(Permission.DNA_PROFILE_CRUD, Permission.MATCHES_MANAGER)
      ).withDefaultValue(Set.empty[Permission])

      when(roleSvc.getRolePermissions()).thenReturn(rolePermissions)
      when(userRepo.findByRole("geneticist")).thenReturn(Future.successful(Seq(UserFixtures.ldapUser)))

      val service = buildService(userRepository = userRepo, roleService = roleSvc)

      val result = Await.result(service.findUsersIdWithPermission(Permission.DNA_PROFILE_CRUD), timeout)

      result must contain("testuser")
    }
  }

  // ── isSuperUser ────────────────────────────────────────────────

  "UserServiceImpl.isSuperUser" must {

    "return true for superuser" in {
      val userRepo = mock[UserRepository]
      when(userRepo.get("superuser")).thenReturn(Future.successful(UserFixtures.superLdapUser))

      val service = buildService(userRepository = userRepo)

      Await.result(service.isSuperUser("superuser"), timeout) mustBe true
    }

    "return false for non-superuser" in {
      val userRepo = mock[UserRepository]
      when(userRepo.get("testuser")).thenReturn(Future.successful(UserFixtures.ldapUser))

      val service = buildService(userRepository = userRepo)

      Await.result(service.isSuperUser("testuser"), timeout) mustBe false
    }
  }
