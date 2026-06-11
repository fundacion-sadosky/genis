package pdgconf

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

import play.api.mvc.{EssentialAction, EssentialFilter}

/** Agrega X-Genis-Backend: core a todas las respuestas del módulo modern.
 *  Visible en DevTools → Network → Response Headers → permite saber en tiempo real
 *  qué backend sirvió cada request.
 */
@Singleton
class BackendIdentityFilter @Inject()()(using ec: ExecutionContext) extends EssentialFilter:
  override def apply(next: EssentialAction): EssentialAction =
    EssentialAction { rh =>
      next(rh).map(_.withHeaders("X-Genis-Backend" -> "core"))
    }