package matching

import play.api.libs.json.Json
import profile.Profile

case class CompareMixtureGenotypification(
  locus: Profile.Marker,
  g: Map[String, Seq[profile.AlleleValue]]
)

object CompareMixtureGenotypification:
  implicit val format: play.api.libs.json.OFormat[CompareMixtureGenotypification] =
    Json.format[CompareMixtureGenotypification]
