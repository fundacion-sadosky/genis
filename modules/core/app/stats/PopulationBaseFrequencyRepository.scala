package stats

import scala.concurrent.Future

trait PopulationBaseFrequencyRepository:
  def add(populationBase: PopulationBaseFrequency): Future[Option[Int]]
  def getByName(name: String): Future[Option[PopulationBaseFrequency]]
  def getAll(): Future[Seq[PopulationSampleFrequency]]
  def getAllNames(): Future[Seq[(String, Double, String, Boolean, Boolean)]]
  def toggleStatePopulationBaseFrequency(name: String): Future[Option[Int]]
  def setAsDefault(name: String): Future[Int]
