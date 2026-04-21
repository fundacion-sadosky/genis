package fixtures

import configdata.{BioMaterialType, CrimeType}
import matching.{MongoId, MatchingResults}
import models.Tables.{ProfileReceivedRow, ProfileSentRow, ProfileUploadedRow}
import profiledata.*
import scenarios.{CalculationScenario, Hypothesis, Scenario, ScenarioStatus}
import types.{AlphanumericId, Laboratory, MongoDate, SampleCode, StatOption}

import java.util.Date

object ProfileDataFixtures:

  val sampleCode: SampleCode  = SampleCode("AR-C-SHDG-1")
  val sampleCode2: SampleCode = SampleCode("AR-C-SHDG-2")
  val categoryId: AlphanumericId = AlphanumericId("SOSPECHOSO")

  val dataFiliation: DataFiliation = DataFiliation(
    fullName = Some("Juan Pérez"),
    nickname = None,
    birthday = None,
    birthPlace = Some("Buenos Aires"),
    nationality = Some("AR"),
    identification = Some("12345678"),
    identificationIssuingAuthority = Some("RENAPER"),
    address = Some("Av. Corrientes 123"),
    inprints = Nil,
    pictures = Nil,
    signatures = Nil
  )

  val profileData: ProfileData = ProfileData(
    category = categoryId,
    globalCode = sampleCode,
    attorney = Some("attorney"),
    bioMaterialType = Some("SANGRE"),
    court = Some("Juzgado 1"),
    crimeInvolved = Some("ASESINATO"),
    crimeType = Some("PERSONAS"),
    criminalCase = Some("causa-123"),
    internalSampleCode = "INT-001",
    assignee = "analyst1",
    laboratory = "SHDG",
    deleted = false,
    deletedMotive = None,
    responsibleGeneticist = Some("Geneticist Name"),
    profileExpirationDate = None,
    sampleDate = None,
    sampleEntryDate = None,
    dataFiliation = Some(dataFiliation),
    isExternal = false
  )

  val profileData2: ProfileData = profileData.copy(globalCode = sampleCode2, internalSampleCode = "INT-002")

  val dataFiliationAttempt: DataFiliationAttempt = DataFiliationAttempt(
    fullName = Some("Juan Pérez"),
    nickname = None,
    birthday = None,
    birthPlace = None,
    nationality = Some("AR"),
    identification = Some("12345678"),
    identificationIssuingAuthority = None,
    address = None,
    inprint = "uuid-inprint-001",
    picture = "uuid-picture-001",
    signature = "uuid-signature-001"
  )

  val profileDataAttempt: ProfileDataAttempt = ProfileDataAttempt(
    category = categoryId,
    attorney = Some("attorney"),
    bioMaterialType = Some("SANGRE"),
    court = Some("Juzgado 1"),
    crimeInvolved = Some("ASESINATO"),
    crimeType = Some("PERSONAS"),
    criminalCase = Some("causa-123"),
    internalSampleCode = "INT-001",
    assignee = "analyst1",
    laboratory = None,
    responsibleGeneticist = Some(1L),
    profileExpirationDate = None,
    sampleDate = None,
    sampleEntryDate = None,
    dataFiliation = Some(dataFiliationAttempt)
  )

  val deletedMotive: DeletedMotive = DeletedMotive(
    solicitor = "solicitor1",
    motive = "investigacion concluida",
    selectedMotive = 1L
  )

  val bioMaterialTypes: Seq[BioMaterialType] =
    Seq(BioMaterialType(AlphanumericId("SANGRE"), "sangre", None))

  val crimeTypes: Map[String, CrimeType] =
    Map("PERSONAS" -> CrimeType("PERSONAS", "contra las personas", None, Nil))

  val laboratory: Laboratory = Laboratory(
    name = "Servicio de Huellas Digitales Genéticos",
    code = "SHDG",
    country = "AR",
    province = "C",
    address = "Calle 123",
    telephone = "5411-12345",
    contactEmail = "lab@example.com",
    dropIn = 0.0,
    dropOut = 0.0
  )

  val testScenario: Scenario = Scenario(
    _id = MongoId("507f1f77bcf86cd799439011"),
    name = "Test Scenario",
    state = ScenarioStatus.Pending,
    geneticist = "geneticist1",
    calculationScenario = CalculationScenario(
      sample = sampleCode,
      prosecutor = Hypothesis(Nil, Nil, 0, 0.0),
      defense = Hypothesis(Nil, Nil, 0, 0.0),
      stats = StatOption("table", "model", 0.0, 0.0, None)
    ),
    date = MongoDate(new Date()),
    isRestricted = false,
    result = None,
    description = None
  )

  val matchingResults: MatchingResults =
    MatchingResults("match-id-1", sampleCode, "analyst1", Nil)
