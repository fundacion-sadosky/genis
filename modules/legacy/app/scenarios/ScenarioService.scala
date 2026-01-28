package scenarios

import types.{MongoId, SampleCode}

import scala.concurrent.Future
import javax.inject.Inject

import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject.Singleton

import matching._
import probability._
import profile.{Profile, ProfileService}
import stats.PopulationBaseFrequencyService

import play.api.i18n.Messages

abstract class ScenarioService {

  def getLRMix(scenario: CalculationScenario,allelesRanges:Option[NewMatchingResult.AlleleMatchRange] = None): Future[Option[LRResult]]
  def findExistingMatches(scenario: Scenario): Future[Seq[ScenarioOption]]
  def findMatchesForScenario(userId: String, globalCode: SampleCode, isSuperUser: Boolean): Future[Seq[ScenarioOption]]
  def findMatchesForRestrainedScenario(userId: String, firingCode: SampleCode, matchingCode: SampleCode, isSuperUser: Boolean): Future[Seq[ScenarioOption]]
  def search(userId: String, scenarioSearch: ScenarioSearch, isSuperUser: Boolean) : Future[Seq[Scenario]]
  def add(scenario: Scenario) : Future[Either[String, String]]
  def delete(userId: String, id: MongoId, isSuperUser: Boolean) : Future[Either[String, String]]
  def get(userId: String, id: MongoId, isSuperUser: Boolean) : Future[Option[Scenario]]
  def validate(userId: String, scenario: Scenario, isSuperUser: Boolean) : Future[Either[String, String]]
  def update(userId: String, scenario: Scenario, isSuperUser: Boolean) : Future[Either[String, String]]
  def getNCorrection(request: NCorrectionRequest) : Future[Either[String, NCorrectionResponse]]
}

@Singleton
class ScenarioServiceImpl @Inject() (
    profileService: ProfileService,
    matchingRepository: MatchingRepository,
    populationBaseFrequencyService: PopulationBaseFrequencyService,
    scenarioRepository: ScenarioRepository,
    calculationTypeService: CalculationTypeService
  ) extends ScenarioService {

  val calculation = LRMixCalculator.name

  override def getLRMix(
    scenario: CalculationScenario,
    allelesRanges: Option[NewMatchingResult.AlleleMatchRange] = None
  ): Future[Option[LRResult]] = {
    val frequencyTableFuture = populationBaseFrequencyService
      .getByName(scenario.stats.frequencyTable)
      .map {
        maybeFrequencyTable => PValueCalculator.parseFrequencyTable(maybeFrequencyTable.get)
      }
    val fullScenarioFuture = convertFullScenario(scenario)
    for {
      frequencyTable <- frequencyTableFuture
      fullScenario <- fullScenarioFuture
      lr <- LRMixCalculator.calculateLRMix(fullScenario, frequencyTable, allelesRanges)
    } yield {
      Some(lr)
    }
  }

  private def convertFullScenario(scenario: CalculationScenario): Future[FullCalculationScenario] = {

    val sampleFut = calculationTypeService.filterCodes(List(scenario.sample), calculation,scenario.profiles.getOrElse(Nil)) map { profiles => profiles.head }
    val selectedPFut = calculationTypeService.filterCodes(scenario.prosecutor.selected, calculation,scenario.profiles.getOrElse(Nil))
    val unselectedPFut = calculationTypeService.filterCodes(scenario.prosecutor.unselected, calculation,scenario.profiles.getOrElse(Nil))
    val selectedDFut = calculationTypeService.filterCodes(scenario.defense.selected, calculation,scenario.profiles.getOrElse(Nil))
    val unselectedDFut = calculationTypeService.filterCodes(scenario.defense.unselected, calculation,scenario.profiles.getOrElse(Nil))

    for {
      sample <- sampleFut
      selectedP <- selectedPFut
      unselectedP <- unselectedPFut
      selectedD <- selectedDFut
      unselectedD <- unselectedDFut
    } yield {
      FullCalculationScenario(
        sample,
        FullHypothesis(selectedP.toArray, unselectedP.toArray, scenario.prosecutor.unknowns, scenario.prosecutor.dropOut),
        FullHypothesis(selectedD.toArray, unselectedD.toArray, scenario.defense.unknowns, scenario.defense.dropOut),
        scenario.stats)
    }
  }

  override def findExistingMatches(scenario: Scenario): Future[Seq[ScenarioOption]] = {
    profileService.findByCode(scenario.calculationScenario.sample) flatMap { firingProfile =>
      val profiles = scenario.calculationScenario.prosecutor.selected ++ scenario.calculationScenario.prosecutor.unselected
      profilesToScenarioOptions(firingProfile.get, profiles, false)
    }

  }

  private def filterRepeatedOptions(list: Seq[ScenarioOption]): Seq[ScenarioOption] = {
    var result = List.empty[ScenarioOption]
    list.foreach(so => if (!result.exists(_.globalCode == so.globalCode)) result = result ++ List(so))
    result
  }

  override def findMatchesForScenario(userId: String, globalCode: SampleCode, isSuperUser: Boolean): Future[Seq[ScenarioOption]] = {
    val notDiscardedFut = getOptionsForNotDiscardedMatches(userId, globalCode, isSuperUser)
    val associatedFut = getOptionsForAssociations(userId, globalCode, isSuperUser)
    for {
      notDiscarded <- notDiscardedFut
      associated <- associatedFut
    } yield {
      filterRepeatedOptions(notDiscarded ++ associated)
    }
  }

  private def getOptionsForNotDiscardedMatches(userId: String, globalCode: SampleCode, isSuperUser: Boolean) = {
      matchingRepository.matchesNotDiscarded(globalCode) flatMap { sMr =>
        calculationTypeService.filterMatchResults(sMr, LRMixCalculator.name) flatMap { matchResults =>
          if (isSuperUser || matchResults.forall(isMatchResultFromUser(_, globalCode, userId))) {
            val scenarioOptions = matchResultsToScenarioOptions(globalCode, matchResults)
            scenarioOptions.map { seq => seq.filter(mr => mr.contributors == 1) }
          } else {
            Future.successful(List())
          }
        }
    }
  }

  private def getOptionsForAssociations(userId: String, globalCode: SampleCode, isSuperUser: Boolean): Future[Seq[ScenarioOption]] = {
    profileService.findByCode(globalCode) flatMap { profileOpt =>
      val firingProfile = profileOpt.get

      if (isSuperUser || firingProfile.assignee == userId) {
        val associations = profileService.validProfilesAssociated(firingProfile.labeledGenotypification).map(SampleCode(_))
        profilesToScenarioOptions(firingProfile, associations, true)
      } else {
        Future.successful(Seq())
      }

    }
  }

  private def profilesToScenarioOptions(firingProfile: Profile, codes: Seq[SampleCode], association: Boolean) = {
    val fut = codes.map { code =>
      for {
        opt <- profileService.findByCode(code)
        at <- calculationTypeService.getAnalysisTypeByCalculation(calculation)
        dropOuts <- calculateDropOuts(firingProfile, opt.get)
      } yield {
        val matchingProfile = opt.get
        val (leftPonderation, rightPonderation) =
          MatchingAlgorithm.sharedAllelePonderation(firingProfile.genotypification.getOrElse(at.id, Map.empty), matchingProfile.genotypification.getOrElse(at.id, Map.empty))

        ScenarioOption(matchingProfile.globalCode, matchingProfile.internalSampleCode, matchingProfile.categoryId,
          if(!firingProfile.isReference && matchingProfile.isReference) rightPonderation else leftPonderation,
          matchingProfile.contributors.getOrElse(1), dropOuts, association)
      }
    }
    Future.sequence(fut)
  }

  def findMatchesForRestrainedScenario(userId: String, firingCode: SampleCode, matchingCode: SampleCode, isSuperUser: Boolean): Future[Seq[ScenarioOption]] = {
    matchingRepository.getByFiringAndMatchingProfile(firingCode, matchingCode) flatMap {
      case Some(mr) => {
        if (isSuperUser || isMatchResultFromUser(mr, matchingCode, userId)) {
          matchingRepository.matchesWithFullHit(firingCode) flatMap { matchResults =>
            for {
              result1 <- matchResultsToScenarioOptions(firingCode, Seq(mr))
              result2 <- matchResultsToScenarioOptions(firingCode, matchResults, true)
              associations <- getOptionsForAssociations(userId, firingCode, true)
            } yield {
              filterRepeatedOptions(result1 ++ result2 ++ associations)
            }
          }
        } else {
          Future.successful(List())
        }
      }
      case None => Future.successful(List())
    }

  }

  private def isMatchResultFromUser(mr: MatchResult, code: SampleCode, userId: String) = {
    val isRightProfile = mr.rightProfile.globalCode == code
    val profile = if (isRightProfile) mr.rightProfile else mr.leftProfile
    profile.assignee == userId
  }

  private def matchResultsToScenarioOptions(firingCode: SampleCode, results: Seq[MatchResult], onlyReferences: Boolean = false) = {
    val fut = results.map { mr =>
      val right = firingCode == mr.rightProfile.globalCode

      val matchingCode = if (right)
        mr.leftProfile.globalCode
      else
        mr.rightProfile.globalCode

      val a: Future[Option[ScenarioOption]] = for {
        firingProfileOpt <- profileService.findByCode(firingCode)
        matchingProfileOpt <- profileService.findByCode(matchingCode)
        dropOuts <- calculateDropOuts(firingProfileOpt.get, matchingProfileOpt.get)
      } yield {
        val firingProfile = firingProfileOpt.get
        val matchingProfile = matchingProfileOpt.get

        if ((onlyReferences && matchingProfile.isReference) || !onlyReferences) {
          Some(ScenarioOption(matchingProfile.globalCode, matchingProfile.internalSampleCode, matchingProfile.categoryId,
            MatchingAlgorithm.uniquePonderation(mr, firingProfile, matchingProfile),
            matchingProfile.contributors.getOrElse(1), dropOuts, false))
        } else {
          None
        }
      }
      a
    }
    Future.sequence(fut).map { result => result.flatten }
  }

  def calculateDropOuts(firingProfile: Profile, matchingProfile: Profile) = {
    calculationTypeService.getAnalysisTypeByCalculation(calculation) map { at =>
      val leftGenotype = firingProfile.genotypification.getOrElse(at.id, Map.empty)
      val rightGenotype = matchingProfile.genotypification.getOrElse(at.id, Map.empty)

      val sharedMarkers: Set[Profile.Marker] = leftGenotype.keySet.intersect(rightGenotype.keySet)

      sharedMarkers.foldLeft(0)((prev, marker) => {
        val rightAlleles = rightGenotype.get(marker).get
        val leftAlleles = leftGenotype.get(marker).get
        prev + rightAlleles.toSet.diff(leftAlleles.toSet).size
      })
    }
  }

  override def search(userId: String, scenarioSearch: ScenarioSearch, isSuperUser: Boolean) : Future[Seq[Scenario]] = {
    scenarioRepository.get(userId, scenarioSearch, isSuperUser)
  }

  override def add(scenario: Scenario) : Future[Either[String, String]] = {
    scenarioRepository.add(scenario)
  }

  override def delete(userId: String, id: MongoId, isSuperUser: Boolean) : Future[Either[String, String]] = {
    scenarioRepository.delete(userId, id, isSuperUser)
  }

  override def get(userId: String, id: MongoId, isSuperUser: Boolean) : Future[Option[Scenario]] = {
    scenarioRepository.get(userId, id, isSuperUser)
  }

  override def validate(userId: String, scenario: Scenario, isSuperUser: Boolean) : Future[Either[String, String]] = {
    scenarioRepository.validate(userId, scenario, isSuperUser)
  }

  override def update(userId: String, scenario: Scenario, isSuperUser: Boolean) : Future[Either[String, String]] = {
    scenarioRepository.update(userId, scenario, isSuperUser)
  }

  override def getNCorrection(request: NCorrectionRequest) : Future[Either[String, NCorrectionResponse]] = {
    matchingRepository.getByFiringAndMatchingProfile(request.firingCode, request.matchingCode).map { mr =>
      val n = mr.get.n
      if (request.bigN > n) {
        val dmp = 1 - Math.pow(1 - (1 / request.lr), n)
        val donnellyBaldwin = (1 + ((n - 1.0) / (request.bigN - n))) * request.lr

        Right(NCorrectionResponse(dmp, donnellyBaldwin, n))
      } else {
        Left(Messages("error.E0120", request.bigN, n))
      }
    }
  }
}
