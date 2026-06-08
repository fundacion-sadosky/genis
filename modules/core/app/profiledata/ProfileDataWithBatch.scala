package profiledata

import types.{AlphanumericId, SampleCode}

// Used by PedigreeService (node association). Migrated alongside pedigree.
case class ProfileDataWithBatch(
  globalCode: SampleCode,
  category: AlphanumericId,
  internalSampleCode: String,
  assignee: String,
  idBatch: Option[Long]
)
