package unit.matching

import matching.Algorithm
import matching.MatchingAlgorithm.*
import matching.Stringency.*
import matching.{AleleRange, MatchingAlgorithm}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import pedigree.{Individual, NodeAlias, Sex}
import profile.*
import profile.GenotypificationByType.GenotypificationByType
import types.{AlphanumericId, SampleCode}

class MatchingAlgorithmTest extends AnyWordSpec with Matchers {

  // ─── shared fixtures ──────────────────────────────────────────────────────

  private val catA1 = AlphanumericId("CATA1")

  private val genotypification: GenotypificationByType = Map(
    1 -> Map("CSF1PO" -> List(Allele(10), Allele(11)))
  )

  private val noAnalyses: Option[List[Analysis]] = None

  private def makeProfile(
    code: String,
    internalCode: String,
    geno: GenotypificationByType = genotypification
  ): Profile =
    Profile(
      SampleCode(code), SampleCode(code), internalCode, "",
      catA1, geno, noAnalyses, None, None, None, None, None,
      false, true, false
    )

  // ─── matchLevel ───────────────────────────────────────────────────────────

  "MatchingAlgorithm.matchLevel" must {

    "return HighStringency for identical Alleles" in {
      matchLevel(Allele(1), Allele(1)) mustBe HighStringency
    }

    "return NoMatch for different Alleles" in {
      matchLevel(Allele(1), Allele(2)) mustBe NoMatch
    }

    "return HighStringency for OutOfLadderAllele '>' against Allele above boundary" in {
      matchLevel(OutOfLadderAllele(12, ">"), Allele(12.5)) mustBe HighStringency
    }

    "return NoMatch for OutOfLadderAllele '>' against Allele at boundary" in {
      matchLevel(OutOfLadderAllele(12, ">"), Allele(12)) mustBe NoMatch
    }

    "return HighStringency for OutOfLadderAllele '<' against Allele below boundary" in {
      matchLevel(OutOfLadderAllele(12, "<"), Allele(11)) mustBe HighStringency
    }

    "return NoMatch for OutOfLadderAllele '<' against Allele at boundary" in {
      matchLevel(OutOfLadderAllele(12, "<"), Allele(12)) mustBe NoMatch
    }

    "return HighStringency for two OutOfLadderAlleles with same direction" in {
      matchLevel(OutOfLadderAllele(12, ">"), OutOfLadderAllele(11, ">")) mustBe HighStringency
      matchLevel(OutOfLadderAllele(12, "<"), OutOfLadderAllele(11, "<")) mustBe HighStringency
    }

    "return NoMatch for OutOfLadderAlleles with opposite directions" in {
      matchLevel(OutOfLadderAllele(12, "<"), OutOfLadderAllele(11, ">")) mustBe NoMatch
      matchLevel(OutOfLadderAllele(12, ">"), OutOfLadderAllele(11, "<")) mustBe NoMatch
    }

    "return HighStringency for MicroVariant vs Allele in range (x < allele < x+1)" in {
      matchLevel(MicroVariant(12), Allele(12.1)) mustBe HighStringency
    }

    "return NoMatch for MicroVariant vs Allele outside range" in {
      matchLevel(MicroVariant(12), Allele(11)) mustBe NoMatch
      matchLevel(MicroVariant(12), Allele(13)) mustBe NoMatch
    }

    "return HighStringency for equal MicroVariants" in {
      matchLevel(MicroVariant(11), MicroVariant(11)) mustBe HighStringency
    }

    "return NoMatch for different MicroVariants" in {
      matchLevel(MicroVariant(11), MicroVariant(12)) mustBe NoMatch
      matchLevel(MicroVariant(12), MicroVariant(11)) mustBe NoMatch
    }

    "return HighStringency for OutOfLadderAllele '>' vs MicroVariant at boundary" in {
      matchLevel(OutOfLadderAllele(12, ">"), MicroVariant(12)) mustBe HighStringency
    }

    "return HighStringency for OutOfLadderAllele '<' vs MicroVariant below boundary" in {
      matchLevel(OutOfLadderAllele(12, "<"), MicroVariant(11)) mustBe HighStringency
    }

    "return HighStringency for identical XY alleles" in {
      matchLevel(XY('X'), XY('X')) mustBe HighStringency
      matchLevel(XY('Y'), XY('Y')) mustBe HighStringency
    }

    "return NoMatch for different XY alleles" in {
      matchLevel(XY('X'), XY('Y')) mustBe NoMatch
    }
  }

  // ─── worstMatchLevel ──────────────────────────────────────────────────────

  "MatchingAlgorithm.worstMatchLevel" must {

    "return HighStringency when all pairs match" in {
      worstMatchLevel(List(Allele(1), Allele(2)), List(Allele(1), Allele(2))) mustBe HighStringency
    }

    "return NoMatch when all pairs differ" in {
      worstMatchLevel(List(Allele(1), Allele(2)), List(Allele(3), Allele(4))) mustBe NoMatch
    }

    "return NoMatch for empty sequences" in {
      worstMatchLevel(Nil, Nil) mustBe NoMatch
    }
  }

  // ─── genotypeMatch ────────────────────────────────────────────────────────

  "MatchingAlgorithm.genotypeMatch" must {

    "return HighStringency for identical genotypes" in {
      genotypeMatch(List(Allele(1), Allele(2)), List(Allele(1), Allele(2))) mustBe HighStringency
    }

    "return HighStringency for genotype permutation" in {
      genotypeMatch(List(Allele(1), Allele(2)), List(Allele(2), Allele(1))) mustBe HighStringency
    }

    "return ModerateStringency when one allele is a subset of the other" in {
      genotypeMatch(List(Allele(1), Allele(2)), List(Allele(2))) mustBe ModerateStringency
    }

    "return NoMatch for completely different genotypes" in {
      genotypeMatch(List(Allele(1)), List(Allele(2))) mustBe NoMatch
    }

    "return LowStringency for MicroVariants in the same integer range (UBA test 1)" in {
      val level = genotypeMatch(
        List(MicroVariant(80), MicroVariant(81)),
        List(MicroVariant(80), MicroVariant(80))
      )
      level mustBe LowStringency
    }

    "return LowStringency for MicroVariant vs OutOfLadderAllele in same range (UBA test 2)" in {
      val level = genotypeMatch(
        List(MicroVariant(80), MicroVariant(80)),
        List(Allele(80.2), OutOfLadderAllele(BigDecimal("80.5"), ">"))
      )
      level mustBe LowStringency
    }
  }

  // ─── mergeRanges ──────────────────────────────────────────────────────────

  "MatchingAlgorithm.mergeRanges" must {

    "return the intersection for overlapping ranges (p contains q)" in {
      mergeRanges((1, 100), (10, 50)) mustBe Some((10, 50))
    }

    "return the intersection for partially overlapping ranges" in {
      mergeRanges((1, 50), (30, 100)) mustBe Some((30, 50))
    }

    "return None for non-overlapping ranges (p before q)" in {
      mergeRanges((1, 10), (20, 30)) mustBe None
    }

    "return None for non-overlapping ranges (q before p)" in {
      mergeRanges((20, 30), (1, 10)) mustBe None
    }

    "return the common range when ranges are identical" in {
      mergeRanges((5, 15), (5, 15)) mustBe Some((5, 15))
    }
  }

  // ─── getMtProfile ─────────────────────────────────────────────────────────

  "MatchingAlgorithm.getMtProfile" must {

    val mtGenotypification: GenotypificationByType = Map(
      4 -> Map("HV1" -> List(Mitocondrial('A', 1), Mitocondrial('A', 2.1), Mitocondrial('-', 3)))
    )

    "return None when no profile has mitochondrial genotypification" in {
      val profiles = List(
        makeProfile("AR-C-SHDG-1", "Unknown"),
        makeProfile("AR-C-SHDG-2", "FatherCode"),
        makeProfile("AR-C-SHDG-3", "MotherCode"),
        makeProfile("AR-C-SHDG-4", "GrandmotherCode")
      )
      val individuals: Seq[Individual] = Seq(
        Individual(NodeAlias("PI"),          Some(NodeAlias("Father")), Some(NodeAlias("Mother")), Sex.Unknown, Some(SampleCode("AR-C-SHDG-1")), true, None),
        Individual(NodeAlias("Mother"),      None,                       Some(NodeAlias("Grandmother")), Sex.Female, Some(SampleCode("AR-C-SHDG-3")), true, None),
        Individual(NodeAlias("Father"),      None, None, Sex.Male, Some(SampleCode("AR-C-SHDG-2")), true, None),
        Individual(NodeAlias("Grandmother"), None, None, Sex.Female, Some(SampleCode("AR-C-SHDG-4")), true, None)
      )

      getMtProfile(individuals, profiles) mustBe None
    }

    "return the grandmother profile when she has mitochondrial genotypification" in {
      val profiles = List(
        makeProfile("AR-C-SHDG-1", "Unknown"),
        makeProfile("AR-C-SHDG-2", "FatherCode"),
        makeProfile("AR-C-SHDG-3", "MotherCode"),
        makeProfile("AR-C-SHDG-4", "GrandmotherCode", mtGenotypification)
      )
      val individuals: Seq[Individual] = Seq(
        Individual(NodeAlias("PI"),          Some(NodeAlias("Father")), Some(NodeAlias("Mother")), Sex.Unknown, Some(SampleCode("AR-C-SHDG-1")), true, None),
        Individual(NodeAlias("Mother"),      None,                       Some(NodeAlias("Grandmother")), Sex.Female, Some(SampleCode("AR-C-SHDG-3")), true, None),
        Individual(NodeAlias("Father"),      None, None, Sex.Male, Some(SampleCode("AR-C-SHDG-2")), true, None),
        Individual(NodeAlias("Grandmother"), None, None, Sex.Female, Some(SampleCode("AR-C-SHDG-4")), true, None)
      )

      val result = getMtProfile(individuals, profiles)
      result mustBe defined
      result.get.internalSampleCode mustBe "GrandmotherCode"
    }

    "return the brother profile when he has mitochondrial genotypification (maternal sibling)" in {
      val profiles = List(
        makeProfile("AR-C-SHDG-1", "Unknown"),
        makeProfile("AR-C-SHDG-2", "FatherCode"),
        makeProfile("AR-C-SHDG-3", "MotherCode"),
        makeProfile("AR-C-SHDG-4", "GrandmotherCode"),
        makeProfile("AR-C-SHDG-5", "Brother", mtGenotypification)
      )
      val individuals: Seq[Individual] = Seq(
        Individual(NodeAlias("PI"),          Some(NodeAlias("Father")), Some(NodeAlias("Mother")), Sex.Unknown, Some(SampleCode("AR-C-SHDG-1")), true, None),
        Individual(NodeAlias("Mother"),      None,                       Some(NodeAlias("Grandmother")), Sex.Female, Some(SampleCode("AR-C-SHDG-3")), true, None),
        Individual(NodeAlias("Father"),      None, None, Sex.Male, Some(SampleCode("AR-C-SHDG-2")), true, None),
        Individual(NodeAlias("Grandmother"), None, None, Sex.Female, Some(SampleCode("AR-C-SHDG-4")), true, None),
        Individual(NodeAlias("Brother"),     None, Some(NodeAlias("Mother")), Sex.Male, Some(SampleCode("AR-C-SHDG-5")), true, None)
      )

      val result = getMtProfile(individuals, profiles)
      result mustBe defined
      result.get.internalSampleCode mustBe "Brother"
    }

    "return the uncle profile when he has mitochondrial genotypification (maternal uncle)" in {
      val profiles = List(
        makeProfile("AR-C-SHDG-1", "Unknown"),
        makeProfile("AR-C-SHDG-2", "FatherCode"),
        makeProfile("AR-C-SHDG-3", "MotherCode"),
        makeProfile("AR-C-SHDG-4", "GrandmotherCode"),
        makeProfile("AR-C-SHDG-5", "Brother"),
        makeProfile("AR-C-SHDG-6", "Uncle", mtGenotypification)
      )
      val individuals: Seq[Individual] = Seq(
        Individual(NodeAlias("PI"),          Some(NodeAlias("Father")), Some(NodeAlias("Mother")), Sex.Unknown, Some(SampleCode("AR-C-SHDG-1")), true, None),
        Individual(NodeAlias("Mother"),      None,                       Some(NodeAlias("Grandmother")), Sex.Female, Some(SampleCode("AR-C-SHDG-3")), true, None),
        Individual(NodeAlias("Father"),      None, None, Sex.Male, Some(SampleCode("AR-C-SHDG-2")), true, None),
        Individual(NodeAlias("Grandmother"), None, None, Sex.Female, Some(SampleCode("AR-C-SHDG-4")), true, None),
        Individual(NodeAlias("Brother"),     None, Some(NodeAlias("Mother")), Sex.Male, Some(SampleCode("AR-C-SHDG-5")), true, None),
        Individual(NodeAlias("Uncle"),       None, Some(NodeAlias("Grandmother")), Sex.Female, Some(SampleCode("AR-C-SHDG-6")), true, None)
      )

      val result = getMtProfile(individuals, profiles)
      result mustBe defined
      result.get.internalSampleCode mustBe "Uncle"
    }
  }

  // ─── convertSingleAlele ──────────────────────────────────────────────────

  "MatchingAlgorithm.convertSingleAlele" must {

    "convert Allele below range min to OutOfLadderAllele '<'" in {
      convertSingleAlele(Allele(8), AleleRange(11, 15)).toString mustBe OutOfLadderAllele(11, "<").toString
    }

    "convert Allele above range max to OutOfLadderAllele '>'" in {
      convertSingleAlele(Allele(16), AleleRange(11, 15)).toString mustBe OutOfLadderAllele(15, ">").toString
    }

    "keep Allele within range unchanged" in {
      convertSingleAlele(Allele(12), AleleRange(11, 15)) mustBe Allele(12)
    }

    "keep Allele at boundary unchanged" in {
      convertSingleAlele(Allele(11), AleleRange(11, 15)) mustBe Allele(11)
      convertSingleAlele(Allele(15), AleleRange(11, 15)) mustBe Allele(15)
    }

    "re-clamp OutOfLadderAllele '>' that exceeds range max" in {
      convertSingleAlele(OutOfLadderAllele(16, ">"), AleleRange(12, 15)).toString mustBe OutOfLadderAllele(15, ">").toString
    }

    "re-clamp OutOfLadderAllele '<' that is below range min" in {
      convertSingleAlele(OutOfLadderAllele(11, "<"), AleleRange(12, 15)).toString mustBe OutOfLadderAllele(12, "<").toString
    }

    "convert MicroVariant entirely below range to OutOfLadderAllele '<'" in {
      convertSingleAlele(MicroVariant(11), AleleRange(12, 15)).toString mustBe OutOfLadderAllele(12, "<").toString
    }

    "keep MicroVariant at range boundary unchanged" in {
      convertSingleAlele(MicroVariant(12), AleleRange(12, 15)).toString mustBe MicroVariant(12).toString
    }

    "convert MicroVariant above range to OutOfLadderAllele '>'" in {
      convertSingleAlele(MicroVariant(16), AleleRange(12, 15)).toString mustBe OutOfLadderAllele(15, ">").toString
    }
  }

  // ─── convertAleles ───────────────────────────────────────────────────────

  "MatchingAlgorithm.convertAleles" must {

    "convert all alleles below and above range for locus TH01" in {
      val result = convertAleles(List(Allele(10), Allele(20)), "TH01", Map("TH01" -> AleleRange(11, 15)))
      result.toList.map(_.toString) mustBe List(OutOfLadderAllele(11, "<").toString, OutOfLadderAllele(15, ">").toString)
    }

    "keep alleles within range unchanged" in {
      val result = convertAleles(List(Allele(12), Allele(14)), "TH01", Map("TH01" -> AleleRange(11, 15)))
      result.toList mustBe List(Allele(12), Allele(14))
    }

    "convert only the out-of-range allele when one is below range" in {
      val result = convertAleles(List(Allele(8), Allele(14)), "TH01", Map("TH01" -> AleleRange(11, 15)))
      result.toList.map(_.toString) mustBe List(OutOfLadderAllele(11, "<").toString, Allele(14).toString)
    }

    "convert only the out-of-range allele when one is above range" in {
      val result = convertAleles(List(Allele(12), Allele(16)), "TH01", Map("TH01" -> AleleRange(11, 15)))
      result.toList.map(_.toString) mustBe List(Allele(12).toString, OutOfLadderAllele(15, ">").toString)
    }

    "use default range (0..99) when locus is not in the range map" in {
      val result = convertAleles(List(Allele(50)), "UNKNOWN_LOCUS", Map.empty)
      result.toList mustBe List(Allele(50))
    }
  }

  // ─── anyMatch ────────────────────────────────────────────────────────────

  "MatchingAlgorithm.anyMatch" must {

    "return true when at least one pair matches" in {
      anyMatch(Set(Allele(1), Allele(2)), Set(Allele(2), Allele(3))) mustBe true
    }

    "return false when no pair matches" in {
      anyMatch(Set(Allele(1)), Set(Allele(2))) mustBe false
    }
  }
}
