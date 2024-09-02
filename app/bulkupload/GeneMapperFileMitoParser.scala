package bulkupload

import java.io.File
import com.github.tototoshi.csv.{CSVReader, DefaultCSVFormat}
import play.api.i18n.Messages
import profile.{AlleleValue, Mitocondrial, MtRCRS}
import profiledata.ProfileDataRepository

object GeneMapperFileMitoParser{

  implicit object GeneMapperFileMitoFormat extends DefaultCSVFormat {
    override val delimiter = '\t'
  }

  private def parseHeader(header: Seq[String])(implicit messages : Messages): Either[String, GeneMaperMitoFileHeader] = {
    header
      .zipWithIndex
      .foldLeft(GeneMaperFileMitoHeaderBuilder(HeaderLine = header)) { case (builder, tup) => builder.buildWith(tup._1, tup._2) }
      .build
  }

  private def parseLine(
    prev: List[String],
    mapa: GeneMaperMitoFileHeader,
    builder: ProtoProfileBuilder,
    input: Stream[List[String]],
    index : Int,
    mito : MtRCRS
  )(implicit messages : Messages): (ProtoProfile, Stream[List[String]]) = {
    input match {
      case Stream.Empty => (builder.build, Stream.Empty)
      case line #:: tail if (line(mapa.sampleName) == prev(mapa.sampleName)) => {
        val MitoRango = Seq(line(mapa.RangeFrom), line(mapa.RangeTo))
        val valida = validarMaxMin(line(mapa.RangeTo), line(mapa.RangeFrom))
        val bldrMd = builder
          .buildWithSampleName(line(mapa.sampleName))
          .buildWithAssigne(line(mapa.UD1).toLowerCase)
          .buildWithCategory(line(mapa.SpecimenCategory))
          .buildWithKit("Mitocondrial")
          .buildWithGenemapperLine(line)
          .buildWithMarker("HV"+index+"_RANGE", MitoRango,true)
          .buildWithErrors(valida)
        val Mito = Seq(
          line(mapa.Variacion1), line(mapa.Variacion2), line(mapa.Variacion3),
          line(mapa.Variacion4), line(mapa.Variacion5), line(mapa.Variacion6),
          line(mapa.Variacion7), line(mapa.Variacion8), line(mapa.Variacion9),
          line(mapa.Variacion10), line(mapa.Variacion11), line(mapa.Variacion12),
          line(mapa.Variacion13), line(mapa.Variacion14), line(mapa.Variacion15),
          line(mapa.Variacion16), line(mapa.Variacion17), line(mapa.Variacion18),
          line(mapa.Variacion19), line(mapa.Variacion20), line(mapa.Variacion21),
          line(mapa.Variacion22), line(mapa.Variacion23), line(mapa.Variacion24),
          line(mapa.Variacion25), line(mapa.Variacion26), line(mapa.Variacion27),
          line(mapa.Variacion28), line(mapa.Variacion29), line(mapa.Variacion30),
          line(mapa.Variacion31), line(mapa.Variacion32), line(mapa.Variacion33),
          line(mapa.Variacion34), line(mapa.Variacion35), line(mapa.Variacion36),
          line(mapa.Variacion37), line(mapa.Variacion38), line(mapa.Variacion39),
          line(mapa.Variacion40), line(mapa.Variacion41), line(mapa.Variacion42),
          line(mapa.Variacion43), line(mapa.Variacion44), line(mapa.Variacion45),
          line(mapa.Variacion46), line(mapa.Variacion47), line(mapa.Variacion48),
          line(mapa.Variacion49), line(mapa.Variacion50)
        )
        val valido =  validarRangoVariaciones(
          line(mapa.RangeTo),
          line(mapa.RangeFrom),
          Mito
        )
        val posiciones = convertirPosiciones(
          List(
            line(mapa.Variacion1), line(mapa.Variacion2), line(mapa.Variacion3),
            line(mapa.Variacion4), line(mapa.Variacion5), line(mapa.Variacion6),
            line(mapa.Variacion7), line(mapa.Variacion8), line(mapa.Variacion9),
            line(mapa.Variacion10), line(mapa.Variacion11), line(mapa.Variacion12),
            line(mapa.Variacion13), line(mapa.Variacion14), line(mapa.Variacion15),
            line(mapa.Variacion16), line(mapa.Variacion17), line(mapa.Variacion18),
            line(mapa.Variacion19), line(mapa.Variacion20), line(mapa.Variacion21),
            line(mapa.Variacion22), line(mapa.Variacion23), line(mapa.Variacion24),
            line(mapa.Variacion25), line(mapa.Variacion26), line(mapa.Variacion27),
            line(mapa.Variacion28), line(mapa.Variacion29), line(mapa.Variacion30),
            line(mapa.Variacion31), line(mapa.Variacion32), line(mapa.Variacion33),
            line(mapa.Variacion34), line(mapa.Variacion35), line(mapa.Variacion36),
            line(mapa.Variacion37), line(mapa.Variacion38), line(mapa.Variacion39),
            line(mapa.Variacion40), line(mapa.Variacion41), line(mapa.Variacion42),
            line(mapa.Variacion43), line(mapa.Variacion44), line(mapa.Variacion45),
            line(mapa.Variacion46), line(mapa.Variacion47), line(mapa.Variacion48),
            line(mapa.Variacion49), line(mapa.Variacion50)
          )
        )
        val bldr = bldrMd
          .buildWithMarker("HV"+index, Mito,true)
          .buildWithErrors(valido)
          .buildWithAllelesVal(posiciones,mito)
          .buildWithMtExistente
        val i= index + 1
        parseLine(line, mapa, bldr, tail, i, mito)
      }
      case head #:: tail => (builder.build, input)
    }
  }

  def parse(
    csvFile: File,
    validator: Validator,
    mito : MtRCRS
  )(implicit messages : Messages): Either[String, Stream[ProtoProfile]] = {
    val stream = CSVReader.open(csvFile).toStream
    parseCsvStream(stream, validator, mito)
  }

  // Call greet method without parameters
  def parseCsvStream(
    csv: Stream[List[String]],
    validator: Validator,
    mito: MtRCRS
  )(implicit messages : Messages): Either[String, Stream[ProtoProfile]] = {
    val index = 1;
    def profileStream(
      source: Stream[List[String]],
      header: GeneMaperMitoFileHeader
    ): Stream[ProtoProfile] = {
      if (source.isEmpty) {
        Stream.Empty
      } else {
        val (protoProfile, remainingLines) = parseLine(
          source.head,
          header,
          ProtoProfileBuilder(
            validator,
            genemapperLine = Seq(header.HeaderLine)
          ),
          source,
          index,
          mito
        )
        Stream
          .cons(
            protoProfile,
            profileStream(
              remainingLines,
              header
            )
          )
      }
    }
    val stream = csv
    val header = stream.head
    val lines = stream.tail
    val mapar = parseHeader(header)
    mapar.right.map { h => profileStream(lines, h) }
  }

  def validarMaxMin(max:String, min:String): Option[String] ={
    val maxInt = toInt(max)
    val minInt = toInt(min)
    val minMito = 1 // Mitochodrial genome is circular with 16569 positions.
    val maxMito = 16569
    val minDLoop = 16024 // D loop goes 16024,...,16569,1,...,576.
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
    max:String,
    min:String,
    variaciones : Seq[String]
  ): Option[String] = {
    val maxInt = toInt(max).get + 0.9
    val minInt = toInt(min)
    var contador = 0
    val invalidPositionInGivenRanges = (v:String) => {
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
    val invalidPositionInReferenceRanges = (s:String) => {
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
    lazy val valido = variaciones exists invalidPositionInGivenRanges
    lazy val validar = variaciones exists invalidPositionInReferenceRanges
    valido match {
      case true => Option("E0307")
      case false if validar => Option("E0311")
      case false => None
    }
  }

  def toInt(s: String): Option[Int] = {
    try {
      Some(s.toInt)
    } catch {
          case e: Exception => None
    }
  }

  def convertirPosiciones(
    alelos: List[String]
  ): List[(Mitocondrial,String)] = {
   val result = alelos.map{
     alelo =>
       if(alelo!="") {
         val al = AlleleValue(alelo)
         al match {
           case Mitocondrial(base,pos) if !pos.toString.contains(".") =>
             (Mitocondrial(base,pos), alelo.substring(0,1))
           case _ => (null ,"")
         }
       } else {
         (null ,"")
       }
    }
    result
  }







}

