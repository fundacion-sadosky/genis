package matching

import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import profile.Profile
import types.{AlphanumericId, SampleCode}

// ---------------------------------------------------------------------------
// MatchingResult — view model derived from a MatchResult for display in the
// UI. Distinct from MatchResult (the raw MongoDB document).
// ---------------------------------------------------------------------------

case class MatchingResult(
  oid: String,
  globalCode: SampleCode,
  internalSampleCode: String,
  stringency: Stringency.Value,
  matchingAlleles: MatchingResult.AlleleMatchResult,
  totalAlleles: Int,
  categoryId: AlphanumericId,
  ownerStatus: MatchStatus.Value,
  otherStatus: MatchStatus.Value,
  matchGlobalStatus: MatchGlobalStatus.Value,
  sharedAllelePonderation: Double,
  contributors: Int,
  isReference: Boolean,
  algorithm: Algorithm.Value,
  `type`: Int,
  allelesRanges: Option[NewMatchingResult.AlleleMatchRange] = None,
  lr: Double = 0.0,
  mismatches: Int = 0,
  isInterconnectionMatch: Boolean = false
)

object MatchingResult:
  type AlleleMatchResult = Map[Profile.Marker, Stringency.Value]

  implicit val doubleReads: Reads[Double] = Reads(jv => JsSuccess(jv.toString.toDouble))
  implicit val doubleWrites: Writes[Double] = Writes(l => Json.toJson(l.toString))
  implicit val doubleFormat: Format[Double] = Format(doubleReads, doubleWrites)

  implicit val matchingResultReads: Reads[MatchingResult] = (
    (__ \ "oid").read[String] ~
    (__ \ "globalCode").read[SampleCode] ~
    (__ \ "internalSampleCode").read[String] ~
    (__ \ "stringency").read[Stringency.Value] ~
    (__ \ "matchingAlleles").read[AlleleMatchResult] ~
    (__ \ "totalAlleles").read[Int] ~
    (__ \ "categoryId").read[AlphanumericId] ~
    (__ \ "owenerStatus").read[MatchStatus.Value] ~  // note: legacy typo preserved
    (__ \ "otherStatus").read[MatchStatus.Value] ~
    (__ \ "matchGlobalStatus").read[MatchGlobalStatus.Value] ~
    (__ \ "sharedAllelePonderation").read[Double] ~
    (__ \ "contributors").read[Int] ~
    (__ \ "isReference").read[Boolean].orElse(Reads.pure(true)) ~
    (__ \ "algorithm").read[Algorithm.Value].orElse(Reads.pure(Algorithm.ENFSI)) ~
    (__ \ "type").read[Int] ~
    (__ \ "allelesRanges").readNullable[NewMatchingResult.AlleleMatchRange] ~
    (__ \ "lr").read[Double] ~
    (__ \ "mismatches").read[Int] ~
    (__ \ "isInterconnectionMatch").read[Boolean]
  )(MatchingResult.apply)

  implicit val matchingResultWrites: Writes[MatchingResult] = (
    (__ \ "oid").write[String] ~
    (__ \ "globalCode").write[SampleCode] ~
    (__ \ "internalSampleCode").write[String] ~
    (__ \ "stringency").write[Stringency.Value] ~
    (__ \ "matchingAlleles").write[AlleleMatchResult] ~
    (__ \ "totalAlleles").write[Int] ~
    (__ \ "categoryId").write[AlphanumericId] ~
    (__ \ "ownerStatus").write[MatchStatus.Value] ~
    (__ \ "otherStatus").write[MatchStatus.Value] ~
    (__ \ "matchGlobalStatus").write[MatchGlobalStatus.Value] ~
    (__ \ "sharedAllelePonderation").write[Double] ~
    (__ \ "contributors").write[Int] ~
    (__ \ "isReference").write[Boolean] ~
    (__ \ "algorithm").write[Algorithm.Value] ~
    (__ \ "type").write[Int] ~
    (__ \ "allelesRanges").writeNullable[NewMatchingResult.AlleleMatchRange] ~
    (__ \ "lr").write[Double] ~
    (__ \ "mismatches").write[Int] ~
    (__ \ "isInterconnectionMatch").write[Boolean]
  )((r: MatchingResult) => (
    r.oid, r.globalCode, r.internalSampleCode, r.stringency, r.matchingAlleles, r.totalAlleles,
    r.categoryId, r.ownerStatus, r.otherStatus, r.matchGlobalStatus, r.sharedAllelePonderation,
    r.contributors, r.isReference, r.algorithm, r.`type`, r.allelesRanges, r.lr, r.mismatches,
    r.isInterconnectionMatch))

  implicit val format: Format[MatchingResult] = Format(matchingResultReads, matchingResultWrites)

// ---------------------------------------------------------------------------
// MatchingResults — container for the list returned to the UI
// ---------------------------------------------------------------------------

case class MatchingResults(
  matchingId: String,
  searchedProfileGlobalCode: SampleCode,
  userId: String,
  results: List[MatchingResult]
)

object MatchingResults:
  implicit val matchingResultsReads: Reads[MatchingResults] = (
    (__ \ "matchingId").read[String] ~
    (__ \ "searchedProfileGlobalCode").read[SampleCode] ~
    (__ \ "userId").read[String] ~
    (__ \ "results").read[List[MatchingResult]]
  )(MatchingResults.apply)

  implicit val matchingResultsWrites: Writes[MatchingResults] = (
    (__ \ "matchingId").write[String] ~
    (__ \ "searchedProfileGlobalCode").write[SampleCode] ~
    (__ \ "userId").write[String] ~
    (__ \ "results").write[List[MatchingResult]]
  )((r: MatchingResults) => (r.matchingId, r.searchedProfileGlobalCode, r.userId, r.results))

  implicit val format: Format[MatchingResults] = Format(matchingResultsReads, matchingResultsWrites)
