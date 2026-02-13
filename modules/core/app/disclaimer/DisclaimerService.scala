package disclaimer

import scala.concurrent.Future

import scala.concurrent.Future

trait DisclaimerService {
  def get(): Future[Disclaimer]
}

class DisclaimerServiceImpl(repository: DisclaimerRepository) extends DisclaimerService {
  override def get(): Future[Disclaimer] = {
    repository.get()
  }
}
