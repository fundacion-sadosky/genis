package matching

import play.api.libs.json.*
import play.api.libs.functional.syntax.*
import types.{AlphanumericId, MongoDate, SampleCode}
import matching.MatchStatus.MatchStatus
import profile.Profile

case class MongoId(id: String)

object MongoId {
  implicit val reads: Reads[MongoId] = Reads {
    case obj: JsObject => (obj \ "$oid").validate[String].map(MongoId(_))
    case _ => JsError("Expected object with $oid")
  }
  implicit val writes: Writes[MongoId] = Writes { mongoId =>
    Json.obj("$oid" -> mongoId.id)
  }
}

case class AleleRange(min: BigDecimal, max: BigDecimal)

object AleleRange {
  implicit val format: Format[AleleRange] = Json.format[AleleRange]
}

case class NewMatchingResult(
  stringency: Stringency.Value,
  matchingAlleles: NewMatchingResult.AlleleMatchResult,
  totalAlleles: Int,
  categoryId: AlphanumericId,
  leftPonderation: Double,
  rightPonderation: Double,
  algorithm: Algorithm.Value,
  allelesRanges: Option[NewMatchingResult.AlleleMatchRange] = None
)

object NewMatchingResult {
  type AlleleMatchResult = Map[Profile.Marker, Stringency.Value]
  type AlleleMatchRange = Map[Profile.Marker, AleleRange]

  implicit val reads: Reads[NewMatchingResult] = (
    (__ \ "stringency").read[Stringency.Value] ~
    (__ \ "matchingAlleles").read[AlleleMatchResult] ~
    (__ \ "totalAlleles").read[Int] ~
    (__ \ "categoryId").read[AlphanumericId] ~
    (__ \ "leftPonderation").read[Double] ~
    (__ \ "rightPonderation").read[Double] ~
    (__ \ "algorithm").read[Algorithm.Value] ~
    (__ \ "allelesRanges").readNullable[AlleleMatchRange]
  )(NewMatchingResult.apply)

  implicit val writes: OWrites[NewMatchingResult] = Json.writes[NewMatchingResult]
}

case class MatchingProfile(
  globalCode: SampleCode,
  assignee: String,
  status: MatchStatus,
  comments: Option[String],
  categoryId: AlphanumericId = AlphanumericId("IR")
)

object MatchingProfile {
  implicit val format: Format[MatchingProfile] = Json.format[MatchingProfile]
}

case class MatchResult(
  _id: MongoId,
  matchingDate: MongoDate,
  `type`: Int,
  leftProfile: MatchingProfile,
  rightProfile: MatchingProfile,
  result: NewMatchingResult,
  n: Long = 0,
  superiorProfileInfo: Option[JsValue] = None,
  idCourtCase: Option[Long] = None,
  lr: Double = 0.0,
  mismatches: Int = 0
)

object MatchResult {

  implicit val doubleReads: Reads[Double] = Reads {
    case JsNumber(n) => JsSuccess(n.toDouble)
    case JsString(s) => JsSuccess(s.toDouble)
    case _ => JsError("Expected number or string")
  }

  implicit val doubleWrites: Writes[Double] = Writes { l => JsString(l.toString) }

  implicit val reads: Reads[MatchResult] = (
    (__ \ "_id").read[MongoId] ~
    (__ \ "matchingDate").read[MongoDate] ~
    (__ \ "type").read[Int] ~
    (__ \ "leftProfile").read[MatchingProfile] ~
    (__ \ "rightProfile").read[MatchingProfile] ~
    (__ \ "result").read[NewMatchingResult] ~
    (__ \ "n").read[Long].orElse(Reads.pure(0L)) ~
    (__ \ "superiorProfileInfo").readNullable[JsValue] ~
    Reads.pure(None: Option[Long]) ~
    (__ \ "lr").read[Double](doubleReads) ~
    (__ \ "mismatches").read[Int]
  )(MatchResult.apply)

  implicit val writes: OWrites[MatchResult] = Json.writes[MatchResult]
}
