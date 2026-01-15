package repositories

import models.configdata.{Crime, CrimeRow, CrimeType, CrimeTypeRow}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CrimeTypeRepository @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) {
  
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig.profile.api._
  import dbConfig.db

  // ============================================
  // Table Definitions
  // ============================================
  
  private class CrimeTypesTable(tag: Tag) extends Table[CrimeTypeRow](tag, "crime_type") {
    def id = column[String]("id", O.PrimaryKey)
    def name = column[String]("name")
    def description = column[Option[String]]("description")
    
    def * = (id, name, description).mapTo[CrimeTypeRow]
  }
  
  private class CrimesTable(tag: Tag) extends Table[CrimeRow](tag, "crime_involved") {
    def id = column[String]("id", O.PrimaryKey)
    def crimeTypeId = column[String]("crime_type_id")
    def name = column[String]("name")
    def description = column[Option[String]]("description")
    
    def * = (id, crimeTypeId, name, description).mapTo[CrimeRow]
    
    def crimeType = foreignKey("fk_crime_type", crimeTypeId, crimeTypes)(_.id)
  }
  
  private val crimeTypes = TableQuery[CrimeTypesTable]
  private val crimes = TableQuery[CrimesTable]

  // ============================================
  // Repository Methods
  // ============================================
  
  /**
   * Obtiene todos los tipos de crimen con sus crímenes asociados
   */
  def list(): Future[Seq[CrimeType]] = db.run {
    val query = for {
      (ct, c) <- crimeTypes joinLeft crimes on (_.id === _.crimeTypeId)
    } yield (ct, c)
    
    query.result.map { results =>
      results
        .groupBy(_._1)
        .map { case (crimeTypeRow, grouped) =>
          val crimesList = grouped.flatMap(_._2).map { crimeRow =>
            Crime(crimeRow.id, crimeRow.name, crimeRow.description)
          }
          CrimeType(
            id = crimeTypeRow.id,
            name = crimeTypeRow.name,
            description = crimeTypeRow.description,
            crimes = crimesList
          )
        }
        .toSeq
    }
  }
  
  /**
   * Obtiene un tipo de crimen por ID con sus crímenes asociados
   */
  def findById(id: String): Future[Option[CrimeType]] = db.run {
    val query = for {
      (ct, c) <- crimeTypes.filter(_.id === id) joinLeft crimes on (_.id === _.crimeTypeId)
    } yield (ct, c)
    
    query.result.map { results =>
      results.headOption.map { _ =>
        val crimeTypeRow = results.head._1
        val crimesList = results.flatMap(_._2).map { crimeRow =>
          Crime(crimeRow.id, crimeRow.name, crimeRow.description)
        }
        CrimeType(
          id = crimeTypeRow.id,
          name = crimeTypeRow.name,
          description = crimeTypeRow.description,
          crimes = crimesList
        )
      }
    }
  }
  
  /**
   * Crea un nuevo tipo de crimen
   */
  def createCrimeType(crimeType: CrimeTypeRow): Future[CrimeTypeRow] = db.run {
    (crimeTypes += crimeType).map(_ => crimeType)
  }
  
  /**
   * Crea un nuevo crimen asociado a un tipo
   */
  def createCrime(crime: CrimeRow): Future[CrimeRow] = db.run {
    (crimes += crime).map(_ => crime)
  }
  
  /**
   * Elimina un tipo de crimen por ID (también elimina crímenes asociados por FK)
   */
  def deleteCrimeType(id: String): Future[Int] = db.run {
    crimeTypes.filter(_.id === id).delete
  }
  
  /**
   * Elimina un crimen por ID
   */
  def deleteCrime(id: String): Future[Int] = db.run {
    crimes.filter(_.id === id).delete
  }
  
  /**
   * Obtiene solo los tipos de crimen (sin crímenes anidados)
   */
  def listCrimeTypesOnly(): Future[Seq[CrimeTypeRow]] = db.run {
    crimeTypes.result
  }
  
  /**
   * Obtiene crímenes por tipo de crimen
   */
  def findCrimesByType(crimeTypeId: String): Future[Seq[CrimeRow]] = db.run {
    crimes.filter(_.crimeTypeId === crimeTypeId).result
  }
}
