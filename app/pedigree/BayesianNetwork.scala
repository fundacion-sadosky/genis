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
    locusRangeMap:NewMatchingResult.AlleleMatchRange = Map.empty,
    maxExclusionsAllowed: Int = 0
  ): (Double, Map[String, MarkerLRDetail]) = {
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
//    if (verbose) {
//      val deltaT = endQueryProfilesTime-starQueryProfilesTime
//      logger.info(s"--- GetQueryProfiles: ${deltaT} ---")
//    }
    val markers = queryProfiles.values.head.keys.toArray
    val linkageMarkers = linkage.keySet.union(linkage.values.map(_._1).toSet)
    val starNormalizedFrequencyTableTime = System.currentTimeMillis()
    val normalizedFrequencyTable = getNormalizedFrequencyTable(frequencyTable)
    val endNormalizedFrequencyTableTime = System.currentTimeMillis()
//    if (verbose) {
//      val deltaT = endNormalizedFrequencyTableTime -
//        starNormalizedFrequencyTableTime
//      logger.info(s"--- GetNormalizedFrequencyTable: ${deltaT} ---")
//    }
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
//    if (verbose) {
//      val deltaT = endGenotypificationTime - startGenotypificationTime
//      logger.info(s"--- GetGenotypification: ${deltaT} ---")
//    }
    // Genotipificacion "estricta" (sin modelo mutacional), calculada
    // aparte, solo para poder clasificar el detalle por marcador del
    // escenario (exclusion vs. salto mutacional) independientemente de lo
    // que el modelo mutacional real pueda "explicar" — mismo mecanismo
    // que PedigreeGenotypificationService.saveGenotypification usa para
    // el matching de pedigries. Si no hay modelo mutacional configurado,
    // no hace falta recalcular: ya son la misma.
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
      strictGenotypification
    )
    val endLRTime = System.currentTimeMillis()
    if (verbose) {
      logger.info(s"--- GetLR: ${endLRTime-startLRTime} ---")
      logger.info(s"--- LR: ${lr._1} ---")
    }
    (lr._1, lr._4)
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
    n: Map[String,List[Double]] = Map.empty,
    maxExclusionsAllowed: Int = 0,
    strictGenotypification: Array[PlainCPT] = Array.empty
  ): (Double, String, List[String], Map[String, MarkerLRDetail]) = {
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
      n,
      maxExclusionsAllowed,
      strictGenotypification
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
    strictGenotypification: Array[PlainCPT] = Array.empty
  ) : (Double, String, List[String], Map[String, MarkerLRDetail]) = {
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
    // getQueryProbabilityLog consume (via prodFactor) el iterador de
    // matrix de los CPTs que recibe, sin resetearlo. genotypificationFiltered
    // se vuelve a usar mas abajo para calcular la evidencia del
    // denominador (evidenceGenotypification) — si le pasaramos los
    // mismos objetos, quedarian "vacios" para esa segunda lectura aunque
    // los datos originales esten intactos. Por eso getQueryProbabilityLog
    // trabaja sobre una COPIA independiente (mismos datos, iteradores
    // propios), dejando genotypificationFiltered intacto para reutilizar.
    val genotypificationForQuery = genotypificationFiltered.map { cpt =>
      val arr = cpt.matrix.toArray
      cpt.matrix = arr.iterator
      new PlainCPT(cpt.header, arr.iterator, arr.length)
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
      strictGenotypification
    )
    // Los marcadores con exclusion mendeliana tolerada ya quedan afuera
    // del numerador (queryProbabilityLog los excluye del calculo). Para
    // que su contribucion sea realmente neutra (multiplicador 1 en el LR)
    // y no una penalizacion, tambien hay que sacarlos del denominador
    // (evidencia familiar + probabilidad del genotipo del candidato) —
    // si no, el denominador sigue "cobrando" un marcador que el
    // numerador no aporta, y el LR queda artificialmente bajo aunque el
    // marcador este tolerado.
    val toleratedMarkers = queryProbabilityLog._3.toArray
    val evidenceGenotypification = filterGenotypification(
      genotypificationFiltered,
      toleratedMarkers
    )
    val evidenceProbabilityLog = getEvidenceProbabilityLog(evidenceGenotypification)
    val genotypeProbabilityLog = getGenotypeProbabilityLog(
      filterQueryProfile(queryProfilesFiltered, toleratedMarkers),
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
          // si no tiene el marcador no hace nada
          val alleles = queryProfiles(unknown)(marker)
          // Un marcador solo se descarta del calculo de LR cuando el
          // alelo observado es realmente desconocido para la poblacion
          // (no figura en la tabla de frecuencias `n`). Si el alelo
          // existe en `n` pero no es alcanzable a partir del genotipo
          // familiar conocido (isAnyAlellesInGenotipeAndN), NO se
          // descarta: es una exclusion mendeliana real y debe pesar en
          // el LR (via el CPT/producto de factores, o via el modelo de
          // mutacion si hay uno configurado), no desaparecer del calculo
          // como si no se hubiera tipificado.
          val noAlleleInN = ! isAlleleInN(marker, alleles, n)
          if (noAlleleInN) {
            markersToFilter = markersToFilter ++ List(marker)
          }
        }
      }
    }
    markersToFilter
  }

  private def isAlleleInN(
    marker: String,
    alelles: Array[Double],
    n: Map[String,List[Double]]
  ) : Boolean = {
    alelles.forall(
      n.getOrElse(marker, List.empty)
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
//    if (verbose) {
//      val deltaT = endGeneratingGraphTime-startGeneratingGraphTime
//      logger.info(s"--- GenerateGraph: ${deltaT} ---")
//    }
    val startSubgraphsGraphTime = System.currentTimeMillis()
    val subgraphs = getSubgraphs(graph)
    val endSubgraphsGraphTime = System.currentTimeMillis()
//    if (verbose) {
//      logger.info(s"--- GetSubgraphs: ${
//        endSubgraphsGraphTime-startSubgraphsGraphTime
//      } ---")
//    }

//    if (verbose){
//      logger.info(s"--- Cant subgraphs: ${subgraphs.length} ---")
//    }
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
//          if (verbose) {
//            logger.info(s"--- subgraphs: ${index} ---")
//            logger.info(s"--- subgraphs i, cant variables: ${
//              variables.keys.size
//            } ---")
//          }

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

//          if (verbose) {
//            logger.info(s"--- PruneCPTs: ${
//              endPrunningCPTsTime - startPrunningCPTsTime
//            } ---")
//            logger.info(s"--- Cant. PruneCPTs: ${prunnedCPTs.length} ---")
//          }

          if (prunnedCPTs.nonEmpty) {
            val prunnedVariables = prunnedCPTs.map(_.variable.name)
            val prunnedGraph = subgraph filter subgraph.having(node = n => prunnedVariables.contains(n.toOuter))

            val queries = getQueryVariables(variables)
            val startVariableEliminationTime = System.currentTimeMillis()
            val ve = variableElimination(unknown, prunnedCPTs.map(_.getPlain()), queries, prunnedGraph, verbose)
            val endVariableEliminationTime = System.currentTimeMillis()
//            if (verbose) {
//              val deltaT = endVariableEliminationTime -
//                startVariableEliminationTime
//              logger.info(s"--- VariableElimination: ${deltaT} ---")
//            }
            Some(ve)
          } else {
            None
          }
      }
      .filter(_.matrix.nonEmpty)
//    logger.info(s"--- Genotification Size: ${geno.length} ---")
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
    n: Map[String, List[Double]] = Map.empty,
    maxExclusionsAllowed: Int = 0,
    strictGenotypification: Array[PlainCPT] = Array.empty
  ): (Option[Double], String, List[String], Map[String, MarkerLRDetail]) = {
    var cpts = genotypification
    var message = ""
    val unknown = queryProfiles.keys.head
    // LR individual por marcador (para el detalle del reporte de
    // escenario): se llena a medida que se procesa cada marcador en el
    // loop principal, mas abajo.
    val markerQueryLog = scala.collection.mutable.Map[String, Double]()

    // Deteccion de exclusiones mendelianas ESTRICTAS (sin modelo
    // mutacional), para decidir cuales tolerar: para cada marcador se
    // arma el CPT del candidato y se lo cruza (join) contra los CPTs
    // "estrictos" de la familia (calculados sin ningun modelo de
    // mutacion, sea cual sea el configurado para el LR real). Si el join
    // no produce ninguna fila, el candidato no comparte ningun alelo
    // directo con la familia en ese locus — una exclusion mendeliana real
    // e independiente del modelo mutacional (que podria "explicarla" via
    // una mutacion de pocos pasos, pero eso no es lo que el usuario
    // configura como tolerancia).
    // Si no vino strictGenotypification (pedigries activados antes de
    // este cambio), se usa la genotipificacion real como fallback: puede
    // subestimar exclusiones si el modelo mutacional es muy permisivo.
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
            // PlainCPT.prodFactor consume el iterador de matrix de AMBOS
            // operandos sin resetearlo despues: es seguro en el uso
            // original porque cada CPT se descartaba tras usarse una vez.
            // Aca dependentCPTs son objetos COMPARTIDOS por referencia
            // (se pueden volver a leer para otro marcador o mas abajo),
            // asi que hay que restaurarles el iterador despues del join.
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

    // De las exclusionMarkers de arriba, solo las que el modelo mutacional
    // activo NO puede explicar (o no hay modelo configurado) son
    // candidatas a tolerancia. Si el modelo SI las explica — misma logica
    // que las ramas normales del loop principal, mas abajo (linea
    // "es con mutaciones y no estan todos los alelos en el genotipo") —
    // se prefiere puntuarlas via el modelo mutacional en vez de sacarlas
    // del calculo: la tolerancia es para exclusiones que el modelo no
    // logra justificar, no para reemplazar el calculo del modelo cuando
    // si puede. Se evalua con una "sonda" que replica exactamente esa
    // rama sin tocar el acumulador `cpts` (que en este punto, antes del
    // loop principal, todavia es igual a `genotypification`).
    val unexplainableExclusions: Set[String] = exclusionMarkers.filter { marker =>
      val alleles = queryProfiles(unknown)(marker)
      val pVariable = s"${unknown}_${marker}_p"
      val mVariable = s"${unknown}_${marker}_m"
      if (mutationModelType.isEmpty) {
        true
      } else if (isAllAlellesInGenotipeAndN(marker, alleles, pVariable, mVariable, genotypification, n)) {
        // El CPT con modelo mutacional ya incluye este alelo en su
        // dominio (join exacto explicara la mutacion): explicable.
        false
      } else {
        val dependentCPTs = genotypification.filter(
          cpt => cpt.header.contains(pVariable) || cpt.header.contains(mVariable)
        )
        val alelleInGenotypeOpt = getAlelleInGenotype(
          marker, alleles, pVariable, mVariable, genotypification
        )
        // Misma formula que la rama "es con mutaciones" del loop
        // principal, mas abajo: frecuencia poblacional del alelo no
        // rastreado, no el minimo del CPT.
        val probability = alelleInGenotypeOpt match {
          case Some(alelleInGenotype) =>
            val otherAllele = alleles.find(_ != alelleInGenotype).getOrElse(alelleInGenotype)
            val otherAlleleFrequency = getFrequency(otherAllele, marker, frequencyTable)
            val probabilityPVariable: Double = getProbabilityOf(
              pVariable, alelleInGenotype, dependentCPTs
            )
            val probabilityMVariable: Double = getProbabilityOf(
              mVariable, alelleInGenotype, dependentCPTs
            )
            (probabilityMVariable * otherAlleleFrequency) +
              (probabilityPVariable * otherAlleleFrequency)
          case None =>
            val freq0 = getFrequency(alleles(0), marker, frequencyTable)
            val freq1 = if (alleles.length > 1) getFrequency(alleles(1), marker, frequencyTable) else freq0
            freq0 * freq1
        }
        // probability > 0: el modelo mutacional le asigna una probabilidad
        // real (paso alcanzable dentro de cantSaltos) => explicable, no
        // tolerar. probability == 0 (paso fuera de rango, o sin datos):
        // el modelo no la explica => candidata a tolerancia.
        probability <= 0
      }
    }

    val toleratedExclusions: Set[String] =
      if (unexplainableExclusions.size <= maxExclusionsAllowed) unexplainableExclusions else Set.empty

    logger.info(
      s"--- Tolerancia exclusiones: unknown=$unknown maxAllowed=$maxExclusionsAllowed " +
      s"exclusionMarkers=${exclusionMarkers.mkString(",")} " +
      s"noExplicablesPorModelo=${unexplainableExclusions.mkString(",")} " +
      s"toleradas=${toleratedExclusions.mkString(",")} ---"
    )

    // Log de la evidencia familiar CRUDA por marcador (antes de cruzarla
    // con el candidato), para el detalle de LR por marcador. Se calcula
    // ANTES del loop principal porque ese loop hace joins (prodFactor)
    // que consumen el iterador de estos mismos CPTs compartidos — leerlo
    // despues encontraria todo vacio (el mismo problema que con
    // dependentCPTs en la deteccion de exclusiones, mas arriba).
    val markerEvidenceLog: Map[String, Double] = markers.flatMap {
      marker =>
        if (!queryProfiles(unknown).contains(marker)) {
          None
        } else {
          val pVariable = s"${unknown}_${marker}_p"
          val mVariable = s"${unknown}_${marker}_m"
          val rawFamilyCpts = genotypification.filter(
            cpt => cpt.header.contains(pVariable) || cpt.header.contains(mVariable)
          )
          val rows = rawFamilyCpts.flatMap(extractMatrixFromPlainCPT)
          if (rows.isEmpty) None else Some(marker -> math.log(rows.map(_.last).sum))
        }
    }.toMap

    markers.foreach {
      marker => {
        val pVariable = s"${unknown}_${marker}_p"
        val mVariable = s"${unknown}_${marker}_m"
        // si no tiene el marcador no hace nada
        if (queryProfiles(unknown).contains(marker)) {
          val alleles = queryProfiles(unknown)(marker)
          if (toleratedExclusions.contains(marker)) {
            // Exclusion mendeliana estricta tolerada: el marcador no debe
            // aportar evidencia al LR (como si no se hubiera tipificado),
            // sea cual sea la rama (con o sin modelo mutacional) que le
            // hubiera tocado. Hay que sacar del acumulador el CPT
            // familiar crudo de este marcador (no solo dejar de
            // agregarle el producto con el candidato): si quedara
            // adentro y tuviera 0 filas (puede pasar si el modelo de
            // mutacion podo todas las combinaciones), getEvidenceProbabilityLog
            // devuelve None para TODO el calculo, no solo para este
            // marcador.
            val dependentCPTs = cpts.filter(
              cpt => cpt.header.contains(pVariable) ||
                cpt.header.contains(mVariable)
            )
            cpts = cpts diff dependentCPTs
          } else if (
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
              markerQueryLog(marker) = math.log(productRows.map(_.last).sum)
            }
          } else {
            // es con mutaciones y no estan todos los alelos en el genotipo.
            // Puede ser que falte uno solo (mutacion aislada: uno de los
            // alelos si esta en el genotipo familiar) o que falten los dos
            // (exclusion completa en este locus, solo posible ahora que el
            // marcador ya no se descarta del calculo).
            val alelleInGenotypeOpt = getAlelleInGenotype(
              marker, alleles, pVariable, mVariable, genotypification
            )
            val dependentCPTs = cpts.filter(
              cpt => cpt.header.contains(pVariable) ||
                cpt.header.contains(mVariable)
            )
            val header = Array(pVariable, mVariable) :+ "Probability"
            // El alelo que no se puede rastrear hasta un ancestro
            // genotipificado (mutado, o simplemente heredado de una rama
            // del pedigri sin genotipificar — ej. un abuelo genotipificado
            // pero padre no) se modela con su frecuencia poblacional real,
            // no con el minimo de probabilidad del CPT familiar. Ese
            // minimo representa el caso mutacional mas improbable de todo
            // el dominio del CPT y subestima brutalmente la probabilidad
            // de un alelo comun que solo no aparece en el CPT porque viene
            // de un ancestro sin genotipificar (ver seccion 10 del doc:
            // caso FAM9, PI1 con abuelo paterno genotipificado pero padre
            // no — un alelo perfectamente normal tiraba el LR a ~0).
            val probability = alelleInGenotypeOpt match {
              case Some(alelleInGenotype) =>
                // Un alelo si esta en el genotipo familiar: su probabilidad
                // real de esa variable, mas la frecuencia poblacional del
                // alelo no rastreado en la otra variable.
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
                // Ningun alelo del candidato es alcanzable desde la familia:
                // se tratan los dos como no rastreados, usando la
                // frecuencia poblacional real de cada uno.
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
              markerQueryLog(marker) = math.log(probability)
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

    // Detalle de LR por marcador (para el reporte de escenario): mismo
    // razonamiento que el LR total (query - evidencia - genotipo) pero
    // aislado por marcador, ya que bajo independencia de loci el LR total
    // es el producto de los LR individuales. Los marcadores tolerados no
    // participan del calculo (LR neutro = 1.0, ya excluidos por diseño);
    // el resto se clasifica como "mutation" si no tiene un alelo
    // compartido de forma directa con la familia (exclusionMarkers) o
    // "normal" si si lo tiene.
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

    (evidenceProbability, message, toleratedExclusions.toList, markerDetails)
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
        // Puede no haber ninguna fila para este alelo (CPT podado por
        // probabilidad 0 tras aplicar el modelo de mutacion): en ese caso
        // no hay probabilidad real que usar, se mantiene 0.0 en vez de
        // reventar con un .apply(0) sobre un array vacio.
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
  ) : Option[Double] = {
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
    // Puede no haber ningun alelo alcanzable desde la familia (exclusion
    // completa en este locus): antes del fix del bug de descarte de
    // marcadores ese caso nunca llegaba aca, porque el marcador se
    // eliminaba del calculo entero. Ahora que las exclusiones reales
    // pesan en el LR, este metodo debe devolver None en vez de asumir
    // que siempre hay al menos un alelo compatible.
    alellesInGenotype.headOption
  }

  /**
   * Returns the natural-log-probability of given evidence CPTs.
   *
   * @param cpts : Array[PlainCPT]
   * @return The probabilty of teh Evidence as natural log to avoid precision
   *         overflow. We use None, when prob is Zero, and cannot compute log(0).
   */
  def getEvidenceProbabilityLog(cpts: Array[PlainCPT]): Option[Double] = {
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
            // Replace all dependent factors by result cpt
            cpts = (cpts diff dependentCPTs) :+ cpt
          }
    }
    val startFinalProdFactor = System.currentTimeMillis()
    val pf = prodFactor(unknown, cpts)
    val endFinalProdFactor = System.currentTimeMillis()
//    if (verbose) {
//      logger.info(
//        s"""--- Plain CPT cant header: ${
//          pf.header.foldLeft("")((acum, h) => acum ++ " " ++ h)
//        } ---"""
//      )
//    }
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
            // Distancia entre alelos, en unidades de repeticion. Se admiten
            // pasos no enteros (microvariantes, ej. 13 -> 13.2 o 13 -> 15.2)
            // con la misma formula de decaimiento geometrico: cuanto mayor
            // la distancia (entera o fraccionaria), menor la probabilidad.
            // Solo se excluye si la distancia supera la cantidad maxima de
            // saltos configurada en el modelo.
            val alleleDiff = (allelei - allelej).abs
            if (alleleDiff <= mutationMarkerData._3.cantSaltos.toDouble) {
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

          // Si el padre o la madre contra quien se compara es el nodo
          // "desconocido" (una hipotesis bajo evaluacion, ej. un
          // candidato asignado en un escenario para probar si es la
          // persona buscada), NUNCA hay que descartar la tipificacion
          // real de este individuo aunque no coincida y no haya modelo
          // mutacional: esa inconsistencia ES la exclusion mendeliana que
          // hay que detectar, no un dato ambiguo para ignorar. El
          // mecanismo de "descartar por mutacion sin modelo" solo tiene
          // sentido cuando AMBOS parientes son individuos confirmados
          // (con perfil propio, no una hipotesis).
          lazy val mutationsWithoutMutationModel =
            allelesMutated(alleles, allelesM, allelesP) &&
            mutationModelType.isEmpty &&
            !motherIsUnknown && !fatherIsUnknown

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

          cptMatrix.foreach {
            row =>
              cpt.header.zipWithIndex.foreach {
                case (column, index) =>
                  if (column!= "Probability" && row.last == 0) {
                    cpt0 += (column -> (cpt0.getOrElse(column, Set.empty) + row(index)))
//                    logger.info(s"--- Adding to CPT1: ${column} ---")
                  } else if (column != "Probability" && row.last != 0) {
                    cpt1 += (column -> (cpt1.getOrElse(column, Set.empty) + row(index)))
//                    logger.info(s"--- Adding to CPT2: ${column} ---")
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

//    logger.info(s"--- cptOut = ${cptOut.keys.mkString(" ")} ---")

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
