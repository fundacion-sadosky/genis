package pdgconf

import org.apache.commons.codec.binary.Base64

import javax.inject.Inject
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Enumeratee
import play.api.mvc.EssentialAction
import play.api.mvc.EssentialFilter
import play.api.mvc.RequestHeader
import security.CryptoService
import security.AuthService

class BodyDecryptFilter @Inject() (authService: AuthService, cryptoService: CryptoService) extends EssentialFilter {

  val logger: Logger = Logger(this.getClass())

  def apply(next: EssentialAction): EssentialAction = new EssentialAction {

    def apply(request: RequestHeader) = {

      val base64Decoder = Enumeratee.map[Array[Byte]] { bytes => Base64.decodeBase64(bytes) }

      request.headers
        .get("X-USER")
        .fold {
          next(request) // no user header, do nothing?! pass to next?
        } { userName =>
          val credentials = authService.getCredentials(userName).get
          base64Decoder &>> (cryptoService.decryptEnumeratee(credentials) &>> next(request))
        }
    }

  }

}