package profile

import matching.Stringency.Stringency
import play.api.libs.json._
import types.SampleCode

case class ProfileAsociation (profile: SampleCode, stringency: Stringency, genotypification: Profile.Genotypification)

object ProfileAsociation {
  implicit val pa = Json.format[ProfileAsociation]
}