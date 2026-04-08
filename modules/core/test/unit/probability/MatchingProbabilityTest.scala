package unit.probability

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import probability.{HardyWeinbergCalculationProbability, PValueCalculator}
import profile.Allele
import profile.AlleleValue

class MatchingProbabilityTest extends AnyWordSpec with Matchers {

  val frequencyTable3 = Map(
    ("TPOX", BigDecimal(1)) -> 0.03,
    ("TPOX", BigDecimal(2)) -> 0.1,
    ("TPOX", BigDecimal(3)) -> 0.3,
    ("TPOX", BigDecimal(4)) -> 0.23,
    ("TPOX", BigDecimal(5)) -> 0.12,
    ("TPOX", BigDecimal(6)) -> 0.22
  )

  val hw = new HardyWeinbergCalculationProbability

  // Each scenario: (ma, mb, va, vb) → (expected p3, expected p2a, expected p2b)
  // None means the function is expected to return None
  case class Scenario(
    ma: Seq[Int], mb: Seq[Int],
    va: Option[Seq[Int]], vb: Option[Seq[Int]],
    p3: Option[Double], p2a: Option[Double], p2b: Option[Double]
  )

  val scenarios: List[Scenario] = List(
    Scenario(Seq(1,2,3), Seq(1,2,3,4), None,            None,            Some(0.00022), Some(0.0046), Some(0.005)),
    Scenario(Seq(1,2,3), Seq(1,2,3,4), Some(Seq(1,3)),  None,            Some(0.0017),  Some(0.076),  Some(0.005)),
    Scenario(Seq(1,2,3), Seq(1,2,4),   None,            None,            Some(0.00012), Some(0.0046), Some(0.003)),
    Scenario(Seq(1,2,3), Seq(1,2,4),   Some(Seq(1,1)),  None,            None,          Some(0.06),   Some(0.003)),
    Scenario(Seq(1,2,3), Seq(1,2,4),   Some(Seq(1)),    None,            Some(0.00024), Some(0.041),  Some(0.003)),
    Scenario(Seq(1,2,3), Seq(1,2,4),   Some(Seq(3)),    None,            Some(3e-4),    Some(0.0066), Some(0.003)),
    Scenario(Seq(1,2,3), Seq(1,2,3,4), Some(Seq(1,4)),  None,            None,          None,         Some(0.005)),
    Scenario(Seq(1,2,3), Seq(1,2,3,4), None,            Some(Seq(1,4)),  Some(0.0015),  Some(0.0046), Some(0.06)),
    Scenario(Seq(1,2,3), Seq(1,2,5),   None,            None,            Some(4.9e-5),  Some(0.0046), Some(0.0011)),
    Scenario(Seq(1,2,3), Seq(1,2,5),   Some(Seq(1,1)),  None,            None,          Some(0.06),   Some(0.0011)),
    Scenario(Seq(1,2,3), Seq(1,2,5),   Some(Seq(1)),    None,            Some(1e-4),    Some(0.041),  Some(0.0011)),
    Scenario(Seq(1,2,3), Seq(1,2,5),   Some(Seq(2,3)),  None,            Some(3e-4),    Some(0.025),  Some(0.0011)),
    Scenario(Seq(1),     Seq(1,2,3),   None,            None,            Some(4.9e-8),  Some(8.1e-7), Some(0.0046)),
    Scenario(Seq(1),     Seq(1,2,3),   Some(Seq(1,2)),  None,            None,          None,         Some(0.0046)),
    Scenario(Seq(1),     Seq(1,2,3),   Some(Seq(1,1)),  Some(Seq(1,2)),  None,          Some(9e-4),   Some(0.17)),
    Scenario(Seq(1),     Seq(1,2,3),   Some(Seq(1)),    Some(Seq(1,2)),  None,          Some(2.7e-5), Some(0.17))
  )

  "PValueCalculator.mixmixH2 and mixmixH3" must {

    scenarios.foreach { scenario =>
      val label = s"(ma=${scenario.ma}, mb=${scenario.mb}, va=${scenario.va}, vb=${scenario.vb})" +
        s" → p3=${scenario.p3}, p2a=${scenario.p2a}, p2b=${scenario.p2b}"

      s"return correct probabilities for $label" in {
        val ma: Seq[AlleleValue] = scenario.ma.map(v => Allele(BigDecimal(v)))
        val mb: Seq[AlleleValue] = scenario.mb.map(v => Allele(BigDecimal(v)))
        val va: Option[Seq[AlleleValue]] = scenario.va.map(_.map(v => Allele(BigDecimal(v))))
        val vb: Option[Seq[AlleleValue]] = scenario.vb.map(_.map(v => Allele(BigDecimal(v))))

        val p3  = PValueCalculator.mixmixH3(frequencyTable3)(hw)("TPOX", ma, va, mb, vb)
        val p2a = PValueCalculator.mixmixH2(frequencyTable3)(hw)("TPOX", ma, va)
        val p2b = PValueCalculator.mixmixH2(frequencyTable3)(hw)("TPOX", mb, vb)

        scenario.p3 match {
          case Some(expected) =>
            p3 must not be None
            p3.get mustBe expected +- (expected / 10)
          case None =>
            p3 mustBe None
        }
        scenario.p2a match {
          case Some(expected) =>
            p2a must not be None
            p2a.get mustBe expected +- (expected / 10)
          case None =>
            p2a mustBe None
        }
        scenario.p2b match {
          case Some(expected) =>
            p2b must not be None
            p2b.get mustBe expected +- (expected / 10)
          case None =>
            p2b mustBe None
        }
      }
    }
  }
}
