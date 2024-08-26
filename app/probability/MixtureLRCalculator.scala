package probability

import profile._
import probability.PValueCalculator._
import play.api.libs.json.Json
import profile.Profile.Genotypification
import types.SampleCode

case class GroupSpec(groupLabel: String, theta: Double, frequencyTable: PValueCalculator.FrequencyTable)

case class MixParticipant(maybeProfile: Option[Genotypification], groupLabel: String)
case class MixParticipantLocus(mayBeAlleles: Option[Seq[AlleleValue]], groupLabel: String)

case class ObservedProfile(profile: Genotypification, groupLabel: String)
case class ObservedLoci(alleles: Seq[AlleleValue], groupLabel: String)


case class MixHypothesisParticipant(profile: String, freqDb: String)

object MixHypothesisParticipant {
  implicit val f = Json.format[MixHypothesisParticipant]
}

case class MixtureHypothesis(
  mixProfile: SampleCode,
  prosecutorHypothesis: List[String],
  defenderHypothesis: List[String],
  statsOptions: Map[String, MixHypothesisParticipant])
object MixtureHypothesis {
  implicit val f = Json.format[MixtureHypothesis]
}

object MixtureLRCalculator {

  // con este nombre se configura en el application.conf
  val name = "mixmix"

  def unexplained(mixValues: Seq[AlleleValue], knownLoci: Seq[Seq[AlleleValue]]): Set[AlleleValue] = {
    knownLoci.foldLeft[Set[AlleleValue]](mixValues.toSet) { (prev, current) => prev -- current.toSet }
  }

  def erre(r: Double, m: Int, k: Int, theta: Double): Option[Double] = {
    val result = (0 to m - 1).foldLeft[Double](1.0) { (prod, i) => prod * ((k + i) * theta + (1 - theta) * r) }
    Some(result)
  }

  def w(locus: Profile.Marker, c: Seq[AlleleValue], specs: Seq[MixParticipantLocus], groupSpecs: Seq[GroupSpec],
        observed: Seq[ObservedLoci]) = {

    val groupSpecMap = groupSpecs.map { groupSpec => groupSpec.groupLabel -> groupSpec }.toMap
    val groups = specs.map { x => x.groupLabel }.toSet
    val specsByGroup = specs.groupBy { spec => spec.groupLabel }
    val observedByGroup = observed.groupBy { spec => spec.groupLabel }
    val numberOfUnknowns = specs.count { spec => spec.mayBeAlleles.isEmpty } // x_g
    groups.foldLeft[Option[Double]](Some(1.0)) { (prod, group) =>
      val numberOfUnknowns = specsByGroup.get(group).get.count { spec => spec.mayBeAlleles.isEmpty } // x_g
      val groupSpec = groupSpecMap.get(group).get
      val maybeR = c.foldLeft[Option[Double]](Some(0.0)) { (sum, al) =>
        al match {
          case Allele(value) =>
            for {
              s <- sum
              p <- getProbability(groupSpec.frequencyTable)(locus, value) 
            } yield s + p
          case _ => None
        }
      }
      val availableProfilesForGroup = observedByGroup.getOrElse(group, List()).map { spec => spec.alleles }
      val maybeK = c.foldLeft[Option[Int]](Some(0)) { (sum, al) =>
        al match {
          case Allele(value) => {
            val availableProfilesHavingValue = availableProfilesForGroup.count { listOfAllelesForOneProfile => listOfAllelesForOneProfile.contains(Allele(value)) }
            for {
              s <- sum
            } yield s + availableProfilesHavingValue

          }
          case _ => None
        }
      }
      val theta_g = groupSpecMap.get(group).get.theta
      for {
        p <- prod
        r <- maybeR
        k <- maybeK
        num <- erre(r, 2 * numberOfUnknowns, k, theta_g)
        denom <- erre(1.0, 2 * numberOfUnknowns, availableProfilesForGroup.size * 2, theta_g)
      } yield {
        p * (num / denom)
      }
    }
  }

  def pmixGivenForLocus(locus: Profile.Marker, groupSpecs: Seq[GroupSpec], mixValues: Seq[AlleleValue], specs: Seq[MixParticipantLocus], observed: Seq[ObservedLoci]) = {

    val isInvalid = specs.exists { p => !p.mayBeAlleles.isEmpty && !(p.mayBeAlleles.get diff mixValues).isEmpty }

    if (isInvalid)
      None
    else {

      val knownParticipants = specs.filter { x => x.mayBeAlleles.isDefined }
      val knownLoci = knownParticipants.flatMap { x => x.mayBeAlleles }
      val unknownValues = unexplained(mixValues, knownLoci)

      unknownValues.subsets.map(subset => mixValues.toSet -- subset).foldLeft[Option[Double]](Some(0.0)) { (sum, c) =>
        val m_c = mixValues.toSet -- c
        val sign = Math.pow(-1.0, m_c.size)
        for {
          w <- w(locus, c.toList, specs, groupSpecs, observed)
          s <- sum
        } yield s + sign * w
      }
    }
  }

  def pmixGivenH(mixProfile: Genotypification, groupSpecs: Seq[GroupSpec], participants: Seq[MixParticipant], observed: Seq[ObservedProfile]): Map[Profile.Marker, Double] = {
    val result = mixProfile.map {
      case (locus, mixValues) =>

        val specs = participants.map { mixParticipant =>

          val mayBeAlleles = mixParticipant.maybeProfile match {
            case Some(profile) => {
              profile.get(locus)
            }
            case None => None
          }

          MixParticipantLocus(mayBeAlleles, mixParticipant.groupLabel)
        }

        val specsForObserved = observed.map { mixParticipant =>
          ObservedLoci(mixParticipant.profile.get(locus).get, mixParticipant.groupLabel)
        }

        val thisLocusP = pmixGivenForLocus(locus, groupSpecs, mixValues, specs, specsForObserved)
        (locus, thisLocusP)
    }
    for ((key, maybeVal) <- result; value <- maybeVal) yield key -> value
  }

  def lrMixMix(mix1: Genotypification, victim1: Option[Genotypification], mix2: Genotypification, victim2: Option[Genotypification], frequencyTable: FrequencyTable, mode: MatchingProbabilityCalculationMode): Map[Profile.Marker, Option[Double]] = {

    val loci = mix1.keySet intersect mix2.keySet

    def mixmixH2ForTable = mixmixH2(frequencyTable)(mode)_
    def mixmixH3ForTable = mixmixH3(frequencyTable)(mode)_

    loci.map { locus =>

      val p3locus = mixmixH3ForTable(locus, mix1.get(locus).get, victim1.flatMap { v => v.get(locus) }, mix2.get(locus).get, victim2.flatMap { v => v.get(locus) }, false)
      val p2alocus = mixmixH2ForTable(locus, mix1.get(locus).get, victim1.flatMap { v => v.get(locus) }, false)
      val p2blocus = mixmixH2ForTable(locus, mix2.get(locus).get, victim2.flatMap { v => v.get(locus) }, false)
      locus -> (for {n <- p3locus; d1 <- p2alocus; d2 <- p2blocus} yield n / (d1 * d2))
    }.toMap

  }
}