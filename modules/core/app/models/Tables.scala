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

    // ---------------------------------------------------------------------------
    // Population Base Frequency tables
    // ---------------------------------------------------------------------------

    final case class PopulationBaseFrequencyNameRow(
      id: Long,
      name: String,
      theta: Double,
      model: String,
      active: Boolean,
      default: Boolean
    )
    object PopulationBaseFrequencyNameRow {
      def tupled = (apply _).tupled
    }

    class PopulationBaseFrequencyNameTable(tag: Tag)
      extends Table[PopulationBaseFrequencyNameRow](tag, Some("APP"), "POPULATION_BASE_FREQUENCY_NAME") {
      def id      = column[Long]("ID", O.PrimaryKey, O.AutoInc)
      def name    = column[String]("NAME", O.Length(50, varying = true))
      def theta   = column[Double]("THETA")
      def model   = column[String]("MODEL", O.Length(50, varying = true))
      def active  = column[Boolean]("ACTIVE")
      def default = column[Boolean]("DEFAULT")
      def *       = (id, name, theta, model, active, default) <>
                    (PopulationBaseFrequencyNameRow.tupled, PopulationBaseFrequencyNameRow.unapply)
    }
    val PopulationBaseFrequencyName = TableQuery[PopulationBaseFrequencyNameTable]

    final case class PopulationBaseFrequencyRow(
      id: Long,
      baseName: Long,
      marker: String,
      allele: Double,
      frequency: BigDecimal
    )
    object PopulationBaseFrequencyRow {
      def tupled = (apply _).tupled
    }

    class PopulationBaseFrequencyTable(tag: Tag)
      extends Table[PopulationBaseFrequencyRow](tag, Some("APP"), "POPULATION_BASE_FREQUENCY") {
      def id        = column[Long]("ID", O.PrimaryKey, O.AutoInc)
      def baseName  = column[Long]("BASE_NAME")
      def marker    = column[String]("MARKER", O.Length(50, varying = true))
      def allele    = column[Double]("ALLELE")
      def frequency = column[BigDecimal]("FREQUENCY")
      def nameFk    = foreignKey("POPULATION_BASE_FREQUENCY_FK", baseName, PopulationBaseFrequencyName)(_.id,
                        onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Restrict)
      def *         = (id, baseName, marker, allele, frequency) <>
                      (PopulationBaseFrequencyRow.tupled, PopulationBaseFrequencyRow.unapply)
    }
    val PopulationBaseFrequency = TableQuery[PopulationBaseFrequencyTable]

    // Trace table
    case class TraceRow(id: Long, profile: String, user: String, date: java.sql.Timestamp, trace: String, kind: String)
    object TraceRow {
      def tupled = (apply _).tupled
    }

    class TraceTable(tag: Tag) extends Table[TraceRow](tag, Some("APP"), "TRACE") {
      def id      = column[Long]("ID", O.AutoInc, O.PrimaryKey)
      def profile = column[String]("PROFILE", O.Length(100, varying = true))
      def user    = column[String]("USER", O.Length(50, varying = true))
      def date    = column[java.sql.Timestamp]("DATE")
      def trace   = column[String]("TRACE")
      def kind    = column[String]("KIND", O.Length(100, varying = true))
      def *       = (id, profile, user, date, trace, kind) <> (TraceRow.tupled, TraceRow.unapply)
    }
    val Trace = TableQuery[TraceTable]

    // TracePedigree table
    case class TracePedigreeRow(id: Long, pedigree: Long, user: String, date: java.sql.Timestamp, trace: String, kind: String)
    object TracePedigreeRow {
      def tupled = (apply _).tupled
    }

    class TracePedigreeTable(tag: Tag) extends Table[TracePedigreeRow](tag, Some("APP"), "TRACE_PEDIGREE") {
      def id       = column[Long]("ID", O.AutoInc, O.PrimaryKey)
      def pedigree = column[Long]("PEDIGREE")
      def user     = column[String]("USER", O.Length(50, varying = true))
      def date     = column[java.sql.Timestamp]("DATE")
      def trace    = column[String]("TRACE")
      def kind     = column[String]("KIND", O.Length(100, varying = true))
      def *        = (id, pedigree, user, date, trace, kind) <> (TracePedigreeRow.tupled, TracePedigreeRow.unapply)
    }
    val TracePedigree = TableQuery[TracePedigreeTable]
  // ---------------------------------------------------------------------------
  // BulkUpload tables
  // ---------------------------------------------------------------------------

  case class BatchProtoProfileRow(
    id: Long,
    user: String,
    date: java.sql.Date,
    label: Option[String],
    analysisType: String
  )
  object BatchProtoProfileRow {
    def tupled = (apply _).tupled
  }

  class BatchProtoProfileTable(tag: Tag, schema: Option[String], tableName: String)
      extends Table[BatchProtoProfileRow](tag, schema, tableName) {
    def id           = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    def user         = column[String]("USER", O.Length(50, varying = true))
    def date         = column[java.sql.Date]("DATE")
    def label        = column[Option[String]]("LABEL", O.Length(50, varying = true))
    def analysisType = column[String]("ANALYSISTYPE", O.Length(50, varying = true))
    def *            = (id, user, date, label, analysisType) <> (BatchProtoProfileRow.tupled, BatchProtoProfileRow.unapply)
  }

  val batchProtoProfiles = new TableQuery(tag => new BatchProtoProfileTable(tag, Some("APP"), "BATCH_PROTO_PROFILE"))

  case class ProtoProfileRow(
    id: Long,
    sampleName: String,
    idBatch: Long,
    assignee: String,
    category: String,
    status: String,
    panel: String,
    errors: Option[String] = None,
    genotypifications: String,
    matchingRules: String,
    mismatchs: String,
    rejectMotive: Option[String] = None,
    preexistence: Option[String] = None,
    genemapperLine: String,
    rejectionUser: Option[String] = None,
    rejectionDate: Option[java.sql.Timestamp] = None,
    idRejectMotive: Option[Long] = None
  )
  object ProtoProfileRow {
    def tupled = (apply _).tupled
  }

  class ProtoProfileTable(tag: Tag, schema: Option[String], tableName: String)
      extends Table[ProtoProfileRow](tag, schema, tableName) {
    def id               = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    def sampleName       = column[String]("SAMPLE_NAME", O.Length(100, varying = true))
    def idBatch          = column[Long]("ID_BATCH")
    def assignee         = column[String]("ASSIGNEE", O.Length(100, varying = true))
    def category         = column[String]("CATEGORY", O.Length(100, varying = true))
    def status           = column[String]("STATUS", O.Length(150, varying = true))
    def panel            = column[String]("PANEL", O.Length(150, varying = true))
    def errors           = column[Option[String]]("ERRORS", O.Length(500, varying = true), O.Default(None))
    def genotypifications= column[String]("GENOTYPIFICATIONS", O.Length(2000, varying = true))
    def matchingRules    = column[String]("MATCHING_RULES", O.Length(2000, varying = true))
    def mismatchs        = column[String]("MISMATCHS", O.Length(2000, varying = true))
    def rejectMotive     = column[Option[String]]("REJECT_MOTIVE", O.Length(2000, varying = true), O.Default(None))
    def preexistence     = column[Option[String]]("PREEXISTENCE", O.Length(100, varying = true), O.Default(None))
    def genemapperLine   = column[String]("GENEMAPPER_LINE", O.Length(5000, varying = true))
    def rejectionUser    = column[Option[String]]("REJECTION_USER", O.Length(5000, varying = true), O.Default(None))
    def rejectionDate    = column[Option[java.sql.Timestamp]]("REJECTION_DATE", O.Default(None))
    def idRejectMotive   = column[Option[Long]]("ID_REJECT_MOTIVE", O.Default(None))
    def * = (id, sampleName, idBatch, assignee, category, status, panel, errors,
             genotypifications, matchingRules, mismatchs, rejectMotive, preexistence,
             genemapperLine, rejectionUser, rejectionDate, idRejectMotive) <>
            (ProtoProfileRow.tupled, ProtoProfileRow.unapply)
  }

  val protoProfiles = new TableQuery(tag => new ProtoProfileTable(tag, Some("APP"), "PROTO_PROFILE"))

  // ---------------------------------------------------------------------------
  // ProfileData full tables (schema-parameterized for APP and STASH usage)
  // ---------------------------------------------------------------------------

  case class ProfileDataRow(
    id: Long,
    category: String,
    globalCode: String,
    internalCode: String,
    description: Option[String] = None,
    attorney: Option[String] = None,
    bioMaterialType: Option[String] = None,
    court: Option[String] = None,
    crimeInvolved: Option[String] = None,
    crimeType: Option[String] = None,
    criminalCase: Option[String] = None,
    internalSampleCode: String,
    assignee: String,
    laboratory: String,
    profileExpirationDate: Option[java.sql.Date] = None,
    responsibleGeneticist: Option[String] = None,
    sampleDate: Option[java.sql.Date] = None,
    sampleEntryDate: Option[java.sql.Date] = None,
    deleted: Boolean = false,
    deletedSolicitor: Option[String] = None,
    deletedMotive: Option[String] = None,
    fromDesktopSearch: Boolean = false
  )
  object ProfileDataRow {
    def tupled = (apply _).tupled
  }

  class ProfileDataTable(tag: Tag, schema: Option[String], tableName: String)
      extends Table[ProfileDataRow](tag, schema, tableName) {
    def id                   = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    def category             = column[String]("CATEGORY", O.Length(50, varying = true))
    def globalCode           = column[String]("GLOBAL_CODE", O.Length(100, varying = true))
    def internalCode         = column[String]("INTERNAL_CODE", O.Length(100, varying = true))
    def description          = column[Option[String]]("DESCRIPTION", O.Length(1024, varying = true), O.Default(None))
    def attorney             = column[Option[String]]("ATTORNEY", O.Length(100, varying = true), O.Default(None))
    def bioMaterialType      = column[Option[String]]("BIO_MATERIAL_TYPE", O.Length(50, varying = true), O.Default(None))
    def court                = column[Option[String]]("COURT", O.Length(100, varying = true), O.Default(None))
    def crimeInvolved        = column[Option[String]]("CRIME_INVOLVED", O.Length(50, varying = true), O.Default(None))
    def crimeType            = column[Option[String]]("CRIME_TYPE", O.Length(50, varying = true), O.Default(None))
    def criminalCase         = column[Option[String]]("CRIMINAL_CASE", O.Length(50, varying = true), O.Default(None))
    def internalSampleCode   = column[String]("INTERNAL_SAMPLE_CODE", O.Length(50, varying = true))
    def assignee             = column[String]("ASSIGNEE", O.Length(50, varying = true))
    def laboratory           = column[String]("LABORATORY", O.Length(50, varying = true))
    def profileExpirationDate= column[Option[java.sql.Date]]("PROFILE_EXPIRATION_DATE", O.Default(None))
    def responsibleGeneticist= column[Option[String]]("RESPONSIBLE_GENETICIST", O.Length(50, varying = true), O.Default(None))
    def sampleDate           = column[Option[java.sql.Date]]("SAMPLE_DATE", O.Default(None))
    def sampleEntryDate      = column[Option[java.sql.Date]]("SAMPLE_ENTRY_DATE", O.Default(None))
    def deleted              = column[Boolean]("DELETED", O.Default(false))
    def deletedSolicitor     = column[Option[String]]("DELETED_SOLICITOR", O.Length(100, varying = true), O.Default(None))
    def deletedMotive        = column[Option[String]]("DELETED_MOTIVE", O.Length(8192, varying = true), O.Default(None))
    def fromDesktopSearch    = column[Boolean]("FROM_DESKTOP_SEARCH", O.Default(false))
    def * = (id, category, globalCode, internalCode, description, attorney, bioMaterialType,
             court, crimeInvolved, crimeType, criminalCase, internalSampleCode, assignee,
             laboratory, profileExpirationDate, responsibleGeneticist, sampleDate,
             sampleEntryDate, deleted, deletedSolicitor, deletedMotive, fromDesktopSearch) <>
            (ProfileDataRow.tupled, ProfileDataRow.unapply)
  }

  val profilesData     = new TableQuery(tag => new ProfileDataTable(tag, Some("APP"), "PROFILE_DATA"))
  val stashProfileData = new TableQuery(tag => new ProfileDataTable(tag, Some("STASH"), "PROFILE_DATA"))

  case class ProfileDataFiliationRow(
    id: Long,
    profileData: String,
    fullName: Option[String],
    nickname: Option[String],
    birthday: Option[java.sql.Date] = None,
    birthPlace: Option[String],
    nationality: Option[String],
    identification: Option[String],
    identificationIssuingAuthority: Option[String],
    address: Option[String]
  )
  object ProfileDataFiliationRow {
    def tupled = (apply _).tupled
  }

  class ProfileDataFiliationTable(tag: Tag, schema: Option[String], tableName: String)
      extends Table[ProfileDataFiliationRow](tag, schema, tableName) {
    def id                             = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    def profileData                    = column[String]("PROFILE_DATA", O.Length(100, varying = true))
    def fullName                       = column[Option[String]]("FULL_NAME", O.Length(150, varying = true), O.Default(None))
    def nickname                       = column[Option[String]]("NICKNAME", O.Length(150, varying = true), O.Default(None))
    def birthday                       = column[Option[java.sql.Date]]("BIRTHDAY", O.Default(None))
    def birthPlace                     = column[Option[String]]("BIRTH_PLACE", O.Length(100, varying = true), O.Default(None))
    def nationality                    = column[Option[String]]("NATIONALITY", O.Length(50, varying = true), O.Default(None))
    def identification                 = column[Option[String]]("IDENTIFICATION", O.Length(100, varying = true), O.Default(None))
    def identificationIssuingAuthority = column[Option[String]]("IDENTIFICATION_ISSUING_AUTHORITY", O.Length(100, varying = true), O.Default(None))
    def address                        = column[Option[String]]("ADDRESS", O.Length(100, varying = true), O.Default(None))
    def * = (id, profileData, fullName, nickname, birthday, birthPlace, nationality,
             identification, identificationIssuingAuthority, address) <>
            (ProfileDataFiliationRow.tupled, ProfileDataFiliationRow.unapply)
  }

  val profileDataFiliations     = new TableQuery(tag => new ProfileDataFiliationTable(tag, Some("APP"), "PROFILE_DATA_FILIATION"))
  val stashProfileDataFiliation = new TableQuery(tag => new ProfileDataFiliationTable(tag, Some("STASH"), "PROFILE_DATA_FILIATION"))

  // resource column uses Array[Byte] — Slick 3.5 maps PostgreSQL bytea to Array[Byte] (structural change from java.sql.Blob)
  case class ProfileDataFiliationResourcesRow(
    id: Long,
    profileDataFiliation: String,
    resource: Array[Byte],
    resourceType: String
  )
  object ProfileDataFiliationResourcesRow {
    def tupled = (apply _).tupled
  }

  class ProfileDataFiliationResourcesTable(tag: Tag, schema: Option[String], tableName: String)
      extends Table[ProfileDataFiliationResourcesRow](tag, schema, tableName) {
    def id                   = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    def profileDataFiliation = column[String]("PROFILE_DATA_FILIATION", O.Length(100, varying = true))
    def resource             = column[Array[Byte]]("RESOURCE")
    def resourceType         = column[String]("RESOURCE_TYPE", O.Length(1, varying = true))
    def * = (id, profileDataFiliation, resource, resourceType) <>
            (ProfileDataFiliationResourcesRow.tupled, ProfileDataFiliationResourcesRow.unapply)
  }

  val profileDataFiliationResources     = new TableQuery(tag => new ProfileDataFiliationResourcesTable(tag, Some("APP"), "PROFILE_DATA_FILIATION_RESOURCES"))
  val stashProfileDataFiliationResources= new TableQuery(tag => new ProfileDataFiliationResourcesTable(tag, Some("STASH"), "PROFILE_DATA_FILIATION_RESOURCES"))

  // ---------------------------------------------------------------------------
  // ProfileDataMotive — audit log for logical deletes
  // ---------------------------------------------------------------------------
  case class ProfileDataMotiveRow(id: Long, idProfileData: Long, deletedDate: java.sql.Timestamp, idDeletedMotive: Long)
  object ProfileDataMotiveRow {
    def tupled = (apply _).tupled
  }

  class ProfileDataMotiveTable(tag: Tag) extends Table[ProfileDataMotiveRow](tag, Some("APP"), "PROFILE_DATA_MOTIVE") {
    def id              = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    def idProfileData   = column[Long]("ID_PROFILE_DATA")
    def deletedDate     = column[java.sql.Timestamp]("DELETED_DATE")
    def idDeletedMotive = column[Long]("ID_DELETED_MOTIVE")
    def * = (id, idProfileData, deletedDate, idDeletedMotive) <> (ProfileDataMotiveRow.tupled, ProfileDataMotiveRow.unapply)
  }

  val profileDataMotive = TableQuery[ProfileDataMotiveTable]

  // ---------------------------------------------------------------------------
  // ExternalProfileData — tracks origin of profiles received from other instances
  // ---------------------------------------------------------------------------
  case class ExternalProfileDataRow(id: Long, laboratoryOrigin: String, laboratoryImmediate: String)
  object ExternalProfileDataRow {
    def tupled = (apply _).tupled
  }

  class ExternalProfileDataTable(tag: Tag) extends Table[ExternalProfileDataRow](tag, Some("APP"), "EXTERNAL_PROFILE_DATA") {
    def id                  = column[Long]("ID", O.PrimaryKey)
    def laboratoryOrigin    = column[String]("LABORATORY_ORIGIN", O.Length(50, varying = true))
    def laboratoryImmediate = column[String]("LABORATORY_IMMEDIATE", O.Length(50, varying = true))
    def * = (id, laboratoryOrigin, laboratoryImmediate) <> (ExternalProfileDataRow.tupled, ExternalProfileDataRow.unapply)
  }

  val externalProfileData = TableQuery[ExternalProfileDataTable]

  // ---------------------------------------------------------------------------
  // ProfileUploaded — tracks replication status to superior instance
  // ---------------------------------------------------------------------------
  case class ProfileUploadedRow(
    id: Long,
    globalCode: String,
    status: Long,
    motive: Option[String] = None,
    interconnectionError: Option[String] = None,
    userName: Option[String] = None,
    operationOriginatedInInstance: Option[String] = None,
    dateUploaded: Option[java.sql.Timestamp] = None
  )
  object ProfileUploadedRow {
    def tupled = (apply _).tupled
  }

  class ProfileUploadedTable(tag: Tag) extends Table[ProfileUploadedRow](tag, Some("APP"), "PROFILE_UPLOADED") {
    def id                            = column[Long]("ID", O.PrimaryKey)
    def globalCode                    = column[String]("GLOBAL_CODE", O.Length(100, varying = true))
    def status                        = column[Long]("STATUS")
    def motive                        = column[Option[String]]("MOTIVE", O.Length(1024, varying = true), O.Default(None))
    def interconnectionError          = column[Option[String]]("INTERCONNECTION_ERROR", O.Length(1024, varying = true), O.Default(None))
    def userName                      = column[Option[String]]("USER_NAME", O.Length(50, varying = true), O.Default(None))
    def operationOriginatedInInstance = column[Option[String]]("OPERATION_ORIGINATED_IN_INSTANCE", O.Length(100, varying = true), O.Default(None))
    def dateUploaded                  = column[Option[java.sql.Timestamp]]("DATE_UPLOADED", O.Default(None))
    def * = (id, globalCode, status, motive, interconnectionError, userName, operationOriginatedInInstance, dateUploaded) <>
            (ProfileUploadedRow.tupled, ProfileUploadedRow.unapply)
  }

  val profileUploaded = TableQuery[ProfileUploadedTable]

  // ---------------------------------------------------------------------------
  // ProfileSent — tracks profiles sent to inferior instances
  // ---------------------------------------------------------------------------
  case class ProfileSentRow(
    id: Long,
    labCode: String,
    globalCode: String,
    status: Long,
    motive: Option[String] = None,
    interconnectionError: Option[String] = None,
    userName: Option[String] = None
  )
  object ProfileSentRow {
    def tupled = (apply _).tupled
  }

  class ProfileSentTable(tag: Tag) extends Table[ProfileSentRow](tag, Some("APP"), "PROFILE_SENT") {
    def id                   = column[Long]("ID", O.PrimaryKey)
    def labCode              = column[String]("LAB_CODE", O.Length(50, varying = true))
    def globalCode           = column[String]("GLOBAL_CODE", O.Length(100, varying = true))
    def status               = column[Long]("STATUS")
    def motive               = column[Option[String]]("MOTIVE", O.Length(1024, varying = true), O.Default(None))
    def interconnectionError = column[Option[String]]("INTERCONNECTION_ERROR", O.Length(1024, varying = true), O.Default(None))
    def userName             = column[Option[String]]("USER_NAME", O.Length(50, varying = true), O.Default(None))
    def * = (id, labCode, globalCode, status, motive, interconnectionError, userName) <>
            (ProfileSentRow.tupled, ProfileSentRow.unapply)
  }

  val profileSent = TableQuery[ProfileSentTable]

  // ---------------------------------------------------------------------------
  // ProfileReceived — tracks profiles received from inferior instances
  // ---------------------------------------------------------------------------
  case class ProfileReceivedRow(
    globalCode: String,
    labCode: String,
    status: Long,
    motive: Option[String] = None,
    userName: Option[String] = None,
    isCategoryModification: Boolean,
    interconnectionError: Option[String] = None,
    operationOriginatedInInstance: String,
    dateReceived: Option[java.sql.Timestamp] = None
  )
  object ProfileReceivedRow {
    def tupled = (apply _).tupled
  }

  class ProfileReceivedTable(tag: Tag) extends Table[ProfileReceivedRow](tag, Some("APP"), "PROFILE_RECEIVED") {
    def globalCode                    = column[String]("GLOBAL_CODE", O.Length(100, varying = true), O.PrimaryKey)
    def labCode                       = column[String]("LAB_CODE", O.Length(50, varying = true))
    def status                        = column[Long]("STATUS")
    def motive                        = column[Option[String]]("MOTIVE", O.Length(1024, varying = true), O.Default(None))
    def userName                      = column[Option[String]]("USER_NAME", O.Length(50, varying = true), O.Default(None))
    def isCategoryModification        = column[Boolean]("IS_CATEGORY_MODIFICATION")
    def interconnectionError          = column[Option[String]]("INTERCONNECTION_ERROR", O.Length(1024, varying = true), O.Default(None))
    def operationOriginatedInInstance = column[String]("OPERATION_ORIGINATED_IN_INSTANCE", O.Length(100, varying = true))
    def dateReceived                  = column[Option[java.sql.Timestamp]]("DATE_RECEIVED", O.Default(None))
    def * = (globalCode, labCode, status, motive, userName, isCategoryModification,
             interconnectionError, operationOriginatedInInstance, dateReceived) <>
            (ProfileReceivedRow.tupled, ProfileReceivedRow.unapply)
  }

  val profileReceived = TableQuery[ProfileReceivedTable]

  // ---------------------------------------------------------------------------
  // MitochondrialRcrs — reference RCRS sequence for mitochondrial analysis
  // ---------------------------------------------------------------------------
  case class MitochondrialRcrsRow(position: Int, base: String)
  object MitochondrialRcrsRow {
    def tupled = (apply _).tupled
  }

  class MitochondrialRcrsTable(tag: Tag) extends Table[MitochondrialRcrsRow](tag, Some("APP"), "MITOCHONDRIAL_RCRS") {
    def position = column[Int]("POSITION", O.PrimaryKey)
    def base     = column[String]("BASE", O.Length(1, varying = true))
    def * = (position, base) <> (MitochondrialRcrsRow.tupled, MitochondrialRcrsRow.unapply)
  }

  val mitochondrialRcrs = TableQuery[MitochondrialRcrsTable]

  // ---------------------------------------------------------------------------
  // SuperiorInstanceProfileApproval — interconnection approval tracking
  // ---------------------------------------------------------------------------
  case class SuperiorInstanceProfileApprovalRow(
    id: Long,
    globalCode: String,
    profile: String,
    laboratory: String,
    laboratoryInstanceOrigin: String,
    laboratoryImmediateInstance: String,
    sampleEntryDate: Option[java.sql.Date] = None,
    receptionDate: Option[java.sql.Timestamp] = None,
    errors: Option[String] = None,
    rejectionUser: Option[String] = None,
    rejectionDate: Option[java.sql.Timestamp] = None,
    idRejectMotive: Option[Long] = None,
    rejectMotive: Option[String] = None,
    deleted: Boolean = false,
    profileAssociated: Option[String] = None
  )
  object SuperiorInstanceProfileApprovalRow {
    def tupled = (apply _).tupled
  }

  class SuperiorInstanceProfileApprovalTable(tag: Tag)
      extends Table[SuperiorInstanceProfileApprovalRow](tag, Some("APP"), "SUPERIOR_INSTANCE_PROFILE_APPROVAL") {
    def id                          = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    def globalCode                  = column[String]("GLOBAL_CODE", O.Length(100, varying = true))
    def profile                     = column[String]("PROFILE", O.Length(100, varying = true))
    def laboratory                  = column[String]("LABORATORY", O.Length(50, varying = true))
    def laboratoryInstanceOrigin    = column[String]("LABORATORY_INSTANCE_ORIGIN", O.Length(50, varying = true))
    def laboratoryImmediateInstance = column[String]("LABORATORY_IMMEDIATE_INSTANCE", O.Length(50, varying = true))
    def sampleEntryDate             = column[Option[java.sql.Date]]("SAMPLE_ENTRY_DATE", O.Default(None))
    def receptionDate               = column[Option[java.sql.Timestamp]]("RECEPTION_DATE", O.Default(None))
    def errors                      = column[Option[String]]("ERRORS", O.Length(1024, varying = true), O.Default(None))
    def rejectionUser               = column[Option[String]]("REJECTION_USER", O.Length(50, varying = true), O.Default(None))
    def rejectionDate               = column[Option[java.sql.Timestamp]]("REJECTION_DATE", O.Default(None))
    def idRejectMotive              = column[Option[Long]]("ID_REJECT_MOTIVE", O.Default(None))
    def rejectMotive                = column[Option[String]]("REJECT_MOTIVE", O.Length(1024, varying = true), O.Default(None))
    def deleted                     = column[Boolean]("DELETED", O.Default(false))
    def profileAssociated           = column[Option[String]]("PROFILE_ASSOCIATED", O.Length(100, varying = true), O.Default(None))
    def * = (id, globalCode, profile, laboratory, laboratoryInstanceOrigin, laboratoryImmediateInstance,
             sampleEntryDate, receptionDate, errors, rejectionUser, rejectionDate,
             idRejectMotive, rejectMotive, deleted, profileAssociated) <>
            (SuperiorInstanceProfileApprovalRow.tupled, SuperiorInstanceProfileApprovalRow.unapply)
  }

  val superiorInstanceProfileApproval = TableQuery[SuperiorInstanceProfileApprovalTable]
  // Operation Log tables (audit / LOG_DB schema)
  // ---------------------------------------------------------------------------

  case class OperationLogLotRow(id: Long, keyZero: String, initTime: java.sql.Timestamp)

  class OperationLogLotTable(tag: Tag)
      extends Table[OperationLogLotRow](tag, Some("LOG_DB"), "OPERATION_LOG_LOT") {
    def id       = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def keyZero  = column[String]("KEY_ZERO", O.Length(200, varying = true))
    def initTime = column[java.sql.Timestamp]("INIT_TIME")
    def *        = (id, keyZero, initTime) <> ((OperationLogLotRow.apply _).tupled, OperationLogLotRow.unapply)
  }
  val OperationLogLot = TableQuery[OperationLogLotTable]

  case class OperationLogRecordRow(
    id:          Long,
    userId:      String,
    otp:         Option[String],
    timestamp:   java.sql.Timestamp,
    method:      String,
    path:        String,
    action:      String,
    buildNo:     String,
    result:      Option[String],
    status:      Int,
    signature:   String,
    lot:         Long,
    description: String
  )

  class OperationLogRecordTable(tag: Tag)
      extends Table[OperationLogRecordRow](tag, Some("LOG_DB"), "OPERATION_LOG_RECORD") {
    def id          = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def userId      = column[String]("USER_ID", O.Length(50, varying = true))
    def otp         = column[Option[String]]("OTP", O.Length(50, varying = true))
    def timestamp   = column[java.sql.Timestamp]("TIMESTAMP")
    def method      = column[String]("METHOD", O.Length(50, varying = true))
    def path        = column[String]("PATH", O.Length(1024, varying = true))
    def action      = column[String]("ACTION", O.Length(512, varying = true))
    def buildNo     = column[String]("BUILD_NO", O.Length(150, varying = true))
    def result      = column[Option[String]]("RESULT", O.Length(150, varying = true))
    def status      = column[Int]("STATUS")
    def signature   = column[String]("SIGNATURE", O.Length(8192, varying = true))
    def lot         = column[Long]("LOT")
    def description = column[String]("DESCRIPTION", O.Length(1024, varying = true))
    def lotFk       = foreignKey("OPERATION_LOG_RECORD_FK", lot, OperationLogLot)(_.id,
                        onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)
    def *           = (id, userId, otp, timestamp, method, path, action, buildNo, result,
                        status, signature, lot, description) <>
                      ((OperationLogRecordRow.apply _).tupled, OperationLogRecordRow.unapply)
  }
  val OperationLogRecord = TableQuery[OperationLogRecordTable]

  // ---------------------------------------------------------------------------
  // Mutation model tables (pedigree / MPI-DVI)
  // ---------------------------------------------------------------------------

  case class MutationModelTypeRow(id: Long, description: String)
  object MutationModelTypeRow { def tupled = (apply _).tupled }

  class MutationModelTypeTable(tag: Tag)
      extends Table[MutationModelTypeRow](tag, Some("APP"), "MUTATION_MODEL_TYPE") {
    def id          = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def description = column[String]("DESCRIPTION")
    def *           = (id, description) <> (MutationModelTypeRow.tupled, MutationModelTypeRow.unapply)
  }
  val MutationModelType = TableQuery[MutationModelTypeTable]

  case class MutationModelRow(
    id: Long,
    name: String,
    mutationType: Long,
    active: Boolean,
    ignoreSex: Boolean,
    cantSaltos: Long
  )
  object MutationModelRow { def tupled = (apply _).tupled }

  class MutationModelTable(tag: Tag)
      extends Table[MutationModelRow](tag, Some("APP"), "MUTATION_MODEL") {
    def id           = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def name         = column[String]("NAME")
    def mutationType = column[Long]("MUTATION_MODEL_TYPE")
    def active       = column[Boolean]("ACTIVE")
    def ignoreSex    = column[Boolean]("IGNORE_SEX")
    def cantSaltos   = column[Long]("CANT_SALTOS")
    def * = (id, name, mutationType, active, ignoreSex, cantSaltos) <> (MutationModelRow.tupled, MutationModelRow.unapply)
  }
  val MutationModel = TableQuery[MutationModelTable]

  case class MutationModelParameterRow(
    id: Long,
    idMutationModel: Long,
    locus: String,
    sex: String,
    mutationRate: Option[scala.math.BigDecimal],
    mutationRange: Option[scala.math.BigDecimal],
    mutationRateMicrovariant: Option[scala.math.BigDecimal]
  )
  object MutationModelParameterRow { def tupled = (apply _).tupled }

  class MutationModelParameterTable(tag: Tag)
      extends Table[MutationModelParameterRow](tag, Some("APP"), "MUTATION_MODEL_PARAMETER") {
    def id                        = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def idMutationModel           = column[Long]("ID_MUTATION_MODEL")
    def locus                     = column[String]("LOCUS")
    def sex                       = column[String]("SEX")
    def mutationRate              = column[Option[scala.math.BigDecimal]]("MUTATION_RATE")
    def mutationRange             = column[Option[scala.math.BigDecimal]]("MUTATION_RANGE")
    def mutationRateMicrovariant  = column[Option[scala.math.BigDecimal]]("MUTATION_RATE_MICROVARIANT")
    def * = (id, idMutationModel, locus, sex, mutationRate, mutationRange, mutationRateMicrovariant) <> (MutationModelParameterRow.tupled, MutationModelParameterRow.unapply)
  }
  val MutationModelParameter = TableQuery[MutationModelParameterTable]

  case class MutationModelKiRow(
    id: Long,
    idMutationModelParameter: Long,
    allele: Double,
    ki: scala.math.BigDecimal
  )
  object MutationModelKiRow { def tupled = (apply _).tupled }

  class MutationModelKiTable(tag: Tag)
      extends Table[MutationModelKiRow](tag, Some("APP"), "MUTATION_MODEL_KI") {
    def id                       = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def idMutationModelParameter = column[Long]("ID_MUTATION_MODEL_PARAMETER")
    def allele                   = column[Double]("ALLELE")
    def ki                       = column[scala.math.BigDecimal]("KI")
    def * = (id, idMutationModelParameter, allele, ki) <> (MutationModelKiRow.tupled, MutationModelKiRow.unapply)
  }
  val MutationModelKi = TableQuery[MutationModelKiTable]

  case class MutationDefaultParameterRow(
    id: Long,
    locus: String,
    sex: String,
    mutationRate: Option[scala.math.BigDecimal]
  )
  object MutationDefaultParameterRow { def tupled = (apply _).tupled }

  class MutationDefaultParameterTable(tag: Tag)
      extends Table[MutationDefaultParameterRow](tag, Some("APP"), "MUTATION_DEFAULT_PARAMETER") {
    def id           = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def locus        = column[String]("LOCUS")
    def sex          = column[String]("SEX")
    def mutationRate = column[Option[scala.math.BigDecimal]]("MUTATION_RATE")
    def * = (id, locus, sex, mutationRate) <> (MutationDefaultParameterRow.tupled, MutationDefaultParameterRow.unapply)
  }
  val MutationDefaultParameter = TableQuery[MutationDefaultParameterTable]

  case class LocusAllelesRow(id: Long, locus: String, allele: Double)
  object LocusAllelesRow { def tupled = (apply _).tupled }

  class LocusAlleleTable(tag: Tag)
      extends Table[LocusAllelesRow](tag, Some("APP"), "LOCUS_ALLELE") {
    def id     = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def locus  = column[String]("LOCUS")
    def allele = column[Double]("ALLELE")
    def *      = (id, locus, allele) <> (LocusAllelesRow.tupled, LocusAllelesRow.unapply)
  }
  val LocusAllele = TableQuery[LocusAlleleTable]

  // ---------------------------------------------------------------------------
  // Court Case / Pedigree tables
  // ---------------------------------------------------------------------------

  case class CourtCaseRow(
    id: Long,
    attorney: Option[String] = None,
    court: Option[String] = None,
    assignee: String,
    internalSampleCode: String,
    crimeInvolved: Option[String] = None,
    crimeType: Option[String] = None,
    criminalCase: Option[String] = None,
    status: String = "Open",
    caseType: String = "MPI"
  )
  object CourtCaseRow {
    def tupled = (CourtCaseRow.apply _).tupled
  }

  class CourtCaseTable(tag: Tag) extends Table[CourtCaseRow](tag, Some("APP"), "COURT_CASE") {
    def id               = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def attorney         = column[Option[String]]("ATTORNEY", O.Length(100, varying = true), O.Default(None))
    def court            = column[Option[String]]("COURT", O.Length(100, varying = true), O.Default(None))
    def assignee         = column[String]("ASSIGNEE", O.Length(50, varying = true))
    def internalSampleCode = column[String]("INTERNAL_SAMPLE_CODE", O.Length(50, varying = true))
    def crimeInvolved    = column[Option[String]]("CRIME_INVOLVED", O.Length(50, varying = true), O.Default(None))
    def crimeType        = column[Option[String]]("CRIME_TYPE", O.Length(50, varying = true), O.Default(None))
    def criminalCase     = column[Option[String]]("CRIMINAL_CASE", O.Length(50, varying = true), O.Default(None))
    def status           = column[String]("STATUS", O.Default("Open"))
    def caseType         = column[String]("CASE_TYPE", O.Length(50, varying = true), O.Default("MPI"))
    def *                = (id, attorney, court, assignee, internalSampleCode, crimeInvolved, crimeType, criminalCase, status, caseType) <> (CourtCaseRow.tupled, CourtCaseRow.unapply)
  }
  val CourtCase = TableQuery[CourtCaseTable]

  case class CaseTypeRow(id: String, name: String)
  object CaseTypeRow {
    def tupled = (CaseTypeRow.apply _).tupled
  }

  class CaseTypeTable(tag: Tag) extends Table[CaseTypeRow](tag, Some("APP"), "CASE_TYPE") {
    def id   = column[String]("ID", O.PrimaryKey, O.Length(50, varying = true))
    def name = column[String]("NAME", O.Length(50, varying = true))
    def *    = (id, name) <> (CaseTypeRow.tupled, CaseTypeRow.unapply)
  }
  val CaseType = TableQuery[CaseTypeTable]

  case class CourtCaseFiliationDataRow(
    id: Long,
    courtCaseId: Long,
    firstname: Option[String] = None,
    lastname: Option[String] = None,
    sex: Option[String] = None,
    dateOfBirth: Option[java.sql.Date] = None,
    dateOfBirthFrom: Option[java.sql.Date] = None,
    dateOfBirthTo: Option[java.sql.Date] = None,
    dateOfMissing: Option[java.sql.Date] = None,
    nationality: Option[String] = None,
    identification: Option[String] = None,
    height: Option[String] = None,
    weight: Option[String] = None,
    haircolor: Option[String] = None,
    skincolor: Option[String] = None,
    clothing: Option[String] = None,
    alias: String,
    particularities: Option[String] = None
  )
  object CourtCaseFiliationDataRow {
    def tupled = (CourtCaseFiliationDataRow.apply _).tupled
  }

  class CourtCaseFiliationDataTable(tag: Tag) extends Table[CourtCaseFiliationDataRow](tag, Some("APP"), "COURT_CASE_DATA_FILIATION") {
    def id              = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def courtCaseId     = column[Long]("ID_COURT_CASE")
    def firstname       = column[Option[String]]("FIRSTNAME", O.Length(100, varying = true), O.Default(None))
    def lastname        = column[Option[String]]("LASTNAME", O.Length(100, varying = true), O.Default(None))
    def sex             = column[Option[String]]("SEX", O.Length(50, varying = true), O.Default(None))
    def dateOfBirth     = column[Option[java.sql.Date]]("DATE_OF_BIRTH", O.Default(None))
    def dateOfBirthFrom = column[Option[java.sql.Date]]("DATE_OF_BIRTH_FROM", O.Default(None))
    def dateOfBirthTo   = column[Option[java.sql.Date]]("DATE_OF_BIRTH_TO", O.Default(None))
    def dateOfMissing   = column[Option[java.sql.Date]]("DATE_OF_MISSING", O.Default(None))
    def nationality     = column[Option[String]]("NATIONALITY", O.Length(50, varying = true), O.Default(None))
    def identification  = column[Option[String]]("IDENTIFICATION", O.Length(50, varying = true), O.Default(None))
    def height          = column[Option[String]]("HEIGHT", O.Length(50, varying = true), O.Default(None))
    def weight          = column[Option[String]]("WEIGHT", O.Length(50, varying = true), O.Default(None))
    def haircolor       = column[Option[String]]("HAIRCOLOR", O.Length(50, varying = true), O.Default(None))
    def skincolor       = column[Option[String]]("SKINCOLOR", O.Length(50, varying = true), O.Default(None))
    def clothing        = column[Option[String]]("CLOTHING", O.Length(50, varying = true), O.Default(None))
    def alias           = column[String]("ALIAS", O.Length(50, varying = true))
    def particularities = column[Option[String]]("PARTICULARITIES", O.Length(50, varying = true), O.Default(None))
    def *               = (id, courtCaseId, firstname, lastname, sex, dateOfBirth, dateOfBirthFrom, dateOfBirthTo,
                           dateOfMissing, nationality, identification, height, weight, haircolor, skincolor,
                           clothing, alias, particularities) <> (CourtCaseFiliationDataRow.tupled, CourtCaseFiliationDataRow.unapply)
  }
  val CourtCaseFiliationData = TableQuery[CourtCaseFiliationDataTable]

  case class PedigreeRow(
    id: Long,
    courtCaseId: Long,
    name: String,
    creationDate: java.sql.Date,
    status: String = "UnderConstruction",
    assignee: String,
    consistencyRun: Boolean = false
  )
  object PedigreeRow {
    def tupled = (PedigreeRow.apply _).tupled
  }

  class PedigreeTable(tag: Tag) extends Table[PedigreeRow](tag, Some("APP"), "PEDIGREE") {
    def id              = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def courtCaseId     = column[Long]("ID_COURT_CASE")
    def name            = column[String]("NAME", O.Length(100, varying = true))
    def creationDate    = column[java.sql.Date]("CREATION_DATE")
    def status          = column[String]("STATUS", O.Default("UnderConstruction"))
    def assignee        = column[String]("ASSIGNEE", O.Length(50, varying = true))
    def consistencyRun  = column[Boolean]("CONSISTENCY_RUN")
    def *               = (id, courtCaseId, name, creationDate, status, assignee, consistencyRun) <> (PedigreeRow.tupled, PedigreeRow.unapply)
  }
  val Pedigree = TableQuery[PedigreeTable]

  case class CourtCaseProfilesRow(
    idCourtCase: Long,
    globalCode: String,
    profileType: String,
    groupedBy: Option[String]
  )
  object CourtCaseProfilesRow {
    def tupled = (CourtCaseProfilesRow.apply _).tupled
  }

  class CourtCaseProfilesTable(tag: Tag) extends Table[CourtCaseProfilesRow](tag, Some("APP"), "COURT_CASE_PROFILE") {
    def idCourtCase = column[Long]("ID_COURT_CASE")
    def globalCode  = column[String]("GLOBAL_CODE", O.Length(50, varying = true))
    def profileType = column[String]("PROFILE_TYPE", O.Length(50, varying = true))
    def groupedBy   = column[Option[String]]("GROUPED_BY", O.Default(None))
    def pk          = primaryKey("COURT_CASE_PROFILE_PK", (idCourtCase, globalCode))
    def *           = (idCourtCase, globalCode, profileType, groupedBy) <> (CourtCaseProfilesRow.tupled, CourtCaseProfilesRow.unapply)
  }
  val CourtCaseProfiles = TableQuery[CourtCaseProfilesTable]

  // Slim ProfileData row — only columns needed for pedigree joins
  case class PedigreeProfileDataRow(
    id: Long,
    category: String,
    globalCode: String,
    internalCode: String,
    internalSampleCode: String,
    assignee: String
  )
  object PedigreeProfileDataRow {
    def tupled = (PedigreeProfileDataRow.apply _).tupled
  }

  class PedigreeProfileDataTable(tag: Tag) extends Table[PedigreeProfileDataRow](tag, Some("APP"), "PROFILE_DATA") {
    def id                 = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def category           = column[String]("CATEGORY", O.Length(50, varying = true))
    def globalCode         = column[String]("GLOBAL_CODE", O.Length(50, varying = true))
    def internalCode       = column[String]("INTERNAL_CODE", O.Length(50, varying = true))
    def internalSampleCode = column[String]("INTERNAL_SAMPLE_CODE", O.Length(50, varying = true))
    def assignee           = column[String]("ASSIGNEE", O.Length(50, varying = true))
    def deleted            = column[Boolean]("DELETED", O.Default(false))
    def *                  = (id, category, globalCode, internalCode, internalSampleCode, assignee) <> (PedigreeProfileDataRow.tupled, PedigreeProfileDataRow.unapply)
  }
  val PedigreeProfileData = TableQuery[PedigreeProfileDataTable]

  // Slim ProtoProfile row — only columns needed for pedigree joins
  case class PedigreeProtoProfileRow(
    id: Long,
    sampleName: String,
    idBatch: Long,
    status: String,
    preexistence: Option[String]
  )
  object PedigreeProtoProfileRow {
    def tupled = (PedigreeProtoProfileRow.apply _).tupled
  }

  class PedigreeProtoProfileTable(tag: Tag) extends Table[PedigreeProtoProfileRow](tag, Some("APP"), "PROTO_PROFILE") {
    def id          = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def sampleName  = column[String]("SAMPLE_NAME", O.Length(50, varying = true))
    def idBatch     = column[Long]("ID_BATCH")
    def status      = column[String]("STATUS", O.Length(50, varying = true))
    def preexistence = column[Option[String]]("PREEXISTENCE", O.Default(None))
    def *           = (id, sampleName, idBatch, status, preexistence) <> (PedigreeProtoProfileRow.tupled, PedigreeProtoProfileRow.unapply)
  }
  val PedigreeProtoProfile = TableQuery[PedigreeProtoProfileTable]

  // Slim BatchProtoProfile row — only columns needed for pedigree joins
  case class PedigreeBatchProtoProfileRow(id: Long, label: Option[String])
  object PedigreeBatchProtoProfileRow {
    def tupled = (PedigreeBatchProtoProfileRow.apply _).tupled
  }

  class PedigreeBatchProtoProfileTable(tag: Tag) extends Table[PedigreeBatchProtoProfileRow](tag, Some("APP"), "BATCH_PROTO_PROFILE") {
    def id    = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def label = column[Option[String]]("LABEL", O.Default(None))
    def *     = (id, label) <> (PedigreeBatchProtoProfileRow.tupled, PedigreeBatchProtoProfileRow.unapply)
  }
  val PedigreeBatchProtoProfile = TableQuery[PedigreeBatchProtoProfileTable]

  case class PedCheckRow(id: Long, idPedigree: Long, locus: String, globalCode: String)
  object PedCheckRow {
    def tupled = (PedCheckRow.apply _).tupled
  }

  class PedCheckTable(tag: Tag) extends Table[PedCheckRow](tag, Some("APP"), "PEDCHECK") {
    def id         = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def idPedigree = column[Long]("ID_PEDIGREE")
    def locus      = column[String]("LOCUS", O.Length(50, varying = true))
    def globalCode = column[String]("GLOBAL_CODE", O.Length(50, varying = true))
    def *          = (id, idPedigree, locus, globalCode) <> (PedCheckRow.tupled, PedCheckRow.unapply)
  }
  val PedCheck = TableQuery[PedCheckTable]
}
