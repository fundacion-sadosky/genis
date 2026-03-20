package probability

import types.StatOption

import scala.concurrent.Future

trait ProbabilityService {
  def getStats(laboratory: String): Future[Option[StatOption]]
  def calculateContributors(analysis: profile.Analysis, category: types.AlphanumericId, stats: StatOption): Future[Int]
}

@javax.inject.Singleton
class ProbabilityServiceStub extends ProbabilityService {
  override def getStats(laboratory: String): Future[Option[StatOption]] = Future.successful(None)
  override def calculateContributors(analysis: profile.Analysis, category: types.AlphanumericId, stats: StatOption): Future[Int] = Future.successful(0)
}

class NoFrequencyException(message: String) extends RuntimeException(message)
