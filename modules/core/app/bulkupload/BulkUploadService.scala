package bulkupload

import configdata.{CategoryRepository, CategoryService, FullCategory, MatchingRule}
import connections.InterconnectionService
import inbox.{BulkUploadInfo, NotificationService, ProfileDataAssociationInfo}
import jakarta.inject.{Inject, Named, Singleton}
import kits.StrKitService
import play.api.Logging
import play.api.i18n.{Lang, MessagesApi}
import play.api.libs.Files.TemporaryFile
import profile.{NewAnalysis, Profile, ProfileService}
import profiledata.*
import search.PaginationSearch
import services.{CacheService, UserService}
import types.{AlphanumericId, SampleCode}
import user.UserView

import java.io.File
import java.util.Calendar
import scala.concurrent.{ExecutionContext, Future}

trait BulkUploadService:
  def getBatchesStep1(userId: String, isSuperUser: Boolean, offset: Int, limit: Int): Future[Seq[ProtoProfilesBatchView]]
  def countBatchesStep1(userId: String, isSuperUser: Boolean): Future[Int]
  def getBatchesStep2(userId: String, geneMapperId: String, isSuperUser: Boolean, offset: Int, limit: Int): Future[Seq[ProtoProfilesBatchView]]
  def countBatchesStep2(userId: String, geneMapperId: String, isSuperUser: Boolean): Future[Int]
  def countAllProtoProfilesInBatch(batchId: Long): Future[Int]
  def getProtoProfile(id: Long): Future[Option[ProtoProfile]]
  def getProtoProfileWithBatchId(id: Long): Future[Option[(ProtoProfile, Long)]]
  def getProtoProfilesStep1(batchId: Long, paginationSearch: Option[PaginationSearch]): Future[Seq[ProtoProfile]]
  def getProtoProfilesStep2(batchId: Long, geneMapperId: String, isSuperUser: Boolean, paginationSearch: Option[PaginationSearch] = None): Future[Seq[ProtoProfile]]
  def uploadProtoProfiles(user: String, csvFile: TemporaryFile, label: Option[String], analysisType: String): Future[Either[String, Long]]
  def rejectProtoProfile(id: Long, motive: String, userId: String, idMotive: Long): Future[Seq[String]]
  def updateProtoProfileStatus(id: Long, status: ProtoProfileStatus.Value, userId: String, replicate: Boolean, desktopSearch: Boolean): Future[Seq[String]]
  def updateProtoProfileData(id: Long, category: AlphanumericId, userId: String): Future[Either[Seq[String], ProtoProfile]]
  def updateBatchStatus(idBatch: Long, status: ProtoProfileStatus.Value, userId: String, isSuperUser: Boolean, replicateAll: Boolean, idsToReplicate: List[Long]): Future[Either[String, Long]]
  def updateProtoProfileRulesMismatch(id: Long, matchingRules: Seq[MatchingRule], mismatches: Profile.Mismatch): Future[Boolean]
  def deleteBatch(id: Long): Future[Either[String, Long]]
  def searchBatch(userId: String, isSuperUser: Boolean, search: String): Future[Seq[ProtoProfilesBatchView]]
  def getBatchSearchModalViewByIdOrLabel(input: String, idCase: Long): Future[List[BatchModelView]]
  protected def replicateProtoProfile(protoProfile: ProtoProfile, userId: String): Future[Seq[String]]

@Singleton
class BulkUploadServiceImpl @Inject()(
    val protoRepo: ProtoProfileRepository,
    val userService: UserService,
    val kitService: StrKitService,
    val categoryRepo: CategoryRepository,
    profileService: ProfileService,
    @Named("stashed") protoProfiledataService: ProfileDataService,
    profileDataRepo: ProfileDataRepository,
    notificationService: NotificationService,
    importToProfileData: ImportToProfileData,
    @Named("labCode") val labCode: String,
    @Named("country") val country: String,
    @Named("province") val province: String,
    @Named("protoProfileGcDummy") val ppGcD: String,
    categoryService: CategoryService,
    cache: CacheService,
    interconnectionService: InterconnectionService,
    messagesApi: MessagesApi
)(implicit ec: ExecutionContext) extends BulkUploadService with Logging:

  private val messages = messagesApi.preferred(Seq(Lang("es")))

  private val allowTransition = (a: ProtoProfileStatus.Value, b: ProtoProfileStatus.Value) => (a, b) match
    case (ProtoProfileStatus.Incomplete, ProtoProfileStatus.ReadyForApproval)           => true
    case (ProtoProfileStatus.ReadyForApproval, ProtoProfileStatus.ReadyForApproval)     => true
    case (ProtoProfileStatus.Incomplete, ProtoProfileStatus.Disapproved)                => true
    case (ProtoProfileStatus.ReadyForApproval, ProtoProfileStatus.Approved)             => true
    case (ProtoProfileStatus.ReadyForApproval, ProtoProfileStatus.Disapproved)          => true
    case (ProtoProfileStatus.Approved, ProtoProfileStatus.Imported)                     => true
    case (ProtoProfileStatus.Approved, ProtoProfileStatus.DesktopSearch)                => true
    case (ProtoProfileStatus.Approved, ProtoProfileStatus.Rejected)                     => true
    case (ProtoProfileStatus.Imported, ProtoProfileStatus.Uploaded)                     => true
    case (ProtoProfileStatus.Uploaded, ProtoProfileStatus.Imported)                     => true
    case (ProtoProfileStatus.Imported, ProtoProfileStatus.ReplicatedMatchingProfile)    => true
    case (ProtoProfileStatus.Uploaded, ProtoProfileStatus.ReplicatedMatchingProfile)    => true
    case (ProtoProfileStatus.ReplicatedMatchingProfile, ProtoProfileStatus.Uploaded)    => true
    case (_, _)                                                                          => false

  override def getProtoProfilesStep1(batchId: Long, paginationSearch: Option[PaginationSearch] = None): Future[Seq[ProtoProfile]] =
    protoRepo.getProtoProfilesStep1(batchId, paginationSearch)

  override def getProtoProfilesStep2(batchId: Long, geneMapperId: String, isSuperUser: Boolean, paginationSearch: Option[PaginationSearch] = None): Future[Seq[ProtoProfile]] =
    protoRepo.getProtoProfilesStep2(batchId, geneMapperId, isSuperUser, paginationSearch).flatMap { profiles =>
      Future.sequence(profiles.map { profile =>
        if profile.status == ProtoProfileStatus.Imported then
          interconnectionService.isUplpoadableInternalCode(profile.sampleName).map { isReplicable =>
            if !isReplicable then profile.copy(status = ProtoProfileStatus.ReplicatedMatchingProfile)
            else profile
          }
        else
          Future.successful(profile)
      })
    }

  override def getProtoProfile(id: Long): Future[Option[ProtoProfile]] =
    protoRepo.getProtoProfile(id)

  override def getProtoProfileWithBatchId(id: Long): Future[Option[(ProtoProfile, Long)]] =
    protoRepo.getProtoProfileWithBatchId(id)

  override def getBatchesStep1(userId: String, isSuperUser: Boolean, offset: Int, limit: Int): Future[Seq[ProtoProfilesBatchView]] =
    protoRepo.getBatchesStep1(userId, isSuperUser, offset, limit)

  override def countBatchesStep1(userId: String, isSuperUser: Boolean): Future[Int] =
    protoRepo.countBatchesStep1(userId, isSuperUser)

  override def getBatchesStep2(userId: String, geneMapperId: String, isSuperUser: Boolean, offset: Int, limit: Int): Future[Seq[ProtoProfilesBatchView]] =
    protoRepo.getBatchesStep2(userId, geneMapperId, isSuperUser, offset, limit)

  override def countBatchesStep2(userId: String, geneMapperId: String, isSuperUser: Boolean): Future[Int] =
    protoRepo.countBatchesStep2(userId, geneMapperId, isSuperUser)

  override def countAllProtoProfilesInBatch(batchId: Long): Future[Int] =
    protoRepo.countAllProtoProfilesInBatch(batchId)

  override def uploadProtoProfiles(user: String, tempFile: TemporaryFile, label: Option[String], analysisType: String): Future[Either[String, Long]] =
    given play.api.i18n.Messages = messages
    val aliasKits  = kitService.getKitAlias
    val lociAlias  = kitService.getLocusAlias
    val kitsF = kitService.list().flatMap { kits =>
      Future.sequence(kits.map { kit =>
        kitService.findLociByKit(kit.id).map(loci => kit.id -> loci.map(_.id))
      }).map(_.toMap)
    }
    val categoryAliasesF = categoryService.listCategories.map { cats =>
      cats.flatMap { case (id, category) =>
        category.aliases.map { _ -> id } :+ (id.text -> id)
      }
    }
    val validatorF = for
      geneticists      <- userService.findUserAssignable.map(_.map(u =>
                           UserView(u.id, u.firstName, u.lastName, u.email, u.roles, u.status, u.geneMapperId, u.phone1, u.phone2, u.superuser)).toList)
      kits             <- kitsF
      alKit            <- aliasKits
      lociAl           <- lociAlias
      categoryAliases  <- categoryAliasesF
    yield
      Validator(
        protoRepo,
        kits.map((k, v) => k.toLowerCase -> v),
        alKit.map((k, v) => k.toLowerCase -> v),
        lociAl.map((k, v) => k.toLowerCase -> v),
        geneticists,
        categoryAliases.map((k, v) => k -> v).toMap,
        messages
      )
    protoProfiledataService.getMtRcrs().flatMap { mtRcrs =>
      validatorF.flatMap { validator =>
        val csvFile = new File(tempFile.file.getAbsolutePath + "_permanent")
        tempFile.moveTo(csvFile.toPath, replace = true)
        // Parsing may throw synchronously (e.g. IndexOutOfBoundsException). Capture it into the
        // Future so the _permanent temp file (forensic genetic data) is removed on EVERY path —
        // parser Left, sync throw, downstream failure and success — not only the success branch.
        val resultF: Future[Either[String, Long]] = scala.util.Try(
          if analysisType == "Autosomal" then
            GeneMapperFileParser.parse(csvFile, validator)
          else
            GeneMapperFileMitoParser.parse(csvFile, validator, mtRcrs)
        ).fold(
          e => Future.failed(e),
          _.fold[Future[Either[String, Long]]](
            error => {
              logger.error(error)
              Future.successful(Left(error))
            },
            stream => {
              val kits = stream.map(_.kit).distinct.toSeq
              kitService.findLociByKits(kits).flatMap { kits =>
                protoRepo.createBatch(user, stream, labCode, kits, label, analysisType).map(Right(_))
              }
            }
          )
        )
        // delete only after createBatch has fully consumed the lazy stream (Future completed).
        resultF.andThen { case _ => csvFile.delete() }
      }
    }.recover {
      case _: IndexOutOfBoundsException =>
        logger.error(messages("error.E0302"))
        Left(messages("error.E0302"))
      case error: KitNotExistsException =>
        val errorMessage = messages("error.E0316", error.getMessage)
        logger.error(errorMessage)
        Left(errorMessage)
      case error =>
        logger.error(error.getMessage)
        Left(messages("error.E0301"))
    }

  private def updateStatus(id: Long, status: ProtoProfileStatus.Value): Future[Seq[String]] =
    protoRepo.updateProtoProfileStatus(id, status).map { count =>
      if count == 1 then Nil
      else Seq(messages("error.E0100", count))
    }

  override protected def replicateProtoProfile(protoProfile: ProtoProfile, userId: String): Future[Seq[String]] =
    profileDataRepo.getGlobalCode(protoProfile.sampleName).flatMap {
      _.fold(Future.successful(Seq(messages("error.E0101")))) { case SampleCode(globalCode) =>
        interconnectionService.uploadProfile(globalCode, userId).map {
          case Right(_)      => Seq("Success")
          case Left(error)   =>
            logger.error(s"Failed to upload profile: $error")
            Seq(messages("error.E0731", error))
        }
      }
    }

  private def importLinkedProtoProfile(protoProfile: ProtoProfile, userId: String, replicate: Boolean = false): Future[Seq[String]] =
    profileDataRepo.getGlobalCode(protoProfile.sampleName).flatMap {
      _.fold[Future[Seq[String]]](Future.successful(Seq(messages("error.E0101")))) { sampleCode =>
        val analysis = NewAnalysis(
          sampleCode, userId, null,
          Some(protoProfile.kit), None,
          protoProfile.genotypification.map(g => g.locus -> g.alleles).toMap,
          None, None,
          Option(protoProfile.mismatches),
          Option(protoProfile.matchingRules))
        profileService.create(analysis, savePictures = false, replicate = replicate).map {
          case Left(err)  => err
          case Right(_)   => Nil
        }
      }
    }

  private def importToProfile(protoProfile: ProtoProfile, assignee: String, userId: String, replicate: Boolean = false, desktopSearch: Boolean = false): Future[Seq[String]] =
    importToProfileData.fromProtoProfileData(protoProfile.id, labCode, country, province, assignee, desktopSearch).flatMap { case (sampleCode, labo) =>
      val analysis = NewAnalysis(
        sampleCode, userId, null,
        Some(protoProfile.kit), None,
        protoProfile.genotypification.map(g => g.locus -> g.alleles).toMap,
        None, None,
        Option(protoProfile.mismatches),
        Option(protoProfile.matchingRules))
      val pd: ProfileData = protoProfile.protoProfileData.fold(
        ProfileData(
          category = AlphanumericId(protoProfile.category),
          globalCode = sampleCode,
          attorney = None, bioMaterialType = None, court = None,
          crimeInvolved = None, crimeType = None, criminalCase = None,
          internalSampleCode = protoProfile.sampleName, assignee = assignee,
          laboratory = labo, deleted = false, deletedMotive = None,
          responsibleGeneticist = None, profileExpirationDate = None,
          sampleDate = None, sampleEntryDate = None, dataFiliation = None,
          isExternal = false)
      )(_.pdAttempToPd(labo))
      profileService.importProfile(pd, analysis, replicate).flatMap {
        case Left(list) =>
          importToProfileData.deleteProfileData(sampleCode.text).map(_ => list)
        case Right(_) =>
          categoryService.listCategories.map { cats =>
            if cats.get(AlphanumericId(protoProfile.category)).exists(_.associations.nonEmpty) then
              notificationService.push(assignee, ProfileDataAssociationInfo(protoProfile.sampleName, sampleCode))
              userService.sendNotifToAllSuperUsers(ProfileDataAssociationInfo(protoProfile.sampleName, sampleCode), Seq(assignee))
            Nil
          }
      }.recoverWith {
        case e =>
          importToProfileData.deleteProfileData(sampleCode.text).map { _ =>
            logger.error(protoProfile.sampleName, e)
            Seq(messages("error.E0303"))
          }
      }
    }

  override def rejectProtoProfile(id: Long, motive: String, userId: String, idMotive: Long): Future[Seq[String]] =
    updateProtoProfileStatus(id, ProtoProfileStatus.Rejected, userId).flatMap { errors =>
      if errors.isEmpty then
        protoRepo.setRejectMotive(id, motive, userId, idMotive,
          new java.sql.Timestamp(Calendar.getInstance().getTime.getTime)).map { count =>
          if count == 1 then Nil
          else Seq(messages("error.E0102"))
        }
      else
        Future.successful(errors)
    }

  override def updateBatchStatus(idBatch: Long, status: ProtoProfileStatus.Value, userId: String, isSuperUser: Boolean, replicateAll: Boolean, idsToReplicate: List[Long]): Future[Either[String, Long]] =
    userService.listAllUsers().flatMap { users =>
      users.find(_.userName == userId).fold(Future.successful(Left(messages("error.E0200", userId)): Either[String, Long])) { loggedUser =>
      val profilesF =
        if status == ProtoProfileStatus.Imported || status == ProtoProfileStatus.Uploaded || status == ProtoProfileStatus.ReplicatedMatchingProfile then
          protoRepo.getProtoProfilesStep2(idBatch, loggedUser.geneMapperId, loggedUser.superuser)
        else
          protoRepo.getProtoProfilesStep1(idBatch)
      profilesF.flatMap { protoProfiles =>
        Future.sequence(protoProfiles.map { protoProfile =>
          val geneticistOpt = users.find(_.geneMapperId == protoProfile.assignee)
          val errorMessage  = () => Future.successful(Seq(messages("error.E0200", protoProfile.assignee)))
          val performTransition = (geneticist: UserView) =>
            val replicateF: Future[Boolean] =
              if replicateAll then
                categoryService.getCategory(AlphanumericId(protoProfile.category)).map(_.exists(_.replicate))
              else
                Future.successful(idsToReplicate.contains(protoProfile.id))
            replicateF.flatMap { replicate =>
              if (status == ProtoProfileStatus.Imported || status == ProtoProfileStatus.Approved) && !idsToReplicate.contains(protoProfile.id) then
                if allowTransition(protoProfile.status, status) then
                  transitionStatus(status, protoProfile, geneticist.userName, userId, replicate)
                else
                  Future.successful(Seq())
              else if replicate && idsToReplicate.contains(protoProfile.id) then
                if allowTransition(protoProfile.status, status) then
                  transitionStatus(status, protoProfile, geneticist.userName, userId, replicate = true)
                else
                  Future.successful(Seq())
              else
                Future.successful(Seq())
            }
          geneticistOpt.fold(errorMessage())(performTransition)
        })
      }.map { sequences =>
        if sequences.exists(_.contains("Success")) then
          Right(idBatch)
        else if sequences.exists(_.nonEmpty) then
          val joinedMessage = (sequences.flatten ++ Seq(messages("error.E0103")))
            .distinct.map(msg => s"<br>$msg").mkString("")
          Left(joinedMessage)
        else
          Right(idBatch)
      }
      } // close fold Some-branch
    }

  private def transitionStatus(status: ProtoProfileStatus.Value, protoProfile: ProtoProfile, assignee: String, userId: String, replicate: Boolean = false, desktopSearch: Boolean = false): Future[Seq[String]] =
    if allowTransition(protoProfile.status, status) then
      status match
        case ProtoProfileStatus.Uploaded =>
          val perform = replicateProtoProfile(protoProfile, userId)
          updateStatus(protoProfile.id, status)
          perform.flatMap { errors =>
            if errors.isEmpty then
              notificationService.solve(assignee, BulkUploadInfo(protoProfile.id.toString, protoProfile.sampleName))
              updateStatus(protoProfile.id, status).map { res =>
                if res.isEmpty then
                  userService.sendNotifToAllSuperUsers(BulkUploadInfo(protoProfile.id.toString, protoProfile.sampleName), Seq(assignee))
                res
              }
            else
              val replicationErrors = errors.map(error =>
                if error.contains("replication") then messages("error.E0731") else error)
              Future.successful(replicationErrors)
          }
        case ProtoProfileStatus.Imported | ProtoProfileStatus.DesktopSearch =>
          val res =
            if protoProfile.preexistence.isDefined then importLinkedProtoProfile(protoProfile, userId, replicate)
            else importToProfile(protoProfile, assignee, userId, replicate, desktopSearch)
          res.flatMap { errors =>
            if errors.isEmpty then
              notificationService.solve(assignee, BulkUploadInfo(protoProfile.id.toString, protoProfile.sampleName))
              updateStatus(protoProfile.id, status)
            else
              Future.successful(errors)
          }
        case ProtoProfileStatus.Approved =>
          updateStatus(protoProfile.id, status).map { x =>
            if x.isEmpty then
              notificationService.push(assignee, BulkUploadInfo(protoProfile.id.toString, protoProfile.sampleName))
            userService.sendNotifToAllSuperUsers(BulkUploadInfo(protoProfile.id.toString, protoProfile.sampleName), Seq(assignee))
            x
          }
        case ProtoProfileStatus.Disapproved =>
          updateStatus(protoProfile.id, status)
        case ProtoProfileStatus.Rejected =>
          val us = updateStatus(protoProfile.id, status)
          us.foreach { list =>
            if list.isEmpty then
              notificationService.solve(assignee, BulkUploadInfo(protoProfile.id.toString, protoProfile.sampleName))
          }
          us
        case _ => updateStatus(protoProfile.id, status)
    else
      Future.successful(Seq(messages("error.E0104", protoProfile.status, status)))

  override def updateProtoProfileStatus(id: Long, status: ProtoProfileStatus.Value, userId: String, replicate: Boolean = false, desktopSearch: Boolean = false): Future[Seq[String]] =
    val effectiveStatus =
      if status == ProtoProfileStatus.Imported && desktopSearch then ProtoProfileStatus.DesktopSearch
      else status
    protoRepo.getProtoProfile(id).flatMap { protoProfileOpt =>
      val error105 = Future.successful(Seq(messages("error.E0105", id)))
      val genError200 = (assignee: String) => Future.successful(Seq(messages("error.E0200", assignee)))
      val transitionWithGeneticist = (protoProfile: ProtoProfile) => (g: UserView) =>
        transitionStatus(effectiveStatus, protoProfile, g.userName, userId, replicate, desktopSearch)
      val transitionate = (protoProfile: ProtoProfile) =>
        userService.findByGeneMapper(protoProfile.assignee).flatMap(
          _.fold(genError200(protoProfile.assignee))(transitionWithGeneticist(protoProfile))
        )
      protoProfileOpt.fold(error105)(transitionate)
    }

  override def updateProtoProfileRulesMismatch(id: Long, matchingRules: Seq[MatchingRule], mismatches: Profile.Mismatch): Future[Boolean] =
    protoRepo.updateProtoProfileMatchingRulesMismatch(id, matchingRules, mismatches).map(_ == 1)

  override def updateProtoProfileData(id: Long, category: AlphanumericId, userId: String): Future[Either[Seq[String], ProtoProfile]] =
    protoRepo.getProtoProfile(id).flatMap { protoProfileOpt =>
      categoryService.listCategories.flatMap { cats =>
        cats.get(category) match
          case None =>
            Future.successful(Left(Seq(messages("error.E0600", category.text))))
          case Some(fullCategory) =>
            protoProfileOpt match
              case None =>
                Future.successful(Left(Seq(messages("error.E0900", id))))
              case Some(protoProfile) =>
                protoProfile.preexistence.fold[Future[Option[String]]](Future.successful(None))(gc =>
                  protoRepo.validateAssigneAndCategory(gc, protoProfile.assignee, Some(fullCategory.id))
                ).flatMap {
                  _.fold(
                    protoRepo.updateProtoProfileData(id, fullCategory.id).flatMap {
                      case 1 =>
                        if ProtoProfileStatus.Approved == protoProfile.status then
                          Future.successful(Right(protoProfile.copy(category = fullCategory.id.text)))
                        else
                          updateProtoProfileStatus(id, ProtoProfileStatus.ReadyForApproval, userId).flatMap {
                            case Nil => getProtoProfile(id).map(optPPp => Right(optPPp.get))
                            case err => Future.successful(Left(err))
                          }
                      case count =>
                        Future.successful(Left(Seq(messages("error.E0660", count))))
                    }
                  )(ss => Future.successful(Left(Seq(ss))))
                }
      }
    }

  override def deleteBatch(id: Long): Future[Either[String, Long]] =
    protoRepo.countImportedProfilesByBatch(id).flatMap {
      _.fold(
        msg => Future.successful(Left(msg)),
        count =>
          if count > 0 then Future.successful(Left(messages("error.E0304")))
          else protoRepo.deleteBatch(id)
      )
    }

  override def searchBatch(userId: String, isSuperUser: Boolean, search: String): Future[Seq[ProtoProfilesBatchView]] =
    protoRepo.getSearchBachLabelID(userId, isSuperUser, search)

  override def getBatchSearchModalViewByIdOrLabel(input: String, idCase: Long): Future[List[BatchModelView]] =
    if input.isEmpty then Future.successful(Nil)
    else protoRepo.getBatchSearchModalViewByIdOrLabel(input, idCase)
