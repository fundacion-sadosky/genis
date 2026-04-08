package stats

import javax.inject.*
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api.*
import slick.jdbc.JdbcBackend.Database
import models.Tables
import models.Tables.{PopulationBaseFrequencyRow, PopulationBaseFrequencyNameRow}

@Singleton
class PopulationBaseFrequencyRepositoryImpl @Inject()(
  db: Database
)(implicit ec: ExecutionContext) extends PopulationBaseFrequencyRepository:
  private val pbf  = Tables.PopulationBaseFrequency
  private val pbfn = Tables.PopulationBaseFrequencyName

  // ---------------------------------------------------------------------------
  // Helpers de conversión
  // ---------------------------------------------------------------------------

  private def rowToModel(
    nameRow: PopulationBaseFrequencyNameRow,
    freqRows: Seq[PopulationBaseFrequencyRow]
  ): PopulationBaseFrequency =
    val samples = freqRows.map(r => PopulationSampleFrequency(r.marker, r.allele, r.frequency))
    PopulationBaseFrequency(
      nameRow.name,
      nameRow.theta,
      ProbabilityModel.withName(nameRow.model),
      samples
    )

  // ---------------------------------------------------------------------------
  // Implementación de operaciones
  // ---------------------------------------------------------------------------

  override def add(populationBase: PopulationBaseFrequency): Future[Option[Int]] =
    val nameRow = PopulationBaseFrequencyNameRow(
      id      = 0L,
      name    = populationBase.name,
      theta   = populationBase.theta,
      model   = populationBase.model.toString,
      active  = true,
      default = false
    )

    // Inserta el nombre (obteniendo el id auto-generado), luego inserta las frecuencias
    val action = (for
      id      <- (pbfn returning pbfn.map(_.id)) += nameRow
      freqRows = populationBase.base.map(r =>
                   PopulationBaseFrequencyRow(0L, id, r.marker, r.allele, r.frequency))
      count   <- pbf ++= freqRows
    yield count).transactionally

    db.run(action).recover { case ex =>
      play.api.Logger("PopulationBaseFrequencyRepositoryImpl").debug(ex.getMessage)
      None
    }

  override def getByName(name: String): Future[Option[PopulationBaseFrequency]] =
    // Usamos join explícito para que Slick 3 infiera los tipos correctamente
    val q = pbfn
      .filter(n => n.name === name && n.active)
      .join(pbf).on(_.id === _.baseName)

    db.run(q.result).map { rows =>
      rows.headOption.map { case (nameRow, _) =>
        val freqRows = rows.map(_._2)
        rowToModel(nameRow, freqRows)
      }
    }

  override def getAll(): Future[Seq[PopulationSampleFrequency]] =
    val q = pbfn
      .filter(_.active)
      .join(pbf).on(_.id === _.baseName)
      .map(_._2)

    db.run(q.result).map(_.map(r => PopulationSampleFrequency(r.marker, r.allele, r.frequency)))

  override def getAllNames(): Future[Seq[(String, Double, String, Boolean, Boolean)]] =
    db.run(pbfn.result).map(_.map(r => (r.name, r.theta, r.model, r.active, r.default)))

  override def toggleStatePopulationBaseFrequency(name: String): Future[Option[Int]] =
    val action = (for
      currentOpt <- pbfn.filter(_.name === name).map(_.active).result.headOption
      count      <- currentOpt match
                     case Some(current) => pbfn.filter(_.name === name).map(_.active).update(!current)
                     case None          => DBIO.successful(0)
    yield Some(count)).transactionally

    db.run(action)

  override def setAsDefault(name: String): Future[Int] =
    val action = (for
      _     <- pbfn.map(_.default).update(false)
      count <- pbfn.filter(n => n.name === name && n.active).map(_.default).update(true)
    yield count).transactionally

    db.run(action)
