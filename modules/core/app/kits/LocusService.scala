package kits

import javax.inject.Singleton
import matching.{AleleRange, NewMatchingResult}
import profile.Profile
import scala.concurrent.Future
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.Await

trait LocusService:
  def list(): Future[Seq[Locus]]
  def getLocusByAnalysisTypeName(analysisType: String): Future[Seq[String]]
  def getLocusByAnalysisType(analysisType: Int): Future[Seq[String]]
  def saveLocusAllelesFromProfile(profile: Profile): Future[Either[String, Int]]
  def refreshAllKis(): Future[Unit]
  def locusRangeMap(): NewMatchingResult.AlleleMatchRange

@Singleton
class LocusServiceStub extends LocusService:
  override def list(): Future[Seq[Locus]] = Future.successful(Seq.empty)
  override def getLocusByAnalysisTypeName(analysisType: String): Future[Seq[String]] = Future.successful(Seq.empty)
  override def getLocusByAnalysisType(analysisType: Int): Future[Seq[String]] = Future.successful(Seq.empty)
  override def saveLocusAllelesFromProfile(profile: Profile): Future[Either[String, Int]] = Future.successful(Right(0))
  override def refreshAllKis(): Future[Unit] = Future.successful(())
  override def locusRangeMap(): NewMatchingResult.AlleleMatchRange =
    Await.result(list(), Duration(100, SECONDS))
      .map(l => l.id -> AleleRange(l.minAlleleValue.getOrElse(0), l.maxAlleleValue.getOrElse(99)))
      .toMap
