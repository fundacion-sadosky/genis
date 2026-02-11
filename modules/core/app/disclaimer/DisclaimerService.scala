package disclaimer

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

abstract class DisclaimerService {
  def get(): Future[Disclaimer]
}

@Singleton
class DisclaimerServiceImpl @Inject()(disclaimerRepository: DisclaimerRepository) extends DisclaimerService {
  override def get(): Future[Disclaimer] = {
    disclaimerRepository.get()
  }
}
