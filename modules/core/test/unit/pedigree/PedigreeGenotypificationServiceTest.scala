package unit.pedigree

import fixtures.PedigreeFixtures._
import kits.AnalysisType
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import pedigree.*
import probability.CalculationTypeService
import profile.ProfileRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class PedigreeGenotypificationServiceTest extends AnyWordSpec with Matchers with MockitoSugar {

  val duration: Duration = 10.seconds
  def await[T](f: Future[T]): T = Await.result(f, duration)

  private val analysisType = AnalysisType(1, "BayesianNetwork", mitochondrial = false)
  private val emptyFreqTable: BayesianNetwork.FrequencyTable = Map.empty
  private val emptyLinkage: BayesianNetwork.Linkage = Map.empty

  def buildService(
    pedigreeRepository: PedigreeRepository = mock[PedigreeRepository],
    profileRepository: ProfileRepository = mock[ProfileRepository],
    calculationTypeService: CalculationTypeService = mock[CalculationTypeService],
    bayesianNetworkService: BayesianNetworkService = mock[BayesianNetworkService],
    pedigreeGenotypificationRepository: PedigreeGenotypificationRepository = mock[PedigreeGenotypificationRepository],
    pedigreeMatcher: PedigreeMatcher = mock[PedigreeMatcher],
    mutationService: MutationService = mock[MutationService],
    mutationRepository: MutationRepository = mock[MutationRepository],
    pedigreeDataRepository: PedigreeDataRepository = mock[PedigreeDataRepository]
  ): PedigreeGenotypificationServiceImpl =
    new PedigreeGenotypificationServiceImpl(
      profileRepository, calculationTypeService, bayesianNetworkService,
      pedigreeGenotypificationRepository, pedigreeMatcher, pedigreeRepository,
      mutationService, mutationRepository, pedigreeDataRepository
    )

  // ─── generateGenotypificationAndFindMatches ───────────────────────────────

  "PedigreeGenotypificationServiceImpl.generateGenotypificationAndFindMatches" must {

    "happy path: persiste genotypification y dispara findMatchesInBackGround → Right(pedigreeId)" in {
      val ped = pedigreeGenogram().copy(frequencyTable = Some("ARGENTINA"))
      val pedigreeRepo = mock[PedigreeRepository]
      when(pedigreeRepo.get(pedigreeId)).thenReturn(Future.successful(Some(ped)))

      val profileRepo = mock[ProfileRepository]
      when(profileRepo.findByCodes(any())).thenReturn(Future.successful(Seq.empty))
      when(profileRepo.getProfilesMarkers(any())).thenReturn(List.empty)

      val calcSvc = mock[CalculationTypeService]
      when(calcSvc.getAnalysisTypeByCalculation(any())).thenReturn(Future.successful(analysisType))

      val bnSvc = mock[BayesianNetworkService]
      when(bnSvc.getFrequencyTable(eqTo("ARGENTINA"))).thenReturn(Future.successful(("ARGENTINA", emptyFreqTable)))
      when(bnSvc.getLinkage()).thenReturn(Future.successful(emptyLinkage))
      when(bnSvc.getGenotypification(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Future.successful(Array.empty[PlainCPT]))

      val genoRepo = mock[PedigreeGenotypificationRepository]
      when(genoRepo.upsertGenotypification(any())).thenReturn(Future.successful(Right(pedigreeId)))

      val mutSvc = mock[MutationService]
      when(mutSvc.generateN(any(), any())).thenReturn(Future.successful(Right(())))
      when(mutSvc.getMutationModelData(any(), any())).thenReturn(Future.successful(None))
      when(mutSvc.getAllPossibleAllelesByLocus()).thenReturn(Future.successful(Map.empty))

      val matcher = mock[PedigreeMatcher]

      val svc = buildService(
        pedigreeRepository = pedigreeRepo,
        profileRepository = profileRepo,
        calculationTypeService = calcSvc,
        bayesianNetworkService = bnSvc,
        pedigreeGenotypificationRepository = genoRepo,
        pedigreeMatcher = matcher,
        mutationService = mutSvc
      )

      val result = await(svc.generateGenotypificationAndFindMatches(pedigreeId))
      result mustBe Right(pedigreeId)
      // foreach es side-effect — esperá brevemente a que se dispare
      Thread.sleep(100)
      verify(matcher).findMatchesInBackGround(pedigreeId)
    }

    "frequencyTable=None: el RuntimeException(\"error.E0201\") es atrapado por recover de Q8 → Left(\"error.E0630\")" in {
      val pedSinFreq = pedigreeGenogram().copy(frequencyTable = None)
      val pedigreeRepo = mock[PedigreeRepository]
      when(pedigreeRepo.get(pedigreeId)).thenReturn(Future.successful(Some(pedSinFreq)))
      val matcher = mock[PedigreeMatcher]
      val svc = buildService(pedigreeRepository = pedigreeRepo, pedigreeMatcher = matcher)

      val result = await(svc.generateGenotypificationAndFindMatches(pedigreeId))
      result mustBe Left("error.E0630")
      Thread.sleep(50)
      verify(matcher, never()).findMatchesInBackGround(any())
    }

    "saveGenotypification falla (upsertGenotypification → Left): retorna Left y NO dispara findMatchesInBackGround" in {
      val ped = pedigreeGenogram().copy(frequencyTable = Some("ARGENTINA"))
      val pedigreeRepo = mock[PedigreeRepository]
      when(pedigreeRepo.get(pedigreeId)).thenReturn(Future.successful(Some(ped)))
      // postgres rollback dentro de recoverWith: changeStatus + getCourtCase
      when(pedigreeRepo.changeStatus(any(), any())).thenReturn(Future.successful(Right(0L)))

      val profileRepo = mock[ProfileRepository]
      when(profileRepo.findByCodes(any())).thenReturn(Future.successful(Seq.empty))
      when(profileRepo.getProfilesMarkers(any())).thenReturn(List.empty)

      val calcSvc = mock[CalculationTypeService]
      when(calcSvc.getAnalysisTypeByCalculation(any())).thenReturn(Future.successful(analysisType))

      val bnSvc = mock[BayesianNetworkService]
      when(bnSvc.getFrequencyTable(any())).thenReturn(Future.successful(("ARGENTINA", emptyFreqTable)))
      when(bnSvc.getLinkage()).thenReturn(Future.successful(emptyLinkage))
      when(bnSvc.getGenotypification(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Future.successful(Array.empty[PlainCPT]))

      val genoRepo = mock[PedigreeGenotypificationRepository]
      // El upsert falla con una excepción → cae en el recoverWith de saveGenotypification
      when(genoRepo.upsertGenotypification(any()))
        .thenReturn(Future.failed(new RuntimeException("mongo write failed")))

      val mutSvc = mock[MutationService]
      when(mutSvc.generateN(any(), any())).thenReturn(Future.successful(Right(())))
      when(mutSvc.getMutationModelData(any(), any())).thenReturn(Future.successful(None))
      when(mutSvc.getAllPossibleAllelesByLocus()).thenReturn(Future.successful(Map.empty))

      val pedDataRepo = mock[PedigreeDataRepository]
      when(pedDataRepo.getCourtCase(any())).thenReturn(Future.successful(Some(courtCase())))

      val matcher = mock[PedigreeMatcher]

      val svc = buildService(
        pedigreeRepository = pedigreeRepo,
        profileRepository = profileRepo,
        calculationTypeService = calcSvc,
        bayesianNetworkService = bnSvc,
        pedigreeGenotypificationRepository = genoRepo,
        pedigreeMatcher = matcher,
        mutationService = mutSvc,
        pedigreeDataRepository = pedDataRepo
      )

      val result = await(svc.generateGenotypificationAndFindMatches(pedigreeId))
      result.isLeft mustBe true
      Thread.sleep(100)
      verify(matcher, never()).findMatchesInBackGround(any())
    }
  }
}
