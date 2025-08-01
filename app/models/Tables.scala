// scalastyle:off
package models

import java.util.Date

import inbox.NotificationType

import scala.slick.model.ForeignKeyAction

// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = scala.slick.driver.PostgresDriver
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: scala.slick.driver.JdbcProfile
  import profile.simple._
  import scala.slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import scala.slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val ddl = BatchProtoProfile.ddl ++ BioMaterialType.ddl ++ Category.ddl ++ CategoryAlias.ddl ++ CategoryAssociation.ddl ++ CategoryMatching.ddl ++ Country.ddl ++ CourtCase.ddl ++ CrimeInvolved.ddl ++ CrimeType.ddl ++ Geneticist.ddl ++ Group.ddl ++ Laboratory.ddl ++ Locus.ddl ++ LocusAlias.ddl ++ MitochondrialRcrs.ddl ++ OperationLogLot.ddl ++ OperationLogRecord.ddl ++ PopulationBaseFrequency.ddl ++ PopulationBaseFrequencyName.ddl ++ ProfileData.ddl ++ ProfileDataFiliation.ddl ++ ProfileDataFiliationResources.ddl ++ ProtoProfile.ddl ++ Province.ddl ++ Strkit.ddl ++ StrkitAlias.ddl ++ StrkitLocus.ddl ++ Notification.ddl ++ AnalysisType.ddl ++ LocusLink.ddl ++ CategoryConfiguration.ddl ++ CategoryConfiguration.ddl ++ Trace.ddl ++ CourtCaseFiliationData.ddl ++ CaseType.ddl ++ Pedigree.ddl

  /** Entity class storing rows of table BatchProtoProfile
   *
   *  @param id Database column ID DBType(BIGINT), AutoInc, PrimaryKey
   *  @param user Database column USER DBType(VARCHAR), Length(50,true)
   *  @param date Database column DATE DBType(DATE)
   *  @param label Database column LABEL DBType(VARCHAR), Length(50)
   *  @param analysisType Database column ANALYSISTYPE DBType(VARCHAR), Length(50)*/

  case class BatchProtoProfileRow(id: Long, user: String, date: java.sql.Date, label: Option[String], analysisType: String)
  /** GetResult implicit for fetching BatchProtoProfileRow objects using plain SQL queries */
  implicit def GetResultBatchProtoProfileRow(implicit e0: GR[Long], e1: GR[String], e2: GR[java.sql.Date]): GR[BatchProtoProfileRow] = GR{
    prs => import prs._
      BatchProtoProfileRow.tupled((<<[Long], <<[String], <<[java.sql.Date], <<?[String], <<[String]))
  }
  /** Table description of table BATCH_PROTO_PROFILE. Objects of this class serve as prototypes for rows in queries. */
  class BatchProtoProfile(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[BatchProtoProfileRow](_tableTag, schema, tableName) {
    def * = (id, user, date, label, analysisType) <> (BatchProtoProfileRow.tupled, BatchProtoProfileRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, user.?, date.?, label, analysisType.?).shaped.<>({r=>import r._; _1.map(_=> BatchProtoProfileRow.tupled((_1.get, _2.get, _3.get, _4,_5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(BIGINT), AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column USER DBType(VARCHAR), Length(50,true) */
    val user: Column[String] = column[String]("USER", O.Length(50,varying=true))
    /** Database column DATE DBType(DATE) */
    val date: Column[java.sql.Date] = column[java.sql.Date]("DATE")
    /** Database column LABEL DBType(VARCHAR), Length(50,true) */
    val label: Column[Option[String]] = column[Option[String]]("LABEL", O.Length(50,varying=true))
    /** Database column ANALYSISTYPE DBType(VARCHAR), Length(50,true) */
    val analysisType: Column[String] = column[String]("ANALYSISTYPE", O.Length(50,varying=true))


  }
  /** Collection-like TableQuery object for table BatchProtoProfile */
  lazy val BatchProtoProfile = new TableQuery(tag => new BatchProtoProfile(tag, Some("APP"), "BATCH_PROTO_PROFILE"))

  /** Entity class storing rows of table BioMaterialType
   *
   *  @param id Database column ID DBType(VARCHAR), PrimaryKey, Length(50,true)
   *  @param name Database column NAME DBType(VARCHAR), Length(100,true)
   *  @param description Database column DESCRIPTION DBType(VARCHAR), Length(10000,true), Default(None) */
  case class BioMaterialTypeRow(id: String, name: String, description: Option[String] = None)
  /** GetResult implicit for fetching BioMaterialTypeRow objects using plain SQL queries */
  implicit def GetResultBioMaterialTypeRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[BioMaterialTypeRow] = GR{
    prs => import prs._
      BioMaterialTypeRow.tupled((<<[String], <<[String], <<?[String]))
  }
  /** Table description of table BIO_MATERIAL_TYPE. Objects of this class serve as prototypes for rows in queries. */
  class BioMaterialType(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[BioMaterialTypeRow](_tableTag, schema, tableName) {
    def * = (id, name, description) <> (BioMaterialTypeRow.tupled, BioMaterialTypeRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, name.?, description).shaped.<>({r=>import r._; _1.map(_=> BioMaterialTypeRow.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(VARCHAR), PrimaryKey, Length(50,true) */
    val id: Column[String] = column[String]("ID", O.PrimaryKey, O.Length(50,varying=true))
    /** Database column NAME DBType(VARCHAR), Length(100,true) */
    val name: Column[String] = column[String]("NAME", O.Length(100,varying=true))
    /** Database column DESCRIPTION DBType(VARCHAR), Length(100,true), Default(None) */
    val description: Column[Option[String]] = column[Option[String]]("DESCRIPTION", O.Length(100,varying=true), O.Default(None))
  }
  /** Collection-like TableQuery object for table BioMaterialType */
  lazy val BioMaterialType = new TableQuery(tag => new BioMaterialType(tag, Some("APP"), "BIO_MATERIAL_TYPE"))

  /** Entity class storing rows of table Category
   *
   *  @param id Database column ID DBType(VARCHAR), PrimaryKey, Length(50,true)
   *  @param group Database column GROUP DBType(VARCHAR), Length(50,true)
   *  @param name Database column NAME DBType(VARCHAR), Length(100,true)
   *  @param description Database column DESCRIPTION DBType(VARCHAR), Length(1024,true), Default(None) */
  case class CategoryRow(id: String, group: String, name: String, isReference: Boolean, description: Option[String] = None, filiationData: Boolean = false, replicate: Boolean = true, pedigreeAssociation: Boolean = false, allowManualLoading: Boolean = true, tipo: Int = 1)
  /** GetResult implicit for fetching CategoryRow objects using plain SQL queries */
  implicit def GetResultCategoryRow(implicit e0: GR[String], e1: GR[Option[String]], e2: GR[Boolean]): GR[CategoryRow] = GR{
    prs => import prs._
      CategoryRow.tupled((<<[String], <<[String], <<[String], <<[Boolean], <<?[String], <<[Boolean], <<[Boolean], <<[Boolean], <<[Boolean], <<[Int]))
  }
  /** Table description of table CATEGORY. Objects of this class serve as prototypes for rows in queries. */
  class Category(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[CategoryRow](_tableTag, schema, tableName) {
    def * = (id, group, name, isReference,  description, filiationData, replicate, pedigreeAssociation, allowManualLoading, tipo) <> (CategoryRow.tupled, CategoryRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, group.?, name.?, isReference.?, description, filiationData.?, replicate.?, pedigreeAssociation.?, allowManualLoading.?,tipo.?).shaped.<>({r=>import r._; _1.map(_=> CategoryRow.tupled((_1.get, _2.get, _3.get, _4.get, _5, _6.get, _7.get, _8.get, _9.get, _10.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(VARCHAR), PrimaryKey, Length(50,true) */
    val id: Column[String] = column[String]("ID", O.PrimaryKey, O.Length(50,varying=true))
    /** Database column GROUP DBType(VARCHAR), Length(50,true) */
    val group: Column[String] = column[String]("GROUP", O.Length(50,varying=true))
    /** Database column NAME DBType(VARCHAR), Length(100,true) */
    val name: Column[String] = column[String]("NAME", O.Length(100,varying=true))
    /** Database column DESCRIPTION DBType(VARCHAR), Length(1024,true), Default(None) */
    val description: Column[Option[String]] = column[Option[String]]("DESCRIPTION", O.Length(1024,varying=true), O.Default(None))
    /** Database column IS_REFERENCE DBType(BOOLEAN), Default(true) */
    val isReference: Column[Boolean] = column[Boolean]("IS_REFERENCE", O.Default(true))
    /** Database column FILIATION_DATA DBType(BOOLEAN), Default(false) */
    val filiationData: Column[Boolean] = column[Boolean]("FILIATION_DATA", O.Default(false))
    /** Database column REPLICATE DBType(BOOLEAN), Default(true) */
    val replicate: Column[Boolean] = column[Boolean]("REPLICATE", O.Default(true))
    /** Database column PEDIGREE_ASSOCIATION DBType(BOOLEAN), Default(false) */
    val pedigreeAssociation: Column[Boolean] = column[Boolean]("PEDIGREE_ASSOCIATION", O.Default(false))
    /** Database column ALLOW_MANUAL_LOADING DBType(BOOLEAN), Default(true) */
    val allowManualLoading: Column[Boolean] = column[Boolean]("ALLOW_MANUAL_LOADING", O.Default(true))

    val tipo : Column[Int] = column[Int]("TYPE",O.Default(1))

    /** Foreign key referencing Group (database name CATEGORY_GROUP_FKEY) */
    lazy val groupFk = foreignKey("CATEGORY_GROUP_FKEY", group, Group)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table Category */
  lazy val Category = new TableQuery(tag => new Category(tag, Some("APP"), "CATEGORY"))

  /**
   * Row definition for CategoryModifications table.
   *
   * @param from an Category ID
   * @param to an Category ID.
   */
  case class CategoryModificationsRow(
                                       from: String,
                                       to: String
                                     )

  implicit def GetResultCategoryModificationsRow(implicit e0: GR[String]): GR[CategoryModificationsRow] = GR {
    prs =>
      import prs._
      CategoryModificationsRow(
        <<[String], // Read the 'from' column
        <<[String]  // Read the 'to' column
      )
  }
  class CategoryModifications(_tableTag: Tag, schema: Option[String], tableName: String)
    extends Table[CategoryModificationsRow](_tableTag, schema, tableName) {
    def * = (from, to) <> (
      CategoryModificationsRow.tupled,
      CategoryModificationsRow.unapply
    )

    val from: Column[String] = column[String]("From", O.Length(50, varying=true))
    val to: Column[String] = column[String]("To", O.Length(50, varying=true))

    /** Foreign keys referencing Category*/
    lazy val fromFk = foreignKey("fk_from", from, Category)(
      r => r.id,
      onUpdate = ForeignKeyAction.Cascade,
      onDelete = ForeignKeyAction.Restrict
    )
    lazy val toFk = foreignKey("fk_to", from, Category)(
      r => r.id,
      onUpdate = ForeignKeyAction.Cascade,
      onDelete = ForeignKeyAction.Restrict
    )
  }
  lazy val CategoryModifications = new TableQuery(
    tag => new CategoryModifications(
      tag,
      Some("APP"),
      "CATEGORY_MODIFICATIONS"
    )
  )

  /** Entity class storing rows of table Category_Configuration
   *
   *  @param id Database column ID DBType(BIGINT), AutoInc, PrimaryKey
   *  @param category Database column CATEGORY DBType(VARCHAR), Length(50,true)
   *  @param type Database column TYPE DBType(SMALLINT)
   *  @param collectionUri Database column COLLECTION_URI DBType(VARCHAR), Length(500,true), Default()
   *  @param draftUri Database column DRAFT_URI DBType(VARCHAR), Length(500,true), Default()
   *  @param minLocusPerProfile Database column MIN_LOCUS_PER_PROFILE DBType(VARCHAR), Length(1024,true), Default(K)
   *  @param maxOverageDeviatedLoci Database column MAX_OVERAGE_DEVIATED_LOCI DBType(VARCHAR), Length(1024,true), Default(0)
   *  @param maxAllelesPerLocus Database column MAX_ALLELES_PER_LOCI DBType(SMALLINT), Default(6) */
  case class CategoryConfigurationRow(id: Long, category: String, `type`: Int, collectionUri: String = "", draftUri: String = "", minLocusPerProfile: String = "K", maxOverageDeviatedLoci: String = "0", maxAllelesPerLocus: Int = 6)
  /** GetResult implicit for fetching CategoryConfigurationRow objects using plain SQL queries */
  implicit def GetResultCategoryConfigurationRow(implicit e0: GR[String], e1: GR[Option[String]], e2: GR[Boolean]): GR[CategoryConfigurationRow] = GR{
    prs => import prs._
      CategoryConfigurationRow.tupled((<<[Long], <<[String], <<[Int], <<[String], <<[String], <<[String], <<[String], <<[Int]))
  }
  /** Table description of table CATEGORY_CONFIGURATION. Objects of this class serve as prototypes for rows in queries. */
  class CategoryConfiguration(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[CategoryConfigurationRow](_tableTag, schema, tableName) {
    def * = (id, category, `type`, collectionUri, draftUri, minLocusPerProfile, maxOverageDeviatedLoci, maxAllelesPerLocus) <> (CategoryConfigurationRow.tupled, CategoryConfigurationRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, category.?, `type`.?, collectionUri.?, draftUri.?, minLocusPerProfile.?, maxOverageDeviatedLoci.?, maxAllelesPerLocus.?).shaped.<>({r=>import r._; _1.map(_=> CategoryConfigurationRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(BIGINT), AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column CATEGORY DBType(VARCHAR), Length(50,true) */
    val category: Column[String] = column[String]("CATEGORY", O.PrimaryKey, O.Length(50,varying=true))
    /** Database column TYPE DBType(SMALLINT)
     *  NOTE: The name was escaped because it collided with a Scala keyword. */
    val `type`: Column[Int] = column[Int]("TYPE")
    /** Database column COLLECTION_URI DBType(VARCHAR), Length(500,true), Default() */
    val collectionUri: Column[String] = column[String]("COLLECTION_URI", O.Length(500,varying=true), O.Default(""))
    /** Database column DRAFT_URI DBType(VARCHAR), Length(500,true), Default() */
    val draftUri: Column[String] = column[String]("DRAFT_URI", O.Length(500,varying=true), O.Default(""))
    /** Database column MIN_LOCUS_PER_PROFILE DBType(VARCHAR), Length(1024,true), Default(K) */
    val minLocusPerProfile: Column[String] = column[String]("MIN_LOCUS_PER_PROFILE", O.Length(1024,varying=true), O.Default("K"))
    /** Database column MAX_OVERAGE_DEVIATED_LOCI DBType(VARCHAR), Length(1024,true), Default(0) */
    val maxOverageDeviatedLoci: Column[String] = column[String]("MAX_OVERAGE_DEVIATED_LOCI", O.Length(1024,varying=true), O.Default("0"))
    /** Database column MAX_ALLELES_PER_LOCUS DBType(SMALLINT), Default(6) */
    val maxAllelesPerLocus: Column[Int] = column[Int]("MAX_ALLELES_PER_LOCUS", O.Default(6))

  }
  /** Collection-like TableQuery object for table CategoryConfiguration */
  lazy val CategoryConfiguration = new TableQuery(tag => new CategoryConfiguration(tag, Some("APP"), "CATEGORY_CONFIGURATION"))

  /** Entity class storing rows of table CategoryAlias
   * @param alias Database column ALIAS DBType(VARCHAR), PrimaryKey, Length(100,true)
   * @param category Database column CATEGORY DBType(VARCHAR), Length(50,true) */
  case class CategoryAliasRow(alias: String, category: String)
  /** GetResult implicit for fetching CategoryAliasRow objects using plain SQL queries */
  implicit def GetResultCategoryAliasRow(implicit e0: GR[String]): GR[CategoryAliasRow] = GR{
    prs => import prs._
      CategoryAliasRow.tupled((<<[String], <<[String]))
  }
  /** Table description of table CATEGORY_ALIAS. Objects of this class serve as prototypes for rows in queries. */
  class CategoryAlias(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[CategoryAliasRow](_tableTag, schema, tableName) {
    def * = (alias, category) <> (CategoryAliasRow.tupled, CategoryAliasRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (alias.?, category.?).shaped.<>({r=>import r._; _1.map(_=> CategoryAliasRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ALIAS DBType(VARCHAR), PrimaryKey, Length(100,true) */
    val alias: Column[String] = column[String]("ALIAS", O.Length(100,varying=true))
    /** Database column CATEGORY DBType(VARCHAR), Length(50,true) */
    val category: Column[String] = column[String]("CATEGORY", O.Length(50,varying=true))

    /** Foreign key referencing Category (database name CATEGORY_ALIAS_FK) */
    lazy val categoryFk = foreignKey("CATEGORY_ALIAS_FK", category, Category)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table CategoryAlias */
  lazy val CategoryAlias = new TableQuery(tag => new CategoryAlias(tag, Some("APP"), "CATEGORY_ALIAS"))

  /** Entity class storing rows of table CategoryAssociation
   * @param id Database column ID DBType(BIGINT), AutoInc, PrimaryKey
   * @param category Database column CATEGORY DBType(VARCHAR), Length(50,true)
   *  @param categoryRelated Database column CATEGORY_RELATED DBType(VARCHAR), Length(50,true)
   *  @param mismatchs Database column MISMATCHS DBType(INTEGER), Default(0)
   *  @param type Database column TYPE DBType(SMALLINT) */
  case class CategoryAssociationRow(id: Long, category: String, categoryRelated: String, mismatchs: Int = 0, `type`: Int)
  /** GetResult implicit for fetching CategoryAssociationRow objects using plain SQL queries */
  implicit def GetResultCategoryAssociationRow(implicit e0: GR[String], e1: GR[Int]): GR[CategoryAssociationRow] = GR{
    prs => import prs._
      CategoryAssociationRow.tupled((<<[Long], <<[String], <<[String], <<[Int], <<[Int]))
  }
  /** Table description of table CATEGORY_ASSOCIATION. Objects of this class serve as prototypes for rows in queries. */
  class CategoryAssociation(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[CategoryAssociationRow](_tableTag, schema, tableName) {
    def * = (id, category, categoryRelated, mismatchs, `type`) <> (CategoryAssociationRow.tupled, CategoryAssociationRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, category.?, categoryRelated.?, mismatchs.?, `type`.?).shaped.<>({r=>import r._; _1.map(_=> CategoryAssociationRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(BIGINT), AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column CATEGORY DBType(VARCHAR), Length(50,true) */
    val category: Column[String] = column[String]("CATEGORY", O.Length(50,varying=true))
    /** Database column CATEGORY_RELATED DBType(VARCHAR), Length(50,true) */
    val categoryRelated: Column[String] = column[String]("CATEGORY_RELATED", O.Length(50,varying=true))
    /** Database column MISMATCHS DBType(INTEGER), Default(0) */
    val mismatchs: Column[Int] = column[Int]("MISMATCHS", O.Default(0))
    /** Database column TYPE DBType(SMALLINT)
     *  NOTE: The name was escaped because it collided with a Scala keyword. */
    val `type`: Column[Int] = column[Int]("TYPE")

    /** Primary key of CategoryAssociation (database name CATEGORY_ASSOCIATION_PKEY) */
    val pk = primaryKey("CATEGORY_ASSOCIATION_PKEY", id)

    /** Foreign key referencing Category (database name CATEGORY_ASSOCIATION_CATEGORY_FKEY) */
    lazy val categoryFk1 = foreignKey("CATEGORY_ASSOCIATION_CATEGORY_FKEY", category, Category)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing Category (database name CATEGORY_ASSOCIATION_CATEGORY_RELATED_FKEY) */
    lazy val categoryFk2 = foreignKey("CATEGORY_ASSOCIATION_CATEGORY_RELATED_FKEY", categoryRelated, Category)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table CategoryAssociation */
  lazy val CategoryAssociation = new TableQuery(tag => new CategoryAssociation(tag, Some("APP"), "CATEGORY_ASSOCIATION"))

  /** Entity class storing rows of table CategoryMatching
   *
   * @param id  Database column ID DBType(BIGINT), AutoInc, PrimaryKey
   *  @param category Database column CATEGORY DBType(VARCHAR), Length(50,true)
   *  @param categoryRelated Database column CATEGORY_RELATED DBType(VARCHAR), Length(50,true)
   *  @param priority Database column PRIORITY DBType(INTEGER), Default(1)
   *  @param minimumStringency Database column MINIMUM_STRINGENCY DBType(VARCHAR), Length(50,true), Default(ImpossibleMatch)
   *  @param failOnMatch Database column FAIL_ON_MATCH DBType(BOOLEAN), Default(Some(false))
   *  @param forwardToUpper Database column FORWARD_TO_UPPER DBType(BOOLEAN), Default(Some(false))
   *  @param matchingAlgorithm Database column MATCHING_ALGORITHM DBType(VARCHAR), Length(1024,true), Default(ENFSI)
   *  @param minLocusMatch Database column MIN_LOCUS_MATCH DBType(INTEGER), Default(10)
   *  @param mismatchsAllowed Database column MISMATCHS_ALLOWED DBType(INTEGER), Default(0)
   *  @param type Database column TYPE DBType(SMALLINT)
   *  @param considerForN Database column TYPE DBType(BOOLEAN9 Default(true) */
  case class CategoryMatchingRow(id: Long, category: String, categoryRelated: String, priority: Int = 1, minimumStringency: String = "ImpossibleMatch", failOnMatch: Option[Boolean] = Some(false), forwardToUpper: Option[Boolean] = Some(false), matchingAlgorithm: String = "ENFSI", minLocusMatch: Int = 10, mismatchsAllowed: Int = 0, `type`: Int, considerForN: Boolean = true)
  /** GetResult implicit for fetching CategoryMatchingRow objects using plain SQL queries */
  implicit def GetResultCategoryMatchingRow(implicit e0: GR[String], e1: GR[Int], e2: GR[Option[Boolean]]): GR[CategoryMatchingRow] = GR{
    prs => import prs._
      CategoryMatchingRow.tupled((<<[Long], <<[String], <<[String], <<[Int], <<[String], <<?[Boolean], <<?[Boolean], <<[String], <<[Int], <<[Int], <<[Int], <<[Boolean]))
  }
  /** Table description of table CATEGORY_MATCHING. Objects of this class serve as prototypes for rows in queries. */
  class CategoryMatching(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[CategoryMatchingRow](_tableTag, schema, tableName) {
    def * = (id, category, categoryRelated, priority, minimumStringency, failOnMatch, forwardToUpper, matchingAlgorithm, minLocusMatch, mismatchsAllowed, `type`, considerForN) <> (CategoryMatchingRow.tupled, CategoryMatchingRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, category.?, categoryRelated.?, priority.?, minimumStringency.?, failOnMatch, forwardToUpper, matchingAlgorithm.?, minLocusMatch.?, mismatchsAllowed.?, `type`.?, considerForN).shaped.<>({r=>import r._; _1.map(_=> CategoryMatchingRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6, _7, _8.get, _9.get, _10.get, _11.get, _12)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(BIGINT), AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column CATEGORY DBType(VARCHAR), Length(50,true) */
    val category: Column[String] = column[String]("CATEGORY", O.Length(50,varying=true))
    /** Database column CATEGORY_RELATED DBType(VARCHAR), Length(50,true) */
    val categoryRelated: Column[String] = column[String]("CATEGORY_RELATED", O.Length(50,varying=true))
    /** Database column PRIORITY DBType(INTEGER), Default(1) */
    val priority: Column[Int] = column[Int]("PRIORITY", O.Default(1))
    /** Database column MINIMUM_STRINGENCY DBType(VARCHAR), Length(50,true), Default(ImpossibleMatch) */
    val minimumStringency: Column[String] = column[String]("MINIMUM_STRINGENCY", O.Length(50,varying=true), O.Default("ImpossibleMatch"))
    /** Database column FAIL_ON_MATCH DBType(BOOLEAN), Default(Some(false)) */
    val failOnMatch: Column[Option[Boolean]] = column[Option[Boolean]]("FAIL_ON_MATCH", O.Default(Some(false)))
    /** Database column FORWARD_TO_UPPER DBType(BOOLEAN), Default(Some(false)) */
    val forwardToUpper: Column[Option[Boolean]] = column[Option[Boolean]]("FORWARD_TO_UPPER", O.Default(Some(false)))
    /** Database column MATCHING_ALGORITHM DBType(VARCHAR), Length(1024,true), Default(ENFSI) */
    val matchingAlgorithm: Column[String] = column[String]("MATCHING_ALGORITHM", O.Length(1024,varying=true), O.Default("ENFSI"))
    /** Database column MIN_LOCUS_MATCH DBType(INTEGER), Default(10) */
    val minLocusMatch: Column[Int] = column[Int]("MIN_LOCUS_MATCH", O.Default(10))
    /** Database column MISMATCHS_ALLOWED DBType(INTEGER), Default(0) */
    val mismatchsAllowed: Column[Int] = column[Int]("MISMATCHS_ALLOWED", O.Default(0))
    /** Database column TYPE DBType(SMALLINT)
     *  NOTE: The name was escaped because it collided with a Scala keyword. */
    val `type`: Column[Int] = column[Int]("TYPE")
    /** Database column CONSIDER_FOR_N DBType(Boolean)**/
    val considerForN: Column[Boolean] = column[Boolean]("CONSIDER_FOR_N", O.Default(true))

    /** Primary key of CategoryMatching (database name CATEGORY_MATCHING_PKEY) */
    val pk = primaryKey("CATEGORY_MATCHING_PKEY", id)

    /** Foreign key referencing Category (database name CATEGORY_MATCHING_CATEGORY_FKEY) */
    lazy val categoryFk1 = foreignKey("CATEGORY_MATCHING_CATEGORY_FKEY", category, Category)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing Category (database name CATEGORY_MATCHING_CATEGORY_RELATED_FKEY) */
    lazy val categoryFk2 = foreignKey("CATEGORY_MATCHING_CATEGORY_RELATED_FKEY", categoryRelated, Category)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table CategoryMatching */
  lazy val CategoryMatching = new TableQuery(tag => new CategoryMatching(tag, Some("APP"), "CATEGORY_MATCHING"))

  /** Entity class storing rows of table Country
   *
   *  @param code Database column CODE DBType(VARCHAR), PrimaryKey, Length(2,true)
   *  @param name Database column NAME DBType(VARCHAR), Length(50,true) */
  case class CountryRow(code: String, name: String)
  /** GetResult implicit for fetching CountryRow objects using plain SQL queries */
  implicit def GetResultCountryRow(implicit e0: GR[String]): GR[CountryRow] = GR{
    prs => import prs._
      CountryRow.tupled((<<[String], <<[String]))
  }
  /** Table description of table COUNTRY. Objects of this class serve as prototypes for rows in queries. */
  class Country(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[CountryRow](_tableTag, schema, tableName) {
    def * = (code, name) <> (CountryRow.tupled, CountryRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (code.?, name.?).shaped.<>({r=>import r._; _1.map(_=> CountryRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column CODE DBType(VARCHAR), PrimaryKey, Length(2,true) */
    val code: Column[String] = column[String]("CODE", O.PrimaryKey, O.Length(2,varying=true))
    /** Database column NAME DBType(VARCHAR), Length(50,true) */
    val name: Column[String] = column[String]("NAME", O.Length(50,varying=true))
  }
  /** Collection-like TableQuery object for table Country */
  lazy val Country = new TableQuery(tag => new Country(tag, Some("APP"), "COUNTRY"))

  /** Entity class storing rows of table CourtCase
   *
   *  @param id Database column ID DBType(BIGINT), AutoInc, PrimaryKey
   *  @param attorney Database column ATTORNEY DBType(VARCHAR), Length(100,true), Default(None)
   *  @param court Database column COURT DBType(VARCHAR), Length(100,true), Default(None)
   *  @param assignee Database column ASSIGNEE DBType(VARCHAR), Length(50,true)
   *  @param internalSampleCode Database column INTERNAL_SAMPLE_CODE DBType(VARCHAR), Length(50,true)
   *  @param crimeInvolved Database column CRIME_INVOLVED DBType(VARCHAR), Length(50,true), Default(None)
   *  @param crimeType Database column CRIME_TYPE DBType(VARCHAR), Length(50,true), Default(None)
   *  @param criminalCase Database column CRIMINAL_CASE DBType(VARCHAR), Length(50,true), Default(None)
   *  @param status Database column STATUS DBType(String), Default(UnderConstruction
   *  @param caseType Database column CASE_TYPE DBType(String),
   */
  case class CourtCaseRow(id: Long, attorney: Option[String] = None, court: Option[String] = None, assignee: String, internalSampleCode: String, crimeInvolved: Option[String] = None, crimeType: Option[String] = None, criminalCase: Option[String] = None, status: String = "Open", caseType:String)
  /** GetResult implicit for fetching CourtCaseRow objects using plain SQL queries */
  implicit def GetResultCourtCaseRow(implicit e0: GR[Long], e1: GR[Option[String]], e2: GR[String], e3: GR[Boolean], e4: GR[Option[java.sql.Date]]): GR[CourtCaseRow] = GR{
    prs => import prs._
      CourtCaseRow.tupled((<<[Long], <<?[String], <<?[String], <<[String], <<[String], <<?[String], <<?[String], <<?[String], <<[String], <<[String]))
  }
  /** Table description of table COURT_CASE. Objects of this class serve as prototypes for rows in queries. */
  class CourtCase(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[CourtCaseRow](_tableTag, schema, tableName) {
    def * = (id, attorney, court, assignee, internalSampleCode, crimeInvolved, crimeType, criminalCase, status, caseType) <> (CourtCaseRow.tupled, CourtCaseRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, attorney, court, assignee.?, internalSampleCode.?, crimeInvolved, crimeType, criminalCase, status, caseType.?).shaped.<>({r=>import r._; _1.map(_=> CourtCaseRow.tupled((_1.get, _2, _3, _4.get, _5.get, _6, _7, _8, _9, _10.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(BIGINT), AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column ATTORNEY DBType(VARCHAR), Length(100,true), Default(None) */
    val attorney: Column[Option[String]] = column[Option[String]]("ATTORNEY", O.Length(100,varying=true), O.Default(None))
    /** Database column COURT DBType(VARCHAR), Length(100,true), Default(None) */
    val court: Column[Option[String]] = column[Option[String]]("COURT", O.Length(100,varying=true), O.Default(None))
    /** Database column ASSIGNEE DBType(VARCHAR), Length(50,true) */
    val assignee: Column[String] = column[String]("ASSIGNEE", O.Length(50,varying=true))
    /** Database column INTERNAL_SAMPLE_CODE DBType(VARCHAR), Length(50,true) */
    val internalSampleCode: Column[String] = column[String]("INTERNAL_SAMPLE_CODE", O.Length(50,varying=true))
    /** Database column CRIME_INVOLVED DBType(VARCHAR), Length(50,true), Default(None) */
    val crimeInvolved: Column[Option[String]] = column[Option[String]]("CRIME_INVOLVED", O.Length(50,varying=true), O.Default(None))
    /** Database column CRIME_TYPE DBType(VARCHAR), Length(50,true), Default(None) */
    val crimeType: Column[Option[String]] = column[Option[String]]("CRIME_TYPE", O.Length(50,varying=true), O.Default(None))
    /** Database column CRIMINAL_CASE DBType(VARCHAR), Length(50,true), Default(None) */
    val criminalCase: Column[Option[String]] = column[Option[String]]("CRIMINAL_CASE", O.Length(50,varying=true), O.Default(None))
    /** Database column STATUS DBType(String), Default(UnderConstruction) */
    val status: Column[String] = column[String]("STATUS", O.Default("Open"))

    /** Database column CASE_TYPE DBType(String) */
    val caseType: Column[String] = column[String]("CASE_TYPE", O.Length(50,varying=true), O.Default("MPI"))

    /** Uniqueness Index over (internalSampleCode) (database name COURT_CASE_UNQ_INTERNAL_SAMPLE_CODE_INDEX_B) */
    val index1 = index("COURT_CASE_UNQ_INTERNAL_SAMPLE_CODE_INDEX_B", internalSampleCode, unique=true)
  }
  /** Collection-like TableQuery object for table CourtCase */
  lazy val CourtCase = new TableQuery(tag => new CourtCase(tag, Some("APP"), "COURT_CASE"))

  /** Entity class storing rows of table CaseType
   *
   *  @param id Database column ID DBType(VARCHAR), Length(50,true)
   *  @param name Database column NAME DBType(VARCHAR), Length(50,true)
   */
  case class CaseTypeRow(id: String, name: String)
  /** GetResult implicit for fetching CaseTypeRow objects using plain SQL queries */
  implicit def GetResultCaseTypeRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[CaseTypeRow] = GR{
    prs => import prs._
      CaseTypeRow.tupled((<<[String], <<[String]))
  }
  /** Table description of table CASE_TYPE. Objects of this class serve as prototypes for rows in queries. */
  class CaseType(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[CaseTypeRow](_tableTag, schema, tableName) {
    def * = (id, name) <> (CaseTypeRow.tupled, CaseTypeRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, name.?).shaped.<>({r=>import r._; _1.map(_=> CaseTypeRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(VARCHAR), PrimaryKey, Length(50,true) */
    val id: Column[String] = column[String]("ID", O.PrimaryKey, O.Length(50,varying=true))
    /** Database column NAME DBType(VARCHAR), PrimaryKey, Length(50,true) */
    val name: Column[String] = column[String]("NAME", O.Length(50,varying=true))

  }
  /** Collection-like TableQuery object for table CaseType */
  lazy val CaseType = new TableQuery(tag => new CaseType(tag, Some("APP"), "CASE_TYPE"))


  /** Entity class storing rows of table CourtCaseFiliationData
   *
   *  @param id Database column ID DBType(BIGINT), AutoInc, PrimaryKey
   *  @param courtCaseid Database column ID_COURT_CASE DBType(BIGINT), FoeringKey
   *  @param firstname Database column FIRSTNAME DBType(VARCHAR), Length(100,true), Default(None)
   *  @param lastname Database column LASTNAME DBType(VARCHAR), Length(100,true), Default(None)
   *  @param sex Database column SEX DBType(VARCHAR), Length(50,true), Default(None)
   *  @param dateOfBirth Database column DATE_OF_BIRTH DBType(DATE), Default(None)
   *  @param dateOfBirthFrom Database column DATE_OF_BIRTH_FROM DBType(DATE), Default(None)
   *  @param dateOfBirthTo Database column DATE_OF_BIRTH_TO DBType(DATE), Default(None)
   *  @param dateOfMissing Database column DATE_OF_MISSING DBType(DATE), Default(None)
   *  @param nationality Database column NATIONALITY DBType(VARCHAR), Length(50,true), Default(None)
   *  @param identification Database column IDENTIFICATION DBType(VARCHAR), Length(50,true), Default(None)
   *  @param height Database column HEIGHT DBType(VARCHAR), Length(50,true), Default(None)
   *  @param weight Database column WEIGHT DBType(VARCHAR), Length(50,true), Default(None)
   *  @param haircolor Database column HAIRCOLOR DBType(VARCHAR), Length(50,true), Default(None)
   *  @param skincolor Database column SKINCOLOR DBType(VARCHAR), Length(50,true), Default(None)
   *  @param clothing Database column CLOTHING DBType(VARCHAR), Length(50,true), Default(None)
   *  @param alias Database column ALIAS DBType(VARCHAR), Length(50), Default(None)
   *  @param particularities Database column PARTICULARITIES DBType(VARCHAR), Length(50), Default(None)
   *  */
  case class CourtCaseFiliationDataRow(id: Long, courtCaseId: Long, firstname: Option[String] = None, lastname: Option[String] = None, sex: Option[String] = None, dateOfBirth: Option[java.sql.Date] = None, dateOfBirthFrom: Option[java.sql.Date] = None, dateOfBirthTo: Option[java.sql.Date] = None, dateOfMissing: Option[java.sql.Date] = None, nationality: Option[String] = None, identification: Option[String] = None, height: Option[String] = None, weight: Option[String] = None, haircolor: Option[String] = None, skincolor: Option[String] = None, clothing: Option[String] = None, alias : String, particularities: Option[String] = None)
  /** GetResult implicit for fetching CourtCaseRow objects using plain SQL queries */
  implicit def GetResultCourtCaseFiliationDataRow(implicit e0: GR[Long], e1: GR[Option[String]], e2: GR[String], e3: GR[Boolean], e4: GR[Option[java.sql.Date]]): GR[CourtCaseFiliationDataRow] = GR{
    prs => import prs._
      CourtCaseFiliationDataRow.tupled((<<[Long], <<[Long], <<?[String], <<?[String], <<?[String], <<?[java.sql.Date], <<?[java.sql.Date], <<?[java.sql.Date], <<?[java.sql.Date], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<[String] ,<<?[String] ))
  }
  /** Table description of table COURT_CASE_FILIATION_DATA. Objects of this class serve as prototypes for rows in queries. */
  class CourtCaseFiliationData(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[CourtCaseFiliationDataRow](_tableTag, schema, tableName) {
    def * = (id, courtCaseId, firstname, lastname, sex, dateOfBirth, dateOfBirthFrom, dateOfBirthTo, dateOfMissing, nationality, identification, height, weight, haircolor, skincolor, clothing, alias, particularities) <> (CourtCaseFiliationDataRow.tupled, CourtCaseFiliationDataRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, courtCaseId.?, firstname, lastname, sex, dateOfBirth, dateOfBirthFrom, dateOfBirthTo, dateOfMissing, nationality, identification, height, weight, haircolor, skincolor, clothing, alias, particularities).shaped.<>({r=>import r._; _1.map(_=> CourtCaseFiliationDataRow.tupled((_1.get, _2.get, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14, _15, _16, _17, _18)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(BIGINT), AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column ID_COURT_CASE DBType(BIGINT), AutoInc, PrimaryKey */
    val courtCaseId: Column[Long] = column[Long]("ID_COURT_CASE")
    /** Database column FIRSTNAME DBType(VARCHAR), Length(100,true), Default(None) */
    val firstname: Column[Option[String]] = column[Option[String]]("FIRSTNAME", O.Length(100,varying=true), O.Default(None))
    /** Database column LASTNAME DBType(VARCHAR), Length(100,true), Default(None) */
    val lastname: Column[Option[String]] = column[Option[String]]("LASTNAME", O.Length(100,varying=true), O.Default(None))
    /** Database column SEX DBType(VARCHAR), Length(50,true), Default(None) */
    val sex: Column[Option[String]] = column[Option[String]]("SEX", O.Length(50,varying=true), O.Default(None))
    /** Database column DATE_OF_BIRTH DBType(DATE), Default(None) */
    val dateOfBirth: Column[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_OF_BIRTH", O.Default(None))
    /** Database column DATE_OF_BIRTH DBType(DATE), Default(None) */
    val dateOfBirthFrom: Column[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_OF_BIRTH_FROM", O.Default(None))
    /** Database column DATE_OF_BIRTH DBType(DATE), Default(None) */
    val dateOfBirthTo: Column[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_OF_BIRTH_TO", O.Default(None))
    /** Database column DATE_OF_BIRTH DBType(DATE), Default(None) */
    val dateOfMissing: Column[Option[java.sql.Date]] = column[Option[java.sql.Date]]("DATE_OF_MISSING", O.Default(None))
    /** Database column NATIONALITY DBType(VARCHAR), Length(50,true), Default(None) */
    val nationality: Column[Option[String]] = column[Option[String]]("NATIONALITY", O.Length(50,varying=true), O.Default(None))
    /** Database column IDENTIFICATION DBType(VARCHAR), Length(50,true), Default(None) */
    val identification: Column[Option[String]] = column[Option[String]]("IDENTIFICATION", O.Length(50,varying=true), O.Default(None))
    /** Database column HEIGHT DBType(VARCHAR), Length(50,true), Default(None) */
    val height: Column[Option[String]] = column[Option[String]]("HEIGHT", O.Length(50,varying=true), O.Default(None))
    /** Database column WEIGHT DBType(VARCHAR), Length(50,true), Default(None) */
    val weight: Column[Option[String]] = column[Option[String]]("WEIGHT", O.Length(50,varying=true), O.Default(None))
    /** Database column HAIRCOLOR DBType(VARCHAR), Length(50,true), Default(None) */
    val haircolor: Column[Option[String]] = column[Option[String]]("HAIRCOLOR", O.Length(50,varying=true), O.Default(None))
    /** Database column SKINCOLOR DBType(VARCHAR), Length(50,true), Default(None) */
    val skincolor: Column[Option[String]] = column[Option[String]]("SKINCOLOR", O.Length(50,varying=true), O.Default(None))
    /** Database column CLOTHING DBType(VARCHAR), Length(50,true), Default(None) */
    val clothing: Column[Option[String]] = column[Option[String]]("CLOTHING", O.Length(50,varying=true), O.Default(None))
    /** Database column ALIAS DBType(VARCHAR), Length(50), Default(None) */
    val alias: Column[String] = column[String]("ALIAS", O.Length(50,varying=true))
    /** Database column PARTICULARITIES DBType(VARCHAR), Length(50,true), Default(None) */
    val particularities: Column[Option[String]] = column[Option[String]]("PARTICULARITIES", O.Length(50,varying=true), O.Default(None))


    /** Foreign key referencing CourtCase (database name COURT_CASE_DATA_FILIATION_FK) */
    lazy val courtCaseIdFk = foreignKey("COURT_CASE_DATA_FILIATION_FK", courtCaseId, CourtCase)(r => r.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)

  }
  /** Collection-like TableQuery object for table CourtCaseFiliationData */
  lazy val CourtCaseFiliationData = new TableQuery(tag => new CourtCaseFiliationData(tag, Some("APP"), "COURT_CASE_DATA_FILIATION"))


  /** Entity class storing rows of table PEDIGREE
   *
   *  @param id Database column ID DBType(BIGINT), AutoInc, PrimaryKey
   *  @param courtCaseid Database column ID_COURT_CASE DBType(BIGINT), FoeringKey
   *  @param name Database column NAME DBType(VARCHAR), Length(100,true), not null
   *  @param creationDate Database column CREATION_DATE DBType(timestamp), not null
   *  @param status Database column STATUS DBType(String), Default(UnderConstruction
   *  @param assignee Database column ASSIGNEE DBType(VARCHAR), Length(50,true)
   */
  case class PedigreeRow(id: Long, courtCaseId: Long, name: String, creationDate: java.sql.Date, status: String = "UnderConstruction", assignee : String,consistencyRun:Boolean=false)
  /** GetResult implicit for fetching PedigreeRow objects using plain SQL queries */
  implicit def GetResultPedigreRow(implicit e0: GR[Long], e1: GR[Option[String]], e2: GR[String], e3: GR[Boolean], e4: GR[Option[java.sql.Date]]): GR[PedigreeRow] = GR{
    prs => import prs._
      PedigreeRow.tupled((<<[Long], <<[Long], <<[String], <<[java.sql.Date], <<[String], <<[String], <<[Boolean]))
  }
  /** Table description of table PEDIGREE. Objects of this class serve as prototypes for rows in queries. */
  class Pedigree(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[PedigreeRow](_tableTag, schema, tableName) {
    def * = (id, courtCaseId, name, creationDate, status, assignee,consistencyRun) <> (PedigreeRow.tupled, PedigreeRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, courtCaseId.?, name, creationDate, status, assignee,consistencyRun.?).shaped.<>({r=>import r._; _1.map(_=> PedigreeRow.tupled((_1.get, _2.get, _3, _4, _5, _6,_7.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(BIGINT), AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column ID_COURT_CASE DBType(BIGINT), AutoInc, PrimaryKey */
    val courtCaseId: Column[Long] = column[Long]("ID_COURT_CASE")
    /** Database column FIRSTNAME DBType(VARCHAR), Length(100,true), Default(None) */
    val name: Column[String] = column[String]("NAME", O.Length(100,varying=true))
    /** Database column DATE_OF_BIRTH DBType(DATE), Default(None) */
    val creationDate: Column[java.sql.Date] = column[java.sql.Date]("CREATION_DATE")
    /** Database column STATUS DBType(String), Default(UnderConstruction) */
    val status: Column[String] = column[String]("STATUS", O.Default("UnderConstruction"))
    /** Database column ASSIGNEE DBType(VARCHAR), Length(50,true) */
    val assignee: Column[String] = column[String]("ASSIGNEE", O.Length(50,varying=true))

    val consistencyRun: Column[Boolean] = column[Boolean]("CONSISTENCY_RUN")

    /** Foreign key referencing CourtCase (database name PEDIGREE_COURT_CASE_FK) */
    lazy val courtCaseIdFk = foreignKey("PEDIGREE_COURT_CASE_FK", courtCaseId, CourtCase)(r => r.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)

  }
  /** Collection-like TableQuery object for table Pedigree */
  lazy val Pedigree = new TableQuery(tag => new Pedigree(tag, Some("APP"), "PEDIGREE"))


  /** Entity class storing rows of table CrimeInvolved
   *
   *  @param id Database column ID DBType(VARCHAR), PrimaryKey, Length(50,true)
   *  @param crimeType Database column CRIME_TYPE DBType(VARCHAR), Length(50,true)
   *  @param name Database column NAME DBType(VARCHAR), Length(100,true)
   *  @param description Database column DESCRIPTION DBType(VARCHAR), Length(1024,true), Default(None) */
  case class CrimeInvolvedRow(id: String, crimeType: String, name: String, description: Option[String] = None)
  /** GetResult implicit for fetching CrimeInvolvedRow objects using plain SQL queries */
  implicit def GetResultCrimeInvolvedRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[CrimeInvolvedRow] = GR{
    prs => import prs._
      CrimeInvolvedRow.tupled((<<[String], <<[String], <<[String], <<?[String]))
  }
  /** Table description of table CRIME_INVOLVED. Objects of this class serve as prototypes for rows in queries. */
  class CrimeInvolved(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[CrimeInvolvedRow](_tableTag, schema, tableName) {
    def * = (id, crimeType, name, description) <> (CrimeInvolvedRow.tupled, CrimeInvolvedRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, crimeType.?, name.?, description).shaped.<>({r=>import r._; _1.map(_=> CrimeInvolvedRow.tupled((_1.get, _2.get, _3.get, _4)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(VARCHAR), PrimaryKey, Length(50,true) */
    val id: Column[String] = column[String]("ID", O.PrimaryKey, O.Length(50,varying=true))
    /** Database column CRIME_TYPE DBType(VARCHAR), Length(50,true) */
    val crimeType: Column[String] = column[String]("CRIME_TYPE", O.Length(50,varying=true))
    /** Database column NAME DBType(VARCHAR), Length(100,true) */
    val name: Column[String] = column[String]("NAME", O.Length(100,varying=true))
    /** Database column DESCRIPTION DBType(VARCHAR), Length(1024,true), Default(None) */
    val description: Column[Option[String]] = column[Option[String]]("DESCRIPTION", O.Length(1024,varying=true), O.Default(None))

    /** Foreign key referencing CrimeType (database name CRIME_INVOLVED_TYPE_FKEY) */
    lazy val crimeTypeFk = foreignKey("CRIME_INVOLVED_TYPE_FKEY", crimeType, CrimeType)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table CrimeInvolved */
  lazy val CrimeInvolved = new TableQuery(tag => new CrimeInvolved(tag, Some("APP"), "CRIME_INVOLVED"))

  /** Entity class storing rows of table CrimeType
   *
   *  @param id Database column ID DBType(VARCHAR), PrimaryKey, Length(50,true)
   *  @param name Database column NAME DBType(VARCHAR), Length(100,true)
   *  @param description Database column DESCRIPTION DBType(VARCHAR), Length(1024,true), Default(None) */
  case class CrimeTypeRow(id: String, name: String, description: Option[String] = None)
  /** GetResult implicit for fetching CrimeTypeRow objects using plain SQL queries */
  implicit def GetResultCrimeTypeRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[CrimeTypeRow] = GR{
    prs => import prs._
      CrimeTypeRow.tupled((<<[String], <<[String], <<?[String]))
  }
  /** Table description of table CRIME_TYPE. Objects of this class serve as prototypes for rows in queries. */
  class CrimeType(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[CrimeTypeRow](_tableTag, schema, tableName) {
    def * = (id, name, description) <> (CrimeTypeRow.tupled, CrimeTypeRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, name.?, description).shaped.<>({r=>import r._; _1.map(_=> CrimeTypeRow.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(VARCHAR), PrimaryKey, Length(50,true) */
    val id: Column[String] = column[String]("ID", O.PrimaryKey, O.Length(50,varying=true))
    /** Database column NAME DBType(VARCHAR), Length(100,true) */
    val name: Column[String] = column[String]("NAME", O.Length(100,varying=true))
    /** Database column DESCRIPTION DBType(VARCHAR), Length(1024,true), Default(None) */
    val description: Column[Option[String]] = column[Option[String]]("DESCRIPTION", O.Length(1024,varying=true), O.Default(None))

    /** Uniqueness Index over (name) (database name CRIME_TYPE_NAME_KEY_INDEX_7) */
    val index1 = index("CRIME_TYPE_NAME_KEY_INDEX_7", name, unique=true)
  }
  /** Collection-like TableQuery object for table CrimeType */
  lazy val CrimeType = new TableQuery(tag => new CrimeType(tag, Some("APP"), "CRIME_TYPE"))

  /** Entity class storing rows of table Geneticist
   *
   *  @param id Database column ID DBType(BIGINT), AutoInc, PrimaryKey
   *  @param laboratory Database column LABORATORY DBType(VARCHAR), Length(20,true)
   *  @param name Database column NAME DBType(VARCHAR), Length(100,true)
   *  @param lastname Database column LASTNAME DBType(VARCHAR), Length(100,true)
   *  @param email Database column EMAIL DBType(VARCHAR), Length(100,true)
   *  @param telephone Database column TELEPHONE DBType(VARCHAR), Length(100,true) */
  case class GeneticistRow(id: Long, laboratory: String, name: String, lastname: String, email: String, telephone: String)
  /** GetResult implicit for fetching GeneticistRow objects using plain SQL queries */
  implicit def GetResultGeneticistRow(implicit e0: GR[Long], e1: GR[String]): GR[GeneticistRow] = GR{
    prs => import prs._
      GeneticistRow.tupled((<<[Long], <<[String], <<[String], <<[String], <<[String], <<[String]))
  }
  /** Table description of table GENETICIST. Objects of this class serve as prototypes for rows in queries. */
  class Geneticist(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[GeneticistRow](_tableTag, schema, tableName) {
    def * = (id, laboratory, name, lastname, email, telephone) <> (GeneticistRow.tupled, GeneticistRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, laboratory.?, name.?, lastname.?, email.?, telephone.?).shaped.<>({r=>import r._; _1.map(_=> GeneticistRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(BIGINT), AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column LABORATORY DBType(VARCHAR), Length(20,true) */
    val laboratory: Column[String] = column[String]("LABORATORY", O.Length(20,varying=true))
    /** Database column NAME DBType(VARCHAR), Length(100,true) */
    val name: Column[String] = column[String]("NAME", O.Length(100,varying=true))
    /** Database column LASTNAME DBType(VARCHAR), Length(100,true) */
    val lastname: Column[String] = column[String]("LASTNAME", O.Length(100,varying=true))
    /** Database column EMAIL DBType(VARCHAR), Length(100,true) */
    val email: Column[String] = column[String]("EMAIL", O.Length(100,varying=true))
    /** Database column TELEPHONE DBType(VARCHAR), Length(100,true) */
    val telephone: Column[String] = column[String]("TELEPHONE", O.Length(100,varying=true))

    /** Foreign key referencing Laboratory (database name GENTICIST_LAB_FK) */
    lazy val laboratoryFk = foreignKey("GENTICIST_LAB_FK", laboratory, Laboratory)(r => r.codeName, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)

    /** Uniqueness Index over (laboratory,name,lastname) (database name GENETICIST_UNIQUE_INDEX_3) */
    val index1 = index("GENETICIST_UNIQUE_INDEX_3", (laboratory, name, lastname), unique=true)
  }
  /** Collection-like TableQuery object for table Geneticist */
  lazy val Geneticist = new TableQuery(tag => new Geneticist(tag, Some("APP"), "GENETICIST"))

  /** Entity class storing rows of table Group
   *
   *  @param id Database column ID DBType(VARCHAR), PrimaryKey, Length(50,true)
   *  @param name Database column NAME DBType(VARCHAR), Length(100,true)
   *  @param description Database column DESCRIPTION DBType(VARCHAR), Length(1024,true), Default(None) */
  case class GroupRow(id: String, name: String, description: Option[String] = None)
  /** GetResult implicit for fetching GroupRow objects using plain SQL queries */
  implicit def GetResultGroupRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[GroupRow] = GR{
    prs => import prs._
      GroupRow.tupled((<<[String], <<[String], <<?[String]))
  }
  /** Table description of table GROUP. Objects of this class serve as prototypes for rows in queries. */
  class Group(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[GroupRow](_tableTag, schema, tableName) {
    def * = (id, name, description) <> (GroupRow.tupled, GroupRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, name.?, description).shaped.<>({r=>import r._; _1.map(_=> GroupRow.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(VARCHAR), PrimaryKey, Length(50,true) */
    val id: Column[String] = column[String]("ID", O.PrimaryKey, O.Length(50,varying=true))
    /** Database column NAME DBType(VARCHAR), Length(100,true) */
    val name: Column[String] = column[String]("NAME", O.Length(100,varying=true))
    /** Database column DESCRIPTION DBType(VARCHAR), Length(1024,true), Default(None) */
    val description: Column[Option[String]] = column[Option[String]]("DESCRIPTION", O.Length(1024,varying=true), O.Default(None))

    /** Uniqueness Index over (name) (database name GROUP_NAME_KEY_INDEX_4) */
    val index1 = index("GROUP_NAME_KEY_INDEX_4", name, unique=true)
  }
  /** Collection-like TableQuery object for table Group */
  lazy val Group = new TableQuery(tag => new Group(tag, Some("APP"), "GROUP"))

  /** Entity class storing rows of table Laboratory
   *
   *  @param codeName Database column CODE_NAME DBType(VARCHAR), PrimaryKey, Length(20,true)
   *  @param name Database column NAME DBType(VARCHAR), Length(100,true)
   *  @param country Database column COUNTRY DBType(VARCHAR), Length(2,true)
   *  @param province Database column PROVINCE DBType(VARCHAR), Length(1,true)
   *  @param address Database column ADDRESS DBType(VARCHAR), Length(100,true)
   *  @param telephone Database column TELEPHONE DBType(VARCHAR), Length(50,true)
   *  @param contactEmail Database column CONTACT_EMAIL DBType(VARCHAR), Length(100,true)
   *  @param dropIn Database column DROP_IN DBType(DOUBLE)
   *  @param dropOut Database column DROP_OUT DBType(DOUBLE)  */
  case class LaboratoryRow(codeName: String, name: String, country: String, province: String, address: String, telephone: String, contactEmail: String, dropIn: Double, dropOut: Double)
  /** GetResult implicit for fetching LaboratoryRow objects using plain SQL queries */
  implicit def GetResultLaboratoryRow(implicit e0: GR[String]): GR[LaboratoryRow] = GR{
    prs => import prs._
      LaboratoryRow.tupled((<<[String], <<[String], <<[String], <<[String], <<[String], <<[String], <<[String], <<[Double], <<[Double]))
  }
  /** Table description of table LABORATORY. Objects of this class serve as prototypes for rows in queries. */
  class Laboratory(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[LaboratoryRow](_tableTag, schema, tableName) {
    def * = (codeName, name, country, province, address, telephone, contactEmail, dropIn, dropOut) <> (LaboratoryRow.tupled, LaboratoryRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (codeName.?, name.?, country.?, province.?, address.?, telephone.?, contactEmail.?, dropIn.?, dropOut.?).shaped.<>({r=>import r._; _1.map(_=> LaboratoryRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get, _9.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column CODE_NAME DBType(VARCHAR), PrimaryKey, Length(20,true) */
    val codeName: Column[String] = column[String]("CODE_NAME", O.PrimaryKey, O.Length(20,varying=true))
    /** Database column NAME DBType(VARCHAR), Length(100,true) */
    val name: Column[String] = column[String]("NAME", O.Length(100,varying=true))
    /** Database column COUNTRY DBType(VARCHAR), Length(2,true) */
    val country: Column[String] = column[String]("COUNTRY", O.Length(2,varying=true))
    /** Database column PROVINCE DBType(VARCHAR), Length(1,true) */
    val province: Column[String] = column[String]("PROVINCE", O.Length(1,varying=true))
    /** Database column ADDRESS DBType(VARCHAR), Length(100,true) */
    val address: Column[String] = column[String]("ADDRESS", O.Length(100,varying=true))
    /** Database column TELEPHONE DBType(VARCHAR), Length(50,true) */
    val telephone: Column[String] = column[String]("TELEPHONE", O.Length(50,varying=true))
    /** Database column CONTACT_EMAIL DBType(VARCHAR), Length(100,true) */
    val contactEmail: Column[String] = column[String]("CONTACT_EMAIL", O.Length(100,varying=true))
    /** Database column DROP_IN DBType(DOUBLE) */
    val dropIn: Column[Double] = column[Double]("DROP_IN")
    /** Database column DROP_OUT DBType(DOUBLE) */
    val dropOut: Column[Double] = column[Double]("DROP_OUT")

    /** Foreign key referencing Country (database name COUNTRY_FK) */
    lazy val countryFk = foreignKey("COUNTRY_FK", country, Country)(r => r.code, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing Province (database name PROVINCE_FK) */
    lazy val provinceFk = foreignKey("PROVINCE_FK", province, Province)(r => r.code, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table Laboratory */
  lazy val Laboratory = new TableQuery(tag => new Laboratory(tag, Some("APP"), "LABORATORY"))

  /** Entity class storing rows of table Locus
   *
   *  @param id Database column ID DBType(VARCHAR), PrimaryKey, Length(50,true)
   *  @param name Database column NAME DBType(VARCHAR), Length(100,true)
   *  @param chromosome Database column CHROMOSOME DBType(VARCHAR), Length(2,true), Default(None)
   *  @param minimumAllelesQty Database column MINIMUM_ALLELES_QTY DBType(INTEGER), Default(2)
   *  @param maximumAllelesQty Database column MAXIMUM_ALLELES_QTY DBType(INTEGER), Default(2)
   *  @param type Database column TYPE DBType(SMALLINT) */
  case class LocusRow(id: String, name: String, chromosome: Option[String] = None, minimumAllelesQty: Int = 2, maximumAllelesQty: Int = 2, `type`: Int,required: Boolean=true,minAlleleValue:Option[scala.math.BigDecimal]=None,maxAlleleValue:Option[scala.math.BigDecimal]=None)
  /** GetResult implicit for fetching LocusRow objects using plain SQL queries */
  implicit def GetResultLocusRow(implicit e0: GR[String], e1: GR[Option[String]], e2: GR[Int], e3: GR[Boolean]): GR[LocusRow] = GR{
    prs => import prs._
      LocusRow.tupled((<<[String], <<[String], <<?[String], <<[Int], <<[Int], <<[Int],<<[Boolean], <<?[scala.math.BigDecimal], <<?[scala.math.BigDecimal]))
  }
  /** Table description of table LOCUS. Objects of this class serve as prototypes for rows in queries. */
  class Locus(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[LocusRow](_tableTag, schema, tableName) {
    def * = (id, name, chromosome, minimumAllelesQty, maximumAllelesQty, `type`,required,minAlleleValue,maxAlleleValue) <> (LocusRow.tupled, LocusRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, name.?, chromosome, minimumAllelesQty.?, maximumAllelesQty.?, `type`.?,required.?,minAlleleValue,maxAlleleValue).shaped.<>({r=>import r._; _1.map(_=> LocusRow.tupled((_1.get, _2.get, _3, _4.get, _5.get, _6.get,_7.get,_8,_9)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(VARCHAR), PrimaryKey, Length(50,true) */
    val id: Column[String] = column[String]("ID", O.PrimaryKey, O.Length(50,varying=true))
    /** Database column NAME DBType(VARCHAR), Length(100,true) */
    val name: Column[String] = column[String]("NAME", O.Length(100,varying=true))
    /** Database column CHROMOSOME DBType(VARCHAR), Length(2,true), Default(None) */
    val chromosome: Column[Option[String]] = column[Option[String]]("CHROMOSOME", O.Length(2,varying=true), O.Default(None))
    /** Database column MINIMUM_ALLELES_QTY DBType(INTEGER), Default(2) */
    val minimumAllelesQty: Column[Int] = column[Int]("MINIMUM_ALLELES_QTY", O.Default(2))
    /** Database column MAXIMUM_ALLELES_QTY DBType(INTEGER), Default(2) */
    val maximumAllelesQty: Column[Int] = column[Int]("MAXIMUM_ALLELES_QTY", O.Default(2))
    /** Database column TYPE DBType(SMALLINT)
     *  NOTE: The name was escaped because it collided with a Scala keyword. */
    val `type`: Column[Int] = column[Int]("TYPE")
    val required: Column[Boolean] = column[Boolean]("REQUIRED", O.Default(false))
    val minAlleleValue: Column[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("MIN_ALLELE_VALUE", O.Default(None))
    val maxAlleleValue: Column[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("MAX_ALLELE_VALUE", O.Default(None))

  }
  /** Collection-like TableQuery object for table Locus */
  lazy val Locus = new TableQuery(tag => new Locus(tag, Some("APP"), "LOCUS"))

  /** Entity class storing rows of table LocusAlias
   *
   *  @param alias Database column ALIAS DBType(VARCHAR), PrimaryKey, Length(100,true)
   *  @param marker Database column MARKER DBType(VARCHAR), Length(50,true) */
  case class LocusAliasRow(alias: String, marker: String)
  /** GetResult implicit for fetching LocusAliasRow objects using plain SQL queries */
  implicit def GetResultLocusAliasRow(implicit e0: GR[String]): GR[LocusAliasRow] = GR{
    prs => import prs._
      LocusAliasRow.tupled((<<[String], <<[String]))
  }
  /** Table description of table LOCUS_ALIAS. Objects of this class serve as prototypes for rows in queries. */
  class LocusAlias(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[LocusAliasRow](_tableTag, schema, tableName) {
    def * = (alias, marker) <> (LocusAliasRow.tupled, LocusAliasRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (alias.?, marker.?).shaped.<>({r=>import r._; _1.map(_=> LocusAliasRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ALIAS DBType(VARCHAR), PrimaryKey, Length(100,true) */
    val alias: Column[String] = column[String]("ALIAS", O.PrimaryKey, O.Length(100,varying=true))
    /** Database column MARKER DBType(VARCHAR), Length(50,true) */
    val marker: Column[String] = column[String]("MARKER", O.Length(50,varying=true))

    /** Foreign key referencing Locus (database name LOCUS_ALIAS_FK) */
    lazy val locusFk = foreignKey("LOCUS_ALIAS_FK", marker, Locus)(r => r.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table LocusAlias */
  lazy val LocusAlias = new TableQuery(tag => new LocusAlias(tag, Some("APP"), "LOCUS_ALIAS"))

  /** Entity class storing rows of table MitochondrialRcrs
   *
   *  @param position Database column POSITION DBType(INTEGER), PrimaryKey
   *  @param base Database column BASE DBType(VARCHAR), Length(1,true) */
  case class MitochondrialRcrsRow(position: Int, base: String)
  /** GetResult implicit for fetching MitochondrialRcrsRow objects using plain SQL queries */
  implicit def GetResultMitochondrialRcrsRow(implicit e0: GR[Int], e1: GR[String]): GR[MitochondrialRcrsRow] = GR{
    prs => import prs._
      MitochondrialRcrsRow.tupled((<<[Int], <<[String]))
  }
  /** Table description of table MITOCHONDRIAL_RCRS. Objects of this class serve as prototypes for rows in queries. */
  class MitochondrialRcrs(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[MitochondrialRcrsRow](_tableTag, schema, tableName) {
    def * = (position, base) <> (MitochondrialRcrsRow.tupled, MitochondrialRcrsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (position.?, base.?).shaped.<>({r=>import r._; _1.map(_=> MitochondrialRcrsRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column POSITION DBType(INTEGER), PrimaryKey */
    val position: Column[Int] = column[Int]("POSITION", O.PrimaryKey)
    /** Database column BASE DBType(VARCHAR), Length(1,true) */
    val base: Column[String] = column[String]("BASE", O.Length(1,varying=true))
  }
  /** Collection-like TableQuery object for table MitochondrialRcrs */
  lazy val MitochondrialRcrs = new TableQuery(tag => new MitochondrialRcrs(tag, Some("APP"), "MITOCHONDRIAL_RCRS"))

  /** Entity class storing rows of table OperationLogLot
   *
   *  @param id Database column ID DBType(BIGINT), AutoInc, PrimaryKey
   *  @param keyZero Database column KEY_ZERO DBType(VARCHAR), Length(200,true)
   *  @param initTime Database column INIT_TIME DBType(TIMESTAMP) */
  case class OperationLogLotRow(id: Long, keyZero: String, initTime: java.sql.Timestamp)
  /** GetResult implicit for fetching OperationLogLotRow objects using plain SQL queries */
  implicit def GetResultOperationLogLotRow(implicit e0: GR[Long], e1: GR[String], e2: GR[java.sql.Timestamp]): GR[OperationLogLotRow] = GR{
    prs => import prs._
      OperationLogLotRow.tupled((<<[Long], <<[String], <<[java.sql.Timestamp]))
  }
  /** Table description of table OPERATION_LOG_LOT. Objects of this class serve as prototypes for rows in queries. */
  class OperationLogLot(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[OperationLogLotRow](_tableTag, schema, tableName) {
    def * = (id, keyZero, initTime) <> (OperationLogLotRow.tupled, OperationLogLotRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, keyZero.?, initTime.?).shaped.<>({r=>import r._; _1.map(_=> OperationLogLotRow.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(BIGINT), AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column KEY_ZERO DBType(VARCHAR), Length(200,true) */
    val keyZero: Column[String] = column[String]("KEY_ZERO", O.Length(200,varying=true))
    /** Database column INIT_TIME DBType(TIMESTAMP) */
    val initTime: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("INIT_TIME")
  }
  /** Collection-like TableQuery object for table OperationLogLot */
  lazy val OperationLogLot = new TableQuery(tag => new OperationLogLot(tag, Some("LOG_DB"), "OPERATION_LOG_LOT"))

  /** Entity class storing rows of table OperationLogRecord
   *
   *  @param id Database column ID DBType(BIGINT), AutoInc, PrimaryKey
   *  @param userId Database column USER_ID DBType(VARCHAR), Length(50,true)
   *  @param otp Database column OTP DBType(VARCHAR), Length(50,true), Default(None)
   *  @param timestamp Database column TIMESTAMP DBType(TIMESTAMP)
   *  @param method Database column METHOD DBType(VARCHAR), Length(50,true)
   *  @param path Database column PATH DBType(VARCHAR), Length(1024,true)
   *  @param action Database column ACTION DBType(VARCHAR), Length(512,true)
   *  @param buildNo Database column BUILD_NO DBType(VARCHAR), Length(150,true)
   *  @param result Database column RESULT DBType(VARCHAR), Length(150,true)
   *  @param status Database column STATUS DBType(INT)
   *  @param signature Database column SIGNATURE DBType(VARCHAR), Length(8192,true)
   *  @param lot Database column LOT DBType(BIGINT)
   *  @param description Database column DESCRIPTION DBType(VARCHAR), Length(1024,true) */
  case class OperationLogRecordRow(id: Long, userId: String, otp: Option[String] = None, timestamp: java.sql.Timestamp, method: String, path: String, action: String, buildNo: String, result: Option[String], status: Int, signature: String, lot: Long, description: String)
  /** GetResult implicit for fetching OperationLogRecordRow objects using plain SQL queries */
  implicit def GetResultOperationLogRecordRow(implicit e0: GR[Long], e1: GR[String], e2: GR[Option[String]], e3: GR[java.sql.Timestamp]): GR[OperationLogRecordRow] = GR{
    prs => import prs._
      OperationLogRecordRow.tupled((<<[Long], <<[String], <<?[String], <<[java.sql.Timestamp], <<[String], <<[String], <<[String], <<[String], <<?[String], <<[Int], <<[String], <<[Long], <<[String]))
  }
  /** Table description of table OPERATION_LOG_RECORD. Objects of this class serve as prototypes for rows in queries. */
  class OperationLogRecord(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[OperationLogRecordRow](_tableTag, schema, tableName) {
    def * = (id, userId, otp, timestamp, method, path, action, buildNo, result, status, signature, lot, description) <> (OperationLogRecordRow.tupled, OperationLogRecordRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, userId.?, otp, timestamp.?, method.?, path.?, action.?, buildNo.?, result, status.?, signature.?, lot.?, description.?).shaped.<>({r=>import r._; _1.map(_=> OperationLogRecordRow.tupled((_1.get, _2.get, _3, _4.get, _5.get, _6.get, _7.get, _8.get, _9, _10.get, _11.get, _12.get, _13.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(BIGINT), AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column USER_ID DBType(VARCHAR), Length(50,true) */
    val userId: Column[String] = column[String]("USER_ID", O.Length(50,varying=true))
    /** Database column OTP DBType(VARCHAR), Length(50,true), Default(None) */
    val otp: Column[Option[String]] = column[Option[String]]("OTP", O.Length(50,varying=true), O.Default(None))
    /** Database column TIMESTAMP DBType(TIMESTAMP) */
    val timestamp: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("TIMESTAMP")
    /** Database column METHOD DBType(VARCHAR), Length(50,true) */
    val method: Column[String] = column[String]("METHOD", O.Length(50,varying=true))
    /** Database column PATH DBType(VARCHAR), Length(1024,true) */
    val path: Column[String] = column[String]("PATH", O.Length(1024,varying=true))
    /** Database column ACTION DBType(VARCHAR), Length(512,true) */
    val action: Column[String] = column[String]("ACTION", O.Length(512,varying=true))
    /** Database column BUILD_NO DBType(VARCHAR), Length(150,true) */
    val buildNo: Column[String] = column[String]("BUILD_NO", O.Length(150,varying=true))
    /** Database column RESULT DBType(VARCHAR), Length(150,true) */
    val result: Column[Option[String]] = column[Option[String]]("RESULT", O.Length(150,varying=true))
    /** Database column STATUS STATUS DBType(INT) */
    val status: Column[Int] = column[Int]("STATUS")
    /** Database column SIGNATURE DBType(VARCHAR), Length(8192,true) */
    val signature: Column[String] = column[String]("SIGNATURE", O.Length(8192,varying=true))
    /** Database column LOT DBType(BIGINT) */
    val lot: Column[Long] = column[Long]("LOT")
    /** Database column DESCRIPTION DBType(VARCHAR), Length(1024,true) */
    val description: Column[String] = column[String]("DESCRIPTION", O.Length(1024,varying=true))

    /** Foreign key referencing OperationLogLot (database name OPERATION_LOG_RECORD_FK) */
    lazy val operationLogLotFk = foreignKey("OPERATION_LOG_RECORD_FK", lot, OperationLogLot)(r => r.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table OperationLogRecord */
  lazy val OperationLogRecord = new TableQuery(tag => new OperationLogRecord(tag, Some("LOG_DB"), "OPERATION_LOG_RECORD"))

  /** Entity class storing rows of table PopulationBaseFrequency
   *
   *  @param id Database column ID DBType(BIGINT), AutoInc, PrimaryKey
   *  @param baseName Database column BASE_NAME DBType(BIGINT)
   *  @param marker Database column MARKER DBType(VARCHAR), Length(50,true)
   *  @param allele Database column ALLELE DBType(DOUBLE)
   *  @param frequency Database column FREQUENCY DBType(DECIMAL)
   *  */
  case class PopulationBaseFrequencyRow(id: Long, baseName: Long, marker: String, allele: Double, frequency: scala.math.BigDecimal)
  /** GetResult implicit for fetching PopulationBaseFrequencyRow objects using plain SQL queries */
  implicit def GetResultPopulationBaseFrequencyRow(implicit e0: GR[Long], e1: GR[String], e2: GR[Double], e3: GR[scala.math.BigDecimal]): GR[PopulationBaseFrequencyRow] = GR{
    prs => import prs._
      PopulationBaseFrequencyRow.tupled((<<[Long], <<[Long], <<[String], <<[Double], <<[scala.math.BigDecimal]))
  }
  /** Table description of table POPULATION_BASE_FREQUENCY. Objects of this class serve as prototypes for rows in queries. */
  class PopulationBaseFrequency(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[PopulationBaseFrequencyRow](_tableTag, schema, tableName) {
    def * = (id, baseName, marker, allele, frequency) <> (PopulationBaseFrequencyRow.tupled, PopulationBaseFrequencyRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, baseName.?, marker.?, allele.?, frequency.?).shaped.<>({r=>import r._; _1.map(_=> PopulationBaseFrequencyRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(BIGINT), AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column BASE_NAME DBType(BIGINT) */
    val baseName: Column[Long] = column[Long]("BASE_NAME")
    /** Database column MARKER DBType(VARCHAR), Length(50,true) */
    val marker: Column[String] = column[String]("MARKER", O.Length(50,varying=true))
    /** Database column ALLELE DBType(DOUBLE) */
    val allele: Column[Double] = column[Double]("ALLELE")
    /** Database column FREQUENCY DBType(DECIMAL) */
    val frequency: Column[scala.math.BigDecimal] = column[scala.math.BigDecimal]("FREQUENCY")

    /** Foreign key referencing PopulationBaseFrequencyName (database name POPULATION_BASE_FREQUENCY_FK) */
    lazy val populationBaseFrequencyNameFk = foreignKey("POPULATION_BASE_FREQUENCY_FK", baseName, PopulationBaseFrequencyName)(r => r.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table PopulationBaseFrequency */
  lazy val PopulationBaseFrequency = new TableQuery(tag => new PopulationBaseFrequency(tag, Some("APP"), "POPULATION_BASE_FREQUENCY"))

  /** Entity class storing rows of table PopulationBaseFrequencyName
   *
   *  @param id Database column ID DBType(BIGINT), AutoInc, PrimaryKey
   *  @param name Database column NAME DBType(VARCHAR), Length(50,true)
   *  @param theta Database column THETA DBType(DOUBLE)
   *  @param model Database column MODEL DBType(VARCHAR), Length(50,true)
   *  @param active Database column ACTIVE DBType(BOOLEAN)
   *  @param default Database column DEFAULT DBType(BOOLEAN) */
  case class PopulationBaseFrequencyNameRow(id: Long, name: String, theta: Double, model: String, active: Boolean, default: Boolean)
  /** GetResult implicit for fetching PopulationBaseFrequencyNameRow objects using plain SQL queries */
  implicit def GetResultPopulationBaseFrequencyNameRow(implicit e0: GR[Long], e1: GR[String], e2: GR[Double], e3: GR[Boolean]): GR[PopulationBaseFrequencyNameRow] = GR{
    prs => import prs._
      PopulationBaseFrequencyNameRow.tupled((<<[Long], <<[String], <<[Double], <<[String], <<[Boolean], <<[Boolean]))
  }
  /** Table description of table POPULATION_BASE_FREQUENCY_NAME. Objects of this class serve as prototypes for rows in queries. */
  class PopulationBaseFrequencyName(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[PopulationBaseFrequencyNameRow](_tableTag, schema, tableName) {
    def * = (id, name, theta, model, active, default) <> (PopulationBaseFrequencyNameRow.tupled, PopulationBaseFrequencyNameRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, name.?, theta.?, model.?, active.?, default.?).shaped.<>({r=>import r._; _1.map(_=> PopulationBaseFrequencyNameRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(BIGINT), AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column NAME DBType(VARCHAR), Length(50,true) */
    val name: Column[String] = column[String]("NAME", O.Length(50,varying=true))
    /** Database column THETA DBType(DOUBLE) */
    val theta: Column[Double] = column[Double]("THETA")
    /** Database column MODEL DBType(VARCHAR), Length(50,true) */
    val model: Column[String] = column[String]("MODEL", O.Length(50,varying=true))
    /** Database column ACTIVE DBType(BOOLEAN) */
    val active: Column[Boolean] = column[Boolean]("ACTIVE")
    /** Database column DEFAULT DBType(BOOLEAN) */
    val default: Column[Boolean] = column[Boolean]("DEFAULT")

    /** Uniqueness Index over (name) (database name POPULATION_BASE_FREQUENCY_UNQ_INDEX_4) */
    val index1 = index("POPULATION_BASE_FREQUENCY_UNQ_INDEX_4", name, unique=true)
  }
  /** Collection-like TableQuery object for table PopulationBaseFrequencyName */
  lazy val PopulationBaseFrequencyName = new TableQuery(tag => new PopulationBaseFrequencyName(tag, Some("APP"), "POPULATION_BASE_FREQUENCY_NAME"))

  /** Entity class storing rows of table ProfileData
   *
   *  @param id Database column ID DBType(BIGINT), AutoInc, PrimaryKey
   *  @param category Database column CATEGORY DBType(VARCHAR), Length(50,true)
   *  @param globalCode Database column GLOBAL_CODE DBType(VARCHAR), Length(100,true)
   *  @param internalCode Database column INTERNAL_CODE DBType(VARCHAR), Length(100,true)
   *  @param description Database column DESCRIPTION DBType(VARCHAR), Length(1024,true), Default(None)
   *  @param attorney Database column ATTORNEY DBType(VARCHAR), Length(100,true), Default(None)
   *  @param bioMaterialType Database column BIO_MATERIAL_TYPE DBType(VARCHAR), Length(50,true), Default(None)
   *  @param court Database column COURT DBType(VARCHAR), Length(100,true), Default(None)
   *  @param crimeInvolved Database column CRIME_INVOLVED DBType(VARCHAR), Length(50,true), Default(None)
   *  @param crimeType Database column CRIME_TYPE DBType(VARCHAR), Length(50,true), Default(None)
   *  @param criminalCase Database column CRIMINAL_CASE DBType(VARCHAR), Length(50,true), Default(None)
   *  @param internalSampleCode Database column INTERNAL_SAMPLE_CODE DBType(VARCHAR), Length(50,true)
   *  @param assignee Database column ASSIGNEE DBType(VARCHAR), Length(50,true)
   *  @param laboratory Database column LABORATORY DBType(VARCHAR), Length(50,true)
   *  @param profileExpirationDate Database column PROFILE_EXPIRATION_DATE DBType(DATE), Default(None)
   *  @param responsibleGeneticist Database column RESPONSIBLE_GENETICIST DBType(VARCHAR), Length(50,true), Default(None)
   *  @param sampleDate Database column SAMPLE_DATE DBType(DATE), Default(None)
   *  @param sampleEntryDate Database column SAMPLE_ENTRY_DATE DBType(DATE), Default(None)
   *  @param deleted Database column DELETED DBType(BOOLEAN), Default(false)
   *  @param deletedSolicitor Database column DELETED_SOLICITOR DBType(VARCHAR), Length(100,true), Default(None)
   *  @param deletedMotive Database column DELETED_MOTIVE DBType(VARCHAR), Length(8192,true), Default(None) */
  case class ProfileDataRow(id: Long, category: String, globalCode: String, internalCode: String, description: Option[String] = None, attorney: Option[String] = None, bioMaterialType: Option[String] = None, court: Option[String] = None, crimeInvolved: Option[String] = None, crimeType: Option[String] = None, criminalCase: Option[String] = None, internalSampleCode: String, assignee: String, laboratory: String, profileExpirationDate: Option[java.sql.Date] = None, responsibleGeneticist: Option[String] = None, sampleDate: Option[java.sql.Date] = None, sampleEntryDate: Option[java.sql.Date] = None, deleted: Boolean = false, deletedSolicitor: Option[String] = None, deletedMotive: Option[String] = None)
  /** GetResult implicit for fetching ProfileDataRow objects using plain SQL queries */
  implicit def GetResultProfileDataRow(implicit e0: GR[Long], e1: GR[String], e2: GR[Option[String]], e3: GR[Option[java.sql.Date]], e4: GR[Boolean]): GR[ProfileDataRow] = GR{
    prs => import prs._
      ProfileDataRow.tupled((<<[Long], <<[String], <<[String], <<[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String], <<[String], <<[String], <<[String], <<?[java.sql.Date], <<?[String], <<?[java.sql.Date], <<?[java.sql.Date], <<[Boolean], <<?[String], <<?[String]))
  }
  /** Table description of table PROFILE_DATA. Objects of this class serve as prototypes for rows in queries. */
  class ProfileData(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[ProfileDataRow](_tableTag, schema, tableName) {
    def * = (id, category, globalCode, internalCode, description, attorney, bioMaterialType, court, crimeInvolved, crimeType, criminalCase, internalSampleCode, assignee, laboratory, profileExpirationDate, responsibleGeneticist, sampleDate, sampleEntryDate, deleted, deletedSolicitor, deletedMotive) <> (ProfileDataRow.tupled, ProfileDataRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, category.?, globalCode.?, internalCode.?, description, attorney, bioMaterialType, court, crimeInvolved, crimeType, criminalCase, internalSampleCode.?, assignee.?, laboratory.?, profileExpirationDate, responsibleGeneticist, sampleDate, sampleEntryDate, deleted.?, deletedSolicitor, deletedMotive).shaped.<>({r=>import r._; _1.map(_=> ProfileDataRow.tupled((_1.get, _2.get, _3.get, _4.get, _5, _6, _7, _8, _9, _10, _11, _12.get, _13.get, _14.get, _15, _16, _17, _18, _19.get, _20, _21)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(BIGINT), AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column CATEGORY DBType(VARCHAR), Length(50,true) */
    val category: Column[String] = column[String]("CATEGORY", O.Length(50,varying=true))
    /** Database column GLOBAL_CODE DBType(VARCHAR), Length(100,true) */
    val globalCode: Column[String] = column[String]("GLOBAL_CODE", O.Length(100,varying=true))
    /** Database column INTERNAL_CODE DBType(VARCHAR), Length(100,true) */
    val internalCode: Column[String] = column[String]("INTERNAL_CODE", O.Length(100,varying=true))
    /** Database column DESCRIPTION DBType(VARCHAR), Length(1024,true), Default(None) */
    val description: Column[Option[String]] = column[Option[String]]("DESCRIPTION", O.Length(1024,varying=true), O.Default(None))
    /** Database column ATTORNEY DBType(VARCHAR), Length(100,true), Default(None) */
    val attorney: Column[Option[String]] = column[Option[String]]("ATTORNEY", O.Length(100,varying=true), O.Default(None))
    /** Database column BIO_MATERIAL_TYPE DBType(VARCHAR), Length(50,true), Default(None) */
    val bioMaterialType: Column[Option[String]] = column[Option[String]]("BIO_MATERIAL_TYPE", O.Length(50,varying=true), O.Default(None))
    /** Database column COURT DBType(VARCHAR), Length(100,true), Default(None) */
    val court: Column[Option[String]] = column[Option[String]]("COURT", O.Length(100,varying=true), O.Default(None))
    /** Database column CRIME_INVOLVED DBType(VARCHAR), Length(50,true), Default(None) */
    val crimeInvolved: Column[Option[String]] = column[Option[String]]("CRIME_INVOLVED", O.Length(50,varying=true), O.Default(None))
    /** Database column CRIME_TYPE DBType(VARCHAR), Length(50,true), Default(None) */
    val crimeType: Column[Option[String]] = column[Option[String]]("CRIME_TYPE", O.Length(50,varying=true), O.Default(None))
    /** Database column CRIMINAL_CASE DBType(VARCHAR), Length(50,true), Default(None) */
    val criminalCase: Column[Option[String]] = column[Option[String]]("CRIMINAL_CASE", O.Length(50,varying=true), O.Default(None))
    /** Database column INTERNAL_SAMPLE_CODE DBType(VARCHAR), Length(50,true) */
    val internalSampleCode: Column[String] = column[String]("INTERNAL_SAMPLE_CODE", O.Length(50,varying=true))
    /** Database column ASSIGNEE DBType(VARCHAR), Length(50,true) */
    val assignee: Column[String] = column[String]("ASSIGNEE", O.Length(50,varying=true))
    /** Database column LABORATORY DBType(VARCHAR), Length(50,true) */
    val laboratory: Column[String] = column[String]("LABORATORY", O.Length(50,varying=true))
    /** Database column PROFILE_EXPIRATION_DATE DBType(DATE), Default(None) */
    val profileExpirationDate: Column[Option[java.sql.Date]] = column[Option[java.sql.Date]]("PROFILE_EXPIRATION_DATE", O.Default(None))
    /** Database column RESPONSIBLE_GENETICIST DBType(VARCHAR), Length(50,true), Default(None) */
    val responsibleGeneticist: Column[Option[String]] = column[Option[String]]("RESPONSIBLE_GENETICIST", O.Length(50,varying=true), O.Default(None))
    /** Database column SAMPLE_DATE DBType(DATE), Default(None) */
    val sampleDate: Column[Option[java.sql.Date]] = column[Option[java.sql.Date]]("SAMPLE_DATE", O.Default(None))
    /** Database column SAMPLE_ENTRY_DATE DBType(DATE), Default(None) */
    val sampleEntryDate: Column[Option[java.sql.Date]] = column[Option[java.sql.Date]]("SAMPLE_ENTRY_DATE", O.Default(None))
    /** Database column DELETED DBType(BOOLEAN), Default(false) */
    val deleted: Column[Boolean] = column[Boolean]("DELETED", O.Default(false))
    /** Database column DELETED_SOLICITOR DBType(VARCHAR), Length(100,true), Default(None) */
    val deletedSolicitor: Column[Option[String]] = column[Option[String]]("DELETED_SOLICITOR", O.Length(100,varying=true), O.Default(None))
    /** Database column DELETED_MOTIVE DBType(VARCHAR), Length(8192,true), Default(None) */
    val deletedMotive: Column[Option[String]] = column[Option[String]]("DELETED_MOTIVE", O.Length(8192,varying=true), O.Default(None))

    /** Foreign key referencing BioMaterialType (database name PROFILE_DATA_BIO_MATERIAL_TYPE) */
    lazy val bioMaterialTypeFk = foreignKey("PROFILE_DATA_BIO_MATERIAL_TYPE", bioMaterialType, BioMaterialType)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing Category (database name PROFILE_DATA_CATEGORY_FKEY) */
    lazy val categoryFk = foreignKey("PROFILE_DATA_CATEGORY_FKEY", category, Category)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Restrict)

    /** Uniqueness Index over (globalCode) (database name PROFILE_DATA_GLOBAL_CODE_KEY_INDEX_D) */
    val index1 = index("PROFILE_DATA_GLOBAL_CODE_KEY_INDEX_D", globalCode, unique=true)
    /** Uniqueness Index over (internalSampleCode) (database name PROFILE_DATA_INTERNAL_SAMPLE_CODE_KEY_INDEX_D) */
    val index2 = index("PROFILE_DATA_INTERNAL_SAMPLE_CODE_KEY_INDEX_D", internalSampleCode, unique=true)
  }
  /** Collection-like TableQuery object for table ProfileData */
  lazy val ProfileData = new TableQuery(tag => new ProfileData(tag, Some("APP"), "PROFILE_DATA"))

  /** Entity class storing rows of table ProfileDataFiliation
   *
   *  @param id Database column ID DBType(BIGINT), AutoInc, PrimaryKey
   *  @param profileData Database column PROFILE_DATA DBType(VARCHAR), Length(100,true)
   *  @param fullName Database column FULL_NAME DBType(VARCHAR), Length(150,true)
   *  @param nickname Database column NICKNAME DBType(VARCHAR), Length(150,true)
   *  @param birthday Database column BIRTHDAY DBType(DATE), Default(None)
   *  @param birthPlace Database column BIRTH_PLACE DBType(VARCHAR), Length(100,true), Default(None)
   *  @param nationality Database column NATIONALITY DBType(VARCHAR), Length(50,true), Default(None)
   *  @param identification Database column IDENTIFICATION DBType(VARCHAR), Length(100,true)
   *  @param identificationIssuingAuthority Database column IDENTIFICATION_ISSUING_AUTHORITY DBType(VARCHAR), Length(100,true)
   *  @param address Database column ADDRESS DBType(VARCHAR), Length(100,true) */
  case class ProfileDataFiliationRow(id: Long, profileData: String, fullName: Option[String], nickname: Option[String], birthday: Option[java.sql.Date]=None, birthPlace: Option[String], nationality: Option[String], identification: Option[String], identificationIssuingAuthority: Option[String], address: Option[String])
  /** GetResult implicit for fetching ProfileDataFiliationRow objects using plain SQL queries */
  implicit def GetResultProfileDataFiliationRow(implicit e0: GR[Long], e1: GR[String], e2: GR[java.sql.Date]): GR[ProfileDataFiliationRow] = GR{
    prs => import prs._
      ProfileDataFiliationRow.tupled((<<[Long], <<[String], <<?[String], <<?[String], <<?[java.sql.Date], <<?[String], <<?[String], <<?[String], <<?[String], <<?[String]))
  }
  /** Table description of table PROFILE_DATA_FILIATION. Objects of this class serve as prototypes for rows in queries. */
  class ProfileDataFiliation(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[ProfileDataFiliationRow](_tableTag, schema, tableName) {
    def * = (id, profileData, fullName, nickname, birthday, birthPlace, nationality, identification, identificationIssuingAuthority, address) <> (ProfileDataFiliationRow.tupled, ProfileDataFiliationRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, profileData.?, fullName, nickname, birthday, birthPlace, nationality, identification, identificationIssuingAuthority, address).shaped.<>({r=>import r._; _1.map(_=> ProfileDataFiliationRow.tupled((_1.get, _2.get, _3, _4, _5, _6, _7, _8, _9, _10)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(BIGINT), AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column PROFILE_DATA DBType(VARCHAR), Length(100,true) */
    val profileData: Column[String] = column[String]("PROFILE_DATA", O.Length(100,varying=true))
    /** Database column FULL_NAME DBType(VARCHAR), Length(150,true) */
    val fullName: Column[Option[String]] = column[Option[String]]("FULL_NAME", O.Length(150,varying=true), O.Default(None))
    /** Database column NICKNAME DBType(VARCHAR), Length(150,true) */
    val nickname: Column[Option[String]]= column[Option[String]]("NICKNAME", O.Length(150,varying=true), O.Default(None))
    /** Database column BIRTHDAY DBType(DATE) */
    val birthday: Column[Option[java.sql.Date]] = column[Option[java.sql.Date]]("BIRTHDAY", O.Default(None))
    /** Database column BIRTH_PLACE DBType(VARCHAR), Length(100,true) */
    val birthPlace: Column[Option[String]] = column[Option[String]]("BIRTH_PLACE", O.Length(100,varying=true), O.Default(None))
    /** Database column NATIONALITY DBType(VARCHAR), Length(50,true) */
    val nationality: Column[Option[String]] = column[Option[String]]("NATIONALITY", O.Length(50,varying=true), O.Default(None))
    /** Database column IDENTIFICATION DBType(VARCHAR), Length(100,true) */
    val identification: Column[Option[String]] = column[Option[String]]("IDENTIFICATION", O.Length(100,varying=true), O.Default(None))
    /** Database column IDENTIFICATION_ISSUING_AUTHORITY DBType(VARCHAR), Length(100,true) */
    val identificationIssuingAuthority: Column[Option[String]] = column[Option[String]]("IDENTIFICATION_ISSUING_AUTHORITY", O.Length(100,varying=true), O.Default(None))
    /** Database column ADDRESS DBType(VARCHAR), Length(100,true) */
    val address: Column[Option[String]] = column[Option[String]]("ADDRESS", O.Length(100,varying=true), O.Default(None))

    /** Foreign key referencing ProfileData (database name PROFILE_DATA_FILIATION_FK) */
    lazy val profileDataFk = foreignKey("PROFILE_DATA_FILIATION_FK", profileData, ProfileData)(r => r.globalCode, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)

    /** Uniqueness Index over (profileData) (database name PROFILE_DATA_FILIATION_CODE_KEY_INDEX_D) */
    val index1 = index("PROFILE_DATA_FILIATION_CODE_KEY_INDEX_D", profileData, unique=true)
  }
  /** Collection-like TableQuery object for table ProfileDataFiliation */
  lazy val ProfileDataFiliation = new TableQuery(tag => new ProfileDataFiliation(tag, Some("APP"), "PROFILE_DATA_FILIATION"))

  /** Entity class storing rows of table ProfileDataFiliationResources
   *
   *  @param id Database column ID DBType(BIGINT), AutoInc, PrimaryKey
   *  @param profileDataFiliation Database column PROFILE_DATA_FILIATION DBType(VARCHAR), Length(100,true)
   *  @param resource Database column RESOURCE DBType(BLOB)
   *  @param resourceType Database column RESOURCE_TYPE DBType(VARCHAR), Length(1,true) */
  case class ProfileDataFiliationResourcesRow(id: Long, profileDataFiliation: String, resource: java.sql.Blob, resourceType: String)
  /** GetResult implicit for fetching ProfileDataFiliationResourcesRow objects using plain SQL queries */
  implicit def GetResultProfileDataFiliationResourcesRow(implicit e0: GR[Long], e1: GR[String], e2: GR[java.sql.Blob]): GR[ProfileDataFiliationResourcesRow] = GR{
    prs => import prs._
      ProfileDataFiliationResourcesRow.tupled((<<[Long], <<[String], <<[java.sql.Blob], <<[String]))
  }
  /** Table description of table PROFILE_DATA_FILIATION_RESOURCES. Objects of this class serve as prototypes for rows in queries. */
  class ProfileDataFiliationResources(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[ProfileDataFiliationResourcesRow](_tableTag, schema, tableName) {
    def * = (id, profileDataFiliation, resource, resourceType) <> (ProfileDataFiliationResourcesRow.tupled, ProfileDataFiliationResourcesRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, profileDataFiliation.?, resource.?, resourceType.?).shaped.<>({r=>import r._; _1.map(_=> ProfileDataFiliationResourcesRow.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(BIGINT), AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column PROFILE_DATA_FILIATION DBType(VARCHAR), Length(100,true) */
    val profileDataFiliation: Column[String] = column[String]("PROFILE_DATA_FILIATION", O.Length(100,varying=true))
    /** Database column RESOURCE DBType(BLOB) */
    val resource: Column[java.sql.Blob] = column[java.sql.Blob]("RESOURCE")
    /** Database column RESOURCE_TYPE DBType(VARCHAR), Length(1,true) */
    val resourceType: Column[String] = column[String]("RESOURCE_TYPE", O.Length(1,varying=true))

    /** Foreign key referencing ProfileDataFiliation (database name PROFILE_DATA_FILIATION_RESOURSE_FK) */
    lazy val profileDataFiliationFk = foreignKey("PROFILE_DATA_FILIATION_RESOURSE_FK", profileDataFiliation, ProfileDataFiliation)(r => r.profileData, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table ProfileDataFiliationResources */
  lazy val ProfileDataFiliationResources = new TableQuery(tag => new ProfileDataFiliationResources(tag, Some("APP"), "PROFILE_DATA_FILIATION_RESOURCES"))

  /** Entity class storing rows of table ProtoProfile
   *
   *  @param id Database column ID DBType(BIGINT), AutoInc, PrimaryKey
   *  @param sampleName Database column SAMPLE_NAME DBType(VARCHAR), Length(100,true)
   *  @param idBatch Database column ID_BATCH DBType(BIGINT)
   *  @param assignee Database column ASSIGNEE DBType(VARCHAR), Length(100,true)
   *  @param category Database column CATEGORY DBType(VARCHAR), Length(100,true)
   *  @param status Database column STATUS DBType(VARCHAR), Length(150,true)
   *  @param panel Database column PANEL DBType(VARCHAR), Length(150,true)
   *  @param errors Database column ERRORS DBType(VARCHAR), Length(500,true), Default(None)
   *  @param genotypifications Database column GENOTYPIFICATIONS DBType(VARCHAR), Length(2000,true)
   *  @param matchingRules Database column MATCHING_RULES DBType(VARCHAR), Length(2000,true)
   *  @param mismatchs Database column MISMATCHS DBType(VARCHAR), Length(2000,true)
   *  @param rejectMotive Database column REJECT_MOTIVE DBType(VARCHAR), Length(2000,true), Default(None)
   *  @param preexistence Database column PREEXISTENCE DBType(VARCHAR), Length(100,true), Default(None)
   *  @param genemapperLine Database column GENEMAPPER_LINE DBType(VARCHAR), Length(5000,true) */
  case class ProtoProfileRow(id: Long, sampleName: String, idBatch: Long, assignee: String, category: String, status: String, panel: String, errors: Option[String] = None, genotypifications: String, matchingRules: String, mismatchs: String, rejectMotive: Option[String] = None, preexistence: Option[String] = None, genemapperLine: String,rejectionUser: Option[String]= None, rejectionDate: Option[java.sql.Timestamp]= None, idRejectMotive: Option[Long]= None)
  /** GetResult implicit for fetching ProtoProfileRow objects using plain SQL queries */
  implicit def GetResultProtoProfileRow(implicit e0: GR[Long], e1: GR[String], e2: GR[Option[String]]): GR[ProtoProfileRow] = GR{
    prs => import prs._
      ProtoProfileRow.tupled((<<[Long], <<[String], <<[Long], <<[String], <<[String], <<[String], <<[String], <<?[String], <<[String], <<[String], <<[String], <<?[String], <<?[String], <<[String],<<?[String],<<?[java.sql.Timestamp],<<?[Long]))
  }
  /** Table description of table PROTO_PROFILE. Objects of this class serve as prototypes for rows in queries. */
  class ProtoProfile(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[ProtoProfileRow](_tableTag, schema, tableName) {
    def * = (id, sampleName, idBatch, assignee, category, status, panel, errors, genotypifications, matchingRules, mismatchs, rejectMotive, preexistence, genemapperLine,rejectionUser,rejectionDate,idRejectMotive) <> (ProtoProfileRow.tupled, ProtoProfileRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, sampleName.?, idBatch.?, assignee.?, category.?, status.?, panel.?, errors, genotypifications.?, matchingRules.?, mismatchs.?, rejectMotive, preexistence, genemapperLine.?,rejectionUser,rejectionDate,idRejectMotive).shaped.<>({r=>import r._; _1.map(_=> ProtoProfileRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8, _9.get, _10.get, _11.get, _12, _13, _14.get,_15,_16,_17)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(BIGINT), AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column SAMPLE_NAME DBType(VARCHAR), Length(100,true) */
    val sampleName: Column[String] = column[String]("SAMPLE_NAME", O.Length(100,varying=true))
    /** Database column ID_BATCH DBType(BIGINT) */
    val idBatch: Column[Long] = column[Long]("ID_BATCH")
    /** Database column ASSIGNEE DBType(VARCHAR), Length(100,true) */
    val assignee: Column[String] = column[String]("ASSIGNEE", O.Length(100,varying=true))
    /** Database column CATEGORY DBType(VARCHAR), Length(100,true) */
    val category: Column[String] = column[String]("CATEGORY", O.Length(100,varying=true))
    /** Database column STATUS DBType(VARCHAR), Length(150,true) */
    val status: Column[String] = column[String]("STATUS", O.Length(150,varying=true))
    /** Database column PANEL DBType(VARCHAR), Length(150,true) */
    val panel: Column[String] = column[String]("PANEL", O.Length(150,varying=true))
    /** Database column ERRORS DBType(VARCHAR), Length(500,true), Default(None) */
    val errors: Column[Option[String]] = column[Option[String]]("ERRORS", O.Length(500,varying=true), O.Default(None))
    /** Database column GENOTYPIFICATIONS DBType(VARCHAR), Length(2000,true) */
    val genotypifications: Column[String] = column[String]("GENOTYPIFICATIONS", O.Length(2000,varying=true))
    /** Database column MATCHING_RULES DBType(VARCHAR), Length(2000,true) */
    val matchingRules: Column[String] = column[String]("MATCHING_RULES", O.Length(2000,varying=true))
    /** Database column MISMATCHS DBType(VARCHAR), Length(2000,true) */
    val mismatchs: Column[String] = column[String]("MISMATCHS", O.Length(2000,varying=true))
    /** Database column REJECT_MOTIVE DBType(VARCHAR), Length(2000,true), Default(None) */
    val rejectMotive: Column[Option[String]] = column[Option[String]]("REJECT_MOTIVE", O.Length(2000,varying=true), O.Default(None))
    /** Database column PREEXISTENCE DBType(VARCHAR), Length(100,true), Default(None) */
    val preexistence: Column[Option[String]] = column[Option[String]]("PREEXISTENCE", O.Length(100,varying=true), O.Default(None))
    /** Database column GENEMAPPER_LINE DBType(VARCHAR), Length(5000,true) */
    val genemapperLine: Column[String] = column[String]("GENEMAPPER_LINE", O.Length(5000,varying=true))

    val rejectionUser: Column[Option[String]] = column[Option[String]]("REJECTION_USER", O.Length(5000,varying=true),O.Default(None))
    val rejectionDate: Column[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("REJECTION_DATE",O.Default(None))
    val idRejectMotive: Column[Option[Long]] = column[Option[Long]]("ID_REJECT_MOTIVE",O.Default(None))

    /** Foreign key referencing BatchProtoProfile (database name PROTO_PROFILE_FK) */
    lazy val batchProtoProfileFk = foreignKey("PROTO_PROFILE_FK", idBatch, BatchProtoProfile)(r => r.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table ProtoProfile */
  lazy val ProtoProfile = new TableQuery(tag => new ProtoProfile(tag, Some("APP"), "PROTO_PROFILE"))

  /** Entity class storing rows of table Province
   *
   *  @param code Database column CODE DBType(VARCHAR), PrimaryKey, Length(2,true)
   *  @param name Database column NAME DBType(VARCHAR), Length(50,true)
   *  @param country Database column COUNTRY DBType(VARCHAR), Length(2,true) */
  case class ProvinceRow(code: String, name: String, country: String)
  /** GetResult implicit for fetching ProvinceRow objects using plain SQL queries */
  implicit def GetResultProvinceRow(implicit e0: GR[String]): GR[ProvinceRow] = GR{
    prs => import prs._
      ProvinceRow.tupled((<<[String], <<[String], <<[String]))
  }
  /** Table description of table PROVINCE. Objects of this class serve as prototypes for rows in queries. */
  class Province(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[ProvinceRow](_tableTag, schema, tableName) {
    def * = (code, name, country) <> (ProvinceRow.tupled, ProvinceRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (code.?, name.?, country.?).shaped.<>({r=>import r._; _1.map(_=> ProvinceRow.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column CODE DBType(VARCHAR), PrimaryKey, Length(2,true) */
    val code: Column[String] = column[String]("CODE", O.PrimaryKey, O.Length(2,varying=true))
    /** Database column NAME DBType(VARCHAR), Length(50,true) */
    val name: Column[String] = column[String]("NAME", O.Length(50,varying=true))
    /** Database column COUNTRY DBType(VARCHAR), Length(2,true) */
    val country: Column[String] = column[String]("COUNTRY", O.Length(2,varying=true))

    /** Foreign key referencing Country (database name COUNTRY_PROV_FK) */
    lazy val countryFk = foreignKey("COUNTRY_PROV_FK", country, Country)(r => r.code, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table Province */
  lazy val Province = new TableQuery(tag => new Province(tag, Some("APP"), "PROVINCE"))

  /** Entity class storing rows of table Strkit
   *
   *  @param id Database column ID DBType(VARCHAR), PrimaryKey, Length(50,true)
   *  @param name Database column NAME DBType(VARCHAR), Length(100,true)
   *  @param `type` Database column TYPE DBType(SMALLINT)
   *  @param lociQty Database column LOCI_QTY DBType(INTEGER)
   *  @param representativeParameter Database column REPRESENTATIVE_PARAMETER DBType(INTEGER) */
  case class StrkitRow(id: String, name: String, `type`: Int, lociQty: Int, representativeParameter: Int)
  /** GetResult implicit for fetching StrkitRow objects using plain SQL queries */
  implicit def GetResultStrkitRow(implicit e0: GR[String], e1: GR[Int]): GR[StrkitRow] = GR{
    prs => import prs._
      StrkitRow.tupled((<<[String], <<[String], <<[Int], <<[Int], <<[Int]))
  }
  /** Table description of table STRKIT. Objects of this class serve as prototypes for rows in queries.
   *  NOTE: The following names collided with Scala keywords and were escaped: type */
  class Strkit(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[StrkitRow](_tableTag, schema, tableName) {
    def * = (id, name, `type`, lociQty, representativeParameter) <> (StrkitRow.tupled, StrkitRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, name.?, `type`.?, lociQty.?, representativeParameter.?).shaped.<>({r=>import r._; _1.map(_=> StrkitRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(VARCHAR), PrimaryKey, Length(50,true) */
    val id: Column[String] = column[String]("ID", O.PrimaryKey, O.Length(50,varying=true))
    /** Database column NAME DBType(VARCHAR), Length(100,true) */
    val name: Column[String] = column[String]("NAME", O.Length(100,varying=true))
    /** Database column TYPE DBType(SMALLINT)
     *  NOTE: The name was escaped because it collided with a Scala keyword. */
    val `type`: Column[Int] = column[Int]("TYPE")
    /** Database column LOCI_QTY DBType(INTEGER) */
    val lociQty: Column[Int] = column[Int]("LOCI_QTY")
    /** Database column REPRESENTATIVE_PARAMETER DBType(INTEGER) */
    val representativeParameter: Column[Int] = column[Int]("REPRESENTATIVE_PARAMETER")
  }
  /** Collection-like TableQuery object for table Strkit */
  lazy val Strkit = new TableQuery(tag => new Strkit(tag, Some("APP"), "STRKIT"))

  /** Entity class storing rows of table StrkitAlias
   *
   *  @param kit Database column KIT DBType(VARCHAR), Length(50,true)
   *  @param alias Database column ALIAS DBType(VARCHAR), PrimaryKey, Length(100,true) */
  case class StrkitAliasRow(kit: String, alias: String)
  /** GetResult implicit for fetching StrkitAliasRow objects using plain SQL queries */
  implicit def GetResultStrkitAliasRow(implicit e0: GR[String]): GR[StrkitAliasRow] = GR{
    prs => import prs._
      StrkitAliasRow.tupled((<<[String], <<[String]))
  }
  /** Table description of table STRKIT_ALIAS. Objects of this class serve as prototypes for rows in queries. */
  class StrkitAlias(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[StrkitAliasRow](_tableTag, schema, tableName) {
    def * = (kit, alias) <> (StrkitAliasRow.tupled, StrkitAliasRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (kit.?, alias.?).shaped.<>({r=>import r._; _1.map(_=> StrkitAliasRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column KIT DBType(VARCHAR), Length(50,true) */
    val kit: Column[String] = column[String]("KIT", O.Length(50,varying=true))
    /** Database column ALIAS DBType(VARCHAR), PrimaryKey, Length(100,true) */
    val alias: Column[String] = column[String]("ALIAS", O.PrimaryKey, O.Length(100,varying=true))

    /** Foreign key referencing Strkit (database name STRKIT_ALIAS_FK) */
    lazy val strkitFk = foreignKey("STRKIT_ALIAS_FK", kit, Strkit)(r => r.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table StrkitAlias */
  lazy val StrkitAlias = new TableQuery(tag => new StrkitAlias(tag, Some("APP"), "STRKIT_ALIAS"))

  /** Entity class storing rows of table StrkitLocus
   *
   *  @param strkit Database column STRKIT DBType(VARCHAR), Length(50,true)
   *  @param locus Database column LOCUS DBType(VARCHAR), Length(50,true)
   *  @param fluorophore Database column FLUOROPHORE DBType(VARCHAR), Length(10,true), Default(None)
   *  @param order Database column ORDER DBType(INTEGER), Default(None) */
  case class StrkitLocusRow(strkit: String, locus: String, fluorophore: Option[String] = None, order: Option[Int] = None)
  /** GetResult implicit for fetching StrkitLocusRow objects using plain SQL queries */
  implicit def GetResultStrkitLocusRow(implicit e0: GR[String], e1: GR[Option[String]], e2: GR[Option[Int]]): GR[StrkitLocusRow] = GR{
    prs => import prs._
      StrkitLocusRow.tupled((<<[String], <<[String], <<?[String], <<?[Int]))
  }
  /** Table description of table STRKIT_LOCUS. Objects of this class serve as prototypes for rows in queries. */
  class StrkitLocus(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[StrkitLocusRow](_tableTag, schema, tableName) {
    def * = (strkit, locus, fluorophore, order) <> (StrkitLocusRow.tupled, StrkitLocusRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (strkit.?, locus.?, fluorophore, order).shaped.<>({r=>import r._; _1.map(_=> StrkitLocusRow.tupled((_1.get, _2.get, _3, _4)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column STRKIT DBType(VARCHAR), Length(50,true) */
    val strkit: Column[String] = column[String]("STRKIT", O.Length(50,varying=true))
    /** Database column LOCUS DBType(VARCHAR), Length(50,true) */
    val locus: Column[String] = column[String]("LOCUS", O.Length(50,varying=true))
    /** Database column FLUOROPHORE DBType(VARCHAR), Length(10,true), Default(None) */
    val fluorophore: Column[Option[String]] = column[Option[String]]("FLUOROPHORE", O.Length(10,varying=true), O.Default(None))
    /** Database column ORDER DBType(INTEGER), Default(None) */
    val order: Column[Option[Int]] = column[Option[Int]]("ORDER", O.Default(None))

    /** Primary key of StrkitLocus (database name STRKIT_LOCUS_PKEY) */
    val pk = primaryKey("STRKIT_LOCUS_PKEY", (strkit, locus))

    /** Foreign key referencing Locus (database name STRKIT_LOCUS_LOCUS_FKEY) */
    lazy val locusFk = foreignKey("STRKIT_LOCUS_LOCUS_FKEY", locus, Locus)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Restrict)
    /** Foreign key referencing Strkit (database name STRKIT_LOCUS_STRKITS_FKEY) */
    lazy val strkitFk = foreignKey("STRKIT_LOCUS_STRKITS_FKEY", strkit, Strkit)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table StrkitLocus */
  lazy val StrkitLocus = new TableQuery(tag => new StrkitLocus(tag, Some("APP"), "STRKIT_LOCUS"))


  /** Entity class storing rows of table Notification
   *
   *  @param id Database column ID DBType(BIGINT), AutoInc, PrimaryKey
   *  @param user Database column USER DBType(VARCHAR), Length(50,true)
   *  @param kind Database column KIND DBType(VARCHAR), Length(100,true)
   *  @param info Database column INFO DBType(VARCHAR), Length(1024,true)
   *  @param creationDate Database column CREATION_DATE DBType(TIMESTAMP)
   *  @param updateDate Database column UPDATE_DATE DBType(TIMESTAMP), Default(None)
   *  @param pending Database column PENDING DBType(BOOLEAN), Default(true)
   *  @param flagged Database column FLAGGED DBType(BOOLEAN), Default(false) */
  case class NotificationRow(id: Long, user: String, kind: String, creationDate: java.sql.Timestamp, updateDate: Option[java.sql.Timestamp] = None, flagged: Boolean = false, pending: Boolean = true, info: String)
  /** GetResult implicit for fetching NotificationRow objects using plain SQL queries */
  implicit def GetResultNotificationRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[NotificationRow] = GR{
    prs => import prs._
      NotificationRow.tupled((<<[Long], <<[String], <<[String], <<[java.sql.Timestamp], <<?[java.sql.Timestamp], <<[Boolean], <<[Boolean], <<[String]))
  }
  /** Table description of table NOTIFICATION. Objects of this class serve as prototypes for rows in queries. */
  class Notification(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[NotificationRow](_tableTag, schema, tableName) {
    def * = (id, user, kind, creationDate, updateDate, flagged, pending, info) <> (NotificationRow.tupled, NotificationRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, user.?, kind.?, creationDate.?, updateDate, flagged.?, pending.?, info.?).shaped.<>({r=>import r._; _1.map(_=> NotificationRow.tupled((_1.get, _2.get, _3.get, _4.get, _5, _6.get, _7.get, _8.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(BIGINT), AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column USER DBType(VARCHAR), Length(50,true) */
    val user: Column[String] = column[String]("USER", O.Length(50,varying=true))
    /** Database column KIND DBType(VARCHAR), Length(100,true) */
    val kind: Column[String] = column[String]("KIND", O.Length(100,varying=true))
    /** Database column CREATION_DATE DBType(TIMESTAMP) */
    val creationDate: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("CREATION_DATE")
    /** Database column UPDATE_DATE DBType(TIMESTAMP), Default(None) */
    val updateDate: Column[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("UPDATE_DATE", O.Default(None))
    /** Database column FLAGGED DBType(BOOLEAN), Default(false) */
    val flagged: Column[Boolean] = column[Boolean]("FLAGGED", O.Default(false))
    /** Database column PENDING DBType(BOOLEAN), Default(true) */
    val pending: Column[Boolean] = column[Boolean]("PENDING", O.Default(true))
    /** Database column INFO DBType(VARCHAR), Length(1024,true) */
    val info: Column[String] = column[String]("INFO", O.Length(1024,varying=true))

  }
  /** Collection-like TableQuery object for table Notification */
  lazy val Notification = new TableQuery(tag => new Notification(tag, Some("APP"), "NOTIFICATION"))


  /** Entity class storing rows of table Analysis_Type
   *
   *  @param id Database column ID DBType(SMALLINT), AutoInc, PrimaryKey
   *  @param name Database column NAME DBType(VARCHAR), Length(50,true)
   *  @param mitochondrial Database column MITOCHONDRIAL DBType(BOOLEAN), Default(false) */
  case class AnalysisTypeRow(id: Int, name: String, mitochondrial: Boolean)
  /** GetResult implicit for fetching AnalysisTypeRow objects using plain SQL queries */
  implicit def GetResultAnalysisTypeRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[AnalysisTypeRow] = GR{
    prs => import prs._
      AnalysisTypeRow.tupled((<<[Int], <<[String], <<[Boolean]))
  }
  /** Table description of table ANALYSIS_TYPE. Objects of this class serve as prototypes for rows in queries. */
  class AnalysisType(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[AnalysisTypeRow](_tableTag, schema, tableName) {
    def * = (id, name, mitochondrial) <> (AnalysisTypeRow.tupled, AnalysisTypeRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, name.?, mitochondrial.?).shaped.<>({r=>import r._; _1.map(_=> AnalysisTypeRow.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(SMALLINT), AutoInc, PrimaryKey */
    val id: Column[Int] = column[Int]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column NAME DBType(VARCHAR), Length(50,true) */
    val name: Column[String] = column[String]("NAME", O.Length(50,varying=true))
    /** Database column MITOCHONDRIAL DBType(BOOLEAN), Default(false) */
    val mitochondrial: Column[Boolean] = column[Boolean]("MITOCHONDRIAL", O.Default(false))
  }
  /** Collection-like TableQuery object for table AnalysisType */
  lazy val AnalysisType = new TableQuery(tag => new AnalysisType(tag, Some("APP"), "ANALYSIS_TYPE"))


  /** Entity class storing rows of table Locus_Link
   *
   *  @param id Database column ID DBType(BIGINT), AutoInc, PrimaryKey
   *  @param locus Database column LOCUS DBType(VARCHAR), Length(50,true)
   *  @param link Database column LINK DBType(VARCHAR), Length(50,true)
   *  @param factor Database column FACTOR DBType(DOUBLE)
   *  @param distance Database column DISTANCE DBType(DOUBLE)
   * */
  case class LocusLinkRow(id: Long, locus: String, link: String, factor: Double, distance: Double)
  /** GetResult implicit for fetching LocusLinkRow objects using plain SQL queries */
  implicit def GetResultLocusLinkRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[LocusLinkRow] = GR{
    prs => import prs._
      LocusLinkRow.tupled((<<[Long], <<[String], <<[String], <<[Double], <<[Double]))
  }
  /** Table description of table LOCUS_LINK. Objects of this class serve as prototypes for rows in queries. */
  class LocusLink(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[LocusLinkRow](_tableTag, schema, tableName) {
    def * = (id, locus, link, factor, distance) <> (LocusLinkRow.tupled, LocusLinkRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, locus.?, link.?, factor.?, distance.?).shaped.<>({r=>import r._; _1.map(_=> LocusLinkRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(BIGINT), AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column LOCUS DBType(VARCHAR), Length(50,true) */
    val locus: Column[String] = column[String]("LOCUS", O.Length(50,varying=true))
    /** Database column LINK DBType(VARCHAR), Length(50,true) */
    val link: Column[String] = column[String]("LINK", O.Length(50,varying=true))
    /** Database column FACTOR DBType(DOUBLE) */
    val factor: Column[Double] = column[Double]("FACTOR")
    /** Database column DISTANCE DBType(DOUBLE) */
    val distance: Column[Double] = column[Double]("DISTANCE")
  }
  /** Collection-like TableQuery object for table LocusLink */
  lazy val LocusLink = new TableQuery(tag => new LocusLink(tag, Some("APP"), "LOCUS_LINK"))


  /** Entity class storing rows of table Trace
   *
   *  @param id Database column ID DBType(BIGINT), AutoInc, PrimaryKey
   *  @param profile Database column PROFILE DBType(VARCHAR), Length(100,true)
   *  @param user Database column USER DBType(VARCHAR), Length(50,true)
   *  @param date Database column DATE DBType(TIMESTAMP)
   *  @param trace Database column INFO DBType(VARCHAR)
   *  @param kind Database column KIND DBType(VARCHAR), Length(100,true) */
  case class TraceRow(id: Long, profile: String, user: String, date: java.sql.Timestamp, trace: String, kind: String)
  /** GetResult implicit for fetching TraceRow objects using plain SQL queries */
  implicit def GetResultTraceRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[TraceRow] = GR{
    prs => import prs._
      TraceRow.tupled((<<[Long], <<[String], <<[String], <<[java.sql.Timestamp], <<[String], <<[String]))
  }
  /** Table description of table TRACE. Objects of this class serve as prototypes for rows in queries. */
  class Trace(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[TraceRow](_tableTag, schema, tableName) {
    def * = (id, profile, user, date, trace, kind) <> (TraceRow.tupled, TraceRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, profile.?, user.?, date.?, trace.?, kind.?).shaped.<>({r=>import r._; _1.map(_=> TraceRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(BIGINT), AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column PROFILE DBType(VARCHAR), Length(100,true) */
    val profile: Column[String] = column[String]("PROFILE", O.Length(100,varying=true))
    /** Database column USER DBType(VARCHAR), Length(50,true) */
    val user: Column[String] = column[String]("USER", O.Length(50,varying=true))
    /** Database column DATE DBType(TIMESTAMP) */
    val date: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("DATE")
    /** Database column TRACE DBType(VARCHAR) */
    val trace: Column[String] = column[String]("TRACE")
    /** Database column KIND DBType(VARCHAR), Length(100,true) */
    val kind: Column[String] = column[String]("KIND", O.Length(100,varying=true))

  }
  /** Collection-like TableQuery object for table Trace */
  lazy val Trace = new TableQuery(tag => new Trace(tag, Some("APP"), "TRACE"))

  /** Entity class storing rows of table TracePedigree
   *
   *  @param id Database column ID DBType(BIGINT), AutoInc, PrimaryKey
   *  @param pedigree Database column PEDIGREE DBType(VARCHAR), Length(100,true)
   *  @param user Database column USER DBType(VARCHAR), Length(50,true)
   *  @param date Database column DATE DBType(TIMESTAMP)
   *  @param trace Database column INFO DBType(VARCHAR)
   *  @param kind Database column KIND DBType(VARCHAR), Length(100,true) */
  case class TracePedigreeRow(id: Long, pedigree: Long, user: String, date: java.sql.Timestamp, trace: String, kind: String)
  /** GetResult implicit for fetching TracePedigreeRow objects using plain SQL queries */
  implicit def GetResultTracePedigreeRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[TracePedigreeRow] = GR{
    prs => import prs._
      TracePedigreeRow.tupled((<<[Long], <<[Long], <<[String], <<[java.sql.Timestamp], <<[String], <<[String]))
  }
  /** Table description of table TRACE. Objects of this class serve as prototypes for rows in queries. */
  class TracePedigree(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[TracePedigreeRow](_tableTag, schema, tableName) {
    def * = (id, pedigree, user, date, trace, kind) <> (TracePedigreeRow.tupled, TracePedigreeRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, pedigree.?, user.?, date.?, trace.?, kind.?).shaped.<>({r=>import r._; _1.map(_=> TracePedigreeRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(BIGINT), AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column PEDIGREE DBType(VARCHAR), Length(100,true) */
    val pedigree: Column[Long] = column[Long]("PEDIGREE")
    /** Database column USER DBType(VARCHAR), Length(50,true) */
    val user: Column[String] = column[String]("USER", O.Length(50,varying=true))
    /** Database column DATE DBType(TIMESTAMP) */
    val date: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("DATE")
    /** Database column TRACE DBType(VARCHAR) */
    val trace: Column[String] = column[String]("TRACE")
    /** Database column KIND DBType(VARCHAR), Length(100,true) */
    val kind: Column[String] = column[String]("KIND", O.Length(100,varying=true))

  }
  /** Collection-like TableQuery object for table TracePedigree */
  lazy val TracePedigree = new TableQuery(tag => new TracePedigree(tag, Some("APP"), "TRACE_PEDIGREE"))
  /** Entity class storing rows of table Connection
   *
   *  @param id Database column ID DBType(BIGINT), AutoInc, PrimaryKey
   *  @param name Database column NAME DBType(VARCHAR), Length(200,true)
   *  @param url Database column URL DBType(VARCHAR), Length(200,true)
   *  @param deleted Database column DELETED DBType(BOOLEAN), Default(false)  */
  case class ConnectionRow(id: Long, name: String, url: String, deleted: Boolean = false)
  /** GetResult implicit for fetching ConnectionRow objects using plain SQL queries */
  implicit def GetResultConnectionRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[ConnectionRow] = GR{
    prs => import prs._
      ConnectionRow.tupled((<<[Long], <<[String], <<[String],<<[Boolean]))
  }
  /** Table description of table CONNECTION. Objects of this class serve as prototypes for rows in queries. */
  class Connection(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[ConnectionRow](_tableTag, schema, tableName) {
    def * = (id, name, url,deleted) <> (ConnectionRow.tupled, ConnectionRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, name.?, url.?,deleted.?).shaped.<>({r=>import r._; _1.map(_=> ConnectionRow.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    val name: Column[String] = column[String]("NAME", O.Length(200,varying=true))
    val url: Column[String] = column[String]("URL", O.Length(200,varying=true))
    val deleted: Column[Boolean] = column[Boolean]("DELETED", O.Default(false))

  }
  lazy val Connection = new TableQuery(tag => new Connection(tag, Some("APP"), "CONNECTION"))

  case class CategoryMappingRow(id: String, idSuperior: String)
  /** GetResult implicit for fetching CategoryMappingRow objects using plain SQL queries */
  implicit def GetResultCategoryMappingRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[CategoryMappingRow] = GR{
    prs => import prs._
      CategoryMappingRow.tupled((<<[String], <<[String]))
  }

  /** Table description of table CATEGORY_MAPPING. Objects of this class serve as prototypes for rows in queries. */
  class CategoryMapping(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[CategoryMappingRow](_tableTag, schema, tableName) {
    def * = (id, idSuperior) <> (CategoryMappingRow.tupled, CategoryMappingRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, idSuperior.?).shaped.<>({r=>import r._; _1.map(_=> CategoryMappingRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    val id: Column[String] = column[String]("ID", O.PrimaryKey, O.Length(50,varying=true))
    val idSuperior: Column[String] = column[String]("ID_SUPERIOR", O.Length(50,varying=true))

  }

  lazy val CategoryMapping = new TableQuery(tag => new CategoryMapping(tag, Some("APP"), "CATEGORY_MAPPING"))

  case class InferiorInstanceRow(id: Long, url: String,  status:Long,laboratory:String)
  /** GetResult implicit for fetching InferiorInstanceRow objects using plain SQL queries */
  implicit def GetResultInferiorInstanceRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[InferiorInstanceRow] = GR{
    prs => import prs._
      InferiorInstanceRow.tupled((<<[Long], <<[String],<<[Long], <<[String]))
  }

  /** Table description of table INFERIOR_INSTANCE. Objects of this class serve as prototypes for rows in queries. */
  class InferiorInstance(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[InferiorInstanceRow](_tableTag, schema, tableName) {
    def * = (id, url,status,laboratory) <> (InferiorInstanceRow.tupled, InferiorInstanceRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, url.?,status.?,laboratory.?).shaped.<>({r=>import r._; _1.map(_=> InferiorInstanceRow.tupled((_1.get, _2.get, _3.get,_4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    val url: Column[String] = column[String]("URL", O.Length(200,varying=true))
    val laboratory: Column[String] = column[String]("LABORATORY", O.Length(50,varying=true))
    val status:  Column[Long] = column[Long]("STATUS")

  }

  lazy val InferiorInstance = new TableQuery(tag => new InferiorInstance(tag, Some("APP"), "INFERIOR_INSTANCE"))

  case class InstanceStatusRow(id: Long, status: String)
  /** GetResult implicit for fetching InstanceStatusRow objects using plain SQL queries */
  implicit def GetResultInstanceStatusRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[InstanceStatusRow] = GR{
    prs => import prs._
      InstanceStatusRow.tupled((<<[Long], <<[String]))
  }

  /** Table description of table INSTANCE_STATUS. Objects of this class serve as prototypes for rows in queries. */
  class InstanceStatus(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[InstanceStatusRow](_tableTag, schema, tableName) {
    def * = (id, status) <> (InstanceStatusRow.tupled, InstanceStatusRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, status.?).shaped.<>({r=>import r._; _1.map(_=> InstanceStatusRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    val status: Column[String] = column[String]("STATUS", O.Length(200,varying=true))

  }

  lazy val InstanceStatus = new TableQuery(tag => new InstanceStatus(tag, Some("APP"), "INSTANCE_STATUS"))
  //
  case class SuperiorInstanceProfileApprovalRow(id: Long,globalCode:String, profile: String, laboratory: String,
                                                laboratoryInstanceOrigin: String, laboratoryImmediateInstance: String,
                                                sampleEntryDate: Option[java.sql.Date] = None,
                                                receptionDate: Option[java.sql.Timestamp] = None,
                                                errors: Option[String] = None,
                                                rejectionUser: Option[String] = None,
                                                rejectionDate: Option[java.sql.Timestamp] = None,
                                                idRejectMotive: Option[Long] = None,
                                                rejectMotive: Option[String] = None,
                                                deleted: Boolean = false,
                                                profileAssociated: Option[String] = None)
  /** GetResult implicit for fetching SuperiorInstanceProfileApprovalRow objects using plain SQL queries */
  implicit def GetResultSuperiorInstanceProfileApprovalRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[SuperiorInstanceProfileApprovalRow] = GR{
    prs => import prs._
      SuperiorInstanceProfileApprovalRow.tupled((<<[Long], <<[String], <<[String], <<[String],<<[String], <<[String], <<?[java.sql.Date], <<?[java.sql.Timestamp], <<?[String], <<?[String],<<?[java.sql.Timestamp],<<?[Long],<<?[String],<<[Boolean],<<?[String]))
  }

  /** Table description of table SUPERIOR_INSTANCE_PROFILE_APPROVAL. Objects of this class serve as prototypes for rows in queries. */
  class SuperiorInstanceProfileApproval(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[SuperiorInstanceProfileApprovalRow](_tableTag, schema, tableName) {
    def * = (id,globalCode, profile,laboratory,laboratoryInstanceOrigin,laboratoryImmediateInstance,sampleEntryDate,receptionDate,errors,rejectionUser,rejectionDate,idRejectMotive,rejectMotive,deleted,profileAssociated) <> (SuperiorInstanceProfileApprovalRow.tupled, SuperiorInstanceProfileApprovalRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?,globalCode.?, profile.?,laboratory.?,laboratoryInstanceOrigin.?,laboratoryImmediateInstance.?,sampleEntryDate,receptionDate,errors,rejectionUser,rejectionDate,idRejectMotive,rejectMotive,deleted.?, profileAssociated).shaped.<>({ r=>import r._; _1.map(_=> SuperiorInstanceProfileApprovalRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get,_6.get,_7,_8,_9,_10,_11,_12,_13,_14.get,_15)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))
    //
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    val globalCode: Column[String] = column[String]("GLOBAL_CODE", O.Length(100,varying=true))
    val profile: Column[String] = column[String]("PROFILE")
    val laboratory: Column[String] = column[String]("LABORATORY", O.Length(50,varying=true))
    val laboratoryInstanceOrigin: Column[String] = column[String]("LABORATORY_INSTANCE_ORIGIN", O.Length(50,varying=true))
    val laboratoryImmediateInstance: Column[String] = column[String]("LABORATORY_INSTANCE_INMEDIATE", O.Length(50,varying=true))
    val sampleEntryDate: Column[Option[java.sql.Date]] = column[Option[java.sql.Date]]("SAMPLE_ENTRY_DATE", O.Default(None))
    val receptionDate: Column[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("RECEPTION_DATE", O.Default(Some(new java.sql.Timestamp(System.currentTimeMillis()))))
    val errors: Column[Option[String]] = column[Option[String]]("ERRORS")
    val rejectionUser: Column[Option[String]] = column[Option[String]]("REJECTION_USER", O.Length(50,varying=true),O.Default(None))
    val rejectionDate: Column[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("REJECTION_DATE",O.Default(None))
    val idRejectMotive: Column[Option[Long]] = column[Option[Long]]("ID_REJECT_MOTIVE",O.Default(None))
    val rejectMotive: Column[Option[String]] = column[Option[String]]("REJECT_MOTIVE", O.Default(None))
    val deleted: Column[Boolean] = column[Boolean]("DELETED", O.Default(false))
    val profileAssociated: Column[Option[String]] = column[Option[String]]("PROFILE_ASSOCIATED")

  }

  lazy val SuperiorInstanceProfileApproval = new TableQuery(tag => new SuperiorInstanceProfileApproval(tag, Some("APP"), "SUPERIOR_INSTANCE_PROFILE_APPROVAL"))

  case class ExternalProfileDataRow(id: Long, laboratoryOrigin: String, laboratoryImmediate: String)
  /** GetResult implicit for fetching ExternalProfileDataRow objects using plain SQL queries */
  implicit def GetResultExternalProfileData(implicit e0: GR[String], e1: GR[Option[String]]): GR[ExternalProfileDataRow] = GR{
    prs => import prs._
      ExternalProfileDataRow.tupled((<<[Long], <<[String], <<[String]))
  }

  /** Table description of table EXTERNAL_PROFILE_DATA. Objects of this class serve as prototypes for rows in queries. */
  class ExternalProfileData(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[ExternalProfileDataRow](_tableTag, schema, tableName) {
    def * = (id, laboratoryOrigin,laboratoryImmediate) <> (ExternalProfileDataRow.tupled, ExternalProfileDataRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, laboratoryOrigin.?,laboratoryImmediate.?).shaped.<>({ r=>import r._; _1.map(_=> ExternalProfileDataRow.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    val id: Column[Long] = column[Long]("ID", O.PrimaryKey)
    val laboratoryOrigin: Column[String] = column[String]("LABORATORY_INSTANCE_ORIGIN", O.Length(50,varying=true))
    val laboratoryImmediate: Column[String] = column[String]("LABORATORY_INSTANCE_INMEDIATE", O.Length(50,varying=true))

  }

  lazy val ExternalProfileData = new TableQuery(tag => new ExternalProfileData(tag, Some("APP"), "EXTERNAL_PROFILE_DATA"))

  case class ProfileUploadedRow(id: Long, globalCode:String ,status: Long,motive:Option[String] = None: None.type , interconnection_error: Option[String] = None, userName:Option[String]=None)
  /** GetResult implicit for fetching ProfileUploaded objects using plain SQL queries */
  implicit def GetResultProfileUploaded(implicit e0: GR[String], e1: GR[Option[String]]): GR[ProfileUploadedRow] = GR{
    prs => import prs._
      ProfileUploadedRow.tupled((<<[Long],<<[String], <<[Long],<<?[String] ,<<?[String], <<?[String]))
  }

  /** Table description of table PROFILE_UPLOADED. Objects of this class serve as prototypes for rows in queries. */
  class ProfileUploaded(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[ProfileUploadedRow](_tableTag, schema, tableName) {
    def * = (id,globalCode, status,motive, interconnection_error, userName) <> (ProfileUploadedRow.tupled, ProfileUploadedRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ?  = (id.?,globalCode.?, status.?,motive,interconnection_error, userName).shaped.<>({ r=>import r._; _1.map(_=> ProfileUploadedRow.tupled((_1.get, _2.get,_3.get,_4, _5, _6)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    val id: Column[Long] = column[Long]("ID", O.PrimaryKey)
    val globalCode: Column[String] = column[String]("GLOBAL_CODE", O.Length(100,varying=true))
    val status: Column[Long] = column[Long]("STATUS")
    val motive: Column[Option[String]] = column[Option[String]]("MOTIVE")
    val interconnection_error: Column[Option[String]] = column[Option[String]]("INTERCONNECTION_ERROR")
    val userName: Column[Option[String]] = column[Option[String]]("USER")
  }

  lazy val ProfileUploaded = new TableQuery(tag => new ProfileUploaded(tag, Some("APP"), "PROFILE_UPLOADED"))

  case class ProfileSentRow(id: Long, labCode:String,globalCode:String ,status: Long,motive:Option[String] = None, interconnectionError:Option[String] = None, userName:Option[String]=None)
  /** GetResult implicit for fetching ProfileSent objects using plain SQL queries */
  implicit def GetResultProfileSent(implicit e0: GR[Long], e1: GR[String], e2: GR[Option[String]]): GR[ProfileSentRow] = GR{
    prs => import prs._
      ProfileSentRow.tupled((<<[Long],<<[String],<<[String], <<[Long],<<?[String], <<?[String], <<?[String]))
  }

  /** Table description of table PROFILE_SENT. Objects of this class serve as prototypes for rows in queries. */
  class ProfileSent(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[ProfileSentRow](_tableTag, schema, tableName) {
    def * = (id, labCode,globalCode, status,motive, interconnectionError, userName) <> (ProfileSentRow.tupled, ProfileSentRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, labCode.?,globalCode.?, status.?,motive, interconnectionError, userName).shaped.<>({ r=>import r._; _1.map(_=> ProfileSentRow.tupled((_1.get, _2.get,_3.get,_4.get,_5,_6,_7)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    val id: Column[Long] = column[Long]("ID", O.PrimaryKey)
    val labCode: Column[String] = column[String]("LABCODE", O.Length(100,varying=true))
    val globalCode: Column[String] = column[String]("GLOBAL_CODE", O.Length(100,varying=true))
    val status: Column[Long] = column[Long]("STATUS")
    val motive: Column[Option[String]] = column[Option[String]]("MOTIVE")
    val interconnectionError: Column[Option[String]] = column[Option[String]]("INTERCONNECTION_ERROR")
    val userName: Column[Option[String]] = column[Option[String]]("USER")
  }
  lazy val ProfileSent = new TableQuery(tag => new ProfileSent(tag, Some("APP"), "PROFILE_SENT"))

  case class ProfileReceivedRow(globalCode:String,labCode:String,status: Long,motive:Option[String] = None,  userName:Option[String]= None, isCategoryModification: Boolean ,interconnectionError:Option[String] = None)
  /** GetResult implicit for fetching ProfileSent objects using plain SQL queries */
  implicit def GetResultProfileReceived(implicit e0: GR[Long], e1: GR[String], e2: GR[Option[String]]): GR[ProfileReceivedRow] = GR{
    prs => import prs._
      ProfileReceivedRow.tupled((<<[String],<<[String], <<[Long],<<?[String], <<?[String], <<[Boolean], <<?[String]))
  }
  /** Table description of table PROFILE_RECEIVED. Objects of this class serve as prototypes for rows in queries. */
  class ProfileReceived(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[ProfileReceivedRow](_tableTag, schema, tableName) {

    def * = (globalCode, labCode, status, motive, userName, isCategoryModification, interconnectionError) <>
      (ProfileReceivedRow.tupled, ProfileReceivedRow.unapply)

    def ? = (globalCode.?, labCode.?, status.?, motive, userName, isCategoryModification.?, interconnectionError).shaped.<>(
      { r => import r._; _1.map(_ => ProfileReceivedRow.tupled((_1.get, _2.get, _3.get, _4, _5, _6.get, _7))) },
      (_: Any) => throw new Exception("Inserting into ? projection not supported.")
    )

    val globalCode: Column[String] = column[String]("GLOBAL_CODE", O.Length(100, varying = true), O.PrimaryKey)
    val labCode: Column[String] = column[String]("LABCODE", O.Length(100, varying = true))
    val status: Column[Long] = column[Long]("STATUS")
    val motive: Column[Option[String]] = column[Option[String]]("MOTIVE")
    val userName: Column[Option[String]] = column[Option[String]]("USER")
    val isCategoryModification: Column[Boolean] = column[Boolean]("IS_CATEGORY_MODIFICATION")
    val interconnectionError: Column[Option[String]] = column[Option[String]]("INTERCONNECTION_ERROR")
  }

  lazy val ProfileReceived = new TableQuery(tag => new ProfileReceived(tag, Some("APP"), "PROFILE_RECEIVED"))



  case class DisclaimerRow(id: Long, text:String )
  /** GetResult implicit for fetching Disclaimer objects using plain SQL queries */
  implicit def GetResultDisclaimerRow(implicit e0: GR[String]): GR[DisclaimerRow] = GR{
    prs => import prs._
      DisclaimerRow.tupled((<<[Long],<<[String]))
  }

  /** Table description of table DISCLAIMER. Objects of this class serve as prototypes for rows in queries. */
  class Disclaimer(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[DisclaimerRow](_tableTag, schema, tableName) {
    def * = (id,text) <> (DisclaimerRow.tupled, DisclaimerRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?,text.?).shaped.<>({ r=>import r._; _1.map(_=> DisclaimerRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))
    val id: Column[Long] = column[Long]("ID", O.PrimaryKey)
    val text: Column[String] = column[String]("TEXT")
  }

  lazy val Disclaimer = new TableQuery(tag => new Disclaimer(tag, Some("APP"), "DISCLAIMER"))

  //

  case class MotiveTypeRow(id: Long, description:String )
  /** GetResult implicit for fetching MotiveType objects using plain SQL queries */
  implicit def GetResultMotiveTypeRow(implicit e0: GR[String]): GR[MotiveTypeRow] = GR{
    prs => import prs._
      MotiveTypeRow.tupled((<<[Long],<<[String]))
  }

  /** Table description of table MOTIVE_TYPE. Objects of this class serve as prototypes for rows in queries. */
  class MotiveType(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[MotiveTypeRow](_tableTag, schema, tableName) {
    def * = (id,description) <> (MotiveTypeRow.tupled, MotiveTypeRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?,description.?).shaped.<>({ r=>import r._; _1.map(_=> DisclaimerRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))
    val id: Column[Long] = column[Long]("ID", O.PrimaryKey)
    val description: Column[String] = column[String]("DESCRIPTION")
  }

  lazy val MotiveType = new TableQuery(tag => new MotiveType(tag, Some("APP"), "MOTIVE_TYPE"))


  //

  case class MotiveRow(id: Long, motiveType: Long, description:String,freeText: Boolean,deleted: Boolean =false )
  /** GetResult implicit for fetching Disclaimer objects using plain SQL queries */
  implicit def GetResultMotiveRow(implicit e0: GR[String]): GR[MotiveRow] = GR{
    prs => import prs._
      MotiveRow.tupled((<<[Long],<<[Long],<<[String],<<[Boolean],<<[Boolean]))
  }

  /** Table description of table MOTIVE. Objects of this class serve as prototypes for rows in queries. */
  class Motive(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[MotiveRow](_tableTag, schema, tableName) {
    def * = (id,motiveType,description,freeText,deleted) <> (MotiveRow.tupled, MotiveRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?,motiveType.?,description.?,freeText.?,deleted.?).shaped.<>({ r=>import r._; _1.map(_=> MotiveRow.tupled((_1.get, _2.get,_3.get,_4.get,_5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    val motiveType: Column[Long] = column[Long]("MOTIVE_TYPE")
    val description: Column[String] = column[String]("DESCRIPTION")
    val freeText: Column[Boolean] = column[Boolean]("FREE_TEXT")
    val deleted: Column[Boolean] = column[Boolean]("DELETED")

  }

  lazy val Motive = new TableQuery(tag => new Motive(tag, Some("APP"), "MOTIVE"))


  case class ProfileDataMotiveRow(id: Long, idProfileData:Long, deletedDate: java.sql.Timestamp, idDeletedMotive:Long )
  /** GetResult implicit for fetching Disclaimer objects using plain SQL queries */
  implicit def GetResultProfileDataMotiveRow(implicit e0: GR[String]): GR[ProfileDataMotiveRow] = GR{
    prs => import prs._
      ProfileDataMotiveRow.tupled((<<[Long],<<[Long],<<[java.sql.Timestamp],<<[Long]))
  }

  /** Table description of table PROFILE_DATA_MOTIVE. Objects of this class serve as prototypes for rows in queries. */
  class ProfileDataMotive(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[ProfileDataMotiveRow](_tableTag, schema, tableName) {
    def * = (id,idProfileData,deletedDate,idDeletedMotive) <> (ProfileDataMotiveRow.tupled, ProfileDataMotiveRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?,idProfileData.?,deletedDate.?,idDeletedMotive.?).shaped.<>({ r=>import r._; _1.map(_=> ProfileDataMotiveRow.tupled((_1.get, _2.get,_3.get,_4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))
    val id: Column[Long] = column[Long]("ID",O.AutoInc, O.PrimaryKey)
    val idProfileData: Column[Long] = column[Long]("ID_PROFILE_DATA")
    val deletedDate: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("DELETED_DATE")
    val idDeletedMotive: Column[Long] = column[Long]("ID_DELETED_MOTIVE")
  }

  lazy val ProfileDataMotive = new TableQuery(tag => new ProfileDataMotive(tag, Some("APP"), "PROFILE_DATA_MOTIVE"))


  case class MatchSendStatusRow(id: String,targetLab: String, status:Option[Long] = None, message:Option[String] = None,date:Option[java.sql.Timestamp] = None)
  /** GetResult implicit for fetching MatchSendStatus objects using plain SQL queries */
  implicit def GetResultMatchSendStatus(implicit e0: GR[String], e1: GR[Option[String]]): GR[MatchSendStatusRow] = GR{
    prs => import prs._
      MatchSendStatusRow.tupled((<<[String],<<[String], <<[Option[Long]], <<[Option[String]], <<[Option[java.sql.Timestamp]]))
  }

  /** Table description of table MATCH_SEND_STATUS. Objects of this class serve as prototypes for rows in queries. */
  class MatchSendStatus(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[MatchSendStatusRow](_tableTag, schema, tableName) {
    def * = (id, targetLab,status,message,date) <> (MatchSendStatusRow.tupled, MatchSendStatusRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, targetLab.?,status,message,date).shaped.<>({ r=>import r._; _1.map(_=> MatchSendStatusRow.tupled((_1.get, _2.get, _3, _4, _5)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))
    val id: Column[String] = column[String]("ID", O.PrimaryKey)
    val targetLab: Column[String] = column[String]("TARGET_LAB", O.PrimaryKey)

    val status: Column[Option[Long]] = column[Option[Long]]("STATUS")
    val message: Column[Option[String]] = column[Option[String]]("MESSAGE")
    val date: Column[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("DATE", O.Default(Some(new java.sql.Timestamp(System.currentTimeMillis()))))

  }

  lazy val MatchSendStatus = new TableQuery(tag => new MatchSendStatus(tag, Some("APP"), "MATCH_SEND_STATUS"))

  case class MatchUpdateSendStatusRow(id: String,targetLab: String, status:Option[Long] = None,message:Option[String] = None,date:Option[java.sql.Timestamp] = None)
  /** GetResult implicit for fetching MatchUpdateSendStatus objects using plain SQL queries */
  implicit def GetResultMatchUpdateSendStatus(implicit e0: GR[String], e1: GR[Option[String]]): GR[MatchUpdateSendStatusRow] = GR{
    prs => import prs._
      MatchUpdateSendStatusRow.tupled((<<[String],<<[String], <<[Option[Long]], <<[Option[String]], <<[Option[java.sql.Timestamp]]))
  }

  /** Table description of table MATCH_UPDATE_SEND_STATUS. Objects of this class serve as prototypes for rows in queries. */
  class MatchUpdateSendStatus(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[MatchUpdateSendStatusRow](_tableTag, schema, tableName) {
    def * = (id, targetLab, status, message, date) <> (MatchUpdateSendStatusRow.tupled, MatchUpdateSendStatusRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, targetLab.?, status, message, date).shaped.<>({ r => import r._; _1.map(_ => MatchUpdateSendStatusRow.tupled((_1.get, _2.get, _3, _4, _5))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    val id: Column[String] = column[String]("ID", O.PrimaryKey)
    val targetLab: Column[String] = column[String]("TARGET_LAB", O.PrimaryKey)
    val status: Column[Option[Long]] = column[Option[Long]]("STATUS")
    val message: Column[Option[String]] = column[Option[String]]("MESSAGE")
    val date: Column[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("DATE", O.Default(Some(new java.sql.Timestamp(System.currentTimeMillis()))))
  }
  lazy val MatchUpdateSendStatus = new TableQuery(tag => new MatchUpdateSendStatus(tag, Some("APP"), "MATCH_UPDATE_SEND_STATUS"))


  case class CourtCaseProfilesRow(idCourtCase:Long,globalCode: String , profileType: String,groupedBy: Option[String])
  /** GetResult implicit for fetching CourtCaseProfiles objects using plain SQL queries */
  implicit def GetResultCourtCaseProfiles(implicit e0: GR[String], e1: GR[Option[String]]): GR[CourtCaseProfilesRow] = GR{
    prs => import prs._
      CourtCaseProfilesRow.tupled((<<[Long],<<[String],<<[String],<<[Option[String]]))
  }

  /** Table description of table COURT_CASE_PROFILE. Objects of this class serve as prototypes for rows in queries. */
  class CourtCaseProfiles(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[CourtCaseProfilesRow](_tableTag, schema, tableName) {
    def * = (idCourtCase,globalCode,profileType,groupedBy) <> (CourtCaseProfilesRow.tupled, CourtCaseProfilesRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (idCourtCase.?,globalCode.?,profileType.?,groupedBy).shaped.<>({ r=>import r._; _1.map(_=> CourtCaseProfilesRow.tupled((_1.get, _2.get,_3.get,_4)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    val idCourtCase: Column[Long] = column[Long]("ID_COURT_CASE", O.PrimaryKey)
    val globalCode: Column[String] = column[String]("GLOBAL_CODE", O.PrimaryKey)
    val profileType: Column[String] = column[String]("PROFILE_TYPE")
    val groupedBy: Column[Option[String]] = column[Option[String]]("GROUPED_BY")
  }

  lazy val CourtCaseProfiles = new TableQuery(tag => new CourtCaseProfiles(tag, Some("APP"), "COURT_CASE_PROFILE"))

  case class FileSentRow(id: String,targetLab: String, status:Option[Long] = None,date:Option[java.sql.Timestamp] = None,fileType:String)
  /** GetResult implicit for fetching FileSent objects using plain SQL queries */
  implicit def GetResultFileSent(implicit e0: GR[String], e1: GR[Option[String]]): GR[FileSentRow] = GR{
    prs => import prs._
      FileSentRow.tupled((<<[String],<<[String], <<[Option[Long]], <<[Option[java.sql.Timestamp]],<<[String]))
  }

  /** Table description of table MATCH_UPDATE_SEND_STATUS. Objects of this class serve as prototypes for rows in queries. */
  class FileSent(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[FileSentRow](_tableTag, schema, tableName) {
    def * = (id, targetLab, status,  date,fileType) <> (FileSentRow.tupled, FileSentRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, targetLab.?, status,  date,fileType.?).shaped.<>({ r => import r._; _1.map(_ => FileSentRow.tupled((_1.get, _2.get, _3, _4,_5.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    val id: Column[String] = column[String]("ID", O.PrimaryKey)
    val targetLab: Column[String] = column[String]("TARGET_LAB", O.PrimaryKey)
    val status: Column[Option[Long]] = column[Option[Long]]("STATUS")
    val date: Column[Option[java.sql.Timestamp]] = column[Option[java.sql.Timestamp]]("DATE", O.Default(Some(new java.sql.Timestamp(System.currentTimeMillis()))))
    val fileType: Column[String] = column[String]("FILE_TYPE")
  }
  lazy val FileSent = new TableQuery(tag => new FileSent(tag, Some("APP"), "FILE_SENT"))

  case class MutationModelTypeRow(id: Long,description: String)
  /** GetResult implicit for fetching MutationModelType objects using plain SQL queries */
  implicit def GetResultMutationModelType(implicit e0: GR[String], e1: GR[Option[String]]): GR[MutationModelTypeRow] = GR{
    prs => import prs._
      MutationModelTypeRow.tupled((<<[Long],<<[String]))
  }
  /** Table description of table MUTATION_MODEL_TYPE. Objects of this class serve as prototypes for rows in queries. */
  class MutationModelType(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[MutationModelTypeRow](_tableTag, schema, tableName) {
    def * = (id, description) <> (MutationModelTypeRow.tupled, MutationModelTypeRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, description.?).shaped.<>({ r => import r._; _1.map(_ => MutationModelTypeRow.tupled((_1.get, _2.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    val id: Column[Long] = column[Long]("ID",O.AutoInc, O.PrimaryKey)
    val description: Column[String] = column[String]("DESCRIPTION")
  }
  lazy val MutationModelType = new TableQuery(tag => new MutationModelType(tag, Some("APP"), "MUTATION_MODEL_TYPE"))

  case class MutationModelRow(id: Long,name: String,mutationType:Long,active:Boolean,ignoreSex:Boolean, cantSaltos:Long)
  /** GetResult implicit for fetching MutationModel objects using plain SQL queries */
  implicit def GetResultMutationModel(implicit e0: GR[String], e1: GR[Option[String]]): GR[MutationModelRow] = GR{
    prs => import prs._
      MutationModelRow.tupled((<<[Long],<<[String],<<[Long],<<[Boolean],<<[Boolean],<<[Long]))
  }
  /** Table name of table MUTATION_MODEL_TYPE. Objects of this class serve as prototypes for rows in queries. */
  class MutationModel(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[MutationModelRow](_tableTag, schema, tableName) {
    def * = (id, name,mutationType,active,ignoreSex, cantSaltos) <> (MutationModelRow.tupled, MutationModelRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, name.?,mutationType.?,active.?,ignoreSex.?, cantSaltos.?).shaped.<>({ r => import r._; _1.map(_ => MutationModelRow.tupled((_1.get,_2.get,_3.get,_4.get,_5.get, _6.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    val id: Column[Long] = column[Long]("ID",O.AutoInc, O.PrimaryKey)
    val name: Column[String] = column[String]("NAME")
    val mutationType: Column[Long] = column[Long]("MUTATION_MODEL_TYPE")
    val active: Column[Boolean] = column[Boolean]("ACTIVE")
    val ignoreSex: Column[Boolean] = column[Boolean]("IGNORE_SEX")
    val cantSaltos: Column[Long] = column[Long]("CANT_SALTOS")

  }
  lazy val MutationModel = new TableQuery(tag => new MutationModel(tag, Some("APP"), "MUTATION_MODEL"))

  case class MutationModelParameterRow(id: Long,idMutationModel: Long,locus:String,sex:String,mutationRate:Option[scala.math.BigDecimal],
                                       mutationRange:Option[scala.math.BigDecimal],mutationRateMicrovariant:Option[scala.math.BigDecimal])
  /** GetResult implicit for fetching MutationModelParameter objects using plain SQL queries */
  implicit def GetResultMutationModelParameter(implicit e0: GR[String], e1: GR[Option[String]]): GR[MutationModelParameterRow] = GR{
    prs => import prs._
      MutationModelParameterRow.tupled((<<[Long],<<[Long],<<[String],<<[String],<<[Option[scala.math.BigDecimal]],<<[Option[scala.math.BigDecimal]],<<[Option[scala.math.BigDecimal]]))
  }
  /** Table name of table MUTATION_MODEL_PARAMETER. Objects of this class serve as prototypes for rows in queries. */
  class MutationModelParameter(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[MutationModelParameterRow](_tableTag, schema, tableName) {
    def * = (id, idMutationModel,locus,sex,mutationRate,mutationRange,mutationRateMicrovariant) <> (MutationModelParameterRow.tupled, MutationModelParameterRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, idMutationModel.?,locus.?,sex.?,mutationRate,mutationRange,mutationRateMicrovariant).shaped.<>({ r => import r._; _1.map(_ => MutationModelParameterRow.tupled((_1.get, _2.get,_3.get, _4.get,_5,_6,_7))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    val id: Column[Long] = column[Long]("ID",O.AutoInc, O.PrimaryKey)
    val idMutationModel: Column[Long] = column[Long]("ID_MUTATION_MODEL")
    val locus: Column[String] = column[String]("LOCUS")
    val sex: Column[String] = column[String]("SEX")
    val mutationRate: Column[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("MUTATION_RATE")
    val mutationRange: Column[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("MUTATION_RANGE")
    val mutationRateMicrovariant: Column[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("MUTATION_RATE_MICROVARIANT")
  }
  lazy val MutationModelParameter = new TableQuery(tag => new MutationModelParameter(tag, Some("APP"), "MUTATION_MODEL_PARAMETER"))

  case class MutationModelKiRow(id: Long,idMutationModelParameter: Long,allele:Double,ki:scala.math.BigDecimal)
  /** GetResult implicit for fetching MutationModelKi objects using plain SQL queries */
  implicit def GetResultMutationModelKi(implicit e0: GR[String], e1: GR[Option[String]]): GR[MutationModelKiRow] = GR{
    prs => import prs._
      MutationModelKiRow.tupled((<<[Long],<<[Long],<<[Double],<<[scala.math.BigDecimal]))
  }
  /** Table name of table MUTATION_MODEL_KI. Objects of this class serve as prototypes for rows in queries. */
  class MutationModelKi(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[MutationModelKiRow](_tableTag, schema, tableName) {
    def * = (id, idMutationModelParameter,allele,ki) <> (MutationModelKiRow.tupled, MutationModelKiRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, idMutationModelParameter.?,allele.?,ki.?).shaped.<>({ r => import r._; _1.map(_ => MutationModelKiRow.tupled((_1.get, _2.get,_3.get, _4.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    val id: Column[Long] = column[Long]("ID",O.AutoInc, O.PrimaryKey)
    val idMutationModelParameter: Column[Long] = column[Long]("ID_MUTATION_MODEL_PARAMETER")
    val allele: Column[Double] = column[Double]("ALLELE")
    val ki: Column[scala.math.BigDecimal] = column[scala.math.BigDecimal]("KI")

  }
  lazy val MutationModelKi = new TableQuery(tag => new MutationModelKi(tag, Some("APP"), "MUTATION_MODEL_KI"))

  case class MutationDefaultParameterRow(id: Long,locus:String,sex:String,mutationRate:Option[scala.math.BigDecimal])

  /** GetResult implicit for fetching MutationDefaultParameter objects using plain SQL queries */
  implicit def GetResultMutationDefaultParameter(implicit e0: GR[String], e1: GR[Option[String]]): GR[MutationDefaultParameterRow] = GR{
    prs => import prs._
      MutationDefaultParameterRow.tupled((<<[Long],<<[String],<<[String],<<[Option[scala.math.BigDecimal]]))
  }
  /** Table name of table MUTATION_DEFAULT_PARAMETER. Objects of this class serve as prototypes for rows in queries. */
  class MutationDefaultParameter(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[MutationDefaultParameterRow](_tableTag, schema, tableName) {
    def * = (id, locus,sex,mutationRate) <> (MutationDefaultParameterRow.tupled, MutationDefaultParameterRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, locus.?,sex.?,mutationRate).shaped.<>({ r => import r._; _1.map(_ => MutationDefaultParameterRow.tupled((_1.get, _2.get,_3.get, _4))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    val id: Column[Long] = column[Long]("ID",O.AutoInc, O.PrimaryKey)
    val locus: Column[String] = column[String]("LOCUS")
    val sex: Column[String] = column[String]("SEX")
    val mutationRate: Column[Option[scala.math.BigDecimal]] = column[Option[scala.math.BigDecimal]]("MUTATION_RATE")
  }
  lazy val MutationDefaultParameter = new TableQuery(tag => new MutationDefaultParameter(tag, Some("APP"), "MUTATION_DEFAULT_PARAMETER"))

  case class LocusAllelesRow(id: Long,locus: String,allele:Double)
  /** GetResult implicit for fetching LocusAlleles objects using plain SQL queries */
  implicit def GetResultLocusAlleles(implicit e0: GR[String], e1: GR[Option[String]]): GR[LocusAllelesRow] = GR{
    prs => import prs._
      LocusAllelesRow.tupled((<<[Long],<<[String],<<[Double]))
  }
  /** Table name of table LOCUS_ALLELES. Objects of this class serve as prototypes for rows in queries. */
  class LocusAllele(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[LocusAllelesRow](_tableTag, schema, tableName) {
    def * = (id, locus,allele) <> (LocusAllelesRow.tupled, LocusAllelesRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, locus.?,allele.?).shaped.<>({ r => import r._; _1.map(_ => LocusAllelesRow.tupled((_1.get, _2.get,_3.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    val id: Column[Long] = column[Long]("ID",O.AutoInc, O.PrimaryKey)
    val locus: Column[String] = column[String]("LOCUS")
    val allele: Column[Double] = column[Double]("ALLELE")

  }
  lazy val LocusAllele = new TableQuery(tag => new LocusAllele(tag, Some("APP"), "LOCUS_ALLELE"))

  /** Entity class storing rows of table PEDCHECK
   */
  case class PedCheckRow(id: Long, idPedigree: Long, locus: String,globalCode: String)
  /** GetResult implicit for fetching PedigreeRow objects using plain SQL queries */
  implicit def GetResultPedCheckRow(implicit e0: GR[String], e1: GR[Option[String]]): GR[PedCheckRow] = GR{
    prs => import prs._
      PedCheckRow.tupled((<<[Long], <<[Long], <<[String], <<[String]))
  }
  /** Table description of table PED_CHECK. Objects of this class serve as prototypes for rows in queries. */
  class PedCheck(_tableTag: Tag, schema: Option[String], tableName: String) extends Table[PedCheckRow](_tableTag, schema, tableName) {
    def * = (id, idPedigree, locus, globalCode) <> (PedCheckRow.tupled, PedCheckRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, idPedigree.?, locus.?, globalCode.?).shaped.<>({r=>import r._; _1.map(_=> PedCheckRow.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID DBType(BIGINT), AutoInc, PrimaryKey */
    val id: Column[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column ID_PEDIGREE DBType(BIGINT)*/
    val idPedigree: Column[Long] = column[Long]("ID_PEDIGREE")
    /** Database column LOCUS DBType(VARCHAR), Length(100,true), Default(None) */
    val locus: Column[String] = column[String]("LOCUS", O.Length(100,varying=true))
    /** Database column GLOBAL_CODE DBType(DATE), Default(None) */
    val globalCode: Column[String] = column[String]("GLOBAL_CODE")

    /** Foreign key referencing CourtCase (database name PEDCHECK_PEDIGREE_FK) */
    lazy val pedigreeIdFk = foreignKey("PEDCHECK_PEDIGREE_FK", idPedigree, CourtCase)(r => r.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Restrict)

  }
  lazy val PedCheck = new TableQuery(tag => new PedCheck(tag, Some("APP"), "PEDCHECK"))

}

// scalastyle:on