package pedigree

import akka.actor.ActorSystem
import javax.inject.{Inject, Named, Singleton}
import kits.FullLocus
import stats.{PopulationBaseFrequencyGrouppedByLocus, PopulationBaseFrequencyService}
import kits.LocusService
import matching.CollapsingActor
import profile.{Profile, ProfileService}
import search.FullTextSearchService
import pedigree.MutationActor
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.math.BigDecimal.RoundingMode
trait MutationService {
  def getAllMutationModelType():Future[List[MutationModelType]]
  def getAllMutationModels():Future[List[MutationModel]]
  def getActiveMutationModels():Future[List[MutationModel]]
  def deleteMutationModelById(id: Long): Future[Either[String, Unit]]
  def insertMutationModel(row: MutationModelFull): Future[Either[String, Long]]
  def updateMutationModel(fullMutationModel: MutationModelFull): Future[Either[String, Unit]]
  def getMutationModel(id: Option[Long]) : Future[Option[MutationModelFull]]
  def getMutatitionModelParameters(idMutationModel: Long) : Future[List[MutationModelParameter]]
  def getMutationModelData(mutationModel: Option[MutationModel],markers:scala.List[String]):Future[Option[List[(MutationModelParameter, List[MutationModelKi],MutationModel)]]]
  def addLocus(full: FullLocus): Future[Either[String, Unit]]
  def generateKis(mutationModel: MutationModel):Future[Unit]
  def refreshAllKis():Future[Unit]
  def refreshAllKisSecuential():Future[Unit]
  def getAllMutationDefaultParameters():Future[List[MutationDefaultParam]]
  def getAllLocusAlleles():Future[List[(String,Double)]]
  def saveLocusAlleles(list:List[(String,Double)]):Future[Either[String, Int]]
  def generateN(profiles: Array[Profile],mutationModel: Option[MutationModel]):Future[Either[String, Unit]]
  def getAllPossibleAllelesByLocus(): Future[Map[String,List[Double]]]
}
@Singleton
class MutationServiceImpl @Inject() (mutationRepository: MutationRepository,
                                     @Named("defaultMutationRateI") val defaultMutationRateI: String,
                                     @Named("defaultMutationRateF") val defaultMutationRateF: String,
                                     @Named("defaultMutationRateM") val defaultMutationRateM: String,
                                     @Named("defaultMutationRange") val defaultMutationRange: String,
                                     @Named("defaultMutationRateMicrovariant") val defaultMutationRateMicrovariant: String,
                                     val populationBaseFrequencyService:PopulationBaseFrequencyService,
                                     val locusService:LocusService,
                                     val fullTextSearchService:FullTextSearchService,
                                     val profileService:ProfileService,
                                     akkaSystem: ActorSystem) extends MutationService {
  val generateMatrixMutationTypes = List(2L)
  private val mutationActor = akkaSystem.actorOf(MutationActor.props(this))

  override def getMutationModel(id: Option[Long]): Future[Option[MutationModelFull]]= {
    this.mutationRepository.getMutationModel(id).flatMap{
      case None => Future.successful(None)
      case Some(mutationModel) => {
        this.getMutatitionModelParameters(mutationModel.id).map( list => {
          Some(MutationModelFull(mutationModel,list))
        })
      }
    }
  }

  override def getMutatitionModelParameters(idMutationModel: Long) : Future[List[MutationModelParameter]] = {
    this.mutationRepository.getMutatitionModelParameters(idMutationModel)
  }

  override def getAllMutationModelType():Future[List[MutationModelType]] = {
    this.mutationRepository.getAllMutationModelType()
  }

  override def getActiveMutationModels():Future[List[MutationModel]] = {
    this.mutationRepository.getActiveMutationModels()
  }
  override def getAllMutationModels():Future[List[MutationModel]] = {
    this.mutationRepository.getAllMutationModels()
  }
  override def deleteMutationModelById(id: Long): Future[Either[String, Unit]] = {
    Future.successful(Right(()))
  }

  override def insertMutationModel(row: MutationModelFull): Future[Either[String, Long]] = {
    this.mutationRepository.insertMutationModel(row)
  }

  override def updateMutationModel(fullMutationModel: MutationModelFull): Future[Either[String, Unit]] = {
    this.mutationRepository.updateMutationModel(fullMutationModel)
  }
  override def generateKis(mutationModel: MutationModel):Future[Unit] = {
    Future.successful(
      if(generateMatrixMutationTypes.contains(mutationModel.mutationType)){
        locusService.getLocusByAnalysisType(1).flatMap(autosomalLocus => {
          this.mutationRepository.deleteMutationModelKiByIdMutationModel(mutationModel.id).flatMap{
            case Right(_) => {
              populationBaseFrequencyService.getDefaultTableAllelesByLocus().flatMap(populationBaseFrequency => {
                  getMutatitionModelParameters(mutationModel.id).map(parameters => {
                    if((autosomalLocus.size* 3) == parameters.size) {
                      generateMatrixForParameters(mutationModel, parameters, populationBaseFrequency)
                    }
                    Future.successful(())
                  })
              })
            }
            case _ => Future.successful(None)
          }
        })
      }
    )
  }
  override def refreshAllKis():Future[Unit] = {
    this.getAllMutationModels().flatMap(listModels => {
      Future.sequence(listModels.map(model => {
        generateKis(model)
      })).map(_ => ())
    })
  }
  override def refreshAllKisSecuential():Future[Unit] = {
    mutationActor ! "refreshAllKis"
    Future.successful(())
  }
  private def generateMatrixForParameters(mutationModel: MutationModel,parameters:List[MutationModelParameter],
                                          populationBaseFrequency:PopulationBaseFrequencyGrouppedByLocus):Future[Unit] = {

    calculateKis(mutationModel,parameters,populationBaseFrequency).map(kis => {
      this.mutationRepository.insertKi(kis)
    }).flatMap(_ => {
      Future.successful(())
    })
  }
  private def calculateKis(mutationModel: MutationModel,
                           parameters:List[MutationModelParameter],
                           populationBaseFrequency:PopulationBaseFrequencyGrouppedByLocus):
                           Future[List[MutationModelKi]] = {
    Future.sequence(parameters.map(param => {
      if(param.mutationRate.isEmpty || param.mutationRange.isEmpty){
        Future.successful(Nil)
      }else{
        getMutationModelKit(param,populationBaseFrequency.base.get(param.locus), mutationModel.cantSaltos)
      }
    })).map(result => result.flatten)

  }
  private def getMutationModelKit(param:MutationModelParameter,
                                  alleles: Option[scala.List[Double]], cantSaltos : Long):Future[List[MutationModelKi]] = {
    if(alleles.isEmpty || alleles.get.isEmpty){
      Future.successful(Nil)
    }else{
      val allelesI = alleles.get

      Future.successful(allelesI.map(i => {
        val sum = MutationService.mutationNormalizationSum(
          i, allelesI, param.mutationRange.get.toDouble, cantSaltos
        )

        var ki = BigDecimal.valueOf(0.0)
        if (!sum.equals(0.0)) {
          ki = param.mutationRate.get / sum
        }

        MutationModelKi(id = 0,
          idMutationModelParameter = param.id,
          allele = i,
          ki.setScale(8,RoundingMode.HALF_EVEN))
      }))
    }
  }
  override def addLocus(full: FullLocus): Future[Either[String, Unit]] = {
    val defaultMutationRateTypes = List(1L,2L,3L)
    val defaultMutationRangeTypes = List(2L,3L)
    val defaultMutationRateMicrovariantTypes = List(3L)
    this.getAllMutationModels().flatMap(listModels => {
      val profilesF = listModels.map(model => {
        MutationModelParameter(id = 0,idMutationModel = model.id,locus = full.locus.id,sex = "F",
          if(defaultMutationRateTypes.contains(model.mutationType))
            {Some(BigDecimal(defaultMutationRateF))} else None,
          if(defaultMutationRangeTypes.contains(model.mutationType))
            {Some(BigDecimal(defaultMutationRange))} else None,
          if(defaultMutationRateMicrovariantTypes.contains(model.mutationType))
            {Some(BigDecimal(defaultMutationRateMicrovariant))} else None)
      })
      val profilesM = profilesF.map(x => x.copy(sex = "M",mutationRate = x.mutationRate.map( _ => BigDecimal(defaultMutationRateM))))
      val profilesI = profilesF.map(x => x.copy(sex = "I",mutationRate = x.mutationRate.map( _ => BigDecimal(defaultMutationRateI))))

      val allProfiles = profilesF ++ profilesM ++ profilesI
      this.mutationRepository.insertParameters(allProfiles).map(_ => {
        Future.sequence(listModels.map(model => {
            generateKis(model)
        }))
      })
      Future.successful(Right(()))
    })
    Future.successful(Right(()))
  }

  override def getMutationModelData(mutationModel: Option[MutationModel],markers:scala.List[String]):Future[Option[List[(MutationModelParameter, List[MutationModelKi],MutationModel)]]] = {
    if (mutationModel.nonEmpty) {
      mutationRepository.getMutatitionModelData(mutationModel.get.id,
        mutationModel.get.mutationType,markers).map(result => {
        Some((result.map(r => (r._1,r._2,mutationModel.get))))
      })
    } else {
      Future.successful(None)
    }
  }

  override def getAllMutationDefaultParameters():Future[List[MutationDefaultParam]] = {
    this.mutationRepository.getAllMutationDefaultParameters()
  }
  override def getAllLocusAlleles():Future[List[(String,Double)]] = {
    this.mutationRepository.getAllLocusAlleles()
  }
  override def saveLocusAlleles(list:List[(String,Double)]):Future[Either[String, Int]] = {
    this.populationBaseFrequencyService.getAllPossibleAllelesByLocus().flatMap(result => {
      this.mutationRepository.insertLocusAlleles(list.filter(tuple => !result.base.getOrElse(tuple._1,Nil).contains(tuple._2)))
    })
  }
  override def generateN(profiles: Array[Profile],mutationModel: Option[MutationModel]):Future[Either[String, Unit]] = {
    if(mutationModel.isDefined){
      Future.sequence(profiles.toSeq.map(profile=> {
        locusService.saveLocusAllelesFromProfile(profile)
      })).flatMap(result => {
        if(result.forall(_.isRight)){
          val total = result.map(x => x.right.get).sum
          if(total> 0){
            refreshAllKis().flatMap(_ => {
              Future.successful(Right(()))
            })
          }else{
            Future.successful(Right(()))
          }
        }else{
          Future.successful(Left("Error al generar el matriz de mutacion"))
        }
      })
      Future.successful(Right(()))
    }else{
      Future.successful(Right(()))
    }
  }
  def getAllPossibleAllelesByLocus(): Future[Map[String,List[Double]]] = {
    this.populationBaseFrequencyService.getAllPossibleAllelesByLocus().map(_.base)
  }
}

object MutationService {
  private val stepTolerance = 1e-9

  def isWholeRepeatStep(i: Double, j: Double): Boolean = {
    val d = scala.math.abs(i - j)
    scala.math.abs(d - scala.math.rint(d)) < stepTolerance
  }

  def stepDistance(i: Double, j: Double, cantSaltos: Long): Option[Double] = {
    val pasos = scala.math.rint(scala.math.abs(i - j))
    if (isWholeRepeatStep(i, j) && pasos <= cantSaltos.toDouble) {
      Some(pasos)
    } else {
      None
    }
  }

  def recalibrateKis(
    data: List[(MutationModelParameter, List[MutationModelKi], MutationModel)],
    n: Map[String, List[Double]]
  ): List[(MutationModelParameter, List[MutationModelKi], MutationModel)] =
    data.map {
      case entry @ (param, _, model) =>
        // (3) "Stepwise extended" reusa la misma k_i que (2) para sus vecinos
        // de la misma clase de microvariante -- BayesianNetwork.
        // getProbabityOfNonDiagonal caso (3) la busca en mutationMarkerData._2
        // igual que el caso (2). La mutationRateMicrovariant de (3) no tiene
        // k_i propia: se reparte directo, sin calibrar (ver ese caso).
        if ((model.mutationType != 2L && model.mutationType != 3L) ||
            param.mutationRate.isEmpty ||
            param.mutationRange.isEmpty) {
          entry
        } else {
          val universe = n.getOrElse(param.locus, Nil).filter(_ > 0).distinct.sorted
          if (universe.isEmpty) {
            entry
          } else {
            (param, universe.map(allele => MutationModelKi(
              id = 0L,
              idMutationModelParameter = param.id,
              allele = allele,
              ki = mutationNormalizationKi(
                allele, universe, param.mutationRate.get,
                param.mutationRange.get.toDouble, model.cantSaltos))), model)
          }
        }
    }

  def mutationNormalizationKi(
    allele: Double,
    alleles: List[Double],
    mutationRate: BigDecimal,
    mutationRange: Double,
    cantSaltos: Long
  ): BigDecimal = {
    val sum = mutationNormalizationSum(allele, alleles, mutationRange, cantSaltos)
    if (sum == 0.0) BigDecimal(0.0) else mutationRate / sum
  }

  def mutationNormalizationSum(
    i: Double,
    alleles: List[Double],
    mutationRange: Double,
    cantSaltos: Long
  ): Double = {
    alleles
      .filter(_ != i)
      .flatMap(j => stepDistance(i, j, cantSaltos))
      .map(pasos => scala.math.pow(mutationRange, pasos))
      .sum
  }
}
