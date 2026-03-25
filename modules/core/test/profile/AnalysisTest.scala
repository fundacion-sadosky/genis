package profile

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}
import types.SampleCode

class AnalysisTest extends PlaySpec {

  "NewAnalysis" should {

    "serialize to JSON" in {
      val na = NewAnalysis(
        globalCode = SampleCode("AR-B-IMBICE-1"),
        userId = "user1",
        token = "token123",
        kit = Some("Identifiler"),
        `type` = Some(1),
        genotypification = Map(
          "CSF1PO" -> List(AlleleValue("10"), AlleleValue("12"))
        ),
        labeledGenotypification = None,
        contributors = Some(1),
        mismatches = None
      )
      val json = Json.toJson(na)
      (json \ "globalCode").as[String] mustBe "AR-B-IMBICE-1"
      (json \ "userId").as[String] mustBe "user1"
      (json \ "token").as[String] mustBe "token123"
      (json \ "kit").as[String] mustBe "Identifiler"
    }

    "deserialize from JSON" in {
      val json = Json.obj(
        "globalCode" -> "AR-B-IMBICE-1",
        "userId" -> "user1",
        "token" -> "token123",
        "genotypification" -> Json.obj(
          "CSF1PO" -> Json.arr("10", "12")
        ),
        "labeledGenotypification" -> None.orNull,
        "contributors" -> 1,
        "mismatches" -> None.orNull
      )
      val parsed = json.validate[NewAnalysis]
      parsed mustBe a[JsSuccess[?]]
      parsed.get.globalCode mustBe SampleCode("AR-B-IMBICE-1")
      parsed.get.contributors mustBe Some(1)
    }

    "round-trip serialize/deserialize" in {
      val na = NewAnalysis(
        globalCode = SampleCode("AR-B-IMBICE-1"),
        userId = "user1",
        token = "token123",
        genotypification = Map("D3S1358" -> List(AlleleValue("15"))),
        labeledGenotypification = None,
        contributors = None,
        mismatches = None
      )
      val json = Json.toJson(na)
      val back = json.as[NewAnalysis]
      back.globalCode mustBe na.globalCode
      back.userId mustBe na.userId
      back.token mustBe na.token
    }
  }

  "Analysis" should {

    "serialize to JSON" in {
      val a = Analysis(
        id = "analysis-1",
        date = types.MongoDate(new java.util.Date()),
        kit = "Identifiler",
        genotypification = Map("CSF1PO" -> List(AlleleValue("10"))),
        `type` = Some(1)
      )
      val json = Json.toJson(a)
      (json \ "id").as[String] mustBe "analysis-1"
      (json \ "kit").as[String] mustBe "Identifiler"
    }
  }

  "FileUploadedType" should {

    "serialize to JSON" in {
      val f = FileUploadedType("file-123", "image.png")
      val json = Json.toJson(f)
      (json \ "fileId").as[String] mustBe "file-123"
      (json \ "name").as[String] mustBe "image.png"
    }

    "deserialize from JSON" in {
      val json = Json.obj("fileId" -> "f1", "name" -> "photo.jpg")
      val parsed = json.as[FileUploadedType]
      parsed.fileId mustBe "f1"
      parsed.name mustBe "photo.jpg"
    }
  }
}
