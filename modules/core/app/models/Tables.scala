package models

import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._

object Tables {
    // Laboratory table
    final case class LaboratoryRow(
      codeName: String,
      name: String,
      country: String,
      province: String,
      address: String,
      telephone: String,
      contactEmail: String,
      dropIn: Double,
      dropOut: Double
    )
    object LaboratoryRow {
      def tupled = (LaboratoryRow.apply _).tupled
    }

    class LaboratoryTable(tag: Tag) extends Table[LaboratoryRow](tag, Some("APP"), "LABORATORY") {
      def codeName = column[String]("CODE_NAME", O.PrimaryKey, O.Length(20, varying = true))
      def name = column[String]("NAME", O.Length(100, varying = true))
      def country = column[String]("COUNTRY", O.Length(100, varying = true))
      def province = column[String]("PROVINCE", O.Length(100, varying = true))
      def address = column[String]("ADDRESS", O.Length(200, varying = true))
      def telephone = column[String]("TELEPHONE", O.Length(50, varying = true))
      def contactEmail = column[String]("CONTACT_EMAIL", O.Length(100, varying = true))
      def dropIn = column[Double]("DROP_IN")
      def dropOut = column[Double]("DROP_OUT")
      def * = (codeName, name, country, province, address, telephone, contactEmail, dropIn, dropOut) <> (LaboratoryRow.tupled, LaboratoryRow.unapply)
    }
    val laboratories = TableQuery[LaboratoryTable]

    // Geneticist table
    final case class GeneticistRow(
      id: Option[Long],
      name: String,
      lastname: String,
      laboratory: String,
      email: String,
      telephone: String
    )
    object GeneticistRow {
      def tupled = (GeneticistRow.apply _).tupled
    }

    class GeneticistTable(tag: Tag) extends Table[GeneticistRow](tag, Some("APP"), "GENETICIST") {
      def id = column[Option[Long]]("ID", O.PrimaryKey, O.AutoInc)
      def name = column[String]("NAME", O.Length(100, varying = true))
      def lastname = column[String]("LASTNAME", O.Length(100, varying = true))
      def laboratory = column[String]("LABORATORY", O.Length(50, varying = true))
      def email = column[String]("EMAIL", O.Length(100, varying = true))
      def telephone = column[String]("TELEPHONE", O.Length(50, varying = true))
      def * = (id, name, lastname, laboratory, email, telephone) <> (GeneticistRow.tupled, GeneticistRow.unapply)
      def labFk = foreignKey("GENETICIST_LAB_FKEY", laboratory, laboratories)(_.codeName)
    }
    val geneticists = TableQuery[GeneticistTable]
  // Disclaimer table
  class Disclaimer(_tableTag: Tag) extends Table[Option[String]](_tableTag, Some("APP"), "DISCLAIMER") {
    def text = column[Option[String]]("TEXT")
    def * = text
  }
  val Disclaimer = TableQuery[Disclaimer]

  // CrimeType and CrimeInvolved tables
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
    def crimeTypeFk = foreignKey("CRIME_INVOLVED_TYPE_FKEY", crimeType, crimeTypes)(_.id)
  }

  val crimeTypes = TableQuery[CrimeTypeTable]
  val crimeInvolved = TableQuery[CrimeInvolvedTable]

  // BioMaterialType table
  case class BioMaterialTypeRow(id: String, name: String, description: Option[String])
  class BioMaterialType(_tableTag: Tag) extends Table[BioMaterialTypeRow](_tableTag, Some("APP"), "BIO_MATERIAL_TYPE") {
    def id = column[String]("ID", O.PrimaryKey, O.Length(50, varying = true))
    def name = column[String]("NAME", O.Length(100, varying = true))
    def description = column[Option[String]]("DESCRIPTION", O.Length(100, varying = true), O.Default(None))
    def * = (id, name, description) <> ((BioMaterialTypeRow.apply _).tupled, BioMaterialTypeRow.unapply)
  }
  val BioMaterialType = TableQuery[BioMaterialType]

  // MotiveType table
  case class MotiveTypeRow(id: Long, description: String)
  class MotiveTypeTable(tag: Tag) extends Table[MotiveTypeRow](tag, Some("APP"), "MOTIVE_TYPE") {
    def id          = column[Long]("ID", O.PrimaryKey)
    def description = column[String]("DESCRIPTION")
    def *           = (id, description) <> ((MotiveTypeRow.apply _).tupled, MotiveTypeRow.unapply)
  }
  val MotiveType = TableQuery[MotiveTypeTable]

  // Motive table
  case class MotiveRow(id: Long, motiveType: Long, description: String, freeText: Boolean, deleted: Boolean = false)
  class MotiveTable(tag: Tag) extends Table[MotiveRow](tag, Some("APP"), "MOTIVE") {
    def id          = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    def motiveType  = column[Long]("MOTIVE_TYPE")
    def description = column[String]("DESCRIPTION")
    def freeText    = column[Boolean]("FREE_TEXT")
    def deleted     = column[Boolean]("DELETED")
    def *           = (id, motiveType, description, freeText, deleted) <> ((MotiveRow.apply _).tupled, MotiveRow.unapply)
  }
  val Motive = TableQuery[MotiveTable]
}
