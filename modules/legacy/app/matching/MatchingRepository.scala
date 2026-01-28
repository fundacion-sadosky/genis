package matching

import reactivemongo.bson.{BSONDocument, BSONDouble, BSONJavaScript, BSONObjectID}
import com.mongodb.util.JSON
import java.text.ParseException
import java.util.Date
import javax.inject.{Inject, Named}

import com.mongodb.BasicDBObject
import profiledata.ProfileData
import org.bson.types.ObjectId
import com.mongodb.client.model.Aggregates._
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections._
import com.mongodb.spark.MongoSpark
import com.mongodb.spark.config.ReadConfig
import matching.MatchingSortHelper._
import org.apache.spark.rdd.RDD
import org.bson.Document
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.{Format, JsArray, JsObject, JsValue, Json, Reads, Writes, __, _}
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import profile.Profile
import reactivemongo.api.commands.Command
import reactivemongo.api.{Cursor, FailoverStrategy, QueryOpts, ReadPreference}
import types.{AlphanumericId, Count, SampleCode}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import connections.SuperiorProfileInfo
import types.{AlphanumericId, MongoDate, MongoId, SampleCode}
import matching.MatchStatus.MatchStatus
import play.api.Logger
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps

import scala.math.BigDecimal.RoundingMode
abstract class MatchingRepository {
  def getMatches(search: MatchCardSearch): Future[Seq[JsValue]]
  def getTotalMatches(search: MatchCardSearch): Future[Int]
  def matchesByAssignee(userId: String): Future[Seq[MatchResult]]
  def getAllTotalMatches(search: MatchCardSearch): Future[Int]
  def getMatchesPaginated(search: MatchCardSearch): Future[Seq[MatchResult]]
  def matchesByGlobalCode(globalCode: SampleCode): Future[Seq[MatchResult]]
  def getByMatchingProfileId(matchingId: String,isCollapsing:Option[Boolean] = None,isScreening:Option[Boolean] = None): Future[Option[MatchResult]]
  def convertStatus(matchId: String, firingCode: SampleCode, status: String): Future[Seq[SampleCode]]
  def deleteMatches(matchId: String, globalCode: SampleCode): Future[Boolean]
  def deleteMatch(matchId: String): Future[Boolean]
  def getByFiringAndMatchingProfile(firingCode: SampleCode, matchingCode: SampleCode): Future[Option[MatchResult]]
  def matchesNotDiscarded(globalCode: SampleCode): Future[Seq[MatchResult]]

  def removeMatchesByProfile(globalCode: SampleCode): Future[Either[String,String]]
  def matchesWithFullHit(globalCode: SampleCode): Future[Seq[MatchResult]]
  def matchesWithPartialHit(globalCode: SampleCode): Future[Seq[MatchResult]]
  def getGlobalMatchStatus(leftProfileStatus: MatchStatus.Value, rightProfileStatus: MatchStatus.Value): MatchGlobalStatus.Value
  def getMatchesByGroup(search: MatchGroupSearch): Seq[MatchingResult]
  def getTotalMatchesByGroup(search: MatchGroupSearch): Int
  def insertMatchingResult(matchingResult:MatchResult):Future[Unit]
  def findSuperiorProfile(globalCode:SampleCode):Future[Option[Profile]]
  def findSuperiorProfileData(globalCode:SampleCode):Future[Option[ProfileData]]
  def discardCollapsingMatches(ids:List[String],courtCaseId:Long) : Future[Unit]
  def discardCollapsingByLeftProfile(id:String,courtCaseId:Long) : Future[Unit]
  def discardCollapsingByLeftAndRightProfile(id:String,courtCaseId:Long) : Future[Unit]
  def discardCollapsingByRightProfile(id:String,courtCaseId:Long) : Future[Unit]
  def discardScreeningMatches(ids:List[String]) : Future[Unit]
  def getScreeningByFiringProfile(firingCode: SampleCode): Future[Seq[MatchResultScreening]]
  def numberOfMatchesHit(globalCode: String): Future[Int]
  def numberOfMatches(globalCode: String): Future[Int]
  def numberOfMatchesPending(globalCode: String): Future[Int]
  def numberOfMatchesDescarte(globalCode: String): Future[Int]
  def numberOfMatchesConflic(globalCode: String): Future[Int]
  def getProfileLr( globalCode: SampleCode, isCollapsing : Boolean): Future[MatchCardMejorLr]
  def updateMatchesLR(matchingLRs: Set[(String, Double)]):Future[Unit]
  def numberOfMt(globalCode: String ): Future[Boolean]
  def convertToJson(matchingResult:MatchResult): JsValue
  def getByDateBetween(from : Option[Date], to : Option[Date]): Future[Seq[MatchResult]]
}

class MongoMatchingRepository @Inject() (@Named("mongoUri") val mongoUri: String) extends MatchingRepository {
  val logger = Logger(this.getClass())

  private def matches = Await.result(play.modules.reactivemongo.ReactiveMongoPlugin.database.map(_.collection[JSONCollection]("matches")), Duration(10, SECONDS))
  private def profiles = Await.result(play.modules.reactivemongo.ReactiveMongoPlugin.database.map(_.collection[JSONCollection]("profiles")), Duration(10, SECONDS))
  private def collapsingMatches = Await.result(play.modules.reactivemongo.ReactiveMongoPlugin.database.map(_.collection[JSONCollection]("collapsingMatches")), Duration(10, SECONDS))
  private def screeningMatches = Await.result(play.modules.reactivemongo.ReactiveMongoPlugin.database.map(_.collection[JSONCollection]("screeningMatches")), Duration(10, SECONDS))

  override def getMatchesPaginated(search: MatchCardSearch): Future[Seq[MatchResult]] = {
    val query = Json.obj()

    matches.find(query)
      .options(QueryOpts().skip(search.page*search.pageSize))
      .cursor[MatchResult]().collect[Seq](search.pageSize, Cursor.FailOnError[Seq[MatchResult]]())
  }
  private def isFinal(matchRes: MatchResult): Boolean = {
    (matchRes.leftProfile.status, matchRes.rightProfile.status) match {
      case (MatchStatus.hit, MatchStatus.hit)             => true
      case (MatchStatus.discarded, MatchStatus.discarded) => true
      case (_, _)                                         => false
    }
  }
  private def updateMatchLR(matchId:String,lr:Double): Future[Unit] = {
    if(lr>=0.0){
      val find = Json.obj("_id" -> Json.obj("$oid" -> matchId))
      val update = Json.obj("$set" -> Json.obj("lr" -> formatDouble(lr)))
      matches.update(Json.obj("_id" -> Json.obj("$oid" -> matchId)),update).map { _ => ()}
    }
    Future.successful(())
  }
  def formatDouble(d:Double):JsNumber={
    val bigD = BigDecimal.valueOf(d)
    if(bigD.scale < 6){
      JsNumber(bigD.setScale(6,RoundingMode.HALF_DOWN))
    }else{
      JsNumber(bigD)
    }
  }
  override def updateMatchesLR(matchingLRs: Set[(String, Double)]):Future[Unit] = {
    Future.sequence(matchingLRs.map(mLR => {
      this.updateMatchLR(mLR._1,mLR._2)
    })).map(_ => ())
  }
  override def deleteMatches(matchId: String, globalCode: SampleCode): Future[Boolean] = {
    val find = Json.obj("_id" -> Json.obj("$oid" -> matchId))

    matches.find(find)
      .one[MatchResult] flatMap { matchResOpt =>
      matchResOpt.fold[Future[Boolean]](Future.successful(true))(matchRes => {

        if (!isFinal(matchRes)) {
          val update =
            if (matchRes.leftProfile.globalCode == globalCode)
              Json.obj("$set" -> Json.obj("leftProfile.status" -> "deleted"))
            else
              Json.obj("$set" -> Json.obj("rightProfile.status" -> "deleted"))

          matches.update(Json.obj("_id" -> Json.obj("$oid" -> matchId)), update).map {_.ok}
        } else {
          Future.successful(true)
        }
      })
    }
  }
  override def deleteMatch(matchId: String): Future[Boolean] = {
    val find = Json.obj("_id" -> Json.obj("$oid" -> matchId))

    matches.find(find)
      .one[MatchResult] flatMap { matchResOpt =>
      matchResOpt.fold[Future[Boolean]](Future.successful(true))(matchRes => {

        if (!isFinal(matchRes)) {
          val update = Json.obj("$set" -> Json.obj("leftProfile.status" -> "deleted","rightProfile.status" -> "deleted"))

          matches.update(Json.obj("_id" -> Json.obj("$oid" -> matchId)), update).map {_.ok}
        } else {
          Future.successful(true)
        }
      })
    }
  }
  override def convertStatus(matchId: String, firingCode: SampleCode, status: String): Future[Seq[SampleCode]] = {

    val jsArrayOr = JsArray(Seq(Json.obj("leftProfile.globalCode" -> firingCode), Json.obj("rightProfile.globalCode" -> firingCode)))
    val jsArrayAnd = JsArray(Seq(Json.obj("_id" -> Json.obj("$oid" -> matchId)), Json.obj("$or" -> jsArrayOr)))

    val find = Json.obj("$and" -> jsArrayAnd)

    matches.find(find)
      .one[MatchResult] flatMap { matchResOpt =>
      matchResOpt.fold[Future[Seq[SampleCode]]](Future.successful(Nil))(matchRes => {
        val leftAssignee = matchRes.leftProfile.globalCode
        val rightAssignee = matchRes.rightProfile.globalCode

        val update = if (matchRes.leftProfile.assignee == matchRes.rightProfile.assignee) {
          (Json.obj("$set" -> Json.obj("leftProfile.status" -> status, "rightProfile.status" -> status)),
            Seq(matchRes.leftProfile.globalCode, matchRes.rightProfile.globalCode))
        } else if (rightAssignee == firingCode) {
          (Json.obj("$set" -> Json.obj("rightProfile.status" -> status)),
            Seq(matchRes.rightProfile.globalCode))
        } else { //(leftAssignee == userId)
          (Json.obj("$set" -> Json.obj("leftProfile.status" -> status)),
            Seq(matchRes.leftProfile.globalCode))
        }
        matches.update(Json.obj("_id" -> Json.obj("$oid" -> matchId)), update._1).map { x =>
          if (x.ok) {
            update._2
          } else
            Nil
        }
      })
    }
  }

  private def getMatchesRDD(search: MatchGroupSearch): RDD[Document] = {
    val matchesTables = if(search.isCollapsing.contains(true)){"collapsingMatches"}else{"matches"}
    val matchesReadConfig = ReadConfig(Map("uri" -> s"$mongoUri.$matchesTables"), None)


    val filterStatus = if(search.status.isDefined ){
      search.status match {
        case Some("conflict") => Filters.or(
            Filters.and( Filters.eq("leftProfile.status", MatchGlobalStatus.discarded.toString), Filters.eq("rightProfile.status", MatchGlobalStatus.hit.toString)),
            Filters.and( Filters.eq("leftProfile.status", MatchGlobalStatus.hit.toString), Filters.eq("rightProfile.status", MatchGlobalStatus.discarded.toString)))

        case Some("pending") => Filters.or(
            Filters.eq("leftProfile.status", MatchStatus.pending.toString),
            Filters.eq("rightProfile.status", MatchStatus.pending.toString))

        case _ => Filters.and(
            Filters.eq("leftProfile.status", search.status.get),
            Filters.eq("rightProfile.status", search.status.get))
      }
    }else {
      Filters.and(
      Filters.ne("leftProfile.status", MatchStatus.deleted.toString),
      Filters.ne("rightProfile.status", MatchStatus.deleted.toString))
    }

    val tipo  = if(search.tipo.isDefined){
      Filters.and( filterStatus,
       Filters.eq("type", search.tipo.get ))
    }else{
      Filters.and( filterStatus,
        Filters.ne("type", 4 ))
    }



    val filters = if(search.isCollapsing.contains(true) && search.courtCaseId.isDefined){
      Filters.and(
      Filters.eq("idCourtCase", search.courtCaseId.get),
      Filters.eq("leftProfile.globalCode", search.globalCode.text),
      Filters.ne("leftProfile.status", MatchStatus.deleted.toString),
      Filters.ne("rightProfile.status", MatchStatus.deleted.toString))
    }else{
      Filters.and( tipo,
        Filters.or(Filters.eq("leftProfile.globalCode", search.globalCode.text),
          Filters.eq("rightProfile.globalCode", search.globalCode.text)),
        Filters.ne("leftProfile.status", MatchStatus.deleted.toString),
        Filters.ne("rightProfile.status", MatchStatus.deleted.toString))
    }

    val matchesFilter =
      if ( (search.isCollapsing.contains(true) && search.courtCaseId.isDefined) || search.isSuperUser) `match`(filters)
      else `match`(Filters.and(filters,
        Filters.or(Filters.eq("leftProfile.assignee", search.user),
          Filters.eq("rightProfile.assignee", search.user))))

    MongoSpark
      .builder()
      .sparkContext(Spark2.context)
      .connector(Spark2.connector)
      .readConfig(matchesReadConfig)
      .pipeline(Seq(matchesFilter))
      .build()
      .toRDD()
  }

  private def getProfilesRDD(globalCodes: Array[String]) = {
    import scala.collection.JavaConverters._

    val profilesReadConfig = ReadConfig(Map("uri" -> s"$mongoUri.profiles"), None)

    val profilesFilter = `match`(Filters.and(
     // Filters.ne("deleted", true),
      Filters.in("globalCode", globalCodes.toIterable.asJava)
    ))

    val profilesProjection = project(include("globalCode", "contributors", "isReference", "internalSampleCode"))

    MongoSpark
      .builder()
      .sparkContext(Spark2.context)
      .connector(Spark2.connector)
      .readConfig(profilesReadConfig)
      .pipeline(Seq(profilesFilter, profilesProjection))
      .build()
      .toRDD()
      .map { doc =>
        val globalCode = doc.getString("globalCode")
        val contributors = doc.getInteger("contributors") // TODO: ¿Or else 1?
        val isReference = doc.getBoolean("isReference")
        val internalSampleCode = doc.getString("internalSampleCode")
        (globalCode, contributors, isReference, internalSampleCode)
      }.keyBy(_._1)
  }

  private def getMatchesAndProfilesRDD(search: MatchGroupSearch) = {
    val matchesRDD = getMatchesRDD(search)
    val globalCodes = matchesRDD.flatMap(doc => {
      val leftProfile = doc.get("leftProfile", classOf[Document]).getString("globalCode")
      val rightProfile = doc.get("rightProfile", classOf[Document]).getString("globalCode")
        Array(leftProfile, rightProfile)
    }).distinct.collect
    val profilesRDD = getProfilesRDD(globalCodes)

    val leftProfileRDD = matchesRDD.keyBy(_.get("leftProfile", classOf[Document]).getString("globalCode"))
    val joined1 = leftProfileRDD.join(profilesRDD)
    val rightProfileRDD = joined1.keyBy(_._2._1.get("rightProfile", classOf[Document]).getString("globalCode"))
    val joined2 = rightProfileRDD.join(profilesRDD)

    joined2.map {
      case (rightGlobalCode, ((leftGlobalCode, (matchResult, leftProfile)), rightProfile)) => (matchResult, leftProfile, rightProfile)
    }
  }

  private def filterByGroup(search: MatchGroupSearch,
                            rdd: RDD[(Document, (String, Integer, java.lang.Boolean, String), (String, Integer, java.lang.Boolean, String))]) = {
    val globalCode = search.globalCode.text
//  _1 = globalCode, _2 = contributors, _3 = isReference, _4 = internalSampleCode
    search.kind match {
      case MatchKind.Normal =>
        rdd.filter { case (mr, leftProfile, rightProfile) =>
        (globalCode == leftProfile._1 && rightProfile._2 == 1 && !leftProfile._3) ||
        (globalCode == rightProfile._1 && leftProfile._2 == 1 && !rightProfile._3) || (leftProfile._3 && rightProfile._3) }
      case MatchKind.MixMix =>
        rdd.filter { case (mr, leftProfile, rightProfile) => leftProfile._2 == 2 && rightProfile._2 == 2}
      case MatchKind.Restricted => rdd.filter {
        case (mr, leftProfile, rightProfile) => (globalCode == leftProfile._1 && leftProfile._3 && ! rightProfile._3) ||
          (globalCode == rightProfile._1 && rightProfile._3 && !leftProfile._3) ||
          (globalCode == leftProfile._1 && !leftProfile._3 && leftProfile._2 == 1 && ! rightProfile._3 && rightProfile._2 > 1) ||
          (globalCode == rightProfile._1 && !rightProfile._3 && rightProfile._2 == 1 && ! leftProfile._3 && leftProfile._2 > 1)
      }
      case MatchKind.Other => rdd.filter { case (mr, leftProfile, rightProfile) =>
        (leftProfile._2 > 2 && rightProfile._2 > 1) || (leftProfile._2 > 1 && rightProfile._2 > 2) }
      case _ => rdd
    }
  }

  private def paginateGroup(search: MatchGroupSearch,
                    rdd: RDD[(Document, (String, Integer, java.lang.Boolean, String), (String, Integer, java.lang.Boolean, String))]) = {
    val page = search.page
    val pageSize = search.pageSize
    rdd.zipWithIndex.flatMap { case (element, index) => if (index < pageSize*(page-1)) None else Some(element) }.take(pageSize)
  }

  private def sortGroup(search: MatchGroupSearch,
                      rdd: RDD[(Document, (String, Integer, java.lang.Boolean, String), (String, Integer, java.lang.Boolean, String))]) = {

    val isSearchingByOwnerStatus = search.sortField == "ownerStatus"
    val globalCode = search.globalCode.text
    val ascending = search.ascending

    search.sortField match {
      case "globalCode" => rdd.sortBy ({ case (document, leftProfile, rightProfile) => (getGlobalCode(globalCode, leftProfile, rightProfile), document.getObjectId("_id").toString) }, ascending)
      case "type" => rdd.sortBy ({ case (document, _, _) => (document.getInteger("type"), document.getObjectId("_id").toString) }, search.ascending)
      case "totalAlleles" => rdd.sortBy ({ case (document, _, _) => (document.get("result", classOf[Document]).getInteger("totalAlleles"), document.getObjectId("_id").toString) }, ascending)
      case "sharedAllelePonderation" => rdd.sortBy ({ case (document, leftProfile, rightProfile) => (getSharedAllelePonderation(globalCode, document, leftProfile, rightProfile), document.getObjectId("_id").toString) }, ascending)
      case "ownerStatus" => rdd.sortBy ({ case (document, leftProfile, rightProfile) => (getStatus(globalCode, isSearchingByOwnerStatus, document, leftProfile, rightProfile), document.getObjectId("_id").toString) }, ascending)
      case "otherStatus" => rdd.sortBy ({ case (document, leftProfile, rightProfile) => (getStatus(globalCode, isSearchingByOwnerStatus, document, leftProfile, rightProfile), document.getObjectId("_id").toString) }, ascending)
      case _ => rdd.sortBy ({ case (document, _, _) => (document.getDate("matchingDate"), document.getObjectId("_id")) }, ascending)
    }
  }

  override def getGlobalMatchStatus(leftProfileStatus: MatchStatus.Value, rightProfileStatus: MatchStatus.Value): MatchGlobalStatus.Value = {
    (leftProfileStatus, rightProfileStatus) match {
      case (MatchStatus.pending, _)                       => MatchGlobalStatus.pending
      case (_, MatchStatus.pending)                       => MatchGlobalStatus.pending
      case (MatchStatus.hit, MatchStatus.hit)             => MatchGlobalStatus.hit
      case (MatchStatus.discarded, MatchStatus.discarded) => MatchGlobalStatus.discarded
      case (_, _)                                         => MatchGlobalStatus.conflict
    }
  }

  def getTotalMatchesByGroup(search: MatchGroupSearch): Int = {
    val rdd = getMatchesAndProfilesRDD(search)
    val groupRDD = filterByGroup(search, rdd)
    groupRDD.count().toInt
  }

  def getMatchesByGroup(search: MatchGroupSearch): Seq[MatchingResult] = {
    val globalCode = search.globalCode

    val docToMatch = (doc: Document, leftProfile: (String, Integer, java.lang.Boolean, String),
                      rightProfile: (String, Integer, java.lang.Boolean, String)) => {
      val json = Json.parse(doc.toJson())
      val result = Json.fromJson[MatchResult](json)
      result.fold[MatchingResult](err => {
        throw new ParseException(s"Can't parse: ${doc.toJson()}, error is: $err", 1)
      }, { mr =>

        val right = mr.rightProfile.globalCode == globalCode

        val (ownerProfile, matchingProfile) = if (right) (mr.rightProfile, mr.leftProfile) else (mr.leftProfile, mr.rightProfile)
        val (ownerInfo, matchingInfo) = if (right) (rightProfile, leftProfile) else (leftProfile, rightProfile)

        val sharedAllelePonderation = if (!ownerInfo._3 && matchingInfo._3) {
          if (right) mr.result.leftPonderation else mr.result.rightPonderation
        } else {
          if (right) mr.result.rightPonderation else mr.result.leftPonderation
        }

        MatchingResult(
          mr._id.id,
          matchingProfile.globalCode,
          matchingInfo._4,
          mr.result.stringency,
          mr.result.matchingAlleles,
          mr.result.totalAlleles,
          matchingProfile.categoryId,
          ownerProfile.status,
          matchingProfile.status,
          getGlobalMatchStatus(ownerProfile.status, matchingProfile.status),
          sharedAllelePonderation,
          matchingInfo._2,
          matchingInfo._3,
          mr.result.algorithm,
          mr.`type`,
          mr.result.allelesRanges,
          mr.lr,
          mr.mismatches)

      })
    }

    val rdd = getMatchesAndProfilesRDD(search)
    val groupRDD = filterByGroup(search, rdd)
    val sortedRDD = sortGroup(search, groupRDD)
    val paginated = paginateGroup(search, sortedRDD)

    paginated.map { case (mr, lp, rp) => docToMatch(mr, lp, rp) }
  }


  override def getProfileLr( globalCode: SampleCode, isCollapsing : Boolean): Future[MatchCardMejorLr] = {
    val matchesName = if(isCollapsing){collapsingMatches}else{matches}

    val perfil = matchesName.find(Json.obj("$or" -> Json.arr(
      Json.obj("leftProfile.globalCode" -> globalCode.text),
      Json.obj("rightProfile.globalCode" -> globalCode.text))))
        .sort(Json.obj("lr" -> -1)).cursor[MatchResult]()
      .collect[Seq](Int.MaxValue,Cursor.FailOnError[Seq[MatchResult]]())

    perfil.map{ x =>

      val right = (x.head.rightProfile.globalCode == globalCode)
      val (ownerInfo, matchingInfo) = if(right) (getProfileReference(x.head.rightProfile.globalCode.text), getProfileReference(x.head.leftProfile.globalCode.text) ) else (getProfileReference(x.head.leftProfile.globalCode.text), getProfileReference(x.head.rightProfile.globalCode.text))

      val sharedAllelePonderation = if (!ownerInfo && matchingInfo) {
        if (right) x.head.result.leftPonderation else x.head.result.rightPonderation
      } else {
        if (right) x.head.result.rightPonderation  else  x.head.result.leftPonderation
      }

        if(x.head.leftProfile.globalCode.equals(globalCode)){
          MatchCardMejorLr(x.head.leftProfile.globalCode.text,x.head.rightProfile.categoryId,x.head.result.totalAlleles,
            x.head.leftProfile.status,x.head.rightProfile.status,sharedAllelePonderation,x.head.mismatches,x.head.lr,x.head.rightProfile.globalCode,x.head.`type`)
        }else{

          MatchCardMejorLr(x.head.leftProfile.globalCode.text,x.head.leftProfile.categoryId,x.head.result.totalAlleles,
            x.head.rightProfile.status,x.head.leftProfile.status,sharedAllelePonderation,x.head.mismatches,x.head.lr,x.head.leftProfile.globalCode,x.head.`type`)
        }
      }


  }


  private def getProfileReference(globalCodes: String) = {

    val profilesReadConfig = ReadConfig(Map("uri" -> s"$mongoUri.profiles"), None)

    val profilesFilter = `match`(Filters.and(
      //Filters.ne("deleted", true),
      Filters.in("globalCode", globalCodes)
    ))

    val profilesProjection = project(include("isReference"))

   MongoSpark
      .builder()
      .sparkContext(Spark2.context)
      .connector(Spark2.connector)
      .readConfig(profilesReadConfig)
      .pipeline(Seq(profilesFilter, profilesProjection))
      .build()
      .toRDD()
      .map { doc =>
      val globalCode = doc.getString("globalCode")
      val isReference = doc.getBoolean("isReference")
        (globalCode,isReference)
      }.keyBy(_._1).first()._2._2
  }

  override def matchesByGlobalCode(globalCode: SampleCode): Future[Seq[MatchResult]] = {

    val arrayOr = Json.obj("$or" -> Seq(Json.obj("leftProfile.globalCode" -> globalCode.text),
      Json.obj("rightProfile.globalCode" -> globalCode.text)))

    val notDeleted = Json.obj("$ne" -> "deleted")

    val query = Json.obj("$and" -> Seq(arrayOr, Json.obj("leftProfile.status" -> notDeleted), Json.obj("rightProfile.status" -> notDeleted)))

    matches.find(query).cursor[MatchResult]().collect[Seq](Int.MaxValue, Cursor.FailOnError[Seq[MatchResult]]())
  }

  def matchesByAssignee(userId: String): Future[Seq[MatchResult]] = {

    val notDeleted = Json.obj("$ne" -> "deleted")

    val arrayOr = Json.obj("$or" -> Seq(Json.obj("leftProfile.assignee" -> userId), Json.obj("rightProfile.assignee" -> userId)))

    val query = Json.obj("$and" -> Seq(arrayOr, Json.obj("leftProfile.status" -> notDeleted), Json.obj("rightProfile.status" -> notDeleted)))

    matches.find(query).cursor[MatchResult]().collect[Seq](Int.MaxValue, Cursor.FailOnError[Seq[MatchResult]]())
  }

  private def prepareDataQuery(isSuperUser: Boolean, userId: String,isCollapsing:Boolean): List[JsValue] = {
    val groupString = if(isCollapsing){
      s"""
{"$$group": {
  "_id": {"left":"$$leftProfile.globalCode",
        "leftStatus":"$$leftProfile.status",
        "date": "$$matchingDate"},
  "relevant": { "$$push": { "$$cond": {
                "if": {"$$or":[{"$$eq": [true, true]},
                           {"$$and":[{"$$eq": ["$$leftProfile.assignee", "$userId"]}]}]},
                "then": ["$$leftProfile.globalCode"],
                "else": {"$$cond": {
                        "if": {"$$eq": ["$$leftProfile.assignee", "$userId"]},
                        "then": ["$$leftProfile.globalCode"],
                        "else": {"$$cond": { "if": {"$$eq": ["$$rightProfile.assignee", "$userId"]},
                                             "then": ["$$rightProfile.globalCode"],
                                             "else": [] }}
                        }} } } } } }"""
    }
    else{s"""
{"$$group": {
  "_id": {"left":"$$leftProfile.globalCode",
        "right":"$$rightProfile.globalCode",
        "leftStatus":"$$leftProfile.status",
        "rightStatus": "$$rightProfile.status",
        "date": "$$matchingDate",
         "leftCategory": "$$leftProfile.categoryId",
         "rightCategory": "$$rightProfile.categoryId"},
  "relevant": { "$$push": { "$$cond": {
                "if": {"$$or":[{"$$eq": [$isSuperUser, true]},
                           {"$$and":[{"$$eq": ["$$leftProfile.assignee", "$userId"]}, {"$$eq": ["$$rightProfile.assignee", "$userId"]}]}]},
                "then": ["$$leftProfile.globalCode", "$$rightProfile.globalCode"],
                "else": {"$$cond": {
                        "if": {"$$eq": ["$$leftProfile.assignee", "$userId"]},
                        "then": ["$$leftProfile.globalCode"],
                        "else": {"$$cond": { "if": {"$$eq": ["$$rightProfile.assignee", "$userId"]},
                                             "then": ["$$rightProfile.globalCode"],
                                             "else": [] }}
                        }} } } } } }"""}
    val projectString = s"""
{"$$project": {
  "group": "$$relevant",
  "match": {"$$cond": { "if": {"$$eq": ["$$_id.left", "$$relevant"]}, "then": "$$_id.right", "else": "$$_id.left" } },
  "status": {"$$switch": {"branches": [
          { "case": {"$$and":[{"$$eq":["$$_id.leftStatus", "${MatchStatus.hit.toString}"]}, {"$$eq":["$$_id.rightStatus", "${MatchStatus.hit.toString}"]}]}, "then": "${MatchGlobalStatus.hit.toString}" },
          { "case": {"$$and":[{"$$eq":["$$_id.leftStatus", "${MatchStatus.discarded.toString}"]}, {"$$eq":["$$_id.rightStatus", "${MatchStatus.discarded.toString}"]}]}, "then": "${MatchGlobalStatus.discarded.toString}" },
          { "case": {"$$and":[{"$$eq":["$$_id.leftStatus", "${MatchStatus.discarded.toString}"]}, {"$$eq":["$$_id.rightStatus", "${MatchStatus.hit.toString}"]}]}, "then": "${MatchGlobalStatus.conflict.toString}" },
          { "case": {"$$and":[{"$$eq":["$$_id.leftStatus", "${MatchStatus.hit.toString}"]}, {"$$eq":["$$_id.rightStatus", "${MatchStatus.discarded.toString}"]}]}, "then": "${MatchGlobalStatus.conflict.toString}" }],
          "default": "${MatchGlobalStatus.pending.toString}"}},
  "date": "$$_id.date",
  "category":{"$$cond": { "if": {"$$eq": ["$$_id.left", "$$relevant"]}, "then": "$$_id.leftCategory", "else": "$$_id.rightCategory" } }   }}"""

    val group = Json.parse(groupString)
    val unwind = Json.parse(s"""{"$$unwind": "$$relevant"}""")
    val project = Json.parse(projectString)
    List(group, unwind, unwind, project)
  }

  private def groupDataQuery(): List[JsValue] = {
    List(Json.parse(s"""
{"$$group": {"_id": "$$group",
           "matches": {"$$push": "$$match"},
           "dates": {"$$push": "$$date"},
           "lastDate": {"$$max": "$$date"},
           "category" : {"$$push": "$$category"},
           "${MatchGlobalStatus.pending.toString}": {"$$sum": {"$$cond": { "if": {"$$eq": ["$$status", "${MatchGlobalStatus.pending.toString}"]}, "then": 1, "else": 0 } }},
           "${MatchGlobalStatus.hit.toString}": {"$$sum": {"$$cond": { "if": {"$$eq": ["$$status", "${MatchGlobalStatus.hit.toString}"]}, "then": 1, "else": 0 } }},
           "${MatchGlobalStatus.discarded.toString}": {"$$sum": {"$$cond": { "if": {"$$eq": ["$$status", "${MatchGlobalStatus.discarded.toString}"]}, "then": 1, "else": 0 } }},
           "${MatchGlobalStatus.conflict.toString}": {"$$sum": {"$$cond": { "if": {"$$eq": ["$$status", "${MatchGlobalStatus.conflict.toString}"]}, "then": 1, "else": 0 } }}
}}"""))
  }

  private def applyFiltersQuery(search: MatchCardSearch) = {
    var filters = List.empty[JsValue]

    if (search.profile.isDefined) {
      filters = filters :+ Json.obj("_id" -> search.profile.get)
    }
    if (search.hourFrom.isDefined) {
      filters = filters :+ Json.obj("dates" -> Json.obj("$gte" -> Json.obj("$date" -> search.hourFrom.get)))
    }
    if (search.hourUntil.isDefined) {
      filters = filters :+ Json.obj("dates" -> Json.obj("$lte" -> Json.obj("$date" -> search.hourUntil.get)))
    }
    if (search.status.isDefined) {
      filters = filters :+ Json.obj(search.status.get.toString -> Json.obj("$gt" -> 0))
    }
    if (search.laboratoryCode.isDefined) {
      filters = filters :+ Json.obj("_id" -> Json.obj("$regex" -> s".*-${search.laboratoryCode.get}-.*"))
    }

    if (search.categoria.isDefined){
      filters = filters :+ Json.obj("category" -> search.categoria.get)
    }

    val sort: JsObject = Json.obj("$sort" -> Json.obj("lastDate" -> {if (search.ascending) 1 else -1}, "_id" -> 1))

    if (filters.isEmpty) List(sort)
    else List (Json.obj("$match" -> Json.obj("$and" -> filters)), sort)
  }


  override def getMatches(search: MatchCardSearch) = {
    val matchesName = if(search.isCollapsing.contains(true)){collapsingMatches.name}else{matches.name}
    val query = Json.obj(
      "aggregate" -> matchesName,
      "pipeline" -> getMatchesQuery(search)
        .++(List(Json.obj("$skip" -> search.page*search.pageSize), Json.obj("$limit" -> search.pageSize)))
    )

    val runner = Command.run(JSONSerializationPack, FailoverStrategy.default)

    runner.apply(if(search.isCollapsing.contains(true)){collapsingMatches.db}else{matches.db}, runner.rawCommand(query))
      .cursor[BSONDocument](ReadPreference.nearest)
      .collect[List](search.pageSize, Cursor.FailOnError[List[BSONDocument]]()) map { list =>
      list flatMap { document =>
        val json = Json.toJson(document)
        (json \ "result").as[Seq[JsValue]]
      }
    }
  }

  override def getTotalMatches(search: MatchCardSearch): Future[Int] = {
    val matchesName = if(search.isCollapsing.contains(true)){collapsingMatches.name}else{matches.name}
    val query = Json.obj(
      "aggregate" -> matchesName,
      "pipeline" -> getMatchesQuery(search).++(List(Json.obj("$group" -> Json.obj("_id" -> "0", "count" -> Json.obj("$sum" -> 1)))))
    )
    val json = query.toString()
    println(json)
    val runner = Command.run(JSONSerializationPack, FailoverStrategy.default)
    runner.apply(if(search.isCollapsing.contains(true)){collapsingMatches.db}else{matches.db}, runner.rawCommand(query))
      .one[BSONDocument](ReadPreference.nearest)
      .map { bson => (Json.toJson(bson) \ "result").as[Seq[Count]].headOption.fold(0)(_.count) }
  }
  override def getAllTotalMatches(search: MatchCardSearch): Future[Int] = {
    val query = Json.obj()
    matches.count(Some(query))
  }
  def getByMatchingProfileId(matchingId: String,isCollapsing:Option[Boolean] = None,isScreening:Option[Boolean] = None): Future[Option[MatchResult]] = {
    (if(isCollapsing.contains(true)){collapsingMatches}else{if(isScreening.contains(true)){screeningMatches}else{matches}})
      .find(Json.obj("_id" -> Json.obj("$oid" -> matchingId))).one[MatchResult]
  }

  def getByFiringAndMatchingProfile(firingCode: SampleCode, matchingCode: SampleCode): Future[Option[MatchResult]] = {

    val leftAndRight = Json.obj("$and" -> Seq(Json.obj("leftProfile.globalCode" -> firingCode), Json.obj("rightProfile.globalCode" -> matchingCode)))
    val rightAndLeft = Json.obj("$and" -> Seq(Json.obj("leftProfile.globalCode" -> matchingCode), Json.obj("rightProfile.globalCode" -> firingCode)))

    val query = Json.obj("$or" -> Seq(leftAndRight, rightAndLeft))
    matches.find(query).one[MatchResult]
  }
  def getScreeningByFiringProfile(firingCode: SampleCode): Future[Seq[MatchResultScreening]] = {
    val query = Json.obj("leftProfile.globalCode" -> firingCode)
    screeningMatches.find(query).cursor[MatchResult]().collect[Seq](Int.MaxValue, Cursor.FailOnError[Seq[MatchResult]]())
      .map(x => x.map(y => MatchResultScreening(y.rightProfile.globalCode.text,y._id.id,false)))
  }
  override def matchesNotDiscarded(globalCode: SampleCode): Future[Seq[MatchResult]] = {

    val notDeleted = Json.obj("$ne" -> MatchStatus.deleted)
    val notDiscarded = Json.obj("$ne" -> MatchStatus.discarded)

    val leftAndNotDiscarded = Json.obj("leftProfile.globalCode" -> globalCode,
      "leftProfile.status" -> notDiscarded,
      "rightProfile.status" -> notDeleted)
    val rightAndNotDiscarded = Json.obj("rightProfile.globalCode" -> globalCode,
      "rightProfile.status" -> notDiscarded,
      "leftProfile.status" -> notDeleted)

    val query = Json.obj("$or" -> Seq(leftAndNotDiscarded, rightAndNotDiscarded))
    matches.find(query).cursor[MatchResult]().collect[Seq](Int.MaxValue, Cursor.FailOnError[Seq[MatchResult]]())
  }

  override def matchesWithFullHit(globalCode: SampleCode): Future[Seq[MatchResult]] = {
    matchesWithHit(globalCode, "$and")
  }

  override def matchesWithPartialHit(globalCode: SampleCode): Future[Seq[MatchResult]] = {
    matchesWithHit(globalCode, "$or")
  }

  override def removeMatchesByProfile(globalCode: SampleCode): Future[Either[String,String]] = Future{
    val query = Json.obj("$or" -> Json.arr(
      Json.obj("leftProfile.globalCode" -> globalCode),
      Json.obj("rightProfile.globalCode" -> globalCode))
    )

    //val query = Json.obj("$and" -> Seq(leftOrRight, Json.obj("leftProfile.status" -> MatchStatus.deleted),
    //  Json.obj("rightProfile.status" -> MatchStatus.deleted)))

    matches.remove(query)
    Right(globalCode.text)
  }
  private def matchesWithHit(globalCode: SampleCode, operator: String): Future[Seq[MatchResult]] = {
    val leftOrRight = Json.obj("$or" -> Json.arr(
      Json.obj("leftProfile.globalCode" -> globalCode),
      Json.obj("rightProfile.globalCode" -> globalCode))
    )

    val hit = Json.obj(operator -> Seq(Json.obj("leftProfile.status" -> MatchStatus.hit),
      Json.obj("rightProfile.status" -> MatchStatus.hit)))

    val query = Json.obj("$and" -> Seq(leftOrRight, hit))
    matches.find(query).cursor[MatchResult]().collect[Seq](Int.MaxValue, Cursor.FailOnError[Seq[MatchResult]]())
  }
//  override def matchesNotDiscarded(globalCode: SampleCode): Future[Seq[MatchResult]] = {
//    val leftOrRight = Json.obj("$or" -> Json.arr(
//      Json.obj("leftProfile.globalCode" -> globalCode),
//      Json.obj("rightProfile.globalCode" -> globalCode))
//    )
//
//    val status = Json.obj("$or" -> Seq(
//      Json.obj("leftProfile.status" -> MatchStatus.hit),
//      Json.obj("rightProfile.status" -> MatchStatus.hit),
//      Json.obj("leftProfile.status" -> MatchStatus.pending),
//      Json.obj("rightProfile.status" -> MatchStatus.pending)))
//
//    val query = Json.obj("$and" -> Seq(leftOrRight, status))
//    matches.find(query).cursor[MatchResult]().collect[Seq](Int.MaxValue, Cursor.FailOnError[Seq[MatchResult]]())
//  }
  private def getMatchesQuery(search: MatchCardSearch): List[JsValue] = {
    val notDeleted = if(search.isCollapsing.contains(true) && search.courtCaseId.isDefined){
      Json.obj("$match" ->
        Json.obj(
          "leftProfile.status" -> Json.obj("$ne" -> MatchStatus.deleted.toString),
          "rightProfile.status" -> Json.obj("$ne" -> MatchStatus.deleted.toString),
          "idCourtCase" -> Json.obj("$eq" -> search.courtCaseId.get)
        )
      )
    } else{
      Json.obj("$match" ->
        Json.obj(
          "leftProfile.status" -> Json.obj("$ne" -> MatchStatus.deleted.toString),
          "rightProfile.status" -> Json.obj("$ne" -> MatchStatus.deleted.toString)
        )
      )

    }

    List(notDeleted) ++
      prepareDataQuery(search.isSuperUser, search.user,search.isCollapsing.contains(true)) ++
      groupDataQuery() ++
      applyFiltersQuery(search)
  }
  def convertToBson(matchingResult:MatchResult):BSONDocument = {

    BSONDocument("_id"->BSONObjectID(matchingResult._id.id),
      "matchingDate"->matchingResult.matchingDate.date,
      "leftProfile"->createBsonProfile(matchingResult.leftProfile),
      "rightProfile"->createBsonProfile(matchingResult.rightProfile),
      "result"->createResultBson(matchingResult.result),
      "type"->matchingResult.`type`,
      "n"->matchingResult.n,
      "lr"-> formatDouble(matchingResult.lr),
      "mismatches"-> matchingResult.mismatches

    )
  }
  def convertToJson(matchingResult:MatchResult): JsValue = {
      Json.toJson(convertToBson(matchingResult))
  }
  def insertMatchingResult(matchingResult:MatchResult):Future[Unit] = {

    var matchBSON = convertToBson(matchingResult)

    val selector = Json.obj("_id" -> BSONObjectID(matchingResult._id.id))
    if(matchingResult.superiorProfileInfo.isDefined){
      val superiorProfileInfo = Json.obj("superiorProfileData"->Json.toJson(matchingResult.superiorProfileInfo.get.superiorProfileData),
                                          "profile"->Json.toJson(matchingResult.superiorProfileInfo.get.profile))
      matchBSON = matchBSON.++("superiorProfileInfo"->superiorProfileInfo)
    }
    val modifier = Json.obj("$set" -> matchBSON)

    matches.findAndUpdate(selector,modifier,upsert = true).map {result => ()}
//    matches.insert(matchBSON).map {result => ()}

  }
  def createResultBson(mr: NewMatchingResult):BSONDocument = {
    val matchingAlleles = mr.matchingAlleles.foldLeft(BSONDocument(""->None)){
      (bson,element) => bson.++(element._1.toString->element._2.toString)
    }

    val allelesRanges = mr.allelesRanges.map(allelesRanges =>
      allelesRanges.foldLeft(BSONDocument(""->None)){
        (bson,element) => bson.++(element._1.toString->BSONDocument("min"->element._2.min.doubleValue(),"max"->element._2.max.doubleValue()))
      }
    )
    BSONDocument("stringency"->mr.stringency.toString,
      "matchingAlleles"->matchingAlleles,
      "totalAlleles"->mr.totalAlleles,
      "categoryId"->mr.categoryId.text,
      "leftPonderation"->mr.leftPonderation,
      "rightPonderation"->mr.rightPonderation,
      "algorithm"->mr.algorithm.toString,
      "allelesRanges"->allelesRanges
    )
  }
  def createBsonProfile(matchingProfile:MatchingProfile) : BSONDocument= {
    BSONDocument("globalCode"->matchingProfile.globalCode.text,
      "assignee"->matchingProfile.assignee,
      "status"->matchingProfile.status.toString,
      "comments"->matchingProfile.comments,
      "categoryId"->matchingProfile.categoryId.text)
  }
  override def findSuperiorProfile(globalCode:SampleCode):Future[Option[Profile]]= {

   matches.find(BSONDocument("superiorProfileInfo.profile.globalCode" -> globalCode.text)).one[BSONDocument].map{
      _.map(bson => Json.toJson(bson))
    }.map{
      json =>  json.flatMap(jsonValue => Json.fromJson[MatchResult](jsonValue).fold(  invalid = {
        fieldErrors =>
          None
      },
        valid = {
          mr => Some(mr)
        }))
    }.map(x => x.map(_.superiorProfileInfo).flatMap(_.map(_.profile)))
  }

  override def findSuperiorProfileData(globalCode:SampleCode):Future[Option[ProfileData]]= {

    matches.find(BSONDocument("superiorProfileInfo.profile.globalCode" -> globalCode.text)).one[BSONDocument].map{
      _.map(bson => Json.toJson(bson))
    }.map{
      json =>  json.flatMap(jsonValue => Json.fromJson[MatchResult](jsonValue).fold(  invalid = {
        fieldErrors =>
          None
      },
        valid = {
          mr => Some(mr)
        }))
    }.map(x => x.map(_.superiorProfileInfo).flatMap(_.map(x =>
      ProfileData(
        x.profile.categoryId,
        x.profile.globalCode,
        None,
        None,
        None,
        None,
        None,
        None,
        x.superiorProfileData.internalSampleCode,
        x.superiorProfileData.assignee,
        x.superiorProfileData.laboratory,
        false,
        None,
        None,
        None,
        None,
        None,
        None,true))))
  }

  override def discardCollapsingMatches(ids:List[String],courtCaseId:Long) : Future[Unit] = {
    val query = Json.obj("_id" -> Json.obj("$oid" -> ids.head),"idCourtCase" -> courtCaseId)
    collapsingMatches.remove(query).map(result => ())
  }
  override def discardScreeningMatches(ids:List[String]) : Future[Unit] = {
    if(!ids.isEmpty){
      Future.sequence(ids.map(discardScreeningMatch)).map(_ => ())
    }else{
      Future.successful(())
    }
  }
  def discardScreeningMatch(id:String) : Future[Unit] = {
      val query = Json.obj("_id" -> Json.obj("$oid" -> id))
      screeningMatches.remove(query).map(result => ())
  }
  override def discardCollapsingByLeftProfile(globalCode:String,courtCaseId:Long) : Future[Unit] = {
    val gc = Json.obj("leftProfile.globalCode" -> globalCode)
    val query = Json.obj("$and" -> Seq(gc, Json.obj("idCourtCase" -> courtCaseId)))

    collapsingMatches.remove(query).map(result => ())
  }

  override def discardCollapsingByRightProfile(globalCode:String,courtCaseId:Long) : Future[Unit] = {
    val gc = Json.obj("rightProfile.globalCode" -> globalCode)
    val query = Json.obj("$and" -> Seq(gc, Json.obj("idCourtCase" -> courtCaseId)))

    collapsingMatches.remove(query).map(result => ())
  }
  override def discardCollapsingByLeftAndRightProfile(globalCode:String,courtCaseId:Long) : Future[Unit] = {
    val leftOrRight = Json.obj("$or" -> Json.arr(
      Json.obj("leftProfile.globalCode" -> globalCode),
      Json.obj("rightProfile.globalCode" -> globalCode))
    )
    val query = Json.obj("$and" -> Seq(leftOrRight, Json.obj("idCourtCase" -> courtCaseId)))

    collapsingMatches.remove(query).map(result => ())
  }

  def numberOfMatchesHit(globalCode: String): Future[Int] = {
    val query = Json.obj("$or" -> Json.arr(
      Json.obj("leftProfile.globalCode" -> globalCode),
      Json.obj("rightProfile.globalCode" -> globalCode)) ,  "$and" -> Json.arr(
      Json.obj("leftProfile.status" -> MatchStatus.hit.toString),Json.obj("rightProfile.status" -> MatchStatus.hit.toString))
      )
    matches.count(Some(query))
  }

  def numberOfMatches(globalCode: String): Future[Int] = {
    val query = Json.obj("$or" -> Json.arr(
      Json.obj("leftProfile.globalCode" -> globalCode),
      Json.obj("rightProfile.globalCode" -> globalCode))
    )
    matches.count(Some(query))
  }

  def numberOfMatchesPending(globalCode: String): Future[Int] = {
    val query = Json.obj("$or" -> Json.arr(
      Json.obj("leftProfile.globalCode" -> globalCode ),
      Json.obj("rightProfile.globalCode" -> globalCode)) ,  "$or" -> Json.arr(
      Json.obj("leftProfile.status" -> MatchStatus.pending.toString),Json.obj("rightProfile.status" -> MatchStatus.pending.toString))
    )
    matches.count(Some(query))
  }

  def numberOfMatchesDescarte(globalCode: String): Future[Int] = {
    val query = Json.obj("$or" -> Json.arr(
      Json.obj("leftProfile.globalCode" -> globalCode ),
      Json.obj("rightProfile.globalCode" -> globalCode)) ,  "$and" -> Json.arr(
      Json.obj("leftProfile.status" -> MatchStatus.discarded.toString),Json.obj("rightProfile.status" -> MatchStatus.discarded.toString))
    )
    matches.count(Some(query))
  }

  def numberOfMatchesConflic(globalCode: String): Future[Int] = {
    val query = Json.obj("$or" -> Json.arr(
      Json.obj("leftProfile.globalCode" -> globalCode , "$or" -> Json.arr(
        Json.obj("leftProfile.status" -> MatchStatus.hit.toString,"rightProfile.status" -> MatchStatus.discarded.toString),
        Json.obj("leftProfile.status" -> MatchStatus.discarded.toString,"rightProfile.status" -> MatchStatus.hit.toString))),
      Json.obj("rightProfile.globalCode" -> globalCode, "$or" -> Json.arr(
        Json.obj("rightProfile.status" -> MatchStatus.hit.toString ,"leftProfile.status" -> MatchStatus.discarded.toString),
        Json.obj("rightProfile.status" -> MatchStatus.discarded.toString ,"leftProfile.status" -> MatchStatus.hit.toString)))
    ))
    matches.count(Some(query))
  }

  def numberOfMt(globalCode: String ): Future[Boolean] = {
    val query = Json.obj("$and" -> Json.arr(  Json.obj("$or" -> Json.arr(
      Json.obj("leftProfile.globalCode" -> globalCode),
        Json.obj("rightProfile.globalCode" -> globalCode)   ) ) , Json.obj("type" -> 4 )) )

   val mt = matches.count(Some(query))
    mt.map{x =>
    if(x>0){  true
    }else{  false } }
  }


  override def getByDateBetween(from : Option[Date], to : Option[Date]): Future[Seq[MatchResult]] = {
    var filters = List.empty[JsValue]

    filters = filters :+
      Json.obj(
        "leftProfile.status" -> Json.obj("$ne" -> MatchStatus.deleted.toString),
        "rightProfile.status" -> Json.obj("$ne" -> MatchStatus.deleted.toString)
      )


    if (from.isDefined) {
      filters = filters :+ Json.obj("matchingDate" -> Json.obj("$gte" -> Json.obj("$date" -> from.get)))
    }
    if (to.isDefined) {
      filters = filters :+ Json.obj("matchingDate" -> Json.obj("$lte" -> Json.obj("$date" -> to.get)))
    }

    val query = Json.obj("$and" -> filters)
    matches.find(query).cursor[MatchResult]().collect[Seq](Int.MaxValue, Cursor.FailOnError[Seq[MatchResult]]())
  }

}