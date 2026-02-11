package disclaimer

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

abstract class DisclaimerRepository {
  def get(): Future[Disclaimer]
}

// Implementación real debe adaptarse a la infraestructura de datos del módulo core
@Singleton
class SlickDisclaimerRepository @Inject() extends DisclaimerRepository {
  override def get(): Future[Disclaimer] = {
    // TODO: Implementar acceso a datos real
    Future.successful(Disclaimer(Some("Disclaimer migrado")))
  }
}
