package unit.security

import java.util.Base64

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*
import scala.util.{Failure, Success}

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest

import fixtures.{SecurityFixtures, UserFixtures}
import security.*
import services.*
import types.{Permission, TotpToken}
import user.UserStatus

class AuthServiceImplTest extends AnyWordSpec with Matchers with MockitoSugar {

  given ec: ExecutionContext = ExecutionContext.global
  val timeout: Duration = Duration(5, "seconds")

  // Shared crypto service (real, for encrypt/decrypt roundtrips)
  val crypto = new CryptoServiceImpl(keyLength = 2048)

  /** Helper: builds an AuthServiceImpl with given mocks/stubs */
  private def buildService(
      cache: CacheService = new fixtures.StubCacheService,
      userRepository: UserRepository = mock[UserRepository],
      otpService: OTPService = mock[OTPService],
      cryptoService: CryptoService = crypto,
      tokenExpTime: Int = 3,
      credentialsExpTime: Int = 3,
      roleService: RoleService = mock[RoleService],
      inferiorInstanceRepo: InferiorInstanceRepository = new InferiorInstanceRepositoryStub(),
      connectionRepo: ConnectionRepository = new ConnectionRepositoryStub()
  ): AuthServiceImpl =
    new AuthServiceImpl(
      cache, userRepository, otpService, cryptoService,
      tokenExpTime, credentialsExpTime, roleService,
      inferiorInstanceRepo, connectionRepo
    )

  // ── authenticate ──────────────────────────────────────────────────

  "AuthServiceImpl.authenticate" must {

    "return Some(FullUser) when bind succeeds, user is active, and OTP is valid" in {
      val cache = new fixtures.StubCacheService
      val userRepo = mock[UserRepository]
      when(userRepo.bind("testuser", "pass")).thenReturn(Future.successful(true))
      when(userRepo.get("testuser")).thenReturn(Future.successful(UserFixtures.ldapUser))

      val cryptoSvc = mock[CryptoService]
      when(cryptoSvc.generateRandomCredentials()).thenReturn(SecurityFixtures.authPair)
      when(cryptoSvc.generateDerivatedCredentials("pass")).thenReturn(SecurityFixtures.authPair)
      when(cryptoSvc.decrypt(any[Array[Byte]], any[AuthenticatedPair]))
        .thenAnswer((invocation: org.mockito.invocation.InvocationOnMock) =>
          invocation.getArguments()(0).asInstanceOf[Array[Byte]]
        )

      val otpSvc = mock[OTPService]
      when(otpSvc.validate(any[TotpToken], any[String])).thenReturn(true)

      val roleSvc = mock[RoleService]
      when(roleSvc.getRolePermissions()).thenReturn(SecurityFixtures.rolePermissionMap)

      val service = buildService(
        cache = cache, userRepository = userRepo,
        otpService = otpSvc, cryptoService = cryptoSvc, roleService = roleSvc
      )

      val result = Await.result(service.authenticate("testuser", "pass", SecurityFixtures.totpToken), timeout)

      result must not be None
      result.get.userDetail.id mustBe "testuser"
    }

    "return None when bind fails" in {
      val userRepo = mock[UserRepository]
      when(userRepo.bind("testuser", "wrong")).thenReturn(Future.successful(false))

      val service = buildService(userRepository = userRepo)

      val result = Await.result(service.authenticate("testuser", "wrong", SecurityFixtures.totpToken), timeout)
      result mustBe None
    }

    "return None when user status is blocked" in {
      val cache = new fixtures.StubCacheService
      val userRepo = mock[UserRepository]
      when(userRepo.bind("blockeduser", "pass")).thenReturn(Future.successful(true))
      when(userRepo.get("blockeduser")).thenReturn(Future.successful(UserFixtures.blockedLdapUser))

      val cryptoSvc = mock[CryptoService]
      when(cryptoSvc.generateRandomCredentials()).thenReturn(SecurityFixtures.authPair)
      when(cryptoSvc.generateDerivatedCredentials("pass")).thenReturn(SecurityFixtures.authPair)
      when(cryptoSvc.decrypt(any[Array[Byte]], any[AuthenticatedPair]))
        .thenAnswer((inv: org.mockito.invocation.InvocationOnMock) =>
          inv.getArguments()(0).asInstanceOf[Array[Byte]]
        )

      val roleSvc = mock[RoleService]
      when(roleSvc.getRolePermissions()).thenReturn(SecurityFixtures.rolePermissionMap)

      val service = buildService(
        cache = cache, userRepository = userRepo,
        cryptoService = cryptoSvc, roleService = roleSvc
      )

      val result = Await.result(service.authenticate("blockeduser", "pass", SecurityFixtures.totpToken), timeout)
      result mustBe None
    }

    "return None when user status is pending" in {
      val cache = new fixtures.StubCacheService
      val userRepo = mock[UserRepository]
      when(userRepo.bind("pendinguser", "pass")).thenReturn(Future.successful(true))
      when(userRepo.get("pendinguser")).thenReturn(Future.successful(UserFixtures.pendingLdapUser))

      val cryptoSvc = mock[CryptoService]
      when(cryptoSvc.generateRandomCredentials()).thenReturn(SecurityFixtures.authPair)
      when(cryptoSvc.generateDerivatedCredentials("pass")).thenReturn(SecurityFixtures.authPair)
      when(cryptoSvc.decrypt(any[Array[Byte]], any[AuthenticatedPair]))
        .thenAnswer((inv: org.mockito.invocation.InvocationOnMock) =>
          inv.getArguments()(0).asInstanceOf[Array[Byte]]
        )

      val roleSvc = mock[RoleService]
      when(roleSvc.getRolePermissions()).thenReturn(SecurityFixtures.rolePermissionMap)

      val service = buildService(
        cache = cache, userRepository = userRepo,
        cryptoService = cryptoSvc, roleService = roleSvc
      )

      val result = Await.result(service.authenticate("pendinguser", "pass", SecurityFixtures.totpToken), timeout)
      result mustBe None
    }

    "return None when OTP is invalid" in {
      val cache = new fixtures.StubCacheService
      val userRepo = mock[UserRepository]
      when(userRepo.bind("testuser", "pass")).thenReturn(Future.successful(true))
      when(userRepo.get("testuser")).thenReturn(Future.successful(UserFixtures.ldapUser))

      val cryptoSvc = mock[CryptoService]
      when(cryptoSvc.generateRandomCredentials()).thenReturn(SecurityFixtures.authPair)
      when(cryptoSvc.generateDerivatedCredentials("pass")).thenReturn(SecurityFixtures.authPair)
      when(cryptoSvc.decrypt(any[Array[Byte]], any[AuthenticatedPair]))
        .thenAnswer((inv: org.mockito.invocation.InvocationOnMock) =>
          inv.getArguments()(0).asInstanceOf[Array[Byte]]
        )

      val otpSvc = mock[OTPService]
      when(otpSvc.validate(any[TotpToken], any[String])).thenReturn(false)

      val roleSvc = mock[RoleService]
      when(roleSvc.getRolePermissions()).thenReturn(SecurityFixtures.rolePermissionMap)

      val service = buildService(
        cache = cache, userRepository = userRepo,
        otpService = otpSvc, cryptoService = cryptoSvc, roleService = roleSvc
      )

      val result = Await.result(service.authenticate("testuser", "pass", SecurityFixtures.totpToken), timeout)
      result mustBe None
    }

    "return None when OTP has already been used (replay protection)" in {
      val cache = new fixtures.StubCacheService
      // Pre-populate cache with the same OTP token as "already used"
      cache.set(LoggedUserKey("testuser"), SecurityFixtures.totpToken)

      val userRepo = mock[UserRepository]
      when(userRepo.bind("testuser", "pass")).thenReturn(Future.successful(true))
      when(userRepo.get("testuser")).thenReturn(Future.successful(UserFixtures.ldapUser))

      val cryptoSvc = mock[CryptoService]
      when(cryptoSvc.generateRandomCredentials()).thenReturn(SecurityFixtures.authPair)
      when(cryptoSvc.generateDerivatedCredentials("pass")).thenReturn(SecurityFixtures.authPair)
      when(cryptoSvc.decrypt(any[Array[Byte]], any[AuthenticatedPair]))
        .thenAnswer((inv: org.mockito.invocation.InvocationOnMock) =>
          inv.getArguments()(0).asInstanceOf[Array[Byte]]
        )

      val otpSvc = mock[OTPService]
      when(otpSvc.validate(any[TotpToken], any[String])).thenReturn(true)

      val roleSvc = mock[RoleService]
      when(roleSvc.getRolePermissions()).thenReturn(SecurityFixtures.rolePermissionMap)

      val service = buildService(
        cache = cache, userRepository = userRepo,
        otpService = otpSvc, cryptoService = cryptoSvc, roleService = roleSvc
      )

      val result = Await.result(service.authenticate("testuser", "pass", SecurityFixtures.totpToken), timeout)
      result mustBe None
    }
  }

  // ── verifyAndDecryptRequest ───────────────────────────────────────

  "AuthServiceImpl.verifyAndDecryptRequest" must {

    "return Success for public resources without decryption" in {
      val service = buildService()

      service.verifyAndDecryptRequest("/login", "GET", None, None) mustBe Success("/login")
      service.verifyAndDecryptRequest("/status", "GET", None, None) mustBe Success("/status")
      service.verifyAndDecryptRequest("/assets/js/app.js", "GET", None, None) mustBe Success("/assets/js/app.js")
    }

    "return Success for import URIs without decryption" in {
      val service = buildService()

      service.verifyAndDecryptRequest("/categories/import", "POST", None, None) mustBe Success("/categories/import")
      service.verifyAndDecryptRequest("/strkit/import", "POST", None, None) mustBe Success("/strkit/import")
      service.verifyAndDecryptRequest("/locus/import", "POST", None, None) mustBe Success("/locus/import")
      service.verifyAndDecryptRequest("/roles/import", "POST", None, None) mustBe Success("/roles/import")
    }

    "return Failure when X-USER header is missing for non-public resource" in {
      val service = buildService()

      val result = service.verifyAndDecryptRequest("/some/protected", "GET", None, None)
      result.isFailure mustBe true
      result.failed.get mustBe a[IllegalAccessException]
    }

    "return Failure when user is not authenticated in cache" in {
      val cache = new fixtures.StubCacheService // empty cache
      val service = buildService(cache = cache)

      val result = service.verifyAndDecryptRequest("/some/protected", "GET", Some("unknownuser"), None)
      result.isFailure mustBe true
      result.failed.get mustBe a[java.util.NoSuchElementException]
    }

    "decrypt URI and return Success when user is authorized" in {
      val cache = new fixtures.StubCacheService

      // Create a fullUser with a permission that allows GET on /populationBaseFreq
      val roleSvc = mock[RoleService]
      when(roleSvc.getRolePermissions()).thenReturn(
        Map("geneticist" -> Set[Permission](Permission.ALLELIC_FREQ_DB_CRUD)).withDefaultValue(Set.empty)
      )

      val fullUser = SecurityFixtures.fullUser.copy(
        userDetail = UserFixtures.user.copy(roles = Seq("geneticist")),
        credentials = crypto.generateRandomCredentials()
      )
      cache.set(FullUserKey("testuser", 3), fullUser)

      val service = buildService(cache = cache, roleService = roleSvc)

      // Encrypt a URI using the user's credentials
      val plainUri = "/populationBaseFreq"
      val encryptedBytes = crypto.encrypt(plainUri.getBytes("UTF-8"), fullUser.credentials)
      val encryptedUri = "/" + Base64.getEncoder.encodeToString(encryptedBytes)

      val result = service.verifyAndDecryptRequest(encryptedUri, "GET", Some("testuser"), Some(SecurityFixtures.totpToken))
      result mustBe Success("/populationBaseFreq")
    }

    "return Failure when user lacks permissions for the resource" in {
      val cache = new fixtures.StubCacheService

      // Role with no permissions
      val roleSvc = mock[RoleService]
      when(roleSvc.getRolePermissions()).thenReturn(Map.empty[String, Set[Permission]].withDefaultValue(Set.empty))

      val fullUser = SecurityFixtures.fullUser.copy(
        userDetail = UserFixtures.user.copy(roles = Seq("norole")),
        credentials = crypto.generateRandomCredentials()
      )
      cache.set(FullUserKey("testuser", 3), fullUser)

      val service = buildService(cache = cache, roleService = roleSvc)

      val plainUri = "/populationBaseFreq"
      val encryptedBytes = crypto.encrypt(plainUri.getBytes("UTF-8"), fullUser.credentials)
      val encryptedUri = "/" + Base64.getEncoder.encodeToString(encryptedBytes)

      val result = service.verifyAndDecryptRequest(encryptedUri, "GET", Some("testuser"), None)
      result.isFailure mustBe true
      result.failed.get mustBe a[IllegalAccessException]
    }
  }

  // ── isPublicResource ──────────────────────────────────────────────

  "AuthServiceImpl.isPublicResource" must {

    "return true for known public URIs" in {
      val service = buildService()

      service.isPublicResource("/") mustBe true
      service.isPublicResource("/login") mustBe true
      service.isPublicResource("/status") mustBe true
      service.isPublicResource("/favicon.ico") mustBe true
      service.isPublicResource("/assets/js/app.js") mustBe true
      service.isPublicResource("/signup") mustBe true
      service.isPublicResource("/signup/step2") mustBe true
      service.isPublicResource("/clear-password/reset") mustBe true
      service.isPublicResource("/disclaimer") mustBe true
      service.isPublicResource("/rolesForSU") mustBe true
      service.isPublicResource("/jsroutes.js") mustBe true
      service.isPublicResource("/sensitiveOper") mustBe true
      service.isPublicResource("/appConf") mustBe true
      service.isPublicResource("/resources/temporary/file.pdf") mustBe true
      service.isPublicResource("/resources/static/logo.png") mustBe true
      service.isPublicResource("/resources/proto/static/x") mustBe true
      service.isPublicResource("/profiles-uploader/batch") mustBe true
      service.isPublicResource("/profiles/ABC123/epgs") mustBe true
      service.isPublicResource("/profiles/ABC123/file") mustBe true
      service.isPublicResource("/superior/category-tree-combo") mustBe true
      service.isPublicResource("/superior/connection") mustBe true
      service.isPublicResource("/superior/profile/123") mustBe true
      service.isPublicResource("/inferior/match/abc") mustBe true
      service.isPublicResource("/inferior/profile/status") mustBe true
      service.isPublicResource("/interconection/match/status") mustBe true
      service.isPublicResource("/interconnection/file") mustBe true
    }

    "return false for non-public URIs" in {
      val service = buildService()

      service.isPublicResource("/users") mustBe false
      service.isPublicResource("/profiles") mustBe false
      service.isPublicResource("/laboratory") mustBe false
      service.isPublicResource("/geneticist") mustBe false
      service.isPublicResource("/populationBaseFreq") mustBe false
    }
  }

  // ── isBlockeableBySuperiorInstance ─────────────────────────────────

  "AuthServiceImpl.isBlockeableBySuperiorInstance" must {

    "return true for blockeable URIs" in {
      val service = buildService()

      service.isBlockeableBySuperiorInstance("/superior/profile") mustBe true
      service.isBlockeableBySuperiorInstance("/superior/profile/123") mustBe true
      service.isBlockeableBySuperiorInstance("/interconnection/file") mustBe true
      service.isBlockeableBySuperiorInstance("/interconection/match/status") mustBe true
      service.isBlockeableBySuperiorInstance("/inferior/match/") mustBe true
    }

    "return false for non-blockeable URIs" in {
      val service = buildService()

      service.isBlockeableBySuperiorInstance("/login") mustBe false
      service.isBlockeableBySuperiorInstance("/status") mustBe false
      service.isBlockeableBySuperiorInstance("/users") mustBe false
    }
  }

  // ── isInterconnectionResource ─────────────────────────────────────

  "AuthServiceImpl.isInterconnectionResource" must {

    "return true for interconnection URIs" in {
      val service = buildService()

      service.isInterconnectionResource("/status") mustBe true
      service.isInterconnectionResource("/superior/category-tree-combo") mustBe true
      service.isInterconnectionResource("/superior/connection") mustBe true
      service.isInterconnectionResource("/superior/profile") mustBe true
      service.isInterconnectionResource("/interconnection/file") mustBe true
    }

    "return false for non-interconnection URIs" in {
      val service = buildService()

      service.isInterconnectionResource("/login") mustBe false
      service.isInterconnectionResource("/users") mustBe false
    }
  }

  // ── getSensitiveOperations ────────────────────────────────────────

  "AuthServiceImpl.getSensitiveOperations" must {

    "return only operations marked as sensitive" in {
      val service = buildService()

      val sensitiveOps = service.getSensitiveOperations()
      sensitiveOps must not be empty
      // All returned operations should come from isSensitive=true StaticAuthorisationOperations
      // Verify at least one known sensitive operation exists (LABORATORY_CRUD has POST as sensitive)
      sensitiveOps.exists(op => op.resource.contains("laboratory") && op.action.contains("POST")) mustBe true
    }
  }

  // ── getCredentials ────────────────────────────────────────────────

  "AuthServiceImpl.getCredentials" must {

    "return Some(AuthenticatedPair) when user is in cache" in {
      val cache = new fixtures.StubCacheService
      cache.set(FullUserKey("testuser", 3), SecurityFixtures.fullUser)

      val service = buildService(cache = cache)

      val result = service.getCredentials("testuser")
      result mustBe Some(SecurityFixtures.authPair)
    }

    "return None when user is not in cache" in {
      val cache = new fixtures.StubCacheService

      val service = buildService(cache = cache)

      val result = service.getCredentials("unknown")
      result mustBe None
    }
  }

  // ── verifyInferiorInstance ────────────────────────────────────────

  "AuthServiceImpl.verifyInferiorInstance" must {

    "return Success for non-blockeable URIs" in {
      val service = buildService()

      val request = FakeRequest("GET", "/some/other/path")
      Await.result(service.verifyInferiorInstance(request), timeout) mustBe Success("/some/other/path")
    }

    "return Success for blockeable URI when inferior instance is enabled" in {
      val infRepo = mock[InferiorInstanceRepository]
      when(infRepo.isInferiorInstanceEnabled("pdg-devclient")).thenReturn(Future.successful(true))

      val service = buildService(inferiorInstanceRepo = infRepo)

      val request = FakeRequest("GET", "/superior/profile")
        .withHeaders("X-URL-INSTANCIA-INFERIOR" -> "pdg-devclient")

      Await.result(service.verifyInferiorInstance(request), timeout) mustBe Success("/superior/profile")
    }

    "return Failure for blockeable URI when inferior instance is disabled" in {
      val infRepo = mock[InferiorInstanceRepository]
      when(infRepo.isInferiorInstanceEnabled("pdg-devclient")).thenReturn(Future.successful(false))

      val connRepo = mock[ConnectionRepository]
      when(connRepo.getSupInstanceUrl()).thenReturn(Future.successful(None))

      val service = buildService(inferiorInstanceRepo = infRepo, connectionRepo = connRepo)

      val request = FakeRequest("GET", "/superior/profile")
        .withHeaders("X-URL-INSTANCIA-INFERIOR" -> "pdg-devclient")

      val result = Await.result(service.verifyInferiorInstance(request), timeout)
      result.isFailure mustBe true
      result.failed.get mustBe a[IllegalAccessException]
    }

    "return Failure for blockeable URI when X-URL-INSTANCIA-INFERIOR header is missing" in {
      val service = buildService()

      val request = FakeRequest("GET", "/superior/profile")
      // No header

      val result = Await.result(service.verifyInferiorInstance(request), timeout)
      result.isFailure mustBe true
      result.failed.get mustBe a[IllegalAccessException]
    }

    "return Success for blockeable URI when URL matches superior instance" in {
      val infRepo = mock[InferiorInstanceRepository]
      when(infRepo.isInferiorInstanceEnabled("sup-url")).thenReturn(Future.successful(false))

      val connRepo = mock[ConnectionRepository]
      when(connRepo.getSupInstanceUrl()).thenReturn(Future.successful(Some("sup-url")))

      val service = buildService(inferiorInstanceRepo = infRepo, connectionRepo = connRepo)

      val request = FakeRequest("GET", "/superior/profile")
        .withHeaders("X-URL-INSTANCIA-INFERIOR" -> "sup-url")

      Await.result(service.verifyInferiorInstance(request), timeout) mustBe Success("/superior/profile")
    }
  }
}
