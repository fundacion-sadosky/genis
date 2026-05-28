package kits

import scala.concurrent.Future

trait LocusRepository:
  def add(fullLocus: FullLocus): Future[Either[String, String]]
  def update(fullLocus: FullLocus): Future[Either[String, Unit]]
  def delete(id: String): Future[Either[String, String]]
  def listFull(): Future[Seq[FullLocus]]
  def getLocusByAnalysisTypeName(analysisType: String): Future[Seq[Locus]]
  def getLocusByAnalysisType(analysisType: Int): Future[Seq[Locus]]
