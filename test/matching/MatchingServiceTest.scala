package matching

import java.util.Date

import configdata.{CategoryService, FullCategory}
import connections.InterconnectionService
import org.mockito.Matchers.{any, anyObject}
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import profile._
import profiledata.ProfileDataRepository
import scenarios.ScenarioRepository
import specs.PdgSpec
import stubs.Stubs
import trace.{Trace, TraceService}
import types.{AlphanumericId, MongoDate, MongoId, SampleCode}
import matching.Stringency._
import org.bson.types.ObjectId

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}
import org.mockito.Mockito.verify
import org.mockito.Mockito._
import play.api.libs.json.Json

class MatchingServiceTest extends PdgSpec with MockitoSugar {

  val duration = Duration(1000, SECONDS)

  val fullCat = FullCategory(AlphanumericId("CATTT"),
    "", None, AlphanumericId("GROUP"),
    false, false, true, false,manualLoading=true,
    Map.empty, Seq.empty, Seq.empty, Seq.empty)

  "MatchingService" must {

    "get MatchingResult by firing and matching profile" in {
      val matchingRepository = mock[MongoMatchingRepository]
      val profileRepository = mock[ProfileRepository]
      val categoryService = mock[CategoryService]

      when(matchingRepository.getByFiringAndMatchingProfile(any[SampleCode], any[SampleCode])).thenReturn(Future.successful(Some(Stubs.matchResult)))
      when(profileRepository.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.newProfile)))
      when(categoryService.getCategory(any[AlphanumericId])).thenReturn(Some(fullCat))

      val matchingService = new MatchingServiceSparkImpl(profileRepository, matchingRepository, null, categoryService, null, null, null, Stubs.traceServiceMock, null, null, null, null, null, null)

      val result = Await.result(matchingService.getByFiringAndMatchingProfile(SampleCode("AR-B-IMBICE-400"), SampleCode("AR-B-IMBICE-500")), duration)
      result.isDefined mustBe true
      println(result.get)
    }

    "get an empty MatchingResult by firing and matching profile" in {
      val matchingRepository = mock[MongoMatchingRepository]
      when(matchingRepository.getByFiringAndMatchingProfile(any[SampleCode], any[SampleCode])).thenReturn(Future.successful(None))

      val matchingService = new MatchingServiceSparkImpl(mock[ProfileRepository], matchingRepository, null, mock[CategoryService], null, null, null, Stubs.traceServiceMock, null, null, null, null, null, null)

      val result = Await.result(matchingService.getByFiringAndMatchingProfile(SampleCode("AR-B-IMBICE-400"), SampleCode("AR-B-IMBICE-500")), duration)
      result.isDefined mustBe false
    }

    "convert discard - successful" in {

      val matchingRepository = mock[MongoMatchingRepository]

      when(matchingRepository.getByMatchingProfileId(any[String],any[Option[Boolean]],any[Option[Boolean]])).thenReturn(Future.successful(Some(Stubs.matchResult)))
      when(matchingRepository.convertStatus(any[String], any[SampleCode], any[String])).thenReturn(Future.successful(Seq()))

      val scenarioRepository = mock[ScenarioRepository]
      when(scenarioRepository.getByMatch(any[SampleCode], any[SampleCode], any[String], any[Boolean])).thenReturn(Future.successful(Seq()))

      val matchingService = new MatchingServiceSparkImpl(null, matchingRepository, Stubs.notificationServiceMock, null, null, scenarioRepository, null, Stubs.traceServiceMock, null, null, null, null, null, null)

      val result = Await.result(matchingService.convertDiscard("548395fssei2938f", SampleCode("AR-B-IMBICE-501"), false,false), duration)

      result.isRight mustBe true

    }

    "convert discard - failure E0902" in {

      val matchingRepository = mock[MongoMatchingRepository]

      when(matchingRepository.getByMatchingProfileId(any[String],any[Option[Boolean]],any[Option[Boolean]])).thenReturn(Future.successful(Some(Stubs.matchResult)))
      when(matchingRepository.convertStatus(any[String], any[SampleCode], any[String])).thenReturn(Future.successful(Seq()))

      val scenarioRepository = mock[ScenarioRepository]
      when(scenarioRepository.getByMatch(any[SampleCode], any[SampleCode], any[String], any[Boolean])).thenReturn(Future.successful(Seq(Stubs.newScenario)))

      val matchingService = new MatchingServiceSparkImpl(null, matchingRepository, null, null, null, scenarioRepository, null, Stubs.traceServiceMock, null, null, null, null, null, null)

      val result = Await.result(matchingService.convertDiscard("548395fssei2938f", SampleCode("AR-B-IMBICE-501"), false,false), duration)

      result.isLeft mustBe true
      result mustBe Left("E0902: La coincidencia está siendo utilizada en un escenario pendiente.")
    }

    "convert hit and trace action" in {
      val mongoId = MongoId(new ObjectId().toString)
      val matchResult = MatchResult(mongoId,
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-HIBA-500"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-B-IMBICE-500"),"tst-genetist",MatchStatus.pending,None),
        NewMatchingResult(ModerateStringency,
          Map("TH01" -> ModerateStringency, "CSF1PO" -> ModerateStringency, "D18S51" -> ModerateStringency, "AMEL" -> HighStringency, "D8S1179" -> ModerateStringency, "D3S1358" -> ModerateStringency, "D7S820" -> ModerateStringency, "FGA" -> ModerateStringency, "D16S539" -> HighStringency, "D13S317" -> ModerateStringency, "vWA" -> ModerateStringency, "D5S818" -> ModerateStringency, "TPOX" -> ModerateStringency, "D21S11" -> ModerateStringency),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI), 1)

      val traceService = mock[TraceService]
      when(traceService.add(any[Trace])).thenReturn(Future.successful(Right(1l)))

      val matchingRepository = mock[MongoMatchingRepository]

      when(matchingRepository.getByMatchingProfileId(any[String],any[Option[Boolean]],any[Option[Boolean]])).thenReturn(Future.successful(Some(matchResult)))
      when(matchingRepository.convertStatus(mongoId.id, SampleCode("AR-B-IMBICE-500"), "hit")).thenReturn(Future.successful(Seq()))

      val matchingService = new MatchingServiceSparkImpl(null, matchingRepository, Stubs.notificationServiceMock, null, null, null, null, traceService, null, null, null, null, null, null)

      Await.result(matchingService.convertHit(mongoId.id, SampleCode("AR-B-IMBICE-500"),false), duration)

      //Se agrega el thread sleep porque hay un promise onSuccess
      Thread.sleep(2000)

      verify(traceService, times(2)).add(any[Trace])

    }

    "convert discard and trace action" in {
      val mongoId = MongoId(new ObjectId().toString)
      val matchResult = MatchResult(mongoId,
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-HIBA-500"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-B-IMBICE-500"),"tst-genetist",MatchStatus.pending,None),
        NewMatchingResult(ModerateStringency,
          Map("TH01" -> ModerateStringency, "CSF1PO" -> ModerateStringency, "D18S51" -> ModerateStringency, "AMEL" -> HighStringency, "D8S1179" -> ModerateStringency, "D3S1358" -> ModerateStringency, "D7S820" -> ModerateStringency, "FGA" -> ModerateStringency, "D16S539" -> HighStringency, "D13S317" -> ModerateStringency, "vWA" -> ModerateStringency, "D5S818" -> ModerateStringency, "TPOX" -> ModerateStringency, "D21S11" -> ModerateStringency),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI), 1)

      val traceService = mock[TraceService]
      when(traceService.add(any[Trace])).thenReturn(Future.successful(Right(1l)))

      val matchingRepository = mock[MongoMatchingRepository]

      when(matchingRepository.getByMatchingProfileId(any[String],any[Option[Boolean]],any[Option[Boolean]])).thenReturn(Future.successful(Some(matchResult)))
      when(matchingRepository.convertStatus(mongoId.id, SampleCode("AR-B-IMBICE-500"), "discarded")).thenReturn(Future.successful(Seq()))

      val scenarioRepository = mock[ScenarioRepository]
      when(scenarioRepository.getByMatch(any[SampleCode], any[SampleCode], any[String], any[Boolean])).thenReturn(Future.successful(Seq()))

      val matchingService = new MatchingServiceSparkImpl(null, matchingRepository, Stubs.notificationServiceMock, null, null, scenarioRepository, null, traceService, null, null, null, null, null, null)

      Await.result(matchingService.convertDiscard(mongoId.id, SampleCode("AR-B-IMBICE-500"),false, false), duration)

      //Se agrega el thread sleep porque hay un promise onSuccess
      Thread.sleep(2000)

      verify(traceService, times(2)).add(any[Trace])

    }

    "get partial hits" in {
      val mockResult = Seq(Stubs.matchResult)

      val matchingRepository = mock[MongoMatchingRepository]
      when(matchingRepository.matchesWithPartialHit(any[SampleCode])).thenReturn(Future.successful(mockResult))

      val matchingService = new MatchingServiceSparkImpl(null, matchingRepository, null, null, null, null, null, Stubs.traceServiceMock, null, null, null, null, null, null)

      val result = Await.result(matchingService.matchesWithPartialHit(Stubs.sampleCode), duration)

      result mustBe mockResult
    }

    "get total matches" in {
      val matchingRepository = mock[MongoMatchingRepository]
      val mockProfileDataRepo = mock[ProfileDataRepository]
      when(matchingRepository.getTotalMatches(any[MatchCardSearch])).thenReturn(Future.successful(543))
      when(mockProfileDataRepo.getGlobalCode(any[String])).thenReturn(Future.successful(None))

      val matchingService = new MatchingServiceSparkImpl(null, matchingRepository, null, null, mockProfileDataRepo, null, null, null, null, null, null, null, null, null)

      val result = Await.result(matchingService.getTotalMatches(MatchCardSearch("pdg", false)), duration)

      result mustBe 543
    }

    "get matches" in {
      val matches = Seq(
        Json.obj(
          "_id" -> "AR-C-SHDG-1",
          "pending" -> 4,
          "discarded" -> 0,
          "hit" -> 0,
          "conflict" -> 0,
          "lastDate" -> MongoDate(new Date())
        ),
        Json.obj(
          "_id" -> "AR-C-SHDG-2",
          "pending" -> 0,
          "discarded" -> 0,
          "hit" -> 4,
          "conflict" -> 0,
          "lastDate" -> MongoDate(new Date())
        )
      )

      val matchingRepository = mock[MongoMatchingRepository]
      when(matchingRepository.getMatches(any[MatchCardSearch])).thenReturn(Future.successful(matches))
      when(matchingRepository.getProfileLr(any[SampleCode],any[Boolean])).thenReturn(Future.successful(MatchCardMejorLr(
        "internalSampleCode",
        AlphanumericId("UNICO"),
        12,
        MatchStatus.hit,
        MatchStatus.hit,
        0.52,
        2.5,
        25.6,
        SampleCode("AR-C-SHDG-1"),
        1
      )))

      val profileRepository = mock[ProfileRepository]
      when(profileRepository.findByCode(SampleCode("AR-C-SHDG-1"))).thenReturn(Future.successful(Some(Stubs.newProfile)))
      when(profileRepository.findByCode(SampleCode("AR-C-SHDG-2"))).thenReturn(Future.successful(Some(Stubs.newProfile)))


      val profileDataRepository = mock[ProfileDataRepository]
      when(profileDataRepository.findByCode(SampleCode("AR-C-SHDG-1"))).thenReturn(Future.successful(Some(Stubs.profileData)))
      when(profileDataRepository.findByCode(SampleCode("AR-C-SHDG-2"))).thenReturn(Future.successful(Some(Stubs.profileData)))
      when(profileDataRepository.getGlobalCode(any[String])).thenReturn(Future.successful(None))

      val matchingService = new MatchingServiceSparkImpl(profileRepository, matchingRepository, null, null, profileDataRepository, null, null, null, null, null, null, null, null, null)

      val result = Await.result(matchingService.getMatches(MatchCardSearch("pdg", false)), duration)

      result.size mustBe 2

      result(0).matchCard.conflict mustBe 0
      result(0).matchCard.hit mustBe 0
      result(0).matchCard.discarded mustBe 0
      result(0).matchCard.pending mustBe 4

      result(1).matchCard.conflict mustBe 0
      result(1).matchCard.hit mustBe 4
      result(1).matchCard.discarded mustBe 0
      result(1).matchCard.pending mustBe 0
    }

    "get total matches by group" in {
      val mockResult = 1000
      val search = MatchGroupSearch("user", true, SampleCode("AR-C-SHDG-500"), MatchKind.MixMix, 1, 1, "", true)

      val matchingRepository = mock[MongoMatchingRepository]
      when(matchingRepository.getTotalMatchesByGroup(any[MatchGroupSearch])).thenReturn(mockResult)

      val matchingService = new MatchingServiceSparkImpl(null, matchingRepository, null, null, null, null, null, null, null, null, null, null, null, null)

      val result = Await.result(matchingService.getTotalMatchesByGroup(search), duration)

      result mustBe mockResult
    }

    "get matches by group" in {
      val mockResult = Seq(Stubs.matchingResult)
      val matchResult = MatchResult(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-HIBA-500"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-B-IMBICE-500"),"tst-genetist",MatchStatus.pending,None),
        NewMatchingResult(ModerateStringency,
          Map("TH01" -> ModerateStringency, "CSF1PO" -> ModerateStringency, "D18S51" -> ModerateStringency, "AMEL" -> HighStringency, "D8S1179" -> ModerateStringency, "D3S1358" -> ModerateStringency, "D7S820" -> ModerateStringency, "FGA" -> ModerateStringency, "D16S539" -> HighStringency, "D13S317" -> ModerateStringency, "vWA" -> ModerateStringency, "D5S818" -> ModerateStringency, "TPOX" -> ModerateStringency, "D21S11" -> ModerateStringency),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI), 1)

      val search = MatchGroupSearch("user", true, SampleCode("AR-C-SHDG-500"), MatchKind.MixMix, 1, 1, "", true)
      val interconnectionService = mock[InterconnectionService]
      val matchingRepository = mock[MongoMatchingRepository]
      when(matchingRepository.getMatchesByGroup(any[MatchGroupSearch])).thenReturn(mockResult)
      when(matchingRepository.getByMatchingProfileId(any[String],anyObject(), anyObject())).thenReturn(Future.successful(Some(matchResult)))

      when(interconnectionService.isInterconnectionMatch(any[MatchResult])).thenReturn(false)
      val matchingService = new MatchingServiceSparkImpl(null, matchingRepository, null, null, null, null, null, null, null, null, null, interconnectionService, null, null)

      val result = Await.result(matchingService.getMatchesByGroup(search), duration)

      result mustBe mockResult
    }
  }
}
