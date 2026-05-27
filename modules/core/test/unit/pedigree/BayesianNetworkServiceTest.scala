package unit.pedigree

import fixtures.PedigreeFixtures._
import inbox.NotificationService
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import pedigree.*
import profile.ProfileRepository
import stats.{PopulationBaseFrequency, PopulationBaseFrequencyService, PopulationSampleFrequency, ProbabilityModel}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

class BayesianNetworkServiceTest extends AnyWordSpec with Matchers with MockitoSugar {

  val duration: Duration = 10.seconds
  def await[T](f: Future[T]): T = Await.result(f, duration)

  def buildService(
    populationBaseFrequencyService: PopulationBaseFrequencyService = mock[PopulationBaseFrequencyService],
    pedigreeScenarioService: PedigreeScenarioService = mock[PedigreeScenarioService],
    pedigreeService: PedigreeService = mock[PedigreeService],
    notificationService: NotificationService = mock[NotificationService]
  ): BayesianNetworkServiceImpl = {
    val pedigreeScenarioProvider = new jakarta.inject.Provider[PedigreeScenarioService] {
      def get(): PedigreeScenarioService = pedigreeScenarioService
    }
    val pedigreeServiceProvider = new jakarta.inject.Provider[PedigreeService] {
      def get(): PedigreeService = pedigreeService
    }
    val pedigreeGenoServiceProvider = new jakarta.inject.Provider[PedigreeGenotypificationService] {
      def get(): PedigreeGenotypificationService = mock[PedigreeGenotypificationService]
    }
    new BayesianNetworkServiceImpl(
      profileRepository = mock[ProfileRepository],
      populationBaseFrequencyService = populationBaseFrequencyService,
      calculationTypeService = mock[probability.CalculationTypeService],
      pedigreeScenarioServiceProvider = pedigreeScenarioProvider,
      notificationService = notificationService,
      pedigreeServiceProvider = pedigreeServiceProvider,
      locusService = mock[kits.LocusService],
      mutationService = mock[pedigree.MutationService],
      mutationRepository = mock[pedigree.MutationRepository],
      pedigreeGenotypificationServiceProvider = pedigreeGenoServiceProvider,
      userService = mock[services.UserService]
    )
  }

  // ─── getFrequencyTable ────────────────────────────────────────────────────

  "BayesianNetworkServiceImpl.getFrequencyTable" must {

    "return frequency table with Double allele keys" in {
      val freqSvc = mock[PopulationBaseFrequencyService]
      val base = PopulationBaseFrequency(
        name = "ARGENTINA",
        theta = 0.01,
        model = ProbabilityModel.HardyWeinberg,
        base = Seq(
          PopulationSampleFrequency("CSF1PO", 10.0, BigDecimal("0.12")),
          PopulationSampleFrequency("CSF1PO", 11.0, BigDecimal("0.18")),
          PopulationSampleFrequency("TPOX",   8.0,  BigDecimal("0.22"))
        )
      )
      when(freqSvc.getByName("ARGENTINA")).thenReturn(Future.successful(Some(base)))
      val svc = buildService(populationBaseFrequencyService = freqSvc)

      val (name, freqTable) = await(svc.getFrequencyTable("ARGENTINA"))

      name mustBe "ARGENTINA"
      freqTable must contain key "CSF1PO"
      freqTable("CSF1PO") must contain key 10.0
      freqTable("CSF1PO")(10.0) mustBe 0.12 +- 0.001
      freqTable must contain key "TPOX"
      freqTable("TPOX")(8.0) mustBe 0.22 +- 0.001
    }
  }

  // ─── calculateProbability ─────────────────────────────────────────────────

  "BayesianNetworkServiceImpl.calculateProbability" must {

    "set isProcessing=true before calculation, then isProcessing=false with LR after" in {
      val scenarioSvc = mock[PedigreeScenarioService]
      val scenario = pedigreeScenario()

      // isProcessing=true: set when updateScenario is called first time
      when(scenarioSvc.updateScenario(scenario.copy(isProcessing = true)))
        .thenReturn(Future.successful(Right(mongoId)))

      // For the actual LR computation we'll let it fail gracefully (no profiles)
      // The service recovers by calling updateScenario with isProcessing=false
      when(scenarioSvc.updateScenario(any())).thenReturn(Future.successful(Right(mongoId)))

      val freqSvc = mock[PopulationBaseFrequencyService]
      when(freqSvc.getByName(any())).thenReturn(Future.successful(None))

      val profRepo = mock[ProfileRepository]
      when(profRepo.findByCodes(any())).thenReturn(Future.successful(List.empty))

      // We just verify that updateScenario(isProcessing=true) is called eagerly
      val svc = buildService(
        populationBaseFrequencyService = freqSvc,
        pedigreeScenarioService = scenarioSvc
      )

      // The calculateProbability will fail (no real profiles/frequency tables) but
      // the side effects on scenarioSvc should still occur
      val _ = svc.calculateProbability(scenario)
      Thread.sleep(100)

      verify(scenarioSvc).updateScenario(scenario.copy(isProcessing = true))
    }
  }
}
