package profile

import java.io.FileOutputStream

import configdata.{CategoryRepository, _}
import connections.InterconnectionService
import kits.{AnalysisType, AnalysisTypeService, LocusService, StrKit, StrKitService}
import matching._
import org.mockito.Matchers.any
import org.mockito.Mockito.{when, _}
import org.scalatest.mock.MockitoSugar
import pedigree.{PedigreeSearch, PedigreeService}
import play.api.libs.Files.TemporaryFile
import probability.ProbabilityModel.ProbabilityModel
import probability.{NoFrequencyException, ProbabilityService}
import profile.GenotypificationByType.GenotypificationByType
import profile.Profile.LabeledGenotypification
import profiledata._
import services._
import specs.PdgSpec
import stubs.Stubs
import trace.{Trace, TraceService}
import types.{AlphanumericId, SampleCode, StatOption}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, SECONDS}
import scala.math.BigDecimal.{double2bigDecimal, int2bigDecimal}


class ProfileServiceTest extends PdgSpec with MockitoSugar {

  val newAnalysis = Stubs.newAnalysis
  implicit val sessionExpirationTime = 10;
  val duration = Duration(10, SECONDS)

  val subcatRelRespository = mock[CategoryRepository]

  val globalCode = SampleCode("AR-C-HIBA-500")

  val fullCat = FullCategory(AlphanumericId("CATTT"),
    "", None, AlphanumericId("GROUP"),
    false, false, true, false,manualLoading=true,
    Map.empty, Seq.empty, Seq.empty, Seq.empty,Some(1))

  val qualityParamsMockProvider = mock[QualityParamsProvider]
  when(qualityParamsMockProvider.minLocusQuantityAllowedPerProfile(any[FullCategory], any[StrKit])).thenReturn(5)
  when(qualityParamsMockProvider.maxOverageDeviatedLociPerProfile(any[FullCategory], any[StrKit])).thenReturn(5)
  when(qualityParamsMockProvider.maxAllelesPerLocus(any[FullCategory], any[StrKit])).thenReturn(6)

  val categoryServiceMock = mock[CategoryService]
  when(categoryServiceMock.listCategories).thenReturn(Stubs.categoryMap)
  when(categoryServiceMock.getCategory(any[AlphanumericId])).thenReturn(Option(fullCat))
  when(categoryServiceMock.getCategoryTypeFromFullCategory(any[FullCategory])).thenReturn(None)

  val kitServiceMock = mock[StrKitService]
  when(kitServiceMock.list()).thenReturn(Future.successful(Stubs.strKits))
  when(kitServiceMock.findLociByKit(any[String])).thenReturn(Future.successful(Stubs.loci))
  when(kitServiceMock.get(any[String])).thenReturn(Future.successful(Stubs.strKits.headOption))

  val kitServiceMockY = mock[StrKitService]
  when(kitServiceMockY.list()).thenReturn(Future.successful(Stubs.strKits))
  when(kitServiceMockY.findLociByKit(any[String])).thenReturn(Future.successful(Stubs.loci))
  when(kitServiceMockY.get(any[String])).thenReturn(Future.successful(Some(StrKit("Identifiler", "Identifiler", 3, 16, 14))))

  val locusServiceMock = mock[LocusService]
  when(locusServiceMock.list()).thenReturn(Future.successful(Stubs.locus))
  when(locusServiceMock.getLocusByAnalysisType(any[Int])).thenReturn(Future.successful(Stubs.locus.map(_.id)))
  when(locusServiceMock.getLocusByAnalysisTypeName(any[String])).thenReturn(Future.successful(Stubs.locus.map(_.id)))

  val analysisTypeServiceMock = mock[AnalysisTypeService]
  when(analysisTypeServiceMock.getById(any[Int])).thenReturn(Future.successful(Stubs.analysisTypes.headOption))

  val analysisTypeServiceMockY = mock[AnalysisTypeService]
  when(analysisTypeServiceMockY.getById(any[Int])).thenReturn(Future.successful(Some(AnalysisType(3, "Cy"))))

  "ProfileService" should {
    "add a new analysis to a non existing profile without electropherograms" in {
      val cacheService = mock[CacheService]
      when(cacheService.pop(TemporaryAssetKey(newAnalysis.token))).thenReturn(Option(List[TemporaryFile]())) // no images
      //when(cacheService.get[List[String]](anyString())).thenReturn(Option(List[String]()))

      val profileRespository = mock[MongoProfileRepository]
      when(profileRespository.findByCode(newAnalysis.globalCode)).thenReturn(Future.successful(None)) // unexisting profile
      when(profileRespository.add(any[Profile])).thenReturn(Future.successful(Stubs.sampleCode))

      val mService = mock[MatchingService]

      val profileDataRepository = mock[ProfileDataRepository]
      when(profileDataRepository.findByCode(newAnalysis.globalCode)).thenReturn(Future.successful(Some(Stubs.profileData)))

      val probabilityService = mock[ProbabilityService]
      when(probabilityService.getStats(any[String])).thenReturn(Future.successful(Some(Stubs.statOption)))

      val target = new ProfileServiceImpl(cacheService, profileRespository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryServiceMock, Stubs.notificationServiceMock, probabilityService, locusServiceMock, Stubs.traceServiceMock, null, analysisTypeServiceMock, Stubs.labelsSets)

      val future = target.create(newAnalysis)
      val profileId = Await.result(future, duration)

      profileId must not be null
      assert(profileId.isRight, profileId.left)
      profileId.right.get.globalCode mustBe Stubs.sampleCode
    }

    "add a new analysis to a non existing profile with an electropherograms" in {

      val stream = new FileOutputStream("ImagePath")
      try {
        stream.write(Array[Byte](0))
      } finally {
        stream.close
      }
      val cacheService = mock[CacheService]
      when(cacheService.pop(TemporaryAssetKey(newAnalysis.token))).thenReturn(Option(List[TemporaryFile](TemporaryFile(new java.io.File("ImagePath")))))

      val profileRespository = mock[MongoProfileRepository]
      when(profileRespository.findByCode(newAnalysis.globalCode)).thenReturn(Future.successful(None)) // unexisting profile
      when(profileRespository.add(any[Profile])).thenReturn(Future.successful(Stubs.sampleCode))
      when(profileRespository.addElectropherogram(any[SampleCode], any[String], any[Array[Byte]],any[String])).thenReturn(Future(Right(any[SampleCode])))

      val profileDataRepository = mock[ProfileDataRepository]
      when(profileDataRepository.findByCode(newAnalysis.globalCode)).thenReturn(Future.successful(Some(Stubs.profileData)))

      val mService = mock[MatchingService]

      val probabilityService = mock[ProbabilityService]
      when(probabilityService.getStats(any[String])).thenReturn(Future.successful(Some(Stubs.statOption)))

      val target = new ProfileServiceImpl(cacheService, profileRespository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryServiceMock, Stubs.notificationServiceMock, probabilityService, locusServiceMock, Stubs.traceServiceMock, null, analysisTypeServiceMock, Stubs.labelsSets)

      val future = target.create(newAnalysis)
      val profileId = Await.result(future, duration)

      profileId must not be null
      assert(profileId.isRight, profileId.left)
      profileId.right.get.globalCode mustBe Stubs.sampleCode
    }

    "fail when analysis doesn't have a profile data asociated E0113" in {
      val cacheService = mock[CacheService]
      val profileRespository = mock[MongoProfileRepository]
      when(profileRespository.findByCode(newAnalysis.globalCode)).thenReturn(Future.successful(None)) // unexisting profile

      val profileDataRepository = mock[ProfileDataRepository]
      when(profileDataRepository.findByCode(newAnalysis.globalCode)).thenReturn(Future.successful(None))

      val mService = mock[MatchingService]

      val target = new ProfileServiceImpl(cacheService, profileRespository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryServiceMock, Stubs.notificationServiceMock, null, locusServiceMock, Stubs.traceServiceMock, null, analysisTypeServiceMock, Stubs.labelsSets)

      val future = target.create(newAnalysis)
      val profileId = Await.result(future, duration)

      profileId must not be null
      profileId.isLeft mustBe true
      profileId.left.get(0) mustBe "E0113: No existe un perfil con el código: " + newAnalysis.globalCode+"."
    }

    //    "add a new analysis to an existing profile without electropherograms" in {
    //      val cacheService = mock[CacheService]
    //      when(cacheService.pop(TemporaryAssetKey(newAnalysis.token))).thenReturn(Option(List[TemporaryFile]())) // no images
    //
    //      val profileRespository = mock[MongoProfileRepository]
    //      when(profileRespository.findByCode(newAnalysis.globalCode)).thenReturn(Future.successful(Option(Stubs.newProfile))) // existing profile
    //      when(profileRespository.addAnalysis(any[SampleCode], any[Analysis], any[Profile.Genotypification], any[Option[Profile.LabeledGenotypification]], any[Option[Profile.MatchingRules]], any[Option[Profile.Mismatch]], any[Boolean])).thenReturn(Future.successful(Stubs.sampleCode))
    //
    //      val profileDataRepository = mock[ProfileDataRepository]
    //      when(profileDataRepository.findByCode(newAnalysis.globalCode)).thenReturn(Future.successful(Option(Stubs.profileData))) // existing profile
    //
    //      val mService = mock[MatchingService]
    //
    //      val target = new ProfileServiceImpl(cacheService, profileRespository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryServiceMock, null)
    //
    //      val g = newAnalysis.genotypification + ("LOCUS 7" -> List()) + ("LOCUS 6" -> List(Allele(1)))
    //      val na: NewAnalysis = new NewAnalysis(Stubs.sampleCode, "token", "Identifiler", g, None, Some(1), None)
    //
    //      val future = target.create(na)
    //      val profileId = Await.result(future, duration)
    //
    //      profileId must not be null
    //      assert(profileId.isRight, profileId.left)
    //      profileId.right.get mustBe Stubs.sampleCode
    //    }

    "throw an error msg list when attempting to add a new analysis to an existing profile with different genotypification" in {
      val cacheService = mock[CacheService]
      when(cacheService.pop(TemporaryAssetKey(newAnalysis.token))).thenReturn(Option(List[TemporaryFile]())) // no images

      val profileRespository = mock[MongoProfileRepository]
      when(profileRespository.findByCode(newAnalysis.globalCode)).thenReturn(Future.successful(Option(Stubs.newProfile))) // existing profile

      val profileDataRepository = mock[ProfileDataRepository]
      when(profileDataRepository.findByCode(newAnalysis.globalCode)).thenReturn(Future.successful(Option(Stubs.profileData))) // existing profile

      val probabilityService = mock[ProbabilityService]
      when(probabilityService.getStats(any[String])).thenReturn(Future.successful(Some(Stubs.statOption)))

      val mService = mock[MatchingService]
      when(mService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Seq())
      when(mService.matchesWithPartialHit(any[SampleCode])).thenReturn(Future.successful(Seq()))
      val interconnectionService = mock[InterconnectionService]
      when(interconnectionService.isFromCurrentInstance(any[SampleCode])).thenReturn(true)
      when(profileDataRepository.isDeleted(any[SampleCode])).thenReturn(Future.successful(Some(false)))
      when(profileDataRepository.getProfileUploadStatusByGlobalCode(any[SampleCode])).thenReturn(Future.successful(None))

      val target = new ProfileServiceImpl(cacheService, profileRespository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryServiceMock, Stubs.notificationServiceMock, probabilityService, locusServiceMock, Stubs.traceServiceMock, null, analysisTypeServiceMock, Stubs.labelsSets,interconnectionService)

      val wrongGenotypification: Map[String, List[AlleleValue]] = Map(
        "LOCUS 1" -> List(Allele(2.2)),
        "LOCUS 3" -> List(Allele(1), XY('Y')),
        "LOCUS 4" -> List(Mitocondrial('C', 1), Mitocondrial('A', 2.1), Mitocondrial('-', 3)),
        "LOCUS 5" -> List(Allele(1), Allele(4)))

      val wrongAnalysis: NewAnalysis = new NewAnalysis(
        newAnalysis.globalCode, "pdg", "token", Some("Identifiler"), None, wrongGenotypification, None, Some(1), None)

      val future = target.create(wrongAnalysis)
      val errorList = Await.result(future, duration)

      errorList must not be null
      errorList.isLeft mustBe true

      //      val expectedErrorList: List[String] =
      //        List[String]("LOCUS 1: Error, no coincide con los alelos almacenados",
      //          "LOCUS 3: Error en cantidad de alelos",
      //          "LOCUS 4: Error, no coincide con los alelos almacenados",
      //          "LOCUS 5: Error, no coincide con los alelos almacenados")
      //      errorList.left.get mustBe expectedErrorList
    }

    "merge two genotyfications" in {

      val existingGenotyfication = newAnalysis.genotypification
      val newLocusGenotyfication: Map[String, List[AlleleValue]] =
        Map("LOCUS 7" -> List(), "LOCUS 6" -> List(Allele(1)))

      val cacheService = mock[CacheService]
      val profileRespository = mock[MongoProfileRepository]
      val profileDataRepository = mock[ProfileDataRepository]

      val mService = mock[MatchingService]

      val target = new ProfileServiceImpl(cacheService, profileRespository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryServiceMock, Stubs.notificationServiceMock, null, locusServiceMock, Stubs.traceServiceMock, null, analysisTypeServiceMock, Stubs.labelsSets)

      //      val mergedGenotypification = target.mergeGenotypification(existingGenotyfication, newLocusGenotyfication)
      //
      //      mergedGenotypification must not be null
      //      mergedGenotypification.isDefinedAt("LOCUS 1") mustBe true
      //      mergedGenotypification.isDefinedAt("LOCUS 2") mustBe true
      //      mergedGenotypification.isDefinedAt("LOCUS 3") mustBe true
      //      mergedGenotypification.isDefinedAt("LOCUS 4") mustBe true
      //      mergedGenotypification.isDefinedAt("LOCUS 5") mustBe true
      //      mergedGenotypification.isDefinedAt("LOCUS 6") mustBe true
      //      mergedGenotypification.isDefinedAt("LOCUS 7") mustBe false
      //      mergedGenotypification.get("LOCUS 1").get.length mustBe 3
      //      mergedGenotypification.get("LOCUS 2").get.length mustBe 3
      //      mergedGenotypification.get("LOCUS 3").get.length mustBe 3
      //      mergedGenotypification.get("LOCUS 4").get.length mustBe 3
      //      mergedGenotypification.get("LOCUS 5").get.length mustBe 3
      //      mergedGenotypification.get("LOCUS 6").get.length mustBe 1
      //      mergedGenotypification.get("LOCUS 1").get.contains(Allele(2.1)) mustBe true
      //      mergedGenotypification.get("LOCUS 1").get.contains(Dropout) mustBe true
      //      mergedGenotypification.get("LOCUS 2").get.contains(OutOfLadder(2)) mustBe true
      //      mergedGenotypification.get("LOCUS 2").get.contains(SequencedAllele(5.1, 3)) mustBe true
      //      mergedGenotypification.get("LOCUS 3").get.contains(XY('X')) mustBe true
      //      mergedGenotypification.get("LOCUS 3").get.contains(XY('Y')) mustBe true
      //      mergedGenotypification.get("LOCUS 4").get.contains(Mitocondrial('A', 1)) mustBe true
      //      mergedGenotypification.get("LOCUS 4").get.contains(Mitocondrial('A', 2.1)) mustBe true
      //      mergedGenotypification.get("LOCUS 4").get.contains(Mitocondrial('-', 3)) mustBe true
      //      mergedGenotypification.get("LOCUS 5").get.contains(Allele(1)) mustBe true
      //      mergedGenotypification.get("LOCUS 5").get.contains(Allele(2)) mustBe true
      //      mergedGenotypification.get("LOCUS 5").get.contains(Allele(3)) mustBe true
      //      mergedGenotypification.get("LOCUS 6").get.contains(Allele(1)) mustBe true
    }

    "return the electropherograms ids searched by profileId" in {
      val cacheService = mock[CacheService]

      val ret: List[(String, String, String)] = List(("efgId1", "analysisId1","name"), ("efgId2", "analysisId2","name2"), ("efgId3", "analysisId3","name3"))
      val profileRespository = mock[MongoProfileRepository]
      when(profileRespository.getElectropherogramsByCode(newAnalysis.globalCode)).thenReturn(Future.successful(ret))
      when(profileRespository.getFileByCode(any[SampleCode])).thenReturn(Future.successful(Nil))

      val profileDataRepository = mock[ProfileDataRepository]

      val mService = mock[MatchingService]

      val target = new ProfileServiceImpl(cacheService, profileRespository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryServiceMock, Stubs.notificationServiceMock, null, locusServiceMock, Stubs.traceServiceMock, null, analysisTypeServiceMock, Stubs.labelsSets)

      val future = target.getElectropherogramsByCode(newAnalysis.globalCode)
      val efgIds = Await.result(future, duration)

      val expected: List[FileUploadedType] = List[FileUploadedType](FileUploadedType("efgId1","name"),
        FileUploadedType("efgId2","name2"), FileUploadedType("efgId3","name3"))
      efgIds must not be null
      efgIds.toList mustBe expected
    }

    "return an Array[Bytes] when searched by profileId and electropherogramId" in {
      val cacheService = mock[CacheService]

      val ret: Array[Byte] = Array[Byte](0)
      val profileRespository = mock[MongoProfileRepository]
      when(profileRespository.getElectropherogramImage(newAnalysis.globalCode, "electropherogramId")).thenReturn(Future.successful(Option(ret)))

      val profileDataRepository = mock[ProfileDataRepository]

      val mService = mock[MatchingService]

      val target = new ProfileServiceImpl(cacheService, profileRespository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryServiceMock, Stubs.notificationServiceMock, null, locusServiceMock, Stubs.traceServiceMock, null, analysisTypeServiceMock, Stubs.labelsSets)

      val future = target.getElectropherogramImage(newAnalysis.globalCode, "electropherogramId")
      val imageBytes = Await.result(future, duration)

      imageBytes must not be null
      imageBytes.get mustBe ret
    }

    "return the electropherograms ids searched by analysisId" in {
      val cacheService = mock[CacheService]

      val ret: List[FileUploadedType] = List[FileUploadedType](FileUploadedType("efgId1","name1"), FileUploadedType("efgId2","name1"), FileUploadedType("efgId3","name1"))
      val profileRespository = mock[MongoProfileRepository]
      when(profileRespository.getElectropherogramsByAnalysisId(newAnalysis.globalCode, "analysisId")).thenReturn(Future.successful(ret))

      val profileDataRepository = mock[ProfileDataRepository]

      val mService = mock[MatchingService]

      val target = new ProfileServiceImpl(cacheService, profileRespository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryServiceMock, Stubs.notificationServiceMock, null, locusServiceMock, Stubs.traceServiceMock, null, analysisTypeServiceMock, Stubs.labelsSets)

      val future = target.getElectropherogramsByAnalysisId(newAnalysis.globalCode, "analysisId")
      val efgIds = Await.result(future, duration)

      efgIds must not be null
      efgIds mustBe ret
    }

    //    "parse a xml with no kit specified in XML" in {
    //      val src = new FileReader("test/resources/profile_bad_formed.xml")
    //
    //      val profileService = new ProfileServiceImpl(null, null, null, null, null, null, null)
    //
    //      val future = profileService.parseCodisXMlProfile(SampleCode("AA-A-AAA-5"), src)
    //      val result = Await.result(future, duration)
    //
    //      result.isLeft mustBe true
    //      val errors = result.left.get
    //      errors.size mustBe 1
    //      errors(0) mustBe "El archivo no define un STR Kit"
    //
    //    }
    //
    //    "parse a xml with invalid kit defined in XML" in {
    //      val src = new FileReader("test/resources/profile.xml")
    //
    //      val kitService = mock[StrKitService]
    //      when(kitService.list()).thenReturn(Future.successful(Nil))
    //
    //      val profileService = new ProfileServiceImpl(null, null, null, kitService, null, null, subcatRelRespository)
    //
    //      val future = profileService.parseCodisXMlProfile(SampleCode("AA-A-AAA-5"), src)
    //      val result = Await.result(future, duration)
    //
    //      result.isLeft mustBe true
    //      val errors = result.left.get
    //      errors.size mustBe 1
    //      errors(0) mustBe "El STR kit PowerPlex16 especificado en el archivo no existe en el sistema"
    //    }
    //
    //    "parse a xml with invalid loci defined in XML" in {
    //      val src = new FileReader("test/resources/profile_with_invalid_loci.xml")
    //
    //      val kit = new StrKit("PowerPlex16", "PowerPlex 16", "")
    //      val kitService = mock[StrKitService]
    //      when(kitService.list()).thenReturn(Future.successful(List(kit)))
    //      when(kitService.findLociByKit(kit.id)).thenReturn(Future.successful(Nil))
    //
    //      val profileService = new ProfileServiceImpl(null, null, null, kitService, null, null, subcatRelRespository)
    //
    //      val future = profileService.parseCodisXMlProfile(SampleCode("AA-A-AAA-5"), src)
    //      val result = Await.result(future, duration)
    //
    //      result.isLeft mustBe true
    //      val errors = result.left.get
    //      errors.size mustBe 2
    //      errors(0) mustBe "El marcador BAD1 no se encuentra definido en el kit PowerPlex 16"
    //      errors(1) mustBe "El marcador BAD2 no se encuentra definido en el kit PowerPlex 16"
    //    }
    //
    //    "parse a xml with invalid alelle value defined in XML" in {
    //      val src = new FileReader("test/resources/profile_with_invalid_loci.xml")
    //
    //      val kit = new StrKit("PowerPlex16", "PowerPlex 16", "")
    //      val kitService = mock[StrKitService]
    //      when(kitService.list()).thenReturn(Future.successful(List(kit)))
    //      when(kitService.findLociByKit(kit.id)).thenReturn(Future.successful(Nil))
    //
    //      val profileService = new ProfileServiceImpl(null, null, null, kitService, null, null, subcatRelRespository)
    //
    //      val future = profileService.parseCodisXMlProfile(SampleCode("AA-A-AAA-5"), src)
    //      val result = Await.result(future, duration)
    //
    //      result.isLeft mustBe true
    //      val errors = result.left.get
    //      errors.size mustBe 2
    //      errors(0) mustBe "El marcador BAD1 no se encuentra definido en el kit PowerPlex 16"
    //    }
    //
    //    "parse a xml with invalid allele values defined in XML" in {
    //      val src = new FileReader("test/resources/profile_with_invalid_allele_values.xml")
    //
    //      val kit = new StrKit("PowerPlex16", "PowerPlex 16", "")
    //      val locus = new Locus("TH01", "TH01", None, 2, 2, None)
    //
    //      val kitService = mock[StrKitService]
    //      when(kitService.list()).thenReturn(Future.successful(List(kit)))
    //      when(kitService.findLociByKit(kit.id)).thenReturn(Future.successful(List(locus)))
    //
    //      val profileService = new ProfileServiceImpl(null, null, null, kitService, null, null, subcatRelRespository)
    //
    //      val future = profileService.parseCodisXMlProfile(SampleCode("AA-A-AAA-5"), src)
    //      val result = Await.result(future, duration)
    //
    //      result.isLeft mustBe true
    //      val errors = result.left.get
    //      errors.size mustBe 1
    //      errors(0) mustBe "Allele Value Expected, but found '11.333:5'"
    //    }
    //
    //    "parse a well formed xml profile" in {
    //      val src = new FileReader("test/resources/profile.xml")
    //
    //      val kit = new StrKit("PowerPlex16", "PowerPlex 16", "")
    //      val loci = List(
    //        new Locus("TH01", "TH01", None, 2, 2, None),
    //        new Locus("CSF1PO", "CSF1PO", None, 2, 2, None),
    //        new Locus("D8S1179", "D8S1179", None, 2, 2, None),
    //        new Locus("D7S820", "D7S820", None, 2, 2, None),
    //        new Locus("FGA", "FGA", None, 2, 2, None),
    //        new Locus("D16S539", "D16S539", None, 2, 2, None),
    //        new Locus("D13S317", "D13S317", None, 2, 2, None),
    //        new Locus("vWA", "vWA", None, 2, 2, None),
    //        new Locus("D5S818", "D5S818", None, 2, 2, None),
    //        new Locus("D18S51", "D18S51", None, 2, 2, None),
    //        new Locus("D3S1358", "D3S1358", None, 2, 2, None),
    //        new Locus("TPOX", "TPOX", None, 2, 2, None)
    //      )
    //
    //      val kitService = mock[StrKitService]
    //      when(kitService.list()).thenReturn(Future.successful(List(kit)))
    //      when(kitService.findLociByKit(kit.id)).thenReturn(Future.successful(loci))
    //
    //      val profileService = new ProfileServiceImpl(null, null, null, kitService, null, null, subcatRelRespository)
    //
    //      val future = profileService.parseCodisXMlProfile(SampleCode("AA-A-AAA-5"), src)
    //      val result = Await.result(future, duration)
    //
    //      assert(result.isRight, result.left)
    //      val analysis = result.right.get
    //      analysis.genotypification.size mustBe 2
    //    }

    "add a new analysis to a non existing profile with Some(labeledGenotypification) and Some(contributors)" in {

      val newAn = Stubs.newAnalysisLabeledGenotypification

      val cacheService = mock[CacheService]
      when(cacheService.pop(TemporaryAssetKey(newAn.token))).thenReturn(Option(List[TemporaryFile]())) // no images

      val profileRespository = mock[MongoProfileRepository]
      when(profileRespository.findByCode(newAn.globalCode)).thenReturn(Future.successful(None)) // unexisting profile
      when(profileRespository.add(any[Profile])).thenReturn(Future.successful(Stubs.sampleCode))

      val profileDataRepository = mock[ProfileDataRepository]
      when(profileDataRepository.findByCode(newAn.globalCode)).thenReturn(Future.successful(Some(Stubs.profileData)))

      val mService = mock[MatchingService]

      val probabilityService = mock[ProbabilityService]
      when(probabilityService.getStats(any[String])).thenReturn(Future.successful(Some(Stubs.statOption)))

      val target = new ProfileServiceImpl(cacheService, profileRespository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryServiceMock, Stubs.notificationServiceMock, probabilityService, locusServiceMock, Stubs.traceServiceMock, null, analysisTypeServiceMock, Stubs.labelsSets)

      val future = target.create(newAn)
      val profileId = Await.result(future, duration)

      profileId must not be null
      assert(profileId.isRight, profileId.left)
      profileId.right.get.globalCode mustBe Stubs.sampleCode
    }

    "verify a mixture association (happy path)" in {

      val mixtureGenot = Stubs.mixtureGenotypification

      val pVictima = Stubs.newProfile(AlphanumericId("VICTIMA"), Stubs.mix1Genotypification)
      //val pSospechoso = Stubs.newProfile(AlphanumericId("SOSPECHOSO"), Stubs.mix2Genotypification)

      val profileRespository = mock[MongoProfileRepository]
      when(profileRespository.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(pVictima)))

      val cacheService = mock[CacheService]
      when(cacheService.pop(TemporaryAssetKey("token"))).thenReturn(Option(List[TemporaryFile]())) // no images

      val categoryServiceMock2 = mock[CategoryService]
      when(categoryServiceMock2.listCategories).thenReturn(Map(Stubs.fullCatMixtureVictim.id -> Stubs.fullCatMixtureVictim))

      val target = new ProfileServiceImpl(cacheService, profileRespository, null, null, null, qualityParamsMockProvider, categoryServiceMock2, Stubs.notificationServiceMock, null, locusServiceMock, Stubs.traceServiceMock, null, analysisTypeServiceMock, Stubs.labelsSets)

      val future = target.verifyMixtureAssociation(mixtureGenot, pVictima.globalCode, Stubs.fullCatMixtureVictim.id)
      val res = Await.result(future, duration)

      res must not be null
      assert(res.isRight, res.left)

      val expectedResult = ProfileAsociation(pVictima.globalCode, Stringency.ModerateStringency, pVictima.genotypification(1))
      res.right.get mustBe expectedResult
    }

    //    "fail when verifying a mixture association with not associated subcats" in {
    //
    //      val mixtureGenot = Stubs.mixtureGenotypification
    //
    //      val pVictima = Stubs.newProfile(AlphanumericId("VICTIMA"), Stubs.mix1Genotypification)
    //      val pSospechoso = Stubs.newProfile(AlphanumericId("SOSPECHOSO"), Stubs.mix2Genotypification)
    //
    //      val globalCodesList: List[SampleCode] = List(pVictima.globalCode, pSospechoso.globalCode)
    //
    //      val profileRespository = mock[MongoProfileRepository]
    //      when(profileRespository.findByCodes(any[List[SampleCode]])).thenReturn(Future.successful(List(pVictima, pSospechoso)))
    //
    //      val cacheService = mock[CacheService]
    //      when(cacheService.pop(TemporaryAssetKey("token"))).thenReturn(Option(List[TemporaryFile]())) // no images
    //
    //      val target = new ProfileServiceImpl(cacheService, profileRespository, null, null, null, qualityParamsMockProvider, categoryServiceMock, null)
    //
    //      val future = target.verifyMixtureAssociation(mixtureGenot, globalCodesList, AlphanumericId("MULTIPLE"))
    //      val res = Await.result(future, duration)
    //
    //      res must not be null
    //      assert(res.isLeft, res.right)
    //      res.left.get.contains("no puede asociarse") mustBe true
    //      res.left.get.contains(pSospechoso.globalCode.text) mustBe true
    //    }

  }
  
  "ProfileService " should {
    
    case class Test(profile: Option[Profile], pdCat: Option[AlphanumericId], associable: Boolean, labeleable: Boolean, editable: Boolean)

      val tests = List(Test(Some(Stubs.mixtureVictimProfileNonAssociated), Some(Stubs.mixtureVictimProfileNonAssociated.categoryId), true, false, true),
        Test(Some(Stubs.mixtureVictimLabeled), Some(Stubs.mixtureVictimLabeled.categoryId), true, false, true),
        Test(Some(Stubs.mixtureVictimProfileAssociated), Some(Stubs.mixtureVictimProfileAssociated.categoryId), false, false, false),
        Test(Some(Stubs.mixtureProfile), Some(Stubs.mixtureProfile.categoryId), false, true, true),
        Test(Some(Stubs.mixtureP1), Some(Stubs.mixtureP1.categoryId), false, false, true),
        Test(None, Some(AlphanumericId("MULTIPLE")), false, true, true),
        Test(None, Some(AlphanumericId("MULTIPLE_VICTIMA")), false, false, true),
        Test(None, None, false, false, false))

    val profileRespository = mock[MongoProfileRepository]
      when(profileRespository.getElectropherogramsByCode(any[SampleCode])).thenReturn(Future.successful(List.empty))
      when(profileRespository.getFileByCode(any[SampleCode])).thenReturn(Future.successful(Nil))

      val categoryServiceMockFull = mock[CategoryService]
      when(categoryServiceMockFull.listCategories).thenReturn(Stubs.fullCategoryMap)

      val profileDataRepositoryMock = mock[ProfileDataRepository]
    
      tests.foreach { t =>
        
        "return the correct associable, labeleable and editable for " + 
          t.profile.fold("no")(_.globalCode.text) + " profile with " + t.pdCat.fold("no")(_.text) + " category "  in {
        val pd: Option[ProfileData] = if (t.pdCat.isDefined) Some(ProfileData(t.pdCat.get, SampleCode("XX-X-ANY-1"), None, None, None, None, None, None, "iCode", "asignee", "lab", false, None, None, None, None, None, None,false)) else None
        when(profileRespository.findByCode(any[SampleCode])).thenReturn(Future.successful(t.profile))
        when(profileDataRepositoryMock.findByCode(any[SampleCode])).thenReturn(Future.successful(pd))

          val matchingRepo = mock[MatchingRepository]
          when(matchingRepo.findSuperiorProfile(any[SampleCode])).thenReturn(Future.successful(t.profile))
          when(matchingRepo.findSuperiorProfileData(any[SampleCode])).thenReturn(Future.successful(pd))

          val mService = mock[MatchingService]
        when(mService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(if(t.associable) Seq() else Seq("AR-C-SHDH-1"))
          val interconnectionService = mock[InterconnectionService]
          when(interconnectionService.isFromCurrentInstance(any[SampleCode])).thenReturn(true)
          when(profileDataRepositoryMock.isDeleted(any[SampleCode])).thenReturn(Future.successful(Some(false)))
          when(profileDataRepositoryMock.getProfileUploadStatusByGlobalCode(any[SampleCode])).thenReturn(Future.successful(None))

        val target = new ProfileServiceImpl(null, profileRespository, profileDataRepositoryMock, null, mService, null, categoryServiceMockFull, null, null, locusServiceMock, Stubs.traceServiceMock, null, analysisTypeServiceMock, Stubs.labelsSets,interconnectionService,matchingRepo)

        val future = target.getProfileModelView(SampleCode("XX-X-ANY-1"))
        val res = Await.result(future, duration)

        res.associable mustBe t.associable
        res.labelable mustBe t.labeleable
        res.editable mustBe t.editable
      }
    }}

  "create profile" should {
    "trace action" in {
      val traceService = mock[TraceService]
      when(traceService.add(any[Trace])).thenReturn(Future.successful(Right(1l)))

      val categoryService = mock[CategoryService]
      when(categoryService.listCategories).thenReturn(Stubs.categoryMap)
      when(categoryService.getCategory(any[AlphanumericId])).thenReturn(Option(Stubs.fullCatA1))

      val cacheService = mock[CacheService]
      when(cacheService.pop(TemporaryAssetKey(newAnalysis.token))).thenReturn(Option(List[TemporaryFile]()))

      val profileRepository = mock[MongoProfileRepository]
      when(profileRepository.findByCode(newAnalysis.globalCode)).thenReturn(Future.successful(None))
      when(profileRepository.add(any[Profile])).thenReturn(Future.successful(Stubs.sampleCode))

      val mService = mock[MatchingService]
      when(mService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Seq())

      val profileDataRepository = mock[ProfileDataRepository]
      when(profileDataRepository.findByCode(newAnalysis.globalCode)).thenReturn(Future.successful(Some(Stubs.profileData)))

      val probabilityService = mock[ProbabilityService]
      when(probabilityService.getStats(any[String])).thenReturn(Future.successful(Some(Stubs.statOption)))

      val target = new ProfileServiceImpl(cacheService, profileRepository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryService, Stubs.notificationServiceMock, probabilityService, locusServiceMock, traceService, null, analysisTypeServiceMock, Stubs.labelsSets)

      val future = target.create(newAnalysis)
      Await.result(future, duration)

      verify(traceService).add(any[Trace])
    }

    "set isReference = true" in {
      val referenceCat = FullCategory(AlphanumericId("CATTT"),
        "", None, AlphanumericId("GROUP"),
        true, false, true, false,manualLoading=true,
        Map.empty, Seq.empty, Seq.empty, Seq.empty,Some(1))

      val categoryService = mock[CategoryService]
      when(categoryService.listCategories).thenReturn(Stubs.categoryMap)
      when(categoryService.getCategory(any[AlphanumericId])).thenReturn(Option(referenceCat))

      val cacheService = mock[CacheService]
      when(cacheService.pop(TemporaryAssetKey(newAnalysis.token))).thenReturn(Option(List[TemporaryFile]()))

      val profileRepository = mock[MongoProfileRepository]
      when(profileRepository.findByCode(newAnalysis.globalCode)).thenReturn(Future.successful(None))
      when(profileRepository.add(any[Profile])).thenReturn(Future.successful(Stubs.sampleCode))

      val mService = mock[MatchingService]
      when(mService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Seq())

      val profileDataRepository = mock[ProfileDataRepository]
      when(profileDataRepository.findByCode(newAnalysis.globalCode)).thenReturn(Future.successful(Some(Stubs.profileData)))

      val probabilityService = mock[ProbabilityService]
      when(probabilityService.getStats(any[String])).thenReturn(Future.successful(Some(Stubs.statOption)))

      val target = new ProfileServiceImpl(cacheService, profileRepository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryService, Stubs.notificationServiceMock, probabilityService, locusServiceMock, Stubs.traceServiceMock, null, analysisTypeServiceMock, Stubs.labelsSets)

      val future = target.create(newAnalysis)
      val profile = Await.result(future, duration)

      profile must not be null
      profile.isRight mustBe true
      profile.right.get.globalCode mustBe Stubs.sampleCode
      profile.right.get.isReference mustBe true
    }

    "set isReference = false" in {
      val evidenceCat = FullCategory(AlphanumericId("CATTT"),
        "", None, AlphanumericId("GROUP"),
        false, false, true, false,manualLoading=true,
        Map.empty, Seq.empty, Seq.empty, Seq.empty,Some(1))

      val categoryService = mock[CategoryService]
      when(categoryService.listCategories).thenReturn(Stubs.categoryMap)
      when(categoryService.getCategory(any[AlphanumericId])).thenReturn(Option(evidenceCat))

      val cacheService = mock[CacheService]
      when(cacheService.pop(TemporaryAssetKey(newAnalysis.token))).thenReturn(Option(List[TemporaryFile]()))

      val profileRepository = mock[MongoProfileRepository]
      when(profileRepository.findByCode(newAnalysis.globalCode)).thenReturn(Future.successful(None))
      when(profileRepository.add(any[Profile])).thenReturn(Future.successful(Stubs.sampleCode))

      val mService = mock[MatchingService]
      when(mService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Seq())

      val probabilityService = mock[ProbabilityService]
      when(probabilityService.getStats(any[String])).thenReturn(Future.successful(Some(Stubs.statOption)))

      val profileDataRepository = mock[ProfileDataRepository]
      when(profileDataRepository.findByCode(newAnalysis.globalCode)).thenReturn(Future.successful(Some(Stubs.profileData)))

      val target = new ProfileServiceImpl(cacheService, profileRepository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryService, Stubs.notificationServiceMock, probabilityService, locusServiceMock, Stubs.traceServiceMock, null, analysisTypeServiceMock, Stubs.labelsSets)

      val future = target.create(newAnalysis)
      val profile = Await.result(future, duration)

      profile must not be null
      profile.isRight mustBe true
      profile.right.get.globalCode mustBe Stubs.sampleCode
      profile.right.get.isReference mustBe false
    }

    "calculate contributors if not present in analysis" in {
      val analysis = NewAnalysis(Stubs.sampleCode, "pdg", "token", Some("Identifiler"), None, Stubs.genotypification(1), None, None, None)

      val evidenceCat = FullCategory(AlphanumericId("CATTT"),
        "", None, AlphanumericId("GROUP"),
        false, false, true, false,manualLoading=true,
        Map.empty, Seq.empty, Seq.empty, Seq.empty,Some(1))

      val categoryService = mock[CategoryService]
      when(categoryService.listCategories).thenReturn(Stubs.categoryMap)
      when(categoryService.getCategory(any[AlphanumericId])).thenReturn(Option(evidenceCat))

      val cacheService = mock[CacheService]
      when(cacheService.pop(TemporaryAssetKey(analysis.token))).thenReturn(Option(List[TemporaryFile]()))

      val profileRepository = mock[MongoProfileRepository]
      when(profileRepository.findByCode(analysis.globalCode)).thenReturn(Future.successful(None))
      when(profileRepository.add(any[Profile])).thenReturn(Future.successful(Stubs.sampleCode))

      val mService = mock[MatchingService]
      when(mService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Seq())

      val probabilityService = mock[ProbabilityService]
      when(probabilityService.getStats(any[String])).thenReturn(Future.successful(Some(Stubs.statOption)))
      when(probabilityService.calculateContributors(any[Analysis], any[AlphanumericId], any[StatOption])).thenReturn(Future.successful(3))

      val profileDataRepository = mock[ProfileDataRepository]
      when(profileDataRepository.findByCode(analysis.globalCode)).thenReturn(Future.successful(Some(Stubs.profileData)))

      val target = new ProfileServiceImpl(cacheService, profileRepository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryService, Stubs.notificationServiceMock, probabilityService, locusServiceMock, Stubs.traceServiceMock, null, analysisTypeServiceMock, Stubs.labelsSets)

      val future = target.create(analysis)
      val profile = Await.result(future, duration)

      profile must not be null
      profile.isRight mustBe true
      profile.right.get.contributors mustBe Some(3)
    }

  }

  "update profile" should {
    "not set isReference flag according to category" in {
      val referenceCat = FullCategory(AlphanumericId("CATTT"), "", None, AlphanumericId("GROUP"),
        true, false, true, false,manualLoading=true, Map.empty, Seq.empty, Seq.empty, Seq.empty,Some(1))

      val evidenceProfile = Profile(SampleCode("XX-X-ANY-1"), SampleCode("XX-X-ANY-1"), "", "", AlphanumericId("MULTIPLE_VICTIMA"),
        Map(), None, None, Some(2), None, None, None, false, true, false)

      val categoryService = mock[CategoryService]
      when(categoryService.listCategories).thenReturn(Stubs.categoryMap)
      when(categoryService.getCategory(any[AlphanumericId])).thenReturn(Option(referenceCat))

      val profileRepository = mock[MongoProfileRepository]
      when(profileRepository.findByCode(newAnalysis.globalCode)).thenReturn(Future.successful(Option(evidenceProfile)))
      when(profileRepository.addAnalysis(any[SampleCode], any[Analysis], any[GenotypificationByType], any[Option[Profile.LabeledGenotypification]], any[Option[Seq[MatchingRule]]], any[Option[Profile.Mismatch]])).thenReturn(Future.successful(evidenceProfile.globalCode))

      val mService = mock[MatchingService]
      when(mService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Seq())
      when(mService.matchesWithPartialHit(any[SampleCode])).thenReturn(Future.successful(Seq()))
      when(mService.matchesNotDiscarded(any[SampleCode])).thenReturn(Future.successful(Seq()))

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.getAllCourtCases(any[PedigreeSearch])).thenReturn(Future.successful(Seq()))

      val interconnectionService = mock[InterconnectionService]
      when(interconnectionService.isFromCurrentInstance(any[SampleCode])).thenReturn(true)
      val profileDataRepository = mock[ProfileDataRepository]

      when(profileDataRepository.isDeleted(any[SampleCode])).thenReturn(Future.successful(Some(false)))
      when(profileDataRepository.getProfileUploadStatusByGlobalCode(any[SampleCode])).thenReturn(Future.successful(None))

      val target = new ProfileServiceImpl(null, profileRepository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryService, Stubs.notificationServiceMock, null, locusServiceMock, Stubs.traceServiceMock, pedigreeService, analysisTypeServiceMock, Stubs.labelsSets,interconnectionService)

      val future = target.importProfile(Stubs.profileData, newAnalysis)
      val profile = Await.result(future, duration)

      profile must not be null
      profile.isRight mustBe true
      profile.right.get.globalCode mustBe evidenceProfile.globalCode
      profile.right.get.isReference mustBe false
    }
    "not allow new analysis when there is a hit E0112" in {
      val analysis = NewAnalysis(Stubs.sampleCode, "pdg", "token", Some("Identifiler"), None, Map(), None, None, None)

      val category = FullCategory(AlphanumericId("CATTT"),
        "", None, AlphanumericId("GROUP"),
        false, false, true, false,manualLoading=true,
        Map.empty, Seq.empty, Seq.empty, Seq.empty,Some(1))

      val profile = Profile(SampleCode("XX-X-ANY-1"), SampleCode("XX-X-ANY-1"), "", "", AlphanumericId("MULTIPLE_VICTIMA"),
        Map(), None, None, Some(2), None, None, None, false, true, false)

      val categoryService = mock[CategoryService]
      when(categoryService.listCategories).thenReturn(Stubs.categoryMap)
      when(categoryService.getCategory(any[AlphanumericId])).thenReturn(Some(category))

      val profileRepository = mock[MongoProfileRepository]
      when(profileRepository.findByCode(analysis.globalCode)).thenReturn(Future.successful(Some(profile)))
      when(profileRepository.addAnalysis(any[SampleCode], any[Analysis], any[GenotypificationByType], any[Option[Profile.LabeledGenotypification]], any[Option[Seq[MatchingRule]]], any[Option[Profile.Mismatch]])).thenReturn(Future.successful(profile.globalCode))

      val mService = mock[MatchingService]
      when(mService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Seq())
      when(mService.matchesWithPartialHit(any[SampleCode])).thenReturn(Future.successful(Seq(Stubs.matchResult)))
      when(mService.matchesNotDiscarded(any[SampleCode])).thenReturn(Future.successful(Seq(Stubs.matchResult)))
      val probabilityService = mock[ProbabilityService]
      when(probabilityService.getStats(any[String])).thenReturn(Future.successful(Some(Stubs.statOption)))

      val interconnectionService = mock[InterconnectionService]
      when(interconnectionService.isFromCurrentInstance(any[SampleCode])).thenReturn(true)
      val profileDataRepository = mock[ProfileDataRepository]
      when(profileDataRepository.isDeleted(any[SampleCode])).thenReturn(Future.successful(Some(false)))
      when(profileDataRepository.getProfileUploadStatusByGlobalCode(any[SampleCode])).thenReturn(Future.successful(None))

      val target = new ProfileServiceImpl(null, profileRepository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryService, Stubs.notificationServiceMock, probabilityService, locusServiceMock, Stubs.traceServiceMock, null, analysisTypeServiceMock, Stubs.labelsSets,interconnectionService)

      val future = target.importProfile(Stubs.profileData, analysis)
      val result = Await.result(future, duration)

      result.isLeft mustBe true
      result.left.get.head contains "E0112" mustBe true
    }

    "fail when no default stats configured E0610" in {
      val analysis = NewAnalysis(Stubs.sampleCode, "pdg", "token", Some("Identifiler"), None, Map(), None, None, None)

      val category = FullCategory(AlphanumericId("CATTT"),
        "", None, AlphanumericId("GROUP"),
        false, false, true, false,manualLoading=true,
        Map.empty, Seq.empty, Seq.empty, Seq.empty,Some(1))

      val profile = Profile(SampleCode("XX-X-ANY-1"), SampleCode("XX-X-ANY-1"), "", "", AlphanumericId("MULTIPLE_VICTIMA"),
        Map(), None, None, Some(2), None, None, None, false, true, false)

      val categoryService = mock[CategoryService]
      when(categoryService.listCategories).thenReturn(Stubs.categoryMap)
      when(categoryService.getCategory(any[AlphanumericId])).thenReturn(Some(category))

      val profileRepository = mock[MongoProfileRepository]
      when(profileRepository.findByCode(analysis.globalCode)).thenReturn(Future.successful(None))
      when(profileRepository.addAnalysis(any[SampleCode], any[Analysis], any[GenotypificationByType], any[Option[Profile.LabeledGenotypification]], any[Option[Seq[MatchingRule]]], any[Option[Profile.Mismatch]])).thenReturn(Future.successful(profile.globalCode))

      val mService = mock[MatchingService]
      when(mService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Seq())
      when(mService.matchesWithPartialHit(any[SampleCode])).thenReturn(Future.successful(Seq(Stubs.matchResult)))

      val probabilityService = mock[ProbabilityService]
      when(probabilityService.getStats(any[String])).thenReturn(Future.successful(None))

      val target = new ProfileServiceImpl(null, profileRepository, null, kitServiceMock, mService, qualityParamsMockProvider, categoryService, Stubs.notificationServiceMock, probabilityService, locusServiceMock, Stubs.traceServiceMock, null, analysisTypeServiceMock, Stubs.labelsSets)

      val future = target.importProfile(Stubs.profileData, analysis)
      val result = Await.result(future, duration)

      result.isLeft mustBe true
      result.left.get.head mustBe "E0610: No están configuradas las opciones estadísticas por defecto."

    }

    "add new locus with inconsistencies" in {

      val genotypification =
        Map(1 -> Map("LOCUS 1" -> List(Allele(2), Allele(3)),
          "LOCUS 2" -> List(Allele(15)),
          "LOCUS 3" -> List(Allele(1), Allele(2)),
          "LOCUS 4" -> List(Allele(1.5)),
          "LOCUS 5" -> List(Allele(22))))
      val profile = Profile(null, null, null, null, null, genotypification, None, None, None, None)

      val newAn = NewAnalysis(
        Stubs.sampleCode,
        "pdg",
        "token",
        None,
        Some(1),
        Map("LOCUS 1" -> List(Allele(2.1), Allele(3))),
        None,
        Some(1),
        None)

      val cacheService = mock[CacheService]
      when(cacheService.pop(TemporaryAssetKey(newAn.token))).thenReturn(Option(List[TemporaryFile]())) // no images

      val profileRespository = mock[MongoProfileRepository]
      when(profileRespository.findByCode(newAn.globalCode)).thenReturn(Future.successful(Some(profile)))

      val profileDataRepository = mock[ProfileDataRepository]
      when(profileDataRepository.findByCode(newAn.globalCode)).thenReturn(Future.successful(Some(Stubs.profileData)))

      val mService = mock[MatchingService]
      when(mService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Seq())
      when(mService.matchesWithPartialHit(any[SampleCode])).thenReturn(Future.successful(Seq()))

      val probabilityService = mock[ProbabilityService]
      when(probabilityService.getStats(any[String])).thenReturn(Future.successful(Some(Stubs.statOption)))

      val interconnectionService = mock[InterconnectionService]
      when(interconnectionService.isFromCurrentInstance(any[SampleCode])).thenReturn(true)
      when(profileDataRepository.isDeleted(any[SampleCode])).thenReturn(Future.successful(Some(false)))
      when(profileDataRepository.getProfileUploadStatusByGlobalCode(any[SampleCode])).thenReturn(Future.successful(None))

      val target = new ProfileServiceImpl(cacheService, profileRespository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryServiceMock, Stubs.notificationServiceMock, probabilityService, locusServiceMock, Stubs.traceServiceMock, null, analysisTypeServiceMock, Stubs.labelsSets,interconnectionService)

      val future = target.create(newAn)
      val result = Await.result(future, duration)

      result.isLeft mustBe true

    }

    "start matching process" in {

      val sc = Stubs.profileData.globalCode
      val profile = Profile(sc, sc, null, null, null, Map(), None, None, Some(2), None, None, None, false, true, false)

      val categoryService = mock[CategoryService]
      when(categoryService.listCategories).thenReturn(Stubs.categoryMap)
      when(categoryService.getCategory(any[AlphanumericId])).thenReturn(Option(Stubs.fullCatA1))
      when(categoryService.getCategoryTypeFromFullCategory(any[FullCategory])).thenReturn(None)

      val profileRepository = mock[MongoProfileRepository]
      when(profileRepository.findByCode(any[SampleCode])).thenReturn(Future.successful(Option(profile)))
      when(profileRepository.addAnalysis(any[SampleCode], any[Analysis], any[GenotypificationByType], any[Option[Profile.LabeledGenotypification]], any[Option[Seq[MatchingRule]]], any[Option[Profile.Mismatch]])).thenReturn(Future.successful(Stubs.sampleCode))

      val mService = mock[MatchingService]
      when(mService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Seq())
      when(mService.matchesWithPartialHit(any[SampleCode])).thenReturn(Future.successful(Seq()))
      when(mService.matchesNotDiscarded(any[SampleCode])).thenReturn(Future.successful(Seq()))

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.getAllCourtCases(any[PedigreeSearch])).thenReturn(Future.successful(Seq()))

      val interconnectionService = mock[InterconnectionService]
      when(interconnectionService.isFromCurrentInstance(any[SampleCode])).thenReturn(true)
      val profileDataRepository = mock[ProfileDataRepository]
      when(profileDataRepository.isDeleted(any[SampleCode])).thenReturn(Future.successful(Some(false)))
      when(profileDataRepository.getProfileUploadStatusByGlobalCode(any[SampleCode])).thenReturn(Future.successful(None))

      val target = new ProfileServiceImpl(null, profileRepository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryService, Stubs.notificationServiceMock, null, locusServiceMock, Stubs.traceServiceMock, pedigreeService, analysisTypeServiceMock, Stubs.labelsSets,interconnectionService)

      val future = target.importProfile(Stubs.profileData, newAnalysis)
      val result = Await.result(future, duration)

      result must not be null
      result.isRight mustBe true

      verify(mService).findMatches(sc,None)
    }

  }

  "save labels" should {
    "not save labels if profile is already associated E0116" in {

      val locusService = mock[LocusService]
      when(locusService.getLocusByAnalysisTypeName(any[String])).thenReturn(Future.successful(Stubs.locus.map(_.id)))

      val profileRepository = mock[MongoProfileRepository]
      when(profileRepository.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.newProfile)))

      val profileDataRepository = mock[ProfileDataRepository]
      when(profileDataRepository.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.profileData)))

      val mService = mock[MatchingService]
      when(mService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Seq("AR-C-SHDG-1100"))

      val interconnectionService = mock[InterconnectionService]
      when(interconnectionService.isFromCurrentInstance(any[SampleCode])).thenReturn(true)
      when(profileDataRepository.isDeleted(any[SampleCode])).thenReturn(Future.successful(Some(false)))
      when(profileDataRepository.getProfileUploadStatusByGlobalCode(any[SampleCode])).thenReturn(Future.successful(None))

      val target = new ProfileServiceImpl(null, profileRepository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryServiceMock, Stubs.notificationServiceMock, null, locusServiceMock, Stubs.traceServiceMock, null, analysisTypeServiceMock, Stubs.labelsSets,interconnectionService)

      val result = Await.result(target.saveLabels(Stubs.sampleCode, Stubs.labeledGenotypification, "pdg"), duration)

      result.isLeft mustBe true
      result mustBe Left(List("E0116: No se puede realizar la asociación, ya existe un perfil asociado."))
    }

    "save labels and start matching process" in {

      val locusService = mock[LocusService]
      when(locusService.getLocusByAnalysisTypeName(any[String])).thenReturn(Future.successful(Stubs.locus.map(_.id)))

      val profileRepository = mock[MongoProfileRepository]
      when(profileRepository.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.newProfile)))
      when(profileRepository.saveLabels(any[SampleCode], any[Profile.LabeledGenotypification])).thenReturn(Future.successful(Stubs.sampleCode))

      val profileDataRepository = mock[ProfileDataRepository]
      when(profileDataRepository.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.profileData)))

      val mService = mock[MatchingService]
      when(mService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Seq())

      val interconnectionService = mock[InterconnectionService]
      when(interconnectionService.isFromCurrentInstance(any[SampleCode])).thenReturn(true)
      when(profileDataRepository.isDeleted(any[SampleCode])).thenReturn(Future.successful(Some(false)))
      when(profileDataRepository.getProfileUploadStatusByGlobalCode(any[SampleCode])).thenReturn(Future.successful(None))
      categoryServiceMock
      val target = new ProfileServiceImpl(null, profileRepository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryServiceMock, Stubs.notificationServiceMock, null, locusServiceMock, Stubs.traceServiceMock, null, analysisTypeServiceMock, Stubs.labelsSets,interconnectionService)

      val result = Await.result(target.saveLabels(Stubs.sampleCode, Stubs.labeledGenotypification, "pdg"), duration)

      result mustBe Right(Stubs.sampleCode)
      verify(mService).findMatches(Stubs.sampleCode,None)
    }

    "save labels and trace action" in {
      val traceService = mock[TraceService]
      when(traceService.add(any[Trace])).thenReturn(Future.successful(Right(1l)))

      val locusService = mock[LocusService]
      when(locusService.getLocusByAnalysisTypeName(any[String])).thenReturn(Future.successful(Stubs.locus.map(_.id)))

      val profileRepository = mock[MongoProfileRepository]
      when(profileRepository.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.newProfile)))
      when(profileRepository.saveLabels(any[SampleCode], any[Profile.LabeledGenotypification])).thenReturn(Future.successful(Stubs.sampleCode))
      when(profileRepository.updateAssocTo(any[SampleCode], any[SampleCode])).thenReturn(Future.successful(("pdg", "code", SampleCode("AR-C-SHDG-1"))))

      val profileDataRepository = mock[ProfileDataRepository]
      when(profileDataRepository.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.profileData)))

      val mService = mock[MatchingService]
      when(mService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Seq())
      val interconnectionService = mock[InterconnectionService]
      when(interconnectionService.isFromCurrentInstance(any[SampleCode])).thenReturn(true)
      when(profileDataRepository.isDeleted(any[SampleCode])).thenReturn(Future.successful(Some(false)))
      when(profileDataRepository.getProfileUploadStatusByGlobalCode(any[SampleCode])).thenReturn(Future.successful(None))

      val target = new ProfileServiceImpl(null, profileRepository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryServiceMock, Stubs.notificationServiceMock, null, locusServiceMock, traceService, null, analysisTypeServiceMock, Stubs.labelsSets,interconnectionService)

      val labeledGenotypification = Map("AR-C-SHDG-1234" -> Map("LOCUS 1" -> List(Allele(1))))
      Await.result(target.saveLabels(Stubs.sampleCode, labeledGenotypification, "pdg"), duration)

      verify(traceService, times(2)).add(any[Trace])
    }

    "not merge or validate mitochondrial genotypification" in {
      val cacheService = mock[CacheService]
      when(cacheService.pop(TemporaryAssetKey(newAnalysis.token))).thenReturn(Option(List[TemporaryFile]())) // no images

      val mockProfile = Stubs.newProfile // existing profile
      val profileRepository = mock[MongoProfileRepository]
      when(profileRepository.findByCode(newAnalysis.globalCode)).thenReturn(Future.successful(Option(mockProfile)))
      when(profileRepository.addAnalysis(any[SampleCode], any[Analysis], any[GenotypificationByType], any[Option[Profile.LabeledGenotypification]], any[Option[Seq[MatchingRule]]], any[Option[Profile.Mismatch]])).thenReturn(Future.successful(mockProfile.globalCode))

      val profileDataRepository = mock[ProfileDataRepository]
      when(profileDataRepository.findByCode(newAnalysis.globalCode)).thenReturn(Future.successful(Option(Stubs.profileData)))

      val probabilityService = mock[ProbabilityService]
      when(probabilityService.getStats(any[String])).thenReturn(Future.successful(Some(Stubs.statOption)))

      val mService = mock[MatchingService]
      when(mService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Seq())
      when(mService.matchesWithPartialHit(any[SampleCode])).thenReturn(Future.successful(Seq()))

      val analysisTypeServiceMock = mock[AnalysisTypeService]
      when(analysisTypeServiceMock.getById(any[Int])).thenReturn(Future.successful(Some(Stubs.analysisTypes.last)))

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.getAllCourtCases(any[PedigreeSearch])).thenReturn(Future.successful(Seq()))

      val interconnectionService = mock[InterconnectionService]
      when(interconnectionService.isFromCurrentInstance(any[SampleCode])).thenReturn(true)
      when(profileDataRepository.isDeleted(any[SampleCode])).thenReturn(Future.successful(Some(false)))
      when(profileDataRepository.getProfileUploadStatusByGlobalCode(any[SampleCode])).thenReturn(Future.successful(None))

      val target = new ProfileServiceImpl(cacheService, profileRepository, profileDataRepository, kitServiceMock, mService, qualityParamsMockProvider, categoryServiceMock, Stubs.notificationServiceMock, probabilityService, locusServiceMock, Stubs.traceServiceMock, pedigreeService, analysisTypeServiceMock, Stubs.labelsSets,interconnectionService)

      val genotypification: Map[String, List[AlleleValue]] = Map(
        "LOCUS 4" -> List(Mitocondrial('C', 1), Mitocondrial('A', 2.1), Mitocondrial('-', 3)))

      val analysis: NewAnalysis = new NewAnalysis(
        newAnalysis.globalCode, "pdg", "token", Some("Identifiler"), None, genotypification, None, Some(1), None)

      val future = target.create(analysis)
      val result = Await.result(future, duration)

      result.isRight mustBe false
    }

  }

}