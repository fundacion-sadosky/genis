package matching

import javax.inject.{Inject, Singleton}

import configdata.CategoryService
import play.api.Logger
import probability.{CalculationTypeService, LRMixCalculator, LRResult, PValueCalculator}
import profile.{Profile, ProfileService}
import scenarios.{CalculationScenario, Hypothesis, ScenarioService}
import stats.PopulationBaseFrequencyService
import types.{SampleCode, StatOption}

import scala.concurrent.{ExecutionContext, Future}

trait MatchingCalculatorService:
  def getLRByAlgorithm(
    firingProfile: Profile,
    matchingProfile: Profile,
    stats: StatOption,
    allelesRanges: Option[NewMatchingResult.AlleleMatchRange] = None
  ): Future[Option[LRResult]]
  def createDefaultScenario(sample: Profile, profile: Profile, stats: StatOption): scenarios.CalculationScenario

@Singleton
class MatchingCalculatorServiceImpl @Inject()(
  scenarioService: ScenarioService,
  categoryService: CategoryService,
  profileService: ProfileService,
  calculationTypeService: CalculationTypeService,
  populationBaseFrequencyService: PopulationBaseFrequencyService
)(using ec: ExecutionContext) extends MatchingCalculatorService:

  val logger: Logger = Logger(this.getClass)

  // Structural change: legacy determineProfiles was sync (listCategories was sync in legacy).
  // Modern categoryService.listCategories is async → returns Future.
  private def determineProfiles(
    firingProfile: Profile,
    matchingProfile: Profile
  ): Future[(Profile, Profile, Boolean)] =
    val firingContributors   = firingProfile.contributors.getOrElse(1)
    val matchingContributors = matchingProfile.contributors.getOrElse(1)
    if firingContributors != matchingContributors then
      val (profile, sample) =
        if firingContributors > matchingContributors then (matchingProfile, firingProfile)
        else (firingProfile, matchingProfile)
      Future.successful((profile, sample, false))
    else
      categoryService.listCategories.map { categories =>
        val firingIsRef   = categories.get(firingProfile.categoryId).exists(_.isReference)
        val matchingIsRef = categories.get(matchingProfile.categoryId).exists(_.isReference)
        (firingIsRef, matchingIsRef) match
          case (true, false)  => (firingProfile, matchingProfile, false)
          case (false, true)  => (matchingProfile, firingProfile, false)
          case _              => (firingProfile, matchingProfile, true)
      }

  def createDefaultScenario(sample: Profile, profile: Profile, stats: StatOption): CalculationScenario =
    val sampleContributors = sample.contributors.getOrElse(1)
    val associations       = profileService.validProfilesAssociated(sample.labeledGenotypification)
    var prosecutor = Hypothesis(List(profile.globalCode), List(), sampleContributors - 1, stats.dropOut.getOrElse(0.0))
    var defense    = Hypothesis(List(), List(profile.globalCode), sampleContributors, stats.dropOut.getOrElse(0.0))
    if sampleContributors > 1 && associations.nonEmpty then
      val associated = SampleCode(associations.head)
      prosecutor = Hypothesis(List(associated, profile.globalCode), List(), sampleContributors - 2, stats.dropOut.getOrElse(0.0))
      defense    = Hypothesis(List(associated), List(profile.globalCode), sampleContributors - 1, stats.dropOut.getOrElse(0.0))
    val isMixMix = sampleContributors >= 2 && profile.contributors.getOrElse(1) >= 2
    CalculationScenario(sample.globalCode, prosecutor, defense, stats, Some(List(sample, profile)), isMixMix)

  override def getLRByAlgorithm(
    firingProfile: Profile,
    matchingProfile: Profile,
    stats: StatOption,
    allelesRanges: Option[NewMatchingResult.AlleleMatchRange] = None
  ): Future[Option[LRResult]] =
    determineProfiles(firingProfile, matchingProfile).flatMap { case (profile1, profile2, shouldAvg) =>
      if shouldAvg then
        for
          r1 <- doGetLR(profile1, profile2, stats, allelesRanges)
          r2 <- doGetLR(profile2, profile1, stats, allelesRanges)
        yield avgLr(r1, r2)
      else
        doGetLR(profile1, profile2, stats, allelesRanges)
    }

  private def doGetLR(
    firingProfile: Profile,
    matchingProfile: Profile,
    stats: StatOption,
    allelesRanges: Option[NewMatchingResult.AlleleMatchRange]
  ): Future[Option[LRResult]] =
    val firingContributors   = firingProfile.contributors.getOrElse(1)
    val matchingContributors = matchingProfile.contributors.getOrElse(1)
    if firingContributors >= 2 && matchingContributors >= 2 then
      getMixtureMixtureLR(firingProfile, matchingProfile, stats, allelesRanges)
    else if firingContributors > 1 && matchingContributors > 1 then
      Future.successful(None)
    else
      scenarioService.getLRMix(createDefaultScenario(firingProfile, matchingProfile, stats), allelesRanges)

  private def getMixtureMixtureLR(
    firingProfile: Profile,
    matchingProfile: Profile,
    stats: StatOption,
    allelesRanges: Option[NewMatchingResult.AlleleMatchRange]
  ): Future[Option[LRResult]] =
    val calculation = LRMixCalculator.name
    for
      at       <- calculationTypeService.getAnalysisTypeByCalculation(calculation)
      assoc1   <- profileService.getAssociatedProfiles(firingProfile).map(_.map(_.genotypification.getOrElse(at.id, Map.empty)))
      assoc2   <- profileService.getAssociatedProfiles(matchingProfile).map(_.map(_.genotypification.getOrElse(at.id, Map.empty)))
      freqData <- populationBaseFrequencyService.getByName(stats.frequencyTable).map(_.get)
    yield
      val frequencyTable = PValueCalculator.parseFrequencyTable(freqData)
      Some(LRMixCalculator.lrMixMixNuevo(
        firingProfile.genotypification.getOrElse(at.id, Map.empty), assoc1.headOption,
        matchingProfile.genotypification.getOrElse(at.id, Map.empty), assoc2.headOption,
        frequencyTable,
        firingProfile.contributors.getOrElse(1),
        matchingProfile.contributors.getOrElse(1),
        allelesRanges,
        stats.theta,
        stats.dropIn,
        stats.dropOut.getOrElse(0.0)
      ))

  private def avgLr(r1: Option[LRResult], r2: Option[LRResult]): Option[LRResult] =
    (r1, r2) match
      case (Some(a), Some(b)) if a.total > 0 && b.total > 0 =>
        val markers = a.detailed.keySet.union(b.detailed.keySet)
        val avgDetailed = markers.map { marker =>
          val va = a.detailed.getOrElse(marker, None).getOrElse(0.0)
          val vb = b.detailed.getOrElse(marker, None).getOrElse(0.0)
          marker -> Some((va + vb) / 2).asInstanceOf[Option[Double]]
        }.toMap
        Some(LRResult((a.total + b.total) / 2, avgDetailed))
      case (Some(a), Some(_)) if a.total == 0 => r2
      case (Some(_), Some(b)) if b.total == 0 => r1
      case (Some(_), None)                     => r1
      case (None, Some(_))                     => r2
      case _                                   => r1