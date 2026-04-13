package unit.profile

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import profile.*
import java.util.Date

class ExportFiltersTest extends AnyWordSpec with Matchers {

  "ExportProfileFilters" must {

    "serialize to JSON" in {
      val f = ExportProfileFilters(
        user = "user1",
        isSuperUser = false,
        internalSampleCode = Some("ISC-001"),
        categoryId = Some("SOSPECHOSO"),
        laboratory = Some("IMBICE")
      )
      val json = Json.toJson(f)
      (json \ "user").as[String] mustBe "user1"
      (json \ "isSuperUser").as[Boolean] mustBe false
      (json \ "internalSampleCode").as[String] mustBe "ISC-001"
    }

    "deserialize from JSON" in {
      val json = Json.obj(
        "user" -> "admin",
        "isSuperUser" -> true,
        "internalSampleCode" -> "ISC-002"
      )
      val f = json.as[ExportProfileFilters]
      f.user mustBe "admin"
      f.isSuperUser mustBe true
      f.categoryId mustBe None
    }

    "round-trip with dates" in {
      val now = new Date()
      val f = ExportProfileFilters("u", false, None, hourFrom = Some(now), hourUntil = Some(now))
      val json = Json.toJson(f)
      val back = json.as[ExportProfileFilters]
      back.user mustBe "u"
      back.hourFrom must not be empty
    }
  }

  "ExportLimsFilesFilter" must {

    "serialize to JSON" in {
      val f = ExportLimsFilesFilter(user = "user1", isSuperUser = false, tipo = "alta")
      val json = Json.toJson(f)
      (json \ "user").as[String] mustBe "user1"
      (json \ "tipo").as[String] mustBe "alta"
    }

    "deserialize from JSON" in {
      val json = Json.obj("user" -> "admin", "isSuperUser" -> true, "tipo" -> "match")
      val f = json.as[ExportLimsFilesFilter]
      f.tipo mustBe "match"
      f.isSuperUser mustBe true
    }
  }
}
