package profile

import java.util.Date
import configdata.MatchingRule
import connections.FileInterconnection
import profile.GenotypificationByType.GenotypificationByType
import types.SampleCode

import scala.concurrent.Future

trait ProfileRepository {

  def get(id: SampleCode): Future[Option[Profile]]

  def getBy(
    user: String,
    isSuperUser: Boolean,
    internalSampleCode: Option[String] = None,
    categoryId: Option[String] = None,
    laboratory: Option[String] = None,
    hourFrom: Option[Date] = None,
    hourUntil: Option[Date] = None
  ): Future[List[Profile]]

  def getBetweenDates(
    hourFrom: Option[Date] = None,
    hourUntil: Option[Date] = None
  ): Future[List[Profile]]

  def findByCode(globalCode: SampleCode): Future[Option[Profile]]

  def add(profile: Profile): Future[SampleCode]

  def addElectropherogram(
    globalCode: SampleCode,
    analysisId: String,
    image: Array[Byte],
    name: String
  ): Future[Either[String, SampleCode]]

  def getElectropherogramsByCode(globalCode: SampleCode): Future[List[(String, String, String)]]

  def getElectropherogramImage(profileId: SampleCode, electropherogramId: String): Future[Option[Array[Byte]]]

  def getElectropherogramsByAnalysisId(profileId: SampleCode, analysisId: String): Future[List[FileUploadedType]]

  def getFullElectropherogramsByCode(globalCode: SampleCode): Future[List[FileInterconnection]]

  def getFullFilesByCode(globalCode: SampleCode): Future[List[FileInterconnection]]

  def addElectropherogramWithId(
    globalCode: SampleCode,
    analysisId: String,
    image: Array[Byte],
    name: String,
    id: String
  ): Future[Either[String, SampleCode]]

  def addFileWithId(
    globalCode: SampleCode,
    analysisId: String,
    image: Array[Byte],
    name: String,
    id: String
  ): Future[Either[String, SampleCode]]

  def getGenotyficationByCode(globalCode: SampleCode): Future[Option[GenotypificationByType]]

  def findByCodes(globalCodes: Seq[SampleCode]): Future[Seq[Profile]]

  def addAnalysis(
    _id: SampleCode,
    analysis: Analysis,
    genotypification: GenotypificationByType,
    labeledGenotypification: Option[Profile.LabeledGenotypification],
    matchingRules: Option[Seq[MatchingRule]],
    mismatches: Option[Profile.Mismatch]
  ): Future[SampleCode]

  def saveLabels(globalCode: SampleCode, labels: Profile.LabeledGenotypification): Future[SampleCode]

  def existProfile(globalCode: SampleCode): Future[Boolean]

  def delete(globalCode: SampleCode): Future[Either[String, SampleCode]]

  def getLabels(globalCode: SampleCode): Future[Option[Profile.LabeledGenotypification]]

  def updateAssocTo(globalCode: SampleCode, to: SampleCode): Future[(String, String, SampleCode)]

  def setMatcheableAndProcessed(globalCode: SampleCode): Future[Either[String, SampleCode]]

  def getUnprocessed(): Future[Seq[SampleCode]]

  def canDeleteKit(id: String): Future[Boolean]

  def updateProfile(profile: Profile): Future[SampleCode]

  def findByCodeWithoutAceptedLocus(globalCode: SampleCode, aceptedLocus: Seq[String]): Future[Option[Profile]]

  def addFile(
    globalCode: SampleCode,
    analysisId: String,
    image: Array[Byte],
    name: String
  ): Future[Either[String, SampleCode]]

  def getFileByCode(globalCode: SampleCode): Future[List[(String, String, String)]]

  def getFile(profileId: SampleCode, electropherogramId: String): Future[Option[Array[Byte]]]

  def getFileByAnalysisId(profileId: SampleCode, analysisId: String): Future[List[FileUploadedType]]

  def getFullElectropherogramsById(id: String): Future[List[FileInterconnection]]

  def getFullFilesById(id: String): Future[List[FileInterconnection]]

  def getProfilesMarkers(profiles: Array[Profile]): List[String]

  def removeFile(id: String): Future[Either[String, String]]

  def removeEpg(id: String): Future[Either[String, String]]

  def removeProfile(globalCode: SampleCode): Future[Either[String, String]]

  def getProfileOwnerByFileId(id: String): Future[(String, SampleCode)]

  def getProfileOwnerByEpgId(id: String): Future[(String, SampleCode)]

  def getAllProfiles(): Future[List[(SampleCode, String)]]
}
