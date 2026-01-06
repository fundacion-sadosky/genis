package probability

import java.util.Date
import matching.MatchingAlgorithm.convertSingleAlele
import matching.{AleleRange, NewMatchingResult}
import play.api.Play.current
import play.api.Logger
import play.api.libs.concurrent.Akka
import probability.PValueCalculator.FrequencyTable
import profile.Profile.{Genotypification, Marker}
import profile.{Allele, AlleleValue, MicroVariant, OutOfLadderAllele, Profile}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object LRMixCalculator {
  // con este nombre se configura en el application.conf
  val name = "lrmix"

  val logger: Logger = Logger(this.getClass)

  implicit val executionContext = Akka.system.dispatchers.lookup("play.akka.actor.lrmix-context")

  private val factorials: Array[Int] = Array(1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880, 3628800, 39916800, 479001600)

  def pCond(frequencyTable: FrequencyTable, theta: Double)
           (marker: Profile.Marker, unknown : Array[Double], known : Array[Double],
            tOccurrences: Map[Double, Int] = Map.empty[Double, Int], vOccurrences: Map[Double, Int] = Map.empty[Double, Int]): (Double, Map[Double,Int]) = {

    try {
      var tuv = known
      var alleleOccurrences = tOccurrences

      val probability = unknown.foldLeft(1.0)((probability,u) => {
        val n = tuv.length
        val ni = alleleOccurrences.getOrElse(u, 0) + vOccurrences.getOrElse(u, 0)
        val pi = frequencyTable.getOrElse((marker, u), frequencyTable((marker, -1)))
        tuv = tuv :+ u
        alleleOccurrences += (u -> (alleleOccurrences.getOrElse(u, 0) + 1))
        probability * (ni * theta + (1 - theta) * pi) / (1 + (n - 1) * theta)
      })

      val unknownsNum = factorial(unknown.length)
      val unknownsDenom = unknown.groupBy(identity).values.foldLeft(1){(prev, current) =>
        prev * factorial(current.length)
      }
      val unknownsFactor = unknownsNum/unknownsDenom
      (probability * unknownsFactor, alleleOccurrences)

    } catch {
      case error: Throwable => throw new NoFrequencyException(s"Marker $marker doesn't exist in frequency table")
    }

  }

  def pCondMixMix(frequencyTable: FrequencyTable, theta: Double)
           (marker: Profile.Marker, unknown : Array[Double], known : Array[Double],
            tOccurrences: Map[Double, Int] = Map.empty[Double, Int], vOccurrences: Map[Double, Int] = Map.empty[Double, Int]): (Double, Map[Double,Int]) = {

    try {
      var tuv = known
      var alleleOccurrences = tOccurrences

      val probability = unknown.foldLeft(1.0)((probability,u) => {
        val n = tuv.length
        val ni = alleleOccurrences.getOrElse(u, 0) + vOccurrences.getOrElse(u, 0)
        val pi = frequencyTable.getOrElse((marker, u), frequencyTable((marker, -1)))
        tuv = tuv :+ u
        alleleOccurrences += (u -> (alleleOccurrences.getOrElse(u, 0) + 1))
        probability * (ni * theta + (1 - theta) * pi) / (1 + (n - 1) * theta)
      })

      (probability, alleleOccurrences)

    } catch {
      case error: Throwable => throw new NoFrequencyException(s"Marker $marker doesn't exist in frequency table")
    }

  }

  def alleleValues(frequencyTable: FrequencyTable, marker: Profile.Marker): Array[Double] = {
    frequencyTable.keySet.foldLeft(Array[Double]()){ (prev, key) =>
      key match {
          // fmin is loaded as a (-1.0) value
        case (m,value) if m == marker && value != -1 => prev :+ value.toDouble
        case _ => prev
      }
    }
  }

  def combinationsWithRepetitions(array: Array[Double], n: Int): Iterator[Array[Double]] = {
    array match {
      case Array() => Iterator.single(Array())
      case _ => Array.fill(n)(array).flatten.combinations(n)
    }
  }

  def generateUnknowns(alleles: Array[Double], n: Int): Iterator[Array[Double]] =  {
    combinationsWithRepetitions(alleles,n)
  }

  def factorial(n: Int): Int = if (n < factorials.length) factorials(n) else (2 to n).product

  def dropIns(sample: Array[Double], participants: Array[Double]): Array[Double] = sample.distinct diff participants.distinct

  def dropOuts(sample: Array[Double], participants: Array[Double]): Array[Double] = participants.distinct diff sample.distinct

  def commonAlleles(sample: Array[Double], participants: Array[Double]): Array[Double] = (participants intersect sample).distinct

  def pRepOut(pOut: Double)(sample: Array[Double], participants: Array[Double])(occurrences: Map[Double, Int]): Double =  {
    val common = commonAlleles(sample, participants)
    val dropOut = dropOuts(sample, participants)

    val productCommon =
      common.foldLeft(1.0)((prev,allele) => prev * (1-pow(pOut, occurrences(allele))))

    val productDropOut =
      dropOut.foldLeft(1.0)((prev,allele) => prev * pow(pOut, occurrences(allele)))

    productCommon * productDropOut
  }

  def pRepIn(pIn: Double, frequencyTable: FrequencyTable, marker: Profile.Marker)
            (sample: Array[Double], participants: Array[Double]): Double =  {

    val dropIn = dropIns(sample, participants)

    if (dropIn.isEmpty) {
      1 - pIn
    } else {
      val productDropIn = dropIn.foldLeft(1.0)((prev,allele) => {
        try {
          prev * frequencyTable.getOrElse((marker, allele), frequencyTable((marker, -1)))
        } catch {
          case error: Throwable => throw new NoFrequencyException(s"Value ${allele} for Marker $marker doesn't exist in frequency table")
        }
      })

      pow(pIn, dropIn.length) * productDropIn
    }
  }

  def pRep(pIn: Double, pOut: Double, frequencyTable: FrequencyTable, marker: Profile.Marker)
          (sample: Array[Double], participants: Array[Double])
          (occurrences: Map[Double, Int]): Double =
    pRepOut(pOut)(sample, participants)(occurrences) * pRepIn(pIn, frequencyTable, marker)(sample,participants)

  def pow(x: Double, y: Int) :Double = {
    var tmp = 1.0
    var aux = y
    while(aux>0) {
      tmp = tmp*x
      aux -= 1
    }
    tmp
  }  //if (y==0) 1 else x * pow(x, y-1)

  def calculateLRMix(
    scenario: FullCalculationScenario,
    frequencyTable: FrequencyTable,
    allelesRanges:Option[NewMatchingResult.AlleleMatchRange] = None
  ): Future[LRResult] = Future {
    val prosecutor = getHypothesisLR(
      "H1",
      scenario.sample,
      scenario.prosecutor,
      frequencyTable,
      scenario.stats.theta,
      scenario.stats.dropIn,
      allelesRanges
    )
    val defense = getHypothesisLR(
      "H2",
      scenario.sample,
      scenario.defense,
      frequencyTable,
      scenario.stats.theta,
      scenario.stats.dropIn,
      allelesRanges
    )
    logger.debug(s"Prosecutor- Total: ${prosecutor._1} Detailed: ${prosecutor._2}")
    logger.debug(s"Defense- Total: ${defense._1} Detailed: ${defense._2}")
    val result: LRResult = LRResult(
      getResultTotal(prosecutor, defense),
      getResultDetailed(prosecutor, defense)
    )
    logger.debug(s"Result- Total: ${result.total} Detailed: ${result.detailed}")
    result
  }

  private def getResultDetailed(
    prosecutor: (Double, Map[Marker, Option[Double]]),
    defense: (Double, Map[Marker, Option[Double]])
  ): Map[Marker, Option[Double]] = {
    prosecutor._2.keySet.map(key => {
      if (prosecutor._2(key).isDefined && defense._2(key).isDefined) {
        if (defense._2(key).get > 0) {
          (key, Some(prosecutor._2(key).get / defense._2(key).get))
        } else {
          (key, Some(0.0))
        }
      } else {
        (key, None)
      }
    }).toMap
  }

  private def getResultTotal(
    prosecutor: (Double, Map[Marker, Option[Double]]),
    defense: (Double, Map[Marker, Option[Double]])
  ): Double = {
    if (defense._1 > 0) {
      prosecutor._1 / defense._1
    } else {
      0.0
    }
  }

  def getHypothesisLR(
    h: String,
    sample: Genotypification,
    hypothesis: FullHypothesis,
    frequencyTable: FrequencyTable,
    theta: Double,
    dropIn: Double,allelesRanges:Option[NewMatchingResult.AlleleMatchRange] = None
  ): (Double, Map[Marker, Option[Double]]) = {
    val detailed = sample
      .keySet
      .toArray
      .map(
      {
        marker =>
          logger.debug(s"$h - $marker - started: ${new Date()}")
          try {
            val sampleGenotypification = flattenGenotypifications(marker, Array(sample), allelesRanges)
            val selectedGenotypification = flattenGenotypifications(marker, hypothesis.selected, allelesRanges)
            val unselectedGenotypification = flattenGenotypifications(marker, hypothesis.unselected, allelesRanges)
            val typified = selectedGenotypification.union(unselectedGenotypification)
            val selectedOccurrences = getOccurrencesMap(selectedGenotypification)
            val unselectedOccurrences = getOccurrencesMap(unselectedGenotypification)
    //        val alleles = alleleValues(frequencyTable, marker).union(sampleGenotypification).union(typified).distinct
            var alleles = sampleGenotypification.union(typified).distinct
            // Agregamos un valor extra que simboliza el resto de los valores de la tabla de frecuencias para el marcador
            alleles = alleles :+ 666.0
            // Agregamos como probabilidad de 666 la suma de las probabilidades de los alelos no observados
            val frequencyTableExtended = addFrequencyOfDefault(666.0, alleles, marker, frequencyTable)
            val unknowns = generateUnknowns(alleles, hypothesis.unknowns*2)
            logger.debug(s"$h - $marker - unknowns: ${new Date()}")
            var calculations = 0.0
            while(unknowns.hasNext) {
              val u = unknowns.next()
  //            val (pCondRes, alleleOccurrences) = pCond(frequencyTable, theta)(
  //             marker, u, typified, selectedOccurrences, unselectedOccurrences)
              val (pCondRes, alleleOccurrences) = pCond(
                frequencyTableExtended, theta
              )(
                marker, u, typified, selectedOccurrences, unselectedOccurrences
              )
  //            val pRepRes = pRep(dropIn, hypothesis.dropOut, frequencyTable, marker)(
  //            sampleGenotypification, u ++ selectedGenotypification)(alleleOccurrences)
              val pRepRes = pRep(
                dropIn, hypothesis.dropOut, frequencyTableExtended, marker
              )(
                sampleGenotypification, u ++ selectedGenotypification
              )(
                alleleOccurrences
              )
              calculations += pRepRes * pCondRes
            }
            logger.debug(s"$h - $marker - finished: ${new Date()}")
            (marker, Some(calculations))
          } catch {
            case error: NoFrequencyException => {
              logger.debug(s"$h - $marker - finished: ${new Date()}")
              (marker, None)
            }
          }
        }
      )
    .toMap
    val total =
      if (detailed.values.flatten.exists(_>0)) {
        detailed
          .values
          .foldLeft(1.0)(
            (prev, current) =>
              if (current.isDefined && current .get != 0) prev * current.get else prev
          )
      } else {
        0.0
      }
    (total, detailed)
  }

  def getOccurrencesMap(selectedGenotypification: Array[Double]): Map[Double, Int] = {
    selectedGenotypification.foldLeft(Map.empty[Double, Int]) { (m, x) => m + ((x, m.getOrElse(x, 0) + 1)) }
  }
  def convertAllelesValues(alleles: Array[AlleleValue],marker:Profile.Marker,allelesRanges:Option[NewMatchingResult.AlleleMatchRange] = None) = {
    allelesRanges match {
      case Some(ar) => convertAleles(alleles,marker,ar)
      case _ => alleles
    }
  }
  def convertAleles(alelles:Array[AlleleValue],locus:Profile.Marker,locusRangeMap:NewMatchingResult.AlleleMatchRange): Array[AlleleValue] = {
    val range = locusRangeMap.get(locus).getOrElse(AleleRange(0,99))
    alelles.map(a=>convertSingleAlele(a,range))
//    alelles
  }
  def flattenGenotypifications(marker: Marker, genotypifications: Array[Genotypification],allelesRanges:Option[NewMatchingResult.AlleleMatchRange] = None) = {
    transformAlleleValues(convertAllelesValues(genotypifications.flatMap(gen => gen.getOrElse(marker, ArrayBuffer())),marker,allelesRanges))
  }


  def transformAlleleValues(alleles: Array[AlleleValue]): Array[Double] = {
    alleles.map {
      case Allele(v) => v.toDouble
      case OutOfLadderAllele(_,_) => -1.0
      case MicroVariant(_) => -1.0
      case _ => -1.0
    }
  }

  def lrMixMixNuevo(mix1: Genotypification, victim1: Option[Genotypification], mix2: Genotypification, victim2: Option[Genotypification], frequencyTable: FrequencyTable, m1Contributors : Int, m2Contributors : Int, allelesRanges:Option[NewMatchingResult.AlleleMatchRange] = None, theta: Double, dropIn: Double, dropOut : Double): LRResult = {
    val sharedLocuses = mix1.keySet intersect mix2.keySet
    val lrH1 = getLRMixMixHypotesis1(sharedLocuses, mix1, victim1, mix2, victim2, frequencyTable, m1Contributors, m2Contributors, allelesRanges, theta, dropIn, dropOut)
    val lrH0 = getLRMixMixHypotesis0(sharedLocuses, mix1, victim1, mix2, victim2, frequencyTable, m1Contributors, m2Contributors, allelesRanges, theta, dropIn, dropOut)

    println(s"evHx- Total: ${lrH1._1} Detailed: ${lrH1._2}")
    println(s"evH0- Total: ${lrH0._1} Detailed: ${lrH0._2}")
    val result = LRResult(getResultTotal(lrH1, lrH0), getResultDetailed(lrH1, lrH0))
    println(s"Result- Total: ${result.total} Detailed: ${result.detailed}")
    result

  }

  def addFrequencyOfDefault(defaultAllele: BigDecimal, observedAlleles: Array[Double], marker: Marker, frequencyTable: FrequencyTable) : FrequencyTable = {
    var frequencyTableExtended = frequencyTable

    val notObservedAllelesProbabilities = frequencyTable.filter(entry => entry._1._1.equals(marker) && !observedAlleles.contains(entry._1._2) && !entry._1._2.equals(-1)).map(_._2).toList
    val probability = notObservedAllelesProbabilities.foldLeft(0.0)((prev, current) => prev + current)

    frequencyTableExtended = frequencyTableExtended.updated((marker, defaultAllele), probability)

    frequencyTableExtended
  }

  def getLRMixMixHypotesis1(markers: Set[Marker], mix1: Genotypification, victim1: Option[Genotypification], mix2: Genotypification, victim2: Option[Genotypification], frequencyTable: FrequencyTable, m1Contributors : Int, m2Contributors : Int, allelesRanges:Option[NewMatchingResult.AlleleMatchRange] = None, theta: Double, dropIn: Double, dropOut : Double): (Double, Map[Marker, Option[Double]]) = {
    val detailed = markers.toArray.map({ marker =>

      try {
        val mix1Genotipification = flattenGenotypifications(marker, Array(mix1), allelesRanges)
        val mix2Genotipification = flattenGenotypifications(marker, Array(mix2), allelesRanges)

        val xAlleles = mix1Genotipification.intersect(mix2Genotipification)
        val posiblesX = generateUnknowns(xAlleles, 2);

        val known1 = if (victim1.isEmpty) Array[Double]() else flattenGenotypifications(marker, Array(victim1.get), allelesRanges)
        val known2 = if (victim2.isEmpty) Array[Double]() else flattenGenotypifications(marker, Array(victim2.get), allelesRanges)

        val cantRandomMenMix1 = m1Contributors - (if (victim1.isEmpty) 0 else 1) - 1 //le resto 1 mas que es el q voy a probar como compartido de los Xj
        val cantRandomMenMix2 = m2Contributors - (if (victim2.isEmpty) 0 else 1) - 1 //le resto 1 mas que es el q voy a probar como compartido de los Xj

//        val alleles = alleleValues(frequencyTable, marker).union(mix1Genotipification).union(mix2Genotipification).distinct
        var alleles = mix1Genotipification.union(mix2Genotipification).distinct
        alleles = alleles :+ 666.0 //Agregamos un valor extra que simboliza el resto de los valores de la tabla de frecuencias para el marcador

        val frequencyTableExtended = addFrequencyOfDefault(666.0, alleles, marker, frequencyTable) //Agregamos como probabilidad de 666 la suma de las probabilidades de los alelos no observados

        var posiblesYMix1 = generateUnknowns(alleles, cantRandomMenMix1 * 2)
        var posiblesYMix2 = generateUnknowns(alleles, cantRandomMenMix2 * 2)

        var probK = 0.0
        var probJ = 0.0

        while (posiblesX.hasNext) {
          val Xj = posiblesX.next()
          probK = 0.0
          val knownsOccurrences = getOccurrencesMap(known1 ++ known2 ++ Xj)

          while (posiblesYMix1.hasNext) {
            val Y1jk = posiblesYMix1.next()

            while (posiblesYMix2.hasNext) {
              val Y2jl = posiblesYMix2.next()
              val (probCond, allellesOcurrences) = pCondMixMix(frequencyTableExtended, theta)(marker, (Y1jk ++ Y2jl), (known1 ++ known2 ++ Xj), knownsOccurrences, Map.empty)

              val repetitioFactor = calculateRepetitionFactor(Array(Y1jk, Y2jl))
              val probWithFactor = repetitioFactor * probCond

              val m1Ocurrences = getOccurrencesMap(known1 ++ Xj ++ Y1jk)
              val probRepM1 = pRep(dropIn, dropOut, frequencyTableExtended, marker)(mix1Genotipification, (known1 ++ Xj ++ Y1jk))(m1Ocurrences)
              val m2Ocurrences = getOccurrencesMap(known2 ++ Xj ++ Y2jl)
              val probRepM2 = pRep(dropIn, dropOut, frequencyTableExtended, marker)(mix2Genotipification, (known2 ++ Xj ++ Y2jl))(m2Ocurrences)

              probK += probWithFactor * probRepM1 * probRepM2
            }
            posiblesYMix2 = generateUnknowns(alleles, cantRandomMenMix2 * 2)

          }

          val (probCondXj, ocurr) = pCondMixMix(frequencyTableExtended, theta)(marker, known1 ++ known2 ++ Xj, Array.empty, Map.empty, Map.empty)

          val repetitioFactor = calculateRepetitionFactor(Array(known1, known2, Xj))
          val probWithFactor = repetitioFactor * probCondXj

          val probXj = probWithFactor * probK
          probJ += probXj
          posiblesYMix1 = generateUnknowns(alleles, cantRandomMenMix1 * 2)
/*
          Xj.map(print(_))
          println(s" Prob: ${probXj}")
*/
        }

        (marker, Some(probJ))

      } catch {
        case error: NoFrequencyException => {
          (marker, None)
        }
      }

    }).toMap

    val total =
      if (detailed.values.flatten.exists(_ > 0)) detailed.values.foldLeft(1.0)((prev, current) => if (current.isDefined && current.get != 0) prev * current.get else prev)
      else 0.0

    (total, detailed)
  }

  def calculateRepetitionFactor(genotypes: Array[Array[Double]]) : Double= {
    var factor = 1.0
    genotypes.foreach(genotype => {
      val unknownsNum = factorial(genotype.length)
      val unknownsDenom = genotype.groupBy(identity).values.foldLeft(1){(prev, current) =>
        prev * factorial(current.length)
      }
      val unknownsFactor = unknownsNum/unknownsDenom

      factor = factor * unknownsFactor
    })

    factor
  }

  def getLRMixMixHypotesis0(markers: Set[Marker], mix1: Genotypification, victim1: Option[Genotypification], mix2: Genotypification, victim2: Option[Genotypification], frequencyTable: FrequencyTable, m1Contributors : Int, m2Contributors : Int, allelesRanges:Option[NewMatchingResult.AlleleMatchRange] = None, theta: Double, dropIn: Double, dropOut : Double): (Double, Map[Marker, Option[Double]]) = {
    val detailed = markers.toArray.map({ marker =>

      try {
        val mix1Genotipification = flattenGenotypifications(marker, Array(mix1), allelesRanges)
        val mix2Genotipification = flattenGenotypifications(marker, Array(mix2), allelesRanges)

        val known1 = if (victim1.isEmpty) Array[Double]() else flattenGenotypifications(marker, Array(victim1.get), allelesRanges)
        val known2 = if (victim2.isEmpty) Array[Double]() else flattenGenotypifications(marker, Array(victim2.get), allelesRanges)

        val cantRandomMenMix1 = m1Contributors - (if (victim1.isEmpty) 0 else 1)
        val cantRandomMenMix2 = m2Contributors - (if (victim2.isEmpty) 0 else 1)

//        val alleles = alleleValues(frequencyTable, marker).union(mix1Genotipification).union(mix2Genotipification).distinct
        var alleles = mix1Genotipification.union(mix2Genotipification).distinct
        alleles = alleles :+ 666.0 //Agregamos un valor extra que simboliza el resto de los valores de la tabla de frecuencias para el marcador

        val frequencyTableExtended = addFrequencyOfDefault(666.0, alleles, marker, frequencyTable) //Agregamos como probabilidad de 666 la suma de las probabilidades de los alelos no observados

        val posiblesYMix1 = generateUnknowns(alleles, cantRandomMenMix1 * 2)
        val posiblesYMix2 = generateUnknowns(alleles, cantRandomMenMix2 * 2)

        var probM1 = 0.0
        var probM2 = 0.0

        while (posiblesYMix1.hasNext) {
          val Y1jk = posiblesYMix1.next()
          val knownsOccurrences = getOccurrencesMap(known1 ++ Y1jk)
          val (probCondM1, allelesOcurrences) = pCondMixMix(frequencyTableExtended, theta)(marker, known1++Y1jk, Array.empty, Map.empty, Map.empty)

          val repetitioFactor = calculateRepetitionFactor(Array(known1, Y1jk))
          val probWithFactor = repetitioFactor * probCondM1

          val probRepM1 = pRep(dropIn, dropOut, frequencyTableExtended, marker)(mix1Genotipification, known1++Y1jk)(allelesOcurrences)

          probM1 += (probWithFactor * probRepM1)

        }

        while (posiblesYMix2.hasNext) {
          val Y2jk = posiblesYMix2.next()
          val knownsOccurrences = getOccurrencesMap(known2 ++ Y2jk)
          val (probCondM2, allelesOcurrences) = pCondMixMix(frequencyTableExtended, theta)(marker, known2++Y2jk, Array.empty, Map.empty, Map.empty)

          val repetitioFactor = calculateRepetitionFactor(Array(known2, Y2jk))
          val probWithFactor = repetitioFactor * probCondM2

          val probRepM2 = pRep(dropIn, dropOut, frequencyTableExtended, marker)(mix2Genotipification, known2++Y2jk)(allelesOcurrences)

          probM2 += (probWithFactor * probRepM2)

        }

        val probM1M2 = probM1 * probM2
        (marker, Some(probM1M2))

      } catch {
        case error: NoFrequencyException => {
          (marker, None)
        }
      }

    }).toMap

    val total =
      if (detailed.values.flatten.exists(_ > 0)) detailed.values.foldLeft(1.0)((prev, current) => if (current.isDefined && current.get != 0) prev * current.get else prev)
      else 0.0

    (total, detailed)
  }

 }
