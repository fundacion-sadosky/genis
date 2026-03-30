package profile

import matching.Stringency.Stringency
import play.api.libs.json.*
import types.SampleCode

case class ProfileAsociation(profile: SampleCode, stringency: Stringency, genotypification: Profile.Genotypification)

object ProfileAsociation {
  implicit val format: Format[ProfileAsociation] = Json.format[ProfileAsociation]
}
