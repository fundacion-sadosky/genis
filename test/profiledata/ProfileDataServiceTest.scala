package profiledata

import java.io.File

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import configdata.BioMaterialType
import configdata.BioMaterialTypeService
import configdata.CategoryService
import configdata.Crime
import configdata.CrimeType
import configdata.CrimeTypeService
import inbox.NotificationService
import laboratories.Laboratory
import laboratories.LaboratoryService
import matching.{MatchingResults, MatchingService}
import pedigree.PedigreeService
import play.api.libs.Files.TemporaryFile
import profile.{ProfileRepository, ProfileService, ProfileServiceImpl}
import scenarios.ScenarioRepository
import services.CacheService
import services.TemporaryAssetKey
import specs.PdgSpec
import stubs.Stubs
import trace.{Trace, TraceService}
import types.AlphanumericId
import types.Email
import types.SampleCode

class ProfileDataServiceTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)
  val sampleCode = new SampleCode("AR-C-SHDG-1")
  val pda = Stubs.profileDataAttempt
  val pd = Stubs.profileData
  val optReturn: Future[Option[ProfileData]] = Future.successful(Some(pd))
  val seqBioMat = Seq(BioMaterialType(AlphanumericId("SANGRE"), "sangre", None), BioMaterialType(AlphanumericId("Piel"), "piel", None))
  val ct = CrimeType("PERSONAS", "contra las personas", None, Seq(Crime("ASESINATO", "Asesinato", None)))
  val mapCrimeType = Map("PERSONAS" -> ct)
  val labo = Laboratory("Servicio de Huellas Digitales", "SHDG", "AR", "C", "address 123", "541112345678", Email("mail@example.com"), 0, 0)

  val globalCode = SampleCode("AR-C-HIBA-500")
  val profileRepo = mock[ProfileRepository]
  when(profileRepo.delete(globalCode)).thenReturn(Future.successful(Right(globalCode)))

  val matchingServiceMock = mock[MatchingService]

  "ProfileDataService" must {
    "delete a profile and all his matches" in {
      when(matchingServiceMock.findMatchingResults(globalCode)).thenReturn(Future.successful(None))

      val mockProfileDataRepository = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(null, mockProfileDataRepository, null, null, null, null, null, matchingServiceMock, null, profileRepo, Stubs.traceServiceMock, "SHDG", "AR", "C")

      val res = Await.result(service.delete(globalCode), duration)

      res.isRight mustBe true
      res.right.get mustBe globalCode
    }

    "accept a ProfileData and return a sampleCode" in {

      val mockCacheService = mock[CacheService]
      when(mockCacheService.get(TemporaryAssetKey(pda.dataFiliation.get.picture))).thenReturn(Option(List[TemporaryFile]()))
      when(mockCacheService.get(TemporaryAssetKey(pda.dataFiliation.get.inprint))).thenReturn(Option(List[TemporaryFile]()))
      when(mockCacheService.get(TemporaryAssetKey(pda.dataFiliation.get.signature))).thenReturn(Option(List[TemporaryFile]()))

      val mockProfileDataRepository = mock[ProfileDataRepository]
      when(mockProfileDataRepository.add(any[ProfileData], any[String], any[Option[List[File]]], any[Option[List[File]]], any[Option[List[File]]])).thenReturn(Future.successful(sampleCode))
      when(mockProfileDataRepository.giveGlobalCode("SHDG")).thenReturn(Future.successful("AR-C-SHDG"))

      val mockNotiService = mock[NotificationService]

      val target: ProfileDataService = new ProfileDataServiceImpl(mockCacheService, mockProfileDataRepository, null, mockNotiService, null, null, null, null, null, null, Stubs.traceServiceMock, "SHDG", "AR", "C")

      val result: Either[String, SampleCode] = Await.result(target.create(pda), duration)

      result.isRight mustBe true

      val globalCode = result.right.get
      globalCode mustBe sampleCode
    }

    "create a ProfileData and trace action" in {
      val traceService = mock[TraceService]
      when(traceService.add(any[Trace])).thenReturn(Future.successful(Right(1l)))

      val mockCacheService = mock[CacheService]
      when(mockCacheService.get(TemporaryAssetKey(pda.dataFiliation.get.picture))).thenReturn(Option(List[TemporaryFile]()))
      when(mockCacheService.get(TemporaryAssetKey(pda.dataFiliation.get.inprint))).thenReturn(Option(List[TemporaryFile]()))
      when(mockCacheService.get(TemporaryAssetKey(pda.dataFiliation.get.signature))).thenReturn(Option(List[TemporaryFile]()))

      val mockProfileDataRepository = mock[ProfileDataRepository]
      when(mockProfileDataRepository.add(any[ProfileData], any[String], any[Option[List[File]]], any[Option[List[File]]], any[Option[List[File]]])).thenReturn(Future.successful(sampleCode))
      when(mockProfileDataRepository.giveGlobalCode("SHDG")).thenReturn(Future.successful("AR-C-SHDG"))

      val mockNotiService = mock[NotificationService]

      val target: ProfileDataService = new ProfileDataServiceImpl(mockCacheService, mockProfileDataRepository, null, mockNotiService, null, null, null, null, null, null, traceService, "SHDG", "AR", "C")

      Await.result(target.create(pda), duration)

      //Se agrega el thread sleep porque dentro del create hay un promise onSuccess
      Thread.sleep(2000)

      verify(traceService).add(any[Trace])
    }

    "get a ProfileData by sampleCode" in {

      val mockCacheService = mock[CacheService]
      val mockProfileDataRepository = mock[ProfileDataRepository]
      when(mockProfileDataRepository.findByCode(sampleCode)).thenReturn(optReturn)

      val mockNotiService = mock[NotificationService]

      val mockCategoryService = mock[CategoryService]
      when(mockCategoryService.listCategories).thenReturn(Stubs.categoryMap)

      val bioMatService = mock[BioMaterialTypeService]
      when(bioMatService.list).thenReturn(Future.successful(seqBioMat))

      val crimeType = mock[CrimeTypeService]
      when(crimeType.list).thenReturn(Future.successful(mapCrimeType))

      val labService = mock[LaboratoryService]
      when(labService.list).thenReturn(Future.successful(Seq(labo)))

      val target: ProfileDataService = new ProfileDataServiceImpl(mockCacheService,
        mockProfileDataRepository, mockCategoryService,
        mockNotiService, bioMatService, crimeType, labService, null, null, null, Stubs.traceServiceMock, "", "", "")

      val recived = Await.result(target.findByCode(sampleCode), duration).get

      recived.laboratory mustBe "Servicio de Huellas Digitales"
      recived.crimeType.get mustBe "contra las personas"
      recived.bioMaterialType.get mustBe "sangre"
    }
    
    "get several ProfileData by sampleCodes" in {

      val mockCacheService = mock[CacheService]
      val mockProfileDataRepository = mock[ProfileDataRepository]
      
      val optReturn2: Future[Seq[ProfileData]] = Future.successful(Seq(Stubs.profileData, Stubs.profileData2nd))
      
      when(mockProfileDataRepository.findByCodes(List(SampleCode("AR-C-SHDG-1"), SampleCode("AR-C-SHDG-2")))).thenReturn(optReturn2)

      val mockNotiService = mock[NotificationService]

      val mockCategoryService = mock[CategoryService]
      when(mockCategoryService.listCategories).thenReturn(Stubs.categoryMap)

      val bioMatService = mock[BioMaterialTypeService]
      when(bioMatService.list).thenReturn(Future.successful(seqBioMat))

      val crimeType = mock[CrimeTypeService]
      when(crimeType.list).thenReturn(Future.successful(mapCrimeType))

      val labService = mock[LaboratoryService]
      when(labService.list).thenReturn(Future.successful(Seq(labo)))

      val target: ProfileDataService = new ProfileDataServiceImpl(mockCacheService,
        mockProfileDataRepository, mockCategoryService,
        mockNotiService, bioMatService, crimeType, labService, null, null, null, Stubs.traceServiceMock, "", "", "")

      val recived = Await.result(target.findByCodes(List(SampleCode("AR-C-SHDG-1"), SampleCode("AR-C-SHDG-2"))), duration)

      recived.length mustBe 2      
      recived(0).laboratory mustBe "Servicio de Huellas Digitales"
      recived(0).crimeType.get mustBe "contra las personas"
      recived(0).bioMaterialType.get mustBe "sangre"
    }

    "delete a profile - succesful" in {
      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.getTotalProfilesOccurenceInCase(any[SampleCode])).thenReturn(Future.successful(0))
      when(pedigreeService.countActivePedigreesByProfile(any[String])).thenReturn(Future.successful(0))
      when(pedigreeService.getTotalProfilesPedigreeMatches(any[SampleCode])).thenReturn(Future.successful(0))
      when(pedigreeService.getTotalProfileNumberOfMatches(any[SampleCode])).thenReturn(Future.successful(0))

      when(profileRepo.delete(any[SampleCode])).thenReturn(Future.successful(Right(Stubs.sampleCode)))

      when(matchingServiceMock.findMatchingResults(Stubs.sampleCode)).thenReturn(Future.successful(None))

      val profileDataRepository = mock[ProfileDataRepository]
      when(profileDataRepository.delete(any[SampleCode], any[DeletedMotive])).thenReturn(Future.successful(1))

      val scenarioRepository = mock[ScenarioRepository]
      when(scenarioRepository.getByProfile(any[SampleCode])).thenReturn(Future.successful(Seq()))

      val target = new ProfileDataServiceImpl(null, profileDataRepository, null, null, null, null, null, matchingServiceMock, scenarioRepository, profileRepo, Stubs.traceServiceMock, null, null, null,null,null,pedigreeService)

      val result = Await.result(target.deleteProfile(Stubs.sampleCode, DeletedMotive("jerkovicm", "motive"), "pdg",false), duration)

      result.isRight mustBe true
    }

    "delete a profile - failure E0118" in {
      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.getTotalProfilesOccurenceInCase(any[SampleCode])).thenReturn(Future.successful(0))
      when(pedigreeService.countActivePedigreesByProfile(any[String])).thenReturn(Future.successful(0))
      when(pedigreeService.getTotalProfilesPedigreeMatches(any[SampleCode])).thenReturn(Future.successful(0))
      when(pedigreeService.getTotalProfileNumberOfMatches(any[SampleCode])).thenReturn(Future.successful(0))

      val scenarioRepository = mock[ScenarioRepository]
      when(scenarioRepository.getByProfile(any[SampleCode])).thenReturn(Future.successful(Seq(Stubs.newScenario)))

      val target = new ProfileDataServiceImpl(null, null, null, null, null, null, null, null, scenarioRepository, null, Stubs.traceServiceMock, null, null, null,null,null,pedigreeService)

      val result = Await.result(target.deleteProfile(Stubs.sampleCode, DeletedMotive("jerkovicm", "motive"), "pdg"), duration)

      result.isLeft mustBe true
      result mustBe Left("E0118: El perfil AR-B-LAB-1 está participando de un escenario pendiente.")
    }

    "delete a profile and trace action" in {
      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.getTotalProfilesOccurenceInCase(any[SampleCode])).thenReturn(Future.successful(0))
      when(pedigreeService.countActivePedigreesByProfile(any[String])).thenReturn(Future.successful(0))
      when(pedigreeService.getTotalProfilesPedigreeMatches(any[SampleCode])).thenReturn(Future.successful(0))
      when(pedigreeService.getTotalProfileNumberOfMatches(any[SampleCode])).thenReturn(Future.successful(0))

      val traceService = mock[TraceService]
      when(traceService.add(any[Trace])).thenReturn(Future.successful(Right(1l)))

      when(profileRepo.delete(any[SampleCode])).thenReturn(Future.successful(Right(Stubs.sampleCode)))

      when(matchingServiceMock.findMatchingResults(Stubs.sampleCode)).thenReturn(Future.successful(None))

      val profileDataRepository = mock[ProfileDataRepository]
      when(profileDataRepository.delete(any[SampleCode], any[DeletedMotive])).thenReturn(Future.successful(1))

      val scenarioRepository = mock[ScenarioRepository]
      when(scenarioRepository.getByProfile(any[SampleCode])).thenReturn(Future.successful(Seq()))

      val target = new ProfileDataServiceImpl(null, profileDataRepository, null, null, null, null, null, matchingServiceMock, scenarioRepository, profileRepo, traceService, null, null, null,null,null,pedigreeService)

      Await.result(target.deleteProfile(Stubs.sampleCode, DeletedMotive("jerkovicm", "motive"), "pdg",false), duration)

      verify(traceService).add(any[Trace])
    }
    
    
  }

}