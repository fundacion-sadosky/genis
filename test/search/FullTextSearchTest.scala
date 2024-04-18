package search

import profiledata.ProfileDataSearch
import specs.PdgSpec

import scala.concurrent.Await
import scala.concurrent.duration._

class FullTextSearchTest extends PdgSpec {

  val duration = Duration(10, SECONDS)

  "FullTextSearch" must {
    "search profiles - input empty" in {

      val fts = new FullTextSearchPg

      val search = ProfileDataSearch("", true, 0, Int.MaxValue, "", true, true)
      val profiles = Await.result(fts.searchProfiles(search), duration)

      profiles.size mustBe 0
    }

    "search profiles - input null" in {

      val fts = new FullTextSearchPg

      val search = ProfileDataSearch("", true, 0, Int.MaxValue, null, true, true)
      val profiles = Await.result(fts.searchProfiles(search), duration)

      profiles.size mustBe 0
    }

    "search profiles - input with white spaces" in {

      val fts = new FullTextSearchPg

      val search = ProfileDataSearch("", true, 0, Int.MaxValue, "     ", true, true)
      val profiles = Await.result(fts.searchProfiles(search), duration)

      profiles.size mustBe 0
    }

    "search profiles - retrieve 1021 profiles starting with AR" in {

      val fts = new FullTextSearchPg

      val search = ProfileDataSearch("", true, 0, Int.MaxValue, "AR", true, true)
      val profiles = Await.result(fts.searchProfiles(search), duration)

      profiles.size mustBe 1021
      profiles.map(_.globalCode).foreach {
        gc => gc.text must startWith ("AR")
      }
    }

    "search profiles - retrieve 22 profiles starting with IMG - case insensitive" in {

      val fts = new FullTextSearchPg

      val search = ProfileDataSearch("", true, 0, Int.MaxValue, "img", true, true)
      val profiles = Await.result(fts.searchProfiles(search), duration)

      profiles.size mustBe 22
      profiles.map(_.internalSampleCode).foreach {
        isc => isc must startWith ("IMG")
      }
    }

    "search profiles - input im retrieve 22 profiles starting with IMG - case insensitive" in {

      val fts = new FullTextSearchPg

      val search = ProfileDataSearch("", true, 0, Int.MaxValue, "im", true, true)
      val profiles = Await.result(fts.searchProfiles(search), duration)

      profiles.size mustBe 22
      profiles.map(_.internalSampleCode).foreach {
        isc => isc must startWith ("IMG")
      }
    }

    "search profiles - retrieve only profiles having category sospechoso - case insensitive" in {

      val fts = new FullTextSearchPg

      val search = ProfileDataSearch("", true, 0, Int.MaxValue, "sospechoso", true, true)
      val profiles = Await.result(fts.searchProfiles(search), duration)

      profiles.size mustBe 20
      profiles.map(_.category).foreach {
        c => c.text mustBe "SOSPECHOSO"
      }
    }

    "search profiles - input sosp retrieve only profiles having category sospechoso" in {

      val fts = new FullTextSearchPg

      val search = ProfileDataSearch("", true, 0, Int.MaxValue, "sosp", true, true)
      val profiles = Await.result(fts.searchProfiles(search), duration)

      profiles.size mustBe 20
      profiles.map(_.category).foreach {
        c => c.text mustBe "SOSPECHOSO"
      }
    }

    "search profiles - no results for only deleted profiles" in {

      val fts = new FullTextSearchPg

      val search = ProfileDataSearch("", true, 0, Int.MaxValue, "sosp", false, true)
      val profiles = Await.result(fts.searchProfiles(search), duration)

      profiles.isEmpty mustBe true
    }
  }
}