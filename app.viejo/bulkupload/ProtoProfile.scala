package bulkupload

import configdata.MatchingRule
import play.api.libs.json._
import play.api.libs.functional.syntax._
import profile.Profile
import profiledata.ProfileDataAttempt
import types.SampleCode
import types.AlphanumericId

import scala.collection.immutable.ListMap
import profile.AlleleValue

case class GenotypificationItem (locus: Profile.Marker, alleles: List[AlleleValue])
object GenotypificationItem {
  implicit val viewFormat = Json.format[GenotypificationItem]
}

case class ProtoProfile(
  id: Long = 0,
  sampleName: String,
  assignee: String,
  category: String,
  status: ProtoProfileStatus.Value,
  kit: String,
  genotypification: ProtoProfile.Genotypification,
  mismatches: Profile.Mismatch,
  matchingRules: Seq[MatchingRule],
  errors: Seq[String],
  geneMapperLine: String,
  preexistence: Option[SampleCode],
  protoProfileData: Option[ProfileDataAttempt] = None,
  rejectMotive: Option[String] = None)

object ProtoProfile {
  
  type Genotypification = List[GenotypificationItem]

  implicit val protoProfileWrites: Writes[ProtoProfile] = (
    (__ \ "id").write[Long] and
    (__ \ "sampleName").write[String] and
    (__ \ "assignee").write[String] and
    (__ \ "category").write[String] and
    (__ \ "status").write[ProtoProfileStatus.Value] and
    (__ \ "kit").write[String] and
    (__ \ "genotypification").write[ProtoProfile.Genotypification] and
    (__ \ "mismatches").write[Profile.Mismatch] and
    (__ \ "matchingRules").write[Seq[MatchingRule]] and
    (__ \ "errors").write[Seq[String]] and
    (__ \ "preexistence").writeNullable[SampleCode] and
    (__ \ "protoProfileData").writeNullable[ProfileDataAttempt] and
    (__ \ "rejectMotive").writeNullable[String]) { p: ProtoProfile =>
      (p.id, p.sampleName, p.assignee, p.category, p.status, p.kit, p.genotypification,
        p.mismatches, p.matchingRules, p.errors, p.preexistence, p.protoProfileData, p.rejectMotive)
    }
}

case class ProtoProfileMatchingQuality(
  id: Long = 0,
  mismatches: Profile.Mismatch,
  matchingRules: Seq[MatchingRule])

object ProtoProfileMatchingQuality {
  implicit val viewFormat = Json.format[ProtoProfileMatchingQuality]
}

