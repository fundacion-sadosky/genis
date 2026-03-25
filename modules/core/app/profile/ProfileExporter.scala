package profile

import java.io.{BufferedInputStream, File, FileInputStream, FileOutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}

import com.github.tototoshi.csv.{CSVWriter, DefaultCSVFormat}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object ProfileExporter {

  implicit object ProfileExporterFormat extends DefaultCSVFormat {
    override val delimiter = '\t'
  }

  def alelleWrite(allele: AlleleValue, mitoPositions: MtRCRS = MtRCRS(Map.empty)): String = {
    allele match {
      case Allele(count) => count.toString()
      case XY(value) => String.valueOf(value)
      case OutOfLadderAllele(base, sign) => base.toString().concat(sign)
      case MicroVariant(count) => String.valueOf(count).concat(".X")
      case Mitocondrial('-', position) => mitoPositions.tabla(position.toInt).concat(String.valueOf(position).concat("DEL"))
      case Mitocondrial(base, position) =>
        if (position.toString().contains(".")) {
          String.valueOf(position).concat(base.toString)
        } else {
          mitoPositions.tabla(position.toInt).concat(String.valueOf(position).concat(base.toString))
        }
      case _ => ""
    }
  }

  def fillWithEmptyAllelesWithTabs(alleles: List[String], cantMaxima: Int): List[String] = {
    if (alleles.size == cantMaxima) {
      alleles
    } else {
      val diff = cantMaxima - alleles.size
      val ab = new ArrayBuffer[String]()
      for (_ <- 1 to diff) {
        ab.append("")
      }
      alleles ++ ab
    }
  }

  def exportProfiles(profiles: List[(Profile, String)],
    user: String,
    exportProfilesPageSize: Int,
    exportProfilesPath: String,
    mitoPositions: MtRCRS
  ): Either[String, String] = {

    val folder = s"$exportProfilesPath$user${File.separator}"
    val folderFile = new File(folder)
    folderFile.mkdirs

    val paginatedGroupProfiles = profiles.grouped(exportProfilesPageSize).zipWithIndex.toList
    var files: List[File] = List.empty
    var lastFileNumber = 0

    paginatedGroupProfiles.foreach { case (listaPerfilesPaginado, i) =>
      if (listaPerfilesPaginado.exists(perfil => perfil._1.analyses.nonEmpty && perfil._1.analyses.get.filterNot(_.kit.equals("Mitocondrial")).nonEmpty)) {
        files = files :+ generateAutosomalFile(folder, listaPerfilesPaginado, i)
      }
      if (listaPerfilesPaginado.exists(perfil => perfil._1.analyses.nonEmpty && perfil._1.analyses.get.filter(_.kit.equals("Mitocondrial")).nonEmpty)) {
        files = files :+ generateMitocondrialFile(folder, listaPerfilesPaginado, i, mitoPositions)
      }
      lastFileNumber = i
    }

    var maxCantAnalysis = 1
    profiles.foreach { profile =>
      maxCantAnalysis = Math.max(maxCantAnalysis, profile._1.analyses.getOrElse(List.empty).filterNot(_.kit.equals("Mitocondrial")).size)
    }

    if (maxCantAnalysis > 1) {
      for (j <- 1 to maxCantAnalysis - 1) {
        val moreThanjAutosomalAnalisys = profiles.filter(profile => profile._1.analyses.getOrElse(List.empty).filterNot(_.kit.equals("Mitocondrial")).size > j)
        files = files :+ generateAutosomalFile(folder, moreThanjAutosomalAnalisys, lastFileNumber + 1, j)
        lastFileNumber = lastFileNumber + 1
      }
    }

    val result = zipFolder(folder)
    files.foreach(file => file.delete())
    result
  }

  def generateAutosomalFile(folder: String, listaPerfilesPaginado: List[(Profile, String)], i: Int, indexOfAnalisys: Int = 0): File = {
    val file = new File(s"${folder}loteAutosomal_${i + 1}.csv")
    val writer = CSVWriter.open(file)
    writer.writeAll(List(List("Sample Name",
      "Specimen Category", "UD1", "UD2", "Marker",
      "Allele 1", "Allele 2", "Allele 3", "Allele 4",
      "Allele 5", "Allele 6", "Allele 7", "Allele 8")))
    listaPerfilesPaginado.foreach { case (profile, geneMapperId) =>
      profile.analyses.foreach(analisisList => {
        val analisisAutosomales = analisisList.sortBy(_.date.date.getTime).filterNot(_.kit.equals("Mitocondrial"))
        if (analisisAutosomales.size > indexOfAnalisys) {
          val analisis = analisisAutosomales.apply(indexOfAnalisys)
          analisis.genotypification.foreach(geno => {
            writer.writeAll(List(
              List(profile.internalSampleCode, profile.categoryId.text, geneMapperId, analisis.kit, geno._1)
                ++ fillWithEmptyAllelesWithTabs(geno._2.map(allele => alelleWrite(allele)), 8)
            ))
          })
        }
      })
    }
    writer.close()
    file
  }

  def generateMitocondrialFile(folder: String, listaPerfilesPaginado: List[(Profile, String)], i: Int, mitoPositions: MtRCRS): File = {
    val file = new File(s"${folder}loteMitocondrial_${i + 1}.csv")
    val writer = CSVWriter.open(file)
    val mutColumns = (1 to 50).map(n => s"Mut$n").toList
    writer.writeAll(List(List("Sample Name", "Specimen Category", "UD1", "Range From", "Range To") ++ mutColumns))

    listaPerfilesPaginado.foreach { case (profile, geneMapperId) =>
      val markerMap: mutable.HashMap[String, List[AlleleValue]] = mutable.HashMap.empty
      val rangeMap: mutable.HashMap[(BigDecimal, BigDecimal), List[String]] = mutable.HashMap.empty
      profile.analyses.foreach(analisisList => {
        analisisList
          .sortBy(_.date.date.getTime)
          .filter(_.kit.equals("Mitocondrial"))
          .foreach(analisis => {
            analisis.genotypification.foreach(geno => {
              markerMap += geno._1 -> geno._2
            })
          })
      })
      for (x <- 1 to 4) {
        if (markerMap.contains("HV" + x.toString + "_RANGE")) {
          val range: (BigDecimal, BigDecimal) = (BigDecimal(alelleWrite(markerMap("HV" + x.toString + "_RANGE")(0), mitoPositions)), BigDecimal(alelleWrite(markerMap("HV" + x.toString + "_RANGE")(1), mitoPositions)))
          val alelles: List[String] = markerMap("HV" + x.toString).map(alelleWrite(_, mitoPositions))
          rangeMap += range -> rangeMap.getOrElse(range, List.empty).++(alelles)
        }
      }
      rangeMap.keys.foreach(range => {
        writer.writeAll(List(
          List(profile.internalSampleCode, profile.categoryId.text, geneMapperId, range._1, range._2)
            ++ fillWithEmptyAllelesWithTabs(rangeMap(range), 50)))
      })
    }
    writer.close()
    file
  }

  def zipFolder(exportProfilesPath: String): Either[String, String] = {
    val carpetaComprimir = new File(exportProfilesPath)
    val fullName = exportProfilesPath.concat("GENisExport.zip")
    if (carpetaComprimir.exists()) {
      val files = carpetaComprimir.listFiles()
      val fileOut = new FileOutputStream(fullName)
      val zip = new ZipOutputStream(fileOut)

      files.foreach { file =>
        if (file.toString.contains(".csv")) {
          val zipEntry = new ZipEntry(file.getName)
          zip.putNextEntry(zipEntry)
          val in = new BufferedInputStream(new FileInputStream(file.toString))
          var b = in.read()
          while (b > -1) {
            zip.write(b)
            b = in.read()
          }
          in.close()
        }
      }
      zip.closeEntry()
      zip.close()
      Right(fullName)
    } else {
      Left("Error al generar el archivo")
    }
  }
}
