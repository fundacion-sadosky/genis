package fixtures

import profiledata.{ProfileData, ProfileDataFull, ProfileDataWithBatch}
import types.{AlphanumericId, SampleCode}

object SearchFixtures:

  val catId = AlphanumericId("SOSPECHOSO")

  val profileData1: ProfileData = ProfileData(
    category              = catId,
    globalCode            = SampleCode("AR-C-SHDG-1100"),
    attorney              = Some("attorney"),
    bioMaterialType       = Some("SANGRE"),
    court                 = Some("court desc"),
    crimeInvolved         = Some("ASESINATO"),
    crimeType             = Some("PERSONAS"),
    criminalCase          = Some("case number"),
    internalSampleCode    = "internalSampleCode",
    assignee              = "assignee",
    laboratory            = "SHDG",
    deleted               = false,
    responsibleGeneticist = Some("responsibleGeneticist"),
    profileExpirationDate = None,
    sampleDate            = None,
    sampleEntryDate       = None,
    isExternal            = false
  )

  val profileData2: ProfileData = profileData1.copy(globalCode = SampleCode("AR-C-SHDG-1101"))

  /** Construye un ProfileDataFull a partir de un ProfileData, igual que hace FullTextSearchServiceImpl */
  def toFull(pd: ProfileData, readOnly: Boolean = false): ProfileDataFull =
    ProfileDataFull(
      category              = pd.category,
      globalCode            = pd.globalCode,
      attorney              = pd.attorney,
      bioMaterialType       = pd.bioMaterialType,
      court                 = pd.court,
      crimeInvolved         = pd.crimeInvolved,
      crimeType             = pd.crimeType,
      criminalCase          = pd.criminalCase,
      internalSampleCode    = pd.internalSampleCode,
      assignee              = pd.assignee,
      laboratory            = pd.laboratory,
      deleted               = pd.deleted,
      deletedMotive         = None,
      responsibleGeneticist = pd.responsibleGeneticist,
      profileExpirationDate = pd.profileExpirationDate,
      sampleDate            = pd.sampleDate,
      sampleEntryDate       = pd.sampleEntryDate,
      dataFiliation         = None,
      readOnly              = readOnly,
      isExternal            = pd.isExternal
    )

  val profileDataFull1: ProfileDataFull = toFull(profileData1)
  val profileDataFull2: ProfileDataFull = toFull(profileData2)

  val profileDataWithBatch: ProfileDataWithBatch = ProfileDataWithBatch(
    category              = catId,
    globalCode            = SampleCode("AR-C-SHDG-1100"),
    attorney              = Some("attorney"),
    bioMaterialType       = Some("SANGRE"),
    court                 = Some("court desc"),
    crimeInvolved         = Some("ASESINATO"),
    crimeType             = Some("PERSONAS"),
    criminalCase          = Some("case number"),
    internalSampleCode    = "internalSampleCode",
    assignee              = "assignee",
    laboratory            = "SHDG",
    deleted               = false,
    deletedMotive         = None,
    responsibleGeneticist = Some("responsibleGeneticist"),
    profileExpirationDate = None,
    sampleDate            = None,
    sampleEntryDate       = None,
    dataFiliation         = None,
    label                 = Some("batch-label")
  )
