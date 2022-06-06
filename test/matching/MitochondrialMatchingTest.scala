package matching

import java.util.Date

import configdata.{MatchingRule, MtConfiguration, MtRegion}
import matching.MatchingAlgorithm._
import matching.Stringency._
import org.scalatestplus.play.PlaySpec
import profile.{Allele, Analysis, Mitocondrial, Profile}
import types.{AlphanumericId, MongoDate, SampleCode}

class MitochondrialMatchingTest extends PlaySpec {

  val config = MtConfiguration(
    List(
      MtRegion("HV1", (16023, 16428)),
      MtRegion("HV2", (50, 340)),
      MtRegion("HV3", (341, 600))),
    List(16193, 309, 455, 463, 573))

  "mtDnaMatch" must {
    "return no differences for 1 exact match" in {
      val s1 = List(Mitocondrial('A', 100), Mitocondrial('G', 105), Mitocondrial('T', 106))

      val result = mtDnaMatch(config, s1, s1)

      result mustBe 0
    }
//    "ale" in {
//      val s1 = List(
//        Mitocondrial('-',551),
//        Mitocondrial('H',553),
//        Mitocondrial('-',555))
//
//      val s2 = List(
//        Mitocondrial('-',551),
//        Mitocondrial('H',554),
//        Mitocondrial('-',555))
//
//      val result = mtDnaMatch( s1, s2)
//
//      result mustBe 0
//    }

   "return 1 difference" in {
      val s1 = List(Mitocondrial('A', 100))
      val s2 = List(Mitocondrial('A', 100), Mitocondrial('G', 101))

      mtDnaMatch(config, s1, s2) mustBe 1
      mtDnaMatch(config, s2, s1) mustBe 1
    }

    "return 2 differences" in {
      val s1 = List(Mitocondrial('A', 100))
      val s2 = List(Mitocondrial('A', 100), Mitocondrial('G', 105), Mitocondrial('T', 106))

      mtDnaMatch(config,s1, s2) mustBe 2
      mtDnaMatch(config, s2, s1) mustBe 2
    }

/**
Q: CAGGTA
K:  TAGATA
**/

    "return mismatches for exclusion example" in {
      val s1 = Seq(Mitocondrial('C', 100), Mitocondrial('A', 101), Mitocondrial('G', 102), Mitocondrial('G', 103), Mitocondrial('T', 104), Mitocondrial('A', 105))
      val s2 = Seq(Mitocondrial('T', 100), Mitocondrial('A', 101), Mitocondrial('G', 102), Mitocondrial('A', 103), Mitocondrial('T', 104), Mitocondrial('A', 105))

      val result = mtDnaMatch(config, s1, s2)

      result mustBe 2
    }
  }

  "baseMatch" must {
    "match bases" in {
      baseMatch('A', 'A') mustBe true
      baseMatch('A', 'T') mustBe false
      baseMatch('A', 'C') mustBe false
      baseMatch('A', 'G') mustBe false

      baseMatch('T', 'T') mustBe true
      baseMatch('T', 'A') mustBe false
      baseMatch('T', 'C') mustBe false
      baseMatch('T', 'G') mustBe false

      baseMatch('C', 'C') mustBe true
      baseMatch('C', 'A') mustBe false
      baseMatch('C', 'T') mustBe false
      baseMatch('C', 'G') mustBe false

      baseMatch('G', 'G') mustBe true
      baseMatch('G', 'A') mustBe false
      baseMatch('G', 'T') mustBe false
      baseMatch('G', 'C') mustBe false
    }

/**
R	A or G
Y	C or T
S	G or C
W	A or T
K	G or T
M	A or C
B	C or G or T
D	A or G or T
H	A or C or T
V	A or C or G
N	any base
**/

    "match wildcards against bases" in {
      baseMatch('R', 'A') mustBe true
      baseMatch('R', 'G') mustBe true
      baseMatch('R', 'C') mustBe false
      baseMatch('R', 'T') mustBe false

      baseMatch('Y', 'C') mustBe true
      baseMatch('Y', 'T') mustBe true
      baseMatch('Y', 'A') mustBe false
      baseMatch('Y', 'G') mustBe false

      baseMatch('S', 'G') mustBe true
      baseMatch('S', 'C') mustBe true
      baseMatch('S', 'A') mustBe false
      baseMatch('S', 'T') mustBe false

      baseMatch('W', 'A') mustBe true
      baseMatch('W', 'T') mustBe true
      baseMatch('W', 'C') mustBe false
      baseMatch('W', 'G') mustBe false

      baseMatch('K', 'G') mustBe true
      baseMatch('K', 'T') mustBe true
      baseMatch('K', 'A') mustBe false
      baseMatch('K', 'C') mustBe false

      baseMatch('M', 'A') mustBe true
      baseMatch('M', 'C') mustBe true
      baseMatch('M', 'G') mustBe false
      baseMatch('M', 'T') mustBe false

/**
B	C or G or T
D	A or G or T
H	A or C or T
V	A or C or G
N	any base
**/

      baseMatch('B', 'C') mustBe true
      baseMatch('B', 'G') mustBe true
      baseMatch('B', 'T') mustBe true
      baseMatch('B', 'A') mustBe false

      baseMatch('D', 'A') mustBe true
      baseMatch('D', 'G') mustBe true
      baseMatch('D', 'T') mustBe true
      baseMatch('D', 'C') mustBe false

      baseMatch('H', 'A') mustBe true
      baseMatch('H', 'C') mustBe true
      baseMatch('H', 'T') mustBe true
      baseMatch('H', 'G') mustBe false

      baseMatch('V', 'A') mustBe true
      baseMatch('V', 'C') mustBe true
      baseMatch('V', 'G') mustBe true
      baseMatch('V', 'T') mustBe false

      baseMatch('N', 'A') mustBe true
      baseMatch('N', 'C') mustBe true
      baseMatch('N', 'G') mustBe true
      baseMatch('N', 'T') mustBe true
    }
  }

  "profileMtMatch" must {
    "match with 0 mismatches" in {
      val p = Profile(null, SampleCode("AR-C-SHDG-0001"), null, null, null,
        Map(4-> Map(
       "HV2_RANGE"->List(Allele(10),Allele(115)), "HV2" -> List(Mitocondrial('A', 100)),
       "HV3_RANGE"->List(Allele(130),Allele(200)),"HV3" -> List(Mitocondrial('G', 150))
      )), None, None, None, None)

      val q = Profile(null, SampleCode("AR-C-SHDG-0002"), null, null, null,
        Map(4-> Map(
          "HV1_RANGE"->List(Allele(50),Allele(110)), "HV1" -> List(Mitocondrial('A', 100)),
          "HV2_RANGE"->List(Allele(115),Allele(125)), "HV2" -> List(Mitocondrial('C', 120))
        )), None, None, None, None)

      val mismatches = 0
      val matchingRule = MatchingRule(4, AlphanumericId("IR"), Stringency.HighStringency, true, true, Algorithm.ENFSI, 11, mismatches, true)

      val result = profileMtMatch(config, p, q, matchingRule, None, 1000)

      result.isDefined mustBe true
      result.get.result.stringency mustBe HighStringency
      result.get.result.matchingAlleles mustBe Map("A@100" -> HighStringency)
    }

    "match with 4 mismatches" in {
      val p = Profile(null, SampleCode("AR-C-SHDG-0001"), null, null, null,
         Map(4-> Map(
          "HV2_RANGE" -> List(Allele(200),Allele(450)), "HV2" -> List(Mitocondrial('C', 432)),
          "HV1_RANGE" -> List(Allele(1),Allele(200)), "HV1" -> List(Mitocondrial('A', 100), Mitocondrial('G', 150)),
          "HV3_RANGE" -> List(Allele(16024),Allele(16569)), "HV3" -> List(Mitocondrial('A', 16025.1), Mitocondrial('T', 16401))
         )),None, None, None, None)

      val q = Profile(null, SampleCode("AR-C-SHDG-0002"), null, null, null,
        Map(4-> Map(
       "HV2_RANGE" -> List(Allele(1),Allele(200)), "HV2" -> List(Mitocondrial('A', 100)),
       "HV3_RANGE" -> List(Allele(300),Allele(576)), "HV3" -> List(Mitocondrial('A', 450), Mitocondrial('C', 401.1)),
       "HV1_RANGE" -> List(Allele(450),Allele(550)), "HV1" -> List(Mitocondrial('Y', 500))
      )),None, None, None, None)

      val mismatches = 4
      val matchingRule = MatchingRule(4, AlphanumericId("IR"), Stringency.HighStringency, true, true, Algorithm.ENFSI, 11, mismatches, true)

      val result = profileMtMatch(config, p, q, matchingRule, None, 1000)

      result.isDefined mustBe true
      result.get.result.stringency mustBe HighStringency
      result.get.result.matchingAlleles mustBe Map("A@100" -> HighStringency)
    }

    "not match with 1 mismatch" in {
      val p = Profile(null, SampleCode("AR-C-SHDG-0001"), null, null, null,
        Map(4 -> Map(
        "HV2_RANGE" -> List(Allele(50), Allele(200)),  "HV2" -> List(Mitocondrial('A', 100),Mitocondrial('G', 150)),
        "HV1_RANGE" -> List(Allele(16024), Allele(16569)),"HV1" -> List(Mitocondrial('A', 16025.1), Mitocondrial('T', 16401),Mitocondrial('C', 16025.1)),
        "HV3_RANGE" -> List(Allele(300), Allele(450)), "HV3" -> List(Mitocondrial('C', 432))
        )),None, None, None, None)

      val q = Profile(null, SampleCode("AR-C-SHDG-0002"), null, null, null,
         Map(4 -> Map(
       "HV2_RANGE" -> List(Allele(10), Allele(1500)),  "HV2" -> List(Mitocondrial('A', 100))  ,
       "HV3_RANGE" -> List(Allele(450), Allele(576)),"HV3" -> List(Mitocondrial('A', 450),Mitocondrial('Y', 500), Mitocondrial('C', 441.1), Mitocondrial('-', 559))
      )),None, None, None, None)

      val mismatches = 1
      val matchingRule = MatchingRule(4, AlphanumericId("IR"), Stringency.HighStringency, true, true, Algorithm.ENFSI, 11, mismatches, true)

      val result = profileMtMatch(config, p, q, matchingRule, None, 1000)

      result.isDefined mustBe false
    }

    "merge ranges and match" in {
      val p = Profile(null, SampleCode("AR-C-SHDG-0001"), null, null, null,
       Map( 4 -> Map(
         "HV2_RANGE" -> List(Allele(100), Allele(200)),
         "HV2" -> List(Mitocondrial('C', 120)) ) ),None, None, None, None)

      val q = Profile(null, SampleCode("AR-C-SHDG-0002"), null, null, null,
        Map( 4 -> Map(
          "HV2_RANGE" -> List(Allele(200), Allele(300)),
          "HV2" -> List(Mitocondrial('T', 251.1))  )  ),None, None, None, None)

      val matchingRule = MatchingRule(4, AlphanumericId("IR"), Stringency.HighStringency, true, true, Algorithm.ENFSI, 11, 0, true)

      val result = profileMtMatch(config, p, q, matchingRule, None, 1000)

      result.isDefined mustBe true
      result.get.result.stringency mustBe HighStringency
      result.get.result.matchingAlleles mustBe Map.empty
    }

    "merge ranges and not match" in {
      val p = Profile(null, SampleCode("AR-C-SHDG-0001"), null, null, null,
          Map(4-> Map(
            "HV2_RANGE" -> List(Allele(100), Allele(200)),
            "HV2" -> List(Mitocondrial('C', 120), Mitocondrial('A', 160))
          )),None, None, None, None)

      val q = Profile(null, SampleCode("AR-C-SHDG-0002"), null, null, null,
       Map(4-> Map(
          "HV2_RANGE" -> List(Allele(150), Allele(250)),
          "HV2" -> List(Mitocondrial('T', 220.1), Mitocondrial('C', 160))
        )), None , None, None, None)

      val matchingRule = MatchingRule(4, AlphanumericId("IR"), Stringency.HighStringency, true, true, Algorithm.ENFSI, 11, 0, true)

      val result = profileMtMatch(config,p, q, matchingRule, None, 1000)

      result.isDefined mustBe false
    }
     /*
    "ignore differences in ignore points" in {
      val p = Profile(null, SampleCode("AR-C-SHDG-0001"), null, null, null, Map.empty, Some(List(
        Analysis("1", MongoDate(new Date()), "KIT", Map(
          "HV2" -> List(Mitocondrial('C', 309), Mitocondrial('A', 160))
        ), Some(4))
      )), None, None, None)

      val q = Profile(null, SampleCode("AR-C-SHDG-0002"), null, null, null, Map.empty, Some(List(
        Analysis("1", MongoDate(new Date()), "KIT", Map(
          "HV2" -> List(Mitocondrial('T', 309), Mitocondrial('A', 160))
        ), Some(4))
      )), None, None, None)

      val matchingRule = MatchingRule(4, AlphanumericId("IR"), Stringency.HighStringency, true, true, Algorithm.ENFSI, 11, 0, true)

      val result = profileMtMatch(config, p, q, matchingRule, None, 1000)

      result.isDefined mustBe true
      result.get.result.stringency mustBe HighStringency
      result.get.result.matchingAlleles mustBe Map("HV2" -> HighStringency)
    }    */
        /*
    "ignore different insertions in ignore points" in {
      val p = Profile(null, SampleCode("AR-C-SHDG-0001"), null, null, null, Map.empty, Some(List(
        Analysis("1", MongoDate(new Date()), "KIT", Map(
          "HV2" -> List(Mitocondrial('C', 309.1), Mitocondrial('A', 160))
        ), Some(4))
      )), None, None, None)

      val q = Profile(null, SampleCode("AR-C-SHDG-0002"), null, null, null, Map.empty, Some(List(
        Analysis("1", MongoDate(new Date()), "KIT", Map(
          "HV2" -> List(Mitocondrial('T', 309.1), Mitocondrial('A', 160))
        ), Some(4))
      )), None, None, None)

      val matchingRule = MatchingRule(4, AlphanumericId("IR"), Stringency.HighStringency, true, true, Algorithm.ENFSI, 11, 0, true)

      val result = profileMtMatch(config, p, q, matchingRule, None, 1000)

      result.isDefined mustBe true
      result.get.result.stringency mustBe HighStringency
      result.get.result.matchingAlleles mustBe Map("HV2" -> HighStringency)
    }       */

    "not match if a profile doesn't have analysis" in {
      val p = Profile(null, SampleCode("AR-C-SHDG-0001"), null, null, null, Map.empty, None, None, None, None)

      val q = Profile(null, SampleCode("AR-C-SHDG-0002"), null, null, null, Map(4-> Map(
       "HV2_RANGE" -> List(Allele(100), Allele(200)),"HV2" -> List(Mitocondrial('A', 100)),
       "HV3_RANGE" -> List(Allele(400), Allele(576)), "HV3" -> List(Mitocondrial('A', 450),Mitocondrial('Y', 500), Mitocondrial('C', 401.1), Mitocondrial('-', 569))
      )), None, None, None, None)

      val matchingRule = MatchingRule(4, AlphanumericId("IR"), Stringency.HighStringency, true, true, Algorithm.ENFSI, 11, 0, true)

      val result = profileMtMatch(config, p, q, matchingRule, None, 1000)

      result.isDefined mustBe false
    }

    "not match if the only mergeable range has mismatch" in {
      val p = Profile(null, SampleCode("AR-C-SHDG-0001"), null, null, null, Map ( 4-> Map(
          "HV2_RANGE" -> List(Allele(50), Allele(70)),
          "HV2" -> List(Mitocondrial('C', 65))
      )), None, None, None, None)

      val q = Profile(null, SampleCode("AR-C-SHDG-0002"), null, null, null, Map( 4-> Map(
          "HV2_RANGE" -> List(Allele(50), Allele(70)),
          "HV2" -> List(Mitocondrial('C', 65), Mitocondrial('C', 32.1), Mitocondrial('-', 68)),
          "HV1_RANGE" -> List(Allele(120), Allele(150)),
          "HV1" -> List(Mitocondrial('Y', 135))
      )), None, None, None, None)

      val matchingRule = MatchingRule(4, AlphanumericId("IR"), Stringency.HighStringency, true, true, Algorithm.ENFSI, 11, 0, true)

      val result = profileMtMatch(config, p, q, matchingRule, None, 1000)

      result.isDefined mustBe false
    }
  }

  "merge ranges" must {
    "merge (a,b) and (c,d) when a<c and b<d" in {
      val range1 = (1,5)
      val range2 = (2,8)
      mergeRanges(range1, range2) mustBe Some(2,5)
    }
    "merge (a,b) and (c,d) when a==c and b<d" in {
      val range1 = (1,5)
      val range2 = (1,10)
      mergeRanges(range1, range2) mustBe Some(1,5)
    }
    "merge (a,b) and (c,d) when a<c and b==d" in {
      val range1 = (1,10)
      val range2 = (5,10)
      mergeRanges(range1, range2) mustBe Some(5,10)
    }
    "merge (a,b) and (c,d) when a>c and b>d" in {
      val range1 = (2,8)
      val range2 = (1,5)
      mergeRanges(range1, range2) mustBe Some(2,5)
    }
    "merge (a,b) and (c,d) when a==c and b>d" in {
      val range1 = (1,8)
      val range2 = (1,5)
      mergeRanges(range1, range2) mustBe Some(1,5)
    }
    "merge (a,b) and (c,d) when a>c and b==d" in {
      val range1 = (5,10)
      val range2 = (1,10)
      mergeRanges(range1, range2) mustBe Some(5,10)
    }
    "merge (a,b) and (c,d) when a<c and b>d" in {
      val range1 = (1,5)
      val range2 = (3,4)
      mergeRanges(range1, range2) mustBe Some(3,4)
    }
    "merge (a,b) and (c,d) when a>c and b<d" in {
      val range1 = (3,4)
      val range2 = (1,5)
      mergeRanges(range1, range2) mustBe Some(3,4)
    }
    "merge (a,b) and (c,d) when b==c" in {
      val range1 = (1,5)
      val range2 = (5,10)
      mergeRanges(range1, range2) mustBe Some(5,5)
    }
    "merge (a,b) and (c,d) when a==d" in {
      val range1 = (5,10)
      val range2 = (1,5)
      mergeRanges(range1, range2) mustBe Some(5,5)
    }
    "merge (a,b) and (c,d) when c>b and d>b" in {
      val range1 = (5,10)
      val range2 = (1,3)
      mergeRanges(range1, range2) mustBe None
    }
    "merge (a,b) and (c,d) when a>d and b>d" in {
      val range1 = (1,3)
      val range2 = (5,10)
      mergeRanges(range1, range2) mustBe None
    }
  }

}