package pdgconf

import java.net.URLDecoder
import javax.inject.{Inject, Provider, Singleton}

import scala.util.{Failure, Success}

import play.api.{Logger, OptionalDevContext}
import play.api.http.{DefaultHttpRequestHandler, HttpConfiguration, HttpErrorHandler, HttpFilters}
import play.api.libs.streams.Accumulator
import play.api.mvc.{EssentialAction, Handler, RequestHeader, Results}
import play.api.mvc.request.RequestTarget
import play.api.routing.Router
import play.core.WebCommands

import security.AuthService
import types.TotpToken

/** Descifra la URL encriptada por el frontend Angular antes del routing.
 *
 *  El frontend encripta cada URL con AES-CBC usando las credenciales del
 *  usuario logueado (base64 URL-safe). Este handler replica el comportamiento
 *  de `PdgGlobal.onRequestReceived` del stack legacy.
 */
@Singleton
class UrlDecryptRequestHandler @Inject()(
  webCommands: WebCommands,
  optDevContext: OptionalDevContext,
  router: Provider[Router],
  errorHandler: HttpErrorHandler,
  configuration: HttpConfiguration,
  filters: HttpFilters,
  authService: AuthService
) extends DefaultHttpRequestHandler(webCommands, optDevContext, router, errorHandler, configuration, filters):

  private val logger = Logger(this.getClass)

  override def handlerForRequest(request: RequestHeader): (RequestHeader, Handler) =
    val userNameOpt = request.headers.get("X-USER")
      .orElse(request.session.get("X-USER"))
    val totpOpt = request.headers.get("X-TOTP").map(TotpToken(_))

    authService.verifyAndDecryptRequest(request.uri, request.method, userNameOpt, totpOpt) match
      case Success(decryptedUri) =>
        logger.trace(s"url decrypted: ${request.uri} -> $decryptedUri")
        val qIdx     = decryptedUri.indexOf('?')
        val path     = if qIdx < 0 then decryptedUri else decryptedUri.substring(0, qIdx)
        val rawQuery = if qIdx < 0 then null else decryptedUri.substring(qIdx + 1)
        val target   = RequestTarget(decryptedUri, path, parseQueryString(rawQuery))
        super.handlerForRequest(request.withTarget(target))

      case Failure(e: java.util.NoSuchElementException) =>
        (request, EssentialAction(_ => Accumulator.done(Results.Unauthorized(e.getMessage))))
      case Failure(e: IllegalAccessException) if e.getMessage == "Non public reosurces request must have 'X-USER' header" =>
        (request, EssentialAction(_ => Accumulator.done(Results.BadRequest(e.getMessage))))
      case Failure(e: IllegalAccessException) =>
        (request, EssentialAction(_ => Accumulator.done(Results.Forbidden(e.getMessage))))
      case Failure(e) =>
        logger.error(s"error processing request ${request.uri}", e)
        (request, EssentialAction(_ => Accumulator.done(Results.InternalServerError(e.getMessage))))

  private def parseQueryString(rawQuery: String | Null): Map[String, Seq[String]] =
    if rawQuery == null then Map.empty
    else
      rawQuery.split("&").foldLeft(Map.empty[String, Seq[String]]) { (acc, param) =>
        val eqIdx = param.indexOf('=')
        if eqIdx < 0 then
          acc + (URLDecoder.decode(param, "UTF-8") -> Seq.empty)
        else
          val key   = URLDecoder.decode(param.substring(0, eqIdx), "UTF-8")
          val value = URLDecoder.decode(param.substring(eqIdx + 1), "UTF-8")
          acc + (key -> (acc.getOrElse(key, Seq.empty) :+ value))
      }