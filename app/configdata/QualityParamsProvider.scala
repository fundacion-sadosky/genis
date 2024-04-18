package configdata

import javax.inject.Singleton

import kits.StrKit
import play.api.Logger
import util.InfixArithmeticCalculator

abstract class QualityParamsProvider {
  def minLocusQuantityAllowedPerProfile(category: FullCategory, kit: StrKit): Int
  def maxOverageDeviatedLociPerProfile(category: FullCategory, kit: StrKit): Int
  def maxAllelesPerLocus(category: FullCategory, kit: StrKit): Int
}

@Singleton
class QualityParamsProviderImpl() extends QualityParamsProvider {

  val logger: Logger = Logger(this.getClass())

  private def calcExpression(kit: StrKit, expr: String) = {
    val p = Map(
      "N" -> kit.locy_quantity.toFloat,
      "K" -> kit.representative_parameter.toFloat)

    val calc = new InfixArithmeticCalculator(p)
    calc.evaluate(expr) match {
      case Right(result) => result.floor.toInt
      case Left(error) => {
        logger.error(s"Error while calculating quality params: $error")
        10000
      }
    }
  }

  override def minLocusQuantityAllowedPerProfile(category: FullCategory, kit: StrKit): Int = {
    val configuration = findConfiguration(category, kit.`type`).fold("K")(_.minLocusPerProfile)
    calcExpression(kit, configuration)
  }

  override def maxOverageDeviatedLociPerProfile(category: FullCategory, kit: StrKit): Int = {
    val configuration = findConfiguration(category, kit.`type`).fold("0")(_.maxOverageDeviatedLoci)
    calcExpression(kit, configuration)
  }

  override def maxAllelesPerLocus(category: FullCategory, kit: StrKit): Int = {
    findConfiguration(category, kit.`type`).fold(6)(_.maxAllelesPerLocus)
  }

  private def findConfiguration(category: FullCategory, searched: Int): Option[CategoryConfiguration] = {
    category.configurations.get(searched)
  }

}
