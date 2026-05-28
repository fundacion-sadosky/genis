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
import scalax.collection.immutable.Graph
import scalax.collection.edges.{DiEdge, UnDiEdge, labeled}
import scalax.collection.edges.DiEdge
import scalax.collection.edges.UnDiEdge

object BayesianNetwork {
  type MutationModelData = (
    MutationModelParameter,
    List[MutationModelKi],
    MutationModel
  )
  type FrequencyTable = Map[String, Map[Double, Double]]
  type Matrix = Iterator[Array[Double]]
  type Linkage = Map[Marker, (Marker, Double)]

  // con este nombre se configura en el application.conf
  val name = "pedigree"
  val zero = BigDecimal.valueOf(0)
  val logger: Logger = Logger(this.getClass)
  val attrs = Array("m", "p")

  def getNFromTable(ft: BayesianNetwork.FrequencyTable):
    Map[String, List[Double]] = {
    ft.map(
      x => (
        x._1,
        x._2
          .map(y => y._1)
          .toList
      )
    )
  }

  def getMarkersFromCpts(cpts2: Array[PlainCPT2]): Set[String] = {
    val cpts = cpts2.map(
      plainCpt2 => PlainCPT(
        plainCpt2.header,
        plainCpt2.matrix.iterator,
        plainCpt2.matrix.length
      )
    )
    getMarkersFromCpts(cpts)
  }

  def getMarkersFromCpts(cpts: Array[PlainCPT]): Set[String] = {
    cpts
      .filter(!_.matrix.isEmpty)
      .map(
        cpt => extractMarker(cpt.header.head)
      )
      .toSet
  }

  // Scenario or recalculate genotypification for new alleles
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
    locusRangeMap: NewMatchingResult.AlleleMatchRange = Map.empty
  ): Double = {
    val n = if (seenAlleles.isEmpty) {
      getNFromTable(frequencyTable)
    } else {
      seenAlleles
    }
    val queryProfiles = getQueryProfiles(
      profiles,
      genogram,
      analysisType,
      frequencyTable
    )
    val markers = queryProfiles.values.head.keys.toArray
    val linkageMarkers = linkage.keySet.union(linkage.values.map(_._1).toSet)
    val normalizedFrequencyTable = getNormalizedFrequencyTable(frequencyTable)
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
      n
    )
    if (verbose) {
      logger.info(s"--- GetLR time ---")
      logger.info(s"--- LR: ${lr._1} ---")
    }
    lr._1
  }

  // Al activar el pedigree
  def getLR(
    unknowns: Array[String],
    profile: Profile,
    frequencyTable: FrequencyTable,
    analysisType: AnalysisType,
    genotypification: Array[PlainCPT],
    mutationModelType: Option[Long] = None,
    mutationModelData: Option[List[MutationModelData]] = None,
    n: Map[String, List[Double]] = Map.empty
  ): (Double, String) = {
    val linkage: Linkage = Map.empty
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
      linkage,
      mutationModelType,
      mutationModelData,
      n
    )
    logger.info(
      s"--- Profile: ${profile.globalCode} - ${profile.internalSampleCode}"
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
    n: Map[String, List[Double]] = Map.empty
  ): (Double, String) = {
    val markersToFilter = getMarkersToFilter(
      markers,
      queryProfiles,
      genotypification,
      n
    )
    val messageInit = s"""${"El valor calculado de LR es aproximado debido a que se detectaron "}${"alelos no vistos que no estan en el genotipo de la familia,"}"""
    var message = ""
    var messageMarkersFitered = ""
    val genotypificationFiltered = filterGenotypification(
      genotypification,
      markersToFilter
    )
    val queryProfilesFiltered = filterQueryProfile(queryProfiles, markersToFilter)
    if (markersToFilter.length > 0) {
      messageMarkersFitered += s"""${" se descartaron el o los marcadores que lo/s contenían y que no "}${"compartían ningún alelo con la familia "}"""
    }
    val evidenceProbabilityLog = getEvidenceProbabilityLog(genotypificationFiltered)
    val queryProbabilityLog = getQueryProbabilityLog(
      markers,
      queryProfilesFiltered,
      genotypificationFiltered,
      analysisType,
      normalizedFrequencyTable,
      linkage,
      mutationModelType,
      mutationModelData,
      n
    )
    val genotypeProbabilityLog = getGenotypeProbabilityLog(
      queryProfilesFiltered,
      normalizedFrequencyTable
    )
    val lr = if (evidenceProbabilityLog.isEmpty || genotypeProbabilityLog.isEmpty || queryProbabilityLog._1.isEmpty) {
      0.0
    } else {
      val subjectProbabilityLog = evidenceProbabilityLog.get + genotypeProbabilityLog.get
      math.exp(
        queryProbabilityLog._1.get - subjectProbabilityLog
      )
    }

    // Process Error Message
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
    (lr, message)
  }

  private def getMarkersToFilter(
    markers: Array[Profile.Marker],
    queryProfiles: Map[String, Map[Profile.Marker, Array[Double]]],
    genotypification: Array[PlainCPT],
    n: Map[String, List[Double]]
  ): Array[Profile.Marker] = {
    val unknown = queryProfiles.keys.head
    var markersToFilter: Array[Profile.Marker] = Array.empty
    markers.foreach {
      marker => {
        val pVariable = s"${unknown}_${marker}_p"
        val mVariable = s"${unknown}_${marker}_m"
        if (queryProfiles(unknown).contains(marker)) {
          val alleles = queryProfiles(unknown)(marker)
          val noAlleleInGenotypeAndN = !isAnyAlellesInGenotipeAndN(
            marker,
            alleles,
            pVariable,
            mVariable,
            genotypification,
            n
          )
          if (noAlleleInGenotypeAndN) {
            markersToFilter = markersToFilter ++ List(marker)
          }
        }
      }
    }
    markersToFilter
  }

  private def getIncompleteMarkers(
    markers: Array[Profile.Marker],
    queryProfiles: Map[String, Map[Profile.Marker, Array[Double]]],
    genotypification: Array[PlainCPT],
    n: Map[String, List[Double]]
  ): Array[Profile.Marker] = {
    val unknown = queryProfiles.keys.head
    var incompleteMarkers: Array[Profile.Marker] = Array.empty
    markers.foreach {
      marker => {
        val pVariable = s"${unknown}_${marker}_p"
        val mVariable = s"${unknown}_${marker}_m"
        if (queryProfiles(unknown).contains(marker)) {
          val alleles = queryProfiles(unknown)(marker)
          lazy val isAny = isAnyAlellesInGenotipeAndN(
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
  ): Array[PlainCPT] = {
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
  ): Map[String, Map[Profile.Marker, Array[Double]]] = {
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

  def getNormalizedFrequencyTable(frequencyTable: FrequencyTable): FrequencyTable = {
    frequencyTable.map {
      case (marker, alleles) => {
        val sum = alleles.values.sum
        if (sum > 1) {
          (
            marker,
            alleles.map {
              case (allele, frequency) => (allele, frequency / sum)
            }
          )
        } else if (sum < 1) {
          (marker, alleles + (-100.0 -> (1 - sum)))
        } else {
          (marker, alleles)
        }
      }
    }
  }

  def transformAlleleValues(alleles: Array[AlleleValue]): Array[Double] = {
    alleles.flatMap {
      case Allele(v) => Some(v.toDouble)
      case OutOfLadderAllele(_, _) => Some(-1.0)
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
    locusRangeMap: NewMatchingResult.AlleleMatchRange = Map.empty,
    mutationModelType: Option[Long] = None,
    mutationModelData: Option[List[MutationModelData]] = None,
    n: Map[String, List[Double]] = Map.empty
  ): Array[PlainCPT] = {
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

    val geno = subgraphs
      .zipWithIndex
      .flatMap {
        case (subgraph, index) =>
          val variables = variablesMap.filter {
            case (n, _) => subgraph.nodes.exists(_.outer == n)
          }
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
          val prunnedCPTs = pruneCPTs(variables, subgraph, cpts)

          if (prunnedCPTs.nonEmpty) {
            val prunnedVariables = prunnedCPTs.map(_.variable.name)
            val keepNodes = prunnedVariables.toSet
            val prunnedGraph = Graph.from(
              subgraph.nodes.collect { case n if keepNodes.contains(n.outer) => n.outer },
              subgraph.edges.collect { case e if keepNodes.contains(e.outer.source) && keepNodes.contains(e.outer.target) => e.outer }
            )

            val queries = getQueryVariables(variables)
            val ve = variableElimination(unknown, prunnedCPTs.map(_.getPlain()), queries, prunnedGraph, verbose)
            Some(ve)
          } else {
            None
          }
      }
      .filter(_.matrix.nonEmpty)
    geno
  }

  def getQueryProbabilityLog(
    markers: Array[String],
    queryProfiles: Map[String, Map[Marker, Array[Double]]],
    genotypification: Array[PlainCPT],
    analysisType: AnalysisType,
    frequencyTable: FrequencyTable,
    linkage: Linkage,
    mutationModelType: Option[Long] = None,
    mutationModelData: Option[List[MutationModelData]] = None,
    n: Map[String, List[Double]] = Map.empty
  ): (Option[Double], String) = {
    var cpts = genotypification
    var message = ""
    val unknown = queryProfiles.keys.head
    markers.foreach {
      marker => {
        val pVariable = s"${unknown}_${marker}_p"
        val mVariable = s"${unknown}_${marker}_m"
        if (queryProfiles(unknown).contains(marker)) {
          val alleles = queryProfiles(unknown)(marker)
          if (
            mutationModelType.isEmpty ||
            isAllAlellesInGenotipeAndN(
              marker, alleles, pVariable, mVariable, genotypification, n
            )
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
          } else {
            val alelleInGenotype = getAlelleInGenotype(
              marker, alleles, pVariable, mVariable, genotypification
            )
            val dependentCPTs = cpts.filter(
              cpt => cpt.header.contains(pVariable) ||
                cpt.header.contains(mVariable)
            )
            val header = Array(pVariable, mVariable) :+ "Probability"
            val probabilityPVariable: Double = getProbabilityOf(
              pVariable, alelleInGenotype, dependentCPTs
            )
            val probabilityMVariable: Double = getProbabilityOf(
              mVariable, alelleInGenotype, dependentCPTs
            )
            val minPorbabilityOfPVarible = getMinProbabilityofVariable(
              pVariable, dependentCPTs, marker, frequencyTable
            )
            val minPorbabilityOfMVarible = getMinProbabilityofVariable(
              mVariable, dependentCPTs, marker, frequencyTable
            )
            var x: Array[Double] = Array.empty
            x = x ++ alleles :+ (
              (probabilityMVariable * minPorbabilityOfPVarible) +
                (probabilityPVariable * minPorbabilityOfMVarible)
            )

            val matrixResult = ArrayBuffer(x)
            val cptResult = new PlainCPT(
              header,
              matrixResult.iterator,
              matrixResult.size
            )
            cpts = (cpts diff dependentCPTs) :+ cptResult

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
    (evidenceProbability, message)
  }

  private def getProbabilityOf(
    variable: String,
    alelle: Double,
    cpts: Array[PlainCPT]
  ): Double = {
    val cptsOfVariable = cpts.filter(
      cpt => cpt.header.contains(variable)
    )
    var probability = 0.0
    cptsOfVariable.foreach {
      cpt => {
        val matrixArray = cpt.matrix.toArray
        cpt.matrix = matrixArray.iterator
        probability = matrixArray
          .filter(
            elem => elem.contains(alelle)
          )
          .apply(0)
          .last
      }
    }
    probability
  }

  private def getCountMinFrecbeforeMin(
    marker: String,
    frequencyTable: FrequencyTable
  ): Int = {
    val frecMin = frequencyTable
      .getOrElse(marker, Map.empty)
      .get(-1.0)
    val menorFrecMin = frequencyTable
      .getOrElse(marker, Map.empty)
      .filter(tuple => (tuple._2 < frecMin.get))
    val cantMenores = menorFrecMin.keys.size
    cantMenores
  }

  private def getMinProbabilityofVariable(
    variable: String,
    cpts: Array[PlainCPT],
    marker: String,
    frequencyTable: FrequencyTable
  ): Double = {
    val cptsOfVariable = cpts.filter(cpt => cpt.header.contains(variable))
    var probability = 0.0
    cptsOfVariable.foreach {
      cpt => {
        val matrixArray = cpt.matrix.toArray
        cpt.matrix = matrixArray.iterator
        probability = matrixArray.minBy(elem => elem.last).last
      }
    }
    probability
  }

  private def isAnyAlellesInGenotipeAndN(
    marker: String,
    alelles: Array[Double],
    pVariable: String,
    mVariable: String,
    genotypification: Array[PlainCPT],
    n: Map[String, List[Double]]
  ): Boolean = {
    val isInN = alelles.forall(
      n.getOrElse(marker, List.empty)
        .contains(_)
    )
    val markerCpts = genotypification.filter(
      cpt => cpt.header.contains(mVariable) || cpt.header.contains(pVariable)
    )
    val alellesInGenotype = alelles.filter(
      allele => markerCpts.forall(
        cpt => {
          val matrixArray = cpt.matrix.toArray
          cpt.matrix = matrixArray.iterator
          matrixArray
            .map(_.apply(0))
            .contains(allele)
        }
      )
    )
    isInN && (alellesInGenotype.length > 0)
  }

  private def isAllAlellesInGenotipeAndN(
    marker: String,
    alelles: Array[Double],
    pVariable: String,
    mVariable: String,
    genotypification: Array[PlainCPT],
    n: Map[String, List[Double]]
  ): Boolean = {
    val isInN = alelles.forall(n.getOrElse(marker, List.empty).contains(_))
    val markerCpts = genotypification.filter(
      cpt => cpt.header.contains(mVariable) || cpt.header.contains(pVariable)
    )
    val isInGenotipe = alelles.forall(
      allele => markerCpts.forall(
        cpt => {
          val matrixArray = cpt.matrix.toArray
          cpt.matrix = matrixArray.iterator
          matrixArray
            .map(_.apply(0))
            .contains(allele)
        }
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
  ): Double = {
    val markerCpts = genotypification
      .filter(
        cpt => cpt.header.contains(mVariable) || cpt.header.contains(pVariable)
      )
    val alellesInGenotype = alelles.filter(
      allele => markerCpts.forall(
        cpt => {
          val matrixArray = cpt.matrix.toArray
          cpt.matrix = matrixArray.iterator
          (matrixArray.map(_.apply(0))).contains(allele)
        }
      )
    )
    alellesInGenotype(0)
  }

  /**
   * Returns the natural-log-probability of given evidence CPTs.
   */
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
          // TODO: Trisomías
          1
        }
    }
    val result: Option[Double] = probs.foldLeft(Option(0d))(
      (acc, n) => acc match {
        case Some(x) if (n > 0) => Some(x + math.log(n))
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
        alleleFrecuency.get > frecuenciaMinima.get
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
  ): Map[String, Map[Marker, Array[Double]]] = {
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
    graph: Graph[String, DiEdge[String]],
    cpts: Array[CPT]
  ): Array[CPT] = {
    val pruning = Array(
      nodePrunning(variables, graph)(_),
      zeroProbabilityPrunning(_)
    )
    pruning
      .foldLeft(cpts) {
        case (accum, prunning) => prunning(accum)
      }
  }

  def variableElimination(
    unknown: String,
    cptsInput: Array[PlainCPT],
    queries: Array[String],
    graph: Graph[String, DiEdge[String]],
    verbose: Boolean = false
  ): PlainCPT = {
    var cpts = cptsInput
    var sortedVariables = getOrdering(graph, queries, cptsInput)
    sortedVariables
      .zipWithIndex
      .foreach {
        case (variable, index) =>
          val dependentCPTs = cpts.filter(_.header.contains(variable))
          if (dependentCPTs.nonEmpty) {
            val product = prodFactor(unknown, dependentCPTs)
            val cpt = sumFactor(product, variable)
            cpts = (cpts diff dependentCPTs) :+ cpt
          }
      }
    val pf = prodFactor(unknown, cpts)
    pf
  }

  def sumFactor(cpt: PlainCPT, variable: String): PlainCPT = {
    cpt.sumFactor(variable)
  }

  def prodFactor(unknown: String, cpts: Array[PlainCPT]): PlainCPT = {
    cpts
      .tail
      .foldLeft[PlainCPT](cpts.head) {
        case (prev, current) => prev.prodFactor(current)
      }
  }

  def makeInteractionGraph(
    inputGraph: Graph[String, DiEdge[String]],
    cptsInput: Array[PlainCPT]
  ): Graph[String, UnDiEdge[String]] = {
    var edges: List[UnDiEdge[String]] = List.empty
    cptsInput.foreach(
      cpt => {
        val header = cpt.header.filter(head => !head.equals("Probability"))
        val pairs = header.combinations(2)
        pairs.foreach(
          pair => edges = edges :+ UnDiEdge(pair(0), pair(1))
        )
      }
    )
    Graph.from(
      inputGraph.nodes.map(_.outer),
      edges
    )
  }

  def getOrdering(
    inputGraph: Graph[String, DiEdge[String]],
    queries: Array[String],
    cptsInput: Array[PlainCPT]
  ): Array[String] = {
    var graph: Graph[String, UnDiEdge[String]] = Graph.from(
      inputGraph.nodes.map(_.outer),
      inputGraph.edges.map(
        e => UnDiEdge(e.outer.source, e.outer.target)
      )
    ) // make it undirected
    val variables: Array[String] = graph
      .nodes
      .toList
      .map(n => n.outer)
      .toArray diff queries
    var interactionGraph: Graph[String, UnDiEdge[String]] =
      makeInteractionGraph(inputGraph, cptsInput)

    variables.map {
      _ =>
        val minVertex = interactionGraph
          .nodes
          .toList
          .filter(
            n => !queries.contains(n.outer)
          )
          .minBy(
            n => (n.degree, n.outer)
          )
          .outer
        val neighbors = (interactionGraph get minVertex)
          .neighbors
          .toList
          .map(_.outer)
        if (neighbors.length > 1) {
          val pairs = neighbors.combinations(2)
          while (pairs.hasNext) {
            val pair = pairs.next()
            interactionGraph = interactionGraph + UnDiEdge(pair(0), pair(1))
          }
        }
        interactionGraph = interactionGraph - minVertex
        minVertex
    }
  }

  def generateCPTs(
    variablesMap: Map[String, Variable],
    graph: Graph[String, DiEdge[String]],
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
          .map(n => variablesMap(n.outer))
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
    variableName: String,
    mutationModelData: Option[List[MutationModelData]] = None
  ): String = {
    val variableSex =
      if (variableName.endsWith("_p") || variableName.endsWith("_m")) {
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
    val sex = if (variableSex != "I" && !ignoreSex) { variableSex } else { "I" }
    sex
  }

  def getRowProbability(
    row: Array[Double],
    header: Array[String],
    variable: Variable,
    frequencyTable: FrequencyTable,
    linkage: Linkage,
    mutationModelType: Option[Long] = None,
    mutationModelData: Option[List[MutationModelData]] = None,
    n: Map[String, List[Double]] = Map.empty
  ): Double = {
    variable.kind match {
      case VariableKind.Genotype => {
        val node = getNode(row, header, variable.name)
        val s = getSelector(row, header)
        val ap = getAlleleFather(row, header, variable.name)
        val am = getAlleleMother(row, header, variable.name)
        val sex = getSex(variable.name, mutationModelData)
        if (s.contains(1) && ap.contains(node)) {
          getProbabityOfDiagonal(
            variable,
            mutationModelType,
            mutationModelData,
            sex
          )
        }
        else if (s.contains(2) && am.contains(node)) {
          getProbabityOfDiagonal(
            variable,
            mutationModelType,
            mutationModelData,
            sex
          )
        }
        else if (s.isEmpty && ap.isEmpty && am.isEmpty) {
          getFrequency(node, variable.marker, frequencyTable)
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

  def getProbabityOfDiagonal(
    variable: Variable,
    mutationModelType: Option[Long],
    mutationModelData: Option[List[MutationModelData]],
    sex: String = "I"
  ): Double = {
    mutationModelType match {
      case None => 1.0
      case Some(mutationType) => {
        val mutationMarkerDataOpt = mutationModelData
          .getOrElse(Nil)
          .find(
            x => x._1.locus == variable.marker && x._1.sex == sex
          )
        (mutationType, mutationMarkerDataOpt) match {
          case (1 | 2, Some(mutationMarkerData)) => {
            (
              1 - mutationMarkerData
                ._1.mutationRate
                .getOrElse(zero)
            )
              .doubleValue()
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
    sex: String = "I"
  ): Double = {
    mutationModelType match {
      case None => 0.0
      case Some(mutationType) => {
        val mutationMarkerDataOpt = mutationModelData
          .getOrElse(Nil)
          .find(x => x._1.locus == variable.marker && x._1.sex == sex)
        (mutationType, mutationMarkerDataOpt) match {
          case (1, Some(mutationMarkerData)) => {
            (mutationMarkerData._1.mutationRate.get / (n - 1)).doubleValue()
          }
          case (2, Some(mutationMarkerData)) => {
            val ki = mutationMarkerData._2
              .find(_.allele == allelei)
              .map(_.ki)
              .getOrElse(zero)
            val alleleDiff = (allelei - allelej).abs
            if (
              (alleleDiff <= mutationMarkerData._3.cantSaltos.toDouble) && (alleleDiff % 1 == 0)
            ) {
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
    n: Map[String, List[Double]] = Map.empty,
    individuals: Array[Individual]
  ): CPT = {
    val header = variables.map(_.name) :+ "Probability"
    val possibilities = variables.map {
      case Variable(_, _, VariableKind.Selector, _, _) => Array(1.0, 2.0)
      case Variable(name, _, VariableKind.Genotype, Some(alleles), false) =>
        getPossibilitiesAlleles(alleles, name, individuals)
      case Variable(_, _, VariableKind.Genotype, _, _) =>
        n.getOrElse(vertex.marker, Nil).toArray
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
      n
    )
    new CPT(vertex, header, matrix.iterator, matrix.size)
  }

  private def getPossibilitiesAlleles(
    alleles: Array[Double],
    variableName: String,
    individuals: Array[Individual]
  ): Array[Double] = {
    val individualName = variableName
      .substring(0, variableName.indexOf('_'))
    val individual = individuals
      .filter(ind => ind.alias.text.equals(individualName))
      .apply(0)
    val selector = variableName.charAt(variableName.length - 1)
    var ret = Array[Double]()
    if (heterocygoteFounder(individual, Option(alleles))) {
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
    mutationModelType: Option[Long] = None,
    mutationModelData: Option[List[MutationModelData]] = None,
    n: Map[String, List[Double]] = Map.empty
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
          n
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

  def getSubgraphs(graph: Graph[String, DiEdge[String]]): Array[Graph[String, DiEdge[String]]] = {
    graph.componentTraverser().map(component =>
      Graph.from(component.nodes.map(_.outer), component.edges.map(e => DiEdge(e.outer.source, e.outer.target)))).toArray
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
                        case OutOfLadderAllele(_, _) => true;
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
                      None // Trisomias
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
  ): (Map[String, Variable], Graph[String, DiEdge[String]]) = {
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
                      edges :+= DiEdge(mother, variable.name)
                  }
                  val selector = getVariableName(individual.alias, marker, "m_s")
                  edges :+= DiEdge(selector, variable.name)
                }

                if (attr == "p" && individual.idFather.isDefined) {
                  attrs.foreach {
                    fatherAttr =>
                      val father = getVariableName(individual.idFather.get, marker, fatherAttr)
                      edges :+= DiEdge(father, variable.name)
                  }
                  val selector = getVariableName(individual.alias, marker, "p_s")
                  edges :+= DiEdge(selector, variable.name)
                }

                if (heterocygoteNonFounder(individual, variable.alleles)) {
                  val pm = getVariableName(individual.alias, marker, "pm")
                  edges :+= DiEdge(variable.name, pm)
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
                  edges :+= DiEdge(previousSelector, selector)
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

          if (individual.idMother.isDefined) {
            allelesM = getAlleles(
              marker,
              individuals
                .find(_.alias == individual.idMother.get)
                .get
                .globalCode,
              profiles
            )
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
            allelesP = getAlleles(
              marker,
              individuals
                .find(_.alias == individual.idFather.get)
                .get.globalCode,
              profiles
            )
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
            allelesMutated(alleles, allelesM, allelesP) && mutationModelType.isEmpty

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

  def stateRemoval(cpts: Array[CPT]): Array[CPT] = {
    var cptOut: Map[String, Set[Double]] = Map.empty

    cpts.foreach {
      cpt =>
        var cpt0: Map[String, Set[Double]] = Map.empty
        var cpt1: Map[String, Set[Double]] = Map.empty

        if (cpt.header.size > 2) {
          var cptMatrix = Array[Array[Double]]()
          cptMatrix = cpt.matrix.toArray

          cptMatrix.foreach {
            row =>
              cpt.header.zipWithIndex.foreach {
                case (column, index) =>
                  if (column != "Probability" && row.last == 0) {
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
                row => { !allelesToRemove.contains(row(index)) }
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
    graph: Graph[String, DiEdge[String]]
  )(cpts: Array[CPT]): Array[CPT] = {
    val var_filter = (variable: Variable) => (
      variable.alleles.isEmpty &&
        !variable.unknown &&
        variable.kind != VariableKind.Heterocygote
    )
    val variables = graph
      .nodes
      .filter(_.outDegree == 0)
      .map(v => variablesMap(v.outer))
      .filter(var_filter)
      .map(_.name)
      .toList

    if (variables.nonEmpty) {
      nodePrunning(
        variablesMap,
        Graph.from(
          graph.nodes.collect { case n if !variables.contains(n.outer) => n.outer },
          graph.edges.collect { case e if !variables.contains(e.outer.source) && !variables.contains(e.outer.target) => e.outer }
        )
      )(
        cpts filter { cpt => !variables.contains(cpt.variable.name) }
      )
    } else {
      cpts
    }
  }

  def zeroProbabilityPrunning(cpts: Array[CPT]): Array[CPT] = {
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
          row => row.zipWithIndex.map {
            case (col, index) => if (index == row.length - 1) col / sum else col
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
