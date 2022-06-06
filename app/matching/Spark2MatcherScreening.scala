package matching

import java.text.ParseException

import akka.actor.{ActorSystem, actorRef2Scala}
import com.mongodb.client.model.Aggregates._
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections._
import com.mongodb.spark.MongoSpark
import com.mongodb.spark.config.ReadConfig
import configdata.{CategoryService, MatchingRule, MtConfiguration}
import connections.InterconnectionService
import inbox.{CollapsingInfo, NotificationService}
import javax.inject.{Inject, Named, Singleton}
import kits.LocusService
import matching.MatchingAlgorithm.{mergeMismatches, performMatch, profileMtMatch}
import org.apache.spark.rdd.RDD
import org.bson.Document
import pedigree.PedigreeService
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import probability.CalculationTypeService
import profile.{Profile, ProfileRepository}
import trace._
import types.{AlphanumericId, SampleCode}

import scala.collection.Seq
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS};

@Singleton
class Spark2MatcherScreening @Inject()(akkaSystem: ActorSystem,
                                       profileRepo: ProfileRepository,
                                       notificationService: NotificationService,
                                       categoryService: CategoryService,
                                       matchStatusService: MatchingProcessStatus,
                                       locusService: LocusService,
                                       traceService: TraceService,
                                       @Named("mongoUri") mongoUri: String,
                                       @Named("mtConfig") mtConfiguration: MtConfiguration,
                                       interconnectionService:InterconnectionService = null,
                                       @Named("defaultAssignee") nomRespSuperior : String,
                                       @Named("labCode") currentInstanceLabCode: String,
                                       pedigreeService:PedigreeService = null,
                                       matches:String = "",
                                       calculatorService:MatchingCalculatorService/*,
                                       /*@Named("limsArchivesPath")*/ exportProfilesPath: String = "",
                                       /*@Named("generateLimsFiles")*/ exportaALims: Boolean = false*/)
                extends Spark2Matcher(akkaSystem
                                      ,profileRepo
                                      ,notificationService
                                      ,categoryService
                                      ,matchStatusService
                                      ,locusService
                                      ,traceService
                                      ,mongoUri
                                      ,mtConfiguration
                                      ,interconnectionService
                                      ,nomRespSuperior
                                      ,currentInstanceLabCode
                                      ,pedigreeService
                                      ,"screeningMatches"
                                      ,calculatorService
                                      /*,""
                                      ,false*/){


  private val logger = Logger(this.getClass)

  def getProfilesRDDForScreening(globalCodes: List[String], categories: Seq[AlphanumericId]) = {
    val profilesReadConf = ReadConfig(Map("uri" -> s"$mongoUri.profiles"), None)

    import collection.JavaConverters._

    val profilesFilter = `match`(Filters.and(
      Filters.eq("deleted", false),
      Filters.nin("globalCode", globalCodes.toIterable.asJava),
      Filters.in("categoryId", categories.map(_.text).toIterable.asJava)
    ))

    val profilesProjection = project(include("_id", "globalCode", "assignee", "categoryId", "deleted",
      "genotypification", "labeledGenotypification", "contributors", "matchingRules", "internalSampleCode",
      "matcheable", "isReference", "analyses"))

    val docToProfile = (doc: Document) => {
      val json = Json.parse(doc.toJson( /*new JsonWriterSettings(JsonMode.SHELL)*/ ))
      val result = Json.fromJson[Profile](json)
      result.fold[Profile](err => {
        throw new ParseException(s"Can't parse: ${doc.toJson()}, error is: $err", 1)
      }, identity)
    }

    val profilesRDD: RDD[Profile] = MongoSpark
      .builder()
      .sparkContext(Spark2.context)
      .connector(Spark2.connector)
      .readConfig(profilesReadConf)
      .pipeline(Seq(profilesFilter, profilesProjection))
      .build()
      .toRDD()
      .map(docToProfile)

    profilesRDD
  }

  private val findMatchesFunc = (searched: Profile,queryProfiles:List[String],numberOfMismatches: Option[Int])=> {

    logger.debug(s"Start matching process for profile ${searched.globalCode}")

    // map of (global code, analysis type) -> mongo document id
    val existingMatchesRDD = this.getExistingMatchesRDD(searched.globalCode.text)
    val nOM = numberOfMismatches.toSeq
    val existingMatches = existingMatchesRDD.collect().toMap

    val mapOfRules = this.getMatchingRules(searched)

    val profilesRDD: RDD[Profile] = this.getProfilesRDDForScreening(queryProfiles, mapOfRules.map(_.categoryRelated))

    val n = profilesRDD.filter(profile => mapOfRules.exists(rule => profile.genotypification.keySet.contains(rule.`type`) && rule.categoryRelated == profile.categoryId && rule.considerForN)).count

    val config = mtConfiguration

    val newMatchesRDD = profilesRDD
        .flatMap { profile =>

        mapOfRules.filter(_.mitochondrial).filter(_.categoryRelated == profile.categoryId).map(_.`type`).flatMap { at =>

          val rules = mapOfRules.filter(mr => mr.`type` == at && mr.categoryRelated == profile.categoryId)
          val enfsi = rules.find(_.matchingAlgorithm == Algorithm.ENFSI)

          val matchingAlgorithm = mergeMismatches(enfsi,nOM)
          val result = matchingAlgorithm.flatMap { profileMtMatch(config, searched, profile, _, None, n) }
          result
        }

      }
      .cache
      
    val matchesToInsertRDD = newMatchesRDD.filter { mr =>
      !existingMatches.contains((mr.rightProfile.globalCode.text, mr.`type`))
    }

    val matchesToReplaceRDD = newMatchesRDD
      .filter { mr =>
        existingMatches.contains((mr.rightProfile.globalCode.text, mr.`type`))
      }
      .map { mr =>
        val id = existingMatches((mr.rightProfile.globalCode.text, mr.`type`))
        id -> mr
      }

    val matchesToReplace = matchesToReplaceRDD
      .map { case (docId, _) => docId }
      .collect()
      .toSet

    // .cache is necessary otherwise deleteRDD content changes
    val matchesToDeleteRDD = existingMatchesRDD
      .filter {
        case ((globalCode, analysisType), docId) =>
          !matchesToReplace.contains(docId)
      }.cache

    val msg = s"""
Profile ${searched.globalCode} has matched in screening against ${newMatchesRDD.count()} profiles of ${profilesRDD.count} candidates
  to insert: ${matchesToInsertRDD.count}
  to replace: ${matchesToReplaceRDD.count}
  to delete: ${matchesToDeleteRDD.count}
            """
    logger.info(msg)

    saveMatchesToDelete(matchesToDeleteRDD)

    saveMatchesToReplace(matchesToReplaceRDD,Map.empty,searched)

    saveMatchesToInsert(matchesToInsertRDD,Map.empty,searched)

    (newMatchesRDD.map(x => MatchResultScreening(x.rightProfile.globalCode.text,x._id.id))
      .collect()
      .toSet,
    matchesToInsertRDD.map(x => MatchResultScreening(x.rightProfile.globalCode.text,x._id.id))
      .collect()
      .toSet,existingMatches)

  }

  def findMatchesBlocking(profile:Profile, queryProfiles:List[String],numberOfMismatches: Option[Int]):(Set[MatchResultScreening],Set[MatchResultScreening],Map[(String, Integer), String]) = {
      findMatchesFunc(profile,queryProfiles,numberOfMismatches)
  }
  override def getMatchingRules(profile: Profile): Seq[MatchingRule] = {
    profile.matchingRules match {
      case Some(matchingRules) if matchingRules.nonEmpty => matchingRules
      case _ => categoryService.listCategories(profile.categoryId).matchingRules
    }
  }


}