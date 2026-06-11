package matching

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsArray, JsValue, Json}
import profile.{Profile, ProfileRepository}
import profiledata.ProfileDataRepository
import types.SampleCode
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MatchingServiceImpl @Inject()(
  matchingRepo: MatchingRepository,
  profileMatcher: ProfileMatcher,
  profileRepo: ProfileRepository,
  profileDataRepo: ProfileDataRepository
)(using ec: ExecutionContext) extends MatchingService:

  override def findMatches(globalCode: SampleCode, matchType: Option[String]): Unit =
    matchType match {
      case Some("MPI") => () // pedigree matching not yet migrated
      case _           => profileMatcher.findMatchesInBackground(globalCode)
    }

  override def matchesNotDiscarded(globalCode: SampleCode): Future[Seq[MatchResult]] =
    matchingRepo.matchesNotDiscarded(globalCode)

  override def matchesWithPartialHit(globalCode: SampleCode): Future[Seq[MatchResult]] =
    matchingRepo.matchesWithPartialHit(globalCode)

  override def validProfilesAssociated(labels: Option[Profile.LabeledGenotypification]): Seq[String] =
    labels.getOrElse(Map.empty).keySet
      .filter(_.matches(SampleCode.validationRe.toString()))
      .toSeq

  override def findScreeningMatches(
    profile: Profile,
    queryProfiles: List[String],
    numberOfMismatches: Option[Int]
  ): Future[(Set[MatchResultScreening], Set[MatchResultScreening])] =
    Future.successful((Set.empty, Set.empty))

  override def collapse(idCourtCase: Long, user: String): Unit = ()

  override def discardCollapsingByLeftAndRightProfile(globalCode: String, courtCaseId: Long): Future[Unit] =
    matchingRepo.discardCollapsingByLeftAndRightProfile(globalCode, courtCaseId)

  override def discardCollapsingByRightProfile(globalCode: String, courtCaseId: Long): Future[Unit] =
    matchingRepo.discardCollapsingByRightProfile(globalCode, courtCaseId)

  override def discardCollapsingByLeftProfile(globalCode: String, courtCaseId: Long): Future[Unit] =
    matchingRepo.discardCollapsingByLeftProfile(globalCode, courtCaseId)

  // ── view methods ────────────────────────────────────────────────────────────

  override def findMatchingResults(globalCode: SampleCode): Future[Option[MatchingResults]] =
    matchingRepo.matchesByGlobalCode(globalCode).flatMap { mrs =>
      if mrs.isEmpty then Future.successful(None)
      else
        val resultsFutures = mrs.map { mr =>
          val isRight = mr.rightProfile.globalCode == globalCode
          val matchingGc = if isRight then mr.leftProfile.globalCode else mr.rightProfile.globalCode
          val ownerProf  = if isRight then mr.rightProfile else mr.leftProfile
          val otherProf  = if isRight then mr.leftProfile  else mr.rightProfile

          profileRepo.findByCode(matchingGc).map { profileOpt =>
            val contributors   = profileOpt.flatMap(_.contributors).getOrElse(1)
            val isReference    = profileOpt.exists(_.isReference)
            val internalCode   = profileOpt.map(_.internalSampleCode).getOrElse(matchingGc.text)
            val sharedAlleles  = if isRight then mr.result.rightPonderation else mr.result.leftPonderation
            MatchingResult(
              mr._id.id, otherProf.globalCode, internalCode,
              mr.result.stringency, mr.result.matchingAlleles, mr.result.totalAlleles,
              otherProf.categoryId, ownerProf.status, otherProf.status,
              matchingRepo.getGlobalMatchStatus(ownerProf.status, otherProf.status),
              sharedAlleles, contributors, isReference, mr.result.algorithm, mr.`type`,
              mr.result.allelesRanges, mr.lr, mr.mismatches
            )
          }
        }
        Future.sequence(resultsFutures).map { results =>
          Some(MatchingResults("", globalCode, "", results.toList))
        }
    }

  override def getMatches(search: MatchCardSearch): Future[Seq[MatchCardForense]] =
    profileDataRepo.getGlobalCode(search.profile.getOrElse("")).flatMap {
      case Some(gc) => matchingRepo.getMatches(search.copy(profile = Some(gc.text)))
      case None     => matchingRepo.getMatches(search)
    }

  override def getTotalMatches(search: MatchCardSearch): Future[Int] =
    profileDataRepo.getGlobalCode(search.profile.getOrElse("")).flatMap {
      case Some(gc) => matchingRepo.getTotalMatches(search.copy(profile = Some(gc.text)))
      case None     => matchingRepo.getTotalMatches(search)
    }

  override def getMatchesByGroup(search: MatchGroupSearch): Future[Seq[MatchingResult]] =
    Future(matchingRepo.getMatchesByGroup(search)).flatMap { mrs =>
      Future.sequence(mrs.map { mr =>
        canUploadMatchStatus(mr.oid, search.isCollapsing, None)
          .map(isIntercon => mr.copy(isInterconnectionMatch = isIntercon))
      })
    }

  override def getTotalMatchesByGroup(search: MatchGroupSearch): Future[Int] =
    Future(matchingRepo.getTotalMatchesByGroup(search))

  override def searchMatchesProfile(globalCode: String): Future[Seq[MatchCard]] =
    matchingRepo.matchesByGlobalCode(SampleCode(globalCode)).flatMap { mrs =>
      Future.sequence(mrs.map { mr =>
        for {
          pending   <- matchingRepo.numberOfMatchesPending(globalCode)
          hit       <- matchingRepo.numberOfMatchesHit(globalCode)
          discarded <- matchingRepo.numberOfMatchesDescarte(globalCode)
          conflict  <- matchingRepo.numberOfMatchesConflic(globalCode)
          profileOpt     <- profileRepo.findByCode(SampleCode(globalCode))
          profileDataOpt <- profileDataRepo.findByCode(SampleCode(globalCode))
          mt        <- matchingRepo.numberOfMt(globalCode)
        } yield {
          val contributors   = profileOpt.flatMap(_.contributors).getOrElse(1)
          val internalCode   = profileDataOpt.map(_.internalSampleCode).getOrElse(globalCode)
          val category       = profileOpt.map(_.categoryId).getOrElse(types.AlphanumericId(""))
          val laboratory     = profileDataOpt.map(_.laboratory).getOrElse("")
          val assignee       = profileOpt.map(_.assignee).getOrElse("")
          MatchCard(SampleCode(globalCode), pending, hit, discarded, conflict, contributors,
            internalCode, category, mr.matchingDate.date, laboratory, assignee, Some(mt))
        }
      }).map(_.distinctBy(_.globalCode))
    }

  override def getByMatchedProfileId(
    matchingId: String,
    isCollapsing: Option[Boolean],
    isScreening: Option[Boolean]
  ): Future[Option[JsValue]] =
    matchingRepo.getByMatchingProfileId(matchingId, isCollapsing, isScreening).map {
      _.map { mr =>
        val supInfo = mr.superiorProfileInfo   // Option[JsValue]
        val result = Json.obj(
          "globalCode"       -> mr.leftProfile.globalCode,
          "stringency"       -> mr.result.stringency,
          "matchingAlleles"  -> mr.result.matchingAlleles,
          "allelesRanges"    -> mr.result.allelesRanges,
          "totalAlleles"     -> mr.result.totalAlleles,
          "categoryId"       -> mr.result.categoryId,
          "type"             -> mr.`type`,
          "status"           -> Json.obj(
            mr.leftProfile.globalCode.text  -> mr.leftProfile.status,
            mr.rightProfile.globalCode.text -> mr.rightProfile.status
          ),
          "superiorProfileData" -> supInfo.map(si => (si \ "superiorProfileData").getOrElse(play.api.libs.json.JsNull)),
          "superiorProfile"     -> supInfo.map(si => (si \ "profile").getOrElse(play.api.libs.json.JsNull))
        )
        Json.obj("_id" -> matchingId, "results" -> JsArray(Seq(result)))
      }
    }

  override def getComparedMixtureGenotypification(
    globalCodes: Seq[SampleCode],
    matchId: String,
    isCollapsing: Option[Boolean],
    isScreening: Option[Boolean]
  ): Future[Seq[CompareMixtureGenotypification]] =
    profileRepo.findByCodes(globalCodes).flatMap { profiles =>
      if profiles.size < globalCodes.size then
        matchingRepo.getByMatchingProfileId(matchId, isCollapsing, isScreening).map {
          case Some(mr) =>
            // superiorProfileInfo is JsValue — parse the embedded profile if present
            val supProfileOpt = mr.superiorProfileInfo.flatMap(si => (si \ "profile").validate[Profile].asOpt)
            val withSup = supProfileOpt.fold(profiles)(sp => profiles :+ sp)
            obtainCompareMixtureGenotypification(withSup)
          case None =>
            obtainCompareMixtureGenotypification(profiles)
        }
      else
        Future.successful(obtainCompareMixtureGenotypification(profiles))
    }

  private def obtainCompareMixtureGenotypification(profiles: Seq[Profile]): Seq[CompareMixtureGenotypification] =
    val types = profiles.foldLeft(Set[Int]())((a, b) => a union b.genotypification.keySet)
    types.flatMap { at =>
      val loci = profiles.foldLeft(Set[Profile.Marker]())((a, b) => a union b.genotypification.getOrElse(at, Map.empty).keySet)
      loci.map { locus =>
        val r = profiles.filter(_.genotypification.getOrElse(at, Map.empty).contains(locus))
          .map(p => p.globalCode.text -> p.genotypification.getOrElse(at, Map.empty).getOrElse(locus, Seq()))
        CompareMixtureGenotypification(locus, r.toMap)
      }
    }.toSeq.sortBy(_.locus)

  // ── status mutations ─────────────────────────────────────────────────────────

  override def convertHit(
    matchId: String,
    firingCode: SampleCode,
    replicate: Boolean,
    userName: String
  ): Future[Either[String, Seq[SampleCode]]] =
    matchingRepo.getByMatchingProfileId(matchId).flatMap {
      case None => Future.successful(Left("error.E0500"))
      case Some(mr) =>
        val isRight       = mr.rightProfile.globalCode == firingCode
        val matchingProf  = if isRight then mr.leftProfile else mr.rightProfile
        for {
          c1 <- matchingRepo.convertStatus(matchId, firingCode, MatchStatus.hit.toString)
          c2 <- matchingRepo.convertStatus(matchId, matchingProf.globalCode, MatchStatus.hit.toString)
        } yield Right(c1 ++ c2)
    }

  override def convertDiscard(
    matchId: String,
    firingCode: SampleCode,
    isSuperUser: Boolean,
    replicate: Boolean,
    userName: String
  ): Future[Either[String, Seq[SampleCode]]] =
    matchingRepo.getByMatchingProfileId(matchId).flatMap {
      case None => Future.successful(Left("error.E0500"))
      case Some(mr) =>
        val isRight      = mr.rightProfile.globalCode == firingCode
        val matchingProf = if isRight then mr.leftProfile else mr.rightProfile
        for {
          c1 <- matchingRepo.convertStatus(matchId, firingCode, MatchStatus.discarded.toString)
          c2 <- matchingRepo.convertStatus(matchId, matchingProf.globalCode, MatchStatus.discarded.toString)
        } yield Right(c1 ++ c2)
    }

  override def uploadStatus(matchId: String, firingCode: SampleCode, isSuperUser: Boolean, userName: String): Future[String] =
    Future.successful("Se envio la actualizacion de estado de match")

  override def canUploadMatchStatus(matchId: String, isCollapsing: Option[Boolean], isScreening: Option[Boolean]): Future[Boolean] =
    Future.successful(false)

  override def masiveGroupDiscardByGlobalCode(
    firingCode: SampleCode,
    isSuperUser: Boolean,
    replicate: Boolean,
    userName: String
  ): Future[Either[String, Seq[SampleCode]]] =
    matchingRepo.matchesNotDiscarded(firingCode).flatMap { mrs =>
      if mrs.isEmpty then Future.successful(Left("Sin matches"))
      else
        Future.sequence(mrs.map(mr => convertDiscard(mr._id.id, firingCode, isSuperUser, replicate, userName)))
          .map { results =>
            if results.forall(_.isRight) then Right(Seq(firingCode))
            else Left(results.collect { case Left(e) => e }.mkString(","))
          }
    }

  override def masiveGroupDiscardByMatchesList(
    firingCode: SampleCode,
    matchIds: List[String],
    isSuperUser: Boolean,
    replicate: Boolean,
    userName: String
  ): Future[Either[String, Seq[SampleCode]]] =
    if matchIds.isEmpty then Future.successful(Left("Sin matches"))
    else
      Future.sequence(matchIds.map(id => convertDiscard(id, firingCode, isSuperUser, replicate, userName)))
        .map { results =>
          if results.forall(_.isRight) then Right(Seq(firingCode))
          else Left(results.collect { case Left(e) => e }.mkString(","))
        }

  override def getMatchResultById(matchingId: Option[String]): Future[Option[MatchResult]] =
    matchingId.fold(Future.successful(Option.empty[MatchResult]))(
      id => matchingRepo.getByMatchingProfileId(id)
    )