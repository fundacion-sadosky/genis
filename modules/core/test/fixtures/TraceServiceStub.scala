package trace

import jakarta.inject.Singleton

import scala.concurrent.Future

/** No-op TraceService for tests that exercise other controllers (Profiles, Pedigrees)
 *  and don't care about audit/trace side-effects. Implements the full TraceService
 *  contract migrated in #214. */
@Singleton
class TraceServiceStub extends TraceService {
  override def add(trace: Trace): Future[Either[String, Long]] = Future.successful(Right(0L))
  override def search(traceSearch: TraceSearch): Future[Seq[Trace]] = Future.successful(Seq.empty)
  override def count(traceSearch: TraceSearch): Future[Int] = Future.successful(0)
  override def searchPedigree(traceSearch: TraceSearchPedigree): Future[Seq[TracePedigree]] = Future.successful(Seq.empty)
  override def countPedigree(traceSearch: TraceSearchPedigree): Future[Int] = Future.successful(0)
  override def addTracePedigree(trace: TracePedigree): Future[Either[String, Long]] = Future.successful(Right(0L))
  override def getFullDescription(id: Long): Future[String] = Future.successful("")
}
