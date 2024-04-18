package pedigree
import java.sql.Timestamp
import java.util.Calendar

import javax.inject.{Inject, Singleton}
import models.Tables
import models.Tables.{CourtCaseFiliationDataRow, CourtCaseRow, PedigreeRow}
import motive.{Motive, MotiveType}
import play.api.{Application, Logger}
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB
import play.api.i18n.Messages
import play.api.libs.json.Json
import types.{SampleCode, Sex}
import util.{DefaultDb, Transaction}
import pedigree.MutationDefaultParam
import scala.slick.jdbc.{StaticQuery => Q}
import scala.concurrent.Future
import scala.math.BigDecimal.RoundingMode
import scala.math.BigDecimal.RoundingMode.RoundingMode
import scala.slick.lifted.Compiled
abstract class MutationRepository extends DefaultDb with Transaction {
  def getAllMutationModelType():Future[List[MutationModelType]]
  def getAllMutationModels():Future[List[MutationModel]]
  def getAllLocusAlleles():Future[List[(String,Double)]]
  def getActiveMutationModels():Future[List[MutationModel]]
  def insertMutationModel(row: MutationModelFull): Future[Either[String, Long]]
  def updateMutationModel(fullMutationModel: MutationModelFull): Future[Either[String, Unit]]
  def deleteMutationModelById(id: Long): Future[Either[String, Unit]]
  def getMutationModel(id: Option[Long]) : Future[Option[MutationModel]]
  def getMutatitionModelParameters(idMutationModel: Long) : Future[List[MutationModelParameter]]
  def getMutatitionModelData(idMutationModel: Long, mutationModelType: Long,markers:scala.List[String]) : Future[List[(MutationModelParameter, List[MutationModelKi])]]
  def insertParameters(parameterList: List[MutationModelParameter]): Future[Either[String, Unit]]
  def insertKi(kiList:List[MutationModelKi]): Future[Either[String, Unit]]
  def deleteMutationModelKiByIdMutationModel(id: Long): Future[Either[String, Unit]]
  def getAllMutationDefaultParameters():Future[List[MutationDefaultParam]]
  def insertLocusAlleles(parameterList: List[(String,Double)]): Future[Either[String, Int]]
}
@Singleton
class SlickMutationRepository @Inject() (implicit app: Application) extends MutationRepository {
  val mutationModelTable: TableQuery[Tables.MutationModel] = Tables.MutationModel
  val mutationModelTypeTable: TableQuery[Tables.MutationModelType] = Tables.MutationModelType
  val mutationModelParameterTable: TableQuery[Tables.MutationModelParameter] = Tables.MutationModelParameter
  val mutationModelKiTable: TableQuery[Tables.MutationModelKi] = Tables.MutationModelKi
  val mutationDefaultParameter: TableQuery[Tables.MutationDefaultParameter] = Tables.MutationDefaultParameter
  val locusAllele: TableQuery[Tables.LocusAllele] = Tables.LocusAllele

  val logger: Logger = Logger(this.getClass())

  private def queryGetMutationModelById(id: Column[Long]) = mutationModelTable.filter(_.id === id)

  val getMutationModelById = Compiled(queryGetMutationModelById _)

  private def queryGetMutationModelParamsById(id: Column[Long]) = mutationModelParameterTable.filter(_.idMutationModel === id)

  val getMutationModelParamsById = Compiled(queryGetMutationModelParamsById _)

  private def queryGetMutationModelKiById(ids: List[Long]) = {
    mutationModelKiTable.filter(_.idMutationModelParameter inSet ids)
  }

  val getActiveMutationModelQuery = Compiled(for (mt <- mutationModelTable.filter(_.active).sortBy(_.id))
    yield (mt.id, mt.name,mt.mutationType,mt.active,mt.ignoreSex, mt.cantSaltos))

  val getAllMutationModelQuery = Compiled(for (mt <- mutationModelTable.sortBy(_.id))
    yield (mt.id, mt.name,mt.mutationType,mt.active,mt.ignoreSex, mt.cantSaltos))

  //ignoro el Stepwise extended y el Equal, solo tomo el Stepwise
  val getAllMutationModelTypeQuery = Compiled(for (mt <- mutationModelTypeTable.filter(_.id === 2L).sortBy(_.id))
    yield (mt.id, mt.description))

  val getAllLocusAllelesQuery = Compiled(for (mt <- locusAllele.sortBy(_.id))
    yield (mt.locus, mt.allele))

  override def getAllLocusAlleles():Future[List[(String,Double)]] = {
    this.runInTransactionAsync { implicit session =>
      getAllLocusAllelesQuery.list
    }
  }
  override def getAllMutationModelType():Future[List[MutationModelType]] =  {
    this.runInTransactionAsync { implicit session =>
      getAllMutationModelTypeQuery.list.map(x => MutationModelType(x._1, x._2))
    }
  }
  override def getActiveMutationModels():Future[List[MutationModel]] = {
    this.runInTransactionAsync { implicit session =>
      getActiveMutationModelQuery.list.map(x => MutationModel(x._1, x._2,x._3,x._4,x._5, x._6))
    }
  }
  override def getAllMutationModels():Future[List[MutationModel]] = {
    this.runInTransactionAsync { implicit session =>
      getAllMutationModelQuery.list.map(x => MutationModel(x._1, x._2,x._3,x._4,x._5, x._6))
    }
  }
  override def insertMutationModel(fullMutationModel: MutationModelFull): Future[Either[String, Long]] = {

    this.runInTransactionAsync { implicit session => {
      try {

        val id = (mutationModelTable returning mutationModelTable.map(_.id)) += models.Tables.MutationModelRow(id = 0,
            name = fullMutationModel.header.name,
            mutationType = fullMutationModel.header.mutationType,
            active = fullMutationModel.header.active,
            ignoreSex = fullMutationModel.header.ignoreSex,
            cantSaltos = fullMutationModel.header.cantSaltos
        )
        fullMutationModel.parameters.foreach(x => {
          (mutationModelParameterTable returning mutationModelParameterTable.map(_.id)) +=
            models.Tables.MutationModelParameterRow(id = 0, id,
              x.locus, x.sex, x.mutationRate, x.mutationRange, x.mutationRateMicrovariant)
        })
        Right(id)
      } catch {
        case e: Exception => {
          logger.error(e.getMessage, e)
          Left(e.getMessage)
        }
      }
    }
    }
  }
  override def insertParameters(parameterList: List[MutationModelParameter]): Future[Either[String, Unit]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        val ids = parameterList.map(x => {
          val id = (mutationModelParameterTable returning mutationModelParameterTable.map(_.id)) +=
            models.Tables.MutationModelParameterRow(id = 0, x.idMutationModel,
              x.locus, x.sex, x.mutationRate, x.mutationRange, x.mutationRateMicrovariant)
          id
        })
        Right(())
      } catch {
        case e: Exception => {
          logger.error(e.getMessage, e)
          Left(e.getMessage)
        }
      }
    }
    }
  }
  override def insertLocusAlleles(locusAlleles: List[(String,Double)]): Future[Either[String, Int]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        val ids = locusAlleles.map(la => {
          locusAllele returning locusAllele.map(_.id) +=
            models.Tables.LocusAllelesRow(0,la._1,la._2)
        })
        Right(ids.size)
      } catch {
        case e: Exception => {
          logger.error(e.getMessage, e)
          Left(e.getMessage)
        }
      }
    }
    }
  }
  override def insertKi(kiList:List[MutationModelKi]): Future[Either[String, Unit]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        val ids = kiList.map(x => {
          val id = (mutationModelKiTable returning mutationModelKiTable.map(_.id)) +=
            models.Tables.MutationModelKiRow(id = 0, x.idMutationModelParameter,
              x.allele, x.ki)
          id
        })
        Right(())
      } catch {
        case e: Exception => {
          logger.error(e.getMessage, e)
          Right(())
        }
      }
    }
    }
  }
  override def updateMutationModel(fullMutationModel: MutationModelFull): Future[Either[String, Unit]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        val mutationModel = fullMutationModel.header
        mutationModelTable.filter(_.id === mutationModel.id)
          .map(x => (x.name,x.mutationType,x.active,x.ignoreSex, x.cantSaltos))
          .update((mutationModel.name,mutationModel.mutationType,mutationModel.active,mutationModel.ignoreSex, mutationModel.cantSaltos))
        fullMutationModel.parameters.filter(_.id>0).foreach(row =>{
          mutationModelParameterTable.filter(_.id === row.id)
            .map(x => (x.mutationRate,x.mutationRange,x.mutationRateMicrovariant))
            .update((row.mutationRate,row.mutationRange,row.mutationRateMicrovariant))
        })
        fullMutationModel.parameters.filter(_.id==0).foreach(x => {
          (mutationModelParameterTable returning mutationModelParameterTable.map(_.id)) +=
            models.Tables.MutationModelParameterRow(id = 0, mutationModel.id,
              x.locus, x.sex, x.mutationRate, x.mutationRange, x.mutationRateMicrovariant)
        })
        Right(())
      } catch {
        case e: Exception => {
          logger.error(e.getMessage,e)
          Left(Messages("error.E0500"))
        }
      }
    }
    }
  }
  override def deleteMutationModelById(id: Long): Future[Either[String, Unit]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        val ids = getMutationModelParamsById(id).list.map(_.id)
        Compiled(queryGetMutationModelKiById(ids)).delete
        getMutationModelParamsById(id).delete
        getMutationModelById(id).delete
        Right(())
      } catch {
        case e: Exception => {
          logger.error(e.getMessage, e)
          Left(e.getMessage)
        }
      }
    }
    }
  }
  override def deleteMutationModelKiByIdMutationModel(id: Long): Future[Either[String, Unit]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        val ids = getMutationModelParamsById(id).list.map(_.id)
        Compiled(queryGetMutationModelKiById(ids)).delete
        Right(())
      } catch {
        case e: Exception => {
          logger.error(e.getMessage, e)
          Left(e.getMessage)
        }
      }
    }
    }
  }

  override def getMutationModel(id: Option[Long]): Future[Option[MutationModel]] = {
    id match {
      case None => return Future.successful(None)
      case Some(idMut) => {
        this.runInTransactionAsync { implicit session => {
          mutationModelTable.filter(_.id === idMut).firstOption.map { x =>
            MutationModel(x.id, x.name, x.mutationType, x.active,x.ignoreSex, x.cantSaltos)
          }
         }
        }
      }
    }
   }

  override def getMutatitionModelData(idMutationModel: Long, mutationModelType: Long,markers:scala.List[String]) : Future[List[(MutationModelParameter, List[MutationModelKi])]] = {
    this.runInTransactionAsync { implicit session => {
      mutationModelParameterTable.filter(_.idMutationModel === idMutationModel).filter(_.locus inSet markers).list.map { x =>
        mutationModelType match {
            // TODO agregar los datos q se necesiten para nuevos modelos
            // TODO filtrar por locus del pedigree, q se reciban de parametro
          case 1 /*Modelo Equals*/ => (MutationModelParameter(x.id, x.idMutationModel, x.locus, x.sex, x.mutationRate, x.mutationRange, x.mutationRateMicrovariant), List())
          case 2 /*Modelo StepWise*/ => {
            (MutationModelParameter(x.id, x.idMutationModel, x.locus, x.sex, x.mutationRate, x.mutationRange, x.mutationRateMicrovariant),
              mutationModelKiTable.filter(_.idMutationModelParameter === x.id).list.map(ki => MutationModelKi(ki.id, ki.idMutationModelParameter, ki.allele, ki.ki)))
          }
        }
       }
      }
    }

    }

  override def getMutatitionModelParameters(idMutationModel: Long) : Future[List[MutationModelParameter]] = {
    this.runInTransactionAsync { implicit session => {
      mutationModelParameterTable.filter(_.idMutationModel === idMutationModel).sortBy(_.locus).list.map { x =>
         MutationModelParameter(x.id, x.idMutationModel, x.locus, x.sex, x.mutationRate, x.mutationRange, x.mutationRateMicrovariant)
        }
      }
    }
  }

  override def getAllMutationDefaultParameters():Future[List[MutationDefaultParam]] = {
    this.runInTransactionAsync { implicit session => {
      mutationDefaultParameter.sortBy(_.locus).list.map { x =>
        MutationDefaultParam(x.locus, x.sex, x.mutationRate)
      }
    }
    }
  }

}