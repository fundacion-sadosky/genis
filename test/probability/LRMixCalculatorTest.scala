package probability

import org.scalatest.mock.MockitoSugar
import profile.{Allele, Profile}
import specs.PdgSpec
import types.{SampleCode, StatOption}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.math.BigDecimal.int2bigDecimal

class LRMixCalculatorTest extends PdgSpec with MockitoSugar {


  val duration = Duration(100, SECONDS)

  val theta = 0.01

  val frequencyTable = Map(
    ("DUMMY", BigDecimal(-1)) -> 0.0000001,
    ("DUMMY", BigDecimal(1)) -> 0.03,
    ("DUMMY", BigDecimal(2)) -> 0.1,
    ("DUMMY", BigDecimal(3)) -> 0.3,
    ("DUMMY", BigDecimal(4)) -> 0.23,
    ("DUMMY", BigDecimal(5)) -> 0.12,
    ("DUMMY", BigDecimal(6)) -> 0.22)

  val frequencyTable2 = Map(
    ("DUMMY", BigDecimal(-1)) -> 0.0000001,
    ("DUMMY", BigDecimal(12)) -> 0.03,
    ("DUMMY", BigDecimal(13)) -> 0.1,
    ("DUMMY", BigDecimal(14)) -> 0.3)

  val frequencyTable3 = Map(
    ("DUMMY", BigDecimal(-1)) -> 0.0000001,
    ("DUMMY", BigDecimal(12)) -> 0.03563941,
    ("DUMMY", BigDecimal(13)) -> 0.30922432,
    ("DUMMY", BigDecimal(14)) -> 0.30922432,
    ("DUMMY", BigDecimal(15)) -> 0.20440252,
    ("DUMMY", BigDecimal(16)) -> 0.14150943)

  val frequencyTable4 = Map(
    ("D1S1656", BigDecimal(-1)) -> 0.00005,
    ("D1S1656", BigDecimal(16)) -> 0.16637,
    ("D2S1338", BigDecimal(-1)) -> 0.00005,
    ("D2S1338", BigDecimal(17)) -> 0.232955,
    ("D2S1338", BigDecimal(21)) -> 0.025568,
    ("D2S1338", BigDecimal(23)) -> 0.139205,
    ("Penta D", BigDecimal(-1)) -> 0.00005,
    ("Penta D", BigDecimal(9)) -> 0.19036,
    ("Penta D", BigDecimal(11)) -> 0.16045,
    ("Penta D", BigDecimal(12)) -> 0.17008)

  val frequencyTable5 = Map(
    ("MARKER", BigDecimal(-1)) -> 0.0000001,
    ("MARKER", BigDecimal(11)) -> 0.16637,
    ("MARKER", BigDecimal(12)) -> 0.232955,
    ("MARKER", BigDecimal(13)) -> 0.025568,
    ("MARKER", BigDecimal(14)) -> 0.139205,
    ("MARKER", BigDecimal(15)) -> 0.19036)

  val frequencyTable6 = Map(
    ("MARKER", BigDecimal(-1)) -> 0.0000001,
    ("MARKER", BigDecimal(21.3)) -> 0.16637,
    ("MARKER", BigDecimal(20.0)) -> 0.232955,
    ("MARKER", BigDecimal(17.3)) -> 0.025568,
    ("MARKER", BigDecimal(11.0)) -> 0.139205,
    ("MARKER", BigDecimal(15)) -> 0.19036,
    ("MARKER", BigDecimal(13.0)) -> 0.232955,
    ("MARKER", BigDecimal(20.3)) -> 0.025568,
    ("MARKER", BigDecimal(23.3)) -> 0.16637,
    ("MARKER", BigDecimal(15.3)) -> 0.139205,
    ("MARKER", BigDecimal(17.0)) -> 0.19036,
    ("MARKER", BigDecimal(21.0)) -> 0.16637,
    ("MARKER", BigDecimal(12.0)) -> 0.232955,
    ("MARKER", BigDecimal(19.3)) -> 0.025568,
    ("MARKER", BigDecimal(10.0)) -> 0.139205,
    ("MARKER", BigDecimal(22.3)) -> 0.19036,
    ("MARKER", BigDecimal(18.0)) -> 0.16637,
    ("MARKER", BigDecimal(9.0)) -> 0.232955,
    ("MARKER", BigDecimal(19.0)) -> 0.025568,
    ("MARKER", BigDecimal(14.0)) -> 0.139205,
    ("MARKER", BigDecimal(16.0)) -> 0.19036)

  val frequencyTableWithoutAlleleValue = Map(
    ("MARKER", BigDecimal(-1)) -> 0.01,
    ("MARKER", BigDecimal(1)) -> 0.1,
    ("MARKER", BigDecimal(2)) -> 0.2,
    ("MARKER", BigDecimal(3)) -> 0.3)

  val mixture = Map("MARKER" -> List(Allele(11), Allele(12), Allele(13)))

  val sample_mixture = Map(
    "D1S1656" -> List(Allele(16)),
    "D2S1338" -> List(Allele(17), Allele(21), Allele(23)),
    "Penta D" -> List(Allele(9), Allele(11), Allele(12)))
  val sample_unique = Map(
    "D1S1656" -> List(Allele(16)),
    "D2S1338" -> List(Allele(21), Allele(23)),
    "Penta D" -> List(Allele(12)))
  val victim = Map(
    "D1S1656" -> List(Allele(16)),
    "D2S1338" -> List(Allele(21), Allele(23)),
    "Penta D" -> List(Allele(12)))
  val suspect = Map(
    "D1S1656" -> List(Allele(16)),
    "D2S1338" -> List(Allele(17)),
    "Penta D" -> List(Allele(9), Allele(11)))
  val other_suspect = Map(
    "D1S1656" -> List(Allele(16)),
    "D2S1338" -> List(Allele(17)),
    "Penta D" -> List(Allele(9)))

  "frequency" should {
    "be calculated for single unknown" in {
      val uAlleles = Array(2.0)
      val kAlleles = Array(1.0, 1.0)

      val p = LRMixCalculator.pCond(frequencyTable, theta)("DUMMY", uAlleles, kAlleles, Map(1.0 -> 2))._1

      p mustBe ((1-theta)*frequencyTable(("DUMMY", 2)) / (1+theta))
    }
  }

  "frequency" should {
    "be calculated for zero knowns and exactly one unknown" in {

      val uAlleles = Array(1.0)
      val kAlleles = Array[Double]()

      val p = LRMixCalculator.pCond(frequencyTable, theta)("DUMMY", uAlleles, kAlleles)._1

      p mustBe ((1-theta)*frequencyTable(("DUMMY", 1)) / (1-theta))
    }
  }

  "frequency" should {
    "be calculated for zero knowns and more than one unknown" in {

      val uAlleles = Array(1.0, 1.0, 2.0, 3.0)
      val kAlleles = Array[Double]()

      val p = LRMixCalculator.pCond(frequencyTable, theta)("DUMMY", uAlleles, kAlleles)._1

      p mustBe ((1-theta)*(1-theta)*frequencyTable(("DUMMY", 1))*frequencyTable(("DUMMY", 2))*frequencyTable(("DUMMY", 3))
        * (theta + (1-theta) * frequencyTable(("DUMMY", 1))) / (1+theta) / (1+2*theta)) * 12
    }
  }

  "frequency" should {
    "be calculated for more than one known and more than one unknown" in {

      val uAlleles = Array(1.0, 2.0)
      val kAlleles = Array(1.0, 2.0)

      val p = LRMixCalculator.pCond(frequencyTable, theta)("DUMMY", uAlleles, kAlleles, Map(1.0 -> 1, 2.0 -> 1))._1


      p mustBe (theta + (1 - theta) * frequencyTable(("DUMMY", 1))) / (1 + theta) * (theta + (1 - theta) * frequencyTable(("DUMMY", 2))) * 2 / (1 + 2 * theta) +- 0.0001
    }
  }

  "generate unknown" should {
    "repeat alleles when necessary" in {
      val unknowns = LRMixCalculator.generateUnknowns(LRMixCalculator.alleleValues(frequencyTable4, "D1S1656"), 4)

      unknowns.toArray must contain (Array(16.0,16.0,16.0,16.0))
    }
  }

  "dropIns" should {
    "return empty set" in {
      val result = LRMixCalculator.dropIns(Array(13.0,13.0,14.0), Array(13.0,14.0))
      result mustEqual Array()
    }
    "return alleles in sample but not in participants" in {
      val result = LRMixCalculator.dropIns(Array(15.0, 13.0, 14.0), Array(15.0, 16.0))
      result mustEqual Array(13.0, 14.0)
    }
  }

  "dropOuts" should {
    "return empty set" in {
      val result = LRMixCalculator.dropOuts(Array(12.0,13.0,14.0), Array(14.0,14.0, 12.0, 13.0))
      result mustEqual Array()
    }
    "return alleles in participants but not in sample" in {
      val result = LRMixCalculator.dropOuts(Array(12.0,13.0,14.0), Array(15.0,14.0, 12.0, 13.0))
      result mustEqual Array(15.0)
    }
  }

  "commonAlleles" should {
    "return empty set" in {
      val result = LRMixCalculator.commonAlleles(Array(12.0,13.0,14.0), Array(15.0,17.0, 16.0, 15.0))
      result mustEqual Array()
    }
    "return alleles present in participants and in sample" in {
      val result = LRMixCalculator.commonAlleles(Array(12.0,13.0,14.0), Array(15.0,14.0, 13.0, 12.0))
      result mustEqual Array(14.0, 13.0, 12.0)
    }
  }

  "lr by scenario" should {

    "compare mixture sample against 1 profile and 2 unknowns" in {
      val prosecutor = FullHypothesis(Array(victim), Array(), 1, 0)
      val defense = FullHypothesis(Array(), Array(victim), 2, 0)
      val scenario = FullCalculationScenario(sample_mixture, prosecutor, defense, StatOption("", "", 0, 0, Some(0)))

      val result = Await.result(LRMixCalculator.calculateLRMix(scenario, frequencyTable4), duration)

      result.detailed("D1S1656").get mustBe 36.1285 +- 0.0001
      result.detailed("D2S1338").get mustBe 33.1134 +- 0.0001
      result.detailed("Penta D").get mustBe 1.8812 +- 0.0001

      result.total mustBe 2250.6281 +- 0.0001
    }

    "compare mixture sample against 1 profile and 2 unknowns with parameters" in {
      val prosecutor = FullHypothesis(Array(victim), Array(), 1, 0.1)
      val defense = FullHypothesis(Array(), Array(victim), 2, 0.1)
      val scenario = FullCalculationScenario(sample_mixture, prosecutor, defense, StatOption("", "", 0.02, 0.5, Some(0.0)))

      val result = Await.result(LRMixCalculator.calculateLRMix(scenario, frequencyTable4), duration)

      result.detailed("D1S1656").get mustBe 20.3178 +- 0.0001
      result.detailed("D2S1338").get mustBe 16.3549 +- 0.0001
      result.detailed("Penta D").get mustBe 2.0325 +- 0.0001

      result.total mustBe 675.3949 +- 0.0001
    }

    "compare mixture sample against 2 profiles and 1 unknown" in {
      val prosecutor = FullHypothesis(Array(victim, suspect), Array(), 0, 0)
      val defense = FullHypothesis(Array(victim), Array(suspect), 1, 0)
      val scenario = FullCalculationScenario(sample_mixture, prosecutor, defense, StatOption("", "", 0, 0, Some(0)))

      val result = Await.result(LRMixCalculator.calculateLRMix(scenario, frequencyTable4), duration)

      result.detailed("D1S1656").get mustBe 36.1285 +- 0.0001
      result.detailed("D2S1338").get mustBe 7.6314 +- 0.0001
      result.detailed("Penta D").get mustBe 16.3702 +- 0.0001

      result.total mustBe 4513.4560 +- 0.0001
    }

    "compare mixture sample against 2 profiles and 1 unknown with parameters" in {
      val prosecutor = FullHypothesis(Array(victim, suspect), Array(), 0, 0.1)
      val defense = FullHypothesis(Array(victim), Array(suspect), 1, 0.1)
      val scenario = FullCalculationScenario(sample_mixture, prosecutor, defense, StatOption("", "", 0.02, 0.01, Some(0)))

      val result = Await.result(LRMixCalculator.calculateLRMix(scenario, frequencyTable4), duration)

      result.detailed("D1S1656").get mustBe 23.2128 +- 0.0001
      result.detailed("D2S1338").get mustBe 6.0086 +- 0.0001
      result.detailed("Penta D").get mustBe 14.9574 +- 0.0001

      result.total mustBe 2086.2272 +- 0.0001

    }

    "compare mixture sample against 2 profiles and 2 unknowns" in {
      val prosecutor = FullHypothesis(Array(victim, suspect), Array(), 0, 0)
      val defense = FullHypothesis(Array(), Array(victim, suspect), 2, 0)
      val scenario = FullCalculationScenario(sample_mixture, prosecutor, defense, StatOption("", "", 0, 0, Some(0)))

      val result = Await.result(LRMixCalculator.calculateLRMix(scenario, frequencyTable4), duration)

      result.detailed("D1S1656").get mustBe 1305.2687 +- 0.0001
      result.detailed("D2S1338").get mustBe 252.7021 +- 0.0001
      result.detailed("Penta D").get mustBe 30.7966 +- 0.0001

      result.total mustBe 10158111.0 +- 1.0
    }

    "compare mixture sample against 2 profiles and 2 unknowns with parameters" in {
      val prosecutor = FullHypothesis(Array(victim, suspect), Array(), 0, 0.1)
      val defense = FullHypothesis(Array(), Array(victim, suspect), 2, 0.1)
      val scenario = FullCalculationScenario(sample_mixture, prosecutor, defense, StatOption("", "", 0, 0.02, Some(0)))

      val result = Await.result(LRMixCalculator.calculateLRMix(scenario, frequencyTable4), duration)

      result.detailed("D1S1656").get mustBe 1292.3452 +- 0.0001
      result.detailed("D2S1338").get mustBe 228.7760 +- 0.0001
      result.detailed("Penta D").get mustBe 27.8625 +- 0.0001

      result.total mustBe 8237789.1486 +- 0.0001
    }

    "compare unique sample against 1 profile and 1 unknown" in {
      val prosecutor = FullHypothesis(Array(victim), Array(), 0, 0)
      val defense = FullHypothesis(Array(), Array(victim), 1, 0)
      val scenario = FullCalculationScenario(sample_unique, prosecutor, defense, StatOption("", "", 0, 0, Some(0)))

      val result = Await.result(LRMixCalculator.calculateLRMix(scenario, frequencyTable4), duration)

      result.detailed("D1S1656").get mustBe 36.1285 +- 0.0001
      result.detailed("D2S1338").get mustBe 140.4812 +- 0.0001
      result.detailed("Penta D").get mustBe 34.5695 +- 0.0001

      result.total mustBe 175453.4387 +- 0.0001
    }

    "compare unique sample against 1 profile and 1 unknown with parameters" in {
      val prosecutor = FullHypothesis(Array(victim), Array(), 0, 0.1)
      val defense = FullHypothesis(Array(), Array(victim), 1, 0.1)
      val scenario = FullCalculationScenario(sample_unique, prosecutor, defense, StatOption("", "", 0.02, 0.01, Some(0)))

      val result = Await.result(LRMixCalculator.calculateLRMix(scenario, frequencyTable4), duration)

      result.detailed("D1S1656").get mustBe 24.9498 +- 0.0001
      result.detailed("D2S1338").get mustBe 75.1561 +- 0.0001
      result.detailed("Penta D").get mustBe 18.4519 +- 0.0001

      result.total mustBe 34599.8565 +- 0.0001
    }

    "performance test" in {
      val h = FullHypothesis(Array(), Array(mixture), 3, 0.1)

      val start = System.currentTimeMillis()
      val result = LRMixCalculator.getHypothesisLR("H", mixture, h, frequencyTable6, 0.01, 0.1)
      println(s"Time: ${System.currentTimeMillis() - start}")
      println(result)

      1 mustBe 1
    }

    "get fMin if allele has no value" in {
      val participants = Array(2.0, 3.0)
      val sample = Array(4.0, 1.0)
      val result = LRMixCalculator.pRepIn(0.01, frequencyTableWithoutAlleleValue, "MARKER")(sample, participants)

      result mustBe 0.0000001 +- 0.0001
    }

    "add new alleles to unknowns generation" in {
      val suspectWithNewAlleles = Map(
        "D1S1656" -> List(Allele(16)),
        "D2S1338" -> List(Allele(17), Allele(20)),
        "Penta D" -> List(Allele(9), Allele(11), Allele(10)))

      val prosecutor = FullHypothesis(Array(suspectWithNewAlleles), Array(), 1, 0.1)
      val defense = FullHypothesis(Array(), Array(suspectWithNewAlleles), 2, 0.1)
      val scenario = FullCalculationScenario(sample_mixture, prosecutor, defense, StatOption("", "", 0, 0, Some(0)))

      val result = Await.result(LRMixCalculator.calculateLRMix(scenario, frequencyTable4), duration)

      result.detailed("D1S1656").get mustBe 36.0959 +- 0.0001
      result.detailed("D2S1338").get mustBe 0.1635 +- 0.0001
      result.detailed("Penta D").get mustBe 0.4565 +- 0.0001

      result.total mustBe 2.6950 +- 0.0001
    }

    "not be 0 with mismatch alleles" in {
      val profileWithMismatch = Map(
        "D1S1656" -> List(Allele(15)),
        "D2S1338" -> List(Allele(21), Allele(23)),
        "Penta D" -> List(Allele(13)))

      val prosecutor = FullHypothesis(Array(profileWithMismatch), Array(), 1, 0)
      val defense = FullHypothesis(Array(), Array(profileWithMismatch), 2, 0)
      val scenario = FullCalculationScenario(sample_unique, prosecutor, defense, StatOption("", "", 0, 0, Some(0)))

      val result = Await.result(LRMixCalculator.calculateLRMix(scenario, frequencyTable4), duration)

      result.detailed("D1S1656").get mustBe 0
      result.detailed("Penta D").get mustBe 0

      result.total must not be 0
    }

  }

  "Mix mix New 3 2" should {
    "work without profile associated" in {

      val frequencyTable = Map(
        ("DUMMY", BigDecimal(1)) -> 0.09194810,
        ("DUMMY", BigDecimal(2)) -> 0.01176686,
        ("DUMMY", BigDecimal(3)) -> 0.10626805,
        ("DUMMY", BigDecimal(4)) -> 0.17218569,
        ("DUMMY", BigDecimal(5)) -> 0.10083325,
        ("DUMMY", BigDecimal(6)) -> 0.04264576,
        ("DUMMY", BigDecimal(7)) -> 0.06630600,
        ("DUMMY", BigDecimal(8)) -> 0.22168813,
        ("DUMMY", BigDecimal(9)) -> 0.18635815)

      //M1: 1 2 5 7 9  M2: 4 5 7 9  K1:  K2:  n1: 3  n2: 2
      val mix1 = Map("DUMMY" -> List(Allele(1),Allele(2),Allele(5),Allele(7),Allele(9)))
      val mix2 = Map("DUMMY" -> List(Allele(4),Allele(5),Allele(7),Allele(9)))

      val resultWithoutDropoutDropInTheta = LRMixCalculator.lrMixMixNuevo(mix1, None, mix2, None, frequencyTable, 3, 2, None, 0.0, 0, 0)
      val resultWithDropOut = LRMixCalculator.lrMixMixNuevo(mix1, None, mix2, None, frequencyTable, 3, 2, None, 0.0, 0, 0.05)
      val resultWithDropIn = LRMixCalculator.lrMixMixNuevo(mix1, None, mix2, None, frequencyTable, 3, 2, None, 0.0, 0.005, 0)
      val resultWithDropInAndDropOut = LRMixCalculator.lrMixMixNuevo(mix1, None, mix2, None, frequencyTable, 3, 2, None, 0.0, 0.005, 0.05)
      val resultWithTheta = LRMixCalculator.lrMixMixNuevo(mix1, None, mix2, None, frequencyTable, 3, 2, None, 0.02, 0, 0)
      val resultWithThetaAndDropOut = LRMixCalculator.lrMixMixNuevo(mix1, None, mix2, None, frequencyTable, 3, 2, None, 0.02, 0, 0.05)
      val resultWithThetaAndDropIn = LRMixCalculator.lrMixMixNuevo(mix1, None, mix2, None, frequencyTable, 3, 2, None, 0.02, 0.005, 0)
      val resultWithThetaAndDropInAndDropOut = LRMixCalculator.lrMixMixNuevo(mix1, None, mix2, None, frequencyTable, 3, 2, None, 0.02, 0.005, 0.05)

      resultWithoutDropoutDropInTheta.detailed("DUMMY").get mustBe 2.314 +- 0.01
      resultWithoutDropoutDropInTheta.total mustBe 2.314 +- 0.01

      resultWithDropOut.total mustBe 2.239 +- 0.01
      resultWithDropIn.total mustBe 2.312 +-0.01
      resultWithDropInAndDropOut.total mustBe 2.236 +- 0.01
      resultWithTheta.total mustBe 2.434 +- 0.01
      resultWithThetaAndDropOut.total mustBe 2.37 +- 0.01
      resultWithThetaAndDropIn.total mustBe 2.431 +- 0.01
      resultWithThetaAndDropInAndDropOut.total mustBe 2.367 +- 0.001
    }

    "work with profile associated" in {

      val frequencyTable = Map(
        ("DUMMY", BigDecimal(1)) -> 0.09194810,
        ("DUMMY", BigDecimal(2)) -> 0.01176686,
        ("DUMMY", BigDecimal(3)) -> 0.10626805,
        ("DUMMY", BigDecimal(4)) -> 0.17218569,
        ("DUMMY", BigDecimal(5)) -> 0.10083325,
        ("DUMMY", BigDecimal(6)) -> 0.04264576,
        ("DUMMY", BigDecimal(7)) -> 0.06630600,
        ("DUMMY", BigDecimal(8)) -> 0.22168813,
        ("DUMMY", BigDecimal(9)) -> 0.18635815)

//      M1: 1 2 3 7 9  M2: 4 5 7 9  K1: 1 2  K2:  n1: 3  n2: 2
      val mix1 = Map("DUMMY" -> List(Allele(1),Allele(2),Allele(3),Allele(7),Allele(9)))
      val mix2 = Map("DUMMY" -> List(Allele(4),Allele(5),Allele(7),Allele(9)))
      val victim1 = Map("DUMMY" -> List(Allele(1),Allele(2)))

      val resultWithoutDopInDropOutAndTheta = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, None, frequencyTable, 3, 2, None, 0.0, 0.0, 0.0)
      val resultWithDropOut = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, None, frequencyTable, 3, 2, None, 0, 0.0, 0.05)
      val resultWithDropIn = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, None, frequencyTable, 3, 2, None, 0, 0.005, 0)
      val resultWithDropInAndDropOut = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, None, frequencyTable, 3, 2, None, 0, 0.005, 0.05)
      val resultWithTheta = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, None, frequencyTable, 3, 2, None, 0.02, 0.0, 0.0)
      val resultWithThetaAndDropOut = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, None, frequencyTable, 3, 2, None, 0.02, 0.0, 0.05)
      val resultWithThetaAndDropIn = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, None, frequencyTable, 3, 2, None, 0.02, 0.005, 0.0)
      val resultWithThetaAndDropInAndDropOut = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, None, frequencyTable, 3, 2, None, 0.02, 0.005, 0.05)

      resultWithoutDopInDropOutAndTheta.detailed("DUMMY").get mustBe 1.625 +- 0.01
      resultWithoutDopInDropOutAndTheta.total mustBe 1.625 +- 0.01

      resultWithDropOut.total mustBe 1.584 +-0.01
      resultWithDropIn.total mustBe 1.624 +- 0.01
      resultWithDropInAndDropOut.total mustBe 1.582 +- 0.01
      resultWithTheta.total mustBe 1.396 +- 0.01
      resultWithThetaAndDropOut.total mustBe 1.374 +- 0.01
      resultWithThetaAndDropIn.total mustBe 1.395 +- 0.01
      resultWithThetaAndDropInAndDropOut.total mustBe 1.372 +- 0.01
    }

    "work with two profiles associated" in {

      val frequencyTable = Map(
        ("DUMMY", BigDecimal(-1)) -> 0.0000001,
        ("DUMMY", BigDecimal(1)) -> 0.09194810,
        ("DUMMY", BigDecimal(2)) -> 0.01176686,
        ("DUMMY", BigDecimal(3)) -> 0.10626805,
        ("DUMMY", BigDecimal(4)) -> 0.17218569,
        ("DUMMY", BigDecimal(5)) -> 0.10083325,
        ("DUMMY", BigDecimal(6)) -> 0.04264576,
        ("DUMMY", BigDecimal(7)) -> 0.06630600,
        ("DUMMY", BigDecimal(8)) -> 0.22168813,
        ("DUMMY", BigDecimal(9)) -> 0.18635815)

//      M1: 1 2 5 7 9  M2: 4 5 7 9  K1: 1 2  K2: 4 7  n1: 3  n2: 2
      val mix1 = Map("DUMMY" -> List(Allele(1),Allele(2),Allele(5),Allele(7),Allele(9)))
      val mix2 = Map("DUMMY" -> List(Allele(4),Allele(5),Allele(7),Allele(9)))
      val victim1 = Map("DUMMY" -> List(Allele(1),Allele(2)))
      val victim2 = Map("DUMMY" -> List(Allele(4),Allele(7)))

      val resultWithoutDopInDropOutAndTheta = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, Some(victim2), frequencyTable, 3, 2, None, 0.0, 0.0, 0.0)
      val resultWithDropOut = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, Some(victim2), frequencyTable, 3, 2, None, 0, 0.0, 0.05)
      val resultWithDropIn = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, Some(victim2), frequencyTable, 3, 2, None, 0, 0.005, 0)
      val resultWithDropInAndDropOut = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, Some(victim2), frequencyTable, 3, 2, None, 0, 0.005, 0.05)
      val resultWithTheta = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, Some(victim2), frequencyTable, 3, 2, None, 0.02, 0.0, 0.0)
      val resultWithThetaAndDropOut = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, Some(victim2), frequencyTable, 3, 2, None, 0.02, 0.0, 0.05)
      val resultWithThetaAndDropIn = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, Some(victim2), frequencyTable, 3, 2, None, 0.02, 0.005, 0.0)
      val resultWithThetaAndDropInAndDropOut = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, Some(victim2), frequencyTable, 3, 2, None, 0.02, 0.005, 0.05)

      resultWithoutDopInDropOutAndTheta.detailed("DUMMY").get mustBe 6.705 +- 0.01
      resultWithoutDopInDropOutAndTheta.total mustBe 6.705 +- 0.01

      resultWithDropOut.total mustBe 6.514 +-0.01
      resultWithDropIn.total mustBe 6.691 +- 0.01
      resultWithDropInAndDropOut.total mustBe 6.496 +- 0.01
      resultWithTheta.total mustBe 7.618 +- 0.01
      resultWithThetaAndDropOut.total mustBe 7.459 +- 0.01
      resultWithThetaAndDropIn.total mustBe 7.595 +- 0.01
      resultWithThetaAndDropInAndDropOut.total mustBe 7.43 +- 0.01
    }

  }

  "Mix mix New 2 2" should {
    "work without profile associated" in {

      val frequencyTable = Map(
        ("DUMMY", BigDecimal(1)) -> 0.09194810,
        ("DUMMY", BigDecimal(2)) -> 0.01176686,
        ("DUMMY", BigDecimal(3)) -> 0.10626805,
        ("DUMMY", BigDecimal(4)) -> 0.17218569,
        ("DUMMY", BigDecimal(5)) -> 0.10083325,
        ("DUMMY", BigDecimal(6)) -> 0.04264576,
        ("DUMMY", BigDecimal(7)) -> 0.06630600,
        ("DUMMY", BigDecimal(8)) -> 0.22168813,
        ("DUMMY", BigDecimal(9)) -> 0.18635815)

//      M1: 5 7 9  M2: 4 7 9  K1:  K2:  n1: 2  n2: 2
      val mix1 = Map("DUMMY" -> List(Allele(5),Allele(7),Allele(9)))
      val mix2 = Map("DUMMY" -> List(Allele(4),Allele(7),Allele(9)))

      val resultWithoutDropoutDropInTheta = LRMixCalculator.lrMixMixNuevo(mix1, None, mix2, None, frequencyTable, 2, 2, None, 0.0, 0, 0)
      val resultWithDropOut = LRMixCalculator.lrMixMixNuevo(mix1, None, mix2, None, frequencyTable, 2, 2, None, 0.0, 0, 0.05)
      val resultWithDropIn = LRMixCalculator.lrMixMixNuevo(mix1, None, mix2, None, frequencyTable, 2, 2, None, 0.0, 0.005, 0)
      val resultWithDropInAndDropOut = LRMixCalculator.lrMixMixNuevo(mix1, None, mix2, None, frequencyTable, 2, 2, None, 0.0, 0.005, 0.05)
      val resultWithTheta = LRMixCalculator.lrMixMixNuevo(mix1, None, mix2, None, frequencyTable, 2, 2, None, 0.02, 0, 0)
      val resultWithThetaAndDropOut = LRMixCalculator.lrMixMixNuevo(mix1, None, mix2, None, frequencyTable, 2, 2, None, 0.02, 0, 0.05)
      val resultWithThetaAndDropIn = LRMixCalculator.lrMixMixNuevo(mix1, None, mix2, None, frequencyTable, 2, 2, None, 0.02, 0.005, 0)
      val resultWithThetaAndDropInAndDropOut = LRMixCalculator.lrMixMixNuevo(mix1, None, mix2, None, frequencyTable, 2, 2, None, 0.02, 0.005, 0.05)

      resultWithoutDropoutDropInTheta.detailed("DUMMY").get mustBe 3.444 +- 0.01
      resultWithoutDropoutDropInTheta.total mustBe 3.444 +- 0.01

      resultWithDropOut.total mustBe 3.04 +- 0.01
      resultWithDropIn.total mustBe 3.443 +-0.01
      resultWithDropInAndDropOut.total mustBe 3.038 +- 0.01
      resultWithTheta.total mustBe 3.415 +- 0.01
      resultWithThetaAndDropOut.total mustBe 3.06 +- 0.01
      resultWithThetaAndDropIn.total mustBe 3.414 +- 0.01
      resultWithThetaAndDropInAndDropOut.total mustBe 3.058 +- 0.001
    }

    "work with profile associated" in {

      val frequencyTable = Map(
        ("DUMMY", BigDecimal(1)) -> 0.09194810,
        ("DUMMY", BigDecimal(2)) -> 0.01176686,
        ("DUMMY", BigDecimal(3)) -> 0.10626805,
        ("DUMMY", BigDecimal(4)) -> 0.17218569,
        ("DUMMY", BigDecimal(5)) -> 0.10083325,
        ("DUMMY", BigDecimal(6)) -> 0.04264576,
        ("DUMMY", BigDecimal(7)) -> 0.06630600,
        ("DUMMY", BigDecimal(8)) -> 0.22168813,
        ("DUMMY", BigDecimal(9)) -> 0.18635815)

//      M1: 1 2  M2: 2 5 7  K1: 1 2  K2:  n1: 2  n2: 2
      val mix1 = Map("DUMMY" -> List(Allele(1),Allele(2)))
      val mix2 = Map("DUMMY" -> List(Allele(2),Allele(5),Allele(7)))
      val victim1 = Map("DUMMY" -> List(Allele(1),Allele(2)))

      val resultWithoutDopInDropOutAndTheta = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, None, frequencyTable, 2, 2, None, 0.0, 0.0, 0.0)
      val resultWithDropOut = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, None, frequencyTable, 2, 2, None, 0, 0.0, 0.05)
      val resultWithDropIn = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, None, frequencyTable, 2, 2, None, 0, 0.005, 0)
      val resultWithDropInAndDropOut = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, None, frequencyTable, 2, 2, None, 0, 0.005, 0.05)
      val resultWithTheta = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, None, frequencyTable, 2, 2, None, 0.02, 0.0, 0.0)
      val resultWithThetaAndDropOut = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, None, frequencyTable, 2, 2, None, 0.02, 0.0, 0.05)
      val resultWithThetaAndDropIn = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, None, frequencyTable, 2, 2, None, 0.02, 0.005, 0.0)
      val resultWithThetaAndDropInAndDropOut = LRMixCalculator.lrMixMixNuevo(mix1, Some(victim1), mix2, None, frequencyTable, 2, 2, None, 0.02, 0.005, 0.05)

      resultWithoutDopInDropOutAndTheta.detailed("DUMMY").get mustBe 1.019 +- 0.01
      resultWithoutDopInDropOutAndTheta.total mustBe 1.019 +- 0.01

      resultWithDropOut.total mustBe 0.3461 +-0.01
      resultWithDropIn.total mustBe 1.019 +- 0.01
      resultWithDropInAndDropOut.total mustBe 0.3462 +- 0.01
      resultWithTheta.total mustBe 3.958 +- 0.01
      resultWithThetaAndDropOut.total mustBe 1.822 +- 0.01
      resultWithThetaAndDropIn.total mustBe 3.961 +- 0.01
      resultWithThetaAndDropInAndDropOut.total mustBe 1.824 +- 0.01
    }

  }

}
