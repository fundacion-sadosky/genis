package unit.probability

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import probability.{HardyWeinbergCalculationProbability, NRCII41CalculationProbability, NRCII410CalculationProbability}

class MatchingProbabilityCalculationModeTest extends AnyWordSpec with Matchers {

  private val theta  = 0.01
  private val p      = 0.3
  private val p1     = 0.3
  private val p2     = 0.2
  private val n      = 10L
  private val tol    = 1e-10

  // ---------------------------------------------------------------------------
  // HardyWeinbergCalculationProbability
  // ---------------------------------------------------------------------------

  "HardyWeinbergCalculationProbability" must {

    val hw = new HardyWeinbergCalculationProbability

    "compute homo correctly: p²" in {
      hw.homo(p) mustBe (p * p +- tol)
    }

    "compute homo of 0 as 0" in {
      hw.homo(0.0) mustBe (0.0 +- tol)
    }

    "compute homo of 1 as 1" in {
      hw.homo(1.0) mustBe (1.0 +- tol)
    }

    "compute hetero correctly: 2*p1*p2" in {
      hw.hetero(p1, p2) mustBe (2 * p1 * p2 +- tol)
    }

    "compute hetero as 0 when one probability is 0" in {
      hw.hetero(p1, 0.0) mustBe (0.0 +- tol)
    }

    "compute wildcard correctly: p*(2-p)" in {
      val expected = p * (2 - p)
      hw.wildcard(p, n) mustBe (expected +- tol)
    }

    "compute wildcard ignoring n parameter" in {
      hw.wildcard(p, 0) mustBe (hw.wildcard(p, 100) +- tol)
    }

    "compute wildcard of 0 as 0" in {
      hw.wildcard(0.0, n) mustBe (0.0 +- tol)
    }

    "compute wildcard of 1 as 1" in {
      hw.wildcard(1.0, n) mustBe (1.0 +- tol)
    }
  }

  // ---------------------------------------------------------------------------
  // NRCII41CalculationProbability
  // ---------------------------------------------------------------------------

  "NRCII41CalculationProbability" must {

    val nrcii41 = new NRCII41CalculationProbability(theta)

    "compute homo correctly: p² + p*(1-p)*θ" in {
      val expected = p * p + p * (1 - p) * theta
      nrcii41.homo(p) mustBe (expected +- tol)
    }

    "compute hetero correctly: 2*p1*p2" in {
      val expected = 2 * p1 * p2
      nrcii41.hetero(p1, p2) mustBe (expected +- tol)
    }

    "compute wildcard correctly: p²*(θ-1) + p*(2-θ)" in {
      val expected = p * p * (theta - 1) + p * (2 - theta)
      nrcii41.wildcard(p, n) mustBe (expected +- tol)
    }

    "use default theta of 0.01 when none provided" in {
      val defaultNrcii41 = new NRCII41CalculationProbability()
      val expected = p * p + p * (1 - p) * 0.01
      defaultNrcii41.homo(p) mustBe (expected +- tol)
    }
  }

  // ---------------------------------------------------------------------------
  // NRCII410CalculationProbability
  // ---------------------------------------------------------------------------

  "NRCII410CalculationProbability" must {

    val nrcii410 = new NRCII410CalculationProbability(theta)

    "compute homo correctly: ((p*(1-θ)+2θ)*(p*(1-θ)+3θ))/((1+θ)*(1+2θ))" in {
      val expected = ((p * (1 - theta) + 2 * theta) * (p * (1 - theta) + 3 * theta)) /
                     (1 + theta) / (1 + 2 * theta)
      nrcii410.homo(p) mustBe (expected +- tol)
    }

    "compute hetero correctly: (2*(p1*(1-θ)+θ)*(p2*(1-θ)+θ))/((1+θ)*(1+2θ))" in {
      val expected = (2 * (p1 * (1 - theta) + theta) * (p2 * (1 - theta) + theta)) /
                     (1 + theta) / (1 + 2 * theta)
      nrcii410.hetero(p1, p2) mustBe (expected +- tol)
    }

    "compute wildcard as homo + het-component for n unknowns" in {
      val pii = nrcii410.homo(p)
      val pij = 2 * (theta + (1 - theta) * p) / (1 + theta) / (1 + 2 * theta) *
                (p * (theta - 1) + theta * (n - 2))
      val expected = pii + pij
      nrcii410.wildcard(p, n) mustBe (expected +- tol)
    }

    "use default theta of 0.01 when none provided" in {
      val defaultNrcii410 = new NRCII410CalculationProbability()
      val defaultTheta = 0.01
      val expected = ((p * (1 - defaultTheta) + 2 * defaultTheta) * (p * (1 - defaultTheta) + 3 * defaultTheta)) /
                     (1 + defaultTheta) / (1 + 2 * defaultTheta)
      defaultNrcii410.homo(p) mustBe (expected +- tol)
    }
  }
}
