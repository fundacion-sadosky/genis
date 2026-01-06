package controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import services.AuthServiceV2

/**
 * Controlador de Autenticación Mejorado
 * Soporta LDAP e JWT para autenticación y autorización
 * Compatible con cliente original pero con respuestas JSON mejoradas
 */
@Singleton
class AuthControllerV2 @Inject()(
    cc: ControllerComponents,
    authService: AuthServiceV2
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  /**
   * Endpoint de login mejorado
   * POST /api/auth/login
   * Body: {"username": "usuario", "password": "contraseña"}
   * Response: {"success": true, "token": "...", "user": {...}}
   */
  def login(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    val usernameOpt = (request.body \ "username").asOpt[String]
    val passwordOpt = (request.body \ "password").asOpt[String]

    (usernameOpt, passwordOpt) match {
      case (Some(username), Some(password)) =>
        authService.authenticateWithDetails(username, password).map {
          case Right((token, details)) =>
            Ok(Json.obj(
              "success" -> true,
              "token" -> token,
              "expiresIn" -> 86400,
              "user" -> details,
              "message" -> "Autenticación exitosa"
            ))
          case Left(error) =>
            Unauthorized(Json.obj(
              "success" -> false,
              "error" -> error
            ))
        }
      case _ =>
        Future.successful(BadRequest(Json.obj(
          "success" -> false,
          "error" -> "username y password son requeridos"
        )))
    }
  }

  /**
   * Endpoint de logout
   * POST /api/auth/logout
   */
  def logout(): Action[AnyContent] = Action.async { implicit request =>
    // En un sistema real, aquí invalidaríamos el token
    // Por ahora es un endpoint dummy
    Future.successful(Ok(Json.obj(
      "success" -> true,
      "message" -> "Sesión cerrada"
    )))
  }

  /**
   * Endpoint para validar token JWT
   * GET /api/auth/validate
   * Headers: Authorization: Bearer <token>
   */
  def validateToken(): Action[AnyContent] = Action.async { implicit request =>
    val tokenOpt = request.headers.get("Authorization")
      .flatMap { authHeader =>
        val parts = authHeader.split(" ")
        if (parts.length == 2 && parts(0) == "Bearer") {
          Some(parts(1))
        } else {
          None
        }
      }

    tokenOpt match {
      case Some(token) =>
        authService.validateToken(token).map {
          case Right(claims) =>
            val username = (claims \ "username").asOpt[String].getOrElse("")
            val expiresAt = (claims \ "expiresAt").asOpt[Long].getOrElse(0L)
            Ok(Json.obj(
              "valid" -> true,
              "user" -> username,
              "expiresAt" -> expiresAt
            ))
          case Left(error) =>
            Unauthorized(Json.obj(
              "valid" -> false,
              "error" -> error
            ))
        }
      case None =>
        Future.successful(BadRequest(Json.obj(
          "valid" -> false,
          "error" -> "Token no proporcionado en header Authorization"
        )))
    }
  }

  /**
   * Endpoint para refrescar token (si está próximo a expirar)
   * POST /api/auth/refresh
   * Headers: Authorization: Bearer <token>
   */
  def refreshToken(): Action[AnyContent] = Action.async { implicit request =>
    val tokenOpt = request.headers.get("Authorization")
      .flatMap { authHeader =>
        val parts = authHeader.split(" ")
        if (parts.length == 2 && parts(0) == "Bearer") {
          Some(parts(1))
        } else {
          None
        }
      }

    tokenOpt match {
      case Some(token) =>
        authService.validateToken(token).flatMap {
          case Right(claims) =>
            val username = (claims \ "username").as[String]
            val newToken = authService.generateToken(username)
            Future.successful(Ok(Json.obj(
              "success" -> true,
              "token" -> newToken,
              "expiresIn" -> 86400
            )))
          case Left(error) =>
            Future.successful(Unauthorized(Json.obj(
              "success" -> false,
              "error" -> error
            )))
        }
      case None =>
        Future.successful(BadRequest(Json.obj(
          "success" -> false,
          "error" -> "Token no proporcionado"
        )))
    }
  }
}
