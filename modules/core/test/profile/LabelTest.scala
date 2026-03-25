package profile

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class LabelTest extends PlaySpec {

  "Label" should {

    "serialize to JSON" in {
      val label = Label("1", "Víctima")
      val json = Json.toJson(label)
      (json \ "id").as[String] mustBe "1"
      (json \ "caption").as[String] mustBe "Víctima"
    }

    "deserialize from JSON" in {
      val json = Json.obj("id" -> "2", "caption" -> "Sospechoso")
      val label = json.as[Label]
      label.id mustBe "2"
      label.caption mustBe "Sospechoso"
    }

    "round-trip through JSON" in {
      val label = Label("3", "Otro")
      val back = Json.toJson(label).as[Label]
      back mustBe label
    }
  }
}
