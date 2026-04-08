package trace

import jakarta.inject.{Inject, Singleton}
import configdata.{CategoryAssociation, CategoryConfiguration, CategoryService, MatchingRule}
import kits.{AnalysisType, AnalysisTypeService}
import matching.{Algorithm, Stringency}
import pedigree.PedigreeDataRepository
import play.api.Logger
import play.api.i18n.{Lang, MessagesApi}
import profile.ProfileRepository

import scala.concurrent.{ExecutionContext, Future}

trait TraceService {
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
  pedigreeDataRepository: PedigreeDataRepository,
  messagesApi: MessagesApi
)(implicit ec: ExecutionContext) extends TraceService {

  private val logger = Logger(this.getClass)
  private implicit val lang: Lang = Lang.defaultLang

  override def addTracePedigree(trace: TracePedigree): Future[Either[String, Long]] =
    traceRepository.addTracePedigree(trace).recover {
      case e: Throwable =>
        val error = messagesApi("error.E0131", trace.kind.toString, trace.pedigree.toString)
        logger.error(error, e)
        Left(error)
    }

  override def add(trace: Trace): Future[Either[String, Long]] =
    traceRepository.add(trace).recover {
      case e: Throwable =>
        val error = messagesApi("error.E0121", trace.kind.toString, trace.profile.text)
        logger.error(error, e)
        Left(error)
    }

  override def count(traceSearch: TraceSearch): Future[Int] =
    traceRepository.count(traceSearch)

  override def search(traceSearch: TraceSearch): Future[Seq[Trace]] =
    traceRepository.search(traceSearch)

  override def countPedigree(traceSearch: TraceSearchPedigree): Future[Int] =
    traceRepository.countPedigree(traceSearch)

  override def searchPedigree(traceSearch: TraceSearchPedigree): Future[Seq[TracePedigree]] =
    traceRepository.searchPedigree(traceSearch)

  override def getFullDescription(id: Long): Future[String] =
    traceRepository.getById(id) flatMap { traceOpt =>
      val trace = traceOpt.get
      trace.trace match {
        case ti: AnalysisInfo            => stringify(ti)
        case ti: ProcessInfo             => stringify(ti)
        case ti: MatchActionInfo         => stringify(ti)
        case ti: PedigreeMatchActionInfo => stringify(ti)
        case ti: AssociationInfo         => stringify(ti)
        case ti: PedigreeStatusChangeInfo => stringify(ti)
        case ti: PedigreeCopyInfo        => stringify(ti)
        case ti: PedigreeEditInfo        => stringify(ti)
        case ti: PedigreeNewScenarioInfo => stringify(ti)
        case _                           => Future.successful("")
      }
    }

  private def stringify(ti: AssociationInfo): Future[String] = {
    def stringifyCategoryAssociation(ca: CategoryAssociation): Future[String] =
      categoryService.getCategory(ca.categoryRelated).map { categoryOpt =>
        val category = categoryOpt.fold(ca.categoryRelated.toString)(_.name)
        s"Categoría: $category / No coincidencias toleradas: ${ca.mismatches}"
      }

    for {
      profileOpt   <- profileRepository.findByCode(ti.profile)
      profile       = profileOpt.get
      assocStrings <- Future.sequence(ti.categoryAssociations.map(stringifyCategoryAssociation))
    } yield {
      s"Perfil: ${profile.globalCode.text} (${profile.internalSampleCode})\nReglas de asociación:\n${assocStrings.mkString("\n")}"
    }
  }

  private def stringify(ti: AnalysisInfo): Future[String] = {
    def stringifyCategoryConfiguration(cc: CategoryConfiguration): String =
      s"Cantidad mínima de marcadores con alelos: ${cc.minLocusPerProfile} / " +
        s"Cantidad máxima de marcadores con trisomías: ${cc.maxOverageDeviatedLoci} / " +
        s"Cantidad máxima de alelos por marcador: ${cc.maxAllelesPerLocus} / " +
        s"Es categoría multialélica?: ${if (cc.multiallelic) "Sí" else "No"} "

    val future = ti.analysisType.fold[Future[Option[AnalysisType]]](Future.successful(None))(at => analysisTypeService.getById(at))
    future.map { analysisTypeOpt =>
      val suffix = analysisTypeOpt.fold("")(at => s"Tipo: ${at.name}\n")
      s"${suffix}Reglas de validación:\n${stringifyCategoryConfiguration(ti.categoryConfiguration)}"
    }
  }

  private def stringify(ti: MatchActionInfo): Future[String] =
    for {
      profileOpt      <- profileRepository.findByCode(ti.profile)
      analysisTypeOpt <- analysisTypeService.getById(ti.analysisType)
      profile          = profileOpt.get
      categoryOpt     <- categoryService.getCategory(profile.categoryId)
    } yield {
      val category = categoryOpt.fold(profile.categoryId.toString)(_.name)
      s"Tipo: ${analysisTypeOpt.get.name} / " +
        s"Perfil coincidente: ${profile.globalCode.text} (${profile.internalSampleCode}) / Categoría: $category / " +
        s"Usuario responsable: ${profile.assignee}"
    }

  private def stringify(ti: PedigreeEditInfo): Future[String] =
    Future.successful("PedigreeEdit")

  private def stringify(ti: PedigreeCopyInfo): Future[String] =
    Future.successful("PedigreeCopy")

  private def stringify(ti: PedigreeNewScenarioInfo): Future[String] =
    Future.successful("PedigreeNewScenario")

  private def stringify(ti: PedigreeStatusChangeInfo): Future[String] =
    Future.successful("PedigreeStatusChange")

  private def stringify(ti: PedigreeMatchActionInfo): Future[String] =
    for {
      pedigreeOpt     <- pedigreeDataRepository.getPedigreeMetaData(ti.pedigree)
      analysisTypeOpt <- analysisTypeService.getById(ti.analysisType)
    } yield {
      // pedigreeOpt es None cuando PedigreeDataRepository está en stub (TODO: migrate pedigree).
      // Devuelve descripción parcial en lugar de lanzar NoSuchElementException.
      val pedigreeDesc = pedigreeOpt.fold(s"id=${ti.pedigree}") { p =>
        s"${p.pedigreeMetaData.id} (${p.pedigreeMetaData.name})"
      }
      val assigneeDesc = pedigreeOpt.fold("N/A")(_.pedigreeMetaData.assignee)
      s"Tipo: ${analysisTypeOpt.get.name} / " +
        s"Pedigrí coincidente: $pedigreeDesc / " +
        s"Usuario responsable: $assigneeDesc"
    }

  private def stringify(ti: ProcessInfo): Future[String] = {
    def stringifyMatchingRule(mr: MatchingRule): Future[String] =
      for {
        categoryOpt     <- categoryService.getCategory(mr.categoryRelated)
        analysisTypeOpt <- analysisTypeService.getById(mr.`type`)
      } yield {
        val analysisType = analysisTypeOpt.get
        val category = categoryOpt.fold(mr.categoryRelated.toString)(_.name)
        s"Categoría: $category / Tipo: ${analysisType.name} / Exigencia: ${stringifyStringency(mr.minimumStringency, mr.matchingAlgorithm)}" +
          s" / Cant. mínima de coincidencias: ${mr.minLocusMatch} / Cant. máxima de no coincidencias: ${mr.mismatchsAllowed}"
      }

    def stringifyStringency(stringency: Stringency.Value, algorithm: Algorithm.Value): String =
      algorithm match {
        case Algorithm.GENIS_MM => "Mezcla Mezcla"
        case Algorithm.ENFSI => stringency match {
          case Stringency.LowStringency      => "Baja"
          case Stringency.ModerateStringency => "Media"
          case Stringency.HighStringency     => "Alta"
          case _                             => stringency.toString
        }
      }

    Future.sequence(ti.matchingRules.map(stringifyMatchingRule)).map { seq =>
      "Reglas de búsqueda:\n" + seq.mkString("\n")
    }
  }
}
