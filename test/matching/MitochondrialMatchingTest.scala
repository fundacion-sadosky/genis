package matching

import configdata.{MatchingRule, MtConfiguration, MtRegion}
import matching.MatchingAlgorithm._
import matching.Stringency._
import org.scalatestplus.play.PlaySpec
import profile.{Allele, Mitocondrial, MtRCRS, Profile, ProfileBuilder}
import types.{AlphanumericId, SampleCode}

class MitochondrialMatchingTest extends PlaySpec {
  val hv1Start = 16023
  val hv1End = 16428
  val hv2Start = 50
  val hv2End = 340
  val hv3Start = 341
  val hv3End = 600
  val ignorePoints:List[Int] = List(
    16193, 309, 455, 463, 574
  )
  val config: MtConfiguration = MtConfiguration(
    List(
      MtRegion("HV1", (hv1Start, hv1End)),
      MtRegion("HV2", (hv2Start, hv2End)),
      MtRegion("HV3", (hv3Start, hv3End))
    ),
    ignorePoints
  )
  val mtRcrs:MtRCRS = MtRCRS(
    Map(
      68 -> "G",
      75 -> "G",
      100 -> "G",
      101 -> "G",
      102 -> "A",
      103 -> "G",
      104 -> "C",
      105 -> "C",
      106 -> "G",
      120 -> "C",
      135 -> "T",
      150 -> "C",
      160 -> "A",
      432 -> "A",
      450 -> "T",
      500 -> "C",
      559 -> "C",
      569 -> "C",
      16401 -> "C"
    )
  )
  
  "mtDnaMatch" must {
    "return no differences for 1 exact match" in {
      val s1 = List(
        Mitocondrial('A', 100),
        Mitocondrial('G', 105),
        Mitocondrial('T', 106)
      )
      val result = mtDnaMatch(config, s1, s1, mtRcrs)
      result mustBe 0
    }

   "return 1 difference" in {
      val s1 = List(Mitocondrial('A', 100))
      val s2 = List(Mitocondrial('A', 100), Mitocondrial('T', 101))
      mtDnaMatch(config, s1, s2, mtRcrs) mustBe 1
      mtDnaMatch(config, s2, s1, mtRcrs) mustBe 1
    }

    "return 2 differences" in {
      val s1 = List(Mitocondrial('A', 100))
      val s2 = List(
        Mitocondrial('A', 100),
        Mitocondrial('G', 105),
        Mitocondrial('T', 106)
      )
      mtDnaMatch(config, s1, s2, mtRcrs) mustBe 2
      mtDnaMatch(config, s2, s1, mtRcrs) mustBe 2
    }
    
    """return 0 differences when there is a mutation apperring in only one range
       that matches with Mt reference sequence""" in {
      val s1a = List(Mitocondrial('G', 100))
      val s1b = List(Mitocondrial('S', 100))
      val s1c = List(Mitocondrial('K', 100))
      val s1d = List(Mitocondrial('R', 100))
      val s1e = List(Mitocondrial('N', 100))
      val s2 = List()
      mtDnaMatch(config, s1a, s2, mtRcrs) mustBe 0
      mtDnaMatch(config, s1b, s2, mtRcrs) mustBe 0
      mtDnaMatch(config, s1c, s2, mtRcrs) mustBe 0
      mtDnaMatch(config, s1d, s2, mtRcrs) mustBe 0
      mtDnaMatch(config, s1e, s2, mtRcrs) mustBe 0
    }
    
    """return 1 differences when there is a mutation apperring in only one range
   that does not match with Mt reference sequence""" in {
      val s1a = List(Mitocondrial('A', 100))
      val s1b = List(Mitocondrial('W', 100))
      val s1c = List(Mitocondrial('H', 100))
      val s2 = List()
      mtDnaMatch(config, s1a, s2, mtRcrs) mustBe 1
      mtDnaMatch(config, s1b, s2, mtRcrs) mustBe 1
      mtDnaMatch(config, s1c, s2, mtRcrs) mustBe 1
    }

    /**
Q: CAGGTA
K:  TAGATA
**/

    "return mismatches for exclusion example" in {
      val s1 = Seq(
        Mitocondrial('C', 100),
        Mitocondrial('A', 101),
        Mitocondrial('G', 102),
        Mitocondrial('G', 103),
        Mitocondrial('T', 104),
        Mitocondrial('A', 105)
      )
      val s2 = Seq(
        Mitocondrial('T', 100),
        Mitocondrial('A', 101),
        Mitocondrial('G', 102),
        Mitocondrial('A', 103),
        Mitocondrial('T', 104),
        Mitocondrial('A', 105)
      )
      val result = mtDnaMatch(config, s1, s2, mtRcrs)
      result mustBe 2
    }
    
    "return 2 mismatch for mutation vs polymorphism" in  {
      val s1 = Seq(
        Mitocondrial('A', 100)
      )
      val s2 = Seq(
        Mitocondrial('G', 100.1)
      )
      val result = mtDnaMatch(config, s1, s2, mtRcrs)
      result mustBe 2
    }
    
  }

  "mtDnaMatchWithMatchingAlleles" must {
    "return no differences for 1 exact match" in {
      val s1 = List(
        Mitocondrial('A', 100),
        Mitocondrial('G', 105),
        Mitocondrial('T', 106)
      )
      val result = mtDnaMatchWithMatchingAlleles(config, s1, s1, mtRcrs)
      result._1 mustBe 0
      result._2.length mustBe 3
      result._2.contains(Mitocondrial('A', 100)) mustBe true
      result._2.contains(Mitocondrial('G', 105)) mustBe true
      result._2.contains(Mitocondrial('T', 106)) mustBe true
    }

    "return 1 difference" in {
      val s1 = List(Mitocondrial('A', 100))
      val s2 = List(Mitocondrial('A', 100), Mitocondrial('T', 101))
      val res1 = mtDnaMatchWithMatchingAlleles(config, s1, s2, mtRcrs)
      res1._1 mustBe 1
      res1._2.length mustBe 1
      res1._2.contains(Mitocondrial('A', 100)) mustBe true
      val res2 = mtDnaMatchWithMatchingAlleles(config, s2, s1, mtRcrs)
      res2._1 mustBe 1
      res2._2.length mustBe 1
      res2._2.contains(Mitocondrial('A', 100)) mustBe true
    }

    "return 2 differences" in {
      val s1 = List(Mitocondrial('A', 100))
      val s2 = List(
        Mitocondrial('A', 100),
        Mitocondrial('G', 105),
        Mitocondrial('T', 106)
      )
      val res1 = mtDnaMatchWithMatchingAlleles(config, s1, s2, mtRcrs)
      res1._1 mustBe 2
      res1._2.length mustBe 1
      res1._2.contains(Mitocondrial('A', 100)) mustBe true
      val res2 = mtDnaMatchWithMatchingAlleles(config, s2, s1, mtRcrs)
      res2._1 mustBe 2
      res2._2.length mustBe 1
      res2._2.contains(Mitocondrial('A', 100)) mustBe true
    }

    """return 0 differences when there is a mutation apperring in only one range
     that matches with Mt reference sequence""" in {
      val s1a = List(Mitocondrial('G', 100))
      val s1b = List(Mitocondrial('S', 100))
      val s1c = List(Mitocondrial('K', 100))
      val s1d = List(Mitocondrial('R', 100))
      val s1e = List(Mitocondrial('N', 100))
      val s2 = List()
      val res1 = mtDnaMatchWithMatchingAlleles(config, s1a, s2, mtRcrs)
      res1._1 mustBe 0
      res1._2.length mustBe 1
      res1._2.contains(Mitocondrial('G', 100)) mustBe true
      val res2 = mtDnaMatchWithMatchingAlleles(config, s1b, s2, mtRcrs)
      res2._1 mustBe 0
      res2._2.length mustBe 1
      res2._2.contains(Mitocondrial('S', 100)) mustBe true
      val res3 = mtDnaMatchWithMatchingAlleles(config, s1c, s2, mtRcrs)
      res3._1 mustBe 0
      res3._2.length mustBe 1
      res3._2.contains(Mitocondrial('K', 100)) mustBe true
      val res4 = mtDnaMatchWithMatchingAlleles(config, s1d, s2, mtRcrs)
      res4._1 mustBe 0
      res4._2.length mustBe 1
      res4._2.contains(Mitocondrial('R', 100)) mustBe true
      val res5 = mtDnaMatchWithMatchingAlleles(config, s1e, s2, mtRcrs)
      res5._1 mustBe 0
      res5._2.length mustBe 1
      res5._2.contains(Mitocondrial('N', 100)) mustBe true
    }

    """return 1 differences when there is a mutation in only one range
 that does not match with Mt reference sequence""" in {
      val s1a = List(Mitocondrial('A', 100))
      val s1b = List(Mitocondrial('W', 100))
      val s1c = List(Mitocondrial('H', 100))
      val s2 = List()
      val res1 = mtDnaMatchWithMatchingAlleles(config, s1a, s2, mtRcrs)
      res1._1 mustBe 1
      res1._2.isEmpty mustBe true
      val res2 = mtDnaMatchWithMatchingAlleles(config, s1b, s2, mtRcrs)
      res2._1 mustBe 1
      res2._2.isEmpty mustBe true
      val res3 = mtDnaMatchWithMatchingAlleles(config, s1c, s2, mtRcrs)
      res3._1 mustBe 1
      res3._2.isEmpty mustBe true
    }
    /**
     * Q: CAGGTA
     * K:  TAGATA
     * */
    "return mismatches for exclusion example" in {
      val s1 = Seq(
        Mitocondrial('C', 100),
        Mitocondrial('A', 101),
        Mitocondrial('G', 102),
        Mitocondrial('G', 103),
        Mitocondrial('T', 104),
        Mitocondrial('A', 105)
      )
      val s2 = Seq(
        Mitocondrial('T', 100),
        Mitocondrial('A', 101),
        Mitocondrial('G', 102),
        Mitocondrial('A', 103),
        Mitocondrial('T', 104),
        Mitocondrial('A', 105)
      )
      val result = mtDnaMatchWithMatchingAlleles(config, s1, s2, mtRcrs)
      result._1 mustBe 2
      result._2.length mustBe 4
      result._2.contains(Mitocondrial('A', 101)) mustBe true
      result._2.contains(Mitocondrial('G', 102)) mustBe true
      result._2.contains(Mitocondrial('T', 104)) mustBe true
      result._2.contains(Mitocondrial('A', 105)) mustBe true
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
R A or G
Y C or T
S G or C
W A or T
K G or T
M A or C
B C or G or T
D A or G or T
H A or C or T
V A or C or G
N any base
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
B C or G or T
D A or G or T
H A or C or T
V A or C or G
N any base
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
      val pGeno = Map(
        4 -> Map(
          "HV2_RANGE" -> List(Allele(10), Allele(115)),
          "HV2" -> List(Mitocondrial('A', 100)), // ref 100 -> "G"
          "HV3_RANGE" -> List(Allele(130), Allele(200)),
          "HV3" -> List(Mitocondrial('G', 150)) // 150 -> "C"
        )
      )
      val p = ProfileBuilder
        .withGlobalCode(SampleCode("AR-C-SHDG-0001"))
        .withGenotypification(pGeno)
        .build()
      val qGeno = Map(
        4 -> Map(
          "HV1_RANGE" -> List(Allele(50), Allele(110)),
          "HV1" -> List(Mitocondrial('A', 100)), // ref 100 -> "G"
          "HV2_RANGE" -> List(Allele(115), Allele(125)),
          "HV2" -> List(Mitocondrial('G', 120)) // ref 120 -> "C"
        )
      )
      val q = ProfileBuilder
        .withGlobalCode(SampleCode("AR-C-SHDG-0002"))
        .withGenotypification(qGeno)
        .build()

      val mismatches = 0
      val matchingRule = MatchingRule(
        4,
        AlphanumericId("IR"),
        Stringency.HighStringency,
        failOnMatch = true,
        forwardToUpper = true,
        Algorithm.ENFSI,
        11,
        mismatches,
        considerForN = true
      )
      val result = profileMtMatch(
        config,
        p,
        q,
        matchingRule,
        mtRcrs,
        None,
        1000
      )
      result.isDefined mustBe true
      result.get.result.stringency mustBe HighStringency
      val expectedMatchingAlleles = Map(
        "A@100" -> HighStringency,
        "G@120" -> HighStringency,
        "G@150" -> HighStringency
      )
      result.get.result.matchingAlleles mustBe expectedMatchingAlleles
    }

    """match with 0 mismatches if a mutation falls in one range and matches to
      |reference mt sequence""".stripMargin in {
      val pGeno = Map(
        4 -> Map(
          "HV2_RANGE" -> List(Allele(10), Allele(115)),
          "HV2" -> List(Mitocondrial('R', 100)),
          "HV3_RANGE" -> List(Allele(130), Allele(200)),
          "HV3" -> List(Mitocondrial('S', 150))
        )
      )
      val p = ProfileBuilder
        .withGlobalCode(SampleCode("AR-C-SHDG-0001"))
        .withGenotypification(pGeno)
        .build()
      val qGeno = Map(
        4 -> Map(
          "HV1_RANGE" -> List(Allele(50), Allele(110)),
          "HV1" -> List(),
          "HV2_RANGE" -> List(Allele(115), Allele(175)),
          "HV2" -> List()
        )
      )
      val q = ProfileBuilder
        .withGlobalCode(SampleCode("AR-C-SHDG-0002"))
        .withGenotypification(qGeno)
        .build()

      val mismatches = 0
      val matchingRule = MatchingRule(
        4,
        AlphanumericId("IR"),
        Stringency.HighStringency,
        failOnMatch = true,
        forwardToUpper = true,
        Algorithm.ENFSI,
        11,
        mismatches,
        considerForN = true
      )
      val result = profileMtMatch(
        config,
        p,
        q,
        matchingRule,
        mtRcrs,
        None,
        1000
      )
      result.isDefined mustBe true
      result.get.result.stringency mustBe HighStringency
      val expectedMatchingAlleles = Map(
        "R@100" -> HighStringency,
        "S@150" -> HighStringency
      )
      result.get.result.matchingAlleles mustBe expectedMatchingAlleles
    }
    
    """match with 0 mismatches if a mutation falls in one range, does not match
      |the reference mt sequence and the position of the mutation
      | is out of the second range""".stripMargin in {
      val pGeno = Map(
        4 -> Map(
          "HV1_RANGE" -> List(Allele(10), Allele(115)),
          "HV1" -> List(
            Mitocondrial('G', 75),
            Mitocondrial('C', 100)
          )
        )
      )
      val p = ProfileBuilder
        .withGlobalCode(SampleCode("AR-C-SHDG-0001"))
        .withGenotypification(pGeno)
        .build()
      val qGeno = Map(
        4 -> Map(
          "HV1_RANGE" -> List(Allele(10), Allele(80)),
          "HV1" -> List(
            Mitocondrial('G', 75)
          )
        )
      )
      val q = ProfileBuilder
        .withGlobalCode(SampleCode("AR-C-SHDG-0002"))
        .withGenotypification(qGeno)
        .build()

      val mismatches = 0
      val matchingRule = MatchingRule(
        4,
        AlphanumericId("IR"),
        Stringency.HighStringency,
        failOnMatch = true,
        forwardToUpper = true,
        Algorithm.ENFSI,
        11,
        mismatches,
        considerForN = true
      )
      val result = profileMtMatch(
        config,
        p,
        q,
        matchingRule,
        mtRcrs,
        None,
        1000
      )
      result.isDefined mustBe true
      result.get.result.stringency mustBe HighStringency
      val expectedMatchingAlleles = Map(
        "G@75" -> HighStringency,
        "C@100" -> HighStringency
      )
      result.get.result.matchingAlleles mustBe expectedMatchingAlleles
    }

    """match with 2 mismatches if mutations falls in one range and does not
      |match to reference mt sequence""".stripMargin in {
      val pGeno = Map(
        4 -> Map(
          "HV2_RANGE" -> List(Allele(10), Allele(115)),
          "HV2" -> List(Mitocondrial('Y', 100)),
          "HV3_RANGE" -> List(Allele(130), Allele(200)),
          "HV3" -> List(Mitocondrial('W', 150))
        )
      )
      val p = ProfileBuilder
        .withGlobalCode(SampleCode("AR-C-SHDG-0001"))
        .withGenotypification(pGeno)
        .build()
      val qGeno = Map(
        4 -> Map(
          "HV1_RANGE" -> List(Allele(50), Allele(110)),
          "HV1" -> List(),
          "HV2_RANGE" -> List(Allele(115), Allele(175)),
          "HV2" -> List()
        )
      )
      val q = ProfileBuilder
        .withGlobalCode(SampleCode("AR-C-SHDG-0002"))
        .withGenotypification(qGeno)
        .build()

      val mismatches = 2
      val matchingRule = MatchingRule(
        4,
        AlphanumericId("IR"),
        Stringency.HighStringency,
        failOnMatch = true,
        forwardToUpper = true,
        Algorithm.ENFSI,
        11,
        mismatches,
        considerForN = true
      )
      val result = profileMtMatch(
        config,
        p,
        q,
        matchingRule,
        mtRcrs,
        None,
        1000
      )
      result.isDefined mustBe true
      result.get.result.stringency mustBe HighStringency
      result.get.result.matchingAlleles mustBe Map()
    }
    "match with 4 mismatches" in {
      val pGeno = Map(
        4 -> Map(
          "HV2_RANGE" -> List(Allele(200), Allele(450)),
          "HV2" -> List(Mitocondrial('C', 432)), // ref 432 -> "A"
          "HV1_RANGE" -> List(Allele(1), Allele(200)),
          "HV1" -> List(
            Mitocondrial('A', 100), // ref 100 -> "G"
            Mitocondrial('G', 150)  // ref 150 -> "C",
          ),
          "HV3_RANGE" -> List(Allele(16024), Allele(16569)),
          "HV3" -> List(
            Mitocondrial('A', 16025.1),
            Mitocondrial('T', 16401) // ref 16401 -> "C"
          )
        )
      )
      val p = ProfileBuilder
        .withGlobalCode(SampleCode("AR-C-SHDG-0001"))
        .withGenotypification(pGeno)
        .build()
      val qGeno = Map(
        4 -> Map(
          "HV2_RANGE" -> List(Allele(1), Allele(200)),
          "HV2" -> List(Mitocondrial('A', 100)), // ref 100 -> "G"
          "HV3_RANGE" -> List(Allele(300), Allele(576)),
          "HV3" -> List(
            Mitocondrial('A', 450), // 450 -> "T"
            Mitocondrial('C', 401.1)
          ),
          "HV1_RANGE" -> List(Allele(450), Allele(550)),
          "HV1" -> List(Mitocondrial('Y', 500)) // ref 500 -> "C"
        )
      )
      val q = ProfileBuilder
        .withGlobalCode(SampleCode("AR-C-SHDG-0002"))
        .withGenotypification(qGeno)
        .build()
      val mismatches = 4
      val matchingRule = MatchingRule(
        4,
        AlphanumericId("IR"),
        Stringency.HighStringency,
        failOnMatch = true,
        forwardToUpper = true,
        Algorithm.ENFSI,
        11,
        mismatches,
        considerForN = true
      )
      val result = profileMtMatch(
        config,
        p,
        q,
        matchingRule,
        mtRcrs,
        None,
        1000
      )
      result.isDefined mustBe true
      result.get.result.stringency mustBe HighStringency
      val expectedMatchingAlleles = Map(
        "A@100" -> HighStringency,
        "Y@500" -> HighStringency,
        "A@16025,1" -> HighStringency,
        "T@16401" -> HighStringency
      )
      result.get.result.matchingAlleles mustBe expectedMatchingAlleles
    }

    "not match with 1 mismatch" in {
      val pGeno = Map(
        4 -> Map(
          "HV2_RANGE" -> List(Allele(50), Allele(200)),
          "HV2" -> List(Mitocondrial('A', 100), Mitocondrial('G', 150)),
          "HV1_RANGE" -> List(Allele(16024), Allele(16569)),
          "HV1" -> List(
            Mitocondrial('A', 16025.1),
            Mitocondrial('T', 16401),
            Mitocondrial('C', 16025.1)
          ),
          "HV3_RANGE" -> List(Allele(300), Allele(450)),
          "HV3" -> List(Mitocondrial('C', 432))
        )
      )
      val p = ProfileBuilder
        .withGlobalCode(SampleCode("AR-C-SHDG-0001"))
        .withGenotypification(pGeno)
        .build()

      val qGeno = Map(
        4 -> Map(
          "HV2_RANGE" -> List(Allele(10), Allele(1500)),
          "HV2" -> List(Mitocondrial('A', 100)),
          "HV3_RANGE" -> List(Allele(450), Allele(576)),
          "HV3" -> List(
            Mitocondrial('A', 450),
            Mitocondrial('Y', 500),
            Mitocondrial('C', 441.1),
            Mitocondrial('-', 559)
          )
        )
      )
      val q = ProfileBuilder
        .withGlobalCode(SampleCode("AR-C-SHDG-0002"))
        .withGenotypification(qGeno)
        .build()

      val mismatches = 1
      val matchingRule = MatchingRule(
        4,
        AlphanumericId("IR"),
        Stringency.HighStringency,
        failOnMatch = true,
        forwardToUpper = true,
        Algorithm.ENFSI,
        11,
        mismatches,
        considerForN = true
      )
      val result = profileMtMatch(
        config,
        p,
        q,
        matchingRule,
        mtRcrs,
        None,
        1000
      )
      result.isDefined mustBe false
    }

    "merge ranges and match" in {
      val pGeno = Map(
        4 -> Map(
          "HV2_RANGE" -> List(Allele(100), Allele(200)),
          "HV2" -> List(Mitocondrial('C', 120))
        )
      )
      val p = ProfileBuilder
        .withGlobalCode(SampleCode("AR-C-SHDG-0001"))
        .withGenotypification(pGeno)
        .build()
      val qGeno = Map(
        4 -> Map(
          "HV2_RANGE" -> List(Allele(200), Allele(300)),
          "HV2" -> List(Mitocondrial('T', 251.1))
        )
      )
      val q = ProfileBuilder
        .withGlobalCode(SampleCode("AR-C-SHDG-0002"))
        .withGenotypification(qGeno)
        .build()
      val matchingRule = MatchingRule(
        4,
        AlphanumericId("IR"),
        Stringency.HighStringency,
        failOnMatch = true,
        forwardToUpper = true,
        Algorithm.ENFSI,
        11,
        0,
        considerForN = true
      )
      val result = profileMtMatch(
        config,
        p,
        q,
        matchingRule,
        mtRcrs,
        None,
        1000
      )
      result.isDefined mustBe true
      result.get.result.stringency mustBe HighStringency
      val expectedMatchingAlleles = Map(
        "C@120" -> HighStringency,
        "T@251,1" -> HighStringency
      )
      result.get.result.matchingAlleles mustBe expectedMatchingAlleles
    }

    "merge ranges and not match" in {
      val pGeno = Map(
        4 -> Map(
          "HV2_RANGE" -> List(Allele(100), Allele(200)),
          "HV2" -> List(Mitocondrial('C', 120), Mitocondrial('A', 160))
        )
      )
      val p = ProfileBuilder
        .withGlobalCode(SampleCode("AR-C-SHDG-0001"))
        .withGenotypification(pGeno)
        .build()
      val qGeno = Map(
        4 -> Map(
          "HV2_RANGE" -> List(Allele(150), Allele(250)),
          "HV2" -> List(Mitocondrial('T', 220.1), Mitocondrial('C', 160))
        )
      )
      val q = ProfileBuilder
        .withGlobalCode(SampleCode("AR-C-SHDG-0002"))
        .withGenotypification(qGeno)
        .build()
      val matchingRule = MatchingRule(
        4,
        AlphanumericId("IR"),
        Stringency.HighStringency,
        failOnMatch = true,
        forwardToUpper = true,
        Algorithm.ENFSI,
        11,
        0,
        considerForN = true
      )
      val result = profileMtMatch(config,p, q, matchingRule, mtRcrs, None, 1000)
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
      val p  = ProfileBuilder
        .withGlobalCode(SampleCode("AR-C-SHDG-0001"))
        .withGenotypification(Map.empty)
        .build()
      val qGeno = Map(
        4 -> Map(
          "HV2_RANGE" -> List(Allele(100), Allele(200)),
          "HV2" -> List(Mitocondrial('A', 100)),
          "HV3_RANGE" -> List(Allele(400), Allele(576)),
          "HV3" -> List(
            Mitocondrial('A', 450),
            Mitocondrial('Y', 500),
            Mitocondrial('C', 401.1),
            Mitocondrial('-', 569)
          )
        )
      )
      val q = ProfileBuilder
        .withGlobalCode(SampleCode("AR-C-SHDG-0002"))
        .withGenotypification(qGeno)
        .build()
      val matchingRule = MatchingRule(
        4,
        AlphanumericId("IR"),
        Stringency.HighStringency,
        failOnMatch = true,
        forwardToUpper = true,
        Algorithm.ENFSI,
        11,
        0,
        considerForN = true
      )
      val result = profileMtMatch(
        config,
        p,
        q,
        matchingRule,
        mtRcrs,
        None,
        1000
      )
      result.isDefined mustBe false
    }

    "not match if the only mergeable range has mismatch" in {
      val pGeno = Map(
        4 -> Map(
          "HV2_RANGE" -> List(Allele(50), Allele(70)),
          "HV2" -> List(Mitocondrial('C', 65))
        )
      )
      val p = ProfileBuilder
        .withGlobalCode(SampleCode("AR-C-SHDG-0001"))
        .withGenotypification(pGeno)
        .build()
      val qGeno = Map(
        4 -> Map(
          "HV2_RANGE" -> List(Allele(50), Allele(70)),
          "HV2" -> List(
            Mitocondrial('C', 65),
            Mitocondrial('C', 32.1),
            Mitocondrial('-', 68)
          ),
          "HV1_RANGE" -> List(Allele(120), Allele(150)),
          "HV1" -> List(Mitocondrial('Y', 135))
        )
      )
      val q = ProfileBuilder
        .withGlobalCode(SampleCode("AR-C-SHDG-0002"))
        .withGenotypification(qGeno)
        .build()
      val matchingRule = MatchingRule(
        4,
        AlphanumericId("IR"),
        Stringency.HighStringency,
        failOnMatch = true,
        forwardToUpper = true,
        Algorithm.ENFSI,
        11,
        0,
        considerForN = true
      )
      val result = profileMtMatch(
        config,
        p,
        q,
        matchingRule,
        mtRcrs,
        None,
        1000
      )
      result.isDefined mustBe false
    }
  }

  "merge ranges" must {
    "merge (a,b) and (c,d) when a<c and b<d" in {
      val range1:(Int, Int) = (1, 5)
      val range2:(Int, Int) = (2, 8)
      mergeRanges(range1, range2) mustBe Option((2, 5))
    }
    "merge (a,b) and (c,d) when a==c and b<d" in {
      val range1:(Int, Int) = (1, 5)
      val range2:(Int, Int) = (1, 10)
      mergeRanges(range1, range2) mustBe Option((1, 5))
    }
    "merge (a,b) and (c,d) when a<c and b==d" in {
      val range1:(Int, Int) = (1, 10)
      val range2:(Int, Int) = (5, 10)
      mergeRanges(range1, range2) mustBe Option((5, 10))
    }
    "merge (a,b) and (c,d) when a>c and b>d" in {
      val range1:(Int, Int) = (2, 8)
      val range2:(Int, Int) = (1, 5)
      mergeRanges(range1, range2) mustBe Option((2, 5))
    }
    "merge (a,b) and (c,d) when a==c and b>d" in {
      val range1:(Int, Int) = (1, 8)
      val range2:(Int, Int) = (1, 5)
      mergeRanges(range1, range2) mustBe Option((1, 5))
    }
    "merge (a,b) and (c,d) when a>c and b==d" in {
      val range1:(Int, Int) = (5,10)
      val range2:(Int, Int) = (1,10)
      mergeRanges(range1, range2) mustBe Option((5, 10))
    }
    "merge (a,b) and (c,d) when a<c and b>d" in {
      val range1:(Int, Int) = (1,5)
      val range2:(Int, Int) = (3,4)
      mergeRanges(range1, range2) mustBe Option((3, 4))
    }
    "merge (a,b) and (c,d) when a>c and b<d" in {
      val range1:(Int, Int) = (3,4)
      val range2:(Int, Int) = (1,5)
      mergeRanges(range1, range2) mustBe Option((3, 4))
    }
    "merge (a,b) and (c,d) when b==c" in {
      val range1:(Int, Int) = (1,5)
      val range2:(Int, Int) = (5,10)
      mergeRanges(range1, range2) mustBe Option((5, 5))
    }
    "merge (a,b) and (c,d) when a==d" in {
      val range1:(Int, Int) = (5,10)
      val range2:(Int, Int) = (1,5)
      mergeRanges(range1, range2) mustBe Option((5, 5))
    }
    "merge (a,b) and (c,d) when c>b and d>b" in {
      val range1:(Int, Int) = (5,10)
      val range2:(Int, Int) = (1,3)
      mergeRanges(range1, range2) mustBe None
    }
    "merge (a,b) and (c,d) when a>d and b>d" in {
      val range1:(Int, Int) = (1,3)
      val range2:(Int, Int) = (5,10)
      mergeRanges(range1, range2) mustBe None
    }
  }
  
  "getSequencesToCompare" must {
    "Keep mutations that fall in the both ranges" in {
      val p:Profile.Genotypification = Map(
        "HV1_RANGE" -> List(
          Allele(16024), Allele(16569)
        ),
        "HV1" -> List(
          Mitocondrial('A', 16500)
        )
      )
      val q: Profile.Genotypification = Map(
        "HV1_RANGE" -> List(
          Allele(16424), Allele(16509)
        ),
        "HV1" -> List(
          Mitocondrial('A', 16500)
        )
      )
      val result = getSequencesToCompare(p, q)
      result.length mustBe 1
    }
    "Remove mutations if the mutated position is not in both ranges" in {
      val p:Profile.Genotypification = Map(
        "HV1_RANGE" -> List(
          Allele(16024), Allele(16569)
        ),
        "HV1" -> List(
          Mitocondrial('A', 16500),
          Mitocondrial('G', 16501)
        )
      )
      val q: Profile.Genotypification = Map(
        "HV1_RANGE" -> List(
          Allele(16424), Allele(16500)
        ),
        "HV1" -> List(
          Mitocondrial('T', 16500)
        )
      )
      val result = getSequencesToCompare(p, q)
      result.length mustBe 1
    }
    "Keep no mutations if one range is empty" in {
      val p: Profile.Genotypification = Map(
        "HV1_RANGE" -> List(
          Allele(16024), Allele(16569)
        ),
        "HV1" -> List(
          Mitocondrial('A', 16500)
        )
      )
      val q: Profile.Genotypification = Map()
      val result = getSequencesToCompare(p, q)
      result.length mustBe 0
    }
    """Keep mutations of one range if the other range is defined and
        has not mutations""" in {
      val p: Profile.Genotypification = Map(
        "HV1_RANGE" -> List(
          Allele(16024), Allele(16569)
        ),
        "HV1" -> List(
          Mitocondrial('A', 16500)
        )
      )
      val q: Profile.Genotypification = Map(
        "HV1_RANGE" -> List(
          Allele(16424), Allele(16509)
        ),
        "HV1" -> List()
      )
      val result = getSequencesToCompare(p, q)
      result.length mustBe 1
    }
  }
}