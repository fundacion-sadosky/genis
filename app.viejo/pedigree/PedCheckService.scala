package pedigree

import inbox.{NotificationService, PedigreeConsistencyInfo}
import javax.inject.{Inject, Singleton}
import matching.MatchingService

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import profile.{Profile, ProfileRepository}
import profiledata.ProfileDataService
import types.SampleCode

trait PedCheckService {
  def insert(pedChecks:List[PedCheck]): Future[Either[String, Unit]]
  def getPedCheck(idPedigree:Long):Future[List[PedCheck]]
  def generatePedCheck(idPedigree:Long,idCourtCase:Long,userId:String):Future[Either[String, PedCheckResult]]
  def getPedCheckProfiles(idPedigree:Long,idCourtCase:Long):Future[PedCheckResult]
  def cleanConsistency(idPedigree:Long):Unit
}
@Singleton
class PedCheckServiceImpl @Inject() (pedCheckRepository:PedCheckRepository,
                                     pedigreeService:PedigreeService,
                                     pedigreeRepository:PedigreeRepository,
                                     profileDataService: ProfileDataService,
                                     notificationService: NotificationService,
                                     pedigreeDataRepository:PedigreeDataRepository,
                                     matchingService:MatchingService,
                                     profileRepo: ProfileRepository
                                    ) extends PedCheckService {

  override def cleanConsistency(idPedigree: Long): Unit = {
    this.pedCheckRepository.cleanConsistency(idPedigree).onComplete(_=>{
    this.pedigreeDataRepository.doCleanConsistency(idPedigree)
    })
    ()
  }
  override def insert(pedChecks:List[PedCheck]): Future[Either[String, Unit]] = {
    this.pedCheckRepository.insert(pedChecks)
  }
  override def getPedCheck(idPedigree:Long):Future[List[PedCheck]] = {
    this.pedCheckRepository.getPedCheck(idPedigree)
  }
  override def generatePedCheck(idPedigree:Long,idCourtCase:Long,userId:String):Future[Either[String, PedCheckResult]] = {
    pedigreeDataRepository.getPedigreeMetaData(idPedigree).flatMap(metadata => pedigreeRepository.get(idPedigree)
      .map(geno=> metadata.map(x => x.copy(pedigreeGenogram = geno))))
      .flatMap(pedigreeMetadata => {
      if(pedigreeMetadata.isDefined && pedigreeMetadata.get.pedigreeGenogram.isDefined){

        val genogram = pedigreeMetadata.get.pedigreeGenogram.get.genogram
        val globalCodes = genogram.map(_.globalCode.map(_.text)).toList.flatten
        profileRepo.findByCodes(globalCodes.map(SampleCode(_))).flatMap(profiles  => {
          profileDataService.findByCodes(globalCodes.map(x => SampleCode(x))).flatMap(profileDatas => {
            val pedigreeConsistencyCheck = checkConsistency(genogram,profiles)
            val inserts = pedigreeConsistencyCheck.flatMap(x => x.locus.map(l => PedCheck(0L,idPedigree,l,x.globalCode)))
            this.insert(inserts.toList).map{
              case Left(m) => Left(m)
              case Right(_) =>
                val consistencyResult = pedigreeConsistencyCheck
                  .map(result => PedigreeConsistency(result.globalCode,
                    profileDatas.find(_.globalCode.text == result.globalCode).map(_.internalSampleCode).getOrElse(""),
                    result.locus))

                Right(PedCheckResult(profileDatas.toList.map(profileData => {
                  consistencyResult.find(_.globalCode == profileData.globalCode.text).getOrElse(
                    PedigreeConsistency(profileData.globalCode.text,profileData.internalSampleCode,Nil)
                  )
                }).sortBy(_.internalCode),!consistencyResult.exists(_.locus.nonEmpty),true))
            }
          }).flatMap(result => {
            this.pedigreeDataRepository.changePedigreeConsistencyFlag(idPedigree).map(_ => {
//              notificationService.push(userId,PedigreeConsistencyInfo(idCourtCase,idPedigree,pedigreeMetadata.get.pedigreeMetaData.name))
              result
            })
          })
        })
      }else{
        Future.successful(Left("No hay perfiles asociados"))
      }
    })
  }
  private def checkConsistency(genogram: Seq[Individual],profiles: scala.Seq[Profile]):Seq[PedigreeConsistencyCheck]={
    PedigreeConsistencyAlgorithm.isConsistent(profiles.toArray,genogram.toArray)
  }
  override def getPedCheckProfiles(idPedigree:Long,idCourtCase:Long):Future[PedCheckResult] = {
    this.getPedCheck(idPedigree).flatMap(pedChecks => {
      pedigreeRepository.get(idPedigree).flatMap(genogram => {
        if(genogram.isEmpty){
          Future.successful(Nil)
        }else{
          val globalCodes =genogram.get.genogram.map(_.globalCode.map(_.text)).toList.flatten
          profileDataService.findByCodes(globalCodes.map(x => SampleCode(x))).map(profileDatas => {
            val locusByGlobalCode = pedChecks.groupBy(_.globalCode)
            globalCodes.map(globalCode => {
              val l:List[String] = locusByGlobalCode.get(globalCode).map(_.map(_.locus)).toList.flatten
              PedigreeConsistency(globalCode,profileDatas.find(_.globalCode.text == globalCode).map(_.internalSampleCode).getOrElse(""),l)
            })
          })
        }
      })
    }).flatMap(consistencyList => {
      pedigreeDataRepository.getPedigreeMetaData(idPedigree).map(pedigreeCreation => {
        val consistencyRun = pedigreeCreation.map(_.pedigreeMetaData.consistencyRun.getOrElse(false)).exists(x => x)
        PedCheckResult(consistencyList.sortBy(_.internalCode),!consistencyList.exists(_.locus.nonEmpty) && consistencyRun,consistencyRun)
      })
    })
  }
}
