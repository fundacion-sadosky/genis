// VERBATIM EXTRACT of app/pedigree/MutationService.scala
//
// In GENis that file defines two things: the service class (which depends on persistence
// and other services, so it cannot compile in this repository) and `object MutationService`,
// the pure functions pedigree.BayesianNetwork calls. This file contains only that object,
// copied without changes. test/standalone/MutationServiceExtractTest.scala checks that it
// matches the original line by line.

package pedigree

// ---- extract begins ----
object MutationService {
  private val stepTolerance = 1e-9

  def isWholeRepeatStep(i: Double, j: Double): Boolean = {
    val d = scala.math.abs(i - j)
    scala.math.abs(d - scala.math.rint(d)) < stepTolerance
  }

  def stepDistance(i: Double, j: Double, cantSaltos: Long): Option[Double] = {
    val pasos = scala.math.rint(scala.math.abs(i - j))
    if (isWholeRepeatStep(i, j) && pasos <= cantSaltos.toDouble) {
      Some(pasos)
    } else {
      None
    }
  }

  def recalibrateKis(
    data: List[(MutationModelParameter, List[MutationModelKi], MutationModel)],
    n: Map[String, List[Double]]
  ): List[(MutationModelParameter, List[MutationModelKi], MutationModel)] =
    data.map {
      case entry @ (param, _, model) =>
        // (3) "Stepwise extended" reusa la misma k_i que (2) para sus vecinos
        // de la misma clase de microvariante -- BayesianNetwork.
        // getProbabityOfNonDiagonal caso (3) la busca en mutationMarkerData._2
        // igual que el caso (2). La mutationRateMicrovariant de (3) no tiene
        // k_i propia: se reparte directo, sin calibrar (ver ese caso).
        if ((model.mutationType != 2L && model.mutationType != 3L) ||
            param.mutationRate.isEmpty ||
            param.mutationRange.isEmpty) {
          entry
        } else {
          val universe = n.getOrElse(param.locus, Nil).filter(_ > 0).distinct.sorted
          if (universe.isEmpty) {
            entry
          } else {
            (param, universe.map(allele => MutationModelKi(
              id = 0L,
              idMutationModelParameter = param.id,
              allele = allele,
              ki = mutationNormalizationKi(
                allele, universe, param.mutationRate.get,
                param.mutationRange.get.toDouble, model.cantSaltos))), model)
          }
        }
    }

  def mutationNormalizationKi(
    allele: Double,
    alleles: List[Double],
    mutationRate: BigDecimal,
    mutationRange: Double,
    cantSaltos: Long
  ): BigDecimal = {
    val sum = mutationNormalizationSum(allele, alleles, mutationRange, cantSaltos)
    if (sum == 0.0) BigDecimal(0.0) else mutationRate / sum
  }

  def mutationNormalizationSum(
    i: Double,
    alleles: List[Double],
    mutationRange: Double,
    cantSaltos: Long
  ): Double = {
    alleles
      .filter(_ != i)
      .flatMap(j => stepDistance(i, j, cantSaltos))
      .map(pasos => scala.math.pow(mutationRange, pasos))
      .sum
  }
}
