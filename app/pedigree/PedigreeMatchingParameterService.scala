package pedigree

import javax.inject.{Inject, Singleton}

import scala.concurrent.Future

trait PedigreeMatchingParameterService {
  def getMaxMendelianExclusions: Future[Int]
  def setMaxMendelianExclusions(value: Int): Future[Int]
}

@Singleton
class PedigreeMatchingParameterServiceImpl @Inject() (
  repository: PedigreeMatchingParameterRepository
) extends PedigreeMatchingParameterService {

  def getMaxMendelianExclusions: Future[Int] = repository.getMaxMendelianExclusions

  def setMaxMendelianExclusions(value: Int): Future[Int] = repository.setMaxMendelianExclusions(value)

}