package profiledata

import play.api.libs.json.{Json, OFormat}
import types.SampleCode

// Used by PedigreeService (getMatchByProfile). Migrated alongside pedigree.
case class ProfileDataViewCoincidencia(
  globalCode: SampleCode,
  category: String,
  internalSampleCode: String,
  assignee: String,
  lastMatch: java.util.Date,
  hit: Int,
  pending: Int,
  descarte: Int
)

object ProfileDataViewCoincidencia:
  implicit val format: OFormat[ProfileDataViewCoincidencia] = Json.format[ProfileDataViewCoincidencia]
