package inbox

import util.PlayEnumUtils

object NotificationType extends Enumeration {
  type NotificationType = Value
  val matching, bulkImport, userNotification, profileData, profileDataAssociation, pedigreeMatching, pedigreeLR, inferiorInstancePending,hitMatch,discardMatch,deleteProfile,collapsing,pedigreeConsistency, profileUploaded, aprovedProfile, rejectedProfile, deletedProfileInSuperiorInstance, deletedProfileInInferiorInstance = Value
  
  implicit val enumTypeFormat = PlayEnumUtils.enumFormat(NotificationType)
}
