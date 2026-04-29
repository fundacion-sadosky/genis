package fixtures

import java.util.Date

import audit.{Key, OperationLogEntryAttemp, OperationLogLotView, SignedOperationLogEntry}
import types.TotpToken

object AuditFixtures:

  val zeroKey32: Key = Key(Seq.fill(32)(0.toByte))

  def makeAttempt(
      userId: String = "user1",
      otp: Option[TotpToken] = Some(TotpToken("123456")),
      timestamp: Date = new Date(),
      method: String = "GET",
      path: String = "/api/v2/profiles",
      action: String = "controllers.ProfilesController.findByCode()",
      buildNo: String = "develop",
      result: Option[String] = None,
      status: Int = 200
  ): OperationLogEntryAttemp =
    OperationLogEntryAttemp(userId, otp, timestamp, method, path, action, buildNo, result, status)

  def makeSigned(
      index: Long = 0L,
      userId: String = "user1",
      otp: Option[TotpToken] = None,
      timestamp: Date = new Date(),
      method: String = "GET",
      path: String = "/api/v2/profiles",
      action: String = "controllers.ProfilesController.findByCode()",
      buildNo: String = "develop",
      result: Option[String] = None,
      status: Int = 200,
      lotId: Long = 1L,
      signature: Key = zeroKey32,
      description: String = "Ver perfil"
  ): SignedOperationLogEntry =
    SignedOperationLogEntry(
      index, userId, otp, timestamp, method, path, action, buildNo,
      result, status, lotId, signature, description
    )
