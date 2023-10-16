package pedigree

import matching.MatchStatus
import play.api.Play.current
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.api.{Cursor, QueryOpts}
import play.modules.reactivemongo.json.collection.{JSONCollection, _}
import reactivemongo.api._
import play.modules.reactivemongo.json._
import reactivemongo.api.commands.Command
import reactivemongo.bson.BSONDocument
import types.Count

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

abstract class PedigreeMatchesRepository {
  def getMatches(search: PedigreeMatchCardSearch) : Future[Seq[PedigreeMatch]]
  def countMatches(search: PedigreeMatchCardSearch): Future[Int]
  def getMatchById(matchingId: String): Future[Option[PedigreeMatchResult]]
  def allMatchesDiscarded(pedigreeId: Long): Future[Boolean]
  def getMatchesByGroup(search: PedigreeMatchGroupSearch) : Future[Seq[PedigreeMatchResult]]
  def countMatchesByGroup(search: PedigreeMatchGroupSearch) : Future[Int]
  def discardProfile(matchId: String): Future[Either[String, String]]
  def discardPedigree(matchId: String): Future[Either[String, String]]
  def deleteMatches(idPedigree: Long): Future[Either[String, Long]]
  def confirmProfile(matchId: String): Future[Either[String, String]]
  def confirmPedigree(matchId: String): Future[Either[String, String]]
  def hasPendingMatches(pedigreeId: Long): Future[Boolean]
  def hasMatches(pedigreeId: Long): Future[Boolean]
  def numberOfPendingMatches(pedigreeId: Long): Future[Int]
  def numberOfMatches(pedigreeId: Long): Future[Int]
  def profileNumberOfPendingMatchesInPedigrees(globalCode: String,pedigreesIds:Seq[Long]): Future[Int]
  def profileNumberOfPendingMatches(globalCode: String): Future[Int]
  def countProfilesHitPedigrees(globalCodes:String):Future[Int]
  def countProfilesDiscardedPedigrees(globalCodes:String):Future[Int]
  def numberOfDiscardedMatches(pedigreeId: Long): Future[Int]
  def numberOfHitMatches(pedigreeId: Long): Future[Int]
  def getMatchByPedigree(matchingId: Long): Future[Option[PedigreeMatch]]
  def getMatchByProfile(globalCode: String): Future[Option[PedigreeMatch]]
  def getTypeCourtCasePedigree(pedigreeId: Long): Future[Option[String]]
  def getMejorLrPedigree(idPedigree: Long): Future[Option[MatchCardMejorLrPed]]
  def getMejorLrProf(globalCode: String): Future[Option[MatchCardMejorLrPed]]
  def getMatchesByGroupPedigree(search: PedigreeMatchGroupSearch): Future[List[MatchCardPed]]
  def getAllMatchNonDiscardedByGroup(id: String, group: String): Future[Seq[PedigreeMatchResult]]
}

class MongoPedigreeMatchesRepository extends PedigreeMatchesRepository {
  private def pedigreeMatches = Await.result(
    ReactiveMongoPlugin
      .database
      .map(
        _.collection[JSONCollection]("pedigreeMatches")
      ),
    Duration(10, SECONDS)
  )

  private def getMatchesQuery(search: PedigreeMatchCardSearch) = {
    var filters = List.empty[JsObject]

    val userFilter = if (!search.isSuperUser) {
        Json.obj("pedigree.assignee" -> search.user, "pedigree.status" -> Json.obj("$ne" -> MatchStatus.deleted.toString))
    } else {
      Json.obj("$or" -> Json.arr(
        Json.obj("profile.status" -> Json.obj("$ne" -> MatchStatus.deleted.toString)),
        Json.obj("pedigree.status" -> Json.obj("$ne" -> MatchStatus.deleted.toString))
      ))
    }
    filters = userFilter :: filters
    if (search.profile.isDefined) {
      val profileFilter = Json.obj("$or" -> Json.arr(
        Json.obj("profile.globalCode" -> search.profile.get),
        Json.obj("pedigree.globalCode" -> search.profile.get)
      ))
      filters = profileFilter :: filters
    }

    if (search.hourFrom.isDefined) {
      filters = Json.obj("matchingDate" -> Json.obj("$gte" -> Json.obj("$date" -> search.hourFrom.get))) :: filters
    }

    if (search.hourUntil.isDefined) {
      filters = Json.obj("matchingDate" -> Json.obj("$lte" -> Json.obj("$date" -> search.hourUntil.get))) :: filters
    }

    if(search.category.isDefined){
      filters = Json.obj("profile.categoryId" -> search.category.get ) :: filters
    }

    if(search.caseType.isDefined){
      filters = Json.obj("pedigree.caseType" -> search.caseType.get) :: filters
    }

    if(search.status.isDefined){
      filters = Json.obj("profile.status" -> search.status.get) :: filters
    }

    if(search.idCourtCase.isDefined){
      filters = Json.obj("pedigree.idCourtCase" -> search.idCourtCase.get) :: filters
    }
    val query = Json.obj("$and" -> filters)

    val groupId = search.group match {
      case "pedigree" => "$pedigree.idPedigree"
      case "profile" => "$profile.globalCode"
    }

    List(
      Json.obj("$match" -> query),
      Json.obj(
        "$group" -> Json.obj(
          "_id" -> groupId,
          "count" -> Json.obj("$sum" -> 1),
          "lastMatchDate" -> Json.obj("$max" -> "$matchingDate"),
          "assignee" -> Json.obj("$first" -> s"$$${search.group}.assignee")))
    )

  }

  override def countMatches(search: PedigreeMatchCardSearch): Future[Int] = {
    val query = Json.obj(
      "aggregate" -> pedigreeMatches.name,
      "pipeline" -> getMatchesQuery(search)
        .++(
          List(
            Json.obj(
              "$group" -> Json.obj(
                "_id" -> "0",
                "count" -> Json.obj("$sum" -> 1)
              )
            )
          )
        )
    )
    val queryString = query.toString()
    val runner = Command.run(JSONSerializationPack, FailoverStrategy.default)
    val result = runner
      .apply(pedigreeMatches.db, runner.rawCommand(query))
      .one[BSONDocument](ReadPreference.nearest)
      .map {
        bson => {
          val j = Json
            .toJson(bson)
          (j \ "result")
            .as[Seq[Count]]
            .headOption
            .fold(0)(_.count)
        }
      }
    result
  }

  override def getMatches(search: PedigreeMatchCardSearch): Future[Seq[PedigreeMatch]] = {
    val query = Json.obj(
      "aggregate" -> pedigreeMatches.name,
      "pipeline" -> getMatchesQuery(search)
        .++(
          List(
            Json.obj("$skip" -> search.page*search.pageSize),
            Json.obj("$limit" -> search.pageSize)
          )
        )
      )
    val runner = Command
      .run(JSONSerializationPack, FailoverStrategy.default)
    val result = runner
      .apply(
        pedigreeMatches.db,
        runner.rawCommand(query)
      )
      .cursor[BSONDocument](ReadPreference.nearest)
      .collect[List](
        search.pageSize,
        Cursor.FailOnError[List[BSONDocument]]()
      ) map {
        list =>
          list flatMap {
            document =>
              val json = Json.toJson(document)
              (json \ "result").as[Seq[PedigreeMatch]]
          }
      }
    result
  }

  override def getMatchById(matchingId: String): Future[Option[PedigreeMatchResult]] = {
    pedigreeMatches
      .find(
        Json.obj(
          "_id" -> Json.obj(
            "$oid" -> matchingId
          )
        )
      )
      .one[PedigreeMatchResult]
  }

  override def getMatchByPedigree(pedigreeId: Long): Future[Option[PedigreeMatch]] = {
  val listaPedigree = pedigreeMatches
    .find(
      Json.obj("pedigree.idPedigree" -> pedigreeId)
    )
    .sort(
      Json.obj("matchingDate" -> -1)
    )
    .cursor[PedigreeMatchResult]().collect[Seq](
      Int.MaxValue,
      Cursor.FailOnError[Seq[PedigreeMatchResult]]()
    )
    listaPedigree
      .map(
        _.headOption
          .map(
            x=> PedigreeMatch(
              Left(pedigreeId),
              x.matchingDate,
              0,
              x.pedigree.assignee
            )
          )
      )
  }

  override def getMatchByProfile(globalCode: String): Future[Option[PedigreeMatch]] = {
    val listaPedigree = pedigreeMatches
      .find(
        Json.obj("profile.globalCode" -> globalCode)
      )
      .sort(
        Json.obj("matchingDate" -> -1)
      )
      .cursor[PedigreeMatchResult]().collect[Seq](
        Int.MaxValue,
        Cursor.FailOnError[Seq[PedigreeMatchResult]]()
      )
    listaPedigree
      .map(
        _.headOption.map(
          x=> PedigreeMatch(
            Right(globalCode),
            x.matchingDate,
            0,
            x.pedigree.assignee
          )
        )
      )
  }

   def getMejorLrPedigree(pedigreeId: Long): Future[Option[MatchCardMejorLrPed]] = {
    val listaPedigree = pedigreeMatches
      .find(
        Json.obj("pedigree.idPedigree" -> pedigreeId)
      )
      .sort(
        Json.obj("compatibility" -> -1)
      )
      .cursor[PedigreeCompatibilityMatch]().collect[Seq](
        Int.MaxValue,
        Cursor.FailOnError[Seq[PedigreeCompatibilityMatch]]()
      )
      listaPedigree
        .map(
          _.headOption
            .map(
              x=> MatchCardMejorLrPed(
                x.profile.globalCode.text,
                x.profile.globalCode.text,
                Some(x.profile.categoryId.text),
                x.compatibility,
                x.profile.status
              )
            )
        )
  }

  def getMejorLrProf(globalCode: String): Future[Option[MatchCardMejorLrPed]] = {
    val listaPedigree = pedigreeMatches
      .find(
        Json.obj("profile.globalCode" -> globalCode)
      )
      .sort(
        Json.obj("compatibility" -> -1)
      )
      .cursor[PedigreeCompatibilityMatch]().collect[Seq](
        Int.MaxValue,
        Cursor.FailOnError[Seq[PedigreeCompatibilityMatch]]()
      )
    listaPedigree
      .map(
        _.headOption
          .map(
            x=> MatchCardMejorLrPed(
              x.pedigree.idPedigree.toString,
              x.pedigree.idPedigree.toString,
              Some(x.pedigree.caseType),
              x.compatibility,
              x.profile.status
            )
          )
      )
  }

  override def getTypeCourtCasePedigree(pedigreeId: Long): Future[Option[String]] = {
    val lista = pedigreeMatches
      .find(
        Json.obj("pedigree.idPedigree" -> pedigreeId)
      )
      .sort(
        Json.obj("matchingDate" -> -1)
      )
      .cursor[PedigreeMatchResult]().collect[Seq](
        Int.MaxValue,
        Cursor.FailOnError[Seq[PedigreeMatchResult]]()
      )
    lista
      .map(
        _.headOption
          .map(x => x.pedigree.caseType)
      )
  }

  override def allMatchesDiscarded(pedigreeId: Long): Future[Boolean] = {
    val notDeleted = Json
      .obj(
        "$nin" -> List(
          MatchStatus.discarded.toString,
          MatchStatus.deleted.toString
        )
      )
    val query = Json.obj(
      "pedigree.idPedigree" -> pedigreeId,
      "pedigree.status" -> notDeleted,
      "profile.status" -> notDeleted)
    pedigreeMatches
      .count(Some(query))
      .map { n => n == 0 }
  }
  override def hasPendingMatches(pedigreeId: Long): Future[Boolean] = {
    val query = Json.obj(
      "pedigree.idPedigree" -> pedigreeId,
      "pedigree.status" -> MatchStatus.pending.toString,
      "profile.status" -> MatchStatus.pending.toString
    )
    pedigreeMatches
      .count(
        Some(query)
      )
      .map { n => n > 0 }
  }
  override def hasMatches(pedigreeId: Long): Future[Boolean] = {
    val query = Json.obj(
      "pedigree.idPedigree" -> pedigreeId
    )
    pedigreeMatches
      .count(Some(query))
      .map { n => n > 0 }
  }
  override def numberOfPendingMatches(pedigreeId: Long): Future[Int] = {
    val query = Json.obj(
      "pedigree.idPedigree" -> pedigreeId,
      "pedigree.status" -> MatchStatus.pending.toString,
      "profile.status" -> MatchStatus.pending.toString
    )
    pedigreeMatches
      .count(Some(query))
  }
  override def numberOfMatches(pedigreeId: Long): Future[Int] = {
    val query = Json.obj("pedigree.idPedigree" -> pedigreeId)
    pedigreeMatches.count(Some(query))
  }

  override def profileNumberOfPendingMatches(globalCode: String): Future[Int] = {
    val query = Json.obj(
      "profile.globalCode" -> globalCode,
      "pedigree.status" -> MatchStatus.pending.toString,
      "profile.status" -> MatchStatus.pending.toString
    )
    pedigreeMatches
      .count(Some(query))
  }

override def  countProfilesHitPedigrees(globalCodes:String):Future[Int]={
  val query = Json.obj(
    "profile.globalCode" -> globalCodes,
    "pedigree.status" -> MatchStatus.hit.toString,
    "profile.status" -> MatchStatus.hit.toString
  )
  pedigreeMatches
    .count(Some(query))
}

  override def  countProfilesDiscardedPedigrees(globalCodes:String):Future[Int]={
    val query = Json.obj(
      "profile.globalCode" -> globalCodes,
      "pedigree.status" -> MatchStatus.discarded.toString,
      "profile.status" -> MatchStatus.discarded.toString
    )
    pedigreeMatches
      .count(Some(query))
  }

  override def numberOfDiscardedMatches(pedigreeId: Long): Future[Int] = {
    val query = Json.obj(
      "pedigree.idPedigree" -> pedigreeId,
      "pedigree.status" -> MatchStatus.discarded.toString,
      "profile.status" -> MatchStatus.discarded.toString
    )
    pedigreeMatches
      .count(Some(query))
  }

  override def numberOfHitMatches(pedigreeId: Long): Future[Int] = {
    val query = Json.obj(
      "pedigree.idPedigree" -> pedigreeId,
      "pedigree.status" -> MatchStatus.hit.toString,
      "profile.status" -> MatchStatus.hit.toString
    )
    pedigreeMatches
      .count(Some(query))
  }

  override def profileNumberOfPendingMatchesInPedigrees(
    globalCode: String,
    pedigreesIds:Seq[Long]
  ): Future[Int] = {
    val query = Json.obj(
      "profile.globalCode" -> globalCode,
      "pedigree.status" -> MatchStatus.pending.toString,
      "profile.status" -> MatchStatus.pending.toString,
      "pedigree.idPedigree" -> Json.obj("$in" -> pedigreesIds)
    )
    pedigreeMatches
      .count(Some(query))
  }
  private def getMatchesByGroupQuery(search: PedigreeMatchGroupSearch) = {
    val userQuery = if (!search.isSuperUser) {
      Json.obj(
        "$or" -> Json.arr(
          Json.obj(
            "profile.assignee" -> search.user,
            "profile.status" -> Json.obj(
              "$ne" -> MatchStatus.deleted.toString
            )
          ),
          Json.obj(
            "pedigree.assignee" -> search.user,
            "pedigree.status" -> Json.obj("$ne" -> MatchStatus.deleted.toString)
          )
        )
      )
    } else {
      Json.obj("$or" -> Json.arr(
        Json.obj("profile.status" -> Json.obj("$ne" -> MatchStatus.deleted.toString)),
        Json.obj("pedigree.status" -> Json.obj("$ne" -> MatchStatus.deleted.toString))
      ))
    }
    val status = if(search.status.isDefined){
      Json.obj("$or" -> Json.arr(
        Json.obj("profile.status" -> search.status.get),
        Json.obj("pedigree.status" -> search.status.get)
      ))
    }else{
      Json.obj("$or" -> Json.arr(
        Json.obj("profile.status" -> Json.obj("$ne" -> MatchStatus.deleted.toString)),
        Json.obj("pedigree.status" -> Json.obj("$ne" -> MatchStatus.deleted.toString))
      ))
    }

    val idCaso = if(search.idCourCase.isDefined){
      Json.obj("$or" -> Json.arr(
        Json.obj("pedigree.idCourtCase" -> search.idCourCase.get)
      ))
    }else{
      Json.obj("$or" -> Json.arr(
        Json.obj("profile.status" -> Json.obj("$ne" -> MatchStatus.deleted.toString)),
        Json.obj("pedigree.status" -> Json.obj("$ne" -> MatchStatus.deleted.toString))
      ))
    }

    val kindQuery = Json.obj("kind" -> search.kind)
    val groupByQuery = search.groupBy match {
      case "profile" => Json.obj("profile.globalCode" -> search.id)
      case "pedigree" => Json.obj("pedigree.idPedigree" -> search.id.toLong)
    }

    Json.obj(
      "$and" -> Json.arr(
        userQuery,
        kindQuery,
        groupByQuery,
        status,
        idCaso
      )
    )
  }

  override def getMatchesByGroup(search: PedigreeMatchGroupSearch): Future[Seq[PedigreeMatchResult]] = {
    val directionValue = if (search.ascending) 1 else -1
    val sortQuery = search.sortField match {
      case "date" => Json.obj("matchingDate" -> directionValue)
      case "profile" => Json.obj("profile.globalCode" -> directionValue)
      case "category" => Json.obj("profile.categoryId" -> directionValue)
      case "profileg" => Json.obj("profile.globalCode" -> directionValue)
      case "unknown" => Json.obj("pedigree.unknown" -> directionValue)
      case "profileStatus" => Json.obj("profile.status" -> directionValue)
      case "pedigreeStatus" => Json.obj("pedigree.status" -> directionValue)
      case "compatibility" => Json.obj("compatibility" -> directionValue)
    }
    pedigreeMatches
      .find(getMatchesByGroupQuery(search))
      .sort(sortQuery)
      .options(QueryOpts().skip(search.page*search.pageSize))
      .cursor[PedigreeMatchResult]()
      .collect[Seq](search.pageSize, Cursor.FailOnError[Seq[PedigreeMatchResult]]())
  }

  override def getMatchesByGroupPedigree(search: PedigreeMatchGroupSearch): Future[List[MatchCardPed]] = {

    val directionValue = if (search.ascending) 1 else -1
    val sortQuery = search.sortField match {
      case "date" => Json.obj("matchingDate" -> directionValue)
      case "profile" => Json.obj("profile.globalCode" -> directionValue)
      case "unknown" => Json.obj("pedigree.unknown" -> directionValue)
      case "profileStatus" => Json.obj("profile.status" -> directionValue)
      case "pedigreeStatus" => Json.obj("pedigree.status" -> directionValue)
      case "compatibility" => Json.obj("compatibility" -> directionValue)
      case _ => Json.obj("matchingDate" -> directionValue)
    }

   var ped= pedigreeMatches
      .find(getMatchesByGroupQuery(search))
      .sort(sortQuery)
      .options(QueryOpts().skip(search.page*search.pageSize))
      .cursor[PedigreeCompatibilityMatch]()
      .collect[List](search.pageSize, Cursor.FailOnError[List[PedigreeCompatibilityMatch]]())
      if(search.groupBy == "pedigree") {
        ped.map {
          x => x.map {
            pd => MatchCardPed(
              pd.profile.globalCode.text,
              pd.profile.globalCode.text,
              pd.profile.categoryId.text,
              pd.compatibility,
              pd.profile.status,
              pd.pedigree.assignee,
              pd.matchingDate.date,
              pd._id,
              pd.pedigree.unknown,
              pd.mtProfile,
              pd.matchingId
            )
          }
        }
      } else {
        ped.map {
          x => x.map {
            pd => MatchCardPed(
              pd.pedigree.idPedigree.toString,
              pd.pedigree.idCourtCase.toString,
              pd.pedigree.caseType,
              pd.compatibility,
              pd.profile.status,
              pd.pedigree.assignee,
              pd.matchingDate.date,
              pd._id,
              pd.pedigree.unknown,
              pd.mtProfile,
              pd.matchingId,
              pd.pedigree.idPedigree.toString
            )
          }
        }
      }
  }

  override def countMatchesByGroup(search: PedigreeMatchGroupSearch): Future[Int] = {
    pedigreeMatches.count(Some(getMatchesByGroupQuery(search)))
  }

  private def discardMatch(matchId: String, side: String) = {
    val query = Json.obj("_id" -> Json.obj("$oid" -> matchId))
    val modify: JsObject = Json.obj("$set" -> Json.obj(
      s"$side.status" -> MatchStatus.discarded.toString
    ))
    pedigreeMatches.update(query, modify) map {
      case result if result.ok => Right(matchId)
      case error => Left(error.errmsg.getOrElse("Error"))
    }
  }

  override def discardProfile(matchId: String): Future[Either[String, String]] = {
    discardMatch(matchId, "profile")
  }

  override def discardPedigree(matchId: String): Future[Either[String, String]] = {
    discardMatch(matchId, "pedigree")
  }

  override def deleteMatches(idPedigree: Long): Future[Either[String, Long]] = {
    val query = Json.obj("pedigree.idPedigree" -> idPedigree)
    val modify: JsObject = Json.obj(
      "$set" -> Json.obj(
        "profile.status" -> MatchStatus.deleted.toString,
        "pedigree.status" -> MatchStatus.deleted.toString
      )
    )
    pedigreeMatches.update(query, modify, multi = true) map {
      case result if result.ok => Right(idPedigree)
      case error => Left(error.errmsg.getOrElse("Error"))
    }
  }

  private def confirmMatch(matchId: String, side: String) = {
    val query = Json.obj("_id" -> Json.obj("$oid" -> matchId))
    val modify: JsObject = Json.obj("$set" -> Json.obj(s"$side.status" -> MatchStatus.hit.toString))
    pedigreeMatches.update(query, modify) map {
      case result if result.ok => Right(matchId)
      case error => Left(error.errmsg.getOrElse("Error"))
    }
  }

  override def confirmProfile(matchId: String): Future[Either[String, String]] = {
    confirmMatch(matchId, "profile")
  }

  override def confirmPedigree(matchId: String): Future[Either[String, String]] = {
    confirmMatch(matchId, "pedigree")
  }

  override def getAllMatchNonDiscardedByGroup(id: String, group: String): Future[Seq[PedigreeMatchResult]] = {
    val notDeleted = Json.obj(
      "$nin" -> List(
        MatchStatus.discarded.toString,
        MatchStatus.deleted.toString,
        MatchStatus.hit.toString
      )
    )
    group match {
      case "pedigree" => {
        pedigreeMatches.find(Json.obj("pedigree.idPedigree" -> id.toLong,
          "pedigree.status" -> notDeleted,
          "profile.status" -> notDeleted)).sort(
          Json.obj("matchingDate" -> -1)).cursor[PedigreeMatchResult]().collect[Seq](Int.MaxValue,Cursor.FailOnError[Seq[PedigreeMatchResult]]())
      }
      case "profile" => {
        pedigreeMatches.find(Json.obj("profile.globalCode" -> id,
          "pedigree.status" -> notDeleted,
          "profile.status" -> notDeleted)).sort(
          Json.obj("matchingDate" -> -1)).cursor[PedigreeMatchResult]().collect[Seq](Int.MaxValue,Cursor.FailOnError[Seq[PedigreeMatchResult]]())
      }
      case _ => Future.successful(Seq())
    }
  }
}