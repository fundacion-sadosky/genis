package probability

import profile.Profile.Marker
import profile.{Allele, _}
import stats.{PopulationBaseFrequency, PopulationSampleFrequency}

import scala.language.postfixOps

object PValueCalculator {

  type FrequencyTable = Map[(Marker, BigDecimal), Double]

  def parseFrequencyTable(populationBaseFrequency: PopulationBaseFrequency): FrequencyTable = {

    var minimumValuesPerMarker: Map[Marker, Double] = Map()

    val list = populationBaseFrequency.base.map { entry: PopulationSampleFrequency =>
      if (entry.allele == -1) {
        minimumValuesPerMarker += ((entry.marker, entry.frequency.toDouble))
      }

      ((entry.marker, BigDecimal(entry.allele)), entry.frequency.toDouble)
    }

    val listWithMinimumValues = list.map { entry =>
      (entry._1, math.max(entry._2, minimumValuesPerMarker(entry._1._1)))
    }

    listWithMinimumValues.toMap[(String, BigDecimal), Double]
  }

  def getMinimumFrequency(marker: Marker, frequencyTable: FrequencyTable): Option[Double] = {
    frequencyTable.get((marker, BigDecimal(-1)))
  }


  def getProbability(frequencyTable: FrequencyTable)(marker: Marker, allele: BigDecimal): Option[Double] = {
    val p = frequencyTable.get((marker, allele)) match {
      case Some(freq) => Some(freq)
      case None       => getMinimumFrequency(marker, frequencyTable).map { min => min }
    }
    p
  }
  def getMinimumFrequencyForLocus(frequencyTable: FrequencyTable)(marker: Marker): Option[Double] = {
      getMinimumFrequency(marker, frequencyTable).map { min => min }
  }
  
  def calculateRMP(frequencyTable: FrequencyTable)(mode: MatchingProbabilityCalculationMode)(locus: Marker, alleles: Option[Seq[AlleleValue]]): Option[Double] = {
    alleles match {
      case Some(List(Allele(n))) => {
        for {
          p <- getProbability(frequencyTable)(locus, n)

        } yield mode.wildcard(p, 0) // Asumir que el otro es dropout porque favorece al acusado - Visto con Ariel
      }
      case Some(List(Allele(n), Allele(m))) if (n == m) => {
        for {
          p <- getProbability(frequencyTable)(locus, n)
        } yield mode.homo(p)
      }
      case Some(List(Allele(n), Allele(m))) if (n != m) => {
        for {
          p1 <- getProbability(frequencyTable)(locus, n)
          p2 <- getProbability(frequencyTable)(locus, m)
        } yield mode.hetero(p1, p2)
      }
      case _ => {
        Some(1.0)
      }
    }
  }

  /*def autosomalRMP(frequencyTable: FrequencyTable)(mode: MatchingProbabilityCalculationMode)(markers: Set[String], profile: Genotypification): Map[Profile.Marker, Option[Double]] = {

    val nAlleles = frequencyTable.keys.groupBy(_._1).map { case (marker, list) => (marker, list.size) }.toMap

    val interestingLoci = profile.keySet.intersect(markers)

    def calculateRMPForLocus = calculateRMP(frequencyTable)(mode)_

    val set = interestingLoci.map { locus =>
      val alleles = profile.get(locus)

      val rmp = calculateRMPForLocus(locus, alleles)
      (locus -> rmp)
    }
    set.toMap
  }

  def xyRMP(frequencyTable: FrequencyTable)(markers: Set[String], profile: Profile): Option[Double] = {
    Some(1.0)
  }

  def crRMP(frequencyTable: FrequencyTable)(mode: MatchingProbabilityCalculationMode)(markers: Set[String], profile: Profile): Option[Double] = {
    Some(1.0)
  }
  def mtDnaRMP(frequencyTable: FrequencyTable)(markers: Set[String], profile: Profile): Option[Double] = {
    Some(1.0)
  }*/

  def showal(x: Seq[AlleleValue]) = "{" + x.map(a => a.toString()).mkString(", ") + "}"
  def showala(x: Option[Seq[AlleleValue]]) = x match {
    case Some(l) => showal(l)
    case None    => "{}"
  }


  def mixmixH3(frequencyTable: FrequencyTable)(mode: MatchingProbabilityCalculationMode)(locus: Marker, mix1: Seq[AlleleValue], victim1: Option[Seq[AlleleValue]], mix2: Seq[AlleleValue], victim2: Option[Seq[AlleleValue]], verbose: Boolean = false): Option[Double] = {

    if (verbose) {
      println("ENTER mixmixH3 WITH m1=" + showal(mix1) + ", m2=" + showal(mix2) +
        " v1=" + showala(victim1) + " v2=" + showala(victim2))
    }

    val suspectBase = mix1 intersect mix2

    val oneElementCombinations = suspectBase.combinations(1)

    val oneElementCombinationsDuplicated = oneElementCombinations.map { list => list ++ list }

    val possibleSuspects = oneElementCombinationsDuplicated ++ suspectBase.combinations(2)

    def calculateRMPForLocus = calculateRMP(frequencyTable)(mode)_
    def calculatePForLocus = getProbability(frequencyTable)_
    def calculateMinimumFrequencyForLocus = getMinimumFrequencyForLocus(frequencyTable)_
    
    val pOfScenario = possibleSuspects map { suspect =>
      val rem1 = mix1 diff suspect
      val rem2 = mix2 diff suspect

      if (rem1.length <= 2 && rem2.length <= 2) {

        val victims1 = if (rem1.length == 0) Seq(suspect) else if (rem1.length == 1) {
          val theAllele = rem1(0)
          Seq(theAllele, theAllele) +: suspect.map { a: AlleleValue => Seq(a, theAllele) }
        } else Seq(rem1)

        val victims2 = if (rem2.length == 0) Seq(suspect) else if (rem2.length == 1) {
          val theAllele = rem2(0)
          Seq(theAllele, theAllele) +: suspect.map { a: AlleleValue => Seq(a, theAllele) }
        } else Seq(rem2)

        val scenarios = for {
          v1 <- victims1 if (victim1.isEmpty || ((victim1.get) diff v1).isEmpty) // if victim informed it must be contained in generated
          v2 <- victims2 if (victim2.isEmpty || ((victim2.get) diff v2).isEmpty) // if victim informed it must be contained in generated
        } yield {
          (suspect, v1, v2)
        }

        if (verbose) {
          println("[mixmixH3] scenarios for suspect = " + showal(suspect))
          scenarios foreach {
            sce => println("    S=" + showal(sce._1) + " v1=" + showal(sce._2) + " v2=" + showal(sce._3))
          }
        }

        if (scenarios.isEmpty) {
          println("    NO SCENARIOS")
          None
        } else {
          // probability is 
          // sum of scenarios product for each. 
          val sumOfP = scenarios.foldLeft(0.0) { (prev, current) =>
            val pSuspect = calculateRMPForLocus(locus, Some(current._1))
            
            val unconditionatedAlleles1 = current._2 diff (if (victim1.isEmpty) Seq() else victim1.get)
            
            val pVictim1 = if (!unconditionatedAlleles1.isEmpty) {
              if (unconditionatedAlleles1.length > 1) {
                calculateRMPForLocus(locus, Some(unconditionatedAlleles1))
              } else {
                val theAllele = unconditionatedAlleles1(0)
                getP(locus, calculatePForLocus , theAllele,calculateMinimumFrequencyForLocus)

              }
            } else {
              None
            }

            val unconditionatedAlleles2 = current._3 diff (if (victim2.isEmpty) Seq() else victim2.get)
            
            val pVictim2 = if (!unconditionatedAlleles2.isEmpty) {
              if (unconditionatedAlleles2.length > 1) {
                calculateRMPForLocus(locus, Some(unconditionatedAlleles2))
              } else {
                val theAllele = unconditionatedAlleles2(0)
                getP(locus, calculatePForLocus , theAllele,calculateMinimumFrequencyForLocus)
              }
            } else {
              None
            }
            
            val p = Seq(pSuspect, pVictim1, pVictim2).flatten
            if (verbose) {
              println("       p=" + p.product)

            }
            prev + p.product

          }
          if (verbose) {
            println("sumOfP =" + sumOfP)
          }

          Some(sumOfP)
        }
      } else {
        None
      }
    }
    val interesting = pOfScenario.flatten
    if (interesting.isEmpty)
      None
    else
      Some(interesting.sum)

  }

  def mixmixH2(frequencyTable: FrequencyTable)(mode: MatchingProbabilityCalculationMode)(locus: Marker, mix1: Seq[AlleleValue], victim1: Option[Seq[AlleleValue]], verbose: Boolean = false): Option[Double] = {

    if (verbose) {
      println("ENTER mixmixH2 WITH m1=" + showal(mix1) + 
        " v1=" + showala(victim1) )
    }

    val suspectBase = mix1

    val oneElementCombinations = suspectBase.combinations(1)

    val oneElementCombinationsDuplicated = oneElementCombinations.map { list => list ++ list }

    val possibleSuspects = oneElementCombinationsDuplicated ++ suspectBase.combinations(2)

    def calculateRMPForLocus = calculateRMP(frequencyTable)(mode)_
    def calculatePForLocus = getProbability(frequencyTable)_
    def calculateMinimumFrequencyForLocus = getMinimumFrequencyForLocus(frequencyTable)_

    val pOfScenario = possibleSuspects map { suspect =>
      val rem1 = mix1 diff suspect

      if (rem1.length <= 2) {

        val victims1 = if (rem1.length == 0)
          Seq(suspect)
        else if (rem1.length == 1) {
          val theAllele = rem1(0)
          Seq(theAllele, theAllele) +: suspect.map { a: AlleleValue => Seq(a, theAllele) }
        } else
          Seq(rem1)

        val scenarios = for {
          v1 <- victims1 if (victim1.isEmpty || ((victim1.get) diff v1).isEmpty) // if victim informed it must be contained in generated
        } yield {
          (suspect, v1)
        }

        if (verbose) {
          println("[mixmixH2] scenarios for  suspect = " + showal(suspect))
          scenarios foreach {
            sce => println("    S=" + showal(sce._1) + " v=" + showal(sce._2))
          }
        }

        if (scenarios.isEmpty) {
          println("    NO SCENARIOS")
          None
        } else {
          // probability is 
          // sum of scenarios product for each. 
          val sumOfP = scenarios.foldLeft(0.0) { (prev, current) =>
            
            val pSuspect = calculateRMPForLocus(locus, Some(current._1))
            
            val unconditionatedAlleles = current._2 diff (if (victim1.isEmpty) Seq() else victim1.get)
            
            val pVictim = if (!unconditionatedAlleles.isEmpty) {
              if (unconditionatedAlleles.length > 1) {
                calculateRMPForLocus(locus, Some(unconditionatedAlleles))
              } else {
                val theAllele = unconditionatedAlleles(0)
                getP(locus, calculatePForLocus, theAllele,calculateMinimumFrequencyForLocus)
                
              }
            } else {
              None
            }

            val p = Seq(pSuspect, pVictim).flatten
            if (verbose) {
              println("       p=" + p.product)

            }

            prev + p.product

          }
          Some(sumOfP)
        }
      } else {
        None
      }
    }
    val interesting = pOfScenario.flatten
    if (interesting.isEmpty)
      None
    else
      Some(interesting.sum)

  }

  private def getP(locus: Marker, calculatePForLocus: (Marker, BigDecimal) => Option[Double], theAllele: AlleleValue,calculateMinimumFrecuency: (Marker) => Option[Double]) = {
    theAllele match {
      case Allele(n) => calculatePForLocus(locus, n)
      case XY(_) => Some(1.0)
      case _ => calculateMinimumFrecuency(locus)
    }
  }

  def mixmixH2Q(frequencyTable: FrequencyTable)(mode: MatchingProbabilityCalculationMode)(locus: Marker, mix1: Seq[AlleleValue], victim1: Option[Seq[AlleleValue]], verbose: Boolean = false): Option[Double] = {
    val groupSpecs = List(GroupSpec("UNIQUE", 0.0d, frequencyTable)) // TODO: ojo theta

    val specs = List(MixParticipantLocus(victim1, "UNIQUE"), MixParticipantLocus(None, "UNIQUE"))
    val observed = victim1.toList.map { ObservedLoci(_, "UNIQUE") }

    MixtureLRCalculator.pmixGivenForLocus(locus, groupSpecs, mix1, specs, observed)
  }

}
