package services

import scala.concurrent.{ExecutionContext, Future}
import types.Geneticist
import configdata.GeneticistRepository


trait GeneticistService {
  def add(geneticist: Geneticist): Future[Int]
  def getAll(laboratory: String): Future[Seq[Geneticist]]
  def update(geneticist: Geneticist): Future[Int]
  def get(id: Long): Future[Option[Geneticist]]
}

import javax.inject.{Inject, Singleton}

@Singleton
class GeneticistServiceImpl @Inject() (
  genRepository: GeneticistRepository
)(implicit ec: ExecutionContext) extends GeneticistService {
  override def add(geneticist: Geneticist): Future[Int] = genRepository.add(geneticist)
  override def getAll(laboratory: String): Future[Seq[Geneticist]] = genRepository.getAll(laboratory)
  override def update(geneticist: Geneticist): Future[Int] = genRepository.update(geneticist)
  override def get(id: Long): Future[Option[Geneticist]] = genRepository.get(id)
}
