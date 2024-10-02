package trace

import javax.inject.{Inject, Singleton}

import models.Tables
import play.api.Application
import play.api.db.slick.Config.driver.simple.{Column, Compiled, TableQuery, booleanColumnExtensionMethods, booleanColumnType, columnExtensionMethods, queryToInsertInvoker, runnableCompiledToAppliedQueryInvoker, slickDriver, stringColumnType}
import play.api.db.slick._
import play.api.libs.json.Json
import types.SampleCode
import util.DefaultDb

import scala.concurrent.Future
import scala.language.postfixOps
import slick.driver.PostgresDriver.simple._


abstract class TraceRepository {
  def add(trace: Trace): Future[Either[String, Long]]
  def search(traceSearch: TraceSearch): Future[IndexedSeq[Trace]]
  def count(traceSearch: TraceSearch): Future[Int]
  def searchPedigree(traceSearch: TraceSearchPedigree): Future[IndexedSeq[TracePedigree]]
  def countPedigree(traceSearch: TraceSearchPedigree): Future[Int]
  def getById(id: Long): Future[Option[Trace]]
  def addTracePedigree(trace: TracePedigree): Future[Either[String, Long]]

}

@Singleton
class SlickTraceRepository @Inject() (implicit app: Application) extends TraceRepository with DefaultDb {

  val traces: TableQuery[Tables.Trace] = Tables.Trace
  val tracesPedigree: TableQuery[Tables.TracePedigree] = Tables.TracePedigree

  implicit private def date2timestamp(date: java.util.Date) = new java.sql.Timestamp(date.getTime)

  implicit private def timestamp2date(timestamp: java.sql.Timestamp) = new java.util.Date(timestamp.getTime)

  val queryGetByProfileAndUser = Compiled {
    (profile: Column[String], userId: Column[String], isSuperUser: Column[Boolean]) =>
    {for{
      t <- traces if t.profile === profile && (t.user === userId || isSuperUser)
    } yield t}.sortBy(_.date.desc)
  }
  val queryGetByPedigreeAndUser = Compiled {
    (pedigree: Column[Long], userId: Column[String], isSuperUser: Column[Boolean]) =>
      {for{
        t <- tracesPedigree if t.pedigree === pedigree && (t.user === userId || isSuperUser)
      } yield t}.sortBy(_.date.desc)
  }
  val queryGetById = Compiled { id: Column[Long] =>
    for {
      t <- traces if t.id === id
    } yield t
  }

  override def addTracePedigree(trace: TracePedigree): Future[Either[String, Long]] = Future {
    DB.withTransaction { implicit session =>
      try {
        val traceRow = Tables.TracePedigreeRow(0, trace.pedigree, trace.user,
          trace.date, Json.toJson(trace.trace).toString, trace.kind.toString)
        Right(tracesPedigree returning tracesPedigree.map(_.id) += traceRow)
      } catch {
        case e: Exception => {
          e.printStackTrace()
          Left(e.getMessage)
        }
      }
    }
  }
  override def add(trace: Trace): Future[Either[String, Long]] = Future {
    DB.withTransaction { implicit session =>
      try {
        val traceRow = Tables.TraceRow(0, trace.profile.text, trace.user,
          trace.date, Json.toJson(trace.trace).toString, trace.kind.toString)
        Right(traces returning traces.map(_.id) += traceRow)
      } catch {
        case e: Exception => {
          e.printStackTrace()
          Left(e.getMessage)
        }
      }
    }
  }
  override def count(traceSearch: TraceSearch) = Future {
    DB.withSession { implicit session =>
      queryGetByProfileAndUser((traceSearch.profile.text, traceSearch.user, traceSearch.isSuperUser)).list.length
    }
  }

  override def search(traceSearch: TraceSearch): Future[IndexedSeq[Trace]] = Future {
    DB.withSession { implicit session =>

      val searchQuery = queryGetByProfileAndUser((traceSearch.profile.text, traceSearch.user, traceSearch.isSuperUser)).list

      searchQuery
        .drop(traceSearch.page * traceSearch.pageSize)
        .take(traceSearch.pageSize).iterator.toVector map {
        trace =>
          val kind = TraceType.withName(trace.kind.toString)
          Trace(SampleCode(trace.profile), trace.user, trace.date, TraceInfo(kind, Json.parse(trace.trace)), trace.id)
      }
    }
  }
  override def countPedigree(traceSearch: TraceSearchPedigree) = Future {
    DB.withSession { implicit session =>
      queryGetByPedigreeAndUser((traceSearch.pedigreeId, traceSearch.user, traceSearch.isSuperUser)).list.length
    }
  }

  override def searchPedigree(traceSearch: TraceSearchPedigree): Future[IndexedSeq[TracePedigree]] = Future {
    DB.withSession { implicit session =>

      val searchQuery = queryGetByPedigreeAndUser((traceSearch.pedigreeId, traceSearch.user, traceSearch.isSuperUser)).list

      searchQuery
        .drop(traceSearch.page * traceSearch.pageSize)
        .take(traceSearch.pageSize).iterator.toVector map {
        trace =>
          val kind = TraceType.withName(trace.kind.toString)
          TracePedigree(trace.pedigree,trace.user, trace.date, TraceInfo(kind, Json.parse(trace.trace)), trace.id)
      }
    }
  }
  override def getById(id: Long): Future[Option[Trace]] = Future {
    DB.withSession { implicit session =>
      queryGetById(id).firstOption map { trace =>
        val kind = TraceType.withName(trace.kind.toString)
        Trace(SampleCode(trace.profile), trace.user, trace.date, TraceInfo(kind, Json.parse(trace.trace)), trace.id)
      }
    }

  }

}