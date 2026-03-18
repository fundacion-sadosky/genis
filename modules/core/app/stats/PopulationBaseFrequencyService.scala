package stats

import scala.concurrent.Future
import java.io.File

trait PopulationBaseFrequencyService:
  def save(popBaseFreq: PopulationBaseFrequency): Future[Int]
  def getByNamePV(name: String): Future[PopulationBaseFrequencyView]
  def getByName(name: String): Future[Option[PopulationBaseFrequency]]
  def getAllNames(): Future[Seq[PopulationBaseFrequencyNameView]]
  def toggleStateBase(name: String): Future[Option[Int]]
  def setAsDefault(name: String): Future[Int]
  def parseFile(name: String, theta: Double, model: ProbabilityModel, csvFile: File): Future[PopBaseFreqResult]
  def insertFmin(id: String, fmins: Fmins): Future[PopBaseFreqResult]
  def getDefault(): Future[Option[PopulationBaseFrequencyNameView]]
  def getAllPossibleAllelesByLocus(): Future[PopulationBaseFrequencyGrouppedByLocus]
