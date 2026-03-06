
package models

import slick.jdbc.PostgresProfile.api._

case class StrKitRow(
  id: String,
  name: String,
  `type`: Int,
  lociQty: Int,
  representativeParameter: Int
)
object StrKitRow {
  val tupled = (apply _).tupled
}

class StrKitTable(tag: Tag) extends Table[StrKitRow](tag, "STRKIT") {
  def id = column[String]("ID", O.PrimaryKey, O.Length(50, varying = true))
  def name = column[String]("NAME", O.Length(100, varying = true))
  def `type` = column[Int]("TYPE")
  def lociQty = column[Int]("LOCI_QTY")
  def representativeParameter = column[Int]("REPRESENTATIVE_PARAMETER")
  def * = (id, name, `type`, lociQty, representativeParameter) <> (StrKitRow.tupled, StrKitRow.unapply)
}

object StrKitTable {
  val query = TableQuery[StrKitTable]
}

case class StrKitAliasRow(
  kit: String,
  alias: String
)
object StrKitAliasRow {
  val tupled = (apply _).tupled
}

class StrKitAliasTable(tag: Tag) extends Table[StrKitAliasRow](tag, "STRKIT_ALIAS") {
  def kit = column[String]("KIT", O.Length(50, varying = true))
  def alias = column[String]("ALIAS", O.PrimaryKey, O.Length(100, varying = true))
  def * = (kit, alias) <> (StrKitAliasRow.tupled, StrKitAliasRow.unapply)
}

object StrKitAliasTable {
  val query = TableQuery[StrKitAliasTable]
}

case class StrKitLocusRow(
  strkit: String,
  locus: String,
  fluorophore: Option[String],
  order: Option[Int]
)
object StrKitLocusRow {
  val tupled = (apply _).tupled
}

class StrKitLocusTable(tag: Tag) extends Table[StrKitLocusRow](tag, "STRKIT_LOCUS") {
  def strkit = column[String]("STRKIT", O.Length(50, varying = true))
  def locus = column[String]("LOCUS", O.Length(50, varying = true))
  def fluorophore = column[Option[String]]("FLUOROPHORE", O.Length(10, varying = true))
  def order = column[Option[Int]]("ORDER")
  def pk = primaryKey("STRKIT_LOCUS_PKEY", (strkit, locus))
  def * = (strkit, locus, fluorophore, order) <> (StrKitLocusRow.tupled, StrKitLocusRow.unapply)
}

object StrKitLocusTable {
  val query = TableQuery[StrKitLocusTable]
}
