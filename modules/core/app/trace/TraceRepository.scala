package trace

import jakarta.inject.{Inject, Singleton}
import models.Tables
import play.api.libs.json.Json
import slick.jdbc.PostgresProfile.api._
import types.SampleCode

import scala.concurrent.{ExecutionContext, Future}

trait TraceRepository {
  def add(trace: Trace): Future[Either[String, Long]]
  def search(traceSearch: TraceSearch): Future[Seq[Trace]]
  def count(traceSearch: TraceSearch): Future[Int]
  def searchPedigree(traceSearch: TraceSearchPedigree): Future[Seq[TracePedigree]]
  def countPedigree(traceSearch: TraceSearchPedigree): Future[Int]
  def getById(id: Long): Future[Option[Trace]]
  def addTracePedigree(trace: TracePedigree): Future[Either[String, Long]]
}

@Singleton
class SlickTraceRepository @Inject() (db: slick.jdbc.JdbcBackend.Database)(implicit ec: ExecutionContext)
    extends TraceRepository {

  private val traces        = Tables.Trace
  private val tracesPedigree = Tables.TracePedigree

  private def date2timestamp(date: java.util.Date) = new java.sql.Timestamp(date.getTime)
  private def timestamp2date(ts: java.sql.Timestamp) = new java.util.Date(ts.getTime)

  private def rowToTrace(row: Tables.TraceRow): Trace = {
    val kind = TraceType.withName(row.kind)
    Trace(SampleCode(row.profile), row.user, timestamp2date(row.date), TraceInfo(kind, Json.parse(row.trace)), row.id)
  }

  private def rowToTracePedigree(row: Tables.TracePedigreeRow): TracePedigree = {
    val kind = TraceType.withName(row.kind)
    TracePedigree(row.pedigree, row.user, timestamp2date(row.date), TraceInfo(kind, Json.parse(row.trace)), row.id)
  }

  override def add(trace: Trace): Future[Either[String, Long]] = {
    val row = Tables.TraceRow(0L, trace.profile.text, trace.user,
      date2timestamp(trace.date), Json.toJson(trace.trace).toString, trace.kind.toString)
    db.run((traces returning traces.map(_.id)) += row)
      .map(id => Right(id))
      .recover { case e: Exception => Left(e.getMessage) }
  }

  override def addTracePedigree(trace: TracePedigree): Future[Either[String, Long]] = {
    val row = Tables.TracePedigreeRow(0L, trace.pedigree, trace.user,
      date2timestamp(trace.date), Json.toJson(trace.trace).toString, trace.kind.toString)
    db.run((tracesPedigree returning tracesPedigree.map(_.id)) += row)
      .map(id => Right(id))
      .recover { case e: Exception => Left(e.getMessage) }
  }

  override def search(traceSearch: TraceSearch): Future[Seq[Trace]] = {
    val query = traces
      .filter(t => t.profile === traceSearch.profile.text && (t.user === traceSearch.user || traceSearch.isSuperUser))
      .sortBy(_.date.desc)
      .drop(traceSearch.page * traceSearch.pageSize)
      .take(traceSearch.pageSize)
    db.run(query.result).map(_.map(rowToTrace))
  }

  override def count(traceSearch: TraceSearch): Future[Int] = {
    val query = traces
      .filter(t => t.profile === traceSearch.profile.text && (t.user === traceSearch.user || traceSearch.isSuperUser))
    db.run(query.length.result)
  }

  override def searchPedigree(traceSearch: TraceSearchPedigree): Future[Seq[TracePedigree]] = {
    val query = tracesPedigree
      .filter(t => t.pedigree === traceSearch.pedigreeId.toLong && (t.user === traceSearch.user || traceSearch.isSuperUser))
      .sortBy(_.date.desc)
      .drop(traceSearch.page * traceSearch.pageSize)
      .take(traceSearch.pageSize)
    db.run(query.result).map(_.map(rowToTracePedigree))
  }

  override def countPedigree(traceSearch: TraceSearchPedigree): Future[Int] = {
    val query = tracesPedigree
      .filter(t => t.pedigree === traceSearch.pedigreeId.toLong && (t.user === traceSearch.user || traceSearch.isSuperUser))
    db.run(query.length.result)
  }

  override def getById(id: Long): Future[Option[Trace]] = {
    db.run(traces.filter(_.id === id).result.headOption).map(_.map(rowToTrace))
  }
}
