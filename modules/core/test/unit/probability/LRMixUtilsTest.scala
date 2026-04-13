package unit.probability

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

import probability.{LRMixCalculator, LRResult, NoFrequencyException}
import profile.{Allele, MicroVariant, OutOfLadderAllele}

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
