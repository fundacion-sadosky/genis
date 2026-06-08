package fixtures

import configdata.CategoryService
import connections.FileInterconnection
import kits.AnalysisType
import matching.Stringency
import profile.*
import profile.GenotypificationByType.GenotypificationByType
import profiledata.{ProfileData, ProfileDataRepository, ProfileDataService}
import types.{AlphanumericId, SampleCode}

import java.io.File
import scala.concurrent.Future

class StubProfileService extends ProfileService {
  val sc = SampleCode("AR-B-IMBICE-1")
  val catId = AlphanumericId("SOSPECHOSO")
  val genot: GenotypificationByType = Map(1 -> Map("CSF1PO" -> List(AlleleValue("10"))))

  val sampleProfile = Profile(
    _id = sc, globalCode = sc, internalSampleCode = "ISC-001",
    assignee = "user1", categoryId = catId, genotypification = genot,
    analyses = None, labeledGenotypification = None, contributors = Some(1),
    mismatches = None, deleted = false, matcheable = true, isReference = false, processed = false
  )

  var createResult: Future[Either[List[String], Profile]] = Future.successful(Right(sampleProfile))
  var findByCodeResult: Future[Option[Profile]] = Future.successful(Some(sampleProfile))
  var findByCodesResult: Future[Seq[Profile]] = Future.successful(Seq(sampleProfile))
  var getEpgsByCodeResult: Future[List[FileUploadedType]] = Future.successful(List(FileUploadedType("epg1", "epg.png")))
  var getEpgImageResult: Future[Option[Array[Byte]]] = Future.successful(Some(Array[Byte](1, 2, 3)))
  var getEpgsByAnalysisResult: Future[List[FileUploadedType]] = Future.successful(List.empty)
  var profileModelViewResult: Future[ProfileModelView] = Future.successful(
    ProfileModelView(Some(sc), Some(sc), Some(catId), Some(genot), Nil, None, Some(1), None,
      associable = true, labelable = false, editable = true, isReference = false, readOnly = false, isUploadedToSuperior = false)
  )
  var saveEpgsResult: Future[List[Either[String, SampleCode]]] = Future.successful(List(Right(sc)))
  var verifyMixtureResult: Future[Either[String, ProfileAsociation]] =
    Future.successful(Right(ProfileAsociation(sc, Stringency.HighStringency, Map("CSF1PO" -> List(AlleleValue("10"))))))
  var saveLabelsResult: Future[Either[List[String], SampleCode]] = Future.successful(Right(sc))
  var getLabelsResult: Future[Option[Profile.LabeledGenotypification]] = Future.successful(None)
  var labelsSets: Profile.LabelSets = Map("set1" -> Map("1" -> Label("1", "Víctima")))
  var getFilesByCodeResult: Future[List[FileUploadedType]] = Future.successful(List.empty)
  var getFileResult: Future[Option[Array[Byte]]] = Future.successful(Some(Array[Byte](4, 5, 6)))
  var getFilesByAnalysisResult: Future[List[FileUploadedType]] = Future.successful(List.empty)
  var saveFileResult: Future[List[Either[String, SampleCode]]] = Future.successful(List(Right(sc)))
  var isReadOnlyResult: Future[(Boolean, String)] = Future.successful((false, ""))
  var removeFileResult: Future[Either[String, String]] = Future.successful(Right("ok"))
  var removeEpgResult: Future[Either[String, String]] = Future.successful(Right("ok"))
  var removeProfileResult: Future[Either[String, String]] = Future.successful(Right("ok"))

  override def create(na: NewAnalysis, savePictures: Boolean, replicate: Boolean) = createResult
  override def findByCode(gc: SampleCode) = findByCodeResult
  override def get(id: SampleCode) = findByCodeResult
  override def getElectropherogramsByCode(gc: SampleCode) = getEpgsByCodeResult
  override def getElectropherogramImage(pid: SampleCode, eid: String) = getEpgImageResult
  override def getElectropherogramsByAnalysisId(pid: SampleCode, aid: String) = getEpgsByAnalysisResult
  override def getProfileModelView(gc: SampleCode) = profileModelViewResult
  override def saveElectropherograms(t: String, gc: SampleCode, ia: String, n: String) = saveEpgsResult
  override def verifyMixtureAssociation(mg: GenotypificationByType, gc: SampleCode, sid: AlphanumericId) = verifyMixtureResult
  override def saveLabels(gc: SampleCode, l: Profile.LabeledGenotypification, u: String) = saveLabelsResult
  override def findByCodes(gcs: List[SampleCode]) = findByCodesResult
  override def getLabels(gc: SampleCode) = getLabelsResult
  override def validateAnalysis(a: Profile.Genotypification, c: AlphanumericId, k: Option[String], cont: Int, t: Option[Int], at: AnalysisType) =
    Future.successful(Right(configdata.CategoryConfiguration()))
  override def existProfile(gc: SampleCode) = Future.successful(true)
  override def importProfile(pd: ProfileData, na: NewAnalysis, rep: Boolean) = createResult
  override def getAssociatedProfiles(p: Profile) = Future.successful(Seq.empty)
  override def getLabelsSets() = labelsSets
  override def validProfilesAssociated(l: Option[Profile.LabeledGenotypification]) = Seq.empty
  override def isExistingKit(k: String) = Future.successful(true)
  override def isExistingCategory(id: AlphanumericId) = Future.successful(true)
  override def addProfile(p: Profile) = Future.successful(sc)
  override def fireMatching(sc: SampleCode) = ()
  override def updateProfile(p: Profile) = Future.successful(sc)
  override def getAnalysisType(k: Option[String], t: Option[Int]) = Future.successful(AnalysisType(1, "Autosomal"))
  override def validateExistingLocusForKit(a: Profile.Genotypification, k: Option[String]) = Future.successful(Right(()))
  override def findProfileLocalOrSuperior(gc: SampleCode) = findByCodeResult
  override def findProfileDataLocalOrSuperior(gc: SampleCode) = Future.successful(None)
  override def getFilesByCode(gc: SampleCode) = getFilesByCodeResult
  override def getFile(pid: SampleCode, fid: String) = getFileResult
  override def getFilesByAnalysisId(pid: SampleCode, aid: String) = getFilesByAnalysisResult
  override def saveFile(t: String, gc: SampleCode, ia: String, n: String) = saveFileResult
  override def isReadOnly(p: Option[Profile], u: Boolean, a: Boolean) = isReadOnlyResult
  override def isReadOnlySampleCode(gc: SampleCode, u: Boolean, a: Boolean) = isReadOnlyResult
  override def isReadOnly2(p: Option[Profile]) = Future.successful((false, "", false))
  override def getFullElectropherogramsByCode(gc: SampleCode) = Future.successful(List.empty)
  override def getFullFilesByCode(gc: SampleCode) = Future.successful(List.empty)
  override def addElectropherogramWithId(gc: SampleCode, aid: String, img: Array[Byte], n: String, id: String) = Future.successful(Right(sc))
  override def addFileWithId(gc: SampleCode, aid: String, img: Array[Byte], n: String, id: String) = Future.successful(Right(sc))
  override def getFullElectropherogramsById(id: String) = Future.successful(List.empty)
  override def getFullFilesById(id: String) = Future.successful(List.empty)
  override def removeFile(id: String, user: String) = removeFileResult
  override def removeEpg(id: String, user: String) = removeEpgResult
  override def removeProfile(gc: SampleCode) = removeProfileResult
  override def profilesAll() = Future.successful(List.empty)
}

class StubProfileExporterService extends ProfileExporterService {
  var filterResult: Future[List[Profile]] = Future.successful(List.empty)
  var exportResult: Future[Either[String, String]] = Future.successful(Right("export-file"))
  var exportFile: File = File.createTempFile("test-export", ".csv")

  override def filterProfiles(f: ExportProfileFilters) = filterResult
  override def exportProfiles(profiles: List[Profile], user: String) = exportResult
  override def getFileOf(user: String) = exportFile
}

class StubLimsArchivesExporterService extends LimsArchivesExporterService {
  var exportResult: Future[Either[String, String]] = Future.successful(Right("lims-export"))
  var altaFile: File = File.createTempFile("test-alta", ".csv")
  var matchFile: File = File.createTempFile("test-match", ".csv")

  override def exportLimsFiles(filter: ExportLimsFilesFilter) = exportResult
  override def getFileOfAlta = altaFile
  override def getFileOfMatch = matchFile
}

import jakarta.inject.Singleton
import models.Tables.{ProfileReceivedRow, ProfileSentRow, ProfileUploadedRow, ExternalProfileDataRow}
import profile.MtRCRS
import profiledata.*
import types.SampleCode

@Singleton
class StubProfileDataService extends ProfileDataService:
  var getMtRcrsResult: Future[MtRCRS]                                    = Future.successful(MtRCRS(Map.empty))
  var createResult: Future[Either[String, SampleCode]]                   = Future.successful(Left("stub"))
  var updateProfileDataResult: Future[Boolean]                           = Future.successful(false)
  var updateProfileCategoryDataResult: Future[Option[String]]            = Future.successful(None)
  var getByCodeResult: Future[Option[ProfileData]]                       = Future.successful(None)
  // alias used by ProtoProfileDataControllerTest
  def getResult_=(v: Future[Option[ProfileData]]): Unit = getByCodeResult = v
  def getResult: Future[Option[ProfileData]] = getByCodeResult
  var getByIdResult: Future[(ProfileData, configdata.Group, configdata.Category)] =
    Future.failed(new UnsupportedOperationException("stub"))
  var getResourceResult: Future[Option[Array[Byte]]]                     = Future.successful(None)
  var findByCodeResult: Future[Option[ProfileData]]                      = Future.successful(None)
  var findByCodesResult: Future[Seq[ProfileData]]                        = Future.successful(Seq.empty)
  var findByCodeWithAssociationsResult: Future[Option[(ProfileData, configdata.Group, configdata.FullCategory)]] =
    Future.successful(None)
  var isEditableResult: Future[Option[Boolean]]                          = Future.successful(Some(true))
  var isDesktopProfileResult: Future[Option[Boolean]]                    = Future.successful(None)
  var getDeleteMotiveResult: Future[Option[DeletedMotive]]               = Future.successful(None)
  var deleteProfileResult: Future[Either[String, SampleCode]]            = Future.successful(Left("stub"))
  var deleteResult: Future[Either[String, SampleCode]]                   = Future.successful(Left("stub"))
  var removeProfileResult: Future[Either[String, SampleCode]]            = Future.successful(Right(SampleCode("AR-C-SHDG-1")))
  var getDesktopProfilesResult: Future[Seq[SampleCode]]                  = Future.successful(Seq.empty)
  var countProfilesResult: Future[Int]                                   = Future.successful(0)

  override def getMtRcrs()                                                                                                = getMtRcrsResult
  override def create(pd: ProfileDataAttempt)                                                                            = createResult
  override def updateProfileData(gc: SampleCode, pd: ProfileDataAttempt, allow: Boolean = false)                        = updateProfileDataResult
  override def updateProfileCategoryData(gc: SampleCode, pd: ProfileDataAttempt, u: String)                             = updateProfileCategoryDataResult
  override def get(sc: SampleCode)                                                                                       = getByCodeResult
  override def get(id: Long)                                                                                             = getByIdResult
  override def getResource(rt: String, id: Long)                                                                        = getResourceResult
  override def findByCode(gc: SampleCode)                                                                               = findByCodeResult
  override def findByCodeWithoutDetails(gc: SampleCode)                                                                 = findByCodeResult
  override def findByCodes(codes: List[SampleCode])                                                                     = findByCodesResult
  override def findByCodeWithAssociations(gc: SampleCode)                                                               = findByCodeWithAssociationsResult
  override def findProfileDataLocalOrSuperior(gc: SampleCode)                                                           = findByCodeResult
  override def isEditable(sc: SampleCode, allow: Boolean = false)                                                       = isEditableResult
  override def isDesktopProfile(gc: SampleCode)                                                                         = isDesktopProfileResult
  override def getDeleteMotive(sc: SampleCode)                                                                          = getDeleteMotiveResult
  override def deleteProfile(gc: SampleCode, m: DeletedMotive, u: String, v: Boolean = true)                           = deleteProfileResult
  override def delete(gc: SampleCode)                                                                                   = deleteResult
  override def removeAll()                                                                                               = Future.successful(0)
  override def removeProfile(gc: SampleCode)                                                                            = removeProfileResult
  override def getDesktopProfiles()                                                                                     = getDesktopProfilesResult
  override def importFromAnotherInstance(pd: ProfileData, lo: String, li: String)                                       = Future.successful(())
  override def updateUploadStatus(gc: String, s: Long, m: Option[String], ie: Option[String], u: Option[String], op: String) = Future.successful(Right(()))
  override def getProfileUploadStatusByGlobalCode(gc: SampleCode)                                                       = Future.successful(None)
  override def getExternalProfileDataByGlobalCode(gc: String)                                                           = Future.successful(None)
  override def gefFailedProfilesUploaded()                                                                               = Future.successful(Seq.empty)
  override def gefFailedProfilesUploadedDeleted()                                                                       = Future.successful(Seq.empty)
  override def gefFailedProfilesSentDeleted(lc: String)                                                                 = Future.successful(Seq.empty)
  override def updateProfileSentStatus(gc: String, s: Long, m: Option[String], lc: String, ie: Option[String], u: Option[String]) = Future.successful(Right(()))
  override def updateInterconnectionError(gc: String, s: Long, ie: String)                                              = Future.successful(Right(()))
  override def addProfileReceivedApproved(lc: String, gc: String, s: Long, u: String, cm: Boolean)                     = Future.successful(Right(()))
  override def addProfileReceivedRejected(lc: String, gc: String, s: Long, m: String, u: String, cm: Boolean)          = Future.successful(Right(()))
  override def updateProfileReceivedStatus(lc: String, gc: String, s: Long, m: String, cm: Boolean, ie: String, u: Option[String], op: String) = Future.successful(Right(()))
  override def getPendingApprovalNotification(lc: String)                                                               = Future.successful(Seq.empty)
  override def getPendingRejectionNotification(lc: String)                                                              = Future.successful(Seq.empty)
  override def gefFailedProfilesReceivedDeleted(lc: String)                                                             = Future.successful(Seq.empty)
  override def shouldSendDeleteToSuperiorInstance(gc: SampleCode)                                                       = Future.successful(false)
  override def shouldSendDeleteToInferiorInstance(gc: SampleCode)                                                       = Future.successful(false)
  override def getLabFromGlobalCode(gc: SampleCode)                                                                     = None
  override def getIsProfileReplicatedInternalCode(ic: String)                                                           = Future.successful(false)
  override def getProfileReceivedLabCode(gc: SampleCode)                                                                = Future.successful(None)
  override def countProfiles()                                                                                           = countProfilesResult
