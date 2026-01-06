package filters

import javax.inject._
import play.api.mvc._
import play.api.libs.json.Json
import scala.concurrent.{ExecutionContext, Future}
import services.AuthServiceV2

/**
 * Middleware para validar JWT tokens en requests
 * Los controladores pueden usar AuthFilter.validateTokenFromRequest para verificar tokens
 */
@Singleton
class AuthFilter @Inject()(
    authService: AuthServiceV2
)(implicit ec: ExecutionContext) {

  /**
   * Rutas públicas que no requieren autenticación
   */
  private val publicPaths = Set(
    "/api/health",
    "/api/health/db",
    "/api/auth/login",
    "/api/v2/auth/login"
  )

  /**
   * Verifica si una ruta es pública
   */
  def isPublicPath(path: String): Boolean = {
    publicPaths.exists(publicPath => path.startsWith(publicPath))
  }

  /**
   * Valida un token JWT extraído del header Authorization
   */
  def validateTokenFromRequest(request: RequestHeader): Future[Either[String, String]] = {
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
          case Right(_) => Right("valid")
          case Left(error) => Left(error)
        }
      case None =>
        Future.successful(Left("Token no proporcionado en Authorization header"))
    }
  }

  /**
   * Crea una respuesta JSON de error de autenticación
   */
  def unauthorizedResponse(error: String): Result = {
    Results.Unauthorized(Json.obj(
      "error" -> "No autorizado",
      "message" -> error
    ))
  }
}




