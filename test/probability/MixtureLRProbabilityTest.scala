package probability

import scala.math.BigDecimal.int2bigDecimal
import org.specs2.mutable.Specification
import org.specs2.matcher.Matcher
import org.specs2.specification.Fragment
import profile._
import probability.MixtureLRCalculator._
import org.specs2.matcher.Expectable
import stats.PopulationSampleFrequency
import types.SampleCode

class MixtureLRProbabilityTest extends Specification {

  "unexplained" should {
    "should calculate unexplained" in {

      val unk1 = unexplained(List(Allele(10), Allele(11), Allele(12)), List(List(Allele(10)), List(Allele(11))))
      unk1 must beEqualTo(Set(Allele(12)))

      val unk2 = unexplained(List(Allele(10), Allele(11), Allele(12)), List())
      unk2 must beEqualTo(Set(Allele(10), Allele(11), Allele(12)))

      val unk3 = unexplained(List(Allele(10), Allele(11), Allele(12)), List(List(Allele(12)), List(Allele(11), Allele(10))))
      unk3 must beEqualTo(Set())

    }
  }

  "pmixGivenForLocus should" >> {

    val mixValues = List(Allele(1), Allele(2), Allele(3))

    val victim = List(Allele(1), Allele(3))

    val suspect = List(Allele(1), Allele(2))

 
    
    val frecuenciesCA = Map(
      ("D2S44", BigDecimal(1)) -> 0.0859,
      ("D2S44", BigDecimal(2)) -> 0.0827,
      ("D2S44", BigDecimal(3)) -> 0.1073)
    val frecuenciesAA = Map(
      ("D2S44", BigDecimal(1)) -> 0.0316,
      ("D2S44", BigDecimal(2)) -> 0.0842,
      ("D2S44", BigDecimal(3)) -> 0.0926)
    val frecuenciesCH = Map(
      ("D2S44", BigDecimal(1)) -> 0.0169,
      ("D2S44", BigDecimal(2)) -> 0.0749,
      ("D2S44", BigDecimal(3)) -> 0.1522)

    sealed case class Scenario(theta: Double, victimGroup: String, suspectGroup: String, expectedLR: Double)

    val scenarios = List(
      Scenario(0.0, "CA", "CA", 396),
      Scenario(0.0, "AA", "AA", 1623),
      Scenario(0.0, "AA", "CA", 727),
      Scenario(0.0, "CH", "CH", 1773),
      Scenario(0.0, "AA", "CH", 1519),
      Scenario(0.0, "CA", "CH", 599))

    scenarios.foreach { scenario =>

      "return " + scenario.expectedLR + " for (" + scenario.victimGroup + ", " + scenario.suspectGroup + ") with theta = " + scenario.theta in {
        val groupSpecs = List(
          GroupSpec("CA", scenario.theta, frecuenciesCA),
          GroupSpec("AA", scenario.theta, frecuenciesAA),
          GroupSpec("CH", scenario.theta, frecuenciesCH))

        val specs = List(
          MixParticipantLocus(None, scenario.suspectGroup),
          MixParticipantLocus(None, scenario.victimGroup))

             
        val observed = List(ObservedLoci(suspect, scenario.suspectGroup),
          ObservedLoci(victim, scenario.victimGroup))
          
        val d = pmixGivenForLocus("D2S44", groupSpecs, mixValues, specs, observed)

        (d.get - scenario.expectedLR) must beLessThan(1.0)

      }

    }; end

  }

  "pmixGivenForLocus with theta > 0 should" >> {

    val mixValues = List(Allele(1), Allele(2), Allele(3))

    val victim = List(Allele(1), Allele(3))

    val suspect = List(Allele(1), Allele(2))

    val frecuenciesCA = Map(
      ("D2S44", BigDecimal(1)) -> 0.0859,
      ("D2S44", BigDecimal(2)) -> 0.0827,
      ("D2S44", BigDecimal(3)) -> 0.1073)
    val frecuenciesAA = Map(
      ("D2S44", BigDecimal(1)) -> 0.0316,
      ("D2S44", BigDecimal(2)) -> 0.0842,
      ("D2S44", BigDecimal(3)) -> 0.0926)
    val frecuenciesCH = Map(
      ("D2S44", BigDecimal(1)) -> 0.0169,
      ("D2S44", BigDecimal(2)) -> 0.0749,
      ("D2S44", BigDecimal(3)) -> 0.1522)

    sealed case class Scenario(theta: Double, victimGroup: String, suspectGroup: String, expectedLR: Double, n: Int, m: Int)

    val scenarios = List(
      Scenario(0.0, "AA", "AA", 1623, 2, 0),
      Scenario(0.0, "AA", "AA", 70, 2, 1),
      Scenario(0.0, "AA", "AA", 3.06, 2, 2),

      Scenario(0.01, "AA", "AA", 739, 2, 0),
      Scenario(0.01, "AA", "AA", 44, 2, 1),
      Scenario(0.01, "AA", "AA", 2.88, 2, 2),

      Scenario(0.03, "AA", "AA", 276, 2, 0),
      Scenario(0.03, "AA", "AA", 26, 2, 1),
      Scenario(0.03, "AA", "AA", 2.98, 2, 2),

      Scenario(0.0, "AA", "AA", 21606, 3, 0),
      Scenario(0.0, "AA", "AA", 938, 3, 1),
      Scenario(0.0, "AA", "AA", 41, 3, 2),

      Scenario(0.01, "AA", "AA", 5853, 3, 0),
      Scenario(0.01, "AA", "AA", 345, 3, 1),
      Scenario(0.01, "AA", "AA", 23, 3, 2),

      Scenario(0.03, "AA", "AA", 1150, 3, 0),
      Scenario(0.03, "AA", "AA", 107, 3, 1),
      Scenario(0.03, "AA", "AA", 12, 3, 2),

      Scenario(0.0, "AA", "AA", 396495, 4, 0),
      Scenario(0.0, "AA", "AA", 17220, 4, 1),
      Scenario(0.0, "AA", "AA", 748, 4, 2),

      Scenario(0.01, "AA", "AA", 58264, 4, 0),
      Scenario(0.01, "AA", "AA", 3434, 4, 1),
      Scenario(0.01, "AA", "AA", 227, 4, 2),

      Scenario(0.03, "AA", "AA", 5682, 4, 0),
      Scenario(0.03, "AA", "AA", 528, 4, 1),
      Scenario(0.03, "AA", "AA", 61, 4, 2))

    scenarios.foreach { scenario =>

      "return " + scenario.expectedLR + " for (" + scenario.victimGroup + ", " + scenario.suspectGroup + ") with theta = " + scenario.theta + ", n=" + scenario.n + ",m =" + scenario.m in {
        val groupSpecs = List(
          GroupSpec("CA", scenario.theta, frecuenciesCA),
          GroupSpec("AA", scenario.theta, frecuenciesAA),
          GroupSpec("CH", scenario.theta, frecuenciesCH))

        val unknownsForProsecutor = ((1 to scenario.m).map { i => MixParticipantLocus(None, scenario.victimGroup) })

        val specsHp = List(MixParticipantLocus(Some(suspect), scenario.suspectGroup),
          MixParticipantLocus(Some(victim), scenario.victimGroup)) ++ unknownsForProsecutor

        val unknownsForDefense = ((1 to scenario.n - 2).map { i => MixParticipantLocus(None, scenario.victimGroup) })

//        val specsHd = List(MixParticipantLocus(Some(suspect), scenario.suspectGroup, false),
//          MixParticipantLocus(Some(victim), scenario.victimGroup, false)) ++ unknownsForDefense
        val specsHd = List(MixParticipantLocus(None, scenario.suspectGroup),
          MixParticipantLocus(None, scenario.victimGroup)) ++ unknownsForDefense

        val observed = List(ObservedLoci(suspect, scenario.suspectGroup),
          ObservedLoci(victim, scenario.victimGroup))
          
        val hp = pmixGivenForLocus("D2S44", groupSpecs, mixValues, specsHp, observed)
        val hd = pmixGivenForLocus("D2S44", groupSpecs, mixValues, specsHd, observed)

        val lr = for {
          h1 <- hp
          h2 <- hd
        } yield (h1 / h2)

        lr.get must beCloseTo(scenario.expectedLR, 0.5)

      }

    }; end

  }

  "pmixGivenForLocus for Simpson case should" >> {

    val mixValues = List(Allele(1), Allele(2), Allele(3))

    val victim = List(Allele(1), Allele(3))

    val suspect = List(Allele(1), Allele(2))

    val frecuenciesCA = Map(
      ("D2S44", BigDecimal(1)) -> 0.0859,
      ("D2S44", BigDecimal(2)) -> 0.0827,
      ("D2S44", BigDecimal(3)) -> 0.1073)
    val frecuenciesAA = Map(
      ("D2S44", BigDecimal(1)) -> 0.0316,
      ("D2S44", BigDecimal(2)) -> 0.0842,
      ("D2S44", BigDecimal(3)) -> 0.0926)
    val frecuenciesCH = Map(
      ("D2S44", BigDecimal(1)) -> 0.0169,
      ("D2S44", BigDecimal(2)) -> 0.0749,
      ("D2S44", BigDecimal(3)) -> 0.1522)

    sealed case class Scenario(theta: Double, expectedLR: Double, hp: List[MixParticipantLocus], hd: List[MixParticipantLocus])

    val unknownAA = MixParticipantLocus(None, "AA")
    val unknownCA = MixParticipantLocus(None, "CA")
    val unknownCH = MixParticipantLocus(None, "CH")

    val unKnownVictim = MixParticipantLocus(None, "CA")
    val unKnownSuspect = MixParticipantLocus(None, "AA")

    val knownVictim = MixParticipantLocus(Some(victim), "CA")
    val knownSuspect = MixParticipantLocus(Some(suspect), "AA")

    val observed = List(ObservedLoci(suspect, "AA"), ObservedLoci(victim, "CA"))
    
    val scenarios = List(
      //Scenario 1
      Scenario(0.0, 1623, List(knownVictim, knownSuspect), List(unKnownSuspect, unknownAA)),
      Scenario(0.03, 518, List(knownVictim, knownSuspect), List(unKnownSuspect, unknownAA)),
      Scenario(0.0, 396, List(knownVictim, knownSuspect), List(unknownCA, unKnownVictim)),
      Scenario(0.03, 218, List(knownVictim, knownSuspect), List(unknownCA, unKnownVictim)),
      Scenario(0.0, 1773, List(knownVictim, knownSuspect), List(unknownCH, unknownCH)),
      Scenario(0.03, 1536, List(knownVictim, knownSuspect), List(unknownCH, unknownCH)),
      Scenario(0.0, 727, List(knownVictim, knownSuspect), List(unKnownSuspect, unKnownVictim)),
      Scenario(0.03, 329, List(knownVictim, knownSuspect), List(unKnownSuspect, unKnownVictim)),
      Scenario(0.0, 1519, List(knownVictim, knownSuspect), List(unKnownSuspect, unknownCH)),
      Scenario(0.03, 739, List(knownVictim, knownSuspect), List(unKnownSuspect, unknownCH)),
      Scenario(0.0, 599, List(knownVictim, knownSuspect), List(unKnownVictim, unknownCH)),
      Scenario(0.03, 420, List(knownVictim, knownSuspect), List(unKnownVictim, unknownCH)),

      Scenario(0.0, 21606, List(knownVictim, knownSuspect), List(unKnownSuspect, unknownAA, unknownAA)),
      Scenario(0.03, 2561, List(knownVictim, knownSuspect), List(unKnownSuspect, unknownAA, unknownAA)),
      Scenario(0.0, 3112, List(knownVictim, knownSuspect), List(unknownCA, unknownCA, unKnownVictim)),
      Scenario(0.03, 799, List(knownVictim, knownSuspect), List(unknownCA, unknownCA, unKnownVictim)),
      Scenario(0.0, 16007, List(knownVictim, knownSuspect), List(unknownCH, unknownCH, unknownCH)),
      Scenario(0.03, 7432, List(knownVictim, knownSuspect), List(unknownCH, unknownCH, unknownCH)),

      // scenario 2
      Scenario(0.0, 70, List(knownVictim, knownSuspect, unknownAA), List(unKnownSuspect, unknownAA)),
      Scenario(0.03, 36, List(knownVictim, knownSuspect, unknownAA), List(unKnownSuspect, unknownAA)),
      Scenario(0.0, 17, List(knownVictim, knownSuspect, unknownAA), List(unknownCA, unKnownVictim)),
      Scenario(0.03, 15, List(knownVictim, knownSuspect, unknownAA), List(unknownCA, unKnownVictim)),
      Scenario(0.0, 77, List(knownVictim, knownSuspect, unknownAA), List(unknownCH, unknownCH)),
      Scenario(0.03, 108, List(knownVictim, knownSuspect, unknownAA), List(unknownCH, unknownCH)),
      Scenario(0.0, 32, List(knownVictim, knownSuspect, unknownAA), List(unKnownSuspect, unKnownVictim)),
      Scenario(0.03, 23, List(knownVictim, knownSuspect, unknownAA), List(unKnownSuspect, unKnownVictim)),
      Scenario(0.0, 66, List(knownVictim, knownSuspect, unknownAA), List(unKnownSuspect, unknownCH)),
      Scenario(0.03, 52, List(knownVictim, knownSuspect, unknownAA), List(unKnownSuspect, unknownCH)),
      Scenario(0.0, 26, List(knownVictim, knownSuspect, unknownAA), List(unKnownVictim, unknownCH)),
      Scenario(0.03, 29, List(knownVictim, knownSuspect, unknownAA), List(unKnownVictim, unknownCH)),

      Scenario(0.0, 938, List(knownVictim, knownSuspect, unknownAA), List(unKnownSuspect, unknownAA, unknownAA)),
      Scenario(0.03, 180, List(knownVictim, knownSuspect, unknownAA), List(unKnownSuspect, unknownAA, unknownAA)),
      Scenario(0.0, 135, List(knownVictim, knownSuspect, unknownAA), List(unknownCA, unknownCA, unKnownVictim)),
      Scenario(0.03, 56, List(knownVictim, knownSuspect, unknownAA), List(unknownCA, unknownCA, unKnownVictim)),
      Scenario(0.0, 695, List(knownVictim, knownSuspect, unknownAA), List(unknownCH, unknownCH, unknownCH)),
      Scenario(0.03, 521, List(knownVictim, knownSuspect, unknownAA), List(unknownCH, unknownCH, unknownCH)),

      // scenario 3
      Scenario(0.0, 124, List(knownVictim, knownSuspect, unknownCA), List(unKnownSuspect, unknownAA)),
      Scenario(0.03, 56, List(knownVictim, knownSuspect, unknownCA), List(unKnownSuspect, unknownAA)),
      Scenario(0.0, 30, List(knownVictim, knownSuspect, unknownCA), List(unknownCA, unKnownVictim)),
      Scenario(0.03, 23, List(knownVictim, knownSuspect, unknownCA), List(unknownCA, unKnownVictim)),
      Scenario(0.0, 135, List(knownVictim, knownSuspect, unknownCA), List(unknownCH, unknownCH)),
      Scenario(0.03, 165, List(knownVictim, knownSuspect, unknownCA), List(unknownCH, unknownCH)),
      Scenario(0.0, 55, List(knownVictim, knownSuspect, unknownCA), List(unKnownSuspect, unKnownVictim)),
      Scenario(0.03, 35, List(knownVictim, knownSuspect, unknownCA), List(unKnownSuspect, unKnownVictim)),
      Scenario(0.0, 116, List(knownVictim, knownSuspect, unknownCA), List(unKnownSuspect, unknownCH)),
      Scenario(0.03, 79, List(knownVictim, knownSuspect, unknownCA), List(unKnownSuspect, unknownCH)),
      Scenario(0.0, 46, List(knownVictim, knownSuspect, unknownCA), List(unKnownVictim, unknownCH)),
      Scenario(0.03, 45, List(knownVictim, knownSuspect, unknownCA), List(unKnownVictim, unknownCH)),

      Scenario(0.0, 1645, List(knownVictim, knownSuspect, unknownCA), List(unKnownSuspect, unknownAA, unknownAA)),
      Scenario(0.03, 275, List(knownVictim, knownSuspect, unknownCA), List(unKnownSuspect, unknownAA, unknownAA)),
      Scenario(0.0, 237, List(knownVictim, knownSuspect, unknownCA), List(unknownCA, unknownCA, unKnownVictim)),
      Scenario(0.03, 86, List(knownVictim, knownSuspect, unknownCA), List(unknownCA, unknownCA, unKnownVictim)),
      Scenario(0.0, 1218, List(knownVictim, knownSuspect, unknownCA), List(unknownCH, unknownCH, unknownCH)),
      Scenario(0.03, 798, List(knownVictim, knownSuspect, unknownCA), List(unknownCH, unknownCH, unknownCH)))

    scenarios.foreach { scenario =>

      val hd_unknowns = scenario.hd.filter { p => p.mayBeAlleles.isEmpty }.map { p => p.groupLabel }.sorted

      val hp_unknowns = scenario.hp.filter { p => p.mayBeAlleles.isEmpty }.map { p => p.groupLabel }.sorted

      "return " + scenario.expectedLR + " for (hd=" + hd_unknowns + ", hp=victim, suspect," + hp_unknowns + ") with theta = " + scenario.theta in {
        val groupSpecs = List(
          GroupSpec("CA", scenario.theta, frecuenciesCA),
          GroupSpec("AA", scenario.theta, frecuenciesAA),
          GroupSpec("CH", scenario.theta, frecuenciesCH))

          
        val hp = pmixGivenForLocus("D2S44", groupSpecs, mixValues, scenario.hp, observed)
        val hd = pmixGivenForLocus("D2S44", groupSpecs, mixValues, scenario.hd, observed)


        
        val lr = for {
          h1 <- hp
          h2 <- hd
        } yield (h1 / h2)

        lr.get must beCloseTo(scenario.expectedLR, 0.5)

      }

    }; end

  }

  "prosecutor's hypothesis for a mixture matching against one profile one unknown should" >> {

    val mixValues = List(Allele(14), Allele(8), Allele(12))
    val suspect = List(Allele(8), Allele(12))

    val frecuenciesHW = Map(
      ("D2S44", BigDecimal(14)) -> 0.20496,
      ("D2S44", BigDecimal(8)) -> 0.00038,
      ("D2S44", BigDecimal(12)) -> 0.12708)

    sealed case class Scenario(theta: Double, victimGroup: String, suspectGroup: String, expectedP: Double)

    val scenarios = List(
      Scenario(0.0, "HW", "HW", 0.09425))

    scenarios.foreach { scenario =>

      "return " + scenario.expectedP + " for (" + scenario.victimGroup + ", " + scenario.suspectGroup + ") with theta = " + scenario.theta in {
        val groupSpecs = List(
          GroupSpec("HW", scenario.theta, frecuenciesHW))

        val specs = List(
          MixParticipantLocus(Some(suspect), scenario.suspectGroup),
          MixParticipantLocus(None, scenario.suspectGroup))

            val observed = List(ObservedLoci(suspect,  scenario.suspectGroup))
    
          
        val d = pmixGivenForLocus("D2S44", groupSpecs, mixValues, specs, observed)

        d.get must beCloseTo(scenario.expectedP, 0.001)

      }

    }; end

  }

  "defender's hypothesis for a mixture matching against one profile one unknown should" >> {

    val mixValues = List(Allele(14), Allele(9), Allele(12))
    val suspect = List(Allele(9), Allele(12))

    val frecuenciesHW = Map(
      ("D2S44", BigDecimal(14)) -> 0.20496,
      ("D2S44", BigDecimal(9)) -> 0.00038,
      ("D2S44", BigDecimal(12)) -> 0.12708)

    sealed case class Scenario(theta: Double, victimGroup: String, suspectGroup: String, expectedP: Double)

    val scenarios = List(
      Scenario(0.0, "HW", "HW", 8.3238E-6))

    scenarios.foreach { scenario =>

      "return " + scenario.expectedP + " for (" + scenario.victimGroup + ", " + scenario.suspectGroup + ") with theta = " + scenario.theta in {
        val groupSpecs = List(
          GroupSpec("HW", scenario.theta, frecuenciesHW))

        val specs = List(
          MixParticipantLocus(None, scenario.suspectGroup),
          MixParticipantLocus(None, scenario.suspectGroup),
          MixParticipantLocus(None, scenario.suspectGroup))

          
            val observed = List(ObservedLoci(suspect,  scenario.suspectGroup))
    
          
        val d = pmixGivenForLocus("D2S44", groupSpecs, mixValues, specs, observed)

        d.get must beCloseTo(scenario.expectedP, 0.001)

      }

    }; end

  }

  "defender's hypothesis for a mixture 8, 9, 10, 11" >> {

    val mixValues = List(Allele(8), Allele(9), Allele(10), Allele(11))
    val suspect = List(Allele(10), Allele(11))

    val frecuenciesHW = Map(
      ("TPOX", BigDecimal(8)) -> 0.48349,
      ("TPOX", BigDecimal(9)) -> 0.0779,
      ("TPOX", BigDecimal(10)) -> 0.04761,
      ("TPOX", BigDecimal(11)) -> 0.28963)

    sealed case class Scenario(theta: Double, victimGroup: String, suspectGroup: String, expectedP: Double)

    val scenarios = List(Scenario(0.0, "HW", "HW", 0.012))

    scenarios.foreach { scenario =>

      "return " + scenario.expectedP + " for (" + scenario.victimGroup + ", " + scenario.suspectGroup + ") with theta = " + scenario.theta in {
        val groupSpecs = List(
          GroupSpec("HW", scenario.theta, frecuenciesHW))

        val specs = List(
          //          MixParticipantLocus(Some(suspect), scenario.suspectGroup, false),
          MixParticipantLocus(None, scenario.suspectGroup),
          MixParticipantLocus(None, scenario.suspectGroup))

          
            val observed = List(ObservedLoci(suspect,  scenario.suspectGroup))
          
        val d = pmixGivenForLocus("TPOX", groupSpecs, mixValues, specs, observed)

        d.get must beCloseTo(scenario.expectedP, 0.001)

      }

    }; end

  }

  "case where it matters to consider the unknown alleles, i.e., theta > 0" >> {

    val mixValues = List(Allele(8), Allele(9), Allele(10))
    val suspect = List(Allele(9), Allele(10))

    val frecuenciesTable = Map(
      ("M1", BigDecimal(8)) -> 0.48349,
      ("M1", BigDecimal(9)) -> 0.0779,
      ("M1", BigDecimal(10)) -> 0.04761)

    val groupSpecs = List(
      GroupSpec(groupLabel = "X-MEN", theta = 0.01, frecuenciesTable))

    val observed = List(ObservedLoci(suspect, "X-MEN"))

    val specs1 = List(
      MixParticipantLocus(None, "X-MEN"),
      MixParticipantLocus(None, "X-MEN"))

    val specs2 = List(
      MixParticipantLocus(None, "X-MEN"),
      MixParticipantLocus(None, "X-MEN"))

    val p1 = pmixGivenForLocus("M1", groupSpecs, mixValues, specs1, observed)
    val p2 = pmixGivenForLocus("M1", groupSpecs, mixValues, specs2, List())

    p1 must beSome
    p2 must beSome

    p1.get must beCloseTo(0.017, 0.001)
    p2.get must beCloseTo(0.013, 0.001)

  }

  "BUG should" >> {

    val mixValues = List(Allele(16), Allele(18))

    val suspect = List(Allele(16))

    val frecuencies = Map(
      ("D2S44", BigDecimal(16)) -> 0.0859,
      ("D2S44", BigDecimal(18)) -> 0.0827)

    sealed case class Scenario(theta: Double, victimGroup: String, suspectGroup: String, expectedLR: Double)

    val scenarios = List(
      Scenario(0.0, "CA", "CA", 396))

    scenarios.foreach { scenario =>

      "return " + scenario.expectedLR + " for (" + scenario.victimGroup + ", " + scenario.suspectGroup + ") with theta = " + scenario.theta in {
        val groupSpecs = List(GroupSpec("CA", scenario.theta, frecuencies))

        val specs = List(
          MixParticipantLocus(None, scenario.suspectGroup),
          MixParticipantLocus(None, scenario.victimGroup))

          val observed = List(ObservedLoci(suspect, scenario.suspectGroup))
          
        val d = pmixGivenForLocus("D2S44", groupSpecs, mixValues, specs, observed)

        (d.get - scenario.expectedLR) must beLessThan(1.0)

      }

    }; end

  }


  "Mix mix" should {
    "work without profile associated" in {

      val frequencyTable = Map(
        ("DUMMY", BigDecimal(-1)) -> 0.0000001,
        ("DUMMY", BigDecimal(1)) -> 0.03,
        ("DUMMY", BigDecimal(2)) -> 0.1,
        ("DUMMY", BigDecimal(3)) -> 0.3,
        ("DUMMY", BigDecimal(4)) -> 0.23,
        ("DUMMY", BigDecimal(5)) -> 0.12,
        ("DUMMY", BigDecimal(6)) -> 0.22)

      val mix1 = Map("DUMMY" -> List(Allele(1),Allele(2),Allele(3)))
      val mix2 = Map("DUMMY" -> List(Allele(1),Allele(2),Allele(3),Allele(4)))
      //val victim1 = Profile(null, SampleCode("AR-B-IMBICE-400"), null, null, null, Map(), None, None, Some(1), None)
      //val victim2 = Profile(null, SampleCode("AR-B-IMBICE-400"), null, null, null, Map(), None, None, Some(1), None)
      val result = MixtureLRCalculator.lrMixMix(mix1, None, mix2, None, frequencyTable, new NRCII41CalculationProbability(0))

      result("DUMMY").get must beCloseTo(9.65, 0.01)
    }

    "work with profile associated" in {

      val frequencyTable = Map(
        ("DUMMY", BigDecimal(-1)) -> 0.0000001,
        ("DUMMY", BigDecimal(1)) -> 0.03,
        ("DUMMY", BigDecimal(2)) -> 0.1,
        ("DUMMY", BigDecimal(3)) -> 0.3,
        ("DUMMY", BigDecimal(4)) -> 0.23,
        ("DUMMY", BigDecimal(5)) -> 0.12,
        ("DUMMY", BigDecimal(6)) -> 0.22)

      val mix1 = Map("DUMMY" -> List(Allele(1),Allele(2),Allele(3)))
      val mix2 = Map("DUMMY" -> List(Allele(1),Allele(2),Allele(3),Allele(4)))
      val victim1 = Map("DUMMY" -> List(Allele(1),Allele(3)))
      //val victim2 = Profile(null, SampleCode("AR-B-IMBICE-400"), null, null, null, Map(), None, None, Some(1), None)
      val result = MixtureLRCalculator.lrMixMix(mix1, Some(victim1), mix2, None, frequencyTable, new NRCII41CalculationProbability(0))

      result("DUMMY").get must beCloseTo(4.3859, 0.01)
    }

    "fail with drop out" in {

      val frequencyTable = Map(
        ("DUMMY", BigDecimal(-1)) -> 0.0000001,
        ("DUMMY", BigDecimal(1)) -> 0.03,
        ("DUMMY", BigDecimal(2)) -> 0.1,
        ("DUMMY", BigDecimal(3)) -> 0.3,
        ("DUMMY", BigDecimal(4)) -> 0.23,
        ("DUMMY", BigDecimal(5)) -> 0.12,
        ("DUMMY", BigDecimal(6)) -> 0.22)

      val mix1 = Map("DUMMY" -> List(Allele(1),Allele(2),Allele(3)))
      val mix2 = Map("DUMMY" -> List(Allele(1),Allele(2),Allele(3),Allele(4)))
      val victim1 = Map("DUMMY" -> List(Allele(1),Allele(5)))
      //val victim2 = Profile(null, SampleCode("AR-B-IMBICE-400"), null, null, null, Map(), None, None, Some(1), None)
      val result = MixtureLRCalculator.lrMixMix(mix1, Some(victim1), mix2, None, frequencyTable, new NRCII41CalculationProbability(0))

      result("DUMMY") must beNone
    }
  }

}