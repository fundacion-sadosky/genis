package pedigrees

import kits.FullLocus
import matching.MatchingService
import org.scalatest.mock.MockitoSugar
import pedigree._
import specs.PdgSpec
import org.mockito.Matchers.any
import org.mockito.Mockito._
import play.api.libs.concurrent.Akka
import probability.CalculationTypeService
import profile.{Allele, Profile, ProfileRepository}
import stubs.Stubs
import types.{SampleCode, Sex}

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, _}

class PedigreeGenotypificationServiceTest extends PdgSpec with MockitoSugar {

  val possibleLocus : Map[String,List[Double]] = Map.empty

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

  "PedigreeGenotypificationService" must {

    "create genotypification and find matches - ok" in {
      val mutationService = mock[MutationService]
      when(mutationService.getAllPossibleAllelesByLocus()).thenReturn(Future.successful(possibleLocus))
      when(mutationService.getMutationModelData(any[Option[MutationModel]],any[List[String]])).thenReturn(Future.successful(None))
      when(mutationService.generateN(profiles.toArray,None)).thenReturn(Future.successful(Right(())))

      val genotypification = Array(PlainCPT(Array("Probability"), Array.empty.iterator, 0))
      val pedigreeGenogram = PedigreeGenogram(76, "user", Seq(), PedigreeStatus.Active, Some("test"), false, 0.8,false,None,"MPI", None,7l)

      val pedigreeRepository = mock[PedigreeRepository]
      when(pedigreeRepository.get(76)).thenReturn(Future.successful(Some(pedigreeGenogram)))

      val pedigreeGenotypificationRepository = mock[PedigreeGenotypificationRepository]
      when(pedigreeGenotypificationRepository.upsertGenotypification(any[PedigreeGenotypification])).thenReturn(Future.successful(Right(76l)))

      val pedigreeSparkMatcher = mock[PedigreeSparkMatcher]

      val profileRepository = mock[ProfileRepository]
      when(profileRepository.findByCodes(any[List[SampleCode]])).thenReturn(Future.successful(profiles))

      val calculationTypeService = mock[CalculationTypeService]
      when(calculationTypeService.getAnalysisTypeByCalculation(any[String])).thenReturn(Future.successful(Stubs.analysisTypes.head))

      val pedigreeDataRepository = mock[PedigreeDataRepository]

      val bayesianNetworkService = mock[BayesianNetworkService]
      when(
        bayesianNetworkService.getGenotypification(
          pedigreeGenogram, profiles.toArray, frequencyTable, Stubs.analysisTypes.head, Map.empty,None,None,Map.empty
        )
      ).thenReturn(Future.successful(genotypification))
      when(bayesianNetworkService.getFrequencyTable("test")).thenReturn(Future.successful(("test", frequencyTable)))
      when(bayesianNetworkService.getLinkage()).thenReturn(Future.successful(Map.empty[String, (String, Double)]))

      val mutationRepository = mock[MutationRepository]

      val service = new PedigreeGenotypificationServiceImpl(
        Akka.system,
        profileRepository,
        calculationTypeService,
        bayesianNetworkService,
        pedigreeGenotypificationRepository,
        pedigreeSparkMatcher,
        pedigreeRepository,
        mutationService,
        mutationRepository,
        pedigreeDataRepository
      )

      service.generateGenotypificationAndFindMatches(76)

      //Se pone porque adentro se llama a un actor de akka
      Thread.sleep(1000)

      verify(pedigreeSparkMatcher).findMatchesInBackGround(76)
    }

    "create genotypification and find matches - error updating mongo" in {
      val genotypification = Array(PlainCPT(Array("Probability"), Array.empty.iterator, 0))
      val pedigreeGenogram = PedigreeGenogram(76, "user", Seq(), PedigreeStatus.Active, Some("test"), false, 0.8,false,None,"MPI",None,7l)

      val pedigreeRepository = mock[PedigreeRepository]
      when(pedigreeRepository.get(76)).thenReturn(Future.successful(Some(pedigreeGenogram)))

      val pedigreeGenotypificationRepository = mock[PedigreeGenotypificationRepository]
      when(pedigreeGenotypificationRepository.upsertGenotypification(any[PedigreeGenotypification])).thenReturn(Future.successful(Left("Error")))

      val pedigreeSparkMatcher = mock[PedigreeSparkMatcher]

      val profileRepository = mock[ProfileRepository]
      when(profileRepository.findByCodes(any[List[SampleCode]])).thenReturn(Future.successful(profiles))

      val calculationTypeService = mock[CalculationTypeService]
      when(calculationTypeService.getAnalysisTypeByCalculation(any[String])).thenReturn(Future.successful(Stubs.analysisTypes.head))

      val bayesianNetworkService = mock[BayesianNetworkService]
      when(bayesianNetworkService.getGenotypification(pedigreeGenogram, profiles.toArray, frequencyTable, Stubs.analysisTypes.head, Map.empty)).thenReturn(Future.successful(genotypification))
      when(bayesianNetworkService.getFrequencyTable("test")).thenReturn(Future.successful(("test", frequencyTable)))
      when(bayesianNetworkService.getLinkage()).thenReturn(Future.successful(Map.empty[String, (String, Double)]))

      val pedigreeDataRepository = mock[PedigreeDataRepository]

      val mutationService = mock[MutationService]

      val mutationRepository = mock[MutationRepository]

      val service = new PedigreeGenotypificationServiceImpl(
        Akka.system,
        profileRepository,
        calculationTypeService,
        bayesianNetworkService,
        pedigreeGenotypificationRepository,
        pedigreeSparkMatcher,
        pedigreeRepository,
        mutationService,
        mutationRepository,
        pedigreeDataRepository

      )

      service.generateGenotypificationAndFindMatches(76)

      verify(pedigreeSparkMatcher, times(0)).findMatchesInBackGround(76)
    }

  }
}