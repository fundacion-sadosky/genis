package bulkupload

import configdata.MatchingRule
import play.api.libs.json.*
import play.api.libs.functional.syntax.*
import profile.{AlleleValue, Profile}
import profiledata.ProfileDataAttempt
import types.{AlphanumericId, SampleCode}

case class GenotypificationItem(locus: Profile.Marker, alleles: List[AlleleValue])

object GenotypificationItem:
  implicit val viewFormat: OFormat[GenotypificationItem] = Json.format[GenotypificationItem]

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
  rejectMotive: Option[String] = None
)

object ProtoProfile:
  type Genotypification = List[GenotypificationItem]

  // geneMapperLine intentionally omitted from JSON output (matches legacy)
  implicit val protoProfileWrites: Writes[ProtoProfile] = (
    (__ \ "id").write[Long] ~
    (__ \ "sampleName").write[String] ~
    (__ \ "assignee").write[String] ~
    (__ \ "category").write[String] ~
    (__ \ "status").write[ProtoProfileStatus.Value] ~
    (__ \ "kit").write[String] ~
    (__ \ "genotypification").write[ProtoProfile.Genotypification] ~
    (__ \ "mismatches").write[Profile.Mismatch] ~
    (__ \ "matchingRules").write[Seq[MatchingRule]] ~
    (__ \ "errors").write[Seq[String]] ~
    (__ \ "preexistence").writeNullable[SampleCode] ~
    (__ \ "protoProfileData").writeNullable[ProfileDataAttempt] ~
    (__ \ "rejectMotive").writeNullable[String]
  )((p: ProtoProfile) =>
    (p.id, p.sampleName, p.assignee, p.category, p.status, p.kit,
     p.genotypification, p.mismatches, p.matchingRules, p.errors,
     p.preexistence, p.protoProfileData, p.rejectMotive)
  )

case class ProtoProfileMatchingQuality(
  id: Long = 0,
  mismatches: Profile.Mismatch,
  matchingRules: Seq[MatchingRule]
)

object ProtoProfileMatchingQuality:
  implicit val viewFormat: OFormat[ProtoProfileMatchingQuality] = Json.format[ProtoProfileMatchingQuality]
