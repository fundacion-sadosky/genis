package pdgconf

import java.util.Date
import javax.inject.{Inject, Named, Singleton}

import audit.{OperationLogEntryAttemp, OperationLogService}
import play.api.{Logger, Routes}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{EssentialAction, EssentialFilter, RequestHeader}
import security.AuthService
import types.TotpToken

@Singleton
class InterconnectionFilter @Inject()(@Named("genisManifest") manifest: Map[String, String], operationLogService: OperationLogService, authService: AuthService) extends EssentialFilter {

  val logger: Logger = Logger(this.getClass())

  val buildNo = manifest.getOrElse("Git-Head-Rev", "develop")

  def apply(next: EssentialAction): EssentialAction = new EssentialAction {

    def apply(request: RequestHeader) = next(request).map { result =>
      if (!authService.isPublicResource(request.path) || request.path.startsWith("/login")) {

        val userName = request.headers
          .get("X-USER")
          .orElse { request.session.get("X-USER") }
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