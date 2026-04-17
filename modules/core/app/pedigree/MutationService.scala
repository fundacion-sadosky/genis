package pedigree

import javax.inject.{Inject, Named, Singleton}
import org.apache.pekko.actor.ActorSystem
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode
import kits.{FullLocus, LocusService}
import profile.Profile
import stats.{PopulationBaseFrequencyGrouppedByLocus, PopulationBaseFrequencyService}

trait MutationService:
  def getAllMutationModelType(): Future[List[MutationModelType]]
  def getAllMutationModels(): Future[List[MutationModel]]
  def getActiveMutationModels(): Future[List[MutationModel]]
  def deleteMutationModelById(id: Long): Future[Either[String, Unit]]
  def insertMutationModel(row: MutationModelFull): Future[Either[String, Long]]
  def updateMutationModel(fullMutationModel: MutationModelFull): Future[Either[String, Unit]]
  def getMutationModel(id: Option[Long]): Future[Option[MutationModelFull]]
  def getMutatitionModelParameters(idMutationModel: Long): Future[List[MutationModelParameter]]
  def getMutationModelData(
    mutationModel: Option[MutationModel],
    markers: List[String]
  ): Future[Option[List[(MutationModelParameter, List[MutationModelKi], MutationModel)]]]
  def addLocus(full: FullLocus): Future[Either[String, Unit]]
  def generateKis(mutationModel: MutationModel): Future[Unit]
  def refreshAllKis(): Future[Unit]
  def refreshAllKisSecuential(): Future[Unit]
  def getAllMutationDefaultParameters(): Future[List[MutationDefaultParam]]
  def getAllLocusAlleles(): Future[List[(String, Double)]]
  def saveLocusAlleles(list: List[(String, Double)]): Future[Either[String, Int]]
  def generateN(profiles: Array[Profile], mutationModel: Option[MutationModel]): Future[Either[String, Unit]]
  def getAllPossibleAllelesByLocus(): Future[Map[String, List[Double]]]

@Singleton
class MutationServiceImpl @Inject() (
  mutationRepository: MutationRepository,
  @Named("defaultMutationRateI") val defaultMutationRateI: String,
  @Named("defaultMutationRateF") val defaultMutationRateF: String,
  @Named("defaultMutationRateM") val defaultMutationRateM: String,
  @Named("defaultMutationRange") val defaultMutationRange: String,
  @Named("defaultMutationRateMicrovariant") val defaultMutationRateMicrovariant: String,
  val populationBaseFrequencyService: PopulationBaseFrequencyService,
  val locusService: LocusService,
  akkaSystem: ActorSystem
)(implicit ec: ExecutionContext) extends MutationService:

  val generateMatrixMutationTypes = List(2L)
  private val mutationActor = akkaSystem.actorOf(MutationActor.props(this))

  override def getMutationModel(id: Option[Long]): Future[Option[MutationModelFull]] =
    this.mutationRepository.getMutationModel(id).flatMap {
      case None => Future.successful(None)
      case Some(mutationModel) =>
        this.getMutatitionModelParameters(mutationModel.id).map { list =>
          Some(MutationModelFull(mutationModel, list))
        }
    }

  override def getMutatitionModelParameters(idMutationModel: Long): Future[List[MutationModelParameter]] =
    this.mutationRepository.getMutatitionModelParameters(idMutationModel)

  override def getAllMutationModelType(): Future[List[MutationModelType]] =
    this.mutationRepository.getAllMutationModelType()

  override def getActiveMutationModels(): Future[List[MutationModel]] =
    this.mutationRepository.getActiveMutationModels()

  override def getAllMutationModels(): Future[List[MutationModel]] =
    this.mutationRepository.getAllMutationModels()

  // Legacy: nunca delega al repo — retorna Right(()) sin efectos. Preservado por fidelidad.
  override def deleteMutationModelById(id: Long): Future[Either[String, Unit]] =
    Future.successful(Right(()))

  override def insertMutationModel(row: MutationModelFull): Future[Either[String, Long]] =
    this.mutationRepository.insertMutationModel(row)

  override def updateMutationModel(fullMutationModel: MutationModelFull): Future[Either[String, Unit]] =
    this.mutationRepository.updateMutationModel(fullMutationModel)

  // Legacy: el Future retornado NO espera al pipeline interno (fire-and-forget).
  // Se conserva ese comportamiento para fidelidad con la versión original.
  override def generateKis(mutationModel: MutationModel): Future[Unit] =
    if generateMatrixMutationTypes.contains(mutationModel.mutationType) then
      locusService.getLocusByAnalysisType(1).flatMap { autosomalLocus =>
        this.mutationRepository.deleteMutationModelKiByIdMutationModel(mutationModel.id).flatMap {
          case Right(_) =>
            populationBaseFrequencyService.getAllPossibleAllelesByLocus().flatMap { populationBaseFrequency =>
              getMutatitionModelParameters(mutationModel.id).map { parameters =>
                if (autosomalLocus.size * 3) == parameters.size then
                  generateMatrixForParameters(mutationModel, parameters, populationBaseFrequency)
                ()
              }
            }
          case _ => Future.successful(())
        }
      }
    Future.successful(())

  override def refreshAllKis(): Future[Unit] =
    this.getAllMutationModels().flatMap { listModels =>
      Future.sequence(listModels.map(model => generateKis(model))).map(_ => ())
    }

  override def refreshAllKisSecuential(): Future[Unit] =
    mutationActor ! "refreshAllKis"
    Future.successful(())

  private def generateMatrixForParameters(
    mutationModel: MutationModel,
    parameters: List[MutationModelParameter],
    populationBaseFrequency: PopulationBaseFrequencyGrouppedByLocus
  ): Future[Unit] =
    calculateKis(mutationModel, parameters, populationBaseFrequency).map { kis =>
      this.mutationRepository.insertKi(kis)
    }.flatMap(_ => Future.successful(()))

  private def calculateKis(
    mutationModel: MutationModel,
    parameters: List[MutationModelParameter],
    populationBaseFrequency: PopulationBaseFrequencyGrouppedByLocus
  ): Future[List[MutationModelKi]] =
    Future.sequence(parameters.map { param =>
      if param.mutationRate.isEmpty || param.mutationRange.isEmpty then
        Future.successful(Nil)
      else
        getMutationModelKit(param, populationBaseFrequency.base.get(param.locus), mutationModel.cantSaltos)
    }).map(_.flatten)

  private def getMutationModelKit(
    param: MutationModelParameter,
    alleles: Option[List[Double]],
    cantSaltos: Long
  ): Future[List[MutationModelKi]] =
    if alleles.isEmpty || alleles.get.isEmpty then
      Future.successful(Nil)
    else
      val allelesI = alleles.get
      Future.successful(allelesI.map { i =>
        // entero menor o igual a la cant de saltos
        val sum = allelesI
          .filter(j => (j != i) && ((j - i).abs <= cantSaltos.toDouble) && ((j - i).abs % 1 == 0))
          .map(j =>
            scala.math.pow(param.mutationRange.get.toDouble, scala.math.abs(i - j))
          ).sum

        var ki = BigDecimal.valueOf(0.0)
        if !sum.equals(0.0) then
          ki = param.mutationRate.get / sum

        MutationModelKi(
          id = 0L,
          idMutationModelParameter = param.id,
          allele = i,
          ki = ki.setScale(8, RoundingMode.HALF_EVEN)
        )
      })

  // Legacy: dispara insertParameters + generateKis fire-and-forget y retorna Right(())
  // incondicionalmente. Preservado por fidelidad.
  override def addLocus(full: FullLocus): Future[Either[String, Unit]] =
    val defaultMutationRateTypes             = List(1L, 2L, 3L)
    val defaultMutationRangeTypes            = List(2L, 3L)
    val defaultMutationRateMicrovariantTypes = List(3L)
    this.getAllMutationModels().flatMap { listModels =>
      val profilesF = listModels.map(model =>
        MutationModelParameter(
          id = 0L,
          idMutationModel = model.id,
          locus = full.locus.id,
          sex = "F",
          mutationRate =
            if defaultMutationRateTypes.contains(model.mutationType)
            then Some(BigDecimal(defaultMutationRateF))
            else None,
          mutationRange =
            if defaultMutationRangeTypes.contains(model.mutationType)
            then Some(BigDecimal(defaultMutationRange))
            else None,
          mutationRateMicrovariant =
            if defaultMutationRateMicrovariantTypes.contains(model.mutationType)
            then Some(BigDecimal(defaultMutationRateMicrovariant))
            else None
        )
      )
      val profilesM = profilesF.map(x =>
        x.copy(sex = "M", mutationRate = x.mutationRate.map(_ => BigDecimal(defaultMutationRateM)))
      )
      val profilesI = profilesF.map(x =>
        x.copy(sex = "I", mutationRate = x.mutationRate.map(_ => BigDecimal(defaultMutationRateI)))
      )

      val allProfiles = profilesF ++ profilesM ++ profilesI
      this.mutationRepository.insertParameters(allProfiles).map { _ =>
        Future.sequence(listModels.map(model => generateKis(model)))
      }
      Future.successful(Right(()))
    }
    Future.successful(Right(()))

  override def getMutationModelData(
    mutationModel: Option[MutationModel],
    markers: List[String]
  ): Future[Option[List[(MutationModelParameter, List[MutationModelKi], MutationModel)]]] =
    if mutationModel.nonEmpty then
      mutationRepository.getMutatitionModelData(
        mutationModel.get.id,
        mutationModel.get.mutationType,
        markers
      ).map(result => Some(result.map(r => (r._1, r._2, mutationModel.get))))
    else
      Future.successful(None)

  override def getAllMutationDefaultParameters(): Future[List[MutationDefaultParam]] =
    this.mutationRepository.getAllMutationDefaultParameters()

  override def getAllLocusAlleles(): Future[List[(String, Double)]] =
    this.mutationRepository.getAllLocusAlleles()

  override def saveLocusAlleles(list: List[(String, Double)]): Future[Either[String, Int]] =
    this.populationBaseFrequencyService.getAllPossibleAllelesByLocus().flatMap { result =>
      this.mutationRepository.insertLocusAlleles(
        list.filter(tuple => !result.base.getOrElse(tuple._1, Nil).contains(tuple._2))
      )
    }

  // Legacy: dispara refreshAllKis fire-and-forget y retorna Right(()).
  // Preservado por fidelidad.
  override def generateN(
    profiles: Array[Profile],
    mutationModel: Option[MutationModel]
  ): Future[Either[String, Unit]] =
    if mutationModel.isDefined then
      Future.sequence(profiles.toSeq.map(profile => locusService.saveLocusAllelesFromProfile(profile)))
        .flatMap { result =>
          if result.forall(_.isRight) then
            val total = result.map(_.toOption.get).sum
            if total > 0 then
              refreshAllKis().flatMap(_ => Future.successful(Right(())))
            else
              Future.successful(Right(()))
          else
            Future.successful(Left("Error al generar el matriz de mutacion"))
        }
      Future.successful(Right(()))
    else
      Future.successful(Right(()))

  override def getAllPossibleAllelesByLocus(): Future[Map[String, List[Double]]] =
    this.populationBaseFrequencyService.getAllPossibleAllelesByLocus().map(_.base)
