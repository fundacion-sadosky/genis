package models

import slick.jdbc.PostgresProfile.api._

package models

import slick.jdbc.PostgresProfile.api._

// ----- PROFILE_DATA -----

// Row unchanged from legacy Tables.scala
final case class ProfileDataRow(
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

// Slick 3: class takes only (tag) — schema/tableName hardcoded in the class,
// no longer passed as constructor params as in legacy Slick 2 TableQuery instantiation.
class ProfileDataTable(tag: Tag)
  extends Table[ProfileDataRow](tag, Some("APP"), "PROFILE_DATA") {

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
  def profileExpirationDate = column[Option[java.sql.Date]]("PROFILE_EXPIRATION_DATE", O.Default(None))
  def responsibleGeneticist = column[Option[String]]("RESPONSIBLE_GENETICIST", O.Length(50, varying = true), O.Default(None))
  def sampleDate           = column[Option[java.sql.Date]]("SAMPLE_DATE", O.Default(None))
  def sampleEntryDate      = column[Option[java.sql.Date]]("SAMPLE_ENTRY_DATE", O.Default(None))
  def deleted              = column[Boolean]("DELETED", O.Default(false))
  def deletedSolicitor     = column[Option[String]]("DELETED_SOLICITOR", O.Length(100, varying = true), O.Default(None))
  def deletedMotive        = column[Option[String]]("DELETED_MOTIVE", O.Length(8192, varying = true), O.Default(None))
  def fromDesktopSearch    = column[Boolean]("FROM_DESKTOP_SEARCH", O.Default(false))

  // Slick 3: <> syntax unchanged, but mapTo is preferred for simple case classes.
  // Keeping tupled/unapply pattern for consistency with rest of codebase.
  def * = (
    id, category, globalCode, internalCode, description, attorney,
    bioMaterialType, court, crimeInvolved, crimeType, criminalCase,
    internalSampleCode, assignee, laboratory, profileExpirationDate,
    responsibleGeneticist, sampleDate, sampleEntryDate, deleted,
    deletedSolicitor, deletedMotive, fromDesktopSearch
  ) <> (ProfileDataRow.tupled, ProfileDataRow.unapply)

  def idxGlobalCode        = index("PROFILE_DATA_GLOBAL_CODE_KEY_INDEX_D", globalCode, unique = true)
  def idxInternalSampleCode = index("PROFILE_DATA_INTERNAL_SAMPLE_CODE_KEY_INDEX_D", internalSampleCode, unique = true)
}

// ----- PROFILE_DATA_FILIATION -----

final case class ProfileDataFiliationRow(
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

class ProfileDataFiliationTable(tag: Tag)
  extends Table[ProfileDataFiliationRow](tag, Some("APP"), "PROFILE_DATA_FILIATION") {

  def id                            = column[Long]("ID", O.AutoInc, O.PrimaryKey)
  def profileData                   = column[String]("PROFILE_DATA", O.Length(100, varying = true))
  def fullName                      = column[Option[String]]("FULL_NAME", O.Length(150, varying = true), O.Default(None))
  def nickname                      = column[Option[String]]("NICKNAME", O.Length(150, varying = true), O.Default(None))
  def birthday                      = column[Option[java.sql.Date]]("BIRTHDAY", O.Default(None))
  def birthPlace                    = column[Option[String]]("BIRTH_PLACE", O.Length(100, varying = true), O.Default(None))
  def nationality                   = column[Option[String]]("NATIONALITY", O.Length(50, varying = true), O.Default(None))
  def identification                = column[Option[String]]("IDENTIFICATION", O.Length(100, varying = true), O.Default(None))
  def identificationIssuingAuthority = column[Option[String]]("IDENTIFICATION_ISSUING_AUTHORITY", O.Length(100, varying = true), O.Default(None))
  def address                       = column[Option[String]]("ADDRESS", O.Length(100, varying = true), O.Default(None))

  def * = (
    id, profileData, fullName, nickname, birthday, birthPlace,
    nationality, identification, identificationIssuingAuthority, address
  ) <> (ProfileDataFiliationRow.tupled, ProfileDataFiliationRow.unapply)

  def profileDataFk = foreignKey(
    "PROFILE_DATA_FILIATION_FK", profileData, ProfileDataTables.profileData
  )(_.globalCode, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Restrict)

  def idxProfileData = index("PROFILE_DATA_FILIATION_CODE_KEY_INDEX_D", profileData, unique = true)
}

// ----- PROFILE_DATA_FILIATION_RESOURCES -----

// Changed from legacy: resource column type changed from java.sql.Blob to Array[Byte].
// Legacy used SerialBlob as a wrapper; Slick 3 + PostgreSQL maps bytea directly to Array[Byte].
// All read/write code using SerialBlob and .getBytes() must be updated in the repository.
final case class ProfileDataFiliationResourcesRow(
  id: Long,
  profileDataFiliation: String,
  resource: Array[Byte],
  resourceType: String
)

class ProfileDataFiliationResourcesTable(tag: Tag)
  extends Table[ProfileDataFiliationResourcesRow](tag, Some("APP"), "PROFILE_DATA_FILIATION_RESOURCES") {

  def id                   = column[Long]("ID", O.AutoInc, O.PrimaryKey)
  def profileDataFiliation = column[String]("PROFILE_DATA_FILIATION", O.Length(100, varying = true))
  // Changed from legacy: java.sql.Blob -> Array[Byte] (see row comment above)
  def resource             = column[Array[Byte]]("RESOURCE")
  def resourceType         = column[String]("RESOURCE_TYPE", O.Length(1, varying = true))

  def * = (id, profileDataFiliation, resource, resourceType) <> (
    ProfileDataFiliationResourcesRow.tupled,
    ProfileDataFiliationResourcesRow.unapply
  )

  def profileDataFiliationFk = foreignKey(
    "PROFILE_DATA_FILIATION_RESOURSE_FK",
    profileDataFiliation,
    ProfileDataTables.profileDataFiliation
  )(_.profileData, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Restrict)
}

// ----- PROFILE_UPLOADED -----

// Row unchanged from legacy Tables.scala
final case class ProfileUploadedRow(
  id: Long,
  globalCode: String,
  status: Long,
  motive: Option[String] = None,
  interconnectionError: Option[String] = None,
  userName: Option[String] = None
)

class ProfileUploadedTable(tag: Tag)
  extends Table[ProfileUploadedRow](tag, Some("APP"), "PROFILE_UPLOADED") {

  def id                 = column[Long]("ID", O.PrimaryKey)
  def globalCode         = column[String]("GLOBAL_CODE", O.Length(100, varying = true))
  def status             = column[Long]("STATUS")
  def motive             = column[Option[String]]("MOTIVE")
  // Renamed from legacy: interconnection_error -> interconnectionError (camelCase convention)
  def interconnectionError = column[Option[String]]("INTERCONNECTION_ERROR")
  def userName           = column[Option[String]]("USER")

  def * = (id, globalCode, status, motive, interconnectionError, userName) <> (
    ProfileUploadedRow.tupled,
    ProfileUploadedRow.unapply
  )
}

// ----- PROFILE_SENT -----

// Row unchanged from legacy Tables.scala
final case class ProfileSentRow(
  id: Long,
  labCode: String,
  globalCode: String,
  status: Long,
  motive: Option[String] = None,
  interconnectionError: Option[String] = None,
  userName: Option[String] = None
)

class ProfileSentTable(tag: Tag)
  extends Table[ProfileSentRow](tag, Some("APP"), "PROFILE_SENT") {

  def id               = column[Long]("ID", O.PrimaryKey)
  def labCode          = column[String]("LABCODE", O.Length(100, varying = true))
  def globalCode       = column[String]("GLOBAL_CODE", O.Length(100, varying = true))
  def status           = column[Long]("STATUS")
  def motive           = column[Option[String]]("MOTIVE")
  def interconnectionError = column[Option[String]]("INTERCONNECTION_ERROR")
  def userName         = column[Option[String]]("USER")

  def * = (id, labCode, globalCode, status, motive, interconnectionError, userName) <> (
    ProfileSentRow.tupled,
    ProfileSentRow.unapply
  )
}

// ----- PROFILE_RECEIVED -----

// Row unchanged from legacy Tables.scala
final case class ProfileReceivedRow(
  globalCode: String,
  labCode: String,
  status: Long,
  motive: Option[String] = None,
  userName: Option[String] = None,
  isCategoryModification: Boolean,
  interconnectionError: Option[String] = None
)

class ProfileReceivedTable(tag: Tag)
  extends Table[ProfileReceivedRow](tag, Some("APP"), "PROFILE_RECEIVED") {

  def globalCode            = column[String]("GLOBAL_CODE", O.Length(100, varying = true), O.PrimaryKey)
  def labCode               = column[String]("LABCODE", O.Length(100, varying = true))
  def status                = column[Long]("STATUS")
  def motive                = column[Option[String]]("MOTIVE")
  def userName              = column[Option[String]]("USER")
  def isCategoryModification = column[Boolean]("IS_CATEGORY_MODIFICATION")
  def interconnectionError  = column[Option[String]]("INTERCONNECTION_ERROR")

  def * = (globalCode, labCode, status, motive, userName, isCategoryModification, interconnectionError) <> (
    ProfileReceivedRow.tupled,
    ProfileReceivedRow.unapply
  )
}

// ----- EXTERNAL_PROFILE_DATA -----

// Row unchanged from legacy Tables.scala
final case class ExternalProfileDataRow(
  id: Long,
  laboratoryOrigin: String,
  laboratoryImmediate: String
)

class ExternalProfileDataTable(tag: Tag)
  extends Table[ExternalProfileDataRow](tag, Some("APP"), "EXTERNAL_PROFILE_DATA") {

  def id                  = column[Long]("ID", O.PrimaryKey)
  def laboratoryOrigin    = column[String]("LABORATORY_INSTANCE_ORIGIN", O.Length(50, varying = true))
  def laboratoryImmediate = column[String]("LABORATORY_INSTANCE_INMEDIATE", O.Length(50, varying = true))

  def * = (id, laboratoryOrigin, laboratoryImmediate) <> (
    ExternalProfileDataRow.tupled,
    ExternalProfileDataRow.unapply
  )
}

// ----- PROFILE_DATA_MOTIVE -----

// Row unchanged from legacy Tables.scala
final case class ProfileDataMotiveRow(
  id: Long,
  idProfileData: Long,
  deletedDate: java.sql.Timestamp,
  idDeletedMotive: Long
)

class ProfileDataMotiveTable(tag: Tag)
  extends Table[ProfileDataMotiveRow](tag, Some("APP"), "PROFILE_DATA_MOTIVE") {

  def id               = column[Long]("ID", O.AutoInc, O.PrimaryKey)
  def idProfileData    = column[Long]("ID_PROFILE_DATA")
  def deletedDate      = column[java.sql.Timestamp]("DELETED_DATE")
  def idDeletedMotive  = column[Long]("ID_DELETED_MOTIVE")

  def * = (id, idProfileData, deletedDate, idDeletedMotive) <> (
    ProfileDataMotiveRow.tupled,
    ProfileDataMotiveRow.unapply
  )
}

// ----- TableQuery instances -----

// Slick 3: TableQuery[T] takes a single (Tag => T) function.
// Legacy used new TableQuery(tag => new T(tag, Some("APP"), "TABLE_NAME")) with schema/table as params.
// Here schema and table are hardcoded in each Table class, so instantiation is simpler.
object ProfileDataTables {
  val profileData              = TableQuery[ProfileDataTable]
  val profileDataFiliation     = TableQuery[ProfileDataFiliationTable]
  val profileDataFiliationResources = TableQuery[ProfileDataFiliationResourcesTable]
  val profileUploaded          = TableQuery[ProfileUploadedTable]
  val profileSent              = TableQuery[ProfileSentTable]
  val profileReceived          = TableQuery[ProfileReceivedTable]
  val externalProfileData      = TableQuery[ExternalProfileDataTable]
  val profileDataMotive        = TableQuery[ProfileDataMotiveTable]
}