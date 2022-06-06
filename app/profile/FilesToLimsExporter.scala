package profile

import java.io.File

import com.github.tototoshi.csv.{CSVWriter, DefaultCSVFormat}
import matching.{MatchResult, MatchStatus}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object FilesToLimsExporter {

  implicit object FilesToLimsExporterFormat extends DefaultCSVFormat {
    override val delimiter = '\t'
  }

  def createIngresoLimsArchive(result: Future[List[Profile]], exportProfilesPath : String) = {

    result map { profileList =>
      if (profileList.size > 0) {
        val folder = s"$exportProfilesPath${File.separator}"
        val folderFile = new File(folder)

        folderFile.mkdirs
        val file = new File(s"${folder}IngresoPerfiles.txt")
        val writer = CSVWriter.open(file)
        writer.writeAll(List(List("Sample Name",
          "GENis code",
          "Status",
          "DateTime")))

        val format = new java.text.SimpleDateFormat("dd/MM/yyyy hh:mm:ss a")

        //Tomamos como fecha de ingreso del perfil la fecha del primer analisis
        profileList.foreach { p =>
          val analisis = p.analyses.get.sortBy(_.date.date.getTime)
          writer.writeAll(List(List(p.internalSampleCode, p.globalCode.text, "INGBD", format.format(analisis.apply(0).date.date))))
        }

        writer.close()
        //    file
        Right(file.getName)
      } else {
        Left("No hay registros para las fechas seleccionadas")
      }
    }
  }

  def createMatchLimsArchive(matches : Future[Seq[MatchResult]], exportProfilesPath:String, profileRepo :ProfileRepository, currentInstanceLabCode : String) = {
    val folder = s"$exportProfilesPath${File.separator}"
    val folderFile = new File(folder)

    folderFile.mkdirs
    matches map { matchesList => {
      if (matchesList.size > 0) {
        val file = new File(s"${folder}MatchesFile.txt")

        val writer = CSVWriter.open(file)
        writer.writeAll(List(List("MatchId", "Sample GENis Code", "Sample Name", "MatchedSample GENis Code", "MatchedSample Name", "status", "Datetime")))
        val format = new java.text.SimpleDateFormat("dd/MM/yyyy hh:mm:ss a")

        //verificar que el primero pertenece a la instancia y si alguno de los dos es pending mandar eso
        matchesList
          .map { mr =>
            val leftProfileFuture = profileRepo.findByCode(mr.leftProfile.globalCode)
            val leftProfile = Await.result(leftProfileFuture, Duration(100, SECONDS))
            val rightProfileFuture = profileRepo.findByCode(mr.rightProfile.globalCode)
            val rigthProfile = Await.result(rightProfileFuture, Duration(100, SECONDS))
            val status = if(mr.leftProfile.status.equals(MatchStatus.pending) || mr.rightProfile.status.equals(MatchStatus.pending)) "Pending" else mr.leftProfile.status

            if(!leftProfile.get.deleted && !rigthProfile.get.deleted
             /* && mr.leftProfile.status.equals(MatchStatus.pending) && mr.rightProfile.status.equals(MatchStatus.pending)*/) {
              if (leftProfile.get.globalCode.text.contains("-"+currentInstanceLabCode+"-")) {
                writer.writeAll(List(List(mr._id.id, mr.leftProfile.globalCode.text, leftProfile.get.internalSampleCode, mr.rightProfile.globalCode.text, rigthProfile.get.internalSampleCode, status, format.format(mr.matchingDate.date))))
              } else {
                writer.writeAll(List(List(mr._id.id, mr.rightProfile.globalCode.text, rigthProfile.get.internalSampleCode, mr.leftProfile.globalCode.text, leftProfile.get.internalSampleCode, status, format.format(mr.matchingDate.date))))
              }
            }
          }

        writer.close()
        //file
        Right(file.getName)
      } else {
        Left("No hay registros para las fechas seleccionadas")
      }
    }

    }
  }

}

