package pedigrees

import java.util.Date

import configdata.CategoryService
import matching.Stringency._
import matching._
import org.bson.types.ObjectId
import org.mockito.Matchers._
import org.mockito.Mockito.{when, _}
import org.scalatest.mock.MockitoSugar
import pedigree.PedigreeStatus.PedigreeStatus
import pedigree._
import play.api.libs.json.{JsArray, Json}
import profile.{Allele, Profile}
import profiledata.{ProfileData, ProfileDataRepository}
import specs.PdgSpec
import stubs.Stubs
import stubs.Stubs.{catA1, onlyDate, pmdf}
import trace.{Trace, TracePedigree, TraceService}
import types.{AlphanumericId, MongoDate, MongoId, SampleCode}

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}
import scala.sys.process.ProcessImpl

class PedigreeMatchesServiceTest extends PdgSpec with MockitoSugar {

  val traceService = mock[TraceService]
  when(traceService.addTracePedigree(any[TracePedigree])).thenReturn(Future.successful(Right(1L)))
  val pedigreeDataRepository = mock[PedigreeDataRepository]
  val opt:Option[String] = None
  when(pedigreeDataRepository.getPedigreeDescriptionById(any[Long])).thenReturn(Future.successful(opt,opt))
  val duration = Duration(10, SECONDS)

  val profile1 = Profile(null, SampleCode("AR-C-SHDG-0001"), null, null, null, Map(
    1 -> Map("CSF1PO" -> List(Allele(8), Allele(8)),
      "D13S317" -> List(Allele(6), Allele(6)),
      "D16S539" -> List(Allele(7), Allele(9)))
  ), None, None, None, None)

  val profile2 = Profile(null, SampleCode("AR-C-SHDG-0003"), null, null, null, Map(
    1 -> Map("CSF1PO" -> List(Allele(8), Allele(11)),
      "D13S317" -> List(Allele(6), Allele(7)),
      "D16S539" -> List(Allele(9), Allele(5)))
  ), None, None, None, None)

  val profiles = Seq(profile1, profile2)

  val frequencyTable: BayesianNetwork.FrequencyTable = Map(
    "LOCUS" -> Map(
      0.0 -> 0.000287455444406117,
      6.0 -> 0.000287455444406117,
      7.0 -> 0.00167,
      8.0 -> 0.00448,
      8.3 -> 0.000287455444406117,
      9.0 -> 0.02185,
      10.0 -> 0.26929,
      10.3 -> 0.000287455444406117,
      11.0 -> 0.28222,
      12.0 -> 0.34753,
      13.0 -> 0.06209,
      14.0 -> 0.00868,
      15.0 -> 0.00167)
  )

  "PedigreeMatchService" must {
    "get matches by pedigree match card search grouped by pedigree" in {
      val mockPedigreeDataRepository = mock[PedigreeDataRepository]
      val mockPedigreeMatchesRepository = mock[PedigreeMatchesRepository]
      val mockProfileDataRepo = mock[ProfileDataRepository]

      val pedigreeMatchCardSearch = PedigreeMatchCardSearch("userId", true, "pedigree")
      val pedigreeMatch1 = PedigreeMatch(Left(56), MongoDate(new Date()), 3, "Assignee56")
      val pedigreeMatch2 = PedigreeMatch(Left(150), MongoDate(new Date()), 15, "Assignee150")
      val pedigreeMatches = Seq[PedigreeMatch](pedigreeMatch1, pedigreeMatch2)
      when(mockPedigreeMatchesRepository.getMatches(pedigreeMatchCardSearch)).thenReturn(Future.successful(pedigreeMatches))
      when(mockPedigreeMatchesRepository.getMejorLrPedigree(any[Long])).thenReturn(Future.successful(Some(MatchCardMejorLrPed(
        "AR-C-SHDG-1101",
        "AR-C-SHDG-1101",
        Some("UNICO"),
        0.0,
        MatchStatus.hit
      ))))
      when(mockProfileDataRepo.get(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.profileData)))
      when(mockPedigreeMatchesRepository.numberOfDiscardedMatches(any[Long])).thenReturn(Future.successful(1))
      when(mockPedigreeMatchesRepository.numberOfHitMatches(any[Long])).thenReturn(Future.successful(1))
      when(mockPedigreeMatchesRepository.getTypeCourtCasePedigree(any[Long])).thenReturn(Future.successful(Some("MPI")))
      when(mockPedigreeMatchesRepository.countProfilesHitPedigrees(any[String])).thenReturn(Future.successful(1))
      when(mockPedigreeMatchesRepository.numberOfPendingMatches(any[Long])).thenReturn(Future.successful(1))
      val categoryService = mock[CategoryService]
      when(categoryService.getCategory(any[AlphanumericId])).thenReturn(Some(Stubs.fullCatA1))
      val courtCase56 = CourtCase(
        56,
        "InternalSampleCode56",
        None,
        None,
        "Assignee56",
        None,
        None,
        None,
        PedigreeStatus.Active,
        null,
        "MPI"
      )

      val pedigree56Metadata = PedigreeMetaData(56, 56, "InternalSampleCode56", new Date(), PedigreeStatus.Active, "Assignee56");
      val pedigree56 = PedigreeDataCreation(pedigree56Metadata, None);

      val courtCase150 = CourtCase(
        150,
        "InternalSampleCode150",
        None,
        None,
        "Assignee150",
        None,
        None,
        None,
        PedigreeStatus.Active,
        null,
        "MPI"
      )
      val pedigree150Metadata = PedigreeMetaData(150, 150, "InternalSampleCode150", new Date(), PedigreeStatus.Active, "Assignee150");
      val pedigree150 = PedigreeDataCreation(pedigree150Metadata, None);


      when(mockPedigreeDataRepository.getPedigreeMetaData(56)).thenReturn(Future.successful(Some(pedigree56)))
      when(mockPedigreeDataRepository.getPedigreeMetaData(150)).thenReturn(Future.successful(Some(pedigree150)))
      when(mockProfileDataRepo.getGlobalCode(pedigreeMatchCardSearch.profile.getOrElse(""))).thenReturn(Future.successful(None))

      val service = new PedigreeMatchesServiceImpl(mockPedigreeDataRepository, mockPedigreeMatchesRepository, null, null, null, null, null,null,mockProfileDataRepo, Stubs.notificationServiceMock,categoryService)
      val result = Await.result(service.getMatches(pedigreeMatchCardSearch), duration)

      result.size mustBe 2
      result(0).matchCard.assignee mustBe "Assignee56"
      result(0).matchCard.count mustBe 3
      result(0).matchCard.title mustBe "InternalSampleCode56"
      result(0).matchCard.lastMatchDate mustBe pedigreeMatch1.lastMatchDate.date
      result(0).matchCard.id mustBe "56"
      result(1).matchCard.assignee mustBe "Assignee150"
      result(1).matchCard.count mustBe 15
      result(1).matchCard.title mustBe "InternalSampleCode150"
      result(1).matchCard.lastMatchDate mustBe pedigreeMatch2.lastMatchDate.date
      result(1).matchCard.id mustBe "150"

      verify(mockPedigreeDataRepository, times(1)).getPedigreeMetaData(56)
      verify(mockPedigreeDataRepository, times(1)).getPedigreeMetaData(150)
    }

    "get matches by pedigree match card search grouped by profile" in {
      val mockPedigreeMatchesRepository = mock[PedigreeMatchesRepository]
      val pedigreeMatchCardSearch = PedigreeMatchCardSearch("userId", true, "profile")
      val pedigreeMatch1 = PedigreeMatch(Right("AR-C-SHDG-1101"), MongoDate(new Date()), 3, "Assignee1101")
      val pedigreeMatch2 = PedigreeMatch(Right("AR-C-SHDG-1102"), MongoDate(new Date()), 15, "Assignee1102")
      val pedigreeMatches = Seq[PedigreeMatch](pedigreeMatch1, pedigreeMatch2)
      val mockProfileDataRepo = mock[ProfileDataRepository]
      when(mockPedigreeMatchesRepository.getMatches(pedigreeMatchCardSearch)).thenReturn(Future.successful(pedigreeMatches))
      when(mockProfileDataRepo.getGlobalCode(pedigreeMatchCardSearch.profile.getOrElse(""))).thenReturn(Future.successful(None))

      when(mockProfileDataRepo.get(any[SampleCode])).thenReturn(Future.successful(None))
      when(mockPedigreeMatchesRepository.getMejorLrPedigree(any[Long])).thenReturn(Future.successful(Some(MatchCardMejorLrPed(
        "1",
        "sampleCode",
        Some("UNICO"),
        0.0,
        MatchStatus.hit
      ))))
      val profileData1 = ProfileData(catA1.id, SampleCode("AR-C-SHDG-1101"),
        Option("attorney"), Option("SANGRE"), Option("court desc"), Option("ASESINATO"),
        Option("PERSONAS"), Option("case number"), "AR-C-SHDG-1101", "assignee", "SHDG",
        false, None, Some("responsibleGeneticist"), Some(onlyDate), Some(onlyDate),
        Option(onlyDate), Some(pmdf),false)
      val profileData2 = ProfileData(catA1.id, SampleCode("AR-C-SHDG-1102"),
        Option("attorney"), Option("SANGRE"), Option("court desc"), Option("ASESINATO"),
        Option("PERSONAS"), Option("case number"), "AR-C-SHDG-1102", "assignee", "SHDG",
        false, None, Some("responsibleGeneticist"), Some(onlyDate), Some(onlyDate),
        Option(onlyDate), Some(pmdf),false)
      when(mockProfileDataRepo.get(SampleCode("AR-C-SHDG-1101"))).thenReturn(Future.successful(Some(profileData1)))
      when(mockProfileDataRepo.get(SampleCode("AR-C-SHDG-1102"))).thenReturn(Future.successful(Some(profileData2)))
      when(mockPedigreeMatchesRepository.numberOfDiscardedMatches(any[Long])).thenReturn(Future.successful(1))
      when(mockPedigreeMatchesRepository.numberOfHitMatches(any[Long])).thenReturn(Future.successful(1))
      when(mockPedigreeMatchesRepository.getTypeCourtCasePedigree(any[Long])).thenReturn(Future.successful(Some("MPI")))
      when(mockPedigreeMatchesRepository.countProfilesHitPedigrees(any[String])).thenReturn(Future.successful(1))
      when(mockPedigreeMatchesRepository.countProfilesDiscardedPedigrees(any[String])).thenReturn(Future.successful(1))
      when(mockPedigreeMatchesRepository.profileNumberOfPendingMatches(any[String])).thenReturn(Future.successful(1))
      when(mockPedigreeMatchesRepository.getMejorLrProf(any[String])).thenReturn(Future.successful(Some(MatchCardMejorLrPed(
        "1",
        "sampleCode",
        Some("categoryId"),
        1.0,
        MatchStatus.hit
      ))))
      val pedigreeDataRepository = mock[PedigreeDataRepository]
      when(pedigreeDataRepository.getPedigreeMetaData(any[Long])).thenReturn(Future.successful(Some(PedigreeDataCreation (
        PedigreeMetaData (
          1L,
          2L,
          "name",
          new Date(),
          PedigreeStatus.Active,
      "assignee",
          "courtCaseName",
      Some(false)
      ),
        Some(PedigreeGenogram(
          1L,
          "assignee",
          Nil,
           PedigreeStatus.UnderConstruction,
          None,
        false,
        0.5,
         false,
          Some(1),
        "MPI",
          Some(1),
          7L))
      ))))
      val categoryService = mock[CategoryService]
      when(categoryService.getCategory(any[AlphanumericId])).thenReturn(Some(Stubs.fullCatA1))


      val service = new PedigreeMatchesServiceImpl(pedigreeDataRepository, mockPedigreeMatchesRepository, null, null, null, null, null, null,mockProfileDataRepo, Stubs.notificationServiceMock,categoryService)
      val result = Await.result(service.getMatches(pedigreeMatchCardSearch), duration)

      result.size mustBe 2
      result(0).matchCard.assignee mustBe "Assignee1101"
      result(0).matchCard.count mustBe 3
      result(0).matchCard.title mustBe "AR-C-SHDG-1101"
      result(0).matchCard.lastMatchDate mustBe pedigreeMatch1.lastMatchDate.date
      result(0).matchCard.id mustBe "AR-C-SHDG-1101"
      result(1).matchCard.assignee mustBe "Assignee1102"
      result(1).matchCard.count mustBe 15
      result(1).matchCard.title mustBe "AR-C-SHDG-1102"
      result(1).matchCard.lastMatchDate mustBe pedigreeMatch2.lastMatchDate.date
      result(1).matchCard.id mustBe "AR-C-SHDG-1102"
    }

    "determine if a pedigree is editable" in {
      val mockResult = true

      val pedigreeMatchesRepository = mock[PedigreeMatchesRepository]
      when(pedigreeMatchesRepository.allMatchesDiscarded(any[Long])).thenReturn(Future.successful(mockResult))

      val service = new PedigreeMatchesServiceImpl(null, pedigreeMatchesRepository, null, null, null, null, null,null, null, Stubs.notificationServiceMock,null)

      val result = Await.result(service.allMatchesDiscarded(1l), duration)

      result mustBe mockResult
    }

    "get matches by group and pedigree id" in {
      val mockResult = Seq(PedigreeMissingInfoMatch(MongoId("58ac62e6ebb12c3bfcc1afab"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1103"), "jerkovicm", MatchStatus.pending, None),
        PedigreeMatchingProfile(12, NodeAlias("PI2"), SampleCode("AR-C-SHDG-1102"), "tst-admintist", MatchStatus.pending,"MPI",7l)))
      val pedigreeMatchGroupSearch = PedigreeMatchGroupSearch("pdg", true, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "profileStatus", true)

      val pedigreeMatchesRepository = mock[PedigreeMatchesRepository]
      when(pedigreeMatchesRepository.getMatchesByGroup(any[PedigreeMatchGroupSearch])).thenReturn(Future.successful(mockResult))
      val profileDataRepo: ProfileDataRepository = mock[ProfileDataRepository]
      when(profileDataRepo.get(any[SampleCode])).thenReturn(Future.successful(None))
      val service = new PedigreeMatchesServiceImpl(null, pedigreeMatchesRepository, null, null, null, null, null, null,profileDataRepo, Stubs.notificationServiceMock,null)
      val result = Await.result(service.getMatchesByGroup(pedigreeMatchGroupSearch), duration)

      result.map(_.pedigreeMatchResult) mustBe mockResult
    }

    "get matches by group and profile global code" in {
      val mockResult = Seq(PedigreeMissingInfoMatch(MongoId("58ac62e6ebb12c3bfcc1afab"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1103"), "jerkovicm", MatchStatus.pending, None),
        PedigreeMatchingProfile(12, NodeAlias("PI2"), SampleCode("AR-C-SHDG-1102"), "tst-admintist", MatchStatus.pending,"MPI",7l)))
      val pedigreeMatchGroupSearch = PedigreeMatchGroupSearch("pdg", true, "AR-C-SHDG-1103", "profile", PedigreeMatchKind.DirectLink, 0, 30, "profileStatus", true)

      val pedigreeMatchesRepository = mock[PedigreeMatchesRepository]
      when(pedigreeMatchesRepository.getMatchesByGroup(any[PedigreeMatchGroupSearch])).thenReturn(Future.successful(mockResult))

      val profileDataRepo: ProfileDataRepository = mock[ProfileDataRepository]
      when(profileDataRepo.get(any[SampleCode])).thenReturn(Future.successful(None))

      val service = new PedigreeMatchesServiceImpl(null, pedigreeMatchesRepository, null, null, null, null, null, null,profileDataRepo, Stubs.notificationServiceMock,null)
      val result = Await.result(service.getMatchesByGroup(pedigreeMatchGroupSearch), duration)

      result.map(_.pedigreeMatchResult) mustBe mockResult
    }

    "discard match both sides as super user" in {
      val id = "58ac62e6ebb12c3bfcc1afac"
      val mockResult = Right(id)

      val pedigreeMatchesRepository = mock[PedigreeMatchesRepository]
      when(pedigreeMatchesRepository.discardPedigree(id)).thenReturn(Future.successful(mockResult))
      when(pedigreeMatchesRepository.discardProfile(id)).thenReturn(Future.successful(mockResult))
      when(pedigreeMatchesRepository.getMatchById(id)).thenReturn(Future.successful(Some(Stubs.pedigreeMatch)))

      val service = new PedigreeMatchesServiceImpl(null, pedigreeMatchesRepository, null, null, null, null, null, null,null, Stubs.notificationServiceMock,null)
      val result = Await.result(service.discard(id, "pdg", true), duration)

      result mustBe mockResult
      verify(pedigreeMatchesRepository).discardPedigree(id)
      verify(pedigreeMatchesRepository).discardProfile(id)
    }

    "discard match both sides as assignee" in {
      val id = "58ac62e6ebb12c3bfcc1afac"
      val mockResult = Right(id)

      val pedigreeMatch = PedigreeDirectLinkMatch(MongoId("58ac62e6ebb12c3bfcc1afaa"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "tst-admintist", MatchStatus.pending, None),
        PedigreeMatchingProfile(12, NodeAlias("PI1"), SampleCode("AR-C-SHDG-1101"), "tst-admintist", MatchStatus.hit,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      val pedigreeMatchesRepository = mock[PedigreeMatchesRepository]
      when(pedigreeMatchesRepository.discardPedigree(id)).thenReturn(Future.successful(mockResult))
      when(pedigreeMatchesRepository.discardProfile(id)).thenReturn(Future.successful(mockResult))
      when(pedigreeMatchesRepository.getMatchById(id)).thenReturn(Future.successful(Some(pedigreeMatch)))
      val service = new PedigreeMatchesServiceImpl(null, pedigreeMatchesRepository, null, null, null, null, null, traceService,null, Stubs.notificationServiceMock,null)
      val result = Await.result(service.discard(id, "tst-admintist", false), duration)

      result mustBe mockResult
      verify(pedigreeMatchesRepository).discardPedigree(id)
      verify(pedigreeMatchesRepository).discardProfile(id)
    }

    "discard match pedigree side as assignee" in {
      val id = "58ac62e6ebb12c3bfcc1afac"
      val mockResult = Right(id)

      val pedigreeMatch = PedigreeDirectLinkMatch(MongoId("58ac62e6ebb12c3bfcc1afaa"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "pdg", MatchStatus.pending, None),
        PedigreeMatchingProfile(12, NodeAlias("PI1"), SampleCode("AR-C-SHDG-1101"), "tst-admintist", MatchStatus.hit,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      val pedigreeMatchesRepository = mock[PedigreeMatchesRepository]
      when(pedigreeMatchesRepository.discardPedigree(id)).thenReturn(Future.successful(mockResult))
      when(pedigreeMatchesRepository.discardProfile(id)).thenReturn(Future.successful(mockResult))
      when(pedigreeMatchesRepository.getMatchById(id)).thenReturn(Future.successful(Some(pedigreeMatch)))

      val service = new PedigreeMatchesServiceImpl(pedigreeDataRepository, pedigreeMatchesRepository, null, null, null, null, null, traceService,null, Stubs.notificationServiceMock,null)
      val result = Await.result(service.discard(id, "tst-admintist", false), duration)

      result mustBe mockResult
      verify(pedigreeMatchesRepository).discardPedigree(id)
    }

    "not discard match profile side as assignee" in {
      val id = "58ac62e6ebb12c3bfcc1afac"
      val mockResult =Left("E0642: El usuario no tiene permisos para descartar la coincidencia.")

      val pedigreeMatch = PedigreeDirectLinkMatch(MongoId("58ac62e6ebb12c3bfcc1afaa"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "pdg", MatchStatus.pending, None),
        PedigreeMatchingProfile(12, NodeAlias("PI1"), SampleCode("AR-C-SHDG-1101"), "tst-admintist", MatchStatus.hit,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      val pedigreeMatchesRepository = mock[PedigreeMatchesRepository]
      when(pedigreeMatchesRepository.discardPedigree(id)).thenReturn(Future.successful(mockResult))
      when(pedigreeMatchesRepository.discardProfile(id)).thenReturn(Future.successful(mockResult))
      when(pedigreeMatchesRepository.getMatchById(id)).thenReturn(Future.successful(Some(pedigreeMatch)))

      val service = new PedigreeMatchesServiceImpl(null, pedigreeMatchesRepository, null, null, null, null, null, traceService, null, Stubs.notificationServiceMock,null)
      val result = Await.result(service.discard(id, "pdg", false), duration)

      result mustBe mockResult
      verify(pedigreeMatchesRepository, times(0)).discardProfile(id)
      verify(pedigreeMatchesRepository, times(0)).discardPedigree(id)
    }

    "not discard match if user is not assignee or superuser E0642" in {
      val id = "58ac62e6ebb12c3bfcc1afac"
      val mockResult = Right(id)

      val pedigreeMatch = PedigreeDirectLinkMatch(MongoId("58ac62e6ebb12c3bfcc1afaa"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "tst-admintist", MatchStatus.pending, None),
        PedigreeMatchingProfile(12, NodeAlias("PI1"), SampleCode("AR-C-SHDG-1101"), "tst-admintist", MatchStatus.hit,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      val pedigreeMatchesRepository = mock[PedigreeMatchesRepository]
      when(pedigreeMatchesRepository.discardPedigree(id)).thenReturn(Future.successful(mockResult))
      when(pedigreeMatchesRepository.discardProfile(id)).thenReturn(Future.successful(mockResult))
      when(pedigreeMatchesRepository.getMatchById(id)).thenReturn(Future.successful(Some(pedigreeMatch)))

      val service = new PedigreeMatchesServiceImpl(null, pedigreeMatchesRepository, null, null, null, null, null, null,null, Stubs.notificationServiceMock,null)
      val result = Await.result(service.discard(id, "pdg", false), duration)

      result.isLeft mustBe true
      result mustBe Left("E0642: El usuario no tiene permisos para descartar la coincidencia.")
      verify(pedigreeMatchesRepository, times(0)).discardProfile(id)
      verify(pedigreeMatchesRepository, times(0)).discardPedigree(id)
    }

    "count matches" in {
      val mockResult = 5
      val search = PedigreeMatchCardSearch("pdg", false, "pedigree", 0, 30)

      val pedigreeMatchesRepository = mock[PedigreeMatchesRepository]
      val mockProfileDataRepo = mock[ProfileDataRepository]
      when(pedigreeMatchesRepository.countMatches(search)).thenReturn(Future.successful(mockResult))
      when(mockProfileDataRepo.getGlobalCode(search.profile.getOrElse(""))).thenReturn(Future.successful(None))
      val service = new PedigreeMatchesServiceImpl(null, pedigreeMatchesRepository, null, null, null, null, null, null,mockProfileDataRepo, Stubs.notificationServiceMock,null)

      val result = Await.result(service.countMatches(search), duration)
      result mustBe mockResult
    }

    "count matches by group" in {
      val mockResult = 10
      val pedigreeMatchGroupSearch = PedigreeMatchGroupSearch("pdg", true, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "profileStatus", true)

      val pedigreeMatchesRepository = mock[PedigreeMatchesRepository]
      when(pedigreeMatchesRepository.countMatchesByGroup(any[PedigreeMatchGroupSearch])).thenReturn(Future.successful(mockResult))

      val service = new PedigreeMatchesServiceImpl(null, pedigreeMatchesRepository, null, null, null, null, null, null, null,Stubs.notificationServiceMock,null)
      val result = Await.result(service.countMatchesByGroup(pedigreeMatchGroupSearch), duration)

      result mustBe mockResult
    }

    "get match by id return a jsvalue" in {
      val matchResult = PedigreeDirectLinkMatch(
        MongoId(new ObjectId().toString),
        MongoDate(new Date()),
        1,
        MatchingProfile(SampleCode("SA-M-PLE-1"), "assignee", MatchStatus.pending, None),
        PedigreeMatchingProfile(1, NodeAlias("PI1"), SampleCode("PE-D-IGREESAMPLE-1"), "assignee", MatchStatus.pending,"MPI",7l),
        NewMatchingResult(Stringency.HighStringency, Map("LOCUS" -> Stringency.HighStringency), 1, AlphanumericId("AAAA"), 1.0, 1.0, Algorithm.ENFSI)
      )
      val pedigreeMatchesRepository = mock[PedigreeMatchesRepository]
      when(pedigreeMatchesRepository.getMatchById("7654")).thenReturn(Future.successful(Some(matchResult)))

      val service = new PedigreeMatchesServiceImpl(null, pedigreeMatchesRepository, null, null, null, null, null, null,null, Stubs.notificationServiceMock,null)
      val result = Await.result(service.getMatchById("7654"), duration)

      result.isDefined mustBe true
      result.get.\("_id").as[String] mustBe "7654"

      val arrayResult = JsArray(
        Seq(
          Json.obj("globalCode" -> SampleCode("SA-M-PLE-1"),
          "stringency" -> Stringency.HighStringency,
          "matchingAlleles" -> Map("LOCUS" -> Stringency.HighStringency),
          "totalAlleles" -> 1,
          "categoryId" -> AlphanumericId("AAAA"),
          "type" -> 1,
          "status" -> Json.obj("SA-M-PLE-1" -> MatchStatus.pending,
            "PE-D-IGREESAMPLE-1" -> MatchStatus.pending))
        )
      )

      result.get.\("results") mustBe arrayResult
    }

    "delete all matches by pedigree" in {
      val mockResult = Right(12l)

      val pedigreeMatchesRepository = mock[PedigreeMatchesRepository]
      when(pedigreeMatchesRepository.deleteMatches(any[Long])).thenReturn(Future.successful(mockResult))

      val service = new PedigreeMatchesServiceImpl(null, pedigreeMatchesRepository, null, null, null, null, null, null, null, Stubs.notificationServiceMock,null)
      val result = Await.result(service.deleteMatches(12l), duration)

      result mustBe mockResult
    }

  }
}
