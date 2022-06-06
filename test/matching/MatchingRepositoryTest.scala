package matching

import java.text.SimpleDateFormat
import java.util.Date

import play.modules.reactivemongo.json.collection.JSONCollection
import specs.PdgSpec
import types.{AlphanumericId, MongoDate, MongoId, SampleCode}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import profile.{Allele, Profile}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

class MatchingRepositoryTest extends PdgSpec {

  val duration = Duration(10, SECONDS)

  val matches = Await.result(new reactivemongo.api.MongoDriver().connection("localhost:27017").get.database("pdgdb-unit-test").map(_.collection[JSONCollection]("matches")), Duration(10, SECONDS))
  val profiles = Await.result(new reactivemongo.api.MongoDriver().connection("localhost:27017").get.database("pdgdb-unit-test").map(_.collection[JSONCollection]("profiles")), Duration(10, SECONDS))

  "A MatchingRepository repository" must {
    "convert in hit a match" in {

      val matchingRespository = new MongoMatchingRepository(null)

      val result = Await.result(matchingRespository.convertStatus("54eb50c42cdc8a94c6ee7949",SampleCode("AR-B-IACA-506"),"hit"), duration)

      result.size mustBe >(0)
    }
  }

  "A MatchingRepository repository" must {
    "get all the result of a user" in {
      val matchingRespository = new MongoMatchingRepository(null)

      Await.result(matchingRespository.matchesByAssignee("tst-admintist"), duration)
      ()
    }
  }

     "A MatchingRepository repository" must {
    "get a MatchResult by Id" in {
      val matchingRespository = new MongoMatchingRepository(null)

        val ssss = Await.result(matchingRespository.getByMatchingProfileId("54eb50c42cdc8a94c6ee7949"),duration)

        ssss.isDefined mustBe true
    }
  }

  "A MatchingRepository repository" must {
    "get a MatchResult by firing and matching profile (left and right)" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val result = Await.result(matchingRepository.getByFiringAndMatchingProfile(SampleCode("AR-C-HIBA-500"), SampleCode("AR-B-IMBICE-500")), duration)

      result.isDefined mustBe true
    }

    "get a MatchResult by firing and matching profile (right and left)" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val result = Await.result(matchingRepository.getByFiringAndMatchingProfile(SampleCode("AR-B-IMBICE-500"), SampleCode("AR-C-HIBA-500")), duration)

      result.isDefined mustBe true
    }

    "get an empty MatchResult by firing and matching profile" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val result = Await.result(matchingRepository.getByFiringAndMatchingProfile(SampleCode("AR-B-IMBICE-999"), SampleCode("AR-C-HIBA-500")), duration)

      result.isDefined mustBe false
    }
  }

  "A MatchingRepository repository" must {
    "get matches not discarded (left)" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val result = Await.result(matchingRepository.matchesNotDiscarded(SampleCode("AR-S-SECE-700")), duration)

      result.isEmpty mustBe false
      result.exists(mr => mr.rightProfile.globalCode.equals(SampleCode("AR-C-HIBA-500"))) mustBe true
      result.exists(mr => mr.rightProfile.globalCode.equals(SampleCode("AR-B-IMBICE-500"))) mustBe false
    }

    "get matches not discarded (right)" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val result = Await.result(matchingRepository.matchesNotDiscarded(SampleCode("AR-B-IMBICE-603")), duration)

      result.isEmpty mustBe true
    }

    "get matches not discarded or deleted" in {
      val matchingRepository = new MongoMatchingRepository(null)

      Await.result(matchingRepository.deleteMatches("54eb50cd2cdc8a94c6ee7950", SampleCode("AR-B-IMBICE-508")), duration)

      val result = Await.result(matchingRepository.matchesNotDiscarded(SampleCode("AR-C-HIBA-508")), duration)

      result.isEmpty mustBe true
    }
  }

  "A MatchingRepository repository" must {
    "get full hits" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val matchId = "54eb50cc2cdc8a94c6ee794c"
      Await.result(matchingRepository.convertStatus(matchId, SampleCode("AR-C-HIBA-500"), "hit"), duration)
      Await.result(matchingRepository.convertStatus(matchId, SampleCode("AR-B-IMBICE-501"), "hit"), duration)

      val result = Await.result(matchingRepository.matchesWithFullHit(SampleCode("AR-C-HIBA-500")), duration)

      result.exists(mr => mr._id.id == matchId) mustBe true

      Await.result(matchingRepository.convertStatus(matchId, SampleCode("AR-C-HIBA-500"), "pending"), duration)
      Await.result(matchingRepository.convertStatus(matchId, SampleCode("AR-B-IMBICE-501"), "pending"), duration)
    }

    "get full hits - no partial hits" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val matchId = "54eb50cc2cdc8a94c6ee794c"
      Await.result(matchingRepository.convertStatus(matchId, SampleCode("AR-B-IMBICE-501"), "hit"), duration)

      val result = Await.result(matchingRepository.matchesWithFullHit(SampleCode("AR-B-IMBICE-501")), duration)

      result.isEmpty mustBe true

      Await.result(matchingRepository.convertStatus(matchId, SampleCode("AR-B-IMBICE-501"), "pending"), duration)
    }

    "get full hits - empty matches with no hit" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val result = Await.result(matchingRepository.matchesWithFullHit(SampleCode("AR-B-IMBICE-501")), duration)

      result.isEmpty mustBe true
    }

    "get partial hits" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val matchId = "54eb50cc2cdc8a94c6ee794c"
      Await.result(matchingRepository.convertStatus(matchId, SampleCode("AR-B-IMBICE-501"), "hit"), duration)

      val result = Await.result(matchingRepository.matchesWithPartialHit(SampleCode("AR-B-IMBICE-501")), duration)

      result.exists(mr => mr._id.id == matchId) mustBe true

      Await.result(matchingRepository.convertStatus(matchId, SampleCode("AR-B-IMBICE-501"), "pending"), duration)
    }

    "get partial hits - empty matches with no hit" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val result = Await.result(matchingRepository.matchesWithPartialHit(SampleCode("AR-B-IMBICE-501")), duration)

      result.isEmpty mustBe true
    }
  }

  "A Matching Repository" must {
    "get matches" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val result = Await.result(matchingRepository.getMatches(MatchCardSearch("pdg", true)), duration)
      result.exists(m => (m \ "_id").as[String] == "AR-B-IMBICE-501") mustBe true
    }

    "get matches without deletions" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val matchId = "54eb50cc2cdc8a94c6ee794c"
      Await.result(matchingRepository.convertStatus(matchId, SampleCode("AR-B-IMBICE-501"), "deleted"), duration)
      Await.result(matchingRepository.convertStatus(matchId, SampleCode("AR-C-HIBA-500"), "deleted"), duration)

      val result = Await.result(matchingRepository.getMatches(MatchCardSearch("pdg", true)), duration)
      result.find(m => (m \ "_id").as[String] == "AR-B-IMBICE-501") mustBe None

      Await.result(matchingRepository.convertStatus(matchId, SampleCode("AR-B-IMBICE-501"), "pending"), duration)
      Await.result(matchingRepository.convertStatus(matchId, SampleCode("AR-C-HIBA-500"), "pending"), duration)
    }

    "get matches by both sides if user owns both profiles" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val result = Await.result(matchingRepository.getMatches(MatchCardSearch("tst-admintist", false)), duration)
      result.exists(m => (m \ "_id").as[String] == "AR-C-SHDG-2") mustBe true
      result.exists(m => (m \ "_id").as[String] == "AR-B-IMBICE-1002") mustBe true
    }

    "get matches by both sides if user is superuser" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val result = Await.result(matchingRepository.getMatches(MatchCardSearch("pdg", true)), duration)
      result.exists(m => (m \ "_id").as[String] == "AR-C-HIBA-641") mustBe true
      result.exists(m => (m \ "_id").as[String] == "AR-B-IMBICE-641") mustBe true
    }

    "get matches by correct side if user doesn't own both profiles" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val result = Await.result(matchingRepository.getMatches(MatchCardSearch("tst-clerknetist", false)), duration)
      result.exists(m => (m \ "_id").as[String] == "AR-C-HIBA-641") mustBe false
      result.exists(m => (m \ "_id").as[String] == "AR-B-IMBICE-641") mustBe true
    }

    "get matches by laboratory" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val results = Await.result(matchingRepository.getMatches(MatchCardSearch("pdg", true, laboratoryCode = Some("SHDG"))), duration)
      val noResults = Await.result(matchingRepository.getMatches(MatchCardSearch("pdg", true, laboratoryCode = Some("LABORATORY"))), duration)

      results.isEmpty mustBe false
      noResults.isEmpty mustBe true
    }

    "get matches by profile" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val results = Await.result(matchingRepository.getMatches(MatchCardSearch("pdg", true, profile = Some("AR-B-IMBICE-638"))), duration)
      val noResults = Await.result(matchingRepository.getMatches(MatchCardSearch("pdg", true, profile = Some("AR-C-IMBICE-638"))), duration)

      results.size mustBe 1
      noResults.isEmpty mustBe true
    }

    "get matches by status" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val results = Await.result(matchingRepository.getMatches(MatchCardSearch("pdg", true, status = Some(MatchGlobalStatus.pending))), duration)
      val noResults = Await.result(matchingRepository.getMatches(MatchCardSearch("pdg", true, status = Some(MatchGlobalStatus.discarded))), duration)

      results.isEmpty mustBe false
      noResults.isEmpty mustBe true
    }

    "get matches by hour from" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val format = new SimpleDateFormat("dd-MM-yyyy")
      val results = Await.result(matchingRepository.getMatches(MatchCardSearch("pdg", true, hourFrom = Some(format.parse("10-08-2015")))), duration)
      val noResults = Await.result(matchingRepository.getMatches(MatchCardSearch("pdg", true, hourFrom = Some(format.parse("15-08-2015")))), duration)

      results.isEmpty mustBe false
      noResults.isEmpty mustBe true
    }

    "get matches by hour until" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val format = new SimpleDateFormat("dd-MM-yyyy")
      val results = Await.result(matchingRepository.getMatches(MatchCardSearch("pdg", true, hourUntil = Some(format.parse("15-08-2015")))), duration)
      val noResults = Await.result(matchingRepository.getMatches(MatchCardSearch("pdg", true, hourUntil = Some(format.parse("10-02-2015")))), duration)

      results.isEmpty mustBe false
      noResults.isEmpty mustBe true
    }

    "get matches by multiple filters" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val format = new SimpleDateFormat("dd-MM-yyyy")
      val results = Await.result(matchingRepository.getMatches(MatchCardSearch("tst-admintist", false, status = Some(MatchGlobalStatus.pending), hourUntil = Some(format.parse("15-08-2015")))), duration)
      val noResults = Await.result(matchingRepository.getMatches(MatchCardSearch("tst-admintist", false, status = Some(MatchGlobalStatus.hit), hourUntil = Some(format.parse("15-08-2015")))), duration)

      results.isEmpty mustBe false
      noResults.isEmpty mustBe true
    }

    "sort matches by date" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val format = new SimpleDateFormat("dd-MM-yyyy")
      val ascending = Await.result(matchingRepository.getMatches(MatchCardSearch("pdg", true, ascending = true)), duration)
      val descending = Await.result(matchingRepository.getMatches(MatchCardSearch("pdg", true, ascending = false)), duration)

      (ascending(0) \ "lastDate").as[MongoDate].date mustBe >= ((ascending(1) \ "lastDate").as[MongoDate].date)
      (descending(0) \ "lastDate").as[MongoDate].date mustBe <= ((descending(1) \ "lastDate").as[MongoDate].date)
    }

    "get matches paginated" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val result = Await.result(matchingRepository.getMatches(MatchCardSearch("tst-admintist", false, pageSize=2)), duration)

      result.size mustBe 2
    }

    "get total matches" in {
      val matchingRepository = new MongoMatchingRepository(null)

      val nonEmpty = Await.result(matchingRepository.getTotalMatches(MatchCardSearch("tst-admintist", false)), duration)
      val empty = Await.result(matchingRepository.getTotalMatches(MatchCardSearch("fake_user", false)), duration)

      nonEmpty mustBe >(0)
      empty mustBe 0
    }

    "get matches by group and global code" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-B-IMBICE-500"), MatchKind.Normal, 1, 30, "", true)
      val result = matchingRepository.getMatchesByGroup(search)

      result.isEmpty mustBe false
    }

    "get matches by group and user" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val searchForResults = MatchGroupSearch("tst-clerknetist", false, SampleCode("AR-B-IMBICE-500"), MatchKind.Normal, 1, 30, "", true)
      val results = matchingRepository.getMatchesByGroup(searchForResults)

      val searchForNoResults = MatchGroupSearch("another-user", false, SampleCode("AR-B-IMBICE-500"), MatchKind.Normal, 1, 30, "", true)
      val noResults = matchingRepository.getMatchesByGroup(searchForNoResults)

      results.isEmpty mustBe false
      noResults.isEmpty mustBe true
    }

    "get matches by group and superuser" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val searchForResults = MatchGroupSearch("another-user", true, SampleCode("AR-B-IMBICE-500"), MatchKind.Normal, 1, 30, "", true)
      val results = matchingRepository.getMatchesByGroup(searchForResults)

      val searchForNoResults = MatchGroupSearch("another-user", false, SampleCode("AR-B-IMBICE-500"), MatchKind.Normal, 1, 30, "", true)
      val noResults = matchingRepository.getMatchesByGroup(searchForNoResults)

      results.isEmpty mustBe false
      noResults.isEmpty mustBe true
    }

    "get matches by group - reference & reference" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-B-IMBICE-500"), MatchKind.Normal, 1, 30, "", true)
      val result = matchingRepository.getMatchesByGroup(search)

      result.size mustBe 1
      result(0).globalCode mustBe SampleCode("AR-S-SECE-700")
    }

    "get matches by group - reference & evidence" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-B-IMBICE-500"), MatchKind.Restricted, 1, 30, "", true)
      val result = matchingRepository.getMatchesByGroup(search)

      result.size mustBe 1
      result(0).globalCode mustBe SampleCode("AR-C-HIBA-500")
    }

    "get matches by group - evidence n=2 & reference" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-C-HIBA-500"), MatchKind.Normal, 1, 30, "", true)
      val result = matchingRepository.getMatchesByGroup(search)

      result.size mustBe 3
      result(0).globalCode mustBe SampleCode("AR-B-IMBICE-500")
      result(1).globalCode mustBe SampleCode("AR-B-IMBICE-501")
      result(2).globalCode mustBe SampleCode("AR-S-SECE-700")
    }

    "get matches by group - evidence n=1 & reference" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-B-IMBICE-1001"), MatchKind.Normal, 1, 30, "", true)
      val result = matchingRepository.getMatchesByGroup(search)

      result.exists(_.globalCode == SampleCode("AR-C-SHDG-2")) mustBe true
    }

    "get matches by group - evidence n=1 & evidence n=1" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-B-IMBICE-1001"), MatchKind.Normal, 1, 30, "", true)
      val result = matchingRepository.getMatchesByGroup(search)

      result.exists(_.globalCode == SampleCode("AR-B-IMBICE-1002")) mustBe true
    }

    "get matches by group - evidence n=2 & evidence n=2" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-C-SHDG-500"), MatchKind.MixMix, 1, 30, "", true)
      val result = matchingRepository.getMatchesByGroup(search)

      result.size mustBe 2
      result(0).globalCode mustBe SampleCode("AR-C-SHDG-504")
      result(1).globalCode mustBe SampleCode("AR-C-SHDG-501")
    }

    "get matches by group - evidence n=1 & evidence n>1" in {
      val matchResult = MatchResult(MongoId("54eb50cc2cdc8a94c6ee7944"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-44"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-C-HIBA-641"),"tst-admintist",MatchStatus.pending,None),
        NewMatchingResult(Stringency.ModerateStringency, Map.empty,14,AlphanumericId("MULTIPLE"),0.7023809523809523,1.0,Algorithm.ENFSI), 1)

      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")
      Await.result(matchingRepository.insertMatchingResult(matchResult), duration)


      val search = MatchGroupSearch("user", true, SampleCode("AR-C-SHDG-44"), MatchKind.Restricted, 1, 30, "", true)
      val result = matchingRepository.getMatchesByGroup(search)

      Await.result(matches.remove(matchResult), duration)
      result.size mustBe 1
      result(0).globalCode mustBe SampleCode("AR-C-HIBA-641")

    }

    "get matches by group - evidence n=2 & evidence n=1" in {
      val matchResult = MatchResult(MongoId("54eb50cc2cdc8a94c6ee7944"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-SHDG-44"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-C-HIBA-641"),"tst-admintist",MatchStatus.pending,None),
        NewMatchingResult(Stringency.ModerateStringency, Map.empty,14,AlphanumericId("MULTIPLE"),0.7023809523809523,1.0,Algorithm.ENFSI), 1)

      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")
      Await.result(matchingRepository.insertMatchingResult(matchResult), duration)


      val search = MatchGroupSearch("user", true, SampleCode("AR-C-HIBA-641"), MatchKind.Normal, 1, 30, "", true)
      val result = matchingRepository.getMatchesByGroup(search)

      Await.result(matches.remove(matchResult), duration)
      result.exists(_.globalCode == SampleCode("AR-C-SHDG-44")) mustBe true

    }

    "get matches by group - evidence n=2 & evidence n>2" in {
      val profile = Profile(SampleCode("XX-X-TEST-0"), SampleCode("XX-X-TEST-0"), "", "", AlphanumericId("MULTIPLE"),
                            Map.empty, None, None, Some(3), None, None, None, false, true, false)

      val matchResult = MatchResult(MongoId("54eb50cc2cdc8a94c6ee7944"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("XX-X-TEST-0"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-C-HIBA-641"),"tst-admintist",MatchStatus.pending,None),
        NewMatchingResult(Stringency.ModerateStringency, Map.empty,14,AlphanumericId("MULTIPLE"),0.7023809523809523,1.0,Algorithm.ENFSI), 1)

      Await.result(profiles.insert(profile), duration)
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")
      Await.result(matchingRepository.insertMatchingResult(matchResult), duration)


      val search = MatchGroupSearch("user", true, SampleCode("AR-C-HIBA-641"), MatchKind.Other, 1, 30, "", true)
      val result = matchingRepository.getMatchesByGroup(search)

      Await.result(matches.remove(matchResult), duration)
      Await.result(profiles.remove(profile), duration)
      result.exists(_.globalCode == SampleCode("XX-X-TEST-0")) mustBe true

    }

    "get matches by group - evidence n>2 & evidence n>1" in {
      val profile = Profile(SampleCode("XX-X-TEST-0"), SampleCode("XX-X-TEST-0"), "", "", AlphanumericId("MULTIPLE"),
        Map.empty, None, None, Some(3), None, None, None, false, true, false)

      val matchResult = MatchResult(MongoId("54eb50cc2cdc8a94c6ee7944"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("XX-X-TEST-0"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-C-HIBA-641"),"tst-admintist",MatchStatus.pending,None),
        NewMatchingResult(Stringency.ModerateStringency, Map.empty,14,AlphanumericId("MULTIPLE"),0.7023809523809523,1.0,Algorithm.ENFSI), 1)

      Await.result(profiles.insert(profile), duration)
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")
      Await.result(matchingRepository.insertMatchingResult(matchResult), duration)


      val search = MatchGroupSearch("user", true, SampleCode("XX-X-TEST-0"), MatchKind.Other, 1, 30, "", true)
      val result = matchingRepository.getMatchesByGroup(search)

      Await.result(matches.remove(matchResult), duration)
      Await.result(profiles.remove(profile), duration)
      result.size mustBe 1
      result(0).globalCode mustBe SampleCode("AR-C-HIBA-641")

    }

    "get matches by group - evidence n>2 & reference" in {
      val profile = Profile(SampleCode("XX-X-TEST-0"), SampleCode("XX-X-TEST-0"), "", "", AlphanumericId("MULTIPLE"),
        Map.empty, None, None, Some(3), None, None, None, false, true, false)

      val matchResult = MatchResult(MongoId("54eb50cc2cdc8a94c6ee7944"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("XX-X-TEST-0"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-B-IMBICE-644"),"tst-admintist",MatchStatus.pending,None),
        NewMatchingResult(Stringency.ModerateStringency, Map.empty,14,AlphanumericId("MULTIPLE"),0.7023809523809523,1.0,Algorithm.ENFSI), 1)

      Await.result(profiles.insert(profile), duration)
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")
      Await.result(matchingRepository.insertMatchingResult(matchResult), duration)


      val search = MatchGroupSearch("user", true, SampleCode("XX-X-TEST-0"), MatchKind.Normal, 1, 30, "", true)
      val result = matchingRepository.getMatchesByGroup(search)

      Await.result(matches.remove(matchResult), duration)
      Await.result(profiles.remove(profile), duration)
      result.size mustBe 1
      result(0).globalCode mustBe SampleCode("AR-B-IMBICE-644")

    }

    "get matches by group - evidence n>2 & evidence n=1" in {
      val profile = Profile(SampleCode("XX-X-TEST-0"), SampleCode("XX-X-TEST-0"), "", "", AlphanumericId("MULTIPLE"),
        Map.empty, None, None, Some(3), None, None, None, false, true, false)

      val matchResult = MatchResult(MongoId("54eb50cc2cdc8a94c6ee7944"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("XX-X-TEST-0"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-C-SHDG-44"),"tst-admintist",MatchStatus.pending,None),
        NewMatchingResult(Stringency.ModerateStringency, Map.empty,14,AlphanumericId("UNICO"),0.7023809523809523,1.0,Algorithm.ENFSI), 1)

      Await.result(profiles.insert(profile), duration)
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")
      Await.result(matchingRepository.insertMatchingResult(matchResult), duration)


      val search = MatchGroupSearch("user", true, SampleCode("XX-X-TEST-0"), MatchKind.Normal, 1, 30, "", true)
      val result = matchingRepository.getMatchesByGroup(search)

      Await.result(matches.remove(matchResult), duration)
      Await.result(profiles.remove(profile), duration)
      result.size mustBe 1
      result(0).globalCode mustBe SampleCode("AR-C-SHDG-44")

    }

    "get matches by group and not deleted" in {
      val profile = Profile(SampleCode("XX-X-TEST-0"), SampleCode("XX-X-TEST-0"), "", "", AlphanumericId("MULTIPLE"),
        Map.empty, None, None, Some(3), None, None, None, false, true, false)

      val matchResult = MatchResult(MongoId("54eb50cc2cdc8a94c6ee7944"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("XX-X-TEST-0"),"tst-admintist",MatchStatus.deleted,None),
        MatchingProfile(SampleCode("AR-C-SHDG-44"),"tst-admintist",MatchStatus.deleted,None),
        NewMatchingResult(Stringency.ModerateStringency, Map.empty,14,AlphanumericId("UNICO"),0.7023809523809523,1.0,Algorithm.ENFSI), 1)

      Await.result(profiles.insert(profile), duration)
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")
      Await.result(matchingRepository.insertMatchingResult(matchResult), duration)


      val search = MatchGroupSearch("user", true, SampleCode("XX-X-TEST-0"), MatchKind.Normal, 1, 30, "", true)
      val result = matchingRepository.getMatchesByGroup(search)

      Await.result(matches.remove(matchResult), duration)
      Await.result(profiles.remove(profile), duration)
      result.isEmpty mustBe true

    }

    "get matches by group page 1" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-C-SHDG-500"), MatchKind.MixMix, 1, 1, "", true)
      val result = matchingRepository.getMatchesByGroup(search)

      result.size mustBe 1
      result(0).globalCode mustBe SampleCode("AR-C-SHDG-504")
    }

    "get matches by group page 2" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-C-SHDG-500"), MatchKind.MixMix, 2, 1, "", true)
      val result = matchingRepository.getMatchesByGroup(search)

      result.size mustBe 1
      result(0).globalCode mustBe SampleCode("AR-C-SHDG-501")
    }

    "get total matches by group" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-C-SHDG-500"), MatchKind.MixMix, 1, 1, "", true)
      val result = matchingRepository.getTotalMatchesByGroup(search)

      result mustBe 2
    }

    "get matches by group sort by globalCode ascending" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-C-HIBA-500"), MatchKind.Normal, 1, 30, "globalCode", true)
      val result = matchingRepository.getMatchesByGroup(search)

      result.size mustBe 3
      result(0).globalCode mustBe SampleCode("AR-B-IMBICE-500")
      result(1).globalCode mustBe SampleCode("AR-B-IMBICE-501")
      result(2).globalCode mustBe SampleCode("AR-S-SECE-700")
    }

    "get matches by group sort by globalCode descending" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-C-HIBA-500"), MatchKind.Normal, 1, 30, "globalCode", false)
      val result = matchingRepository.getMatchesByGroup(search)

      result.size mustBe 3
      result(0).globalCode mustBe SampleCode("AR-S-SECE-700")
      result(1).globalCode mustBe SampleCode("AR-B-IMBICE-501")
      result(2).globalCode mustBe SampleCode("AR-B-IMBICE-500")
    }

    "get matches by group sort by type descending" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-C-HIBA-500"), MatchKind.Normal, 1, 30, "type", false)
      val result = matchingRepository.getMatchesByGroup(search)

      result.size mustBe 3
      result(0).globalCode mustBe SampleCode("AR-S-SECE-700")
      result(1).globalCode mustBe SampleCode("AR-B-IMBICE-501")
      result(2).globalCode mustBe SampleCode("AR-B-IMBICE-500")
    }

    "get matches by group sort by type ascending" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-C-HIBA-500"), MatchKind.Normal, 1, 30, "type", true)
      val result = matchingRepository.getMatchesByGroup(search)

      result.size mustBe 3
      result(0).globalCode mustBe SampleCode("AR-B-IMBICE-500")
      result(1).globalCode mustBe SampleCode("AR-B-IMBICE-501")
      result(2).globalCode mustBe SampleCode("AR-S-SECE-700")
    }

    "get matches by group sort by total alleles ascending" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-C-HIBA-500"), MatchKind.Normal, 1, 30, "totalAlleles", true)
      val result = matchingRepository.getMatchesByGroup(search)

      result.size mustBe 3
      result(0).globalCode mustBe SampleCode("AR-B-IMBICE-500")
      result(1).globalCode mustBe SampleCode("AR-B-IMBICE-501")
      result(2).globalCode mustBe SampleCode("AR-S-SECE-700")
    }

    "get matches by group sort by total alleles descending" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-C-HIBA-500"), MatchKind.Normal, 1, 30, "totalAlleles", false)
      val result = matchingRepository.getMatchesByGroup(search)

      result.size mustBe 3
      result(0).globalCode mustBe SampleCode("AR-S-SECE-700")
      result(1).globalCode mustBe SampleCode("AR-B-IMBICE-501")
      result(2).globalCode mustBe SampleCode("AR-B-IMBICE-500")
    }

    "get matches by group sort by sharedAllelePonderation ascending" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-C-HIBA-500"), MatchKind.Normal, 1, 30, "sharedAllelePonderation", true)
      val result = matchingRepository.getMatchesByGroup(search)

      result.size mustBe 3
      result(0).globalCode mustBe SampleCode("AR-S-SECE-700")
      result(1).globalCode mustBe SampleCode("AR-B-IMBICE-500")
      result(2).globalCode mustBe SampleCode("AR-B-IMBICE-501")
    }

    "get matches by group sort by sharedAllelePonderation descending" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-C-HIBA-500"), MatchKind.Normal, 1, 30, "sharedAllelePonderation", false)
      val result = matchingRepository.getMatchesByGroup(search)

      result.size mustBe 3
      result(0).globalCode mustBe SampleCode("AR-B-IMBICE-501")
      result(1).globalCode mustBe SampleCode("AR-B-IMBICE-500")
      result(2).globalCode mustBe SampleCode("AR-S-SECE-700")
    }

    "get matches by group sort by ownerStatus ascending" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-C-HIBA-500"), MatchKind.Normal, 1, 30, "ownerStatus", true)
      val result = matchingRepository.getMatchesByGroup(search)

      result.size mustBe 3
      result(0).globalCode mustBe SampleCode("AR-B-IMBICE-500")
      result(1).globalCode mustBe SampleCode("AR-B-IMBICE-501")
      result(2).globalCode mustBe SampleCode("AR-S-SECE-700")
    }

    "get matches by group sort by ownerStatus descending" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-C-HIBA-500"), MatchKind.Normal, 1, 30, "ownerStatus", false)
      val result = matchingRepository.getMatchesByGroup(search)

      result.size mustBe 3
      result(0).globalCode mustBe SampleCode("AR-S-SECE-700")
      result(1).globalCode mustBe SampleCode("AR-B-IMBICE-501")
      result(2).globalCode mustBe SampleCode("AR-B-IMBICE-500")
    }

    "get matches by group sort by otherStatus ascending" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-C-HIBA-500"), MatchKind.Normal, 1, 30, "otherStatus", true)
      val result = matchingRepository.getMatchesByGroup(search)

      result.size mustBe 3
      result(0).globalCode mustBe SampleCode("AR-B-IMBICE-500")
      result(1).globalCode mustBe SampleCode("AR-B-IMBICE-501")
      result(2).globalCode mustBe SampleCode("AR-S-SECE-700")
    }

    "get matches by group sort by otherStatus descending" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-C-HIBA-500"), MatchKind.Normal, 1, 30, "otherStatus", false)
      val result = matchingRepository.getMatchesByGroup(search)

      result.size mustBe 3
      result(0).globalCode mustBe SampleCode("AR-S-SECE-700")
      result(1).globalCode mustBe SampleCode("AR-B-IMBICE-501")
      result(2).globalCode mustBe SampleCode("AR-B-IMBICE-500")
    }

    "get matches by group sort by date ascending" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-C-HIBA-500"), MatchKind.Normal, 1, 30, "", true)
      val result = matchingRepository.getMatchesByGroup(search)

      result.size mustBe 3
      result(0).globalCode mustBe SampleCode("AR-B-IMBICE-500")
      result(1).globalCode mustBe SampleCode("AR-B-IMBICE-501")
      result(2).globalCode mustBe SampleCode("AR-S-SECE-700")
    }

    "get matches by group sort by date descending" in {
      val matchingRepository = new MongoMatchingRepository("mongodb://localhost:27017/pdgdb-unit-test")

      val search = MatchGroupSearch("user", true, SampleCode("AR-C-HIBA-500"), MatchKind.Normal, 1, 30, "", false)
      val result = matchingRepository.getMatchesByGroup(search)

      result.size mustBe 3
      result(0).globalCode mustBe SampleCode("AR-S-SECE-700")
      result(1).globalCode mustBe SampleCode("AR-B-IMBICE-501")
      result(2).globalCode mustBe SampleCode("AR-B-IMBICE-500")
    }
  }
}