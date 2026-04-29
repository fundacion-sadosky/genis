package unit.probability

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.*

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import probability.{FullCalculationScenario, FullHypothesis, LRMixCalculator}
import profile.Allele
import types.StatOption

class LRMixCalculatorTest extends AnyWordSpec with Matchers {

  given ec: ExecutionContext = ExecutionContext.global
  val timeout: Duration = Duration(100, "seconds")

  val theta = 0.01

  val frequencyTable = Map(
    ("DUMMY", BigDecimal(-1)) -> 0.0000001,
    ("DUMMY", BigDecimal(1))  -> 0.03,
    ("DUMMY", BigDecimal(2))  -> 0.1,
    ("DUMMY", BigDecimal(3))  -> 0.3,
    ("DUMMY", BigDecimal(4))  -> 0.23,
    ("DUMMY", BigDecimal(5))  -> 0.12,
    ("DUMMY", BigDecimal(6))  -> 0.22
  )

  val frequencyTable4 = Map(
    ("D1S1656", BigDecimal(-1))  -> 0.00005,
    ("D1S1656", BigDecimal(16))  -> 0.16637,
    ("D2S1338", BigDecimal(-1))  -> 0.00005,
    ("D2S1338", BigDecimal(17))  -> 0.232955,
    ("D2S1338", BigDecimal(21))  -> 0.025568,
    ("D2S1338", BigDecimal(23))  -> 0.139205,
    ("Penta D", BigDecimal(-1))  -> 0.00005,
    ("Penta D", BigDecimal(9))   -> 0.19036,
    ("Penta D", BigDecimal(11))  -> 0.16045,
    ("Penta D", BigDecimal(12))  -> 0.17008
  )

  val sample_mixture = Map(
    "D1S1656" -> List(Allele(16)),
    "D2S1338" -> List(Allele(17), Allele(21), Allele(23)),
    "Penta D"  -> List(Allele(9), Allele(11), Allele(12))
  )
  val sample_unique = Map(
    "D1S1656" -> List(Allele(16)),
    "D2S1338" -> List(Allele(21), Allele(23)),
    "Penta D"  -> List(Allele(12))
  )
  val victim = Map(
    "D1S1656" -> List(Allele(16)),
    "D2S1338" -> List(Allele(21), Allele(23)),
    "Penta D"  -> List(Allele(12))
  )
  val suspect = Map(
    "D1S1656" -> List(Allele(16)),
    "D2S1338" -> List(Allele(17)),
    "Penta D"  -> List(Allele(9), Allele(11))
  )

  // ── pCond ────────────────────────────────────────────────────────

  "LRMixCalculator.pCond" must {

    "calculate frequency for single unknown with known alleles" in {
      val uAlleles = Array(2.0)
      val kAlleles = Array(1.0, 1.0)

      val p = LRMixCalculator.pCond(frequencyTable, theta)("DUMMY", uAlleles, kAlleles, Map(1.0 -> 2))._1

      p mustBe ((1 - theta) * frequencyTable(("DUMMY", 2)) / (1 + theta))
    }

    "calculate frequency for zero knowns and exactly one unknown" in {
      val uAlleles = Array(1.0)
      val kAlleles = Array[Double]()

      val p = LRMixCalculator.pCond(frequencyTable, theta)("DUMMY", uAlleles, kAlleles)._1

      p mustBe ((1 - theta) * frequencyTable(("DUMMY", 1)) / (1 - theta))
    }

    "calculate frequency for zero knowns and more than one unknown" in {
      val uAlleles = Array(1.0, 1.0, 2.0, 3.0)
      val kAlleles = Array[Double]()

      val p = LRMixCalculator.pCond(frequencyTable, theta)("DUMMY", uAlleles, kAlleles)._1

      p mustBe (
        (1 - theta) * (1 - theta) *
          frequencyTable(("DUMMY", 1)) * frequencyTable(("DUMMY", 2)) * frequencyTable(("DUMMY", 3)) *
          (theta + (1 - theta) * frequencyTable(("DUMMY", 1))) / (1 + theta) / (1 + 2 * theta)
      ) * 12
    }

    "calculate frequency for more than one known and more than one unknown" in {
      val uAlleles = Array(1.0, 2.0)
      val kAlleles = Array(1.0, 2.0)

      val p = LRMixCalculator.pCond(frequencyTable, theta)("DUMMY", uAlleles, kAlleles, Map(1.0 -> 1, 2.0 -> 1))._1

      p mustBe (
        (theta + (1 - theta) * frequencyTable(("DUMMY", 1))) / (1 + theta) *
          (theta + (1 - theta) * frequencyTable(("DUMMY", 2))) * 2 / (1 + 2 * theta)
      ) +- 0.0001
    }
  }

  // ── generateUnknowns ─────────────────────────────────────────────

  "LRMixCalculator.generateUnknowns" must {

    "include combinations where all alleles are the same" in {
      val unknowns = LRMixCalculator.generateUnknowns(
        LRMixCalculator.alleleValues(frequencyTable4, "D1S1656"), 4
      )
      unknowns.toArray must contain(Array(16.0, 16.0, 16.0, 16.0))
    }
  }

  // ── dropIns ──────────────────────────────────────────────────────

  "LRMixCalculator.dropIns" must {

    "return empty array when sample is subset of participants" in {
      val result = LRMixCalculator.dropIns(Array(13.0, 13.0, 14.0), Array(13.0, 14.0))
      result mustEqual Array.empty[Double]
    }

    "return alleles in sample but not in any participant" in {
      val result = LRMixCalculator.dropIns(Array(15.0, 13.0, 14.0), Array(15.0, 16.0))
      result mustEqual Array(13.0, 14.0)
    }
  }

  // ── dropOuts ─────────────────────────────────────────────────────

  "LRMixCalculator.dropOuts" must {

    "return empty array when all participant alleles appear in sample" in {
      val result = LRMixCalculator.dropOuts(Array(12.0, 13.0, 14.0), Array(14.0, 14.0, 12.0, 13.0))
      result mustEqual Array.empty[Double]
    }

    "return alleles in participants but absent from sample" in {
      val result = LRMixCalculator.dropOuts(Array(12.0, 13.0, 14.0), Array(15.0, 14.0, 12.0, 13.0))
      result mustEqual Array(15.0)
    }
  }

  // ── commonAlleles ────────────────────────────────────────────────

  "LRMixCalculator.commonAlleles" must {

    "return empty array when sets are disjoint" in {
      val result = LRMixCalculator.commonAlleles(Array(12.0, 13.0, 14.0), Array(15.0, 17.0, 16.0, 15.0))
      result mustEqual Array.empty[Double]
    }

    "return alleles present in both sample and participants" in {
      val result = LRMixCalculator.commonAlleles(Array(12.0, 13.0, 14.0), Array(15.0, 14.0, 13.0, 12.0))
      result mustEqual Array(14.0, 13.0, 12.0)
    }
  }

  // ── calculateLRMix ───────────────────────────────────────────────

  "LRMixCalculator.calculateLRMix" must {

    "compare mixture sample against 1 profile and 2 unknowns" in {
      val prosecutor = FullHypothesis(Array(victim), Array(), 1, 0)
      val defense    = FullHypothesis(Array(), Array(victim), 2, 0)
      val scenario   = FullCalculationScenario(sample_mixture, prosecutor, defense, StatOption("", "", 0, 0, Some(0)))

      val result = Await.result(LRMixCalculator.calculateLRMix(scenario, frequencyTable4), timeout)

      result.detailed("D1S1656").get mustBe 36.1285  +- 0.0001
      result.detailed("D2S1338").get mustBe 33.1134  +- 0.0001
      result.detailed("Penta D").get mustBe  1.8812  +- 0.0001
      result.total                   mustBe 2250.6281 +- 0.0001
    }

    "compare mixture sample against 1 profile and 2 unknowns with dropOut/dropIn parameters" in {
      val prosecutor = FullHypothesis(Array(victim), Array(), 1, 0.1)
      val defense    = FullHypothesis(Array(), Array(victim), 2, 0.1)
      val scenario   = FullCalculationScenario(sample_mixture, prosecutor, defense, StatOption("", "", 0.02, 0.5, Some(0.0)))

      val result = Await.result(LRMixCalculator.calculateLRMix(scenario, frequencyTable4), timeout)

      result.detailed("D1S1656").get mustBe 20.3178 +- 0.0001
      result.detailed("D2S1338").get mustBe 16.3549 +- 0.0001
      result.detailed("Penta D").get mustBe  2.0325 +- 0.0001
      result.total                   mustBe 675.3949 +- 0.0001
    }

    "compare mixture sample against 2 profiles and 1 unknown" in {
      val prosecutor = FullHypothesis(Array(victim, suspect), Array(), 0, 0)
      val defense    = FullHypothesis(Array(victim), Array(suspect), 1, 0)
      val scenario   = FullCalculationScenario(sample_mixture, prosecutor, defense, StatOption("", "", 0, 0, Some(0)))

      val result = Await.result(LRMixCalculator.calculateLRMix(scenario, frequencyTable4), timeout)

      result.detailed("D1S1656").get mustBe  36.1285 +- 0.0001
      result.detailed("D2S1338").get mustBe   7.6314 +- 0.0001
      result.detailed("Penta D").get mustBe  16.3702 +- 0.0001
      result.total                   mustBe 4513.4560 +- 0.0001
    }

    "compare mixture sample against 2 profiles and 1 unknown with parameters" in {
      val prosecutor = FullHypothesis(Array(victim, suspect), Array(), 0, 0.1)
      val defense    = FullHypothesis(Array(victim), Array(suspect), 1, 0.1)
      val scenario   = FullCalculationScenario(sample_mixture, prosecutor, defense, StatOption("", "", 0.02, 0.01, Some(0)))

      val result = Await.result(LRMixCalculator.calculateLRMix(scenario, frequencyTable4), timeout)

      result.detailed("D1S1656").get mustBe 23.2128  +- 0.0001
      result.detailed("D2S1338").get mustBe  6.0086  +- 0.0001
      result.detailed("Penta D").get mustBe 14.9574  +- 0.0001
      result.total                   mustBe 2086.2272 +- 0.0001
    }

    "compare mixture sample against 2 profiles and 2 unknowns" in {
      val prosecutor = FullHypothesis(Array(victim, suspect), Array(), 0, 0)
      val defense    = FullHypothesis(Array(), Array(victim, suspect), 2, 0)
      val scenario   = FullCalculationScenario(sample_mixture, prosecutor, defense, StatOption("", "", 0, 0, Some(0)))

      val result = Await.result(LRMixCalculator.calculateLRMix(scenario, frequencyTable4), timeout)

      result.detailed("D1S1656").get mustBe 1305.2687   +- 0.0001
      result.detailed("D2S1338").get mustBe  252.7021   +- 0.0001
      result.detailed("Penta D").get mustBe   30.7966   +- 0.0001
      result.total                   mustBe 10158111.0  +- 1.0
    }

    "compare mixture sample against 2 profiles and 2 unknowns with parameters" in {
      val prosecutor = FullHypothesis(Array(victim, suspect), Array(), 0, 0.1)
      val defense    = FullHypothesis(Array(), Array(victim, suspect), 2, 0.1)
      val scenario   = FullCalculationScenario(sample_mixture, prosecutor, defense, StatOption("", "", 0, 0.02, Some(0)))

      val result = Await.result(LRMixCalculator.calculateLRMix(scenario, frequencyTable4), timeout)

      result.detailed("D1S1656").get mustBe 1292.3452    +- 0.0001
      result.detailed("D2S1338").get mustBe  228.7760    +- 0.0001
      result.detailed("Penta D").get mustBe   27.8625    +- 0.0001
      result.total                   mustBe 8237789.1486 +- 0.0001
    }

    "compare unique sample against 1 profile and 1 unknown" in {
      val prosecutor = FullHypothesis(Array(victim), Array(), 0, 0)
      val defense    = FullHypothesis(Array(), Array(victim), 1, 0)
      val scenario   = FullCalculationScenario(sample_unique, prosecutor, defense, StatOption("", "", 0, 0, Some(0)))

      val result = Await.result(LRMixCalculator.calculateLRMix(scenario, frequencyTable4), timeout)

      result.detailed("D1S1656").get mustBe  36.1285 +- 0.0001
      result.detailed("D2S1338").get mustBe 140.4812 +- 0.0001
      result.detailed("Penta D").get mustBe  34.5695 +- 0.0001
    }
  }
}
