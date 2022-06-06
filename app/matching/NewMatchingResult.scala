package matching

import play.api.libs.json.{Format, Writes, _}
import profile.Profile
import types.AlphanumericId
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
//import matching.Stringency.Stringency
import matching.Stringency._

case class AleleRange(min:BigDecimal,max:BigDecimal)
object AleleRange {
  implicit val motiveFormat: Format[AleleRange] = Json.format
}
case class NewMatchingResult(
  stringency: Stringency,
  matchingAlleles: NewMatchingResult.AlleleMatchResult,
  totalAlleles: Int,
  categoryId: AlphanumericId,
  leftPonderation: Double,
  rightPonderation: Double,
  algorithm: Algorithm.Value,
  allelesRanges: Option[NewMatchingResult.AlleleMatchRange] = None)

object NewMatchingResult {
  type AlleleMatchResult = Map[Profile.Marker, Stringency.Value]
  type AlleleMatchRange = Map[Profile.Marker,AleleRange]
  implicit val matchResultReads: Reads[NewMatchingResult] = (
    (__ \ "stringency").read[Stringency] ~
    (__ \ "matchingAlleles").read[NewMatchingResult.AlleleMatchResult] ~
    (__ \ "totalAlleles").read[Int] ~
    (__ \ "categoryId").read[AlphanumericId] ~
    (__ \ "leftPonderation").read[Double] ~
    (__ \ "rightPonderation").read[Double] ~
      // or else because it was added later
    (__ \ "algorithm").read[Algorithm.Value].orElse(Reads.pure(Algorithm.ENFSI)) ~
      (__ \ "allelesRanges").readNullable[NewMatchingResult.AlleleMatchRange])(NewMatchingResult.apply _)

  implicit val matchResultWrites: Writes[NewMatchingResult] = (
    (__ \ "stringency").write[Stringency] ~
    (__ \ "matchingAlleles").write[NewMatchingResult.AlleleMatchResult] ~
    (__ \ "totalAlleles").write[Int] ~
    (__ \ "categoryId").write[AlphanumericId] ~
    (__ \ "leftPonderation").write[Double] ~
    (__ \ "rightPonderation").write[Double] ~
    (__ \ "algorithm").write[Algorithm.Value] ~
    (__ \ "allelesRanges").writeNullable[NewMatchingResult.AlleleMatchRange])((matchResult: NewMatchingResult) => (
      matchResult.stringency,
      matchResult.matchingAlleles,
      matchResult.totalAlleles,
      matchResult.categoryId,
      matchResult.leftPonderation,
      matchResult.rightPonderation,
      matchResult.algorithm,
      matchResult.allelesRanges))

  implicit val matchResultFormat: Format[NewMatchingResult] = Format(matchResultReads, matchResultWrites)

}
