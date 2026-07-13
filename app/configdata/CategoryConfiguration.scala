package configdata

import play.api.libs.functional.syntax._
import play.api.libs.json._
import types.AlphanumericId

case class CategoryConfiguration(
    collectionUri: String = "",
    draftUri: String = "",
    minLocusPerProfile: String = "K",
    maxOverageDeviatedLoci: String = "0",
    maxAllelesPerLocus: Int = 6,
    multiallelic: Boolean = false
)

object CategoryConfiguration {
  // Reads manual (en vez de Json.format, que en esta version de Play no
  // respeta los valores por defecto del case class): "multiallelic" se
  // agrego despues de que ya existian registros de trazabilidad guardados
  // sin ese campo, y Json.format exige que todo campo este presente en el
  // JSON aunque el case class tenga default. Sin el .orElse, parsear un
  // Trace/CategoryConfiguration viejo (sin "multiallelic") tira
  // error.path.missing en vez de usar el default de false.
  private val reads: Reads[CategoryConfiguration] = (
    (__ \ "collectionUri").read[String].orElse(Reads.pure("")) and
    (__ \ "draftUri").read[String].orElse(Reads.pure("")) and
    (__ \ "minLocusPerProfile").read[String].orElse(Reads.pure("K")) and
    (__ \ "maxOverageDeviatedLoci").read[String].orElse(Reads.pure("0")) and
    (__ \ "maxAllelesPerLocus").read[Int].orElse(Reads.pure(6)) and
    (__ \ "multiallelic").read[Boolean].orElse(Reads.pure(false))
  )(CategoryConfiguration.apply _)

  private val writes: Writes[CategoryConfiguration] = Json.writes[CategoryConfiguration]

  implicit val format: Format[CategoryConfiguration] = Format(reads, writes)
}

case class CategoryAssociation(
    `type`: Int,
    categoryRelated: AlphanumericId,
    mismatches: Int
)

object CategoryAssociation {
  implicit val format = Json.format[CategoryAssociation]
}