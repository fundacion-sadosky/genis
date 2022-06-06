package profile

import java.util.Date
import java.io.File
import javax.inject.{Inject, Named, Singleton}

import matching.{MatchResult, MatchingRepository}
import play.api.Logger
import profiledata.ProfileDataService

import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import user.UserService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

trait LimsArchivesExporterService {
/*
  def filterProfiles(exportProfileFilters:ExportProfileFilters):Future[List[Profile]]
  def exportProfiles(profile:  List[Profile] ,user:String):Future[ Either[String, String]]
*/
  def exportLimsFiles(exportLimsFilesFilters:ExportLimsFilesFilter):Future[ Either[String, String]]
  def getFileOfAlta() : java.io.File
  def getFileOfMatch() : java.io.File
}

@Singleton
class LimsArchivesExporterServiceImpl @Inject()(userService:UserService,
                                           profileService: ProfileService,
                                           profileDataService: ProfileDataService,
                                           @Named("limsArchivesPath")  exportProfilesPath: String = "",
                                           @Named("generateLimsFiles") exportaALims: Boolean = false,
                                           @Named("labCode") val currentInstanceLabCode: String,
                                           profileRepository:ProfileRepository,
                                                matchingRepository: MatchingRepository   ) extends LimsArchivesExporterService {

  val logger = Logger(this.getClass())

  override def exportLimsFiles(exportLimsFilesFilters:ExportLimsFilesFilter):Future[ Either[String, String]] = {
    exportLimsFilesFilters match {
      case ExportLimsFilesFilter(user, isSuperUser, tipo, hourFrom, hourUntil) => {
        tipo match {
          case "alta" => {
            FilesToLimsExporter.createIngresoLimsArchive(filterProfiles(hourFrom, hourUntil), exportProfilesPath)
          }
          case  "match" => {
            FilesToLimsExporter.createMatchLimsArchive(filterMatches(hourFrom, hourUntil), exportProfilesPath, profileRepository, currentInstanceLabCode)
          }
        }
      }
    }
  }

  def filterProfiles(from: Option[Date], to: Option[Date]) : Future[List[Profile]] = {
    profileRepository.getBetweenDates(from, to).map(profiles => {
      profiles.filter(_.analyses.isDefined)
        .filter(p => {
          val analisis = p.analyses.get.sortBy(_.date.date.getTime)
          analisis.apply(0).date.date.after(from.get) && analisis.apply(0).date.date.before(to.get)})

    })


  }

  def filterMatches(from: Option[Date], to: Option[Date]) : Future[Seq[MatchResult]] = {
    matchingRepository.getByDateBetween(from, to)
  }

  override def getFileOfAlta(): File = {
    val file = new java.io.File(s"$exportProfilesPath${File.separator}IngresoPerfiles.txt")
    file
  }

  override def getFileOfMatch(): File = {
    val file = new java.io.File(s"$exportProfilesPath${File.separator}MatchesFile.txt")
    file
  }

}
