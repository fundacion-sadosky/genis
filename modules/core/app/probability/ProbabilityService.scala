package probability

import javax.inject.{Inject, Singleton}

import configdata.{CategoryService, FullCategory}
import kits.{QualityParamsProvider, StrKit, StrKitService}
import play.api.Logger
import probability.PValueCalculator.FrequencyTable
import profile.{Allele, Analysis}
import profile.Profile.Genotypification
import profiledata.ProfileDataRepository
import services.LaboratoryService
import stats.PopulationBaseFrequencyService
import types.{AlphanumericId, StatOption}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

trait ProbabilityService {
  def getStats(laboratory: String): Future[Option[StatOption]]
  def calculateContributors(analysis: profile.Analysis, category: types.AlphanumericId, stats: StatOption): Future[Int]
}

@Singleton
class ProbabilityServiceImpl @Inject()(
  populationBaseFrequencyService: PopulationBaseFrequencyService,
  profileDataRepository: ProfileDataRepository,
  laboratoryService: LaboratoryService,
  kitService: StrKitService,
  paramsProvider: QualityParamsProvider,
  categoryService: CategoryService
)(implicit ec: ExecutionContext) extends ProbabilityService {

  val logger: Logger = Logger(this.getClass())

  // Structural change: categoryService.getCategory is async in modern (was sync in legacy).
  // The method now returns Future[Int] via a for-comprehension.
  private def getMaxOverageDeviatedLoci(analysis: Analysis, catId: AlphanumericId): Future[Int] = {
    val kitId = analysis.kit
    for {
      kits        <- kitService.list()
      categoryOpt <- categoryService.getCategory(catId)
    } yield {
      val kitOpt = kits.find(_.id == kitId)
      kitOpt.fold(0) { kit =>
        categoryOpt.fold(0) { category => paramsProvider.maxOverageDeviatedLociPerProfile(category, kit) }
      }
    }
  }

  override def calculateContributors(analysis: Analysis, categoryId: AlphanumericId, statOption: StatOption): Future[Int] = {
    if (analysis.kit == "Mitocondrial") {
      Future.successful(1)
    } else {
      val maxOverageFut   = getMaxOverageDeviatedLoci(analysis, categoryId)
      val populationFut   = populationBaseFrequencyService.getByName(statOption.frequencyTable)
      for {
        maxOverage          <- maxOverageFut
        maybeFrequencyTable <- populationFut
      } yield {
        val frequencyTable = PValueCalculator.parseFrequencyTable(maybeFrequencyTable.get)
        getPossibleContributors(analysis.genotypification, frequencyTable, statOption, maxOverage)
      }
    }
  }

  private def getPossibleContributors(genotypification: Genotypification, frequencyTable: FrequencyTable, statOption: StatOption, maxOverage: Int): Int = {
    var restricted: ArrayBuffer[Double] = ArrayBuffer.empty
    var availableOverage: Int = maxOverage

    for (x <- 1 to 4) {
      availableOverage = maxOverage
      var contributorProduct = 1.0
      genotypification.foreach {
        case (marker, alleles) =>
          try {
            val allelesArray = LRMixCalculator.transformAlleleValues(alleles.toArray)
            val r = 2 * x - allelesArray.size
            contributorProduct = contributorProduct * {
              if (r < 0) {
                if (availableOverage > 0) {
                  availableOverage -= 1
                  1.0
                } else {
                  0.0
                }
              } else if (r == 0) {
                LRMixCalculator.pCond(frequencyTable, statOption.theta)(marker, allelesArray, Array.empty)._1
              } else {
                val combinations: Iterator[Array[Double]] = LRMixCalculator.generateUnknowns(allelesArray, r)
                var combinationSum = 0.0
                while (combinations.hasNext) {
                  val combination = combinations.next()
                  val unknowns = allelesArray ++ combination
                  combinationSum += LRMixCalculator.pCond(frequencyTable, statOption.theta)(marker, unknowns, Array.empty)._1
                }
                combinationSum
              }
            }
          } catch {
            case e: NoFrequencyException => println(e.getMessage)
          }
      }
      restricted :+= contributorProduct
    }
    restricted.indexOf(restricted.max) + 1
  }

  // Structural change: legacy uses profileDataService.findProfileDataLocalOrSuperior (not migrated).
  // Modern uses profileDataRepository.findByCode (local lookup only; superior-instance scenario not covered).
  override def getStats(laboratory: String): Future[Option[StatOption]] = {
    for {
      defaultBase   <- populationBaseFrequencyService.getDefault()
      laboratoryOpt <- laboratoryService.get(laboratory)
    } yield {
      if (defaultBase.isDefined && laboratoryOpt.isDefined) {
        Some(StatOption(
          defaultBase.get.name,
          defaultBase.get.model,
          defaultBase.get.theta,
          laboratoryOpt.get.dropIn,
          Some(laboratoryOpt.get.dropOut)
        ))
      } else {
        None
      }
    }
  }
}


