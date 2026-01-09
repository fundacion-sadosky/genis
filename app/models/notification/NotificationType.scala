package models.notification

object NotificationType extends Enumeration {
  type NotificationType = Value
  
  val matching = Value("matching")
  val bulkImport = Value("bulkImport")
  val userNotification = Value("userNotification")
  val profileData = Value("profileData")
  val profileDataAssociation = Value("profileDataAssociation")
  val pedigreeMatching = Value("pedigreeMatching")
  val pedigreeLR = Value("pedigreeLR")
  val inferiorInstancePending = Value("inferiorInstancePending")
  val hitMatch = Value("hitMatch")
  val discardMatch = Value("discardMatch")
  val deleteProfile = Value("deleteProfile")
  val collapsing = Value("collapsing")
  val pedigreeConsistency = Value("pedigreeConsistency")
  val profileUploaded = Value("profileUploaded")
  val approvedProfile = Value("approvedProfile")
  val rejectedProfile = Value("rejectedProfile")
  val deletedProfileInSuperiorInstance = Value("deletedProfileInSuperiorInstance")
  val deletedProfileInInferiorInstance = Value("deletedProfileInInferiorInstance")
  val profileChangeCategory = Value("profileChangeCategory")

  implicit val reads: play.api.libs.json.Reads[NotificationType.Value] =
    play.api.libs.json.Reads.enumNameReads(NotificationType)
  implicit val writes: play.api.libs.json.Writes[NotificationType.Value] =
    play.api.libs.json.Writes.enumNameWrites
}
