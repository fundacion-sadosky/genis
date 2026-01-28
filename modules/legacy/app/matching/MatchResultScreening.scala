package matching

import play.api.libs.json.Json

case class MatchResultScreening(globalCode:String,matchId:String,deleteable:Boolean = true)
object MatchResultScreening {
  implicit val matchResultScreening = Json.format[MatchResultScreening]
}