package stats

import scala.concurrent.Future
import java.io.File

import javax.inject.Inject
import javax.inject.Singleton
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.i18n.Messages

import scala.language.postfixOps
import scala.collection.immutable.TreeMap
import scala.collection.immutable.SortedMap
import com.github.tototoshi.csv.CSVReader
import java.io.FileInputStream

import scala.io.BufferedSource
import com.github.tototoshi.csv.DefaultCSVFormat
import kits.StrKitService
import pedigree.MutationService
import probability.ProbabilityModel
import services.CacheService
import services.TemporaryAssetKey
import services.TemporaryFreqDbKey
import types.{MinimunFrequencyCalc, StatOption}
import play.api.Logger

trait PopulationBaseFrequencyService {
  def save(popBaseFreq: PopulationBaseFrequency): Future[Int]
  def getByNamePV(name: String): Future[PopulationBaseFrequencyView]
  def getByName(name: String): Future[Option[PopulationBaseFrequency]]
  def getAllNames(): Future[Seq[PopulationBaseFrequencyNameView]]
  def toggleStateBase(name: String): Future[Option[Int]]
  def setAsDefault(name: String): Future[Int]
  def parseFile(name: String, theta: Double, model: ProbabilityModel.Value, csvFile: File): Future[PopBaseFreqResult]
  def insertFmin(id: String, fmins: Fmins): Future[PopBaseFreqResult]
  def getDefault(): Future[Option[PopulationBaseFrequencyNameView]]
  def getAllPossibleAllelesByLocus(): Future[PopulationBaseFrequencyGrouppedByLocus]
  def getDefaultTableAllelesByLocus(): Future[PopulationBaseFrequencyGrouppedByLocus]
}

@Singleton
class PopulationBaseFrequencyImpl @Inject() (
  cache: CacheService,
  popBaseFreqRepo: PopulationBaseFrequencyRepository,
  strKitService: StrKitService,
  mutationService:MutationService = null) extends PopulationBaseFrequencyService {

  val logger = Logger(this.getClass())

  object comaFileFormat extends DefaultCSVFormat {
    override val delimiter = ','
  }

  object semiColonFileFormat extends DefaultCSVFormat {
    override val delimiter = ';'
  }

  private val stringToOpt = (xx: String) => if (xx != null && xx.isEmpty) None else Option(xx)

  val toBigDecimalNonZero = (s: String) => {
    val bc = BigDecimal(s.replaceAll(",", "."))
    if (bc > 1 || bc <= 0){
      logger.error(s"Number error: $s")
      throw new java.lang.NumberFormatException(s)
    }
    bc
  }

  val toDouble = (s: String) => {
    s.replaceAll(",", ".").toDouble
  }

  private def validate(base: List[List[String]], locusAlias: Map[String, String]): Either[Seq[String], NormalizedPbF] = {

    val last = base.last
    val header = base.head.drop(1)
    val tail = base.tail
    var errors: List[String] = List.empty

    val markers = {
      val rr = header.flatMap { x => locusAlias.get(x) }
      if (rr.length == header.length) {
        rr
      } else {
        val diff= header diff rr
        logger.error(s"The diff between header and locus is: $diff")
        val diffs = diff.reduce{ (a,b) => s"$a $b" }
        errors = errors :+ Messages("error.E0689",diffs)
        List.empty
      }
    }

    val headerLength = markers.length

    if ((headerLength + 1) != last.length) {
      logger.error(s"HeaderLength is: ${headerLength + 1} and last entry length is: ${last.length}")
      errors = errors :+ Messages("error.E0960",(headerLength + 1),last.length)
    }

    var fmins: List[BigDecimal] = List.empty

    if ("FMIN" == last(0).toUpperCase()) {
      try {
        fmins = last.drop(1).map { toBigDecimalNonZero(_) }
      } catch {
        case nfe: java.lang.NumberFormatException => {
          errors = errors :+ Messages("error.E0611",nfe.getMessage)
          fmins = List(BigDecimal(1))
        }
      }
    }

    val allelesAsString = if (fmins.length != 0) tail.take(tail.length - 1) else tail

    val alleles = try {
      allelesAsString map { ss => toDouble(ss(0)) }
    } catch {
      case e: Exception => {
        errors = errors :+ Messages("error.E0123")
        List.empty[Double]
      }
    }

    val duplicatedAlleles = alleles.groupBy(identity).filter(_._2.length > 1).keys
    if (duplicatedAlleles.nonEmpty) {
      errors = errors :+ Messages("error.E0124",duplicatedAlleles.mkString(", "))
    }

    val freq = try {
      tail map { s =>
        s.drop(1).map { xx =>
          val opt = stringToOpt(xx)
          opt.map { toBigDecimalNonZero(_) }
        }
      }
    } catch {
      case nfe: java.lang.NumberFormatException => {
        errors = errors :+ Messages("error.E0612",nfe.getMessage)
        List.empty
      }
    }

    if (errors.isEmpty) {
      Right(NormalizedPbF(markers, alleles, freq, fmins))
    } else {
      Left(errors)
    }
  }

  override def parseFile(name: String, theta: Double, model: ProbabilityModel.Value, csvFile: File): Future[PopBaseFreqResult] = {
    val src = new BufferedSource(new FileInputStream(csvFile))

    val iter = src.getLines.map(_.split(",")).next

    val fileFormat = if (iter.length > 1)
      comaFileFormat
    else
      semiColonFileFormat

    src.close

    val base = CSVReader.open(csvFile)(fileFormat).all

    strKitService.getLocusAlias.flatMap { locusAlias =>

      validate(base, locusAlias).fold(fa => Future.successful(PopBaseFreqResult("Invalid", None, None, fa, None)), npbf => {

        var seqPsf = Seq[PopulationSampleFrequency]()

        npbf.markers.zipWithIndex foreach {
          case (mk, indexM) =>
            npbf.alleles.zipWithIndex foreach {
              case (allele, indexA) =>
                val ere = npbf.freqs(indexA)(indexM)
                if (ere.isDefined) {
                  seqPsf = seqPsf :+ PopulationSampleFrequency(mk, allele, ere.get)
                }
            }
        }

        if (npbf.fmins.nonEmpty) {

          val psfFmins = npbf.fmins.zip(npbf.markers) map { case (fmin, marker) => PopulationSampleFrequency(marker, -1, fmin) }

          val lines = this.save(PopulationBaseFrequency(name, theta, model, seqPsf ++ psfFmins))

          lines map { l => PopBaseFreqResult("Imported", None, None, Seq.empty, Option(l)) }

        } else {
          val key = TemporaryFreqDbKey(java.util.UUID.randomUUID.toString)

          cache.set(key, PopulationBaseFrequency(name, theta, model, seqPsf))

          Future.successful(PopBaseFreqResult("Incomplete", Option(key.toString), Option(npbf.markers), Seq.empty, None))
        }
      })
    }
  }

  override def insertFmin(id: String, fmins: Fmins): Future[PopBaseFreqResult] = {

    val fminsAsMap = fmins.calcOption match {
      case MinimunFrequencyCalc.BudowleMonsonChakraborty => {
        fmins.config.map {
          case (k, v) =>
            val ft = 1 - v(1)
            val pft = 1 - Math.pow(ft, 1.0 / v(2))
            val result = 1 - Math.pow(pft, 1.0 / (2 * v(0)))
            PopulationSampleFrequency(k, -1, BigDecimal(result))
        }
      }
      case MinimunFrequencyCalc.NRCII => {
        fmins.config.map {
          case (k, v) =>
            val result = 5.0 / (2.0 * v(0))
            PopulationSampleFrequency(k, -1, BigDecimal(result))
        }
      }
      case MinimunFrequencyCalc.Weir => {
        fmins.config.map {
          case (k, v) =>
            val alpha = v(1)
            val N2 = 2 * v(0)
            val result = 1 - Math.pow(alpha, 1.0 / N2)
            PopulationSampleFrequency(k, -1, BigDecimal(result))
        }
      }
      case MinimunFrequencyCalc.FminValue => {
        fmins.config.map {
          case (k, v) =>
            PopulationSampleFrequency(k, -1, BigDecimal(v(0)))
        }
      }
    }

    insertFminInDb(id, fminsAsMap.toSeq) map { i => PopBaseFreqResult("Inserted", None, None, Seq.empty, Option(i)) }
  }

  private def insertFminInDb(id: String, fmins: Seq[PopulationSampleFrequency]): Future[Int] = {

    val key = TemporaryFreqDbKey(id)

    cache.pop(key).fold[Future[Int]](Future.successful(0)) { pbf =>

      val newPbf = PopulationBaseFrequency(pbf.name, pbf.theta, pbf.model, pbf.base ++ fmins)

      this.save(newPbf)
    }
  }

  override def setAsDefault(name: String): Future[Int] = {
    popBaseFreqRepo.setAsDefault(name)
  }

  override def getByName(name: String): Future[Option[PopulationBaseFrequency]] = {
    popBaseFreqRepo.getByName(name)
  }
  override def getAllPossibleAllelesByLocus(): Future[PopulationBaseFrequencyGrouppedByLocus] = {
    this.mutationService.getAllLocusAlleles().flatMap(locusAlleles => {
      popBaseFreqRepo.getAll().map(result =>
        {
      PopulationBaseFrequencyGrouppedByLocus(
        (result.map(x => (x.marker,x.allele)) ++ locusAlleles).groupBy(_._1)
            .map(x => x.copy(_2 = x._2.map(y => y._2).toList.filter(_ > 0).distinct.sortBy(identity)))
        )
        }
      )
    })

  }
  // A diferencia de getAllPossibleAllelesByLocus (union de TODAS las tablas
  // poblacionales cargadas + el catalogo LOCUS_ALLELE), este metodo calibra
  // contra la MISMA tabla que BayesianNetworkService.calculateProbability usa
  // como "n" para un pedigri (ver comentario en BayesianNetworkService.scala:92-102):
  // la tabla poblacional por defecto (hoy la unica en uso real: ESPAÑA). Usar
  // un dominio mas amplio en la calibracion de k_i que el que despues se
  // enumera en el calculo real hace que la fila de la matriz de mutacion ya
  // no sume 1 (k_i le resta masa a un vecino que en el calculo real nunca
  // aparece).
  //
  // Por locus se usa el dominio de la tabla por defecto SOLO cuando esa
  // tabla tiene alelos cargados bajo el mismo nombre que usa
  // MUTATION_MODEL_PARAMETER.LOCUS, o bajo un alias reconocido (ACTBP2 y
  // SE33 son el mismo locus forense -- ver isActbp2Se33Alias). Los locus que
  // ni siquiera bajo alias aparecen en la tabla por defecto (ej. D6S1043,
  // YIndel: ausentes de ESPAÑA) mantienen el comportamiento anterior
  // (dominio amplio via getAllPossibleAllelesByLocus, alimentado desde
  // LOCUS_ALLELE) para no perder silenciosamente la cobertura del modelo de
  // mutacion en un locus que sigue usandose en matches reales.
  //
  // Si en el futuro conviven pedigries con distintas tablas poblacionales
  // activas, este metodo deja de ser suficiente y hay que calibrar k_i por
  // tabla (cambio de esquema en MUTATION_MODEL_KI).
  private def isActbp2Se33Alias(name: String): Boolean = {
    val upper = name.toUpperCase
    upper.contains("ACTBP2") || upper.contains("SE33")
  }

  override def getDefaultTableAllelesByLocus(): Future[PopulationBaseFrequencyGrouppedByLocus] = {
    for {
      broad <- getAllPossibleAllelesByLocus()
      defaultOpt <- getDefault()
      defaultTable <- defaultOpt match {
        case Some(default) => popBaseFreqRepo.getByName(default.name)
        case None => Future.successful(None)
      }
    } yield {
      val defaultByLocus: Map[String, List[Double]] = defaultTable
        .map(_.base
          .groupBy(_.marker)
          .map(x => x.copy(_2 = x._2.map(_.allele).filter(_ > 0).distinct.sortBy(identity).toList)))
        .getOrElse(Map.empty)
      def resolvedDefault(locus: String): Option[List[Double]] = {
        defaultByLocus.get(locus).filter(_.nonEmpty).orElse(
          if (isActbp2Se33Alias(locus)) {
            defaultByLocus.collectFirst { case (k, v) if isActbp2Se33Alias(k) && v.nonEmpty => v }
          } else {
            None
          }
        )
      }
      PopulationBaseFrequencyGrouppedByLocus(
        broad.base.map {
          case (locus, broadAlleles) =>
            locus -> resolvedDefault(locus).getOrElse(broadAlleles)
        }
      )
    }
  }

  override def save(popBaseFreq: PopulationBaseFrequency): Future[Int] = {
    val result = popBaseFreqRepo.add(popBaseFreq) map { optInt =>
      optInt.getOrElse(-1)
    }
    result.onComplete(_ =>mutationService.refreshAllKis())
    result
  }

  override def toggleStateBase(name: String): Future[Option[Int]] = {
    popBaseFreqRepo.toggleStatePopulationBaseFrequency(name)
  }

  override def getAllNames(): Future[Seq[PopulationBaseFrequencyNameView]] = {
    popBaseFreqRepo.getAllNames map { list =>
      list.map(f =>
        PopulationBaseFrequencyNameView(f._1, f._2, f._3, f._4, f._5))
    }
  }

  override def getByNamePV(name: String): Future[PopulationBaseFrequencyView] = {
    popBaseFreqRepo.getByName(name).map { opbf =>
      opbf.map { pbf =>

        val base = pbf.base

        val alleles = base.groupBy(f => f.allele).keys.toList.sorted

        val markers = base.groupBy(f => f.marker).keys.toList.sorted

        val s = base.groupBy(f => f.allele).mapValues(f => f.sortBy(psf => psf.marker))

        def normalizeMarkers(listFromMarkers: List[PopulationSampleFrequency], listOfMarkers: List[String]): List[Option[BigDecimal]] =
          (listFromMarkers, listOfMarkers) match {
            case (Nil, j) => List.fill(j.length)(None)
            case (h :: tail, l :: tail2) =>
              if (h.marker == l)
                Some(h.frequency) :: normalizeMarkers(tail, tail2)
              else
                None :: normalizeMarkers(listFromMarkers, tail2)
            case (h, Nil) => throw new RuntimeException("oops")
          }

        val g = s.mapValues(f => normalizeMarkers(f.toList, markers))

        val i = SortedMap(g.toSeq: _*)

        val h = i.flatMap { case (k, v) => List(v) } toList

        PopulationBaseFrequencyView(alleles, markers, h)
      } getOrElse {
        val a: List[Double] = List()
        val s: List[String] = List()
        val h: List[List[Option[BigDecimal]]] = List()
        PopulationBaseFrequencyView(a, s, h)
      }
    }
  }

  override def getDefault(): Future[Option[PopulationBaseFrequencyNameView]] = {
    getAllNames() map { names =>
      val namesDefault = names.filter(freq => freq.default)
      namesDefault.headOption
    }
  }

}
