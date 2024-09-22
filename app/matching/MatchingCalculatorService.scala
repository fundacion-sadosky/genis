package matching

import java.math.MathContext
import configdata.CategoryService

import scala.math.BigDecimal.RoundingMode
import javax.inject.{Inject, Named, Singleton}
import laboratories.LaboratoryService
import org.codehaus.jackson.map.ext.CoreXMLDeserializers.DurationDeserializer
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import probability.{MixtureLRCalculator, _}
import profile.{Profile, ProfileService}
import profiledata.ProfileDataService
import scenarios.{CalculationScenario, Hypothesis, ScenarioService}
import stats.{PopulationBaseFrequency, PopulationBaseFrequencyService}
import types.{SampleCode, StatOption}

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}
import kits.{AnalysisType, LocusService}
import profile.Profile.Genotypification
abstract class MatchingCalculatorService {
  def getLRByAlgorithm(firingProfile: Profile, matchingProfile: Profile, stats: StatOption,allelesRanges:Option[NewMatchingResult.AlleleMatchRange] = None): Future[Option[LRResult]]
  def doGetLRByAlgorithm(firingProfile: Profile, matchingProfile: Profile, stats: StatOption,allelesRanges:Option[NewMatchingResult.AlleleMatchRange] = None): Future[Option[LRResult]]
  def getDefaultLRForMachingId(firingProfile: Profile, matchingGlobalCode: String,idMatching:String,allelesRanges:Option[NewMatchingResult.AlleleMatchRange] = None): Future[LRCalculation]
  def updateMatchesLR(matchingLRs: Set[(String, Double)]):Future[Unit]
  def createDefaultScenario(sample: Profile, profile: Profile, stats: StatOption): CalculationScenario
  def updateLrDefault(): Unit
}

@Singleton
class MatchingCalculatorServiceImpl @Inject() (
     matchingService: MatchingService,//No se usa
     profileService: ProfileService,
     populationBaseFrequencyService: PopulationBaseFrequencyService,
     profileDataService: ProfileDataService,//No se usa
     laboratoryService: LaboratoryService,//No se usa
     scenarioService: ScenarioService,
     calculationTypeService: CalculationTypeService,
     probabilityService:ProbabilityService  = null,
     @Named("labCode") val currentInstanceLabCode: String = null,
     @Named("updateLr") val updateLr: Boolean = false,
     locusService:LocusService = null,
     categoryService: CategoryService = null) extends MatchingCalculatorService {

  val logger = Logger(this.getClass())

  val calculation = MixtureLRCalculator.name

  override def updateLrDefault(): Unit = Future{
    if(updateLr){
      logger.error("#### updateLr true ####")
      val mc = MatchCardSearch("tst-admin",true,0)
      val totalMatches =  Await.result(matchingService.getAllTotalMatches(mc),Duration.Inf)
      val totalPages = (BigDecimal.valueOf(totalMatches) / BigDecimal.valueOf(mc.pageSize)).setScale(0, RoundingMode.FLOOR).intValue();
      val statsOpt = Await.result(probabilityService.getStats(currentInstanceLabCode),Duration.Inf)
      val listLocus = Await.result(locusService.list(), Duration.Inf)
      val allelesRangesDefault: NewMatchingResult.AlleleMatchRange = listLocus.map(locus => locus.id -> AleleRange(locus.minAlleleValue.getOrElse(0), locus.maxAlleleValue.getOrElse(99))).toMap

      if(statsOpt.isDefined) {
        val stats = statsOpt.get

        logger.error(s"#### total matches ${totalMatches} ####")
        for (i <- 0 to totalPages) {

          val matchResults: Seq[MatchResult] = Await.result(matchingService.getMatchesPaginated(mc.copy(page = i)), Duration.Inf)
          val updates = matchResults.map(matchResult => {
            if(matchResult.`type`==4){
              None
            }else{
              try{
                val firingProfileOpt = Await.result(profileService.get(matchResult.leftProfile.globalCode), Duration.Inf)
                val matchingProfileOpt = Await.result(profileService.get(matchResult.rightProfile.globalCode), Duration.Inf)
                (firingProfileOpt,matchingProfileOpt) match {
                  case (Some(firingProfile),Some(matchingProfile)) =>{
                    val lr = Await.result(getLRByAlgorithm(firingProfile, matchingProfile, stats, Some(matchResult.result.allelesRanges.getOrElse(allelesRangesDefault))).map(_.map(_.total)), Duration.Inf)
                    Some((matchResult._id.id, lr.getOrElse(0.0)))
                  }
                  case _ => None
                }

              }catch {
                case e:Exception => {
                  logger.error(e.getMessage,e)
                  None
                }
              }
            }
          }).flatten
          this.matchingService.updateMatchesLR(updates.toSet)
        }
        logger.error(s"######## DEFAULT LR CALCULATION COMPETED ########")
      }
    }else{
      logger.info("#### updateLr false ####")
    }
    ()
  }

  override def updateMatchesLR(matchingLRs: Set[(String, Double)]):Future[Unit] = {
    this.matchingService.updateMatchesLR(matchingLRs)
  }
  override def getDefaultLRForMachingId(
    firingProfile: Profile,
    matchingGlobalCode: String,
    idMatching:String,
    allelesRanges:Option[NewMatchingResult.AlleleMatchRange] = None
  ): Future[LRCalculation] = {
    val lrDefault = LRCalculation(idMatching, 0.0, 0.0)
    probabilityService
      .getStats(currentInstanceLabCode)
      .flatMap{
        case Some(stats) =>
          profileService
            .get(SampleCode(matchingGlobalCode))
            .flatMap{
              case Some(matchingProfile) => {
    //              getLRByAlgorithm(firingProfile,matchingProfile,stats.get,allelesRanges).map(_.map(_.total).getOrElse(0.0)).flatMap(lr => {
    //                getLRByAlgorithm(matchingProfile,firingProfile,stats.get,allelesRanges).map(_.map(_.total).getOrElse(0.0)).map(lrRight => {
    //                  LRCalculation(idMatching,lr,lrRight)
    //                })
                getLRByAlgorithm(firingProfile, matchingProfile, stats, allelesRanges)
                  .map(_.map(_.total).getOrElse(0.0))
                  .map(lr => { LRCalculation(idMatching, lr, lr) })
              }
              case None => Future.successful(lrDefault)
          }
        case None => Future.successful(lrDefault)
      }
  }
  def determineProfiles(firingProfile: Profile, matchingProfile: Profile):(Profile, Profile, Boolean) = {
    var sample = firingProfile
    var profile = matchingProfile
    val firingContributors = firingProfile.contributors.getOrElse(1)
    val matchingContributors = matchingProfile.contributors.getOrElse(1)

    if (firingContributors != matchingContributors) {
      sample = if (firingContributors > matchingContributors) firingProfile else matchingProfile
      profile = if (firingContributors > matchingContributors) matchingProfile else firingProfile
      (profile, sample, false)
    } else {
      val categories = categoryService.listCategories
      val firingCategoryIsReference = categories.get(firingProfile.categoryId).map(_.isReference).contains(true)
      val matchingCategoryIsReference = categories.get(matchingProfile.categoryId).map(_.isReference).contains(true)

      (firingCategoryIsReference:Boolean, matchingCategoryIsReference) match {
        case (true,false) => (firingProfile, matchingProfile, false)
        case (false,true) => (matchingProfile, firingProfile, false)
        case (_,_) => (firingProfile, matchingProfile, true)
      }
    }
  }
  private def avgDetailed(
    result1: Map[Profile.Marker, Option[Double]],
    result2: Map[Profile.Marker, Option[Double]]
  ):Map[Profile.Marker, Option[Double]] ={
    val markers = result1.keySet.union(result2.keySet)
    markers
      .map(
        marker => {
          val a = result1.getOrElse(marker, None).getOrElse(0.0)
          val b = result2.getOrElse(marker, None).getOrElse(0.0)
          val sum = a + b
          val avg = Some(sum/2).asInstanceOf[Option[Double]]
          (marker, avg)
        }
      )
      .toMap
  }

  def avgLr(result1: Option[LRResult], result2: Option[LRResult]):Option[LRResult] = {
    (result1, result2) match {
      case (Some(r1), Some(r2)) if r1.total > 0 && r2.total > 0 =>
        Some(LRResult((r1.total + r2.total) / 2, avgDetailed(r1.detailed, r2.detailed)))
      case (Some(r1), Some(r2)) if r1.total == 0 && r2.total == 0 =>
        None
      case (Some(r1), Some(_)) if r1.total == 0 =>
        result2
      case (Some(_), Some(r2)) if r2.total == 0 =>
        result1
      case (Some(_), None) => result1
      case (None, Some(_)) => result2
      case (None, None) => result1
    }
  }
  override def getLRByAlgorithm(
    firingProfile: Profile,
    matchingProfile: Profile,
    stats: StatOption,
    allelesRanges:Option[NewMatchingResult.AlleleMatchRange] = None
  ): Future[Option[LRResult]] = {
    val (profile1, profile2, shouldAvg) = determineProfiles(firingProfile, matchingProfile)
    if (shouldAvg) {
      for {
        result1 <- this.doGetLRByAlgorithm(profile1, profile2, stats, allelesRanges)
        result2 <- this.doGetLRByAlgorithm(profile2, profile1, stats, allelesRanges)
      }
        yield this.avgLr(result1, result2)
    } else {
      this.doGetLRByAlgorithm(profile1, profile2, stats, allelesRanges)
    }
  }
  def doGetLRByAlgorithm(
    firingProfile: Profile,
    matchingProfile: Profile,
    stats: StatOption,
    allelesRanges: Option[NewMatchingResult.AlleleMatchRange] = None
  ): Future[Option[LRResult]] = {
    val firingContributors = firingProfile.contributors.getOrElse(1)
    val matchingContributors = matchingProfile.contributors.getOrElse(1)
    if (firingContributors >= 2 && matchingContributors >= 2) {
      // Q(n>=2) y P(n>=2) -> Mezcla-Mezcla
      this.getMixtureMixtureLRByLocus(
        firingProfile.globalCode,
        matchingProfile.globalCode,
        stats,
        firingProfile,
        matchingProfile,
        allelesRanges
      )

    } else if (firingContributors.equals(1) && matchingContributors.equals(1)) {
      // Q(n=1) y P(n=1) -> Enfsi
      scenarioService.getLRMix(
        createDefaultScenario(firingProfile, matchingProfile, stats),
        allelesRanges
      )
      //this.getLRByLocus(firingProfile.globalCode, matchingProfile.globalCode, stats)

    } else if (firingContributors > 1 && matchingContributors > 1) {
      // This case is never reached.
      // Q(n>1) y P(n>1) -> -
      Future.successful(None)
    }
    else {
      // Q(n>1) y P(n=1) o Q(n=1) y P(n>1) -> LRMix
      scenarioService.getLRMix(
        createDefaultScenario(firingProfile, matchingProfile, stats),
        allelesRanges
      )
    }
  }

  private def filterByCodes(codes: List[SampleCode], profiles:List[Profile]=Nil): Future[Seq[Profile]] ={
    val profilesToAdd = profiles.filter(p=>codes.contains(p.globalCode))
    if(profilesToAdd.map(_.globalCode).containsSlice(codes)){
      Future.successful(profilesToAdd)
    }else{
      profileService.findByCodes(codes)
    }
  }
  def createDefaultScenario(firingProfile: Profile, matchingProfile: Profile, stats: StatOption): CalculationScenario = {
    var sample = firingProfile
    var profile = matchingProfile
    var (p,s,sameGroupCategory) = determineProfiles(firingProfile,matchingProfile)
    if(!sameGroupCategory){
      sample = s
      profile = p
    }

    val sampleContributors = sample.contributors.getOrElse(1)
    val profileContributors = profile.contributors.getOrElse(1)

    var prosecutor = Hypothesis(List(profile.globalCode), List(), sampleContributors - 1, stats.dropOut.get)
    var defense = Hypothesis(List(), List(profile.globalCode), sampleContributors, stats.dropOut.get)

    if (sampleContributors > 1) {
      val associations = profileService.validProfilesAssociated(sample.labeledGenotypification)
      if (associations.nonEmpty) {
        val associated = SampleCode(associations.head)
        prosecutor = Hypothesis(List(associated, profile.globalCode), List(), sampleContributors - 2, stats.dropOut.get)
        defense = Hypothesis(List(associated), List(profile.globalCode), sampleContributors - 1, stats.dropOut.get)
      }
    }
    var isMixMix = false
    if (sampleContributors >= 2 && profileContributors >= 2) {
      isMixMix = true
    }

    CalculationScenario(sample.globalCode, prosecutor, defense, stats,Some(List(firingProfile,matchingProfile)), isMixMix)
  }

  /*private def getRMPByLocus(firingGlobalCode: SampleCode, matchingProfileCode: SampleCode, stats: StatOption) = {

    lazy val calculationModel = getCalculationModel(stats.theta)
    val model = calculationModel(ProbabilityModel.withName(stats.probabilityModel))

    val frequencyTableFuture = populationBaseFrequencyService.getByName(stats.frequencyTable).map { maybeFrequencyTable =>
      PValueCalculator.parseFrequencyTable(maybeFrequencyTable.get)
    }

    val resultFuture = matchingService.getByFiringAndMatchingProfile(firingGlobalCode, matchingProfileCode)

    val futureOfFutureOfResult = frequencyTableFuture.flatMap { frequencyTable =>
      val calculator = PValueCalculator.autosomalRMP(frequencyTable)(model) _

      val emptyMap: Map[Profile.Marker, Option[Double]] = Map()

      resultFuture flatMap {
        case Some(result) => profileService.findByCode(result.globalCode) map {
          case Some(profile) => calculator(result.matchingAlleles.keySet, profile)
          case None => emptyMap
        }
        case None => Future.successful(emptyMap)
      }

    }

    futureOfFutureOfResult
  }*/


  private def getTotalRmpByGlobacode(rmpByLocus: Map[Profile.Marker, Option[Double]]) = {
    rmpByLocus.foldLeft(1.0: Double) { (prev, m) =>
      if (m._2.isDefined && (m._2.get.isNaN || m._2.get == 0))  prev
      else prev * m._2.getOrElse(1.0)
    }
  }

  /*def getLRByLocus(firingGlobalCode: SampleCode, matchingProfileCode: SampleCode, stats: StatOption) = {

    val resFutRmp = getRMPByLocus(firingGlobalCode, matchingProfileCode, stats)

    val resFutLr = resFutRmp.map { rmpByGlobalcodeMap =>
      rmpByGlobalcodeMap.map {
        case (marker, rmp) =>
          val lr = if (rmp.isDefined) Some(1.0 / rmp.get) else None
          marker -> lr
      }
    }
    val totalFut = resFutLr.map(getTotalRmpByGlobacode(_))

    for {
      detailedMap <- resFutLr
      totalMap <- totalFut
    } yield {
      LRResult(totalMap, detailedMap)
    }

  }
*/

  def getMixtureMixtureLRByLocus(
    mixture1: SampleCode,
    mixture2: SampleCode,
    stats: StatOption,
    firingProfile:Profile,
    matchingProfile:Profile,
    allelesRanges:Option[NewMatchingResult.AlleleMatchRange] = None
  ): Future[Some[LRResult]] = {
    val f: Future[(
      AnalysisType,
      Seq[Profile],
      PopulationBaseFrequency,
      Seq[Genotypification],
      Seq[Genotypification]
    )] = for {
      analysisType <- calculationTypeService
        .getAnalysisTypeByCalculation(calculation)
      lProfiles <- this.filterByCodes(
        List(mixture1, mixture2),
        List(firingProfile,matchingProfile)
      )
      asocProfMix1 <- profileService
        .getAssociatedProfiles( lProfiles(0) ) map {
          profiles => profiles.map(_.genotypification.getOrElse(analysisType.id, Map.empty))
        }
      asocProfMix2 <- profileService
        .getAssociatedProfiles( lProfiles(1) ) map {
          profiles => profiles.map(_.genotypification.getOrElse(analysisType.id, Map.empty))
        }
      maybeFrequencyTable <- populationBaseFrequencyService.getByName(stats.frequencyTable)
    } yield (analysisType, lProfiles, maybeFrequencyTable.get, asocProfMix1, asocProfMix2)
    f.map {
      case (analysisType, mixtureProfiles, frequencyTable, asocProfMix1, asocProfMix2) =>
/*
        val res = MixtureLRCalculator.lrMixMix(
          mixtureProfiles(0).genotypification.getOrElse(analysisType.id, Map.empty), asocProfMix1.headOption,
          mixtureProfiles(1).genotypification.getOrElse(analysisType.id, Map.empty), asocProfMix2.headOption,
          PValueCalculator.parseFrequencyTable(frequencyTable),
          getCalculationModel(frequencyTable.theta)(ProbabilityModel.withName(stats.probabilityModel)))
        val total = getTotalRmpByGlobacode(res)
*/
        val lrResult = LRMixCalculator.lrMixMixNuevo(
          mixtureProfiles(0).genotypification.getOrElse(analysisType.id, Map.empty),
          asocProfMix1.headOption,
          mixtureProfiles(1).genotypification.getOrElse(analysisType.id, Map.empty),
          asocProfMix2.headOption,
          PValueCalculator.parseFrequencyTable(frequencyTable),
          firingProfile.contributors.getOrElse(1),
          matchingProfile.contributors.getOrElse(1),
          allelesRanges,
          stats.theta,
          stats.dropIn,
          stats.dropOut.getOrElse(0.0)
        )
//        Some(LRResult(total, res))
        Some(lrResult)

    }
  }


  private def getCalculationModel(theta: Double) = {
    Map(ProbabilityModel.HardyWeinberg -> new probability.HardyWeinbergCalculationProbability,
      ProbabilityModel.NRCII41 -> new probability.NRCII41CalculationProbability(theta),
      ProbabilityModel.NRCII410 -> new probability.NRCII410CalculationProbability(theta))
  }

}