package probability

import javax.inject.{Inject, Singleton}

import configdata.{CategoryService, QualityParamsProvider}
import kits.StrKitService
import laboratories.LaboratoryService
import play.api.Logger
import probability.PValueCalculator.FrequencyTable
import profile.{Allele, Analysis, NewAnalysis, Profile}
import profile.Profile.Genotypification
import profiledata.ProfileDataService
import stats.PopulationBaseFrequencyService
import types.{AlphanumericId, SampleCode, StatOption}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

abstract class ProbabilityService {
  def calculateContributors(analysis: Analysis, categoryId: AlphanumericId, statOption : StatOption) : Future[Int]
  def getStats(globalCode: SampleCode): Future[Option[StatOption]]
  def getStats(laboratory: String): Future[Option[StatOption]]
}

@Singleton
class ProbabilityServiceImpl @Inject() (
  populationBaseFrequencyService: PopulationBaseFrequencyService,
  profileDataService: ProfileDataService,
  laboratoryService: LaboratoryService,
  kitService: StrKitService,
  paramsProvider: QualityParamsProvider,
  categoryService: CategoryService) extends ProbabilityService {

  val logger = Logger(this.getClass())

  private def getMaxOverageDeviatedLoci(analysis: Analysis, catId: AlphanumericId) = {
    val kitId = analysis.kit

    kitService.list map { kits =>
      val kitOpt = kits.find(_.id == kitId)
      kitOpt.fold(0)({ kit =>
        categoryService.getCategory(catId)
          .fold(0)({ category => paramsProvider.maxOverageDeviatedLociPerProfile(category, kit) })
      })
    }
  }

  def calculateContributors(analysis: Analysis, categoryId: AlphanumericId, statOption : StatOption): Future[Int] = {
    //val max: Int = genotypification.map(gen => math.ceil(gen._2.size/2.0).toInt).max
    if(analysis.kit == "Mitocondrial"){
      Future.successful(1)
    }else{
      val maxOverageFut = getMaxOverageDeviatedLoci(analysis, categoryId)
      val populationFut = populationBaseFrequencyService.getByName(statOption.frequencyTable)
      for {
        maxOverage <- maxOverageFut
        maybeFrequencyTable <- populationFut
      } yield {
        val frequencyTable = PValueCalculator.parseFrequencyTable(maybeFrequencyTable.get)
        getPossibleContributors(analysis.genotypification, frequencyTable, statOption, maxOverage)
      }
    }

  }

  private def getPossibleContributors(genotypification: Genotypification, frequencyTable: FrequencyTable, statOption: StatOption, maxOverage: Int) = {
    var restricted: ArrayBuffer[Double] = ArrayBuffer.empty
    var availableOverage: Int = maxOverage

    for (x <- 1 to 4) {
      availableOverage = maxOverage

      var contributorProduct = 1.0
      genotypification.foreach {
        case (marker, alleles) => {
          try {
            val allelesArray = LRMixCalculator.transformAlleleValues(alleles.toArray)

            val r = 2 * x - allelesArray.size

            contributorProduct *= {
              if (r < 0) {
                  if (availableOverage > 0) {
                    availableOverage -= 1
                    1
                  } else {
                    0
                  }
              } else if (r == 0) {
                LRMixCalculator.pCond(frequencyTable, statOption.theta)(marker, allelesArray, Array.empty)._1
              } else {
                val combinations: Iterator[Array[Double]] = LRMixCalculator.generateUnknowns(allelesArray, r)

                var combinationSum = 0.0
                while(combinations.hasNext) {
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
      }

      restricted :+= contributorProduct
    }
    restricted.indexOf(restricted.max) + 1
  }

  override def getStats(globalCode: SampleCode) = {
    for {
      profileDataOpt <- profileDataService.findProfileDataLocalOrSuperior(globalCode)
      defaultBase <- populationBaseFrequencyService.getDefault()
      stats <- getStats(profileDataOpt.get.laboratory)
    } yield {
      stats
    }
  }

  override def getStats(laboratory: String) = {
    for {
      defaultBase <- populationBaseFrequencyService.getDefault()
      laboratoryOpt <- laboratoryService.get(laboratory)
    } yield {

      if (defaultBase.isDefined && laboratoryOpt.isDefined) {
        Some(StatOption(defaultBase.get.name, defaultBase.get.model, defaultBase.get.theta, laboratoryOpt.get.dropIn, Some(laboratoryOpt.get.dropOut)))
      } else {
        None
      }

    }
  }

}

