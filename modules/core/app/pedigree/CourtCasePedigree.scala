package pedigree

import play.api.libs.json.*
import profile.GenotypificationByType
import profile.GenotypificationByType.GenotypificationByType

// ---------------------------------------------------------------------------
// CourtCasePedigree — profile associated to a court case, with its genotypification.
// ---------------------------------------------------------------------------

case class CourtCasePedigree(
  globalCode: String,
  internalCode: String,
  genotypification: GenotypificationByType,
  idBatch: Option[Long] = None,
  batchLabel: Option[String] = None,
  statusProfile: String,
  groupedBy: Option[String] = None
)

object CourtCasePedigree:
  // Re-use the GenotypificationByType implicit formats from profile package
  given Reads[GenotypificationByType]   = GenotypificationByType.mapReads
  given Writes[GenotypificationByType]  = GenotypificationByType.mapWrites
  implicit val format: OFormat[CourtCasePedigree] = Json.format[CourtCasePedigree]

// ---------------------------------------------------------------------------
// PedigreeConsistency — stores inconsistent loci for a profile in a pedigree.
// ---------------------------------------------------------------------------

case class PedigreeConsistency(
  globalCode: String,
  internalCode: String,
  locus: List[String]
)

object PedigreeConsistency:
  implicit val format: OFormat[PedigreeConsistency] = Json.format[PedigreeConsistency]

case class PedigreeConsistencyCheck(
  globalCode: String,
  locus: List[String]
)

object PedigreeConsistencyCheck:
  implicit val format: OFormat[PedigreeConsistencyCheck] = Json.format[PedigreeConsistencyCheck]
