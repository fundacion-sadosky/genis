package unit.probability

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import probability.{HardyWeinbergCalculationProbability, PValueCalculator}
import profile.Allele
import stats.{PopulationBaseFrequency, PopulationSampleFrequency}
import stats.ProbabilityModel

class PValueCalculatorTest extends AnyWordSpec with Matchers {

  val seqPopulation: Seq[PopulationSampleFrequency] = List(
    PopulationSampleFrequency("TPOX", 10, BigDecimal(0.1)),
    PopulationSampleFrequency("TPOX", 11, BigDecimal(0.3)),
    PopulationSampleFrequency("TPOX", -1, BigDecimal(0.2)),
    PopulationSampleFrequency("D21S11", -1, BigDecimal(0.01)),
    PopulationSampleFrequency("D21S11", 36.2, BigDecimal(0.000272064424855806)),
    PopulationSampleFrequency("D21S11", 37, BigDecimal(0.1))
  )

  val populationBaseFrequency = PopulationBaseFrequency(
    "A", 0, ProbabilityModel.HardyWeinberg, seqPopulation
  )

  "PValueCalculator.parseFrequencyTable" must {

    "apply minimum frequency as floor for entries below it" in {
      val result = PValueCalculator.parseFrequencyTable(populationBaseFrequency)

      // TPOX 10 = max(0.1, 0.2) = 0.2
      result(("TPOX", 10)) mustBe 0.2
      // TPOX 11 = max(0.3, 0.2) = 0.3
      result(("TPOX", 11)) mustBe 0.3
      // D21S11 36.2 = max(0.000272..., 0.01) = 0.01
      result(("D21S11", 36.2)) mustBe 0.01
      // D21S11 37 = max(0.1, 0.01) = 0.1
      result(("D21S11", 37)) mustBe 0.1
    }
  }

  "PValueCalculator.getMinimumFrequency" must {

    "return the -1 sentinel entry as minimum frequency" in {
      val table = Map(("TPOX", BigDecimal(-1)) -> 0.2, ("TPOX", BigDecimal(10)) -> 0.1)
      PValueCalculator.getMinimumFrequency("TPOX", table) mustBe Some(0.2)
    }

    "return None when marker has no minimum entry" in {
      val table = Map(("TPOX", BigDecimal(10)) -> 0.1)
      PValueCalculator.getMinimumFrequency("TPOX", table) mustBe None
    }
  }

  "PValueCalculator.getProbability" must {

    "return the frequency for a known allele" in {
      val table = Map(
        ("TPOX", BigDecimal(-1)) -> 0.01,
        ("TPOX", BigDecimal(10)) -> 0.5
      )
      PValueCalculator.getProbability(table)("TPOX", BigDecimal(10)) mustBe Some(0.5)
    }

    "return minimum frequency for an unknown allele (off-ladder)" in {
      val table = Map(("TPOX", BigDecimal(-1)) -> 0.01)
      PValueCalculator.getProbability(table)("TPOX", BigDecimal(99)) mustBe Some(0.01)
    }

    "return None when neither the allele nor a minimum exists" in {
      val table = Map(("TPOX", BigDecimal(10)) -> 0.5)
      PValueCalculator.getProbability(table)("TPOX", BigDecimal(99)) mustBe None
    }
  }

  "PValueCalculator.calculateRMP" must {

    val hw = new HardyWeinbergCalculationProbability

    "calculate RMP for homozygous locus" in {
      val table = Map(("TPOX", BigDecimal(-1)) -> 0.01, ("TPOX", BigDecimal(10)) -> 0.5)
      val result = PValueCalculator.calculateRMP(table)(hw)("TPOX", Some(List(Allele(10), Allele(10))))
      result mustBe Some(0.25) // 0.5 * 0.5
    }

    "calculate RMP for heterozygous locus" in {
      val table = Map(
        ("TPOX", BigDecimal(-1)) -> 0.01,
        ("TPOX", BigDecimal(10)) -> 0.5,
        ("TPOX", BigDecimal(12)) -> 0.2
      )
      val result = PValueCalculator.calculateRMP(table)(hw)("TPOX", Some(List(Allele(10), Allele(12))))
      result mustBe Some(0.2) // 2 * 0.5 * 0.2
    }

    "calculate RMP for single allele (wildcard dropout)" in {
      val table = Map(("TPOX", BigDecimal(-1)) -> 0.01, ("TPOX", BigDecimal(10)) -> 0.5)
      val result = PValueCalculator.calculateRMP(table)(hw)("TPOX", Some(List(Allele(10))))
      // wildcard: p * (2 - p) = 0.5 * 1.5 = 0.75
      result mustBe Some(0.75)
    }

    "return Some(1.0) for allele lists of length other than 1 or 2" in {
      val table = Map(("TPOX", BigDecimal(-1)) -> 0.01)
      val result = PValueCalculator.calculateRMP(table)(hw)("TPOX", Some(List(Allele(1), Allele(2), Allele(3))))
      result mustBe Some(1.0)
    }
  }
}
