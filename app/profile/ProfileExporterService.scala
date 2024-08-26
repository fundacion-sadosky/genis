package profile


import java.io.File
import javax.inject.{Inject, Named, Singleton}

import play.api.Logger
import profiledata.ProfileDataService

import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import user.UserService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

trait ProfileExporterService {
  def filterProfiles(exportProfileFilters:ExportProfileFilters):Future[List[Profile]]
  def exportProfiles(profile:  List[Profile] ,user:String):Future[ Either[String, String]]
  def getFileOf(user: String) : java.io.File
}

@Singleton
class ProfileExporterServiceImpl @Inject()(userService:UserService,
                                           profileService: ProfileService,
                                           profileDataService: ProfileDataService,
                                           @Named("exportProfilesPageSize") val exportProfilesPageSize: Int,
                                           @Named("exportProfilesPath") val exportProfilesPath: String,
                                           profileRepository:ProfileRepository) extends ProfileExporterService {

  val logger = Logger(this.getClass())

  def filterProfiles(exportProfileFilters:ExportProfileFilters):Future[List[Profile]] = {
    exportProfileFilters match {
      case ExportProfileFilters(user,isSuperUser,internalSampleCode,categoryId,laboratory,hourFrom,hourUntil) => {
        profileRepository.getBy(user,isSuperUser,internalSampleCode,categoryId,laboratory,hourFrom,hourUntil).map(profiles => {
          profiles.filter(_.analyses.isDefined).map(profile => {
            if(profile.analyses.get.exists(x => x.kit == "Manual")){
              profile.copy(analyses = profile.analyses.map(list => list.filter(_.kit != "Manual")))
            }else{
              profile
            }
          })
        })
      }
    }
  }
  def exportProfiles(profiles:  List[Profile],user:String ): Future[Either[String, String]] = {
    val assignees:Set[String] = profiles.map(_.assignee).toSet
    val mitoPositions = Await.result(profileDataService.getMtRcrs(), Duration.Inf)
    Future.sequence(assignees.map(assignee =>
      userService.getUserOrEmpty(assignee).map{case Some(userView) => (assignee,userView.geneMapperId)
                                              case None => (assignee,"assignee")}))
  //userView => (assignee,userView.geneMapperId))))
      .map(map => map.toMap)
      .map(genneMapperByAssignees => {
        ProfileExporter.export(profiles.map(profile => (profile,genneMapperByAssignees.getOrElse(profile.assignee,"assignee"))),
          user,
          exportProfilesPageSize,
          exportProfilesPath, mitoPositions
        )
    })

  }

  override def getFileOf(user: String): File = {
    val file = new java.io.File(s"$exportProfilesPath$user${File.separator}GENisExport.zip")
    file
  }
}
