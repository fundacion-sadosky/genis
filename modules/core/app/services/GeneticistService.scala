package services

import scala.concurrent.{ExecutionContext, Future}
import types.Geneticist
import configdata.GeneticistRepository


trait GeneticistService {
  def add(geneticist: Geneticist): Future[Either[String, Int]]
  def getAll(laboratory: String): Future[Seq[Geneticist]]
  def update(geneticist: Geneticist): Future[Int]
  def get(id: Long): Future[Option[Geneticist]]
}

import javax.inject.{Inject, Singleton}
import org.postgresql.util.PSQLException
import play.api.Logger

@Singleton
class GeneticistServiceImpl @Inject() (
  genRepository: GeneticistRepository
)(implicit ec: ExecutionContext) extends GeneticistService {

  private val logger = Logger(classOf[GeneticistServiceImpl])

  override def add(geneticist: Geneticist): Future[Either[String, Int]] =
    genRepository.add(geneticist).map(Right(_)).recover {
      case psql: PSQLException =>
        logger.error(s"Error adding geneticist: ${psql.getMessage}", psql)
        psql.getSQLState match
          case "23505" => Left("Nombre ya utilizado en el Laboratorio")
          case _       => Left("Error inesperado en la base de datos")
    }

  override def getAll(laboratory: String): Future[Seq[Geneticist]] = genRepository.getAll(laboratory)
  override def update(geneticist: Geneticist): Future[Int] = genRepository.update(geneticist)
  override def get(id: Long): Future[Option[Geneticist]] = genRepository.get(id)
}
