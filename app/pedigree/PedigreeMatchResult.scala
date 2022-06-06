package pedigree

import matching.{MatchingProfile, NewMatchingResult}
import play.api.libs.json.{Format, Writes, _}
import types.{MongoDate, MongoId}

trait PedigreeMatchResult {
  val _id: MongoId
  val matchingDate: MongoDate
  val `type`: Int
  val profile: MatchingProfile
  val pedigree: PedigreeMatchingProfile
  val kind: PedigreeMatchKind.Value
}

case class PedigreeDirectLinkMatch(
  _id: MongoId,
  matchingDate: MongoDate,
  `type`: Int,
  profile: MatchingProfile,
  pedigree: PedigreeProfileInfo,
  result: NewMatchingResult) extends PedigreeMatchResult {
  override val kind = PedigreeMatchKind.DirectLink
}

object PedigreeDirectLinkMatch {
  implicit val reads: Reads[PedigreeDirectLinkMatch] = Json.reads[PedigreeDirectLinkMatch]

  implicit val writes: OWrites[PedigreeDirectLinkMatch] = new OWrites[PedigreeDirectLinkMatch] {
    def writes(mr: PedigreeDirectLinkMatch): JsObject = {
      Json.obj(
        "_id" -> mr._id,
        "matchingDate" -> mr.matchingDate,
        "type" -> mr.`type`,
        "profile" -> mr.profile,
        "pedigree" -> mr.pedigree,
        "result" -> mr.result,
        "kind" -> mr.kind
      )
    }
  }

  implicit val format: OFormat[PedigreeDirectLinkMatch] = OFormat(reads, writes)
}

case class PedigreeMissingInfoMatch(
  _id: MongoId,
  matchingDate: MongoDate,
  `type`: Int,
  profile: MatchingProfile,
  pedigree: PedigreeProfileInfo) extends PedigreeMatchResult {
  override val kind = PedigreeMatchKind.MissingInfo
}

object PedigreeMissingInfoMatch {
  implicit val reads: Reads[PedigreeMissingInfoMatch] = Json.reads[PedigreeMissingInfoMatch]

  implicit val writes: OWrites[PedigreeMissingInfoMatch] = new OWrites[PedigreeMissingInfoMatch] {
    def writes(mr: PedigreeMissingInfoMatch): JsObject = {
      Json.obj(
        "_id" -> mr._id,
        "matchingDate" -> mr.matchingDate,
        "type" -> mr.`type`,
        "profile" -> mr.profile,
        "pedigree" -> mr.pedigree,
        "kind" -> mr.kind
      )
    }
  }

  implicit val format: OFormat[PedigreeMissingInfoMatch] = OFormat(reads, writes)
}

case class PedigreeCompatibilityMatch(
   _id: MongoId,
   matchingDate: MongoDate,
   `type`: Int,
   profile: MatchingProfile,
   pedigree: PedigreeInfo,
   compatibility: Double,
   matchingId:String = "",
   mtProfile:String = "",
   message:String = "") extends PedigreeMatchResult {
  override val kind = PedigreeMatchKind.Compatibility
}

object PedigreeCompatibilityMatch {
  implicit val reads: Reads[PedigreeCompatibilityMatch] = Json.reads[PedigreeCompatibilityMatch]

  implicit val writes: OWrites[PedigreeCompatibilityMatch] = new OWrites[PedigreeCompatibilityMatch] {
    def writes(mr: PedigreeCompatibilityMatch): JsObject = {
      Json.obj(
        "_id" -> mr._id,
        "matchingDate" -> mr.matchingDate,
        "type" -> mr.`type`,
        "profile" -> mr.profile,
        "pedigree" -> mr.pedigree,
        "compatibility" -> mr.compatibility,
        "kind" -> mr.kind,
        "matchingId" -> mr.matchingId,
        "mtProfile" -> mr.mtProfile,
        "message" -> mr.message
      )
    }
  }

  implicit val format: OFormat[PedigreeCompatibilityMatch] = OFormat(reads, writes)
}

object PedigreeMatchResult {
  implicit val longReads: Reads[Long] = new Reads[Long] {
    def reads(jv: JsValue): JsResult[Long] = JsSuccess(jv.as[String].toLong)
  }

  implicit val longWrites: Writes[Long] = new Writes[Long] {
    def writes(l: Long): JsValue = JsString(l.toString)
  }

  implicit val longFormat: Format[Long] = Format(longReads, longWrites)

  implicit val writes: OWrites[PedigreeMatchResult] = new OWrites[PedigreeMatchResult] {
    def writes(mr: PedigreeMatchResult): JsObject = {
      mr match {
        case p: PedigreeDirectLinkMatch => Json.toJson(p)(implicitly[OFormat[PedigreeDirectLinkMatch]]).as[JsObject]
        case p: PedigreeCompatibilityMatch => Json.toJson(p)(implicitly[OFormat[PedigreeCompatibilityMatch]]).as[JsObject]
        case p: PedigreeMissingInfoMatch => Json.toJson(p)(implicitly[OFormat[PedigreeMissingInfoMatch]]).as[JsObject]
      }
    }
  }

  implicit val reads: Reads[PedigreeMatchResult] = new Reads[PedigreeMatchResult] {
    def reads(js: JsValue): JsResult[PedigreeMatchResult] = {
      (js \ "kind").as[PedigreeMatchKind.Value] match {
        case PedigreeMatchKind.DirectLink => Json.fromJson(js)(implicitly[OFormat[PedigreeDirectLinkMatch]])
        case PedigreeMatchKind.Compatibility => Json.fromJson(js)(implicitly[OFormat[PedigreeCompatibilityMatch]])
        case PedigreeMatchKind.MissingInfo => Json.fromJson(js)(implicitly[OFormat[PedigreeMissingInfoMatch]])
      }
    }
  }

  implicit val format: OFormat[PedigreeMatchResult] = OFormat(reads, writes)

  def unapply(directLink: PedigreeDirectLinkMatch) =
    Some((directLink._id, directLink.matchingDate, directLink.`type`, directLink.profile, directLink.pedigree, directLink.result))

  def apply(_id: MongoId, matchingDate: MongoDate, `type`: Int, profile: MatchingProfile, pedigree: PedigreeProfileInfo, result: NewMatchingResult) =
    PedigreeDirectLinkMatch(_id, matchingDate, `type`, profile, pedigree, result)

  def unapply(missingInfo: PedigreeMissingInfoMatch) =
    Some((missingInfo._id, missingInfo.matchingDate, missingInfo.`type`, missingInfo.profile, missingInfo.pedigree))

  def apply(_id: MongoId, matchingDate: MongoDate, `type`: Int, profile: MatchingProfile, pedigree: PedigreeProfileInfo) =
    PedigreeMissingInfoMatch(_id, matchingDate, `type`, profile, pedigree)

  def unapply(compatibility: PedigreeCompatibilityMatch) =
    Some((compatibility._id, compatibility.matchingDate, compatibility.`type`, compatibility.profile, compatibility.pedigree, compatibility.compatibility))

  def apply(_id: MongoId, matchingDate: MongoDate, `type`: Int, profile: MatchingProfile, pedigree: PedigreeInfo, compatibility: Double) =
    PedigreeCompatibilityMatch(_id, matchingDate, `type`, profile, pedigree, compatibility)

}