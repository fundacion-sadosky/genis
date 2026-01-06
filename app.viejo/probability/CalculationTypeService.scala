package probability

import javax.inject.{Inject, Named, Singleton}

import kits.{AnalysisType, AnalysisTypeService}
import matching.MatchResult
import play.api.Logger
import play.api.i18n.Messages
import profile.{Profile, ProfileRepository, ProfileService}
import profile.Profile.Genotypification
import types.SampleCode

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

abstract class CalculationTypeService {
  def getAnalysisTypeByCalculation(calculation: String): Future[AnalysisType]
  def filterProfile(p: Profile, calculation: String): Future[Genotypification]
  def filterProfiles(profiles: Seq[Profile], calculation: String): Future[Seq[Genotypification]]
  def filterCodes(codes: List[SampleCode], calculation: String,profiles:List[Profile]=Nil): Future[Seq[Genotypification]]
  def filterMatchResults(matchResults: Seq[MatchResult], calculation: String): Future[Seq[MatchResult]]
}

@Singleton
class CalculationTypeServiceImpl @Inject()(
  profileRepository: ProfileRepository,
  analysisTypeService: AnalysisTypeService,
  @Named("calculationTypes") calculationTypes: Map[String,String]) extends CalculationTypeService {

  val logger = Logger(this.getClass)

  override def getAnalysisTypeByCalculation(calculation: String): Future[AnalysisType] = {
    val analysisTypeName = calculationTypes.get(calculation).get
    analysisTypeService.getByName(analysisTypeName) map { opt => opt.get }
  }

  override def filterProfile(p: Profile, calculation: String): Future[Genotypification] = {
    getAnalysisTypeByCalculation(calculation) map { at =>
      p.genotypification(at.id)
    }
  }

  override def filterProfiles(profiles: Seq[Profile], calculation: String): Future[Seq[Genotypification]] = {
    Future.sequence(profiles.map(p => this.filterProfile(p, calculation)))
  }

  override def filterCodes(codes: List[SampleCode], calculation: String,profiles:List[Profile]=Nil): Future[Seq[Genotypification]] = {
    filterByCode(codes,profiles) flatMap { profiles =>{
      this.filterProfiles(profiles , calculation)
    }
    }
  }
  def filterByCode(codes: List[SampleCode],profiles:List[Profile]=Nil): Future[Seq[Profile]] ={
    val profilesToAdd = profiles.filter(p=>codes.contains(p.globalCode))
    if(profilesToAdd.map(_.globalCode).containsSlice(codes)){
      Future.successful(profilesToAdd)
    }else{
      profileRepository.findByCodes(codes)
    }
  }
  override def filterMatchResults(matchResults: Seq[MatchResult], calculation: String): Future[Seq[MatchResult]] = {
    getAnalysisTypeByCalculation(calculation) map { at =>
      matchResults.filter(mr => mr.`type` == at.id)
    }
  }

}

