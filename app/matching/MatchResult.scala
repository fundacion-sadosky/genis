package matching

import connections.SuperiorProfileInfo
import play.api.libs.json.{Format, Reads, Writes, __, _}
import types.{AlphanumericId, MongoDate, MongoId, SampleCode}
import matching.MatchStatus.MatchStatus
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps

case class MatchingProfile(globalCode: SampleCode, assignee: String, status: MatchStatus, comments: Option[String], categoryId: AlphanumericId = AlphanumericId("IR") )

object MatchingProfile {
  implicit val matchingProfileFormat = Json.format[MatchingProfile]
}

case class MatchResult(
                        _id: MongoId,
                        matchingDate: MongoDate,
                        `type`: Int,
                        leftProfile: MatchingProfile,
                        rightProfile: MatchingProfile,
                        result: NewMatchingResult,
                        n: Long,
                        superiorProfileInfo:Option[SuperiorProfileInfo] = None,
                        idCourtCase: Option[Long] = None,
                        lr: Double = 0.0,
                        //  lrRight: Double = 0.0,
                        mismatches:Int = 0)

object MatchResult {

  implicit val doubleReads: Reads[Double] = new Reads[Double] {
    def reads(jv: JsValue): JsResult[Double] = JsSuccess(jv.toString().toDouble)
  }

  implicit val doubleWrites: Writes[Double] = new Writes[Double] {
    def writes(l: Double): JsValue = Json.toJson(l.toString)
  }
  implicit val FormatMatchDouble: Format[Double] = Format(doubleReads, doubleWrites)

  implicit val matchResultReads: Reads[MatchResult] = (
    (__ \ "_id").read[MongoId] ~
    (__ \ "matchingDate").read[MongoDate] ~
    (__ \ "type").read[Int] ~
    (__ \ "leftProfile").read[MatchingProfile] ~
    (__ \ "rightProfile").read[MatchingProfile] ~
    (__ \ "result").read[NewMatchingResult] ~
    (__ \ "n").read[Long].orElse(Reads.pure(0l)) ~
    (__ \ "superiorProfileInfo").readNullable[SuperiorProfileInfo] ~
    Reads.pure(None) ~
    (__ \ "lr").read[Double] ~
//      (__ \ "lrRight").read[Double] ~
      (__ \ "mismatches").read[Int]
    )(MatchResult.apply _)

  implicit val matchResultWrites: OWrites[MatchResult] = (
    (__ \ "_id").write[MongoId] ~
    (__ \ "matchingDate").write[MongoDate] ~
    (__ \ "type").write[Int] ~
    (__ \ "leftProfile").write[MatchingProfile] ~
    (__ \ "rightProfile").write[MatchingProfile] ~
    (__ \ "result").write[NewMatchingResult] ~
    (__ \ "n").write[Long] ~
    (__ \ "superiorProfileInfo").writeNullable[SuperiorProfileInfo] ~
    (__ \ "idCourtCase").writeNullable[Long] ~
    (__ \ "lr").write[Double] ~
//      (__ \ "lrRight").write[Double] ~
      (__ \ "mismatches").write[Int]
    )((matchResult: MatchResult) => (
    matchResult._id,
    matchResult.matchingDate,
    matchResult.`type`,
    matchResult.leftProfile,
    matchResult.rightProfile,
    matchResult.result,
    matchResult.n,
    matchResult.superiorProfileInfo,
    matchResult.idCourtCase,
    matchResult.lr,
//    matchResult.lrRight,
    matchResult.mismatches
  ))

  implicit val matchResultFormat: OFormat[MatchResult] = OFormat(matchResultReads, matchResultWrites)
}

case class MixtureMatchResult(
  mixtureProfile: SampleCode,
  contributors: Seq[SampleCode],
  result: NewMatchingResult)

object MixtureMatchResult {
  implicit val mixtureMatchResultFormat = Json.format[MixtureMatchResult]
}