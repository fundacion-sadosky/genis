package integration.pedigree

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.{Filters, Updates}
import fixtures.MongoSpec
import matching.{Algorithm, MatchStatus, MatchingProfile, MongoId, NewMatchingResult, Stringency}
import org.bson.Document
import org.bson.types.ObjectId
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import pedigree.*
import play.api.libs.json.Json
import types.{AlphanumericId, MongoDate, SampleCode}

import java.util.Date
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future}

/**
 * Integration tests for MongoPedigreeMatchesRepository.
 *
 * Requires:
 *   utils/docker/docker-compose up -d   (mongo on localhost:27017)
 *
 * Uses a dedicated `genis_test` database to avoid polluting `pdgdb` (dev DB).
 * Each test cleans the `pedigreeMatches` collection in beforeEach/afterEach.
 */
class PedigreeMatchesRepositoryIntegrationTest
    extends AnyWordSpec with Matchers with MongoSpec with BeforeAndAfterEach:

  private val timeout = 15.seconds
  private given ec: ExecutionContext = ExecutionContext.Implicits.global

  private lazy val repo = new MongoPedigreeMatchesRepository(mongoDb, ec)
  private def col: MongoCollection[Document] = mongoDb.getCollection("pedigreeMatches")

  private def await[T](f: Future[T]): T = Await.result(f, timeout)

  // ─── test data ────────────────────────────────────────────────────────────

  private val pedigreeId  = 555L
  private val pedigreeId2 = 666L
  private val assignee    = "tst-assignee"
  private val otherUser   = "other-user"
  private val courtCase   = 7L

  private def directLink(
    id: String,
    profileCode: String,
    pedCode: String,
    profileStatus: MatchStatus.Value = MatchStatus.pending,
    pedigreeStatus: MatchStatus.Value = MatchStatus.pending,
    user: String = assignee,
    profileUser: String = assignee,
    pedId: Long = pedigreeId,
    `type`: Int = 1,
    compatibilityValue: Double = 0.7
  ): PedigreeDirectLinkMatch =
    PedigreeDirectLinkMatch(
      _id = MongoId(id),
      matchingDate = MongoDate(new Date()),
      `type` = `type`,
      profile = MatchingProfile(SampleCode(profileCode), profileUser, profileStatus, None, AlphanumericId("IR")),
      pedigree = PedigreeProfileInfo(pedId, NodeAlias("PI"), SampleCode(pedCode), user, pedigreeStatus, "MPI", courtCase),
      result = NewMatchingResult(Stringency.ModerateStringency, Map.empty, 14,
        AlphanumericId("SOSPECHOSO"), 1.0, 1.0, Algorithm.ENFSI)
    )

  private def insert(matches: PedigreeMatchResult*): Unit =
    matches.foreach { m =>
      val doc = Document.parse(Json.toJson(m).toString())
      // The Json writer encodes `_id` as a string; the Mongo doc needs an ObjectId
      // so the find-by-id queries match.
      doc.put("_id", new ObjectId(m._id.id))
      col.insertOne(doc)
    }

  override protected def beforeEach(): Unit =
    super.beforeEach()
    col.deleteMany(new Document())

  override protected def afterEach(): Unit =
    col.deleteMany(new Document())
    super.afterEach()

  // ─── getMatchById ─────────────────────────────────────────────────────────

  "MongoPedigreeMatchesRepository.getMatchById" must {

    "return Some(match) when the id exists" in {
      val id    = new ObjectId().toString
      val match0 = directLink(id, "AR-C-SHDG-1102", "AR-C-SHDG-1101")
      insert(match0)

      val result = await(repo.getMatchById(id))
      result mustBe defined
      result.get._id.id mustBe id
    }

    "return None when the id is valid hex but not present" in {
      await(repo.getMatchById(new ObjectId().toString)) mustBe None
    }

    // Q-C2 verification
    "return None when the id is not a valid 24-char hex ObjectId" in {
      await(repo.getMatchById("not-a-valid-objectid")) mustBe None
      await(repo.getMatchById("")) mustBe None
      await(repo.getMatchById("123")) mustBe None
    }
  }

  // ─── discardProfile / discardPedigree ─────────────────────────────────────

  "MongoPedigreeMatchesRepository.discardProfile" must {

    "set profile.status to discarded and return Right(id) for a valid id" in {
      val id    = new ObjectId().toString
      insert(directLink(id, "AR-C-SHDG-1", "AR-C-SHDG-2"))

      val result = await(repo.discardProfile(id))
      result mustBe Right(id)

      val doc = col.find(Filters.eq("_id", new ObjectId(id))).first()
      doc.get("profile", classOf[Document]).getString("status") mustBe MatchStatus.discarded.toString
    }

    // Q-C2 verification
    "return Left(error.E0631) for an invalid (non-hex) id" in {
      await(repo.discardProfile("not-a-valid-objectid")) mustBe Left("error.E0631")
    }

    "succeed even when the id is hex but no document matches (Mongo updateOne is a no-op)" in {
      // updateOne with a non-matching filter is not an error in MongoDB.
      // The repo only catches *exceptions*, so this returns Right(id).
      await(repo.discardProfile(new ObjectId().toString)) mustBe a[Right[?, ?]]
    }
  }

  "MongoPedigreeMatchesRepository.discardPedigree" must {

    "set pedigree.status to discarded and return Right(id) for a valid id" in {
      val id = new ObjectId().toString
      insert(directLink(id, "AR-C-SHDG-3", "AR-C-SHDG-4"))

      val result = await(repo.discardPedigree(id))
      result mustBe Right(id)

      val doc = col.find(Filters.eq("_id", new ObjectId(id))).first()
      doc.get("pedigree", classOf[Document]).getString("status") mustBe MatchStatus.discarded.toString
    }

    // Q-C2 verification
    "return Left(error.E0631) for an invalid (non-hex) id" in {
      await(repo.discardPedigree("garbage")) mustBe Left("error.E0631")
    }
  }

  // ─── confirmProfile / confirmPedigree ─────────────────────────────────────

  "MongoPedigreeMatchesRepository.confirmProfile" must {

    "set profile.status to hit and return Right(id) for a valid id" in {
      val id = new ObjectId().toString
      insert(directLink(id, "AR-C-SHDG-5", "AR-C-SHDG-6"))

      val result = await(repo.confirmProfile(id))
      result mustBe Right(id)

      val doc = col.find(Filters.eq("_id", new ObjectId(id))).first()
      doc.get("profile", classOf[Document]).getString("status") mustBe MatchStatus.hit.toString
    }

    // Q-C2 verification
    "return Left(error.E0631) for an invalid id" in {
      await(repo.confirmProfile("not-an-id")) mustBe Left("error.E0631")
    }
  }

  "MongoPedigreeMatchesRepository.confirmPedigree" must {

    "set pedigree.status to hit and return Right(id) for a valid id" in {
      val id = new ObjectId().toString
      insert(directLink(id, "AR-C-SHDG-7", "AR-C-SHDG-8"))

      val result = await(repo.confirmPedigree(id))
      result mustBe Right(id)

      val doc = col.find(Filters.eq("_id", new ObjectId(id))).first()
      doc.get("pedigree", classOf[Document]).getString("status") mustBe MatchStatus.hit.toString
    }

    // Q-C2 verification
    "return Left(error.E0631) for an invalid id" in {
      await(repo.confirmPedigree("xxx")) mustBe Left("error.E0631")
    }
  }

  // ─── deleteMatches ────────────────────────────────────────────────────────

  "MongoPedigreeMatchesRepository.deleteMatches" must {

    "set both profile and pedigree status to deleted for all matches of a pedigree" in {
      val id1 = new ObjectId().toString
      val id2 = new ObjectId().toString
      val idOther = new ObjectId().toString
      insert(
        directLink(id1, "AR-C-SHDG-9",  "AR-C-SHDG-10", pedId = pedigreeId),
        directLink(id2, "AR-C-SHDG-11", "AR-C-SHDG-12", pedId = pedigreeId),
        directLink(idOther, "AR-C-SHDG-13", "AR-C-SHDG-14", pedId = pedigreeId2)
      )

      val result = await(repo.deleteMatches(pedigreeId))
      result mustBe Right(pedigreeId)

      val touched = col.find(Filters.eq("pedigree.idPedigree", pedigreeId))
        .into(new java.util.ArrayList[Document]())
      touched.size() mustBe 2
      val s = scala.jdk.CollectionConverters.ListHasAsScala(touched).asScala.toList
      s.foreach { d =>
        d.get("profile",  classOf[Document]).getString("status") mustBe MatchStatus.deleted.toString
        d.get("pedigree", classOf[Document]).getString("status") mustBe MatchStatus.deleted.toString
      }

      val untouched = col.find(Filters.eq("_id", new ObjectId(idOther))).first()
      untouched.get("profile",  classOf[Document]).getString("status") mustBe MatchStatus.pending.toString
      untouched.get("pedigree", classOf[Document]).getString("status") mustBe MatchStatus.pending.toString
    }
  }

  // ─── allMatchesDiscarded / hasMatches / hasPendingMatches ─────────────────

  "MongoPedigreeMatchesRepository.allMatchesDiscarded" must {

    "return true when no active matches remain for the pedigree" in {
      insert(directLink(new ObjectId().toString, "AR-C-SHDG-1", "AR-C-SHDG-2",
        profileStatus = MatchStatus.discarded, pedigreeStatus = MatchStatus.discarded))
      await(repo.allMatchesDiscarded(pedigreeId)) mustBe true
    }

    "return false when at least one pending match exists" in {
      insert(
        directLink(new ObjectId().toString, "AR-C-SHDG-3", "AR-C-SHDG-4",
          profileStatus = MatchStatus.discarded, pedigreeStatus = MatchStatus.discarded),
        directLink(new ObjectId().toString, "AR-C-SHDG-5", "AR-C-SHDG-6",
          profileStatus = MatchStatus.pending,   pedigreeStatus = MatchStatus.pending)
      )
      await(repo.allMatchesDiscarded(pedigreeId)) mustBe false
    }

    "return true for a pedigree with no matches at all" in {
      await(repo.allMatchesDiscarded(9999L)) mustBe true
    }
  }

  "MongoPedigreeMatchesRepository.hasMatches / hasPendingMatches" must {

    "hasMatches returns true when any match exists, regardless of status" in {
      insert(directLink(new ObjectId().toString, "AR-C-SHDG-7", "AR-C-SHDG-8",
        profileStatus = MatchStatus.discarded, pedigreeStatus = MatchStatus.discarded))
      await(repo.hasMatches(pedigreeId)) mustBe true
      await(repo.hasMatches(9999L)) mustBe false
    }

    "hasPendingMatches returns true only when both sides are pending" in {
      insert(directLink(new ObjectId().toString, "AR-C-SHDG-9", "AR-C-SHDG-10"))
      await(repo.hasPendingMatches(pedigreeId)) mustBe true
    }

    "hasPendingMatches returns false when at least one side is not pending" in {
      insert(directLink(new ObjectId().toString, "AR-C-SHDG-11", "AR-C-SHDG-12",
        profileStatus = MatchStatus.discarded, pedigreeStatus = MatchStatus.pending))
      await(repo.hasPendingMatches(pedigreeId)) mustBe false
    }
  }

  // ─── number-of counters ───────────────────────────────────────────────────

  "MongoPedigreeMatchesRepository counters" must {

    "numberOfPendingMatches counts only matches where both sides are pending" in {
      insert(
        directLink(new ObjectId().toString, "AR-C-SHDG-13", "AR-C-SHDG-14"),
        directLink(new ObjectId().toString, "AR-C-SHDG-15", "AR-C-SHDG-16",
          profileStatus = MatchStatus.discarded, pedigreeStatus = MatchStatus.pending),
        directLink(new ObjectId().toString, "AR-C-SHDG-17", "AR-C-SHDG-18")
      )
      await(repo.numberOfPendingMatches(pedigreeId)) mustBe 2
    }

    "numberOfMatches counts all matches regardless of status" in {
      insert(
        directLink(new ObjectId().toString, "AR-C-SHDG-19", "AR-C-SHDG-20"),
        directLink(new ObjectId().toString, "AR-C-SHDG-21", "AR-C-SHDG-22",
          profileStatus = MatchStatus.discarded, pedigreeStatus = MatchStatus.discarded),
        directLink(new ObjectId().toString, "AR-C-SHDG-23", "AR-C-SHDG-24",
          profileStatus = MatchStatus.hit, pedigreeStatus = MatchStatus.hit)
      )
      await(repo.numberOfMatches(pedigreeId)) mustBe 3
    }

    "numberOfDiscardedMatches counts only matches where both sides are discarded" in {
      insert(
        directLink(new ObjectId().toString, "AR-C-SHDG-25", "AR-C-SHDG-26",
          profileStatus = MatchStatus.discarded, pedigreeStatus = MatchStatus.discarded),
        directLink(new ObjectId().toString, "AR-C-SHDG-27", "AR-C-SHDG-28",
          profileStatus = MatchStatus.discarded, pedigreeStatus = MatchStatus.pending)
      )
      await(repo.numberOfDiscardedMatches(pedigreeId)) mustBe 1
    }

    "numberOfHitMatches counts only matches where both sides are hit" in {
      insert(
        directLink(new ObjectId().toString, "AR-C-SHDG-29", "AR-C-SHDG-30",
          profileStatus = MatchStatus.hit, pedigreeStatus = MatchStatus.hit),
        directLink(new ObjectId().toString, "AR-C-SHDG-31", "AR-C-SHDG-32",
          profileStatus = MatchStatus.hit, pedigreeStatus = MatchStatus.pending)
      )
      await(repo.numberOfHitMatches(pedigreeId)) mustBe 1
    }
  }

  // ─── getAllMatchNonDiscardedByGroup ───────────────────────────────────────

  "MongoPedigreeMatchesRepository.getAllMatchNonDiscardedByGroup" must {

    "by pedigree: return only matches not discarded/deleted/hit" in {
      val activeId   = new ObjectId().toString
      val discardedId = new ObjectId().toString
      val deletedId   = new ObjectId().toString
      val hitId       = new ObjectId().toString
      insert(
        directLink(activeId,    "AR-C-SHDG-33", "AR-C-SHDG-34"),
        directLink(discardedId, "AR-C-SHDG-35", "AR-C-SHDG-36",
          profileStatus = MatchStatus.discarded, pedigreeStatus = MatchStatus.discarded),
        directLink(deletedId,   "AR-C-SHDG-37", "AR-C-SHDG-38",
          profileStatus = MatchStatus.deleted, pedigreeStatus = MatchStatus.deleted),
        directLink(hitId,       "AR-C-SHDG-39", "AR-C-SHDG-40",
          profileStatus = MatchStatus.hit, pedigreeStatus = MatchStatus.hit)
      )

      val out = await(repo.getAllMatchNonDiscardedByGroup(pedigreeId.toString, "pedigree"))
      out.map(_._id.id) mustBe Seq(activeId)
    }

    "by profile: filter by profile.globalCode and skip discarded/deleted/hit" in {
      val targetProfile = "AR-C-TGTP-1"
      val activeId      = new ObjectId().toString
      val otherProfileId = new ObjectId().toString
      val hitId         = new ObjectId().toString
      insert(
        directLink(activeId,        targetProfile, "AR-C-XXP-1"),
        directLink(otherProfileId,  "AR-C-OTHP-1",  "AR-C-XXP-2"),
        directLink(hitId,           targetProfile, "AR-C-XXP-3",
          profileStatus = MatchStatus.hit, pedigreeStatus = MatchStatus.hit)
      )

      val out = await(repo.getAllMatchNonDiscardedByGroup(targetProfile, "profile"))
      out.map(_._id.id) mustBe Seq(activeId)
    }
  }

  // ─── getMatchByPedigree / getMatchByProfile ──────────────────────────────

  "MongoPedigreeMatchesRepository.getMatchByPedigree" must {

    "return Some(PedigreeMatch) wrapping the pedigree id for an existing pedigree" in {
      insert(directLink(new ObjectId().toString, "AR-C-SHDG-41", "AR-C-SHDG-42"))
      val out = await(repo.getMatchByPedigree(pedigreeId))
      out mustBe defined
      out.get._id mustBe Left(pedigreeId)
    }

    "return None when the pedigree has no matches" in {
      await(repo.getMatchByPedigree(9999L)) mustBe None
    }
  }

  "MongoPedigreeMatchesRepository.getMatchByProfile" must {

    "return Some(PedigreeMatch) wrapping the profile code for an existing profile" in {
      val code = "AR-C-PRFP-1"
      insert(directLink(new ObjectId().toString, code, "AR-C-XYZ-1"))
      val out = await(repo.getMatchByProfile(code))
      out mustBe defined
      out.get._id mustBe Right(code)
    }

    "return None when the profile has no matches" in {
      await(repo.getMatchByProfile("AR-C-NONEP-1")) mustBe None
    }
  }
