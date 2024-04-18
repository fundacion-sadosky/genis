package reporting.profileReports

import play.api.libs.json.{Json, _}
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import play.api.Play.current
import java.util.Date

import play.modules.reactivemongo.json.JSONSerializationPack
import reactivemongo.api.{FailoverStrategy, ReadPreference}
import reactivemongo.bson.{BSONArray, BSONDocument}
import reactivemongo.api.commands.Command
import types.Count
import java.text.ParseException
import javax.inject.{Inject, Named}

import com.mongodb.client.model.Aggregates._
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections._
import com.mongodb.spark.MongoSpark
import com.mongodb.spark.config.ReadConfig
import matching.MatchStatus
import org.apache.spark.rdd.RDD
import org.bson.Document
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.modules.reactivemongo.json._
import reactivemongo.api.{Cursor, FailoverStrategy, ReadPreference}
import reactivemongo.core.commands.RawCommand


abstract class ProfileReportRepository {

  def countProfilesCreated(startDate: Date, endDate : Date) : Future[Int]
  def countProfilesDeleted() : Future[Int]
  def countMatches(startDate: Date, endDate : Date) : Future[Int]
  def countHit(startDate: Date, endDate : Date) : Future[Int]
  def countDescartes(startDate: Date, endDate : Date) : Future[Int]

}

class MongoProfileReportRepository extends ProfileReportRepository
{
  private def profiles = Await.result(play.modules.reactivemongo.ReactiveMongoPlugin.database.map(_.collection[JSONCollection]("profiles")), Duration(10, SECONDS))
  private def matches = Await.result(play.modules.reactivemongo.ReactiveMongoPlugin.database.map(_.collection[JSONCollection]("matches")), Duration(10, SECONDS))

  override def countProfilesCreated(startDate: Date, endDate: Date): Future[Int] = {

    val projection = Json.obj("$project" -> Json.obj("analysisDateMin" -> Json.obj("$min" -> "$analyses.date")))

    val matching = Json.obj("$match" -> Json.obj("analysisDateMin" -> Json.obj("$gte" -> Json.obj("$date" -> startDate),
      "$lte" -> Json.obj("$date" -> endDate))))

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

  override def countProfilesDeleted(): Future[Int] = {
    profiles.count(Some(Json.obj("deleted" -> true)))
  }

  override def countMatches(startDate: Date, endDate: Date): Future[Int] = {
    matches count Some(Json.obj("matchingDate" ->
        Json.obj("$gte" -> Json.obj("$date" -> startDate), "$lte" -> Json.obj("$date" -> endDate))))
  }

  override def countHit(startDate: Date, endDate: Date): Future[Int] = {
    val filterDate = Json.obj("matchingDate" ->
      Json.obj("$gte" -> Json.obj("$date" -> startDate), "$lte" -> Json.obj("$date" -> endDate)))

    val hit = Json.obj("$and" -> Seq(Json.obj("leftProfile.status" -> MatchStatus.hit),
      Json.obj("rightProfile.status" -> MatchStatus.hit)))

    val query = Json.obj("$and" -> Seq(filterDate, hit))

    matches.count(Some(query))
  }

  override def countDescartes(startDate: Date, endDate: Date): Future[Int] = {
    val filterDate = Json.obj("matchingDate" ->
      Json.obj("$gte" -> Json.obj("$date" -> startDate), "$lte" -> Json.obj("$date" -> endDate)))

    val descartes = Json.obj("$and" -> Seq(Json.obj("leftProfile.status" -> MatchStatus.discarded),
      Json.obj("rightProfile.status" -> MatchStatus.discarded)))

    val query = Json.obj("$and" -> Seq(filterDate, descartes))

    matches.count(Some(query))

  }
}
