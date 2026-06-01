package bulkupload

import java.io.File
import java.util.Calendar

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, ExecutionContext, Future}

import javax.inject.{Inject, Named, Singleton}

import configdata.{CategoryService, MatchingRule}
import connections.InterconnectionService
import inbox.{BulkUploadInfo, NotificationService, ProfileDataAssociationInfo}
import kits.StrKitService
import play.api.Logger
import play.api.libs.Files.TemporaryFile
import profile.{NewAnalysis, Profile, ProfileService}
import profiledata.{ImportToProfileData, ProfileDataService}
import search.PaginationSearch
import services.UserService
import types.{AlphanumericId, SampleCode}
import user.UserView

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

@Singleton
class BulkUploadServiceImpl @Inject() (
  protoRepo: ProtoProfileRepository,
  userService: UserService,
  kitService: StrKitService,
  categoryService: CategoryService,
  profileService: ProfileService,
  profileDataService: ProfileDataService,
  notificationService: NotificationService,
  importToProfileData: ImportToProfileData,
  interconnectionService: InterconnectionService,
  @Named("labCode") labCode: String,
  @Named("country") country: String,
  @Named("province") province: String,
  @Named("protoProfileGcDummy") ppGcD: String
)(using ec: ExecutionContext) extends BulkUploadService:

  private val logger = Logger(this.getClass)

  private implicit def stringToAlphanumericId(s: String): AlphanumericId = AlphanumericId(s)

  private val allowTransition: (ProtoProfileStatus.Value, ProtoProfileStatus.Value) => Boolean = {
    case (ProtoProfileStatus.Incomplete,               ProtoProfileStatus.ReadyForApproval) => true
    case (ProtoProfileStatus.ReadyForApproval,         ProtoProfileStatus.ReadyForApproval) => true
    case (ProtoProfileStatus.Incomplete,               ProtoProfileStatus.Disapproved)      => true
    case (ProtoProfileStatus.ReadyForApproval,         ProtoProfileStatus.Approved)         => true
    case (ProtoProfileStatus.ReadyForApproval,         ProtoProfileStatus.Disapproved)      => true
    case (ProtoProfileStatus.Approved,                 ProtoProfileStatus.Imported)         => true
    case (ProtoProfileStatus.Approved,                 ProtoProfileStatus.DesktopSearch)    => true
    case (ProtoProfileStatus.Approved,                 ProtoProfileStatus.Rejected)         => true
    case (ProtoProfileStatus.Imported,                 ProtoProfileStatus.Uploaded)         => true
    case (ProtoProfileStatus.Uploaded,                 ProtoProfileStatus.Imported)         => true
    case (ProtoProfileStatus.Imported,                 ProtoProfileStatus.ReplicatedMatchingProfile) => true
    case (ProtoProfileStatus.Uploaded,                 ProtoProfileStatus.ReplicatedMatchingProfile) => true
    case (ProtoProfileStatus.ReplicatedMatchingProfile,ProtoProfileStatus.Uploaded)         => true
    case _                                                                                  => false
  }

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
        else Future.successful(profile)
      })
    }

  override def getProtoProfile(id: Long): Future[Option[ProtoProfile]] = protoRepo.getProtoProfile(id)

  override def getProtoProfileWithBatchId(id: Long): Future[Option[(ProtoProfile, Long)]] = protoRepo.getProtoProfileWithBatchId(id)

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
    val aliasKitsF    = kitService.getKitAlias
    val lociAliasF    = kitService.getLocusAlias
    val kitsF = kitService.list().flatMap { kits =>
      Future.sequence(kits.map { kit =>
        kitService.findLociByKit(kit.id).map(loci => kit.id -> loci.map(_.id))
      }).map(_.toMap)
    }
    val categoryAliasesF = categoryService.listCategories.map { categories =>
      categories.flatMap { case (id, cat) =>
        cat.aliases.map(_ -> id) :+ (id.text -> id)
      }
    }
    val validatorF = for {
      geneticists     <- userService.findUserAssignable
      kits            <- kitsF
      alKit           <- aliasKitsF
      lociAl          <- lociAliasF
      categoryAliases <- categoryAliasesF
    } yield Validator(
      protoRepo,
      kits.map  { case (k, v) => k.toLowerCase -> v },
      alKit.map { case (k, v) => k.toLowerCase -> v },
      lociAl.map{ case (k, v) => k.toLowerCase -> v },
      geneticists.toList,
      categoryAliases
    )

    profileDataService.getMtRcrs().flatMap { mtRcrs =>
      validatorF.flatMap { validator =>
        val csvFile = new File(tempFile.path.toAbsolutePath.toString + "_permanent")
        tempFile.moveTo(csvFile.toPath, replace = true)
        val either =
          if analysisType == "Autosomal" then GeneMapperFileParser.parse(csvFile, validator)
          else GeneMapperFileMitoParser.parse(csvFile, validator, mtRcrs)

        either.fold[Future[Either[String, Long]]](
          error => {
            logger.error(error)
            Future.successful(Left(error))
          },
          stream => {
            val kits = stream.map(_.kit).distinct.toSeq
            kitService.findLociByKits(kits).flatMap { kitsWithLoci =>
              val batchIdF = protoRepo.createBatch(user, stream, labCode, kitsWithLoci, label, analysisType)
              batchIdF.foreach(_ => csvFile.delete())
              batchIdF.map(Right(_))
            }
          }
        )
      }
    }.recover {
      case _: IndexOutOfBoundsException     => Left("error.E0302")
      case e: KitNotExistsException         => Left(s"error.E0316 kit=${e.getMessage}")
      case e                                => logger.error(e.getMessage); Left("error.E0301")
    }

  private def updateStatus(id: Long, status: ProtoProfileStatus.Value): Future[Seq[String]] =
    protoRepo.updateProtoProfileStatus(id, status).map { count =>
      if count == 1 then Nil else Seq(s"error.E0100 count=$count")
    }

  private def replicateProtoProfile(protoProfile: ProtoProfile, userId: String): Future[Seq[String]] =
    profileDataService.getGlobalCode(protoProfile.sampleName).flatMap {
      case None => Future.successful(Seq("error.E0101"))
      case Some(SampleCode(globalCode)) =>
        interconnectionService.uploadProfile(globalCode, userId).map {
          case Right(_)    => Seq("Success")
          case Left(error) => Seq(s"error.E0731 detail=$error")
        }
    }

  private def importLinkedProtoProfile(protoProfile: ProtoProfile, userId: String, replicate: Boolean = false): Future[Seq[String]] =
    profileDataService.getGlobalCode(protoProfile.sampleName).flatMap {
      case None => Future.successful(Seq("error.E0101"))
      case Some(sampleCode) =>
        val analysis = NewAnalysis(
          sampleCode, userId, null,
          Some(protoProfile.kit), None,
          protoProfile.genotypification.map(g => g.locus -> g.alleles).toMap,
          None, None,
          Some(protoProfile.mismatches),
          Some(protoProfile.matchingRules))
        profileService.create(analysis, savePictures = false, replicate).map {
          case Left(err) => err
          case Right(_)  => Nil
        }
    }

  private def importToProfile(protoProfile: ProtoProfile, assignee: String, userId: String, replicate: Boolean = false, desktopSearch: Boolean = false): Future[Seq[String]] =
    importToProfileData.fromProtoProfileData(protoProfile.id, labCode, country, province, assignee, desktopSearch).flatMap {
      case (sampleCode, labo) =>
        val analysis = NewAnalysis(
          sampleCode, userId, null,
          Some(protoProfile.kit), None,
          protoProfile.genotypification.map(g => g.locus -> g.alleles).toMap,
          None, None,
          Some(protoProfile.mismatches),
          Some(protoProfile.matchingRules))

        val pd = protoProfile.protoProfileData.fold(
          profiledata.ProfileData(
            protoProfile.category, sampleCode, None, None, None, None, None, None,
            protoProfile.sampleName, assignee, labo, false, None, None, None, None, false)
        )(_.pdAttempToPd(labo))

        profileService.importProfile(pd, analysis, replicate).map {
          case Left(list) =>
            importToProfileData.deleteProfileData(sampleCode.text)
            list
          case Right(_) =>
            categoryService.getCategory(AlphanumericId(protoProfile.category)).foreach { catOpt =>
              catOpt.foreach { cat =>
                if cat.associations.nonEmpty then
                  notificationService.push(assignee, ProfileDataAssociationInfo(protoProfile.sampleName, sampleCode))
                  userService.sendNotifToAllSuperUsers(ProfileDataAssociationInfo(protoProfile.sampleName, sampleCode), Seq(assignee))
              }
            }
            Nil
        }.recover { case e =>
          importToProfileData.deleteProfileData(sampleCode.text)
          logger.error(protoProfile.sampleName, e)
          Seq("error.E0303")
        }
    }

  override def rejectProtoProfile(id: Long, motive: String, userId: String, idMotive: Long): Future[Seq[String]] =
    updateProtoProfileStatus(id, ProtoProfileStatus.Rejected, userId).flatMap { errors =>
      if errors.isEmpty then
        protoRepo.setRejectMotive(id, motive, userId, idMotive,
          new java.sql.Timestamp(Calendar.getInstance().getTime.getTime)).map { count =>
          if count == 1 then Nil else Seq("error.E0102")
        }
      else Future.successful(Nil)
    }

  override def updateBatchStatus(idBatch: Long, status: ProtoProfileStatus.Value, userId: String, isSuperUser: Boolean, replicateAll: Boolean, idsToReplicate: List[Long]): Future[Either[String, Long]] =
    userService.listAllUsers().flatMap { users =>
      val loggedUser = users.find(_.userName == userId).get
      val profiles =
        if status == ProtoProfileStatus.Imported || status == ProtoProfileStatus.Uploaded || status == ProtoProfileStatus.ReplicatedMatchingProfile then
          protoRepo.getProtoProfilesStep2(idBatch, loggedUser.geneMapperId, loggedUser.superuser)
        else
          protoRepo.getProtoProfilesStep1(idBatch)

      profiles.flatMap { pps =>
        Future.sequence(pps.map { pp =>
          val geneticistOpt = users.find(_.geneMapperId == pp.assignee)
          geneticistOpt.fold(Future.successful(Seq(s"error.E0200 assignee=${pp.assignee}"))) { geneticist =>
            val replicate =
              if replicateAll then
                Await.result(categoryService.getCategory(AlphanumericId(pp.category)), Duration(3, SECONDS))
                  .exists(_.replicate)
              else idsToReplicate.contains(pp.id)
            if (status == ProtoProfileStatus.Imported || status == ProtoProfileStatus.Approved) && !idsToReplicate.contains(pp.id) then
              if allowTransition(pp.status, status) then transitionStatus(status, pp, geneticist.userName, userId, replicate)
              else Future.successful(Seq.empty)
            else if replicate && idsToReplicate.contains(pp.id) then
              if allowTransition(pp.status, status) then transitionStatus(status, pp, geneticist.userName, userId, replicate = true)
              else Future.successful(Seq.empty)
            else Future.successful(Seq.empty)
          }
        })
      }
    }.map { seqs =>
      if seqs.exists(_.contains("Success")) then Right(idBatch)
      else if seqs.exists(_.nonEmpty) then
        Left(seqs.flatten.distinct.map(m => s"<br>$m").mkString("") + "<br>error.E0103")
      else Right(idBatch)
    }

  private def transitionStatus(status: ProtoProfileStatus.Value, protoProfile: ProtoProfile, assignee: String, userId: String, replicate: Boolean = false, desktopSearch: Boolean = false): Future[Seq[String]] =
    if !allowTransition(protoProfile.status, status) then
      Future.successful(Seq(s"error.E0104 from=${protoProfile.status} to=$status"))
    else status match
      case ProtoProfileStatus.Uploaded =>
        val perform = replicateProtoProfile(protoProfile, userId)
        updateStatus(protoProfile.id, status)
        perform.flatMap { errors =>
          if errors.isEmpty then
            notificationService.solve(assignee, BulkUploadInfo(protoProfile.id.toString, protoProfile.sampleName))
            updateStatus(protoProfile.id, status).map { res =>
              if res.isEmpty then userService.sendNotifToAllSuperUsers(BulkUploadInfo(protoProfile.id.toString, protoProfile.sampleName), Seq(assignee))
              res
            }
          else Future.successful(errors)
        }
      case ProtoProfileStatus.Imported | ProtoProfileStatus.DesktopSearch =>
        val res =
          if protoProfile.preexistence.isDefined then importLinkedProtoProfile(protoProfile, userId, replicate)
          else importToProfile(protoProfile, assignee, userId, replicate, desktopSearch)
        res.flatMap { errors =>
          if errors.isEmpty then
            notificationService.solve(assignee, BulkUploadInfo(protoProfile.id.toString, protoProfile.sampleName))
            updateStatus(protoProfile.id, status)
          else Future.successful(errors)
        }
      case ProtoProfileStatus.Approved =>
        updateStatus(protoProfile.id, status).map { x =>
          if x.isEmpty then
            notificationService.push(assignee, BulkUploadInfo(protoProfile.id.toString, protoProfile.sampleName))
            userService.sendNotifToAllSuperUsers(BulkUploadInfo(protoProfile.id.toString, protoProfile.sampleName), Seq(assignee))
          x
        }
      case ProtoProfileStatus.Disapproved => updateStatus(protoProfile.id, status)
      case ProtoProfileStatus.Rejected =>
        val us = updateStatus(protoProfile.id, status)
        us.foreach { list =>
          if list.isEmpty then notificationService.solve(assignee, BulkUploadInfo(protoProfile.id.toString, protoProfile.sampleName))
        }
        us
      case _ => updateStatus(protoProfile.id, status)

  override def updateProtoProfileStatus(id: Long, status: ProtoProfileStatus.Value, userId: String, replicate: Boolean = false, desktopSearch: Boolean = false): Future[Seq[String]] =
    val effectiveStatus =
      if status == ProtoProfileStatus.Imported && desktopSearch then ProtoProfileStatus.DesktopSearch
      else status
    protoRepo.getProtoProfile(id).flatMap {
      case None => Future.successful(Seq(s"error.E0105 id=$id"))
      case Some(pp) =>
        userService.findByGeneMapper(pp.assignee).flatMap {
          case None => Future.successful(Seq(s"error.E0200 assignee=${pp.assignee}"))
          case Some(geneticist) =>
            transitionStatus(effectiveStatus, pp, geneticist.userName, userId, replicate, desktopSearch)
        }
    }

  override def updateProtoProfileRulesMismatch(id: Long, matchingRules: Seq[MatchingRule], mismatches: Profile.Mismatch): Future[Boolean] =
    protoRepo.updateProtoProfileMatchingRulesMismatch(id, matchingRules, mismatches).map(_ == 1)

  override def updateProtoProfileData(id: Long, category: AlphanumericId, userId: String): Future[Either[Seq[String], ProtoProfile]] =
    protoRepo.getProtoProfile(id).flatMap {
      case None => Future.successful(Left(Seq(s"error.E0900 id=$id")))
      case Some(pp) =>
        categoryService.getCategory(category).flatMap {
          case None => Future.successful(Left(Seq(s"error.E0600 cat=${category.text}")))
          case Some(cat) =>
            val validateF = pp.preexistence.fold[Future[Option[String]]](Future.successful(None)) { gc =>
              protoRepo.validateAssigneAndCategory(gc, pp.assignee, Some(cat.id))
            }
            validateF.flatMap {
              case Some(err) => Future.successful(Left(Seq(err)))
              case None =>
                protoRepo.updateProtoProfileData(id, cat.id).flatMap {
                  case 1 =>
                    if pp.status == ProtoProfileStatus.Approved then
                      Future.successful(Right(pp.copy(category = cat.id.text)))
                    else
                      updateProtoProfileStatus(id, ProtoProfileStatus.ReadyForApproval, userId).flatMap {
                        case Nil => protoRepo.getProtoProfile(id).map(opt => Right(opt.get))
                        case err => Future.successful(Left(err))
                      }
                  case count => Future.successful(Left(Seq(s"error.E0660 count=$count")))
                }
            }
        }
    }

  override def deleteBatch(id: Long): Future[Either[String, Long]] =
    protoRepo.countImportedProfilesByBatch(id).flatMap {
      case Left(msg) => Future.successful(Left(msg))
      case Right(count) =>
        if count > 0 then Future.successful(Left("error.E0304"))
        else protoRepo.deleteBatch(id)
    }

  override def searchBatch(userId: String, isSuperUser: Boolean, search: String): Future[Seq[ProtoProfilesBatchView]] =
    protoRepo.getSearchBachLabelID(userId, isSuperUser, search)

  override def getBatchSearchModalViewByIdOrLabel(input: String, idCase: Long): Future[List[BatchModelView]] =
    if input.isEmpty then Future.successful(Nil)
    else protoRepo.getBatchSearchModalViewByIdOrLabel(input, idCase)
