package pedigree

import play.api.libs.json.{JsResult, JsValue, Json}
import types.MongoDate
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class PedigreeMatch (
   _id: Either[Long, String],
   lastMatchDate: MongoDate,
   count: Int,
   assignee: String
)

object PedigreeMatch {
  implicit def reads: Reads[PedigreeMatch] = new Reads[PedigreeMatch] {
    override def reads(json: JsValue): JsResult[PedigreeMatch] = {
      val reads = (
        (__ \ "_id").read[Long] ~
          (__ \ "lastMatchDate").read[MongoDate] ~
          (__ \ "count").read[Int] ~
          (__ \ "assignee").read[String]
        ) { (id: Long, date: MongoDate, count: Int, assignee: String) => PedigreeMatch(Left(id), date, count, assignee)
      } | (
        (__ \ "_id").read[String] ~
          (__ \ "lastMatchDate").read[MongoDate] ~
          (__ \ "count").read[Int] ~
          (__ \ "assignee").read[String]
        ) { (id: String, date: MongoDate, count: Int, assignee: String) => PedigreeMatch(Right(id), date, count, assignee)
      }
      reads.reads(json)
    }
  }
}