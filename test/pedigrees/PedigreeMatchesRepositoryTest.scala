package pedigrees

import java.util.Date

import matching.Stringency._
import matching.{Algorithm, MatchStatus, MatchingProfile, NewMatchingResult}
import org.apache.commons.lang.time.DateUtils
import org.bson.types.ObjectId
import pedigree.{PedigreeMatchCardSearch, _}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import specs.PdgSpec
import types._

import scala.concurrent.Await
import scala.concurrent.duration._
import play.modules.reactivemongo.json._
import reactivemongo.api.Cursor

class PedigreeMatchesRepositoryTest extends PdgSpec {
  val pedigreeMatches = Await.result(new reactivemongo.api.MongoDriver().connection("localhost:27017").get.database("pdgdb-unit-test").map(_.collection[JSONCollection]("pedigreeMatches")), Duration(10, SECONDS))
  val duration = Duration(10, SECONDS)

  val individuals: Seq[Individual] = Seq(Individual(NodeAlias("PI"), None, None, Sex.Unknown, Some(SampleCode("AR-C-SHDG-1")), true, None))
  val id = 555
  val genogram = PedigreeGenogram(id, "tst-admintist", individuals, PedigreeStatus.UnderConstruction, None, false,0.5,false,None,"MPI",None,7l)

  val pedigreeMatchDirect = PedigreeDirectLinkMatch(MongoId("58ac62e6ebb12c3bfcc1afaa"),
    MongoDate(new Date()), 1,
    MatchingProfile(SampleCode("AR-C-SHDG-1102"), "jerkovicm", MatchStatus.pending, None),
    PedigreeMatchingProfile(12, NodeAlias("PI1"), SampleCode("AR-C-SHDG-1101"), "tst-admintist", MatchStatus.hit,"MPI",7l),
    NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

  val pedigreeMatchEmpty = PedigreeMissingInfoMatch(MongoId("58ac62e6ebb12c3bfcc1afab"),
    MongoDate(new Date()), 1,
    MatchingProfile(SampleCode("AR-C-SHDG-1103"), "jerkovicm", MatchStatus.pending, None),
    PedigreeMatchingProfile(12, NodeAlias("PI2"), SampleCode("AR-C-SHDG-1102"), "tst-admintist", MatchStatus.pending,"MPI",7l))

  val pedigreeMatchDirect2 = PedigreeDirectLinkMatch(MongoId("58ac62e6ebb12c3bfcc1afac"),
    MongoDate(new Date()), 1,
    MatchingProfile(SampleCode("AR-C-SHDG-1104"), "jerkovicm", MatchStatus.discarded, None),
    PedigreeMatchingProfile(12, NodeAlias("PI3"), SampleCode("AR-C-SHDG-1103"), "tst-admintist", MatchStatus.hit,"MPI",7l),
    NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

  "A Pedigree Matches repository" must {
    "get matches by id" in {
      val mongoId = MongoId(new ObjectId().toString)
      val pedigreeMatch = PedigreeDirectLinkMatch(mongoId,
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "assignee", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1101"), "assignee", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      Await.result(pedigreeMatches.insert(pedigreeMatch), duration)

      val repository = new MongoPedigreeMatchesRepository

      val result = Await.result(repository.getMatchById(mongoId.id), duration)

      result.isDefined mustBe true
      result.get mustBe pedigreeMatch

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "retrieve matches filtered by userId when super user is false" in {
      val repository = new MongoPedigreeMatchesRepository

      val pedigreeMatch = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "assignee", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1101"), "assignee", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      val pedigreeMatch2 = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "assignee2", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1101"), "assignee2", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      Await.result(pedigreeMatches.insert(pedigreeMatch), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatch2), duration)

      val result = Await.result(repository.getMatches(PedigreeMatchCardSearch("assignee", false, "pedigree")), duration)

      result.size mustBe 1
      result(0).count mustBe 1
      result(0).lastMatchDate mustBe pedigreeMatch.matchingDate
      result(0)._id mustBe Left(12)

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "retrieve all matches when super user is true" in {
      val repository = new MongoPedigreeMatchesRepository

      val pedigreeMatch = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "assignee", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1101"), "assignee", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      val pedigreeMatch2 = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "assignee2", MatchStatus.discarded, None),
        PedigreeMatchingProfile(13, NodeAlias("PI"), SampleCode("AR-C-SHDG-1101"), "assignee2", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      Await.result(pedigreeMatches.insert(pedigreeMatch), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatch2), duration)

      val result = Await.result(repository.getMatches(PedigreeMatchCardSearch("lgoldin", true, "pedigree")), duration)

      result.size mustBe 2
      result(0).count mustBe 1
      result(0).lastMatchDate mustBe pedigreeMatch2.matchingDate
      result(0)._id mustBe Left(13)
      result(1).count mustBe 1
      result(1).lastMatchDate mustBe pedigreeMatch.matchingDate
      result(1)._id mustBe Left(12)

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "retrieve all matches group by pedigreeId when super user is true" in {
      val repository = new MongoPedigreeMatchesRepository

      val pedigreeMatch = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "assignee", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1101"), "assignee", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      val pedigreeMatch2 = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "assignee2", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1101"), "assignee2", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      Await.result(pedigreeMatches.insert(pedigreeMatch), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatch2), duration)

      val result = Await.result(repository.getMatches(PedigreeMatchCardSearch("lgoldin", true, "pedigree")), duration)

      result.size mustBe 1
      result(0).count mustBe 2
      result(0).lastMatchDate mustBe pedigreeMatch2.matchingDate
      result(0)._id mustBe Left(12)

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "retrieve all matches group when super user is true filtered by profile in Pedigree" in {
      val repository = new MongoPedigreeMatchesRepository

      val pedigreeMatch = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "assignee", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1101"), "assignee", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      val pedigreeMatch2 = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1162"), "assignee2", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1501"), "assignee2", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      Await.result(pedigreeMatches.insert(pedigreeMatch), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatch2), duration)

      val result = Await.result(repository.getMatches(PedigreeMatchCardSearch("lgoldin", true, "pedigree", 0, 30, Some("AR-C-SHDG-1101"))), duration)

      result.size mustBe 1
      result(0).count mustBe 1
      result(0).lastMatchDate mustBe pedigreeMatch.matchingDate
      result(0)._id mustBe Left(12)

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "retrieve all matches group when super user is true filtered by profile in Profile" in {
      val repository = new MongoPedigreeMatchesRepository

      val pedigreeMatch = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "assignee", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1101"), "assignee", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      val pedigreeMatch2 = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1162"), "assignee2", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1501"), "assignee2", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      Await.result(pedigreeMatches.insert(pedigreeMatch), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatch2), duration)

      val result = Await.result(repository.getMatches(PedigreeMatchCardSearch("lgoldin", true, "pedigree", 0, 30, Some("AR-C-SHDG-1102"))), duration)

      result.size mustBe 1
      result(0).count mustBe 1
      result(0).lastMatchDate mustBe pedigreeMatch.matchingDate
      result(0)._id mustBe Left(12)

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "retrieve empty seq when super user is true filtered by unexistent profile" in {
      val repository = new MongoPedigreeMatchesRepository

      val pedigreeMatch = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "assignee", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1101"), "assignee", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      val pedigreeMatch2 = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1162"), "assignee2", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1501"), "assignee2", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      Await.result(pedigreeMatches.insert(pedigreeMatch), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatch2), duration)

      val result = Await.result(repository.getMatches(PedigreeMatchCardSearch("lgoldin", true, "pedigree", 0, 30, Some("Unexistent"))), duration)

      result.size mustBe 0

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "retrieve empty seq when super user is true filtered by hour from greater than matchingDate" in {
      val repository = new MongoPedigreeMatchesRepository

      val pedigreeMatch = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "assignee", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1101"), "assignee", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      val pedigreeMatch2 = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1162"), "assignee2", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1501"), "assignee2", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      Await.result(pedigreeMatches.insert(pedigreeMatch), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatch2), duration)

      val result = Await.result(repository.getMatches(PedigreeMatchCardSearch("lgoldin", true, "pedigree", 0, 30, None, Some(new Date()))), duration)

      result.size mustBe 0

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "retrieve non empty seq when super user is true filtered by hour from less than matchingDate" in {
      val repository = new MongoPedigreeMatchesRepository

      val pedigreeMatch = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "assignee", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1101"), "assignee", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      val pedigreeMatch2 = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1162"), "assignee2", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1501"), "assignee2", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      Await.result(pedigreeMatches.insert(pedigreeMatch), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatch2), duration)

      val result = Await.result(repository.getMatches(PedigreeMatchCardSearch("lgoldin", true, "pedigree", 0, 30, None, Some(DateUtils.addDays(new Date(), -1)))), duration)

      result.size mustBe 1
      result(0).count mustBe 2
      result(0).lastMatchDate mustBe pedigreeMatch2.matchingDate
      result(0)._id mustBe Left(12)

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "retrieve non empty seq when super user is true filtered by hour until greater than matchingDate" in {
      val repository = new MongoPedigreeMatchesRepository

      val pedigreeMatch = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "assignee", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1101"), "assignee", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      val pedigreeMatch2 = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1162"), "assignee2", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1501"), "assignee2", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      Await.result(pedigreeMatches.insert(pedigreeMatch), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatch2), duration)

      val result = Await.result(repository.getMatches(PedigreeMatchCardSearch("lgoldin", true, "pedigree", 0, 30, None, None, Some(DateUtils.addDays(new Date(), 1)))), duration)

      result.size mustBe 1
      result(0).count mustBe 2
      result(0).lastMatchDate mustBe pedigreeMatch2.matchingDate
      result(0)._id mustBe Left(12)

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "retrieve empty seq when super user is true filtered by hour until less than matchingDate" in {
      val repository = new MongoPedigreeMatchesRepository

      val pedigreeMatch = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "assignee", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1101"), "assignee", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      val pedigreeMatch2 = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1162"), "assignee2", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1501"), "assignee2", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      Await.result(pedigreeMatches.insert(pedigreeMatch), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatch2), duration)

      val result = Await.result(repository.getMatches(PedigreeMatchCardSearch("lgoldin", true, "pedigree", 0, 30, None, None, Some(DateUtils.addDays(new Date(), -1)))), duration)

      result.size mustBe 0

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "retrieve all matches group by pedigreeId when super user is true filtering deleted matches" in {
      val repository = new MongoPedigreeMatchesRepository

      val pedigreeMatch = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "assignee", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1101"), "assignee", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      val pedigreeMatch2 = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "assignee2", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1101"), "assignee2", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      val pedigreeMatch3 = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "assignee3", MatchStatus.discarded, None),
        PedigreeMatchingProfile(13, NodeAlias("PI"), SampleCode("AR-C-SHDG-1101"), "assignee4", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      val pedigreeMatch4 = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "assignee5", MatchStatus.deleted, None),
        PedigreeMatchingProfile(14, NodeAlias("PI"), SampleCode("AR-C-SHDG-1101"), "assignee6", MatchStatus.deleted,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      Await.result(pedigreeMatches.insert(pedigreeMatch), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatch2), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatch3), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatch4), duration)

      val result = Await.result(repository.getMatches(PedigreeMatchCardSearch("lgoldin", true, "pedigree")), duration)

      result.size mustBe 2
      result(0).count mustBe 1
      result(0).lastMatchDate mustBe pedigreeMatch3.matchingDate
      result(0)._id mustBe Left(13)
      result(1).count mustBe 2
      result(1).lastMatchDate mustBe pedigreeMatch2.matchingDate
      result(1)._id mustBe Left(12)

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "tell when a pedigree with no matches is editable" in {
      val repository = new MongoPedigreeMatchesRepository

      val result = Await.result(repository.allMatchesDiscarded(id), duration)

      result mustBe true
    }

    "tell when a pedigree with discarded matches is editable" in {
      val repository = new MongoPedigreeMatchesRepository

      val pedigreeMatch = PedigreeDirectLinkMatch(MongoId("58ac62e6ebb12c3bfcc1afac"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "jerkovicm", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1101"), "jerkovicm", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      Await.result(pedigreeMatches.insert(pedigreeMatch), duration)
      val result = Await.result(repository.allMatchesDiscarded(id), duration)

      result mustBe true

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "tell when a pedigree with pending matches is not editable" in {
      val repository = new MongoPedigreeMatchesRepository

      Await.result(pedigreeMatches.insert(pedigreeMatchDirect), duration)
      val result = Await.result(repository.allMatchesDiscarded(id), duration)

      result mustBe true

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "tell when a pedigree with deleted matches is editable" in {
      val repository = new MongoPedigreeMatchesRepository

      val deletedMatch = PedigreeDirectLinkMatch(MongoId("58ac62e6ebb12c3bfcc1afaa"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "jerkovicm", MatchStatus.deleted, None),
        PedigreeMatchingProfile(12, NodeAlias("PI1"), SampleCode("AR-C-SHDG-1101"), "tst-admintist", MatchStatus.deleted,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      Await.result(pedigreeMatches.insert(deletedMatch), duration)
      val result = Await.result(repository.allMatchesDiscarded(id), duration)

      result mustBe true

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "count matches by group" in {
      val repository = new MongoPedigreeMatchesRepository

      val pedigreeSearch = PedigreeMatchGroupSearch("pdg", true, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "date", false)

      Await.result(pedigreeMatches.insert(pedigreeMatchDirect), duration)

      val result = Await.result(repository.countMatchesByGroup(pedigreeSearch), duration)

      result mustBe 1

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "get direct matches by group" in {
      val repository = new MongoPedigreeMatchesRepository

      val pedigreeSearch = PedigreeMatchGroupSearch("pdg", true, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "date", false)

      Await.result(pedigreeMatches.insert(pedigreeMatchDirect), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatchEmpty), duration)

      val result = Await.result(repository.getMatchesByGroup(pedigreeSearch), duration)

      result.size mustBe 1
      result.head mustBe pedigreeMatchDirect

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "get empty matches by group" in {
      val repository = new MongoPedigreeMatchesRepository

      val pedigreeSearch = PedigreeMatchGroupSearch("pdg", true, "12", "pedigree", PedigreeMatchKind.MissingInfo, 0, 30, "date", false)

      Await.result(pedigreeMatches.insert(pedigreeMatchDirect), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatchEmpty), duration)

      val result = Await.result(repository.getMatchesByGroup(pedigreeSearch), duration)

      result.size mustBe 1
      result.head mustBe pedigreeMatchEmpty

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "get matches by group and user" in {
      val repository = new MongoPedigreeMatchesRepository

      Await.result(pedigreeMatches.insert(pedigreeMatchDirect), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatchEmpty), duration)

      val results = Await.result(repository.getMatchesByGroup(PedigreeMatchGroupSearch("jerkovicm", false, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "date", false)), duration)
      val noResults = Await.result(repository.getMatchesByGroup(PedigreeMatchGroupSearch("pdg", false, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "date", false)), duration)

      results.size mustBe 1
      results.head mustBe pedigreeMatchDirect
      noResults.isEmpty mustBe true

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "get matches by group and superuser" in {
      val repository = new MongoPedigreeMatchesRepository

      Await.result(pedigreeMatches.insert(pedigreeMatchDirect), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatchEmpty), duration)

      val results = Await.result(repository.getMatchesByGroup(PedigreeMatchGroupSearch("pdg", true, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "date", false)), duration)
      val noResults = Await.result(repository.getMatchesByGroup(PedigreeMatchGroupSearch("pdg", false, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "date", false)), duration)

      results.size mustBe 1
      results.head mustBe pedigreeMatchDirect
      noResults.isEmpty mustBe true

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "get matches by group and pedigree id" in {
      val repository = new MongoPedigreeMatchesRepository

      Await.result(pedigreeMatches.insert(pedigreeMatchDirect), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatchEmpty), duration)

      val results = Await.result(repository.getMatchesByGroup(PedigreeMatchGroupSearch("pdg", true, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "date", false)), duration)
      val noResults = Await.result(repository.getMatchesByGroup(PedigreeMatchGroupSearch("pdg", true, "5", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "date", false)), duration)

      results.size mustBe 1
      results.head mustBe pedigreeMatchDirect
      noResults.isEmpty mustBe true

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "get matches by group paginated" in {
      val repository = new MongoPedigreeMatchesRepository

      Await.result(pedigreeMatches.insert(pedigreeMatchDirect), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatchDirect2), duration)

      val results1 = Await.result(repository.getMatchesByGroup(PedigreeMatchGroupSearch("pdg", true, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 1, "date", false)), duration)
      val results2 = Await.result(repository.getMatchesByGroup(PedigreeMatchGroupSearch("pdg", true, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "date", false)), duration)

      results1.size mustBe 1
      results2.size mustBe 2

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "get matches by group sorted by date" in {
      val repository = new MongoPedigreeMatchesRepository

      Await.result(pedigreeMatches.insert(pedigreeMatchDirect), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatchDirect2), duration)

      val resultAscending = Await.result(repository.getMatchesByGroup(PedigreeMatchGroupSearch("pdg", true, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "date", true)), duration)
      val resultDescending = Await.result(repository.getMatchesByGroup(PedigreeMatchGroupSearch("pdg", true, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "date", false)), duration)

      resultAscending(0).matchingDate.date mustBe <=(resultAscending(1).matchingDate.date)
      resultDescending(0).matchingDate.date mustBe >=(resultDescending(1).matchingDate.date)

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "get matches by group sorted by profile" in {
      val repository = new MongoPedigreeMatchesRepository

      Await.result(pedigreeMatches.insert(pedigreeMatchDirect), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatchDirect2), duration)

      val resultAscending = Await.result(repository.getMatchesByGroup(PedigreeMatchGroupSearch("pdg", true, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "profile", true)), duration)
      val resultDescending = Await.result(repository.getMatchesByGroup(PedigreeMatchGroupSearch("pdg", true, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "profile", false)), duration)

      resultAscending(0).profile.globalCode.text mustBe <=(resultAscending(1).profile.globalCode.text)
      resultDescending(0).profile.globalCode.text mustBe >=(resultDescending(1).profile.globalCode.text)

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "get matches by group sorted by unknown" in {
      val repository = new MongoPedigreeMatchesRepository

      Await.result(pedigreeMatches.insert(pedigreeMatchDirect), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatchDirect2), duration)

      val resultAscending = Await.result(repository.getMatchesByGroup(PedigreeMatchGroupSearch("pdg", true, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "unknown", true)), duration)
      val resultDescending = Await.result(repository.getMatchesByGroup(PedigreeMatchGroupSearch("pdg", true, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "unknown", false)), duration)

      resultAscending(0).pedigree.unknown.text mustBe <=(resultAscending(1).pedigree.unknown.text)
      resultDescending(0).pedigree.unknown.text mustBe >=(resultDescending(1).pedigree.unknown.text)

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "get matches by group sorted by pedigree profile status" in {
      val repository = new MongoPedigreeMatchesRepository

      Await.result(pedigreeMatches.insert(pedigreeMatchDirect), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatchDirect2), duration)

      val resultAscending = Await.result(repository.getMatchesByGroup(PedigreeMatchGroupSearch("pdg", true, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "pedigreeStatus", true)), duration)
      val resultDescending = Await.result(repository.getMatchesByGroup(PedigreeMatchGroupSearch("pdg", true, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "pedigreeStatus", false)), duration)

      resultAscending(0).pedigree.status mustBe <=(resultAscending(1).pedigree.status)
      resultDescending(0).pedigree.status mustBe >=(resultDescending(1).pedigree.status)

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "get matches by group sorted by profile status" in {
      val repository = new MongoPedigreeMatchesRepository

      Await.result(pedigreeMatches.insert(pedigreeMatchDirect), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatchDirect2), duration)

      val resultAscending = Await.result(repository.getMatchesByGroup(PedigreeMatchGroupSearch("pdg", true, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "profileStatus", true)), duration)
      val resultDescending = Await.result(repository.getMatchesByGroup(PedigreeMatchGroupSearch("pdg", true, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "profileStatus", false)), duration)

      resultAscending(0).profile.status mustBe <=(resultAscending(1).profile.status)
      resultDescending(0).profile.status mustBe >=(resultDescending(1).profile.status)

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "discard match profile side" in {
      val repository = new MongoPedigreeMatchesRepository

      Await.result(pedigreeMatches.insert(pedigreeMatchDirect), duration)

      val result = Await.result(repository.discardProfile(pedigreeMatchDirect._id.id), duration)
      val list = Await.result(pedigreeMatches.find(Json.obj()).cursor[PedigreeMatchResult]().collect[List](Int.MaxValue, Cursor.FailOnError[List[PedigreeMatchResult]]()), duration)

      result mustBe Right(pedigreeMatchDirect._id.id)
      list.head.profile.status mustBe MatchStatus.discarded

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "discard match pedigree side" in {
      val repository = new MongoPedigreeMatchesRepository

      Await.result(pedigreeMatches.insert(pedigreeMatchDirect), duration)

      val result = Await.result(repository.discardPedigree(pedigreeMatchDirect._id.id), duration)
      val list = Await.result(pedigreeMatches.find(Json.obj()).cursor[PedigreeMatchResult]().collect[List](Int.MaxValue, Cursor.FailOnError[List[PedigreeMatchResult]]()), duration)

      result mustBe Right(pedigreeMatchDirect._id.id)
      list.head.pedigree.status mustBe MatchStatus.discarded

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "count matches" in {
      val repository = new MongoPedigreeMatchesRepository

      val anotherPedigree = PedigreeDirectLinkMatch(MongoId("58ac62e6ebb12c3bfcc1afac"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1104"), "jerkovicm", MatchStatus.discarded, None),
        PedigreeMatchingProfile(3, NodeAlias("PI3"), SampleCode("AR-C-SHDG-1103"), "tst-admintist", MatchStatus.hit,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))
      Await.result(pedigreeMatches.insert(pedigreeMatchDirect), duration)
      Await.result(pedigreeMatches.insert(anotherPedigree), duration)

      val search = PedigreeMatchCardSearch("tst-admintist", false, "pedigree", 0, 30)

      val result = Await.result(repository.countMatches(search), duration)
      result mustBe 2

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "paginate matches in cards" in {
      val repository = new MongoPedigreeMatchesRepository

      val pedigreeMatch = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "assignee", MatchStatus.discarded, None),
        PedigreeMatchingProfile(12, NodeAlias("PI"), SampleCode("AR-C-SHDG-1101"), "assignee", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      val pedigreeMatch2 = PedigreeDirectLinkMatch(MongoId(new ObjectId().toString),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-1102"), "assignee2", MatchStatus.discarded, None),
        PedigreeMatchingProfile(13, NodeAlias("PI"), SampleCode("AR-C-SHDG-1101"), "assignee2", MatchStatus.discarded,"MPI",7l),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

      Await.result(pedigreeMatches.insert(pedigreeMatch), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatch2), duration)

      val results = Await.result(repository.getMatches(PedigreeMatchCardSearch("tst-admintist", true, "pedigree", 0, 30)), duration)
      val lessResults = Await.result(repository.getMatches(PedigreeMatchCardSearch("tst-admintist", true, "pedigree", 0, 1)), duration)

      results.size mustBe 2
      lessResults.size mustBe 1

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "delete all matches by pedigree" in {
      Await.result(pedigreeMatches.insert(pedigreeMatchDirect), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatchDirect2), duration)

      val repository = new MongoPedigreeMatchesRepository

      val result = Await.result(repository.deleteMatches(12), duration)

      result mustBe Right(12)

      val list = Await.result(pedigreeMatches.find(Json.obj()).cursor[PedigreeMatchResult]().collect[List](Int.MaxValue, Cursor.FailOnError[List[PedigreeMatchResult]]()), duration)
      list(0).pedigree.status mustBe MatchStatus.deleted
      list(0).profile.status mustBe MatchStatus.deleted
      list(1).pedigree.status mustBe MatchStatus.deleted
      list(1).profile.status mustBe MatchStatus.deleted

      Await.result(pedigreeMatches.drop(false), duration)
    }

    "get matches by group and profile global code" in {
      val repository = new MongoPedigreeMatchesRepository

      Await.result(pedigreeMatches.insert(pedigreeMatchDirect), duration)
      Await.result(pedigreeMatches.insert(pedigreeMatchEmpty), duration)

      val results = Await.result(repository.getMatchesByGroup(PedigreeMatchGroupSearch("pdg", true, "AR-C-SHDG-1102", "profile", PedigreeMatchKind.DirectLink, 0, 30, "date", false)), duration)
      val noResults = Await.result(repository.getMatchesByGroup(PedigreeMatchGroupSearch("pdg", true, "AR-C-SHDG-1100", "profile", PedigreeMatchKind.DirectLink, 0, 30, "date", false)), duration)

      results.size mustBe 1
      results.head mustBe pedigreeMatchDirect
      noResults.isEmpty mustBe true

      Await.result(pedigreeMatches.drop(false), duration)
    }

  }
}
