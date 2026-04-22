package pedigree

import java.util.Date
import matching.{MatchGlobalStatus, MatchStatus}
import matching.MongoId
import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import types.{MongoDate, SampleCode}

/**
 * Aggregated match card — result of a grouped aggregation over pedigreeMatches.
 * _id is Left(pedigreeId) when grouped by pedigree, Right(globalCode) when by profile.
 */
case class PedigreeMatch(
  _id: Either[Long, String],
  lastMatchDate: MongoDate,
  count: Int,
  assignee: String
)

object PedigreeMatch:
  implicit val reads: Reads[PedigreeMatch] = new Reads[PedigreeMatch]:
    def reads(json: JsValue): JsResult[PedigreeMatch] =
      val long: Reads[PedigreeMatch] = (
        (__ \ "_id").read[Long] ~
        (__ \ "lastMatchDate").read[MongoDate] ~
        (__ \ "count").read[Int] ~
        (__ \ "assignee").read[String]
      )((id, date, count, assignee) => PedigreeMatch(Left(id), date, count, assignee))

      val str: Reads[PedigreeMatch] = (
        (__ \ "_id").read[String] ~
        (__ \ "lastMatchDate").read[MongoDate] ~
        (__ \ "count").read[Int] ~
        (__ \ "assignee").read[String]
      )((id, date, count, assignee) => PedigreeMatch(Right(id), date, count, assignee))

      long.reads(json) orElse str.reads(json)

case class PedigreeMatchCard(
  title: String,
  id: String,
  assignee: String,
  lastMatchDate: Date,
  count: Int,
  groupBy: String,
  courtCaseId: String,
  courtCaseName: String = "",
  category: Option[String] = None,
  hit: Int = 0,
  pending: Int = 0,
  discarded: Int = 0
)

object PedigreeMatchCard:
  implicit val pedigreeMatchCardFormat: play.api.libs.json.OFormat[PedigreeMatchCard] = Json.format[PedigreeMatchCard]

case class MatchCardMejorLrPed(
  id: String,
  internalCode: String,
  sampleCode: String,
  categoryId: Option[String] = None,
  lr: Double,
  estado: MatchStatus.Value
)

object MatchCardMejorLrPed:
  implicit val matchCardMejorLrFormat: play.api.libs.json.OFormat[MatchCardMejorLrPed] = Json.format[MatchCardMejorLrPed]

case class MatchCardPedigree(
  matchCard: PedigreeMatchCard,
  matchCardMejorLrPed: MatchCardMejorLrPed
)

object MatchCardPedigree:
  implicit val matchCardPedFormat: play.api.libs.json.OFormat[MatchCardPedigree] = Json.format[MatchCardPedigree]

case class MatchCardPed(
  internalCode: String,
  sampleCode: String,
  categoryId: String,
  lr: Double,
  estado: MatchStatus.Value,
  assigne: String,
  lastMatchDate: Date,
  id: MongoId,
  courtCaseId: String,
  courtCaseName: String,
  unknown: NodeAlias,
  mtProfile: String  = "",
  matchingId: String = "",
  idPedigree: String = ""
)

object MatchCardPed:
  implicit val matchCardPedFormat: play.api.libs.json.OFormat[MatchCardPed] = Json.format[MatchCardPed]

case class MatchCardPedigrees(
  matchCard: PedigreeMatchCard,
  matchCardPed: List[MatchCardPed]
)

object MatchCardPedigrees:
  implicit val matchCardPedFormat: play.api.libs.json.OFormat[MatchCardPedigrees] = Json.format[MatchCardPedigrees]
