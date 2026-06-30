package pdgconf

import org.apache.commons.codec.binary.Base64

import javax.inject.Inject
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee
import play.api.mvc.EssentialAction
import play.api.mvc.EssentialFilter
import play.api.mvc.RequestHeader
import security.CryptoService
import security.AuthService

class BodyDecryptFilter @Inject() (authService: AuthService, cryptoService: CryptoService) extends EssentialFilter {

  val logger: Logger = Logger(this.getClass())

  def apply(next: EssentialAction): EssentialAction = new EssentialAction {

    def apply(request: RequestHeader) = {

      request.headers
        .get("X-USER")
        .fold {
          next(request)
        } { userName =>
          val credentials = authService.getCredentials(userName).get
          // Accumulate ALL body bytes before decoding to avoid Base64 group
          // misalignment when Netty delivers the body in multiple chunks.
          Iteratee.fold[Array[Byte], Array[Byte]](Array.empty[Byte]) { (acc, chunk) => acc ++ chunk }
            .flatMap { allBytes =>
              if (allBytes.nonEmpty) {
                val decoded = Base64.decodeBase64(new String(allBytes, "UTF-8").replace('_', '/'))
                val decrypted = cryptoService.decrypt(decoded, credentials)
                Iteratee.flatten(Enumerator(decrypted) |>> next(request))
              } else {
                Iteratee.flatten(Enumerator.empty[Array[Byte]] |>> next(request))
              }
            }
        }
    }

  }

}