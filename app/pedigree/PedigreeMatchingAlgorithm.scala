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

/**
  * Created by pdg on 5/11/17.
  */
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
    pG.copy(genotypification = pG.genotypification.
      filter(plainCPT => plainCPT.header
          .find(marker => markers.find(marker2 => marker.contains(marker2)).isDefined).isDefined
          ))
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
  def findCompatibilityMatches(frequencyTable: BayesianNetwork.FrequencyTable, p: Profile, pG: PedigreeGenotypification,
                               idPedigree: Long, assignee: String, analysisType: AnalysisType, mutationModelType: Option[Long] = None,
                               mutationModelData: Option[List[(MutationModelParameter, List[MutationModelKi],MutationModel)]] = None,
                               n: Map[String,List[Double]] = Map.empty, caseType : String, idCourtCase: Long): Seq[PedigreeMatchResult] = {
    val genoMarkers = BayesianNetwork.getMarkersFromCpts(pG.genotypification)
    val profile :Profile = p.copy(genotypification = filterGeno(p.genotypification,genoMarkers))
    val markers = getMarkers(profile)
    val pedigreeGenotypification = filterGenotipification(pG,markers)
    // val markersFromCpts = BayesianNetwork.getMarkersFromCpts(pedigreeGenotypification.genotypification)

    //TODO: cambiar pedigreeGenotypification.unknowns.head para que use todos los del array
    val genotypification = pedigreeGenotypification.genotypification.map(plainCPT2 => PlainCPT(plainCPT2.header, plainCPT2.matrix.iterator, plainCPT2.matrix.length))
    val lr = BayesianNetwork.getLR(pedigreeGenotypification.unknowns, profile, frequencyTable, analysisType, genotypification, mutationModelType, mutationModelData,n)
//    val lr = BayesianNetwork.getLR(pedigreeGenotypification.unknowns, profile, frequencyTable, analysisType, pedigreeGenotypification.genotypification, mutationModelType, mutationModelData,n)
      if (lr._1 >= pedigreeGenotypification.boundary) {
        val oid = MongoId(new ObjectId().toString)
        val matchingProfile = MatchingProfile(profile.globalCode, profile.assignee, MatchStatus.pending, None, profile.categoryId)
        val matchingPedigree = PedigreeMatchingProfile(idPedigree, NodeAlias(pedigreeGenotypification.unknowns.head), assignee, MatchStatus.pending, caseType, idCourtCase)

        logger.debug(s"Created compatibility match result with id ${oid.id}")
        Seq(PedigreeCompatibilityMatch(oid, MongoDate(new Date()), analysisType.id, matchingProfile, matchingPedigree, lr._1, message = lr._2))
      } else Seq.empty
  }



}
