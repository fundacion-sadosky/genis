package fixtures

import java.util.Date

import matching.{MatchGlobalStatus, MatchStatus, MongoId, NewMatchingResult}
import pedigree.*
import pedigree.PedigreeStatus.*
import scenarios.ScenarioStatus
import types.{AlphanumericId, MongoDate, SampleCode}

object PedigreeFixtures {

  val assignee = "user1"
  val superUser = "admin"

  val courtCaseId: Long = 100L
  val pedigreeId: Long  = 1L

  val mongoId: MongoId = MongoId("5e8f8f8f8f8f8f8f8f8f8f00")

  def pedigreeGenogram(
    id: Long = pedigreeId,
    status: PedigreeStatus.Value = PedigreeStatus.UnderConstruction
  ): PedigreeGenogram =
    PedigreeGenogram(
      _id = id,
      assignee = assignee,
      genogram = Seq.empty,
      status = status,
      frequencyTable = None,
      processed = false,
      boundary = 0.5,
      executeScreeningMitochondrial = false,
      numberOfMismatches = None,
      caseType = "MPI",
      mutationModelId = None,
      idCourtCase = courtCaseId
    )

  def pedigreeMetaData(
    id: Long = pedigreeId,
    status: PedigreeStatus.Value = PedigreeStatus.UnderConstruction,
    user: String = assignee
  ): PedigreeMetaData =
    PedigreeMetaData(
      id = id,
      courtCaseId = courtCaseId,
      name = "Test Pedigree",
      courtCaseName = "Test Court Case",
      assignee = user,
      creationDate = Some(new Date()),
      status = status
    )

  def pedigreeDataCreation(
    id: Long = pedigreeId,
    status: PedigreeStatus.Value = PedigreeStatus.UnderConstruction,
    user: String = assignee
  ): PedigreeDataCreation =
    PedigreeDataCreation(
      pedigreeMetaData = pedigreeMetaData(id, status, user),
      pedigreeGenogram = Some(pedigreeGenogram(id, status))
    )

  def courtCase(
    id: Long = courtCaseId,
    status: PedigreeStatus.Value = PedigreeStatus.Open,
    user: String = assignee
  ): CourtCase =
    CourtCase(
      id = id,
      internalSampleCode = "CC-001",
      attorney = Some("attorney1"),
      court = Some("court1"),
      assignee = user,
      crimeInvolved = None,
      crimeType = None,
      criminalCase = None,
      status = status,
      personData = Nil,
      caseType = "MPI"
    )

  def pedigreeScenario(
    id: MongoId = mongoId,
    pedId: Long = pedigreeId,
    status: ScenarioStatus.Value = ScenarioStatus.Pending
  ): PedigreeScenario =
    PedigreeScenario(
      _id = id,
      pedigreeId = pedId,
      name = "Test Scenario",
      description = "Test Description",
      genogram = Seq.empty,
      status = status,
      frequencyTable = "ARGENTINA",
      isProcessing = false,
      lr = None,
      validationComment = None,
      matchId = None,
      mutationModelId = None
    )

  val now: MongoDate = MongoDate(new Date())

  def pedigreeInfo(
    pedId: Long = pedigreeId,
    user: String = assignee
  ): PedigreeInfo =
    PedigreeInfo(
      idPedigree = pedId,
      unknown = NodeAlias("PI"),
      assignee = user,
      status = MatchStatus.pending,
      caseType = "MPI",
      idCourtCase = courtCaseId
    )

  def pedigreeProfileInfo(
    pedId: Long = pedigreeId,
    user: String = assignee
  ): PedigreeProfileInfo =
    PedigreeProfileInfo(
      idPedigree = pedId,
      unknown = NodeAlias("PI"),
      globalCode = SampleCode("AR-B-HIBA-300"),
      assignee = user,
      status = MatchStatus.pending,
      caseType = "MPI",
      idCourtCase = courtCaseId
    )

  def matchingProfile(
    globalCode: String = "AR-B-HIBA-100",
    user: String = assignee
  ): matching.MatchingProfile =
    matching.MatchingProfile(
      globalCode = SampleCode(globalCode),
      assignee = user,
      status = MatchStatus.pending,
      comments = None,
      categoryId = AlphanumericId("IR")
    )

  def pedigreeDirectLinkMatch(
    id: MongoId = mongoId,
    pedId: Long = pedigreeId,
    profileCode: String = "AR-B-HIBA-100",
    user: String = assignee
  ): PedigreeDirectLinkMatch = {
    val matchingResult = NewMatchingResult(
      stringency = matching.Stringency.ModerateStringency,
      matchingAlleles = Map.empty,
      totalAlleles = 0,
      categoryId = AlphanumericId("IR"),
      leftPonderation = 1.0,
      rightPonderation = 1.0,
      algorithm = matching.Algorithm.ENFSI
    )
    PedigreeDirectLinkMatch(
      _id = id,
      matchingDate = now,
      `type` = 4,
      profile = matchingProfile(profileCode, user),
      pedigree = pedigreeProfileInfo(pedId, user),
      result = matchingResult
    )
  }

  def pedigreeMatch(pedId: Long = pedigreeId, user: String = assignee): PedigreeMatch =
    PedigreeMatch(
      _id = Left(pedId),
      lastMatchDate = now,
      count = 3,
      assignee = user
    )

  def profileMatch(code: String = "AR-B-HIBA-1-100-P", user: String = assignee): PedigreeMatch =
    PedigreeMatch(
      _id = Right(code),
      lastMatchDate = now,
      count = 2,
      assignee = user
    )
}
