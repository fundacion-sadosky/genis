package pedigree

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api.*
import slick.dbio.DBIO
import play.api.Logger
import models.Tables

trait MutationRepository:
  def getAllMutationModelType(): Future[List[MutationModelType]]
  def getAllMutationModels(): Future[List[MutationModel]]
  def getAllLocusAlleles(): Future[List[(String, Double)]]
  def getActiveMutationModels(): Future[List[MutationModel]]
  def insertMutationModel(row: MutationModelFull): Future[Either[String, Long]]
  def updateMutationModel(fullMutationModel: MutationModelFull): Future[Either[String, Unit]]
  def deleteMutationModelById(id: Long): Future[Either[String, Unit]]
  def getMutationModel(id: Option[Long]): Future[Option[MutationModel]]
  def getMutatitionModelParameters(idMutationModel: Long): Future[List[MutationModelParameter]]
  def getMutatitionModelData(
    idMutationModel: Long,
    mutationModelType: Long,
    markers: List[String]
  ): Future[List[(MutationModelParameter, List[MutationModelKi])]]
  def insertParameters(parameterList: List[MutationModelParameter]): Future[Either[String, Unit]]
  def insertKi(kiList: List[MutationModelKi]): Future[Either[String, Unit]]
  def deleteMutationModelKiByIdMutationModel(id: Long): Future[Either[String, Unit]]
  def getAllMutationDefaultParameters(): Future[List[MutationDefaultParam]]
  def insertLocusAlleles(parameterList: List[(String, Double)]): Future[Either[String, Int]]

@Singleton
class SlickMutationRepository @Inject()(
  db: slick.jdbc.JdbcBackend.Database
)(implicit ec: ExecutionContext) extends MutationRepository:

  private val logger = Logger(this.getClass)

  private val mutationModelTable          = Tables.MutationModel
  private val mutationModelTypeTable      = Tables.MutationModelType
  private val mutationModelParameterTable = Tables.MutationModelParameter
  private val mutationModelKiTable        = Tables.MutationModelKi
  private val mutationDefaultParameter    = Tables.MutationDefaultParameter
  private val locusAllele                 = Tables.LocusAllele

  // ---------------------------------------------------------------------------
  // Reads
  // ---------------------------------------------------------------------------

  override def getAllLocusAlleles(): Future[List[(String, Double)]] =
    db.run(locusAllele.sortBy(_.id).map(r => (r.locus, r.allele)).result)
      .map(_.toList)

  // Legacy: ignora Stepwise extended y Equal, solo toma el Stepwise (id === 2L)
  override def getAllMutationModelType(): Future[List[MutationModelType]] =
    db.run(
      mutationModelTypeTable
        .filter(_.id === 2L)
        .sortBy(_.id)
        .map(r => (r.id, r.description))
        .result
    ).map(_.toList.map((id, desc) => MutationModelType(id, desc)))

  override def getActiveMutationModels(): Future[List[MutationModel]] =
    db.run(mutationModelTable.filter(_.active).sortBy(_.id).result)
      .map(_.toList.map(r =>
        MutationModel(r.id, r.name, r.mutationType, r.active, r.ignoreSex, r.cantSaltos)
      ))

  override def getAllMutationModels(): Future[List[MutationModel]] =
    db.run(mutationModelTable.sortBy(_.id).result)
      .map(_.toList.map(r =>
        MutationModel(r.id, r.name, r.mutationType, r.active, r.ignoreSex, r.cantSaltos)
      ))

  override def getMutationModel(id: Option[Long]): Future[Option[MutationModel]] =
    id match
      case None => Future.successful(None)
      case Some(idMut) =>
        db.run(mutationModelTable.filter(_.id === idMut).result.headOption)
          .map(_.map(r =>
            MutationModel(r.id, r.name, r.mutationType, r.active, r.ignoreSex, r.cantSaltos)
          ))

  override def getMutatitionModelParameters(idMutationModel: Long): Future[List[MutationModelParameter]] =
    db.run(
      mutationModelParameterTable
        .filter(_.idMutationModel === idMutationModel)
        .sortBy(_.locus)
        .result
    ).map(_.toList.map(r =>
      MutationModelParameter(r.id, r.idMutationModel, r.locus, r.sex,
        r.mutationRate, r.mutationRange, r.mutationRateMicrovariant)
    ))

  override def getMutatitionModelData(
    idMutationModel: Long,
    mutationModelType: Long,
    markers: List[String]
  ): Future[List[(MutationModelParameter, List[MutationModelKi])]] =
    val paramsAction = mutationModelParameterTable
      .filter(_.idMutationModel === idMutationModel)
      .filter(_.locus.inSet(markers))
      .result
    val action = paramsAction.flatMap { params =>
      DBIO.sequence(params.toList.map { x =>
        mutationModelType match
          // TODO agregar los datos q se necesiten para nuevos modelos
          // TODO filtrar por locus del pedigree, q se reciban de parametro
          case 1 /* Modelo Equals */ =>
            DBIO.successful(
              (MutationModelParameter(x.id, x.idMutationModel, x.locus, x.sex,
                x.mutationRate, x.mutationRange, x.mutationRateMicrovariant),
                List.empty[MutationModelKi])
            )
          case 2 /* Modelo StepWise */ =>
            mutationModelKiTable
              .filter(_.idMutationModelParameter === x.id)
              .result
              .map(kis =>
                (MutationModelParameter(x.id, x.idMutationModel, x.locus, x.sex,
                  x.mutationRate, x.mutationRange, x.mutationRateMicrovariant),
                  kis.toList.map(ki => MutationModelKi(ki.id, ki.idMutationModelParameter, ki.allele, ki.ki)))
              )
      })
    }
    db.run(action.transactionally)

  override def getAllMutationDefaultParameters(): Future[List[MutationDefaultParam]] =
    db.run(mutationDefaultParameter.sortBy(_.locus).result)
      .map(_.toList.map(r => MutationDefaultParam(r.locus, r.sex, r.mutationRate)))

  // ---------------------------------------------------------------------------
  // Writes
  // ---------------------------------------------------------------------------

  override def insertMutationModel(fullMutationModel: MutationModelFull): Future[Either[String, Long]] =
    val headerRow = Tables.MutationModelRow(
      id = 0L,
      name = fullMutationModel.header.name,
      mutationType = fullMutationModel.header.mutationType,
      active = fullMutationModel.header.active,
      ignoreSex = fullMutationModel.header.ignoreSex,
      cantSaltos = fullMutationModel.header.cantSaltos
    )
    val insertHeader = (mutationModelTable returning mutationModelTable.map(_.id)) += headerRow
    val action = insertHeader.flatMap { newId =>
      val paramRows = fullMutationModel.parameters.map(x =>
        Tables.MutationModelParameterRow(
          id = 0L, idMutationModel = newId,
          locus = x.locus, sex = x.sex,
          mutationRate = x.mutationRate,
          mutationRange = x.mutationRange,
          mutationRateMicrovariant = x.mutationRateMicrovariant
        )
      )
      (mutationModelParameterTable ++= paramRows).map(_ => newId)
    }
    db.run(action.transactionally)
      .map(id => Right(id))
      .recover { case e: Exception =>
        logger.error(e.getMessage, e)
        Left(e.getMessage)
      }

  override def insertParameters(parameterList: List[MutationModelParameter]): Future[Either[String, Unit]] =
    val rows = parameterList.map(x =>
      Tables.MutationModelParameterRow(
        id = 0L, idMutationModel = x.idMutationModel,
        locus = x.locus, sex = x.sex,
        mutationRate = x.mutationRate,
        mutationRange = x.mutationRange,
        mutationRateMicrovariant = x.mutationRateMicrovariant
      )
    )
    db.run((mutationModelParameterTable ++= rows).transactionally)
      .map(_ => Right(()))
      .recover { case e: Exception =>
        logger.error(e.getMessage, e)
        Left(e.getMessage)
      }

  override def insertLocusAlleles(locusAlleles: List[(String, Double)]): Future[Either[String, Int]] =
    val rows = locusAlleles.map((loc, allele) => Tables.LocusAllelesRow(0L, loc, allele))
    db.run((locusAllele ++= rows).transactionally)
      .map(_ => Right(rows.size))
      .recover { case e: Exception =>
        logger.error(e.getMessage, e)
        Left(e.getMessage)
      }

  // Legacy preserva un bug: retorna Right(()) incluso ante excepciones.
  override def insertKi(kiList: List[MutationModelKi]): Future[Either[String, Unit]] =
    val rows = kiList.map(x =>
      Tables.MutationModelKiRow(
        id = 0L,
        idMutationModelParameter = x.idMutationModelParameter,
        allele = x.allele,
        ki = x.ki
      )
    )
    db.run((mutationModelKiTable ++= rows).transactionally)
      .map(_ => Right(()))
      .recover { case e: Exception =>
        logger.error(e.getMessage, e)
        Right(())
      }

  override def updateMutationModel(fullMutationModel: MutationModelFull): Future[Either[String, Unit]] =
    val m = fullMutationModel.header
    val updateHeader = mutationModelTable
      .filter(_.id === m.id)
      .map(x => (x.name, x.mutationType, x.active, x.ignoreSex, x.cantSaltos))
      .update((m.name, m.mutationType, m.active, m.ignoreSex, m.cantSaltos))

    val (existing, nuevos) = fullMutationModel.parameters.partition(_.id > 0)

    val updates = DBIO.sequence(existing.map { row =>
      mutationModelParameterTable
        .filter(_.id === row.id)
        .map(x => (x.mutationRate, x.mutationRange, x.mutationRateMicrovariant))
        .update((row.mutationRate, row.mutationRange, row.mutationRateMicrovariant))
    })

    val newRows = nuevos.map(x =>
      Tables.MutationModelParameterRow(
        id = 0L, idMutationModel = m.id,
        locus = x.locus, sex = x.sex,
        mutationRate = x.mutationRate,
        mutationRange = x.mutationRange,
        mutationRateMicrovariant = x.mutationRateMicrovariant
      )
    )
    val inserts = mutationModelParameterTable ++= newRows

    val action = updateHeader.flatMap(_ => updates.flatMap(_ => inserts))
    db.run(action.transactionally)
      .map(_ => Right(()))
      .recover { case e: Exception =>
        logger.error(e.getMessage, e)
        // Legacy: Messages("error.E0500") -> hardcoded para no acoplar repo a i18n
        Left("E0500: Error al actualizar la configuración el motivo.")
      }

  override def deleteMutationModelById(id: Long): Future[Either[String, Unit]] =
    val paramIdsAction = mutationModelParameterTable
      .filter(_.idMutationModel === id)
      .map(_.id)
      .result
    val action = paramIdsAction.flatMap { ids =>
      val deleteKis    = mutationModelKiTable.filter(_.idMutationModelParameter.inSet(ids)).delete
      val deleteParams = mutationModelParameterTable.filter(_.idMutationModel === id).delete
      val deleteModel  = mutationModelTable.filter(_.id === id).delete
      deleteKis.flatMap(_ => deleteParams.flatMap(_ => deleteModel))
    }
    db.run(action.transactionally)
      .map(_ => Right(()))
      .recover { case e: Exception =>
        logger.error(e.getMessage, e)
        Left(e.getMessage)
      }

  override def deleteMutationModelKiByIdMutationModel(id: Long): Future[Either[String, Unit]] =
    val paramIdsAction = mutationModelParameterTable
      .filter(_.idMutationModel === id)
      .map(_.id)
      .result
    val action = paramIdsAction.flatMap { ids =>
      mutationModelKiTable.filter(_.idMutationModelParameter.inSet(ids)).delete
    }
    db.run(action.transactionally)
      .map(_ => Right(()))
      .recover { case e: Exception =>
        logger.error(e.getMessage, e)
        Left(e.getMessage)
      }
