package controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import services.AuthServiceV2

/**
 * Controlador de Autenticación Moderno
 * Soporta LDAP e JWT para autenticación y autorización
 */
@Singleton
class AuthController @Inject()(
    cc: ControllerComponents,
    authService: AuthServiceV2
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  /**
   * Endpoint de login
   * POST /api/auth/login
   * Body: {"username": "usuario", "password": "contraseña"}
   */
  def login(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    val usernameOpt = (request.body \ "username").asOpt[String]
    val passwordOpt = (request.body \ "password").asOpt[String]

    (usernameOpt, passwordOpt) match {
      case (Some(username), Some(password)) =>
        authService.authenticate(username, password).map {
          case Right(token) =>
            Ok(Json.obj(
              "success" -> true,
              "token" -> token,
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
    Future.successful(Ok(Json.obj(
      "success" -> true,
      "message" -> "Sesión cerrada"
    )))
  }

  /**
   * Endpoint para validar token
   * GET /api/auth/validate
   */
  def validateToken(): Action[AnyContent] = Action.async { implicit request =>
    val tokenOpt = request.headers.get("Authorization")
      .flatMap(_.stripPrefix("Bearer ").split(" ").headOption)

    tokenOpt match {
      case Some(token) =>
        authService.validateToken(token).map {
          case Right(user) =>
            Ok(Json.obj(
              "valid" -> true,
              "user" -> user
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
          "error" -> "Token no proporcionado"
        )))
    }
  }
}
