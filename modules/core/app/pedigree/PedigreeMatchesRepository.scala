package pedigree

import com.mongodb.client.model.{Aggregates, Filters, Projections, Sorts, Updates}
import com.mongodb.client.{MongoCollection, MongoDatabase}
import matching.MatchStatus
import org.bson.Document
import org.bson.types.ObjectId
import play.api.Logger
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

// ---------------------------------------------------------------------------
// PedigreeMatchesRepository
// ---------------------------------------------------------------------------

trait PedigreeMatchesRepository:
  def getMatches(search: PedigreeMatchCardSearch): Future[Seq[PedigreeMatch]]
  def countMatches(search: PedigreeMatchCardSearch): Future[Int]
  def getMatchById(matchingId: String): Future[Option[PedigreeMatchResult]]
  def allMatchesDiscarded(pedigreeId: Long): Future[Boolean]
  def getMatchesByGroup(search: PedigreeMatchGroupSearch): Future[Seq[PedigreeMatchResult]]
  def countMatchesByGroup(search: PedigreeMatchGroupSearch): Future[Int]
  def discardProfile(matchId: String): Future[Either[String, String]]
  def discardPedigree(matchId: String): Future[Either[String, String]]
  def deleteMatches(idPedigree: Long): Future[Either[String, Long]]
  def confirmProfile(matchId: String): Future[Either[String, String]]
  def confirmPedigree(matchId: String): Future[Either[String, String]]
  def hasPendingMatches(pedigreeId: Long): Future[Boolean]
  def hasMatches(pedigreeId: Long): Future[Boolean]
  def numberOfPendingMatches(pedigreeId: Long): Future[Int]
  def numberOfMatches(pedigreeId: Long): Future[Int]
  def profileNumberOfPendingMatchesInPedigrees(globalCode: String, pedigreesIds: Seq[Long]): Future[Int]
  def profileNumberOfPendingMatches(globalCode: String): Future[Int]
  def countProfilesHitPedigrees(globalCodes: String): Future[Int]
  def countProfilesDiscardedPedigrees(globalCodes: String): Future[Int]
  def numberOfDiscardedMatches(pedigreeId: Long): Future[Int]
  def numberOfHitMatches(pedigreeId: Long): Future[Int]
  def getMatchByPedigree(pedigreeId: Long): Future[Option[PedigreeMatch]]
  def getMatchByProfile(globalCode: String): Future[Option[PedigreeMatch]]
  def getTypeCourtCasePedigree(pedigreeId: Long): Future[Option[String]]
  def getMejorLrPedigree(idPedigree: Long): Future[Option[MatchCardMejorLrPed]]
  def getMejorLrProf(globalCode: String): Future[Option[MatchCardMejorLrPed]]
  def getMatchesByGroupPedigree(search: PedigreeMatchGroupSearch): Future[List[MatchCardPed]]
  def getAllMatchNonDiscardedByGroup(id: String, group: String): Future[Seq[PedigreeMatchResult]]

// ---------------------------------------------------------------------------
// MongoDB sync driver implementation
// ---------------------------------------------------------------------------

@jakarta.inject.Singleton
class MongoPedigreeMatchesRepository @jakarta.inject.Inject() (
  database: MongoDatabase
)(implicit ec: ExecutionContext) extends PedigreeMatchesRepository:

  private val logger: Logger = Logger(this.getClass)

  private def col: MongoCollection[Document] = database.getCollection("pedigreeMatches")

  private def parseResult(doc: Document): PedigreeMatchResult =
    Json.parse(doc.toJson()).as[PedigreeMatchResult]

  // --------------- helpers -------------------------------------------------

  /** Builds the $match filter for PedigreeMatchCardSearch (card-level queries). */
  private def buildCardMatchFilter(search: PedigreeMatchCardSearch): Document =
    val clauses = java.util.ArrayList[org.bson.conversions.Bson]()

    if !search.isSuperUser then
      clauses.add(Filters.and(
        Filters.eq("pedigree.assignee", search.user),
        Filters.ne("pedigree.status", MatchStatus.deleted.toString)
      ))
    else
      clauses.add(Filters.or(
        Filters.ne("profile.status", MatchStatus.deleted.toString),
        Filters.ne("pedigree.status", MatchStatus.deleted.toString)
      ))

    search.profile.foreach(p =>
      clauses.add(Filters.or(
        Filters.eq("profile.globalCode", p),
        Filters.eq("pedigree.globalCode", p)
      ))
    )
    search.hourFrom.foreach(d  => clauses.add(Filters.gte("matchingDate", d)))
    search.hourUntil.foreach(d => clauses.add(Filters.lte("matchingDate", d)))
    search.category.foreach(c  => clauses.add(Filters.eq("profile.categoryId", c)))
    search.caseType.foreach(ct => clauses.add(Filters.eq("pedigree.caseType", ct)))
    search.status.foreach(s    => clauses.add(Filters.eq("profile.status", s)))
    search.idCourtCase.foreach(cc => clauses.add(Filters.eq("pedigree.idCourtCase", cc)))

    if clauses.isEmpty then new Document()
    else Document.parse(Json.obj("$and" ->
      Json.arr(clauses.asScala.toSeq.map(f => Json.parse(f.toBsonDocument.toJson())): _*)).toString())

  private def buildGroupByFilter(search: PedigreeMatchGroupSearch): Document =
    val notDeleted = Filters.or(
      Filters.ne("profile.status", MatchStatus.deleted.toString),
      Filters.ne("pedigree.status", MatchStatus.deleted.toString)
    )
    val userFilter = if !search.isSuperUser then
      Filters.or(
        Filters.and(Filters.eq("profile.assignee", search.user), Filters.ne("profile.status", MatchStatus.deleted.toString)),
        Filters.and(Filters.eq("pedigree.assignee", search.user), Filters.ne("pedigree.status", MatchStatus.deleted.toString))
      )
    else notDeleted

    val statusFilter = search.status.map(s =>
      Filters.or(Filters.eq("profile.status", s), Filters.eq("pedigree.status", s))
    ).getOrElse(notDeleted)

    val idCasoFilter = search.idCourCase.map(cc =>
      Filters.eq("pedigree.idCourtCase", cc)
    ).getOrElse(notDeleted)

    val kindFilter  = Filters.eq("kind", search.kind.toString)
    val groupFilter = search.groupBy match
      case "profile"  => Filters.eq("profile.globalCode", search.id)
      case "pedigree" => Filters.eq("pedigree.idPedigree", search.id.toLong)
      case _          => new Document()

    val bsonDoc = Filters.and(userFilter, kindFilter, groupFilter, statusFilter, idCasoFilter)
      .toBsonDocument
    Document.parse(bsonDoc.toJson())

  private def sortDocForGroup(search: PedigreeMatchGroupSearch): Document =
    val dir = if search.ascending then 1 else -1
    val field = search.sortField match
      case "date"          => "matchingDate"
      case "profile"       => "profile.globalCode"
      case "category"      => "profile.categoryId"
      case "profileg"      => "profile.globalCode"
      case "unknown"       => "pedigree.unknown"
      case "profileStatus" => "profile.status"
      case "pedigreeStatus"=> "pedigree.status"
      case "compatibility" => "compatibility"
      case _               => "matchingDate"
    Document.parse(s"""{"$field": $dir}""")

  // --------------- interface implementations --------------------------------

  override def getMatches(search: PedigreeMatchCardSearch): Future[Seq[PedigreeMatch]] = Future {
    val matchFilter = buildCardMatchFilter(search)
    val groupField = if search.group == "pedigree" then "$pedigree.idPedigree" else "$profile.globalCode"
    val assigneeRef = s"$$${search.group}.assignee"
    val pipeline = java.util.Arrays.asList(
      Document.parse(s"""{"$$match": ${matchFilter.toJson()}}"""),
      Document.parse(s"""{"$$group": {"_id": "$groupField", "count": {"$$sum": 1}, "lastMatchDate": {"$$max": "$$matchingDate"}, "assignee": {"$$first": "$assigneeRef"}}}"""),
      Document.parse(s"""{"$$skip": ${search.page * search.pageSize}}"""),
      Document.parse(s"""{"$$limit": ${search.pageSize}}""")
    )
    col.aggregate(pipeline)
      .into(new java.util.ArrayList[Document]())
      .asScala.toSeq
      .flatMap { doc =>
        Json.parse(doc.toJson()).asOpt[PedigreeMatch]
      }
  }

  override def countMatches(search: PedigreeMatchCardSearch): Future[Int] = Future {
    val matchFilter = buildCardMatchFilter(search)
    val groupField = if search.group == "pedigree" then "$pedigree.idPedigree" else "$profile.globalCode"
    val pipeline = java.util.Arrays.asList(
      Document.parse(s"""{"$$match": ${matchFilter.toJson()}}"""),
      Document.parse(s"""{"$$group": {"_id": "$groupField"}}"""),
      Document.parse("""{"$count": "total"}""")
    )
    val result = col.aggregate(pipeline).into(new java.util.ArrayList[Document]()).asScala.headOption
    result.flatMap(doc => Option(doc.getInteger("total"))).map(_.toInt).getOrElse(0)
  }

  override def getMatchById(matchingId: String): Future[Option[PedigreeMatchResult]] = Future {
    val filter = Filters.eq("_id", new ObjectId(matchingId))
    Option(col.find(filter).first()).map(parseResult)
  }

  override def allMatchesDiscarded(pedigreeId: Long): Future[Boolean] = Future {
    val notActives = Filters.nin("pedigree.status", MatchStatus.discarded.toString, MatchStatus.deleted.toString)
    val notActivesP = Filters.nin("profile.status", MatchStatus.discarded.toString, MatchStatus.deleted.toString)
    val filter = Filters.and(
      Filters.eq("pedigree.idPedigree", pedigreeId),
      notActives,
      notActivesP
    )
    col.countDocuments(filter) == 0L
  }

  override def getMatchesByGroup(search: PedigreeMatchGroupSearch): Future[Seq[PedigreeMatchResult]] = Future {
    val filter = buildGroupByFilter(search)
    val sort   = sortDocForGroup(search)
    col.find(filter)
      .sort(sort)
      .skip(search.page * search.pageSize)
      .limit(search.pageSize)
      .into(new java.util.ArrayList[Document]())
      .asScala.toSeq
      .map(parseResult)
  }

  override def countMatchesByGroup(search: PedigreeMatchGroupSearch): Future[Int] = Future {
    col.countDocuments(buildGroupByFilter(search)).toInt
  }

  override def discardProfile(matchId: String): Future[Either[String, String]] = Future {
    try
      col.updateOne(
        Filters.eq("_id", new ObjectId(matchId)),
        Updates.set("profile.status", MatchStatus.discarded.toString)
      )
      Right(matchId)
    catch case e: Exception => logger.error(s"discardProfile failed for match=$matchId", e); Left("error.E0630")
  }

  override def discardPedigree(matchId: String): Future[Either[String, String]] = Future {
    try
      col.updateOne(
        Filters.eq("_id", new ObjectId(matchId)),
        Updates.set("pedigree.status", MatchStatus.discarded.toString)
      )
      Right(matchId)
    catch case e: Exception => logger.error(s"discardPedigree failed for match=$matchId", e); Left("error.E0630")
  }

  override def deleteMatches(idPedigree: Long): Future[Either[String, Long]] = Future {
    try
      col.updateMany(
        Filters.eq("pedigree.idPedigree", idPedigree),
        Updates.combine(
          Updates.set("profile.status",  MatchStatus.deleted.toString),
          Updates.set("pedigree.status", MatchStatus.deleted.toString)
        )
      )
      Right(idPedigree)
    catch case e: Exception => logger.error(s"deleteMatches failed for pedigree=$idPedigree", e); Left("error.E0630")
  }

  override def confirmProfile(matchId: String): Future[Either[String, String]] = Future {
    try
      col.updateOne(
        Filters.eq("_id", new ObjectId(matchId)),
        Updates.set("profile.status", MatchStatus.hit.toString)
      )
      Right(matchId)
    catch case e: Exception => logger.error(s"confirmProfile failed for match=$matchId", e); Left("error.E0630")
  }

  override def confirmPedigree(matchId: String): Future[Either[String, String]] = Future {
    try
      col.updateOne(
        Filters.eq("_id", new ObjectId(matchId)),
        Updates.set("pedigree.status", MatchStatus.hit.toString)
      )
      Right(matchId)
    catch case e: Exception => logger.error(s"confirmPedigree failed for match=$matchId", e); Left("error.E0630")
  }

  override def hasPendingMatches(pedigreeId: Long): Future[Boolean] = Future {
    val filter = Filters.and(
      Filters.eq("pedigree.idPedigree", pedigreeId),
      Filters.eq("pedigree.status", MatchStatus.pending.toString),
      Filters.eq("profile.status",  MatchStatus.pending.toString)
    )
    col.countDocuments(filter) > 0L
  }

  override def hasMatches(pedigreeId: Long): Future[Boolean] = Future {
    col.countDocuments(Filters.eq("pedigree.idPedigree", pedigreeId)) > 0L
  }

  override def numberOfPendingMatches(pedigreeId: Long): Future[Int] = Future {
    col.countDocuments(Filters.and(
      Filters.eq("pedigree.idPedigree", pedigreeId),
      Filters.eq("pedigree.status", MatchStatus.pending.toString),
      Filters.eq("profile.status",  MatchStatus.pending.toString)
    )).toInt
  }

  override def numberOfMatches(pedigreeId: Long): Future[Int] = Future {
    col.countDocuments(Filters.eq("pedigree.idPedigree", pedigreeId)).toInt
  }

  override def profileNumberOfPendingMatchesInPedigrees(globalCode: String, pedigreesIds: Seq[Long]): Future[Int] = Future {
    col.countDocuments(Filters.and(
      Filters.eq("profile.globalCode", globalCode),
      Filters.eq("pedigree.status", MatchStatus.pending.toString),
      Filters.eq("profile.status",  MatchStatus.pending.toString),
      Filters.in("pedigree.idPedigree", pedigreesIds.map(java.lang.Long.valueOf).asJava)
    )).toInt
  }

  override def profileNumberOfPendingMatches(globalCode: String): Future[Int] = Future {
    col.countDocuments(Filters.and(
      Filters.eq("profile.globalCode", globalCode),
      Filters.eq("pedigree.status", MatchStatus.pending.toString),
      Filters.eq("profile.status",  MatchStatus.pending.toString)
    )).toInt
  }

  override def countProfilesHitPedigrees(globalCodes: String): Future[Int] = Future {
    col.countDocuments(Filters.and(
      Filters.eq("profile.globalCode", globalCodes),
      Filters.eq("pedigree.status", MatchStatus.hit.toString),
      Filters.eq("profile.status",  MatchStatus.hit.toString)
    )).toInt
  }

  override def countProfilesDiscardedPedigrees(globalCodes: String): Future[Int] = Future {
    col.countDocuments(Filters.and(
      Filters.eq("profile.globalCode", globalCodes),
      Filters.eq("pedigree.status", MatchStatus.discarded.toString),
      Filters.eq("profile.status",  MatchStatus.discarded.toString)
    )).toInt
  }

  override def numberOfDiscardedMatches(pedigreeId: Long): Future[Int] = Future {
    col.countDocuments(Filters.and(
      Filters.eq("pedigree.idPedigree", pedigreeId),
      Filters.eq("pedigree.status", MatchStatus.discarded.toString),
      Filters.eq("profile.status",  MatchStatus.discarded.toString)
    )).toInt
  }

  override def numberOfHitMatches(pedigreeId: Long): Future[Int] = Future {
    col.countDocuments(Filters.and(
      Filters.eq("pedigree.idPedigree", pedigreeId),
      Filters.eq("pedigree.status", MatchStatus.hit.toString),
      Filters.eq("profile.status",  MatchStatus.hit.toString)
    )).toInt
  }

  override def getMatchByPedigree(pedigreeId: Long): Future[Option[PedigreeMatch]] = Future {
    val filter = Filters.eq("pedigree.idPedigree", pedigreeId)
    col.find(filter).sort(Sorts.descending("matchingDate")).limit(1)
      .into(new java.util.ArrayList[Document]()).asScala.headOption
      .map { doc =>
        val mr = parseResult(doc)
        PedigreeMatch(Left(pedigreeId), mr.matchingDate, 0, mr.pedigree.assignee)
      }
  }

  override def getMatchByProfile(globalCode: String): Future[Option[PedigreeMatch]] = Future {
    val filter = Filters.eq("profile.globalCode", globalCode)
    col.find(filter).sort(Sorts.descending("matchingDate")).limit(1)
      .into(new java.util.ArrayList[Document]()).asScala.headOption
      .map { doc =>
        val mr = parseResult(doc)
        PedigreeMatch(Right(globalCode), mr.matchingDate, 0, mr.pedigree.assignee)
      }
  }

  override def getTypeCourtCasePedigree(pedigreeId: Long): Future[Option[String]] = Future {
    val filter = Filters.eq("pedigree.idPedigree", pedigreeId)
    col.find(filter).sort(Sorts.descending("matchingDate")).limit(1)
      .into(new java.util.ArrayList[Document]()).asScala.headOption
      .map(doc => parseResult(doc).pedigree.caseType)
  }

  override def getMejorLrPedigree(idPedigree: Long): Future[Option[MatchCardMejorLrPed]] = Future {
    val filter = Filters.eq("pedigree.idPedigree", idPedigree)
    col.find(filter).sort(Sorts.descending("compatibility")).limit(1)
      .into(new java.util.ArrayList[Document]()).asScala.headOption
      .flatMap { doc =>
        Json.parse(doc.toJson()).asOpt[PedigreeCompatibilityMatch].map { x =>
          MatchCardMejorLrPed(
            x.pedigree.idPedigree.toString,
            x.profile.globalCode.text,
            x.profile.globalCode.text,
            Some(x.profile.categoryId.text),
            x.compatibility,
            x.profile.status
          )
        }
      }
  }

  override def getMejorLrProf(globalCode: String): Future[Option[MatchCardMejorLrPed]] = Future {
    val filter = Filters.eq("profile.globalCode", globalCode)
    col.find(filter).sort(Sorts.descending("compatibility")).limit(1)
      .into(new java.util.ArrayList[Document]()).asScala.headOption
      .flatMap { doc =>
        Json.parse(doc.toJson()).asOpt[PedigreeCompatibilityMatch].map { x =>
          MatchCardMejorLrPed(
            x.pedigree.idPedigree.toString,
            x.pedigree.idPedigree.toString,
            x.pedigree.idPedigree.toString,
            Some(x.pedigree.caseType),
            x.compatibility,
            x.profile.status
          )
        }
      }
  }

  override def getMatchesByGroupPedigree(search: PedigreeMatchGroupSearch): Future[List[MatchCardPed]] = Future {
    val filter = buildGroupByFilter(search)
    val sort   = sortDocForGroup(search)
    col.find(filter).sort(sort)
      .skip(search.page * search.pageSize)
      .limit(search.pageSize)
      .into(new java.util.ArrayList[Document]())
      .asScala.toList
      .flatMap { doc =>
        Json.parse(doc.toJson()).asOpt[PedigreeCompatibilityMatch].map { pd =>
          if search.groupBy == "pedigree" then
            MatchCardPed(
              pd.profile.globalCode.text,
              pd.profile.globalCode.text,
              pd.profile.categoryId.text,
              pd.compatibility,
              pd.profile.status,
              pd.pedigree.assignee,
              pd.matchingDate.date,
              pd._id,
              "",
              "",
              pd.pedigree.unknown,
              pd.mtProfile,
              pd.matchingId
            )
          else
            MatchCardPed(
              pd.pedigree.idPedigree.toString,
              pd.pedigree.idCourtCase.toString,
              pd.pedigree.caseType,
              pd.compatibility,
              pd.profile.status,
              pd.pedigree.assignee,
              pd.matchingDate.date,
              pd._id,
              "",
              "",
              pd.pedigree.unknown,
              pd.mtProfile,
              pd.matchingId,
              pd.pedigree.idPedigree.toString
            )
        }
      }
  }

  override def getAllMatchNonDiscardedByGroup(id: String, group: String): Future[Seq[PedigreeMatchResult]] = Future {
    val notEliminated = Filters.nin("pedigree.status",
      MatchStatus.discarded.toString, MatchStatus.deleted.toString, MatchStatus.hit.toString)
    val notEliminatedP = Filters.nin("profile.status",
      MatchStatus.discarded.toString, MatchStatus.deleted.toString, MatchStatus.hit.toString)
    val groupFilter = group match
      case "pedigree" => Filters.eq("pedigree.idPedigree", id.toLong)
      case "profile"  => Filters.eq("profile.globalCode", id)
      case _          => new Document()
    col.find(Filters.and(groupFilter, notEliminated, notEliminatedP))
      .sort(Sorts.descending("matchingDate"))
      .into(new java.util.ArrayList[Document]())
      .asScala.toSeq.map(parseResult)
  }
