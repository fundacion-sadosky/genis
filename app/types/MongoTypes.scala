package types

import java.util.Date

import play.api.libs.json._
import play.api.libs.json.Json.toJsFieldJsValueWrapper

case class MongoId(id: String)

object MongoId {

  implicit val mongoIdReads = new Reads[MongoId] {
    def reads(mongoIdJson: JsValue) = {
      mongoIdJson \ "$oid" match {
        case a: JsUndefined => JsError("Formato inválido")
        case b: JsValue => JsSuccess(MongoId(b.as[String]))
      }
    }

  }

  implicit val mongoIdWrites = new Writes[MongoId] {
    def writes(mongoId: MongoId): JsValue = Json.obj("$oid" -> mongoId.id)
  }

}

case class MongoDate(date: Date)

object MongoDate {
  implicit val mongoDateReads = new Reads[MongoDate] {
    def reads(mongoIdJson: JsValue) = {
      val date: Date = (mongoIdJson \ "$date").as[Date]
      JsSuccess(MongoDate(date))
    }

  }

  implicit val mongoDateWrites = new Writes[MongoDate] {
    def writes(mongoDate: MongoDate): JsValue = Json.obj("$date" -> mongoDate.date)
  }
}