package bulkupload

import java.io.File
import com.github.tototoshi.csv.{CSVReader, DefaultCSVFormat}
import play.api.i18n.Messages
import profile.{AlleleValue, Mitocondrial, MtRCRS}

import scala.annotation.tailrec

object GeneMapperFileMitoParser {

  implicit object GeneMapperFileMitoFormat extends DefaultCSVFormat {
    override val delimiter = '\t'
  }

  private def parseHeader(header: Seq[String])(using messages: Messages): Either[String, GeneMaperMitoFileHeader] =
    header
      .zipWithIndex
      .foldLeft(GeneMaperFileMitoHeaderBuilder(HeaderLine = header)) { case (builder, tup) => builder.buildWith(tup._1, tup._2) }
      .build

  @tailrec
  private def parseLine(
    prev: List[String],
    mapa: GeneMaperMitoFileHeader,
    builder: ProtoProfileBuilder,
    input: LazyList[List[String]],
    index: Int,
    mito: MtRCRS
  ): (ProtoProfile, LazyList[List[String]]) = {
    input match {
      case LazyList() => (builder.build, LazyList.empty)
      case line #:: tail if line(mapa.sampleName) == prev(mapa.sampleName) =>
        val MitoRango = Seq(line(mapa.RangeFrom), line(mapa.RangeTo))
        val valida = validarMaxMin(line(mapa.RangeTo), line(mapa.RangeFrom))
        val bldrMd = builder
          .buildWithSampleName(line(mapa.sampleName))
          .buildWithAssigne(line(mapa.UD1).toLowerCase)
          .buildWithCategory(line(mapa.SpecimenCategory))
          .buildWithKit("Mitocondrial")
          .buildWithGenemapperLine(line)
          .buildWithMarker(s"HV${index}_RANGE", MitoRango, mitocondrial = true)
          .buildWithErrors(valida)
        val variations = List(
          mapa.Variacion1, mapa.Variacion2, mapa.Variacion3, mapa.Variacion4,
          mapa.Variacion5, mapa.Variacion6, mapa.Variacion7, mapa.Variacion8,
          mapa.Variacion9, mapa.Variacion10, mapa.Variacion11, mapa.Variacion12,
          mapa.Variacion13, mapa.Variacion14, mapa.Variacion15, mapa.Variacion16,
          mapa.Variacion17, mapa.Variacion18, mapa.Variacion19, mapa.Variacion20,
          mapa.Variacion21, mapa.Variacion22, mapa.Variacion23, mapa.Variacion24,
          mapa.Variacion25, mapa.Variacion26, mapa.Variacion27, mapa.Variacion28,
          mapa.Variacion29, mapa.Variacion30, mapa.Variacion31, mapa.Variacion32,
          mapa.Variacion33, mapa.Variacion34, mapa.Variacion35, mapa.Variacion36,
          mapa.Variacion37, mapa.Variacion38, mapa.Variacion39, mapa.Variacion40,
          mapa.Variacion41, mapa.Variacion42, mapa.Variacion43, mapa.Variacion44,
          mapa.Variacion45, mapa.Variacion46, mapa.Variacion47, mapa.Variacion48,
          mapa.Variacion49, mapa.Variacion50
        )
        val Mito = variations.map(line.lift).map(_.getOrElse(""))
        val valido = validarRangoVariaciones(line(mapa.RangeTo), line(mapa.RangeFrom), Mito)
        val posiciones = convertirPosiciones(Mito)
        val bldr = bldrMd
          .buildWithMarker(s"HV${index}", Mito, mitocondrial = true)
          .buildWithErrors(valido)
          .buildWithAllelesVal(posiciones, mito)
          .buildWithMtExistente()
        parseLine(line, mapa, bldr, tail, index + 1, mito)
      case _ => (builder.build, input)
    }
  }

  def parse(
    csvFile: File,
    validator: Validator,
    mito: MtRCRS
  )(using messages: Messages): Either[String, LazyList[ProtoProfile]] = {
    val stream = CSVReader.open(csvFile).iterator.map(_.toList).to(LazyList)
    parseCsvStream(stream, validator, mito)
  }

  def parseCsvStream(
    csv: LazyList[List[String]],
    validator: Validator,
    mito: MtRCRS
  )(using messages: Messages): Either[String, LazyList[ProtoProfile]] = {
    val index = 1
    def profileStream(
      source: LazyList[List[String]],
      header: GeneMaperMitoFileHeader
    ): LazyList[ProtoProfile] = {
      if (source.isEmpty) LazyList.empty
      else {
        val (protoProfile, remainingLines) = parseLine(
          source.head,
          header,
          ProtoProfileBuilder(validator, genemapperLine = Seq(header.HeaderLine)),
          source,
          index,
          mito
        )
        protoProfile #:: profileStream(remainingLines, header)
      }
    }
    val header = csv.head
    val lines = csv.tail
    parseHeader(header).map(h => profileStream(lines, h))
  }

  def validarMaxMin(max: String, min: String): Option[String] = {
    val maxInt = toInt(max)
    val minInt = toInt(min)
    val minMito = 1
    val maxMito = 16569
    val minDLoop = 16024
    val maxDLoop = 576
    lazy val anyIsEmpty = minInt.isEmpty || maxInt.isEmpty
    lazy val maxOutOfMito = maxInt.get > maxMito || maxInt.get < minMito
    lazy val minOutOfMito = minInt.get > maxMito || minInt.get < minMito
    lazy val outOfGenome = anyIsEmpty || maxOutOfMito || minOutOfMito
    lazy val maxOutDloop = maxInt.get > maxDLoop && maxInt.get < minDLoop
    lazy val minOutDloop = minInt.get > maxDLoop && minInt.get < minDLoop
    lazy val outOfDloop = maxOutDloop || minOutDloop
    lazy val minGreaterThanMax = minInt.get >= maxInt.get
    outOfGenome match {
      case true => Option("E0308")
      case false if outOfDloop => Option("E0312")
      case false if minGreaterThanMax => Option("E0309")
      case _ => None
    }
  }

  def validarRangoVariaciones(
    max: String,
    min: String,
    variaciones: Seq[String]
  ): Option[String] = {
    val maxInt = toInt(max).get + 0.9
    val minInt = toInt(min)
    var contador = 0
    val invalidPositionInGivenRanges = (v: String) => {
      var result = true
      if (minInt.isEmpty || maxInt <= 0) {
        result = false
      } else {
        if (v != "") {
          val allele = AlleleValue(v)
          result = allele match {
            case Mitocondrial(_, position) => !(
              position == null ||
              maxInt < position.toDouble ||
              position.toDouble < minInt.get
            )
            case _ => false
          }
        } else {
          contador += 1
        }
      }
      !result
    }
    val invalidPositionInReferenceRanges = (s: String) => {
      var resultado = true
      if (s != "") {
        val alelo = AlleleValue(s)
        resultado = alelo match {
          case Mitocondrial(_, position) => !(
            position == null ||
              (576 < position.toDouble && position.toDouble < 16024) ||
              position.toDouble > 16569 ||
              position.toDouble < 1
            )
          case _ => false
        }
      }
      !resultado
    }
    lazy val valido = variaciones.exists(invalidPositionInGivenRanges)
    lazy val validar = variaciones.exists(invalidPositionInReferenceRanges)
    valido match {
      case true => Option("E0307")
      case false if validar => Option("E0311")
      case false => None
    }
  }

  def toInt(s: String): Option[Int] =
    try Some(s.toInt)
    catch { case _: Exception => None }

  def convertirPosiciones(alelos: List[String]): List[(Mitocondrial, String)] =
    alelos.map { alelo =>
      if (alelo != "") {
        val al = AlleleValue(alelo)
        al match {
          case Mitocondrial(base, pos) if !pos.toString.contains(".") =>
            (Mitocondrial(base, pos), alelo.substring(0, 1))
          case _ => (null, "")
        }
      } else {
        (null, "")
      }
    }
}
