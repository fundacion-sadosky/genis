package matching

import util.EnumJsonUtils
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.libs.json._

object Stringency extends Enumeration {
  type Stringency = Value
  val ImpossibleMatch, HighStringency, ModerateStringency, LowStringency, Mismatch, NoMatch = Value

  implicit val stringencyReads: Reads[Stringency] = EnumJsonUtils.enumReads(Stringency)

  implicit def stringencyWrites: Writes[Stringency] = EnumJsonUtils.enumWrites

}

