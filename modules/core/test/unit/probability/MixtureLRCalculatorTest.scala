package unit.probability

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import probability.{GroupSpec, MixParticipantLocus, MixtureLRCalculator, ObservedLoci}
import probability.MixtureLRCalculator.*
import profile.Allele

class MixtureLRCalculatorTest extends AnyWordSpec with Matchers {

  val mixValues: List[Allele] = List(Allele(1), Allele(2), Allele(3))
  val victim: List[Allele]    = List(Allele(1), Allele(3))
  val suspect: List[Allele]   = List(Allele(1), Allele(2))

  val frecuenciesCA = Map(
    ("D2S44", BigDecimal(1)) -> 0.0859,
    ("D2S44", BigDecimal(2)) -> 0.0827,
    ("D2S44", BigDecimal(3)) -> 0.1073
  )
  val frecuenciesAA = Map(
    ("D2S44", BigDecimal(1)) -> 0.0316,
    ("D2S44", BigDecimal(2)) -> 0.0842,
    ("D2S44", BigDecimal(3)) -> 0.0926
  )
  val frecuenciesCH = Map(
    ("D2S44", BigDecimal(1)) -> 0.0169,
    ("D2S44", BigDecimal(2)) -> 0.0749,
    ("D2S44", BigDecimal(3)) -> 0.1522
  )

  val groupSpecs: List[GroupSpec] = List(
    GroupSpec("CA", 0.0, frecuenciesCA),
    GroupSpec("AA", 0.0, frecuenciesAA),
    GroupSpec("CH", 0.0, frecuenciesCH)
  )

  val observed: List[ObservedLoci] = List(
    ObservedLoci(suspect, "AA"),
    ObservedLoci(victim, "CA")
  )

  // ── unexplained ──────────────────────────────────────────────────

  "MixtureLRCalculator.unexplained" must {

    "return unaccounted alleles not covered by any participant" in {
      val unk1 = unexplained(List(Allele(10), Allele(11), Allele(12)), List(List(Allele(10)), List(Allele(11))))
      unk1 mustBe Set(Allele(12))
    }

    "return all alleles when no participants provided" in {
      val unk2 = unexplained(List(Allele(10), Allele(11), Allele(12)), List())
      unk2 mustBe Set(Allele(10), Allele(11), Allele(12))
    }

    "return empty set when all alleles are explained" in {
      val unk3 = unexplained(
        List(Allele(10), Allele(11), Allele(12)),
        List(List(Allele(12)), List(Allele(11), Allele(10)))
      )
      unk3 mustBe Set()
    }
  }

  // ── pmixGivenForLocus (θ = 0) ────────────────────────────────────

  "MixtureLRCalculator.pmixGivenForLocus" must {

    case class Scenario(theta: Double, victimGroup: String, suspectGroup: String, expectedLR: Double)

    val scenarios = List(
      Scenario(0.0, "CA", "CA", 396),
      Scenario(0.0, "AA", "AA", 1623),
      Scenario(0.0, "AA", "CA", 727),
      Scenario(0.0, "CH", "CH", 1773),
      Scenario(0.0, "AA", "CH", 1519),
      Scenario(0.0, "CA", "CH", 599)
    )

    scenarios.foreach { scenario =>
      s"return ~${scenario.expectedLR} for (victim=${scenario.victimGroup}, suspect=${scenario.suspectGroup})" in {
        val gs = List(
          GroupSpec("CA", scenario.theta, frecuenciesCA),
          GroupSpec("AA", scenario.theta, frecuenciesAA),
          GroupSpec("CH", scenario.theta, frecuenciesCH)
        )
        val specs = List(
          MixParticipantLocus(None, scenario.suspectGroup),
          MixParticipantLocus(None, scenario.victimGroup)
        )
        val obs = List(
          ObservedLoci(suspect, scenario.suspectGroup),
          ObservedLoci(victim, scenario.victimGroup)
        )

        val d = pmixGivenForLocus("D2S44", gs, mixValues, specs, obs)
        (d.get - scenario.expectedLR) must be < 1.0
      }
    }
  }

  // ── pmixGivenForLocus with varying n unknowns ─────────────────────

  "MixtureLRCalculator.pmixGivenForLocus with variable unknowns" must {

    case class Scenario(theta: Double, victimGroup: String, suspectGroup: String, expectedLR: Double, n: Int, m: Int)

    val scenarios = List(
      Scenario(0.0,  "AA", "AA", 1623, 2, 0),
      Scenario(0.0,  "AA", "AA",   70, 2, 1),
      Scenario(0.01, "AA", "AA",  739, 2, 0),
      Scenario(0.01, "AA", "AA",   44, 2, 1),
      Scenario(0.03, "AA", "AA",  276, 2, 0),
      Scenario(0.03, "AA", "AA",   26, 2, 1),
      Scenario(0.0,  "AA", "AA", 21606, 3, 0),
      Scenario(0.0,  "AA", "AA",  938, 3, 1),
      Scenario(0.01, "AA", "AA", 5853, 3, 0),
      Scenario(0.03, "AA", "AA", 1150, 3, 0)
    )

    scenarios.foreach { scenario =>
      s"return ~${scenario.expectedLR} for AA/AA θ=${scenario.theta} n=${scenario.n} m=${scenario.m}" in {
        val gs = List(
          GroupSpec("CA", scenario.theta, frecuenciesCA),
          GroupSpec("AA", scenario.theta, frecuenciesAA),
          GroupSpec("CH", scenario.theta, frecuenciesCH)
        )
        val unknownsForProsecutor = (1 to scenario.m).toList.map(_ => MixParticipantLocus(None, scenario.victimGroup))
        val specsHp = List(
          MixParticipantLocus(Some(suspect), scenario.suspectGroup),
          MixParticipantLocus(Some(victim), scenario.victimGroup)
        ) ++ unknownsForProsecutor

        val unknownsForDefense = (1 to (scenario.n - 2)).toList.map(_ => MixParticipantLocus(None, scenario.victimGroup))
        val specsHd = List(
          MixParticipantLocus(None, scenario.suspectGroup),
          MixParticipantLocus(None, scenario.victimGroup)
        ) ++ unknownsForDefense

        val obs = List(ObservedLoci(suspect, scenario.suspectGroup), ObservedLoci(victim, scenario.victimGroup))

        val hp = pmixGivenForLocus("D2S44", gs, mixValues, specsHp, obs)
        val hd = pmixGivenForLocus("D2S44", gs, mixValues, specsHd, obs)

        val lr = for { h1 <- hp; h2 <- hd } yield h1 / h2
        lr.get mustBe scenario.expectedLR +- 0.5
      }
    }
  }
}
