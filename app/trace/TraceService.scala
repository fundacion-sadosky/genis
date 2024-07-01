package trace

import javax.inject.{Inject, Singleton}

import configdata.{CategoryAssociation, CategoryConfiguration, CategoryService, MatchingRule}
import kits.{AnalysisType, AnalysisTypeService, LocusRepository, StrKitRepository}
import matching.Algorithm.Algorithm
import matching.{Algorithm, Stringency}
import pedigree.PedigreeDataRepository
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import profile.ProfileRepository

import play.api.i18n.Messages

import scala.concurrent.Future

abstract class TraceService {
  def add(trace: Trace): Future[Either[String, Long]]
  def search(traceSearch: TraceSearch): Future[Seq[Trace]]
  def count(traceSearch: TraceSearch): Future[Int]
  def searchPedigree(traceSearch: TraceSearchPedigree): Future[Seq[TracePedigree]]
  def countPedigree(traceSearch: TraceSearchPedigree): Future[Int]
  def addTracePedigree(trace: TracePedigree): Future[Either[String, Long]]
  def getFullDescription(id: Long): Future[String]
}

@Singleton
class TraceServiceImpl @Inject() (
  traceRepository: TraceRepository,
  categoryService: CategoryService,
  analysisTypeService: AnalysisTypeService,
  profileRepository: ProfileRepository,
  pedigreeDataRepository: PedigreeDataRepository
  ) extends TraceService {

  val logger = Logger(this.getClass)

  override def addTracePedigree(trace: TracePedigree): Future[Either[String, Long]] = {
    traceRepository.addTracePedigree(trace).recover {
      case e: Throwable => {
        val error = Messages("error.E0131",trace.kind.toString, trace.pedigree)
        logger.error(error, e)
        Left(error)
      }
    }
  }
  override def add(trace: Trace): Future[Either[String, Long]] = {
    traceRepository.add(trace).recover {
      case e: Throwable => {
        val error = Messages("error.E0121",trace.kind.toString, trace.profile.text)
        logger.error(error, e)
        Left(error)
      }
    }
  }
  override def count(traceSearch: TraceSearch): Future[Int] = {
    traceRepository.count(traceSearch)
  }

  override def search(traceSearch: TraceSearch): Future[Seq[Trace]] = {
    traceRepository.search(traceSearch)
  }
  override def countPedigree(traceSearch: TraceSearchPedigree): Future[Int] = {
    traceRepository.countPedigree(traceSearch)
  }

  override def searchPedigree(traceSearch: TraceSearchPedigree): Future[Seq[TracePedigree]] = {
    traceRepository.searchPedigree(traceSearch)
  }

  override def getFullDescription(id: Long): Future[String] = {
    traceRepository.getById(id) flatMap { traceOpt =>
      val trace = traceOpt.get
      trace.trace match {
        case ti: AnalysisInfo => stringify(ti)
        case ti: ProcessInfo => stringify(ti)
        case ti: MatchActionInfo => stringify(ti)
        case ti: PedigreeMatchActionInfo => stringify(ti)
        case ti: AssociationInfo => stringify(ti)
        case ti: PedigreeStatusChangeInfo => stringify(ti)
        case ti: PedigreeCopyInfo => stringify(ti)
        case ti: PedigreeEditInfo => stringify(ti)
        case ti: PedigreeNewScenarioInfo => stringify(ti)
        case _ => Future.successful("")
      }
    }
  }

  private def stringify(ti: AssociationInfo) = {

    def stringifyCategoryAssociation(ca: CategoryAssociation): String = {
      val categoryOpt = categoryService.getCategory(ca.categoryRelated)
      val category = if (categoryOpt.isDefined) categoryOpt.get.name else ca.categoryRelated.toString
      s"Category: $category / Mismatches tolerated: ${ca.mismatches}"
    }

    profileRepository.findByCode(ti.profile) map { profileOpt =>
      val profile = profileOpt.get
      val associations = ti.categoryAssociations.map(stringifyCategoryAssociation(_)).mkString("\n")
      s"Profile: ${profile.globalCode.text} (${profile.internalSampleCode})\nAssociation rules:\n$associations"
    }
  }

  private def stringify(ti: AnalysisInfo) = {

    def stringifyCategoryConfiguration(cc: CategoryConfiguration): String = {
      s"Minimum number of markers with alleles: ${cc.minLocusPerProfile} / " +
        s"Maximum number of markers with trisomies: ${cc.maxOverageDeviatedLoci} / " +
        s"Maximum number of alleles per marker: ${cc.maxAllelesPerLocus}"
    }

    val future = ti.analysisType.fold[Future[Option[AnalysisType]]](Future.successful(None))({ at => analysisTypeService.getById(at)})

    future map { analysisTypeOpt =>
      val suffix = analysisTypeOpt.fold("")({at => s"Type: ${at.name}\n"})
      s"${suffix}Validation rules:\n${stringifyCategoryConfiguration(ti.categoryConfiguration)}"
    }
  }

  private def stringify(ti: MatchActionInfo) = {
    for {
      profileOpt <- profileRepository.findByCode(ti.profile)
      analysisTypeOpt <- analysisTypeService.getById(ti.analysisType)
    } yield {
      val profile = profileOpt.get
      val categoryOpt = categoryService.getCategory(profile.categoryId)
      val category = if (categoryOpt.isDefined) categoryOpt.get.name else profile.categoryId.toString
      s"Type: ${analysisTypeOpt.get.name} / " +
        s"Matching profile: ${profile.globalCode.text} (${profile.internalSampleCode}) / Category: $category / " +
        s"Responsible user: ${profile.assignee}"
    }

  }

  private def stringify(ti: PedigreeEditInfo) = {
    Future.successful("PedigreeEdit")
  }
  private def stringify(ti: PedigreeCopyInfo) = {
    Future.successful("PedigreeCopy")
  }
  private def stringify(ti: PedigreeNewScenarioInfo) = {
    Future.successful("PedigreeNewScenario")
  }
  private def stringify(ti: PedigreeStatusChangeInfo) = {
    Future.successful("PedigreeStatusChange")
  }
  private def stringify(ti: PedigreeMatchActionInfo) = {
    for {
      pedigreeOpt <- pedigreeDataRepository.getPedigreeMetaData(ti.pedigree)
      analysisTypeOpt <- analysisTypeService.getById(ti.analysisType)
    } yield {
      val pedigree = pedigreeOpt.get
      s"Type: ${analysisTypeOpt.get.name} / " +
        s"Matching pedigree: ${pedigree.pedigreeMetaData.id} (${pedigree.pedigreeMetaData.name}) / " +
        s"Responsible user: ${pedigree.pedigreeMetaData.assignee}"
    }

  }

  private def stringify(ti: ProcessInfo) = {

    def stringifyMatchingRule(mr: MatchingRule): Future[String] = {
      val categoryOpt = categoryService.getCategory(mr.categoryRelated)

      analysisTypeService.getById(mr.`type`) map { analysisTypeOpt =>
        val analysisType = analysisTypeOpt.get
        val category = if (categoryOpt.isDefined) categoryOpt.get.name else mr.categoryRelated.toString
        s"Category: $category / Type: ${analysisType.name} / Stringency: ${stringifyStringency(mr.minimumStringency, mr.matchingAlgorithm)}" +
          s" / Minimum matches: ${mr.minLocusMatch} / Maximum mismatches: ${mr.mismatchsAllowed}"
      }
    }

    def stringifyStringency(stringency: Stringency.Value, algorithm: Algorithm.Value): String = {
      algorithm match {
        case Algorithm.GENIS_MM => "Mixture Mixture"
        case Algorithm.ENFSI => stringency match {
          case Stringency.LowStringency => "Low"
          case Stringency.ModerateStringency => "Moderate"
          case Stringency.HighStringency => "High"
        }
      }
    }

    Future.sequence(ti.matchingRules.map(stringifyMatchingRule(_))) map { seq => "Matching rules:\n" + seq.mkString("\n") }

  }

}
