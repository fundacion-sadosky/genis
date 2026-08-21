package pedigree

import kits.AnalysisType
import matching.{MatchingAlgorithm, NewMatchingResult}
import pedigree.PedigreeMatchingAlgorithm.extractMarker
import profile.Profile.Marker
import profile._
import types.SampleCode
import play.api.Logger

import scala.collection.mutable.ArrayBuffer
import scala.language.{higherKinds, postfixOps}
import scalax.collection.Graph
import scalax.collection.GraphEdge.{DiEdge, _}
import scalax.collection.GraphPredef._

object BayesianNetwork {
  type MutationModelData = (
    MutationModelParameter,
      List[MutationModelKi],
      MutationModel
    )
  type FrequencyTable = Map[String, Map[Double, Double]]
  type Matrix = Iterator[Array[Double]]
  type Linkage = Map[Marker, (Marker, Double)]

  val name = "pedigree"
  val zero = BigDecimal.valueOf(0)
  val logger = Logger(this.getClass())
  val attrs = Array("m", "p")

  def getNFromTable(ft:BayesianNetwork.FrequencyTable):
    Map[String, List[Double]]= {
    ft.map(
      x => (
        x._1,
        x._2
          .map(y => y._1)
          .toList
      )
    )
  }
  def getMarkersFromCpts(cpts2:Array[PlainCPT2]):Set[String] = {
    val cpts = cpts2.map(
      plainCpt2 => PlainCPT(
        plainCpt2.header,
        plainCpt2.matrix.iterator,
        plainCpt2.matrix.length
      )
    )
    getMarkersFromCpts(cpts)
  }
  def getMarkersFromCpts(cpts:Array[PlainCPT]):Set[String] = {
    cpts
      .filter(!_.matrix.isEmpty)
      .map(
        cpt => extractMarker(cpt.header.head)
      )
      .toSet
  }
  def buildHypothesis2Genogram(genogram: Array[Individual]): Array[Individual] = {
    genogram.find(_.unknown).map(_.alias) match {
      case None => genogram
      case Some(unknownAlias) =>
        genogram.map { ind =>
          if (ind.alias == unknownAlias) {
            ind.copy(idFather = None, idMother = None)
          } else {
            val newFather =
              if (ind.idFather.contains(unknownAlias)) None else ind.idFather
            val newMother =
              if (ind.idMother.contains(unknownAlias)) None else ind.idMother
            ind.copy(idFather = newFather, idMother = newMother)
          }
        }
    }
  }

  def unknownHasChildren(genogram: Array[Individual]): Boolean = {
    genogram.find(_.unknown).map(_.alias).exists {
      unknownAlias =>
        genogram.exists { ind =>
          val isChildOfUnknown = ind.idFather.contains(unknownAlias) || ind.idMother.contains(unknownAlias)
          isChildOfUnknown && ind.globalCode.isDefined
        }
    }
  }

  def filterOutUnknownOwnCpts(
    cpts: Array[PlainCPT],
    unknown: String
  ): Array[PlainCPT] = {
    cpts.filterNot(
      cpt => cpt.header.exists(_.startsWith(s"${unknown}_"))
    )
  }

  def extendNWithObservedAlleles(
    n: Map[String, List[Double]],
    profiles: Array[Profile],
    genogram: Array[Individual],
    analysisType: AnalysisType
  ): Map[String, List[Double]] = {
    val observedByMarker: Map[String, Set[Double]] = genogram
      .flatMap(_.globalCode)
      .distinct
      .flatMap { code =>
        n.keys.flatMap { marker =>
          getAlleles(marker, Some(code), profiles).toList.flatten.map(marker -> _)
        }
      }
      .groupBy(_._1)
      .map { case (marker, entries) => marker -> entries.map(_._2).toSet }
    observedByMarker.foldLeft(n) {
      case (acc, (marker, observedAlleles)) =>
        val current = acc.getOrElse(marker, Nil)
        val missing = observedAlleles -- current.toSet
        if (missing.isEmpty) acc else acc + (marker -> (current ++ missing).sorted)
    }
  }

  def getGenotypificationExtras(
    profiles: Array[Profile],
    genogram: Array[Individual],
    normalizedFrequencyTable: FrequencyTable,
    analysisType: AnalysisType,
    linkage: Linkage,
    markers: Array[String],
    verbose: Boolean = false,
    locusRangeMap: NewMatchingResult.AlleleMatchRange = Map.empty,
    mutationModelType: Option[Long] = None,
    mutationModelData: Option[List[MutationModelData]] = None,
    n: Map[String, List[Double]] = Map.empty
  ): (Option[Array[(String, PlainCPT)]], Array[(String, PlainCPT)]) = {
    val genotypificationH2: Option[Array[(String, PlainCPT)]] = if (unknownHasChildren(genogram)) {
      val genogramH2 = buildHypothesis2Genogram(genogram)
      val g2 = getGenotypificationWithMarkers(
        profiles,
        genogramH2,
        normalizedFrequencyTable,
        analysisType,
        linkage,
        Some(markers),
        verbose,
        locusRangeMap,
        mutationModelType,
        mutationModelData,
        n
      )
      Some(g2)
    } else {
      None
    }
    val genotypificationWithMarkers = getGenotypificationWithMarkers(
      profiles,
      genogram,
      normalizedFrequencyTable,
      analysisType,
      linkage,
      Some(markers),
      verbose,
      locusRangeMap,
      mutationModelType,
      mutationModelData,
      n
    )
    (genotypificationH2, genotypificationWithMarkers)
  }

  def calculateProbability(
    profiles: Array[Profile],
    genogram: Array[Individual],
    frequencyTable: FrequencyTable,
    analysisType: AnalysisType,
    linkage: Linkage,
    verbose: Boolean = false,
    mutationModelType: Option[Long] = None,
    mutationModelData: Option[List[MutationModelData]] = None,
    seenAlleles: Map[String, List[Double]] = Map.empty,
    locusRangeMap:NewMatchingResult.AlleleMatchRange = Map.empty,
    maxExclusionsAllowed: Int = 0
  ): (Double, Map[String, MarkerLRDetail]) = {
    val nBase = if(seenAlleles.isEmpty) {
      getNFromTable(frequencyTable)
    } else {
      seenAlleles
    }
    val n = extendNWithObservedAlleles(nBase, profiles, genogram, analysisType)
    val starQueryProfilesTime = System.currentTimeMillis()
    var queryProfiles = getQueryProfiles(
      profiles,
      genogram,
      analysisType,
      frequencyTable
    )
    val endQueryProfilesTime = System.currentTimeMillis()
    val markers = queryProfiles.values.head.keys.toArray
    val linkageMarkers = linkage.keySet.union(linkage.values.map(_._1).toSet)
    val starNormalizedFrequencyTableTime = System.currentTimeMillis()
    val normalizedFrequencyTable = getNormalizedFrequencyTable(frequencyTable)
    val endNormalizedFrequencyTableTime = System.currentTimeMillis()
    val startGenotypificationTime = System.currentTimeMillis()
    val genotypification = getGenotypification(
      profiles,
      genogram,
      normalizedFrequencyTable,
      analysisType,
      linkage,
      Some(markers),
      verbose,
      locusRangeMap,
      mutationModelType,
      mutationModelData,
      n
    )
    val endGenotypificationTime = System.currentTimeMillis()
    val strictGenotypification = if (mutationModelType.isEmpty) {
      genotypification
    } else {
      getGenotypification(
        profiles,
        genogram,
        normalizedFrequencyTable,
        analysisType,
        linkage,
        Some(markers),
        verbose,
        locusRangeMap,
        None,
        None,
        n
      )
    }
    val (genotypificationH2, genotypificationWithMarkers) = getGenotypificationExtras(
      profiles, genogram, normalizedFrequencyTable, analysisType, linkage, markers,
      verbose, locusRangeMap, mutationModelType, mutationModelData, n
    )
    val startLRTime = System.currentTimeMillis()
    val genoMarkers = getMarkersFromCpts(genotypification).toArray
    val lr = calculateLR(
      queryProfiles
        .map(
          x => (
            x._1,
            x._2.filter(
              y => genoMarkers.contains(y._1) || linkageMarkers.contains(y._1)
            )
          )
        ),
      genoMarkers
        .toSet
        .union(linkageMarkers.map(_.toString))
        .toArray,
      genotypification,
      normalizedFrequencyTable,
      analysisType,
      linkage,
      mutationModelType,
      mutationModelData,
      n,
      maxExclusionsAllowed,
      strictGenotypification,
      genotypificationH2,
      genotypificationWithMarkers
    )
    val endLRTime = System.currentTimeMillis()
    if (verbose) {
      logger.info(s"--- GetLR: ${endLRTime-startLRTime} ---")
      logger.info(s"--- LR: ${lr._1} ---")
    }
    (lr._1, lr._4)
  }
  def getLR(
    unknowns: Array[String],
    profile: Profile,
    frequencyTable: FrequencyTable,
    analysisType: AnalysisType,
    genotypification: Array[PlainCPT],
    mutationModelType: Option[Long] = None,
    mutationModelData: Option[List[MutationModelData]] = None,
    n: Map[String,List[Double]] = Map.empty,
    maxExclusionsAllowed: Int = 0,
    strictGenotypification: Array[PlainCPT] = Array.empty,
    genotypificationH2: Option[Array[(String, PlainCPT)]] = None,
    genotypificationWithMarkers: Array[(String, PlainCPT)] = Array.empty
  ): (Double, String, List[String], Map[String, MarkerLRDetail]) = {
    val linkeage:Linkage = Map.empty
    val unknownAlias = unknowns.head
    val queryProfiles = Map(
      unknownAlias -> getQueryProfileAlleles(
        profile, analysisType, frequencyTable
      )
    )
    val markers = queryProfiles.values.head.keys.toArray
    val normalizedFrequencyTable = getNormalizedFrequencyTable(frequencyTable)
    val lr = calculateLR(
      queryProfiles,
      markers,
      genotypification,
      normalizedFrequencyTable,
      analysisType,
      linkeage,
      mutationModelType,
      mutationModelData,
      n,
      maxExclusionsAllowed,
      strictGenotypification,
      genotypificationH2,
      genotypificationWithMarkers
    )
    logger.info(
      s"--- Profile: ${
        profile.globalCode
      } - ${
        profile .internalSampleCode
      }"
    )
    logger.info(s"--- LR: ${lr._1} ---")
    lr
  }

  private def calculateLR(
    queryProfiles: Map[String, Map[Profile.Marker, Array[Double]]],
    markers: Array[Profile.Marker],
    genotypification: Array[PlainCPT],
    normalizedFrequencyTable: FrequencyTable,
    analysisType: AnalysisType,
    linkage: Linkage,
    mutationModelType: Option[Long] = None,
    mutationModelData: Option[List[MutationModelData]] = None,
    n: Map[String, List[Double]] = Map.empty,
    maxExclusionsAllowed: Int = 0,
    strictGenotypification: Array[PlainCPT] = Array.empty,
    genotypificationH2: Option[Array[(String, PlainCPT)]] = None,
    genotypificationWithMarkers: Array[(String, PlainCPT)] = Array.empty
  ) : (Double, String, List[String], Map[String, MarkerLRDetail]) = {
    val unknown = queryProfiles.keys.head
    val markersToFilter = getMarkersToFilter(
      markers,
      queryProfiles,
      genotypification,
      n
    )
    val messageInit = s"""${
        "El valor calculado de LR es aproximado debido a que se detectaron "
      }${
        "alelos no vistos que no estan en el genotipo de la familia,"
      }"""
    var message = ""
    var messageMarkersFitered = ""
    val genotypificationFiltered = filterGenotypification(
      genotypification,
      markersToFilter
    )
    val queryProfilesFiltered = filterQueryProfile(queryProfiles, markersToFilter)
    if (markersToFilter.length > 0) {
      messageMarkersFitered += s"""${
        " se descartaron el o los marcadores que lo/s contenían y que no "
      }${
        "compartían ningún alelo con la familia "
      }"""
    }
    val genotypificationForQuery = genotypificationFiltered.map { cpt =>
      val arr = cpt.matrix.toArray
      cpt.matrix = arr.iterator
      new PlainCPT(cpt.header, arr.iterator, arr.length)
    }
    val genotypificationH2Filtered: Option[Array[(String, PlainCPT)]] = genotypificationH2.map(
      g => g.filterNot { case (marker, _) => markersToFilter.contains(marker) }
    )
    val genotypificationH2ForQuery: Option[Array[(String, PlainCPT)]] = genotypificationH2Filtered.map(
      _.map { case (marker, cpt) =>
        val arr = cpt.matrix.toArray
        cpt.matrix = arr.iterator
        marker -> new PlainCPT(cpt.header, arr.iterator, arr.length)
      }
    )
    val genotypificationWithMarkersFiltered = genotypificationWithMarkers.filterNot {
      case (marker, _) => markersToFilter.contains(marker)
    }
    val genotypificationWithMarkersForQuery = genotypificationWithMarkersFiltered.map {
      case (marker, cpt) =>
        val arr = cpt.matrix.toArray
        cpt.matrix = arr.iterator
        marker -> new PlainCPT(cpt.header, arr.iterator, arr.length)
    }
    val queryProbabilityLog = getQueryProbabilityLog(
      markers,
      queryProfilesFiltered,
      genotypificationForQuery,
      analysisType,
      normalizedFrequencyTable,
      linkage,
      mutationModelType,
      mutationModelData,
      n,
      maxExclusionsAllowed,
      strictGenotypification,
      genotypificationH2ForQuery,
      genotypificationWithMarkersForQuery
    )
    val toleratedMarkers = queryProbabilityLog._3.toArray
    val genotypeProbabilityLog = getGenotypeProbabilityLog(
      filterQueryProfile(queryProfilesFiltered, toleratedMarkers),
      normalizedFrequencyTable
    )
    val lr = if (linkage.nonEmpty) {
      val evidenceGenotypification = filterGenotypification(
        genotypificationFiltered,
        toleratedMarkers
      )
      val evidenceProbabilityLogLinked = getEvidenceProbabilityLog(evidenceGenotypification)
      if (evidenceProbabilityLogLinked.isEmpty || genotypeProbabilityLog.isEmpty || queryProbabilityLog._1.isEmpty) {
        0.0
      } else {
        math.exp(queryProbabilityLog._1.get - (evidenceProbabilityLogLinked.get + genotypeProbabilityLog.get))
      }
    } else if (queryProbabilityLog._4.isEmpty) {
      0.0
    } else {
      queryProbabilityLog._4.values.map(_.lr).product
    }

    if (!messageMarkersFitered.isEmpty || !queryProbabilityLog._2.isEmpty) {
      if (!messageMarkersFitered.isEmpty) {
        message = messageInit + messageMarkersFitered
        if (!queryProbabilityLog._2.isEmpty) {
          message += " y " + queryProbabilityLog._2
        }
      } else {
        message = messageInit + queryProbabilityLog._2
      }
    }
    (lr, message, queryProbabilityLog._3, queryProbabilityLog._4)
  }

  private def getMarkersToFilter(
    markers: Array[Profile.Marker],
    queryProfiles: Map[String, Map[Profile.Marker, Array[Double]]],
    genotypification: Array[PlainCPT],
    n: Map[String,List[Double]]
  ): Array[Profile.Marker] = {
    val unknown = queryProfiles.keys.head
    var markersToFilter: Array[Profile.Marker] = Array.empty
    markers.foreach {
      marker => {
        if (queryProfiles(unknown).contains(marker)) {
          val alleles = queryProfiles(unknown)(marker)
          val noAlleleInN = ! isAlleleInN(marker, alleles, n)
          if (noAlleleInN) {
            markersToFilter = markersToFilter ++ List(marker)
          }
        }
      }
    }
    markersToFilter
  }

  private def isActbp2Se33Alias(name: String): Boolean = {
    val upper = name.toUpperCase
    upper.contains("ACTBP2") || upper.contains("SE33")
  }

  private def allelesInN(marker: String, n: Map[String, List[Double]]): List[Double] = {
    n.get(marker).getOrElse(
      if (isActbp2Se33Alias(marker)) {
        n.collectFirst { case (k, v) if isActbp2Se33Alias(k) => v }.getOrElse(List.empty)
      } else {
        List.empty
      }
    )
  }

  private def isAlleleInN(
    marker: String,
    alelles: Array[Double],
    n: Map[String,List[Double]]
  ) : Boolean = {
    alelles.forall(
      allelesInN(marker, n)
        .contains(_)
    )
  }

  private def getIncompleteMarkers(
    markers: Array[Profile.Marker],
    queryProfiles: Map[String, Map[Profile.Marker, Array[Double]]],
    genotypification: Array[PlainCPT],
    n: Map[String,List[Double]]
  ): Array[Profile.Marker] = {
    val unknown = queryProfiles.keys.head
    var incompleteMarkers: Array[Profile.Marker] = Array.empty
    markers.foreach {
      marker => {
        val pVariable = s"${unknown}_${marker}_p"
        val mVariable = s"${unknown}_${marker}_m"
        if (queryProfiles(unknown).contains(marker)) {
          val alleles = queryProfiles(unknown)(marker)
          lazy val isAny =  isAnyAlellesInGenotipeAndN(
            marker,
            alleles,
            pVariable,
            mVariable,
            genotypification,
            n
          )
          lazy val isAll = isAllAlellesInGenotipeAndN(
            marker,
            alleles,
            pVariable,
            mVariable,
            genotypification,
            n
          )
          if (isAny && !isAll) {
            incompleteMarkers = incompleteMarkers ++ List(marker)
          }
        }
      }
    }
    incompleteMarkers
  }

  def filterGenotypification(
    genotypification: Array[PlainCPT],
    markersToFilter: Array[Profile.Marker]
  ) : Array[PlainCPT] = {
    var genotipeToReturn = genotypification
    markersToFilter.foreach {
      marker => {
        genotipeToReturn = genotipeToReturn
          .filterNot(_.header(0).contains(marker))
      }
    }
    genotipeToReturn
  }

  def filterQueryProfile(
    queryProfiles: Map[String, Map[Profile.Marker, Array[Double]]],
    markersToFilter: Array[Profile.Marker]
  ) : Map[String, Map[Profile.Marker, Array[Double]]]= {
    val queryProfilesFilterer = queryProfiles
    val unknown = queryProfilesFilterer.keys.head
    var unknownMarkers = queryProfilesFilterer.get(unknown).get
    markersToFilter.foreach {
      marker => {
        unknownMarkers = unknownMarkers.filterNot {
          case (unknownMarker, _) => unknownMarker.equals(marker)
        }
      }
    }
    queryProfilesFilterer.updated(unknown, unknownMarkers)
  }

  def getNormalizedFrequencyTable(frequencyTable: FrequencyTable):
    FrequencyTable = {
    frequencyTable.map {
      case (marker, alleles) => {
        val sum = alleles.filterKeys(_ != -1.0).values.sum
        if (sum != 1) {
          (
            marker,
            alleles.map {
              case (allele, frequency) => (allele, frequency / sum)
            }
          )
        } else {
          (marker, alleles)
        }
      }
    }
  }

  def transformAlleleValues(alleles: Array[AlleleValue]): Array[Double] = {
    alleles.flatMap {
      case Allele(v) => Some(v.toDouble)
      case OutOfLadderAllele(_,_) => Some(-1.0)
      case MicroVariant(_) => Some(-1.0)
      case _ => None
    }
  }

  def getQueryProfileAlleles(
    profile: Profile,
    analysisType: AnalysisType,
    frequencyTable: FrequencyTable
  ): Map[Profile.Marker, Array[Double]] = {
    val strs = profile
      .genotypification
      .getOrElse(analysisType.id, Map.empty)
    val frequencyTableMarkers = frequencyTable.keySet
    val strsFiltered = strs.filter(
      str => frequencyTableMarkers.contains(str._1)
    )
    strsFiltered.map {
      case (marker, alleles) => {
        marker -> transformAlleleValues(alleles.distinct.toArray)
      }
    }
  }

  def getGenotypification(
    profiles: Array[Profile],
    genogram: Array[Individual],
    frequencyTable: FrequencyTable,
    analysisType: AnalysisType,
    linkage: Linkage,
    ukMarkers: Option[Array[String]] = None,
    verbose: Boolean = false,
    locusRangeMap:NewMatchingResult.AlleleMatchRange = Map.empty,
    mutationModelType: Option[Long]=None,
    mutationModelData: Option[List[MutationModelData]] = None,
    n: Map[String,List[Double]] = Map.empty
  ):Array[PlainCPT] = {
    val markers = ukMarkers.fold(frequencyTable.keys.toArray)(identity)
    val startGeneratingGraphTime = System.currentTimeMillis()
    val (variablesMap, graph) = generateGraph(
      profiles
        .map(
          p => MatchingAlgorithm
            .convertProfileWithConvertedOutOfLadderAlleles(p, locusRangeMap)
        ),
      markers,
      genogram,
      linkage,
      mutationModelType
    )
    val endGeneratingGraphTime = System.currentTimeMillis()
    val startSubgraphsGraphTime = System.currentTimeMillis()
    val subgraphs = getSubgraphs(graph)
    val endSubgraphsGraphTime = System.currentTimeMillis()

    val unknown: String = genogram.find(_.unknown) match {
      case None => throw new PedigreeNotHavingUnknownException()
      case Some(x) => x.alias.text
    }
    var cont = 0

    val geno = subgraphs
      .zipWithIndex
      .flatMap {
        case (subgraph, index) =>
          val variables = variablesMap.filter {
            case (n, _) => subgraph.nodes.exists(_.toOuter == n)
          }

          val startGeneratingCPTsTime = System.currentTimeMillis()
          val cpts = generateCPTs(
            variables,
            subgraph,
            frequencyTable,
            linkage,
            mutationModelType,
            mutationModelData,
            n,
            genogram
          )

          val endGeneratingCPTsTime = System.currentTimeMillis()

          val startPrunningCPTsTime = System.currentTimeMillis()
          val prunnedCPTs = pruneCPTs(variables, subgraph, cpts)
          val endPrunningCPTsTime = System.currentTimeMillis()

          if (prunnedCPTs.nonEmpty) {
            val prunnedVariables = prunnedCPTs.map(_.variable.name)
            val prunnedGraph = subgraph filter subgraph.having(node = n => prunnedVariables.contains(n.toOuter))

            val queries = getQueryVariables(variables)
            val startVariableEliminationTime = System.currentTimeMillis()
            val ve = variableElimination(unknown, prunnedCPTs.map(_.getPlain()), queries, prunnedGraph, verbose)
            val endVariableEliminationTime = System.currentTimeMillis()
            Some(ve)
          } else {
            None
          }
      }
      .filter(_.matrix.nonEmpty)
    geno
  }

  def getGenotypificationWithMarkers(
    profiles: Array[Profile],
    genogram: Array[Individual],
    frequencyTable: FrequencyTable,
    analysisType: AnalysisType,
    linkage: Linkage,
    ukMarkers: Option[Array[String]] = None,
    verbose: Boolean = false,
    locusRangeMap:NewMatchingResult.AlleleMatchRange = Map.empty,
    mutationModelType: Option[Long]=None,
    mutationModelData: Option[List[MutationModelData]] = None,
    n: Map[String,List[Double]] = Map.empty
  ): Array[(String, PlainCPT)] = {
    val markers = ukMarkers.fold(frequencyTable.keys.toArray)(identity)
    val (variablesMap, graph) = generateGraph(
      profiles
        .map(
          p => MatchingAlgorithm
            .convertProfileWithConvertedOutOfLadderAlleles(p, locusRangeMap)
        ),
      markers,
      genogram,
      linkage,
      mutationModelType
    )
    val subgraphs = getSubgraphs(graph)
    val unknown: String = genogram.find(_.unknown) match {
      case None => throw new PedigreeNotHavingUnknownException()
      case Some(x) => x.alias.text
    }

    subgraphs.flatMap {
      subgraph =>
        val variables = variablesMap.filter {
          case (n, _) => subgraph.nodes.exists(_.toOuter == n)
        }
        val subgraphMarker = variables.values.headOption.map(_.marker)
        val cpts = generateCPTs(
          variables, subgraph, frequencyTable, linkage,
          mutationModelType, mutationModelData, n, genogram
        )
        val prunnedCPTs = pruneCPTs(variables, subgraph, cpts)
        if (prunnedCPTs.nonEmpty && subgraphMarker.isDefined) {
          val prunnedVariables = prunnedCPTs.map(_.variable.name)
          val prunnedGraph = subgraph filter subgraph.having(node = n => prunnedVariables.contains(n.toOuter))
          val queries = getQueryVariables(variables)
          val ve = variableElimination(unknown, prunnedCPTs.map(_.getPlain()), queries, prunnedGraph, verbose)
          if (ve.matrix.nonEmpty) Some(subgraphMarker.get -> ve) else None
        } else {
          None
        }
    }
  }

  def getQueryProbabilityLog(
    markers: Array[String],
    queryProfiles: Map[String, Map[Marker, Array[Double]]],
    genotypification: Array[PlainCPT],
    analysisType: AnalysisType,
    frequencyTable: FrequencyTable,
    linkage: Linkage, mutationModelType: Option[Long] = None,
    mutationModelData: Option[List[MutationModelData]] = None,
    n: Map[String, List[Double]] = Map.empty,
    maxExclusionsAllowed: Int = 0,
    strictGenotypification: Array[PlainCPT] = Array.empty,
    genotypificationH2: Option[Array[(String, PlainCPT)]] = None,
    genotypificationWithMarkers: Array[(String, PlainCPT)] = Array.empty
  ): (Option[Double], String, List[String], Map[String, MarkerLRDetail], Option[Double]) = {
    var cpts = genotypification
    var message = ""
    val unknown = queryProfiles.keys.head
    val markerQueryLog = scala.collection.mutable.Map[String, Double]()

    val exclusionDetectionCpts = if (strictGenotypification.nonEmpty) strictGenotypification else genotypification
    val exclusionMarkers: Set[String] = markers.filter {
      marker =>
        val pVariable = s"${unknown}_${marker}_p"
        val mVariable = s"${unknown}_${marker}_m"
        queryProfiles(unknown).contains(marker) && {
          val alleles = queryProfiles(unknown)(marker)
          val dependentCPTs = exclusionDetectionCpts.filter(
            cpt => cpt.header.contains(pVariable) ||
              cpt.header.contains(mVariable)
          )
          if (dependentCPTs.isEmpty) {
            false
          } else {
            val header = Array(pVariable, mVariable) :+ "Probability"
            val variable = Variable(
              "",
              marker,
              if (alleles.length > 1) { VariableKind.Heterocygote }
                else { VariableKind.Homocygote }
            )
            val matrix = generatePermutations(
              Array(alleles, alleles),
              header,
              variable,
              frequencyTable,
              linkage,
              None,
              None,
              n
            )
            val ukCPT = new PlainCPT(header, matrix.iterator, matrix.size)
            val dependentCPTsSnapshot = dependentCPTs.map { cpt =>
              val arr = cpt.matrix.toArray
              cpt.matrix = arr.iterator
              arr
            }
            val product = prodFactor(unknown, ukCPT +: dependentCPTs)
            dependentCPTs.zip(dependentCPTsSnapshot).foreach {
              case (cpt, arr) => cpt.matrix = arr.iterator
            }
            extractMatrixFromPlainCPT(product).isEmpty
          }
        }
    }.toSet

    val unexplainableExclusions: Set[String] = exclusionMarkers.filter { marker =>
      if (mutationModelType.isEmpty) {
        true
      } else {
        val alleles = queryProfiles(unknown)(marker)
        val pVariable = s"${unknown}_${marker}_p"
        val mVariable = s"${unknown}_${marker}_m"
        val dependentCPTs = genotypification.filter(
          cpt => cpt.header.contains(pVariable) || cpt.header.contains(mVariable)
        )
        if (dependentCPTs.isEmpty) {
          true
        } else {
          val header = Array(pVariable, mVariable) :+ "Probability"
          val variable = Variable(
            "",
            marker,
            if (alleles.length > 1) { VariableKind.Heterocygote }
              else { VariableKind.Homocygote }
          )
          val matrix = generatePermutations(
            Array(alleles, alleles),
            header,
            variable,
            frequencyTable,
            linkage,
            mutationModelType,
            mutationModelData,
            n
          )
          val ukCPT = new PlainCPT(header, matrix.iterator, matrix.size)
          val dependentCPTsSnapshot = dependentCPTs.map { cpt =>
            val arr = cpt.matrix.toArray
            cpt.matrix = arr.iterator
            arr
          }
          val product = prodFactor(unknown, ukCPT +: dependentCPTs)
          dependentCPTs.zip(dependentCPTsSnapshot).foreach {
            case (cpt, arr) => cpt.matrix = arr.iterator
          }
          extractMatrixFromPlainCPT(product).isEmpty
        }
      }
    }

    val toleratedExclusions: Set[String] =
      if (unexplainableExclusions.size <= maxExclusionsAllowed) unexplainableExclusions else Set.empty

    logger.info(
      s"--- Tolerancia exclusiones: unknown=$unknown maxExclusionsAllowed=$maxExclusionsAllowed " +
      s"exclusionMarkers=${exclusionMarkers.mkString(",")} " +
      s"noExplicablesPorModelo=${unexplainableExclusions.mkString(",")} " +
      s"toleradas=${toleratedExclusions.mkString(",")} ---"
    )

    val markerEvidenceLog: Map[String, Double] = markers.flatMap {
      marker =>
        if (!queryProfiles(unknown).contains(marker)) {
          None
        } else {
          val pVariable = s"${unknown}_${marker}_p"
          val mVariable = s"${unknown}_${marker}_m"
          val rawFamilyCpts = if (genotypificationWithMarkers.nonEmpty) {
            genotypificationWithMarkers.collect { case (m, cpt) if m == marker => cpt }
          } else {
            genotypification.filter(
              cpt => cpt.header.contains(pVariable) || cpt.header.contains(mVariable)
            )
          }
          val cptSums = rawFamilyCpts.map(cpt => extractMatrixFromPlainCPT(cpt).map(_.last).sum)
          val logSum = if (cptSums.isEmpty) {
            Some(0d)
          } else {
            cptSums.foldLeft(Option(0d)) {
              (acc, s) => acc match {
                case Some(x) if s > 0 => Some(x + math.log(s))
                case _ => None
              }
            }
          }
          logSum.map(marker -> _)
        }
    }.toMap

    markers.foreach {
      marker => {
        val pVariable = s"${unknown}_${marker}_p"
        val mVariable = s"${unknown}_${marker}_m"
        if (queryProfiles(unknown).contains(marker)) {
          val alleles = queryProfiles(unknown)(marker)
          val residualCptsForMarker = genotypificationWithMarkers.collect {
            case (m, cpt) if m == marker &&
              !cpt.header.contains(pVariable) &&
              !cpt.header.contains(mVariable) => cpt
          }
          val residualLogSum: Double = residualCptsForMarker
            .map(cpt => extractMatrixFromPlainCPT(cpt).map(_.last).sum)
            .filter(_ > 0)
            .map(math.log)
            .sum
          if (toleratedExclusions.contains(marker)) {
            val dependentCPTs = cpts.filter(
              cpt => cpt.header.contains(pVariable) ||
                cpt.header.contains(mVariable)
            )
            cpts = cpts diff dependentCPTs
          } else if (
            mutationModelType.isEmpty ||
            isAllAlellesInGenotipeAndN(
              marker, alleles, pVariable, mVariable, genotypification, n
            ) ||
            (isAlleleInN(marker, alleles, n) &&
              genotypification.exists(
                cpt => cpt.header.contains(pVariable) ||
                  cpt.header.contains(mVariable)
              ))
          ) {
            val header = Array(pVariable, mVariable) :+ "Probability"
            val variable = Variable(
              "",
              marker,
              if (alleles.length > 1) { VariableKind.Heterocygote }
                else { VariableKind.Homocygote }
            )
            val matrix = generatePermutations(
              Array(alleles, alleles),
              header,
              variable,
              frequencyTable,
              linkage,
              mutationModelType,
              mutationModelData,
              n
            )
            val ukCPT = new PlainCPT(header, matrix.iterator, matrix.size)
            val dependentCPTs = cpts.filter(
              cpt => cpt.header.contains(pVariable) ||
                cpt.header.contains(mVariable)
            )
            val product = prodFactor(unknown, ukCPT +: dependentCPTs)
            cpts = (cpts diff dependentCPTs) :+ product
            val productRows = extractMatrixFromPlainCPT(product)
            if (productRows.nonEmpty) {
              markerQueryLog(marker) = math.log(productRows.map(_.last).sum) + residualLogSum
            }
          } else {
            val alelleInGenotypeOpt = getAlelleInGenotype(
              marker, alleles, pVariable, mVariable, genotypification
            )
            val dependentCPTs = cpts.filter(
              cpt => cpt.header.contains(pVariable) ||
                cpt.header.contains(mVariable)
            )
            val header = Array(pVariable, mVariable) :+ "Probability"
            val probability = alelleInGenotypeOpt match {
              case Some(alelleInGenotype) =>
                val otherAllele = alleles.find(_ != alelleInGenotype).getOrElse(alelleInGenotype)
                val otherAlleleFrequency = getFrequency(otherAllele, marker, frequencyTable)
                val probabilityPVariable : Double = getProbabilityOf(
                  pVariable, alelleInGenotype, dependentCPTs
                )
                val probabilityMVariable : Double = getProbabilityOf(
                  mVariable, alelleInGenotype, dependentCPTs
                )
                (probabilityMVariable * otherAlleleFrequency) +
                  (probabilityPVariable * otherAlleleFrequency)
              case None =>
                val freq0 = getFrequency(alleles(0), marker, frequencyTable)
                val freq1 = if (alleles.length > 1) getFrequency(alleles(1), marker, frequencyTable) else freq0
                freq0 * freq1
            }
            var x : Array[Double] = Array.empty
            x = x ++ alleles :+ probability

            val matrixResult = ArrayBuffer(x)
            val cptResult = new PlainCPT(
              header,
              matrixResult.iterator,
              matrixResult.size
            )
            cpts = (cpts diff dependentCPTs) :+ cptResult
            if (probability > 0) {
              markerQueryLog(marker) = math.log(probability) + residualLogSum
            }

            if (message.isEmpty) {
              message =
                """ se utilizó la minima probabilidad asociada al
                |pedigri para estimar la probabilidad de alelos no vistos
                |para marcadores que comparten algún alelo con la familia"""
                  .stripMargin
            }
          }
        }
      }
    }

    val evidenceProbability = getEvidenceProbabilityLog(cpts)

    val markerDetails: Map[String, MarkerLRDetail] = markers.flatMap {
      marker =>
        if (!queryProfiles(unknown).contains(marker)) {
          None
        } else if (toleratedExclusions.contains(marker)) {
          Some(marker -> MarkerLRDetail(1.0, "excluded"))
        } else {
          val alleles = queryProfiles(unknown)(marker)
          val genotypeProb = if (alleles.length == 1) {
            val f = getFrequency(alleles(0), marker, frequencyTable)
            f * f
          } else if (alleles.length == 2) {
            2 * getFrequency(alleles(0), marker, frequencyTable) *
              getFrequency(alleles(1), marker, frequencyTable)
          } else {
            1.0
          }
          val classification = if (exclusionMarkers.contains(marker)) "mutation" else "normal"
          val markerLR = (markerQueryLog.get(marker), markerEvidenceLog.get(marker)) match {
            case (Some(q), Some(e)) if genotypeProb > 0 => math.exp(q - e - math.log(genotypeProb))
            case _ => 0.0
          }
          Some(marker -> MarkerLRDetail(markerLR, classification))
        }
    }.toMap

    val evidenceProbabilityLogTotal: Option[Double] = {
      val relevant = markerEvidenceLog.filterKeys(m => !toleratedExclusions.contains(m))
      if (relevant.isEmpty) None else Some(relevant.values.sum)
    }

    (evidenceProbability, message, toleratedExclusions.toList, markerDetails, evidenceProbabilityLogTotal)
  }

  private def getProbabilityOf(
    variable: String,
    alelle: Double,
    cpts: Array[PlainCPT]
  ) : Double = {
    val cptsOfVariable = cpts.filter(
      cpt => cpt.header.contains(variable)
    )
    var probability = 0.0
    cptsOfVariable.foreach{
      cpt => {
        val matrixArray = cpt.matrix.toArray
        cpt.matrix = matrixArray.iterator
        matrixArray
          .filter(
            elem => elem.contains(alelle)
          )
          .headOption
          .foreach(row => probability = row.last)
      }
    }

    probability
  }

  private def alleleAppearsInCpt(
    cpt: PlainCPT,
    allele: Double,
    pVariable: String,
    mVariable: String
  ): Boolean = {
    val matrixArray = cpt.matrix.toArray
    cpt.matrix = matrixArray.iterator
    val alleleColumnIndexes = cpt.header.zipWithIndex.collect {
      case (name, index) if name == pVariable || name == mVariable => index
    }
    matrixArray.exists(row => alleleColumnIndexes.exists(index => row(index) == allele))
  }

  private def isAnyAlellesInGenotipeAndN(
    marker: String,
    alelles: Array[Double],
    pVariable: String,
    mVariable: String,
    genotypification: Array[PlainCPT],
    n: Map[String,List[Double]]
  ) : Boolean = {
    val isInN = alelles.forall(
      allelesInN(marker, n)
        .contains(_)
    )
    val markerCpts = genotypification.filter(
      cpt => cpt.header.contains(mVariable) || cpt.header.contains(pVariable)
    )
    val alellesInGenotype = alelles.filter(
      allele => markerCpts.exists(
        cpt => alleleAppearsInCpt(cpt, allele, pVariable, mVariable)
      )
    )
    isInN && (alellesInGenotype.length>0)
  }

  private def isAllAlellesInGenotipeAndN(
    marker: String,
    alelles: Array[Double],
    pVariable: String,
    mVariable: String,
    genotypification: Array[PlainCPT],
    n: Map[String, List[Double]]
  ) : Boolean = {
    val isInN = alelles.forall(allelesInN(marker, n).contains(_))


    val markerCpts = genotypification.filter(
      cpt => cpt.header.contains(mVariable) || cpt.header.contains(pVariable)
    )
    val isInGenotipe = alelles.forall(
      allele => markerCpts.exists(
        cpt => alleleAppearsInCpt(cpt, allele, pVariable, mVariable)
      )
    )
    isInN && isInGenotipe
  }

  private def getAlelleInGenotype(
    marker: String,
    alelles: Array[Double],
    pVariable: String,
    mVariable: String,
    genotypification: Array[PlainCPT]
  ) : Option[Double] = {
    val markerCpts = genotypification
      .filter(
        cpt =>cpt.header.contains(mVariable) || cpt.header.contains(pVariable)
      )
    val alellesInGenotype = alelles.filter(
      allele => markerCpts.exists(
        cpt => alleleAppearsInCpt(cpt, allele, pVariable, mVariable)
      )
    )
    alellesInGenotype.headOption
  }

  def getEvidenceProbabilityLog(cpts: Array[PlainCPT]): Option[Double] = {
    val sums = cpts
      .map(
        cpt => {
          val matrixArray = extractMatrixFromPlainCPT(cpt)
          matrixArray
            .map(_.last)
            .sum
        }
      )
    val result: Option[Double] = sums.foldLeft(Option(0d))(
      (acc, n) => acc match {
        case Some(x) if n > 0 => Some(x + math.log(n))
        case _ => None
      }
    )
    result
  }

  private def extractMatrixFromCPT(cpt: CPT): Array[Array[Double]] = {
    val matrix = cpt.matrix.toArray
    cpt.matrix = matrix.iterator
    matrix
  }

  private def extractMatrixFromPlainCPT(cpt: PlainCPT): Array[Array[Double]] = {
    val matrix = cpt.matrix.toArray
    cpt.matrix = matrix.iterator
    matrix
  }

  def getGenotypeProbabilityLog(
    queryProfiles: Map[String, Map[Marker, Array[Double]]],
    frequencyTable: FrequencyTable
  ): Option[Double] = {
    val probs: Iterable[Double] = queryProfiles.head._2.map {
      case (marker, alleles) =>
        if (alleles.length == 1) {
          val frequency = getFrequency(alleles(0), marker, frequencyTable)
          frequency * frequency
        } else if (alleles.length == 2) {
          2 * getFrequency(
            alleles(0),
            marker,
            frequencyTable
          ) * getFrequency(
            alleles(1),
            marker,
            frequencyTable
          )
        } else {
          1
        }
    }
    val result: Option[Double] = probs.foldLeft(Option(0d))(
       (acc, n) => acc match {
         case Some(x) if (n>0) => Some(x + math.log(n))
         case _ => None
        }
    )
    result
  }

  def getFrequency(
    allele: Double,
    marker: Marker,
    frequencyTable: FrequencyTable
  ): Double = {
    val frecuenciaMinima = frequencyTable(marker).get(-1.0)
    val alleleFrecuency = frequencyTable(marker).get(allele)
    if (
      alleleFrecuency.isDefined &&
        alleleFrecuency.get.>(frecuenciaMinima.get)
    ) {
      alleleFrecuency.get
    } else {
      frecuenciaMinima.get
    }
  }

  def getQueryProfiles(
    profiles: Array[Profile],
    genogram: Array[Individual],
    analysisType: AnalysisType,
    frequencyTable: FrequencyTable
  ) : Map[String, Map[Marker, Array[Double]]] = {
    genogram
      .filter(_.unknown)
      .map(
        individual => {
          individual.alias.text -> {
            val profile = profiles
              .find(profile => profile.globalCode == individual.globalCode.get)
              .get
            getQueryProfileAlleles(profile, analysisType, frequencyTable)
          }
        }
      ).toMap
  }

  def getQueryVariables(variables: Map[String, Variable]): Array[String] = {
    variables
      .values
      .toArray
      .filter(
        v => v.unknown && v.kind == VariableKind.Genotype
      )
      .map(_.name)
  }

  def pruneCPTs(
    variables: Map[String, Variable],
    graph: Graph[String, DiEdge],
    cpts: Array[CPT]
  ): Array[CPT] = {
    val pruning = Array(
      nodePrunning(variables, graph)(_),
      zeroProbabilityPrunning(_)
    )
    pruning
      .foldLeft(cpts){
        case (accum, prunning) => prunning(accum)
      }
  }

  private def printCPTs(cpts: Array[CPT]) = {
    cpts.foreach {
      cpt =>
        logger.info(cpt.header.mkString(" "))
        logger.info(cpt.matrix.map(row => row.mkString(" ")).mkString("\n"))
    }
  }

  private def printArrayCPTs(cpts: Array[Array[Array[Double]]]) = {
    cpts.foreach {
      cpt =>
        logger.info(
          cpt.map(
            row => row.mkString(" ")
          )
          .mkString("\n")
        )
    }
  }

  def variableElimination(
    unknown: String,
    cptsInput: Array[PlainCPT],
    queries: Array[String],
    graph: Graph[String, DiEdge],
    verbose: Boolean = false
  ): PlainCPT = {
    var cpts = cptsInput
    val startGetOrderingTime = System.currentTimeMillis()
    var sortedVariables = getOrdering(graph, queries, cptsInput)
    val endGetOrderingTime = System.currentTimeMillis()
    sortedVariables
      .zipWithIndex
      .foreach {
        case (variable, index) =>
          val dependentCPTs = cpts.filter(_.header.contains(variable))
          if (dependentCPTs.nonEmpty) {
            val startProdFactor = System.currentTimeMillis()
            val product = prodFactor(unknown, dependentCPTs)
            val endProdFactor = System.currentTimeMillis()
            val startSumFactor = System.currentTimeMillis()
            val cpt = sumFactor(product, variable)
            val endSumFactor = System.currentTimeMillis()
            cpts = (cpts diff dependentCPTs) :+ cpt
          }
    }
    val startFinalProdFactor = System.currentTimeMillis()
    val pf = prodFactor(unknown, cpts)
    val endFinalProdFactor = System.currentTimeMillis()
    pf
  }

  def sumFactor(cpt: PlainCPT, variable: String): PlainCPT = {
    cpt.sumFactor(variable)
  }

  def prodFactor(unknown: String, cpts: Array[PlainCPT]) : PlainCPT = {
    cpts
      .tail
      .foldLeft[PlainCPT](cpts.head){
        case (prev, current) => prev.prodFactor(current)
      }
  }

  def makeInteractionGraph (
    inputGraph: Graph[String, DiEdge],
    cptsInput: Array[PlainCPT]
  ) : Graph[String, UnDiEdge] = {
    var edges : Array[UnDiEdge[String]] = Array.empty
      cptsInput.foreach(
        cpt => {
          var header = cpt.header.filter(head => !head.equals("Probability"))
          val pairs = header.combinations(2)
          pairs.foreach(
            pair => edges =edges :+ UnDiEdge(pair(0),pair(1))
          )
      }
    )
    var graph = Graph.from(
      inputGraph.nodes.toOuter,
      edges
    )
    graph
  }

  def getOrdering(
    inputGraph: Graph[String, DiEdge],
    queries: Array[String],
    cptsInput:
    Array[PlainCPT]
  ): Array[String] = {
    var graph: Graph[String, UnDiEdge] = Graph.from(
      inputGraph.nodes.toOuter,
      inputGraph.edges.map(
        e => e.source.toOuter ~ e.target.toOuter
      )
    )
    val variables: Array[String] = graph
      .nodes
      .toList
      .map(n => n.toOuter)
      .toArray diff queries
    var interactionGraph : Graph[String, UnDiEdge] =
      makeInteractionGraph(inputGraph, cptsInput)

    variables.map {
      _ =>
        val minVertex = interactionGraph
          .nodes
          .toList
          .filter(
            n => !queries.contains(n.toOuter)
          )
          .minBy(
            n => (n.degree, n.toOuter)
          )
          .toOuter
        val neighbors = (interactionGraph get minVertex)
          .neighbors
          .toList
          .map(_.toOuter)
        if (neighbors.length > 1) {
          val pairs = neighbors.combinations(2)
          while (pairs.hasNext) {
            val pair = pairs.next()
            interactionGraph += pair(0) ~ pair(1)
          }
        }
        interactionGraph = interactionGraph -! minVertex
        minVertex
    }
  }

  def generateCPTs(
    variablesMap: Map[String, Variable],
    graph: Graph[String, DiEdge],
    frequencyTable: FrequencyTable,
    linkage: Linkage,
    mutationModelType: Option[Long] = None,
    mutationModelData: Option[List[MutationModelData]] = None,
    n: Map[String, List[Double]] = Map.empty,
    individuals: Array[Individual]
  ): Array[CPT] = {
    variablesMap.values.toArray.map {
      vertex =>
        val node = graph get vertex.name
        val variables = node
          .diPredecessors
          .map(n => variablesMap(n))
          .toArray
        getCPT(
          frequencyTable,
          variables :+ vertex,
          vertex,
          linkage,
          mutationModelType,
          mutationModelData,
          n,
          individuals
        )
    }
  }
  def getSex(
    variableName:String,
    mutationModelData: Option[List[MutationModelData]] = None
  ): String = {
    val variableSex =
      if (variableName.endsWith("_p")||variableName.endsWith("_m")) {
        if (variableName.endsWith("_p")) {
          "M"
        } else {
          "F"
        }
      } else {
        "I"
      }
    val ignoreSex = mutationModelData
      .map(
        list => list
          .find(!_._3.ignoreSex)
          .map(_._3.ignoreSex)
      )
      .flatten
      .getOrElse(true)
    val sex = if (variableSex != "I" && !ignoreSex){variableSex}else{"I"}
    sex
  }

  private def isAlleleFromUndeclaredParent(
    vertex: Variable,
    individuals: Array[Individual]
  ): Boolean = {
    vertex.kind == VariableKind.Genotype &&
    individuals.exists { individual =>
      val soloMadre = individual.idFather.isEmpty && individual.idMother.isDefined
      val soloPadre = individual.idMother.isEmpty && individual.idFather.isDefined
      (soloPadre &&
        vertex.name == getVariableName(individual.alias, vertex.marker, "m")) ||
      (soloMadre &&
        vertex.name == getVariableName(individual.alias, vertex.marker, "p"))
    }
  }

  private def frequencyOfInheritedAllele(
    node: Double,
    variable: Variable,
    frequencyTable: FrequencyTable,
    mutationModelType: Option[Long],
    mutationModelData: Option[List[MutationModelData]],
    n: Map[String, List[Double]],
    sex: String,
    fromUndeclaredParent: Boolean
  ): Double = {
    val plain = getFrequency(node, variable.marker, frequencyTable)
    val hasModel = mutationModelType.isDefined &&
      findMutationParam(mutationModelData, variable.marker, sex).isDefined
    if (!fromUndeclaredParent || !hasModel) {
      plain
    } else {
      val domain = n.getOrElse(variable.marker, Nil).filterNot(_.equals(-1.0))
      val domainSize = n.getOrElse(variable.marker, Nil).size
      var acc = 0.0
      domain.foreach { a =>
        val pa = getFrequency(a, variable.marker, frequencyTable)
        val t = if (a == node) {
          getProbabityOfDiagonal(
            variable, mutationModelType, mutationModelData, node, n, sex
          )
        } else {
          getProbabityOfNonDiagonal(
            variable, mutationModelType, mutationModelData, domainSize, a, node, sex
          )
        }
        acc += pa * t
      }
      if (acc <= 0.0) {
        plain
      } else {
        acc
      }
    }
  }

  def getRowProbability(
    row: Array[Double],
    header: Array[String],
    variable: Variable,
    frequencyTable: FrequencyTable,
    linkage: Linkage,
    mutationModelType: Option[Long] = None,
    mutationModelData: Option[List[MutationModelData]] = None,
    n: Map[String,List[Double]] = Map.empty,
    fromUndeclaredParent: Boolean = false
  ):Double = {
    variable.kind match {
      case VariableKind.Genotype => {
        val node = getNode(row, header, variable.name)
        val s = getSelector(row, header)
        val ap = getAlleleFather(row, header, variable.name)
        val am = getAlleleMother(row, header, variable.name)
        val sex = getSex(variable.name,mutationModelData)
        if (s.contains(1) && ap.contains(node)) {
          getProbabityOfDiagonal(
            variable,
            mutationModelType,
            mutationModelData,
            node,
            n,
            sex
          )
        }
        else if (s.contains(2) && am.contains(node)) {
          getProbabityOfDiagonal(
            variable,
            mutationModelType,
            mutationModelData,
            node,
            n,
            sex
          )
        }
        else if (s.isEmpty && ap.isEmpty && am.isEmpty) {
          frequencyOfInheritedAllele(
            node,
            variable,
            frequencyTable,
            mutationModelType,
            mutationModelData,
            n,
            sex,
            fromUndeclaredParent
          )
        }
        else {
          val allelei = if (s.contains(1)) ap else am
          getProbabityOfNonDiagonal(
            variable,
            mutationModelType,
            mutationModelData,
            n.getOrElse(variable.marker, Nil).size,
            allelei.get,
            node,
            sex
          )
        }
      }
      case VariableKind.Selector =>
        if (row.length > 1) {
          val ancestor = getSelector(row, header).get
          val node = getNode(row, header, variable.name)
          val recombinationFactor = linkage(variable.marker)._2
          if (ancestor == node) {
            1 - recombinationFactor
          } else {
            recombinationFactor
          }
        } else {
          0.5
        }
      case VariableKind.Heterocygote => {
        val ap = getAlleleFather(row, header, variable.name)
        val am = getAlleleMother(row, header, variable.name)
        if (ap != am) {
          1.0
        } else {
          0.0
        }
      }
      case VariableKind.Homocygote => {
        val ap = getAlleleFather(row, header, variable.name)
        val am = getAlleleMother(row, header, variable.name)
        if (ap == am) {
          1.0
        } else {
          0.0
        }
      }
    }
  }

  private def findMutationParam(
    mutationModelData: Option[List[MutationModelData]],
    marker: String,
    sex: String
  ): Option[MutationModelData] = {
    val data = mutationModelData.getOrElse(Nil)
    data.find(x => x._1.locus == marker && x._1.sex == sex).orElse(
      if (isActbp2Se33Alias(marker)) {
        data.find(x => isActbp2Se33Alias(x._1.locus) && x._1.sex == sex)
      } else {
        None
      }
    )
  }

  def getProbabityOfDiagonal(
    variable: Variable,
    mutationModelType: Option[Long],
    mutationModelData: Option[List[MutationModelData]],
    node: Double,
    n: Map[String, List[Double]],
    sex:String = "I"
  ): Double = {
    mutationModelType match {
      case None => 1.0
      case Some(mutationType) => {
        val mutationMarkerDataOpt = findMutationParam(mutationModelData, variable.marker, sex)
        (mutationType, mutationMarkerDataOpt) match {
          case (1,Some(mutationMarkerData)) => {
            (
              1 - mutationMarkerData
                ._1.mutationRate
                .getOrElse(zero)
            )
            .doubleValue()
          }
          case (2,Some(mutationMarkerData)) => {
            val domain = n.getOrElse(variable.marker, Nil)
            val offDiagonalSum = domain
              .filterNot(_ == node)
              .map(other => getProbabityOfNonDiagonal(
                variable, mutationModelType, mutationModelData, domain.size, node, other, sex
              ))
              .sum
            1.0 - offDiagonalSum
          }
          case _ => 0.0
        }
      }
    }
  }

  def getProbabityOfNonDiagonal(
    variable: Variable,
    mutationModelType: Option[Long],
    mutationModelData: Option[List[MutationModelData]],
    n: Int,
    allelei: Double,
    allelej: Double,
    sex:String = "I"
  ) : Double= {
    mutationModelType match {
      case None => 0.0
      case Some(mutationType) => {
        val mutationMarkerDataOpt = findMutationParam(mutationModelData, variable.marker, sex)
        (mutationType, mutationMarkerDataOpt) match {
          case (1, Some(mutationMarkerData)) => {
            (mutationMarkerData._1.mutationRate.get/(n-1)).doubleValue()
          }
          case (2, Some(mutationMarkerData)) => {
            val ki = mutationMarkerData._2
              .find(_.allele == allelei)
              .map(_.ki)
              .getOrElse(zero)
            val alleleDiff = (allelei - allelej).abs
            if (alleleDiff <= mutationMarkerData._3.cantSaltos.toDouble &&
                MutationService.isWholeRepeatStep(allelei, allelej)) {
              (
                ki * Math.pow(
                  mutationMarkerData._1
                    .mutationRange
                    .getOrElse(zero)
                    .doubleValue(),
                  alleleDiff
                )
              )
              .doubleValue()
            } else {
              0.0
            }
          }
          case _ => 0.0
        }
      }
    }
  }

  def getCPT(
    frequencyTable: FrequencyTable,
    variables: Array[Variable],
    vertex: Variable,
    linkage: Linkage,
    mutationModelType: Option[Long],
    mutationModelData: Option[List[MutationModelData]] = None,
    n: Map[String,List[Double]] = Map.empty,
    individuals: Array[Individual]
  ): CPT = {
    val header = variables.map(_.name) :+ "Probability"
    val possibilities = variables.map {
      case Variable(_, _, VariableKind.Selector, _, _) => Array(1.0,2.0)
      case Variable(name, _, VariableKind.Genotype, Some(alleles), false) =>
        getPossibilitiesAlleles(alleles, name, individuals)
      case Variable(_, _, VariableKind.Genotype, _, _) =>
        n.getOrElse(vertex.marker, Nil).filterNot(_.equals(-1.0)).toArray
      case Variable(_, _, VariableKind.Heterocygote, _, _) => Array(0.0)
    }
    val matrix = generatePermutations(
      possibilities,
      header,
      vertex,
      frequencyTable,
      linkage,
      mutationModelType,
      mutationModelData,
      n,
      isAlleleFromUndeclaredParent(vertex, individuals)
    )
    val compensatedMatrix =
      if (needsHeterocygoteFounderPhaseFactor(vertex, individuals)) {
        matrix.map { row => row.updated(row.length - 1, row(row.length - 1) * 2.0) }
      } else {
        matrix
      }
    new CPT(vertex, header, compensatedMatrix.iterator, compensatedMatrix.size)
  }

  private def needsHeterocygoteFounderPhaseFactor(
    vertex: Variable,
    individuals: Array[Individual]
  ): Boolean = {
    vertex.kind == VariableKind.Genotype && vertex.name.endsWith("_p") && {
      val individualName = vertex.name.substring(0, vertex.name.indexOf('_'))
      individuals
        .find(_.alias.text == individualName)
        .exists(ind => heterocygoteFounder(ind, vertex.alleles))
    }
  }

  private def getPossibilitiesAlleles(
    alleles : Array[Double],
    variableName : String,
    individuals: Array[Individual]
  ) : Array[Double] = {
    val individualName = variableName
      .substring(0, variableName.indexOf('_'))
    val individual = individuals
      .filter(ind => ind.alias.text.equals(individualName))
      .apply(0)
    val selector = variableName.charAt(variableName.length-1)
    var ret = Array[Double]()
    if(heterocygoteFounder(individual, Option(alleles))) {
      if (selector.equals('p')) {
        ret = ret :+ alleles.apply(0)
      } else if (selector.equals('m')) {
        ret = ret :+ alleles.apply(1)
      }
    } else {
      ret = alleles
    }
    ret
  }

def generatePermutations(
  lists: Array[Array[Double]],
  header: Array[String],
  variable: Variable,
  frequencyTable: FrequencyTable,
  linkage: Linkage,
  mutationModelType: Option[Long]=None,
  mutationModelData: Option[List[MutationModelData]]=None,
  n: Map[String,List[Double]] = Map.empty,
  fromUndeclaredParent: Boolean = false
): ArrayBuffer[Array[Double]] = {
    val result = ArrayBuffer[Array[Double]]()
    def recursive(depth: Int, current: ArrayBuffer[Double]): Unit = {
      if (depth == lists.length) {
        val row = current.toArray
        val probability = getRowProbability(
          row,
          header,
          variable,
          frequencyTable,
          linkage,
          mutationModelType,
          mutationModelData,
          n,
          fromUndeclaredParent
        )
        result += (row :+ probability)
      } else {
        for (i <- lists(depth).indices) {
          recursive(depth + 1, current :+ lists(depth)(i))
        }
      }
    }

    recursive(0, ArrayBuffer[Double]())
    result
  }

  def getSubgraphs(graph: Graph[String, DiEdge]): Array[Graph[String, DiEdge]] = {
    graph.componentTraverser().map(component =>
      Graph.from(component.nodes.map(_.toOuter), component.edges.map(_.toOuter))).toArray
  }

  def getAlleles(
    marker: Profile.Marker,
    globalCode: Option[SampleCode],
    profiles: Array[Profile]
  ): Option[Array[Double]] = {
    globalCode.flatMap {
      gc =>
        val profile = profiles.find(_.globalCode == gc).get
        profile.genotypification
          .get(1)
          .flatMap {
            genotypification =>
              genotypification
                .get(marker)
                .flatMap {
                  alleles =>
                    val strs = alleles
                      .filter {
                        case Allele(_) => true;
                        case OutOfLadderAllele(_,_) => true;
                        case MicroVariant(_) => true;
                        case _ => false
                      }
                      .map(
                        x => x match {
                          case Allele(count) => count.toDouble;
                          case _ => -1.0
                        }
                      )
                      .distinct
                    if (strs.size <= 2) {
                      Some(strs.toArray)
                    } else {
                      None
                    }
          }
      }
    }
  }

  def generateGraph(
    profiles: Array[Profile],
    markers: Array[Profile.Marker],
    individuals: Array[Individual],
    linkage: Linkage,
    mutationModelType: Option[Long] = None
  ): (Map[String, Variable], Graph[String, DiEdge]) = {
    val vertices: Map[String, Variable] = generateVertices(
      markers,
      profiles,
      individuals,
      mutationModelType
    )
    val edges: List[DiEdge[String]] = generateEdges(
      markers,
      individuals,
      vertices,
      linkage)

    (vertices, Graph.from(vertices.keys, edges))
  }

  def generateEdges(
    markers: Array[Profile.Marker],
    individuals: Array[Individual],
    variables: Map[String, Variable],
    linkage: Linkage
  ): List[DiEdge[String]] = {
    var edges = List.empty[DiEdge[String]]
    individuals.foreach {
      individual =>
      markers.foreach {
        marker =>
        attrs.foreach {
          attr =>
            val variable = variables(getVariableName(individual.alias, marker, attr))

            if (attr == "m" && individual.idMother.isDefined) {
              attrs.foreach {
                motherAttr =>
                  val mother = getVariableName(individual.idMother.get, marker, motherAttr)
                  edges :+= mother ~> variable.name
              }
              val selector = getVariableName(individual.alias, marker, "m_s")
              edges :+= selector ~> variable.name
            }

            if (attr == "p" && individual.idFather.isDefined) {
              attrs.foreach {
                fatherAttr =>
                  val father = getVariableName(individual.idFather.get, marker, fatherAttr)
                  edges :+= father ~> variable.name
              }
              val selector = getVariableName(individual.alias, marker, "p_s")
              edges :+= selector ~> variable.name
            }

            if (heterocygoteNonFounder(individual, variable.alleles)) {
              val pm = getVariableName(individual.alias, marker, "pm")
              edges :+= variable.name ~> pm
            }
            lazy val marker_is_linked = linkage.contains(marker)
            lazy val is_father_case = attr == "p" && individual.idFather.isDefined
            lazy val is_mother_case = attr == "m" && individual.idMother.isDefined
            if (marker_is_linked && (is_father_case || is_mother_case)) {
              val previousSelector = getVariableName(
                individual.alias,
                linkage(marker)._1,
                s"${attr}_s"
              )
              val selector = getVariableName(
                individual.alias,
                marker,
                s"${attr}_s"
              )
              edges :+= previousSelector ~> selector
            }

        }
      }
    }

    edges
  }

  def allelesMutated(
    alleles: Option[Array[Double]],
    allelesM: Option[Array[Double]],
    allelesP: Option[Array[Double]]
  ): Boolean = {
    (alleles.map(_.toList), allelesM, allelesP) match {
      case (Some(a :: Nil), Some(mother), Some(father)) =>
        !(mother.contains(a) && father.contains(a))
      case (Some(a1 :: a2 :: Nil), Some(mother), Some(father)) =>
        !((mother.contains(a1) && father.contains(a2)) ||
          (mother.contains(a2) && father.contains(a1)))
      case (Some(child), None, Some(father)) => !father.exists(child.contains(_))
      case (Some(child), Some(mother), None) => !mother.exists(child.contains(_))
      case _ => false
    }
  }

  def generateVertices(
    markers: Array[Profile.Marker],
    profiles: Array[Profile],
    individuals: Array[Individual],
    mutationModelType: Option[Long]
  ): Map[String, Variable] = {
    var variables = Map.empty[String, Variable]
    individuals.foreach {
      individual =>
        markers.foreach { marker =>
          var alleles: Option[Array[Double]] = getAlleles(
            marker,
            individual.globalCode,
            profiles
          )
          var allelesM: Option[Array[Double]] = None
          var allelesP: Option[Array[Double]] = None
          var motherIsUnknown = false
          var fatherIsUnknown = false

          if (individual.idMother.isDefined) {
            val mother = individuals.find(_.alias == individual.idMother.get).get
            allelesM = getAlleles(
              marker,
              mother.globalCode,
              profiles
            )
            motherIsUnknown = mother.unknown
            val name = getVariableName(
              individual.alias,
              marker,
              "m_s"
            )
            variables += name -> Variable(
              name,
              marker,
              VariableKind.Selector,
              None,
              individual.unknown
            )
          }

          if (individual.idFather.isDefined) {
            val father = individuals.find(_.alias == individual.idFather.get).get
            allelesP = getAlleles(
              marker,
              father.globalCode,
              profiles
            )
            fatherIsUnknown = father.unknown
            val name = getVariableName(
              individual.alias,
              marker,
              "p_s"
            )
            variables += name -> Variable(
              name,
              marker,
              VariableKind.Selector,
              None,
              individual.unknown
            )
          }

          lazy val mutationsWithoutMutationModel =
            allelesMutated(alleles, allelesM, allelesP) &&
            mutationModelType.isEmpty &&
            !motherIsUnknown && !fatherIsUnknown

          if (mutationsWithoutMutationModel) {
            alleles = None
          }

          variables ++= attrs.map {
            attr =>
              val name = getVariableName(
                individual.alias,
                marker,
                attr
              )
              name -> Variable(
                name,
                marker,
                VariableKind.Genotype,
                alleles,
                individual.unknown
              )
          }

          if (heterocygoteNonFounder(individual, alleles)) {
            val name = getVariableName(individual.alias, marker, "pm")
            variables += name -> Variable(
              name,
              marker,
              VariableKind.Heterocygote,
              None,
              individual.unknown
            )
          }
        }
      }
      variables
  }

  private def heterocygoteFounder(
    individual: Individual,
    alleles: Option[Array[Double]]
  ) = {
    val founder = !individual.idFather.isDefined && !individual.idMother.isDefined
    val heterocygote = alleles.fold(false)(_.length == 2)
    founder && heterocygote && !individual.unknown
  }

  private def heterocygoteNonFounder(
    individual: Individual,
    alleles: Option[Array[Double]]
  ) = {
    val notFounder = individual.idFather.isDefined || individual.idMother.isDefined
    val heterocygote = alleles.fold(false)(_.length == 2)
    notFounder && heterocygote && !individual.unknown
  }

  private def getVariableName(
    alias: NodeAlias,
    locus: String,
    attr: String
  ) = {
    s"${alias.text}_${locus}_$attr"
  }

  def stateRemoval(cpts:Array[CPT]): Array[CPT] = {
    var cptOut: Map[String, Set[Double]] = Map.empty

    cpts.foreach {
      cpt =>
        var cpt0: Map[String, Set[Double]] = Map.empty
        var cpt1: Map[String, Set[Double]] = Map.empty

        if (cpt.header.size > 2){
          var cptMatrix = Array[Array[Double]]()
          cptMatrix = cpt.matrix.toArray

          cptMatrix.foreach {
            row =>
              cpt.header.zipWithIndex.foreach {
                case (column, index) =>
                  if (column!= "Probability" && row.last == 0) {
                    cpt0 += (column -> (cpt0.getOrElse(column, Set.empty) + row(index)))
                  } else if (column != "Probability" && row.last != 0) {
                    cpt1 += (column -> (cpt1.getOrElse(column, Set.empty) + row(index)))
                  }
              }
          }

          val lout = (cpt0.keySet union cpt1.keySet).map {
            key => (key, cpt0.getOrElse(key, Set.empty) diff cpt1.getOrElse(key, Set.empty))
          }.filter(_._2.nonEmpty).toMap

          cptOut = cptOut.keySet.union(lout.keySet).map {
            key => (key, cptOut.getOrElse(key, Set.empty) ++ lout.getOrElse(key, Set.empty))
          }.toMap
          cpt.matrix = cptMatrix.iterator
        }
    }

    cpts.map {
      cpt =>
        cpt.matrix = cptOut.foldLeft[Matrix](cpt.matrix) {
          case (matrix, (columnName, allelesToRemove)) =>
            val column = cpt
              .header
              .zipWithIndex
              .find(_._1 == columnName)
            if (column.isDefined) {
              val index = column.get._2
              matrix.filter(
                row => {!allelesToRemove.contains(row(index))}
              )
            } else {
              matrix
            }
        }
        val matrixArray = cpt.matrix.toArray
        cpt.matrixSize = matrixArray.size
        cpt.matrix = matrixArray.iterator
        cpt
    }
  }

  def nodePrunning(
    variablesMap: Map[String, Variable],
    graph: Graph[String, DiEdge]
  )(cpts:Array[CPT]): Array[CPT] = {
    val var_filter = (variable:Variable) => (
      variable.alleles.isEmpty &&
        !variable.unknown &&
        variable.kind != VariableKind.Heterocygote
    )
    val variables = graph
      .nodes
      .filter(_.outDegree == 0)
      .map(v => variablesMap(v))
      .filter(var_filter)
      .map(_.name)
      .toList

    if (variables.nonEmpty) {
      nodePrunning(
        variablesMap,
        graph filter graph.having(node = n => !variables.contains(n.toOuter))
      )(
        cpts filter {cpt => !variables.contains(cpt.variable.name)}
      )
    } else {
      cpts
    }
  }

  def zeroProbabilityPrunning(cpts:Array[CPT]): Array[CPT] = {
    cpts.map(
      cpt => {
        cpt.matrix = cpt.matrix.filter(
          row => {
            row.last > 0
          }
        )
      val matrixArray = cpt.matrix.toArray
      cpt.matrixSize = matrixArray.size
      cpt.matrix = matrixArray.iterator
      cpt
    })
  }

  def getKeys(cpt: PlainCPT): Array[String] = {
    cpt.header.filter(_ != "Probability")
  }

  def getConditional(cpts: Array[CPT]): Array[CPT] = {
    cpts.map(
      cpt => {
        val (matrix1, matrixCopy) = cpt.matrix.duplicate
        val sum = matrix1.map(row => row.last).sum
        cpt.matrix = matrixCopy.map(
          row => row.zipWithIndex.map{
            case (col, index) => if (index == row.length-1) col/sum else col
          }
        )
        cpt
      }
    )
  }

  def getAlleleFather(
    row: Array[Double],
    header: Array[String],
    variableName: String
  ): Option[Double] =
    findKey(
      header,
      key => key.endsWith("p") && key != variableName
    )
    .map(row(_))
  def getAlleleMother(
    row: Array[Double],
    header: Array[String],
    variableName: String
  ): Option[Double] =
    findKey(
      header,
      key => key.endsWith("m") && key != variableName
    )
    .map(row(_))

  def getSelector(
    row: Array[Double],
    header: Array[String]
  ): Option[Double] =
    findKey(
      header,
      _.endsWith("s")
    )
    .map(x => row(x))

  def getNode(
    row: Array[Double],
    header: Array[String],
    variableName: String
  ): Double =
    row(
      findKey(
        header,
        _ == variableName
      ).get
    )

  private def findKey(
    header: Array[String],
    condition: String => Boolean
  ): Option[Int] = {
    header
      .zipWithIndex
      .find { case (key, _) => condition(key) }
      .map(_._2)
  }

}
