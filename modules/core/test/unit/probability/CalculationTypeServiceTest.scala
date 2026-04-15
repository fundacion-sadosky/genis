package unit.probability

import java.util.Date
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import kits.{AnalysisType, AnalysisTypeService}
import matching.Stringency.*
import matching.*
import probability.CalculationTypeServiceImpl
import profile.{Allele, Profile, ProfileRepository}
import types.{AlphanumericId, MongoDate, SampleCode}

class CalculationTypeServiceTest extends AnyWordSpec with Matchers with MockitoSugar {

  given ec: ExecutionContext = ExecutionContext.global
  val timeout: Duration = Duration(10, "seconds")

  val locusAutosomal = Seq("LOCUS 1", "LOCUS 2", "LOCUS 3", "LOCUS 20")
  val locusIntersect = Set("LOCUS 1", "LOCUS 2", "LOCUS 3")

  val profile = Profile(
    _id = SampleCode("AR-C-SHDG-1110"),
    globalCode = SampleCode("AR-C-SHDG-1110"),
    internalSampleCode = null,
    assignee = null,
    categoryId = null,
    genotypification = Map(
      1 -> Map(
        "LOCUS 1" -> List(Allele(13)),
        "LOCUS 2" -> List(Allele(14), Allele(20)),
        "LOCUS 3" -> List(Allele(9), Allele(10))
      ),
      2 -> Map(
        "LOCUS 4" -> List(Allele(6), Allele(15)),
        "LOCUS 5" -> List(Allele(9))
      )
    ),
    analyses = None,
    labeledGenotypification = None,
    contributors = None,
    mismatches = None
  )

  val calculationTypes = Map(
    "lrmix"  -> "Autosomal",
    "mixmix" -> "Autosomal"
  )

  val autosomalType = AnalysisType(1, "Autosomal")

  "CalculationTypeServiceImpl" must {

    "filter a profile genotypification according to the calculation type" in {
      val mockAnalysisTypeService = mock[AnalysisTypeService]
      when(mockAnalysisTypeService.getByName(any[String])).thenReturn(Future.successful(Some(autosomalType)))

      val service = new CalculationTypeServiceImpl(null, mockAnalysisTypeService, calculationTypes)

      val result = Await.result(service.filterProfile(profile, "lrmix"), timeout)

      result.keySet mustBe locusIntersect
    }

    "filter multiple profiles genotypifications according to calculation type" in {
      val mockAnalysisTypeService = mock[AnalysisTypeService]
      when(mockAnalysisTypeService.getByName(any[String])).thenReturn(Future.successful(Some(autosomalType)))

      val service = new CalculationTypeServiceImpl(null, mockAnalysisTypeService, calculationTypes)

      val result = Await.result(service.filterProfiles(Seq(profile, profile), "lrmix"), timeout)

      result(0).keySet mustBe locusIntersect
      result(1).keySet mustBe locusIntersect
    }

    "filter profiles from sample codes by looking up ProfileRepository" in {
      val mockAnalysisTypeService = mock[AnalysisTypeService]
      when(mockAnalysisTypeService.getByName(any[String])).thenReturn(Future.successful(Some(autosomalType)))

      val mockProfileRepository = mock[ProfileRepository]
      when(mockProfileRepository.findByCodes(any[List[SampleCode]])).thenReturn(Future.successful(Seq(profile, profile)))

      val service = new CalculationTypeServiceImpl(mockProfileRepository, mockAnalysisTypeService, calculationTypes)

      val result = Await.result(service.filterCodes(List(profile.globalCode, profile.globalCode), "lrmix"), timeout)

      result(0).keySet mustBe locusIntersect
      result(1).keySet mustBe locusIntersect
    }

    "use provided profile list when it satisfies all requested codes (skip repository)" in {
      val mockAnalysisTypeService = mock[AnalysisTypeService]
      when(mockAnalysisTypeService.getByName(any[String])).thenReturn(Future.successful(Some(autosomalType)))

      // repository must NOT be called — passing null would blow up if called
      val service = new CalculationTypeServiceImpl(null, mockAnalysisTypeService, calculationTypes)

      val result = Await.result(service.filterCodes(List(profile.globalCode), "lrmix", List(profile)), timeout)
      result(0).keySet mustBe locusIntersect
    }

    "filter match results keeping only those matching the analysis type id" in {
      val mockAnalysisTypeService = mock[AnalysisTypeService]
      when(mockAnalysisTypeService.getByName(any[String])).thenReturn(Future.successful(Some(autosomalType)))

      val service = new CalculationTypeServiceImpl(null, mockAnalysisTypeService, calculationTypes)

      val mrs = Seq(
        MatchResult(
          MongoId("54eb50cc2cdc8a94c6ee794a"), MongoDate(new Date()), 1,
          MatchingProfile(SampleCode("AR-C-HIBA-500"), "user1", MatchStatus.pending, None),
          MatchingProfile(SampleCode("AR-B-IMBICE-500"), "user2", MatchStatus.pending, None),
          NewMatchingResult(ModerateStringency, Map(), 14, AlphanumericId("SOSPECHOSO"), 0.7, 1.0, Algorithm.ENFSI)
        ),
        MatchResult(
          MongoId("54eb50cc2cdc8a94c6ee794b"), MongoDate(new Date()), 2,
          MatchingProfile(SampleCode("AR-C-HIBA-500"), "user1", MatchStatus.pending, None),
          MatchingProfile(SampleCode("AR-B-IMBICE-500"), "user2", MatchStatus.pending, None),
          NewMatchingResult(ModerateStringency, Map(), 14, AlphanumericId("SOSPECHOSO"), 0.7, 1.0, Algorithm.ENFSI)
        )
      )

      val result = Await.result(service.filterMatchResults(mrs, "lrmix"), timeout)

      result.size mustBe 1
    }
  }
}
