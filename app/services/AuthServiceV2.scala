package services

import play.api.libs.json._
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import java.time.Instant
import java.util.Date

/**
 * Servicio de Autenticación con JWT + LDAP (Mejorado)
 * Maneja tokens JWT para usuarios autenticados via LDAP
 * Compatible con sistema antiguo pero mejorado
 */
@Singleton
class AuthServiceV2 @Inject()(
    ldapService: LdapService
)(implicit ec: ExecutionContext) {

  private val SECRET_KEY = sys.env.getOrElse("JWT_SECRET", "your-secret-key-change-me")
  private val TOKEN_EXPIRATION_HOURS = 24
  private val ALGORITHM = Algorithm.HMAC256(SECRET_KEY)

  /**
   * Autentica usuario contra LDAP y genera token JWT
   * Retorna Either[Error, Token]
   */
  def authenticate(username: String, password: String): Future[Either[String, String]] = {
    ldapService.authenticate(username, password).map {
      case Right(_) =>
        val token = generateToken(username)
        Right(token)
      case Left(error) =>
        Left(error)
    }
  }

  /**
   * Versión extendida: autentica y devuelve datos del usuario
   */
  def authenticateWithDetails(username: String, password: String): 
      Future[Either[String, (String, JsObject)]] = {
    ldapService.authenticate(username, password).map {
      case Right(ldapUser) =>
        val token = generateToken(username)
        val details = Json.obj(
          "username" -> username,
          "email" -> ldapUser.mail,
          "displayName" -> ldapUser.cn,
          "roles" -> Json.arr() // TODO: Obtener de BD existente
        )
        Right((token, details))
      case Left(error) =>
        Left(error)
    }
  }

  /**
   * Genera un token JWT válido por 24 horas
   */
  def generateToken(username: String): String = {
    val now = Instant.now()
    val expiration = now.plusSeconds(TOKEN_EXPIRATION_HOURS * 3600)

    JWT.create()
      .withSubject(username)
      .withIssuedAt(Date.from(now))
      .withExpiresAt(Date.from(expiration))
      .withClaim("type", "auth")
      .sign(ALGORITHM)
  }

  /**
   * Valida un token JWT sin excepciones
   */
  def validateToken(token: String): Future[Either[String, JsValue]] = {
    Future {
      try {
        val decodedJWT = JWT.require(ALGORITHM)
          .build()
          .verify(token)

        val username = decodedJWT.getSubject
        val issuedAt = decodedJWT.getIssuedAt
        val expiresAt = decodedJWT.getExpiresAt

        Right(Json.obj(
          "username" -> username,
          "issuedAt" -> issuedAt.getTime,
          "expiresAt" -> expiresAt.getTime,
          "valid" -> true
        ))
      } catch {
        case _: JWTVerificationException =>
          Left("Token inválido o expirado")
        case e: Exception =>
          Left(s"Error validando token: ${e.getMessage}")
      }
    }
  }

  /**
   * Decodifica token sin validar (solo para extraer claims)
   */
  def decodeToken(token: String): Either[String, JsValue] = {
    try {
      val decodedJWT = JWT.decode(token)
      val username = decodedJWT.getSubject
      val expiresAt = decodedJWT.getExpiresAt
      
      Right(Json.obj(
        "username" -> username,
        "expiresAt" -> expiresAt.getTime
      ))
    } catch {
      case e: Exception =>
        Left(s"Error decodificando token: ${e.getMessage}")
    }
  }
}
