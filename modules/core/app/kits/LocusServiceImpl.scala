package kits

import com.google.inject.Provider
import javax.inject.{Inject, Singleton}
import matching.AleleRange
import pedigree.MutationService
import profile.{Allele, Profile}
import services.{CacheService, LocusCacheKey}

import scala.concurrent.{ExecutionContext, Future}

trait LocusService:
  def add(locus: FullLocus): Future[Either[String, String]]
  def update(locus: FullLocus): Future[Either[String, Unit]]
  def listFull(): Future[Seq[FullLocus]]
  def list(): Future[Seq[Locus]]
  def delete(id: String): Future[Either[String, String]]
  def getLocusByAnalysisTypeName(analysisType: String): Future[Seq[String]]
  def getLocusByAnalysisType(analysisType: Int): Future[Seq[String]]
  def locusRangeMap(): Future[Map[String, AleleRange]]
  def saveLocusAlleles(list: List[(String, Double)]): Future[Either[String, Int]]
  def saveLocusAllelesFromProfile(p: Profile): Future[Either[String, Int]]
  def refreshAllKis(): Future[Unit]

@Singleton
class LocusServiceImpl @Inject()(
  cache: CacheService,
  locusRepository: LocusRepository,
  mutationServiceProvider: Provider[MutationService]
)(implicit ec: ExecutionContext) extends LocusService:

  private def mutationService: MutationService = mutationServiceProvider.get()

  private def cleanCache(): Unit =
    cache.pop(LocusCacheKey)

  override def add(full: FullLocus): Future[Either[String, String]] =
    locusRepository.add(full).map { result =>
      result.foreach { _ =>
        cleanCache()
        if full.locus.analysisType == 1 then
          mutationService.addLocus(full)
      }
      result
    }

  override def update(locus: FullLocus): Future[Either[String, Unit]] =
    locusRepository.update(locus).map { result =>
      result.foreach(_ => cleanCache())
      result
    }

  override def listFull(): Future[Seq[FullLocus]] =
    cache.asyncGetOrElse(LocusCacheKey)(locusRepository.listFull())

  override def list(): Future[Seq[Locus]] =
    listFull().map(_.map(_.locus))

  override def delete(id: String): Future[Either[String, String]] =
    locusRepository.delete(id).map { result =>
      result.foreach(_ => cleanCache())
      result
    }

  override def getLocusByAnalysisTypeName(analysisType: String): Future[Seq[String]] =
    locusRepository.getLocusByAnalysisTypeName(analysisType).map(_.map(_.id))

  override def getLocusByAnalysisType(analysisType: Int): Future[Seq[String]] =
    locusRepository.getLocusByAnalysisType(analysisType).map(_.map(_.id))

  override def locusRangeMap(): Future[Map[String, AleleRange]] =
    list().map { loci =>
      loci.map(l => l.id -> AleleRange(l.minAlleleValue.getOrElse(0), l.maxAlleleValue.getOrElse(99))).toMap
    }

  override def saveLocusAlleles(list: List[(String, Double)]): Future[Either[String, Int]] =
    mutationService.saveLocusAlleles(list)

  override def saveLocusAllelesFromProfile(profile: Profile): Future[Either[String, Int]] =
    val locusAlleles = profile.genotypification.get(1)
      .map(_.toList.flatMap { case (marker, alleles) =>
        alleles.collect { case Allele(z) => (marker.toString, z.toDouble) }
      })
      .getOrElse(List.empty)
      .filter(_._1.nonEmpty)
    saveLocusAlleles(locusAlleles)

  override def refreshAllKis(): Future[Unit] =
    mutationService.refreshAllKisSecuential()
