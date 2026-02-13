package disclaimer

import scala.concurrent.Future

import scala.concurrent.Future

trait DisclaimerRepository {
  def get(): Future[Disclaimer]
}

class DisclaimerRepositoryImpl extends DisclaimerRepository {
  override def get(): Future[Disclaimer] = {
    // TODO: Reemplazar por lógica real
    Future.successful(Disclaimer(Some("Texto de ejemplo del disclaimer (placeholder)")))
  }
}
