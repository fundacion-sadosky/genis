
package disclaimer

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

abstract class DisclaimerService {
  def get(): Future[Disclaimer]
}

@Singleton
class DisclaimerServiceImpl @Inject()(disclaimerRepository: DisclaimerRepository)(implicit ec: ExecutionContext)
  extends DisclaimerService {

  override def get(): Future[Disclaimer] = {
    disclaimerRepository.get()
  }
}
