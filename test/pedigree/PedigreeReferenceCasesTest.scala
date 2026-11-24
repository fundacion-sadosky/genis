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
}
