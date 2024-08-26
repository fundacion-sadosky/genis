package probability

import java.util.Date

import kits.{AnalysisTypeService, LocusService}
import matching.Stringency._
import matching._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.specs2.mutable.Specification
import play.api.mvc.Results
import profile.{Allele, Profile, ProfileRepository, ProfileService}
import stubs.Stubs
import types.{AlphanumericId, MongoDate, MongoId, SampleCode}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import profile.GenotypificationByType._
import profile.GenotypificationByType._

class CalculationTypeServiceTest extends Specification with MockitoSugar with Results {

  val duration = Duration(100, SECONDS)
  val locusAutosomal = Seq("LOCUS 1", "LOCUS 2", "LOCUS 3", "LOCUS 20")
  val locusIntersect = Set("LOCUS 1", "LOCUS 2", "LOCUS 3")

  val profile = Profile(null, SampleCode("AR-C-SHDG-1110"), null, null, null, Map(
    1 -> Map(
        "LOCUS 1" -> List(Allele(13)),
        "LOCUS 2" -> List(Allele(14), Allele(20)),
        "LOCUS 3" -> List(Allele(9), Allele(10))),
    2 -> Map(
        "LOCUS 4" -> List(Allele(6), Allele(15)),
        "LOCUS 5" -> List(Allele(9)))
  ), None, None, None, None)

  val calculationTypes = Map(
    "lrmix" -> "Autosomal",
    "mixmix" -> "Autosomal"
  )

  "A CalculationTypeService" should {
    "filter a profile's genotypification according to calculation type" in {
      val mockAnalysisTypeService = mock[AnalysisTypeService]
      when(mockAnalysisTypeService.getByName(any[String])).thenReturn(Future.successful(Stubs.analysisTypes.headOption))

      val service = new CalculationTypeServiceImpl(null, mockAnalysisTypeService, calculationTypes)

      val result = Await.result(service.filterProfile(profile, "lrmix"), duration)

      result.keySet must beEqualTo(locusIntersect)
    }

    "filter many profiles' genotypifications according to calculation type" in {
      val mockAnalysisTypeService = mock[AnalysisTypeService]
      when(mockAnalysisTypeService.getByName(any[String])).thenReturn(Future.successful(Stubs.analysisTypes.headOption))

      val service = new CalculationTypeServiceImpl(null, mockAnalysisTypeService, calculationTypes)

      val result = Await.result(service.filterProfiles(Seq(profile,profile), "lrmix"), duration)

      result(0).keySet must beEqualTo(locusIntersect)
      result(1).keySet must beEqualTo(locusIntersect)
    }

    "filter many profiles' genotypifications from their sample codes" in {
      val mockAnalysisTypeService = mock[AnalysisTypeService]
      when(mockAnalysisTypeService.getByName(any[String])).thenReturn(Future.successful(Stubs.analysisTypes.headOption))

      val mockProfileRepository = mock[ProfileRepository]
      when(mockProfileRepository.findByCodes(any[List[SampleCode]])).thenReturn(Future.successful(Seq(profile, profile)))

      val service = new CalculationTypeServiceImpl(mockProfileRepository, mockAnalysisTypeService, calculationTypes)

      val result = Await.result(service.filterCodes(List(profile.globalCode,profile.globalCode), "lrmix"), duration)

      result(0).keySet must beEqualTo(locusIntersect)
      result(1).keySet must beEqualTo(locusIntersect)
    }

    "filter match results by analysis type" in {
      val mockAnalysisTypeService = mock[AnalysisTypeService]
      when(mockAnalysisTypeService.getByName(any[String])).thenReturn(Future.successful(Stubs.analysisTypes.headOption))

      val service = new CalculationTypeServiceImpl(null, mockAnalysisTypeService, calculationTypes)

      val mrs = Seq(
        MatchResult(MongoId("54eb50cc2cdc8a94c6ee794a"), MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-HIBA-500"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-B-IMBICE-500"),"tst-genetist",MatchStatus.pending,None),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI), 1),

        MatchResult(MongoId("54eb50cc2cdc8a94c6ee794b"), MongoDate(new Date()), 2,
        MatchingProfile(SampleCode("AR-C-HIBA-500"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-B-IMBICE-500"),"tst-genetist",MatchStatus.pending,None),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI), 1))

      val result = Await.result(service.filterMatchResults(mrs, "lrmix"), duration)

      result.size must beEqualTo(1)
    }

  }

}
