package bulkupload

import com.github.tototoshi.csv.{CSVReader, DefaultCSVFormat}
import play.api.i18n.Messages

import java.io.File
import scala.annotation.tailrec

object GeneMapperFileParser {

  implicit object GeneMapperFileFormat extends DefaultCSVFormat {
    override val delimiter = '\t'
  }

  // #218 review S5Q3: primer pase barato para precargar validaciones de BD. Lee solo la columna
  // "Sample Name" (presente en el header autosomal y mitocondrial) y devuelve los valores distintos.
  def readDistinctSampleNames(csvFile: File): Seq[String] = {
    val reader = CSVReader.open(csvFile)(GeneMapperFileFormat)
    try {
      val rows = reader.iterator
      if (!rows.hasNext) Seq.empty
      else {
        val header = rows.next()
        val idx = header.indexOf("Sample Name")
        if (idx < 0) Seq.empty
        else rows.map(_.lift(idx).getOrElse("")).filter(_.nonEmpty).toSeq.distinct
      }
    } finally reader.close()
  }

  private def parseHeader(header: Seq[String])(using messages: Messages): Either[String, GeneMaperFileHeader] =
    header
      .zipWithIndex
      .foldLeft(GeneMaperFileHeaderBuilder(HeaderLine = header)) { case (builder, tup) => builder.buildWith(tup._1, tup._2) }
      .build

  @tailrec
  private def parseLine(
    prev: List[String],
    mapa: GeneMaperFileHeader,
    builder: ProtoProfileBuilder,
    input: LazyList[List[String]]
  ): (ProtoProfile, LazyList[List[String]]) = {
    input match {
      case LazyList() => (builder.build, LazyList.empty)
      case line #:: tail if line(mapa.sampleName) == prev(mapa.sampleName) =>
        val bldrMd = builder
          .buildWithSampleName(line(mapa.sampleName))
          .buildWithAssigne(line(mapa.UD1).toLowerCase)
          .buildWithCategory(line(mapa.SpecimenCategory))
          .buildWithKit(line(mapa.UD2))
          .buildWithGenemapperLine(line)
        val alleleIndexes = Seq(
          mapa.Allele1, mapa.Allele2, mapa.Allele3, mapa.Allele4,
          mapa.Allele5, mapa.Allele6, mapa.Allele7, mapa.Allele8
        )
        val alleles = alleleIndexes.map(line.lift).map(_.getOrElse(""))
        val bldr = bldrMd.buildWithMarker(line(mapa.Marker), alleles)
        parseLine(line, mapa, bldr, tail)
      case _ => (builder.build, input)
    }
  }

  def parse(
    csvFile: File,
    validator: Validator
  )(using messages: Messages): Either[String, LazyList[ProtoProfile]] = {
    def profileStream(
      source: LazyList[List[String]],
      header: GeneMaperFileHeader
    ): LazyList[ProtoProfile] = {
      if (source.isEmpty) LazyList.empty
      else {
        val (protoProfile, remainingLines) = parseLine(
          source.head,
          header,
          ProtoProfileBuilder(validator, genemapperLine = Seq(header.HeaderLine)),
          source
        )
        protoProfile #:: profileStream(remainingLines, header)
      }
    }
    val stream = CSVReader.open(csvFile).iterator.map(_.toList).to(LazyList)
    val header = stream.head
    val lines = stream.tail
    parseHeader(header).map(h => profileStream(lines, h))
  }
}
