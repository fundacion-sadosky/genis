package profiledata

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api.*
import types.SampleCode
import models.Tables

abstract class ImportToProfileData:
  def fromProtoProfileData(id: Long, labCode: String, country: String, prov: String, assignee: String, desktopSearch: Boolean): Future[(SampleCode, String)]
  def deleteProfileData(sampleCode: String): Future[Int]

@Singleton
class SlickImportToProfileData @Inject() (
  @Named("protoProfileGcDummy") ppGcD: String,
  db: Database
)(using ec: ExecutionContext) extends ImportToProfileData:

  override def fromProtoProfileData(id: Long, labCode: String, country: String, prov: String, assignee: String, desktopSearch: Boolean): Future[(SampleCode, String)] =
    val action = for {
      nextVal <- sql"""SELECT nextval('"APP"."PROFILE_DATA_GLOBAL_CODE_seq"')""".as[Long].head
      laboOpt <- Tables.stashProfileData.filter(_.id === id).map(_.laboratory).result.headOption
      labo     = laboOpt.getOrElse(labCode)
      labRow  <- Tables.laboratories.filter(_.codeName === labo).result.headOption
      preFicGc = if labo == labCode then s"$country-$prov-$labCode-"
                 else labRow.fold(s"$country-$prov-$labCode-") { lab => s"${lab.country}-${lab.province}-${lab.codeName}-" }
      gc       = preFicGc + nextVal
      ppGc     = ppGcD + id
      stashRows <- Tables.stashProfileData.filter(_.id === id).result
      _ <- DBIO.seq(stashRows.map { row =>
        Tables.profilesData += Tables.ProfileDataRow(
          0, row.category, gc, row.internalCode, row.description,
          row.attorney, row.bioMaterialType, row.court,
          row.crimeInvolved, row.crimeType, row.criminalCase,
          row.internalSampleCode, assignee, row.laboratory,
          row.profileExpirationDate, row.responsibleGeneticist,
          row.sampleDate, row.sampleEntryDate, false, None, None, desktopSearch
        )
      }*)
      filRows <- Tables.stashProfileDataFiliation.filter(_.profileData === ppGc).result
      _ <- DBIO.seq(filRows.map { f =>
        Tables.profileMetaDataFiliations += Tables.ProfileDataFiliationRow(
          0, gc, f.fullName, f.nickname, f.birthday, f.birthPlace,
          f.nationality, f.identification, f.identificationIssuingAuthority, f.address
        )
      }*)
      resRows <- Tables.stashProfileDataFiliationResources.filter(_.profileDataFiliation === ppGc).result
      _ <- DBIO.seq(resRows.map { r =>
        Tables.profileMetaDataFiliationResources += Tables.ProfileDataFiliationResourcesRow(0, gc, r.resource, r.resourceType)
      }*)
    } yield (SampleCode(gc), labo)

    db.run(action.transactionally)

  override def deleteProfileData(sampleCode: String): Future[Int] =
    val action = for {
      _ <- Tables.profileMetaDataFiliationResources.filter(_.profileDataFiliation === sampleCode).delete
      _ <- Tables.profileMetaDataFiliations.filter(_.profileData === sampleCode).delete
      n <- Tables.profilesData.filter(_.globalCode === sampleCode).delete
    } yield n
    db.run(action.transactionally)