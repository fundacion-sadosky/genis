package types

import java.util.Date
import play.api.libs.json.*

case class MongoDate(date: Date)

object MongoDate {
  implicit val mongoDateReads: Reads[MongoDate] = new Reads[MongoDate] {
    def reads(json: JsValue): JsResult[MongoDate] = {
      val date: Date = (json \ "$date").as[Date]
      JsSuccess(MongoDate(date))
    }
  }

  implicit val mongoDateWrites: Writes[MongoDate] = new Writes[MongoDate] {
    def writes(mongoDate: MongoDate): JsValue = Json.obj("$date" -> mongoDate.date)
  }
}
