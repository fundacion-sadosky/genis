package services

import scala.concurrent.Future
import types.Geneticist


trait GeneticistService {
  def add(geneticist: Geneticist): Future[Int]
  def getAll(laboratory: String): Future[Seq[Geneticist]]
  def update(geneticist: Geneticist): Future[Int]
  def get(id: Long): Future[Option[Geneticist]]
}

import javax.inject.Singleton

@Singleton
class GeneticistServiceImpl extends GeneticistService {
  override def add(geneticist: Geneticist): Future[Int] = Future.successful(0)
  override def getAll(laboratory: String): Future[Seq[Geneticist]] = Future.successful(Seq.empty)
  override def update(geneticist: Geneticist): Future[Int] = Future.successful(0)
  override def get(id: Long): Future[Option[Geneticist]] = Future.successful(None)
}
