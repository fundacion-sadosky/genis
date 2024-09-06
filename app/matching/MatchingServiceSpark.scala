package matching

import java.io.File
import java.util.Date
import javax.inject.{Inject, Named, Singleton}
import com.github.tototoshi.csv.{CSVWriter, DefaultCSVFormat}
import profiledata.ProfileData
import configdata.{CategoryService, QualityParamsProvider}
import inbox.{MatchingInfo, NotificationService}
import matching.MatchGlobalStatus.MatchGlobalStatus
import matching.MatchStatus.MatchStatus
import pedigree.{PedigreeGenotypificationService, PedigreeRepository, PedigreeService, PedigreeSparkMatcher}
import play.api.Logger
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import profile.{Profile, ProfileRepository}
import profiledata.ProfileDataRepository
import scenarios.{Scenario, ScenarioRepository}
import trace._
import types.{AlphanumericId, MongoDate, MongoId, SampleCode}
import util.EnumJsonUtils.enumWrites
import play.api.i18n.{Messages, MessagesApi}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import connections.InterconnectionService

import scala.concurrent.duration._
@Singleton
class MatchingServiceSparkImpl @Inject() (
    profileRepo: ProfileRepository,
    matchingRepo: MatchingRepository,
    notificationService: NotificationService,
    categoryService: CategoryService,
    profileDataRepo: ProfileDataRepository,
    scenarioRepository: ScenarioRepository,
    spak2Matcher: Spark2Matcher,
    traceService: TraceService,
    pedigreeSparkMatcher: PedigreeSparkMatcher,
    pedigreeRepo: PedigreeRepository,
    pedigreeGenotypificationService: PedigreeGenotypificationService,
    interconnectionService: InterconnectionService,
    spark2MatcherCollapsing: Spark2MatcherCollapsing,
    spark2MatcherScreening: Spark2MatcherScreening,
    messagesApi: MessagesApi
    /*,
    /*@Named("limsArchivesPath")*/ exportProfilesPath: String = "",
    /*@Named("generateLimsFiles")*/ exportaALims: Boolean = false
    */
) extends MatchingService {
  implicit val messages: Messages = messagesApi.preferred(Seq.empty)

  val discarded = MatchStatus.discarded.toString
  val hit = MatchStatus.hit.toString
  val deleted = MatchStatus.deleted.toString

  val logger = Logger(this.getClass)
  override def getAllTotalMatches(search: MatchCardSearch): Future[Int] = {
    this.matchingRepo.getAllTotalMatches(search)
  }
  override def updateMatchesLR(matchingLRs: Set[(String, Double)]):Future[Unit] = {
    this.matchingRepo.updateMatchesLR(matchingLRs)
  }
  override def deleteForce(matchId: String, globalCode: SampleCode): Future[Boolean] = {
    matchingRepo.deleteMatches(matchId, globalCode)
  }

  def delete(matchId: String): Future[Boolean] = {
    matchingRepo.deleteMatch(matchId)
  }

  private def canDiscardMatch(
    firingCode: SampleCode,
    matchingCode: SampleCode,
    userId: String,
    isSuperUser: Boolean
  ): Future[Boolean] = {
    scenarioRepository
      .getByMatch(firingCode, matchingCode, userId, isSuperUser)
      .map { scenarios => scenarios.isEmpty }
  }

  private def getFiringCode(
    firingCodeParam: SampleCode,
    leftCode: SampleCode,
    rightCode: SampleCode,
    replicate: Boolean
  ): SampleCode = {
    if (replicate && !this.interconnectionService.isFromCurrentInstance(firingCodeParam)) {
      if (this.interconnectionService.isFromCurrentInstance(leftCode)) {
        leftCode
      } else {
        rightCode
      }
    } else {
      firingCodeParam
    }
  }

/*
  implicit object MatchArchiveFormat extends DefaultCSVFormat {
    override val delimiter = '\t'
  }

  private def createMatchLimsArchive(leftCode: SampleCode, rightCode: SampleCode, matchId: String, status: String) = {
    val folder = s"$exportProfilesPath${File.separator}"
    val folderFile = new File(folder)

    folderFile.mkdirs

    generateMatchesFile(folder, leftCode, rightCode, matchId, status)

  }

  private def generateMatchesFile(folder: String, leftCode: SampleCode, rightCode: SampleCode, matchId: String, status: String) = {
    var name = folder +"MatchesPerfil" + leftCode.text
    if (status.equals(MatchStatus.hit.toString) || status.equals(MatchStatus.discarded.toString)) {
      name = name + status + ".txt"
    } else {
      name = name + ".txt"
    }
//    val file = new File(s"${folder}MatchesPerfil${leftCode.text}.csv")
    val file = new File(name)
    val format = new java.text.SimpleDateFormat("dd/MM/yyyy hh:mm:ss a")
    val leftProfileFuture = profileRepo.findByCode(leftCode)
    val leftProfile = Await.result(leftProfileFuture, Duration(100, SECONDS))

    val rightProfileFuture = profileRepo.findByCode(rightCode)
    val rightProfile = Await.result(rightProfileFuture, Duration(100, SECONDS))

    val writer = CSVWriter.open(file)
    writer.writeAll(List(List("MatchId", "Sample GENis Code", "Sample Name", "MatchedSample GENis Code", "MatchedSample Name", "status", "Datetime" )))

    writer.writeAll(List(List(matchId, leftCode.text, leftProfile.get.internalSampleCode, rightCode.text, rightProfile.get.internalSampleCode, status, format.format(new Date()))))

    writer.close()
    file
  }
*/

  override def convertDiscard(
    matchId: String,
    firingCodeParam: SampleCode,
    isSuperUser: Boolean,
    replicate: Boolean = true
  ): Future[Either[String, Seq[SampleCode]]] = {
    matchingRepo
      .getByMatchingProfileId(matchId)
      .flatMap {
        matchResult =>
          if (replicate && this.interconnectionService.isExternalMatch(matchResult.get)) {
            Future.successful(Left(Messages("error.E0726")))
          } else if (
            (matchResult.get.leftProfile.status == MatchStatus.deleted || matchResult.get.rightProfile.status == MatchStatus.deleted) ||
              !(matchResult.get.leftProfile.status == MatchStatus.pending || matchResult.get.rightProfile.status == MatchStatus.pending)
          ) {
            Future.successful(Left(Messages("error.E1000")))
          }
          else {
            val leftCode = matchResult.get.leftProfile.globalCode
            val rightCode = matchResult.get.rightProfile.globalCode
            val firingCode = getFiringCode(firingCodeParam, leftCode, rightCode, replicate)
            val isRight = rightCode == firingCode
            val userId = if (isRight) matchResult.get.rightProfile.assignee else matchResult.get.leftProfile.assignee
            val matchingProfile = if (isRight) matchResult.get.leftProfile else matchResult.get.rightProfile

            canDiscardMatch(leftCode, rightCode, userId, isSuperUser) flatMap {
              allowed =>
              if (allowed) {
                val res = matchingRepo.convertStatus(matchId, firingCode, discarded)

                res.onSuccess({ case _ => {
                  traceService.add(Trace(firingCode, userId, new Date(),
                    DiscardInfo(matchResult.get._id.id, matchingProfile.globalCode, matchingProfile.assignee, matchResult.get.`type`)))
                  traceService.add(Trace(matchingProfile.globalCode, userId, new Date(),
                    DiscardInfo(matchResult.get._id.id, firingCode, matchingProfile.assignee, matchResult.get.`type`)))
                  notificationService.solve(userId, MatchingInfo(leftCode, rightCode, matchId))
                  notificationService.solve(userId, MatchingInfo(rightCode, leftCode, matchId))
    /*
                  if(exportaALims) {
                    createMatchLimsArchive(leftCode, rightCode, matchId, discarded)
                  }
    */
                  if (replicate) {
                    this.interconnectionService.convertStatus(matchId, firingCode, discarded, matchResult.get)
                  }
                }
                })
                res map { codes => Right(codes) }
              } else {
                Future.successful(Left(Messages("error.E0902")))
              }
            }
          }
    }
  }

  override def uploadStatus(matchId: String, firingCodeParam: SampleCode, isSuperUser: Boolean): Future[String] = {
    matchingRepo.getByMatchingProfileId(matchId) flatMap { matchResult =>
        val leftCode = matchResult.get.leftProfile.globalCode
        val rightCode = matchResult.get.rightProfile.globalCode
        this.interconnectionService.convertStatus(matchId, leftCode, matchResult.get.leftProfile.status.toString, matchResult.get,true)
        this.interconnectionService.convertStatus(matchId, rightCode, matchResult.get.rightProfile.status.toString, matchResult.get,true)
        Future.successful("Se envio la actualizacion de estado de match")
      }
  }

  override def canUploadMatchStatus(matchId: String, isCollapsing:Option[Boolean] = None, isScreening:Option[Boolean] = None): Future[Boolean] = {
    matchingRepo.getByMatchingProfileId(matchId,isCollapsing, isScreening) flatMap { matchResult =>
      Future.successful(this.interconnectionService.isInterconnectionMatch(matchResult.get))
    }
  }

  override def convertHit(matchId: String, firingCodeParam: SampleCode, replicate: Boolean = true): Future[Either[String, Seq[SampleCode]]] = {
    matchingRepo.getByMatchingProfileId(matchId) flatMap { matchResult =>
      if (replicate && this.interconnectionService.isExternalMatch(matchResult.get)) {
        Future.successful(Left(Messages("error.E0726")))
      } else if (
        (matchResult.get.leftProfile.status == MatchStatus.deleted || matchResult.get.rightProfile.status == MatchStatus.deleted) ||
          !(matchResult.get.leftProfile.status == MatchStatus.pending || matchResult.get.rightProfile.status == MatchStatus.pending)
      ) {
        Future.successful(Left(Messages("error.E1000")))
      } else {
        val leftCode = matchResult.get.leftProfile.globalCode
        val rightCode = matchResult.get.rightProfile.globalCode
        val firingCode = getFiringCode(firingCodeParam, leftCode, rightCode, replicate)

        val isRight = rightCode == firingCode
        val userId = if (isRight) matchResult.get.rightProfile.assignee else matchResult.get.leftProfile.assignee
        val matchingProfile = if (isRight) matchResult.get.leftProfile else matchResult.get.rightProfile

        val res = matchingRepo.convertStatus(matchId, firingCode, hit)
        res.onSuccess({
          case _ => {
            traceService.add(Trace(firingCode, userId, new Date(),
              HitInfo(matchResult.get._id.id, matchingProfile.globalCode, matchingProfile.assignee, matchResult.get.`type`)))
            traceService.add(Trace(matchingProfile.globalCode, userId, new Date(),
              HitInfo(matchResult.get._id.id, firingCode, matchingProfile.assignee, matchResult.get.`type`)))
            notificationService.solve(userId, MatchingInfo(leftCode, rightCode, matchId))
            notificationService.solve(userId, MatchingInfo(rightCode, leftCode, matchId))
/*
            if (exportaALims) {
              createMatchLimsArchive(leftCode, rightCode, matchId, hit)
            }
*/

            if (replicate) {
              this.interconnectionService.convertStatus(matchId, firingCode, hit, matchResult.get)
            }
          }
        })
        res.map(result => Right(result))
      }
    }
  }

  override def getMatches(search: MatchCardSearch): Future[Seq[MatchCardForense]] = {
    profileDataRepo.getGlobalCode(search.profile.getOrElse("")).flatMap{
      case Some(globalCode)=>{
        doGetMatches(search.copy(profile = Some(globalCode.text)))
      }
      case None => {
        doGetMatches(search)
      }
    }
  }

   override def searchMatchesProfile(globalCode: String): Future[Seq[MatchCard]] = {
    matchingRepo.matchesByGlobalCode(SampleCode(globalCode)) flatMap { list =>
      Future.sequence(list.map { card =>
        val grouper = card._id.id
        for {
          pending <- matchingRepo.numberOfMatchesPending(globalCode)
          hit <- matchingRepo.numberOfMatchesHit(globalCode)
          discarded <- matchingRepo.numberOfMatchesDescarte(globalCode)
          conflict <- matchingRepo.numberOfMatchesConflic(globalCode)
          profileOpt <- this.profileFind(SampleCode(globalCode))
          profileDataOpt <- this.profileFindData(SampleCode(globalCode))
          mt <- matchingRepo.numberOfMt(globalCode)
        }yield{
          val profile = profileOpt.get
          val profileData = profileDataOpt.get
          MatchCard(SampleCode(globalCode), pending, hit, discarded, conflict, profile.contributors.getOrElse(1),
            profileData.internalSampleCode, profile.categoryId, card.matchingDate.date, profileData.laboratory,
            profile.assignee, Some(mt))
        }
      })

    }
  }


  def doGetMatches(search: MatchCardSearch): Future[Seq[MatchCardForense]] = {
    matchingRepo.getMatches(search) flatMap { list =>
     Future.sequence (list.map { card =>
         val grouper = (card \ "_id").as[String]
         val pending = (card \ MatchGlobalStatus.pending.toString).as[Int]
         val hit = (card \ MatchGlobalStatus.hit.toString).as[Int]
         val discarded = (card \ MatchGlobalStatus.discarded.toString).as[Int]
         val conflict = (card \ MatchGlobalStatus.conflict.toString).as[Int]
         val date = (card \ "lastDate").as[MongoDate]

         for {
           profileOpt <- this.profileFind(SampleCode(grouper))
           profileDataOpt <- this.profileFindData(SampleCode(grouper))
           profileMatch <- matchingRepo.getProfileLr(SampleCode(grouper), search.isCollapsing.contains(true))
           profileDataMatch <- this.profileFind(profileMatch.globalCode)
         } yield {
           var matchCardMLr = MatchCardMejorLr(profileDataMatch.get.internalSampleCode,profileMatch.categoryId,profileMatch.totalAlleles,
             profileMatch.ownerStatus,profileMatch.otherStatus,profileMatch.sharedAllelePonderation,profileMatch.mismatches,profileMatch.lr,profileDataMatch.get.globalCode, profileMatch.typeAnalisis)
           val profile = profileOpt.get
           val profileData = profileDataOpt.get
           var mC = MatchCard(profile.globalCode, pending, hit, discarded, conflict, profile.contributors.getOrElse(1),
             profileData.internalSampleCode, profile.categoryId, date.date, profileData.laboratory, profile.assignee)
           MatchCardForense(mC, matchCardMLr)

         }
       }
     )
    }
  }

  def profileFind(globalCode:SampleCode):Future[Option[Profile]] = {
    profileRepo.findByCode(globalCode).flatMap{
      case None => matchingRepo.findSuperiorProfile(globalCode)
      case Some(p) => Future.successful(Some(p))
    }
  }

  def profileFindData(globalCode: SampleCode): Future[Option[ProfileData]] = {
    profileDataRepo.findByCode(globalCode).flatMap {
      case None => matchingRepo.findSuperiorProfileData(globalCode)
      case Some(p) => Future.successful(Some(p))
    }
  }

  override def getTotalMatches(search: MatchCardSearch): Future[Int] = {
    profileDataRepo.getGlobalCode(search.profile.getOrElse("")).flatMap {
      case Some(globalCode) => {
        matchingRepo.getTotalMatches(search.copy(profile = Some(globalCode.text)))
      }
      case None => {
        matchingRepo.getTotalMatches(search)
      }
    }


  }

  override def findMatchingResults(globalCode: SampleCode): Future[Option[MatchingResults]] = {
    matchingRepo.matchesByGlobalCode(globalCode) flatMap { sMr =>
      val sMatchingResult = sMr map { mr => getMatchingResult(globalCode, mr) }
      if (sMr.isEmpty)
        Future.successful(None)
      else {
        val dr = Future.sequence(sMatchingResult)
        dr.map { seq => Some(MatchingResults("-1", globalCode, "userId no lo tengo", seq.toList)) }
      }
    }
  }

  override def findMatches(
    globalCode: SampleCode,
    matchType: Option[String] = None
  ): Unit = {
    logger.debug(s"Enqueue profile $globalCode to spark matcher")
    matchType match {
      case None =>
        spak2Matcher.findMatchesInBackGround(globalCode)
      case Some("MPI") =>
        pedigreeSparkMatcher.findMatchesInBackGround(globalCode, matchType.get)
      case _ => ()
    }
  }


  override def findMatches(pedigreeId: Long): Unit = {
    logger.debug(s"Enqueue pedigree $pedigreeId to spark matcher")
    pedigreeSparkMatcher.findMatchesInBackGround(pedigreeId)
  }

  override def resumeFindingMatches() = {
    profileRepo.getUnprocessed()
      .foreach {
        case toResume =>
          toResume.
            foreach { globalCode =>
            logger.debug(s"Resuming find matches for profile $globalCode")
              profileDataRepo.get(globalCode).map(_.map(profileData => {
                findMatches(globalCode, categoryService.getCategoryType(profileData.category))
            }))
          }
      }

    pedigreeRepo.getUnprocessed
      .foreach {
        case toResume =>
          toResume.
            foreach { pedigreeId =>
              logger.debug(s"Resuming generate genotypification and find matches for pedigree $pedigreeId")
              pedigreeGenotypificationService.generateGenotypificationAndFindMatches(pedigreeId)
            }
      }
  }

  override def validProfilesAssociated(labels: Option[Profile.LabeledGenotypification]) = {
    labels.getOrElse(Map()).keySet.filter(code => code.matches(SampleCode.validationRe.toString())).toSeq
  }
  override def getMatchResultById(matchingId: Option[String]): Future[Option[MatchResult]] = {
    matchingId match {
      case Some(id) => matchingRepo.getByMatchingProfileId(id)
      case None => Future.successful(None)
    }
  }
  override def getByMatchedProfileId(matchingId: String,isCollapsing:Option[Boolean] = None,isScreening:Option[Boolean] = None): Future[Option[JsValue]] = {
    matchingRepo.getByMatchingProfileId(matchingId,isCollapsing,isScreening) map {
      _ map { matchResult =>
        val result = Json.obj("globalCode" -> matchResult.leftProfile.globalCode,
          "stringency" -> matchResult.result.stringency,
          "matchingAlleles" -> matchResult.result.matchingAlleles,
          "allelesRanges" -> matchResult.result.allelesRanges,
          "totalAlleles" -> matchResult.result.totalAlleles,
          "categoryId" -> matchResult.result.categoryId,
          "type" -> matchResult.`type`,
          "status" -> Json.obj(matchResult.leftProfile.globalCode.text -> matchResult.leftProfile.status,
            matchResult.rightProfile.globalCode.text -> matchResult.rightProfile.status),
          "superiorProfileData"->Json.toJson(matchResult.superiorProfileInfo.map(_.superiorProfileData)),
          "superiorProfile"->Json.toJson(matchResult.superiorProfileInfo.map(_.profile)))

        Json.obj("_id" -> matchingId, "results" -> JsArray(Seq(result)))
      }
    }
  }

  private def getMatchingResult(firingCode: SampleCode, mr: MatchResult) = {
    val right = firingCode == mr.rightProfile.globalCode

    val globalCodeC = if (right)
      mr.leftProfile.globalCode
    else
      mr.rightProfile.globalCode

    profileRepo.findByCode(firingCode) flatMap { profileOption =>
      {
        val profile = profileOption.get
        profileRepo.findByCode(globalCodeC) map { matchingProfileOpt =>
          {
            val matchingProfile = matchingProfileOpt.get

            val ownerStatus = if (right) mr.rightProfile.status else mr.leftProfile.status
            val otherStatus = if (right) mr.leftProfile.status else mr.rightProfile.status

            val status = matchingRepo.getGlobalMatchStatus(mr.leftProfile.status, mr.rightProfile.status)

            MatchingResult(mr._id.id, globalCodeC, matchingProfile.internalSampleCode, mr.result.stringency,
              mr.result.matchingAlleles, mr.result.totalAlleles, matchingProfile.categoryId, ownerStatus,
              otherStatus, status, MatchingAlgorithm.uniquePonderation(mr, profile, matchingProfile),
              matchingProfile.contributors.getOrElse(1), matchingProfile.isReference, mr.result.algorithm, mr.`type`,None, mr.lr)
          }
        }
      }
    }
  }

  override def getByFiringAndMatchingProfile(firingCode: SampleCode, matchingCode: SampleCode): Future[Option[MatchingResult]] = {
    matchingRepo.getByFiringAndMatchingProfile(firingCode, matchingCode) flatMap {
      case Some(mr) => getMatchingResult(firingCode, mr).map { result => Some(result) }
      case None     => Future.successful(None)
    }
  }

  override def getComparedMixtureGenotypification(globalCodes: Seq[SampleCode],matchId:String,isCollapsing:Option[Boolean] = None,isScreening:Option[Boolean] = None): Future[Seq[CompareMixtureGenotypification]] = {
      profileRepo.findByCodes(globalCodes) flatMap {
        results =>
          if(results.size<globalCodes.size){
            matchingRepo.getByMatchingProfileId(matchId,isCollapsing,isScreening).map{ case Some(mr) =>{
              mr.superiorProfileInfo.fold(obtainCompareMixtureGenotypification(results))
                {superiorProfileInfo => obtainCompareMixtureGenotypification(results.:+(superiorProfileInfo.profile))}
            }
            case None => obtainCompareMixtureGenotypification(results)
            }
          }else{
            Future.successful(obtainCompareMixtureGenotypification(results))
          }
      }
  }
  override def obtainCompareMixtureGenotypification(results:scala.Seq[Profile]):Seq[CompareMixtureGenotypification] = {
    val types = results.foldLeft(Set[Int]()) { (a, b) => a.union(b.genotypification.keySet) }
    types.flatMap { at =>

      val loci = results.foldLeft(Set[Profile.Marker]()) { (a, b) => a.union(b.genotypification.getOrElse(at, Map.empty).keySet) }

      loci.map { locus =>

        val r = results.filter { x => x.genotypification.getOrElse(at, Map.empty).keySet.contains(locus) } map { jj =>

          val list = jj.genotypification.getOrElse(at, Map.empty).getOrElse(locus, Seq())

          (jj.globalCode.text, list)
        }
        CompareMixtureGenotypification(locus, r.toMap)
      }

    }.toSeq.sortBy(_.locus)
  }

  /*override def getComparedGenotyfications(globalCode: SampleCode, matchedGlobalCode: SampleCode): Future[Option[JsValue]] = {
    for {
      p1 <- profileRepo.getGenotyficationByCode(globalCode)
      p2 <- profileRepo.getGenotyficationByCode(matchedGlobalCode)
    } yield for {
      a <- p1
      b <- p2
    } yield getUnitedGenotyfications(a, b)
  }

  def getUnitedGenotyfications(p1: Map[Int, Profile.Genotypification], p2: Map[Int, Profile.Genotypification]): JsValue = {
    val types = p1.keySet.union(p2.keySet)



    val loci =

    val resultJson = loci.map(locus => {

      val l1 = p1.getOrElse(locus, List())
      val l2 = p2.getOrElse(locus, List())

      Json.obj(
        "locus" -> locus,
        "p1" -> l1,
        "p2" -> l2)
    })

    Json.toJson(resultJson) //Json.obj("result" -> resultJson)
  }*/

  override def validate(scenario: Scenario): Future[Either[String,String]] = {
    val sampleCodes: Set[SampleCode] = scenario.calculationScenario.prosecutor.selected.union(scenario.calculationScenario.defense.selected).toSet

    val futures = sampleCodes.map(sampleCode => {
      getByFiringAndMatchingProfile(scenario.calculationScenario.sample, sampleCode) flatMap {
        case Some(matchingResult) => convertHit(matchingResult.oid, if (!scenario.isRestricted) scenario.calculationScenario.sample else sampleCode)
        case None => Future.successful(Right(Seq(sampleCode)))
      }
    })

    Future.sequence(futures) map { result => {
      if(result.forall(_.isRight)){
        Right(scenario._id.id)
      }else{
        Left(result.filter(_.isLeft).map(x => x.left.get).mkString("",",",""))
      }
    } }
  }

  override def matchesWithPartialHit(globalCode: SampleCode): Future[Seq[MatchResult]] = {
    matchingRepo.matchesWithPartialHit(globalCode)
  }
  override def matchesNotDiscarded(globalCode: SampleCode): Future[Seq[MatchResult]] = {
    matchingRepo.matchesNotDiscarded(globalCode)
  }
  override def getTotalMatchesByGroup(search: MatchGroupSearch): Future[Int] = {
    Future { matchingRepo.getTotalMatchesByGroup(search) }
  }

  override def getMatchesByGroup(search: MatchGroupSearch): Future[Seq[MatchingResult]] = {
     val listMatchingResult = matchingRepo.getMatchesByGroup(search)
     val result =  Future.sequence(listMatchingResult.map( mr => {
      this.canUploadMatchStatus(mr.oid,search.isCollapsing).map(isInterconection => mr.copy(isInterconnectionMatch = isInterconection))
    }))
    result
  }

  def canUploadMatchFunction (mr : MatchingResult) : Boolean = {
    false
  }

  override def convertHitOrDiscard(matchId: String, firingCode: SampleCode, isSuperUser: Boolean,action:String): Future[Either[String, Seq[SampleCode]]] = {
    action match {
      case this.hit => {
        this.convertHit(matchId,firingCode,false)
      }
      case this.discarded => {
        this.convertDiscard(matchId,firingCode,isSuperUser,false)
      }
      case this.deleted => {
        this.delete(matchId).map{
          case false => {Left("Error al borrar el match")}
          case true => {Right(Nil)}
        }
      }
      case _ => {
        Future.successful(Left("Acción inválida"))
      }
    }
  }

  override def collapse(idCourtCase:Long,user:String):Unit = {
    this.spark2MatcherCollapsing.collapseInBackGround(idCourtCase,user)
  }

  override def discardCollapsingMatches(ids:List[String],courtCaseId:Long) : Future[Unit] = {
    this.matchingRepo.discardCollapsingMatches(ids,courtCaseId)
  }

  override def discardCollapsingByLeftProfile(id:String,courtCaseId:Long) : Future[Unit] = {
    this.matchingRepo.discardCollapsingByLeftProfile(id,courtCaseId)
  }
  override def discardCollapsingByRightProfile(id:String,courtCaseId:Long) : Future[Unit] = {
    this.matchingRepo.discardCollapsingByRightProfile(id,courtCaseId)
  }
  override def discardCollapsingByLeftAndRightProfile(id:String,courtCaseId:Long) : Future[Unit] = {
    this.matchingRepo.discardCollapsingByLeftAndRightProfile(id,courtCaseId)
  }
  override def findScreeningMatches(
    profile:Profile,
    queryProfiles:List[String],
    numberOfMismatches: Option[Int]
  ):Future[(Set[MatchResultScreening],Set[MatchResultScreening])]  = Future{
    val result = this
      .spark2MatcherScreening
      .findMatchesBlocking(profile, queryProfiles, numberOfMismatches)
    (
      result._1,
      (result._1 union result._2)
        .map(
          x => {
            val oldMatchId = result._3.get((x.globalCode,4))
            if (oldMatchId.isDefined) {
              x.copy(matchId = oldMatchId.get)
            } else {
              x
            }
          }
        )
    )
  }

  override def masiveGroupDiscardByGlobalCode(firingCode: SampleCode, isSuperUser: Boolean,replicate:Boolean = true) : Future[Either[String, Seq[SampleCode]]] = {
    val matches = Await.result(matchingRepo.matchesNotDiscarded(firingCode), Duration.Inf)

    if(matches.isEmpty) {
      Future.successful(Left("Sin matches"))
    } else {
      val futures = for (matchResult <- matches) yield convertDiscard(matchResult._id.id, firingCode, isSuperUser, replicate)

      Future.sequence(futures) flatMap { result => {
        if (result.forall(_.isRight)) {
          Future(Right(Seq(firingCode)))
        } else {
          Future(Left(result.filter(_.isLeft).map(x => x.left.get).mkString("", ",", "")))
        }
      }
      }
    }

  }

  def masiveGroupDiscardByMatchesList(firingCode: types.SampleCode, matches: List[String], isSuperUser: Boolean,replicate:Boolean = true) : Future[Either[String, Seq[SampleCode]]] = {
    if(matches.isEmpty) {
      Future.successful(Left("Sin matches"))
    } else {
      val futures = for (matchId <- matches) yield convertDiscard(matchId, firingCode, isSuperUser, replicate)

      Future.sequence(futures) flatMap { result => {
        if (result.forall(_.isRight)) {
          Future(Right(Seq(firingCode)))
        } else {
          Future(Left(result.filter(_.isLeft).map(x => x.left.get).mkString("", ",", "")))
        }
      }
      }
    }

  }

  def getMatchesPaginated(search: MatchCardSearch): Future[Seq[MatchResult]] = {
    this.matchingRepo.getMatchesPaginated(search)
  }

}