package pedigree

import java.util.Arrays.ArrayList

import kits.AnalysisType
import matching.{MatchingAlgorithm, NewMatchingResult}
import pedigree.PedigreeMatchingAlgorithm.extractMarker
import play.api.libs.json.Json
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
  private type MutationModelData = (
    MutationModelParameter,
      List[MutationModelKi],
      MutationModel
    )
  type FrequencyTable = Map[String, Map[Double, Double]]
//  type Matrix = Array[Array[Double]]
  type Matrix = Iterator[Array[Double]]
  type Linkage = Map[Marker, (Marker, Double)]
  
  // con este nombre se configura en el application.conf
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
  // Scenario or reclculate genotipification for new alleles
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
    locusRangeMap:NewMatchingResult.AlleleMatchRange = Map.empty
  ): Double = {
    val n = if(seenAlleles.isEmpty) {
      getNFromTable(frequencyTable)
    } else {
      seenAlleles
    }
    val starQueryProfilesTime = System.currentTimeMillis()
    var queryProfiles = getQueryProfiles(
      profiles,
      genogram,
      analysisType,
      frequencyTable
    )
    val endQueryProfilesTime = System.currentTimeMillis()
    if (verbose) {
      val deltaT = endQueryProfilesTime-starQueryProfilesTime
      logger.info(s"--- GetQueryProfiles: ${deltaT} ---")
    }
    val markers = queryProfiles.values.head.keys.toArray
    val linkageMarkers = linkage.keySet.union(linkage.values.map(_._1).toSet)
    val starNormalizedFrequencyTableTime = System.currentTimeMillis()
    val normalizedFrequencyTable = getNormalizedFrequencyTable(frequencyTable)
    val endNormalizedFrequencyTableTime = System.currentTimeMillis()
    if (verbose) {
      val deltaT = endNormalizedFrequencyTableTime -
        starNormalizedFrequencyTableTime
      logger.info(s"--- GetNormalizedFrequencyTable: ${deltaT} ---")
    }
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
    // TODO pasarle el modelo de mutacion, como cuando se llama desde
    //      PedigreeGenotificationService
    val endGenotypificationTime = System.currentTimeMillis()
    if (verbose) {
      val deltaT = endGenotypificationTime - startGenotypificationTime
      logger.info(s"--- GetGenotypification: ${deltaT} ---")
    }
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
      n
    )
    val endLRTime = System.currentTimeMillis()
    if (verbose) {
      logger.info(s"--- GetLR: ${endLRTime-startLRTime} ---")
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
    n: Map[String,List[Double]] = Map.empty
  ): (Double, String) = {
    val linkeage:Linkage = Map.empty
    //TODO: cambiar unknowns.head para que use todos los del array
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
      n
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
    n: Map[String, List[Double]] = Map.empty
  ) : (Double, String) = {
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
    val evidenceProbabilityLog = getEvidenceProbabilityLog(genotypificationFiltered)
//    print("------------------------------")
//    print(s"Evidence probability: ${evidenceProbability}!!!!!!")
//    print("---------------------\n")
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
//    print("------------------------------")
//    print(s"Query probability: ${queryProbability}!!!!!!")
//    print("---------------------\n")
    val genotypeProbabilityLog = getGenotypeProbabilityLog(
      queryProfilesFiltered,
      normalizedFrequencyTable
    )
//    print("----------------------")
//    print(s"Genotype probability: ${genotypeProbability}!!!!!!")
//    print("---------------------\n")
    val subjectProbabilityLog = evidenceProbabilityLog + genotypeProbabilityLog
    val lr = math.exp(
      queryProbabilityLog._1 - subjectProbabilityLog
    )
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
    n: Map[String,List[Double]]
  ): Array[Profile.Marker] = {
    val unknown = queryProfiles.keys.head
    var markersToFilter: Array[Profile.Marker] = Array.empty
    markers.foreach {
      marker => {
        val pVariable = s"${unknown}_${marker}_p"
        val mVariable = s"${unknown}_${marker}_m"
        if (queryProfiles(unknown).contains(marker)) {
          // si no tiene el marcador no hace nada
          val alleles = queryProfiles(unknown)(marker)
          val noAlleleInGenotypeAndN = ! isAnyAlellesInGenotipeAndN(
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
    n: Map[String,List[Double]]
  ): Array[Profile.Marker] = {
    val unknown = queryProfiles.keys.head
    var incompleteMarkers: Array[Profile.Marker] = Array.empty
    markers.foreach {
      marker => {
        val pVariable = s"${unknown}_${marker}_p"
        val mVariable = s"${unknown}_${marker}_m"
        // si no tiene el marcador no hace nada
        if (queryProfiles(unknown).contains(marker)) {
          val alleles = queryProfiles(unknown)(marker)
          // si hay alguno y no estan todos, es un marcador incompleto
          // con alelos nuevos
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
//        val allelesSinfrecMin = alleles.filter(x=>(!x._1.equals(-1.0)))
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
    if (verbose) {
      val deltaT = endGeneratingGraphTime-startGeneratingGraphTime
      print("------------------------------")
      print(s"GenerateGraph: ${deltaT}")
      print("----------------------\n")
    }
    val startSubgraphsGraphTime = System.currentTimeMillis()
    var subgraphs = getSubgraphs(graph)
    val endSubgraphsGraphTime = System.currentTimeMillis()
    if (verbose) {
      print("------------------------------")
      print(s"GetSubgraphs: ${endSubgraphsGraphTime-startSubgraphsGraphTime}")
      print("----------------------\n")
    }

    if (verbose){
      print("------------------------------")
      print(s"Cant subgraphs: ${subgraphs.length}")
      print("----------------------\n")
    }
    val unknown = genogram.filter(_.unknown).head.alias.text
    var cont = 0

    // DEBUG CODE ///
    subgraphs = subgraphs.filter(x=>x.toString().contains("ACTBP2"))
    logger.info(s"Length of subgraphs: ${subgraphs.length}")
    // END of DEBUG CODE

    val geno = subgraphs
      .zipWithIndex
      .flatMap {
        case (subgraph, index) =>
          val variables = variablesMap.filter {
            case (n, _) => subgraph.nodes.exists(_.toOuter == n)
          }
          if (verbose) {
            print("------------------------------")
            print(s"subgraphs : ${index}----------------------\n")
            print("------------------------------")
            print(s"subgraphs i, cant variables: ${variables.keys.size}")
            print(s"----------------------\n")
          }

          val startGeneratingCPTsTime = System.currentTimeMillis()
          val cpts = generateCPTs(variables, subgraph, frequencyTable, linkage, mutationModelType, mutationModelData, n, genogram)

//          cpts.foreach(
//            cpt => {
//              val matrix = extractMatrixFromCPT(cpt)
//              logger.info(s"CPT: ${cpt.header.mkString("/")} | ${
//                matrix.length match {
//                  case x if x <= 2 =>
//                    matrix
//                      .map(
//                        x => s"${x(0)} ${x(0)} ... ${x.length - 2} more."
//                      )
//                      .mkString(" ")
//                  case x => s"Array of length: ${x}"
//                }
//              }")
//            }
//          )

          cpts.foreach(
            cpt => {
              val matrix = extractMatrixFromCPT(cpt)
              logger.info(
                s"CPT: ${cpt.header.mkString("/")} | ${
                  s"Cant of last diff of zero: ${
                    matrix.count(x => x.last != 0)
                  }"
                }"
              )
            }
          )

          val endGeneratingCPTsTime = System.currentTimeMillis()

          val startPrunningCPTsTime = System.currentTimeMillis()
          val prunnedCPTs = pruneCPTs(variables, subgraph, cpts)
          val endPrunningCPTsTime = System.currentTimeMillis()

//          prunnedCPTs.foreach(
//            cpt => {
//              val matrix = extractMatrixFromCPT(cpt)
//              logger.info(s"CPT: ${cpt.header.mkString("/")} | ${
//                matrix.length match {
//                  case 0 => "Empty Array"
//                  case x if x <= 2 =>
//                    matrix
//                      .map(
//                        x => s"${x(0)} ${x(0)} ... ${x.length - 2} more."
//                      )
//                      .mkString(" ")
//                  case x => s"Array of length: ${x}"
//                }
//              }")
//            }
//          )

          if (verbose) {
            print("------------------------------")
            print(s"PruneCPTs: ${endPrunningCPTsTime - startPrunningCPTsTime}")
            print("----------------------\n")
            print("------------------------------")
            print(s"Cant. PruneCPTs: ${prunnedCPTs.length}")
            print("----------------------\n")
          }

          if (prunnedCPTs.nonEmpty) {
            val prunnedVariables = prunnedCPTs.map(_.variable.name)
            val prunnedGraph = subgraph filter subgraph.having(node = n => prunnedVariables.contains(n.toOuter))

            val queries = getQueryVariables(variables)
            val startVariableEliminationTime = System.currentTimeMillis()
            val ve = variableElimination(unknown, prunnedCPTs.map(_.getPlain()), queries, prunnedGraph, verbose)
            val endVariableEliminationTime = System.currentTimeMillis()
            if (verbose) {
              val deltaT = endVariableEliminationTime -
                startVariableEliminationTime
              print("------------------------------")
              print(s"VariableElimination: ${deltaT}----------------------\n")
            }
            Some(ve)
          } else {
            None
          }
      }
      .filter(_.matrix.nonEmpty)
    logger.info(s"--- Genotification Size: ${geno.length} ---")
    geno
  }

  def getQueryProbabilityLog(
    markers: Array[String],
    queryProfiles: Map[String, Map[Marker, Array[Double]]],
    genotypification: Array[PlainCPT],
    analysisType: AnalysisType,
    frequencyTable: FrequencyTable,
    linkage: Linkage, mutationModelType: Option[Long] = None,
    mutationModelData: Option[List[MutationModelData]] = None,
    n: Map[String, List[Double]] = Map.empty
  ): (Double, String) = {
    var cpts = genotypification
    var message = ""
    val unknown = queryProfiles.keys.head
    markers.foreach {
      marker => {
        val pVariable = s"${unknown}_${marker}_p"
        val mVariable = s"${unknown}_${marker}_m"
        // si no tiene el marcador no hace nada
        if (queryProfiles(unknown).contains(marker)) {
          val alleles = queryProfiles(unknown)(marker)
          // Si es sin mutaciones o todos los alelos estan en el genotipo toma
          // las prob normal,
          // sino toma la prob solo del alelo que esta y usa como prob del alelo
          // raro la menor prob de la variable en el genotipo del pedigree
          if (
            mutationModelType.isEmpty /*Sin mutaciones*/ ||
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
            ) //TODO pasarle los datos del modelo de mutacion
            val ukCPT = new PlainCPT(header, matrix.iterator, matrix.size)
            val dependentCPTs = cpts.filter(
              cpt => cpt.header.contains(pVariable) ||
                cpt.header.contains(mVariable)
            )
            val product = prodFactor(unknown, ukCPT +: dependentCPTs)
            cpts = (cpts diff dependentCPTs) :+ product
          } else {
            // es con mutaciones y no estan todos los alelos en el genotipo
            // Hay algun alelo que no esta en la genotipificacion, pero alguno
            // que si
            val alelleInGenotype = getAlelleInGenotype(
              marker, alleles, pVariable, mVariable, genotypification
            )
            val dependentCPTs = cpts.filter(
              cpt => cpt.header.contains(pVariable) ||
                cpt.header.contains(mVariable)
            )
            val header = Array(pVariable, mVariable) :+ "Probability"
            val probabilityPVariable : Double = getProbabilityOf(
              pVariable, alelleInGenotype, dependentCPTs
            )
            val probabilityMVariable : Double = getProbabilityOf(
              mVariable, alelleInGenotype, dependentCPTs
            )
            val minPorbabilityOfPVarible = getMinProbabilityofVariable(
              pVariable, dependentCPTs, marker, frequencyTable
            )
            val minPorbabilityOfMVarible = getMinProbabilityofVariable(
              mVariable, dependentCPTs, marker, frequencyTable
            )
/*
            println(s"------------------------------Variable ${mVariable} ---Min probability of: ${minPorbabilityOfMVarible}!!!!!!---------------------")
            println(s"------------------------------Variable ${pVariable} ---Min probability of: ${minPorbabilityOfPVarible}!!!!!!---------------------")
*/
            var x : Array[Double] = Array.empty
            //La probabilidad va a ser la suma de las probabilidades de la variable de la madre y la variable del padre en el genotipo del alelo que si está
            x = x ++ alleles :+ (
              (probabilityMVariable * minPorbabilityOfPVarible) +
                (probabilityPVariable * minPorbabilityOfMVarible)
            )

            val matrixResult = ArrayBuffer(x)
            val cptResult = new PlainCPT(header, matrixResult.iterator, matrixResult.size)
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

  private def getProbabilityOf(variable: String, alelle: Double, cpts: Array[PlainCPT]) : Double = {
    val cptsOfVariable = cpts.filter(cpt => cpt.header.contains(variable))
    var probability = 0.0
    cptsOfVariable.foreach{
      cpt => {
        val matrixArray = cpt.matrix.toArray
        cpt.matrix = matrixArray.iterator
        probability = matrixArray.filter(elem => elem.contains(alelle)).apply(0).last
      }
    }

    probability
  }

  private def getCountMinFrecbeforeMin( marker: String, frequencyTable: FrequencyTable) : Int = {
    val frecMin = frequencyTable.getOrElse(marker, Map.empty).get(-1.0)

    val menorFrecMin = frequencyTable.getOrElse(marker, Map.empty).filter(tuple => (tuple._2.<(frecMin.get)))

    val cantMenores = menorFrecMin.keys.size
    cantMenores
  }

  private def getMinProbabilityofVariable(variable: String, cpts: Array[PlainCPT], marker: String, frequencyTable: FrequencyTable) : Double = {
    val cptsOfVariable = cpts.filter(cpt => cpt.header.contains(variable))
//    val cantMinFrecBeforeMin = getCountMinFrecbeforeMin(marker, frequencyTable)
    var probability = 0.0
    cptsOfVariable.foreach {
      cpt => {
        val matrixArray = cpt.matrix.toArray
        cpt.matrix = matrixArray.iterator
        probability = matrixArray.minBy(elem => elem.last).last
/*
        val matrixArraYOrderer = matrixArray.sortBy(elem => elem.last)
        if ((matrixArraYOrderer.length >= cantMinFrecBeforeMin) && (cantMinFrecBeforeMin != 0))
          probability = matrixArraYOrderer.apply(cantMinFrecBeforeMin-1).last
        else
          probability = matrixArraYOrderer.apply(0).last
*/

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
    n: Map[String,List[Double]]
  ) : Boolean = {
    val isInN = alelles.forall(
      n.getOrElse(marker, List.empty)
        .contains(_)
    )
    val markerCpts = genotypification.filter(
      cpt => cpt.header.contains(mVariable) || cpt.header.contains(pVariable)
    )
    val alellesInGenotype = alelles.filter(
      allele => markerCpts.forall(
        cpt=> {
          val matrixArray = cpt.matrix.toArray
          cpt.matrix = matrixArray.iterator
          matrixArray
            .map(_.apply(0))
            .contains(allele)
        }
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
    val isInN = alelles.forall(n.getOrElse(marker, List.empty).contains(_))


    val markerCpts = genotypification.filter(
      cpt => cpt.header.contains(mVariable) || cpt.header.contains(pVariable)
    )
    val isInGenotipe = alelles.forall(
      allele => markerCpts.forall(
        cpt=> {
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
  ) : Double = {
    val markerCpts = genotypification
      .filter(
        cpt =>cpt.header.contains(mVariable) || cpt.header.contains(pVariable)
      )
    val alellesInGenotype = alelles.filter(
      allele => markerCpts.forall(
        cpt=> {
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
   *
   * @param cpts: Array[PlainCPT]
   * @return The probabilty of teh Evidence as natural log to avoid precision
   *         overflow.
   */
  def getEvidenceProbabilityLog(cpts: Array[PlainCPT]): Double = {
    val sums = cpts
      .map(
        cpt => {
//          val matrixArray = cpt.matrix.toArray
//          // Here, We need to 're-fill' the matrix iterator after consuming it
//          // in the previous line.
//          cpt.matrix = matrixArray.iterator
          val matrixArray = extractMatrixFromPlainCPT(cpt)
          matrixArray
            .map(_.last)
            .sum
        }
      )
    val logs_sum = sums
      // Here all zero values are removed to avoid errors when computing logs.
      // However, no element should be zero here.
      .filter(n => n > 0)
      .map(n => math.log(n))
      .sum
    logs_sum
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
  ): Double = {
    val probs = queryProfiles.head._2.map {
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
    probs
      .filter(n => n>0)
      .map(n => math.log(n))
      .sum
  }

  def getFrequency(
    allele: Double,
    marker: Marker,
    frequencyTable: FrequencyTable
  ): Double = {
/*
    if (frequencyTable(marker).get(allele).isEmpty || allele.equals(-1.0)) {
      println(s"------------------------------Busca la frecuencia minima de ${allele} y ${marker}:: ${frequencyTable(marker).getOrElse(allele, frequencyTable(marker)(-1))}----------")
    }
    frequencyTable(marker).getOrElse(allele, frequencyTable(marker)(-1))
*/
    // Para cualquier frecuencia menor a la minima se toma la
    // frecuencia minima por definicion de Mariana Herrera - 01-2020
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
//      stateRemoval(_) //,
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
    //printCPTs(cpts)
    val startGetOrderingTime = System.currentTimeMillis()
    var sortedVariables = getOrdering(graph, queries, cptsInput)
    val endGetOrderingTime = System.currentTimeMillis()
//    if (verbose) println(s"--- GetOrdering: ${endGetOrderingTime-startGetOrderingTime} ---")

//    if (verbose) println(s"--- Cant variable: ${sortedVariables.length} ---")
    sortedVariables
      .zipWithIndex
      .foreach {
        case (variable, index) =>
//          if (verbose) println(s"--- Variable: ${variable} ---")
          val dependentCPTs = cpts.filter(_.header.contains(variable))
    //      if (verbose) println(s"--- Cant dependentCPTs: ${dependentCPTs.length} ---")
          if (dependentCPTs.nonEmpty) {
            val startProdFactor = System.currentTimeMillis()
            val product = prodFactor(unknown, dependentCPTs)
            val endProdFactor = System.currentTimeMillis()
    //        if (verbose) println(s"--- ProdFactor: ${endProdFactor-startProdFactor} ---")
            val startSumFactor = System.currentTimeMillis()
            val cpt = sumFactor(product, variable)
            val endSumFactor = System.currentTimeMillis()
    //        if (verbose) println(s"--- SumFactor: ${endSumFactor-startSumFactor} ---
            // Replace all dependent factors by result cpt
            cpts = (cpts diff dependentCPTs) :+ cpt
          }
    }
    val startFinalProdFactor = System.currentTimeMillis()
    val pf = prodFactor(unknown, cpts)
    val endFinalProdFactor = System.currentTimeMillis()
//    if (verbose) println(s"--- Final ProdFactor: ${endFinalProdFactor-startFinalProdFactor} ---")
    if (verbose) {
      logger.info(
        s"""--- Plain CPT cant header: ${
          pf.header.foldLeft("")((acum, h) => acum ++ " " ++ h)
        } ---"""
      )
    }
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
/*
    cptsInput.foreach(cpt => {
      var header = cpt.header
      val pairs = header.combinations(2)
      while (pairs.hasNext) {
        val pair = pairs.next()
        graph.+(pair(0) ~ pair(1))
      }
    })
*/
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
    ) // make it undirected
    val variables: Array[String] = graph
      .nodes
      .toList
      .map(n => n.toOuter)
      .toArray diff queries
    var interactionGraph : Graph[String, UnDiEdge] =
      makeInteractionGraph(inputGraph, cptsInput)
/*
    variables.map { _ =>
      val minVertex = graph.nodes.toList.filter(n => !queries.contains(n.toOuter)).minBy(n => (n.degree, n.toOuter)).toOuter
      val neighbors = (graph get minVertex).neighbors.toList.map(_.toOuter)

      if (neighbors.length > 1) {
        val pairs = neighbors.combinations(2)
        while (pairs.hasNext) {
          val pair = pairs.next()
          graph += pair(0) ~ pair(1)
        }
      }
      graph = graph -! minVertex

      minVertex
    }
*/
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
  def getRowProbability(
    row: Array[Double],
    header: Array[String],
    variable: Variable,
    frequencyTable: FrequencyTable,
    linkage: Linkage,
    mutationModelType: Option[Long] = None,
    mutationModelData: Option[List[MutationModelData]] = None,
    n: Map[String,List[Double]] = Map.empty
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
            sex
          )
        }
        // TODO aca poner la probabilidad para el i=j de la tabla de
        // mutaciones si tiene modelo de mutacion definido )
        else if (s.contains(2) && am.contains(node)) {
          getProbabityOfDiagonal(
            variable,
            mutationModelType,
            mutationModelData,
            sex
          )
        }
        // TODO aca poner la probabilidad para el i=j de la tabla de mutaciones
        // si tiene modelo de mutacion definido
        else if (s.isEmpty && ap.isEmpty && am.isEmpty) {
          getFrequency(node, variable.marker, frequencyTable)
        }
        else {
          // TODO aca poner la probabilidad de la tabla de mutaciones con indice
          //  ap o am (de acuerdo al selector) y node
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
    sex:String = "I"
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
          case (1|2,Some(mutationMarkerData)) /*Equals o Stepwise*/ => {
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
    sex:String = "I"
  ) : Double= {
    mutationModelType match {
      case None => 0.0
      case Some(mutationType) => {
        val mutationMarkerDataOpt = mutationModelData
          .getOrElse(Nil)
          .find(x => x._1.locus == variable.marker && x._1.sex == sex)
        (mutationType, mutationMarkerDataOpt) match {
          case (1, Some(mutationMarkerData)) /*Equals*/ => {
            (mutationMarkerData._1.mutationRate.get/(n-1)).doubleValue()
          }
          case (2, Some(mutationMarkerData)) /*Stepwise*/ => {
            val ki = mutationMarkerData._2
              .find(_.allele == allelei)
              .map(_.ki)
              .getOrElse(zero)
            // si es menor o igual a la cant de saltos del modelo y es salto
            // entero
            val alleleDiff = (allelei - allelej).abs
            if (
                (
                  alleleDiff <= mutationMarkerData._3.cantSaltos.toDouble
                ) && (
                  alleleDiff % 1 == 0
                )
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
    n: Map[String,List[Double]] = Map.empty,
    individuals: Array[Individual]
  ): CPT = {
    val header = variables.map(_.name) :+ "Probability"
    val possibilities = variables.map {
      case Variable(_, _, VariableKind.Selector, _, _) => Array(1.0,2.0)
      case Variable(name, _, VariableKind.Genotype, Some(alleles), false) =>
        getPossibilitiesAlleles(alleles, name, individuals)// alleles
      case Variable(_, _, VariableKind.Genotype, _, _) =>
        n.getOrElse(vertex.marker, Nil).toArray
      //frequencyTable.getOrElse(vertex.marker, Map.empty).keys.toArray.filterNot(d => d.equals(-1.0))
      /*{
        if(mutationModelType.isEmpty) {
           frequencyTable.get(vertex.marker).get.keySet.toArray.filterNot(nro => nro.equals(-1.0))
        } else {
          n.getOrElse(vertex.marker, Nil).toArray
        }
      }*/
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
    //si es fundador y heterocigota //padre primer alelo madre segundo alelo
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

/*
  def generatePermutations(lists: Matrix, header: Array[String], variable: Variable, frequencyTable: FrequencyTable, linkage: Linkage,
                            mutationModelType: Option[Long]=None, mutationModelData: Option[List[(MutationModelParameter, List[MutationModelKi],MutationModel)]]=None,
                           n: Map[String,List[Double]] = Map.empty): Matrix = {
*/
/*
  def generatePermutations(lists: Array[Array[Double]], header: Array[String], variable: Variable, frequencyTable: FrequencyTable, linkage: Linkage,
                         mutationModelType: Option[Long]=None, mutationModelData: Option[List[(MutationModelParameter, List[MutationModelKi],MutationModel)]]=None,
                         n: Map[String,List[Double]] = Map.empty): Matrix = {
*/
def generatePermutations(
  lists: Array[Array[Double]],
  header: Array[String],
  variable: Variable,
  frequencyTable: FrequencyTable,
  linkage: Linkage,
  mutationModelType: Option[Long]=None,
  mutationModelData: Option[List[MutationModelData]]=None,
  n: Map[String,List[Double]] = Map.empty
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
    var edges = List.empty[DiEdge[String]] // no puede ser array
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

          if (mutationsWithoutMutationModel /* && TODO modelo sin mutaciones */) {
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
          //El foreach borra todos los elementos del iterador, pasarlos a un array para recorrerlos
          var cptMatrix = Array[Array[Double]]()
          cptMatrix = cpt.matrix.toArray

  //        cpt.matrix.foreach { row =>
          cptMatrix.foreach {
            row =>
              cpt.header.zipWithIndex.foreach {
                case (column, index) =>
                  if (column!= "Probability" && row.last == 0) {
                    cpt0 += (column -> (cpt0.getOrElse(column, Set.empty) + row(index)))
                    logger.info(s"--- Adding to CPT1: ${column} ---")
                  } else if (column != "Probability" && row.last != 0) {
                    cpt1 += (column -> (cpt1.getOrElse(column, Set.empty) + row(index)))
                    logger.info(s"--- Adding to CPT2: ${column} ---")
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

    logger.info(s"--- cptOut = ${cptOut.keys.mkString(" ")} ---")

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
  /*
           matrix.map(row => {
             val rowArray = row.toArray
             if(!allelesToRemove.contains(rowArray(index))) rowArray.iterator else Iterator.empty
           }).filterNot(rowIt => rowIt.isEmpty)
  */
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

//    logger.info(s"--- Node Prunning: Variable is empty=${variables.isEmpty}")

    if (variables.nonEmpty) {
      // todo: hacerlo con --! y List[Param[String,DiEdge]]
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
/*
      cpt.matrix = cpt.matrix.map(row => {
        val rowArray = row.toArray
        if( rowArray.last > 0) rowArray.iterator else Iterator.empty
      }).filterNot(rowIt => rowIt.isEmpty)
*/
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
        val sum = matrix1.map(row => row.last).sum //todo: 1
        cpt.matrix = matrixCopy.map(
          row => row.zipWithIndex.map{
            case (col, index) => if (index == row.length-1) col/sum else col //todo: 2
          }
        )
/*
      val matrixArray = cpt.matrix.map(rowIt => rowIt.toArray).toArray
      val sum = matrixArray.map(row => row.last).sum //todo: 1
      cpt.matrix = matrixArray.map(row => row.zipWithIndex.map{
        case (col, index) => if(index == row.length-1) col/sum else col //todo: 2
      }).map(row => row.iterator).iterator
*/
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
    // TODO: Why to use zipWithIndex if the index is not used later?
    header
      .zipWithIndex
      .find { case (key, _) => condition(key) }
      .map(_._2)
  }

}
