package scenarios

import java.util.Date

import matching.Stringency._
import matching.{MatchStatus, NewMatchingResult, _}
import org.mockito.{AdditionalMatchers, ArgumentCaptor, Matchers}
import profile.{_}
import specs.PdgSpec
import stubs.Stubs
import types._
import org.mockito.Matchers.any
import org.mockito.AdditionalMatchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import probability.{CalculationTypeService, LRResult, ProbabilityModel}
import profile.Profile.LabeledGenotypification
import stats.{PopulationBaseFrequency, PopulationBaseFrequencyService, PopulationSampleFrequency}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class ScenarioServiceTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)

  "A Scenario Service" must {
    "calculate drop outs" in {

      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(5)), "M2" -> List(Allele(2)))), None, None, None, None, None, None, false, true)
      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2), Allele(3)), "M2" -> List(Allele(1), Allele(2)), "M3" -> List(Allele(5)))), None, None, None, None, None, None, false, true)

      val mockCalculationTypeService = mock[CalculationTypeService]
      when(mockCalculationTypeService.getAnalysisTypeByCalculation(any[String])).thenReturn(Future.successful(Stubs.analysisTypes(0)))

      val scenarioService = new ScenarioServiceImpl(null, null, null, null, mockCalculationTypeService)

      val result = Await.result(scenarioService.calculateDropOuts(p1, p2), duration)

      result mustBe 3

    }
    "calculate drop outs with no genotypes" in {

      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"), Map(), None, None, None, None, None, None, false, true)
      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"), Map(), None, None, None, None, None, None, false, true)

      val mockCalculationTypeService = mock[CalculationTypeService]
      when(mockCalculationTypeService.getAnalysisTypeByCalculation(any[String])).thenReturn(Future.successful(Stubs.analysisTypes(0)))

      val scenarioService = new ScenarioServiceImpl(null, null, null, null, mockCalculationTypeService)

      val result = Await.result(scenarioService.calculateDropOuts(p1, p2), duration)

      result mustBe 0

    }
  }

  "A Scenario Service" must {

    "find some matches for restrained scenario" in {

      val mockMatchingRepository = mock[MatchingRepository]
      val mr = Stubs.matchResult
      when(mockMatchingRepository.matchesWithFullHit(any[SampleCode])).thenReturn(Future.successful(Seq()))
      when(mockMatchingRepository.getByFiringAndMatchingProfile(any[SampleCode], any[SampleCode])).thenReturn(Future.successful(Some(mr)))

      val mockProfileService = mock[ProfileService]
      when(mockProfileService.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.newProfile)))
      when(mockProfileService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Nil)

      val mockCalculationTypeService = mock[CalculationTypeService]
      when(mockCalculationTypeService.getAnalysisTypeByCalculation(any[String])).thenReturn(Future.successful(Stubs.analysisTypes(0)))

      val scenarioService =  new ScenarioServiceImpl(mockProfileService, mockMatchingRepository, null, null, mockCalculationTypeService)

      val future = scenarioService.findMatchesForRestrainedScenario(mr.leftProfile.assignee, mr.rightProfile.globalCode, mr.leftProfile.globalCode, false)
      val result = Await.result(future, duration)

      result.size mustBe 1

    }

    "not find matches for restrained scenario" in {

      val mockMatchingRepository = mock[MatchingRepository]
      when(mockMatchingRepository.matchesWithFullHit(any[SampleCode])).thenReturn(Future.successful(Seq()))
      when(mockMatchingRepository.getByFiringAndMatchingProfile(any[SampleCode], any[SampleCode])).thenReturn(Future.successful(None))

      val mockProfileService = mock[ProfileService]
      val profile = Stubs.newProfile
      when(mockProfileService.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(profile)))

      val scenarioService = new ScenarioServiceImpl(mockProfileService, mockMatchingRepository, null, null, null)

      val future = scenarioService.findMatchesForRestrainedScenario(profile.assignee, SampleCode("AR-C-SHDG-1"), SampleCode("AR-C-SHDG-2"), false)
      val result = Await.result(future, duration)

      result.size mustBe 0
    }

    "validate matching profile assignee for restrained scenario" in {

      val mockMatchingRepository = mock[MatchingRepository]
      val mr = Stubs.matchResult
      when(mockMatchingRepository.matchesWithFullHit(any[SampleCode])).thenReturn(Future.successful(Seq()))
      when(mockMatchingRepository.getByFiringAndMatchingProfile(any[SampleCode], any[SampleCode])).thenReturn(Future.successful(Some(mr)))

      val mockProfileService = mock[ProfileService]
      when(mockProfileService.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.newProfile)))

      val scenarioService =  new ScenarioServiceImpl(mockProfileService, mockMatchingRepository, null, null, null)

      val future = scenarioService.findMatchesForRestrainedScenario("esurijon", mr.rightProfile.globalCode, mr.leftProfile.globalCode, false)
      val result = Await.result(future, duration)

      result.size mustBe 0

    }

    "add references full hits to restrained scenario" in {

      val mockMatchingRepository = mock[MatchingRepository]
      val mr = Stubs.matchResult
      val mr2 = MatchResult(MongoId("54eb50cc2cdc8a94c6ee794b"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-HIBA-501"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-B-IMBICE-500"),"tst-genetist",MatchStatus.pending,None),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI), 1)
      when(mockMatchingRepository.matchesWithFullHit(any[SampleCode])).thenReturn(Future.successful(Seq(mr)))
      when(mockMatchingRepository.getByFiringAndMatchingProfile(any[SampleCode], any[SampleCode])).thenReturn(Future.successful(Some(mr2)))

      val profile = Profile(null, SampleCode("AR-B-IMBICE-500"), null, null, null, Map(), None, None, Some(1), None, None, None, false, false, true)
      val reference = Profile(null, SampleCode("AR-C-HIBA-501"), null, null, null, Map(), None, None, Some(1), None, None, None, false, false, true)
      val reference2 = Profile(null, SampleCode("AR-C-HIBA-500"), null, null, null, Map(), None, None, Some(1), None, None, None, false, false, true)

      val mockProfileService = mock[ProfileService]
      when(mockProfileService.findByCode(SampleCode("AR-B-IMBICE-500"))).thenReturn(Future.successful(Some(profile)))
      when(mockProfileService.findByCode(SampleCode("AR-C-HIBA-501"))).thenReturn(Future.successful(Some(reference)))
      when(mockProfileService.findByCode(SampleCode("AR-C-HIBA-500"))).thenReturn(Future.successful(Some(reference2)))
      when(mockProfileService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Nil)

      val mockCalculationTypeService = mock[CalculationTypeService]
      when(mockCalculationTypeService.getAnalysisTypeByCalculation(any[String])).thenReturn(Future.successful(Stubs.analysisTypes(0)))

      val scenarioService =  new ScenarioServiceImpl(mockProfileService, mockMatchingRepository, null, null, mockCalculationTypeService)

      val future = scenarioService.findMatchesForRestrainedScenario(mr.leftProfile.assignee, mr.rightProfile.globalCode, mr.leftProfile.globalCode, false)
      val result = Await.result(future, duration)

      result.size mustBe 2

    }

    "NOT add evidences full hits to restrained scenario" in {

      val mockMatchingRepository = mock[MatchingRepository]
      val mr = Stubs.matchResult
      when(mockMatchingRepository.matchesWithFullHit(any[SampleCode])).thenReturn(Future.successful(Seq(mr)))
      when(mockMatchingRepository.getByFiringAndMatchingProfile(any[SampleCode], any[SampleCode])).thenReturn(Future.successful(Some(mr)))

      val evidence = Profile(null, SampleCode("AR-B-IMBICE-400"), null, null, null, Map(), None, None, Some(1), None, None, None, false, false, false)

      val mockProfileService = mock[ProfileService]
      when(mockProfileService.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(evidence)))
      when(mockProfileService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Nil)

      val mockCalculationTypeService = mock[CalculationTypeService]
      when(mockCalculationTypeService.getAnalysisTypeByCalculation(any[String])).thenReturn(Future.successful(Stubs.analysisTypes(0)))

      val scenarioService =  new ScenarioServiceImpl(mockProfileService, mockMatchingRepository, null, null, mockCalculationTypeService)

      val future = scenarioService.findMatchesForRestrainedScenario(mr.leftProfile.assignee, mr.rightProfile.globalCode, mr.leftProfile.globalCode, false)
      val result = Await.result(future, duration)

      result.size mustBe 1

    }

    "add sample associations to restrained scenario" in {
      val mockMatchingRepository = mock[MatchingRepository]
      val mr = Stubs.matchResult
      when(mockMatchingRepository.matchesWithFullHit(any[SampleCode])).thenReturn(Future.successful(Nil))
      when(mockMatchingRepository.getByFiringAndMatchingProfile(any[SampleCode], any[SampleCode])).thenReturn(Future.successful(Some(mr)))

      val mockCalculationTypeService = mock[CalculationTypeService]
      when(mockCalculationTypeService.getAnalysisTypeByCalculation(any[String])).thenReturn(Future.successful(Stubs.analysisTypes(0)))

      val mockProfileService = mock[ProfileService]
      when(mockProfileService.findByCode(mr.rightProfile.globalCode)).thenReturn(Future.successful(Some(Stubs.newProfile)))
      when(mockProfileService.findByCode(mr.leftProfile.globalCode)).thenReturn(Future.successful(Some(Stubs.newProfile)))
      when(mockProfileService.findByCode(SampleCode("XX-X-XXXX-1"))).thenReturn(Future.successful(Some(Stubs.newProfile)))
      when(mockProfileService.findByCode(SampleCode("XX-X-XXXX-2"))).thenReturn(Future.successful(Some(Stubs.newProfile)))
      when(mockProfileService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(List("XX-X-XXXX-1", "XX-X-XXXX-2"))

      val scenarioService =  new ScenarioServiceImpl(mockProfileService, mockMatchingRepository, null, null, mockCalculationTypeService)

      val future = scenarioService.findMatchesForRestrainedScenario(mr.leftProfile.assignee, mr.rightProfile.globalCode, mr.leftProfile.globalCode, false)
      val result = Await.result(future, duration)

      result.size mustBe 3

    }

    "not repeat options for restrained scenario" in {

      val mockMatchingRepository = mock[MatchingRepository]
      val mr = Stubs.matchResult
      when(mockMatchingRepository.matchesWithFullHit(any[SampleCode])).thenReturn(Future.successful(Seq(mr)))
      when(mockMatchingRepository.getByFiringAndMatchingProfile(any[SampleCode], any[SampleCode])).thenReturn(Future.successful(Some(mr)))

      val reference = Profile(null, mr.leftProfile.globalCode, null, null, null, Map(), None, None, Some(1), None, None, None, false, false, true)

      val mockProfileService = mock[ProfileService]
      when(mockProfileService.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(reference)))
      when(mockProfileService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Nil)

      val mockCalculationTypeService = mock[CalculationTypeService]
      when(mockCalculationTypeService.getAnalysisTypeByCalculation(any[String])).thenReturn(Future.successful(Stubs.analysisTypes(0)))

      val scenarioService =  new ScenarioServiceImpl(mockProfileService, mockMatchingRepository, null, null, mockCalculationTypeService)

      val future = scenarioService.findMatchesForRestrainedScenario(mr.leftProfile.assignee, mr.rightProfile.globalCode, mr.leftProfile.globalCode, false)
      val result = Await.result(future, duration)

      result.size mustBe 1

    }

  }

  "A Scenario Service" must {
    "find matches with n=1 for normal scenario" in {

      val mr1 = MatchResult(MongoId("54eb50cc2cdc8a94c6ee794b"), MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-B-IMBICE-111"),"tst-genetist",MatchStatus.pending,None),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI), 1)

      val mr2 = MatchResult(MongoId("54eb50cc2cdc8a94c6ee794b"), MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-B-IMBICE-222"),"tst-genetist",MatchStatus.pending,None),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI), 1)

      val mockMatchingRepository = mock[MatchingRepository]
      when(mockMatchingRepository.matchesNotDiscarded(any[SampleCode])).thenReturn(Future.successful(Seq(mr1, mr2)))

      val mockProfileService = mock[ProfileService]
      when(mockProfileService.findByCode(SampleCode("AR-B-IMBICE-111")))
        .thenReturn(Future.successful(Some(Profile(SampleCode("AR-B-IMBICE-111"), SampleCode("AR-B-IMBICE-111"), "", "", null, Map(), None, None, None, None, None, None, false, true))))
      when(mockProfileService.findByCode(SampleCode("AR-B-IMBICE-222")))
        .thenReturn(Future.successful(Some(Profile(SampleCode("AR-B-IMBICE-222"), SampleCode("AR-B-IMBICE-222"), "", "", null, Map(), None, None, Some(2), None, None, None, false, true))))
      when(mockProfileService.findByCode(SampleCode("AR-C-SHDG-1")))
        .thenReturn(Future.successful(Some(Profile(SampleCode("AR-C-SHDG-1"), SampleCode("AR-C-SHDG-1"), "", "", null, Map(), None, None, None, None, None, None, false, true))))
      when(mockProfileService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Nil)

      val mockCalculationTypeService = mock[CalculationTypeService]
      when(mockCalculationTypeService.filterMatchResults(any[Seq[MatchResult]], any[String])).thenReturn(Future.successful(Seq(mr1, mr2)))
      when(mockCalculationTypeService.getAnalysisTypeByCalculation(any[String])).thenReturn(Future.successful(Stubs.analysisTypes(0)))

      val scenarioService = new ScenarioServiceImpl(mockProfileService, mockMatchingRepository, null, null, mockCalculationTypeService)

      val future = scenarioService.findMatchesForScenario("tst-admintist", SampleCode("AR-C-SHDG-1"), false)
      val result = Await.result(future, duration)

      result.size mustBe 1
      result.head.associated mustBe false

    }

    "validate user for normal scenario" in {

      val mr1 = MatchResult(MongoId("54eb50cc2cdc8a94c6ee794b"), MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-B-IMBICE-111"),"tst-genetist",MatchStatus.pending,None),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI), 1)

      val mr2 = MatchResult(MongoId("54eb50cc2cdc8a94c6ee794b"), MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-B-IMBICE-222"),"tst-genetist",MatchStatus.pending,None),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI), 1)

      val mockMatchingRepository = mock[MatchingRepository]
      when(mockMatchingRepository.matchesNotDiscarded(any[SampleCode])).thenReturn(Future.successful(Seq(mr1, mr2)))

      val mockProfileService = mock[ProfileService]
      when(mockProfileService.findByCode(SampleCode("AR-B-IMBICE-111")))
        .thenReturn(Future.successful(Some(Profile(SampleCode("AR-B-IMBICE-111"), SampleCode("AR-B-IMBICE-111"), "", "", null, Map(), None, None, None, None, None, None, false, true))))
      when(mockProfileService.findByCode(SampleCode("AR-B-IMBICE-222")))
        .thenReturn(Future.successful(Some(Profile(SampleCode("AR-B-IMBICE-222"), SampleCode("AR-B-IMBICE-222"), "", "", null, Map(), None, None, Some(2), None, None, None, false, true))))
      when(mockProfileService.findByCode(SampleCode("AR-C-SHDG-1")))
        .thenReturn(Future.successful(Some(Profile(SampleCode("AR-C-SHDG-1"), SampleCode("AR-C-SHDG-1"), "", "", null, Map(), None, None, None, None, None, None, false, true))))
      when(mockProfileService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Nil)

      val mockCalculationTypeService = mock[CalculationTypeService]
      when(mockCalculationTypeService.filterMatchResults(any[Seq[MatchResult]], any[String])).thenReturn(Future.successful(Seq(mr1, mr2)))
      when(mockCalculationTypeService.getAnalysisTypeByCalculation(any[String])).thenReturn(Future.successful(Stubs.analysisTypes(0)))

      val scenarioService = new ScenarioServiceImpl(mockProfileService, mockMatchingRepository, null, null, mockCalculationTypeService)

      val future = scenarioService.findMatchesForScenario("esurijon", SampleCode("AR-C-SHDG-1"), false)
      val result = Await.result(future, duration)

      result.size mustBe 0
    }

    "find associations for normal scenario" in {

      val sample = Stubs.newProfileLabeledGenotypification

      val mockProfileService = mock[ProfileService]
      when(mockProfileService.findByCode(sample.globalCode)).thenReturn(Future.successful(Some(sample)))
      when(mockProfileService.findByCode(SampleCode("XX-X-XXXX-1"))).thenReturn(Future.successful(Some(Stubs.newProfile)))
      when(mockProfileService.findByCode(SampleCode("XX-X-XXXX-2"))).thenReturn(Future.successful(Some(Stubs.newProfile)))
      when(mockProfileService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(List("XX-X-XXXX-1", "XX-X-XXXX-2"))

      val mockMatchingRepository = mock[MatchingRepository]
      when(mockMatchingRepository.matchesNotDiscarded(any[SampleCode])).thenReturn(Future.successful(Nil))

      val mockCalculationTypeService = mock[CalculationTypeService]
      when(mockCalculationTypeService.filterMatchResults(any[Seq[MatchResult]], any[String])).thenReturn(Future.successful(Nil))
      when(mockCalculationTypeService.getAnalysisTypeByCalculation(any[String])).thenReturn(Future.successful(Stubs.analysisTypes(0)))

      val scenarioService = new ScenarioServiceImpl(mockProfileService, mockMatchingRepository, null, null, mockCalculationTypeService)

      val future = scenarioService.findMatchesForScenario(sample.assignee, sample.globalCode, false)
      val result = Await.result(future, duration)

      result.size mustBe 2
      result.head.associated mustBe true

    }

    "not repeat options for normal scenario" in {

      val sample = Stubs.newProfileLabeledGenotypification

      val mockProfileService = mock[ProfileService]
      when(mockProfileService.findByCode(sample.globalCode)).thenReturn(Future.successful(Some(sample)))
      when(mockProfileService.findByCode(AdditionalMatchers.not(Matchers.eq(sample.globalCode)))).thenReturn(Future.successful(Some(Stubs.newProfile)))
      when(mockProfileService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(List("XX-X-XXXX-1", "XX-X-XXXX-1"))

      val mockMatchingRepository = mock[MatchingRepository]
      when(mockMatchingRepository.matchesNotDiscarded(any[SampleCode])).thenReturn(Future.successful(Nil))

      val mockCalculationTypeService = mock[CalculationTypeService]
      when(mockCalculationTypeService.filterMatchResults(any[Seq[MatchResult]], any[String])).thenReturn(Future.successful(Nil))
      when(mockCalculationTypeService.getAnalysisTypeByCalculation(any[String])).thenReturn(Future.successful(Stubs.analysisTypes(0)))

      val scenarioService = new ScenarioServiceImpl(mockProfileService, mockMatchingRepository, null, null, mockCalculationTypeService)

      val future = scenarioService.findMatchesForScenario(sample.assignee, sample.globalCode, false)
      val result = Await.result(future, duration)

      result.size mustBe 1
      result.head.associated mustBe true

    }
  }

  "A Scenario Service" must {
    "find matches for an existing scenario" in {

      val scenario = Stubs.newScenario

      val mockProfileService = mock[ProfileService]
      when(mockProfileService.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.newProfile)))

      val mockCalculationTypeService = mock[CalculationTypeService]
      when(mockCalculationTypeService.getAnalysisTypeByCalculation(any[String])).thenReturn(Future.successful(Stubs.analysisTypes(0)))

      val scenarioService =  new ScenarioServiceImpl(mockProfileService, null, null, null, mockCalculationTypeService)

      val future = scenarioService.findExistingMatches(scenario)
      val result = Await.result(future, duration)

      result.size mustBe scenario.calculationScenario.prosecutor.selected.size + scenario.calculationScenario.prosecutor.unselected.size
    }

  }


    "A Scenario Service" must {
    "add a scenario returns right" in {

      val scenario = Stubs.newScenario

      val scenarioRepository = mock[ScenarioRepository]
      when(scenarioRepository.add(scenario)).thenReturn(Future.successful(Right(scenario.name + " (" + scenario._id.id + ")")))
      val scenarioService = new ScenarioServiceImpl(null, null, null, scenarioRepository, null)

      val future = scenarioService.add(scenario)
      val result = Await.result(future, duration)

      result must not be null
      result.isRight mustBe true
      result.right.get mustBe scenario.name + " (" + scenario._id.id + ")"
    }

    "add a scenario returns left" in {

      val scenario = Stubs.newScenario

      val scenarioRepository = mock[ScenarioRepository]
      when(scenarioRepository.add(scenario)).thenReturn(Future.successful(Left("Error")))
      val scenarioService = new ScenarioServiceImpl(null, null, null, scenarioRepository, null)

      val future = scenarioService.add(scenario)
      val result = Await.result(future, duration)

      result must not be null
      result.isLeft mustBe true
      result.left.get mustBe "Error"
    }

    "get a scenario returns a list with one scenario" in {

      val scenario = Stubs.newScenario
      val search = ScenarioSearch(scenario.calculationScenario.sample)

      val scenarioRepository = mock[ScenarioRepository]
      when(scenarioRepository.get(scenario.geneticist, search, false)).thenReturn(Future.successful(Seq(scenario)))
      val scenarioService = new ScenarioServiceImpl(null, null, null, scenarioRepository, null)

      val result: Seq[Scenario] = Await.result(scenarioService.search(scenario.geneticist, search, false), duration)

      result must not be null
      result.size mustBe 1
    }

    "get a scenario by id returns the scenario" in {

      val scenario = Stubs.newScenario

      val scenarioRepository = mock[ScenarioRepository]
      when(scenarioRepository.get(scenario.geneticist, scenario._id, false)).thenReturn(Future.successful(Some(scenario)))
      val scenarioService = new ScenarioServiceImpl(null, null, null, scenarioRepository, null)

      val result: Option[Scenario] = Await.result(scenarioService.get(scenario.geneticist, scenario._id, false), duration)

      result.isDefined mustBe true
      result.get mustBe scenario
    }

    "delete a scenario" in {

      val scenario = Stubs.newScenario

      val scenarioRepository = mock[ScenarioRepository]
      when(scenarioRepository.delete(scenario.geneticist, scenario._id, false)).thenReturn(Future.successful(Right(scenario._id.id)))
      val scenarioService = new ScenarioServiceImpl(null, null, null, scenarioRepository, null)

      val future = scenarioService.delete(scenario.geneticist, scenario._id, false)
      val result = Await.result(future, duration)

      result must not be null
      result.isRight mustBe true
      result.right.get mustBe scenario._id.id
    }

    "validate a scenario" in {

      val scenario = Stubs.newScenario

      val scenarioRepository = mock[ScenarioRepository]
      when(scenarioRepository.validate(scenario.geneticist, scenario, false)).thenReturn(Future.successful(Right(scenario._id.id)))
      val scenarioService = new ScenarioServiceImpl(null, null, null, scenarioRepository, null)

      val future = scenarioService.validate(scenario.geneticist, scenario, false)
      val result = Await.result(future, duration)

      result must not be null
      result.isRight mustBe true
      result.right.get mustBe scenario._id.id
    }

    "update a scenario" in {

      val scenario = Stubs.newScenario

      val scenarioRepository = mock[ScenarioRepository]
      when(scenarioRepository.update(scenario.geneticist, scenario, false)).thenReturn(Future.successful(Right(scenario._id.id)))
      val scenarioService = new ScenarioServiceImpl(null, null, null, scenarioRepository, null)

      val future = scenarioService.update(scenario.geneticist, scenario, false)
      val result = Await.result(future, duration)

      result must not be null
      result.isRight mustBe true
      result.right.get mustBe scenario._id.id
    }
  }

  "A Scenario Service" must {
    "get lr-mix" in {

      val seqPopulation: Seq[PopulationSampleFrequency] = List(
        PopulationSampleFrequency("TPOX", -1, BigDecimal("0.0000000001")),
        PopulationSampleFrequency("TPOX", 5, BigDecimal("0.00016000")),
        PopulationSampleFrequency("TPOX", 6, BigDecimal("0.00261000")),
        PopulationSampleFrequency("TPOX", 7, BigDecimal("0.00142000")),
        PopulationSampleFrequency("TPOX", 8, BigDecimal("0.48349000")),
        PopulationSampleFrequency("TPOX", 9, BigDecimal("0.07790000")),
        PopulationSampleFrequency("TPOX", 10, BigDecimal("0.04761000")),
        PopulationSampleFrequency("TPOX", 11, BigDecimal("0.28963000")),
        PopulationSampleFrequency("TPOX", 12, BigDecimal("0.09435000")),
        PopulationSampleFrequency("TPOX", 13, BigDecimal("0.00256000")),
        PopulationSampleFrequency("TPOX", 14, BigDecimal("0.00011000")),
        PopulationSampleFrequency("TPOX", 16, BigDecimal("0.00005000")),
        PopulationSampleFrequency("TPOX", 21, BigDecimal("0.00005000")),
        PopulationSampleFrequency("TPOX", 21.2, BigDecimal("0.00005000")),
        PopulationSampleFrequency("D3S1358", -1, BigDecimal("0.0000000001")),
        PopulationSampleFrequency("D3S1358", 10, BigDecimal("0.00005000")),
        PopulationSampleFrequency("D3S1358", 11, BigDecimal("0.00055000")),
        PopulationSampleFrequency("D3S1358", 12, BigDecimal("0.00186000")),
        PopulationSampleFrequency("D3S1358", 13, BigDecimal("0.00399000")),
        PopulationSampleFrequency("D3S1358", 14, BigDecimal("0.07485000")),
        PopulationSampleFrequency("D3S1358", 15, BigDecimal("0.35112000")),
        PopulationSampleFrequency("D3S1358", 16, BigDecimal("0.27983000")),
        PopulationSampleFrequency("D3S1358", 17, BigDecimal("0.16096000")),
        PopulationSampleFrequency("D3S1358", 18, BigDecimal("0.11706000")),
        PopulationSampleFrequency("D3S1358", 19, BigDecimal("0.00891000")),
        PopulationSampleFrequency("D3S1358", 20, BigDecimal("0.00071000")),
        PopulationSampleFrequency("D3S1358", 21, BigDecimal("0.00011000")),
        PopulationSampleFrequency("FGA", -1, BigDecimal("0.0000000001")),
        PopulationSampleFrequency("FGA", 16, BigDecimal("0.00038000")),
        PopulationSampleFrequency("FGA", 17, BigDecimal("0.00223000")),
        PopulationSampleFrequency("FGA", 18, BigDecimal("0.01084000")),
        PopulationSampleFrequency("FGA", 18.2, BigDecimal("0.00038000")),
        PopulationSampleFrequency("FGA", 19, BigDecimal("0.08764000")),
        PopulationSampleFrequency("FGA", 19.2, BigDecimal("0.00016000")),
        PopulationSampleFrequency("FGA", 20, BigDecimal("0.09248000")),
        PopulationSampleFrequency("FGA", 20.2, BigDecimal("0.00022000")),
        PopulationSampleFrequency("FGA", 21, BigDecimal("0.14417000")),
        PopulationSampleFrequency("FGA", 21.2, BigDecimal("0.00202000")),
        PopulationSampleFrequency("FGA", 22, BigDecimal("0.12609000")),
        PopulationSampleFrequency("FGA", 22.2, BigDecimal("0.00398000")),
        PopulationSampleFrequency("FGA", 22.3, BigDecimal("0.00005000")),
        PopulationSampleFrequency("FGA", 23, BigDecimal("0.12130000")),
        PopulationSampleFrequency("FGA", 23.2, BigDecimal("0.00245000")),
        PopulationSampleFrequency("FGA", 24, BigDecimal("0.15153000")),
        PopulationSampleFrequency("FGA", 24.2, BigDecimal("0.00071000")),
        PopulationSampleFrequency("FGA", 25, BigDecimal("0.15044000")),
        PopulationSampleFrequency("FGA", 25.2, BigDecimal("0.00005000")),
        PopulationSampleFrequency("FGA", 25.3, BigDecimal("0.00005000")),
        PopulationSampleFrequency("FGA", 26, BigDecimal("0.07375000")),
        PopulationSampleFrequency("FGA", 26.2, BigDecimal("0.00005000")),
        PopulationSampleFrequency("FGA", 27, BigDecimal("0.02200000")),
        PopulationSampleFrequency("FGA", 28, BigDecimal("0.00599000")),
        PopulationSampleFrequency("FGA", 29, BigDecimal("0.00054000")),
        PopulationSampleFrequency("FGA", 30, BigDecimal("0.00016000")),
        PopulationSampleFrequency("FGA", 31, BigDecimal("0.00022000")),
        PopulationSampleFrequency("D5S818", -1, BigDecimal("0.0000000001")),
        PopulationSampleFrequency("D5S818", 7, BigDecimal("0.06930000")),
        PopulationSampleFrequency("D5S818", 8, BigDecimal("0.00635000")),
        PopulationSampleFrequency("D5S818", 9, BigDecimal("0.04210000")),
        PopulationSampleFrequency("D5S818", 10, BigDecimal("0.05326000")),
        PopulationSampleFrequency("D5S818", 11, BigDecimal("0.42216000")),
        PopulationSampleFrequency("D5S818", 12, BigDecimal("0.27108000")),
        PopulationSampleFrequency("D5S818", 13, BigDecimal("0.12503000")),
        PopulationSampleFrequency("D5S818", 14, BigDecimal("0.00843000")),
        PopulationSampleFrequency("D5S818", 15, BigDecimal("0.00208000")),
        PopulationSampleFrequency("D5S818", 16, BigDecimal("0.00022000")),
        PopulationSampleFrequency("CSF1PO", -1, BigDecimal("0.0000000001")),
        PopulationSampleFrequency("CSF1PO", 0, BigDecimal("0.00011000")),
        PopulationSampleFrequency("CSF1PO", 6, BigDecimal("0.00006000")),
        PopulationSampleFrequency("CSF1PO", 7, BigDecimal("0.00167000")),
        PopulationSampleFrequency("CSF1PO", 8, BigDecimal("0.00448000")),
        PopulationSampleFrequency("CSF1PO", 8.3, BigDecimal("0.00006000")),
        PopulationSampleFrequency("CSF1PO", 9, BigDecimal("0.02185000")),
        PopulationSampleFrequency("CSF1PO", 10, BigDecimal("0.26929000")),
        PopulationSampleFrequency("CSF1PO", 10.3, BigDecimal("0.00006000")),
        PopulationSampleFrequency("CSF1PO", 11, BigDecimal("0.28222000")),
        PopulationSampleFrequency("CSF1PO", 12, BigDecimal("0.34753000")),
        PopulationSampleFrequency("CSF1PO", 13, BigDecimal("0.06209000")),
        PopulationSampleFrequency("CSF1PO", 14, BigDecimal("0.00868000")),
        PopulationSampleFrequency("CSF1PO", 15, BigDecimal("0.00167000")),
        PopulationSampleFrequency("D7S520", -1, BigDecimal("0.0000000001")),
        PopulationSampleFrequency("D7S520", 6, BigDecimal("0.00011000")),
        PopulationSampleFrequency("D7S520", 7, BigDecimal("0.01408000")),
        PopulationSampleFrequency("D7S520", 8, BigDecimal("0.10337000")),
        PopulationSampleFrequency("D7S520", 9, BigDecimal("0.08820000")),
        PopulationSampleFrequency("D7S520", 9.1, BigDecimal("0.00005000")),
        PopulationSampleFrequency("D7S520", 10, BigDecimal("0.26243000")),
        PopulationSampleFrequency("D7S520", 11, BigDecimal("0.30968000")),
        PopulationSampleFrequency("D7S520", 12, BigDecimal("0.18630000")),
        PopulationSampleFrequency("D7S520", 12.1, BigDecimal("0.00005000")),
        PopulationSampleFrequency("D7S520", 12.2, BigDecimal("0.00005000")),
        PopulationSampleFrequency("D7S520", 12.3, BigDecimal("0.00005000")),
        PopulationSampleFrequency("D7S520", 13, BigDecimal("0.03094000")),
        PopulationSampleFrequency("D7S520", 14, BigDecimal("0.00462000")),
        PopulationSampleFrequency("D7S520", 15, BigDecimal("0.00016000")),
        PopulationSampleFrequency("D8S1179", -1, BigDecimal("0.0000000001")),
        PopulationSampleFrequency("D8S1179", 8, BigDecimal("0.00898000")),
        PopulationSampleFrequency("D8S1179", 9, BigDecimal("0.00762000")),
        PopulationSampleFrequency("D8S1179", 10, BigDecimal("0.06872000")),
        PopulationSampleFrequency("D8S1179", 11, BigDecimal("0.07247000")),
        PopulationSampleFrequency("D8S1179", 12, BigDecimal("0.14353000")),
        PopulationSampleFrequency("D8S1179", 13, BigDecimal("0.30446000")),
        PopulationSampleFrequency("D8S1179", 14, BigDecimal("0.22236000")),
        PopulationSampleFrequency("D8S1179", 15, BigDecimal("0.13732000")),
        PopulationSampleFrequency("D8S1179", 16, BigDecimal("0.03047000")),
        PopulationSampleFrequency("D8S1179", 17, BigDecimal("0.00337000")),
        PopulationSampleFrequency("D8S1179", 18, BigDecimal("0.00065000")),
        PopulationSampleFrequency("D8S1179", 19, BigDecimal("0.00005000")),
        PopulationSampleFrequency("TH01", -1, BigDecimal("0.0000000001")),
        PopulationSampleFrequency("TH01", 4, BigDecimal("0.00006000")),
        PopulationSampleFrequency("TH01", 5, BigDecimal("0.00064000")),
        PopulationSampleFrequency("TH01", 6, BigDecimal("0.29564000")),
        PopulationSampleFrequency("TH01", 7, BigDecimal("0.26031000")),
        PopulationSampleFrequency("TH01", 8, BigDecimal("0.08001000")),
        PopulationSampleFrequency("TH01", 9, BigDecimal("0.12342000")),
        PopulationSampleFrequency("TH01", 9.3, BigDecimal("0.23091000")),
        PopulationSampleFrequency("TH01", 10, BigDecimal("0.00808000")),
        PopulationSampleFrequency("TH01", 11, BigDecimal("0.00070000")),
        PopulationSampleFrequency("TH01", 13.3, BigDecimal("0.00006000")),
        PopulationSampleFrequency("vWA", -1, BigDecimal("0.0000000001")),
        PopulationSampleFrequency("vWA", 11, BigDecimal("0.00087000")),
        PopulationSampleFrequency("vWA", 12, BigDecimal("0.00082000")),
        PopulationSampleFrequency("vWA", 13, BigDecimal("0.00414000")),
        PopulationSampleFrequency("vWA", 14, BigDecimal("0.06419000")),
        PopulationSampleFrequency("vWA", 15, BigDecimal("0.09430000")),
        PopulationSampleFrequency("vWA", 16, BigDecimal("0.30923000")),
        PopulationSampleFrequency("vWA", 17, BigDecimal("0.28278000")),
        PopulationSampleFrequency("vWA", 18, BigDecimal("0.16705000")),
        PopulationSampleFrequency("vWA", 19, BigDecimal("0.06425000")),
        PopulationSampleFrequency("vWA", 20, BigDecimal("0.01123000")),
        PopulationSampleFrequency("vWA", 21, BigDecimal("0.00093000")),
        PopulationSampleFrequency("vWA", 22, BigDecimal("0.00005000")),
        PopulationSampleFrequency("vWA", 23, BigDecimal("0.00005000")),
        PopulationSampleFrequency("D13S317", -1, BigDecimal("0.0000000001")),
        PopulationSampleFrequency("D13S317", 6, BigDecimal("0.00054000")),
        PopulationSampleFrequency("D13S317", 7, BigDecimal("0.00027000")),
        PopulationSampleFrequency("D13S317", 8, BigDecimal("0.09117000")),
        PopulationSampleFrequency("D13S317", 9, BigDecimal("0.15994000")),
        PopulationSampleFrequency("D13S317", 10, BigDecimal("0.07562000")),
        PopulationSampleFrequency("D13S317", 11, BigDecimal("0.22469000")),
        PopulationSampleFrequency("D13S317", 12, BigDecimal("0.24182000")),
        PopulationSampleFrequency("D13S317", 13, BigDecimal("0.12787000")),
        PopulationSampleFrequency("D13S317", 14, BigDecimal("0.07606000")),
        PopulationSampleFrequency("D13S317", 15, BigDecimal("0.00196000")),
        PopulationSampleFrequency("D13S317", 16, BigDecimal("0.00005000")),
        PopulationSampleFrequency("Penta E", -1, BigDecimal("0.0000000001")),
        PopulationSampleFrequency("Penta E", 5, BigDecimal("0.03706000")),
        PopulationSampleFrequency("Penta E", 6, BigDecimal("0.00051000")),
        PopulationSampleFrequency("Penta E", 7, BigDecimal("0.09435000")),
        PopulationSampleFrequency("Penta E", 8, BigDecimal("0.02785000")),
        PopulationSampleFrequency("Penta E", 9, BigDecimal("0.00695000")),
        PopulationSampleFrequency("Penta E", 10, BigDecimal("0.06017000")),
        PopulationSampleFrequency("Penta E", 11, BigDecimal("0.08780000")),
        PopulationSampleFrequency("Penta E", 12, BigDecimal("0.18220000")),
        PopulationSampleFrequency("Penta E", 13, BigDecimal("0.09463000")),
        PopulationSampleFrequency("Penta E", 13.2, BigDecimal("0.00011000")),
        PopulationSampleFrequency("Penta E", 14, BigDecimal("0.07299000")),
        PopulationSampleFrequency("Penta E", 15, BigDecimal("0.09814000")),
        PopulationSampleFrequency("Penta E", 16, BigDecimal("0.05927000")),
        PopulationSampleFrequency("Penta E", 16.3, BigDecimal("0.00090000")),
        PopulationSampleFrequency("Penta E", 16.4, BigDecimal("0.00006000")),
        PopulationSampleFrequency("Penta E", 17, BigDecimal("0.05322000")),
        PopulationSampleFrequency("Penta E", 18, BigDecimal("0.03859000")),
        PopulationSampleFrequency("Penta E", 19, BigDecimal("0.02616000")),
        PopulationSampleFrequency("Penta E", 20, BigDecimal("0.02537000")),
        PopulationSampleFrequency("Penta E", 21, BigDecimal("0.02107000")),
        PopulationSampleFrequency("Penta E", 22, BigDecimal("0.00757000")),
        PopulationSampleFrequency("Penta E", 23, BigDecimal("0.00345000")),
        PopulationSampleFrequency("Penta E", 24, BigDecimal("0.00113000")),
        PopulationSampleFrequency("Penta E", 25, BigDecimal("0.00028000")),
        PopulationSampleFrequency("Penta E", 26, BigDecimal("0.00006000")),
        PopulationSampleFrequency("D16S539", -1, BigDecimal("0.0000000001")),
        PopulationSampleFrequency("D16S539", 5, BigDecimal("0.00005000")),
        PopulationSampleFrequency("D16S539", 7, BigDecimal("0.00011000")),
        PopulationSampleFrequency("D16S539", 8, BigDecimal("0.01758000")),
        PopulationSampleFrequency("D16S539", 9, BigDecimal("0.16177000")),
        PopulationSampleFrequency("D16S539", 10, BigDecimal("0.10915000")),
        PopulationSampleFrequency("D16S539", 11, BigDecimal("0.27673000")),
        PopulationSampleFrequency("D16S539", 12, BigDecimal("0.27623000")),
        PopulationSampleFrequency("D16S539", 13, BigDecimal("0.13790000")),
        PopulationSampleFrequency("D16S539", 14, BigDecimal("0.01917000")),
        PopulationSampleFrequency("D16S539", 15, BigDecimal("0.00131000")),
        PopulationSampleFrequency("D18S51", -1, BigDecimal("0.0000000001")),
        PopulationSampleFrequency("D18S51", 9, BigDecimal("0.00038000")),
        PopulationSampleFrequency("D18S51", 10, BigDecimal("0.00728000")),
        PopulationSampleFrequency("D18S51", 10.2, BigDecimal("0.00011000")),
        PopulationSampleFrequency("D18S51", 11, BigDecimal("0.01144000")),
        PopulationSampleFrequency("D18S51", 12, BigDecimal("0.12708000")),
        PopulationSampleFrequency("D18S51", 13, BigDecimal("0.11537000")),
        PopulationSampleFrequency("D18S51", 13.2, BigDecimal("0.00011000")),
        PopulationSampleFrequency("D18S51", 14, BigDecimal("0.20496000")),
        PopulationSampleFrequency("D18S51", 14.2, BigDecimal("0.00016000")),
        PopulationSampleFrequency("D18S51", 15, BigDecimal("0.14191000")),
        PopulationSampleFrequency("D18S51", 15.2, BigDecimal("0.00005000")),
        PopulationSampleFrequency("D18S51", 16, BigDecimal("0.12002000")),
        PopulationSampleFrequency("D18S51", 16.2, BigDecimal("0.00005000")),
        PopulationSampleFrequency("D18S51", 17, BigDecimal("0.12407000")),
        PopulationSampleFrequency("D18S51", 18, BigDecimal("0.06715000")),
        PopulationSampleFrequency("D18S51", 19, BigDecimal("0.03639000")),
        PopulationSampleFrequency("D18S51", 20, BigDecimal("0.01954000")),
        PopulationSampleFrequency("D18S51", 21, BigDecimal("0.01144000")),
        PopulationSampleFrequency("D18S51", 22, BigDecimal("0.00859000")),
        PopulationSampleFrequency("D18S51", 22.2, BigDecimal("0.00005000")),
        PopulationSampleFrequency("D18S51", 23, BigDecimal("0.00235000")),
        PopulationSampleFrequency("D18S51", 24, BigDecimal("0.00104000")),
        PopulationSampleFrequency("D18S51", 25, BigDecimal("0.00016000")),
        PopulationSampleFrequency("D18S51", 26, BigDecimal("0.00005000")),
        PopulationSampleFrequency("D18S51", 27, BigDecimal("0.00016000")),
        PopulationSampleFrequency("PentaD", -1, BigDecimal("0.0000000001")),
        PopulationSampleFrequency("PentaD", 2.2, BigDecimal("0.00358000")),
        PopulationSampleFrequency("PentaD", 3.2, BigDecimal("0.00011000")),
        PopulationSampleFrequency("PentaD", 5, BigDecimal("0.00146000")),
        PopulationSampleFrequency("PentaD", 6, BigDecimal("0.00039000")),
        PopulationSampleFrequency("PentaD", 7, BigDecimal("0.01014000")),
        PopulationSampleFrequency("PentaD", 8, BigDecimal("0.01366000")),
        PopulationSampleFrequency("PentaD", 9, BigDecimal("0.19036000")),
        PopulationSampleFrequency("PentaD", 9.2, BigDecimal("0.00056000")),
        PopulationSampleFrequency("PentaD", 10, BigDecimal("0.20671000")),
        PopulationSampleFrequency("PentaD", 11, BigDecimal("0.16045000")),
        PopulationSampleFrequency("PentaD", 12, BigDecimal("0.17008000")),
        PopulationSampleFrequency("PentaD", 13, BigDecimal("0.16840000")),
        PopulationSampleFrequency("PentaD", 14, BigDecimal("0.05410000")),
        PopulationSampleFrequency("PentaD", 15, BigDecimal("0.01512000")),
        PopulationSampleFrequency("PentaD", 16, BigDecimal("0.00364000")),
        PopulationSampleFrequency("PentaD", 17, BigDecimal("0.00090000")),
        PopulationSampleFrequency("PentaD", 18, BigDecimal("0.00017000")),
        PopulationSampleFrequency("PentaD", 19, BigDecimal("0.00006000")),
        PopulationSampleFrequency("D21S11", -1, BigDecimal("0.0000000001")),
        PopulationSampleFrequency("D21S11", 19, BigDecimal("0.00005000")),
        PopulationSampleFrequency("D21S11", 24, BigDecimal("0.00005000")),
        PopulationSampleFrequency("D21S11", 24.2, BigDecimal("0.00087000")),
        PopulationSampleFrequency("D21S11", 25, BigDecimal("0.00033000")),
        PopulationSampleFrequency("D21S11", 25.2, BigDecimal("0.00054000")),
        PopulationSampleFrequency("D21S11", 26, BigDecimal("0.00158000")),
        PopulationSampleFrequency("D21S11", 26.2, BigDecimal("0.00016000")),
        PopulationSampleFrequency("D21S11", 27, BigDecimal("0.01638000")),
        PopulationSampleFrequency("D21S11", 27.2, BigDecimal("0.00011000")),
        PopulationSampleFrequency("D21S11", 28, BigDecimal("0.09375000")),
        PopulationSampleFrequency("D21S11", 28.2, BigDecimal("0.00076000")),
        PopulationSampleFrequency("D21S11", 29, BigDecimal("0.19643000")),
        PopulationSampleFrequency("D21S11", 29.2, BigDecimal("0.00207000")),
        PopulationSampleFrequency("D21S11", 30, BigDecimal("0.27228000")),
        PopulationSampleFrequency("D21S11", 30.2, BigDecimal("0.02536000")),
        PopulationSampleFrequency("D21S11", 31, BigDecimal("0.06209000")),
        PopulationSampleFrequency("D21S11", 31.2, BigDecimal("0.11312000")),
        PopulationSampleFrequency("D21S11", 32, BigDecimal("0.00920000")),
        PopulationSampleFrequency("D21S11", 32.1, BigDecimal("0.00016000")),
        PopulationSampleFrequency("D21S11", 32.2, BigDecimal("0.13843000")),
        PopulationSampleFrequency("D21S11", 33, BigDecimal("0.00212000")),
        PopulationSampleFrequency("D21S11", 33.1, BigDecimal("0.00011000")),
        PopulationSampleFrequency("D21S11", 33.2, BigDecimal("0.05430000")),
        PopulationSampleFrequency("D21S11", 33.3, BigDecimal("0.00011000")),
        PopulationSampleFrequency("D21S11", 34, BigDecimal("0.00044000")),
        PopulationSampleFrequency("D21S11", 34.2, BigDecimal("0.00637000")),
        PopulationSampleFrequency("D21S11", 35, BigDecimal("0.00103000")),
        PopulationSampleFrequency("D21S11", 35.1, BigDecimal("0.00027000")),
        PopulationSampleFrequency("D21S11", 35.2, BigDecimal("0.00087000")),
        PopulationSampleFrequency("D21S11", 36, BigDecimal("0.00027000")),
        PopulationSampleFrequency("D21S11", 36.2, BigDecimal("0.00005000")),
        PopulationSampleFrequency("D21S11", 37, BigDecimal("0.00005000")))

      val baseFrequency = new PopulationBaseFrequency("pop freq 1", 0, ProbabilityModel.HardyWeinberg, seqPopulation)

      val mockPopulationService = mock[PopulationBaseFrequencyService]
      when(mockPopulationService.getByName(any[String])).thenReturn(Future.successful(Some(baseFrequency)))

      val mockCalculationTypeService = mock[CalculationTypeService]
      when(mockCalculationTypeService.filterCodes(any[List[SampleCode]], any[String],any[List[Profile]])).thenReturn(Future.successful(List(Stubs.newProfile.genotypification(1),Stubs.newProfile.genotypification(1))))
      when(mockCalculationTypeService.getAnalysisTypeByCalculation(any[String])).thenReturn(Future.successful(Stubs.analysisTypes(0)))
      
      val scenarioService = new ScenarioServiceImpl(null, null, mockPopulationService, null, mockCalculationTypeService)

      val resultFut = scenarioService.getLRMix(Stubs.calculationScenario)
      val result = Await.result(resultFut, duration)

      result mustBe defined

    }

    "get N Correction dmp 1 and donnelly baldwin 1" in {
      val mockMatchingRepository = mock[MatchingRepository]
      val mr = MatchResult(MongoId("54eb50cc2cdc8a94c6ee794b"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-HIBA-500"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-B-IMBICE-500"),"tst-genetist",MatchStatus.pending,None),
        NewMatchingResult(ModerateStringency,
          Map("TH01" -> ModerateStringency, "CSF1PO" -> ModerateStringency, "D18S51" -> ModerateStringency, "AMEL" -> HighStringency, "D8S1179" -> ModerateStringency, "D3S1358" -> ModerateStringency, "D7S820" -> ModerateStringency, "FGA" -> ModerateStringency, "D16S539" -> HighStringency, "D13S317" -> ModerateStringency, "vWA" -> ModerateStringency, "D5S818" -> ModerateStringency, "TPOX" -> ModerateStringency, "D21S11" -> ModerateStringency),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI), 1)
      when(mockMatchingRepository.getByFiringAndMatchingProfile(any[SampleCode], any[SampleCode])).thenReturn(Future.successful(Some(mr)))

      val scenarioService =  new ScenarioServiceImpl(null, mockMatchingRepository, null, null, null)

      val future: Future[Either[String, NCorrectionResponse]] = scenarioService.getNCorrection(NCorrectionRequest(mr.leftProfile.globalCode, mr.rightProfile.globalCode, 2, 1))
      val result = Await.result(future, duration)

      result.isRight mustBe true
      result.right.get.dmp mustBe 1
      result.right.get.donnellyBaldwin mustBe 1
      result.right.get.n mustBe 1
    }

    "get N Correction dmp 0.5 and donnelly baldwin 2" in {
      val mockMatchingRepository = mock[MatchingRepository]
      val mr = MatchResult(MongoId("54eb50cc2cdc8a94c6ee794b"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-HIBA-500"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-B-IMBICE-500"),"tst-genetist",MatchStatus.pending,None),
        NewMatchingResult(ModerateStringency,
          Map("TH01" -> ModerateStringency, "CSF1PO" -> ModerateStringency, "D18S51" -> ModerateStringency, "AMEL" -> HighStringency, "D8S1179" -> ModerateStringency, "D3S1358" -> ModerateStringency, "D7S820" -> ModerateStringency, "FGA" -> ModerateStringency, "D16S539" -> HighStringency, "D13S317" -> ModerateStringency, "vWA" -> ModerateStringency, "D5S818" -> ModerateStringency, "TPOX" -> ModerateStringency, "D21S11" -> ModerateStringency),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI), 1)
      when(mockMatchingRepository.getByFiringAndMatchingProfile(any[SampleCode], any[SampleCode])).thenReturn(Future.successful(Some(mr)))

      val scenarioService =  new ScenarioServiceImpl(null, mockMatchingRepository, null, null, null)

      val future: Future[Either[String, NCorrectionResponse]] = scenarioService.getNCorrection(NCorrectionRequest(mr.leftProfile.globalCode, mr.rightProfile.globalCode, 2, 2))
      val result = Await.result(future, duration)

      result.isRight mustBe true
      result.right.get.dmp mustBe 0.5
      result.right.get.donnellyBaldwin mustBe 2
      result.right.get.n mustBe 1
    }

    "get N Correction dmp 0.75 and donnelly baldwin 3" in {
      val mockMatchingRepository = mock[MatchingRepository]
      val mr = MatchResult(MongoId("54eb50cc2cdc8a94c6ee794b"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-HIBA-500"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-B-IMBICE-500"),"tst-genetist",MatchStatus.pending,None),
        NewMatchingResult(ModerateStringency,
          Map("TH01" -> ModerateStringency, "CSF1PO" -> ModerateStringency, "D18S51" -> ModerateStringency, "AMEL" -> HighStringency, "D8S1179" -> ModerateStringency, "D3S1358" -> ModerateStringency, "D7S820" -> ModerateStringency, "FGA" -> ModerateStringency, "D16S539" -> HighStringency, "D13S317" -> ModerateStringency, "vWA" -> ModerateStringency, "D5S818" -> ModerateStringency, "TPOX" -> ModerateStringency, "D21S11" -> ModerateStringency),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI), 2)
      when(mockMatchingRepository.getByFiringAndMatchingProfile(any[SampleCode], any[SampleCode])).thenReturn(Future.successful(Some(mr)))

      val scenarioService =  new ScenarioServiceImpl(null, mockMatchingRepository, null, null, null)

      val future: Future[Either[String, NCorrectionResponse]] = scenarioService.getNCorrection(NCorrectionRequest(mr.leftProfile.globalCode, mr.rightProfile.globalCode, 4, 2))
      val result = Await.result(future, duration)

      result.isRight mustBe true
      result.right.get.dmp mustBe 0.75
      result.right.get.donnellyBaldwin mustBe 3
      result.right.get.n mustBe 2
    }

    "get N Correction dmp 2 and donnelly baldwin 1.5" in {
      val mockMatchingRepository = mock[MatchingRepository]
      val mr = MatchResult(MongoId("54eb50cc2cdc8a94c6ee794b"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-HIBA-500"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-B-IMBICE-500"),"tst-genetist",MatchStatus.pending,None),
        NewMatchingResult(ModerateStringency,
          Map("TH01" -> ModerateStringency, "CSF1PO" -> ModerateStringency, "D18S51" -> ModerateStringency, "AMEL" -> HighStringency, "D8S1179" -> ModerateStringency, "D3S1358" -> ModerateStringency, "D7S820" -> ModerateStringency, "FGA" -> ModerateStringency, "D16S539" -> HighStringency, "D13S317" -> ModerateStringency, "vWA" -> ModerateStringency, "D5S818" -> ModerateStringency, "TPOX" -> ModerateStringency, "D21S11" -> ModerateStringency),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI), 3)
      when(mockMatchingRepository.getByFiringAndMatchingProfile(any[SampleCode], any[SampleCode])).thenReturn(Future.successful(Some(mr)))

      val scenarioService =  new ScenarioServiceImpl(null, mockMatchingRepository, null, null, null)

      val future: Future[Either[String, NCorrectionResponse]] = scenarioService.getNCorrection(NCorrectionRequest(mr.leftProfile.globalCode, mr.rightProfile.globalCode, 4, 0.5))
      val result = Await.result(future, duration)

      result.isRight mustBe true
      result.right.get.dmp mustBe 2
      result.right.get.donnellyBaldwin mustBe 1.5
      result.right.get.n mustBe 3
    }

    "get N Correction dmp -8 and donnelly baldwin 0.375" in {
      val mockMatchingRepository = mock[MatchingRepository]
      val mr = MatchResult(MongoId("54eb50cc2cdc8a94c6ee794b"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-HIBA-500"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-B-IMBICE-500"),"tst-genetist",MatchStatus.pending,None),
        NewMatchingResult(ModerateStringency,
          Map("TH01" -> ModerateStringency, "CSF1PO" -> ModerateStringency, "D18S51" -> ModerateStringency, "AMEL" -> HighStringency, "D8S1179" -> ModerateStringency, "D3S1358" -> ModerateStringency, "D7S820" -> ModerateStringency, "FGA" -> ModerateStringency, "D16S539" -> HighStringency, "D13S317" -> ModerateStringency, "vWA" -> ModerateStringency, "D5S818" -> ModerateStringency, "TPOX" -> ModerateStringency, "D21S11" -> ModerateStringency),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI), 2)
      when(mockMatchingRepository.getByFiringAndMatchingProfile(any[SampleCode], any[SampleCode])).thenReturn(Future.successful(Some(mr)))

      val scenarioService =  new ScenarioServiceImpl(null, mockMatchingRepository, null, null, null)

      val future: Future[Either[String, NCorrectionResponse]] = scenarioService.getNCorrection(NCorrectionRequest(mr.leftProfile.globalCode, mr.rightProfile.globalCode, 4, 0.25))
      val result = Await.result(future, duration)

      result.isRight mustBe true
      result.right.get.dmp mustBe -8
      result.right.get.donnellyBaldwin mustBe 0.375
      result.right.get.n mustBe 2
    }

    "get N Correction retrieve an error" in {
      val mockMatchingRepository = mock[MatchingRepository]
      val mr = MatchResult(MongoId("54eb50cc2cdc8a94c6ee794b"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-HIBA-500"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-B-IMBICE-500"),"tst-genetist",MatchStatus.pending,None),
        NewMatchingResult(ModerateStringency,
          Map("TH01" -> ModerateStringency, "CSF1PO" -> ModerateStringency, "D18S51" -> ModerateStringency, "AMEL" -> HighStringency, "D8S1179" -> ModerateStringency, "D3S1358" -> ModerateStringency, "D7S820" -> ModerateStringency, "FGA" -> ModerateStringency, "D16S539" -> HighStringency, "D13S317" -> ModerateStringency, "vWA" -> ModerateStringency, "D5S818" -> ModerateStringency, "TPOX" -> ModerateStringency, "D21S11" -> ModerateStringency),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI), 4)
      when(mockMatchingRepository.getByFiringAndMatchingProfile(any[SampleCode], any[SampleCode])).thenReturn(Future.successful(Some(mr)))

      val scenarioService =  new ScenarioServiceImpl(null, mockMatchingRepository, null, null, null)

      val future: Future[Either[String, NCorrectionResponse]] = scenarioService.getNCorrection(NCorrectionRequest(mr.leftProfile.globalCode, mr.rightProfile.globalCode, 2, 2))
      val result = Await.result(future, duration)

      result.isLeft mustBe true
      result.left.get mustBe "E0120: La cantidad de individuos en la población de interés (2) tiene que ser mayor a la cantidad de perfiles evaluados (4)."
    }

    "get N Correction dmp infinity and donnelly baldwin very small" in {
      val mockMatchingRepository = mock[MatchingRepository]
      val mr = MatchResult(MongoId("54eb50cc2cdc8a94c6ee794b"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-HIBA-500"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-B-IMBICE-500"),"tst-genetist",MatchStatus.pending,None),
        NewMatchingResult(ModerateStringency,
          Map("TH01" -> ModerateStringency, "CSF1PO" -> ModerateStringency, "D18S51" -> ModerateStringency, "AMEL" -> HighStringency, "D8S1179" -> ModerateStringency, "D3S1358" -> ModerateStringency, "D7S820" -> ModerateStringency, "FGA" -> ModerateStringency, "D16S539" -> HighStringency, "D13S317" -> ModerateStringency, "vWA" -> ModerateStringency, "D5S818" -> ModerateStringency, "TPOX" -> ModerateStringency, "D21S11" -> ModerateStringency),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI), 1023)
      when(mockMatchingRepository.getByFiringAndMatchingProfile(any[SampleCode], any[SampleCode])).thenReturn(Future.successful(Some(mr)))

      val scenarioService =  new ScenarioServiceImpl(null, mockMatchingRepository, null, null, null)

      val future: Future[Either[String, NCorrectionResponse]] = scenarioService.getNCorrection(NCorrectionRequest(mr.leftProfile.globalCode, mr.rightProfile.globalCode, 10000, 0.000000000000000000000000000000001752691))
      val result = Await.result(future, duration)

      result.isRight mustBe true
      result.right.get.dmp.isInfinity mustBe true
      result.right.get.donnellyBaldwin mustBe 1.9522287299766068E-33
      result.right.get.n mustBe 1023
    }
  }
}
