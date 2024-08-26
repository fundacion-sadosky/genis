package laboratories

import scala.concurrent.Future
import javax.inject.Inject
import javax.inject.Singleton

abstract class GeneticistService {
  def add(geneticist: Geneticist): Future[Int]
  def getAll(laboratory: String): Future[Seq[Geneticist]]
  def update(geneticist: Geneticist): Future[Int]
  def get(id: Long): Future[Option[Geneticist]]
}

@Singleton
class GeneticistServiceImpl @Inject() (genRepository: GeneticistRepository) extends GeneticistService {
 
  override def add(geneticist: Geneticist): Future[Int] = {
    genRepository.add(geneticist)
  }
  
  override def getAll(laboratory: String): Future[Seq[Geneticist]] = {
    genRepository.getAll(laboratory)
  }
  
  override def update(geneticist: Geneticist): Future[Int] = {
    genRepository.update(geneticist)
  }
  
  override def get(id: Long): Future[Option[Geneticist]] = {
    genRepository.get(id)
  }
  
}