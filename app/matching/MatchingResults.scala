package matching

import play.api.libs.json._
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import types.SampleCode
import types.AlphanumericId
import profile._
import matching.Stringency.Stringency

case class MatchingResult(oid: String,
                          globalCode: SampleCode,
                          internalSampleCode: String,
                          stringency: Stringency,
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
                          mismatches : Int = 0,
                          isInterconnectionMatch: Boolean = false)

case class MatchingResults(matchingId: String, searchedProfileGlobalCode: SampleCode, userId: String, results: List[MatchingResult])

object MatchingResult {

  implicit val doubleReads: Reads[Double] = new Reads[Double] {
    def reads(jv: JsValue): JsResult[Double] = JsSuccess(jv.toString().toDouble)
  }

  implicit val doubleWrites: Writes[Double] = new Writes[Double] {
    def writes(l: Double): JsValue = Json.toJson(l.toString)
  }

  implicit val formatMatchingDouble: Format[Double] = Format(doubleReads,doubleWrites)

  type AlleleMatchResult = Map[Profile.Marker, Stringency.Value]

  implicit val matchingResultReads: Reads[MatchingResult] = (
    (__ \ "oid").read[String] ~
    (__ \ "globalCode").read[SampleCode] ~
    (__ \ "internalSampleCode").read[String] ~
    (__ \ "stringency").read[Stringency] ~
    (__ \ "matchingAlleles").read[AlleleMatchResult] ~
    (__ \ "totalAlleles").read[Int] ~
    (__ \ "categoryId").read[AlphanumericId] ~
    (__ \ "owenerStatus").read[MatchStatus.Value] ~
    (__ \ "otherStatus").read[MatchStatus.Value] ~
    (__ \ "matchGlobalStatus").read[MatchGlobalStatus.Value] ~
    (__ \ "sharedAllelePonderation").read[Double] ~
    (__ \ "contributors").read[Int] ~
      // or else because they were added later
    (__ \ "isReference").read[Boolean].orElse(Reads.pure(true)) ~
    (__ \ "algorithm").read[Algorithm.Value].orElse(Reads.pure(Algorithm.ENFSI)) ~
    (__ \ "type").read[Int] ~
    (__ \ "allelesRanges").readNullable[NewMatchingResult.AlleleMatchRange] ~
    (__ \ "lr").read[Double] ~
    (__ \ "mismatches").read[Int] ~
    (__ \ "isInterconnectionMatch").read[Boolean] )(MatchingResult.apply _)

  implicit val matchingResultWrites: Writes[MatchingResult] = (
    (__ \ "oid").write[String] ~
    (__ \ "globalCode").write[SampleCode] ~
    (__ \ "internalSampleCode").write[String] ~
    (__ \ "stringency").write[Stringency] ~
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
    (__ \ "lr").write[Double]~
    (__ \ "mismatches").write[Int] ~
    (__ \ "isInterconnectionMatch").write[Boolean] )((matchingResul: MatchingResult) => (
      matchingResul.oid,
      matchingResul.globalCode,
      matchingResul.internalSampleCode,
      matchingResul.stringency,
      matchingResul.matchingAlleles,
      matchingResul.totalAlleles,
      matchingResul.categoryId,
      matchingResul.ownerStatus,
      matchingResul.otherStatus,
      matchingResul.matchGlobalStatus,
      matchingResul.sharedAllelePonderation,
      matchingResul.contributors,
      matchingResul.isReference,
      matchingResul.algorithm,
      matchingResul.`type`,
      matchingResul.allelesRanges,
      matchingResul.lr,
      matchingResul.mismatches,
      matchingResul.isInterconnectionMatch))

  implicit val matchingResultFormat: Format[MatchingResult] = Format(matchingResultReads, matchingResultWrites)
}

object MatchingResults {
  implicit val matchingResultsReads: Reads[MatchingResults] = (
    (__ \ "matchingId").read[String] ~
    (__ \ "searchedProfileGlobalCode").read[SampleCode] ~
    (__ \ "userId").read[String] ~
    (__ \ "results").read[List[MatchingResult]])(MatchingResults.apply _)

  implicit val matchingResultsWrites: Writes[MatchingResults] = (
    (__ \ "matchingId").write[String] ~
    (__ \ "searchedProfileGlobalCode").write[SampleCode] ~
    (__ \ "userId").write[String] ~
    (__ \ "results").write[List[MatchingResult]])((matchingResults: MatchingResults) => (
      matchingResults.matchingId,
      matchingResults.searchedProfileGlobalCode,
      matchingResults.userId,
      matchingResults.results))

  implicit val matchingResultsFormat: Format[MatchingResults] = Format(matchingResultsReads, matchingResultsWrites)
}
