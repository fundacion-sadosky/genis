package reporting

import matching.MatchStatus
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.{JSONSerializationPack, _}
import reactivemongo.api.commands.Command
import reactivemongo.api.{FailoverStrategy, ReadPreference}
import reactivemongo.bson.BSONDocument

import java.util.Date
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

abstract class ProfileReportMongoRepository {

  def countProfilesCreated(startDate: Option[Date], endDate : Option[Date]) : Future[Int]
  def countProfilesDeleted() : Future[Int]
  def countProfilesDeleted(startDate: Option[Date], endDate : Option[Date]) : Future[Int]
  def countMatches(startDate: Option[Date], endDate : Option[Date]) : Future[Int]
  def countHit(startDate: Option[Date], endDate : Option[Date]) : Future[Int]
  def countDescartes(startDate: Option[Date], endDate : Option[Date]) : Future[Int]

  // Implemento los métodos sin parámetros para que tome todo lo que hay en la colección to ignode dates as in the originals
  def countProfilesCreated(): Future[Int]
  def countMatches(): Future[Int]
  def countHit(): Future[Int]
  def countDescartes(): Future[Int]

}

class MongoProfileReportRepository extends ProfileReportMongoRepository
{
  private def profiles = Await.result(play.modules.reactivemongo.ReactiveMongoPlugin.database.map(_.collection[JSONCollection]("profiles")), Duration(10, SECONDS))
  private def matches = Await.result(play.modules.reactivemongo.ReactiveMongoPlugin.database.map(_.collection[JSONCollection]("matches")), Duration(10, SECONDS))

  override def countProfilesCreated(startDate: Option[Date], endDate: Option[Date]): Future[Int] = {

    val dateFilter = (startDate, endDate) match {
      case (Some(start), Some(end)) =>
        Json.obj("analysisDateMin" -> Json.obj("$gte" -> Json.obj("$date" -> start), "$lte" -> Json.obj("$date" -> end)))
      case _ => Json.obj() // Empty filter, matches all documents
    }

    val projection = Json.obj("$project" -> Json.obj("analysisDateMin" -> Json.obj("$min" -> "$analyses.date")))

    val matching = Json.obj("$match" -> dateFilter)

    val pipelineQuery = List(projection) ++ List(matching)
    val query = Json.obj(
      "aggregate" -> profiles.name,
      "pipeline" -> pipelineQuery
    )

    val runner = Command.run(JSONSerializationPack, FailoverStrategy.default)
    val result = runner.apply(profiles.db, runner.rawCommand(query))
      .one[BSONDocument](ReadPreference.nearest)

    result.map { bson => (Json.toJson(bson) \ "result").as[Seq[JsObject]].size }

  }

  override def countProfilesCreated(): Future[Int] = {
    this.countProfilesCreated(Option.empty[Date], Option.empty[Date])
  }

  override def countProfilesDeleted(): Future[Int] = {
    profiles.count(Some(Json.obj("deleted" -> true)))
  }

  override def countProfilesDeleted(startDate: Option[Date], endDate: Option[Date]): Future[Int] = {
    val dateFilter = (startDate, endDate) match {
      case (Some(start), Some(end)) =>
        Json.obj(
          "deletionDate" -> Json.obj(
            "$gte" -> Json.obj("$date" -> start),
            "$lte" -> Json.obj("$date" -> end)
          ),
          "deleted" -> true // Ensure we're only counting deleted profiles
        )
      case _ => Json.obj("deleted" -> true) // If no dates are provided, get all deleted profiles
    }

    profiles.count(Some(dateFilter))
  }

  override def countMatches(startDate: Option[Date], endDate: Option[Date]): Future[Int] = {
    val dateFilter = (startDate, endDate) match {
      case (Some(start), Some(end)) =>
        Json.obj("matchingDate" -> Json.obj("$gte" -> Json.obj("$date" -> start), "$lte" -> Json.obj("$date" -> end)))
      case _ => Json.obj() // Empty filter, matches all documents
    }

    matches count Some(dateFilter)
  }

  override def countMatches(): Future[Int] = {
    this.countMatches(Option.empty[Date], Option.empty[Date])
  }

  override def countHit(startDate: Option[Date], endDate: Option[Date]): Future[Int] = {
    val dateFilter = (startDate, endDate) match {
      case (Some(start), Some(end)) =>
        Json.obj("matchingDate" -> Json.obj("$gte" -> Json.obj("$date" -> start), "$lte" -> Json.obj("$date" -> end)))
      case _ => Json.obj() // Empty filter, matches all documents
    }

    val hit = Json.obj("$and" -> Seq(Json.obj("leftProfile.status" -> MatchStatus.hit),
      Json.obj("rightProfile.status" -> MatchStatus.hit)))

    val query = if (dateFilter == Json.obj()) {
      Json.obj("$and" -> Seq(hit))
    } else {
      Json.obj("$and" -> Seq(dateFilter, hit))
    }

    matches.count(Some(query))
  }

  override def countHit(): Future[Int] = {
    this.countHit(Option.empty[Date], Option.empty[Date])
  }

  override def countDescartes(startDate: Option[Date], endDate: Option[Date]): Future[Int] = {
    val dateFilter = (startDate, endDate) match {
      case (Some(start), Some(end)) =>
        Json.obj("matchingDate" -> Json.obj("$gte" -> Json.obj("$date" -> start), "$lte" -> Json.obj("$date" -> end)))
      case _ => Json.obj() // Empty filter, matches all documents
    }

    val descartes = Json.obj("$and" -> Seq(Json.obj("leftProfile.status" -> MatchStatus.discarded),
      Json.obj("rightProfile.status" -> MatchStatus.discarded)))

    val query = if (dateFilter == Json.obj()) {
      Json.obj("$and" -> Seq(descartes))
    } else {
      Json.obj("$and" -> Seq(dateFilter, descartes))
    }

    matches.count(Some(query))

  }

  override def countDescartes(): Future[Int] = {
    this.countDescartes(Option.empty[Date], Option.empty[Date])
  }
}
