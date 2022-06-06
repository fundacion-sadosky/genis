package motive
import play.api.libs.json.{Format, Json}

case class Motive(id: Long, motiveType: Long, description:String,freeText: Boolean )

object Motive {
  implicit val motiveFormat: Format[Motive] = Json.format
}
case class MotiveType(id: Long, description:String )

object MotiveType {
  implicit val motiveTypeFormat: Format[MotiveType] = Json.format
}