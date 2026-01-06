package user

import inbox.{Notification, NotificationInfo, NotificationService}
import specs.PdgSpec
import org.scalatest.mock.MockitoSugar
import services.{CacheService, ClearPassRequestKey}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mock.MockitoSugar
import stubs.Stubs

import scala.concurrent.Future
import security.{AuthenticatedPair, CryptoServiceImpl, OTPService}
import types.Permission
import types.Permission.{INF_INS_CRUD, USER_CRUD}

import scala.collection.immutable.HashMap
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS

class UserServiceTest extends PdgSpec with MockitoSugar {

  val notiService = mock[NotificationService]
  val roleService = mock[RoleService]
  val cacheMockService = mock[CacheService]
  val oTPService = mock[OTPService]
  val cryptoMock = mock[CryptoServiceImpl]

  val cryptoService = new CryptoServiceImpl(2048)
  val userRepo = mock[UserRepository]
  val users = Seq(Stubs.ldapUser)
  val duration = Duration(10, SECONDS)
  
  "UserService" must{
    "provide a unique username" in{
      
      
      when(userRepo.finfByUid(any[Seq[String]])).thenReturn(Future.successful(users))

      val userService = new UserServiceImpl(userRepo,cacheMockService,cryptoService,null, null, null)
      
      val signUp = SignupSolicitude("user","lastone","enter","a@g.cc",Seq("rol1"),"idgen","14789652", None, false)
      
      val result = Await.result(userService.signupRequest(signUp), duration)
      
      result.isRight mustBe true
      
      result.right.get.userNameCandidates(0) mustBe "ulastone"
    }
    "provide username and password for reset" in{

      val ldapUser = LdapUser("user", "username", "lastname",
        "usl@example.com", Seq("clerk", "geneticist"),
        "genId", "41188080", None, UserStatus.pending_reset,
        Array.emptyByteArray, Array.emptyByteArray,
        Array.emptyByteArray)
      when(userRepo.finfByUid(any[Seq[String]])).thenReturn(Future.successful(users))

      when(userRepo.get("user")).thenReturn(Future.successful(ldapUser))

      val userService = new UserServiceImpl(userRepo,cacheMockService,cryptoService,null, null, null)

      val clearPassSolicitud = ClearPassSolicitud("user","123456")

      val result = Await.result(userService.clearPassRequest(clearPassSolicitud), duration)

      result.isRight mustBe true

    }
    "provide username and password for reset with invalid user E0641" in{

      val ldapUser = LdapUser("user", "username", "lastname",
        "usl@example.com", Seq("clerk", "geneticist"),
        "genId", "41188080", None, UserStatus.active,
        Array.emptyByteArray, Array.emptyByteArray,
        Array.emptyByteArray)
      when(userRepo.finfByUid(any[Seq[String]])).thenReturn(Future.successful(users))

      when(userRepo.get("user")).thenReturn(Future.successful(ldapUser))

      val userService = new UserServiceImpl(userRepo,cacheMockService,cryptoService,null, null, null)

      val clearPassSolicitud = ClearPassSolicitud("user","123456")

      val result = Await.result(userService.clearPassRequest(clearPassSolicitud), duration)

      result.isLeft mustBe true
      result mustBe Left("E0641: No existe el usuario.")

    }

    "provide username and password for reset with inexistent user E0641" in{


      when(userRepo.finfByUid(any[Seq[String]])).thenReturn(Future.successful(users))

      when(userRepo.get("user")).thenReturn(Future.failed(new RuntimeException()))

      val userService = new UserServiceImpl(userRepo,cacheMockService,cryptoService,null, null, null)

      val clearPassSolicitud = ClearPassSolicitud("user","123456")

      val result = Await.result(userService.clearPassRequest(clearPassSolicitud), duration)

      result.isLeft mustBe true
      result mustBe Left("E0641: No existe el usuario.")
    }

    "confirm password change ok" in {

      when(userRepo.finfByUid(any[Seq[String]])).thenReturn(Future.successful(users))

      when(userRepo.get("user")).thenReturn(Future.failed(new RuntimeException()))
      val totpToken = Stubs.totpToken
      val clearPassChallenge = ClearPassChallenge("ID",totpToken)
      val clearPassSolicitud = ClearPassSolicitud("user","123456")

      when(cacheMockService.pop(ClearPassRequestKey(clearPassChallenge.clearPassRequestId)))
        .thenReturn(Some((clearPassSolicitud, Stubs.userCredentials,clearPassSolicitud.userName)))

      when(oTPService.validate(totpToken,Stubs.userCredentials.totpSecret)).thenReturn(true)
      when(userRepo.clearPass(clearPassSolicitud.userName,UserStatus.pending,clearPassSolicitud.newPassword,
        cryptoService.encrypt(Stubs.userCredentials.totpSecret.getBytes, cryptoService.generateDerivatedCredentials(clearPassSolicitud.newPassword))))
          .thenReturn(Future.successful(clearPassSolicitud.userName))

      val permissions: Map[String, Set[Permission]] = HashMap("admin" -> Set(USER_CRUD))

      when(roleService.getRolePermissions()).thenReturn(permissions)
      val ldapUserAdmin = LdapUser("user", "username", "lastname",
        "usl@example.com", Seq("admin"),
        "genId", "41188080", None, UserStatus.active,
        Array.emptyByteArray, Array.emptyByteArray,
        Array.emptyByteArray)
      when(userRepo.findByRole("admin")).thenReturn(Future.successful(List(ldapUserAdmin)))

      val userService = new UserServiceImpl(userRepo,cacheMockService,cryptoService,oTPService, notiService, roleService)

      val result = Await.result(userService.clearPassConfirmation(clearPassChallenge), duration)

      result.isRight mustBe true

    }
    "confirm password change with totp invalid E0803" in {

      when(userRepo.finfByUid(any[Seq[String]])).thenReturn(Future.successful(users))

      when(userRepo.get("user")).thenReturn(Future.failed(new RuntimeException()))
      val totpToken = Stubs.totpToken
      val clearPassChallenge = ClearPassChallenge("ID",totpToken)
      val clearPassSolicitud = ClearPassSolicitud("user","987654")

      when(cacheMockService.pop(ClearPassRequestKey(clearPassChallenge.clearPassRequestId)))
        .thenReturn(Some((clearPassSolicitud, Stubs.userCredentials,clearPassSolicitud.userName)))

      when(oTPService.validate(totpToken,Stubs.userCredentials.totpSecret)).thenReturn(false)
      when(userRepo.clearPass(clearPassSolicitud.userName,UserStatus.pending,clearPassSolicitud.newPassword,
        cryptoService.encrypt(Stubs.userCredentials.totpSecret.getBytes, cryptoService.generateDerivatedCredentials(clearPassSolicitud.newPassword))))
        .thenReturn(Future.successful(clearPassSolicitud.userName))

      val permissions: Map[String, Set[Permission]] = HashMap("admin" -> Set(USER_CRUD))

      when(roleService.getRolePermissions()).thenReturn(permissions)
      val ldapUserAdmin = LdapUser("user", "username", "lastname",
        "usl@example.com", Seq("admin"),
        "genId", "41188080", None, UserStatus.active,
        Array.emptyByteArray, Array.emptyByteArray,
        Array.emptyByteArray)
      when(userRepo.findByRole("admin")).thenReturn(Future.successful(List(ldapUserAdmin)))

      val userService = new UserServiceImpl(userRepo,cacheMockService,cryptoService,oTPService, notiService, roleService)

      val result = Await.result(userService.clearPassConfirmation(clearPassChallenge), duration)

      result.isLeft mustBe true
      result mustBe Left("E0803: Token inválido.")
    }
    "list all users" in {
      
      when(userRepo.listAllUsers()).thenReturn(Future.successful(users))
      
      val userService = new UserServiceImpl(userRepo,null,null,null, null, null)
      
      val result = Await.result(userService.listAllUsers(), duration)
      
      result.length mustBe 1
    }
    
    "get a user" in {
      
      when(userRepo.get("username")).thenReturn(Future.successful(Stubs.ldapUser))
      
      val userService = new UserServiceImpl(userRepo,null,null,null, null, null)
      
      val result = Await.result(userService.getUser("username"), duration)
      
      result.userName mustBe "user"
      result.status mustBe UserStatus.active
    }

    "get a user by genemapper" in {

      when(userRepo.findByGeneMapper("genId")).thenReturn(Future.successful(Some(Stubs.ldapUser)))

      val userService = new UserServiceImpl(userRepo,null,null,null, null, null)

      val result = Await.result(userService.findByGeneMapper("genId"), duration)

      result.isDefined mustBe true
      result.get.userName mustBe "user"
    }

    "get a user by genemapper - no results" in {

      when(userRepo.findByGeneMapper("genId")).thenReturn(Future.successful(None))

      val userService = new UserServiceImpl(userRepo,null,null,null, null, null)

      val result = Await.result(userService.findByGeneMapper("genId"), duration)

      result.isDefined mustBe false
    }


  }
  
}