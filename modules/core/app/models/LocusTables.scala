package models

import slick.jdbc.PostgresProfile.api._

case class LocusRow(
  id: String,
  name: String,
  chromosome: Option[String] = None,
  minimumAllelesQty: Int = 2,
  maximumAllelesQty: Int = 2,
  `type`: Int,
  required: Boolean = true,
  minAlleleValue: Option[BigDecimal] = None,
  maxAlleleValue: Option[BigDecimal] = None
)
object LocusRow:
  val tupled = (apply _).tupled

class LocusTable(tag: Tag) extends Table[LocusRow](tag, Some("APP"), "LOCUS"):
  def id = column[String]("ID", O.PrimaryKey, O.Length(50, varying = true))
  def name = column[String]("NAME", O.Length(100, varying = true))
  def chromosome = column[Option[String]]("CHROMOSOME", O.Length(2, varying = true), O.Default(None))
  def minimumAllelesQty = column[Int]("MINIMUM_ALLELES_QTY", O.Default(2))
  def maximumAllelesQty = column[Int]("MAXIMUM_ALLELES_QTY", O.Default(2))
  def `type` = column[Int]("TYPE")
  def required = column[Boolean]("REQUIRED", O.Default(false))
  def minAlleleValue = column[Option[BigDecimal]]("MIN_ALLELE_VALUE", O.Default(None))
  def maxAlleleValue = column[Option[BigDecimal]]("MAX_ALLELE_VALUE", O.Default(None))
  def * = (id, name, chromosome, minimumAllelesQty, maximumAllelesQty, `type`, required, minAlleleValue, maxAlleleValue) <> (LocusRow.tupled, LocusRow.unapply)

object LocusTable:
  val query = TableQuery[LocusTable]

case class LocusAliasRow(
  alias: String,
  marker: String
)
object LocusAliasRow:
  val tupled = (apply _).tupled

class LocusAliasTable(tag: Tag) extends Table[LocusAliasRow](tag, Some("APP"), "LOCUS_ALIAS"):
  def alias = column[String]("ALIAS", O.PrimaryKey, O.Length(100, varying = true))
  def marker = column[String]("MARKER", O.Length(50, varying = true))
  def locusFk = foreignKey("LOCUS_ALIAS_FK", marker, LocusTable.query)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Restrict)
  def * = (alias, marker) <> (LocusAliasRow.tupled, LocusAliasRow.unapply)

object LocusAliasTable:
  val query = TableQuery[LocusAliasTable]

case class LocusLinkRow(
  id: Long,
  locus: String,
  link: String,
  factor: Double,
  distance: Double
)
object LocusLinkRow:
  val tupled = (apply _).tupled

class LocusLinkTable(tag: Tag) extends Table[LocusLinkRow](tag, Some("APP"), "LOCUS_LINK"):
  def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)
  def locus = column[String]("LOCUS", O.Length(50, varying = true))
  def link = column[String]("LINK", O.Length(50, varying = true))
  def factor = column[Double]("FACTOR")
  def distance = column[Double]("DISTANCE")
  def * = (id, locus, link, factor, distance) <> (LocusLinkRow.tupled, LocusLinkRow.unapply)

object LocusLinkTable:
  val query = TableQuery[LocusLinkTable]

case class AnalysisTypeRow(
  id: Int,
  name: String,
  mitochondrial: Boolean
)
object AnalysisTypeRow:
  val tupled = (apply _).tupled

class AnalysisTypeTable(tag: Tag) extends Table[AnalysisTypeRow](tag, Some("APP"), "ANALYSIS_TYPE"):
  def id = column[Int]("ID", O.AutoInc, O.PrimaryKey)
  def name = column[String]("NAME", O.Length(50, varying = true))
  def mitochondrial = column[Boolean]("MITOCHONDRIAL", O.Default(false))
  def * = (id, name, mitochondrial) <> (AnalysisTypeRow.tupled, AnalysisTypeRow.unapply)

object AnalysisTypeTable:
  val query = TableQuery[AnalysisTypeTable]
