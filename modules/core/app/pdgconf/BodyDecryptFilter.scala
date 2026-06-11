package pdgconf

import java.util.Base64
import javax.inject.Inject
import scala.concurrent.ExecutionContext

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.util.ByteString
import play.api.Logger
import play.api.mvc.{EssentialAction, EssentialFilter, RequestHeader}

import security.{AuthService, CryptoService}

class BodyDecryptFilter @Inject() (authService: AuthService, cryptoService: CryptoService)
    (using ec: ExecutionContext, mat: Materializer) extends EssentialFilter:

  private val logger = Logger(this.getClass)

  override def apply(next: EssentialAction): EssentialAction = EssentialAction { request =>
    val userNameOpt = request.headers.get("X-USER")
      .orElse(request.session.get("X-USER"))

    val contentType = request.contentType.getOrElse("")
    val isEncryptedBody = !contentType.contains("multipart/form-data") &&
                          !contentType.contains("application/octet-stream")

    val credentialsOpt = for
      userName    <- userNameOpt
      credentials <- authService.getCredentials(userName)
      if !authService.isPublicResource(request.uri)
      if isEncryptedBody
    yield credentials

    credentialsOpt match
      case Some(credentials) =>
        next(request).through(
          Flow[ByteString]
            .fold(ByteString.empty)(_ ++ _)
            .map { fullBody =>
              if fullBody.isEmpty then fullBody
              else
                val decoded   = Base64.getUrlDecoder.decode(fullBody.toArray)
                val decrypted = cryptoService.decrypt(decoded, credentials)
                logger.trace(s"body decrypted for ${request.uri}")
                ByteString(decrypted)
            }
        )
      case None =>
        next(request)
  }
