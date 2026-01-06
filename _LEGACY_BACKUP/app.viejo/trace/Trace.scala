package trace

import java.util.Date

import play.api.libs.json.{Json, Writes, _}
import types.SampleCode

case class Trace(
   profile: SampleCode,
   user: String,
   date: Date,
   trace: TraceInfo,
   id: Long = 0) {
  val description = trace.description
  val kind = trace.kind
}

object Trace {
  implicit val writes = new Writes[Trace] {
    def writes(ti: Trace): JsValue = {
      Json.obj(
        "profile" -> ti.profile,
        "user" -> ti.user,
        "date" -> ti.date,
        "kind" -> ti.kind,
        "id" -> ti.id,
        "description" -> ti.description
      )
    }
  }
}
case class TracePedigree(
                  pedigree: Long,
                  user: String,
                  date: Date,
                  trace: TraceInfo,
                  id: Long = 0) {
  val description = trace.description
  val kind = trace.kind
}

object TracePedigree {
  implicit val writes = new Writes[TracePedigree] {
    def writes(ti: TracePedigree): JsValue = {
      Json.obj(
        "user" -> ti.user,
        "date" -> ti.date,
        "kind" -> ti.kind,
        "id" -> ti.id,
        "description" -> ti.description
      )
    }
  }
}