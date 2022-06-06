package matching

import java.util.Date

import play.api.libs.json.Json
import types.{AlphanumericId, SampleCode}

case class MatchCard(
  globalCode: SampleCode,
  pending: Int,
  hit: Int,
  discarded: Int,
  conflict: Int,
  contributors: Int,
  internalSampleCode: String,
  categoryId: AlphanumericId,
  lastMatchDate: Date,
  laboratoryCode: String,
  assignee: String,
  tipo: Option[Boolean] = None
)

object MatchCard {
  implicit val matchCardFormat = Json.format[MatchCard]
}

case class MatchCardMejorLr(
                      internalSampleCode: String,
                      categoryId: AlphanumericId,
                      totalAlleles: Int,
                      ownerStatus: MatchStatus.Value,
                      otherStatus: MatchStatus.Value,
                      sharedAllelePonderation: Double,
                      mismatches: Double,
                      lr: Double,
                      globalCode: SampleCode,
                      typeAnalisis : Int
                           )

object MatchCardMejorLr {
  implicit val matchCardMejorFormat = Json.format[MatchCardMejorLr]
}

case class MatchCardForense(
                           matchCard: MatchCard,
                           matchCardMejorLr: MatchCardMejorLr
                           )

object MatchCardForense {
  implicit val matchCardForenseFormat = Json.format[MatchCardForense]
}


