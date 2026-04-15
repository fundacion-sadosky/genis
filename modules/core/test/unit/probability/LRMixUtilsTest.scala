package unit.probability

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

import matching.AleleRange
import probability.{LRMixCalculator, LRResult, NoFrequencyException}
import profile.{Allele, AlleleValue, MicroVariant, OutOfLadderAllele, XY}

class LRMixUtilsTest extends AnyWordSpec with Matchers {

  private val frequencyTable = Map(
    ("D3S1358", BigDecimal(-1)) -> 0.0001,
    ("D3S1358", BigDecimal(14)) -> 0.123,
    ("D3S1358", BigDecimal(15)) -> 0.250,
    ("D3S1358", BigDecimal(16)) -> 0.175
  )

  // ---------------------------------------------------------------------------
  // transformAlleleValues
  // ---------------------------------------------------------------------------

  "LRMixCalculator.transformAlleleValues" must {

    "convert Allele(v) to v.toDouble" in {
      LRMixCalculator.transformAlleleValues(Array(Allele(14), Allele(15))).toSeq mustBe Seq(14.0, 15.0)
    }

    "convert OutOfLadderAllele to -1.0" in {
      LRMixCalculator.transformAlleleValues(Array(OutOfLadderAllele(BigDecimal(14), ">"))) .toSeq mustBe Seq(-1.0)
    }

    "convert MicroVariant to -1.0" in {
      LRMixCalculator.transformAlleleValues(Array(MicroVariant(9))).toSeq mustBe Seq(-1.0)
    }

    "handle mixed allele types" in {
      val input  = Array(Allele(16), OutOfLadderAllele(BigDecimal(22), ">"), MicroVariant(9))
      val result = LRMixCalculator.transformAlleleValues(input)
      result.toSeq mustBe Seq(16.0, -1.0, -1.0)
    }

    "return empty array for empty input" in {
      LRMixCalculator.transformAlleleValues(Array.empty[profile.AlleleValue]).toSeq mustBe Seq.empty
    }
  }

  // ---------------------------------------------------------------------------
  // pRepOut
  // ---------------------------------------------------------------------------

  "LRMixCalculator.pRepOut" must {

    "return 1.0 when sample and participants share all alleles and no drop-out needed" in {
      // sample == participants → no dropOuts, all common
      val sample       = Array(14.0, 15.0)
      val participants = Array(14.0, 15.0)
      val occurrences  = Map(14.0 -> 1, 15.0 -> 1)
      val pOut         = 0.01
      // productCommon = (1 - pOut^1) * (1 - pOut^1) = (0.99)^2, productDropOut = 1 (empty)
      val expected     = (1 - math.pow(pOut, 1)) * (1 - math.pow(pOut, 1))
      LRMixCalculator.pRepOut(pOut)(sample, participants)(occurrences) mustBe (expected +- 1e-10)
    }

    "compute drop-out penalty for alleles in participants not in sample" in {
      val sample       = Array(14.0)
      val participants = Array(14.0, 15.0)
      val occurrences  = Map(14.0 -> 1, 15.0 -> 1)
      val pOut         = 0.05
      // common = [14.0], dropout = [15.0]
      val productCommon  = 1 - LRMixCalculator.pow(pOut, 1)
      val productDropOut = LRMixCalculator.pow(pOut, 1)
      LRMixCalculator.pRepOut(pOut)(sample, participants)(occurrences) mustBe (productCommon * productDropOut +- 1e-10)
    }
  }

  // ---------------------------------------------------------------------------
  // pRepIn
  // ---------------------------------------------------------------------------

  "LRMixCalculator.pRepIn" must {

    "return (1 - pIn) when there are no drop-ins" in {
      val sample       = Array(14.0, 15.0)
      val participants = Array(14.0, 15.0)
      val pIn          = 0.01
      LRMixCalculator.pRepIn(pIn, frequencyTable, "D3S1358")(sample, participants) mustBe ((1 - pIn) +- 1e-10)
    }

    "compute drop-in probability when sample has alleles not in participants" in {
      val sample       = Array(14.0, 15.0, 16.0)
      val participants = Array(14.0, 15.0)
      // dropIn = [16.0], freq = 0.175
      val pIn          = 0.05
      val expected     = math.pow(pIn, 1) * frequencyTable(("D3S1358", BigDecimal(16)))
      LRMixCalculator.pRepIn(pIn, frequencyTable, "D3S1358")(sample, participants) mustBe (expected +- 1e-10)
    }

    "throw NoFrequencyException when drop-in allele has no frequency and no -1 fallback" in {
      val noFallbackTable = Map(
        ("D3S1358", BigDecimal(14)) -> 0.123
      )
      val sample       = Array(14.0, 99.0)
      val participants = Array(14.0)
      intercept[NoFrequencyException] {
        LRMixCalculator.pRepIn(0.05, noFallbackTable, "D3S1358")(sample, participants)
      }
    }
  }

  // ---------------------------------------------------------------------------
  // pRep
  // ---------------------------------------------------------------------------

  "LRMixCalculator.pRep" must {

    "equal pRepOut * pRepIn" in {
      val sample       = Array(14.0, 15.0)
      val participants = Array(14.0, 15.0)
      val occurrences  = Map(14.0 -> 1, 15.0 -> 1)
      val pIn          = 0.01
      val pOut         = 0.01

      val expectedOut = LRMixCalculator.pRepOut(pOut)(sample, participants)(occurrences)
      val expectedIn  = LRMixCalculator.pRepIn(pIn, frequencyTable, "D3S1358")(sample, participants)
      val actual      = LRMixCalculator.pRep(pIn, pOut, frequencyTable, "D3S1358")(sample, participants)(occurrences)

      actual mustBe (expectedOut * expectedIn +- 1e-10)
    }
  }

  // ---------------------------------------------------------------------------
  // factorial
  // ---------------------------------------------------------------------------

  "LRMixCalculator.factorial" must {

    "return 1 for 0" in {
      LRMixCalculator.factorial(0) mustBe 1
    }

    "return 1 for 1" in {
      LRMixCalculator.factorial(1) mustBe 1
    }

    "return 120 for 5" in {
      LRMixCalculator.factorial(5) mustBe 120
    }

    "return 479001600 for 12 (last cached value)" in {
      LRMixCalculator.factorial(12) mustBe 479001600
    }
  }

  // ---------------------------------------------------------------------------
  // getOccurrencesMap
  // ---------------------------------------------------------------------------

  "LRMixCalculator.getOccurrencesMap" must {

    "count occurrences of each allele" in {
      val result = LRMixCalculator.getOccurrencesMap(Array(1.0, 2.0, 1.0, 3.0, 2.0, 1.0))
      result mustBe Map(1.0 -> 3, 2.0 -> 2, 3.0 -> 1)
    }

    "return empty map for empty array" in {
      LRMixCalculator.getOccurrencesMap(Array.empty[Double]) mustBe Map.empty[Double, Int]
    }

    "return single entry for uniform array" in {
      LRMixCalculator.getOccurrencesMap(Array(5.0, 5.0, 5.0)) mustBe Map(5.0 -> 3)
    }
  }

  // ---------------------------------------------------------------------------
  // addFrequencyOfDefault
  // ---------------------------------------------------------------------------

  "LRMixCalculator.addFrequencyOfDefault" must {

    "sum unobserved allele frequencies into the default entry" in {
      // Observed: 14.0, 15.0, 666.0. Not observed (excl -1): 16→0.175
      val observed = Array(14.0, 15.0, 666.0)
      val result = LRMixCalculator.addFrequencyOfDefault(BigDecimal(666.0), observed, "D3S1358", frequencyTable)
      result(("D3S1358", BigDecimal(666.0))) mustBe (0.175 +- 1e-9)
    }

    "sum all non-observed alleles when none are observed" in {
      // Observed: only 666.0. Not observed: 14→0.123, 15→0.250, 16→0.175 = 0.548
      val observed = Array(666.0)
      val result = LRMixCalculator.addFrequencyOfDefault(BigDecimal(666.0), observed, "D3S1358", frequencyTable)
      result(("D3S1358", BigDecimal(666.0))) mustBe (0.548 +- 1e-9)
    }

    "return 0 for default when all alleles are observed" in {
      val observed = Array(14.0, 15.0, 16.0, 666.0)
      val result = LRMixCalculator.addFrequencyOfDefault(BigDecimal(666.0), observed, "D3S1358", frequencyTable)
      result(("D3S1358", BigDecimal(666.0))) mustBe (0.0 +- 1e-9)
    }

    "preserve existing frequency table entries" in {
      val observed = Array(14.0, 666.0)
      val result = LRMixCalculator.addFrequencyOfDefault(BigDecimal(666.0), observed, "D3S1358", frequencyTable)
      result(("D3S1358", BigDecimal(14))) mustBe frequencyTable(("D3S1358", BigDecimal(14)))
    }
  }

  // ---------------------------------------------------------------------------
  // alleleValues
  // ---------------------------------------------------------------------------

  "LRMixCalculator.alleleValues" must {

    "return all allele values for a marker excluding -1 sentinel" in {
      val result = LRMixCalculator.alleleValues(frequencyTable, "D3S1358").sorted
      result mustBe Array(14.0, 15.0, 16.0)
    }

    "return empty array for unknown marker" in {
      LRMixCalculator.alleleValues(frequencyTable, "UNKNOWN") mustBe Array.empty[Double]
    }
  }

  // ---------------------------------------------------------------------------
  // calculateRepetitionFactor
  // ---------------------------------------------------------------------------

  "LRMixCalculator.calculateRepetitionFactor" must {

    "compute n!/prod(ki!) for distinct elements → 2!/1!*1! = 2" in {
      LRMixCalculator.calculateRepetitionFactor(Array(Array(1.0, 2.0))) mustBe 2.0
    }

    "compute 1 for identical elements → 2!/2! = 1" in {
      LRMixCalculator.calculateRepetitionFactor(Array(Array(1.0, 1.0))) mustBe 1.0
    }

    "multiply factors across multiple genotypes" in {
      // [1,2] → 2, [3,3] → 1 → total = 2
      LRMixCalculator.calculateRepetitionFactor(Array(Array(1.0, 2.0), Array(3.0, 3.0))) mustBe 2.0
    }

    "compute 6 for 3 distinct elements → 3!/1!*1!*1! = 6" in {
      LRMixCalculator.calculateRepetitionFactor(Array(Array(1.0, 2.0, 3.0))) mustBe 6.0
    }

    "compute 3 for [1,1,2] → 3!/2!*1! = 3" in {
      LRMixCalculator.calculateRepetitionFactor(Array(Array(1.0, 1.0, 2.0))) mustBe 3.0
    }
  }

  // ---------------------------------------------------------------------------
  // convertSingleAlele
  // ---------------------------------------------------------------------------

  // NOTE: OutOfLadderAllele and MicroVariant override equals to always return false,
  // so we use pattern matching to verify results instead of mustBe equality.

  "LRMixCalculator.convertSingleAlele" must {

    val range = AleleRange(10, 20)

    "return Allele unchanged when within range" in {
      LRMixCalculator.convertSingleAlele(Allele(15), range) mustBe Allele(15)
    }

    "convert Allele below range to OutOfLadderAllele(<)" in {
      LRMixCalculator.convertSingleAlele(Allele(5), range) match {
        case OutOfLadderAllele(base, sign) =>
          base mustBe range.min
          sign mustBe "<"
        case other => fail(s"Expected OutOfLadderAllele, got $other")
      }
    }

    "convert Allele above range to OutOfLadderAllele(>)" in {
      LRMixCalculator.convertSingleAlele(Allele(25), range) match {
        case OutOfLadderAllele(base, sign) =>
          base mustBe range.max
          sign mustBe ">"
        case other => fail(s"Expected OutOfLadderAllele, got $other")
      }
    }

    "keep Allele at range boundary unchanged" in {
      LRMixCalculator.convertSingleAlele(Allele(10), range) mustBe Allele(10)
      LRMixCalculator.convertSingleAlele(Allele(20), range) mustBe Allele(20)
    }

    "clamp OutOfLadderAllele(>) to range max" in {
      LRMixCalculator.convertSingleAlele(OutOfLadderAllele(25, ">"), range) match {
        case OutOfLadderAllele(base, sign) =>
          base mustBe range.max
          sign mustBe ">"
        case other => fail(s"Expected OutOfLadderAllele, got $other")
      }
    }

    "clamp OutOfLadderAllele(<) to range min" in {
      LRMixCalculator.convertSingleAlele(OutOfLadderAllele(5, "<"), range) match {
        case OutOfLadderAllele(base, sign) =>
          base mustBe range.min
          sign mustBe "<"
        case other => fail(s"Expected OutOfLadderAllele, got $other")
      }
    }

    "convert MicroVariant below range to OutOfLadderAllele(<)" in {
      LRMixCalculator.convertSingleAlele(MicroVariant(8), range) match {
        case OutOfLadderAllele(base, sign) =>
          base mustBe range.min
          sign mustBe "<"
        case other => fail(s"Expected OutOfLadderAllele, got $other")
      }
    }

    "convert MicroVariant above range to OutOfLadderAllele(>)" in {
      LRMixCalculator.convertSingleAlele(MicroVariant(21), range) match {
        case OutOfLadderAllele(base, sign) =>
          base mustBe range.max
          sign mustBe ">"
        case other => fail(s"Expected OutOfLadderAllele, got $other")
      }
    }

    "keep MicroVariant within range unchanged" in {
      LRMixCalculator.convertSingleAlele(MicroVariant(15), range) match {
        case MicroVariant(count) =>
          count mustBe 15
        case other => fail(s"Expected MicroVariant, got $other")
      }
    }

    "pass through XY unchanged" in {
      LRMixCalculator.convertSingleAlele(XY(1), range) mustBe XY(1)
    }
  }

  // ---------------------------------------------------------------------------
  // convertAllelesValues
  // ---------------------------------------------------------------------------

  "LRMixCalculator.convertAllelesValues" must {

    "return alleles unchanged when no ranges provided" in {
      val alleles = Array[AlleleValue](Allele(5), Allele(25))
      val result = LRMixCalculator.convertAllelesValues(alleles, "D3S1358", None)
      result.toSeq mustBe Seq(Allele(5), Allele(25))
    }

    "convert alleles when ranges are provided" in {
      val alleles = Array[AlleleValue](Allele(5), Allele(15), Allele(25))
      val ranges = Map("D3S1358" -> AleleRange(10, 20))
      val result = LRMixCalculator.convertAllelesValues(alleles, "D3S1358", Some(ranges))
      result(0) match {
        case OutOfLadderAllele(base, sign) =>
          base mustBe BigDecimal(10)
          sign mustBe "<"
        case other => fail(s"Expected OutOfLadderAllele, got $other")
      }
      result(1) mustBe Allele(15)
      result(2) match {
        case OutOfLadderAllele(base, sign) =>
          base mustBe BigDecimal(20)
          sign mustBe ">"
        case other => fail(s"Expected OutOfLadderAllele, got $other")
      }
    }

    "use default range (0,99) when marker not in range map" in {
      val alleles = Array[AlleleValue](Allele(5))
      val ranges = Map("OTHER" -> AleleRange(10, 20))
      val result = LRMixCalculator.convertAllelesValues(alleles, "D3S1358", Some(ranges))
      result(0) mustBe Allele(5)
    }
  }

  // ---------------------------------------------------------------------------
  // pow
  // ---------------------------------------------------------------------------

  "LRMixCalculator.pow" must {

    "compute x^0 = 1" in {
      LRMixCalculator.pow(5.0, 0) mustBe 1.0
    }

    "compute x^1 = x" in {
      LRMixCalculator.pow(3.0, 1) mustBe 3.0
    }

    "compute x^3 correctly" in {
      LRMixCalculator.pow(2.0, 3) mustBe 8.0
    }
  }

  // ---------------------------------------------------------------------------
  // LRResult: JSON serialization (custom Double as String)
  // ---------------------------------------------------------------------------

  "LRResult JSON serialization" must {

    "serialize Double total as a JSON string" in {
      val lr   = LRResult(3.14, Map.empty)
      val json = Json.toJson(lr)
      (json \ "total").as[String] mustBe "3.14"
    }

    "serialize detailed markers as strings, None as null" in {
      val lr   = LRResult(1.0, Map("TPOX" -> Some(1.5), "D3S1358" -> None))
      val json = Json.toJson(lr)
      (json \ "detailed" \ "TPOX").as[String] mustBe "1.5"
      // None is serialized as JsNull
      (json \ "detailed" \ "D3S1358").toOption mustBe Some(play.api.libs.json.JsNull)
    }

    "round-trip serialize and deserialize LRResult" in {
      val original = LRResult(2.718, Map("TPOX" -> Some(1.23)))
      val json     = Json.toJson(original)
      val decoded  = json.as[LRResult]
      decoded.total mustBe (original.total +- 1e-10)
      decoded.detailed("TPOX").get mustBe (1.23 +- 1e-10)
    }

    "deserialize LRResult where total is a JSON string" in {
      val json = Json.parse("""{"total":"5.0","detailed":{}}""")
      val lr   = json.as[LRResult]
      lr.total mustBe (5.0 +- 1e-10)
      lr.detailed mustBe Map.empty
    }
  }
}
