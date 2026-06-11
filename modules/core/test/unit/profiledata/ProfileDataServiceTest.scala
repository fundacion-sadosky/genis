package unit.profiledata

import configdata.{BioMaterialTypeService, CategoryService, CrimeTypeService}
import connections.InterconnectionService
import jakarta.inject.Provider
import security.ConnectionRepository
import fixtures.{ProfileDataFixtures, StubCacheService}
import inbox.NotificationService
import matching.{MatchingResults, MatchingService}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, verify, when}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import pedigree.PedigreeService
import play.api.i18n.MessagesApi
import profile.{ProfileRepository, ProfileService}
import profiledata.{ProfileData, ProfileDataAttempt, ProfileDataRepository, ProfileDataServiceImpl}
import scenarios.ScenarioRepository
import services.{LaboratoryService, TemporaryAssetKey, UserService}
import trace.{Trace, TraceService}
import types.SampleCode

import scala.concurrent.ExecutionContext.global as ec
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class ProfileDataServiceTest extends AnyWordSpec with Matchers with MockitoSugar:

  import ProfileDataFixtures.*

  private val timeout = 5.seconds

  private def makeService(
    cache:                CacheService         = new StubCacheService,
    repo:                 ProfileDataRepository = mock[ProfileDataRepository],
    categoryService:      CategoryService       = mock[CategoryService],
    connectionRepo:       ConnectionRepository  = mock[ConnectionRepository],
    notificationService:  NotificationService   = mock[NotificationService],
    bioMatService:        BioMaterialTypeService = mock[BioMaterialTypeService],
    crimeTypeService:     CrimeTypeService      = mock[CrimeTypeService],
    labService:           LaboratoryService     = mock[LaboratoryService],
    matchingService:      MatchingService       = mock[MatchingService],
    scenarioRepo:         ScenarioRepository    = mock[ScenarioRepository],
    profileRepo:          ProfileRepository     = mock[ProfileRepository],
    traceService:         TraceService          = mock[TraceService],
    interconnectionSvc:   InterconnectionService = mock[InterconnectionService],
    profileService:       ProfileService        = mock[ProfileService],
    pedigreeService:      PedigreeService       = mock[PedigreeService],
    userService:          UserService           = mock[UserService],
    messagesApi:          MessagesApi           = mock[MessagesApi]
  ): ProfileDataServiceImpl =
    new ProfileDataServiceImpl(
      cache, repo, categoryService, connectionRepo, notificationService,
      bioMatService, crimeTypeService, labService, () => matchingService, scenarioRepo,
      profileRepo, traceService, "SHDG", "AR", "C",
      interconnectionSvc, () => profileService, () => pedigreeService, userService, messagesApi
    )(using ec)

  "ProfileDataService.findByCode" must {

    "resolve bioMaterialType, crimeType and laboratory display names" in {
      val repo = mock[ProfileDataRepository]
      when(repo.findByCode(sampleCode)).thenReturn(Future.successful(Some(profileData)))

      val bioMatSvc = mock[BioMaterialTypeService]
      when(bioMatSvc.list()).thenReturn(Future.successful(bioMaterialTypes))

      val crimeTypeSvc = mock[CrimeTypeService]
      when(crimeTypeSvc.list()).thenReturn(Future.successful(crimeTypes))

      val labSvc = mock[LaboratoryService]
      when(labSvc.list()).thenReturn(Future.successful(Seq(laboratory)))

      val service = makeService(repo = repo, bioMatService = bioMatSvc, crimeTypeService = crimeTypeSvc, labService = labSvc)
      val result = Await.result(service.findByCode(sampleCode), timeout).get

      result.bioMaterialType mustBe Some("sangre")
      result.crimeType mustBe Some("contra las personas")
      result.laboratory mustBe "Servicio de Huellas Digitales Genéticos"
    }

    "return None when profile does not exist" in {
      val repo = mock[ProfileDataRepository]
      when(repo.findByCode(sampleCode)).thenReturn(Future.successful(None))

      val service = makeService(repo = repo)
      val result = Await.result(service.findByCode(sampleCode), timeout)

      result mustBe None
    }
  }

  "ProfileDataService.findByCodes" must {

    "resolve display names for multiple profiles" in {
      val repo = mock[ProfileDataRepository]
      when(repo.findByCodes(List(sampleCode, sampleCode2)))
        .thenReturn(Future.successful(Seq(profileData, profileData2)))

      val bioMatSvc = mock[BioMaterialTypeService]
      when(bioMatSvc.list()).thenReturn(Future.successful(bioMaterialTypes))

      val crimeTypeSvc = mock[CrimeTypeService]
      when(crimeTypeSvc.list()).thenReturn(Future.successful(crimeTypes))

      val labSvc = mock[LaboratoryService]
      when(labSvc.list()).thenReturn(Future.successful(Seq(laboratory)))

      val service = makeService(repo = repo, bioMatService = bioMatSvc, crimeTypeService = crimeTypeSvc, labService = labSvc)
      val result = Await.result(service.findByCodes(List(sampleCode, sampleCode2)), timeout)

      result must have size 2
      result.head.bioMaterialType mustBe Some("sangre")
      result.head.crimeType mustBe Some("contra las personas")
    }
  }

  "ProfileDataService.deleteProfile" must {

    "return Right(globalCode) when all guards pass and deletion succeeds" in {
      val repo = mock[ProfileDataRepository]
      when(repo.delete(any[SampleCode], any())).thenReturn(Future.successful(1))
      when(repo.getProfileUploadStatusByGlobalCode(any())).thenReturn(Future.successful(None))
      when(repo.getProfileReceivedStatusByGlobalCode(any())).thenReturn(Future.successful(None))

      val profileRepo = mock[ProfileRepository]
      when(profileRepo.delete(any())).thenReturn(Future.successful(Right(sampleCode)))

      val traceService = mock[TraceService]
      when(traceService.add(any[Trace])).thenReturn(Future.successful(()))

      val scenarioRepo = mock[ScenarioRepository]
      when(scenarioRepo.getByProfile(any())).thenReturn(Future.successful(Seq.empty))

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.getTotalProfilesOccurenceInCase(any())).thenReturn(Future.successful(0))
      when(pedigreeService.countActivePedigreesByProfile(any())).thenReturn(Future.successful(0))
      when(pedigreeService.getTotalProfilesPedigreeMatches(any())).thenReturn(Future.successful(0))
      when(pedigreeService.getTotalProfileNumberOfMatches(any())).thenReturn(Future.successful(0))

      val service = makeService(
        repo = repo, profileRepo = profileRepo, traceService = traceService,
        scenarioRepo = scenarioRepo, pedigreeService = pedigreeService
      )
      val result = Await.result(service.deleteProfile(sampleCode, deletedMotive, "analyst1", validateMPI = false), timeout)

      result.isRight mustBe true
      result mustBe Right(sampleCode)
    }

    "call traceService.add after successful deletion" in {
      val repo = mock[ProfileDataRepository]
      when(repo.delete(any[SampleCode], any())).thenReturn(Future.successful(1))
      when(repo.getProfileUploadStatusByGlobalCode(any())).thenReturn(Future.successful(None))
      when(repo.getProfileReceivedStatusByGlobalCode(any())).thenReturn(Future.successful(None))

      val profileRepo = mock[ProfileRepository]
      when(profileRepo.delete(any())).thenReturn(Future.successful(Right(sampleCode)))

      val traceService = mock[TraceService]
      when(traceService.add(any[Trace])).thenReturn(Future.successful(()))

      val scenarioRepo = mock[ScenarioRepository]
      when(scenarioRepo.getByProfile(any())).thenReturn(Future.successful(Seq.empty))

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.getTotalProfilesOccurenceInCase(any())).thenReturn(Future.successful(0))
      when(pedigreeService.countActivePedigreesByProfile(any())).thenReturn(Future.successful(0))
      when(pedigreeService.getTotalProfilesPedigreeMatches(any())).thenReturn(Future.successful(0))
      when(pedigreeService.getTotalProfileNumberOfMatches(any())).thenReturn(Future.successful(0))

      val service = makeService(
        repo = repo, profileRepo = profileRepo, traceService = traceService,
        scenarioRepo = scenarioRepo, pedigreeService = pedigreeService
      )
      Await.result(service.deleteProfile(sampleCode, deletedMotive, "analyst1", validateMPI = false), timeout)

      verify(traceService).add(any[Trace])
    }

    "return Left when profile has a pending scenario (E0118)" in {
      val scenarioRepo = mock[ScenarioRepository]
      when(scenarioRepo.getByProfile(any())).thenReturn(Future.successful(Seq(testScenario)))

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.getTotalProfilesOccurenceInCase(any())).thenReturn(Future.successful(0))
      when(pedigreeService.countActivePedigreesByProfile(any())).thenReturn(Future.successful(0))
      when(pedigreeService.getTotalProfilesPedigreeMatches(any())).thenReturn(Future.successful(0))
      when(pedigreeService.getTotalProfileNumberOfMatches(any())).thenReturn(Future.successful(0))

      val service = makeService(scenarioRepo = scenarioRepo, pedigreeService = pedigreeService)
      val result = Await.result(service.deleteProfile(sampleCode, deletedMotive, "analyst1"), timeout)

      result.isLeft mustBe true
    }

    "return Left when profile is associated to an active pedigree (E0126)" in {
      val scenarioRepo = mock[ScenarioRepository]
      when(scenarioRepo.getByProfile(any())).thenReturn(Future.successful(Seq.empty))

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.getTotalProfilesOccurenceInCase(any())).thenReturn(Future.successful(0))
      when(pedigreeService.countActivePedigreesByProfile(any())).thenReturn(Future.successful(1))
      when(pedigreeService.getTotalProfilesPedigreeMatches(any())).thenReturn(Future.successful(0))
      when(pedigreeService.getTotalProfileNumberOfMatches(any())).thenReturn(Future.successful(0))

      val service = makeService(scenarioRepo = scenarioRepo, pedigreeService = pedigreeService)
      val result = Await.result(service.deleteProfile(sampleCode, deletedMotive, "analyst1"), timeout)

      result.isLeft mustBe true
    }
  }

  "ProfileDataService.create" must {

    "return Left(error) without calling repo when cache misses on image UUID" in {
      val cache = new StubCacheService  // empty — returns None for all keys
      val repo = mock[ProfileDataRepository]

      val service = makeService(cache = cache, repo = repo)
      val result = Await.result(service.create(profileDataAttempt), timeout)

      result.isLeft mustBe true
    }

    "return Right(sampleCode) when cache is bypassed (no dataFiliation) and repo succeeds" in {
      val repo = mock[ProfileDataRepository]
      when(repo.add(any[ProfileData], any[String], any(), any(), any())).thenReturn(Future.successful(sampleCode))

      val traceService = mock[TraceService]
      when(traceService.add(any[Trace])).thenReturn(Future.successful(()))

      val categoryService = mock[CategoryService]
      when(categoryService.listCategories).thenReturn(Future.successful(Map.empty))

      val service = makeService(repo = repo, traceService = traceService, categoryService = categoryService)
      val pdNoFiliation = profileDataAttempt.copy(dataFiliation = None)
      val result = Await.result(service.create(pdNoFiliation), timeout)

      result mustBe Right(sampleCode)
    }

    "pop cache keys after successful insert" in {
      val cache = new StubCacheService
      cache.set(TemporaryAssetKey("uuid-inprint-001"), List.empty[java.io.File])
      cache.set(TemporaryAssetKey("uuid-picture-001"), List.empty[java.io.File])
      cache.set(TemporaryAssetKey("uuid-signature-001"), List.empty[java.io.File])

      val repo = mock[ProfileDataRepository]
      when(repo.add(any[ProfileData], any[String], any(), any(), any())).thenReturn(Future.successful(sampleCode))

      val traceService = mock[TraceService]
      when(traceService.add(any[Trace])).thenReturn(Future.successful(()))

      val categoryService = mock[CategoryService]
      when(categoryService.listCategories).thenReturn(Future.successful(Map.empty))

      val service = makeService(cache = cache, repo = repo, traceService = traceService, categoryService = categoryService)
      Await.result(service.create(profileDataAttempt), timeout)

      cache.get[List[java.io.File]](TemporaryAssetKey("uuid-inprint-001")) mustBe None
      cache.get[List[java.io.File]](TemporaryAssetKey("uuid-picture-001")) mustBe None
      cache.get[List[java.io.File]](TemporaryAssetKey("uuid-signature-001")) mustBe None
    }
  }

  "ProfileDataService.updateProfileData" must {

    "return false without calling repo when profile is not editable" in {
      val repo = mock[ProfileDataRepository]

      val matchingService = mock[MatchingService]
      when(matchingService.findMatchingResults(sampleCode)).thenReturn(Future.successful(Some(matchingResults)))

      val profileService = mock[ProfileService]
      when(profileService.isReadOnlySampleCode(any(), any(), any())).thenReturn(Future.successful((false, "")))

      val service = makeService(repo = repo, matchingService = matchingService, profileService = profileService)
      val result = Await.result(service.updateProfileData(sampleCode, profileDataAttempt), timeout)

      result mustBe false
    }
  }

  "ProfileDataService.isEditable" must {

    "return Some(false) when profile has matching results" in {
      val matchingService = mock[MatchingService]
      when(matchingService.findMatchingResults(sampleCode)).thenReturn(Future.successful(Some(matchingResults)))

      val profileService = mock[ProfileService]
      when(profileService.isReadOnlySampleCode(any(), any(), any())).thenReturn(Future.successful((false, "")))

      val service = makeService(matchingService = matchingService, profileService = profileService)
      val result = Await.result(service.isEditable(sampleCode), timeout)

      result mustBe Some(false)
    }

    "return Some(true) when profile has no matches and is not read-only" in {
      val matchingService = mock[MatchingService]
      when(matchingService.findMatchingResults(sampleCode)).thenReturn(Future.successful(None))

      val profileService = mock[ProfileService]
      when(profileService.isReadOnlySampleCode(any(), any(), any())).thenReturn(Future.successful((false, "")))

      val service = makeService(matchingService = matchingService, profileService = profileService)
      val result = Await.result(service.isEditable(sampleCode), timeout)

      result mustBe Some(true)
    }
  }

  // needed for makeService parameter type
  private type CacheService = services.CacheService
