package services

import models.configdata.{Crime, CrimeRow, CrimeType, CrimeTypeRow}
import repositories.CrimeTypeRepository
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Servicio para gestionar tipos de crimen y crímenes.
 * Proporciona una capa de abstracción sobre el repositorio.
 */
@Singleton
class CrimeTypeService @Inject() (
  crimeTypeRepository: CrimeTypeRepository
)(implicit ec: ExecutionContext) {

  /**
   * Lista todos los tipos de crimen con sus crímenes asociados
   */
  def list(): Future[Seq[CrimeType]] = {
    crimeTypeRepository.list()
  }
  
  /**
   * Lista tipos de crimen como mapa por ID (útil para lookups rápidos)
   */
  def listAsMap(): Future[Map[String, CrimeType]] = {
    list().map(_.map(ct => ct.id -> ct).toMap)
  }

  /**
   * Obtiene un tipo de crimen por su ID
   */
  def findById(id: String): Future[Option[CrimeType]] = {
    crimeTypeRepository.findById(id)
  }

  /**
   * Crea un nuevo tipo de crimen
   */
  def createCrimeType(id: String, name: String, description: Option[String]): Future[CrimeTypeRow] = {
    val crimeTypeRow = CrimeTypeRow(id, name, description)
    crimeTypeRepository.createCrimeType(crimeTypeRow)
  }

  /**
   * Crea un nuevo crimen asociado a un tipo
   */
  def createCrime(
    id: String, 
    crimeTypeId: String, 
    name: String, 
    description: Option[String]
  ): Future[CrimeRow] = {
    val crimeRow = CrimeRow(id, crimeTypeId, name, description)
    crimeTypeRepository.createCrime(crimeRow)
  }

  /**
   * Elimina un tipo de crimen
   */
  def deleteCrimeType(id: String): Future[Boolean] = {
    crimeTypeRepository.deleteCrimeType(id).map(_ > 0)
  }

  /**
   * Elimina un crimen
   */
  def deleteCrime(id: String): Future[Boolean] = {
    crimeTypeRepository.deleteCrime(id).map(_ > 0)
  }

  /**
   * Obtiene los crímenes de un tipo específico
   */
  def getCrimesByType(crimeTypeId: String): Future[Seq[Crime]] = {
    crimeTypeRepository.findCrimesByType(crimeTypeId).map { rows =>
      rows.map(r => Crime(r.id, r.name, r.description))
    }
  }
}
