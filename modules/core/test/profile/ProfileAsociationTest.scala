package profile

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import matching.Stringency
import types.SampleCode

class ProfileAsociationTest extends PlaySpec {

  "ProfileAsociation" should {

    "serialize to JSON" in {
      val pa = ProfileAsociation(
        profile = SampleCode("AR-B-IMBICE-1"),
        stringency = Stringency.HighStringency,
        genotypification = Map("CSF1PO" -> List(AlleleValue("10"), AlleleValue("12")))
      )
      val json = Json.toJson(pa)
      (json \ "profile").as[String] mustBe "AR-B-IMBICE-1"
    }

    "round-trip through JSON" in {
      val pa = ProfileAsociation(
        profile = SampleCode("AR-B-IMBICE-2"),
        stringency = Stringency.ModerateStringency,
        genotypification = Map("D3S1358" -> List(AlleleValue("15")))
      )
      val json = Json.toJson(pa)
      val back = json.as[ProfileAsociation]
      back.profile mustBe pa.profile
      back.stringency mustBe pa.stringency
    }
  }
}
