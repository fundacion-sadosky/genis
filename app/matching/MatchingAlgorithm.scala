package matching

import java.util.Date

import kits.Locus
import configdata.{MatchingRule, MtConfiguration, MtRegion}
import matching.MatchingResult.AlleleMatchResult
import matching.Stringency.{Stringency, _}
import matching.MatchingCalculatorService
import org.bson.types.ObjectId
import pedigree.{Individual, _}
import play.api.Logger
import probability.ProbabilityService
import profile.GenotypificationByType.GenotypificationByType
import profile.Profile.Genotypification
import profile._
import types.{MongoDate, MongoId, SampleCode}

import scala.collection.Seq
import scala.language.postfixOps

object MatchingAlgorithm {

  val logger: Logger = Logger(this.getClass)

  val baseMtDnaWildcards = Map(
    'A' -> Set('A'),
    'T' -> Set('T'),
    'C' -> Set('C'),
    'G' -> Set('G'),
    'R' -> Set('A', 'G'),
    'Y' -> Set('C', 'T'),
    'S' -> Set('G', 'C'),
    'W' -> Set('A', 'T'),
    'K' -> Set('G', 'T'),
    'M' -> Set('A', 'C'),
    'B' -> Set('C', 'G', 'T'),
    'D' -> Set('A', 'G', 'T'),
    'H' -> Set('A', 'C', 'T'),
    'V' -> Set('A', 'C', 'G'),
    'N' -> Set('C', 'G', 'A', 'T'),
    '-' -> Set('-')
  )

  private def getSequencesToCompare(p: Profile.Genotypification, q: Profile.Genotypification): List[(List[Mitocondrial], List[Mitocondrial])] = {
    val prang = p.filterKeys(x=> x.endsWith("_RANGE")).map { pRange =>
      getRange(pRange._1.toString, p)
    }.toList

    val rrange = q.filterKeys(x=> x.endsWith("_RANGE")).map{ qRange =>
      getRange(qRange._1.toString,q)
    }.toList


    val a = for{ x<- prang; y <- rrange} yield(x,y)

      a.map{ case (pr,qr) => mergeRanges(pr._1,qr._1).map(range => (getSequence(p,pr._2.replace("_RANGE",""),range), getSequence(q,qr._2.replace("_RANGE",""),range)))
     case _ => None
     }.filter(_.isDefined).map(_.get)

  }

  private def getSequence(analysis: Profile.Genotypification,hv: String, range: (Int, Int)) = {
    analysis(hv) flatMap {
      case allele@Mitocondrial(base, pos) if pos >= range._1 && pos <= range._2 => Some(allele)
      case _ => None
    }
  }
  private def getAllMitocondrialSequence(genotypification: Profile.Genotypification):List[Mitocondrial] = {
    genotypification.values.flatten.toList flatMap {
      case allele@Mitocondrial(base, pos) => Some(allele)
      case _ => None
    }
  }

  private def getAnalysis(p: Profile, matchingRule: MatchingRule) = {
      p.genotypification.get(matchingRule.`type`).map(x => List(x)).getOrElse(List.empty[Profile.Genotypification])
  }

  private def getRange(hv: String, genotypification: Profile.Genotypification) = {
    if (genotypification.contains(hv)) {
      val range = genotypification(hv)
      ((range(0).asInstanceOf[Allele].count.toInt, range(1).asInstanceOf[Allele].count.toInt),hv)
    }else{
      ((0,16569),"")
    }
  }

  def mergeRanges(range1: (Int, Int), range2: (Int, Int)): Option[(Int, Int)] = {
    (range1, range2) match {
      case ((a, b), (c, d)) if c > b && d > b => None
      case ((a, b), (c, d)) if a > d && b > d => None
      case ((a, b), (c, d)) if a <= c && b <= d => Some((c, b))
      case ((a, b), (c, d)) if a <= c && b >= d => Some((c, d))
      case ((a, b), (c, d)) if a >= c && b <= d => Some((a, b))
      case ((a, b), (c, d)) if a >= c && b >= d => Some((a, d))
    }
  }
  def mergeMismatches(matchingRule: Option[MatchingRule],numberOfMismatches: Seq[Int]):Option[MatchingRule] = {
    if(numberOfMismatches.isEmpty){
      matchingRule
    }else{
      matchingRule.map(x => x.copy(mismatchsAllowed = numberOfMismatches.head))
    }
  }
  def profileMtMatch(config: MtConfiguration, p: Profile, q: Profile, matchingRule: MatchingRule, id: Option[MongoId] = None, n: Long): Option[MatchResult] = {
    val pGenotypes = getAnalysis(p, matchingRule)
    val qGenotypes = getAnalysis(q, matchingRule)

    val (mismatches, result) = getMtResult(matchingRule.mismatchsAllowed,config, pGenotypes, qGenotypes)
    if (result == HighStringency) {
      val matchingAlleles = getMatchingVariances(getAllMitocondrialSequence(pGenotypes.head),getAllMitocondrialSequence(qGenotypes.head))
      val map = matchingAlleles.map(x => x -> HighStringency).toMap
      val matchingResult = NewMatchingResult(HighStringency, map, map.size, q.categoryId, 1, 1, matchingRule.matchingAlgorithm)
      createMatchResult(p, q, matchingRule, id, n, matchingResult,None,mismatches)
    } else None
  }
  def getMatchingVariances(p:List[Mitocondrial],q:List[Mitocondrial]):List[String] = {
    (for{ x<- p; y <- q} yield(x,y)).flatMap{
      case (var1@Mitocondrial(base,pos),var2@Mitocondrial(base2,pos2)) if pos == pos2 && baseMatch(base,base2) => {
        Some(List(s"${base.toString}@$pos".replace(".",","),s"${base2.toString}@$pos2".replace(".",",")))
      }
      case _ => None
    }.flatten.distinct
  }

  def getMtResult(maxMismatches: Int,config: MtConfiguration, pGenotypes: List[Profile.Genotypification],
                  qGenotypes: List[Profile.Genotypification]): (Int,Stringency.Value) = {

    val partialResults = pGenotypes.flatMap(pAn => {
          qGenotypes.flatMap(qAn => {
            val sequences = getSequencesToCompare(pAn, qAn)
          val a = sequences.map{case (pSequence, qSequence) =>  mtDnaMatch(config, pSequence.sortBy(_.position), qSequence.sortBy(_.position))}
            a
          })

    })

    val (mismatches, intersection) = if (partialResults.isEmpty) (0, false)
                                      else(partialResults.sum, true)

    if (mismatches <= maxMismatches  && intersection) (mismatches,HighStringency)
    else (mismatches,NoMatch)
  }

  def baseMatch(base1: Char, base2: Char) = baseMtDnaWildcards.getOrElse(base1, Set()).intersect(baseMtDnaWildcards.getOrElse(base2, Set())).nonEmpty

  def mtDnaMatch(config: MtConfiguration,s: Seq[Mitocondrial], t: Seq[Mitocondrial]): Int = {

    def unmatched(s1: Seq[Mitocondrial], s2: Seq[Mitocondrial], unmatchCount: Int): Int = {
      (s1, s2) match {
        case (Nil, _) => unmatchCount + s2.size
        case (_, Nil) => unmatchCount + s1.size
        case (s1e :: s1s, t1e :: t1s) if s1e.position == t1e.position && baseMatch(s1e.base, t1e.base) => unmatched(s1s, t1s, unmatchCount)
        case (s1e :: s1s, t1e :: t1s) if s1e.position == t1e.position && !baseMatch(s1e.base, t1e.base) => unmatched(s1s, t1s, unmatchCount + 1)
        case (s1e :: s1s, t1e :: t1s) if s1e.position < t1e.position => unmatched(s1s, t1e :: t1s, unmatchCount + 1)
        case (s1e :: s1s, t1e :: t1s) if s1e.position > t1e.position => unmatched(s1e :: s1s, t1s, unmatchCount + 1)
      }
    }
    val ignorePoints = config.ignorePoints
    unmatched(s.filter(mt => !ignorePoints.contains(mt.position.intValue())),
      t.filter(mt => !ignorePoints.contains(mt.position.intValue())),
      0)
  }


  def isBetweeen(x:Integer,y:BigDecimal):Boolean ={
    return y>BigDecimal.valueOf(x.intValue()) && y<BigDecimal.valueOf(x.intValue())+1
  }
  def toBigDecimal(x:Integer):BigDecimal = {
    BigDecimal.valueOf(x.intValue())
  }
  def matchLevel(a1: AlleleValue, a2: AlleleValue): Stringency = (a1, a2) match {
    case (Allele(x), Allele(y)) if x == y => HighStringency

    case (MicroVariant(x), Allele(y)) if isBetweeen(x,y) => HighStringency

    case (Allele(y),MicroVariant(x)) if isBetweeen(x,y) => HighStringency

    case (MicroVariant(y),MicroVariant(x)) if y == x => HighStringency

    case (OutOfLadderAllele(_,">"),OutOfLadderAllele(_,">")) => HighStringency
    case (OutOfLadderAllele(_,"<"),OutOfLadderAllele(_,"<")) => HighStringency

    case (OutOfLadderAllele(x,">"),Allele(y)) if y > x => HighStringency
    case (OutOfLadderAllele(x,"<"),Allele(y)) if y < x => HighStringency

    case (Allele(y),OutOfLadderAllele(x,">")) if y > x => HighStringency
    case (Allele(y),OutOfLadderAllele(x,"<")) if y < x => HighStringency

    case (OutOfLadderAllele(x,">"),MicroVariant(y)) if toBigDecimal(y) >= x => HighStringency
    case (OutOfLadderAllele(x,"<"),MicroVariant(y)) if toBigDecimal(y) < x => HighStringency

    case (MicroVariant(y),OutOfLadderAllele(x,">")) if toBigDecimal(y) >= x => HighStringency
    case (MicroVariant(y),OutOfLadderAllele(x,"<")) if toBigDecimal(y) < x => HighStringency

    //    case (SequencedAllele(x, p), Allele(y)) if x == y => HighStringency
//    case (Allele(x), SequencedAllele(y, p)) if x == y => HighStringency
//    case (SequencedAllele(x, p), SequencedAllele(y, q)) if x == y => HighStringency
    case (XY(x), XY(y)) if x == y => HighStringency
//    case (Mitocondrial(base1, pos1), SequencedAllele(base2, pos2)) if base1 == base2 && pos1 == pos2 => HighStringency
    case _ => NoMatch
  }

  def worstMatchLevel(s: Seq[AlleleValue], t: Seq[AlleleValue]): Stringency = {
    val zipped = s.zip(t)
    val zm = zipped.map({ pair => matchLevel(pair._1, pair._2) })
    if (zm.isEmpty)
      NoMatch
    else
      zm.max
  }

  // Note: HighStringency < MediumStringency < LowStringency

  def genotypeMatch(s: Seq[AlleleValue], t: Seq[AlleleValue]): Stringency = {
    val ordered = Seq(s, t).sortBy(a => a.toSet.size)
    val smallerSet = ordered(0).toSet
    val largerSet = ordered(1).toSet
    val smaller = smallerSet.toSeq
    val larger = largerSet.toSeq

    // if different size, it can match at most ModerateStringency

    // find the best matching permutation
    val bestFoundLevelWithoutLow = larger.permutations.map { permutation =>
      worstMatchLevel(permutation, smaller)
    }.min

    val stringencyLimit = if (smaller.size == larger.size) Stringency.HighStringency else Stringency.ModerateStringency

    val bestFoundLevel = if (Stringency.NoMatch == bestFoundLevelWithoutLow && anyMatch(smallerSet,largerSet)) Stringency.LowStringency else bestFoundLevelWithoutLow

    // Cannot be better than limit
    List(stringencyLimit, bestFoundLevel).max

  }
  def anyMatch(a: Set[AlleleValue],b:Set[AlleleValue]) :Boolean = {
    a.flatMap(aitem => b.map(bitem => matchLevel(aitem,bitem) == Stringency.HighStringency )).exists(identity)
  }
  def getMostRestrictive(alleleA:AlleleValue,alleleB:AlleleValue):AlleleValue = {
    (alleleA,alleleB) match {
      case (alleleA: MicroVariant,alleleB: OutOfLadderAllele) => alleleA
      case (alleleA: MicroVariant,alleleB) => alleleB
      case (OutOfLadderAllele(x,">"),OutOfLadderAllele(y,">")) if y > x => OutOfLadderAllele(y,">")
      case (OutOfLadderAllele(x,">"),OutOfLadderAllele(y,">")) if y <= x => OutOfLadderAllele(x,">")
      case (OutOfLadderAllele(x,"<"),OutOfLadderAllele(y,"<")) if y > x => OutOfLadderAllele(x,"<")
      case (OutOfLadderAllele(x,"<"),OutOfLadderAllele(y,"<")) if y <= x => OutOfLadderAllele(y,"<")
      case (alleleA: OutOfLadderAllele,alleleB) => alleleB
      case (_,_) => alleleA
    }
  }
  def intersectMatch(a: Seq[AlleleValue],b:Seq[AlleleValue]): Seq[AlleleValue] = {
    a.flatMap(aitem => b.map(bitem => {
      if(matchLevel(aitem,bitem) == Stringency.HighStringency){
        //me quedo con el mas restrictivo
        Some(getMostRestrictive(aitem,bitem))
      }else{
        None
      }
    } )).flatten
  }
  def diffMatch(a: Seq[AlleleValue],b:Seq[AlleleValue]): Seq[AlleleValue] = {
    a.filter(aitem => !anyMatch(Set(aitem),b.toSet))
  }
  def containsMatch(a: Seq[AlleleValue],item:AlleleValue): Boolean ={
    anyMatch(Set(item),a.toSet)
  }
  def genotypeMatchOfMixes(mix1: Seq[AlleleValue], victim1: Option[Seq[AlleleValue]], mix2: Seq[AlleleValue], victim2: Option[Seq[AlleleValue]]): Stringency = {

    val matchLevel = (victim1, victim2) match {
      case (Some(seqV1), Some(seqV2)) => {
        val suspect = (intersectMatch(diffMatch(mix1,seqV1),mix2) union (intersectMatch(diffMatch(mix2,seqV2),mix1)))
        val cond1 = mix1 forall (x => containsMatch((suspect union seqV1),x))
        val cond2 = mix2 forall (x => containsMatch((suspect union seqV2),x))
        val cond3 = suspect forall (x => containsMatch(intersectMatch(mix1,mix2),x))
        if (cond1 && cond2 && cond3) Stringency.ModerateStringency else Stringency.Mismatch
      }
      case (Some(seqV1), None) => {
        val suspectBase = intersectMatch(mix1,mix2)
        val possibleSuspects = suspectBase.combinations(1) ++ suspectBase.combinations(2)

        val maybeSuspect = possibleSuspects.find { suspect =>

          // check if exists V with lenght 1 or 2 that fits
          val cond1 = mix1 forall (x => containsMatch((suspect union seqV1),x))
          val cond2 = diffMatch(mix2,suspect).length <= 2 // equivalent to: exists V' of length 2 that fits
          val cond3 = suspect forall (x => containsMatch(intersectMatch(mix1,mix2),x))

          val result = cond1 && cond2 && cond3
          result
        }
        maybeSuspect match {
          case Some(suspect) => Stringency.ModerateStringency
          case None => Stringency.Mismatch
        }
      }
      case (None, Some(seqV2)) => {
        val suspectBase = intersectMatch(mix1,mix2)
        val possibleSuspects = suspectBase.combinations(1) ++ suspectBase.combinations(2)

        val maybeSuspect = possibleSuspects.find { suspect =>

          // check if exists V with lenght 1 or 2 that fits
          val cond1 = diffMatch(mix1,suspect).length <= 2 // equivalent to: exists V of length 2 that fits
          val cond2 = mix2 forall (x => containsMatch((suspect union seqV2),x))
          val cond3 = suspect forall (x => containsMatch(intersectMatch(mix1,mix2),x))

          val result = cond1 && cond2 && cond3
          result
        }
        maybeSuspect match {
          case Some(suspect) => Stringency.ModerateStringency
          case None => Stringency.Mismatch
        }
      }
      case (None, None) => {
        val suspectBase = intersectMatch(mix1,mix2)
        val possibleSuspects = suspectBase.combinations(1) ++ suspectBase.combinations(2)

        val maybeSuspect = possibleSuspects.find { suspect =>

          // check if exists V with lenght 1 or 2 that fits
          val cond1 = diffMatch(mix1,suspect).length <= 2 // equivalent to: exists V of length 2 that fits
          val cond2 = diffMatch(mix2,suspect).length <= 2 // equivalent to: exists V' of length 2 that fits
          val cond3 = suspect forall (x => containsMatch(intersectMatch(mix1,mix2),x)) // Always true!

          val result = cond1 && cond2 && cond3
          result
        }
        maybeSuspect match {
          case Some(suspect) => Stringency.ModerateStringency
          case None => Stringency.Mismatch
        }

      }
    }

    matchLevel
  }

  def genotypeMatchOfMixeWithVictim(mix1: Seq[AlleleValue], victim1: Option[Seq[AlleleValue]], mix2: Seq[AlleleValue], victim2: Option[Seq[AlleleValue]]): Stringency = {

    val matchLevel = (victim1, victim2) match {
      case (Some(seqV1), None) => {
        val suspect = mix2

        val cond1 = mix1 forall (x => containsMatch((suspect union seqV1),x))
        val cond2 = diffMatch(suspect, mix1).length == 0 // equivalent to: exists V' of length 2 that fits

        val result = cond1 && cond2
        result match {
          case true => Stringency.ModerateStringency
          case false => Stringency.Mismatch
        }
      }

      case (None, Some(seqV2)) => {
        val suspect = mix1
        val cond1 = mix2 forall (x => containsMatch((suspect union seqV2),x))
        val cond2 = diffMatch(suspect, mix2).length == 0 // equivalent to: exists V' of length 2 that fits
        val result = cond1 && cond2
        result match {
          case true => Stringency.ModerateStringency
          case false => Stringency.Mismatch
        }
      }
      case (None, None) => {
        val cond1 = mix1 forall (x => containsMatch(mix2,x))
        val cond2 = mix2 forall (x => containsMatch(mix1,x))
        val result = cond1 || cond2
        result match {
          case true => Stringency.ModerateStringency
          case false => Stringency.Mismatch
        }


      }
    }

    matchLevel
  }


  case class MatchGroup(group: String, worstMatchLevel: Stringency.Value, matches: AlleleMatchResult)

  def markerGroup(marker: String): String = {
    "Aut"
  }

  def uniquePonderation(mr: MatchResult, firingProfile: Profile, matchingProfile: Profile) = {
    val right = firingProfile.globalCode == mr.rightProfile.globalCode
    if (!firingProfile.isReference && matchingProfile.isReference) {
      if (right) mr.result.leftPonderation else mr.result.rightPonderation
    } else {
      if (right) mr.result.rightPonderation else mr.result.leftPonderation
    }
  }

  def sharedAllelePonderation(leftGenotype: Genotypification, rightGenotype: Genotypification) = {
    val sharedMarkers = leftGenotype.keySet.intersect(rightGenotype.keySet)
    val sharedMarkersQty = sharedMarkers.size
    var leftAccum = 0.0
    var rightAccum = 0.0

    sharedMarkers.foreach(marker => {
      val rightAlleles = rightGenotype.get(marker).get
      val leftAlleles = leftGenotype.get(marker).get

      val sharedAlleles = rightAlleles.toSet.intersect(leftAlleles.toSet)
      rightAccum = rightAccum + sharedAlleles.size.toFloat / rightAlleles.toSet.size
      leftAccum = leftAccum + sharedAlleles.size.toFloat / leftAlleles.toSet.size
    })

    if (sharedMarkersQty.equals(0)) (0.0, 0.0)
    else (leftAccum / sharedMarkersQty, rightAccum / sharedMarkersQty)

  }

  def profileMatch(p: Profile, q: Profile, matchingRule: MatchingRule, id: Option[MongoId] = None, n: Long,locusRangeMap:NewMatchingResult.AlleleMatchRange = Map.empty,idCourtCase: Option[Long] = None): Option[MatchResult] = {
    val pGenotype = p.genotypification.getOrElse(matchingRule.`type`, Map.empty)
    val qGenotype = q.genotypification.getOrElse(matchingRule.`type`, Map.empty)

    val matchLevels = getMatchLevels(p, q, matchingRule, pGenotype, qGenotype,locusRangeMap)

    matchLevels match {
      case None => None
      case Some(map) => {
        val result = getNewMatchingResult(map, q, matchingRule, pGenotype, qGenotype,locusRangeMap)
        val misMatchList = List(Stringency.Mismatch,Stringency.NoMatch,Stringency.ImpossibleMatch)
        val mismatches = result.matchingAlleles.count(x => misMatchList.contains(x._2))
        createMatchResult(p, q, matchingRule, id, n, result,idCourtCase,mismatches)
      }
    }
  }

  def createMatchResult(p: Profile, q: Profile, matchingRule: MatchingRule, id: Option[MongoId], n: Long, result: NewMatchingResult,idCourtCase: Option[Long] = None,misMatches:Int = 0): Some[MatchResult] = {
    val leftProfile = MatchingProfile(p.globalCode, p.assignee, MatchStatus.pending, None , p.categoryId)
    val rightProfile = MatchingProfile(q.globalCode, q.assignee, MatchStatus.pending, None, q.categoryId)

    val oid = id.getOrElse(MongoId(new ObjectId().toString))
    logger.debug(s"Created match result with id ${oid.id}")

//    Some(MatchResult(oid, MongoDate(new Date()), matchingRule.`type`, leftProfile, rightProfile, result, n,None,idCourtCase,0.0,0.0,misMatches))
    Some(MatchResult(oid, MongoDate(new Date()), matchingRule.`type`, leftProfile, rightProfile, result, n,None,idCourtCase,0.0,misMatches))
  }

  def convertAleles(alelles:Seq[AlleleValue],locus:Profile.Marker,locusRangeMap:NewMatchingResult.AlleleMatchRange): Seq[AlleleValue] = {
    val range = locusRangeMap.get(locus).getOrElse(AleleRange(0,99))
    alelles.map(a=>convertSingleAlele(a,range))
  }

  def convertSingleAlele(a: AlleleValue,range: AleleRange): AlleleValue={
      a match {
        case Allele(x) if x<range.min => OutOfLadderAllele(range.min,"<")
        case Allele(x) if x>range.max => OutOfLadderAllele(range.max,">")
        case OutOfLadderAllele(x,">") if x>range.max => OutOfLadderAllele(range.max,">")
        case OutOfLadderAllele(x,"<") if x<range.min => OutOfLadderAllele(range.min,"<")
        case MicroVariant(x) if toBigDecimal(x+1)<=range.min => OutOfLadderAllele(range.min,"<")
        case MicroVariant(x) if toBigDecimal(x)>range.max => OutOfLadderAllele(range.max,">")
        case _ => a
      }
  }

  def getMatchLevels(p: Profile, q: Profile, matchingRule: MatchingRule, pGenotype: Genotypification, qGenotype: Genotypification,locusRangeMap:NewMatchingResult.AlleleMatchRange): Option[Map[Profile.Marker, Stringency]] = {
    logger.trace(s"Match ${p.globalCode} against ${q.globalCode} at ${matchingRule.minimumStringency} accepting ${matchingRule.mismatchsAllowed} mismatches")

    def applyMinLocusMatch(lastMap: Option[Map[Profile.Marker, Stringency]]) = {
      lastMap.fold[Option[Map[Profile.Marker, Stringency]]](None)({ m =>
        if (m.values.count(s => s < Stringency.Mismatch && s > Stringency.ImpossibleMatch) < matchingRule.minLocusMatch) None else Some(m)
      })
    }

    val loci = pGenotype.keySet.intersect(qGenotype.keySet)
    if (loci.nonEmpty) {

      def buildMatchResult(gp: Profile.Genotypification, gq: Profile.Genotypification, minStringency: Stringency,
                           lociToMatch: Seq[Profile.Marker], mismatchesRemaining: Int, lastMap: Option[Map[Profile.Marker, Stringency]]): Option[Map[Profile.Marker, Stringency]] = {
        if (mismatchesRemaining < 0)
          None
        else
          lociToMatch match {
            case Nil => applyMinLocusMatch(lastMap)
            case locus :: ls => {
              val s = genotypeMatch(convertAleles(gp(locus),locus,locusRangeMap), convertAleles(gq(locus),locus,locusRangeMap))
              val isMismatch = s > minStringency // Note: HighStringency < MediumStringency < LowStringency
              buildMatchResult(gp, gq, minStringency,
                ls, mismatchesRemaining - (if (isMismatch) 1 else 0),
                Some(lastMap.get + (locus -> (if (isMismatch) Stringency.Mismatch else s))))
            }
          }
      }

      def buildMatchResultMixMix(gp: Profile.Genotypification, gpv: Option[Profile.Genotypification], gq: Profile.Genotypification, gqv: Option[Profile.Genotypification], minStringency: Stringency,
                                 lociToMatch: Seq[Profile.Marker], mismatchesRemaining: Int, lastMap: Option[Map[Profile.Marker, Stringency]]): Option[Map[Profile.Marker, Stringency]] = {
        if (mismatchesRemaining < 0)
          None
        else
          lociToMatch match {
            case Nil => applyMinLocusMatch(lastMap)
            case locus :: ls => {

              val v1 = gpv.flatMap { gv => gv.get(locus) }
              val v2 = gqv.flatMap { gv => gv.get(locus) }

              val s = genotypeMatchOfMixes(convertAleles(gp(locus),locus,locusRangeMap), v1, convertAleles(gq(locus),locus,locusRangeMap), v2)
              val isMismatch = s > minStringency // Note: HighStringency < MediumStringency < LowStringency
              buildMatchResultMixMix(gp, gpv, gq, gqv, minStringency,
                ls, mismatchesRemaining - (if (isMismatch) 1 else 0),
                Some(lastMap.get + (locus -> (if (isMismatch) Stringency.Mismatch else s))))
            }
          }
      }

      def buildMatchResultWithVictim(gp: Profile.Genotypification, gpv: Option[Profile.Genotypification], gq: Profile.Genotypification, gqv: Option[Profile.Genotypification], minStringency: Stringency,
                                 lociToMatch: Seq[Profile.Marker], mismatchesRemaining: Int, lastMap: Option[Map[Profile.Marker, Stringency]]): Option[Map[Profile.Marker, Stringency]] = {
        if (mismatchesRemaining < 0)
          None
        else
          lociToMatch match {
            case Nil => applyMinLocusMatch(lastMap)
            case locus :: ls => {

              val v1 = gpv.flatMap { gv => gv.get(locus) }
              val v2 = gqv.flatMap { gv => gv.get(locus) }

              val s = genotypeMatchOfMixeWithVictim(convertAleles(gp(locus),locus,locusRangeMap), v1, convertAleles(gq(locus),locus,locusRangeMap), v2)
              val isMismatch = s > minStringency // Note: HighStringency < MediumStringency < LowStringency
              buildMatchResultWithVictim(gp, gpv, gq, gqv, minStringency,
                ls, mismatchesRemaining - (if (isMismatch) 1 else 0),
                Some(lastMap.get + (locus -> (if (isMismatch) Stringency.Mismatch else s))))
            }
          }
      }
      // WARNING: THIS just picks the first associated profile

      val matchLevels = if (matchingRule.matchingAlgorithm == Algorithm.GENIS_MM) {
        val pVictimGenotype = p.labeledGenotypification.flatMap { mapOfLabeled => mapOfLabeled.values.toVector.lift(0) }
        val qVictimGenotype = q.labeledGenotypification.flatMap { mapOfLabeled => mapOfLabeled.values.toVector.lift(0) }
        buildMatchResultMixMix(pGenotype, pVictimGenotype, qGenotype, qVictimGenotype, matchingRule.minimumStringency, loci.toList, matchingRule.mismatchsAllowed, Some(Map[Profile.Marker, Stringency]()))
      } else if (matchingRule.matchingAlgorithm == Algorithm.ENFSI) {
        val pVictimGenotype = p.labeledGenotypification.flatMap { mapOfLabeled => mapOfLabeled.values.toVector.lift(0) }
        val qVictimGenotype = q.labeledGenotypification.flatMap { mapOfLabeled => mapOfLabeled.values.toVector.lift(0) }
        if (pVictimGenotype.isDefined || qVictimGenotype.isDefined) {
          buildMatchResultWithVictim(pGenotype, pVictimGenotype, qGenotype, qVictimGenotype, matchingRule.minimumStringency, loci.toList, matchingRule.mismatchsAllowed, Some(Map[Profile.Marker, Stringency]()))
        } else {
          buildMatchResult(pGenotype, qGenotype, matchingRule.minimumStringency, loci.toList, matchingRule.mismatchsAllowed, Some(Map[Profile.Marker, Stringency]()))
        }
      } else {
        None
      }

      matchLevels
    } else {
      None
    }
  }

  def getNewMatchingResult(map: Map[Profile.Marker, Stringency], q: Profile, matchingRule: MatchingRule, pGenotype: Genotypification, qGenotype: Genotypification,locusRangeMap:NewMatchingResult.AlleleMatchRange ): NewMatchingResult = {
    val worstMatchLevel = map.values.filter { s => s < Stringency.Mismatch }.max

    logger.trace(q._id + " " + q.categoryId + " " + worstMatchLevel + " against " + matchingRule.minimumStringency)
    val groups = List(MatchGroup("Aut", worstMatchLevel, map))
    val rangeLocus = map.keys.filter(key=>locusRangeMap.contains(key)).map(key=> key -> locusRangeMap.get(key).getOrElse(AleleRange(0,99))).toMap
    val rangeLocusOpt = rangeLocus.size match {
      case 0  => None
      case _ => Some(rangeLocus)
    }
    val (leftPonderation, rightPonderation) = sharedAllelePonderation(pGenotype, qGenotype)
    NewMatchingResult(
      worstMatchLevel,
      map,
      map.size,
      q.categoryId,
      leftPonderation,
      rightPonderation,
      matchingRule.matchingAlgorithm,
      rangeLocusOpt
    )

  }

  def performMatch(config: MtConfiguration, p: Profile, q: Profile, matchingRule: MatchingRule, id: Option[MongoId] = None, n: Long,locusRangeMap:NewMatchingResult.AlleleMatchRange = Map.empty,idCourtCase: Option[Long] = None): Option[MatchResult] = {
    if (matchingRule.mitochondrial) profileMtMatch(config, p, q, matchingRule, None, n)
    else profileMatch(p, q, matchingRule, None, n,locusRangeMap,idCourtCase)
  }
  def convertProfileWithConvertedOutOfLadderAlleles(p:Profile,locusRangeMap:NewMatchingResult.AlleleMatchRange):Profile = {
    if(p.genotypification.get(1).isEmpty){
      return p;
    }

    val pGenoCopy = p.genotypification.map(x => {
      if(x._1 == 1){
        (x._1,x._2.map(g => (g._1,convertAleles(g._2,g._1,locusRangeMap).toList)))
      }else{
        x
      }
    })

    p.copy(genotypification = pGenoCopy)
  }
  def convertProfileWithoutAcceptedLocus(p:Profile,aceptedLocusList: Seq[String] = Nil):Profile = {
    if(p.genotypification.get(1).isEmpty){
      return p;
    }
    val genotypification = p.genotypification.get(1).get
    val markers = genotypification.map(x => x._1.toString).toSet
    val markersIntersection = markers.intersect(aceptedLocusList.toSet)
    if(markersIntersection.isEmpty){
      return p;
    }

    val pGenoCopy = p.genotypification.map(x => {
      if(x._1 == 1){
        (x._1,x._2.filter(g => !markersIntersection.contains(g._1)))
      }else{
        x
      }
    })

    p.copy(genotypification = pGenoCopy)
  }
  def getMtProfile(genogram: scala.Seq[Individual],profiles:scala.Seq[Profile]):Option[Profile] = {
    recursiveGetMtProfile(genogram.find(_.unknown),genogram,profiles)
      .flatMap(_.globalCode)
      .flatMap(globalCode => profiles.find(_.globalCode == globalCode))
  }
  def isMitocondrial(globalCode: SampleCode,profiles:scala.Seq[Profile]):Boolean = {
    val MITOCONDRIAL_TYPE = 4
    profiles.find(_.globalCode == globalCode).exists(p => p.genotypification.contains(MITOCONDRIAL_TYPE))
  }
  def getMtSibling(individual:Individual,genogram: scala.Seq[Individual],profiles: scala.Seq[Profile]):Option[Individual] = {
    if(individual.idMother.isDefined){
      genogram.find(x => x.globalCode.isDefined &&
        x.idMother.contains(individual.idMother.get)
        && isMitocondrial(x.globalCode.get,profiles))
    }else{
      None
    }
  }
  def recursiveGetMtProfile(node: Option[Individual], genogram: scala.Seq[Individual],
                            profiles:scala.Seq[Profile]):Option[Individual] = {

    val isMt = node.flatMap(_.globalCode).exists(isMitocondrial(_,profiles))
    if(isMt){
      node
    }else{
      if(node.isEmpty){
        None
      }else{
        val sibling:Option[Individual] = getMtSibling(node.get,genogram,profiles)
        if(sibling.isDefined){
          sibling
        }else{
          if(node.get.idMother.isEmpty){
            None
          }else{
            recursiveGetMtProfile(genogram.find(_.alias == node.get.idMother.get),genogram,profiles)
          }
        }
      }
    }
  }

}
