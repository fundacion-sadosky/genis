package profile

import java.io.File
import com.github.tototoshi.csv.{CSVWriter, DefaultCSVFormat}
import matching.{MatchResult, MatchStatus}
import profiledata.ProfileDataRepository
import types.SampleCode

import scala.collection.immutable.HashSet
import scala.collection.{immutable, mutable}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}

object FilesToLimsExporter {

  private val maximumTime:Int = 100
  implicit object FilesToLimsExporterFormat extends DefaultCSVFormat {
    override val delimiter = '\t'
  }

  private def getUploadedProfileCodes
    (codes: List[SampleCode])
      (implicit profileDataRepository: ProfileDataRepository)
  : Set[SampleCode] = {
    val a = Await
      .result(
        profileDataRepository
          findUploadedProfilesByCodes
          codes,
        Duration(maximumTime, SECONDS)
      )
      .toSet
    a
  }

  def createIngresoLimsArchive(
    result: Future[List[Profile]],
    profileDataRepository: ProfileDataRepository,
    exportProfilesPath: String
  ): Future[Either[String, String]] = {
    result map {
      profileList =>
        if (profileList.nonEmpty) {
          val profileGlobalCodes = profileList map {p => p.globalCode}
          implicit val repo = profileDataRepository
          val uploaded =
            getUploadedProfileCodes(profileGlobalCodes)
          val folder = s"$exportProfilesPath${File.separator}"
          val folderFile = new File(folder)
          folderFile.mkdirs
          val file = new File(s"${folder}IngresoPerfiles.txt")
          val writer = CSVWriter.open(file)
          writer.writeAll(
            List(
              List(
                "Sample Name",
                "GENis code",
                "Status",
                "DateTime",
                "Uploaded to Superior Instance"
              )
            )
          )
          val format = new java.text.SimpleDateFormat("dd/MM/yyyy hh:mm:ss a")
          // Tomamos como fecha de ingreso del
          // perfil la fecha del primer analisis
          profileList.foreach {
            p =>
              val analisis = p.analyses.get.sortBy(_.date.date.getTime)
              writer.writeAll(
                List(
                  List(
                    p.internalSampleCode,
                    p.globalCode.text,
                    "INGBD",
                    format.format(analisis.apply(0).date.date),
                    (uploaded contains p.globalCode).toString
                  )
                )
              )
          }
          writer.close()
          Right(file.getName)
        } else {
          Left("No hay registros para las fechas seleccionadas")
        }
    }
  }

  def createMatchLimsArchive(
    matches: Future[Seq[MatchResult]],
    exportProfilesPath: String,
    profileRepo: ProfileRepository,
    profileDataRepository: ProfileDataRepository,
    currentInstanceLabCode: String
  ): Future[Either[String, String]] = {
    val folder = s"$exportProfilesPath${File.separator}"
    val folderFile = new File(folder)
    folderFile.mkdirs
    matches map {
      matchesList => {
        if (matchesList.nonEmpty) {
          val profileGlobalCodes = matchesList map {
            p => p.leftProfile.globalCode
          }
          implicit val repo = profileDataRepository
          val uploaded =
            getUploadedProfileCodes(profileGlobalCodes.toList)
          val file = new File(s"${folder}MatchesFile.txt")
          val writer = CSVWriter.open(file)
          writer.writeAll(
            List(
              List(
                "MatchId",
                "Sample GENis Code",
                "Sample Name",
                "MatchedSample GENis Code",
                "MatchedSample Name",
                "status",
                "Datetime",
                "Sample Profile Uploaded to Superior Instance",
                "Matched Profile Uploaded to Superior Instance"
              )
            )
          )
          val format = new java.text.SimpleDateFormat("dd/MM/yyyy hh:mm:ss a")

          // verificar que el primero pertenece a la instancia
          // y si alguno de los dos es pending mandar eso
          matchesList
            .foreach {
              mr =>
                val leftProfileFuture = profileRepo.findByCode(
                  mr.leftProfile.globalCode
                )
                val responseMaxDuration = 100
                val leftProfile = {
                  Await.result(
                    leftProfileFuture, Duration(responseMaxDuration, SECONDS)
                  )
                }
                val rightProfileFuture = profileRepo.findByCode(
                  mr.rightProfile.globalCode
                )
                val rigthProfile = Await.result(
                  rightProfileFuture, Duration(responseMaxDuration, SECONDS)
                )
                val status = if (
                  mr.leftProfile.status.equals(MatchStatus.pending) ||
                  mr.rightProfile.status.equals(MatchStatus.pending)
                )
                  {
                    "Pending"
                  }
                else
                  {
                    mr.leftProfile.status
                  }

                if (!leftProfile.get.deleted && !rigthProfile.get.deleted
                /*
                  && mr.leftProfile.status.equals(MatchStatus.pending) &&
                  mr.rightProfile.status.equals(MatchStatus.pending)*/
                ) {
                  val leftContainsInstanceCode =
                    leftProfile
                      .get
                      .globalCode
                      .text
                      .contains(s"-$currentInstanceLabCode-")
                  if (leftContainsInstanceCode) {
                    writer.writeAll(
                      List(
                        List(
                          mr._id.id,
                          mr.leftProfile.globalCode.text,
                          leftProfile.get.internalSampleCode,
                          mr.rightProfile.globalCode.text,
                          rigthProfile.get.internalSampleCode,
                          status,
                          format.format(mr.matchingDate.date),
                          uploaded.contains(mr.leftProfile.globalCode),
                          uploaded.contains(mr.rightProfile.globalCode)
                        )
                      )
                    )
                  } else {
                    writer.writeAll(
                      List(
                        List(
                          mr._id.id,
                          mr.rightProfile.globalCode.text,
                          rigthProfile.get.internalSampleCode,
                          mr.leftProfile.globalCode.text,
                          leftProfile.get.internalSampleCode,
                          status,
                          format.format(mr.matchingDate.date),
                          uploaded.contains(mr.leftProfile.globalCode),
                          uploaded.contains(mr.rightProfile.globalCode)
                        )
                      )
                    )
                  }
                }
            }
            writer.close()
            Right(file.getName)
          } else {
          Left("No hay registros para las fechas seleccionadas")
        }
      }
    }
  }
}
