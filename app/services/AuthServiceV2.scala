package services

import play.api.libs.json._
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import java.time.Instant
import java.util.Date
import repositories.UserRepository
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import com.google.common.io.BaseEncoding
import java.nio.ByteBuffer

/**
 * Servicio de Autenticación con JWT + LDAP (Mejorado)
 * Maneja tokens JWT para usuarios autenticados via LDAP
 * Compatible con sistema antiguo pero mejorado
 */
@Singleton
class AuthServiceV2 @Inject()(
    ldapService: LdapService,
    userRepository: UserRepository
)(implicit ec: ExecutionContext) {

  private val SECRET_KEY = sys.env.getOrElse("JWT_SECRET", "your-secret-key-change-me")
  private val TOKEN_EXPIRATION_HOURS = 24
  private val ALGORITHM = Algorithm.HMAC256(SECRET_KEY)
  
  // Credenciales de setup legacy
  private val SETUP_USER = "setup"
  private val SETUP_PASS = "pass"
  private val SETUP_SECRET = "ETZK6M66LFH3PHIG"

  /**
   * Autentica usuario contra LDAP y genera token JWT
   * Retorna Either[Error, Token]
   */
  def authenticate(username: String, password: String, totp: Option[String] = None): Future[Either[String, String]] = {
    // Caso especial: usuario setup
    if (username == SETUP_USER && password == SETUP_PASS) {
      if (validateTotp(SETUP_SECRET, totp.getOrElse(""))) {
         val token = generateToken(username)
         return Future.successful(Right(token))
      } else {
         return Future.successful(Left("Código de autenticación (Google Authenticator) inválido o requerido"))
      }
    }
    
    // Autenticación LDAP standard
    ldapService.authenticate(username, password).map {
      case Right(_) =>
        // TODO: En el futuro, obtener secret TOTP del usuario LDAP y validar
        val token = generateToken(username)
        Right(token)
      case Left(error) =>
        Left(error)
    }
  }

  /**
   * Versión extendida: autentica y devuelve datos del usuario
   */
  def authenticateWithDetails(username: String, password: String, totp: Option[String] = None): 
      Future[Either[String, (String, JsObject)]] = {
      
    // Caso especial: usuario setup
    if (username == SETUP_USER && password == SETUP_PASS) {
      if (validateTotp(SETUP_SECRET, totp.getOrElse(""))) {
         val token = generateToken(username)
         val details = Json.obj(
          "username" -> username,
          "email" -> "setup@genis.local",
          "displayName" -> "Setup User",
          "roles" -> Json.arr("admin")
        )
         return Future.successful(Right((token, details)))
      } else {
         return Future.successful(Left("Código de autenticación (Google Authenticator) inválido o requerido"))
      }
    }

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
   * Valida un código TOTP contra un secreto Base32
   */
  private def validateTotp(secretKey: String, code: String): Boolean = {
    if (code == null || code.trim.isEmpty) return false
    try {
      val decodedKey = BaseEncoding.base32().decode(secretKey)
      val timeWindow = 30000L // 30 segundos
      val time = System.currentTimeMillis() / timeWindow
      
      // Verificamos ventana actual y una hacia atrás/adelante para tolerancia (±30s)
      val timesToCheck = Seq(time, time - 1, time + 1)
      
      timesToCheck.exists { t => 
        checkCode(decodedKey, t, code)
      }
    } catch {
      case e: Exception => 
        false
    }
  }

  private def checkCode(key: Array[Byte], t: Long, code: String): Boolean = {
    val data = ByteBuffer.allocate(8).putLong(t).array()
    val signKey = new SecretKeySpec(key, "HmacSHA1")
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(signKey)
    val hash = mac.doFinal(data)
    
    val offset = hash(hash.length - 1) & 0xF
    val binary =
      ((hash(offset) & 0x7f) << 24) |
      ((hash(offset + 1) & 0xff) << 16) |
      ((hash(offset + 2) & 0xff) << 8) |
      (hash(offset + 3) & 0xff)
      
    val otp = binary % 1000000
    val otpStr = f"$otp%06d"
    
    otpStr == code
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
