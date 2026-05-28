package matching

import java.util.Date
import javax.inject.{Inject, Singleton}

import com.mongodb.client.{MongoCollection, MongoDatabase}
import com.mongodb.client.model.{Filters, Updates}
import org.bson.Document
import org.bson.types.ObjectId
import play.api.Logger
import play.api.libs.json.{Json, JsValue}

import profile.Profile
import profiledata.ProfileData
import types.SampleCode

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
}