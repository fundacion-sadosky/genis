package profile

import matching.{MatchResult, MatchingRepository}
import play.api.Logger
import profiledata.{ProfileDataRepository, ProfileDataService}
import user.UserService

import java.io.File
import java.util.Date
import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

trait LimsArchivesExporterService {
/*
  def filterProfiles(exportProfileFilters:ExportProfileFilters):Future[List[Profile]]
  def exportProfiles(profile: List[Profile], user:String):Future[Either[String, String]]
*/
  def exportLimsFiles(
    exportLimsFilesFilters:ExportLimsFilesFilter
  ):Future[Either[String, String]]
  def getFileOfAlta: java.io.File
  def getFileOfMatch : java.io.File
}

@Singleton
class LimsArchivesExporterServiceImpl @Inject() (
  userService:UserService,
  profileService: ProfileService,
  profileDataService: ProfileDataService,
  profileDataRepository: ProfileDataRepository,
  @Named("limsArchivesPath")  exportProfilesPath: String = "",
  @Named("generateLimsFiles") exportaALims: Boolean = false,
  @Named("labCode") val currentInstanceLabCode: String,
  profileRepository:ProfileRepository,
  matchingRepository: MatchingRepository
) extends LimsArchivesExporterService {
  val logger: Logger = Logger(this.getClass)
  override def exportLimsFiles(
    exportLimsFilesFilters:ExportLimsFilesFilter
  ): Future[Either[String, String]] = {
    exportLimsFilesFilters match {
      case ExportLimsFilesFilter(_, _, tipo, hourFrom, hourUntil) =>
        tipo match {
          case "alta" =>
            FilesToLimsExporter.createIngresoLimsArchive(
              filterProfiles(hourFrom, hourUntil),
              profileDataRepository,
              exportProfilesPath
            )
          case "match" =>
            FilesToLimsExporter.createMatchLimsArchive(
              filterMatches(hourFrom, hourUntil),
              exportProfilesPath,
              profileRepository,
              profileDataRepository,
              currentInstanceLabCode
            )
          // This code should not be reached.
          case _ => Future{
            Left("Lims can be exported using 'alta' and 'match' options only.")
          }
        }
    }
  }

  def filterProfiles(
    from: Option[Date],
    to: Option[Date]
  ) : Future[List[Profile]] = {
    profileRepository
      .getBetweenDates(from, to)
      .map(
        profiles => {
          profiles
          .filter(_.analyses.isDefined)
          .filter(
            p => {
              val analisis = p.analyses.get.sortBy(_.date.date.getTime)
              analisis.head.date.date.after(from.get) &&
                analisis.head.date.date.before(to.get)
            }
         )
      }
    )
  }

  private def filterMatches(
    from: Option[Date], to: Option[Date]
  ) : Future[Seq[MatchResult]] = {
    matchingRepository.getByDateBetween(from, to)
  }

  override def getFileOfAlta: File = {
    new java.io.File(
      s"$exportProfilesPath${File.separator}IngresoPerfiles.txt"
    )
  }

  override def getFileOfMatch: File = {
    new java.io.File(
      s"$exportProfilesPath${File.separator}MatchesFile.txt"
    )
  }
}
