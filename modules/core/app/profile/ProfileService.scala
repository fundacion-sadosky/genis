package profile

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.{Calendar, Date, UUID}
import javax.inject.{Inject, Named, Singleton}
import configdata.*
import connections.InterconnectionService
import inbox.{NotificationService, ProfileDataAssociationInfo, ProfileDataInfo}
import kits.*
import matching.*
import pedigree.{PedigreeSearch, PedigreeService, PedigreeStatus}
import play.api.Logger
import play.api.i18n.{Messages, MessagesApi}
import probability.{NoFrequencyException, ProbabilityService}
import profile.GenotypificationByType.GenotypificationByType
import profiledata.{ProfileData, ProfileDataRepository}
import services.{CacheService, TemporaryAssetKey, UserService}
import trace.{AnalysisInfo, AssociationInfo, Trace, TraceService}
import types.{AlphanumericId, MongoDate, SampleCode}
import util.Misc
import play.api.libs.json.Json
import profile.Profile.Marker

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Right, Try}

trait ProfileService {
  def create(newAnalysis: NewAnalysis, savePictures: Boolean = true, replicate: Boolean = false): Future[Either[List[String], Profile]]
  def findByCode(globalCode: SampleCode): Future[Option[Profile]]
  def get(id: SampleCode): Future[Option[Profile]]
  def getElectropherogramsByCode(globalCode: SampleCode): Future[List[FileUploadedType]]
  def getElectropherogramImage(profileId: SampleCode, electropherogramId: String): Future[Option[Array[Byte]]]
  def getElectropherogramsByAnalysisId(profileId: SampleCode, analysisId: String): Future[List[FileUploadedType]]
  def getProfileModelView(globalCode: SampleCode): Future[ProfileModelView]
  def saveElectropherograms(token: String, globalCode: SampleCode, idAnalysis: String, name: String): Future[List[Either[String, SampleCode]]]
  def verifyMixtureAssociation(mixtureGenot: GenotypificationByType, globalCode: SampleCode, mixtureSubcategoryId: AlphanumericId): Future[Either[String, ProfileAsociation]]
  def saveLabels(globalCode: SampleCode, labels: Profile.LabeledGenotypification, userId: String): Future[Either[List[String], SampleCode]]
  def findByCodes(globalCodes: List[SampleCode]): Future[Seq[Profile]]
  def getLabels(globalCode: SampleCode): Future[Option[Profile.LabeledGenotypification]]
  def validateAnalysis(analysis: Profile.Genotypification, categoryId: AlphanumericId, kitId: Option[String], contributors: Int, `type`: Option[Int], analysisType: AnalysisType): Future[Either[List[String], CategoryConfiguration]]
  def existProfile(globalCode: SampleCode): Future[Boolean]
  def importProfile(profileData: ProfileData, analysis: NewAnalysis, replicate: Boolean = false): Future[Either[List[String], Profile]]
  def getAssociatedProfiles(profile: Profile): Future[Seq[Profile]]
  def getLabelsSets(): Profile.LabelSets
  def validProfilesAssociated(labels: Option[Profile.LabeledGenotypification]): Seq[String]
  def isExistingKit(kitId: String): Future[Boolean]
  def isExistingCategory(idCategory: AlphanumericId): Future[Boolean]
  def addProfile(profile: Profile): Future[SampleCode]
  def fireMatching(sampleCode: SampleCode): Unit
  def updateProfile(profile: Profile): Future[SampleCode]
  def getAnalysisType(kit: Option[String], `type`: Option[Int]): Future[AnalysisType]
  def validateExistingLocusForKit(analysis: Profile.Genotypification, kitId: Option[String]): Future[Either[List[String], Unit]]
  def findProfileLocalOrSuperior(globalCode: SampleCode): Future[Option[Profile]]
  def findProfileDataLocalOrSuperior(globalCode: SampleCode): Future[Option[ProfileData]]

  def getFilesByCode(globalCode: SampleCode): Future[List[FileUploadedType]]
  def getFile(profileId: SampleCode, electropherogramId: String): Future[Option[Array[Byte]]]
  def getFilesByAnalysisId(profileId: SampleCode, analysisId: String): Future[List[FileUploadedType]]
  def saveFile(token: String, globalCode: SampleCode, idAnalysis: String, name: String): Future[List[Either[String, SampleCode]]]
  def isReadOnly(
    profile: Option[Profile],
    uploadedIsAllowed: Boolean = false,
    allowFromOtherInstances: Boolean = false
  ): Future[(Boolean, String)]
  def isReadOnlySampleCode(
    globalCode: SampleCode,
    uploadedIsAllowed: Boolean = false,
    allowFromOtherInstances: Boolean = false
  ): Future[(Boolean, String)]
  def isReadOnly2(profileOpt: Option[Profile]): Future[(Boolean, String, Boolean)]
  def getFullElectropherogramsByCode(globalCode: SampleCode): Future[List[connections.FileInterconnection]]
  def getFullFilesByCode(globalCode: SampleCode): Future[List[connections.FileInterconnection]]
  def addElectropherogramWithId(globalCode: SampleCode, analysisId: String, image: Array[Byte], name: String, id: String): Future[Either[String, SampleCode]]
  def addFileWithId(globalCode: SampleCode, analysisId: String, image: Array[Byte], name: String, id: String): Future[Either[String, SampleCode]]
  def getFullElectropherogramsById(id: String): Future[List[connections.FileInterconnection]]
  def getFullFilesById(id: String): Future[List[connections.FileInterconnection]]

  def removeFile(id: String, user: String): Future[Either[String, String]]
  def removeEpg(id: String, user: String): Future[Either[String, String]]

  def removeAll(): Future[Either[String, String]]

  def removeProfile(globalCode: SampleCode): Future[Either[String, String]]
  def profilesAll(): Future[List[(SampleCode, String)]]
}

@Singleton
class ProfileServiceImpl @Inject()(
  cache: CacheService,
  profileRepository: ProfileRepository,
  profileDataRepository: ProfileDataRepository,
  matchingRepository: MatchingRepository,
  kitService: StrKitService,
  matchingService: MatchingService,
  qualityParams: QualityParamsProvider,
  categoryService: CategoryService,
  notificationService: NotificationService,
  probabilityService: ProbabilityService,
  locusService: LocusService,
  traceService: TraceService,
  pedigreeService: PedigreeService,
  analysisTypeService: AnalysisTypeService,
  @Named("labelsSet") labelsSet: Profile.LabelSets,
  interconnectionService: InterconnectionService,
  matchingRepo: MatchingRepository,
  userService: UserService,
  matchingAlgorithmService: MatchingAlgorithmService,
  messagesApi: MessagesApi
)(implicit ec: ExecutionContext) extends ProfileService {

  private implicit val messages: Messages = messagesApi.preferred(Seq.empty)

  val RECHAZADA = 3L
  val DELETE_IN_SUP_INSTANCE_SENT_TO_INFERIOR = 20L

  val logger: Logger = Logger(this.getClass)

  val manualKit = "Manual"
  val manualMitocondrialKit = "Mitocondrial"

  override def isReadOnly(
    profileOpt: Option[Profile],
    uploadedIsAllowed: Boolean = false,
    allowFromOtherInstances: Boolean = false
  ): Future[(Boolean, String)] = {
    profileOpt.fold(Future.successful((false, ""))) { profile =>
      if (!allowFromOtherInstances && !this.interconnectionService.isFromCurrentInstance(profile.globalCode)) {
        Future.successful((true, Messages("error.E0727")))
      } else {
        (for {
          isDeleted <- profileDataRepository.isDeleted(profile.globalCode)
          uploadStatus <- this.profileDataRepository.getProfileUploadStatusByGlobalCode(profile.globalCode)
        } yield (uploadStatus, isDeleted)).map {
          case (_, Some(true)) => (true, Messages("error.E0729"))
          case (None, _) => (false, "")
          case (Some(_), _) if uploadedIsAllowed => (false, "")
          case (Some(_), _) => (true, Messages("error.E0728"))
        }
      }
    }
  }

  override def isReadOnly2(profileOpt: Option[Profile]): Future[(Boolean, String, Boolean)] = {
    profileOpt.fold(Future.successful((false, "", false))) { profile =>
      if (!this.interconnectionService.isFromCurrentInstance(profile.globalCode)) {
        Future.successful((true, Messages("error.E0727"), false))
      } else {
        (for {
          isDeleted <- profileDataRepository.isDeleted(profile.globalCode)
          uploadStatus <- this.profileDataRepository.getProfileUploadStatusByGlobalCode(profile.globalCode)
        } yield (uploadStatus, isDeleted)).map {
          case (us, Some(true)) => (true, Messages("error.E0729"), us.exists(x => x >= 4))
          case (None, _) => (false, "", false)
          case (Some(status), _) => (true, Messages("error.E0728"), status >= 4)
        }
      }
    }
  }

  override def isReadOnlySampleCode(
    globalCode: SampleCode,
    uploadedIsAllowed: Boolean = false,
    allowFromOtherInstances: Boolean = false
  ): Future[(Boolean, String)] = {
    this.findByCode(globalCode).flatMap(x => this.isReadOnly(x, uploadedIsAllowed, allowFromOtherInstances))
  }

  override def existProfile(globalCode: SampleCode): Future[Boolean] = {
    profileRepository.existProfile(globalCode)
  }

  private val canExtend = (a: AlleleValue, b: AlleleValue) => {
    a.extendTo(b).isDefined
  }

  private def mergeLocusAlleles(
    base: List[AlleleValue],
    extension: List[AlleleValue]
  ): Either[String, List[AlleleValue]] = {
    if (Misc.existsInjection(base, extension, canExtend)) {
      Right(extension)
    } else {
      val l1 = base.map(_.toString()).mkString("(", ", ", ")")
      val l2 = extension.map(_.toString()).mkString("(", ", ", ")")
      Left(Messages("error.E0400", l1, l2))
    }
  }

  private def merge(
    base: GenotypificationByType,
    extension: Profile.Genotypification
  ): Future[Map[Int, Map[Marker, Either[String, List[AlleleValue]]]]] = {
    locusService.list().map { locus =>
      val locusMap = locus.map(l => l.id -> l.analysisType).toMap
      val newGen = extension.groupBy { case (m, _) => locusMap(m) }
      base.keySet.union(newGen.keySet).map { analysisType =>
        val markers = base.getOrElse(analysisType, Map.empty).keySet
          .union(newGen.getOrElse(analysisType, Map.empty).keySet)
        analysisType -> markers.map { l =>
          val safeBase = base.getOrElse(analysisType, Map.empty).withDefaultValue(Nil)
          val safeExt = newGen.getOrElse(analysisType, Map.empty).withDefault(marker => safeBase(marker))
          l -> mergeLocusAlleles(safeBase(l), safeExt(l))
        }.toMap
      }.toMap
    }
  }

  def isExistingKit(kitId: String): Future[Boolean] = {
    kitService.listFull().map { list =>
      list.map(x => x.id).contains(kitId) ||
        list.map(x => x.alias).count(a => a.contains(kitId)) > 0 ||
        kitId == manualKit || kitId == manualMitocondrialKit
    }
  }

  def isExistingCategory(idCategory: AlphanumericId): Future[Boolean] = {
    categoryService.getCategory(idCategory).map(_.isDefined)
  }

  def getRequiredLocusInAnalysis(analysis: Profile.Genotypification, fullLocusList: Seq[Locus]): Int = {
    analysis.filter(x => x._2.nonEmpty)
      .filterKeys(ma => fullLocusList.find(fullLocus => fullLocus.id == ma.toString).map(as => as.required).getOrElse(true))
      .size
  }

  def getNumberOfLocusInAnalysis(analysis: Profile.Genotypification, fullLocusList: Seq[Locus]): Int = {
    analysis.count(x => x._2.nonEmpty)
  }

  def validateExistingLocusForKit(analysis: Profile.Genotypification, kitId: Option[String]): Future[Either[List[String], Unit]] = {
    if (kitId.isEmpty) return Future.successful(Right(()))
    val kit = kitId.get
    for {
      listFullKits <- kitService.listFull()
    } yield {
      val locusCointainedInKitOpt = listFullKits.find(x => x.id == kit).map(x => x.locus).map(x => x.map(x => x.locus))
      if (locusCointainedInKitOpt.isDefined) {
        val locusCointainedInKit = locusCointainedInKitOpt.get.toSet
        val locusCointainedInProfile = analysis.keySet.map(x => x.toString).toSet
        if (!locusCointainedInProfile.subsetOf(locusCointainedInKit)) {
          val diff = locusCointainedInProfile.diff(locusCointainedInKit).mkString(",")
          Left(List(Messages("error.E0682", diff)))
        } else {
          Right(())
        }
      } else {
        Right(())
      }
    }
  }

  def validateAnalysis(
    analysis: Profile.Genotypification,
    categoryId: AlphanumericId,
    kitId: Option[String],
    contributors: Int,
    `type`: Option[Int],
    analysisType: AnalysisType
  ): Future[Either[List[String], CategoryConfiguration]] = {

    for {
      fullLocus <- locusService.list()
      kits <- kitService.list()
      loci <- locusService.list()
      categoryOpt <- categoryService.getCategory(categoryId)
      kit = kitId.fold(
        StrKit(null, null, `type`.get, analysis.size, analysis.size)
      )(id => kits.find(k => k.id == id).get)
      kitLoci <- kitService.findLociByKit(kit.id)
    } yield {
      val category = categoryOpt.get
      val requiredLociKit = kitLoci.count(p => p.required)

      val amel = analysis.filter(x => x._1.equals("AMEL")).get("AMEL")
      var modifiedKit = kit.copy()
      if (amel.isDefined && amel.get.size >= 2 && amel.get(0).equals(XY('X')) && amel.get(1).equals(XY('X'))) {
        val cantYMarker = kitLoci.count(m => m.chromosome.nonEmpty && m.chromosome.get.equals("Y"))
        val newK = kit.representative_parameter - cantYMarker
        modifiedKit = kit.copy(representative_parameter = newK)
      }

      val minLocusQuantityAllowed = qualityParams.minLocusQuantityAllowedPerProfile(category, modifiedKit)
      val maxAllelesPerLocus = qualityParams.maxAllelesPerLocus(category, kit)
      val maxOverageDeviatedLociPerProfile = qualityParams.maxOverageDeviatedLociPerProfile(category, kit)
      val multiallelic = qualityParams.multiallelic(category, kit)
      val trisomyTreshold = if (multiallelic) 4 else 2

      val categoryConfiguration = category.configurations.getOrElse(kit.`type`, CategoryConfiguration("", "", "K", "0", 6))

      def cond[T](p: => Boolean, v: T): Option[T] = if (p) Some(v) else None

      if (analysisType.mitochondrial) {
        Right(categoryConfiguration)
      } else {
        val errors = (
          cond(getNumberOfLocusInAnalysis(analysis, fullLocus) < minLocusQuantityAllowed, Messages("error.E0683", minLocusQuantityAllowed)) ::
          cond(getRequiredLocusInAnalysis(analysis, fullLocus) < requiredLociKit, Messages("error.E0698")) ::
          cond(
            !category.isReference && analysis.exists { case (_, alleles) => alleles.size > maxAllelesPerLocus },
            Messages("error.E0684", maxAllelesPerLocus)
          ) ::
          cond(
            analysis.count { case (marker, alleles) =>
              val locus = loci.find(l => l.id == marker).get
              if (contributors == 1) alleles.size > locus.minimumAllelesQty && alleles.size > trisomyTreshold
              else false
            } > maxOverageDeviatedLociPerProfile,
            Messages("error.E0685", maxOverageDeviatedLociPerProfile)
          ) :: Nil
        ).flatten

        if (errors.nonEmpty) Left(errors)
        else Right(categoryConfiguration)
      }
    }
  }

  override def importProfile(profileData: ProfileData, analysis: NewAnalysis, replicate: Boolean = false): Future[Either[List[String], Profile]] = {
    profileRepository.findByCode(analysis.globalCode).flatMap { profileOpt =>
      if (profileOpt.isEmpty) {
        this.upsert(profileData, None, analysis, false, replicate, null)
      } else if (validProfilesAssociated(profileOpt.get.labeledGenotypification).nonEmpty) {
        Future.successful(Left(List(Messages("error.E0111"))))
      } else {
        this.upsert(profileData, profileOpt, analysis, false, replicate, null)
      }
    }
  }

  private def mergeGenotypification(profileOpt: Option[Profile], newAnalysis: NewAnalysis): Profile.Genotypification = {
    if (newAnalysis.kit.isDefined) newAnalysis.genotypification
    else {
      profileOpt match {
        case Some(p) =>
          val filteredGen = p.genotypification.getOrElse(newAnalysis.`type`.get, List.empty)
          var merge: Profile.Genotypification = Map.empty
          newAnalysis.genotypification.foreach { case (marker, values) => merge += marker -> values }
          filteredGen.foreach { case (marker, values) => if (!merge.contains(marker)) merge += marker -> values }
          merge
        case None => newAnalysis.genotypification
      }
    }
  }

  override def getAnalysisType(kit: Option[String], `type`: Option[Int]): Future[AnalysisType] = {
    kit.fold(Future.successful(`type`.get)) { kit => kitService.get(kit).map(opt => opt.get.`type`) }
      .flatMap { at => analysisTypeService.getById(at).map(opt => opt.get) }
  }

  private def getAnalysisTypeOf(profile: Profile): Future[List[AnalysisType]] =
    profile.analyses match {
      case Some(analyses) =>
        Future.sequence(analyses.map(analisis => getAnalysisType(Some(analisis.kit), analisis.`type`)))
      case None =>
        Future.successful(List.empty)
    }

  private def validateMatchesAndPedigrees(profileOpt: Option[Profile], at: AnalysisType): Future[Either[String, String]] = {
    if (profileOpt.isDefined) {
      val profile = profileOpt.get
      getAnalysisTypeOf(profile).flatMap { analysisTypes =>
      if (analysisTypes.contains(at)) {
        matchingService.matchesNotDiscarded(profile.globalCode).flatMap { results =>
          if (results.nonEmpty) Future.successful(Left(Messages("error.E0112")))
          else {
            pedigreeService.getAllCourtCases(PedigreeSearch(0, Int.MaxValue, "", true, None, Some(profile.globalCode.text))).map { pedigrees =>
              if (pedigrees.exists(p => p.status == PedigreeStatus.Validated || p.status == PedigreeStatus.Active))
                Left(Messages("error.E0202"))
              else
                Right(profile.globalCode.text)
            }
          }
        }
      } else {
        matchingService.matchesWithPartialHit(profile.globalCode).flatMap { results =>
          if (results.nonEmpty) Future.successful(Left(Messages("error.E0112")))
          else {
            pedigreeService.getAllCourtCases(PedigreeSearch(0, Int.MaxValue, "", true, None, Some(profile.globalCode.text))).map { pedigrees =>
              if (pedigrees.exists(p => p.status == PedigreeStatus.Validated))
                Left(Messages("error.E0202"))
              else
                Right(profile.globalCode.text)
            }
          }
        }
      }
      }
    } else Future.successful(Right(""))
  }

  private def upsert(
    profileData: ProfileData,
    profileOpt: Option[Profile],
    newAnalysis: NewAnalysis,
    savePictures: Boolean,
    replicate: Boolean = false,
    userName: String
  ): Future[Either[List[String], Profile]] = {
    this.isReadOnly(profileOpt).flatMap {
      case (true, message) => Future.successful(Left(List(message)))
      case (false, _) =>
        categoryService.getCategory(profileData.category).flatMap { categoryOpt =>
          val category = categoryOpt.get
          val isReference = category.isReference
          val sizeOfMitocondrialKit: Int = 4
          val newAnalysisIsMitochondrial = newAnalysis.genotypification.keys.exists(marker => marker.endsWith("_RANGE"))
          val analysis = Analysis(
            UUID.randomUUID.toString,
            MongoDate(new Date()),
            newAnalysis.kit.getOrElse(
              if (newAnalysis.`type`.contains(sizeOfMitocondrialKit)) manualMitocondrialKit
              else manualKit
            ),
            newAnalysis.genotypification.filterNot { case (marker, alleles) =>
              val thereAreNoAlleles = alleles.isEmpty
              lazy val isMitochondrialRange = marker.endsWith("_RANGE")
              lazy val isMitochondrialMutation = newAnalysisIsMitochondrial && !isMitochondrialRange
              thereAreNoAlleles && !isMitochondrialMutation
            },
            newAnalysis.`type`
          )

          val analysisTypeFut = newAnalysis.kit
            .fold(Future.successful(newAnalysis.`type`.get)) { kit => kitService.get(kit).map(opt => opt.get.`type`) }
            .flatMap { at => analysisTypeService.getById(at).map(opt => opt.get) }

          val labelsFut = newAnalysis.labeledGenotypification.fold[Future[Option[Profile.LabeledGenotypification]]](
            Future.successful(None))(filterLabeledGenotypification(_).map(Some(_)))

          val genotypificationToValidate = mergeGenotypification(profileOpt, newAnalysis)

          val fut = for {
            at <- analysisTypeFut
            labels <- labelsFut
            matchesValidation <- validateMatchesAndPedigrees(profileOpt, at)
            contributors <- getContributors(profileData, analysis, profileOpt, isReference, newAnalysis.contributors, at)
            analysisValidation <- validateAnalysis(genotypificationToValidate, profileData.category, newAnalysis.kit, contributors, newAnalysis.`type`, at)
          } yield {
            if (matchesValidation.isLeft) {
              throw new RuntimeException(matchesValidation.left.get)
            }
            (labels, analysisValidation, contributors)
          }

          val newfut = fut.flatMap { case (labels, validation, contributors) =>
            val mergeFut = merge(profileOpt.fold[GenotypificationByType](Map.empty)(_.genotypification), analysis.genotypification)
            mergeFut.flatMap { mergeResult =>
              val invalid = mergeResult.flatMap(_._2).filter(_._2.isLeft)

              val result: Future[Either[List[String], Profile]] = if (validation.isLeft) {
                Future.successful(Left(validation.left.get))
              } else if (invalid.nonEmpty) {
                val errors = invalid.map { case (marker, error) =>
                  Messages("error.E0686", marker, error.left.get)
                }.toList
                Future.successful(Left(errors))
              } else {
                val n = if (newAnalysis.kit.contains("Mitocondrial")) {
                  profileOpt match {
                    case Some(profile) => profile.genotypification.get(4).isDefined
                    case _ => false
                  }
                } else false

                if (n) {
                  Future.successful(Left(List(Messages("error.E0315"))))
                } else {
                  val newGenotypification = mergeResult.map { case (analysisType, locusMap) =>
                    analysisType -> locusMap.map { case (marker, result) => marker -> result.toOption.get }
                  }

                  val profile = profileOpt.getOrElse(Profile(
                    newAnalysis.globalCode,
                    newAnalysis.globalCode,
                    profileData.internalSampleCode,
                    profileData.assignee,
                    profileData.category,
                    newGenotypification,
                    Some(List(analysis)),
                    labels,
                    Some(contributors),
                    newAnalysis.mismatches,
                    newAnalysis.matchingRules,
                    deleted = false,
                    matcheable = false,
                    isReference = isReference
                  ))

                  val result = if (profileOpt.isEmpty) {
                    profileRepository.add(profile)
                  } else {
                    profileRepository.addAnalysis(
                      profile._id,
                      analysis,
                      newGenotypification,
                      labels,
                      newAnalysis.matchingRules,
                      newAnalysis.mismatches
                    )
                  }
                  result.foreach { _ =>
                    locusService.saveLocusAllelesFromProfile(profile).flatMap {
                      case Right(count) if count > 0 => locusService.refreshAllKis()
                      case _ => Future.successful(())
                    }
                  }

                  result.map { _ =>
                    if (savePictures) {
                      saveElectropherograms(newAnalysis.token, newAnalysis.globalCode, analysis.id, null)
                      (newAnalysis.tokenRawFile, newAnalysis.nameRawFile) match {
                        case (Some(tokenRawFile), Some(nameRawFile)) =>
                          saveFile(tokenRawFile, newAnalysis.globalCode, analysis.id, nameRawFile)
                        case _ => Future.successful(Nil)
                      }
                    }
                    traceService.add(
                      Trace(
                        newAnalysis.globalCode, newAnalysis.userId, new Date(),
                        AnalysisInfo(
                          newAnalysis.genotypification.keySet.toList,
                          newAnalysis.kit,
                          newAnalysis.`type`,
                          validation.toOption.get
                        )
                      )
                    )
                    Right(profile)
                  }
                }
              }
              result.map { createEither =>
                createEither.foreach { p =>
                  matchingService.findMatches(
                    p.globalCode,
                    categoryService.getCategoryTypeFromFullCategory(category)
                  )
                }
                createEither
              }
            }
          }
          if (replicate) {
            newfut.foreach {
              case Right(profile) =>
                interconnectionService.uploadProfileToSuperiorInstance(profile, profileData, userName)
              case _ =>
            }
          }
          newfut.recover {
            case t: Throwable =>
              logger.error("Error creating profile", t)
              Left(List("Error interno al crear el perfil"))
          }
        }
    }
  }

  private def getContributors(
    profileData: ProfileData,
    analysis: Analysis,
    profileOpt: Option[Profile],
    isReference: Boolean,
    contributorsOpt: Option[Int],
    at: AnalysisType
  ): Future[Int] = {
    val statsFut = if (!isReference) {
      probabilityService.getStats(profileData.laboratory)
    } else {
      Future.successful(None)
    }
    statsFut.flatMap { statOption =>
      contributorsOpt match {
        case Some(c) => Future.successful(c)
        case None =>
          if (profileOpt.isDefined) {
            Future.successful(profileOpt.get.contributors.getOrElse(1))
          } else {
            if (!isReference && at.id != 3) {
              val stats = statOption.getOrElse(
                throw new NoFrequencyException(Messages("error.E0610"))
              )
              probabilityService.calculateContributors(analysis, profileData.category, stats)
            } else {
              Future.successful(1)
            }
          }
      }
    }
  }

  override def create(
    newAnalysis: NewAnalysis,
    savePictures: Boolean = true,
    replicate: Boolean = false
  ): Future[Either[List[String], Profile]] = {
    val globalCode = newAnalysis.globalCode
    profileDataRepository.findByCode(globalCode).flatMap { profileDataOpt =>
      profileDataOpt.fold[Future[Either[List[String], Profile]]] {
        Future.successful(Left(List(Messages("error.E0113", globalCode))))
      } { profileData =>
        profileRepository.findByCode(globalCode).flatMap { profileOpt =>
          val res = upsert(profileData, profileOpt, newAnalysis, savePictures, replicate, profileData.assignee)
          res.foreach {
            case Right(_) if profileOpt.isEmpty =>
              notificationService.solve(
                profileData.assignee,
                ProfileDataInfo(profileData.internalSampleCode.trim, profileData.globalCode)
              )
            case _ =>
          }
          res
        }
      }
    }
  }

  override def saveElectropherograms(token: String, globalCode: SampleCode, idAnalysis: String, name: String): Future[List[Either[String, SampleCode]]] = {
    this.findByCode(globalCode).flatMap { p =>
      this.isReadOnly(p).flatMap {
        case (true, message) => Future.successful(List(Left(message)))
        case (false, _) =>
          cache.pop(TemporaryAssetKey(token)).map { imageList =>
            if (imageList.nonEmpty) {
              val f = imageList.map { imagePath =>
                val imageByteArray = Files.readAllBytes(Paths.get(imagePath.toURI))
                profileRepository.addElectropherogram(globalCode, idAnalysis, imageByteArray, name)
              }
              Future.sequence(f)
            } else {
              Future.successful(Nil)
            }
          }.getOrElse {
            Future.failed(new Exception(Messages("error.E0114")))
          }
      }
    }
  }

  override def findByCode(globalCode: SampleCode): Future[Option[Profile]] = {
    profileRepository.findByCode(globalCode)
  }

  override def get(id: SampleCode): Future[Option[Profile]] = {
    profileRepository.get(id)
  }

  override def getElectropherogramsByCode(globalCode: SampleCode): Future[List[FileUploadedType]] = {
    profileRepository.getElectropherogramsByCode(globalCode).map(lista => lista.map { case (efgId, _, name) => FileUploadedType(efgId, name) })
  }

  override def getElectropherogramImage(profileId: SampleCode, electropherogramId: String): Future[Option[Array[Byte]]] = {
    profileRepository.getElectropherogramImage(profileId, electropherogramId)
  }

  override def getElectropherogramsByAnalysisId(profileId: SampleCode, analysisId: String): Future[List[FileUploadedType]] = {
    profileRepository.getElectropherogramsByAnalysisId(profileId, analysisId)
  }

  def getProfileModelView(globalCode: SampleCode): Future[ProfileModelView] = {

    def getAnalyses(profile: Profile, imageList: List[(String, String, String)], fileList: List[(String, String, String)]) = {
      val analisysEfgsMap = imageList.groupBy { case (_, analisysId, _) => analisysId }
      val analisysFileMap = fileList.groupBy { case (_, analisysId, _) => analisysId }

      profile.analyses.map(analysesList => {
        analysesList.map(a => {
          val efgIds = analisysEfgsMap.get(a.id) match {
            case Some(lista) => lista.map { case (efgId, _, name) => FileUploadedType(efgId, name) }
            case _ => List()
          }
          val filesIds = analisysFileMap.get(a.id) match {
            case Some(lista) => lista.map { case (fileId, _, name) => FileUploadedType(fileId, name) }
            case _ => List()
          }
          AnalysisModelView(a.id, a.date.date, a.kit, a.genotypification, efgIds, a.`type`, filesIds)
        })
      })
    }

    val unexisting = ProfileModelView(None, None, None, None, List.empty, None, None, None, false, false, false, true, false, false)

    this.findProfileDataLocalOrSuperior(globalCode).flatMap { profileDataOpt =>
      profileDataOpt.fold(Future.successful(unexisting)) { profileData =>
        categoryService.getCategory(profileData.category).flatMap { categoryOpt =>
          val cat = categoryOpt.get
          val labelable = cat.associations.nonEmpty && !cat.isReference
          for {
            profileOpt <- this.findProfileLocalOrSuperior(globalCode)
            imageList <- profileRepository.getElectropherogramsByCode(globalCode)
            fileList <- profileRepository.getFileByCode(globalCode)
            readOnly <- this.isReadOnly2(profileOpt)
          } yield {
            profileOpt.fold {
              ProfileModelView(None, None, None, None, List.empty, None, None, None, false, labelable, true, cat.isReference, false, false)
            } { profile =>
              val associable = !(cat.associations.isEmpty || profileOpt.fold(false)(p => validProfilesAssociated(p.labeledGenotypification).nonEmpty))
              val editable = cat.associations.isEmpty || associable || labelable

              ProfileModelView(
                Some(profile.globalCode),
                Some(profile.globalCode),
                Some(profile.categoryId),
                Some(profile.genotypification),
                getAnalyses(profile, imageList, fileList).getOrElse(List.empty),
                profile.labeledGenotypification,
                profile.contributors,
                profile.matchingRules,
                associable, labelable, editable, profile.isReference, readOnly._1, readOnly._3
              )
            }
          }
        }
      }
    }
  }

  override def removeAll(): Future[Either[String, String]] = {
    this.profileRepository.removeAll()
  }

  override def removeProfile(globalCode: SampleCode): Future[Either[String, String]] = {
    this.profileRepository.removeProfile(globalCode)
    this.matchingRepository.removeMatchesByProfile(globalCode)
  }

  override def removeFile(id: String, user: String): Future[Either[String, String]] = {
    this.removeFileOrEpg(id, user, this.profileRepository.removeFile, this.profileRepository.getProfileOwnerByFileId)
  }

  override def removeEpg(id: String, user: String): Future[Either[String, String]] = {
    this.removeFileOrEpg(id, user, this.profileRepository.removeEpg, this.profileRepository.getProfileOwnerByEpgId)
  }

  private def removeFileOrEpg(
    id: String,
    user: String,
    removeFile: String => Future[Either[String, String]],
    getOwner: String => Future[(String, SampleCode)]
  ): Future[Either[String, String]] = {
    userService.isSuperUser(user).flatMap { isSuperUser =>
      getOwner(id).flatMap { case (assignee, globalCode) =>
        if (assignee == user || isSuperUser) {
          this.isReadOnlySampleCode(globalCode).flatMap {
            case (false, _) =>
              this.matchingRepo.numberOfMatches(globalCode.text).flatMap {
                case 0 => removeFile(id)
                case _ => Future.successful(Left("El perfil tiene matches"))
              }
            case (true, message) =>
              Future.successful(Left(message))
          }
        } else {
          Future.successful(Left("No es super usuario ni es dueño del perfil"))
        }
      }
    }
  }

  override def verifyMixtureAssociation(mixtureGenot: GenotypificationByType, globalCode: SampleCode, mixtureSubcategoryId: AlphanumericId): Future[Either[String, ProfileAsociation]] = {
    profileRepository.findByCode(globalCode).flatMap { profileOpt =>
      if (profileOpt.isEmpty) {
        Future.successful(Left(Messages("error.E0110")))
      } else {
        val profile = profileOpt.get
        val profMixture = new Profile(SampleCode("AR-M-MIX-1"), SampleCode("AR-C-QWER-1"), "INTERNAL", "HOLDER",
          mixtureSubcategoryId, mixtureGenot,
          None, None, None, None, None, None, false, true)

        categoryService.getCategory(profMixture.categoryId).map { categoryOpt =>
          val valAssocError = validateSubcategoriesAssociation(categoryOpt.get, profMixture, profile)
          if (valAssocError.isDefined) {
            Left(valAssocError.get)
          } else {
            validateMatching(categoryOpt.get, profMixture, profile)
          }
        }
      }
    }
  }

  def validateSubcategoriesAssociation(category: FullCategory, profMixture: Profile, p: Profile): Option[String] = {
    val associationRules = category.associations
    associationRules.exists(sc => sc.categoryRelated == p.categoryId) match {
      case false => Some(Messages("error.E0667", p.globalCode.text))
      case true => None
    }
  }

  def validateMatching(category: FullCategory, profileMixture: Profile, profileAssociation: Profile): Either[String, ProfileAsociation] = {
    val association = category.associations.find(_.categoryRelated == profileAssociation.categoryId).get
    matchingAlgorithmService.profileMatch(
      profileAssociation, profileMixture,
      MatchingRule(association.`type`, profileAssociation.categoryId, Stringency.LowStringency,
        true, true, Algorithm.ENFSI, 0, association.mismatches, true),
      None, 0
    ) match {
      case None => Left(Messages("error.E0115", profileAssociation.globalCode.text))
      case Some(mr) => Right(ProfileAsociation(profileAssociation.globalCode, mr.result.stringency, profileAssociation.genotypification.getOrElse(association.`type`, Map.empty)))
    }
  }

  private def labelIsProfile(labels: Profile.LabeledGenotypification): Option[SampleCode] = {
    Try(SampleCode(labels.head._1)).toOption
  }

  override def validProfilesAssociated(labels: Option[Profile.LabeledGenotypification]): Seq[String] = {
    matchingService.validProfilesAssociated(labels)
  }

  override def saveLabels(globalCode: SampleCode, labeledGenotypification: Profile.LabeledGenotypification, userId: String): Future[Either[List[String], SampleCode]] = {
    val labelsFut = filterLabeledGenotypification(labeledGenotypification)
    val profileFut = findByCode(globalCode)

    val fut = for {
      labels <- labelsFut
      p <- profileFut
    } yield {
      this.isReadOnly(p).flatMap {
        case (true, message) => Future.successful(Left(List(message)))
        case (false, _) =>
          val profile = p.get
          categoryService.getCategory(profile.categoryId).flatMap { categoryOpt =>
            val category = categoryOpt.get

            if (validProfilesAssociated(profile.labeledGenotypification).nonEmpty) {
              Future.successful(Left(List(Messages("error.E0116"))))
            } else {
              def saveLabelsCore = profileRepository.saveLabels(globalCode, labels).map { id =>
                matchingService.findMatches(globalCode, categoryService.getCategoryTypeFromFullCategory(category))
                notificationService.solve(profile.assignee, ProfileDataInfo(profile.internalSampleCode, profile.globalCode))
                notificationService.solve(profile.assignee, ProfileDataAssociationInfo(profile.internalSampleCode, profile.globalCode))
                Right(id)
              }

              labelIsProfile(labels).fold(saveLabelsCore)(profileRepository.updateAssocTo(_, globalCode).flatMap {
                case (assignee, isc, gc) =>
                  traceService.add(Trace(globalCode, userId, new Date(), AssociationInfo(gc, assignee, category.associations)))
                  traceService.add(Trace(gc, userId, new Date(), AssociationInfo(globalCode, profile.assignee, category.associations)))
                  notificationService.solve(assignee, ProfileDataInfo(isc, gc))
                  notificationService.solve(assignee, ProfileDataAssociationInfo(isc, gc))
                  saveLabelsCore
              })
            }
          }
      }
    }
    fut.flatMap(identity)
  }

  override def findByCodes(globalCodes: List[SampleCode]): Future[Seq[Profile]] = {
    profileRepository.findByCodes(globalCodes)
  }

  override def getLabels(globalCode: SampleCode): Future[Option[Profile.LabeledGenotypification]] = {
    profileRepository.getLabels(globalCode)
  }

  override def getAssociatedProfiles(profile: Profile): Future[Seq[Profile]] = {
    val validCodes = validProfilesAssociated(profile.labeledGenotypification)
    if (validCodes.nonEmpty) {
      findByCodes(validCodes.map(SampleCode(_)).toList)
    } else { Future(Nil) }
  }

  override def getLabelsSets(): Profile.LabelSets = labelsSet

  private def filterLabeledGenotypification(labeledGenotypification: Profile.LabeledGenotypification): Future[Profile.LabeledGenotypification] = {
    Future.sequence(labeledGenotypification.map { case (label, genotypification) =>
      filterGenotypificationByTypeName(genotypification, "Autosomal").map(filtered => (label, filtered))
    }.toList).map(list => list.toMap)
  }

  private def filterGenotypificationByTypeName(genotypification: Profile.Genotypification, analysisType: String): Future[Profile.Genotypification] = {
    locusService.getLocusByAnalysisTypeName(analysisType).map { validLocus =>
      genotypification.view.filterKeys(validLocus.contains(_)).toMap
    }
  }

  def addProfile(profile: Profile): Future[SampleCode] = {
    profileRepository.add(profile)
  }

  def fireMatching(sampleCode: SampleCode): Unit = {
    matchingService.findMatches(sampleCode, None)
  }

  def updateProfile(profile: Profile): Future[SampleCode] = {
    profileRepository.updateProfile(profile)
  }

  def findProfileLocalOrSuperior(globalCode: SampleCode): Future[Option[Profile]] = {
    profileRepository.findByCode(globalCode).flatMap {
      case None => matchingRepo.findSuperiorProfile(globalCode)
      case Some(p) => Future.successful(Some(p))
    }
  }

  def findProfileDataLocalOrSuperior(globalCode: SampleCode): Future[Option[ProfileData]] = {
    profileDataRepository.findByCode(globalCode).flatMap {
      case None => matchingRepo.findSuperiorProfileData(globalCode)
      case Some(p) => Future.successful(Some(p))
    }
  }

  override def saveFile(token: String, globalCode: SampleCode, idAnalysis: String, name: String): Future[List[Either[String, SampleCode]]] = {
    this.findByCode(globalCode).flatMap { p =>
      this.isReadOnly(p).flatMap {
        case (true, message) => Future.successful(List(Left(message)))
        case (false, _) =>
          cache.pop(TemporaryAssetKey(token)).map { imageList =>
            if (imageList.nonEmpty) {
              val f = imageList.map { imagePath =>
                val imageByteArray = Files.readAllBytes(Paths.get(imagePath.toURI))
                profileRepository.addFile(globalCode, idAnalysis, imageByteArray, name)
              }
              Future.sequence(f)
            } else {
              Future.successful(Nil)
            }
          }.getOrElse {
            Future.failed(new Exception(Messages("error.E0114")))
          }
      }
    }
  }

  override def getFilesByCode(globalCode: SampleCode): Future[List[FileUploadedType]] = {
    profileRepository.getFileByCode(globalCode).map(lista => lista.map { case (efgId, _, name) => FileUploadedType(efgId, name) })
  }

  override def getFile(profileId: SampleCode, electropherogramId: String): Future[Option[Array[Byte]]] = {
    profileRepository.getFile(profileId, electropherogramId)
  }

  override def getFilesByAnalysisId(profileId: SampleCode, analysisId: String): Future[List[FileUploadedType]] = {
    profileRepository.getFileByAnalysisId(profileId, analysisId)
  }

  override def getFullElectropherogramsByCode(globalCode: SampleCode): Future[List[connections.FileInterconnection]] = {
    this.profileRepository.getFullElectropherogramsByCode(globalCode)
  }

  override def getFullFilesByCode(globalCode: SampleCode): Future[List[connections.FileInterconnection]] = {
    this.profileRepository.getFullFilesByCode(globalCode)
  }

  override def addElectropherogramWithId(globalCode: SampleCode, analysisId: String, image: Array[Byte], name: String, id: String): Future[Either[String, SampleCode]] = {
    this.profileRepository.addElectropherogramWithId(globalCode, analysisId, image, name, id)
  }

  override def addFileWithId(globalCode: SampleCode, analysisId: String, image: Array[Byte], name: String, id: String): Future[Either[String, SampleCode]] = {
    this.profileRepository.addFileWithId(globalCode, analysisId, image, name, id)
  }

  override def getFullElectropherogramsById(id: String): Future[List[connections.FileInterconnection]] = {
    this.profileRepository.getFullElectropherogramsById(id)
  }

  override def getFullFilesById(id: String): Future[List[connections.FileInterconnection]] = {
    this.profileRepository.getFullFilesById(id)
  }

  override def profilesAll(): Future[List[(SampleCode, String)]] = {
    this.profileRepository.getAllProfiles()
  }
}
