package pedigree

import java.text.ParseException
import java.util.Date
import javax.inject.{Inject, Named, Singleton}

import akka.actor.ActorSystem
import com.mongodb.BasicDBObject
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Aggregates._
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections._
import com.mongodb.spark.MongoSpark
import com.mongodb.spark.config.{ReadConfig, WriteConfig}
import com.mongodb.util.JSON
import configdata.{CategoryService, MatchingRule, MtConfiguration}
import inbox.{NotificationService, PedigreeMatchingInfo}
import kits.LocusService
import matching.Stringency.ModerateStringency
import matching._
import org.apache.spark.rdd.RDD
import org.bson.Document
import org.bson.types.ObjectId
import pedigree.PedigreeMatchingAlgorithm.{logger, _}
import play.api.Logger
import play.api.libs.json.Json
import probability.CalculationTypeService
import profile.{Profile, ProfileRepository}
import trace._
import types.{AlphanumericId, MongoDate, MongoId, SampleCode}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import user.UserService

import scala.collection.JavaConverters._
import scala.collection.Seq
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

trait PedigreeSparkMatcher{
  def findMatchesInBackGround(idPedigree: Long): Unit
  def findMatchesInBackGround(globalCode: SampleCode, matchType: String): Unit
  def findMatchesBlocking(globalCode: SampleCode, matchType: String)
  def findMatchesBlocking(idPedigree: Long): Unit
}

@Singleton
class PedigreeSparkMatcherImpl @Inject()(
  akkaSystem: ActorSystem,
  profileRepo: ProfileRepository,
  notificationService: NotificationService,
  categoryService: CategoryService,
  matchStatusService: MatchingProcessStatus,
  traceService: TraceService,
  pedigreeRepo: PedigreeRepository,
  calculationTypeService: CalculationTypeService,
  bayesianNetworkService: BayesianNetworkService,
  @Named("mongoUri") val mongoUri: String,
  @Named("mtConfig") val mtConfiguration: MtConfiguration,
  locusService: LocusService = null,
  pedigreeDataRepository: PedigreeDataRepository = null,
  mutationRepository: MutationRepository = null,
  mutationService: MutationService = null,
  matchingServiceSpark:MatchingService = null,
  matchingRepository:MatchingRepository = null,
  pedigreeDataRepo: PedigreeDataRepository= null,
  userService: UserService = null) extends PedigreeSparkMatcher{

  private val matchingActor = akkaSystem.actorOf(PedigreeMatchingActor.props(this))

  private val logger = Logger(this.getClass)

  private val duration = Duration(100, SECONDS)

  private def getExistingMatchesRDD(input: Either[Profile, PedigreeGenogram]) = {
    val readConfig = ReadConfig(Map("uri" -> s"$mongoUri.pedigreeMatches"), None)

    val filter = input match {
      case Left(profile) => `match`(Filters.eq("profile.globalCode", profile.globalCode.text))
      case Right(pedigree) => `match`(Filters.eq("pedigree.idPedigree", pedigree._id))
    }

    val projection = project(include("_id", "profile.globalCode", "pedigree.globalCode", "pedigree.idPedigree", "type", "kind"))

    val existingMatchesRDD = MongoSpark
      .builder()
      .sparkContext(Spark2.context)
      .connector(Spark2.connector)
      .readConfig(readConfig)
      .pipeline(Seq(filter, projection))
      .build()
      .toRDD()
      .map { doc =>
        val idPedigree = doc.get("pedigree", classOf[Document]).get("idPedigree").toString
        val `type` = doc.getInteger("type")
        val profile = doc.get("profile", classOf[Document]).getString("globalCode")
        val pedigree = doc.get("pedigree", classOf[Document]).getOrDefault("globalCode", "").toString

        (idPedigree, pedigree, profile, `type`) -> doc.getObjectId("_id").toString
      }

    existingMatchesRDD

  }

  private def getMatchingRules(profile: Profile): (AlphanumericId, Seq[MatchingRule]) = {
    val rules = profile.matchingRules match {
      case Some(matchingRules) if matchingRules.nonEmpty => matchingRules
      case _ =>
        val category = categoryService.listCategories(profile.categoryId)
        if (category.pedigreeAssociation) {
          category.matchingRules.filter(mr => categoryService.listCategories(mr.categoryRelated).pedigreeAssociation)
        } else {
          Seq()
        }
    }
    (profile.categoryId, rules)
  }
  private def getScreeningProfilesRDD(screeningProfiles:Set[SampleCode]): RDD[Profile] = {
    val readConfig = ReadConfig(Map("uri" -> s"$mongoUri.profiles"), None)

    var filter =
      `match`(Filters.and(Filters.eq("deleted", false), Filters.in("globalCode", screeningProfiles.map(_.text).asJava)))

    val projection = project(include("_id", "globalCode", "assignee", "categoryId", "deleted",
      "genotypification", "labeledGenotypification", "contributors", "matchingRules", "internalSampleCode",
      "matcheable", "isReference", "analyses"))

    val docToProfile = (doc: Document) => {
      val json = Json.parse(doc.toJson())
      val result = Json.fromJson[Profile](json)
      result.fold[Profile](err => {
        throw new ParseException(s"Can't parse: ${doc.toJson()}, error is: $err", 1)
      }, identity)
    }

    MongoSpark
      .builder()
      .sparkContext(Spark2.context)
      .connector(Spark2.connector)
      .readConfig(readConfig)
      .pipeline(Seq(filter, projection))
      .build()
      .toRDD()
      .map(docToProfile)
  }
  private def getProfilesRDD(input: Either[Profile, PedigreeGenogram]): RDD[Profile] = {
    val readConfig = ReadConfig(Map("uri" -> s"$mongoUri.profiles"), None)

    /*Si es de un caso de DVI busca solo en los perfiles de restos del caso*/

    val categoriesOrProfiles: Either[Seq[AlphanumericId], Seq[SampleCode]] = input match {
      case Left(profile) => Left(getMatchingRules(profile)._2.map(_.categoryRelated))
      case Right(pedigree) => {
        val courtCase = Await.result(pedigreeDataRepository.getCourtCaseOfPedigree(pedigree._id), duration)
        if (courtCase.get.caseType == "MPI") {
          Left(categoryService.listCategories
            .filter { case (id, cat) => cat.pedigreeAssociation && id.text != "IR" && cat.tipo.contains(2)}.keys.toList)
        } else {
          //Es un caso de DVI, se obtienen los globalCodes de lo perfiles de restos del caso activos - no colapsados
          val nnProfiles = Await.result(pedigreeDataRepository.getActiveNNProfiles(courtCase.get.id), duration)
          Right(nnProfiles)
        }
      }
    }


    var filter = if (categoriesOrProfiles.isLeft) {
        `match`(Filters.and(Filters.eq("matcheable", true), Filters.eq("deleted", false), Filters.in("categoryId", categoriesOrProfiles.left.get.map(_.text).asJava)))
    } else {
      `match`(Filters.and( Filters.eq("deleted", false), Filters.in("globalCode", categoriesOrProfiles.right.get.map(_.text).asJava)))
    }

    val projection = project(include("_id", "globalCode", "assignee", "categoryId", "deleted",
      "genotypification", "labeledGenotypification", "contributors", "matchingRules", "internalSampleCode",
      "matcheable", "isReference", "analyses"))

    val docToProfile = (doc: Document) => {
      val json = Json.parse(doc.toJson())
      val result = Json.fromJson[Profile](json)
      result.fold[Profile](err => {
        throw new ParseException(s"Can't parse: ${doc.toJson()}, error is: $err", 1)
      }, identity)
    }

    MongoSpark
      .builder()
      .sparkContext(Spark2.context)
      .connector(Spark2.connector)
      .readConfig(readConfig)
      .pipeline(Seq(filter, projection))
      .build()
      .toRDD()
      .map(docToProfile)
  }

  private def getPedigrees(profile: Profile, matchType: String): Seq[(Long, String, String,Boolean/*, Seq[Individual], Option[Long]*/)] = {
    val readConf = ReadConfig(Map("uri" -> s"$mongoUri.pedigrees"), None)

    val filter = `match`(
      Filters.and(Filters.eq("caseType", matchType), Filters.in("status", Seq(PedigreeStatus.Active.toString).asJava)))

    val projection = project(include("_id", "assignee", "frequencyTable"))//, "genogram", "mutationModelId"))

    MongoSpark
      .builder()
      .sparkContext(Spark2.context)
      .connector(Spark2.connector)
      .readConfig(readConf)
      .pipeline(Seq(filter, projection))
      .build()
      .toRDD()
      .map { doc =>
        val pedigreeId = doc.getString("_id").toLong
        val assignee = doc.getString("assignee")
        val frequencyTable = doc.getString("frequencyTable")
        val executeScreeningMitochondrial = doc.getBoolean("executeScreeningMitochondrial")

        // val genogram = doc.get("genogram", Seq[Individual].getClass)
       // val mutatationModelId = doc.getOrDefault("mutationModelId", None)
        (pedigreeId, assignee, frequencyTable,executeScreeningMitochondrial == true)
      }
      .collect()
      .toList
  }

  private def getPedigreeGenotypificationRDD(pedigreeId: Long): RDD[PedigreeGenotypification] = {
    val readConfig = ReadConfig(Map("uri" -> s"$mongoUri.pedigreeGenotypification"), None)

    val filter = `match`(Filters.eq("_id", pedigreeId.toString))

    val projection = project(include("_id", "genotypification", "boundary", "frequencyTable", "unknowns"))

    val docToEntity = (doc: Document) => {
      val json = Json.parse(doc.toJson())
      val result = Json.fromJson[PedigreeGenotypification](json)
      result.fold[PedigreeGenotypification](err => {
        throw new ParseException(s"Can't parse: ${doc.toJson()}, error is: $err", 1)
      }, identity)
    }

    MongoSpark
      .builder()
      .sparkContext(Spark2.context)
      .connector(Spark2.connector)
      .readConfig(readConfig)
      .pipeline(Seq(filter, projection))
      .build()
      .toRDD()
      .map(docToEntity)
  }

  private def classifyMatches(existingMatchesRDD: RDD[((String, String, String, Integer), String)],
                              matchesRDD: RDD[PedigreeMatchResult], input: Either[Profile, PedigreeGenogram]) = {


    val mappedInput = input.right.map(pedigree => pedigree._id.toString)

    val getPedigreeGlobalCode = (mr: PedigreeMatchResult) => {
      mr match {
        case PedigreeDirectLinkMatch(_,_,_,_,pedigree,_) => pedigree.globalCode.text
        case PedigreeMissingInfoMatch(_,_,_,_,pedigree) => pedigree.globalCode.text
        case _ => ""
      }
    }

    val existingMatches = existingMatchesRDD.collect().toMap

    val matchesToInsertRDD = matchesRDD.filter { mr =>
      !existingMatches.contains(
        (mr.pedigree.idPedigree.toString, getPedigreeGlobalCode(mr), mr.profile.globalCode.text, mr.`type`))
    }.cache

    val matchesToReplaceRDD = matchesRDD
      .filter { mr =>
        existingMatches.contains(
          (mr.pedigree.idPedigree.toString, getPedigreeGlobalCode(mr), mr.profile.globalCode.text, mr.`type`))
      }
      .map { mr =>
        val id = existingMatches(
          (mr.pedigree.idPedigree.toString, getPedigreeGlobalCode(mr), mr.profile.globalCode.text, mr.`type`))
        id -> mr
      }.cache

    // Set of mongo doc id
    val matchesToReplace = matchesToReplaceRDD
      .map { case (docId, _) => docId }
      .collect()
      .toSet

    val matchesToDeleteRDD = existingMatchesRDD
      .filter {
        case ((idPedigree, _, _, _), docId) =>
          !matchesToReplace.contains(docId) && mappedInput.fold(profile => true, id => id == idPedigree)
      }.cache

    (matchesToInsertRDD, matchesToReplaceRDD, matchesToDeleteRDD)
  }

  private def saveMatches(matchesRDD: RDD[PedigreeMatchResult], input: Either[Profile, PedigreeGenogram]) = {

    val existingMatchesRDD = getExistingMatchesRDD(input)
    val (insertRDD, replaceRDD, deleteRDD) = classifyMatches(existingMatchesRDD, matchesRDD, input)

    val inputMsg = input.fold(profile => s"Profile ${profile.globalCode.text}", pedigree => s"Pedigree ${pedigree._id}")
    val inputMatch = input.fold(_ => "pedigree profiles", _ => "profiles")
    val msg = s"""
$inputMsg has matched against ${matchesRDD.count()} $inputMatch candidates
  to insert: ${insertRDD.count}
  to replace: ${replaceRDD.count}
  to delete: ${deleteRDD.count}
            """
    logger.info(msg)

    saveMatchesToDelete(deleteRDD)
    saveMatchesToReplace(replaceRDD)
    saveMatchesToInsert(insertRDD)

    sendNotifications(insertRDD, replaceRDD)
    traceMatch(input, insertRDD, replaceRDD, deleteRDD)
  }

  private def saveMatchesToInsert(matchesToInsertRDD: RDD[PedigreeMatchResult]) = {
    val matchesWriteConf = WriteConfig(Map("uri" -> s"$mongoUri.pedigreeMatches"), None)

    val matchResultToDoc = (matchResult: PedigreeMatchResult) => {
      val profile = Json.toJson(matchResult.profile).toString()
      val pedigree = Json.toJson(matchResult.pedigree).toString()
      val bson = new Document()
      val id = new ObjectId(matchResult._id.id)
      bson.put("_id", id)
      bson.put("matchingDate", matchResult.matchingDate.date)
      bson.put("profile", JSON.parse(profile))
      bson.put("pedigree", JSON.parse(pedigree))
      bson.put("type", matchResult.`type`)
      bson.put("kind", matchResult.kind.toString)
      matchResult match {
        case PedigreeDirectLinkMatch(_,_,_,_,_,result) => bson.put("result", JSON.parse(Json.toJson(result).toString))
        case PedigreeCompatibilityMatch(_,_,_,_,_,compatibility,matchingId,mtProfile, message) => {
          bson.put("compatibility", compatibility)
          bson.put("matchingId", matchingId)
          bson.put("mtProfile", mtProfile)
          bson.put("message", message)
        }
        case _ => ()
      }
      bson
    }

    MongoSpark.save(matchesToInsertRDD.map(matchResultToDoc), matchesWriteConf)
  }

  private def saveMatchesToReplace(matchesToReplaceRDD: RDD[(String, PedigreeMatchResult)]) = {

    val matchesWriteConf = WriteConfig(Map("uri" -> s"$mongoUri.pedigreeMatches"), None)

    val matchResultToDocWithId = (tup: (String, PedigreeMatchResult)) => {
      val matchResult = tup._2
      val profile = Json.toJson(matchResult.profile).toString()
      val pedigree = Json.toJson(matchResult.pedigree).toString()
      val bson = new Document()
      val id = new ObjectId(tup._1)
      bson.put("_id", id)
      bson.put("matchingDate", matchResult.matchingDate.date)
      bson.put("profile", JSON.parse(profile))
      bson.put("pedigree", JSON.parse(pedigree))
      bson.put("type", matchResult.`type`)
      bson.put("kind", matchResult.kind.toString)
      matchResult match {
        case PedigreeDirectLinkMatch(_,_,_,_,_,result) => bson.put("result", JSON.parse(Json.toJson(result).toString))
        case PedigreeCompatibilityMatch(_,_,_,_,_,compatibility,matchingId,mtProfile, message) => {
          bson.put("compatibility", compatibility)
          bson.put("matchingId", matchingId)
          bson.put("mtProfile", mtProfile)
          bson.put("message", message)
        }
        case _ => ()
      }
      bson
    }

    matchesToReplaceRDD
      .map(matchResultToDocWithId)
      .foreachPartition(iter =>
        if (iter.nonEmpty) {
          iter.grouped(512).foreach(batch => {
            Spark2.connector.withCollectionDo(matchesWriteConf, { collection: MongoCollection[Document] =>
              batch.foreach { doc =>
                val filter = new Document("_id", doc.get("_id"))
                collection.replaceOne(filter, doc)
              }
            })

          })
        })

  }

  private def saveMatchesToDelete(matchesToDeleteRDD: RDD[((String, String, String, Integer), String)]) = {
    val matchesWriteConf = WriteConfig(Map("uri" -> s"$mongoUri.pedigreeMatches"), None)

    matchesToDeleteRDD
      .foreachPartition(iter =>
        if (iter.nonEmpty) {
          iter.grouped(512).foreach(batch => {
            Spark2.connector.withCollectionDo(matchesWriteConf, { collection: MongoCollection[Document] =>
              batch.foreach { case ((_,_,_,_),id) =>
                val logicalDelete = Document.parse(s"""
{
  $$set : {
    "profile.status":  "${MatchStatus.deleted}",
    "pedigree.status": "${MatchStatus.deleted}"
  }
}
""")
                val filter = new Document("_id", new ObjectId(id))
                collection.updateOne(filter, logicalDelete)
              }
            })

          })
        })

  }

  private val findMatchesByPedigreeFunc = (pedigree: PedigreeGenogram,locusRangeMap:NewMatchingResult.AlleleMatchRange) => {

    logger.debug(s"Start pedigree matching process for pedigree ${pedigree._id}")
    matchStatusService.pushJobStatus(PedigreeMatchJobStarted)
    try{
    val pedigreeId = pedigree._id
    val assignee = pedigree.assignee
    val caseType = pedigree.caseType
    val idCourtCase = pedigree.idCourtCase

    val config = mtConfiguration

    // Compatibilidad
    val frequencyTable = Await.result(
      bayesianNetworkService
        .getFrequencyTable(pedigree.frequencyTable.get),
      duration
    )
    val codes = pedigree.genogram.flatMap(_.globalCode).toList
    val pedigreeProfiles = Await.result(
      profileRepo
        .findByCodes(codes),
      duration
    )
    var mithocondrialMatches:(
      Set[MatchResultScreening],Set[MatchResultScreening]
      ) = (Set.empty,Set.empty)
    var mtProfileCode = ""
    val executeScreeningMitochondrial = pedigree
      .executeScreeningMitochondrial
    if(executeScreeningMitochondrial) {
      val mtProfile = MatchingAlgorithm
        .getMtProfile(pedigree.genogram, pedigreeProfiles)
      if (mtProfile.isDefined) {
        mtProfileCode = mtProfile.get.globalCode.text
        val cProfile = mtProfile.get
        val pedProfilesCodes = pedigreeProfiles
          .toList
          .map(_.globalCode.text)
        mithocondrialMatches = Await
          .result(
            this
              .matchingServiceSpark
              .findScreeningMatches(
                cProfile,
                pedProfilesCodes,
                pedigree.numberOfMismatches
              ),
            duration
          )
      }
    }
    val pedigreeGenotypification = getPedigreeGenotypificationRDD(pedigreeId)
      .first()
    val alias = pedigreeGenotypification.unknowns.head

    val analysisType = Await
      .result(
        calculationTypeService.
          getAnalysisTypeByCalculation(BayesianNetwork.name),
        duration
      )

    val mutationModel: Option[MutationModel] = if(pedigree.mutationModelId.isDefined) {
      Await.result(mutationRepository.getMutationModel(pedigree.mutationModelId), duration)
    } else { None }

    val markers = profileRepo.getProfilesMarkers(pedigreeProfiles.toArray)

    val mutationModelType: Option[Long] = if (mutationModel.nonEmpty) Some(mutationModel.get.mutationType) else None
    val mutationModelData = Await.result(mutationService.getMutationModelData(mutationModel,markers),duration)
    val n = Await.result(mutationService.getAllPossibleAllelesByLocus(),duration)
    val profilesRDD = getProfilesRDD(Right(pedigree))
    val matchesRDD: RDD[PedigreeMatchResult] = profilesRDD.map(p => {
      MatchingAlgorithm.convertProfileWithConvertedOutOfLadderAlleles(p,locusRangeMap)
    }).flatMap( p => {
      val mtAnalysis = 4
      val isMt = p.genotypification.get(mtAnalysis).isDefined
      // El _2 son los matches a insertar
      val mitoMatchesMap = mithocondrialMatches._2.map(t => t.globalCode -> t.matchId).toMap
      val mitoM = mitoMatchesMap.get(p.globalCode.text)
      if(executeScreeningMitochondrial && mitoM.isDefined){
        findCompatibilityMatches(frequencyTable._2, p, pedigreeGenotypification, pedigreeId, assignee, analysisType, mutationModelType, mutationModelData,n, caseType, idCourtCase)
          .map{
            case pedigreeMatchResult:PedigreeCompatibilityMatch =>{
              Some(pedigreeMatchResult.copy(matchingId = mitoM.get,mtProfile = mtProfileCode))
            }
            case x => Some(x)
        }.flatten
      }else {
        if(executeScreeningMitochondrial && isMt && mtProfileCode.nonEmpty){
          Nil
        }else{
          findCompatibilityMatches(frequencyTable._2, p, pedigreeGenotypification, pedigreeId, assignee, analysisType, mutationModelType, mutationModelData,n, caseType,idCourtCase)
        }
      }
    }
    )

      saveMatches(matchesRDD.cache, Right(pedigree))
    // El _1 son los matches que se generaron nuevos
    deleteNotUsedScreeningMatches(matchesRDD,mithocondrialMatches._1)
    }finally {
      matchStatusService.pushJobStatus(PedigreeMatchJobEnded)
    }
  }
  def deleteNotUsedScreeningMatches(
    matchesRDD: RDD[PedigreeMatchResult],
    mithocondrialMatches: Set[MatchResultScreening]
  ) = {
    val mitocondrialMatches = matchesRDD.map{
      case pedigreeMatchResult:PedigreeCompatibilityMatch => {
        pedigreeMatchResult.matchingId
      }
      case _ => ""
    }.map(x => if(x.length== 0) None else Some(x)).collect().flatten.toSet
    val allMitocondrialMatches = mithocondrialMatches
      .filter(_.deleteable)
      .map(_.matchId)
    matchingRepository
      .discardScreeningMatches(
        allMitocondrialMatches
          .diff(mitocondrialMatches)
          .toList
      )
  }
  private val findMatchesByProfileFunc = (
    profile: Profile,
    locusRangeMap:NewMatchingResult.AlleleMatchRange,
    matchType: String
  ) => {
    logger.debug(s"Start pedigree matching process for profile ${profile.globalCode.text}")
    matchStatusService.pushJobStatus(PedigreeMatchJobStarted)
    try {
      val isReferenceProfileMatchingAnMpiCase = {
        profile.categoryId.text == "IR" && matchType == "MPI"
      }
      val pedigrees = if (isReferenceProfileMatchingAnMpiCase) {
        Seq[PedigreeGenogram]()
      } else {
        Await
          .result(
            pedigreeRepo.getActivePedigreesByCaseType(matchType),
            duration
          )
      }
      val mapOfRules = getMatchingRules(profile)
      val mtAnalysis = 4
      val isMt = profile
        .genotypification
        .get(mtAnalysis)
        .isDefined
      val config = mtConfiguration
      val frequencyTables = Await
        .result(
          bayesianNetworkService
            .getFrequencyTables(
              pedigrees
                .map(_.frequencyTable)
                .flatten
                .distinct
            ),
          duration
        )
      var mithocondrialMatches:(
        Set[MatchResultScreening],
        Set[MatchResultScreening]
      ) = (Set.empty, Set.empty)
      val matchesRDD: RDD[PedigreeMatchResult] = pedigrees.map {
        pedigree =>
          var mtProfileCode = ""
          val executeScreeningMitochondrial = pedigree.executeScreeningMitochondrial
          val pedigreeId = pedigree._id
          val assignee = pedigree.assignee
          val idCourtCase = pedigree.idCourtCase
          val completePedigree = Await.result(pedigreeRepo.get(pedigreeId), duration)
          val codes = completePedigree.get.genogram.flatMap(_.globalCode).toList
          val pedigreeProfiles = Await.result(profileRepo.findByCodes(codes), duration)
          val mutationModel: Option[MutationModel] = if(completePedigree.get.mutationModelId.isDefined) {
            Await.result(mutationRepository.getMutationModel(completePedigree.get.mutationModelId), duration)
          } else {
            None
          }
          val markers = profileRepo.getProfilesMarkers(pedigreeProfiles.toArray)
          val mutationModelType: Option[Long] = if (mutationModel.nonEmpty) Some(mutationModel.get.mutationType) else None
          val mutationModelData = Await.result(mutationService.getMutationModelData(mutationModel,markers),duration)
          val n = Await.result(mutationService.getAllPossibleAllelesByLocus(),duration)
          val caseType = completePedigree.get.caseType
          // Compatibilidad
          val pedigreeGenotypificationRDD = getPedigreeGenotypificationRDD(pedigreeId)
          val analysisType = Await
            .result(
              calculationTypeService.getAnalysisTypeByCalculation(BayesianNetwork.name),
              duration
            )
          if(executeScreeningMitochondrial && isMt){
            val mtProfile = MatchingAlgorithm
              .getMtProfile(
                pedigree.genogram,
                pedigreeProfiles
              )
            if (mtProfile.isDefined) {
              mtProfileCode = mtProfile
                .get
                .globalCode
                .text
              mithocondrialMatches = Await.result(
                this
                  .matchingServiceSpark
                  .findScreeningMatches(
                    mtProfile.get,
                    pedigreeProfiles
                      .toList
                      .map(_.globalCode.text),
                    pedigree.numberOfMismatches
                  ),
                duration
              )
            }
          }
          pedigreeGenotypificationRDD
            .flatMap {
              pedigreeGenotypification =>
                val mitoMatchesMap = mithocondrialMatches._2
                  .map(t => t.globalCode -> t.matchId)
                  .toMap
                val mitoM = mitoMatchesMap.get(profile.globalCode.text)
                if(executeScreeningMitochondrial && mitoM.isDefined){
                  findCompatibilityMatches(
                    frequencyTables(pedigreeGenotypification.frequencyTable),
                    profile,
                    pedigreeGenotypification,
                    pedigreeId,
                    assignee,
                    analysisType,
                    mutationModelType,
                    mutationModelData,
                    n,
                    caseType,
                    idCourtCase
                  )
                  .map {
                    case pedigreeMatchResult:PedigreeCompatibilityMatch =>{
                      Some(pedigreeMatchResult.copy(matchingId = mitoM.get,mtProfile = mtProfileCode))
                    }
                    case x => Some(x)
                  }.flatten
                } else {
                    if(executeScreeningMitochondrial && isMt && mtProfileCode.nonEmpty){
                      Nil
                  } else {
                      findCompatibilityMatches(
                        frequencyTables(pedigreeGenotypification.frequencyTable),
                        profile,
                        pedigreeGenotypification,
                        pedigreeId,
                        assignee,
                        analysisType,
                        mutationModelType,
                        mutationModelData,
                        n,
                        caseType,
                        idCourtCase
                      )
                  }
              }
            }
        }
        .foldLeft(
          Spark2.context.emptyRDD[PedigreeMatchResult]
        ) {
          case (prev, current) => prev.union(current)
        }
      saveMatches(matchesRDD.cache, Left(profile))
      deleteNotUsedScreeningMatches(matchesRDD, mithocondrialMatches._1)
    } finally {
      matchStatusService.pushJobStatus(PedigreeMatchJobEnded)
    }
  }

  private def sendNotifications(matchesToInsertRDD: RDD[PedigreeMatchResult], matchesToReplaceRDD: RDD[(String, PedigreeMatchResult)]) {
    val matchResultToDocWithId = (tup: (String, PedigreeMatchResult)) => {
      val pmr = tup._2
      val pedigree = Json.toJson(pmr.pedigree).toString()
      val profile = Json.toJson(pmr.profile).toString()
      val bson = new Document()
      val id = new ObjectId(tup._1)
      bson.put("_id", id)
      bson.put("pedigree", JSON.parse(pedigree))
      bson.put("profile", JSON.parse(profile))
      bson
    }

    matchesToReplaceRDD
      .map { matchResultToDocWithId }
      .collect
      .foreach { doc =>
        val matchingId = doc.getObjectId("_id").toString
        val pedigreeAssignee = doc.get("pedigree", classOf[BasicDBObject]).getString("assignee")
        val profileAssignee = doc.get("profile", classOf[BasicDBObject]).getString("assignee")
        val profile = doc.get("profile", classOf[BasicDBObject]).getString("globalCode")
        val caseType = doc.get("pedigree", classOf[BasicDBObject]).getString("caseType")
        val courtCaseId = doc.get("pedigree", classOf[BasicDBObject]).getString("idCourtCase")

        val notification =  PedigreeMatchingInfo(SampleCode(profile), Some(caseType),Some(courtCaseId))

        notificationService.push(pedigreeAssignee, notification)
        userService.sendNotifToAllSuperUsers(notification, Seq(pedigreeAssignee))
      }

    matchesToInsertRDD
      .map { mr => matchResultToDocWithId((mr._id.id, mr)) }
      .collect
      .foreach { doc =>
        val matchingId = doc.getObjectId("_id").toString
        val pedigreeAssignee = doc.get("pedigree", classOf[BasicDBObject]).getString("assignee")
        val profileAssignee = doc.get("profile", classOf[BasicDBObject]).getString("assignee")
        val profile = doc.get("profile", classOf[BasicDBObject]).getString("globalCode")
        val caseType = doc.get("pedigree", classOf[BasicDBObject]).getString("caseType")
        val courtCaseId = doc.get("pedigree", classOf[BasicDBObject]).getString("idCourtCase")

        val notification =  PedigreeMatchingInfo(SampleCode(profile), Some(caseType),Some(courtCaseId))
        notificationService.push(pedigreeAssignee, notification)
        userService.sendNotifToAllSuperUsers(notification, Seq(pedigreeAssignee))

      }
  }

  private def traceMatch(input: Either[Profile, PedigreeGenogram],
                         matchesToInsertRDD: RDD[PedigreeMatchResult], matchesToReplaceRDD: RDD[(String, PedigreeMatchResult)],
                         matchesToDeleteRDD: RDD[((String, String, String, Integer), String)]) {

    val assignee = input.fold(_.assignee, _.assignee)

    val matchResultToDocWithId = (tup: (String, PedigreeMatchResult)) => {
      val matchResult = tup._2
      val profile = Json.toJson(matchResult.profile).toString
      val pedigree = Json.toJson(matchResult.pedigree).toString
      val bson = new Document()
      val id = new ObjectId(tup._1)
      bson.put("_id", id)
      bson.put("matchingDate", matchResult.matchingDate.date)
      bson.put("profile", JSON.parse(profile))
      bson.put("pedigree", JSON.parse(pedigree))
      bson.put("type", matchResult.`type`)
      bson
    }

    matchesToDeleteRDD
      .collect
      .foreach { case ((idPedigree, pedigree, profile, analysisType), matchingId) =>{
        traceService.addTracePedigree(TracePedigree(idPedigree.toLong, assignee, new Date(),
          PedigreeMatchInfo2(matchingId, idPedigree.toLong,profile, analysisType, MatchTypeInfo.Delete)))
        pedigreeDataRepo.getPedigreeDescriptionById(idPedigree.toLong).map{
          case (pedName,courtCaseName) => traceService.add(Trace(SampleCode(profile), assignee, new Date(),
            PedigreeMatchInfo(matchingId, idPedigree.toLong, analysisType, MatchTypeInfo.Delete,courtCaseName,pedName)))
        }

      }
      }

    matchesToReplaceRDD
      .map { case (id, mr) => matchResultToDocWithId((id, mr)) }
      .collect
      .foreach { doc =>
        val matchingId = doc.getObjectId("_id").toString
        val profile = doc.get("profile", classOf[BasicDBObject]).getString("globalCode")
        val idPedigree = doc.get("pedigree", classOf[BasicDBObject]).getString("idPedigree")
        val analysisType = doc.getInteger("type")
        traceService.addTracePedigree(TracePedigree(idPedigree.toLong, assignee, new Date(),
          PedigreeMatchInfo2(matchingId, idPedigree.toLong,profile, analysisType, MatchTypeInfo.Update)))
        pedigreeDataRepo.getPedigreeDescriptionById(idPedigree.toLong).map{
          case (pedName,courtCaseName) => traceService.add(Trace(SampleCode(profile), assignee, new Date(),
              PedigreeMatchInfo(matchingId, idPedigree.toLong, analysisType, MatchTypeInfo.Update,courtCaseName,pedName)))
        }

      }

    matchesToInsertRDD
      .map { mr => matchResultToDocWithId((mr._id.id, mr)) }
      .collect
      .foreach { doc =>
        val matchingId = doc.getObjectId("_id").toString
        val profile = doc.get("profile", classOf[BasicDBObject]).getString("globalCode")
        val idPedigree = doc.get("pedigree", classOf[BasicDBObject]).getString("idPedigree")
        val analysisType = doc.getInteger("type")
        traceService.addTracePedigree(TracePedigree(idPedigree.toLong, assignee, new Date(),
          PedigreeMatchInfo2(matchingId, idPedigree.toLong,profile, analysisType, MatchTypeInfo.Insert)))
        pedigreeDataRepo.getPedigreeDescriptionById(idPedigree.toLong).map{
          case (pedName,courtCaseName) => traceService.add(Trace(SampleCode(profile), assignee, new Date(),
            PedigreeMatchInfo(matchingId, idPedigree.toLong, analysisType, MatchTypeInfo.Insert,courtCaseName,pedName)))
        }

      }

  }

  override def findMatchesInBackGround(globalCode: SampleCode, matchType: String): Unit = {
    matchingActor ! (globalCode, matchType)
  }

  override def findMatchesInBackGround(idPedigree: Long): Unit = {
    matchingActor ! idPedigree
  }

  override def findMatchesBlocking(
    globalCode: SampleCode,
    matchType: String
  ): Unit = {
    val f = profileRepo.findByCode(globalCode)
    val locusRangeMap:NewMatchingResult.AlleleMatchRange = locusService.locusRangeMap()
    val result = Await.result(f, duration)
    result.foreach {
      p =>
        val category = categoryService.getCategory(p.categoryId).get
        traceService.add(
          Trace(
            globalCode,
            p.assignee,
            new Date(),
            PedigreeMatchProcessInfo(category.matchingRules)
          )
        )
        findMatchesByProfileFunc(
          MatchingAlgorithm
            .convertProfileWithConvertedOutOfLadderAlleles(
              p,
              locusRangeMap
            ),
            locusRangeMap,
            matchType
        )
        Await.result(
          profileRepo.setMatcheableAndProcessed(globalCode),
          duration
        )
    }
  }

  override def findMatchesBlocking(idPedigree: Long): Unit = {
    val f = pedigreeRepo.get(idPedigree)
    val locusRangeMap:NewMatchingResult.AlleleMatchRange = locusService.locusRangeMap()
    val result = Await.result(f, duration)

    result.foreach { p =>

      if (p.status == PedigreeStatus.Active) {
        findMatchesByPedigreeFunc(p,locusRangeMap)
        Await.result(pedigreeRepo.setProcessed(idPedigree), duration)
      }

    }
  }
}