package security

import java.util.{Base64, NoSuchElementException}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import javax.inject.{Inject, Named, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import types.TotpToken

import scala.concurrent.duration.Duration
import scala.util.matching.Regex

// ============================================================================
// STUBS para dependencias del legacy que aún no están migradas
// ============================================================================

// TODO: Migrar desde services.CacheService
trait CacheService {
  def get[T](key: CacheKey[T]): Option[T]
  def set[T](key: CacheKey[T], value: T): Unit
}

trait CacheKey[T] {
  def key: String
  def expiration: Int
}

case class FullUserKey(userName: String, expiration: Int) extends CacheKey[FullUser] {
  def key = s"fullUser:$userName"
}

case class LoggedUserKey(userName: String) extends CacheKey[TotpToken] {
  def key = s"loggedUser:$userName"
  def expiration = 3600
}

// Stub implementation
@Singleton
class CacheServiceStub @Inject() () extends CacheService {
  private val logger = Logger(this.getClass)
  private val cache = scala.collection.mutable.Map[String, Any]()

  override def get[T](key: CacheKey[T]): Option[T] = {
    logger.warn(s"CacheService.get STUB para key ${key.key}")
    cache.get(key.key).map(_.asInstanceOf[T])
  }

  override def set[T](key: CacheKey[T], value: T): Unit = {
    logger.warn(s"CacheService.set STUB para key ${key.key}")
    cache.put(key.key, value)
  }
}

// TODO: Migrar desde user.*
case class User(
  userName: String,
  roles: Set[String],
  status: String,
  canLogin: Boolean = true
)

case class LdapUser(
  userName: String,
  encryptedPrivateKey: Array[Byte],
  encryptedPublicKey: Array[Byte],
  encryptrdTotpSecret: Array[Byte]
)

object LdapUser {
  def toUser(ldapUser: LdapUser, rolePermissions: Map[String, Set[Permission]]): User = {
    User(ldapUser.userName, Set.empty, "active", true)
  }
}

case class UserCredentials(publicKey: Array[Byte], privateKey: Array[Byte], totpSecret: String)

case class FullUser(userDetail: User, cryptoCredentials: UserCredentials, credentials: AuthenticatedPair)

object FullUser {
  // Nota: FullUser no tiene formato JSON completo por los Array[Byte]
}

trait UserRepository {
  def bind(userName: String, password: String): Future[Boolean]
  def get(userName: String): Future[LdapUser]
}

@Singleton
class UserRepositoryStub @Inject() ()(using ec: ExecutionContext) extends UserRepository {
  private val logger = Logger(this.getClass)

  override def bind(userName: String, password: String): Future[Boolean] = {
    logger.warn(s"UserRepository.bind STUB para $userName")
    Future.successful(false)
  }

  override def get(userName: String): Future[LdapUser] = {
    logger.warn(s"UserRepository.get STUB para $userName")
    Future.failed(new NoSuchElementException(s"User $userName not found (STUB)"))
  }
}

// TODO: Migrar desde user.RoleService
case class Permission(name: String, operations: Set[StaticAuthorisationOperation])

object Permission {
  // STUB: lista vacía de permisos
  def list: List[Permission] = List.empty
}

trait RoleService {
  def getRolePermissions(): Map[String, Set[Permission]]
}

@Singleton
class RoleServiceStub @Inject() () extends RoleService {
  override def getRolePermissions(): Map[String, Set[Permission]] = Map.empty
}

// TODO: Migrar desde connections.*
trait InferiorInstanceRepository {
  def isInferiorInstanceEnabled(url: String): Future[Boolean]
}

@Singleton
class InferiorInstanceRepositoryStub @Inject() ()(using ec: ExecutionContext) extends InferiorInstanceRepository {
  override def isInferiorInstanceEnabled(url: String): Future[Boolean] = Future.successful(false)
}

trait ConnectionRepository {
  def getSupInstanceUrl(): Future[Option[String]]
}

@Singleton
class ConnectionRepositoryStub @Inject() ()(using ec: ExecutionContext) extends ConnectionRepository {
  override def getSupInstanceUrl(): Future[Option[String]] = Future.successful(None)
}

// ============================================================================
// Código migrado de AuthService
// ============================================================================

abstract class AuthService {
  def authenticate(userId: String, password: String, otp: TotpToken): Future[Option[FullUser]]
  def verifyAndDecryptRequest(encryptedUri: String, verb: String, userId: Option[String], otp: Option[TotpToken]): Try[String]
  def getSensitiveOperations(): Set[AuthorisationOperation]
  def getCredentials(userName: String): Option[AuthenticatedPair]
  def isPublicResource(uri: String): Boolean
  def isBlockeableBySuperiorInstance(uri: String): Boolean
  def isInterconnectionResource(uri: String): Boolean
  def verifyInferiorInstance(request: RequestHeader): Try[String]
}

case class AuthorisationOperation(resource: String, action: String)

case class StaticAuthorisationOperation(resource: Regex, action: Regex, descriptionKey: String, isSensitive: Boolean = false)

case class RequestToken(token: String)
case class AuthenticatedPair(verifier: String, key: String, iv: String)

object RequestToken {
  given requestTokenFormat: play.api.libs.json.Format[RequestToken] = Json.format[RequestToken]
}

object AuthenticatedPair {
  given authenticatedPairFormat: play.api.libs.json.Format[AuthenticatedPair] = Json.format[AuthenticatedPair]
}

@Singleton
class AuthServiceImpl @Inject() (
    cache: CacheService,
    userRepository: UserRepository,
    otpService: OTPService,
    cryptoService: CryptoService,
    @Named("tokenExpTime") val tokenExpTime: Int,
    @Named("credentialsExpTime") val credentialsExpTime: Int,
    roleService: RoleService,
    inferiorInstanceRepository: InferiorInstanceRepository,
    connectionRepository: ConnectionRepository
)(using ec: ExecutionContext) extends AuthService {

  val logger: Logger = Logger(this.getClass)

  override def isPublicResource(uri: String): Boolean = {
    uri.startsWith("/assets") ||
      uri.equals("/") ||
      uri.equals("/favicon.ico") ||
      uri.equals("/login") ||
      uri.equals("/status") ||
      uri.equals("/superior/category-tree-combo") ||
      uri.equals("/superior/connection") ||
      uri.startsWith("/superior/profile") ||
      uri.startsWith("/inferior/match/") ||
      uri.startsWith("/inferior/profile/status") ||
      uri.equals("/interconection/match/status") ||
      uri.equals("/interconnection/file") ||
      uri.startsWith("/jsroutes.js") ||
      uri.startsWith("/sensitiveOper") ||
      uri.startsWith("/profiles-uploader/") ||
      uri.startsWith("/appConf") ||
      uri.startsWith("/resources/temporary/") ||
      uri.startsWith("/resources/static/") ||
      uri.startsWith("/resources/proto/static/") ||
      uri.startsWith("/signup") ||
      uri.startsWith("/clear-password") ||
      uri.startsWith("/disclaimer") ||
      uri.startsWith("/rolesForSU") ||
      uri.matches("^/profiles/.+/epgs$") ||
      uri.matches("^/profiles/.+/file$")
  }

  override def isBlockeableBySuperiorInstance(uri: String): Boolean = {
    uri.startsWith("/superior/profile") || uri.equals("/interconnection/file") || uri.equals("/interconection/match/status") || uri.equals("/inferior/match/")
  }

  override def isInterconnectionResource(uri: String): Boolean = {
    uri.equals("/status") || uri.equals("/superior/category-tree-combo") || uri.equals("/superior/connection") || isBlockeableBySuperiorInstance(uri)
  }

  private def validateOneTimePass(userName: String, otp: TotpToken): Boolean = {
    cache.get(LoggedUserKey(userName))
      .fold(true)(usedOtp => usedOtp != otp)
  }

  override def authenticate(userName: String, password: String, otp: TotpToken): Future[Option[FullUser]] = {

    userRepository.bind(userName, password).flatMap { success =>
      if (success) {
        userRepository.get(userName).map { ldapUser =>
          val user = LdapUser.toUser(ldapUser, roleService.getRolePermissions())

          val authenticatedPair = cryptoService.generateRandomCredentials()
          val userCredentials = cryptoService.generateDerivatedCredentials(password)
          logger.debug(ldapUser.toString)
          logger.debug(ldapUser.encryptedPrivateKey.toString)
          logger.debug(userCredentials.toString)
          val privateKey = cryptoService.decrypt(ldapUser.encryptedPrivateKey, userCredentials)
          val publicKey = cryptoService.decrypt(ldapUser.encryptedPublicKey, userCredentials)
          val userTotp = cryptoService.decrypt(ldapUser.encryptrdTotpSecret, userCredentials)
          val userSecret = new String(userTotp, "UTF-8")
          val userCrypto = UserCredentials(publicKey, privateKey, userSecret)
          val fullUser = FullUser(user, userCrypto, authenticatedPair)

          if (user.canLogin && validateOneTimePass(userName, otp)) {
            if (otpService.validate(otp, fullUser.cryptoCredentials.totpSecret)) {
              cache.set(FullUserKey(userName, credentialsExpTime), fullUser)
              cache.set(LoggedUserKey(userName), otp)
              Some(fullUser)
            } else {
              logger.warn(s"Token $otp is expired or invalid for $userName")
              None
            }
          } else {
            logger.warn(s"$userName is with status ${user.status} is not allowed to login")
            None
          }
        }
      } else {
        logger.warn(s"Can't bind $userName to LDAP")
        Future.successful(None)
      }
    }
  }

  def verifyInferiorInstance(request: RequestHeader): Try[String] = {
    if (isBlockeableBySuperiorInstance(request.uri)) {
      val url = request.headers.get("X-URL-INSTANCIA-INFERIOR")

      url match {
        case Some(url) =>
          val isEnabled = Await.result(inferiorInstanceRepository.isInferiorInstanceEnabled(url), Duration.Inf)
          val isEnabledSuperior = Await.result(connectionRepository.getSupInstanceUrl(), Duration.Inf).contains(url)
          if (isEnabled || isEnabledSuperior) {
            Success(request.uri)
          } else {
            if (!isEnabled) {
              Failure(new IllegalAccessException(s"Inferior instance is disabled"))
            } else {
              Failure(new IllegalAccessException(s"Instance Unknown"))
            }
          }
        case _ => Failure(new IllegalAccessException(s"Inferior instance is disabled"))
      }
    } else {
      Success(request.uri)
    }
  }

  def verifyAndDecryptRequest(
    encryptedUri: String,
    verb: String,
    userNameOpt: Option[String],
    otp: Option[TotpToken]
  ): Try[String] = {
    if (encryptedUri.startsWith("/categories/import")) {
      Success(encryptedUri)
    } else if (encryptedUri.startsWith("/strkit/import")) {
      Success(encryptedUri)
    } else if (encryptedUri.startsWith("/locus/import")) {
      Success(encryptedUri)
    } else if (encryptedUri.startsWith("/roles/import")) {
      Success(encryptedUri)
    } else if (isPublicResource(encryptedUri)) {
      Success(encryptedUri)
    } else {
      userNameOpt.fold[Try[String]] {
        Failure(
          new IllegalAccessException(
            "Non public reosurces request must have 'X-USER' header"
          )
        )
      } { userName =>
        cache
          .get(FullUserKey(userName, credentialsExpTime))
          .fold[Try[String]]({
            val msg = s"no authenticated for $userName"
            logger.info(msg)
            Failure(new NoSuchElementException(msg))
          })({ user =>
            val authenticatedPair = user.credentials
            logger.trace("using " + authenticatedPair)
            val uriToDecrypt = encryptedUri.substring(1)
            logger.trace("decrypting " + uriToDecrypt)
            val decryptedUriBytes = cryptoService.decrypt(
              Base64.getDecoder.decode(uriToDecrypt),
              authenticatedPair
            )
            val decryptedUri = new String(decryptedUriBytes)
            logger.trace("decryptedUri is " + decryptedUri)
            val operation = AuthorisationOperation(decryptedUri.takeWhile(_ != '?'), verb)
            val isAuthorized = canPerform(userName, operation)
            if (isAuthorized) {
              if (validateSensitiveTotp(userName, operation, otp)) {
                Success(decryptedUri)
              } else {
                Failure(
                  new IllegalAccessException(
                    s"User $userName has not provided a valid Totp for resource $operation"
                  )
                )
              }
            } else {
              Failure(
                new IllegalAccessException(
                  s"User $userName has no priviledges for resource $operation"
                )
              )
            }
          })
      }
    }
  }

  override def getSensitiveOperations(): Set[AuthorisationOperation] = {
    val permissionToOperationSet: Set[StaticAuthorisationOperation] =
      Permission.list.foldLeft(Set[StaticAuthorisationOperation]())((ops, permission) => ops ++ permission.operations)

    permissionToOperationSet.filter(_.isSensitive).map(x => AuthorisationOperation(x.resource.toString, x.action.toString))
  }

  override def getCredentials(userName: String): Option[AuthenticatedPair] = {
    cache.get(FullUserKey(userName, credentialsExpTime)).map(_.credentials)
  }

  private def matchAuthOp(static: StaticAuthorisationOperation, operation: AuthorisationOperation): Boolean = {
    val resource = operation.resource match {
      case static.resource(_*) => true
      case _                   => false
    }
    val httpVerb = operation.action match {
      case static.action(_*) => true
      case _                 => false
    }
    resource && httpVerb
  }

  private def canPerform(userId: String, operation: AuthorisationOperation): Boolean = {
    cache.get(FullUserKey(userId, credentialsExpTime)).fold(false) { user =>
      val permissions = user.userDetail.roles.flatMap { role =>
        roleService.getRolePermissions().getOrElse(role, Set.empty)
      }
      (for {
        permission <- permissions
        operations <- permission.operations
      } yield operations).exists(x => matchAuthOp(x, operation))
    }
  }

  private def validateSensitiveTotp(userId: String, operation: AuthorisationOperation, otp: Option[TotpToken]): Boolean = {

    def isSensitive: Boolean = {
      val permissionToOperationList = Permission.list.foldLeft(
        List[StaticAuthorisationOperation]())((appendList, permission) => appendList ::: permission.operations.toList)

      permissionToOperationList.find(static => matchAuthOp(static, operation)) match {
        case Some(staticAuthOperation) => staticAuthOperation.isSensitive
        case _                         => false
      }
    }

    isSensitive match {
      case false => true
      case true =>
        if (otp.isEmpty) false
        else {
          val user = cache.get(FullUserKey(userId, credentialsExpTime)).get
          otpService.validate(otp.get, user.cryptoCredentials.totpSecret)
        }
    }
  }
}
