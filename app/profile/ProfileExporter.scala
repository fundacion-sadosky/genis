package profile

import java.io.{BufferedWriter, File, FileOutputStream, FileWriter}

import com.github.tototoshi.csv.{CSVWriter, DefaultCSVFormat}
import play.api.mvc.Result

import scala.concurrent.{Await, Future}
import scala.util.Try
import java.io.{BufferedInputStream, FileInputStream, FileOutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}

import user.UserView

import scala.collection.immutable.HashMap
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
      case Mitocondrial(base, position) => {
        if (position.toString().contains(".")) {
         //es una insercion
          String.valueOf(position).concat(base.toString)
        } else {
          //es un reemplazo
          mitoPositions.tabla(position.toInt).concat(String.valueOf(position).concat(base.toString))
        }
      }
      case _ => ""
    }

  }

  def fillWithEmptyAllelesWithTabs(alleles: List[String], cantMaxima: Int): List[String] = {
    if (alleles.size == cantMaxima) {
      alleles
    } else {
      val diff = cantMaxima - alleles.size
      var ab = new ArrayBuffer[String]()
      for (i <- 1 to diff) {
        ab.append("")
      }
      val tabs = ab
      val result = alleles ++ ab
      result
    }
  }

  def export(profiles: List[(Profile, String)],
             user: String,
             exportProfilesPageSize: Int,
             exportProfilesPath: String, mitoPositions: MtRCRS): Either[String, String] = {

    val folder = s"$exportProfilesPath$user${File.separator}"
    val folderFile = new File(folder)

    folderFile.mkdirs

    val paginatedGroupProfiles = profiles.grouped(exportProfilesPageSize).zipWithIndex.toList
    var files : List[File] = List.empty

    var lastFileNumber = 0
    paginatedGroupProfiles.foreach({
      case (listaPerfilesPaginado,i) => {
        if ( listaPerfilesPaginado.exists(perfil => perfil._1.analyses.nonEmpty && (perfil._1.analyses.get.filterNot(_.kit.equals("Mitocondrial"))).nonEmpty ) ) {
         files = files :+ generateAutosomalFile(folder, listaPerfilesPaginado, i)
        }
        if( listaPerfilesPaginado.exists(perfil => perfil._1.analyses.nonEmpty && perfil._1.analyses.get.filter(_.kit.equals("Mitocondrial")).nonEmpty)) {
          files = files :+ generateMitocondrialFile(folder, listaPerfilesPaginado, i, mitoPositions)
        }
        lastFileNumber = i
      }
    })

   var maxCantAnalysis = 1
   profiles.foreach(profile => {
      maxCantAnalysis = Math.max(maxCantAnalysis, profile._1.analyses.getOrElse(List.empty).filterNot(_.kit.equals("Mitocondrial")).size)
   })

   if (maxCantAnalysis > 1) { //hay algun perfil con mas de un analisys
     for (j <- 1 to maxCantAnalysis - 1) {
       val moreThanjAutosomalAnalisys = profiles.filter(profile => profile._1.analyses.getOrElse(List.empty).filterNot(_.kit.equals("Mitocondrial")).size > j)
       files = files :+ generateAutosomalFile(folder, moreThanjAutosomalAnalisys, lastFileNumber+1, j)
       lastFileNumber = lastFileNumber + 1
     }

   }

    val result =zipFolder(folder)
    files.foreach(file => file.delete())
    result
  }

  def generateAutosomalFile(folder: String, listaPerfilesPaginado: List[(Profile, String)], i: Int, indexOfAnalisys : Int = 0): File = {
    val file = new File(s"${folder}loteAutosomal_${i + 1}.csv")

    val writer = CSVWriter.open(file)
    writer.writeAll(List(List("Sample Name",
      "Specimen Category",
      "UD1",
      "UD2",
      "Marker",
      "Allele 1",
      "Allele 2",
      "Allele 3",
      "Allele 4",
      "Allele 5",
      "Allele 6",
      "Allele 7",
      "Allele 8")))
    listaPerfilesPaginado.foreach {
      case (profile, geneMapperId) => {
        profile.analyses.foreach(analisisList => {
          val analisisAutosomales = analisisList.sortBy(_.date.date.getTime).filterNot(_.kit.equals("Mitocondrial"))
          if (analisisAutosomales.size > indexOfAnalisys) {
            val analisis = analisisAutosomales.apply(indexOfAnalisys)
            analisis.genotypification.foreach(geno => {
              writer.writeAll(List(
                List(profile.internalSampleCode, profile.categoryId.text, geneMapperId, analisis.kit, geno._1)
                  ++ fillWithEmptyAllelesWithTabs(geno._2.map(allele => alelleWrite(allele)), 8)
              ))
            })          }

        })
      }
    }

    writer.close()
    file
  }

  def generateMitocondrialFile(folder: String, listaPerfilesPaginado: List[(Profile, String)], i: Int, mitoPositions: MtRCRS): File = {
    val file = new File(s"${folder}loteMitocondrial_${i + 1}.csv")

    val writer = CSVWriter.open(file)
    writer.writeAll(List(List("Sample Name",
      "Specimen Category",
      "UD1",
      "Range From",
      "Range To",
      "Mut1",
      "Mut2",
      "Mut3",
      "Mut4",
      "Mut5",
      "Mut6",
      "Mut7",
      "Mut8",
      "Mut9",
      "Mut10",
      "Mut11",
      "Mut12",
      "Mut13",
      "Mut14",
      "Mut15",
      "Mut16",
      "Mut17",
      "Mut18",
      "Mut19",
      "Mut20",
      "Mut21",
      "Mut22",
      "Mut23",
      "Mut24",
      "Mut25",
      "Mut26",
      "Mut27",
      "Mut28",
      "Mut29",
      "Mut30",
      "Mut31",
      "Mut32",
      "Mut33",
      "Mut34",
      "Mut35",
      "Mut36",
      "Mut37",
      "Mut38",
      "Mut39",
      "Mut40",
      "Mut41",
      "Mut42",
      "Mut43",
      "Mut44",
      "Mut45",
      "Mut46",
      "Mut47",
      "Mut48",
      "Mut49",
      "Mut50"
    )))

    listaPerfilesPaginado.foreach {
      case (profile, geneMapperId) => {
        var markerMap : mutable.HashMap[String, List[AlleleValue]] = mutable.HashMap.empty
        var rangeMap : mutable.HashMap[(BigDecimal, BigDecimal), List[String]] = mutable.HashMap.empty
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
            val range : (BigDecimal, BigDecimal) = (BigDecimal(alelleWrite(markerMap("HV" + x.toString + "_RANGE")(0), mitoPositions)), BigDecimal(alelleWrite(markerMap("HV" + x.toString + "_RANGE")(1), mitoPositions)))
            val alelles : List[String] =  markerMap("HV" + x.toString).map(alelleWrite(_, mitoPositions))
            rangeMap += range -> rangeMap.get(range).getOrElse(List.empty).++(alelles)
          }
        }

        rangeMap.keys.foreach(range => {
          writer.writeAll(List(
            List(profile.internalSampleCode, profile.categoryId.text, geneMapperId, range._1, range._2)
              ++ fillWithEmptyAllelesWithTabs(rangeMap(range), 50)))
        })
      }
    }

    writer.close()
    file
  }

  def zipFolder(exportProfilesPath: String): Either[String, String] = {
    var directorioZip = exportProfilesPath
    var carpetaComprimir = new File(directorioZip)
    val fullName = directorioZip.concat("GENisExport.zip")
    if (carpetaComprimir.exists()) {
      var files = carpetaComprimir.listFiles()
      var fileOut = new FileOutputStream(fullName)
      var zip = new ZipOutputStream(fileOut)

      files.foreach { file =>
        if (file.toString.contains(".csv")) {
          var relative = new File(file.toString)
          var zipEntry = new ZipEntry(file.getName)

          zip.putNextEntry(new ZipEntry(zipEntry))
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

