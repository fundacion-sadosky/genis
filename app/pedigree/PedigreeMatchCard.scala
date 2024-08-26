package pedigree

import java.util.Date

import matching.{MatchGlobalStatus, MatchStatus}
import matching.MatchGlobalStatus.MatchGlobalStatus
import play.api.libs.json.Json
import types.{AlphanumericId, MongoId, SampleCode}

case class PedigreeMatchCard (
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

object PedigreeMatchCard {
  implicit val pedigreeMatchCardFormat = Json.format[PedigreeMatchCard]
}



case class MatchCardMejorLrPed(
  id: String,
  internalCode: String,
  sampleCode: String,
  categoryId: Option[String] = None,
  lr: Double,
  estado: MatchStatus.Value
)

object MatchCardMejorLrPed {
  implicit val matchCardMejorLrFormat = Json.format[MatchCardMejorLrPed]
}

case class MatchCardPedigree(
                             matchCard: PedigreeMatchCard,
                             matchCardMejorLrPed: MatchCardMejorLrPed
                           )

object MatchCardPedigree {
  implicit val matchCardPedFormat = Json.format[MatchCardPedigree]
}

case class MatchCardPed(
                                internalCode: String,
                                sampleCode: String,
                                categoryId: String,
                                lr: Double,
                                estado: MatchStatus.Value,
                                assigne : String,
                                lastMatchDate: Date,
                                id: MongoId,
                                courtCaseId: String,
                                courtCaseName: String,
                                unknown: NodeAlias,
                                mtProfile : String = "",
                                matchingId: String = "",
                                idPedigree: String = ""
                              )

object MatchCardPed{
  implicit val matchCardPedFormat = Json.format[MatchCardPed]
}

case class MatchCardPedigrees(
                               matchCard: PedigreeMatchCard,
                               matchCardPed: List[MatchCardPed]
                             )

object MatchCardPedigrees {
  implicit val matchCardPedFormat = Json.format[MatchCardPedigrees]
}