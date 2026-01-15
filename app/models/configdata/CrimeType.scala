package models.configdata

import play.api.libs.json.{Json, Format, Reads, Writes}

/**
 * Representa un crimen específico dentro de un tipo de crimen.
 * 
 * @param id Identificador único del crimen
 * @param name Nombre del crimen
 * @param description Descripción opcional del crimen
 */
case class Crime(
  id: String,
  name: String,
  description: Option[String]
)

object Crime {
  implicit val format: Format[Crime] = Json.format[Crime]
}

/**
 * Representa un tipo de crimen que agrupa varios crímenes relacionados.
 * 
 * @param id Identificador único del tipo de crimen
 * @param name Nombre del tipo de crimen
 * @param description Descripción opcional
 * @param crimes Lista de crímenes asociados a este tipo
 */
case class CrimeType(
  id: String,
  name: String,
  description: Option[String],
  crimes: Seq[Crime] = Seq.empty
)

object CrimeType {
  implicit val format: Format[CrimeType] = Json.format[CrimeType]
}

/**
 * Row para la tabla crime_type (sin relaciones anidadas)
 */
case class CrimeTypeRow(
  id: String,
  name: String,
  description: Option[String]
)

/**
 * Row para la tabla crime_involved
 */
case class CrimeRow(
  id: String,
  crimeTypeId: String,
  name: String,
  description: Option[String]
)
