package kits

import javax.inject.Singleton
import profile.Profile
import scala.concurrent.Future

trait LocusService {
  def list(): Future[Seq[Locus]]
  def getLocusByAnalysisTypeName(analysisType: String): Future[Seq[String]]
  def getLocusByAnalysisType(analysisType: Int): Future[Seq[String]]
  def saveLocusAllelesFromProfile(profile: Profile): Future[Either[String, Int]]
  def refreshAllKis(): Future[Unit]
}

@Singleton
class LocusServiceStub extends LocusService {
  override def list(): Future[Seq[Locus]] = Future.successful(Seq.empty)
  override def getLocusByAnalysisTypeName(analysisType: String): Future[Seq[String]] = Future.successful(Seq.empty)
  override def getLocusByAnalysisType(analysisType: Int): Future[Seq[String]] = Future.successful(Seq.empty)
  override def saveLocusAllelesFromProfile(profile: Profile): Future[Either[String, Int]] = Future.successful(Right(0))
  override def refreshAllKis(): Future[Unit] = Future.successful(())
}
