package matching

import java.util.{Arrays => JArrays, Date}
import javax.inject.{Inject, Singleton}

import com.mongodb.client.{MongoCollection, MongoDatabase}
import com.mongodb.client.model.{Aggregates, Filters, Projections, Sorts, Updates}
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import play.api.Logger
import play.api.libs.json.{Json, JsValue}

import profile.Profile
import profiledata.ProfileData
import types.{AlphanumericId, SampleCode}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

/** MongoDB sync driver implementation of the matching repository.
 *
 *  Structural changes from legacy (ReactiveMongo + MongoSpark):
 *    - All queries use the sync MongoDB Java driver 5.x (same driver as MongoProfileRepository).
 *    - Upsert uses `replaceOne` with `upsert=true` option instead of ReactiveMongo's `findAndUpdate`.
 *    - `findSuperiorProfile` / `findSuperiorProfileData` search the embedded
 *      `superiorProfileInfo` sub-document as in legacy.
 */
@Singleton
class MongoMatchingRepository @Inject()(
  database: MongoDatabase
)(using ec: ExecutionContext) extends MatchingRepository {

  private val logger = Logger(this.getClass)

  private def matches: MongoCollection[Document] = database.getCollection("matches")

  private def docToMatchResult(doc: Document): MatchResult =
    Json.parse(doc.toJson()).as[MatchResult]

  // ── core trait ──────────────────────────────────────────────────────────────

  override def matchesNotDiscarded(globalCode: SampleCode): Future[Seq[MatchResult]] = Future {
    val filter = Filters.and(
      Filters.or(
        Filters.eq("leftProfile.globalCode", globalCode.text),
        Filters.eq("rightProfile.globalCode", globalCode.text)
      ),
      Filters.ne("leftProfile.status",  MatchStatus.deleted.toString),
      Filters.ne("rightProfile.status", MatchStatus.deleted.toString),
      Filters.or(
        Filters.ne("leftProfile.status",  MatchStatus.discarded.toString),
        Filters.ne("rightProfile.status", MatchStatus.discarded.toString)
      )
    )
    matches.find(filter).into(new java.util.ArrayList[Document]()).asScala.map(docToMatchResult).toSeq
  }

  override def matchesWithPartialHit(globalCode: SampleCode): Future[Seq[MatchResult]] = Future {
    val profileFilter = Filters.or(
      Filters.eq("leftProfile.globalCode", globalCode.text),
      Filters.eq("rightProfile.globalCode", globalCode.text)
    )
    val hitFilter = Filters.or(
      Filters.eq("leftProfile.status",  MatchStatus.hit.toString),
      Filters.eq("rightProfile.status", MatchStatus.hit.toString)
    )
    matches.find(Filters.and(profileFilter, hitFilter))
      .into(new java.util.ArrayList[Document]()).asScala.map(docToMatchResult).toSeq
  }

  override def matchesWithFullHit(globalCode: SampleCode): Future[Seq[MatchResult]] = Future {
    val profileFilter = Filters.or(
      Filters.eq("leftProfile.globalCode", globalCode.text),
      Filters.eq("rightProfile.globalCode", globalCode.text)
    )
    val hitFilter = Filters.and(
      Filters.eq("leftProfile.status",  MatchStatus.hit.toString),
      Filters.eq("rightProfile.status", MatchStatus.hit.toString)
    )
    matches.find(Filters.and(profileFilter, hitFilter))
      .into(new java.util.ArrayList[Document]()).asScala.map(docToMatchResult).toSeq
  }

  override def getByFiringAndMatchingProfile(firingCode: SampleCode, matchingCode: SampleCode): Future[Option[MatchResult]] = Future {
    val filter = Filters.or(
      Filters.and(Filters.eq("leftProfile.globalCode", firingCode.text),  Filters.eq("rightProfile.globalCode", matchingCode.text)),
      Filters.and(Filters.eq("leftProfile.globalCode", matchingCode.text), Filters.eq("rightProfile.globalCode", firingCode.text))
    )
    Option(matches.find(filter).first()).map(docToMatchResult)
  }

  override def removeMatchesByProfile(globalCode: SampleCode): Future[Either[String, String]] = Future {
    val filter = Filters.or(
      Filters.eq("leftProfile.globalCode",  globalCode.text),
      Filters.eq("rightProfile.globalCode", globalCode.text)
    )
    val update = Updates.combine(
      Updates.set("leftProfile.status",  MatchStatus.deleted.toString),
      Updates.set("rightProfile.status", MatchStatus.deleted.toString)
    )
    matches.updateMany(filter, update)
    Right(globalCode.text)
  }

  override def findSuperiorProfile(globalCode: SampleCode): Future[Option[Profile]] = Future {
    Option(matches.find(Filters.eq("superiorProfileInfo.profile.globalCode", globalCode.text)).first())
      .flatMap { doc =>
        val json = Json.parse(doc.toJson())
        Json.fromJson[MatchResult](json).fold(_ => None, mr => mr.superiorProfileInfo.flatMap { si =>
          Json.fromJson[MatchResult](si).fold(_ => None, nested => None) // superiorProfileInfo is JsValue
        })
      }
      // Fallback: parse superiorProfileInfo.profile directly
      .orElse {
        Option(matches.find(Filters.eq("superiorProfileInfo.profile.globalCode", globalCode.text)).first())
          .flatMap { doc =>
            val si = doc.get("superiorProfileInfo", classOf[Document])
            if (si == null) None
            else {
              val profileDoc = si.get("profile", classOf[Document])
              if (profileDoc == null) None
              else scala.util.Try(Json.parse(profileDoc.toJson()).as[Profile]).toOption
            }
          }
      }
  }

  override def findSuperiorProfileData(globalCode: SampleCode): Future[Option[ProfileData]] =
    Future.successful(None) // Superior instance data not used in standalone core

  // TODO: migrar lógica real desde legacy. new-dev sólo lo tenía stubbeado (no-op).
  override def discardScreeningMatches(matchIds: List[String]): Unit = ()

  override def numberOfMatches(globalCode: String): Future[Int] = Future {
    val filter = Filters.or(
      Filters.eq("leftProfile.globalCode",  globalCode),
      Filters.eq("rightProfile.globalCode", globalCode)
    )
    matches.countDocuments(filter).toInt
  }

  override def getByDateBetween(from: Option[Date], to: Option[Date]): Future[Seq[MatchResult]] = Future {
    val filters = new java.util.ArrayList[org.bson.conversions.Bson]()
    filters.add(Filters.ne("leftProfile.status",  MatchStatus.deleted.toString))
    filters.add(Filters.ne("rightProfile.status", MatchStatus.deleted.toString))
    from.foreach(f => filters.add(Filters.gte("matchingDate", f)))
    to.foreach(t   => filters.add(Filters.lte("matchingDate", t)))
    val filter = if (filters.size() == 1) filters.get(0) else Filters.and(filters)
    matches.find(filter).into(new java.util.ArrayList[Document]()).asScala.map(docToMatchResult).toSeq
  }

  // ── extra (used by ProfileMatcher) ─────────────────────────────────────────

  /** Returns existing matches for a profile as Map[(matchedGlobalCode, analysisType) -> matchId]. */
  def getExistingMatches(globalCode: SampleCode): Map[(String, Int), String] = {
    val filter = Filters.or(
      Filters.eq("leftProfile.globalCode",  globalCode.text),
      Filters.eq("rightProfile.globalCode", globalCode.text)
    )
    matches.find(filter).into(new java.util.ArrayList[Document]()).asScala.map { doc =>
      val left  = doc.get("leftProfile",  classOf[Document]).getString("globalCode")
      val right = doc.get("rightProfile", classOf[Document]).getString("globalCode")
      val gc    = if (left == globalCode.text) right else left
      val typ   = doc.getInteger("type").toInt
      val id    = doc.getObjectId("_id").toString
      (gc, typ) -> id
    }.toMap
  }

  /** Upsert a match result by its _id. */
  def upsertMatch(matchResult: MatchResult): Unit = {
    val doc = buildMatchDocument(matchResult)
    val filter = Filters.eq("_id", new ObjectId(matchResult._id.id))
    val opts   = com.mongodb.client.model.ReplaceOptions().upsert(true)
    matches.replaceOne(filter, doc, opts)
  }

  /** Logical-delete a set of match document ids. */
  def markMatchesDeleted(ids: Iterable[String]): Unit = {
    ids.foreach { id =>
      val filter = Filters.eq("_id", new ObjectId(id))
      val update = Updates.combine(
        Updates.set("leftProfile.status",  MatchStatus.deleted.toString),
        Updates.set("rightProfile.status", MatchStatus.deleted.toString)
      )
      matches.updateOne(filter, update)
    }
  }

  // ── helpers ─────────────────────────────────────────────────────────────────

  private def buildMatchDocument(mr: MatchResult): Document = {
    val json = Json.toJson(mr).toString()
    val doc  = Document.parse(json)
    // Replace the {"$oid":"..."} id representation with an ObjectId
    doc.put("_id", new ObjectId(mr._id.id))
    doc
  }

  // ── new methods ─────────────────────────────────────────────────────────────

  override def matchesByGlobalCode(globalCode: SampleCode): Future[Seq[MatchResult]] = Future {
    val filter = Filters.and(
      Filters.or(
        Filters.eq("leftProfile.globalCode",  globalCode.text),
        Filters.eq("rightProfile.globalCode", globalCode.text)
      ),
      Filters.ne("leftProfile.status",  MatchStatus.deleted.toString),
      Filters.ne("rightProfile.status", MatchStatus.deleted.toString)
    )
    matches.find(filter).into(new java.util.ArrayList[Document]()).asScala.map(docToMatchResult).toSeq
  }

  override def getByMatchingProfileId(
    matchingId: String,
    isCollapsing: Option[Boolean] = None,
    isScreening: Option[Boolean] = None
  ): Future[Option[MatchResult]] = Future {
    // core only has the main matches collection; collapsing/screening not yet migrated
    Option(matches.find(Filters.eq("_id", new ObjectId(matchingId))).first())
      .map(docToMatchResult)
  }

  override def convertStatus(matchId: String, firingCode: SampleCode, status: String): Future[Seq[SampleCode]] = Future {
    val filter = Filters.and(
      Filters.eq("_id", new ObjectId(matchId)),
      Filters.or(
        Filters.eq("leftProfile.globalCode",  firingCode.text),
        Filters.eq("rightProfile.globalCode", firingCode.text)
      )
    )
    Option(matches.find(filter).first()).fold[Seq[SampleCode]](Nil) { doc =>
      val mr        = docToMatchResult(doc)
      val isRight   = mr.rightProfile.globalCode == firingCode
      val field     = if (isRight) "rightProfile.status" else "leftProfile.status"
      val code      = if (isRight) mr.rightProfile.globalCode else mr.leftProfile.globalCode
      matches.updateOne(Filters.eq("_id", new ObjectId(matchId)), Updates.set(field, status))
      Seq(code)
    }
  }

  override def getGlobalMatchStatus(left: MatchStatus.Value, right: MatchStatus.Value): MatchGlobalStatus.Value =
    (left, right) match {
      case (MatchStatus.pending,   _)                        => MatchGlobalStatus.pending
      case (_,                     MatchStatus.pending)      => MatchGlobalStatus.pending
      case (MatchStatus.hit,       MatchStatus.hit)          => MatchGlobalStatus.hit
      case (MatchStatus.discarded, MatchStatus.discarded)    => MatchGlobalStatus.discarded
      case _                                                 => MatchGlobalStatus.conflict
    }

  // ── getMatches / getTotalMatches (MongoDB aggregation replacing Spark) ──────

  private def buildMatchesAggrPipeline(search: MatchCardSearch): Seq[Bson] = {
    // Base filter: exclude deleted, honour isCollapsing+courtCase
    val baseFilter: Bson =
      if (search.isCollapsing.contains(true) && search.courtCaseId.isDefined)
        Filters.and(
          Filters.ne("leftProfile.status",  MatchStatus.deleted.toString),
          Filters.ne("rightProfile.status", MatchStatus.deleted.toString),
          Filters.eq("idCourtCase", search.courtCaseId.get)
        )
      else
        Filters.and(
          Filters.ne("leftProfile.status",  MatchStatus.deleted.toString),
          Filters.ne("rightProfile.status", MatchStatus.deleted.toString)
        )

    // Assignee restriction for non-super-users
    val matchFilter: Bson =
      if (search.isSuperUser) baseFilter
      else Filters.and(
        baseFilter,
        Filters.or(
          Filters.eq("leftProfile.assignee",  search.user),
          Filters.eq("rightProfile.assignee", search.user)
        )
      )

    // Additional post-group filters (applied after grouping, _id = ownerCodes)
    val profileFilter: Option[Bson]  = search.profile.map(gc => Filters.eq("_id", gc))
    val labFilter: Option[Bson]      = search.laboratoryCode.map(lc => Filters.regex("_id", s".*-$lc-.*"))
    val fromFilter: Option[Bson]     = search.hourFrom.map(d  => Filters.gte("lastDate", d))
    val untilFilter: Option[Bson]    = search.hourUntil.map(d  => Filters.lte("lastDate", d))
    val catFilter: Option[Bson]      = search.categoria.map(c  => Filters.eq("category", c))
    val statusFilter: Option[Bson]   = search.status.map(s     => Filters.gt(s.toString, 0))

    val postFilters: List[Bson] = List(profileFilter, labFilter, fromFilter, untilFilter, catFilter, statusFilter).flatten
    val postMatchStage: Option[Bson] = if (postFilters.isEmpty) None else Some(Aggregates.`match`(Filters.and(postFilters*)))

    // Mirrors the legacy two-pass pipeline from MatchingRepository.prepareDataQuery:
    // (1) add an ownerCodes array with the globalCode(s) that "own" this match from the user's view,
    // (2) unwind to produce one document per owning profile per match,
    // (3) group by ownerCode accumulating status counts from both left and right positions.
    // This ensures a profile's pending/hit/discarded/conflict counts are correct regardless of
    // whether it appears as the left or right side of the stored match document.
    val addOwnerCodesStage: Document =
      if (search.isSuperUser)
        Document.parse("""{
          "$addFields": {
            "ownerCodes": ["$leftProfile.globalCode", "$rightProfile.globalCode"]
          }
        }""")
      else
        Document.parse(s"""{
          "$$addFields": {
            "ownerCodes": {
              "$$cond": [
                {"$$and": [
                  {"$$eq": ["$$leftProfile.assignee",  "${search.user}"]},
                  {"$$eq": ["$$rightProfile.assignee", "${search.user}"]}
                ]},
                ["$$leftProfile.globalCode", "$$rightProfile.globalCode"],
                {"$$cond": [
                  {"$$eq": ["$$leftProfile.assignee", "${search.user}"]},
                  ["$$leftProfile.globalCode"],
                  ["$$rightProfile.globalCode"]
                ]}
              ]
            }
          }
        }""")

    val unwindOwnerCodesStage = Aggregates.unwind("$ownerCodes")

    val groupStage = Document.parse(s"""{
      "$$group": {
        "_id": "$$ownerCodes",
        "lastDate": {"$$max": "$$matchingDate"},
        "category": {
          "$$first": {
            "$$cond": [{"$$eq": ["$$ownerCodes", "$$leftProfile.globalCode"]},
                       "$$leftProfile.categoryId",
                       "$$rightProfile.categoryId"]
          }
        },
        "${MatchGlobalStatus.pending}":   {"$$sum": {"$$cond": [{"$$or": [{"$$eq":["$$leftProfile.status","${MatchStatus.pending}"]},{"$$eq":["$$rightProfile.status","${MatchStatus.pending}"]}]}, 1, 0]}},
        "${MatchGlobalStatus.hit}":       {"$$sum": {"$$cond": [{"$$and":[{"$$eq":["$$leftProfile.status","${MatchStatus.hit}"]},{"$$eq":["$$rightProfile.status","${MatchStatus.hit}"]}]}, 1, 0]}},
        "${MatchGlobalStatus.discarded}": {"$$sum": {"$$cond": [{"$$and":[{"$$eq":["$$leftProfile.status","${MatchStatus.discarded}"]},{"$$eq":["$$rightProfile.status","${MatchStatus.discarded}"]}]}, 1, 0]}},
        "${MatchGlobalStatus.conflict}":  {"$$sum": {"$$cond": [{"$$or": [{"$$and":[{"$$eq":["$$leftProfile.status","${MatchStatus.hit}"]},{"$$eq":["$$rightProfile.status","${MatchStatus.discarded}"]}]},{"$$and":[{"$$eq":["$$leftProfile.status","${MatchStatus.discarded}"]},{"$$eq":["$$rightProfile.status","${MatchStatus.hit}"]}]}]}, 1, 0]}}
      }
    }""")

    val sortDoc = Document.parse(s"""{"$$sort": {"lastDate": ${if (search.ascending) 1 else -1}, "_id": 1}}""")

    val pipeline = scala.collection.mutable.Buffer[Bson](
      Aggregates.`match`(matchFilter),
      addOwnerCodesStage,
      unwindOwnerCodesStage,
      groupStage
    )
    postMatchStage.foreach(pipeline += _)
    pipeline += sortDoc
    pipeline.toSeq
  }

  override def getMatches(search: MatchCardSearch): Future[Seq[MatchCardForense]] = {
    val isCollapsing = search.isCollapsing.contains(true)
    val summariesF = Future {
      val pipeline = (buildMatchesAggrPipeline(search) :+
        Document.parse(s"""{"$$skip": ${search.page * search.pageSize}}""") :+
        Document.parse(s"""{"$$limit": ${search.pageSize}}""")).asJava
      matches.aggregate(pipeline).into(new java.util.ArrayList[Document]()).asScala.toSeq.map { doc =>
        val globalCode = doc.getString("_id")
        val pending    = doc.getInteger(MatchGlobalStatus.pending.toString, 0)
        val hit        = doc.getInteger(MatchGlobalStatus.hit.toString, 0)
        val discarded  = doc.getInteger(MatchGlobalStatus.discarded.toString, 0)
        val conflict   = doc.getInteger(MatchGlobalStatus.conflict.toString, 0)
        val lastDate   = doc.getDate("lastDate")
        val category   = Option(doc.getString("category")).getOrElse("")
        (globalCode, pending, hit, discarded, conflict, lastDate, category)
      }
    }
    summariesF.flatMap { summaries =>
      Future.sequence(summaries.map { (globalCode, pending, hit, discarded, conflict, lastDate, category) =>
        val mc = MatchCard(SampleCode(globalCode), pending, hit, discarded, conflict,
          1, globalCode, AlphanumericId(category), lastDate, "", "")
        getProfileLr(SampleCode(globalCode), isCollapsing)
          .map(lr => MatchCardForense(mc, lr))
          .recover { case _ =>
            val lr = MatchCardMejorLr(globalCode, AlphanumericId(category), 0, MatchStatus.pending,
              MatchStatus.pending, 0.0, 0.0, 0.0, SampleCode(globalCode), 1)
            MatchCardForense(mc, lr)
          }
      })
    }
  }

  override def getTotalMatches(search: MatchCardSearch): Future[Int] = Future {
    val countPipeline = (buildMatchesAggrPipeline(search) :+
      Document.parse("""{"$count": "total"}""")).asJava
    Option(matches.aggregate(countPipeline).first())
      .fold(0)(_.getInteger("total", 0))
  }

  // ── getMatchesByGroup / getTotalMatchesByGroup ───────────────────────────────

  private def buildGroupFilter(search: MatchGroupSearch): Bson = {
    val gc = search.globalCode.text

    val statusFilter: Bson = search.status match {
      case Some("conflict") => Filters.or(
        Filters.and(Filters.eq("leftProfile.status", MatchStatus.discarded.toString), Filters.eq("rightProfile.status", MatchStatus.hit.toString)),
        Filters.and(Filters.eq("leftProfile.status", MatchStatus.hit.toString),       Filters.eq("rightProfile.status", MatchStatus.discarded.toString))
      )
      case Some("pending") => Filters.or(
        Filters.eq("leftProfile.status", MatchStatus.pending.toString),
        Filters.eq("rightProfile.status", MatchStatus.pending.toString)
      )
      case Some(s) => Filters.and(
        Filters.eq("leftProfile.status", s),
        Filters.eq("rightProfile.status", s)
      )
      case None => Filters.and(
        Filters.ne("leftProfile.status", MatchStatus.deleted.toString),
        Filters.ne("rightProfile.status", MatchStatus.deleted.toString)
      )
    }

    val typeFilter: Bson = search.tipo match {
      case Some(t) => Filters.and(statusFilter, Filters.eq("type", t))
      case None    => Filters.and(statusFilter, Filters.ne("type", 4))
    }

    val profileFilter: Bson = Filters.or(
      Filters.eq("leftProfile.globalCode",  gc),
      Filters.eq("rightProfile.globalCode", gc)
    )

    val combined = if (search.isCollapsing.contains(true) && search.courtCaseId.isDefined)
      Filters.and(
        Filters.eq("idCourtCase", search.courtCaseId.get),
        Filters.eq("leftProfile.globalCode", gc),
        Filters.ne("leftProfile.status",  MatchStatus.deleted.toString),
        Filters.ne("rightProfile.status", MatchStatus.deleted.toString)
      )
    else
      Filters.and(typeFilter, profileFilter,
        Filters.ne("leftProfile.status",  MatchStatus.deleted.toString),
        Filters.ne("rightProfile.status", MatchStatus.deleted.toString))

    if (search.isSuperUser) combined
    else Filters.and(combined, Filters.or(
      Filters.eq("leftProfile.assignee",  search.user),
      Filters.eq("rightProfile.assignee", search.user)
    ))
  }

  override def getTotalMatchesByGroup(search: MatchGroupSearch): Int =
    matches.countDocuments(buildGroupFilter(search)).toInt

  override def getMatchesByGroup(search: MatchGroupSearch): Seq[MatchingResult] = {
    val gc = search.globalCode

    val sortField = search.sortField match {
      case "globalCode"             => Sorts.orderBy(if (search.ascending) Sorts.ascending("rightProfile.globalCode") else Sorts.descending("rightProfile.globalCode"))
      case "totalAlleles"           => Sorts.orderBy(if (search.ascending) Sorts.ascending("result.totalAlleles") else Sorts.descending("result.totalAlleles"))
      case "sharedAllelePonderation"=> Sorts.orderBy(if (search.ascending) Sorts.ascending("result.leftPonderation") else Sorts.descending("result.leftPonderation"))
      case "ownerStatus" | "otherStatus" => Sorts.orderBy(if (search.ascending) Sorts.ascending("leftProfile.status") else Sorts.descending("leftProfile.status"))
      case _                        => Sorts.orderBy(if (search.ascending) Sorts.ascending("matchingDate") else Sorts.descending("matchingDate"))
    }

    val skip  = (search.page - 1).max(0) * search.pageSize
    val docs  = matches.find(buildGroupFilter(search))
      .sort(sortField)
      .skip(skip)
      .limit(search.pageSize)
      .into(new java.util.ArrayList[Document]()).asScala.toSeq

    docs.map { doc =>
      val mr      = docToMatchResult(doc)
      val isRight = mr.rightProfile.globalCode == gc

      val (ownerProf, matchingProf) = if (isRight) (mr.rightProfile, mr.leftProfile) else (mr.leftProfile, mr.rightProfile)

      val sharedAllelePonderation =
        if (isRight) mr.result.rightPonderation else mr.result.leftPonderation

      MatchingResult(
        mr._id.id,
        matchingProf.globalCode,
        matchingProf.globalCode.text,
        mr.result.stringency,
        mr.result.matchingAlleles,
        mr.result.totalAlleles,
        matchingProf.categoryId,
        ownerProf.status,
        matchingProf.status,
        getGlobalMatchStatus(ownerProf.status, matchingProf.status),
        sharedAllelePonderation,
        1,
        false,
        mr.result.algorithm,
        mr.`type`,
        mr.result.allelesRanges,
        mr.lr,
        mr.mismatches
      )
    }
  }

  // ── count helpers ────────────────────────────────────────────────────────────

  private def countByStatus(globalCode: String, status: String): Future[Int] = Future {
    matches.countDocuments(Filters.and(
      Filters.or(Filters.eq("leftProfile.globalCode", globalCode), Filters.eq("rightProfile.globalCode", globalCode)),
      Filters.or(Filters.eq("leftProfile.status", status), Filters.eq("rightProfile.status", status))
    )).toInt
  }

  override def numberOfMatchesHit(globalCode: String): Future[Int]      = countByStatus(globalCode, MatchStatus.hit.toString)
  override def numberOfMatchesPending(globalCode: String): Future[Int]  = countByStatus(globalCode, MatchStatus.pending.toString)
  override def numberOfMatchesDescarte(globalCode: String): Future[Int] = countByStatus(globalCode, MatchStatus.discarded.toString)
  override def numberOfMatchesConflic(globalCode: String): Future[Int]  = Future {
    matches.countDocuments(Filters.and(
      Filters.or(Filters.eq("leftProfile.globalCode", globalCode), Filters.eq("rightProfile.globalCode", globalCode)),
      Filters.or(
        Filters.and(Filters.eq("leftProfile.status", MatchStatus.hit.toString),       Filters.eq("rightProfile.status", MatchStatus.discarded.toString)),
        Filters.and(Filters.eq("leftProfile.status", MatchStatus.discarded.toString), Filters.eq("rightProfile.status", MatchStatus.hit.toString))
      )
    )).toInt
  }

  override def numberOfMt(globalCode: String): Future[Boolean] = Future {
    matches.countDocuments(Filters.and(
      Filters.or(Filters.eq("leftProfile.globalCode", globalCode), Filters.eq("rightProfile.globalCode", globalCode)),
      Filters.eq("type", 2)
    )) > 0
  }

  override def getProfileLr(globalCode: SampleCode, isCollapsing: Boolean): Future[MatchCardMejorLr] = Future {
    val filter = Filters.and(
      Filters.ne("leftProfile.status",  MatchStatus.deleted.toString),
      Filters.ne("rightProfile.status", MatchStatus.deleted.toString),
      Filters.or(Filters.eq("leftProfile.globalCode", globalCode.text), Filters.eq("rightProfile.globalCode", globalCode.text))
    )
    val doc = Option(matches.find(filter).sort(Sorts.descending("lr")).first())
      .getOrElse(throw new NoSuchElementException(s"No match found for ${globalCode.text}"))
    val mr      = docToMatchResult(doc)
    val isRight = mr.rightProfile.globalCode == globalCode
    val (ownerP, matchP) = if (isRight) (mr.rightProfile, mr.leftProfile) else (mr.leftProfile, mr.rightProfile)
    val shared  = if (isRight) mr.result.rightPonderation else mr.result.leftPonderation
    MatchCardMejorLr(matchP.globalCode.text, matchP.categoryId, mr.result.totalAlleles,
      ownerP.status, matchP.status, shared, mr.mismatches.toDouble, mr.lr, matchP.globalCode, mr.`type`)
  }

  // ── collapsing ───────────────────────────────────────────────────────────────

  override def discardCollapsingByLeftProfile(id: String, courtCaseId: Long): Future[Unit] = Future {
    matches.deleteMany(Filters.and(
      Filters.eq("leftProfile.globalCode", id),
      Filters.eq("idCourtCase", courtCaseId)
    ))
    ()
  }

  override def discardCollapsingByRightProfile(id: String, courtCaseId: Long): Future[Unit] = Future {
    matches.deleteMany(Filters.and(
      Filters.eq("rightProfile.globalCode", id),
      Filters.eq("idCourtCase", courtCaseId)
    ))
    ()
  }

  override def discardCollapsingByLeftAndRightProfile(id: String, courtCaseId: Long): Future[Unit] = Future {
    matches.deleteMany(Filters.and(
      Filters.or(Filters.eq("leftProfile.globalCode", id), Filters.eq("rightProfile.globalCode", id)),
      Filters.eq("idCourtCase", courtCaseId)
    ))
    ()
  }

  override def discardCollapsingMatches(ids: List[String], courtCaseId: Long): Future[Unit] = Future {
    ids.headOption.foreach { id =>
      matches.deleteMany(Filters.and(
        Filters.eq("_id", new ObjectId(id)),
        Filters.eq("idCourtCase", courtCaseId)
      ))
    }
    ()
  }
}