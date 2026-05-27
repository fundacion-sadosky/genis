package pedigree

import java.text.ParseException
import java.util.Date
import org.apache.pekko.actor.{Actor, ActorSystem, Props}
import com.mongodb.client.model.{Filters, Projections}
import com.mongodb.client.{MongoCollection, MongoDatabase}
import configdata.{CategoryService, MtConfiguration}
import inbox.{NotificationService, PedigreeMatchingInfo}
import kits.LocusService
import matching.*
import org.bson.Document
import org.bson.types.ObjectId
import play.api.Logger
import play.api.libs.json.Json
import probability.CalculationTypeService
import profile.{Profile, ProfileRepository}
import services.UserService
import trace.*
import types.{MongoDate, SampleCode}

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

// ---------------------------------------------------------------------------
// PedigreeMatcher trait (replaces PedigreeSparkMatcher)
// ---------------------------------------------------------------------------

trait PedigreeMatcher:
  def findMatchesInBackGround(idPedigree: Long): Unit
  def findMatchesInBackGround(globalCode: SampleCode, matchType: String): Unit
  def findMatchesBlocking(globalCode: SampleCode, matchType: String): Unit
  def findMatchesBlocking(idPedigree: Long): Unit

// ---------------------------------------------------------------------------
// Akka actor (unchanged structure from legacy)
// ---------------------------------------------------------------------------

private class PedigreeMatchingActor(matcher: PedigreeMatcher) extends Actor:
  private val logger = Logger(this.getClass)
  def receive: Receive =
    case (sampleCode: SampleCode, matchType: String) =>
      logger.debug(s"Dequeue profile $sampleCode with match type $matchType")
      matcher.findMatchesBlocking(sampleCode, matchType)
    case pedigreeId: Long =>
      logger.debug(s"Dequeue pedigree $pedigreeId")
      matcher.findMatchesBlocking(pedigreeId)

private object PedigreeMatchingActor:
  def props(matcher: PedigreeMatcher): Props = Props(new PedigreeMatchingActor(matcher))

// ---------------------------------------------------------------------------
// PedigreeMatcherImpl — Spark-free implementation using MongoDB sync driver
// ---------------------------------------------------------------------------

@jakarta.inject.Singleton
class PedigreeMatcherImpl @jakarta.inject.Inject() (
  akkaSystem: ActorSystem,
  profileRepo: ProfileRepository,
  notificationService: NotificationService,
  categoryService: CategoryService,
  matchStatusService: MatchingProcessStatus,
  traceService: TraceService,
  pedigreeRepo: PedigreeRepository,
  calculationTypeService: CalculationTypeService,
  bayesianNetworkService: BayesianNetworkService,
  database: MongoDatabase,
  @jakarta.inject.Named("mtConfig") val mtConfiguration: MtConfiguration,
  locusService: LocusService,
  pedigreeDataRepository: PedigreeDataRepository,
  pedigreeGenotypificationRepository: PedigreeGenotypificationRepository,
  mutationRepository: MutationRepository,
  mutationService: MutationService,
  matchingService: MatchingService,
  matchingRepository: MatchingRepository,
  userService: UserService,
  @jakarta.inject.Named("mongoBlockingEC") implicit val ec: ExecutionContext
) extends PedigreeMatcher:

  private val matchingActor = akkaSystem.actorOf(PedigreeMatchingActor.props(this))
  private val logger        = Logger(this.getClass)
  private val duration      = Duration(100, SECONDS)

  private def pedigreeMatchesCol: MongoCollection[Document] =
    database.getCollection("pedigreeMatches")

  private def profilesCol: MongoCollection[Document] =
    database.getCollection("profiles")

  // ---- helpers --------------------------------------------------------------

  private def getMatchingRules(profile: Profile) =
    val rules = profile.matchingRules match
      case Some(matchingRules) if matchingRules.nonEmpty => matchingRules
      case _ =>
        val category = Await.result(categoryService.listCategories, duration).get(profile.categoryId).get
        if category.pedigreeAssociation then
          category.matchingRules.filter(mr => Await.result(categoryService.listCategories, duration).get(mr.categoryRelated).exists(_.pedigreeAssociation))
        else Seq()
    (profile.categoryId, rules)

  /** Fetch profiles from MongoDB by category IDs. */
  private def fetchProfilesByCategories(categoryIds: Seq[types.AlphanumericId]): Seq[Profile] =
    val catTexts = categoryIds.map(_.text).asJava
    profilesCol
      .find(Filters.and(Filters.eq("matcheable", true), Filters.eq("deleted", false), Filters.in("categoryId", catTexts)))
      .into(new java.util.ArrayList[Document]()).asScala.toSeq
      .map(doc => Json.parse(doc.toJson()).as[Profile])

  /** Fetch profiles from MongoDB by globalCodes. */
  private def fetchProfilesByCodes(codes: Seq[SampleCode]): Seq[Profile] =
    profilesCol
      .find(Filters.and(Filters.eq("deleted", false), Filters.in("globalCode", codes.map(_.text).asJava)))
      .into(new java.util.ArrayList[Document]()).asScala.toSeq
      .map(doc => Json.parse(doc.toJson()).as[Profile])

  /** Load existing matches from pedigreeMatches collection. */
  private def getExistingMatches(input: Either[Profile, PedigreeGenogram]): Map[(String, String, String, Int), String] =
    val filter = input match
      case Left(profile)  => Filters.eq("profile.globalCode", profile.globalCode.text)
      case Right(pedigree) => Filters.eq("pedigree.idPedigree", pedigree._id)

    pedigreeMatchesCol.find(filter)
      .projection(Projections.include(
        "_id",
        "profile.globalCode",
        "pedigree.globalCode",
        "pedigree.idPedigree",
        "type",
        "kind"
      ))
      .into(new java.util.ArrayList[Document]()).asScala.toSeq
      .map { doc =>
        val pedigreeDoc = doc.get("pedigree", classOf[Document])
        val profileDoc  = doc.get("profile",  classOf[Document])
        val idPedigree  = pedigreeDoc.getString("idPedigree")
        val globalCode  = if pedigreeDoc.containsKey("globalCode") then pedigreeDoc.getString("globalCode") else ""
        val profCode    = profileDoc.getString("globalCode")
        val `type`      = doc.getInteger("type", 0)
        (idPedigree, globalCode, profCode, `type`) -> doc.getObjectId("_id").toString
      }
      .toMap

  private def getPedigreeGlobalCode(mr: PedigreeMatchResult): String = mr match
    case PedigreeDirectLinkMatch(_, _, _, _, pedigree, _) => pedigree.globalCode.text
    case PedigreeMissingInfoMatch(_, _, _, _, pedigree)   => pedigree.globalCode.text
    case _                                                 => ""

  // ---- persistence ----------------------------------------------------------

  private def matchResultToDoc(matchResult: PedigreeMatchResult, idOpt: Option[String] = None): Document =
    val bson = new Document()
    val id   = idOpt.map(new ObjectId(_)).getOrElse(new ObjectId(matchResult._id.id))
    bson.put("_id",          id)
    bson.put("matchingDate", matchResult.matchingDate.date)
    bson.put("profile",      Document.parse(Json.toJson(matchResult.profile).toString()))
    bson.put("pedigree",     Document.parse(Json.toJson(matchResult.pedigree).toString()))
    bson.put("type",         matchResult.`type`)
    bson.put("kind",         matchResult.kind.toString)
    matchResult match
      case PedigreeDirectLinkMatch(_, _, _, _, _, result) =>
        bson.put("result", Document.parse(Json.toJson(result).toString()))
      case PedigreeCompatibilityMatch(_, _, _, _, _, compatibility, matchingId, mtProfile, message) =>
        bson.put("compatibility", compatibility)
        bson.put("matchingId",    matchingId)
        bson.put("mtProfile",     mtProfile)
        bson.put("message",       message)
      case _ => ()
    bson

  private def saveMatches(matches: Seq[PedigreeMatchResult], input: Either[Profile, PedigreeGenogram]): Unit =
    val existing = getExistingMatches(input)

    val toInsert  = matches.filterNot(mr => existing.contains((mr.pedigree.idPedigree.toString, getPedigreeGlobalCode(mr), mr.profile.globalCode.text, mr.`type`)))
    val toReplace = matches.filter(mr => existing.contains((mr.pedigree.idPedigree.toString, getPedigreeGlobalCode(mr), mr.profile.globalCode.text, mr.`type`)))
      .map(mr => existing((mr.pedigree.idPedigree.toString, getPedigreeGlobalCode(mr), mr.profile.globalCode.text, mr.`type`)) -> mr)
    val replaceIds = toReplace.map(_._1).toSet

    val mappedInput: Either[Unit, String] = input match
      case Left(_)         => Left(())
      case Right(pedigree) => Right(pedigree._id.toString)

    val toDelete = existing.filterNot { case ((idPed, _, _, _), docId) =>
      replaceIds.contains(docId) || (
        mappedInput match
          case Left(_)   => false
          case Right(id) => id != idPed
      )
    }

    logger.info(
      s"Pedigree matching: ${matches.size} candidates, insert ${toInsert.size}, replace ${toReplace.size}, delete ${toDelete.size}"
    )

    // deletions (logical)
    toDelete.values.foreach { docId =>
      pedigreeMatchesCol.updateOne(
        Filters.eq("_id", new ObjectId(docId)),
        Document.parse(s"""{"$$set": {"profile.status": "${MatchStatus.deleted}", "pedigree.status": "${MatchStatus.deleted}"}}""")
      )
    }

    // replacements
    toReplace.foreach { case (docId, mr) =>
      val doc    = matchResultToDoc(mr, Some(docId))
      val filter = new Document("_id", new ObjectId(docId))
      pedigreeMatchesCol.replaceOne(filter, doc)
    }

    // inserts
    if toInsert.nonEmpty then
      pedigreeMatchesCol.insertMany(toInsert.map(matchResultToDoc(_)).asJava)

    sendNotifications(toInsert, toReplace)
    traceMatchResults(input, toInsert, toReplace, toDelete)

  private def sendNotifications(
    toInsert:  Seq[PedigreeMatchResult],
    toReplace: Seq[(String, PedigreeMatchResult)]
  ): Unit =
    val notify: PedigreeMatchResult => Unit = mr =>
      val notification = PedigreeMatchingInfo(mr.profile.globalCode, Some(mr.pedigree.caseType), Some(mr.pedigree.idCourtCase.toString))
      notificationService.push(mr.pedigree.assignee, notification)
      userService.sendNotifToAllSuperUsers(notification, Seq(mr.pedigree.assignee))

    toInsert.foreach(notify)
    toReplace.foreach { case (_, mr) => notify(mr) }

  private def traceMatchResults(
    input:     Either[Profile, PedigreeGenogram],
    toInsert:  Seq[PedigreeMatchResult],
    toReplace: Seq[(String, PedigreeMatchResult)],
    toDelete:  Map[(String, String, String, Int), String]
  ): Unit =
    val assignee = input.fold(_.assignee, _.assignee)

    def doTrace(matchId: String, mr: PedigreeMatchResult, action: MatchTypeInfo.Value): Unit =
      traceService.addTracePedigree(TracePedigree(
        mr.pedigree.idPedigree,
        assignee,
        new Date(),
        PedigreeMatchInfo2(matchId, mr.pedigree.idPedigree, mr.profile.globalCode.text, mr.`type`, action)
      ))
      pedigreeDataRepository.getPedigreeDescriptionById(mr.pedigree.idPedigree).foreach {
        case (pedName, courtCaseName) =>
          traceService.add(Trace(mr.profile.globalCode, assignee, new Date(),
            PedigreeMatchInfo(matchId, mr.pedigree.idPedigree, mr.`type`, action, courtCaseName, pedName)))
      }

    toDelete.foreach { case ((idPed, _, profile, analysisType), matchId) =>
      traceService.addTracePedigree(TracePedigree(
        idPed.toLong, assignee, new Date(),
        PedigreeMatchInfo2(matchId, idPed.toLong, profile, analysisType, MatchTypeInfo.Delete)
      ))
      pedigreeDataRepository.getPedigreeDescriptionById(idPed.toLong).foreach {
        case (pedName, courtCaseName) =>
          traceService.add(Trace(SampleCode(profile), assignee, new Date(),
            PedigreeMatchInfo(matchId, idPed.toLong, analysisType, MatchTypeInfo.Delete, courtCaseName, pedName)))
      }
    }
    toReplace.foreach { case (matchId, mr) => doTrace(matchId, mr, MatchTypeInfo.Update) }
    toInsert.foreach { mr => doTrace(mr._id.id, mr, MatchTypeInfo.Insert) }

  // ---- find matches by pedigree ---------------------------------------------

  private def findMatchesByPedigree(pedigree: PedigreeGenogram, locusRangeMap: NewMatchingResult.AlleleMatchRange): Unit =
    logger.debug(s"Start pedigree matching for pedigree ${pedigree._id}")
    matchStatusService.pushJobStatus(PedigreeMatchJobStarted)
    try
      val pedigreeId = pedigree._id
      val assignee   = pedigree.assignee
      val caseType   = pedigree.caseType
      val idCourtCase = pedigree.idCourtCase
      val config     = mtConfiguration
      val frequencyTable = Await.result(bayesianNetworkService.getFrequencyTable(pedigree.frequencyTable.get), duration)
      val codes      = pedigree.genogram.flatMap(_.globalCode).toList
      val pedigreeProfiles = Await.result(profileRepo.findByCodes(codes), duration)

      var mitoMatches: (Set[MatchResultScreening], Set[MatchResultScreening]) = (Set.empty, Set.empty)
      var mtProfileCode = ""
      val executeScreeningMitochondrial = pedigree.executeScreeningMitochondrial
      if executeScreeningMitochondrial then
        val mtProfile = MatchingAlgorithm.getMtProfile(pedigree.genogram, pedigreeProfiles)
        if mtProfile.isDefined then
          mtProfileCode = mtProfile.get.globalCode.text
          mitoMatches = Await.result(matchingService.findScreeningMatches(mtProfile.get, pedigreeProfiles.map(_.globalCode.text).toList, pedigree.numberOfMismatches), duration)

      val pedigreeGenotypification = Await.result(pedigreeGenotypificationRepository.get(pedigreeId), duration).get
      val analysisType = Await.result(calculationTypeService.getAnalysisTypeByCalculation(BayesianNetwork.name), duration)
      val mutationModel: Option[MutationModel] = if pedigree.mutationModelId.isDefined then
        Await.result(mutationRepository.getMutationModel(pedigree.mutationModelId), duration) else None
      val markers = profileRepo.getProfilesMarkers(pedigreeProfiles.toArray)
      val mutationModelType = mutationModel.map(_.mutationType)
      val mutationModelData = Await.result(mutationService.getMutationModelData(mutationModel, markers), duration)
      val n = Await.result(mutationService.getAllPossibleAllelesByLocus(), duration)

      val courtCase = Await.result(pedigreeDataRepository.getCourtCaseOfPedigree(pedigreeId), duration)
      val candidateProfiles: Seq[Profile] = courtCase match
        case Some(cc) if cc.caseType == "MPI" =>
          val cats = Await.result(categoryService.listCategories, duration)
          fetchProfilesByCategories(
            cats.filter { case (id, cat) =>
              cat.pedigreeAssociation && id.text != "IR" && cat.tipo.contains(2)
            }.keys.toList
          )
        case Some(cc) =>
          val nnProfiles = Await.result(pedigreeDataRepository.getActiveNNProfiles(cc.id), duration)
          fetchProfilesByCodes(nnProfiles)
        case None => Seq.empty

      val mitoMatchesMap = mitoMatches._2.map(t => t.globalCode -> t.matchId).toMap
      val mtAnalysis = 4

      val matches: Seq[PedigreeMatchResult] = candidateProfiles.map(
        MatchingAlgorithm.convertProfileWithConvertedOutOfLadderAlleles(_, locusRangeMap)
      ).flatMap { p =>
        val isMt   = p.genotypification.get(mtAnalysis).isDefined
        val mitoM  = mitoMatchesMap.get(p.globalCode.text)
        if executeScreeningMitochondrial && mitoM.isDefined then
          PedigreeMatchingAlgorithm.findCompatibilityMatches(frequencyTable._2, p, pedigreeGenotypification, pedigreeId, assignee, analysisType, mutationModelType, mutationModelData, n, caseType, idCourtCase)
            .collect { case x: PedigreeCompatibilityMatch => Some(x.copy(matchingId = mitoM.get, mtProfile = mtProfileCode)) case x => Some(x) }.flatten
        else if executeScreeningMitochondrial && isMt && mtProfileCode.nonEmpty then Nil
        else
          PedigreeMatchingAlgorithm.findCompatibilityMatches(frequencyTable._2, p, pedigreeGenotypification, pedigreeId, assignee, analysisType, mutationModelType, mutationModelData, n, caseType, idCourtCase)
      }

      saveMatches(matches, Right(pedigree))
      deleteNotUsedScreeningMatches(matches, mitoMatches._1)
    finally
      matchStatusService.pushJobStatus(PedigreeMatchJobEnded)

  // ---- find matches by profile ----------------------------------------------

  private def findMatchesByProfile(profile: Profile, locusRangeMap: NewMatchingResult.AlleleMatchRange, matchType: String): Unit =
    logger.debug(s"Start pedigree matching for profile ${profile.globalCode.text}")
    matchStatusService.pushJobStatus(PedigreeMatchJobStarted)
    try
      val isReferenceProfileMatchingMpi = profile.categoryId.text == "IR" && matchType == "MPI"
      val pedigrees: Seq[PedigreeGenogram] = if isReferenceProfileMatchingMpi then Seq.empty
        else Await.result(pedigreeRepo.getActivePedigreesByCaseType(matchType), duration)

      val frequencyTables = Await.result(
        bayesianNetworkService.getFrequencyTables(pedigrees.flatMap(_.frequencyTable).distinct),
        duration
      )

      var allMitoMatches: (Set[MatchResultScreening], Set[MatchResultScreening]) = (Set.empty, Set.empty)
      val mtAnalysis = 4
      val isMt = profile.genotypification.get(mtAnalysis).isDefined

      val matches: Seq[PedigreeMatchResult] = pedigrees.flatMap { pedigree =>
        var mtProfileCode = ""
        val pedigreeId  = pedigree._id
        val assignee    = pedigree.assignee
        val idCourtCase = pedigree.idCourtCase
        val completePedigree = Await.result(pedigreeRepo.get(pedigreeId), duration)
        val codes = completePedigree.get.genogram.flatMap(_.globalCode).toList
        val pedigreeProfiles = Await.result(profileRepo.findByCodes(codes), duration)
        val mutationModel: Option[MutationModel] = if completePedigree.get.mutationModelId.isDefined then
          Await.result(mutationRepository.getMutationModel(completePedigree.get.mutationModelId), duration) else None
        val markers = profileRepo.getProfilesMarkers(pedigreeProfiles.toArray)
        val mutationModelType = mutationModel.map(_.mutationType)
        val mutationModelData = Await.result(mutationService.getMutationModelData(mutationModel, markers), duration)
        val n = Await.result(mutationService.getAllPossibleAllelesByLocus(), duration)
        val caseType = completePedigree.get.caseType
        val pedigreeGenotypification = Await.result(pedigreeGenotypificationRepository.get(pedigreeId), duration)
        val analysisType = Await.result(calculationTypeService.getAnalysisTypeByCalculation(BayesianNetwork.name), duration)

        if pedigree.executeScreeningMitochondrial && isMt then
          val mtProfile = MatchingAlgorithm.getMtProfile(pedigree.genogram, pedigreeProfiles)
          if mtProfile.isDefined then
            mtProfileCode = mtProfile.get.globalCode.text
            allMitoMatches = Await.result(matchingService.findScreeningMatches(mtProfile.get, pedigreeProfiles.map(_.globalCode.text).toList, pedigree.numberOfMismatches), duration)

        pedigreeGenotypification.toSeq.flatMap { pg =>
          val mitoMatchesMap = allMitoMatches._2.map(t => t.globalCode -> t.matchId).toMap
          val mitoM = mitoMatchesMap.get(profile.globalCode.text)
          if pedigree.executeScreeningMitochondrial && mitoM.isDefined then
            PedigreeMatchingAlgorithm.findCompatibilityMatches(frequencyTables(pg.frequencyTable), profile, pg, pedigreeId, assignee, analysisType, mutationModelType, mutationModelData, n, caseType, idCourtCase)
              .collect { case x: PedigreeCompatibilityMatch => Some(x.copy(matchingId = mitoM.get, mtProfile = mtProfileCode)) case x => Some(x) }.flatten
          else if pedigree.executeScreeningMitochondrial && isMt && mtProfileCode.nonEmpty then Nil
          else
            PedigreeMatchingAlgorithm.findCompatibilityMatches(frequencyTables(pg.frequencyTable), profile, pg, pedigreeId, assignee, analysisType, mutationModelType, mutationModelData, n, caseType, idCourtCase)
        }
      }

      saveMatches(matches, Left(profile))
      deleteNotUsedScreeningMatches(matches, allMitoMatches._1)
    finally
      matchStatusService.pushJobStatus(PedigreeMatchJobEnded)

  private def deleteNotUsedScreeningMatches(matches: Seq[PedigreeMatchResult], mitoMatches: Set[MatchResultScreening]): Unit =
    val usedMitoIds = matches.collect {
      case x: PedigreeCompatibilityMatch if x.matchingId.nonEmpty => x.matchingId
    }.toSet
    val allDeletable = mitoMatches.filter(_.deleteable).map(_.matchId)
    matchingRepository.discardScreeningMatches(allDeletable.diff(usedMitoIds).toList)

  // ---- public interface -----------------------------------------------------

  override def findMatchesInBackGround(globalCode: SampleCode, matchType: String): Unit =
    matchingActor ! (globalCode, matchType)

  override def findMatchesInBackGround(idPedigree: Long): Unit =
    matchingActor ! idPedigree

  override def findMatchesBlocking(globalCode: SampleCode, matchType: String): Unit =
    val locusRangeMap = locusService.locusRangeMap()
    Await.result(profileRepo.findByCode(globalCode), duration).foreach { p =>
      traceService.add(Trace(globalCode, p.assignee, new Date(), PedigreeMatchProcessInfo(Await.result(categoryService.getCategory(p.categoryId), duration).get.matchingRules)))
      findMatchesByProfile(MatchingAlgorithm.convertProfileWithConvertedOutOfLadderAlleles(p, locusRangeMap), locusRangeMap, matchType)
      Await.result(profileRepo.setMatcheableAndProcessed(globalCode), duration)
    }

  override def findMatchesBlocking(idPedigree: Long): Unit =
    val locusRangeMap = locusService.locusRangeMap()
    Await.result(pedigreeRepo.get(idPedigree), duration).foreach { p =>
      if p.status == PedigreeStatus.Active then
        findMatchesByPedigree(p, locusRangeMap)
        Await.result(pedigreeRepo.setProcessed(idPedigree), duration)
    }


@jakarta.inject.Singleton
class PedigreeMatcherStub extends PedigreeMatcher:
  override def findMatchesInBackGround(idPedigree: Long): Unit = ()
  override def findMatchesInBackGround(globalCode: SampleCode, matchType: String): Unit = ()
  override def findMatchesBlocking(globalCode: SampleCode, matchType: String): Unit = ()
  override def findMatchesBlocking(idPedigree: Long): Unit = ()
