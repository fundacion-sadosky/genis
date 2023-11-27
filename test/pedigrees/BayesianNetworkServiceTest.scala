package pedigrees

import java.util.Date

import inbox.{NotificationService, PedigreeLRInfo}
import kits.LocusService
import org.bson.types.ObjectId
import org.mockito.Matchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.mock.MockitoSugar
import pedigree.{CourtCaseFull, MutationRepository, _}
import play.api.libs.concurrent.Akka
import probability.{CalculationTypeService, ProbabilityModel}
import profile.{Allele, Profile, ProfileRepository}
import scenarios.ScenarioStatus
import specs.PdgSpec
import stats.{PopulationBaseFrequency, PopulationBaseFrequencyService, PopulationSampleFrequency}
import stubs.Stubs
import types.{MongoId, SampleCode, Sex}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


class BayesianNetworkServiceTest extends PdgSpec with MockitoSugar {

  val duration = Duration(100, MINUTES)

  val seqPopulation: Seq[PopulationSampleFrequency] = List(
    PopulationSampleFrequency("LOCUS", 10, BigDecimal("0.26929")),
    PopulationSampleFrequency("LOCUS", 6, BigDecimal("0.00006")),
    PopulationSampleFrequency("LOCUS", 7, BigDecimal("0.00167")),
    PopulationSampleFrequency("LOCUS", 8, BigDecimal("0.00448")),
    PopulationSampleFrequency("LOCUS", 9, BigDecimal("0.02185")),
    PopulationSampleFrequency("LOCUS", -1, BigDecimal("0.000005")))
  val populationBaseFrequency = new PopulationBaseFrequency("pop freq 1", 0, ProbabilityModel.HardyWeinberg, seqPopulation)
  val mutationService = mock[MutationService]
  val locusService = mock[LocusService]
  val mutationRepository = mock[MutationRepository]
  val pedigreeGenotypificationService = new PedigreeGenotypificationServiceImpl(
    Akka.system,
    null,
    null,
    null,null,
    null,
    null,
    null,
    null,
    null
  )
  val possibleLocus : Map[String,List[Double]] = Map.empty
  when(mutationService.getAllPossibleAllelesByLocus()).thenReturn(Future.successful(possibleLocus))
  when(mutationService.getMutationModelData(any[Option[MutationModel]],any[List[String]])).thenReturn(Future.successful(None))
  when(locusService.list()).thenReturn(Future.successful(Nil))
  "BayesianNetworkService" must {
    "calculate probability" in {
      val populationBaseFrequencyService = mock[PopulationBaseFrequencyService]
      when(populationBaseFrequencyService.getByName("pop freq 1")).thenReturn(Future.successful(Some(populationBaseFrequency)))

      val calculationTypeService = mock[CalculationTypeService]
      when(calculationTypeService.getAnalysisTypeByCalculation("pedigree")).thenReturn(Future.successful(Stubs.analysisTypes.head))

      val father = Profile(null, SampleCode("AR-C-SHDG-1121"), null, null, null, Map(
        1 -> Map("LOCUS" -> List(Allele(8), Allele(9)))
      ), None, None, None, None)

      val unknown = Profile(null, SampleCode("AR-C-SHDG-1150"), null, null, null, Map(
        1 -> Map("LOCUS" -> List(Allele(8), Allele(10)))
      ), None, None, None, None)

      val profiles = Seq(father, unknown)

      val profileRepository = mock[ProfileRepository]
      when(profileRepository.findByCodes(any[List[SampleCode]])).thenReturn(Future.successful(profiles))

      val courtCase = CourtCaseFull(123l, "internalSampleCode", None, None, "assignee", None, None, None,
        PedigreeStatus.Active, List(PersonData(None, None, None, None, None, None, None, None, None, None, None, None, None, None,"Jose",None)) , "MPI")

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.getCourtCase(123l, "", true)).thenReturn(Future.successful(Some(courtCase)))

      val individuals = Array(
        Individual(NodeAlias("Padre"), None, None, Sex.Male, Some(father.globalCode), false,None),
        Individual(NodeAlias("Madre"), None, None, Sex.Female, None, false,None),
        Individual(NodeAlias("PI"), Some(NodeAlias("Padre")), Some(NodeAlias("Madre")), Sex.Male, Some(unknown.globalCode), true, None)
      )

      val scenario = PedigreeScenario(MongoId(new ObjectId().toString), 123l, "Scenario",
        "Description", individuals, ScenarioStatus.Pending, "pop freq 1", false, None)

      val pedigreeScenarioService = mock[PedigreeScenarioService]
      when(pedigreeScenarioService.updateScenario(any[PedigreeScenario])).thenReturn(Future.successful(Right(scenario._id)))

      val service = new BayesianNetworkServiceImpl(Akka.system, profileRepository, populationBaseFrequencyService, calculationTypeService, pedigreeScenarioService, Stubs.notificationServiceMock, pedigreeService,locusService,mutationService,mutationRepository,pedigreeGenotypificationService)

      val lr = Await.result(service.calculateProbability(scenario), duration)

      lr mustBe >(0.0)
    }

    "send a notification after calculating probability" in {

      val populationBaseFrequencyService = mock[PopulationBaseFrequencyService]
      when(populationBaseFrequencyService.getByName("pop freq 1")).thenReturn(Future.successful(Some(populationBaseFrequency)))

      val calculationTypeService = mock[CalculationTypeService]
      when(calculationTypeService.getAnalysisTypeByCalculation("pedigree")).thenReturn(Future.successful(Stubs.analysisTypes.head))

      val father = Profile(null, SampleCode("AR-C-SHDG-1121"), null, null, null, Map(
        1 -> Map("LOCUS" -> List(Allele(8), Allele(9)))
      ), None, None, None, None)

      val unknown = Profile(null, SampleCode("AR-C-SHDG-1150"), null, null, null, Map(
        1 -> Map("LOCUS" -> List(Allele(7), Allele(10)))
      ), None, None, None, None)

      val profiles = Seq(father, unknown)

      val profileRepository = mock[ProfileRepository]
      when(profileRepository.findByCodes(any[List[SampleCode]])).thenReturn(Future.successful(profiles))

      val courtCase = CourtCaseFull(123l, "internalSampleCode", None, None, "assignee", None, None, None,
        PedigreeStatus.Active, List(PersonData(None, None, None, None, None, None, None, None, None, None, None, None, None, None,"Maria",None)), "MPI")

      val pedigree = PedigreeDataCreation(PedigreeMetaData(123, 123, "Pedigree", new Date(), PedigreeStatus.Active, "assignee"), None)

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.getPedigree(123l)).thenReturn(Future.successful(Some(pedigree)))

      val individuals = Array(
        Individual(NodeAlias("Padre"), None, None, Sex.Male, Some(father.globalCode), false, None),
        Individual(NodeAlias("Madre"), None, None, Sex.Female, None, false, None),
        Individual(NodeAlias("PI"), Some(NodeAlias("Padre")), Some(NodeAlias("Madre")), Sex.Male, Some(unknown.globalCode), true, None)
      )

      val scenario = PedigreeScenario(MongoId(new ObjectId().toString), 123l, "Scenario",
        "Description", individuals, ScenarioStatus.Pending, "pop freq 1", false, None)

      val notificationService = mock[NotificationService]

      val pedigreeScenarioService = mock[PedigreeScenarioService]
      when(pedigreeScenarioService.updateScenario(any[PedigreeScenario])).thenReturn(Future.successful(Right(scenario._id)))

      val service = new BayesianNetworkServiceImpl(Akka.system, profileRepository, populationBaseFrequencyService, calculationTypeService, pedigreeScenarioService, notificationService, pedigreeService,locusService,mutationService,mutationRepository,pedigreeGenotypificationService)

      Await.result(service.calculateProbability(scenario), duration)

      //Thread sleep porque hay un future on success
      Thread.sleep(1000)

      verify(notificationService).push("assignee", PedigreeLRInfo(123l, 123l, "Scenario"))
    }

    "mark scenario as processing when calculating probability" in {
      val populationBaseFrequencyService = mock[PopulationBaseFrequencyService]
      when(populationBaseFrequencyService.getByName("pop freq 1")).thenReturn(Future.successful(Some(populationBaseFrequency)))

      val calculationTypeService = mock[CalculationTypeService]
      when(calculationTypeService.getAnalysisTypeByCalculation("pedigree")).thenReturn(Future.successful(Stubs.analysisTypes.head))

      val father = Profile(null, SampleCode("AR-C-SHDG-1121"), null, null, null, Map(
        1 -> Map("LOCUS" -> List(Allele(8), Allele(9)))
      ), None, None, None, None)

      val unknown = Profile(null, SampleCode("AR-C-SHDG-1150"), null, null, null, Map(
        1 -> Map("LOCUS" -> List(Allele(7), Allele(10)))
      ), None, None, None, None)

      val profiles = Seq(father, unknown)

      val profileRepository = mock[ProfileRepository]
      when(profileRepository.findByCodes(any[List[SampleCode]])).thenReturn(Future.successful(profiles))

      val courtCase = CourtCaseFull(123l, "internalSampleCode", None, None, "assignee", None, None, None,
        PedigreeStatus.Active, List(PersonData(None, None, None, None, None, None, None, None, None, None, None, None, None, None,"juan",None)), "MPI")

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.getCourtCase(123l, "", true)).thenReturn(Future.successful(Some(courtCase)))

      val individuals = Array(
        Individual(NodeAlias("Padre"), None, None, Sex.Male, Some(father.globalCode), false,None),
        Individual(NodeAlias("Madre"), None, None, Sex.Female, None, false,None),
        Individual(NodeAlias("PI"), Some(NodeAlias("Padre")), Some(NodeAlias("Madre")), Sex.Male, Some(unknown.globalCode), true, None)
      )

      val scenario = PedigreeScenario(MongoId(new ObjectId().toString), 123l, "Scenario",
        "Description", individuals, ScenarioStatus.Pending, "pop freq 1", false, None)

      val notificationService = mock[NotificationService]

      val pedigreeScenarioService = mock[PedigreeScenarioService]
      when(pedigreeScenarioService.updateScenario(any[PedigreeScenario])).thenReturn(Future.successful(Right(scenario._id)))

      val service = new BayesianNetworkServiceImpl(Akka.system, profileRepository, populationBaseFrequencyService, calculationTypeService, pedigreeScenarioService, notificationService, pedigreeService,locusService,mutationService,mutationRepository,pedigreeGenotypificationService)

      val lr = Await.result(service.calculateProbability(scenario), duration)

      //Thread sleep porque hay un future on success
      Thread.sleep(1000)

      val processingScenario = PedigreeScenario(scenario._id, scenario.pedigreeId, scenario.name, scenario.description,
        scenario.genogram, scenario.status, scenario.frequencyTable, true, None)
      verify(pedigreeScenarioService).updateScenario(processingScenario)

      val doneProcessingScenario = PedigreeScenario(scenario._id, scenario.pedigreeId, scenario.name, scenario.description,
        scenario.genogram, scenario.status, scenario.frequencyTable, false, Some(lr.toString))
      verify(pedigreeScenarioService).updateScenario(doneProcessingScenario)

    }
/* no funciona
    "unmark scenario's processing bool when calculate probability fails" in {
      val populationBaseFrequencyService = mock[PopulationBaseFrequencyService]
      when(populationBaseFrequencyService.getByName("pop freq 1")).thenReturn(Future.successful(Some(populationBaseFrequency)))

      val calculationTypeService = mock[CalculationTypeService]
      when(calculationTypeService.getAnalysisTypeByCalculation("pedigree")).thenReturn(Future.successful(Stubs.analysisTypes.head))

      val profileRepository = mock[ProfileRepository]
      when(profileRepository.findByCodes(any[List[SampleCode]])).thenReturn(Future.failed(throw new Exception()))

      val courtCase = CourtCaseFull(123l, "internalSampleCode", None, None, "assignee", None, None, None,
        PedigreeStatus.Active, PersonData(None, None, None, None, None, None, None, None, None, None, None))

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.getCourtCase(123l, "", true)).thenReturn(Future.successful(Some(courtCase)))

      val individuals = Array(
        Individual(NodeAlias("Padre"), None, None, Sex.Male, None, false),
        Individual(NodeAlias("Madre"), None, None, Sex.Female, None, false),
        Individual(NodeAlias("PI"), Some(NodeAlias("Padre")), Some(NodeAlias("Madre")), Sex.Male, None, true)
      )

      val scenario = PedigreeScenario(MongoId(new ObjectId().toString), 123l, "Scenario",
        "Description", individuals, ScenarioStatus.Pending, "pop freq 1", false, None)

      val notificationService = mock[NotificationService]

      val pedigreeScenarioService = mock[PedigreeScenarioService]
      when(pedigreeScenarioService.updateScenario(any[PedigreeScenario])).thenReturn(Future.successful(Right(scenario._id)))

      val service = new BayesianNetworkServiceImpl(Akka.system, profileRepository, populationBaseFrequencyService, calculationTypeService, pedigreeScenarioService, notificationService, pedigreeService,locusService,mutationService,mutationRepository)

      Await.result(service.calculateProbability(scenario), duration)

      //Thread sleep porque hay un future on success
      Thread.sleep(1000)

      val processingScenario = PedigreeScenario(scenario._id, scenario.pedigreeId, scenario.name, scenario.description,
        scenario.genogram, scenario.status, scenario.frequencyTable, true, None)
      verify(pedigreeScenarioService).updateScenario(processingScenario)

      val doneProcessingScenario = PedigreeScenario(scenario._id, scenario.pedigreeId, scenario.name, scenario.description,
        scenario.genogram, scenario.status, scenario.frequencyTable, false, None)
      verify(pedigreeScenarioService).updateScenario(doneProcessingScenario)

    }*/

  }
}
