package pedigree

import kits.AnalysisType
import org.scalatest.{FlatSpec, Matchers}
import profile.{Allele, AlleleValue, Profile}
import types.{SampleCode, Sex}

/** Reference cases for the pedigree engine (`BayesianNetwork.calculateProbability`): small
  * pedigrees whose LR can be worked out by hand.
  *
  * How a case is put together:
  *
  *  - The genogram is an array of `Individual`. `idFather` / `idMother` point to the alias of
  *    the parents (`None` for founders). Exactly one individual has `unknown = true`: the
  *    person whose identity is in question. H1: the profile assigned to that node belongs to
  *    that member of the family. H2: it belongs to someone unrelated to the family.
  *    `isReference` (the last field) is not read by the engine.
  *  - A typed individual carries a `globalCode`; the profile with the same code holds the
  *    genotype. An individual without `globalCode` is not typed.
  *  - Of a `Profile` the engine reads only `globalCode` and `genotypification`, a map keyed
  *    by analysis type. Use key 1 (autosomal), as here: the relatives' genotypes are always
  *    read under key 1, whatever `AnalysisType` is passed to `calculateProbability`.
  *  - A `SampleCode` must look like `AR-C-TEST-1` (two or three capital letters, one capital
  *    letter, capital letters, a number, joined by hyphens); anything else is rejected with
  *    an `IllegalArgumentException`.
  *  - The frequency table must carry the key -1.0 (the minimum frequency, fmin) in every
  *    marker. */
class PedigreeReferenceCasesTest extends FlatSpec with Matchers {

  private val autosomal = AnalysisType(1, "Autosomal")

  // Frequencies add up to 1 in every marker; -1.0 is the minimum frequency (fmin).
  private val freq: Map[String, Map[Double, Double]] = Map(
    "M1" -> Map(10.0 -> 0.10, 11.0 -> 0.20, 12.0 -> 0.30, 13.0 -> 0.15, 14.0 -> 0.25),
    "M2" -> Map(7.0 -> 0.05, 8.0 -> 0.35, 9.0 -> 0.40, 10.0 -> 0.20),
    "M3" -> Map(15.0 -> 0.50, 16.0 -> 0.30, 17.0 -> 0.20))
  private val frequencyTable = freq.map { case (m, alleles) => m -> (alleles + (-1.0 -> 0.0001)) }
  private val alleleUniverse = freq.map { case (m, alleles) => m -> alleles.keys.toList.sorted }

  private val fatherCode = SampleCode("AR-C-TEST-1")
  private val childCode = SampleCode("AR-C-TEST-2")
  private val motherCode = SampleCode("AR-C-TEST-3")

  private def profile(code: SampleCode, loci: (String, Seq[Int])*): Profile = {
    val genotype = loci.map { case (m, as) => m -> as.map(a => Allele(BigDecimal(a)): AlleleValue).toList }.toMap
    Profile(null, code, null, null, null, Map(autosomal.id -> genotype), None, None, None, None)
  }

  // Father and Mother are the parents of Child; Child is the person of interest.
  private def family(motherTyped: Boolean) = Array(
    Individual(NodeAlias("Father"), None, None, Sex.Male, Some(fatherCode), unknown = false, None),
    Individual(NodeAlias("Mother"), None, None, Sex.Female, if (motherTyped) Some(motherCode) else None,
      unknown = false, None),
    Individual(NodeAlias("Child"), Some(NodeAlias("Father")), Some(NodeAlias("Mother")), Sex.Male,
      Some(childCode), unknown = true, None))

  "A paternity duo" should "give the paternity index marker by marker" in {
    // Mother not typed, no mutation model.
    val father = profile(fatherCode, "M1" -> Seq(11, 12), "M2" -> Seq(9, 9), "M3" -> Seq(15, 16))
    val child = profile(childCode, "M1" -> Seq(11, 13), "M2" -> Seq(8, 9), "M3" -> Seq(15, 16))

    val (lr, detail) = BayesianNetwork.calculateProbability(
      profiles = Array(father, child),
      genogram = family(motherTyped = false),
      frequencyTable = frequencyTable,
      analysisType = autosomal,
      linkage = Map.empty,
      seenAlleles = alleleUniverse)

    // Paternity index of a duo, child a,b:
    val m1 = 1 / (4 * 0.20)                       // father a,c shares one allele:  1 / (4 pa)
    val m2 = 1 / (2 * 0.40)                       // father a,a (homozygote):       1 / (2 pa)
    val m3 = (0.50 + 0.30) / (4 * 0.50 * 0.30)    // father a,b same as child:      (pa + pb) / (4 pa pb)

    println(f"paternity duo   M1 expected $m1%.6f GENis ${detail("M1").lr}%.6f")
    println(f"paternity duo   M2 expected $m2%.6f GENis ${detail("M2").lr}%.6f")
    println(f"paternity duo   M3 expected $m3%.6f GENis ${detail("M3").lr}%.6f")
    println(f"paternity duo   total expected ${m1 * m2 * m3}%.6f GENis $lr%.6f")

    detail("M1").lr shouldBe m1 +- 1e-9
    detail("M2").lr shouldBe m2 +- 1e-9
    detail("M3").lr shouldBe m3 +- 1e-9
    lr shouldBe (m1 * m2 * m3) +- 1e-9
  }

  "A trio with a paternal inconsistency" should "give 0 without a mutation model and the Equal-model value with one" in {
    // Father c,d; mother b,d; child a,b: the father carries neither allele of the child.
    val profiles = Array(
      profile(fatherCode, "M1" -> Seq(12, 13)),
      profile(motherCode, "M1" -> Seq(11, 13)),
      profile(childCode, "M1" -> Seq(10, 11)))
    val (pa, pb) = (0.10, 0.20)

    def lr(mutationModelType: Option[Long], mutationModelData: Option[List[BayesianNetwork.MutationModelData]]) =
      BayesianNetwork.calculateProbability(profiles, family(motherTyped = true), frequencyTable, autosomal,
        Map.empty, mutationModelType = mutationModelType, mutationModelData = mutationModelData,
        seenAlleles = alleleUniverse)

    val (withoutModel, detail) = lr(None, None)
    withoutModel shouldBe 0.0
    detail("M1").classification shouldBe "mutation"   // incompatible unless a mutation is assumed

    // Mutation model type 1 (Equal), rate r, one parameter row per marker. With
    // `ignoreSex = true` the parameter row must carry sex "I"; otherwise one row per sex,
    // "F" and "M". Types 2 and 3 also need the k_i of every allele, computed with
    // MutationService.recalibrateKis; type 1 takes none.
    val r = 0.002
    val model = MutationModel(1L, "equal", mutationType = 1L, active = true, ignoreSex = true, cantSaltos = 1L)
    val parameter = MutationModelParameter(1L, model.id, "M1", "I",
      mutationRate = Some(BigDecimal(r)), mutationRange = Some(BigDecimal(0.5)),
      mutationRateMicrovariant = Some(BigDecimal(0.0)))
    val (withModel, _) = lr(Some(1L), Some(List((parameter, Nil, model))))

    // A parental allele is transmitted unchanged with probability 1 - r and becomes each of
    // the other k - 1 alleles of the marker with probability m = r / (k - 1).
    //   father c,d -> a: m          father c,d -> b: m
    //   mother b,d -> b: (1 - r)/2 + m/2          mother b,d -> a: m
    // P(child a,b | parents) = m ((1 - r)/2 + m/2) + m m,   and under H2 it is 2 pa pb.
    val m = r / (freq("M1").size - 1)
    val expected = (m * ((1 - r) / 2 + m / 2) + m * m) / (2 * pa * pb)

    println(f"trio, Equal model   M1 expected $expected%.9f GENis $withModel%.9f")
    withModel shouldBe expected +- (expected * 1e-9)
  }

  // The three cases below have more than two typed relatives or untyped people in between.
  // They share one marker with four alleles.
  private val (p1, p2, p3, p4) = (0.10, 0.20, 0.30, 0.40)
  private val fourAlleles = Map("L1" -> Map(1.0 -> p1, 2.0 -> p2, 3.0 -> p3, 4.0 -> p4, -1.0 -> 0.0001))
  private val fourAllelesUniverse = Map("L1" -> List(1.0, 2.0, 3.0, 4.0))

  private def code(n: Int) = SampleCode(s"AR-C-TEST-$n")
  private def founder(alias: String, sex: Sex.Value, typed: Option[Int]) =
    Individual(NodeAlias(alias), None, None, sex, typed.map(code), unknown = false, None)
  private def child(alias: String, father: String, mother: String, typed: Option[Int], unknown: Boolean = false) =
    Individual(NodeAlias(alias), Some(NodeAlias(father)), Some(NodeAlias(mother)), Sex.Male, typed.map(code),
      unknown, None)
  private def lrOnL1(genogram: Array[Individual], profiles: Profile*): Double =
    BayesianNetwork.calculateProbability(profiles.toArray, genogram, fourAlleles, autosomal, Map.empty,
      seenAlleles = fourAllelesUniverse)._1

  "Both paternal grandparents" should "give the LR of a grandchild whose parents are not typed" in {
    // Grandfather 1,2 and grandmother 3,4; the father and the mother are not typed; the
    // grandchild, 1,3, is the person of interest.
    val genogram = Array(
      founder("Grandfather", Sex.Male, Some(1)),
      founder("Grandmother", Sex.Female, Some(2)),
      child("Father", "Grandfather", "Grandmother", None),
      founder("Mother", Sex.Female, None),
      child("Grandchild", "Father", "Mother", Some(3), unknown = true))
    val lr = lrOnL1(genogram,
      profile(code(1), "L1" -> Seq(1, 2)), profile(code(2), "L1" -> Seq(3, 4)), profile(code(3), "L1" -> Seq(1, 3)))

    // The father passes on each of the four grandparental alleles with probability 1/4; the
    // other allele comes from the untyped mother, that is, from the population.
    //   H1: P(1,3) = 1/4 p3 + 1/4 p1          H2: P(1,3) = 2 p1 p3
    val expected = (p1 + p3) / (8 * p1 * p3)

    println(f"two grandparents      expected $expected%.9f GENis $lr%.9f")
    lr shouldBe expected +- (expected * 1e-9)
  }

  "Three full siblings" should "give the LR of a fourth sibling when no parent is typed" in {
    // Siblings 1,2 / 1,3 / 2,4; neither parent is typed; the person of interest is 1,2.
    val genogram = Array(
      founder("Father", Sex.Male, None),
      founder("Mother", Sex.Female, None),
      child("Sibling1", "Father", "Mother", Some(1)),
      child("Sibling2", "Father", "Mother", Some(2)),
      child("Sibling3", "Father", "Mother", Some(3)),
      child("Sibling4", "Father", "Mother", Some(4), unknown = true))
    val lr = lrOnL1(genogram,
      profile(code(1), "L1" -> Seq(1, 2)), profile(code(2), "L1" -> Seq(1, 3)), profile(code(3), "L1" -> Seq(2, 4)),
      profile(code(4), "L1" -> Seq(1, 2)))

    // The three siblings show four alleles, and each sibling takes one from each parent: 1
    // and 2 are in different parents, so are 1 and 3, and 2 and 4. The parents can only be
    // 1,4 and 2,3, and a further child of theirs is 1,2 with probability 1/4.
    //   H1: P(1,2) = 1/4          H2: P(1,2) = 2 p1 p2
    val expected = 0.25 / (2 * p1 * p2)

    println(f"three siblings        expected $expected%.9f GENis $lr%.9f")
    lr shouldBe expected +- (expected * 1e-9)
  }

  "A father and a full sibling" should "give the LR of another child of the same couple" in {
    // Father 1,2; the mother is not typed; a sibling is 2,3; the person of interest is 2,4.
    val genogram = Array(
      founder("Father", Sex.Male, Some(1)),
      founder("Mother", Sex.Female, None),
      child("Sibling", "Father", "Mother", Some(2)),
      child("Child", "Father", "Mother", Some(3), unknown = true))
    val lr = lrOnL1(genogram,
      profile(code(1), "L1" -> Seq(1, 2)), profile(code(2), "L1" -> Seq(2, 3)), profile(code(3), "L1" -> Seq(2, 4)))

    // The sibling's 3 comes from the mother, so she carries a 3; her other allele is x with
    // probability px. The child of interest takes 2 from the father (1/2) and, from the
    // mother, her other allele (1/2), which is a 4 with probability p4.
    //   H1: P(2,4) = 1/2 * 1/2 * p4          H2: P(2,4) = 2 p2 p4
    val expected = 1 / (8 * p2)

    println(f"father and sibling    expected $expected%.9f GENis $lr%.9f")
    lr shouldBe expected +- (expected * 1e-9)
  }
}
