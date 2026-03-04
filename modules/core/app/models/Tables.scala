package models

import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._

object Tables {
  // Disclaimer table
  class Disclaimer(_tableTag: Tag) extends Table[Option[String]](_tableTag, Some("APP"), "DISCLAIMER") {
    def text = column[Option[String]]("TEXT")
    def * = text
  }
  val Disclaimer = TableQuery[Disclaimer]

  // BioMaterialType table
  case class BioMaterialTypeRow(id: String, name: String, description: Option[String])
  class BioMaterialType(_tableTag: Tag) extends Table[BioMaterialTypeRow](_tableTag, Some("APP"), "BIO_MATERIAL_TYPE") {
    def id = column[String]("ID", O.PrimaryKey, O.Length(50, varying = true))
    def name = column[String]("NAME", O.Length(100, varying = true))
    def description = column[Option[String]]("DESCRIPTION", O.Length(100, varying = true), O.Default(None))
    def * = (id, name, description) <> ((BioMaterialTypeRow.apply _).tupled, BioMaterialTypeRow.unapply)
  }
  val BioMaterialType = TableQuery[BioMaterialType]
}
