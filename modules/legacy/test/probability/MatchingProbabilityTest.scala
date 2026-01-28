package probability

import org.apache.hadoop.mapred.JobConf
import org.scalatestplus.play.PlaySpec
import profile.Profile
import reactivemongo.core.commands.Aggregate
import profile.Allele
import scala.math.BigDecimal.double2bigDecimal
import scala.math.BigDecimal.int2bigDecimal
import org.specs2.matcher.ValueCheck.valueIsTypedValueCheck
import org.specs2.mutable.Specification
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat
import reactivemongo.bson.BSONObjectID
import stubs.Stubs
import play.api.libs.json.JsUndefined
import probability.PValueCalculator._
import scala.io.Source
import types.SampleCode
import profile.Analysis
import java.util.Date
import types.AlphanumericId

class MatchingProbabilityTest extends Specification {

  val analyses: Option[List[Analysis]] = stubs.Stubs.analyses

  val frequencyTable = {

    val pbf = Stubs.populationBaseFrequency

    val list = pbf.base map { sample => ((sample.marker, BigDecimal(sample.allele)), sample.frequency.toDouble) }

    list.toMap[(String, BigDecimal), Double]
  }

  "frequency file" should {
    "read ok" in {

      val p = getProbability(frequencyTable)("TPOX", 10) 

      println(p)
      "" must not beNull
    }
  }

  "frequency" should {
    "be calculated for offlader" in {

      val frequencyTable1 = Map(("TPOX", BigDecimal(10)) -> 0.5, ("TPOX", BigDecimal(12)) -> 0.5)

      val p = getProbability(frequencyTable1)("TH01", 1) 

      p must not beNull
    }
  }

//  "pval HW" should {
//    "work ok for example 1" in {
//
//      val hw = new HardyWeinbergCalculationProbability
//      val markers = Set("TPOX", "PEPE")
//      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"), Map("TPOX" -> List(Allele(BigDecimal(10)), Allele(BigDecimal(12)))),
//        analyses, None, None, None, None, false, true)
//
//      val frequencyTable1 = Map(("TPOX", BigDecimal(10)) -> 0.5, ("TPOX", BigDecimal(12)) -> 0.5)
//
//      val x1 = autosomalRMP(frequencyTable1)(hw)(markers, p1)
//
//      val expected = Map("TPOX" -> Some(0.5))
//      x1 must beEqualTo(expected)
//
//    }
//  }
//
//  "pval HW" should {
//    "work ok for example 2" in {
//
//      val hw = new HardyWeinbergCalculationProbability
//      val markers = Set("TPOX", "PEPE")
//
//      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"), Map("TPOX" -> List(Allele(BigDecimal(10)), Allele(BigDecimal(13)))),
//        analyses, None, None, None, None, false, true)
//
//      val frequencyTable2 = Map(("TPOX", BigDecimal(10)) -> 0.2, ("TPOX", BigDecimal(12)) -> 0.5, ("TPOX", BigDecimal(-1)) -> 0.001)
//
//      val x2 = autosomalRMP(frequencyTable2)(hw)(markers, p2)
//
//      val expected = Map("TPOX" -> Some(0.0004))
//      x2 must beEqualTo(expected)
//
//    }
//  }
//
//  "pval HW" should {
//    "work ok for example 3" in {
//
//      val hw = new HardyWeinbergCalculationProbability
//      val markers = Set("TPOX", "PEPE")
//
//      val p3 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"), Map("TPOX" -> List(Allele(BigDecimal(10)), Allele(BigDecimal(12))),
//        "PEPE" -> List(Allele(BigDecimal(20)), Allele(BigDecimal(22)))),
//        analyses, None, None, None, None, false, true)
//
//      val frequencyTable3 = Map(("TPOX", BigDecimal(10)) -> 0.2,
//        ("TPOX", BigDecimal(12)) -> 0.5,
//        ("PEPE", BigDecimal(20)) -> 0.3,
//        ("PEPE", BigDecimal(22)) -> 0.7)
//
//      val x3 = autosomalRMP(frequencyTable3)(hw)(markers, p3)
//
//      val expected = Map("TPOX" -> Some(0.2), "PEPE" -> Some(0.42))
//      x3 must beEqualTo(expected)
//    }
//  }

  "mixmix calculus" should {
    val frequencyTable3 = Map(
      ("TPOX", BigDecimal(1)) -> 0.03,
      ("TPOX", BigDecimal(2)) -> 0.1,
      ("TPOX", BigDecimal(3)) -> 0.3,
      ("TPOX", BigDecimal(4)) -> 0.23,
      ("TPOX", BigDecimal(5)) -> 0.12,
      ("TPOX", BigDecimal(6)) -> 0.22)

    val hw = new HardyWeinbergCalculationProbability

    sealed case class Scenario(ma: Seq[Int], mb: Seq[Int], va: Option[Seq[Int]], vb: Option[Seq[Int]], p3: Option[Double], p2a: Option[Double], p2b: Option[Double])

    val scenarios = List[Scenario](       
        Scenario(Seq(1, 2, 3), Seq(1, 2, 3, 4), None, None,                               Some(0.00022), Some(0.0046), Some(0.005))
          ,Scenario(Seq(1, 2, 3), Seq(1, 2, 3, 4), Some(Seq(1, 3)), None,                    Some(0.0017), Some(0.076), Some(0.005))
          ,Scenario(Seq(1, 2, 3), Seq(1, 2, 4),    None, None,                               Some(0.00012), Some(0.0046), Some(0.003))
          ,Scenario(Seq(1, 2, 3), Seq(1, 2, 4),    Some(Seq(1, 1)), None,                    None, Some(0.06), Some(0.003))
          ,Scenario(Seq(1, 2, 3), Seq(1, 2, 4),    Some(Seq(1)), None,                       Some(0.00024), Some(0.041), Some(0.003))
          ,Scenario(Seq(1, 2, 3), Seq(1, 2, 4),    Some(Seq(3)), None,                       Some(3e-04), Some(0.0066), Some(0.003))
          ,Scenario(Seq(1, 2, 3), Seq(1, 2, 3, 4), Some(Seq(1, 4)), None,                    None, None, Some(0.005))
          ,Scenario(Seq(1, 2, 3), Seq(1, 2, 3, 4), None, Some(Seq(1, 4)),                    Some(0.0015), Some(0.0046), Some(0.06))
          ,Scenario(Seq(1, 2, 3), Seq(1, 2, 5),    None, None,                               Some(4.9e-05), Some(0.0046), Some(0.0011))
          ,Scenario(Seq(1, 2, 3), Seq(1, 2, 5),    Some(Seq(1, 1)), None,                    None, Some(0.06), Some(0.0011))
          ,Scenario(Seq(1, 2, 3), Seq(1, 2, 5),    Some(Seq(1)), None,                       Some(1e-04), Some(0.041), Some(0.0011))
          ,Scenario(Seq(1, 2, 3), Seq(1, 2, 5),    Some(Seq(2, 3)), None,                    Some(3e-04), Some(0.025), Some(0.0011))
          ,Scenario(Seq(1),       Seq(1, 2, 3),    None, None,                               Some(4.9e-08), Some(8.1E-7), Some(0.0046))
          ,Scenario(Seq(1),       Seq(1, 2, 3),    Some(Seq(1, 2)), None,                    None, None, Some(0.0046))
          ,Scenario(Seq(1),       Seq(1, 2, 3),    Some(Seq(1, 1)), Some(Seq(1, 2)),         None, Some(9e-04), Some(0.17))
          ,Scenario(Seq(1),       Seq(1, 2, 3),    Some(Seq(1)), Some(Seq(1, 2)),            None, Some(2.7E-5), Some(0.17))
    )
  

    scenarios.foreach { scenario =>

      "return (" + scenario.p3 + ", " + scenario.p2a + ", " + scenario.p2b +
        ") FOR (ma=" + scenario.ma + ", mb=" + scenario.mb + ", va=" + scenario.va + ", vb=" + scenario.vb in {

          val ma = scenario.ma.map { v => Allele(BigDecimal(v)) }
          val mb = scenario.mb.map { v => Allele(BigDecimal(v)) }
          val va = scenario.va.map { x => x.map { v => Allele(BigDecimal(v)) } }
          val vb = scenario.vb.map { x => x.map { v => Allele(BigDecimal(v)) } }

          val p3 = mixmixH3(frequencyTable3)(hw)("TPOX", ma, va, mb, vb, verbose=true)
          val p2a = mixmixH2(frequencyTable3)(hw)("TPOX", ma, va, verbose=true)
          val p2b = mixmixH2(frequencyTable3)(hw)("TPOX", mb, vb, verbose=true)

          scenario.p3 match {
            case Some(p) => {
              p3 must beSome
              p3.get must beCloseTo(p, p / 10)
            }
            case None => {
              p3 must beNone
            }
          }

          scenario.p2a match {
            case Some(p) => {
              p2a must beSome
              p2a.get must beCloseTo(p, p / 10)
            }
            case None => {
              p2a must beNone
            }
          }

          scenario.p2b match {
            case Some(p) => {
              p2b must beSome
              p2b.get must beCloseTo(p, p / 10)
            }
            case None => {
              p2b must beNone
            }
          }

        }

    }; end

  }

}