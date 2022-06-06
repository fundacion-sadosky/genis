package matching

import java.io.File
import java.text.ParseException
import java.util.Date
import javax.inject.{Inject, Named, Singleton}

import matching.LRCalculation
import kits.Locus
import akka.actor.{Actor, ActorSystem, Props, actorRef2Scala}
import com.github.tototoshi.csv.{CSVWriter, DefaultCSVFormat}
import com.mongodb.BasicDBObject
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Aggregates._
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections._
import com.mongodb.spark.MongoSpark
import com.mongodb.spark.config.{ReadConfig, WriteConfig}
import com.mongodb.util.JSON
import configdata.{CategoryService, MatchingRule, MtConfiguration}
import inbox.{MatchingInfo, NotificationService}
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
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, SECONDS}
import connections.InterconnectionService
import pedigree.PedigreeService
import user.UserService;
@Singleton
class Spark2Matcher @Inject() (
    akkaSystem: ActorSystem,
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
    userService: UserService = null/*,
    /*@Named("limsArchivesPath")*/exportProfilesPath: String = "",
    /*@Named("generateLimsFiles")*/exportaALims: Boolean = false*/) {

  private val matchesTable:String = if(matches.isEmpty){"matches"}else{matches}
  private val matchingActor = akkaSystem.actorOf(MatchingActor.props(this))

  private val logger = Logger(this.getClass)

  def findMatchesInBackGround(globalCode: SampleCode): Unit = {
    matchingActor ! globalCode
  }

  protected def getProfilesRDD(globalCode: SampleCode, categories: Seq[AlphanumericId]) = {
    val profilesReadConf = ReadConfig(Map("uri" -> s"$mongoUri.profiles"), None)

    import collection.JavaConverters._

    val profilesFilter = `match`(Filters.and(
      Filters.eq("matcheable", true),
      Filters.eq("deleted", false),
      Filters.ne("globalCode", globalCode.text),
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

  protected def getExistingMatchesRDD(globalCode: String) = {
    val matchesReadConf = ReadConfig(Map("uri" -> s"$mongoUri.$matchesTable"), None)

    val existingMatchesFilter = `match`(
      Filters.or(
        Filters.eq("leftProfile.globalCode", globalCode),
        Filters.eq("rightProfile.globalCode", globalCode)))

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

  protected def saveMatchesToInsert(matchesToInsertRDD: RDD[MatchResult],locusRangeMap: NewMatchingResult.AlleleMatchRange,searched: Profile) = {
    val matchingLRs: Set[LRCalculation] = mapLRs(matchesToInsertRDD,locusRangeMap,searched)

    val matchesWriteConf = WriteConfig(Map("uri" -> s"$mongoUri.$matchesTable"), None)

    val matchResultToDoc = (matchResult: MatchResult) => {
      val leftProfile = Json.toJson(matchResult.leftProfile).toString()
      val rightProfile = Json.toJson(matchResult.rightProfile).toString()
      val result = Json.toJson(matchResult.result).toString()
      val bson = new Document()
      val id = new ObjectId(matchResult._id.id)
      bson.put("_id", id)
      bson.put("matchingDate", matchResult.matchingDate.date)
      bson.put("leftProfile", JSON.parse(leftProfile))
      bson.put("rightProfile", JSON.parse(rightProfile))
      bson.put("result", JSON.parse(result))
      bson.put("type", matchResult.`type`)
      bson.put("n", matchResult.n)

      if(matchResult.idCourtCase.isDefined){
        bson.put("idCourtCase", matchResult.idCourtCase.get)
      }
      val lr = matchingLRs.find(_.matchingId == matchResult._id.id).map(_.lrLeft).getOrElse(0.0)
//      val lrRight = matchingLRs.find(_.matchingId == matchResult._id.id).map(_.lrRight).getOrElse(0.0)
      bson.put("lr",lr)
//      bson.put("lrRight", lrRight)
      bson.put("mismatches",matchResult.mismatches)

      bson
    }

    MongoSpark.save(matchesToInsertRDD.map(matchResultToDoc), matchesWriteConf)
  }

  protected def saveMatchesToReplace(matchesToReplaceRDD: RDD[(String, MatchResult)],locusRangeMap: NewMatchingResult.AlleleMatchRange,searched: Profile) = {
    val matchingLRs: Set[LRCalculation] = mapLRs(matchesToReplaceRDD.map(_._2),locusRangeMap,searched)
    val matchesWriteConf = WriteConfig(Map("uri" -> s"$mongoUri.$matchesTable"), None)

    val matchResultToDocWithId = (tup: (String, MatchResult)) => {
      val matchResult = tup._2
      val leftProfile = Json.toJson(matchResult.leftProfile).toString()
      val rightProfile = Json.toJson(matchResult.rightProfile).toString()
      val result = Json.toJson(matchResult.result).toString()
      val bson = new Document()
      val id = new ObjectId(tup._1)
      bson.put("_id", id)
      bson.put("matchingDate", matchResult.matchingDate.date)
      bson.put("leftProfile", JSON.parse(leftProfile))
      bson.put("rightProfile", JSON.parse(rightProfile))
      bson.put("result", JSON.parse(result))
      bson.put("type", matchResult.`type`)
      bson.put("n", matchResult.n)
      if(matchResult.idCourtCase.isDefined){
        bson.put("idCourtCase", matchResult.idCourtCase.get)
      }
      val lr = matchingLRs.find(_.matchingId == matchResult._id.id).map(_.lrLeft).getOrElse(0.0)
//      val lrRight = matchingLRs.find(_.matchingId == matchResult._id.id).map(_.lrRight).getOrElse(0.0)

      bson.put("lr", lr)
//      bson.put("lrRight", lrRight)
      bson.put("mismatches",matchResult.mismatches)
      bson
    }

    matchesToReplaceRDD
      .map(matchResultToDocWithId)
      .foreachPartition(iter =>
        if (iter.nonEmpty) {
          iter.grouped(512).foreach(batch => {
            Spark2.connector.withCollectionDo(matchesWriteConf, { collection: MongoCollection[Document] =>
              batch.foreach { doc =>
                val filter = new Document("_id", doc.get("_id"))
                collection.replaceOne(filter, doc)
              }
            })
          })
        })

  }
  protected def saveMatchesToDelete(matchesToDeleteRDD: RDD[((String, Integer), String)]) = {
    val matchesWriteConf = WriteConfig(Map("uri" -> s"$mongoUri.$matchesTable"), None)

    matchesToDeleteRDD
      .foreachPartition(iter =>
        if (iter.nonEmpty) {
          iter.grouped(512).foreach(batch => {
            Spark2.connector.withCollectionDo(matchesWriteConf, { collection: MongoCollection[Document] =>
              batch.foreach { case ((_,_),id) =>
                val logicalDelete = Document.parse(s"""
{ 
  $$set : { 
    "leftProfile.status":  "${MatchStatus.deleted}", 
    "rightProfile.status": "${MatchStatus.deleted}" 
  }
}  
""")
                val filter = new Document("_id", new ObjectId(id))
                collection.updateOne(filter, logicalDelete)
              }
            })

          })
        })

  }

  protected def traceMatch(searched: Profile,
                         matchesToInsertRDD: RDD[MatchResult], matchesToReplaceRDD: RDD[(String, MatchResult)],
                         matchesToDeleteRDD: RDD[((String, Integer), String)]) {

    val matchResultToDocWithId = (tup: (String, MatchResult)) => {
      val matchResult = tup._2
      val leftProfile = Json.toJson(matchResult.leftProfile).toString()
      val rightProfile = Json.toJson(matchResult.rightProfile).toString()
      val result = Json.toJson(matchResult.result).toString()
      val bson = new Document()
      val id = new ObjectId(tup._1)
      bson.put("_id", id)
      bson.put("matchingDate", matchResult.matchingDate.date)
      bson.put("leftProfile", JSON.parse(leftProfile))
      bson.put("rightProfile", JSON.parse(rightProfile))
      bson.put("result", JSON.parse(result))
      bson.put("type", matchResult.`type`)
      bson.put("lr", matchResult.lr)
//      bson.put("lrRight", matchResult.lrRight)
      bson.put("mismatches",matchResult.mismatches)
      bson
    }

    matchesToDeleteRDD
      .collect
      .foreach { case ((rightProfile, analysisType), matchingId) =>
        traceService.add(Trace(searched.globalCode, searched.assignee, new Date(),
          MatchInfo(matchingId, SampleCode(rightProfile), analysisType, MatchTypeInfo.Delete)))
        traceService.add(Trace(SampleCode(rightProfile), searched.assignee, new Date(),
          MatchInfo(matchingId, searched.globalCode, analysisType, MatchTypeInfo.Delete)))
    }

    matchesToReplaceRDD
      .map { case (id, mr) => matchResultToDocWithId((id, mr)) }
      .collect
      .foreach { doc =>
        val matchingId = doc.getObjectId("_id").toString()
        val leftProfile = doc.get("leftProfile", classOf[BasicDBObject]).getString("globalCode")
        val rightProfile = doc.get("rightProfile", classOf[BasicDBObject]).getString("globalCode")
        val matchProfile = if (searched.globalCode.text == leftProfile) rightProfile else leftProfile
        val analysisType = doc.getInteger("type")

        traceService.add(Trace(searched.globalCode, searched.assignee, new Date(),
          MatchInfo(matchingId, SampleCode(matchProfile), analysisType, MatchTypeInfo.Update)))
        traceService.add(Trace(SampleCode(matchProfile), searched.assignee, new Date(),
          MatchInfo(matchingId, searched.globalCode, analysisType, MatchTypeInfo.Update)))
    }

    matchesToInsertRDD
      .map { mr => matchResultToDocWithId((mr._id.id, mr)) }
      .collect
      .foreach { doc =>
        val matchingId = doc.getObjectId("_id").toString()
        val rightProfile = doc.get("rightProfile", classOf[BasicDBObject]).getString("globalCode")
        val analysisType = doc.getInteger("type")

        traceService.add(Trace(searched.globalCode, searched.assignee, new Date(),
          MatchInfo(matchingId, SampleCode(rightProfile), analysisType, MatchTypeInfo.Insert)))
        traceService.add(Trace(SampleCode(rightProfile), searched.assignee, new Date(),
          MatchInfo(matchingId, searched.globalCode, analysisType, MatchTypeInfo.Insert)))

    }

  }
  protected def sendToInferiorInstance(matchesToInsertRDD: RDD[MatchResult],matchesToReplaceRDD:RDD[(String, MatchResult)],matchesToDeleteRDD:RDD[((String, Integer), String)]) {

    val matchResultToDocWithId = (tup: (String, MatchResult)) => {
      val matchResult = tup._2
      val leftProfile = Json.toJson(matchResult.leftProfile).toString()
      val rightProfile = Json.toJson(matchResult.rightProfile).toString()
      val result = Json.toJson(matchResult.result).toString()
      val bson = new Document()
      val id = new ObjectId(tup._1)
      bson.put("_id", id)
      bson.put("matchingDate", matchResult.matchingDate.date)
      bson.put("leftProfile", JSON.parse(leftProfile))
      bson.put("rightProfile", JSON.parse(rightProfile))
      bson.put("result", JSON.parse(result))
      bson.put("type", matchResult.`type`)
      bson.put("n", matchResult.n)
      bson.put("lr", matchResult.lr)
//      bson.put("lrRight", matchResult.lrRight)
      bson.put("mismatches",matchResult.mismatches)

      bson
    }

    val lambda = (matchResult: MatchResult) => {
      interconnectionService.sendMatchToInferiorInstance(matchResult)
    }

    val documents = matchesToInsertRDD
      .map { mr => matchResultToDocWithId((mr._id.id, mr)) }
      .collect
        .toList

    val documents2Replace = matchesToReplaceRDD
      .map { mr => matchResultToDocWithId((mr._1, mr._2)) }
      .collect
      .toList

    val documentAll = documents ++ documents2Replace

    val mrs = documentAll.flatMap(doc => {
      if(Json.fromJson[MatchResult](Json.parse(doc.toJson())).isSuccess)
        Some(Json.fromJson[MatchResult](Json.parse(doc.toJson())).get)
      else None
    })

     mrs.foreach(lambda)

    matchesToDeleteRDD
      .collect
      .foreach { case ((rightProfile, analysisType), matchingId) =>
        this.interconnectionService.deleteMatch(matchingId,rightProfile)
      }

  }
  protected def sendNotifications(matchesToInsertRDD: RDD[MatchResult], matchesToReplaceRDD: RDD[(String, MatchResult)]) {
    val matchResultToDocWithId = (tup: (String, MatchResult)) => {
      val matchResult = tup._2
      val leftProfile = Json.toJson(matchResult.leftProfile).toString()
      val rightProfile = Json.toJson(matchResult.rightProfile).toString()
      val result = Json.toJson(matchResult.result).toString()
      val bson = new Document()
      val id = new ObjectId(tup._1)
      bson.put("_id", id)
      bson.put("matchingDate", matchResult.matchingDate.date)
      bson.put("leftProfile", JSON.parse(leftProfile))
      bson.put("rightProfile", JSON.parse(rightProfile))
      bson.put("result", JSON.parse(result))
      bson.put("type", matchResult.`type`)
      bson.put("lr", matchResult.lr)
//      bson.put("lrRight", matchResult.lrRight)
      bson.put("mismatches",matchResult.mismatches)

      bson
    }
    val labActual = "-"+currentInstanceLabCode+"-"
    matchesToReplaceRDD
      .map { matchResultToDocWithId }
      .collect
      .foreach { doc =>
        val matchingId = doc.getObjectId("_id").toString()
        val leftAssignee = doc.get("leftProfile", classOf[BasicDBObject]).getString("assignee")
        val leftProfile = doc.get("leftProfile", classOf[BasicDBObject]).getString("globalCode")
        val rightAssignee = doc.get("rightProfile", classOf[BasicDBObject]).getString("assignee")
        val rightProfile = doc.get("rightProfile", classOf[BasicDBObject]).getString("globalCode")

        val leftNoti = MatchingInfo(SampleCode(leftProfile), SampleCode(rightProfile), matchingId)

        if(!leftProfile.contains(labActual) && !rightProfile.contains(labActual) ){
          this.interconnectionService.notify(leftNoti,Permission.INTERCON_NOTIF)
        }else {
          notificationService.push(leftAssignee, leftNoti)
          userService.sendNotifToAllSuperUsers(leftNoti, Seq(leftAssignee))
          if (leftAssignee != rightAssignee) {
            val rightNoti = MatchingInfo(SampleCode(rightProfile), SampleCode(leftProfile), matchingId)
            notificationService.push(rightAssignee, rightNoti)
            userService.sendNotifToAllSuperUsers(rightNoti, Seq(rightAssignee))
          }
        }
      }

    matchesToInsertRDD
      .map { mr => matchResultToDocWithId((mr._id.id, mr)) }
      .collect
      .foreach { doc =>
        val matchingId = doc.getObjectId("_id").toString
        val leftAssignee = doc.get("leftProfile", classOf[BasicDBObject]).getString("assignee")
        val leftProfile = doc.get("leftProfile", classOf[BasicDBObject]).getString("globalCode")
        val rightAssignee = doc.get("rightProfile", classOf[BasicDBObject]).getString("assignee")
        val rightProfile = doc.get("rightProfile", classOf[BasicDBObject]).getString("globalCode")

        val leftNoti = MatchingInfo(SampleCode(leftProfile), SampleCode(rightProfile), matchingId)

        if(!leftProfile.contains(labActual) && !rightProfile.contains(labActual) ){
          notificationService.push(nomRespSuperior,leftNoti)
          userService.sendNotifToAllSuperUsers(leftNoti, Seq(leftAssignee))
        }else {
          notificationService.push(leftAssignee, leftNoti)
          userService.sendNotifToAllSuperUsers(leftNoti, Seq(leftAssignee))
          if (leftAssignee != rightAssignee) {
            val rightNoti = MatchingInfo(SampleCode(rightProfile), SampleCode(leftProfile), matchingId)
            notificationService.push(rightAssignee, rightNoti)
            userService.sendNotifToAllSuperUsers(rightNoti, Seq(rightAssignee))

          }
        }
      }
  }
  private def mapLRs(matchesRDD: RDD[MatchResult],locusRangeMap: NewMatchingResult.AlleleMatchRange,searched: Profile): Set[LRCalculation] = {
    val lrs  = matchesRDD.map(mr => (mr.rightProfile.globalCode.text,mr._id.id,mr.`type`))
      .collect()
      .toSet
    lrs.map(matchR => {
      if(matchR._3 == 1){
        val res =Await.result(calculatorService.getDefaultLRForMachingId(searched,matchR._1,matchR._2,Some(locusRangeMap)),Duration.Inf)
        res
      }else{
        LRCalculation(matchR._2,0.0,0.0)
      }
      })
  }
  private val findMatchesFunc = (searched: Profile,locusRangeMap:NewMatchingResult.AlleleMatchRange) => {

    logger.debug(s"Start matching process for profile ${searched.globalCode}")

    matchStatusService.pushJobStatus(MatchJobStarted)

    // map of (global code, analysis type) -> mongo document id
    val existingMatchesRDD = getExistingMatchesRDD(searched.globalCode.text)

    val existingMatches = existingMatchesRDD.collect().toMap

    val mapOfRules = getMatchingRules(searched)

    val profilesRDD: RDD[Profile] = getProfilesRDD(searched.globalCode, mapOfRules.map(_.categoryRelated).distinct)

    val n = profilesRDD.filter(profile => mapOfRules.exists(rule => profile.genotypification.keySet.contains(rule.`type`) && rule.categoryRelated == profile.categoryId && rule.considerForN)).count

    val config = mtConfiguration

    val newMatchesRDD = profilesRDD
        .flatMap { profile =>

        mapOfRules.filter(_.categoryRelated == profile.categoryId).map(_.`type`).flatMap { at =>

          val rules = mapOfRules.filter(mr => mr.`type` == at && mr.categoryRelated == profile.categoryId)
          val enfsi = rules.find(_.matchingAlgorithm == Algorithm.ENFSI)
          val mixmix = rules.find(_.matchingAlgorithm == Algorithm.GENIS_MM)
          val pIsMix = searched.contributors.getOrElse(1) == 2
          val qIsMix = profile.contributors.getOrElse(1) == 2

          val matchingAlgorithm = if (pIsMix && qIsMix && mixmix.isDefined) mixmix.orElse(enfsi) else enfsi
          matchingAlgorithm.flatMap { performMatch(config, searched, profile, _, None, n,locusRangeMap) }

        }

      }
      .cache

    // Persist and notify matches
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

    // Set of mongo doc id 
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
Profile ${searched.globalCode} has matched against ${newMatchesRDD.count()} profiles of ${profilesRDD.count} candidates
  to insert: ${matchesToInsertRDD.count}
  to replace: ${matchesToReplaceRDD.count}
  to delete: ${matchesToDeleteRDD.count}
            """
    logger.info(msg)

    saveMatchesToDelete(matchesToDeleteRDD)

    saveMatchesToReplace(matchesToReplaceRDD,locusRangeMap,searched)

    saveMatchesToInsert(matchesToInsertRDD,locusRangeMap,searched)

/*
    if (exportaALims){
      createMatchLimsArchive(searched, matchesToInsertRDD, matchesToReplaceRDD)
    }
*/

    sendNotifications(matchesToInsertRDD, matchesToReplaceRDD)

    sendToInferiorInstance(matchesToInsertRDD,matchesToReplaceRDD,matchesToDeleteRDD)

    traceMatch(searched, matchesToInsertRDD, matchesToReplaceRDD, matchesToDeleteRDD)

    matchStatusService.pushJobStatus(MatchJobEndend)
  }

/*
  implicit object MatchArchiveFormat extends DefaultCSVFormat {
    override val delimiter = '\t'
  }

  def createMatchLimsArchive(searched: Profile, matchesToInsertRDD: RDD[MatchResult], matchesToReplaceRDD: RDD[(String, MatchResult)]) = {
    val folder = s"$exportProfilesPath${File.separator}"
    val folderFile = new File(folder)

    folderFile.mkdirs
    if(matchesToInsertRDD.count() > 0 || matchesToReplaceRDD.count() > 0)
      generateMatchesFile(folder, searched, matchesToInsertRDD, matchesToReplaceRDD)

  }
  def generateMatchesFile(folder: String, searched: Profile, matchesToInsertRDD: RDD[MatchResult], matchesToReplaceRDD: RDD[(String, MatchResult)]): File = {
    val file = new File(s"${folder}MatchesPerfil${searched.globalCode.text}.txt")

    val writer = CSVWriter.open(file)
    writer.writeAll(List(List("MatchId", "Sample GENis Code", "Sample Name", "MatchedSample GENis Code", "MatchedSample Name", "status", "Datetime" )))
    val format = new java.text.SimpleDateFormat("dd/MM/yyyy hh:mm:ss a")

    matchesToInsertRDD
      .map{ mr => (mr._id.id, mr.leftProfile.globalCode, mr.rightProfile.globalCode, format.format(mr.matchingDate.date))}
      .collect()
      .map { mr => {
        val leftProfileFuture = profileRepo.findByCode(mr._2)
        val leftProfile = Await.result(leftProfileFuture, Duration(100, SECONDS))
        val rightProfileFuture = profileRepo.findByCode(mr._3)
        val rigthProfile = Await.result(rightProfileFuture, Duration(100, SECONDS))

        writer.writeAll(List(List(mr._1, mr._2.text, leftProfile.get.internalSampleCode, mr._3.text, rigthProfile.get.internalSampleCode, "Pending", mr._4)))
      }}

    matchesToReplaceRDD
        .map{ case (id, mr) => (mr._id.id, mr.leftProfile.globalCode, mr.rightProfile.globalCode, format.format(mr.matchingDate.date)) }
      .collect()
        .map { mr => {
          val leftProfileFuture = profileRepo.findByCode(mr._2)
          val leftProfile = Await.result(leftProfileFuture, Duration(100, SECONDS))
          val rightProfileFuture = profileRepo.findByCode(mr._3)
          val rigthProfile = Await.result(rightProfileFuture, Duration(100, SECONDS))

          writer.writeAll(List(List(mr._1, mr._2.text, leftProfile.get.internalSampleCode, mr._3.text, rigthProfile.get.internalSampleCode, "Pending", mr._4)))
      }}

    writer.close()
    file
  }
*/

  def findMatchesBlocking(globalCode: SampleCode):Unit = {
    val listLocus = Await.result(locusService.list(), Duration(100, SECONDS))
    val locusRangeMap:NewMatchingResult.AlleleMatchRange = listLocus.map(locus=>locus.id -> AleleRange(locus.minAlleleValue.getOrElse(0),locus.maxAlleleValue.getOrElse(99))).toMap
    val f = profileRepo.findByCode(globalCode)
    val result = Await.result(f, Duration(100, SECONDS))
    result.foreach { p =>
      val category = categoryService.getCategory(p.categoryId).get
      traceService.add(Trace(globalCode, p.assignee, new Date(), MatchProcessInfo(category.matchingRules)))
      findMatchesFunc(p,locusRangeMap)
      if (!category.pedigreeAssociation) {
        Await.result(profileRepo.setMatcheableAndProcessed(globalCode), Duration(100, SECONDS))
      }
    }
  }
  def findMatchesBlockingMT(result: Option[Profile]):Unit = {
    val listLocus = Await.result(locusService.list(), Duration(100, SECONDS))
    val locusRangeMap:NewMatchingResult.AlleleMatchRange = listLocus.map(locus=>locus.id -> AleleRange(locus.minAlleleValue.getOrElse(0),locus.maxAlleleValue.getOrElse(99))).toMap
    result.foreach { p =>
      val category = categoryService.getCategory(p.categoryId).get
      findMatchesFunc(p,locusRangeMap)
    }
  }

  protected def validProfilesAssociated(labels: Option[Profile.LabeledGenotypification]) = {
    labels.getOrElse(Map()).keySet.filter(code => code.matches(SampleCode.validationRe.toString())).toSeq
  }

  protected def getMatchingRules(profile: Profile): Seq[MatchingRule] = {
    val forense = 1
    profile.matchingRules match {
      case Some(matchingRules) if matchingRules.nonEmpty => matchingRules
      case _ =>{
        val categories = categoryService.listCategories
        val category = categories(profile.categoryId)
        if(category.tipo.contains(forense) && !category.pedigreeAssociation){
            category.matchingRules.filter(x=> categories(x.categoryRelated).tipo.contains(forense))
        }else{ Nil }
      }
    }
  }
}