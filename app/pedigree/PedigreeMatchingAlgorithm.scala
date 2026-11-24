package pedigree

import java.util.Date
import configdata.{MatchingRule, MtConfiguration}
import kits.AnalysisType
import matching._
import org.bson.types.ObjectId
import play.api.Logger
import profile.{MtRCRS, Profile}
import types.{AlphanumericId, MongoDate, MongoId}

import scala.collection.Seq
import profile.GenotypificationByType.GenotypificationByType

object PedigreeMatchingAlgorithm {

  val logger: Logger = Logger(this.getClass())

  def matchToPedigreeMatch(matchResult: Option[MatchResult], p: Profile, q: Profile,
                           idPedigree: Long, assignee: String, alias: String,locusRangeMap:NewMatchingResult.AlleleMatchRange): Option[PedigreeMatchResult] = {
    matchResult.map { mr =>
      val profile = MatchingProfile(p.globalCode, p.assignee, MatchStatus.pending, None, p.categoryId)
      val pedigree = PedigreeMatchingProfile(idPedigree, NodeAlias(alias), q.globalCode, assignee, MatchStatus.pending, "MPI",mr.idCourtCase.get)
      PedigreeDirectLinkMatch(
        mr._id,
        mr.matchingDate,
        mr.`type`,
        profile,
        pedigree,
        mr.result)
    }
  }

  def performMatch(
    config: MtConfiguration,
    p: Profile,
    q: Profile,
    matchingRule: MatchingRule,
    mtRcrs: MtRCRS,
    id: Option[MongoId] = None,
    idPedigree: Long,
    assignee: String,
    alias: String,
    locusRangeMap:NewMatchingResult.AlleleMatchRange
  ): Option[PedigreeMatchResult] = {
    val matchResult = MatchingAlgorithm
      .performMatch(config, p, q, matchingRule, mtRcrs, id, 0,locusRangeMap)
    matchToPedigreeMatch(
      matchResult, p, q, idPedigree, assignee, alias, locusRangeMap
    )
  }
  def getMarkers(p:Profile):List[String] = {
    p.genotypification.get(1).map(result => {
        result.keySet.map(_.toString)
      }).getOrElse(Nil).toList.map(marker => "_"+marker+"_")
  }
  def filterGenotipification(pG:PedigreeGenotypification,markers:List[String]):PedigreeGenotypification = {
    def matchesMarkers(plainCPT: PlainCPT2): Boolean =
      plainCPT.header.find(marker => markers.find(marker2 => marker.contains(marker2)).isDefined).isDefined
    pG.copy(
      genotypification = pG.genotypification.filter(matchesMarkers),
      strictGenotypification = pG.strictGenotypification.filter(matchesMarkers)
    )
  }
  def extractMarker(s:String):String={
    if(s.contains("_")){
      s.substring(s.indexOf("_")+1,s.lastIndexOf("_"))
    }else{
      ""
    }
  }

  def filterGeno(genotypification: profile.GenotypificationByType.GenotypificationByType,markers:Set[String]): profile.GenotypificationByType.GenotypificationByType
    = {
   genotypification.map(x => {
      if(x._1 == 1){
        (x._1,x._2.filter(p => markers.contains(p._1.toString)))
      }else{
        x
      }
    })
  }
  /** Memo de marcadores rehechos en una corrida de cotejo: la clave es
    * (pedigrí, marcador, alelos agregados ordenados). Spark corre en local[*],
    * así que un TrieMap capturado en la clausura se
    * comparte entre las tareas; en un Spark distribuido cada ejecutor tendría
    * el suyo y el memo sólo perdería eficacia, no corrección. */
  type RebuildCache = scala.collection.concurrent.TrieMap[(Long, String, List[Double]), BayesianNetwork.RebuiltMarker]
  def newRebuildCache(): RebuildCache = scala.collection.concurrent.TrieMap.empty

  /** Cota de lo que pueden aportar al LR los marcadores `markers` del candidato:
    * producto de 2 / P(g_m), con P(g_m) bajo Hardy-Weinberg sobre la tabla
    * normalizada del universo del cotejo, que ya trae adentro los alelos del
    * candidato (getNormalizedFrequencyTable(tabla, n)). LR_m <= 1/P(g_m) porque
    * P(g_m | familia) <= 1; el 2 es margen por redondeo. Con tres o más alelos
    * (trialelo) no hay P(g) de Hardy-Weinberg que acote el LR: la cota es
    * infinita y ese marcador nunca poda. */
  def rebuildBound(profile: Profile, analysisType: AnalysisType, markers: Iterable[String],
                   normalizedTable: BayesianNetwork.FrequencyTable): Double = {
    val alleles = CandidateUniverse.candidateAlleles(profile, analysisType)
    markers.map { m =>
      val f = (a: Double) => BayesianNetwork.getFrequency(a, m, normalizedTable)
      alleles.getOrElse(m, Nil) match {
        case List(a) => 2.0 / (f(a) * f(a))
        case List(a, b) => 2.0 / (2 * f(a) * f(b))
        case _ => Double.PositiveInfinity
      }
    }.product
  }

  /** Reemplaza en la genotipificación las CPT de `marker` por las rehechas. */
  def withRebuiltMarker(pG: PedigreeGenotypification, marker: String, r: BayesianNetwork.RebuiltMarker): PedigreeGenotypification = {
    val tag = "_" + marker + "_"
    def keep(c: PlainCPT2): Boolean = !c.header(0).contains(tag)
    pG.copy(
      genotypification = pG.genotypification.filter(keep) ++ r.genotypification,
      strictGenotypification = pG.strictGenotypification.filter(keep) ++ r.strictGenotypification,
      genotypificationH2 = (pG.genotypificationH2, r.genotypificationH2) match {
        case (Some(h2), Some(nuevo)) => Some(h2.filterNot(_.marker == marker) ++ nuevo)
        case (Some(h2), None) => Some(h2.filterNot(_.marker == marker))
        case (None, nuevo) => nuevo
      },
      genotypificationWithMarkers = pG.genotypificationWithMarkers.filterNot(_.marker == marker) ++ r.genotypificationWithMarkers)
  }

  def findCompatibilityMatches(frequencyTable: BayesianNetwork.FrequencyTable, p: Profile, pG: PedigreeGenotypification,
                               idPedigree: Long, assignee: String, analysisType: AnalysisType, mutationModelType: Option[Long] = None,
                               mutationModelData: Option[List[(MutationModelParameter, List[MutationModelKi],MutationModel)]] = None,
                               caseType : String, idCourtCase: Long,
                               familyProfilesCrudos: Array[Profile] = Array.empty, genogram: Array[Individual] = Array.empty,
                               locusRangeMap: NewMatchingResult.AlleleMatchRange = Map.empty,
                               rebuildCache: Option[RebuildCache] = None): Seq[PedigreeMatchResult] = {
    // Candidato y familia entran normalizados acá (ACTBP2 canónico, sin marcadores
    // en 0), sea cual sea el llamador: RDD de candidatos, perfil nuevo o familia del matcher.
    val candidato = BayesianNetwork.normalizarParaParentesco(p, frequencyTable)
    val familyProfiles = familyProfilesCrudos.map(BayesianNetwork.normalizarParaParentesco(_, frequencyTable))
    val genoMarkers = BayesianNetwork.getMarkersFromCpts(pG.genotypification)
    val profile :Profile = candidato.copy(genotypification = filterGeno(candidato.genotypification,genoMarkers))
    val markers = getMarkers(profile)
    val filtered = filterGenotipification(pG,markers)

    // Universo con que se construyó la CPT. Persistido desde esta versión;
    // para genotipificaciones viejas se recomputa con la familia, que es una
    // función pura de los mismos perfiles. Sin familia (llamador viejo) queda el
    // panel crudo y el comportamiento de antes.
    val canRebuild = familyProfiles.nonEmpty && genogram.nonEmpty
    val universe: Map[String, List[Double]] =
      if (pG.universe.nonEmpty) {
        pG.universe
      } else if (canRebuild) {
        BayesianNetwork.extendNWithObservedAlleles(BayesianNetwork.getNFromTable(frequencyTable), familyProfiles, genogram, analysisType)
      } else {
        BayesianNetwork.getNFromTable(frequencyTable)
      }
    val stale = canRebuild && pG.familyFingerprint.exists(_ != CandidateUniverse.familyFingerprint(familyProfiles, genogram))
    val missing: Map[String, List[Double]] =
      if (canRebuild) CandidateUniverse.missingAlleles(profile, analysisType, genoMarkers, universe) else Map.empty
    val nCotejo = CandidateUniverse.extend(universe, missing)

    // Materializa PlainCPT frescos (el iterador es de un solo uso) y coteja.
    def lrOf(pg: PedigreeGenotypification, prof: Profile, nUsed: Map[String, List[Double]]): (Double, String, Map[String, MarkerLRDetail]) = {
      val geno = pg.genotypification.map(c => PlainCPT(c.header, c.matrix.iterator, c.matrix.length))
      val strict = pg.strictGenotypification.map(c => PlainCPT(c.header, c.matrix.iterator, c.matrix.length))
      def toMarkerPlainCPT(mc: MarkerPlainCPT2): (String, PlainCPT) = mc.marker -> PlainCPT(mc.cpt.header, mc.cpt.matrix.iterator, mc.cpt.matrix.length)
      BayesianNetwork.getLR(pg.unknowns, prof, frequencyTable, analysisType, geno, mutationModelType, mutationModelData, nUsed,
        strict, pg.genotypificationH2.map(_.map(toMarkerPlainCPT)), pg.genotypificationWithMarkers.map(toMarkerPlainCPT))
    }

    // Poda: antes de rehacer, cotejar los demás marcadores con lo guardado y
    // acotar lo que los rehechos podrían aportar. LR_m <= 1/P(g_m) porque
    // P(g_m | familia) <= 1. Si ni con la cota se llega al umbral, el par no
    // califica y no hay nada que rehacer. Si el candidato no tiene otro marcador
    // que getLR evalúe (la familia no los tiene, o el candidato no los tipificó,
    // o son de banda única), LR_otros = 1: getLR devolvería 0 con una lista
    // vacía, y eso podaría mal.
    val prunedOut: Boolean = missing.nonEmpty && {
      val otherMarkers: Set[String] = genoMarkers -- missing.keySet
      val pOthers = profile.copy(genotypification = filterGeno(profile.genotypification, otherMarkers))
      val lrOthers =
        if (BayesianNetwork.getQueryProfileAlleles(pOthers, analysisType, frequencyTable).isEmpty) {
          1.0
        } else {
          lrOf(filterGenotipification(filtered, getMarkers(pOthers)), pOthers, universe)._1
        }
      val bound = rebuildBound(profile, analysisType, missing.keys, BayesianNetwork.getNormalizedFrequencyTable(frequencyTable, nCotejo))
      lrOthers * bound < filtered.boundary
    }

    if (prunedOut) Seq.empty else {
      val pedigreeGenotypification = missing.foldLeft(filtered) { case (acc, (marker, alleles)) =>
        val key = (idPedigree, marker, alleles)
        def rebuild: BayesianNetwork.RebuiltMarker = BayesianNetwork.rebuildMarker(familyProfiles, genogram, frequencyTable, analysisType, marker,
          locusRangeMap, mutationModelType, mutationModelData, CandidateUniverse.extend(universe, Map(marker -> alleles)))
        val rebuilt = rebuildCache.map(_.getOrElseUpdate(key, rebuild)).getOrElse(rebuild)
        withRebuiltMarker(acc, marker, rebuilt)
      }

      val lr = lrOf(pedigreeGenotypification, profile, nCotejo)
      // Guarda: con el universo ampliado ningún marcador debería descartarse. Si
      // pasa, queda en el message de la coincidencia y en el log.
      if (canRebuild && lr._2.nonEmpty) {
        logger.warn(s"#340 marcador descartado pese al universo ampliado: pedigrí $idPedigree, candidato ${profile.globalCode.text}: ${lr._2.trim}")
      }
      if (lr._1 >= pedigreeGenotypification.boundary) {
        val oid = MongoId(new ObjectId().toString)
        val matchingProfile = MatchingProfile(profile.globalCode, profile.assignee, MatchStatus.pending, None, profile.categoryId)
        val matchingPedigree = PedigreeMatchingProfile(idPedigree, NodeAlias(pedigreeGenotypification.unknowns.head), assignee, MatchStatus.pending, caseType, idCourtCase)
        val singleBandMarkers = lr._3
          .collect { case (marker, detail) if detail.classification == MarkerLRDetail.SingleBand => marker }
          .toList
          .sorted

        logger.debug(s"Created compatibility match result with id ${oid.id}")
        Seq(PedigreeCompatibilityMatch(oid, MongoDate(new Date()), analysisType.id, matchingProfile, matchingPedigree, lr._1,
          message = lr._2,
          singleBandMarkers = singleBandMarkers, familySingleBand = pG.familySingleBand,
          extendedMarkers = missing, staleGenotypification = stale))
      } else Seq.empty
    }
  }

}
