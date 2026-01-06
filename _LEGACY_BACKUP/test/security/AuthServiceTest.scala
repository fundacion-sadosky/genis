package security

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import javax.inject.Singleton

import play.api.test.FakeRequest
import user.UserRepository
import services.CacheService
import services.FullUserKey
import audit.OperationLogService
import connections.{ConnectionRepository, InferiorInstanceRepository}
import specs.PdgSpec
import stubs.Stubs
import types.TotpToken
import user.LdapUser
import user.UserStatus
import services.LoggedUserKey
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import play.api.mvc.{Headers, RequestHeader}

import scala.language.implicitConversions
import user.RoleService
import types.Permission
import types.Permission.ALLELIC_FREQ_DB_CRUD

import scala.util.{Failure, Success}

class AuthServiceTest extends PdgSpec with MockitoSugar with AnswerSugar {

  val totpSecret = "CRII5DCIVF4WPP2R"

  val duration = Duration(10, SECONDS)
  val user = LdapUser("userName", "firstName", "lastName", "email", Nil, "userName", "41188080", None, UserStatus.active, totpSecret.getBytes,
    Array.emptyByteArray,
    Array.emptyByteArray)

  val reqToken = Stubs.requestToken
  val totp = Stubs.totpToken
  val authPair = Stubs.authPair
  val reqHeader = FakeRequest.apply("GET", "/Q-ITbmNHC8n15agPXKLyQ3Iomkp9fKryRXaLzCeEwds")
  val fullUser = Stubs.fullUser

  "AuthService" must {
    "authenticate a user" in {
      val cryptoService = mock[CryptoService]
      when(cryptoService.generateRandomCredentials()).thenReturn(Stubs.authPair)
      when(cryptoService.generateDerivatedCredentials("pass")).thenReturn(Stubs.authPair)
      when(cryptoService.decrypt(any[Array[Byte]], any[AuthenticatedPair])).thenAnswer((invocation: InvocationOnMock) => {
        invocation.getArguments()(0)
      })
      val cacheService = mock[CacheService]
      when(cacheService.get(LoggedUserKey("user"))).thenReturn(None)
      val userRepoMock = mock[UserRepository]
      when(userRepoMock.get("user")).thenReturn(Future.successful(user))
      when(userRepoMock.bind("user", "pass")).thenReturn(Future.successful(true))
      val otpServiceMock = mock[OTPService]
      when(otpServiceMock.validate(any[TotpToken], any[String])).thenReturn(true)

      val roleServiceMock = mock[RoleService]
      when(roleServiceMock.getRolePermissions()).thenReturn(Map.empty[String, Set[Permission]].withDefaultValue(Set.empty))

      val service = new AuthServiceImpl(cacheService, userRepoMock, otpServiceMock, cryptoService, 3, 3, roleServiceMock,mock[InferiorInstanceRepository])

      val authPair = Await.result(service.authenticate("user", "pass", totp), duration)

      authPair must not be None
    }

    "decrypt a request header" in {

      val cacheService = mock[CacheService]
      when(cacheService.get(FullUserKey("user", 3))).thenReturn(Some(fullUser))
      val userRepoMock = mock[UserRepository]
      val otpServiceMock = mock[OTPService]
      val operationLogMock = mock[OperationLogService]

      val roleServiceMock = mock[RoleService]
      when(roleServiceMock.getRolePermissions()).thenReturn(Map.empty[String, Set[Permission]].withDefaultValue(Set[Permission](ALLELIC_FREQ_DB_CRUD)))
      val service = new AuthServiceImpl(cacheService, userRepoMock, otpServiceMock, new CryptoServiceImpl(2048), 3, 3, roleServiceMock,mock[InferiorInstanceRepository])

      val requestDecript = service.verifyAndDecryptRequest(reqHeader.uri, reqHeader.method, Some("user"), Some(totp))

      requestDecript mustBe Success("/populationBaseFreq")
    }
    "verify if the inferior instance is enabled" in {
      val cacheService = mock[CacheService]
      when(cacheService.get(FullUserKey("user", 3))).thenReturn(Some(fullUser))
      val userRepoMock = mock[UserRepository]
      val otpServiceMock = mock[OTPService]
      val operationLogMock = mock[OperationLogService]
      val requestHeader = mock[RequestHeader]
      val headers:Headers = mock[Headers]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val connectionRepository = mock[ConnectionRepository]
      when(inferiorInstanceRepository.isInferiorInstanceEnabled("pdg-devclient")).thenReturn(Future.successful(true))
      when(requestHeader.headers).thenReturn(headers)
      when(requestHeader.uri).thenReturn("/status")
      when(connectionRepository.getSupInstanceUrl()).thenReturn(Future.successful(None))
      when(headers.get("X-URL-INSTANCIA-INFERIOR")).thenReturn(Some("pdg-devclient"))


      val roleServiceMock = mock[RoleService]
      when(roleServiceMock.getRolePermissions()).thenReturn(Map.empty[String, Set[Permission]].withDefaultValue(Set[Permission](ALLELIC_FREQ_DB_CRUD)))
      val service = new AuthServiceImpl(cacheService, userRepoMock, otpServiceMock, new CryptoServiceImpl(2048), 3, 3, roleServiceMock,inferiorInstanceRepository,connectionRepository)

      val request1 = service.verifyInferiorInstance(requestHeader)

      request1 mustBe Success("/status")

      when(requestHeader.uri).thenReturn("/else")

      val request2 = service.verifyInferiorInstance(requestHeader)

      request2 mustBe Success("/else")

      when(requestHeader.uri).thenReturn("/superior/profile")
      when(inferiorInstanceRepository.isInferiorInstanceEnabled("pdg-devclient")).thenReturn(Future.successful(false))

      val request3 = service.verifyInferiorInstance(requestHeader)

      request3.isFailure mustBe true

      when(headers.get("X-URL-INSTANCIA-INFERIOR")).thenReturn(None)
      when(inferiorInstanceRepository.isInferiorInstanceEnabled("pdg-devclient")).thenReturn(Future.successful(true))

      val request4 = service.verifyInferiorInstance(requestHeader)

      request4.isFailure mustBe true


    }
  }

}