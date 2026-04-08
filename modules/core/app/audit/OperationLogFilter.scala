package audit

import java.util.Date
import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext

import javax.inject.Named

import play.api.Logger
import play.api.mvc.{EssentialAction, EssentialFilter, RequestHeader}
import play.api.routing.Router
import security.AuthService
import types.TotpToken

@Singleton
class OperationLogFilter @Inject()(
  operationLogService: OperationLogService,
  authService: AuthService,
  @Named("buildNo") buildNo: String
)(implicit ec: ExecutionContext) extends EssentialFilter {

  private val logger = Logger(this.getClass)

  override def apply(next: EssentialAction): EssentialAction = EssentialAction { rh =>
    next(rh).map { result =>
      if (shouldLog(rh)) {
        val fieldUser =
          if (authService.isInterconnectionResource(rh.path)) "X-URL-INSTANCIA-INFERIOR"
          else "X-USER"

        val userName = rh.headers.get(fieldUser)
          .orElse(rh.session.get(fieldUser))
          .getOrElse("ANONYMOUS")

        val otp = rh.headers.get("X-TOTP").map(TotpToken(_))

        val handlerDef = rh.attrs.get(Router.Attrs.HandlerDef)
        val action = handlerDef
          .map(hd => s"${hd.controller}.${hd.method}()")
          .getOrElse("NotFound")

        val resultCode = result.header.headers.get("X-CREATED-ID")

        operationLogService.add(
          OperationLogEntryAttemp(
            userId    = userName,
            otp       = otp,
            timestamp = new Date(),
            method    = rh.method,
            path      = rh.path,
            action    = action,
            buildNo   = buildNo,
            result    = resultCode,
            status    = result.header.status
          )
        )
      }
      result
    }
  }

  /** Log unless it's a public non-interconnection resource (mirrors legacy LogginFilter logic). */
  private def shouldLog(rh: RequestHeader): Boolean = {
    val pub  = authService.isPublicResource(rh.path)
    val icon = authService.isInterconnectionResource(rh.path)
    !pub || icon || rh.path.startsWith("/login") || rh.path.startsWith("/api/v2/login")
  }
}
