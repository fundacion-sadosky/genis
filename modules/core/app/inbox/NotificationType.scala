package inbox

import play.api.libs.json.*

enum NotificationType:
  case matching, bulkImport, userNotification, profileData, profileDataAssociation,
       pedigreeMatching, pedigreeLR, inferiorInstancePending, hitMatch, discardMatch,
       deleteProfile, collapsing, pedigreeConsistency, profileUploaded, aprovedProfile,
       rejectedProfile, deletedProfileInSuperiorInstance, deletedProfileInInferiorInstance,
       profileChangeCategory

object NotificationType:
  given format: Format[NotificationType] = Format(
    Reads[NotificationType] { js =>
      js.validate[String].flatMap { s =>
        scala.util.Try(NotificationType.valueOf(s))
          .fold(_ => JsError(s"Tipo de notificación desconocido: $s"), JsSuccess(_))
      }
    },
    Writes[NotificationType](t => JsString(t.toString))
  )