package stats

import javax.inject.*
import scala.concurrent.{ExecutionContext, Future}
import scala.collection.immutable.SortedMap
import java.io.File
import scala.io.Source
import kits.StrKitService
import play.api.Logging
import play.api.cache.AsyncCacheApi

import scala.concurrent.duration.*

@Singleton
class PopulationBaseFrequencyServiceImpl @Inject()(
  cache: AsyncCacheApi,
  popBaseFreqRepo: PopulationBaseFrequencyRepository,
  strKitService: StrKitService
)(implicit ec: ExecutionContext) extends PopulationBaseFrequencyService with Logging:

  private val CacheTtl = 30.minutes

  // ---------------------------------------------------------------------------
  // CSV parser (sin dependencias externas)
  // ---------------------------------------------------------------------------

  private def parseCSV(file: File): List[List[String]] =
    val src    = Source.fromFile(file)
    try
      val lines = src.getLines().toList.filter(_.trim.nonEmpty)
      if lines.isEmpty then return List.empty
      // Detectar delimitador analizando la primera línea
      val delim  = if lines.head.split(",").length > lines.head.split(";").length then "," else ";"
      lines.map(_.split(delim, -1).map(_.trim).toList)
    finally src.close()

  // ---------------------------------------------------------------------------
  // CSV value converters (lógica idéntica al legacy)
  // ---------------------------------------------------------------------------

  private val toBigDecimalNonZero: String => BigDecimal = s =>
    val bc = BigDecimal(s.replaceAll(",", "."))
    if bc > 1 || bc <= 0 then
      logger.error(s"Number out of range (0,1]: $s")
      throw new NumberFormatException(s)
    bc

  private val toDouble: String => Double = s =>
    s.replaceAll(",", ".").toDouble

  private val stringToOpt: String => Option[String] = xx =>
    if xx != null && xx.nonEmpty then Some(xx) else None

  // ---------------------------------------------------------------------------
  // Validación del CSV parseado
  // ---------------------------------------------------------------------------

  private def validate(
    base: List[List[String]],
    locusAlias: Map[String, String]
  ): Either[Seq[String], NormalizedPbF] =

    val last   = base.last
    val header = base.head.drop(1)
    val tail   = base.tail
    var errors: List[String] = List.empty

    val markers =
      val resolved = header.flatMap(x => locusAlias.get(x))
      if resolved.length == header.length then
        resolved
      else
        val diff = header.diff(resolved)
        logger.error(s"Loci not found in alias map: $diff")
        val diffs = diff.mkString(" ")
        errors = errors :+ s"Los siguientes marcadores no están registrados: $diffs"
        List.empty

    val headerLength = markers.length

    if (headerLength + 1) != last.length then
      logger.error(s"Header length mismatch: expected ${headerLength + 1}, got ${last.length}")
      errors = errors :+ s"El encabezado tiene ${headerLength + 1} columnas pero la última fila tiene ${last.length}"

    var fmins: List[BigDecimal] = List.empty

    if "FMIN" == last.head.toUpperCase then
      try
        fmins = last.drop(1).map(toBigDecimalNonZero)
      catch
        case nfe: NumberFormatException =>
          errors = errors :+ s"Error en valores FMIN: ${nfe.getMessage}"
          fmins = List(BigDecimal(1))

    val allelesAsString = if fmins.nonEmpty then tail.take(tail.length - 1) else tail

    val alleles =
      try allelesAsString.map(ss => toDouble(ss.head))
      catch
        case _: Exception =>
          errors = errors :+ "No se pudieron parsear los alelos (primera columna)"
          List.empty[Double]

    val duplicatedAlleles = alleles.groupBy(identity).filter(_._2.length > 1).keys
    if duplicatedAlleles.nonEmpty then
      errors = errors :+ s"Alelos duplicados: ${duplicatedAlleles.mkString(", ")}"

    val freq =
      try tail.map { s =>
        s.drop(1).map { xx =>
          stringToOpt(xx).map(toBigDecimalNonZero)
        }
      }
      catch
        case nfe: NumberFormatException =>
          errors = errors :+ s"Error en valor de frecuencia: ${nfe.getMessage}"
          List.empty

    if errors.isEmpty then
      Right(NormalizedPbF(markers, alleles, freq, fmins))
    else
      Left(errors)

  // ---------------------------------------------------------------------------
  // Service methods
  // ---------------------------------------------------------------------------

  override def parseFile(
    name: String,
    theta: Double,
    model: ProbabilityModel,
    csvFile: File
  ): Future[PopBaseFreqResult] =

    val base = parseCSV(csvFile)

    strKitService.getLocusAlias.flatMap { locusAlias =>
      validate(base, locusAlias).fold(
        errors => Future.successful(PopBaseFreqResult("Invalid", None, None, errors, None)),
        npbf   =>
          var seqPsf = Seq.empty[PopulationSampleFrequency]

          npbf.markers.zipWithIndex.foreach { (mk, indexM) =>
            npbf.alleles.zipWithIndex.foreach { (allele, indexA) =>
              val ere = npbf.freqs(indexA)(indexM)
              if ere.isDefined then
                seqPsf = seqPsf :+ PopulationSampleFrequency(mk, allele, ere.get)
            }
          }

          if npbf.fmins.nonEmpty then
            val psfFmins = npbf.fmins.zip(npbf.markers).map { (fmin, marker) =>
              PopulationSampleFrequency(marker, -1, fmin)
            }
            save(PopulationBaseFrequency(name, theta, model, seqPsf ++ psfFmins))
              .map(l => PopBaseFreqResult("Imported", None, None, Seq.empty, Some(l)))
          else
            val key = java.util.UUID.randomUUID().toString
            cache.set(key, PopulationBaseFrequency(name, theta, model, seqPsf), CacheTtl)
              .map { _ =>
                PopBaseFreqResult("Incomplete", Some(key), Some(npbf.markers), Seq.empty, None)
              }
      )
    }

  override def insertFmin(id: String, fmins: Fmins): Future[PopBaseFreqResult] =
    val fminsAsSeq: Seq[PopulationSampleFrequency] = fmins.calcOption match
      case MinimunFrequencyCalc.BudowleMonsonChakraborty =>
        fmins.config.map { (k, v) =>
          val ft     = 1 - v(1)
          val pft    = 1 - Math.pow(ft, 1.0 / v(2))
          val result = 1 - Math.pow(pft, 1.0 / (2 * v(0)))
          PopulationSampleFrequency(k, -1, BigDecimal(result))
        }.toSeq
      case MinimunFrequencyCalc.NRCII =>
        fmins.config.map { (k, v) =>
          val result = 5.0 / (2.0 * v(0))
          PopulationSampleFrequency(k, -1, BigDecimal(result))
        }.toSeq
      case MinimunFrequencyCalc.Weir =>
        fmins.config.map { (k, v) =>
          val alpha  = v(1)
          val n2     = 2 * v(0)
          val result = 1 - Math.pow(alpha, 1.0 / n2)
          PopulationSampleFrequency(k, -1, BigDecimal(result))
        }.toSeq
      case MinimunFrequencyCalc.FminValue =>
        fmins.config.map { (k, v) =>
          PopulationSampleFrequency(k, -1, BigDecimal(v(0)))
        }.toSeq

    insertFminInDb(id, fminsAsSeq)
      .map(i => PopBaseFreqResult("Inserted", None, None, Seq.empty, Some(i)))

  private def insertFminInDb(id: String, fmins: Seq[PopulationSampleFrequency]): Future[Int] =
    cache.get[PopulationBaseFrequency](id).flatMap {
      case None      => Future.successful(0)
      case Some(pbf) =>
        cache.remove(id)   // limpiar la entrada temporal
        val newPbf = PopulationBaseFrequency(pbf.name, pbf.theta, pbf.model, pbf.base ++ fmins)
        save(newPbf)
    }

  override def save(popBaseFreq: PopulationBaseFrequency): Future[Int] =
    popBaseFreqRepo.add(popBaseFreq).map(_.getOrElse(-1))
    // Nota: MutationService.refreshAllKis() omitido — no portado aún al core

  override def setAsDefault(name: String): Future[Int] =
    popBaseFreqRepo.setAsDefault(name)

  override def getByName(name: String): Future[Option[PopulationBaseFrequency]] =
    popBaseFreqRepo.getByName(name)

  override def getAllPossibleAllelesByLocus(): Future[PopulationBaseFrequencyGrouppedByLocus] =
    // MutationService no existe aún en core — retornamos solo los alelos de la BD
    popBaseFreqRepo.getAll().map { result =>
      PopulationBaseFrequencyGrouppedByLocus(
        result
          .map(x => (x.marker, x.allele))
          .groupBy(_._1)
          .map { (mk, pairs) =>
            mk -> pairs.map(_._2).toList.filter(_ > 0).distinct.sorted
          }
      )
    }

  override def toggleStateBase(name: String): Future[Option[Int]] =
    popBaseFreqRepo.toggleStatePopulationBaseFrequency(name)

  override def getAllNames(): Future[Seq[PopulationBaseFrequencyNameView]] =
    popBaseFreqRepo.getAllNames().map(_.map { (name, theta, model, active, default) =>
      PopulationBaseFrequencyNameView(name, theta, model, active, default)
    })

  override def getByNamePV(name: String): Future[PopulationBaseFrequencyView] =
    popBaseFreqRepo.getByName(name).map { opbf =>
      opbf.map { pbf =>
        val base    = pbf.base
        val alleles = base.groupBy(_.allele).keys.toList.sorted
        val markers = base.groupBy(_.marker).keys.toList.sorted
        val byAllele = base.groupBy(_.allele).view.mapValues(_.sortBy(_.marker)).toMap

        def normalizeMarkers(
          listFromMarkers: List[PopulationSampleFrequency],
          listOfMarkers: List[String]
        ): List[Option[BigDecimal]] = (listFromMarkers, listOfMarkers) match
          case (Nil, j)             => List.fill(j.length)(None)
          case (h :: tail, l :: t2) =>
            if h.marker == l then Some(h.frequency) :: normalizeMarkers(tail, t2)
            else None :: normalizeMarkers(listFromMarkers, t2)
          case (_, Nil)             => throw new RuntimeException("marker list mismatch")

        val normalizedRows = SortedMap(byAllele.toSeq*).toSeq.map { (_, v) =>
          normalizeMarkers(v.toList, markers)
        }

        PopulationBaseFrequencyView(alleles, markers, normalizedRows.toList)
      }.getOrElse {
        PopulationBaseFrequencyView(List.empty, List.empty, List.empty)
      }
    }

  override def getDefault(): Future[Option[PopulationBaseFrequencyNameView]] =
    getAllNames().map(_.find(_.default))
