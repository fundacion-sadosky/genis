package matching

import scala.math.BigDecimal.int2bigDecimal
import org.specs2.mutable.Specification
import matching.MatchingAlgorithm._
import org.scalatest._
import profile._

@Ignore
class MatchingMixesTest extends Specification {

  val analyses: Option[List[Analysis]] = stubs.Stubs.analyses

  "getMostRestrictive " should {
    "getMostRestrictive 1 " in {
      getMostRestrictive(MicroVariant(3),Allele(3.3)) must beEqualTo(Allele(3.3))
    }
    "getMostRestrictive 2 " in {
      getMostRestrictive(Allele(3.3),MicroVariant(3)) must beEqualTo(Allele(3.3))
    }
    "getMostRestrictive 3 " in {
      val r1 = getMostRestrictive(OutOfLadderAllele(3,"<"),OutOfLadderAllele(4,"<"))

      val result = r1 match {
        case a:OutOfLadderAllele if a.base == 3 => true
        case _ => false
      }
      result must beEqualTo(true)
    }
    "getMostRestrictive 4" in {
      val r1 = getMostRestrictive(OutOfLadderAllele(3,">"),OutOfLadderAllele(4,">"))

      val result = r1 match {
        case a:OutOfLadderAllele if a.base == 4 => true
        case _ => false
      }
      result must beEqualTo(true)
    }
    "getMostRestrictive 5 " in {
      val r1 = getMostRestrictive(OutOfLadderAllele(4,"<"),OutOfLadderAllele(3,"<"))

      val result = r1 match {
        case a:OutOfLadderAllele if a.base == 3 => true
        case _ => false
      }
      result must beEqualTo(true)
    }
    "getMostRestrictive 6" in {
      val r1 = getMostRestrictive(OutOfLadderAllele(4,">"),OutOfLadderAllele(3,">"))

      val result = r1 match {
        case a:OutOfLadderAllele if a.base == 4 => true
        case _ => false
      }
      result must beEqualTo(true)
    }
    "getMostRestrictive 7 " in {
      getMostRestrictive(OutOfLadderAllele(4,"<"),Allele(3.3)) must beEqualTo(Allele(3.3))
    }
    "getMostRestrictive 8 " in {
      getMostRestrictive(Allele(3.3),OutOfLadderAllele(4,"<")) must beEqualTo(Allele(3.3))
    }
    "getMostRestrictive 9 " in {
      val r1 = getMostRestrictive(MicroVariant(3),OutOfLadderAllele(4,"<"))
      val result = r1 match {
        case a:MicroVariant if a.count == 3 => true
        case _ => false
      }
      result must beEqualTo(true)
    }
    "getMostRestrictive 10 " in {
      val r1 = getMostRestrictive(OutOfLadderAllele(4,">"),MicroVariant(6))
      val result = r1 match {
        case a:MicroVariant if a.count == 6 => true
        case _ => false
      }
      result must beEqualTo(true)
    }
  }
  "Microvariants and Out of ladders " should {
    "Microvariant match (4, 4 with 2 in common)" in {
      val mix1 = List(MicroVariant(1), MicroVariant(2), Allele(3), MicroVariant(4))
      val mix2 = List(Allele(1.3), MicroVariant(2), Allele(5.5), MicroVariant(6))
      val v1 = List(Allele(3), MicroVariant(4))
      val v2 = List(MicroVariant(5), Allele(6.1))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, Some(v2))
      level must beEqualTo(Stringency.ModerateStringency)
    }
    "OutOfLadder match (4, 4 with 2 in common)" in {
      val mix1 = List(OutOfLadderAllele(2,"<"), MicroVariant(2), Allele(3), MicroVariant(4))
      val mix2 = List(MicroVariant(1), Allele(2.3), Allele(5.5), MicroVariant(6))
      val v1 = List(Allele(3), MicroVariant(4))
      val v2 = List(MicroVariant(5), Allele(6.1))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, Some(v2))
      level must beEqualTo(Stringency.ModerateStringency)
    }
    "OutOfLadder 2 match (4, 4 with 2 in common)" in {
      val mix1 = List(OutOfLadderAllele(2,"<"), MicroVariant(2), OutOfLadderAllele(9,">"), Allele(4))
      val mix2 = List(OutOfLadderAllele(2,"<"), MicroVariant(2), MicroVariant(5), Allele(6))
      val v1 = List(OutOfLadderAllele(9,">"), Allele(4))
      val v2 = List(MicroVariant(5), Allele(6))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, Some(v2))
      level must beEqualTo(Stringency.ModerateStringency)
    }
    "Microvariant on victim match MV with M (g1=4) because length (r `intersect` g2) == 2" in {
      val mix1 = List(Allele(1), Allele(2), MicroVariant(3), MicroVariant(4))
      val mix2 = List(Allele(1), Allele(2), Allele(5))
      val v1 = List(MicroVariant(3), MicroVariant(4))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, None)
      level must beEqualTo(Stringency.ModerateStringency)
      val level2 = genotypeMatchOfMixes(mix2, None, mix1, Some(v1))
      level2 must beEqualTo(Stringency.ModerateStringency)
    }
    "MicroVariant match MV with M (g1=1) because length (g1 `intersect` g2) == 1" in {
      val mix1 = List(MicroVariant(1))
      val mix2 = List(MicroVariant(1))
      val v1 = List(MicroVariant(1))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, None)
      level must beEqualTo(Stringency.ModerateStringency)
      val level2 = genotypeMatchOfMixes(mix2, None, mix1, Some(v1))
      level2 must beEqualTo(Stringency.ModerateStringency)
    }
    "OutOfLadderAllele match MV with M (g1=1) because length (g1 `intersect` g2) == 1" in {
      val mix1 = List(OutOfLadderAllele(BigDecimal(2),"<"))
      val mix2 = List(OutOfLadderAllele(BigDecimal(2),"<"))
      val v1 = List(OutOfLadderAllele(BigDecimal(2),"<"))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, None)
      level must beEqualTo(Stringency.ModerateStringency)
      val level2 = genotypeMatchOfMixes(mix2, None, mix1, Some(v1))
      level2 must beEqualTo(Stringency.ModerateStringency)
    }
    "OutOfLadderAllele2 match MV with M (g1=1) because length (g1 `intersect` g2) == 1" in {
      val mix1 = List(OutOfLadderAllele(BigDecimal(2),"<"))
      val mix2 = List(OutOfLadderAllele(BigDecimal(1),"<"))
      val v1 = List(OutOfLadderAllele(BigDecimal(2),"<"))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, None)
      level must beEqualTo(Stringency.ModerateStringency)
      val level2 = genotypeMatchOfMixes(mix2, None, mix1, Some(v1))
      level2 must beEqualTo(Stringency.ModerateStringency)
    }
    "NOT OutOfLadderAllele2 match MV with M (g1=1) because length (g1 `intersect` g2) == 1" in {
      val mix1 = List(OutOfLadderAllele(BigDecimal(2),">"))
      val mix2 = List(OutOfLadderAllele(BigDecimal(1),"<"))
      val v1 = List(OutOfLadderAllele(BigDecimal(2),"<"))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, None)
      level must beEqualTo(Stringency.Mismatch)
      val level2 = genotypeMatchOfMixes(mix2, None, mix1, Some(v1))
      level2 must beEqualTo(Stringency.Mismatch)
    }
    "OutOfLadder on victim match MV with M (g1=4) because length (r `intersect` g2) == 2" in {
      val mix1 = List(Allele(3), Allele(2), MicroVariant(1), MicroVariant(4))
      val mix2 = List(Allele(3), Allele(2), Allele(5))
      val v1 = List(OutOfLadderAllele(BigDecimal(2),"<"), MicroVariant(4))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, None)
      level must beEqualTo(Stringency.ModerateStringency)
      val level2 = genotypeMatchOfMixes(mix2, None, mix1, Some(v1))
      level2 must beEqualTo(Stringency.ModerateStringency)
    }
    "Microvariant match (4, 4 with 2 in common)" in {
      val mix1 = List(Allele(1.1), Allele(2.1), Allele(3.1), Allele(4.1))
      val mix2 = List(MicroVariant(1), MicroVariant(2), MicroVariant(5), MicroVariant(6))

      val level = genotypeMatchOfMixes(mix1, None, mix2, None)
      level must beEqualTo(Stringency.ModerateStringency)
    }
    "Microvariant match (4, 4 with 2 in common) 2" in {
      val mix1 = List(MicroVariant(1), MicroVariant(2), Allele(3.1), Allele(4.1))
      val mix2 = List(MicroVariant(1), MicroVariant(2), MicroVariant(5), MicroVariant(6))

      val level = genotypeMatchOfMixes(mix1, None, mix2, None)
      level must beEqualTo(Stringency.ModerateStringency)
    }
    "Microvariant and OutOfLadder match (4, 4 with 2 in common) 3" in {
      val mix1 = List(MicroVariant(1), MicroVariant(2), Allele(3.1), Allele(4.1))
      val mix2 = List(OutOfLadderAllele(BigDecimal(2),"<"), MicroVariant(2), MicroVariant(5), MicroVariant(6))

      val level = genotypeMatchOfMixes(mix1, None, mix2, None)
      level must beEqualTo(Stringency.ModerateStringency)
    }
    "OutOfLadder match (4, 4 with 2 in common)" in {
      val mix1 = List(Allele(1.1), Allele(2.1), Allele(3.1), Allele(4.1))
      val mix2 = List(OutOfLadderAllele(BigDecimal(2),"<"), MicroVariant(2), MicroVariant(5), MicroVariant(6))

      val level = genotypeMatchOfMixes(mix1, None, mix2, None)
      level must beEqualTo(Stringency.ModerateStringency)
    }
    "Microvariant NOT match (4, 3 with 1 in common) " in {
      val mix1 = List(Allele(1.1), Allele(2), Allele(3), Allele(4))
      val mix2 = List(MicroVariant(1), MicroVariant(5), MicroVariant(6))

      val level = genotypeMatchOfMixes(mix1, None, mix2, None)
      level must beEqualTo(Stringency.Mismatch)
    }
  }
  "genotypeMatch without victim " should {
    "match (4, 4 with 2 in common)" in {
      val mix1 = List(Allele(1), Allele(2), Allele(3), Allele(4))
      val mix2 = List(Allele(1), Allele(2), Allele(5), Allele(6))

      val level = genotypeMatchOfMixes(mix1, None, mix2, None)
      level must beEqualTo(Stringency.ModerateStringency)
    }
    "match (4, 3 with 2 in common)" in {
      val mix1 = List(Allele(1), Allele(2), Allele(3), Allele(4))
      val mix2 = List(Allele(1), Allele(2), Allele(5))

      val level = genotypeMatchOfMixes(mix1, None, mix2, None)
      level must beEqualTo(Stringency.ModerateStringency)
    }
    "match (4, 2 with 2 in common)" in {
      val mix1 = List(Allele(1), Allele(2), Allele(3), Allele(4))
      val mix2 = List(Allele(1), Allele(2))

      val level = genotypeMatchOfMixes(mix1, None, mix2, None)
      level must beEqualTo(Stringency.ModerateStringency)
    }
    "NOT match (4, 3 with 1 in common) " in {
      val mix1 = List(Allele(1), Allele(2), Allele(3), Allele(4))
      val mix2 = List(Allele(1), Allele(5), Allele(6))

      val level = genotypeMatchOfMixes(mix1, None, mix2, None)
      level must beEqualTo(Stringency.Mismatch)
    }
    "NOT match (4, 2 with 1 in common)  " in {
      val mix1 = List(Allele(1), Allele(2), Allele(3), Allele(4))
      val mix2 = List(Allele(1), Allele(5))

      val level = genotypeMatchOfMixes(mix1, None, mix2, None)
      level must beEqualTo(Stringency.Mismatch)
    }
    "NOT match (4, 2 with 1 in common)" in {
      val mix1 = List(Allele(1), Allele(2), Allele(3), Allele(4))
      val mix2 = List(Allele(1))

      val level = genotypeMatchOfMixes(mix1, None, mix2, None)
      level must beEqualTo(Stringency.Mismatch)
    }
    "match (3, 3 with 2 in common)" in {
      val mix1 = List(Allele(1), Allele(2), Allele(3))
      val mix2 = List(Allele(1), Allele(2), Allele(5))

      val level = genotypeMatchOfMixes(mix1, None, mix2, None)
      level must beEqualTo(Stringency.ModerateStringency)
    }
  }

  "genotypeMatch with 2 victims " should {
    "match (4, 4 with 2 in common)" in {
      val mix1 = List(Allele(1), Allele(2), Allele(3), Allele(4))
      val mix2 = List(Allele(1), Allele(2), Allele(5), Allele(6))
      val v1 = List(Allele(3), Allele(4))
      val v2 = List(Allele(5), Allele(6))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, Some(v2))
      level must beEqualTo(Stringency.ModerateStringency)
    }
    "match (4, 3 with 2 in common)" in {
      val mix1 = List(Allele(1), Allele(2), Allele(3), Allele(4))
      val mix2 = List(Allele(1), Allele(2), Allele(5))
      val v1 = List(Allele(3), Allele(4))
      val v2 = List(Allele(5))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, Some(v2))
      level must beEqualTo(Stringency.ModerateStringency)
    }
    "NOT match (4, 3 with 1 in common)" in {
      val mix1 = List(Allele(1), Allele(2), Allele(3), Allele(4))
      val mix2 = List(Allele(1), Allele(2), Allele(3))
      val v1 = List(Allele(1), Allele(2))
      val v2 = List(Allele(1), Allele(2))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, Some(v2))
      level must beEqualTo(Stringency.Mismatch)
    }
    "match (g1=3, r1=2, r2=2 )" in {
      val mix1 = List(Allele(1), Allele(2), Allele(3))
      val mix2 = List(Allele(1), Allele(2), Allele(5))
      val v1 = List(Allele(3))
      val v2 = List(Allele(5))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, Some(v2))
      level must beEqualTo(Stringency.ModerateStringency)
    }
    "NOT match (g1=3, r1=2, r2=2 ) because length (r1 `intersect` r2) != 2" in {
      val mix1 = List(Allele(1), Allele(2), Allele(3))
      val mix2 = List(Allele(1), Allele(4), Allele(5))
      val v1 = List(Allele(3))
      val v2 = List(Allele(5))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, Some(v2))
      level must beEqualTo(Stringency.Mismatch)
    }
    "match (g1=3, r1=2, r2=1 )" in {
      val mix1 = List(Allele(1), Allele(2), Allele(3))
      val mix2 = List(Allele(1), Allele(2), Allele(5))
      val v1 = List(Allele(3))
      val v2 = List(Allele(2), Allele(5))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, Some(v2))
      level must beEqualTo(Stringency.ModerateStringency)
    }
    "NOT match (g1=3, r1=2, r2=1 ) because length (r1 `intersect` g2) != 2" in {
      val mix1 = List(Allele(1), Allele(2), Allele(3))
      val mix2 = List(Allele(3), Allele(2), Allele(5))
      val v1 = List(Allele(3))
      val v2 = List(Allele(2), Allele(5))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, Some(v2))
      level must beEqualTo(Stringency.Mismatch)
    }
    "NOT match (g1=3, r1=2, r2=1 ) because length (r1 `intersect` r2) != 1" in {
      val mix1 = List(Allele(1), Allele(2), Allele(3))
      val mix2 = List(Allele(1), Allele(2), Allele(5))
      val v1 = List(Allele(3))
      val v2 = List(Allele(1), Allele(2))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, Some(v2))
      level must beEqualTo(Stringency.Mismatch)
    }
  }
  "genotypeMatch with 1 victim " should {

    "match MV with M (g1=4) because length (r `intersect` g2) == 2" in {
      val mix1 = List(Allele(1), Allele(2), Allele(3), Allele(4))
      val mix2 = List(Allele(1), Allele(2), Allele(5))
      val v1 = List(Allele(3), Allele(4))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, None)
      level must beEqualTo(Stringency.ModerateStringency)
      val level2 = genotypeMatchOfMixes(mix2, None, mix1, Some(v1))
      level2 must beEqualTo(Stringency.ModerateStringency)
    }
    "NOT match MV with M (g1=4) because length (r `intersect` g2) != 2" in {
      val mix1 = List(Allele(1), Allele(2), Allele(3), Allele(4))
      val mix2 = List(Allele(1), Allele(3), Allele(5))
      val v1 = List(Allele(3), Allele(4))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, None)
      level must beEqualTo(Stringency.Mismatch)
      val level2 = genotypeMatchOfMixes(mix2, None, mix1, Some(v1))
      level2 must beEqualTo(Stringency.Mismatch)

    }
    "match MV with M (g1=3, r=2) because length (r `intersect` g2) == 2" in {
      val mix1 = List(Allele(1), Allele(2), Allele(3))
      val mix2 = List(Allele(1), Allele(2), Allele(5))
      val v1 = List(Allele(3))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, None)
      level must beEqualTo(Stringency.ModerateStringency)
      val level2 = genotypeMatchOfMixes(mix2, None, mix1, Some(v1))
      level2 must beEqualTo(Stringency.ModerateStringency)
    }
    "NOT match MV with M (g1=3, r=2) because length (r `intersect` g2) != 2" in {
      val mix1 = List(Allele(1), Allele(2), Allele(3))
      val mix2 = List(Allele(1), Allele(4), Allele(5))
      val v1 = List(Allele(3))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, None)
      level must beEqualTo(Stringency.Mismatch)
      val level2 = genotypeMatchOfMixes(mix2, None, mix1, Some(v1))
      level2 must beEqualTo(Stringency.Mismatch)
    }
    "match MV with M (g1=3, r=1) because length (g1 `intersect` g2) >= 1 && (length (r `intersect` g2) == 1)" in {
      val mix1 = List(Allele(1), Allele(2), Allele(3))
      val mix2 = List(Allele(1), Allele(4), Allele(5))
      val v1 = List(Allele(2), Allele(3))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, None)
      level must beEqualTo(Stringency.ModerateStringency)
      val level2 = genotypeMatchOfMixes(mix2, None, mix1, Some(v1))
      level2 must beEqualTo(Stringency.ModerateStringency)
    }
    "NOT match MV with M (g1=3, r=1) because length (g1 `intersect` g2) == 0 " in {
      val mix1 = List(Allele(1), Allele(2), Allele(3))
      val mix2 = List(Allele(4), Allele(5))
      val v1 = List(Allele(2), Allele(3))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, None)
      level must beEqualTo(Stringency.Mismatch)
      val level2 = genotypeMatchOfMixes(mix2, None, mix1, Some(v1))
      level2 must beEqualTo(Stringency.Mismatch)
    }
    "NOT match MV with M (g1=3, r=1) because length (r `intersect` g2) == 0 " in {
      val mix1 = List(Allele(1), Allele(2), Allele(3))
      val mix2 = List(Allele(2), Allele(3))
      val v1 = List(Allele(2), Allele(3))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, None)
      level must beEqualTo(Stringency.Mismatch)
      val level2 = genotypeMatchOfMixes(mix2, None, mix1, Some(v1))
      level2 must beEqualTo(Stringency.Mismatch)
    }
    "match MV with M (g1=2, r=1) because length (r `intersect` g2) == 1" in {
      val mix1 = List(Allele(1), Allele(2))
      val mix2 = List(Allele(1), Allele(4))
      val v1 = List(Allele(2))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, None)
      level must beEqualTo(Stringency.ModerateStringency)
      val level2 = genotypeMatchOfMixes(mix2, None, mix1, Some(v1))
      level2 must beEqualTo(Stringency.ModerateStringency)
    }
    "NOT match MV with M (g1=2, r=1) because length (r `intersect` g2) == 0" in {
      val mix1 = List(Allele(1), Allele(2))
      val mix2 = List(Allele(2), Allele(3))
      val v1 = List(Allele(2))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, None)
      level must beEqualTo(Stringency.Mismatch)
      val level2 = genotypeMatchOfMixes(mix2, None, mix1, Some(v1))
      level2 must beEqualTo(Stringency.Mismatch)
    }
    "match MV with M (g1=2, r=0) because length (g1 `intersect` g2) >= 1" in {
      val mix1 = List(Allele(1), Allele(2))
      val mix2 = List(Allele(1), Allele(4))
      val v1 = List(Allele(1), Allele(2))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, None)
      level must beEqualTo(Stringency.ModerateStringency)
      val level2 = genotypeMatchOfMixes(mix2, None, mix1, Some(v1))
      level2 must beEqualTo(Stringency.ModerateStringency)
    }
    "NOT match MV with M (g1=2, r=0) because length (g1 `intersect` g2) == 0" in {
      val mix1 = List(Allele(1), Allele(2))
      val mix2 = List(Allele(3), Allele(4))
      val v1 = List(Allele(1), Allele(2))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, None)
      level must beEqualTo(Stringency.Mismatch)
      val level2 = genotypeMatchOfMixes(mix2, None, mix1, Some(v1))
      level2 must beEqualTo(Stringency.Mismatch)
    }
    "match MV with M (g1=1) because length (g1 `intersect` g2) == 1" in {
      val mix1 = List(Allele(1))
      val mix2 = List(Allele(1))
      val v1 = List(Allele(1))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, None)
      level must beEqualTo(Stringency.ModerateStringency)
      val level2 = genotypeMatchOfMixes(mix2, None, mix1, Some(v1))
      level2 must beEqualTo(Stringency.ModerateStringency)
    }
    "NOT match MV with M (g1=1) because length (g1 `intersect` g2) == 0" in {
      val mix1 = List(Allele(1))
      val mix2 = List(Allele(2))
      val v1 = List(Allele(1))

      val level = genotypeMatchOfMixes(mix1, Some(v1), mix2, None)
      level must beEqualTo(Stringency.Mismatch)
      val level2 = genotypeMatchOfMixes(mix2, None, mix1, Some(v1))
      level2 must beEqualTo(Stringency.Mismatch)
    }

  }

}