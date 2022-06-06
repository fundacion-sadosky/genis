package probability

import util.EnumJsonUtils

import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.libs.json.Format
import play.api.libs.json._

abstract class MatchingProbabilityCalculationMode {

  def homo(alleleProbability: Double): Double
  def hetero(allele1Probability: Double, allele2Probability: Double): Double
  def wildcard(alleleProbability: Double, n: Long): Double

}

object ProbabilityModel extends Enumeration {
  type ProbabilityModel = Value
  val HardyWeinberg, NRCII41, NRCII410 = Value

  implicit val stringencyReads: Reads[ProbabilityModel] = EnumJsonUtils.enumReads(ProbabilityModel)
  implicit def stringencyWrites: Writes[ProbabilityModel] = EnumJsonUtils.enumWrites

}

class HardyWeinbergCalculationProbability extends MatchingProbabilityCalculationMode {
  def homo(alleleProbability: Double): Double = {
    alleleProbability * alleleProbability
  }

  def hetero(allele1Probability: Double, allele2Probability: Double): Double = {
    2 * allele1Probability * allele2Probability
  }

  def wildcard(alleleProbability: Double, n: Long): Double = {
    alleleProbability * (2 - alleleProbability)
  }

}

class NRCII41CalculationProbability(theta: Double = 0.01) extends MatchingProbabilityCalculationMode {
  def homo(alleleProbability: Double): Double = {
    alleleProbability * alleleProbability + alleleProbability * (1 - alleleProbability) * theta

  }

  def hetero(allele1Probability: Double, allele2Probability: Double): Double = {
    2 * allele1Probability * allele2Probability
  }

  def wildcard(alleleProbability: Double, n: Long): Double = {
    alleleProbability * alleleProbability * (theta - 1) + alleleProbability * (2 - theta)
  }
}

class NRCII410CalculationProbability(theta: Double = 0.01) extends MatchingProbabilityCalculationMode {
  def homo(alleleProbability: Double): Double = {
    ((alleleProbability * (1 - theta) + 2 * theta) * (alleleProbability * (1 - theta) + 3 * theta)) / (1 + theta) / (1 + 2 * theta)
  }

  def hetero(allele1Probability: Double, allele2Probability: Double): Double = {
    (2 * (allele1Probability * (1 - theta) + theta) * (allele2Probability * (1 - theta) + theta)) / (1 + theta) / (1 + 2 * theta)

  }

  def wildcard(alleleProbability: Double, n: Long): Double = {
    alleleProbability * (2 - alleleProbability)

    val pii = homo(alleleProbability)

    val pij = 2 * (theta + (1 - theta) * alleleProbability) / (1 + theta) / (1 + 2 * theta) *
      (alleleProbability * (theta - 1) + theta * (n - 2))

    pii + pij

  }
}
