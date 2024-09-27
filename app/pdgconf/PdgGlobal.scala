package pdgconf

import com.google.inject.{Guice, Injector}
import matching.{MatchingCalculatorService, MatchingService}
import org.jboss.netty.handler.codec.http.QueryStringDecoder
import play.api._
import play.api.http.HeaderNames
import play.api.mvc.Results._
import play.api.mvc._
import play.filters.gzip.GzipFilter
import security.AuthService
import types.TotpToken

import java.net.URI
import scala.concurrent.Future
import scala.util.{Failure, Success}

object Filters {
  val gzipFilter = new GzipFilter(
    shouldGzip = (request, _) => {
      val isAssets = !request.path.startsWith("/assets/")
      val isEventStream = request.headers.get(HeaderNames.ACCEPT).exists { _ == "text/event-stream" }
      !isAssets || !isEventStream
    })
}

object PdgGlobal extends WithFilters(Filters.gzipFilter) {

  val logger: Logger = Logger(this.getClass())

  var injector: Injector = null

  override def doFilter(action: EssentialAction): EssentialAction = {
    val decryptFilter = injector.getInstance(classOf[BodyDecryptFilter])
    val logginFilter = injector.getInstance(classOf[LogginFilter])
    val filteredAction = play.api.mvc.Filters(action, decryptFilter, logginFilter)
    super.doFilter(filteredAction)
  }

  override def onRequestReceived(request: RequestHeader): (RequestHeader, Handler) = {

    def unauthorizedHandler(msg: String) = Action(BodyParsers.parse.empty) { req =>
      Results.Unauthorized(msg)
    }

    def forbiddenHandler(msg: String) = Action(BodyParsers.parse.empty) { req =>
      Results.Forbidden(msg)
    }

    def serverErrorHandler(msg: String) = Action(BodyParsers.parse.empty) { req =>
      Results.InternalServerError(msg)
    }

    def badRequestHandler(msg: String) = Action(BodyParsers.parse.empty) { req =>
      Results.BadRequest(msg)
    }

    val authService = injector.getInstance(classOf[AuthService])

    logger.trace("request path " + request.path)

    val userNameOpt = request.headers
      .get("X-USER")
      .orElse {
        request.session.get("X-USER")
      }

    val totpFromReq = request.headers
      .get("X-TOTP")
      .map { TotpToken(_) }

    var requestHeader = authService.verifyAndDecryptRequest(request.uri, request.method, userNameOpt, totpFromReq)
    val requestHeaderInferiorInstance = authService.verifyInferiorInstance(request)
    requestHeaderInferiorInstance match {
      case Success(_) => ()
      case Failure(_) => requestHeader = requestHeaderInferiorInstance
    }
    requestHeader match {
      case Success(decryptedRequest) =>
        val decryptedRequestHeader = {
          // From: framework/src/play-netty-server/src/main/scala/play/core/server/netty/PlayDefaultUpstreamHandler.scala
          val nettyUri = new QueryStringDecoder(decryptedRequest);
          import scala.collection.JavaConverters._
          val parameters = Map.empty[String, Seq[String]] ++ nettyUri.getParameters.asScala.mapValues(_.asScala)

          request.copy(
            path = new URI(nettyUri.getPath).getRawPath,
            queryString = parameters,
            uri = decryptedRequest)
        }

        logger.trace(s"forwarded as $decryptedRequestHeader with TOTP $totpFromReq")
        logger.trace(" decryptedRequestHeader is " + decryptedRequestHeader)
        logger.trace(" path is " + decryptedRequestHeader.path)
        logger.trace(" uri is " + decryptedRequestHeader.uri)
        logger.trace(" queryStrkng " + decryptedRequestHeader.queryString)

        super.onRequestReceived(decryptedRequestHeader)

      case Failure(e: NoSuchElementException) => (request, unauthorizedHandler(e.getMessage()))
      case Failure(e: IllegalAccessException) => e.getMessage match {
        case "Non public reosurces request must have 'X-USER' header" => (request, badRequestHandler(e.getMessage()))
        case msg => (request, forbiddenHandler(msg))
      }

      case Failure(e) => (request, serverErrorHandler(e.getMessage()))
    }
  }

  override def onStart(app: Application): Unit = {
    val pdgModule = new PdgModule(app)
    injector = Guice.createInjector(pdgModule)
    val matchingService = injector.getInstance(classOf[MatchingService])
    val matchingCalculatorService = injector.getInstance(classOf[MatchingCalculatorService])
    matchingService.resumeFindingMatches()
    matchingCalculatorService.updateLrDefault()
}

  override def onError(request: RequestHeader, ex: Throwable) = {
    Play.maybeApplication.map {
      case app if app.mode == Mode.Prod =>
        Future.successful(InternalServerError(s"Ha ocurrido un error loggeado con id @${ex.asInstanceOf[PlayException].id}"))
      case app => Future.successful(InternalServerError(ex.getMessage))
    }.getOrElse(Future.successful(InternalServerError))
  }

  /**
   * Controllers must be resolved through the application context. There is a special method of GlobalSettings
   * that we can override to resolve a given controller. This resolution is required by the Play router.
   */
  def getControllerInstance[A](controllerClass: Class[A]): A = injector.getInstance(controllerClass)
  //override def getControllerInstance[A](controllerClass: Class[A]): A = injector.getInstance(controllerClass)
}
