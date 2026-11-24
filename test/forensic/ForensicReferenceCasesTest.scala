package forensic

import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeApplication
import probability.{FullCalculationScenario, FullHypothesis, LRMixCalculator}
import profile.Profile.Genotypification
import profile.{Allele, AlleleValue}
import standalone.EngineApp
import types.StatOption

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration._

/** Reference cases for the forensic engine (`LRMixCalculator.calculateLRMix`).
  *
  * Every case states its expected LR as a closed formula, written here from the model
  * described in the README (Balding-Nichols sampling formula, drop-out and drop-in) and
  * evaluated without calling the engine. The engine result is then compared against it,
  * and a summary table is printed at the end of the run.
  *
  * To try a case of your own, copy one of the `check(...)` blocks and change the sample,
  * the hypotheses, the parameters and the formula. */
class ForensicReferenceCasesTest extends FlatSpec with Matchers with OneAppPerSuite with BeforeAndAfterAll {

  implicit override lazy val app: FakeApplication = EngineApp()

  // ---- population data: one table per marker, frequencies add up to 1 -----------------
  // The key -1 is the minimum frequency (fmin) GENis uses for alleles absent from the table.
  private val freq: Map[String, Map[Int, Double]] = Map(
    "M1" -> Map(10 -> 0.10, 11 -> 0.20, 12 -> 0.30, 13 -> 0.15, 14 -> 0.25),
    "M2" -> Map(7 -> 0.05, 8 -> 0.35, 9 -> 0.40, 10 -> 0.20))
  private val fmin = 0.0001

  private val frequencyTable: Map[(String, BigDecimal), Double] =
    freq.flatMap { case (marker, alleles) =>
      alleles.map { case (a, f) => (marker, BigDecimal(a)) -> f } + ((marker, BigDecimal(-1)) -> fmin)
    }

  private def p(marker: String, allele: Int): Double = freq(marker)(allele)

  // ---- helpers ------------------------------------------------------------------------
  private def genotype(loci: (String, Seq[Int])*): Genotypification =
    loci.map { case (m, alleles) => m -> alleles.map(a => Allele(BigDecimal(a)): AlleleValue).toList }.toMap

  private def engineLR(sample: Genotypification, h1: FullHypothesis, h2: FullHypothesis,
                       theta: Double = 0.0, dropIn: Double = 0.0): Double = {
    // calculateLRMix reads only theta and dropIn from StatOption. The table and model names
    // and StatOption.dropOut are not used here: drop-out is set per hypothesis.
    val scenario = FullCalculationScenario(sample, h1, h2, StatOption("table", "model", theta, dropIn, None))
    Await.result(LRMixCalculator.calculateLRMix(scenario, frequencyTable), 60.seconds).total
  }

  private val rows = ArrayBuffer[(String, String, Double, Double)]()

  private def check(name: String, formula: String, expected: Double, obtained: Double): Unit = {
    rows += ((name, formula, expected, obtained))
    obtained shouldBe expected +- (math.abs(expected) * 1e-9)
  }

  override def afterAll(): Unit = {
    val line = "-" * 120
    println(line)
    println(f"${"case"}%-46s ${"expected"}%14s ${"GENis"}%14s ${"rel. diff"}%10s   formula")
    println(line)
    rows.foreach { case (name, formula, expected, obtained) =>
      val diff = math.abs(obtained - expected) / math.abs(expected)
      println(f"$name%-46s $expected%14.6f $obtained%14.6f $diff%10.1e   $formula")
    }
    println(line)
    super.afterAll()
  }

  // ---- the sampling formula on its own (worked examples of the README) ----------------
  private val pCondTable: Map[(String, BigDecimal), Double] = Map(
    ("M", BigDecimal(-1)) -> fmin, ("M", BigDecimal(12)) -> 0.2, ("M", BigDecimal(13)) -> 0.3)
  private def pCond(theta: Double, genotype: Double*): Double =
    LRMixCalculator.pCond(pCondTable, theta)("M", genotype.toArray, Array())._1

  "pCond" should "give p^2 for a homozygote with theta 0 (worked example of the README)" in {
    check("pCond homozygote, theta 0", "p^2", 0.04, pCond(0.0, 12, 12))
  }

  it should "give 2pq for a heterozygote with theta 0 (worked example of the README)" in {
    check("pCond heterozygote, theta 0", "2pq", 0.12, pCond(0.0, 12, 13))
  }

  it should "give p (theta + (1-theta) p) for a homozygote with theta 0.01 (worked example of the README)" in {
    check("pCond homozygote, theta 0.01", "p (theta + (1-theta) p)", 0.2 * (0.01 + 0.99 * 0.2), pCond(0.01, 12, 12))
  }

  // ---- single-source stain against a suspect ------------------------------------------
  // H1: the suspect left the stain.   H2: an unknown, unrelated person did.
  private def singleSource(sample: Genotypification, suspect: Genotypification,
                           theta: Double = 0.0, dropIn: Double = 0.0, dropOut: Double = 0.0): Double =
    engineLR(sample,
      FullHypothesis(Array(suspect), Array(), 0, dropOut),
      FullHypothesis(Array(), Array(suspect), 1, dropOut),
      theta, dropIn)

  "A single-source match" should "give 1/(2 pa pb) for a heterozygote" in {
    val g = genotype("M1" -> Seq(11, 12))
    check("single source, heterozygote", "1 / (2 pa pb)",
      1 / (2 * p("M1", 11) * p("M1", 12)), singleSource(g, g))
  }

  it should "give 1/pa^2 for a homozygote" in {
    check("single source, homozygote", "1 / pa^2",
      1 / (p("M1", 12) * p("M1", 12)),
      singleSource(genotype("M1" -> Seq(12)), genotype("M1" -> Seq(12, 12))))
  }

  it should "apply the theta correction (NRC II 4.10) to a heterozygote" in {
    val t = 0.01
    val (pa, pb) = (p("M1", 11), p("M1", 12))
    val g = genotype("M1" -> Seq(11, 12))
    check("single source, heterozygote, theta 0.01",
      "(1+t)(1+2t) / (2 (t+(1-t)pa) (t+(1-t)pb))",
      (1 + t) * (1 + 2 * t) / (2 * (t + (1 - t) * pa) * (t + (1 - t) * pb)),
      singleSource(g, g, theta = t))
  }

  it should "apply the theta correction (NRC II 4.10) to a homozygote" in {
    val t = 0.01
    val pa = p("M1", 12)
    check("single source, homozygote, theta 0.01",
      "(1+t)(1+2t) / ((2t+(1-t)pa) (3t+(1-t)pa))",
      (1 + t) * (1 + 2 * t) / ((2 * t + (1 - t) * pa) * (3 * t + (1 - t) * pa)),
      singleSource(genotype("M1" -> Seq(12)), genotype("M1" -> Seq(12, 12)), theta = t))
  }

  it should "multiply the LR of each marker" in {
    val g = genotype("M1" -> Seq(11, 12), "M2" -> Seq(8, 9))
    check("single source, two markers", "product of 1 / (2 pa pb)",
      1 / (2 * p("M1", 11) * p("M1", 12)) * 1 / (2 * p("M2", 8) * p("M2", 9)),
      singleSource(g, g))
  }

  // ---- mixtures -----------------------------------------------------------------------
  private val victim = genotype("M1" -> Seq(10, 11))

  "A two-person mixture" should "give 1/(2 pc pd) when the suspect explains the two remaining alleles" in {
    // H1: victim + suspect.   H2: victim + one unknown.
    val suspect = genotype("M1" -> Seq(12, 13))
    val lr = engineLR(genotype("M1" -> Seq(10, 11, 12, 13)),
      FullHypothesis(Array(victim, suspect), Array(), 0, 0.0),
      FullHypothesis(Array(victim), Array(suspect), 1, 0.0))
    check("mixture of 4 alleles, victim known", "1 / (2 pc pd)",
      1 / (2 * p("M1", 12) * p("M1", 13)), lr)
  }

  it should "give 1/(pc (2pa + 2pb + pc)) when only one allele is left to explain" in {
    // Sample a,b,c; victim a,b; suspect a,c. Under H2 the unknown carries c plus a, b or c.
    val suspect = genotype("M1" -> Seq(10, 12))
    val (pa, pb, pc) = (p("M1", 10), p("M1", 11), p("M1", 12))
    val lr = engineLR(genotype("M1" -> Seq(10, 11, 12)),
      FullHypothesis(Array(victim, suspect), Array(), 0, 0.0),
      FullHypothesis(Array(victim), Array(suspect), 1, 0.0))
    check("mixture of 3 alleles, victim known", "1 / (pc (2pa + 2pb + pc))",
      1 / (pc * (2 * pa + 2 * pb + pc)), lr)
  }

  it should "give 1/(24 pa pb pc pd) against two unknowns" in {
    // H1: victim + suspect.   H2: two unknowns.
    val suspect = genotype("M1" -> Seq(12, 13))
    val lr = engineLR(genotype("M1" -> Seq(10, 11, 12, 13)),
      FullHypothesis(Array(victim, suspect), Array(), 0, 0.0),
      FullHypothesis(Array(), Array(victim, suspect), 2, 0.0))
    check("mixture of 4 alleles, two unknowns", "1 / (24 pa pb pc pd)",
      1 / (24 * p("M1", 10) * p("M1", 11) * p("M1", 12) * p("M1", 13)), lr)
  }

  // ---- drop-out and drop-in -----------------------------------------------------------
  "Drop-out" should "explain a suspect allele missing from the sample" in {
    // Sample a; suspect a,b; drop-out probability d under both hypotheses.
    // H1 = d (1-d).   H2 = pa^2 (1-d^2) + 2 pa (1-pa) d (1-d).
    val d = 0.1
    val pa = p("M1", 11)
    check("drop-out: sample a, suspect a,b (d = 0.1)",
      "d(1-d) / (pa^2 (1-d^2) + 2 pa (1-pa) d (1-d))",
      d * (1 - d) / (pa * pa * (1 - d * d) + 2 * pa * (1 - pa) * d * (1 - d)),
      singleSource(genotype("M1" -> Seq(11)), genotype("M1" -> Seq(11, 12)), dropOut = d))
  }

  "Drop-in" should "explain a sample allele the suspect does not carry" in {
    // Sample a,b,c; suspect a,b; drop-in probability c under both hypotheses, no drop-out.
    // H1 = c pc.   H2 = c pa pb pc (6 + c (pa + pb + pc)).
    val c = 0.05
    val (pa, pb, pc) = (p("M1", 10), p("M1", 11), p("M1", 12))
    check("drop-in: sample a,b,c, suspect a,b (c = 0.05)",
      "1 / (pa pb (6 + c (pa + pb + pc)))",
      1 / (pa * pb * (6 + c * (pa + pb + pc))),
      singleSource(genotype("M1" -> Seq(10, 11, 12)), genotype("M1" -> Seq(10, 11)), dropIn = c))
  }
}
