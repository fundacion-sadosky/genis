package disclaimer

import javax.inject.{Inject, Singleton}

import scala.concurrent.Future

abstract class DisclaimerRepository {
  def get(): Future[Disclaimer]
}

@Singleton
class SlickDisclaimerRepository @Inject() extends DisclaimerRepository {
  // TODO: Implement DB access. Placeholder for compilation.
  override def get(): Future[Disclaimer] = Future.successful(Disclaimer(Some("Disclaimer placeholder")))
}
