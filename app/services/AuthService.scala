package services

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import play.api.Configuration
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.time.Instant
import java.util.Date

/**
 * Servicio de Autenticación Moderno
 * Maneja LDAP y JWT
 */
@Singleton
class AuthService @Inject()(
    config: Configuration,
    ldapService: LdapService
)(implicit ec: ExecutionContext) {

  private val jwtSecret = config.get[String]("jwt.secret")
  private val jwtExpiration = config.get[Long]("jwt.expiration")
  private val algorithm = Algorithm.HMAC256(jwtSecret)

  /**
   * Autentica un usuario contra LDAP y genera un JWT
   */
  def authenticate(username: String, password: String): Future[Either[String, String]] = {
    ldapService.authenticate(username, password).map {
      case Right(ldapUser) =>
        Try {
          val now = Instant.now()
          val expiresAt = new Date(now.toEpochMilli + jwtExpiration * 1000)
          
          val token = JWT.create()
            .withSubject(username)
            .withClaim("userId", ldapUser.uid)
            .withClaim("email", ldapUser.mail)
            .withClaim("cn", ldapUser.cn)
            .withIssuedAt(new Date())
            .withExpiresAt(expiresAt)
            .sign(algorithm)
          
          Right(token)
        } match {
          case Success(token) => token
          case Failure(e) => 
            Left(s"Error generando token JWT: ${e.getMessage}")
        }
      case Left(error) => Left(error)
    }
  }

  /**
   * Valida un token JWT
   */
  def validateToken(token: String): Future[Either[String, Map[String, String]]] = {
    Try {
      val decodedJWT = JWT.require(algorithm)
        .build()
        .verify(token)
      
      Right(Map(
        "userId" -> decodedJWT.getSubject,
        "email" -> decodedJWT.getClaim("email").asString(),
        "cn" -> decodedJWT.getClaim("cn").asString()
      ))
    } match {
      case Success(result) => Future.successful(result)
      case Failure(e) => Future.successful(Left(s"Token inválido: ${e.getMessage}"))
    }
  }

  /**
   * Decodifica un JWT sin validar firma (solo para lectura)
   */
  def decodeToken(token: String): Either[String, Map[String, String]] = {
    Try {
      val decodedJWT = JWT.decode(token)
      Right(Map(
        "userId" -> decodedJWT.getSubject,
        "email" -> Option(decodedJWT.getClaim("email")).map(_.asString()).getOrElse(""),
        "cn" -> Option(decodedJWT.getClaim("cn")).map(_.asString()).getOrElse("")
      ))
    } match {
      case Success(result) => result
      case Failure(e) => Left(s"Error decodificando token: ${e.getMessage}")
    }
  }
}

/**
 * Modelo de usuario LDAP
 */
case class LdapUser(
    uid: String,
    cn: String,
    mail: String,
    distinguishedName: String
)
