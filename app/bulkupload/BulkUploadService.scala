  package bulkupload

  import javax.inject.Inject
  import play.api.libs.ws.WSClient

  import scala.concurrent.{ExecutionContext, Future}
  import play.api.mvc.Call

  import java.io.File
  import java.util.Calendar
  import javax.inject.{Inject, Named, Singleton}
  import configdata._
  import connections.InterconnectionService
  import inbox._
  import kits.StrKitService
  import play.api.Logger
  import play.api.libs.Files.TemporaryFile
  import play.api.libs.concurrent.Execution.Implicits.defaultContext
  import profile.{NewAnalysis, Profile, ProfileService}
  import profiledata._
  import search.PaginationSearch
  import services.CacheService
  import types.{AlphanumericId, SampleCode}
  import user.{UserService, UserView}
  import play.api.i18n.Messages

  import scala.concurrent.duration._
  import scala.concurrent.{Await, Future}
  import scala.language.{implicitConversions, postfixOps}

  abstract class BulkUploadService @Inject() (
                                               profileDataRepo: ProfileDataRepository,
                                               interconnectionService: InterconnectionService // Inject InterconnectionService
                                             ) {
    def getBatchesStep1(userId: String, isSuperUser: Boolean, offset: Int, limit: Int): Future[Seq[ProtoProfilesBatchView]]
    def countBatchesStep1(userId: String, isSuperUser: Boolean): Future[Int]
    def getBatchesStep2(userId: String, geneMapperId: String, isSuperUser: Boolean, offset: Int, limit: Int): Future[Seq[ProtoProfilesBatchView]]
    def countBatchesStep2(userId: String, geneMapperId: String, isSuperUser: Boolean): Future[Int]
    def getProtoProfile(id: Long): Future[Option[ProtoProfile]]
    def getProtoProfileWithBatchId(id: Long): Future[Option[(ProtoProfile, Long)]]
    def getProtoProfilesStep1(batchId: Long, paginationSearch: Option[PaginationSearch]): Future[Seq[ProtoProfile]]
    def getProtoProfilesStep2(batchId: Long, geneMapperId: String, isSuperUser: Boolean, paginationSearch: Option[PaginationSearch] = None): Future[Seq[ProtoProfile]]
    def uploadProtoProfiles(user: String, csvFile: TemporaryFile, label: Option[String], analysisType: String): Future[Either[String, Long]]
    //def getBatchDetails(batchId: Long, paginationSearch: PaginationSearch): Future[Option[BatchDetails]]
    def rejectProtoProfile(id: Long, motive: String, userId: String, idMotive: Long): Future[Seq[String]]
    def updateProtoProfileStatus(id: Long, status: ProtoProfileStatus.Value, userId: String, replicate: Boolean, desktopSearch: Boolean): Future[Seq[String]]
    def updateProtoProfileData(id: Long, category: AlphanumericId, userId: String): Future[Either[Seq[String], ProtoProfile]]
    def updateBatchStatus(idBatch: Long, status: ProtoProfileStatus.Value, userId: String, isSuperUser: Boolean, replicateAll: Boolean, idsToReplicate: scala.List[Long]): Future[Either[String, Long]]
    def updateProtoProfileRulesMismatch(id: Long, matchingRules: Seq[MatchingRule], mismatches: Profile.Mismatch): Future[Boolean]
    def deleteBatch(id: Long): Future[Either[String, Long]]
    def searchBatch(userId: String, isSuperUser: Boolean, search: String): Future[Seq[ProtoProfilesBatchView]]
    def getBatchSearchModalViewByIdOrLabel(input: String, idCase: Long): Future[List[BatchModelView]]
    protected def replicateProtoProfile(protoProfile: ProtoProfile, userId: String): Future[Seq[String]] //Declared Abstract
  }

  @Singleton
  class BulkUploadServiceImpl @Inject() (
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
                                          val interconnectionService: InterconnectionService // And inject in the Implementation class
                                        ) extends BulkUploadService(profileDataRepo, interconnectionService) { //Pass the dependencies to the Abstract class

    implicit private def stringToAlphanumericId(s: String) = AlphanumericId(s)

    val logger = Logger(this.getClass())

    private val allowTransition = (a: ProtoProfileStatus.Value, b: ProtoProfileStatus.Value) => (a, b) match {
      case (ProtoProfileStatus.Incomplete, ProtoProfileStatus.ReadyForApproval) => true
      case (ProtoProfileStatus.ReadyForApproval, ProtoProfileStatus.ReadyForApproval) => true
      case (ProtoProfileStatus.Incomplete, ProtoProfileStatus.Disapproved) => true
      case (ProtoProfileStatus.ReadyForApproval, ProtoProfileStatus.Approved) => true
      case (ProtoProfileStatus.ReadyForApproval, ProtoProfileStatus.Disapproved) => true
      case (ProtoProfileStatus.Approved, ProtoProfileStatus.Imported) => true
      case (ProtoProfileStatus.Approved, ProtoProfileStatus.DesktopSearch) => true
      case (ProtoProfileStatus.Approved, ProtoProfileStatus.Rejected) => true
      case (ProtoProfileStatus.Imported, ProtoProfileStatus.Uploaded) => true
      case (ProtoProfileStatus.Uploaded, ProtoProfileStatus.Imported) => true
      case (ProtoProfileStatus.Imported,ProtoProfileStatus.ReplicatedMatchingProfile) => true
      case (ProtoProfileStatus.Uploaded, ProtoProfileStatus.ReplicatedMatchingProfile) => true
      case (ProtoProfileStatus.ReplicatedMatchingProfile, ProtoProfileStatus.Uploaded) => true
      case (ProtoProfileStatus.ReplicatedMatchingProfile, ProtoProfileStatus.Uploaded) => true //Por si se cambia de categoría a una en la que no haga match
      case (_, _) => false
    }

    override def getProtoProfilesStep1(batchId: Long, paginationSearch: Option[PaginationSearch] = None): Future[Seq[ProtoProfile]] = {
      protoRepo.getProtoProfilesStep1(batchId, paginationSearch)
    }

    override def getProtoProfilesStep2(batchId: Long, geneMapperId: String, isSuperUser: Boolean, paginationSearch: Option[PaginationSearch] = None): Future[Seq[ProtoProfile]] = {
      protoRepo.getProtoProfilesStep2(batchId, geneMapperId, isSuperUser, paginationSearch).flatMap { profiles =>
        Future.sequence(profiles.map { profile =>
          if (profile.status == ProtoProfileStatus.Imported) {
            interconnectionService.isUplpoadableInternalCode(profile.sampleName).map { isReplicable =>
              if (!isReplicable) profile.copy(status = ProtoProfileStatus.ReplicatedMatchingProfile)
              else profile
            }
          } else {
            Future.successful(profile)
          }
        })
      }
    }

    override def getProtoProfile(id: Long): Future[Option[ProtoProfile]] = {
      protoRepo.getProtoProfile(id)
    }

    override def getProtoProfileWithBatchId(id: Long): Future[Option[(ProtoProfile, Long)]] = {
      protoRepo.getProtoProfileWithBatchId(id)
    }

    override def getBatchesStep1(
                         userId: String,
                         isSuperUser: Boolean,
                         offset: Int,
                         limit: Int
                       ): Future[Seq[ProtoProfilesBatchView]] =
      protoRepo.getBatchesStep1(userId, isSuperUser, offset, limit)

    override def countBatchesStep1(userId: String, isSuperUser: Boolean): Future[Int] =
      protoRepo.countBatchesStep1(userId, isSuperUser)

    override def getBatchesStep2(userId: String,
                                 geneMapperId: String,
                                 isSuperUser: Boolean,
                                 offset: Int,
                                 limit: Int
                                ): Future[Seq[ProtoProfilesBatchView]] = {
      protoRepo.getBatchesStep2(userId, geneMapperId, isSuperUser, offset, limit)
    }
    override def countBatchesStep2(userId: String, geneMapperId: String, isSuperUser: Boolean): Future[Int] =
      protoRepo.countBatchesStep2(userId, geneMapperId, isSuperUser)

    override def uploadProtoProfiles(
                                      user: String,
                                      tempFile: TemporaryFile,
                                      label: Option[String],
                                      analysisType: String
                                    ): Future[Either[String, Long]] = {
      val aliasKits = kitService.getKitAlias
      val lociAlias = kitService.getLocusAlias
      val kitsPromise = kitService.list() flatMap { kits =>
        val primisedLoci = kits map {
          kit =>
            val kitLoci = kitService
              .findLociByKit(kit.id) map { loci => (kit.id, loci.map(_.id)) }
            kitLoci
        }
        Future.sequence(primisedLoci) map { _.toMap }
      }
      val categoryAliasesPromise = categoryService.listCategories flatMap {
        case (id, category) =>
          category.aliases.map { _ -> id } :+ ((id.text, id))
      }
      val vaildatorPromise = for {
        geneticists <- userService.findUserAssignable.map { _.toList }
        kits <- kitsPromise
        alKit <- aliasKits
        lociAl <- lociAlias
        categoryAliases <- Future.successful(categoryAliasesPromise)
      } yield {
        Validator(
          protoRepo,
          kits.map(tuple => (tuple._1.toLowerCase, tuple._2)),
          alKit.map(tuple => (tuple._1.toLowerCase, tuple._2)),
          lociAl.map(tuple => (tuple._1.toLowerCase, tuple._2)),
          geneticists,
          categoryAliases
        )
      }
      protoProfiledataService
        .getMtRcrs()
        .flatMap(
          mtRcrs => {
            vaildatorPromise
              .flatMap {
                vaildator =>
                  val csvFile = new File(
                    tempFile.file.getAbsolutePath + "_permanent"
                  )
                  tempFile.moveTo(csvFile)
                  tempFile.clean()
                  val either = if (analysisType.equals("Autosomal")) {
                    GeneMapperFileParser.parse(csvFile, vaildator)
                  } else {
                    GeneMapperFileMitoParser.parse(csvFile, vaildator, mtRcrs)
                  }
                  either.fold[Future[Either[String, Long]]](
                    error => {
                      logger.error(error)
                      Future.successful(Left(error))
                    },
                    stream => {
                      val kits = stream.map(_.kit).distinct.toSeq
                      kitService.findLociByKits(kits).flatMap {
                        kits =>
                          val batchIdPromise = protoRepo.createBatch(
                            user, stream, labCode, kits, label, analysisType
                          )
                          batchIdPromise
                            .onComplete { batchId => csvFile.delete() }
                          batchIdPromise map { Right(_) }
                      }
                    }
                  )
              }
          }
        )
        .recover {
          case e: IndexOutOfBoundsException =>
            logger.error(Messages("error.E0302"), e)
            Left(Messages("error.E0302"))
          case error: KitNotExistsException =>
            val errorMessage = Messages("error.E0316", error.getMessage)
            logger.error(errorMessage)
            Left(errorMessage)
          case error => {
            logger.error(error.getMessage)
            Left(Messages("error.E0301"))
          }
        }
    }

    private def updateStatus(
                              id: Long,
                              status: ProtoProfileStatus.Value
                            ): Future[Seq[String]] = {
      protoRepo
        .updateProtoProfileStatus(id, status)
        .map {
          count =>
            if (count == 1) {
              Nil
            } else {
              Seq(Messages("error.E0100", count))
            }
        }
    }

    /*private def couldImport(protoProfile: ProtoProfile): Future[Boolean] = {
      if (!categoryService.getCategory(protoProfile.category).get.filiationDataRequired) {
        Future.successful(true)
      } else {
        protoRepo.hasProfileDataFiliation(protoProfile.id)
      }
    }*/
    //Now is protected and is implemented on the Implementation class
    override protected def replicateProtoProfile(protoProfile: ProtoProfile, userId: String): Future[Seq[String]] = {
      //protoProfile.preexistence.fold[Future[Seq[String]]](Future.successful(Seq(Messages("error.E0101"))))(gc => {
      profileDataRepo.getGlobalCode(protoProfile.sampleName).flatMap {
        _.fold(Future.successful(Seq(Messages("error.E0101")))) {
          case SampleCode(globalCode) => {
            interconnectionService.uploadProfile(globalCode, userId).map {
              case Right(success) => Seq("Success") //success is Unit.
              case Left(error) =>
                logger.error(s"Failed to upload profile: $error")
                Seq(Messages("error.E0731", error))
            }
          }
        }
      }
    }


    private def importLinkedProtoProfile(protoProfile: ProtoProfile, userId: String,replicate : Boolean = false): Future[Seq[String]] = {

      profileDataRepo.getGlobalCode(protoProfile.sampleName).flatMap {
        _.fold[Future[Seq[String]]](Future.successful(Seq(Messages("error.E0101"))))(sampleCode => {

          val analysis = NewAnalysis(
            sampleCode, userId, null,
            Some(protoProfile.kit), None,
            protoProfile.genotypification.map(g => g.locus -> g.alleles).toMap,
            None, None,
            Option(protoProfile.mismatches),
            Option(protoProfile.matchingRules))

          profileService.create(analysis, false,replicate).map {
            _ match {
              case Left(err) => err
              case Right(_)  => Nil
            }
          }

        })
      }
    }



    private def importToProfile(protoProfile: ProtoProfile, assignee: String, userId: String,replicate : Boolean = false, desktopSearch:Boolean = false): Future[Seq[String]] = {
      val (sampleCode, labo) = importToProfileData.fromProtoProfileData(protoProfile.id, labCode, country, province, assignee, desktopSearch)

      val analysis = NewAnalysis(
        sampleCode, userId, null,
        Some(protoProfile.kit), None,
        protoProfile.genotypification.map(g => g.locus -> g.alleles).toMap,
        None, None,
        Option(protoProfile.mismatches),
        Option(protoProfile.matchingRules))

      val pd: ProfileData = protoProfile.protoProfileData.fold(
        ProfileData(
          protoProfile.category,
          sampleCode, None, None, None, None, None, None, protoProfile.sampleName,
          assignee, labo, false, None, None, None, None, None, None,false))(_.pdAttempToPd(labo))

      profileService.importProfile(pd, analysis,replicate).map {
        case Left(list) => {
          importToProfileData.deleteProfileData(sampleCode.text)

          list
        }
        case Right(gc) => {
          if (categoryService.listCategories(protoProfile.category).associations.nonEmpty) {
            notificationService.push(assignee,
              ProfileDataAssociationInfo(protoProfile.sampleName, sampleCode))
            userService.sendNotifToAllSuperUsers(ProfileDataAssociationInfo(protoProfile.sampleName, sampleCode), Seq(assignee))
          }
          Nil
        }
      }.recover {
        case e => {
          importToProfileData.deleteProfileData(sampleCode.text)

          logger.error( protoProfile.sampleName , e)
          Seq(Messages("error.E0303"))
        }
      }
    }

    override def rejectProtoProfile(
                                     id: Long,
                                     motive: String,
                                     userId: String,
                                     idMotive:Long
                                   ): Future[Seq[String]] = {
      this
        .updateProtoProfileStatus(
          id,
          ProtoProfileStatus.Rejected,
          userId
        )
        .flatMap {
          errors =>
            if (errors.isEmpty) {
              protoRepo
                .setRejectMotive(
                  id,
                  motive,
                  userId,
                  idMotive,
                  new java.sql.Timestamp(Calendar.getInstance().getTime().getTime)
                )
                .map {
                  count =>
                    if (count == 1) {
                      Nil
                    } else {
                      Seq(Messages("error.E0102"))
                    }
                }
            } else {
              Future.successful(Nil)
            }
        }
    }

    override def updateBatchStatus(
                                    idBatch: Long,
                                    status: ProtoProfileStatus.Value,
                                    userId: String,
                                    isSuperUser: Boolean,
                                    replicateAll:Boolean,
                                    idsToReplicate: scala.List[Long]
                                  ): Future[Either[String, Long]] = {
      val result = userService
        .listAllUsers()
        .flatMap {
          users =>
            val loggedUser = users.find(u => u.userName == userId).get

            val profiles = if (status == ProtoProfileStatus.Imported || status == ProtoProfileStatus.Uploaded || status == ProtoProfileStatus.ReplicatedMatchingProfile ) {
              protoRepo.getProtoProfilesStep2(
                idBatch,
                loggedUser.geneMapperId,
                loggedUser.superuser
              )
            } else {
              protoRepo.getProtoProfilesStep1(idBatch)
            }
            profiles.flatMap {
              protoProfiles =>
                Future.sequence(
                  protoProfiles.map(
                    protoProfile => {
                      val geneticistOpt = users
                        .find(u => u.geneMapperId == protoProfile.assignee)
                      val errorMessage = () => {
                        Future.successful(
                          Seq(Messages("error.E0200", protoProfile.assignee))
                        )
                      }
                      val performTransition = (geneticist:UserView) => {
                        // Determine whether to replicate or not

                        var replicate = false
                        if (replicateAll) {
                          val category = categoryService
                            .getCategory(protoProfile.category)
                          if (category.isDefined && category.get.replicate) {
                            replicate = true;
                          }
                        } else {
                          if (idsToReplicate.contains(protoProfile.id)) {
                            replicate = true;
                          }
                        }

                        //Only import if is not in idsToReplicate List
                        if ((status == ProtoProfileStatus.Imported || status == ProtoProfileStatus.Approved) && !idsToReplicate.contains(protoProfile.id)) {
                          if (allowTransition(protoProfile.status, status)) {
                            transitionStatus(
                              status,
                              protoProfile,
                              geneticist.userName,
                              userId,
                              replicate
                            )
                          } else {
                            Future.successful(Seq())
                          }
                        } else if (replicate && idsToReplicate.contains(protoProfile.id)) {
                          // If we are only replicating, and this profile should be replicated
                          if (allowTransition(protoProfile.status, status)) {
                            //transitionStatus with replicate = true only
                            transitionStatus(
                              status,
                              protoProfile,
                              geneticist.userName,
                              userId,
                              true //FORCE REPLICATE TO TRUE
                            )
                          } else {
                            Future.successful(Seq())
                          }

                        }
                        else{
                          Future.successful(Seq()) // do nothing
                        }
                      }
                      geneticistOpt.fold(errorMessage())(performTransition)
                    }
                  )
                )
            }
        }
      result.map { sequences =>
        // Check if any sequence contains "Success"
        if (sequences.exists(_.contains("Success"))) {
          Right(idBatch) // Return Right(idBatch) if "Success" is found
        } else if (sequences.exists(_.nonEmpty)) {
          // If there are non-empty sequences but no "Success", return the error message
          val joinedMessage = (
            sequences
              .flatten
              ++ Seq(Messages("error.E0103"))
            )
            .distinct
            .map(msg => s"<br>${msg}")
            .mkString("")
          Left(joinedMessage)
        } else {
          Right(idBatch) // If all sequences are empty, return Right(idBatch)
        }
      }
    }


    private def transitionStatus(
                                  status: ProtoProfileStatus.Value,
                                  protoProfile: ProtoProfile,
                                  assignee: String,
                                  userId: String,
                                  replicate: Boolean = false
                                  , desktopSearch: Boolean = false
                                ): Future[Seq[String]] = {
      if (allowTransition(protoProfile.status, status)) {
        status match {
          case ProtoProfileStatus.Uploaded => {
            // If preexistence present, replicate to superior via importLinkedProtoProfile,
            // otherwise perform a normal import but with replicate=true to force replication.
            val perform: Future[Seq[String]] =
              replicateProtoProfile(protoProfile, userId)
            updateStatus(protoProfile.id, status)
            perform.flatMap {
              errors =>
                if (errors.isEmpty) {
                  // mark as Uploaded and notify assignee and superusers
                  notificationService.solve(
                    assignee,
                    BulkUploadInfo(protoProfile.id.toString, protoProfile.sampleName)
                  )
                  updateStatus(protoProfile.id, status).map { res =>
                    if (res.isEmpty) {
                      userService.sendNotifToAllSuperUsers(
                        BulkUploadInfo(protoProfile.id.toString, protoProfile.sampleName),
                        Seq(assignee)
                      )
                    }
                    res
                  }
                } else {
                  // ADDED: Return E0731 if replication fails
                  val replicationErrors = errors.map(error =>
                    if (error.contains("replication")) Messages("error.E0731") else error
                  )
                  Future.successful(replicationErrors)
                }
            }
          }

          case ProtoProfileStatus.Imported | ProtoProfileStatus.DesktopSearch  => {
            val res = if (protoProfile.preexistence.isDefined) {
              importLinkedProtoProfile(protoProfile, userId, replicate)
            } else {
              importToProfile(protoProfile, assignee, userId, replicate, desktopSearch)
            }
            res flatMap {
              errors =>
                if (errors.isEmpty) {
                  notificationService.solve(
                    assignee,
                    BulkUploadInfo(
                      protoProfile.id.toString,
                      protoProfile.sampleName
                    )
                  )
                  updateStatus(protoProfile.id, status)
                } else {
                  Future.successful(errors)
                }
            }
          }
          case ProtoProfileStatus.Approved => {
            updateStatus(protoProfile.id, status)
              .map {
                x =>
                  if (x.isEmpty) {
                    notificationService.push(
                      assignee,
                      BulkUploadInfo(
                        protoProfile.id.toString,
                        protoProfile.sampleName
                      )
                    )
                  }
                  userService
                    .sendNotifToAllSuperUsers(
                      BulkUploadInfo(
                        protoProfile.id.toString,
                        protoProfile.sampleName
                      ),
                      Seq(assignee)
                    )
                  x
              }
          }
          case ProtoProfileStatus.Disapproved => {
            updateStatus(protoProfile.id, status)
          }
          case ProtoProfileStatus.Rejected => {
            val us = updateStatus(protoProfile.id, status)
            us.onSuccess {
              case list =>
                if (list.isEmpty) {
                  notificationService.solve(
                    assignee,
                    BulkUploadInfo(
                      protoProfile.id.toString,
                      protoProfile.sampleName
                    )
                  )
                }
            }
            us
          }
          case _ => updateStatus(protoProfile.id, status)
        }

      } else {
        Future.successful(
          Seq(Messages("error.E0104", protoProfile.status, status))
        )
      }
    }


    override def updateProtoProfileStatus(
                                           id: Long,
                                           status: ProtoProfileStatus.Value,
                                           userId: String,
                                           replicate : Boolean = false,
                                           desktopSearch: Boolean = false
                                         ): Future[Seq[String]] = {
      // Determinar el estado real: si viene Imported + desktopSearch => DesktopSearch
      val effectiveStatus =
        if (status == ProtoProfileStatus.Imported && desktopSearch)
          ProtoProfileStatus.DesktopSearch
        else
          status

      protoRepo
        .getProtoProfile(id)
        .flatMap {
          protoProfileOpt =>
            lazy val error105 = Future.successful(
              Seq(Messages("error.E0105", id))
            )
            val genError200 = (assignee:String) => {
              Future.successful(
                Seq(
                  Messages("error.E0200", assignee )
                )
              )
            }
            val transitionWithGeneticist =
              (protoProfile: ProtoProfile) =>
                (g:UserView) => {
                  transitionStatus(
                    effectiveStatus,
                    protoProfile,
                    g.userName,
                    userId,
                    replicate,
                    desktopSearch
                  )
                }
            val transitionate = (protoProfile:ProtoProfile) => {
              userService
                .findByGeneMapper(protoProfile.assignee)
                .flatMap(
                  _.fold
                  (genError200(protoProfile.assignee))
                  (transitionWithGeneticist(protoProfile))
                )
            }
            protoProfileOpt.fold(error105)(transitionate)
        }
    }

    override def updateProtoProfileRulesMismatch(id: Long, matchingRules: Seq[MatchingRule], mismatches: Profile.Mismatch): Future[Boolean] = {
      protoRepo.updateProtoProfileMatchingRulesMismatch(id, matchingRules, mismatches) map { _ == 1 }
    }

    override def updateProtoProfileData(id: Long, category: AlphanumericId, userId: String): Future[Either[Seq[String], ProtoProfile]] = {

      protoRepo.getProtoProfile(id) flatMap { protoProfileOpt =>

        categoryService.listCategories.get(category) match {

          case None => Future.successful(Left(Seq(Messages("error.E0600",category.text))))

          case Some(category) =>

            protoProfileOpt match {

              case None => Future.successful(Left(Seq(Messages("error.E0900", id))))

              case Some(protoProfile) =>
                protoProfile.preexistence.fold[Future[Option[String]]](Future.successful(None))(gc =>
                    protoRepo.validateAssigneAndCategory(gc, protoProfile.assignee, Some(category.id)))
                  .flatMap {
                    _.fold(protoRepo.updateProtoProfileData(id, category.id) flatMap {
                      case 1 => {

                        if(ProtoProfileStatus.Approved == protoProfile.status){
                          Future.successful(Right((protoProfile.copy(category=category.id.text))))
                        }else{
                          updateProtoProfileStatus(id, ProtoProfileStatus.ReadyForApproval, userId) flatMap {
                            case Nil => this.getProtoProfile(id) map { optPPp => Right(optPPp.get) }
                            case err => Future.successful(Left(err))
                          }
                        }
                      }
                      case count => Future.successful(Left(Seq(Messages("error.E0660" , count ))))
                    })(ss => Future.successful(Left(Seq(ss))))
                  }
            }
        }
      }
    }
    override def deleteBatch(id: Long):Future[Either[String,Long]] = {

      val countImported = Await
        .result(
          protoRepo.countImportedProfilesByBatch(id),
          Duration(10, SECONDS)
        )
      countImported.fold(msg => {
        Future.successful(Left(msg))
      }, count =>{
        if(count>0){
          Future.successful(Left(Messages("error.E0304")))
        }else{
          protoRepo.deleteBatch(id)

        }
      })

    }

    override def searchBatch(userId: String, isSuperUser : Boolean, search: String): Future[Seq[ProtoProfilesBatchView]] = {
      protoRepo.getSearchBachLabelID(userId, isSuperUser, search)
    }

    override def getBatchSearchModalViewByIdOrLabel(input:String,idCase:Long):Future[List[BatchModelView]] = {
      if(input.isEmpty){
        Future.successful(Nil)
      }else{
        this.protoRepo.getBatchSearchModalViewByIdOrLabel(input,idCase)
      }
    }

  }
