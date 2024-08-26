package pedigree

import java.io.File

import com.github.tototoshi.csv.{CSVWriter, DefaultCSVFormat}
import matching.{MatchResult, MatchStatus}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object MPIMatchesExporter {

  implicit object MPIMatchesExporterFormat extends DefaultCSVFormat {
    override val delimiter = '\t'
  }


  def createMatchLimsArchive(matches : Seq[PedigreeMatchResultData], exportProfilesPath:String, pedigreeDataRepository : PedigreeDataRepository) = {
    val folder = s"$exportProfilesPath${File.separator}"
    val folderFile = new File(folder)

    folderFile.mkdirs

      if (matches.size > 0) {
        val file = new File(s"${folder}MatchesMPIFile.csv")

        val writer = CSVWriter.open(file)
        writer.writeAll(List(List("Caso", "Pedigree", "Perfil", "Categoria", "Fecha", "LR", "Estado")))
        val format = new java.text.SimpleDateFormat("dd/MM/yyyy hh:mm:ss a")

        matches
          .map { mr =>
            val pedigreeFuture = pedigreeDataRepository.getPedigreeMetaData(mr.pedigreeMatchResult.pedigree.idPedigree)
            val pedigree = Await.result(pedigreeFuture, Duration(100, SECONDS))
            val courtCaseFuture = pedigreeDataRepository.getCourtCase(mr.pedigreeMatchResult.pedigree.idCourtCase)
            val courtCase = Await.result(courtCaseFuture, Duration(100, SECONDS))

            writer.writeAll(List(List(courtCase.get.internalSampleCode, pedigree.get.pedigreeMetaData.name,
              mr.internalCode/*mr.pedigreeMatchResult.profile.globalCode.text*/, mr.pedigreeMatchResult.profile.categoryId.text,
              format.format(mr.pedigreeMatchResult.matchingDate.date), mr.pedigreeMatchResult.asInstanceOf[PedigreeCompatibilityMatch].compatibility.toString, mr.pedigreeMatchResult.pedigree.status)))

          }

        writer.close()
        Right(file.getName)
      } else {
        Left("No hay registros para exportar")
      }

  }

}

