package security

import java.util.NoSuchElementException

import scala.annotation.migration
import scala.concurrent.{Await, Future}
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import org.apache.commons.codec.binary.Base64
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import connections.{ConnectionRepository, InferiorInstanceRepository}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import services.CacheService
import services.FullUserKey
import services.LoggedUserKey
import types.Permission
import types.TotpToken
import user.FullUser
import user.LdapUser
import user.RoleService
import user.UserCredentials
import user.UserRepository

import scala.concurrent.duration.Duration
import scala.util.matching.Regex

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

  implicit val requestTokenFormat = Json.format[RequestToken]
}

object AuthenticatedPair {
  implicit val authenticatedPairFormat = Json.format[AuthenticatedPair]
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
    connectionRepository:ConnectionRepository = null) extends AuthService {

  val logger: Logger = Logger(this.getClass())

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
      //      uri.equals("/superior/profile/approval") ||
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

    uri.equals("/status") || uri.equals("/superior/category-tree-combo") || uri.equals("/superior/connection")|| isBlockeableBySuperiorInstance(uri)

  }
  private def validateOneTimePass(userName: String, otp: TotpToken): Boolean = {
    cache.get(LoggedUserKey(userName))
      .fold(true)(usedOtp => usedOtp != otp)
  }

  override def authenticate(userName: String, password: String, otp: TotpToken): Future[Option[FullUser]] = {

    userRepository.bind(userName, password) flatMap { success =>
      if (success) {
        userRepository.get(userName) map { ldapUser =>
          val user = LdapUser.toUser(ldapUser, roleService.getRolePermissions())

          val authenticatedPair = cryptoService.generateRandomCredentials()
          val userCredentials = cryptoService.generateDerivatedCredentials(password)
          logger.debug(ldapUser.toString())
          logger.debug(ldapUser.encryptedPrivateKey.toString())
          logger.debug(userCredentials.toString())
          val privateKey = cryptoService.decrypt(ldapUser.encryptedPrivateKey, userCredentials)
          val publicKey = cryptoService.decrypt(ldapUser.encryptedPublicKey, userCredentials)
          val userTotp = /*ldapUser.encryptrdTotpSecret*/ cryptoService.decrypt(ldapUser.encryptrdTotpSecret, userCredentials)
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
    if(isBlockeableBySuperiorInstance(request.uri)){

      val url = request.headers
        .get("X-URL-INSTANCIA-INFERIOR")

      url match {
        case Some(url) =>{
          val isEnabled = Await.result(inferiorInstanceRepository.isInferiorInstanceEnabled(url),Duration.Inf)
          val isEnabledSuperior = Await.result(connectionRepository.getSupInstanceUrl(),Duration.Inf).contains(url)
          if(isEnabled || isEnabledSuperior){
            Success(request.uri)
          }else{
            if(!isEnabled){
              Failure(new IllegalAccessException(s"Inferior instance is disabled"))
            }else{
              Failure(new IllegalAccessException(s"Instance Unknown"))
            }
          }
        }
        case _ =>   Failure(new IllegalAccessException(s"Inferior instance is disabled"))
      }

    }else{
      Success(request.uri)
    }
  }

  def verifyAndDecryptRequest(encryptedUri: String, verb: String, userNameOpt: Option[String], otp: Option[TotpToken]): Try[String] = {
    if (isPublicResource(encryptedUri)) {
      Success(encryptedUri)
    } else {

      userNameOpt.fold[Try[String]] {
        Failure(new IllegalAccessException("Non public reosurces request must have 'X-USER' header"))
      } { userName =>
        cache.get(FullUserKey(userName, credentialsExpTime)).fold[Try[String]]({
          val msg = s"no authenticated for $userName"
          logger.info(msg);
          Failure(new NoSuchElementException(msg)) // No authenticatedPair, badRequest
        })({ user =>
          // http://blog.cloudme.org/2013/08/interoperable-aes-encryption-with-java-and-javascript/
          val authenticatedPair = user.credentials
          logger.trace("using " + authenticatedPair)

          val uriToDecrypt = encryptedUri.substring(1)

          logger.trace("decrypting " + uriToDecrypt)

          val decryptedUriBytes = cryptoService.decrypt(Base64.decodeBase64(uriToDecrypt), authenticatedPair)

          val decryptedUri = new String(decryptedUriBytes)

          logger.trace("decryptedUri is " + decryptedUri)

          val operation = AuthorisationOperation(decryptedUri.takeWhile { _ != '?' }, verb)
          val isAuthorized = canPerform(userName, operation)

          if (isAuthorized) {

            if (validateSensitiveTotp(userName, operation, otp)) {
              Success(decryptedUri)
            } else {
              Failure(new IllegalAccessException(s"User $userName has not provided a valid Totp for resource $operation"))
            }

          } else {
            Failure(new IllegalAccessException(s"User $userName has no priviledges for resource $operation"))
          }

        })

      }

    }

  }

  override def getSensitiveOperations(): Set[AuthorisationOperation] = {
    val permissionToOperationSet: Set[StaticAuthorisationOperation] =
      Permission.list.foldLeft(
        Set[StaticAuthorisationOperation]())((ops, permission) => ops ++ permission.operations)

    permissionToOperationSet.filter(_.isSensitive).map(x => AuthorisationOperation(x.resource.toString, x.action.toString))
  }

  override def getCredentials(userName: String): Option[AuthenticatedPair] = {
    cache.get(FullUserKey(userName, credentialsExpTime)).map { _.credentials }
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

    cache.get(FullUserKey(userId, credentialsExpTime)).fold({
      false
    })({ user =>
      val permissions = (user.userDetail.roles flatMap { role =>
        roleService.getRolePermissions()(role)
      }).toSet
      (for {
        permision <- permissions
        operations <- permision.operations
      } yield (operations)).exists { x => matchAuthOp(x, operation) }
    })
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
      case true => {
        if (otp.isEmpty) false
        else {
          val user = cache.get(FullUserKey(userId, credentialsExpTime)).get
          otpService.validate(otp.get, user.cryptoCredentials.totpSecret)
        }
      }
    }
  }

}
