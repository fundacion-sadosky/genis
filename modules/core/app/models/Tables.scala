package models

import slick.jdbc.PostgresProfile.api._

final case class CrimeTypeRow(id: String, name: String, description: Option[String] = None)
object CrimeTypeRow {
  def tupled = (CrimeTypeRow.apply _).tupled
}

final case class CrimeInvolvedRow(id: String, crimeType: String, name: String, description: Option[String] = None)
object CrimeInvolvedRow {
  def tupled = (CrimeInvolvedRow.apply _).tupled
}

class CrimeTypeTable(tag: Tag) extends Table[CrimeTypeRow](tag, Some("APP"), "CRIME_TYPE") {
  def id = column[String]("ID", O.PrimaryKey, O.Length(50, varying = true))
  def name = column[String]("NAME", O.Length(100, varying = true))
  def description = column[Option[String]]("DESCRIPTION", O.Length(1024, varying = true))
  def * = (id, name, description) <> (CrimeTypeRow.tupled, CrimeTypeRow.unapply)
}

class CrimeInvolvedTable(tag: Tag) extends Table[CrimeInvolvedRow](tag, Some("APP"), "CRIME_INVOLVED") {
  def id = column[String]("ID", O.PrimaryKey, O.Length(50, varying = true))
  def crimeType = column[String]("CRIME_TYPE", O.Length(50, varying = true))
  def name = column[String]("NAME", O.Length(100, varying = true))
  def description = column[Option[String]]("DESCRIPTION", O.Length(1024, varying = true))
  def * = (id, crimeType, name, description) <> (CrimeInvolvedRow.tupled, CrimeInvolvedRow.unapply)
  def crimeTypeFk = foreignKey("CRIME_INVOLVED_TYPE_FKEY", crimeType, Tables.crimeTypes)(_.id)
}

object Tables {
  val crimeTypes = TableQuery[CrimeTypeTable]
  val crimeInvolved = TableQuery[CrimeInvolvedTable]
}
