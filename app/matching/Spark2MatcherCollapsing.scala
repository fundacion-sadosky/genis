package matching

import java.text.ParseException
import java.util.Date
import javax.inject.{Inject, Named, Singleton}

import kits.Locus
import akka.actor.{Actor, ActorSystem, Props, actorRef2Scala}
import com.mongodb.BasicDBObject
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Aggregates._
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections._
import com.mongodb.spark.MongoSpark
import com.mongodb.spark.config.{ReadConfig, WriteConfig}
import com.mongodb.util.JSON
import configdata.{CategoryService, MatchingRule, MtConfiguration}
import inbox.{CollapsingInfo, MatchingInfo, NotificationService}
import kits.LocusService
import matching.MatchingAlgorithm.performMatch
import org.apache.spark.rdd.RDD
import org.bson.Document
import org.bson.types.ObjectId
import play.api.Logger
import play.api.libs.json.Json
import profile.{Profile, ProfileRepository}
import trace._
import types.{AlphanumericId, Permission, SampleCode}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.collection.Seq
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
import connections.InterconnectionService
import pedigree.PedigreeService
import probability.CalculationTypeService
import user.UserService;

@Singleton
class Spark2MatcherCollapsing @Inject()(akkaSystem: ActorSystem,
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
                                        calculatorService:MatchingCalculatorService,
                                        userService: UserService = null /*,
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
                                      ,"collapsingMatches"
                                      ,calculatorService,
                                      userService
                                      /*, ""
                                      ,false*/){

  private val collapsingActor = akkaSystem.actorOf(CollapsingActor.props(this))

  private val logger = Logger(this.getClass)

  def collapseInBackGround(idCourtCase: Long,user:String): Unit = {
    collapsingActor ! (idCourtCase,user)
  }
  def collapseBlocking(idCourtCase: Long,user:String):Unit = {
    pedigreeService.getProfilesForCollapsing(idCourtCase).map(profiles => {
      if(profiles.size >= 2){
        val numberMatches = profiles.map( profile => {
          findMatchesBlocking(SampleCode(profile._1),profiles.filter(!_._2).filter(x => x._1 != profile._1).map(_._1),idCourtCase)
        }).sum
        notificationService.push(user, CollapsingInfo(idCourtCase,numberMatches>0))
        userService.sendNotifToAllSuperUsers(CollapsingInfo(idCourtCase,numberMatches>0), Seq(user))
      }else{
        notificationService.push(user, CollapsingInfo(idCourtCase,false))
        userService.sendNotifToAllSuperUsers(CollapsingInfo(idCourtCase,false), Seq(user))
      }
    })
    ()
  }
  def getProfilesRDDForCollapsing(globalCodes: List[String], categories: Seq[AlphanumericId]) = {
    val profilesReadConf = ReadConfig(Map("uri" -> s"$mongoUri.profiles"), None)

    import collection.JavaConverters._

    val profilesFilter = `match`(Filters.and(
      Filters.eq("deleted", false),
      Filters.in("globalCode", globalCodes.toIterable.asJava),
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

  private val findMatchesFunc = (searched: Profile,locusRangeMap:NewMatchingResult.AlleleMatchRange,profiles:List[String],idCourtCase: Long)=> {

    logger.debug(s"Start matching process for profile ${searched.globalCode}")

    // map of (global code, analysis type) -> mongo document id
    val existingMatchesRDD = this.getExistingMatchesRDD(searched.globalCode.text,idCourtCase)

    val existingMatches = existingMatchesRDD.collect().toMap

    val mapOfRules = this.getMatchingRules(searched)

    val profilesRDD: RDD[Profile] = this.getProfilesRDDForCollapsing(profiles, mapOfRules.map(_.categoryRelated))

    val n = profilesRDD.filter(profile => mapOfRules.exists(rule => profile.genotypification.keySet.contains(rule.`type`) && rule.categoryRelated == profile.categoryId && rule.considerForN)).count

    val config = mtConfiguration

    val newMatchesRDD = profilesRDD
        .flatMap { profile =>

        mapOfRules.filter(!_.mitochondrial).filter(_.categoryRelated == profile.categoryId).map(_.`type`).flatMap { at =>

          val rules = mapOfRules.filter(mr => mr.`type` == at && mr.categoryRelated == profile.categoryId)
          val enfsi = rules.find(_.matchingAlgorithm == Algorithm.ENFSI)
          val mixmix = rules.find(_.matchingAlgorithm == Algorithm.GENIS_MM)
          val pIsMix = searched.contributors.getOrElse(1) == 2
          val qIsMix = profile.contributors.getOrElse(1) == 2

          val matchingAlgorithm = if (pIsMix && qIsMix && mixmix.isDefined) mixmix.orElse(enfsi) else enfsi
          matchingAlgorithm.flatMap { performMatch(config, searched, profile, _, None, n,locusRangeMap,Some(idCourtCase)) }

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
Profile ${searched.globalCode} has matched in collapsing against ${newMatchesRDD.count()} profiles of ${profilesRDD.count} candidates
  to insert: ${matchesToInsertRDD.count}
  to replace: ${matchesToReplaceRDD.count}
  to delete: ${matchesToDeleteRDD.count}
            """
    logger.info(msg)

    saveMatchesToDelete(matchesToDeleteRDD)

    saveMatchesToReplace(matchesToReplaceRDD,locusRangeMap,searched)

    saveMatchesToInsert(matchesToInsertRDD,locusRangeMap,searched)

    matchesToInsertRDD.map { case (_) => 1 }
      .collect()
      .toSet.size
  }

  def findMatchesBlocking(globalCode: SampleCode,profiles:List[String],idCourtCase: Long):Int = {
    val listLocus = Await.result(locusService.list(), Duration(100, SECONDS))
    val locusRangeMap:NewMatchingResult.AlleleMatchRange = listLocus.map(locus=>locus.id -> AleleRange(locus.minAlleleValue.getOrElse(0),locus.maxAlleleValue.getOrElse(99))).toMap
    val f = profileRepo.findByCode(globalCode)
    val result = Await.result(f, Duration(100, SECONDS))
    result.map { p =>
      findMatchesFunc(p,locusRangeMap,profiles,idCourtCase)
    }.getOrElse(0)
  }
  override def getMatchingRules(profile: Profile): Seq[MatchingRule] = {
    profile.matchingRules match {
      case Some(matchingRules) if matchingRules.nonEmpty => matchingRules
      case _ =>
        val category = categoryService.listCategories(profile.categoryId)
          category.matchingRules
    }
  }

  def getExistingMatchesRDD(globalCode: String,idCourtCase: Long) = {
    val matchesReadConf = ReadConfig(Map("uri" -> s"$mongoUri.collapsingMatches"), None)

    val existingMatchesFilter = `match`(Filters.and(
      Filters.eq("leftProfile.globalCode", globalCode),
      Filters.eq("idCourtCase", idCourtCase)
    ))

    val existingMatchesProjection = project(include("_id", "leftProfile.globalCode", "rightProfile.globalCode", "type"))

    val existingMatchesRDD = MongoSpark
      .builder()
      .sparkContext(Spark2.context)
      .connector(Spark2.connector)
      .readConfig(matchesReadConf)
      .pipeline(Seq(existingMatchesFilter, existingMatchesProjection))
      .build()
      .toRDD()
      .map { doc =>
        val `type` = doc.getInteger("type")
        val left = doc.get("leftProfile", classOf[Document]).getString("globalCode")
        val right = doc.get("rightProfile", classOf[Document]).getString("globalCode")
        val gc = if (left == globalCode) {
          right
        } else {
          left
        }
        (gc, `type`) -> doc.getObjectId("_id").toString
      }

    existingMatchesRDD

  }
}