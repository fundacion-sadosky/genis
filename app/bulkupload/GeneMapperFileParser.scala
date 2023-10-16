package bulkupload

import com.github.tototoshi.csv.DefaultCSVFormat
import java.io.File
import com.github.tototoshi.csv.CSVReader

object GeneMapperFileParser {

  implicit object GeneMapperFileFormat extends DefaultCSVFormat {
    override val delimiter = '\t'
  }

  private def parseHeader(header: Seq[String]): Either[String, GeneMaperFileHeader] = {
    header
      .zipWithIndex
      .foldLeft(GeneMaperFileHeaderBuilder(HeaderLine = header)) { case (builder, tup) => builder.buildWith(tup._1, tup._2) }
      .build
  }

  private def parseLine(
    prev: List[String],
    mapa: GeneMaperFileHeader,
    builder: ProtoProfileBuilder,
    input: Stream[List[String]]
  ): (ProtoProfile, Stream[List[String]]) = {
    input match {
      case Stream.Empty => (builder.build, Stream.Empty)
      case line #:: tail if (line(mapa.sampleName) == prev(mapa.sampleName)) => {
        val bldrMd = builder
          .buildWithSampleName(line(mapa.sampleName))
          .buildWithAssigne(line(mapa.UD1).toLowerCase)
          .buildWithCategory(line(mapa.SpecimenCategory))
          .buildWithKit(line(mapa.UD2))
          .buildWithGenemapperLine(line)
        val alleles = Seq(
          line(mapa.Allele1),
          line(mapa.Allele2),
          line(mapa.Allele3),
          line(mapa.Allele4),
          line(mapa.Allele5),
          line(mapa.Allele6),
          line(mapa.Allele7),
          line(mapa.Allele8)
        )
        val bldr = bldrMd.buildWithMarker(line(mapa.Marker), alleles)
        parseLine(line, mapa, bldr, tail)
      }
      case head #:: tail => (builder.build, input)
    }
  }

  def parse(
    csvFile: File,
    validator: Validator
  ): Either[String, Stream[ProtoProfile]] = {
    def profileStream(
      source: Stream[List[String]],
      header: GeneMaperFileHeader
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
          source
        )
        Stream.cons(protoProfile, profileStream(remainingLines, header))
      }
    }
    val stream = CSVReader.open(csvFile).toStream
    val header = stream.head
    val lines = stream.tail
    val mapar = parseHeader(header)
    val result = mapar.right.map {header => profileStream(lines, header) }
    result
  }
}
