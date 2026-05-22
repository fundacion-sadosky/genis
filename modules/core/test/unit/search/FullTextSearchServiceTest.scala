package search

import fixtures.SearchFixtures
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import profiledata.{ProfileDataRepository, ProfileDataSearch}
import types.SampleCode

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*

class FullTextSearchServiceTest extends AnyWordSpec with Matchers with MockitoSugar:

  private given ExecutionContext = ExecutionContext.global
  private val timeout = 5.seconds

  private val emptySearch    = ProfileDataSearch(userId = "", isSuperUser = true)
  private val nonEmptySearch = ProfileDataSearch(userId = "", isSuperUser = true, input = "SOSPECHOSO")

  // ─── searchProfileDatas ───────────────────────────────────────────────────

  "searchProfileDatas" must {

    "delegate to profileDataRepo.getProfilesByUser when input is empty" in {
      val mockRepo = mock[ProfileDataRepository]
      when(mockRepo.getProfilesByUser(any[ProfileDataSearch]()))
        .thenReturn(Future.successful(Seq(SearchFixtures.profileDataFull1, SearchFixtures.profileDataFull2)))

      val service = new FullTextSearchServiceImpl(null, mockRepo)
      val result  = Await.result(service.searchProfileDatas(emptySearch), timeout)

      result mustBe Seq(SearchFixtures.profileDataFull1, SearchFixtures.profileDataFull2)
    }

    "call fullTextSearch.searchProfiles and build ProfileDataFull when input is non-empty" in {
      val profiles = Seq(SearchFixtures.profileData1, SearchFixtures.profileData2)
      val mockFts  = mock[FullTextSearch]
      val mockRepo = mock[ProfileDataRepository]

      when(mockFts.searchProfiles(any[ProfileDataSearch]()))
        .thenReturn(Future.successful(profiles))
      when(mockRepo.findUploadedProfilesByCodes(any[List[SampleCode]]()))
        .thenReturn(Future.successful(List.empty))
      when(mockRepo.getIsProfileReplicated(any[SampleCode]()))
        .thenReturn(Future.successful(false))

      val service  = new FullTextSearchServiceImpl(mockFts, mockRepo)
      val result   = Await.result(service.searchProfileDatas(nonEmptySearch), timeout)
      val expected = profiles.map(SearchFixtures.toFull(_, readOnly = false))

      result mustBe expected
    }

    "set readOnly=true in ProfileDataFull when getIsProfileReplicated returns true" in {
      val profiles = Seq(SearchFixtures.profileData1)
      val mockFts  = mock[FullTextSearch]
      val mockRepo = mock[ProfileDataRepository]

      when(mockFts.searchProfiles(any[ProfileDataSearch]()))
        .thenReturn(Future.successful(profiles))
      when(mockRepo.findUploadedProfilesByCodes(any[List[SampleCode]]()))
        .thenReturn(Future.successful(List.empty))
      when(mockRepo.getIsProfileReplicated(any[SampleCode]()))
        .thenReturn(Future.successful(true))

      val service = new FullTextSearchServiceImpl(mockFts, mockRepo)
      val result  = Await.result(service.searchProfileDatas(nonEmptySearch), timeout)

      result.head.readOnly mustBe true
    }

    "return empty sequence when fullTextSearch returns no profiles" in {
      val mockFts  = mock[FullTextSearch]
      val mockRepo = mock[ProfileDataRepository]

      when(mockFts.searchProfiles(any[ProfileDataSearch]()))
        .thenReturn(Future.successful(Seq.empty))
      when(mockRepo.findUploadedProfilesByCodes(any[List[SampleCode]]()))
        .thenReturn(Future.successful(List.empty))

      val service = new FullTextSearchServiceImpl(mockFts, mockRepo)
      val result  = Await.result(service.searchProfileDatas(nonEmptySearch), timeout)

      result mustBe Seq.empty
    }
  }

  // ─── searchTotalProfileDatas ──────────────────────────────────────────────

  "searchTotalProfileDatas" must {

    "delegate to profileDataRepo.getTotalProfilesByUser when input is empty" in {
      val mockRepo = mock[ProfileDataRepository]
      when(mockRepo.getTotalProfilesByUser(any[ProfileDataSearch]()))
        .thenReturn(Future.successful(42))

      val service = new FullTextSearchServiceImpl(null, mockRepo)
      val result  = Await.result(service.searchTotalProfileDatas(emptySearch), timeout)

      result mustBe 42
    }

    "delegate to fullTextSearch.searchTotalProfiles when input is non-empty" in {
      val mockFts  = mock[FullTextSearch]
      when(mockFts.searchTotalProfiles(any[ProfileDataSearch]()))
        .thenReturn(Future.successful(7))

      val service = new FullTextSearchServiceImpl(mockFts, null)
      val result  = Await.result(service.searchTotalProfileDatas(nonEmptySearch), timeout)

      result mustBe 7
    }
  }

  // ─── searchFilterTotalAndTotalProfileDatas ────────────────────────────────

  "searchFilterTotalAndTotalProfileDatas" must {

    "return (filteredSize, totalDBSize) both from repo when input is empty" in {
      val mockRepo = mock[ProfileDataRepository]
      when(mockRepo.getTotalProfilesByUser(any[String](), any[Boolean](), any[String]()))
        .thenReturn(Future.successful(100))
      when(mockRepo.getTotalProfilesByUser(any[ProfileDataSearch]()))
        .thenReturn(Future.successful(50))

      val service = new FullTextSearchServiceImpl(null, mockRepo)
      val result  = Await.result(service.searchFilterTotalAndTotalProfileDatas(emptySearch), timeout)

      result mustBe (50, 100)
    }

    "return (filteredSize from FTS, totalDBSize from repo) when input is non-empty" in {
      val mockFts  = mock[FullTextSearch]
      val mockRepo = mock[ProfileDataRepository]
      when(mockRepo.getTotalProfilesByUser(any[String](), any[Boolean](), any[String]()))
        .thenReturn(Future.successful(100))
      when(mockFts.searchTotalProfiles(any[ProfileDataSearch]()))
        .thenReturn(Future.successful(20))

      val service = new FullTextSearchServiceImpl(mockFts, mockRepo)
      val result  = Await.result(service.searchFilterTotalAndTotalProfileDatas(nonEmptySearch), timeout)

      result mustBe (20, 100)
    }
  }

  // ─── searchProfileDatasWithFilter ────────────────────────────────────────

  "searchProfileDatasWithFilter" must {

    "return all profiles when filter accepts everything" in {
      val profiles = Seq(SearchFixtures.profileData1, SearchFixtures.profileData2)
      val mockFts  = mock[FullTextSearch]
      when(mockFts.searchProfiles(any[ProfileDataSearch]()))
        .thenReturn(Future.successful(profiles))

      val service = new FullTextSearchServiceImpl(mockFts, null)
      val result  = Await.result(service.searchProfileDatasWithFilter("AR")(_ => true), timeout)

      result mustBe profiles
    }

    "apply filter and return only matching profiles" in {
      val profiles = Seq(SearchFixtures.profileData1, SearchFixtures.profileData2)
      val mockFts  = mock[FullTextSearch]
      when(mockFts.searchProfiles(any[ProfileDataSearch]()))
        .thenReturn(Future.successful(profiles))

      val service = new FullTextSearchServiceImpl(mockFts, null)
      val result  = Await.result(
        service.searchProfileDatasWithFilter("AR")(_.globalCode == SearchFixtures.profileData1.globalCode),
        timeout
      )

      result mustBe Seq(SearchFixtures.profileData1)
    }

    "use isSuperUser=true and pageSize=Int.MaxValue internally" in {
      val mockFts = mock[FullTextSearch]
      when(mockFts.searchProfiles(any[ProfileDataSearch]()))
        .thenReturn(Future.successful(Seq.empty))

      val service = new FullTextSearchServiceImpl(mockFts, null)
      Await.result(service.searchProfileDatasWithFilter("test")(_ => true), timeout)

      org.mockito.Mockito.verify(mockFts).searchProfiles(
        org.mockito.ArgumentMatchers.argThat[ProfileDataSearch](s =>
          s.isSuperUser && s.pageSize == Int.MaxValue && s.input == "test"
        )
      )
    }
  }

  // ─── searchProfileDatasWithFilterPaging ──────────────────────────────────

  "searchProfileDatasWithFilterPaging" must {

    "pass page and pageSize correctly to searchProfiles" in {
      val mockFts = mock[FullTextSearch]
      when(mockFts.searchProfiles(any[ProfileDataSearch]()))
        .thenReturn(Future.successful(Seq.empty))

      val service = new FullTextSearchServiceImpl(mockFts, null)
      Await.result(service.searchProfileDatasWithFilterPaging("AR", page = 2, pageSize = 10)(_ => true), timeout)

      org.mockito.Mockito.verify(mockFts).searchProfiles(
        org.mockito.ArgumentMatchers.argThat[ProfileDataSearch](s =>
          s.page == 2 && s.pageSize == 10 && s.isSuperUser
        )
      )
    }

    "apply filter to results" in {
      val profiles = Seq(SearchFixtures.profileData1, SearchFixtures.profileData2)
      val mockFts  = mock[FullTextSearch]
      when(mockFts.searchProfiles(any[ProfileDataSearch]()))
        .thenReturn(Future.successful(profiles))

      val service = new FullTextSearchServiceImpl(mockFts, null)
      val result  = Await.result(
        service.searchProfileDatasWithFilterPaging("AR", 0, 10)(_.deleted == false),
        timeout
      )

      result mustBe profiles // both have deleted=false
    }
  }

  // ─── searchProfileDatasWithFilterNodeAssociation ──────────────────────────

  "searchProfileDatasWithFilterNodeAssociation" must {

    "call searchProfilesNodeAssociation and apply filter" in {
      val batch   = SearchFixtures.profileDataWithBatch
      val mockFts = mock[FullTextSearch]
      when(mockFts.searchProfilesNodeAssociation(any[ProfileDataSearch]()))
        .thenReturn(Future.successful(Seq(batch)))

      val service = new FullTextSearchServiceImpl(mockFts, null)
      val result  = Await.result(
        service.searchProfileDatasWithFilterNodeAssociation("AR")(_ => true),
        timeout
      )

      result mustBe Seq(batch)
    }

    "use isSuperUser=true and pageSize=Int.MaxValue internally" in {
      val mockFts = mock[FullTextSearch]
      when(mockFts.searchProfilesNodeAssociation(any[ProfileDataSearch]()))
        .thenReturn(Future.successful(Seq.empty))

      val service = new FullTextSearchServiceImpl(mockFts, null)
      Await.result(service.searchProfileDatasWithFilterNodeAssociation("test")(_ => true), timeout)

      org.mockito.Mockito.verify(mockFts).searchProfilesNodeAssociation(
        org.mockito.ArgumentMatchers.argThat[ProfileDataSearch](s =>
          s.isSuperUser && s.pageSize == Int.MaxValue
        )
      )
    }

    "filter out non-matching results" in {
      val batch   = SearchFixtures.profileDataWithBatch
      val mockFts = mock[FullTextSearch]
      when(mockFts.searchProfilesNodeAssociation(any[ProfileDataSearch]()))
        .thenReturn(Future.successful(Seq(batch)))

      val service = new FullTextSearchServiceImpl(mockFts, null)
      val result  = Await.result(
        service.searchProfileDatasWithFilterNodeAssociation("AR")(_.deleted == true), // excludes all
        timeout
      )

      result mustBe Seq.empty
    }
  }
