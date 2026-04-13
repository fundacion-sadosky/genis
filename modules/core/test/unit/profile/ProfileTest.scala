package unit.profile

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}
import profile.*
import types.{AlphanumericId, SampleCode}

class ProfileTest extends AnyWordSpec with Matchers {

  val sampleCode = SampleCode("AR-B-IMBICE-1")
  val categoryId = AlphanumericId("SOSPECHOSO")

  val genotypification: Profile.Genotypification = Map(
    "CSF1PO" -> List(AlleleValue("10"), AlleleValue("12")),
    "D3S1358" -> List(AlleleValue("15"), AlleleValue("16"))
  )

  val genotypificationByType: GenotypificationByType.GenotypificationByType = Map(
    1 -> genotypification
  )

  val profile = Profile(
    _id = sampleCode,
    globalCode = sampleCode,
    internalSampleCode = "ISC-001",
    assignee = "user1",
    categoryId = categoryId,
    genotypification = genotypificationByType,
    analyses = None,
    labeledGenotypification = None,
    contributors = Some(1),
    mismatches = None,
    matchingRules = None,
    associatedTo = None,
    deleted = false,
    matcheable = true,
    isReference = false,
    processed = false
  )

  "Profile" must {

    "serialize to JSON" in {
      val json = Json.toJson(profile)
      (json \ "globalCode").as[String] mustBe "AR-B-IMBICE-1"
      (json \ "internalSampleCode").as[String] mustBe "ISC-001"
      (json \ "assignee").as[String] mustBe "user1"
      (json \ "categoryId").as[String] mustBe "SOSPECHOSO"
      (json \ "deleted").as[Boolean] mustBe false
      (json \ "matcheable").as[Boolean] mustBe true
      (json \ "isReference").as[Boolean] mustBe false
      (json \ "contributors").as[Int] mustBe 1
    }

    "deserialize from JSON" in {
      val json = Json.toJson(profile)
      val parsed = json.validate[Profile]
      parsed mustBe a[JsSuccess[?]]
      val p = parsed.get
      p.globalCode mustBe sampleCode
      p.internalSampleCode mustBe "ISC-001"
      p.categoryId mustBe categoryId
      p.contributors mustBe Some(1)
      p.deleted mustBe false
    }

    "round-trip serialize/deserialize" in {
      val json = Json.toJson(profile)
      val deserialized = json.as[Profile]
      deserialized.globalCode mustBe profile.globalCode
      deserialized.internalSampleCode mustBe profile.internalSampleCode
      deserialized.assignee mustBe profile.assignee
      deserialized.categoryId mustBe profile.categoryId
      deserialized.matcheable mustBe profile.matcheable
    }

    "handle optional fields as None" in {
      val p = profile.copy(analyses = None, labeledGenotypification = None, matchingRules = None)
      val json = Json.toJson(p)
      val deserialized = json.as[Profile]
      deserialized.analyses mustBe None
      deserialized.labeledGenotypification mustBe None
      deserialized.matchingRules mustBe None
    }

    "handle labeled genotypification" in {
      val labels: Profile.LabeledGenotypification = Map(
        "Víctima" -> Map("CSF1PO" -> List(AlleleValue("10")))
      )
      val p = profile.copy(labeledGenotypification = Some(labels))
      val json = Json.toJson(p)
      val deserialized = json.as[Profile]
      deserialized.labeledGenotypification mustBe Some(labels)
    }
  }

  "GenotypificationByType" must {

    "serialize map with integer keys as strings" in {
      val json = Json.toJson(genotypificationByType)
      (json \ "1").toOption must not be empty
    }

    "deserialize from string-keyed JSON" in {
      val json = Json.obj("1" -> Json.toJson(genotypification))
      val parsed = json.as[GenotypificationByType.GenotypificationByType]
      parsed.keys must contain(1)
      parsed(1) mustBe genotypification
    }
  }

  "ProfileModelView" must {

    "serialize to JSON" in {
      val view = ProfileModelView(
        _id = Some(sampleCode),
        globalCode = Some(sampleCode),
        categoryId = Some(categoryId),
        genotypification = Some(genotypificationByType),
        analyses = Nil,
        labeledGenotypification = None,
        contributors = Some(1),
        matchingRules = None,
        associable = true,
        labelable = false,
        editable = true,
        isReference = false,
        readOnly = false,
        isUploadedToSuperior = false
      )
      val json = Json.toJson(view)
      (json \ "associable").as[Boolean] mustBe true
      (json \ "editable").as[Boolean] mustBe true
      (json \ "readOnly").as[Boolean] mustBe false
    }
  }
}
