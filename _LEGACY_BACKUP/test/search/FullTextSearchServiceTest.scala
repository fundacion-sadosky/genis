package search

import profiledata.{ProfileDataRepository, ProfileDataSearch}
import specs.PdgSpec

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import stubs.Stubs
import types.SampleCode

class FullTextSearchServiceTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)

  val profiles = Seq(Stubs.profileData, Stubs.profileData2nd)
  val profilesFull = Seq(Stubs.profileDataFull, Stubs.profileData2ndFull)

  "FullTextSearchService" must {
    "search profiles - input empty" in {
      val search = ProfileDataSearch("", true, 0, Int.MaxValue, "", true, true)

      val mockProfileDataRepo = mock[ProfileDataRepository]
      when(mockProfileDataRepo.getProfilesByUser(any[ProfileDataSearch])).thenReturn(Future.successful(profilesFull))

      val service = new FullTextSearchServiceImpl(null, mockProfileDataRepo)

      val result = Await.result(service.searchProfileDatas(search), duration)

      result mustBe profilesFull
    }

    "search profiles - input non empty" in {
      val search = ProfileDataSearch("", true, 0, Int.MaxValue, "SOSPECHOSO", true, true)

      val mockFullTextSearch = mock[FullTextSearch]
      when(mockFullTextSearch.searchProfiles(any[ProfileDataSearch])).thenReturn(Future.successful(profiles))
      val mockProfileDataRepo = mock[ProfileDataRepository]
      when(mockProfileDataRepo.getProfilesByUser(any[ProfileDataSearch])).thenReturn(Future.successful(profilesFull))
      when(mockProfileDataRepo.findUploadedProfilesByCodes(any[Seq[SampleCode]])).thenReturn(Future.successful(Nil))

      val service = new FullTextSearchServiceImpl(mockFullTextSearch, mockProfileDataRepo)

      val result = Await.result(service.searchProfileDatas(search), duration)

      result mustBe profilesFull
    }

    "search total profiles - input empty" in {
      val search = ProfileDataSearch("", true, 0, Int.MaxValue, "", true, true)
      val mockResult = 4

      val mockProfileDataRepo = mock[ProfileDataRepository]
      when(mockProfileDataRepo.getTotalProfilesByUser(any[ProfileDataSearch])).thenReturn(Future.successful(mockResult))

      val service = new FullTextSearchServiceImpl(null, mockProfileDataRepo)

      val result = Await.result(service.searchTotalProfileDatas(search), duration)

      result mustBe mockResult
    }

    "search total profiles - input non empty" in {
      val search = ProfileDataSearch("", true, 0, Int.MaxValue, "SOSPECHOSO", true, true)
      val mockResult = 4

      val mockFullTextSearch = mock[FullTextSearch]
      when(mockFullTextSearch.searchTotalProfiles(any[ProfileDataSearch])).thenReturn(Future.successful(mockResult))

      val service = new FullTextSearchServiceImpl(mockFullTextSearch, null)

      val result = Await.result(service.searchTotalProfileDatas(search), duration)

      result mustBe mockResult
    }

    "search profile datas with filter" in {
      val mockFullTextSearch = mock[FullTextSearch]
      when(mockFullTextSearch.searchProfiles(any[ProfileDataSearch])).thenReturn(Future.successful(profiles))

      val service = new FullTextSearchServiceImpl(mockFullTextSearch, null)

      val result = Await.result(service.searchProfileDatasWithFilter("FE01")(_ => true), duration)

      result mustBe profiles
    }

  }

}