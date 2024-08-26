package connections

import java.util.Date

import matching.MatchResult
import play.api.libs.json.Json
import profile.Profile
import types.AlphanumericId

case class Connection(
                       superiorInstance: String,
                       pki: String
                     )

object Connection {
  implicit val format = Json.format[Connection]
}


case class SuperiorProfileData(category: String,
                               bioMaterialType: Option[String],
                               assignee: String,
                               laboratoryDescription: Option[String],
                               laboratory: String,
                               laboratoryOrigin: String,
                               laboratoryImmediate: String,
                               responsibleGeneticist: Option[String],
                               internalSampleCode: String,
                               profileExpirationDate: Option[Date],
                               sampleDate: Option[Date],
                               sampleEntryDate: Option[Date])

object SuperiorProfileData {
  implicit val format = Json.format[SuperiorProfileData]
}

case class MatchSuperiorInstance(
                       matchResult: MatchResult,
                       superiorProfile: Profile,
                       superiorProfileData: SuperiorProfileData,
                       superiorProfileAssociated: Option[Profile]
                     )

object MatchSuperiorInstance {
  implicit val format = Json.format[MatchSuperiorInstance]
}

case class SuperiorProfileInfo(superiorProfileData:SuperiorProfileData,profile:Profile)
object SuperiorProfileInfo {
  implicit val format = Json.format[SuperiorProfileInfo]
}

case class ProfileTransfer(profile:Profile,profileAssociated:Option[Profile] = None)
object ProfileTransfer {
  implicit val format = Json.format[ProfileTransfer]
}

case class FileInterconnection(id:String,
                               profileId:String,
                               analysisId:String,
                               name:Option[String],
                               typeFile:String,
                               content:String)

object FileInterconnection {
  implicit val format = Json.format[FileInterconnection]
}