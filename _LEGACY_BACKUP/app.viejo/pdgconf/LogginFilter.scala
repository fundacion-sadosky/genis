package pdgconf

import java.util.Date
import audit.OperationLogEntryAttemp
import audit.OperationLogService
import javax.inject.Inject
import javax.inject.Singleton
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.EssentialAction
import play.api.mvc.EssentialFilter
import play.api.mvc.RequestHeader
import javax.inject.Named
import types.TotpToken
import play.api.Routes
import security.AuthService

@Singleton
class LogginFilter @Inject() (@Named("genisManifest") manifest: Map[String, String], operationLogService: OperationLogService, authService: AuthService) extends EssentialFilter {

  val logger: Logger = Logger(this.getClass())

  val buildNo = manifest.getOrElse("Git-Head-Rev", "develop")

  def apply(next: EssentialAction): EssentialAction = new EssentialAction {

    def apply(request: RequestHeader) = next(request).map { result =>
      if (!(authService.isPublicResource(request.path) && !authService.isInterconnectionResource(request.path))|| request.path.startsWith("/login")) {
        var fieldUser = "X-USER";

        if(authService.isInterconnectionResource(request.path)){
          fieldUser = "X-URL-INSTANCIA-INFERIOR"
        }

        val userName = request.headers
          .get(fieldUser)
          .orElse { request.session.get(fieldUser) }
          .getOrElse("ANONYMOUS")


        val otp = request.headers
          .get("X-TOTP")
          .map { TotpToken(_) }

        val tags = request.tags.withDefault(_ => "NotFound")

        val action: String = tags(Routes.ROUTE_CONTROLLER) + "." + tags(Routes.ROUTE_ACTION_METHOD) + "()"
        val resultCode = result.header.headers.get("X-CREATED-ID")

        operationLogService.add(OperationLogEntryAttemp(
          userName,
          otp,
          new Date,
          request.method,
          request.path,
          action,
          buildNo,
          resultCode,
          result.header.status
          ))

      }
      result
    }
  }

}