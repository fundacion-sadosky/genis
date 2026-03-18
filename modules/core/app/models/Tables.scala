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

  // Group table
  case class GroupRow(id: String, name: String, description: Option[String] = None)
  class GroupTable(tag: Tag) extends Table[GroupRow](tag, Some("APP"), "GROUP") {
    def id          = column[String]("ID", O.PrimaryKey, O.Length(50, varying = true))
    def name        = column[String]("NAME", O.Length(100, varying = true))
    def description = column[Option[String]]("DESCRIPTION", O.Length(1024, varying = true), O.Default(None))
    def *           = (id, name, description) <> ((GroupRow.apply _).tupled, GroupRow.unapply)
  }
  val Group = TableQuery[GroupTable]

  // Category table
  case class CategoryRow(
    id: String, group: String, name: String, isReference: Boolean,
    description: Option[String] = None, filiationData: Boolean = false,
    replicate: Boolean = true, pedigreeAssociation: Boolean = false,
    allowManualLoading: Boolean = true, tipo: Int = 1
  )
  class CategoryTable(tag: Tag) extends Table[CategoryRow](tag, Some("APP"), "CATEGORY") {
    def id               = column[String]("ID", O.PrimaryKey, O.Length(50, varying = true))
    def group            = column[String]("GROUP", O.Length(50, varying = true))
    def name             = column[String]("NAME", O.Length(100, varying = true))
    def isReference      = column[Boolean]("IS_REFERENCE", O.Default(true))
    def description      = column[Option[String]]("DESCRIPTION", O.Length(1024, varying = true), O.Default(None))
    def filiationData    = column[Boolean]("FILIATION_DATA", O.Default(false))
    def replicate        = column[Boolean]("REPLICATE", O.Default(true))
    def pedigreeAssoc    = column[Boolean]("PEDIGREE_ASSOCIATION", O.Default(false))
    def allowManualLoad  = column[Boolean]("ALLOW_MANUAL_LOADING", O.Default(true))
    def tipo             = column[Int]("TYPE", O.Default(1))
    def * = (id, group, name, isReference, description, filiationData, replicate, pedigreeAssoc, allowManualLoad, tipo) <>
      ((CategoryRow.apply _).tupled, CategoryRow.unapply)
  }
  val Category = TableQuery[CategoryTable]

  // CategoryModifications table
  case class CategoryModificationsRow(from: String, to: String)
  class CategoryModificationsTable(tag: Tag) extends Table[CategoryModificationsRow](tag, Some("APP"), "CATEGORY_MODIFICATIONS") {
    def from = column[String]("From", O.Length(50, varying = true))
    def to   = column[String]("To",   O.Length(50, varying = true))
    def *    = (from, to) <> ((CategoryModificationsRow.apply _).tupled, CategoryModificationsRow.unapply)
  }
  val CategoryModifications = TableQuery[CategoryModificationsTable]

  // CategoryAlias table
  case class CategoryAliasRow(alias: String, category: String)
  class CategoryAliasTable(tag: Tag) extends Table[CategoryAliasRow](tag, Some("APP"), "CATEGORY_ALIAS") {
    def alias    = column[String]("ALIAS", O.Length(100, varying = true))
    def category = column[String]("CATEGORY", O.Length(50, varying = true))
    def *        = (alias, category) <> ((CategoryAliasRow.apply _).tupled, CategoryAliasRow.unapply)
  }
  val CategoryAlias = TableQuery[CategoryAliasTable]

  // CategoryAssociation table
  case class CategoryAssociationRow(id: Long, category: String, categoryRelated: String, mismatchs: Int = 0, `type`: Int)
  class CategoryAssociationTable(tag: Tag) extends Table[CategoryAssociationRow](tag, Some("APP"), "CATEGORY_ASSOCIATION") {
    def id              = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    def category        = column[String]("CATEGORY", O.Length(50, varying = true))
    def categoryRelated = column[String]("CATEGORY_RELATED", O.Length(50, varying = true))
    def mismatchs       = column[Int]("MISMATCHS", O.Default(0))
    def `type`          = column[Int]("TYPE")
    def * = (id, category, categoryRelated, mismatchs, `type`) <> ((CategoryAssociationRow.apply _).tupled, CategoryAssociationRow.unapply)
  }
  val CategoryAssociation = TableQuery[CategoryAssociationTable]

  // CategoryMatching table
  case class CategoryMatchingRow(
    id: Long, category: String, categoryRelated: String, priority: Int = 1,
    minimumStringency: String = "ImpossibleMatch", failOnMatch: Option[Boolean] = Some(false),
    forwardToUpper: Option[Boolean] = Some(false), matchingAlgorithm: String = "ENFSI",
    minLocusMatch: Int = 10, mismatchsAllowed: Int = 0, `type`: Int, considerForN: Boolean = true
  )
  class CategoryMatchingTable(tag: Tag) extends Table[CategoryMatchingRow](tag, Some("APP"), "CATEGORY_MATCHING") {
    def id                = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    def category          = column[String]("CATEGORY", O.Length(50, varying = true))
    def categoryRelated   = column[String]("CATEGORY_RELATED", O.Length(50, varying = true))
    def priority          = column[Int]("PRIORITY", O.Default(1))
    def minimumStringency = column[String]("MINIMUM_STRINGENCY", O.Length(50, varying = true), O.Default("ImpossibleMatch"))
    def failOnMatch       = column[Option[Boolean]]("FAIL_ON_MATCH", O.Default(Some(false)))
    def forwardToUpper    = column[Option[Boolean]]("FORWARD_TO_UPPER", O.Default(Some(false)))
    def matchingAlgorithm = column[String]("MATCHING_ALGORITHM", O.Length(1024, varying = true), O.Default("ENFSI"))
    def minLocusMatch     = column[Int]("MIN_LOCUS_MATCH", O.Default(10))
    def mismatchsAllowed  = column[Int]("MISMATCHS_ALLOWED", O.Default(0))
    def `type`            = column[Int]("TYPE")
    def considerForN      = column[Boolean]("CONSIDER_FOR_N", O.Default(true))
    def * = (id, category, categoryRelated, priority, minimumStringency, failOnMatch, forwardToUpper,
             matchingAlgorithm, minLocusMatch, mismatchsAllowed, `type`, considerForN) <>
      ((CategoryMatchingRow.apply _).tupled, CategoryMatchingRow.unapply)
  }
  val CategoryMatching = TableQuery[CategoryMatchingTable]

  // CategoryConfiguration table
  case class CategoryConfigurationRow(
    id: Long, category: String, `type`: Int, collectionUri: String = "",
    draftUri: String = "", minLocusPerProfile: String = "K",
    maxOverageDeviatedLoci: String = "0", maxAllelesPerLocus: Int = 6, multiallelic: Boolean = false
  )
  class CategoryConfigurationTable(tag: Tag) extends Table[CategoryConfigurationRow](tag, Some("APP"), "CATEGORY_CONFIGURATION") {
    def id                   = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    def category             = column[String]("CATEGORY", O.Length(50, varying = true))
    def `type`               = column[Int]("TYPE")
    def collectionUri        = column[String]("COLLECTION_URI", O.Length(500, varying = true), O.Default(""))
    def draftUri             = column[String]("DRAFT_URI", O.Length(500, varying = true), O.Default(""))
    def minLocusPerProfile   = column[String]("MIN_LOCUS_PER_PROFILE", O.Length(1024, varying = true), O.Default("K"))
    def maxOverageDeviatedLoci = column[String]("MAX_OVERAGE_DEVIATED_LOCI", O.Length(1024, varying = true), O.Default("0"))
    def maxAllelesPerLocus   = column[Int]("MAX_ALLELES_PER_LOCUS", O.Default(6))
    def multiallelic         = column[Boolean]("MULTIALLELIC", O.Default(false))
    def * = (id, category, `type`, collectionUri, draftUri, minLocusPerProfile,
             maxOverageDeviatedLoci, maxAllelesPerLocus, multiallelic) <>
      ((CategoryConfigurationRow.apply _).tupled, CategoryConfigurationRow.unapply)
  }
  val CategoryConfiguration = TableQuery[CategoryConfigurationTable]

  // CategoryMapping table
  case class CategoryMappingRow(id: String, idSuperior: String)
  class CategoryMappingTable(tag: Tag) extends Table[CategoryMappingRow](tag, Some("APP"), "CATEGORY_MAPPING") {
    def id         = column[String]("ID", O.PrimaryKey, O.Length(50, varying = true))
    def idSuperior = column[String]("ID_SUPERIOR", O.Length(50, varying = true))
    def *          = (id, idSuperior) <> ((CategoryMappingRow.apply _).tupled, CategoryMappingRow.unapply)
  }
  val CategoryMapping = TableQuery[CategoryMappingTable]

  // AnalysisType table
  case class AnalysisTypeRow(id: Int, name: String, mitochondrial: Boolean)
  class AnalysisTypeTable(tag: Tag) extends Table[AnalysisTypeRow](tag, Some("APP"), "ANALYSIS_TYPE") {
    def id            = column[Int]("ID", O.AutoInc, O.PrimaryKey)
    def name          = column[String]("NAME", O.Length(50, varying = true))
    def mitochondrial = column[Boolean]("MITOCHONDRIAL", O.Default(false))
    def *             = (id, name, mitochondrial) <> ((AnalysisTypeRow.apply _).tupled, AnalysisTypeRow.unapply)
  }
  val AnalysisType = TableQuery[AnalysisTypeTable]

  // ProfileData (minimal — only the category column needed for listCategoriesWithProfiles)
  case class ProfileDataCategoryRow(category: String)
  class ProfileDataCategoryTable(tag: Tag) extends Table[ProfileDataCategoryRow](tag, Some("APP"), "PROFILE_DATA") {
    def category = column[String]("CATEGORY")
    def *        = category.mapTo[ProfileDataCategoryRow]
  }
  val ProfileDataCategory = TableQuery[ProfileDataCategoryTable]

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
